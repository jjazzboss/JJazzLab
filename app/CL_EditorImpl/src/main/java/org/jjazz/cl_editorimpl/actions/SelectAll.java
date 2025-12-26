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
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.logging.Logger;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * SelectAll : perform selection all in 2 steps, first in the current section, then in the whole leadsheet.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.selectall")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
    {
        @ActionReference(path = "Actions/Bar", position = 1300),
        @ActionReference(path = "Actions/Section", position = 1300),
        @ActionReference(path = "Actions/ChordSymbol", position = 1300),
        @ActionReference(path = "Actions/BarAnnotation", position = 1300)
    })
public class SelectAll extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_A);
    private static final Logger LOGGER = Logger.getLogger(SelectAll.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SelectAll"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES, ListeningTarget.BAR_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.get(cls).getEditor();
        if (selection.isEmpty())
        {
            // Select all leadsheet
            int lastBar = cls.getSizeInBars() - 1;
            editor.selectBars(0, lastBar, true);
        } else if (selection.isBarSelected())
        {
            selectBarsIn2Steps(editor, selection);
        } else
        {
            selectItemsIn2Steps(editor, selection);
        }
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        setEnabled(true);
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
    // Private methods
    // =============================================================================================   
    /**
     *
     * @param editor
     * @param selection Will be non empty
     */
    private void selectBarsIn2Steps(CL_Editor editor, CL_Selection selection)
    {
        ChordLeadSheet cls = editor.getModel();
        int clsSize = cls.getSizeInBars();
        if (selection.getSelectedBarIndexesWithinCls().size() < selection.getSelectedBars().size())
        {
            // There are some selected bars which are past end, reselect from 1st bar to last selected bar
            editor.clearSelection();
            editor.selectBars(0, selection.getMaxBarIndex(), true);
            return;
        }

        CLI_Section sectionStart = cls.getSection(selection.getMinBarIndexWithinCls());
        int sectionIndex = sectionStart.getPosition().getBar();
        int sectionSize = cls.getBarRange(sectionStart).size();
        CLI_Section sectionEnd = cls.getSection(selection.getMaxBarIndexWithinCls());

        if (sectionStart == sectionEnd && selection.getSelectedBarIndexesWithinCls().size() < sectionSize)
        {
            // Selection is within 1 section, extend selection to current section 
            editor.selectBars(sectionIndex, sectionIndex + sectionSize - 1, true);
        } else
        {
            // Select the whole leadsheet
            editor.selectBars(0, clsSize - 1, true);
        }
    }

    @SuppressWarnings(
        {
            "unchecked", "rawtypes"
        })
    private void selectItemsIn2Steps(CL_Editor editor, CL_Selection selection)
    {
        ChordLeadSheet cls = editor.getModel();
        int minBarIndex = selection.getMinBarIndexWithinCls();
        int maxBarIndex = selection.getMaxBarIndexWithinCls();
        var items = selection.getSelectedItems();

        var itemClass = items.get(0).getClass();
        CLI_Section sectionStart = cls.getSection(minBarIndex);
        CLI_Section sectionEnd = cls.getSection(maxBarIndex);
        var sectionItems = cls.getItems(sectionStart, itemClass);

        if (sectionStart == sectionEnd && items.size() < sectionItems.size())
        {
            // Selection is within 1 section, extend selection to current section only     
            editor.selectItems(sectionItems, true);
        } else
        {
            // Select all similar items in the leadsheet
            editor.selectItems(cls.getItems(itemClass), true);
        }

    }
}
