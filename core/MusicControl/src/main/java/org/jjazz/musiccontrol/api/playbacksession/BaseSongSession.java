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
package org.jjazz.musiccontrol.api.playbacksession;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.musiccontrol.spi.ActiveSongBackgroundMusicBuilder;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.ControlTrack;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import static org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.PROP_LOOP_COUNT;
import static org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.PROP_MUTED_TRACKS;
import static org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.PROP_STATE;
import static org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.PROP_TEMPO;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;

/**
 * A base implementation of a PlaybackSession to render a SongContext.
 * <p>
 * Music generation uses the SongSequenceBuilder (and optionnaly ActiveSongBackgroundMusicBuilder) then adds control/click/precount tracks, taking into account
 * drums rerouting.
 * <p>
 * Once generated the session listens to the following changes: <br>
 * - Song tempo, song closing<br>
 * - MidiMix channel mute changes<br>
 * - PlaybackSettings Click and Loop changes<p>
 * <p>
 * The session never makes the session dirty. Use the provided subclasses for more advanced behaviors, e.g. update the dirty state, etc.
 */
public class BaseSongSession implements PropertyChangeListener, PlaybackSession, ControlTrackProvider, SongContextProvider, EndOfPlaybackActionProvider
{

    public static final int PLAYBACK_SETTINGS_LOOP_COUNT = -1298;
    private State state = State.NEW;
    private boolean isDirty;
    private final boolean isUseActiveSongBackgroundMusicBuilder;
    private final SongContext songContext;
    private final SessionConfig sessionConfig;
    private Sequence sequence;
    private ControlTrack controlTrack;
    private int playbackClickTrackId = -1;
    private int precountClickTrackId = -1;
    private long loopStartTick = 0;
    private long loopEndTick = -1;
    private final Context context;
    private Map<RhythmVoice, Integer> mapRvTrackId;
    private Map<RhythmVoice, Phrase> mapRvPhrase;
    private Map<Integer, Boolean> mapTrackIdMuted;
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(BaseSongSession.class.getSimpleName());

    /**
     * Create a session with the specified parameters.
     * <p>
     * @param sgContext                           Can not be null
     * @param sConfig                             Can not be null
     * @param useActiveSongBackgroundMusicBuilder If true use ActiveSongBackgroundMusicBuilder when possible to speed up music generation
     * @param context                             Can not be null
     */
    public BaseSongSession(SongContext sgContext, SessionConfig sConfig, boolean useActiveSongBackgroundMusicBuilder, Context context)
    {
        Objects.requireNonNull(sgContext);
        Objects.requireNonNull(context);
        Objects.requireNonNull(sConfig);
        this.songContext = sgContext;
        this.sessionConfig = sConfig;
        this.context = context;
        this.isUseActiveSongBackgroundMusicBuilder = useActiveSongBackgroundMusicBuilder;
    }


    @Override
    public BaseSongSession getFreshCopy(SongContext sgContext)
    {
        var newContext = sgContext == null ? getSongContext().clone() : sgContext;
        BaseSongSession res = new BaseSongSession(newContext, sessionConfig, isUseActiveSongBackgroundMusicBuilder(), context);
        return res;
    }

    @Override
    public Context getContext()
    {
        return context;
    }


    public boolean isUseActiveSongBackgroundMusicBuilder()
    {
        return isUseActiveSongBackgroundMusicBuilder;
    }


    @Override
    public synchronized State getState()
    {
        return state;
    }


    /**
     * Generate the sequence for the SongContext.
     * <p>
     * When all parameters are enabled, sequence will contain: <br>
     * - track 0: song name, tempo and tempo factor changes (see {@link org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder})<br>
     * - track 1-N: the song tracks, one per RhythmVoice(see {@link org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder})<br>
     * - track N+1: control track with beat events + chord symbol markers<br>
     * - track N+2: click track<br>
     * - track N+3: precount-click track<p>
     * Manage the drums rerouting.
     *
     * @param silent
     * @throws MusicGenerationException
     * @throws IllegalStateException    If state is not NEW
     */
    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        if (state != State.NEW)
        {
            throw new IllegalStateException("state=" + state);
        }


        // Generate music
        var songSeq = generateSongSequence(songContext, silent, isUseActiveSongBackgroundMusicBuilder);


        // Retrieve the data
        sequence = songSeq.sequence;
        mapRvPhrase = songSeq.mapRvPhrase;
        mapRvTrackId = songSeq.mapRvTrackId;


        // Save the mute status of each RhythmVoice track
        mapTrackIdMuted = new HashMap<>();
        MidiMix mm = songContext.getMidiMix();
        for (RhythmVoice rv : mapRvTrackId.keySet())
        {
            mapTrackIdMuted.put(mapRvTrackId.get(rv), mm.getInstrumentMix(rv).isMute());
        }


        // Add the control track
        if (sessionConfig.includeControlTrack())
        {
            Track track = sequence.createTrack();
            controlTrack = new ControlTrack(songContext, Arrays.asList(sequence.getTracks()).indexOf(track));
            controlTrack.fillTrack(track);
            mapTrackIdMuted.put(controlTrack.getTrackId(), false);
        }


