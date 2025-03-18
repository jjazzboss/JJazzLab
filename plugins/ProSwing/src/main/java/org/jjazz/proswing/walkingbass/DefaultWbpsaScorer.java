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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythm.api.TempoRange;

/**
 * Evaluates the compatibility of a WbpSourceAdaptation using chord type, transposition, tempo, target notes before/after.
 * <p>
 */
public class DefaultWbpsaScorer implements WbpsaScorer
{

    private final PhraseAdapter wbpSourceAdapter;
    private final int tempo;
    private final EnumSet<BassStyle> bassStyles;
    private final Predicate<Score> minCompatibilityTester;

    /**
     *
     * @param sourceAdapter          If null target note matching score will not impact the overall score
     * @param tempo                  If &lt;= 0 tempo is ignored in score computing
     * @param minCompatibilityTester If null use the default value (!Score.ZERO.equals(s)). Used by
     *                               {@link #computeCompatibilityScore(org.jjazz.proswing.walkingbass.WbpSourceAdaptation, org.jjazz.proswing.walkingbass.WbpTiling)}
     * @param bStyles                Accept only WbpSources which match these bassStyle(s). If empty any BassStyle is accepted
     */
    public DefaultWbpsaScorer(PhraseAdapter sourceAdapter, int tempo, Predicate<Score> minCompatibilityTester, BassStyle... bStyles)
    {
        this.wbpSourceAdapter = sourceAdapter;
        this.tempo = tempo;
        this.bassStyles = EnumSet.allOf(BassStyle.class);
        this.minCompatibilityTester = minCompatibilityTester == null ? s -> !Score.ZERO.equals(s) : minCompatibilityTester;
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

    /**
     *
     * @param wbpsa
     * @param tiling
     * @return If the resulting Score does not satisfy the minCompatibilityTester predicate, returned score is Score.ZERO.
     * @see #DefaultWbpsaScorer(org.jjazz.proswing.walkingbass.PhraseAdapter, int, java.util.function.Predicate, org.jjazz.proswing.BassStyle...)
     */
    @Override
    public Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        Score res = Score.ZERO;

        if (bassStyles.contains(wbpsa.getWbpSource().getBassStyle()))
        {
            var ctScores = getHarmonyCompatibilityScores(wbpsa);
            float ctScore = (float) ctScores.stream().mapToDouble(f -> Double.valueOf(f)).average().orElse(0);      // 0-100

            float teScore = getTempoScore(wbpsa);           // 0 - 100

            var scs = wbpsa.getSimpleChordSequence();
            var scsFirstChordRoot = scs.first().getData().getRootNote();
            int trScore = wbpsa.getWbpSource().getTransposabilityScore(scsFirstChordRoot);     // 0 - 100

            float preTargetNoteScore = getPreTargetNoteScore(wbpsa, tiling);  //  0 - 100
            float postTargetNoteScore = getPostTargetNoteScore(wbpsa, tiling);  //  0 - 100

            var s = new Score(ctScore, trScore, teScore, preTargetNoteScore, postTargetNoteScore);
            res = minCompatibilityTester.test(s) ? s : Score.ZERO;
        }

        wbpsa.setCompatibilityScore(res);

        return res;

    }

    @Override
    public ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling)
    {
        // Score in ascending order
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
            if (score.compareTo(Score.ZERO) > 0)
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
     *
     * @param wbpsa
     * @return An empty list (if 2 incompatible chords types are found) or a list of float values in the [0;100] range
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

    /**
     * Compute the tempo score.
     * <p>
     * Return value will be &lt; 50 if there is a significant incompatibility.
     *
     * @param wbpsa
     * @return
     */
    public float getTempoScore(WbpSourceAdaptation wbpsa)
    {
        float res;
        var wbpSource = wbpsa.getWbpSource();
        var bassStyle = wbpSource.getBassStyle();
        assert bassStyle.is2feel() || bassStyle.isWalking() : " wbpSource=" + wbpSource;
        var stats = wbpSource.getStats();

        if (tempo <= TempoRange.MEDIUM_SLOW.getMin())       // < 75
        {
            // Slow, better if many short notes
            if (bassStyle.is2feel())
            {
                float bonus = stats.nbShortNotes() * 10 + stats.nbDottedEighthNotes() * 10 + stats.nbQuarterNotes() * 10;
                res = 60 + bonus;
            } else
            {
                // Walking
                float bonus = stats.nbShortNotes() * 10 + stats.nbDottedEighthNotes() * 10;
                res = 60 + bonus;
            }
            
        } else if (tempo <= TempoRange.MEDIUM_SLOW.getMax())    // < 115
        {
            // Medium slow, sightly better if additional extra notes
            if (bassStyle.is2feel())
            {
                float bonus = stats.nbShortNotes() * 5 + stats.nbDottedEighthNotes() * 5 + stats.nbQuarterNotes() * 5;
                res = 60 + bonus;
            } else
            {
                // Walking
                float bonus = stats.nbShortNotes() * 5 + stats.nbDottedEighthNotes() * 5;
                res = 60 + bonus;
            }
            
        } else if (tempo <= TempoRange.MEDIUM.getMax())     // < 135
        {
            // Medium, everything is ok
            res = 80;
            
        } else if (tempo <= TempoRange.MEDIUM_FAST.getMax())        // < 180
        {
            // Medium fast, from 2 short notes we'll be < 50
            res = 100 - stats.nbShortNotes() * 30;     
            
        } else
        {
            // Medium fast, from one short note we'll be < 50
            res = 100 - stats.nbShortNotes() * 60;     
            
        }

        res = Math.clamp(res, 0, 100);

        return res;
    }
}
