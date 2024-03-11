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
package org.jjazz.musiccontrol;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.activesong.spi.ActiveSongBackgroundMusicBuilder;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.MusicController.State;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.openide.util.ChangeSupport;
import org.openide.util.lookup.ServiceProvider;

/**
 * Service implementation.
 * <p>
 * When the active song is not playing, we use a SongMusicBuilderTask to get new Phrases each time the song changes. When song is playing, we listen to the
 * current UpdatableSongSession to get the updated phrases (which are provided as long as there is no structural change).
 */
@ServiceProvider(service = ActiveSongBackgroundMusicBuilder.class)
public class ActiveSongBackgroundMusicBuilderImpl implements PropertyChangeListener, ChangeListener, ActiveSongBackgroundMusicBuilder
{

    private enum Mode
    {
        OFF, PLAYING_SONG, NON_PLAYING_SONG
    };

    private volatile MusicGenerationQueue.Result lastResult;
    private SongMusicBuilderTask songMusicBuilderTask;
    private UpdatableSongSession updatableSongSession;
    private Song activeSong;
    private MidiMix activeMidiMix;
    private Mode mode;
    private boolean enabled;
    private final ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(ActiveSongBackgroundMusicBuilder.class.getSimpleName());


    public ActiveSongBackgroundMusicBuilderImpl()
    {
        this.mode = Mode.OFF;
        this.enabled = true;

        var asm = ActiveSongManager.getDefault();
        asm.addPropertyListener(this);

        var mc = MusicController.getInstance();
        mc.addPropertyChangeListener(this);

        activeSong = asm.getActiveSong();
        activeMidiMix = asm.getActiveMidiMix();
        if (activeSong != null)
        {
            propertyChange(new PropertyChangeEvent(asm, ActiveSongManager.PROP_ACTIVE_SONG, activeMidiMix, activeSong));
        }
    }

    /**
     * Get the last music generation result available.
     * <p>
     *
     * @return Can be null.
     */
    @Override
    public MusicGenerationQueue.Result getLastResult()
    {
        return lastResult;
    }

    /**
     * Check if ActiveSongMusicBuilder is directly being generating music that will produce a new Result.
     *
     * @return True if song is not playing and music is being generated because there was a song change.
     */
    @Override
    public boolean isDirectlyGeneratingMusic()
    {
        return songMusicBuilderTask != null && songMusicBuilderTask.isGeneratingMusic();
    }

    /**
     * Get the active song for this ActiveSongMusicBuilder.
     *
     * @return Can be null
     */
    @Override
    public Song getSong()
    {
        return activeSong;
    }

    /**
     * Change the ActiveSongMusicBuilder state.
     * <p>
     * When disabled the ActiveSongMusicBuilder does nothing. Convenient for debugging in specific cases.
     *
     * @param b
     */
    @Override
    synchronized public void setEnabled(boolean b)
    {
        if (enabled == b)
        {
            return;
        }
        enabled = b;
        LOGGER.info("setEnabled() b=" + b);
        if (!enabled)
        {
            setState(Mode.OFF, null, null, null);
        }
    }

