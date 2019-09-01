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
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import static org.jjazz.songeditormanager.Bundle.CTL_JJazzOpenSongs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.jjazz.ui.utilities.Utilities;
import org.openide.windows.WindowManager;

@ActionID(category = "File", id = "org.jjazz.songeditormanager.OpenSong")
@ActionRegistration(displayName = "#CTL_OpenSong", lazy = true, iconBase = "org/jjazz/songeditormanager/resources/OpenFile.png") // Will also automatically find OpenFile24.png 
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 5),
            @ActionReference(path = "Toolbars/File", position = 10),
            @ActionReference(path = "Shortcuts", name = "C-O")
        })
@Messages(
        {
            "CTL_OpenSong=Open Song...",
            "CTL_JJazzOpenSongs=JJazz Songs"
        })
public final class OpenSong implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(OpenSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JFileChooser chooser = Utilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(CTL_JJazzOpenSongs() + " (" + "." + FileDirectoryManager.SONG_EXTENSION + ")", FileDirectoryManager.SONG_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(FileDirectoryManager.getInstance().getLastSongDirectory());
        chooser.setDialogTitle(CTL_JJazzOpenSongs());
        chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());

        for (File songFile : chooser.getSelectedFiles())
        {
            SongEditorManager.getInstance().showSong(songFile);
        }
    }
}
