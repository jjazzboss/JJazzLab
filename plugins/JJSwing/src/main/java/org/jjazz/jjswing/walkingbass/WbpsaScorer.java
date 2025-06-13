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

import org.jjazz.jjswing.walkingbass.db.WbpSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythm.api.TempoRange;

/**
 * Compute the compatibility Score of a WbpSourceAdaptation.
 * <p>
 * @see Score
 */
public class WbpsaScorer
{

    public static final float DEFAULT_TARGET_NOTE_SCORE = 50;
    private final PhraseAdapter wbpSourceAdapter;
    private final Predicate<Score> minCompatibilityTester;
    private static final Logger LOGGER = Logger.getLogger(WbpsaScorer.class.getSimpleName());

    /**
     * @param sourceAdapter If null target note matching score will not impact the overall score
     * @param minCompatibilityTester If null use Score.DEFAULT_TESTER.
     * @see Score#DEFAULT_TESTER
     */
    public WbpsaScorer(PhraseAdapter sourceAdapter, Predicate<Score> minCompatibilityTester)
    {
        this.wbpSourceAdapter = sourceAdapter;
        this.minCompatibilityTester = minCompatibilityTester == null ? Score.DEFAULT_TESTER : minCompatibilityTester;
    }

    /**
     * Compute and update the compatibility Score of a WbpSourceAdaptation using chord type, transposition, tempo, pre/post target notes.
     * <p>
     * Score is computed only for wbpsa score elements which are equal to 0, i.e. non-zero elements are directly reused in the resulting Score. Result
     * is Score.ZERO if the resulting Score does not satisfy the minCompatibilityTester predicate. The passed wbpsa instance is updated with the new
     * Score.
     * <p>
     *
     * @param wbpsa
     * @param tiling Can be null, in this case pre/post target notes scores are 0
     * @param tempo If &lt;= 0 tempo is ignored in score computing
     * @return The wbpsa Score
     */
    public Score updateCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling, int tempo)
    {
        Objects.requireNonNull(wbpsa);

        Score res = wbpsa.getCompatibilityScore();

        var wbpSource = wbpsa.getWbpSource();

        if (JJSwingBassMusicGeneratorSettings.getInstance().isAcceptNonChordBassStartNote() || wbpSource.isStartingOnChordBass())
        {
            float preTargetNoteScore = DEFAULT_TARGET_NOTE_SCORE;
            float postTargetNoteScore = DEFAULT_TARGET_NOTE_SCORE;

            if (wbpSourceAdapter != null && tiling != null)
            {
                // We can evaluate pre/post notes matching
                if (wbpsa.getAdaptedPhrase() == null)
                {
                    wbpsa.setAdaptedPhrase(wbpSourceAdapter.getPhrase(wbpsa));
                }
                if (wbpsa.getAdaptedTargetPitch() == -1)
                {
                    wbpsa.setAdaptedTargetPitch(wbpSourceAdapter.getTargetPitch(wbpsa));        // Can be -1
                }
                if (res.preTargetNoteMatch() == 0)
                {
                    preTargetNoteScore = computePreTargetNoteScore(wbpsa, tiling, wbpsa.getAdaptedPhrase().first().getPitch());
                }
                if (res.postTargetNoteMatch() == 0)
                {
                    postTargetNoteScore = computePostTargetNoteScore(wbpsa, tiling);
                }
            }

            // Check constraints on non-root start note and non-chord-tone last note: 
            // - if start note is not root, make sure it's the target note of the previous wbpsa
            // - if last note is not a chord tone, make sure next wbpsa matches our target note
            boolean startAndEndNotesCheck = (wbpSource.isStartingOnChordBass() || preTargetNoteScore == 100f) && (wbpSource.isEndingOnChordTone() || postTargetNoteScore == 100f);

            if (startAndEndNotesCheck)
            {

                // Harmonic compatibility
                float ctScore = res.harmonicCompatibility();
                if (ctScore == 0)
                {
                    var ctScores = getHarmonicCompatibilityScores(wbpsa);
                    ctScore = (float) ctScores.stream().mapToDouble(f -> Double.valueOf(f)).average().orElse(0);      // 0-100
                }

                // Tempo compatibility
                float teScore = res.tempoCompatibility();
                if (teScore == 0 && tempo > 0)
                {
                    teScore = getTempoScore(wbpsa, tempo);           // 0 - 100
                }

                // Transposability
                float trScore = res.transposability();
                if (trScore == 0)
                {
                    var scs = wbpsa.getSimpleChordSequence();
                    var scsFirstChordRoot = scs.first().getData().getRootNote();
                    trScore = wbpsa.getWbpSource().getTransposabilityScore(scsFirstChordRoot);     // 0 - 100
                }

                // Final score
                var s = new Score(ctScore, trScore, teScore, preTargetNoteScore, postTargetNoteScore);
                res = minCompatibilityTester.test(s) ? s : Score.ZERO;

            } else
            {
//                if (wbpsaTargetPitch > -1 && nextWbpsaFirstPitch > -1)
//                {
//                    LOGGER.log(Level.SEVERE, "computeCompatibilityScore() DBG last note tone/mismatch wbpsaTargetPitch={0} nextWbpsaFirstPitch={1}",
//                            new Object[]
//                            {
//                                wbpsaTargetPitch,
//                                nextWbpsaFirstPitch
//                            });
//                }
            }

        }

        wbpsa.setCompatibilityScore(res);

        return res;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    /**
     * Depend on target note matching + same-pitch notes between wbpsas.
     *
     * @param wbpsa
     * @param tiling
     * @param firstNotePitch
     * @return
     */
    private float computePreTargetNoteScore(WbpSourceAdaptation wbpsa, WbpTiling tiling, int firstNotePitch)
    {
        float score;
        if (getPrevWbpsaTargetPitch(wbpsa, tiling) == firstNotePitch)
        {
            // Perfect
            score = 100f;
        } else if (getPrevWbpsaLastPitch(wbpsa, tiling) == firstNotePitch)
        {
            // Worst case
            score = 0f;
        } else
        {
            // Default
            score = DEFAULT_TARGET_NOTE_SCORE;
        }
        return score;
    }

    /**
     * Depend on target note matching + same-pitch notes between wbpsas.
     *
     * @param wbpsa
     * @param tiling
     * @return
     */
    private float computePostTargetNoteScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int nextWbpsaFirstPitch = getNextWbpsaFirstPitch(wbpsa, tiling);        // Can be -1
        int targetPitch = wbpsa.getAdaptedTargetPitch();
        float score;
        if (targetPitch != -1 && nextWbpsaFirstPitch == targetPitch)
        {
            // Perfect
            score = 100f;
        } else if (nextWbpsaFirstPitch == wbpsa.getAdaptedPhrase().last().getPitch())
        {
            // Worst case
            score = 0f;
        } else
        {
            // Default
            score = DEFAULT_TARGET_NOTE_SCORE;
        }
        return score;
    }

    /**
     * Get the (optional) target pitch of the previous WbpSourceAdaptation.
     *
     * @param wbpsa
     * @param tiling
     * @return -1 if no target note available
     */
    private int getPrevWbpsaTargetPitch(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        WbpSourceAdaptation prevWbpsa = getPrevWbpsa(wbpsa, tiling);
        int res = prevWbpsa == null ? -1 : prevWbpsa.getAdaptedTargetPitch();
        return res;
    }

    /**
     * Get the (optional) last pitch of previous WbpSourceAdaptation.
     *
     * @param wbpsa
     * @param tiling
     * @return -1 if no note available
     */
    private int getPrevWbpsaLastPitch(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int res = -1;
        WbpSourceAdaptation prevWbpsa = getPrevWbpsa(wbpsa, tiling);
        if (prevWbpsa != null)
        {
            var p = prevWbpsa.getAdaptedPhrase();
            res = p != null ? p.last().getPitch() : -1;
        }
        return res;
    }

    /**
     * Get the (optional) first pitch of next WbpSourceAdaptation.
     *
     * @param wbpsa
     * @param tiling
     * @return -1 if no target note available
     */
    private int getNextWbpsaFirstPitch(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int res = -1;
        WbpSourceAdaptation nextWbpsa = getNextWbpsa(wbpsa, tiling);
        if (nextWbpsa != null)
        {
            var p = nextWbpsa.getAdaptedPhrase();
            res = p != null ? p.first().getPitch() : -1;
        }
        return res;
    }

    /**
     * Get the WbpSourceAdaptation just before wbpsa.
     *
     * @param wbpsa
     * @param tiling
     * @return Can not be null
     */
    private WbpSourceAdaptation getPrevWbpsa(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int prevBar = wbpsa.getBarRange().from - 1;
        WbpSourceAdaptation res = tiling.getWbpSourceAdaptation(prevBar);
        return res;
    }

    /**
     * Get the next Wbpsa right after wbpsa.
     *
     * @param wbpsa
     * @param tiling
     * @return Can not be null
     */
    private WbpSourceAdaptation getNextWbpsa(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int nextBar = wbpsa.getBarRange().to + 1;
        WbpSourceAdaptation res = tiling.getWbpSourceAdaptation(nextBar);
        return res;
    }

    /**
     * Compute the harmony compatibility score for each chord-pair.
     * <p>
     *
     * @param wbpsa
     * @return An empty list (if 2 incompatible chords types are found) or a list of float values in the [1;100] range
     */
    private List<Float> getHarmonicCompatibilityScores(WbpSourceAdaptation wbpsa)
    {
        List<Float> res = Collections.emptyList();

        WbpSource wbpSource = wbpsa.getWbpSource();
        SimpleChordSequence wbpSourceScs = wbpSource.getSimpleChordSequence();
        SimpleChordSequence scs = wbpsa.getSimpleChordSequence();

        if (wbpSourceScs.size() == scs.size())
        {
            res = new ArrayList<>();
            var itSrc = wbpSourceScs.iterator();
            var it = scs.iterator();
            while (itSrc.hasNext())
            {
                var csSrc = itSrc.next();
                var csTarget = it.next();
                var slice = wbpSource.getSlice(csSrc);
                var score = slice.getHarmonicCompatibilityScore(csTarget.getData());
                if (score > 0)
                {
                    res.add(score);
                } else
                {
                    res.clear();
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Compute the tempo score.
     * <p>
     * Return value will be &lt; 50 if there is a significant incompatibility.
     *
     * @param wbpsa
     * @return
     */
    public float getTempoScore(WbpSourceAdaptation wbpsa, int tempo)
    {
        float res;
        var wbpSource = wbpsa.getWbpSource();
        var bassStyle = wbpSource.getBassStyle();
        var stats = wbpSource.getStats();

        res = switch (bassStyle)
        {
            case TWO_FEEL_CUSTOM, WALKING_CUSTOM, WALKING_DOUBLE_CUSTOM ->
                60;

            case TWO_FEEL ->
            {
                if (tempo <= TempoRange.MEDIUM_SLOW.getMin())       // < 75
                {
                    // Slow, better if many short notes
                    float bonus = stats.nbShortNotes() * 10 + stats.nbDottedEighthNotes() * 10 + stats.nbQuarterNotes() * 10;
                    yield 60 + bonus;

                } else if (tempo <= TempoRange.MEDIUM_SLOW.getMax())    // < 115
                {
                    // Medium slow, sightly better if additional extra notes
                    float bonus = stats.nbShortNotes() * 5 + stats.nbDottedEighthNotes() * 5 + stats.nbQuarterNotes() * 5;
                    yield 60 + bonus;

                } else if (tempo <= TempoRange.MEDIUM.getMax())     // < 135
                {
                    // Medium, everything is ok
                    yield 80;

                } else if (tempo <= TempoRange.MEDIUM_FAST.getMax())        // < 180
                {
                    // Medium fast, from 2 short notes we'll be < 50
                    yield 100 - stats.nbShortNotes() * 30;

                } else
                {
                    // Medium fast, from one short note we'll be < 50
                    yield 100 - stats.nbShortNotes() * 60;

                }
            }

            case WALKING ->
            {
                if (tempo <= TempoRange.MEDIUM_SLOW.getMin())       // < 75
                {
                    // Slow, better if many short notes
                    float bonus = stats.nbShortNotes() * 10 + stats.nbDottedEighthNotes() * 10;
                    yield 60 + bonus;

                } else if (tempo <= TempoRange.MEDIUM_SLOW.getMax())    // < 115
                {
                    // Medium slow, sightly better if additional extra notes
                    float bonus = stats.nbShortNotes() * 5 + stats.nbDottedEighthNotes() * 5;
                    yield 60 + bonus;

                } else if (tempo <= TempoRange.MEDIUM.getMax())     // < 135
                {
                    // Medium, everything is ok
                    yield 80;

                } else if (tempo <= TempoRange.MEDIUM_FAST.getMax())        // < 180
                {
                    // Medium fast, from 2 short notes we'll be < 50
                    yield 100 - stats.nbShortNotes() * 30;

                } else
                {
                    // Medium fast, from one short note we'll be < 50
                    yield 100 - stats.nbShortNotes() * 60;
                }
            }

            default ->
                throw new IllegalStateException("bassStyle=" + bassStyle);
        };

        res = Math.clamp(res, 0, 100);

        return res;
    }
}
