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
package org.jjazz.harmony;

import java.text.ParseException;
import java.util.logging.Logger;
import static org.jjazz.harmony.Bundle.ERR_EmptyString;
import static org.jjazz.harmony.Bundle.ERR_InvalidNote;
import org.openide.util.NbBundle.Messages;

/**
 * A note has a pitch, a duration (symbolic and in beats), and a velocity.
 * <p>
 * This is an immutable class.
 */
@Messages(
        {
            "ERR_InvalidNote=Invalid note",
            "ERR_EmptyString=Empty string"
        })
public class Note implements Comparable<Note>, Cloneable
{

    public static final int VELOCITY_MIN = 0;
    public static final int VELOCITY_STD = 100;
    public static final int VELOCITY_MAX = 127;
    public static final int PITCH_MIN = 0;
    public static final int PITCH_STD = 60;
    public static final int PITCH_MAX = 127;
    public static final int OCTAVE_MIN = 0;
    public static final int OCTAVE_STD = 4;
    public static final int OCTAVE_MAX = 10;
    private static final Logger LOGGER = Logger.getLogger(Note.class.getSimpleName());

    public enum Alteration
    {
        FLAT, SHARP;
    }
    public static final String[] notesFlat =
    {
        "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
    };
    /**
     * Available for general use.
     */
    public static final String[] notesSharp =
    {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };
    /**
     * The pitch of the note (0-127).
     */
    private int pitch;
    /**
     * The symbolic duration of the note.
     */
    private SymbolicDuration symbolicDuration;
    /**
     * The duration in beats of the note.
     */
    private float beatDuration;
    /**
     * Sharp or flat
     */
    private Alteration alterationDisplay;
    private int velocity;

    /**
     * Use MidiConst.PITCH_STD, Quarter duration, Alteration.Flat and standard velocity.
     */
    public Note()
    {
        this(PITCH_STD);
    }

    /**
     * Create a note with a default beatDuration=1 and standard velocity. Use FLAT symbols if relevant
     *
     * @param p The pitch of the note.
     */
    public Note(int p)
    {
        this(p, SymbolicDuration.QUARTER.getBeatDuration());
    }

    /**
     * Create a Note with a pitch and a beat duration and standard velocity. Use FLAT symbol by default.
     *
     * @param p  The pitch of the note.
     * @param bd The beat duration of the note.
     */
    public Note(int p, float bd)
    {
        this(p, bd, VELOCITY_STD);
    }

    /**
     * Create a Note with a pitch, a duration in beat, standard velocity and an alteration if any.
     *
     * @param p
     * @param bd
     * @param v
     */
    public Note(int p, float bd, int v)
    {
        this(p, bd, v, Alteration.FLAT);
    }

    /**
     * Create a Note with a pitch, a duration in beat, a velocity and an alteration if any.
     *
     * @param p
     * @param bd  Must be &gt; 0
     * @param v   velocity
     * @param alt
     */
    public Note(int p, float bd, int v, Alteration alt)
    {
        if (!checkPitch(p) || bd <= 0 || alt == null || !checkVelocity(v))
        {
            throw new IllegalArgumentException("p=" + p + " bd=" + bd + " alt=" + alt + " v=" + v);
        }
        pitch = p;
        beatDuration = bd;
        symbolicDuration = SymbolicDuration.getSymbolicDuration(beatDuration);;
        alterationDisplay = alt;
        velocity = v;
    }

    /**
     * A new note based on n but with pitch=newPitch
     *
     * @param n
     * @param newPitch
     */
    public Note(Note n, int newPitch)
    {
        this(newPitch, n.beatDuration, n.velocity, n.alterationDisplay);
    }

