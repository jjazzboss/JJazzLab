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

import org.jjazz.jjswing.bass.db.WbpSource;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import com.google.common.base.Preconditions;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Defines which WbpSourceAdaptation (of various sizes) covers which bar(s).
 * <p>
 */
public class WbpTiling
{

    /**
     * Property change event fired when a new WbpSourceAdaptation is added.
     * <p>
     * newValue=the added WbpSourceAdaptation.
     */
    private static final String PROP_WBPSA_ADDED = "PropWbpsaAdded";

    /**
     * An interface to create custom WbpSources.
     *
     * @see #buildMissingWbpSources(org.jjazz.jjswing.walkingbass.WbpTiling.CustomWbpSourcesBuilder, java.lang.Integer...)
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

    private final List<SimpleChordSequence> scsList;
    private final List<Integer> usableBars;
    private final WbpSourceAdaptation[] wbpsas;
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(WbpTiling.class.getSimpleName());

    /**
     * Create an empty WbpTiling for a list of SimpleChordSequence using the same style.
     *
     * @param scsList
     */
    public WbpTiling(List<SimpleChordSequence> scsList)
    {
        this.scsList = scsList;
        usableBars = scsList.stream()
            .map(scs -> scs.getBarRange())
            .flatMap(br -> br.stream().boxed())
            .toList();
        wbpsas = new WbpSourceAdaptation[usableBars.getLast() + 1];
    }

    /**
     * The list of SimpleChordSequences passed to constructor.
     *
     * @return
     */
    public List<SimpleChordSequence> getScsList()
    {
        return scsList;
    }

    /**
     * The bar indexes managed by this WbpTiling.
     *
     * @return
     */
    public List<Integer> getUsableBars()
    {
        return usableBars;
    }

    /**
     * Add a WbpSourceAdaptation.
     * <p>
     * Fire a PROP_WBPSA_ADDED change event.
     *
     * @param wbpsa
     * @throws IllegalArgumentException If wbpsa's bar zone is not empty nor usable.
     */
    public void add(WbpSourceAdaptation wbpsa)
    {
        Objects.requireNonNull(wbpsa);
        var br = wbpsa.getBarRange();
        Preconditions.checkArgument(isUsableAndFree(br), "wbpsa=%s  this=%s", wbpsa, this);
        wbpsas[br.from] = wbpsa;
        pcs.firePropertyChange(PROP_WBPSA_ADDED, null, wbpsa);
    }

