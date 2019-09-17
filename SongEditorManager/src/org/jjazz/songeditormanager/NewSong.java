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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "File", id = "org.jjazz.songeditormanager.NewSong")
@ActionRegistration(displayName = "#CTL_NewSong", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 0, separatorAfter = 2),
            @ActionReference(path = "Shortcuts", name = "C-N")
        })
@Messages("CTL_NewSong=New Song")
public final class NewSong implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(NewSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Song song = createSongFromTemplate();
        SongEditorManager.getInstance().showSong(song);
    }

    /**
     * Create a new song with its mix based on the saved template file.
     * <p>
     * If no song template file found or problem reading file, use SongManager.createEmptySong(name).<br>
     * Related MidiMix object is obtained using MidiMixManager.findMidiMix(song).
     * <p>
     * Created song will have its file property set to null.
     *
     * @return     
     */
    static public Song createSongFromTemplate()
    {
        SongManager sf = SongManager.getInstance();
        Song song = null;
        String name = sf.getNewSongName();
        File songTemplateFile = FileDirectoryManager.getInstance().getNewSongTemplateSongFile();
        if (songTemplateFile.exists())
        {
            song = sf.loadFromFile(songTemplateFile);
            if (song != null)
            {
                MidiMixManager mmm = MidiMixManager.getInstance();
                try
                {
                    // SongEditorManager will create the MidiMix, using the associated template file or
                    // creating a new one if no template file.
                    // Need to do this because we'll reset the song's file after, so SongEditorManager will not be able anymore
                    // to retrieve a MidiMix from the template file.
                    MidiMix mm = mmm.findMix(song);
                    mm.setFile(null);  // Do like it was created from scratch
                } catch (MidiUnavailableException ex)
                {
                    // Netbeans will show a user Dialog (because we used the Throwable argument
                    LOGGER.log(Level.SEVERE, "createSongFromTemplate() Unexpected problem building mix", ex);
                }

                song.setFile(null);    // Do like it was created from scratch. Must be done AFTER mmm.findMix(song)
                song.setName(name);
                song.resetNeedSave();
            }
        }
        if (song == null)
        {
            song = sf.createEmptySong(name);
        }
        return song;
    }

}
