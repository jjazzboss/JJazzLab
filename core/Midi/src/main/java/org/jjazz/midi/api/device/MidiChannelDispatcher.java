/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midi.api.device;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import org.jjazz.midi.api.MidiConst;

/**
 * This MidiDevice dispatches incoming MidiMessages on different transmitters depending on the midi channel of the message.
 */
@SuppressWarnings(
        {
            "unchecked", "rawtypes"
        })
public class MidiChannelDispatcher extends JJazzMidiDevice
{

    /**
     * 16 midi channels + 1 for messages like sysex, metamessages, etc.
     */
    private static final int LAST_CHANNEL = 16;
    /**
     * The transmitters per channel.
     */
    private ArrayList<Transmitter>[] channelTransmitters = new ArrayList[LAST_CHANNEL + 1];

    public MidiChannelDispatcher()
    {
        super("MidiChannelDispatcher");

        // Initialize the array for each channel
        for (int i = 0; i < channelTransmitters.length; i++)
        {
            channelTransmitters[i] = new ArrayList<>();
        }
    }

    /**
     * Return a transmitter for channel 0.
     *
     * @return
     */
    @Override
    public Transmitter getTransmitter()
    {
        return getTransmitter(0);
    }

    /**
     * Return a transmitter for a specific channel only.
     *
     * @param channel The channel associated to this transmitter. 0 &lt;= channel &lt;= 16. Channel 16 is used to transmit MidiMessages not
     *                bound to a channel (SysExMessage, MetaMessage...).
     * @return
     */
    public Transmitter getTransmitter(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   
        }

        Transmitter mt = super.getTransmitter();
        channelTransmitters[channel].add(mt);
        return mt;
    }

    public List<Transmitter> getTransmitters(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   
        }

        return channelTransmitters[channel];
    }

    @Override
    public Receiver getReceiver()
    {
        Receiver r = new ChannelReceiver();
        receivers.add(r);
        open();
        return r;
    }

    //--------------------------------------------------------------------------------------------
    // ChannelReceiver internal class
    //--------------------------------------------------------------------------------------------
    private class ChannelReceiver implements Receiver
    {

        boolean isReceiverOpen = true;

        /**
         * Operation called each time a MidiMessage arrives. Dispatch the message on transmitters associated to the message channel.
         */
        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            // Default channel is for sysex etc...
            int msgChannel = LAST_CHANNEL;

            if (!isOpen || !isReceiverOpen)
            {
                throw new IllegalStateException("ChannelDispatcher object is closed");   
            }

            // Channel information available only for ShortMessage, otherwise channel 0 is used.
            if (msg instanceof ShortMessage)
            {
                msgChannel = ((ShortMessage) msg).getChannel();
            }

            // send the messages via the transmitters for the selected channel
            for (Transmitter t : channelTransmitters[msgChannel])
            {
                Receiver rcv = t.getReceiver();
                if (rcv != null)
                {
                    rcv.send(msg, timeStamp);
                }
            }
        }

        @Override
        public void close()
        {
            isReceiverOpen = false;
        }
    }
}
