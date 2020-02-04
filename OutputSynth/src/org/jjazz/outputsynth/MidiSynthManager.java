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
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.util.Utilities;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
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
    private static final String JJAZZLAB_SOUNDFONT_SYNTH_PATH = "resources/JJazzLabSoundFontSynth.ins";
    private static MidiSynth JJAZZLAB_SOUNDFONT_SYNTH = null;
    private static final String YAMAHA_REF_SYNTH_PATH = "resources/YamahaRefSynth.ins";
    private static MidiSynth YAMAHA_REF_SYNTH = null;

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
        // Copy the default MidiSynths if not done
        final File dir = getMidiSynthFilesDir();
        final File[] files = dir.listFiles();
        if (files.length == 0)
        {
            Runnable run = new Runnable()
            {
                @Override
                public void run()
                {
                    File[] res = MidiSynthManager.this.copyMidiSynthFilesFromZipResource(dir);
                    LOGGER.info("MidiSynthManager() First time initialization - copied " + res.length + " files into " + dir.getAbsolutePath());
                }
            };
            BaseProgressUtils.showProgressDialogAndRun(run, "First time initialization - copying builtin Midi synth files...");
        }
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
                    if (synth.getNbInstruments() > 0)
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

        /**
         * Search the MidiSynthManager instance.
         *
         * @param synthName
         * @param synthFile If no parent directory, search the MidiSynthManager default directory for output synth config files.
         * @return
         */
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
                if (synthFile.getParentFile() == null)
                {
                    // If no parent file search the default dir
                    File dir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
                    synthFile = new File(dir, synthFile.getName());
                }
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

    /**
     * The synth associated to the JJazzLab soundfont.
     *
     * @return
     */
    public MidiSynth getJJazzLabSoundFontSynth()
    {
        if (JJAZZLAB_SOUNDFONT_SYNTH == null)
        {
            // Read the synth from the .ins file
            InputStream is = getClass().getResourceAsStream(JJAZZLAB_SOUNDFONT_SYNTH_PATH);
            assert is != null : "JJAZZLAB_SOUNDFONT_SYNTH_PATH=" + JJAZZLAB_SOUNDFONT_SYNTH_PATH;
            MidiSynthFileReader r = MidiSynthFileReader.Util.getReader("ins");
            assert r != null;
            try
            {
                List<MidiSynth> synths = r.readSynthsFromStream(is, null);
                assert synths.size() == 1;
                JJAZZLAB_SOUNDFONT_SYNTH = synths.get(0);
            } catch (IOException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
        return JJAZZLAB_SOUNDFONT_SYNTH;
    }

    /**
     * The Yamaha Tyros/PSR reference synth which tries to contain most of the Yamaha instruments used in Yamaha styles.
     *
     * @return
     */
    public MidiSynth getYamahaRefSynth()
    {
        if (YAMAHA_REF_SYNTH == null)
        {
            // Read the synth from the .ins file
            InputStream is = getClass().getResourceAsStream(YAMAHA_REF_SYNTH_PATH);
            assert is != null : "TYROS_PSR_SYNTH=" + YAMAHA_REF_SYNTH_PATH;
            MidiSynthFileReader r = MidiSynthFileReader.Util.getReader("ins");
            assert r != null;
            try
            {
                List<MidiSynth> synths = r.readSynthsFromStream(is, null);
                assert synths.size() == 1;
                YAMAHA_REF_SYNTH = synths.get(0);
            } catch (IOException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
        return YAMAHA_REF_SYNTH;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * The last used directory, or if not set the standard directory for MidiSynth.
     * <p>
     *
     * @return
     */
    private File getMidiSynthFilesDir()
    {
        if (lastSynthDir != null)
        {
            return lastSynthDir;
        }
        File rDir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
        if (rDir == null)
        {
            String msg = "SERIOUS ERROR - Can't find the app. config. directory for " + MIDISYNTH_FILES_DEST_DIRNAME;
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
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
    private File[] copyMidiSynthFilesFromZipResource(File destDir)
    {
        List<File> res = Utilities.extractZipResource(getClass(), MIDISYNTH_FILES_RESOURCE_ZIP, destDir.toPath());
        if (res.isEmpty())
        {
            LOGGER.warning("copyBuiltinResourceFiles() No synth definition files found in " + MIDISYNTH_FILES_RESOURCE_ZIP);
        }
        return res.toArray(new File[0]);
    }

}
