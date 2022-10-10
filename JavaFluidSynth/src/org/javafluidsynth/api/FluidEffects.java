/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2022 Jerome Lelasseux. All rights reserved.
 *
 *  You can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  Software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  Contributor(s): 
 */
package org.javafluidsynth.api;

import java.util.Arrays;
import java.util.List;

/**
 * Model for FluidSynth reverb and chorus settings.
 */
public class FluidEffects
{

    public static final Reverb ZERO_REVERB = new Reverb(0, 0, 0, 0);
    public static final Chorus ZERO_CHORUS = new Chorus(0, 0.1d, 0d, 0, 0d);
    public static final FluidEffects ZERO = new FluidEffects("Zero", ZERO_REVERB, ZERO_CHORUS);

    public static final Reverb FLUID_REVERB = new Reverb(0.2d, 0d, 0.5d, 0.9d);
    public static final Chorus FLUID_CHORUS = new Chorus(3, 0.3d, 8d, 0, 2d);
    public static final FluidEffects FLUID_DEFAULT = new FluidEffects("FluidDefault", FLUID_REVERB, FLUID_CHORUS);

    public static final Reverb DOSBOX_X_REVERB = new Reverb(0.61d, 0.23d, 0.76d, 0.57d);
    public static final Chorus DOSBOX_X_CHORUS = new Chorus(3, 0.3d, 8d, 0, 1.2d);
    public static final FluidEffects DOSBOX_X = new FluidEffects("DosBosX", DOSBOX_X_REVERB, DOSBOX_X_CHORUS);

    public static final Reverb JJAZZLAB_REVERB = new Reverb(0.61d, 0.6d, 0.76d, 0.57d);
    public static final Chorus JJAZZLAB_CHORUS = new Chorus(3, 0.3d, 10d, 0, 0.8d);
    public static final FluidEffects JJAZZLAB = new FluidEffects("JJazzLab", JJAZZLAB_REVERB, JJAZZLAB_CHORUS);

    public static final List<FluidEffects> PRESETS = Arrays.asList(ZERO, FLUID_DEFAULT, DOSBOX_X, JJAZZLAB);

    private Reverb reverb;
    private Chorus chorus;
    private String preset;

    public FluidEffects(String preset)
    {
        this(preset, new Reverb(), new Chorus());
    }

    public FluidEffects(String preset, Reverb rev, Chorus cho)
    {
        this.preset = preset;
        this.reverb = rev;
        this.chorus = cho;
    }

    public void apply(JavaFluidSynth synth)
    {
        synth.setReverb(reverb);
        synth.setChorus(chorus);
    }

    public void set(FluidEffects fe)
    {
        this.preset = fe.getPreset();
        this.chorus = fe.getChorus();
        this.reverb = fe.getReverb();
    }

    @Override
    public String toString()
    {
        return preset;
    }

    public String getPreset()
    {
        return preset;
    }

    public Reverb getReverb()
    {
        return reverb;
    }

    public void setReverb(Reverb reverb)
    {
        this.reverb = reverb;
    }

    public Chorus getChorus()
    {
        return chorus;
    }

    public void setChorus(Chorus chorus)
    {
        this.chorus = chorus;
    }

    public void reset()
    {
        reverb = ZERO_REVERB;
        chorus = ZERO_CHORUS;
    }

    /**
     * All reverb parameters.
     */
    public record Reverb(double room, double damp, double width, double level)
            {

        public Reverb()
        {
            // Values from https://dosbox-x.com/wiki/Guide%3ASetting-up-MIDI-in-DOSBox%E2%80%90X  "carefully optimized for game music, much better than FluidSynth defaults..."
            this(JJAZZLAB_REVERB.room, JJAZZLAB_REVERB.damp, JJAZZLAB_REVERB.width, JJAZZLAB_REVERB.level);
        }

        public Reverb   
        {
            if (room < 0 || room > 1
                    || damp < 0 || damp > 1
                    || width < 0 || width > 100
                    || level < 0 || level > 1)
            {
                throw new IllegalArgumentException("room=" + room + " damp=" + damp + " width=" + width + " level=" + level);
            }
        }
    }


    /**
     * All chorus parameters
     */
    public record Chorus(int nr, double speed, double depth, int type, double level)
            {

        public Chorus()
        {
            this(JJAZZLAB_CHORUS.nr, JJAZZLAB_CHORUS.speed, JJAZZLAB_CHORUS.depth, JJAZZLAB_CHORUS.type, JJAZZLAB_CHORUS.level);
        }

        public Chorus    
        {
            if (nr < 0 || nr > 99
                    || speed < 0.1d || speed > 5
                    || depth < 0 || depth > 256
                    || type < 0 || type > 1 // 0 = Sine, 1 = Triangle
                    || level < 0 || level > 10)
            {
                throw new IllegalArgumentException("nr=" + nr + " speed=" + speed + " depth=" + depth + " type=" + type + " level=" + level);
            }
        }

    }
}
