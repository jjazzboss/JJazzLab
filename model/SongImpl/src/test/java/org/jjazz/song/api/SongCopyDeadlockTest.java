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
package org.jjazz.song.api;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_Variation;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * JUnit 5 test to reproduce the deadlock described in issue #676.
 * <p>
 * <b>Deadlock Topology:</b>
 * <ul>
 * <li><b>Thread A (background music generation):</b> Holds the Song monitor via {@code synchronized Song.getDeepCopy()}) and then attempts to acquire the
 * SongStructureImpl monitor to deep-copy the SongStructure.</li>
 * <li><b>Thread B (EDT-like):</b> User makes a change in the UI, {@code synchronized SongStructureImpl.setRhythmParameterValue(...)}) is called which fires
 * events without releasing the obtained monitor. A listener (Song) calls a synchronized method on Song (e.g., {@code fireIsModified()}), requiring the Song
 * monitor, which he can not get ==> DEADLOCK</li>
 * </ul>
 *
 * @see <a href="https://github.com/jjazzboss/JJazzLab/issues/676">Issue #676</a>
 */
public class SongCopyDeadlockTest
{

    private static final Logger LOGGER = Logger.getLogger(SongCopyDeadlockTest.class.getSimpleName());
    private static DefaultRhythmDatabase rdb;

    static
    {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %3$s  %5$s %n");
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

        ChordLeadSheet cls = SongFactory.getDefault().createEmptyChordLeadSheet("A", TimeSignature.FOUR_FOUR, 4, "C7");
        Song song = SongFactory.getDefault().createSong("TestSong", cls);

        int TIME_OUT_SEC = 5;
        assertTrue(executeDeadlockScenario(TIME_OUT_SEC, song, sg -> changeRhythmParameter(sg)));
    }

    // ========================================================================================================
    // Private methods
    // ========================================================================================================

    /**
     * Run 2 threads: one is continuously calling Song.getSongCopy(), the other continuously modifying the same song.
     * <p>
     * If no deadlock method should return before timeOutSec.
     *
     * @param timeOutSec
     * @param song
     * @param songModifier Perform a modification of song
     * @return
     * @throws java.lang.Exception
     */
    private boolean executeDeadlockScenario(int timeOutSec, Song song, Consumer<Song> songModifier) throws Exception
    {
        LOGGER.log(Level.INFO, "executeDeadlockScenario() -- timeOutSec={0}", timeOutSec);

        final long COUNT = 1000;
        final long LOG_COUNT = COUNT / 10;


        // Thread 1 : loop on Song.getSongCopy() 
        Thread songDeepCopyThread = new Thread(() -> 
        {
            LOGGER.info("songDeepCopyThread started --");
            try
            {
                for (long i = 0; i < COUNT; i++)
                {
                    Song copy = song.getDeepCopy(false);
                    if (i % LOG_COUNT == 0 && copy != null)
                    {
                        LOGGER.log(Level.INFO, "songDeepCopyThread i={0}", i);
                    }
                    Thread.sleep((long) (Math.random() * 2));
                }
            } catch (InterruptedException ex)
            {
                LOGGER.info(() -> "songDeepCopyThread interrupted ex=" + ex.getMessage());
            }

        }, "songDeepCopyThread");


        // Thread 2: loop on calling songMofifier
        Thread songModifierThread = new Thread(() -> 
        {
            LOGGER.info("songModifierThread started --");
            try
            {
                for (long j = 0; j < COUNT; j++)
                {
                    songModifier.accept(song);
                    if (j % LOG_COUNT == 0)
                    {
                        LOGGER.log(Level.INFO, "songModifierThread j={0}", j);
                    }
                    Thread.sleep((long) (Math.random() * 2));
                }
            } catch (InterruptedException ex)
            {
                LOGGER.info(() -> "songModifierThread interrupted ex=" + ex.getMessage());
            }

        }, "songModifierThread");


        // Start both threads
        songDeepCopyThread.start();
        songModifierThread.start();


        // Wait for both threads to complete with a reasonable timeout
        boolean b1 = songDeepCopyThread.join(Duration.ofSeconds(timeOutSec));       // true if thread completed before timeOutSec
        boolean b2 = songModifierThread.join(Duration.ofMillis(1));
        LOGGER.log(Level.INFO, "executeDeadlockScenario() b1={0} b2={1}", new Object[]
        {
            b1, b2
        });

        return b1 && b2;
    }


    private void changeRhythmParameter(Song song)
    {
        SongStructure sgs = song.getSongStructure();
        SongPart spt0 = sgs.getSongParts().get(0);
        var r = spt0.getRhythm();
        var rpVariation = RP_SYS_Variation.getVariationRp(r);
        assert rpVariation != null : "r=" + r;
        String rpValue = spt0.getRPValue(rpVariation);
        String newRpValue = rpVariation.getNextValue(rpValue);
        sgs.setRhythmParameterValue(spt0, rpVariation, newRpValue);
    }
}
