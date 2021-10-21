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
package org.jjazz.harmony.api;

public enum SymbolicDuration
{
    UNKNOWN(0.0F),
    SIXTEENTH(0.25F),
    EIGHTH_TRIPLET(0.3333F),
    EIGHTH(0.5F),
    QUARTER_TRIPLET(0.6666F),
    EIGHTH_DOTTED(0.75F),
    QUARTER(1.0F),
    HALF_TRIPLET(1.3333F),
    QUARTER_DOTTED(1.5F),
    HALF(2.0F),
    WHOLE_TRIPLET(2.6666F),
    HALF_DOTTED(3.0F),
    WHOLE(4.0F),
    WHOLE_DOTTED(6.0F);
    private final float beatDuration;

    SymbolicDuration(float d)
    {
        if (d < 0)
        {
            throw new IllegalArgumentException("d=" + d);   //NOI18N
        }
        beatDuration = d;
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
