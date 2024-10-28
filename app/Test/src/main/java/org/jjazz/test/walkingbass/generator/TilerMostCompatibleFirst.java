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
 * Do the tiling from WbpSourceAdaptations proposed in the bWpsaStore.
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
     * Strategy is to place the most compatible WbpSourceAdaptations first, then continue with remaining WbpSourceAdaptations, until it's not possible to tile
     * any remaining WbpSourceAdatation from bestWbpsas.<p>
     * <p>
     */
    public void tile()
    {
        // Start with the best adaptations
        List<WbpSourceAdaptation> wbpsasRank0OrderedByScore = bWpsaStore.getWbpSourceAdaptationsRanked(0);
        Collections.sort(wbpsasRank0OrderedByScore);        // Descending score
        for (WbpSourceAdaptation wbpsa : wbpsasRank0OrderedByScore)
        {
            if (wbpsa == null)
            {
                continue;
            }
            WbpSource wbpSource = wbpsa.getWbpSource();
            if (tiling.isUsableAndFree(wbpsa.getBarRange()) && !usedWbpSourceMoreThan(wbpSource, 0))
            {
                tiling.add(wbpsa);
                increaseWbpSourceCount(wbpSource);
            }
        }
        
        // 2nd best adaptations
        List<WbpSourceAdaptation> wbpsasRank1OrderedByScore = bWpsaStore.getWbpSourceAdaptationsRanked(1);
        Collections.sort(wbpsasRank1OrderedByScore);
        for (WbpSourceAdaptation wbpsa : wbpsasRank1OrderedByScore)
        {
            if (wbpsa == null)
            {
                continue;
            }
            WbpSource wbpSource = wbpsa.getWbpSource();
            if (tiling.isUsableAndFree(wbpsa.getBarRange()) && !usedWbpSourceMoreThan(wbpSource, 3))
            {
                tiling.add(wbpsa);
                increaseWbpSourceCount(wbpSource);
            }
        }
        
        // 3rd best adaptations
        List<WbpSourceAdaptation> wbpsasRank2OrderedByScore = bWpsaStore.getWbpSourceAdaptationsRanked(2);
        Collections.sort(wbpsasRank2OrderedByScore);
        for (WbpSourceAdaptation wbpsa : wbpsasRank2OrderedByScore)
        {
            if (wbpsa == null)
            {
                continue;
            }
            WbpSource wbpSource = wbpsa.getWbpSource();
            if (tiling.isUsableAndFree(wbpsa.getBarRange()) && !usedWbpSourceMoreThan(wbpSource, 5))
            {
                tiling.add(wbpsa);
                increaseWbpSourceCount(wbpSource);
            }
        }
    }

    private boolean usedWbpSourceMoreThan(WbpSource wbpSource, int nbUses)
    {
        java.lang.Integer count = mapSourceCount.get(wbpSource);
        return count == null ? false : count > nbUses;
    }

    private void increaseWbpSourceCount(WbpSource wbpSource)
    {
        java.lang.Integer count = mapSourceCount.get(wbpSource);
        if (count == null)
        {
            count = 0;
        }
        mapSourceCount.put(wbpSource, count + 1);
    }

}
