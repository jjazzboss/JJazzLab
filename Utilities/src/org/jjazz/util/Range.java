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
package org.jjazz.util;

/**
 * A basic integer (zero or positive) interval.
 * <p>
 * This is an immutable class.
 */
public class Range
{

    /** The special shared instance for the empty range. */
    public static final Range EMPTY_RANGE = new VoidRange();
    public final int from, to;

    /**
     * A range representing [from; to].
     *
     * @param from Must be &gt;= 0
     * @param to   Must be &gt;= from
     */
    public Range(int from, int to)
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
    public boolean isIn(int x)
    {
        return this == EMPTY_RANGE ? false : x >= from && x <= to;
    }

    /**
     *
     * @param r
     * @return Can return the EMPTY_RANGE if no intersection.
     */
    public Range getIntersectRange(Range r)
    {
        if (!intersect(r))
        {
            return EMPTY_RANGE;
        }
        int maxFrom = Math.max(from, r.from);
        int minTo = Math.min(to, r.to);
        return new Range(maxFrom, minTo);
    }

    public boolean intersect(Range r)
    {
        return !(this == EMPTY_RANGE || r == EMPTY_RANGE || r.from > to || r.to < from);
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
        if (!(obj instanceof Range))
        {
            return false;
        }
        final Range other = (Range) obj;
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

    private static class VoidRange extends Range
    {

        private VoidRange()
        {
            super(-1, -1);
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
