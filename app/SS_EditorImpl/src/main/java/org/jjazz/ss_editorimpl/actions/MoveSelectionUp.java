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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.ss_editor.rpviewer.api.RpViewer;
import org.jjazz.songstructure.api.SongPart;

public class MoveSelectionUp extends AbstractAction
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var activeTc = SS_EditorTopComponent.getActive();
        if (activeTc == null)
        {
            return;
        }
        SS_Editor editor = activeTc.getEditor();
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof RpViewer)
        {
            RpViewer rpv = ((RpViewer) c);
            SongPart spt = rpv.getSptModel();
            List<RhythmParameter<?>> rps = rpv.getSptModel().getRhythm().getRhythmParameters();
            int rpIndex = rps.indexOf(rpv.getRpModel());
            assert rpIndex >= 0;
            SS_Selection selection = new SS_Selection(editor.getLookup());
            selection.unselectAll(editor);
            if (rpIndex > 0)
            {
                // Go to RhythmParameter above
                editor.selectRhythmParameter(spt, rps.get(rpIndex - 1), true);
                editor.setFocusOnRhythmParameter(spt, rps.get(rpIndex - 1));
            } else
            {
                // Go to enclosing SongPart
                editor.selectSongPart(spt, true);
                editor.setFocusOnSongPart(spt);
            }
        }
    }
}
