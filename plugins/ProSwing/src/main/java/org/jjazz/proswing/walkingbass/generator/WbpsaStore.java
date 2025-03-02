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
package org.jjazz.proswing.walkingbass.generator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.proswing.walkingbass.generator.WbpsaScorer.Score;

/**
 * Store the best WbpSourceAdaptations of all sizes for each non-tiled bar of a tiling.
 */
public class WbpsaStore
{

    public static final float DEFAULT_RANDOMIZE_WITHIN_OVERALL_SCORE_WINDOW = 5f;
    private final WbpTiling tiling;
    /**
     * [0] not used, [1] for size=1 bar, ... [4] for size=4 bars.
     */
    private final ListMultimap<Integer, WbpSourceAdaptation>[] mmapWbpsAdaptations = new ListMultimap[WbpSourceDatabase.SIZE_MAX + 1];
    private final int width;
    private static final Logger LOGGER = Logger.getLogger(WbpsaStore.class.getSimpleName());
    private final WbpsaScorer wbpsaScorer;
    private final float randomizeWithinOverallScoreWindow;

    /**
     * Create a WbpsaStore for the untiled bars of tiling.
     * <p>
     * If randomizeWithinOverallScoreWindow is &gt; 0; some randomization is added between WbpSourceAdaptations whose compatibility scores are similar (i.e.
     * within randomizeWithinOverallScoreWindow).
     *
     * @param tiling                            The store will ignore already tiled bars
     * @param width                             Max number of WbpSourceAdaptations kept per bar
     * @param scorer
     * @param randomizeWithinOverallScoreWindow If &lt;= 0 there is no randomization
     */
    public WbpsaStore(WbpTiling tiling, int width, WbpsaScorer scorer, float randomizeWithinOverallScoreWindow)
    {
        this.tiling = tiling;
        this.wbpsaScorer = scorer;
        this.width = width;
        this.randomizeWithinOverallScoreWindow = randomizeWithinOverallScoreWindow;

        for (int size = WbpSourceDatabase.SIZE_MIN; size <= WbpSourceDatabase.SIZE_MAX; size++)
        {
            mmapWbpsAdaptations[size] = MultimapBuilder.treeKeys() // Sort by bar
                    .arrayListValues() // Wbpsas NOT sorted to enable possible randomization
                    .build();
        }

        initialize();

    }

    public int getWidth()
    {
        return width;
    }


    /**
     * Get the list of WbpSourceAdaptations for specified bar and size.
     *
     * @param bar  Must be a usable bar
     * @param size The size in bars of returned WbpSourceAdaptations
     * @return Unmodifiable list which can be empty. The list contains maximum getWidth() elements. List is ordered only if store does not use randomization.
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(int bar, int size)
    {
        Preconditions.checkArgument(tiling.getBarRange().contains(bar), "bar=%s", bar);
        Preconditions.checkArgument(size >= WbpSourceDatabase.SIZE_MIN && size <= WbpSourceDatabase.SIZE_MAX, "size=%s", size);
        var res = Collections.unmodifiableList(mmapWbpsAdaptations[size].get(bar));
        return res;
    }

    /**
     * Get all-size WbpSourceAdaptations list for specified bar.
     * <p>
     * Returned list is ordered by descending size then, if the store does not use randomization, by descending compatibility order.
     *
     * @param bar Must be a usable bar
     * @return Can be empty.
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(int bar)
    {
        List<WbpSourceAdaptation> res = new ArrayList<>();
        for (int i = WbpSourceDatabase.SIZE_MAX; i >= WbpSourceDatabase.SIZE_MIN; i--)
        {
            res.addAll(getWbpSourceAdaptations(bar, i));
        }
        return res;
    }

    /**
     * Get the ordered list (descending Score) of WbpSourceAdaptations of a specified rank.
     * <p>
     * For a given bar, if there is no more WbpSourceAdaptation at specified rank, use the last WbpSourceAdaptation for this bar. If there is no
     * WbpSourceAdaptation at all, bar is skipped.
     *
     * @param rank 0 means takes the best for each bar, 1 means the 2nd best etc.
     * @param size The size in bars of returned WbpSourceAdaptations
     * @return Can be empty.
     */
    public ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptationsRanked(int rank, int size)
    {
        Preconditions.checkArgument(rank >= 0);
        Preconditions.checkArgument(size >= WbpSourceDatabase.SIZE_MIN && size <= WbpSourceDatabase.SIZE_MAX, "size=%s", size);
        ListMultimap<Score, WbpSourceAdaptation> mmap = MultimapBuilder.treeKeys().arrayListValues().build();

        for (int bar : mmapWbpsAdaptations[size].keySet())
        {
            List<WbpSourceAdaptation> wbpsas = getWbpSourceAdaptations(bar, size);
            if (!wbpsas.isEmpty())
            {
                WbpSourceAdaptation wbpsa = rank + 1 <= wbpsas.size() ? wbpsas.get(rank) : wbpsas.get(wbpsas.size() - 1);
                mmap.put(wbpsa.getCompatibilityScore(), wbpsa);
            }

        }
        return mmap;
    }


