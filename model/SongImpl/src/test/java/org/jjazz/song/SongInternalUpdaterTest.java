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
package org.jjazz.song;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.SectionAddedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.chordleadsheet.item.CLI_ChordSymbolImpl;
import org.jjazz.chordleadsheet.spi.item.CLI_Factory;
import org.jjazz.harmony.api.Position;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.openide.util.Exceptions;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.TestInfo;

public class SongInternalUpdaterTest
{

    private static final String UT_EDIT_NAME = "UTedit";
    Song song;
    ChordLeadSheet cls1, u_cls1;
    CLI_ChordSymbol newChord;
    CLI_Section newSection1_44, newSection2_34, newSection3_54;
    CLI_Section section1_44, section2_34, section3_44;
    SongStructure sgs;
    SongStructure u_sgs;
    RhythmDatabase rdb;
    Rhythm r44, r44bis;
    SongPart spt0, spt1, spt2, spt3, spt4;
    SongPart u_spt0;
    SongPart u_spt1, u_spt2, u_spt3, u_spt4;
    JJazzUndoManager undoManager;


    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    public SongInternalUpdaterTest()
    {

    }

    @BeforeAll
    public static void setUpClass(TestInfo testInfo) throws Exception
    {
        System.out.println("\n" + testInfo.getDisplayName() + "     ########################\n");
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }


