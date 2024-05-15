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
package org.jjazz.startup.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.startup.spi.OnShowingTask;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * Helper methods to notify user of early startup messages via informational modal dialogs, when UI is ready.
 * <p>
 * {@literal @}OnStart or OnStartTask should not directly use Netbeans modal NotifyDialogs because on Linux the dialog might be hidden behind the splash screen
 * and unattainable! Instead use the methods provided here.
 *
 * @see org.jjazz.startup.spi.OnStartTask
 */
public class OnStartMessageNotifier
{

    // Show these messages first since they should have been shown with @OnStart
    public static final int ON_SHOWING_TASK_PRIORITY = 0;
    private static final List<String> infoMessages = new ArrayList<>();
    private static final List<String> errorMessages = new ArrayList<>();

    /**
     * Display an info message when UI is ready.
     *
     * @param msg
     */
    static public void postInfoMessage(String msg)
    {
        Objects.requireNonNull(msg);
        infoMessages.add(msg);
    }

    /**
     * Display an error message when UI is ready.
     *
     * @param msg
     */
    static public void postErrorMessage(String msg)
    {
        Objects.requireNonNull(msg);
        errorMessages.add(msg);
    }

    // ======================================================================================
    // OnShowingTask interface
    // ======================================================================================    
    @ServiceProvider(service = OnShowingTask.class)
    static public class ShowMessagesTask implements OnShowingTask
    {

        @Override
        public void run()
        {
            for (var msg : errorMessages)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }

            for (var msg : infoMessages)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
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
            return "Notify user of startup messages";
        }
    }

}
