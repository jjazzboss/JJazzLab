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

/**
 * A range between to position in beats.
 * <p>
 */
public class BeatRange 
{

    private float from;
    private float to;
    /**
     * The special shared instance for the empty range.
     */
    public static final BeatRange EMPTY_BEAT_RANGE = new VoidBeatRange();

    /**
     * A range representing [from; to].
     *
     * @param from Must be &gt;= 0
     * @param to   Must be &gt;= from
     */
    public BeatRange(float from, float to)
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
        return this == EMPTY_BEAT_RANGE;
    }

    /**
     * Get the size of the range.
     *
     * @return (to - from), or 0 for the EMPTY_RANGE.
     */
    public float size()
    {
        return this == EMPTY_BEAT_RANGE ? 0 : to - from;
    }

    /**
     * Check if value is within the range.
     *
     * @param x
     * @return
     */
    public boolean isIn(float x)
    {
        return this == EMPTY_BEAT_RANGE ? false : x >= from && x <= to;
    }

    private static class VoidBeatRange extends BeatRange
    {

        private VoidBeatRange()
        {
            super(Float.MAX_VALUE, Float.MAX_VALUE);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj == EMPTY_BEAT_RANGE;
        }

        @Override
        public int hashCode()
        {
            return -1;
        }

    }
}
