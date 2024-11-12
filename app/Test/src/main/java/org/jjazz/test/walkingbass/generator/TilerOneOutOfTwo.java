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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jjazz.test.walkingbass.WbpSource;

/**
 * Tile in bar order, using a given WbpSource one out of two times.
 * <p>
 * A WbpSource can't be used to cover more than wbpSourceMaxCoveragePercent of the tiling.
 */
public class TilerOneOutOfTwo implements Tiler
{

    private final float wbpSourceMaxCoveragePercentage;
    private final Set<WbpSource> wbpSourcesToBeSkipped;
    private final Map<WbpSource, Integer> mapSourceCount;   // How many times a WbpSource has been used
    private int wbpSourceCountMax;

    /**
     *
     * @param wbpSourceMaxCoveragePercentage A value in the ]0;1] range
     * @see #getWbpSourceMaxCoveragePercentage()
     */
    public TilerOneOutOfTwo(float wbpSourceMaxCoveragePercentage)
    {
        Preconditions.checkArgument(wbpSourceMaxCoveragePercentage > 0 && wbpSourceMaxCoveragePercentage <= 1,
                "wbpSourceMaxCoveragePercent=%f", wbpSourceMaxCoveragePercentage);
        this.wbpSourceMaxCoveragePercentage = wbpSourceMaxCoveragePercentage;
        this.wbpSourcesToBeSkipped = new HashSet<>();
        this.mapSourceCount = new HashMap<>();
    }

    @Override
    public void tile(WbpTiling tiling, BestWbpsaStore store)
    {
        clearState();
        wbpSourceCountMax = (int) Math.max(tiling.getBarRange().size() * wbpSourceMaxCoveragePercentage / store.getWbpSourceSize(), 1);


        var usableBars = store.getUsableBars();
        var itBar = usableBars.iterator();

        // TODO: Try starting with the bar containing the highest ranking, then circle the usableBars ?

        while (itBar.hasNext())
        {
            int bar = itBar.next();
            var wbpsas = store.getWbpSourceAdaptations(bar);

            // Take first WbpSourceAdaptation which is usable 
            for (var wbpsa : wbpsas)
            {
                var wbpSource = wbpsa.getWbpSource();
                if (canUse(wbpSource))
                {
                    tiling.add(wbpsa);
                    markAsUsed(wbpSource);
                    // Move to next usable bar
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

    // ====================================================================================================================
    // Private methods
    // ====================================================================================================================
    private void clearState()
    {
        wbpSourcesToBeSkipped.clear();
        mapSourceCount.clear();
    }

    /**
     * Returns OK if wbpSource is not marked as "to be skipped" and WbpSourceMaxCoveragePercentage is respected.
     * <p>
     * If wbpSource was marked as "to be skipped" returns false and remove wbpSource from the "to be skipped" list.
     *
     * @param wbpSource
     * @return
     */
    private boolean canUse(WbpSource wbpSource)
    {
        boolean b = false;
        if (!wbpSourcesToBeSkipped.contains(wbpSource))
        {
            int count = mapSourceCount.getOrDefault(wbpSource, 0);
            b = (count + 1) <= wbpSourceCountMax;
        } else
        {
            // We skipped it once, don't skip for next time
            wbpSourcesToBeSkipped.remove(wbpSource);
        }
        return b;
    }

    /**
     * Update state after wbpSource was used for a tile.
     *
     * @param wbpSource
     */
    private void markAsUsed(WbpSource wbpSource)
    {
        if (!wbpSourcesToBeSkipped.add(wbpSource))
        {
            throw new IllegalStateException("wbpSource=" + wbpSource + " wbpSourcesToBeSkipped=" + wbpSourcesToBeSkipped);
        }
        int count = mapSourceCount.getOrDefault(wbpSource, 0);
        mapSourceCount.put(wbpSource, count + 1);

    }


}
