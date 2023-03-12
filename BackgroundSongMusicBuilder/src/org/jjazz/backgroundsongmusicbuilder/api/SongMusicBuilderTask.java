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
package org.jjazz.backgroundsongmusicbuilder.api;

import com.google.common.base.Preconditions;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.Utilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.openide.util.ChangeSupport;

/**
 * A task to regenerate song music Phrases each time the song changes.
 * <p>
 * The task uses a MusicGenerationQueue to handle rapid successive song changes efficiently.
 * <p>
 * A ChangeEvent is fired when a new result is available via getLastResult().
 */
public class SongMusicBuilderTask implements ChangeListener
{

    private static final int PRE_UPDATE_BUFFER_TIME_MS = 1000;
    private static final int POST_UPDATE_SLEEP_TIME_MS = 500;

    private MusicGenerationQueue.Result lastResult;
    private MusicGenerationQueue musicGenerationQueue;
    private SongMusicGenerationListener songMusicGenerationListener;
    private final Song song;
    private final MidiMix midiMix;
    private final int preUpdateBufferTimeMs;
    private final int postUpdateSleepTimeMs;
    private ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongMusicBuilderTask.class.getSimpleName());


    public SongMusicBuilderTask(Song song, MidiMix midiMix)
    {
        this(song, midiMix, PRE_UPDATE_BUFFER_TIME_MS, POST_UPDATE_SLEEP_TIME_MS);
    }

    /**
     * Create the task.
     *
     * @param song
     * @param midiMix
     * @param preUpdateBufferTimeMs
     * @param postUpdateSleepTimeMs
     * @see MusicGenerationQueue#getPreUpdateBufferTimeMs()
     * @see MusicGenerationQueue#getPostUpdateSleepTimeMs()
     */
    public SongMusicBuilderTask(Song song, MidiMix midiMix, int preUpdateBufferTimeMs, int postUpdateSleepTimeMs)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        this.song = song;
        this.midiMix = midiMix;
        this.preUpdateBufferTimeMs = preUpdateBufferTimeMs;
        this.postUpdateSleepTimeMs = postUpdateSleepTimeMs;
    }

    public Song getSong()
    {
        return song;
    }

    /**
     * This task should be stopped via this method in order to release all resources.
     */
    public void stop()
    {
        if (musicGenerationQueue != null)
        {
            musicGenerationQueue.stop();
            musicGenerationQueue.removeChangeListener(this);
            songMusicGenerationListener.removeChangeListener(this);
            songMusicGenerationListener.cleanup();
        }
    }


    public void start()
    {
        if (musicGenerationQueue == null || !musicGenerationQueue.isRunning())
        {
            musicGenerationQueue = new MusicGenerationQueue(preUpdateBufferTimeMs, postUpdateSleepTimeMs);
            musicGenerationQueue.addChangeListener(this);
            musicGenerationQueue.start();

            songMusicGenerationListener = new SongMusicGenerationListener(song, midiMix);
            songMusicGenerationListener.addChangeListener(this);

            // Force the 1st generation
            stateChanged(new ChangeEvent(songMusicGenerationListener));
        }
    }

    public boolean isStarted()
    {
        return musicGenerationQueue.isRunning();
    }

    /**
     * Get the last music generation result.
     *
     * @return Can be null.
     */
    public MusicGenerationQueue.Result getLastResult()
    {
        return lastResult;
    }

    /**
     * Register a listener to be notified when a new music result is available.
     *
     * @param listener
     * @see #getLastResult()
     */
    public void addChangeListener(ChangeListener listener)
    {
        cs.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener)
    {
        cs.removeChangeListener(listener);
    }


    //=============================================================================
    // ChangeListener interface
    //=============================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == songMusicGenerationListener)
        {
            // Song has changed musically
            Runnable r = () -> 
            {
                // Prepare a copy of the song context
                LOGGER.log(Level.FINE, "stateChanged() -- posting music generation request for {0}", song.getName());
                SongContext sgContextCopy = new SongContext(song, midiMix).deepClone(false);
                Utilities.transpose(sgContextCopy.getSong().getChordLeadSheet(),
                        PlaybackSettings.getInstance().getPlaybackKeyTransposition());

                // Request music generation
                musicGenerationQueue.add(sgContextCopy);
            };
            new Thread(r, "SongMusicBuilderTask-PrepareSgContextCopy").start();

        } else if (e.getSource() == musicGenerationQueue)
        {

            // MusicGenerationQueue produced a new result
            LOGGER.log(Level.FINE, "stateChanged() -- received music generation result for {0}", song.getName());
            lastResult = musicGenerationQueue.getLastResult();
            cs.fireChange();

        }
    }


    //=============================================================================
    // Private methods
    //=============================================================================
}
