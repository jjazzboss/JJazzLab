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
import org.jjazz.harmony.api.Note;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.ui.utilities.api.Utilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.transposedown")
@ActionRegistration(displayName = "#CTL_TransposeDown", lazy = false)   // lazy is false to show the accelerator key in the menu
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 410),
        })
public final class TransposeDown extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_TransposeDown");
    private Lookup context;
    private CL_ContextActionSupport cap;

    public TransposeDown()
    {
        this(Utilities.actionsGlobalContext());
    }

    public TransposeDown(Lookup context)
    {
        this.context = context;

        // Help class to get notified of selection change in the current leadsheet editor
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);


        // As lazy=false above, need to set action properties to have the correct display in the menu
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_DOWN));


        // Update enabled state
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Get current selection state
        CL_SelectionUtilities selection = cap.getSelection();


        // Prepare the undoable action to receive the individual undoable edits
        ChordLeadSheet cls = selection.getChordLeadSheet();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);


        // Transpose down use FLAT notes by default (transpose up will use SHARP notes)
        for (CLI_ChordSymbol cliCs : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = cliCs.getData();
            ExtChordSymbol newEcs = ecs.getTransposedChordSymbol(-1, Note.Alteration.FLAT);
            cliCs.getContainer().changeItem(cliCs, newEcs);     // will generate undoable edits
        }


        // End the undoable action
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        setEnabled(selection.isItemSelected() && (selection.getSelectedItems().get(0) instanceof CLI_ChordSymbol));
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new TransposeDown(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing: transpose is not impacted by resize 
    }
}
