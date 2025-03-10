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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.proswing.walkingbass.WbpSource;
import org.jjazz.proswing.walkingbass.WbpSourceChordPhrase;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;
import org.jjazz.rhythm.api.TempoRange;

/**
 * Evaluates compatibility using chord type, transposition, target notes before/after, tempo.
 * <p>
 */
public class DefaultWbpsaScorer implements WbpsaScorer
{

    private final PhraseAdapter wbpSourceAdapter;
    private final int tempo;
    private final EnumSet<BassStyle> bassStyles;

    /**
     *
     * @param sourceAdapter If null target note matching score will not impact the overall score
     * @param tempo         If &lt;= 0 tempo is ignored in score computing
     * @param bStyles       Accept only WbpSources which match these bassStyle(s). If empty any BassStyle is accepted
     */
    public DefaultWbpsaScorer(PhraseAdapter sourceAdapter, int tempo, BassStyle... bStyles)
    {
        this.wbpSourceAdapter = sourceAdapter;
        this.tempo = tempo;
        this.bassStyles = EnumSet.allOf(BassStyle.class);
        if (bStyles.length > 0)
        {
            bassStyles.clear();
            this.bassStyles.addAll(Arrays.asList(bStyles));
        }
    }

    public int getTempo()
    {
        return tempo;
    }

    public Set<BassStyle> getBassStyles()
    {
        return Collections.unmodifiableSet(bassStyles);
    }

    @Override
    public Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        Score res = WbpsaScorer.SCORE_ZERO;

        if (bassStyles.contains(wbpsa.getWbpSource().getBassStyle()))
        {
            var ctScores = getHarmonyCompatibilityScores(wbpsa);
            float ctScore = (float) ctScores.stream().mapToDouble(f -> Double.valueOf(f)).average().orElse(0);      // 0-100

            float teScore = getTempoScore(wbpsa);           // 0 - 100

            var scs = wbpsa.getSimpleChordSequence();
            var scsFirstChordRoot = scs.first().getData().getRootNote();
            int trScore = wbpsa.getWbpSource().getTransposibilityScore(scsFirstChordRoot);     // 0 - 100

            float preTargetNoteScore = getPreTargetNoteScore(wbpsa, tiling);  //  0 - 100
            float postTargetNoteScore = getPostTargetNoteScore(wbpsa, tiling);  //  0 - 100

            res = new Score(ctScore, trScore, teScore, preTargetNoteScore, postTargetNoteScore);
        }

        wbpsa.setCompatibilityScore(res);

