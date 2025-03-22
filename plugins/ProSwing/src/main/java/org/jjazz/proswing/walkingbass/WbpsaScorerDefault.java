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
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.rhythm.api.TempoRange;

/**
 * Evaluates the compatibility of a WbpSourceAdaptation using chord type, transposition, tempo, target notes before/after.
 * <p>
 */
public class WbpsaScorerDefault implements WbpsaScorer
{

    private final PhraseAdapter wbpSourceAdapter;
    private final int tempo;
    private final EnumSet<BassStyle> bassStyles;
    private final Predicate<Score> minCompatibilityTester;
    private static final Logger LOGGER = Logger.getLogger(WbpsaScorerDefault.class.getSimpleName());

    /**
     *
     * @param sourceAdapter          If null target note matching score will not impact the overall score
     * @param tempo                  If &lt;= 0 tempo is ignored in score computing
     * @param minCompatibilityTester If null use Score.DEFAULT_TESTER. Used by
     *                               {@link #computeCompatibilityScore(org.jjazz.proswing.walkingbass.WbpSourceAdaptation, org.jjazz.proswing.walkingbass.WbpTiling)}
     * @param bStyles                Accept only WbpSources which match these bassStyle(s). If empty any BassStyle is accepted
     */
    public WbpsaScorerDefault(PhraseAdapter sourceAdapter, int tempo, Predicate<Score> minCompatibilityTester, BassStyle... bStyles)
    {
        this.wbpSourceAdapter = sourceAdapter;
        this.tempo = tempo;
        this.bassStyles = EnumSet.allOf(BassStyle.class);
        this.minCompatibilityTester = minCompatibilityTester == null ? Score.DEFAULT_TESTER : minCompatibilityTester;
        if (bStyles.length > 0)
        {
            bassStyles.clear();
            this.bassStyles.addAll(Arrays.asList(bStyles));
        }
    }

    public int getTempo()
    {
        return tempo;
    }

    public Set<BassStyle> getBassStyles()
    {
        return Collections.unmodifiableSet(bassStyles);
    }

