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
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import static org.jjazz.savablesong.SavableSong.SAVE_CODE_ERROR_SONGFILE;
import static org.jjazz.savablesong.SavableSong.SAVE_CODE_ERROR_SONGMIX;
import static org.jjazz.savablesong.SavableSong.SAVE_CODE_OK;
import org.jjazz.song.api.Song;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * Convenience methods for the package
 */
class Util
{

    private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

    /**
     * Show a FileChooser dialog to pick a save file.
     * <p>
     * Add extension to selected file name if required. Ask for confirmation if file overwrite.
     * <p>
     *
     * @param presetFile If null JFileChooser is not preset with this file
     * @return The actual save file selected by user. Null if canceled.
     */
    static public File showSaveSongFileChooser(File presetFile)
    {
        JFileChooser chooser = Utilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JJazzLab songs" + " (" + "." + FileDirectoryManager.SONG_EXTENSION + ")", FileDirectoryManager.SONG_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Save song");                

        if (presetFile != null)
        {
            chooser.setCurrentDirectory(presetFile.getParentFile()); // required because if defaultSongFile does not yet exist, setSelectedFile does not set the current directory
            chooser.setSelectedFile(presetFile);
        }

        int returnCode = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        File songFile = null;
        if (returnCode == JFileChooser.APPROVE_OPTION)
        {
            songFile = chooser.getSelectedFile();
            String songName = songFile.getName();

            // Add file extension if required
            if (!org.jjazz.util.api.Utilities.endsWithIgnoreCase(songName, "." + FileDirectoryManager.SONG_EXTENSION))
            {
                songFile = new File(songFile.getParent(), songName + "." + FileDirectoryManager.SONG_EXTENSION);
            }

            if (songFile.exists())
            {
                // Confirm overwrite
                NotifyDescriptor nd = new NotifyDescriptor.Confirmation(songFile.getName() + " - " + "Confirm overwrite ?", NotifyDescriptor.OK_CANCEL_OPTION);
                Object result = DialogDisplayer.getDefault().notify(nd);
                if (result != NotifyDescriptor.OK_OPTION)
                {
                    // Cancel
                    songFile = null;
                }
            }
        }

        return songFile;
    }

    /**
     * Save the song and the related midiMix file.
     * <p>
     *
     * @param song
     * @return SAVE_CODE_OK, SAVE_CODE_ERROR_SONGMIX or SAVE_CODE_ERROR_SONGFILE
     */
    static public int saveSongAndMix(Song song, File songFile)
    {
        if (song == null || songFile == null)
        {
            throw new IllegalArgumentException("song=" + song + " songFile=" + songFile);   //NOI18N
        }
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        int resSong;
        int resMix;

        // Save Mix
        MidiMix songMix = getMidiMixSilent(song);
        if (songMix != null)
        {
            File songMixFile = fdm.getSongMixFile(songFile);
            resMix = songMix.saveToFileNotify(songMixFile, false) ? SAVE_CODE_OK : SAVE_CODE_ERROR_SONGMIX;
        } else
        {
            resMix = SAVE_CODE_ERROR_SONGMIX;
        }

        // Save song
        resSong = song.saveToFileNotify(songFile, false) ? SAVE_CODE_OK : SAVE_CODE_ERROR_SONGFILE;

        int res = (resSong != SAVE_CODE_OK) ? resSong : resMix;

        return res;
    }

    /**
     * Get the MidiMix object from the song.
     * <p>
     *
     * @param song
     * @return Can be null if problem
     */
    static public MidiMix getMidiMixSilent(Song song)
    {
        MidiMix midiMix = null;
        try
        {
            midiMix = MidiMixManager.getInstance().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            LOGGER.severe("getMidiMixSilent() Could not retrieve MidiMix for song " + song.getName() + " - ex=" + ex.getMessage());   //NOI18N
        }
        return midiMix;
    }

    /**
     * Get the file to be used for the specified song.
     * <p>
     * Handle cases when file is not set yet or file has no parent path.
     *
     * @param song
     * @return
     */
    public static File getValidatedSaveFile(Song song)
    {
        File defaultFile = song.getFile();
        if (defaultFile == null)
        {
            // This is the first save of the song, build the file name from song name + add extension if not already present
            String defaultName = song.getName().replace(" ", "");
            if (!org.jjazz.util.api.Utilities.endsWithIgnoreCase(defaultName, "." + FileDirectoryManager.SONG_EXTENSION))
            {
                defaultName += "." + FileDirectoryManager.SONG_EXTENSION;
            }
            defaultFile = new File(defaultName);
        }
        if (defaultFile.getParent() == null)
        {
            // If no parent directory, try to reuse the last used directory
            File parent = FileDirectoryManager.getInstance().getLastSongDirectory();
            if (parent != null && parent.isDirectory())
            {
                defaultFile = new File(parent, defaultFile.getName());
            }
        }
        return defaultFile;
    }
}
