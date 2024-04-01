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
package org.jjazz.ss_editorimpl.actions;

import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ss_editorimpl.SongPartCopyBuffer;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;

/**
 * Manage paste of SongParts and RpValues.
 * <p>
 * Triggered by the SongPart menu entry and the CTRL-V keyboard shortcut. If RhythmParameters are selected reuse PasteRpValue methods.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.paste")
@ActionRegistration(displayName = "#CTL_Paste", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1200),
        })
public class Paste extends AbstractAction implements ContextAwareAction, SS_ContextActionListener, ChangeListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_Paste");

    public Paste()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Paste(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_V));
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        buffer.addChangeListener(this);
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Paste(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        if (selection.isSongPartSelected())
        {
            performSongPartPasteAction(selection);
        } else
        {
            PasteRpValue.performAction(selection);
        }
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isSongPartSelected())
        {
            b = isSongPartPasteEnabled(selection);
        } else if (selection.isRhythmParameterSelected())
        {
            b = PasteRpValue.isEnabled(selection);
        }
        setEnabled(b);
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc != null)
        {
            SS_SelectionUtilities selection = new SS_SelectionUtilities(tc.getEditor().getLookup());
            selectionChange(selection);
        }
    }

    // =======================================================================================
    // Private methods
    // =======================================================================================

    private boolean isSongPartPasteEnabled(SS_SelectionUtilities selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        boolean b = selection.isContiguousSptSelection() && !buffer.isEmpty();
        return b;
    }

    private void performSongPartPasteAction(SS_SelectionUtilities selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        SongStructure targetSgs = selection.getModel();
        List<SongPart> spts = targetSgs.getSongParts();
        // Paste before first selected spt
        int startBarIndex = spts.get(selection.getMinStartSptIndex()).getStartBarIndex();
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(targetSgs);
        um.startCEdit(undoText);
        for (SongPart spt : buffer.get(targetSgs, startBarIndex))
        {
            try
            {
                targetSgs.addSongParts(Arrays.asList(spt));
            } catch (UnsupportedEditException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_Paste");
                msg += "\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(undoText, msg);
                return;
            }
        }
        um.endCEdit(undoText);
    }
}
