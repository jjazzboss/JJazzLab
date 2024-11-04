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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jjazz.test.walkingbass.WbpSource;

/**
 * Tile a WbpTiling object using selected WbpSourceAdaptations from a BestWbpsaStore.
 */
public class TilerMostCompatibleFirst
{

    private final BestWbpsaStore bWpsaStore;
    /**
     * Count how many time a WbpSource is used.
     */
    private final Map<WbpSource, Integer> mapSourceCount;
    private final WbpTiling tiling;

    public TilerMostCompatibleFirst(WbpTiling tiling, BestWbpsaStore store)
    {
        this.tiling = tiling;
        this.bWpsaStore = store;
        this.mapSourceCount = new HashMap<>();
    }

    /**
     * Tile the most compatible WbpSourceAdaptations first while avoiding using a same WbpSource twice. Then repeat with less compatible WbpSourceAdaptations,
     * until it's not possible to tile anything.<p>
     * <p>
     */
    public void tile()
    {
        boolean tiled;

        do
        {
            tiled = false;
            resetWbpSourceCount();

            for (int rank = 0; rank < WbpTiling.MAX_NB_BEST_ADAPTATIONS; rank++)
            {
                List<WbpSourceAdaptation> wbpsasOrderedByScore = bWpsaStore.getWbpSourceAdaptationsRanked(rank);
                Collections.sort(wbpsasOrderedByScore);         // Descending score
                for (WbpSourceAdaptation wbpsa : wbpsasOrderedByScore)
                {
                    if (wbpsa == null)
                    {
                        continue;
                    }
                    WbpSource wbpSource = wbpsa.getWbpSource();
                    if (tiling.isUsableAndFree(wbpsa.getBarRange()) && getWbpSourceCount(wbpSource) == 0)
                    {
                        tiling.add(wbpsa);
                        increaseWbpSourceCount(wbpSource);
                        tiled = true;
                    }
                }
            }
        } while (tiled);

    }

    private void resetWbpSourceCount()
    {
        mapSourceCount.clear();
    }

    /**
     * Get how many times a WbpSource has been used.
     *
     * @param wbpSource
     * @return
     */
    private int getWbpSourceCount(WbpSource wbpSource)
    {
        Integer count = mapSourceCount.get(wbpSource);
        return count == null ? 0 : count;
    }

    private void increaseWbpSourceCount(WbpSource wbpSource)
    {
        Integer count = mapSourceCount.get(wbpSource);
        if (count == null)
        {
            count = 0;
        }
        mapSourceCount.put(wbpSource, count + 1);
    }

}
