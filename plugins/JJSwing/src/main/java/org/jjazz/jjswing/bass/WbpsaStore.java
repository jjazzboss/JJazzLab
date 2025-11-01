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
package org.jjazz.jjswing.bass;

import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.Utilities;

/**
 * Stores all compatible (compatibility Score &gt; 0) WbpSourceAdaptations of all sizes for each non-tiled bar of a tiling.
 * <p>
 * When a WbpSourceAdaptation is added to the tiling, possibly update the WbpSourceAdaptation lists for the above/below bars.
 */
public class WbpsaStore
{

    private static final float RANDOMIZATION_SCORE_WINDOW_SIZE = 7f;  // Overall score is [0;100]

    private final WbpTiling tiling;
    /**
     * [0] not used, [1] for size=1 bar, ... [4] for size=4 bars.
     */
    private final ListMultimap<Integer, WbpSourceAdaptation>[] mmapWbpsAdaptations;
    private final WbpsaScorer wbpsaScorer;
    private final int tempo;
    private static final Logger LOGGER = Logger.getLogger(WbpsaStore.class.getSimpleName());

    /**
     * Create an empty WbpsaStore for the nontiled bars of a tiling.
     * <p>
     *
     * @param tiling The store will ignore already tiled bars
     * @param tempo  If &lt;0 tempo is ignored in the selection and ranking of WbpSourceAdaptations
     * @see #populate(int, java.util.List)
     */
    public WbpsaStore(WbpTiling tiling, int tempo)
    {
        Objects.requireNonNull(tiling);
        this.tempo = tempo;
        this.mmapWbpsAdaptations = new ListMultimap[WbpSourceDatabase.SIZE_MAX + 1];
        this.tiling = tiling;
        this.tiling.addPropertyChangeListener(e -> tilingUpdated((WbpSourceAdaptation) e.getNewValue()));
        this.wbpsaScorer = new WbpsaScorer(new DefaultPhraseAdapter(), Score.DEFAULT_TESTER);
        for (int size = WbpSourceDatabase.SIZE_MIN; size <= WbpSourceDatabase.SIZE_MAX; size++)
        {
            mmapWbpsAdaptations[size] = MultimapBuilder.hashKeys() // bars
                    .arrayListValues() // WbpSourceAdaptations
                    .build();
        }

    }

