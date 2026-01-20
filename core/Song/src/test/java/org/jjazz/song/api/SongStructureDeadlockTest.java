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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test to reproduce the deadlock described in issue #676.
 * <p>
 * <b>Deadlock Topology:</b>
 * <ul>
 * <li><b>Thread A (background generation):</b> Holds the Song monitor (via synchronized {@code Song.getDeepCopy()})
 * and then attempts to acquire the SongStructureImpl monitor to deep-copy the SongStructure.</li>
 * <li><b>Thread B (EDT-like):</b> Holds the SongStructureImpl monitor (inside synchronized
 * {@code SongStructureImpl.setRhythmParameterValue(...)}) and fires events without releasing that monitor.
 * A listener (Song) calls a synchronized method on Song (e.g., {@code fireIsModified()}), requiring the Song monitor.</li>
 * </ul>
 * <p>
 * This test uses {@link CountDownLatch}es to coordinate the two threads and trigger the deadlock deterministically.
 * The test uses {@link org.junit.jupiter.api.Assertions#assertTimeoutPreemptively} to ensure it fails (rather than hanging)
 * if the deadlock occurs, preventing CI from hanging indefinitely.
 * <p>
 * <b>Expected behavior after the fix:</b> The test should pass because mutators no longer dispatch listener callbacks
 * while holding the SongStructure lock, eliminating the lock inversion.
 *
 * @see <a href="https://github.com/jjazzboss/JJazzLab/issues/676">Issue #676</a>
 */
public class SongStructureDeadlockTest
{
    private static final Logger LOGGER = Logger.getLogger(SongStructureDeadlockTest.class.getSimpleName());
    private static DefaultRhythmDatabase rdb;

    @BeforeAll
    public static void setUpClass() throws Exception
    {
        rdb = (DefaultRhythmDatabase) RhythmDatabase.getDefault();
        rdb.addRhythmsFromRhythmProviders(false, false, false);
        LOGGER.info("RhythmDatabase initialized: " + rdb.toStatsString());
    }

    /**
     * Test the deadlock scenario with two threads:
     * - Thread 1: Calls Song.getDeepCopy() which holds Song monitor and tries to acquire SongStructure monitor
     * - Thread 2: Calls SongStructure.setRhythmParameterValue() which holds SongStructure monitor and triggers
     *            Song listener that tries to acquire Song monitor
     * <p>
     * The test uses latches to coordinate execution to maximize the chance of triggering the deadlock.
     * It uses assertTimeoutPreemptively to ensure the test fails if deadlock occurs, rather than hanging CI.
     */
    @Test
    public void testDeadlockBetweenSongDeepCopyAndSetRhythmParameterValue() throws Exception
    {
        // Create a song with a chord leadsheet
        ChordLeadSheetImpl cls = new ChordLeadSheetImpl("TestSection", TimeSignature.FOUR_FOUR, 8);
        Song song = SongFactory.getInstance().createSong("TestSong", cls);
        SongStructure sgs = song.getSongStructure();

        // Get the first song part and check if it has any rhythm parameters
        List<SongPart> songParts = sgs.getSongParts();
        Assumptions.assumeFalse(songParts.isEmpty(), "Song must have at least one song part");
        
        SongPart spt = songParts.get(0);
        List<RhythmParameter<?>> rps = spt.getRhythm().getRhythmParameters();
        Assumptions.assumeFalse(rps.isEmpty(), "First song part's rhythm must have at least one rhythm parameter");

        // Find a rhythm parameter we can modify
        RhythmParameter<?> rp = rps.get(0);
        Object currentValue = spt.getRPValue(rp);
        
        // Try to find an alternative value for the parameter
        Object alternativeValue = findAlternativeValue(rp, currentValue);
        Assumptions.assumeTrue(alternativeValue != null, 
                "Could not find an alternative value for rhythm parameter " + rp.getId());

        LOGGER.info("Testing deadlock scenario with SongPart=" + spt + ", RhythmParameter=" + rp.getId() 
                + ", currentValue=" + currentValue + ", newValue=" + alternativeValue);

        // Run the deadlock test with a timeout to prevent CI from hanging
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            executeDeadlockScenario(song, sgs, spt, rp, alternativeValue);
        }, "Deadlock detected: Test timed out waiting for threads to complete. "
           + "This indicates the lock inversion issue has not been fixed.");
    }

    /**
     * Executes the actual deadlock scenario with two coordinated threads.
     */
    private void executeDeadlockScenario(Song song, SongStructure sgs, SongPart spt, 
                                         RhythmParameter rp, Object alternativeValue) throws Exception
    {
        // Latches to coordinate thread execution
        CountDownLatch thread1Ready = new CountDownLatch(1);
        CountDownLatch thread2Ready = new CountDownLatch(1);

        
        AtomicReference<Throwable> thread1Exception = new AtomicReference<>();
        AtomicReference<Throwable> thread2Exception = new AtomicReference<>();

        // Thread 1: Attempts to deep copy the song (holds Song monitor, needs SongStructure monitor)
        Thread deepCopyThread = new Thread(() -> {
            try
            {
                LOGGER.fine("Thread 1: Ready to acquire Song monitor");
                thread1Ready.countDown();
                
                // Wait for Thread 2 to be ready
                if (!thread2Ready.await(5, TimeUnit.SECONDS))
                {
                    throw new IllegalStateException("Thread 2 did not become ready in time");
                }
                
                // Small delay to let Thread 2 start acquiring its lock
                Thread.sleep(50);
                
                LOGGER.fine("Thread 1: Calling getDeepCopy() - will acquire Song monitor");
                // This will hold the Song monitor and then try to acquire SongStructure monitor
                Song copy = song.getDeepCopy();
                
                LOGGER.fine("Thread 1: Successfully created deep copy");
                assertNotNull(copy, "Deep copy should not be null");
            } catch (Exception e)
            {
                LOGGER.severe("Thread 1 exception: " + e.getMessage());
                thread1Exception.set(e);
            }
        }, "DeepCopyThread");

        // Thread 2: Calls setRhythmParameterValue (holds SongStructure monitor, triggers Song listener)
        Thread setParameterThread = new Thread(() -> {
            try
            {
                LOGGER.fine("Thread 2: Ready to call setRhythmParameterValue");
                thread2Ready.countDown();
                
                // Wait for Thread 1 to be ready
                if (!thread1Ready.await(5, TimeUnit.SECONDS))
                {
                    throw new IllegalStateException("Thread 1 did not become ready in time");
                }
                
                // Small delay to let Thread 1 start acquiring its lock
                Thread.sleep(50);
                
                LOGGER.fine("Thread 2: Calling setRhythmParameterValue() - will acquire SongStructure monitor");
                // This will hold the SongStructure monitor and then fire events that trigger Song.fireIsModified()
                // which needs the Song monitor
                setRhythmParameterValueUnchecked(sgs, spt, rp, alternativeValue);
                
                LOGGER.fine("Thread 2: Successfully set rhythm parameter value");
            } catch (Exception e)
            {
                LOGGER.severe("Thread 2 exception: " + e.getMessage());
                thread2Exception.set(e);
            }
        }, "SetParameterThread");

        // Start both threads
        deepCopyThread.start();
        setParameterThread.start();

        // Wait for both threads to complete with a reasonable timeout
        deepCopyThread.join(8000); // 8 seconds max
        setParameterThread.join(8000); // 8 seconds max

        // Check if threads completed
        boolean thread1Finished = !deepCopyThread.isAlive();
        boolean thread2Finished = !setParameterThread.isAlive();
        assertTrue(thread1Finished, "Thread 1 (deep copy) did not finish in time - possible deadlock");
        assertTrue(thread2Finished, "Thread 2 (set parameter) did not finish in time - possible deadlock");

        // Check for exceptions in threads
        if (thread1Exception.get() != null)
        {
            throw new AssertionError("Thread 1 threw an exception", thread1Exception.get());
        }
        if (thread2Exception.get() != null)
        {
            throw new AssertionError("Thread 2 threw an exception", thread2Exception.get());
        }

        LOGGER.info("Deadlock test passed: Both threads completed successfully");
    }

    /**
     * Helper method to call setRhythmParameterValue with unchecked cast.
     * This is necessary because we're working with RhythmParameter<?> at runtime.
     */
    @SuppressWarnings("unchecked")
    private <T> void setRhythmParameterValueUnchecked(SongStructure sgs, SongPart spt, 
                                                       RhythmParameter rp, Object value)
    {
        sgs.setRhythmParameterValue(spt, (RhythmParameter<T>) rp, (T) value);
    }

    /**
     * Attempts to find an alternative value for a rhythm parameter.
     * For testing purposes, we only need a value that's different from the current one.
     */
    @SuppressWarnings("unchecked")
    private Object findAlternativeValue(RhythmParameter<?> rp, Object currentValue)
    {
        // Try to get the default value if it's different
        Object defaultValue = rp.getDefaultValue();
        if (defaultValue != null && !defaultValue.equals(currentValue))
        {
            return defaultValue;
        }

        // If the parameter has a method to get possible values, try to find an alternative
        // For now, we'll check if it's an enumerable type via reflection
        try
        {
            // Try to find a "getValues" or similar method that returns possible values
            var method = rp.getClass().getMethod("getValues");
            Object result = method.invoke(rp);
            if (result instanceof List)
            {
                List<?> values = (List<?>) result;
                for (Object value : values)
                {
                    if (!value.equals(currentValue))
                    {
                        return value;
                    }
                }
            }
        } catch (Exception e)
        {
            // Method doesn't exist or failed, try other approaches
        }

        // Try common value types
        if (currentValue instanceof Boolean)
        {
            return !(Boolean) currentValue;
        }
        else if (currentValue instanceof Integer)
        {
            int intValue = (Integer) currentValue;
            return intValue == 0 ? 1 : 0;
        }
        else if (currentValue instanceof String)
        {
            return currentValue.equals("test") ? "alternative" : "test";
        }

        // Could not find an alternative value
        return null;
    }
}
