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
package org.jjazz.jjswing.walkingbass;

import com.google.common.collect.ListMultimap;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;

/**
 * Evaluates the compatibility of a tiling's WbpSourceAdaptation with a chord sequence.
 */
public interface WbpsaScorer
{

    /**
     * Compute the compatibility Score of wbpsa in the context of tiling.
     * <p>
     * Uses chord type, transposition, tempo, target notes before/after. Resulting Score is Score.ZERO if the resulting Score does not satisfy the minCompatibilityTester
     * predicate.
     * <p>
     *
     * @param wbpsa  The instance for which we will update the compatibility score (non-zero Score elements are not updated, they are directly reused)
     * @param tiling Can be null, in this case pre/post target notes scores are 0
     * @return The wbpsa Score
     * @see #DefaultWbpsaScorer(org.jjazz.jjswing.walkingbass.PhraseAdapter, int, java.util.function.Predicate, org.jjazz.jjswing.BassStyle...)
     */
    Score updateCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling);


    /**
     * Find the WbpSources from WbpDatabase which are compatible with scs (compatibility score is not Score.ZERO), and return the corresponding
     * WbpSourceAdaptations.
     * <p>
     * @param scs
     * @param tiling Can be null. Used in the compatibility score calculation.
     * @return A multimap with WbpSourceAdaptation's Score keys in ascending order
     */
    ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling);


}
