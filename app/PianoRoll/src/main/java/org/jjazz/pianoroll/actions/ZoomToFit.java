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
package org.jjazz.pianoroll.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.uiutilities.api.Zoomable;


/**
 * Adjust zoom and scrollbars to try to make all notes visible.
 */
public class ZoomToFit extends AbstractAction
{

    public static final String ACTION_ID = "ZoomToFit";
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(ZoomToFit.class.getSimpleName());

    public ZoomToFit(PianoRollEditor editor)
    {
        this.editor = editor;

    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        var nvs = editor.getNoteViews();
        if (nvs.isEmpty())
        {
            return;
        }
        Zoomable zoomable = editor.getLookup().lookup(Zoomable.class);
        if (zoomable == null)
        {
            return;
        }
        zoomable.setZoomXFactorToFitContent();
        zoomable.setZoomYFactorToFitContent();
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}
