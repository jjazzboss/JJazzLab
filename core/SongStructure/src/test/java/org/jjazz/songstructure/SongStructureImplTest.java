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

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SgsVetoableChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import org.openide.util.Exceptions;

public class SongStructureImplTest
{

    private static final String UNDO_EDIT = "UT-edit";
    ChordLeadSheet cls;
    Rhythm r54, r44, r44bis, r34, r34bis;
    CLI_Section sectionA_44, sectionB_34, sectionC_44;
    CLI_ChordSymbol cs1, cs2;
    SongStructure sgs;
    SongStructure u_sgs;
    static DefaultRhythmDatabase rdb;
    SongPart spt0;
    SongPart spt1, spt2;
    JJazzUndoManager undoManager;


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
    public void setUp() throws UnsupportedEditException, ParseException
    {
        undoManager = new JJazzUndoManager();

        // Build a 16 bars chordleadsheet [0-15]
        // bar 0: SectionA 4/4
        // bar 4: SectionB 3/4
        // bar 8: SectionC 4/4
        cls = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet("SectionA", TimeSignature.FOUR_FOUR, 16, "C7");
        cs1 = cls.getItems(CLI_ChordSymbol.class).get(0); // C7 at bar 0 beat 0
        sectionA_44 = cls.getSection(0);
        sectionB_34 = (CLI_Section) sectionA_44.getCopy(new Section("SectionB", TimeSignature.THREE_FOUR), new Position(4));
        cls.addSection(sectionB_34);
        sectionC_44 = (CLI_Section) sectionA_44.getCopy(new Section("SectionC", TimeSignature.FOUR_FOUR), new Position(8));
        cls.addSection(sectionC_44);
        cs2 = (CLI_ChordSymbol) cs1.getCopy(ExtChordSymbol.get("Dm"), sectionB_34.getPosition().getMoved(1, 1));    // Dm at bar 5, beat 1
        cls.addItem(cs2);


        // Build a SongStructure from chordleadsheet => create 3 song parts, one per section
        sgs = SongStructureFactory.getDefault().createSgs(cls);
        sgs.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(sgs, undoManager);

        var spts = sgs.getSongParts();
        spt0 = spts.get(0);     // 4/4 rhythm        
        spt1 = spts.get(1);     // 3/4 rhythm
        spt2 = spts.get(2);     // 4/4 rhythm

        r44 = spt0.getRhythm();
        r34 = spt1.getRhythm();

        // Other rhythm instance to be used in tests
        try
        {
            r54 = rdb.getRhythmInstance(rdb.getDefaultRhythm(TimeSignature.FIVE_FOUR));

            var r44All = rdb.getRhythms(TimeSignature.FOUR_FOUR);
            var ri44bis = r44All.stream().filter(ri -> ri != rdb.getRhythm(r44.getUniqueId())).toList().get(0);
            r44bis = rdb.getRhythmInstance(ri44bis);

            var r34All = rdb.getRhythms(TimeSignature.THREE_FOUR);
            var ri34bis = r34All.stream().filter(ri -> ri != rdb.getRhythm(r34.getUniqueId())).toList().get(0);
            r34bis = rdb.getRhythmInstance(ri34bis);
        } catch (UnavailableRhythmException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        // Copy for the test after undo in tearDown()
        u_sgs = sgs.getDeepCopy(cls);

        undoManager.startCEdit(UNDO_EDIT);
    }

    @After
    public void tearDown()
    {
        if (undoManager.getCurrentCEditName() == null)
        {
            return;
        }
        undoManager.endCEdit(UNDO_EDIT);

        undoAll();
        redoAll();
        undoAll();

        boolean b = equals(sgs, u_sgs);
        if (!b)
        {
            System.out.println("==== MISMATCH AFTER UNDO SEQUENCE:");
            System.out.println("sgs after Undo=" + sgs);
            System.out.println("u_sgs after Undo=" + u_sgs);
            assertTrue(b);
        }
    }

    @Test
    public void testRemoveSongPart()
    {
        sgs.removeSongParts(List.of(spt0));
        assertEquals(12, sgs.getSizeInBars());
        assertSame(spt1, sgs.getSongPart(0));
        assertSame(spt2, sgs.getSongPart(4));
    }

    // -----------------------------------------------------------------------------------------
    // Additional tests
    // -----------------------------------------------------------------------------------------

    @Test
    public void testContainerSetAndClearedOnRemoveAndUndoRedo()
    {
        // Initial containers
        assertSame(sgs, spt0.getContainer());
        assertSame(sgs, spt1.getContainer());
        assertSame(sgs, spt2.getContainer());

        sgs.removeSongParts(List.of(spt0));
        assertNull("Removed SongPart container must be cleared", spt0.getContainer());
        assertSame("Remaining SongPart must keep container", sgs, spt1.getContainer());
        assertSame("Remaining SongPart must keep container", sgs, spt2.getContainer());

        // Undo should restore part and container
        undoManager.endCEdit(UNDO_EDIT);
        assertTrue(undoManager.canUndo());
        undoManager.undo();
        assertSame(spt0, sgs.getSongPart(0));
        assertSame("Undo must restore container", sgs, spt0.getContainer());

        // Redo should remove it again and clear container again
        assertTrue(undoManager.canRedo());
        undoManager.redo();
        assertSame(spt1, sgs.getSongPart(0)); // because first part removed, bar0 belongs to spt1 now
        assertNull("Redo must clear container again", spt0.getContainer());
    }

    @Test
    public void testRemoveMiddleSongPartShiftsIndexes()
    {
        // Remove sectionB (bars 4..7, 3/4)
        sgs.removeSongParts(List.of(spt1));

        assertEquals("Song size must shrink by removed part length", 12, sgs.getSizeInBars());

        // spt0 remains at start
        assertSame(spt0, sgs.getSongPart(0));
        assertEquals(0, spt0.getStartBarIndex());

        // spt2 should now start at bar 4
        assertSame(spt2, sgs.getSongPart(4));
        assertEquals(4, spt2.getStartBarIndex());
    }


    @Test
    public void testToPositionInNaturalBeatsAndToPositionAcrossTimeSignatures()
    {
        // With the fixture:
        // bars 0-3: 4/4 => 4 beats per bar
        // bars 4-7: 3/4 => 3 beats per bar
        // bars 8-15: 4/4 => 4 beats per bar

        assertEquals(0f, sgs.toPositionInNaturalBeats(0), 0.0001f);
        assertEquals(16f, sgs.toPositionInNaturalBeats(4), 0.0001f);
        assertEquals(28f, sgs.toPositionInNaturalBeats(8), 0.0001f);
        assertEquals(60f, sgs.toPositionInNaturalBeats(16), 0.0001f); // end of song

        // Reverse mapping at boundaries
        Position p4 = sgs.toPosition(16f);
        assertNotNull(p4);
        assertEquals(4, p4.getBar());
        assertEquals(0f, p4.getBeat(), 0.0001f);

        Position p8 = sgs.toPosition(28f);
        assertNotNull(p8);
        assertEquals(8, p8.getBar());
        assertEquals(0f, p8.getBeat(), 0.0001f);

        // Beyond end should return null (per interface)
        assertNull(sgs.toPosition(60.0001f));
    }

    @Test
    public void testToBeatRangeCrossingTimeSignatureBoundary()
    {
        // Range [3..5] crosses 4/4 -> 3/4 boundary
        // start at bar 3 => 3 * 4 = 12
        // beats: bar 3 (4 beats) + bars 4-5 (2 * 3 beats) = 10 beats
        // expected [12, 22]
        FloatRange rg = sgs.toBeatRange(new IntRange(3, 5));
        assertFalse(rg.isEmpty());
        assertEquals(12f, rg.from, 0.0001f);
        assertEquals(22f, rg.to, 0.0001f);
    }

    @Test
    public void testGetBarRangeEmptySong()
    {
        SongStructure empty = new SongStructureImpl(cls); // parent cls present, but empty song parts list
        assertEquals(0, empty.getSizeInBars());
        assertEquals(IntRange.EMPTY_RANGE, empty.getBarRange());
        assertEquals(FloatRange.EMPTY_FLOAT_RANGE, empty.toBeatRange(null));
        assertNull(empty.getSongPart(0));
    }

    @Test
    public void testSetSongPartsRhythmReplacesWithClones() throws UnsupportedEditException
    {
        SongPart original = spt0;
        List<SongPart> spts = sgs.setSongPartsRhythm(List.of(spt0), r44bis);

        assertNotSame("Should return clones, not modify originals", original, spts.get(0));
        assertSame(r44bis, spts.get(0).getRhythm());
        assertSame("New clone should have container set", sgs, spts.get(0).getContainer());
    }

    @Test
    public void testResizeMultiplePartsSimultaneously()
    {
        sgs.resizeSongParts(Map.of(spt0, 2, spt2, 10));

        assertEquals(2, spt0.getNbBars());
        assertEquals(10, spt2.getNbBars());
        // Verify correct index shifting
        assertEquals(0, spt0.getStartBarIndex());
        assertEquals(2, spt1.getStartBarIndex());
        assertEquals(6, spt2.getStartBarIndex());
    }

    @Test
    public void testRemoveSongPartsNonMemberThrows()
    {
        SongPart nonMember = sgs.createSongPart(r44bis, "NonMember", 0, sectionA_44, false);
        try
        {
            sgs.removeSongParts(List.of(nonMember));
            fail("Expected IllegalArgumentException when removing non-member SongPart");
        } catch (IllegalArgumentException expected)
        {
            // ok
        }
    }

    @Test
    public void testGetUniqueAdaptedRhythms() throws UnsupportedEditException
    {
        Rhythm adapted34 = rdb.getAdaptedRhythmInstance(r44, TimeSignature.THREE_FOUR);
        SongPart adaptedSpt = sgs.createSongPart(adapted34, "Adapted", sgs.getSizeInBars(), sectionB_34, true);
        sgs.addSongParts(List.of(adaptedSpt));

        List<AdaptedRhythm> adaptedRhythms = sgs.getUniqueAdaptedRhythms();
        assertEquals(1, adaptedRhythms.size());
        assertSame(adapted34, adaptedRhythms.get(0));
    }

    @Test
    public void testGetLastUsedRhythmReturnsNull()
    {
        assertNull("Never-used time signature should return null", sgs.getLastUsedRhythm(TimeSignature.FIVE_FOUR));
    }

    @Test
    public void testVetoPreventsAddAndDoesNotChangeState()
    {
        // Add a sync listener that vetoes add
        sgs.addSgsChangeSyncListener((SgsChangeEvent e) -> 
        {
            if (e instanceof SgsVetoableChangeEvent ve && ve.getChangeEvent() instanceof SptAddedEvent)
            {
                throw new UnsupportedEditException("vetoed SptAddedEvent");
            }
        });

        int sizeBefore = sgs.getSizeInBars();
        List<SongPart> partsBefore = sgs.getSongParts();

        try
        {
            var newSpt = spt0.getCopy(null, 0, spt0.getNbBars(), spt0.getParentSection());
            sgs.addSongParts(List.of(newSpt));
            fail("Expected veto to throw UnsupportedEditException");
        } catch (UnsupportedEditException expected)
        {
            // ok
        }

        // Ensure no change
        assertEquals(sizeBefore, sgs.getSizeInBars());
        assertEquals(partsBefore, sgs.getSongParts());
        assertSame(spt0, sgs.getSongPart(0));
        assertSame(sgs, spt0.getContainer());
    }
// Add the following 7 @Test methods to SongStructureImplTest

    @Test
    public void testRemoveMultipleSongPartsInOneCall()
    {
        // Remove first and last song part in one call => only middle remains
        sgs.removeSongParts(List.of(spt0, spt2));

        int expectedSize = spt1.getNbBars();
        assertEquals(expectedSize, sgs.getSizeInBars());

        // Only one part should remain, starting at bar 0
        assertEquals(1, sgs.getSongParts().size());
        assertSame(spt1, sgs.getSongParts().get(0));
        assertEquals(0, spt1.getStartBarIndex());
        assertSame(spt1, sgs.getSongPart(0));

        // Containers cleared for removed parts
        assertNull(spt0.getContainer());
        assertNull(spt2.getContainer());
        assertSame(sgs, spt1.getContainer());
    }

    @Test
    public void testToBeatRangeNullIsWholeSong()
    {
        // Whole song beat range should be [0, 60] with the current fixture:
        // 4 bars of 4/4 => 16
        // 4 bars of 3/4 => 12 => cumulative 28
        // 8 bars of 4/4 => 32 => cumulative 60
        FloatRange rg = sgs.toBeatRange(null);
        assertFalse(rg.isEmpty());
        assertEquals(0f, rg.from, 0.0001f);
        assertEquals(60f, rg.to, 0.0001f);
    }

    @Test
    public void testToPositionInsideBar()
    {
        // Beat 17 is 1 beat after bar 4 boundary (which starts at 16 beats)
        Position p = sgs.toPosition(17f);
        assertNotNull(p);
        assertEquals(4, p.getBar());
        assertEquals(1f, p.getBeat(), 0.0001f);

        // 27.9 is still within the 3/4 section (bar 4..7), just before bar 8 boundary at 28
        Position p2 = sgs.toPosition(27.9f);
        assertNotNull(p2);
        assertTrue("Expected bar < 8 for 27.9 beats", p2.getBar() < 8);
    }

    @Test
    public void testDefaultToPositionInNaturalBeatsWithPosition()
    {
        // Default method: toPositionInNaturalBeats(Position) = toPositionInNaturalBeats(bar) + beat
        float b = sgs.toPositionInNaturalBeats(new Position(4, 1f));
        assertEquals(17f, b, 0.0001f);
    }

    @Test
    public void testGetSongPartBoundaries()
    {
        assertNull("Negative bar index should return null", sgs.getSongPart(-1));
        assertNull("barIndex == size should return null", sgs.getSongPart(sgs.getSizeInBars()));
        assertSame("Last bar should return last part", spt2, sgs.getSongPart(sgs.getSizeInBars() - 1));
    }

    @Test
    public void testVetoDoesNotRecordUndoableEdit()
    {
        // End the compound edit so we can check undo stack cleanly for this test
        undoManager.endCEdit(UNDO_EDIT);

        // Add a sync listener that vetoes add
        sgs.addSgsChangeSyncListener((SgsChangeEvent e) -> 
        {
            if (e instanceof SgsVetoableChangeEvent ve && ve.getChangeEvent() instanceof SptAddedEvent)
            {
                throw new UnsupportedEditException("vetoed SptAddedEvent");
            }
        });

        int sizeBefore = sgs.getSizeInBars();

        try
        {
            var newSpt = spt0.getCopy(null, 0, spt0.getNbBars(), spt0.getParentSection());
            sgs.addSongParts(List.of(newSpt));
            fail("Expected veto to throw UnsupportedEditException");
        } catch (UnsupportedEditException expected)
        {
            // ok
        }

        // Ensure no change
        assertEquals(sizeBefore, sgs.getSizeInBars());

        // Ensure no undoable edit has been recorded by the vetoed operation
        assertFalse("Vetoed operation must not create undoable edit", undoManager.canUndo());
    }


    @Test
    public void testAddSongPartAppendAtEnd()
    {
        int sizeBefore = sgs.getSizeInBars();
        int nbBars = 8;

        SongPart newSpt = sgs.createSongPart(r44bis, "Append-4-4", sizeBefore, sectionC_44, true);
        try
        {
            sgs.addSongParts(List.of(newSpt));
        } catch (UnsupportedEditException ex)
        {
            fail("addSongParts should not throw here: " + ex.getMessage());
        }

        assertEquals(sizeBefore + nbBars, sgs.getSizeInBars());
        assertSame("Appended part should own the first new bar", newSpt, sgs.getSongPart(sizeBefore));
        assertSame("Container should be set on add", sgs, newSpt.getContainer());

        // Existing parts should keep their boundaries
        assertSame(spt0, sgs.getSongPart(0));
        assertSame(spt1, sgs.getSongPart(4));
        assertSame(spt2, sgs.getSongPart(8));
    }

    @Test
    public void testAddSongPartInsertAtBeginningShiftsAll()
    {
        int sizeInBars = sgs.getSizeInBars();
        SongPart newSpt = sgs.createSongPart(r44, "Insert-0", 0, sectionA_44, false);
        try
        {
            sgs.addSongParts(List.of(newSpt));
        } catch (UnsupportedEditException ex)
        {
            fail("addSongParts should not throw here: " + ex.getMessage());
        }

        assertEquals(newSpt.getNbBars() + sizeInBars, sgs.getSizeInBars());
        assertSame(newSpt, sgs.getSongPart(0));
        assertSame("Old first part should start after inserted part", spt0, sgs.getSongPart(4));
        assertEquals(4, spt0.getStartBarIndex());
        assertEquals(8, spt1.getStartBarIndex());
        assertEquals(12, spt2.getStartBarIndex());
        assertSame(sgs, newSpt.getContainer());
    }

    @Test
    public void testAddSongPartInsertAtSectionBoundary()
    {
        // Insert at bar 4 (boundary between spt0 and spt1)
        int sizeInBars = sgs.getSizeInBars();
        SongPart newSpt = sgs.createSongPart(r44, "Insert-4", 4, sectionA_44, false);

        try
        {
            sgs.addSongParts(List.of(newSpt));
        } catch (UnsupportedEditException ex)
        {
            fail("addSongParts should not throw here: " + ex.getMessage());
        }

        assertEquals(newSpt.getNbBars() + sizeInBars, sgs.getSizeInBars());

        // New part begins at bar 4
        assertSame(newSpt, sgs.getSongPart(4));
        assertEquals(4, newSpt.getStartBarIndex());
        assertEquals(8, spt1.getStartBarIndex());
        assertEquals(12, spt2.getStartBarIndex());
    }

    @Test
    public void testAddSongPartsInvalidStartBarIndexThrows()
    {
        // Start bar index in the middle of spt0 (spt0 spans [0..3]) => invalid
        SongPart bad = sgs.createSongPart(r44, "BadInsert", 2, sectionA_44, false);
        try
        {
            sgs.addSongParts(List.of(bad));
            fail("Expected IllegalArgumentException for invalid startBarIndex inside an existing SongPart");
        } catch (IllegalArgumentException expected)
        {
            // ok
        } catch (UnsupportedEditException ex)
        {
            fail("Unexpected UnsupportedEditException: " + ex.getMessage());
        }
    }

    @Test
    public void testAddSongPartsParentSectionNull()
    {
        SongPart sptNew = spt0.getCopy(null, 0, 4, sectionA_44);
        SongPart bad = new SongPartImpl(spt0.getRhythm(), 4, 4, null);
        try
        {
            sgs.addSongParts(List.of(sptNew, bad));
            fail("Expected IllegalArgumentException for SongPart with parentSection==null");
        } catch (IllegalArgumentException expected)
        {
            undoManager.endCEdit(UNDO_EDIT);    // inconsistent state, don't try to undo anything in tearDown
            // ok
        } catch (UnsupportedEditException ex)
        {
            fail("Unexpected UnsupportedEditException: " + ex.getMessage());
        }
    }

    @Test
    public void testResizeUndoRedoRestoresSizesAndIndexes()
    {
        // Resize spt0 down, which will shift spt1/spt2
        sgs.resizeSongParts(Map.of(spt0, 2));

        assertEquals(14, sgs.getSizeInBars());
        assertEquals(2, spt0.getNbBars());
        assertEquals(2, spt1.getStartBarIndex());
        assertEquals(6, spt2.getStartBarIndex());

        undoManager.endCEdit(UNDO_EDIT);

        // Undo restores original
        assertTrue(undoManager.canUndo());
        undoManager.undo();

        assertEquals(16, sgs.getSizeInBars());
        assertEquals(4, spt0.getNbBars());
        assertEquals(0, spt0.getStartBarIndex());
        assertEquals(4, spt1.getStartBarIndex());
        assertEquals(8, spt2.getStartBarIndex());

        // Redo reapplies
        assertTrue(undoManager.canRedo());
        undoManager.redo();

        assertEquals(14, sgs.getSizeInBars());
        assertEquals(2, spt0.getNbBars());
        assertEquals(2, spt1.getStartBarIndex());
        assertEquals(6, spt2.getStartBarIndex());
    }

    @Test
    public void testToClsPositionMapping()
    {
        // SongStructure pos (bar 4 beat 1) is inside sectionB at bar 4 in the chord lead sheet
        Position clsPos = sgs.toClsPosition(new Position(4, 1f));
        assertNotNull(clsPos);
        assertEquals(4, clsPos.getBar());
        assertEquals(1f, clsPos.getBeat(), 0.0001f);

        // A position within sectionC: bar 9 beat 0 should map to CLS bar 9 beat 0
        Position clsPos2 = sgs.toClsPosition(new Position(9, 0f));
        assertNotNull(clsPos2);
        assertEquals(9, clsPos2.getBar());
        assertEquals(0f, clsPos2.getBeat(), 0.0001f);
    }

    @Test
    public void testSetSongPartsName()
    {
        String newName = "NewName";
        String originalName1 = spt1.getName();
        sgs.setSongPartsName(List.of(spt0, spt2), newName);

        assertEquals(newName, spt0.getName());
        assertEquals(newName, spt2.getName());
        // spt1 should remain unchanged
        assertEquals(originalName1, spt1.getName());
    }


    @Test
    public void testSetRhythmParameterValue()
    {
        Rhythm r = spt0.getRhythm();
        @SuppressWarnings("unchecked")
        RP_SYS_Variation rp = RP_SYS_Variation.getVariationRp(r);
        String originalValue = spt0.getRPValue(rp);
        String newValue = rp.getNextValue(originalValue);
        sgs.setRhythmParameterValue(spt0, rp, newValue);
        assertEquals(newValue, spt0.getRPValue(rp));
    }


    @Test
    public void testGetRecommendedRhythmUsesLastUsed()
    {
        SongPart newSpt = sgs.createSongPart(r34bis, "Sptbis", sgs.getSizeInBars(), sectionB_34, true);
        try
        {
            sgs.addSongParts(List.of(newSpt));
        } catch (UnsupportedEditException ex)
        {
            fail("addSongParts should not throw: " + ex.getMessage());
        }

        Rhythm recommended = sgs.getRecommendedRhythm(TimeSignature.THREE_FOUR, sgs.getSizeInBars());
        assertSame(r34bis, recommended);
    }

    @Test
    public void testGetRecommendedRhythmReturnsAdaptedRhythmWhenAppropriate()
    {
        // When no last-used rhythm exists for a time signature,
        // but current rhythm at position can be adapted
        Rhythm recommended = sgs.getRecommendedRhythm(TimeSignature.FIVE_FOUR, 2);
        assertNotNull(recommended);
        // Should be either AdaptedRhythm or default from database
        assertEquals(TimeSignature.FIVE_FOUR, recommended.getTimeSignature());
    }

    @Test
    public void testGetRecommendedRhythmFallsBackToDefault()
    {
        // For a completely unused time signature with no adaptable rhythm
        Rhythm recommended = sgs.getRecommendedRhythm(TimeSignature.TWELVE_EIGHT, sgs.getSizeInBars());
        assertNotNull("Should always return a non-null rhythm", recommended);
        assertEquals(TimeSignature.TWELVE_EIGHT, recommended.getTimeSignature());
    }


    @Test
    public void testGetSptItemPosition()
    {
        Position pos = sgs.getSptItemPosition(spt1, cs2);
        assertEquals(new Position(5, 1), pos);
    }

    @Test
    public void testGetSptItemPositionThrowsForWrongSection()
    {
        try
        {
            Position pos = sgs.getSptItemPosition(spt0, cs2);
        } catch (IllegalArgumentException expected)
        {
            // ok
        }
    }

    @Test
    public void testGetSongPartsWithPredicate()
    {
        List<SongPart> fourFourParts = sgs.getSongParts(
                spt -> spt.getRhythm().getTimeSignature().equals(TimeSignature.FOUR_FOUR)
        );

        assertEquals(2, fourFourParts.size());
        assertTrue(fourFourParts.contains(spt0));
        assertTrue(fourFourParts.contains(spt2));
        assertFalse(fourFourParts.contains(spt1));
    }


    @Test
    public void testSynchronizedListenerCalledWhileLockHeld()
    {
        final boolean[] lockHeldDuringCallback =
        {
            false
        };

        sgs.addSgsChangeSyncListener((SgsChangeEvent e) -> 
        {
            // Check if write lock is held
            lockHeldDuringCallback[0] = sgs.getLock().isWriteLockedByCurrentThread();
        });

        sgs.removeSongParts(List.of(spt0));
        assertTrue("Sync listener should be called while write lock held", lockHeldDuringCallback[0]);
    }

    @Test
    public void testNonSynchronizedListenerCalledAfterLockReleased()
    {
        final boolean[] lockHeldDuringCallback =
        {
            false
        };

        sgs.addSgsChangeListener((SgsChangeEvent e) -> 
        {
            lockHeldDuringCallback[0] = sgs.getLock().isWriteLockedByCurrentThread();
        });

        sgs.removeSongParts(List.of(spt0));
        assertFalse("Non-sync listener should be called after lock released", lockHeldDuringCallback[0]);
    }

    @Test
    public void testGetDeepCopyIndependence()
    {
        SongStructure sgsCopy = sgs.getDeepCopy(cls);

        // Verify initial equality
        int copySize = sgsCopy.getSizeInBars();
        assertTrue(equals(sgsCopy, sgs));

        // Modify original
        sgs.removeSongParts(List.of(spt0));

        // Copy should be unaffected
        assertFalse(equals(sgsCopy, sgs));
        assertEquals(copySize, sgsCopy.getSizeInBars());
    }


    @Test
    public void testGetDeepCopySongPartsAreDistinct()
    {
        SongStructure copy = sgs.getDeepCopy(cls);

        List<SongPart> origParts = sgs.getSongParts();
        List<SongPart> copyParts = copy.getSongParts();

        for (int i = 0; i < origParts.size(); i++)
        {
            assertNotSame("SongParts should be distinct objects", origParts.get(i), copyParts.get(i));
            assertTrue("SongParts should be equal in content", origParts.get(i).isEqual(copyParts.get(i)));
        }
    }

    @Test
    public void testGetUniqueRhythmsExcludeAdaptedRhythms()
    {
        // Add an adapted rhythm
        var rOrig = spt0.getRhythm();
        Rhythm adapted = rdb.getAdaptedRhythmInstance(rOrig, TimeSignature.THREE_FOUR);
        assert adapted != null : "rOrig=" + rOrig;
        SongPart adaptedSpt = sgs.createSongPart(adapted, "Adapted", sgs.getSizeInBars(), sectionB_34, true);
        try
        {
            sgs.addSongParts(List.of(adaptedSpt));
        } catch (UnsupportedEditException ex)
        {
            fail();
        }

        List<Rhythm> rhythms = sgs.getUniqueRhythms(true, false);
        assertFalse("Should not contain AdaptedRhythm", rhythms.contains(adapted));
    }


    @Test
    public void testToBeatRangeOutOfBounds()
    {
        FloatRange rg = sgs.toBeatRange(new IntRange(20, 25));
        assertTrue("Out of bounds range should return empty", rg.isEmpty());
    }

    @Test
    public void testToPositionNegativeBeats()
    {
        Position p = sgs.toPosition(-1f);
        assertNull("Negative beats should return null", p);
    }

    @Test
    public void testToClsPositionBeyondEnd()
    {
        Position clsPos = sgs.toClsPosition(new Position(100, 0f));
        assertNull("Position beyond end should return null", clsPos);
    }

    @Test
    public void testGetUniqueTimeSignatures()
    {
        List<TimeSignature> timeSigs = sgs.getUniqueTimeSignatures();
        assertEquals(2, timeSigs.size());
        assertTrue(timeSigs.contains(TimeSignature.FOUR_FOUR));
        assertTrue(timeSigs.contains(TimeSignature.THREE_FOUR));

        // Should be ordered by appearance
        assertEquals(TimeSignature.FOUR_FOUR, timeSigs.get(0));
        assertEquals(TimeSignature.THREE_FOUR, timeSigs.get(1));
    }

    // =========================================================================================================
    // CONCURRENCY TESTS
    // =========================================================================================================

    @Test(timeout = 50000) // 5 second timeout to detect deadlocks
    public void testConcurrentDeepCopyWhileMutating() throws InterruptedException
    {
        final int DEEP_COPY_ITERATIONS = 2000;
        final int MUTATION_ITERATIONS = DEEP_COPY_ITERATIONS / 2;
        final AtomicInteger deepCopyCount = new AtomicInteger(0);
        final AtomicInteger mutationCount = new AtomicInteger(0);
        final AtomicReference<Throwable> readerException = new AtomicReference<>();
        final AtomicReference<Throwable> writerException = new AtomicReference<>();
        final Rhythm r44 = spt0.getRhythm();
        final Rhythm r34 = spt1.getRhythm();

        // Thread 1: Repeatedly calls getDeepCopy (read operations)
        Thread readerThread = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < DEEP_COPY_ITERATIONS; i++)
                {
                    SongStructure copy = sgs.getDeepCopy(cls);
                    assertNotNull("Deep copy should not be null", copy);
                    assertTrue(copy.getSizeInBars() > 0);
                    deepCopyCount.incrementAndGet();

                    // Also test read operations
                    if (i % 10 == 0)
                    {
                        List<SongPart> parts = sgs.getSongParts();
                        assertNotNull(parts);
                        assertTrue(sgs.getSizeInBars() > 0);
                    }

                    // Small yield to encourage interleaving
                    if (i % 100 == 0)
                    {
                        Thread.yield();
                    }
                }
            } catch (Throwable t)
            {
                readerException.set(t);
            }
        }, "DeepCopy-Reader-Thread");


        // Thread 2: Performs various mutations
        Thread writerThread = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < MUTATION_ITERATIONS; i++)
                {
                    try
                    {
                        // Cycle through different mutation operations
                        switch (i % 5)
                        {
                            case 0 ->
                            {
                                // Add a song part at the end
                                int sizeBeforeAdd = sgs.getSizeInBars();
                                SongPart newSpt = sgs.createSongPart(r44, "Concurrent-" + i, sizeBeforeAdd, sectionC_44, true);
                                sgs.addSongParts(List.of(newSpt));
                                mutationCount.incrementAndGet();
                            }

                            case 1 ->
                            {
                                // Remove the last song part
                                List<SongPart> spts = sgs.getSongParts();
                                if (spts.size() > 1)
                                {
                                    sgs.removeSongParts(List.of(spts.getLast()));
                                    mutationCount.incrementAndGet();
                                }
                            }

                            case 2 ->
                            {
                                // Replace a middle song part with a different rhythm
                                List<SongPart> spts = sgs.getSongParts();
                                if (!spts.isEmpty())
                                {
                                    SongPart spt = spts.get(1);
                                    var newRhythm = spt.getRhythm() == r34 ? r34bis : r34;
                                    sgs.setSongPartsRhythm(List.of(spt), newRhythm);
                                    mutationCount.incrementAndGet();
                                }
                            }

                            case 3 ->
                            {
                                // Change song part name
                                List<SongPart> spts = sgs.getSongParts();
                                if (!spts.isEmpty())
                                {
                                    sgs.setSongPartsName(List.of(spts.get(0)), "Modified-" + i);
                                    mutationCount.incrementAndGet();
                                }
                            }

                            case 4 ->
                            {
                                // Change rhythm parameter
                                List<SongPart> rpParts = sgs.getSongParts();
                                if (!rpParts.isEmpty())
                                {
                                    SongPart spt = rpParts.get(1);
                                    Rhythm r = spt.getRhythm();
                                    RP_SYS_Variation rp = RP_SYS_Variation.getVariationRp(r);
                                    if (rp != null)
                                    {
                                        String currentValue = spt.getRPValue(rp);
                                        String newValue = rp.getNextValue(currentValue);
                                        sgs.setRhythmParameterValue(spt, rp, newValue);
                                        mutationCount.incrementAndGet();
                                    }
                                }
                            }
                        }

                        // Small yield to encourage interleaving
                        if (i % 50 == 0)
                        {
                            Thread.yield();
                        }

                    } catch (UnsupportedEditException ex)
                    {
                        // Expected in some cases, just continue
                    }
                }
            } catch (Throwable t)
            {
                writerException.set(t);
            }
        }, "Mutation-Writer-Thread");

        // Start both threads
        readerThread.start();
        writerThread.start();

        // Wait for both to complete
        readerThread.join();
        writerThread.join();

        // Check for exceptions
        if (readerException.get() != null)
        {
            fail("Reader thread failed: " + readerException.get().getMessage());
        }
        if (writerException.get() != null)
        {
            fail("Writer thread failed: " + writerException.get().getMessage());
        }

        // Verify both threads made progress
        assertTrue("Deep copy should have been called multiple times", deepCopyCount.get() > DEEP_COPY_ITERATIONS * 0.9);
        assertTrue("Mutations should have been performed multiple times", mutationCount.get() > MUTATION_ITERATIONS * 0.9);

        // Verify SongStructure is still in valid state
        assertNotNull(sgs.getSongParts());
        assertTrue(sgs.getSizeInBars() >= 0);

        System.out.println("Concurrency test completed successfully:");
        System.out.println("  Deep copies: " + deepCopyCount.get());
        System.out.println("  Mutations: " + mutationCount.get());
        System.out.println("  Final song size: " + sgs.getSizeInBars() + " bars");
        System.out.println("  Final parts count: " + sgs.getSongParts().size());

    }
