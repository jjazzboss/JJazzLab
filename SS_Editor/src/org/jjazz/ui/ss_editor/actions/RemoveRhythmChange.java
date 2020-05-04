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
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.AdaptedRhythm;
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
import org.openide.util.Exceptions;

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


        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);


        SongPart spt = selection.getSelectedSongParts().get(0);
        SongPart prevSpt = selection.getModel().getSongPart(spt.getStartBarIndex() - 1);
        Rhythm prevRhythm = prevSpt.getRhythm();
        SongPart newSpt = spt.clone(prevRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
        try
        {
            sgs.replaceSongParts(Arrays.asList(spt), Arrays.asList(newSpt));
        } catch (UnsupportedEditException ex)
        {
            // We possibly removed 1 rhythm, can't be here
            Exceptions.printStackTrace(ex);
        }


        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
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
                SongPart prevSpt = selection.getModel().getSongPart(spt.getStartBarIndex() - 1);
                Rhythm prevRhythm = prevSpt.getRhythm();
                b = r.getTimeSignature().equals(prevRhythm.getTimeSignature()) && r != prevRhythm;
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
