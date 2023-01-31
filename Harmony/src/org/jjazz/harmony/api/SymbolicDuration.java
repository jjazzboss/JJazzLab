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
    UNKNOWN(0.0F, "unknown"),
    SIXTEENTH_TRIPLET(0.16667F, "1/16 triplet"),
    SIXTEENTH(0.25F, "1/16"),
    EIGHTH_TRIPLET(0.33333F, "1/8 triplet"),
    EIGHTH(0.5F, "1/8"),
    QUARTER_TRIPLET(0.66667F, "1/4 triplet"),
    EIGHTH_DOTTED(0.75F, "1/8 dotted"),
    QUARTER(1.0F, "1/4"),
    HALF_TRIPLET(1.33333F, "1/2 triplet"),
    QUARTER_DOTTED(1.5F, "1/4 dotted"),
    HALF(2.0F, "2"),
    WHOLE_TRIPLET(2.66667F, "4 triplet"),
    HALF_DOTTED(3.0F, "2 dotted"),
    WHOLE(4.0F, "4"),
    WHOLE_DOTTED(6.0F, "4 dotted");
    private final float duration;
    private final String name;


    SymbolicDuration(float d, String name)
    {
        if (d < 0)
        {
            throw new IllegalArgumentException("d=" + d);   //NOI18N
        }
        duration = d;
        this.name = name;
    }

    /**
     * Get a human-readable name, e.g "1/4" for QUARTER, "1/8 triplet" for EIGHTH_TRIPLET.
     *
     * @return
     */
    public String getReadableName()
    {
        return name;
    }

    /**
     * For example 0.5f for EIGHTH.
     *
     * @return 
     */
    public float getDuration()
    {
        return duration;
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
            if (Float.floatToIntBits(sd.getDuration()) == Float.floatToIntBits(bd))
            {
                return sd;
            }
        }
        return SymbolicDuration.UNKNOWN;
    }
}
