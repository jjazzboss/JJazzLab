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
import org.jjazz.utilities.api.IntRange;

/**
 * Associates a WbpSource to a chord sequence, with a compatibility score.
 * <p>
 * NOTE: Comparable implementation is implemented so that natural order is by descending compatibility score.
 */
public class WbpSourceAdaptation implements Comparable<WbpSourceAdaptation>
{

    private WbpsaScorer.Score compatibilityScore;
    private final WbpSource wbpSource;
    private final SimpleChordSequence scs;

    /**
     * Create an object with a score=0.
     *
     * @param wbpSource
     * @param scs
     */
    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs)
    {
        this(wbpSource, scs, WbpsaScorer.SCORE_ZERO);
    }

    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs, WbpsaScorer.Score compatibilityScore)
    {
        this.wbpSource = wbpSource;
        this.scs = scs;
        this.compatibilityScore = compatibilityScore;
    }

    public WbpSource getWbpSource()
    {
        return wbpSource;
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return scs;
    }

    public WbpsaScorer.Score getCompatibilityScore()
    {
        return compatibilityScore;
    }

    public void setCompatibilityScore(WbpsaScorer.Score compatibilityScore)
    {
        this.compatibilityScore = compatibilityScore;
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
        return -Float.compare(compatibilityScore.overall(), other.compatibilityScore.overall());
    }

    /**
     * Same as scs.getBarRange().
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return scs.getBarRange();
    }

    @Override
    public String toString()
    {
        return "wbpsa{" + compatibilityScore + " " + getBarRange() + " " + wbpSource.getId() + " " + wbpSource.getSimpleChordSequence() + "}";
    }

    public String toString2()
    {
        return "wbpsa{" + getBarRange() + " " + wbpSource + "}";
    }

}
