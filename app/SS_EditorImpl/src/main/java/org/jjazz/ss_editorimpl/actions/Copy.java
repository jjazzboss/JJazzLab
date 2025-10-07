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
import java.util.List;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import static org.jjazz.ss_editor.api.SS_ContextAction.LISTENING_TARGETS;
import org.jjazz.ss_editor.api.SS_ContextAction.ListeningTarget;
import org.jjazz.ss_editorimpl.SongPartCopyBuffer;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.CopyAction;
import org.openide.util.actions.SystemAction;


/**
 * Manage copy of SongParts and RpValues.
 * <p>
 * Triggered by the SongPart menu entry and the CTRL-C keyboard shortcut. If RhythmParameters are selected reuse CopyRpValue methods.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.copy")
@ActionRegistration(displayName = "copy-not-used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1100),     // CopyRpValue action will also insert its own entry in Actions/RhythmParameter
        })
public class Copy extends SS_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_C);


    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getCommonString("CTL_Copy"));
        Icon icon = SystemAction.get(CopyAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.RHYTHM_PARAMETER_SELECTION, ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        if (selection.isSongPartSelected())
        {
            performSongPartCopyAction(selection);
        } else
        {
            CopyRpValue.performAction(selection);
        }
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = false;
        if (selection.isSongPartSelected())
        {
            b = isSongPartCopyEnabled(selection);
        } else if (selection.isRhythmParameterSelected())
        {
            b = CopyRpValue.isEnabled(selection);
        }
        setEnabled(b);
    }

    // =======================================================================================
    // Private methods
    // =======================================================================================

    private boolean isSongPartCopyEnabled(SS_Selection selection)
    {
        return selection.isSongPartSelected() && selection.isContiguousSptSelection();
    }

    private void performSongPartCopyAction(SS_Selection selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        List<SongPart> spts = selection.getSelectedSongParts();
        buffer.put(spts);


        // Force a selection change so that the Paste action enabled status can be updated 
        // (otherwise Paste action will not see that SongPartCopyBuffer is no more empty)
        SS_Editor editor = SS_EditorTopComponent.getActive().getEditor();
        editor.selectSongPart(spts.get(0), false);
        editor.selectSongPart(spts.get(0), true);
    }

}
