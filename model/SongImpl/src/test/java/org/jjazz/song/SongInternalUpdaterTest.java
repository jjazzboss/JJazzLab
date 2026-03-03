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
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
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

public class SongInternalUpdaterTest
{

    private static final String UT_EDIT_NAME = "UTedit";
    Song song;
    ChordLeadSheet cls1, u_cls1;
    CLI_ChordSymbol newChord;
    CLI_Section newSection1, newSection2, newSection3;
    CLI_Section section1, section2, section3;
    SongStructure sgs;
    SongStructure u_sgs;
    static DefaultRhythmDatabase rdb;
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
    public static void setUpClass() throws Exception
    {
        rdb = (DefaultRhythmDatabase) RhythmDatabase.getDefault();
        rdb.addRhythmsFromRhythmProviders(false, false, false);
        System.out.println(rdb.toStatsString());
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }

    @BeforeEach
    public void setUp()
    {
        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        section1 = (CLI_SectionImpl) cls1.getSection(0);
        undoManager = new JJazzUndoManager();
        try
        {
            cls1.setSizeInBars(8);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Dm7"), new Position(0)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("F#7"), new Position(1)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Bbmaj7#5"), new Position(1, 3)));
            section2 = new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2);
            cls1.addSection(section2);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("D7b9b5"), new Position(2)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("FM7#11"), new Position(4, 1)));
            section3 = new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5);
            cls1.addSection(section3);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Db"), new Position(7, 3f)));

            cls1.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(cls1, undoManager);

            // Copy for undo/redo test
            u_cls1 = cls1.getDeepCopy();

            // Extra items to play with
            newChord = new CLI_ChordSymbolImpl(ExtChordSymbol.get("A"), new Position(2, 1));
            newSection1 = new CLI_SectionImpl("NewSECTION1", TimeSignature.FOUR_FOUR, 4);
            newSection2 = new CLI_SectionImpl("NewSECTION2", TimeSignature.THREE_FOUR, 6);
            newSection3 = new CLI_SectionImpl("NewSECTION3", TimeSignature.FIVE_FOUR, 7);


            // Song structure
            song = SongFactory.getDefault().createSong("testSong", cls1);
            song.addUndoableEditListener(undoManager);
            JJazzUndoManagerFinder.getDefault().put(song, undoManager);

            sgs = song.getSongStructure();
            Rhythm r34 = sgs.getSongParts().get(1).getRhythm();
            spt0 = sgs.createSongPart(r34, null, 8, section2, true);
            sgs.addSongParts(List.of(spt0));


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
        boolean b1 = cls1.equals(u_cls1);
        boolean b2 = sgs.equals(u_sgs);
        if (!b1)
        {
            System.out.println("\nUNDO MISMATCH u_cls1=" + u_cls1.toDebugString());
            System.out.println("cls1 after Undo ALL=" + cls1.toDebugString());
        }
        if (!b2)
        {
            System.out.println("\nUNDO MISMATCH  u_sgs=" + u_sgs);
            System.out.println("sgs after Undo ALL =" + sgs);
        }
        assertTrue(b1);
        assertTrue(b2);
    }

    @Test
    public void testInsertAtBar0() throws UnsupportedEditException
    {
        System.out.println("\n============ testInsertAtBar0");
        cls1.deleteBars(0, 1);      // So that section2, which is used by 2 song parts, becomes the init section
        assertEquals(section2, cls1.getSection(0));
        assertEquals(9, sgs.getSizeInBars());
        var saveSptSection2 = sgs.getSongPart(0);
        String saveSection2Name = section2.getData().getName();
        var saveSection2ChordSymbols = cls1.getItems(section2, CLI_ChordSymbol.class);

        cls1.insertBars(0, 1);
        System.out.println(" sgs after=" + sgs);

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
        System.out.println("\n============ Test testAddAndRemove add chord symbol");
        assertEquals(11, sgs.getSizeInBars());
        cls1.addItem(newChord);
        System.out.println(" sgs after=" + sgs);
        assertEquals(11, sgs.getSizeInBars());
        assertEquals(3, sgs.getSongParts().get(1).getNbBars());
        System.out.println("\n== Test testAddAndRemove add section");
        try
        {
            cls1.addSection(newSection1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
        assertNotEquals(null, cls1.getSection(newSection1.getData().getName()));
        assertSame(newSection1, sgs.getSongParts().get(2).getParentSection());
        assertEquals(1, sgs.getSongParts().get(2).getNbBars());
        assertEquals(2, sgs.getSongParts().get(1).getNbBars());
        assertEquals(2, sgs.getSongParts().get(4).getNbBars());
        System.out.println("\n== Test testAddAndRemove removeSection");
        assertEquals(10, sgs.getSizeInBars());
        cls1.removeSection(section2);
        System.out.println(" sgs after=" + sgs);
        assertEquals(8, sgs.getSizeInBars());
        assertEquals(3, sgs.getSongParts().size());
        assertNotSame(section2, sgs.getSongParts().get(1).getParentSection());
        assertEquals(4, sgs.getSongParts().get(1).getStartBarIndex());
        assertEquals(5, sgs.getSongParts().get(2).getStartBarIndex());
    }

    @Test
    public void testAdd2()
    {
        System.out.println("\n============ testAdd2");
        assertTrue(sgs.getSizeInBars() == 11);
        sgs.removeSongParts(List.of(sgs.getSongParts().get(1)));
        System.out.println(" sgs after(1)=" + sgs);
        try
        {
            cls1.addSection(newSection1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after(2)=" + sgs);
        assertEquals(8, sgs.getSizeInBars());
        assertSame(newSection1, sgs.getSongParts().get(3).getParentSection());
        assertEquals(7, sgs.getSongParts().get(3).getStartBarIndex());
    }

    @Test
    public void testAdd3()
    {
        System.out.println("\n============ testAdd3 after absent section");
        assertTrue(sgs.getSizeInBars() == 11);
        sgs.removeSongParts(List.of(sgs.getSongParts().get(2)));
        System.out.println(" sgs after(1)=" + sgs);
        try
        {
            cls1.addSection(newSection2);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after(2)=" + sgs);
        assertEquals(10, sgs.getSizeInBars());
        assertSame(newSection2, sgs.getSongParts().get(3).getParentSection());
        assertEquals(8, sgs.getSongParts().get(3).getStartBarIndex());
    }

    @Test
    public void testAddAdaptedRhythm()
    {
        System.out.println("\n============ testAddAdaptedRhythm add section with new time signature => adapted rhythm");
        assertTrue(sgs.getSizeInBars() == 11);
        try
        {
            cls1.addSection(newSection3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // System.out.println("rdb=" + RhythmDatabase.getDefault().toString());
        System.out.println(" sgs after(1)=" + Utilities.toMultilineString(sgs.getSongParts()));
        assertTrue(sgs.getSizeInBars() == 11);
        assertTrue(sgs.getSongParts().get(3).getParentSection() == newSection3);
        Rhythm r = sgs.getSongParts().get(3).getRhythm();
        assertTrue(r instanceof AdaptedRhythm);
        assertTrue(((AdaptedRhythm) r).getSourceRhythm() == sgs.getSongParts().get(2).getRhythm());
    }

    @Test
    public void testChangeTimeSignature()
    {
        System.out.println("\n============ testChangeTimeSignature");

        try
        {
            cls1.setSectionTimeSignature(section2, TimeSignature.FOUR_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        System.out.println(" sgs after=" + sgs);
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
        System.out.println("\n============ testRhythmDivisionChange");


//            section3 = new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5);
//            cls1.addSection(section3);
//            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Eb7b9#5"), new Position(5, 0.75f)));
//            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Db"), new Position(7, 3f)));
        var spt = sgs.getSongPart(5);
        assertSame(section3, spt.getParentSection());       // Section 4/4 bar 5
        var rBinary = spt.getRhythm();
        assertTrue(rBinary.getFeatures().division().isBinary());
        Rhythm rTernary = getRhythm(TimeSignature.FOUR_FOUR, Division.EIGHTH_SHUFFLE);

        CLI_ChordSymbol chord1 = CLI_Factory.getDefault().createChordSymbol("A", new Position(5, 1.75f)); // throws ParseException
        CLI_ChordSymbol chord2 = CLI_Factory.getDefault().createChordSymbol("A", new Position(5, 3.5f)); // throws ParseException;

        cls1.addItem(chord1);
        cls1.addItem(chord2);

        // Change rhythm, should impact off-beat chords position
        sgs.setSongPartsRhythm(List.of(spt), rTernary, null);
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
        sgs.setSongPartsRhythm(List.of(spt), rTernary, null);    // Switch back to ternary
        assertEquals(1.75f, chord1.getPosition().getBeat(), 0.001f);        // Could not be moved
        assertEquals(1.66666f, chord2.getPosition().getBeat(), 0.001f);

        undoManager.endCEdit(UT_EDIT_NAME);
        undoManager.undo();
        undoManager.startCEdit(UT_EDIT_NAME);

        System.out.println(" sgs after=" + sgs);
    }


    @Test
    public void testChangeTimeSignaturePlusDivisionChange() throws UnavailableRhythmException, UnsupportedEditException
    {
        System.out.println("\n============ testChangeTimeSignaturePlusDivisionChange");

        var rWaltzSwing = getRhythm(TimeSignature.THREE_FOUR, Division.EIGHTH_SHUFFLE);

        // Use waltz swing for the existing 3/4 section, so that changing section3 to 3/4 will reuse the same rhythm
        var spt = sgs.getSongParts().get(1);
        sgs.setSongPartsRhythm(List.of(spt), rWaltzSwing, null);


        var chord1 = cls1.getBarFirstItem(5, CLI_ChordSymbol.class, cli -> true);   // bar 5 beat 0.75
        // Now we can change section3, it should switch also to rWaltzSwing, then adjust position of chord1
        try
        {
            cls1.setSectionTimeSignature(section3, TimeSignature.THREE_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        assertSame(rWaltzSwing, sgs.getSongParts().get(2).getRhythm());
        assertEquals(0.666f, chord1.getPosition().getBeat(), 0.001f);

        System.out.println(" sgs after=" + sgs);
    }

    @Test
    public void testMoveSection()
    {
        System.out.println("\n============ testMoveSection");
        cls1.moveSection(section2, 1);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().get(1).getStartBarIndex() == 1);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);
        assertTrue(sgs.getSongParts().get(3).getNbBars() == 4);
    }

    @Test
    public void testMoveSectionBig()
    {
        System.out.println("\n============ testMoveSectionBig");
        var lastChord = cls1.getItems(CLI_ChordSymbol.class).getLast();
        assertEquals(3, lastChord.getPosition().getBeat());

        cls1.moveSection(section2, 6);
        System.out.println(" sgs after=" + sgs);
        assertEquals(6, section2.getPosition().getBar());
        assertEquals(2, lastChord.getPosition().getBeat(), "chord symbol was beat 3 in 4/4, now we're in 3/4");

        assertTrue(sgs.getSongParts().size() == 3);
        assertEquals(6, sgs.getSongParts().get(2).getStartBarIndex());
        assertEquals(2, sgs.getSongParts().get(2).getNbBars());

    }

    @Test
    public void testResize()
    {
        System.out.println("\n============ TestResize");
        cls1.setSizeInBars(10);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 13);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 5);
        assertTrue(sgs.getSongParts().get(3).getStartBarIndex() == 10);
    }

    @Test
    public void testRemoveSection()
    {
        System.out.println("\n============ Test testRemoveOneSection");
        cls1.removeSection(section2);
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 2);
        assertTrue(sgs.getSongParts().get(0).getNbBars() == 5);
        assertTrue(sgs.getSongParts().get(1).getParentSection() == section3);
        assertTrue(sgs.getSizeInBars() == 8);
    }

    @Test
    public void testRemoveBars()
    {
        System.out.println("\n============ testRemoveBars");
        cls1.deleteBars(4, 5);
        System.out.println(" cls1 after=" + cls1.toDebugString());
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(1).getNbBars() == 4);
        assertTrue(sgs.getSongParts().get(2).getStartBarIndex() == 6);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 4);
    }

    @Test
    public void testRemoveInitialBarWithSectionOnBar1()
    {
        System.out.println("\n============ testRemoveInitialBarWithSectionOnBar1");
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
        System.out.println(" cls1 after=" + cls1.toDebugString());
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 4 && sgs.getSizeInBars() == 10);
        assertTrue(sgs.getSongParts().get(0).getParentSection() == newSection);
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
        assertTrue(sgs.getSongParts().size() == 5);
        cls1.deleteBars(6, 7);
        System.out.println(" cls1 after=" + cls1.toDebugString());
        System.out.println(" sgs after=" + sgs);
        assertTrue(sgs.getSongParts().size() == 4);
        assertTrue(sgs.getSongParts().get(2).getNbBars() == 1);
    }

    @Test
    public void testUserPhraseAddRemoveUpdatesMidiMixUserChannel() throws UnsupportedEditException
    {
        System.out.println("\n============ testUserPhraseAddRemoveUpdatesMidiMixUserChannel");
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
        System.out.println("\n============ testUserPhraseRenameUpdatesMidiMixUserChannel");
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
        System.out.println("\n============ testUserPhrasePreCheckVetoesIfNoMidiChannelAvailable");
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
        System.out.println("\n============ testUserPhrasePreCheckVetoesIfSameNameUserChannelExists");
        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);

        midiMix.addUserChannel("phrase1", false);
        assertThrows(UnsupportedEditException.class, () -> song.setUserPhrase("phrase1", new Phrase(0)));
    }

    @Test
    public void testSptAddedAndRemovedUpdatesMidiMixRhythms() throws UnavailableRhythmException, UnsupportedEditException
    {
        System.out.println("\n============ testSptAddedAndRemovedUpdatesMidiMixRhythms");
        MidiMix midiMix = MidiMixManager.getDefault().findMix(song);
        var initialRhythms = new HashSet<>(midiMix.getUniqueRhythms());

        Rhythm newRhythm = findAdditionalRhythm(TimeSignature.FOUR_FOUR, midiMix);
        assertFalse(midiMix.getUniqueRhythms().contains(newRhythm));

        SongPart sptNew = sgs.createSongPart(newRhythm, null, sgs.getSizeInBars(), section1, true);
        sgs.addSongParts(List.of(sptNew));
        assertTrue(midiMix.getUniqueRhythms().contains(newRhythm));

        sgs.removeSongParts(List.of(sptNew));
        assertEquals(initialRhythms, midiMix.getUniqueRhythms());
    }

    @Test
    public void testGetDerivedOperationsNoForwardDuringUndoRedo()
    {
        System.out.println("\n============ testGetDerivedOperationsNoForwardDuringUndoRedo");
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
        System.out.println("\n============ testReplaceSectionSameBarRenamesOnlyDefaultNamedSongParts");
        var sptsSection2 = sgs.getSongParts(spt -> spt.getParentSection() == section2);
        assertFalse(sptsSection2.isEmpty());

        SongPart customizedSpt = sptsSection2.get(0);
        sgs.setSongPartsName(List.of(customizedSpt), "custom");

        CLI_Section replaced = new CLI_SectionImpl("ReplacedSection2", TimeSignature.THREE_FOUR, section2.getPosition().getBar());
        cls1.addSection(replaced);

        assertSame(replaced, cls1.getSection(section2.getPosition().getBar()));
        assertTrue(sgs.getSongParts(spt -> spt.getParentSection() == section2).isEmpty());

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
