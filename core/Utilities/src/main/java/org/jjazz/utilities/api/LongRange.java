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

/**
 * A basic long integer (zero or positive) interval.
 * <p>
 * This is an immutable class.
 */
public class LongRange
{

    /**
     * The special shared instance for the empty range.
     */
    public static final LongRange EMPTY_LONG_RANGE = new VoidLongRange();
    public final long from, to;

    /**
     * A range representing [from; to].
     *
     * @param from Must be &gt;= 0
     * @param to Must be &gt;= from
     */
    public LongRange(long from, long to)
    {
        if (from < 0 || from > to)
        {
            throw new IllegalArgumentException("from=" + from + " to=" + to);   
        }
        this.from = from;
        this.to = to;
    }

    public boolean isEmpty()
    {
        return this == EMPTY_LONG_RANGE;
    }

    /**
     * Get the size of the range.
     *
     * @return (to - from + 1), or 0 for the EMPTY_LONG_RANGE.
     */
    public long size()
    {
        return this == EMPTY_LONG_RANGE ? 0 : to - from + 1;
    }

    /**
     * Check if value is within the range.
     *
     * @param x
     * @return
     */
    public boolean contains(long x)
    {
        return this == EMPTY_LONG_RANGE ? false : x >= from && x <= to;
    }

    /**
     *
     * Check if specified range is contained in this range.
     *
     * @param r
     * @return
     */
    public boolean contains(LongRange r)
    {
        return r != EMPTY_LONG_RANGE && contains(r.from) && contains(r.to);
    }

    /**
     *
     * @param r
     * @return Return the EMPTY_LONG_RANGE if no intersection.
     */
    public LongRange getIntersectRange(LongRange r)
    {
        if (!intersects(r))
        {
            return EMPTY_LONG_RANGE;
        }
        long maxFrom = Math.max(from, r.from);
        long minTo = Math.min(to, r.to);
        return new LongRange(maxFrom, minTo);
    }

    /**
     * Check if specified range intersects with this range.
     * <p>
     * Note: [2;4] and [4;7] intersects.
     *
     * @param r
     * @return
     */
    public boolean intersects(LongRange r)
    {
        return !(this == EMPTY_LONG_RANGE || r == EMPTY_LONG_RANGE || r.from > to || from > r.to);
    }

    /**
     * Get a new range with bounds modified by adding fromOffset and toOffset.
     * <p>
     * If this object is the the empty range, just return the empty range.
     *
     * @param fromOffset
     * @param toOffset
     * @return
     */
    public LongRange getTransformed(long fromOffset, long toOffset)
    {
        if (isEmpty())
        {
            return this;
        } else
        {
            return new LongRange(from + fromOffset, to + toOffset);
        }
    }

    /**
     * Get a new range with bounds modified by adding offset.
     * <p>
     * If this object is the the empty range, just return the empty range.
     *
     * @param offset
     *
     * @return
     */
    public LongRange getTransformed(long offset)
    {
        if (isEmpty())
        {
            return this;
        } else
        {
            return new LongRange(from + offset, to + offset);
        }
    }

    @Override
    public String toString()
    {
        return "[" + from + ";" + to + "]";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || (obj instanceof VoidLongRange))
        {
            return false;
        }
        if (!(obj instanceof LongRange))
        {
            return false;
        }
        final LongRange other = (LongRange) obj;
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
    public int hashCode()
    {
        int hash = 5;
        hash = 67 * hash + (int) (this.from ^ (this.from >>> 32));
        hash = 67 * hash + (int) (this.to ^ (this.to >>> 32));
        return hash;
    }

    private static class VoidLongRange extends LongRange
    {

        private VoidLongRange()
        {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj == EMPTY_LONG_RANGE;
        }

        @Override
        public int hashCode()
        {
            return -1;
        }

    }
}
