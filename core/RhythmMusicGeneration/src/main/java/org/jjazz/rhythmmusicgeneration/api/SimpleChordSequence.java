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
package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A ChordSequence which has only one TimeSignature.
 * <p>
 * User is responsible to ensure CLI_ChordSymbols are added in the right position order, in the startBar/nbBars range, and are compatible with the
 * TimeSignature.
 */
public class SimpleChordSequence extends ChordSequence
{

    private TimeSignature timeSignature;

    public SimpleChordSequence(IntRange barRange, TimeSignature ts)
    {
        super(barRange);
        this.timeSignature = ts;
    }

    /**
     * Construct a SimpleChordSequence from a standard ChordSequence.
     *
     * @param cSeq All ChordSymbols are checked to be compatible with the specified TimeSignature.
     * @param ts
     */
    public SimpleChordSequence(ChordSequence cSeq, TimeSignature ts)
    {
        this(cSeq.getBarRange(), ts);

        // Add the ChordSymbols
        for (var cliCs : cSeq)
        {
            checkArgument(ts.checkBeat(cliCs.getPosition().getBeat()), "cliCs=%s", cliCs);
            add(cliCs);
        }
    }

    /**
     * @return A shallow copy (CLI_ChordSymbols are not cloned).
     */
    @Override
    public SimpleChordSequence clone()
    {
        SimpleChordSequence scs = new SimpleChordSequence(this, getTimeSignature());
        return scs;
    }

    /**
     * Create a SimpleChordSequence from a section of ChordLeadSheet.
     *
     * @param cls
     * @param section
     * @return
     */
    static public SimpleChordSequence of(ChordLeadSheet cls, CLI_Section section)
    {
        SimpleChordSequence res = new SimpleChordSequence(cls.getBarRange(section), section.getData().getTimeSignature());
        for (CLI_ChordSymbol cliCs : cls.getItems(section, CLI_ChordSymbol.class))
        {
            res.add(cliCs);
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 37 * hash + Objects.hashCode(this.timeSignature);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final SimpleChordSequence other = (SimpleChordSequence) obj;
        if (this.timeSignature != other.timeSignature)
        {
            return false;
        }
        return super.equals(other);
    }

    /**
     * Get a copy of this SimpleChordSequence shifted by barOffset.
     *
     * @param barOffset Must be &gt;= -getBarRange().from
     * @return
     */
    public SimpleChordSequence getShifted(int barOffset)
    {
        Preconditions.checkArgument(barOffset >= -getBarRange().from, "barOffset=%s this=%s", barOffset, this);

        IntRange barRange = getBarRange().getTransformed(barOffset);
        SimpleChordSequence res = new SimpleChordSequence(barRange, getTimeSignature());

        for (var cliCs : this)
        {
            Position pos = cliCs.getPosition();
            var newCliCs = (CLI_ChordSymbol) cliCs.getCopy(new Position(pos.getBar() + barOffset, pos.getBeat()));
            res.add(newCliCs);
        }
        return res;
    }

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }


    /**
     * The beat range of this SimpleChordSequence provided it starts at startBarPosInBeats.
     *
     * @param startBarPosInBeats
     * @return
     */
    public FloatRange getBeatRange(float startBarPosInBeats)
    {
        if (startBarPosInBeats < 0)
        {
            throw new IllegalArgumentException("startBarPosInBeats=" + startBarPosInBeats);
        }
        return new FloatRange(startBarPosInBeats, startBarPosInBeats + getBarRange().size() * timeSignature.getNbNaturalBeats());
    }

    /**
     * The beat range of a ChordSymbol, provided ChordSequence starts at startBarPosInBeats.
     *
     * @param cliCs              Must belong to this SimpleChordSequence
     * @param startBarPosInBeats
     * @return
     */
    public FloatRange getBeatRange(CLI_ChordSymbol cliCs, float startBarPosInBeats)
    {
        Preconditions.checkArgument(startBarPosInBeats >= 0, "%s", startBarPosInBeats);
        Preconditions.checkArgument(contains(cliCs), "cliCs=%s  this=%s", cliCs, this);

        var beatPos = toPositionInBeats(cliCs.getPosition(), startBarPosInBeats);
        FloatRange br = new FloatRange(beatPos, beatPos + getChordDuration(cliCs));

        return br;
    }

    
    /**
     * Return the specified chord duration in natural beats.
     * <p>
     * This is the duration until next chord or the end of the chordsequence.
     *
     * @param cliCs
     * @return
     */
    public float getChordDuration(CLI_ChordSymbol cliCs)
    {
        Preconditions.checkNotNull(cliCs);

        Position pos = cliCs.getPosition();
        Position nextPos = cliCs == last() ? new Position(getBarRange().to + 1) : higher(cliCs).getPosition();
        float duration = pos.getDuration(nextPos, timeSignature);

        return duration;
    }


