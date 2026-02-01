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

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Call a task after a delay.
 * <p>
 * - User can call request(Runnable) repeatedly<br>
 * - Runs the *last* Runnable only, after delayMs without new requests<br>
 * <p>
 * By default, the Runnable executes on the scheduler thread. If you need EDT execution, wrap the Runnable: () -> SwingUtilities.invokeLater(...).
 */
public final class CoalescingTaskScheduler
{

    private final long delayMs;
    private final Object lock = new Object();
    private ScheduledFuture<?> scheduled;
    private Runnable lastAction;

    public CoalescingTaskScheduler(long delayMs)
    {
        if (delayMs < 0)
        {
            throw new IllegalArgumentException("delayMs=" + delayMs);
        }
        this.delayMs = delayMs;
    }

    /**
     * Schedule action to run after delayMs.
     * <p>
     * If called again before it runs, the previous schedule is cancelled and replaced; only the most recent action will run.
     *
     * @param action
     */
    public void request(Runnable action)
    {
        Objects.requireNonNull(action, "action");
        synchronized (lock)
        {
            lastAction = action;

            if (scheduled != null)
            {
                scheduled.cancel(false);
            }

            scheduled = SharedExecutorServices.getScheduledExecutor().schedule(() -> 
            {
                final Runnable toRun;
                synchronized (lock)
                {
                    // run whatever is the latest action at execution time
                    toRun = lastAction;
                    scheduled = null;
                }
                if (toRun != null)
                {
                    toRun.run();
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Cancel any pending action.
     */
    public void cancel()
    {
        synchronized (lock)
        {
            if (scheduled != null)
            {
                scheduled.cancel(false);
                scheduled = null;
            }
            lastAction = null;
        }
    }
}
