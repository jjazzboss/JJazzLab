/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2025 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.song;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.ClsCyclicMutator;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songstructure.SgsCyclicMutator;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * JUnit 5 test to reproduce the deadlock described in issue #676 (Song.getDeepCopy() while user makes song changes via UI).
 * <p>
 * @see <a href="https://github.com/jjazzboss/JJazzLab/issues/676">Issue #676</a>
 */
public class SongCopyDeadlockTest
{

    private static final Logger LOGGER = Logger.getLogger(SongCopyDeadlockTest.class.getSimpleName());
    private static DefaultRhythmDatabase rdb;

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
        LOGGER.log(Level.INFO, "RhythmDatabase initialized: {0}", rdb.toStatsString());
        assert !rdb.getRhythms().isEmpty();
    }

    @Test
    public void testDeadlock1() throws Exception
    {
        LOGGER.log(Level.INFO, "testDeadlock1() --");

        ChordLeadSheet cls = SongFactory.getDefault().createSampleChordLeadSheet("A", 12);
        Song song = SongFactory.getDefault().createSong("TestSong", cls);

        executeDeadlockScenario(song);
    }

    // ========================================================================================================
    // Private methods
    // ========================================================================================================

    /**
     * Run 2 threads: one is continuously calling Song.getSongCopy(), the other continuously modifying the same song.
     * <p>
     *
     * @param song
     * @return 
     * @throws java.lang.InterruptedException
     */
    private boolean executeDeadlockScenario(Song song) throws InterruptedException
    {
        final int TIME_OUT_SEC = 7;     // Must leave enough time to execute all DEEP_COPY_ITERATION (a DeepCopy is longer than a mutation)
        final int DEEP_COPY_ITERATIONS = 3000;
        final int MUTATION_ITERATIONS = DEEP_COPY_ITERATIONS;
        final int LOG_COUNT = 1000;
        final AtomicInteger deepCopyCount = new AtomicInteger(0);
        final AtomicInteger mutationCount = new AtomicInteger(0);
        final AtomicReference<Throwable> readerException = new AtomicReference<>();
        final AtomicReference<Throwable> writerException = new AtomicReference<>();

        LOGGER.log(Level.INFO, "executeDeadlockScenario() -- TIME_OUT_SEC={0}sec. DEEP_COPY_ITERATIONS={1}", new Object[]
        {
            TIME_OUT_SEC, DEEP_COPY_ITERATIONS
        });

        // Thread 1 : loop on Song.getSongCopy() 
        Thread songDeepCopyThread = new Thread(() -> 
        {
            LOGGER.info("songDeepCopyThread started --");

            try
            {
                for (long i = 0; i < DEEP_COPY_ITERATIONS; i++)
                {
                    Song copy = song.getDeepCopy(false);
                    deepCopyCount.incrementAndGet();
                    if (i % LOG_COUNT == 0 && copy != null)
                    {
                        LOGGER.log(Level.INFO, "songDeepCopyThread i={0}", i);
                    }
                    if (Math.random() > 0.2d)
                    {
                        Thread.yield();
                    }
                }
            } catch (Throwable t)
            {
                readerException.set(t);
            }
            LOGGER.log(Level.INFO, "songDeepCopyThread finished OK");

        }, "songDeepCopyThread");


        // Thread 2: loop on calling songMofifier
        Thread songModifierThread = new Thread(() -> 
        {
            LOGGER.info("songModifierThread started --");
            ClsCyclicMutator clsMutator = new ClsCyclicMutator(song.getChordLeadSheet());
            SgsCyclicMutator sgsMutator = new SgsCyclicMutator(song.getSongStructure());

            try
            {
                for (long i = 0; i < MUTATION_ITERATIONS; i++)
                {
                    clsMutator.mutate();
                    sgsMutator.mutate();

                    mutationCount.incrementAndGet();
                    if (i % LOG_COUNT == 0)
                    {
                        LOGGER.log(Level.INFO, "songMODIFIERThread i={0}", i);
                    }
                    // Small yield to encourage interleaving
                    if (Math.random() > 0.2d)
                    {
                        Thread.yield();
                    }
                }
            } catch (Throwable t)
            {
                writerException.set(t);
            }
            LOGGER.log(Level.INFO, "songMODIFIERThread finished OK");

        }, "songModifierThread");


        // Start both threads
        songDeepCopyThread.start();
        songModifierThread.start();


        // Wait for both threads to complete within TIME_OUT_SEC
        // throws InterruptedException
        boolean b1 = songDeepCopyThread.join(Duration.ofSeconds(TIME_OUT_SEC));       // true if thread completed before TIME_OUT_SEC
        boolean b2 = songModifierThread.join(Duration.ofMillis(100));


        LOGGER.log(Level.INFO, "executeDeadlockScenario() DEEP_COPY_ITERATIONS={0} deepCopyCount={1}", new Object[]
        {
            DEEP_COPY_ITERATIONS, deepCopyCount.get()
        });
        LOGGER.log(Level.INFO, "executeDeadlockScenario() MUTATION_ITERATIONS={0} mutationCount={1}", new Object[]
        {
            MUTATION_ITERATIONS, mutationCount.get()
        });

        // Check for exceptions
        if (readerException.get() != null)
        {
            fail("Reader thread failed: " + readerException.get());

        }
        if (writerException.get() != null)
        {
            fail("Writer thread failed: " + writerException.get());
        }

        assertTrue(b1 && b2, "b1=" + b1 + " b2=" + b2);
        return b1 && b2;
    }

}
