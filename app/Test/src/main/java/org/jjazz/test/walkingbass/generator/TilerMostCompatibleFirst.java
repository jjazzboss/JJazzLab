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
 * Tile the most compatible WbpSourceAdaptations first while avoiding using a same WbpSource twice.
 * <p>
 * Repeat with less compatible WbpSourceAdaptations, until it's not possible to tile anything.<p>
 * <p>
 */
public class TilerMostCompatibleFirst implements Tiler
{

    public static final int MAX_NB_BEST_ADAPTATIONS = 5;
    /**
     * Count how many time a WbpSource is used.
     */
    private final Map<WbpSource, Integer> mapSourceCount;
    private final int size;
    

    public TilerMostCompatibleFirst(int size)
    {
        this.size= size;
        this.mapSourceCount = new HashMap<>();
    }


    @Override
    public void tile(WbpTiling tiling)
    {
        var scse = tiling.getSimpleChordSequenceExt();
        BestWbpsaStoreOLD store = new BestWbpsaStoreOLD(scse, scse.getUsableBars(), size, MAX_NB_BEST_ADAPTATIONS);

        boolean tiled;

        do
        {
            tiled = false;
            resetWbpSourceCount();

            for (int rank = 0; rank < MAX_NB_BEST_ADAPTATIONS; rank++)
            {
                List<WbpSourceAdaptation> wbpsasOrderedByScore = store.getWbpSourceAdaptationsRanked(rank);
                Collections.sort(wbpsasOrderedByScore);         // Descending score
                for (WbpSourceAdaptation wbpsa : wbpsasOrderedByScore)
                {
                    if (wbpsa == null)
                    {
                        continue;
                    }
                    WbpSource wbpSource = wbpsa.getWbpSource();
                    if (tiling.isUsableAndFree(wbpsa.getBarRange()) && mapSourceCount.getOrDefault(wbpSource, 0) == 0)
                    {
                        tiling.add(wbpsa);
                        increaseWbpSourceCount(wbpSource);
                        tiled = true;
                    }
                }
            }
        } while (tiled);

    }

    // ===============================================================================================================
    // Private methods
    // ===============================================================================================================

    private void resetWbpSourceCount()
    {
        mapSourceCount.clear();
    }


    private void increaseWbpSourceCount(WbpSource wbpSource)
    {
        Integer count = mapSourceCount.getOrDefault(wbpSource, 0);
        mapSourceCount.put(wbpSource, count + 1);
    }

}
