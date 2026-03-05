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

import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jerome
 */
public class ChordTypeTest
{

    private final ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();

    // Chord types used across tests
    private ChordType ctMajor;      // major triad ""
    private ChordType ctMinor;      // minor triad "m"
    private ChordType ctM7;         // major 7th "M7"
    private ChordType ct7;          // dominant 7th "7"
    private ChordType ctm7;         // minor 7th "m7"
    private ChordType ctm7b5;       // half-diminished "m7b5"
    private ChordType ctDim7;       // diminished 7th "dim7"
    private ChordType ctSus;        // suspended 4th "sus"

    public ChordTypeTest()
    {
    }

    @BeforeAll
    public static void setUpClass()
    {
    }

    @AfterAll
    public static void tearDownClass()
    {
    }

    @BeforeEach
    public void setUp()
    {
        ctMajor = ctdb.getChordType("");
        ctMinor = ctdb.getChordType("m");
        ctM7 = ctdb.getChordType("M7");
        ct7 = ctdb.getChordType("7");
        ctm7 = ctdb.getChordType("m7");
        ctm7b5 = ctdb.getChordType("m7b5");
        ctDim7 = ctdb.getChordType("dim7");
        ctSus = ctdb.getChordType("sus");

        assertNotNull(ctMajor, "major triad not found");
        assertNotNull(ctMinor, "minor triad not found");
        assertNotNull(ctM7, "M7 not found");
        assertNotNull(ct7, "7 not found");
        assertNotNull(ctm7, "m7 not found");
        assertNotNull(ctm7b5, "m7b5 not found");
        assertNotNull(ctDim7, "dim7 not found");
        assertNotNull(ctSus, "sus not found");
    }

    @AfterEach
    public void tearDown()
    {
    }


    // =========================================================================================================
    // getDegreeMostProbable
    // =========================================================================================================

    @Test
    public void testGetDegreeMostProbable_directMatch_third()
    {
        // Major triad has THIRD at relPitch=4 — direct match
        assertEquals(Degree.THIRD, ctMajor.getDegreeMostProbable(4));
    }

    @Test
    public void testGetDegreeMostProbable_directMatch_thirdFlat()
    {
        // m7 has THIRD_FLAT at relPitch=3 — direct match (Javadoc: Cm7,relPitch=Eb=3 => THIRD_FLAT)
        assertEquals(Degree.THIRD_FLAT, ctm7.getDegreeMostProbable(3));
    }

