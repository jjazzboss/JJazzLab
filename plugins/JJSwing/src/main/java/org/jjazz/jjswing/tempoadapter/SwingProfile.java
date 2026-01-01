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

/**
 * Tempo-dependent parameters and global intensity control to be used by SwingDrumsTempoAdapter or SwingBassTempoAdapter.
 * @see SwingBassTempoAdapter
 * @see SwingDrumsTempoAdapter
 */
public class SwingProfile
{

    // Tempo reference points for interpolation
    public static final float SLOW_TEMPO_MIN = 50f;
    public static final float MEDIUM_TEMPO = 120f; // Baseline tempo of recorded phrases
    public static final float FAST_TEMPO = 190f;
    public static final float FAST_TEMPO_MAX = 240f;

    // Global intensity factor (0.0 = no adaptation, 1.0 = full adaptation)
    public float intensity = 1.0f;

    // Swing ratio adjustment (most important!)
    // [slowMin, medium, fast, fastMax]
    public float[] swingRatios =
    {
        2.3f, 2.0f, 1.8f, 1.6f
    };

    // Forward lean in milliseconds (negative = ahead of beat)
    // [slowMin, medium, fast, fastMax]
    public float[] forwardLeans =
    {
        2f, 0f, -5f, -9f
    };

    // Duration/legato percentage
    // [slowMin, medium, fast, fastMax]
    public float[] legatoPercents =
    {
        1f, 1f, 0.97f, 0.94f
    };

    // Velocity dynamics (accent delta)
    // [slowMin, medium, fast, fastMax]
    public int[] accentDeltas =
    {
        6, 4, 2, 1
    };

    // Velocity dynamic range compression (0.0 = no compression, 1.0 = full compression to median)
    // [slowMin, medium, fast, fastMax]
    public float[] velocityCompressionRates =
    {
        0.0f, 0.0f, 0.1f, 0.2f
    };

    // Microtiming scaling (subtle, disabled by default)
    // [slowMin, medium, fast, fastMax]
    public float[] microtimingScales =
    {
        1.0f, 1.0f, 0.7f, 0.5f
    };
    
    // Humanization jitter standard deviation in ms (subtle, disabled by default)
    // [slowMin, medium, fast, fastMax]    
    public float[] jitterSDs =
    {
        1.0f, 2f, 3f, 4f
    };

    // Enable/disable flags
    public boolean applySwingRatio = true;
    public boolean applyForwardLean = true;
    public boolean applyDuration = true;
    public boolean applyVelocityDynamics = true;
    public boolean applyHumanization = true;
    public boolean applyMicrotiming = false;

    private SwingProfile()
    {
    }

    /**
     * Create a SwingProfile to process melodic phrases.
     *
     * @param intensity 1 means the profile parameters will be fully applied, 0 means no change will be performed. Value &gt; 1 is possible but more
     *                  experimental.
     * @return
     */
    public static SwingProfile create(float intensity)
    {
        SwingProfile profile = new SwingProfile();
        profile.intensity = intensity;
        return profile;
    }

    /**
     * Create a SwingProfile to process drums phrases.
     *
     * @param intensity 1 means the profile parameters will be fully applied, 0 means no change will be performed. Value &gt; 1 is possible but more
     *                  experimental.
     *
     * @return
     */
    public static SwingProfile createForDrums(float intensity)
    {
        SwingProfile profile = new SwingProfile();
        profile.intensity = intensity;
        profile.applyDuration = false;
        return profile;
    }

    public float getSwingRatio(int tempo)
    {
        float baseValue = interpolateArray(tempo, swingRatios);
        return 2.0f + (baseValue - 2.0f) * intensity;
    }

    public float getForwardLean(int tempo)
    {
        return interpolateArray(tempo, forwardLeans) * intensity;
    }

    public float getLegatoPercent(int tempo)
    {
        float baseValue = interpolateArray(tempo, legatoPercents);
        return 1.0f + (baseValue - 1.0f) * intensity;
    }

    public float getAccentDelta(int tempo)
    {
        return interpolateArray(tempo, accentDeltas) * intensity;
    }

