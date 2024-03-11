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

import javax.sound.midi.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiUtilities;

/**
 * This MidiDevice forwards MidiMessages from in to out, except for messages that are filtered out (discarded).
 * <p>
 */
public class MidiFilter extends JJazzMidiDevice
{

    public enum Config
    {
        FILTER_EVERYTHING, FILTER_SYSEXMESSAGE, FILTER_METAMESSAGE, FILTER_ALL_EXCEPT_NOTE_ON_OFF_MESSAGES, FILTER_ACTIVE_SENSING, FILTER_CHANNEL_PRESSURE, FILTER_MIDI_TIME_CODE, FILTER_POLY_PRESSURE, FILTER_SONG_POSITION_POINTER, FILTER_SONG_SELECT,
        FILTER_TIMING_CLOCK, FILTER_TUNE_REQUEST, FILTER_START, FILTER_STOP, FILTER_CONTINUE, FILTER_PITCHBEND
    }

    public enum ConfigLog
    {
        LOG_FILTERED_MESSAGES,
        LOG_PASSED_MESSAGES
    }
    /**
     * Configure what is filtered.
     */
    private EnumSet<Config> configFilter = EnumSet.noneOf(Config.class);

    /**
     * Configure what is logged.
     */
    public EnumSet<ConfigLog> configLog = EnumSet.noneOf(ConfigLog.class);
    private static final Logger LOGGER = Logger.getLogger(MidiFilter.class.getSimpleName());

    /**
     * By default does not filter nor log anything.
     *
     * @param s String
     */
    public MidiFilter(String s)
    {
        super(s);
    }

    @Override
    public Receiver getReceiver()
    {
        FilterReceiver rcv = new FilterReceiver();
        receivers.add(rcv);
        open();
        LOGGER.log(Level.FINE, "getReceiver() rcv={0}", rcv);   
        return rcv;
    }

    public EnumSet<Config> getFilterConfig()
    {
        return EnumSet.copyOf(configFilter);
    }

    public void setFilterConfig(EnumSet<Config> newConfig)
    {
        configFilter = EnumSet.copyOf(newConfig);
    }

    // ================================================================================
    // Private methods
    // ================================================================================    
    /**
     * Operation called when a MidiMessage has been filtered
     *
     * @param msg
     * @param timestamp
     */
    private void filtered(MidiMessage msg, long timestamp)
    {
        if (configLog.contains(ConfigLog.LOG_FILTERED_MESSAGES))
        {
            LOGGER.log(Level.INFO, "{0} : FILTERED={1}", new Object[]   
            {
                devInfo.getName(),
                MidiUtilities.toString(msg, timestamp)
            });
        }
    }

    /**
     * Operation called when a MidiMessage has not been filtered
     *
     * @param msg
     * @param timestamp
     */
    private void passed(MidiMessage msg, long timestamp)
    {
        // Forward the message to transmitters
        for (Transmitter t : transmitters.toArray(new Transmitter[0]))
        {
            Receiver rcv = t.getReceiver();
            if (rcv != null)
            {
                rcv.send(msg, timestamp);
            }
        }

        // Optional log
        if (configLog.contains(ConfigLog.LOG_PASSED_MESSAGES))
        {
            LOGGER.log(Level.INFO, "{0} : PASSED={1}", new Object[]   
            {
                devInfo.getName(),
                MidiUtilities.toString(msg, timestamp)
            });
        }
    }

    /**
     * The receiver that does the filtering
     */
    private class FilterReceiver implements Receiver
    {

        boolean isOpen = true;

        @Override
        synchronized public void close()
        {
            LOGGER.fine("FilterReceiver.close()");   
            isOpen = false;
            receivers.remove(this);
        }

        /**
         * Operation called each time a MidiMessage arrives.Filter incoming MidiMessages.
         *
         * @param msg
         * @param timestamp
         */
        @Override
        synchronized public void send(MidiMessage msg, long timestamp)
        {

            if (!isOpen)
            {
                throw new IllegalStateException("FilterReceiver object is closed");   
            }

            if (configFilter.isEmpty())
            {
                passed(msg, timestamp);
                return;
            }

            if (configFilter.contains(Config.FILTER_EVERYTHING))
            {
                filtered(msg, timestamp);
                return;
            }

            if (configFilter.contains(Config.FILTER_ALL_EXCEPT_NOTE_ON_OFF_MESSAGES))
            {
                if (msg instanceof ShortMessage)
                {
                    ShortMessage sm = (ShortMessage) msg;

                    if ((sm.getCommand() == ShortMessage.NOTE_ON) || (sm.getCommand() == ShortMessage.NOTE_OFF))
                    {
                        passed(msg, timestamp);
                        return;
                    } else
                    {
                        filtered(msg, timestamp);
                        return;
                    }
                } else
                {
                    filtered(msg, timestamp);
                    return;
                }
            }


            if (configFilter.contains(Config.FILTER_SYSEXMESSAGE) && msg instanceof SysexMessage)
            {
                filtered(msg, timestamp);
                return;
            }


            if (configFilter.contains(Config.FILTER_METAMESSAGE) && msg instanceof MetaMessage)
            {
                filtered(msg, timestamp);
                return;
            }


            if (msg instanceof ShortMessage)
            {
                int cmd = ((ShortMessage) msg).getCommand();
                if ((configFilter.contains(Config.FILTER_CHANNEL_PRESSURE) && cmd == ShortMessage.CHANNEL_PRESSURE)
                        || (configFilter.contains(Config.FILTER_POLY_PRESSURE) && cmd == ShortMessage.POLY_PRESSURE)
                        || (configFilter.contains(Config.FILTER_PITCHBEND) && cmd == ShortMessage.PITCH_BEND))
                {
                    filtered(msg, timestamp);
                    return;
                }
            }


            int st = msg.getStatus();
            if ((configFilter.contains(Config.FILTER_ACTIVE_SENSING) && st == ShortMessage.ACTIVE_SENSING)
                    || (configFilter.contains(Config.FILTER_CONTINUE) && st == ShortMessage.CONTINUE)
                    || (configFilter.contains(Config.FILTER_MIDI_TIME_CODE) && st == ShortMessage.MIDI_TIME_CODE)
                    || (configFilter.contains(Config.FILTER_SONG_POSITION_POINTER) && st == ShortMessage.SONG_POSITION_POINTER)
                    || (configFilter.contains(Config.FILTER_SONG_SELECT) && st == ShortMessage.SONG_SELECT)
                    || (configFilter.contains(Config.FILTER_START) && st == ShortMessage.START)
                    || (configFilter.contains(Config.FILTER_STOP) && st == ShortMessage.STOP)
                    || (configFilter.contains(Config.FILTER_TIMING_CLOCK) && st == ShortMessage.TIMING_CLOCK)
                    || (configFilter.contains(Config.FILTER_TUNE_REQUEST) && st == ShortMessage.TUNE_REQUEST))
            {
                filtered(msg, timestamp);
                return;
            }


            // Ok message is not filtered
            passed(msg, timestamp);
        }
    }
}
