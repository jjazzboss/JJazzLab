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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midi.MidiConst;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Convenient class for Midi tracks generation.
 */
public class Utilities
{

    protected static final Logger LOGGER = Logger.getLogger(org.jjazz.rhythmmusicgeneration.Utilities.class.getName());

    /**
     * Get the length of specified song part in Midi ticks.
     *
     * @param spt
     * @return
     */
    static public long getTickLength(SongPart spt)
    {
        int nbNaturalBeatsPerBar = spt.getRhythm().getTimeSignature().getNbNaturalBeats();
        int nbNaturalBeats = spt.getNbBars() * nbNaturalBeatsPerBar;
        long l = nbNaturalBeats * MidiConst.PPQ_RESOLUTION;
        return l;
    }

    /**
     * Search a NOTE_ON MidiMessage event whose tick position is NOT between tickFrom and tickTo.
     *
     * @param t
     * @param tickFrom
     * @param tickTo
     * @return The first found MidiEvent or null.
     */
    static public MidiEvent findOutOfRangeNoteOn(Track t, long tickFrom, long tickTo)
    {
        if (tickFrom > tickTo || t == null)
        {
            throw new IllegalArgumentException("t=" + t + " tickFrom=" + tickFrom + " tickTo=" + tickTo);
        }
        for (int i = 0; i < t.size(); i++)
        {
            MidiEvent me = t.get(i);
            long tick = me.getTick();
            MidiMessage mm = me.getMessage();
            if ((mm instanceof ShortMessage) && ((ShortMessage) mm).getCommand() == ShortMessage.NOTE_ON)
            {
                if (tick < tickFrom || tick > tickTo)
                {
                    return me;
                }
            }
        }
        return null;
    }

    /**
     * Add chords root notes (or bass note for slash chords) to the specified track.
     *
     * @param track
     * @param channel
     * @param song
     * @param r Add notes for this rhythm.
     * @return The tick position right after the entire song.
     */
    static public long addBassNoteEvents(Track track, int channel, Song song, Rhythm r)
    {
        if (track == null || song == null)
        {
            throw new IllegalArgumentException("track=" + track + " song=" + song);
        }
        SongStructure sgs = song.getSongStructure();
        long tick = 0;
        // Scan all SongParts
        for (SongPart spt : sgs.getSongParts())
        {
            if (spt.getRhythm().equals(r))
            {
                CLI_Section section = spt.getParentSection();
                tick = addBassNoteEvents(track, channel, tick, section);
            } else
            {
                TimeSignature ts = spt.getRhythm().getTimeSignature();
                tick += spt.getNbBars() * MidiConst.PPQ_RESOLUTION * ts.getNbNaturalBeats();
            }
        }
        return tick;
    }

    /**
     * Add chord root notes (or bass note for slash chords) to the specified track based. Note remains on until next chord.
     *
     * @param track
     * @param channel
     * @param tickOffset
     * @param section Take chords from this section.
     * @return The tick position corresponding to the start of next section.
     */
    static public long addBassNoteEvents(Track track, int channel, long tickOffset, CLI_Section section)
    {
        if (track == null || !MidiConst.checkMidiChannel(channel) || tickOffset < 0 || section == null)
        {
            throw new IllegalArgumentException("track=" + track + " channel=" + channel + " tickOffset=" + tickOffset + " section=" + section);
        }
        TimeSignature ts = section.getData().getTimeSignature();
        int startBar = section.getPosition().getBar();
        List<? extends CLI_ChordSymbol> clis = section.getContainer().getItems(section, CLI_ChordSymbol.class);
        ShortMessage noteOn = null;
        ShortMessage noteOff;
        int lastNotePitch = 0;
        long tick = tickOffset;
        try
        {
            for (CLI_ChordSymbol cli : clis)
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
            tick = section.getContainer().getSectionSize(section) * ts.getNbNaturalBeats() * MidiConst.PPQ_RESOLUTION + tickOffset;
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
     * Add drums notes to the specified track.
     *
     * @param track
     * @param channel
     * @param song
     * @param r Add notes for this rhythm.
     * @return The tick position right after the entire song.
     */
    static public long addDrumsNoteEvents(Track track, int channel, Song song, Rhythm r)
    {
        if (track == null || song == null)
        {
            throw new IllegalArgumentException("track=" + track + " song=" + song);
        }
        SongStructure sgs = song.getSongStructure();
        long tick = 0;
        // Scan all SongParts
        for (SongPart spt : sgs.getSongParts())
        {
            TimeSignature ts = spt.getRhythm().getTimeSignature();
            if (spt.getRhythm().equals(r))
            {
                tick = addDrumsNoteEvents(track, channel, tick, spt.getNbBars(), ts);
            } else
            {
                tick += spt.getNbBars() * MidiConst.PPQ_RESOLUTION * ts.getNbNaturalBeats();
            }
        }
        return tick;
    }

    /**
     * Add drums to the specified track based.
     *
     * @param track
     * @param channel
     * @param tickOffset
     * @param nbBars
     * @param ts
     * @return The tick position corresponding to the start of next section.
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
