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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.cl_editorimpl.ItemsTransferable;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editorimpl.BarsTransferable;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.CutAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.SystemAction;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.cut")
@ActionRegistration(displayName = "cl-cut-not-used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 1000),
            @ActionReference(path = "Actions/ChordSymbol", position = 1000, separatorBefore = 950),
            @ActionReference(path = "Actions/Bar", position = 1000),
            @ActionReference(path = "Actions/BarAnnotation", position = 1000, separatorBefore = 999)
        })
public class Cut extends CL_ContextAction implements ClipboardOwner
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_X);

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getCommonString("CTL_Cut"));
        Icon icon = SystemAction.get(CutAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES, ListeningTarget.BAR_SELECTION));                
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        Transferable t = null;
        List<ChordLeadSheetItem> items = new ArrayList<>();


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());


        // Prepare the transferable        
        if (selection.isBarSelectedWithinCls())
        {
            var clsBarIndexes = selection.getSelectedBarIndexesWithinCls();
            for (Integer modelBarIndex : clsBarIndexes)
            {
                items.addAll(cls.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class));
            }

            IntRange barRange = selection.getBarRangeWithinCls();
            var firstSection = cls.getSection(clsBarIndexes.get(0)).getData();
            var data = new BarsTransferable.Data(firstSection, barRange, items);
            t = new BarsTransferable(data);

            try
            {
                cls.deleteBars(barRange.from, barRange.to);
            } catch (UnsupportedEditException ex)
            {
                String msg = "Impossible to cut bars.\n" + ex.getLocalizedMessage();
                um.abortCEdit(getActionName(), msg);
                return;
            }
        } else if (selection.isItemSelected())
        {
            items.addAll(selection.getSelectedItems());

            var data = new ItemsTransferable.Data(items);
            t = new ItemsTransferable(data);


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
                            um.abortCEdit(getActionName(), msg);
                            return;
                        }
                    }
                } else
                {
                    cls.removeItem(item);
                }
            }
        }


        // Store into clipboard
        assert t != null;
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(t, this);


        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;
        if (selection.isItemSelected() || selection.isContiguousBarboxSelectionWithinCls())
        {
            b = true;
        }
        setEnabled(b);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
    }

    // =========================================================================================================
    // ClipboardOwner
    // =========================================================================================================    
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {
        // Nothing
    }


}
