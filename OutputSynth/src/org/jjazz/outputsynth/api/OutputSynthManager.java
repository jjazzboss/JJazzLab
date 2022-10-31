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
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.ui.utilities.api.SingleRootFileSystemView;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Management of the OutputSynth instances.
 */
public class OutputSynthManager
{

    /**
     * Property change event fired each time a new OutputSynth is associated to a MidiDevice OUT: oldValue=Midi device OUT name,
     * newValue=OutputSynth.
     */
    public final static String PROP_MDOUT_OUTPUTSYNTH = "MdOut-OutputSynth";
    /**
     * Property change event fired each time a new OutputSynth is associated to the default JJazzLab MidiDevice OUT: oldValue=old
     * OutputSynth, newValue=new OutputSynth. The change event is also fired when default JJazzLab MidiDevice OUT changes. Note
     * that newValue might be null if no default JJazzLab OUT MidiDevice is set. 
     */
    public final static String PROP_DEFAULT_OUTPUTSYNTH = "Default-OutputSynth";

    private static OutputSynthManager INSTANCE;
    private static JFileChooser CHOOSER_INSTANCE;
    private final HashMap<String, OutputSynth> mapDeviceNameSynth = new HashMap<>();
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
        JJazzMidiSystem.getInstance().addPropertyChangeListener(JJazzMidiSystem.PROP_MIDI_OUT,
                e -> midiOutChanged((MidiDevice) e.getOldValue(), (MidiDevice) e.getNewValue()));
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the GMSynth.
     *
     * @return
     */
    public OutputSynth getNewGMOuputSynth()
    {
        return new OutputSynth(new MidiSynthList());
    }

    /**
     * Get the current OuputSynth associated to the default JJazzLab Midi Device OUT.
     *
     * @return Can be null if no default JJazzLab Midi OUT device is set.
     */
    public OutputSynth getDefaultOutputSynth()
    {
        OutputSynth res = null;
        var mdOut = JJazzMidiSystem.getInstance().getDefaultOutDevice();
        if (mdOut != null)
        {
            res = getOutputSynth(mdOut.getDeviceInfo().getName());
        }
        return res;
    }

    /**
     * Get the OutputSynth associated to the specified output MidiDevice.
     *
     * @param mdOutName A Midi device OUT name, can't be null or empty
     * @return Can't be null.
     */
    public OutputSynth getOutputSynth(String mdOutName)
    {
        Preconditions.checkNotNull(mdOutName);
        Preconditions.checkArgument(!mdOutName.isBlank());
        OutputSynth outSynth = mapDeviceNameSynth.get(mdOutName);
        if (outSynth != null)
        {
            return outSynth;
        }

        // First time call for this MidiDevice : create an OutpuSynth and associate it to mdOutName

        // Try to restore the OutputSynth from preferences
        String s = prefs.get(mdOutName, null);
        if (s != null)
        {
            try
            {
                outSynth = OutputSynth.loadFromString(s);
            } catch (IOException ex)
            {
                LOGGER.warning("getOutputSynth() mdOutName=" + mdOutName + " Can't restore OutputSynth from String s=" + s + ". ex=" + ex.getMessage());
            }
        }

        if (outSynth == null)
        {
            // Create a default OutputSynth
            outSynth = getNewGMOuputSynth();
        }

        // Associate the created OutputSynth to mdOut
        setOutputSynth(mdOutName, outSynth);

        return outSynth;
    }

    /**
     * Associate outSynth to the specified midi OUT device name.
     *
     * @param mdOutName
     * @param outSynth
     */
    public void setOutputSynth(String mdOutName, OutputSynth outSynth)
    {
        Preconditions.checkNotNull(mdOutName);
        Preconditions.checkArgument(!mdOutName.isBlank());
        Preconditions.checkNotNull(outSynth);

        var oldSynth = mapDeviceNameSynth.get(mdOutName);
        mapDeviceNameSynth.put(mdOutName, outSynth);
        prefs.get(mdOutName, outSynth.saveAsString());

        pcs.firePropertyChange(PROP_MDOUT_OUTPUTSYNTH, mdOutName, outSynth);
        var mdOut = JJazzMidiSystem.getInstance().getDefaultOutDevice();
        if (mdOut != null && mdOutName.equals(mdOut.getDeviceInfo().getName()))
        {
            pcs.firePropertyChange(PROP_DEFAULT_OUTPUTSYNTH, oldSynth, outSynth);
        }

    }

    /**
     * Show a dialog to select an OutputSynth file.
     * <p>
     *
     * @param save If true show a save dialog, if false an open dialog.
     * @return The selected file. Null if user cancelled or no valid selection. File is guaranteed to have appropriate location
     * and extension.
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


    // ===============================================================================
    // Private methods
    // ===============================================================================
    private void midiOutChanged(MidiDevice oldMd, MidiDevice newMd)
    {
        var oldSynth = oldMd == null ? null : getOutputSynth(oldMd.getDeviceInfo().getName());
        var newSynth = newMd == null ? null : getOutputSynth(newMd.getDeviceInfo().getName());
        pcs.firePropertyChange(PROP_DEFAULT_OUTPUTSYNTH, oldSynth, newSynth);   // newSynth might be null !
    }

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