    /**
     * Build a note from a string specification, see examples below.
     * <p>
     * Ex: "G", "Cb", "A#m6", "E7" will generate notes G, B, A#, E with Const.OCTAVE_STD for the octave id.<br>
     * Ex: "C!3", "Db!6", "A#dim!2" will generate notes C octave 3, Db octave 6, A# octave 2<br>
     *
     * @param s A string
     *
     * @throws ParseException If syntax error in string specification.
     *
     */
    public Note(String s) throws ParseException
    {
        if (s == null)
        {
            throw new NullPointerException("s");
        }
        String str = s.trim();
        Alteration alt = Alteration.FLAT;         // By default

        if (str.length() == 0)
        {
            throw new ParseException(ERR_EmptyString(), 0);
        }

        // Special case 
        if (str.equals("Cb"))
        {

        }

        // Get the degree string, eg "A", "G", "Eb", "F#" etc.
        String degreeStr = str.substring(0, 1);
        if (str.length() > 1 && (str.charAt(1) == 'b' || str.charAt(1) == '#'))
        {
            degreeStr = str.substring(0, 2);
        }

        // Get the octave number string "!3" => "3"
        String octaveStr = null;
        int octaveIndex = str.indexOf("!");
        if (octaveIndex == str.length() - 1)
        {
            // "!" on last position, missing octave number !
            throw new ParseException(ERR_InvalidNote() + "\"" + str + "\"", str.length() - 1);
        }
        if (octaveIndex != -1)
        {
            // There is an octave specified
            octaveStr = str.substring(octaveIndex + 1);
        }

        // Get the pitch and alteration
        int relPitch = -1;
        for (int i = 0; i < notesFlat.length; i++)
        {
            if (degreeStr.compareTo("Cb") == 0)
            {
                relPitch = 11;
                alt = Alteration.FLAT;
                break;
            }
            if (degreeStr.compareToIgnoreCase("B#") == 0)
            {
                relPitch = 0;
                alt = Alteration.SHARP;
                break;
            }
            if (degreeStr.compareToIgnoreCase("E#") == 0)
            {
                relPitch = 5;
                alt = Alteration.SHARP;
                break;
            }
            if (degreeStr.compareToIgnoreCase("Fb") == 0)
            {
                relPitch = 4;
                alt = Alteration.FLAT;
                break;
            }
            if (degreeStr.compareToIgnoreCase(notesFlat[i]) == 0)
            {
                relPitch = i;
                alt = Alteration.FLAT;
                break;
            }
            if (degreeStr.compareToIgnoreCase(notesSharp[i]) == 0)
            {
                relPitch = i;
                alt = Alteration.SHARP;
                break;
            }
        }

        if (relPitch == -1)
        {
            throw new ParseException(ERR_InvalidNote() + "\"" + str + "\"", 0);
        }

        // Get the octave
        int o = OCTAVE_STD;
        if (octaveStr != null)
        {
            // If octave specified, decode it
            try
            {
                o = Integer.parseInt(octaveStr);
            } catch (NumberFormatException e)
            {
                throw new ParseException(ERR_InvalidNote() + "\"" + str + "\" : " + e.getLocalizedMessage(), 0);
            }
        }
        if (!checkOctave(o))
        {
            throw new ParseException(ERR_InvalidNote() + "\"" + str + "\"", 0);
        }

        // Build the note
        pitch = o * 12 + relPitch;
        beatDuration = SymbolicDuration.QUARTER.getBeatDuration();
        symbolicDuration = SymbolicDuration.QUARTER;
        alterationDisplay = alt;
        velocity = VELOCITY_STD;
    }

    @Override
    public Note clone()
    {
        return new Note(this.pitch, this.beatDuration, this.velocity, this.alterationDisplay);
    }

    public final int getPitch()
    {
        return pitch;
    }

    /**
     * @return An integer between 0 and 11.
     */
    public final int getRelativePitch()
    {
        return pitch % 12;
    }

    public final float getDurationInBeats()
    {
        return beatDuration;
    }

    public final SymbolicDuration getSymbolicDuration()
    {
        return symbolicDuration;
    }

    /**
     * The octave starting at 0 for pitch=0 (which is different from the Midi convention, where pitch=0 is octave=-1).
     * @return 
     */
    public final int getOctave()
    {
        return pitch / 12;
    }

    public final Alteration getAlterationDisplay()
    {
        return alterationDisplay;
    }

    /**
     * @param n A Note.
     *
     * @return The relative ascendant interval in semitons towards n.
     */
    public int getRelativeAscInterval(Note n)
    {
        int delta = n.getRelativePitch() - getRelativePitch();

        if (delta < 0)
        {
            delta += 12;
        }

        return delta;
    }

