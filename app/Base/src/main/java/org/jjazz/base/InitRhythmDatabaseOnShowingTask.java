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
package org.jjazz.base;

import java.util.logging.Logger;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import org.jjazz.startup.spi.OnShowingTask;
import org.openide.util.lookup.ServiceProvider;

/**
 * Initialize the RhythmDatabase.
 * <p>
 */
@ServiceProvider(service = OnShowingTask.class)
public class InitRhythmDatabaseOnShowingTask implements OnShowingTask
{

    /**
     * Must be after MidiSynths initialization.
     */
    public static final int ON_SHOWING_TASK_PRIORITY = 200;
    private static final Logger LOGGER = Logger.getLogger(InitRhythmDatabaseOnShowingTask.class.getSimpleName());

    @Override
    public void run()
    {
        RhythmDatabaseFactory.getDefault().initialize();
    }

    @Override
    public int getPriority()
    {
        return ON_SHOWING_TASK_PRIORITY;
    }

    @Override
    public String getName()
    {
        return "Initialize rhythm database";
    }


}
