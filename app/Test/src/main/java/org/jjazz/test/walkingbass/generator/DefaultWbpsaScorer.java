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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.test.walkingbass.WbpSourceChordPhrase;
import org.jjazz.test.walkingbass.generator.WalkingBassGenerator.BassStyle;

/**
 * Relies on WbpSource.computeChordTypeCompatibilityScore() + WbpSource.getTransposibilityScore() + bonus if target notes match before/after.
 */
public class DefaultWbpsaScorer implements WbpsaScorer
{

    private final PhraseAdapter wbpSourceAdapter;
    private final int tempo;
    private final BassStyle bassStyle;

    /**
     *
     * @param sourceAdapter If null target notes match score can not be computed
     */
    public DefaultWbpsaScorer(PhraseAdapter sourceAdapter, BassStyle bStyle, int tempo)
    {
        this.wbpSourceAdapter = sourceAdapter;
        this.tempo = tempo;
        this.bassStyle = bStyle;
    }

    @Override
    public Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        var ctScores = getHarmonyCompatibilityScores(wbpsa);
        float ctScore = (float) ctScores.stream().mapToDouble(f -> Double.valueOf(f)).average().orElse(0);      // 0-100

        var scs = wbpsa.getSimpleChordSequence();
        var scsFirstChordRoot = scs.first().getData().getRootNote();
        var trScore = wbpsa.getWbpSource().getTransposibilityScore(scsFirstChordRoot);     // 0 - 100

        var preTargetNoteScore = getPreTargetNoteScore(wbpsa, tiling);  //  0 - 100
        var postTargetNoteScore = getPostTargetNoteScore(wbpsa, tiling);  //  0 - 100

        var res = new Score(ctScore, trScore, preTargetNoteScore, postTargetNoteScore);
        wbpsa.setCompatibilityScore(res);

        return res;

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
}
