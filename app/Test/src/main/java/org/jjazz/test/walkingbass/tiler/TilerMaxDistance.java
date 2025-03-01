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
package org.jjazz.test.walkingbass.tiler;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.test.walkingbass.api.WbpSource;
import org.jjazz.test.walkingbass.generator.Tiler;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;
import org.jjazz.test.walkingbass.generator.WbpTiling;
import org.jjazz.test.walkingbass.generator.WbpsaScorer;
import org.jjazz.test.walkingbass.generator.WbpsaStore;

/**
 * Tile in bar order the most compatible WbpSourceAdaptation whose WbpSource was never used or was used the furthest from current bar.
 * <p>
 */
public class TilerMaxDistance implements Tiler
{

    private final Set<WbpSource> usedWbpSources;
    private final int wbpsaStoreWidth;
    private final float randomizeWithinOverallScoreWindow;
    private final WbpsaScorer scorer;    
    private static final Logger LOGGER = Logger.getLogger(TilerMaxDistance.class.getSimpleName());

    public TilerMaxDistance(WbpsaScorer scorer, int wbpsaStoreWidth)
    {
        this(scorer, wbpsaStoreWidth, WbpsaStore.DEFAULT_RANDOMIZE_WITHIN_OVERALL_SCORE_WINDOW);
    }

    public TilerMaxDistance(WbpsaScorer scorer, int wbpsaStoreWidth, float randomizeWithinOverallScoreWindow)
    {
        this.usedWbpSources = new HashSet<>();
        this.wbpsaStoreWidth = wbpsaStoreWidth;
        this.randomizeWithinOverallScoreWindow = randomizeWithinOverallScoreWindow;
        this.scorer = scorer;
    }


    @Override
    public void tile(WbpTiling tiling)
    {
        LOGGER.log(Level.SEVERE, "tile() --");
        reset();

        WbpsaStore store = new WbpsaStore(tiling, wbpsaStoreWidth, scorer, randomizeWithinOverallScoreWindow);


        // LOGGER.log(Level.SEVERE, "tile() store=\n{0}", store.toDebugString(true));


        var nonTiledBars = tiling.getNonTiledBars();
        var itBar = nonTiledBars.iterator();

        while (itBar.hasNext())
        {
            int bar = itBar.next();
            var wbpsas = store.getWbpSourceAdaptations(bar);        // Might be partly shuffled
            if (wbpsas.isEmpty())
            {
                continue;
            }

            var wbpsa = select(tiling, wbpsas);
            if (wbpsa != null)
            {
                tiling.add(wbpsa);

                // Advance
                int wbpsaSize = wbpsa.getBarRange().size();
                while (wbpsaSize-- > 1)
                {
                    itBar.next();
                }
            }
        }

    }

    // ====================================================================================================================
    // Private methods
    // ====================================================================================================================
    private void reset()
    {
        usedWbpSources.clear();
    }

    /**
     * Select the first WbpSourceAdaptation in wbpsas whose WbpSource was never used, or was used at the furthest bar.
     *
     * @param tiling
     * @param wbpsas Can not be empty. All WbpSourceAdaptation must start at the same bar.
     * @return Can be null
     */
    private WbpSourceAdaptation select(WbpTiling tiling, List<WbpSourceAdaptation> wbpsas)
    {
        Preconditions.checkArgument(!wbpsas.isEmpty());

        WbpSourceAdaptation res = null;
        int maxMinDistance = -1;
        int wbpsaBar = wbpsas.get(0).getBarRange().from; 

        for (var wbpsa : wbpsas)
        {
            var usageBars = tiling.getStartBarIndexes(wbpsa.getWbpSource(), true);  // Use false for less possible redundancies
            
            if (usageBars.isEmpty())
            {
                res = wbpsa;    // not used before, use it now
                break;
            }

            // Used, find the largest min distance
            int minDistance = usageBars.stream()
                    .mapToInt(bar -> Math.abs(bar - wbpsaBar))
                    .min()
                    .orElseThrow();
            if (minDistance > maxMinDistance)
            {
                maxMinDistance = minDistance;
                res = wbpsa;
            } else if (minDistance == maxMinDistance)
            {
                // Randomly select to avoid repetitions
                var oldRes = res;
                res = Math.random() > 0.5 ? oldRes : wbpsa;
            } else
            {
                assert res != null;
            }
        }

        return res;
    }

}
