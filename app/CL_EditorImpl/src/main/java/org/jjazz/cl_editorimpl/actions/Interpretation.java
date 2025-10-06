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

import java.awt.event.ActionEvent;
import java.awt.Component;
import java.util.EnumSet;
import java.util.logging.Logger;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.cl_editorimpl.actions.AccentOptionsStronger.KEYSTROKE;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

/**
 * A submenu to let user update chord symbol interpretation options via a popupmenu.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.interpretation")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 470)
        })
public final class Interpretation extends CL_ContextAction implements Presenter.Popup
{

    JMenu subMenu;
    private static final Logger LOGGER = Logger.getLogger(Interpretation.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_Interpretation"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION));        
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = selection.isChordSymbolSelected();
        setEnabled(b);
        if (subMenu != null)
        {
            // subMenu.setEnabled(b);   testing if useless
        }
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_SelectionUtilities selection)
    {
        // Not used
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (subMenu == null)
        {
            assert SwingUtilities.isEventDispatchThread();
            subMenu = new JMenu(getActionName());
            var actions = Utilities.actionsForPath("Actions/ChordSymbolInterpretation");
            for (Action action : actions)
            {
                if (action == null)
                {
                    subMenu.add(new JSeparator());
                } else
                {
                    for (Component c : UIUtilities.actionToMenuItems(action, getContext()))
                    {
                        subMenu.add(c);
                    }
                }
            }
        }
        subMenu.setEnabled(isEnabled());
        return subMenu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================     

}
