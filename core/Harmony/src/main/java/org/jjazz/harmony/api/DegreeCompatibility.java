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
package org.jjazz.harmony.api;

import java.util.List;

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
     * @param notes Notes corresponding to a C-based chord context like Cm or C7. Non "significant" notes (ghost notes, chromatic approach notes, ...) must have been
     *              removed by caller.
     * @param d     By convention if d is root, returns one of the 2 COMPATIBLE values.
     * @return
     */
    static public DegreeCompatibility get(List<Note> cBasedNotes, Degree d)
    {
        DegreeCompatibility res = INCOMPATIBLE;

        boolean compatible = d == Degree.ROOT || avoidsDegrees(notes, d);
        if (compatible)
        {
            res = isUsed(notes, d) ? COMPATIBLE_USE : COMPATIBLE_NO_USE;
        }

        return res;
    }


    // ===========================================================================================================
    // Private methods
    // ===========================================================================================================
    private static boolean avoidsDegrees(List<Note> notes, Degree degree)
    {
        var degreesToAvoid = degree.getChordIncompatibleDegrees();

        // If no degreesToAvoid, it's OK
        boolean b = true;
        for (Degree d : degreesToAvoid)
        {
            b = isUsed(notes, d);
            if (!b)
            {
                break;
            }
        }

        if (!b)
        {
            // There is at least 1 degree to avoid, check we don't use much than our degree
            float totalAvoidDuration = 0;
            for (Degree d : degreesToAvoid)
            {
                totalAvoidDuration += getTotalDuration(notes, d);
            }
            b = getTotalDuration(notes, degree) > totalAvoidDuration;
        }

        return b;
    }

    private static float getTotalDuration(List<Note> notes, Degree d)
    {
        var dur = notes.stream()
                .filter(n -> n.getRelativePitch() == d.getPitch())
                .mapToDouble(n -> n.getDurationInBeats())
                .sum();
        return (float) dur;
    }

    private static boolean isUsed(List<Note> notes, Degree d)
    {
        return notes.stream().anyMatch(n -> n.getRelativePitch() == d.getPitch());
    }

}
