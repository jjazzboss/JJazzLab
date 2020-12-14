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
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

@ActionID(category = "OutputSynth", id = "org.jjazz.outputsynth.ui.editoutputsynth")
@ActionRegistration(displayName = "#CTL_editoutputsynth", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Menu/Edit", position = 98392)
        })
public class EditOutputSynthAction extends AbstractAction
{

    private String undoText = ResUtil.getString(getClass(), "CTL_editoutputsynth");
    private static final Logger LOGGER = Logger.getLogger(EditOutputSynthAction.class.getSimpleName());

    public EditOutputSynthAction()
    {
        putValue(NAME, undoText);
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/OutputSynth2.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/OutputSynth2.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_OpenOutputSynthEditor"));
        putValue("hideActionText", true);

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        EditOutputSynthDialog dlg = EditOutputSynthDialog.getInstance();
        OutputSynth outputSynth = OutputSynthManager.getInstance().getOutputSynth();
        dlg.preset(outputSynth);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
    }
  
}
