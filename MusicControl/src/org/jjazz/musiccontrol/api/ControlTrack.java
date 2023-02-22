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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A control track contains special Midi events used by the MusicController to fire PlaybackListener events.
 * <p>
 * - a trackname event <br>
 - a marker event for each chord symbol, with text="csIndex=chord_symbol_index" (index of the SongChordSequence provided by
 {@link #getContextChordSequence()}). <br>
 * - a CTRL_CHG_JJAZZ_BEAT_CHANGE controller Midi event at every beat change, use {@link #getSongPositions()} to get the
 * corresponding Position.<br>
 */
public class ControlTrack
{

    public static String TRACK_NAME = "JJazzControlTrack";
    private List<MidiEvent> midiEvents = new ArrayList<>();
    private SongChordSequence contextChordSequence;
    private List<Position> songPositions = new ArrayList<>();
    private int trackId;
    private static final Logger LOGGER = Logger.getLogger(ControlTrack.class.getSimpleName());

    /**
     * Create a control track for the specified SongContext.
     *
     * @param sgContext
     * @param trackId
     */
    public ControlTrack(SongContext sgContext, int trackId)
    {
        try
        {
            contextChordSequence = new SongChordSequence(sgContext.getSong(), sgContext.getBarRange());       // This will process the substitute chord symbols
        } catch (UserErrorGenerationException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        // Add track name
        midiEvents.add(new MidiEvent(MidiUtilities.getTrackNameMetaMessage(TRACK_NAME), 0));

        long tick = 0;

        // Add the beat change events and update songPositions
        for (SongPart spt : sgContext.getSongParts())
        {
            tick = addBeatChangeEvents(sgContext, tick, spt);
        }

        // Add the chord symbol marker events
        addChordSymbolEvents(sgContext);
        this.trackId = trackId;
    }


    /**
     * Create a control track with custom values.
     * <p>
     * No null value allowed.
     *
     * @param controlTrackEvents
     * @param contextChordSequence
     * @param songPositions
     * @param trackId
     */
    public ControlTrack(List<MidiEvent> controlTrackEvents, SongChordSequence contextChordSequence, List<Position> songPositions, int trackId)
    {
        if (controlTrackEvents == null || contextChordSequence == null || songPositions == null)
        {
            throw new IllegalArgumentException("controlTrackEvents=" + controlTrackEvents + " contextChordSequence=" + contextChordSequence + " songPositions=" + songPositions);
        }
        this.midiEvents = controlTrackEvents;
        this.contextChordSequence = contextChordSequence;
        this.songPositions = songPositions;
        this.trackId = trackId;
    }

    public int getTrackId()
    {
        return trackId;
    }

    /**
     * The list of MidiEvents of the control track: track name event, CTRL_CHG_JJAZZ_BEAT_CHANGE controller change events and
     * chord symbol marker events.
     *
     * @return Can't be null. IMPORTANT: events may NOT be ordered by tick position.
     */
    public List<MidiEvent> getMidiEvents()
    {
        return midiEvents;
    }

    /**
     * The list is used to convert a CTRL_CHG_JJAZZ_BEAT_CHANGE controller event index into into a Position in the song.
     *
     * @return Can't be null
     *
     */
    public List<Position> getSongPositions()
    {
        return songPositions;
    }

    /**
     * The chord sequence used to retrieve the chord symbol from the index passed in the chord symbol Meta marker event.
     *
     * @return Can't be null
     */
    public SongChordSequence getContextChordGetSequence()
    {
        return contextChordSequence;
    }

    /**
     * Fill the specified track with this control track MidiEvents.
     *
     * @param track
     */
    public void fillTrack(Track track)
    {
        if (track == null)
        {
            throw new IllegalArgumentException("track=" + track);   
        }
        getMidiEvents().forEach(me -> track.add(me));
    }

    @Override
    public String toString()
    {
        return "midiEvents.size()=" + midiEvents.size() + " contextChordSeq=" + contextChordSequence;
    }


    // =================================================================================
    // Private methods
    // =================================================================================
    /**
     * Create control events for the specified SongPart.
     * <p>
     * Update the songPositions list.
     *
     * @param context
     * @param tickOffset Will be 0 for the first SongPart
     * @param spt
     * @return The tick position corresponding to the start of next spt.
     */
    private long addBeatChangeEvents(SongContext context, long tickOffset, SongPart spt)
    {
        IntRange sptRange = context.getSptBarRange(spt);    // Use only the relevant bars for the context
        int sptStartBar = sptRange.from;
        float nbNaturalBeatsPerBar = spt.getRhythm().getTimeSignature().getNbNaturalBeats();
        float nbNaturalBeats = sptRange.size() * nbNaturalBeatsPerBar;

        // LOGGER.fine("addBeatChangeEvents() -- tickOffset=" + tickOffset + " spt=" + spt + " sptRange=" + sptRange);   

        // Add CTRL_CHG_JJAZZ_BEAT_CHANGE events every beat change
        for (float beat = 0; beat < nbNaturalBeats; beat++)
        {
            long tick = (long) (tickOffset + beat * MidiConst.PPQ_RESOLUTION);
            int bar = (int) Math.floor(beat / nbNaturalBeatsPerBar);
            float inbarBeat = beat - (bar * nbNaturalBeatsPerBar);
            Position pos = new Position(bar + sptStartBar, inbarBeat);
            songPositions.add(pos);
            ShortMessage sm = MidiUtilities.getJJazzBeatChangeControllerMessage(MidiConst.CHANNEL_MIN);
            midiEvents.add(new MidiEvent(sm, tick));
        }

        return (long) (tickOffset + nbNaturalBeats * MidiConst.PPQ_RESOLUTION);
    }

    private void addChordSymbolEvents(SongContext context)
    {
        int csIndex = 0;
        for (CLI_ChordSymbol cliCs : contextChordSequence)
        {
            long tick = context.getRelativeTick(cliCs.getPosition());
            assert tick != -1 : "cliCs=" + cliCs + " contextChordSequence=" + contextChordSequence + " context=" + context;
            MetaMessage mm = MidiUtilities.getMarkerMetaMessage("csIndex=" + csIndex);
            // HACK!
            // tick+1 is a hack, otherwise when tick==0 the first Meta event is sometimes not fired! Don't know why
            midiEvents.add(new MidiEvent(mm, tick + 1));
            csIndex++;
        }
    }

}
