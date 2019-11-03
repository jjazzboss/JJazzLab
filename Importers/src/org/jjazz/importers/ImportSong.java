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
 * 
 * @todo If several SongImporter.PostProcessor instances are available in the lookup, the UI should ask the user which one should be used.
 * 
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
public final class ImportSong implements ActionListener
{

    public static final String PREF_LAST_IMPORT_DIRECTORY = "LastImportDirectory";
    private static Preferences prefs = NbPreferences.forModule(ImportSong.class);
    private static final Logger LOGGER = Logger.getLogger(ImportSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        List<SongImporter> importers = getProviders();
        if (importers.isEmpty())
        {
            NotifyDescriptor d = new NotifyDescriptor.Message("Can't import song : no importer found on the system.", NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        // Collect supported file extensions
        // First collect all file extensions managed by the SongImporters
        final HashMap<String, SongImporter> mapExtImporter = new HashMap<>();
        List<FileNameExtensionFilter> allFilters = new ArrayList<>();
        for (SongImporter p : Lookup.getDefault().lookupAll(SongImporter.class))
        {
            List<FileNameExtensionFilter> filters = p.getSupportedFileTypes();
            for (FileNameExtensionFilter filter : filters)
            {
                allFilters.add(filter);
                for (String s : filter.getExtensions())
                {
                    mapExtImporter.put(s.toLowerCase(), p);
                }
            }
        }

        // Initialize the file chooser
        JFileChooser chooser = org.jjazz.ui.utilities.Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        for (FileNameExtensionFilter filter : allFilters)
        {
            chooser.addChoosableFileFilter(filter);
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

        // Use thread because possible import of many files
        final File[] files = chooser.getSelectedFiles();
        if (files.length > 0)
        {
            File dir = files[0].getParentFile();
            if (dir != null)
            {
                prefs.put(PREF_LAST_IMPORT_DIRECTORY, dir.getAbsolutePath());
            }
        }
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                importFiles(files, mapExtImporter);
            }
        };
        new Thread(r).start();
    }

    private void importFiles(File[] files, HashMap<String, SongImporter> mapExtImporter)
    {
        for (File f : files)
        {
            String ext = org.jjazz.util.Utilities.getExtension(f.getAbsolutePath());
            SongImporter sp = mapExtImporter.get(ext.toLowerCase());
            if (sp == null)
            {
                // Extension not managed by any SongImporter
                String msg = "File type is not supported : " + f.getAbsolutePath();
                LOGGER.log(Level.WARNING, "actionPerformed() " + msg + ", supportedFileExtensions=" + mapExtImporter.keySet());
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                continue;
            }

            Song song = null;
            try
            {
                song = sp.importFromFile(f);
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "actionPerformed() sp=" + sp.getId() + ", ex=" + ex.getLocalizedMessage());
                NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                continue;
            }

            if (song == null)
            {
                LOGGER.log(Level.WARNING, "actionPerformed() song=null, sp=" + sp.getId() + " f=" + f.getAbsolutePath());
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
    private List<SongImporter> getProviders()
    {
        ArrayList<SongImporter> providers = new ArrayList<>();
        for (SongImporter p : Lookup.getDefault().lookupAll(SongImporter.class))
        {
            providers.add(p);
        }
        return providers;
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
