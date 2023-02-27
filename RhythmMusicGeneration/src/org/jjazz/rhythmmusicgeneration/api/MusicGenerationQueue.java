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
package org.jjazz.rhythmmusicgeneration.api;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.util.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ChangeSupport;


/**
 * A thread to handle successive incoming music generation requests.
 * <p>
 * If several music generation requests arrive while a music generation task is already running, requests are buffered. When generation task
 * is done, a new music generation task is started with the last request received, the previous ones are discarded.
 * <p>
 * A ChangeEvent is fired when a music generation task is complete and a result is available. Note that ChangeEvent is fired outside of the
 * Swing EDT.
 */
public class MusicGenerationQueue implements Runnable
{

    private static final int POLL_INTERVAL_MS = 20;

    /**
     * A result from a music generation.
     *
     * @param userException If not null an exception occured.
     */
    public record Result(SongContext songContext, Map<RhythmVoice, Phrase> mapRvPhrases, Exception userException)
            {

    }


    private final Queue<SongContext> queue = new ConcurrentLinkedQueue<>();
    private ExecutorService executorService;
    private ScheduledExecutorService generationExecutorService;
    private Future<?> generationFuture;
    private UpdateGenerationTask generationTask;
    private SongContext pendingSongContext;
    private Result lastResult;
    private final int preUpdateBufferTimeMs;
    private final int postUpdateSleepTimeMs;
    private volatile boolean running;
    private final ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(MusicGenerationQueue.class.getSimpleName());

    /**
     * Create the handler.
     *
     * @param preUpdateBufferTimeMs (milliseconds) Wait this time upon receiving the first request before starting the music generation
     * @param postUpdateSleepTimeMs (milliseconds) Wait this time before restarting a music generation
     */
    public MusicGenerationQueue(int preUpdateBufferTimeMs, int postUpdateSleepTimeMs)
    {
        this.preUpdateBufferTimeMs = preUpdateBufferTimeMs;
        this.postUpdateSleepTimeMs = postUpdateSleepTimeMs;
    }

    /**
     * Add a music generation request to this queue.
     *
     * @param sgContext Generate music for this context. sgContext should not be modified after being passed to the method.
     */
    public void add(SongContext sgContext)
    {
        queue.add(sgContext);
    }

    public int getQueueSize()
    {
        return queue.size();
    }

    public boolean isRunning()
    {
        return running;
    }

