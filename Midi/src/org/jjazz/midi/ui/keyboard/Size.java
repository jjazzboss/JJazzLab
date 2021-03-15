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
package org.jjazz.midi.ui.keyboard;

public enum Size
{
    _128_KEYS(0, 127, 75), _88_KEYS(21, 108, 52), _76_KEYS(28, 103, 45), _61_KEYS(36, 96, 36), _49_KEYS(36, 84, 29), _37_KEYS(48, 84, 22);
    int lowPitch;
    int highPitch;
    int nbWhiteKeys;

    Size(int low, int high, int nbWhiteKeys)
    {
        this.lowPitch = low;
        this.highPitch = high;
        this.nbWhiteKeys = nbWhiteKeys;
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

    public void checkPitch(int pitch)
    {
        if (pitch < lowPitch || pitch > highPitch)
        {
            throw new IllegalArgumentException("pitch=" + pitch + " lowPitch=" + lowPitch + " highPitch=" + highPitch);
        }
    }
}
