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
package org.jjazz.song;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for songs with multiple different rhythm instances (multi-rhythm songs).
 * <p>
 * The fixture is an 8-bar song with two CLS sections, each using a distinct 4/4 rhythm:
 * <ul>
 * <li>SectionA (4/4, bar 0, 4 bars): spt uses r44 (default 4/4 rhythm)</li>
 * <li>SectionB (4/4, bar 4, 4 bars): spt uses r44bis (a different 4/4 rhythm, explicitly assigned)</li>
 * </ul>
 * After setUp: mapTsLastRhythm = {4/4: r44bis}.
 * <p>
 * Using only two distinct source rhythms keeps the MidiMix within the 16-channel MIDI limit (2 × 6 voices = 12). AdaptedRhythms reuse their source rhythm's
 * channels and do not count toward the limit.
 * <p>
 * Tests verify that CLS operations (add/remove section, insert/delete bars, move section, change time signature) correctly consult mapTsLastRhythm when
 * assigning rhythms to new or modified song parts, and that existing song parts retain their rhythms when they are not directly affected. Each test also
 * verifies full undo/redo correctness via the tearDown check.
 */
public class SongMultiRhythmTest
{

    private static final String UT_EDIT_NAME = "UTedit";

    Song song;
    ChordLeadSheet cls;
    ChordLeadSheet u_cls;
    CLI_SectionImpl sectionA, sectionB;
    SongStructure sgs;
    SongStructure u_sgs;
    Rhythm r44, r44bis;
    SongPart spt_A, spt_B;
    RhythmDatabase rdb;
    JJazzUndoManager undoManager;

    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    public static void setUpClass(TestInfo testInfo) throws Exception
    {
        System.out.println("\n" + testInfo.getDisplayName() + "     ########################\n");
    }

    @AfterAll
    public static void tearDownClass()
    {
    }

