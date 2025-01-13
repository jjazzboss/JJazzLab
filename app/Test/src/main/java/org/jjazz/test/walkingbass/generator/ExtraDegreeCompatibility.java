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
package org.jjazz.test.walkingbass.generator;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Degree;
import static org.jjazz.harmony.api.Degree.ELEVENTH_SHARP;
import static org.jjazz.harmony.api.Degree.FOURTH_OR_ELEVENTH;
import static org.jjazz.harmony.api.Degree.NINTH;
import static org.jjazz.harmony.api.Degree.NINTH_FLAT;
import static org.jjazz.harmony.api.Degree.NINTH_SHARP;
import static org.jjazz.harmony.api.Degree.SEVENTH;
import static org.jjazz.harmony.api.Degree.SEVENTH_FLAT;
import static org.jjazz.harmony.api.Degree.SIXTH_OR_THIRTEENTH;
import static org.jjazz.harmony.api.Degree.THIRTEENTH_FLAT;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;

/**
 * Indicates if a list of notes is "musically compatible" with an extra degree of a chord symbol.
 * <p>
 * Ex: 6/7 degree for a C chord, 9 for a C7 chord, 13 for a C chord.
 */
public enum ExtraDegreeCompatibility
{
    /**
     * Phrase seems not compatible with this degree.
     * <p>
     * Example: A C7 phrase which contains significant Db notes and no D note, when tested against the 9th.
     */
    INCOMPATIBLE,
    /**
     * Phrase seems compatible and it does not use the degree (non-significant notes ignored).
     */
    COMPATIBLE_NO_USE,
    /**
     * Phrase seems compatible and it does use the degree (non-significant notes ignored).
     */
    COMPATIBLE_USE;


    public boolean isCompatible()
    {
        return this != INCOMPATIBLE;
    }

    /**
     * Assuming a musical phrase corresponds to cs, check if it is "musically compatible" with an extra degree of cs.
     * <p>
     * Essentially check the absence of notes usually "musically incompatible" with the extra degree.
     * <p>
     * Examples:<br>
     * - If d=9th of C9, check that notes do not contain "significant" Db or D# notes.<br>
     * - If d=9th of Cm9, check that notes do not contain "significant" Db notes.<br>
     *
     * @param ts
     * @param notes Notes of the musical phrase. Non "significant" notes (ghost notes, chromatic approach notes, ...) MUST have been previously removed by
     *              caller.
     * @param cs
     * @param d     An additional Degree of cs to be tested against the musical phrase. It must not be an existing degree of cs.
     * @return
     */
    static public ExtraDegreeCompatibility test(TimeSignature ts, Collection<NoteEvent> notes, ChordSymbol cs, Degree d)
    {
        Preconditions.checkArgument(!cs.getChordType().getDegrees().contains(d), "cs=%s d=% notes=%s", cs, d, notes);

        ExtraDegreeCompatibility res = INCOMPATIBLE;

        if (checkNoIncompatibleDegrees(ts, cs, notes, d))
        {
            res = isUsed(notes, cs.getRelativePitch(d)) ? COMPATIBLE_USE : COMPATIBLE_NO_USE;
        }

        return res;
    }


    // ===========================================================================================================
    // Private methods
    // ===========================================================================================================
    private static boolean checkNoIncompatibleDegrees(TimeSignature ts, ChordSymbol cs, Collection<NoteEvent> notes, Degree degree)
    {
        boolean b = true;

        var degreesToAvoid = getIncompatibleDegrees(cs.getChordType(), degree);

        boolean avoidNotesPresent = degreesToAvoid.stream()
                .anyMatch(d -> isUsed(notes, cs.getRelativePitch(d)));

        if (avoidNotesPresent)
        {
            // There is at least 1 degree to avoid, check that it's not more "significant" than degree
            float totalAvoidDuration = 0;
            for (Degree dAvoid : degreesToAvoid)
            {
                totalAvoidDuration += getTotalWeightedDuration(ts, notes, cs.getRelativePitch(dAvoid)); // Promote downbeat over upbeat 
            }
            b = getTotalWeightedDuration(ts, notes, cs.getRelativePitch(degree)) > totalAvoidDuration;
        }

        return b;
    }

