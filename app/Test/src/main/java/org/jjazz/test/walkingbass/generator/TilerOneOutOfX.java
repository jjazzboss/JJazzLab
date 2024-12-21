/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.test.walkingbass.generator;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.jjazz.test.walkingbass.WbpSource;

/**
 * Tile in bar order, using a given WbpSource one out of X times.
 * <p>
 * When a 2/3/4-bar WbpSource is used, its "sub-WbpSources" (1/2/3 bars) are also considered used. Also, a WbpSource can't be used to cover more than
 * wbpSourceMaxCoveragePercentage of the tiling.
 */
public class TilerOneOutOfX implements Tiler
{

    private final float wbpSourceMaxCoveragePercentage;
    /**
     * How many times a WbpSource was used
     */
    private final Map<WbpSource, Integer> mapSourceCountUsed;
    /**
     * How many times a WbpSource was used or could have been used
     */
    private final Map<WbpSource, Integer> mapSourceCount;
    private final int x;


    /**
     *
     * @param wbpSourceMaxCoveragePercentage A value in the ]0;1] range
     * @param x                              Tile a given WbpSource one out of x times
     * @see #getWbpSourceMaxCoveragePercentage()
     */
    public TilerOneOutOfX(float wbpSourceMaxCoveragePercentage, int x)
    {
        Preconditions.checkArgument(wbpSourceMaxCoveragePercentage > 0 && wbpSourceMaxCoveragePercentage <= 1,
                "wbpSourceMaxCoveragePercent=%f", wbpSourceMaxCoveragePercentage);
        Preconditions.checkArgument(x > 0);
        this.wbpSourceMaxCoveragePercentage = wbpSourceMaxCoveragePercentage;
        this.x = x;
        this.mapSourceCountUsed = new HashMap<>();
        this.mapSourceCount = new HashMap<>();
    }

    @Override
    public void tile(WbpTiling tiling)
    {
        clearState();
        WbpsaStore store = new WbpsaStore(tiling.getSimpleChordSequenceExt());

        // TODO: Try starting with the bar containing the highest ranking, then circle the usableBars ?

        while (itBar.hasNext())
        {
            int bar = itBar.next();
            var wbpsas = store.getWbpSourceAdaptations(bar);

            // Use the first WbpSourceAdaptation which is usable
            for (var wbpsa : wbpsas)
            {
                var wbpSource = wbpsa.getWbpSource();
                if (canUse(wbpSource))      // Updates state
                {
                    tiling.add(wbpsa);

                    // Move to the next usable bar
                    for (int i = 0; i < store.getWbpSourceSize() - 1; i++)
                    {
                        itBar.next();       // Should never fail since BestWbpaStore stores a WbpSourceAdaptation only it has room to do so
                    }
                    break;
                }
            }
        }

    }

    /**
     * A given WbpSource can't be used to cover more than this percentage of the tiling.
     *
     * @return ]0;1]
     */
    public float getWbpSourceMaxCoveragePercentage()
    {
        return wbpSourceMaxCoveragePercentage;
    }

    public int getX()
    {
        return x;
    }

    // ====================================================================================================================
    // Private methods
    // ====================================================================================================================
    private void clearState()
    {
        wbpSourceCountMax = 0;
        mapSourceCountUsed.clear();
        mapSourceCount.clear();
    }

    /**
     * Returns true if use of wbpSource is one out of X, and WbpSourceMaxCoveragePercentage is respected.
     * <p>
     * If true is returned, state is changed because we assume the wbpSource *will* be used after the call.
     *
     * @param wbpSource
     * @return
     */
    private boolean canUse(WbpSource wbpSource)
    {
        boolean b = false;

        // Is it one out of X
        int count = mapSourceCount.getOrDefault(wbpSource, 0);
        if (count % x == 0)
        {
            // Now check that WbpSourceMaxCoveragePercentage is respected
            int countUsed = mapSourceCountUsed.getOrDefault(wbpSource, 0);
            b = (countUsed + 1) <= wbpSourceCountMax;
            mapSourceCountUsed.put(wbpSource, countUsed + 1);
        }

        mapSourceCount.put(wbpSource, count + 1);

        return b;
    }

}
