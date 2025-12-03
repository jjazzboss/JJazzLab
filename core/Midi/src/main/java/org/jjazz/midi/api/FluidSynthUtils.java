/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2025 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midi.api;

import javax.sound.midi.MidiDevice;


/**
 * Helper methods to quickly detect if the internal FluidSynth is the current Midi output, without having to rely on OutputSynth or FluidSynthEmbeddedSynth.
 */
public class FluidSynthUtils
{

    public static boolean IS_FLUID_SYNTH_IN_USE()
    {
        return IS_FLUID_SYNTH(JJazzMidiSystem.getInstance().getDefaultOutDevice());
    }

    public static boolean IS_FLUID_SYNTH(MidiDevice md)
    {
        return md != null && IS_FLUID_SYNTH(md.getDeviceInfo().getName());
    }

    public static boolean IS_FLUID_SYNTH(String midiDeviceOutputName)
    {
        return midiDeviceOutputName.toLowerCase().contains("fluidsynth");
    }
}
