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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ss_editorimpl.SongPartCopyBuffer;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ss_editor.api.SS_ContextAction;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.cut")
@ActionRegistration(displayName = "cut-not-used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1000, separatorBefore = 900),
        })
public class Cut extends SS_ContextAction
{
    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_X);

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getCommonString("CTL_Cut"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.RHYTHM_PARAMETER_SELECTION, ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        buffer.put(selection.getSelectedSongParts());
        SongStructure sgs = selection.getModel();
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(getActionName());
        try
        {
            sgs.removeSongParts(selection.getSelectedSongParts());
        } catch (UnsupportedEditException ex)
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantCut");
            msg += "\n" + ex.getLocalizedMessage();
            um.abortCEdit(getActionName(), msg);
            return;
        }
        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        setEnabled(selection.isSongPartSelected() && selection.isContiguousSptSelection());
    }
}
