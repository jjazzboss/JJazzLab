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
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
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
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.rhythm.api.RpEnumerable;
import org.jjazz.ss_editor.api.SS_ContextAction;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.nextrpvalue")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 400),
        })
public final class NextRpValue extends SS_ContextAction
{
    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_UP);
    private static final Logger LOGGER = Logger.getLogger(NextRpValue.class.getSimpleName());

  @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_NextRpValue"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        SongStructure sgs = selection.getModel();
        assert sgs != null : "selection=" + selection;
        LOGGER.log(Level.FINE, "actionPerformed() sgs={0} selection={1}", new Object[]
        {
            sgs, selection
        });
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(getActionName());
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            RhythmParameter rp = sptp.getRp();
            if (rp instanceof RpEnumerable)
            {
                SongPart spt = sptp.getSpt();
                Object newValue = ((RpEnumerable) rp).getNextValue(spt.getRPValue(rp));
                sgs.setRhythmParameterValue(spt, rp, newValue);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(getActionName());
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = selection.isEnumerableRhythmParameterSelected();
        setEnabled(b);
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
    }
}
