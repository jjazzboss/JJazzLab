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
package org.jjazz.pianoroll.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.pianoroll.api.CopyNoteBuffer;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.NotesSelectionListener;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.util.api.ResUtil;

/**
 * CopyNotes the selected notes.
 */
public class CutNotes extends AbstractAction
{

    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(CutNotes.class.getSimpleName());

    public CutNotes(PianoRollEditor editor)
    {
        this.editor = editor;

        // We need to enable/disable as required because action is a callback from the JJazzLab-level Delete action (key + menu entry)
        var nsl = NotesSelectionListener.getInstance(editor);
        nsl.addListener(e -> setEnabled(!nsl.isEmpty()));
        
        setEnabled(false);
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.fine("actionPerformed() --");
        
        var nvs = editor.getSelectedNoteViews();
        if (nvs.isEmpty())
        {
            return;
        }
        CopyNoteBuffer.getInstance().copy(NoteView.getNotes(nvs));


        String undoText = ResUtil.getString(getClass(), "CutNotes");
        editor.getUndoManager().startCEdit(editor, undoText);

        nvs.forEach(nv -> editor.getModel().remove(nv.getModel()));

        editor.getUndoManager().endCEdit(undoText);

    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
