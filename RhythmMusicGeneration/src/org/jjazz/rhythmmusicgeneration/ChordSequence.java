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
package org.jjazz.rhythmmusicgeneration;

import java.util.ArrayList;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.songstructure.api.SongStructure;

/*
 * A convenience class to manipulate chord symbols sequences.
 *
 * It is assumed that the time signature does not change within one chord sequence.
 *
 * User is responsible to ensure CLI_ChordSymbols are added in the right position order and in the startBar/nbBars range.
 */
public class ChordSequence extends ArrayList<CLI_ChordSymbol> implements Comparable<ChordSequence>, Cloneable
{

    private static int HASH_CODE_COUNTER = 0;
    private int hashCode;
    private int startBar;
    private int nbBars;

    public ChordSequence(int startBar, int nbBars)
    {
        super();
        if (startBar < 0 || nbBars < 0)
        {
            throw new IllegalArgumentException("startBar=" + startBar + " nbBars=" + nbBars);
        }
        this.startBar = startBar;
        this.nbBars = nbBars;
        hashCode = HASH_CODE_COUNTER++;
    }

    /**
     *
     * @return A shallow copy (CLI_ChordSymbols are not cloned).
     */
    @Override
    public ChordSequence clone()
    {
        ChordSequence cSeq = new ChordSequence(startBar, nbBars);
        for (CLI_ChordSymbol cs : this)
        {
            cSeq.add(cs);
        }
        return cSeq;
    }

    public final int getNbBars()
    {
        return nbBars;
    }

    public final int getStartBar()
    {
        return startBar;
    }

    /**
     * Override hashCode to use a constant for each object, ie it does not depend on ChordSequence contents.
     * <p>
     * This way ChordSequence objects can be used as HashMap keys.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * @param o
     * @return True if o==this;
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o)
    {
        return this == o;
    }

    /**
     * Return the duration in natural beats of the chord at specified index.
     * <p>
     * This is the duration until next chord or the end of the chordsequence.
     *
     * @param chordIndex
     * @return
     */
    public float getChordDuration(int chordIndex, TimeSignature ts)
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
            nextPos = new Position(startBar + nbBars, 0);
        } else
        {
            // Duration until next chord
            nextPos = get(chordIndex + 1).getPosition();
        }
        float duration = pos.getDuration(nextPos, ts);
        return duration;
    }

    /**
     * Get the CLI_ChordSymbol from this ChordSequence which is active at posInBeats for specified sgs.
     *
     * @param sgs
     * @param posInBeats
     * @return Can be null.
     */
    public CLI_ChordSymbol getChordSymbol(SongStructure sgs, float posInBeats)
    {
        for (int i = size() - 1; i >= 0; i--)
        {
            CLI_ChordSymbol cliCs = get(i);
            Position pos = cliCs.getPosition();
            float cliPosInBeats = sgs.getPositionInNaturalBeats(pos.getBar()) + pos.getBeat();
            if (posInBeats >= cliPosInBeats)
            {
                return cliCs;
            }
        }
        return null;
    }

    /**
     * A new sub-sequence from this sequence.
     *
     * @param startBar Chords from startBar are included
     * @param endBar Chords until endBar (included) are included
     * @return
     */
    public ChordSequence subSequence(int startBar, int endBar)
    {
        ChordSequence cSeq = new ChordSequence(startBar, endBar - startBar + 1);
        for (CLI_ChordSymbol cs : this)
        {
            int bar = cs.getPosition().getBar();
            if (bar < startBar)
            {
                continue;
            }
            if (bar > endBar)
            {
                break;
            }
            cSeq.add(cs);
        }
        return cSeq;
    }

    /**
     * Return the index of the first chord whose bar position is absoluteBarIndex or after.
     *
     * @param absoluteBarIndex
     * @return -1 if not found.
     */
    public int indexOfFirstFromBar(int absoluteBarIndex)
    {
        for (int i = 0; i < size(); i++)
        {
            if (get(i).getPosition().getBar() >= absoluteBarIndex)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the last chord (with absolute position) whose bar position is before absoluteBarIndex
     *
     * @param absoluteBarIndex Must be &gt;= 1.
     * @return -1 if no chord found before absoluteBarIndex.
     */
    public int indexOfLastBeforeBar(int absoluteBarIndex)
    {
        if (absoluteBarIndex < 1)
        {
            throw new IllegalArgumentException("absoluteBarIndex=" + absoluteBarIndex);
        }
        int index = -1;
        int firstIndex = indexOfFirstFromBar(absoluteBarIndex);
        if (isEmpty())
        {
            // Do nothing
        } else if (firstIndex == -1)
        {
            index = size() - 1;
        } else if (firstIndex > 0)
        {
            index = firstIndex - 1;
        } else
        {
            // Do nothing
        }
        return index;
    }

    /**
     *
     * @return True if there is a chord on bar=startBar, beat=0.
     */
    public final boolean hasChordAtBeginning()
    {
        if (isEmpty())
        {
            return false;
        }
        Position pos = get(0).getPosition();
        return pos.getBar() == startBar && pos.getBeat() == 0;
    }

    /**
     * Comparison is only based on startBar.
     *
     * @param cSeq
     * @return
     */
    @Override
    public int compareTo(ChordSequence cSeq)
    {
        return Integer.valueOf(startBar).compareTo(cSeq.startBar);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("cSeq<start=").append(startBar).append(", nbBars=").append(nbBars).append(":").append(super.toString()).append(">");
        return sb.toString();
    }

}
