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
package org.jjazz.jjswing.bass;

import com.google.common.base.Preconditions;
import org.jjazz.jjswing.bass.db.WbpSource;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.utilities.api.IntRange;

/**
 * Tile the longest and most compatible WbpSourceAdaptations first, using a given WbpSource only once.
 * <p>
 * When a 2/3/4-bar WbpSource is used, its "sub-WbpSources" (1/2/3 bars) are also considered used.
 */
public class TilerLongestFirstNoRepeat implements Tiler
{

    private static final int NB_RANKS = 5;
    private final Set<WbpSource> usedWbpSources;
    private final Predicate<WbpSourceAdaptation> wbpsaTester;
    private static final Logger LOGGER = Logger.getLogger(TilerLongestFirstNoRepeat.class.getSimpleName());

    /**
     *
     * @param wbpsaTester Ignore WbpSourceAdapatations which do not satisfy this predicate
     */
    public TilerLongestFirstNoRepeat(Predicate<WbpSourceAdaptation> wbpsaTester)
    {
        Objects.requireNonNull(wbpsaTester);
        this.usedWbpSources = new HashSet<>();
        this.wbpsaTester = wbpsaTester;
    }


    @Override
    public void tile(WbpTiling tiling, WbpsaStore store)
    {
        Objects.requireNonNull(tiling);
        Objects.requireNonNull(store);
        Preconditions.checkArgument(store.getTiling() == tiling, "tiling=%s  store.getTiling()=%s", tiling, store.getTiling());

        LOGGER.log(Level.FINE, "tile() --");
        reset();


        // LOGGER.log(Level.FINE, "tile() store=\n{0}", store.toDebugString(true));
        for (int size = WbpSourceDatabase.SIZE_MAX; size >= WbpSourceDatabase.SIZE_MIN; size--)
        {
            for (int rank = 0; rank < NB_RANKS; rank++)
            {
                for (int bar : tiling.getNonTiledBars())
                {
                    if (!tiling.isUsableAndFree(new IntRange(bar, bar + size - 1)))
                    {
                        continue;
                    }

                    var wbpsas = store.getWbpSourceAdaptations(bar, size);      // Might be partly shuffled
                    if (rank >= wbpsas.size())
                    {
                        continue;
                    }

                    var wbpsa = wbpsas.get(rank);
                    var wbpSource = wbpsa.getWbpSource();
                    if (wbpsaTester.test(wbpsa) && canUse(wbpSource))
                    {
                        tiling.add(wbpsa);
                        markAsUsed(wbpSource, false);     // use stric=false for now, switch back to true if it generates annoying redundancies 
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
     * Mark wbpSource and some of its related sources as used.
     * <p>
     * All related WbpSources are marked as used except, if strict is false, for some large WbpSources comparison (2-3, 2-4, 3-3, 3-4, 4-4) which have only 1
     * bar in common.
     *
     * @param wbpSource
     * @param strict    If true all related WbpSources are marked as used
     */
    private void markAsUsed(WbpSource wbpSource, boolean strict)
    {
        usedWbpSources.add(wbpSource);
        int size = wbpSource.getBarRange().size();
        var relatedWbpSources = WbpSourceDatabase.getInstance().getRelatedWbpSources(wbpSource);
        relatedWbpSources.stream()
                .filter(wbps -> strict
                || size == 1
                || wbps.getBarRange().size() == 1
                || (size == 2 && wbps.getBarRange().size() == 2)
                || wbpSource.getBarRange().getIntersection(wbps.getBarRange()).size() > 1)
                .forEach(wbps -> usedWbpSources.add(wbps));

    }

    public boolean canUse(WbpSource wbpSource)
    {
        return !usedWbpSources.contains(wbpSource);
    }

}
