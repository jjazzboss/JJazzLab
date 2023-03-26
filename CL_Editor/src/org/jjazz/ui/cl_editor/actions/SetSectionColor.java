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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/**
 * Allow user to change color of selected sections via a JPopupMenu.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.SetSectionColor")
@ActionRegistration(displayName = "#CTL_SetSectionColor", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 2100, separatorBefore=2099)
        })
public final class SetSectionColor extends AbstractAction implements Presenter.Popup
{

    private ColorMenu menu;
    private final String undoText = ResUtil.getString(getClass(), "CTL_SetSectionColor");
    private static final Logger LOGGER = Logger.getLogger(SetSectionColor.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Useless
    }

    
    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (menu == null)
        {
            menu = new ColorMenu(undoText);
        }
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    

    // ============================================================================================= 
    // Private class
    // =============================================================================================    
    private static class ColorMenu extends JMenu implements PropertyChangeListener
    {

        public ColorMenu(String title)
        {
            super(title);

            prepareMenu();

            ColorSetManager.getDefault().addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            // We may be outside the EDT
            if (e.getSource() == ColorSetManager.getDefault())
            {
                if (e.getPropertyName().equals(ColorSetManager.PROP_REF_COLOR_CHANGED))
                {
                    org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(() -> prepareMenu());
                }
            }
        }

        private void prepareMenu()
        {
            removeAll();

            ColorSetManager csm = ColorSetManager.getDefault();

            for (final Color c : csm.getReferenceColors())
            {
                JMenuItem mi = new JMenuItem("    ");
                mi.setEnabled(true);                
                mi.setOpaque(true);
                mi.setBackground(c);
                mi.addActionListener(ae -> 
                {
                    CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
                    CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
                    for (var cliSection: selection.getSelectedSections())
                    {
                        editor.setSectionColor(cliSection, c);
                    }
                });
                add(mi);
            }
        }
    }
}
