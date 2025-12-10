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
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordleadsheet.api.ClsUtilities;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.progress.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * The import song action.
 * <p>
 * If System property SYSTEM_PROP_SKIP_OPENING is defined, then import is done but the imported song is not shown in an editor (testing purpose only).
 */
@ActionID(category = "File", id = "org.jjazz.songeditormanager.ImportSong")
@ActionRegistration(displayName = "#CTL_ImportSong", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 10),
        })
public final class ImportSong implements ActionListener
{

    public static final String SYSTEM_PROP_SKIP_OPENING = "importSongSkipOpening";
    public static final String PREF_LAST_IMPORT_DIRECTORY = "LastImportDirectory";
    private static JFileChooser FILE_CHOOSER;
    private static AccessoryComponent accessoryComponent;
    private static final Preferences prefs = NbPreferences.forModule(ImportSong.class);
    private static final Logger LOGGER = Logger.getLogger(ImportSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final List<SongImporter> importers = SongImporter.getImporters();
        if (importers.isEmpty())
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ResUtil.getString(getClass(), "ErrNoImporterFound"), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Prepare a special filter that shows all accepted extensions
        var allExtensions = SongImporter.getAllSupportedFileExtensions();
        FileNameExtensionFilter allExtensionsFilter = allExtensions.isEmpty() ? null
                : new FileNameExtensionFilter(ResUtil.getString(getClass(), "ALL_IMPORTABLE_FILES"), allExtensions.toArray(new String[0]));


        // Initialize the file chooser
        JFileChooser chooser = getFileChooser();


        // Update file filters each time, in case a new import plugin was added at runtime
        chooser.resetChoosableFileFilters();
        chooser.setAcceptAllFileFilterUsed(false);
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
            String ext = org.jjazz.utilities.api.Utilities.getExtension(f.getAbsolutePath());
            SongImporter importer = mapExtImporter.get(ext);
            if (importer == null)
            {
                // No association yet, search the compatible importers
                List<SongImporter> fImporters = SongImporter.getMatchingImporters(importers, ext);
                if (fImporters.isEmpty())
                {
                    // Extension not managed by any SongImporter
                    String msg = ResUtil.getString(getClass(), "FILE_TYPE_IS_NOT_SUPPORTED", f.getAbsolutePath());
                    LOGGER.log(Level.WARNING, "actionPerformed() {0}", msg);
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

        boolean batchConvertMode = accessoryComponent.cb_batchConvertMode.isSelected();

        // Log event
        List<String> importerUniqueNames = mapFileImporter.values().stream().distinct().map(i -> i.getId()).collect(Collectors.toList());
        Analytics.logEvent("Import Song From File", Analytics.buildMap("Importers", importerUniqueNames, "BatchConvertMode", batchConvertMode));


        // Use a different thread because possible import of many files
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                importFiles(mapFileImporter, batchConvertMode);
            }
        };
        // new Thread(r).start();
        BaseProgressUtils.showProgressDialogAndRun(r, ResUtil.getString(getClass(), "IMPORTING"));
    }

