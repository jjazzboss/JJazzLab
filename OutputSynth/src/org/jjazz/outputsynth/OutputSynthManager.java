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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.StdSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.util.Utilities;
import org.openide.*;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * Management of the OutputSynth.
 * <p>
 */
public class OutputSynthManager
{

    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static final String MIDISYNTH_FILES_RESOURCE_ZIP = "resources/MidiSynthFiles.zip";
    private final static String SGM_SOUNDFONT_INS = "resources/SGM-v2.01.ins";

    private static OutputSynthManager INSTANCE;
    private OutputSynth outputSynth;

    private File lastSynthDir;

    private static final Logger LOGGER = Logger.getLogger(OutputSynthManager.class.getSimpleName());

    public static OutputSynthManager getInstance()
    {
        synchronized (OutputSynthManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OutputSynthManager();
            }
        }
        return INSTANCE;
    }

    private OutputSynthManager()
    {
        outputSynth = new OutputSynth();
        outputSynth.getGMRemapTable().setDrumsInstrument(new Instrument(20, "MyDrums"));
        outputSynth.getGMRemapTable().setPercussionInstrument(StdSynth.getGM2Bank().getDefaultDrumsInstrument());
        outputSynth.getGMRemapTable().setInstrument(StdSynth.getGM1Bank().getInstrument(17), StdSynth.getXGBank().getInstrument(23), true);
        outputSynth.getGMRemapTable().setInstrument(StdSynth.getGM1Bank().getInstrument(30), StdSynth.getXGBank().getInstrument(90), false);
    }

    /**
     * The current OutputSynth.
     *
     * @return Can't be null.
     */
    public OutputSynth getOutputSynth()
    {
        return outputSynth;
    }

    public void setOutputSynth(OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth);
        }
        outputSynth = outSynth;
    }

    /**
     * Show the AddCustomDialog synth dialog.
     * <p>
     * Errors are notified to user.
     *
     * @return The loaded MidiSynths or an empty list if user cancelled the dialog or other error.
     */
    public List<MidiSynth> showAddCustomSynthDialog()
    {
        ArrayList<MidiSynth> res = new ArrayList<>();

        // First collect all file extensions managed by the MidiSynthFileReaders
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
            return res;
        }

        File synthFile = chooser.getSelectedFile();
        lastSynthDir = synthFile.getParentFile();

        // Process file
        String ext = org.jjazz.util.Utilities.getExtension(synthFile.getAbsolutePath());
        MidiSynthFileReader r = mapExtReader.get(ext.toLowerCase());
        if (r == null)
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
                synths = r.readSynthsFromStream(new FileInputStream(synthFile), synthFile); // Can raise exception
                for (MidiSynth synth : synths)
                {
                    if (synth.getNbPatches() > 0)
                    {
                        res.add(synth);
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
