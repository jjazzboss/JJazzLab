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
package org.jjazz.midi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.MidiAddress.BankSelectMethod;
import org.jjazz.util.Utilities;
import org.openide.util.Exceptions;

/**
 * Convenience MIDI functions.
 */
public class MidiUtilities
{

    private static String[] CONTROL_CHANGE_STRINGS;
    private static String[] COMMAND_STRINGS;
    private static final Logger LOGGER = Logger.getLogger(MidiUtilities.class.getSimpleName());

    /**
     * Make sure x is a in the range [0-127].
     *
     * @param x
     * @return
     */
    static public int limit(int x)
    {
        x = Math.min(x, 127);
        x = Math.max(x, 0);
        return x;
    }

    /**
     * Get track MidiEvents whose tick position is within range [tickMin;tickMax] and which satisfy the specified tester.
     *
     * @param track
     * @param tester
     * @param tickMin
     * @param tickMax
     * @return
     */
    static public List<MidiEvent> getMidiEvents(Track track, Predicate<MidiEvent> tester, long tickMin, long tickMax)
    {
        var res = new ArrayList<MidiEvent>();
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            if (me.getTick() < tickMin)
            {
                continue;
            } else if (me.getTick() > tickMax)
            {
                break;
            }
            if (tester.test(me))
            {
                res.add(me);
            }
        }
        return res;
    }

    /**
     * Get track MidiEvents whose MidiMessage is instance of msgClass, whose tick position is within range [tickMin;tickMax] and
     * which satisfy the specified tester.
     *
     * @param <T>
     * @param track
     * @param msgClass
     * @param tester
     * @param tickMin
     * @param tickMax
     * @return
     */
    static public <T> List<MidiEvent> getMidiEvents(Track track, Class<T> msgClass, Predicate<T> tester, long tickMin, long tickMax)
    {
        var res = new ArrayList<MidiEvent>();
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            if (me.getTick() < tickMin)
            {
                continue;
            } else if (me.getTick() > tickMax)
            {
                break;
            }
            MidiMessage mm = me.getMessage();
            if (msgClass.isInstance(mm))
            {
                T typedMsg = msgClass.cast(mm);
                if (tester.test(typedMsg))
                {
                    res.add(me);
                }
            }
        }
        return res;
    }

    /**
     * Remove all MidiEvents from a track.
     *
     * @param track
     */
    static public void clearTrack(Track track)
    {
        int last = track.size() - 1;
        for (int i = last; i >= 0; i--)
        {
            MidiEvent me = track.get(i);
            track.remove(me);
        }
    }

    /**
     * Get the last MidiEvent before beforeTick which satisfies predicate.
     *
     * @param <T>
     * @param track
     * @param msgClass
     * @param tester
     * @param beforeTick
     * @return
     */
    static public <T> MidiEvent getLastMidiEvent(Track track, Class<T> msgClass, Predicate<T> tester, long beforeTick)
    {
        MidiEvent res = null;
        int last = track.size() - 1;
        for (int i = last; i >= 0; i--)
        {
            MidiEvent me = track.get(i);
            if (me.getTick() >= beforeTick)
            {
                continue;
            }
            MidiMessage mm = me.getMessage();
            if (msgClass.isInstance(mm))
            {
                T typedMsg = msgClass.cast(mm);
                if (tester.test(typedMsg))
                {
                    res = me;
                    break;
                }
            }
        }
        return res;
    }

    static public SysexMessage getGmModeOnSysExMessage()
    {
        SysexMessage sm = new SysexMessage();
        byte[] data =
        {
            (byte) 0xF0, (byte) 0x7E, (byte) 0x7F, (byte) 0x09, (byte) 0x01, (byte) 0xF7
        };
        try
        {
            sm.setMessage(data, 6);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);   //NOI18N
        }
        return sm;
    }

    static public SysexMessage getGm2ModeOnSysExMessage()
    {
        SysexMessage sm = new SysexMessage();
        byte[] data =
        {
            (byte) 0xF0, (byte) 0x7E, (byte) 0x7F, (byte) 0x09, (byte) 0x03, (byte) 0xF7
        };
        try
        {
            sm.setMessage(data, 6);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);   //NOI18N
        }
        return sm;
    }

    static public SysexMessage getXgModeOnSysExMessage()
    {
        SysexMessage sm = new SysexMessage();
        byte[] data =
        {
            (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x4C, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x00, (byte) 0xF7
        };
        try
        {
            sm.setMessage(data, 9);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);   //NOI18N
        }
        return sm;
    }

    static public SysexMessage getGsModeOnSysExMessage()
    {
        SysexMessage sm = new SysexMessage();
        byte[] data =
        {
            (byte) 0xF0, (byte) 0x41, (byte) 0x10, (byte) 0x42, (byte) 0x12, (byte) 0x40, (byte) 0x00, (byte) 0x7F, (byte) 0x00, (byte) 0x41, (byte) 0XF7
        };
        try
        {
            sm.setMessage(data, 11);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);   //NOI18N
        }
        return sm;
    }

    /**
     * Send the specified SysExMessage.
     * <p>
     * Add a little Thread.sleep() to give time for harware to execute before sending other possible Midi messages.
     *
     * @param sm
     */
    static public void sendSysExMessage(SysexMessage sm)
    {
        LOGGER.fine("sendSysExMessage() sm=" + sm);   //NOI18N
        JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(sm);
        try
        {
            Thread.sleep(50);  // Give time for the hardware to execute
        } catch (InterruptedException ex)
        {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);   //NOI18N
        }
    }

    /**
     *
     * @param command
     * @param channel
     * @param d1
     * @param d2
     * @return
     */
    static public ShortMessage buildMessage(int command, int channel, int d1, int d2)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("command=" + command + " channel=" + channel + " d1=" + d1 + " d2=" + d2);   //NOI18N
        }
        ShortMessage sm = new ShortMessage();
        try
        {
            sm.setMessage(command, channel, d1, d2);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, "Invalid Midi message: command=" + command + " channel=" + channel + " d1=" + d1 + " d2=" + d2, ex);   //NOI18N
        }
        return sm;
    }

    static public ShortMessage getVolumeMessage(int channel, int data)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_VOLUME_MSB, data);
    }

    static public ShortMessage getChorusMessage(int channel, int data)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_CHORUS_DEPTH, data);
    }

    static public ShortMessage getReverbMessage(int channel, int data)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_REVERB_DEPTH, data);
    }

    static public ShortMessage getPanoramicMessage(int channel, int data)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_PAN_MSB, data);
    }

    static public MetaMessage getTimeSignatureMessage(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new IllegalArgumentException("ts=" + ts);   //NOI18N
        }
        byte powerOf2;
        switch (ts.getLower())
        {
            case 2:
                powerOf2 = 1;
                break;
            case 4:
                powerOf2 = 2;
                break;
            case 8:
                powerOf2 = 3;
                break;
            default:
                throw new IllegalArgumentException("ts=" + ts);   //NOI18N
        }
        byte[] data = new byte[]
        {
            (byte) ts.getUpper(), powerOf2, 24, 8
        };
        MetaMessage mm = new MetaMessage();
        try
        {
            mm = new MetaMessage(88, data, 4);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getLocalizedMessage(), ex);   //NOI18N
        }
        return mm;
    }

    static public MetaMessage getTempoMessage(int channel, int bpm)
    {
        if (!MidiConst.checkMidiChannel(channel) || bpm < 10 || bpm > 400)
        {
            throw new IllegalArgumentException("channel=" + channel + " tempo=" + bpm);   //NOI18N
        }
        long mspq = Math.round(60000000f / bpm);        // microseconds per quarter note
        byte[] data = new byte[]
        {
            (byte) ((mspq & 0xFF0000) >> 16), (byte) ((mspq & 0xFF00) >> 8), (byte) ((mspq & 0xFF))
        };
        MetaMessage mm = new MetaMessage();
        try
        {
            mm = new MetaMessage(81, data, 3);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getLocalizedMessage(), ex);   //NOI18N
        }
        return mm;
    }

    /**
     * Build the messages depending of the specified instrument's bank BankSelectionMethod.
     *
     * @param channel
     * @param ins
     * @return
     */
    static public ShortMessage[] getPatchMessages(int channel, Instrument ins)
    {
        ShortMessage[] sms = null;
        BankSelectMethod bsm = ins.getMidiAddress().getBankSelectMethod();
        int bankMSB = ins.getMidiAddress().getBankMSB();
        int bankLSB = ins.getMidiAddress().getBankLSB();
        int pc = ins.getMidiAddress().getProgramChange();
        if (bsm == null || bankMSB > 127 || bankLSB > 127)
        {
            throw new IllegalArgumentException(   //NOI18N
                    "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                            getBank());
        }
        switch (bsm)
        {
            case MSB_LSB:
                if (bankMSB < 0 || bankLSB < 0)
                {
                    throw new IllegalArgumentException(   //NOI18N
                            "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                                    getBank());
                }
                sms = new ShortMessage[3];
                // Bank Select MSB
                sms[0] = buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_BANK_SELECT_MSB, bankMSB);
                // Bank Select LSB
                sms[1] = buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_BANK_SELECT_LSB, bankLSB);
                // Program Change
                sms[2] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
                break;
            case MSB_ONLY:
                if (bankMSB < 0)
                {
                    throw new IllegalArgumentException(   //NOI18N
                            "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                                    getBank());
                }
                sms = new ShortMessage[2];
                // Bank Select MSB
                sms[0] = buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_BANK_SELECT_MSB, bankMSB);
                // Program Change
                sms[1] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
                break;
            case LSB_ONLY:
                if (bankLSB < 0)
                {
                    throw new IllegalArgumentException(   //NOI18N
                            "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                                    getBank());
                }
                sms = new ShortMessage[2];
                // Bank Select LSB
                sms[0] = buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_BANK_SELECT_LSB, bankLSB);
                // Program Change
                sms[1] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
                break;
            default:
                // PC_ONLY
                sms = new ShortMessage[1];
                sms[0] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
                break;
        }
        LOGGER.log(Level.FINE, "getPatchMessages() chan={0} ins={1} bsm={2}", new Object[]   //NOI18N
        {
            channel, ins.getPatchName(), bsm
        });
        return sms;
    }

    /**
     * Get the messages to apply a pitch bend of pitchDelta semitons.
     *
     * @param pitchDelta In semitons, can be negative or positive.
     * @return
     */
    static public ShortMessage[] getPitchBendMessages(int channel, int pitchDelta)
    {
        LOGGER.warning("getPitchBendMessages() NOT VALIDED YET !");   //NOI18N
        final int PITCH_UP_MAX_D2 = 0x40;
        final int PITCH_UP_MAX_D1 = 0;
        final int PITCH_CENTER_D2 = 0x20;
        final int PITCH_CENTER_D1 = 0;
        final int PITCH_DOWN_MAX_D2 = 0;
        final int PITCH_DOWN_MAX_D1 = 0;

        // Default value
        int pitchD1 = PITCH_CENTER_D1;
        int pitchD2 = PITCH_CENTER_D2;
        int sensitivity = 4;

        if (pitchDelta > 0)
        {
            sensitivity = 2 * pitchDelta;
            pitchD1 = PITCH_UP_MAX_D1;
            pitchD2 = PITCH_UP_MAX_D2;
        } else if (pitchDelta < 0)
        {
            sensitivity = 2 * pitchDelta;
            pitchD1 = PITCH_DOWN_MAX_D1;
            pitchD2 = PITCH_DOWN_MAX_D2;
        }

        // Set the pitch sensitivity to pitchDelta * 2 so we can use full pitch bend down/up
        ShortMessage[] sms = new ShortMessage[6];
        try
        {
            sms[0] = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 101, 0);    // MSB for RPN PitchBend Sensitivity
            sms[1] = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 100, 0);    // LSB for RPN PitchBend Sensitivity
            sms[2] = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 6, sensitivity);      // PitchBend Sensitivity value
            sms[3] = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 101, 127);      // Reset RPN
            sms[4] = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 100, 127);      // Reset RPN
            sms[5] = new ShortMessage(ShortMessage.PITCH_BEND, channel, pitchD1, pitchD2);      // Pitch bend message
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return sms;
    }

    static public ShortMessage getResetAllControllersMessage(int channel)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_RESET_ALL_CONTROLLERS, 0);
    }

    static public ShortMessage getJJazzBeatChangeControllerMessage(int channel)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_JJAZZ_BEAT_CHANGE, 0);
    }

    static public ShortMessage getJJazzChordChangeControllerMessage(int channel)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_JJAZZ_CHORD_CHANGE, 0);
    }

    static public ShortMessage getJJazzActivityControllerMessage(int channel)
    {
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_JJAZZ_ACTIVITY, 0);
    }

    /**
     * Get the Midi controller message to code a tempo factor.
     *
     * @param channel
     * @param tempoFactor A percentage [.5;2.0]
     * @return
     */
    static public ShortMessage getJJazzTempoFactorControllerMessage(int channel, float tempoFactor)
    {
        if (tempoFactor < .5 || tempoFactor > 2)
        {
            throw new IllegalArgumentException("channel=" + channel + " tempoFactor=" + tempoFactor);   //NOI18N
        }
        int tempoFactorByte = Math.round((100 * tempoFactor - 50) * 127f / 150);
        return buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR, tempoFactorByte);
    }

    /**
     * Get the tempo factor [.5;2.0] from the specified Tempo Factor JJazz controller message.
     *
     * @param tempoFactorMsg
     * @return
     */
    static public float getTempoFactor(ShortMessage tempoFactorMsg)
    {
        if (tempoFactorMsg.getData1() != MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR)
        {
            throw new IllegalArgumentException("tempoFactorMsg=" + tempoFactorMsg);   //NOI18N
        }
        float res = (50 + tempoFactorMsg.getData2() * 150f / 127f) / 100;
        return res;
    }

    /**
     * Convert a tempo in BPM (beat per minute) into a tempo in microseconds per quarter.
     *
     * @param tempoBPM
     * @return
     */
    static public double toTempoMPQ(double tempoBPM)
    {
        return ((double) 60000000l) / tempoBPM;
    }

    /**
     * Convert PPQ tick to microsecond tick.
     *
     * @param tickInPPQ
     * @param tempoMPQ Tempo in microseconds per quarter
     * @param resolution
     * @return
     */
    static public long toTickInUs(long tickInPPQ, double tempoMPQ, int resolution)
    {
        return (long) (((double) tickInPPQ) * tempoMPQ / resolution);
    }

    /**
     * Convert micro-second tick to PPQ tick
     *
     * @param tickInUs
     * @param tempoMPQ Tempo in microseconds per quarter
     * @param resolution
     * @return
     */
    public static long toTickInPPQ(long tickInUs, double tempoMPQ, int resolution)
    {
        return (long) ((((double) tickInUs) * resolution) / tempoMPQ);      // Do not round
    }

    /**
     * Search for the first TrackName MetaEvent in the specified track and return its name.
     *
     * @param track
     * @return Can be null if no track name found
     */
    static public String getTrackName(Track track)
    {
        String name = null;
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            MidiMessage msg = me.getMessage();
            if (msg instanceof MetaMessage)
            {
                MetaMessage mm = (MetaMessage) msg;
                if (mm.getType() == 3)        // TrackName type
                {
                    name = Utilities.toString(mm.getData());
                    break;
                }
            }
        }
        return name;
    }

    static public MetaMessage getTrackNameMetaMessage(String txt)
    {
        if (txt == null || txt.trim().isEmpty())
        {
            throw new IllegalArgumentException("txt=" + txt);   //NOI18N
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(0x03, txt.getBytes(), txt.length());
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return mm;
    }

    static public MetaMessage getTextMetaMessage(String txt)
    {
        if (txt == null || txt.isEmpty())
        {
            throw new IllegalArgumentException("txt=" + txt);   //NOI18N
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(0x01, txt.getBytes(), txt.length());
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return mm;
    }

    static public MetaMessage getMarkerMetaMessage(String txt)
    {
        if (txt == null || txt.isEmpty())
        {
            throw new IllegalArgumentException("txt=" + txt);   //NOI18N
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(0x06, txt.getBytes(), txt.length());
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return mm;
    }

    static public MetaMessage getCopyrightMetaMessage(String txt)
    {
        if (txt == null || txt.isEmpty())
        {
            throw new IllegalArgumentException("txt=" + txt);   //NOI18N
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(0x02, txt.getBytes(), txt.length());
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return mm;
    }

    /**
     * Change the channel of ShortMessage events in the sequence.
     * <p>
     * All ShortMessages belonging to one of the fromChannels are reassigned to channel destChannel.
     * <p>
     * This can be used for example to reroute percussion notes to the GM drums channel.
     *
     * @param sequence
     * @param fromChannels
     * @param destChannel
     */
    static public void rerouteShortMessages(Sequence sequence, List<Integer> fromChannels, int destChannel)
    {
        if (fromChannels.isEmpty())
        {
            return;
        }
        for (Track track : sequence.getTracks())
        {
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent me = track.get(i);
                MidiMessage msg = me.getMessage();
                if (msg instanceof ShortMessage)
                {
                    ShortMessage sm = (ShortMessage) msg;
                    int channel = sm.getChannel();
                    if (channel != destChannel && fromChannels.contains(channel))
                    {
                        try
                        {
                            sm.setMessage(sm.getCommand(), destChannel, sm.getData1(), sm.getData2());
                        } catch (InvalidMidiDataException ex)
                        {
                            LOGGER.warning("rerouteShortMessages() ex=" + ex.getLocalizedMessage());   //NOI18N
                        }
                    }
                }
            }
        }
    }

    /**
     * Convert srcTick in srcPPQresolution into a tick for the JJazz program's PPQ resolution.
     * <p>
     * Example: srcTick=200, srcPPQresolution=400, MidiConst.PPQ_RESOLUTION=1920<br>
     * then return tick=960.
     *
     * @param srcTick
     * @param srcPPQresolution
     * @return A tick with MidiConst.PPQ_RESOLUTION
     */
    static public long convertTick(long srcTick, long srcPPQresolution)
    {
        if (srcTick < 0 || srcPPQresolution <= 0)
        {
            throw new IllegalArgumentException("srcTick=" + srcTick + " srcPPQresolution=" + srcPPQresolution);   //NOI18N
        }
        double ratio = ((double) srcTick) / srcPPQresolution;
        long tick = Math.round(MidiConst.PPQ_RESOLUTION * ratio);
        return tick;
    }

    /**
     * Get the tempo in BPM coded in a Tempo Midi message.
     *
     * @param tempoMsg Must be a tempo MetaMessage (type=81)
     * @return
     */
    static public int getTempoInBPM(MetaMessage tempoMsg)
    {
        byte[] data = tempoMsg.getData();
        if (tempoMsg.getType() != 81 || data.length != 3)
        {
            throw new IllegalArgumentException("tempoMsg=" + tempoMsg);   //NOI18N
        }
        int mspq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
        int tempo = Math.round(60000001f / mspq);
        return tempo;
    }

    /**
     * Provide an explicit string for a MidiMessage.
     *
     * @param msg A MidiMessage.
     * @param tick The tick of the MidiMessage. Ignore if &lt; 0.
     * @return A string representing the MidiMessage.
     */
    public static String toString(MidiMessage msg, long tick)
    {
        StringBuilder sb = new StringBuilder();

        // First class name and status byte
        sb.append(String.format("%s (st=%3d)", msg.getClass().getSimpleName(), msg.getStatus()));

        // for ShortMessage
        if (msg instanceof ShortMessage)
        {
            ShortMessage sm = (ShortMessage) msg;
            sb.append(String.format(" ch=%-2d", sm.getChannel()));
            String cmd = getCmdString(sm.getCommand());
            if (cmd.equals("?"))
            {
                cmd = String.valueOf(sm.getCommand());
            }
            sb.append(String.format(" cmd=%-8s", cmd));
            String d1 = String.valueOf(sm.getData1()); // By default display the value
            if (sm.getCommand() == ShortMessage.CONTROL_CHANGE)
            {
                // It's a control change, we can display a text
                d1 = getCtrlChgString(sm.getData1());
            } else if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
            {
                // Display the note instead of pitch
                Note n = new Note(sm.getData1());
                d1 = n.toAbsoluteNoteString();
            }
            sb.append(String.format(" d1=%-4s", d1));
            sb.append(String.format("  d2=%-3d", sm.getData2()));
        } else if (msg instanceof SysexMessage)
        {
            SysexMessage sm = (SysexMessage) msg;
            sb.append(" SysEx ").append(String.format("  data=%h", sm.getData()));
        } else if (msg instanceof MetaMessage)
        {
            MetaMessage mm = (MetaMessage) msg;
            switch (mm.getType())
            {
                case 1:  // Text
                    sb.append(" text=").append(Utilities.toString(mm.getData()));
                    break;
                case 2:  // Copyright
                    sb.append(" copyright=").append(Utilities.toString(mm.getData()));
                    break;
                case 3:  // TrackName
                    sb.append(" trackname=").append(Utilities.toString(mm.getData()));
                    break;
                case 4:  // Instrument Name
                    sb.append(" instrumentName=").append(Utilities.toString(mm.getData()));
                    break;
                case 5:  // Lyrics
                    sb.append(" lyrics=").append(Utilities.toString(mm.getData()));
                    break;
                case 6:  // Marker
                    sb.append(" marker=").append(Utilities.toString(mm.getData()));
                    break;
                case 88:  // TimeSignature        
                    int upper = mm.getData()[0];
                    int lower = (int) Math.pow(2, mm.getData()[1]);
                    TimeSignature ts = TimeSignature.get(upper, lower);
                    sb.append(" timeSignature=").append(ts);
                    break;
                case 81:  // Tempo
                    sb.append(" tempo=").append(getTempoInBPM(mm));
                    break;
                default:
                    sb.append(" type=").append(mm.getType());
                    break;
            }
        }

        // append time
        if (tick >= 0)
        {
            float timeInBeats = (float) tick / MidiConst.PPQ_RESOLUTION;
            String timeStr = String.format("%.2f", timeInBeats);
            int bar44 = (int) (timeInBeats / 4);
            float bar44beat = timeInBeats - bar44 * 4;
            sb.append(" t=").append(tick).append(" beat=").append(timeStr).append(" (pos4/4=[")
                    .append(bar44).append(",").append(bar44beat).append("])");
        }

        return sb.toString();
    }

    public static String toString(Track track)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Track:\n");
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            sb.append(String.format(" %4d: %s\n", i, toString(me.getMessage(), me.getTick())));
        }
        sb.append("Track End\n");
        return sb.toString();
    }

    public static String toString(Sequence seq)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Sequence:\n");
        Track[] tracks = seq.getTracks();
        for (int i = 0; i < tracks.length; i++)
        {
            Track track = tracks[i];
            sb.append(toString(track));
        }
        sb.append("Sequence End\n");
        return sb.toString();
    }

    /**
     * Add a TrackName MetaMessage (type=3) to the beginning of track.
     *
     * @param t
     * @param name Name of the track
     */
    public static void addTrackNameEvent(Track t, String name)
    {
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(name), 0);
        t.add(me);
    }

    /**
     * Set the track's EndOfTrack MetaMessage position to tick.
     *
     * @param t
     * @param tick The tick position must be equal or above the last tick in the sequence.
     * @return True if EndOfTrack position was correctly set, false if there was a problem.
     */
    public static boolean setEndOfTrackPosition(Track t, long tick)
    {
        boolean res = false;
        assert tick >= 0 && t != null && t.size() > 0 : "t=" + t + " tick=" + tick;   //NOI18N
        MidiEvent me = t.get(t.size() - 1);
        MidiMessage mm = me.getMessage();
        if ((mm instanceof MetaMessage) && ((MetaMessage) mm).getType() == 47)
        {
            me.setTick(tick);
            res = (me.getTick() == tick);      // Check position was correctly set
        }
        return res;
    }

    /**
     * Convert a control Midi id into string.
     */
    public static String getCtrlChgString(int ctrl)
    {
        if (ctrl < 0 || ctrl >= 256)
        {
            throw new IllegalArgumentException("ctrl=" + ctrl);   //NOI18N
        }
        if (CONTROL_CHANGE_STRINGS == null)
        {
            initControlChangeStrings();
        }

        return CONTROL_CHANGE_STRINGS[ctrl];
    }

    /**
     * Convert a command Midi id into string.
     */
    public static String getCmdString(int cmd)
    {
        if (cmd < 0 || cmd > 255)
        {
            throw new IllegalArgumentException("cmd=" + cmd);   //NOI18N
        }
        if (COMMAND_STRINGS == null)
        {
            initCommandStrings();
        }
        return COMMAND_STRINGS[cmd];
    }

    private static void initCommandStrings()
    {
        COMMAND_STRINGS = new String[256];
        for (int i = 0; i < 256; i++)
        {
            COMMAND_STRINGS[i] = "?";
        }
        COMMAND_STRINGS[ShortMessage.ACTIVE_SENSING] = "ACTIVE_SENSING";
        COMMAND_STRINGS[ShortMessage.CHANNEL_PRESSURE] = "CHANNEL_PRESSURE";
        COMMAND_STRINGS[ShortMessage.CONTINUE] = "CONTINUE";
        COMMAND_STRINGS[ShortMessage.CONTROL_CHANGE] = "CONTROL_CHANGE";
        COMMAND_STRINGS[ShortMessage.END_OF_EXCLUSIVE] = "END_OF_EXCLUSIVE";
        COMMAND_STRINGS[ShortMessage.MIDI_TIME_CODE] = "MIDI_TIME_CODE";
        COMMAND_STRINGS[ShortMessage.NOTE_OFF] = "NOTE_OFF";
        COMMAND_STRINGS[ShortMessage.NOTE_ON] = "NOTE_ON";
        COMMAND_STRINGS[ShortMessage.PITCH_BEND] = "PITCH_BEND";
        COMMAND_STRINGS[ShortMessage.POLY_PRESSURE] = "POLY_PRESSURE";
        COMMAND_STRINGS[ShortMessage.PROGRAM_CHANGE] = "PROGRAM_CHANGE";
        COMMAND_STRINGS[ShortMessage.SONG_POSITION_POINTER] = "SONG_POSITION_POINTER";
    }

    private static void initControlChangeStrings()
    {
        CONTROL_CHANGE_STRINGS = new String[256];
        for (int i = 0; i < 256; i++)
        {
            CONTROL_CHANGE_STRINGS[i] = "?";
        }
        CONTROL_CHANGE_STRINGS[0] = "BANK_SELECT_MSB";
        CONTROL_CHANGE_STRINGS[1] = "MODULATION_MSB";
        CONTROL_CHANGE_STRINGS[7] = "VOLUME_MSB";
        CONTROL_CHANGE_STRINGS[10] = "PAN_MSB";
        CONTROL_CHANGE_STRINGS[11] = "EXPRESSION_MSB";
        CONTROL_CHANGE_STRINGS[32] = "BANK_SELECT_LSB";
        CONTROL_CHANGE_STRINGS[64] = "SUSTAIN";
        CONTROL_CHANGE_STRINGS[91] = "REVERB_DEPTH";
        CONTROL_CHANGE_STRINGS[93] = "CHORUS_DEPTH";
        CONTROL_CHANGE_STRINGS[110] = "JJAZZ_MARKER_SYNC";
        CONTROL_CHANGE_STRINGS[111] = "JJAZZ_CHORD_CHANGE";
        CONTROL_CHANGE_STRINGS[112] = "JJAZZ_BEAT_CHANGE";
        CONTROL_CHANGE_STRINGS[113] = "JJAZZ_ACTIVITY_CHANGE";
        CONTROL_CHANGE_STRINGS[114] = "JJAZZ_TEMPO_FACTOR";
        CONTROL_CHANGE_STRINGS[120] = "ALL_SOUND_OFF";
        CONTROL_CHANGE_STRINGS[121] = "RESET_ALL_CONTROLLERS";
        CONTROL_CHANGE_STRINGS[123] = "ALL_NOTES_OFF";
    }

}
