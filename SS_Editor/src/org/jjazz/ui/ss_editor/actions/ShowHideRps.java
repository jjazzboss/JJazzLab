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

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.util.SmallMap;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

@ActionID(category = "JJazz", id = "org.jjazz.ui.rl_editor.actions.showhiderps")
@ActionRegistration(displayName = "#CTL_ShowHideRp", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 3000, separatorBefore = 2990),
            @ActionReference(path = "Actions/RhythmParameter", position = 3000, separatorBefore = 2990),
            @ActionReference(path = "Actions/RL_Editor", position = 3000, separatorBefore = 2990)
        })
@NbBundle.Messages("CTL_ShowHideRp=Show/Hide Parameters...")
public class ShowHideRps extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(ShowHideRps.class.getSimpleName());

    public ShowHideRps()
    {
        putValue(NAME, CTL_ShowHideRp());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            return;
        }
        LOGGER.log(Level.FINE, "actionPerformed()");
        SS_Editor editor = tc.getSS_Editor();
        SongStructure sgs = editor.getModel();
        SmallMap<Rhythm, List<RhythmParameter<?>>> map = new SmallMap<>();
        for (SongPart spt : sgs.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            map.putValue(r, editor.getVisibleRps(r));
        }
        ShowHideRpsDialog dlg = ShowHideRpsDialog.getInstance();
        dlg.setModel(map);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        if (dlg.isExitOk())
        {
            for (Rhythm r : map.getKeys())
            {
                editor.setVisibleRps(r, map.getValue(r));
            }
        }
    }
}
