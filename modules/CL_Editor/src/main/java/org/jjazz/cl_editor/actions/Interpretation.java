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
package org.jjazz.cl_editor.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
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
 * A submenu to directly set some interpretations options via the chord symbol popupmenu.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.interpretation")
@ActionRegistration(displayName = "#CTL_Interpretation", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 470)
        })
public final class Interpretation extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup
{

    private CL_ContextActionSupport cap;
    private final Lookup context;
    JMenu subMenu;
    private ChordLeadSheet currentCls;
    private static final Logger LOGGER = Logger.getLogger(Interpretation.class.getSimpleName());

    public Interpretation()
    {
        this(Utilities.actionsGlobalContext());
    }

    public Interpretation(Lookup context)
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
       
        // Disable menu of no accent features ON on selected chord symbols
        boolean b = selection.isChordSymbolSelected();
        setEnabled(b);
        if (subMenu != null)
        {
            subMenu.setEnabled(b);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Interpretation(context);
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
            subMenu = new JMenu(ResUtil.getString(getClass(), "CTL_Interpretation"));
            var actions = Utilities.actionsForPath("Actions/ChordSymbolInterpretation");   
            for (Action action : actions)
            {
                if (action == null)
                {
                    subMenu.add(new JSeparator());
                } else
                {
                    for (Component c : org.jjazz.uiutilities.api.UIUtilities.actionToMenuItems(action, context))
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
