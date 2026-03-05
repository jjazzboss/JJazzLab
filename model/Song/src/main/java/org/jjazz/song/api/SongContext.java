/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.song.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;

/**
 * A Song music generation context.
 * <p>
 * Associates a Song, a MidiMix and a bar range within that song.
 * <p>
 */
public interface SongContext
{

    /**
     * Music should be produced for this MidiMix.
     *
     * @return
     */
    MidiMix getMidiMix();

    /**
     * Music should be produced for this song.
     *
     * @return
     */
    Song getSong();

    /**
     * Music should be produced for the song's bar range.
     * <p>
     *
     * @return
     */
    IntRange getBarRange();

    /**
     * Deep copy the Song and the MidiMix.
     *
     * @param disableInternalUpdates If true the returned Song instance will have internal consistency updates disabled, e.g. changing a section in the
     *                               ChordLeadSheet won't impact the SongStructure. The returned MidiMix instance will also not be associated to the Song copy.
     *                               For special purposes only, this can lead to inconsistent states.
     * @return
     */
    SongContext getDeepCopy(boolean disableInternalUpdates);

    /**
     * Shallow cloning which reuses the same song and midi mix.
     *
     * @return
     */
    SongContext clone();

    /**
     * The beat range within the song for which music should be produced.
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part). If getBarRange().from &gt; 0 then toBeatRange().from is also &gt;
     * 0.
     *
     * @return
     */
    default FloatRange getBeatRange()
    {
        var res = getSong().getSongStructure().toBeatRange(getBarRange());
        return res;
    }


    /**
     * Get all the song parts contained in this context bar range.
     * <p>
     *
     * @return Can be empty.
     * @see #isInRange(org.jjazz.songstructure.api.SongPart)
     */
    default List<SongPart> getSongParts()
    {
        var res = getSong().getSongStructure().getSongParts().stream()
                .filter(spt -> isInRange(spt))
                .toList();
        return res;
    }

    /**
     * The tick range corresponding to getBarRange() or getBeatRange().
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part). If toBeatRange().from &gt; 0 then getTickRange().from is also &gt;
     * 0.
     *
     * @return
     */
    default public LongRange getTickRange()
    {
        var beatRange = getBeatRange();
        var res = new LongRange((long) (beatRange.from * MidiConst.PPQ_RESOLUTION), (long) (beatRange.to * MidiConst.PPQ_RESOLUTION));
        return res;
    }

    /**
     * Get the intersection between the song part bar range and this SongContext bar range.
     * <p>
     *
     * @param spt
     * @return Can be the EMPTY_RANGE if spt is not part of this context.
     */
    default IntRange getSptBarRange(SongPart spt)
    {
        return spt.getBarRange().getIntersection(getBarRange());
    }

    /**
     * Get the intersection between the song part beat range and this SongContext beat range.
     * <p>
     *
     * @param spt
     * @return
     */
    default FloatRange getSptBeatRange(SongPart spt)
    {
        FloatRange sptRange = getSong().getSongStructure().toBeatRange(spt.getBarRange());
        return sptRange.getIntersectRange(getBeatRange());
    }

    /**
     * Get the range of ticks of spt belonging to this context.
     *
     * @param spt
     * @return
     */
    default LongRange getSptTickRange(SongPart spt)
    {
        FloatRange sptRange = getSptBeatRange(spt);
        LongRange lr = new LongRange((long) (sptRange.from * MidiConst.PPQ_RESOLUTION), (long) (sptRange.to * MidiConst.PPQ_RESOLUTION));
        return lr;
    }


    /**
     * Get the bar ranges in this context whose SongParts use rpValue.
     * <p>
     * Adjacent BarRanges are merged in the returned list.
     *
     * @param <E>
     * @param r
     * @param rp      A RhythmParameter from r
     * @param rpValue A value for rp
     * @return Can be empty
     */
    default <E> List<IntRange> getMergedBarRanges(Rhythm r, RhythmParameter<E> rp, E rpValue)
    {
        Objects.requireNonNull(r);
        Objects.requireNonNull(rp);
        Preconditions.checkArgument(r.getRhythmParameters().contains(rp));

        List<IntRange> res = new ArrayList<>();
        for (var spt : getSongParts())
        {
            if (spt.getRhythm() == r && spt.getRPValue(rp).equals(rpValue))
            {
                res.add(getSptBarRange(spt));
            }
        }
        res = IntRange.merge(res);
        return res;
    }

