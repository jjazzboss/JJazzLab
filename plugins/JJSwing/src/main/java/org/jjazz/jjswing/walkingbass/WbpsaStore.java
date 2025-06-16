/**
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
 * <p>
 * Contributor(s):
 * <p>
 */
package org.jjazz.jjswing.walkingbass;

import org.jjazz.jjswing.walkingbass.db.WbpSourceDatabase;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.utilities.api.IntRange;

/**
 * Create and store all compatible (compatibility Score &gt; 0) WbpSourceAdaptations of all sizes for each non-tiled bar of a tiling.
 */
public class WbpsaStore
{

    private static final float SIMILAR_OVERALL_SCORE_WINDOW_SIZE = 7f;  // Overall score is [0;100]

    private final WbpTiling tiling;
    /**
     * [0] not used, [1] for size=1 bar, ... [4] for size=4 bars.
     */
    private final ListMultimap<Integer, WbpSourceAdaptation>[] mmapWbpsAdaptations = new ListMultimap[WbpSourceDatabase.SIZE_MAX + 1];
    private final WbpsaScorer wbpsaScorer;
    private static final Logger LOGGER = Logger.getLogger(WbpsaStore.class.getSimpleName());

    /**
     * Create an empty WbpsaStore for the nontiled bars of a tiling.
     * <p>
     *
     * @param tiling The store will ignore already tiled bars
     * @see #populate(int, java.util.List)
     */
    public WbpsaStore(WbpTiling tiling)
    {
        Objects.requireNonNull(tiling);

        this.tiling = tiling;
        this.wbpsaScorer = new WbpsaScorer(new DefaultPhraseAdapter(), Score.DEFAULT_TESTER);
        for (int size = WbpSourceDatabase.SIZE_MIN; size <= WbpSourceDatabase.SIZE_MAX; size++)
        {
            mmapWbpsAdaptations[size] = MultimapBuilder.hashKeys() // bars
                    .arrayListValues() // WbpSourceAdaptations
                    .build();
        }

    }

    /**
     * Create and store compatible WbpSourceAdaptations for the tiling with the specified parameters.
     * <p>
     * WbpSourceAdaptations created by this method are added in the store along with the existing ones.
     *
     * @param tempo      If &lt;0 tempo is ignored
     * @param bassStyles Can not be empty
     * @see WbpSourceAdaptation#getWbpSourceAdaptations(org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence, org.jjazz.jjswing.walkingbass.WbpsaScorer,
     * org.jjazz.jjswing.walkingbass.WbpTiling, int, java.util.List)
     */
    public void populate(int tempo, List<BassStyle> bassStyles)
    {
        Preconditions.checkArgument(!bassStyles.isEmpty(), "bassStyles=" + bassStyles);

        var nonTiledBars = tiling.getNonTiledBars();

        LOGGER.log(Level.FINE, "initialize() nonTiledBars={0}", nonTiledBars);

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
                var subSeq = tiling.getSimpleChordSequence(br, true);
                var wbpsas = WbpSourceAdaptation.getWbpSourceAdaptations(subSeq, wbpsaScorer, tiling, tempo, bassStyles);

                var oldWbpsas = mmapWbpsAdaptations[size].get(bar);
                if (!oldWbpsas.isEmpty())
                {
                    // Need to merge them and resort
                    wbpsas.addAll(oldWbpsas);
                    Collections.sort(wbpsas, (o1, o2) -> o2.compareTo(o1));     // Restore descending order
                    oldWbpsas.clear();
                }

                // Handle partial randomization
                var rWbpsas = JJSwingBassMusicGeneratorSettings.getInstance().isWbpsaStoreRandomized() ? randomizeSimilarScoreSets(wbpsas) : wbpsas;

                // Save state
                mmapWbpsAdaptations[size].get(bar).addAll(rWbpsas);


                if (wbpsas.isEmpty())
                {
                    LOGGER.log(Level.FINE, "initialize() No {0}-bar compatible WbpSources  found for {1}", new Object[]
                    {
                        size, subSeq
                    });
                }
            }
        }

    }

    public WbpTiling getTiling()
    {
        return tiling;
    }

    /**
     * Get the list of WbpSourceAdaptations for specified bar and specified sizes.
     *
     * @param bar  Must be a usable bar
     * @param size The bar size of returned WbpSourceAdaptations.
     * @return Can be empty. List is ordered by descending Score with some possible partial randomization depending on {@link JJSwingBassMusicGeneratorSettings#isWbpsaStoreRandomized()
     *         }
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(int bar, int size)
    {
        Preconditions.checkArgument(tiling.getUsableBars().contains(bar), "bar=%s", bar);
        Preconditions.checkArgument(WbpSourceDatabase.checkWbpSourceSize(size), "size=%s", size);

        var res = Collections.unmodifiableList(mmapWbpsAdaptations[size].get(bar));

        return res;
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
        for (int bar : tiling.getUsableBars())
        {
            var wbpsas = getWbpSourceAdaptations(bar, size);

            if (!hideEmptyBars || !wbpsas.isEmpty())
            {
                var br = new IntRange(bar, bar + size - 1);
                var barChords = tiling.getSimpleChordSequence(br, true);
                sb.append(String.format("%1$03d: %2$s\n", bar, barChords.toString()));
            }

            wbpsas.forEach(wbpsa -> sb.append("     ").append(wbpsa).append("\n"));
        }
        return sb.toString();
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================
    /**
     * Add some order randomization within lists of WbpSourceAdaptations which are in the same similar score window.
     *
     * @param wbpsas Must be ordered by descending Score
     * @return A new list globally ordered by descending Score, but some randomness is applied within SIMILAR_SCORE_WINDOW_SIZE sets.
     */
    private List<WbpSourceAdaptation> randomizeSimilarScoreSets(List<WbpSourceAdaptation> wbpsas)
    {
        if (wbpsas.size() <= 1)
        {
            return wbpsas;
        }

        List<WbpSourceAdaptation> res = new ArrayList<>();


        // Group wbpsas per similar score set and shuffle the set
        var wbpsa = wbpsas.getFirst();
        Score setLimitScore = Score.buildSampleFromOverallValue(Math.max(wbpsa.getCompatibilityScore().overall() - SIMILAR_OVERALL_SCORE_WINDOW_SIZE, 0));
        List<WbpSourceAdaptation> subset = new ArrayList<>();

        var it = wbpsas.iterator();
        while (it.hasNext())
        {
            wbpsa = it.next();
            Score score = wbpsa.getCompatibilityScore();
            if (score.compareTo(setLimitScore) > 0)
            {
                subset.add(wbpsa);
            } else
            {
                if (!subset.isEmpty())
                {
                    Collections.shuffle(subset);
                    res.addAll(subset);
                    subset.clear();
                }

                setLimitScore = Score.buildSampleFromOverallValue(Math.max(score.overall() - SIMILAR_OVERALL_SCORE_WINDOW_SIZE, 0));
                subset.add(wbpsa);
            }
        }

        if (!subset.isEmpty())
        {
            Collections.shuffle(subset);
            res.addAll(subset);
        }

        return res;
    }

}
