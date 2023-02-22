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
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.startup.spi.StartupTask;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.util.api.ResUtil;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.util.lookup.ServiceProvider;

/**
 * Start the Midi configuration wizard.
 * <p>
 * Automatically started upon first clean start (after installation).
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.helpers.midiwizard.MidiWizardAction")
@ActionRegistration(displayName = "#MidiWizardAction.CTL_MidiConfigWizardAction")
@ActionReference(path = "Menu/Tools", position = 1650, separatorAfter = 1651)
public final class MidiWizardAction implements ActionListener
{

    // SoundFont sequence data
    public static final String PROP_USE_JJAZZLAB_SOUNDFONT = "UseJJazzLabSoundFont";    
    public static String PROP_MIDI_OUT_DEVICE = "PropMidiOutDevice";
    public static String PROP_JJAZZLAB_SOUNDFONT_FILE = "PropJJazzLabSoundFontFile";

    // Midi sequence data
    public static String PROP_GM2_SUPPORT = "PropGM2Support";
    public static String PROP_GS_SUPPORT = "PropGSSupport";
    public static String PROP_XG_SUPPORT = "PropXGSupport";

    private static final Logger LOGGER = Logger.getLogger(MidiWizardAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.severe("actionPerformed() COMMENTED OUT!!");
//        
//        
//        MidiWizardIterator iterator = new MidiWizardIterator();
//        WizardDescriptor wiz = new WizardDescriptor(iterator);
//        iterator.setWizardDescriptor(wiz);
//        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
//        // {1} will be replaced by WizardDescriptor.Iterator.name()
//        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
//        wiz.setTitle(ResUtil.getString(getClass(), "MidiWizardAction.CTL_MidiConfigWizard"));
//        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION)
//        {
//            // Retrieve configuration then apply it
//            JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
//            boolean useJJazzLabSoundFont = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_USE_JJAZZLAB_SOUNDFONT);
//            MidiDevice md = (MidiDevice) wiz.getProperty(MidiWizardAction.PROP_MIDI_OUT_DEVICE);
//            boolean gm2Support = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_GM2_SUPPORT);
//            boolean xgSupport = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_XG_SUPPORT);
//            boolean gsSupport = MidiWizardAction.getBooleanProp(wiz, MidiWizardAction.PROP_GS_SUPPORT);
//            File soundFontFile = (File) wiz.getProperty(MidiWizardAction.PROP_JJAZZLAB_SOUNDFONT_FILE);
//
//            // Midi device OUT
//            if (md != null)
//            {
//
//                try
//                {
//                    jms.setDefaultOutDevice(md);
//                } catch (MidiUnavailableException ex)
//                {
//                    LOGGER.warning("actionPerformed() Can't set default Midi out device to " + JJazzMidiSystem.getInstance().getDeviceFriendlyName(md) + ". ex=" + ex.getMessage());   
//                    NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
//                    DialogDisplayer.getDefault().notify(nd);
//                }
//            }
//
//            OutputSynthManager osm = OutputSynthManager.getInstance();
//            OutputSynth os = osm.getDefaultOutputSynth();
//            File f = os.getFile();
//            if (useJJazzLabSoundFont)
//            {
//                // Use the preset output synth
//                if (Utilities.isMac())
//                {
//                    os = new OutputSynth(OS_JJazzLabSoundFont_GM2.getInstance());
//                    if (jms.getDefaultOutDevice() == jms.getJavaInternalSynth() && soundFontFile != null)
//                    {
//                        // Load the soundfont file
//                        jms.loadSoundbankFileOnSynth(soundFontFile, false);
//                    }
//                } else if (Utilities.isUnix())
//                {
//                    os = new OutputSynth(OS_JJazzLabSoundFont_XG.getInstance());
//                } else
//                {
//                    // Win
//                    os = new OutputSynth(OS_JJazzLabSoundFont_GS.getInstance());
//                }
//                if (f != null)
//                {
//                    os.setFile(f);
//                }
//                osm.setOutputSynth(os);
//            } else
//            {
//                // Update the std banks compatibility                
//                os.reset();
//                if (gm2Support)
//                {
//                    os.setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.GM2);
//                    os.addCompatibleStdBank(StdSynth.getInstance().getGM2Bank());
//                }
//                if (xgSupport)
//                {
//                    os.setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.XG);
//                    os.removeCompatibleStdBank(GSSynth.getInstance().getGSBank());
//                    os.addCompatibleStdBank(StdSynth.getInstance().getXGBank());
//                }
//                if (gsSupport && !xgSupport)
//                {
//                    os.setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.GS);
//                    os.removeCompatibleStdBank(StdSynth.getInstance().getXGBank());
//                    os.addCompatibleStdBank(GSSynth.getInstance().getGSBank());
//                }
//            }
//            if (f != null)
//            {
//                try
//                {
//                    os.saveToFile(f);
//                    StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "MidiWizardAction.CTL_Saved", f.getAbsolutePath()));
//                } catch (IOException ex)
//                {
//                    String msg = ResUtil.getString(getClass(), "MidiWizardAction.CTL_ErrSavingOutputFile", f.getName()) + " : " + ex.getLocalizedMessage();
//                    LOGGER.warning("actionPerformed() " + msg);   
//                    NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
//                    DialogDisplayer.getDefault().notify(nd);
//                }
//            }
//        }
    }

    public static boolean getBooleanProp(WizardDescriptor wiz, String prop)
    {
        Boolean b = (Boolean) wiz.getProperty(prop);
        return b == null ? false : b;
    }

    // =====================================================================================
    // Startup Task
    // =====================================================================================
    @ServiceProvider(service = StartupTask.class)
    static public class FreshStartupTask implements StartupTask
    {

        /**
         * Must be done last upon a fresh startup.
         */
        public static final int PRIORITY = 100000;

        @Override
        public boolean run()
        {
            // Show the wizard upon fresh start and only if no settings to import
            UpgradeManager um = UpgradeManager.getInstance();
            if (um.isFreshStart() && um.getImportSourceVersion() == null)
            {
                Action a = Actions.forID("JJazz", "org.jjazz.helpers.midiwizard.MidiWizardAction");   
                assert a != null;   
                a.actionPerformed(null);
                return true;
            } else
            {
                return false;
            }
        }

        @Override
        public int getPriority()
        {
            return PRIORITY;
        }

        @Override
        public String getName()
        {
            return ResUtil.getString(getClass(), "MidiWizardAction.CTL_MidiConfigWizard");
        }

    }

}
