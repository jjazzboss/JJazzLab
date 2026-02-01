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
package org.jjazz.utilities.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application-level shared executor services.
 */
public class SharedExecutorServices
{

    private static ExecutorService executor;
    private static ScheduledExecutorService singleThreadScheduledExecutor;
    private static final Logger LOGGER = Logger.getLogger(SharedExecutorServices.class.getSimpleName());

    /**
     * For CPU/IO tasks.
     *
     * @return
     */
    static public ExecutorService getExecutor()
    {
        if (executor == null)
        {
            executor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 1), getThreadFactory("JL-SharedExecutor", true));
        }
        return executor;
    }

    /**
     * For timers/debounce tasks.
     *
     * @return
     */
    static public ScheduledExecutorService getScheduledExecutor()
    {
        if (singleThreadScheduledExecutor == null)
        {
            singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor(getThreadFactory("JL-SharedScheduledExecutor", true));
        }
        return singleThreadScheduledExecutor;
    }

    /**
     * Helper method to create a ThreadFactory which produce a named thread.
     * <p>
     * Thread is attached an exception handler which log the exception.
     *
     * @param baseName
     * @param daemon
     * @return
     */
    public static ThreadFactory getThreadFactory(String baseName, boolean daemon)
    {
        AtomicInteger n = new AtomicInteger(1);
        ThreadFactory tf = r -> 
        {
            Thread t = new Thread(r);
            t.setName(baseName + "-" + n.getAndIncrement());
            t.setDaemon(daemon);
            t.setUncaughtExceptionHandler((th, ex) -> LOGGER.log(Level.SEVERE, "Uncaught in " + th.getName(), ex));
            return t;
        };
        return tf;
    }
}