    /**
     * Build an 8-bar song with two 4/4 sections, then override SectionB's rhythm to r44bis so that both sections use distinct rhythms. State is captured BEFORE
     * undo listeners are registered, so the setUp operations are not undoable.
     */
    @BeforeEach
    public void setUp(TestInfo testInfo) throws UnsupportedEditException, ParseException, UnavailableRhythmException
    {
        System.out.println(testInfo.getDisplayName() + " ------");

        rdb = RhythmDatabase.getSharedInstance();
        
        // CLS: 8 bars
        // bar 0: SectionA 4/4 (4 bars)
        // bar 4: SectionB 4/4 (4 bars)
        var clsImpl = new ChordLeadSheetImpl("SectionA", TimeSignature.FOUR_FOUR, 8);
        sectionA = (CLI_SectionImpl) clsImpl.getSection(0);
        sectionB = new CLI_SectionImpl("SectionB", TimeSignature.FOUR_FOUR, 4);
        clsImpl.addSection(sectionB);
        cls = clsImpl;

        // Create song — also creates SongStructure with the SongInternalUpdater wired to cls.
        song = SongFactory.getDefault().createSong("TestSong", cls);
        sgs = song.getSongStructure();

        // Collect initial song parts and their (default) rhythms.
        spt_A = sgs.getSongPart(0);
        spt_B = sgs.getSongPart(4);
        r44 = spt_A.getRhythm();    // default 4/4 rhythm

        // Locate a second, distinct 4/4 rhythm instance.
        var ri44bis = rdb.getRhythms(TimeSignature.FOUR_FOUR).stream()
                .filter(ri -> !ri.equals(rdb.getRhythm(r44.getUniqueId())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Need at least 2 distinct 4/4 rhythms in the test database"));
        r44bis = rdb.getRhythmInstance(ri44bis);


        // Assign r44bis to spt_B *before* any undo listeners are registered, so this change is not undoable.
        // After this call: mapTsLastRhythm = {4/4: r44bis}
        sgs.setSongPartsRhythm(List.of(spt_B), r44bis, null);

        // Capture initial state used by tearDown() to verify full undo/redo correctness.
        u_cls = cls.getDeepCopy();
        u_sgs = sgs.getDeepCopy(cls);

        // Register undo listeners only now, so only test operations are undoable.
        undoManager = new JJazzUndoManager();
        cls.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(cls, undoManager);
        song.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(song, undoManager);
        sgs.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(sgs, undoManager);

        undoManager.startCEdit(UT_EDIT_NAME);
    }

    @AfterEach
    public void tearDown()
    {
        if (undoManager.getCurrentCEditName() == null)
        {
            return;
        }

        undoManager.endCEdit(UT_EDIT_NAME);
        undoAll();
        redoAll();
        undoAll();
        boolean b1 = cls.equals(u_cls);
        boolean b2 = sgs.equals(u_sgs);
        if (!b1)
        {
            System.out.println("\nUNDO MISMATCH u_cls=" + u_cls.toDebugString());
            System.out.println("cls after Undo ALL=" + cls.toDebugString());

        }
        if (!b2)
        {
            System.out.println("\nUNDO MISMATCH u_sgs=" + u_sgs);
            System.out.println("sgs after Undo ALL=" + sgs);

        }
        assertTrue(b1 && b2, "b1=" + b1 + " b2=" + b2);
    }

    // =============================================================================================
    // Tests: new section picks up the correct rhythm from mapTsLastRhythm
    // =============================================================================================

    /**
     * Adding a 4/4 section must use r44bis (the last-used 4/4 rhythm from mapTsLastRhythm), not the default r44.
     */
    @Test
    public void testAddSection4_4UsesLastUsedRhythm() throws UnsupportedEditException
    {
        // Add a 4/4 section at bar 2 (within SectionA's bars 0-3).
        // The SongInternalUpdater calls getRecommendedRhythm(4/4, …) which returns r44bis because
        // mapTsLastRhythm[4/4] = r44bis.
        var newSection = new CLI_SectionImpl("SectionD", TimeSignature.FOUR_FOUR, 2);
        cls.addSection(newSection);

        // After the add+resize: SectionD spt appears at bar 2; SectionA spt is resized to 2 bars.
        // SectionB (bar 4) is unaffected.
        var newSpt = sgs.getSongPart(2);
        assertNotNull(newSpt, "A new song part for SectionD must exist at bar 2");
        assertSame(newSection, newSpt.getParentSection());
        assertSame(r44bis, newSpt.getRhythm(),
                "New 4/4 section must inherit the last-used 4/4 rhythm (r44bis), not the default r44");

        // Existing section must be unaffected
        assertSame(r44bis, sgs.getSongPart(4).getRhythm(), "spt_B must remain r44bis");
    }

    /**
     * Adding a section with an entirely new time signature (5/4, absent from mapTsLastRhythm) falls back to adapting the adjacent song part's rhythm. The
     * section is added at bar 6 (within SectionB), so the adjacent spt is spt_B (r44bis). Because AdaptedRhythm reuses the source's RhythmVoice instances, it
     * does not allocate additional MIDI channels.
     */
    @Test
    public void testAddSection5_4AdaptsFromAdjacentRhythm() throws UnsupportedEditException
    {
        // Add a 5/4 section at bar 6 (within SectionB's bars 4-7).
        // No 5/4 in mapTsLastRhythm → getRecommendedRhythm looks at the song part at sptInsertionBar-1,
        // which resolves to spt_B (r44bis). The database adapts r44bis to 5/4; if that fails it falls
        // back to the default 5/4.
        var newSection = new CLI_SectionImpl("SectionD", TimeSignature.FIVE_FOUR, 6);
        cls.addSection(newSection);

        // After the add+resize: new spt is at bar 6; spt_B is resized to 2 bars (4-5).
        var newSpt = sgs.getSongPart(6);
        assertNotNull(newSpt, "A new song part for SectionD must exist at bar 6");
        assertEquals(TimeSignature.FIVE_FOUR, newSpt.getRhythm().getTimeSignature(),
                "New 5/4 section must have a FIVE_FOUR rhythm");

        // If the database produced an AdaptedRhythm, it must be derived from r44bis (not r44).
        if (newSpt.getRhythm() instanceof AdaptedRhythm ar)
        {
            assertSame(r44bis, ar.getSourceRhythm(),
                    "The 5/4 AdaptedRhythm must be derived from adjacent spt_B's r44bis, not the default r44");
        }

        // The MidiMix must NOT have grown: AdaptedRhythm reuses its source rhythm's channels,
        // so getUniqueRhythms() must still contain exactly {r44, r44bis} and the used-channel
        // count must be unchanged (r44.voices + r44bis.voices = 12).
        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);
        var uniqueRhythms = midiMix.getUniqueRhythms();
        assertTrue(uniqueRhythms.contains(r44), "r44 must still be in MidiMix after adding AdaptedRhythm");
        assertTrue(uniqueRhythms.contains(r44bis), "r44bis must still be in MidiMix after adding AdaptedRhythm");
        assertEquals(r44.getRhythmVoices().size() + r44bis.getRhythmVoices().size(),
                midiMix.getUsedChannels().size(),
                "Adding an AdaptedRhythm must not increase the number of used MIDI channels");

        // spt_A must be unaffected
        assertSame(r44, sgs.getSongPart(0).getRhythm(), "spt_A must remain r44");

//        System.out.println("sgs=");
//        System.out.println(Utilities.toMultilineString(sgs.getSongParts(), "  "));
    }

    // =============================================================================================
    // Tests: changing a section's time signature picks the correct rhythm
    // =============================================================================================

    /**
     * Changing sectionB (4/4, r44bis) to 3/4 must assign a 3/4 rhythm. Since mapTsLastRhythm has no 3/4 entry, the algorithm falls back to the default 3/4
     * rhythm. This verifies that the section's previous rhythm (r44bis) is NOT reused after the TS change, and that mapTsLastRhythm[4/4] = r44bis is preserved
     * by the remaining spt_A.
     */
    @Test
    public void testChangeSectionTimeSig4_4To3_4() throws UnsupportedEditException
    {
        // spt_B currently has r44bis (4/4). After setSectionTimeSignature(sectionB, 3/4):
        // getRecommendedRhythm(3/4, 4): no 3/4 in mapTsLastRhythm → fall back to default 3/4 rhythm.
        cls.setSectionTimeSignature(sectionB, TimeSignature.THREE_FOUR);

        var updatedSptB = sgs.getSongParts(spt -> spt.getParentSection() == sectionB).get(0);
        assertEquals(TimeSignature.THREE_FOUR, updatedSptB.getRhythm().getTimeSignature(),
                "After changing sectionB to 3/4, its spt must have a 3/4 rhythm");
        assertNotSame(r44bis, updatedSptB.getRhythm(),
                "The old r44bis must not be reused after a TS change");

        // spt_A is unaffected and still tracks the 4/4 last-used rhythm.
        assertSame(r44, sgs.getSongParts(spt -> spt.getParentSection() == sectionA).get(0).getRhythm(),
                "spt_A must remain r44");
    }

    // =============================================================================================
    // Tests: CLS structural operations preserve existing song part rhythms
    // =============================================================================================

    /**
     * Deleting bars within one section (sectionA) must not affect the rhythms of other sections.
     */
    @Test
    public void testDeleteBarsPreservesMultipleRhythms()
    {
        // Delete bars 1 and 2 (within sectionA's bars 0-3, crossing no section boundary).
        // After deletion: sectionA shrinks to 2 bars; sectionB shifts down 2 bars.
        cls.deleteBars(1, 2);

        var sptsB = sgs.getSongParts(spt -> spt.getParentSection() == sectionB);
        assertFalse(sptsB.isEmpty(), "sectionB must still have a song part after deleteBars");
        assertSame(r44bis, sptsB.get(0).getRhythm(), "spt_B must retain r44bis after deleteBars");
    }

    /**
     * Inserting bars within one section (sectionA) must not affect the rhythms of other sections.
     */
    @Test
    public void testInsertBarsPreservesMultipleRhythms()
    {
        // Insert 2 bars at bar 1 (within sectionA's bars 0-3, crossing no section boundary).
        // After insertion: sectionA grows to 6 bars; sectionB shifts to bar 6.
        cls.insertBars(1, 2);

        var sptsB = sgs.getSongParts(spt -> spt.getParentSection() == sectionB);
        assertFalse(sptsB.isEmpty(), "sectionB must still have a song part after insertBars");
        assertSame(r44bis, sptsB.get(0).getRhythm(), "spt_B must retain r44bis after insertBars");
    }

    /**
     * Removing a section must preserve the rhythm of all remaining song parts. Additionally, mapTsLastRhythm must retain the entry for the removed section's
     * rhythm.
     */
    @Test
    public void testRemoveSectionPreservesOtherRhythms()
    {
        // Remove sectionB: sectionA absorbs its bars (now 8 bars), spt_B is dropped.
        cls.removeSection(sectionB);

        assertEquals(1, sgs.getSongParts().size(),
                "Only spt_A should remain after removing sectionB");

        var sptsA = sgs.getSongParts(spt -> spt.getParentSection() == sectionA);
        assertFalse(sptsA.isEmpty());
        assertSame(r44, sptsA.get(0).getRhythm(), "spt_A must retain r44 after removing sectionB");

        // mapTsLastRhythm must preserve r44bis — the removal does not clear it.
        assertSame(r44bis, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR),
                "mapTsLastRhythm[4/4] must still be r44bis even after its only user (spt_B) is removed");
    }

