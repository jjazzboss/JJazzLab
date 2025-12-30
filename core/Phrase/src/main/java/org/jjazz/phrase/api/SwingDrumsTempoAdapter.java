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
package org.jjazz.phrase.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;

/**
 * Apply tempo-dependent microtiming, velocity, and duration adjustments to jazz swing drums phrases.
 * <p>
 * Input phrases are expected to be already swung drums patterns, typically recorded in real-time at ~120 BPM with slight quantization. This adapter scales the
 * existing timing variations based on the target tempo.
 * <p>
 * Drums require different treatment than bass:
 * <ul>
 * <li>Ride cymbal and hi-hat stay more centered on the beat (primary time reference)</li>
 * <li>Snare can have more variable microtiming for groove</li>
 * <li>Bass drum stays tight for pulse anchoring</li>
 * <li>Ghost notes and articulation vary with tempo</li>
 * </ul>
 * <p>
 * Based on empirical research showing that drums microtiming contributes to swing feel and body movement response.
 * <p>
 * References:<br>
 * - Datseris et al. (2019) "Microtiming Deviations and Swing Feel in Jazz"<br>
 * - Kilchenmann & Senn (2015) "Microtiming in Swing and Funk affects body movement behavior"
 */
public class SwingDrumsTempoAdapter
{

    // Tempo thresholds
    private static final float BASELINE_TEMPO = 120f;
    private static final float SLOW_TEMPO_MAX = 120f;
    private static final float FAST_TEMPO_MIN = 160f;
    private static final float VERY_FAST_TEMPO_MIN = 200f;

    private final SwingProfile profile;
    private final Random random;
    private static final Logger LOGGER = Logger.getLogger(SwingDrumsTempoAdapter.class.getSimpleName());

    /**
     * Configuration for drums tempo adaptation.
     */
    public static class AdaptationConfig
    {

        // Ride/Hi-hat (time reference) - stays very centered
        /**
         * Microtiming scale for ride cymbal at slow tempo
         */
        public float rideSlowTempoScale = 0.8f;
        /**
         * Microtiming scale for ride cymbal at fast tempo
         */
        public float rideFastTempoScale = 0.3f;
        /**
         * Additional jitter for ride at slow tempo (ms)
         */
        public float rideSlowJitterSD = 2f;
        /**
         * Additional jitter for ride at fast tempo (ms)
         */
        public float rideFastJitterSD = 0.5f;

        // Snare - more variable for groove
        /**
         * Microtiming scale for snare at slow tempo
         */
        public float snareSlowTempoScale = 1.15f;
        /**
         * Microtiming scale for snare at fast tempo
         */
        public float snareFastTempoScale = 0.4f;
        /**
         * Additional jitter for snare at slow tempo (ms)
         */
        public float snareSlowJitterSD = 6f;
        /**
         * Additional jitter for snare at fast tempo (ms)
         */
        public float snareFastJitterSD = 2f;
        /**
         * Velocity compression factor at fast tempo for snare (0.0-1.0)
         */
        public float snareFastVelocityCompression = 0.7f;

        // Bass drum - stays tight
        /**
         * Microtiming scale for bass drum at slow tempo
         */
        public float bassSlowTempoScale = 0.9f;
        /**
         * Microtiming scale for bass drum at fast tempo
         */
        public float bassFastTempoScale = 0.35f;
        /**
         * Additional jitter for bass at slow tempo (ms)
         */
        public float bassSlowJitterSD = 2f;
        /**
         * Additional jitter for bass at fast tempo (ms)
         */
        public float bassFastJitterSD = 1f;

        // Ghost notes and other articulations
        /**
         * Microtiming scale for other drums at slow tempo
         */
        public float otherSlowTempoScale = 1.2f;
        /**
         * Microtiming scale for other drums at fast tempo
         */
        public float otherFastTempoScale = 0.5f;
        /**
         * Additional jitter for other drums at slow tempo (ms)
         */
        public float otherSlowJitterSD = 8f;
        /**
         * Additional jitter for other drums at fast tempo (ms)
         */
        public float otherFastJitterSD = 3f;
        /**
         * Velocity threshold to detect ghost notes
         */
        public int ghostNoteVelocityThreshold = 60;
        /**
         * Velocity reduction for ghost notes at fast tempo
         */
        public int ghostNoteFastVelocityReduction = 5;

