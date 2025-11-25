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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.CopyNoteBuffer;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.utilities.api.ResUtil;
import org.openide.*;

/**
 * Paste the selected notes.
 */
public class PasteNotes extends AbstractAction
{

    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(PasteNotes.class.getSimpleName());

    public PasteNotes(PianoRollEditor editor)
    {
        this.editor = editor;

        CopyNoteBuffer.getInstance().addPropertyChangeListener(e -> 
        {
            if (e.getPropertyName().equals(CopyNoteBuffer.PROP_EMPTY))
            {
                setEnabled(!CopyNoteBuffer.getInstance().isEmpty());
            }
        });
        setEnabled(!CopyNoteBuffer.getInstance().isEmpty());
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        var copyBuffer = CopyNoteBuffer.getInstance();
        if (copyBuffer.isEmpty())
        {
            LOGGER.warning("actionPerformed() Should not be here, CopyNoteBuffer is empty.");
            return;
        }
        
        // Compute target start position
        float targetStartPos = -1;
        Point point = MouseInfo.getPointerInfo().getLocation();
        if (point != null)
        {
            SwingUtilities.convertPointFromScreen(point, editor);
            targetStartPos = editor.getPositionFromPoint(editor.toNotesPanelPoint(point));
        }       
        if (targetStartPos == -1)
        {
            return;
        }
        if (editor.isSnapEnabled())
        {
            targetStartPos = Quantizer.getQuantized(editor.getQuantization(), targetStartPos);
        }


        String undoText = ResUtil.getString(getClass(), "PasteNotes");
        editor.getUndoManager().startCEdit(editor, undoText);

        var pbr = editor.getPhraseBeatRange();
        var nes = copyBuffer.getNotesCopy(targetStartPos);
        List<NoteEvent> addedNotes = new ArrayList<>();
        Phrase p = editor.getModel();

        for (var ne : nes)
        {
            var nebr = ne.getBeatRange();
            if (!pbr.contains(nebr, true))
            {
                if (pbr.contains(nebr.from, true) && pbr.to > (nebr.from + 0.1f))
                {
                    var newDur = pbr.to - nebr.from - 0.01f;     // Shorten note
                    ne = ne.setDuration(newDur, true);
                } else
                {
                    ne = null;
                }
            }
            if (ne != null)
            {
                p.add(ne);
                addedNotes.add(ne);
            }
        }
        editor.getUndoManager().endCEdit(undoText);

        editor.unselectAll();
        editor.selectNotes(addedNotes, true);
        if (!addedNotes.isEmpty())
        {
            SwingUtilities.invokeLater(() -> editor.scrollToCenter(addedNotes.get(0).getPitch()));
        }
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
