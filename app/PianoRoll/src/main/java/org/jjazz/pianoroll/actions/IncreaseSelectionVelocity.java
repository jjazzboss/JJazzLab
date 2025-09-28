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
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.utilities.api.ResUtil;

/**
 * Increase the velocity of the selected notes.
 */
public class IncreaseSelectionVelocity extends AbstractAction
{

    public static final int CHANGE = 5;
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(IncreaseSelectionVelocity.class.getSimpleName());

    public IncreaseSelectionVelocity(PianoRollEditor editor)
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
        Phrase model = editor.getModel();


        String undoText = ResUtil.getString(getClass(), "IncreaseSelectionVelocity");
        editor.getUndoManager().startCEdit(editor, undoText);


        Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
        for (var ne : nes)
        {
            int newVel = MidiConst.clamp(ne.getVelocity() + CHANGE);
            if (newVel != ne.getVelocity())
            {
                var newNe = ne.setVelocity(newVel, true);
                mapOldNew.put(ne, newNe);
            }
        }
        model.replaceAll(mapOldNew, false);


        editor.getUndoManager().endCEdit(undoText);
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
