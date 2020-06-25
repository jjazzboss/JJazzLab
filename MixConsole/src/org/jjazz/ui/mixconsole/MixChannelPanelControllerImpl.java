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
package org.jjazz.ui.mixconsole;

import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.instrumentchooser.spi.InstrumentChooserDialog;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.synths.Family;
import org.jjazz.midiconverters.api.ConverterManager;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.UserChannelRvKey;
import org.jjazz.musiccontrol.ClickManager;
import org.jjazz.outputsynth.OutputSynthManager;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;

/**
 *
 */
public class MixChannelPanelControllerImpl implements MixChannelPanelController
{

    private MidiMix midiMix;
    private int channelId;
    private static final Logger LOGGER = Logger.getLogger(MixChannelPanelControllerImpl.class.getSimpleName());

    /**
     * @param mMix The MidiMix containing all data of our model.
     * @param channel Used to retrieve the InstrumentMix from mMix.
     */
    public MixChannelPanelControllerImpl(MidiMix mMix, int channel)
    {
        if (mMix == null || !MidiConst.checkMidiChannel(channel) || mMix.getInstrumentMixFromChannel(channel) == null)
        {
            throw new IllegalArgumentException("mMix=" + mMix + " channel=" + channel);
        }
        channelId = channel;
        midiMix = mMix;
    }

