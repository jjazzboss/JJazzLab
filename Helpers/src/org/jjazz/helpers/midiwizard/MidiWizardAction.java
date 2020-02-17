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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JComponent;
import org.jjazz.helpers.midiwizard.MidiWizardSettings.SoundDevice;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.outputsynth.OS_JJazzLabSoundFont;
import org.jjazz.outputsynth.OutputSynthManager;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

/**
 * Start the Midi configuration wizard.
 * <p>
 * Automatically started upon first clean start (after installation).
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.helpers.midiwizard.MidiWizardAction")
@ActionRegistration(displayName = "Midi configuration wizard...")
@ActionReference(path = "Menu/Tools", position = 1650)
@OnShowing          // Used to check if it's the first clean start after installation
public final class MidiWizardAction implements ActionListener, Runnable
{

    private static Preferences prefs = NbPreferences.forModule(MidiWizardAction.class);

    private static final String PREF_CLEAN_START = "CleanStart";
    private static final Logger LOGGER = Logger.getLogger(MidiWizardAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
        panels.add(new MidiWizardPanel1());
        panels.add(new MidiWizardPanel2());
        panels.add(new MidiWizardPanel3());
        panels.add(new MidiWizardPanel4());
        panels.add(new MidiWizardPanel5());
        panels.add(new MidiWizardPanel6());
        String[] steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++)
        {
            Component c = panels.get(i).getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent)
            { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<WizardDescriptor>(panels));
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle("Midi configuration wizard");

        // First show the special use JJazzLab soundfont dialog
        UseJJazzLabSoundFontDialog dlg = new UseJJazzLabSoundFontDialog(WindowManager.getDefault().getMainWindow(), true);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        if (!dlg.isExitOk())
        {
            return;
        }
        if (dlg.isYesAnswer())
        {
            useJJazzLabSoundFont();
            return;
        }

        // Wizard
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION)
        {
            // Retrieve configuration then apply it
            SoundDevice sd = (SoundDevice) wiz.getProperty(MidiWizardSettings.PROP_SOUND_DEVICE);
            MidiDevice md = (MidiDevice) wiz.getProperty(MidiWizardSettings.PROP_MIDI_OUT_DEVICE);
            boolean gm2DrumsSupport = (Boolean) wiz.getProperty(MidiWizardSettings.PROP_GM2_DRUMS_SUPPORT);

            // Midi device OUT
            JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
            try
            {
                jms.setDefaultOutDevice(md);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.warning("actionPerformed() ex=" + ex.getLocalizedMessage());
                NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }

            // Default Drums instruments
//            Instrument ins = JJazzSynth.getInstance().getNotSetBank().getVoidInstrument();
//            if (gm2DrumsSupport)
//            {
//                ins = StdSynth.getInstance().getGM2Bank().getDefaultDrumsInstrument();
//            }
//            DefaultInstruments di = DefaultInstruments.getInstance();
//            di.setInstrument(RvType.Drums, ins);
//            di.setInstrument(RvType.Percussion, ins);
        }
    }

    @Override
    public void run()
    {
        if (prefs.getBoolean(PREF_CLEAN_START, true))
        {
            actionPerformed(null);
        }
        prefs.putBoolean(PREF_CLEAN_START, false);
    }

    /**
     * Do what's required to use the JJazzLab SoundFont.
     */
    private void useJJazzLabSoundFont()
    {
        // Set the output synth
        OutputSynthManager osm = OutputSynthManager.getInstance();
        osm.setOutputSynth(OS_JJazzLabSoundFont.getInstance());

        // Check the midi out device
        if (Utilities.isWindows())
        {
            // Check if VirtualMidi port is there
            boolean virtualMidiSynthSet = false;
            JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
            MidiDevice md = jms.getDefaultOutDevice();
            if (isVirtualMidiSynthDevice(md))
            {
                // GOOD: VirtualMidi port is already the out device
                virtualMidiSynthSet = true;
            } else
            {
                // Check if it's installed, if yes use it
                for (MidiDevice mdi : jms.getOutDeviceList())
                {
                    if (isVirtualMidiSynthDevice(mdi))
                    {
                        try
                        {
                            jms.setDefaultOutDevice(mdi);
                            virtualMidiSynthSet = true;
                        } catch (MidiUnavailableException ex)
                        {
                            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                            DialogDisplayer.getDefault().notify(nd);
                        }
                    }
                }
            }

            //if (virtualMidiSynthSet)
            if (true)
            {
                String msg = "JJazzLab is now configured to use VirtualMidiSynth. Please make sure VirtualMidiSynth has loaded the JJazzLab SoundFont."
                        + "\nConsult <html><a href=www.jjazzlab.com/en/doc>www.jjazzlab.com/en/doc</a></html> for details how to download the SoundFont and how to configure VirtualMidiSynth.";
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                return;
            }

        } else
        {

        }
    }

    private boolean isVirtualMidiSynthDevice(MidiDevice md)
    {
        return md != null && md.getDeviceInfo().getName().toLowerCase().contains("virtualmidisynt");
    }
}
