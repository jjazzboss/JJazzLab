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
package org.jjazz.chordleadsheet;

import java.text.ParseException;
import java.util.Locale;
import java.util.TreeSet;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.NCExtChordSymbol;
import org.jjazz.chordleadsheet.item.CLI_ChordSymbolImpl;
import org.jjazz.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.openide.util.Exceptions;

public class ChordLeadSheetImplTest
{

    private static final String UNDO_EDIT_NAME = "TEST";
    JJazzUndoManager undoManager;
    ChordLeadSheetImpl cls1;
    ChordLeadSheet cls2;
    CLI_SectionImpl cliSection34_b3;
    CLI_SectionImpl cliSection44_b4;
    CLI_SectionImpl cliSection54_b5;
    CLI_ChordSymbolImpl cliChordSymbolF_b3_3;
    CLI_ChordSymbolImpl cliChordSymbolG_b6_0;
    CLI_ChordSymbolImpl cliChordSymbolA_b12_2;


    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    public ChordLeadSheetImplTest()
    {
    }

    @BeforeAll
    public static void setUpClass() throws Exception
    {
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }

    @BeforeEach
    public void setUp()
    {
        System.out.println("setUp()");
        undoManager = new JJazzUndoManager();

        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);

        try
        {
            // Test leadsheet init
            cls1.setSizeInBars(8);
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("Dm7"), new Position(0)));
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("F#7"), new Position(1)));
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("Bbmaj7#5"), new Position(1, 3)));
            cls1.addSection(new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2));
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("D7b9b5"), new Position(2)));
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("FM7#11"), new Position(4, 1)));
            cls1.addSection(new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5));
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("Db"), new Position(7, 3f)));

            // System.out.println("cls1="+cls1.toDebugString());

            cls1.addUndoableEditListener(undoManager);

            // cls2 = deep copy to make the comparison after a undo/redo/undo cycle
            cls2 = cls1.getDeepCopy();

            // Items to play with
            cliSection34_b3 = new CLI_SectionImpl("NewSection34", TimeSignature.THREE_FOUR, 3);
            cliSection44_b4 = new CLI_SectionImpl("NewSection44", TimeSignature.FOUR_FOUR, 4);
            cliSection54_b5 = new CLI_SectionImpl("NewSection54", TimeSignature.FIVE_FOUR, 5);
            cliChordSymbolF_b3_3 = new CLI_ChordSymbolImpl(getChord("F-"), new Position(3, 3));
            cliChordSymbolG_b6_0 = new CLI_ChordSymbolImpl(getChord("G-"), new Position(6));
            cliChordSymbolA_b12_2 = new CLI_ChordSymbolImpl(getChord("A-"), new Position(12, 2));

            // Start one edit
            undoManager.startCEdit(UNDO_EDIT_NAME);

        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @AfterEach
    public void tearDown()
    {
        if (undoManager.getCurrentCEditName() == null)
        {
            return;
        }
        // System.out.println("tearDown() --");
        undoManager.endCEdit(UNDO_EDIT_NAME);
        var cls1BeforeStr = cls1.toDebugString();
        undoAll();
        redoAll();
        undoAll();
        boolean b = cls1.equals(cls2);
        if (!b)
        {
            System.out.println("cls1 & cls2 MISMATCH after undo/redo/undoss");
            System.out.println(cls1BeforeStr);
            System.out.println("--");
            System.out.println(cls1.toDebugString());
            System.out.println("");
            assertTrue(cls1.equals(cls2));
        }

    }

    private ExtChordSymbol getChord(String name)
    {
        try
        {
            return ExtChordSymbol.get(name);
        } catch (ParseException ex)
        {
            throw new RuntimeException("Failed to parse chord: " + name, ex);
        }
    }

    @Test
    public void testChordLeadSheetItemCreate()
    {
        System.out.println("=== testChordLeadSheetItemCreate() ");
        TreeSet<ChordLeadSheetItem> items = new TreeSet<>();
        CLI_Section initSection = new CLI_SectionImpl("NewSection34", TimeSignature.THREE_FOUR, 0);
        var chord0 = new CLI_ChordSymbolImpl(getChord("Dm7"), new Position(0));
        var chord4 = new CLI_ChordSymbolImpl(getChord("C"), new Position(4));
        items.add(initSection);
        items.add(chord0);
        items.add(cliChordSymbolF_b3_3);
        items.add(cliSection44_b4);
        items.add(chord4);
        items.add(cliChordSymbolG_b6_0);
        System.out.println("all items=" + items);

        var res = items.subSet(initSection, true, cliChordSymbolG_b6_0, false);
        System.out.println("subItems1(true-false=" + res);
        res = items.subSet(initSection, false, cliChordSymbolG_b6_0, true);
        System.out.println("subItems2(false-true)=" + res);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0), true), true, cliChordSymbolG_b6_0, true);
        System.out.println("subItems3(true0, true)=" + res);
        assertSame(res.first(), initSection);
        assertSame(res.last(), cliChordSymbolG_b6_0);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(6), true), true);
        System.out.println("subItems4(false0, true6)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), cliChordSymbolG_b6_0);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(6), false), false);
        System.out.println("subItems5(false0, false6)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), chord4);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(4), true), true);
        System.out.println("subItems6(false0, true4)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), chord4);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(4), false), false);
        System.out.println("subItems7(false0, false4)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), cliChordSymbolF_b3_3);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0), true), true,
                ChordLeadSheetItem.createItemTo(new Position(4), false), false);
        System.out.println("subItems8(true0, false4)=" + res);
        assertSame(res.first(), initSection);
        assertSame(res.last(), cliChordSymbolF_b3_3);

    }

    @Test
    public void testGetItems()
    {
        System.out.println("=== getItems() ");
        var res = cls1.getItems(new Position(0),
                true,
                new Position(2),
                false,
                ChordLeadSheetItem.class,
                cli -> true);
        assertEquals(5, res.size());
        assertSame(cls1.getSection(0), res.get(0));
        assertEquals(new Position(1, 3), res.get(4).getPosition());
    }

    @Test
    public void testGetFirstLastItems()
    {
        System.out.println("=== testGetFirstLastItems() ");
        var chordSymbols = cls1.getItems(CLI_ChordSymbol.class);

        
        // getFirstItemAfter
        var cli = cls1.getFirstItemAfter(chordSymbols.get(3), CLI_ChordSymbol.class, c -> true);
        assertSame(cli, chordSymbols.get(4));

        cli = cls1.getFirstItemAfter(new Position(2), true, CLI_ChordSymbol.class, c -> true);
        assertSame(cli, chordSymbols.get(3));

        cli = cls1.getFirstItemAfter(new Position(2), false, CLI_ChordSymbol.class, c -> true);
        assertSame(cli, chordSymbols.get(4));

        // getLastItemBefore
        cli = cls1.getLastItemBefore(chordSymbols.get(6), CLI_ChordSymbol.class, c -> true);
        assertSame(cli, chordSymbols.get(5));

        cli = cls1.getLastItemBefore(new Position(2), true, CLI_ChordSymbol.class, c -> true);
        assertSame(cli, chordSymbols.get(3));

        cli = cls1.getLastItemBefore(new Position(2), false, CLI_ChordSymbol.class, c -> true);
        assertSame(cli, chordSymbols.get(2));

        cli = cls1.getLastItemBefore(new Position(0), false, CLI_ChordSymbol.class, c -> true);
        assertNull(cli);

    }

    @Test
    public void testAddItem()
    {
        System.out.println("=== addItem ChordSymbol");
        cls1.addItem(cliChordSymbolG_b6_0);
        assertSame(cliChordSymbolG_b6_0, cls1.getItems(6, 6, ChordLeadSheetItem.class).get(0));
    }

    @Test
    public void testAddDifferentItemSamePosition()
    {
        var cli = cls1.getItems(1, 1, CLI_ChordSymbol.class).get(0);
        var cliClone = cli.getCopy(getChord("D7b9"), null);
        assertTrue(cls1.addItem(cliClone));

        var newEcs = cli.getData().getTransposedChordSymbol(1, null);
        var newCli = cli.getCopy(newEcs, null);
        assertTrue(cls1.addItem(newCli));
    }

    @Test
    public void testAddCloneItemSamePosition()
    {
        System.out.println("=== testAddCloneItemSamePosition()");
        var cli = cls1.getItems(1, 1, CLI_ChordSymbol.class).get(0);
        var cliClone = cli.getCopy(null, null);
        assertFalse(cls1.addItem(cliClone));

        var cliNC = cli.getCopy(new NCExtChordSymbol(), null);
        assertTrue(cls1.addItem(cliNC));

        var cliNC2 = cliNC.getCopy(cliNC.getData().getTransposedChordSymbol(1, Note.Accidental.FLAT), null);    // builds an equal NCExtChordSymbol
        assertFalse(cls1.addItem(cliNC2));
    }

    @Test
    public void testAddItemOutOfTimeSignature()
    {
        System.out.println("=== addItem ChordSymbol out of time signature");
        assertTrue(cls1.addItem(cliChordSymbolF_b3_3));
        // bar 3 is within Section2 which is 3/4 (starts at bar 2), so beat 3 must be adjusted <= 2
        assertEquals(2f, cliChordSymbolF_b3_3.getPosition().getBeat(), 0.0001f);
    }

    @Test
    public void testAddItemOutOfBounds()
    {
        System.out.println("=== addItem ChordSymbol out of bounds");
        assertThrows(IllegalArgumentException.class, () -> cls1.addItem(cliChordSymbolA_b12_2));
    }

    @Test
    public void testAddItemAsSection()
    {
        System.out.println("=== addItem as Section");
        assertThrows(IllegalArgumentException.class, () -> cls1.addItem(cliSection34_b3));
    }

    @Test
    public void testRemoveItem()
    {
        System.out.println("=== removeItem");
        ChordLeadSheetItem<?> item = cls1.getItems(0, 0, CLI_ChordSymbol.class).get(0);
        assertTrue(cls1.contains(item));
        assertTrue(cls1.removeItem(item));
        assertFalse(cls1.contains(item));
        assertFalse(cls1.removeItem(item)); // Should return false if not present
    }

    @Test
    public void testContains()
    {
        System.out.println("=== contains");
        ChordLeadSheetItem<?> item = cls1.getItems(0, 0, CLI_ChordSymbol.class).get(0);
        assertTrue(cls1.contains(item));

        // Create an item not in the leadsheet
        CLI_ChordSymbolImpl dummy = new CLI_ChordSymbolImpl(getChord("C"), new Position(10, 0));
        assertFalse(cls1.contains(dummy));
    }

    @Test
    public void testChangeItem()
    {
        System.out.println("=== changeItem ChordSymbol");
        var cli1 = cls1.getItems(1, 1, CLI_ChordSymbol.class).get(0);   // F#7
        var data1 = cliChordSymbolF_b3_3.getData();
        assertTrue(cls1.changeItem(cli1, data1));
        assertSame(data1, cli1.getData());

        // Can not change an item if an equal one (with new data) is already there
        var data2 = cliChordSymbolG_b6_0.getData();
        var cli2 = cli1.getCopy(data2, null);
        assertTrue(cls1.addItem(cli2));
        assertFalse(cls1.changeItem(cli2, data1));
        assertSame(data2, cli2.getData());
    }

    @Test
    public void testAddSection()
    {
        System.out.println("=== addSection");
        try
        {
            cls1.addSection(cliSection34_b3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        var res = cls1.getItems(cliSection34_b3, ChordLeadSheetItem.class);
        System.out.println("res=" + res);
        assertEquals(1, res.size());
    }

    @Test
    public void testAddSectionOver()
    {
        System.out.println("=== addSection over another");
        try
        {
            cls1.addSection(cliSection54_b5);
            assertTrue(cls1.getSection(5) == cliSection54_b5);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @Test
    public void testAddSectionAdjustItemPosition()
    {
        System.out.println("=== addSection adjust position of items");
        cliSection34_b3.setPosition(new Position(1));
        try
        {
            cls1.addSection(cliSection34_b3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertSame(cls1.getItems(1, 1, CLI_Section.class).get(0), cliSection34_b3);
        var res = cls1.getItems(cliSection34_b3, ChordLeadSheetItem.class);
        assertEquals(new Position(1, 2), res.get(1).getPosition());
    }

    @Test
    public void testAddSectionOutOfBounds() throws Exception
    {
        assertThrows(IllegalArgumentException.class, () -> cls1.addSection(new CLI_SectionImpl("TooFar", TimeSignature.FOUR_FOUR, cls1.getSizeInBars())));
    }

    @Test
    public void testRemoveSection()
    {
        System.out.println("=== RemoveSection Section2");
        CLI_Section cliSection = cls1.getSection("Section2");
        cls1.removeSection(cliSection);
        assertEquals(5, cls1.getBarRange(cls1.getSection(0)).size());
    }

    @Test
    public void testRemoveSectionAndAdjustItemPositions()
    {
        System.out.println("=== RemoveSection and adjust items positions");
        CLI_Section cliSection = cls1.getSection("Section3");
        cls1.removeSection(cliSection);
        assertEquals(new Position(7, 2), cls1.getItems(cls1.getSection("Section2"), ChordLeadSheetItem.class).get(3).getPosition());
    }

    @Test
    public void testRemoveSectionBar0Forbidden()
    {
        CLI_Section section0 = cls1.getSection(0);
        assertThrows(IllegalArgumentException.class, () -> cls1.removeSection(section0));
    }

    @Test
    public void testMoveItem()
    {
        System.out.println("=== MoveItem chord symbol");
        var cli1 = cls1.getItems(1, 1, ChordLeadSheetItem.class).get(1);
        var cli2 = cli1.getCopy(null, new Position(1, 1));
        assertTrue(cls1.addItem(cli2));
        assertTrue(cls1.moveItem(cli1, new Position(1, 0.5f)));
        assertEquals(new Position(1, 0.5f), cli1.getPosition());

        // Can not move over equal item cli2
        assertFalse(cls1.moveItem(cli1, new Position(1, 1)));
    }

    @Test
    public void testMoveItemAndAdjustPosition()
    {
        System.out.println("=== MoveItem and auto-adjust item position due to time signature limit");
        ChordLeadSheetItem<?> cli = cls1.getItems(1, 1, ChordLeadSheetItem.class).get(1);
        cls1.moveItem(cli, new Position(2, 3));
        assertEquals(new Position(2, 2), cli.getPosition());
    }

    @Test
    public void testMoveItemRejectsSection()
    {
        CLI_Section section2 = cls1.getSection("Section2");
        assertThrows(IllegalArgumentException.class, () -> cls1.moveItem(section2, new Position(3)));
    }

    @Test
    public void testMoveSection0()
    {
        System.out.println("=== testMoveSection0 move init section");
        CLI_Section cliSection0 = cls1.getSection(0);
        assertThrows(IllegalArgumentException.class, () -> cls1.moveSection(cliSection0, 3));
    }

    @Test
    public void testMoveSection1()
    {
        System.out.println("=== testMoveSection1 move section on another");
        CLI_Section cliSection0 = cls1.getSection(5);
        assertThrows(IllegalArgumentException.class, () -> cls1.moveSection(cliSection0, 2));
    }

    @Test
    public void testMoveSection2()
    {
        System.out.println("=== testMoveSection2 moved section backwards, does not cross other sections");
        CLI_Section cliSection0 = cls1.getSection(2);
        cls1.moveSection(cliSection0, 1);
        assertEquals(new Position(1, 2), cls1.getItems(1, 1, CLI_ChordSymbol.class).get(1).getPosition());
        assertEquals(1, cls1.getSection(1).getPosition().getBar());
    }

    @Test
    public void testMoveSection2bis()
    {
        System.out.println("=== testMoveSection2bis moved section forward, does not cross other sections");
        assertEquals(2, cls1.getBarRange(cls1.getSection(0)).size());
        CLI_Section cliSection = cls1.getSection(2);
        cls1.moveSection(cliSection, 3);
        assertSame(cliSection, cls1.getSection(3));
        assertEquals(0, cls1.getSection(2).getPosition().getBar());
        assertEquals(3, cls1.getBarRange(cls1.getSection(0)).size());
    }

    @Test
    public void testMoveSection3()
    {
        System.out.println("=== testMoveSection3 moved section crosses other sections");
        CLI_Section cliSection0 = cls1.getSection(5);
        cls1.moveSection(cliSection0, 1);
        assertEquals(new Position(7, 2), cls1.getItems(7, 7, ChordLeadSheetItem.class, cli -> true).get(0).getPosition());
        assertSame(cliSection0, cls1.getSection(1));
    }


    @Test
    public void testMoveFirstSection()
    {
        System.out.println("=== moveSection bar 0");
        CLI_Section section0 = cls1.getSection(0);
        assertThrows(IllegalArgumentException.class, () -> cls1.moveSection(section0, 2));
    }

    @Test
    public void testInsertBars0()
    {
        System.out.println("=== insertBars start of leadsheet bar=0  nbBars=3");
        var oldSection0 = cls1.getSection(0);
        CLI_ChordSymbol chordSymbol1 = cls1.getItems(CLI_ChordSymbol.class).get(1);
        int chordSymbol1_oldBar = chordSymbol1.getPosition().getBar();

        cls1.insertBars(0, 3);

        assertEquals(11, cls1.getSizeInBars());
        assertEquals(chordSymbol1_oldBar + 3, chordSymbol1.getPosition().getBar());
        var newSection3 = cls1.getSection(3);
        assertSame(oldSection0, newSection3);

        var newSection0 = cls1.getSection(0);
        assertTrue(newSection0.getData().getName().contains(oldSection0.getData().getName()));
        assertEquals(newSection0.getData().getTimeSignature(), oldSection0.getData().getTimeSignature());

        assertEquals("Section2", cls1.getSection(5).getData().getName());

    }

    @Test
    public void testInsertBars2()
    {
        System.out.println("=== insertBars end of section bar=2  nbBars=2");
        cls1.insertBars(2, 2);
        assertEquals(10, cls1.getSizeInBars());
        assertEquals("Section2", cls1.getSection(5).getData().getName());
    }

    @Test
    public void testInsertBars6()
    {
        System.out.println("=== insertBars middle of section bar=6  nbBars=2");
        cls1.insertBars(6, 2);
        assertEquals(10, cls1.getSizeInBars());
    }

    @Test
    public void testInsertBars8()
    {
        System.out.println("=== insertBars end of leadsheet last bar=8  nbBars=5");
        cls1.insertBars(8, 5);
        assertEquals(13, cls1.getSizeInBars());
    }

    @Test
    public void testInsertBarsRedoBug()
    {
        System.out.println("=== insertBars resize bug when undo/redo");
        cls1.insertBars(8, 5);
        assertEquals(13, cls1.getSizeInBars());
        undoManager.endCEdit(UNDO_EDIT_NAME);
        undoManager.undo();
        assertEquals(8, cls1.getSizeInBars());
        undoManager.redo();
        assertEquals(13, cls1.getSizeInBars());
        undoManager.startCEdit(UNDO_EDIT_NAME);
    }

    @Test
    public void testInsertBarsNegative()
    {
        assertThrows(IllegalArgumentException.class, () -> cls1.insertBars(-1, 1));
    }

    @Test
    public void testInsertBarsBeyondSize()
    {
        assertThrows(IllegalArgumentException.class, () -> cls1.insertBars(cls1.getSizeInBars() + 1, 1));
    }

    @Test
    public void testDeleteBarsFromStartUntilEndOfSection()
    {
        System.out.println("=== deleteBars from start until end of section barFrom=0  barTo=1");
        var section2 = cls1.getSection(2);
        cls1.deleteBars(0, 1);
        assertEquals(6, cls1.getSizeInBars());
        assertEquals(2, cls1.getItems(CLI_Section.class).size());
        assertSame(section2, cls1.getSection(0), "section2 becomes the new init section");
    }

    @Test
    public void testDeleteBarsFromStartUntilMiddleOfSection()
    {
        System.out.println("=== deleteBars from start middle of section barFrom=0  barTo=3");
        var oldCliSection0 = cls1.getSection(0);
        cls1.deleteBars(0, 3);
        assertEquals(4, cls1.getSizeInBars());
        CLI_Section cliSection0 = cls1.getSection(0);
        assertSame(oldCliSection0, cliSection0, "section0 is not removed if no section right after the cut");
        assertEquals(1, cls1.getBarRange(cliSection0).size());
    }

    @Test
    public void testDeleteInitialBarWithSectionOnBar1()
    {
        System.out.println("=== deleteBars on initial bar 0 with a section on bar 1");
        CLI_Section newSection = new CLI_SectionImpl("SectionBar1", TimeSignature.FIVE_FOUR, 1);
        try
        {
            cls1.addSection(newSection);
            cls1.deleteBars(0, 0);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertTrue(cls1.getSizeInBars() == 7 && cls1.getSection(0).getData().equals(newSection.getData()));
    }

    @Test
    public void testDeleteBarsToEnd()
    {
        System.out.println("=== deleteBars until end barFrom=4  barTo=7");
        cls1.deleteBars(4, 7);
        assertEquals(cls1.getSizeInBars(), 4);
    }

    @Test
    public void testDeleteBarsMiddle()
    {
        System.out.println("=== deleteBars multi sections barFrom=1  barTo=5");
        cls1.deleteBars(1, 5);
        assertEquals(3, cls1.getSizeInBars());
        assertEquals("Section1", cls1.getSection(2).getData().getName());
    }

    /**
     * Deleting bars that include a section header removes the header but leaves surviving items of that section. Those items now fall under the previous
     * section's (smaller) time signature, so their beats must be adjusted.
     * <p>
     * cls1 layout: Section1(4/4)@bar0, Section2(3/4)@bar2, Section3(4/4)@bar5<br>
     * Db@7:beat3 lives in Section3. Deleting bars 3-5 removes Section3's header (bar 5 ∈ [3,5]); Db shifts to bar 4 and its beat 3.0 must be adjusted to fit
     * Section2's 3/4.
     */
    @Test
    public void testDeleteBarsAdjustBeatsWhenSectionHeaderDeleted()
    {
        System.out.println("=== testDeleteBarsAdjustBeatsWhenSectionHeaderDeleted()");
        var dbItem = cls1.getItems(7, 7, CLI_ChordSymbol.class).get(0); // Db@7:beat3.0
        assertEquals(3.0f, dbItem.getPosition().getBeat(), 0.001f);

        cls1.deleteBars(3, 5);

        assertEquals(5, cls1.getSizeInBars());
        // Db shifted from bar 7 to bar 4 (7 - 3 deleted bars)
        assertEquals(4, dbItem.getPosition().getBar());
        // Beat must have been adjusted to fit Section2's 3/4 (beat 3.0 is invalid in 3/4)
        assertTrue(dbItem.getPosition().getBeat() < 3.0f,
                "Beat should be adjusted to fit 3/4 time signature, was: " + dbItem.getPosition().getBeat());
    }

    /**
     * When the section header survives the deletion (starts at barIndexTo + 1), the header and its items shift together, so no beat adjustment should be
     * applied.
     * <p>
     * cls1 layout: Section3(4/4)@bar5<br>
     * Deleting bars 3-4 leaves Section3's header at bar 5 = barIndexTo + 1 (outside the deleted range). Db@7:beat3 shifts to bar 5 and stays under Section3's
     * 4/4 — beat must remain 3.0.
     */
    @Test
    public void testDeleteBarsNoAdjustWhenSectionHeaderNotDeleted()
    {
        System.out.println("=== testDeleteBarsNoAdjustWhenSectionHeaderNotDeleted()");
        var dbItem = cls1.getItems(7, 7, CLI_ChordSymbol.class).get(0); // Db@7:beat3.0

        cls1.deleteBars(3, 4);

        assertEquals(6, cls1.getSizeInBars());
        // Db shifted from bar 7 to bar 5 (7 - 2 deleted bars)
        assertEquals(5, dbItem.getPosition().getBar());
        // Section3 header also shifted to bar 3, still governs Db => beat must stay at 3.0
        assertEquals(3.0f, dbItem.getPosition().getBeat(), 0.001f,
                "Beat should NOT be adjusted when section header is not deleted");
    }

    /**
     * Undo must restore the original beat; redo must reapply the adjusted beat.
     */
    @Test
    public void testDeleteBarsAdjustBeatsUndoRedo()
    {
        System.out.println("=== testDeleteBarsAdjustBeatsUndoRedo()");
        var dbItem = cls1.getItems(7, 7, CLI_ChordSymbol.class).get(0); // Db@7:beat3.0

        cls1.deleteBars(3, 5);

        float adjustedBeat = dbItem.getPosition().getBeat();
        assertTrue(adjustedBeat < 3.0f, "Pre-undo: beat should be adjusted");

        undoManager.endCEdit(UNDO_EDIT_NAME);
        undoManager.undo();

        // After undo: Db restored to bar 7, original beat 3.0
        assertEquals(7, dbItem.getPosition().getBar(), "After undo: bar should be restored to 7");
        assertEquals(3.0f, dbItem.getPosition().getBeat(), 0.001f, "After undo: beat should be restored to 3.0");

        undoManager.redo();

        // After redo: Db back at bar 4 with the same adjusted beat
        assertEquals(4, dbItem.getPosition().getBar(), "After redo: bar should be 4");
        assertEquals(adjustedBeat, dbItem.getPosition().getBeat(), 0.001f, "After redo: beat should match post-deletion value");

        undoManager.startCEdit(UNDO_EDIT_NAME);
    }

    @Test
    public void testDeleteBarsInconsistent()
    {
        System.out.println("=== deleteBars inconsistent range");
        assertThrows(IllegalArgumentException.class, () -> cls1.deleteBars(5, 2));
    }

    @Test
    public void testDeleteAllBars()
    {
        System.out.println("=== deleteBars all bars");
        assertThrows(IllegalArgumentException.class, () -> cls1.deleteBars(0, cls1.getSizeInBars() - 1));
    }

    @Test
    public void testSetTimeSignature()
    {
        System.out.println("=== setTimeSignature section 0 => 3/4");
        CLI_Section cliSection0 = cls1.getSection(0);
        try
        {
            cls1.setSectionTimeSignature(cliSection0, TimeSignature.THREE_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        var cli = cls1.getItems(1, 1, ChordLeadSheetItem.class, c -> true).get(1);
        assertEquals(new Position(1, 2f), cli.getPosition());
    }

    /**
     * 2 equal clis (but different positions) are adjusted with time signature change, and end up at same position => one is removed.
     */
    @Test
    public void testSetTimeSignatureAndAdjustWithCollision()
    {
        System.out.println("=== testSetTimeSignatureAndAdjustWithCollision section 0 => 3/4");

        var cli1 = cls1.getItems(1, 1, ChordLeadSheetItem.class, c -> true).get(1); // beat 3
        var cli2 = cli1.getCopy(null, cli1.getPosition().setBeat(3.5f));
        cls1.addItem(cli2);

        CLI_Section cliSection0 = cls1.getSection(0);
        try
        {
            cls1.setSectionTimeSignature(cliSection0, TimeSignature.THREE_FOUR); // moves cli2 to beat 1.75 because cli1 identical present on beat 2
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        var clis = cls1.getItems(1, 1, ChordLeadSheetItem.class, c -> true);
        assertEquals(3, clis.size());
        assertEquals(new Position(1, 2f), cli1.getPosition());
        assertEquals(new Position(1, 1.75f), cli2.getPosition());
    }

    @Test
    public void testSetSectionNameOK()
    {
        System.out.println("=== setSectionName Yeaaaah section 0");
        CLI_Section cliSection0 = cls1.getSection(0);
        cls1.setSectionName(cliSection0, "Yeaaah");
        assertEquals("Yeaaah", cliSection0.getData().getName());
    }

    @Test
    public void testSetSectionNameAlreadyExist()
    {
        System.out.println("=== setSectionName name already exist");
        CLI_Section cliSection0 = cls1.getSection(0);
        assertThrows(IllegalArgumentException.class, () -> cls1.setSectionName(cliSection0, "Section3"));
    }

    @Test
    public void testSetSectionNameForeignSection()
    {
        CLI_Section foreign = new CLI_SectionImpl("Foreign", TimeSignature.FOUR_FOUR, 0);
        assertThrows(IllegalArgumentException.class, () -> cls1.setSectionName(foreign, "X"));
    }

    @Test
    public void testSetSectionTimeSignatureForeignSection() throws Exception
    {
        CLI_Section foreign = new CLI_SectionImpl("Foreign", TimeSignature.FOUR_FOUR, 0);
        assertThrows(IllegalArgumentException.class, () -> cls1.setSectionTimeSignature(foreign, TimeSignature.THREE_FOUR));
    }


    @Test
    public void testInitSectionContainer()
    {
        System.out.println("=== testInitSectionContainer()");
        assertSame(cls1, cls1.getSection(0).getContainer());

    }

    @Test
    public void testSetSize()
    {
        System.out.println("=== setSize()");
        cls1.setSizeInBars(3);
        assertEquals(3, cls1.getSizeInBars());
        assertEquals(7, cls1.getItems(ChordLeadSheetItem.class).size());
    }

    @Test
    public void testSetSizeTooSmall()
    {
        assertThrows(IllegalArgumentException.class, () -> cls1.setSizeInBars(0));
    }

    @Test
    public void testSetSizeTooLarge()
    {
        assertThrows(IllegalArgumentException.class, () -> cls1.setSizeInBars(ChordLeadSheet.MAX_SIZE + 1));
    }

    @Test
    public void testGetSectionOutOfBounds()
    {
        System.out.println("=== getSection out of bounds");
        int size = cls1.getSizeInBars();
        CLI_Section lastSection = cls1.getSection(size - 1);
        // Should return the last section if index >= size
        assertSame(lastSection, cls1.getSection(size + 10));
    }

    @Test
    public void testGetSectionBar()
    {
        System.out.println("=== testGetSectionBar");
        assertEquals(0, cls1.getSection(0).getPosition().getBar());
        assertEquals(0, cls1.getSection(1).getPosition().getBar());
        assertEquals(2, cls1.getSection(2).getPosition().getBar());
    }

    @Test
    public void testGetSectionNegative()
    {
        assertThrows(IllegalArgumentException.class, () -> cls1.getSection(-1));
    }

    @Test
    public void testGetSectionByNameNotFound()
    {
        assertNull(cls1.getSection("DoesNotExist"));
    }

    @Test
    public void testGetBarRangeInvalidSection()
    {
        CLI_Section dummySection = new CLI_SectionImpl("Dummy", TimeSignature.FOUR_FOUR, 0);
        assertThrows(IllegalArgumentException.class, () -> cls1.getBarRange(dummySection));
    }

    @Test
    public void testDeepCopyIntegrity()
    {
        System.out.println("=== deepCopy integrity");
        ChordLeadSheet copy = cls1.getDeepCopy();

        // Modify original
        CLI_ChordSymbolImpl newChord = new CLI_ChordSymbolImpl(getChord("C7"), new Position(0, 2));
        cls1.addItem(newChord);

        assertTrue(cls1.contains(newChord));
        assertFalse(copy.contains(newChord));
    }

    @Test
    public void testChangeListenerNotification()
    {
        System.out.println("=== changeListener notification");
        final boolean[] notified =
        {
            false
        };
        ClsChangeListener listener = event -> notified[0] = true;

        cls1.addClsChangeListener(listener);
        try
        {
            cls1.addItem(new CLI_ChordSymbolImpl(getChord("C"), new Position(0, 2)));
            assertTrue(notified[0], "Listener should have been notified");
        } finally
        {
            cls1.removeClsChangeListener(listener);
        }
    }


    // =========================================================================
    // get*() coverage tests
    // =========================================================================
    /**
     * cls1 has 8 bars => getBarRange() must return [0, 7].
     */
    @Test
    public void testGetBarRange()
    {
        System.out.println("=== testGetBarRange()");
        var range = cls1.getBarRange();
        assertEquals(0, range.from);
        assertEquals(7, range.to);
        assertEquals(8, range.size());
    }

    /**
     * cls1 chord symbols in order: Dm7@0:0, F#7@1:0, Bbmaj7#5@1:3, D7b9b5@2:0, FM7#11@4:1, Eb7b9#5@5:0.75, Db@7:3.
     * <p>
     * Inclusive from bar 4, beat 0 => FM7#11, Eb7b9#5, Db (3 items). Exclusive from bar 4, beat 1 => Eb7b9#5, Db (2 items).
     */
    @Test
    public void testGetItemsAfter()
    {
        System.out.println("=== testGetItemsAfter()");
        var res = cls1.getItemsAfter(new Position(4), true, CLI_ChordSymbol.class, c -> true);
        assertEquals(3, res.size());
        assertEquals(new Position(4, 1), res.get(0).getPosition());

        res = cls1.getItemsAfter(new Position(4, 1), false, CLI_ChordSymbol.class, c -> true);
        assertEquals(2, res.size());
        assertEquals(new Position(5, 0.75f), res.get(0).getPosition());
    }

    /**
     * Inclusive up to bar 4, beat 1 => Dm7, F#7, Bbmaj7#5, D7b9b5, FM7#11 (5 items). Exclusive up to bar 4, beat 1 => Dm7, F#7, Bbmaj7#5, D7b9b5 (4 items).
     */
    @Test
    public void testGetItemsBefore()
    {
        System.out.println("=== testGetItemsBefore()");
        var res = cls1.getItemsBefore(new Position(4, 1), true, CLI_ChordSymbol.class, c -> true);
        assertEquals(5, res.size());
        assertEquals(new Position(4, 1), res.get(4).getPosition());

        res = cls1.getItemsBefore(new Position(4, 1), false, CLI_ChordSymbol.class, c -> true);
        assertEquals(4, res.size());
        assertEquals(new Position(2, 0), res.get(3).getPosition());
    }

    /**
     * Bar 1 contains F#7@1:0 (first) and Bbmaj7#5@1:3 (last). Bar 3 is empty.
     */
    @Test
    public void testGetBarFirstItem()
    {
        System.out.println("=== testGetBarFirstItem()");
        var item = cls1.getBarFirstItem(1, CLI_ChordSymbol.class, c -> true);
        assertNotNull(item);
        assertEquals(new Position(1, 0), item.getPosition());

        assertNull(cls1.getBarFirstItem(3, CLI_ChordSymbol.class, c -> true));
    }

    /**
     * Bar 1 contains F#7@1:0 (first) and Bbmaj7#5@1:3 (last). Bar 3 is empty.
     */
    @Test
    public void testGetBarLastItem()
    {
        System.out.println("=== testGetBarLastItem()");
        var item = cls1.getBarLastItem(1, CLI_ChordSymbol.class, c -> true);
        assertNotNull(item);
        assertEquals(new Position(1, 3), item.getPosition());

        assertNull(cls1.getBarLastItem(3, CLI_ChordSymbol.class, c -> true));
    }

    /**
     * getNextItem on Section2 returns Section3. getNextItem on the last section returns null.
     */
    @Test
    public void testGetNextItem()
    {
        System.out.println("=== testGetNextItem()");
        var section2 = cls1.getSection("Section2");
        var section3 = cls1.getSection("Section3");

        assertSame(section3, cls1.getNextItem(section2));
        assertNull(cls1.getNextItem(section3));
    }

    /**
     * getPreviousItem on Section2 returns Section1. getPreviousItem on the first section returns null.
     */
    @Test
    public void testGetPreviousItem()
    {
        System.out.println("=== testGetPreviousItem()");
        var section1 = cls1.getSection("Section1");
        var section2 = cls1.getSection("Section2");

        assertSame(section1, cls1.getPreviousItem(section2));
        assertNull(cls1.getPreviousItem(section1));
    }

    /**
     * Section1 spans bars 0-1 with Dm7@0:0, F#7@1:0, Bbmaj7#5@1:3. Filtering on beat == 0 must return only Dm7 and F#7.
     */
    @Test
    public void testGetItemsBySectionWithTester()
    {
        System.out.println("=== testGetItemsBySectionWithTester()");
        var section1 = cls1.getSection("Section1");
        var res = cls1.getItems(section1, CLI_ChordSymbol.class, c -> c.getPosition().getBeat() == 0);
        assertEquals(2, res.size());
        assertEquals(new Position(0, 0), res.get(0).getPosition());
        assertEquals(new Position(1, 0), res.get(1).getPosition());
    }

    /**
     * getItems(barFrom, barTo, Class) without tester: bars 2 to 4 contain Section2@2, D7b9b5@2:0, FM7#11@4:1 (3 items).
     */
    @Test
    public void testGetItemsByBarRangeNoTester()
    {
        System.out.println("=== testGetItemsByBarRangeNoTester()");
        var res = cls1.getItems(2, 4, ChordLeadSheetItem.class);
        assertEquals(3, res.size());
        assertSame(cls1.getSection("Section2"), res.get(0));
        assertEquals(new Position(4, 1), res.get(2).getPosition());
    }

    private void undoAll()
    {
        while (undoManager.canUndo())
        {
            undoManager.undo();
        }
    }

    private void redoAll()
    {
        while (undoManager.canRedo())
        {
            undoManager.redo();
        }
    }

}
