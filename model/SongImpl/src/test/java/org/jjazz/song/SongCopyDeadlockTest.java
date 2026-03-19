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
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabaseImpl;
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
    private RhythmDatabase rdb;

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

    @BeforeEach
    public void setUp(TestInfo testInfo)
    {
        System.out.println(testInfo.getDisplayName() + " ------");
        rdb = RhythmDatabase.getSharedInstance();
    }

    @Test
    public void testDeadlock1() throws Exception
    {
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
        final int TIME_OUT_SEC = 10;     // Must leave enough time to execute all iterations
        final int DEEP_COPY_ITERATIONS = 3000;
        final int MUTATION_ITERATIONS = 2500;
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
                    if (i % 20 == 0)
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
                    if (i % 5 == 0)
                    {
                        mutateSong(song);
                    }

                    mutationCount.incrementAndGet();
                    if (i % LOG_COUNT == 0)
                    {
                        LOGGER.log(Level.INFO, "songMODIFIERThread i={0}", i);
                    }
                    // Small yield to encourage interleaving
                    if (i % 30 == 0)
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
        boolean b2 = songModifierThread.join(Duration.ofSeconds(2));


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

    private void mutateSong(Song song) throws UnsupportedEditException
    {
        String oldName = song.getName();
        String newName = oldName.startsWith("BLAHH") ? oldName.substring(5) : oldName + "BLAHH";
        song.setName(newName);

        final String PHRASE_NAME = "PhraseName";
        var userPhraseNames = song.getUserPhraseNames();
        var nbPhrases = userPhraseNames.size();
        switch (nbPhrases)
        {
            case 0 ->
            {
                Phrase p = new Phrase(0);
                p.add(new NoteEvent(64, 1, 60, 5));
                song.setUserPhrase(PHRASE_NAME, p);
                p.add(new NoteEvent(60, 1, 60, 1));
            }
            case 1 -> song.removeUserPhrase(PHRASE_NAME);
            default -> throw new IllegalStateException("nbPhrases=" + nbPhrases);
        }
    }

}
