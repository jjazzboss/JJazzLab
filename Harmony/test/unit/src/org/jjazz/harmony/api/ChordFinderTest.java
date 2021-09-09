/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.harmony.api;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openide.util.Exceptions;

/**
 *
 * @author Jerome
 */
public class ChordFinderTest
{

    private Map<String, List<Integer>> testDatabase = new HashMap<>();

    public ChordFinderTest()
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

        addTestData("C Eb G Bb");
//        addTestData("C E G A");
//        addTestData("A C E G");
//        addTestData("Eb G Bb Db");
//        addTestData("C E G");
//        addTestData("F# A# C#");
//        addTestData("F Bb D");
//        addTestData("A C Eb");
//        addTestData("C Eb Gb");
//        addTestData("C Eb Gb G");       // don't exist

    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of find method, of class ChordFinder.
     */
    @Test
    public void testFind()
    {
        System.out.println("testFind()");
        ChordFinder instance = new ChordFinder();
        for (var strTest : testDatabase.keySet())
        {
            List<Integer> data = testDatabase.get(strTest);
            List<ChordSymbol> res = instance.find(data);
            System.out.println(strTest + " " + data + " => " + res);
        }
    }


    private void addTestData(String s)
    {
        List<Integer> res = new ArrayList<>();
        String[] strs = s.split(" ");
        for (String str : strs)
        {
            try
            {
                res.add(new Note(str).getPitch());
            } catch (ParseException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
        testDatabase.put(s, res);
    }
}
