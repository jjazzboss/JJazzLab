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

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.util.api.IntRange;

/**
 * A convenience class to manipulate a suite of chord symbols extracted from a ChordLeadSheet, possibly with different
 * TimeSignatures.
 * <p>
 * User is responsible to ensure CLI_ChordSymbols are added in the right position order and in the bar range.
 *
 * @see org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence
 */
public class ChordSequence extends ArrayList<CLI_ChordSymbol> implements Comparable<ChordSequence>, Cloneable
{

    private final IntRange barRange;

    private static final Logger LOGGER = Logger.getLogger(ChordSequence.class.getSimpleName());

    public ChordSequence(IntRange barRange)
    {
        checkNotNull(barRange);
        this.barRange = barRange;
    }

    /**
     *
     * @return A shallow copy (CLI_ChordSymbols are not cloned).
     */
    @Override
    public ChordSequence clone()
    {
        ChordSequence cSeq = new ChordSequence(barRange);
        for (CLI_ChordSymbol cs : this)
        {
            cSeq.add(cs);
        }
        return cSeq;
    }

    public final IntRange getBarRange()
    {
        return barRange;
    }

    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 17 * hash + Objects.hashCode(barRange);
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
        final ChordSequence other = (ChordSequence) obj;
        if (!this.barRange.equals(other.barRange))
        {
            return false;
        }
        return super.equals(other);
    }


    /**
     * Get the CLI_ChordSymbol from this ChordSequence which is active at the specified position.
     *
     * @param pos
     * @return Can be null
     */
    public CLI_ChordSymbol getChordSymbol(Position pos)
    {
        for (int i = size() - 1; i >= 0; i--)
        {
            CLI_ChordSymbol cliCs = get(i);
            if (cliCs.getPosition().compareTo(pos) <= 0)
            {
                return cliCs;
            }
        }
        return null;
    }

    /**
     * A new sub-sequence from this sequence.
     *
     * @param subRange The range of the sub-sequence.
     * @param addInitChordSymbol If true, try to add an init chordsymbol if the resulting subsequence does not have one: reuse the
     * last chord symbol before subRange.from
     * @return
     */
    public ChordSequence subSequence(IntRange subRange, boolean addInitChordSymbol)
    {
        if (!barRange.contains(subRange))
        {
            throw new IllegalArgumentException("barRange=" + barRange + " subRange=" + subRange);
        }


        ChordSequence cSeq = new ChordSequence(subRange);


        for (CLI_ChordSymbol cs : this)
        {
            int bar = cs.getPosition().getBar();
            if (!subRange.contains(bar))
            {
                continue;
            }
            cSeq.add(cs);
        }

        if (subRange.from > barRange.from && (cSeq.isEmpty() || !cSeq.get(0).getPosition().equals(new Position(subRange.from, 0))))
        {
            int index = indexOfLastChordBeforeBar(subRange.from);
            if (index != -1)
            {
                CLI_ChordSymbol newCs = cSeq.getInitCopy(get(index));
                cSeq.add(0, newCs);
            }
        }
        return cSeq;
    }

    /**
     * Return the index of the first chord whose bar position is absoluteBarIndex or after.
     *
     * @param absoluteBarIndex
     * @return -1 if not found.
     */
    public int indexOfFirstChordFromBar(int absoluteBarIndex)
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
    public int indexOfLastChordBeforeBar(int absoluteBarIndex)
    {
        if (absoluteBarIndex < 1)
        {
            throw new IllegalArgumentException("absoluteBarIndex=" + absoluteBarIndex);   //NOI18N
        }
        int index = -1;
        int firstIndex = indexOfFirstChordFromBar(absoluteBarIndex);
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
        return pos.getBar() == barRange.from && pos.isFirstBarBeat();
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
        return Integer.valueOf(barRange.from).compareTo(cSeq.barRange.from);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("cSeq=<").append(barRange).append(super.toString()).append(">");
        return sb.toString();
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================
    /**
     * Prepare a copy of the specified CLI_ChordSymbol to be put at first bar/beat of this chord sequence.
     * <p>
     * As it is a copy, reset the ChordRenderingInfo keeping only the possible scale.
     *
     * @param cliCs
     * @return
     */
    protected CLI_ChordSymbol getInitCopy(CLI_ChordSymbol cliCs)
    {
        // Add a copy of the chord symbol
        ExtChordSymbol ecs = cliCs.getData();
        Position newPos = new Position(barRange.from, 0);
        ChordRenderingInfo newCri = new ChordRenderingInfo((EnumSet<Feature>) null, ecs.getRenderingInfo().getScaleInstance());
        ExtChordSymbol newEcs = new ExtChordSymbol(ecs, newCri, null, null);
        CLI_ChordSymbol res = CLI_Factory.getDefault().createChordSymbol(cliCs.getContainer(), newEcs, newPos);
        return res;
    }

}
