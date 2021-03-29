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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.IntRange;

/**
 * Methods to prepare a control track.
 */
public class ControlTrackBuilder
{

    public static final int ACTIVITY_MIN_PERIOD = MidiConst.PPQ_RESOLUTION / 4;
    public static String TRACK_NAME = "JJazzControlTrack";
    private MusicGenerationContext context;
    /**
     * Store the position of each natural beat.
     */
    private final ArrayList<Position> naturalBeatPositions = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(ControlTrackBuilder.class.getSimpleName());

    public ControlTrackBuilder(MusicGenerationContext context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context=" + context);   //NOI18N
        }
        this.context = context;
    }

    /**
     * Add a control track for the given context with the following events:
     * <p>
     * - a marker event for each chord symbol (text=original name of the chord symbol)<br>
     * - a CTRL_CHG_JJAZZ_BEAT_CHANGE Midi event at every beat change<br>
     * - a CTRL_CHG_JJAZZ_ACTIVITY Midi event for most of NOTE_ON messages on each channel. <br>
     *
     * @param sequence The sequence for which we add the control track.
     * @return the index of the track in the sequence.
     */
    public int addControlTrack(Sequence sequence)
    {
        if (sequence == null)
        {
            throw new IllegalArgumentException("sequence=" + sequence);   //NOI18N
        }
        Track track = sequence.createTrack();

        // Add track name
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(TRACK_NAME), 0);
        track.add(me);

        long tick = 0;
        naturalBeatPositions.clear();

        // Scan all SongParts in context
        for (SongPart spt : context.getSongParts())
        {
            tick = fillControlTrack(track, tick, spt);
        }

        // Add the Midi Activity controller messages
        addActivityMessages(sequence, track);

        // Set EndOfTrack
        long lastTick = (long) (context.getBeatRange().size() * MidiConst.PPQ_RESOLUTION) + 1;
        MidiUtilities.setEndOfTrackPosition(track, lastTick);

        return Arrays.asList(sequence.getTracks()).indexOf(track);
    }

    /**
     * Get an array containing the natural beat positions computed by the last call to addControlTrack().
     * <p>
     *
     * @return
     */
    public List<Position> getNaturalBeatPositions()
    {
        return naturalBeatPositions;
    }

    // =================================================================================
    // Private methods
    // =================================================================================
    /**
     * Fill the track with control events for the specified SongPart.
     * <p>
     * Update the naturalBeatPositions list.
     *
     * @param track
     * @param tickOffset Will be 0 for the first SongPart
     * @param spt
     * @return The tick position corresponding to the start of next spt.
     */
    private long fillControlTrack(Track track, long tickOffset, SongPart spt)
    {
        IntRange sptRange = context.getSptBarRange(spt);    // Use only the relevant bars for the context
        int sptStartBar = sptRange.from;
        float nbNaturalBeatsPerBar = spt.getRhythm().getTimeSignature().getNbNaturalBeats();
        float nbNaturalBeats = sptRange.size() * nbNaturalBeatsPerBar;

        LOGGER.fine("fillControlTrack() -- tickOffset=" + tickOffset + " spt=" + spt + " sptRange=" + sptRange);   //NOI18N

        // Add CTRL_CHG_JJAZZ_BEAT_CHANGE events every beat change
        for (float beat = 0; beat < nbNaturalBeats; beat++)
        {
            long tick = (long) (tickOffset + beat * MidiConst.PPQ_RESOLUTION);
            int bar = (int) Math.floor(beat / nbNaturalBeatsPerBar);
            float inbarBeat = beat - (bar * nbNaturalBeatsPerBar);
            Position pos = new Position(bar + sptStartBar, inbarBeat);
            naturalBeatPositions.add(pos);
            ShortMessage sm = MidiUtilities.getJJazzBeatChangeControllerMessage(MidiConst.CHANNEL_MIN);
            track.add(new MidiEvent(sm, tick));
        }


        // Add Marker Meta events for all chord symbol in the context range
        // Make sure that there is a marker at the beginning of the context range 
        // (this might be required only for the first song part, if context range.from is > spt.startBarIndex
        CLI_Section section = spt.getParentSection();
        ChordLeadSheet cls = section.getContainer();
        var items = cls.getItems(section, CLI_ChordSymbol.class);
        assert !items.isEmpty();
        CLI_ChordSymbol prevCli = null;
        boolean firstChordSymbolInRange = true;

        for (CLI_ChordSymbol cli : items)
        {
            Position ssPos = context.getSong().getSongStructure().getSptItemPosition(spt, cli);
            long tick = context.getRelativeTick(ssPos);
            LOGGER.log(Level.FINE, "fillControlTrack() cli={0} tick={1} ssPos={2}", new Object[]   //NOI18N
            {
                cli, tick, ssPos
            });
            if (tick != -1)
            {
                MetaMessage mm = MidiUtilities.getMarkerMetaMessage(cli.getData().getOriginalName());
                // tick+1 is a hack, otherwise when tick==0 the first Meta event is sometimes not fired! Don't know why
                track.add(new MidiEvent(mm, tick + 1));
                if (firstChordSymbolInRange && tickOffset == 0 && tick > 0)
                {
                    assert prevCli != null : "spt=" + spt + " cli=" + cli + " tick=" + tick;
                    // Need to add an initial chord symbol (context range might start in the middle of the section)
                    mm = MidiUtilities.getMarkerMetaMessage(prevCli.getData().getOriginalName());
                    track.add(new MidiEvent(mm, 1));            // 1 instead of 0, see hack explanation above 
                }
                firstChordSymbolInRange = false;
            }

            prevCli = cli;
        }


        return (long) (tickOffset + nbNaturalBeats * MidiConst.PPQ_RESOLUTION);
    }

    /**
     * Add CTRL_CHG_JJAZZ_ACTIVITY controller messages for each NOTE_ON on each channel.
     * <p>
     * For a given channel add a single CTRL_CHG_JJAZZ_ACTIVITY message if several NOTE_ONs start within the same
     * ACTIVITY_MIN_PERIOD.
     *
     * @param sequence The track to analyze
     * @param ctrlTrack Where CTRL_CHG_JJAZZ_ACTIVITY messages will be added
     */
    private void addActivityMessages(Sequence sequence, Track ctrlTrack)
    {
        int nbChannels = MidiConst.CHANNEL_MAX - MidiConst.CHANNEL_MIN + 1;
        long[] lastActivityTick = new long[nbChannels];

        for (Track track : sequence.getTracks())
        {
            for (int i = 0; i < nbChannels; i++)
            {
                lastActivityTick[i] = -2 * ACTIVITY_MIN_PERIOD;
            }
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent me = track.get(i);
                MidiMessage mm = me.getMessage();
                if (!(mm instanceof ShortMessage) || ((ShortMessage) mm).getCommand() != ShortMessage.NOTE_ON)
                {
                    continue;
                }
                int channel = ((ShortMessage) mm).getChannel();
                long tick = me.getTick();
                if (tick - lastActivityTick[channel] > ACTIVITY_MIN_PERIOD)
                {
                    ShortMessage controlSm = MidiUtilities.getJJazzActivityControllerMessage(channel);
                    MidiEvent ctrlMe = new MidiEvent(controlSm, tick);
                    ctrlTrack.add(ctrlMe);
                    lastActivityTick[channel] = tick;
                }
            }
        }
    }
}
