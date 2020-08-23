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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

@ActionID(category = "JJazz", id = "org.jjazz.outputsynth.ui.outputsynthtoolbaraction")
@ActionRegistration(displayName = "#CTL_OutputSynthToolBar", lazy = false)   // Need to be false because we implement Presenter.Toolbar
@ActionReferences(
        {
            @ActionReference(path = "Toolbars/OutputSynth", position = 450)     // This will insert our toolbar
        })
@NbBundle.Messages(
        {
            "CTL_OutputSynthToolBar=Output Synth Toolbar"
        })
public class OutputSynthlToolbarAction extends AbstractAction implements Presenter.Toolbar
{

    private OutputSynthToolbarPanel panel;
    private static final Logger LOGGER = Logger.getLogger(OutputSynthlToolbarAction.class.getSimpleName());

    public OutputSynthlToolbarAction()
    {
        // Build the component
        panel = new OutputSynthToolbarPanel();

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {       
        // Not used because of Presenter.Toolbar implementation
    }

    @Override
    public Component getToolbarPresenter()
    {        
        return panel;
    }
}
