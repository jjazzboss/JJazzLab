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

import org.jjazz.jjswing.bass.db.WbpSource;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.jjswing.bass.db.RootProfile;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Associates a WbpSource to a SimpleChordSequence, with a compatibility score.
 * <p>
 * Instances are cached.
 * <p>
 * NOTE: Comparable implementation is implemented so that natural order is by descending compatibility score. Comparable implementation is NOT consistent with
 * equal(), so WbpSourceAdaptation should NOT be used in a SortedSet or SortedMap.
 */
public class WbpSourceAdaptation implements Comparable<WbpSourceAdaptation>
{

    private Score compatibilityScore;
    private final WbpSource wbpSource;
    private final SimpleChordSequence simpleChordSequence;
    private Phrase adaptedPhrase;
    private int targetPitch;
    private static final Map<String, WbpSourceAdaptation> MAP_KEYSTR_WBPSA = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(WbpSourceAdaptation.class.getSimpleName());

    /**
     * Get an instance (possibly cached) from the specified parameters.
     * <p>
     * An instance created for the first time will have its compatibility score set to 0. A cached instance may have a compatibility score &gt; 0 (harmonic and
     * transposibility values) if already evaluated by a WbpsaScorer (though the tempo/pre/post-target values will be 0 since they depend on the evaluation
     * context).
     *
     * @param wbpSource
     * @param scs
     * @return
     */
    static public WbpSourceAdaptation of(WbpSource wbpSource, SimpleChordSequence scs)
    {
        Objects.requireNonNull(wbpSource);
        Objects.requireNonNull(scs);
        WbpSourceAdaptation res;

        var key = computeKey(wbpSource, scs);
        var wbpsa = MAP_KEYSTR_WBPSA.get(key);
        if (wbpsa == null)
        {
            // Create the instance
            res = new WbpSourceAdaptation(wbpSource, scs);
            MAP_KEYSTR_WBPSA.put(key, res);
            LOGGER.log(Level.FINE, "of() Creating instance for {0}  key={1}  resBr={2}", new Object[]
            {
                wbpSource, key, res.getBeatRange()
            });
        } else
        {
            // Reuse wbpsa harmonic/transposability scores
            res = new WbpSourceAdaptation(wbpSource, scs);
            Score baseScore = wbpsa.getCompatibilityScore();
            Score newScore = new Score(baseScore.harmonicCompatibility(), baseScore.transposability(), 0, 0, 0);    // tempo/pre/post-target scores reset because depend on context
            res.compatibilityScore = newScore;
            if (wbpsa.getAdaptedPhrase() != null)
            {
                res.adaptedPhrase = wbpsa.getAdaptedPhrase().clone();
                float shift = res.getBeatRange().from - wbpsa.getBeatRange().from;
                res.adaptedPhrase.shiftAllEvents(shift, true);
            }
            res.targetPitch = wbpsa.targetPitch;
//            LOGGER.log(Level.SEVERE, "of() reusing values for {0}  key={1}  wbpsaBr={2}  resBr={3}", new Object[]
//            {
//                wbpSource, key, wbpsa.getBeatRange(), res.getBeatRange()
//            });
        }

        return res;
    }

    /**
     * Find the WbpSources from WbpSourceDatabase which are compatible with scs (RootProfiles match and compatibility score is &gt; Score.ZERO) and return the
     * corresponding WbpSourceAdaptations ordered by descending Score.
     * <p>
     * @param scs
     * @param scorer
     * @param tiling     Can be null. Used in the compatibility score calculation
     * @param tempo      If &lt; 0 tempo is ignored in score calculation
     * @param bassStyles Can not be empty. The styles of the target WbpSources to search in the WbpDatabase.
     * @return A new list ordered by descending Score
     * @see WbpSourceDatabase
     */
    static public List<WbpSourceAdaptation> getWbpSourceAdaptations(SimpleChordSequence scs, WbpsaScorer scorer, WbpTiling tiling, int tempo,
            List<BassStyle> bassStyles)
    {
        Objects.requireNonNull(scs);
        Objects.requireNonNull(scorer);
        Objects.requireNonNull(bassStyles);
        Preconditions.checkArgument(!bassStyles.isEmpty(), "bassStyles=%s", bassStyles);

        List<WbpSourceAdaptation> res = new ArrayList<>();

        // Get the WbpSources
        var rootProfile = RootProfile.of(scs);
        var wbpsDb = WbpSourceDatabase.getInstance();
        var wbpSources = bassStyles.size() == 1 ? wbpsDb.getWbpSources(bassStyles.iterator().next(), rootProfile)
                : bassStyles.stream()
                        .flatMap(bs -> wbpsDb.getWbpSources(bs, rootProfile).stream())
                        .toList();

        // Calculate compatibility scores
        for (var wbpSource : wbpSources)
        {
            var wbpsa = WbpSourceAdaptation.of(wbpSource, scs);
            if (scorer.updateCompatibilityScore(wbpsa, tiling, tempo).compareTo(Score.ZERO) > 0)
            {
                res.add(wbpsa);
            }
        }

        Collections.sort(res, (o1, o2) -> o2.compareTo(o1));  // Descending score       
        return res;
    }

