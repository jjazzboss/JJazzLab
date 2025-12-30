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
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;

/**
 * Apply tempo-dependent microtiming, velocity, and duration adjustments to jazz swing bass phrases.
 * <p>
 * Input phrases are expected to be already swung (not straight), typically recorded in real-time at ~120 BPM with slight quantization (notes within ±0.15 beats
 * of standard positions). This adapter scales the existing swing feel based on the target tempo.
 * <p>
 * Based on empirical research showing that swing feel varies with tempo:<br>
 * - Slower tempos preserve or slightly expand microtiming deviations for relaxed feel<br>
 * - Faster tempos compress microtiming deviations and add forward lean for stability and drive
 * <p>
 * References:<br>
 * - Datseris et al. (2019) "Microtiming Deviations and Swing Feel in Jazz"<br>
 * - Honing & de Haas (2008) "Swing once more: Relating timing and tempo in music"
 */
public class SwingBassTempoAdapter
{

    // Tempo thresholds
    private static final float BASELINE_TEMPO = 120f;    // Baseline tempo of recorded phrases
    private static final float SLOW_TEMPO_MAX = 120f;
    private static final float MEDIUM_TEMPO_MAX = 160f;
    private static final float FAST_TEMPO_MIN = 160f;
    private static final float VERY_FAST_TEMPO_MIN = 200f;

    // Configuration parameters
    private final SwingProfile profile;
    private final Random random;
    private static final Logger LOGGER = Logger.getLogger(SwingBassTempoAdapter.class.getSimpleName());


    public static class AdaptationConfig
    {

        // Microtiming scaling factors (applied to existing deviations)
        /**
         * Scale factor for microtiming at slow tempo (1.0 = preserve existing timing)
         */
        public float slowTempoMicrotimingScale = 1.05f;    // Slightly expand at slow tempo
        /**
         * Scale factor for microtiming at fast tempo (reduces existing deviations)
         */
        public float fastTempoMicrotimingScale = 0.35f;    // Compress significantly at fast tempo

        // Forward lean settings (milliseconds, negative = ahead of beat)
        /**
         * Additional forward timing shift at medium tempo (ms)
         */
        public float mediumTempoForwardLean = -2f;
        /**
         * Additional forward timing shift at fast tempo (ms)
         */
        public float fastTempoForwardLean = -6f;

        // Velocity dynamics
        /**
         * Velocity boost for strong beats (1 & 3) at slow tempo
         */
        public int slowTempoAccentDelta = 6;
        /**
         * Velocity boost for strong beats (1 & 3) at fast tempo
         */
        public int fastTempoAccentDelta = 2;

        // Duration/legato settings
        /**
         * Note duration as fraction of written length at slow tempo
         */
        public float slowTempoLegatoPercent = 0.98f;
        /**
         * Note duration as fraction of written length at fast tempo
         */
        public float fastTempoLegatoPercent = 0.90f;

        // Humanization (additional random jitter beyond existing variations)
        /**
         * Additional random jitter standard deviation at slow tempo (ms)
         */
        public float slowTempoJitterSD = 3f;
        /**
         * Additional random jitter standard deviation at fast tempo (ms)
         */
        public float fastTempoJitterSD = 1.5f;

        /**
         * Apply microtiming scaling and forward lean
         */
        public boolean applyMicrotiming = true;
        /**
         * Apply velocity dynamics (accent on strong beats)
         */
        public boolean applyVelocityDynamics = true;
        /**
         * Apply duration adjustment (legato percentage)
         */
        public boolean applyDurationAdjustment = true;
        /**
         * Apply additional humanization jitter
         */
        public boolean applyHumanization = true;
    }

    /**
     * Create adapter with default neutral profile.
     */
    public SwingBassTempoAdapter()
    {
        this(SwingProfile.NEUTRAL, new Random());
    }

    /**
     * Create adapter with specified profile.
     *
     * @param profile Swing feel profile
     * @param random  Random generator for humanization
     */
    public SwingBassTempoAdapter(SwingProfile profile, Random random)
    {
        this.profile = profile;
        this.random = random;
    }

    /**
     * Adapt a bass phrase to the specified tempo with default configuration.
     * <p>
     * The input phrase should be a swing bass phrase, typically recorded at ~120 BPM with slight quantization.
     *
     * @param phrase The phrase to adapt (will be modified in place)
     * @param tester Process only NoteEvents which satisfy the tester
     * @param tempo  Target tempo in BPM
     * @param ts     Time signature for beat detection
     */
    public void adaptToTempo(Phrase phrase, Predicate<NoteEvent> tester, float tempo, TimeSignature ts)
    {
        adaptToTempo(phrase, tester, tempo, ts, new AdaptationConfig());
    }

