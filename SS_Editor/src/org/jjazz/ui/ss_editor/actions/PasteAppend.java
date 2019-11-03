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
package org.jjazz.ui.ss_editor.actions;

import org.jjazz.ui.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ui.ss_editor.api.CopyBuffer;
import static org.jjazz.ui.ss_editor.actions.Bundle.CTL_PasteAppend;
import static org.jjazz.ui.ss_editor.actions.Bundle.ERR_Paste;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;

@ActionID(category = "JJazz", id = "org.jjazz.ui.rl_editor.actions.pasteappend")
@ActionRegistration(displayName = "#CTL_PasteAppend", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RL_Editor", position = 900)
        })
@NbBundle.Messages("CTL_PasteAppend=Paste At The End")
public class PasteAppend extends AbstractAction implements ContextAwareAction, SS_ContextActionListener, ChangeListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = CTL_PasteAppend();

    public PasteAppend()
    {
        this(Utilities.actionsGlobalContext());
    }

    private PasteAppend(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_PasteAppend());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift V"));
        setEnabled(false);
        CopyBuffer buffer = CopyBuffer.getInstance();
        buffer.addChangeListener(this);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new PasteAppend(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Can not rely on selection to retrieve the model, selection can be empty !
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            return;
        }
        SongStructure targetSgs = tc.getSS_Editor().getModel();
        CopyBuffer buffer = CopyBuffer.getInstance();
        List<SongPart> spts = targetSgs.getSongParts();
        int startBarIndex = 0;
        if (!spts.isEmpty())
        {
            // Paste at the end
            SongPart lastSpt = spts.get(spts.size() - 1);
            startBarIndex = lastSpt.getStartBarIndex() + lastSpt.getNbBars();
        }

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(targetSgs);
        um.startCEdit(undoText);
        for (SongPart spt : buffer.get(targetSgs, startBarIndex))
        {
            try
            {
                targetSgs.addSongPart(spt);
            } catch (UnsupportedEditException ex)
            {
                String msg = ERR_Paste() + "\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(undoText, msg);
                return;
            }
        }
        JJazzUndoManagerFinder.getDefault().get(targetSgs).endCEdit(undoText);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        CopyBuffer buffer = CopyBuffer.getInstance();
        setEnabled(!buffer.isEmpty());
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc != null)
        {
            SS_SelectionUtilities selection = new SS_SelectionUtilities(tc.getSS_Editor().getLookup());
            selectionChange(selection);
        }
    }
}
