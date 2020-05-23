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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import static org.jjazz.ui.cl_editor.actions.Bundle.CTL_AccentOptions;
import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

/**
 * Allow user to select a timesignature in a JPopupMenu when a CLI_Section is selected.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.accentoptions")
@ActionRegistration(displayName = "#CTL_AccentOptions", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 470)
        })
@Messages(
        {
            "CTL_AccentOptions=Accent Options"
        })
public final class AccentOptions extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup
{

    private CL_ContextActionSupport cap;
    private final Lookup context;
    JMenu subMenu;
    private static final Logger LOGGER = Logger.getLogger(AccentOptions.class.getSimpleName());

    public AccentOptions()
    {
        this(Utilities.actionsGlobalContext());
    }

    public AccentOptions(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        // Unused
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isItemSelected())
        {
            b = selection.getSelectedItems().stream()
                    .filter(item -> item instanceof CLI_ChordSymbol)
                    .anyMatch(item -> ((CLI_ChordSymbol) item).getData().getRenderingInfo().hasOneFeature(ChordRenderingInfo.Feature.ACCENT, ChordRenderingInfo.Feature.ACCENT_STRONGER));
        }
        setEnabled(b);
        getPopupPresenter().setEnabled(b);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new AccentOptions(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }


    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (subMenu == null)
        {
            subMenu = new JMenu(CTL_AccentOptions());
            var actions = Utilities.actionsForPath("Actions/ChordSymbolAccent");
            for (Action action : actions)
            {
                JMenuItem mi = org.jjazz.ui.utilities.Utilities.actionToMenuItem(action, context);
                subMenu.add(mi);
            }
        }
        return subMenu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================     

}
