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
import java.util.Objects;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.DegreeCompatibility;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.utilities.api.FloatRange;

/**
 * Relies on ChordSequence.getAverageChordTypeSimilarityScore() + WbpSource.getTransposibilityScore() + bonus if target notes match before/after.
 */
public class DefaultWbpsaScorer implements WbpsaScorer
{

    public static final int DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE = 56; // at least 3rd & 5th & 6/7 match
    public static final Score DEFAULT_MIN_WBPSOURCE_COMPATIBILITY_SCORE = new Score(DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE);
    private static final boolean ACCEPT_ABSENT_DEGREES = true;

    private final int minIndividualChordTypeCompatibilityScore;
    private final PhraseAdapter wbpSourceAdapter;

    /**
     *
     * @param sourceAdapter If null target notes match can not be taken into account
     */
    public DefaultWbpsaScorer(PhraseAdapter sourceAdapter)
    {
        this(sourceAdapter, DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE);
    }


    public DefaultWbpsaScorer(PhraseAdapter sourceAdapter, int minIndividualChordTypeCompatibilityScore)
    {
        this.minIndividualChordTypeCompatibilityScore = minIndividualChordTypeCompatibilityScore;
        this.wbpSourceAdapter = sourceAdapter;
    }

    @Override
    public Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        var wbpSource = wbpsa.getWbpSource();
        var scsSource = wbpSource.getSimpleChordSequence();
        var scs = wbpsa.getSimpleChordSequence();
        float ctScore = scsSource.getAverageChordTypeSimilarityScore(scs, minIndividualChordTypeCompatibilityScore, ACCEPT_ABSENT_DEGREES);  // 0-63


        var scsFirstChordRoot = scs.first().getData().getRootNote();
        float trScore = wbpSource.getTransposibilityScore(scsFirstChordRoot) * 0.05f; // 0-5


        float preTargetNoteScore = getPreTargetNoteScore(wbpsa, tiling);        // 0-5
        float postTargetNoteScore = getPostTargetNoteScore(wbpsa, tiling);        // 0-5


        var overall = (ctScore + trScore + preTargetNoteScore + postTargetNoteScore) * (100f / 78);
        var res = new Score(overall);

        wbpsa.setCompatibilityScore(res);
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

        if (tiling != null && wbpSourceAdapter != null
                && tiling.getBarRange().contains(prevBar) && (prevWbpsa = tiling.getWbpSourceAdaptation(prevBar)) != null)
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

