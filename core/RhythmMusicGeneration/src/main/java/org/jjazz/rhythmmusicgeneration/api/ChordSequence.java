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
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.IntRange;

/**
 * A convenience class to analyze and manipulate a suite of chord symbols extracted from a ChordLeadSheet, possibly with different TimeSignatures.
 * <p>
 */
public class ChordSequence extends TreeSet<CLI_ChordSymbol> implements Comparable<ChordSequence>, Cloneable
{

    private final IntRange barRange;

    private static final Logger LOGGER = Logger.getLogger(ChordSequence.class.getSimpleName());

    /**
     * Construct a chord sequence for the specified bar range.
     *
     * @param barRange
     */
    public ChordSequence(IntRange barRange)
    {
        checkNotNull(barRange);
        this.barRange = barRange;
    }

    /**
     * @return A shallow copy (CLI_ChordSymbols are not cloned).
     */
    @Override
    public ChordSequence clone()
    {
        ChordSequence cSeq = new ChordSequence(barRange);
        cSeq.addAll(this);
        return cSeq;
    }

    /**
     * The relative root ascending intervals of N chord symbols of this chord sequence.
     * <p>
     * Example: if chords=[Em,D7,F/A] then returned list is [10;3].
     *
     * @return A list with N-1 values in the range [0;11].
     */
    public List<Integer> getRootAscIntervals()
    {
        List<Integer> res = new ArrayList<>();
        for (var cliCs : this)
        {
            Note root = cliCs.getData().getRootNote();
            Note root2 = higher(cliCs).getData().getRootNote();
            int delta = root.getRelativeAscInterval(root2);
            res.add(delta);
        }
        return res;
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
        var item = CLI_ChordSymbol.createItemTo(pos, true);
        return floor(item);
    }

    /**
     * A new sub-sequence from this sequence.
     *
     * @param subRange           The range of the sub-sequence.
     * @param addInitChordSymbol If true, try to add an init chordsymbol if the resulting subsequence does not have one: reuse the last chord symbol before
     *                           subRange.from if any
     * @return
     * @throws IllegalArgumentException If subRange is not contained in this ChordSequence bar range
     */
    public ChordSequence subSequence(IntRange subRange, boolean addInitChordSymbol)
    {
        Preconditions.checkArgument(barRange.contains(subRange), "barRange=%s subRange=%s", barRange, subRange);

        ChordSequence cSeq = new ChordSequence(subRange);
        cSeq.addAll(subSet(CLI_ChordSymbol.createItemFrom(subRange.from), CLI_ChordSymbol.createItemTo(subRange.to)));

        if (addInitChordSymbol && !cSeq.hasChordAtBeginning())
        {
            var beforeChord = getLastBefore(new Position(subRange.from), false, cs -> true);
            if (beforeChord != null)
            {
                CLI_ChordSymbol newCs = cSeq.getInitCopy(beforeChord);
                cSeq.add(newCs);
            }
        }

        return cSeq;
    }

    /**
     * Get the first matching chord symbol whose position is after (or equal, if inclusive is true) posFrom.
     *
     * @param posFrom
     * @param inclusive
     * @param tester
     * @return
     */
    public CLI_ChordSymbol getFirstAfter(Position posFrom, boolean inclusive, Predicate<CLI_ChordSymbol> tester)
    {
        Preconditions.checkNotNull(posFrom);
        Preconditions.checkNotNull(tester);

        var headSet = headSet(CLI_ChordSymbol.createItemFrom(posFrom, inclusive),
                false);   // useless because of createItemFrom
        var res = headSet.stream().
                filter(cliCs -> tester.test(cliCs))
                .findFirst()
                .orElse(null);

        return res;
    }

    /**
     * Get the last matching chord symbol whose position is before (or equal, if inclusive is true) posTo.
     *
     * @param posTo
     * @param inclusive
     * @param tester
     * @return
     */
    public CLI_ChordSymbol getLastBefore(Position posTo, boolean inclusive, Predicate<CLI_ChordSymbol> tester)
    {
        Preconditions.checkNotNull(posTo);
        Preconditions.checkNotNull(tester);
        CLI_ChordSymbol res = null;

        var headSet = headSet(CLI_ChordSymbol.createItemTo(posTo, inclusive),
                false);   // useless because of createItemFrom
        var it = headSet.descendingIterator();
        while (it.hasNext())
        {
            var item = it.next();
            if (tester.test(item))
            {
                res = item;
                break;
            }
        }

        return res;
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
        Position pos = first().getPosition();
        return pos.getBar() == barRange.from && pos.isFirstBarBeat();
    }

    /**
     * Remove successive identical "standard" chord symbols.
     *
     * @return True if chord sequence was modified
     * @see ExtChordSymbol#isStandard()
     */
    public boolean removeRedundantStandardChords()
    {
        boolean changed = false;
        var it = iterator();
        ExtChordSymbol lastEcs = null;
        while (it.hasNext())
        {
            var ecs = it.next().getData();
            if (ecs.isStandard() && Objects.equals(lastEcs, ecs))
            {
                it.remove();
                changed = true;
            } else
            {
                lastEcs = ecs;
            }
        }
        return changed;
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
        return "<" + barRange + "-" + super.toString() + ">";
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
        Position newPos = new Position(barRange.from);
        ChordRenderingInfo newCri = new ChordRenderingInfo((EnumSet<Feature>) null, ecs.getRenderingInfo().getScaleInstance());
        ExtChordSymbol newEcs = ecs.getCopy(null, newCri, null, null);
        CLI_ChordSymbol res = CLI_Factory.getDefault().createChordSymbol(newEcs, newPos);
        return res;
    }

}
