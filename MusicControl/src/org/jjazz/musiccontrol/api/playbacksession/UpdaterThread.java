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
package org.jjazz.musiccontrol.api.playbacksession;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.util.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Helper class to dynamically update an UpdatableSongSession with song changes.
 * <p>
 * This thread handles incoming song changes via a Queue and starts one MusicGenerationTask at a time with the latest available
 * song change. The MusicGenerationTask uses a StaticSongSessionProvider to generate and retrieve the update to be applied to the
 * UpdatableSongSession.
 * <p>
 * IMPORTANT: This class should be used only when a DynamicSongSession is not sufficient to work with an UpdatableSongSession,
 * usually because the song changes are not handled by a DynamicSongSession.
 *
 * @param <T> The class of the song change, can be a song part's RhythmParameter value, a changed chord symbol, etc.
 */
public class UpdaterThread<T> implements Runnable
{

    /**
     * Required by the MusicGenerationTask.
     *
     * @param <T>
     */
    public interface BaseSongSessionProvider<T>
    {

        /**
         * Get a BaseSongSession for the specified song change.
         * <p>
         * If the returned session is in the NEW state then the MusicGenerationTask will generate it.
         *
         * @param songChange
         * @return If null the MusicGenerationTask will ignore this song change
         */
        BaseSongSession get(T songChange);
    }


    public static final int DEFAULT_POST_UPDATE_SLEEP_TIME_MS = 100;
    private int postUpdateSleepTime = DEFAULT_POST_UPDATE_SLEEP_TIME_MS;

    private UpdatableSongSession updatableSession;
    private BaseSongSessionProvider<T> provider;
    private final Queue<T> queue;
    private T lastSongChange;
    private ExecutorService executorService;
    private ExecutorService generationExecutorService;
    private Future<?> generationFuture;
    private volatile boolean running;
    private static final Logger LOGGER = Logger.getLogger(UpdaterThread.class.getSimpleName());  //NOI18N


    /**
     *
     * @param provider
     * @param queue The queue used by caller to pass the song changes to this thread.
     * @param postUpdateSleepTime
     * @see UpdaterThread<T>#setPostUpdateSleepTime(int)
     */
    public UpdaterThread(BaseSongSessionProvider<T> provider, Queue<T> queue, int postUpdateSleepTime)
    {
        checkNotNull(provider);
        checkNotNull(queue);
        this.queue = queue;
        this.provider = provider;
    }

    /**
     * Use DEFAULT_POST_UPDATE_SLEEP_TIME_MS.
     *
     * @param provider
     * @param queue
     */
    public UpdaterThread(BaseSongSessionProvider<T> provider, Queue<T> queue)
    {
        this(provider, queue, DEFAULT_POST_UPDATE_SLEEP_TIME_MS);
    }

    /**
     * Set the session and start the thread.
     * <p>
     * If thread is already started do nothing.
     *
     * @param session
     */
    public void start(UpdatableSongSession session)
    {
        checkNotNull(session);
        this.updatableSession = session;
        if (!running)
        {
            running = true;
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this);
            generationExecutorService = Executors.newSingleThreadExecutor();
        }
    }

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
            T songChange = queue.poll();           // Does not block if empty
            if (songChange == null)
            {
                // No incoming song change, check if we have a waiting songChange                        
                if (lastSongChange != null)
                {
                    // We have a song change, can we start a musicGenerationTask ?
                    if (generationFuture == null || generationFuture.isDone())
                    {
                        // yes
                        startMusicGenerationTask();
                    } else
                    {
                        // Need to wait a little more for the previous musicGenerationTask to complete                        
                    }
                }
            } else
            {
                lastSongChange = songChange;

//                    LOGGER.info("UpdaterThread.run() song change received=" + songChange);

                // We have an incoming song change, start a musicGenerationTask if possible
                if (generationFuture == null || generationFuture.isDone())
                {
                    startMusicGenerationTask();
                } else
                {
                    // Need to wait a little more for the previous musicGenerationTask to complete                        
//                        LOGGER.info("                                   => can't start generation task, maybe next loop?");
                }
            }
        }
    }


    /**
     * Get the sleep time (in milliseconds) added after a sequence update in order to avoid too many sequence changes in a short
     * period of time.
     * <p>
     * An update on a given track stops ringing notes on that track, so very frequent changes should be avoided when possible.
     * <p>
     * @return
     */
    public int getPostUpdateSleepTime()
    {
        return postUpdateSleepTime;
    }

    /**
     * The minimum delay between 2 consecutive updates.
     * <p>
     * Track changes stop ringing notes on that track, so very frequent changes should be avoided when possible.
     *
     * @param postUpdateSleepTime In milliseconds
     */
    public void setPostUpdateSleepTime(int postUpdateSleepTime)
    {
        this.postUpdateSleepTime = postUpdateSleepTime;
    }


    // ============================================================================
    // Private methods
    // ============================================================================
    private void startMusicGenerationTask()
    {
        try
        {
            generationFuture = generationExecutorService.submit(new MusicGenerationTask(lastSongChange));
        } catch (RejectedExecutionException ex)
        {
            // Task is being shutdown 
            generationFuture = null;
        }
        lastSongChange = null;

    }

    // ============================================================================
    // Private classes
    // ============================================================================
    private class MusicGenerationTask implements Runnable
    {

        private final T songChange;

        MusicGenerationTask(T sgChange)
        {
            this.songChange = sgChange;
        }

        @Override
        public void run()
        {
//            LOGGER.info("MusicGenerationTask.run() >>> STARTING generation with rpValue=" + rpValue);

            BaseSongSession tmpSession = provider.get(songChange);
            if (tmpSession == null)
            {
                return;
            }


            if (tmpSession.getState().equals(PlaybackSession.State.NEW))
            {
                try
                {
                    tmpSession.generate(true);          // This can block for some time, possibly a few seconds on slow computers/complex rhythms
                } catch (MusicGenerationException ex)
                {
                    LOGGER.warning("MusicGenerationTask.run() ex=" + ex.getMessage());
                    NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                    return;
                }
            }


            // Perform the update 
            UpdatableSongSession.Update update = new UpdatableSongSession.Update(tmpSession.getRvPhraseMap(), null);
            updatableSession.updateSequence(update);


            // Avoid to have too many sequencer changes in a short period of time, which can cause audio issues
            // with notes muted/unmuted too many times
            try
            {
                Thread.sleep(getPostUpdateSleepTime());
            } catch (InterruptedException ex)
            {
                return;
            }

//            LOGGER.info("MusicGenerationTask.run() <<< ENDING generation with rpValue=" + rpValue);
        }


    }
}
