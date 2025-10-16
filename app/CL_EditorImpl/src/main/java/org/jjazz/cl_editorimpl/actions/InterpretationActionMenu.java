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
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.cl_editorimpl.actions.AccentOptionsStronger.KEYSTROKE;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

/**
 * Action menu to update chord symbol interpretation
 * <p>
 * Relies on actions found in layer.xml "Actions/ChordSymbolInterpretation".
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.interpretation")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 470)
        })
public final class InterpretationActionMenu extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(InterpretationActionMenu.class.getSimpleName());

    public InterpretationActionMenu()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public InterpretationActionMenu(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(getClass(), "CTL_Interpretation"));

        var selection = new CL_Selection(context);
        boolean b = selection.isChordSymbolSelected();
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }

        prepareMenu(menu, context);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new InterpretationActionMenu(lkp);
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

    private void prepareMenu(JMenu menu, Lookup context)
    {
        var actions = org.openide.util.Utilities.actionsForPath("Actions/ChordSymbolInterpretation");
        for (Action action : actions)
        {
            if (action == null)
            {
                menu.add(new JSeparator());
            } else
            {
                for (Component c : UIUtilities.actionToMenuItems(action, context))
                {
                    menu.add(c);
                }
            }
        }
    }

}
