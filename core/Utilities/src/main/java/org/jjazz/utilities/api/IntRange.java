/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.utilities.api;

import com.google.common.base.Preconditions;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A basic integer (zero or positive) interval.
 * <p>
 * This is an immutable class.
 */
public class IntRange implements Iterable<Integer>
{

    /**
     * The special shared instance for the empty range.
     */
    public static final IntRange EMPTY_RANGE = new VoidRange();

    public final int from, to;

    /**
     * A range representing [from; to].
     *
     * @param from Must be &gt;= 0
     * @param to   Must be &gt;= from
     */
    public IntRange(int from, int to)
    {
        if (from < 0 || from > to)
        {
            throw new IllegalArgumentException("from=" + from + " to=" + to);
        }
        this.from = from;
        this.to = to;
    }

    /**
     * Create an IntRange from the first and last x position of the rectangle.
     *
     * @param r
     * @return An empty range if r.x is &lt; 0 or r.width &lt;= 0
     */
    static public IntRange ofX(Rectangle r)
    {
        var res = EMPTY_RANGE;
        if (r.x >= 0 && r.width > 0)
        {
            res = new IntRange(r.x, r.x + r.width - 1);
        }
        return res;
    }

    /**
     * Create an IntRange from the first and last x position of the rectangle.
     *
     * @param r
     * @return An empty range if r.y is &lt; 0 or r.height &lt;= 0
     */
    static public IntRange ofY(Rectangle r)
    {
        var res = EMPTY_RANGE;
        if (r.y >= 0 && r.height > 0)
        {
            res = new IntRange(r.y, r.y + r.height - 1);
        }
        return res;
    }

    /**
     * Merge adjacent IntRanges from a list.
     *
     * @param rhythmSectionRanges
     * @return
     */
    public static List<IntRange> merge(List<IntRange> rhythmSectionRanges)
    {
        Objects.requireNonNull(rhythmSectionRanges);
        List<IntRange> res = new ArrayList<>();

        if (rhythmSectionRanges.size() <= 1)
        {
            res.addAll(rhythmSectionRanges);
        } else
        {
            res.add(rhythmSectionRanges.get(0));
            for (int i = 1; i < rhythmSectionRanges.size(); i++)
            {
                var rLast = res.getLast();
                var r = rhythmSectionRanges.get(i);
                if (rLast.isAdjacent(r))
                {
                    res.set(res.size() - 1, rLast.getUnion(r));
                } else
                {
                    res.add(r);
                }
            }
        }
        return res;
    }

    public boolean isEmpty()
    {
        return this == EMPTY_RANGE;
    }

    public IntStream stream()
    {
        return IntStream.rangeClosed(from, to);
    }

    /**
     * Get the size of the range.
     *
     * @return (to - from + 1), or 0 for the EMPTY_RANGE.
     */
    public int size()
    {
        return this == EMPTY_RANGE ? 0 : to - from + 1;
    }

    /**
     * Check if value is within the range.
     *
     * @param x
     * @return
     */
    public boolean contains(int x)
    {
        return this == EMPTY_RANGE ? false : x >= from && x <= to;
    }

    /**
     *
     * Check if specified range is contained in this range.
     *
     * @param r
     * @return
     */
    public boolean contains(IntRange r)
    {
        return r != EMPTY_RANGE && contains(r.from) && contains(r.to);
    }

    /**
     * Test if r is adjacent to this range, e.g. [4;6] and [7;12] are adjacent.
     * <p>
     * If ranges intersect, there are not adjacent. If one of the range is empty, return false.
     *
     * @param r
     * @return
     */
    public boolean isAdjacent(IntRange r)
    {
        if (r == EMPTY_RANGE || this == EMPTY_RANGE)
        {
            return false;
        }
        if (isIntersecting(r))
        {
            return false;
        }
        return from < r.from ? to == r.from - 1 : r.to == from - 1;
    }

    /**
     * Get a new range made from the lowest and highest bounds from this object and r.
     * <p>
     * If one of the range is empty, return the empty range.
     *
     * @param r
     * @return
     */
    public IntRange getUnion(IntRange r)
    {
        if (r == EMPTY_RANGE || this == EMPTY_RANGE)
        {
            return EMPTY_RANGE;
        }
        int low = Math.min(from, r.from);
        int high = Math.max(to, r.to);
        return new IntRange(low, high);
    }

    /**
     * Extend the current range to include x.
     * <p>
     * If this range is empty, return the empty range.
     *
     * @param x
     * @return
     */
    public IntRange getUnion(int x)
    {
        Preconditions.checkArgument(x >= 0);
        return getUnion(new IntRange(x, x));
    }


    /**
     *
     * @param r
     * @return Can return the EMPTY_RANGE if no intersection.
     */
    public IntRange getIntersection(IntRange r)
    {
        if (!isIntersecting(r))
        {
            return EMPTY_RANGE;
        }
        int maxFrom = Math.max(from, r.from);
        int minTo = Math.min(to, r.to);
        return new IntRange(maxFrom, minTo);
    }

    /**
     * Check if specified range intersects with this range.
     * <p>
     * Note: [2;4] and [4;7] intersects.
     *
     * @param r
     * @return
     */
    public boolean isIntersecting(IntRange r)
    {
        return !(this == EMPTY_RANGE || r == EMPTY_RANGE || r.from > to || from > r.to);
    }

    /**
     * The center of the range.
     *
     * @return (to-from)/2 + from
     * @throws IllegalStateException If range is empty
     */
    public float getCenter()
    {
        if (isEmpty())
        {
            throw new IllegalStateException("FloatRange is empty.");
        }
        return (to - from) / 2f + from;
    }


    /**
     * Clamps the value to fit in this range.
     *
     * @param value
     * @return
     */
    public int clamp(int value)
    {
        int res = value;
        if (value < from)
        {
            res = from;
        } else if (value > to)
        {
            res = to;
        }
        return res;
    }

    /**
     * Get a new range with bounds modified.
     * <p>
     * If this object is the the empty range, just return the empty range.
     *
     * @param fromOffset
     * @param toOffset
     * @return
     */
    public IntRange getTransformed(int fromOffset, int toOffset)
    {
        if (isEmpty())
        {
            return this;
        } else
        {
            return new IntRange(from + fromOffset, to + toOffset);
        }
    }

    public IntRange getTransformed(int offset)
    {
        return getTransformed(offset, offset);
    }

    @Override
    public String toString()
    {
        return "[" + from + ";" + to + "]";
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 79 * hash + this.from;
        hash = 79 * hash + this.to;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || (obj instanceof VoidRange))
        {
            return false;
        }
        if (!(obj instanceof IntRange))
        {
            return false;
        }
        final IntRange other = (IntRange) obj;
        if (this.from != other.from)
        {
            return false;
        }
        if (this.to != other.to)
        {
            return false;
        }
        return true;
    }

    @Override
    public Iterator<Integer> iterator()
    {
        return new MyIterator();
    }

    private static class VoidRange extends IntRange
    {

        private VoidRange()
        {
            super(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj == EMPTY_RANGE;
        }

        @Override
        public int hashCode()
        {
            return -1;
        }

        @Override
        public String toString()
        {
            return "EmptyRange";
        }
    }

    private class MyIterator implements Iterator<Integer>
    {

        private int index = from - 1;

        @Override
        public boolean hasNext()
        {
            return index < to;
        }

        @Override
        public Integer next()
        {
            index++;
            return index;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("not supported");
        }
    }
}