    /**
     * @param n A Note.
     *
     * @return The ascendant interval in semitons towards n.
     */
    public int getRelativeDescInterval(Note n)
    {
        int delta = getRelativePitch() - n.getRelativePitch();

        if (delta < 0)
        {
            delta += 12;
        }

        return delta;
    }

    /**
     * Calculate the shortest pitch delta from this note's relative pitch to relPitch.
     * <p>
     * Ex: if this=B3 and relPitch=0(C) then return 1.<br>
     * Ex: if this=E2 and relPitch=1(Db) then return -3.
     *
     * @param relPitch
     * @return A value between -5 and +6.
     */
    public int getRelativePitchDelta(int relPitch)
    {
        if (relPitch > 11 || relPitch < 0)
        {
            throw new IllegalArgumentException("relPitch=" + relPitch);
        }
        int pitchDelta = relPitch - getRelativePitch();
        if (pitchDelta > 6)
        {
            pitchDelta -= 12;
        } else if (pitchDelta < -5)
        {
            pitchDelta += 12;
        }
        return pitchDelta;
    }

    /**
     * Transpose the note from t semi-tons.
     *
     * @param t Transposition value in positive/negative semi-tons.
     * @return A new note transposed.
     */
    public Note getTransposed(int t)
    {
        return new Note(this, pitch + t);
    }

    /**
     * Change the octave of this note so that pitch is within the pitch limits (included).
     *
     * @param lowPitch  Must be &lt; (highPitch-12)
     * @param highPitch
     * @return The new note with correct ed pitch.
     */
    public Note getCentered(int lowPitch, int highPitch)
    {
        if (lowPitch > highPitch - 12)
        {
            throw new IllegalArgumentException("lowPitch=" + lowPitch + " highPïtch=" + highPitch);
        }
        int newPitch = pitch;
        while (pitch < lowPitch)
        {
            newPitch += 12;
        }
        while (pitch > highPitch)
        {
            newPitch -= 12;
        }
        Note n = new Note(this, newPitch);
        return n;
    }

    /**
     * Get a new transposed note.
     * <p>
     * If the new note is beyond pitchLimit, the note's octave is changed to remain below (pitchShift &gt; 0) or above (pitchSshift &lt; 0)
     * pitchLimit.
     * <p>
     * @param pitchShift A negative or positive value i semi-tons.
     * @param pitchLimit Authorized values are [13, 119]
     * @return The new transposed Note
     */
    public Note getTransposed(int pitchShift, int pitchLimit)
    {
        if (pitchLimit < 13 || pitchLimit > 119)
        {
            throw new IllegalArgumentException("t=" + pitchShift + " pitchLimit=" + pitchLimit);
        }
        int newPitch = this.pitch + pitchShift;
        if (pitchShift > 0)
        {
            while (newPitch > pitchLimit)
            {
                newPitch -= 12;
            }
        } else if (pitchShift < 0)
        {
            while (newPitch < pitchLimit)
            {
                newPitch += 12;
            }
        }
        Note n = new Note(this, newPitch);
        return n;
    }

    /**
     * Get a transposed note which is guaranteed to be in the same C-based octave.
     *
     * @param t Transposition value in positive/negative semi-tons.
     * @return A new note transposed from t semi-tons but within the same octave.
     */
    public Note getTransposedWithinOctave(int t)
    {
        int rp = getRelativePitch() + t;
        if ((t != 0) && ((t % 12) != 0))
        {
            rp = (rp < 0) ? (rp + 12) : rp;
            rp = (rp > 11) ? (rp - 12) : rp;
            rp = (getOctave() * 12) + rp;
        }
        return new Note(this, rp);
    }

    /**
     * Return the absolute pitch corresponding to relPitch below this note (or possibly equals if acceptEquals is true).
     * <p>
     * If this note's relative pitch is less than relativePitch, return this note's absolute pitch-12.<br>
     * Ex: this=G3. If relativePitch=F return value=F3, if relativePitch=A return value=A2<p>
     * If resulting pitch &lt; 0, return resulting pitch+12
     *
     * @param relPitch
     * @param acceptEquals
     * @return An absolute note pitch
     * @throws IllegalArgumentException If there is no lower pitch possible.
     */
    public int getLowerPitch(int relPitch, boolean acceptEquals)
    {
        if (relPitch < 0 || relPitch > 11)
        {
            throw new IllegalArgumentException("this=" + this + " relPitch=" + relPitch);
        }
        int p = getOctave() * 12 + relPitch;
        if ((relPitch == getRelativePitch() && !acceptEquals) || relPitch > getRelativePitch())
        {
            p = (getOctave() - 1) * 12 + relPitch;
        }
        if (p < 0)
        {
            p += 12;
        }
        return p;
    }

