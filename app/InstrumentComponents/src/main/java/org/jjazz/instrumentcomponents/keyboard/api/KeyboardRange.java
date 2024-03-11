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
package org.jjazz.instrumentcomponents.keyboard.api;

/**
 * Define the size (number of keys) of a KeyboardComponent.
 *
 * @author Administrateur
 */
public class KeyboardRange
{

    public static final KeyboardRange _128_KEYS = new KeyboardRange(0, 127, 75);
    public static final KeyboardRange _88_KEYS = new KeyboardRange(21, 108, 52);
    public static final KeyboardRange _76_KEYS = new KeyboardRange(28, 103, 45);
    public static final KeyboardRange _61_KEYS = new KeyboardRange(36, 96, 36);
    public static final KeyboardRange _49_KEYS = new KeyboardRange(36, 84, 29);
    public static final KeyboardRange _37_KEYS = new KeyboardRange(48, 84, 22);
    public static final KeyboardRange BASS_KEYS = new KeyboardRange(12, 60, 29);
    public static final KeyboardRange DRUMS_KEYS = new KeyboardRange(24, 84, 36);

    int lowPitch;
    int highPitch;
    int nbWhiteKeys;

    /**
     * Should be at least 1 octave (8 white keys).
     *
     * @param low
     * @param high
     * @param nbWhiteKeys
     */
    KeyboardRange(int low, int high, int nbWhiteKeys)
    {
        if (low < 0 || high > 127 || (high - low) < 12 || nbWhiteKeys < 8)
        {
            throw new IllegalArgumentException("low=" + low + " high=" + high + " nbWhiteKeys=" + nbWhiteKeys);
        }
        this.lowPitch = low;
        this.highPitch = high;
        this.nbWhiteKeys = nbWhiteKeys;
    }

    /**
     * Get a bigger keyboard, or if too big reset size to the smallest one (3 octaves, keeping approximatively the same center
     * note)
     *
     * @return
     */
    public KeyboardRange next()
    {
        KeyboardRange res;
        int newLowPitch = lowPitch - 12;
        int newHighPitch = highPitch + 12;
        if (newLowPitch >= 0 && newHighPitch <= 127)
        {
            // Grow bigger
            res = new KeyboardRange(newLowPitch, newHighPitch, nbWhiteKeys + 14);
        } else
        {
            // Smallest = 3 octaves
            newLowPitch = lowPitch;
            newHighPitch = highPitch;
            int nbWKeys = nbWhiteKeys;
            while (newHighPitch - newLowPitch + 1 > 48)
            {
                newLowPitch += 12;
                newHighPitch -= 12;
                nbWKeys -= 14;
            }
            res = new KeyboardRange(newLowPitch, newHighPitch, nbWKeys);
        }
        return res;
    }

    public int getLowestPitch()
    {
        return lowPitch;
    }

    public int getHighestPitch()
    {
        return highPitch;
    }

    public int getNbKeys()
    {
        return highPitch - lowPitch + 1;
    }

    public int getNbWhiteKeys()
    {
        return nbWhiteKeys;
    }

    /**
     * Get the Midi pitch of the central C for this keyboard range.
     *
     * @return
     */
    public int getCentralC()
    {
        float p = (highPitch + lowPitch) / 2f;
        int o = (int) (p / 12);
        int cLow = o * 12;
        int cHigh = (o + 1) * 12;
        int res = (p - cLow) <= (cHigh - p) ? cLow : cHigh;
        return res;
    }

    /**
     *
     * @param pitch
     * @return True if pitch is valid for this keyboard range.
     */
    public boolean isValid(int pitch)
    {
        return pitch >= lowPitch && pitch <= highPitch;
    }
}