        // Duration
        /**
         * Note duration multiplier at slow tempo
         */
        public float slowTempoDurationScale = 1.0f;
        /**
         * Note duration multiplier at fast tempo
         */
        public float fastTempoDurationScale = 0.88f;

        /**
         * Apply microtiming adjustments
         */
        public boolean applyMicrotiming = true;
        /**
         * Apply velocity adjustments
         */
        public boolean applyVelocityDynamics = true;
        /**
         * Apply duration adjustments
         */
        public boolean applyDurationAdjustment = true;
    }

    /**
     * Create adapter with default neutral profile.
     */
    public SwingDrumsTempoAdapter()
    {
        this(SwingProfile.NEUTRAL, new Random());
    }

    /**
     * Create adapter with specified profile.
     *
     * @param profile Swing feel profile
     * @param random  Random generator for humanization
     */
    public SwingDrumsTempoAdapter(SwingProfile profile, Random random)
    {
        this.profile = profile;
        this.random = random;
    }

    /**
     * Adapt a drums phrase to the specified tempo with default configuration.
     * <p>
     * The input phrase should be a swing drums phrase, typically recorded at ~120 BPM with slight quantization.
     *
     * @param phrase  The drums phrase to adapt (will be modified in place)
     * @param tester  Process only NoteEvents which satisfy the tester
     * @param tempo   Target tempo in BPM
     * @param ts      Time signature for beat detection
     * @param drumKit The drum kit to identify drum types by pitch
     */
    public void adaptToTempo(Phrase phrase, Predicate<NoteEvent> tester, float tempo, TimeSignature ts, DrumKit drumKit)
    {
        adaptToTempo(phrase, tester, tempo, ts, drumKit, new AdaptationConfig());
    }

