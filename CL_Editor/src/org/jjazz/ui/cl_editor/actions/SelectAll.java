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

import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.*;

/**
 * SelectAll : perform selection all in 2 steps, first in the current section, then in the whole leadsheet.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.selectall")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1300),
            @ActionReference(path = "Actions/Section", position = 1300),
            @ActionReference(path = "Actions/ChordSymbol", position = 1300)
        })
@Messages("CTL_SelectAll=Select all")
public class SelectAll extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(SelectAll.class.getSimpleName());

    public SelectAll()
    {
        this(Utilities.actionsGlobalContext());
    }

    private SelectAll(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_SelectAll());
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_A));
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        CL_Editor editor = CL_EditorTopComponent.get(cls).getCL_Editor();
        if (selection.isEmpty())
        {
            // Select all leadsheet
            int lastBar = cls.getSize() - 1;
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
    public void selectionChange(CL_SelectionUtilities selection)
    {
        setEnabled(true);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new SelectAll(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }

    /**
     *
     * @param editor
     * @param selection Will be non empty
     */
    private void selectBarsIn2Steps(CL_Editor editor, CL_SelectionUtilities selection)
    {
        ChordLeadSheet cls = editor.getModel();
        int clsSize = cls.getSize();
        if (selection.getSelectedBarIndexesWithinCls().size() < selection.getSelectedBars().size())
        {
            // There are some selected bars which are past end, reselect from 1st bar to last selected bar
            selection.unselectAll(editor);
            editor.selectBars(0, selection.geMaxBarIndex(), true);
            return;
        }

        CLI_Section sectionStart = cls.getSection(selection.getMinBarIndexWithinCls());
        int sectionIndex = sectionStart.getPosition().getBar();
        int sectionSize = cls.getSectionRange(sectionStart).size();
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
    private void selectItemsIn2Steps(CL_Editor editor, CL_SelectionUtilities selection)
    {
        ChordLeadSheet cls = editor.getModel();
        int minBarIndex = selection.getMinBarIndexWithinCls();
        int maxBarIndex = selection.getMaxBarIndexWithinCls();
        List<ChordLeadSheetItem<?>> items = selection.getSelectedItems();

        Class<? extends ChordLeadSheetItem> itemClass = items.get(0).getClass();
        CLI_Section sectionStart = cls.getSection(minBarIndex);
        CLI_Section sectionEnd = cls.getSection(maxBarIndex);
        List<ChordLeadSheetItem<?>> sectionItems = (List<ChordLeadSheetItem<?>>) cls.getItems(sectionStart, itemClass);

        if (sectionStart == sectionEnd && items.size() < sectionItems.size())
        {
            // Selection is within 1 section, extend selection to current section only     
            editor.selectItems(sectionItems, true);
        } else
        {
            // Select all similar items in the leadsheet
            editor.selectItems((List<ChordLeadSheetItem<?>>) cls.getItems(itemClass), true);
        }

    }
}
