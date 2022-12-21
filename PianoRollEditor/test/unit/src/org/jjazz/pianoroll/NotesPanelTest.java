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
package org.jjazz.pianoroll;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.KeyboardRange;
import org.jjazz.util.api.FloatRange;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.jupiter.api.Disabled;

/**
 *
 * @author Jerome
 */
public class NotesPanelTest
{

    PianoRollEditorImpl editor;
    KeyboardComponent keyboard;
    NoteEvent ne1, ne2;
    int neHash1, neHash2;
    NoteView nv1, nv2;
    NotesPanel notesPanel;

    public NotesPanelTest()
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
        keyboard = new KeyboardComponent(KeyboardRange._128_KEYS);

        SizedPhrase sp = new SizedPhrase(0, new FloatRange(0, 4), TimeSignature.FOUR_FOUR);
        ne1 = new NoteEvent(64, 1, 64, 1.6f);
        ne2 = new NoteEvent(64, 1, 64, 1.6f);
        neHash1 = System.identityHashCode(ne1);
        neHash2 = System.identityHashCode(ne2);
        sp.add(ne1);
        sp.add(ne2);

        editor = new PianoRollEditorImpl(0, sp, null, PianoRollEditorSettings.getDefault());

        notesPanel = new NotesPanel(editor, keyboard);
        nv1 = notesPanel.addNoteView(ne1);
        nv2 = notesPanel.addNoteView(ne2);
    }

    @After
    public void tearDown()
    {
    }
    
    /**
     * Test of addNoteView method, of class NotesPanel.
     */
    @Disabled
    public void testRemoveNoteView()
    {
        System.out.println("reomveNoteView");
        assertEquals(notesPanel.removeNoteView(ne2), nv2);
        assertEquals(notesPanel.removeNoteView(ne1), nv1);
        assertTrue(nv1.compareTo(ne1)==0);
        assertTrue(nv1.compareTo(ne2)!=0);
        assertTrue(nv2.compareTo(ne1)!=0);
        assertTrue(nv2.compareTo(ne2)==0);
        
    }


    /**
     * Test of replaceNoteViewModel method, of class NotesPanel.
     */
    @Disabled
    public void testReplaceNoteViewModel()
    {
        System.out.println("replaceNoteViewModel");
        NoteEvent oldNe = null;
        NoteEvent newNe = null;
        NotesPanel instance = null;
//        instance.replaceNoteViewModel(oldNe, newNe);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }



}
