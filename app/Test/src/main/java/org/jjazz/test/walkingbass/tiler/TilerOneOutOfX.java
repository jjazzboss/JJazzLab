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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.test.walkingbass.WbpDatabase;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.test.walkingbass.generator.DefaultWbpsaScorer;
import org.jjazz.test.walkingbass.generator.Tiler;
import org.jjazz.test.walkingbass.generator.TransposerPhraseAdapter;
import org.jjazz.test.walkingbass.generator.WbpTiling;
import org.jjazz.test.walkingbass.generator.WbpsaScorer;
import org.jjazz.test.walkingbass.generator.WbpsaStore;

/**
 * Tile in bar order, using the longest and more compatible WbpSource one out of X times.
 * <p>
 * When a 2/3/4-bar WbpSource is used, its "sub-WbpSources" (1/2/3 bars) are also considered used. Also, a WbpSource can't be used to cover more than
 * wbpSourceMaxCoveragePercentage of the tiling.
 */

public class TilerOneOutOfX implements Tiler
{

    public final int wbpsaStoreWidth;
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
     * @param wbpSourceMaxCoveragePercentage A value in the ]0;1] range. A given WbpSource can't be used to cover more than this percentage of the tiling.
     * @param x                              Tile a given WbpSource one out of x times
     * @param wbpaStoreWidth
     * @see #getWbpSourceMaxCoveragePercentage()
     */
    public TilerOneOutOfX(float wbpSourceMaxCoveragePercentage, int x, int wbpaStoreWidth)
    {
        Preconditions.checkArgument(wbpSourceMaxCoveragePercentage > 0 && wbpSourceMaxCoveragePercentage <= 1,
                "wbpSourceMaxCoveragePercent=%f", wbpSourceMaxCoveragePercentage);
        Preconditions.checkArgument(x > 0);
        this.wbpsaStoreWidth = wbpaStoreWidth;
        this.wbpSourceMaxCoveragePercentage = wbpSourceMaxCoveragePercentage;
        this.x = x;
        this.mapSourceCountUsed = new HashMap<>();
        this.mapSourceCount = new HashMap<>();
    }

    @Override
    public void tile(WbpTiling tiling)
    {
        reset();

        WbpsaScorer scorer = new DefaultWbpsaScorer(new TransposerPhraseAdapter());
        WbpsaStore store = new WbpsaStore(tiling, wbpsaStoreWidth, scorer, WbpsaStore.DEFAULT_RANDOMIZE_WITHIN_OVERALL_SCORE_WINDOW);
        LOGGER.log(Level.SEVERE, "tile() store=\n{0}", store.toDebugString(true));


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

            wbpsas.stream()
                    .filter(wbpsa -> canUse(tiling, wbpsa.getWbpSource(), bar))
                    .findFirst()
                    .ifPresent(wbpsa -> 
                    {
                        tiling.add(wbpsa);

                        // Move to the next usable bar
                        int wbpsaSize = wbpsa.getBarRange().size();
                        while (wbpsaSize-- > 1)
                        {
                            itBar.next();
                        }
                    });
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
     * If true is returned, state is changed because we assume the wbpSource *will* be used after the call. Also updates the "subWbpSources" of WbpSource.
     *
     * @param tiling
     * @param wbpSource
     * @param bar
     * @return
     */
    private boolean canUse(WbpTiling tiling, WbpSource wbpSource, int bar)
    {
        boolean b = false;
        int count = mapSourceCount.getOrDefault(wbpSource, 0);
        count++;
        int countUsed = mapSourceCountUsed.getOrDefault(wbpSource, 0);

        if (countUsed == 0 || isCoveragePercentageOk(tiling, wbpSource, countUsed + 1))
        {
            List<WbpSource> relSources = WbpDatabase.getInstance().getRelatedWbpSources(wbpSource);

            if ((count - 1) % x == 0)
            {
                // It is one use out of X
                b = true;
                countUsed++;
                mapSourceCountUsed.put(wbpSource, countUsed);

                LOGGER.log(Level.SEVERE, "canUse() bar={0} wbpSource USED count={1} countUsed={2} source={3}", new Object[]
                {
                    bar, count, countUsed, wbpSource
                });

                // Update the related sources as well
                relSources.forEach(w -> 
                {
                    int c = mapSourceCountUsed.getOrDefault(w, 0);
                    mapSourceCountUsed.put(w, c + 1);
                });
            } else
            {
                LOGGER.log(Level.SEVERE, "canUse() bar={0} wbpSource SKIPPED count={1} countUsed={2} source={3}", new Object[]
                {
                    bar, count, countUsed, wbpSource
                });
            }


            // Count the fact that we used it or we could have used it
            mapSourceCount.put(wbpSource, count);
            relSources.forEach(w -> 
            {
                int c = mapSourceCount.getOrDefault(w, 0);
                mapSourceCount.put(w, c + 1);
            });
        } else
        {
            LOGGER.log(Level.SEVERE, "canUse() bar={0} WbpSource COVERAGE limit exceeded countUsed={1} source={2}", new Object[]
            {
                bar, countUsed, wbpSource
            });
        }

        return b;
    }

    /**
     * Check if coverage percentage is OK with nbOccurences x wbpSource.
     *
     * @param tiling
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
