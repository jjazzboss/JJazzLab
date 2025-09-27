/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2025 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.mixconsole;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.mixconsole.actions.SendGm2On;
import org.jjazz.mixconsole.actions.SendGmOn;
import org.jjazz.mixconsole.actions.SendGsOn;
import org.jjazz.mixconsole.actions.SendXgOn;
import org.openide.awt.Actions;
import org.openide.modules.OnStart;

/**
 * This class disables the midi messages actions when FluidSymth is used and
 * enables them in any other case.
 *
 * This class registers itself to listen for changes in {@link JJazzMidiSystem#PROP_MIDI_OUT}
 * on application startup.
 */
@OnStart
public class SynthChangesListener implements PropertyChangeListener, Runnable {
    private static final Logger LOGGER = Logger.getLogger(SynthChangesListener.class.getSimpleName());

    /**
     * NOTE: This method expects only changes for {@link JJazzMidiSystem#PROP_MIDI_OUT},
     * so objects of this class should be registered using
     * {@link JJazzMidiSystem#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     * with {@link JJazzMidiSystem#PROP_MIDI_OUT} as the first  parameter.
     * @param evt 
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        
        boolean enable = !this.isFluidSynth(evt.getNewValue());

        setActionEnabled(SendGmOn.ACTION_CATEGORY, SendGmOn.ACTION_ID, enable);
        setActionEnabled(SendGm2On.ACTION_CATEGORY, SendGm2On.ACTION_ID, enable);
        setActionEnabled(SendXgOn.ACTION_CATEGORY, SendXgOn.ACTION_ID, enable);
        setActionEnabled(SendGsOn.ACTION_CATEGORY, SendGsOn.ACTION_ID, enable);
    }

    private void setActionEnabled(String category, String id, boolean enable) {
        var action = Actions.forID(category, id);
        if (action != null)
        {
            action.setEnabled(enable);
        }
    }

    private boolean isFluidSynth(Object midiDeviceObj)
    {
        if (midiDeviceObj != null);
        {
            try
            {
                MidiDevice midiDevice = (MidiDevice) midiDeviceObj;
                if (midiDevice != null)
                {
                    MidiDevice.Info info = midiDevice.getDeviceInfo();
                    return info.getName().toLowerCase().contains("fluidsynth");
                }
            }
            catch(Exception e)
            {
                return false;
            }
        }
        return false;
    }

    @Override
    public void run() {
        JJazzMidiSystem jms =  JJazzMidiSystem.getInstance();
        jms.addPropertyChangeListener(JJazzMidiSystem.PROP_MIDI_OUT, this);
    }
}
