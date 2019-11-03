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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.ui.sptviewer.api.SptViewer;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.jjazz.songstructure.api.SongPart;

public class MoveSelectionRight extends AbstractAction
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_Editor editor = SS_EditorTopComponent.getActive().getSS_Editor();
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof SptViewer)
        {
            SptViewer sptv = ((SptViewer) c);
            moveSelectionRightSpt(editor, sptv.getModel(), false);
        } else if (c instanceof RpViewer)
        {
            RpViewer rpv = ((RpViewer) c);
            moveSelectionRightRp(editor, rpv.getSptModel(), rpv.getRpModel(), false);
        }
    }

    /**
     * Move selection from a RhythmParameter in specified editor.
     *
     * @param ed
     * @param spt SongPart
     * @param rp RhythmParameter
     * @param extend If true extend the selection rather than move.
     */
    static public void moveSelectionRightRp(SS_Editor ed, SongPart spt, RhythmParameter<?> rp, boolean extend)
    {
        List<SongPart> spts = ed.getModel().getSongParts();
        int index = spts.indexOf(spt);
        assert index >= 0;
        int indexRp = spt.getRhythm().getRhythmParameters().indexOf(rp);
        assert indexRp >= 0;
        if (index < spts.size() - 1)
        {
            SongPart nextSpt = spts.get(index + 1);
            RhythmParameter<?> NextRp = RhythmParameter.Utilities.findFirstCompatibleRp(nextSpt.getRhythm().getRhythmParameters(), rp);
            if (NextRp == null)
            {
                // Use Rp at same index
                List<RhythmParameter<?>> rps = nextSpt.getRhythm().getRhythmParameters();
                int prevRpIndex = indexRp > rps.size() - 1 ? rps.size() - 1 : indexRp;
                NextRp = rps.get(prevRpIndex);
                //  but forbid selection extension since RP compatibility problem                
                extend = false;
            }
            if (!extend)
            {
                SS_SelectionUtilities selection = new SS_SelectionUtilities(ed.getLookup());
                selection.unselectAll(ed);
            }

            ed.selectRhythmParameter(nextSpt, NextRp, true);
            ed.setFocusOnRhythmParameter(nextSpt, NextRp);
        }
    }

    /**
     * Move selection from SongPart.
     *
     * @param ed
     * @param spt
     * @param extend If true extend the selection rather than move.
     */
    public static void moveSelectionRightSpt(SS_Editor ed, SongPart spt, boolean extend)
    {
        List<SongPart> spts = ed.getModel().getSongParts();
        int index = spts.indexOf(spt);
        assert index >= 0;
        if (index < spts.size() - 1)
        {
            SongPart nextSpt = spts.get(index + 1);
            if (!extend)
            {
                SS_SelectionUtilities selection = new SS_SelectionUtilities(ed.getLookup());
                selection.unselectAll(ed);
            }
            ed.selectSongPart(nextSpt, true);
            ed.setFocusOnSongPart(nextSpt);
        }
    }
}