    @Test
    public void testGetDegreeMostProbable_directMatch_fifth()
    {
        // Major triad has FIFTH at relPitch=7 — direct match
        assertEquals(Degree.FIFTH, ctMajor.getDegreeMostProbable(7));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_ninthFlat()
    {
        // Major triad has no degree at relPitch=1 — fallback to NINTH_FLAT
        assertEquals(Degree.NINTH_FLAT, ctMajor.getDegreeMostProbable(1));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_ninth()
    {
        // Major triad has no degree at relPitch=2 — fallback to NINTH
        assertEquals(Degree.NINTH, ctMajor.getDegreeMostProbable(2));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_ninthSharp_whenMajor()
    {
        // C7 has no degree at relPitch=3 (Eb), isMajor()=true — fallback to NINTH_SHARP
        // (Javadoc: C7, relPitch=Eb=3 => NINTH_SHARP)
        assertEquals(Degree.NINTH_SHARP, ct7.getDegreeMostProbable(3));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_thirdFlat_whenNotMajor()
    {
        // Sus chord has no degree at relPitch=3 (Eb), isMajor()=false — fallback to THIRD_FLAT
        assertEquals(Degree.THIRD_FLAT, ctSus.getDegreeMostProbable(3));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_fourthOrEleventh()
    {
        // Major triad has no degree at relPitch=5 — fallback to FOURTH_OR_ELEVENTH
        // (Javadoc: C7, relPitch=F=5 => FOURTH_OR_ELEVENTH)
        assertEquals(Degree.FOURTH_OR_ELEVENTH, ctMajor.getDegreeMostProbable(5));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_eleventhSharp()
    {
        // Major triad has no degree at relPitch=6 — fallback to ELEVENTH_SHARP
        assertEquals(Degree.ELEVENTH_SHARP, ctMajor.getDegreeMostProbable(6));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_thirteenthFlat()
    {
        // Major triad has no degree at relPitch=8 — fallback to THIRTEENTH_FLAT
        assertEquals(Degree.THIRTEENTH_FLAT, ctMajor.getDegreeMostProbable(8));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_sixthOrThirteenth()
    {
        // Major triad has no degree at relPitch=9 — fallback to SIXTH_OR_THIRTEENTH
        assertEquals(Degree.SIXTH_OR_THIRTEENTH, ctMajor.getDegreeMostProbable(9));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_seventhFlat()
    {
        // Major triad has no degree at relPitch=10 — fallback to SEVENTH_FLAT
        assertEquals(Degree.SEVENTH_FLAT, ctMajor.getDegreeMostProbable(10));
    }

    @Test
    public void testGetDegreeMostProbable_fallback_seventh()
    {
        // Major triad has no degree at relPitch=11 — fallback to SEVENTH
        assertEquals(Degree.SEVENTH, ctMajor.getDegreeMostProbable(11));
    }


    // =========================================================================================================
    // fitDegree
    // =========================================================================================================

    @Test
    public void testFitDegree_naturalMatch_major()
    {
        // Natural match: major triad has THIRD with Natural=THIRD
        assertEquals(Degree.THIRD, ctMajor.fitDegree(Degree.THIRD));
    }

    @Test
    public void testFitDegree_naturalMatch_minor()
    {
        // Natural match: m7 has THIRD_FLAT with Natural=THIRD — maps input THIRD_FLAT to chord's THIRD_FLAT
        assertEquals(Degree.THIRD_FLAT, ctm7.fitDegree(Degree.THIRD_FLAT));
    }

    @Test
    public void testFitDegree_naturalMatch_crossAlteration()
    {
        // Natural match across alteration: m7 has THIRD_FLAT, input THIRD — getDegree(Natural.THIRD) = THIRD_FLAT
        assertEquals(Degree.THIRD_FLAT, ctm7.fitDegree(Degree.THIRD));
    }

    @Test
    public void testFitDegree_pitchMatch_halfDim()
    {
        // Javadoc: d=ELEVENTH_SHARP (pitch=6), this=m7b5 (has FIFTH_FLAT at pitch=6) => FIFTH_FLAT
        assertEquals(Degree.FIFTH_FLAT, ctm7b5.fitDegree(Degree.ELEVENTH_SHARP));
    }

    @Test
    public void testFitDegree_noMatch_returnsNull()
    {
        // Javadoc: d=ELEVENTH_SHARP, this=M7 (no #11, no pitch=6) => null
        assertNull(ctM7.fitDegree(Degree.ELEVENTH_SHARP));
    }

    @Test
    public void testFitDegree_noMatch_seventhOnMajorTriad()
    {
        // Major triad has no 7th by natural degree, and pitch=11 not present — returns null
        assertNull(ctMajor.fitDegree(Degree.SEVENTH));
    }


    // =========================================================================================================
    // fitDegreeAdvanced
    // =========================================================================================================

    @Test
    public void testFitDegreeAdvanced_ninthFlat_onMajor_returnsNinth()
    {
        // Major triad has no 9th — fallback assumption: NINTH (not m7b5/m9b5)
        assertEquals(Degree.NINTH, ctMajor.fitDegreeAdvanced(Degree.NINTH_FLAT, null));
    }

    @Test
    public void testFitDegreeAdvanced_ninthFlat_onM7b5_returnsNinthFlat()
    {
        // m7b5 (locrian mode) — special case: NINTH_FLAT
        assertEquals(Degree.NINTH_FLAT, ctm7b5.fitDegreeAdvanced(Degree.NINTH_FLAT, null));
    }

    @Test
    public void testFitDegreeAdvanced_ninth_onM7b5_returnsNinthFlat()
    {
        // m7b5 overrides NINTH to NINTH_FLAT (locrian mode)
        assertEquals(Degree.NINTH_FLAT, ctm7b5.fitDegreeAdvanced(Degree.NINTH, null));
    }

    @Test
    public void testFitDegreeAdvanced_third_onSus_returnsFourth()
    {
        // Sus chord has no third — THIRD maps to FOURTH_OR_ELEVENTH
        assertEquals(Degree.FOURTH_OR_ELEVENTH, ctSus.fitDegreeAdvanced(Degree.THIRD, null));
    }

    @Test
    public void testFitDegreeAdvanced_thirdFlat_onSus_returnsFourth()
    {
        // Sus chord has no third — THIRD_FLAT also maps to FOURTH_OR_ELEVENTH
        assertEquals(Degree.FOURTH_OR_ELEVENTH, ctSus.fitDegreeAdvanced(Degree.THIRD_FLAT, null));
    }

    @Test
    public void testFitDegreeAdvanced_fourthOrEleventh_onMinor_returnsFourth()
    {
        // Minor family: FOURTH_OR_ELEVENTH is valid for all minor chords
        assertEquals(Degree.FOURTH_OR_ELEVENTH, ctm7.fitDegreeAdvanced(Degree.FOURTH_OR_ELEVENTH, null));
    }

    @Test
    public void testFitDegreeAdvanced_fourthOrEleventh_onMajor_returnsFourth()
    {
        // Major triad: no #11, no altered 9th — fallback to FOURTH_OR_ELEVENTH
        assertEquals(Degree.FOURTH_OR_ELEVENTH, ctMajor.fitDegreeAdvanced(Degree.FOURTH_OR_ELEVENTH, null));
    }

    @Test
    public void testFitDegreeAdvanced_sixthOrThirteenth_onM7b5_returnsThirteenthFlat()
    {
        // m7b5 (locrian): 13th maps to b13 — exercises the getName().equals("m7b5") fix
        assertEquals(Degree.THIRTEENTH_FLAT, ctm7b5.fitDegreeAdvanced(Degree.SIXTH_OR_THIRTEENTH, null));
    }

    @Test
    public void testFitDegreeAdvanced_sixthOrThirteenth_onMajor_returnsSixthOrThirteenth()
    {
        // Major triad: no #5, no direct 13th — fallback to SIXTH_OR_THIRTEENTH
        assertEquals(Degree.SIXTH_OR_THIRTEENTH, ctMajor.fitDegreeAdvanced(Degree.SIXTH_OR_THIRTEENTH, null));
    }

    @Test
    public void testFitDegreeAdvanced_seventhFlat_onDim7_returnsSixthOrThirteenth()
    {
        // dim7: b7 is actually bb7 (diminished 7th = enharmonic 13th) — exercises getName().equals("dim7") fix
        assertEquals(Degree.SIXTH_OR_THIRTEENTH, ctDim7.fitDegreeAdvanced(Degree.SEVENTH_FLAT, null));
    }

    @Test
    public void testFitDegreeAdvanced_seventh_onMinorTriad_returnsSeventhFlat()
    {
        // Minor triad with no 6th — dorian assumption: 7M maps to b7
        assertEquals(Degree.SEVENTH_FLAT, ctMinor.fitDegreeAdvanced(Degree.SEVENTH, null));
    }

    @Test
    public void testFitDegreeAdvanced_seventh_onSus_returnsSeventhFlat()
    {
        // Sus family: 7M maps to b7
        assertEquals(Degree.SEVENTH_FLAT, ctSus.fitDegreeAdvanced(Degree.SEVENTH, null));
    }


    // =========================================================================================================
    // Private methods
    // =========================================================================================================


}
