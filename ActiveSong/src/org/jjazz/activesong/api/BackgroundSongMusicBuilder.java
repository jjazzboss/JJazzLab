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
package org.jjazz.activesong.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.Utilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;

/**
 * Background task to generate the music Phrases for the whole song each time the active song changes.
 * <p>
 * In case of successive song changes, the class limits the number of music generation tasks to avoid using too much resources (eg ~1 per
 * second). The generated music phrases are intended to be used for non-interactive UI purposes, such as showing a bird-eye view of tracks
 * in the MixConsole.
 */
public class BackgroundSongMusicBuilder implements ChangeListener, PropertyChangeListener
{

    /**
     * This property change event is fired when a new result from the music generation of the active song is available.
     * <p>
     * The event is fired on the Swing EDT.
     * <p>
     * oldValue=old MusicGenerationQueue.Result, newValue=new MusicGenerationQueue.Result.
     * <p>
     */
    public static final String PROP_MUSIC_GENERATION_RESULT = "PropMusicGenerationResult";

    private static BackgroundSongMusicBuilder INSTANCE;
    private static final int PRE_UPDATE_BUFFER_TIME_MS = 600;
    private static final int POST_UPDATE_SLEEP_TIME_MS = 400;

    private MusicGenerationQueue.Result lastResult;
    private MusicGenerationQueue musicGenerationQueue;
    private SongMusicGenerationListener songMusicGenerationListener;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(BackgroundSongMusicBuilder.class.getSimpleName());

    public static BackgroundSongMusicBuilder getInstance()
    {
        synchronized (BackgroundSongMusicBuilder.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new BackgroundSongMusicBuilder();
            }
        }
        return INSTANCE;
    }

    private BackgroundSongMusicBuilder()
    {
        musicGenerationQueue = new MusicGenerationQueue(PRE_UPDATE_BUFFER_TIME_MS, POST_UPDATE_SLEEP_TIME_MS);
        musicGenerationQueue.addChangeListener(e -> musicGenerationResultReceived(musicGenerationQueue.getLastResult()));
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

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
        start();
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
        if (!pcs.hasListeners(PROP_MUSIC_GENERATION_RESULT))
        {
            stop();
        }
    }
    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {

        if (!musicGenerationQueue.isRunning())
        {
            return;
        }

        // It is an ActiveSongManager.PROP_ACTIVE_SONG event                  
        Song song = (Song) evt.getNewValue();
        MidiMix midiMix = (MidiMix) evt.getOldValue();
        if (songMusicGenerationListener != null)
        {
            songMusicGenerationListener.removeChangeListener(this);
            songMusicGenerationListener.cleanup();
        }

        if (song != null)
        {
            // Listen to new song context changes
            songMusicGenerationListener = new SongMusicGenerationListener(song, midiMix);
            songMusicGenerationListener.addChangeListener(this);
            stateChanged(null);     // Request an initial music generation
        } else
        {
            songMusicGenerationListener = null;
        }
    }

    //=============================================================================
    // ChangeListener interface
    //=============================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        // The musical content of the active song has changed

        // Prepare a copy of the song context
        var song = songMusicGenerationListener.getSong();
        LOGGER.log(Level.FINE, "stateChanged() -- posting music generation request for {0}", song.getName());
        var midiMix = songMusicGenerationListener.getMidiMix();
        SongContext sgContextCopy = new SongContext(song, midiMix).deepClone(false);
        Utilities.transpose(sgContextCopy.getSong().getChordLeadSheet(), PlaybackSettings.getInstance().getPlaybackKeyTransposition());


        // Request music generation
        musicGenerationQueue.add(sgContextCopy);
    }


    //=============================================================================
    // Private methods
    //=============================================================================
    private void musicGenerationResultReceived(MusicGenerationQueue.Result newResult)
    {
        LOGGER.fine("musicGenerationResultReceived() --");
        SwingUtilities.invokeLater(() -> 
        {
            var old = lastResult;
            lastResult = newResult;
            pcs.firePropertyChange(PROP_MUSIC_GENERATION_RESULT, old, lastResult);
        });

    }

    private void start()
    {
        if (!musicGenerationQueue.isRunning())
        {
            musicGenerationQueue.start();
            var asm = ActiveSongManager.getInstance();
            asm.addPropertyListener(ActiveSongManager.PROP_ACTIVE_SONG, this);
            
            
            Song song = asm.getActiveSong();
            MidiMix midiMix = asm.getActiveMidiMix();
            propertyChange(new PropertyChangeEvent(this, ActiveSongManager.PROP_ACTIVE_SONG, midiMix, song));
        }
    }

    private void stop()
    {
        if (musicGenerationQueue.isRunning())
        {
            musicGenerationQueue.stop();
            ActiveSongManager.getInstance().removePropertyListener(ActiveSongManager.PROP_ACTIVE_SONG, this);
        }
    }

}
