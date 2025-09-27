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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = SendXgOn.ACTION_CATEGORY, id = SendXgOn.ACTION_ID)
@ActionRegistration(displayName = "#CTL_SendXgOn", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 400)
        })
public class SendXgOn extends AbstractAction
{
    private final String undoText = ResUtil.getString(getClass(), "CTL_SendXgOn");
    public static final String ACTION_CATEGORY = "MixConsole";
    public static final String ACTION_ID = "org.jjazz.mixconsole.actions.sendxgon";
    private static final Logger LOGGER = Logger.getLogger(SendXgOn.class.getSimpleName());

    public SendXgOn()
    {
        putValue(NAME, undoText);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MidiUtilities.sendSysExMessage(MidiUtilities.getXgModeOnSysExMessage());
        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_XGMidiMessageSent"));
    }
}