    /**
     * Moving a section (a "small move" that only resizes adjacent song parts, no section boundary crossed) must preserve the moved section's rhythm.
     */
    @Test
    public void testMoveSectionPreservesRhythm()
    {
        // Move sectionB from bar 4 to bar 6 (a "small" move: bar 5 still belongs to sectionA).
        // The SongInternalUpdater resizes spt_A and spt_B without recalculating their rhythms.
        cls.moveSection(sectionB, 6);

        var sptsB = sgs.getSongParts(spt -> spt.getParentSection() == sectionB);
        assertFalse(sptsB.isEmpty());
        assertSame(r44bis, sptsB.get(0).getRhythm(), "spt_B must retain r44bis after moveSection");
    }

    // =============================================================================================
    // Tests: undo/redo correctness with multiple rhythms
    // =============================================================================================

    /**
     * Changing spt_B's rhythm via setSongPartsRhythm and then undoing must restore both the song part's rhythm and the mapTsLastRhythm entry to their
     * pre-change values.
     */
    @Test
    public void testSetRhythmUndoRedoRestoresLastUsedRhythm() throws UnsupportedEditException
    {
        // Before: spt_B = r44bis, mapTsLastRhythm[4/4] = r44bis.
        // Change spt_B to r44 — updates mapTsLastRhythm[4/4] = r44.
        sgs.setSongPartsRhythm(List.of(spt_B), r44, null);
        assertSame(r44, spt_B.getRhythm(), "After change, spt_B should use r44");
        assertSame(r44, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR),
                "mapTsLastRhythm[4/4] must reflect the new r44 after the change");

