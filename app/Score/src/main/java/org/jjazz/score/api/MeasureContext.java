/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.score.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Chord;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.SymbolicDuration;
import static org.jjazz.harmony.api.SymbolicDuration.EIGHTH;
import static org.jjazz.harmony.api.SymbolicDuration.EIGHTH_DOTTED;
import static org.jjazz.harmony.api.SymbolicDuration.EIGHTH_TRIPLET;
import static org.jjazz.harmony.api.SymbolicDuration.HALF;
import static org.jjazz.harmony.api.SymbolicDuration.HALF_DOTTED;
import static org.jjazz.harmony.api.SymbolicDuration.HALF_TRIPLET;
import static org.jjazz.harmony.api.SymbolicDuration.QUARTER;
import static org.jjazz.harmony.api.SymbolicDuration.QUARTER_DOTTED;
import static org.jjazz.harmony.api.SymbolicDuration.QUARTER_TRIPLET;
import static org.jjazz.harmony.api.SymbolicDuration.SIXTEENTH;
import static org.jjazz.harmony.api.SymbolicDuration.SIXTEENTH_TRIPLET;
import static org.jjazz.harmony.api.SymbolicDuration.UNKNOWN;
import static org.jjazz.harmony.api.SymbolicDuration.WHOLE;
import static org.jjazz.harmony.api.SymbolicDuration.WHOLE_DOTTED;
import static org.jjazz.harmony.api.SymbolicDuration.WHOLE_TRIPLET;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_FLAT;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_NATURAL;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_NO;
import static org.jjazz.score.api.NotationGraphics.ACCIDENTAL_SHARP;
import org.jjazz.score.api.NotationGraphics.ScoreNote;

/**
 * A single measure context to build ScoreNotes for NotationGraphics.
 */
public class MeasureContext
{

    public static final int G_STAFF_LOWEST_PITCH = 57;
    private final NotationGraphics ng;
    private final Note majorKey;
    private final Map<Integer, Note.Accidental> mapPitchAccidental = new HashMap<>();
    private final int barIndex;
    private final int gStaffLowestPitch;
    private static final Logger LOGGER = Logger.getLogger(MeasureContext.class.getSimpleName());

    public MeasureContext(NotationGraphics ng, int barIndex, Note majorKey, int gStaffLowestPitch)
    {
        this.ng = ng;
        this.barIndex = barIndex;
        this.majorKey = majorKey;
        this.gStaffLowestPitch = gStaffLowestPitch;
    }

    public MeasureContext(NotationGraphics ng, int barIndex, Note majorKey)
    {
        this(ng, barIndex, majorKey, G_STAFF_LOWEST_PITCH);
    }

    public int getgStaffLowestPitch()
    {
        return gStaffLowestPitch;
    }

    public Note getMajorKey()
    {
        return majorKey;
    }

    public int getBarIndex()
    {
        return barIndex;
    }

    /**
     * Build several ScoreNotes which belong to a chord (same position).
     * <p>
     * Handle accidental selection and note base shift.
     *
     * @param notes
     * @param cs    Can be null. If specified used to try to select the appropriate ScoreNote accidental
     * @return
     */
    public List<ScoreNote> buildChordScoreNotes(List<Note> notes, ChordSymbol cs)
    {
        Objects.requireNonNull(notes);
        List<ScoreNote> res = new ArrayList<>();

//        LOGGER.log(Level.SEVERE, "buildChordScoreNotes() cs={0} notes={1}", new Object[]
//        {
//            cs, notes
//        });

        // Adjust all notes to the chord symbol                    
        List<Note> adjustedNotes = new ArrayList<>(notes.size());
        for (int i = 0; i < notes.size(); i++)
        {
            var ne = getAdjustedNoteToChordSymbol(notes.get(i), cs);
            adjustedNotes.add(ne);
        }


        // Fix notes on the same line
        // Search for adjacent notes and shift the top one
        // The algorithm does not check for all the possible adjacency combinations, but should be enough most of the time
        List<Note> needShift = new ArrayList<>();
        for (int i = 0; i < adjustedNotes.size() - 1; i++)
        {
            var ne = adjustedNotes.get(i);
            boolean neFstaff = isFstaff(ne.getPitch());
            int neLine = neFstaff ? ne.getFStaffLineNumber() : ne.getGStaffLineNumber();
            var neNext = adjustedNotes.get(i + 1);


            if (neFstaff != isFstaff(neNext.getPitch()))
            {
                // Different staves, no risk
                continue;
            }

            // Adjust if 2 notes on same line            
            var neNextLine = neFstaff ? neNext.getFStaffLineNumber() : neNext.getGStaffLineNumber();
            if (neLine == neNextLine)
            {
                if (ne.isWhiteKey())
                {
                    // ne=A  neNext=A# -> use a flat for next note  (we assume that a chord rarely contains 3 successive chromatic notes in a row)
//                    LOGGER.log(Level.FINE, "buildChordScoreNotes() adjacent lines ne={0} neNext={1} => neNext will be changed to FLAT", new Object[]
//                    {
//                        ne,
//                        neNext
//                    });
                    neNext = new Note(neNext, Note.Accidental.FLAT);
                    neNextLine++;
                    adjustedNotes.set(i + 1, neNext);

                } else
                {
                    // ne=Bb  neNext=B -> use a sharp for first note
                    ne = new Note(ne, Note.Accidental.SHARP);
                    neLine--;
                    adjustedNotes.set(i, ne);
                }
            }


            // Adjust shift for adjacent notes
            if (neNextLine == neLine + 1)
            {
                // Shift the next note
                needShift.add(neNext);
                i++;        // Make sure we don't process neNext again
            }
        }


        for (int i = 0; i < adjustedNotes.size(); i++)
        {
            var n = adjustedNotes.get(i);
            ScoreNote sn = new ScoreNote();

            // Set accidental depending on last accidental used for the same staff line
            updateAccidental(sn, n);

            // Set duration
            updateDuration(sn, n);


            // Set staff line
            updateStaffLine(sn, n);

            sn.lateralShift = needShift.contains(n) ? 1 : 0;

            sn.note = notes.get(i);

            res.add(sn);
        }


        return res;
    }

