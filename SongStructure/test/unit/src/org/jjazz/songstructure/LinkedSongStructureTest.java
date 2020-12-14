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
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
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
    CLI_SectionImpl newSection1, newSection2, newSection3;
    CLI_SectionImpl section1, section2, section3;
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
        System.out.println("rdb=" + rdb);

    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp()
    {


        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        section1 = (CLI_SectionImpl) cls1.getSection(0);
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
            section3 = new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5);
            cls1.addSection(section3);
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Db"), new Position(7, 3f)));

            newChord = new CLI_ChordSymbolImpl(new ExtChordSymbol("A"), new Position(2, 1));
            newSection1 = new CLI_SectionImpl("NewSECTION1", TimeSignature.FOUR_FOUR, 4);
            newSection2 = new CLI_SectionImpl("NewSECTION2", TimeSignature.THREE_FOUR, 6);
            newSection3 = new CLI_SectionImpl("NewSECTION3", TimeSignature.FIVE_FOUR, 7);

            cls1.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(undoManager, cls1);

            SongStructureFactory sgsf = SongStructureFactory.getDefault();

            sgs = sgsf.createSgs(cls1, true);
            Rhythm r = null;
            try
            {
                r = rdb.getRhythmInstance(rdb.getDefaultRhythm(TimeSignature.THREE_FOUR));
            } catch (UnavailableRhythmException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            spt0 = new SongPartImpl(r, 8, 3, section2);
            sgs.addSongParts(Arrays.asList(spt0));

            // To compare after undo all
            u_sgs = sgsf.createSgs(cls1, false);        // Must be false to be the unchanged reference
            u_spt0 = new SongPartImpl(r, 8, 3, section2);
            u_sgs.addSongParts(Arrays.asList(u_spt0));
        } catch (ParseException ex)
        {
            throw new IllegalStateException("ParseException ex=" + ex);   //NOI18N
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
        redoAll();
        undoAll();
        System.out.println("\ncls after Undo ALL=" + cls1.toDumpString());
        System.out.println("\n  sgs after Undo ALL =" + sgs);
        System.out.println("u_sgs after Undo ALL =" + u_sgs);
        assertTrue(sgs.getSizeInBars() == u_sgs.getSizeInBars());   //NOI18N
        for (int i = 0; i < sgs.getSongParts().size(); i++)
        {
            SongPart spt1 = sgs.getSongParts().get(i);
            SongPart spt2 = u_sgs.getSongParts().get(i);
            assertTrue(spt1.getNbBars() == spt2.getNbBars());   //NOI18N
            assertTrue(spt1.getStartBarIndex() == spt2.getStartBarIndex());   //NOI18N
            assertTrue(spt1.getRhythm() == spt2.getRhythm());   //NOI18N
        }
    }

    @Test
    public void testAddAndRemove()
    {
        System.out.println("\n============ Test testAddAndRemove add chord symbol");
        assertTrue(sgs.getSizeInBars() == 11);   //NOI18N
        cls1.addItem(newChord);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 11);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 3);   //NOI18N
        System.out.println("\n== Test testAddAndRemove add section");
        try
        {
            cls1.addSection(newSection1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(cls1.getSection(newSection1.getData().getName()) != null);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getParentSection() == newSection1);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 1);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 2);   //NOI18N
        assertTrue(sgs.getSongParts().get(4).getNbBars() == 2);   //NOI18N
        System.out.println("\n== Test testAddAndRemove removeSection");
        assertTrue(sgs.getSizeInBars() == 10);   //NOI18N
        try
        {
            cls1.removeSection(section2);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 8);   //NOI18N
        assertTrue(sgs.getSongParts().size() == 3);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getParentSection() != section2);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getStartBarIndex() == 4);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 5);   //NOI18N
    }

    @Test
    public void testAdd2()
    {
        System.out.println("\n============ testAdd2");
        assertTrue(sgs.getSizeInBars() == 11);   //NOI18N
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
        assertTrue(sgs.getSizeInBars() == 8);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection1);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 7);   //NOI18N
    }

    @Test
    public void testAdd3()
    {
        System.out.println("\n============ testAdd3 after absent section");
        assertTrue(sgs.getSizeInBars() == 11);   //NOI18N
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
        assertTrue(sgs.getSizeInBars() == 10);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection2);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 8);   //NOI18N
    }

    @Test
    public void testAddAdaptedRhythm()
    {
        System.out.println("\n============ testAddAdaptedRhythm add section with new time signature => adapted rhythm");
        assertTrue(sgs.getSizeInBars() == 11);   //NOI18N
        try
        {
            cls1.addSection(newSection3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println("rdb=" + RhythmDatabase.getDefault().toString());
        System.out.println(" sgs after(1)=" + sgs);
        assertTrue(sgs.getSizeInBars() == 11);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection3);   //NOI18N
        Rhythm r = sgs.getSongParts().get(3).getRhythm();
        assertTrue(r instanceof AdaptedRhythm);   //NOI18N
        assertTrue(((AdaptedRhythm) r).getSourceRhythm() == sgs.getSongParts().get(2).getRhythm());   //NOI18N
    }


    @Test
    public void testChg()
    {
        System.out.println("\n============ testChg");
        try
        {
            cls1.setSectionTimeSignature(section2, TimeSignature.FOUR_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().get(1).getRhythm().getTimeSignature() == TimeSignature.FOUR_FOUR);   //NOI18N
    }

    @Test
    public void testMove()
    {
        System.out.println("\n============ testMove");
        try
        {
            cls1.moveSection(section2, 1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().get(1).getStartBarIndex() == 1);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getNbBars() == 4);   //NOI18N
    }

    @Test
    public void testMoveBig()
    {
        System.out.println("\n============ testMoveBig");
        try
        {
            cls1.moveSection(section2, 6);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 3);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 6);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 2);   //NOI18N
    }

    @Test
    public void testResize()
    {
        System.out.println("\n============ TestResize");
        try
        {
            cls1.setSize(10);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 13);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 5);   //NOI18N
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 10);   //NOI18N
    }

    @Test
    public void testRemoveSection()
    {
        System.out.println("\n============ Test testRemoveOneSection");
        try
        {
            cls1.removeSection(section2);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 2);   //NOI18N
        assertTrue(sgs.getSongParts().get(0).getNbBars() == 5);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getParentSection() == section3);   //NOI18N
        assertTrue(sgs.getSizeInBars() == 8);   //NOI18N
    }


    @Test
    public void testRemoveBars()
    {
        System.out.println("\n============ testRemoveBars");
        try
        {
            cls1.deleteBars(4, 5);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" cls1 after=" + cls1.toDumpString());
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 10);   //NOI18N
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 6);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 4);   //NOI18N
    }

    @Test
    public void testRemoveAdaptedRhythm()
    {
        System.out.println("\n============ testRemoveAdaptedRhythm");
        try
        {
            cls1.addSection(newSection3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertTrue(sgs.getSongParts().size() == 5);   //NOI18N
        try
        {
            cls1.deleteBars(6, 7);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" cls1 after=" + cls1.toDumpString());
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 4);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 1);   //NOI18N
    }

    @Test
    public void testRemoveAdaptedSourceRhythm()
    {
        System.out.println("\n============ testRemoveAdaptedSourceRhythm");
        try
        {
            cls1.addSection(newSection3);
            cls1.setSectionTimeSignature(section1, TimeSignature.TWO_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" cls1 after(1) =" + cls1.toDumpString());
        System.out.println(" sgs after(1) =" + sgs);

        assertTrue(sgs.getSongParts().size() == 5);   //NOI18N
        Rhythm r = sgs.getSongParts().get(2).getRhythm();
        boolean exceptionOccured = false;
        try
        {
            cls1.deleteBars(0, 5);
        } catch (UnsupportedEditException ex)
        {
            exceptionOccured = true;
        }
        System.out.println(" cls1 after (2) =" + cls1.toDumpString());
        System.out.println(" sgs after (2) =" + sgs);
        assertTrue(exceptionOccured);   //NOI18N
        assertTrue(sgs.getSongParts().get(2).getRhythm() == r);   //NOI18N
        assertTrue(sgs.getSongParts().size() == 5);   //NOI18N
    }

    @Test
    public void testChangeAdaptedSourceSection()
    {
        System.out.println("\n============ testChangeAdaptedSourceSection");
        try
        {
            cls1.addSection(newSection3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" cls1 after add newSection3=" + cls1.toDumpString());
        System.out.println(" sgs after add newSection3=" + sgs);
        assertTrue(sgs.getSongParts().size() == 5);   //NOI18N
        Rhythm r = sgs.getSongParts().get(2).getRhythm();
        boolean exceptionOccured = false;
        try
        {
            cls1.setSectionTimeSignature(section3, TimeSignature.TWO_FOUR);
            System.out.println(" cls1 after section3=2/4=" + cls1.toDumpString());
            System.out.println(" sgs after section3=2/4=" + sgs);
            cls1.setSectionTimeSignature(section1, TimeSignature.TWO_FOUR);
        } catch (UnsupportedEditException ex)
        {
            exceptionOccured = true;
        }
        System.out.println(" cls1 after section1=2/4=" + cls1.toDumpString());
        System.out.println(" sgs after section1=2/4=" + sgs);
        assertTrue(exceptionOccured);   //NOI18N
        assertTrue(sgs.getSongParts().get(0).getRhythm() == r);   //NOI18N
        assertTrue(sgs.getSongParts().size() == 5);   //NOI18N
    }

    private void redoAll()
    {
        while (undoManager.canRedo())
        {
            undoManager.redo();
        }
    }

    private void undoAll()
    {
        while (undoManager.canUndo())
        {
            undoManager.undo();
        }
    }

    private List<SongPart> ml(SongPart spt)
    {
        ArrayList<SongPart> l = new ArrayList<>();
        l.add(spt);
        return l;
    }

    private SmallMap<SongPart, SongPart> msm(SongPartImpl rp1, SongPartImpl rp2)
    {
        SmallMap<SongPart, SongPart> sm = new SmallMap<>();
        sm.putValue(rp1, rp2);
        return sm;
    }
}
