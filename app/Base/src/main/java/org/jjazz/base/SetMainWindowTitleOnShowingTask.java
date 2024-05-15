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
package org.jjazz.base;

import java.util.logging.Logger;
import javax.swing.JFrame;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

/**
 * Set the main window title.
 * <p>
 * Need to be done once the UI is ready.
 */
@OnShowing
public class SetMainWindowTitleOnShowingTask implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger(SetMainWindowTitleOnShowingTask.class.getSimpleName());

    @Override
    public void run()
    {
        JFrame mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
        String version = System.getProperty("jjazzlab.version");
        if (version == null)
        {
            LOGGER.warning("SetMainWindowTitle.run() The jjazzlab.version system property is not set.");
            version = "";
        } else
        {
            version = " " + version;
        }
        mainFrame.setTitle("JJazzLab " + version);
    }
}
