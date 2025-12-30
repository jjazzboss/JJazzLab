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

/**
 * Swing profile for the Swing TempoAdapter classes.
 */
public enum SwingProfile
{
    /**
     * Relaxed profile: preserves more microtiming variation, less forward lean
     */
    RELAXED(1.15f, 0.85f, 0.85f),
    /**
     * Neutral profile: balanced microtiming scaling and forward lean
     */
    NEUTRAL(1.0f, 1.0f, 1.0f),
    /**
     * Driving profile: more aggressive compression and forward lean at fast tempos
     */
    DRIVING(0.85f, 1.20f, 1.0f),
    /**
     * Special profile which does nothing
     */
    DISABLED(1.0f, 1.0f, 1.0f);
    private final float microtimingMultiplier;
    private final float forwardLeanMultiplier;
    private final float dynamicCompressionMultiplier;

    /**
     * Used only by Drums adapter.
     *
     * @return
     */
    public float getDynamicCompressionMultiplier()
    {
        return dynamicCompressionMultiplier;
    }

    public float getMicrotimingMultiplier()
    {
        return microtimingMultiplier;
    }

    static public SwingProfile toSwingProfile(String name)
    {
        SwingProfile res = NEUTRAL;
        try
        {
            res = SwingProfile.valueOf(name);
        } catch (Throwable t)
        {
            // Nothing
        }
        return res;
    }

    /**
     * Used only by Bass adapter.
     *
     * @return
     */
    public float getForwardLeanMultiplier()
    {
        return forwardLeanMultiplier;
    }

    SwingProfile(float microtiming, float forwardLean, float dynamicCompressionMultiplier)
    {
        this.microtimingMultiplier = microtiming;
        this.forwardLeanMultiplier = forwardLean;
        this.dynamicCompressionMultiplier = dynamicCompressionMultiplier;
    }


    /**
     * Get recommended profile based on tempo and desired feel.
     *
     * @param tempo Target tempo in BPM
     * @return Recommended swing profile
     */
    public static SwingProfile getRecommended(float tempo)
    {
        if (tempo < 100)
        {
            return SwingProfile.RELAXED;
        } else if (tempo > 180)
        {
            return SwingProfile.DRIVING;
        } else
        {
            return SwingProfile.NEUTRAL;
        }
    }
}
