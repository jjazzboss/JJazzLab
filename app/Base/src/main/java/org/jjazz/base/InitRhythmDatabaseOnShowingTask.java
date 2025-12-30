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

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import org.jjazz.startup.spi.OnShowingTask;
import org.jjazz.upgrade.api.UpgradeManager;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 * Initialize the RhythmDatabase and set default rhythms on first run.
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
        var future = RhythmDatabaseFactory.getDefault().initialize();

        try
        {
            if (future.get() == null)      // Blocks until rdb initialization is complete
            {
                if (UpgradeManager.getInstance().isFreshStart())
                {
                    setDefaultRhythms();
                }

                var rdb = RhythmDatabase.getDefault();
                LOGGER.log(Level.INFO, "run() Default 4/4 rhythm: {0}", rdb.getDefaultRhythm(TimeSignature.FOUR_FOUR).name());
                LOGGER.log(Level.INFO, "run() Default 3/4 rhythm: {0}", rdb.getDefaultRhythm(TimeSignature.THREE_FOUR).name());
            }
        } catch (InterruptedException | ExecutionException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    private void setDefaultRhythms()
    {
        LOGGER.log(Level.INFO, "setDefaultRhythms() Setting default 4/4 and 3/4 rhythms upon fresh start");

        var rdb = RhythmDatabase.getDefault();
        String rId = "jjSwing-ID";
        var ri44 = rdb.getRhythm(rId);
        if (ri44 != null)
        {
            rdb.setDefaultRhythm(TimeSignature.FOUR_FOUR, ri44);

        } else
        {
            LOGGER.log(Level.WARNING, "run() Could not find 4/4 default rhythm rId={0}", rId);
        }


        rId = "JazzWaltzMed.S351.sst-ID";

        var ri34 = rdb.getRhythm(rId);
        if (ri34 != null)
        {
            rdb.setDefaultRhythm(TimeSignature.THREE_FOUR, ri34);
        } else
        {
            LOGGER.log(Level.WARNING, "run() Could not find 3/4 default rhythm rId={0}", rId);
        }
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
