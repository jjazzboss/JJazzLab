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
package org.jjazz.importers.jfugue;

//import org.jfugue.midi.MidiDefaults;
//import org.jfugue.pattern.Pattern;
//import org.jfugue.pattern.PatternProducer;
//import org.jfugue.provider.NoteProviderFactory;
//import org.staccato.DefaultNoteSettingsManager;
//import org.staccato.NoteSubparser;
public class Note
{

    private byte value;
    private double duration;
    private boolean wasOctaveExplicitlySet;
    private boolean wasDurationExplicitlySet;
    private byte onVelocity;
    private byte offVelocity;
    private boolean isRest;
    private boolean isStartOfTie;
    private boolean isEndOfTie;
    private boolean isFirstNote = true;
    private boolean isMelodicNote;
    private boolean isHarmonicNote;
    private boolean isPercussionNote;
    public String originalString;

    public Note()
    {
        this.onVelocity = 64;
        this.offVelocity = 0;
    }

//	public Note(String note) {
//		this(NoteProviderFactory.getNoteProvider().createNote(note));  
//	}
    public Note(Note note)
    {
        this.value = note.value;
        this.duration = note.duration;
        this.wasOctaveExplicitlySet = note.wasOctaveExplicitlySet;
        this.wasDurationExplicitlySet = note.wasDurationExplicitlySet;
        this.onVelocity = note.onVelocity;
        this.offVelocity = note.offVelocity;
        this.isRest = note.isRest;
        this.isStartOfTie = note.isStartOfTie;
        this.isEndOfTie = note.isEndOfTie;
        this.isFirstNote = note.isFirstNote;
        this.isMelodicNote = note.isMelodicNote;
        this.isHarmonicNote = note.isHarmonicNote;
        this.isPercussionNote = note.isPercussionNote;
        this.originalString = note.originalString;
    }

    public Note(int value)
    {
        this((byte) value);
    }

    public Note(byte value)
    {
        this();
        this.value = value;
        this.setOctaveExplicitlySet(false);
        useDefaultDuration();
    }

    public Note(int value, double duration)
    {
        this((byte) value, duration);
    }

    public Note(byte value, double duration)
    {
        this();
        this.value = value;
        setDuration(duration);
    }

    public Note setValue(byte value)
    {
        this.value = value;
        return this;
    }

    public byte getValue()
    {
        return isRest() ? 0 : this.value;
    }

    public Note changeValue(int delta)
    {
        this.setValue((byte) (getValue() + delta));
//		this.originalString = null;
        return this;
    }

    public Note setOctaveExplicitlySet(boolean set)
    {
        this.wasOctaveExplicitlySet = set;
        return this;
    }

    public byte getOctave()
    {
        return isRest() ? 0 : (byte) (this.getValue() / Note.OCTAVE);
    }

    public double getDuration()
    {
        return this.duration;
    }

    public Note setDuration(double d)
    {
        this.duration = d;
        this.wasDurationExplicitlySet = true;
        return this;
    }

    public Note useDefaultDuration()
    {
        this.duration = 0.25d;
        // And do not set wasDurationExplicitlySet
        return this;
    }

    public Note useSameDurationAs(Note note2)
    {
        this.duration = note2.duration;
        this.wasDurationExplicitlySet = note2.wasDurationExplicitlySet;
        return this;
    }

    public Note useSameExplicitOctaveSettingAs(Note note2)
    {
        this.wasOctaveExplicitlySet = note2.wasOctaveExplicitlySet;
        return this;
    }

//    public Note setDuration(String duration)
//    {
//        return setDuration(NoteProviderFactory.getNoteProvider().getDurationForString(duration));
//    }
    public boolean isDurationExplicitlySet()
    {
        return this.wasDurationExplicitlySet;
    }

    public boolean isOctaveExplicitlySet()
    {
        return this.wasOctaveExplicitlySet;
    }

    /**
     * FOR TESTING PURPOSES ONLY - avoids setting "isDurationExplicitlySet" - Please use setDuration instead!
     */
    public Note setImplicitDurationForTestingOnly(double d)
    {
        this.duration = d;
        // And do not set wasDurationExplicitlySet
        return this;
    }

    public Note setRest(boolean rest)
    {
        this.isRest = rest;
        return this;
    }

    public boolean isRest()
    {
        return this.isRest;
    }

    public Note setPercussionNote(boolean perc)
    {
        this.isPercussionNote = perc;
        return this;
    }