    /**
     * Get state (true by default).
     *
     * @return
     */
    @Override
    synchronized public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * Register a listener to be notified each time a new result is available.
     *
     * @param listener
     * @see #getLastResult()
     */
    @Override
    public void addChangeListener(ChangeListener listener)
    {
        cs.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener)
    {
        cs.removeChangeListener(listener);
    }


    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    synchronized public void propertyChange(PropertyChangeEvent evt)
    {
        if (!enabled)
        {
            return;
        }

        var asm = ActiveSongManager.getDefault();
        var mc = MusicController.getInstance();


        Mode newMode = mode;
        UpdatableSongSession newUpdatableSongSession = updatableSongSession;
        Song newSong = activeSong;
        MidiMix newMidiMix = activeMidiMix;


        if (evt.getSource() == asm)
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                newSong = (Song) evt.getNewValue();          // Can be null
                newMidiMix = (MidiMix) evt.getOldValue();

                if (EnumSet.of(State.PAUSED, State.PLAYING).contains(mc.getState())
                        && mc.getPlaybackSession() instanceof UpdatableSongSession uss
                        && uss.getSongContext().getSong() == newSong)
                {
                    newUpdatableSongSession = uss;
                    newMode = Mode.PLAYING_SONG;
                } else
                {
                    newMode = newSong != null ? Mode.NON_PLAYING_SONG : Mode.OFF;
                }
            }

        } else if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                var oldState = (MusicController.State) evt.getOldValue();
                var newState = (MusicController.State) evt.getNewValue();


                if ((oldState.equals(State.PAUSED) && newState.equals(State.PLAYING))
                        || (oldState.equals(State.PLAYING) && newState.equals(State.PAUSED)))
                {
                    // Do nothing

                } else if (newState.equals(State.PLAYING))
                {
                    // Check if our song is playing
                    var session = mc.getPlaybackSession();
                    if (session instanceof UpdatableSongSession uss && uss.getSongContext().getSong() == activeSong)
                    {
                        newUpdatableSongSession = uss;
                        newMode = Mode.PLAYING_SONG;

                    } else
                    {
                        // MusicController plays something else, stop providing updates 
                        // This may be the Arranger, the instrument selection dialog, the RhythmSelectionDialog, the RP Drums Transform dialog, etc...
                        newMode = Mode.OFF;
                    }
                } else
                {
                    newMode = activeSong != null ? Mode.NON_PLAYING_SONG : Mode.OFF;
                }
            }

        } else if (evt.getSource() == updatableSongSession)
        {
            if (evt.getPropertyName().equals(UpdatableSongSession.PROP_UPDATE_RECEIVED))
            {
                var update = (UpdatableSongSession.Update) evt.getNewValue();
                playingSongResultReceived(update);
            }
        }


        setState(newMode, newUpdatableSongSession, newSong, newMidiMix);


    }

    // ==========================================================================================================
    // ChangeListener interface
    // ==========================================================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        nonPlayingSongResultReceived(songMusicBuilderTask.getLastResult());
    }


    //=============================================================================
    // Private methods
    //=============================================================================
    private void setState(Mode newMode, UpdatableSongSession newUpdatableSongSession, Song newSong, MidiMix newMidiMix)
    {
        if (newSong == activeSong && newMidiMix == activeMidiMix && newMode.equals(mode))
        {
            return;
        }

        activeSong = newSong;
        activeMidiMix = newMidiMix;
        mode = newMode;


        stopListeningToNonPlayingSong();
        stopListeningToPlayingSong();


        switch (mode)
        {
            case OFF ->
            {
                // Nothing
            }
            case PLAYING_SONG ->
            {
                updatableSongSession = newUpdatableSongSession;
                assert updatableSongSession != null;
                updatableSongSession.addPropertyChangeListener(this);
            }
            case NON_PLAYING_SONG ->
            {
                assert activeSong != null;
                assert activeMidiMix != null;
                songMusicBuilderTask = new SongMusicBuilderTask(activeSong, activeMidiMix);
                songMusicBuilderTask.addChangeListener(this);
                songMusicBuilderTask.start();
            }
            default -> throw new AssertionError(mode.name());
        }
    }


    private void stopListeningToPlayingSong()
    {
        if (updatableSongSession != null)
        {
            updatableSongSession.removePropertyChangeListener(this);
        }
        updatableSongSession = null;
    }


    private void stopListeningToNonPlayingSong()
    {
        if (songMusicBuilderTask != null)
        {
            songMusicBuilderTask.removeChangeListener(this);
            songMusicBuilderTask.stop();
        }
        songMusicBuilderTask = null;
    }


    private void nonPlayingSongResultReceived(MusicGenerationQueue.Result result)
    {
        assert mode.equals(Mode.NON_PLAYING_SONG);
        lastResult = result;
        cs.fireChange();
    }


    private void playingSongResultReceived(UpdatableSongSession.Update update)
    {
        assert mode.equals(Mode.PLAYING_SONG);
        var mapRvPhrases = update.getMapRvPhrases();
        if (mapRvPhrases == null)
        {
            return;
        }

        // Need to transform it into a MusicGenerationQueue.Result
        lastResult = new MusicGenerationQueue.Result(new SongContext(activeSong, activeMidiMix), mapRvPhrases, null);
        cs.fireChange();
    }


}
