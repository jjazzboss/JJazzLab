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
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.CopyNoteBuffer;
import org.jjazz.pianoroll.api.NotesSelectionListener;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.util.api.ResUtil;
import org.openide.*;
import org.openide.actions.PasteAction;
import org.openide.util.NbPreferences;

/**
 * Paste the selected notes.
 */
public class PasteNotes extends AbstractAction
{
    
    private static final String PREF_NOTIFY_USER = "NotifyUser";
    private final PianoRollEditor editor;
    private static final Preferences prefs = NbPreferences.forModule(PasteNotes.class);
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
        var cnb = CopyNoteBuffer.getInstance();
        if (cnb.isEmpty())
        {
            LOGGER.warning("actionPerformed() Should not be here, CopyNoteBuffer is empty.");
            return;
        }

        // Notify once the user about the paste target position        
        if (prefs.getBoolean(PREF_NOTIFY_USER, true))
        {
            String msg = ResUtil.getString(getClass(), "NotifyUserPasteMechanism");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            prefs.putBoolean(PREF_NOTIFY_USER, false);
        }


        // Compute target start position
        float targetStartPos;
        var selectedNotes = editor.getSelectedNoteViews();
        if (selectedNotes.isEmpty())
        {
            // Find the first visible round beat
            var beatRange = editor.getVisibleBeatRange();
            targetStartPos = beatRange.size() >= 1f ? (float) Math.ceil(beatRange.from) : beatRange.from;
        } else
        {
            // Rely on the first selected note
            targetStartPos = selectedNotes.get(0).getModel().getPositionInBeats();
            editor.unselectAll();
        }
        
        
        String undoText = ResUtil.getString(getClass(), "PasteNotes");
        editor.getUndoManager().startCEdit(editor, undoText);
        
        var nes = cnb.getNotesCopy(targetStartPos);
        for (var ne : nes)
        {
            editor.getModel().add(ne);
            var nv = editor.getNoteView(ne);
            nv.setSelected(true);
        }
        
        editor.getUndoManager().endCEdit(undoText);
        
        
        SwingUtilities.invokeLater(() -> editor.scrollToCenter(nes.get(0).getPitch()));
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
