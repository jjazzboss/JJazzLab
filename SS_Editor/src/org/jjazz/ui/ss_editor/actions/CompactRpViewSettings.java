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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.rhythm.api.Rhythm;
import org.openide.windows.WindowManager;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;


/**
 * Show the compact view settings dialog.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.compactrpviewsettings")
@ActionRegistration(displayName = "#CTL_CompactRpViewSettings", lazy = false)  // To have the tooltip
@ActionReferences(
        {
            @ActionReference(path = "Actions/SS_EditorToolBar", position = 110)
        })
public class CompactRpViewSettings extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(CompactRpViewSettings.class.getSimpleName());
    private static final ImageIcon ICON = new ImageIcon(CompactRpViewSettings.class.getResource("/org/jjazz/ui/ss_editor/actions/resources/CompactViewSettings.png"));

    public CompactRpViewSettings()
    {
        putValue("hideActionText", true);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_CompactRpViewSettings"));
        putValue(SMALL_ICON, ICON);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.log(Level.FINE, "actionPerformed()");   //NOI18N
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.getActive();
        if (ssTc == null)
        {
            return;
        }
        
        var editor = ssTc.getSS_Editor();
        var song = editor.getSongModel();
        if (song.getSongStructure().getSongParts().isEmpty())
        {           
            return;
        }

        
        CompactRpViewSettingsDialog dlg = CompactRpViewSettingsDialog.getInstance();
        dlg.setModel(editor);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        if (dlg.isExitOk())
        {
            var res = dlg.getResult();
            for (Rhythm r : res.keySet())
            {
                ToggleCompactView.setCompactViewModeVisibleRPs(song, r, res.get(r));
            }

            // Only first rhythm visible parameters
            Analytics.logEvent("Compact View Settings", Analytics.buildMap("Visible Rps", Analytics.toStrList(res.values().iterator().next())));

        }
    }


}