//
//    @Test(timeout = 10000) // 10 second timeout
//    public void testConcurrentReadersWithSingleWriter() throws InterruptedException
//    {
//        final int ITERATIONS = 500;
//        final AtomicInteger reader1Count = new AtomicInteger(0);
//        final AtomicInteger reader2Count = new AtomicInteger(0);
//        final AtomicInteger reader3Count = new AtomicInteger(0);
//        final AtomicInteger writerCount = new AtomicInteger(0);
//        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();
//
//        // Multiple reader threads
//        Thread reader1 = new Thread(() -> 
//        {
//            try
//            {
//                for (int i = 0; i < ITERATIONS; i++)
//                {
//                    sgs.getDeepCopy(cls);
//                    reader1Count.incrementAndGet();
//                    if (i % 100 == 0)
//                    {
//                        Thread.yield();
//                    }
//                }
//            } catch (Throwable t)
//            {
//                exceptions.add(t);
//            }
//        }, "Reader-1");
//
//        Thread reader2 = new Thread(() -> 
//        {
//            try
//            {
//                for (int i = 0; i < ITERATIONS; i++)
//                {
//                    List<SongPart> parts = sgs.getSongParts();
//                    int size = sgs.getSizeInBars();
//                    List<Rhythm> rhythms = sgs.getUniqueRhythms(false, false);
//                    reader2Count.incrementAndGet();
//                    if (i % 100 == 0)
//                    {
//                        Thread.yield();
//                    }
//                }
//            } catch (Throwable t)
//            {
//                exceptions.add(t);
//            }
//        }, "Reader-2");
//
//        Thread reader3 = new Thread(() -> 
//        {
//            try
//            {
//                for (int i = 0; i < ITERATIONS; i++)
//                {
//                    sgs.toPositionInNaturalBeats(0);
//                    sgs.toPosition(10f);
//                    sgs.getSongPart(0);
//                    reader3Count.incrementAndGet();
//                    if (i % 100 == 0)
//                    {
//                        Thread.yield();
//                    }
//                }
//            } catch (Throwable t)
//            {
//                exceptions.add(t);
//            }
//        }, "Reader-3");
//
//        // Single writer thread
//        Thread writer = new Thread(() -> 
//        {
//            try
//            {
//                for (int i = 0; i < ITERATIONS / 10; i++)
//                {
//                    try
//                    {
//                        List<SongPart> parts = sgs.getSongParts();
//                        if (!parts.isEmpty())
//                        {
//                            sgs.setSongPartsName(List.of(parts.get(0)), "Name-" + i);
//                            writerCount.incrementAndGet();
//                        }
//                        Thread.sleep(1); // Slower writer
//                    } catch (UnsupportedEditException | InterruptedException ex)
//                    {
//                        // Expected, continue
//                    }
//                }
//            } catch (Throwable t)
//            {
//                exceptions.add(t);
//            }
//        }, "Writer");
//
//        // Start all threads
//        reader1.start();
//        reader2.start();
//        reader3.start();
//        writer.start();
//
//        // Wait for completion
//        reader1.join();
//        reader2.join();
//        reader3.join();
//        writer.join();
//
//        // Check for exceptions
//        if (!exceptions.isEmpty())
//        {
//            fail("Thread failed: " + exceptions.get(0).getMessage());
//        }
//
//        // Verify all threads made progress
//        assertTrue("Reader 1 should have completed iterations", reader1Count.get() > 450);
//        assertTrue("Reader 2 should have completed iterations", reader2Count.get() > 450);
//        assertTrue("Reader 3 should have completed iterations", reader3Count.get() > 450);
//        assertTrue("Writer should have completed some mutations", writerCount.get() > 10);
//
//        System.out.println("Multiple readers test completed successfully:");
//        System.out.println("  Reader 1 (deep copy): " + reader1Count.get());
//        System.out.println("  Reader 2 (get parts): " + reader2Count.get());
//        System.out.println("  Reader 3 (position): " + reader3Count.get());
//        System.out.println("  Writer (mutations): " + writerCount.get());
//
//        undoManager.endCEdit(UNDO_EDIT);
//    }