    /**
     * Compute global compatibility of this WbpSourceAdaptation.
     * <p>
     * Returns Score.ZERO as soon as one chord symbol is incompatible with the phrase.
     *
     * @param wbpsa
     * @param tiling Can be null, in this case pre/post target notes scores are 0
     * @return If the resulting Score does not satisfy the minCompatibilityTester predicate, returned score is Score.ZERO.
     * @see #DefaultWbpsaScorer(org.jjazz.proswing.walkingbass.PhraseAdapter, int, java.util.function.Predicate, org.jjazz.proswing.BassStyle...)
     */
    @Override
    public Score computeCompatibilityScore(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        var wbpSource = wbpsa.getWbpSource();
        var acceptNonRootStartNote = WalkingBassGeneratorSettings.getInstance().isAcceptNonRootStartNote();

        Score res = Score.ZERO;

        if (bassStyles.contains(wbpsa.getWbpSource().getBassStyle()) && (acceptNonRootStartNote || wbpSource.isStartingOnChordRoot()))
        {
            Phrase p = wbpSourceAdapter != null ? wbpSourceAdapter.getPhrase(wbpsa) : null;
            wbpsa.setAdaptedPhrase(p);


            // Pre target note match, 0 or 100
            int prevWbpsaTargetPitch = getPrevWbpsaTargetPitch(wbpsa, tiling);   // Can be -1
            int firstNotePitch = p != null ? p.first().getPitch() : -1;
            boolean preTargetNoteMatch = prevWbpsaTargetPitch != -1 && prevWbpsaTargetPitch == firstNotePitch;
            float preTargetNoteScore = preTargetNoteMatch ? 100f : 0f;


            // Post target note match, 0 or 100
            int wbpsaTargetPitch = getTargetPitch(wbpsa);   // Can be -1
            wbpsa.setAdaptedTargetPitch(wbpsaTargetPitch);
            var nextWbpsa = getNextWbpsa(wbpsa, tiling);        // Can be null -will happen often when starting the tiling...
            int nextWbpsaFirstPitch = getFirstNotePitch(nextWbpsa);  // Can be -1
            boolean postTargetNoteMatch = wbpsaTargetPitch != -1 && wbpsaTargetPitch == nextWbpsaFirstPitch;
            float postTargetNoteScore = postTargetNoteMatch ? 100f : 0f;


            // Check constraints on non-root start note and non-chord-tone last note: 
            // - if start note is not root, make sure it's the target note of the previous wbpsa
            // - if last note is not a chord tone, make sure next wbpsa matches our target note
            boolean startEndCheckOK = (wbpSource.isStartingOnChordRoot() || preTargetNoteMatch)
                    && (wbpSource.isEndingOnChordTone() || postTargetNoteMatch);


            // If wbpsa's last note is not a chord tone, we need a postTargetNoteMatch
            if (startEndCheckOK || tiling == null || wbpSourceAdapter == null)
            {

                // Harmonic compatibility
                var ctScores = getHarmonicCompatibilityScores(wbpsa);
                float ctScore = (float) ctScores.stream().mapToDouble(f -> Double.valueOf(f)).average().orElse(0);      // 0-100


                // Tempo compatibility
                float teScore = getTempoScore(wbpsa);           // 0 - 100


                // Transposability
                var scs = wbpsa.getSimpleChordSequence();
                var scsFirstChordRoot = scs.first().getData().getRootNote();
                int trScore = wbpsa.getWbpSource().getTransposabilityScore(scsFirstChordRoot);     // 0 - 100


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

    @Override
    public ListMultimap<Score, WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpTiling tiling)
    {
        // Score in ascending order
        ListMultimap<Score, WbpSourceAdaptation> mmap = MultimapBuilder.treeKeys().arrayListValues().build();


        int nbBars = scs.getBarRange().size();
        var wbpsDb = WbpSourceDatabase.getInstance();
        var wbpSources = bassStyles.size() == 1 ? wbpsDb.getWbpSources(nbBars, bassStyles.iterator().next())
                : wbpsDb.getWbpSources(nbBars).stream()
                        .filter(s -> bassStyles.contains(s.getBassStyle()))
                        .toList();


        // Check rootProfile first
        var rpScs = scs.getRootProfile();
        var rpWbpSources = wbpSources.stream()
                .filter(s -> rpScs.equals(s.getRootProfile()))
                .toList();
        for (var wbpSource : rpWbpSources)
        {
            var wbpsa = new WbpSourceAdaptation(wbpSource, scs);
            var score = computeCompatibilityScore(wbpsa, tiling);
            if (score.compareTo(Score.ZERO) > 0)
            {
                mmap.put(score, wbpsa);
            }
        }

        return mmap;
    }

    // =====================================================================================================================
    // Private methods
    // =====================================================================================================================

    /**
     * Get the (optional) target pitch of the previous WbpSourceAdaptation.
     *
     * @param wbpsa
     * @param tiling
     * @return -1 if no target note available
     */
    private int getPrevWbpsaTargetPitch(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int res = -1;
        int prevBar = wbpsa.getBarRange().from - 1;
        WbpSourceAdaptation prevWbpsa;
        if (tiling != null && wbpSourceAdapter != null
                && tiling.getBarRange().contains(prevBar)
                && (prevWbpsa = tiling.getWbpSourceAdaptation(prevBar)) != null)
        {
            var tn = wbpSourceAdapter.getTargetNote(prevWbpsa);
            res = tn != null ? tn.getPitch() : -1;
        }
        return res;
    }

    /**
     * Get the target pitch of wbpsa.
     *
     * @param wbpsa
     * @return Can be -1
     */
    private int getTargetPitch(WbpSourceAdaptation wbpsa)
    {
        int res = -1;
        if (wbpSourceAdapter != null)
        {
            var tn = wbpSourceAdapter.getTargetNote(wbpsa);
            res = tn != null ? tn.getPitch() : -1;
        }
        return res;
    }

    /**
     * Get the first note of the source phrase adapted to wbpsa.
     *
     * @param wbpsa
     * @return -1 if can not be computed
     */
    private int getFirstNotePitch(WbpSourceAdaptation wbpsa)
    {
        var res = wbpsa != null && wbpSourceAdapter != null ? wbpSourceAdapter.getPhrase(wbpsa).first().getPitch() : -1;
        return res;
    }

    /**
     * Get the next Wbpsa right after wbpsa.
     *
     * @param wbpsa
     * @param tiling
     * @return Can be null
     */
    private WbpSourceAdaptation getNextWbpsa(WbpSourceAdaptation wbpsa, WbpTiling tiling)
    {
        int nextBar = wbpsa.getBarRange().to + 1;
        WbpSourceAdaptation res = (tiling != null) ? tiling.getWbpSourceAdaptation(nextBar) : null;
        return res;
    }


    /**
     * Compute the harmony compatibility score for each chord-pair.
     * <p>
     *
     * @param wbpsa
     * @return An empty list (if 2 incompatible chords types are found) or a list of float values in the [0;100] range
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
    public float getTempoScore(WbpSourceAdaptation wbpsa)
    {
        float res;
        var wbpSource = wbpsa.getWbpSource();
        var bassStyle = wbpSource.getBassStyle();
        var stats = wbpSource.getStats();

        res = switch (bassStyle)
        {
            case CUSTOM, BASIC ->
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

            default -> throw new IllegalStateException("bassStyle=" + bassStyle);
        };


        res = Math.clamp(res, 0, 100);

        return res;
    }
}