        if (tiling != null && wbpSourceAdapter != null
                && tiling.getBarRange().contains(nextBar) && (nextWbpsa = tiling.getWbpSourceAdaptation(nextBar)) != null)
        {
            int firstNotePitch = wbpSourceAdapter.getPhrase(nextWbpsa, false).first().getPitch();
            int targetNotePitch = wbpSourceAdapter.getTargetNote(wbpsa).getPitch();
            res = targetNotePitch == firstNotePitch ? 5 : 0;
        }
        return res;
    }


    /**
     * Check if 2 chord types are compatible and if yes score this compatibility.
     * <p>
     * If the 2 chord types are considered incompatible, score is 0. If the 2 chord types are equal, score is 100 (max). Note that 6 and 7M degrees are
     * considered equal in this method (e.g. C6=C7M, Cm69=Cm7M9).
     * <p>
     * If chord types do not have the same nb of Degrees:<br>
     * - Score is 0 if at least 1 common degree does not match, or if wbpSourceChordType has more degrees than targetChordType<br>
     * - If targetChordType has more degrees than wbpSourceChordType (e.g. C7b9 and C): use wbpSource phrase & DegreeCompatibility method to compare the extra
     * degrees. Score is 0 if one degree is INCOMPATIBLE, otherwise score is 100 - (10 * each COMPATIBLE_NO_USE degree).<br>
     * <p>
     * Examples (wbpSourceChordType - targetChordType):<br>
     * C - Cm: 0<br>
     * C6 - Cm7M: 0<br>
     * C6 - C7: 0<br>
     * C7b9 - C9: 0<br>
     * C - C9: 90 if WbpSource phrase is DegreeCompatibility.COMPATIBLE_NO_USE with 7th (-10), and DegreeCompatibility.COMPATIBLE_USE with 9th<br>
     * C7 - C: 0<br>
     * C - C or C6 - C7M or C13b9-C13b9: 100 (max value)
     *
     * @param wbpSource
     * @param wbpSourceChordType A chord from wbpSource
     * @param targetChordType
     * @return [10; 45]
     * @see DegreeCompatibility
     */
    private float computeCompatibilityScore(WbpSource wbpSource, ChordSymbol wbpSourceChordType, ChordSymbol targetChordType)
    {
        float res = 0;
        int nbSourceDegrees = wbpSourceChordType.getNbDegrees();
        int nbTargetDegrees = targetChordType.getNbDegrees();


        if (wbpSourceChordType.equals(targetChordType) || isSixthSpecialCompatible(wbpSourceChordType, targetChordType))
        {
            res = 100;

        } else if (nbSourceDegrees < nbTargetDegrees && wbpSourceChordType.getNbCommonDegrees(targetChordType) == nbSourceDegrees)
        {
            // Need to compare target extra degrees with implicit degrees taken from the WbpSource phrase
            for (int i = nbSourceDegrees; i < nbTargetDegrees; i++)
            {
                var dTarget = targetChordType.getDegrees().get(i);
                var srcNo
                var srcPhrase = wbpSource.getSizedPhrase();
                srcPhrase = srcPhrase.subSet(FloatRange.MAX_FLOAT_RANGE, ACCEPT_ABSENT_DEGREES); // notes used during the chord
                var dc=DegreeCompatibility.get(notes, dTarget);
            }
        }

        return res;
    }


    /**
     * Compute the average of the similarity scores of ChordTypes from ChordSequence and cSeq.
     * <p>
     *
     * @param cSeq                                  Should have the same number of chord symbols than this SimpleChordSequence.
     * @param minIndividualChordTypeSimilarityScore If compatibility score for an individual chord is &lt; this value, method will return 0 whatever the other
     *                                              chords.
     * @param acceptAbsentDegrees                   See ChordType.getSimilarityScore()
     * @return [0;63] 0 if ChordSequences don't have the same size, or if minIndividualChordTypeSimilarityScore threshold was not reached
     * @see org.jjazz.harmony.api.ChordType#getSimilarityScore(org.jjazz.harmony.api.ChordType, boolean)
     */
    private float getAverageChordTypeSimilarityScore(ChordSequence cSeq, int minIndividualChordTypeSimilarityScore, boolean acceptAbsentDegrees)
    {
        float res = 0;
        var scores = getChordTypeSimilarityScores(cSeq, acceptAbsentDegrees);
        if (!scores.isEmpty())
        {
            for (var score : scores)
            {
                if (score < minIndividualChordTypeSimilarityScore)
                {
                    res = 0;
                    break;
                }
                res += score;
            }
            res /= scores.size();
        }
        return res;
    }

    /**
     * Get the ChordType similarity score of each chord from this ChordSequence and cSeq.
     * <p>
     * @param cSeq                Should have the same number (&gt;0) of chord symbols than this SimpleChordSequence.
     * @param acceptAbsentDegrees See ChordType.getSimilarityScore()
     * @return A list if Integer [0;63]. Can be empty if ChordSequences are empty or do not have the same size
     * @see org.jjazz.harmony.api.ChordType#getSimilarityScore(org.jjazz.harmony.api.ChordType, boolean)
     */
    private List<Integer> getChordTypeSimilarityScores(ChordSequence cSeq, boolean acceptAbsentDegrees)
    {
        Objects.requireNonNull(cSeq);
        List<Integer> res = Collections.emptyList();
        if (size() == cSeq.size())
        {
            res = new ArrayList<>();
            var it1 = this.iterator();
            var it2 = cSeq.iterator();
            while (it1.hasNext())
            {
                var cliCs1 = it1.next();
                var cliCs2 = it2.next();
                int score = cliCs1.getData().getChordType().getSimilarityScore(cliCs2.getData().getChordType(), acceptAbsentDegrees);
                res.add(score);
            }
        }
        return res;
    }

    private boolean isSixthSpecialCompatible(ChordType ct1, ChordType ct2)
    {
        String n6 = ct1.getName(), n7 = ct2.getName();
        if (!n6.contains("6"))
        {
            n6 = ct2.getName();
            n7 = ct1.getName();
        }
        boolean b = ((n6.equals("6") && (n7.equals("M7") || n7.equals("M13")))
                || (n6.equals("69") && (n7.equals("M9") || n7.equals("M13")))
                || (n6.equals("m6") && n7.equals("m7M"))
                || (n6.equals("m69") && n7.equals("m97M")));
        return b;
    }
}
