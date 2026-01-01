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
package org.jjazz.jjswing.tempoadapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.jjswing.bass.db.Velocities;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.utilities.api.FloatRange;

/**
 * Apply tempo-dependent swing adjustments to jazz swing bass phrases.
 * <p>
 * Input phrases are expected to be already swung (not straight), typically recorded in real-time at ~120 BPM with slight quantization (notes within ±0.15 beats
 * of standard positions). This adapter scales the existing swing feel based on the target tempo.
 * <p>
 * Based on empirical research showing that swing feel varies with tempo.<br>
 */
public class SwingBassTempoAdapter
{

    private final TimeSignature timeSignature;
    private final SwingProfile profile;
    private static final Logger LOGGER = Logger.getLogger(SwingBassTempoAdapter.class.getSimpleName());


    /**
     * Create adapter with specified profile.
     *
     * @param profile Swing feel profile
     * @param ts
     */
    public SwingBassTempoAdapter(SwingProfile profile, TimeSignature ts)
    {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(ts);
        this.profile = profile;
        this.timeSignature = ts;
    }

    /**
     * Adapt a bass phrase to the specified tempo.
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
     * @param phrase    The phrase to adapt (will be modified in place)
     * @param beatRange Adapt only NoteEvents contained in this beat range. Can be null.
     * @param tester    Process only NoteEvents which satisfy the tester. Can be null.
     * @param tempo     Target tempo in BPM
     */
    public void adaptToTempo(Phrase phrase, FloatRange beatRange, Predicate<NoteEvent> tester, int tempo)
    {
        Objects.requireNonNull(phrase);

        if (phrase.isEmpty() || phrase.isDrums() || profile.intensity == 0)
        {
            LOGGER.log(Level.SEVERE, "adaptToTempo() skipped, intensity={0}", profile.intensity);
            return;
        }


        // Process all notes
        Map<NoteEvent, NoteEvent> replacements = new HashMap<>();
        var validNotes = phrase.stream()
                .filter(ne -> beatRange == null || beatRange.contains(ne.getBeatRange(), true))
                .filter(ne -> tester == null || tester.test(ne))
                .toList();

        // Calculate median velocity once for the entire phrase
        int medianVelocity = Phrases.computeMedianVelocity(validNotes, 64);

        for (NoteEvent ne : validNotes)
        {
            float newPos = ne.getPositionInBeats();
            int newVelocity = ne.getVelocity();
            float newDuration = ne.getDurationInBeats();


            // Apply transformations
            newPos = SwingTransformations.applySwingRatio(profile, newPos, tempo);
            newPos = applyForwardLean(newPos, tempo);
            newPos = SwingTransformations.applyMicroTimings(profile, newPos, tempo);
            newPos = SwingTransformations.applyHumanizationJitter(profile, timeSignature, newPos, tempo);
            newVelocity = applyVelocityDynamics(newVelocity, tempo, timeSignature.isDownBeat(newPos % timeSignature.getNbNaturalBeats()), medianVelocity);
            newDuration = applyDurationAdjustment(newDuration, tempo);


            // Check that we remain in the allowed range
            newPos = Math.max(newPos, beatRange != null ? beatRange.from : 0);
            if (beatRange != null)
            {
                newPos = Math.min(newPos, beatRange.to - 0.1f);
                if (newPos + newDuration > beatRange.to)
                {
                    newDuration = beatRange.to - 0.05f - newPos;
                }
            }


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
        phrase.replaceAll(replacements, false);
    }

    // ======================================================================================================
    // Private methods - Transformations
    // ======================================================================================================

    /**
     * Apply forward lean adjustment.
     * <p>
     * Forward lean increases with tempo to create a driving feel at faster tempos.
     *
     * @param posInBeats Current position in beats
     * @param tempo
     * @return Adjusted position
     */
    private float applyForwardLean(float posInBeats, int tempo)
    {
        float res = posInBeats;
        if (!profile.applyForwardLean)
        {
            return res;
        }

        float forwardLean = profile.getForwardLean(tempo);
        res = Math.max(0, posInBeats + MidiUtilities.msToBeats(forwardLean, tempo));

        return res;
    }


    /**
     * Apply velocity dynamics adjustments.
     * <p>
     * Increases velocity on downbeats for accent, and compresses dynamic range at very fast tempos.
     *
     * @param velocity       Original velocity
     * @param tempo          Target tempo in BPM
     * @param isDownBeat     True if this note is on a downbeat
     * @param medianVelocity
     * @return Adjusted velocity (1-127)
     */
    private int applyVelocityDynamics(int velocity, int tempo, boolean isDownBeat, int medianVelocity)
    {
        int res = velocity;

        if (!profile.applyVelocityDynamics)
        {
            return res;
        }

        // Accent downbeats
        if (isDownBeat)
        {
            int accentDelta = Math.round(profile.getAccentDelta(tempo));
            res = velocity + accentDelta;
        }

        // Narrow dynamic range at fast tempos for clarity
        float compressionRate = profile.getVelocityCompressionRate(tempo);
        if (compressionRate > 0.0f)
        {
            int range = res - medianVelocity;
            res = medianVelocity + (int) (range * (1.0f - compressionRate));  // Compress toward median
        }

        return MidiConst.clamp(res);
    }

    /**
     * Apply duration adjustments.
     * <p>
     * Adjusts note durations based on tempo for appropriate legato feel.
     *
     * @param duration Original duration in beats
     * @param tempo    Target tempo in BPM
     * @return Adjusted duration
     */
    private float applyDurationAdjustment(float duration, int tempo)
    {
        float res = duration;
        if (!profile.applyDuration)
        {
            return res;
        }

        float legatoPercent = profile.getLegatoPercent(tempo);
        res = duration * legatoPercent;

        return Math.max(0.01f, res);  // Ensure minimum duration
    }


}
