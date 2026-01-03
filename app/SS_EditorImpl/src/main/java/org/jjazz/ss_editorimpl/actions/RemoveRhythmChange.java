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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ss_editor.api.SS_ContextAction;
import org.jjazz.ss_editorimpl.HideIfDisabledAction;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.removerhythmchange")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 90),
        })
public class RemoveRhythmChange extends SS_ContextAction implements HideIfDisabledAction
{

    private static final Logger LOGGER = Logger.getLogger(RemoveRhythmChange.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_RemoveRhythmChange"));
        // putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        SongStructure sgs = selection.getModel();
        var spts = sgs.getSongParts();
        SongPart spt0 = selection.getSelectedSongParts().get(0);
        Rhythm r0 = spt0.getRhythm();
        SongPart prevSpt = sgs.getSongPart(spt0.getStartBarIndex() - 1);
        Rhythm prevRhythm = prevSpt.getRhythm();


        // Prepare the song parts
        var newSpts = new ArrayList<SongPart>();
        var oldSpts = new ArrayList<SongPart>();
        int index = spts.indexOf(spt0);
        for (int i = index; i < spts.size(); i++)
        {
            SongPart spt = spts.get(i);
            if (spt.getRhythm() != r0)
            {
                // Exit at first different spt
                break;
            }
            oldSpts.add(spt);
            SongPart newSpt = spt.getCopy(prevRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
            newSpts.add(newSpt);
        }


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(getActionName());
        try
        {
            sgs.replaceSongParts(oldSpts, newSpts);
        } catch (UnsupportedEditException ex)
        {
            // We possibly removed 1 rhythm, can't be here
            String msg = ResUtil.getString(getClass(), "ERR_CantRemoveRhythmChange");
            msg += "\n" + ex.getLocalizedMessage();
            um.abortCEdit(getActionName(), msg);
            return;
        }
        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = false;
        if (selection.getSelectedSongParts().size() == 1)
        {
            SongPart spt = selection.getSelectedSongParts().get(0);
            Rhythm r = spt.getRhythm();
            if (spt.getStartBarIndex() > 0)
            {
                SongPart prevSpt = selection.getModel().getSongPart(spt.getStartBarIndex() - 1);    // Can be null when removing last song parts
                if (prevSpt != null)
                {
                    Rhythm prevRhythm = prevSpt.getRhythm();
                    b = r.getTimeSignature().equals(prevRhythm.getTimeSignature()) && r != prevRhythm;
                }
            }
        }
        // LOGGER.fine("selectionChange() b=" + b);
        setEnabled(b);
    }

}
