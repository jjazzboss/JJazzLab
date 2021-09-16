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
public class ChordSymbolFinderTest
{

    private Map<String, List<Note>> testDatabase = new HashMap<>();

    public ChordSymbolFinderTest()
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
        ChordSymbolFinder.buildStaticData();
        addTestData("C Eb G Bb");
        addTestData("G Bb C Eb");
        addTestData("G Bb C D Eb");
        addTestData("C E G A");
        addTestData("A C E G");
        addTestData("G A Bb D E");
        addTestData("A Bb D E G");
        addTestData("G Bb Db F");
        addTestData("C E G");
        addTestData("G E C");
        addTestData("F# A# C#");
        addTestData("F Bb D");
        addTestData("F Bb Db");
        addTestData("A C Eb");
        addTestData("C Eb A");
        addTestData("C Eb Gb");
        addTestData("C Eb Gb Bb");
        addTestData("Gb Bb C Eb");
        addTestData("C Eb Gb G");       // don't exist
        addTestData("F# G# A# C# F");
        addTestData("F A Bb D E");
        addTestData("Eb F A C D");


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
        System.out.println("\n\ntestFind() ========================");
        ChordSymbolFinder instance = new ChordSymbolFinder();
        for (var strTest : testDatabase.keySet())
        {
            System.out.print("testFind() strTest=" + strTest + " => ");
            List<Note> data = testDatabase.get(strTest);
            testPitches(instance, data);
        }
    }


    @Test
    public void testAllChordTypes()
    {
        System.out.println("\n\ntestAllChordTypes() ========================");
        ChordSymbolFinder instance = new ChordSymbolFinder();
        for (ChordType ct : ChordTypeDatabase.getInstance().getChordTypes())
        {
            int rootNote = (int) Math.round(Math.random() * 11);
            ChordSymbol cs = new ChordSymbol(new Note(rootNote), ct);
            Chord chord = cs.getChord();
            if (chord.size() >= 3 && chord.size() <= ChordSymbolFinder.maxNotes)
            {
                System.out.print("testPitches() chord=" + chord.getPitches() + " => ");
                testPitches(instance, chord.getNotes());
            }
        }
    }

    private void testPitches(ChordSymbolFinder instance, List<Note> data)
    {
        long startTime = System.nanoTime();
        var chordSymbols = instance.find(data);
        if (chordSymbols != null)
        {
            var cs1 = instance.getChordSymbol(data, chordSymbols, false);
            var cs2 = instance.getChordSymbol(data, chordSymbols, true);
            double duration = (System.nanoTime() - startTime) / 1000000d;
            System.out.println(chordSymbols + " " + cs1 + " or " + cs2 + "    (dur=" + duration + "ms)");
        } else
        {
            System.out.println("empty");
        }
    }

    private void addTestData(String s)
    {
        List<Note> res = new ArrayList<>();
        String[] strs = s.split(" ");
        for (String str : strs)
        {
            try
            {
                res.add(new Note(str));
            } catch (ParseException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
        testDatabase.put(s, res);
    }
}