    /**
     * Adapt a drums phrase to the specified tempo with custom configuration.
     * <p>
     * The input phrase should be a swing drums phrase, typically recorded at ~120 BPM with slight quantization. Different drum types receive different
     * microtiming treatments:
     * <ul>
     * <li>Ride cymbal/hi-hat: minimal variation, stays close to grid (primary time reference)</li>
     * <li>Snare: more variation allowed for groove and feel</li>
     * <li>Bass drum: tight timing for pulse anchoring</li>
     * <li>Other elements: variable timing based on function (ghost notes, cymbals, etc.)</li>
     * </ul>
     *
     * @param phrase  The drums phrase to adapt (will be modified in place)
     * @param tester  Process only NoteEvents which satisfy the tester
     * @param tempo   Target tempo in BPM
     * @param ts      Time signature for beat detection
     * @param drumKit The drum kit to identify drum types by pitch
     * @param config  Configuration parameters
     */
    public void adaptToTempo(Phrase phrase, Predicate<NoteEvent> tester, float tempo, TimeSignature ts, DrumKit drumKit, AdaptationConfig config)
    {
        if (phrase.isEmpty() || !phrase.isDrums() || profile == SwingProfile.DISABLED)
        {
            return;
        }

        // Get drum type mappings from the KeyMap
        DrumKit.KeyMap keyMap = drumKit.getKeyMap();
        List<Integer> ridePitches = keyMap.getKeys(DrumKit.Subset.CYMBAL);  // Includes ride
        List<Integer> hihatPitches = keyMap.getKeys(DrumKit.Subset.HI_HAT);
        List<Integer> snarePitches = keyMap.getKeys(DrumKit.Subset.SNARE);
        List<Integer> bassPitches = keyMap.getKeys(DrumKit.Subset.BASS);

        // Calculate tempo-dependent parameters
        float tempoFactor = calculateTempoFactor(tempo);

        // Process all notes
        Map<NoteEvent, NoteEvent> replacements = new HashMap<>();
        var nes = phrase.stream()
                .filter(ne -> tester.test(ne))
                .toList();

        for (NoteEvent ne : nes)
        {
            float newPos = ne.getPositionInBeats();
            int newVelocity = ne.getVelocity();
            float newDuration = ne.getDurationInBeats();
            int pitch = ne.getPitch();

            // Determine drum type and apply appropriate adjustments
            DrumType drumType = identifyDrumType(pitch, ridePitches, hihatPitches, snarePitches, bassPitches);

            // 1. Apply microtiming scaling based on drum type
            if (config.applyMicrotiming)
            {
                float microtimingScale;
                float jitterSD;

                switch (drumType)
                {
                    case RIDE_HIHAT ->
                    {
                        // Ride/hi-hat stays very close to grid
                        microtimingScale = interpolate(config.rideSlowTempoScale, config.rideFastTempoScale, tempoFactor);
                        jitterSD = interpolate(config.rideSlowJitterSD, config.rideFastJitterSD, tempoFactor);
                    }
                    case SNARE ->
                    {
                        // Snare can shift more for groove
                        microtimingScale = interpolate(config.snareSlowTempoScale, config.snareFastTempoScale, tempoFactor);
                        jitterSD = interpolate(config.snareSlowJitterSD, config.snareFastJitterSD, tempoFactor);
                    }
                    case BASS ->
                    {
                        // Bass drum stays tight
                        microtimingScale = interpolate(config.bassSlowTempoScale, config.bassFastTempoScale, tempoFactor);
                        jitterSD = interpolate(config.bassSlowJitterSD, config.bassFastJitterSD, tempoFactor);
                    }
                    default ->
                    {
                        // Ghost notes and other articulations
                        microtimingScale = interpolate(config.otherSlowTempoScale, config.otherFastTempoScale, tempoFactor);
                        jitterSD = interpolate(config.otherSlowJitterSD, config.otherFastJitterSD, tempoFactor);
                    }
                }

                // Apply profile multiplier to microtiming
                microtimingScale *= profile.getMicrotimingMultiplier();
                jitterSD *= profile.getMicrotimingMultiplier();

                // Scale existing microtiming deviation
                float nearestGrid = findNearestGridPosition(ne.getPositionInBeats());
                float currentDeviation = ne.getPositionInBeats() - nearestGrid;
                float scaledDeviation = currentDeviation * microtimingScale;
                newPos = nearestGrid + scaledDeviation;

                // Add humanization jitter
                float jitter = (float) (random.nextGaussian() * jitterSD);
                jitter = Math.max(-10f, Math.min(10f, jitter));  // Hard limit ±10ms
                newPos += msToBeats(jitter, tempo);
            }

            // 2. Apply velocity adjustments
            if (config.applyVelocityDynamics)
            {
                if (drumType == DrumType.SNARE && tempo >= FAST_TEMPO_MIN)
                {
                    // Compress snare dynamics at fast tempo
                    int mid = 80;
                    float compressionFactor = config.snareFastVelocityCompression * profile.getDynamicCompressionMultiplier();
                    newVelocity = mid + (int) ((ne.getVelocity() - mid) * compressionFactor);
                }

                // Reduce ghost note velocities at fast tempo
                if (ne.getVelocity() < config.ghostNoteVelocityThreshold && tempo >= FAST_TEMPO_MIN)
                {
                    newVelocity = Math.max(30, ne.getVelocity() - config.ghostNoteFastVelocityReduction);
                }

                // Overall dynamic compression at very fast tempo
                if (tempo >= VERY_FAST_TEMPO_MIN)
                {
                    int mid = 75;
                    float compressionFactor = 0.8f * profile.getDynamicCompressionMultiplier();
                    newVelocity = mid + (int) ((newVelocity - mid) * compressionFactor);
                }
            }

            // 3. Apply duration adjustment
            if (config.applyDurationAdjustment)
            {
                float durationScale = interpolate(config.slowTempoDurationScale, config.fastTempoDurationScale, tempoFactor);
                newDuration = ne.getDurationInBeats() * durationScale;
            }

            // Ensure position doesn't go negative
            newPos = Math.max(0, newPos);
            newVelocity = Math.max(1, Math.min(127, newVelocity));

            // Create replacement note if anything changed
            if (Float.compare(newPos, ne.getPositionInBeats()) != 0
                    || newVelocity != ne.getVelocity()
                    || Float.compare(newDuration, ne.getDurationInBeats()) != 0)
            {
                NoteEvent newNe = ne.setAll(
                        ne.getPitch(),
                        newDuration,
                        newVelocity,
                        newPos,
                        ne.getAccidental(),
                        true // copy properties
                );
                replacements.put(ne, newNe);
            }
        }

        // Apply all replacements at once
        if (!replacements.isEmpty())
        {
            phrase.replaceAll(replacements, false);
        }
    }

