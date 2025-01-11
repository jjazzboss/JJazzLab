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

/**
 * Relies on WbpSource.computeChordTypeCompatibilityScore() + WbpSource.getTransposibilityScore() + bonus if target notes match before/after.
 */
public class DefaultWbpsaScorer implements WbpsaScorer
{

    private final static float TARGET_NOTE_BONUS = 10f;
    private final PhraseAdapter wbpSourceAdapter;

    public DefaultWbpsaScorer(PhraseAdapter sourceAdapter)
    {
        this.wbpSourceAdapter = sourceAdapter;
    }

    @Override
    public Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        float trScore = 0;
        float preTargetNoteScore = 0;
        float postTargetNoteScore = 0;


        var ctScores = getChordTypeCompatibilityScores(wbpsa);
        float ctScore = (float) ctScores.stream().mapToDouble(f -> Double.valueOf(f)).average().orElse(0);      // 0-100


        if (ctScore > 0)
        {
            var wbpSource = wbpsa.getWbpSource();
            var scs = wbpsa.getSimpleChordSequence();
            var scsFirstChordRoot = scs.first().getData().getRootNote();
            trScore = wbpSource.getTransposibilityScore(scsFirstChordRoot) * 0.1f; // 0-10

            preTargetNoteScore = getPreTargetNoteScore(wbpsa, tiling);        // 0-10
            postTargetNoteScore = getPostTargetNoteScore(wbpsa, tiling);        // 0-10
        }

        var overall = (ctScore + trScore + preTargetNoteScore + postTargetNoteScore) * (100f / 130f);
        var res = new Score(overall);

        wbpsa.setCompatibilityScore(res);
        return res;

    }

    // =====================================================================================================================
    // Private methods
    // =====================================================================================================================

    /**
     * +TARGET_NOTE_BONUS if previous WbpSourceAdaptation's target note matches the 1st note of our phrase.
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

        if (tiling != null && wbpSourceAdapter != null
                && tiling.getBarRange().contains(prevBar) && (prevWbpsa = tiling.getWbpSourceAdaptation(prevBar)) != null)
        {
            int firstNotePitch = wbpSourceAdapter.getPhrase(wbpsa, false).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(prevWbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? TARGET_NOTE_BONUS : 0;
        }
        return res;
    }

    /**
     * +TARGET_NOTE_BONUS if next WbpSourceAdaptation's first note matches our target note.
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

        if (tiling != null && wbpSourceAdapter != null
                && tiling.getBarRange().contains(nextBar) && (nextWbpsa = tiling.getWbpSourceAdaptation(nextBar)) != null)
        {
            int firstNotePitch = wbpSourceAdapter.getPhrase(nextWbpsa, false).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(wbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? TARGET_NOTE_BONUS : 0;
        }
        return res;
    }


    /**
     * Compute the ChordType compatibility score for each chord-pair.
     * <p>
     * <p>
     * Return an empty list as soon as 2 incompatible chord types are found.
     *
     * @param wbpsa
     * @return
     */
    private List<Float> getChordTypeCompatibilityScores(WbpSourceAdaptation wbpsa)
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
                var cliCs1 = itSrc.next();
                var cliCs2 = it.next();
                var score = wbpSource.computeChordTypeCompatibilityScore(cliCs1, cliCs2);
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
