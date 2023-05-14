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
package org.jjazz.cl_editor.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;

/**
 * Allow user to insert via a submenu various stuff when a bar is selected.
 * <p>
 * Actions displayed in the menu are the ones found in the layer.xml Actions/Bar/Insert folder.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insert")
@ActionRegistration(displayName = "#CTL_Insert", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 200)
        })
public final class Insert extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JMenu subMenu;
    private final Lookup context;
    private final String undoText = ResUtil.getString(getClass(), "CTL_Insert");
    private static final Logger LOGGER = Logger.getLogger(Insert.class.getSimpleName());

    public Insert()
    {
        this(org.openide.util.Utilities.actionsGlobalContext());
    }

    public Insert(Lookup context)
    {
        this.context = context;
        putValue(NAME, undoText);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Useless
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Insert(context);
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (subMenu == null)
        {
            subMenu = new JMenu(undoText);
            var actions = org.openide.util.Utilities.actionsForPath("Actions/BarInsert");
            for (Action action : actions)
            {
                for (Component c : org.jjazz.uiutilities.api.UIUtilities.actionToMenuItems(action, context))
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
