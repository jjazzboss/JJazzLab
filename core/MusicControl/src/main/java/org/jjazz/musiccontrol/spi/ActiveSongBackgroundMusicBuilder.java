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
package org.jjazz.musiccontrol.spi;

import javax.swing.event.ChangeListener;
import org.jjazz.musiccontrol.api.MusicGenerationQueue.Result;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

/**
 * A service provider which provides the musical phrases of the active song, which are built in some background task.
 * <p>
 */
public interface ActiveSongBackgroundMusicBuilder
{

    /**
     * Get the default implementation.
     *
     * @return Can be null
     */
    public static ActiveSongBackgroundMusicBuilder getDefault()
    {
        var res = Lookup.getDefault().lookup(ActiveSongBackgroundMusicBuilder.class);
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
     * Get the latest music generation result available.
     * <p>
     * Note that the returned value might not be up to date, see {@link #isLastResultUpToDate() }.
     *
     * @return Can be null.
     */
    Result getLastResult();

    /**
     * Check if the Result returned by getLastResult() is up to date.
     * <p>
     * Returns false if :<br>
     * - last result is null<br>
     * - A new music generation is currently being generated<br>
     * - Active song is playing and there was a song structure change<br>
     *
     * @return
     */
    boolean isLastResultUpToDate();

    /**
     * Get the active song for this ActiveSongMusicBuilder.
     *
     * @return Can be null
     */
    Song getSong();

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
