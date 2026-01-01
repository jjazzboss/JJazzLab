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

import java.util.Random;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.quantizer.api.Quantization;

import org.jjazz.quantizer.api.Quantizer;

/**
 * Utility class containing common swing tempo transformation algorithms.
 * <p>
 * These transformations are shared between bass and drum tempo adapters. All methods are stateless and thread-safe (except for the shared Random instance used
 * for humanization).
 */
public class SwingTransformations
{

    private static final Random random = new Random();

    /**
     * Adjust swing ratio based on tempo.
     * <p>
     * Research shows swing ratio decreases with tempo:<br>
     * - Slow tempos have more pronounced swing (2.5:1 or higher)<br>
     * - Fast tempos approach straighter eighths (1.8:1 or lower)
     * <p>
     * This method adjusts notes that fall on the 2nd swing eighth positions (2/3 of each beat) to match the tempo-appropriate swing ratio.
     *
     * @param profile    Swing profile containing configuration
     * @param posInBeats Original position in beats
     * @param tempo      Target tempo in BPM
     * @return Adjusted position with tempo-appropriate swing ratio
     */
    public static float applySwingRatio(SwingProfile profile, float posInBeats, int tempo)
    {
        if (!profile.applySwingRatio)
        {
            return posInBeats;
        }

        // Get target swing ratio for this tempo
        float baseLineRatio = profile.swingRatios[1];  // Medium tempo baseline of the input phrase (2.0 at 120 BPM)
        float targetRatio = profile.getSwingRatio(tempo);

        // No adjustment needed if ratios match
        if (Math.abs(targetRatio - baseLineRatio) < 0.05f)
        {
            return posInBeats;
        }

        // Find position within the beat
        float beatNumber = (float) Math.floor(posInBeats);
        float posInBeat = posInBeats - beatNumber;

        // Check if on a beat (0.0, 1.0, etc.) - don't adjust these
        float distanceToBeat = Math.abs(posInBeat - Math.round(posInBeat));
        if (distanceToBeat < 0.15f)
        {
            return posInBeats;  // On the beat, leave it alone
        }

        // Find which swing position we're closest to
        float nearestSwing = Quantizer.getQuantized(Quantization.ONE_THIRD_BEAT, posInBeat);
        float deviation = posInBeat - nearestSwing;

        // Only adjust if clearly on a swing position (within 0.15 beats)
        if (Math.abs(deviation) > 0.15f)
        {
            return posInBeats;  // Not a swing eighth, leave it alone
        }

        // Calculate new swing position based on target ratio
        // Swing ratio = (long note duration) / (short note duration)
        // For 2:1 ratio -> positions at 0, 0.666, 1.0 (triplets)
        // For 1:1 ratio -> positions at 0, 0.5, 1.0 (straight eighths)

        // Convert ratio to position offset
        float targetOffset = targetRatio / (targetRatio + 1f);

        // Determine which eighth note this is (1st or 2nd in the beat)
        boolean isFirstEighth = nearestSwing < 0.5f;

        float newPosInBeat;
        if (isFirstEighth)
        {
            newPosInBeat = targetOffset / 2;
        } else
        {
            // Second swing eighth - it's at targetOffset
            newPosInBeat = targetOffset;
        }

        float res = Math.max(0, beatNumber + newPosInBeat + deviation);
        return res;
    }

    /**
     * Apply microtiming scaling to position.
     * <p>
     * Scales existing deviations from the nearest grid position based on tempo.
     *
     * @param profile    Swing profile containing configuration
     * @param posInBeats Original position in beats
     * @param tempo      Target tempo in BPM
     * @return Adjusted position
     */
    public static float applyMicroTimings(SwingProfile profile, float posInBeats, int tempo)
    {
        if (!profile.applyMicrotiming)
        {
            return posInBeats;
        }

        float microtimingScale = profile.getMicroTimingScale(tempo);

        // Find nearest swing grid position
        float nearestGrid = Quantizer.getQuantized(Quantization.ONE_THIRD_BEAT, posInBeats);
        float currentDeviation = posInBeats - nearestGrid;

        // Scale the deviation based on tempo
        float scaledDeviation = currentDeviation * microtimingScale;
        float res = Math.max(0, nearestGrid + scaledDeviation);

        return res;
    }

    /**
     * Apply humanization jitter.
     * <p>
     * Adds subtle random timing variation using Gaussian distribution to simulate natural human imperfection. Uses less jitter on downbeats to maintain pulse
     * stability.
     *
     * @param profile       Swing profile containing configuration
     * @param timeSignature Time signature to identify downbeats
     * @param posInBeats    Current position in beats
     * @param tempo         Target tempo in BPM
     * @return Adjusted position
     */
    public static float applyHumanizationJitter(SwingProfile profile, TimeSignature timeSignature, float posInBeats, int tempo)
    {
        if (!profile.applyHumanization)
        {
            return posInBeats;
        }

        float jitterSD = profile.getJitterSD(tempo);
        float jitter = (float) (random.nextGaussian() * jitterSD);
        jitter = Math.clamp(jitter, -7f, 7f);

        // Less jitter on downbeats to maintain pulse
        if (timeSignature.isDownBeat(posInBeats%timeSignature.getNbNaturalBeats()))
        {
            jitter *= 0.5f;
        }

        float res = Math.max(0, posInBeats + MidiUtilities.msToBeats(jitter, tempo));
        return res;
    }

}
