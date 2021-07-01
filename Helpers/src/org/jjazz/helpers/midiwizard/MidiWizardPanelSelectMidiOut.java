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
import javax.sound.midi.MidiDevice;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class MidiWizardPanelSelectMidiOut implements WizardDescriptor.Panel<WizardDescriptor>, ListSelectionListener
{

    /**
     * The visual component that displays this panel. If you need to access the component from this class, just use
     * getComponent().
     */
    private MidiWizardVisualPanelSelectMidiOut component;
    private Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public MidiWizardVisualPanelSelectMidiOut getComponent()
    {
        if (component == null)
        {
            component = new MidiWizardVisualPanelSelectMidiOut();
            component.getOutDeviceList().addListSelectionListener(this);
        }
        return component;
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            fireChangeEvent();
        }
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
        return component.getSelectedOutDevice() != null;
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
        // use wiz.getProperty to retrieve previous panel state
        MidiDevice md = (MidiDevice) wiz.getProperty(MidiWizardAction.PROP_MIDI_OUT_DEVICE);
        if (md == null)
        {
            md = JJazzMidiSystem.getInstance().getDefaultOutDevice();
        }
        component.setSelectedOutDevice(md);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
        wiz.putProperty(MidiWizardAction.PROP_MIDI_OUT_DEVICE, component.getSelectedOutDevice());
    }

}
