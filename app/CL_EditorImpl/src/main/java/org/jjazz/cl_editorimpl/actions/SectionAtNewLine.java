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
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;


/**
 * Show the action as a checkbox menu item.
 * <p>
 * Action has no keyboard shortcut and is only showed in transient popup menus, no need for CL_ContextAction.
 */
@ActionRegistration(displayName = "not_used", lazy = false) // lazy can't be true because of Presenter.Popup implementation
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.sectionatnewline")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1420),
            @ActionReference(path = "Actions/Section", position = 2110)
        })
public final class SectionAtNewLine extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JCheckBoxMenuItem checkBoxMenuItem;
    private CL_Editor editor;
    private CLI_Section cliSection;
    private static final Logger LOGGER = Logger.getLogger(SectionAtNewLine.class.getSimpleName());

    public SectionAtNewLine()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public SectionAtNewLine(Lookup context)
    {
        Objects.requireNonNull(context);


        putValue(NAME, ResUtil.getString(getClass(), "CTL_SectionAtNewLine"));
        checkBoxMenuItem = new JCheckBoxMenuItem(this);


        var selection = new CL_Selection(context);
        CL_EditorTopComponent tc = CL_EditorTopComponent.get(selection.getChordLeadSheet());
        assert tc != null : "cls=" + selection.getChordLeadSheet();
        editor = tc.getEditor();


        cliSection = getSection(selection);
        setEnabled(cliSection != null);
        boolean b = cliSection != null && CL_EditorClientProperties.isSectionIsOnNewLine(cliSection);
        checkBoxMenuItem.setSelected(b);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SectionAtNewLine(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        CL_EditorClientProperties.setSectionIsOnNewLine(cliSection, checkBoxMenuItem.isSelected());
        editor.getSongModel().setSaveNeeded(true);
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        return checkBoxMenuItem;
    }

    // ======================================================================
    // Private methods
    // ======================================================================   

    /**
     * Get the section on which to operate.
     *
     * @param selection
     * @return Null means no valid selection
     */
    private CLI_Section getSection(CL_Selection selection)
    {
        CLI_Section res = null;
        if (selection.getSelectedSections().size() == 1)
        {
            res = (CLI_Section) selection.getSelectedItems().get(0);
        } else if (selection.getSelectedBarsWithinCls().size() == 1)
        {
            int selBarIndex = selection.getSelectedBarsWithinCls().get(0).getModelBarIndex();
            res = selection.getChordLeadSheet().getSection(selBarIndex);
            if (res.getPosition().getBar() != selBarIndex)
            {
                // SelectedBar is not a section bar
                res = null;
            }
        }
        if (res != null && res.getPosition().getBar() == 0)
        {
            res = null;
        }
        return res;
    }
}
