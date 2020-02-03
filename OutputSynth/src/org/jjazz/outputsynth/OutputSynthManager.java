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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.ui.utilities.SingleRootFileSystemView;
import org.jjazz.util.Utilities;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * Management of the OutputSynth.
 */
public class OutputSynthManager implements PropertyChangeListener
{

    public final static String PROP_DEFAULT_OUTPUTSYNTH = "OutputSynth";
    public final static String DEFAULT_OUTPUT_SYNTH_FILENAME = "Default.cfg";
    public final static String JJAZZLAB_OUTPUT_SYNTH_FILENAME = "JJazzLab-SoundFont.cfg";
    public final static String OUTPUT_SYNTH_FILES_DIR = "OutputSynthFiles";
    public final static String OUTPUT_SYNTH_FILES_EXT = "cfg";

    private static final String OUTPUT_SYNTH_FILES_ZIP = "resources/OutputSynthFiles.zip";

    private static OutputSynthManager INSTANCE;
    private static JFileChooser CHOOSER_INSTANCE;
    private OutputSynth outputSynth;
    private static Preferences prefs = NbPreferences.forModule(MidiSynthManager.class);
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
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
        // Copy the default OutputSynth config files if not done
        final File dir = getOutputSynthFilesDir();
        final File[] files = dir.listFiles();
        if (files.length == 0)
        {
            Runnable run = new Runnable()
            {
                @Override
                public void run()
                {
                    File[] res = OutputSynthManager.this.copyBuiltinResourceFiles(dir);
                    LOGGER.info("OutputSynthManager() First time initialization - copied " + res.length + " files into " + dir.getAbsolutePath());
                }
            };
            BaseProgressUtils.showProgressDialogAndRun(run, "First time initialization - copying builtin Output Synth config files...");
        }

        // Load the saved output synth file
        String fileName = prefs.get(PROP_DEFAULT_OUTPUTSYNTH, null);        // Only the filename is stored in the preferences (no path)
        if (fileName != null)
        {
            File f = new File(getOutputSynthFilesDir(), fileName);
            outputSynth = loadOutputSynth(f, false);
        }
        if (outputSynth == null)
        {
            // Try reading the default config file
            File f = new File(getOutputSynthFilesDir(), DEFAULT_OUTPUT_SYNTH_FILENAME);
            outputSynth = loadOutputSynth(f, false);
            if (outputSynth == null)
            {
                outputSynth = new OutputSynth();
                outputSynth.setFile(f);
            }
        }
        
        // Listen to file changes
        outputSynth.addPropertyChangeListener(this); 
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

    /**
     * Set the current OutputSynth.
     *
     * @param outSynth Can't be null
     */
    public void setOutputSynth(OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth);
        }
        OutputSynth old = this.outputSynth;
        if (old != null)
        {
            old.removePropertyChangeListener(this);
        }
        outputSynth = outSynth;
        outputSynth.addPropertyChangeListener(this);        // Listen to file changes
        if (outputSynth.getFile() != null)
        {
            prefs.put(PROP_DEFAULT_OUTPUTSYNTH, outputSynth.getFile().getName());
        }
        pcs.firePropertyChange(PROP_DEFAULT_OUTPUTSYNTH, old, outputSynth);
    }

    /**
     * Show a dialog to select an OutputSynth file.
     * <p>
     *
     * @param save If true show a save dialog, if false an open dialog.
     * @return The selected file. Null if user cancelled or no valid selection. File is guaranteed to have appropriate location
     *         and extension.
     */
    public File showSelectOutputSynthFileDialog(boolean save)
    {
        JFileChooser chooser = getFileChooserInstance();
        chooser.setDialogTitle((save ? "Save" : "Load") + " Output Synth Configuration File");
        Object res;
        if (save)
        {
            res = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        } else
        {
            res = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        }
        File f = null;
        if (res.equals(JFileChooser.APPROVE_OPTION))
        {
            f = chooser.getSelectedFile();
            if (!f.getParentFile().equals(getOutputSynthFilesDir()))
            {
                String msg = "Invalid directory. Output synth configuration file must be in the default directory";
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                chooser.setCurrentDirectory(getOutputSynthFilesDir());
                return null;
            }
            // Make sure we have the right extension
            f = new File(f.getParentFile(), Utilities.replaceExtension(f.getName(), OutputSynthManager.OUTPUT_SYNTH_FILES_EXT));
        }
        return f;
    }

    /**
     * Load an OutputSynth from a file.
     * <p>
     * Notify user if problem occured while reading file.
     *
     * @param f
     * @param notifyUser If true notify user if error occured while reading the file.
     * @return Null if problem.
     */
    public OutputSynth loadOutputSynth(File f, boolean notifyUser)
    {
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        OutputSynth synth = null;
        XStream xstream = new XStream();
        try
        {
            synth = (OutputSynth) xstream.fromXML(f);
        } catch (XStreamException ex)
        {
            String msg = "Problem reading file " + f.getAbsolutePath() + ": " + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, "loadOutputSynth() - {0}", msg);
            if (notifyUser)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
            return null;
        }
        synth.setFile(f);
        return synth;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == outputSynth)
        {
            if (evt.getPropertyName() == OutputSynth.PROP_FILE)
            {
                File f = (File) evt.getNewValue();
                if (f != null)
                {
                    prefs.put(PROP_DEFAULT_OUTPUTSYNTH, f.getName());
                }
            }
        }
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * A fixed user directory.
     *
     * @return
     */
    private File getOutputSynthFilesDir()
    {
        File rDir = FileDirectoryManager.getInstance().getAppConfigDirectory(OUTPUT_SYNTH_FILES_DIR);
        if (rDir == null)
        {
            String msg = "SERIOUS ERROR - Can't find the app. config. directory for " + OUTPUT_SYNTH_FILES_DIR;
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
        return rDir;
    }

    private JFileChooser getFileChooserInstance()
    {
        if (CHOOSER_INSTANCE == null)
        {
            FileSystemView fsv = new SingleRootFileSystemView(getOutputSynthFilesDir());
            CHOOSER_INSTANCE = new JFileChooser(fsv);
            CHOOSER_INSTANCE.resetChoosableFileFilters();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Output synth config. files (" + "." + OUTPUT_SYNTH_FILES_EXT + ")", OUTPUT_SYNTH_FILES_EXT);
            CHOOSER_INSTANCE.addChoosableFileFilter(filter);
            CHOOSER_INSTANCE.setAcceptAllFileFilterUsed(false);
            CHOOSER_INSTANCE.setMultiSelectionEnabled(false);
            CHOOSER_INSTANCE.setFileSelectionMode(JFileChooser.FILES_ONLY);
            CHOOSER_INSTANCE.setCurrentDirectory(getOutputSynthFilesDir());
        }
        return CHOOSER_INSTANCE;
    }

    /**
     * Copy the builtin OutputSynth configuration files within the JAR to destPath.
     * <p>
     *
     * @param destPath
     *
     */
    private File[] copyBuiltinResourceFiles(File destDir)
    {
        List<File> res = Utilities.extractZipResource(getClass(), OutputSynthManager.OUTPUT_SYNTH_FILES_ZIP, destDir.toPath());
        if (res.isEmpty())
        {
            LOGGER.warning("copyBuiltinResourceFiles() No output synth definition file found in " + OUTPUT_SYNTH_FILES_ZIP);
        }
        return res.toArray(new File[0]);
    }
}
