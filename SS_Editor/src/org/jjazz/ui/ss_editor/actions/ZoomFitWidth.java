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
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * This the delegate action for the ZoomFitWidth action of the ZoomableSliders module.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.zoomfitwidth")
@ActionRegistration(displayName = "CTL_ZoomFitWidth()", lazy = true)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/SongPart", position = 100)
            // @ActionReference(path = "Shortcuts", name = "C-F")
        })
@Messages("CTL_ZoomFitWidth=Zoom to Fit Width")
public class ZoomFitWidth extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(ZoomFitWidth.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.fine("actionPerformed()");
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.getActive();
        if (ssTc != null)
        {
            ssTc.getSS_Editor().setZoomHFactorToFitWidth(ssTc.getWidth());
        }
    }

}
