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
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import static org.jjazz.ss_editor.api.SS_ContextAction.LISTENING_TARGETS;
import org.jjazz.ss_editor.api.SS_ContextAction.ListeningTarget;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.resetrpvalue")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 600),
        })
public final class ResetRpValue extends SS_ContextAction
{
    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("Z");
    private static final Logger LOGGER = Logger.getLogger(ResetRpValue.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_ResetRpValue"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.RHYTHM_PARAMETER_SELECTION, ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        SongStructure sgs = selection.getModel();
        LOGGER.log(Level.FINE, "actionPerformed() sgs={0} selection={1}", new Object[]
        {
            sgs, selection
        });
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(getActionName());

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
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(getActionName());
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = !selection.isEmpty();
        setEnabled(b);
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
    }
}
