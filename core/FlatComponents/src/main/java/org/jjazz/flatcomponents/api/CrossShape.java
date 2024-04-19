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
package org.jjazz.flatcomponents.api;

import java.awt.Polygon;

/**
 * A cross shape like a '+'.
 */
public class CrossShape extends Polygon
{

    /**
     * Create a symetrical cross shape. Total side length is equals to (2*branchBigSide+branchSmallSide)
     *
     * @param branchBigSide
     * @param branchSmallSide
     */
    public CrossShape(int branchBigSide, int branchSmallSide)
    {
        if (branchBigSide < 1 || branchSmallSide < 1)
        {
            throw new IllegalArgumentException("branchBigSide=" + branchBigSide + " branchSmallSide=" + branchSmallSide);   
        }
        int v1 = branchBigSide;
        int v2 = branchBigSide + branchSmallSide;
        int v3 = branchBigSide + branchSmallSide + branchBigSide;
        int x[] =
        {
            0, v1, v1, v2, v2, v3, v3, v2, v2, v1, v1, 0
        };
        int y[] =
        {
            v1, v1, 0, 0, v1, v1, v2, v2, v3, v3, v2, v2
        };
        for (int i = 0; i < x.length; i++)
        {
            addPoint(x[i], y[i]);
        }
    }
}
