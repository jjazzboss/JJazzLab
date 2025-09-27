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
package org.jjazz.mixconsole;

import javax.sound.midi.MidiDevice;
import org.jjazz.midi.api.JJazzMidiSystem;


public class SynthUtils {

    public static boolean IS_FLUID_SYNTH_IN_USE()
    {
        var md = JJazzMidiSystem.getInstance().getDefaultOutDevice();
        return SynthUtils.IS_FLUID_SYNTH(md.getDeviceInfo().getName());
    }

    public static boolean IS_FLUID_SYNTH(Object midiDeviceObj)
    {
        if (midiDeviceObj != null);
        {
            try
            {
                MidiDevice md = (MidiDevice) midiDeviceObj;
                return IS_FLUID_SYNTH(md.getDeviceInfo().getName());
            } catch (ClassCastException | NullPointerException e)
            {
                return false;
            }
        }
    }

    public static boolean IS_FLUID_SYNTH(String midiDeviceName)
    {
        return midiDeviceName.toLowerCase().contains("fluidsynth");
    }
}
