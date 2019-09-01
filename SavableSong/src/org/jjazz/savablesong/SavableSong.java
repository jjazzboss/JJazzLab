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
package org.jjazz.savablesong;

import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.base.actions.Savable;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.song.api.Song;

/**
 * Our Savable implementation to save a song and its related MidiMix.
 */
public class SavableSong implements Savable
{

    public static final int SAVE_CODE_OK = 0;
    public static final int SAVE_CODE_CANCEL = 1;
    public static final int SAVE_CODE_ERROR_SONGFILE = 2;
    public static final int SAVE_CODE_ERROR_SONGMIX = 3;

    private Song song;

    private static final Logger LOGGER = Logger.getLogger(SavableSong.class.getSimpleName());

    public SavableSong(Song s)
    {
        if (s == null)
        {
            throw new IllegalArgumentException("s=" + s);
        }
        song = s;
    }

    @Override
    public int save()
    {
        File songFile = song.getFile();
        int res;
        if (songFile == null)
        {
            // Do like SaveAs
            res = new SaveAsCapableSong(song).SaveAs();
        } else
        {
            res = Util.saveSongAndMix(song, songFile);
        }
        return res;
    }

    /**
     * @param o
     * @return True if other SavableSong is for the same song.
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof SavableSong)
        {
            return song == ((SavableSong) o).song;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.song);
        return hash;
    }

    @Override
    public String toString()
    {
        File songFile = song.getFile();
        if (songFile == null)
        {
            return song.getName();
        }
        File songMixFile = FileDirectoryManager.getInstance().getSongMixFile(songFile);
        return songFile.getAbsolutePath() + ",  " + songMixFile.getAbsolutePath();
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================
}
