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
import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.ss_editor.api.SS_Selection;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.ss_editor.api.SS_ContextAction;
import org.jjazz.uisettings.api.ColorSetManager;
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.actions.Presenter;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.setparentsectioncolor")
@ActionRegistration(displayName = "#CTL_SetParentSectionColor", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1400)
        })
public class SetParentSectionColor extends SS_ContextAction implements Presenter.Popup
{
    private ColorMenu menu;
    private static final Logger LOGGER = Logger.getLogger(SetParentSectionColor.class.getSimpleName());

   @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SetParentSectionColor"));
        // putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        // Not used
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = selection.isSongPartSelected();
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }


    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (menu == null)
        {
            menu = new ColorMenu(getActionName());
        }
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    
    private void setColorOfSelectedSpts(Color c)
    {
        SS_Selection selection = getSelection();
        List<SongPart> spts = selection.getIndirectlySelectedSongParts();
        for (var spt : spts)
        {
            var parentSection = spt.getParentSection();
            CL_EditorClientProperties.setSectionColor(parentSection, c);
        }
    }

    // ============================================================================================= 
    // Private class
    // =============================================================================================    
    private class ColorMenu extends JMenu implements PropertyChangeListener
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
                    org.jjazz.uiutilities.api.UIUtilities.invokeLaterIfNeeded(() -> prepareMenu());
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
                mi.addActionListener(ae -> setColorOfSelectedSpts(c));
                add(mi);
            }
        }
    }
}
