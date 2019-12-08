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
package org.jjazz.rhythmmusicgeneration;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midi.MidiConst;

/**
 * Convenient methods for Midi tracks generation.
 */
public class Utilities
{

    protected static final Logger LOGGER = Logger.getLogger(org.jjazz.rhythmmusicgeneration.Utilities.class.getName());

    /**
     * Add chord root notes (or bass note for slash chords) to the specified track based.
     * <p>
     * Note remains on until next chord.
     *
     * @param track
     * @param channel
     * @param tickOffset The start of the first beat of first bar
     * @param cSeq       Add notes for this chord sequence
     * @param ts
     * @return The tick position corresponding to the end of the track or start of next section.
     */
    static public long addBassNoteEvents(Track track, int channel, long tickOffset, ChordSequence cSeq, TimeSignature ts)
    {
        if (track == null || !MidiConst.checkMidiChannel(channel) || tickOffset < 0 || cSeq == null || ts == null)
        {
            throw new IllegalArgumentException("track=" + track + " channel=" + channel + " tickOffset=" + tickOffset + " cSeq=" + cSeq + " ts=" + ts);
        }
        int startBar = cSeq.getStartBar();
        ShortMessage noteOn = null;
        ShortMessage noteOff;
        int lastNotePitch = 0;
        long tick = tickOffset;
        try
        {
            for (CLI_ChordSymbol cli : cSeq)
            {
                Note bassNote = cli.getData().getBassNote();
                int bassPitch = 3 * 12 + bassNote.getRelativePitch(); // stay on the 3rd octave
                LOGGER.fine("addBassNoteEvents() cli=" + cli + " bassNote=" + bassNote);
                int bar = cli.getPosition().getBar();
                float beat = cli.getPosition().getBeat();
                float beatCount = (bar - startBar) * ts.getNbNaturalBeats() + beat;
                tick = Math.round(beatCount * MidiConst.PPQ_RESOLUTION) + tickOffset;
                if (noteOn != null)
                {
                    // NOTE_OFF previous note just before next one starts
                    noteOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, lastNotePitch, 0);
                    MidiEvent event = new MidiEvent(noteOff, tick - 1);
                    track.add(event);
                }
                // NOTE_ON 
                noteOn = new ShortMessage(ShortMessage.NOTE_ON, channel, bassPitch, 100);
                MidiEvent event = new MidiEvent(noteOn, tick);
                track.add(event);
                lastNotePitch = bassPitch;
            }
            // Set the tick to start of next section
            tick = cSeq.getNbBars() * ts.getNbNaturalBeats() * MidiConst.PPQ_RESOLUTION + tickOffset;
            if (noteOn != null)
            {
                // Need to stop last note
                noteOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, lastNotePitch, 0);
                MidiEvent event = new MidiEvent(noteOff, tick - 1);
                track.add(event);
            }
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return tick;
    }

    /**
     * Add dummy drums notes to the specified track.
     *
     * @param track
     * @param channel
     * @param tickOffset Tick position of the first beat of first bar.
     * @param nbBars
     * @param ts
     * @return The tick position corresponding to the end of the track or start of next section.
     */
    static public long addDrumsNoteEvents(Track track, int channel, long tickOffset, int nbBars, TimeSignature ts)
    {
        if (track == null || !MidiConst.checkMidiChannel(channel) || tickOffset < 0 || nbBars < 0 || ts == null)
        {
            throw new IllegalArgumentException("track=" + track + " channel=" + channel + " tickOffset=" + tickOffset + " nbBars=" + nbBars + " ts=" + ts);
        }
        long tick = tickOffset;
        long dur = MidiConst.PPQ_RESOLUTION / 2;
        try
        {
            for (int bar = 0; bar < nbBars; bar++)
            {
                for (int beat = 0; beat < ts.getNbNaturalBeats(); beat++)
                {
                    int pitch = MidiConst.CLOSED_HI_HAT;
                    switch (beat)
                    {
                        case 0:
                            pitch = MidiConst.ACOUSTIC_BASS_DRUM;
                            break;
                        case 1:
                            pitch = MidiConst.ACOUSTIC_SNARE;
                            break;
                        default:
                        // Nothing
                    }
                    ShortMessage msgOn = new ShortMessage(ShortMessage.NOTE_ON, channel, pitch, Note.VELOCITY_STD);
                    ShortMessage msgOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
                    MidiEvent eventOn = new MidiEvent(msgOn, tick);
                    MidiEvent eventOff = new MidiEvent(msgOff, tick + dur);
                    track.add(eventOn);
                    track.add(eventOff);
                    tick += MidiConst.PPQ_RESOLUTION;
                }
            }
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return tick;
    }

}
