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
package org.jjazz.cl_editorimpl.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.openide.windows.WindowManager;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;


/**
 * Show the CL_Editor settings dialog.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.showeditorsettings")
@ActionRegistration(displayName = "not_used", lazy = false)  // To have the tooltip
@ActionReferences(
        {
            @ActionReference(path = "Actions/CL_EditorToolBar", position = 100)
        })
public class ShowEditorSettings extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(ShowEditorSettings.class.getSimpleName());
    @StaticResource(relative=true)
    private static final String ICON_PATH = "resources/EditorSettings.png";
    private static final ImageIcon ICON = new ImageIcon(ShowEditorSettings.class.getResource(ICON_PATH));

    public ShowEditorSettings()
    {
        putValue("hideActionText", true);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "ShowSettingsTooltip"));
        putValue(SMALL_ICON, ICON);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.log(Level.FINE, "actionPerformed()");   
        CL_EditorTopComponent clTc = CL_EditorTopComponent.getActive();
        if (clTc == null)
        {
            return;
        }
        
        var editor = clTc.getEditor();
    
        
        var dlg = new EditorSettingsDialog(editor);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);

    }


}