    public String toDebugString(boolean hideEmptyBars)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = WbpSourceDatabase.SIZE_MAX; i >= WbpSourceDatabase.SIZE_MIN; i--)
        {
            sb.append("############ ").append(i).append(" bars ############\n");
            sb.append(toDebugString(i, hideEmptyBars));
        }
        return sb.toString();
    }

    public String toDebugString(int size, boolean hideEmptyBars)
    {
        StringBuilder sb = new StringBuilder();
        var scsBr = tiling.getBarRange();
        for (int bar : tiling.getUsableBars())
        {
            var wbpsas = getWbpSourceAdaptations(bar, size);

            if (!hideEmptyBars || !wbpsas.isEmpty())
            {
                var br = new IntRange(bar, bar + size - 1).getIntersection(scsBr);
                var barChords = tiling.getSimpleChordSequenceExt().subSequence(br, true);
                sb.append(String.format("%1$03d: %2$s\n", bar, barChords.toString()));
            }

            wbpsas.stream().forEach(wbpsa -> sb.append("     ").append(wbpsa).append("\n"));
        }
        return sb.toString();
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================
    /**
     * For each bar add the most compatible WbpSourceAdptations of specified size.
     * <p>
     * A bar can contain no more than width WbpSourceAdaptations.
     * <p>
     */
    private void initialize()
    {
        var scsExt = tiling.getSimpleChordSequenceExt();
        var nonTiledBars = tiling.getNonTiledBars();
        LOGGER.log(Level.SEVERE, "initialize() nonTiledBars={0}", nonTiledBars);

        for (int bar : nonTiledBars)
        {
            for (int size = WbpSourceDatabase.SIZE_MAX; size >= WbpSourceDatabase.SIZE_MIN; size--)
            {
                IntRange br = new IntRange(bar, bar + size - 1);
                if (!tiling.isUsableAndFree(br))
                {
                    continue;
                }

                // Get all possible wbpsas for the sub sequence
                var subSeq = scsExt.subSequence(br, true);
                ListMultimap<Score, WbpSourceAdaptation> bestWbpsas = wbpsaScorer.getWbpSourceAdaptations(subSeq, tiling);


                var randWbpsas = randomizeSimilarScoreSets(bestWbpsas, randomizeWithinOverallScoreWindow);


                // Trim
                while (randWbpsas.size() > width)
                {
                    randWbpsas.removeLast();
                }


                // Save data
                mmapWbpsAdaptations[size].putAll(bar, randWbpsas);

                if (bestWbpsas.isEmpty())
                {
                    LOGGER.log(Level.FINE, "initialize() No {0}-bar compatible WbpSources  found for {1}", new Object[]
                    {
                        size, subSeq
                    });
                }
            }
        }
    }


    /**
     * Add some order randomization within sets of WbpSourceAdaptations which are in the same similar score window.
     *
     * @param mmap
     * @param similarScoreWindow If 0 no randomization applied
     * @return A new list with some randomness applied
     */
    private List<WbpSourceAdaptation> randomizeSimilarScoreSets(ListMultimap<Score, WbpSourceAdaptation> mmap, float similarScoreWindow)
    {
        List<WbpSourceAdaptation> res = new ArrayList<>();
        SortedSet<Score> scores = ((SortedSet<Score>) mmap.keySet()).reversed();        // Start by highest score

        if (similarScoreWindow > 0 && !mmap.isEmpty())
        {
            // Group wbpsas per similar score
            float scoreFirst = mmap.get(scores.getFirst()).getFirst().getCompatibilityScore().overall();
            float scoreLast = mmap.get(scores.getLast()).getLast().getCompatibilityScore().overall();

            for (float overallScore = scoreFirst; overallScore >= scoreLast; overallScore -= similarScoreWindow)
            {
                var fromScore = Score.buildFromOverallValue(overallScore);
                var toScore = Score.buildFromOverallValue(Math.max(overallScore - similarScoreWindow, 0));
                var scoresSubset = scores.subSet(fromScore, toScore);   // inclusive, exclusive
                List<WbpSourceAdaptation> wbpsaSubset = new ArrayList<>();
                for (var score : scoresSubset)
                {
                    wbpsaSubset.addAll(mmap.get(score));
                }

                Collections.shuffle(wbpsaSubset);
                res.addAll(wbpsaSubset);
            }
        } else
        {
            res.addAll(mmap.values());
        }

        return res;
    }

}
