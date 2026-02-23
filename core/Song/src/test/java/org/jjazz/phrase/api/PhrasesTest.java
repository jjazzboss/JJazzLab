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

import com.google.common.collect.ListMultimap;
import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.function.Predicate;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.Chord;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.Utilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author Jerome
 */
public class PhrasesTest
{

    public PhrasesTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "(p=%2$.1f d=%3$.1f)");
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp() throws Exception
    {
    }

    @After
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

    /**
     * Test of getChord method, of class Phrases.
     */
    // @Test
    public void testGetChord()
    {
        System.out.println("getChord");
        Phrase p = null;
        Chord expResult = null;
        Chord result = Phrases.getChord(p);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of silenceAfter method, of class Phrases.
     */
    // @Test
    public void testSilenceAfter()
    {
        System.out.println("silenceAfter");
        Phrase p = null;
        float posInBeats = 0.0F;
        Phrases.silenceAfter(p, posInBeats);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSlice method, of class Phrases.
     */
    // @Test
    public void testGetSlice()
    {
        System.out.println("getSlice");
        Phrase p = null;
        FloatRange range = null;
        boolean keepLeft = false;
        int cutRight = 0;
        float beatWindow = 0.0F;
        Phrase expResult = null;
        Phrase result = Phrases.getSlice(p, range, keepLeft, cutRight, beatWindow);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of silence method, of class Phrases.
     */
    // @Test
    public void testSilence()
    {
        System.out.println("silence");
        Phrase p = null;
        FloatRange range = null;
        boolean cutLeft = false;
        boolean keepRight = false;
        float beatWindow = 0.0F;
        Phrases.silence(p, range, cutLeft, keepRight, beatWindow);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCrossingNotes method, of class Phrases.
     */
    // @Test
    public void testGetCrossingNotes()
    {
        System.out.println("getCrossingNotes");
        Phrase p = null;
        float posInBeats = 0.0F;
        boolean strict = false;
        List<NoteEvent> expResult = null;
        List<NoteEvent> result = Phrases.getCrossingNotes(p, posInBeats, strict);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNotesByPitch method, of class Phrases.
     */
    // @Test
    public void testGetNotesByPitch()
    {
        System.out.println("getNotesByPitch");
        Phrase p = null;
        Predicate<NoteEvent> tester = null;
        ListMultimap<Integer, NoteEvent> expResult = null;
        ListMultimap<Integer, NoteEvent> result = Phrases.getNotesByPitch(p, tester);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of importPhrase method, of class Phrases.
     */
    // @Test
    public void testImportPhrase() throws Exception
    {
        System.out.println("importPhrase");
        File midiFile = null;
        int channel = 0;
        boolean isDrums = false;
        boolean strictChannel = false;
        boolean notifyUserIfNoChannelNotes = false;
        Phrase expResult = null;
        Phrase result = Phrases.importPhrase(midiFile, channel, isDrums, strictChannel, notifyUserIfNoChannelNotes);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPhrases method, of class Phrases.
     */
    // @Test
    public void testGetPhrases()
    {
        System.out.println("getPhrases");
        int tracksPPQ = 0;
        Track[] tracks = null;
        Integer[] channels = null;
        List<Phrase> expResult = null;
        List<Phrase> result = Phrases.getPhrases(tracksPPQ, tracks, channels);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
