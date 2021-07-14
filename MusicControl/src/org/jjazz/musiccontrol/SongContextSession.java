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

import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.sound.midi.Sequence;
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
import org.jjazz.musiccontrol.api.ClickManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.ChordSymbolProvider;
import org.jjazz.musiccontrol.api.playbacksession.PositionProvider;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.rhythmmusicgeneration.api.MidiSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;

/**
 * A session based on a SongContext.
 * <p>
 * Take into account ClickManager settings (click & precount) and MusicController settings (playback transposition). Listen to
 * MidiMix to update tracks muted status.
 */
public class SongContextSession implements PropertyChangeListener, PlaybackSession, PositionProvider, ChordSymbolProvider, SongContextProvider
{

    private State state = State.NEW;
    private SongContext sgContext;
    private Sequence sequence;
    private List<Position> positions;
    private int playbackClickTrackId = -1;
    private int precountClickTrackId = -1;
    private int controlTrackId = -1;
    private long loopStartTick = -1;
    private long loopEndTick = -1;
    private ContextChordSequence contextChordSequence;
    private MusicGenerator.PostProcessor[] postProcessors;


    /**
     * The sequence track id (index) for each rhythm voice, for the given context.
     * <p>
     * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track id.
     */
    private HashMap<RhythmVoice, Integer> mapRvTrackId;
    private final HashMap<Integer, Boolean> mapRvMuted = new HashMap<>();
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    /**
     * Create a session with the specified parameters.
     *
     * @param sgContext
     * @param postProcessors Can be null, passed to the MidiSequenceBuilder in charge of creating the sequence.
     */
    public SongContextSession(SongContext sgContext, MusicGenerator.PostProcessor... postProcessors)
    {
        if (sgContext == null)
        {
            throw new NullPointerException("context"); //NOI18N
        }
        this.sgContext = sgContext;
        this.postProcessors = postProcessors;

        // Listen to all changes that can impact the generation of the song
        this.sgContext.getSong().addPropertyChangeListener(this);
        this.sgContext.getMidiMix().addPropertyChangeListener(this);
        ClickManager.getInstance().addPropertyChangeListener(this); // click settings
        MusicController.getInstance().addPropertyChangeListener(this);  // playback key transposition    
    }


    @Override
    public State getState()
    {
        return state;
    }

    @Override
    public void generate() throws MusicGenerationException
    {
        if (!state.equals(State.NEW))
        {
            throw new IllegalStateException("state=" + state);
        }


        SongContext workContext = sgContext;
        int t = MusicController.getInstance().getPlaybackKeyTransposition();
        if (t != 0)
        {
            workContext = buildTransposedContext(sgContext, t);
        }


        // Build the sequence
        MidiSequenceBuilder seqBuilder = new MidiSequenceBuilder(workContext, postProcessors);
        sequence = seqBuilder.buildSequence(false); // Can raise MusicGenerationException
        if (sequence == null)
        {
            // If unexpected error, assertion error etc.
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_BuildSeqError"));
        }


        // Used to identify a RhythmVoice's track
        mapRvTrackId = seqBuilder.getRvTrackIdMap();


        // Save the mute status of each RhythmVoice track
        MidiMix mm = sgContext.getMidiMix();
        for (RhythmVoice rv : mapRvTrackId.keySet())
        {
            mapRvMuted.put(mapRvTrackId.get(rv), mm.getInstrumentMixFromKey(rv).isMute());
        }


        // Add the control track
        ControlTrackBuilder ctm = new ControlTrackBuilder(workContext);
        controlTrackId = ctm.addControlTrack(sequence);
        mapRvMuted.put(controlTrackId, false);
        positions = ctm.getNaturalBeatPositions();


        // Add the playback click track
        playbackClickTrackId = preparePlaybackClickTrack(sequence, workContext);
        mapRvMuted.put(playbackClickTrackId, !ClickManager.getInstance().isPlaybackClickEnabled());


        // Add the click precount track - this must be done last because it might shift all song events
        loopStartTick = preparePrecountClickTrack(sequence, workContext);
        loopEndTick = loopStartTick + Math.round(workContext.getBeatRange().size() * MidiConst.PPQ_RESOLUTION);
        precountClickTrackId = sequence.getTracks().length - 1;
        mapRvMuted.put(precountClickTrackId, false);


        // Update the sequence if rerouting needed
        rerouteDrumsChannels(sequence, workContext.getMidiMix());


        // Build the context chord sequence 
        contextChordSequence = new ContextChordSequence(workContext);


        // Change state
        State old = state;
        state = State.GENERATED;
        pcs.firePropertyChange(PROP_STATE, old, state);


    }

