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
import javax.sound.midi.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiConst;

/**
 * This MidiDevice can reroute MidiMessages from one channel to another. Rerouting are added via the addRerouting().
 */
public class ChannelRerouter extends JJazzMidiDevice
{

    ArrayList<Rerouting> reroutings = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(ChannelRerouter.class.getSimpleName());

    /**
     * By default does not reroute any message.
     *
     */
    public ChannelRerouter()
    {
        super("Channel Rerouter");
    }

    public synchronized void addRerouting(int channelFrom, int channelTo)
    {
        if (!MidiConst.checkMidiChannel(channelTo) || MidiConst.checkMidiChannel(channelFrom))
        {
            throw new IllegalArgumentException("channelFrom=" + channelFrom + " channelTo=" + channelTo);   
        }
        if (channelFrom == channelTo)
        {
            return;
        }
        reroutings.add(new Rerouting(channelFrom, channelTo));
    }

    public synchronized void clearReroutings()
    {
        reroutings.clear();
    }

    /**
     *
     * @param fromChannel
     * @return The new channel to be used, or -1 if no rerouting registered.
     */
    public int getRerouting(int fromChannel)
    {
        for (Rerouting r : reroutings)
        {
            if (r.from == fromChannel)
            {
                return r.to;
            }
        }
        return -1;
    }

    @Override
    public Receiver getReceiver()
    {
        RerouterReceiver rcv = new RerouterReceiver();
        receivers.add(rcv);
        open();
        LOGGER.log(Level.FINE, "getReceiver() rcv={0}", rcv);   
        return rcv;
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================
    // ========================================================================================
    // Private classes
    // ========================================================================================
    /**
     * The receiver that does the filtering
     */
    private class RerouterReceiver implements Receiver
    {

        boolean isOpen = true;

        @Override
        public void close()
        {
            LOGGER.fine("RerouterReceiver.close()");   
            isOpen = false;
            receivers.remove(this);
        }

        /**
         * Operation called each time a MidiMessage arrives. Reroute incoming MidiMessages when matching.
         */
        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (!isOpen)
            {
                throw new IllegalStateException("RerouterReceiver object is closed");   
            }

            if (msg instanceof ShortMessage)
            {
                // Only ShortMessage have a channel
                ShortMessage sm = (ShortMessage) msg;
                int channel = sm.getChannel();
                int newChannel = getRerouting(channel);
                if (newChannel != -1)
                {
                    // Reroute it
                    try
                    {
                        sm.setMessage(sm.getCommand(), newChannel, sm.getData1(), sm.getData2());
                    } catch (InvalidMidiDataException ex)
                    {
                        throw new IllegalStateException(ex);   
                    }
                }
            }

            // send the messages
            for (Transmitter t : transmitters.toArray(new Transmitter[0]))
            {
                Receiver rcv = t.getReceiver();
                if (rcv != null)
                {
                    rcv.send(msg, timeStamp);
                }
            }
        }
    }

    private class Rerouting
    {

        int from;
        int to;

        public Rerouting(int from, int to)
        {
            this.from = from;
            this.to = to;
        }
    }
}
