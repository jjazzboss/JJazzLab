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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.EnumSet;
import java.util.logging.Logger;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/**
 * Allow user to insert via a submenu various stuff when a bar is selected.
 * <p>
 * Actions displayed in the menu are the ones found in the layer.xml Actions/Bar/Insert folder.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insert")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 200)
        })
public final class Insert extends CL_ContextAction implements Presenter.Popup
{

    private JMenu subMenu;
    private static final Logger LOGGER = Logger.getLogger(Insert.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_Insert"));
    }

    @Override
    protected EnumSet<ListeningTarget> getListeningTargets()
    {
        return EnumSet.of(ListeningTarget.BAR_SELECTION);
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_SelectionUtilities selection)
    {
        // Nothing
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        setEnabled(selection.isBarSelected());
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (subMenu == null)
        {
            subMenu = new JMenu(getActionName());
            var actions = org.openide.util.Utilities.actionsForPath("Actions/BarInsert");
            for (Action action : actions)
            {
                for (Component c : org.jjazz.uiutilities.api.UIUtilities.actionToMenuItems(action, getContext()))
                {
                    subMenu.add(c);
                }
            }
        }
        subMenu.setEnabled(isEnabled());
        return subMenu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    

    // ============================================================================================= 
    // Private class
    // =============================================================================================    
}
