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
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "File", id = "org.jjazz.songeditormanager.NewSong")
@ActionRegistration(displayName = "#CTL_NewSong", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 0, separatorAfter = 2),
            @ActionReference(path = "Shortcuts", name = "D-N"),
            @ActionReference(path = "Editors/TabActions")
        })
public final class NewSong implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(NewSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Song song = createSongFromTemplate();
        SongEditorManager.getDefault().showSong(song, true, false);
    }

    /**
     * Create a new song with its mix based on the saved template file.
     * <p>
     * if no template file create an empty song with a C starting chord.<br>
     * <p>
     * Created song will have its file property set to null.
     *
     * @return Can't be null
     */
    static public Song createSongFromTemplate()
    {
        SongFactory sf = SongFactory.getInstance();
        Song song = null;
        String name = sf.getNewSongName("NewSong");


        File songTemplateFile = getNewSongTemplateSongFile();
        if (songTemplateFile.exists())
        {
            try
            {
                song = Song.loadFromFile(songTemplateFile);    // Possible SongCreationException here


                // SongEditorManager will create the MidiMix, using the associated template file or
                // creating a new one if no template file.
                // Need to do this because we'll reset the song's file after, so SongEditorManager will not be able anymore
                // to retrieve a MidiMix from the template file.
                MidiMixManager mmm = MidiMixManager.getDefault();
                MidiMix mm = mmm.findMix(song);       // Possible MidiUnavailableException here
                mm.setFile(null);  // Do like it was created from scratch


                song.setFile(null);    // Do like it was created from scratch. Must be done AFTER mmm.findMix(song)
                song.setName(name);

            } catch (SongCreationException | MidiUnavailableException ex)
            {
                song = null; // Because non null if it's a MidiUnavailableException
                String msg = ResUtil.getString(NewSong.class, "ERR_CantCreateSongFromTemplate", songTemplateFile.getAbsolutePath());
                msg += ": " + ex.getLocalizedMessage();
                LOGGER.log(Level.WARNING, "createSongFromTemplate() {0}", msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }
        }

        if (song == null)
        {
            song = sf.createEmptySong(name, 8, "A", TimeSignature.FOUR_FOUR, "C");
        }

        return song;
    }


    static public File getNewSongTemplateSongFile()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        File f = new File(dir, SaveAsNewSongTemplate.TEMPLATE_SONG_NAME + "." + Song.SONG_EXTENSION);
        return f;
    }

}
