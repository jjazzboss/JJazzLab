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
package org.jjazz.midi.api.ui;

import java.util.ArrayList;
import javax.sound.midi.MidiDevice;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import org.jjazz.midi.api.JJazzMidiSystem;

/**
 * A specialized JList to display system's IN MidiDevices.
 */
public class MidiInDeviceList extends JList<MidiDevice>
{

    public MidiInDeviceList()
    {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        MidiDeviceRenderer mdr = new MidiDeviceRenderer();
        setCellRenderer(mdr);
        rescanMidiDevices();
    }

    /**
     * Rescan the available IN MidiDevices on the system and update the list accordingly.
     * <p>
     */
    public final void rescanMidiDevices()
    {
        ArrayList<MidiDevice> inDevices = new ArrayList<>();
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        inDevices.addAll(jms.getInDeviceList());
        setListData(inDevices.toArray(new MidiDevice[0]));
    }
}
