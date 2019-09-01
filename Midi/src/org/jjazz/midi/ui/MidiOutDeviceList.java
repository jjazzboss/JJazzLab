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
package org.jjazz.midi.ui;

import java.util.ArrayList;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.Synthesizer;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import org.jjazz.midi.JJazzMidiSystem;

/**
 * A specialized JList to display system's OUT MidiDevices.
 */
public class MidiOutDeviceList extends JList<MidiDevice>
{

    public MidiOutDeviceList()
    {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        MidiDeviceRenderer mdr = new MidiDeviceRenderer();
        setCellRenderer(mdr);
        rescanMidiDevices();
    }

    /**
     * Rescan the available OUT MidiDevices on the system and update the list accordingly.
     * <p>
     * If the Java default synth is present, make it the first on the list.
     */
    public final void rescanMidiDevices()
    {
        ArrayList<MidiDevice> outDevices = new ArrayList<>();
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        Synthesizer synth = jms.getDefaultJavaSynth();
        if (synth != null)
        {
            outDevices.add(synth);
        }
        outDevices.addAll(jms.getOutDeviceList());
        setListData(outDevices.toArray(new MidiDevice[0]));
    }
}
