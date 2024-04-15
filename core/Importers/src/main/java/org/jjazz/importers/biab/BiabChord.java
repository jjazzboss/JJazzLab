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
package org.jjazz.importers.biab;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.Note;
import org.jjazz.song.api.SongCreationException;

/**
 * Store the data that makes a Band In a Box Chord symbol.
 */
public class BiabChord
{

    public enum Push
    {
        NO_PUSH, PUSH8, PUSH16
    };

    public enum Rest
    {
        NO_REST, SHOT, REST, HOLD
    }

    public enum Instrument
    {
        BASS, PIANO, DRUMS, GUITAR, STRINGS
    }
    private static final String[] CHORD_ROOTS = new String[]
    {
        "NotUsed", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C#", "D#", "F#", "G#", "A#"
    };

    private static final String[] BASS_FLAT = new String[]
    {
        "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb"
    };
    private static final String[] BASS_SHARP = new String[]
    {
        "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#"
    };
    private static String[] CHORD_TYPES;    // The JJazzLab chord strings corresponding to BIAB chord type index
    public int beatPos;
    public Note rootNote;
    public Note bassNote;
    public ChordType chordType;
    public Push push = Push.NO_PUSH;
    public Rest rest = Rest.NO_REST;
    public boolean excludedInstrumentShouldRest;
    public EnumSet<Instrument> excludedInstruments = EnumSet.noneOf(Instrument.class);

    private static final Logger LOGGER = Logger.getLogger(BiabChord.class.getSimpleName());

    public BiabChord(int beatPos)
    {
        this.beatPos = beatPos;
    }

    /**
     * Get the JJazzLab ChordSymbol equivalent.
     *
     * @return
     * @throws org.jjazz.song.api.SongCreationException
     */
    public ChordSymbol getChordSymbol() throws SongCreationException
    {
        if (rootNote == null || chordType == null)
        {
            throw new SongCreationException("Band-In-A-Box chord not complete");
        }
        ChordSymbol cs = new ChordSymbol(rootNote, bassNote, chordType);
        return cs;
    }

    /**
     * Set the chord base string : root name and optional bass.
     * <p>
     * Eg. "C" or "F#" or "Eb/Db"
     *
     * @param value
     */
    public void setChordBase(int value)
    {
        // CHORD_ROOTS: "NotUsed", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C#", "D#", "F#", "G#", "A#"
        // BASS_FLAT:   "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb"
        // BASS_SHARP:  "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#"
        try
        {
            String strRoot = CHORD_ROOTS[value % 18];
            rootNote = new Note(CHORD_ROOTS[value % 18]);
            if (value > 18)
            {
                // A bass note is defined
                int bassIndex;
                if (value % 18 > 12)
                {
                    // Special case for sharp root
                    bassIndex = (Arrays.asList(BASS_SHARP).indexOf(strRoot) + value / 18) % 12;
                } else
                {
                    // Do like BIAB: flat bass note used only if root also is flat, otherwise use sharp (A/Gb is impossible to get!)
                    bassIndex = (value / 18 + value % 18) % 12;
                }
                boolean useFlat = strRoot.contains("b") || strRoot.equals("C") || strRoot.equals("F");
                bassNote = new Note(useFlat ? BASS_FLAT[bassIndex] : BASS_SHARP[bassIndex]);
            } else
            {
                bassNote = rootNote;
            }
        } catch (ParseException ex)
        {
            // Should never happen
        }
    }

    /**
     * Set the chord type, e.g. "m7" or "dim".
     * <p>
     *
     * @param value
     */
    public void setChordType(int value)
    {
        initChordTypes();
        chordType = null;
        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();
        if (value > 0 && value < CHORD_TYPES.length && !CHORD_TYPES[value].equals("?"))
        {
            chordType = ctdb.getChordType(CHORD_TYPES[value]);
        }
        if (chordType == null)
        {
            chordType = ctdb.getChordType(0);
        }
        // LOGGER.finest("setChordType() value=" + value + " => chordType=" + chordType);
    }

    /**
     * Set the rest parameters: rest type,
     *
     * @param byteValue
     */
    public void setRest(int byteValue)
    {
        excludedInstrumentShouldRest = (byteValue & 0b10000000) != 0;
        rest = BiabChord.Rest.values()[((byteValue & 0b01100000) >> 5) + 1];
        List<Instrument> insts = new ArrayList<>();
        if ((byteValue & 0b00000001) == 0)
        {
            insts.add(Instrument.BASS);
        }
        if ((byteValue & 0b00000010) == 0)
        {
            insts.add(Instrument.DRUMS);
        }
        if ((byteValue & 0b00000100) == 0)
        {
            insts.add(Instrument.PIANO);
        }
        if ((byteValue & 0b00001000) == 0)
        {
            insts.add(Instrument.GUITAR);
        }
        if ((byteValue & 0b00010000) == 0)
        {
            insts.add(Instrument.STRINGS);
        }
        excludedInstruments = insts.isEmpty() ? EnumSet.noneOf(Instrument.class) : EnumSet.copyOf(insts);
    }

