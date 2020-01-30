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
package org.jjazz.ui.mixconsole.actions;

import org.jjazz.midimix.MidiMix;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.midi.MidiUtilities;
import static org.jjazz.ui.mixconsole.actions.Bundle.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.sendgson")
@ActionRegistration(displayName = "#CTL_SendGsOn", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 430)
        })
@NbBundle.Messages(
        {
            "CTL_SendGsOn=Send Roland GS Midi On message",
            "CTL_GSMidiMessageSent=GS Midi ON message sent"
        })
public class SendGsOn extends AbstractAction
{

    private MidiMix sonxGidiMix;
    private String undoText = CTL_SendGsOn();
    private static final Logger LOGGER = Logger.getLogger(SendGsOn.class.getSimpleName());

    public SendGsOn(MidiMix context)
    {
        sonxGidiMix = context;
        putValue(NAME, undoText);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MidiUtilities.sendSysExMessage(MidiUtilities.getGsMidiOnSysExMessage());
        StatusDisplayer.getDefault().setStatusText(CTL_GSMidiMessageSent());
    }
}
