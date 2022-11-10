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
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.GM2Synth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.GSSynth;
import org.jjazz.midi.api.synths.XGSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.startup.spi.StartupTask;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;


/**
 * Manager the list of MidiSynths used during a JJazzLab session.
 */
public class MidiSynthManager
{

    /**
     * Property change event fired when a MidiSynth is added or removed from the file-based MidiSynth.
     * <p>
     * If added: oldValue=null, newValue=added MidiSynth<br>
     * If removed: oldValue=removed MidiSynth, newValue=null<br>
     */
    public static String PROP_FILE_BASED_MULTISYNTH_LIST = "PropFileBasedSynthList";


    // Some builtin MidiSynth names retrieved from a .ins file
    public static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME = "JJazzLab SoundFont (GM2)";
    public static String JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME = "JJazzLab SoundFont (GS)";
    public static String JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME = "JJazzLab SoundFont (XG)";
    public static String YAMAHA_REF_SYNTH_NAME = "Tyros5 Synth";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GM2.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GS.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_XG.ins";
    @StaticResource(relative = true)
    private final static String YAMAHA_REF_SYNTH_PATH = "resources/YamahaRefSynth.ins";


    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static MidiSynthManager INSTANCE;

    private File lastSynthDir;
    private final List<MidiSynth> builtinMidiSynths = new ArrayList<>();
    private final List<MidiSynth> fileBasedMidiSynths = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(MidiSynthManager.class.getSimpleName());
    private transient final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    /**
     * Get the instance.
     * <p>
     * Upon creation the MidiSynthManager contains at least the GM/GM2/XG/GS MidiSynths.
     *
     * @return
     */
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
        builtinMidiSynths.add(GMSynth.getInstance());
        builtinMidiSynths.add(GM2Synth.getInstance());
        builtinMidiSynths.add(XGSynth.getInstance());
        builtinMidiSynths.add(GSSynth.getInstance());
        builtinMidiSynths.add(readOneResourceSynth(JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH));
        builtinMidiSynths.add(readOneResourceSynth(JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH));
        builtinMidiSynths.add(readOneResourceSynth(JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH));
        builtinMidiSynths.add(readOneResourceSynth(YAMAHA_REF_SYNTH_PATH));
    }


    /**
     * Add the specified MidiSynth as a file-based MidiSynth.
     * <p>
     *
     * @param midiSynth Must have getFile() returns non-null.
     * @return True if midiSynth was successfully added, false if midiSynth was already referenced by the MidiSynthManager.
     */
    public boolean addFileBasedMidiSynth(MidiSynth midiSynth)
    {
        Preconditions.checkNotNull(midiSynth);
        Preconditions.checkArgument(midiSynth.getFile() != null);
        Preconditions.checkArgument(!builtinMidiSynths.contains(midiSynth));

        if (!fileBasedMidiSynths.contains(midiSynth))
        {
            fileBasedMidiSynths.add(midiSynth);
            pcs.firePropertyChange(PROP_FILE_BASED_MULTISYNTH_LIST, null, midiSynth);
            return true;
        }

        return false;
    }

    /**
     * Remove the specified MidiSynth from the file-based MidiSynths.
     * <p>
     * @param midiSynth
     * @return
     */
    public boolean removeFileBasedMidiSynth(MidiSynth midiSynth)
    {
        boolean res = fileBasedMidiSynths.remove(midiSynth);
        if (res)
        {
            pcs.firePropertyChange(PROP_FILE_BASED_MULTISYNTH_LIST, midiSynth, null);
        }
        return res;
    }

    /**
     * The list of builtin and/or file-based MidiSynths.
     * <p>
     * Builtin MidiSynths are GM, GM2, etc.
     *
     * @param builtin   If true include the builtin MidiSynths
     * @param fileBased If true include the file-based MidiSynths (after the builtin ones).
     * @return Can be empty.
     */
    public List<MidiSynth> getMidiSynths(boolean builtin, boolean fileBased)
    {
        List<MidiSynth> res = new ArrayList<>();
        if (builtin)
        {
            res.addAll(builtinMidiSynths);
        }
        if (fileBased)
        {
            res.addAll(fileBasedMidiSynths);
        }
        return res;
    }

    /**
     * Search a MidiSynth with the specified name in the builtin and file-based MidiSynths.
     *
     * @param name
     * @return Can be null.
     */
    public MidiSynth getMidiSynth(String name)
    {
        return Stream.of(builtinMidiSynths, fileBasedMidiSynths)
                .flatMap(l -> l.stream())
                .filter(midiSynth -> midiSynth.getName().equals(name))
                .findAny()
                .orElse(null);
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
        List<FileNameExtensionFilter> allFilters = new ArrayList<>();
        for (MidiSynthFileReader r : Lookup.getDefault().lookupAll(MidiSynthFileReader.class))
        {
            allFilters.addAll(r.getSupportedFileTypes());
        }

        // Initialize the file chooser
        JFileChooser chooser = org.jjazz.ui.utilities.api.Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        for (FileNameExtensionFilter filter : allFilters)
        {
            chooser.addChoosableFileFilter(filter);
        }
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(ResUtil.getString(getClass(), "MidiSynthManager.DialogTitle"));
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

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    private MidiSynth readOneResourceSynth(String insResourcePath)
    {
        InputStream is = getClass().getResourceAsStream(insResourcePath);
        assert is != null : "insResourcePath=" + insResourcePath;   //NOI18N
        MidiSynthFileReader r = MidiSynthFileReader.getReader("ins");
        assert r != null;   //NOI18N
        try
        {
            List<MidiSynth> synths = r.readSynthsFromStream(is, null);
            assert synths.size() == 1;   //NOI18N
            return synths.get(0);
        } catch (IOException ex)
        {
            throw new IllegalStateException("Unexpected error", ex);   //NOI18N
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

    // ===============================================================================
    // Inner classes
    // ===============================================================================

    @ServiceProvider(service = MidiSynth.Finder.class)
    static public class SynthFinder implements MidiSynth.Finder
    {

        /**
         * Search the MidiSynthManager instance.
         *
         * @param synthName
         * @param synthFile
         * @return
         */
        @Override
        public MidiSynth getMidiSynth(String synthName, File synthFile)
        {
            Preconditions.checkNotNull(synthName);
            MidiSynth res = MidiSynthManager.getInstance().getMidiSynth(synthName);
            return res;
        }
    }
    // =====================================================================================
    // Startup Task
    // =====================================================================================

    /**
     * Copy the default Midi files in the app config directory.
     * <p>
     * Could be an UpgradeTask since it should be executed only upon fresh start. But we use a StartupTask because a user dialog
     * might be used.
     */
    @ServiceProvider(service = StartupTask.class)
    public static class CopyMidiSynthsTask implements StartupTask
    {

        public static final int PRIORITY = 100;
        @StaticResource(relative = true)
        public static final String ZIP_RESOURCE_PATH = "resources/MidiSynthFiles.zip";

        @Override
        public boolean run()
        {
            if (!UpgradeManager.getInstance().isFreshStart())
            {
                return false;
            } else
            {
                initializeDir();
                return true;
            }
        }

        @Override
        public int getPriority()
        {
            return PRIORITY;
        }

        @Override
        public String getName()
        {
            return "Copy default Midi synth definition files";
        }

        private void initializeDir()
        {
            File dir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
            if (dir == null)
            {
                return;
            }
            if (!dir.isDirectory())
            {
                LOGGER.warning("CopyMidiSynthsTask.initializeDir() Could not access directory " + dir.getAbsolutePath() + ".");   //NOI18N
            } else
            {
                // Copy files 
                copyFilesOrNot(dir);
            }
        }

        /**
         * If dir is not empty ask user confirmation to replace files.
         * <p>
         * Normally dir will be empty for a real fresh start. But if user deleted its user settings and has changed some Midi
         * synth definition file, better to ask him if it's OK to copy the files over.
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
                LOGGER.warning("CopyMidiSynthsTask.copyFilesOrNot() Can't check if dir. is empty. ex=" + ex.getMessage());   //NOI18N
                return;
            }

            if (!isEmpty)
            {
                String msg = ResUtil.getString(getClass(), "MidiSynthManager.MidiSynthFilesOverwriteConfirmation", dir.getAbsolutePath());
                String[] options = new String[]
                {
                    "OK", ResUtil.getString(getClass(), "MidiSynthManager.Skip")
                };
                NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(), "MidiSynthManager.FirstTimeInit"), 0, NotifyDescriptor.QUESTION_MESSAGE, options, "OK");
                Object result = DialogDisplayer.getDefault().notify(d);

                if (!result.equals("OK"))
                {
                    return;
                }
            }

            // Copy the default rhythms
            List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
            LOGGER.info("CopyMidiSynthsTask.copyFilesOrNot() Copied " + res.size() + " Midi synth definition files to " + dir.getAbsolutePath());   //NOI18N

        }

    }
}
