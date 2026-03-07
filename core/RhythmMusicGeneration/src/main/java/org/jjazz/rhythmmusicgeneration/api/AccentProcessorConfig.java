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
package org.jjazz.rhythmmusicgeneration.api;

/**
 * Configuration holder for AccentProcessor tunable parameters.
 * <p>
 */
public class AccentProcessorConfig
{

    // ---------------------------------------------------------------------
    // Tempo multipliers and duration tuning
    // ---------------------------------------------------------------------

    /**
     * Multipliers applied to the post-silence nbCells calculation for HOLD/SHOT on drums, when tempo is in TempoRange.SLOW.
     */
    public float holdShotPostSilenceMultiplierSlow = 1.0f;

    /**
     * Same as above for TempoRange.MEDIUM_SLOW.
     */
    public float holdShotPostSilenceMultiplierMediumSlow = 1.2f;

    /**
     * Same as above for TempoRange.MEDIUM.
     */
    public float holdShotPostSilenceMultiplierMedium = 1.4f;

    /**
     * Same as above for TempoRange.MEDIUM_FAST.
     */
    public float holdShotPostSilenceMultiplierMediumFast = 1.6f;

    /**
     * Same as above for TempoRange.FAST.
     */
    public float holdShotPostSilenceMultiplierFast = 2.0f;

    /**
     * Multipliers applied to the base shot note duration depending on tempo. Values correspond to SLOW, MEDIUM_SLOW, MEDIUM, MEDIUM_FAST, FAST respectively.
     */
    public float shotDurationMultiplierSlow = 1.8f;
    public float shotDurationMultiplierMediumSlow = 1.9f;
    public float shotDurationMultiplierMedium = 2.2f;
    public float shotDurationMultiplierMediumFast = 2.5f;
    public float shotDurationMultiplierFast = 2.8f;

    /**
     * Ratio used to compute the minimum accent duration from the shot duration: accentMinDuration = shotDuration * accentMinToShotRatio.
     */
    public float accentMinToShotRatio = 1.5f;

    // ---------------------------------------------------------------------
    // Note window / search size
    // ---------------------------------------------------------------------

    /**
     * Number of surrounding notes to use when computing velocities / pitches for new/inserted notes (was NOTE_WINDOW_SIZE = 10).
     */
    public int noteWindowSize = 10;

    /**
     * Number of surrounding notes used when estimating crash cymbal velocity (was NOTE_WINDOW_SIZE = 5 inside addCrashCymbal).
     */
    public int crashVelocityWindowSize = 5;

    // ---------------------------------------------------------------------
    // Velocity constants
    // ---------------------------------------------------------------------

    /**
     * Default baseline velocity used when not enough context for new regular notes (was 60).
     */
    public int defaultNoteVelocity = 60;

    /**
     * Default baseline velocity used for new drum accent notes when not enough context (was 55).
     */
    public int defaultDrumAccentVelocity = 55;

    /**
     * Default baseline velocity used when inserting crash cymbals (was 60).
     */
    public int defaultCrashVelocity = 60;

    /**
     * Offset added to existing drum accent velocity for a medium accent (was 12).
     */
    public int drumsAccentMediumOffset = 12;

    /**
     * Offset added to existing drum accent velocity for a strong accent (was 25).
     */
    public int drumsAccentStrongOffset = 25;

    /**
     * Extra velocity offset for accents on downbeats / half-bar beats (was 5).
     */
    public int drumsAccentExtraDownBeat = 5;

    /**
     * Offset added to computed reference velocity for medium-level accent insertions (was 14).
     */
    public int accentVelocityOffsetMedium = 14;

    /**
     * Offset added to computed reference velocity for stronger accent insertions (was 30).
     */
    public int accentVelocityOffsetStrong = 30;

    /**
     * Minimum allowed velocity (clamp) (was 10).
     */
    public int minVelocity = 10;

    /**
     * Maximum allowed velocity (clamp) (was 120).
     */
    public int maxVelocity = 120;

    // ---------------------------------------------------------------------
    // Drums / crash probabilities & sliding window
    // ---------------------------------------------------------------------

    /**
     * Minimum probability threshold for crash insertion (was 0.2).
     */
    public double crashThresholdMin = 0.2;

    /**
     * Normal/default probability threshold for crash insertion (was 0.6).
     */
    public double crashThresholdNormal = 0.6;

    /**
     * Maximum probability threshold for crash insertion (was 0.8).
     */
    public double crashThresholdMax = 0.8;

    /**
     * Expected number of crashes in the sliding window. Used to adapt probability when there are more/less crashes than normal (was 1).
     */
    public int slidingWindowCrashCountNormal = 1;

    /**
     * Sliding window size in beats used to compute how many previous crashes to consider (was 4 beats). The actual sliding window in cells =
     * slidingWindowBeatSize * nbCellsPerBeat.
     */
    public int slidingWindowBeatSize = 4;

    /**
     * Velocity threshold to consider an existing drum hit as an accent when building the initial grid of accents (was 40).
     */
    public int drumsAccentThresholdVelocity = 40;

    // ---------------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------------


}
