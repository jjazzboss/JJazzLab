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
package org.jjazz.musiccontrol;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.util.Utilities;

/**
 * Record and store incoming Note_ON/OFF and Controller Midi messages.
 */
public class MidiRecorder implements MetaEventListener
{

    private static final int MSG_BUFFER_SIZE = 0x100000;     // 1M messages
    private static final String START_MARKER = "Recording Start";
    private static final Logger LOGGER = Logger.getLogger(MidiRecorder.class.getSimpleName());

    private final Transmitter midiInTransmitter;
    private final MyReceiver myReceiver;
    private final boolean useTimeStamp;
    private long usOffset = -1;
    private final long[] midiMessageMicroSeconds = new long[MSG_BUFFER_SIZE]; // In micro-second, 0 is start of sequence
    private final ShortMessage[] midiMessages = new ShortMessage[MSG_BUFFER_SIZE];
    private int nbMessages;
    private boolean recording;
    private boolean recordingOccured;
    private final long seqStartInPPQ;
    private final Sequence sequence;
    private final double songTempoFactor;

    /**
     * Create an object to record Midi input for the specified sequence.
     * <p>
     * If not already present a special marker MetaEvent will be inserted on track 0 at position seqStartInPPQ.
     *
     * @param seq The playback and record sequence.
     * @param seqStartInPPQ The position in ticks where playback will start.
     * @param songTempoFactor
     * @throws MidiUnavailableException
     */
    public MidiRecorder(Sequence seq, long seqStartInPPQ, double songTempoFactor) throws MidiUnavailableException
    {
        if (seq == null || seqStartInPPQ < 0 || seq.getTracks().length == 0 || songTempoFactor <= 0)
        {
            throw new IllegalArgumentException("seq=" + seq + " seqStartInPPQ=" + seqStartInPPQ + " songTempoFactor=" + songTempoFactor);
        }
        this.songTempoFactor = songTempoFactor;
        this.sequence = seq;
        this.seqStartInPPQ = seqStartInPPQ;


        addStartMarkerEvent(this.sequence, this.seqStartInPPQ);


        // Does Midi IN device supports time stamping ?
        var jms = JJazzMidiSystem.getInstance();
        var midiIn = jms.getDefaultInDevice();      // Don't use getJJazzMidiInDevice() here, we need getMicrosecondPosition() support
        assert midiIn != null;
        useTimeStamp = midiIn.getMicrosecondPosition() != -1;
        LOGGER.severe("MidiRecorder() ppqOffset=" + seqStartInPPQ + " useTimeStamp=" + useTimeStamp);


        // Connect Midi IN to our receiver
        myReceiver = new MyReceiver();
        midiInTransmitter = midiIn.getTransmitter();        // Possible MidiUnavailableException here
        midiInTransmitter.setReceiver(myReceiver);


        // To be notified at tick=0 (ASSUMPTION: there always is a Meta event at tick 0)
        jms.getDefaultSequencer().addMetaEventListener(this);

    }

    /**
     * Convert the acquired Midi input into a track.
     *
     * @param track The track for which Midi events must be added, must be part of the current sequence.
     */
    public void updateRecordedTrack(Track track)
    {
        assert Arrays.asList(sequence.getTracks()).contains(track);

        if (!isRecordingOccured())
        {
            return;
        }

        // Contain the JJazz tempo factor changes
        Track track0 = sequence.getTracks()[0];
        List<MidiEvent> tempoFactorChangeEvents = MidiUtilities.getMidiEvents(track0, ShortMessage.class,
                sm -> sm.getCommand() == ShortMessage.CONTROL_CHANGE && sm.getData1() == MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR,
                -1, Long.MAX_VALUE);

        List<TempoFactorChange> tempoChanges = tempoFactorChangeEvents.stream()
                .map(me -> new TempoFactorChange(me))
                .collect(Collectors.toList());
        
        // Add a leading tempo change if not already present
        if (tempoChanges.isEmpty() || tempoChanges.get(0).tick>0)
        {
            TempoFactorChange tfc = new TempoFactorChange(0, songTempoFactor);
            tempoChanges.add();
        }


        long tick = seqStartInPPQ;

        for (int i = 0; i < nbMessages; i++)
        {
            ShortMessage sm = midiMessages[i];
            long usTick = midiMessageMicroSeconds[i];
            long ppqTick = toTickPPQ(usTick,tempoChanges);


            float songPartTempoFactor = getCurrentSongPartTempoFactor(tick, tempoFactorChangeEvents);
            float tempoBPM = MidiConst.SEQUENCER_REF_TEMPO * songTempoFactor * songPartTempoFactor;

        }
    }