    /**
     * Convert the specified position into an absolute position in natural beats.
     *
     * @param pos
     * @param startBarPosInBeats The start position of this chord sequence.
     * @return If pos is beyond the end of this ChordSequence, then returned value will also be beyond this ChordSequence.
     */
    public float toPositionInBeats(Position pos, float startBarPosInBeats)
    {
        Objects.requireNonNull(pos);
        Preconditions.checkArgument(startBarPosInBeats >= 0, "startBarPosInBeats=%s", startBarPosInBeats);

        float relPosInBeats = (pos.getBar() - getBarRange().from) * timeSignature.getNbNaturalBeats() + pos.getBeat();
        return startBarPosInBeats + relPosInBeats;
    }

    /**
     * Convert the specified position in beats into a Position.
     *
     * @param posInBeats
     * @param startBarPosInBeats The start position of this chord sequence.
     * @return If posInBeats is beyond the end of this ChordSequence, then returned value will also be beyond this ChordSequence.
     */
    public Position toPosition(float posInBeats, float startBarPosInBeats)
    {
        Preconditions.checkArgument(posInBeats >= 0 && posInBeats >= startBarPosInBeats, "posInBeats=%s startBarPosInBeats=%s", posInBeats, startBarPosInBeats);
        float relPosInBeats = posInBeats - startBarPosInBeats;
        int relBars = (int) Math.floor(relPosInBeats / getTimeSignature().getNbNaturalBeats());
        float beat = relPosInBeats - relBars * getTimeSignature().getNbNaturalBeats();
        Position pos = new Position(getBarRange().from + relBars, beat);
        return pos;
    }


    /**
     * For each bar, check if each chord position is in the corresponding in-bar beat range (excluding the upper bound).
     * <p>
     * For example in 4/4, in order to check there are 2 chords at start and middle of each bar, use:<br>
     * {@code isMatchingInBarBeatPositions(true, new FloatRange(0, 0.01f), new FloatRange(2f, 2.01f))}
     *
     * @param acceptEmptyBars If true, bars with no chords are ignored
     * @param inBarBeatRanges Can not be empty. Each beat range must have an exclusive upper bound.
     * @return
     * @see FloatRange#contains(float, boolean)
     */
    public boolean isMatchingInBarBeatPositions(boolean acceptEmptyBars, FloatRange... inBarBeatRanges)
    {
        Preconditions.checkArgument(inBarBeatRanges.length > 0);

        boolean b = true;

        for (var bar : getBarRange())
        {
            var chordBeatPositions = subSequence(new IntRange(bar, bar), false)
                    .stream()
                    .map(cliCs -> cliCs.getPosition().getBeat())
                    .toList();

            if (chordBeatPositions.isEmpty() && acceptEmptyBars)
            {
                continue;

            } else if (chordBeatPositions.size() != inBarBeatRanges.length)
            {
                b = false;
            } else
            {
                for (int i = 0; i < chordBeatPositions.size(); i++)
                {
                    if (!inBarBeatRanges[i].contains(chordBeatPositions.get(i), true))
                    {
                        b = false;
                        break;
                    }
                }
            }

            if (b == false)
            {
                break;
            }
        }

        return b;
    }

    /**
     * Overridden to return a SimpleChordSequence.
     *
     * @param subRange
     * @param addInitChordSymbol
     * @return
     */
    @Override
    public SimpleChordSequence subSequence(IntRange subRange, boolean addInitChordSymbol)
    {
        ChordSequence cSeq = super.subSequence(subRange, addInitChordSymbol);
        return new SimpleChordSequence(cSeq, timeSignature);
    }


    /**
     * Merge this SimpleChordSequence with scs into a new instance.
     * <p>
     * You might want to use removeRedundantChords() on the result.
     *
     * @param scs must have the same TimeSignature that this object.
     * @return A new SimpleChordSequence instance
     * @see ChordSequence#removeRedundantChords() 
     */
    public SimpleChordSequence getMerged(SimpleChordSequence scs)
    {
        Preconditions.checkArgument(scs.getTimeSignature() == timeSignature);
        IntRange newRange = scs.getBarRange().getUnion(getBarRange());
        SimpleChordSequence res = new SimpleChordSequence(newRange, timeSignature);
        res.addAll(this);
        res.addAll(scs);
        return res;
    }

}
