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
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ui.cl_editor.api.CopyBuffer;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.actions.PasteAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

/**
 * Paste chordleadsheetitems chordsymbols or sections, possibly across songs.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.paste")
@ActionRegistration(displayName = "#CTL_Paste", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 1200),
            @ActionReference(path = "Actions/ChordSymbol", position = 1200),
            @ActionReference(path = "Actions/Bar", position = 1200),
        })
@NbBundle.Messages(
        {
            "CTL_Paste=Paste",
            "ERR_Paste=Impossible to paste"
        })
public class Paste extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private String undoText = CTL_Paste();

    public Paste()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Paste(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        Icon icon = SystemAction.get(PasteAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_V));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Paste(context);
    }

    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        CopyBuffer copyBuffer = CopyBuffer.getInstance();
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet targetCls = selection.getChordLeadSheet();
        int targetBarIndex = selection.geMinBarIndex();
        int lastBar = targetCls.getSize() - 1;


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(targetCls);
        um.startCEdit(undoText);


        if (!copyBuffer.isEmpty())
        {
            if (copyBuffer.isBarCopyMode())
            {
                int range = copyBuffer.getBarMaxIndex() - copyBuffer.getBarMinIndex() + 1;
                // First insert required bars
                if (targetBarIndex > lastBar)
                {
                    try
                    {
                        targetCls.setSize(targetBarIndex + range);
                    } catch (UnsupportedEditException ex)
                    {
                        // Should never happen when resizing bigger
                        String msg = "Impossible to resize.\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(undoText, msg);
                        return;
                    }
                } else
                {
                    targetCls.insertBars(targetBarIndex, range);
                }
            }
            // Then add the items
            for (ChordLeadSheetItem<?> item : copyBuffer.getItemsCopy(targetCls, targetBarIndex))
            {
                int barIndex = item.getPosition().getBar();
                // Items which arrive after end of leadsheet are skipped.
                if (barIndex < targetCls.getSize())
                {
                    if (item instanceof CLI_Section)
                    {
                        // We need a new copy to make sure the new section name is generated with
                        // the possible previous sections added to the chordleadsheet.
                        // Otherwise possible name clash if e.g. bridge1 and bridge2 in the buffer,
                        // bridge1->bridge3, bridge2->bridge3.
                        CLI_Section newSection = (CLI_Section) ((CLI_Section) item).getCopy(targetCls, null);
                        CLI_Section curSection = targetCls.getSection(barIndex);
                        try
                        {
                            if (curSection.getPosition().getBar() != barIndex)
                            {
                                // There is no section on target bar
                                targetCls.addSection(newSection);
                            } else
                            {
                                // There is a section on target bar, directly update existing section
                                targetCls.setSectionName(curSection, newSection.getData().getName());
                                targetCls.setSectionTimeSignature(curSection, newSection.getData().getTimeSignature());
                            }
                        } catch (UnsupportedEditException ex)
                        {
                            String msg = ERR_Paste() + ": " + newSection + ".\n" + ex.getLocalizedMessage();
                            um.handleUnsupportedEditException(undoText, msg);
                            return;
                        }
                    } else
                    {
                        targetCls.addItem(item);
                    }
                }
            }
        }
        um.endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        CopyBuffer copyBuffer = CopyBuffer.getInstance();

        if (copyBuffer.isEmpty() || selection.isEmpty())
        {
            setEnabled(false);
            return;
        }
        if (copyBuffer.isBarCopyMode())
        {
            setEnabled(selection.isBarSelected());
        } else
        {
            setEnabled(selection.isBarSelectedWithinCls());
        }
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }
}
