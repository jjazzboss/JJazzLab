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
package org.jjazz.activesong.spi;

import javax.swing.event.ChangeListener;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

/**
 * A service provider which provides the musical phrases of the active song, which are built in a background task.
 * <p>
 */
public interface ActiveSongBackgroundMusicBuilder
{

    /**
     * Get the default implementation.
     *
     * @return Can't be null
     */
    public static ActiveSongBackgroundMusicBuilder getDefault()
    {
        var res = Lookup.getDefault().lookup(ActiveSongBackgroundMusicBuilder.class);
        assert res != null;
        return res;
    }

    /**
     * Register a listener to be notified each time a new result is available.
     *
     * @param listener
     * @see #getLastResult()
     */
    void addChangeListener(ChangeListener listener);

    /**
     * Get the last music generation result available.
     * <p>
     *
     * @return Can be null.
     */
    MusicGenerationQueue.Result getLastResult();

    /**
     * Get the active song for this ActiveSongMusicBuilder.
     *
     * @return Can be null
     */
    Song getSong();

    /**
     * Check if ActiveSongMusicBuilder is directly being generating music that will produce a new Result.
     *
     * @return True if song is not playing and music is being generated because there was a song change.
     */
    boolean isDirectlyGeneratingMusic();

    /**
     * Get state (true by default).
     *
     * @return
     */
    boolean isEnabled();

    void removeChangeListener(ChangeListener listener);

    /**
     * Change the ActiveSongMusicBuilder state.
     * <p>
     * When disabled the ActiveSongMusicBuilder does nothing. Convenient for debugging in specific cases.
     *
     * @param b
     */
    void setEnabled(boolean b);

}
