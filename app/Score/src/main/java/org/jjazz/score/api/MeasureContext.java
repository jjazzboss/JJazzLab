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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
 * A single measure context to help build ScoreNotes to be used by NotationGraphics.
 */
public class MeasureContext
{

    // private final NotationGraphics ng;
    private final Note majorKey;
    private final Map<Integer, Note.Alteration> mapPitchAlteration = new HashMap<>();
    private final int barIndex;


    public MeasureContext(int barIndex, Note majorKey)
    {
        this.barIndex = barIndex;
        this.majorKey = majorKey;
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
     * Build the ScoreNote for the specified note.
     *
     * @param n
     * @param useFclef If true ScoreNote is built for a staff with F clef, otherwise for a staff with G clef.
     * @param cs       Can be null. If specified used to try to select the appropriate ScoreNote alteration
     * @return
     */
    public ScoreNote buildScoreNote(Note n, boolean useFclef, ChordSymbol cs)
    {
        Objects.requireNonNull(n);
        ScoreNote res = new ScoreNote();
        int pitch = n.getPitch();


        // Duration
        var sd = n.getSymbolicDuration();
        if (sd == SymbolicDuration.UNKNOWN)
        {
            sd = SymbolicDuration.getClosestSymbolicDuration(n.getDurationInBeats());
        }
        res.dur = getScoreNoteDuration(sd);
        res.dotted = sd.isDotted() ? 1 : 0;


        // Alteration
        Note adjustedNote = adjustNote(n, cs);
        int whiteKeyPitch = adjustedNote.getWhiteKeyPitch();
        var prevPitchAlt = mapPitchAlteration.get(whiteKeyPitch);
        if (!Note.isWhiteKey(pitch))
        {
            var alt = adjustedNote.getAlterationDisplay();
            res.accidental = alt == prevPitchAlt ? ACCIDENTAL_NO : toScoreNoteAlteration(alt); // Possibly take into accound previous alteration on same pitch
            mapPitchAlteration.put(whiteKeyPitch, alt);
        } else
        {
            res.accidental = prevPitchAlt == null ? ACCIDENTAL_NO : ACCIDENTAL_NATURAL;
            mapPitchAlteration.remove(whiteKeyPitch);
        }

        res.staffLine = (useFclef ? adjustedNote.getFStaffLineNumber() : adjustedNote.getGStaffLineNumber()) - 2;

        return res;
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

    /**
     * If note is not a white key, adjust the note alteration to this context, e.g. change Db into C# if required.
     *
     * @param n
     * @param cs Can be null. The current chord symbol.
     * @return
     */
    private Note adjustNote(Note n, ChordSymbol cs)
    {
        Note res = n;

        // TODO: use key to adjust

        if (!n.isWhiteKey() && cs != null)
        {
            // If it's a note of the chord reuse its alteration, otherwise use chord symbol default alteration
            Chord c = cs.getChord();
            int index = c.indexOfRelativePitch(n.getRelativePitch());
            var alt = index > -1 ? c.getNote(index).getAlterationDisplay() : cs.getDefaultAlteration();

            if (n.getAlterationDisplay() != alt)
            {
                res = new Note(n, alt);
            }
        }

        return res;
    }

    private int toScoreNoteAlteration(Note.Alteration alt)
    {
        return alt == Note.Alteration.FLAT ? ACCIDENTAL_FLAT : ACCIDENTAL_SHARP;
    }


}
