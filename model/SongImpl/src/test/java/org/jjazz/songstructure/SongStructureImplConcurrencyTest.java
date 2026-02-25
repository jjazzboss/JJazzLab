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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;
import org.openide.util.Exceptions;

public class SongStructureImplConcurrencyTest
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
    public void setUp() throws UnsupportedEditException, ParseException
    {
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
            System.out.println("==== MISMATCH AFTER UNDO SEQUENCE:");
            System.out.println("sgs after Undo=" + sgs);
            System.out.println("u_sgs after Undo=" + u_sgs);
            assertTrue(b);
        }
    }

    // =========================================================================================================
    // CONCURRENCY TESTS
    // =========================================================================================================

    @Test
    @Timeout(50)
    public void testConcurrentDeepCopyWhileMutating() throws InterruptedException
    {
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
                    SongStructure copy = sgs.getDeepCopy(cls);
                    assertNotNull(copy, "Deep copy should not be null");
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
                                    sgs.setSongPartsRhythm(List.of(spt), newRhythm, null);
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
        assertTrue(deepCopyCount.get() > DEEP_COPY_ITERATIONS * 0.9, "Deep copy should have been called multiple times");
        assertTrue(mutationCount.get() > MUTATION_ITERATIONS * 0.9, "Mutations should have been performed multiple times");

        // Verify SongStructure is still in valid state
        assertNotNull(sgs.getSongParts());
        assertTrue(sgs.getSizeInBars() >= 0);

        System.out.println("Concurrency test completed successfully:");
        System.out.println("  Deep copies: " + deepCopyCount.get());
        System.out.println("  Mutations: " + mutationCount.get());
        System.out.println("  Final song size: " + sgs.getSizeInBars() + " bars");
        System.out.println("  Final parts count: " + sgs.getSongParts().size());

    }


    @Test
    @Timeout(5)
    public void testConcurrentReadersWithSingleWriter() throws InterruptedException
    {
        final int ITERATIONS = 500;
        final AtomicInteger reader1Count = new AtomicInteger(0);
        final AtomicInteger reader2Count = new AtomicInteger(0);
        final AtomicInteger reader3Count = new AtomicInteger(0);
        final AtomicInteger writerCount = new AtomicInteger(0);
        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();

        // Multiple reader threads
        Thread reader1 = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < ITERATIONS; i++)
                {
                    sgs.getDeepCopy(cls);
                    reader1Count.incrementAndGet();
                    if (i % 100 == 0)
                    {
                        Thread.yield();
                    }
                }
            } catch (Throwable t)
            {
                exceptions.add(t);
            }
        }, "Reader-1");

        Thread reader2 = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < ITERATIONS; i++)
                {
                    List<SongPart> spts = sgs.getSongParts();
                    int size = sgs.getSizeInBars();
                    List<Rhythm> rhythms = sgs.getUniqueRhythms(false, false);
                    reader2Count.incrementAndGet();
                    if (i % 100 == 0)
                    {
                        Thread.yield();
                    }
                }
            } catch (Throwable t)
            {
                exceptions.add(t);
            }
        }, "Reader-2");

        Thread reader3 = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < ITERATIONS; i++)
                {
                    sgs.toPositionInNaturalBeats(0);
                    sgs.toPosition(10f);
                    sgs.getSongPart(0);
                    reader3Count.incrementAndGet();
                    if (i % 100 == 0)
                    {
                        Thread.yield();
                    }
                }
            } catch (Throwable t)
            {
                exceptions.add(t);
            }
        }, "Reader-3");

        // Single writer thread
        Thread writer = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < ITERATIONS / 10; i++)
                {
                    try
                    {
                        List<SongPart> parts = sgs.getSongParts();
                        if (!parts.isEmpty())
                        {
                            sgs.setSongPartsName(List.of(parts.get(0)), "Name-" + i);
                            writerCount.incrementAndGet();
                        }
                        Thread.sleep(1);        // Slower writer
                    } catch (InterruptedException ex)
                    {
                        // Expected, continue
                    }
                }
            } catch (Throwable t)
            {
                exceptions.add(t);
            }
        }, "Writer");

        // Start all threads
        reader1.start();
        reader2.start();
        reader3.start();
        writer.start();

        // Wait for completion
        reader1.join();
        reader2.join();
        reader3.join();
        writer.join();

        // Check for exceptions
        if (!exceptions.isEmpty())
        {
            fail("Thread failed: " + exceptions.get(0).getMessage());
        }

        // Verify all threads made progress
        assertTrue(reader1Count.get() > 450, "Reader 1 should have completed iterations");
        assertTrue(reader2Count.get() > 450, "Reader 2 should have completed iterations");
        assertTrue(reader3Count.get() > 450, "Reader 3 should have completed iterations");
        assertTrue(writerCount.get() > 10, "Writer should have completed some mutations");

        System.out.println("Multiple readers test completed successfully:");
        System.out.println("  Reader 1 (deep copy): " + reader1Count.get());
        System.out.println("  Reader 2 (get parts): " + reader2Count.get());
        System.out.println("  Reader 3 (position): " + reader3Count.get());
        System.out.println("  Writer (mutations): " + writerCount.get());

    }

    @Test
    @Timeout(10)
    public void testConcurrentListenerNotifications() throws InterruptedException
    {
        final int ITERATIONS = 200;
        final AtomicInteger listenerCount = new AtomicInteger(0);
        final AtomicInteger mutationCount = new AtomicInteger(0);
        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();

        sgs.addSgsChangeListener((SgsChangeEvent e) -> 
        {
            listenerCount.incrementAndGet();
            // Verify lock is NOT held
            assertFalse(getLock().isWriteLockedByCurrentThread(), "listener must be called without write lock");
        });

        // Mutating thread
        Thread mutator = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < ITERATIONS; i++)
                {
                    try
                    {
                        int sizeBeforeAdd = sgs.getSizeInBars();
                        SongPart newSpt1 = sgs.createSongPart(r44, "Listener-" + i, sizeBeforeAdd, sectionC_44, false);
                        sgs.addSongParts(List.of(newSpt1));
                        mutationCount.incrementAndGet();

                        sizeBeforeAdd = sgs.getSizeInBars();
                        SongPart newSpt2 = sgs.createSongPart(r34, "ListenerBis-" + i, sizeBeforeAdd, sectionB_34, true);
                        sgs.addSongParts(List.of(newSpt2));
                        mutationCount.incrementAndGet();

                        sgs.removeSongParts(List.of(newSpt1));
                        mutationCount.incrementAndGet();


                        Thread.sleep(2);
                    } catch (UnsupportedEditException | InterruptedException ex)
                    {
                        // Expected
                    }
                }
            } catch (Throwable t)
            {
                exceptions.add(t);
            }
        }, "Mutator");

        // Reading thread during mutations
        Thread reader = new Thread(() -> 
        {
            try
            {
                for (int i = 0; i < ITERATIONS * 2; i++)
                {
                    sgs.getSongParts();
                    Thread.yield();
                }
            } catch (Throwable t)
            {
                exceptions.add(t);
            }
        }, "Reader");

        mutator.start();
        reader.start();

        mutator.join();
        reader.join();

        if (!exceptions.isEmpty())
        {
            fail("Thread failed: " + exceptions.get(0).getMessage());
        }

        // Both listener types should have been called for each mutation
        assertEquals(mutationCount.get(), listenerCount.get(), "Listener should be called for each mutation");

        System.out.println("Listener concurrency test completed successfully:");
        System.out.println("  Mutations: " + mutationCount.get());
        System.out.println("  Async notifications: " + listenerCount.get());

        undoManager.endCEdit(UNDO_EDIT);
    }


    // =========================================================================================================
    // Helper methods
    // =========================================================================================================

    private ExecutionManager getExecutionManager()
    {
        return ((SongStructureImpl) sgs).getExecutionManager();
    }

    private ReentrantReadWriteLock getLock()
    {
        return getExecutionManager().getLock();
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
