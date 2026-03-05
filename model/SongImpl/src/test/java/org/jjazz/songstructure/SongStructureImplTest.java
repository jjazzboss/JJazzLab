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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_Variation;
import org.jjazz.song.ExecutionManager;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.*;
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
    private TestInfo testInfo;

    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    public static void setUpClass() throws Exception
    {
        rdb = (DefaultRhythmDatabase) RhythmDatabase.getDefault();
        rdb.addRhythmsFromRhythmProviders(false, true, false);
        System.out.println(rdb.toStatsString());
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws UnsupportedEditException, ParseException
    {
        this.testInfo = testInfo;
        undoManager = new JJazzUndoManager();

        // Build a 16 bars chordleadsheet [0-15]
        // bar 0: SectionA 4/4
        // bar 4: SectionB 3/4
        // bar 8: SectionC 4/4
        cls = SongFactory.getDefault().createEmptyChordLeadSheet("SectionA", TimeSignature.FOUR_FOUR, 16, "C7");
        cs1 = cls.getItems(CLI_ChordSymbol.class).get(0); // C7 at bar 0 beat 0
        sectionA_44 = cls.getSection(0);
        sectionB_34 = (CLI_Section) sectionA_44.getCopy(new Section("SectionB", TimeSignature.THREE_FOUR), new Position(4));
        cls.addSection(sectionB_34);
        sectionC_44 = (CLI_Section) sectionA_44.getCopy(new Section("SectionC", TimeSignature.FOUR_FOUR), new Position(8));
        cls.addSection(sectionC_44);
        cs2 = (CLI_ChordSymbol) cs1.getCopy(ExtChordSymbol.get("Dm"), sectionB_34.getPosition().getMoved(1, 1));    // Dm at bar 5, beat 1
        cls.addItem(cs2);

        // Build a SongStructure from chordleadsheet => create 3 song parts, one per section
        sgs = SongFactory.getDefault().createSongStructure(cls);
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

    @AfterEach
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

        boolean b = sgs.equals(u_sgs);
        if (!b)
        {
            System.out.println(
                    "==== MISMATCH AFTER UNDO SEQUENCE for " + testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown") + "()");
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
    public void testContainerSetOnAdd() throws UnsupportedEditException
    {
        // Initial containers
        assertSame(sgs, spt0.getContainer());
        assertSame(sgs, spt1.getContainer());
        assertSame(sgs, spt2.getContainer());

        sgs.removeSongParts(List.of(spt0));
        assertSame(sgs, spt0.getContainer(), "Removed song part must keep its container");
        assertSame(sgs, spt2.getContainer(), "Remaining SongPart must keep container");

        var sgs2 = sgs.getDeepCopy(sgs.getParentChordLeadSheet());
        sgs2.addSongParts(List.of(spt0));

        assertSame(sgs2, spt0.getContainer(), "Add to new SongStructure must update container");
        assertSame(spt0, sgs2.getSongPart(0));

    }

    @Test
    public void testRemoveMiddleSongPartShiftsIndexes()
    {
        // Remove sectionB (bars 4..7, 3/4)
        sgs.removeSongParts(List.of(spt1));

        assertEquals(12, sgs.getSizeInBars(), "Song size must shrink by removed part length");

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
        assertNull(sgs.getLastUsedRhythm(TimeSignature.FIVE_FOUR), "Never-used time signature should return null");
    }

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

        // Containers are not cleared for removed parts
        assertSame(sgs, spt0.getContainer());
        assertSame(sgs, spt1.getContainer());
        assertSame(sgs, spt2.getContainer());
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
        assertTrue(p2.getBar() < 8, "Expected bar < 8 for 27.9 beats");
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
        assertNull(sgs.getSongPart(-1), "Negative bar index should return null");
        assertNull(sgs.getSongPart(sgs.getSizeInBars()), "barIndex == size should return null");
        assertSame(spt2, sgs.getSongPart(sgs.getSizeInBars() - 1), "Last bar should return last part");
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
        assertSame(newSpt, sgs.getSongPart(sizeBefore), "Appended part should own the first new bar");
        assertSame(sgs, newSpt.getContainer(), "Container should be set on add");

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
        assertSame(spt0, sgs.getSongPart(4), "Old first part should start after inserted part");
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
        assertNotNull(recommended, "Should always return a non-null rhythm");
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
    public void testListenerCalledAfterLockReleased()
    {
        final boolean[] lockHeldDuringCallback =
        {
            false
        };

        sgs.addSgsChangeListener((SgsChangeEvent e) -> 
        {
            lockHeldDuringCallback[0] = getLock().isWriteLockedByCurrentThread();
        });

        sgs.removeSongParts(List.of(spt0));
        assertFalse(lockHeldDuringCallback[0], "Non-sync listener should be called after lock released");
    }

    @Test
    public void testGetDeepCopyIndependence()
    {
        SongStructure sgsCopy = sgs.getDeepCopy(cls);

        // Verify initial equality
        int copySize = sgsCopy.getSizeInBars();
        assertEquals(sgs, sgsCopy);

        // Modify original, copy should be unaffected
        sgs.removeSongParts(List.of(spt0));
        assertNotEquals(sgs, sgsCopy);
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
            assertNotSame(origParts.get(i), copyParts.get(i), "SongParts should be distinct objects");
            assertTrue(origParts.get(i).equals(copyParts.get(i)), "SongParts should be equal in content");
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
        assertFalse(rhythms.contains(adapted), "Should not contain AdaptedRhythm");
    }


    @Test
    public void testToBeatRangeOutOfBounds()
    {
        FloatRange rg = sgs.toBeatRange(new IntRange(20, 25));
        assertTrue(rg.isEmpty(), "Out of bounds range should return empty");
    }

    @Test
    public void testToPositionNegativeBeats()
    {
        Position p = sgs.toPosition(-1f);
        assertNull(p, "Negative beats should return null");
    }

    @Test
    public void testToClsPositionBeyondEnd()
    {
        Position clsPos = sgs.toClsPosition(new Position(100, 0f));
        assertNull(clsPos, "Position beyond end should return null");
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
    // Gap 1 — setSongPartsRhythm: forward operation (no test existed)
    // =========================================================================================================

    @Test
    public void testSetSongPartsRhythmChangesRhythm() throws UnsupportedEditException
    {
        // Change spt0 from r44 to r44bis, leaving parentSection unchanged
        sgs.setSongPartsRhythm(List.of(spt0), r44bis, null);

        assertSame(r44bis, spt0.getRhythm(), "Rhythm must be updated after setSongPartsRhythm");
        // spt1 and spt2 must be unaffected
        assertSame(r34, spt1.getRhythm());
        assertSame(r44, spt2.getRhythm());
    }

    @Test
    public void testSetSongPartsRhythmMultipleParts() throws UnsupportedEditException
    {
        // Both 4/4 parts changed at once
        sgs.setSongPartsRhythm(List.of(spt0, spt2), r44bis, null);

        assertSame(r44bis, spt0.getRhythm());
        assertSame(r44bis, spt2.getRhythm());
        assertSame(r34, spt1.getRhythm(), "3/4 part must remain unchanged");
    }

    // =========================================================================================================
    // Gap 2 — getLastUsedRhythm reflects setSongPartsRhythm changes, including undo
    // =========================================================================================================

    @Test
    public void testGetLastUsedRhythmUpdatedAfterSetRhythm() throws UnsupportedEditException
    {
        // Baseline: initial setup populates mapTsLastRhythm
        assertSame(r44, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR));
        assertSame(r34, sgs.getLastUsedRhythm(TimeSignature.THREE_FOUR));

        sgs.setSongPartsRhythm(List.of(spt0), r44bis, null);

        assertSame(r44bis, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR), "mapTsLastRhythm must reflect new rhythm");
        assertSame(r34, sgs.getLastUsedRhythm(TimeSignature.THREE_FOUR), "Unrelated TS must be unaffected");
    }

    @Test
    public void testSetSongPartsRhythmUndoRedoRestoresRhythmAndLastUsed() throws UnsupportedEditException
    {
        sgs.setSongPartsRhythm(List.of(spt0), r44bis, null);

        undoManager.endCEdit(UNDO_EDIT);

        // Undo: rhythm and mapTsLastRhythm must both be restored
        assertTrue(undoManager.canUndo());
        undoManager.undo();

        assertSame(r44, spt0.getRhythm(), "Undo must restore original rhythm");
        assertSame(r44, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR), "Undo must restore mapTsLastRhythm");

        // Redo: rhythm and mapTsLastRhythm must be re-applied
        assertTrue(undoManager.canRedo());
        undoManager.redo();

        assertSame(r44bis, spt0.getRhythm(), "Redo must re-apply rhythm");
        assertSame(r44bis, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR), "Redo must restore mapTsLastRhythm");
    }

    // =========================================================================================================
    // Gap 3 — explicit undo/redo for addSongParts and removeSongParts
    // =========================================================================================================

    @Test
    public void testAddSongPartsUndoRedo() throws UnsupportedEditException
    {
        int sizeBefore = sgs.getSizeInBars();   // 16
        SongPart newSpt = sgs.createSongPart(r44bis, "Extra", sizeBefore, sectionC_44, false);
        sgs.addSongParts(List.of(newSpt));

        int sizeAfterAdd = sgs.getSizeInBars();
        assertTrue(sizeAfterAdd > sizeBefore);
        assertSame(newSpt, sgs.getSongPart(sizeBefore));

        undoManager.endCEdit(UNDO_EDIT);

        undoManager.undo();
        assertEquals(sizeBefore, sgs.getSizeInBars(), "Undo must restore original size");
        assertNull(sgs.getSongPart(sizeBefore), "Undo must remove the added part");
        // Existing parts must be intact
        assertEquals(0, spt0.getStartBarIndex());
        assertEquals(4, spt1.getStartBarIndex());
        assertEquals(8, spt2.getStartBarIndex());

        undoManager.redo();
        assertEquals(sizeAfterAdd, sgs.getSizeInBars(), "Redo must re-add the part");
        assertSame(newSpt, sgs.getSongPart(sizeBefore), "Redo must restore added part");
        assertSame(sgs, newSpt.getContainer(), "Container must be set after redo");
    }

    @Test
    public void testRemoveSongPartsUndoRedo()
    {
        // Remove the middle 3/4 part
        sgs.removeSongParts(List.of(spt1));

        assertEquals(12, sgs.getSizeInBars());
        assertEquals(4, spt2.getStartBarIndex());

        undoManager.endCEdit(UNDO_EDIT);

        undoManager.undo();
        assertEquals(16, sgs.getSizeInBars(), "Undo must restore full size");
        assertSame(spt1, sgs.getSongPart(4), "Undo must restore spt1 at bar 4");
        assertEquals(4, spt1.getStartBarIndex(), "Undo must restore spt1 startBarIndex");
        assertEquals(8, spt2.getStartBarIndex(), "Undo must restore spt2 startBarIndex");

        undoManager.redo();
        assertEquals(12, sgs.getSizeInBars(), "Redo must re-apply removal");
        assertEquals(4, spt2.getStartBarIndex(), "Redo must re-shift spt2");
        assertNull(sgs.getSongPart(12), "Bar 12 must not exist after redo");
    }

    // =========================================================================================================
    // Gap 4 — getUniqueRhythms implicit source rhythm scenario
    // =========================================================================================================

    @Test
    public void testGetUniqueRhythmsImplicitSourceRhythm() throws UnsupportedEditException
    {
        // Create an adapted rhythm: r34 adapted to 4/4
        Rhythm adapted_r34_to_44 = rdb.getAdaptedRhythmInstance(r34, TimeSignature.FOUR_FOUR);
        assertNotNull(adapted_r34_to_44, "Adapted rhythm must be creatable");

        // Switch spt1 from r34 to r34bis so r34 is no longer directly used in the song
        sgs.setSongPartsRhythm(List.of(spt1), r34bis, sectionB_34);
        assertFalse(sgs.getSongParts().stream().anyMatch(s -> s.getRhythm() == r34),
                "r34 must no longer be used directly");

        // Append a 4/4 part using the adapted rhythm (source = r34, now implicit)
        SongPart adaptedSpt = sgs.createSongPart(adapted_r34_to_44, "AdaptedFrom34", sgs.getSizeInBars(), sectionC_44, false);
        sgs.addSongParts(List.of(adaptedSpt));

        // getUniqueRhythms(false, false): adapted rhythm AND its implicit source r34 must both appear
        var allIncluded = sgs.getUniqueRhythms(false, false);
        assertTrue(allIncluded.contains(adapted_r34_to_44), "Must include adapted rhythm");
        assertTrue(allIncluded.contains(r34), "Must include implicit source rhythm when neither flag is set");
        // The adapted rhythm must appear just before its implicit source
        int adaptedIdx = allIncluded.indexOf(adapted_r34_to_44);
        int sourceIdx = allIncluded.indexOf(r34);
        assertEquals(adaptedIdx + 1, sourceIdx, "Implicit source must appear immediately after its adapted rhythm");

        // getUniqueRhythms(false, true): adapted rhythm included, implicit source excluded
        var excludeImplicit = sgs.getUniqueRhythms(false, true);
        assertTrue(excludeImplicit.contains(adapted_r34_to_44));
        assertFalse(excludeImplicit.contains(r34), "Must exclude implicit source when excludeImplicitSourceRhythms=true");

        // getUniqueRhythms(true, false): adapted excluded, implicit source still included
        var excludeAdapted = sgs.getUniqueRhythms(true, false);
        assertFalse(excludeAdapted.contains(adapted_r34_to_44), "Must exclude adapted rhythm");
        assertTrue(excludeAdapted.contains(r34), "Must still include implicit source when excludeAdaptedRhythms=true");
    }

    // =========================================================================================================
    // Copy-paste tests
    // =========================================================================================================

    @Test
    public void testSimulateCopyPasteSongPartAtSameBar() throws UnsupportedEditException
    {
        System.out.println("=== testSimulateCopyPasteSongPartAtSameBar()");
        var saveBarSize = sgs.getSizeInBars();
        var sptCopy = spt0.getCopy(null, spt0.getStartBarIndex(), spt0.getNbBars(), null);
        sgs.addSongParts(List.of(sptCopy));

        assertSame(spt0.getRhythm(), sptCopy.getRhythm(), "Copy must use the same rhythm");
        assertSame(spt0.getParentSection(), sptCopy.getParentSection(), "Copy must use the same name");

        assertEquals(saveBarSize + spt0.getNbBars(), sgs.getSizeInBars(), "Total size must grow by the copy's nbBars");
        assertSame(sptCopy, sgs.getSongPart(0), "Copy must be at bar 0");
        assertEquals(spt0.getNbBars(), spt0.getStartBarIndex(), "Original spt0 must have shifted to bar 4");
        assertEquals(spt0.getNbBars() * 2 + spt1.getNbBars(), spt2.getStartBarIndex(), "spt2 must have shifted by one copy's worth of bars");
    }

    // =========================================================================================================
    // Helper methods
    // =========================================================================================================

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


    private ExecutionManager getExecutionManager()
    {
        return ((SongStructureImpl) sgs).getExecutionManager();
    }

    private ReentrantReadWriteLock getLock()
    {
        return getExecutionManager().getLock();
    }
}