    public float getVelocityCompressionRate(int tempo)
    {
        return interpolateArray(tempo, velocityCompressionRates) * intensity;
    }

    public float getMicroTimingScale(int tempo)
    {
        float baseValue = interpolateArray(tempo, microtimingScales);
        return 1.0f + (baseValue - 1.0f) * intensity;
    }

    public float getJitterSD(int tempo)
    {
        return interpolateArray(tempo, jitterSDs) * intensity;
    }

    /**
     * Interpolate using an array of values corresponding to the 4 tempo reference points.
     *
     * @param tempo  Target tempo in BPM
     * @param values Array of 4 values: [slow, medium, fast, fastMax]
     * @return Interpolated value
     * @throws IllegalArgumentException if array length is not 4
     */
    private float interpolateArray(float tempo, float[] values)
    {
        if (values.length != 4)
        {
            throw new IllegalArgumentException("Values array must have exactly 4 elements. Got " + values.length);
        }
        return interpolate(tempo, SLOW_TEMPO_MIN, values[0], MEDIUM_TEMPO, values[1], FAST_TEMPO, values[2], FAST_TEMPO_MAX, values[3]);
    }

    /**
     * Interpolate using an array of int values corresponding to the 4 tempo reference points.
     *
     * @param tempo  Target tempo in BPM
     * @param values Array of 4 values: [slow, medium, fast, fastMax]
     * @return Interpolated value
     * @throws IllegalArgumentException if array length is not 4
     */
    private float interpolateArray(float tempo, int[] values)
    {
        if (values.length != 4)
        {
            throw new IllegalArgumentException("Values array must have exactly 4 elements. Got " + values.length);
        }
        return interpolate(tempo, SLOW_TEMPO_MIN, (float) values[0], MEDIUM_TEMPO, (float) values[1], FAST_TEMPO, (float) values[2], FAST_TEMPO_MAX,
                (float) values[3]);
    }

    /**
     * Piecewise linear interpolation for any number of control points.
     * <p>
     * Pass alternating tempo and value pairs. The method will interpolate linearly between consecutive points. Values below the first tempo return the first
     * value, values above the last tempo return the last value.
     * <p>
     * Example: interpolate(140, 100, 1.0, 160, 0.5, 200, 0.2) means:
     * <ul>
     * <li>tempo ≤ 100: return 1.0</li>
     * <li>tempo 100-160: interpolate between 1.0 and 0.5</li>
     * <li>tempo 160-200: interpolate between 0.5 and 0.2</li>
     * <li>tempo ≥ 200: return 0.2</li>
     * </ul>
     *
     * @param tempo  Current tempo in BPM
     * @param points Alternating tempo thresholds and values (t1, v1, t2, v2, t3, v3, ...)
     * @return Interpolated value
     * @throws IllegalArgumentException if less than 2 points provided or odd number of values
     */
    private float interpolate(float tempo, float... points)
    {
        if (points.length < 4 || points.length % 2 != 0)
        {
            throw new IllegalArgumentException("Need at least 2 control points (4 values: t1, v1, t2, v2). Got " + points.length + " values.");
        }
        int numPoints = points.length / 2;
        // Before first point
        float firstTempo = points[0];
        float firstValue = points[1];
        if (tempo <= firstTempo)
        {
            return firstValue;
        }
        // After last point
        float lastTempo = points[points.length - 2];
        float lastValue = points[points.length - 1];
        if (tempo >= lastTempo)
        {
            return lastValue;
        }
        // Find the segment containing this tempo and interpolate
        for (int i = 0; i < numPoints - 1; i++)
        {
            float t1 = points[i * 2];
            float v1 = points[i * 2 + 1];
            float t2 = points[(i + 1) * 2];
            float v2 = points[(i + 1) * 2 + 1];
            if (tempo >= t1 && tempo <= t2)
            {
                // Linear interpolation within this segment
                float factor = (tempo - t1) / (t2 - t1);
                return v1 + (v2 - v1) * factor;
            }
        }
        // Fallback (should never reach here)
        return lastValue;
    }

}
