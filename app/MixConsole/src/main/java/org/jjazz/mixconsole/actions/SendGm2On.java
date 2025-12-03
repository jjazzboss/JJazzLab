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
package org.jjazz.mixconsole.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.sound.midi.MidiDevice;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.FluidSynthUtils;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = SendGm2On.ACTION_CATEGORY, id = SendGm2On.ACTION_ID)
@ActionRegistration(displayName = "#CTL_SendGm2On", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 318)
        })
public class SendGm2On extends AbstractAction implements PropertyChangeListener
{
    private final String undoText = ResUtil.getString(getClass(), "CTL_SendGm2On");
    public static final String ACTION_CATEGORY = "MixConsole";
    public static final String ACTION_ID = "org.jjazz.mixconsole.actions.sendgm2on";

    public SendGm2On()
    {
        putValue(NAME, undoText);

        this.setEnabled(!FluidSynthUtils.IS_FLUID_SYNTH_IN_USE());

        JJazzMidiSystem jms =  JJazzMidiSystem.getInstance();
        jms.addPropertyChangeListener(JJazzMidiSystem.PROP_MIDI_OUT, this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MidiUtilities.sendSysExMessage(MidiUtilities.getGm2ModeOnSysExMessage());
        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_GM2MidiMessageSent"));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(!FluidSynthUtils.IS_FLUID_SYNTH((MidiDevice) evt.getNewValue()));
    }
}
