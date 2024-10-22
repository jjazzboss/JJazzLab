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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Describe how a standard WbpSource is adapted to a part of a song.
 */
public class WbpSourceAdaptation
{

    private final WbpSource wbpSource;
    private final SimpleChordSequence simpleChordSequence;
    private float compatibilityScore = Float.NaN;
    private static final Logger LOGGER = Logger.getLogger(WbpSourceAdaptation.class.getSimpleName());

    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs)
    {
        this.wbpSource = wbpSource;
        this.simpleChordSequence = scs;
    }

    public IntRange getBarRange()
    {
        return simpleChordSequence.getBarRange();
    }

    public WbpSource getWbpSource()
    {
        return wbpSource;
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return simpleChordSequence;
    }

    public float getCompatibilityScore()
    {
        if (compatibilityScore == Float.NaN)
        {
            compatibilityScore = computeCompatibilityScore();
        }
        return compatibilityScore;
    }

    // ===================================================================================================
    // Private methods
    // ===================================================================================================
    public float computeCompatibilityScore()
    {
        var ctScore = computeChortTypeCompatibilityScore();
        var tScore = computeTranspositionCompatibilityScore();
        var score = ctScore - tScore;

        LOGGER.log(Level.FINE, "computeCompatibilityScore() ctScore={0} tScore={1} score={2}", new Object[]
        {
            ctScore, score, score
        });

        return score;
    }

    /**
     * Compute a score which indicates how much wbp's chord sequence is suited to scs.
     * <p>
     * If score does not reach a minimum viable score, return value is 0.
     *
     * @return
     */
    public float computeChortTypeCompatibilityScore()
    {
        final int MIN_VIABLE_CHORDTYPE_COMPATIBILITY_SCORE = 40;  // same family=32, fifth=16, seventh/sixth=8, ext1=4; ext2=2, ext3=1
        float score = simpleChordSequence.getChordTypeSimilarityScore(wbpSource.getSimpleChordSequence());
        if (score < MIN_VIABLE_CHORDTYPE_COMPATIBILITY_SCORE)
        {
            score = 0;
        }
        return score;
    }

    public float computeTranspositionCompatibilityScore()
    {
        var cliCs = simpleChordSequence.first();
        float score = wbpSource.getTransposibilityScore(cliCs.getData().getRootNote());  // [0;100] 100 is best
        score = (100 - score) * 0.02f;
        return score;
    }
}