    /**
     * Build the resulting phrase by adapting and merging all the WbpSourceAdaptations.
     *
     * @param phraseAdapter
     * @return A channel 0 phrase. Might be empty.
     */
    public Phrase buildPhrase(DefaultPhraseAdapter phraseAdapter)
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
        return getWbpSourceAdaptations(wbpsa -> true);
    }

    /**
     * Get all the WbpSourceAdaptations ordered by start bar which satisfy tester.
     *
     * @param tester
     * @return
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(Predicate<WbpSourceAdaptation> tester)
    {
        List<WbpSourceAdaptation> res = new ArrayList<>(usableBars.size());
        for (int bar : usableBars)
        {
            var wbpsa = wbpsas[bar];
            if (wbpsa != null && tester.test(wbpsa))
            {
                res.add(wbpsa);
            }
        }
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
     * @param enclosedRelatedOnly If true, an indirect reference requires that wbpSource is an enclosed related WbpSource of a tiling's
     * WbpSourceAdaptation
     * @return A list ordered by ascending start bar
     * @see WbpSourceDatabase#getRelatedWbpSources(org.jjazz.test.walkingbass.WbpSource)
     */
    public List<Integer> getStartBarIndexes(WbpSource wbpSource, boolean enclosedRelatedOnly)
    {
        List<Integer> res = new ArrayList<>();
        var wdb = WbpSourceDatabase.getInstance();
        var wbpsaList = getWbpSourceAdaptations(wbpsa -> wbpsa.getWbpSource().getSessionId().equals(wbpSource.getSessionId()));
        var br = wbpSource.getBarRange();

        for (var wbpsa : wbpsaList)
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
     * Get the WbpSourceAdaptation starting at bar.
     *
     * @param bar
     * @return Can be null (including if bar is covered by a WbpSourceAdaptation which starts on a previous bar)
     */
    public WbpSourceAdaptation getWbpSourceAdaptationStartingAt(int bar)
    {
        WbpSourceAdaptation res = usableBars.contains(bar) ? wbpsas[bar] : null;
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
        checkBarIsValid(bar);
        WbpSourceAdaptation res = null;
        int index = bar;
        int minIndex = Math.max(usableBars.get(0), bar - WbpSourceDatabase.SIZE_MAX + 1);
        do
        {
            res = wbpsas[index];
            index--;
        } while (res == null && index >= minIndex);

        if (res != null && !res.getBarRange().contains(bar))
        {
            res = null;
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
        return getTiledBars().size() == usableBars.size();
    }

    /**
     * Get the usable bars tiled.
     *
     * @return
     */
    public List<Integer> getTiledBars()
    {
        List<Integer> res = new ArrayList<>();

        var it = usableBars.iterator();
        while (it.hasNext())
        {
            int bar = it.next();
            var wbpsa = wbpsas[bar];
            if (wbpsa != null)
            {
                res.add(bar);
                for (int i = 1; i < wbpsa.getBarRange().size(); i++)
                {
                    res.add(bar + i);
                    it.next();
                }
            }
        }

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

        var it = usableBars.iterator();
        while (it.hasNext())
        {
            int bar = it.next();
            var wbpsa = wbpsas[bar];
            if (wbpsa == null)
            {
                res.add(bar);
            } else
            {
                for (int i = 1; i < wbpsa.getBarRange().size(); i++)
                {
                    it.next();
                }
            }
        }

        return res;
    }

    /**
     * Get the start bar indexes of non-overlapping untiled zones.
     * <p>
     * Example: if untiled bars are 0,4,5,6,7,8 and zoneSize=2, then method returns [4,6].
     *
     * @param zoneSize 1 to 4 bars
     * @return
     */
    public List<Integer> getNonTiledZonesStartBarIndexes(int zoneSize)
    {
        Preconditions.checkArgument(zoneSize >= 1 && zoneSize <= 4, "zoneSize=%s", zoneSize);
        List<Integer> res = new ArrayList<>();

        var nonTiledBars = getNonTiledBars();

        if (nonTiledBars.isEmpty())
        {
            return res;
        }

        if (zoneSize == 1)
        {
            return nonTiledBars;
        }

        for (int i = 0; i <= nonTiledBars.size() - zoneSize; i++)
        {
            int bar = nonTiledBars.get(i);
            boolean ok = true;
            for (int j = 1; j < zoneSize; j++)
            {
                if (nonTiledBars.get(i + j) != bar + j)
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
     * Helper method which just delegates to isUsable(IntRange).
     *
     * @param bar
     * @return
     * @see #isUsable(org.jjazz.utilities.api.IntRange)
     */
    public boolean isUsable(int bar)
    {
        return isUsable(new IntRange(bar, bar));
    }

    /**
     * Check if barRange is within one of the SimpleChordSequence bar ranges managed by this tiling.
     *
     * @param barRange
     * @return
     * @see #isUsableAndFree(org.jjazz.utilities.api.IntRange)
     */
    public boolean isUsable(IntRange barRange)
    {
        boolean b = scsList.stream()
            .map(scs -> scs.getBarRange())
            .anyMatch(br -> br.contains(barRange));
        return b;
    }

    /**
     * Check if bar zone is usable and not covered yet by a WbpSourceAdaptation.
     *
     * @param barRange
     * @return
     * @see #isUsable(org.jjazz.utilities.api.IntRange)
     */
    public boolean isUsableAndFree(IntRange barRange)
    {
        if (!isUsable(barRange))
        {
            return false;
        }
        boolean b = barRange.stream()
            .allMatch(bar -> wbpsas[bar] == null);
        if (b)
        {
            int index = barRange.from - 1;
            int minIndex = Math.max(usableBars.get(0), barRange.from - WbpSourceDatabase.SIZE_MAX + 1);
            while (index >= minIndex)
            {
                var wbpsa = wbpsas[index];
                if (wbpsa != null)
                {
                    b = !wbpsa.getBarRange().isIntersecting(barRange);
                    break;
                }
                index--;
            }
        }
        return b;
    }

    /**
     * Get a new SimpleChordSequence corresponding to barRange.
     *
     * @param barRange Must be a subRange of one of the SimpleChordSequences
     * @param addInitChordSympol
     * @return
     * @see #isUsable(org.jjazz.utilities.api.IntRange)
     */
    public SimpleChordSequence getSimpleChordSequence(IntRange barRange, boolean addInitChordSympol)
    {
        Objects.requireNonNull(barRange);

        SimpleChordSequence scs = scsList.stream()
            .filter(cs -> cs.getBarRange().contains(barRange))
            .findAny()
            .orElse(null);
        if (scs == null)
        {
            throw new IllegalArgumentException("barRange=" + barRange + " this=" + this);
        }
        var res = scs.subSequence(barRange, addInitChordSympol);
        return res;
    }

    /**
     * For each chord sequence corresponding to an untiled zone, try to create WbpSources using wbpSourcesBuilder.
     * <p>
     * It might happen than some returned WbpSources could not be added in the WbpSourceDatabase, because the database will consider there are
     * redundant (transposition-wise and using simplified chord symbols).
     *
     * @param wbpSourcesBuilder Generate a list (possibly empty) of WbpSources for a SimpleChordSequence.
     * @param size The size in bars of the WbpSources to be created.
     * @return The created WbpSources
     */
    public List<WbpSource> buildMissingWbpSources(CustomWbpSourcesBuilder wbpSourcesBuilder, int size)
    {
        Objects.requireNonNull(wbpSourcesBuilder);
        Preconditions.checkArgument(WbpSourceDatabase.checkWbpSourceSize(size), "size=%s", size);

        List<WbpSource> res = new ArrayList<>();

        // Avoids processing the exact same chord sequence twice
        Set<SimpleChordSequence> processedChordSequences = new HashSet<>();

        var startBars = getNonTiledZonesStartBarIndexes(size);
        for (int startBar : startBars)
        {
            var br = new IntRange(startBar, startBar + size - 1);
            var subSeq = getSimpleChordSequence(br, true).getShifted(-startBar);
            if (processedChordSequences.contains(subSeq))
            {
                continue;
            }

            var nextWbpsa = getWbpSourceAdaptationStartingAt(br.to + 1);
            int targetPitch = nextWbpsa != null ? nextWbpsa.getAdaptedPhrase().first().getPitch() : -1;

            // Create the WbpSources
            var wbpSources = wbpSourcesBuilder.build(subSeq, targetPitch);
            res.addAll(wbpSources);

            processedChordSequences.add(subSeq);
        }

        return res;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public String toString()
    {
        return "Tiling" + getTiledBars();
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
        for (var wbpsa : getWbpSourceAdaptations())
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
        sb.append("TILING ").append(String.valueOf(getTiledBars().size())).append("/").append(String.valueOf(usableBars.size())).append(" :\n");

        for (int bar : usableBars)
        {
            String s = "";
            var wbpsaStart = getWbpSourceAdaptationStartingAt(bar);
            if (wbpsaStart != null)
            {
                StringJoiner joiner = new StringJoiner(",", " [", "]");
                joiner.setEmptyValue("");
                var wbps = wbpsaStart.getWbpSource();
                if (!wbps.isStartingOnChordBass())
                {
                    joiner.add("nonRootToneStart");
                }
                if (!wbps.isEndingOnChordTone())
                {
                    joiner.add("nonChordToneEnd");
                }
                s = wbpsaStart.toString() + joiner.toString() + " note0BeatShift="
                    + wbpsaStart.getWbpSource().getFirstNoteBeatShift();
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
    private void checkBarIsValid(int bar)
    {
        Preconditions.checkArgument(bar >= 0 && isUsable(bar), "bar=%s usableBars=%s", bar, usableBars);
    }
}
