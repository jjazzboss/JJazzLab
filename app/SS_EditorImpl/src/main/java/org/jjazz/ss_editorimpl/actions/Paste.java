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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ss_editorimpl.SongPartCopyBuffer;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.PasteAction;
import org.openide.util.actions.SystemAction;

/**
 * Manage paste of SongParts and RpValues.
 * <p>
 * Triggered by the SongPart menu entry and the CTRL-V keyboard shortcut. If RhythmParameters are selected reuse PasteRpValue methods.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.paste")
@ActionRegistration(displayName = "paste-not-used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1200),
        })
public class Paste extends SS_ContextAction implements ChangeListener
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_V);


    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getCommonString("CTL_Copy"));
        Icon icon = SystemAction.get(PasteAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.RHYTHM_PARAMETER_SELECTION, ListeningTarget.SONG_PART_SELECTION));
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        buffer.addChangeListener(this);
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        if (selection.isSongPartSelected())
        {
            performSongPartPasteAction(selection);
        } else
        {
            PasteRpValue.performAction(selection);
        }
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = false;
        if (selection.isSongPartSelected())
        {
            b = isSongPartPasteEnabled(selection);
        } else if (selection.isRhythmParameterSelected())
        {
            b = PasteRpValue.isEnabled(selection);
        }
        setEnabled(b);
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc != null)
        {
            SS_Selection selection = new SS_Selection(tc.getEditor().getLookup());
            selectionChange(selection);
        }
    }

    // =======================================================================================
    // Private methods
    // =======================================================================================

    private boolean isSongPartPasteEnabled(SS_Selection selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        boolean b = selection.isContiguousSptSelection() && !buffer.isEmpty();
        return b;
    }

    private void performSongPartPasteAction(SS_Selection selection)
    {
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        SongStructure targetSgs = selection.getModel();
        List<SongPart> spts = targetSgs.getSongParts();

        // Paste before first selected spt
        int startBarIndex = spts.get(selection.getMinStartSptIndex()).getStartBarIndex();
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(targetSgs);
        um.startCEdit(getActionName());
        for (SongPart spt : buffer.get(targetSgs, startBarIndex))
        {
            try
            {
                targetSgs.addSongParts(Arrays.asList(spt));
            } catch (UnsupportedEditException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_Paste");
                msg += "\n" + ex.getLocalizedMessage();
                um.abortCEdit(getActionName(), msg);
                return;
            }
        }
        um.endCEdit(getActionName());
    }
}
