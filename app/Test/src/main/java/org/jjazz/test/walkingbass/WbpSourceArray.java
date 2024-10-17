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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Store a possible WbpSource for each usable bar of a SimpleChordSequence.
 */
class WbpSourceArray
{   
    private final SimpleChordSequence simpleChordSequence;
    private final Object[] wbpSources;
    private final int wbpSourceSize;
    private final int barOffset;
    private final List<Integer> usableBarIndexes;


    private final Map<WbpSource, Integer> mapWbpSourceIndex;
    private final Map<WbpSource, Float> mapWbpSourceCompatibilityScore;
    private static final Logger LOGGER = Logger.getLogger(WbpSourceArray.class.getSimpleName());

    /**
     * Create a WbpSource Array.
     *
     * @param scs              Store WbpSources for the bars of this SimpleChordSequence
     * @param size             Size of the wbpSources
     * @param usableBarIndexes
     */
    public WbpSourceArray(SimpleChordSequence scs, int size, List<Integer> usableBarIndexes)
    {
        this.simpleChordSequence = scs;
        this.wbpSourceSize = size;
        wbpSources = new WbpSource[scs.getBarRange().size()];
        barOffset = scs.getBarRange().from;
        mapWbpSourceIndex = new HashMap<>();
        mapWbpSourceCompatibilityScore = new HashMap<>();
        this.usableBarIndexes = List.copyOf(usableBarIndexes);
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return simpleChordSequence;
    }

    public int getWbpSourceSize()
    {
        return wbpSourceSize;
    }

    public List<Integer> getUsableBarIndexes()
    {
        return usableBarIndexes;
    }

    /**
     * Set the WbpSource for the specified bar.
     * <p>
     * Also compute wbpSource compatibility score.
     *
     * @param bar
     * @param wbpSource Can be null
     * @see #getCompatibilityScore(org.jjazz.walkingbass.WbpSource)
     * @throws IllegalArgumentException If bar is not a usable bar
     */
    public void set(int bar, WbpSource wbpSource)
    {
        Preconditions.checkArgument(usableBarIndexes.contains(bar), "bar=%s", bar);

        int index = bar - barOffset;
        WbpSource old = getWbpSource(bar);
        wbpSources[index] = wbpSource;
        if (wbpSource != null)
        {
            mapWbpSourceIndex.put(wbpSource, index);
            float score = computeCompatibilityScore(wbpSource, simpleChordSequence.subSequence(new IntRange(bar, bar + wbpSourceSize - 1), true));
            mapWbpSourceCompatibilityScore.put(wbpSource, score);
        }

        if (old != null)
        {
            mapWbpSourceIndex.remove(old);
            mapWbpSourceCompatibilityScore.remove(old);
        }
    }


    /**
     * Get all the WbpSources present in this array.
     *
     * @return Collection has no particular order
     */
    public Collection<WbpSource> getWbpSources()
    {
        var res = mapWbpSourceIndex.keySet();
        return res;
    }


    /**
     * Get the WbpSource for specified bar.
     *
     * @param bar
     * @return Can be null
     * @throws IllegalArgumentException If bar is not a usable bar
     */
    public WbpSource getWbpSource(int bar)
    {
        Preconditions.checkArgument(usableBarIndexes.contains(bar), "bar=%s usableBarIndexes=%s", bar, usableBarIndexes);

        if (wbpSources[bar - barOffset] instanceof WbpSource wbps)
        {
            return wbps;
        } else
        {
            return null;
        }
    }

    /**
     * Get the start bar of wbpSource.
     *
     * @param wbpSource
     * @return -1 wbpSource is not used.
     */
    public int getStartBar(WbpSource wbpSource)
    {
        Integer index = mapWbpSourceIndex.get(wbpSource);
        return index == null ? -1 : index + barOffset;
    }

    /**
     * Get the compatibility score of wbp.
     *
     * @param wbp
     * @return
     * @throws IllegalArgumentException If wbp was not set in this tiling.
     */
    public float getCompatibilityScore(WbpSource wbp)
    {
        Float score = mapWbpSourceCompatibilityScore.get(wbp);
        if (score == null)
        {
            throw new IllegalArgumentException("WbpSource " + wbp + " not found in this ScsTiling.");
        }
        return score;
    }

    /**
     * Compute a score which indicates how much wbp's chord sequence is suited to scs.
     * <p>
     * If score does not reach a minimum viable score, return value is 0.
     *
     * @param wbp
     * @param scs
     * @return
     */
    public static float computeCompatibilityScore(WbpSource wbp, SimpleChordSequence scs)
    {
        final int MIN_VIABLE_CHORDTYPE_COMPATIBILITY_SCORE = 16;  // same famiy, fifth and seventh (or sixth)
        float score = scs.getChordTypeSimilarityScore(wbp.getSimpleChordSequence());

        // Alter score if transposition is problematic
        var cliCs = scs.first();
        int tScore = wbp.getTransposibilityScore(cliCs.getData().getRootNote());
        score -= (100 - tScore) * 0.02f;
        if (score < MIN_VIABLE_CHORDTYPE_COMPATIBILITY_SCORE)
        {
            score = 0;
        }
        return score;
    }

    @Override
    public String toString()
    {
        return WbpSourceTiling.toString(barOffset, wbpSources);
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

}
