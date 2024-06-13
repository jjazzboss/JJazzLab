/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
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
package org.jjazz.midi.api.sequencer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provide JDK's MidiSystem access to our Sequencer implementation.
 */
@ServiceProvider(service = MidiDeviceProvider.class)
public class JJazzLabSequencerProvider extends MidiDeviceProvider
{

    static private JJazzLabSequencer sequencer;

    @Override
    public MidiDevice.Info[] getDeviceInfo()
    {
        return new MidiDevice.Info[]
        {
            JJazzLabSequencer.info
        };
    }

    @Override
    public MidiDevice getDevice(MidiDevice.Info info)
    {
        if (info != JJazzLabSequencer.info)
        {
            throw new IllegalArgumentException("info=" + info);
        }
        if (sequencer == null)
        {
            sequencer = new JJazzLabSequencer();
        }
        return sequencer;
    }

}
