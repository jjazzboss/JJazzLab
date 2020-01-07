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
package org.jjazz.outputsynth.ui;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.outputsynth.OutputSynthManager;
import static org.jjazz.outputsynth.ui.Bundle.CTL_EditConnectedSynth;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

@ActionID(category = "JJazz", id = "org.jjazz.outputsynth.ui.editConnectedSynth")
@ActionRegistration(displayName = "#CTL_EditConnectedSynth", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 98392)
        })
@NbBundle.Messages(
        {
            "CTL_EditConnectedSynth=Edit Connected Synth"
        })
public class EditOutputSynth extends AbstractAction
{

    private String undoText = CTL_EditConnectedSynth();
    private static final Logger LOGGER = Logger.getLogger(EditOutputSynth.class.getSimpleName());

    public EditOutputSynth()
    {
        putValue(NAME, undoText);
        // putValue(SHORT_DESCRIPTION, "");
        // putValue("hideActionText", true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        EditOutputSynthDialog dlg = EditOutputSynthDialog.getInstance();
        dlg.preset(OutputSynthManager.getInstance().getOutputSynth());
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
    }
}
