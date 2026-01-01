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
package org.jjazz.jjswing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Show the advanced jjSwing settings dialog.
 */
//@ActionID(category = "JJazz", id = "org.jjazz.jjswing.advancedsettingsaction")
//@ActionRegistration(displayName = "jjSwing Advanced Settings...")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 87012),
//            @ActionReference(path = "Shortcuts", name = "DS-T")      // ctrl-shift T
//        })
public final class AdvancedSettingsAction implements ActionListener
{
    private static AdvancedSettingsDialog dialog;
    private static final Logger LOGGER = Logger.getLogger(AdvancedSettingsAction.class.getSimpleName());

    public AdvancedSettingsAction()
    {
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        if (dialog==null)
        {
            dialog = new AdvancedSettingsDialog();
        }
        dialog.setVisible(true);
    }
}
