/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.helpers.midiwizard;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.midi.JJazzMidiSystem;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;

public class MidiWizardPanel_SfWin_1 implements WizardDescriptor.Panel<WizardDescriptor>
{

    private Set<ChangeListener> listeners = new HashSet<ChangeListener>(2);
    private static final Logger LOGGER = Logger.getLogger(MidiWizardPanel_SfWin_1.class.getSimpleName());
    /**
     * The visual component that displays this panel. If you need to access the component from this class, just use
     * getComponent().
     */
    private MidiWizardVisualPanel_SfWin_1 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public MidiWizardVisualPanel_SfWin_1 getComponent()
    {
        if (component == null)
        {
            component = new MidiWizardVisualPanel_SfWin_1();
        }
        return component;
    }

    @Override
    public HelpCtx getHelp()
    {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }

    @Override
    public boolean isValid()
    {
        // If it is always OK to press Next or Finish, then:
        return true;
        // If it depends on some condition (form filled out...) and
        // this condition changes (last form field filled in...) then
        // use ChangeSupport to implement add/removeChangeListener below.
        // WizardDescriptor.ERROR/WARNING/INFORMATION_MESSAGE will also be useful.
    }

    @Override
    public final void addChangeListener(ChangeListener l)
    {
        synchronized (listeners)
        {
            listeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    protected final void fireChangeEvent()
    {
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener cl : listeners)
        {
            cl.stateChanged(ev);
        }
    }

    @Override
    public void readSettings(WizardDescriptor wiz)
    {
        // We store the property here instead of in storeSettings() because property only depends on external context and we need
        // the info to update the panel display
        MidiDevice md = getVirtualMidiSynthDevice();
        wiz.putProperty(MidiWizardAction.PROP_MIDI_OUT_DEVICE, md);     
        getComponent().setVmsDevice(md);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
        // see readSettings()
    }

  private MidiDevice getVirtualMidiSynthDevice()
    {
        assert Utilities.isWindows();
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        MidiDevice res = null;
        MidiDevice md = jms.getDefaultOutDevice();
        if (isVirtualMidiSynthDevice(md))
        {
            res = md;
        } else
        {
            // Search the available MidiDevices
            for (MidiDevice mdi : jms.getOutDeviceList())
            {
                if (isVirtualMidiSynthDevice(mdi))
                {
                    md = mdi;
                    break;
                }
            }
        }
        return res;
    }

    private boolean isVirtualMidiSynthDevice(MidiDevice md)
    {
        return md != null && md.getDeviceInfo().getName().toLowerCase().contains("virtualmidisynt");
    }
}
