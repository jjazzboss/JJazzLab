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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A control track contains custom MetaEvents used by the MusicController to fire PlaybackListener events.
 * <p>
 * - a trackname event <br>
 * - a MetaEvent for each chord symbol. <br>
 * - a MetaEvent for each beat changel. <br>
 * <p>
 * Methods are provided to extract the original data from these custom MetaEvents.
 */
public class ControlTrack
{

    public static final int POSITION_META_EVENT_TYPE = 10;
    public static final int CHORD_SYMBOL_META_EVENT_TYPE = 11;
    public static String TRACK_NAME = "JJazzControlTrack";
    private List<MidiEvent> midiEvents = new ArrayList<>();
    private SongChordSequence contextChordSequence;
    private final int trackId;
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


    public int getTrackId()
    {
        return trackId;
    }

    /**
     * The list of MidiEvents of the control track: track name, beat changes, chord symbol changes.
     *
     * @return Can't be null. IMPORTANT: events may NOT be ordered by tick position.
     */
    public List<MidiEvent> getMidiEvents()
    {
        return midiEvents;
    }


    /**
     * The chord sequence used to generate the control track events.
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


    /**
     * Retrieve the chord symbol from a control track MetaMessage.
     *
     * @param mm A MetaMessage with type==CHORD_SYMBOL_META_EVENT_TYPE
     * @return Can be null if mm is unknown
     */
    public CLI_ChordSymbol getChordSymbol(MetaMessage mm)
    {
        Preconditions.checkArgument(mm.getType() == CHORD_SYMBOL_META_EVENT_TYPE, "mm=%s", mm);
        return ((MmChordSymbol) mm).cliCs;
    }

    /**
     * Retrieve the Position from a control track MetaMessage.
     *
     * @param mm A MetaMessage with type==POSITION_META_EVENT_TYPE
     * @return Can be null if mm is unknown
     */
    public Position getPosition(MetaMessage mm)
    {
        Preconditions.checkArgument(mm.getType() == POSITION_META_EVENT_TYPE, "mm=%s", mm);
        return ((MmPosition) mm).pos;
    }

    /**
     * Retrieve the position in beats from a control track MetaMessage.
     *
     * @param mm A MetaMessage with type==POSITION_META_EVENT_TYPE
     * @return -1 if mm is unknown
     */
    public float getPositionInBeats(MetaMessage mm)
    {
        Preconditions.checkArgument(mm.getType() == POSITION_META_EVENT_TYPE, "mm=%s", mm);
        return ((MmPosition) mm).posInBeats;
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
        float posInBeatsOffset = context.getSptBeatRange(spt).from;

        LOGGER.log(Level.FINE, "addBeatChangeEvents() -- tickOffset={0} spt={1} sptRange={2}", new Object[]
        {
            tickOffset, spt, sptRange
        });

        // Add events every beat change
        for (float beat = 0; beat < nbNaturalBeats; beat++)
        {
            long tick = (long) (tickOffset + beat * MidiConst.PPQ_RESOLUTION);
            int bar = (int) Math.floor(beat / nbNaturalBeatsPerBar);
            float inbarBeat = beat - (bar * nbNaturalBeatsPerBar);
            float posInBeats = posInBeatsOffset + beat;
            Position pos = new Position(bar + sptStartBar, inbarBeat);
            MmPosition mm = new MmPosition(pos, posInBeats);
            midiEvents.add(new MidiEvent(mm, tick));
        }

        return (long) (tickOffset + nbNaturalBeats * MidiConst.PPQ_RESOLUTION);
    }

    private void addChordSymbolEvents(SongContext context)
    {
        int csIndex = 0;
        for (CLI_ChordSymbol cliCs : contextChordSequence)
        {
            long tick = context.toRelativeTick(cliCs.getPosition());
            assert tick != -1 : "cliCs=" + cliCs + " contextChordSequence=" + contextChordSequence + " context=" + context;
            var mm = new MmChordSymbol(cliCs);
            // HACK!
            // tick+1 is a hack, otherwise when tick==0 the first Meta event is sometimes not fired! Don't know why
            midiEvents.add(new MidiEvent(mm, tick + 1));
            csIndex++;
        }
    }


    // =================================================================================
    // Inner classes
    // =================================================================================
    private class MmPosition extends MetaMessage
    {

        private final float posInBeats;
        private final Position pos;

        private MmPosition(Position pos, float posInBeats)
        {
            Preconditions.checkNotNull(pos);
            this.posInBeats = posInBeats;
            this.pos = pos;
            try
            {
                setMessage(POSITION_META_EVENT_TYPE, new byte[0], 0);
            } catch (InvalidMidiDataException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private class MmChordSymbol extends MetaMessage
    {

        private final CLI_ChordSymbol cliCs;

        private MmChordSymbol(CLI_ChordSymbol cliCs)
        {
            Preconditions.checkNotNull(cliCs);
            this.cliCs = cliCs;
            try
            {
                setMessage(CHORD_SYMBOL_META_EVENT_TYPE, new byte[0], 0);
            } catch (InvalidMidiDataException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
