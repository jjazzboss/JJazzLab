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
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.startup.spi.OnStartTask;
import org.openide.modules.OnStart;
import org.openide.util.Lookup;

/**
 * Execute OnStart startup tasks based on ascending priority order.
 */
@OnStart
public class OnStartStartupManager implements Runnable
{

    private static final Logger LOGGER = Logger.getLogger(OnStartStartupManager.class.getSimpleName());

    @Override
    public void run()
    {
        // Get all tasks sorted by priority
        var res = new ArrayList<>(Lookup.getDefault().lookupAll(OnStartTask.class));
        Collections.sort(res, (t1, t2) -> Integer.compare(t1.getPriority(), t2.getPriority()));
        for (var task : res)
        {
            LOGGER.log(Level.INFO, "Starting task {1} : {0}", new Object[]
            {
                task.getName(), task.getPriority()
            });
            task.run();
        }
    }

}
