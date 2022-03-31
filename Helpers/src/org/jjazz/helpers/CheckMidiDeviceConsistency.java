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
package org.jjazz.helpers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.helpers.midiwizard.MidiWizardAction;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.outputsynth.api.OS_JJazzLabSoundFont_GS;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Utilities;
import org.openide.windows.OnShowing;

/**
 * Check consistency between selected Midi out device and the output synth configuration.
 * <p>
 * Targets an issue when first installing JJazzLab on Windows using the wizard to use VirtualMidiSytnh + JJazzLab, but user
 * defered installation of VirtualMidiSynth. In this case output synth is set to JJazzLab/VMS, but MidiDevice not set to VMS (user
 * forgot to do it, or will do it later).
 */
@OnShowing              // Used only to get the automatic object creation upon startup
public class CheckMidiDeviceConsistency implements VetoableChangeListener, Runnable
{

    private static boolean dontAskAnymore = false;

    private static final Logger LOGGER = Logger.getLogger(CheckMidiDeviceConsistency.class.getSimpleName());

    public CheckMidiDeviceConsistency()
    {
        // Register for song playback
        PlaybackSettings.getInstance().addPlaybackStartVetoableListener(this);
    }

    @Override
    public void run()
    {
        // Do nothing, we just use @OnShowing just to get the automatic object creation...
    }

    /**
     * Listen to pre-playback events.
     *
     * @param evt
     * @throws PropertyVetoException Not used
     */
    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException
    {
        LOGGER.log(Level.FINE, "vetoableChange() -- evt={0}", evt);   //NOI18N

        if (evt.getSource() != PlaybackSettings.getInstance()
                || !evt.getPropertyName().equals(PlaybackSettings.PROP_VETO_PRE_PLAYBACK)
                || !MusicController.getInstance().getState().equals(MusicController.State.STOPPED))  // Don't check in pause mode
        {
            return;
        }

        SongContext context = (SongContext) evt.getNewValue();
        if (context == null)
        {
            // No context, we can't check anything
            return;
        }

        if (!Utilities.isWindows())
        {
            return;
        }

        // Default midi out device
        var jms = JJazzMidiSystem.getInstance();
        var mdOut = jms.getDefaultOutDevice();
        if (mdOut == null)
        {
            return;
        }
        OutputSynth outputSynth = OutputSynthManager.getInstance().getOutputSynth();
        var vmsDevice = jms.getVirtualMidiSynthDevice();


        if (OS_JJazzLabSoundFont_GS.getInstance().isCompatibleWith(outputSynth))
        {
            // OutputSynth is set to JJazzLab SoundFont + VirtualMidiSynth: check that Midi out device is consistent

            if (vmsDevice == null)
            {
                if (!dontAskAnymore)
                {
                    // User needs to install VirtualMidiSynth
                    String msg = ResUtil.getString(getClass(), "ERR_VMS_MidiDeviceExpectedButNotFound");
                    LOGGER.warning("vetoableChange() " + msg);   //NOI18N
                    NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                }
                dontAskAnymore = true;

            } else if (mdOut == vmsDevice)
            {
                // Setup is OK, do nothing

            } else if (!dontAskAnymore)
            {
                // vmsDevice is present but not used, fix it if user is OK 
                String msg = ResUtil.getString(getClass(), "ERR_VMS_MidiDeviceShouldBeUsed");
                LOGGER.warning("vetoableChange() " + msg);   //NOI18N
                NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                Object result = DialogDisplayer.getDefault().notify(nd);


                if (NotifyDescriptor.YES_OPTION == result)
                {
                    try
                    {
                        jms.setDefaultOutDevice(vmsDevice);
                    } catch (MidiUnavailableException ex)
                    {
                        LOGGER.warning("vetoableChange() ex=" + ex.getMessage());   //NOI18N
                        nd = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notify(nd);
                    }

                } else
                {
                    // Used chose to not fix it, stop asking
                    dontAskAnymore = true;
                }
            }
        } else
        {
            // OutputSynth is NOT set to VirtualMidiSynth+JJazzLab SoundFont. 
            if (mdOut == vmsDevice && !dontAskAnymore)
            {
                // Ask user to apply the VirtualMidiSynth preset               
                String msg = ResUtil.getString(getClass(), "ERR_VMSOutputSynthShouldBeUsed");
                LOGGER.warning("vetoableChange() " + msg);   //NOI18N
                NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                Object result = DialogDisplayer.getDefault().notify(nd);

                if (NotifyDescriptor.YES_OPTION == result)
                {
                    // Update the OutputSynth
                    File f = outputSynth.getFile();
                    OutputSynthManager osm = OutputSynthManager.getInstance();
                    OutputSynth outSynth = new OutputSynth(OS_JJazzLabSoundFont_GS.getInstance());
                    outSynth.setFile(f);
                    osm.setOutputSynth(outSynth);
                    if (f != null)
                    {
                        try
                        {
                            outSynth.saveToFile(f);
                            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(MidiWizardAction.class, "MidiWizardAction.CTL_Saved", f.getAbsolutePath()));
                        } catch (IOException ex)
                        {
                            LOGGER.warning("vetoableChange() " + ex.getMessage());   //NOI18N
                            nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                            DialogDisplayer.getDefault().notify(nd);
                        }
                    }

                } else
                {
                    // Used chose to not fix it, stop asking
                    dontAskAnymore = true;
                }

            }
        }

    }

}
