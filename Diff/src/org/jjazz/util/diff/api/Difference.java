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
package org.jjazz.util.diff.api;

/**
 * Represents a difference, as used in <code>Diff</code>. A difference consists of two pairs of starting and ending points, each
 * pair representing either the "from" or the "to" collection passed to <code>Diff</code>. If an ending point is -1, then the
 * difference was either a deletion or an addition. For example, if <code>getDeletedEnd()</code> returns -1, then the difference
 * represents an addition.
 * <p>
 * JJAZZ Changes: minor usability changes.
 */
public class Difference
{

    public static final int NONE = -1;

    public enum ResultType
    {
        CHANGED, ADDED, DELETED
    };

    /**
     * The type of this Difference.
     */
    private ResultType type;

    /**
     * The point at which the deletion starts.
     */
    private int fromStart = NONE;
    /**
     * The point at which the deletion ends.
     */
    private int fromEnd = NONE;
    /**
     * The nb of deleted chars.
     */
    private int fromRange;
    /**
     * The point at which the addition starts.
     */
    private int toStart = NONE;
    /**
     * The point at which the addition ends.
     */
    private int toEnd = NONE;
    /**
     * The nb of added chars.
     */
    private int toRange;

    /**
     * Creates the difference for the given start and end points for the deletion and addition.
     */
    public Difference(int delStart, int delEnd, int addStart, int addEnd)
    {
        this.fromStart = delStart;
        this.fromEnd = delEnd;
        this.toStart = addStart;
        this.toEnd = addEnd;
        updateCalculatedValues();
    }

    public int getFromRange()
    {
        return fromRange;
    }

    public int getToRange()
    {
        return toRange;
    }

    public ResultType getType()
    {
        return type;
    }

    /**
     * The point at which the deletion starts, if any. A value equal to <code>NONE</code> means this is an addition.
     */
    public int getDeletedStart()
    {
        return fromStart;
    }

    /**
     * The point at which the deletion ends, if any. A value equal to <code>NONE</code> means this is an addition.
     */
    public int getDeletedEnd()
    {
        return fromEnd;
    }

    /**
     * The point at which the addition starts, if any. A value equal to <code>NONE</code> means this must be an addition.
     */
    public int getAddedStart()
    {
        return toStart;
    }

    /**
     * The point at which the addition ends, if any. A value equal to <code>NONE</code> means this must be an addition.
     */
    public int getAddedEnd()
    {
        return toEnd;
    }

    /**
     * Sets the point as deleted. The start and end points will be modified to include the given line.
     */
    public void setDeleted(int line)
    {
        fromStart = Math.min(line, fromStart);
        fromEnd = Math.max(line, fromEnd);
        updateCalculatedValues();
    }

    /**
     * Sets the point as added. The start and end points will be modified to include the given line.
     */
    public void setAdded(int line)
    {
        toStart = Math.min(line, toStart);
        toEnd = Math.max(line, toEnd);
        updateCalculatedValues();
    }

    private void updateCalculatedValues()
    {
        if (fromEnd != NONE && toEnd != NONE)
        {
            type = ResultType.CHANGED;
        } else if (fromEnd == NONE)
        {
            type = ResultType.ADDED;
        } else
        {
            type = ResultType.DELETED;
        }
        fromRange = fromEnd - fromStart + 1;
        toRange = toEnd - toStart + 1;
    }

    /**
     * Compares this object to the other for equality. Both objects must be of type Difference, with the same starting and ending
     * points.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Difference)
        {
            Difference other = (Difference) obj;

            return (fromStart == other.fromStart
                    && fromEnd == other.fromEnd
                    && toStart == other.toStart
                    && toEnd == other.toEnd);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + this.fromStart;
        hash = 71 * hash + this.fromEnd;
        hash = 71 * hash + this.toStart;
        hash = 71 * hash + this.toEnd;
        return hash;
    }

    /**
     * Returns a string representation of this difference.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[del:" + fromStart + "," + fromEnd);
        buf.append(" ");
        buf.append("add:" + toStart + "," + toEnd + "]");
        return buf.toString();
    }
}
