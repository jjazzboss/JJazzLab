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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.utilities.api.FloatRange;

/**
 * Apply tempo-dependent swing adjustments to jazz swing drum phrases.
 * <p>
 * Input phrases are expected to be already swung (not straight), typically recorded in real-time at ~120 BPM with slight quantization (notes within ±0.15 beats
 * of standard positions). This adapter scales the existing swing feel based on the target tempo.
 * <p>
 * Based on empirical research showing that swing feel varies with tempo.<br>
 */
public class SwingDrumsTempoAdapter
{

    private final TimeSignature timeSignature;
    private final SwingProfile profile;
    private final DrumKit.KeyMap keyMap;
    private static final Logger LOGGER = Logger.getLogger(SwingDrumsTempoAdapter.class.getSimpleName());

    /**
     * Create adapter with specified profile.
     *
     * @param profile Swing feel profile for drums
     * @param ts      Time signature
     * @param keyMap  Drum kit key map for identifying percussion instruments
     */
    public SwingDrumsTempoAdapter(SwingProfile profile, TimeSignature ts, DrumKit.KeyMap keyMap)
    {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(ts);
        Objects.requireNonNull(keyMap);
        this.profile = profile;
        this.timeSignature = ts;
        this.keyMap = keyMap;
    }

    /**
     * Adapt a drum phrase to the specified tempo.
     * <p>
     * The input phrase should be a swing drum phrase, typically recorded at ~120 BPM with slight quantization. This method:
     * <ul>
     * <li>Adjusts swing ratio based on tempo</li>
     * <li>Scales existing microtiming deviations based on tempo</li>
     * <li>Adds forward lean at faster tempos (different amounts per drum type)</li>
     * <li>Adjusts velocity dynamics (accents on downbeats for ride/hihat)</li>
     * <li>Optionally adds subtle humanization</li>
     * </ul>
     * <p>
     * Note: Duration is NOT modified for drum notes.
     *
     * @param phrase    The phrase to adapt (will be modified in place)
     * @param beatRange Adapt only NoteEvents which start in this beat range. Can be null.
     * @param tester    Process only NoteEvents which satisfy the tester. Can be null.
     * @param tempo     Target tempo in BPM
     */
    public void adaptToTempo(Phrase phrase, FloatRange beatRange, Predicate<NoteEvent> tester, int tempo)
    {
        Objects.requireNonNull(phrase);

        if (phrase.isEmpty() || !phrase.isDrums() || profile.intensity == 0)
        {
            LOGGER.log(Level.WARNING, "adaptToTempo() skipped, intensity={0}", profile.intensity);
            return;
        }

        // Process all notes
        Map<NoteEvent, NoteEvent> replacements = new HashMap<>();
        var validNotes = phrase.stream()
                .filter(ne -> beatRange == null || beatRange.contains(ne.getPositionInBeats(), true))
                .filter(ne -> tester == null || tester.test(ne))
                .toList();

        // Calculate median velocity once for the entire phrase
        int medianVelocity = Phrases.computeMedianVelocity(validNotes, 64);

        for (NoteEvent ne : validNotes)
        {
            float newPos = ne.getPositionInBeats();
            int newVelocity = ne.getVelocity();

            // Apply transformations
            newPos = SwingTransformations.applySwingRatio(profile, newPos, tempo);
            newPos = applyForwardLean(ne.getPitch(), newPos, tempo);
            newPos = SwingTransformations.applyMicroTimings(profile, newPos, tempo);
            newPos = SwingTransformations.applyHumanizationJitter(profile, timeSignature, newPos, tempo);
            newVelocity = applyVelocityDynamics(ne.getPitch(), newVelocity, tempo, timeSignature.isDownBeat(newPos % timeSignature.getNbNaturalBeats()),medianVelocity);

            // Check that we remain in the allowed range
            newPos = Math.max(newPos, beatRange != null ? beatRange.from : 0);
            if (beatRange != null)
            {
                newPos = Math.min(newPos, beatRange.to - 0.1f);
            }

            // Create replacement note if anything changed
            if (Float.compare(newPos, ne.getPositionInBeats()) != 0
                    || newVelocity != ne.getVelocity())
            {
                NoteEvent newNe = ne.setAll(
                        ne.getPitch(),
                        ne.getDurationInBeats(), // Keep original duration for drums
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
     * Forward lean increases with tempo to create a driving feel at faster tempos. Different drum instruments apply different amounts of forward lean for
     * realistic timing.
     *
     * @param pitch      MIDI pitch (to identify drum type)
     * @param posInBeats Current position in beats
     * @param tempo      Target tempo in BPM
     * @return Adjusted position
     */
    private float applyForwardLean(int pitch, float posInBeats, int tempo)
    {
        float res = posInBeats;
        if (!profile.applyForwardLean)
        {
            return res;
        }

        float forwardLean = profile.getForwardLean(tempo);
        float multiplier = getForwardLeanMultiplier(pitch);

        res = Math.max(0, posInBeats + MidiUtilities.msToBeats(forwardLean * multiplier, tempo));

        return res;
    }


    /**
     * Apply velocity dynamics adjustments for drums.
     * <p>
     * Increases velocity on downbeats for ride/hihat accent, and compresses dynamic range at faster tempos for clarity.
     *
     * @param pitch          MIDI pitch (used to identify drum instrument via keyMap)
     * @param velocity       Original velocity
     * @param tempo          Target tempo in BPM
     * @param isDownBeat     True if this note is on a downbeat
     * @param medianVelocity Median velocity of the phrase (used as reference for compression)
     * @return Adjusted velocity (1-127)
     */
    private int applyVelocityDynamics(int pitch, int velocity, int tempo, boolean isDownBeat, int medianVelocity)
    {
        int res = velocity;

        if (!profile.applyVelocityDynamics)
        {
            return res;
        }

        // Accent downbeats for ride cymbal and hihat only
        if (isDownBeat && isRideOrHiHat(pitch))
        {
            int accentDelta = Math.round(profile.getAccentDelta(tempo));
            res = velocity + accentDelta;
        }

        // Compress dynamic range at faster tempos for clarity
        // Use phrase median as reference point
        float compressionRate = profile.getVelocityCompressionRate(tempo);
        if (compressionRate > 0.0f)
        {
            int range = res - medianVelocity;
            res = medianVelocity + (int) (range * (1.0f - compressionRate));
        }

        return MidiConst.clamp(res);
    }


    // ======================================================================================================
    // Private methods - Utilities
    // ======================================================================================================
    /**
     * Get forward lean multiplier for specific drum instrument.
     * <p>
     * In jazz swing, different drums have different timing characteristics: <br>
     * - Ride/HiHat: Minimal forward lean (anchor the swing groove with bass) <br>
     * - Kick: Matches bass forward lean (locks with walking bass) <br>
     * - Snare: * Slightly more forward lean (interactive, responsive accents) <br>
     * - Crash: Extra forward lean (anticipation on accents) <br>
     * - Toms: Moderate forward lean (fills and accents)<br>
     *
     * @param pitch MIDI pitch of the drum
     * @return Multiplier (0.0 - 1.5+)
     */
    private float getForwardLeanMultiplier(int pitch)
    {
        // Use DrumKit.Subset to properly identify drum types
        if (keyMap.getKeys(DrumKit.Subset.CYMBAL).contains(pitch))
        {
            return 0.5f;  // Slightly behind bass - anchors the swing groove
        } else if (keyMap.getKeys(DrumKit.Subset.HI_HAT).contains(pitch))
        {
            return 0.5f;  // Slightly behind bass - anchors the swing groove
        } else if (keyMap.getKeys(DrumKit.Subset.BASS).contains(pitch))
        {
            return 1.0f;  // Matches bass - locks with walking bass
        } else if (keyMap.getKeys(DrumKit.Subset.SNARE).contains(pitch))
        {
            return 1.2f;  // Slightly ahead - interactive/responsive
        } else if (keyMap.getKeys(DrumKit.Subset.CRASH).contains(pitch))
        {
            return 1.5f;  // Most forward - anticipation on accents
        } else if (keyMap.getKeys(DrumKit.Subset.TOM).contains(pitch))
        {
            return 1.0f;  // Moderate - with kick
        }

        return 1f;  // Default for other percussion
    }

    /**
     * Check if the given pitch corresponds to ride cymbal or hihat.
     *
     * @param pitch MIDI pitch
     * @return true if ride or hihat
     */
    private boolean isRideOrHiHat(int pitch)
    {
        return keyMap.getKeys(DrumKit.Subset.CYMBAL).contains(pitch) || keyMap.getKeys(DrumKit.Subset.HI_HAT).contains(pitch);
    }  

}
