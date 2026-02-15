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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.item.CLI_ChordSymbolImpl;
import org.jjazz.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Exceptions;

public class ChordLeadSheetImplConcurrencyTest
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

    public ChordLeadSheetImplConcurrencyTest()
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
        // System.out.println("setUp()");
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

    @After
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



    // =========================================================================================================
    // CONCURRENCY TESTS
    // =========================================================================================================
    @Test(timeout = 50000) // 5 second timeout to detect deadlocks
    public void testConcurrentDeepCopyWhileMutating() throws InterruptedException
    {
        System.out.println("=== testConcurrentDeepCopyWhileMutating");
        final int DEEP_COPY_ITERATIONS = 2000;
        final int MUTATION_ITERATIONS = DEEP_COPY_ITERATIONS / 2;
        final AtomicInteger deepCopyCount = new AtomicInteger(0);
        final AtomicInteger mutationCount = new AtomicInteger(0);
        final AtomicReference<Throwable> readerException = new AtomicReference<>();
        final AtomicReference<Throwable> writerException = new AtomicReference<>();

        // Thread 1: Repeatedly calls getDeepCopy (read operations)
        Thread readerThread = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < DEEP_COPY_ITERATIONS; i++)
                {
                    ChordLeadSheet copy = cls1.getDeepCopy();
                    assertNotNull("Deep copy should not be null", copy);
                    assertTrue(copy.getSizeInBars() > 0);
                    deepCopyCount.incrementAndGet();

                    // Also test read operations
                    if (i % 10 == 0)
                    {
                        List<CLI_ChordSymbol> items = cls1.getItems(CLI_ChordSymbol.class);
                        assertNotNull(items);
                        assertTrue(cls1.getSizeInBars() > 0);
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
                t.printStackTrace();
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
                        switch (i % 6)
                        {
                            case 0 ->
                            {
                                // Add a chord symbol
                                cls1.addItem(cliChordSymbolG_b6_0);
                                mutationCount.incrementAndGet();
                            }

                            case 1 ->
                            {
                                // Remove a chord symbol
                                cls1.removeItem(cliChordSymbolG_b6_0);
                                mutationCount.incrementAndGet();
                            }

                            case 2 ->
                            {
                                // Move a chord symbol
                                var item = cls1.getItems(0, 0, CLI_ChordSymbol.class).get(0);
                                assert item.getData().getName().equals("Dm7");
                                var pos = item.getPosition();
                                var newPos = pos.setBeat(pos.isFirstBarBeat() ? 0.5f : 0f);
                                cls1.moveItem(item, newPos);
                                mutationCount.incrementAndGet();
                            }

                            case 3 ->
                            {
                                // Move a section
                                var item = cls1.getItems(CLI_Section.class).get(1);
                                int bar = item.getPosition().getBar();
                                var newBar = bar == 2 ? 3 : 2;
                                cls1.moveSection(item, newBar);
                                mutationCount.incrementAndGet();
                            }

                            case 4 ->
                            {
                                // Change timesignature
                                var item = cls1.getSection(0);
                                var ts = item.getData().getTimeSignature();
                                var newTs = ts == TimeSignature.FOUR_FOUR ? TimeSignature.FIVE_FOUR : TimeSignature.FOUR_FOUR;
                                cls1.setSectionTimeSignature(item, newTs);
                                mutationCount.incrementAndGet();
                            }

                            case 5 ->
                            {
                                // Change size
                                var size = cls1.getSizeInBars();
                                var newSize = size == 8 ? 12 : 8;
                                cls1.setSizeInBars(newSize);
                                mutationCount.incrementAndGet();
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
                t.printStackTrace();
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

        // Verify ChordLeadSheet is still in valid state
        assertTrue(cls1.getItems(CLI_ChordSymbol.class).size() > 4);
        assertTrue(cls1.getSizeInBars() >= 0);

        System.out.println("Concurrency test completed successfully:");
        System.out.println("  Deep copies: " + deepCopyCount.get());
        System.out.println("  Mutations: " + mutationCount.get());
        System.out.println("  Final song size: " + cls1.getSizeInBars() + " bars");
        System.out.println("  Final chord items count: " + cls1.getItems(CLI_ChordSymbol.class).size());

    }


    // Undo --------------------------------------------------
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