    @Override
    public int getTempo()
    {
        return sgContext.getSong().getTempo();
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
    public long getTick(int barIndex)
    {
        if (!state.equals(State.GENERATED))
        {
            return -1;
        }

        long tick;
        if (ClickManager.getInstance().isClickPrecountEnabled() && barIndex == getBarRange().from)
        {
            // Precount is ON and pos is the first possible bar
            tick = 0;
        } else
        {
            // Precount if OFF or barIndex is not the first possible bar
            tick = sgContext.getRelativeTick(new Position(barIndex, 0));
            if (tick != -1)
            {
                tick += loopStartTick;
            }
        }
        return tick;
    }

    @Override
    public IntRange getBarRange()
    {
        return state.equals(State.GENERATED) || state.equals(State.OUTDATED) ? sgContext.getBarRange() : null;
    }

    /**
     * Include the click track.
     *
     * @return
     */
    @Override
    public HashMap<Integer, Boolean> getTracksMuteStatus()
    {
        return state.equals(State.GENERATED) || state.equals(State.OUTDATED) ? new HashMap<>(mapRvMuted) : null;
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
    public void cleanup()
    {
        ClickManager.getInstance().removePropertyChangeListener(this);
        MusicController.getInstance().removePropertyChangeListener(this);  // playback key transposition        
        sgContext.getSong().removePropertyChangeListener(this);
        sgContext.getMidiMix().removePropertyChangeListener(this);
    }


    public List<MusicGenerator.PostProcessor> getPostProcessors()
    {
        return Arrays.asList(postProcessors);
    }

    // ==========================================================================================================
    // SongContextProvider implementation
    // ==========================================================================================================    
    @Override
    public SongContext getSongContext()
    {
        return sgContext;
    }

    // ==========================================================================================================
    // PositionProvider implementation
    // ==========================================================================================================    

    @Override
    public List<Position> getPositions()
    {
        return state.equals(State.GENERATED) || state.equals(State.OUTDATED) ? positions : null;
    }

    // ==========================================================================================================
    // ChordSymbolProvider implementation
    // ==========================================================================================================    
    @Override
    public ContextChordSequence getContextChordGetSequence()
    {
        return state.equals(State.GENERATED) || state.equals(State.OUTDATED) ? contextChordSequence : null;
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

        State old = state;      // NEW, GENERATED or OUTDATED
        boolean outdated = false;

        if (e.getSource() == sgContext.getSong())
        {
            if (e.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED))
            {
                if ((Boolean) e.getNewValue() == true)
                {
                    outdated = true;        // Even if State is NEW, sgContext bar range might be not compatible with the updated song
                }
            } else if (e.getPropertyName().equals(Song.PROP_TEMPO))
            {
                pcs.firePropertyChange(PROP_TEMPO, (Integer) e.getOldValue(), (Integer) e.getNewValue());

            } else if (e.getPropertyName().equals(Song.PROP_CLOSED))
            {

                state = State.CLOSED;
                pcs.firePropertyChange(PROP_STATE, old, state);

            }
        } else if (e.getSource() == sgContext.getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_INSTRUMENT_MUTE:
                    if (!state.equals(State.NEW))
                    {
                        InstrumentMix insMix = (InstrumentMix) e.getOldValue();
                        MidiMix mm = sgContext.getMidiMix();
                        RhythmVoice rv = mm.geRhythmVoice(insMix);
                        Integer trackId = mapRvTrackId.get(rv);     // Can be null if state==outdated
                        if (trackId != null)
                        {
                            mapRvMuted.put(trackId, insMix.isMute());
                            pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                        }
                    }
                    break;

                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                    if (state.equals(State.GENERATED))
                    {
                        outdated = true;
                    }
                    break;

                default:
                    // eg MidiMix.PROP_USER_CHANNEL: do nothing
                    break;
            }


        } else if (e.getSource() == ClickManager.getInstance())
        {
            if (e.getPropertyName().equals(ClickManager.PROP_PLAYBACK_CLICK_ENABLED))
            {
                if (!state.equals(State.NEW))
                {
                    mapRvMuted.put(playbackClickTrackId, !ClickManager.getInstance().isPlaybackClickEnabled());
                    pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                }

            } else if (e.getPropertyName().equals(ClickManager.PROP_CLICK_PITCH_HIGH)
                    || e.getPropertyName().equals(ClickManager.PROP_CLICK_PITCH_LOW)
                    || e.getPropertyName().equals(ClickManager.PROP_CLICK_PREFERRED_CHANNEL)
                    || e.getPropertyName().equals(ClickManager.PROP_CLICK_VELOCITY_HIGH)
                    || e.getPropertyName().equals(ClickManager.PROP_CLICK_VELOCITY_LOW)
                    || e.getPropertyName().equals(ClickManager.PROP_CLICK_PRECOUNT_MODE))
            {
                if (!state.equals(State.NEW))
                {
                    outdated = true;
                }
            } else if (e.getPropertyName().equals(ClickManager.PROP_CLICK_PRECOUNT_ENABLED))
            {
                // Nothing: only getTick(from) return value will be impacted
            }
        } else if (e.getSource() == MusicController.getInstance())
        {
            if (e.getPropertyName().equals(MusicController.PROP_PLAYBACK_KEY_TRANSPOSITION))
            {
                // Playback transposition has changed
                if (!state.equals(State.NEW))
                {
                    outdated = true;
                }
            }
        }

        if (outdated)
        {
            state = State.OUTDATED;
            pcs.firePropertyChange(PROP_STATE, old, state);
        }

    }

