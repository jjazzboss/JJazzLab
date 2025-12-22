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
package org.jjazz.utilities.api;

/**
 * A range between 2 positive floats.
 */
public class FloatRange
{

    /**
     * Don't use negative value.
     */
    public float from;
    /**
     * Don't use negative value.
     */
    public float to;

    /**
     * The special shared instance for the empty range.
     * <p>
     * EMPTY_FLOAT_RANGE.size() returns 0.
     */
    public static final FloatRange EMPTY_FLOAT_RANGE = new VoidFloatRange();
    /**
     * The biggest FloatRange possible.
     */
    public static final FloatRange MAX_FLOAT_RANGE = new FloatRange(0, Float.MAX_VALUE);

    /**
     * A range representing [from; to].
     * <p>
     * Use the EMPTY_FLOAT_RANGE instance for empty range.
     *
     * @param from Must be &gt;= 0
     * @param to   Must be &gt; from
     * @throws IllegalArgumentException If from and to are not valid
     */
    public FloatRange(float from, float to)
    {
        if (from < 0 || from >= to)
        {
            throw new IllegalArgumentException("from=" + from + " to=" + to);
        }
        this.from = from;
        this.to = to;
    }

    @Override
    public FloatRange clone()
    {
        return new FloatRange(from, to);
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
        return isEmpty() ? 0 : to - from;
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
        return (to - from) / 2 + from;
    }

    /**
     * Get the value expressed as a percentage of the range.
     *
     * @param value
     * @return 0 for a value &lt;= from, 0.5 for getCenter(), 1 for a value &gt;= to.
     * @throws IllegalStateException If range is empty
     * @see #getCenter()
     */
    public float getPercentage(float value)
    {
        if (isEmpty())
        {
            throw new IllegalStateException("FloatRange is empty. value=" + value);
        }
        value = clamp(value, 0);
        return (value - from) / size();
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
        if (isEmpty())
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
        return fr.from >= from && (excludeUpperBound ? fr.to < to : fr.to <= to);
    }

    /**
     *
     * @param rg
     * @return Can return the EMPTY_RANGE if no intersection.
     */
    public FloatRange getIntersectRange(FloatRange rg)
    {
        if (!intersects(rg))
        {
            return EMPTY_FLOAT_RANGE;
        }
        float maxFrom = Math.max(from, rg.from);
        float minTo = Math.min(to, rg.to);
        return new FloatRange(maxFrom, minTo);
    }

    /**
     * Check if specified range intersects with this range.
     * <p>
     * Note: [2;4] and [4;7] does NOT intersect.
     *
     * @param rg
     * @return
     */
    public boolean intersects(FloatRange rg)
    {
        return !(isEmpty() || rg.isEmpty() || rg.from >= to || from >= rg.to);
    }

    /**
     * Clamps the value to fit in this range.
     * <p>
     * If value is &gt;= upper bound, return value is (upper bound - upperBoundDelta).
     *
     * @param value
     * @param upperBoundDelta Use 0 to clamp at the upper bound. If non zero, upperBoundDelta should be consistent with value and float precision (7 digits).
     * @return
     */
    public float clamp(float value, float upperBoundDelta)
    {
        float res = value;
        if (value < from)
        {
            res = from;
        } else if (value >= to)
        {
            res = to - upperBoundDelta;
        }
        return res;
    }

    /**
     * Get a new range with bounds modified.
     * <p>
     * Modifying the empty range returns the empty range.
     *
     * @param fromOffset
     * @param toOffset
     * @return
     * @throws IllegalArgumentException If adding fromOffset and toOffset result in non-valid range values.
     */
    public FloatRange getTransformed(float fromOffset, float toOffset)
    {
        if (isEmpty())
        {
            return this;
        }

        return new FloatRange(from + fromOffset, to + toOffset);
    }

    /**
     * Get a new range with bounds modified by adding offset.
     * <p>
     * Modifying the empty range returns the empty range.
     *
     * @param offset
     * @return
     * @throws IllegalArgumentException If adding soffset result in non-valid range values.
     */
    public FloatRange getTransformed(float offset)
    {
        return getTransformed(offset, offset);
    }

    /**
     * Return a copy of this FloatRange with a new "from" value.
     *
     * @param newFrom Must be stricly less thatn the "to" bound of this object.
     * @return
     */
    public FloatRange setFrom(float newFrom)
    {
        return new FloatRange(newFrom, to);
    }

    /**
     * Return a copy of this FloatRange with a new "to" value.
     *
     * @param newTo Must be stricly greater thatn the "from" bound of this object.
     * @return
     */
    public FloatRange setTo(float newTo)
    {
        return new FloatRange(from, newTo);
    }

    /**
     * Get a new range made from the lowest and highest bounds from this object and r.
     * <p>
     * If one of the range is empty, return the empty range.
     *
     * @param r
     * @return
     */
    public FloatRange getUnion(FloatRange r)
    {
        if (r == EMPTY_FLOAT_RANGE || this == EMPTY_FLOAT_RANGE)
        {
            return EMPTY_FLOAT_RANGE;
        }
        float low = Math.min(from, r.from);
        float high = Math.max(to, r.to);
        return new FloatRange(low, high);
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
        if (!(obj instanceof FloatRange))
        {
            return false;
        }
        final FloatRange other = (FloatRange) obj;
        if (Float.floatToIntBits(this.from) != Float.floatToIntBits(other.from))
        {
            return false;
        }
        if (Float.floatToIntBits(this.to) != Float.floatToIntBits(other.to))
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

        @Override
        public String toString()
        {
            return "EmptyRange";
        }

    }
}
