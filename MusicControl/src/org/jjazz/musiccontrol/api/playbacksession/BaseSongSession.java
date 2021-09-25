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
package org.jjazz.musiccontrol.api.playbacksession;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.Note;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
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
import org.jjazz.song.api.SongFactory;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;

/**
 * A base implementation of a PlaybackSession to render a SongContext.
 * <p>
 * It relies on SongSequenceBuilder and then add control/click/precount tracks, taking into account drums rerouting.
 * <p>
 * Once generated the session listens to: <br>
 * - Song tempo changes, closing<br>
 * - MidiMix channel mute changes<br>
 * - PlaybackSettings Click and Loop changes<p>
 * Use the provided subclasses for more advanced behaviors, e.g. update the dirty state, etc.
 */
public class BaseSongSession implements PropertyChangeListener, PlaybackSession, ControlTrackProvider, SongContextProvider, EndOfPlaybackActionProvider
{
    
    
    public static final int PLAYBACK_SETTINGS_LOOP_COUNT = -1298;
    private State state = State.NEW;
    private boolean isDirty;
    private SongContext songContext;
    private Sequence sequence;
    private ControlTrack controlTrack;
    private int playbackClickTrackId = -1;
    private int precountClickTrackId = -1;
    private long loopStartTick = 0;
    private long loopEndTick = -1;
    protected int loopCount = PLAYBACK_SETTINGS_LOOP_COUNT;         // Need to be accessible from subclass, because of getLoopCount() implementation
    private boolean isPlaybackTranspositionEnabled = true;
    private boolean isClickTrackIncluded = true;
    private boolean isPrecountTrackIncluded = true;
    private boolean isControlTrackIncluded = true;
    private ActionListener endOfPlaybackAction;
    private Map<RhythmVoice, Integer> mapRvTrackId;
    private Map<RhythmVoice, Phrase> mapRvPhrase;
    private Map<Integer, Boolean> mapTrackIdMuted;
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(BaseSongSession.class.getSimpleName());  //NOI18N

    /**
     * Create a session with the specified parameters.
     * <p>
     *
     * @param sgContext
     * @param enablePlaybackTransposition
     * @param enableClickTrack
     * @param enablePrecountTrack
     * @param enableControlTrack
     * @param loopCount
     * @param endOfPlaybackAction
     */
    public BaseSongSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecountTrack, boolean enableControlTrack,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        this.songContext = sgContext;
        this.isPlaybackTranspositionEnabled = enablePlaybackTransposition;
        this.isClickTrackIncluded = enableClickTrack;
        this.isPrecountTrackIncluded = enablePrecountTrack;
        this.isControlTrackIncluded = enableControlTrack;
        this.loopCount = loopCount;
        this.endOfPlaybackAction = endOfPlaybackAction;
        
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
     * - track 0: song name, tempo and tempo factor changes (see
     * {@link org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder})<br>
     * - track 1-N: the song tracks, one per RhythmVoice(see {@link org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder})<br>
     * - track N+1: control track with beat events + chord symbol markers<br>
     * - track N+2: click track<br>
     * - track N+3: precount-click track<p>
     * Manage the drums rerouting.
     *
     * @param silent
     * @throws MusicGenerationException
     */
    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        if (!state.equals(State.NEW))
        {
            throw new IllegalStateException("state=" + state);
        }
        
        
        SongContext workContext = songContext;
        int t = PlaybackSettings.getInstance().getPlaybackKeyTransposition();
        if (isPlaybackTranspositionEnabled() && t != 0)
        {
            workContext = getContextCopy(songContext, t);
        }


        // Build the sequence
        SongSequenceBuilder seqBuilder = new SongSequenceBuilder(workContext);
        SongSequenceBuilder.SongSequence songSeq = seqBuilder.buildAll(silent); // Can raise MusicGenerationException
        if (songSeq == null)
        {
            // If unexpected error, assertion error etc.
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_BuildSeqError"));
        }

        // Retrieve the data
        sequence = songSeq.sequence;
        mapRvPhrase = songSeq.mapRvPhrase;
        mapRvTrackId = songSeq.mapRvTrackId;


