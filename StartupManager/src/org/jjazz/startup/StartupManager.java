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
package org.jjazz.startup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import org.jjazz.startup.spi.StartupTask;
import org.openide.util.Lookup;
import org.openide.windows.OnShowing;

/**
 * Execute StartupTasks when UI is ready, one at a time, by ascending getPriority() order.
 * <p>
 * <p>
 */
public class StartupManager
{

    private static StartupManager INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(StartupManager.class.getSimpleName());

    static public StartupManager getInstance()
    {
        synchronized (StartupManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new StartupManager();
            }
        }
        return INSTANCE;
    }

    private StartupManager()
    {

    }

    @OnShowing
    public static class Launcher implements Runnable
    {

        @Override
        public void run()
        {
            // Get all tasks sorted by priority
            var res = new ArrayList<>(Lookup.getDefault().lookupAll(StartupTask.class));
            Collections.sort(res, (t1, t2) -> Integer.compare(t1.getPriority(), t2.getPriority()));
            
            
            for (var task : res)
            {
                LOGGER.info("Launcher.run() Starting task: " + task.getName() + " priority=" + task.getPriority());   
                task.run();
            }
        }

    }
}