    /**
     * Build the ScoreNote for the specified note.
     *
     * @param n
     * @param cs Can be null. If specified used to try to select the appropriate ScoreNote accidental
     * @return
     */
    public ScoreNote buildScoreNote(Note n, ChordSymbol cs)
    {
//        LOGGER.log(Level.SEVERE, "buildChordScoreNotes() cs={0} note={1}", new Object[]
//        {
//            cs, n
//        });


        Objects.requireNonNull(n);
        ScoreNote res = new ScoreNote();

        // Use chord symbol to adjust the note
        Note adjustedNote = getAdjustedNoteToChordSymbol(n, cs);

        // Set accidental depending on last accidental used for the same staff line
        updateAccidental(res, adjustedNote);

        // Set duration
        updateDuration(res, n);

        // Set staff line
        updateStaffLine(res, adjustedNote);

        res.note = n;

        return res;
    }


    public boolean isFstaff(int pitch)
    {
        return pitch < gStaffLowestPitch;
    }

    // =============================================================================================================================
    // Private methods
    // =============================================================================================================================
    private int getScoreNoteDuration(SymbolicDuration sd)
    {
        int res = switch (sd)
        {
            case SIXTEENTH, SIXTEENTH_TRIPLET ->
                NotationGraphics.NOTE_DURATION_SIXTEENTH;
            case EIGHTH, EIGHTH_DOTTED, EIGHTH_TRIPLET ->
                NotationGraphics.NOTE_DURATION_EIGHTH;
            case UNKNOWN, QUARTER, QUARTER_DOTTED, QUARTER_TRIPLET ->
                NotationGraphics.NOTE_DURATION_QUARTER;
            case HALF, HALF_DOTTED, HALF_TRIPLET ->
                NotationGraphics.NOTE_DURATION_HALF;
            case WHOLE, WHOLE_DOTTED, WHOLE_TRIPLET ->
                NotationGraphics.NOTE_DURATION_WHOLE;
            default -> throw new AssertionError(sd.name());
        };
        return res;
    }


    private void updateDuration(ScoreNote sn, Note n)
    {
        var sd = n.getSymbolicDuration();
        if (sd == SymbolicDuration.UNKNOWN)
        {
            sd = SymbolicDuration.getClosestSymbolicDuration(n.getDurationInBeats());
        }
        sn.dur = getScoreNoteDuration(sd);
        sn.dotted = sd.isDotted() ? 1 : 0;
    }

    private void updateAccidental(ScoreNote res, Note adjustedNote)
    {
        // Set accidental
        int whiteKeyPitch = adjustedNote.getWhiteKeyPitch();
        var prevPitchAlt = mapPitchAccidental.get(whiteKeyPitch);
        if (!Note.isWhiteKey(adjustedNote.getPitch()))
        {
            var acc = adjustedNote.getAccidental();
            res.accidental = acc == prevPitchAlt ? ACCIDENTAL_NO : toScoreNoteAccidental(acc); // Possibly take into accound previous accidental on same pitch
            mapPitchAccidental.put(whiteKeyPitch, acc);
        } else
        {
            res.accidental = prevPitchAlt == null ? ACCIDENTAL_NO : ACCIDENTAL_NATURAL;
            mapPitchAccidental.remove(whiteKeyPitch);
        }
    }

    /**
     * If note is not a white key, get an accidental-adjusted note for this context, e.g. change Db into C# if required.
     *
     * @param n
     * @param cs Can be null. The current chord symbol.
     * @return
     */
    private Note getAdjustedNoteToChordSymbol(Note n, ChordSymbol cs)
    {
        Note res = n;

        // TODO: use key to adjust

        if (!n.isWhiteKey() && cs != null)
        {
            // If it's a note of the chord reuse its accidental, otherwise use chord symbol default accidental
            Chord c = cs.getChord();
            int index = c.indexOfRelativePitch(n.getRelativePitch());
            var acc = index > -1 ? c.getNote(index).getAccidental() : cs.getDefaultAccidental();

            if (n.getAccidental() != acc)
            {
                res = new Note(n, acc);
            }
        }

        return res;
    }

    private int toScoreNoteAccidental(Note.Accidental acc)
    {
        return acc == Note.Accidental.FLAT ? ACCIDENTAL_FLAT : ACCIDENTAL_SHARP;
    }

    private void updateStaffLine(ScoreNote res, Note n)
    {
        res.isFstaff = isFstaff(n.getPitch());
        res.staffLine = (res.isFstaff ? n.getFStaffLineNumber() : n.getGStaffLineNumber()) - 2;
    }


}
