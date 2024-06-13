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
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.panic")
// Need lazy=false for the tooltip to work!
@ActionRegistration(displayName = "#CTL_Panic", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 200),
            @ActionReference(path = "Actions/MixConsole/Master", position = 100)

        })
public class Panic extends AbstractAction
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_Panic");
    private static final Logger LOGGER = Logger.getLogger(Panic.class.getSimpleName());

    public Panic()
    {
        putValue(NAME, " " + undoText + " ");       // " " quick hack because small font small button
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PanicTooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JJazzMidiSystem.getInstance().panic();
        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_SendingPanic"));
    }
}
