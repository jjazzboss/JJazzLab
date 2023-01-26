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
package org.jjazz.test;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.ui.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.modules.OnStart;
import org.openide.util.NbPreferences;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.mytestaction")
//@ActionRegistration(displayName = "MyTestAction")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 870012),
//            @ActionReference(path = "Shortcuts", name = "D-T")
//        })
public final class MyTestAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(MyTestAction.class.getSimpleName());

    public MyTestAction()
    {
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.severe("actionPerformed()");

        var tc = MixConsoleTopComponent.getInstance();
        if (tc != null)
        {
            LOGGER.severe(Utilities.toMultilineString(Arrays.asList(tc.getActions())));
            LOGGER.severe("-- ActionMap keys=");
            LOGGER.severe(Utilities.toMultilineString(tc.getActionMap().allKeys()));
        }
    }
}
