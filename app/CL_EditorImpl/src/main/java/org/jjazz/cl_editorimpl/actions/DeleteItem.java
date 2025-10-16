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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.DeleteAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.SystemAction;

/**
 * DeleteItem selected items or items of the selected bars.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.deleteitem")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 410),
            @ActionReference(path = "Actions/Section", position = 230, separatorAfter = 250),
            @ActionReference(path = "Actions/ChordSymbol", position = 480),
            @ActionReference(path = "Actions/BarAnnotation", position = 110)
        })
public class DeleteItem extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("DELETE");
    private static final Logger LOGGER = Logger.getLogger(DeleteItem.class.getSimpleName());


    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_DeleteItem"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        Icon icon = SystemAction.get(DeleteAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES, ListeningTarget.BAR_SELECTION));                
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        List<ChordLeadSheetItem> items = new ArrayList<>();


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());


        if (selection.isBarSelectedWithinCls())
        {
            for (Integer modelBarIndex : selection.getSelectedBarIndexesWithinCls())
            {
                items.addAll(cls.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class));
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
        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;
        if (selection.isItemSelected() || selection.isBarSelectedWithinCls())
        {
            b = true;
        }
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
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
}
