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
package org.jjazz.rhythmdatabaseimpl;

import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import org.jjazz.startup.spi.StartupTask;
import org.jjazz.uiutilities.api.PleaseWaitDialog;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.TaskListener;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = RhythmDatabaseFactory.class)
public class RhythmDatabaseFactoryImpl implements RhythmDatabaseFactory
{
    
    private static RhythmDatabaseImpl INSTANCE;
    
    public RhythmDatabaseFactoryImpl()
    {
        // INSTANCE should have been created first by CreateDatabaseTask
        if (INSTANCE == null)
        {            
            throw new IllegalStateException("INSTANCE is null");
        }
    }
    
    
    @Override
    public RhythmDatabase get()
    {
        var initTask = INSTANCE.getInitializationTask();
        if (initTask == null || initTask.isFinished())
        {
            return INSTANCE;
        }


        // Initialization task is not yet finished, show a "please wait" dialog until it's available.               
        PleaseWaitDialog dlg = new PleaseWaitDialog(ResUtil.getString(RhythmDatabaseFactoryImpl.class, "CTL_PleaseWait"));


        // Add listener before showing modal dialog. If initTask is finished now directly call the listener
        initTask.addTaskListener(task -> 
        {
            dlg.setVisible(false);
            dlg.dispose();
            initTask.removeTaskListener((TaskListener) this);
        });

        // Show dialog if really not finished (may happen just before this source code line)
        if (!initTask.isFinished())
        {
            dlg.setVisible(true);
        }
        
        return INSTANCE;
    }


    /**
     * Make sure to create the database instance after CopyDefaultRhythmFilesTask.
     */
    @ServiceProvider(service = StartupTask.class)
    public static class CreateDatabaseTask implements StartupTask
    {
        
        public static final int PRIORITY = RhythmDatabaseImpl.CopyDefaultRhythmFilesTask.PRIORITY + 1;
        
        @Override
        public boolean run()
        {
            INSTANCE = new RhythmDatabaseImpl(false);
            return true;
        }
        
        @Override
        public int getPriority()
        {
            return PRIORITY;
        }
        
        @Override
        public String getName()
        {
            return "Create Rhythm database";
        }
        
    }
    
}
