/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midiconfig.api;

import com.google.common.base.Preconditions;
import javax.sound.midi.MidiDevice;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.outputsynth.api.OutputSynth;
import org.openide.util.Utilities;

/**
 * An MidiDeviceConfigs associates an output MidiDevice to an OutputSynth.
 */
public class MidiDeviceConfig
{

    private final MidiDevice outDevice;
    private final OutputSynth ouputSynth;

    public MidiDeviceConfig(MidiDevice outDevice)
    {
        this(outDevice, getOutputSynth(outDevice.getDeviceInfo()));
    }


    public MidiDeviceConfig(MidiDevice outDevice, OutputSynth ouputSynth)
    {
        Preconditions.checkNotNull(outDevice);
        Preconditions.checkNotNull(ouputSynth);
        this.outDevice = outDevice;
        this.ouputSynth = ouputSynth;
    }

    public MidiDevice getOutDevice()
    {
        return outDevice;
    }

    public OutputSynth getOuputSynth()
    {
        return ouputSynth;
    }

    // =========================================================================
    // Private methods
    // =========================================================================    

    /**
     * Provide the "best possible" OutputSynth based on a given MidiDevice description.
     *
     * @param mdInfo
     * @return
     */
    static private OutputSynth getOutputSynth(MidiDevice.Info mdInfo)
    {
        OutputSynth res;
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();

        if (Utilities.isWindows() && mdInfo.getName().toLowerCase().contains("virtualmidisynt"))
        {
            res = 
        } else
        {
            // GM synth by default
            res = 
        }

        return res;

    }
}
