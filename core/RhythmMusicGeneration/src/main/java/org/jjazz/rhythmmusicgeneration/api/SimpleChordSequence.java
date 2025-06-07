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
 * A ChordSequence which has only one TimeSignature and a start position in beats.
 * <p>
 * User is responsible to ensure CLI_ChordSymbols are added in the right position order, in the startBar/nbBars range, and are compatible with the
 * TimeSignature.
 */
public class SimpleChordSequence extends ChordSequence
{

    private final TimeSignature timeSignature;
    private float startBeatPosition;

    /**
     * Create an empty SimpleChordSequence.
     *
     * @param barRange
     * @param startBeatPosition The beat start position of the created SimpleChordSequence
     * @param ts
     */
    public SimpleChordSequence(IntRange barRange, float startBeatPosition, TimeSignature ts)
    {
        super(barRange);
        Objects.requireNonNull(ts);
        Preconditions.checkArgument(startBeatPosition >= 0, "startBeatPosition=%s", startBeatPosition);
        this.startBeatPosition = startBeatPosition;
        this.timeSignature = ts;
    }

    /**
     * Construct a SimpleChordSequence from a standard ChordSequence.
     *
     * @param cSeq              All ChordSymbols are checked to be compatible with the specified TimeSignature.
     * @param startBeatPosition The beat start position of the created SimpleChordSequence
     * @param ts
     */
    public SimpleChordSequence(ChordSequence cSeq, float startBeatPosition, TimeSignature ts)
    {
        this(cSeq.getBarRange(), startBeatPosition, ts);

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
        SimpleChordSequence scs = new SimpleChordSequence(this, this.startBeatPosition, this.timeSignature);
        scs.addAll(this);
        return scs;
    }

    /**
     * @return A deep copy with each CLI_ChordSymbol cloned.
     */
    public SimpleChordSequence deepClone()
    {
        SimpleChordSequence scs = new SimpleChordSequence(this, this.startBeatPosition, this.timeSignature);
        for (var cliCs : this)
        {
            scs.add((CLI_ChordSymbol) cliCs.getCopy(null, null));
        }
        return scs;
    }

    /**
     * Create a SimpleChordSequence starting at beat 0 from a section of ChordLeadSheet.
     *
     * @param cls
     * @param section
     * @return
     */
    static public SimpleChordSequence of(ChordLeadSheet cls, CLI_Section section)
    {
        SimpleChordSequence res = new SimpleChordSequence(cls.getBarRange(section), 0, section.getData().getTimeSignature());
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
        hash = 17 * hash + Objects.hashCode(this.timeSignature);
        hash = 17 * hash + Float.floatToIntBits(this.startBeatPosition);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!super.equals(obj))
        {
            return false;
        }
        final SimpleChordSequence other = (SimpleChordSequence) obj;
        if (Float.floatToIntBits(this.startBeatPosition) != Float.floatToIntBits(other.startBeatPosition))
        {
            return false;
        }
        return this.timeSignature == other.timeSignature;
    }


