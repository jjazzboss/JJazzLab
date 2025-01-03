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

import com.google.common.base.Preconditions;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;
import org.jjazz.test.walkingbass.generator.WbpTiling;

/**
 * Score the compatibility of a WbpSourceAdaptation.
 */
public interface WbpsaScorer
{

    static public record Score(float overall)
            {

        public Score
        {
            Preconditions.checkArgument(overall >= 0 && overall <= 100, "overall=%s", overall);
        }
    }

    /**
     * Scores the compatibility of this WbpSource adaptation in the context of tiling.
     *
     * @param wbpsa
     * @param tiling
     * @return [0-100] 100 being the maximum compatibility
     */
    Score scoreCompatibility(WbpSourceAdaptation wbpsa, WbpTiling tiling);
}
