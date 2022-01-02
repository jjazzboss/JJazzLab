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
package org.jjazz.rhythmmusicgeneration.api;

import static com.google.common.base.Preconditions.checkArgument;
import java.util.Objects;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;

/**
 * A ChordSequence which has only one TimeSignature.
 * <p>
 * User is responsible to ensure CLI_ChordSymbols are added in the right position order, in the startBar/nbBars range, and are
 * compatible with the TimeSignature.
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
     * @param cSeq All ChordSymbols should be compatible with the specified TimeSignature.
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
     * Return the duration in natural beats of the chord at specified index.
     * <p>
     * This is the duration until next chord or the end of the chordsequence.
     *
     * @param chordIndex
     * @return
     */
    public float getChordDuration(int chordIndex)
    {
        if (chordIndex < 0 || chordIndex >= size())
        {
            throw new IllegalArgumentException("chordIndex=" + chordIndex);
        }
        Position pos = get(chordIndex).getPosition();
        Position nextPos;
        if (chordIndex == size() - 1)
        {
            // Duration until end of the sequence
            nextPos = new Position(getBarRange().to + 1, 0);
        } else
        {
            // Duration until next chord
            nextPos = get(chordIndex + 1).getPosition();
        }
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
        if (pos == null || startBarPosInBeats < 0)
        {
            throw new IllegalArgumentException("pos=" + pos + " startBarPosInBeats=" + startBarPosInBeats);   //NOI18N
        }
        float relPosInBeats = (pos.getBar() - getBarRange().from) * timeSignature.getNbNaturalBeats() + pos.getBeat();
        return startBarPosInBeats + relPosInBeats;
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

}
