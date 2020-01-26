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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
import static org.jjazz.outputsynth.ui.Bundle.CTL_editoutputsynth;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

@ActionID(category = "OutputSynth", id = "org.jjazz.outputsynth.ui.editoutputsynth")
@ActionRegistration(displayName = "#CTL_editoutputsynth", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Menu/Edit", position = 98392)
        })
@NbBundle.Messages(
        {
            "CTL_editoutputsynth=Configure Output Synth",
            "CTL_editoutputsynthTooltip=Configure the output synth connected to JJazzLab"
        })
public class EditOutputSynthAction extends AbstractAction implements PropertyChangeListener
{

    private String undoText = CTL_editoutputsynth();
    private static final Logger LOGGER = Logger.getLogger(EditOutputSynthAction.class.getSimpleName());

    public EditOutputSynthAction()
    {
        putValue(NAME, undoText);
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/OutputSynth2.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/outputsynth/ui/resources/OutputSynth2.png")));
        putValue("hideActionText", true);
        OutputSynthManager osm = OutputSynthManager.getInstance();
        osm.addPropertyChangeListener(this);
        updateActionName(osm.getOutputSynth());
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

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == OutputSynthManager.getInstance())
        {
            if (evt.getPropertyName().equals(OutputSynthManager.PROP_DEFAULT_OUTPUTSYNTH))
            {
                updateActionName((OutputSynth) evt.getNewValue());
            }
        }
    }

    private void updateActionName(OutputSynth outSynth)
    {
        File f = outSynth.getFile();
        String s = (f == null) ? "." : ". Current=" + f.getName();
        // putValue(Action.NAME, f == null ? null : f.getName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_editoutputsynthTooltip() + s);
    }
}
