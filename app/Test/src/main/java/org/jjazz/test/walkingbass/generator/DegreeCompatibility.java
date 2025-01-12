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

import java.util.Collection;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.Degree;
import org.jjazz.phrase.api.NoteEvent;

/**
 * Indicates if a list of notes is "musically compatible" with a given degree of a chord symbol.
 *
 * @see Degree#getChordIncompatibleDegrees()
 */
public enum DegreeCompatibility
{
    /**
     * Phrase seems not compatible with this degree.
     * <p>
     * Example: if d=minor 3rd but notes contain (significant) major 3rd.
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
     * Check if a phrase is "musically compatible" with a degree of a chord symbol.
     * <p>
     * Essentially check the absence of notes usually "musically incompatible" with the degree.
     * <p>
     * Example: if d=minor 3rd degree of Cm, check that the total duration of E notes is less than Eb notes.
     *
     * @param notes Notes corresponding to cs context. Non "significant" notes (ghost notes, chromatic approach notes, ...) must have been removed by caller.
     * @param cs
     * @param d     Degree of cs. By convention if d is root, returns COMPATIBLE_USE..
     * @return
     */
    static public DegreeCompatibility get(Collection<NoteEvent> notes, ChordSymbol cs, Degree d)
    {
        DegreeCompatibility res = INCOMPATIBLE;

        if (d == Degree.ROOT)
        {
            res = COMPATIBLE_USE;
        } else if (avoidsDegrees(cs, notes, d))
        {
            res = isUsed(notes, cs.getRelativePitch(d)) ? COMPATIBLE_USE : COMPATIBLE_NO_USE;
        }

        return res;
    }


    // ===========================================================================================================
    // Private methods
    // ===========================================================================================================
    /**
     * Check that cs degree is compatible with the specified notes, i.e. check that notes do not contain incompatible (significant) notes.
     *
     * @param cs
     * @param notes
     * @param degree
     * @return
     */
    private static boolean avoidsDegrees(ChordSymbol cs, Collection<NoteEvent> notes, Degree degree)
    {
        boolean b = true;

        var degreesToAvoid = degree.getChordIncompatibleDegrees();

        boolean avoidNotesPresent = degreesToAvoid.stream()
                .anyMatch(d -> isUsed(notes, cs.getRelativePitch(d)));

        if (avoidNotesPresent)
        {
            // There is at least 1 degree to avoid, check we don't use it more than our degree
            float totalAvoidDuration = 0;
            for (Degree dAvoid : degreesToAvoid)
            {
                totalAvoidDuration += getTotalDuration(notes, cs.getRelativePitch(dAvoid));
            }
            b = getTotalDuration(notes, cs.getRelativePitch(degree)) > totalAvoidDuration;
        }

        return b;
    }

    private static float getTotalDuration(Collection<NoteEvent> notes, int relPitch)
    {
        var dur = notes.stream()
                .filter(n -> n.getRelativePitch() == relPitch)
                .mapToDouble(n -> n.getDurationInBeats())
                .sum();
        return (float) dur;
    }

    private static boolean isUsed(Collection<NoteEvent> notes, int relPitch)
    {
        return notes.stream().anyMatch(n -> n.getRelativePitch() == relPitch);
    }

}
