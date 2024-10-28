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
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Store the best WbpSourceAdaptations for the specified usable bars of a SimpleChordSequenceExt.
 */
class BestWbpSourceAdaptations
{

    private final SimpleChordSequenceExt simpleChordSequenceExt;
    private final List<Integer> usableBars;
    private final SortedSetMultimap<Integer, WbpSourceAdaptation> mmapWbpsAdaptations;
    private final int wbpSourceSize;
    private final int nbBestMax;
    private static final Logger LOGGER = Logger.getLogger(BestWbpSourceAdaptations.class.getSimpleName());


    /**
     * Create a BestWbpSourceAdaptations.
     *
     * @param scs        Store WbpSources for the usable bars of this SimpleChordSequenceExt
     * @param usableBars Must be a subset of scs usable bars
     * @param size       Size of the WbpSources.
     * @param nbBestMax  Max number of WbpSourceAdaptations kept for a bar
     */
    public BestWbpSourceAdaptations(SimpleChordSequenceExt scs, List<Integer> usableBars, int size, int nbBestMax)
    {
        Preconditions.checkArgument(usableBars.stream().allMatch(b -> scs.isUsable(b)), "scs=%s, usableBars=%s size=%d", scs, usableBars, size);
        
        this.simpleChordSequenceExt = scs;
        this.usableBars = usableBars;
        this.wbpSourceSize = size;
        this.mmapWbpsAdaptations = MultimapBuilder.treeKeys()
            .treeSetValues((WbpSourceAdaptation v1, WbpSourceAdaptation v2) -> Float.compare(v1.getCompatibilityScore(), v2.getCompatibilityScore()))
            .build();
        this.nbBestMax = nbBestMax;

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

    public int getNbBestMax()
    {
        return nbBestMax;
    }

    public int getWbpSourceSize()
    {
        return wbpSourceSize;
    }


    /**
     * Get the best WbpSourceAdaptations for specified bar, ordered by descending compatibility.
     *
     * @param bar
     * @return Can be empty. List contains maximum getNbBestMax() elements.
     * @throws IllegalArgumentException If bar is not a usable bar
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(int bar)
    {
        Preconditions.checkArgument(simpleChordSequenceExt.isUsable(bar), "bar=%s simpleChordSequenceExt=%s", bar, simpleChordSequenceExt);
        var res = mmapWbpsAdaptations.get(bar);
        return new ArrayList<>(res);
    }

    /**
     * Get all the WbpSourceAdaptations of specified rank.
     * <p>
     * For a given bar, if there is no more WbpSourceAdaptation at specified rank, return the last WbpSourceAdaptation, or null if no
     * WbpSourceAdaptation at all.
     *
     * @param rank 0 means takes the best for each bar, 1 means the 2nd best etc.
     * @return A list with one WbpSourceAdaptation per bar (value might be null for some bars).
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptationsRanked(int rank)
    {
        List<WbpSourceAdaptation> res = new ArrayList<>();

        for (int bar : usableBars)
        {
            WbpSourceAdaptation wbpsa = null;
            var wbpsas = new ArrayList<>(mmapWbpsAdaptations.get(bar));
            if (!wbpsas.isEmpty())
            {
                wbpsa = wbpsas.size() >= rank + 1 ? wbpsas.get(rank) : wbpsas.get(wbpsas.size() - 1);
            }
            res.add(wbpsa);
        }

        return new ArrayList<>(res);
    }


    @Override
    public String toString()
    {
        return mmapWbpsAdaptations.toString();
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
            int lastBar = bar + wbpSourceSize - 1;
            var br = new IntRange(bar, lastBar);
            var subSeq = simpleChordSequenceExt.subSequence(br, true);
            var rpSources = getRootProfileCompatibleWbpSources(subSeq);
            for (var rpSource : rpSources)
            {
                WbpSourceAdaptation wbpsa = new WbpSourceAdaptation(rpSource, subSeq);
                addIfCompatibleEnough(bar, wbpsa);
            }
        }
    }


    /**
     * Add a WbpSourceAdaptation for the specified bar.
     * <p>
     * <p>
     * If wbpsa compatibility score is not in the getNbBestMax() best ones, wbpsa is not added.
     *
     * @param bar
     * @param wbpsa Can't be null
     * @return True if wbpsa was actually added.
     * @throws IllegalArgumentException If bar is not a usable bar
     */
    private boolean addIfCompatibleEnough(int bar, WbpSourceAdaptation wbpsa)
    {

        mmapWbpsAdaptations.put(bar, wbpsa);
        boolean b = true;

        // Make sure we do not exceed nbBestMax values
        var wbpsas = mmapWbpsAdaptations.get(bar);
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
        var res = WbpDatabase.getInstance().getWbpSources(rp);
        return res;
    }


}
