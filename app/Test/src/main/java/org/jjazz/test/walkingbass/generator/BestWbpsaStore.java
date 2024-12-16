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
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.WbpDatabase;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.utilities.api.IntRange;

/**
 * Store the best WbpSourceAdaptations for some usable bars of a SimpleChordSequenceExt.
 */
public class BestWbpsaStore
{
    private final SimpleChordSequenceExt simpleChordSequenceExt;
    private final List<Integer> usableBars;
    private final SortedSetMultimap<Integer, WbpSourceAdaptation> mmapWbpsAdaptations;
    private final int wbpSourceSize;
    private final int nbBestMax;
    private static final Logger LOGGER = Logger.getLogger(BestWbpsaStore.class.getSimpleName());

    /**
     * Create a BestWbpsaStore.
     *
     * @param scs        scs
     * @param usableBars Must be a subset of scs usable bars
     * @param size       Size of the WbpSources
     * @param nbBestMax  Max number of WbpSourceAdaptations kept for a bar
     */
    public BestWbpsaStore(SimpleChordSequenceExt scs, List<Integer> usableBars, int size, int nbBestMax)
    {
        Preconditions.checkArgument(usableBars.stream().allMatch(b -> scs.isUsable(b)), "scs=%s, usableBars=%s size=%s", scs, usableBars, size);
        this.simpleChordSequenceExt = scs;
        this.usableBars = usableBars;
        this.wbpSourceSize = size;
        this.mmapWbpsAdaptations = MultimapBuilder.treeKeys()
                .treeSetValues() // No need a custom Comparator because WbpSourceAdatpation uses a natural order by DESCENDING score
                .build();
        this.nbBestMax = nbBestMax;

        initialize();
    }

    public int getNbBestMax()
    {
        return nbBestMax;
    }

    public int getWbpSourceSize()
    {
        return wbpSourceSize;
    }
    
    public List<Integer> getUsableBars()
    {
        return usableBars;
    }

    /**
     * Get the best WbpSourceAdaptations for specified bar, ordered by descending compatibility.
     *
     * @param bar Must be a usable bar
     * @return Can be empty. List contains maximum getNbBestMax() elements.
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(int bar)
    {
        Preconditions.checkArgument(usableBars.contains(bar), "bar=%s usableBars=%s", bar, usableBars);
        SortedSet<WbpSourceAdaptation> res = mmapWbpsAdaptations.get(bar);
        return new ArrayList<>(res);
    }

    /**
     * Get all the WbpSourceAdaptations of specified rank.
     * <p>
     * For a given bar, if there is no more WbpSourceAdaptation at specified rank, use the last WbpSourceAdaptation for this bar. If there is no
     * WbpSourceAdaptation at all, bar is skipped.
     *
     * @param rank 0 means takes the best for each bar, 1 means the 2nd best etc.
     * @return Can be empty
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptationsRanked(int rank)
    {
        List<WbpSourceAdaptation> res = new ArrayList<>();
        for (int bar : usableBars)
        {
            List<WbpSourceAdaptation> wbpsas = getWbpSourceAdaptations(bar);
            if (!wbpsas.isEmpty())
            {
                WbpSourceAdaptation wbpsa = wbpsas.size() >= rank + 1 ? wbpsas.get(rank) : wbpsas.get(wbpsas.size() - 1);
                res.add(wbpsa);
            }

        }
        return res;
    }

    @Override
    public String toString()
    {
        return mmapWbpsAdaptations.toString();
    }

    public void dump()
    {
        for (int bar : usableBars)
        {
            var wbpsas = getWbpSourceAdaptations(bar);
            String firstStr = "";
            if (!wbpsas.isEmpty())
            {
                firstStr = wbpsas.get(0).toString();
            }
            LOGGER.log(Level.INFO, "{0}: {1}", new Object[]
            {
                String.format("%1$03d", bar), firstStr
            });
            wbpsas.stream()
                    .skip(1)
                    .forEach(wbpsa -> LOGGER.log(Level.INFO, "     {0}", wbpsa));
        }
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================
    /**
     * For each usable bar add the most compatible WbpSourceAdptations of wbpSourceSize.
     */
    private void initialize()
    {
        for (int bar : usableBars)
        {
            IntRange br = new IntRange(bar, bar + wbpSourceSize - 1);
            if (isUsable(br))
            {
                SimpleChordSequence subSeq = simpleChordSequenceExt.subSequence(br, true);
                List<WbpSource> rpWbpSources = getRootProfileCompatibleWbpSources(subSeq);
                for (WbpSource wbpSource : rpWbpSources)
                {
                    WbpSourceAdaptation wbpsa = new WbpSourceAdaptation(wbpSource, subSeq);
                    addIfCompatibleEnough(bar, wbpsa);
                }
                if (rpWbpSources.isEmpty())
                {
                    LOGGER.log(Level.FINE, "initialize() No {0}-bar rpSources found for {1}", new Object[]
                    {
                        wbpSourceSize, subSeq
                    });
                }
            }
        }
    }

    /**
     * Add a WbpSourceAdaptation for the specified bar.
     * <p>
     * <p>
     * If absolute wbpsa compatibility score is not good enough, or is good enough but not in the getNbBestMax() best ones, wbpsa is not added.
     *
     * @param bar
     * @param wbpsa Can't be null
     * @return True if wbpsa was actually added.
     * @throws IllegalArgumentException If bar is not a usable bar
     */
    private boolean addIfCompatibleEnough(int bar, WbpSourceAdaptation wbpsa)
    {
        if (wbpsa.getCompatibilityScore().overall() < WbpSourceAdaptation.MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE)
        {
            return false;
        }

        mmapWbpsAdaptations.put(bar, wbpsa);
        boolean b = true;

        // Make sure we do not exceed nbBestMax values
        SortedSet<WbpSourceAdaptation> wbpsas = mmapWbpsAdaptations.get(bar);
        if (wbpsas.size() > nbBestMax)
        {
            wbpsas.remove(wbpsa);
            b = false;
        }
        return b;
    }

    /**
     * Get the WbpSources which match the root profile of scs.
     * <p>
     *
     * @param scs
     * @return
     */
    private List<WbpSource> getRootProfileCompatibleWbpSources(SimpleChordSequence scs)
    {
        String rp = scs.getRootProfile();
        LOGGER.log(Level.FINE, "getRootProfileCompatibleWbpSources() -- scs={0} rp={1}", new Object[]
        {
            scs, rp
        });
        List<WbpSource> res = WbpDatabase.getInstance().getWbpSources(rp);
        return res;
    }

    private boolean isUsable(IntRange br)
    {
        return br.stream().allMatch(bar -> usableBars.contains(bar));
    }

}
