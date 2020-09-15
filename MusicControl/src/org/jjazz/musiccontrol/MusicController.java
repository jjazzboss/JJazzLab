/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.musiccontrol;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.Note;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.UserChannelRvKey;
import static org.jjazz.musiccontrol.Bundle.*;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.MidiSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

/**
 * Control the music playback.
 * <p>
 * Property changes are fired for:<br>
 * - start/pause/stop/disabled state changes<br>
 * - pre-playing : vetoable change, ie listeners can fire a PropertyVetoException to prevent playback to start<br>
 * - click and loop ON/OFF changes<br>
 * <p>
 * Use PlaybackListener to get notified of other events (bar/beat changes etc.) during playback. Note that PlaybackListeners will
 * be notified out of the Swing EDT.
 * <p>
 */
@Messages(
        {
            "ERR_SequencerLimited=This sequencer implementation is limited, music playback may not work"
        })
public class MusicController implements PropertyChangeListener, MetaEventListener, ControllerEventListener
{

    public static final String PROP_PLAYBACK_KEY_TRANSPOSITION = "PlaybackTransposition";
    public static final String PROP_STATE = "PropPlaybackState";
    /**
     * This vetoable property is changed/fired just before playing song and can be vetoed by vetoables listeners to cancel
     * playback start.
     * <p>
     * NewValue=MusicGenerationContext object.
     */
    public static final String PROPVETO_PRE_PLAYBACK = "PropVetoPrePlayback";
    public static final String PROP_CLICK = "PropClick";
    public static final String PROP_LOOPCOUNT = "PropLoopCount";

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
    /**
     * The context for which we will play music.
     */
    private MusicGenerationContext mgContext;
    /**
     * The playback context for one version of a song.
     */
    private PlaybackContext playbackContext;
    /**
     * The optional current post processors.
     */
    private MusicGenerator.PostProcessor[] postProcessors;

    private State state;
    /**
     * The current beat position during playback.
     */
    Position currentBeatPosition = new Position();
    // private final Sequencer sequencer;
    private int loopCount;
    private boolean isClickEnabled;
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
    private float songPartTempoFactor = 1;

    /**
     * The list of the controller changes listened to
     */
    private static final int[] listenedControllers =
    {
        MidiConst.CTRL_CHG_JJAZZ_BEAT_CHANGE,
        MidiConst.CTRL_CHG_JJAZZ_ACTIVITY,
        MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR
    };
    /**
     * If true display built sequence when it is built
     */
    private boolean debugBuiltSequence = false;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final VetoableChangeSupport vcs = new VetoableChangeSupport(this);
    private final List<PlaybackListener> playbackListeners = new ArrayList<>();
    private static Preferences prefs = NbPreferences.forModule(MusicController.class);
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
        loopCount = 0;
        isClickEnabled = false;

        state = State.STOPPED;
        sequencer = JJazzMidiSystem.getInstance().getDefaultSequencer();
        initSequencer();
        sequencerLockHolder = null;

        // Listen to click settings changes
        ClickManager.getInstance().addPropertyChangeListener(this);

