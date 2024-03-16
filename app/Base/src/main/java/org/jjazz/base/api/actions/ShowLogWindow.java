/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.base.api.actions;

import java.util.logging.Logger;
import javax.swing.Action;
import org.openide.awt.Actions;
import org.openide.windows.OnShowing;
import org.openide.windows.*;

/**
 * Show the output window connected to the log file.
 * <p>
 * Also on startup check if the output window was persisted, if yes, reconnect it to the log file.
 */
@OnShowing
public class ShowLogWindow implements Runnable
{

    private static final String ACTION_CAT = "View";
    private static final String ACTION_ID = "org.netbeans.core.actions.LogAction";
    private static final Logger LOGGER = Logger.getLogger(ShowLogWindow.class.getSimpleName());

    @Override
    public void run()
    {
        WindowManager wm = WindowManager.getDefault();
        TopComponent tc = wm.findTopComponent("output");
        if (tc != null && tc.isOpened())
        {
            actionPerformed();
        }
    }

    /**
     * Call the Netbeans IDE LogAction.
     */
    static public void actionPerformed()
    {
        Action a = Actions.forID(ACTION_CAT, ACTION_ID);   
        if (a == null)
        {
            LOGGER.warning("actionPerformed() Action not found cat=" + ACTION_CAT + " id=" + ACTION_ID);   
            return;
        }
        a.actionPerformed(null);
    }

}
