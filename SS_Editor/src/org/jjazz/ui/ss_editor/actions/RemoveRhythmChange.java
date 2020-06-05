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
package org.jjazz.ui.ss_editor.actions;

import org.jjazz.ui.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.ss_editor.HideIfDisabledAction;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import org.jjazz.undomanager.JJazzUndoManager;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.removerhythmchange")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 90),
        })
@Messages("CTL_RemoveRhythmChange=Remove Rhythm Change")
public class RemoveRhythmChange extends AbstractAction implements ContextAwareAction, SS_ContextActionListener, HideIfDisabledAction
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = CTL_RemoveRhythmChange();
    private static final Logger LOGGER = Logger.getLogger(RemoveRhythmChange.class.getSimpleName());

    public RemoveRhythmChange()
    {
        this(Utilities.actionsGlobalContext());
    }

    public RemoveRhythmChange(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_RemoveRhythmChange());
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
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
            SongPart newSpt = spt.clone(prevRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
            newSpts.add(newSpt);
        }


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(undoText);
        try
        {
            sgs.replaceSongParts(oldSpts, newSpts);
        } catch (UnsupportedEditException ex)
        {
            // We possibly removed 1 rhythm, can't be here
            String msg = "Impossible to remove rhythm change.\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(undoText, msg);
            return;
        }
        um.endCEdit(undoText);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
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

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new RemoveRhythmChange(context);
    }
}
