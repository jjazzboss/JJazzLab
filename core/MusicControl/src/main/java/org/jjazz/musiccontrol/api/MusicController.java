/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.musiccontrol.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.playbacksession.EndOfPlaybackActionProvider;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.Exceptions;
import org.jjazz.musiccontrol.api.playbacksession.ControlTrackProvider;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.outputsynth.spi.OutputSynthManager;

/**
 * Control the music playback of a PlaybackSession.
 * <p>
 * The PlaybackSession provides the Sequence to be played and various related data which impact how the MusicController manage the Sequence.
 * <p>
 * Property changes are fired for:<br>
 * - start/pause/stop/disabled state changes<br>
 * <p>
 * Use NoteListener to get notified of note ON/OFF events during playback. Use PlaybackListener to get notified of other events such as beat/chord symbol
 * changes -this requires a PlaybackSession which implements ControlTrackprovider. Note that listeners will be notified out of the Swing EDT.<br>
 * The current output synth latency is taken into account to fire events to NoteListeners and PlaybackListeners.
 * <p>
 * Use acquireSequencer()/releaseSequencer() if you want to use the Java system sequencer independently.
 */
public class MusicController implements PropertyChangeListener, MetaEventListener
{

    /**
     * oldValue=old State, newValue=new State
     */
    public static final String PROP_STATE = "PropPlaybackState";
    /**
     * A new PlaybackSession was set.
     * <p>
     * oldValue=old session, newValue=new session
     *
     * @see #setPlaybackSession(org.jjazz.musiccontrol.api.playbacksession.PlaybackSession, boolean)
     */
    public static final String PROP_PLAYBACK_SESSION = "PropPlaybackSession";


    /**
     * The playback states.
     * <p>
     * Property change listeners are notified with property PROP_STATE.
     */
    public enum State
    {
        DISABLED,
        STOPPED,
        PAUSED,
        PLAYING
    }
    private static MusicController INSTANCE;
    private Sequencer sequencer;
    private PlaybackSession playbackSession;

    private State state;
    /**
     * The current beat position during playback (for ControlTrackProvider sessions only).
     */
    private final Position currentBeatPosition;
    /**
     * The current chord symbol during playback (for ControlTrackProvider sessions only).
     */
    private CLI_ChordSymbol currentChordSymbol;
    /**
     * The current song part during playback (for ControlTrackProvider sessions only).
     */
    private SongPart currentSongPart;

    /**
     * Sequencer lock by an external entity.
     */
    private Object sequencerLockHolder;

    /**
     * The tempo factor to go from MidiConst.SEQUENCER_REF_TEMPO to song tempo.
     */
    private float songTempoFactor;
    /**
     * The tempo factor of the SongPart being played.
     */
    private float songPartTempoFactor;
    private int audioLatency;
    /**
     * Keep track of active timers used to compensate the audio latency.
     * <p>
     * Needed to force stop them when sequencer is stopped/paused by user.
     */
    private final Set<Timer> audioLatencyTimers;
    /**
     * Our MidiReceiver to be able to fire events to NoteListeners and PlaybackListener (midiActivity).
     */
    private McReceiver receiver;
    /**
     * If true display built sequence when it is built
     */
    private boolean debugPlayedSequence;
    private final PropertyChangeSupport pcs;
    private final List<PlaybackListener> playbackListeners;
    private final List<NoteListener> noteListeners;
    private static final Logger LOGGER = Logger.getLogger(MusicController.class.getSimpleName());

