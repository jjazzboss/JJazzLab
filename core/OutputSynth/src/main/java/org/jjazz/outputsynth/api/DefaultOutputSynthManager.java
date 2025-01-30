/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.DefaultMidiSynthManager;
import org.jjazz.midi.api.synths.GM2Synth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.GSSynth;
import org.jjazz.midi.api.synths.XGSynth;
import org.jjazz.midi.spi.MidiSynthManager;
import org.jjazz.outputsynth.api.OutputSynth.UserSettings.SendModeOnUponPlay;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;

/**
 * A default implementation of an OutputSynthManager.
 */
public class DefaultOutputSynthManager implements OutputSynthManager, PropertyChangeListener
{

    static private DefaultOutputSynthManager INSTANCE;
    protected final Map<String, OutputSynth> mapDeviceNameSynth = new HashMap<>();
    private final OutputSynth defaultGMoutputSynth;
    protected final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Preferences prefs = NbPreferences.forModule(DefaultOutputSynthManager.class);
    private static final Logger LOGGER = Logger.getLogger(DefaultOutputSynthManager.class.getSimpleName());

    static public DefaultOutputSynthManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new DefaultOutputSynthManager();
        }
        return INSTANCE;
    }

    protected DefaultOutputSynthManager()
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
     * Get the current OuputSynth associated to the default JJazzLab Midi Device OUT.
     * <p>
     * If no Midi Device OUT defined, then return a shared instance of a GM Output Synth.
     *
     * @return Can't be null
     */
    @Override
    public OutputSynth getDefaultOutputSynth()
    {
        OutputSynth res = defaultGMoutputSynth;
        var mdOut = JJazzMidiSystem.getInstance().getDefaultOutDevice();
        if (mdOut != null)
        {
            res = getMidiDeviceOutputSynth(mdOut.getDeviceInfo().getName());
        }
        return res;
    }


    @Override
    public OutputSynth getMidiDeviceOutputSynth(String mdOutName)
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
                LOGGER.log(Level.WARNING, "getOutputSynth() mdOutName={0} Can''t restore OutputSynth from String s={1}. ex={2}", new Object[]
                {
                    mdOutName,
                    s, ex.getMessage()
                });
            }
        }

        if (outSynth == null)
        {
            // Create a default OutputSynth
            outSynth = getStandardOutputSynth(STD_GM);
        }

        // Associate the created OutputSynth to mdOut
        setMidiDeviceOutputSynth(mdOutName, outSynth);

        return outSynth;
    }

    @Override
    public void setMidiDeviceOutputSynth(String mdOutName, OutputSynth outSynth)
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

    @Override
    public OutputSynth getStandardOutputSynth(String stdName)
    {
        MidiSynth synth;
        SendModeOnUponPlay mode;
        switch (stdName)
        {
            case OutputSynthManager.STD_GM ->
            {
                synth = GMSynth.getInstance();
                mode = SendModeOnUponPlay.GM;
            }
            case OutputSynthManager.STD_GM2 ->
            {
                synth = GM2Synth.getInstance();
                mode = SendModeOnUponPlay.GM2;
            }
            case OutputSynthManager.STD_GS ->
            {
                synth = GSSynth.getInstance();
                mode = SendModeOnUponPlay.GS;
            }
            case OutputSynthManager.STD_XG ->
            {
                synth = XGSynth.getInstance();
                mode = SendModeOnUponPlay.XG;
            }
            case OutputSynthManager.STD_JJAZZLAB_SOUNDFONT_GS ->
            {
                synth = MidiSynthManager.getDefault().getMidiSynth(DefaultMidiSynthManager.JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME);
                mode = OutputSynth.UserSettings.SendModeOnUponPlay.GS;
            }
            case OutputSynthManager.STD_JJAZZLAB_SOUNDFONT_XG ->
            {
                synth = MidiSynthManager.getDefault().getMidiSynth(DefaultMidiSynthManager.JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME);
                mode = OutputSynth.UserSettings.SendModeOnUponPlay.XG;
            }
            case OutputSynthManager.STD_JJAZZLAB_SOUNDFONT_GM2 ->
            {
                synth = MidiSynthManager.getDefault().getMidiSynth(DefaultMidiSynthManager.JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME);
                mode = OutputSynth.UserSettings.SendModeOnUponPlay.GM2;
            }
            default ->
            {
                return null;
            }
        }

        var res = new OutputSynth(synth);
        res.getUserSettings().setSendModeOnUponPlay(mode);

        return res;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void addPropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propName, l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
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
                LOGGER.log(Level.WARNING, "propertyChange() Unexpected null mdName! outSynth={0}, mapDeviceNameSynth={1}", new Object[]
                {
                    outSynth,
                    mapDeviceNameSynth
                });
            }
        }
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================
    private void midiOutChanged(MidiDevice oldMd, MidiDevice newMd)
    {
        var oldSynth = oldMd == null ? null : getMidiDeviceOutputSynth(oldMd.getDeviceInfo().getName());
        var newSynth = newMd == null ? null : getMidiDeviceOutputSynth(newMd.getDeviceInfo().getName());
        pcs.firePropertyChange(PROP_DEFAULT_OUTPUTSYNTH, oldSynth, newSynth);   // newSynth might be null !
    }

    private void store(String mdOutName, OutputSynth outSynth)
    {
        prefs.put(mdOutName, outSynth.saveAsString());
    }

}
