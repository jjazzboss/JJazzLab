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
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.utilities.api.ResUtil;

/**
 * Quantize notes action.
 */
public class Quantize extends AbstractAction
{

    public static final String ACTION_ID = "Quantize";
    public static final String KEYBOARD_SHORTCUT = "Q";
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(Quantize.class.getSimpleName());

    public Quantize(PianoRollEditor editor)
    {
        this.editor = editor;

        putValue(Action.NAME, ResUtil.getString(getClass(), "Quantize"));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "QuantizeTooltip") + " (" + KEYBOARD_SHORTCUT + ")");


        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT), Quantize.ACTION_ID);
        editor.getActionMap().put(Quantize.ACTION_ID, this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var nvs = editor.getSelectedNoteViews();
        if (nvs.isEmpty())
        {
            nvs = editor.getNoteViews();
        }
        if (nvs.isEmpty())
        {
            return;
        }


        float qStrength = Quantizer.getInstance().getQuantizeStrength();
        var beatRange = editor.getPhraseBeatRange();


        String undoText = ResUtil.getString(getClass(), "Quantize");
        editor.getUndoManager().startCEdit(editor, undoText);

        
        Map<NoteEvent, Float> mapNotePos = new HashMap<>();
        Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
        for (var nv : nvs)
        {
            var ne = nv.getModel();
            var ts = editor.getTimeSignature(ne.getPositionInBeats());
            var pos = editor.toPosition(ne.getPositionInBeats());
            var newPos = Quantizer.getQuantized(editor.getQuantization(), pos, ts, qStrength, editor.getPhraseBarRange().to);
            float newPosInBeats = editor.toPositionInBeats(newPos);
            if (!beatRange.contains(newPosInBeats + ne.getDurationInBeats(), true))
            {
                float newDur = beatRange.to - newPosInBeats - 0.01f;
                var newNe = ne.setAll(-1, newDur, -1, newPosInBeats, null, true);
                mapOldNew.put(ne, newNe);
            } else
            {
                mapNotePos.put(ne, newPosInBeats);
            }
        }
        editor.getModel().moveAll(mapNotePos, false);
        editor.getModel().replaceAll(mapOldNew, false);


        editor.getUndoManager().endCEdit(undoText);
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