    /**
     * Return the absolute pitch corresponding to relPitch above this note (or possibly equals if acceptEquals is true).
     * <p>
     * Ex: this=G3. If relativePitch=F return value=F4, if relativePitch=A return value=A3
     * <p>
     * If resulting pitch &gt;127, return resulting pitch-12.
     *
     * @param relPitch
     * @param acceptEquals
     * @return
     * @throws IllegalArgumentException If there is no lower pitch possible.
     */
    public int getUpperPitch(int relPitch, boolean acceptEquals)
    {
        if (relPitch < 0 || relPitch > 11)
        {
            throw new IllegalArgumentException("this=" + this + " relPitch=" + relPitch);
        }
        int p = getOctave() * 12 + relPitch;
        if ((relPitch == getRelativePitch() && !acceptEquals) || relPitch < getRelativePitch())
        {
            p = (getOctave() + 1) * 12 + relPitch;
        }
        if (p > 127)
        {
            p -= 12;
        }
        return p;
    }

    /**
     * Return the absolute pitch of the closest note from this note.
     * <p>
     * Ex: if this=Ab3 and relPitch=0(C) then return C4.
     *
     * @param relPitch The relative pitch of the closest note.
     * @return
     */
    public int getClosestPitch(int relPitch)
    {
        int up = getUpperPitch(relPitch, true);
        int low = getLowerPitch(relPitch, true);
        if (up - getPitch() > getPitch() - low)
        {
            return low;
        } else
        {
            return up;
        }
    }

    public int getVelocity()
    {
        return velocity;
    }

    /**
     * Compare the relative pitch of 2 notes.
     *
     * @param n Note The note to compare with this note.
     * @return boolean Return true if Note n has the same relative pitch than this note.
     */
    public boolean equalsRelativePitch(Note n)
    {
        return getRelativePitch() == n.getRelativePitch();
    }

    /**
     * Compare 2 objects.
     * <p>
     * Accept Note object and String objects.
     *
     * @return True if 2 notes have same pitch, beatDuration and velocity. AlterationDisplay is ignored.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Note other = (Note) obj;
        if (this.pitch != other.pitch)
        {
            return false;
        }
        if (Float.floatToIntBits(this.beatDuration) != Float.floatToIntBits(other.beatDuration))
        {
            return false;
        }
        if (this.velocity != other.velocity)
        {
            return false;
        }
        return true;
    }

    /**
     * Use pitch, beatDuration and velocity.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + this.pitch;
        hash = 67 * hash + Float.floatToIntBits(this.beatDuration);
        hash = 67 * hash + this.velocity;
        return hash;
    }

    /**
     * @param o
     * @return True if 2 notes have the same pitch.
     */
    @Override
    public int compareTo(Note o)
    {
        if (!(o instanceof Note))
        {
            throw new ClassCastException("Invalid class to be compared to a Note : " + o);
        }

        int opitch = o.pitch;

        if (opitch > pitch)
        {
            return -1;
        } else if (opitch < pitch)
        {
            return 1;
        } else
        {
            return 0;
        }
    }

    /**
     * Same as toAbsoluteNoteString().
     *
     * @return
     */
    @Override
    public String toString()
    {
        return toAbsoluteNoteString();
    }

    /**
     * @return String A string describing the note and its duration in beats. Example: C!3:2 = C octave=3, beatDuration=2
     */
    public String toFullString()
    {
        return toAbsoluteNoteString() + ":" + beatDuration;
    }

    /**
     * @return E.g. "Db" or "E" (octave independent).
     */
    public String toRelativeNoteString()
    {
        if (alterationDisplay == Alteration.FLAT)
        {
            return notesFlat[getRelativePitch()];
        } else
        {
            return notesSharp[getRelativePitch()];
        }
    }