//    @Test(timeout = 10000)
//    public void testConcurrentListenerNotifications() throws InterruptedException
//    {
//        final int ITERATIONS = 200;
//        final AtomicInteger syncListenerCount = new AtomicInteger(0);
//        final AtomicInteger asyncListenerCount = new AtomicInteger(0);
//        final AtomicInteger mutationCount = new AtomicInteger(0);
//        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();
//
//        // Add listeners
//        sgs.addSgsChangeSyncListener((SgsChangeEvent e) -> 
//        {
//            syncListenerCount.incrementAndGet();
//            // Verify lock is held
//            assertTrue("Sync listener must be called with write lock", sgs.getLock().isWriteLockedByCurrentThread());
//        });
//
//        sgs.addSgsChangeListener((SgsChangeEvent e) -> 
//        {
//            asyncListenerCount.incrementAndGet();
//            // Verify lock is NOT held
//            assertFalse("Async listener must be called without write lock", sgs.getLock().isWriteLockedByCurrentThread());
//        });
//
//        // Mutating thread
//        Thread mutator = new Thread(() -> 
//        {
//            try
//            {
//                for (int i = 0; i < ITERATIONS; i++)
//                {
//                    try
//                    {
//                        int sizeBeforeAdd = sgs.getSizeInBars();
//                        SongPart newSpt = sgs.createSongPart(r54, "Listener-" + i, sizeBeforeAdd, 1, sectionC_44, false);
//                        sgs.addSongParts(List.of(newSpt));
//                        mutationCount.incrementAndGet();
//                        Thread.sleep(2);
//                    } catch (UnsupportedEditException | InterruptedException ex)
//                    {
//                        // Expected
//                    }
//                }
//            } catch (Throwable t)
//            {
//                exceptions.add(t);
//            }
//        }, "Mutator");
//
//        // Reading thread during mutations
//        Thread reader = new Thread(() -> 
//        {
//            try
//            {
//                for (int i = 0; i < ITERATIONS * 2; i++)
//                {
//                    sgs.getSongParts();
//                    Thread.yield();
//                }
//            } catch (Throwable t)
//            {
//                exceptions.add(t);
//            }
//        }, "Reader");
//
//        mutator.start();
//        reader.start();
//
//        mutator.join();
//        reader.join();
//
//        if (!exceptions.isEmpty())
//        {
//            fail("Thread failed: " + exceptions.get(0).getMessage());
//        }
//
//        // Both listener types should have been called for each mutation
//        assertEquals("Sync listener should be called for each mutation", mutationCount.get(), syncListenerCount.get());
//        assertEquals("Async listener should be called for each mutation", mutationCount.get(), asyncListenerCount.get());
//
//        System.out.println("Listener concurrency test completed successfully:");
//        System.out.println("  Mutations: " + mutationCount.get());
//        System.out.println("  Sync notifications: " + syncListenerCount.get());
//        System.out.println("  Async notifications: " + asyncListenerCount.get());
//
//        undoManager.endCEdit(UNDO_EDIT);
//    }

