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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.ui.cl_editor.barbox.api.BarBox;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;

public class MoveSelectionLeft extends AbstractAction
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof BarBox)
        {
            moveSelectionLeftBarBox(editor, ((BarBox) c).getBarIndex(), false);
        } else if (c instanceof ItemRenderer)
        {
            moveSelectionLeftIR(editor, ((ItemRenderer) c), false);
        }
    }

    /**
     * Move selection left from an ItemRenderer in specified editor.
     *
     * @param ed
     * @param ir ItemRenderer
     * @param extend If true extend the selection rather than move.
     */
    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    static public void moveSelectionLeftIR(CL_Editor ed, ItemRenderer ir, boolean extend)
    {
        ChordLeadSheetItem<?> fItem = ir.getModel();
        ChordLeadSheet cls = fItem.getContainer();
        List<? extends ChordLeadSheetItem> items = cls.getItems(0, fItem.getPosition().getBar(), fItem.getClass());
        int index = items.indexOf(fItem);
        if (index > 0)
        {
            ChordLeadSheetItem<?> item = items.get(index - 1);
            if (!extend)
            {
                CL_SelectionUtilities selection = new CL_SelectionUtilities(ed.getLookup());
                selection.unselectAll(ed);
            }
            ed.selectItem(item, true);
            ed.setFocusOnItem(item, ir.getIR_Type());
        }
    }

    /**
     * Move selection left from barIndex in specified editor.
     *
     * @param ed
     * @param barIndex
     * @param extend If true extend the selection rather than move.
     */
    public static void moveSelectionLeftBarBox(CL_Editor ed, int barIndex, boolean extend)
    {
        if (barIndex > 0)
        {
            if (!extend)
            {
                CL_SelectionUtilities selection = new CL_SelectionUtilities(ed.getLookup());
                selection.unselectAll(ed);
            }
            ed.selectBars(barIndex - 1, barIndex - 1, true);
            ed.setFocusOnBar(barIndex - 1);
        }
    }
}
