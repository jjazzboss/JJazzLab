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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barrenderer.BR_Annotation;

public class ToggleBarAnnotationVisible extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(ToggleBarAnnotationVisible.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var clTc = CL_EditorTopComponent.getActive();
        if (clTc == null)
        {
            LOGGER.warning("actionPerformed() but no CL_EditorTopComponent active");
            return;
        }
        CL_Editor editor = clTc.getEditor();
        boolean b = BR_Annotation.isAnnotationBarRendererVisiblePropertyValue(editor.getSongModel());
        BR_Annotation.setAnnotationBarRendererVisiblePropertyValue(editor.getSongModel(), !b);
    }
}
