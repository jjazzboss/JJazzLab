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
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jjazz.utilities.api.IntRange;

/**
 * Which WbpSourceAdaptations (various sizes) cover which usable bars of a SimpleChordSequenceExt.
 * <p>
 */
public class WbpTiling
{
    
    public static final int MAX_NB_BEST_ADAPTATIONS = 5;
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
    
    public IntRange getBarRange()
    {
        return simpleChordSequenceExt.getBarRange();
    }

    /**
     * Do the tiling.
     */
    public void tile()
    {

        // For each bar, get the most compatible 4-bar WbpSources
        var store4 = new BestWbpsaStore(simpleChordSequenceExt, simpleChordSequenceExt.getUsableBars(), 4, MAX_NB_BEST_ADAPTATIONS);
        LOGGER.severe("store4:");
        store4.dump();
        // Tile with this store
        new TilerMostCompatibleFirst(this, store4).tile();
        LOGGER.log(Level.SEVERE, "tile() 4-bar:");
        LOGGER.log(Level.SEVERE, toMultiLineString());


        // For each remaining 3-bar zone, get the most compatible 3-bar WbpSource
        var usableBars3 = getNonTiledBars();
        if (!usableBars3.isEmpty())
        {
            var store3 = new BestWbpsaStore(simpleChordSequenceExt, usableBars3, 3, MAX_NB_BEST_ADAPTATIONS);
            LOGGER.severe("store3:");
            store3.dump();
            new TilerMostCompatibleFirst(this, store3).tile();
            LOGGER.log(Level.SEVERE, "tile() 3-bar:");
            LOGGER.log(Level.SEVERE, toMultiLineString());
        }


        // For each remaining 2-bar zone, get the most compatible 2-bar WbpSource
        var usableBars2 = getNonTiledBars();
        if (!usableBars2.isEmpty())
        {
            var store2 = new BestWbpsaStore(simpleChordSequenceExt, usableBars2, 2, MAX_NB_BEST_ADAPTATIONS);
            LOGGER.severe("store2:");
            store2.dump();
            new TilerMostCompatibleFirst(this, store2).tile();
            LOGGER.log(Level.SEVERE, "tile() 2-bar:");
            LOGGER.log(Level.SEVERE, toMultiLineString());
            
        }


        // For each remaining 1-bar zone, get the most compatible 1-bar WbpSource
        var usableBars1 = getNonTiledBars();
        if (!usableBars1.isEmpty())
        {
            var store1 = new BestWbpsaStore(simpleChordSequenceExt, usableBars1, 1, MAX_NB_BEST_ADAPTATIONS);
            LOGGER.severe("store1:");
            store1.dump();
            new TilerMostCompatibleFirst(this, store1).tile();
            LOGGER.log(Level.SEVERE, "tile() 1-bar:");
            LOGGER.log(Level.SEVERE, toMultiLineString());
        }
    }

    /**
     * Remove the specified WbpSourceAdaptation.
     * <p>
     * @param wbpsa
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
     * @param zoneSize 1 to 4 bars
     * @return
     */
    public List<Integer> getUntiledZonesStartBarIndexes(int zoneSize)
    {
        Preconditions.checkArgument(zoneSize >= 1 && zoneSize <= 4, "zoneSize=%d", zoneSize);
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
    
    public String toMultiLineString()
    {
        StringBuilder sb = new StringBuilder();
        for (int bar : simpleChordSequenceExt.getUsableBars())
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
            sb.append(" ").append(bar).append(": ").append(s).append("\n");
        }
        
        return sb.toString();
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

}
