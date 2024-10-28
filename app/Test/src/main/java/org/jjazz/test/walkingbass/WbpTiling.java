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
package org.jjazz.test.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.Utilities;

/**
 * Which WbpSourceAdaptations (various sizes) cover which usable bars of a SimpleChordSequenceExt.
 * <p>
 */
class WbpTiling
{

    private final SimpleChordSequenceExt simpleChordSequenceExt;
    private final TreeMap<Integer, WbpSourceAdaptation> mapBarWbpsa;
    private final int barOffset;
    private static final Logger LOGGER = Logger.getLogger(WbpTiling.class.getSimpleName());

    public WbpTiling(SimpleChordSequenceExt scs)
    {
        this.simpleChordSequenceExt = scs;
        mapBarWbpsa = new TreeMap<>();
        barOffset = scs.getBarRange().from;
        initialize();
    }

    public SimpleChordSequenceExt getSimpleChordSequenceExt()
    {
        return simpleChordSequenceExt;
    }

    public IntRange getBarRange()
    {
        return simpleChordSequenceExt.getBarRange();
    }

    /**
     * Add a WbpSourceAdaptation.
     * <p>
     * @param wbpsa
     * @throws IllegalArgumentException If wbpsa's bar zone is not empty or usable.
     */
    public void add(WbpSourceAdaptation wbpsa)
    {
        Objects.requireNonNull(wbpsa);
        var br = wbpsa.getBarRange();
        if (!isUsableAndFree(br))
        {
            throw new IllegalArgumentException("Can not add " + wbpsa + ", bar zone is not free. this=" + this);
        }
        mapBarWbpsa.put(br.from, wbpsa);
    }

    /**
     * Remove the specified WbpSourceAdaptation.
     * <p>
     * @return The WbpSourceAdaptation that was removed or null
     */
    public WbpSourceAdaptation remove(WbpSourceAdaptation wbpsa)
    {
        var res = mapBarWbpsa.remove(wbpsa.getBarRange().from);
        return res;
    }

    /**
     * Remove any WbpSourceAdaptation at specified start bar.
     * <p>
     * @param startBar The start bar of the WbpSourceAdaptation
     * @return The WbpSourceAdaptation that was removed or null
     */
    public WbpSourceAdaptation remove(int startBar)
    {
        var res = mapBarWbpsa.remove(startBar);
        return res;
    }


    /**
     * Get all the WbpSourceAdaptations ordered by start bar.
     *
     * @return
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations()
    {
        return new ArrayList<>(mapBarWbpsa.values());
    }

    /**
     * Get all the WbpSourceAdaptations ordered by start bar which satisfy tester.
     *
     * @param tester
     * @return
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(Predicate<WbpSourceAdaptation> tester)
    {
        var res = mapBarWbpsa.values().stream()
            .filter(wbpsa -> tester.test(wbpsa))
            .toList();
        return res;
    }

    /**
     * Get the WbpSourceAdaptation starting at startBar.
     *
     * @param startBar
     * @return Might be null.
     */
    public WbpSourceAdaptation getWbpSourceAdaptationStartingAt(int startBar)
    {
        WbpSourceAdaptation res = mapBarWbpsa.get(startBar);
        return res;
    }

    /**
     * Get the WbpSourceAdaptation covering the specified bar.
     *
     * @param bar
     * @return Might be null. Note that returned WbpSourceAdaptation might start before bar.
     */
    public WbpSourceAdaptation getWbpSourceAdaptation(int bar)
    {
        WbpSourceAdaptation res = null;
        var floorEntry = mapBarWbpsa.floorEntry(bar);
        if (floorEntry != null)
        {
            res = floorEntry.getValue();
            if (!res.getBarRange().contains(bar))
            {
                res = null;
            }
        }
        return res;
    }


    /**
     * Check if all usable bars are covered by a WbpSourceAdaptation.
     *
     * @return
     */
    public boolean isCompletlyTiled()
    {
        boolean res = true;
        var barRange = getBarRange();
        int bar = barRange.from;

        for (var wbpsa : mapBarWbpsa.values())
        {
            var br = wbpsa.getBarRange();
            if (IntStream.range(bar, br.from).anyMatch(b -> simpleChordSequenceExt.isUsable(b)))
            {
                res = false;
                break;
            }
            bar = br.to + 1;
        }

        if (res)
        {
            res = !IntStream.rangeClosed(bar, barRange.to).anyMatch(b -> simpleChordSequenceExt.isUsable(b));
        }

        return res;
    }

