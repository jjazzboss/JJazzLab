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
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;

/**
 * SelectAll
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.selectall")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1300, separatorBefore = 1290),
            @ActionReference(path = "Actions/RhythmParameter", position = 1300, separatorBefore = 1290),
        })
public class SelectAll extends SS_ContextAction
{
    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_A);
    private static final Logger LOGGER = Logger.getLogger(SelectAll.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SelectAll"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.RHYTHM_PARAMETER_SELECTION, ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            return;
        }
        SS_Editor editor = tc.getEditor();
        SongStructure sgs = editor.getModel();
        if (selection.isEmpty() || selection.isSongPartSelected())
        {
            // Select all SongParts
            for (SongPart spt : sgs.getSongParts())
            {
                editor.selectSongPart(spt, true);
            }
        } else
        {
            // Select all compatible RPs
            RhythmParameter<?> rp = selection.getSelectedSongPartParameters().get(0).getRp();
            for (SongPart spt : sgs.getSongParts())
            {
                RhythmParameter<?> crp = RhythmParameter.findFirstCompatibleRp(spt.getRhythm().getRhythmParameters(), rp);
                if (crp != null)
                {
                    editor.selectRhythmParameter(spt, crp, true);
                }
            }
        }
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        // Can not rely on selection to retrieve the model, selection can be empty !
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            setEnabled(false);
            return;
        }
        SongStructure sgs = tc.getEditor().getModel();
        setEnabled(sgs != null && !sgs.getSongParts().isEmpty());
    }

}
