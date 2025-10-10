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
package org.jjazz.ss_editorimpl.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.ss_editor.api.SS_Selection;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.uisettings.api.ColorSetManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;


/**
 * Show a submenu to update the parent section color (and the linked song parts).
 * <p>
 * Simplified implementation because action is only used in a popup menu, with no shortcut key.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.setparentsectioncolor")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1400)
        })
public class SetParentSectionColor extends AbstractAction implements Presenter.Popup, ContextAwareAction
{
    
    private JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(SetParentSectionColor.class.getSimpleName());
    
    public SetParentSectionColor()
    {
        // Not used besides for creating the ContextAwareAction
    }
    
    public SetParentSectionColor(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(getClass(), "CTL_SetParentSectionColor"));
        
        var selection = new SS_Selection(context);
        boolean b = selection.isSongPartSelected();
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }
        
        prepareMenu(menu, selection.getSelectedSongParts());
    }
    
    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SetParentSectionColor(lkp);
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
    private void prepareMenu(JMenu menu, List<SongPart> spts)
    {
        ColorSetManager csm = ColorSetManager.getDefault();
        for (final Color c : csm.getReferenceColors())
        {
            JMenuItem mi = new JMenuItem("    ");
            mi.setEnabled(true);
            mi.setOpaque(true);
            mi.setBackground(c);
            mi.addActionListener(ae -> setColor(c, spts));
            menu.add(mi);
        }
    }
    
    private void setColor(Color c, List<SongPart> spts)
    {
        for (var spt : spts)
        {
            CL_EditorClientProperties.setSectionColor(spt.getParentSection(), c);
        }
        SS_EditorTopComponent.getActive().getEditor().getSongModel().setSaveNeeded(true);
        Analytics.logEvent("Set parent section color");
    }
}
