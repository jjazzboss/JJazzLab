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
import org.jjazz.jjswing.bass.db.WbpSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.utilities.api.IntRange;

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
     * @param sourceAdapter          If null, target note matching score can not be done, in this case pre/post target notes scores are 0
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
     * Score is computed only for wbpsa score elements which are equal to 0, i.e. non-zero elements are directly reused in the resulting Score. The pre/post
     * targing matching scores are 0 if tiling is null or wbpSourceAdapter is null. Result is Score.ZERO if the resulting Score does not satisfy the
     * minCompatibilityTester predicate. The passed wbpsa instance is updated with the new Score.
     * <p>
     *
     * @param wbpsa
     * @param tiling Can be null. If null pre/post target notes scores are 0 (same if wbpSourceAdapter is null)
     * @param tempo  If &lt;= 0 tempo is ignored in score computing
     * @return The wbpsa Score
     */
    public Score updateCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling, int tempo)
    {
        Objects.requireNonNull(wbpsa);

        LOGGER.log(Level.FINE, "updateCompatibilityScore() -- wbpsa={0} ", new Object[]
        {
            wbpsa
        });
        Score res = wbpsa.getCompatibilityScore();
        var wbpSource = wbpsa.getWbpSource();

        if (BassGeneratorSettings.getInstance().isAcceptNonChordBassStartNote() || wbpSource.isStartingOnChordBass())
        {

            float preTargetNoteScore = 0;
            float postTargetNoteScore = 0;
            boolean isPrePostTargetScoreComputable = wbpSourceAdapter != null && tiling != null;


            if (isPrePostTargetScoreComputable)
            {

                // Compute adapted phrase and adapted target pitch
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
                    WbpSourceAdaptation prevWbpsa = getPrevWbpsa(wbpsa, tiling);
                    if (prevWbpsa == null)
                    {
                        // Tiling contains no previous WbpSourceAdaptation right now
                        int prevBar = wbpsa.getBarRange().from - 1;
                        if (prevBar >= 0 && tiling.isUsable(prevBar))
                        {
                            // Can not compute pre-target score now, maybe later when a previous WbpSourceAdaptation is present
                            isPrePostTargetScoreComputable = false;
                        } else
                        {
                            // There will never be a previous WbpSourceAdaptation
                            preTargetNoteScore = DEFAULT_TARGET_NOTE_SCORE;
                        }
                    } else
                    {
                        // Compute the pre-target note score
                        int firstNotePitch = wbpsa.getAdaptedPhrase().first().getPitch();
                        preTargetNoteScore = computePreTargetNoteScore(prevWbpsa, wbpsa, firstNotePitch);
//                        if (!wbpSource.isStartingOnChordBass() && preTargetNoteScore == 100f)
//                        {
//                            LOGGER.log(Level.SEVERE, "updateCompatibilityScore() NON START ON CHORD NOTE BUT OK !! wbpsa={0}", wbpsa);
//                        }

                    }
                }


                if (res.postTargetNoteMatch() == 0)
                {
                    WbpSourceAdaptation nextWbpsa = getNextWbpsa(wbpsa, tiling);
                    if (nextWbpsa == null)
                    {
                        // Tiling contains no next WbpSourceAdaptation right now
                        int nextBar = wbpsa.getBarRange().to + 1;
                        if (tiling.isUsable(nextBar))
                        {
                            // Can not compute post-target score now, maybe later when a next WbpSourceAdaptation is present
                            isPrePostTargetScoreComputable = false;
                        } else
                        {
                            // There will never be a previous WbpSourceAdaptation
                            postTargetNoteScore = DEFAULT_TARGET_NOTE_SCORE;
                        }
                    } else
                    {
                        // Compute the post-target note score
                        postTargetNoteScore = computePostTargetNoteScore(wbpsa, nextWbpsa);
//                        if (!wbpSource.isEndingOnChordTone() && postTargetNoteScore == 100f)
//                        {
//                            LOGGER.log(Level.SEVERE, "updateCompatibilityScore() NON ENDING ON CHORD NOTE BUT OK !! wbpsa={0}", wbpsa);
//                        }
                    }
                }
            }

            // If pre/post target scores can be computed then check constraints on non-root start note and non-chord-tone last note: 
            // - if start note is not root, make sure it's the target note of the previous wbpsa
            // - if last note is not a chord tone, make sure next wbpsa matches our target note
            boolean criticalPrePostTargetScoreIssue = isPrePostTargetScoreComputable
                    && ((!wbpSource.isStartingOnChordBass() && preTargetNoteScore < 100f) || (!wbpSource.isEndingOnChordTone() && postTargetNoteScore < 100f));


            if (!criticalPrePostTargetScoreIssue)
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

            } 
        }

        wbpsa.setCompatibilityScore(res);

        return res;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    /**
     * Compute the pre-target note score.
     * <p>
     * Score:<br>
     * 100 if previous WbpSourceAdaptation's targetNote matches wbpsa's first note.<br>
     * 0 if previous WbpSourceAdaptation's last note is the same than wbpsa's first note.<br>
     * DEFAULT_TARGET_NOTE_SCORE for other cases.
     *
     * @param prevWbpsa
     * @param wbpsa
     * @param firstNotePitch
     * @return
     */
    private float computePreTargetNoteScore(WbpSourceAdaptation prevWbpsa, WbpSourceAdaptation wbpsa, int firstNotePitch)
    {
        Objects.requireNonNull(prevWbpsa);
        Objects.requireNonNull(wbpsa);
        Preconditions.checkArgument(firstNotePitch > 0, "firstNotePitch=%s", firstNotePitch);

        float score;

        int prevWbpsaTargetPitch = prevWbpsa.getAdaptedTargetPitch();       // Can be -1 for some WbpSources (eg the last one of a WbpSession)
        var pPrev = prevWbpsa.getAdaptedPhrase();
        if (prevWbpsaTargetPitch == firstNotePitch)
        {
            // Perfect
            score = 100f;
            // LOGGER.severe("computePreTargetNoteScore() PERFECT MATCH");
        } else if (pPrev != null && pPrev.getLast().getPitch() == firstNotePitch)
        {
            // Bad case
            score = 0f;
            // LOGGER.severe("computePreTargetNoteScore() WORST CASE");
        } else
        {
            score = DEFAULT_TARGET_NOTE_SCORE;
        }

        return score;
    }

    /**
     * Compute the post-target note score.
     * <p>
     * Score:<br>
     * 100 if wbpsa's targetNote matches next WbpSourceAdaptation's first note.<br>
     * 0 if last wbpsa's note is the same than WbpSourceAdaptation's first note.<br>
     * DEFAULT_TARGET_NOTE_SCORE for other cases.
     *
     * @param wbpsa
     * @param nextWbpsa Must have its adapted phrase set
     * @return
     */
    private float computePostTargetNoteScore(WbpSourceAdaptation wbpsa, WbpSourceAdaptation nextWbpsa)
    {
        Objects.requireNonNull(wbpsa);
        Objects.requireNonNull(nextWbpsa);
        Preconditions.checkArgument(nextWbpsa.getAdaptedPhrase() != null, "nextWbpsa=%s", nextWbpsa);

        float score;
        int targetPitch = wbpsa.getAdaptedTargetPitch();
        var p = wbpsa.getAdaptedPhrase();
        assert p != null;
        int nextWbpsaFirstPitch = nextWbpsa.getAdaptedPhrase().first().getPitch();

        if (targetPitch == nextWbpsaFirstPitch)
        {
            // Perfect
            score = 100f;
            // LOGGER.severe("computePostTargetNoteScore() PERFECT MATCH");
        } else if (p.getLast().getPitch() == nextWbpsaFirstPitch)
        {
            // Worst case
            score = 0f;
            // LOGGER.severe("computePostTargetNoteScore() WORST CASE");
        } else
        {
            // Default
            score = DEFAULT_TARGET_NOTE_SCORE;
        }
        return score;
    }


    /**
     * Get the WbpSourceAdaptation just before wbpsa.
     *
     * @param wbpsa
     * @param tiling
     * @return Can be null
     */
    private WbpSourceAdaptation getPrevWbpsa(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int prevBar = wbpsa.getBarRange().from - 1;
        WbpSourceAdaptation res = tiling.getUsableBars().contains(prevBar) ? tiling.getWbpSourceAdaptation(prevBar) : null;
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
        WbpSourceAdaptation res = tiling.getUsableBars().contains(nextBar) ? tiling.getWbpSourceAdaptation(nextBar) : null;
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
     * @param tempo
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
            case TWO_FEEL_CUSTOM, WALKING_CUSTOM, WALKING_DOUBLE_NOTE_CUSTOM ->
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
