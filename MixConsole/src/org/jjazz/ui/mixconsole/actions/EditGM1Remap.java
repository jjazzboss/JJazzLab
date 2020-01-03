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

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.outputsynth.OutputSynthManager;
import static org.jjazz.ui.mixconsole.actions.Bundle.CTL_EditRemap;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

@ActionID(category = "JJazz", id = "org.jjazz.ui.mixconsole.actions.editGM1Remap")
@ActionRegistration(displayName = "#CTL_EditRemap", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 983948)
        })
@NbBundle.Messages(
        {
            "CTL_EditRemap=Edit GM1 Remap"
        })
public class EditGM1Remap extends AbstractAction
{

    private String undoText = CTL_EditRemap();
    private static final Logger LOGGER = Logger.getLogger(EditGM1Remap.class.getSimpleName());

    public EditGM1Remap()
    {
        putValue(NAME, undoText);
        // putValue(SHORT_DESCRIPTION, "");
        // putValue("hideActionText", true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        EditGM1RemapDialog dlg = EditGM1RemapDialog.getInstance();
        dlg.preset(OutputSynthManager.getInstance().getOutputSynth());
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
    }
}
