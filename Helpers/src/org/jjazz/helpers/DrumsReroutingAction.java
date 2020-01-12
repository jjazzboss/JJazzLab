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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.helpers.DrumsReroutingDialog.ReroutingChoice;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midimix.MidiMix;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.openide.modules.OnStart;

/**
 * Listen to pre-playback events, show the DrumsReroutingDialog and reroute channels if needed.
 */
@OnStart               // Used only to get the automatic object creation upon startup
public class DrumsReroutingAction implements VetoableChangeListener, Runnable
{

    private static DrumsReroutingDialog DIALOG;
    private static final Logger LOGGER = Logger.getLogger(DrumsReroutingAction.class.getSimpleName());
    ReroutingChoice savedChoice = DrumsReroutingDialog.ReroutingChoice.CANCEL;

    public DrumsReroutingAction()
    {
        // Register for song playback
        MusicController mc = MusicController.getInstance();
        mc.addVetoableChangeListener(this);
    }

    @Override
    public void run()
    {
        // Do nothing, we just use @OnStart just to get the automatic object creation...
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
        LOGGER.log(Level.FINE, "vetoableChange() -- evt={0}", evt);

        MusicController mc = MusicController.getInstance();
        if (evt.getSource() != mc
                || evt.getPropertyName() != MusicController.PROPVETO_PRE_PLAYBACK
                || !mc.getPlaybackState().equals(MusicController.State.PLAYBACK_STOPPED))  // Don't check in pause mode
        {
            return;
        }

        MusicGenerationContext context = (MusicGenerationContext) evt.getNewValue();
        assert context != null : "evt=" + evt;
        MidiMix midiMix = context.getMidiMix();

        List<Integer> toBeReroutedChannels = new ArrayList<>();
        List<Integer> reroutableChannels = getChannelsToBeRerouted(midiMix);
        if (!reroutableChannels.isEmpty())
        {
            switch (savedChoice)
            {
                case CANCEL:
                    DrumsReroutingDialog dialog = getDialog();
                    dialog.preset(reroutableChannels, midiMix);
                    dialog.setVisible(true);
                    ReroutingChoice choice = dialog.getUserChoice();
                    switch (choice)
                    {
                        case CANCEL:
                            throw new PropertyVetoException(null, evt); // null msg to prevent user notifications by exception handlers
                        case REROUTE:
                            toBeReroutedChannels = reroutableChannels;
                            break;
                        case DONT_REROUTE:
                            // Do nothing, leave toBeReroutedChannels empty
                            break;
                        default:
                            throw new IllegalStateException("choice=" + choice);
                    }
                    if (dialog.isRememberChoiceSelected())
                    {
                        savedChoice = choice;
                    }
                    break;
                case REROUTE:
                    toBeReroutedChannels = reroutableChannels;
                    break;
                case DONT_REROUTE:
                    // Do nothing, leave toBeReroutedChannels empty
                    break;
                default:
                    throw new IllegalStateException("savedChoice=" + savedChoice);
            }
            performRerouting(toBeReroutedChannels, midiMix);
        }
    }

    private DrumsReroutingDialog getDialog()
    {
        if (DIALOG == null)
        {
            DIALOG = new DrumsReroutingDialog();
        }
        return DIALOG;
    }

    /**
     * Get the channels which need rerouting.
     * <p>
     * A channel needs rerouting if all the following conditions are met:<br>
     * 1/ channel != 10 <br>
     * 2/ rv.isDrums()==true and rerouting is not already enabled <br>
     * 3/ instrument is the VoidInstrument<br>
     *
     * @param midiMix
     * @return Can't be null
     */
    private List<Integer> getChannelsToBeRerouted(MidiMix midiMix)
    {
        List<Integer> res = new ArrayList<>();
        for (RhythmVoice rv : midiMix.getRvKeys())
        {
            int channel = midiMix.getChannel(rv);
            InstrumentMix insMix = midiMix.getInstrumentMixFromKey(rv);
            Instrument ins = insMix.getInstrument();
            LOGGER.fine("getChannelsToBeRerouted() rv=" + rv + " channel=" + channel + " ins=" + ins);
            if (channel != MidiConst.CHANNEL_DRUMS
                    && rv.isDrums()
                    && !midiMix.getDrumsReroutedChannels().contains(channel)
                    && ins == StdSynth.getVoidInstrument())
            {
                res.add(channel);
            }
        }
        LOGGER.fine("getChannelsToBeRerouted() res=" + res);
        return res;
    }

    private void performRerouting(List<Integer> channels, MidiMix midiMix)
    {
        for (int ch : channels)
        {
            midiMix.setDrumsReroutedChannel(true, ch);
        }
    }

}
