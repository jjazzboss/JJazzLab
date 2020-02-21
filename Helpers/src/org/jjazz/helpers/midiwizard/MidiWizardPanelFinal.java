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

import java.io.File;
import javax.sound.midi.MidiDevice;
import javax.swing.event.ChangeListener;
import org.jjazz.midi.JJazzMidiSystem;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;

public class MidiWizardPanelFinal implements WizardDescriptor.Panel<WizardDescriptor>
{

    /**
     * The visual component that displays this panel. If you need to access the component from this class, just use getComponent().
     */
    private MidiWizardVisualFinal component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public MidiWizardVisualFinal getComponent()
    {
        if (component == null)
        {
            component = new MidiWizardVisualFinal();
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
        // Retrieve Wizard data
        boolean useJJazzLabSoundFont = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_USE_JJAZZLAB_SOUNDFONT);
        MidiDevice md = (MidiDevice) wiz.getProperty(MidiWizardAction.PROP_MIDI_OUT_DEVICE);
        String mdName = md != null ? JJazzMidiSystem.getInstance().getDeviceFriendlyName(md) : "";
        boolean gm2Support = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_GM2_SUPPORT);
        boolean xgSupport = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_XG_SUPPORT);
        boolean gsSupport = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_GS_SUPPORT);
        File soundFontFile = (File) wiz.getProperty(MidiWizardAction.PROP_JJAZZLAB_SOUNDFONT_FILE);

        StringBuilder sb = new StringBuilder();
        if (useJJazzLabSoundFont)
        {
            String presetName = "JJazzLab SoundFont/VirtualMidiSynth (Windows)";
            if (Utilities.isUnix())
            {
                presetName = "JJazzLab SoundFont/FluidSynth (Linux)";
            } else if (Utilities.isMac())
            {
                presetName = "JJazzLab SoundFont/Java Internal Synth (Mac)";
            }
            sb.append("- Set Output synth config. : " + presetName + "\n\n");
            if (md != null)
            {
                sb.append("- Set Midi Out device : " + mdName + "\n\n");
            }
            if (Utilities.isMac() && soundFontFile != null)
            {
                sb.append("- Load sound file : " + soundFontFile.getAbsolutePath());
            }
        } else
        {
            String stdSupport = "GM ";
            if (gm2Support)
            {
                stdSupport += ", GM2 ";
            }
            if (xgSupport)
            {
                stdSupport += ", XG ";
            }
            if (gsSupport && !xgSupport)
            {
                stdSupport += ", GS ";
            }
            sb.append("- Set Output synth config. : ").append(stdSupport).append("\n\n");
            sb.append("- Set Midi Out device : ").append(mdName).append("\n\n");
        }
        component.setChangesDescription(sb.toString());
    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
    }

}
