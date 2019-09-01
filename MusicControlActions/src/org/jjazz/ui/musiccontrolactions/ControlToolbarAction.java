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
package org.jjazz.ui.musiccontrolactions;

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

@ActionID(category = "JJazz", id = "org.jjazz.ui.musiccontrolactions.controltoolbaraction")
@ActionRegistration(displayName = "#CTL_ControlToolbarAction", lazy = false)   // Need to be false because we implement Presenter.Toolbar
@ActionReferences(
        {
            @ActionReference(path = "Toolbars/MusicControls", position = 50)     // This will insert our toolbar
        })
@NbBundle.Messages(
        {
            "CTL_ControlToolbarAction=Music Control Toolbar"
        })
public class ControlToolbarAction extends AbstractAction implements Presenter.Toolbar
{

    private ControlToolbarPanel panel;
    private static final Logger LOGGER = Logger.getLogger(ControlToolbarAction.class.getSimpleName());

    public ControlToolbarAction()
    {
        // Build the component
        panel = new ControlToolbarPanel();
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