    /**
     * Populate the specified bars (if usable and free) with compatible WbpSourceAdaptations from WbpSourceDatabase.
     * <p>
     *
     * @param bars       Tiling bars
     * @param bassStyles Can not be empty
     * @see WbpSourceAdaptation#getWbpSourceAdaptations(org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence, org.jjazz.jjswing.walkingbass.WbpsaScorer,
     * org.jjazz.jjswing.walkingbass.WbpTiling, int, java.util.List)
     * @see #addWbpSourceAdaptations(int, java.util.List)
     */
    public void populate(List<Integer> bars, List<BassStyle> bassStyles)
    {
        Objects.requireNonNull(bassStyles);
        Preconditions.checkArgument(!bassStyles.isEmpty());

        LOGGER.log(Level.FINE, "initialize() bars={0}", bars);

        for (int bar : bars)
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
                if (!wbpsas.isEmpty())
                {
                    addWbpSourceAdaptations(bar, wbpsas);
                } else
                {
                    LOGGER.log(Level.FINE, "populate() No {0}-bar compatible WbpSources found for {1}", new Object[]
                    {
                        size, subSeq
                    });
                }
            }
        }
    }

    /**
     * Repopulate the specified bar/size with WbpSourceAdaptations from WbpSourceDatabase.
     * <p>
     * Do nothing if the tiling bars are not usable and free.
     *
     * @param bar
     * @param size
     * @param bassStyles
     */
    public void repopulate(int bar, int size, List<BassStyle> bassStyles)
    {
        IntRange br = new IntRange(bar, bar + size - 1);
        if (!tiling.isUsableAndFree(br))
        {
            return;
        }

        LOGGER.log(Level.FINE, "repopulate() -- bar={0} size={1}", new Object[]
        {
            bar, size
        });


        clearWbpSourceAdaptations(bar, size);
        var subSeq = tiling.getSimpleChordSequence(br, true);
        var wbpsas = WbpSourceAdaptation.getWbpSourceAdaptations(subSeq, wbpsaScorer, tiling, tempo, bassStyles);
        addWbpSourceAdaptations(bar, wbpsas);

    }

    /**
     * Remove all the current WbpSourceAdaptations for the specified bar/size.
     *
     * @param bar
     * @param size
     */
    public void clearWbpSourceAdaptations(int bar, int size)
    {
        Preconditions.checkArgument(tiling.isUsable(bar), "bar=%s usableBars=%s", bar, tiling.getUsableBars());
        Preconditions.checkArgument(WbpSourceDatabase.checkWbpSourceSize(size), "size=%s", size);
        mmapWbpsAdaptations[size].removeAll(bar);
    }

    /**
     * Add WbpSourceAdaptations for a specific bar.
     * <p>
     * No check is done to avoid adding a duplicate WbpSourceAdaptation for the specified bar.
     *
     * @param bar
     * @param wbpsas All instances must have the same size
     * @see #populate(int, java.util.List)
     */
    public void addWbpSourceAdaptations(int bar, List<WbpSourceAdaptation> wbpsas)
    {
        Objects.requireNonNull(wbpsas);
        Preconditions.checkArgument(tiling.isUsable(bar), "bar=%s usableBars=%s", bar, tiling.getUsableBars());
        if (wbpsas.isEmpty())
        {
            return;
        }
        int size = wbpsas.get(0).getBarRange().size();
        Preconditions.checkArgument(wbpsas.stream().allMatch(wbpsa -> wbpsa.getBarRange().size() == size), "wbpsas=%s", wbpsas);


        var wbpsaList = new ArrayList<>(wbpsas);
        var oldWbpsas = mmapWbpsAdaptations[size].get(bar);
        if (!oldWbpsas.isEmpty())
        {
            // Need to merge them and resort
            wbpsaList.addAll(oldWbpsas);
            Collections.sort(wbpsaList, (o1, o2) -> o2.compareTo(o1));     // Restore descending order
            oldWbpsas.clear();
        }

        // Handle partial randomization
        var rWbpsas = BassGeneratorSettings.getInstance().isWbpsaStoreRandomized() ? randomizeSimilarScoreSets(wbpsaList) : wbpsaList;

        // Save state
        mmapWbpsAdaptations[size].get(bar).addAll(rWbpsas);

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
     * @return Can be empty. List is ordered by descending Score with some possible partial randomization depending on
     *         {@link BassGeneratorSettings#isWbpsaStoreRandomized()}
     */
    public List<WbpSourceAdaptation> getWbpSourceAdaptations(int bar, int size)
    {
        Preconditions.checkArgument(tiling.getUsableBars().contains(bar), "bar=%s", bar);
        Preconditions.checkArgument(WbpSourceDatabase.checkWbpSourceSize(size), "size=%s", size);

        var res = new ArrayList<>(mmapWbpsAdaptations[size].get(bar));

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
        Score setLimitScore = Score.buildSampleFromOverallValue(Math.max(wbpsa.getCompatibilityScore().overall() - RANDOMIZATION_SCORE_WINDOW_SIZE, 0));
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

                setLimitScore = Score.buildSampleFromOverallValue(Math.max(score.overall() - RANDOMIZATION_SCORE_WINDOW_SIZE, 0));
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

    /**
     * A new WbpSourceAdaptation was added to the tiling.
     * <p>
     * Possibly repopulate WbpSourceAdaptations for the before/after tiling bars.
     *
     * @param wbpsa
     */
    private void tilingUpdated(WbpSourceAdaptation wbpsa)
    {
        var bassStyle = wbpsa.getWbpSource().getBassStyle();
        if (bassStyle.isCustom())
        {
            // Don't bother
            return;
        }


        // repopulate bars before and after
        var br = wbpsa.getBarRange();
        for (int size = WbpSourceDatabase.SIZE_MIN; size <= WbpSourceDatabase.SIZE_MAX; size++)
        {
            if (br.from >= size)
            {
                repopulate(br.from - size, size, List.of(bassStyle));
            }
            repopulate(br.to + 1, size, List.of(bassStyle));
        }
    }


}
