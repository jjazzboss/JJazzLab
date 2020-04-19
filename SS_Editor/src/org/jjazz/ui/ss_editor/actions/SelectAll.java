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
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.openide.util.*;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;

/**
 * SelectAll
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.selectall")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1300),
            @ActionReference(path = "Actions/RhythmParameter", position = 1300, separatorBefore = 1290),
        })
@Messages("CTL_SelectAll=Select all")
public class SelectAll extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(SelectAll.class.getSimpleName());

    public SelectAll()
    {
        this(Utilities.actionsGlobalContext());
    }

    private SelectAll(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_SelectAll());
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_A));
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            return;
        }
        SS_Editor editor = tc.getSS_Editor();
        SongStructure sgs = editor.getModel();
        SS_SelectionUtilities selection = cap.getSelection(); // Warning: selection can be empty ! 
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
                RhythmParameter<?> crp = RhythmParameter.Utilities.findFirstCompatibleRp(spt.getRhythm().getRhythmParameters(), rp);
                if (crp != null)
                {
                    editor.selectRhythmParameter(spt, crp, true);
                }
            }
        }
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        // Can not rely on selection to retrieve the model, selection can be empty !
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            setEnabled(false);
            return;
        }
        SongStructure sgs = tc.getSS_Editor().getModel();
        setEnabled(sgs != null && !sgs.getSongParts().isEmpty());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new SelectAll(context);
    }
}
