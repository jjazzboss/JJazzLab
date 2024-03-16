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
package org.jjazz.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;

/**
 * This is a copy of the SetActive action to be used only in the CL_EditorTopComponent pane popup menu.
 * <p>
 * Fix Issue #311 Activate via song pane popup-menu does not work if song is not selected first.
 * <p>
 * If multiple songs, righ-click popup menu action can be used on any song, even the non-selected/visible one! If popup menu song
 * is not the active one, the SetActive.java song lookup listener is not updated and SetActive is applied on the wrong song (the
 * still selected one).<br>
 * The problem does not occur when using the Netbeans context-sensitive actions which use a Song as a constructor argument (like
 * for DuplicateSong action). So we use this mechanism only for the popup-menu action, action which FIRST selects the Song before calling
 * the original SetActive action.
 * <p>
 * The Netbeans context-sensitive action mechanism seems to work OK only 80% of the time!!! (would need to investigate on NB app). Anyway better than nothing.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.setactivepopupaction")
@ActionRegistration(displayName = "#CTL_SetActivePopupAction", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/CL_EditorTopComponent", position = 100),
        })
public class SetActivePopupAction implements ActionListener
{

    final private Song song;
    private static final Logger LOGGER = Logger.getLogger(SetActivePopupAction.class.getSimpleName());

    public SetActivePopupAction(Song sg)
    {
        song = sg;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Make sure to activate the popupmenu song BEFORE running the original action.
        var clTc = CL_EditorTopComponent.get(song.getChordLeadSheet());
        if (clTc == null)
        {
            LOGGER.warning("actionPerformed() Unexpected clTc=null!");
            return;
        }

        clTc.requestActive();
        Action a = Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.setactive");
        SwingUtilities.invokeLater(() -> a.actionPerformed(null));
    }
}