//    @Test(timeout = 15000)
//    public void testConcurrentStressTest() throws InterruptedException
//    {
//        final int ITERATIONS = 300;
//        final AtomicInteger totalOperations = new AtomicInteger(0);
//        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();
//        final int NUM_THREADS = 5;
//
//        Thread[] threads = new Thread[NUM_THREADS];
//
//        for (int t = 0; t < NUM_THREADS; t++)
//        {
//            final int threadId = t;
//            threads[t] = new Thread(() -> 
//            {
//                try
//                {
//                    for (int i = 0; i < ITERATIONS; i++)
//                    {
//                        int operation = (threadId + i) % 8;
//
//                        switch (operation)
//                        {
//                            case 0: // Read: getDeepCopy
//                                sgs.getDeepCopy(cls);
//                                break;
//
//                            case 1: // Read: getSongParts
//                                sgs.getSongParts();
//                                break;
//
//                            case 2: // Read: position conversion
//                                sgs.toPositionInNaturalBeats(i % 16);
//                                break;
//
//                            case 3: // Read: get rhythms
//                                sgs.getUniqueRhythms(false, false);
//                                break;
//
//                            case 4: // Write: add part
//                                try
//                                {
//                                    int size = sgs.getSizeInBars();
//                                    SongPart newSpt = sgs.createSongPart(r54, "T" + threadId + "-" + i, size, 1, sectionC_44, false);
//                                    sgs.addSongParts(List.of(newSpt));
//                                } catch (UnsupportedEditException ex)
//                                {
//                                    // Expected
//                                }
//                                break;
//
//                            case 5: // Write: remove part
//                                List<SongPart> parts = sgs.getSongParts();
//                                if (parts.size() > 1)
//                                {
//                                    sgs.removeSongParts(List.of(parts.get(parts.size() - 1)));
//                                }
//                                break;
//
//                            case 6: // Write: rename
//                                List<SongPart> nameParts = sgs.getSongParts();
//                                if (!nameParts.isEmpty())
//                                {
//                                    sgs.setSongPartsName(List.of(nameParts.get(0)), "Thread" + threadId);
//                                }
//                                break;
//
//                            case 7: // Read: get song part
//                                int size = sgs.getSizeInBars();
//                                if (size > 0)
//                                {
//                                    sgs.getSongPart(0);
//                                }
//                                break;
//                        }
//
//                        totalOperations.incrementAndGet();
//
//                        if (i % 50 == 0)
//                        {
//                            Thread.yield();
//                        }
//                    }
//                } catch (Throwable t)
//                {
//                    exceptions.add(t);
//                }
//            }, "Stress-Thread-" + threadId);
//        }
//
//        // Start all threads
//        for (Thread thread : threads)
//        {
//            thread.start();
//        }
//
//        // Wait for all to complete
//        for (Thread thread : threads)
//        {
//            thread.join();
//        }
//
//        // Check for exceptions
//        if (!exceptions.isEmpty())
//        {
//            fail("Thread failed: " + exceptions.get(0).getMessage());
//        }
//
//        // Verify operations completed
//        assertTrue("Should have completed many operations", totalOperations.get() > NUM_THREADS * ITERATIONS * 0.9);
//
//        // Verify final state is valid
//        assertNotNull(sgs.getSongParts());
//        assertTrue(sgs.getSizeInBars() >= 0);
//
//        System.out.println("Stress test completed successfully:");
//        System.out.println("  Total operations: " + totalOperations.get());
//        System.out.println("  Threads: " + NUM_THREADS);
//        System.out.println("  Final song size: " + sgs.getSizeInBars() + " bars");
//
//        undoManager.endCEdit(UNDO_EDIT);
//    }
    // =========================================================================================================
    // Helper methods
    // =========================================================================================================
    private boolean equals(SongStructure sgs1, SongStructure sgs2)
    {
        var spts1 = sgs1.getSongParts();
        var spts2 = sgs2.getSongParts();
        if (spts1.size() != spts2.size())
        {
            return false;
        }
        for (int i = 0; i < spts1.size(); i++)
        {
            if (!spts1.get(i).isEqual(spts2.get(i)))
            {
                return false;
            }
        }
        return true;
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
