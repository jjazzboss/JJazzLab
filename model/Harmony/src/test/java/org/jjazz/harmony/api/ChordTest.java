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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jerome
 */
public class ChordTest
{
    
    public ChordTest()
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
    }
    
    @AfterEach
    public void tearDown()
    {
    }

  

    /**
     * Test of computeParallelChord method, of class Chord.
     */
    // @Test
    public void testComputeParallelChord()
    {
        System.out.println("computeParallelChord");
        List<Integer> relPitches = null;
        boolean startBelow = false;
        Chord instance = new Chord();
        Chord expResult = null;
        Chord result = instance.computeParallelChord(relPitches, startBelow);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of centerChordOctave method, of class Chord.
     */
    // @Test
    public void testCenterChordOctave()
    {
        System.out.println("centerChordOctave");
        int lowPitch = 0;
        int maxPitch = 0;
        Chord instance = new Chord();
        instance.centerChordOctave(lowPitch, maxPitch);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of computeDistance method, of class Chord.
     */
    // @Test
    public void testComputeDistance()
    {
        System.out.println("computeDistance");
        Chord c = null;
        Chord instance = new Chord();
        int expResult = 0;
        int result = instance.computeDistance(c);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }    
}