        undoManager.endCEdit(UT_EDIT_NAME);

        // Undo must restore spt_B = r44bis AND mapTsLastRhythm[4/4] = r44bis.
        undoManager.undo();
        assertSame(r44bis, spt_B.getRhythm(), "Undo must restore spt_B to r44bis");
        assertSame(r44bis, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR),
                "Undo must restore mapTsLastRhythm[4/4] to r44bis");

        // Redo must re-apply r44 and mapTsLastRhythm[4/4] = r44.
        undoManager.redo();
        assertSame(r44, spt_B.getRhythm(), "Redo must re-apply r44 on spt_B");
        assertSame(r44, sgs.getLastUsedRhythm(TimeSignature.FOUR_FOUR),
                "Redo must restore mapTsLastRhythm[4/4] to r44");

        undoManager.startCEdit(UT_EDIT_NAME);
    }

    /**
     * After changing sectionB's time signature from 4/4 to 3/4 (spt_B gets the default 3/4 rhythm), undo must restore the original r44bis AND redo must
     * re-apply the 3/4 rhythm.
     */
    @Test
    public void testChangeSectionTimeSigUndoRedoMultiRhythm() throws UnsupportedEditException
    {
        // Change sectionB from 4/4 to 3/4; spt_B gets the default 3/4 rhythm (no 3/4 in mapTsLastRhythm).
        cls.setSectionTimeSignature(sectionB, TimeSignature.THREE_FOUR);
        var sptB_after = sgs.getSongParts(spt -> spt.getParentSection() == sectionB).get(0);
        assertEquals(TimeSignature.THREE_FOUR, sptB_after.getRhythm().getTimeSignature(),
                "spt_B must have a 3/4 rhythm after TS change");
        assertNotSame(r44bis, sptB_after.getRhythm(), "spt_B must no longer use r44bis after TS change");

        undoManager.endCEdit(UT_EDIT_NAME);

        undoManager.undo();
        var sptB_afterUndo = sgs.getSongParts(spt -> spt.getParentSection() == sectionB).get(0);
        assertSame(r44bis, sptB_afterUndo.getRhythm(), "Undo must restore spt_B to r44bis");
        assertEquals(TimeSignature.FOUR_FOUR, sptB_afterUndo.getRhythm().getTimeSignature(),
                "Undo must restore 4/4 time signature on spt_B's rhythm");

        undoManager.redo();
        var sptB_afterRedo = sgs.getSongParts(spt -> spt.getParentSection() == sectionB).get(0);
        assertEquals(TimeSignature.THREE_FOUR, sptB_afterRedo.getRhythm().getTimeSignature(),
                "Redo must re-apply 3/4 rhythm on spt_B");
        assertNotSame(r44bis, sptB_afterRedo.getRhythm(), "Redo must not restore r44bis on spt_B");

        undoManager.startCEdit(UT_EDIT_NAME);
    }

    // =============================================================================================
    // Helper methods
    // =============================================================================================

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
