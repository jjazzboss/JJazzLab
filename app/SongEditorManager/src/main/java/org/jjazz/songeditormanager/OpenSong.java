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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

@ActionID(category = "File", id = "org.jjazz.songeditormanager.OpenSong")
@ActionRegistration(displayName = "#CTL_OpenSong", lazy = true, iconBase = "org/jjazz/songeditormanager/resources/OpenFile.png") // Will also automatically find OpenFile24.png 
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 5),
            @ActionReference(path = "Toolbars/File", position = 10),
            @ActionReference(path = "Shortcuts", name = "D-O")
        })
public final class OpenSong implements ActionListener
{

    private static final String PREF_OPEN_DIALOG_WIDTH = "OpenDialogWidth";
    private static final String PREF_OPEN_DIALOG_HEIGHT = "OpenDialogHeight";
    private static final Preferences prefs = NbPreferences.forModule(OpenSong.class);
    private static final Logger LOGGER = Logger.getLogger(OpenSong.class.getSimpleName());


    @Override
    public void actionPerformed(ActionEvent e)
    {
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                ResUtil.getString(getClass(), "CTL_JJazzOpenSongs") + " (" + "." + Song.SONG_EXTENSION + ")",
                 Song.SONG_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(FileDirectoryManager.getInstance().getLastSongDirectory());
        chooser.setSelectedFile(new File(""));
        chooser.setDialogTitle(ResUtil.getString(getClass(), "CTL_OpenSongFromFile"));
        chooser.setPreferredSize(restorePrefSize());
        chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        storePrefSize(chooser.getSize());
        chooser.setPreferredSize(null);

        var songFiles = chooser.getSelectedFiles();
        for (File songFile : songFiles)
        {
            boolean last = (songFile == songFiles[songFiles.length - 1]);
            LOGGER.log(Level.INFO, "actionPerformed() Loading song {0}", songFile.getAbsolutePath());
            openSong(songFile, last, true);
        }

    }

    /**
     * Calls SongEditorManager.showSong() and notifies user if problem.
     * <p>
     *
     * @param songFile
     * @param makeActive
     * @param updateLastSongDir
     * @return False if file could not be read.
     */
    static protected boolean openSong(File songFile, boolean makeActive, boolean updateLastSongDir)
    {
        boolean b = true;
        try
        {
            // Show the song in the editors
            SongEditorManager.getDefault().showSong(songFile, makeActive, updateLastSongDir);

            Analytics.logEvent("Open Song");
            Analytics.incrementProperties("Nb Open Song", 1);

        } catch (SongCreationException ex)
        {
            String msg = ResUtil.getString(OpenSong.class, "ERR_CantOpenSongFile", songFile.getAbsolutePath(), ex.getLocalizedMessage());
            LOGGER.log(Level.WARNING, "openSong() {0}", msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            b = false;
        }

        return b;
    }

    private void storePrefSize(Dimension d)
    {
        prefs.putInt(PREF_OPEN_DIALOG_WIDTH, d.width);
        prefs.putInt(PREF_OPEN_DIALOG_HEIGHT, d.height);
    }

    private Dimension restorePrefSize()
    {
        int w = prefs.getInt(PREF_OPEN_DIALOG_WIDTH, 0);
        int h = prefs.getInt(PREF_OPEN_DIALOG_HEIGHT, 0);
        return w < 100 || h < 100 ? null : new Dimension(w, h);
    }


}
