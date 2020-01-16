/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.util.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * A central place to manage MidiSynths.
 */
public class MidiSynthManager
{

    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static final String MIDISYNTH_FILES_RESOURCE_ZIP = "resources/MidiSynthFiles.zip";
    private static MidiSynthManager INSTANCE;
    private File lastSynthDir;
    private List<WeakReference<MidiSynth>> midiSynthRefs = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(MidiSynthManager.class.getSimpleName());

    public static MidiSynthManager getInstance()
    {
        synchronized (MidiSynthManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MidiSynthManager();
            }
        }
        return INSTANCE;
    }

    private MidiSynthManager()
    {
    }

    /**
     * Search the standard synths and then through our active MidiSynth references to find a synth with synthName.
     *
     * @param synthName
     * @return Can be null.
     */
    public MidiSynth getMidiSynth(String synthName)
    {
        if (synthName == null)
        {
            throw new IllegalArgumentException("synthName=" + synthName);
        }
        if (StdSynth.getInstance().getName().equals(synthName))
        {
            return StdSynth.getInstance();
        }
        if (GSSynth.getInstance().getName().equals(synthName))
        {
            return GSSynth.getInstance();
        }
        for (WeakReference<MidiSynth> ref : getInstance().midiSynthRefs)
        {
            MidiSynth synth = ref.get();
            if (synth != null)
            {
                if (synth.getName().equals(synthName))
                {
                    return synth;
                }
            }
        }
        return null;
    }

    /**
     * Read the specified file to load one or more MidiSynths.
     * <p>
     * Errors are notified to user. A WeakReference of the loaded MidiSynths is kept.
     *
     * @param synthFile
     * @return A list of loaded MidiSynths. Can be empty. MidiSynths have their getFile() property set to synthFile.
     */
    public List<MidiSynth> loadSynths(File synthFile)
    {
        if (synthFile == null)
        {
            throw new NullPointerException("synthFile");
        }
        ArrayList<MidiSynth> res = new ArrayList<>();

        // Process file
        String ext = org.jjazz.util.Utilities.getExtension(synthFile.getName());
        MidiSynthFileReader reader = MidiSynthFileReader.Util.getReader(ext.toLowerCase());
        if (reader == null)
        {
            // Extension not managed by any MidiSynthFileReader
            String msg = "File extension not supported: " + synthFile.getAbsolutePath();
            LOGGER.log(Level.WARNING, msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        } else
        {
            // Ask provider to read the file and add the non-empty synths
            List<MidiSynth> synths = null;
            try
            {
                synths = reader.readSynthsFromStream(new FileInputStream(synthFile), synthFile); // Can raise exception
                for (MidiSynth synth : synths)
                {
                    if (synth.getNbPatches() > 0)
                    {
                        res.add(synth);
                        midiSynthRefs.add(new WeakReference<>(synth));
                    }
                }
            } catch (IOException ex)
            {
                String msg = "Problem reading file : " + ex.getLocalizedMessage();
                LOGGER.log(Level.WARNING, msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }
        }
        return res;
    }

    /**
     * Show a dialog to select a MidiSynth definition file.
     * <p>
     * Use the file extensions managed by the MidiSynthFileReaders found in the global lookup.
     *
     * @return The selected file. Null if user cancelled or no selection.
     */
    public File showSelectSynthFileDialog()
    {
        // Collect all file extensions managed by the MidiSynthFileReaders
        HashMap<String, MidiSynthFileReader> mapExtReader = new HashMap<>();
        List<FileNameExtensionFilter> allFilters = new ArrayList<>();
        for (MidiSynthFileReader r : Lookup.getDefault().lookupAll(MidiSynthFileReader.class))
        {
            List<FileNameExtensionFilter> filters = r.getSupportedFileTypes();
            for (FileNameExtensionFilter filter : filters)
            {
                allFilters.add(filter);
                for (String s : filter.getExtensions())
                {
                    mapExtReader.put(s.toLowerCase(), r);
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
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Load Midi synth definition file");
        chooser.setCurrentDirectory(getMidiSynthFilesDir());

        // Show dialog
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
        {
            // User cancelled
            return null;
        }

        File synthFile = chooser.getSelectedFile();
        lastSynthDir = synthFile.getParentFile();
        return synthFile;
    }

    @ServiceProvider(service = MidiSynth.Finder.class)
    static public class SynthFinder implements MidiSynth.Finder
    {
        @Override
        public MidiSynth getMidiSynth(String synthName, File synthFile)
        {
            if (synthName == null)
            {
                throw new IllegalArgumentException("synthName=" + synthName + " synthFile=" + synthFile);
            }
            // First search via the name
            MidiSynth res = getInstance().getMidiSynth(synthName);

            // Try to read the file if not null
            if (res == null && synthFile != null)
            {
                List<MidiSynth> synths = getInstance().loadSynths(synthFile);
                for (MidiSynth synth : synths)
                {
                    if (synth.getName().equals(synthName))
                    {
                        res = synth;
                        break;
                    }
                }
            }
            return res;
        }
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * The last used directory, or if not set the standard directory for MidiSynth.
     * <p>
     * Prepare the builtin files when first called.
     *
     * @return
     */
    private File getMidiSynthFilesDir()
    {
        if (lastSynthDir != null)
        {
            return lastSynthDir;
        }
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File rDir = fdm.getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
        assert rDir.isDirectory() : "rDir=" + rDir;
        File[] files = rDir.listFiles();
        if (files.length == 0)
        {
            files = copyBuiltinResourceFiles(rDir);
            LOGGER.info("getMidiSynthFilesDir() copied " + files.length + " files into " + rDir.getAbsolutePath());
        }
        return rDir;
    }

    /**
     * Copy the builtin Midi synth files within the JAR to destPath.
     * <p>
     *
     * @param destPath
     *
     */
    private File[] copyBuiltinResourceFiles(File destDir)
    {
        List<File> res = Utilities.extractZipResource(getClass(), MIDISYNTH_FILES_RESOURCE_ZIP, destDir.toPath());
        if (res.isEmpty())
        {
            LOGGER.warning("copyBuiltinResourceFiles() No synth definition files found in " + MIDISYNTH_FILES_RESOURCE_ZIP);
        }
        return res.toArray(new File[0]);
    }

}
