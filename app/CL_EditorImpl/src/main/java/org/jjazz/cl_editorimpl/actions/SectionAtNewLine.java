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
import java.util.logging.Logger;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

@ActionRegistration(displayName = "not_used", lazy = false) // lazy can't be true because of Presenter.Popup implementation
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.sectionatnewline")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1420),
            @ActionReference(path = "Actions/Section", position = 2110)
        })
public final class SectionAtNewLine extends CL_ContextAction implements Presenter.Popup
{

    private JCheckBoxMenuItem checkBoxMenuItem;
    private CL_Editor editor;
    private CLI_Section cliSection;
    private static final Logger LOGGER = Logger.getLogger(SectionAtNewLine.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SectionAtNewLine"));
    }

    @Override
    protected EnumSet<ListeningTarget> getListeningTargets()
    {
        return EnumSet.of(ListeningTarget.BAR_SELECTION, ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES);
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_SelectionUtilities selection)
    {
        assert cliSection != null && editor != null :
                "cliSection=" + cliSection + " editor=" + editor + " getSelection()=" + getSelection();
        CL_EditorClientProperties.setSectionIsOnNewLine(cliSection, checkBoxMenuItem.isSelected());
        editor.getSongModel().setSaveNeeded(true);
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
        boolean selected = false;
        if (e)
        {
            CL_EditorTopComponent tc = CL_EditorTopComponent.get(selection.getChordLeadSheet());
            assert tc != null : "cls=" + selection.getChordLeadSheet();
            editor = tc.getEditor();
            selected = CL_EditorClientProperties.isSectionIsOnNewLine(cliSection);
        }

        setEnabled(e);
        getPopupPresenter().setSelected(selected);  // Only update UI, actionPerformed() is not called
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
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
