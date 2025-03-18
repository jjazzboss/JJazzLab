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

import com.google.common.collect.ListMultimap;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;

/**
 * Evaluates the compatibility of a tiling's WbpSourceAdaptation with a chord sequence.
 */
public interface WbpsaScorer
{

    /**
     * Scores the compatibility of wbpsa in the context of tiling.
     * <p>
     * Uses chord type, transposition, tempo, target notes before/after.
     * <p>
     * Note that wbpsa's score is also updated with the returned value.
     *
     * @param wbpsa
     * @param tiling Might be null
     * @return Score.ZERO means incompatibility
     */
    Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling);


    /**
     * Find the WbpSourceAdaptations derived from WbpDatabase which are compatible (i.e. compatibility score is not Score.ZERO) with the specified chord
     * sequence.
     * <p>
     * @param scs
     * @param tiling If null this might impact the resulting score
     * @return A multimap with Score keys in ascending order
     */
    ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling);


}
