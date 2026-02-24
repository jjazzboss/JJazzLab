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
package org.jjazz.midi.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.utilities.api.LongRange;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
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
     * Get MidiEvents converted from MidiEvents with a different PPQ resolution (Midi Pulses Per Quarter).
     * <p>
     * Note: if PPQ resolution is made smaller, it might be possible that the result contains zero-length notes (i.e. NOTE_ON and NOTE_OFF have same tick
     * position).
     *
     * @param srcEvents A list of MidiEvents at srcPPQ resolution
     * @param srcPPQ    E.g. 480
     * @param destPPQ   E.g. 960
     * @return A list of new MidiEvents at destPPQ resolution.
     */
    static public List<MidiEvent> getMidiEventsAtPPQ(List<MidiEvent> srcEvents, int srcPPQ, int destPPQ)
    {
        List<MidiEvent> res = new ArrayList<>();

        double tickRatio = (double) destPPQ / srcPPQ;

        for (var me : srcEvents)
        {
            long tick = (long) Math.round(tickRatio * me.getTick());
            res.add(new MidiEvent(me.getMessage(), tick));
        }

        return res;
    }

    /**
     * Get track MidiEvents whose tick position is within trackTickRange and which satisfy the specified tester.
     * <p>
     *
     * @param track
     * @param tester         Test the Midi event
     * @param trackTickRange If null there is no filtering on tick position. The range must be based on the track's resolution.
     * @return A list of track MidiEvents
     */
    static public List<MidiEvent> getMidiEvents(Track track, Predicate<MidiEvent> tester, LongRange trackTickRange)
    {
        var res = new ArrayList<MidiEvent>();

        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            long tick = me.getTick();
            if (trackTickRange != null && tick < trackTickRange.from)
            {
                continue;
            } else if (trackTickRange != null && tick > trackTickRange.to)
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
     * Get track MidiEvents whose MidiMessage is instance of msgClass, which satisfy the specified MidiMessage tester, and whose tick position is within
     * trackTickRange.
     * <p>
     * @param <T>
     * @param track
     * @param msgClass       MidiMessage class
     * @param msgTester      Test the MidiMessage of the MidiEvent
     * @param trackTickRange If null there is no filtering on tick position. The range must be based on the track's resolution.
     * @return A list of track MidiEvents
     */
    static public <T extends MidiMessage> List<MidiEvent> getMidiEvents(Track track, Class<T> msgClass, Predicate<T> msgTester, LongRange trackTickRange)
    {
        var res = new ArrayList<MidiEvent>();

        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            long tick = me.getTick();

            if (trackTickRange != null && tick < trackTickRange.from)
            {
                continue;
            } else if (trackTickRange != null && tick > trackTickRange.to)
            {
                break;
            }
            if (msgClass.isInstance(me.getMessage()))
            {
                T msg = (T) me.getMessage();
                if (msgTester.test(msg))
                {
                    res.add(me);
                }
            }
        }
        return res;
    }

    /**
     * Return the channels used in the specified track.
     *
     * @param track
     * @return
     */
    static public Set<Integer> getUsedChannels(Track track)
    {
        Set<Integer> res = new HashSet<>();
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            MidiMessage mm = me.getMessage();
            if (mm instanceof ShortMessage sm)
            {
                res.add(sm.getChannel());
            }
        }
        return res;
    }

    /**
     * Remove all MidiEvents from a track, but leave the End Of Track MetaEvent unchanged.
     *
     * @param track
     */
    static public void clearTrack(Track track)
    {
        int last = track.size() - 1;
        for (int i = last; i >= 0; i--)
        {
            MidiEvent me = track.get(i);
            MidiMessage mm = me.getMessage();
            if ((mm instanceof MetaMessage) && ((MetaMessage) mm).getType() == MidiConst.META_END_OF_TRACK)
            {
                continue;
            }
            track.remove(me);
        }
    }

    /**
     * Get a clone copy of all MidiEvents found in track.
     * <p>
     * @param track
     * @return The list will contain at least 1 MidiEvent, the special MetaEvent (type=47) marking the end of the track.
     */
    static public List<MidiEvent> getMidiEventsCopy(Track track)
    {
        var res = new ArrayList<MidiEvent>();
        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            MidiMessage mm = me.getMessage();
            MidiEvent newMe = new MidiEvent((MidiMessage) mm.clone(), me.getTick());
            res.add(newMe);
        }

        return res;
    }

    /**
     * Return a non-null ShortMessage only if mm is a ShortMessage.NOTE_ON or NOTE_OFF.
     *
     * @param mm
     * @return
     */
    static public ShortMessage getNoteShortMessage(MidiMessage mm)
    {
        if (mm instanceof ShortMessage sm)
        {
            if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
            {
                return sm;
            }
        }
        return null;
    }

    /**
     * Return a non-null ShortMessage only if mm is a ShortMessage.NOTE_ON with a velocity &gt; 0.
     *
     * @param mm
     * @return
     */
    static public ShortMessage getNoteOnShortMessage(MidiMessage mm)
    {
        if (mm instanceof ShortMessage sm)
        {
            if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0)
            {
                return sm;
            }
        }
        return null;
    }

    /**
     * Return a non-null ShortMessage only if mm is a ShortMessage.NOTE_OFF or ShortMessage.NOTE_ON with velocity = 0.
     *
     * @param mm
     * @return
     */
    static public ShortMessage getNoteOffShortMessage(MidiMessage mm)
    {
        if (mm instanceof ShortMessage sm)
        {
            if (sm.getCommand() == ShortMessage.NOTE_OFF || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0))
            {
                return sm;
            }
        }
        return null;
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
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
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
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
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
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        return sm;
    }

    static public SysexMessage getGsModeOnSysExMessage()
    {
        SysexMessage sm = new SysexMessage();
        byte[] data =
        {
            (byte) 0xF0, (byte) 0x41, (byte) 0x10, (byte) 0x42, (byte) 0x12, (byte) 0x40, (byte) 0x00, (byte) 0x7F, (byte) 0x00, (byte) 0x41,
            (byte) 0XF7
        };
        try
        {
            sm.setMessage(data, 11);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
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
        LOGGER.log(Level.FINE, "sendSysExMessage() sm={0}", sm);
        JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(sm);
        try
        {
            Thread.sleep(50);  // Give time for the hardware to execute
        } catch (InterruptedException ex)
        {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    /**
     * Build a ShortMessage.
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
            throw new IllegalArgumentException("command=" + command + " channel=" + channel + " d1=" + d1 + " d2=" + d2);
        }
        ShortMessage sm = new ShortMessage();
        try
        {
            sm.setMessage(command, channel, d1, d2);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, "Invalid Midi message: command=" + command + " channel=" + channel + " d1=" + d1 + " d2=" + d2, ex);
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

    static public ShortMessage getNoteOnMessage(int channel, int pitch, int velocity)
    {
        return buildMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
    }

    static public ShortMessage getNoteOffMessage(int channel, int pitch)
    {
        return buildMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
    }

    static public MetaMessage getTimeSignatureMessage(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new IllegalArgumentException("ts=" + ts);
        }
        byte powerOf2;
        switch (ts.getLower())
        {
            case 2 -> powerOf2 = 1;
            case 4 -> powerOf2 = 2;
            case 8 -> powerOf2 = 3;
            default -> throw new IllegalArgumentException("ts=" + ts);
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
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return mm;
    }

    static public MetaMessage getTempoMessage(int channel, int bpm)
    {
        if (!MidiConst.checkMidiChannel(channel) || bpm < 10 || bpm > 400)
        {
            throw new IllegalArgumentException("channel=" + channel + " tempo=" + bpm);
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
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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
        ShortMessage[] sms;
        BankSelectMethod bsm = ins.getMidiAddress().getBankSelectMethod();
        int bankMSB = ins.getMidiAddress().getBankMSB();
        int bankLSB = ins.getMidiAddress().getBankLSB();
        int pc = ins.getMidiAddress().getProgramChange();
        if (bsm == null || bankMSB > 127 || bankLSB > 127)
        {
            throw new IllegalArgumentException(
                    "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                            getBank());
        }
        switch (bsm)
        {
            case MSB_LSB ->
            {
                if (bankMSB < 0 || bankLSB < 0)
                {
                    throw new IllegalArgumentException(
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
            }
            case MSB_ONLY ->
            {
                if (bankMSB < 0)
                {
                    throw new IllegalArgumentException(
                            "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                                    getBank());
                }
                sms = new ShortMessage[2];
                // Bank Select MSB
                sms[0] = buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_BANK_SELECT_MSB, bankMSB);
                // Program Change
                sms[1] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
            }
            case LSB_ONLY ->
            {
                if (bankLSB < 0)
                {
                    throw new IllegalArgumentException(
                            "bsm=" + bsm + " bankMSB=" + bankMSB + " bankLSB=" + bankLSB + " channel=" + channel + " ins=" + ins + " ins.bank=" + ins.
                                    getBank());
                }
                sms = new ShortMessage[2];
                // Bank Select LSB
                sms[0] = buildMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_BANK_SELECT_LSB, bankLSB);
                // Program Change
                sms[1] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
            }
            default ->
            {
                // PC_ONLY
                sms = new ShortMessage[1];
                sms[0] = buildMessage(ShortMessage.PROGRAM_CHANGE, channel, pc, 0);
            }
        }
        LOGGER.log(Level.FINE, "getPatchMessages() chan={0} ins={1} bsm={2}", new Object[]
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
        LOGGER.warning("getPitchBendMessages() NOT VALIDED YET !");
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

    /**
     * Utility to convert milliseconds to beat fraction at given tempo.
     *
     * @param ms    Time in milliseconds
     * @param tempo Tempo in BPM
     * @return Beat fraction
     */
    static public float msToBeats(float ms, float tempo)
    {
        return (ms / 60000f) * tempo;
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
     * @param tempoMPQ   Tempo in microseconds per quarter
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
     * @param tempoMPQ   Tempo in microseconds per quarter
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
            if (msg instanceof MetaMessage mm)
            {
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
            throw new IllegalArgumentException("txt=" + txt);
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(MidiConst.META_TRACKNAME, txt.getBytes(), txt.length());
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
            throw new IllegalArgumentException("txt=" + txt);
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(MidiConst.META_TEXT, txt.getBytes(), txt.length());
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
            throw new IllegalArgumentException("txt=" + txt);
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(MidiConst.META_MARKER, txt.getBytes(), txt.length());
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
            throw new IllegalArgumentException("txt=" + txt);
        }
        MetaMessage mm = null;
        try
        {
            mm = new MetaMessage(MidiConst.META_COPYRIGHT, txt.getBytes(), txt.length());
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return mm;
    }

    /**
     * Check if the Midi sequence supports the specified Midi file type.
     *
     * @param sequence
     * @param fileType   0 or 1
     * @param notifyUser If true and fileType is not supported, notify end user.
     * @return True if fileType is supported
     */
    static public boolean checkMidiFileTypeSupport(Sequence sequence, int fileType, boolean notifyUser)
    {
        checkNotNull(sequence);
        checkArgument(fileType == 0 || fileType == 1, "Invalid file type=%s", fileType);

        int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
        boolean res = false;
        for (int type : fileTypes)
        {
            if (type == fileType)
            {
                res = true;
                break;
            }
        }

        if (!res && notifyUser)
        {
            String msg = ResUtil.getString(MidiUtilities.class, "ERR_MidiFileTypeNotSupported", fileType);
            LOGGER.warning(msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        }

        return res;
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
                if (msg instanceof ShortMessage sm)
                {
                    int channel = sm.getChannel();
                    if (channel != destChannel && fromChannels.contains(channel))
                    {
                        try
                        {
                            sm.setMessage(sm.getCommand(), destChannel, sm.getData1(), sm.getData2());
                        } catch (InvalidMidiDataException ex)
                        {
                            LOGGER.log(Level.WARNING, "rerouteShortMessages() ex={0}", ex.getMessage());
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
            throw new IllegalArgumentException("srcTick=" + srcTick + " srcPPQresolution=" + srcPPQresolution);
        }
        double ratio = ((double) srcTick) / srcPPQresolution;
        long tick = Math.round(MidiConst.PPQ_RESOLUTION * ratio);
        return tick;
    }

    /**
     * Change the global duration of the sequence.
     * <p>
     * If tickEnd is shorter than current sequence duration, remove all MidiEvents after tickEnd and adjust notes onsets to stop no later than tickEnd. If
     * tickEnd is greater than current sequence duration, just change end event position.
     *
     * @param sequence
     * @param tickEnd
     */
    static public void setSequenceDuration(Sequence sequence, long tickEnd)
    {
        if (sequence.getTickLength() > tickEnd)
        {
            // Remove MidiEvents which start after tickEnd, or make notes onset shorter
            Track[] tracks = sequence.getTracks();
            for (Track track : tracks)
            {
                if (track.ticks() < tickEnd)
                {
                    // This track is shorter, don't bother                
                    continue;
                }

                List<MidiEvent> eventsToRemove = new ArrayList<>();
                List<MidiEvent> eventsToAdd = new ArrayList<>();

                long lastNoteOnPerChannel[][] = new long[16][128];  // Store tick+1 in the array, so 0 means not initialized                
                for (int j = 0; j < track.size(); j++)
                {
                    MidiEvent me = track.get(j);
                    long tick = me.getTick();
                    MidiMessage mm = me.getMessage();
                    if ((mm instanceof MetaMessage mem) && mem.getType() == MidiConst.META_END_OF_TRACK)
                    {
                        // Special End Event: ignore
                        continue;
                    }


                    if (tick > tickEnd)
                    {
                        // We passed tickEnd, remove everything but re-add a NoteOff if NoteOn still pending
                        if (mm instanceof ShortMessage sm)
                        {
                            int pitch = sm.getData1();
                            if (sm.getCommand() == ShortMessage.NOTE_OFF)
                            {
                                if (lastNoteOnPerChannel[sm.getChannel()][pitch] > 0 && lastNoteOnPerChannel[sm.getChannel()][pitch] - 1 < tickEnd)
                                {
                                    try
                                    {
                                        // Need to shorten the note, add a NoteOff at tickEnd - 1
                                        sm = new ShortMessage(ShortMessage.NOTE_OFF, sm.getChannel(), pitch, 0);
                                    } catch (InvalidMidiDataException ex)
                                    {
                                        // Should never happen
                                        Exceptions.printStackTrace(ex);
                                    }
                                    eventsToAdd.add(new MidiEvent(sm, tickEnd - 1));
                                }
                            }
                        }
                        eventsToRemove.add(me);
                    } else
                    {
                        // Before tickEnd, keep everything, but keep track on NoteON tick start
                        if (mm instanceof ShortMessage sm)
                        {
                            int pitch = sm.getData1();
                            if (sm.getCommand() == ShortMessage.NOTE_ON)
                            {
                                if (tick < tickEnd)
                                {
                                    lastNoteOnPerChannel[sm.getChannel()][pitch] = tick + 1;
                                } else
                                {
                                    // tick == tickEnd
                                    eventsToRemove.add(me);
                                }
                            } else if (sm.getCommand() == ShortMessage.NOTE_OFF)
                            {
                                // Clear the last NoteOn tick position
                                lastNoteOnPerChannel[sm.getChannel()][pitch] = 0;
                            }
                        }
                    }
                } // Next event

                // Update the track
                eventsToRemove.forEach(me -> track.remove(me));
                eventsToAdd.forEach(me -> track.add(me));
            } // Next track


            // Make sure all tracks share the same EndOfTrack event
            for (var track : sequence.getTracks())
            {
                setEndOfTrackPosition(track, tickEnd);
            }
        }
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
            throw new IllegalArgumentException("tempoMsg=" + tempoMsg);
        }
        int mspq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
        int tempo = Math.round(60000001f / mspq);
        return tempo;
    }

    /**
     * Retrieve the text from the specified MidiEvent message, when possible.
     * <p>
     * The event's message should be a text-based MetaMessage such TrackName, Text, Marker, Lyrics, Copyright, Instrument.
     *
     * @param me
     * @return Null if the event's message is not a text-based MetaMessage
     *
     */
    static public String getText(MidiEvent me)
    {
        if (!(me.getMessage() instanceof MetaMessage))
        {
            return null;
        }
        MetaMessage mm = (MetaMessage) me.getMessage();
        String res = switch (mm.getType())
        {
            case MidiConst.META_TEXT, MidiConst.META_COPYRIGHT, MidiConst.META_TRACKNAME, MidiConst.META_INSTRUMENT, MidiConst.META_LYRICS, MidiConst.META_MARKER ->
                Utilities.toString(mm.getData());
            default ->
                null;
        };
        return res;
    }

    /**
     * Try to guess if patchName represents a drums/percussion instrument.
     *
     * @param patchName
     * @return
     */
    static public boolean guessIsPatchNameDrums(String patchName)
    {
        // Exclude drum kits
        String s = patchName.trim().toLowerCase();
        boolean b = !s.contains("steel") && (s.contains("drum") || s.contains("kit") || s.contains("kt:") || s.contains("dr:") || s.contains("drm:"));
        return b;
    }

    /**
     * Provide an explicit string for a MidiMessage.
     *
     * @param msg  A MidiMessage.
     * @param tick The tick of the MidiMessage. Ignore if &lt; 0.
     * @return A string representing the MidiMessage.
     */
    public static String toString(MidiMessage msg, long tick)
    {
        StringBuilder sb = new StringBuilder();

        // First class name and status byte
        sb.append(String.format("%s (st=%3d)", msg.getClass().getSimpleName(), msg.getStatus()));

        // for ShortMessage
        if (msg instanceof ShortMessage sm)
        {
            sb.append(String.format(" ch=%-2d", sm.getChannel()));
            String cmd = getShortMessageCommandString(sm.getCommand());
            if (cmd.equals("?"))
            {
                cmd = String.valueOf(sm.getCommand());
            }
            sb.append(String.format(" cmd=%-8s", cmd));
            String d1 = String.valueOf(sm.getData1()); // By default display the value
            if (sm.getCommand() == ShortMessage.CONTROL_CHANGE)
            {
                // It's a control change, we can display a text
                d1 = getControllerChangeString(sm.getData1());
            } else if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
            {
                // Display the note instead of pitch
                Note n = new Note(sm.getData1());
                d1 = n.toPianoOctaveString();
            }
            sb.append(String.format(" d1=%-4s", d1));
            sb.append(String.format("  d2=%-3d", sm.getData2()));
        } else if (msg instanceof SysexMessage sm)
        {
            sb.append(" SysEx ").append(String.format("  data=%h", sm.getData()));
        } else if (msg instanceof MetaMessage mm)
        {
            switch (mm.getType())
            {
                case MidiConst.META_TEXT -> // Text
                    sb.append(" text=").append(Utilities.toString(mm.getData()));
                case MidiConst.META_COPYRIGHT -> // Copyright
                    sb.append(" copyright=").append(Utilities.toString(mm.getData()));
                case MidiConst.META_TRACKNAME -> // TrackName
                    sb.append(" trackname=").append(Utilities.toString(mm.getData()));
                case MidiConst.META_INSTRUMENT -> // Instrument Name
                    sb.append(" instrumentName=").append(Utilities.toString(mm.getData()));
                case MidiConst.META_LYRICS -> // Lyrics
                    sb.append(" lyrics=").append(Utilities.toString(mm.getData()));
                case MidiConst.META_MARKER -> // Marker
                    sb.append(" marker=").append(Utilities.toString(mm.getData()));
                case MidiConst.META_TIME_SIGNATURE ->
                {
                    // TimeSignature
                    int upper = mm.getData()[0];
                    int lower = (int) Math.pow(2, mm.getData()[1]);
                    TimeSignature ts = TimeSignature.get(upper, lower);
                    sb.append(" timeSignature=").append(ts);
                }
                case MidiConst.META_TEMPO -> // Tempo
                    sb.append(" tempo=").append(getTempoInBPM(mm));
                default -> sb.append(" type=").append(mm.getType());
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
        for (Track track : tracks)
        {
            sb.append(toString(track));
        }
        sb.append("Sequence End\n");
        return sb.toString();
    }


    /**
     * Convert a list of MidiMessages to a save string.
     * <p>
     * Example: "SM.A1.27.C3-MM.BA.3.12"
     *
     * @param mms
     * @return
     * @see #loadMidiMessagesFromString(java.lang.String)
     *
     */
    static public String saveMidiMessagesAsString(List<MidiMessage> mms)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var mm : mms)
        {
            if (!first)
            {
                sb.append("-");
            }
            sb.append(saveMidiMessageAsString(mm));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Convert a MidiMessage as a String for backup purpose.
     * <p>
     * Example: "SM.A1.27.C3"
     *
     * @param mm
     * @return
     * @see #loadMidiMessageFromString(java.lang.String)
     */
    static public String saveMidiMessageAsString(MidiMessage mm)
    {
        checkNotNull(mm);
        StringBuilder s = new StringBuilder();
        if (mm instanceof ShortMessage)
        {
            s.append("SM");
        } else if (mm instanceof MetaMessage)
        {
            s.append("MM");
        } else
        {
            s.append("XM");
        }
        for (byte b : mm.getMessage())
        {
            s.append(".").append(Integer.toHexString((int) (b & 0xFF)));
        }
        return s.toString();
    }

    /**
     * Retrieve a list of MidiMessages from a save string.
     *
     * @param s
     * @return
     * @throws java.text.ParseException
     * @throws javax.sound.midi.InvalidMidiDataException
     * @see #saveMidiMessagesAsString(java.util.List)
     */
    static public List<MidiMessage> loadMidiMessagesFromString(String s) throws ParseException, InvalidMidiDataException
    {
        List<MidiMessage> res = new ArrayList<>();
        String[] strs = s.split("-");
        for (String str : strs)
        {
            MidiMessage mm = loadMidiMessageFromString(str);
            res.add(mm);
        }
        return res;
    }

    /**
     * Retrieve a MidiMessage from a save string.
     *
     * @param s
     * @return
     * @throws java.text.ParseException
     * @throws javax.sound.midi.InvalidMidiDataException
     * @see #saveMidiMessageAsString(javax.sound.midi.MidiMessage)
     */
    static public MidiMessage loadMidiMessageFromString(String s) throws ParseException, InvalidMidiDataException
    {
        // Our own subclasses to get access to the protected MidiMessage.setMessage(byte[]) method
        class ShortMessageSpecial extends ShortMessage
        {

            public void setMessage(byte[] data) throws InvalidMidiDataException
            {
                super.setMessage(data, data.length);
            }
        }

        class MetaMessageSpecial extends MetaMessage
        {

            public void setMessage(byte[] data) throws InvalidMidiDataException
            {
                super.setMessage(data, data.length);
            }
        }

        class SysexMessageSpecial extends SysexMessage
        {

            public void setMessage(byte[] data) throws InvalidMidiDataException
            {
                super.setMessage(data, data.length);
            }
        }

        checkNotNull(s);
        String[] strs = s.split("\\.");
        if (strs.length < 2)
        {
            throw new ParseException("Invalid Midi message save string '" + s + "'", 0);
        }
        byte[] data = new byte[strs.length - 1];


        for (int i = 1; i < strs.length; i++)
        {
            data[i - 1] = (byte) Integer.parseInt(strs[i], 16);
        }


        MidiMessage mm = switch (strs[0])
        {
            case "SM" ->
            {
                ShortMessageSpecial res = new ShortMessageSpecial();
                res.setMessage(data);
                yield res;
            }
            case "MM" ->
            {
                MetaMessageSpecial res = new MetaMessageSpecial();
                res.setMessage(data);
                yield res;
            }
            case "XM" ->
            {
                SysexMessageSpecial res = new SysexMessageSpecial();
                res.setMessage(data);
                yield res;
            }
            default -> throw new ParseException("Invalid Midi message save string '" + s + "'", 0);
        };

        return mm;
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
        assert tick >= 0 && t != null && t.size() > 0 : "t=" + t + " t.size()=" + t.size() + " tick=" + tick;
        MidiEvent me = t.get(t.size() - 1);
        MidiMessage mm = me.getMessage();
        if ((mm instanceof MetaMessage) && ((MetaMessage) mm).getType() == MidiConst.META_END_OF_TRACK)
        {
            me.setTick(tick);
            res = (me.getTick() == tick);      // Check position was correctly set
        }
        return res;
    }

    /**
     * Convert a ShortMessage controller id into an understandable string.
     * <p>
     * For ex. if controller == 7 return "VOLUME_MSB".
     *
     * @param controllerId
     * @return
     */
    public static String getControllerChangeString(int controllerId)
    {
        if (controllerId < 0 || controllerId >= 256)
        {
            throw new IllegalArgumentException("ctrl=" + controllerId);
        }
        if (CONTROL_CHANGE_STRINGS == null)
        {
            initControlChangeStrings();
        }

        return CONTROL_CHANGE_STRINGS[controllerId];
    }

    /**
     * Convert the ShortMessage command number into an understandable string.
     * <p>
     * For example if command==ShortMessage.CONTROL_CHANGE, return "CONTROL_CHANGE".
     *
     * @param command
     * @return
     */
    public static String getShortMessageCommandString(int command)
    {
        if (command < 0 || command > 255)
        {
            throw new IllegalArgumentException("cmd=" + command);
        }
        if (COMMAND_STRINGS == null)
        {
            initCommandStrings();
        }
        return COMMAND_STRINGS[command];
    }


    // =====================================================================================
    // Private methods
    // =====================================================================================
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
        CONTROL_CHANGE_STRINGS[120] = "ALL_SOUND_OFF";
        CONTROL_CHANGE_STRINGS[121] = "RESET_ALL_CONTROLLERS";
        CONTROL_CHANGE_STRINGS[123] = "ALL_NOTES_OFF";
    }


}
