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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import org.jjazz.ui.ss_editor.CompactViewModeController;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * The action to switch between full/compact RP view mode.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.togglecompactview")
@ActionRegistration(displayName = "#CTL_ToggleCompactView", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/SongPart", position = 300)
            @ActionReference(path = "Shortcuts", name = "V")
        })
public class ToggleCompactView extends AbstractAction 
{

    private static final Logger LOGGER = Logger.getLogger(ToggleCompactView.class.getSimpleName());


    private ToggleCompactView()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_ToggleCompactView"));
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_ToggleCompactView"));
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("V"));     // Useful only if action is used to create a menu entry
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/ss_editor/actions/resources/CompactViewMode-OFF.png")));
        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/ss_editor/actions/resources/CompactViewMode-ON.png")));
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.getVisible();
        if (ssTc == null)
        {
            return;
        }

        // Just switch between compact and full mode
        var song = ssTc.getSS_Editor().getSongModel();
        boolean b = CompactViewModeController.isSongInCompactViewMode(song);
        CompactViewModeController.setSongInCompactViewMode(song, !b);

    }

}
