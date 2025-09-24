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
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author Jerome
 */
public class ChordTypeTest
{

    private final ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();

    public ChordTypeTest()
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

    }

    @After
    public void tearDown()
    {
    }


    /**
     * Test of getSimilarityScore method, of class ChordType.
     */
//    @Test
//    public void testGetSimilarityScore()
//    {
//        System.out.println("getSimilarityScore");
//        int result = getSimilarityScore("", "", true);
//        assertEquals(63, result);
//        result = getSimilarityScore("m69", "-69", false);
//        assertEquals(63, result);
//        result = getSimilarityScore("7", "m6", true);
//        assertEquals(23, result);
//        result = getSimilarityScore("7", "m6", false);
//        assertEquals(23, result);
//        result = getSimilarityScore("7", "m69", true);
//        assertEquals(22, result);
//        result = getSimilarityScore("7", "m69", false);
//        assertEquals(19, result);
//        result = getSimilarityScore("7", "9", true);
//        assertEquals(62, result);
//        result = getSimilarityScore("7", "9", false);
//        assertEquals(59, result);
//        result = getSimilarityScore("", "13b9", true);
//        assertEquals(60, result);
//        result = getSimilarityScore("", "13b9", false);
//        assertEquals(49, result);
//        result = getSimilarityScore("m6", "m7M", false);
//        assertEquals(63, result);
//        result = getSimilarityScore("m6", "m7", false);
//        assertEquals(55, result);
//        result = getSimilarityScore("m", "m7", true);
//        assertEquals(62, result);
//        result = getSimilarityScore("m7", "m", true);
//        assertEquals(62, result);        
//        result = getSimilarityScore("m", "m69", true);
//        assertEquals(61, result);
//    }


    /**
     * Test of getExtensionDegreeIndexes method, of class ChordType.
     */
    // @Test
    public void testGetExtensionDegreeIndexes()
    {
        System.out.println("getExtensionDegreeIndexes");
        ChordType instance = null;
        List<ChordType.DegreeIndex> expResult = null;
        List<ChordType.DegreeIndex> result = instance.getExtensionDegreeIndexes();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDegreeIndex method, of class ChordType.
     */
    // @Test
    public void testGetDegreeIndex()
    {
        System.out.println("getDegreeIndex");
        Degree d = null;
        ChordType instance = null;
        ChordType.DegreeIndex expResult = null;
        ChordType.DegreeIndex result = instance.getDegreeIndex(d);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDegree method, of class ChordType.
     */
    // @Test
    public void testGetDegree_ChordTypeDegreeIndex()
    {
        System.out.println("getDegree");
        ChordType.DegreeIndex di = null;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.getDegree(di);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of getBase method, of class ChordType.
     */
    // @Test
    public void testGetBase()
    {
        System.out.println("getBase");
        ChordType instance = null;
        String expResult = "";
        String result = instance.getBase();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of getDegree method, of class ChordType.
     */
    // @Test
    public void testGetDegree_int()
    {
        System.out.println("getDegree");
        int relPitch = 0;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.getDegree(relPitch);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDegree method, of class ChordType.
     */
    // @Test
    public void testGetDegree_DegreeNatural()
    {
        System.out.println("getDegree");
        Degree.Natural nd = null;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.getDegree(nd);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDegreeMostProbable method, of class ChordType.
     */
    // @Test
    public void testGetDegreeMostProbable()
    {
        System.out.println("getDegreeMostProbable");
        int relPitch = 0;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.getDegreeMostProbable(relPitch);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMostImportantDegreeIndexes method, of class ChordType.
     */
    // @Test
    public void testGetMostImportantDegreeIndexes()
    {
        System.out.println("getMostImportantDegreeIndexes");
        ChordType instance = null;
        List<ChordType.DegreeIndex> expResult = null;
        List<ChordType.DegreeIndex> result = instance.getMostImportantDegreeIndexes();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fitDegree method, of class ChordType.
     */
    // @Test
    public void testFitDegree()
    {
        System.out.println("fitDegree");
        Degree d = null;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.fitDegree(d);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fitDegreeAdvanced method, of class ChordType.
     */
    // @Test
    public void testFitDegreeAdvanced_Degree_StandardScaleInstance()
    {
        System.out.println("fitDegreeAdvanced");
        Degree d = null;
        StandardScaleInstance optScale = null;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.fitDegreeAdvanced(d, optScale);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fitDegreeAdvanced method, of class ChordType.
     */
    // @Test
    public void testFitDegreeAdvanced_ChordTypeDegreeIndex_StandardScaleInstance()
    {
        System.out.println("fitDegreeAdvanced");
        ChordType.DegreeIndex di = null;
        StandardScaleInstance optScale = null;
        ChordType instance = null;
        Degree expResult = null;
        Degree result = instance.fitDegreeAdvanced(di, optScale);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSimplified method, of class ChordType.
     */
    // @Test
    public void testGetSimplified()
    {
        System.out.println("getSimplified");
        int nbMaxDegrees = 0;
        ChordType instance = null;
        ChordType expResult = null;
        ChordType result = instance.getSimplified(nbMaxDegrees);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    // =========================================================================================================
    // Private methods
    // =========================================================================================================
//    private int getSimilarityScore(String strCt1, String strCt2, boolean acceptAbsentDegrees)
//    {
//        var ct1 = ctdb.getChordType(strCt1);
//        var ct2 = ctdb.getChordType(strCt2);
//        assert ct1 != null && ct2 != null : "ct1=" + ct1 + " ct2=" + ct2;
//        return ct1.getSimilarityScore(ct2, acceptAbsentDegrees);
//    }

}
