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
import javax.swing.Action;
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
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;

/**
 * Manage pasting of SongParts and RpValues.
 * <p>
 */
public class Paste extends SS_ContextAction implements ChangeListener
{

    private static Paste INSTANCE;
    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_V);

    /**
     * We want a singleton because we need to listen to the system clipboard (and not obvious to find an event to unregister the listener).
     *
     * @return
     */
    @ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.paste")
    @ActionRegistration(displayName = "paste-not-used", lazy = false)
    @ActionReferences(
            {
                @ActionReference(path = "Actions/SongPart", position = 1200),
            })
    public static Paste getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new Paste();
        }
        return INSTANCE;
    }

    /**
     * Enforce singleton
     */
    private Paste()
    {
    }

    /**
     * Enforce singleton.
     *
     * @param lkp
     * @return
     */
    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return this;
    }

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getCommonString("CTL_Paste"));
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
        if (selection.isEmpty() || selection.isSongPartSelected())
        {
            performSongPartPasteAction(selection);
        } else if (selection.isRhythmParameterSelected())
        {
            PasteRpValue.performAction(selection);
        }
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = false;
        SongPartCopyBuffer sptBuffer = SongPartCopyBuffer.getInstance();
        if (selection.isEmpty())
        {
            b = !sptBuffer.isEmpty();
        } else if (selection.isSongPartSelected())
        {
            b = selection.isContiguousSptSelection() && !sptBuffer.isEmpty();
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

    private void performSongPartPasteAction(SS_Selection selection)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();   // Prefer this method because selection can be empty
        if (tc == null)
        {
            return;
        }

        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        SongStructure targetSgs = tc.getEditor().getModel();
        List<SongPart> targetSpts = targetSgs.getSongParts();

        // Paste before first selected SongPart, or after last one if no selection
        int startBarIndex = !selection.isEmpty() ? targetSpts.get(selection.getMinStartSptIndex()).getStartBarIndex() : targetSgs.getSizeInBars();

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