    /**
     * Get all usable bars which are not tiled by a WbpSourceAdaptation.
     *
     * @return
     */
    public List<Integer> getNonTiledBars()
    {
        List<Integer> res = new ArrayList<>();
        var barRange = getBarRange();
        int bar = barRange.from;
        for (var wbpsa : mapBarWbpsa.values())
        {
            var br = wbpsa.getBarRange();
            IntStream.range(bar, br.from)
                .filter(b -> simpleChordSequenceExt.isUsable(b))
                .forEachOrdered(b -> res.add(b));
            bar = br.to + 1;
        }
        // Add the last range
        IntStream.rangeClosed(bar, barRange.to)
            .filter(b -> simpleChordSequenceExt.isUsable(b))
            .forEachOrdered(b -> res.add(b));
        return res;
    }

    /**
     * Get the start bar indexes of zones not covered by a WbpSourceAdaptation.
     *
     * @param zoneSize Typically 1, 2 or 4 bars.
     * @return
     */
    public List<Integer> getUntiledZonesStartBarIndexes(int zoneSize)
    {
        List<Integer> res = new ArrayList<>();

        var nonTiledUsableBars = getNonTiledBars();

        if (nonTiledUsableBars.isEmpty())
        {
            return res;
        }

        if (zoneSize == 1)
        {
            return nonTiledUsableBars;
        }

        for (int i = 0; i <= nonTiledUsableBars.size() - zoneSize; i++)
        {
            int bar = nonTiledUsableBars.get(i);
            boolean ok = true;
            for (int j = 1; j < zoneSize; j++)
            {
                if (nonTiledUsableBars.get(i + j) != bar + j)
                {
                    ok = false;
                    break;
                }
            }
            if (ok)
            {
                res.add(bar);
                i += zoneSize - 1;
            }
        }

        return res;
    }


    /**
     * Select WbpSourceAdaptations from BestWbpSourceAdaptations to tile this object..
     * <p>
     * Tile strategy is to maximize the number of tiled WbpSourceAdaptations used.
     *
     * @param bestWbpsas
     */
    public void tileMaximizeNbWbpSources(BestWbpSourceAdaptations bestWbpsas)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String toString()
    {
        return mapBarWbpsa.toString();
    }

