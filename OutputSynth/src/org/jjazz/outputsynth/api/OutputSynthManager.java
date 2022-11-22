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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.synths.GM2Synth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.GSSynth;
import org.jjazz.midi.api.synths.XGSynth;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.util.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Management of the OutputSynth instances.
 * <p>
 * Keep an OutputSynth instance for each available MidiOut device.
 */
public class OutputSynthManager implements PropertyChangeListener
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
     * The change event is also fired when default JJazzLab MidiDevice OUT changes.
     */
    public final static String PROP_DEFAULT_OUTPUTSYNTH = "PropDefaultOutputSynth";

    private static OutputSynthManager INSTANCE;
    private final HashMap<String, OutputSynth> mapDeviceNameSynth = new HashMap<>();
    private final OutputSynth defaultGMoutputSynth;
    private static final Preferences prefs = NbPreferences.forModule(OutputSynthManager.class);
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(OutputSynthManager.class.getSimpleName());

    /**
     * Get the OutputSynthManager instance.
     * <p>
     * Upon creation the OutputSynthManager preloads all the OutputSynth associated to each available OUT MidiDevice.
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

        defaultGMoutputSynth = new OutputSynth(GMSynth.getInstance());
        defaultGMoutputSynth.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.GM);


        // Listen to Midi out changes
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
            LOGGER.log(Level.FINE, "refresh() mdOut={0} outSynth{1}", new Object[]
            {
                mdOut.getDeviceInfo().getName(), outSynth
            });
        }
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the GMSynth.
     *
     * @return
     */
    public OutputSynth getNewGMOuputSynth()
    {
        var res = new OutputSynth(GMSynth.getInstance());
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.GM);
        return res;
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the GM2Synth.
     *
     * @return
     */
    public OutputSynth getNewGM2OuputSynth()
    {
        var res = new OutputSynth(GM2Synth.getInstance());
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.GM2);
        return res;
    }


    /**
     * Get a new instance of a default OutputSynth which just uses the XGSynth.
     *
     * @return
     */
    public OutputSynth getNewXGOuputSynth()
    {
        var res = new OutputSynth(XGSynth.getInstance());
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.XG);
        return res;
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the GSSynth.
     *
     * @return
     */
    public OutputSynth getNewGSOuputSynth()
    {
        var res = new OutputSynth(GSSynth.getInstance());
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.GS);
        return res;
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the JJazzLab soundfont in XG mode (compatible with FluidSynth).
     *
     * @return
     */
    public OutputSynth getNewJazzLabSoundFontXGOuputSynth()
    {
        var res = new OutputSynth(MidiSynthManager.getInstance().getMidiSynth(MidiSynthManager.JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME));
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.XG);
        return res;
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the JJazzLab soundfont in GS mode (compatible with
     * VirtualMidiSynth).
     *
     * @return
     */
    public OutputSynth getNewJazzLabSoundFontGSOuputSynth()
    {
        var res = new OutputSynth(MidiSynthManager.getInstance().getMidiSynth(MidiSynthManager.JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME));
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.GS);
        return res;
    }

    /**
     * Get a new instance of a default OutputSynth which just uses the Tyros 5 synth.
     *
     * @return
     */
    public OutputSynth getNewYamahaRefOuputSynth()
    {
        var res = new OutputSynth(MidiSynthManager.getInstance().getMidiSynth(MidiSynthManager.YAMAHA_REF_SYNTH_NAME));
        res.getUserSettings().setSendModeOnUponPlay(OutputSynth.UserSettings.SendModeOnUponPlay.OFF);
        return res;
    }

    /**
     * Get the current OuputSynth associated to the default JJazzLab Midi Device OUT.
     * <p>
     * If no Midi Device OUT defined, then return a shared instance of a GM Output Synth.
     *
     * @return Can't be null
     */
    public OutputSynth getDefaultOutputSynth()
    {
        OutputSynth res = defaultGMoutputSynth;
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
     * @param mdOutName Can't be null
     * @param outSynth  Can't be null
     */
    public void setOutputSynth(String mdOutName, OutputSynth outSynth)
    {
        Preconditions.checkNotNull(mdOutName);
        Preconditions.checkArgument(!mdOutName.isBlank());
        Preconditions.checkNotNull(outSynth);


        var oldSynth = mapDeviceNameSynth.get(mdOutName);
        if (oldSynth == outSynth)
        {
            return;
        } else if (oldSynth != null)
        {
            oldSynth.getUserSettings().removePropertyChangeListener(this);
        }


        // Change state
        mapDeviceNameSynth.put(mdOutName, outSynth);
        store(mdOutName, outSynth);
        outSynth.getUserSettings().addPropertyChangeListener(this);  // Listen to user settings changes to keep the saved preference updated


        // Notify listeners
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

    public void addPropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propName, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propName, l);
    }

    // ===============================================================================
    // PropertyChangeListener interface
    // ===============================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() instanceof OutputSynth.UserSettings us)
        {
            // An OutputSynth has changed, save it
            var outSynth = us.getOutputSynth();
            String mdName = (String) Utilities.reverseGet(mapDeviceNameSynth, outSynth);
            if (mdName != null)
            {
                store(mdName, outSynth);
            } else
            {
                LOGGER.warning("propertyChange() Unexpected null mdName! outSynth=" + outSynth + ", mapDeviceNameSynth=" + mapDeviceNameSynth);
            }
        }
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

    private void store(String mdOutName, OutputSynth outSynth)
    {
        prefs.put(mdOutName, outSynth.saveAsString());
    }


    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);
        }

    }

}
