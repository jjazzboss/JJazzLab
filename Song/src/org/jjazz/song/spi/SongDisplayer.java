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
package org.jjazz.song.spi;

import java.io.File;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.openide.util.Lookup;

/**
 * A service provider which can display a song.
 */
public interface SongDisplayer
{
    /**
     * Get the default implementation in the global lookup.
     *
     * @return Can't be null
     */
    static public SongDisplayer getDefault()
    {
        SongDisplayer res = Lookup.getDefault().lookup(SongDisplayer.class);
        assert res != null;
        return res;
    }

    /**
     * Load a song from a file and show it in the application.
     * <p>
     *
     * @param f
     * @param makeActive
     * @param updateLastSongDirectory If true and the file is not already shown, update the LastSongDirectory in
     *                                FileDirectoryManager.
     * @return The created song from file f
     * @throws org.jjazz.song.api.SongCreationException
     */
    Song showSong(File f, boolean makeActive, boolean updateLastSongDirectory) throws SongCreationException;

    /**
     * Do what's required to show a song in the application.
     * <p>
     *
     * @param song
     * @param makeActive If true try to make the song musically active, see ActiveSongManager.
     * @return
     * @throws org.jjazz.song.api.SongCreationException
     */
    void showSong(Song song, boolean makeActive) throws SongCreationException;
}
