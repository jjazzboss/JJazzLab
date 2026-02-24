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
package org.jjazz.phrase.api;

import static com.google.common.base.Preconditions.checkArgument;
import java.util.Arrays;

/**
 * Specify positions which appear repeatedly, for example the beat 1.5 of each 4/4 measure.
 */
public class CyclicPositions
{

    private final float positions[];
    private final float cycleOffsets[];
    private final float cycleSizes[];

    /**
     * Create an instance with one or more cyclic beat positions.
     * <p>
     * Example:<br>
     * pos=0.5f, startCycle=0, cycleSize=1, means positions 0.5, 1.5, 10.5 etc.<br>
     * pos=0.5f, startCycle=3, cycleSize=1, means positions 3.5, 4.5, 5.5 etc.<br>
     * pos=0.5f, starCycle=0, cycleSize=4, means positions 0.5, 4.5, 8.5 etc.<br>
     *
     * @param position Must be &lt; cycleSize
     * @param cycleOffset
     * @param cycleSize Must be &gt; 0
     * @param morePosStartSize Must be 3*n arguments: pos then cycleOffset then cycleSize.
     */
    public CyclicPositions(float position, float cycleOffset, float cycleSize, Float... morePosStartSize)
    {
        checkArgument(position >= 0 && position < cycleSize, "position=%s cycleOffset=%s, cycleSize=%s", position, cycleOffset, cycleSize);
        checkArgument(morePosStartSize.length % 3 == 0, "morePosStartSize=%s", Arrays.asList(morePosStartSize));


        int nbPos = 1 + morePosStartSize.length / 3;
        positions = new float[nbPos];
        cycleOffsets = new float[nbPos];
        cycleSizes = new float[nbPos];


        positions[0] = position;
        cycleOffsets[0] = cycleOffset;
        cycleSizes[0] = cycleSize;

        for (int i = 1; i < nbPos; i++)
        {
            positions[i] = morePosStartSize[(i - 1) * 3];
            cycleOffsets[i] = morePosStartSize[(i - 1) * 3 + 1];
            cycleSizes[i] = morePosStartSize[(i - 1) * 3 + 2];
            if (positions[i] < 0 || positions[i] >= cycleSizes[i])
            {
                throw new IllegalArgumentException("morePosStartSize=" + Arrays.asList(morePosStartSize));
            }
        }
    }


    /**
     * Test if the specified position matches one of the cyclic positions.
     *
     * @param pos
     * @param nearWindow Matches if pos is &gt;= (cyclic_position-nearWindow) AND pos &lt; (cyclic_position+nearWindow). If nearWindow is
     * 0, it matches if pos==cyclic_position.
     * @return
     */
    public boolean matches(float pos, float nearWindow)
    {
        checkArgument(nearWindow >= 0, "pos=%s nearWindows=%s", pos, nearWindow);

        boolean res = false;

        for (int i = 0; i < positions.length; i++)
        {
            int cycleIndex = (int) ((pos - cycleOffsets[i]) / cycleSizes[i]);
            if (cycleIndex < 0)
            {
                continue;
            }
            float inCyclePos = (pos - cycleOffsets[i]) - cycleSizes[i] * cycleIndex;

            if (inCyclePos >= positions[i] - nearWindow && (nearWindow == 0 || inCyclePos < positions[i] + nearWindow))
            {
                res = true;
                break;
            }
        }

        return res;
    }


}
