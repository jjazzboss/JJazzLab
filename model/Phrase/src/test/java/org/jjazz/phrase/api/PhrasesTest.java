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
package org.jjazz.phrase.api;

import java.text.ParseException;
import java.util.Locale;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Phrase related unit tests.
 */
public class PhrasesTest
{

    public PhrasesTest()
    {
    }

    @BeforeAll
    public static void setUpClass() throws Exception
    {
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "(p=%2$.1f d=%3$.1f)");
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }

    @BeforeEach
    public void setUp() throws Exception
    {
    }

    @AfterEach
    public void tearDown() throws Exception
    {
    }

    /**
     * Test of fixOverlappedNotes method, of class Phrases.
     */
    @Test
    public void testFixOverlappedNotes()
    {
        System.out.println("testFixOverlappedNotes");
        Phrase p1 = new Phrase(0);
        Phrase p2 = new Phrase(0);

        p1.add(new NoteEvent(50, 2, 64, 0));         // (pitch, dur, velo, pos)
        p1.add(new NoteEvent(50, 1, 64, 0));
        p2.add(new NoteEvent(50, 1, 64, 0));

        p1.add(new NoteEvent(50, 3, 64, 1));
        p2.add(new NoteEvent(50, 3, 64, 1));

        p1.add(new NoteEvent(50, 1, 64, 1.5f));

        p1.add(new NoteEvent(50, 5, 64, 5));
        p2.add(new NoteEvent(50, 1, 64, 5));

        p1.add(new NoteEvent(50, 5, 64, 6));
        p2.add(new NoteEvent(50, 1.5f, 64, 6));

        p1.add(new NoteEvent(50, 1, 64, 7));

        p1.add(new NoteEvent(50, 10, 64, 7.5f));
        p2.add(new NoteEvent(50, 10, 64, 7.5f));

        p1.add(new NoteEvent(50, 1, 64, 20f));
        p1.add(new NoteEvent(50, 2, 64, 20f));
        p2.add(new NoteEvent(50, 2, 64, 20f));


        System.out.println("# p1=\n" + Utilities.toMultilineString(p1.getNotes()));

        Phrases.fixOverlappedNotes(p1);

        System.out.println("\n# p1=\n" + Utilities.toMultilineString(p1.getNotes()));
        System.out.println("\n# p2=\n" + Utilities.toMultilineString(p2.getNotes()));
        assertTrue(p1.equalsAsNoteNearPosition(p2, 0));
    }

    /**
     * Test loadAsString()/saveAsString()
     */
    @Test
    public void testSaveLoadAsString() throws ParseException
    {
        System.out.println("testSaveLoadAsString");

        Phrase p = new Phrase(0);
        p.add(new NoteEvent(50, 2.2f, 34, 0.9999f));         // (pitch, dur, velo, pos)
        p.add(new NoteEvent(57, 12f, 79, 3f));         // (pitch, dur, velo, pos)
        p.add(new NoteEvent(0, 0.001f, 127, 4.5f));         // (pitch, dur, velo, pos)
        p.add(new NoteEvent(60, .883f, 0, 2072.655f));         // (pitch, dur, velo, pos)

        Phrase pSave = p.clone();
        String s = Phrase.saveAsString(p);
        p = Phrase.loadAsString(s);

        assertTrue(p.equalsAsNoteNearPosition(pSave, 0));
    }

}
