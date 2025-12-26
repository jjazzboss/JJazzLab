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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * For debug purposes...
 */
@ActionID(category = "JJazz", id = "org.jjazz.test.dumpactivechordsheet")
@ActionRegistration(displayName = "DumpActiveChordSheet")
@ActionReferences(
    {
        @ActionReference(path = "Shortcuts", name = "DS-L")      // ctrl-shift L
    })
public final class DumpActiveChordSheet implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(DumpActiveChordSheet.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("actionPerformed()");
        var clTc = CL_EditorTopComponent.getActive();
        if (clTc != null)
        {
            var cls = clTc.getEditor().getModel();
            LOGGER.info("cls=");
            LOGGER.info(cls.toDebugString());
        }
    }
}