        return res;

    }

    @Override
    public ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling)
    {
        ListMultimap<Score, WbpSourceAdaptation> mmap = MultimapBuilder.treeKeys().arrayListValues().build();


        int nbBars = scs.getBarRange().size();
        var wbpsDb = WbpSourceDatabase.getInstance();
        var wbpSources = bassStyles.size() == 1 ? wbpsDb.getWbpSources(nbBars, bassStyles.iterator().next())
                : wbpsDb.getWbpSources(nbBars).stream()
                        .filter(s -> bassStyles.contains(s.getBassStyle()))
                        .toList();


        // Check rootProfile first
        var rpScs = scs.getRootProfile();
        var rpWbpSources = wbpSources.stream()
                .filter(s -> rpScs.equals(s.getRootProfile()))
                .toList();
        for (var wbpSource : rpWbpSources)
        {
            var wbpsa = new WbpSourceAdaptation(wbpSource, scs);
            var score = computeCompatibilityScore(wbpsa, tiling);
            if (score.compareTo(WbpsaScorer.SCORE_ZERO) > 0)
            {
                mmap.put(score, wbpsa);
            }
        }

        return mmap;
    }

    // =====================================================================================================================
    // Private methods
    // =====================================================================================================================

    /**
     * +100 if previous WbpSourceAdaptation's target note matches the 1st note of our phrase.
     *
     * @param wbpsa
     * @param tiling
     * @return
     */
    private float getPreTargetNoteScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        float res = 0;
        int prevBar = wbpsa.getBarRange().from - 1;
        WbpSourceAdaptation prevWbpsa;

        if (tiling != null
                && wbpSourceAdapter != null
                && tiling.getBarRange().contains(prevBar)
                && (prevWbpsa = tiling.getWbpSourceAdaptation(prevBar)) != null
                && prevWbpsa.getWbpSource().getTargetNote() != null)
        {

            int firstNotePitch = wbpSourceAdapter.getPhrase(wbpsa).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(prevWbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? 100f : 0;
        }
        return res;
    }

    /**
     * +100 if next WbpSourceAdaptation's first note matches our target note.
     *
     * @param wbpsa
     * @param tiling
     * @return
     */
    private float getPostTargetNoteScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        float res = 0;
        int nextBar = wbpsa.getBarRange().to + 1;
        WbpSourceAdaptation nextWbpsa;

        if (tiling != null
                && wbpSourceAdapter != null
                && tiling.getBarRange().contains(nextBar)
                && (nextWbpsa = tiling.getWbpSourceAdaptation(nextBar)) != null
                && wbpsa.getWbpSource().getTargetNote() != null)
        {
            int firstNotePitch = wbpSourceAdapter.getPhrase(nextWbpsa).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(wbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? 100f : 0;
        }
        return res;
    }


    /**
     * Compute the harmony compatibility score for each chord-pair.
     * <p>
     * Return an empty list as soon as 2 incompatible chord types are found.
     *
     * @param wbpsa
     * @return
     */
    private List<Float> getHarmonyCompatibilityScores(WbpSourceAdaptation wbpsa)
    {
        List<Float> res = Collections.emptyList();

        WbpSource wbpSource = wbpsa.getWbpSource();
        SimpleChordSequence wbpSourceScs = wbpSource.getSimpleChordSequence();
        SimpleChordSequence scs = wbpsa.getSimpleChordSequence();

        if (wbpSourceScs.size() == scs.size())
        {
            res = new ArrayList<>();
            var itSrc = wbpSourceScs.iterator();
            var it = scs.iterator();
            while (itSrc.hasNext())
            {
                var csSrc = itSrc.next();
                var csTarget = it.next();
                var wbpscp = new WbpSourceChordPhrase(wbpSource, csSrc);
                var score = wbpscp.getHarmonyCompatibilityScore(csTarget.getData());
                if (score > 0)
                {
                    res.add(score);
                } else
                {
                    res.clear();
                    break;
                }
            }
        }
        return res;
    }

    public float getTempoScore(WbpSourceAdaptation wbpsa)
    {
        float res;
        var wbpSource = wbpsa.getWbpSource();
        var bassStyle = wbpSource.getBassStyle();
        assert bassStyle.is2feel() || bassStyle.isWalking() : " wbpSource=" + wbpSource;
        var stats = wbpSource.getStats();

        if (tempo <= TempoRange.MEDIUM_SLOW.getMin())
        {
            // Slow, better if additional extra notes
            if (bassStyle.is2feel())
            {
                float bonus = Math.min(stats.nbShortNotes() * 20 + stats.nbDottedEighthNotes() * 20 + stats.nbQuarterNotes() * 20, 60);
                res = 40 + bonus;
            } else
            {
                // Walking
                float bonus = Math.min(stats.nbShortNotes() * 20 + stats.nbDottedEighthNotes() * 20, 40);
                res = 60 + bonus;
            }
        } else if (tempo <= TempoRange.MEDIUM_SLOW.getMax())
        {
            // Medium slow, sightly better if additional extra notes
            if (bassStyle.is2feel())
            {
                float bonus = Math.min(stats.nbShortNotes() * 15 + stats.nbDottedEighthNotes() * 15 + stats.nbQuarterNotes() * 15, 45);
                res = 55 + bonus;
            } else
            {
                // Walking
                float bonus = Math.min(stats.nbShortNotes() * 10 + stats.nbDottedEighthNotes() * 10, 20);
                res = 80 + bonus;
            }
        } else if (tempo <= TempoRange.MEDIUM.getMax())
        {
            // Medium, everything is ok
            res = 80;
        } else if (tempo <= TempoRange.MEDIUM_FAST.getMax())
        {
            // Medium fast, too much is bad
            float malus = Math.min(stats.nbShortNotes() * 30 + stats.nbDottedEighthNotes() * 10, 70);
            res = 100 - malus;
        } else
        {
            // Fast, simple is best!
            float malus = Math.min(stats.nbShortNotes() * 60 + stats.nbDottedEighthNotes() * 30, 100);
            res = 100 - malus;
        }

        assert res >= 0 && res <= 100 : "res=" + res + " wbpSource=" + wbpSource;
        
        return res;
    }
}