    public void setRecordingEnabled(boolean b)
    {
        recording = b;
        if (recording)
        {
            recordingOccured = true;
        }
    }

    public boolean isRecordingEnabled()
    {
        return recording;
    }

    /**
     * True if recording was enabled at least once since the creation of this object.
     *
     * @return
     */
    public boolean isRecordingOccured()
    {
        return recordingOccured;
    }

    /**
     * Must be called when this object is not used anymore.
     */
    public void cleanup()
    {
        JJazzMidiSystem.getInstance().getDefaultSequencer().removeMetaEventListener(this);
        myReceiver.close();
        midiInTransmitter.close();
    }

    public void dump()
    {
        LOGGER.info("dump() MidiRecorder:");
        for (int i = 0; i < nbMessages; i++)
        {
            LOGGER.info(String.format(" 0x%04x: %s - pos=%dus", i, MidiUtilities.toString(midiMessages[i], -1), midiMessageMicroSeconds[i]));
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the MetaEventListener interface
    //-----------------------------------------------------------------------
    @Override
    public void meta(MetaMessage meta)
    {
//        LOGGER.log(Level.SEVERE, "meta() msg={0} data={1}", new Object[]
//        {
//            MidiUtilities.toString(meta, -1), Utilities.toString(meta.getData())
//        });
        if (usOffset == -1)
        {
            if (meta.getType() == 6 && START_MARKER.equals(Utilities.toString(meta.getData())))
            {
                usOffset = useTimeStamp ? JJazzMidiSystem.getInstance().getDefaultInDevice().getMicrosecondPosition() : System.nanoTime() / 1000;
                assert usOffset != -1;
//                LOGGER.log(Level.SEVERE, "meta() usOffset={0}", usOffset);
            }
        }
    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================    
    private void addStartMarkerEvent(Sequence sequence, long seqStartInPPQ)
    {
        Track track = sequence.getTracks()[0];

        assert seqStartInPPQ < track.ticks() : "seqStartInPPQ=" + seqStartInPPQ + " track.ticks()=" + track.ticks();

        List<MidiEvent> res = MidiUtilities.getMidiEvents(track, MetaMessage.class, mm
                -> mm.getType() == 6 && Utilities.toString(mm.getData()).equals(START_MARKER),
                seqStartInPPQ, seqStartInPPQ);


        if (res.isEmpty())
        {
            // Add our special marker
            MidiMessage mm = MidiUtilities.getMarkerMetaMessage(START_MARKER);
            track.add(new MidiEvent(mm, seqStartInPPQ));
        }
    }

    /**
     * Get the current tempo song part factor at specified tick location.
     *
     * @param tick
     * @param tempoFactorChangeEvents All the JJazz tempo factor MidiEvents.
     * @return
     */
    private float getCurrentSongPartTempoFactor(long tick, List<MidiEvent> tempoFactorChangeEvents)
    {
        assert !tempoFactorChangeEvents.isEmpty() && tempoFactorChangeEvents.get(0).getTick() == 0 :
                "tempoFactorChangeEvents=" + tempoFactorChangeEvents;


        MidiEvent me = null;

        if (tick == 0)
        {
            me = tempoFactorChangeEvents.get(0);
        } else if (tick >= tempoFactorChangeEvents.get(tempoFactorChangeEvents.size() - 1).getTick())
        {
            me = tempoFactorChangeEvents.get(tempoFactorChangeEvents.size() - 1);
        } else
        {
            for (int i = 0; i < tempoFactorChangeEvents.size(); i++)
            {
                if (tempoFactorChangeEvents.get(i).getTick() == tick)
                {
                    me = tempoFactorChangeEvents.get(i);
                    break;
                } else if (tempoFactorChangeEvents.get(i).getTick() > tick)
                {
                    me = tempoFactorChangeEvents.get(i - 1);
                    break;
                }
            }
        }

        assert me != null;

        return MidiUtilities.getTempoFactor((ShortMessage) me.getMessage());

    }

    /**
     * Convert a tick in micro-seconds into a PPQ tick.
     *
     * @param usTick
     * @param tempoChanges 
     * @return
     */
    private long toTickInPPQ(long usTick, List<TempoFactorChange> tempoChanges)
    {
        int nbTempoChanges = tempoChanges.size();
        assert nbTempoChanges > 0 && tempoChanges.get(0).getTick() == 0 :
                "tempoFactorChangeEvents=" + tempoChanges;

        MidiEvent me = null;
        long res = 0;
        long us = 0;
        int resolution = sequence.getResolution();
        int i;

        long curTempoTick = 0;
        long prevTempoTick = 0;
        double prevTempoMPQ = 120;

        if (nbTempoChanges == 1)
        {
            prevTempoTick = tempoChanges.get(0).getTick();
            double songPartTempoFactor = MidiUtilities.getTempoFactor((ShortMessage) tempoChanges.get(0).getMessage());
            prevTempoMPQ = MidiUtilities.toTempoMPQ(songPartTempoFactor * songTempoFactor * MidiConst.SEQUENCER_REF_TEMPO);

        } else
        {
            for (i = 1; i < nbTempoChanges; i++)
            {
                curTempoTick = tempoChanges.get(i).getTick();
                prevTempoTick = tempoChanges.get(i - 1).getTick();
                double prevSongPartTempoFactor = MidiUtilities.getTempoFactor((ShortMessage) tempoChanges.get(i - 1).getMessage());
                prevTempoMPQ = MidiUtilities.toTempoMPQ(prevSongPartTempoFactor * songTempoFactor * MidiConst.SEQUENCER_REF_TEMPO);

                long nextTime = us + MidiUtilities.toTickInUs(curTempoTick - prevTempoTick, prevTempoMPQ, resolution);
                if (nextTime > usTick)
                {
                    break;
                }
                us = nextTime;
            }
        }

        res = prevTempoTick + MidiUtilities.toTickInPPQ(usTick - us, prevTempoMPQ, resolution);

        return res;

    }

    // ==========================================================================================
    // Inner classes
    // ==========================================================================================    
    private class TempoFactorChange
    {

        public long tick;
        public double tempoBPM;
        public double tempoMPQ;

        public TempoFactorChange(long tick, double songPartTempoChange)
        {
            this.tick = tick;
            this.tempoBPM = MidiConst.SEQUENCER_REF_TEMPO * songTempoFactor * songPartTempoChange;
            this.tempoMPQ = MidiUtilities.toTempoMPQ(tempoBPM);
        }

        public TempoFactorChange(MidiEvent me)
        {
            this(me.getTick(), MidiUtilities.getTempoFactor((ShortMessage) me.getMessage()));
        }
    }

    private class MyReceiver implements Receiver
    {

        /**
         * Store incoming Note_ON/OFF and controller messages.
         *
         * @param msg
         * @param timeStamp
         */
        @Override
        public void send(MidiMessage msg, long timeStamp)
        {

            if (!recording || !(msg instanceof ShortMessage))
            {
                return;
            }

            ShortMessage sm = (ShortMessage) msg;
            if ((sm.getStatus() & 0xF0) == 0xF0)
            {
                // All real-time messages have 0xF in the high nibble of the status byte
                return;
            }

//            LOGGER.severe("send() " + MidiUtilities.toString(msg, timeStamp));

            if (nbMessages >= MSG_BUFFER_SIZE)
            {
                LOGGER.log(Level.WARNING, "send() Midi message input buffer is full, ignoring message {0} timeStamp={1}", new Object[]
                {
                    sm, timeStamp
                });
                return;
            }


            if (usOffset >= 0)
            {
                midiMessages[nbMessages] = sm;
                midiMessageMicroSeconds[nbMessages] = (useTimeStamp ? timeStamp : System.nanoTime() / 1000) - usOffset;
                nbMessages++;
            } else
            {
                LOGGER.log(Level.WARNING, "send() Unexpected incoming Midi message={0} timeStamp={1} while usOffset={2}", new Object[]
                {
                    sm, timeStamp, usOffset
                });
            }

        }

        @Override
        public void close()
        {
            LOGGER.fine("close() --");
        }

    }

}
