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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.test.walkingbass.WbpDatabase;
import org.jjazz.test.walkingbass.WbpSource;

/**
 * Tile in bar order, using the longest and more compatible WbpSource one out of X times.
 * <p>
 * When a 2/3/4-bar WbpSource is used, its "sub-WbpSources" (1/2/3 bars) are also considered used. Also, a WbpSource can't be used to cover more than
 * wbpSourceMaxCoveragePercentage of the tiling.
 */
public class TilerOneOutOfX implements Tiler
{

    public static final int MAX_NB_BEST_ADAPTATIONS = 5;
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
    private static final Logger LOGGER = Logger.getLogger(TilerOneOutOfX.class.getSimpleName());

    /**
     * Create the tiler.
     *
     * @param wbpSourceMaxCoveragePercentage A value in the ]0;1] range. A given WbpSource can't be used to cover more than this percentage of the
     *                                       tiling.
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
        reset();

        WbpsaStore store = new WbpsaStore(tiling.getSimpleChordSequenceExt(), MAX_NB_BEST_ADAPTATIONS);


        var usableBars = tiling.getUsableBars();
        var itBar = usableBars.iterator();


        while (itBar.hasNext())
        {
            int bar = itBar.next();
            var wbpsas = getAllWbpsas(store, bar);
            if (wbpsas.isEmpty())
            {
                LOGGER.log(Level.WARNING, "tile() No wbpsa found for bar {0}", bar);
                continue;
            }

            for (var wbpsa : wbpsas)
            {
                var wbpSource = wbpsa.getWbpSource();
                if (canUse(tiling, wbpSource))          // Updates state
                {
                    tiling.add(wbpsa);

                    // Move to the next usable bar
                    for (int i = 0; i < wbpsa.getBarRange().size() - 1; i++)
                    {
                        itBar.next();           // Should never fail since WbpaStore stores a WbpSourceAdaptation only it has room to do so
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
    private void reset()
    {
        mapSourceCountUsed.clear();
        mapSourceCount.clear();
    }

    /**
     * Returns true if use of wbpSource is one out of X and WbpSourceMaxCoveragePercentage is respected.
     * <p>
     * If true is returned, state is changed because we assume the wbpSource *will* be used after the call. Also updates the "subWbpSources" of
     * WbpSource.
     *
     * @param tiling
     * @param wbpSource
     * @return
     */
    private boolean canUse(WbpTiling tiling, WbpSource wbpSource)
    {
        boolean b = false;
        int count = mapSourceCount.getOrDefault(wbpSource, 0);
        int countUsed = mapSourceCountUsed.getOrDefault(wbpSource, 0);

        if (isCoveragePercentageOk(tiling, wbpSource, countUsed + 1))
        {

            var subs = WbpDatabase.getInstance().getSubWbpSources(wbpSource, -1);

            if (count % x == 0)
            {
                // It is one use out of X
                b = true;
                mapSourceCountUsed.put(wbpSource, countUsed + 1);
                // Update the subs as well
                subs.forEach(w ->
                {
                    int c = mapSourceCountUsed.getOrDefault(w, 0);
                    mapSourceCountUsed.put(w, c + 1);
                });
            }

            // Count the fact we could have used it
            mapSourceCount.put(wbpSource, count + 1);
            subs.forEach(w ->
            {
                int c = mapSourceCount.getOrDefault(w, 0);
                mapSourceCount.put(w, c + 1);
            });
        }

        return b;
    }

    /**
     * Get all the possible WbpSourceAdaptations list for specified bar.
     * <p>
     * Ordered by descending size (from 4 bars to 1 bar) then descending compatibility order.
     *
     * @param bar Must be a usable bar
     * @return Can be empty. List contains maximum getNbBestMax() elements.
     */
    private List<WbpSourceAdaptation> getAllWbpsas(WbpsaStore store, int bar)
    {
        List<WbpSourceAdaptation> res = new ArrayList<>();
        for (int i = WbpsaStore.SIZE_MAX; i >= WbpsaStore.SIZE_MIN; i--)
        {
            var l = store.getWbpSourceAdaptations(bar, i);
            res.addAll(l);
        }
        return res;
    }

    /**
     * Check if coverage percentage is OK with nbOccurences x wbpSource.
     *
     * @param wbpSource
     * @param nbOccurences
     * @return
     */
    private boolean isCoveragePercentageOk(WbpTiling tiling, WbpSource wbpSource, int nbOccurences)
    {
        var p = (float) nbOccurences * wbpSource.getBarRange().size() / tiling.getUsableBars().size();
        return p <= wbpSourceMaxCoveragePercentage;
    }
}