    @BeforeEach
    public void setUp(TestInfo testInfo) throws UnsupportedEditException, ParseException
    {
        System.out.println(testInfo.getDisplayName() + " ------");
        rdb = RhythmDatabase.getSharedInstance();


        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        section1_44 = (CLI_SectionImpl) cls1.getSection(0);
        undoManager = new JJazzUndoManager();
        try
        {
            cls1.setSizeInBars(8);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Dm7"), new Position(0)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("F#7"), new Position(1)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Bbmaj7#5"), new Position(1, 3)));
            section2_34 = new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2);
            cls1.addSection(section2_34);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("D7b9b5"), new Position(2)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("FM7#11"), new Position(4, 1)));
            section3_44 = new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5);
            cls1.addSection(section3_44);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Db"), new Position(7, 3f)));

            cls1.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(cls1, undoManager);

            // Copy for undo/redo test
            u_cls1 = cls1.getDeepCopy();

            // Extra items to play with
            newChord = new CLI_ChordSymbolImpl(ExtChordSymbol.get("A"), new Position(2, 1));
            newSection1_44 = new CLI_SectionImpl("NewSECTION1", TimeSignature.FOUR_FOUR, 4);
            newSection2_34 = new CLI_SectionImpl("NewSECTION2", TimeSignature.THREE_FOUR, 6);
            newSection3_54 = new CLI_SectionImpl("NewSECTION3", TimeSignature.FIVE_FOUR, 7);


            // Song structure
            song = SongFactory.getDefault().createSong("testSong", cls1);
            song.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(song, undoManager);

            sgs = song.getSongStructure();
            r44 = sgs.getSongParts().getFirst().getRhythm();   // bossa mock
            Rhythm r34 = sgs.getSongParts().get(1).getRhythm();
            spt0 = sgs.createSongPart(r34, null, 8, section2_34, true);
            sgs.addSongParts(List.of(spt0));


            // Other rhythm instance
            try
            {
                var r44All = rdb.getRhythms(TimeSignature.FOUR_FOUR);
                var ri44bis = r44All.stream().filter(ri -> ri != rdb.getRhythm(r44.getUniqueId())).toList().get(0);
                r44bis = rdb.getRhythmInstance(ri44bis);            // jazz mock
            } catch (UnavailableRhythmException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            

            // Copy for undo/redo test
            u_sgs = SongFactory.getDefault().createSongStructure(cls1);
            u_spt0 = spt0.getCopy(null, spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
            u_sgs.addSongParts(List.of(u_spt0));
        } catch (ParseException ex)
        {
            throw new IllegalStateException("ParseException ex=" + ex);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        sgs.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(sgs, undoManager);

//        System.out.println("\n ==== SETUP cls1=" + cls1.toDebugString());
//        System.out.println(" ==== SETUP   sgs Before=" + Utilities.toMultilineString(sgs.getSongParts()));
//        System.out.println(" ==== SETUP u_sgs Before=" + Utilities.toMultilineString(u_sgs.getSongParts()));

        undoManager.startCEdit(UT_EDIT_NAME);
    }

    @AfterEach
    public void tearDown(TestInfo testInfo)
    {
        if (undoManager.getCurrentCEditName() == null)
        {
            return;
        }
        undoManager.endCEdit(UT_EDIT_NAME);
        undoAll();
        redoAll();
        undoAll();
        boolean b1 = cls1.equals(u_cls1);
        boolean b2 = sgs.equals(u_sgs);
        if (!b1)
        {
            System.out.println("UNDO MISMATCH CLS");
            System.out.println("  u_cls1=" + u_cls1.toDebugString());
            System.out.println("    cls1=" + cls1.toDebugString());
        }
        if (!b2)
        {
            System.out.println("UNDO MISMATCH SGS");
            System.out.println("  u_sgs=" + u_sgs.toString());
            System.out.println("    sgs=" + sgs.toString());
        }
        assertTrue(b1);
        assertTrue(b2);
    }

    @Test
    public void testInsertAtBar0() throws UnsupportedEditException
    {

        cls1.deleteBars(0, 1);      // So that section2_34, which is used by 2 song parts, becomes the init section
        assertEquals(section2_34, cls1.getSection(0));
        assertEquals(9, sgs.getSizeInBars());
        var saveSptSection2 = sgs.getSongPart(0);
        String saveSection2Name = section2_34.getData().getName();
        var saveSection2ChordSymbols = cls1.getItems(section2_34, CLI_ChordSymbol.class);

        cls1.insertBars(0, 1);
        // System.out.println(" sgs after=" + sgs);

        var s0 = cls1.getSection(0);
        var s1 = cls1.getSection(1);
        assertEquals(10, sgs.getSizeInBars());
        assertEquals(saveSection2Name, s1.getData().getName());
        assertSame(s0, sgs.getSongPart(0).getParentSection());
        assertSame(s1, sgs.getSongPart(1).getParentSection());
        assertEquals(saveSptSection2.getNbBars(), sgs.getSongPart(1).getNbBars());
        assertSame(saveSptSection2.getRhythm(), sgs.getSongPart(1).getRhythm());
        assertSame(s1, sgs.getSongPart(9).getParentSection());
        assertEquals(saveSptSection2.getNbBars(), sgs.getSongPart(9).getNbBars());
        assertSame(saveSptSection2.getRhythm(), sgs.getSongPart(9).getRhythm());
        assertEquals(saveSection2ChordSymbols, cls1.getItems(s1, CLI_ChordSymbol.class));
    }

    @Test
    public void testAddAndRemove()
    {

        assertEquals(11, sgs.getSizeInBars());
        cls1.addItem(newChord);
        // System.out.println(" sgs after=" + sgs);
        assertEquals(11, sgs.getSizeInBars());
        assertEquals(3, sgs.getSongParts().get(1).getNbBars());
        // System.out.println("\n== Test testAddAndRemove add section");
        try
        {
            cls1.addSection(newSection1_44);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println(" sgs after=" + sgs);
        assertNotEquals(null, cls1.getSection(newSection1_44.getData().getName()));
        assertSame(newSection1_44, sgs.getSongParts().get(2).getParentSection());
        assertEquals(1, sgs.getSongParts().get(2).getNbBars());
        assertEquals(2, sgs.getSongParts().get(1).getNbBars());
        assertEquals(2, sgs.getSongParts().get(4).getNbBars());
        // System.out.println("\n== Test testAddAndRemove removeSection");
        assertEquals(10, sgs.getSizeInBars());
        cls1.removeSection(section2_34);
        // System.out.println(" sgs after=" + sgs);
        assertEquals(8, sgs.getSizeInBars());
        assertEquals(3, sgs.getSongParts().size());
        assertNotSame(section2_34, sgs.getSongParts().get(1).getParentSection());
        assertEquals(4, sgs.getSongParts().get(1).getStartBarIndex());
        assertEquals(5, sgs.getSongParts().get(2).getStartBarIndex());
    }

    @Test
    public void testAdd2()
    {

        assertTrue(sgs.getSizeInBars() == 11);
        sgs.removeSongParts(List.of(sgs.getSongParts().get(1)));
        // System.out.println(" sgs after(1)=" + sgs);
        try
        {
            cls1.addSection(newSection1_44);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println(" sgs after(2)=" + sgs);
        assertEquals(8, sgs.getSizeInBars());
        assertSame(newSection1_44, sgs.getSongParts().get(3).getParentSection());
        assertEquals(7, sgs.getSongParts().get(3).getStartBarIndex());
    }

    @Test
    public void testAdd3()
    {

        assertTrue(sgs.getSizeInBars() == 11);
        sgs.removeSongParts(List.of(sgs.getSongParts().get(2)));
        // System.out.println(" sgs after(1)=" + sgs);
        try
        {
            cls1.addSection(newSection2_34);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println(" sgs after(2)=" + sgs);
        assertEquals(10, sgs.getSizeInBars());
        assertSame(newSection2_34, sgs.getSongParts().get(3).getParentSection());
        assertEquals(8, sgs.getSongParts().get(3).getStartBarIndex());
    }

    @Test
    public void testAddAdaptedRhythm()
    {

        assertTrue(sgs.getSizeInBars() == 11);
        try
        {
            cls1.addSection(newSection3_54);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println("rdb=" + RhythmDatabase.getSharedInstance().toString());
        // System.out.println(" sgs after(1)=" + Utilities.toMultilineString(sgs.getSongParts()));
        assertTrue(sgs.getSizeInBars() == 11);
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection3_54);
        Rhythm r = sgs.getSongParts().get(3).getRhythm();
        assertTrue(r instanceof AdaptedRhythm);
        assertTrue(((AdaptedRhythm) r).getSourceRhythm() == sgs.getSongParts().get(2).getRhythm());
    }

    @Test
    public void testChangeTimeSignature()
    {
        try
        {
            cls1.setSectionTimeSignature(section2_34, TimeSignature.FOUR_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println(" sgs after=" + Utilities.toMultilineString(sgs.getSongParts()));
        assertSame(TimeSignature.FOUR_FOUR, sgs.getSongParts().get(1).getRhythm().getTimeSignature());
        undoManager.endCEdit(UT_EDIT_NAME);
        undoManager.undo();
        assertSame(TimeSignature.THREE_FOUR, sgs.getSongParts().get(1).getRhythm().getTimeSignature());
        undoManager.redo();
        assertSame(TimeSignature.FOUR_FOUR, sgs.getSongParts().get(1).getRhythm().getTimeSignature());
        undoManager.startCEdit(UT_EDIT_NAME);
    }

    @Test
    public void testRhythmDivisionChange() throws ParseException, UnsupportedEditException, UnavailableRhythmException
    {
        var spt = sgs.getSongPart(5);
        assertSame(section3_44, spt.getParentSection());       // Section 4/4 bar 5
        var rBinary = spt.getRhythm(); 
        assertTrue(rBinary.getFeatures().division().isBinary());
        Rhythm rTernary = getRhythm(TimeSignature.FOUR_FOUR, Division.EIGHTH_SHUFFLE);

        CLI_ChordSymbol chord1 = CLI_Factory.getDefault().createChordSymbol("A", new Position(5, 1.75f)); // throws ParseException
        CLI_ChordSymbol chord2 = CLI_Factory.getDefault().createChordSymbol("A", new Position(5, 3.5f)); // throws ParseException;

        cls1.addItem(chord1);
        cls1.addItem(chord2);

        // Change rhythm, should impact off-beat chords position.
        // Switch ALL binary 4/4 spts at once: remaining = r34 only = 12 channels total, within 16-channel limit.
        var binarySpts = sgs.getSongParts().stream().filter(s -> s.getRhythm().equals(rBinary)).toList();
        sgs.setSongPartsRhythm(binarySpts, rTernary, null);
        assertEquals(1.66666f, chord1.getPosition().getBeat(), 0.001f);
        assertEquals(3.66666f, chord2.getPosition().getBeat(), 0.001f);

        undoManager.endCEdit(UT_EDIT_NAME);
        undoManager.undo();
        undoManager.startCEdit(UT_EDIT_NAME);

        // Readd both chords 
        cls1.addItem(chord1);
        cls1.addItem(chord2);
        assertEquals(1.75f, chord1.getPosition().getBeat(), 0);
        assertEquals(3.5f, chord2.getPosition().getBeat(), 0);

        // Move chord2 so that there should be a collision at 1.6666 after adjustment to ternary
        cls1.moveItem(chord2, new Position(5, 1.5f));
        var binarySpts2 = sgs.getSongParts().stream().filter(s -> s.getRhythm().equals(rBinary)).toList();
        sgs.setSongPartsRhythm(binarySpts2, rTernary, null);    // Switch back to ternary
        assertEquals(1.75f, chord1.getPosition().getBeat(), 0.001f);        // Could not be moved
        assertEquals(1.66666f, chord2.getPosition().getBeat(), 0.001f);

        undoManager.endCEdit(UT_EDIT_NAME);
        undoManager.undo();
        undoManager.startCEdit(UT_EDIT_NAME);

        // System.out.println(" sgs after=" + sgs);
    }


    @Test
    public void testChangeTimeSignaturePlusDivisionChange() throws UnavailableRhythmException, UnsupportedEditException
    {


        // spt0 (bar 8, r34) must be removed first: with 6-voice rhythms, having r44+r34+rWaltzSwing would exceed the 16-channel MIDI limit.
        sgs.removeSongParts(List.of(spt0));

        var rWaltzSwing = getRhythm(TimeSignature.THREE_FOUR, Division.EIGHTH_SHUFFLE);

        // Use waltz swing for the existing 3/4 section, so that changing section3_44 to 3/4 will reuse the same rhythm
        var spt = sgs.getSongParts().get(1);
        sgs.setSongPartsRhythm(List.of(spt), rWaltzSwing, null);


        var chord1 = cls1.getBarFirstItem(5, CLI_ChordSymbol.class, cli -> true);   // bar 5 beat 0.75
        // Now we can change section3_44, it should switch also to rWaltzSwing, then adjust position of chord1
        try
        {
            cls1.setSectionTimeSignature(section3_44, TimeSignature.THREE_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        assertSame(rWaltzSwing, sgs.getSongParts().get(2).getRhythm());
        assertEquals(0.666f, chord1.getPosition().getBeat(), 0.001f);

        // System.out.println(" sgs after=" + sgs);
    }

    @Test
    public void testMoveSection()
    {

        cls1.moveSection(section2_34, 1);
        // System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().get(1).getStartBarIndex() == 1);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);
        assertTrue(sgs.getSongParts().get(3).getNbBars() == 4);
    }

    @Test
    public void testMoveSectionBig()
    {

        var lastChord = cls1.getItems(CLI_ChordSymbol.class).getLast();
        assertEquals(3, lastChord.getPosition().getBeat());

        cls1.moveSection(section2_34, 6);
        // System.out.println(" sgs after=" + sgs);
        assertEquals(6, section2_34.getPosition().getBar());
        assertEquals(2, lastChord.getPosition().getBeat(), "chord symbol was beat 3 in 4/4, now we're in 3/4");

        assertTrue(sgs.getSongParts().size() == 3);
        assertEquals(6, sgs.getSongParts().get(2).getStartBarIndex());
        assertEquals(2, sgs.getSongParts().get(2).getNbBars());

    }

    @Test
    public void testResize()
    {

        cls1.setSizeInBars(10);
        // System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 13);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 5);
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 10);
    }

    @Test
    public void testRemoveSection()
    {

        cls1.removeSection(section2_34);
        // System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 2);
        assertTrue(sgs.getSongParts().get(0).getNbBars() == 5);
        assertTrue(sgs.getSongParts().get(1).getParentSection() == section3_44);
        assertTrue(sgs.getSizeInBars() == 8);
    }

    @Test
    public void testRemoveBars()
    {

        cls1.deleteBars(4, 5);
        // System.out.println(" cls1 after=" + cls1.toDebugString());
        // System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 6);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 4);
    }

    @Test
    public void testRemoveInitialBarWithSectionOnBar1()
    {

        CLI_Section newSection = new CLI_SectionImpl("NewSection", TimeSignature.THREE_FOUR, 1);
        try
        {
            cls1.addSection(newSection);
            assertTrue(sgs.getSongParts().size() == 5 && sgs.getSizeInBars() == 11);
            assertTrue(sgs.getSongParts().get(1).getParentSection() == newSection);
//            System.out.println(" cls1 after=" + cls1.toDebugString());
//            System.out.println(" sgs after=" + sgs);
            cls1.deleteBars(0, 0);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println(" cls1 after=" + cls1.toDebugString());
        // System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 4 && sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(0).getParentSection() == newSection);
    }

    @Test
    public void testRemoveAdaptedRhythm()
    {

        try
        {
            cls1.addSection(newSection3_54);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertTrue(sgs.getSongParts().size() == 5);
        cls1.deleteBars(6, 7);
        // System.out.println(" cls1 after=" + cls1.toDebugString());
        // System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 4);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 1);
    }

    @Test
    public void testUserPhraseAddRemoveUpdatesMidiMixUserChannel() throws UnsupportedEditException
    {

        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);

        String name = "phrase1";
        assertNull(midiMix.getUserRhythmVoice(name));

        song.setUserPhrase(name, new Phrase(0));
        assertNotNull(midiMix.getUserRhythmVoice(name));

        song.removeUserPhrase(name);
        assertNull(midiMix.getUserRhythmVoice(name));
    }

    @Test
    public void testUserPhraseRenameUpdatesMidiMixUserChannel() throws UnsupportedEditException
    {

        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);

        String oldName = "phrase1";
        String newName = "phrase2";
        song.setUserPhrase(oldName, new Phrase(0));

        var oldUrv = midiMix.getUserRhythmVoice(oldName);
        assertNotNull(oldUrv);
        int channel = midiMix.getChannel(oldUrv);

        song.renameUserPhrase(oldName, newName);

        assertNull(midiMix.getUserRhythmVoice(oldName));
        var newUrv = midiMix.getUserRhythmVoice(newName);
        assertNotNull(newUrv);
        assertEquals(channel, midiMix.getChannel(newUrv));
    }

    @Test
    public void testUserPhrasePreCheckVetoesIfNoMidiChannelAvailable() throws UnsupportedEditException
    {

        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);

        int i = 0;
        while (!midiMix.getUnusedChannels().isEmpty())
        {
            midiMix.addUserChannel("fill" + i++, false);
        }

        assertThrows(UnsupportedEditException.class, () -> song.setUserPhrase("phrase1", new Phrase(0)));
    }

    @Test
    public void testUserPhrasePreCheckVetoesIfSameNameUserChannelExists() throws UnsupportedEditException
    {

        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);

        midiMix.addUserChannel("phrase1", false);
        assertThrows(UnsupportedEditException.class, () -> song.setUserPhrase("phrase1", new Phrase(0)));
    }

    @Test
    public void testSptAddedAndRemovedUpdatesMidiMixRhythms() throws UnavailableRhythmException, UnsupportedEditException
    {

        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);

        // Remove both r34 song parts so the MidiMix has only r44
        var sptSection2 = sgs.getSongParts().get(1);  // section2_34 spt at bar 2 (r34)
        sgs.removeSongParts(List.of(sptSection2));
        sgs.removeSongParts(List.of(spt0));           // spt0 also uses r34 (bar index shifted after previous removal)

        var initialRhythms = new HashSet<>(midiMix.getUniqueRhythms());

        Rhythm newRhythm = findAdditionalRhythm(TimeSignature.FOUR_FOUR, midiMix);
        assertFalse(midiMix.getUniqueRhythms().contains(newRhythm));

        SongPart sptNew = sgs.createSongPart(newRhythm, null, sgs.getSizeInBars(), section1_44, true);
        sgs.addSongParts(List.of(sptNew));
        assertTrue(midiMix.getUniqueRhythms().contains(newRhythm));

        sgs.removeSongParts(List.of(sptNew));
        assertEquals(initialRhythms, midiMix.getUniqueRhythms());
    }

    @Test
    public void testChangeSptRhythm() throws UnavailableRhythmException, UnsupportedEditException
    {
        // Remove all 3/4 SongParts
        var spts34 = sgs.getSongParts(spt -> spt.getRhythm().getTimeSignature() == TimeSignature.THREE_FOUR);
        sgs.removeSongParts(spts34);

        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);
        assertFalse(midiMix.getUniqueRhythms().contains(r44bis));

        var spt = sgs.getSongPart(0);
        sgs.setSongPartsRhythm(List.of(spt), r44bis, null);
        assertTrue(midiMix.getUniqueRhythms().contains(r44bis));
    }

    @Test
    public void testGetDerivedOperationsNoForwardDuringUndoRedo()
    {

        SongInternalUpdater updater = new SongInternalUpdater(song);

        SongPropertyChangeEvent evt = new SongPropertyChangeEvent(song, Song.PROP_USER_PHRASE, new Phrase(0), "phrase1");
        evt.setIsUndo();

        var ops = updater.getDerivedOperations(WriteOperationResults.of(evt, null));
        assertTrue(ops.isEmpty());

        SectionAddedEvent clsEvt = new SectionAddedEvent(cls1, new CLI_SectionImpl("Tmp", TimeSignature.FOUR_FOUR, 1), null);
        clsEvt.setIsRedo();
        var ops2 = updater.getDerivedOperations(WriteOperationResults.of(clsEvt, null));
        assertTrue(ops2.isEmpty());

        var sgsEvt = new SptAddedEvent(sgs, List.of(sgs.getSongPart(0)));
        sgsEvt.setIsUndo();
        var ops3 = updater.getDerivedOperations(WriteOperationResults.of(sgsEvt, null));
        assertTrue(ops3.isEmpty());
    }

    @Test
    public void testReplaceSectionSameBarRenamesOnlyDefaultNamedSongParts() throws UnsupportedEditException
    {

        var sptsSection2 = sgs.getSongParts(spt -> spt.getParentSection() == section2_34);
        assertFalse(sptsSection2.isEmpty());

        SongPart customizedSpt = sptsSection2.get(0);
        sgs.setSongPartsName(List.of(customizedSpt), "custom");

        CLI_Section replaced = new CLI_SectionImpl("ReplacedSection2", TimeSignature.THREE_FOUR, section2_34.getPosition().getBar());
        cls1.addSection(replaced);

        assertSame(replaced, cls1.getSection(section2_34.getPosition().getBar()));
        assertTrue(sgs.getSongParts(spt -> spt.getParentSection() == section2_34).isEmpty());

        var newParentSpts = sgs.getSongParts(spt -> spt.getParentSection() == replaced);
        assertEquals(sptsSection2.size(), newParentSpts.size());
        assertEquals(1, newParentSpts.stream().filter(spt -> spt.getName().equals("custom")).count());
        assertEquals(sptsSection2.size() - 1, newParentSpts.stream().filter(spt -> spt.getName().equals("ReplacedSection2")).count());
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

    private Rhythm getRhythm(TimeSignature ts, Division d) throws UnavailableRhythmException
    {
        var rInfo = rdb.getRhythms(ts).stream()
                .filter(ri -> ri.rhythmFeatures().division() == d)
                .findAny()
                .orElseThrow();
        var r = rdb.getRhythmInstance(rInfo);        // throws UnavailableRhythmException
        return r;
    }

    private Rhythm findAdditionalRhythm(TimeSignature ts, MidiMix midiMix) throws UnavailableRhythmException
    {
        int currentVoiceCount = midiMix.getUserChannels().size();
        for (var r : midiMix.getUniqueRhythms())
        {
            currentVoiceCount += r.getRhythmVoices().size();
        }

        int maxNewVoices = MidiMix.NB_AVAILABLE_CHANNELS - currentVoiceCount;
        for (var rInfo : rdb.getRhythms(ts))
        {
            var r = rdb.getRhythmInstance(rInfo);
            if (midiMix.getUniqueRhythms().contains(r))
            {
                continue;
            }
            if (r.getRhythmVoices().size() <= maxNewVoices)
            {
                return r;
            }
        }

        throw new IllegalStateException("No suitable rhythm found for ts=" + ts + " maxNewVoices=" + maxNewVoices);
    }

}