    public boolean isPercussionNote()
    {
        return this.isPercussionNote;
    }

    public Note setOnVelocity(byte velocity)
    {
        this.onVelocity = velocity;
        return this;
    }

    public byte getOnVelocity()
    {
        return this.onVelocity;
    }

    public Note setOffVelocity(byte velocity)
    {
        this.offVelocity = velocity;
        return this;
    }

    public byte getOffVelocity()
    {
        return this.offVelocity;
    }

    public Note setStartOfTie(boolean isStartOfTie)
    {
        this.isStartOfTie = isStartOfTie;
        return this;
    }

    public Note setEndOfTie(boolean isEndOfTie)
    {
        this.isEndOfTie = isEndOfTie;
        return this;
    }

    public boolean isStartOfTie()
    {
        return isStartOfTie;
    }

    public boolean isEndOfTie()
    {
        return isEndOfTie;
    }

    public Note setFirstNote(boolean isFirstNote)
    {
        this.isFirstNote = isFirstNote;
        return this;
    }

    public boolean isFirstNote()
    {
        return this.isFirstNote;
    }

    public Note setMelodicNote(boolean isMelodicNote)
    {
        this.isMelodicNote = isMelodicNote;
        return this;
    }

    public boolean isMelodicNote()
    {
        return this.isMelodicNote;
    }

    public Note setHarmonicNote(boolean isHarmonicNote)
    {
        this.isHarmonicNote = isHarmonicNote;
        return this;
    }

    public boolean isHarmonicNote()
    {
        return this.isHarmonicNote;
    }

    public Note setOriginalString(String originalString)
    {
        this.originalString = originalString;
        return this;
    }

    public String getOriginalString()
    {
        return this.originalString;
    }

    public double getMicrosecondDuration(double mpq)
    {
        return (this.duration * 4.0f) * mpq;
    }

    public byte getPositionInOctave()
    {
        return isRest() ? 0 : (byte) (getValue() % Note.OCTAVE);
    }

