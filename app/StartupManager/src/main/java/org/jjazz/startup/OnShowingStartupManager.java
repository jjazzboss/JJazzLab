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
package org.jjazz.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.startup.spi.OnShowingTask;
import org.openide.util.Lookup;
import org.openide.windows.OnShowing;

/**
 * Execute OnShowing startup tasks based on ascending priority order.
 */
@OnShowing
public class OnShowingStartupManager implements Runnable
{

    private static final Logger LOGGER = Logger.getLogger(OnShowingStartupManager.class.getSimpleName());

    @Override
    public void run()
    {
        // Get all tasks sorted by priority
        List<? extends OnShowingTask> tasks = new ArrayList<>(Lookup.getDefault().lookupAll(OnShowingTask.class));
        Collections.sort(tasks, (t1, t2) -> Integer.compare(t1.getPriority(), t2.getPriority()));
        
        // we're on the EDT, we need a distinct thread
        assert SwingUtilities.isEventDispatchThread();
        new Thread(() -> runTasks(tasks)).start();
    }

    private void runTasks(Collection<? extends OnShowingTask> orderedTasks)
    {
        for (var task : orderedTasks)
        {
            LOGGER.log(Level.INFO, "Starting task {1} : {0}", new Object[]
            {
                task.getName(), task.getPriority()
            });
            task.run();
        }
    }
}
