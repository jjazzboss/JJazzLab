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

import javax.sound.midi.MidiDevice;
import javax.swing.event.ChangeListener;
import org.jjazz.midi.JJazzMidiSystem;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class MidiWizardPanel6 implements WizardDescriptor.Panel<WizardDescriptor>
{

    /**
     * The visual component that displays this panel. If you need to access the component from this class, just use
     * getComponent().
     */
    private MidiWizardVisualPanel6 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public MidiWizardVisualPanel6 getComponent()
    {
        if (component == null)
        {
            component = new MidiWizardVisualPanel6();
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
    public void addChangeListener(ChangeListener l)
    {
    }

    @Override
    public void removeChangeListener(ChangeListener l)
    {
    }

    @Override
    public void readSettings(WizardDescriptor wiz)
    {
        StringBuilder sb = new StringBuilder();
        MidiDevice md = (MidiDevice) wiz.getProperty(MidiWizardSettings.PROP_MIDI_OUT_DEVICE);
        String mdName = JJazzMidiSystem.getInstance().getDeviceFriendlyName(md);
        boolean gm2DrumsSupport = (Boolean) wiz.getProperty(MidiWizardSettings.PROP_GM2_DRUMS_SUPPORT);
        String drumKit = gm2DrumsSupport ? "GM2 Drum Kit Standard" : "Not Set";

        sb.append("Set Midi Out device to : " + mdName + "\n\n");
        sb.append("Set drums and percussion default instruments to : " + drumKit);

        component.setChangesDescription(sb.toString());
    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
    }

}
