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
package org.jjazz.songstructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.SmallMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Exceptions;

public class SongStructureImplTest
{

    SongStructure sgs;
    SongStructure u_sgs;
    static DefaultRhythmDatabase rdb;
    SongPartImpl spt0;
    SongPartImpl spt1, spt2, spt3, spt4;
    SongPart u_spt0;
    SongPart u_spt1, u_spt2, u_spt3, u_spt4;
    Rhythm r44_1, r44_2;
    Rhythm r34;
    JJazzUndoManager undoManager;

    public SongStructureImplTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        rdb = (DefaultRhythmDatabase) RhythmDatabase.getDefault();
        rdb.addRhythmsFromRhythmProviders(false, true, false);
        System.out.println(rdb.toStatsString());
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
            JJazzUndoManagerFinder.getDefault().put(sgs, undoManager);


            r44_1 = rdb.getRhythmInstance(rdb.getRhythms().get(0));
            r44_2 = rdb.getRhythmInstance(rdb.getRhythms().get(1));
            r34 = rdb.getRhythmInstance(rdb.getDefaultRhythm(TimeSignature.THREE_FOUR));


            spt0 = new SongPartImpl(r44_1, 0, 10, null);
            spt1 = new SongPartImpl(r44_1, 10, 4, null);
            spt2 = new SongPartImpl(r44_1, 14, 6, null);
            spt3 = new SongPartImpl(r34, 10, 12, null);
            spt4 = new SongPartImpl(r44_1, 30, 8, null);


            u_spt0 = spt0.getCopy(spt0.getRhythm(), spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
            u_spt1 = spt1.getCopy(spt1.getRhythm(), spt1.getStartBarIndex(), spt1.getNbBars(), spt0.getParentSection());
            u_spt2 = spt2.getCopy(spt2.getRhythm(), spt2.getStartBarIndex(), spt2.getNbBars(), spt0.getParentSection());
            u_spt3 = spt3.getCopy(spt3.getRhythm(), spt3.getStartBarIndex(), spt3.getNbBars(), spt0.getParentSection());
            u_spt4 = spt4.getCopy(spt4.getRhythm(), spt4.getStartBarIndex(), spt4.getNbBars(), spt0.getParentSection());


            sgs.addSongParts(Arrays.asList(spt0, spt1, spt2));


            System.out.println("\n ==== SETUP sgs Before=" + sgs);
            undoManager.startCEdit("UT-edit");


            // Copy to make the undo test
            u_sgs = new SongStructureImpl();
            u_sgs.addSongParts(Arrays.asList(u_spt0, u_spt1, u_spt2));

        } catch (UnsupportedEditException | UnavailableRhythmException ex)
        {
            Exceptions.printStackTrace(ex);
        }

    }

    @After
    public void tearDown()
    {
        undoManager.endCEdit("UT-edit");

        undoAll();
        redoAll();
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
        Rhythm r54 = null;
        try
        {
            r54 = rdb.getRhythmInstance(rdb.getDefaultRhythm(TimeSignature.FIVE_FOUR));
        } catch (UnavailableRhythmException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        
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
        System.out.println("after replace sp1=>spt3 sgs=" + sgs);
        assertTrue(sgs.getSizeInBars() == 20);
        assertTrue(sgs.getSongParts().get(1) == spt3);
        Rhythm r = r44_2;
        SongPartImpl newSpt = (SongPartImpl) spt0.getCopy(r, spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
        try
        {
            sgs.replaceSongParts(Arrays.asList((SongPart) spt0), Arrays.asList((SongPart) newSpt));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println("after replace spt0=>newSpt sgs=" + sgs);
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

    private Map<SongPart, Integer> msm(SongPartImpl rp, Integer i)
    {
        Map<SongPart, Integer> sm = new HashMap<>();
        sm.put(rp, i);
        return sm;
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
}