        // Add the playback click track
        if (sessionConfig.includeClickTrack())
        {
            playbackClickTrackId = preparePlaybackClickTrack(sequence, songContext);
            mapTrackIdMuted.put(playbackClickTrackId, !PlaybackSettings.getInstance().isPlaybackClickEnabled());
        }


        // Add the click precount track - this must be done last because it might shift all song events      
        if (sessionConfig.includePrecountTrack())
        {
            loopStartTick = PlaybackSettings.getInstance().addPrecountClickTrack(sequence, songContext);
            precountClickTrackId = sequence.getTracks().length - 1;
            mapTrackIdMuted.put(precountClickTrackId, false);
        }


        loopEndTick = loopStartTick + Math.round(songContext.getBeatRange().size() * MidiConst.PPQ_RESOLUTION);


        // Listen to changes that can be handled without going dirty
        this.songContext.getSong().addPropertyChangeListener(this); // tempo changes + closing
        this.songContext.getMidiMix().addPropertyChangeListener(this);      // muted changes
        PlaybackSettings.getInstance().addPropertyChangeListener(this); // click on-off changes


        // Change state
        setState(State.GENERATED);

    }

    /**
     * We don't listen to any change on the underlying context data: always return false.
     *
     * @return
     */
    @Override
    public synchronized boolean isDirty()
    {
        return isDirty;
    }

    @Override
    public int getTempo()
    {
        return songContext.getSong().getTempo();
    }

    @Override
    public Sequence getSequence()
    {
        return state == State.GENERATED ? sequence : null;
    }

    @Override
    public long getLoopStartTick()
    {
        return state == State.GENERATED ? loopStartTick : -1;
    }

    @Override
    public long getLoopEndTick()
    {
        return state == State.GENERATED ? loopEndTick : -1;
    }

    @Override
    public int getLoopCount()
    {
        return sessionConfig.loopCount() == PLAYBACK_SETTINGS_LOOP_COUNT ? PlaybackSettings.getInstance().getLoopCount() : sessionConfig.loopCount();
    }

    @Override
    public long getTick(int barIndex)
    {
        long tick = -1;
        if (state == State.GENERATED)
        {
            if (PlaybackSettings.getInstance().isClickPrecountEnabled() && barIndex == getBarRange().from)
            {
                // Precount is ON and pos is the first possible bar
                tick = 0;
            } else
            {
                // Precount if OFF or barIndex is not the first possible bar
                tick = songContext.toRelativeTick(new Position(barIndex));
                if (tick != -1)
                {
                    tick += loopStartTick;
                }
            }
        }
        return tick;
    }

    @Override
    public IntRange getBarRange()
    {
        return state == State.GENERATED ? songContext.getBarRange() : null;
    }

    /**
     * The mute status of all tracks.
     * <p>
     * This includes the click/precount/control tracks when enabled.
     *
     * @return
     */
    @Override
    public HashMap<Integer, Boolean> getTracksMuteStatus()
    {
        return state == State.GENERATED ? new HashMap<>(mapTrackIdMuted) : null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public void close()
    {
        setState(State.CLOSED);
        PlaybackSettings.getInstance().removePropertyChangeListener(this);
        songContext.getSong().removePropertyChangeListener(this);
        songContext.getMidiMix().removePropertyChangeListener(this);
    }

    /**
     * A map providing the track id (index) corresponding to each used RhythmVoice in the given context.
     * <p>
     * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track id.
     * <p>
     *
     * @return Null if no meaningful value can be returned.
     */
    public Map<RhythmVoice, Integer> getRvTrackIdMap()
    {
        return state == State.GENERATED ? new HashMap<>(mapRvTrackId) : null;
    }


    /**
     * A map giving the resulting Phrase for each RhythmVoice, in the current context.
     * <p>
     *
     * @return Null if no meaningful value can be returned.
     */
    public Map<RhythmVoice, Phrase> getRvPhraseMap()
    {
        return state == State.GENERATED ? new HashMap<>(mapRvPhrase) : null;
    }


    /**
     * Get the click sequence track number.
     *
     * @return -1 if no click track
     */
    public int getClickTrackId()
    {
        return playbackClickTrackId;
    }

    /**
     * Get the precount sequence track number.
     *
     * @return -1 if no precount track
     */
    public int getPrecountTrackId()
    {
        return precountClickTrackId;
    }

    public SessionConfig getSessionConfig()
    {
        return sessionConfig;
    }

    // ==========================================================================================================
    // SongContextProvider implementation
    // ==========================================================================================================    
    @Override
    public SongContext getSongContext()
    {
        return songContext;
    }

    // ==========================================================================================================
    // ControlTrackProvider implementation
    // ==========================================================================================================    
    @Override
    public ControlTrack getControlTrack()
    {
        return state == State.GENERATED ? controlTrack : null;
    }


    // ==========================================================================================================
    // EndOfPlaybackActionProvider implementation
    // ==========================================================================================================   
    @Override
    public ActionListener getEndOfPlaybackAction()
    {
        return sessionConfig.endOfPlaybackAction();
    }


    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (state == State.CLOSED)
        {
            return;
        }
        // If here state=GENERATED

        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);

        if (e.getSource() == songContext.getSong())
        {
            if (e.getPropertyName().equals(Song.PROP_TEMPO))
            {
                pcs.firePropertyChange(PROP_TEMPO, (Integer) e.getOldValue(), (Integer) e.getNewValue());

            } else if (e.getPropertyName().equals(Song.PROP_CLOSED))
            {
                close();
            }
        } else if (e.getSource() == songContext.getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_INSTRUMENT_MUTE ->
                {
                    InstrumentMix insMix = (InstrumentMix) e.getOldValue();
                    MidiMix mm = songContext.getMidiMix();
                    RhythmVoice rv = mm.geRhythmVoice(insMix);
                    Integer trackId = mapRvTrackId.get(rv);     // Can be null 
                    if (trackId != null)
                    {
                        mapTrackIdMuted.put(trackId, insMix.isMute());
                        pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                    }
                }
                default ->
                {
                }
            }
            // E.g MidiMix.PROP_USER_CHANNEL: do nothing

        } else if (e.getSource() == PlaybackSettings.getInstance())
        {
            switch (e.getPropertyName())
            {
                case PlaybackSettings.PROP_PLAYBACK_CLICK_ENABLED ->
                {
                    mapTrackIdMuted.put(playbackClickTrackId, !PlaybackSettings.getInstance().isPlaybackClickEnabled());
                    pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                }
                case PlaybackSettings.PROP_LOOPCOUNT ->
                {
                    if (sessionConfig.loopCount() == PLAYBACK_SETTINGS_LOOP_COUNT)
                    {
                        pcs.firePropertyChange(PROP_LOOP_COUNT, (Integer) e.getOldValue(), (Integer) e.getNewValue());
                    }
                }
                default -> // E.g. PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION
                {
                }
            }
            // E.g. PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION
            // Nothing
        }
    }

    @Override
    public String toString()
    {
        return "BaseSongSession=[state=" + state + ", isDirty=" + isDirty + " context=" + context + " songContext=" + songContext + "]";
    }

    // ==========================================================================================================
    // Protected methods
    // ==========================================================================================================
    protected synchronized void setState(State newState)
    {
        State oldState = state;
        state = newState;
        pcs.firePropertyChange(PROP_STATE, oldState, newState);
    }

    /**
     * Set session as dirty (session is not up-to-date with it underlying data) and fire a change event.
     * <p>
     * For use by subclasses.
     */
    protected synchronized void setDirty()
    {
        if (!isDirty)
        {
            LOGGER.fine("setDirty() --");
            isDirty = true;
            pcs.firePropertyChange(PROP_DIRTY, false, true);
        }
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }


    /**
     * Add a click track to the sequence based on the specified context.
     *
     * @param sequence
     * @param context
     * @return
     */
    protected int preparePlaybackClickTrack(Sequence sequence, SongContext context)
    {
        PlaybackSettings cm = PlaybackSettings.getInstance();
        int trackId = cm.addClickTrack(sequence, context);
        // Send a Drums program change if Click channel is not used in the current MidiMix
//        int clickChannel = PlaybackSettings.getInstance().getPreferredClickChannel();
//        if (context.getMidiMix().getInstrumentMix(clickChannel) == null)
//        {
        //                Instrument ins = DefaultInstruments.getInstance().getInstrument(RvType.Drums);
        //                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        //                jms.sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(clickChannel));  // Might not send anything if default instrument is Void Instrument
//        }
        return trackId;
    }


    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    /**
     * @param sgContext
     * @param silent
     * @param useBackgroundMusicBuilder
     * @return
     * @throws MusicGenerationException
     */
    private SongSequenceBuilder.SongSequence generateSongSequence(SongContext sgContext, boolean silent, boolean useBackgroundMusicBuilder) throws MusicGenerationException
    {
        SongSequenceBuilder.SongSequence res = null;

        SongSequenceBuilder seqBuilder = new SongSequenceBuilder(sgContext); // Will work on a deep copy of sgContext

        // Reuse ActiveSongBackgroundMusicBuilder result when possible (map Rv=>Phrase)
        var asbmb = ActiveSongBackgroundMusicBuilder.getDefault();
        if (asbmb != null && useBackgroundMusicBuilder)
        {
            var lastResult = asbmb.getLastResult();
            if (lastResult != null
                    && asbmb.isLastResultUpToDate()
                    && lastResult.throwable() == null
                    && sgContext.equals(lastResult.songContext()))
            {
                // Build sequence directly from phrases
                var mapRvPhrases = lastResult.mapRvPhrases();
                res = seqBuilder.buildSongSequence(mapRvPhrases);   // Can raise MusicGenerationException
            }
        }

        if (res == null)
        {
            // Build from scratch
            res = seqBuilder.buildAll(silent); // Can raise MusicGenerationException
        }

        // Robustness, if unexpected error, assertion error etc.
        if (res == null)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_BuildSeqError"));
        }

        return res;
    }
}