        // Listen to sequencer lock changes
        JJazzMidiSystem.getInstance().addPropertyChangeListener(this);
    }

    /**
     * Try to temporarily acquire the java sequencer opened and connected to the default Midi out device.
     * <p>
     * You can acquire the sequencer only if MusicController is not in the PLAYING state. If acquisition is successful the
     * MusicController is put in DISABLED state. When done with the sequencer, caller must call releaseSequencer(lockHolder).
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

        LOGGER.fine("acquireSequencer() -- lockHolder=" + lockHolder);

        if (sequencerLockHolder == lockHolder)
        {
            // lock already acquired
            return sequencer;
        } else if (sequencerLockHolder == null)
        {
            // lock acquired by external entity: get disabled
            sequencerLockHolder = lockHolder;

            // Remove the MusicController listeners
            releaseSequencer();

            State old = state;
            state = State.DISABLED;
            pcs.firePropertyChange(PROP_STATE, old, state);

            LOGGER.fine("acquireSequencer() external lock acquired.  MusicController released the sequencer, oldState=" + old + " newState=DISABLED");

            return sequencer;
        } else
        {
            LOGGER.fine("acquireSequencer() can't give lock to " + lockHolder + ", current lock=" + sequencerLockHolder);
            return null;
        }
    }

    /**
     * Release the external sequencer lock.
     * <p>
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

        LOGGER.fine("releaseSequencer() -- lockHolder=" + lockHolder);

        sequencerLockHolder = null;

        sequencer.stop(); // Just to make sure

        // Initialize sequencer for MusicController
        initSequencer();

        if (playbackContext != null)
        {
            playbackContext.setDirty();
        }

        // Change state
        State old = state;
        assert old.equals(State.DISABLED);
        state = State.STOPPED;
        pcs.firePropertyChange(PROP_STATE, old, state);
    }

    /**
     * Set the music context on which this controller's methods (play/pause/etc.) will operate, and build the sequence.
     * <p>
     * Stop the playback if it was on. Tempo is set to song's tempo.
     *
     * @param context Can be null.
     * @param postProcessors Optional PostProcessors to use when generating the backing track.
     * @throws org.jjazz.rhythm.api.MusicGenerationException E.g. if state is DISABLED, sequence could not be generated, etc.
     */
    public void setContext(MusicGenerationContext context, MusicGenerator.PostProcessor... postProcessors) throws MusicGenerationException
    {
        if (context != null && context.equals(mgContext))
        {
            return;
        }

        if (state.equals(State.DISABLED))
        {
            throw new MusicGenerationException("Playback is disabled.");
        }

        this.postProcessors = postProcessors;

        stop();
        songPartTempoFactor = 1f;

        if (mgContext != null)
        {
            mgContext.getMidiMix().removePropertyListener(this);
            mgContext.getSong().removePropertyChangeListener(this);
        }

        if (playbackContext != null)
        {
            playbackContext.close();
            playbackContext = null;
        }

        mgContext = context;
        if (mgContext != null)
        {
            mgContext.getMidiMix().addPropertyListener(this);
            mgContext.getSong().addPropertyChangeListener(this);
            songTempoChanged(mgContext.getSong().getTempo());
            try
            {
                playbackContext = new PlaybackContext(mgContext, this.postProcessors); // Exception possible when building the sequence
            } catch (MusicGenerationException ex)
            {
                // Roll back variables state
                mgContext.getMidiMix().removePropertyListener(this);
                mgContext.getSong().removePropertyChangeListener(this);
                mgContext = null;
                throw ex;
            }
        }
    }

    /**
     * Start the playback of a song using the current context.
     * <p>
     * Song is played from the beginning of the context range. Before playing the song, vetoable listeners are notified with a
     * PROPVETO_PRE_PLAYBACK property change.<p>
     *
     *
     * @param fromBarIndex Play the song from this bar. Bar must be within the context's range.
     *
     * @throws java.beans.PropertyVetoException If a vetoable listener vetoed the playback start. A listener who has already
     * notified user should throw an exception with a null message.
     * @throws MusicGenerationException If a problem occurred which prevents song playing: no Midi out, song is already playing,
     * rhythm music generation problem, MusicController is disabled, etc.
     * @throws IllegalStateException If context is null.
     *
     * @see #getPlayingSongCopy()
     */
    public void play(int fromBarIndex) throws MusicGenerationException, PropertyVetoException
    {
        if (mgContext == null)
        {
            throw new IllegalStateException("context=" + mgContext + ", fromBarIndex=" + fromBarIndex);
        }
        if (!mgContext.getBarRange().contains(fromBarIndex))
        {
            throw new IllegalArgumentException("context=" + mgContext + ", fromBarIndex=" + fromBarIndex);
        }


        checkMidi();                // throws MusicGenerationException


        // Check state
        if (state == State.PLAYING)
        {
            throw new MusicGenerationException("A song is already playing.");
        } else if (state == State.DISABLED)
        {
            throw new MusicGenerationException("Playback is disabled.");
        }


        // If we're here then playbackState = PAUSE or STOPPED
        if (mgContext.getBarRange().isEmpty())
        {
            // Throw an exception to let the UI roll back (eg play button)
            throw new MusicGenerationException("Nothing to play");
        }


        // Check that all listeners are OK to start playback
        vcs.fireVetoableChange(PROPVETO_PRE_PLAYBACK, null, mgContext);  // can raise PropertyVetoException


        // Regenerate the sequence and the related data if needed
        if (playbackContext.isDirty())
        {
            try
            {
                playbackContext.buildSequence();
            } catch (MusicGenerationException ex)
            {
                throw ex;
            }
        }


        // Set start position
        setPosition(fromBarIndex);


        // Start or restart the sequencer
        sequencer.setLoopCount(loopCount);
        seqStart();


        State old = this.getState();
        state = State.PLAYING;


        pcs.firePropertyChange(PROP_STATE, old, state);
    }

    /**
     * Resume playback from the pause state.
     * <p>
     * If played/paused song was modified, then resume() will just redirect to the play() method. If state is not PAUSED, nothing
     * is done.
     *
     * @throws org.jjazz.rhythm.api.MusicGenerationException For example if state==DISABLED
     * @throws java.beans.PropertyVetoException
     */
    public void resume() throws MusicGenerationException, PropertyVetoException
    {
        if (state == State.DISABLED)
        {
            throw new MusicGenerationException("Playback is disabled.");
        } else if (!state.equals(State.PAUSED))
        {
            return;
        }


        if (playbackContext.isDirty())
        {
            // Song was modified during playback, do play() instead
            play(mgContext.getBarRange().from);
            return;
        }


        if (mgContext == null)
        {
            throw new IllegalStateException("context=" + mgContext);
        }


        checkMidi();                // throws MusicGenerationException


        // Check that all listeners are OK to resume playback
        vcs.fireVetoableChange(PROPVETO_PRE_PLAYBACK, null, mgContext.getSong());  // can raise PropertyVetoException


        // Let's go again
        seqStart();


        // Update state
        State old = this.getState();
        state = State.PLAYING;
        pcs.firePropertyChange(PROP_STATE, old, state);

    }

    /**
     * Stop the playback of the sequence if it was playing, and reset the position to the beginning of the sequence.
     * <p>
     */
    public void stop()
    {
        switch (state)
        {
            case DISABLED:
            case STOPPED:
                return;
            case PAUSED:
                // Nothing
                break;
            case PLAYING:
                sequencer.stop();
                break;
            default:
                throw new AssertionError(state.name());
        }

        // Update state
        songPartTempoFactor = 1;

        State old = this.getState();
        state = State.STOPPED;
        pcs.firePropertyChange(PROP_STATE, old, state);

        // Position must be reset after the stop so that playback beat change tracking listeners are not reset upon stop
        int bar = mgContext != null ? mgContext.getBarRange().from : 0;
        setPosition(bar);

    }

    /**
     * Stop the playback of the sequence and leave the position unchanged.
     * <p>
     * If played song was modified after playback started, then pause() will just redirect to the stop() method. If state is not
     * PLAYING, nothing is done.
     */
    public void pause()
    {

        if (!state.equals(State.PLAYING))
        {
            return;
        }

        if (playbackContext.isDirty())
        {
            // Song was modified during playback, pause() not allowed, do stop() instead
            stop();
            return;
        }

        sequencer.stop();

        State old = getState();
        state = State.PAUSED;
        pcs.firePropertyChange(PROP_STATE, old, state);
    }

    /**
     * The current MusicGenerationContext.
     *
     * @return Can be null.
     */
    public MusicGenerationContext getContext()
    {
        return mgContext;
    }

    /**
     * The current PostProcessors.
     *
     * @return
     */
    public MusicGenerator.PostProcessor[] getPostProcessors()
    {
        return postProcessors;
    }

    public Position getBeatPosition()
    {
        return currentBeatPosition;
    }

    public State getState()
    {
        return state;
    }

    /**
     * Get the key transposition applied to chord symbols when playing a song.
     * <p>
     *
     * @return [0;-11] Default is 0.
     */
    public int getPlaybackKeyTransposition()
    {
        return prefs.getInt(PROP_PLAYBACK_KEY_TRANSPOSITION, 0);
    }

    /**
     * Set the key transposition applied to chord symbols when playing a song.
     * <p>
     * Ex: if transposition=-2, chord=C#7 will be replaced by B7.
     *
     * @param t [0;-11]
     */
    public void setPlaybackKeyTransposition(int t)
    {
        if (t < -11 || t > 0)
        {
            throw new IllegalArgumentException("t=" + t);
        }

        int old = getPlaybackKeyTransposition();
        if (old != t)
        {
            if (playbackContext != null)
            {
                playbackContext.setDirty();
            }
            prefs.putInt(PROP_PLAYBACK_KEY_TRANSPOSITION, t);
            pcs.firePropertyChange(PROP_PLAYBACK_KEY_TRANSPOSITION, old, t);
        }
    }

    /**
     * Enable the click or not.
     * <p>
     * Do nothing if state == DISABLED.
     *
     * @param b
     */
    public void setClickEnabled(boolean b)
    {
        if (isClickEnabled == b || state.equals(State.DISABLED))
        {
            return;
        }

        isClickEnabled = b;

        if (state.equals(State.PLAYING))
        {
            assert sequencer.isRunning();
            sequencer.setTrackMute(playbackContext.clickTrackId, !isClickEnabled);
        }

        pcs.firePropertyChange(PROP_CLICK, !b, b);
    }

    public boolean isClickEnabled()
    {
        return isClickEnabled;
    }

    public int getLoopCount()
    {
        return loopCount;
    }

    /**
     * Set the loop count of the playback.
     * <p>
     * Do nothing if state == DISABLED.
     *
     * @param loopCount If 0, play the song once (no loop). Use Sequencer.LOOP_CONTINUOUSLY for endless loop.
     */
    public void setLoopCount(int loopCount)
    {
        if (loopCount != Sequencer.LOOP_CONTINUOUSLY && loopCount < 0)
        {
            throw new IllegalArgumentException("loopCount=" + loopCount);
        }

        if (state.equals(State.DISABLED))
        {
            return;
        }

        int old = this.loopCount;
        this.loopCount = loopCount;

        sequencer.setLoopCount(loopCount);

        pcs.firePropertyChange(PROP_LOOPCOUNT, old, this.loopCount);
    }

    public void setDebugBuiltSequence(boolean b)
    {
        debugBuiltSequence = b;
    }

    public boolean isDebugBuiltSequence()
    {
        return debugBuiltSequence;
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

    /**
     * Listeners will be notified via the PROPVETO_PRE_PLAYBACK property change before a playback is started.
     * <p>
     * The NewValue is a MusicGenerationContext object. Listener is responsible for informing the user if the change was vetoed.
     *
     * @param listener
     */
    public synchronized void addVetoableChangeListener(VetoableChangeListener listener)
    {
        vcs.addVetoableChangeListener(listener);
    }

    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener)
    {
        vcs.removeVetoableChangeListener(listener);
    }

    //-----------------------------------------------------------------------
    // Implementation of the ControlEventListener interface
    //-----------------------------------------------------------------------
    /**
     * Handle the listened controllers notifications.
     * <p>
     * CAUTIOUS : the global listenedControllers array must be consistent with this method !
     *
     * @param event
     */
    @Override
    public void controlChange(ShortMessage event)
    {
        long tick = sequencer.getTickPosition() - playbackContext.songTickStart;
        int data1 = event.getData1();
        switch (data1)
        {
            case MidiConst.CTRL_CHG_JJAZZ_MARKER_SYNC:
                // Not used for now
                break;
            case MidiConst.CTRL_CHG_JJAZZ_CHORD_CHANGE:
                // Not used for now
                break;
            case MidiConst.CTRL_CHG_JJAZZ_ACTIVITY:
                fireMidiActivity(event.getChannel(), tick);
                break;
            case MidiConst.CTRL_CHG_JJAZZ_BEAT_CHANGE:
                int index = (int) (tick / MidiConst.PPQ_RESOLUTION);
                long remainder = tick % MidiConst.PPQ_RESOLUTION;
                index += (remainder <= MidiConst.PPQ_RESOLUTION / 2) ? 0 : 1;
                if (index >= playbackContext.naturalBeatPositions.size())
                {
                    index = playbackContext.naturalBeatPositions.size() - 1;
                }
                Position newPos = playbackContext.naturalBeatPositions.get(index);
                setCurrentBeatPosition(newPos.getBar(), newPos.getBeat());
                break;
            case MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR:
                songPartTempoFactor = MidiUtilities.getTempoFactor(event);
                updateTempoFactor();
                break;
            default:
                LOGGER.log(Level.WARNING, "controlChange() controller event not managed data1={0}", data1);
                break;
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the MetaEventListener interface
    //-----------------------------------------------------------------------
    @Override
    public void meta(MetaMessage meta)
    {
        if (meta.getType() == 47) // Meta Event for end of sequence
        {
            // This method  is called from the Sequencer thread, NOT from the EDT !
            // So if this method impacts the UI, it must use SwingUtilities.InvokeLater() (or InvokeAndWait())
            LOGGER.fine("Sequence end reached");
            Runnable doRun = new Runnable()
            {
                @Override
                public void run()
                {
                    stop();
                }
            };
            SwingUtilities.invokeLater(doRun);
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

        // Below property changes are meaningless if no context or if state is DISABLED
        if (mgContext == null || state.equals(State.DISABLED))
        {
            return;
        }

        if (e.getSource() == mgContext.getSong())
        {
            if (e.getPropertyName() == Song.PROP_MODIFIED_OR_SAVED)
            {
                if ((Boolean) e.getNewValue() == true)
                {
                    playbackContext.setDirty();
                }
            } else if (e.getPropertyName() == Song.PROP_TEMPO)
            {
                songTempoChanged((Integer) e.getNewValue());
            } else if (e.getPropertyName() == Song.PROP_CLOSED)
            {
                stop();
            }
        } else if (e.getSource() == mgContext.getMidiMix() && null != e.getPropertyName())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_INSTRUMENT_MUTE:
                    InstrumentMix insMix = (InstrumentMix) e.getOldValue();
                    updateTrackMuteState(insMix, playbackContext.mapRvTrackId);
                    break;
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                    // This can impact the sequence, make sure it is rebuilt
                    playbackContext.setDirty();
                    break;
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                    playbackContext.mapRvTrackId.clear();       // Mapping between RhythmVoice and Sequence tracks is no longer valid
                    playbackContext.setDirty();                 // Make sure sequence is rebuilt
                    break;
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                    // KeyMap has changed, need to regenerate the sequence
                    playbackContext.setDirty();
                    break;
                case MidiMix.PROP_USER_CHANNEL_RECORDING_ENABLED:
                    updateRecordingState();
                    break;
                default:
                    // eg MidiMix.PROP_USER_CHANNEL: do nothing
                    break;
            }
        } else if (e.getSource() == ClickManager.getInstance())
        {
            // Make sure click track is recalculated if click features have changed           
            playbackContext.setDirty();
        }

        if (playbackContext.isDirty() && state == State.PAUSED)
        {
            stop();
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
        int[] res = sequencer.addControllerEventListener(this, listenedControllers);
        if (res.length != listenedControllers.length)
        {
            LOGGER.severe(ERR_SequencerLimited());
        }
        sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);
    }

    /**
     * Remove our listeners.
     */
    private void releaseSequencer()
    {
        sequencer.removeMetaEventListener(this);
        sequencer.removeControllerEventListener(this, listenedControllers);
    }

    /**
     *
     * Set the sequencer and model position to the specified bar.
     * <p>
     * Take into account the possible precount bars.
     * <p>
     * @param fromBar Must be &gt;=0
     */
    private void setPosition(int fromBar)
    {
        assert !state.equals(State.DISABLED);

        long tick = 0;       // Default when fromBar==0 and click precount is true
        if (mgContext != null)
        {
            int relativeBar = fromBar - mgContext.getBarRange().from;
            if (relativeBar > 0 || !ClickManager.getInstance().isClickPrecountEnabled())
            {
                // No precount
                tick = playbackContext.songTickStart + mgContext.getRelativeTick(new Position(relativeBar, 0));
            }
        }

        sequencer.setTickPosition(tick);

        setCurrentBeatPosition(fromBar, 0);
    }

    /**
     * Update the muted tracks
     * <p>
     *
     * @param insMix
     * @param mapRvTrack The map which provides the track index corresponding to each rhythmvoice.
     */
    private void updateTrackMuteState(InstrumentMix insMix, HashMap<RhythmVoice, Integer> mapRvTrack)
    {
        assert !state.equals(State.DISABLED);

        boolean b = insMix.isMute();
        RhythmVoice rv = mgContext.getMidiMix().geRhythmVoice(insMix);
        if (rv instanceof UserChannelRvKey)
        {
            return;
        }
        Integer trackIndex = mapRvTrack.get(rv);
        if (trackIndex != null)
        {
            sequencer.setTrackMute(trackIndex, b);
            if (sequencer.getTrackMute(trackIndex) != b)
            {
                LOGGER.log(Level.SEVERE, "updateTrackMuteState() can''t mute on/off track number: {0} mute={1} insMix={2}", new Object[]
                {
                    trackIndex, b, insMix
                });
            }
        } else
        {
            // Might be null if mapRvTrack was reset because of a MidiMix change and sequence has not been rebuilt yet.
            // Also if multi-song and play with a context on only 1 rhythm.
        }
    }

    private void fireBeatChanged(Position oldPos, Position newPos)
    {
        for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
        {
            pl.beatChanged(oldPos, newPos);
        }
    }

    private void fireBarChanged(int oldBar, int newBar)
    {
        for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
        {
            pl.barChanged(oldBar, newBar);
        }
    }

    private void fireMidiActivity(int channel, long tick)
    {
        for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
        {
            pl.midiActivity(channel, tick);
        }
    }

    /**
     * Start the sequencer for playback, or playback+recording.
     * <p>
     * Apply workaround to JDK setTempo bug (tempo reset at 120 upon each start).
     */
    private void seqStart()
    {
        assert !state.equals(State.DISABLED);
       
        sequencer.start();

        // JDK -11 BUG: start() resets tempo at 120 !
        sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);

        // Enable/disable recording on specific channels
        updateRecordingState();
    }

    private void updateRecordingState()
    {

        MidiMix mm = mgContext.getMidiMix();
        int channel = mm.getUserChannel();
        if (channel == -1)
        {
            // No user channel
            return;
        }
        assert playbackContext.userTrack != null;
        

        // There is a user channel, enable/disable recording
        boolean record = mm.isUserChannelRecordingEnabled();
        if (record)
        {
            sequencer.recordEnable(playbackContext.userTrack, channel);
        } else
        {
            sequencer.recordDisable(playbackContext.userTrack);
        }


        // Start recording only if it's already running
//        if (sequencer.isRunning() && !sequencer.isRecording() && record)
//        {
            sequencer.startRecording();
//        }

    }

    private void setCurrentBeatPosition(int bar, float beat)
    {
        assert !state.equals(State.DISABLED);

        Position oldPos = new Position(currentBeatPosition);
        currentBeatPosition.setBar(bar);
        currentBeatPosition.setBeat(beat);
        fireBeatChanged(oldPos, new Position(currentBeatPosition));
        if (beat == 0)
        {
            fireBarChanged(oldPos.getBar(), bar);
        }
    }

    private void songTempoChanged(float tempoInBPM)
    {
        songTempoFactor = tempoInBPM / MidiConst.SEQUENCER_REF_TEMPO;
        updateTempoFactor();
    }

    /**
     * Update the tempo factor of the sequencer.
     * <p>
     * Depends on songPartTempoFactor and songTempoFactor. Sequencer tempo must always be MidiConst.SEQUENCER_REF_TEMPO.
     *
     * @param bpm
     */
    private void updateTempoFactor()
    {
        assert !state.equals(State.DISABLED);

        // Recommended way instead of setTempoInBpm() which cause problems when playback is looped
        float f = songPartTempoFactor * songTempoFactor;
        sequencer.setTempoFactor(f);
    }

    private void checkMidi() throws MusicGenerationException
    {
        if (JJazzMidiSystem.getInstance().getDefaultOutDevice() == null)
        {
            throw new MusicGenerationException("No MIDI output device set. Please select a Midi device in the Midi panel of the Options/Preferences");
        }
    }

    // =============================================================================================
    // Private classes
    // =============================================================================================
    /**
     * The playback data associated to a snapshot of a song and other parameters (e.g. click, playback key transposition, etc.)
     */
    private class PlaybackContext
    {

        MusicGenerationContext originalContext;

        /**
         * The generated sequence.
         */
        Sequence sequence;

        /**
         * The user channel track (null if no user channel).
         */
        Track userTrack;
        /**
         * The position of each natural beat.
         */
        List<Position> naturalBeatPositions;
        int clickTrackId;
        int precountTrackId;
        long songTickStart;
        long songTickEnd;
        int controlTrackId;
        private boolean dirty;
        MusicGenerator.PostProcessor[] postProcessors;

        /**
         * The sequence track id (index) for each rhythm voice, for the given context.
         * <p>
         * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track
         * id.
         */
        private HashMap<RhythmVoice, Integer> mapRvTrackId;

        /**
         * Create a "dirty" object (needs to be updated) and build the sequence.
         *
         * @param context
         * @param MusicGenerator.PostProcessor[] Optional postprocessors
         */
        private PlaybackContext(MusicGenerationContext context, MusicGenerator.PostProcessor... postProcessors) throws MusicGenerationException
        {
            if (context == null)
            {
                throw new NullPointerException("context");
            }
            this.originalContext = context;
            dirty = true;
            this.postProcessors = postProcessors;

            buildSequence();
        }

        /**
         * Prepare the sequencer to play the specified song.
         * <p>
         * Create the sequence and load it in the sequencer. Store all the other related sequence-dependent data in this object.
         * Object is now "clean".
         *
         * @param song
         * @throws MusicGenerationException If problem occurs when creating the sequence.
         */
        private void buildSequence() throws MusicGenerationException
        {

            // Prepare our work MusicGenerationContext
            MusicGenerationContext workMgContext = buildWorkMgContext(originalContext);


            try
            {

                // Build the sequence
                MidiSequenceBuilder seqBuilder = new MidiSequenceBuilder(workMgContext, postProcessors);
                sequence = seqBuilder.buildSequence(false);                  // Can raise MusicGenerationException
                if (sequence == null)
                {
                    // If unexpected error, assertion error etc.
                    throw new MusicGenerationException("Unexpected error while building sequence. Consult log for details.");
                }


                mapRvTrackId = seqBuilder.getRvTrackIdMap();                 // Used to identify a RhythmVoice's track


                // Add a user channel track if available
                MidiMix mm = originalContext.getMidiMix();
                int userChannel = mm.getUserChannel();
                if (userChannel != -1)
                {
                    RhythmVoice rv = mm.getRhythmVoice(userChannel);
                    userTrack = sequence.createTrack();
                    int trackId = Arrays.asList(sequence.getTracks()).indexOf(userTrack);
                    mapRvTrackId.put(rv, trackId);
                }


                // Add the control track
                ControlTrackBuilder ctm = new ControlTrackBuilder(workMgContext);
                controlTrackId = ctm.addControlTrack(sequence);
                naturalBeatPositions = ctm.getNaturalBeatPositions();


                // Add the click track
                clickTrackId = prepareClickTrack(sequence, workMgContext);


                // Add the click precount track - this must be done last because it will shift all song events
                songTickStart = preparePrecountClickTrack(sequence, workMgContext);
                precountTrackId = sequence.getTracks().length - 1;


                // Update the sequence if rerouting needed
                rerouteDrumsChannels(sequence, workMgContext.getMidiMix());


                if (debugBuiltSequence)
                {
                    LOGGER.info("buildSequence() song=" + workMgContext.getSong().getName() + " sequence :");
                    LOGGER.info(MidiUtilities.toString(sequence));
                }


                // Initialize sequencer with the built sequence
                sequencer.setSequence(sequence);    // Can raise InvalidMidiDataException                                               


                // Update muted state for each track
                updateAllTracksMuteState(workMgContext.getMidiMix());
                sequencer.setTrackMute(controlTrackId, false);
                sequencer.setTrackMute(precountTrackId, false);
                sequencer.setTrackMute(clickTrackId, !isClickEnabled);


                // Set position and loop points
                sequencer.setLoopStartPoint(songTickStart);
                songTickEnd = (long) (songTickStart + workMgContext.getBeatRange().size() * MidiConst.PPQ_RESOLUTION);
                sequencer.setLoopEndPoint(songTickEnd);


                // We're clean
                dirty = false;

            } catch (MusicGenerationException | InvalidMidiDataException ex)
            {
                throw new MusicGenerationException(ex.getLocalizedMessage());
            }
        }

        private void updateAllTracksMuteState(MidiMix mm)
        {
            for (RhythmVoice rv : mm.getRhythmVoices())
            {
                InstrumentMix insMix = mm.getInstrumentMixFromKey(rv);
                Integer trackId = mapRvTrackId.get(rv);
                if (trackId != null)
                {
                    sequencer.setTrackMute(trackId, insMix.isMute());
                } else
                {
                    // It can be null, e.g. if multi-rhythm song and context is only on one of the rhythms.
                }
            }
        }

        /**
         * Prepare a music generation context possibly modified from the original.
         * <p>
         * Apply the playback key transposition.
         *
         * @param context
         * @return
         */
        private MusicGenerationContext buildWorkMgContext(MusicGenerationContext context)
        {
            if (getPlaybackKeyTransposition() == 0)
            {
                return context;
            }

            // Create a new song with transposed chord symbols
            var sf = SongFactory.getInstance();
            CLI_Factory clif = CLI_Factory.getDefault();
            int kt = MusicController.this.getPlaybackKeyTransposition();


            Song songCopy = sf.getCopy(context.getSong());
            sf.unregisterSong(songCopy);
            ChordLeadSheet clsCopy = songCopy.getChordLeadSheet();


            for (var oldCli : clsCopy.getItems(CLI_ChordSymbol.class))
            {
                var newEcs = oldCli.getData().getTransposedChordSymbol(kt, Note.Alteration.FLAT);
                var newCli = clif.createChordSymbol(clsCopy, newEcs, oldCli.getPosition());
                clsCopy.removeItem(oldCli);
                clsCopy.addItem(newCli);
            }

            var res = new MusicGenerationContext(songCopy, context.getMidiMix(), context.getBarRange());
            return res;

        }

        /**
         *
         * @param sequence
         * @param mm
         * @param sg
         * @return The track id
         */
        private int prepareClickTrack(Sequence sequence, MusicGenerationContext context)
        {
            // Add the click track
            ClickManager cm = ClickManager.getInstance();
            int trackId = cm.addClickTrack(sequence, context);

            // Send a Drums program change if Click channel is not used in the current MidiMix
            int clickChannel = ClickManager.getInstance().getPreferredClickChannel();
            if (context.getMidiMix().getInstrumentMixFromChannel(clickChannel) == null)
            {
//                Instrument ins = DefaultInstruments.getInstance().getInstrument(RvType.Drums);
//                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
//                jms.sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(clickChannel));  // Might not send anything if default instrument is Void Instrument
            }
            return trackId;
        }

        /**
         *
         * @param sequence
         * @param mm
         * @param sg
         * @return The tick position of the start of the song.
         */
        private long preparePrecountClickTrack(Sequence sequence, MusicGenerationContext context)
        {
            // Add the click track
            ClickManager cm = ClickManager.getInstance();
            long tickPos = cm.addPreCountClickTrack(sequence, context);

            // Send a Drums program change if Click channel is not used in the current MidiMix
            int clickChannel = ClickManager.getInstance().getPreferredClickChannel();
            if (context.getMidiMix().getInstrumentMixFromChannel(clickChannel) == null)
            {
//                Instrument ins = DefaultInstruments.getInstance().getInstrument(RvType.Drums);
//                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
//                jms.sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(clickChannel));  // Might not send anything if default instrument is Void Instrument                            
            }
            return tickPos;
        }

        private void rerouteDrumsChannels(Sequence seq, MidiMix mm)
        {
            List<Integer> toBeRerouted = mm.getDrumsReroutedChannels();
            MidiUtilities.rerouteShortMessages(seq, toBeRerouted, MidiConst.CHANNEL_DRUMS);
        }

        /**
         * Make sure resources are released.
         */
        private void close()
        {
            if (sequence == null)
            {
                return;
            }
            Track[] tracks = sequence.getTracks();
            if (tracks != null)
            {
                for (Track track : tracks)
                {
                    sequence.deleteTrack(track);
                }
            }
        }

        /**
         * Something has been modified, should rebuild sequence
         *
         * @return
         */
        boolean isDirty()
        {
            return dirty;
        }

        void setDirty()
        {
            dirty = true;
        }
    }

}