    /**
     * Adapt a bass phrase to the specified tempo with custom configuration.
     * <p>
     * The input phrase should be a swing bass phrase, typically recorded at ~120 BPM with slight quantization. This method:
     * <ul>
     * <li>Scales existing microtiming deviations based on tempo</li>
     * <li>Adds forward lean at faster tempos</li>
     * <li>Adjusts velocity dynamics</li>
     * <li>Modifies note durations for appropriate legato feel</li>
     * <li>Optionally adds subtle humanization</li>
     * </ul>
     *
     * @param phrase The phrase to adapt (will be modified in place)
     * @param tester Process only NoteEvents which satisfy the tester
     * @param tempo  Target tempo in BPM
     * @param ts     Time signature for beat detection
     * @param config Configuration parameters
     */
    public void adaptToTempo(Phrase phrase, Predicate<NoteEvent> tester, float tempo, TimeSignature ts, AdaptationConfig config)
    {
        if (phrase.isEmpty() || phrase.isDrums() || profile == SwingProfile.DISABLED)
        {
            LOGGER.log(Level.SEVERE, "adaptToTempo() skipped profile={0}", profile);
            return;
        }

        // Calculate tempo-dependent parameters
        float tempoFactor = calculateTempoFactor(tempo);

        // Scale existing microtiming based on tempo
        float microtimingScale = interpolate(config.slowTempoMicrotimingScale, config.fastTempoMicrotimingScale, tempoFactor);
        microtimingScale *= profile.getMicrotimingMultiplier();

        // Forward lean increases with tempo
        float forwardLean = interpolate(0f, config.mediumTempoForwardLean, Math.min(1.0f, tempoFactor * 1.5f));
        if (tempo >= FAST_TEMPO_MIN)
        {
            forwardLean = interpolate(config.mediumTempoForwardLean, config.fastTempoForwardLean,
                    (tempo - FAST_TEMPO_MIN) / (VERY_FAST_TEMPO_MIN - FAST_TEMPO_MIN));
        }
        forwardLean *= profile.getForwardLeanMultiplier();

        int accentDelta = Math.round(interpolate(config.slowTempoAccentDelta, config.fastTempoAccentDelta, tempoFactor));
        float legatoPercent = interpolate(config.slowTempoLegatoPercent, config.fastTempoLegatoPercent, tempoFactor);
        float jitterSD = interpolate(config.slowTempoJitterSD, config.fastTempoJitterSD, tempoFactor);

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

            // 1. Scale existing microtiming deviations
            if (config.applyMicrotiming)
            {
                // Find nearest grid position (assuming quantization to 0.33 or 0.5 subdivisions)
                float nearestGrid = findNearestGridPosition(ne.getPositionInBeats());
                float currentDeviation = ne.getPositionInBeats() - nearestGrid;

                // Scale the deviation based on tempo
                float scaledDeviation = currentDeviation * microtimingScale;
                newPos = nearestGrid + scaledDeviation;

                // Add forward lean at faster tempos
                if (forwardLean != 0)
                {
                    newPos += msToBeats(forwardLean, tempo);
                }
            }

            // 2. Apply humanization jitter
            if (config.applyHumanization && jitterSD > 0)
            {
                float jitter = (float) (random.nextGaussian() * jitterSD);
                // Hard limit jitter to ±8ms (tighter than original since we're adding to existing variation)
                jitter = Math.max(-8f, Math.min(8f, jitter));
                // Less jitter on strong beats (1 and 3)
                if (isStrongBeat(ne.getPositionInBeats(), ts))
                {
                    jitter *= 0.4f;
                }
                newPos += msToBeats(jitter, tempo);
            }

            // 3. Apply velocity dynamics
            if (config.applyVelocityDynamics)
            {
                if (isStrongBeat(ne.getPositionInBeats(), ts))
                {
                    newVelocity = Math.min(127, ne.getVelocity() + accentDelta);
                }
                // Narrow dynamic range at very fast tempos
                if (tempo >= VERY_FAST_TEMPO_MIN)
                {
                    int mid = 85;  // Target midpoint
                    int range = newVelocity - mid;
                    newVelocity = mid + (int) (range * 0.75f);  // Compress by 25%
                }
            }

            // 4. Apply duration adjustment
            if (config.applyDurationAdjustment)
            {
                newDuration = ne.getDurationInBeats() * legatoPercent;

                // Chromatic approach notes can be slightly shorter at fast tempo
                if (tempo >= FAST_TEMPO_MIN && isApproachNote(ne, phrase))
                {
                    newDuration *= 0.93f;
                }
            }

            // Ensure position doesn't go negative
            newPos = Math.max(0, newPos);

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
            // Linear interpolation between slow and very fast
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
     * Assumes swing phrases use standard subdivisions: quarter notes (0, 1, 2...) and swung eighths (approximately 0.33, 0.67 within each beat, may vary due to
     * swing).
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
            nearestFrac = 1.0f;  // Next beat (will be adjusted by wholeBeat)
        }

        return wholeBeat + nearestFrac;
    }

    /**
     * Check if position is on a strong beat (1 or 3 in 4/4, 1 in 3/4).
     *
     * @param posInBeats Note position in beats
     * @param ts         Time signature
     * @return True if on strong beat
     */
    private boolean isStrongBeat(float posInBeats, TimeSignature ts)
    {
        int beatInBar = ((int) Math.floor(posInBeats % ts.getNbNaturalBeats())) + 1;
        if (ts.getNbNaturalBeats() == 4)
        {
            return beatInBar == 1 || beatInBar == 3;
        } else
        {
            return beatInBar == 1;  // For 3/4 and other meters, only beat 1 is strong
        }
    }

    /**
     * Heuristic to detect chromatic approach notes.
     * <p>
     * An approach note is typically 1 or 2 semitones away from the following note.
     *
     * @param ne     Current note
     * @param phrase Containing phrase
     * @return True if likely an approach note
     */
    private boolean isApproachNote(NoteEvent ne, Phrase phrase)
    {
        NoteEvent next = phrase.higher(ne);
        if (next == null)
        {
            return false;
        }
        int interval = Math.abs(next.getPitch() - ne.getPitch());
        // Chromatic approach if next note is 1 or 2 semitones away
        return interval == 1 || interval == 2;
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
