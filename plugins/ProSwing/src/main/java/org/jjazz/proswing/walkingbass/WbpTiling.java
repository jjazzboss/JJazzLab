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
package org.jjazz.proswing.walkingbass;

import org.jjazz.proswing.walkingbass.db.WbpSource;
import org.jjazz.proswing.walkingbass.db.WbpSourceDatabase;
import com.google.common.base.Preconditions;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Define which WbpSourceAdaptations (of various sizes) cover which usable bars of a SimpleChordSequenceExt.
 * <p>
 */
public class WbpTiling
{

    /**
     * An interface to create custom WbpSources.
     *
     * @see #buildMissingWbpSources(org.jjazz.proswing.walkingbass.WbpTiling.CustomWbpSourcesBuilder, java.lang.Integer...)
     */
    public interface CustomWbpSourcesBuilder
    {

        /**
         * Try to create WbpSources for scs.
         *
         * @param scs
         * @param targetPitch -1 if unknown
         * @return
         */
        List<WbpSource> build(SimpleChordSequence scs, int targetPitch);
    }

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
     * Build the resulting phrase by adapting and merging all the WbpSourceAdaptations.
     *
     * @param phraseAdapter 
     * @return A channel 0 phrase. Might be empty.
     */
    public Phrase buildPhrase(TransposerPhraseAdapter phraseAdapter)
    {
        Objects.requireNonNull(phraseAdapter);
        Phrase res = new Phrase(0);             
        
        for (var wbpsa : getWbpSourceAdaptations())
        {
            var p = wbpsa.getAdaptedPhrase();
            if (p == null)
            {
                p = phraseAdapter.getPhrase(wbpsa);
                wbpsa.setAdaptedPhrase(p);
            }
            LOGGER.log(Level.FINE, "buildPhrase() p={0}", p);
            res.add(p, false);
        }
        
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
     * Get the tiling's WbpSourceAdaptations whose WbpSource is wbpSource.
     *
     * @param wbpSource
     * @return A list ordered by ascending start bar
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(WbpSource wbpSource)
    {
        return getWbpSourceAdaptations(wbpsa -> wbpsa.getWbpSource() == wbpSource);
    }

    /**
     * Get the start bar indexes which directly or indirectly refer to wbpSource.
     * <p>
     * Direct reference: wbpSource is the WbpSource of a tiling's WbpSourceAdaptation.<br>
     * Indirect reference: wbpSource (1/2/3-bar) is a (enclosed or not) related WbpSource of a tiling's WbpSourceAdaptation (2/3/4-bar)<br>
     *
     * @param wbpSource
     * @param enclosedRelatedOnly If true, an indirect reference requires that wbpSource is an enclosed related WbpSource of a tiling's WbpSourceAdaptation
     * @return A list ordered by ascending start bar
     * @see WbpSourceDatabase#getRelatedWbpSources(org.jjazz.test.walkingbass.WbpSource)
     */
    public List<Integer> getStartBarIndexes(WbpSource wbpSource, boolean enclosedRelatedOnly)
    {
        List<Integer> res = new ArrayList<>();
        var wdb = WbpSourceDatabase.getInstance();
        var wbpsas = getWbpSourceAdaptations(wbpsa -> wbpsa.getWbpSource().getSessionId().equals(wbpSource.getSessionId()));
        var br = wbpSource.getBarRange();

        for (var wbpsa : wbpsas)
        {
            var ws = wbpsa.getWbpSource();
            var wsBr = wbpsa.getBarRange();
            int bar = -1;

            if (ws == wbpSource)
            {
                // Easy
                bar = wsBr.from;
            } else if (wsBr.size() > br.size() && wdb.getRelatedWbpSources(ws).contains(wbpSource))
            {
                // wbpSource is a related WbpSource of wbpsa, but might not be enclosed
                if (!enclosedRelatedOnly || wsBr.contains(br))
                {
                    bar = wsBr.from + wbpSource.getBarRangeInSession().from - ws.getBarRangeInSession().from;
                }
            }

            if (bar > -1)
            {
                res.add(bar);
            }
        }

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


    /**
     * For each chord sequence corresponding to an untiled zone, try to create WbpSources using wbpSourcesBuilder.
     * <p>
     *
     * @param wbpSourcesBuilder Generate a list (possibly empty) of WbpSources for a SimpleChordSequence.
     * @param size              The size in bars of the WbpSources to be created.
     * @return The built WbpSources
     */
    public List<WbpSource> buildMissingWbpSources(CustomWbpSourcesBuilder wbpSourcesBuilder, int size)
    {
        Objects.requireNonNull(wbpSourcesBuilder);
        Preconditions.checkArgument(WbpSourceDatabase.checkWbpSourceSize(size), "size=%s", size);

        List<WbpSource> res = new ArrayList<>();

        var startBars = getUntiledZonesStartBarIndexes(size);
        for (int startBar : startBars)
        {
            var br = new IntRange(startBar, startBar + size - 1);

            var subSeq = getSimpleChordSequenceExt().subSequence(br, true).getShifted(-startBar);
            var nextWbpsa = getWbpSourceAdaptationStartingAt(br.to + 1);
            int targetPitch = nextWbpsa != null ? nextWbpsa.getAdaptedTargetPitch() : -1;

            // Create the WbpSources
            var wbpSources = wbpSourcesBuilder.build(subSeq, targetPitch);
            res.addAll(wbpSources);
        }

        return res;
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
        for (int i = WbpSourceDatabase.SIZE_MAX; i >= WbpSourceDatabase.SIZE_MIN; i--)
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
                s = wbpsaStart.toString() + " " + "note0BeatShift=" + wbpsaStart.getWbpSource().getFirstNoteBeatShift();
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
