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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.NotesSelectionListener;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.util.api.ResUtil;
import org.openide.util.HelpCtx;
import org.openide.util.actions.BooleanStateAction;

/**
 * Action to toggle the play of the last selected note.
 */
public class HearSelectedNotes extends BooleanStateAction
{

    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(HearSelectedNotes.class.getSimpleName());

    public HearSelectedNotes(PianoRollEditor editor)
    {
        this.editor = editor;

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/HearNoteOFF.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("resources/HearNoteON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));   //NOI18N                                
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HearNoteTooltip"));
        putValue("hideActionText", true);

        var nsl = NotesSelectionListener.getInstance(editor.getLookup());
        nsl.addListener((ChangeEvent evt) -> selectionChanged(nsl.getLastNoteViewAddedToSelection()));
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!getBooleanState());
    }

    public void setSelected(boolean b)
    {
        if (b == getBooleanState())
        {
            return;
        }
        setBooleanState(b);     // Notify action listeners

        if (b)
        {

        } else
        {

        }
    }

    @Override
    public String getName()
    {
        return "HearSelectedNotesName";
    }

    @Override
    public HelpCtx getHelpCtx()
    {
        return null;
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================

    private void selectionChanged(NoteView lastNoteViewAddedToSelection)
    {
        if (getBooleanState() == false || lastNoteViewAddedToSelection == null)
        {
            return;
        }

        hearNote(lastNoteViewAddedToSelection.getModel());

    }

    private void hearNote(NoteEvent ne)
    {
        LOGGER.log(Level.SEVERE, "hearNote() -- ne={0}", ne);
    }

}