        // Save the mute status of each RhythmVoice track
        mapTrackIdMuted = new HashMap<>();
        MidiMix mm = songContext.getMidiMix();
        for (RhythmVoice rv : mapRvTrackId.keySet())
        {
            mapTrackIdMuted.put(mapRvTrackId.get(rv), mm.getInstrumentMixFromKey(rv).isMute());
        }


        // Add the control track
        if (isControlTrackIncluded())
        {
            Track track = sequence.createTrack();
            controlTrack = new ControlTrack(workContext, Arrays.asList(sequence.getTracks()).indexOf(track));
            controlTrack.fillTrack(track);
            mapTrackIdMuted.put(controlTrack.getTrackId(), false);
        }


        // Add the playback click track
        if (isClickTrackIncluded())
        {
            playbackClickTrackId = preparePlaybackClickTrack(sequence, workContext);
            mapTrackIdMuted.put(playbackClickTrackId, !PlaybackSettings.getInstance().isPlaybackClickEnabled());
        }


        // Add the click precount track - this must be done last because it might shift all song events      
        if (isPrecountTrackIncluded())
        {
            loopStartTick = PlaybackSettings.getInstance().addPrecountClickTrack(sequence, workContext);
            precountClickTrackId = sequence.getTracks().length - 1;
            mapTrackIdMuted.put(precountClickTrackId, false);
        }
        
        
        loopEndTick = loopStartTick + Math.round(workContext.getBeatRange().size() * MidiConst.PPQ_RESOLUTION);


