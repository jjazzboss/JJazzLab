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

import java.util.HashSet;
import java.util.Set;
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
 * Tile the longest and most compatible WbpSourceAdaptations first, using a given WbpSource maximum once.
 * <p>
 * When a 2/3/4-bar WbpSource is used, its "sub-WbpSources" (1/2/3 bars) are also considered used. Also, a WbpSource can't be used to cover more than
 * wbpSourceMaxCoveragePercentage of the tiling.
 */
public class TilerBestFirstNoRepeat implements Tiler
{


    private final Set<WbpSource> usedWbpSources;
    private final int wbpsaStoreWidth;
    private final float randomizeWithinOverallScoreWindow;
    private static final Logger LOGGER = Logger.getLogger(TilerBestFirstNoRepeat.class.getSimpleName());

    public TilerBestFirstNoRepeat(int wbpsaStoreWidth)
    {
        this(wbpsaStoreWidth, WbpsaStore.DEFAULT_RANDOMIZE_WITHIN_OVERALL_SCORE_WINDOW);
    }

    public TilerBestFirstNoRepeat(int wbpsaStoreWidth, float randomizeWithinOverallScoreWindow)
    {
        this.usedWbpSources = new HashSet<>();
        this.wbpsaStoreWidth = wbpsaStoreWidth;
        this.randomizeWithinOverallScoreWindow = randomizeWithinOverallScoreWindow;
    }


    @Override
    public void tile(WbpTiling tiling)
    {
        LOGGER.log(Level.SEVERE, "tile() --");
        reset();

        WbpsaScorer scorer = new DefaultWbpsaScorer(new TransposerPhraseAdapter());
        WbpsaStore store = new WbpsaStore(tiling, wbpsaStoreWidth, scorer, randomizeWithinOverallScoreWindow);


        LOGGER.log(Level.SEVERE, "tile() store=\n{0}", store.toDebugString(true));


        for (int size = WbpDatabase.SIZE_MAX; size >= WbpDatabase.SIZE_MIN; size--)
        {
            for (int rank = 0; rank < wbpsaStoreWidth; rank++)
            {
                var wbpsas = store.getWbpSourceAdaptationsRanked(rank, size);  // Partly ordered (randomization might has been applied)
                for (var wbpsa : wbpsas)
                {
                    WbpSource wbpSource = wbpsa.getWbpSource();
                    if (canUse(wbpSource) && tiling.isUsableAndFree(wbpsa.getBarRange()))
                    {
                        tiling.add(wbpsa);
                        markUsed(wbpSource);
                    }
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
     * Mark wbpSource and its related sources as used.
     *
     * @param wbpSource
     */
    private void markUsed(WbpSource wbpSource)
    {
        usedWbpSources.add(wbpSource);
        var relatedWbpSources = WbpDatabase.getInstance().getRelatedWbpSources(wbpSource);
        usedWbpSources.addAll(relatedWbpSources);
    }

    public boolean canUse(WbpSource wbpSource)
    {
        return !usedWbpSources.contains(wbpSource);
    }

}
