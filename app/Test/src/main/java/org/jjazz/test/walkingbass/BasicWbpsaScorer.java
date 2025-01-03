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

import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;
import org.jjazz.test.walkingbass.generator.WbpTiling;

/**
 * Use ChordSequence.getAverageChordTypeSimilarityScore() + WbpSource.getTransposibilityScore() + bonus if target notes match before/after.
 */
public class BasicWbpsaScorer implements WbpsaScorer
{
    private static final boolean ACCEPT_ABSENT_DEGREES = true;
    private static final int DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE = 56; // at least 3rd & 5th & 6/7 match
    private final int minIndividualChordTypeCompatibilityScore;
    private final WbpSourceAdapter wbpSourceAdapter;

    public BasicWbpsaScorer(WbpSourceAdapter sourceAdapter)
    {
        this(sourceAdapter, DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE);
    }


    public BasicWbpsaScorer(WbpSourceAdapter sourceAdapter, int minIndividualChordTypeCompatibilityScore)
    {
        this.minIndividualChordTypeCompatibilityScore = minIndividualChordTypeCompatibilityScore;
        this.wbpSourceAdapter = sourceAdapter;
    }

    @Override
    public float scoreCompatibility(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        var wbpSource = wbpsa.getWbpSource();
        var scsSource = wbpSource.getSimpleChordSequence();
        var scs = wbpsa.getSimpleChordSequence();
        float ctScore = scsSource.getAverageChordTypeSimilarityScore(scs, minIndividualChordTypeCompatibilityScore, ACCEPT_ABSENT_DEGREES);  // 0-63


        var scsFirstChordRoot = scs.first().getData().getRootNote();
        float trScore = wbpSource.getTransposibilityScore(scsFirstChordRoot) * 0.05f; // 0-5


        float preTargetNoteScore = getPreTargetNoteScore(wbpsa, tiling);        // 0-5
        float postTargetNoteScore = getPostTargetNoteScore(wbpsa, tiling);        // 0-5


        var res = (ctScore + trScore + preTargetNoteScore + postTargetNoteScore) * (100f / 78);
        return res;

    }

    // =====================================================================================================================
    // Private methods
    // =====================================================================================================================

    /**
     * +5 if previous WbpSourceAdaptation's target note matches the 1st note of our phrase.
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

        if (tiling.getBarRange().contains(prevBar) && (prevWbpsa = tiling.getWbpSourceAdaptation(prevBar)) != null)
        {
            int firstNotePitch = wbpSourceAdapter.getPhrase(wbpsa, false).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(prevWbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? 5 : 0;
        }
        return res;
    }

    /**
     * +5 if next WbpSourceAdaptation's first note matches our target note.
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

        if (tiling.getBarRange().contains(nextBar) && (nextWbpsa = tiling.getWbpSourceAdaptation(nextBar)) != null)
        {
            int firstNotePitch = wbpSourceAdapter.getPhrase(nextWbpsa, false).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(wbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? 5 : 0;
        }
        return res;
    }

}
