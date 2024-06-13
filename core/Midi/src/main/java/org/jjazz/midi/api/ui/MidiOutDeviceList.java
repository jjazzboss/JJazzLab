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
import java.util.List;
import java.util.function.Predicate;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.Synthesizer;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import org.jjazz.midi.api.JJazzMidiSystem;

/**
 * A specialized JList to display system's OUT MidiDevices, including the Java internal synth.
 */
public class MidiOutDeviceList extends JList<MidiDevice>
{

    private final Predicate<MidiDevice> tester;
    private List<MidiDevice> outDevices = new ArrayList<>();


    /**
     * List contains all the MidiSystem MidiOutDevice instances.
     */
    public MidiOutDeviceList()
    {
        this(md -> true);
    }

    /**
     * List contains all the MidiSystem MidiOutDevice instances which satisfy tester.
     *
     * @param tester
     */
    public MidiOutDeviceList(Predicate<MidiDevice> tester)
    {
        this.tester = tester;
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        MidiDeviceRenderer mdr = new MidiDeviceRenderer();
        setCellRenderer(mdr);
        rescanMidiDevices();
    }

    public List<MidiDevice> getOutDevices()
    {
        return outDevices;
    }

    /**
     * Rescan the available OUT MidiDevices on the system and update the list accordingly.
     * <p>
     * Any selection is cleared. If the Java default synth is present, make it the first on the list.
     */
    public final void rescanMidiDevices()
    {
        outDevices.clear();
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        Synthesizer javaSynth = jms.getJavaInternalSynth();
        if (javaSynth != null && tester.test(javaSynth))
        {
            outDevices.add(javaSynth);
        }
        jms.getOutDeviceList().stream()
                .filter(tester)
                .forEach(md -> outDevices.add(md));

        setListData(outDevices.toArray(MidiDevice[]::new));
    }
}
