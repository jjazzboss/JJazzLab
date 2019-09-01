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
import org.jjazz.base.actions.RecentFilesProvider;
import org.jjazz.song.api.Song;
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
        return SongEditorManager.getInstance().showSong(f) != null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == SongEditorManager.getInstance())
        {
            Song s = (Song) evt.getNewValue();
            if (evt.getPropertyName() == SongEditorManager.PROP_SONG_CLOSED)
            {
                if (s.getFile() != null)
                {
                    pcs.firePropertyChange(PROP_FILE_CLOSED, false, s.getFile());
                }
                s.removePropertyChangeListener(this);
            } else if (evt.getPropertyName() == SongEditorManager.PROP_SONG_OPENED
                    || evt.getPropertyName() == SongEditorManager.PROP_SONG_SAVED)
            {
                if (s.getFile() != null)
                {
                    pcs.firePropertyChange(PROP_FILE_OPENED, false, s.getFile());
                } else
                {
                    // It's a new song, need to listen to song save/closed event
                    s.addPropertyChangeListener(this);
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
