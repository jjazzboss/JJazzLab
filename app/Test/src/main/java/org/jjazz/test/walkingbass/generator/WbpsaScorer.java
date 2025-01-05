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

import com.google.common.base.Preconditions;
import java.util.TreeSet;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.WbpDatabase;

/**
 * Scores the compatibility of a WbpSource with a chord sequence.
 */
public interface WbpsaScorer
{

    static public Score SCORE_ZERO = new Score(0);

    /**
     * Compatibility score.
     * <p>
     * Just one parameter for now but will probably be extended for other needs.
     *
     * @param overall [0;100]
     */
    public record Score(float overall) implements Comparable<Score>
            {

        public Score
        {
            Preconditions.checkArgument(overall >= 0 && overall <= 100, "overall=%s", overall);
        }

        @Override
        public int compareTo(Score o)
        {
            return Float.compare(overall, o.overall());
        }
    }


    /**
     * Scores the compatibility of wbpsa in the context of tiling.
     * <p>
     * Note that wbpsa's score is also updated with the returned value.
     *
     * @param wbpsa
     * @param tiling Might be null
     * @return [0-100] 100 being the maximum compatibility
     */
    Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling);

    /**
     * Find the WbpSources from the WbpDatabase which match the specified chord sequence.
     * <p>
     * @param scs
     * @param tiling   If null this might impact the resulting score
     * @param minScore Return only adaptations whose score is equal or greater
     * @return An ordered list by descending compatibility
     */
    default TreeSet<WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling, Score minScore)
    {
        TreeSet<WbpSourceAdaptation> res = new TreeSet<>();

        var rpWbpSources = WbpDatabase.getInstance().getWbpSources(scs.getRootProfile());
        for (var wbpSource : rpWbpSources)
        {
            var wbpsa = new WbpSourceAdaptation(wbpSource, scs);
            if (computeCompatibilityScore(wbpsa, tiling).compareTo(minScore) >= 0)
            {
                res.add(wbpsa);
            }
        }

        return res;
    }

}
