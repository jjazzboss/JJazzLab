/**
 * A JJazz Midi parser based on JFugue 5.0 MidiParser.java.
 * Parse a Midi sequence and fire events to MidiParserListeners.
 * AuxilliaryMidiParser can be used to manage events not handled by this MidiParser.
 */

/*
 * JFugue, an Application Programming Interface (API) for Music Programming
 * http://www.jfugue.org
 *
 * Copyright (C) 2003-2014 David Koelle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
package org.jjazz.midi.api.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiUtilities;

public class MidiParser
{

    public static final byte META_SEQUENCE_NUMBER = 0;
    public static final byte META_TEXT_EVENT = 1;
    public static final byte META_COPYRIGHT_NOTICE = 2;
    public static final byte META_TRACK_NAME = 3;
    public static final byte META_INSTRUMENT_NAME = 4;
    public static final byte META_LYRIC = 5;
    public static final byte META_MARKER = 6;
    public static final byte META_CUE_POINT = 7;
    public static final byte META_MIDI_CHANNEL_PREFIX = 32;
    public static final byte META_END_OF_TRACK = 47;
    public static final byte META_TEMPO = 81;
    public static final byte META_SMTPE_OFFSET = 84;
    public static final byte META_TIMESIG = 88;
    public static final byte META_KEYSIG = 89;
    public static final byte META_VENDOR = 127;
    public static final double MS_PER_MIN = 60000.0;

    public interface AuxilliaryMidiParser
    {

        public void parseHandledMidiEvent(MidiEvent me, MidiParser mp);

        public void parseUnhandledMidiEvent(MidiEvent me, MidiParser mp);
    }

    private List<Map<Byte, TempNote>> noteCache;
    private int resolutionTicksPerBeat;
    private int tempoBPM = 120;
    private int currentChannel = -1;
    private double[] currentTimeInBeats;
    private double[] expectedTimeInBeats;
    private boolean skipPitchError = false;
    private boolean skipVelocityError = false;
    private boolean skipUselessNoteError = false;
    private String name;
    private ArrayList<AuxilliaryMidiParser> auxilliaryParsers;
    private ArrayList<MidiParserListener> parserListeners;

    protected static final Logger LOGGER = Logger.getLogger(MidiParser.class.getSimpleName());

    public MidiParser()
    {
        super();
        auxilliaryParsers = new ArrayList<>();
        parserListeners = new ArrayList<>();
    }

    /**
     * Parse the Midi sequence track by track and notifies the registered parsers.
     *
     * @param sequence
     * @param name Optional name use when logging errors. Can be null.
     */
    public void parse(Sequence sequence, String name)
    {
        this.name = (name == null) ? "" : name;

        fireBeforeParsingStarts();

        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            LOGGER.log(Level.SEVERE, "{0} - parse() sequence.getDivisionType() is not PPQ, can''t parse.", this.name);   
            return;
        }
        this.resolutionTicksPerBeat = sequence.getResolution();

        skipPitchError = false;
        skipVelocityError = false;
        skipUselessNoteError = false;

        // Problem ! If multi-track file, noteCache can be wrong if NoteOn/Off
        // are not correct at the end and beginning of several tracks
        // Easier to reset the note cache for each track, it assumes note On/Off
        // should be within each track.
        // initNoteCache();       
        for (Track track : sequence.getTracks())
        {
            initNoteCache();
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                parseEvent(event);
            }
        }

        fireAfterParsingFinished();
    }

    private void initNoteCache()
    {
        noteCache = new ArrayList<Map<Byte, TempNote>>();
        this.currentTimeInBeats = new double[16];
        this.expectedTimeInBeats = new double[16];

        for (int i = 0; i < 16; i++)
        {
            noteCache.add(new HashMap<Byte, TempNote>());
            this.currentTimeInBeats[i] = 0.0d;
            this.expectedTimeInBeats[i] = 0.0d;
        }
    }

    /**
     * Parses the following messages:<br>
     * Note On events ,Note Off events ,Polyphonic Aftertouch ,Controller Events ,Program Change (instrument changes) ,Channel
     * Aftertouch ,Pitch Wheel <br>
     * Meta Events: Tempo, Lyric, Marker, Key Signature, Time Signature, Marker, Text, TrackName<br>
     * SysEx Events
     * <p>
     * Any other MIDI messages (particularly, other Meta Events) are not handled by this MidiParser.
     * <p>
     * You may implement an AuxilliaryMidiParser to know when MidiParser has parsed or not parsed a given MIDI message.
     *
     * @see AuxilliaryMidiParser
     *
     * @param event the event to parse
     */
    public void parseEvent(MidiEvent event)
    {
        MidiMessage message = event.getMessage();
        if (message instanceof ShortMessage)
        {
            parseShortMessage((ShortMessage) message, event);
        } else if (message instanceof MetaMessage)
        {
            parseMetaMessage((MetaMessage) message, event);
        } else if (message instanceof SysexMessage)
        {
            parseSysexMessage((SysexMessage) message, event);
        } else
        {
            fireUnhandledMidiEvent(event);
        }
    }

    public void addParserListener(MidiParserListener listener)
    {
        parserListeners.add(listener);
    }

    public void removeParserListener(MidiParserListener listener)
    {
        parserListeners.remove(listener);
    }

    public void addAuxilliaryMidiParser(AuxilliaryMidiParser auxilliaryParser)
    {
        auxilliaryParsers.add(auxilliaryParser);
    }

    public void removeAuxilliaryMidiParser(AuxilliaryMidiParser auxilliaryParser)
    {
        auxilliaryParsers.remove(auxilliaryParser);
    }

    // ==========================================================================
    // Private methods
    // ==========================================================================    
    private void parseShortMessage(ShortMessage message, MidiEvent event)
    {
        // For any message that isn't a NoteOn event, update the current time and channel.
        // (We don't do this for NoteOn events because NoteOn aren't written until the NoteOff event)
        if (!isNoteOnEvent(message.getCommand(), message.getChannel(), event))
        {
            checkChannel(message.getChannel());
        }

        switch (message.getCommand())
        {
            case ShortMessage.NOTE_OFF:
                noteOff(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            case ShortMessage.NOTE_ON:
                noteOn(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            case ShortMessage.POLY_PRESSURE:
                polyphonicAftertouch(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            case ShortMessage.CONTROL_CHANGE:
                controlChange(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            case ShortMessage.PROGRAM_CHANGE:
                programChange(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            case ShortMessage.CHANNEL_PRESSURE:
                channelAftertouch(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            case ShortMessage.PITCH_BEND:
                pitchWheel(message.getChannel(), event);
                fireHandledMidiEvent(event);
                break;
            default:
                fireUnhandledMidiEvent(event);
                break;
        }
    }

    private void parseMetaMessage(MetaMessage message, MidiEvent event)
    {
        switch (message.getType())
        {
            case META_SEQUENCE_NUMBER:
                fireUnhandledMidiEvent(event);
                break;
            case META_TEXT_EVENT:
                textParsed(message, event);
                fireHandledMidiEvent(event);
                break;
            case META_COPYRIGHT_NOTICE:
                fireUnhandledMidiEvent(event);
                break;
            case META_TRACK_NAME:
                trackNameParsed(message, event);
                fireHandledMidiEvent(event);
                break;
            case META_INSTRUMENT_NAME:
                fireUnhandledMidiEvent(event);
                break;
            case META_LYRIC:
                lyricParsed(message, event);
                fireHandledMidiEvent(event);
                break;
            case META_MARKER:
                markerParsed(message, event);
                fireHandledMidiEvent(event);
                break;
            case META_CUE_POINT:
                fireUnhandledMidiEvent(event);
                break;
            case META_MIDI_CHANNEL_PREFIX:
                fireUnhandledMidiEvent(event);
                break;
            case META_END_OF_TRACK:
                metaEndParsed(event);
                fireHandledMidiEvent(event);
                break;
            case META_TEMPO:
                tempoChanged(message, event);
                fireHandledMidiEvent(event);
                break;
            case META_SMTPE_OFFSET:
                fireUnhandledMidiEvent(event);
                break;
            case META_TIMESIG:
                timeSigParsed(message, event);
                fireHandledMidiEvent(event);
                break;
            case META_KEYSIG:
                fireUnhandledMidiEvent(event);
                break;
            case META_VENDOR:
                fireUnhandledMidiEvent(event);
                break;
            default:
                fireUnhandledMidiEvent(event);
                break;
        }
    }

    private void parseSysexMessage(SysexMessage message, MidiEvent event)
    {
        sysexParsed(message, event);
        fireHandledMidiEvent(event);
    }

    private boolean isNoteOnEvent(int command, int channel, MidiEvent event)
    {
        return ((command == ShortMessage.NOTE_ON) && !((noteCache.get(channel).get(event.getMessage().getMessage()[1]) != null)
                && (event.getMessage().getMessage()[2] == 0)));
    }

    private boolean isNoteOffEvent(int command, int channel, MidiEvent event)
    {
        // An event is a NoteOff event if it is actually a NoteOff event, 
        // or if it is a NoteOn event where the note has already been played and the attack velocity is 0. 
        return ((command == ShortMessage.NOTE_OFF)
                || ((command == ShortMessage.NOTE_ON)
                && (noteCache.get(channel).get(event.getMessage().getMessage()[1]) != null)
                && (event.getMessage().getMessage()[2] == 0)));
    }

    private void noteOff(int channel, MidiEvent event)
    {
        byte note = event.getMessage().getMessage()[1];
        if (note < 0)
        {
            // Robustness to corrupted Midi files
            if (!skipPitchError)
            {
                LOGGER.log(Level.INFO,   
                        name + " - noteOff() invalid Midi note pitch={0} channel={1} posInBeats={2}. Skipping similar errors...",
                        new Object[]
                        {
                            note, channel, getDurationInBeats(event.getTick())
                        });
                skipPitchError = true;
            }

            return;
        }

        TempNote tempNote = noteCache.get(channel).get(note);
        if (tempNote == null)
        {
            // A note was turned off when that note was never indicated as having been turned on
            return;
        }
        noteCache.get(channel).remove(note);
        float startTime = getDurationInBeats(tempNote.startTick);
        this.currentTimeInBeats[this.currentChannel] = startTime;

        long durationInTicks = event.getTick() - tempNote.startTick;
        assert durationInTicks >= 0 : "channel=" + channel + " durationInTicks=" + durationInTicks + " note=" + note + " event=<" + MidiUtilities.   
                toString(event.getMessage(), event.getTick()) + "> tempNote=" + tempNote;
        double durationInBeats = getDurationInBeats(durationInTicks);
        this.expectedTimeInBeats[this.currentChannel] = this.currentTimeInBeats[this.currentChannel] + durationInBeats;

        if (durationInBeats > 0)
        {
            // Some corrupted files have a note with abnormally short durations...
            Note noteObject = new Note(note, (float) durationInBeats, tempNote.noteOnVelocity);
            fireNoteParsed(noteObject, startTime);
        }
    }

    private void noteOn(int channel, MidiEvent event)
    {
        if (isNoteOffEvent(ShortMessage.NOTE_ON, channel, event))
        {
            // Some MIDI files use the Note On event with 0 velocity to indicate Note Off
            noteOff(channel, event);
            return;
        }

        byte note = event.getMessage().getMessage()[1];
        if (note < 0)
        {
            // Robustness to corrupted Midi files
            if (!skipPitchError)
            {
                LOGGER.log(Level.INFO,   
                        name + " - noteOn() invalid Midi note pitch={0} channel={1} posInBeats={2}. Skipping similar errors...",
                        new Object[]
                        {
                            note, channel, getDurationInBeats(event.getTick())
                        });
                skipPitchError = true;
            }
            return;
        }

        byte noteOnVelocity = event.getMessage().getMessage()[2];
        if (noteOnVelocity == 0)
        {
            // It's not a disguised NoteOFF because isNoteOffEvent() did not recognize it before.
            // Still the note is useless and probably an error in the Midi File.
            // Safer to ignore it
            if (!skipUselessNoteError)
            {
                LOGGER.log(Level.FINE,   
                        name + " - noteOn() useless Midi note velocity={0} channel={1} posInBeats={2}. Skipping similar errors...",
                        new Object[]
                        {
                            noteOnVelocity, channel, getDurationInBeats(event.getTick())
                        });
                skipUselessNoteError = true;
            }
            return;
        } else if (noteOnVelocity < 0)
        {
            if (!skipVelocityError)
            {
                LOGGER.log(Level.INFO,   
                        name + " - noteOn() invalid Midi note velocity={0} channel={1} posInBeats={2}. Skipping similar errors...",
                        new Object[]
                        {
                            noteOnVelocity, channel, getDurationInBeats(event.getTick())
                        });
                skipVelocityError = true;
            }
            return;
        }

        if (noteCache.get(channel).get(note) != null)
        {
            // The note already existed in the cache! Nothing to do about it now. This shouldn't happen.
        } else
        {
            noteCache.get(channel).put(note, new TempNote(event.getTick(), noteOnVelocity));
        }
    }

    private void polyphonicAftertouch(int channel, MidiEvent event)
    {
        firePolyphonicPressureParsed(event.getMessage().getMessage()[1], event.getMessage().getMessage()[2], getDurationInBeats(event.
                getTick()));
    }

    private void controlChange(int channel, MidiEvent event)
    {
        fireControllerEventParsed(event.getMessage().getMessage()[1], event.getMessage().getMessage()[2],
                getDurationInBeats(event.getTick()));
    }

    private void programChange(int channel, MidiEvent event)
    {
        fireInstrumentParsed(event.getMessage().getMessage()[1], getDurationInBeats(event.getTick()));
    }

    private void channelAftertouch(int channel, MidiEvent event)
    {
        fireChannelPressureParsed(event.getMessage().getMessage()[1], getDurationInBeats(event.getTick()));
    }

    private void pitchWheel(int channel, MidiEvent event)
    {
        firePitchWheelParsed(event.getMessage().getMessage()[1], event.getMessage().getMessage()[2], getDurationInBeats(event.getTick()));
    }

    private void tempoChanged(MetaMessage meta, MidiEvent event)
    {
        int newTempoMSPQ = (meta.getData()[2] & 0xFF)
                | ((meta.getData()[1] & 0xFF) << 8)
                | ((meta.getData()[0] & 0xFF) << 16);
        this.tempoBPM = 60000000 / newTempoMSPQ;
        fireTempoChanged(tempoBPM, getDurationInBeats(event.getTick()));
    }

    private void lyricParsed(MetaMessage meta, MidiEvent event)
    {
        fireLyricParsed(new String(meta.getData()), getDurationInBeats(event.getTick()));
    }

    private void trackNameParsed(MetaMessage meta, MidiEvent event)
    {
        fireSequenceNameParsed(new String(meta.getData()), getDurationInBeats(event.getTick()));
    }

    private void textParsed(MetaMessage meta, MidiEvent event)
    {
        fireTextParsed(new String(meta.getData()), getDurationInBeats(event.getTick()));
    }

    private void metaEndParsed(MidiEvent event)
    {
        fireMetaEndParsed(getDurationInBeats(event.getTick()));
    }

    private void markerParsed(MetaMessage meta, MidiEvent event)
    {
        fireMarkerParsed(new String(meta.getData()), getDurationInBeats(event.getTick()));
    }

    private void timeSigParsed(MetaMessage meta, MidiEvent event)
    {
        fireTimeSignatureParsed(meta.getData()[0], meta.getData()[1], getDurationInBeats(event.getTick()));
    }

    private void sysexParsed(SysexMessage sysex, MidiEvent event)
    {
        fireSystemExclusiveParsed(getDurationInBeats(event.getTick()), sysex.getData());
    }

    private void checkChannel(int channel)
    {
        if (this.currentChannel != channel)
        {
            this.currentChannel = channel;
            fireChannelChanged((byte) channel);
        }
    }

    //
    // Formulas and converters
    //
    private float getDurationInBeats(long durationInTicks)
    {
        return durationInTicks / (float) this.resolutionTicksPerBeat;
    }

    protected void fireHandledMidiEvent(MidiEvent event)
    {
        for (AuxilliaryMidiParser auxilliaryParser : auxilliaryParsers)
        {
            auxilliaryParser.parseHandledMidiEvent(event, this);
        }
    }

    protected void fireUnhandledMidiEvent(MidiEvent event)
    {
        for (AuxilliaryMidiParser auxilliaryParser : auxilliaryParsers)
        {
            auxilliaryParser.parseUnhandledMidiEvent(event, this);
        }
    }

    //
    // Event firing methods
    //
    public void fireBeforeParsingStarts()
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.beforeParsingStarts();
        }
    }

    public void fireAfterParsingFinished()
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.afterParsingFinished();
        }
    }

    private void fireChannelChanged(byte channel)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onChannelChanged(channel);
        }
    }

    private void fireInstrumentParsed(byte progChange, float positionInBeats)
    {
        for (MidiParserListener listener : parserListeners)
        {
            listener.onInstrumentParsed(progChange, positionInBeats);
        }
    }

    private void fireTempoChanged(int tempoBPM, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onTempoChanged(tempoBPM, positionInBeats);
        }
    }

    private void fireTimeSignatureParsed(byte numerator, byte powerOfTwo, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onTimeSignatureParsed(numerator, powerOfTwo, positionInBeats);
        }
    }

    private void firePitchWheelParsed(byte lsb, byte msb, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onPitchWheelParsed(lsb, msb, positionInBeats);
        }
    }

    private void fireChannelPressureParsed(byte pressure, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onChannelPressureParsed(pressure, positionInBeats);
        }
    }

    private void firePolyphonicPressureParsed(byte key, byte pressure, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onPolyphonicPressureParsed(key, pressure, positionInBeats);
        }
    }

    private void fireSystemExclusiveParsed(float positionInBeats, byte... bytes)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onSystemExclusiveParsed(positionInBeats, bytes);
        }
    }

    private void fireControllerEventParsed(byte controller, byte value, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onControllerEventParsed(controller, value, positionInBeats);
        }
    }

    private void fireLyricParsed(String lyric, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onLyricParsed(lyric, positionInBeats);
        }
    }

    private void fireSequenceNameParsed(String name, float positionInBeats)
    {
        for (MidiParserListener listener : parserListeners)
        {
            listener.onTrackNameParsed(name, positionInBeats);
        }
    }

    private void fireTextParsed(String text, float positionInBeats)
    {
        for (MidiParserListener listener : parserListeners)
        {
            listener.onTextParsed(text, positionInBeats);
        }
    }

    private void fireMetaEndParsed(float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onMetaEndEvent(positionInBeats);
        }
    }

    private void fireMarkerParsed(String marker, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onMarkerParsed(marker, positionInBeats);
        }
    }

    private void fireNoteParsed(Note note, float positionInBeats)
    {

        for (MidiParserListener listener : parserListeners)
        {
            listener.onNoteParsed(note, positionInBeats);
        }
    }

    // 
    // TempNote data structure
    //
    class TempNote
    {

        long startTick;
        byte noteOnVelocity;

        public TempNote(long startTick, byte noteOnVelocity)
        {
            this.startTick = startTick;
            this.noteOnVelocity = noteOnVelocity;
        }

        @Override
        public String toString()
        {
            return "TempNote[tick=" + startTick + ",v=" + noteOnVelocity + "]";
        }
    }
}