    /**
     * Get the list of unique rhythms used in this context.
     *
     * @return
     */
    default List<Rhythm> getUniqueRhythms()
    {
        ArrayList<Rhythm> res = new ArrayList<>();
        for (SongPart spt : getSong().getSongStructure().getSongParts())
        {
            if (isInRange(spt) && !res.contains(spt.getRhythm()))
            {
                res.add(spt.getRhythm());
            }
        }
        return res;
    }

    /**
     * Get the list of unique rhythm voices used in this context.
     *
     * @return
     */
    default List<RhythmVoice> getUniqueRhythmVoices()
    {
        ArrayList<RhythmVoice> rvs = new ArrayList<>();
        for (Rhythm r : getUniqueRhythms())
        {
            rvs.addAll(r.getRhythmVoices());
        }
        return rvs;
    }

    /**
     * Get the unique parent sections used by the context song parts.
     *
     * @return An ordered list by position
     */
    default public List<CLI_Section> getUniqueSections()
    {
        List<CLI_Section> res = new ArrayList<>();

        getSongParts().stream()
                .map(spt -> spt.getParentSection())
                .filter(section -> !res.contains(section))
                .forEach(section -> res.add(section));
        return res;
    }


    /**
     * Check if the specified spt has at least one bar in the range of this context.
     *
     * @param spt
     * @return
     */
    default boolean isInRange(SongPart spt)
    {
        return getBarRange().isIntersecting(spt.getBarRange());
    }

    /**
     * Converts a tick position relative to this context into a ChordLeadSheet Position.
     *
     * @param relativeTick
     * @return Null if tick is out of the bounds of this context.
     */
    default Position toClsPosition(long relativeTick)
    {
        Position ssPos = toPosition(relativeTick);
        if (ssPos == null)
        {
            return null;
        }
        SongPart spt = getSong().getSongStructure().getSongPart(ssPos.getBar());
        int sectionBar = spt.getParentSection().getPosition().getBar();
        return new Position(ssPos.getBar() - spt.getStartBarIndex() + sectionBar, ssPos.getBeat());
    }

    /**
     * Compute the tick relative to this context for the given absolute position expressed in bar/beat.
     * <p>
     *
     * @param pos
     * @return -1 if pos is outside this context range. Returns 0 for the first bar/beat of the context range.
     */
    default long toRelativeTick(Position pos)
    {
        if (pos == null)
        {
            throw new NullPointerException("pos");
        }
        long tick = -1;
        int bar = pos.getBar();
        float beat = pos.getBeat();
        if (getBarRange().contains(bar))
        {
            SongPart spt = getSong().getSongStructure().getSongPart(bar);
            LongRange sptTickRange = getSptTickRange(spt);
            IntRange sptBarRange = getSptBarRange(spt);
            int relativeBar = bar - sptBarRange.from;
            float relativeNbBeats = (relativeBar * spt.getRhythm().getTimeSignature().getNbNaturalBeats()) + beat;
            tick = sptTickRange.from + (long) (relativeNbBeats * MidiConst.PPQ_RESOLUTION);
            tick -= getTickRange().from;
        }
        return tick;
    }


    /**
     * Converts a tick position relative to this context into an absolute SongStructure Position.
     *
     * @param relativeTick 0 for the start of this context bar range.
     * @return Null if tick is out of the bounds of this context.
     */
    default Position toPosition(long relativeTick)
    {
        FloatRange br = getBeatRange();
        float absPosInBeats = toPositionInBeats(relativeTick);
        if (!br.contains(absPosInBeats, true))
        {
            return null;
        }
        return getSong().getSongStructure().toPosition(absPosInBeats);
    }

    /**
     * Converts a tick position relative to this context into an absolute SongStructure position in beats.
     *
     * @param relativeTick 0 for the start of this context bar range.
     * @return -1 if tick is out of the bounds of this context.
     */
    default float toPositionInBeats(long relativeTick)
    {
        FloatRange br = getBeatRange();
        float res = br.from + (float) relativeTick / MidiConst.PPQ_RESOLUTION;
        if (!br.contains(res, true))
        {
            res = -1;
        }
        return res;
    }
}
