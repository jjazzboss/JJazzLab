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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.ui.utilities.api.SingleRootFileSystemView;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Management of the OutputSynth instances.
 */
public class OutputSynthManager implements PropertyChangeListener
{

    public final static String PROP_DEFAULT_OUTPUTSYNTH = "OutputSynth";

    /**
     * Convenience property which replicates the audio latency changes of the current output synth.
     */
    public final static String PROP_AUDIO_LATENCY = "AudioLatency";

    private static OutputSynthManager INSTANCE;
    private static JFileChooser CHOOSER_INSTANCE;
    private HashMap<MidiDevice, OutputSynth> mapDeviceSynth = new HashMap<>();
    private static final Preferences prefs = NbPreferences.forModule(OutputSynthManager.class);
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

    }

    /**
     * The OutputSynth associated to the specified output MidiDevice.
     *
     * @param mdOut
     * @return Can't be null.
     */
    public OutputSynth getOutputSynth(MidiDevice mdOut)
    {
        Preconditions.checkNotNull(mdOut);
        OutputSynth res = mapDeviceSynth.get(mdOut);
        if (res != null)
        {
            return res;
        }

        // Try to restore the OutputSynth from preferences
        String s = prefs.get(mdOut.getDeviceInfo().getName(), null);
        if (s != null)
        {
            try
            {
                res = OutputSynth.loadFromString(s);
            } catch (IOException ex)
            {
                LOGGER.warning("getOutputSynth() mdOut=" + mdOut.getDeviceInfo().getName() + " Can't restore OutputSynth from String s=" + s + ". ex=" + ex.getMessage());
            }
        }

        if (res == null)
        {
            // Create a default OutputSynth
            res = new OutputSynth(new MidiSynthList());            
        }

        return res;
    }

    /**
     * Set the current OutputSynth.
     *
     * @param mdOut
     * @param outSynth Can't be null
     */
    public void setOutputSynth(MidiDevice mdOut, OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth);   //NOI18N
        }
        int oldLatency = 0;
        OutputSynth old = this.outputSynth;
        if (old != null)
        {
            oldLatency = old.getAudioLatency();
            old.removePropertyChangeListener(this);
        }
        outputSynth = outSynth;
        outputSynth.addPropertyChangeListener(this);        // Listen to file and audio latency changes


        if (outputSynth.getFile() != null)
        {
            prefs.put(PROP_DEFAULT_OUTPUTSYNTH, outputSynth.getFile().getName());
        }
        pcs.firePropertyChange(PROP_DEFAULT_OUTPUTSYNTH, old, outputSynth);


        if (outputSynth.getAudioLatency() != oldLatency)
        {
            pcs.firePropertyChange(PROP_AUDIO_LATENCY, oldLatency, outputSynth.getAudioLatency());
        }

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
        chooser.setDialogTitle(save ? ResUtil.getString(getClass(), "CTL_SaveOuputSynthConfigFile") : ResUtil.getString(getClass(), "CTL_LoadOutputSynthConfigFile"));
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
     *
     * @param f
     * @param notifyUser If true notify user if error occured while reading the file.
     * @return Null if problem.
     */
    public OutputSynth loadOutputSynth(File f, boolean notifyUser)
    {
        if (f == null)
        {
            throw new NullPointerException("f");   //NOI18N
        }
        OutputSynth synth = null;
        XStream xstream = Utilities.getSecuredXStreamInstance();

        try ( var fis = new FileInputStream(f))
        {
            Reader r = new BufferedReader(new InputStreamReader(fis, "UTF-8"));        // Needed to support special/accented chars
            synth = (OutputSynth) xstream.fromXML(r);
        } catch (XStreamException | IOException ex)
        {
            String msg = ResUtil.getString(getClass(), "ERR_ProbReadingFile", f.getAbsolutePath());
            msg += ": " + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, "loadOutputSynth() - {0}", msg);   //NOI18N
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
            if (evt.getPropertyName().equals(OutputSynth.PROP_FILE))
            {
                File f = (File) evt.getNewValue();
                if (f != null)
                {
                    prefs.put(PROP_DEFAULT_OUTPUTSYNTH, f.getName());
                }
            } else if (evt.getPropertyName().equals(OutputSynth.PROP_AUDIO_LATENCY_MS))
            {
                pcs.firePropertyChange(OutputSynthManager.PROP_AUDIO_LATENCY, (int) evt.getOldValue(), (int) evt.getNewValue());
            }
        }
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     * A fixed user directory.
     *
     * @return Can be null if error
     */
    private File getOutputSynthFilesDir()
    {
        File rDir = FileDirectoryManager.getInstance().getAppConfigDirectory(OUTPUT_SYNTH_FILES_DIR);
        if (rDir == null)
        {
            String msg = ResUtil.getString(getClass(), "ERR_NoAppConfigDir", OUTPUT_SYNTH_FILES_DIR);
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
            FileNameExtensionFilter filter = new FileNameExtensionFilter(ResUtil.getString(getClass(), "CTL_OutputSynthConfigFiles") + " (." + OUTPUT_SYNTH_FILES_EXT + ")", OUTPUT_SYNTH_FILES_EXT);
            CHOOSER_INSTANCE.addChoosableFileFilter(filter);
            CHOOSER_INSTANCE.setAcceptAllFileFilterUsed(false);
            CHOOSER_INSTANCE.setMultiSelectionEnabled(false);
            CHOOSER_INSTANCE.setFileSelectionMode(JFileChooser.FILES_ONLY);
            CHOOSER_INSTANCE.setCurrentDirectory(getOutputSynthFilesDir());
        }
        return CHOOSER_INSTANCE;
    }

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================

    /**
     * We need to copy the current OutputSynth configuration file (if any) to the new config. directory.
     * <p>
     */
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {

            if (oldVersion == null)
            {
                return;
            }

            var um = UpgradeManager.getInstance();
            var fdm = FileDirectoryManager.getInstance();

            // Get the old output synth config file name
            Properties oldProp = um.getPropertiesFromPrefs(prefs);
            if (oldProp == null)
            {
                LOGGER.warning("upgrade() no old properties found for prefs=" + prefs.absolutePath());   //NOI18N
                return;
            }
            String oldCfgFileName = oldProp.getProperty(PROP_DEFAULT_OUTPUTSYNTH);

            if (oldCfgFileName == null)
            {
                LOGGER.warning("upgrade() oldVersion=" + oldVersion + ", undefined Output Synth config file property" + PROP_DEFAULT_OUTPUTSYNTH);   //NOI18N
                return;
            }

            // Try to get the old file
            File prevAppConfigDir = fdm.getOldAppConfigDirectory(oldVersion, OUTPUT_SYNTH_FILES_DIR);
            if (prevAppConfigDir == null)
            {
                LOGGER.warning("upgrade() can't find prevAppConfigDir=" + prevAppConfigDir);   //NOI18N
                return;
            }

            File oldCfgFile = new File(prevAppConfigDir, oldCfgFileName);
            if (!oldCfgFile.exists())
            {
                LOGGER.warning("upgrade() can't find oldCfgFile=" + oldCfgFile.getAbsolutePath());   //NOI18N
                return;
            }

            File appConfigDir = fdm.getAppConfigDirectory(OUTPUT_SYNTH_FILES_DIR);
            File newCfgFile = new File(appConfigDir, oldCfgFileName);
            try
            {
                Files.copy(oldCfgFile.toPath(), newCfgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                prefs.put(PROP_DEFAULT_OUTPUTSYNTH, oldCfgFileName);
            } catch (IOException ex)
            {
                LOGGER.warning("upgrade() error copying output synth config file=" + oldCfgFile.getAbsolutePath() + ". ex=" + ex.getMessage());   //NOI18N
            }
        }
    }
}