    /**
     * Compute total duration used by relPitch, but upbeat notes are counted for half of their duration.
     *
     * @param ts
     * @param notes    Start at beat position 0
     * @param relPitch
     * @return
     */
    private static float getTotalWeightedDuration(TimeSignature ts, Collection<NoteEvent> notes, int relPitch)
    {
        float res = 0;

        for (var n : notes)
        {
            if (n.getRelativePitch() == relPitch)
            {
                float pos = n.getPositionInBeats();
                int nbBars = (int) (pos / ts.getNbNaturalBeats());
                float beat = pos - (nbBars * ts.getNbNaturalBeats());
                float dur = ts.isDownBeat(beat) ? n.getDurationInBeats() : n.getDurationInBeats() / 2;
                res += dur;
            }
        }

        return res;
    }

    private static boolean isUsed(Collection<NoteEvent> notes, int relPitch)
    {
        return notes.stream().anyMatch(n -> n.getRelativePitch() == relPitch);
    }


    /**
     * Get the "usually" musically-incompatible degrees of extraDegree when playing this chord type.
     * <p>
     *
     * @param ct
     * @param extraDegree
     * @return
     */
    static private List<Degree> getIncompatibleDegrees(ChordType ct, Degree extraDegree)
    {
        List<Degree> res = switch (extraDegree)
        {
            case NINTH_FLAT ->      // ct must be C/C7(b#5)/C13/Csus/C7sus
            {
                if (ct.isMinor())
                {
                    throw new IllegalStateException("ct=" + ct + " d=" + extraDegree);    // minor/major mismatch should have been caught earlier
                }
                yield (List.of(Degree.NINTH));
            }

            case NINTH ->        // ct must be C(b#5)/C7(b#5)/C13/C7M/C7M(b#5)/C6/Csus/C7sus/Cm/Cm6/Cm7/Cm7b5/dim
                ct.isMajor() ? List.of(Degree.NINTH_FLAT, Degree.NINTH_SHARP) : List.of(Degree.NINTH_FLAT);

            case NINTH_SHARP ->  // ct must be C/C7(alt)/C13...
            {
                if (ct.isMinor())
                {
                    throw new IllegalStateException("ct=" + ct + " d=" + extraDegree);    // minor/major mismatch should have been caught earlier
                }
                yield (List.of(Degree.NINTH));
            }

            case FOURTH_OR_ELEVENTH ->      // eleventh, ct must be Cm; Cm7, Cm7b5, Cdim...
            {
                if (!ct.isMinor())
                {
                    throw new IllegalStateException("ct=" + ct + " d=" + extraDegree);
                }
                yield Collections.emptyList();    // If 3rd it's a transition note since we're in minor. b5 also possible but no problem.
            }

            case ELEVENTH_SHARP ->              // ct must be C/C7/C9/C13/C7M/C9M
            {
                if (!ct.isMajor())
                {
                    throw new IllegalStateException("ct=" + ct + " d=" + extraDegree);
                }
                yield Collections.emptyList();    // If 11th it's a transition note since we're in major. 5th is OK.
            }

            case THIRTEENTH_FLAT ->         // not really used ?
                List.of(Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH);

            case SIXTH_OR_THIRTEENTH ->     // ct must be C/C7/C9/C13/C7M/Cm/Cm7/Cm9/Csus/Csus7
                Collections.emptyList();    // if 5# it's a transition note provided the possible ct values

            case SEVENTH_FLAT ->            // ct must be C, C(b#5), Cm, Cdim
                List.of(Degree.SEVENTH);

            case SEVENTH ->                 // ct must be C, C(b#5), Cm, Cdim
                List.of(Degree.SEVENTH_FLAT);

            default ->
                throw new IllegalStateException("d=" + extraDegree);
        };

        return res;
    }

}
