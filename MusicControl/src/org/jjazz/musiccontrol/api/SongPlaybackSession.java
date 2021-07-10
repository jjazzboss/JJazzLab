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
package org.jjazz.musiccontrol.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.sound.midi.Sequence;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.Note;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.rhythmmusicgeneration.api.MidiSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.util.api.ResUtil;

/**
 * An PlaybackSession implementation based on a Song and optional PostProcessors.
 * <p>
 * Supports all the possible data of a PlaybackSession.
 */
public class SongPlaybackSession implements PlaybackSession, PropertyChangeListener
{

    private State state = State.NEW;
    private MusicGenerationContext mgContext;
    private final HashSet<ChangeListener> listeners = new HashSet<>();
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

    /**
     * Create a session with the specified parameters.
     *
     * @param mgContext
     * @param postProcessors Can be null, passed to the MidiSequenceBuilder in charge of creating the sequence.
     */
    public SongPlaybackSession(MusicGenerationContext mgContext, MusicGenerator.PostProcessor... postProcessors)
    {
        if (mgContext == null)
        {
            throw new NullPointerException("context"); //NOI18N
        }
        this.mgContext = mgContext;
        this.postProcessors = postProcessors;

        // Listen to song and midimix changes
        this.mgContext.getSong().addPropertyChangeListener(this);
        this.mgContext.getMidiMix().addPropertyListener(this);
        ClickManager.getInstance().addPropertyChangeListener(this);
    }

    /**
     * Create a new identical session, ready to be generated.
     *
     * @return
     */
    @Override
    public SongPlaybackSession getCopyInNewState()
    {
        return new SongPlaybackSession(mgContext, postProcessors);
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


        MusicGenerationContext workContext = mgContext;
        int t = MusicController.getInstance().getPlaybackKeyTransposition();
        if (t != 0)
        {
            workContext = buildTransposedContext(mgContext, t);
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


        // Build a context chord sequence, needed by some methods
        contextChordSequence = new ContextChordSequence(workContext);


        // Change state
        state = State.GENERATED;
        fireChanged();
    }


    public ContextChordSequence getContextChordGetSequence()
    {
        return contextChordSequence;
    }

    public MusicGenerationContext getMusicGenerationContext()
    {
        return mgContext;
    }

    @Override
    public Sequence getSequence()
    {
        return sequence;
    }

    @Override
    public long getTickStart()
    {
        return songTickStart;
    }

    @Override
    public long getTickEnd()
    {
        return songTickEnd;
    }

    @Override
    public int getClickTrackId()
    {
        return playbackClickTrackId;
    }

    /**
     * The positions in natural beat of all jjazz beat change controller events.
     * <p>
     * If provided, used by the MusicController to notify the current beat position to the framework.
     *
     * @return Null if not used
     */
    @Override
    public List<Position> getNaturalBeatPositions()
    {
        return naturalBeatPositions;
    }


    /**
     * The sequence track id (index) for each rhythm voice for the given context.
     * <p>
     * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track id.
     *
     * @return
     */
    public HashMap<RhythmVoice, Integer> getRvTrackId()
    {
        return mapRvTrackId;
    }

    public int getPrecountClickTrackId()
    {
        return precountClickTrackId;
    }

    public int getControlTrackId()
    {
        return controlTrackId;
    }

    @Override
    public void addChangeListener(ChangeListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener)
    {
        listeners.remove(listener);
    }


    @Override
    public void cleanup()
    {
        ClickManager.getInstance().removePropertyChangeListener(this);
        mgContext.getSong().removePropertyChangeListener(this);
        mgContext.getMidiMix().removePropertyListener(this);
    }


    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (!state.equals(State.GENERATED))
        {
            return;
        }

        boolean outdated = false;
        if (e.getSource() == mgContext.getSong())
        {
            if (e.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED))
            {
                if ((Boolean) e.getNewValue() == true)
                {
                    outdated = true;
                }
            }
        } else if (e.getSource() == mgContext.getMidiMix() && null != e.getPropertyName())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                    outdated = true;
                    break;
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                    outdated = true;
                    break;
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                    // KeyMap has changed, need to regenerate the sequence
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
        }

        if (outdated)
        {
            state = State.OUTDATED;
            fireChanged();
        }
    }

//    @Override
//    public String toString()
//    {
//        return "SongPlaybackSession=[" + mgContext + "," + Arrays.asList(postProcessors) + "]";
//    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    private void fireChanged()
    {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners)
        {
            l.stateChanged(e);
        }
    }


    /**
     *
     * @param sequence
     * @param mm
     * @param sg
     * @return The track id
     */
    private int preparePlaybackClickTrack(Sequence sequence, MusicGenerationContext context)
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
     * Get a new context with chord leadsheet transposed.
     * <p>
     *
     * @param context
     * @param transposition
     * @return
     */
    private MusicGenerationContext buildTransposedContext(MusicGenerationContext context, int transposition)
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
        MusicGenerationContext res = new MusicGenerationContext(songCopy, context.getMidiMix(), context.getBarRange());
        return res;
    }


}
