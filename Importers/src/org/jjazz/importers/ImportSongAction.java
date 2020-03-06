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
package org.jjazz.importers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songeditormanager.SongEditorManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * The import song action.
 * <p>
 */
@ActionID(category = "File", id = "org.jjazz.songeditormanager.ImportSong")
@ActionRegistration(displayName = "#CTL_ImportSong", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1600, separatorBefore = 1590),
        })
@Messages(
        {
            "CTL_ImportSong=Import Songs..."
        })
public final class ImportSongAction implements ActionListener
{

    public static final String PREF_LAST_IMPORT_DIRECTORY = "LastImportDirectory";
    private static Preferences prefs = NbPreferences.forModule(ImportSongAction.class);
    private static final Logger LOGGER = Logger.getLogger(ImportSongAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final List<SongImporter> importers = getAllImporters();
        if (importers.isEmpty())
        {
            NotifyDescriptor d = new NotifyDescriptor.Message("Can't import song : no importer found on the system.", NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        // Initialize the file chooser
        JFileChooser chooser = org.jjazz.ui.utilities.Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        for (SongImporter importer : importers)
        {
            List<FileNameExtensionFilter> filters = importer.getSupportedFileTypes();
            for (FileNameExtensionFilter filter : filters)
            {
                chooser.addChoosableFileFilter(filter);
            }
        }
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(true);
        chooser.setSelectedFile(new File(""));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setCurrentDirectory(getLastImportDirectory());
        chooser.setDialogTitle("Import song from file");
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
        {
            // User cancelled
            return;
        }

        // Save directory for future imports
        final File[] files = chooser.getSelectedFiles();
        if (files.length > 0)
        {
            File dir = files[0].getParentFile();
            if (dir != null)
            {
                prefs.put(PREF_LAST_IMPORT_DIRECTORY, dir.getAbsolutePath());
            }
        }

        // Prepare data
        final HashMap<File, SongImporter> mapFileImporter = new HashMap<>();
        HashMap<String, SongImporter> mapExtImporter = new HashMap<>();
        for (File f : files)
        {
            String ext = org.jjazz.util.Utilities.getExtension(f.getAbsolutePath());
            SongImporter importer = mapExtImporter.get(ext);
            if (importer == null)
            {
                // No association yet, search the compatible importers
                List<SongImporter> fImporters = getMatchingImporters(importers, ext);
                if (fImporters.isEmpty())
                {
                    // Extension not managed by any SongImporter
                    String msg = "File type is not supported : " + f.getAbsolutePath();
                    LOGGER.log(Level.WARNING, "actionPerformed() " + msg);
                    NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                    return;
                } else if (fImporters.size() > 1)
                {
                    // Ask user to choose the provider
                    ChooseImporterDialog dlg = new ChooseImporterDialog(WindowManager.getDefault().getMainWindow(), true);
                    dlg.preset(ext, fImporters);
                    dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                    dlg.setVisible(true);
                    importer = dlg.getSelectedImporter();
                    if (importer == null)
                    {
                        return;
                    }
                } else
                {
                    // Easy only one provider
                    importer = fImporters.get(0);
                }
            }
            // Save the association
            mapFileImporter.put(f, importer);
            mapExtImporter.put(ext, importer);
        }

        // Use a different thread because possible import of many files
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                importFiles(mapFileImporter);
            }
        };
        new Thread(r).start();
    }

    private void importFiles(HashMap<File, SongImporter> mapFileImporter)
    {
        for (File f : mapFileImporter.keySet())
        {
            SongImporter importer = mapFileImporter.get(f);
            Song song = null;
            try
            {
                song = importer.importFromFile(f);
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "actionPerformed() importer=" + importer.getId() + ", ex=" + ex.getLocalizedMessage());
                NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                continue;
            }

            if (song == null)
            {
                LOGGER.log(Level.WARNING, "actionPerformed() song=null, importer=" + importer.getId() + " f=" + f.getAbsolutePath());
                NotifyDescriptor nd = new NotifyDescriptor.Message("An unexpected problem occured", NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            } else
            {
                // Ok we got the new song show it !
                song.setFile(null);     // Make sure song is not associated with the import file
                SongFactory.getInstance().registerSong(song);
                SongEditorManager.getInstance().showSong(song);
            }
        }
    }

    // ================================================================================================
    // Private methods
    // ================================================================================================
    private List<SongImporter> getAllImporters()
    {
        ArrayList<SongImporter> providers = new ArrayList<>();
        for (SongImporter p : Lookup.getDefault().lookupAll(SongImporter.class))
        {
            providers.add(p);
        }
        return providers;
    }

    /**
     * Select the importers which accept fileExtesion.
     *
     * @param importers
     * @param fileExtension
     * @return
     */
    private List<SongImporter> getMatchingImporters(List<SongImporter> importers, String fileExtension)
    {
        ArrayList<SongImporter> res = new ArrayList<>();
        for (SongImporter importer : importers)
        {
            for (FileNameExtensionFilter f : importer.getSupportedFileTypes())
            {
                for (String ext : f.getExtensions())
                {
                    if (ext.toLowerCase().equals(fileExtension.toLowerCase()))
                    {
                        if (!res.contains(importer))
                        {
                            res.add(importer);
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     *
     * @return Can be null.
     */
    private File getLastImportDirectory()
    {
        String s = prefs.get(PREF_LAST_IMPORT_DIRECTORY, null);
        if (s == null)
        {
            return null;
        }
        File f = new File(s);
        if (!f.isDirectory())
        {
            f = null;
        }
        return f;
    }

}
