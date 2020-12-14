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
package org.jjazz.harmony;

public enum SymbolicDuration
{
    UNKNOWN(-1, 0.0F),
    SIXTEENTH(0, 0.25F),
    EIGHTH_TRIPLET(1, 0.33F),
    EIGHTH(2, 0.5F),
    QUARTER_TRIPLET(3, 0.66F),
    EIGHTH_DOTTED(4, 0.75F),
    QUARTER(5, 1.0F),
    HALF_TRIPLET(6, 1.33F),
    QUARTER_DOTTED(7, 1.5F),
    HALF(8, 2.0F),
    WHOLE_TRIPLET(9, 2.66F),
    HALF_DOTTED(10, 3.0F),
    WHOLE(11, 4.0F),
    WHOLE_DOTTED(12, 6.0F);
    private int index;
    private float beatDuration;

    SymbolicDuration(int ind, float d)
    {
        index = ind;
        if (d < 0)
        {
            throw new IllegalArgumentException("ind=" + ind + " d=" + d);   //NOI18N
        }
        beatDuration = d;
    }

    public int getIndex()
    {
        return index;
    }

    public float getBeatDuration()
    {
        return beatDuration;
    }

    public boolean isDotted()
    {
        return (this == EIGHTH_DOTTED) || (this == QUARTER_DOTTED) || (this == HALF_DOTTED) || (this == WHOLE_DOTTED);
    }

    public boolean isTriplet()
    {
        return (this == EIGHTH_TRIPLET) || (this == QUARTER_TRIPLET) || (this == HALF_TRIPLET) || (this == WHOLE_TRIPLET);
    }

    public static SymbolicDuration getSymbolicDuration(float bd)
    {
        for (SymbolicDuration sd : SymbolicDuration.values())
        {
            if (sd.getBeatDuration() == bd)
            {
                return sd;
            }
        }
        return SymbolicDuration.UNKNOWN;
    }
}