    /**
     * Start the thread which listens to requests.
     *
     * @see #add(org.jjazz.songcontext.api.SongContext)
     */
    public void start()
    {
        if (!running)
        {
            running = true;
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this);
            generationExecutorService = Executors.newScheduledThreadPool(1);
        }
    }

    /**
     * Wait this time from the first received change before triggering an update.
     * <p>
     * This helps filtering out meaningless changes when a user action generates several changes (e.g. mouse drag).
     *
     * @return
     */
    public int getPreUpdateBufferTimeMs()
    {
        return preUpdateBufferTimeMs;
    }

    /**
     * The minimum delay between 2 consecutive updates.
     * <p>
     * This avoids too many sequencer changes in a short period of time, which can cause audio issues with notes muted/unmuted too many
     * times.
     * <p>
     * @return
     */
    public int getPostUpdateSleepTimeMs()
    {
        return postUpdateSleepTimeMs;
    }

    /**
     * Stop the thread.
     */
    public void stop()
    {
        if (running)
        {
            running = false;
            Utilities.shutdownAndAwaitTermination(generationExecutorService, 1000, 100);
            Utilities.shutdownAndAwaitTermination(executorService, 1, 1);
        }
    }

    @Override
    public void run()
    {
        while (running)
        {
            SongContext incoming = queue.poll();           // Does not block if empty

            if (incoming != null)
            {
                LOGGER.log(Level.FINE, "MusicGenerationQueue.run() handling incoming={0} nanoTime()={1}", new Object[]
                {
                    incoming, System.nanoTime()
                });
                // LOGGER.info("UpdateRequestsHandler.run() handling cls=" + toDebugString(incoming.getSong().getChordLeadSheet()));

                // Handle new context, save as pending if handling failed
                pendingSongContext = handleContext(incoming) ? null : incoming;

            } else if (pendingSongContext != null)
            {
                // Handle the last pending context, reset it if handling was successful
                if (handleContext(pendingSongContext))
                {
                    LOGGER.log(Level.FINE, "MusicGenerationQueue.run() handled pendingSongContext={0}", pendingSongContext);
                    pendingSongContext = null;
                }
            }


            try
            {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ex)
            {
                return;
            }
        }
    }

    public void addChangeListener(ChangeListener listener)
    {
        cs.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener)
    {
        cs.removeChangeListener(listener);
    }

    /**
     * Get the result from the last generation task.
     * <p>
     * Method should be called right after receiving a change event.
     * <p>
     *
     * @return Can be null if no request processed yet
     */
    public Result getLastResult()
    {
        return lastResult;
    }


    // =============================================================================================
    // Private methods
    // =============================================================================================
    /**
     * Try to start a new task or update existing task if possible.
     * <p>
     * If not possible, sgContext becomes the pendingContext.
     *
     * @param sgContext
     * @return True if task could be started or updated with sgContext, false otherwise
     */
    private boolean handleContext(SongContext sgContext)
    {
        boolean b;
        if (generationFuture == null)
        {
            // No generation task created yet, start one
            LOGGER.fine("handleContext() start generation FIRST TIME");
            startGenerationTask(sgContext);
            b = true;

        } else if (generationFuture.isDone())
        {
            // There is a generation task but it is complete, restart one
            LOGGER.fine("handleContext() start generation");
            startGenerationTask(sgContext);
            b = true;

        } else
        {
            // There is a generation task but not started yet (wait preUpdateBufferTimeMs), try to update it
            if (generationTask.changeContext(sgContext))
            {
                LOGGER.fine("handleContext() changed context of current generation task");
                // OK, task was waiting, we're done
                b = true;

            } else
            {
                // NOK, task is generating music
                b = false;
            }
        }

        return b;
    }


    /**
     * Start a generation task after a fixed delay.
     *
     * @param sgContext
     */
    private void startGenerationTask(SongContext sgContext)
    {
        try
        {
            generationTask = new UpdateGenerationTask(sgContext, postUpdateSleepTimeMs);
            generationFuture = generationExecutorService.schedule(generationTask, preUpdateBufferTimeMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex)
        {
            // Task is being shutdown 
            generationFuture = null;
            generationTask = null;
        }
    }


    // =============================================================================================
    // Inner classes
    // =============================================================================================
    /**
     * A task which creates the update and sleeps postUpdateSleepTime before notifying that the update is ready.
     */
    private class UpdateGenerationTask implements Runnable
    {

        private boolean started = false;
        private SongContext songContext;
        private final int postUpdateSleepTime;

        /**
         * Create an UpdateGenerator task for the given SongContext.
         * <p>
         *
         * @param sgContext           This must be an immutable instance (e.g. song must not be modified in parallel)
         * @param postUpdateSleepTime This delay avoids to have too many sequencer changes in a short period of time, which can cause audio
         *                            issues with notes muted/unmuted too many times.
         */
        UpdateGenerationTask(SongContext sgContext, int postUpdateSleepTime)
        {
            this.songContext = sgContext;
            this.postUpdateSleepTime = postUpdateSleepTime;
        }

        /**
         * Change the context for which to generate the update
         * <p>
         * Once the task has started (run() was called) the context can't be changed anymore.
         *
         * @param sgContext This must be an immutable instance (e.g. song must not be modified in parallel)
         * @return True if context could be changed (task is not started yet)
         */
        synchronized boolean changeContext(SongContext sgContext)
        {
            if (!started)
            {
                this.songContext = sgContext;
                return true;
            }
            return false;
        }


        @Override
        public void run()
        {
            synchronized (this)
            {
                started = true;
            }


            LOGGER.log(Level.FINE, "UpdateGenerationTask.run() >>> STARTING generation nanoTime()={0}", System.nanoTime());
            // LOGGER.info("UpdateGenerationTask.run() >>> STARTING generation cls=" + toDebugString(songContext.getSong().getChordLeadSheet()));


            // Recompute the RhythmVoice mapRvPhrases            
            SongSequenceBuilder sgBuilder = new SongSequenceBuilder(songContext);
            Exception exception = null;
            Map<RhythmVoice, Phrase> map = null;
            try
            {
                map = sgBuilder.buildMapRvPhrase(true);
            } catch (UserErrorGenerationException ex)
            {
                LOGGER.warning(ex.getMessage());
                exception = ex;

            } catch (MusicGenerationException ex)
            {
                // This is not normal (e.g. rhythm generation failure), notify user
                LOGGER.severe(ex.getMessage());
                exception = ex;
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);

            } catch (Exception ex)           // To make sure we catch other programming exceptions, sometimes not seen because in thread
            {
                LOGGER.severe(ex.getMessage());
                exception = ex;
                ex.printStackTrace();
            }


            lastResult = new Result(songContext, map, exception);


            // Notify listeners
            cs.fireChange();


            try
            {
                Thread.sleep(postUpdateSleepTime);
            } catch (InterruptedException ex)
            {
                LOGGER.warning("UpdateGenerator.run() Unexpected UpdateGenerator thread.sleep interruption ex=" + ex.getMessage());
                return;
            }


            LOGGER.log(Level.FINE, "UpdateGenerationTask.run() <<< ENDING generation  nanoTime={0}", System.nanoTime());

        }

    }
}
