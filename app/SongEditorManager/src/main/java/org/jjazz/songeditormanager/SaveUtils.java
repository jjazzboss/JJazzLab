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

import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Convenience methods for the package
 */
class SaveUtils
{

    public static final int SAVE_CODE_OK = 0;
    public static final int SAVE_CODE_CANCEL = 1;
    public static final int SAVE_CODE_ERROR_SONGFILE = 2;
    public static final int SAVE_CODE_ERROR_SONGMIX = 3;

    private static final Logger LOGGER = Logger.getLogger(SaveUtils.class.getName());

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
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JJazzLab songs" + " (" + "." + Song.SONG_EXTENSION + ")", Song.SONG_EXTENSION);
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
            if (!org.jjazz.utilities.api.Utilities.endsWithIgnoreCase(songName, "." + Song.SONG_EXTENSION))
            {
                songFile = new File(songFile.getParent(), songName + "." + Song.SONG_EXTENSION);
            }

            if (songFile.exists())
            {
                // Confirm overwrite
                NotifyDescriptor nd = new NotifyDescriptor.Confirmation(songFile.getName() + " - " + "Confirm overwrite ?",
                        NotifyDescriptor.OK_CANCEL_OPTION);
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
     * @param songFile
     * @return SAVE_CODE_OK, SAVE_CODE_ERROR_SONGMIX or SAVE_CODE_ERROR_SONGFILE
     */
    static public int saveSongAndMix(Song song, File songFile)
    {
        if (song == null || songFile == null)
        {
            throw new IllegalArgumentException("song=" + song + " songFile=" + songFile);
        }
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        int resSong;
        int resMix;

        // Save Mix
        MidiMix songMix = MidiMixManager.getDefault().findExistingMix(song);
        if (songMix != null)
        {
            File songMixFile = MidiMix.getSongMixFile(songFile);
            resMix = songMix.saveToFileNotify(songMixFile, false) ? SAVE_CODE_OK : SAVE_CODE_ERROR_SONGMIX;
        } else
        {
            resMix = SAVE_CODE_ERROR_SONGMIX;
        }

        // Save song
        resSong = song.saveToFileNotify(songFile, false) ? SAVE_CODE_OK : SAVE_CODE_ERROR_SONGFILE;

        if (resSong == SAVE_CODE_OK)
        {
            doAnalytics(song);
        }

        int res = (resSong != SAVE_CODE_OK) ? resSong : resMix;


        return res;
    }

    /**
     * Save the song and the related MidiFile in a user-chosen file.
     *
     * @param song
     * @return
     */
    static public int SaveAs(Song song)
    {
        File songFile = getValidatedSaveFile(song);
        songFile = showSaveSongFileChooser(songFile);
        if (songFile == null)
        {
            return SAVE_CODE_CANCEL;
        }

        // Save song and related mix
        int res = saveSongAndMix(song, songFile);
        return res;
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
            if (!org.jjazz.utilities.api.Utilities.endsWithIgnoreCase(defaultName, "." + Song.SONG_EXTENSION))
            {
                defaultName += "." + Song.SONG_EXTENSION;
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


    /**
     * Compute some anonymous stats about feature usage.
     *
     * @param song
     */
    static private void doAnalytics(Song song)
    {
        var cls = song.getChordLeadSheet();
        var ss = song.getSongStructure();
        var ecss = cls.getItems(CLI_ChordSymbol.class)
                .stream()
                .map(cli -> cli.getData())
                .toList();
        var cris = ecss.stream().map(ecs -> ecs.getRenderingInfo()).collect(Collectors.toList());

        HashMap<String, Object> map = new HashMap<>();
        map.put("Memo Char Size", song.getComments().length());
        map.put("Nb Chord Symbols", cris.stream().count());
        map.put("Nb Song Parts", ss.getSongParts().size());
        map.put("LeadSheet Bar Size", cls.getSizeInBars());
        map.put("Song Structure Bar Size", ss.getSizeInBars());
        map.put("Use Bass Pedal Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.PEDAL_BASS)));
        map.put("Use Accent Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.ACCENT)));
        map.put("Use Stronger Accent Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(
                ChordRenderingInfo.Feature.ACCENT_STRONGER)));
        map.put("Use Crash Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.CRASH)));
        map.put("Use No Crash Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.NO_CRASH)));
        map.put("Use Extended Hold/Shot Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(
                ChordRenderingInfo.Feature.EXTENDED_HOLD_SHOT)));
        map.put("Use Shot Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.SHOT)));
        map.put("Use Hold Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.HOLD)));
        map.put("Use Scale Chord", cris.stream().anyMatch(cri -> cri.getScaleInstance() != null));
        map.put("Use Substitute Chord", ecss.stream().anyMatch(ecs -> ecs.getAlternateChordSymbol() != null));


        Analytics.logEvent("Save Song", map);
        Analytics.incrementProperties("Nb Save Song", 1);
        Analytics.setPropertiesOnce(Analytics.buildMap("First Save", Analytics.toStdDateTimeString()));
    }
}