    public static MusicController getInstance()
    {
        synchronized (MusicController.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MusicController();
            }
        }
        return INSTANCE;
    }

    /**
     * The sequencer tempo must be set to 100.
     */
    private MusicController()
    {
        this.pcs = new PropertyChangeSupport(this);
        this.playbackListeners = new ArrayList<>();
        this.noteListeners = new ArrayList<>();
        this.songPartTempoFactor = 1;
        this.audioLatencyTimers = new HashSet<>();
        this.currentBeatPosition = new Position();
        this.state = State.STOPPED;
        this.sequencer = JJazzMidiSystem.getInstance().getDefaultSequencer();
        this.receiver = new McReceiver();
        initSequencer();
        this.sequencerLockHolder = null;


        // Get notified of the notes sent by the Sequencer
        try
        {
            Transmitter t = sequencer.getTransmitter();
            t.setReceiver(receiver);
        } catch (MidiUnavailableException ex)
        {
            // Should never occur
            Exceptions.printStackTrace(ex);
        }


        // Listen to default OutputSynth changes, in order to listen to its AudioLatency changes
        var osm = OutputSynthManager.getDefault();
        osm.addPropertyChangeListener(OutputSynthManager.PROP_DEFAULT_OUTPUTSYNTH, this);
        var outSynth = osm.getDefaultOutputSynth();
        if (outSynth != null)
        {
            audioLatency = outSynth.getUserSettings().getAudioLatency();
            outSynth.getUserSettings().addPropertyChangeListener(this);
        }

        LOGGER.info("MusicController() Started");
    }

    /**
     * Try to temporarily acquire the java sequencer opened and connected to the default Midi out device.
     * <p>
     * You can acquire the sequencer only if MusicController is in the STOPPED state. If acquisition is successful the MusicController is put in DISABLED state
     * since it can't use the sequencer anymore. When the caller is done with the sequencer, he must call releaseSequencer(lockHolder) so that the
     * MusicController can use it again.
     *
     * @param lockHolder Must be non-null
     * @return Null if sequencer has already a different lock or if MusicController is in the PLAYING state.
     */
    public synchronized Sequencer acquireSequencer(Object lockHolder)
    {
        if (lockHolder == null)
        {
            throw new NullPointerException("lockHolder");
        }

        LOGGER.log(Level.FINE, "acquireSequencer() -- lockHolder={0}", lockHolder);

        if (sequencerLockHolder == lockHolder)
        {
            // lock already acquired
            return sequencer;
        } else if (sequencerLockHolder == null && isStopped())
        {
            // lock acquired by external entity: get disabled
            sequencerLockHolder = lockHolder;


            closeCurrentPlaybackSession();


            // Remove the MusicController listeners
            releaseSequencer();

            State oldState = getState();
            setState(State.DISABLED);

            LOGGER.log(Level.FINE,
                    "acquireSequencer() external lock acquired.  MusicController released the sequencer, oldState={0} newState=DISABLED",
                    oldState);

            return sequencer;
        } else
        {
            LOGGER.log(Level.FINE, "acquireSequencer() can''t give lock to {0}, current lock={1}", new Object[]
            {
                lockHolder,
                sequencerLockHolder
            });
            return null;
        }
    }

    /**
     * Release the external sequencer lock.
     * <p>
     * Once released, MusicController becomes able to take the Sequencer lock when playing music.
     *
     * @param lockHolder
     * @throws IllegalArgumentException If lockHolder does not match
     */
    public synchronized void releaseSequencer(Object lockHolder)
    {
        if (lockHolder == null || sequencerLockHolder != lockHolder)
        {
            throw new IllegalArgumentException("lockHolder=" + lockHolder + " sequencerLockHolder=" + sequencerLockHolder);
        }

        LOGGER.log(Level.FINE, "releaseSequencer() -- lockHolder={0}", lockHolder);

        sequencerLockHolder = null;

        sequencer.stop(); // Just to make sure

        // Initialize sequencer for MusicController
        initSequencer();

        // Change state
        assert isDisabled();
        setState(State.STOPPED);
    }

    /**
     * Set the current playback session which will be used by the play() method.
     * <p>
     * The method tries to generate the sequence if required. If MusicController is paused then current playback is stopped. PlaybackListeners are notified only
     * if session implements ControlTrackProvider. Fire a PROP_PLAYBACK_SESSION change event.
     *
     * @param session Can be null. If not null, must be in NEW or GENERATED state, and can't be dirty.
     * @param silent  If false and session needs to be generated, a progress dialog is shown while generating music.
     * @throws org.jjazz.rhythm.api.MusicGenerationException E.g. if song is already playing, missing start section chord, etc.
     * @throws IllegalStateException                         If session is dirty or is CLOSED.
     *
     * @see #play(int)
     */
    public void setPlaybackSession(PlaybackSession session, boolean silent) throws MusicGenerationException
    {
        if (session == playbackSession)
        {
            return;
        }

        if (session != null && (session.isDirty() || session.getState().equals(PlaybackSession.State.CLOSED)))
        {
            throw new IllegalStateException("session=" + session);
        }


        // Check state
        switch (state)
        {
            case DISABLED -> throw new MusicGenerationException(ResUtil.getString(getClass(), "PLAYBACK_IS_DISABLED"));
            case STOPPED ->
            {
            }
            case PAUSED -> stop();
            case PLAYING -> throw new MusicGenerationException(ResUtil.getString(getClass(), "A_SONG_IS_ALREADY_PLAYING"));
            default -> throw new AssertionError(state.name());
        }

        try
        {
            // Reset sequence
            sequencer.setSequence((Sequence) null);
        } catch (InvalidMidiDataException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        // Update the session
        closeCurrentPlaybackSession();
        PlaybackSession oldSession = playbackSession;
        playbackSession = session;

        if (playbackSession != null)
        {
            playbackSession.addPropertyChangeListener(this);

            // Try to pre-generate the sequence
            if (playbackSession.getState().equals(PlaybackSession.State.NEW))
            {
                playbackSession.generate(silent);         // Throws MusicGenerationException
            }
        }


        pcs.firePropertyChange(PROP_PLAYBACK_SESSION, oldSession, playbackSession);
    }

    /**
     * Play the current playback session from the specified bar.
     * <p>
     * Do nothing if no playback session set.
     *
     * @param fromBarIndex Must be consistent with current playback session
     * @throws MusicGenerationException If a problem occurred which prevents song playing: no Midi out, rhythm music generation problem, MusicController state
     *                                  is not PAUSED nor STOPPED, etc.
     * @throws IllegalStateException    If current session is not in the GENERATED state, or if fromBarIndex is invalid
     * @see #setPlaybackSession(org.jjazz.musiccontrol.api.playbacksession.PlaybackSession, boolean)
     */
    public void play(int fromBarIndex) throws MusicGenerationException
    {
        if (playbackSession == null)
        {
            return;
        }


        if (!playbackSession.getState().equals(PlaybackSession.State.GENERATED))
        {
            throw new IllegalArgumentException("playbackSession=" + playbackSession + " fromBarIndex=" + fromBarIndex);
        }


        // Check that a Midi ouput device is set
        checkMidi();                // throws MusicGenerationException


        // Check fomrBarIndex argument
        if (playbackSession.getBarRange() != null && !playbackSession.getBarRange().contains(fromBarIndex))
        {
            throw new IllegalArgumentException("invalid fromBarIndex=" + fromBarIndex + " playbackSession=" + playbackSession);
        }


        // Check state
        switch (state)
        {
            case DISABLED -> throw new MusicGenerationException(ResUtil.getString(getClass(), "PLAYBACK_IS_DISABLED"));
            case STOPPED, PAUSED ->
            {
            }
            case PLAYING -> throw new MusicGenerationException(ResUtil.getString(getClass(), "A_SONG_IS_ALREADY_PLAYING"));
            default -> throw new AssertionError(state.name());
        }


        if (debugPlayedSequence)
        {
            SongContext sgContext = getSongContext(playbackSession);        // Can be null
            String songName = sgContext != null ? sgContext.getSong().getName() : "unknown";
            LOGGER.log(Level.INFO, "play() song={0} sequence :", songName);
            LOGGER.info(MidiUtilities.toString(playbackSession.getSequence()));
        }


        // Set sequence
        try
        {
            // This also stops sequencer and resets Mute, Solo, LoopStart/End points, LoopCount, tick position
            sequencer.setSequence(playbackSession.getSequence()); // Can raise InvalidMidiDataException

        } catch (InvalidMidiDataException ex)
        {
            closeCurrentPlaybackSession();
            throw new MusicGenerationException(ex.getMessage());
        }


        // Loop settings
        sequencer.setLoopStartPoint(playbackSession.getLoopStartTick());
        sequencer.setLoopEndPoint(playbackSession.getLoopEndTick());
        sequencer.setLoopCount(playbackSession.getLoopCount());


        // Mute/unmute tracks
        updateTracksMuteStatus();
        songPartTempoFactor = 1f;
        songTempoChanged(playbackSession.getTempo());


        // Enable events to PlaybackListeners
        if (playbackSession instanceof ControlTrackProvider)
        {
            firePlaybackListenerEnabledChanged(true);
        }


        // Set sequencer position
        setPosition(fromBarIndex);


        // Start or restart the sequencer
        seqStart();


        // Change state
        setState(State.PLAYING);

    }

    /**
     * Resume playback from the pause state.
     * <p>
     * If state is not PAUSED, nothing is done.
     *
     * @throws MusicGenerationException For example if state==DISABLED
     * @throws IllegalStateException    If current playback session is not in the GENERATED state.
     */
    public void resume() throws MusicGenerationException
    {
        if (isDisabled())
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "PLAYBACK_IS_DISABLED"));
        } else if (!isPaused())
        {
            return;
        }

        // Need a clean session
        if (!playbackSession.getState().equals(PlaybackSession.State.GENERATED))
        {
            throw new IllegalStateException("playbackSession.getState()=" + playbackSession.getState());
        }


        checkMidi();                // throws MusicGenerationException


        // Let's go again
        seqStart();


        // Update state
        setState(State.PLAYING);
    }

    /**
     * Stop the playback of the sequence if it was playing, and reset the position to the beginning of the sequence.
     * <p>
     */
    public void stop()
    {
        switch (state)
        {
            case DISABLED, STOPPED ->
            {
                return;
            }
            case PAUSED ->
            {
            }
            case PLAYING ->
            {
                sequencer.stop();
                clearPendingEvents();
            }
            default -> throw new AssertionError(state.name());
        }
        // Nothing


        // Update state
        currentSongPart = null;
        currentChordSymbol = null;
        songPartTempoFactor = 1;
        setState(State.STOPPED);

        // Position must be reset after the stop so that playback beat change tracking listeners are not reset upon stop   
        int barIndex = playbackSession.getBarRange() != null ? playbackSession.getBarRange().from : 0;
        setPosition(barIndex);

        // Action to be fired after state change
        executeEndOfPlaybackAction();

    }

    /**
     * Stop the playback of the sequence and leave the position unchanged.
     * <p>
     * If state is not PLAYING, do nothing. If session is dirty, use stop() instead.
     */
    public void pause()
    {
        if (!isPlaying())
        {
            return;
        }

        // If session needs to be regenerated use stop() instead
        if (playbackSession.isDirty())
        {
            stop();
            return;
        }


        sequencer.stop();
        clearPendingEvents();


        // Change state
        setState(State.PAUSED);

        // Action to be fired after state change
        executeEndOfPlaybackAction();
    }


    /**
     * Change the current bar when in PAUSED state.
     * <p>
     * Do nothing if not in PAUSED state.
     *
     * @param barIndex
     * @throws IllegalArgumentException If bar index is not valid for the current PlaybackSession.
     */
    public void changePausedBar(int barIndex)
    {
        if (!isPaused())
        {
            return;
        }
        if (!playbackSession.getBarRange().contains(barIndex))
        {
            throw new IllegalArgumentException(
                    "Invalid barIndex=" + barIndex + " playbackSession.getBarRange()=" + playbackSession.getBarRange());
        }

        setPosition(barIndex);
    }

    /**
     * The current playback position updated at every natural beat (eg 0, 1, 2, 3 in 4/4).
     * <p>
     * Note: value is meaningful only for PlaybackSessions which are also ControlTrackProviders. Otherwise returned value is always bar=0, beat=0.
     *
     * @return
     * @see PlaybackListener
     */
    public Position getCurrentBeatPosition()
    {
        return currentBeatPosition;
    }

    /**
     * The current CLI_ChordSymbol being played.
     * <p>
     * Note: value is meaningful only when playback is on or paused, and for PlaybackSessions which are also ControlTrackProviders. Otherwise return value is
     * null.
     *
     * @return Can be null.
     * @see PlaybackListener
     */
    public CLI_ChordSymbol getCurrentChordSymbol()
    {
        return currentChordSymbol;
    }

    /**
     * The current SongPart being played.
     * <p>
     * Note: value is meaningful only when playback is on or paused, and for PlaybackSessions which are also ControlTrackProviders and SongContextProviders.
     * Otherwise return value is null.
     *
     * @return Can be null.
     * @see PlaybackListener
     */
    public SongPart getCurrentSongPart()
    {
        return currentSongPart;
    }

    public State getState()
    {
        return state;
    }

    /**
     * Get the current playback session.
     *
     * @return Can be null if no song has ever been played.
     */
    public PlaybackSession getPlaybackSession()
    {
        return playbackSession;
    }


    /**
     * Helper method equivalent to getState() == State.PLAYING.
     *
     * @return
     */
    public boolean isPlaying()
    {
        return state == State.PLAYING;
    }

    /**
     * Helper method equivalent to getState() == State.PAUSED.
     *
     * @return
     */
    public boolean isPaused()
    {
        return state == State.PAUSED;
    }

    /**
     * Helper method equivalent to getState() == State.STOPPED.
     *
     * @return
     */
    public boolean isStopped()
    {
        return state == State.STOPPED;
    }

    /**
     * Helper method equivalent to getState() == State.DISABLED.
     *
     * @return
     */
    public boolean isDisabled()
    {
        return state == State.DISABLED;
    }

    public void setDebugPlayedSequence(boolean b)
    {
        debugPlayedSequence = b;
    }

    public boolean isDebugPlayedSequence()
    {
        return debugPlayedSequence;
    }

    /**
     * Add a listener of note ON/OFF events.
     * <p>
     * Listeners will be called out of the Swing EDT (Event Dispatch Thread). Can not be called if MusicController is playing.
     *
     * @param listener
     */
    public synchronized void addNoteListener(NoteListener listener)
    {
        if (isPlaying())
        {
            throw new IllegalStateException("addNoteListener() called while MusicController is playing");
        }
        if (!noteListeners.contains(listener))
        {
            noteListeners.add(listener);
        }
    }

    /**
     * Remove a NoteListener. Can not be called if MusicController is playing.
     *
     * @param listener
     */
    public synchronized void removeNoteListener(NoteListener listener)
    {
        if (isPlaying())
        {
            throw new IllegalStateException("addNoteListener() called while MusicController is playing");
        }
        noteListeners.remove(listener);
    }

    /**
     * Add a listener to be notified of playback bar/beat changes events etc.
     * <p>
     * Listeners will be called out of the Swing EDT (Event Dispatch Thread).
     *
     * @param listener
     */
    public synchronized void addPlaybackListener(PlaybackListener listener)
    {
        if (!playbackListeners.contains(listener))
        {
            playbackListeners.add(listener);
        }
    }

    public synchronized void removePlaybackListener(PlaybackListener listener)
    {
        playbackListeners.remove(listener);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }


    //-----------------------------------------------------------------------
    // Implementation of the MetaEventListener interface
    //-----------------------------------------------------------------------
    @Override
    public void meta(MetaMessage meta)
    {
        switch (meta.getType())
        {
            case 47 -> // Meta Event for end of sequence        
            {
                // This method  is called from the Sequencer thread, NOT from the EDT !
                // So if this method impacts the UI, it must use SwingUtilities.InvokeLater() (or InvokeAndWait())
                LOGGER.fine("Sequence end reached");
                SwingUtilities.invokeLater(() -> stop());

            }
            case ControlTrack.CHORD_SYMBOL_META_EVENT_TYPE ->
            {
                if (playbackSession instanceof ControlTrackProvider ctProvider)
                {
                    ControlTrack controlTrack = ctProvider.getControlTrack(); // Might be null for a specific BaseSongSession instance
                    if (controlTrack != null)
                    {
                        var cliCs = controlTrack.getChordSymbol(meta);
                        if (cliCs != null)
                        {
                            fireChordSymbolChanged(cliCs);
                        } else
                        {
                            LOGGER.log(Level.WARNING, "meta() Unexpected null chord symbol meta={0}", meta);
                        }
                    }
                }
            }
            case ControlTrack.POSITION_META_EVENT_TYPE ->
            {
                if (playbackSession instanceof ControlTrackProvider ctProvider)
                {
                    ControlTrack controlTrack = ctProvider.getControlTrack(); // Might be null for a specific BaseSongSession instance
                    if (controlTrack != null && playbackSession.getLoopStartTick() != -1)
                    {
                        Position pos = controlTrack.getPosition(meta);
                        float posInBeats = controlTrack.getPositionInBeats(meta);
                        if (pos != null)
                        {
                            // LOGGER.severe("meta() position received pos=" + pos + " posInBeats=" + posInBeats);
                            updateCurrentPosition(pos.getBar(), pos.getBeat(), posInBeats);
                        } else
                        {
                            LOGGER.log(Level.WARNING, "meta() Unexpected null position meta={0}", meta);
                        }

                    }
                }
            }


            case SongSequenceBuilder.TEMPO_FACTOR_META_EVENT_TYPE ->
            {
                songPartTempoFactor = SongSequenceBuilder.getTempoFactor(meta);
                updateTempoFactor();
            }

            default ->
            {
                // Nothing
            }
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);


        // Always enabled changes
        if (e.getSource() == OutputSynthManager.getDefault())
        {
            if (e.getPropertyName().equals(OutputSynthManager.PROP_DEFAULT_OUTPUTSYNTH))
            {
                OutputSynth oldSynth = (OutputSynth) e.getOldValue();
                OutputSynth newSynth = (OutputSynth) e.getNewValue();
                if (oldSynth != null)
                {
                    oldSynth.getUserSettings().removePropertyChangeListener(this);
                }
                if (newSynth != null)
                {
                    newSynth.getUserSettings().addPropertyChangeListener(this);
                    audioLatency = newSynth.getUserSettings().getAudioLatency();
                } else
                {
                    audioLatency = 0;
                }
            }
        } else if (e.getSource() instanceof OutputSynth.UserSettings)
        {
            if (e.getPropertyName().equals(OutputSynth.UserSettings.PROP_AUDIO_LATENCY))
            {
                // latency of the default OutputSynth has changed
                audioLatency = (int) e.getNewValue();
            }
        }

        if (e.getSource() == playbackSession)
        {
            switch (e.getPropertyName())
            {
                case PlaybackSession.PROP_STATE ->
                {
                    switch (playbackSession.getState())
                    {
                        case NEW, GENERATED ->
                        {
                        }
                        case CLOSED ->
                        {
                            stop();
                            closeCurrentPlaybackSession();
                        }
                        default -> throw new AssertionError(playbackSession.getState().name());

                    }
                    // Nothing
                }
                case PlaybackSession.PROP_DIRTY ->
                {
                    if (isPaused())
                    {
                        stop();
                    }
                }
                case PlaybackSession.PROP_TEMPO -> songTempoChanged((Integer) e.getNewValue());
                case PlaybackSession.PROP_MUTED_TRACKS -> updateTracksMuteStatus();
                case PlaybackSession.PROP_LOOP_COUNT ->
                {
                    if (!isDisabled())
                    {
                        int lc = (Integer) e.getNewValue();
                        sequencer.setLoopCount(lc);
                    }
                }
                case ControlTrackProvider.ENABLED_STATE -> firePlaybackListenerEnabledChanged((Boolean) e.getNewValue());
                default ->
                {
                }
            }
        }
    }


    // =====================================================================================
    // Private methods
    // =====================================================================================
    /**
     * Initialize the sequencer for our usage.
     */
    private void initSequencer()
    {
        sequencer.addMetaEventListener(this);
        sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);
        receiver.setEnabled(true);
    }

    /**
     * Remove our listeners.
     */
    private void releaseSequencer()
    {
        sequencer.removeMetaEventListener(this);
        receiver.setEnabled(false);
    }


    private void setPosition(int fromBar)
    {
        assert !isDisabled() : "state=" + state;
        LOGGER.log(Level.FINE, "setPosition() fromBar={0}", fromBar);
        long tick = Math.max(playbackSession.getTick(fromBar), 0);
        sequencer.setTickPosition(tick);
        float posInBeats = -1;
        if (playbackSession instanceof SongContextProvider scp)
        {

            var sgContext = scp.getSongContext();
            long relativeTick = getRelativeTickFromLoopStart(tick);
            posInBeats = sgContext.toPositionInBeats(relativeTick);
            LOGGER.log(Level.FINE, "setPosition()   > song session: relativeTick={1} posInBeats={2}", new Object[]
            {
                relativeTick,
                posInBeats
            });
        }
        updateCurrentPosition(fromBar, 0, posInBeats);
    }

    private void firePlaybackListenerEnabledChanged(boolean b)
    {
        // No need to use a latency aware event
        for (PlaybackListener pl : playbackListeners.toArray(PlaybackListener[]::new))
        {
            if (playbackSession == null || pl.isAccepted(playbackSession))
            {
                pl.enabledChanged(b);
            }
        }
    }

    private void fireChordSymbolChanged(CLI_ChordSymbol cliCs)
    {
        Objects.requireNonNull(cliCs);

        if (currentChordSymbol != cliCs)
        {
            currentChordSymbol = cliCs;

            fireLatencyAwareEvent(() -> 
            {
                for (PlaybackListener pl : playbackListeners.toArray(PlaybackListener[]::new))
                {
                    // playbackSession might be null because in the meantime of the latency firing session was closed ?
                    if (playbackSession == null || pl.isAccepted(playbackSession))
                    {
                        pl.chordSymbolChanged(cliCs);
                    }
                }
            });
        }
    }

    private void fireBeatChanged(Position oldPos, Position newPos, float newPosInBeats)
    {
        fireLatencyAwareEvent(() -> 
        {
            for (PlaybackListener pl : playbackListeners.toArray(PlaybackListener[]::new))
            {
                // playbackSession might be null because in the meantime of the latency firing session was closed ?
                if (playbackSession == null || pl.isAccepted(playbackSession))
                {
                    pl.beatChanged(oldPos, newPos, newPosInBeats);
                }
            }
        });
    }

    private void fireSongPartChanged(SongPart newSpt)
    {
        if (currentSongPart != newSpt)
        {
            currentSongPart = newSpt;
            fireLatencyAwareEvent(() -> 
            {
                for (PlaybackListener pl : playbackListeners.toArray(PlaybackListener[]::new))
                {
                    // playbackSession might be null because in the meantime of the latency firing session was closed ?
                    if (playbackSession == null || pl.isAccepted(playbackSession))
                    {
                        pl.songPartChanged(newSpt);
                    }
                }
            });
        }
    }

    private void fireNoteOn(long tick, int channel, int pitch, int velocity)
    {
        fireLatencyAwareEvent(() -> 
        {
            for (NoteListener l : noteListeners)
            {
                l.noteOn(tick, channel, pitch, velocity);
            }
        });
    }

    private void fireNoteOff(long tick, int channel, int pitch)
    {
        fireLatencyAwareEvent(() -> 
        {
            for (NoteListener l : noteListeners)
            {
                l.noteOff(tick, channel, pitch);
            }
        });
    }


    private void fireMidiActivity(long tick, int channel)
    {
        fireLatencyAwareEvent(() -> 
        {
            for (PlaybackListener pl : playbackListeners.toArray(PlaybackListener[]::new))
            {
                // playbackSession might be null because in the meantime of the latency firing session was closed ?
                if (playbackSession == null || pl.isAccepted(playbackSession))
                {
                    pl.midiActivity(tick, channel);
                }
            }
        });
    }

    /**
     * Fire an event on the EDT after a time delay to take into account the current output synth latency.
     * <p>
     * Active timers are available in audioLatencyTimers.
     *
     * @param r
     */
    private void fireLatencyAwareEvent(Runnable r)
    {
        if (audioLatency == 0)
        {
            SwingUtilities.invokeLater(r);
        } else
        {
            Timer t = new Timer(audioLatency, evt -> 
            {
                r.run();            // Will be run on the EDT
            });
            t.addActionListener(evt -> 
            {
                synchronized (audioLatencyTimers)
                {
                    audioLatencyTimers.remove(t);
                }
            });
            synchronized (audioLatencyTimers)
            {
                audioLatencyTimers.add(t);
            }
            t.setRepeats(false);
            t.start();
        }
    }

    /**
     * When sequencer is stopped or paused, make sure there is no pending events.
     */
    private void clearPendingEvents()
    {
        for (Iterator<Timer> it = audioLatencyTimers.iterator(); it.hasNext();)
        {
            it.next().stop();
            it.remove();
        }
    }

    private SongContext getSongContext(PlaybackSession session)
    {
        SongContext sgContext = session instanceof SongContextProvider ? ((SongContextProvider) session).getSongContext() : null;
        return sgContext;
    }

    /**
     * Start the sequencer with the bug fix (tempo reset at 120 upon each start) + possibly fire a chord change event.
     * <p>
     * If sequencer is already playing or is disabled, do nothing.
     * <p>
     * If playbackSession is a SongContextProvider and a ControlTrackProvider, and if there is no chord symbol at current position, then fire a chord change
     * event using the previous chord symbol (ie the current chord symbol at this start position).
     */
    private void seqStart()
    {
        switch (state)
        {
            case DISABLED -> LOGGER.log(Level.WARNING, "seqStart() called with state={0}", state);

            case PLAYING ->
            {
            }

            case STOPPED, PAUSED ->
            {
                SongContext sgContext = getSongContext(playbackSession);
                if (sgContext != null && playbackSession instanceof ControlTrackProvider)
                {
                    ControlTrackProvider controlTrackProvider = (ControlTrackProvider) playbackSession;
                    ControlTrack controlTrack = controlTrackProvider.getControlTrack();    // Might be null if disabled for a BaseSongSession instance
                    if (controlTrack != null)
                    {
                        SongChordSequence cSeq = controlTrack.getContextChordGetSequence();
                        // Fire chord symbol change if no chord symbol at current position (current chord symbol is the previous one)
                        // Fire a song part change event
                        long relativeTick = getRelativeTickFromLoopStart(sequencer.getTickPosition());      // Can be negative in some cases
                        Position posStart = sgContext.toPosition(relativeTick);
                        if (posStart != null)
                        {
                            CLI_ChordSymbol lastCliCs = cSeq.getChordSymbol(posStart); // Process substitute chord symbols
                            if (lastCliCs != null)
                            {
                                // Fire the event
                                fireChordSymbolChanged(lastCliCs);
                            }

                            SongPart spt = sgContext.getSong().getSongStructure().getSongPart(posStart.getBar());
                            fireSongPartChanged(spt);
                        }
                    }
                }


                sequencer.start();
                // JDK -11 BUG: start() resets tempo at 120 !
                sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);
            }
            default -> throw new AssertionError(state.name());
        }
        // Nothing
    }

    private void updateCurrentPosition(int bar, float barBeat, float posInBeats)
    {
        assert !isDisabled();
        Position oldPos = new Position(currentBeatPosition);
        currentBeatPosition.setBar(bar);
        currentBeatPosition.setBeat(barBeat);


        // Fire events
        fireBeatChanged(oldPos, new Position(currentBeatPosition), posInBeats);


        // Possibly fire a songpart change as well
        SongContext sgContext = getSongContext(playbackSession);
        if (sgContext != null)
        {
            SongPart newSpt = sgContext.getSongParts().stream()
                    .filter(spt -> spt.getBarRange().contains(bar))
                    .findFirst().orElse(null);
            if (newSpt != null)
            {
                fireSongPartChanged(newSpt);
            }
        }
    }

    private void closeCurrentPlaybackSession()
    {
        if (playbackSession != null)
        {
            playbackSession.removePropertyChangeListener(this);
            playbackSession.close();
            playbackSession = null;
        }
    }

    private void songTempoChanged(float tempoInBPM)
    {
        songTempoFactor = tempoInBPM / MidiConst.SEQUENCER_REF_TEMPO;
        updateTempoFactor();
    }

    private void setState(State newState)
    {
        if (!newState.equals(getState()))
        {
            State oldState = getState();
            state = newState;
            pcs.firePropertyChange(PROP_STATE, oldState, newState);
        }
    }

    /**
     * Update the tempo factor of the sequencer.
     * <p>
     * Depends on songPartTempoFactor and songTempoFactor. Sequencer tempo must always be MidiConst.SEQUENCER_REF_TEMPO.
     * <p>
     */
    private void updateTempoFactor()
    {
        assert !isDisabled();

        // Recommended way instead of setTempoInBpm() which cause problems when playback is looped
        float f = songPartTempoFactor * songTempoFactor;
        sequencer.setTempoFactor(f);
    }

    private void checkMidi() throws MusicGenerationException
    {
        if (JJazzMidiSystem.getInstance().getDefaultOutDevice() == null)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_NoMidiOutputDeviceSet"));
        }
    }

    private void executeEndOfPlaybackAction()
    {
        if (playbackSession instanceof EndOfPlaybackActionProvider actionProvider)
        {
            var al = actionProvider.getEndOfPlaybackAction();
            if (al != null)
            {
                al.actionPerformed(null);
            }
        }
    }

    /**
     * Update the track status of all RhythmVoice tracks using the current session data.
     * <p>
     */
    private void updateTracksMuteStatus()
    {
        var mapTrackMute = playbackSession.getTracksMuteStatus();

        // Make sure sequence is not null: it can happen when setPlayingSession() was called with an UpdatableSongSession but play() 
        // was not called yet, and user changed the song that provoked an update        
        if (mapTrackMute != null && sequencer.getSequence() != null)
        {

            LOGGER.log(Level.FINE, "updateTracksMuteStatus() mapTrackMute={0}", mapTrackMute);
            for (int trackId : mapTrackMute.keySet())
            {
                boolean b = mapTrackMute.get(trackId);
                sequencer.setTrackMute(trackId, b);
                if (sequencer.getTrackMute(trackId) != b)
                {
                    LOGGER.log(Level.FINE, "updateTracksMuteStatus() setTrackMute({0},{1}) failed", new Object[]
                    {
                        trackId, b
                    });
                    LOGGER.log(Level.FINE, "                          sequencer{0}", sequencer.isRunning());
                }
            }
        }
    }

    /**
     * Convert an absolute sequence tick to a relative tick from loop start (whose tick position may be greater than 0 if precount bars are used).
     * <p>
     *
     * @param sequenceTick
     * @return
     */
    private long getRelativeTickFromLoopStart(long sequenceTick)
    {
        assert playbackSession != null;
        return sequenceTick - playbackSession.getLoopStartTick();
    }

    /**
     * Our Midi Receiver used to fire events to NoteListeners and PlaybackListener.midiActivity().
     * <p>
     * Events are fired taking into account the current output synth latency.
     * <p>
     */
    private class McReceiver implements Receiver
    {

        /**
         * Fire only one Activity event for this period of time, even if there are several notes.
         */
        public static final int ACTIVITY_MIN_PERIOD_MS = 100;

        // Store the last Note On millisecond position for each note. Use -1 if initialized.
        private final long lastNoteOnMs[] = new long[16];
        private boolean enabled;

        public McReceiver()
        {
            setEnabled(true);
        }

        /**
         * @return the enabled
         */
        public boolean isEnabled()
        {
            return enabled;
        }

        /**
         * Enable or disable this receiver.
         *
         *
         * @param enabled the enabled to set
         */
        public final void setEnabled(boolean enabled)
        {
            if (this.enabled != enabled)
            {
                this.enabled = enabled;
                reset();
            }
        }

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (!enabled)
            {
                return;
            }

            if (msg instanceof ShortMessage sm)
            {
                if (playbackListeners.isEmpty() && noteListeners.isEmpty())
                {
                    return;
                }


                if (sm.getCommand() == ShortMessage.NOTE_ON)
                {
                    int pitch = sm.getData1();
                    int velocity = sm.getData2();
                    if (velocity > 0)
                    {
                        noteOnReceived(sm.getChannel(), pitch, velocity);
                    } else
                    {
                        noteOffReceived(sm.getChannel(), pitch);
                    }


                } else if (sm.getCommand() == ShortMessage.NOTE_OFF)
                {
                    int pitch = sm.getData1();
                    noteOffReceived(sm.getChannel(), pitch);
                }
            }
        }

        @Override
        public void close()
        {
            // Do nothing
        }

        /**
         * Reset internal state.
         */
        private void reset()
        {
            for (int i = 0; i < 16; i++)
            {
                lastNoteOnMs[i] = -1;
            }
        }

        private void noteOnReceived(int channel, int pitch, int velocity)
        {
            if (enabled)
            {
                // Midi activity only once for a given channel in the ACTIVITY_MIN_PERIOD_MS period 
                long pos = System.currentTimeMillis();
                long lastPos = lastNoteOnMs[channel];
                if (lastPos < 0 || (pos - lastPos) > ACTIVITY_MIN_PERIOD_MS)
                {
                    fireMidiActivity(-1, channel);
                }
                lastNoteOnMs[channel] = pos;


                fireNoteOn(-1, channel, pitch, velocity);
            }
        }

        private void noteOffReceived(int channel, int pitch)
        {
            if (enabled)
            {
                lastNoteOnMs[channel] = -1;
                fireNoteOff(-1, channel, pitch);
            }
        }

    }

}