    public static boolean isSameNote(String note1, String note2)
    {
        if (note1.equalsIgnoreCase(note2))
        {
            return true;
        }
        for (int i = 0; i < NOTE_NAMES_COMMON.length; i++)
        {
            if (note1.equalsIgnoreCase(NOTE_NAMES_FLAT[i]) && note2.equalsIgnoreCase(NOTE_NAMES_SHARP[i]))
            {
                return true;
            }
            if (note1.equalsIgnoreCase(NOTE_NAMES_SHARP[i]) && note2.equalsIgnoreCase(NOTE_NAMES_FLAT[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * This is just Bubble Sort, but allows you to pass a Note.SortingCallback that returns a value that you want to sort for a note. For
     * example, to sort based on position in octave, your SortingCallback would return note.getPositionInOctave(). This lets you sort by
     * note value, octave, position in octave, duration, velocity, and so on.
     */
    public static void sortNotesBy(Note[] notes, Note.SortingCallback callback)
    {
        Note temp;
        for (int i = 0; i < notes.length - 1; i++)
        {
            for (int j = 1; j < notes.length - i; j++)
            {
                if (callback.getSortingValue(notes[j - 1]) > callback.getSortingValue(notes[j]))
                {
                    temp = notes[j - 1];
                    notes[j - 1] = notes[j];
                    notes[j] = temp;
                }
            }
        }
    }

    public static Note createRest(double duration)
    {
        return new Note().setRest(true).setDuration(duration);
    }

    /**
     * Returns a MusicString representation of the given MIDI note value, which indicates a note and an octave.
     *
     * @param noteValue this MIDI note value, like 60
     * @return a MusicString value, like C5
     */
    public static String getToneString(byte noteValue)
    {
        StringBuilder buddy = new StringBuilder();
        buddy.append(getToneStringWithoutOctave(noteValue));
        buddy.append(noteValue / Note.OCTAVE);
        return buddy.toString();
    }

    /**
     * Returns a MusicString representation of the given MIDI note value, but just the note - not the octave. This means that the value
     * returned can not be used to accurately recalculate the noteValue, since information will be missing. But this is useful for knowing
     * what note within any octave the corresponding value belongs to.
     *
     * @param noteValue this MIDI note value, like 60
     * @return a MusicString value, like C
     */
    public static String getToneStringWithoutOctave(byte noteValue)
    {
        return NOTE_NAMES_COMMON[noteValue % Note.OCTAVE];
    }

    /**
     * Returns a MusicString representation of the given MIDI note value, just the note (not the octave), disposed to use either flats or
     * sharps. Pass -1 to get a flat name and +1 to get a sharp name for any notes that are accidentals.
     *
     * @param dispose   -1 to get a flat value, +1 to get a sharp value
     * @param noteValue this MIDI note value, like 61
     * @return a MusicString value, like Db if -1 or C# if +1
     */
    public static String getDispositionedToneStringWithoutOctave(int dispose, byte noteValue)
    {
        if (dispose == -1)
        {
            return NOTE_NAMES_FLAT[noteValue % Note.OCTAVE];
        } else
        {
            return NOTE_NAMES_SHARP[noteValue % Note.OCTAVE];
        }
    }

    /**
     * Returns a MusicString representation of the given MIDI note value using the name of a percussion instrument.
     *
     * @param noteValue this MIDI note value, like 60
     * @return a MusicString value, like [AGOGO]
     */
    public static String getPercussionString(byte noteValue)
    {
        StringBuilder buddy = new StringBuilder();
        buddy.append("[");
        buddy.append(PERCUSSION_NAMES[noteValue - 35]);
        buddy.append("]");
        return buddy.toString();
    }

    /**
     * Returns the frequency, in Hertz, for the given note. For example, the frequency for A5 (MIDI note 69) is 440.0
     *
     * @param noteValue the MIDI note value
     * @return frequency in Hertz
     */
//    public static double getFrequencyForNote(String note)
//    {
//        return (note.toUpperCase().startsWith("R")) ? 0.0d : getFrequencyForNote(NoteProviderFactory.getNoteProvider().createNote(note).getValue());
//    }
    /**
     * Returns the frequency, in Hertz, for the given note value. For example, the frequency for A5 (MIDI note 69) is 440.0
     *
     * @param noteValue the MIDI note value
     * @return frequency in Hertz
     */
    public static double getFrequencyForNote(int noteValue)
    {
        return truncateTo3DecimalPlaces(getPreciseFrequencyForNote(noteValue));
    }

    private static double truncateTo3DecimalPlaces(double preciseNumber)
    {
        return Math.rint(preciseNumber * 10000.0) / 10000.0;
    }

    private static double getPreciseFrequencyForNote(int noteValue)
    {
        return getFrequencyAboveBase(8.1757989156, noteValue / 12.0);
    }

    private static double getFrequencyAboveBase(double baseFrequency, double octavesAboveBase)
    {
        return baseFrequency * Math.pow(2.0, octavesAboveBase);
    }

//    public static boolean isValidNote(String candidateNote)
//    {
//        return NoteSubparser.getInstance().matches(candidateNote);
//    }
    public static boolean isValidQualifier(String candidateQualifier)
    {
        return true; // TODO: Implement Note.isValidQualifier when necessary
    }

    /**
     * Returns a MusicString representation of a decimal duration. This code currently only converts single duration values representing
     * whole, half, quarter, eighth, etc. durations; and dotted durations associated with those durations (such as "h.", equal to 0.75).
     * This method does not convert combined durations (for example, "hi" for 0.625). For these values, the original decimal duration is
     * returned in a string, prepended with a "/" to make the returned value a valid MusicString duration indicator. It does handle
     * durations greater than 1.0 (for example, "wwww" for 4.0).
     *
     * @param decimalDuration The decimal value of the duration to convert
     * @return a MusicString fragment representing the duration
     */
    public static String getDurationString(double decimalDuration)
    {
        double originalDecimalDuration = decimalDuration;
        StringBuilder buddy = new StringBuilder();
        if (decimalDuration >= 1.0)
        {
            int numWholeDurations = (int) Math.floor(decimalDuration);
            buddy.append("w");
            if (numWholeDurations > 1)
            {
                buddy.append(numWholeDurations);
            }
            decimalDuration -= numWholeDurations;
        }
        if (decimalDuration == 0.75)
        {
            buddy.append("h.");
        } else if (decimalDuration == 0.5)
        {
            buddy.append("h");
        } else if (decimalDuration == 0.375)
        {
            buddy.append("q.");
        } else if (decimalDuration == 0.25)
        {
            buddy.append("q");
        } else if (decimalDuration == 0.1875)
        {
            buddy.append("i.");
        } else if (decimalDuration == 0.125)
        {
            buddy.append("i");
        } else if (decimalDuration == 0.09375)
        {
            buddy.append("s.");
        } else if (decimalDuration == 0.0625)
        {
            buddy.append("s");
        } else if (decimalDuration == 0.046875)
        {
            buddy.append("t.");
        } else if (decimalDuration == 0.03125)
        {
            buddy.append("t");
        } else if (decimalDuration == 0.0234375)
        {
            buddy.append("x.");
        } else if (decimalDuration == 0.015625)
        {
            buddy.append("x");
        } else if (decimalDuration == 0.01171875)
        {
            buddy.append("o.");
        } else if (decimalDuration == 0.0078125)
        {
            buddy.append("o");
        } else if (decimalDuration == 0.0)
        {
        } else
        {
            return "/" + originalDecimalDuration;
        }
        return buddy.toString();
    }

    public static String getDurationStringForBeat(int beat)
    {
        switch (beat)
        {
            case 2:
                return "h";
            case 4:
                return "q";
            case 8:
                return "i";
            case 16:
                return "s";
            default:
                return "/" + (1.0 / (double) beat);
        }
    }

//    public String getVelocityString()
//    {
//        StringBuilder buddy = new StringBuilder();
//        if (this.onVelocity != DefaultNoteSettingsManager.getInstance().getDefaultOnVelocity())
//        {
//            buddy.append("a" + getOnVelocity());
//        }
//        if (this.offVelocity != DefaultNoteSettingsManager.getInstance().getDefaultOffVelocity())
//        {
//            buddy.append("d" + getOffVelocity());
//        }
//        return buddy.toString();
//    }
//    /**
//     * Returns a pattern representing this note. Does not return indicators of whether the note is harmonic or melodic.
//     */
//    @Override
//    public Pattern getPattern()
//    {
//        StringBuilder buddy = new StringBuilder();
//        buddy.append(toStringWithoutDuration());
//        buddy.append(getDecoratorString());
//        return new Pattern(buddy.toString());
//    }
//    public Pattern getPercussionPattern()
//    {
//        if (getValue() < MidiDefaults.MIN_PERCUSSION_NOTE || getValue() > MidiDefaults.MAX_PERCUSSION_NOTE)
//        {
//            return getPattern();
//        }
//        StringBuilder buddy = new StringBuilder();
//        buddy.append(Note.getPercussionString(getValue()));
//        buddy.append(getDecoratorString());
//        return new Pattern(buddy.toString());
//    }
    @Override
    public String toString()
    {
//        return getPattern().toString();
        return "Note(" + this.value + ")";
    }

    public String toStringWithoutDuration()
    {
        if (isRest())
        {
            return "R";
        } else if (isPercussionNote())
        {
            return Note.getPercussionString(this.getValue());
        } else
        {
            return (originalString != null) ? this.originalString : Note.getToneString(this.getValue());
        }
    }

    public String getToneString()
    {
        if (isRest)
        {
            return "R";
        }

        StringBuilder buddy = new StringBuilder();
        buddy.append(Note.getToneStringWithoutOctave(getValue()));
        if (this.wasOctaveExplicitlySet)
        {
            buddy.append(getOctave());
        }
        return buddy.toString();
    }

    /**
     * Returns the "decorators" to the base note, which includes the duration if one is explicitly specified, and velocity dynamics if
     * provided
     */
//    public String getDecoratorString()
//    {
//        StringBuilder buddy = new StringBuilder();
//        if (isDurationExplicitlySet())
//        {
//            buddy.append(Note.getDurationString(this.duration));
//        }
//        buddy.append(getVelocityString());
//        return buddy.toString();
//    }
    public boolean equals(Object o)
    {
        if (!(o instanceof Note))
        {
            return false;
        }

        Note n2 = (Note) o;
        boolean originalStringsMatchSufficientlyWell = ((n2.originalString == null) || (this.originalString == null)) ? true : n2.originalString.equalsIgnoreCase(this.originalString);
        return ((n2.value == this.value)
                && (n2.duration == this.duration)
                && (n2.wasOctaveExplicitlySet == this.wasOctaveExplicitlySet)
                && (n2.wasDurationExplicitlySet == this.wasDurationExplicitlySet)
                && (n2.isEndOfTie == this.isEndOfTie)
                && (n2.isStartOfTie == this.isStartOfTie)
                && (n2.isMelodicNote == this.isMelodicNote)
                && (n2.isHarmonicNote == this.isHarmonicNote)
                && (n2.isPercussionNote == this.isPercussionNote)
                && (n2.isFirstNote == this.isFirstNote)
                && (n2.isRest == this.isRest)
                && (n2.onVelocity == this.onVelocity)
                && (n2.offVelocity == this.offVelocity)
                && originalStringsMatchSufficientlyWell);
    }

    public String toDebugString()
    {
        StringBuilder buddy = new StringBuilder();
        buddy.append("Note:");
        buddy.append(" value=").append(this.getValue());
        buddy.append(" duration=").append(this.getDuration());
        buddy.append(" wasOctaveExplicitlySet=").append(this.isOctaveExplicitlySet());
        buddy.append(" wasDurationExplicitlySet=").append(this.isDurationExplicitlySet());
        buddy.append(" isEndOfTie=").append(this.isEndOfTie());
        buddy.append(" isStartOfTie=").append(this.isStartOfTie());
        buddy.append(" isMelodicNote=").append(this.isMelodicNote());
        buddy.append(" isHarmonicNote=").append(this.isHarmonicNote());
        buddy.append(" isPercussionNote=").append(this.isPercussionNote());
        buddy.append(" isFirstNote=").append(this.isFirstNote());
        buddy.append(" isRest=").append(this.isRest());
        buddy.append(" onVelocity=").append(this.getOnVelocity());
        buddy.append(" offVelocity=").append(this.getOffVelocity());
        buddy.append(" originalString=").append(this.getOriginalString());
        return buddy.toString();
    }

    public final static String[] NOTE_NAMES_COMMON = new String[]
    {
        "C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"
    };
    public final static String[] NOTE_NAMES_SHARP = new String[]
    {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };
    public final static String[] NOTE_NAMES_FLAT = new String[]
    {
        "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
    };

    public final static String[] PERCUSSION_NAMES = new String[]
    {
        // Percussion Name		// MIDI Note Value
        "ACOUSTIC_BASS_DRUM", //       35
        "BASS_DRUM", //       36
        "SIDE_STICK", //       37
        "ACOUSTIC_SNARE", //       38
        "HAND_CLAP", //       39
        "ELECTRIC_SNARE", //       40
        "LO_FLOOR_TOM", //       41
        "CLOSED_HI_HAT", //       42
        "HIGH_FLOOR_TOM", //       43
        "PEDAL_HI_HAT", //       44
        "LO_TOM", //       45
        "OPEN_HI_HAT", //       46
        "LO_MID_TOM", //       47
        "HI_MID_TOM", //       48
        "CRASH_CYMBAL_1", //       49
        "HI_TOM", //       50
        "RIDE_CYMBAL_1", //       51
        "CHINESE_CYMBAL", //       52
        "RIDE_BELL", //       53
        "TAMBOURINE", //       54
        "SPLASH_CYMBAL", //       55
        "COWBELL", //       56
        "CRASH_CYMBAL_2", //       57
        "VIBRASLAP", //       58
        "RIDE_CYMBAL_2", //       59
        "HI_BONGO", //       60
        "LO_BONGO", //       61
        "MUTE_HI_CONGA", //       62
        "OPEN_HI_CONGA", //       63
        "LO_CONGA", //       64
        "HI_TIMBALE", //       65
        "LO_TIMBALE", //       66
        "HI_AGOGO", //       67
        "LO_AGOGO", //       68
        "CABASA", //       69
        "MARACAS", //       70
        "SHORT_WHISTLE", //       71
        "LONG_WHISTLE", //       72
        "SHORT_GUIRO", //       73
        "LONG_GUIRO", //       74
        "CLAVES", //       75
        "HI_WOOD_BLOCK", //       76
        "LO_WOOD_BLOCK", //       77
        "MUTE_CUICA", //       78
        "OPEN_CUICA", //       79
        "MUTE_TRIANGLE", //       80
        "OPEN_TRIANGLE"			//       81
    };

    public static final Note REST = new Note(0).setRest(true);
    public static final byte OCTAVE = 12;
    public static final byte MIN_OCTAVE = 0;
    public static final byte MAX_OCTAVE = 10;

    /**
     * For use with Note.sortNotesBy()
     */
    interface SortingCallback
    {

        /**
         * Must return an int. If you want to sort by duration (which is decimal), you'll need to work around this.
         */
        public int getSortingValue(Note note);
    }
}
