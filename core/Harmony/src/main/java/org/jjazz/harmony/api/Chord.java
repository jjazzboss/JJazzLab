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
package org.jjazz.harmony.api;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A chord is an array of notes which have different pitches.
 * <p>
 * Notes are kept ordered by pitch.
 */
public class Chord implements Cloneable
{

    /**
     * The notes of the chord
     */
    private final List<Note> notes = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(Chord.class.getName());

    public Chord()
    {
    }

    public Chord(List<? extends Note> newNotes)
    {
        for (Note note : newNotes)
        {
            add(note);
        }
    }

    /**
     * Get the number of notes in this Chord.
     *
     * @return
     */
    public int size()
    {
        return notes.size();
    }

    /**
     * Add a Note to the chord.
     * <p>
     * Nothing is done if note's pitch is already present. Notes are kept ordered by pitch.
     *
     * @param note
     */
    public void add(Note note)
    {
        int index = Collections.binarySearch(notes, note, (n1, n2) -> Integer.compare(n1.getPitch(), n2.getPitch()));
        if (index < 0)
        {
            index = -(index + 1);
            notes.add(index, note);
        }
    }

    /**
     * Remove the Note who has a specific pitch from the chord.
     *
     * @param p Pitch of the note to remove.
     * @return The removed Note. Null if not found.
     */
    public Note removePitch(int p)
    {
        int i = indexOfPitch(p);
        return (i == -1) ? null : removeNote(i);
    }

    @Override
    public Chord clone()
    {
        Chord c = new Chord();
        for (Note n : notes)
        {
            c.add(n);
        }
        return c;
    }

    /**
     * Remove all notes from the chord.
     */
    public void clear()
    {
        notes.clear();
    }

    /**
     * Return the notes of this chord.
     *
     * @return An unmodifiable list ordered by ascending pitch.
     */
    public List<Note> getNotes()
    {
        return Collections.unmodifiableList(notes);
    }

    /**
     * Return the pitches of this chord.
     *
     * @return A list ordered by ascending pitch.
     */
    public List<Integer> getPitches()
    {
        return notes.stream()
                .map(n -> n.getPitch())
                .toList();
    }

    /**
     * Get a chord built from the unique relative pitches present in this chord.
     * <p>
     * Ex: if chord notes=C2,C3,E3,G4, return a chord with notes=C1,E1,G1
     *
     * @return
     */
    public Chord getRelativePitchChord()
    {
        Chord c = new Chord();
        for (Note n : notes)
        {
            c.add(new Note(n.getRelativePitch()));
        }
        return c;
    }

    /**
     * Use the specified relative pitches and this chord intervals to create a new chord.
     * <p>
     * Ex: if this=C2,G3,E4 and relPitches=Ab0,Eb0,C0 (3,7,5 degrees of Fm7) and startBelow=true<br>
     * then result chord=Ab1,Eb3,C4<p>
     * Ex: if this=C1,C2,G3,E4 and relPitches=Ab0,Eb0,C0 (3,7,5 degrees of Fm7) and startBelow=false<br>
     * then result chord=Ab1,Ab2,Eb4,C5<p>
     * Normally the resulting chord has the same size than this chord. However if we reach the upper pitch limit (127) during the calculation this can result in
     * a smallest chord (because 2 notes end up having the same pitch).
     *
     * @param relPitches A list of relative pitch [0-11]. Size must be equal to this chord's number of unique notes.
     * @param startBelow If true the first (lowest) note is created equals or below the first note of this chord.
     * @return A Chord whose size is equals or less than this Chord size.
     */
    public Chord computeParallelChord(List<Integer> relPitches, boolean startBelow)
    {
        LOGGER.log(Level.FINE, "computeParallelChord() -- relPitches={0}", relPitches);


        Chord result = new Chord();

        if (relPitches.size() != getRelativePitchChord().size())
        {
            throw new IllegalArgumentException("this=" + this + " relPitches=" + relPitches);
        } else if (size() == 0)
        {
            return result;
        }

        // Special handling for the first note
        Note n0 = getNote(0);
        int destRelPitch = relPitches.get(0);
        int destPitch = startBelow ? n0.getLowerPitch(destRelPitch, true) : n0.getUpperPitch(destRelPitch, true);
        Note lastNote = new Note(destPitch);
        result.add(lastNote);

        if (size() > 1)
        {
            // Used to handle chords with two or more identical notes eg [C2,C3,E3,G3]
            HashMap<Integer, Integer> mapSave = new HashMap<>();
            mapSave.put(n0.getRelativePitch(), destRelPitch);

            // The octave intervals between this chord's individual notes 
            List<Integer> skipOctaves = computeSkipOctaves();

            // Other notes
            int destPitchIndex = 1;
            for (int i = 1; i < skipOctaves.size(); i++)
            {
                Note n = getNote(i);
                destRelPitch = mapSave.get(n.getRelativePitch()) == null ? relPitches.get(destPitchIndex++) : mapSave.get(n.getRelativePitch());
                for (int j = 0; j <= skipOctaves.get(i); j++)
                {
                    destPitch = lastNote.getUpperPitch(destRelPitch, false);
                    lastNote = new Note(destPitch);
                }
                result.add(lastNote);
                // Save it so that if we have the same source note at higher octave we reuse the same destination relative pitch
                mapSave.put(n.getRelativePitch(), destRelPitch);
            }
        }
        return result;
    }

