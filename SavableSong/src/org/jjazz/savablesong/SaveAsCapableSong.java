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
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.base.actions.api.SaveAsCapable;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import static org.jjazz.savablesong.SavableSong.SAVE_CODE_CANCEL;
import org.jjazz.song.api.Song;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;

/**
 * Our implementation of SaveAsCapable to save a song and the related MidiMix.
 * <p>
 */
public class SaveAsCapableSong implements SaveAsCapable
{

    private Song song;
    private static final Logger LOGGER = Logger.getLogger(SaveAsCapableSong.class.getSimpleName());

    public SaveAsCapableSong(Song song)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song=" + song);   //NOI18N
        }
        this.song = song;
    }

    @Override
    public int SaveAs()
    {
        File songFile = Util.getValidatedSaveFile(song);
        songFile = Util.showSaveSongFileChooser(songFile);
        if (songFile == null)
        {
            return SAVE_CODE_CANCEL;
        }

        // Save song and related mix
        int res = Util.saveSongAndMix(song, songFile);
        if (res == SavableSong.SAVE_CODE_OK)
        {
            String mixString = "";
            try
            {
                MidiMix mm = MidiMixManager.getInstance().findMix(song);
                mixString = ", " + mm.getFile().getAbsolutePath();
            } catch (MidiUnavailableException ex)
            {
                // We should never be there since searched MidiMix already exist 
                Exceptions.printStackTrace(ex);
            }
            String files=songFile.getAbsolutePath() + mixString;
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(),"SAVED AS {0}", files));
        }
        return res;
    }

    @Override
    public String toString()
    {
        File file = song.getFile();
        return file == null ? song.getName() : file.getName();
    }

}
