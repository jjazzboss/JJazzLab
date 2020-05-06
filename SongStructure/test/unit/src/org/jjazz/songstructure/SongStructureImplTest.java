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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.SmallMap;
import static org.junit.Assert.assertTrue;
import org.junit.*;
import org.openide.util.Exceptions;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

public class SongStructureImplTest
{

    SongStructure sgs;
    SongStructure u_sgs;
    static RhythmDatabase rdb;
    SongPartImpl spt0;
    SongPartImpl spt1, spt2, spt3, spt4;
    SongPart u_spt0;
    SongPart u_spt1, u_spt2, u_spt3, u_spt4;
    JJazzUndoManager undoManager;

    public SongStructureImplTest()
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
        try
        {
            undoManager = new JJazzUndoManager();
            sgs = new SongStructureImpl();
            sgs.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(undoManager, sgs);
            Rhythm r44 = rdb.getDefaultRhythm(TimeSignature.FOUR_FOUR);
            Rhythm r34 = rdb.getDefaultRhythm(TimeSignature.THREE_FOUR);
            spt0 = new SongPartImpl(r44, 0, 10, null);
            spt1 = new SongPartImpl(r44, 10, 4, null);
            spt2 = new SongPartImpl(r44, 14, 6, null);
            spt3 = new SongPartImpl(r34, 10, 12, null);
            spt4 = new SongPartImpl(r44, 30, 8, null);
            u_spt0 = spt0.clone(spt0.getRhythm(), spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
            u_spt1 = spt1.clone(spt1.getRhythm(), spt1.getStartBarIndex(), spt1.getNbBars(), spt0.getParentSection());
            u_spt2 = spt2.clone(spt2.getRhythm(), spt2.getStartBarIndex(), spt2.getNbBars(), spt0.getParentSection());
            u_spt3 = spt3.clone(spt3.getRhythm(), spt3.getStartBarIndex(), spt3.getNbBars(), spt0.getParentSection());
            u_spt4 = spt4.clone(spt4.getRhythm(), spt4.getStartBarIndex(), spt4.getNbBars(), spt0.getParentSection());

            sgs.addSongParts(Arrays.asList(spt0, spt1, spt2));
            System.out.println("\n ==== SETUP sgs Before=" + sgs);
            undoManager.startCEdit("UT-edit");

            // Copy to make the undo test
            u_sgs = new SongStructureImpl();
            u_sgs.addSongParts(Arrays.asList(u_spt0, u_spt1, u_spt2));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @After
    public void tearDown()
    {
        undoManager.endCEdit("UT-edit");
        undoAll();
        System.out.println("sgs after Undo=" + sgs);
        assertTrue(sgs.getSizeInBars() == u_sgs.getSizeInBars());
        for (int i = 0; i < 2; i++)
        {
            SongPart spt1 = sgs.getSongParts().get(i);
            SongPart spt2 = u_sgs.getSongParts().get(i);
            assertTrue(spt1.getNbBars() == spt2.getNbBars());
            assertTrue(spt1.getStartBarIndex() == spt2.getStartBarIndex());
            assertTrue(spt1.getRhythm() == spt2.getRhythm());
            // TODO : check RhythmParameters'valueProfiles
        }
    }

    /**
     * Test of addSongPart method, of class SongStructure.
     */
    @Test
    public void testAddSongPart()
    {
        System.out.println("\n============ Test addSongPart");
        assertTrue(sgs.getSizeInBars() == 20);
        assertTrue(sgs.getSongParts().get(0) == spt0 && sgs.getSongParts().get(1) == spt1);
        assertTrue(spt0.getStartBarIndex() == 0 && spt1.getStartBarIndex() == 10);
        Rhythm r54 = rdb.getDefaultRhythm(TimeSignature.FIVE_FOUR);
        SongPartImpl sptX = new SongPartImpl(r54, 0, 1, null);
        try
        {
            sgs.addSongParts(Arrays.asList(sptX));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println("sgs=" + sgs);
        assertTrue(spt0.getStartBarIndex() == 1 && spt1.getStartBarIndex() == 11);
        assertTrue(sgs.getSizeInBars() == 21);
    }

    /**
     * Test of removeSongParts method, of class SongStructure.
     */
    @Test
    public void testRemoveSongPart()
    {
        System.out.println("\n============ Test removeSongPart");
        try
        {
            sgs.removeSongParts(ml(spt0));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println("sgs=" + sgs);
        assertTrue(sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(0) == spt1 && sgs.getSongParts().get(1) == spt2);
        assertTrue(spt1.getStartBarIndex() == 0 && spt2.getStartBarIndex() == 4);
    }

    /**
     * Test of resizeSongParts method, of class SongStructure.
     */
    @Test
    public void testResizeSongPart()
    {
        System.out.println("\n============ Test resizeSongPart");
        sgs.resizeSongParts(msm(spt1, 1));
        System.out.println("sgs=" + sgs);
        assertTrue(sgs.getSizeInBars() == 17 && spt1.getNbBars() == 1);
        assertTrue(spt2.getStartBarIndex() == 11);
    }

    /**
     * Test of replaceSongParts method, of class SongStructure.
     */
    @Test
    public void testReplaceSongPart()
    {
        System.out.println("\n============ Test replaceSongPart");
        spt3.setStartBarIndex(spt1.getStartBarIndex());
        spt3.setNbBars(spt1.getNbBars());
        try
        {
            sgs.replaceSongParts(Arrays.asList((SongPart) spt1), Arrays.asList((SongPart) spt3));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println("sgs=" + sgs);
        assertTrue(sgs.getSizeInBars() == 20);
        assertTrue(sgs.getSongParts().get(1) == spt3);
        Rhythm r = rdb.getNextRhythm(spt0.getRhythm());
        SongPartImpl newSpt = (SongPartImpl) spt0.clone(r, spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
        try
        {
            sgs.replaceSongParts(Arrays.asList((SongPart) spt0), Arrays.asList((SongPart) newSpt));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println("sgs=" + sgs);
        System.out.println("spt0=" + spt0.toDumpString() + "\nnewSpt=" + newSpt.toDumpString());
        assertTrue(sgs.getSongParts().get(0) == newSpt && !sgs.getSongParts().contains(spt0));
        assertTrue(sgs.getSizeInBars() == 20);
    }

    /**
     * Test of findSongPart method, of class SongStructure.
     */
    @Test
    public void testFindSongPart()
    {
        System.out.println("\n============ Test findSongPart");
        SongPart spt = sgs.getSongPart(0);
        assertTrue(spt == spt0);
        spt = sgs.getSongPart(10);
        assertTrue(spt == spt1);
        spt = sgs.getSongPart(15);
        assertTrue(spt == spt2);
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

    private SmallMap<SongPart, Integer> msm(SongPartImpl rp, Integer i)
    {
        SmallMap<SongPart, Integer> sm = new SmallMap<>();
        sm.putValue(rp, i);
        return sm;
    }

    private void undoAll()
    {
        while (undoManager.canUndo())
        {
            undoManager.undo();
        }
    }
}
