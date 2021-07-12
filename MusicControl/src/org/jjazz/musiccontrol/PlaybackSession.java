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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.sound.midi.Sequence;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.Note;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserChannelRvKey;
import org.jjazz.musiccontrol.api.ClickManager;
import org.jjazz.musiccontrol.api.ControlTrackBuilder;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.rhythmmusicgeneration.api.MidiSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.util.api.ResUtil;

/**
 * A PlaybackSession holds all the data needed by the MusicController to play a song and provide all the related services (firing
 * events, muting/unmuting tracks, managing tempo changes, ...).
 * <p>
 * Data include the Midi sequence, start/end tick, the muted tracks, a ContextChordSequence, etc.
 * <p>
 * The PlaybackSession listens to the underlying data (song, midimix, etc.) changes to move its State from GENERATED to OUTDATED.
 */
public class PlaybackSession implements PropertyChangeListener
{

    public static final String PROP_STATE = "State";
    /**
     * Song tempo has changed.
     */
    public static final String PROP_TEMPO = "Tempo";
    /**
     * One or more tracks muted status has changed.
     *
     * @see getTracksMuteStatus()
     */
    public static final String PROP_MUTED_TRACKS = "MutedTracks";

    public enum State
    {
        /**
         * State of the session upon creation, sequence and related data are not generated yet. Sequence and related data values
         * are undefined in this state.
         */
        NEW,
        /**
         * Sequence and related data have been generated and are up to date with the underlying data.
         */
        GENERATED,
        /**
         * Sequence and related data were generated but are now out of date compared to the underlying data.
         */
        OUTDATED,
        /**
         * The session is closed (e.g. song is no more available) and any playback should be stopped.
         */
        CLOSED
    }

    private State state = State.NEW;
    private SongContext sgContext;
    private Sequence sequence;
    private List<Position> naturalBeatPositions;
    private int playbackClickTrackId = -1;
    private int precountClickTrackId = -1;
    private int controlTrackId = -1;
    private long songTickStart = -1;
    private long songTickEnd = -1;
    private ContextChordSequence contextChordSequence;
    private MusicGenerator.PostProcessor[] postProcessors;
    /**
     * The sequence track id (index) for each rhythm voice, for the given context.
     * <p>
     * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track id.
     */
    private HashMap<RhythmVoice, Integer> mapRvTrackId;
    private HashMap<Integer, Boolean> mapRvMuted = new HashMap<>();
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    /**
     * Create a session with the specified parameters.
     *
     * @param sgContext
     * @param postProcessors Can be null, passed to the MidiSequenceBuilder in charge of creating the sequence.
     */
    public PlaybackSession(SongContext sgContext, MusicGenerator.PostProcessor... postProcessors)
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

    /**
     * Create a new identical session, ready to be generated.
     *
     * @return
     */
    public PlaybackSession getCopyInNewState()
    {
        return new PlaybackSession(sgContext, postProcessors);
    }

    public State getState()
    {
        return state;
    }

    public List<MusicGenerator.PostProcessor> getPostProcessors()
    {
        return Arrays.asList(postProcessors);
    }

    /**
     * Create the sequence and the related data.
     * <p>
     * The method changes the state to GENERATED.
     *
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     * @throws IllegalStateException If State is not NEW.
     */
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
        naturalBeatPositions = ctm.getNaturalBeatPositions();


        // Add the playback click track
        playbackClickTrackId = preparePlaybackClickTrack(sequence, workContext);


        // Add the click precount track - this must be done last because it might shift all song events
        songTickStart = preparePrecountClickTrack(sequence, workContext);
        songTickEnd = songTickStart + Math.round(workContext.getBeatRange().size() * MidiConst.PPQ_RESOLUTION);
        precountClickTrackId = sequence.getTracks().length - 1;


        // Update the sequence if rerouting needed
        rerouteDrumsChannels(sequence, workContext.getMidiMix());


        // Build the context chord sequence 
        contextChordSequence = new ContextChordSequence(workContext);