    static public void clearCacheData()
    {
        MAP_KEYSTR_WBPSA.clear();
    }

    private WbpSourceAdaptation(WbpSource wbpSource, SimpleChordSequence scs)
    {
        Objects.requireNonNull(wbpSource);
        Objects.requireNonNull(scs);

        this.wbpSource = wbpSource;
        this.simpleChordSequence = scs;
        this.compatibilityScore = Score.ZERO;
        this.adaptedPhrase = null;
        this.targetPitch = -1;
    }

    public WbpSource getWbpSource()
    {
        return wbpSource;
    }

    public SimpleChordSequence getSimpleChordSequence()
    {
        return simpleChordSequence;
    }

    /**
     *
     * @return Can not be null
     */
    public Score getCompatibilityScore()
    {
        return compatibilityScore;
    }

    public void setCompatibilityScore(Score compatibilityScore)
    {
        Objects.requireNonNull(compatibilityScore);
        this.compatibilityScore = compatibilityScore;
    }

    /**
     * Set the adapted phrase for this WbpSourceAdaptation.
     *
     * @param p Can be null
     */
    public void setAdaptedPhrase(Phrase p)
    {
        adaptedPhrase = p;
    }

    /**
     * Get the adapted phrase as set by {@link #setAdaptedPhrase(org.jjazz.phrase.api.Phrase) }.
     * <p>
     * First note position might be slightly before this object bar range if WbpSource.getFirstNoteBeatShift() is &lt; 0.
     *
     * @return Can be null.
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
        Preconditions.checkArgument(targetPitch == -1 || MidiConst.check(targetPitch), "targetPitch=%s", targetPitch);
        this.targetPitch = targetPitch;
    }

    /**
     * Rely on Score.compareTo() only.
     *
     * @param other
     * @return
     */
    @Override
    public int compareTo(WbpSourceAdaptation other)
    {
        Objects.requireNonNull(other);
        return compatibilityScore.compareTo(other.compatibilityScore);
    }

    /**
     * Same as scs.getBarRange().
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return simpleChordSequence.getBarRange();
    }

    /**
     * Same as scs.getNotesBeatRange().
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        return simpleChordSequence.getBeatRange();
    }

    @Override
    public String toString()
    {
        return "wbpsa{" + compatibilityScore + ", " + getBarRange() + ", " + getBeatRange() + ", " + wbpSource.getId() + ", " + wbpSource.getSimpleChordSequence() + "}";
    }

    public String toString2()
    {
        return "wbpsa{" + getBarRange() + ", " + wbpSource + "}";
    }

    // ===================================================================================================================
    // Private methods
    // ===================================================================================================================
    /**
     * Compute a key from WbpSource and scs chord symbols, ignoring start beat position.
     *
     * @param wbpSource
     * @param scs
     * @return
     */
    private static String computeKey(WbpSource wbpSource, SimpleChordSequence scs)
    {
        var strChords = scs.stream()
                .map(cliCs -> cliCs.getData().toString())
                .collect(Collectors.joining(":"));
        var rp = RootProfile.of(scs);
        var strRp = rp.nbBars() + ":" + rp.ascendingIntervals() + ":" + rp.relativeChordPositionsInBeats();
        var key = wbpSource.getId() + " " + strRp + " " + strChords;
        return key;
    }
}
