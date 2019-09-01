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
import org.jjazz.instrumentchooser.api.InstrumentChooserDialog;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.MidiConst;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.UserChannelRhythmVoiceKey;

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
        if (!MidiConst.checkMidiChannel(newChannelId))
        {
            return;
        }
        InstrumentMix insMix = midiMix.getInstrumentMixFromChannel(channelId);
        RhythmVoice rvKey = midiMix.getKey(channelId);
        if (newChannelId == MidiConst.CHANNEL_DRUMS && !rvKey.isDrums() && !GM1Bank.couldBeDrums(insMix.getInstrument().getPatchName()))
        {
            LOGGER.warning("Instrument " + insMix.getInstrument().getPatchName() + " might not be a 'Drums' instrument, though it is assigned to a channel usually reserved for Drums.");
        }
        InstrumentMix replacedInsMix = midiMix.getInstrumentMixFromChannel(newChannelId);
        RhythmVoice replacedRvKey = midiMix.getKey(newChannelId);
        if (replacedInsMix == null)
        {
            // We don't replace an existing InstrumentMix, remove old and add new one
            midiMix.setInstrumentMix(channelId, null, null);
            midiMix.setInstrumentMix(newChannelId, rvKey, insMix);
        } else
        {
            // We replace an existing InstrumentMix, swap !
            midiMix.setInstrumentMix(channelId, null, null);
            midiMix.setInstrumentMix(newChannelId, rvKey, insMix);
            midiMix.setInstrumentMix(channelId, replacedRvKey, replacedInsMix);
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
        InstrumentChooserDialog dlg = InstrumentChooserDialog.getDefault();
        String title = buildTitle();
        dlg.preset(insMix.getInstrument(), insMix.getSettings().getTransposition(), channelId, title, null);
        dlg.setVisible(true);
        Instrument ins = dlg.getSelectedInstrument();
        if (ins != null)
        {
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
        RhythmVoice rv = midiMix.getKey(channelId);
        if (rv instanceof UserChannelRhythmVoiceKey)
        {
            title.append(" - User");
        } else
        {
            title.append(" - " + rv.getContainer().getName() + " - " + rv.getName());
        }
        return title.toString();
    }
}
