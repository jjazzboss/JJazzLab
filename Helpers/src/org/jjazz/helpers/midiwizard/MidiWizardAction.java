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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.midi.JJazzMidiSystem;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbPreferences;
import org.openide.windows.OnShowing;

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

    // SoundFont sequence data
    public static final String PROP_USE_JJAZZLAB_SOUNDFONT = "UseJJazzLabSoundFont";

    // Midi config sequence data
    public enum SoundDevice
    {
        SYNTHESIZER, VIRTUAL_INSTRUMENT, OTHER
    }
    public static String PROP_SOUND_DEVICE = "PropSoundDevice";
    public static String PROP_MIDI_OUT_DEVICE = "PropMidiOutDevice";
    public static String PROP_GM2_DRUMS_SUPPORT = "PropGM2DrumsChannel";

    private static Preferences prefs = NbPreferences.forModule(MidiWizardAction.class);

    private static final String PREF_CLEAN_START = "CleanStart";
    private static final Logger LOGGER = Logger.getLogger(MidiWizardAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MidiWizardIterator iterator = new MidiWizardIterator();
        WizardDescriptor wiz = new WizardDescriptor(iterator);
        iterator.setWizardDescriptor(wiz);
        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
        wiz.setTitle("Midi configuration wizard");
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION)
        {
            // Retrieve configuration then apply it
            SoundDevice sd = (SoundDevice) wiz.getProperty(PROP_SOUND_DEVICE);
            MidiDevice md = (MidiDevice) wiz.getProperty(PROP_MIDI_OUT_DEVICE);
            boolean gm2DrumsSupport = getBooleanProp(wiz, PROP_GM2_DRUMS_SUPPORT);

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

        }
    }

    public static boolean getBooleanProp(WizardDescriptor wiz, String prop)
    {
        Boolean b = (Boolean) wiz.getProperty(prop);
        return b == null ? false : b;
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

}
