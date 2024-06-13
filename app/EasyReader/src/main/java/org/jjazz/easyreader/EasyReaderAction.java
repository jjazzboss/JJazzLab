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
package org.jjazz.easyreader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.analytics.api.Analytics;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Show the Easy Reader TopComponent.
 * <p>
 * Do not use @TopComponent.OpenActionRegistration because we want to log an event.
 */
@ActionID(
        category = "Window",
        id = "org.jjazz.easyreader.EasyReaderAction"
)
@ActionRegistration(displayName = "#CTL_EasyReader")
@ActionReference(path = "Menu/Tools", position = 22)
public final class EasyReaderAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(EasyReaderAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Analytics.logEvent("Easy Reader");

        TopComponent tc = WindowManager.getDefault().findTopComponent("EasyReaderTopComponent");
        assert tc != null;
        tc.open();
        tc.requestActive();
    }
}
