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
package org.jjazz.quantizer.api;

public enum Quantization
{
    HALF_BAR(), // For example beat 0-2 for 4/4, 0-1.5 for a waltz
    BEAT(0f, 1f), // This is the "natural beat", beat 0-1-2-3 for 4/4, every 3 eighth note in 12/8.
    HALF_BEAT(0f, 0.5f, 1f),
    ONE_THIRD_BEAT(0f, 1f / 3, 2f / 3, 1f),
    ONE_QUARTER_BEAT(0f, .25f, .5f, .75f, 1f),
    OFF();    // No quantization

    private final float[] beats;

    private Quantization(float... beats)
    {
        this.beats = beats;
    }

    /**
     * An array of the quantized values within a beat.
     * <p>
     * Example: [0, 1/3, 2/3, 1]
     *
     * @return An empty array for OFF and HALF_BAR.
     */
    public float[] getBeats()
    {
        return beats;
    }

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
