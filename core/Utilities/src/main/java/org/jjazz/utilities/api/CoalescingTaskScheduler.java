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

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Call a task after a delay.
 * <p>
 * - User can call request(Runnable task) repeatedly<br>
 * - Runs the *last* Runnable only, after delayMs without new requests (debounce mode) or within a fixed window (throttle mode)<br>
 * <p>
 * By default, the Runnable executes on the scheduler thread. If you need EDT execution, wrap the Runnable: () -> SwingUtilities.invokeLater(...).
 * <p>
 * Thread-safe: all public methods can be called from any thread.
 */
public final class CoalescingTaskScheduler
{

    private final long delayMs;
    private final boolean restartDelayOnNewRequest;
    private final Object lock = new Object();
    private final Consumer<Throwable> exceptionHandler;
    private ScheduledFuture<?> scheduled;
    private Runnable lastAction;
    private static final Logger LOGGER = Logger.getLogger(CoalescingTaskScheduler.class.getSimpleName());

    /**
     * Create a CoalescingTaskScheduler with default settings (debounce mode, logging exceptions).
     *
     * @param delayMs The delay in milliseconds before executing the task
     * @throws IllegalArgumentException if delayMs is negative
     */
    public CoalescingTaskScheduler(long delayMs)
    {
        this(delayMs, true, null);
    }

    /**
     * Create a CoalescingTaskScheduler with custom exception handling.
     *
     * @param delayMs          The delay in milliseconds before executing the task
     * @param exceptionHandler Optional handler for exceptions thrown by scheduled tasks. If null, exceptions are logged at WARNING level.
     * @throws IllegalArgumentException if delayMs is negative
     */
    public CoalescingTaskScheduler(long delayMs, Consumer<Throwable> exceptionHandler)
    {
        this(delayMs, true, exceptionHandler);
    }

    /**
     * Create a CoalescingTaskScheduler with full configuration.
     *
     * @param delayMs                  The delay in milliseconds before executing the task
     * @param restartDelayOnNewRequest If true (debounce mode): each new request resets the delay timer. If false (throttle mode): the first request starts a
     *                                 fixed delay window, subsequent requests only update the task to run.
     * @param exceptionHandler         Optional handler for exceptions thrown by scheduled tasks. If null, exceptions are logged at WARNING level.
     */
    public CoalescingTaskScheduler(long delayMs, boolean restartDelayOnNewRequest, Consumer<Throwable> exceptionHandler)
    {
        Preconditions.checkArgument(delayMs >= 0, "delayMs=%s", delayMs);
        this.delayMs = delayMs;
        this.restartDelayOnNewRequest = restartDelayOnNewRequest;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Schedule action to run after delayMs.
     * <p>
     * Any exception thrown by the action will be handled by the configured exception handler, or logged if no handler was provided.
     *
     * @param action The task to execute
     */
    public void request(Runnable action)
    {
        Objects.requireNonNull(action, "action");

        synchronized (lock)
        {
            lastAction = action;

            // In throttle mode, if already scheduled, just update lastAction without restarting
            if (!restartDelayOnNewRequest && scheduled != null && !scheduled.isDone())
            {
                // Don't restart the delay, just updated lastAction
                return;
            }

            // In debounce mode, or in throttle mode with no pending task: (re)start the delay
            if (scheduled != null)
            {
                scheduled.cancel(false);
            }

            scheduled = SharedExecutorServices.getScheduledExecutor().schedule(() -> 
            {
                final Runnable toRun;
                synchronized (lock)
                {
                    // Run whatever is the latest action at execution time
                    toRun = lastAction;
                    scheduled = null;
                    lastAction = null;  // Clear reference to prevent memory leak
                }

                if (toRun != null)
                {
                    try
                    {
                        toRun.run();
                    } catch (Throwable ex)
                    {
                        handleException(ex);
                    }
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Cancel any pending action.
     * <p>
     * After this call, no task will execute unless request() is called again.
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
            lastAction = null;  // Clear reference to prevent memory leak
        }
    }

    /**
     * Check if there is a task currently scheduled to run.
     * <p>
     * Note: This method returns a snapshot of the current state. By the time the caller acts on this information, the state may have changed.
     *
     * @return true if a task is scheduled and has not yet executed or been cancelled
     */
    public boolean hasPendingTask()
    {
        synchronized (lock)
        {
            return scheduled != null && !scheduled.isDone();
        }
    }

    /**
     * Get the configured delay in milliseconds.
     *
     * @return the delay before task execution
     */
    public long getDelayMs()
    {
        return delayMs;
    }

    /**
     * Check if the scheduler is in debounce mode (restarting delay on each request).
     *
     * @return
     */
    public boolean isRestartDelayOnNewRequest()
    {
        return restartDelayOnNewRequest;
    }

    /**
     * Handle exceptions thrown by scheduled tasks.
     *
     * @param ex The exception that was thrown
     */
    private void handleException(Throwable ex)
    {
        if (exceptionHandler != null)
        {
            try
            {
                exceptionHandler.accept(ex);
            } catch (Throwable handlerEx)
            {
                // If the exception handler itself throws, log both exceptions
                LOGGER.log(Level.SEVERE, "handleException() Custom exception handler threw an exception while handling task exception: {0}", handlerEx.toString());
                LOGGER.log(Level.SEVERE, "handleException() Original task exception: {0}", ex.toString());
            }
        } else
        {
            // Default: log the exception
            LOGGER.log(Level.WARNING, "handleException() Unhandled scheduled task exception, see line below with stack trace");
            LOGGER.log(Level.WARNING, Utilities.getStackTrace(ex));
        }
    }
}