    private void importFiles(HashMap<File, SongImporter> mapFileImporter, boolean batchConvertMode)
    {
        List<String> errorFilenames = new ArrayList<>();          // only used in batch convert mode
        var songFiles = new ArrayList<>(mapFileImporter.keySet());
        songFiles.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));

        if (batchConvertMode)
        {
            LOGGER.info("importFiles() -- Running in batch convert mode");
        }

        for (File f : songFiles)
        {
            String filename = f.getName();
            SongImporter importer = mapFileImporter.get(f);
            Song song = null;
            try
            {
                LOGGER.log(Level.INFO, "importFiles() Importing file {0}  importerId={1}", new Object[]
                {
                    f.getAbsolutePath(),
                    importer.getId()

                });
                song = importer.importFromFile(f);
                ClsUtilities.removeRedundantStandardChords(song.getChordLeadSheet());                                
            } catch (SongCreationException | IOException ex)
            {
                LOGGER.log(Level.WARNING, "importFiles() error: {0}", ex.getMessage());
                if (!batchConvertMode)
                {
                    NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                } else
                {
                    errorFilenames.add(filename);
                }
                continue;
            }

            if (song == null)
            {
                LOGGER.log(Level.WARNING, "importFiles() unexpected song=null");
                if (!batchConvertMode)
                {
                    NotifyDescriptor nd = new NotifyDescriptor.Message(ResUtil.getString(getClass(), "ERR_UnexpectedError"), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                } else if (!errorFilenames.contains(filename))
                {
                    errorFilenames.add(filename);
                }

            } else if (!batchConvertMode)
            {
                // Ok we got the new song show it !
                song.setFile(null);     // Make sure song is not associated with the import file
                SongFactory.getInstance().registerSong(song);

                boolean last = (f == songFiles.get(songFiles.size() - 1));
                SongEditorManager.getDefault().showSong(song, last, true);
            } else
            {
                // Save the imported song in the same directory
                File saveFile = new File(f.getParentFile(), Utilities.replaceExtension(filename, "sng"));
                try
                {
                    song.saveToFile(saveFile, true);
                    LOGGER.log(Level.INFO, "importFiles() saved to {0}", saveFile.getName());
                } catch (IOException ex)
                {
                    LOGGER.log(Level.WARNING, "importFiles() error: {0}", ex.getMessage());
                    errorFilenames.add(saveFile.getName());
                    if (saveFile.exists() && !saveFile.delete())
                    {
                        LOGGER.log(Level.WARNING, "importFiles() could not delete {0}. ex={1}", new Object[]
                        {
                            saveFile.getAbsolutePath(), ex.getMessage()
                        });
                    }
                }
            }
        }

        if (batchConvertMode && !songFiles.isEmpty())
        {
            int nbErrors = errorFilenames.size();
            int nbOk = songFiles.size() - nbErrors;
            String errorStr = nbErrors == 0 ? "" : errorFilenames.toString();
            LOGGER.log(Level.INFO, "importFiles() batch mode convert complete: {0} file(s) successfully converted, {1} error(s) {2} ", new Object[]
            {
                nbOk, nbErrors, errorStr
            });
            String msg = ResUtil.getString(getClass(), "BatchModeImportComplete", nbOk, nbErrors);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        }

    }

    // ================================================================================================
    // Private methods
    // ================================================================================================

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


    /**
     * Get the file chooser with the batch mode convert button.
     *
     * @return
     */
    private JFileChooser getFileChooser()
    {
        if (FILE_CHOOSER != null)
        {
            return FILE_CHOOSER;
        }

        // Prepare FileChooser
        FILE_CHOOSER = new JFileChooser();
        FILE_CHOOSER.setMultiSelectionEnabled(true);
        FILE_CHOOSER.setSelectedFile(new File(""));
        FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FILE_CHOOSER.setCurrentDirectory(getLastImportDirectory());
        FILE_CHOOSER.setDialogTitle(ResUtil.getString(getClass(), "IMPORT_SONG_FROM_FILE"));


        // Add the accessory component
        accessoryComponent = new AccessoryComponent();
        FILE_CHOOSER.setAccessory(accessoryComponent);

        return FILE_CHOOSER;
    }


    /**
     * An accessory component to the FileChooser to indicate that rhythms should be added for this session only.
     */
    private static class AccessoryComponent extends JPanel
    {

        private final JCheckBox cb_batchConvertMode = new JCheckBox(ResUtil.getString(getClass(), "ImportBatchConvertMode"));

        public AccessoryComponent()
        {
            cb_batchConvertMode.setToolTipText(ResUtil.getString(getClass(), "ImportBatchConvertModeTooltip"));
            add(cb_batchConvertMode);
        }
    }

}
