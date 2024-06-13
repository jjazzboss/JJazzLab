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

import java.awt.Component;
import javax.sound.midi.MidiDevice;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import org.jjazz.midi.api.JJazzMidiSystem;

/**
 * A list renderer for MidiDevice.
 */
public class MidiDeviceRenderer extends DefaultListCellRenderer
{

    @Override
    @SuppressWarnings("rawtypes")
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        JComponent jc = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        MidiDevice md = (MidiDevice) value;
        String txt = JJazzMidiSystem.getInstance().getDeviceFriendlyName(md);
        setText(txt);
        jc.setToolTipText(md.getDeviceInfo().getDescription());
        return jc;
    }
}
