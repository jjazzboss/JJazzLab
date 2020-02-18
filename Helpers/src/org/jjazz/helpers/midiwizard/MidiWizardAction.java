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

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.outputsynth.OS_JJazzLabSoundFont;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
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
    public static String PROP_MIDI_OUT_DEVICE = "PropMidiOutDevice";
    // Midi sequence data
    public static String PROP_GM2_SUPPORT = "PropGM2Support";
    public static String PROP_GS_SUPPORT = "PropGSSupport";
    public static String PROP_XG_SUPPORT = "PropXGSupport";

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
        wiz.setTitle("Midi Configuration Wizard");
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION)
        {
            // Retrieve configuration then apply it
            boolean useJJazzLabSoundFont = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_USE_JJAZZLAB_SOUNDFONT);
            MidiDevice md = (MidiDevice) wiz.getProperty(MidiWizardAction.PROP_MIDI_OUT_DEVICE);
            String mdName = md != null ? JJazzMidiSystem.getInstance().getDeviceFriendlyName(md) : "";
            boolean gm2Support = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_GM2_SUPPORT);
            boolean xgSupport = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_XG_SUPPORT);
            boolean gsSupport = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_GS_SUPPORT);

            // Midi device OUT
            if (md != null)
            {
                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                try
                {
                    jms.setDefaultOutDevice(md);
                } catch (MidiUnavailableException ex)
                {
                    LOGGER.warning("actionPerformed() Can't set default Midi out device to " + mdName + ". ex=" + ex.getLocalizedMessage());
                    NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                }
            }
            OutputSynthManager osm = OutputSynthManager.getInstance();
            OutputSynth os = osm.getOutputSynth();
            File f = os.getFile();
            if (useJJazzLabSoundFont)
            {
                // Use the preset output synth
                os = new OutputSynth(OS_JJazzLabSoundFont.getInstance());
                if (f != null)
                {
                    os.setFile(f);
                }
                osm.setOutputSynth(os);
            } else
            {
                // Update the std banks compatibility                
                os.reset();
                if (gm2Support)
                {
                    os.setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.GM2);
                    os.addCompatibleStdBank(StdSynth.getInstance().getGM2Bank());
                }
                if (xgSupport)
                {
                    os.setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.XG);
                    os.removeCompatibleStdBank(GSSynth.getInstance().getGSBank());
                    os.addCompatibleStdBank(StdSynth.getInstance().getXGBank());
                }
                if (gsSupport && !xgSupport)
                {
                    os.setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.GS);
                    os.removeCompatibleStdBank(StdSynth.getInstance().getXGBank());
                    os.addCompatibleStdBank(GSSynth.getInstance().getGSBank());
                }
            }
            if (f != null)
            {
                try
                {
                    os.saveToFile(f);
                    StatusDisplayer.getDefault().setStatusText("Saved " + f.getAbsolutePath());
                } catch (IOException ex)
                {
                    String msg = "Problem saving output synth file " + f.getName() + " : " + ex.getLocalizedMessage();
                    LOGGER.warning("actionPerformed() " + msg);
                    NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                }
            }
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

    public static void openInBrowser(URL url)
    {
        String errMsg = null;
        if (Desktop.isDesktopSupported())
        {
            try
            {
                Desktop.getDesktop().browse(url.toURI());
            } catch (URISyntaxException | IOException ex)
            {
                errMsg = ex.getLocalizedMessage();
            }
        } else
        {
            errMsg = "Open hyperlink in browser not supported";
        }
        if (errMsg != null)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(errMsg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    public static boolean getBooleanProp(WizardDescriptor wiz, String prop)
    {
        Boolean b = (Boolean) wiz.getProperty(prop);
        return b == null ? false : b;
    }

}
