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
package org.jjazz.util.api;

/**
 * A range between 2 positive floats.
 */
public class FloatRange
{

    public float from;
    public float to;
    /**
     * The special shared instance for the empty range.
     * <p>
     * EMPTY_FLOAT_RANGE.size() returns 0.
     */
    public static final FloatRange EMPTY_FLOAT_RANGE = new VoidFloatRange();

    /**
     * A range representing [from; to].
     * <p>
     * Use the EMPTY_FLOAT_RANGE instance for empty range.
     *
     * @param from Must be &gt;= 0
     * @param to Must be &gt; from
     * @throws IllegalArgumentException If from==to
     */
    public FloatRange(float from, float to)
    {
        if (from < 0 || from >= to)
        {
            throw new IllegalArgumentException("from=" + from + " to=" + to);   //NOI18N
        }
        this.from = from;
        this.to = to;
    }

    public boolean isEmpty()
    {
        return this == EMPTY_FLOAT_RANGE;
    }

    /**
     * Get the size of the range.
     *
     * @return (to - from), or 0 for the EMPTY_RANGE.
     */
    public float size()
    {
        return this == EMPTY_FLOAT_RANGE ? 0 : to - from;
    }

    /**
     * Check if value is within the range.
     *
     * @param x
     * @param excludeUpperBound If true, return false if x == this.to.
     * @return
     */
    public boolean contains(float x, boolean excludeUpperBound)
    {
        if (this == EMPTY_FLOAT_RANGE)
        {
            return false;
        }
        return x >= from && (excludeUpperBound ? x < to : x <= to);
    }

    /**
     * Check if specified range is within this float range.
     *
     * @param fr
     * @param excludeUpperBound If true, fr.to must be &lt; this.to to be considered as contained.
     * @return
     */
    public boolean contains(FloatRange fr, boolean excludeUpperBound)
    {
        if (isEmpty() || fr.isEmpty())
        {
            return false;
        }
        return fr.from >= from && (excludeUpperBound ? fr.to < to : fr.from <= to);
    }

    /**
     *
     * @param rg
     * @return Can return the EMPTY_RANGE if no intersection.
     */
    public FloatRange getIntersectRange(FloatRange rg)
    {
        if (!intersect(rg))
        {
            return EMPTY_FLOAT_RANGE;
        }
        float maxFrom = Math.max(from, rg.from);
        float minTo = Math.min(to, rg.to);
        return new FloatRange(maxFrom, minTo);
    }

    /**
     * Get a new range with bounds modified.
     * <p>
     * Modifying the empty range returns the empty range.
     *
     * @param fromOffset
     * @param toOffset
     * @return
     */
    public FloatRange getTransformed(float fromOffset, float toOffset)
    {
        if (isEmpty())
        {
            return this;
        }
        return new FloatRange(from + fromOffset, to + toOffset);
    }

    public boolean intersect(FloatRange rg)
    {
        return !(this == EMPTY_FLOAT_RANGE || rg == EMPTY_FLOAT_RANGE || rg.from >= to || from >= rg.to);
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
        if (obj == null || (obj instanceof VoidFloatRange))
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
    public int hashCode()
    {
        int hash = 5;
        hash = 41 * hash + Float.floatToIntBits(this.from);
        hash = 41 * hash + Float.floatToIntBits(this.to);
        return hash;
    }

    private static class VoidFloatRange extends FloatRange
    {

        private VoidFloatRange()
        {
            super(Float.MAX_VALUE / 100, Float.MAX_VALUE);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj == EMPTY_FLOAT_RANGE;
        }

        @Override
        public int hashCode()
        {
            return -1;
        }

    }
}
