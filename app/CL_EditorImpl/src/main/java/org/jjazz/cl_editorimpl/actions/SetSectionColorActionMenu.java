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
package org.jjazz.cl_editorimpl.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.uisettings.api.ColorSetManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;

/**
 * Action menu to change the color of selected sections.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.SetSectionColor")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 2100, separatorBefore = 2099)
        })
public final class SetSectionColorActionMenu extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(SetSectionColorActionMenu.class.getSimpleName());

    public SetSectionColorActionMenu()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public SetSectionColorActionMenu(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(getClass(), "CTL_SetSectionColor"));

        var selection = new CL_Selection(context);
        boolean b = selection.isSectionSelected();
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }

        prepareMenu(menu, selection);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SetSectionColorActionMenu(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // Not used
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    
    private void prepareMenu(JMenu menu, CL_Selection selection)
    {
        ColorSetManager csm = ColorSetManager.getDefault();
        for (final Color c : csm.getReferenceColors())
        {
            JMenuItem mi = new JMenuItem("    ");
            mi.setEnabled(true);
            mi.setOpaque(true);
            mi.setBackground(c);
            mi.addActionListener(ae -> setColor(c, selection));
            menu.add(mi);
        }
    }

    private void setColor(Color c, CL_Selection selection)
    {
        for (var section : selection.getSelectedSections())
        {
            CL_EditorClientProperties.setSectionColor(section, c);
        }
        CL_EditorTopComponent.get(selection.getChordLeadSheet()).getEditor().getSongModel().setSaveNeeded(true);
        Analytics.logEvent("Set section color");
    }
}
