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
package org.jjazz.pianoroll.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;

/**
 * Invert notes selection.
 */
public class InvertNoteSelection extends AbstractAction
{
    public static final String ACTION_ID = "InvertNoteSelection";
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(InvertNoteSelection.class.getSimpleName());

    public InvertNoteSelection(PianoRollEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var selNotes = editor.getSelectedNoteEvents();
        var notes = new ArrayList<>(editor.getModel());
        notes.removeAll(selNotes);
        editor.unselectAll();
        editor.selectNotes(notes, true);        
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
