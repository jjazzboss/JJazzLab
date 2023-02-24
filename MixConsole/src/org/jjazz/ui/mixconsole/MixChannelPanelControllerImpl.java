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

import com.google.common.base.Preconditions;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.instrumentchooser.spi.InstrumentChooserDialog;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.keymap.StandardKeyMapConverter;
import org.jjazz.midi.api.synths.Family;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;

/**
 *
 */
public class MixChannelPanelControllerImpl implements MixChannelPanelController
{

    private Song song;
    private MidiMix midiMix;
    private static final Logger LOGGER = Logger.getLogger(MixChannelPanelControllerImpl.class.getSimpleName());


    public MixChannelPanelControllerImpl(Song song, MidiMix midiMix)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        this.song = song;
        this.midiMix = midiMix;
    }

    @Override
    public void editChannel(int channel, String strNewChannel)
    {
        int newChannel;
        try
        {
            newChannel = Integer.valueOf(strNewChannel) - 1;
        } catch (NumberFormatException e)
        {
            return;
        }
        if (!MidiConst.checkMidiChannel(newChannel) || newChannel == channel)
        {
            return;
        }


        // The current channel data
        InstrumentMix insMixSrc = midiMix.getInstrumentMixFromChannel(channel);
        assert insMixSrc != null : "midiMix=" + midiMix + " channel=" + channel;
        RhythmVoice rvSrc = midiMix.getRhythmVoice(channel);


        // Check if we use drums channel for a non drums instrument
        if (newChannel == MidiConst.CHANNEL_DRUMS && !rvSrc.isDrums() && !Family.couldBeDrums(insMixSrc.getInstrument().getPatchName()))
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.Channel10reserved");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Can't override the click channel
        int clickChannel = PlaybackSettings.getInstance().getClickChannel(midiMix);
        if (newChannel == clickChannel && !rvSrc.isDrums())
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.Channel10ClickReserved", clickChannel + 1);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Make sure new channel is free
        if (midiMix.getRhythmVoice(newChannel) != null)
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.ChannelInUse", newChannel + 1);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Undoable event
        String undoText = "Change channel";
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(undoText);

        midiMix.setRhythmVoiceChannel(rvSrc, newChannel);

        um.endCEdit(undoText);
    }

    @Override
    public void editClose(int channel)
    {
        midiMix.setInstrumentMix(channel, null, null);
    }

    @Override
    public void editSettings(int channel)
    {
        MixChannelPanelSettingsDialog dlg = MixChannelPanelSettingsDialog.getInstance();
        String title = buildTitle(channel);
        dlg.preset(midiMix, channel, title);
        dlg.setVisible(true);
    }

    @Override
    public void editInstrument(int channel)
    {
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channel);
        RhythmVoice rv = midiMix.getRhythmVoice(channel);
        InstrumentChooserDialog dlg = InstrumentChooserDialog.getDefault();
        dlg.preset(OutputSynthManager.getInstance().getDefaultOutputSynth(), rv, insMix.getInstrument(),
                insMix.getSettings().getTransposition(), channel);
        dlg.setVisible(true);


        Instrument ins = dlg.getSelectedInstrument();
        if (ins != null)
        {
            // Warning if drums keymap is not compatible even via a converter
            if (rv.isDrums() && ins.isDrumKit())
            {
                DrumKit.KeyMap srcKeyMap = rv.getDrumKit().getKeyMap();
                DrumKit.KeyMap destKeyMap = ins.getDrumKit().getKeyMap();
                if (destKeyMap.isContaining(srcKeyMap))
                {
                    // No problem, do nothing
                } else if (!StandardKeyMapConverter.accept(srcKeyMap, destKeyMap))
                {
                    // No conversion possible

                    String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.DrumKeyMapMismatch", ins.getPatchName(),
                            destKeyMap.getName(), srcKeyMap.getName());
                    NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                    Object result = DialogDisplayer.getDefault().notify(d);
                    if (NotifyDescriptor.YES_OPTION != result)
                    {
                        return;
                    }
                } else
                {
                    // Managed via conversion
                    LOGGER.log(Level.INFO, "editInstrument() channel={0} ins={1}: drum keymap conversion will be used {2}>{3}", new Object[]
                    {
                        channel, ins.getPatchName(), srcKeyMap, destKeyMap
                    });
                    String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.DrumKeyMapConversion",
                            srcKeyMap.getName(), destKeyMap.getName(), ins.getPatchName(), (channel + 1));
                    StatusDisplayer.getDefault().setStatusText(msg);
                }
            }
            insMix.setInstrument(ins);
            insMix.getSettings().setTransposition(dlg.getTransposition());
        }
    }

    @Override
    public void editNextInstrument(int channel)
    {
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channel);
        InstrumentBank<?> bank = insMix.getInstrument().getBank();
        Instrument ins = bank.getNextInstrument(insMix.getInstrument());
        insMix.setInstrument(ins);
    }

    @Override
    public void editPreviousInstrument(int channel)
    {
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channel);
        InstrumentBank<?> bank = insMix.getInstrument().getBank();
        Instrument ins = bank.getPreviousInstrument(insMix.getInstrument());
        insMix.setInstrument(ins);
    }

    private String buildTitle(int channel)
    {
        StringBuilder title = new StringBuilder(ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.DialogTitle", channel + 1));
        RhythmVoice rv = midiMix.getRhythmVoice(channel);
        if (rv instanceof UserRhythmVoice)
        {
            title.append(" - ").append(ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.User"));
        } else
        {
            title.append(" - " + rv.getContainer().getName() + " - " + rv.getName());
        }
        return title.toString();
    }
}
