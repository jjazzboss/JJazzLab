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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import javax.swing.ImageIcon;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * This is the delegate action for the ZoomFitWidth action of the ZoomableSliders module.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.zoomfitwidth")
@ActionRegistration(displayName = "not_used", lazy = false)  // To have the tooltip
@ActionReferences(
        {
            @ActionReference(path = "Actions/SS_EditorToolBar", position = 100, separatorAfter = 101)
        // @ActionReference(path = "Shortcuts", name = "D-F")
        })
public class ZoomFitWidth extends AbstractAction
{
    private static final Logger LOGGER = Logger.getLogger(ZoomFitWidth.class.getSimpleName());

    public ZoomFitWidth()
    {
        putValue("hideActionText", true);        
        putValue(NAME, ResUtil.getString(getClass(), "CTL_ZoomFitWidth"));
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("resources/ZoomToFit.png")));
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_F));    // For popupmenu display only
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "DESC_ZoomFitWidth"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.fine("actionPerformed()");   
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.getActive();
        if (ssTc != null)
        {
            ssTc.getEditor().setZoomHFactorToFitWidth(ssTc.getWidth());
        }
    }

}