        // Change state
        State old = state;
        state = State.GENERATED;
        pcs.firePropertyChange(PROP_STATE, old, state);
    }


    public ContextChordSequence getContextChordGetSequence()
    {
        return contextChordSequence;
    }

    public SongContext getSongContext()
    {
        return sgContext;
    }

    public Sequence getSequence()
    {
        return sequence;
    }

    /**
     * The tick position of the start of the song.
     * <p>
     * Take into account the possible precount clicks.
     *
     * @return
     */
    public long getTickStart()
    {
        return songTickStart;
    }

    /**
     * The tick position of the end of the song/loop point.
     * <p>
     * Take into account the possible precount clicks.
     *
     * @return
     */
    public long getTickEnd()
    {
        return songTickEnd;
    }

    public int getClickTrackId()
    {
        return playbackClickTrackId;
    }

    /**
     * The positions in natural beat of all jjazz beat change controller events.
     * <p>
     * Used by the MusicController to notify the current beat position to the framework.
     *
     * @return
     */
    public List<Position> getNaturalBeatPositions()
    {
        return naturalBeatPositions;
    }

    /**
     * Get the mute status of each RhythmVoice track id.
     * <p>
     *
     * @return
     */
    public HashMap<Integer, Boolean> getTracksMuteStatus()
    {
        return new HashMap<>(mapRvMuted);
    }

    public int getPrecountClickTrackId()
    {
        return precountClickTrackId;
    }

    public int getControlTrackId()
    {
        return controlTrackId;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * Must be called before disposing this session.
     */
    public void cleanup()
    {
        ClickManager.getInstance().removePropertyChangeListener(this);
        MusicController.getInstance().removePropertyChangeListener(this);  // playback key transposition        
        sgContext.getSong().removePropertyChangeListener(this);
        sgContext.getMidiMix().removePropertyChangeListener(this);
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

        State old = state;


        boolean outdated = false;
        if (e.getSource() == sgContext.getSong())
        {
            if (e.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED))
            {
                if ((Boolean) e.getNewValue() == true)
                {
                    outdated = true;
                }
            } else if (e.getPropertyName() == null ? Song.PROP_TEMPO == null : e.getPropertyName().equals(Song.PROP_TEMPO))
            {

                pcs.firePropertyChange(PROP_TEMPO, (Integer) e.getOldValue(), (Integer) e.getNewValue());

            } else if (e.getPropertyName() == null ? Song.PROP_CLOSED == null : e.getPropertyName().equals(Song.PROP_CLOSED))
            {

                state = State.CLOSED;
                pcs.firePropertyChange(PROP_STATE, old, state);

            }
        } else if (e.getSource() == sgContext.getMidiMix() && null != e.getPropertyName())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_INSTRUMENT_MUTE:
                    InstrumentMix insMix = (InstrumentMix) e.getOldValue();
                    MidiMix mm = sgContext.getMidiMix();
                    RhythmVoice rv = mm.geRhythmVoice(insMix);
                    Integer trackId = mapRvTrackId.get(rv);     // Can be null if state==outdated
                    if (trackId != null)
                    {
                        mapRvMuted.put(trackId, insMix.isMute());
                        pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                    }
                    break;
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                    // MidiMix and tracks no longer exactly match
                    outdated = true;
                    break;
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                    outdated = true;
                    break;
                default:
                    // eg MidiMix.PROP_USER_CHANNEL: do nothing
                    break;
            }
        } else if (e.getSource() == ClickManager.getInstance())
        {
            if (!e.getPropertyName().equals(ClickManager.PROP_PLAYBACK_CLICK_ENABLED))
            {
                // Make sure click track is recalculated (click channel, instrument, etc. might have changed)      
                outdated = true;
            }
        } else if (e.getSource() == MusicController.getInstance())
        {
            if (e.getPropertyName().equals(MusicController.PROP_PLAYBACK_KEY_TRANSPOSITION))
            {
                // Playback transposition has changed
                outdated = true;
            }
        }

        if (outdated && state.equals(State.GENERATED))
        {
            state = State.OUTDATED;
            pcs.firePropertyChange(PROP_STATE, old, state);
        }
    }

//    @Override
//    public String toString()
//    {
//        return "PlaybackSession=[" + sgContext + "," + Arrays.asList(postProcessors) + "]";
//    }

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
     * Get a new context with chord leadsheet transposed.
     * <p>
     *
     * @param context
     * @param transposition
     * @return
     */
    private SongContext buildTransposedContext(SongContext context, int transposition)
    {

        org.jjazz.song.api.SongFactory sf = SongFactory.getInstance();
        CLI_Factory clif = CLI_Factory.getDefault();
        Song songCopy = sf.getCopy(context.getSong());
        sf.unregisterSong(songCopy);
        ChordLeadSheet clsCopy = songCopy.getChordLeadSheet();
        for (org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol oldCli : clsCopy.getItems(CLI_ChordSymbol.class))
        {
            org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol newEcs = oldCli.getData().getTransposedChordSymbol(transposition, Note.Alteration.FLAT);
            org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol newCli = clif.createChordSymbol(clsCopy, newEcs, oldCli.getPosition());
            clsCopy.removeItem(oldCli);
            clsCopy.addItem(newCli);
        }
        SongContext res = new SongContext(songCopy, context.getMidiMix(), context.getBarRange());
        return res;
    }


}