    @Override
    public void editChannelId(String strNewChannelId)
    {
        int newChannelId;
        try
        {
            newChannelId = Integer.valueOf(strNewChannelId) - 1;
        } catch (NumberFormatException e)
        {
            return;
        }
        if (!MidiConst.checkMidiChannel(newChannelId) || newChannelId == channelId)
        {
            return;
        }

        // The current channel data
        InstrumentMix insMixSrc = midiMix.getInstrumentMixFromChannel(channelId);
        RhythmVoice rvKeySrc = midiMix.getRhythmVoice(channelId);


        // Check if we use drums channel for a non drums instrument
        if (newChannelId == MidiConst.CHANNEL_DRUMS && !rvKeySrc.isDrums() && !Family.couldBeDrums(insMixSrc.getInstrument().getPatchName()))
        {
            String msg = "Channel 10 is reserved for Drums instruments.";
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Can't override the click channel
        int clickChannel = ClickManager.getInstance().getClickChannel(midiMix);
        if (newChannelId == clickChannel && !rvKeySrc.isDrums())
        {
            String msg = "Channel " + (clickChannel + 1) + " is reserved for the click channel. See Click Options/Preferences to change this setting.";
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // The replaced channel data
        InstrumentMix insMixDest = midiMix.getInstrumentMixFromChannel(newChannelId);
        RhythmVoice rvKeyDest = midiMix.getRhythmVoice(newChannelId);


        // Perform the changes
        if (insMixDest == null)
        {
            // We don't replace an existing InstrumentMix

            if (rvKeySrc == UserChannelRvKey.getInstance())
            {
                // It's a user channel
                midiMix.removeUserChannel();
                try
                {
                    midiMix.addUserChannel(insMixSrc, newChannelId);
                } catch (MidiUnavailableException ex)
                {
                    // Should never happen since we removed the user channel just before
                    Exceptions.printStackTrace(ex);
                }
            } else
            {
                // Standard channel                          
                midiMix.setInstrumentMix(channelId, null, null);
                midiMix.setInstrumentMix(newChannelId, rvKeySrc, insMixSrc);
            }

        } else
        {
            // We replace an existing InstrumentMix, swap

            if (rvKeySrc == UserChannelRvKey.getInstance())
            {
                // Source is the user channel
                midiMix.removeUserChannel();    // free channelId
                midiMix.setInstrumentMix(newChannelId, null, null);
                try
                {
                    midiMix.addUserChannel(insMixSrc, newChannelId);
                } catch (MidiUnavailableException ex)
                {
                    // Should never happen since we removed the user channel just before
                    Exceptions.printStackTrace(ex);
                }
                midiMix.setInstrumentMix(channelId, rvKeyDest, insMixDest);

            } else if (rvKeyDest == UserChannelRvKey.getInstance())
            {
                // Destination is the user channel
                midiMix.setInstrumentMix(channelId, null, null);
                midiMix.removeUserChannel();    // free newChannelId                
                try
                {
                    midiMix.addUserChannel(insMixDest, channelId);
                } catch (MidiUnavailableException ex)
                {
                    // Should never happen since we removed the user channel just before
                    Exceptions.printStackTrace(ex);
                }
                midiMix.setInstrumentMix(newChannelId, rvKeySrc, insMixSrc);


            } else
            {
                // No user channel

                midiMix.setInstrumentMix(channelId, null, null);
                midiMix.setInstrumentMix(newChannelId, rvKeySrc, insMixSrc);
                midiMix.setInstrumentMix(channelId, rvKeyDest, insMixDest);
            }
        }
    }

    @Override
    public void editClose()
    {
        midiMix.setInstrumentMix(channelId, null, null);
    }

    @Override
    public void editSettings()
    {
        MixChannelPanelSettingsDialog dlg = MixChannelPanelSettingsDialog.getInstance();
        String title = buildTitle();
        dlg.preset(midiMix, channelId, title);
        dlg.setVisible(true);
    }

    @Override
    public void editInstrument()
    {
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channelId);
        RhythmVoice rv = midiMix.getRhythmVoice(channelId);
        InstrumentChooserDialog dlg = InstrumentChooserDialog.getDefault();
        dlg.preset(OutputSynthManager.getInstance().getOutputSynth(), rv, insMix.getInstrument(), insMix.getSettings().getTransposition(), channelId);
        dlg.setVisible(true);
        Instrument ins = dlg.getSelectedInstrument();
        if (ins != null)
        {
            // Warning if drums keymap is not compatible even via a converter
            if (rv.isDrums() && ins.isDrumKit())
            {
                DrumKit.KeyMap srcKeyMap = rv.getPreferredInstrument().getDrumKit().getKeyMap();
                DrumKit.KeyMap destKeyMap = ins.getDrumKit().getKeyMap();
                if (destKeyMap.isContaining(srcKeyMap))
                {
                    // No problem, do nothing
                } else if (ConverterManager.getInstance().getKeyMapConverter(srcKeyMap, destKeyMap) == null)
                {
                    // No conversion possible
                    String msg = "Selected instrument (" + ins.getPatchName() + ", drum keymap=" + destKeyMap.getName() + ") can't match the recommended original drum keymap " + srcKeyMap.getName() + "."
                            + "\n This may result in incorrect sounds. Do you want to continue ?";
                    NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                    Object result = DialogDisplayer.getDefault().notify(d);
                    if (NotifyDescriptor.YES_OPTION != result)
                    {
                        return;
                    }
                } else
                {
                    // Managed via conversion
                    LOGGER.info("editInstrument() channel=" + channelId + " ins=" + ins.getPatchName() + ": drum keymap conversion will be used " + srcKeyMap + ">" + destKeyMap);
                    StatusDisplayer.getDefault().setStatusText("Using drum keymap conversion " + srcKeyMap + ">" + destKeyMap + " for " + ins.getPatchName() + " (channel " + channelId + ")");
                }
            }
            insMix.setInstrument(ins);
            insMix.getSettings().setTransposition(dlg.getTransposition());
        }
    }

    @Override
    public void editNextInstrument()
    {
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channelId);
        InstrumentBank<?> bank = insMix.getInstrument().getBank();
        Instrument ins = bank.getNextInstrument(insMix.getInstrument());
        insMix.setInstrument(ins);
    }

    @Override
    public void editPreviousInstrument()
    {
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channelId);
        InstrumentBank<?> bank = insMix.getInstrument().getBank();
        Instrument ins = bank.getPreviousInstrument(insMix.getInstrument());
        insMix.setInstrument(ins);
    }

    private String buildTitle()
    {
        StringBuilder title = new StringBuilder("Channel " + (channelId + 1));
        RhythmVoice rv = midiMix.getRhythmVoice(channelId);
        if (rv instanceof UserChannelRvKey)
        {
            title.append(" - User");
        } else
        {
            title.append(" - " + rv.getContainer().getName() + " - " + rv.getName());
        }
        return title.toString();
    }
}