//    /**
//     * WARNING: this equals() makes assumptions on ChordSequence implementation.
//     *
//     * To be updated if ChordSequence state code is updated -which is unlikely to happen.
//     *
//     * @param obj
//     * @return
//     */
//    @Override
//    public boolean equals(Object obj)
//    {
//        if (this == obj)
//        {
//            return true;
//        }
//        if (obj == null)
//        {
//            return false;
//        }
//        if (getClass() != obj.getClass())
//        {
//            return false;
//        }
//        final SimpleChordSequence other = (SimpleChordSequence) obj;
//        if (Float.floatToIntBits(this.startBeatPosition) != Float.floatToIntBits(other.startBeatPosition))
//        {
//            return false;
//        }
//        if (this.timeSignature != other.timeSignature)
//        {
//            return false;
//        }
//        
//        // !! Code below depends on ChordSequence implementation !!
//        if (!getBarRange().equals(other.getBarRange()))
//        {
//            return false;
//        }
//        if (size() != other.size())
//        {
//            return false;
//        }
//        Iterator it = this.iterator();
//        Iterator itOther = other.iterator();
//        while (it.hasNext())
//        {
//            if (!it.next().equals(itOther.next()))
//            {
//                return false;
//            }
//        }
//        return true;
//    }
    /**
     * The start position in beats of the chord sequence.
     *
     * @return A value &gt;= 0
     */
    public float getStartBeatPosition()
    {
        return startBeatPosition;
    }

    /**
     * Set the start position in beats of the chord sequence.
     *
     * @param startBeatPosition Must be &gt;= 0
     */
    public void setStartBeatPosition(float startBeatPosition)
    {
        Preconditions.checkArgument(startBeatPosition >= 0, "startBeatPosition=%s", startBeatPosition);
        this.startBeatPosition = startBeatPosition;
    }

    /**
     * Get a copy of this SimpleChordSequence shifted by barOffset.
     *
     * @param barOffset Must be &gt;= -getBarRange().from
     * @return An instance with a start beat position set to 0
     */
    public SimpleChordSequence getShifted(int barOffset)
    {
        Preconditions.checkArgument(barOffset >= -getBarRange().from, "barOffset=%s this=%s", barOffset, this);

        IntRange barRange = getBarRange().getTransformed(barOffset);
        SimpleChordSequence res = new SimpleChordSequence(barRange, 0, getTimeSignature());

        for (var cliCs : this)
        {
            Position pos = cliCs.getPosition();
            var newCliCs = (CLI_ChordSymbol) cliCs.getCopy(null, new Position(pos.getBar() + barOffset, pos.getBeat()));
            res.add(newCliCs);
        }
        return res;
    }

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    /**
     * The beat range of this SimpleChordSequence.
     *
     * @return
     * @see #getStartBeatPosition()
     */
    public FloatRange getBeatRange()
    {
        return new FloatRange(startBeatPosition, startBeatPosition + getBarRange().size() * timeSignature.getNbNaturalBeats());
    }

    public float getPositionInBeats(CLI_ChordSymbol cliCs)
    {
        return toPositionInBeats(cliCs.getPosition());
    }

    /**
     * The beat range of a ChordSymbol of this SimpleChordSequence
     *
     * @param cliCs Must belong to this SimpleChordSequence
     * @return
     */
    public FloatRange getBeatRange(CLI_ChordSymbol cliCs)
    {
        Preconditions.checkArgument(contains(cliCs), "cliCs=%s  this=%s", cliCs, this);

        Position pos = cliCs.getPosition();
        Position nextPos = cliCs == last() ? new Position(getBarRange().to + 1) : higher(cliCs).getPosition();
        float duration = pos.getDuration(nextPos, timeSignature);
        var beatPos = toPositionInBeats(pos);
        FloatRange br = new FloatRange(beatPos, beatPos + duration);

        return br;
    }

    /**
     * Convert the specified position into an absolute position in natural beats.
     *
     * @param pos
     * @return If pos is beyond the end of this ChordSequence, then returned value will also be beyond this ChordSequence.
     * @see #getStartBeatPosition()
     */
    public float toPositionInBeats(Position pos)
    {
        Objects.requireNonNull(pos);
        float relPosInBeats = (pos.getBar() - getBarRange().from) * timeSignature.getNbNaturalBeats() + pos.getBeat();
        return startBeatPosition + relPosInBeats;
    }

    /**
     * Convert the specified position in beats into a Position.
     *
     * @param posInBeats
     * @return If posInBeats is beyond the end of this ChordSequence, then returned value will also be beyond this ChordSequence.
     * @throws IllegalArgumentException If posInBeats is &lt; than getStartBeatPosition()
     */
    public Position toPosition(float posInBeats)
    {
        Preconditions.checkArgument(posInBeats >= 0 && posInBeats >= startBeatPosition, "posInBeats=%s startBeatPosition=%s", posInBeats, startBeatPosition);
        float relPosInBeats = posInBeats - startBeatPosition;
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
     * Overridden to return a SimpleChordSequence with the appropriate startBeatPosition.
     *
     * @param subRange
     * @param addInitChordSymbol
     * @return
     */
    @Override
    public SimpleChordSequence subSequence(IntRange subRange, boolean addInitChordSymbol)
    {
        ChordSequence cSeq = super.subSequence(subRange, addInitChordSymbol);
        float newStartBeatPos = startBeatPosition + (subRange.from - getBarRange().from) * timeSignature.getNbNaturalBeats();
        return new SimpleChordSequence(cSeq, newStartBeatPos, timeSignature);
    }

}
