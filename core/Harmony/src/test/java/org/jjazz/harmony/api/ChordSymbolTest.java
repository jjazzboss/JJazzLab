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

import java.text.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Exceptions;

/**
 *
 * @author Jerome
 */
public class ChordSymbolTest
{

    ChordSymbol cs1, cs2, cs3, cs4;

    public ChordSymbolTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
        try
        {
            cs1 = new ChordSymbol("Gm7");
            cs2 = new ChordSymbol("A9");
            cs3 = new ChordSymbol("Eb7M#11");
            cs4 = new ChordSymbol("Db13b9");
        } catch (ParseException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of getTransposedChordSymbol method, of class ChordSymbol.
     */
    // @Test
    public void testGetTransposedChordSymbol()
    {
        System.out.println("getTransposedChordSymbol");
        int t = 0;
        Note.Accidental alt = null;
        ChordSymbol instance = new ChordSymbol();
        ChordSymbol expResult = null;
        ChordSymbol result = instance.getTransposedChordSymbol(t, alt);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSimplified method, of class ChordSymbol.
     */
    // @Test
    public void testGetSimplified()
    {
        System.out.println("getSimplified");
        int nbMaxDegrees = 0;
        ChordSymbol instance = new ChordSymbol();
        ChordSymbol expResult = null;
        ChordSymbol result = instance.getSimplified(nbMaxDegrees);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getChord method, of class ChordSymbol.
     */
    @Test
    public void testGetChord()
    {
        System.out.println("getChord");
        Chord c1 = cs1.getChord();
        Chord c2 = cs2.getChord();
        Chord c3 = cs3.getChord();
        Chord c4 = cs4.getChord();
        System.out.println("cs1=" + cs1 + " c1=" + c1);
        System.out.println("cs2=" + cs2 + " c2=" + c2);
        System.out.println("cs3=" + cs3 + " c3=" + c3);
        System.out.println("cs4=" + cs4 + " c4=" + c4);
        // assertEquals(expResult, c);
        // TODO review the generated test code and remove the default call to fail.
        // fail("The test case is a prototype.");
    }

}
