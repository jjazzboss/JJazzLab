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
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.musiccontrol.api.SongMidiExporter;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.song.api.Song;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Export song to a midi file.
 */
@ActionID(category = "File", id = "org.jjazz.musiccontrolactions.exporttomidifile")
@ActionRegistration(displayName = "#CTL_ExportToMidiFile", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1580)
        })
public class ExportToMidiFile extends AbstractAction
{

    private final Song song;
    private static File saveExportDir = null;

    private static final Logger LOGGER = Logger.getLogger(ExportToMidiFile.class.getSimpleName());

    public ExportToMidiFile(Song context)
    {
        song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;


        if (song.getSongStructure().getSongParts().isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantExportEmptySong");
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        // Get the target midi file
        File midiFile = getExportFile(song, ".mid", saveExportDir);
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(midiFile);
        chooser.setDialogTitle(ResUtil.getString(getClass(), "CTL_ExportToMidiDialogTitle"));
        int res = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (res != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        midiFile = chooser.getSelectedFile();
        saveExportDir = midiFile.getParentFile();


        if (midiFile.exists())
        {
            // File overwrite confirm dialog
            String msg = ResUtil.getString(getClass(), "CTL_ConfirmFileOverwrite", midiFile);
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(nd);
            if (result != NotifyDescriptor.OK_OPTION)
            {
                return;
            }
        }


        // Log event
        Analytics.logEvent("Export Midi");


        MidiMix midiMix = null;
        try
        {
            midiMix = MidiMixManager.getDefault().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            return;
        }

        assert midiMix != null : "song=" + song;


        SongMidiExporter.songToMidiFile(song, midiMix, midiFile, null);      // It notifies user if problem occurs

    }


    // ======================================================================
    // Private methods
    // ======================================================================   
    /**
     * Get the export File for the specified song.
     *
     * @param sg
     * @param ext              ".mp3", ".mid"...
     * @param defaultExportDir Can be null
     * @return Can be null
     */
    static protected File getExportFile(Song sg, String ext, File defaultExportDir)
    {
        File f;
        File songFile = sg.getFile();
        String fileName = (songFile == null)
                ? sg.getName().replaceAll(" ", "_") + ext
                : Utilities.replaceExtension(songFile.getName(), ext);


        File dir = defaultExportDir;
        if (dir != null && !dir.isDirectory())
        {
            if (songFile != null)
            {
                dir = songFile.getParentFile();         // Can be null
            }
            if (dir == null)
            {
                FileDirectoryManager fdm = FileDirectoryManager.getInstance();
                dir = fdm.getLastSongDirectory();       // Can be null                       
            }
        }

        f = new File(dir, fileName);
        return f;
    }


}
