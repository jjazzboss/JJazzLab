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
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.actions.DeleteAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

/**
 * DeleteItem selected items or items of the selected bars.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.deleteitem")
@ActionRegistration(displayName = "Delete item", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1250),
            @ActionReference(path = "Actions/Section", position = 1250),
            @ActionReference(path = "Actions/ChordSymbol", position = 1250),
        })
@Messages("CTL_DeleteItem=Delete item")
public class DeleteItem extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private String undoText = CTL_DeleteItem();
    private static final Logger LOGGER = Logger.getLogger(DeleteItem.class.getSimpleName());

    public DeleteItem()
    {
        this(Utilities.actionsGlobalContext());
    }

    private DeleteItem(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_DeleteItem());
        Icon icon = SystemAction.get(DeleteAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("DELETE"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new DeleteItem(context);
    }

    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
        ArrayList<ChordLeadSheetItem<?>> items = new ArrayList<>();
        if (selection.isBarSelectedWithinCls())
        {
            for (Integer modelBarIndex : selection.getSelectedBarIndexesWithinCls())
            {
                items.addAll((Collection<? extends ChordLeadSheetItem<?>>) cls.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class));
            }
        } else
        {
            assert selection.isItemSelected() == true : " selection=" + selection;
            items.addAll(selection.getSelectedItems());
        }

        // Remove items
        for (ChordLeadSheetItem<?> item : items)
        {
            if (item instanceof CLI_Section)
            {
                if (item.getPosition().getBar() > 0)
                {
                    cls.removeSection((CLI_Section) item);
                }
            } else
            {
                cls.removeItem(item);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isItemSelected() || selection.isBarSelectedWithinCls())
        {
            b = true;
        }
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }
}
