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
package org.jjazz.midisynthmanager;

import org.jjazz.midisynthmanager.api.MidiSynthManager;
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
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.jjazz.util.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.*;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;


public class MidiSynthManagerImpl implements MidiSynthManager
{

    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GM2.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GS.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_XG.ins";
    @StaticResource(relative = true)
    private final static String YAMAHA_REF_SYNTH_PATH = "resources/YamahaRefSynth.ins";
    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static MidiSynthManagerImpl INSTANCE;


    private File lastSynthDir;
    private final List<MidiSynth> builtinSynths = new ArrayList<>();
    private final List<WeakReference<MidiSynth>> midiSynthRefs = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(MidiSynthManagerImpl.class.getSimpleName());


    public static MidiSynthManagerImpl getInstance()
    {
        synchronized (UpgradeManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MidiSynthManagerImpl();
            }
        }
        return INSTANCE;
    }

    private MidiSynthManagerImpl()
    {
        // LOGGER.severe("MidiSynthManagerImpl() --");
        // Read the builtin synths        
        builtinSynths.add(readOneResourceSynth(JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH));
        builtinSynths.add(readOneResourceSynth(JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH));
        builtinSynths.add(readOneResourceSynth(JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH));
        builtinSynths.add(readOneResourceSynth(YAMAHA_REF_SYNTH_PATH));
    }

    /**
     * The list of JJazzLab builtin synths.
     * <p>
     * JJazzLabSoundFont, YamahaRef, etc.
     *
     * @return
     */
    @Override
    public List<MidiSynth> getBuiltinSynths()
    {
        return new ArrayList<>(builtinSynths);
    }

    /**
     * Search the standard synths, the builtin synths and then through our active MidiSynth references to find a synth with
     * synthName.
     *
     * @param synthName
     * @return Can be null.
     */
    @Override
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
        for (MidiSynth synth : getBuiltinSynths())
        {
            if (synth.getName().equals(synthName))
            {
                return synth;
            }
        }
        for (WeakReference<MidiSynth> ref : midiSynthRefs)
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
    @Override
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
            try (FileInputStream fis = new FileInputStream(synthFile))
            {
                synths = reader.readSynthsFromStream(fis, synthFile); // Can raise exception
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
    @Override
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

    // ===============================================================================
    // Internal classes
    // ===============================================================================

    @ServiceProvider(service = MidiSynth.Finder.class)
    static public class SynthFinder implements MidiSynth.Finder
    {

        /**
         * Search the MidiSynthManager instance.
         *
         * @param synthName
         * @param synthFile If null, search the builtin synths. If no parent directory, search the MidiSynthManager default
         * directory for output synth config files.
         * @return
         */
        @Override
        public MidiSynth getMidiSynth(String synthName, File synthFile)
        {
            if (synthName == null)
            {
                throw new IllegalArgumentException("synthName=" + synthName + " synthFile=" + synthFile);
            }
            var msm = MidiSynthManagerImpl.getInstance();

            // First search via the name in registered instances
            MidiSynth res = msm.getMidiSynth(synthName);

            // Try to read the file if not null
            if (res == null && synthFile != null)
            {
                if (synthFile.getParentFile() == null)
                {
                    // If no parent file search the default dir
                    File dir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
                    synthFile = new File(dir, synthFile.getName());
                }
                List<MidiSynth> synths = msm.loadSynths(synthFile);
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

    private MidiSynth readOneResourceSynth(String insResourcePath)
    {
        InputStream is = getClass().getResourceAsStream(insResourcePath);
        assert is != null : "insResourcePath=" + insResourcePath;
        MidiSynthFileReader r = MidiSynthFileReader.Util.getReader("ins");
        assert r != null;
        try
        {
            List<MidiSynth> synths = r.readSynthsFromStream(is, null);
            assert synths.size() == 1;
            return synths.get(0);
        } catch (IOException ex)
        {
            throw new IllegalStateException("Unexpected error", ex);
        }
    }

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


    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @StaticResource(relative = true)
        public static final String ZIP_RESOURCE_PATH = "resources/MidiSynthFiles.zip";


        @Override
        public void upgrade(String oldVersion)
        {
            // Nothing
        }

        @Override
        public void initialize()
        {
            // Create the dir if it does not exists
            File dir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
            if (dir == null || (!dir.isDirectory() && !dir.mkdir()))
            {
                LOGGER.warning("upgrade() Could not create directory " + dir + ".");
            } else
            {
                // Copy files 
                copyFilesOrNot(dir);
            }


        }

        /**
         * If dir is not empty ask user confirmation to replace files.
         *
         * @param dir Must exist.
         */
        private void copyFilesOrNot(File dir)
        {
            boolean isEmpty;
            try
            {
                isEmpty = Utilities.isEmpty(dir.toPath());
            } catch (IOException ex)
            {
                LOGGER.warning("copyFilesOrNot() Can't check if dir. is empty. ex=" + ex.getLocalizedMessage());
                return;
            }
            if (!isEmpty)
            {
                String msg = "Fresh start: copying default Midi synth definition files to " + dir.getAbsolutePath() + ".\n\n"
                        + "OK to proceed?";
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
                Object result = DialogDisplayer.getDefault().notify(d);
                if (NotifyDescriptor.YES_OPTION != result)
                {
                    return;
                }
            }

            // Copy the default rhythms
            List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
            LOGGER.info("copyFilesOrNot() Copied " + res.size() + " Midi synth definition files to " + dir.getAbsolutePath());

        }

    }

}
