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
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ui.cl_editor.api.CopyBuffer;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.ui.utilities.api.Utilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.actions.CutAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.cut")
@ActionRegistration(displayName = "#CTL_Cut", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 1000),
            @ActionReference(path = "Actions/ChordSymbol", position = 1000, separatorBefore = 950),
            @ActionReference(path = "Actions/Bar", position = 1000),
        })
public class Cut extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getString(getClass(), "CTL_Cut");

    public Cut()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Cut(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        Icon icon = SystemAction.get(CutAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_X));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Cut(context);
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
        CopyBuffer copyBuffer = CopyBuffer.getInstance();
        ArrayList<ChordLeadSheetItem<?>> items = new ArrayList<>();

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(undoText);


        if (selection.isBarSelectedWithinCls())
        {
            for (Integer modelBarIndex : selection.getSelectedBarIndexesWithinCls())
            {
                items.addAll((Collection<? extends ChordLeadSheetItem<?>>) cls.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class));
            }


            int minBarIndex = selection.getMinBarIndexWithinCls();
            int maxBarIndex = selection.getMaxBarIndexWithinCls();


            copyBuffer.barModeCopy(items, maxBarIndex, maxBarIndex);
            try
            {
                cls.deleteBars(minBarIndex, maxBarIndex);
            } catch (UnsupportedEditException ex)
            {
                String msg = "Impossible to cut bars.\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(undoText, msg);
                return;
            }
        } else if (selection.isItemSelected())
        {
            items.addAll(selection.getSelectedItems());
            copyBuffer.itemModeCopy(items);


            // Remove the items
            for (ChordLeadSheetItem item : items)
            {
                if (item instanceof CLI_Section)
                {
                    CLI_Section section = (CLI_Section) item;
                    if (section.getPosition().getBar() > 0)
                    {
                        try
                        {
                            cls.removeSection(section);
                        } catch (UnsupportedEditException ex)
                        {
                            String msg = "Impossible to cut section " + section.getData().getName() + ".\n" + ex.getLocalizedMessage();
                            um.handleUnsupportedEditException(undoText, msg);
                            return;
                        }
                    }
                } else
                {
                    cls.removeItem(item);
                }
            }
        }


        um.endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isItemSelected() || selection.isContiguousBarboxSelectionWithinCls())
        {
            b = true;
        }
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }
}
