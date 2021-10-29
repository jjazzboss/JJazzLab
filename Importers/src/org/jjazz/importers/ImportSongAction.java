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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songeditormanager.api.SongEditorManager;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.progress.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * The import song action.
 * <p>
 */
@ActionID(category = "File", id = "org.jjazz.songeditormanager.ImportSong")
@ActionRegistration(displayName = "#CTL_ImportSong", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 10),
        })
public final class ImportSongAction implements ActionListener
{

    public static final String PREF_LAST_IMPORT_DIRECTORY = "LastImportDirectory";   //NOI18N 
    private static Preferences prefs = NbPreferences.forModule(ImportSongAction.class);
    private final ResourceBundle bundle = ResUtil.getBundle(getClass());
    private static final Logger LOGGER = Logger.getLogger(ImportSongAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final List<SongImporter> importers = getAllImporters();
        if (importers.isEmpty())
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(bundle.getString("ErrNoImporterFound"), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Prepare a special filter that shows all accepted extensions
        FileNameExtensionFilter allExtensionsFilter = null;
        HashSet<String> allExtensions = new HashSet<>();
        for (SongImporter importer : importers)
        {
            for (FileNameExtensionFilter filter : importer.getSupportedFileTypes())
            {
                Collections.addAll(allExtensions, filter.getExtensions());
            }
        }
        if (allExtensions.size() > 1)
        {
            allExtensionsFilter = new FileNameExtensionFilter(bundle.getString("ALL IMPORTABLE FILES"), allExtensions.toArray(new String[0]));
        }


        // Initialize the file chooser
        JFileChooser chooser = org.jjazz.ui.utilities.api.Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        if (allExtensionsFilter != null)
        {
            chooser.addChoosableFileFilter(allExtensionsFilter);
        }
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
        chooser.setDialogTitle(bundle.getString("IMPORT SONG FROM FILE"));
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
            String ext = org.jjazz.util.api.Utilities.getExtension(f.getAbsolutePath());
            SongImporter importer = mapExtImporter.get(ext);
            if (importer == null)
            {
                // No association yet, search the compatible importers
                List<SongImporter> fImporters = getMatchingImporters(importers, ext);
                if (fImporters.isEmpty())
                {
                    // Extension not managed by any SongImporter
                    String msg = ResUtil.getString(getClass(), "FILE TYPE IS NOT SUPPORTED", f.getAbsolutePath());
                    LOGGER.log(Level.WARNING, "actionPerformed() " + msg);   //NOI18N
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


        // Log event
        List<String> importerUniqueNames = mapFileImporter.values().stream().distinct().map(i -> i.getId()).collect(Collectors.toList());
        Analytics.logEvent("Import Song From File", Analytics.buildMap("Importers", importerUniqueNames));


        // Use a different thread because possible import of many files
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                importFiles(mapFileImporter);
            }
        };
        // new Thread(r).start();
        BaseProgressUtils.showProgressDialogAndRun(r, bundle.getString("IMPORTING..."));
    }

    private void importFiles(HashMap<File, SongImporter> mapFileImporter)
    {
        var songFiles = new ArrayList<>(mapFileImporter.keySet());
        for (File f : songFiles)
        {
            SongImporter importer = mapFileImporter.get(f);
            Song song = null;
            try
            {
                LOGGER.info("importFiles() -- importerId=" + importer.getId() + " Importing file " + f.getAbsolutePath());   //NOI18N
                song = importer.importFromFile(f);
            } catch (SongCreationException | IOException ex)
            {
                LOGGER.warning("importFiles() ex=" + ex.getMessage());   //NOI18N
                NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                continue;
            }

            if (song == null)
            {
                LOGGER.log(Level.WARNING, "importFiles() song=null, importer=" + importer.getId() + " f=" + f.getAbsolutePath());   //NOI18N
                NotifyDescriptor nd = new NotifyDescriptor.Message(bundle.getString("ERR_UnexpectedError"), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            } else
            {
                // Ok we got the new song show it !
                song.setFile(null);     // Make sure song is not associated with the import file
                SongFactory.getInstance().registerSong(song);

                boolean last = (f == songFiles.get(songFiles.size() - 1));
                SongEditorManager.getInstance().showSong(song, last);
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

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);
        }

    }

}
