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
package org.jjazz.proswing.walkingbass.generator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.text.DecimalFormat;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;

/**
 * Scores the compatibility of a WbpSource with a chord sequence.
 */
public interface WbpsaScorer
{

    static public Score SCORE_ZERO = new Score(0, 0, 0, 0);

    /**
     * Compatibility score.
     * <p>
     * compareTo(SCORE_ZERO) == 0 means incompatibility. compareTo(SCORE_ZERO) &gt; 0 means some compatibility.
     */
    public record Score(float chordTypeCompatibility, float transposibility, float preTargetNoteMatch, float postTargetNoteMatch) implements Comparable<Score>
            {

        public Score   
        {
            Preconditions.checkArgument(chordTypeCompatibility >= 0 && chordTypeCompatibility <= 100, "chordTypeCompatibility=%s", chordTypeCompatibility);
            Preconditions.checkArgument(transposibility >= 0 && transposibility <= 100, "transposibility=%s", transposibility);
            Preconditions.checkArgument(preTargetNoteMatch >= 0 && preTargetNoteMatch <= 100, "preTargetNoteMatch=%s", preTargetNoteMatch);
            Preconditions.checkArgument(postTargetNoteMatch >= 0 && postTargetNoteMatch <= 100, "postTargetNoteMatch=%s", postTargetNoteMatch);
        }

        /**
         * Combine the individual score to produce an overall compatibility score.
         *
         * @return
         */
        public float overall()
        {
            float res = 0;
            if (chordTypeCompatibility > 0)
            {
                res = (6 * chordTypeCompatibility + 2 * transposibility + 1 * preTargetNoteMatch + 1 * postTargetNoteMatch) / 10;
            }
            return res;
        }

        /**
         * Create a Score instance which will the specified overall value.
         *
         * @param overallValue
         */
        static public Score buildFromOverallValue(float overallValue)
        {
            Preconditions.checkArgument(overallValue >= 0 && overallValue <= 100, "overallValue=%s", overallValue);

            float ct, tr = 0, pretn = 0, posttn = 0;
            if (overallValue <= 60)
            {
                ct = 10 * overallValue / 6;
            } else if (overallValue <= 80)
            {
                ct = 100;
                tr = 10 * (overallValue - 60) / 2;
            } else if (overallValue <= 90)
            {
                ct = tr = 100;
                pretn = 10 * (overallValue - 80) / 1;
            } else
            {
                ct = tr = pretn = 100;
                posttn = 10 * (overallValue - 90) / 1;
            }

            return new Score(ct, tr, pretn, posttn);
        }

        /**
         * Implementation is consistent with equals().
         *
         * @param o
         * @return
         */
        @Override
        public int compareTo(Score o)
        {
            return Float.compare(overall(), o.overall());
        }

        /**
         * Only overall() is takin into account, in order to be consistent with compareTo().
         *
         * @return
         */
        @Override
        public int hashCode()
        {
            int hash = 5;
            hash = 67 * hash + Float.floatToIntBits(overall());
            return hash;
        }

        /**
         * Only overall() is takin into account, in order to be consistent with compareTo().
         *
         * @return
         */

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Score other = (Score) obj;
            return Float.floatToIntBits(this.overall()) == Float.floatToIntBits(other.overall());
        }


        @Override
        public String toString()
        {
            DecimalFormat df = new DecimalFormat("#.##");

            String res = String.format("[all=%s, ct=%s tr=%s pre-tn=%s post-tn=%s]",
                    df.format(overall()),
                    df.format(chordTypeCompatibility),
                    df.format(transposibility),
                    df.format(preTargetNoteMatch),
                    df.format(postTargetNoteMatch));
            return res;
        }
    }


    /**
     * Scores the compatibility of wbpsa in the context of tiling.
     * <p>
     * Note that wbpsa's score is also updated with the returned value.
     *
     * @param wbpsa
     * @param tiling Might be null
     * @return
     */
    Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling);

    /**
     * Find the WbpSources from the WbpDatabase which are compatible with the specified chord sequence.
     * <p>
     * @param scs
     * @param tiling If null this might impact the resulting score
     * @return A multimap with Score keys in descending order
     */
    default ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling)
    {
        ListMultimap<Score, WbpSourceAdaptation> mmap = MultimapBuilder.treeKeys().arrayListValues().build();

        // Check rootProfile first
        var rpWbpSources = WbpSourceDatabase.getInstance().getWbpSources(scs.getRootProfile());
        for (var wbpSource : rpWbpSources)
        {
            var wbpsa = new WbpSourceAdaptation(wbpSource, scs);
            var score = computeCompatibilityScore(wbpsa, tiling);
            if (score.compareTo(WbpsaScorer.SCORE_ZERO) > 0)
            {
                mmap.put(score, wbpsa);
            }
        }

        return mmap;
    }

}
