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
package org.jjazz.songstructure;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_ChordSymbolImpl;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.SmallMap;
import org.junit.*;
import static org.junit.Assert.assertTrue;
import org.openide.util.Exceptions;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

public class LinkedSongStructureTest
{

    ChordLeadSheetImpl cls1;
    CLI_ChordSymbolImpl newChord;
    CLI_SectionImpl newSection1, newSection2;
    CLI_SectionImpl section2;
    SongStructure sgs;
    SongStructure u_sgs;
    static RhythmDatabase rdb;
    SongPartImpl spt0;
    SongPartImpl spt1, spt2, spt3, spt4;
    SongPart u_spt0;
    SongPart u_spt1, u_spt2, u_spt3, u_spt4;
    JJazzUndoManager undoManager;

    public LinkedSongStructureTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        rdb = RhythmDatabase.getDefault();
        System.out.println("rdb-=" + rdb);
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp()
    {
        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        undoManager = new JJazzUndoManager();
        try
        {
            cls1.setSize(8);
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Dm7"), new Position(0, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("F#7"), new Position(1, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Bbmaj7#5"), new Position(1, 3)));
            section2 = new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2);
            cls1.addSection(section2);
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("D7b9b5"), new Position(2, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("FM7#11"), new Position(4, 1)));
            cls1.addSection(new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Db"), new Position(7, 3f)));

            newChord = new CLI_ChordSymbolImpl(new ExtChordSymbol("A"), new Position(2, 1));
            newSection1 = new CLI_SectionImpl("NewSECTION1", TimeSignature.FOUR_FOUR, 4);
            newSection2 = new CLI_SectionImpl("NewSECTION2", TimeSignature.THREE_FOUR, 6);

            cls1.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(undoManager, cls1);

            SongStructureFactory sgsf = SongStructureFactory.getDefault();

            sgs = sgsf.createSgs(cls1, true);
            Rhythm r = rdb.getDefaultRhythm(TimeSignature.THREE_FOUR);
            spt0 = new SongPartImpl(r, 8, 3, section2);
            sgs.addSongParts(Arrays.asList(spt0));

            // To compare after undo all
            u_sgs = sgsf.createSgs(cls1, true);
            u_spt0 = new SongPartImpl(r, 8, 3, section2);
            u_sgs.addSongParts(Arrays.asList(u_spt0));
        } catch (ParseException ex)
        {
            throw new IllegalStateException("ParseException ex=" + ex);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        sgs.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(undoManager, sgs);

        System.out.println("\n ==== SETUP cls1=" + cls1.toDumpString());
        System.out.println(" ==== SETUP   sgs Before=" + sgs);
        System.out.println(" ==== SETUP u_sgs Before=" + u_sgs);

        undoManager.startCEdit("UT-edit");
    }

    @After
    public void tearDown()
    {
        undoManager.endCEdit("UT-edit");
        undoAll();
        System.out.println("\n  cls after Undo ALL=" + cls1.toDumpString());
        System.out.println("\n  sgs after Undo ALL =" + sgs);
        System.out.println("u_sgs after Undo ALL =" + u_sgs);
        assertTrue(sgs.getSizeInBars() == u_sgs.getSizeInBars());
        for (int i = 0; i < sgs.getSongParts().size(); i++)
        {
            SongPart spt1 = sgs.getSongParts().get(i);
            SongPart spt2 = u_sgs.getSongParts().get(i);
            assertTrue(spt1.getNbBars() == spt2.getNbBars());
            assertTrue(spt1.getStartBarIndex() == spt2.getStartBarIndex());
            assertTrue(spt1.getRhythm() == spt2.getRhythm());
        }
    }

    @Test
    public void testAddAndRemove()
    {
        System.out.println("\n============ Test AddChord");
        assertTrue(sgs.getSizeInBars() == 11);
        cls1.addItem(newChord);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 11);
        System.out.println("\n============ Test AddSection with impact on size");
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 3);
        try
        {
            cls1.addSection(newSection1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(cls1.getSection("NewSECTION1") != null);
        assertTrue(sgs.getSongParts().get(2).getParentSection() == newSection1);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 1);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 2);
        assertTrue(sgs.getSongParts().get(4).getNbBars() == 2);
        System.out.println("\n============ Test RemoveSection");
        assertTrue(sgs.getSizeInBars() == 10);
        cls1.removeSection(section2);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 8);
        assertTrue(sgs.getSongParts().size() == 3);
        assertTrue(sgs.getSongParts().get(1).getParentSection() != section2);
        assertTrue(sgs.getSongParts().get(1).getStartBarIndex() == 4);
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 5);
    }

    @Test
    public void testAdd2()
    {
        System.out.println("\n============ Test AddSection2");
        assertTrue(sgs.getSizeInBars() == 11);
        try
        {
            sgs.removeSongParts(ml(sgs.getSongParts().get(1)));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after(1)=" + sgs);
        try
        {
            cls1.addSection(newSection1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after(2)=" + sgs);
        assertTrue(sgs.getSizeInBars() == 8);
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection1);
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 7);
    }

    @Test
    public void testAdd3()
    {
        System.out.println("\n============ Test AddSection after absent section");
        assertTrue(sgs.getSizeInBars() == 11);
        try
        {
            sgs.removeSongParts(ml(sgs.getSongParts().get(2)));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after(1)=" + sgs);
        try
        {
            cls1.addSection(newSection2);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after(2)=" + sgs);
        assertTrue(sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection2);
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 8);
    }

    @Test
    public void testChg()
    {
        System.out.println("\n============ Test section change");
        try
        {
            cls1.setSectionTimeSignature(section2, TimeSignature.FOUR_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().get(1).getRhythm().getTimeSignature() == TimeSignature.FOUR_FOUR);
    }

    @Test
    public void testMove()
    {
        System.out.println("\n============ Test move small");
        cls1.moveSection(section2, 1);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().get(1).getStartBarIndex() == 1);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);
        assertTrue(sgs.getSongParts().get(3).getNbBars() == 4);
    }

    @Test
    public void testMoveBig()
    {
        System.out.println("\n============ Test move big");
        cls1.moveSection(section2, 6);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 3);
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 6);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 2);
    }

    @Test
    public void testResize()
    {
        System.out.println("\n============ Testresize");
        cls1.setSize(10);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 13);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 5);
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 10);
    }

    @Test
    public void testRemoveBars()
    {
        System.out.println("\n============ testRemoveBars");
        cls1.deleteBars(4, 5);
        System.out.println(" cls1 after=" + cls1.toDumpString());
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 6);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 4);
    }

    private void undoAll()
    {
        while (undoManager.canUndo())
        {
            undoManager.undo();
        }
    }

    private List<SongPart> ml(SongPart rp)
    {
        ArrayList<SongPart> l = new ArrayList<>();
        l.add(rp);
        return l;
    }

    private SmallMap<SongPart, SongPart> msm(SongPartImpl rp1, SongPartImpl rp2)
    {
        SmallMap<SongPart, SongPart> sm = new SmallMap<>();
        sm.putValue(rp1, rp2);
        return sm;
    }
}
