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

import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.utilities.api.IntRange;

/**
 * Defines how a standard WbpSource is adapted to a part of a song.
 * <p>
 * NOTE: Comparable implementation is implemented so that natural order is by descending compatibility score.
 */
public class WbpSourceAdaptation implements Comparable<WbpSourceAdaptation>
{

    public static final int MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE = 56;

    /**
     *
     */
    public record CompatibilityScore(float overall, float ctScore, float trScore)
            {

    }
    private final WbpSource wbpSource;
    private final SimpleChordSequence simpleChordSequence;
    private CompatibilityScore compatibilityScore;
    private static final Logger LOGGER = Logger.getLogger(WbpSourceAdaptation.class.getSimpleName());

    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs)
    {
        this.wbpSource = wbpSource;
        this.simpleChordSequence = scs;
        computeCompatibilityScore();
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

    public CompatibilityScore getCompatibilityScore()
    {
        return compatibilityScore;
    }

    /**
     * Note that natural order is by DESCENDING overall compatibility score.
     *
     * @param other
     * @return
     */
    @Override
    public int compareTo(WbpSourceAdaptation other)
    {
        return -Float.compare(getCompatibilityScore().overall(), other.getCompatibilityScore().overall());
    }

    // ===================================================================================================
    // Private methods
    // ===================================================================================================
    private void computeCompatibilityScore()
    {
        // ChordType
        // max value=63 if both chordtypes are equal
        // Same 3rd=32, +fifth=48, +seventh/sixth=56, +ext1=60; +ext2=62        
        float ctScore = simpleChordSequence.getChordTypeSimilarityScore(wbpSource.getSimpleChordSequence(), MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE, true);


        // Transposition
        var cliCs = simpleChordSequence.first();
        float trScore = wbpSource.getTransposibilityScore(cliCs.getData().getRootNote()) * 0.05f;  // [0;5] 5 is best


        // Overall
        float overall = ctScore + trScore;

        compatibilityScore = new CompatibilityScore(overall, ctScore, trScore);
    }


    public String toString2()
    {
        return "wbpsa{" + getBarRange() + " " + getWbpSource() + "}";
    }

    @Override
    public String toString()
    {
        var wbps = getWbpSource();
        return "wbpsa{" + compatibilityScore + " " + getBarRange() + " " + wbps.getId() + " " + wbps.getSimpleChordSequence() + "}";
    }
}
