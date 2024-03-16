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
package org.jjazz.base.api.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * Reset the Explorer and Output windows.
 * <p>
 * We can't use the standard Netbeans ResetWindows action, it does not work well with JJazzLab TopComponents.
 * <p>
 * Note that using Branding the "Minimize group" buttons can be disabled for all Modes (disable "Sliding Window Group"), but then
 * the UI is ugly... So I prefer to keep the minimize feature which can be nice for Explorer windows (less usefull for output
 * mode), and this reset action will help confused/beginner users to restore things as usual if needed.
 */
@ActionID(
        category = "Window",
        id = "org.jjazz.base.actions.ResetWindows"
)
@ActionRegistration(
        displayName = "#CTL_ResetWindows"
)
@ActionReference(path = "Menu/Window", position = 200)
public final class ResetWindows implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(ResetWindows.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        reset();
    }

    static public void reset()
    {
        WindowManager wm = WindowManager.getDefault();

        for (TopComponent tc : TopComponent.getRegistry().getOpened())
        {
            wm.setTopComponentFloating(tc, false);              // Floated TopComponents
        }
        Mode mode = wm.findMode("leftSlidingSide");           // MixConsole and SongPart editors when minimized
        if (mode != null)
        {
            for (TopComponent tc : mode.getTopComponents())
            {
                wm.setTopComponentMinimized(tc, false);
            }
        }
        mode = WindowManager.getDefault().findMode("bottomSlidingSide");  // Song structures editors when minimized
        if (mode != null)
        {
            for (TopComponent tc : mode.getTopComponents())
            {
                wm.setTopComponentMinimized(tc, false);
            }
        }
    }
}
