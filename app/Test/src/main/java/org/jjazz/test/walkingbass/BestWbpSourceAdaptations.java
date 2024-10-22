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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Store the best WbpSourceAdaptations for each usable bar of a SimpleChordSequence.
 */
class BestWbpSourceAdaptations
{

    private final SimpleChordSequence simpleChordSequence;
    private final SortedSetMultimap<Integer, WbpSourceAdaptation> mmapWbpsAdaptations;
    private final int wbpSourceSize;
    private final List<Integer> usableBarIndexes;
    private final int nbBestMax;
    private static final Logger LOGGER = Logger.getLogger(BestWbpSourceAdaptations.class.getSimpleName());


    /**
     * Create a BestWbpSourceAdaptations.
     *
     * @param scs              Store WbpSources for the bars of this SimpleChordSequence
     * @param size             Size of the WbpSources.
     * @param usableBarIndexes
     * @param nbBestMax        Max number of WbpSourceAdaptations kept for a bar
     */
    public BestWbpSourceAdaptations(SimpleChordSequence scs, int size, List<Integer> usableBarIndexes, int nbBestMax)
    {
        this.simpleChordSequence = scs;
        this.wbpSourceSize = size;
        this.mmapWbpsAdaptations = MultimapBuilder.treeKeys()
                .treeSetValues((WbpSourceAdaptation v1, WbpSourceAdaptation v2) -> Float.compare(v1.getCompatibilityScore(), v2.getCompatibilityScore()))
                .build();
        this.usableBarIndexes = List.copyOf(usableBarIndexes);
        this.nbBestMax = nbBestMax;
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return simpleChordSequence;
    }

    public IntRange getBarRange()
    {
        return simpleChordSequence.getBarRange();

    }

    public int getNbBestMax()
    {
        return nbBestMax;
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
     * Add a WbpSourceAdaptation for the specified bar.
     * <p>
     * <p>
     * If wbpsa compatibility score is not in the getNbBestMax() ones, wbpsa is not added.
     *
     * @param bar
     * @param wbpsa Can't be null
     * @return True if wbpsa was actually added.
     * @throws IllegalArgumentException If bar is not a usable bar
     */
    public boolean add(int bar, WbpSourceAdaptation wbpsa)
    {
        Objects.requireNonNull(wbpsa);
        Preconditions.checkArgument(usableBarIndexes.contains(bar), "bar=%s", bar);

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
     * Remove a WbpSourceAdaptation for the specified bar.
     *
     * @param bar
     * @param wbpsa
     * @return True if wbpsa was actually removed.
     */
    public boolean remove(int bar, WbpSourceAdaptation wbpsa)
    {
        Objects.requireNonNull(wbpsa);
        Preconditions.checkArgument(usableBarIndexes.contains(bar), "bar=%s", bar);
        return mmapWbpsAdaptations.remove(bar, wbpsa);
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
        Preconditions.checkArgument(usableBarIndexes.contains(bar), "bar=%s usableBarIndexes=%s", bar, usableBarIndexes);
        var res = mmapWbpsAdaptations.get(bar);
        return new ArrayList<>(res);
    }


    @Override
    public String toString()
    {
        return WbpSourceTiling.toString(barOffset, wbpSources);
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

    /**
     * For each usable bar set the most compatible WbpSourceAdptations of a given size.
     */
    private void initialize()
    {
        for (int bar : getUsableBarIndexes())
        {
            int lastBar = bar + wbpSourceSize - 1;
            if (!getBarRange().contains(lastBar))
            {
                break;
            }


            var subSeq = simpleChordSequence.subSequence(new IntRange(bar, lastBar), true);
            var rpSources = getRootProfileCompatibleWbpSources(subSeq);
            var bestWpsas = selectBestWbpSources(rpSources, subSeq);    // max nbBestMax elements
            for (var wbpsa : bestWpsas)
            {
                add(bar, wbpsa);
            }

            LOGGER.log(Level.SEVERE, "initialize() bar={0} bestWpsas.size()={1}", new Object[]
            {
                bar, bestWpsas.size()
            });
        }
    }


    /**
     * Find the nbBestMax best WbpSources amongst wbpSources and return the corresponding WbpSourceAdaptations.
     * <p>
     *
     * @param wbpSources
     * @param scs        The SimpleChordSequence we search WbpSources for
     * @return Can be empty
     */
    private List<WbpSourceAdaptation> selectBestWbpSources(List<WbpSource> wbpSources, SimpleChordSequence scs)
    {
        float maxScore = 0;
        WbpSource res = null;
        int count10 = 0;
        for (var wbp : wbpSources)
        {                                    
            WbpSourceAdaptation wbpsa = new WbpSourceAdaptation(wbp, scs);
            var barRange = wbp.getBarRange();
            
            float score = wbpsa.getCompatibilityScore();
            if (score >= 10)
            {
                count10++;
            }
            if (score > 0 && barRange.size() > 1 && barRange.from >= songChordSequence.getBarRange().from && barRange.from <= songChordSequence.getBarRange().from + 1)
            {
                // Provide a little score boost for a "long" WbpSource at first bars: if several long WbpSources are top-scored including the first one, we prefer 
                // that the initial one is chosen to start song with a good-quality bass pattern.
                score += 10f;
            }
            if (score > maxScore)
            {
                res = wbp;
                maxScore = score;
            }
        }
        LOGGER.log(Level.SEVERE, "selectBestWbpSources() res={0} count10={1}", new Object[]
        {
            res, count10
        });
        return res;
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