    /**
     * Return a single note of this chord.
     *
     * @param index Index of the note in the chord.
     * @return
     */
    public Note getNote(int index)
    {
        if ((index < 0) || (index > notes.size()))
        {
            throw new IllegalArgumentException("index=" + index + " notes=" + notes);
        }

        return notes.get(index);
    }

    /**
     * Remove a specific note in the chord.
     *
     * @param index Index of the note in the chord.
     * @return The removed Note.
     */
    public Note removeNote(int index)
    {
        if ((index < 0) || (index > notes.size()))
        {
            throw new IllegalArgumentException("i=" + index);
        }
        Note rn = notes.remove(index);
        return rn;

    }

    /**
     * Find a note within the Chord whose pitch equals p.
     *
     * @param p Pitch to search for.
     *
     * @return The index of the object if found, otherwise -1.
     */
    public int indexOfPitch(int p)
    {
        if (!Note.checkPitch(p))
        {
            throw new IllegalArgumentException("pitch=" + p);
        }

        for (int i = 0; i < notes.size(); i++)
        {
            int pitch = notes.get(i).getPitch();

            if (pitch > p)
            {
                // No need to continue since notes are ordered by pitch
                break;
            } else if (pitch == p)
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * Find a note whose pitch relatively equals p, i.e independently of the octave.
     *
     * @param p A pitch
     *
     * @return The index of the MidiNote if found, otherwise -1.
     */
    public int indexOfRelativePitch(int p)
    {
        int res = -1;
        p = p % 12;
        for (int i = 0; i < notes.size(); i++)
        {
            if (notes.get(i).getRelativePitch() == p)
            {
                res = i;
            }
        }

        return res;
    }

    /**
     * Return the pitch of the highest note of the chord, 0 if no notes in the chord.
     *
     * @return
     */
    public int getMaxPitch()
    {
        int size = notes.size();

        if (size == 0)
        {
            return 0;
        } else
        {
            return notes.get(size - 1).getPitch();
        }
    }

    /**
     * Return the pitch of the lowest note of the chord, 0 if no notes in the chord.
     *
     * @return
     */
    public int getMinPitch()
    {
        int size = notes.size();

        if (size == 0)
        {
            return 0;
        } else
        {
            return notes.get(0).getPitch();
        }
    }

    /**
     * Transpose the chord of +/- n octaves so that chord is centered between lowPitch and maxPitch;
     *
     * @param lowPitch int
     * @param maxPitch int
     */
    public void centerChordOctave(int lowPitch, int maxPitch)
    {
        int cCentralOctave = (getMaxPitch() + getMinPitch()) / (2 * 12);
        int nbTransposeOctave = (lowPitch + maxPitch) / (2 * 12) - cCentralOctave;
        transpose(nbTransposeOctave * 12);
    }

    /**
     * Transpose all the notes of the MidiChord.
     *
     * @param t An integer representing the transposition value in semitons.
     */
    public void transpose(int t)
    {
        Note[] oldNotes = notes.toArray(new Note[0]);
        clear();
        for (Note n : oldNotes)
        {
            add(n.getTransposed(t));
        }
    }

    /**
     * Calculate a "distance" between notes of chord c and this chord.
     *
     * @param c Must have the same number of notes than this chord.
     * @return The sum of note-to-note absolute distances in semi-tons.
     */
    public int computeDistance(Chord c)
    {
        if (c == null || c.size() != size())
        {
            throw new IllegalArgumentException("c=" + c + " this=" + this);
        }
        int dist = 0;
        for (int i = 0; i < size(); i++)
        {
            dist += Math.abs(c.getNote(i).getPitch() - notes.get(i).getPitch());
        }
        return dist;
    }

    /**
     * Make the MidiChord start at first octave : the lowest note of the MidiChord is transposed to be in octave 0.
     */
    public void normalize()
    {
        // First find the lowest pitch
        int lowest = getMinPitch();

        // then transpose to octave 0
        transpose((lowest / 12) * -12);
    }

    /**
     * @param obj
     * @return True if the notes composing the specified MidiChord are the same.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Chord c)
        {
            return notes.equals(c.notes);
        } else
        {
            throw new ClassCastException("obj=" + obj);
        }
    }

    /**
     * Compare the relative intervals of 2 Chords.
     * <p>
     * E.g. [C2,E2].equalsRelative([F4,A4]) will return true.
     *
     * @param c The chord to compare to.
     *
     * @return True if chords relatively represent the same voicing.
     */
    public boolean equalsRelative(Chord c)
    {
        if (c.size() != size())
        {
            return false;
        } else if (size() == 1)
        {
            return true;
        }

        for (int i = 1; i < size(); i++)
        {
            int cDelta = c.getNote(i).getPitch() - c.getNote(i - 1).getPitch();
            int delta = getNote(i).getPitch() - getNote(i - 1).getPitch();
            if (delta != cDelta)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Compute the hashcode based only on the pitches of notes composing the MidiChord.
     *
     * @return The hashcode of the MidiChord.
     */
    @Override
    public int hashCode()
    {
        String s = notes.stream()
                .map(n -> String.valueOf(n.getPitch()))
                .collect(Collectors.joining());
        return s.hashCode();
    }

    @Override
    public String toString()
    {
        return "Chord" + notes.toString();

        // return new String("MidiChord" + notes.toString() + " intervals=" + intervals);
    }

    /**
     * A string like "[D,F#,A,E]".
     * <p>
     * Notes are sorted by ascending absolute pitch.
     *
     * @param acc If not null, force all notes to use acc.
     * @return
     */
    public String toRelativeNoteString(Note.Accidental acc)
    {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        notes.forEach(n -> joiner.add(acc == null ? n.toRelativeNoteString() : n.toRelativeNoteString(acc)));
        return joiner.toString();
    }

    /**
     * @return E.g. the string "[D3,F#4,C5,E5,D6,Bb6]".
     */
    public String toAbsoluteNoteString()
    {
        StringBuilder sb = new StringBuilder("[");

        for (Note n : notes)
        {
            sb.append(n.toPianoOctaveString()).append(",");
        }

        if (!notes.isEmpty())
        {
            sb.setLength(sb.length() - 1);
        }

        sb.append("]");

        return sb.toString();
    }

    // ========================================================================================================
    // Private methods
    // ========================================================================================================
    /**
     * Compute the SkipOctave value for each chord note.
     * <p>
     * SkipOctave value=how many octaves should we skip to go from one chord note to the next chord note ?<br>
     * If 0, take the immediate next upper note. If 1, take the note after immediate upper note. Etc.
     * <p>
     * Example: if chord = C1 E1 G2 F3, then:<br>
     * SkipOctave[0]=0 (always 0 for lowest note)<br>
     * SkipOctave[1]=0 (C1->E1, immediate next note)<br>
     * SkipOctave[2]=1 (E1->G2, G2 is after the immediate next note G1)<br>
     * SkipOctave[3]=0 (G2->F3, immediate next note)<br>
     *
     * @return Size equals the number of unique pitch in p.
     */
    private List<Integer> computeSkipOctaves()
    {
        ArrayList<Integer> res = new ArrayList<>();
        int lastPitch = -1;
        for (Note ne : notes)
        {
            if (res.isEmpty())
            {
                // First note is always 0
                res.add(0);
            } else
            {
                int pitchDelta = ne.getPitch() - lastPitch;
                int skipNext = pitchDelta / 12;
                res.add(skipNext);
            }
            lastPitch = ne.getPitch();
        }
        return res;
    }

}