        // Update the sequence if rerouting is needed
        // rerouteDrumsChannels(sequence, workContext.getMidiMix());
        // Listen to changes that can be handled without going dirty
        this.songContext.getSong().addPropertyChangeListener(this); // tempo changes
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
        return state.equals(State.GENERATED) ? sequence : null;
    }
    
    @Override
    public long getLoopStartTick()
    {
        return state.equals(State.GENERATED) ? loopStartTick : -1;
    }
    
    @Override
    public long getLoopEndTick()
    {
        return state.equals(State.GENERATED) ? loopEndTick : -1;
    }
    
    @Override
    public int getLoopCount()
    {
        return loopCount == PLAYBACK_SETTINGS_LOOP_COUNT ? PlaybackSettings.getInstance().getLoopCount() : loopCount;
    }
    
    @Override
    public long getTick(int barIndex)
    {
        long tick = -1;
        if (state.equals(State.GENERATED))
        {
            if (PlaybackSettings.getInstance().isClickPrecountEnabled() && barIndex == getBarRange().from)
            {
                // Precount is ON and pos is the first possible bar
                tick = 0;
            } else
            {
                // Precount if OFF or barIndex is not the first possible bar
                tick = songContext.getRelativeTick(new Position(barIndex, 0));
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
        return state.equals(State.GENERATED) ? songContext.getBarRange() : null;
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
        return state.equals(State.GENERATED) ? new HashMap<>(mapTrackIdMuted) : null;
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
        return state.equals(State.GENERATED) ? new HashMap<>(mapRvTrackId) : null;
    }


    /**
     * A map giving the resulting Phrase for each RhythmVoice, in the current context.
     * <p>
     *
     * @return Null if no meaningful value can be returned.
     */
    public Map<RhythmVoice, Phrase> getRvPhraseMap()
    {
        return state.equals(State.GENERATED) ? new HashMap<>(mapRvPhrase) : null;
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
    
    public boolean isPlaybackTranspositionEnabled()
    {
        return isPlaybackTranspositionEnabled;
    }
    
    public boolean isClickTrackIncluded()
    {
        return isClickTrackIncluded;
    }
    
    public boolean isPrecountTrackIncluded()
    {
        return isPrecountTrackIncluded;
    }
    
    public boolean isControlTrackIncluded()
    {
        return isControlTrackIncluded;
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
        return state.equals(State.GENERATED) ? controlTrack : null;
    }


    // ==========================================================================================================
    // EndOfPlaybackActionProvider implementation
    // ==========================================================================================================   
    @Override
    public ActionListener getEndOfPlaybackAction()
    {
        return endOfPlaybackAction;
    }


    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (state.equals(State.CLOSED))
        {
            return;
        }
        // If here state=GENERATED

        LOGGER.fine("propertyChange() e=" + e);
        
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
                case MidiMix.PROP_INSTRUMENT_MUTE:
                    InstrumentMix insMix = (InstrumentMix) e.getOldValue();
                    MidiMix mm = songContext.getMidiMix();
                    RhythmVoice rv = mm.geRhythmVoice(insMix);
                    Integer trackId = mapRvTrackId.get(rv);     // Can be null 
                    if (trackId != null)
                    {
                        mapTrackIdMuted.put(trackId, insMix.isMute());
                        pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                    }
                    
                    break;
                
                default:
                    // E.g MidiMix.PROP_USER_CHANNEL: do nothing
                    break;
            }
            
        } else if (e.getSource() == PlaybackSettings.getInstance())
        {
            switch (e.getPropertyName())
            {
                case PlaybackSettings.PROP_PLAYBACK_CLICK_ENABLED:
                    mapTrackIdMuted.put(playbackClickTrackId, !PlaybackSettings.getInstance().isPlaybackClickEnabled());
                    pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                    break;
                
                case PlaybackSettings.PROP_LOOPCOUNT:
                    if (loopCount == PLAYBACK_SETTINGS_LOOP_COUNT)
                    {
                        pcs.firePropertyChange(PROP_LOOP_COUNT, (Integer) e.getOldValue(), (Integer) e.getNewValue());
                    }
                    break;
                
                default:   // E.g. PROP_PLAYBACK_KEY_TRANSPOSITION
                // Nothing
            }
            
            
        }
    }
    
    @Override
    public String toString()
    {
        return "SongSession=[state=" + state + ", isDirty=" + isDirty + " songContext=" + songContext + "]";
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
     * Set as dirty and fire a change event.
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
    
    
    protected int preparePlaybackClickTrack(Sequence sequence, SongContext context)
    {
        // Add the click track
        PlaybackSettings cm = PlaybackSettings.getInstance();
        int trackId = cm.addClickTrack(sequence, context);
        // Send a Drums program change if Click channel is not used in the current MidiMix
//        int clickChannel = PlaybackSettings.getInstance().getPreferredClickChannel();
//        if (context.getMidiMix().getInstrumentMixFromChannel(clickChannel) == null)
//        {
        //                Instrument ins = DefaultInstruments.getInstance().getInstrument(RvType.Drums);
        //                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        //                jms.sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(clickChannel));  // Might not send anything if default instrument is Void Instrument
//        }
        return trackId;
    }
    
    protected void rerouteDrumsChannels(Sequence seq, MidiMix mm)
    {
        List<Integer> toBeRerouted = mm.getDrumsReroutedChannels();
        MidiUtilities.rerouteShortMessages(seq, toBeRerouted, MidiConst.CHANNEL_DRUMS);
    }

    /**
     * Get a context copy with a new song but same MidiMix and bar range.
     * <p>
     *
     * @param context
     * @param chordSymbolTransposition If not 0 use it to transpose chord symbols
     * @return
     */
    protected SongContext getContextCopy(SongContext context, int chordSymbolTransposition)
    {
        
        SongFactory sf = SongFactory.getInstance();
        CLI_Factory clif = CLI_Factory.getDefault();
        Song songCopy = sf.getCopy(context.getSong(), false);
        
        ChordLeadSheet clsCopy = songCopy.getChordLeadSheet();
        if (chordSymbolTransposition != 0)
        {
            for (CLI_ChordSymbol oldCli : clsCopy.getItems(CLI_ChordSymbol.class))
            {
                ExtChordSymbol newEcs = oldCli.getData().getTransposedChordSymbol(chordSymbolTransposition, Note.Alteration.FLAT);
                CLI_ChordSymbol newCli = clif.createChordSymbol(clsCopy, newEcs, oldCli.getPosition());
                clsCopy.removeItem(oldCli);
                clsCopy.addItem(newCli);
            }
        }
        SongContext res = new SongContext(songCopy, context.getMidiMix(), context.getBarRange());
        return res;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================

}
