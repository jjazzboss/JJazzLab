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
package org.jjazz.ui.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.song.api.Song;
import org.jjazz.ui.musiccontrolactions.api.SongExportSupport;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/**
 * Export song to a midi file.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.exporttomidifile")
@ActionRegistration(displayName = "#CTL_ExportToMidiFile", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1580, separatorAfter = 1590)
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
        File midiFile = getMidiFile(song);
        if (midiFile == null)
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantBuildMidiFile", song.getName());
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }
        JFileChooser chooser = Utilities.getFileChooserInstance();
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


        SongExportSupport.songToMidiFile(song, midiFile);

    }


    // ======================================================================
    // Private methods
    // ======================================================================   
    /**
     *
     * @param sg
     * @return Can be null
     */
    private File getMidiFile(Song sg)
    {
        File f = null;
        File songFile = sg.getFile();
        String midiFilename = (songFile == null) ? sg.getName() + ".mid" : org.jjazz.util.api.Utilities.replaceExtension(songFile.getName(), ".mid");
        if (saveExportDir != null && !saveExportDir.isDirectory())
        {
            saveExportDir = null;
        }
        File dir = saveExportDir;
        if (dir == null)
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
        if (dir != null)
        {
            f = new File(dir, midiFilename);
        }
        return f;
    }

  
}
