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
package org.jjazz.jjswing.bass.explorer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.wbpdatabaseexploreraction")
//@ActionRegistration(displayName = "WbpDatabase Explorer")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 60000)
//        })
public final class WbpDatabaseExplorerAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(WbpDatabaseExplorerAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        SwingUtilities.invokeLater(() -> 
        {
            var dlg = new WbpDatabaseExplorerDialog(false);
            dlg.setVisible(true);
        });

    }


}
