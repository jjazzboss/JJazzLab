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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.swing.JFileChooser;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Management of the OutputSynth instances.
 * <p>
 * Keep an OutputSynth instance for each available MidiOut device.
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
     * OutputSynth, newValue=new OutputSynth.
     * <p>
     * The change event is also fired when default JJazzLab MidiDevice OUT changes. Note that newValue might be null if no default
     * JJazzLab OUT MidiDevice is set.
     */
    public final static String PROP_DEFAULT_OUTPUTSYNTH = "Default-OutputSynth";
    
    private static OutputSynthManager INSTANCE;
    private static JFileChooser CHOOSER_INSTANCE;
    private final HashMap<String, OutputSynth> mapDeviceNameSynth = new HashMap<>();
    private static final Preferences prefs = NbPreferences.forModule(OutputSynthManager.class);
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(OutputSynthManager.class.getSimpleName());

    /**
     * Get the OutputSynthManager instance.
     * <p>
     * Upon creation the OutputSynthManager preloads all the OutputSynthManager
     *
     * @return
     */
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
        var jms = JJazzMidiSystem.getInstance();
        jms.addPropertyChangeListener(JJazzMidiSystem.PROP_MIDI_OUT,
                e -> midiOutChanged((MidiDevice) e.getOldValue(), (MidiDevice) e.getNewValue()));
        
        refresh();
    }

    /**
     * Scan all the OUT MidiDevices and make sure each MidiDevice is associated to an OutputSynth.
     * <p>
     * Should be called if the list of available OUT MidiDevices has changed.
     */
    public final void refresh()
    {
        LOGGER.fine("refresh() -- ");
        for (var mdOut : JJazzMidiSystem.getInstance().getOutDeviceList())
        {
            var outSynth = getOutputSynth(mdOut.getDeviceInfo().getName());
            LOGGER.fine("refresh() mdOut=" + mdOut.getDeviceInfo().getName() + " outSynth" + outSynth);
        }
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the GMSynth.
     *
     * @return
     */
    public OutputSynth getNewGMOuputSynth()
    {
        return new OutputSynth(new MultiSynth());
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
    
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }
    
    public void addPropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propName, l);
    }
    
    public void removePropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propName, l);
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
            
            LOGGER.severe("upgrade() COMMENTED OUT! TO RESTORE");
//            // Get the old output synth config file name
//            Properties oldProp = um.getPropertiesFromPrefs(prefs);
//            if (oldProp == null)
//            {
//                LOGGER.warning("upgrade() no old properties found for prefs=" + prefs.absolutePath());   //NOI18N
//                return;
//            }
//            String oldCfgFileName = oldProp.getProperty(PROP_DEFAULT_OUTPUTSYNTH);
//
//            if (oldCfgFileName == null)
//            {
//                LOGGER.warning("upgrade() oldVersion=" + oldVersion + ", undefined Output Synth config file property" + PROP_DEFAULT_OUTPUTSYNTH);   //NOI18N
//                return;
//            }
//
//            // Try to get the old file
//            File prevAppConfigDir = fdm.getOldAppConfigDirectory(oldVersion, OUTPUT_SYNTH_FILES_DIR);
//            if (prevAppConfigDir == null)
//            {
//                LOGGER.warning("upgrade() can't find prevAppConfigDir=" + prevAppConfigDir);   //NOI18N
//                return;
//            }
//
//            File oldCfgFile = new File(prevAppConfigDir, oldCfgFileName);
//            if (!oldCfgFile.exists())
//            {
//                LOGGER.warning("upgrade() can't find oldCfgFile=" + oldCfgFile.getAbsolutePath());   //NOI18N
//                return;
//            }
//
//            File appConfigDir = fdm.getAppConfigDirectory(OUTPUT_SYNTH_FILES_DIR);
//            File newCfgFile = new File(appConfigDir, oldCfgFileName);
//            try
//            {
//                Files.copy(oldCfgFile.toPath(), newCfgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                prefs.put(PROP_DEFAULT_OUTPUTSYNTH, oldCfgFileName);
//            } catch (IOException ex)
//            {
//                LOGGER.warning("upgrade() error copying output synth config file=" + oldCfgFile.getAbsolutePath() + ". ex=" + ex.getMessage());   //NOI18N
//            }
        }
    }
}
