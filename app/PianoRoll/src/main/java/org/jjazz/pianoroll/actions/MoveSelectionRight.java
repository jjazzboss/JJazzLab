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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;

/**
 * Move the selected notes right.
 */
public class MoveSelectionRight extends AbstractAction
{

    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(MoveSelectionRight.class.getSimpleName());

    public MoveSelectionRight(PianoRollEditor editor)
    {
        this.editor = editor;
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        var nes = editor.getSelectedNoteEvents();
        if (nes.isEmpty())
        {
            return;
        }


        float qDur = editor.getQuantization().getSymbolicDuration().getDuration();
        Phrase model = editor.getModel();
        FloatRange br = editor.getPhraseBeatRange();


        String undoText = ResUtil.getString(getClass(), "MoveNoteRight");
        editor.getUndoManager().startCEdit(editor, undoText);

        Map<NoteEvent, Float> mapNoteNewPos = new HashMap<>();
        for (var ne : nes)
        {
            float newPos = ne.getPositionInBeats() + qDur;
            if (br.contains(newPos + ne.getDurationInBeats(), false))
            {
                mapNoteNewPos.put(ne, newPos);
            }
        }
        model.moveAll(mapNoteNewPos, false);

        editor.getUndoManager().endCEdit(undoText);
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
