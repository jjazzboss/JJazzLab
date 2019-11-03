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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.rhythm.parameters.RhythmParameter;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
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
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;

@ActionID(category = "JJazz", id = "org.jjazz.ui.rl_editor.actions.resetrpvalue")
@ActionRegistration(displayName = "#CTL_ResetRpValue", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 600),
        })
@Messages("CTL_ResetRpValue=Reset to default value")
public final class ResetRpValue extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = CTL_ResetRpValue();
    private static final Logger LOGGER = Logger.getLogger(ResetRpValue.class.getSimpleName());

    public ResetRpValue()
    {
        this(Utilities.actionsGlobalContext());
    }

    public ResetRpValue(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_ResetRpValue());                          // For popupmenu 
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("Z"));      // For popupmenu
    }

    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        LOGGER.log(Level.FINE, "actionPerformed() sgs=" + sgs + " selection=" + selection);
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);

        if (selection.isRhythmParameterSelected())
        {
            for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
            {
                RhythmParameter rp = sptp.getRp();
                SongPart spt = sptp.getSpt();
                Object newValue = rp.getDefaultValue();
                sgs.setRhythmParameterValue(spt, rp, newValue);
            }
        } else
        {
            for (SongPart spt : selection.getSelectedSongParts())
            {
                for (RhythmParameter rp : spt.getRhythm().getRhythmParameters())
                {
                    Object newValue = rp.getDefaultValue();
                    sgs.setRhythmParameterValue(spt, rp, newValue);
                }
            }
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = !selection.isEmpty();
        setEnabled(b);
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new ResetRpValue(context);
    }
}