    /**
     * @param alt
     * @return E.g. "Db" or "E" (octave independent).
     */
    public String toRelativeNoteString(Alteration alt)
    {
        if (alt == Alteration.FLAT)
        {
            return notesFlat[getRelativePitch()];
        } else
        {
            return notesSharp[getRelativePitch()];
        }
    }

    /**
     * @return "D!-1" "D!3". This is the Midi octave convention: pitch=0-11 corresponds to Midi octave -1, C!0 is Midi pitch=12.
     */
    public String toAbsoluteNoteString()
    {
        return toRelativeNoteString() + "!" + (getOctave() - 1);
    }

    //----------------------------------------------------------------------------------------------
    // Static functions
    //----------------------------------------------------------------------------------------------
    public static boolean checkPitch(int p)
    {
        if ((p < PITCH_MIN) || (p > PITCH_MAX))
        {
            return false;
        } else
        {
            return true;
        }
    }

    public static boolean checkVelocity(int v)
    {
        if ((v < VELOCITY_MIN) || (v > VELOCITY_MAX))
        {
            return false;
        } else
        {
            return true;
        }
    }

    public static boolean checkOctave(int o)
    {
        if ((o < OCTAVE_MIN) || (o > OCTAVE_MAX))
        {
            return false;
        } else
        {
            return true;
        }
    }

    /**
     * Convert a positive or relative absolute pitch to a relative pitch between 0 and 11.
     * <p>
     * Ex: absPitch=13, result=1<br>
     * Ex: absPitch=-15, result=9<br>
     *
     * @param absPitch Can be negative or positive, provided that -1 means 11(=B), -2 means 10(=Bb) etc.
     * @return
     */
    public static int getNormalizedRelPitch(int absPitch)
    {
        int relPitch;

        if (absPitch >= 0)
        {
            relPitch = absPitch % 12;
        } else
        {
            relPitch = 12 - (-absPitch % 12);
        }
        return relPitch;
    }

    /**
     * Return Check if a pitch corresponds to keyboard white key (C major scale).
     */
    public static boolean isWhiteKey(int p)
    {
        int pitch = p % 12;
        if ((pitch == 1) || (pitch == 3) || (pitch == 6) || (pitch == 8) || (pitch == 10))
        {
            return false;
        } else
        {
            return true;
        }
    }

    /**
     * Return a pitch which is guaranteed to be between lowPitch and highPitch. If pitch is out of bounds, go up/down 1 octave until we're
     * within the limits.
     *
     * @param pitch
     * @param lowPitch  Constraint: highPitch-lowPitch must be &gt; 11.
     * @param highPitch Constraint: highPitch-lowPitch must be &gt; 11.
     * @return A pitch between lowPitch and highPitch
     */
    static public int limitPitch(int pitch, int lowPitch, int highPitch)
    {
        if (lowPitch > highPitch - 11)
        {
            throw new IllegalArgumentException("lowPitch=" + lowPitch + " highPitch=" + highPitch);
        }
        int newPitch = pitch;
        while (newPitch < lowPitch)
        {
            newPitch += 12;
        }
        while (newPitch > highPitch)
        {
            newPitch -= 12;
        }
        return newPitch;
    }

    /**
     * Return an array of notes whose pitch start from pitchFrom up to pitchTo (included).<br>
     * E.g., if pitchFrom=0 and pitchTo=12, return the array ["C0", "C#0", "D0"...."C1"]
     *
     * @param pitchFrom A positive integer.
     * @param pitchTo   A positive integer.
     *
     * @return An array of Note objects.
     */
    public static Note[] getChromaticNotesArray(int pitchFrom, int pitchTo)
    {
        if ((pitchFrom > pitchTo) || (pitchFrom < 0) || (pitchTo < 0))
        {
            throw new IllegalArgumentException("pitchFrom=" + pitchFrom + " pitchTo=" + pitchTo);
        }

        Note[] notes = new Note[pitchTo - pitchFrom + 1];

        for (int i = 0; i < notes.length; i++)
        {
            notes[i] = new Note(pitchFrom + i);
        }

        return notes;
    }

    //----------------------------------------------------------------------------------------------
    // Private methods
    //----------------------------------------------------------------------------------------------
}
