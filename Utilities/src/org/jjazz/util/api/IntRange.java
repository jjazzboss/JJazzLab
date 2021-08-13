/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.util.api;

/**
 * A basic integer (zero or positive) interval.
 * <p>
 * This is an immutable class.
 */
public class IntRange
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
     * @param to Must be &gt;= from
     */
    public IntRange(int from, int to)
    {
        if (from < 0 || from > to)
        {
            throw new IllegalArgumentException("from=" + from + " to=" + to);   //NOI18N
        }
        this.from = from;
        this.to = to;
    }

    public boolean isEmpty()
    {
        return this == EMPTY_RANGE;
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
     *
     * @param r
     * @return Can return the EMPTY_RANGE if no intersection.
     */
    public IntRange getIntersectRange(IntRange r)
    {
        if (!intersects(r))
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
    public boolean intersects(IntRange r)
    {
        return !(this == EMPTY_RANGE || r == EMPTY_RANGE || r.from > to || from > r.to);
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

    }
}
