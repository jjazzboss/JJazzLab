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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jjazz.songcontext.api.SongContext;

/**
 * A helper class to update a UpdatableSongSession in real time.
 */
public class SongSessionUpdater
{

    public static final int DEFAULT_POST_UPDATE_SLEEP_TIME_MS = 100;
    private int postUpdateSleepTime = DEFAULT_POST_UPDATE_SLEEP_TIME_MS;
    private UpdatableSongSession session;
    private SongContext songContext;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private ExecutorService generationExecutorService;
    private Future<?> generationFuture;
    private volatile boolean running;

    /**
     *
     * @param session
     * @param postUpdateSleepTime In milliseconds, see {@link #getPostUpdateSleepTime()}.
     */
    public SongSessionUpdater(UpdatableSongSession session, int postUpdateSleepTime)
    {
        this.postUpdateSleepTime = postUpdateSleepTime;
        this.session = session;
    }

    /**
     * Make a copy of sgContext and try to generate 
     * @param sgContext
     * @return
     */
    public synchronized boolean offerUpdate(SongContext sgContext)
    {
        return false;
    }

    /**
     * Get the sleep time (in milliseconds) added after a session update in order to avoid too many sequence changes in a short
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


}