    public String toMultiLineString()
    {
        return Utilities.toMultilineString(mapBarWbpsa);
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================
    /**
     * Tile a WbpSourceTiling object for scs.
     *
     * @param scs
     * @return
     */
    private void initialize()
    {

        final int MAX_NB_BEST_ADAPTATIONS = 4;

        // For each bar, get the most compatible 4-bar WbpSources
        var bestWbpsas4 = new BestWbpSourceAdaptations(simpleChordSequenceExt, simpleChordSequenceExt.getUsableBars(), 4, MAX_NB_BEST_ADAPTATIONS);
        new TilerMostCompatibleFirst(bestWbpsas4).tile();
        LOGGER.log(Level.SEVERE, "initialize() ----- 4-bar\n{0}", toString());


        // For each remaining 2-bar zone, get the most compatible 2-bar WbpSource
        var usableBars2 = getUntiledZonesStartBarIndexes(2);
        if (!usableBars2.isEmpty())
        {
            var bestWpsas2 = new BestWbpSourceAdaptations(simpleChordSequenceExt, usableBars2, 2, MAX_NB_BEST_ADAPTATIONS);
            new TilerMostCompatibleFirst(bestWpsas2).tile();
            LOGGER.log(Level.SEVERE, "getPhrase() ----- 2-bar \n{0}", toString());
        }


        // For each remaining 1-bar zone, get the most compatible 1-bar WbpSource
        var usableBars1 = getUntiledZonesStartBarIndexes(1);
        if (!usableBars1.isEmpty())
        {
            var bestWpsas1 = new BestWbpSourceAdaptations(simpleChordSequenceExt, usableBars1, 1, MAX_NB_BEST_ADAPTATIONS);
            new TilerMostCompatibleFirst(bestWpsas1).tile();
            LOGGER.log(Level.SEVERE, "getPhrase() ----- 1-bar \n{0}", toString());
        }
    }

    /**
     * Check if bar zone is usable and not covered yet by a WbpSourceAdaptation.
     *
     * @param bar
     * @param nbBars
     * @return
     */
    private boolean isUsableAndFree(IntRange barRange)
    {
        if (!simpleChordSequenceExt.isUsable(barRange))
        {
            return false;
        }
        var floorEntry = mapBarWbpsa.floorEntry(barRange.to);
        return floorEntry == null || !floorEntry.getValue().getBarRange().isIntersecting(barRange);
    }

    // ==========================================================================================================================
    // Inner classes
    // ==========================================================================================================================

    private class TilerMostCompatibleFirst
    {

        private final BestWbpSourceAdaptations bestWbpsas;
        /**
         * Count how many time a WbpSource is used.
         */
        private final Map<WbpSource, Integer> mapSourceCount;

        public TilerMostCompatibleFirst(BestWbpSourceAdaptations bestWbpsas)
        {
            this.bestWbpsas = bestWbpsas;
            this.mapSourceCount = new HashMap<>();
        }

        /**
         * Strategy is to place the most compatible WbpSourceAdaptations first, then continue with remaining WbpSourceAdaptations, until it's not
         * possible to tile any remaining WbpSourceAdatation from bestWbpsas.<p>
         * <p>
         */
        public void tile()
        {
            // First best adaptations
            var wbpsasRank0OrderedByScore = bestWbpsas.getWbpSourceAdaptationsRanked(0);
            wbpsasRank0OrderedByScore.sort((wbpsa1, wbpsa2) -> Float.compare(wbpsa1.getCompatibilityScore(), wbpsa2.getCompatibilityScore()));

            for (var wbpsa : wbpsasRank0OrderedByScore)
            {
                if (wbpsa == null)
                {
                    continue;
                }
                var wbpSource = wbpsa.getWbpSource();
                if (isUsableAndFree(wbpsa.getBarRange()) && !usedWbpSourceMoreThan(wbpSource, 0))
                {
                    add(wbpsa);
                    increaseWbpSourceCount(wbpSource);
                }
            }


            // 2nd best adaptations
            var wbpsasRank1OrderedByScore = bestWbpsas.getWbpSourceAdaptationsRanked(1);
            wbpsasRank1OrderedByScore.sort((wbpsa1, wbpsa2) -> Float.compare(wbpsa1.getCompatibilityScore(), wbpsa2.getCompatibilityScore()));
            for (var wbpsa : wbpsasRank1OrderedByScore)
            {
                if (wbpsa == null)
                {
                    continue;
                }
                var wbpSource = wbpsa.getWbpSource();
                if (isUsableAndFree(wbpsa.getBarRange()) && !usedWbpSourceMoreThan(wbpSource, 3))
                {
                    add(wbpsa);
                    increaseWbpSourceCount(wbpSource);
                }
            }


            // 3rd best adaptations
            var wbpsasRank2OrderedByScore = bestWbpsas.getWbpSourceAdaptationsRanked(2);
            wbpsasRank2OrderedByScore.sort((wbpsa1, wbpsa2) -> Float.compare(wbpsa1.getCompatibilityScore(), wbpsa2.getCompatibilityScore()));
            for (var wbpsa : wbpsasRank2OrderedByScore)
            {
                if (wbpsa == null)
                {
                    continue;
                }
                var wbpSource = wbpsa.getWbpSource();
                if (isUsableAndFree(wbpsa.getBarRange()) && !usedWbpSourceMoreThan(wbpSource, 5))
                {
                    add(wbpsa);
                    increaseWbpSourceCount(wbpSource);
                }
            }

        }

        private boolean usedWbpSourceMoreThan(WbpSource wbpSource, int nbUses)
        {
            var count = mapSourceCount.get(wbpSource);
            return count == null ? false : count > nbUses;
        }

        private void increaseWbpSourceCount(WbpSource wbpSource)
        {
            var count = mapSourceCount.get(wbpSource);
            if (count == null)
            {
                count = 0;
            }
            mapSourceCount.put(wbpSource, count + 1);
        }
    }

}
