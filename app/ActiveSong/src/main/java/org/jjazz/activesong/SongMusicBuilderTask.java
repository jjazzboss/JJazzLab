/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.activesong;

import org.jjazz.musiccontrol.api.MusicGenerationQueue;
import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicGenerationQueue.Result;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.openide.util.ChangeSupport;

/**
 * A task to regenerate song music Phrases each time the song changes.
 * <p>
 * The task uses a SongMusicGenerationListener to use only relevant song changes, and a MusicGenerationQueue to handle rapid successive song changes
 * efficiently.
 * <p>
 * A ChangeEvent is fired when a new result is available via getLastResult().
 */
public class SongMusicBuilderTask implements ChangeListener, PropertyChangeListener
{

    /**
     * @see SongMusicGenerationListener
     */
    public static final int PRE_CHANGE_EVENT_DELAY_MS = 200;
    /**
     * @see MusicGenerationQueue
     */
    public static final int PRE_UPDATE_BUFFER_TIME_MS = 500;
    /**
     * @see MusicGenerationQueue
     */
    public static final int POST_UPDATE_SLEEP_TIME_MS = 500;


    private Result lastResult;
    private MusicGenerationQueue musicGenerationQueue;
    private SongMusicGenerationListener songMusicGenerationListener;
    private final Song song;
    private final MidiMix midiMix;
    private final int preUpdateBufferTimeMs;
    private final int postUpdateSleepTimeMs;
    private final Set<Object> ignoredSongChangeSourceEvents;
    private final ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongMusicBuilderTask.class.getSimpleName());


    /**
     * Create the task.
     *
     * @param song
     * @param midiMix
     * @param ignoredSongChangeSourceEvents Specify song changes which should not trigger a new music generation. See {@link SongMusicGenerationListener#setBlackList(java.util.Set)}
     */
    public SongMusicBuilderTask(Song song, MidiMix midiMix, Set<Object> ignoredSongChangeSourceEvents)
    {
        this(song, midiMix, ignoredSongChangeSourceEvents, PRE_UPDATE_BUFFER_TIME_MS, POST_UPDATE_SLEEP_TIME_MS);
    }

    /**
     * Create the task.
     *
     * @param song
     * @param midiMix
     * @param ignoredSongChangeSourceEvents Specify song changes which should not trigger a new music generation. See {@link SongMusicGenerationListener#setBlackList(java.util.Set)}
     *
     * @param preUpdateBufferTimeMs
     * @param postUpdateSleepTimeMs         @see MusicGenerationQueue#getPreUpdateBufferTimeMs() @see MusicGenerationQueue#getPostUpdateSleepTimeMs()
     */
    public SongMusicBuilderTask(Song song, MidiMix midiMix, Set<Object> ignoredSongChangeSourceEvents, int preUpdateBufferTimeMs, int postUpdateSleepTimeMs)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        Preconditions.checkNotNull(ignoredSongChangeSourceEvents);
        this.song = song;
        this.midiMix = midiMix;
        this.preUpdateBufferTimeMs = preUpdateBufferTimeMs;
        this.postUpdateSleepTimeMs = postUpdateSleepTimeMs;
        this.ignoredSongChangeSourceEvents = ignoredSongChangeSourceEvents;
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
            songMusicGenerationListener.removePropertyChangeListener(this);
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

            songMusicGenerationListener = new SongMusicGenerationListener(song, midiMix, PRE_CHANGE_EVENT_DELAY_MS);
            songMusicGenerationListener.setBlackList(ignoredSongChangeSourceEvents);
            songMusicGenerationListener.addPropertyChangeListener(this);

            // Force the 1st generation
            postMusicGenerationRequest();
        }
    }

    public boolean isStarted()
    {
        return musicGenerationQueue.isRunning();
    }

    /**
     * Get the last music generation result.
     *
     * @return Can be null. The SongContext field will be a SongContextCopy instance.
     */
    public Result getLastResult()
    {
        return lastResult;
    }

    /**
     * Check if task is being generating music to produce a future Result.
     *
     * @return
     */
    public boolean isGeneratingMusic()
    {
        return musicGenerationQueue.isGeneratingMusic();
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
    // =================================================================================================================
    // PropertyChangeListener interface
    // =================================================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == songMusicGenerationListener)
        {
            if (e.getPropertyName().equals(SongMusicGenerationListener.PROP_MUSIC_GENERATION_COMBINED))
            {
                // Song has changed musically
                postMusicGenerationRequest();
            }
        }
    }

    //=============================================================================
    // ChangeListener interface
    //=============================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == musicGenerationQueue)
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
    private void postMusicGenerationRequest()
    {
        // Can't use a thread here because this might lead to concurrent modification (eg of a user phrase) while copy is being made                
        LOGGER.log(Level.FINE, "stateChanged() -- posting music generation request for {0}", song.getName());
        if (song.getChordLeadSheet().getSection(0) == null)
        {
            // Robustness check, happened once but can't reproduce...
            LOGGER.log(Level.WARNING, "postMusicGenerationRequest() called but missing initial section in chordleadsheet={0}",
                    song.getChordLeadSheet().toDebugString());
            return;
        }
        SongContext sgContext = new SongContext(song, midiMix);


        // Request music generation
        musicGenerationQueue.add(sgContext);
    }


    //=============================================================================
    // Inner classes
    //=============================================================================
}
