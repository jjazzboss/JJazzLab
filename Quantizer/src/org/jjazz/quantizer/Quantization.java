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
package org.jjazz.quantizer;

import java.util.List;

public enum Quantization
{
    HALF_BAR, // For example beat 0-2 for 4/4, 0-1.5 for a waltz
    BEAT, // This is the "natural beat", beat 0-1-2-3 for 4/4, every 3 eighth note in 12/8.
    HALF_BEAT,
    ONE_THIRD_BEAT,
    ONE_QUARTER_BEAT,
    OFF;    // No quantization

    /**
     *
     * @param value
     * @return If true, using Quantization.valueOf(value) will not throw an exception.
     */
    static public boolean isValidStringValue(String value)
    {
        for (Quantization q : values())
        {
            if (q.name().equals(value))
            {
                return true;
            }
        }
        return false;
    }
}
