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

import com.google.common.base.Preconditions;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * Associates a WbpSource to a chord sequence, with a compatibility score.
 * <p>
 * NOTE: Comparable implementation is implemented so that natural order is by descending compatibility score. Comparable implementation is NOT consistent with
 * equal(), so WbpSourceAdaptation should NOT be used in a SortedSet or SortedMap.
 */
public class WbpSourceAdaptation implements Comparable<WbpSourceAdaptation>
{

    private Score compatibilityScore;
    private final WbpSource wbpSource;
    private final SimpleChordSequence scs;
    private Phrase adaptedPhrase;
    private int targetPitch;


    /**
     * Create an object with a score=0.
     *
     * @param wbpSource
     * @param scs
     */
    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs)
    {
        this(wbpSource, scs, Score.ZERO);
    }

    public WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs, Score compatibilityScore)
    {
        this.wbpSource = wbpSource;
        this.scs = scs;
        this.compatibilityScore = compatibilityScore;
        this.adaptedPhrase = null;
        this.targetPitch = -1;
    }

    public WbpSource getWbpSource()
    {
        return wbpSource;
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return scs;
    }

    public Score getCompatibilityScore()
    {
        return compatibilityScore;
    }

    public void setCompatibilityScore(Score compatibilityScore)
    {
        this.compatibilityScore = compatibilityScore;
    }

    /**
     * Set the adapted phrase for this WbpSourceAdaptation.
     *
     * @param p
     */
    public void setAdaptedPhrase(Phrase p)
    {
        adaptedPhrase = p;
    }

    /**
     * Get the adapted phrase as set by {@link #setAdaptedPhrase(org.jjazz.phrase.api.Phrase) }.
     *
     * @return Can be null
     */
    public Phrase getAdaptedPhrase()
    {
        return adaptedPhrase;
    }

    /**
     * Get the adapted target pitch as set by {@link #setAdaptedTargetPitch(int) }.
     *
     * @return -1 if not set
     */
    public int getAdaptedTargetPitch()
    {
        return targetPitch;
    }

    /**
     * Set the adapted target pitch.
     *
     * @param targetPitch Use -1 to unset
     */
    public void setAdaptedTargetPitch(int targetPitch)
    {
        Preconditions.checkArgument(targetPitch >= -1 && targetPitch < 128, "targetPitch=%s", targetPitch);
        this.targetPitch = targetPitch;
    }

    /**
     * Rely on Score.compareTo() whose natural ordering is by DESCENDING overall compatibility score.
     *
     * @param other
     * @return
     */
    @Override
    public int compareTo(WbpSourceAdaptation other)
    {
        return compatibilityScore.compareTo(other.compatibilityScore);
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