    // ======================================================================================================
    // Private methods
    // ======================================================================================================

    /**
     * Drum type categories for different microtiming treatment.
     */
    private enum DrumType
    {
        RIDE_HIHAT, // Time reference elements
        SNARE, // Snare drums (all types)
        BASS, // Bass drums
        OTHER        // Everything else (toms, cymbals, percussion, ghost notes)
    }

    /**
     * Identify the drum type category for a given pitch.
     *
     * @param pitch        The MIDI pitch
     * @param ridePitches  List of ride/cymbal pitches from KeyMap
     * @param hihatPitches List of hi-hat pitches from KeyMap
     * @param snarePitches List of snare pitches from KeyMap
     * @param bassPitches  List of bass drum pitches from KeyMap
     * @return The drum type category
     */
    private DrumType identifyDrumType(int pitch, List<Integer> ridePitches, List<Integer> hihatPitches,
            List<Integer> snarePitches, List<Integer> bassPitches)
    {
        if (ridePitches.contains(pitch) || hihatPitches.contains(pitch))
        {
            return DrumType.RIDE_HIHAT;
        } else if (snarePitches.contains(pitch))
        {
            return DrumType.SNARE;
        } else if (bassPitches.contains(pitch))
        {
            return DrumType.BASS;
        } else
        {
            return DrumType.OTHER;
        }
    }

    /**
     * Calculate tempo factor: 0.0 at baseline/slow tempo, 1.0 at very fast tempo.
     *
     * @param tempo Target tempo in BPM
     * @return Value between 0.0 and 1.0
     */
    private float calculateTempoFactor(float tempo)
    {
        if (tempo <= SLOW_TEMPO_MAX)
        {
            return 0f;
        } else if (tempo >= VERY_FAST_TEMPO_MIN)
        {
            return 1f;
        } else
        {
            return (tempo - SLOW_TEMPO_MAX) / (VERY_FAST_TEMPO_MIN - SLOW_TEMPO_MAX);
        }
    }

    /**
     * Linear interpolation between two values based on tempo factor.
     *
     * @param slowValue   Value at slow tempo
     * @param fastValue   Value at fast tempo
     * @param tempoFactor Interpolation factor (0.0 to 1.0)
     * @return Interpolated value
     */
    private float interpolate(float slowValue, float fastValue, float tempoFactor)
    {
        return slowValue + (fastValue - slowValue) * tempoFactor;
    }

    /**
     * Find nearest standard grid position for a given beat position.
     * <p>
     * Drums typically play on quarter notes and swung eighths.
     *
     * @param posInBeats Note position in beats
     * @return Nearest grid position
     */
    private float findNearestGridPosition(float posInBeats)
    {
        int wholeBeat = (int) Math.floor(posInBeats);
        float fracPart = posInBeats - wholeBeat;

        float nearestFrac;
        if (fracPart < 0.165f)
        {
            nearestFrac = 0f;  // On the beat
        } else if (fracPart < 0.5f)
        {
            nearestFrac = 0.333f;  // First triplet eighth (swung eighth)
        } else if (fracPart < 0.835f)
        {
            nearestFrac = 0.667f;  // Second triplet eighth (swung eighth offbeat)
        } else
        {
            nearestFrac = 1.0f;  // Next beat
        }

        return wholeBeat + nearestFrac;
    }

    /**
     * Utility to convert milliseconds to beat fraction at given tempo.
     *
     * @param ms    Time in milliseconds
     * @param tempo Tempo in BPM
     * @return Beat fraction
     */
    private float msToBeats(float ms, float tempo)
    {
        return (ms / 60000f) * tempo;
    }
}
