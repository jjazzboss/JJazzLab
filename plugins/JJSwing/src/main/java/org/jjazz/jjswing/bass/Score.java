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
package org.jjazz.jjswing.bass;

import com.google.common.base.Preconditions;
import java.text.DecimalFormat;
import java.util.function.Predicate;

/**
 * Compatibility score as calculated by a WbpsaScorer.
 * <p>
 * All float values must be in the [0;100] range.
 * <p>
 * compareTo(SCORE_ZERO) == 0 means incompatibility. compareTo(SCORE_ZERO) &gt; 0 means some compatibility.
 */
public record Score(float harmonicCompatibility, float transposability, float tempoCompatibility, float preTargetNoteMatch, float postTargetNoteMatch) implements Comparable<Score>
    {

    static final public Score ZERO = new Score(0, 0, 0, 0, 0);
    /**
     * Harmonic score must be &gt; 0
     */
    static final public Predicate<Score> DEFAULT_TESTER = s -> s.harmonicCompatibility > 0;
    /**
     * DEFAULT_TESTER and tempo score &gt;= 50 and transposibility score &gt;= 50
     */
    static final public Predicate<Score> PREMIUM_ONLY_TESTER = s -> DEFAULT_TESTER.test(s) && s.tempoCompatibility() >= 50 && s.transposability() >= 50;

    public static final float MAX = 100;
    private static final int CT_WEIGHT = 5;
    private static final int TR_WEIGHT = 3;
    private static final int TE_WEIGHT = 3;
    private static final int PRE_TN_WEIGHT = 2;
    private static final int POST_TN_WEIGHT = 2;
    private static final int WEIGHT_SUM = CT_WEIGHT + TR_WEIGHT + TE_WEIGHT + PRE_TN_WEIGHT + POST_TN_WEIGHT;

    public Score    
    {
        Preconditions.checkArgument(harmonicCompatibility >= 0 && harmonicCompatibility <= MAX, "chordTypeCompatibility=%s", harmonicCompatibility);
        Preconditions.checkArgument(transposability >= 0 && transposability <= MAX, "transposibility=%s", transposability);
        Preconditions.checkArgument(tempoCompatibility >= 0 && tempoCompatibility <= MAX, "tempoCompatibility=%s", tempoCompatibility);
        Preconditions.checkArgument(preTargetNoteMatch >= 0 && preTargetNoteMatch <= MAX, "preTargetNoteMatch=%s", preTargetNoteMatch);
        Preconditions.checkArgument(postTargetNoteMatch >= 0 && postTargetNoteMatch <= MAX, "postTargetNoteMatch=%s", postTargetNoteMatch);
    }

    /**
     * Combine the individual score to produce an overall compatibility score.
     *
     * @return
     */
    public float overall()
    {
        float res = 0;
        if (harmonicCompatibility > 0)
        {
            res = (CT_WEIGHT * harmonicCompatibility
                + TR_WEIGHT * transposability
                + TE_WEIGHT * tempoCompatibility
                + PRE_TN_WEIGHT * preTargetNoteMatch
                + POST_TN_WEIGHT * postTargetNoteMatch)
                / WEIGHT_SUM;
        }
        return res;
    }

    /**
     * Create a Score instance whose overall value is equal to overallTarget.
     *
     * @param overallTarget
     */
    static public Score buildSampleFromOverallValue(float overallTarget)
    {
        Preconditions.checkArgument(overallTarget >= 0 && overallTarget <= MAX, "overallValue=%s", overallTarget);

        float ct, tr = 0, te = 0, pretn = 0, posttn = 0;
        final float CT_MAX_OVERALL = CT_WEIGHT * MAX / WEIGHT_SUM;
        final float CT_TR_MAX_OVERALL = (CT_WEIGHT + TR_WEIGHT) * MAX / WEIGHT_SUM;
        final float CT_TR_TE_MAX_OVERALL = (CT_WEIGHT + TR_WEIGHT + TE_WEIGHT) * MAX / WEIGHT_SUM;
        final float CT_TR_TE_PRE_MAX_OVERALL = (CT_WEIGHT + TR_WEIGHT + TE_WEIGHT + PRE_TN_WEIGHT) * MAX / WEIGHT_SUM;

        if (overallTarget <= CT_MAX_OVERALL)
        {
            ct = WEIGHT_SUM * overallTarget / CT_WEIGHT;
        } else if (overallTarget <= CT_TR_MAX_OVERALL)
        {
            ct = MAX;
            tr = Math.min(WEIGHT_SUM * (overallTarget - CT_MAX_OVERALL) / TR_WEIGHT, MAX);
        } else if (overallTarget <= CT_TR_TE_MAX_OVERALL)
        {
            ct = tr = MAX;
            te = Math.min(WEIGHT_SUM * (overallTarget - CT_TR_MAX_OVERALL) / TR_WEIGHT, MAX);
        } else if (overallTarget <= CT_TR_TE_PRE_MAX_OVERALL)
        {
            ct = tr = te = MAX;
            pretn = Math.min(WEIGHT_SUM * (overallTarget - CT_TR_TE_MAX_OVERALL) / TR_WEIGHT, MAX);
        } else
        {
            ct = tr = te = pretn = MAX;
            posttn = Math.min(WEIGHT_SUM * (overallTarget - CT_TR_TE_PRE_MAX_OVERALL) / TR_WEIGHT, MAX);
        }

        return new Score(ct, tr, te, pretn, posttn);
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
     * Only overall() is taken into account, in order to be consistent with compareTo().
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
     * Only overall() is taken into account, in order to be consistent with compareTo().
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

        String res = String.format("[all=%s, ct=%s tr=%s te=%s pre-tn=%s post-tn=%s]",
            df.format(overall()),
            df.format(harmonicCompatibility),
            df.format(transposability),
            df.format(tempoCompatibility),
            df.format(preTargetNoteMatch),
            df.format(postTargetNoteMatch));
        return res;
    }
}
