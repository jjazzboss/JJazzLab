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

import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.WbpSource;

/**
 * Associates a WbpSource to a chord sequence and stores their compatibility score.
 * <p>
 * NOTE: Comparable implementation is implemented so that natural order is by descending compatibility score.
 */
public record WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs, float compatibilityScore) implements Comparable<WbpSourceAdaptation>
{
   /**
     * Note that natural order is by DESCENDING overall compatibility score.
     *
     * @param other
     * @return
     */
    @Override
    public int compareTo(WbpSourceAdaptation other)
    {
        return -Float.compare(compatibilityScore(), other.compatibilityScore());
    }
}
//{
//
//    /**
//     * A score representing how much a WbpSource is compatible with a chord sequence.
//     *
//     * @param ctScore Chord type compatibility 0-63
//     * @param trScore Transposition compatibility 0-5
//     * @param bonus   A bonus (or malus) added to the overall score
//     */
//    public record CompatibilityScore(float ctScore, float trScore, float bonus)
//            {
//
//        public float overall()
//        {
//            return ctScore + trScore + bonus;
//        }
//    }
//    private final WbpSource wbpSource;
//    private final SimpleChordSequence simpleChordSequence;
//    private final int minIndividualChordTypeCompatibilityScore;
//    private CompatibilityScore compatibilityScore;
//    private static final Logger LOGGER = Logger.getLogger(WbpSourceAdaptation.class.getSimpleName());
//
//    /**
//     * Create an object with minIndividualChordTypeCompatibilityScore=DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE
//     *
//     * @param wbpSource
//     * @param scs
//     */
//    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs)
//    {
//        this(wbpSource, scs, DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE);
//    }
//
//    /**
//     *
//     * @param wbpSource
//     * @param scs
//     * @param minIndividualChordTypeCompatibilityScore If one chordType has a compatibility less than this, global score will be 0.
//     */
//    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs, int minIndividualChordTypeCompatibilityScore)
//    {
//        this.wbpSource = wbpSource;
//        this.simpleChordSequence = scs;
//        this.minIndividualChordTypeCompatibilityScore = minIndividualChordTypeCompatibilityScore;
//        computeCompatibilityScore();
//    }
//
//    public IntRange getBarRange()
//    {
//        return simpleChordSequence.getBarRange();
//    }
//
//    public WbpSource getWbpSource()
//    {
//        return wbpSource;
//    }
//
//    public SimpleChordSequence getSimpleChordSequence()
//    {
//        return simpleChordSequence;
//    }
//
//    public CompatibilityScore getCompatibilityScore()
//    {
//        return compatibilityScore;
//    }
//
//    public void setCompatibilityBonus(int bonus)
//    {
//        compatibilityScore = new CompatibilityScore(compatibilityScore.ctScore, compatibilityScore.trScore, bonus);
//    }
//
//    /**
//     * Note that natural order is by DESCENDING overall compatibility score.
//     *
//     * @param other
//     * @return
//     */
//    @Override
//    public int compareTo(WbpSourceAdaptation other)
//    {
//        return -Float.compare(getCompatibilityScore().overall(), other.getCompatibilityScore().overall());
//    }
//
//    // ===================================================================================================
//    // Private methods
//    // ===================================================================================================
//    private void computeCompatibilityScore()
//    {
//        float ctScore = wbpSource.getChordTypeCompatibilityScore(simpleChordSequence, minIndividualChordTypeCompatibilityScore);   // 0-63
//        float trScore = wbpSource.getTransposibilityScore(simpleChordSequence.first().getData().getRootNote()) * 0.05f;  // 0-5
//        compatibilityScore = new CompatibilityScore(ctScore, trScore, 0);
//    }
//
//
//    public String toString2()
//    {
//        return "wbpsa{" + getBarRange() + " " + getWbpSource() + "}";
//    }
//
//    @Override
//    public String toString()
//    {
//        var wbps = getWbpSource();
//        return "wbpsa{" + compatibilityScore + " " + getBarRange() + " " + wbps.getId() + " " + wbps.getSimpleChordSequence() + "}";
//    }
//}
