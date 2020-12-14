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
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.sendxgon")
@ActionRegistration(displayName = "#CTL_SendXgOn", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 400)
        })
public class SendXgOn extends AbstractAction
{

    private MidiMix sonxGidiMix;
    private final String undoText = ResUtil.getString(getClass(), "CTL_SendXgOn");
    private static final Logger LOGGER = Logger.getLogger(SendXgOn.class.getSimpleName());

    public SendXgOn(MidiMix context)
    {
        sonxGidiMix = context;
        putValue(NAME, undoText);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MidiUtilities.sendSysExMessage(MidiUtilities.getXgModeOnSysExMessage());
        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_XGMidiMessageSent"));
    }
}
