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
package org.jjazz.leadsheet.chordleadsheet;

import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.Lookup;
import org.openide.util.lookup.InstanceContent;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.openide.awt.UndoRedo;
import java.text.ParseException;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_ChordSymbolImpl;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Exceptions;

public class TestLookupMutableObject
{

    UndoRedo.Manager undoManager;
    ChordLeadSheetImpl cls1;
    ChordLeadSheetImpl cls2;
    CLI_SectionImpl cliSection34_b3;
    CLI_SectionImpl cliSection44_b4;
    CLI_SectionImpl cliSection54_b5;
    CLI_ChordSymbolImpl cliChordSymbolF_b3_3;
    CLI_ChordSymbolImpl cliChordSymbolG_b6_0;
    CLI_ChordSymbolImpl cliChordSymbolA_b12_2;
    InstanceContent ic;
    Lookup myLookup;

    public TestLookupMutableObject()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp()
    {
        ic = new InstanceContent();
        myLookup = new AbstractLookup(ic);
        undoManager = new UndoRedo.Manager();
        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        cls2 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        try
        {
            // Test leadsheet init
            cls1.setSize(8);
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Dm7"), new Position(0, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("F#7"), new Position(1, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Bbmaj7#5"), new Position(1, 3)));
            cls1.addSection(new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("D7b9b5"), new Position(2, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("FM7#11"), new Position(4, 1)));
            cls1.addSection(new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Db"), new Position(7, 3f)));

            cls1.addUndoableEditListener(undoManager);

            // COPY
            cls2.setSize(8);
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Dm7"), new Position(0, 0)));
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("F#7"), new Position(1, 0)));
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Bbmaj7#5"), new Position(1, 3)));
            cls2.addSection(new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2));
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("D7b9b5"), new Position(2, 0)));
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("FM7#11"), new Position(4, 1)));
            cls2.addSection(new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5));
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Eb7b9#5"), new Position(5, 0.75f)));
            cls2.addItem(new CLI_ChordSymbolImpl(new ExtChordSymbol("Db"), new Position(7, 3f)));

            // Items to play with
            cliSection34_b3 = new CLI_SectionImpl("NewSection34", TimeSignature.THREE_FOUR, 3);
            cliSection44_b4 = new CLI_SectionImpl("NewSection44", TimeSignature.FOUR_FOUR, 4);
            cliSection54_b5 = new CLI_SectionImpl("NewSection54", TimeSignature.FIVE_FOUR, 5);
            cliChordSymbolF_b3_3 = new CLI_ChordSymbolImpl(new ExtChordSymbol("F-"), new Position(3, 3));
            cliChordSymbolG_b6_0 = new CLI_ChordSymbolImpl(new ExtChordSymbol("G-"), new Position(6, 0));
            cliChordSymbolA_b12_2 = new CLI_ChordSymbolImpl(new ExtChordSymbol("A-"), new Position(12, 2));

        } catch (ParseException ex)
        {
            throw new IllegalStateException("ParseException ex=" + ex);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @After
    public void tearDown()
    {
    }

    // AddItem() --------------------------------------------------
    @Test
    public void testRemoveAfterMove()
    {
        System.out.println("=== testRemoveLookupAfterMove");
        List<? extends CLI_ChordSymbol> items = cls1.getItems(CLI_ChordSymbol.class);
        CLI_ChordSymbolImpl cliCS0 = (CLI_ChordSymbolImpl) items.get(0);
        CLI_ChordSymbolImpl cliCS1 = (CLI_ChordSymbolImpl) items.get(1);
        CLI_ChordSymbolImpl cliCS2 = (CLI_ChordSymbolImpl) items.get(2);
        ic.add(cliCS0);
        ic.remove(cliCS0);
        ic.add(cliCS0);
        ic.add(cliCS1);
        ic.add(cliCS2);
        cliCS0.setData(cliCS0.getData().getTransposedChordSymbol(1, null));
        cliCS1.setData(cliCS1.getData().getTransposedChordSymbol(1, null));
        cliCS2.setData(cliCS2.getData().getTransposedChordSymbol(1, null));
        ic.remove(cliCS0);
        ic.remove(cliCS1);
        ic.remove(cliCS2);
        System.out.println("AFTER CHANGE myLookup=" + myLookup);
        assertTrue(myLookup.lookupAll(CLI_ChordSymbol.class).size() == 0);
    }

    @Test
    public void testRemoveAfterMove2()
    {
        System.out.println("=== testRemoveLookupAfterMove222");
        MutableObject m0 = new MutableObject(10);
        MutableObject m1 = new MutableObject(20);
        MutableObject m2 = new MutableObject(30);
        ic.add(m0);
        m0.setX(202);
        ic.remove(m0);
        System.out.println("AFTER 1st REMOVE  myLookup=" + myLookup);
        ic.add(m0);
        ic.add(m1);
        ic.add(m2);
        System.out.println("BEFORE 2nd CHANGE myLookup=" + myLookup);
        m0.setX(111);
        System.out.println("AFTER 2nd CHANGE  myLookup=" + myLookup);
        ic.remove(m0);
        System.out.println("AFTER 2nd REMOVE  myLookup=" + myLookup);
        assertTrue(myLookup.lookupAll(MutableObject.class).size() == 2);
    }
}

class MutableObject
{

    private int x = 0;

    public MutableObject(int x)
    {
        setX(x);
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public String toString()
    {
        return "x=" + x;
    }

    public boolean equals(Object o)
    {
        if (o instanceof MutableObject)
        {
            return ((MutableObject) o).x == x;
        } else
        {
            return false;
        }
    }

    public int hashCode()
    {
        return x;
    }
}
