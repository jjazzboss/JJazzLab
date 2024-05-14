/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.pianoroll;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.utilities.api.IntRange;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "AddLoopZone",
        id = "org.jjazz.pianoroll.AddLoopZone"
)
@ActionRegistration(
        displayName = "#CTL_AddLoopZone"
)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 870012),
            @ActionReference(path = "Shortcuts", name = "D-T")      // ctrl T
        })
@Messages("CTL_AddLoopZone=Add loop zone")
public final class AddLoopZone implements ActionListener
{
    
    private static final Logger LOGGER = Logger.getLogger(AddLoopZone.class.getSimpleName());
    
    @Override public void actionPerformed(ActionEvent e)
    {
        LOGGER.severe("AddLoopZone() --  ");
        var editor = PianoRollEditorTopComponent.getActive().getEditor();
        if (editor.getLoopZone() == null)
        {
            editor.showLoopZone(new IntRange(1, 3));
        } else
        {
            editor.showLoopZone(null);
        }
    }
}