    @Override
    public String toString()
    {
        return "PlaybackSession=[state=" + state + ", " + sgContext + ", " + Arrays.asList(postProcessors) + "]";
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    /**
     *
     * @param sequence
     * @param mm
     * @param sg
     * @return The track id
     */
    private int preparePlaybackClickTrack(Sequence sequence, SongContext context)
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
    private long preparePrecountClickTrack(Sequence sequence, SongContext context)
    {
        // Add the click track
        ClickManager cm = ClickManager.getInstance();
        long tickPos = cm.addPrecountClickTrack(sequence, context);
        // Send a Drums program change if Click channel is not used in the current MidiMix
//        int clickChannel = ClickManager.getInstance().getPreferredClickChannel();
//        if (context.getMidiMix().getInstrumentMixFromChannel(clickChannel) == null)
//        {
//                            Instrument ins = DefaultInstruments.getInstance().getInstrument(RvType.Drums);
//                            JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
//                            jms.sendMidiMessagesOnJJazzMidiOut(ins.getMidiMessages(clickChannel));  // Might not send anything if default instrument is Void Instrument
//        }
        return tickPos;
    }

    private void rerouteDrumsChannels(Sequence seq, MidiMix mm)
    {
        List<Integer> toBeRerouted = mm.getDrumsReroutedChannels();
        MidiUtilities.rerouteShortMessages(seq, toBeRerouted, MidiConst.CHANNEL_DRUMS);
    }

    /**
     * Get a new context with chord leadsheet transposed.
     * <p>
     *
     * @param context
     * @param transposition
     * @return
     */
    private SongContext buildTransposedContext(SongContext context, int transposition)
    {

        SongFactory sf = SongFactory.getInstance();
        CLI_Factory clif = CLI_Factory.getDefault();
        Song songCopy = sf.getCopy(context.getSong());
        sf.unregisterSong(songCopy);

        ChordLeadSheet clsCopy = songCopy.getChordLeadSheet();
        for (CLI_ChordSymbol oldCli : clsCopy.getItems(CLI_ChordSymbol.class))
        {
            ExtChordSymbol newEcs = oldCli.getData().getTransposedChordSymbol(transposition, Note.Alteration.FLAT);
            CLI_ChordSymbol newCli = clif.createChordSymbol(clsCopy, newEcs, oldCli.getPosition());
            clsCopy.removeItem(oldCli);
            clsCopy.addItem(newCli);
        }
        SongContext res = new SongContext(songCopy, context.getMidiMix(), context.getBarRange());
        return res;
    }


}