    /**
     * Set the push type.
     *
     * @param value
     */
    public void setPush(int value)
    {
        if (value < 1 || value > 2)
        {
            LOGGER.log(Level.SEVERE, "setPush() Invalid value={0}", value);
            return;
        }
        push = BiabChord.Push.values()[value];
    }

    @Override
    public String toString()
    {
        return "[pos=" + beatPos + ", root=" + rootNote + ", bass=" + bassNote + ", chordType=" + chordType + ", push=" + push + ", rest=" + rest + ", ex-ins=" + excludedInstruments
                + ", ex-ins-rest=" + excludedInstrumentShouldRest + "]";
    }

    private void initChordTypes()
    {
        if (CHORD_TYPES != null)
        {
            return;
        }
        CHORD_TYPES = new String[]
        {
            "Maj", // 0 index not used
            "Maj", // 1
            "Maj",
            "b5",
            "aug",
            "6",
            "Maj7",
            "Maj9",
            "Maj9#11",
            "Maj13#11",
            "Maj13", 
            "M9", 
            "+",
            "Maj7#5",
            "69",
            "2",
            "m",
            "maug",
            "mMaj7",
            "m7",
            "m9",   
            "m11", 
            "m13",
            "m6",
            "m#5",
            "m7#5",
            "m69",
            "M9#11",
            "M9#11",
            "M7b5",
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "m7b5",
            "dim",
            "m9b5",
            "dim5",
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "5",
            "add2", 
            "madd2",
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "7#5",
            "9#5",
            "7#5",
            "Maj", // Unknown actually
            "7#9",
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually            
            "7",
            "13",
            "7b13",
            "7#11",
            "13#11",
            "7#11",
            "9",
            "Maj", // Unknown actually
            "9#5",
            "9#11",
            "13#11",
            "9#11",
            "7b9",
            "13b9",
            "7b9",
            "7b9#11",
            "13b9#11",
            "7b9#11", 
            "7#9",
            "13#9",
            "7#9",
            "9#11",
            "13#11",
            "7#9#11",
            "7b5",
            "13b5",
            "7alt",
            "9b5", 
            "9b5",
            "7b5b9",
            "13b5b9",
            "7b5b9",
            "7b5#9",
            "13b5#9",   // 97
            "7b5#9",   // 98
            "7#5",    // 99
            "13",    // 100
            "7#5",    // 101
            "13#11",   // 102
            "9#5",    // 103
            "9#5",   // 104
            "7#5b9",  // 105
            "7#5b9", 
            "13b9#11", 
            "7#5#9", 
            "13#9", 
            "7#9#5",  // 110
            "13#9",
            "7alt",
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // 120 Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "7sus",  // 128
            "13sus", 
            "7sus",  // 130
            "7sus",
            "7sus",
            "7sus",
            "9sus",   // 134
            "Maj", // Unknown actually
            "9sus",
            "9sus",
            "13sus",
            "9sus",
            "7susb9",   // 140
            "13susb9",
            "13susb9",
            "7susb9",
            "13susb9",
            "13susb9",   // 145
            "7sus",
            "13sus",
            "7sus",
            "9sus",
            "13sus",   // 150
            "7sus",
            "7sus",
            "13sus",
            "7sus",
            "9sus",  // 155
            "9sus",
            "7susb9",
            "13susb9",
            "7susb9",
            "7sus",    // 160
            "13sus",
            "7sus",
            "7sus",
            "13sus",
            "7sus",   // 165
            "13sus",
            "9sus",
            "9sus",
            "7sus",
            "13susb9",   // 170
            "7susb9",
            "13susb9",
            "7sus",
            "13sus",
            "7sus",
            "13sus",
            "sus",    // 177
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // 180 Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "sus",
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj", // Unknown actually
            "Maj"        // Unknown actually            
        };

//        LOGGER.severe("CONTROL DEBUG REMOVE======");
//        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();
//        for (String s : CHORD_TYPES)
//        {
//            if (ctdb.getChordType(s) == null)
//            {
//                LOGGER.severe("CONTROL DEBUG invalid chordtype s=" + s);
//            }
//        }
//        LOGGER.severe("CONTROL DEBUG REMOVE END======");
    }


}
