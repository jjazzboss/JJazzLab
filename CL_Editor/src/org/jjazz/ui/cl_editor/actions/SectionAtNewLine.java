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
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import static org.jjazz.ui.cl_editor.actions.Bundle.CTL_SectionAtNewLine;
import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

@ActionRegistration(displayName = "#CTL_SectionAtNewLine", lazy = false)       // lazy can't be true because of Presenter.Popup implementation
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.sectionatnewline")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1420),
            @ActionReference(path = "Actions/Section", position = 2100)
        })
@Messages("CTL_SectionAtNewLine=Force section at new line")
public final class SectionAtNewLine extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, Presenter.Popup
{

    private JCheckBoxMenuItem checkBoxMenuItem;
    private Lookup context;
    private CL_ContextActionSupport cap;
    private CL_Editor editor;
    private CLI_Section cliSection;
    private static final Logger LOGGER = Logger.getLogger(SectionAtNewLine.class.getSimpleName());

    public SectionAtNewLine()
    {
        this(Utilities.actionsGlobalContext());
    }

    public SectionAtNewLine(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_SectionAtNewLine());
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new SectionAtNewLine(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert cliSection != null && editor != null :
                "cliSection=" + cliSection + " editor=" + editor + " cap.getSelection()=" + cap.getSelection();
        editor.setSectionStartOnNewLine(cliSection, checkBoxMenuItem.isSelected());
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        editor = null;
        cliSection = null;

        // Retrieve the selected section if valid
        if (selection.getSelectedSections().size() == 1)
        {
            cliSection = (CLI_Section) selection.getSelectedItems().get(0);
        } else if (selection.getSelectedBarsWithinCls().size() == 1)
        {
            int selBarIndex = selection.getSelectedBarsWithinCls().get(0).getModelBarIndex();
            cliSection = selection.getChordLeadSheet().getSection(selBarIndex);
            if (cliSection.getPosition().getBar() != selBarIndex)
            {
                // SelectedBar is not a section bar
                cliSection = null;
            }
        }

        // Update enabled and selected status
        boolean e = (cliSection != null) && cliSection.getPosition().getBar() != 0;
        boolean s = false;
        if (e)
        {
            CL_EditorTopComponent tc = CL_EditorTopComponent.get(selection.getChordLeadSheet());
            if (tc != null)
            {
                editor = tc.getCL_Editor();
                s = editor.isSectionStartOnNewLine(cliSection);
            }
        }
        setEnabled(e);
        getPopupPresenter().setSelected(s);  // Only update UI, actionPerformed() is not called
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
        if (checkBoxMenuItem == null)
        {
            checkBoxMenuItem = new JCheckBoxMenuItem(this);
        }
        return checkBoxMenuItem;
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
}
