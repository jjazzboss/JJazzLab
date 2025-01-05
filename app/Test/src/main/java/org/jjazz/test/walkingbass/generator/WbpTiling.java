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
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jjazz.test.walkingbass.WbpDatabase;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.utilities.api.IntRange;

/**
 * Which WbpSourceAdaptations (various sizes) cover which usable bars of a SimpleChordSequenceExt.
 * <p>
 */
public class WbpTiling
{

    private final SimpleChordSequenceExt simpleChordSequenceExt;
    private final TreeMap<Integer, WbpSourceAdaptation> mapBarWbpsa;
    private static final Logger LOGGER = Logger.getLogger(WbpTiling.class.getSimpleName());


    public WbpTiling(SimpleChordSequenceExt scs)
    {
        this.simpleChordSequenceExt = scs;
        mapBarWbpsa = new TreeMap<>();

    }

    public SimpleChordSequenceExt getSimpleChordSequenceExt()
    {
        return simpleChordSequenceExt;
    }

    /**
     * Redirect to simpleChordSequenceExt.getUsableBars().
     *
     * @return
     */
    public List<Integer> getUsableBars()
    {
        return simpleChordSequenceExt.getUsableBars();
    }

    /**
     * Redirect to simpleChordSequenceExt.getBarRange().
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return simpleChordSequenceExt.getBarRange();
    }

    /**
     * Remove the specified WbpSourceAdaptation.
     * <p>
     * @param wbpsa
     * @return The WbpSourceAdaptation that was removed or null
     */
    public WbpSourceAdaptation remove(WbpSourceAdaptation wbpsa)
    {
        var res = remove(wbpsa.getBarRange().from);
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
     * Add a WbpSourceAdaptation.
     * <p>
     * @param wbpsa
     * @throws IllegalArgumentException If wbpsa's bar zone is not empty nor usable.
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
     * Get all the WbpSourceAdaptations ordered by start bar which use the specified WbpSource.
     *
     * @param wbpSource
     * @return
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(WbpSource wbpSource)
    {
        return getWbpSourceAdaptations(wbpsa -> wbpsa.getWbpSource() == wbpSource);
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
    public boolean isFullyTiled()
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
     * Get the bars tiled.
     *
     * @return
     */
    public List<Integer> getTiledBars()
    {
        var res = new ArrayList<>(simpleChordSequenceExt.getUsableBars());
        res.removeAll(getNonTiledBars());
        return res;
    }

    /**
     * Get the usable bars which are not tiled by a WbpSourceAdaptation.
     *
     * @return An ordered list (ascending)
     */
    public List<Integer> getNonTiledBars()
    {
        List<Integer> res = new ArrayList<>();
        var barRange = getBarRange();
        int bar = barRange.from;

        for (var wbpsa : mapBarWbpsa.values())
        {
            var br = wbpsa.getBarRange();
            var nonTiledBars = IntStream.range(bar, br.from)
                    .boxed()
                    .filter(b -> simpleChordSequenceExt.isUsable(b))
                    .toList();
            res.addAll(nonTiledBars);
            bar = br.to + 1;
        }

        var nonTiledBars = IntStream.rangeClosed(bar, barRange.to)
                .boxed()
                .filter(b -> simpleChordSequenceExt.isUsable(b))
                .toList();
        res.addAll(nonTiledBars);


        return res;
    }


    /**
     * Get the start bar indexes of zones not covered by a WbpSourceAdaptation.
     *
     * @param zoneSize 1 to 4 bars
     * @return
     */
    public List<Integer> getUntiledZonesStartBarIndexes(int zoneSize)
    {
        Preconditions.checkArgument(zoneSize >= 1 && zoneSize <= 4, "zoneSize=%s", zoneSize);
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
     * Check if bar zone is usable and not covered yet by a WbpSourceAdaptation.
     *
     * @param barRange
     * @return
     */
    public boolean isUsableAndFree(IntRange barRange)
    {
        if (!simpleChordSequenceExt.isUsable(barRange))
        {
            return false;
        }
        var floorEntry = mapBarWbpsa.floorEntry(barRange.to);
        return floorEntry == null || !floorEntry.getValue().getBarRange().isIntersecting(barRange);
    }

    @Override
    public String toString()
    {
        return mapBarWbpsa.toString();
    }

    /**
     * Display various stats about used WbpSources in this tiling.
     *
     * @return
     */
    public String toStatsString()
    {
        // Show how often each WbpSource is used, starting by the most used
        SortedSetMultimap<WbpSource, Integer> mapSourceBars = MultimapBuilder.hashKeys().treeSetValues().build();
        for (var wbpsa : mapBarWbpsa.values())
        {
            var source = wbpsa.getWbpSource();
            mapSourceBars.put(source, wbpsa.getBarRange().from);
        }


        // Sort sources per descending number of uses
        List<WbpSource> sortedSources = new ArrayList<>(mapSourceBars.keySet());
        sortedSources.sort((s1, s2) -> Integer.compare(mapSourceBars.get(s2).size(), mapSourceBars.get(s1).size()));


        StringBuilder sb = new StringBuilder();
        for (int i = WbpDatabase.SIZE_MAX; i >= WbpDatabase.SIZE_MIN; i--)
        {
            final int fi = i;
            var sizeList = sortedSources.stream()
                    .filter(s -> s.getBarRange().size() == fi)
                    .toList();
            sb.append(">>> ").append(sizeList.size()).append(" * ").append(i).append("-bar:\n");
            for (var source : sizeList)
            {
                sb.append(source.toString()).append(": ").append(mapSourceBars.get(source).toString()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Display which Wbpsa for which bar.
     *
     * @return
     */
    public String toMultiLineString()
    {
        StringBuilder sb = new StringBuilder();
        for (int bar : getUsableBars())
        {
            String s = "";
            var wbpsaStart = getWbpSourceAdaptationStartingAt(bar);
            if (wbpsaStart != null)
            {
                s = wbpsaStart.toString();
            } else if (getWbpSourceAdaptation(bar) != null)
            {
                s = "  (repeat)";
            }
            sb.append(" ").append(String.format("%1$03d", bar)).append(": ").append(s).append("\n");
        }

        return sb.toString();
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

}
