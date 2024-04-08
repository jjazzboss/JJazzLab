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
package org.jjazz.songeditormanager;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

/**
 * Save the current song as the New Song template.
 */
@ActionID(category = "File", id = "org.jjazz.songeditormanager.saveasnewsongtemplate")
@ActionRegistration(displayName = "#CTL_NewSongTemplate", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1570)
        })
public class SaveAsNewSongTemplate extends AbstractAction
{

    public static final String TEMPLATE_SONG_NAME = "NewSongTemplate";
    private final Song song;

    private static final Logger LOGGER = Logger.getLogger(SaveAsNewSongTemplate.class.getSimpleName());

    public SaveAsNewSongTemplate(Song context)
    {
        this.song = context;
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        assert song != null;
        String fileNames = "";
        File songFile = NewSong.getNewSongTemplateSongFile();

        if (song.saveToFileNotify(songFile, true))
        {
            fileNames += songFile.getAbsolutePath() + "  ";
        }

        MidiMix midiMix = getMidiMixSilent(song);
        if (midiMix != null)
        {
            File mixFile = getNewSongTemplateMixFile();
            if (midiMix.saveToFileNotify(mixFile, true))
            {
                fileNames += mixFile.getAbsolutePath();
            }
        }
        if (!fileNames.isEmpty())
        {
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_SavedTemplates", fileNames));
        }


        Analytics.logEvent("Save As Song Template");
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================
    /**
     * Get the MidiMix object from the song.
     * <p>
     *
     * @param song
     * @return Can be null if problem
     */
    private MidiMix getMidiMixSilent(Song song)
    {
        MidiMix midiMix = null;
        try
        {
            midiMix = MidiMixManager.getDefault().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.SEVERE, "getMidiMixSilent() Could not retrieve MidiMix for song {0} - ex={1}", new Object[]
            {
                song.getName(),
                ex.getMessage()
            });
        }
        return midiMix;
    }


    private File getNewSongTemplateMixFile()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        File f = new File(dir, TEMPLATE_SONG_NAME + "." + MidiMix.MIX_FILE_EXTENSION);
        return f;
    }
}
