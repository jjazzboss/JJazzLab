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
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.util.api.ResUtil;

/**
 * Quantize notes action.
 */
public class Quantize extends AbstractAction
{

    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(Quantize.class.getSimpleName());

    public Quantize(PianoRollEditor editor)
    {
        this.editor = editor;


        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("Q"), "QuantizeNotes");   //NOI18N
        editor.getActionMap().put("QuantizeNotes", this);   //NOI18N
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
        var beatRange = editor.getBeatRange();


        String undoText = ResUtil.getString(getClass(), "QuantizeNotes");
        editor.getUndoManager().startCEdit(editor, undoText);

        for (var nv : nvs)
        {
            var ne = nv.getModel();
            var ts = editor.getTimeSignature(ne.getPositionInBeats());
            var pos = editor.toPosition(ne.getPositionInBeats());
            var newPos = Quantizer.getQuantized(editor.getQuantization(), pos, ts, qStrength, editor.getBarRange().to);
            float newPosInBeats = editor.toPositionInBeats(newPos);
            if (!beatRange.contains(newPosInBeats + ne.getDurationInBeats(), true))
            {
                float newDur = beatRange.to - newPosInBeats - 0.01f;
                var newNe = ne.getCopyDurPos(newDur, newPosInBeats);
                editor.getModel().replace(ne, newNe);
            } else
            {
                editor.getModel().move(ne, newPosInBeats);
            }

        }


        editor.getUndoManager().endCEdit(undoText);
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
