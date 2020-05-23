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
package org.jjazz.songeditormanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.base.actions.RecentFilesProvider;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.song.api.Song;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 * Rely on SongEditorManager to fill the RecentFilesProvider contract.
 */
@ServiceProvider(service = RecentFilesProvider.class)
public class RecentSongsProvider extends RecentFilesProvider implements PropertyChangeListener
{

    private final PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(RecentSongsProvider.class.getSimpleName());

    public RecentSongsProvider()
    {
        SongEditorManager.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public boolean open(File f)
    {
        Song song = SongEditorManager.getInstance().showSong(f);
        
        if (song != null && MusicController.getInstance().getPlaybackState().equals(MusicController.State.PLAYBACK_STOPPED))
        {
            MidiMix mm;
            try
            {
                mm = MidiMixManager.getInstance().findMix(song);
                ActiveSongManager.getInstance().setActive(song, mm);
            } catch (MidiUnavailableException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
        
        return song != null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == SongEditorManager.getInstance())
        {
            Song s = (Song) evt.getNewValue();
            if (evt.getPropertyName().equals(SongEditorManager.PROP_SONG_CLOSED))
            {
                if (s.getFile() != null)
                {
                    pcs.firePropertyChange(PROP_FILE_CLOSED, false, s.getFile());
                }
            } else if (evt.getPropertyName().equals(SongEditorManager.PROP_SONG_OPENED)
                    || evt.getPropertyName().equals(SongEditorManager.PROP_SONG_SAVED))
            {
                if (s.getFile() != null)
                {
                    pcs.firePropertyChange(PROP_FILE_OPENED, false, s.getFile());
                }
            }
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }
}
