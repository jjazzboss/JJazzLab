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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ss_editorimpl.SongPartCopyBuffer;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import static org.jjazz.ss_editor.api.SS_ContextAction.LISTENING_TARGETS;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.pasteappend")
@ActionRegistration(displayName = "note_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SS_Editor", position = 900)
        })
public class PasteAppend extends SS_ContextAction implements ChangeListener
{

    public static final KeyStroke KEYSTROKE
            = KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(),"CTL_PasteAppend"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(SS_ContextAction.ListeningTarget.SONG_PART_SELECTION));
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        buffer.addChangeListener(this);
        setEnabled(false);
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        // Can not rely on selection to retrieve the model, selection can be empty !
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            return;
        }
        SongStructure targetSgs = tc.getEditor().getModel();
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        List<SongPart> spts = targetSgs.getSongParts();
        int startBarIndex = 0;
        if (!spts.isEmpty())
        {
            // Paste at the end
            SongPart lastSpt = spts.get(spts.size() - 1);
            startBarIndex = lastSpt.getStartBarIndex() + lastSpt.getNbBars();
        }

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(targetSgs);
        um.startCEdit(getActionName());
        for (SongPart spt : buffer.get(targetSgs, startBarIndex))
        {
            try
            {
                targetSgs.addSongParts(Arrays.asList(spt));
            } catch (UnsupportedEditException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_Paste");
                msg += "\n" + ex.getLocalizedMessage();
                um.abortCEdit(getActionName(), msg);
                return;
            }
        }
        JJazzUndoManagerFinder.getDefault().get(targetSgs).endCEdit(getActionName());
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        setEnabled(!buffer.isEmpty());
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc != null)
        {
            SS_Selection selection = new SS_Selection(tc.getEditor().getLookup());
            selectionChange(selection);
        }
    }
}
