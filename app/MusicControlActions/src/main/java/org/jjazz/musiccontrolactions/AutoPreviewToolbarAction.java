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
package org.jjazz.musiccontrolactions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/**
 * Insert ExtendedToolbarPanel as a toolbar.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.musiccontrolactions.autopreviewtoolbaraction")
@ActionRegistration(displayName = "not used", lazy = false)   // Need to be false because we implement Presenter.Toolbar
@ActionReferences(
        {
            @ActionReference(path = "Toolbars/AutoPreviewToolbar")   // see Base module / layer.xml for toolbars ordering (position here has mysteriously no impact)
        })
public class AutoPreviewToolbarAction extends AbstractAction implements Presenter.Toolbar
{
    private final AutoPreviewToolbarPanel panel;
    private static final Logger LOGGER = Logger.getLogger(AutoPreviewToolbarAction.class.getSimpleName());

    public AutoPreviewToolbarAction()
    {
        // Build the component
        panel = new AutoPreviewToolbarPanel();
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
