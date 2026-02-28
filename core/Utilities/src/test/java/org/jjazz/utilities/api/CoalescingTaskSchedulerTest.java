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
package org.jjazz.utilities.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for CoalescingTaskScheduler.
 */
public class CoalescingTaskSchedulerTest
{

    private CoalescingTaskScheduler scheduler;
    private static final long SHORT_DELAY = 100; // milliseconds
    private static final long TOLERANCE = 50; // timing tolerance


    static
    {
        Utilities.setLoggingFormat(null);
    }

    @BeforeEach
    public void setUp()
    {
        scheduler = null;
    }

    @AfterEach
    public void tearDown()
    {
        if (scheduler != null)
        {
            scheduler.cancel();
        }
    }

    @Test
    public void testBasicExecution() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);

        scheduler.request(() -> 
        {
            counter.incrementAndGet();
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Task should have executed");
        Assertions.assertEquals(1, counter.get(), "Task should execute exactly once");
    }

    @Test
    public void testDebounceMode_LastTaskWins() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, true, null);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Request multiple tasks rapidly
        scheduler.request(() -> results.add("First"));
        Thread.sleep(20);
        scheduler.request(() -> results.add("Second"));
        Thread.sleep(20);
        scheduler.request(() -> 
        {
            results.add("Third");
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Task should have executed");
        Assertions.assertEquals(1, results.size(), "Only one task should execute");
        Assertions.assertEquals("Third", results.get(0), "Last requested task should execute");
    }

    @Test
    public void testDebounceMode_DelayRestarts() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, true, null);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        scheduler.request(() -> 
        {
            counter.incrementAndGet();
            latch.countDown();
        });

        // Wait half the delay, then request again - should restart timer
        Thread.sleep(SHORT_DELAY / 2);
        scheduler.request(() -> 
        {
            counter.incrementAndGet();
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed, "Task should have executed");
        Assertions.assertEquals(1, counter.get(), "Only second task should execute");
        // Should take at least 1.5 * SHORT_DELAY (half delay + full delay)
        Assertions.assertTrue(elapsed >= (SHORT_DELAY * 3 / 2), "Delay should have restarted");
    }

    @Test
    public void testThrottleMode_LastTaskWins() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, false, null);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Request multiple tasks rapidly
        scheduler.request(() -> results.add("First"));
        Thread.sleep(20);
        scheduler.request(() -> results.add("Second"));
        Thread.sleep(20);
        scheduler.request(() -> 
        {
            results.add("Third");
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Task should have executed");
        Assertions.assertEquals(1, results.size(), "Only one task should execute");
        Assertions.assertEquals("Third", results.get(0), "Last requested task should execute");
    }

    @Test
    public void testThrottleMode_DelayDoesNotRestart() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, false, null);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        scheduler.request(() -> 
        {
            counter.incrementAndGet();
            latch.countDown();
        });

        // Wait half the delay, then request again - should NOT restart timer
        Thread.sleep(SHORT_DELAY / 2);
        scheduler.request(() -> 
        {
            counter.incrementAndGet();
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        Assertions.assertTrue(completed, "Task should have executed");
        Assertions.assertEquals(1, counter.get(), "Only second task should execute");
        // Should take approximately SHORT_DELAY, not 1.5 * SHORT_DELAY
        Assertions.assertTrue(elapsed < (SHORT_DELAY * 3 / 2), "Delay should NOT have restarted");
    }

    @Test
    public void testCancel() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY);

        AtomicInteger counter = new AtomicInteger(0);

        scheduler.request(() -> counter.incrementAndGet());

        // Cancel before execution
        Thread.sleep(SHORT_DELAY / 2);
        scheduler.cancel();

        // Wait for what would have been execution time
        Thread.sleep(SHORT_DELAY);

        Assertions.assertEquals(0, counter.get(), "Task should not execute after cancel");
        Assertions.assertFalse(scheduler.hasPendingTask(), "Should have no pending task after cancel");
    }

    @Test
    public void testHasPendingTask() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY);

        Assertions.assertFalse(scheduler.hasPendingTask(), "Should have no pending task initially");

        CountDownLatch latch = new CountDownLatch(1);
        scheduler.request(() -> latch.countDown());

        Assertions.assertTrue(scheduler.hasPendingTask(), "Should have pending task after request");

        latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        // Give a bit of time for cleanup
        Thread.sleep(10);

        Assertions.assertFalse(scheduler.hasPendingTask(), "Should have no pending task after execution");
    }

    @Test
    public void testExceptionHandling_CustomHandler() throws InterruptedException
    {
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        CountDownLatch exceptionLatch = new CountDownLatch(1);

        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, ex -> 
        {
            caughtException.set(ex);
            exceptionLatch.countDown();
        });

        scheduler.request(() -> 
        {
            throw new RuntimeException("Test exception");
        });

        boolean exceptionHandled = exceptionLatch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(exceptionHandled, "Exception handler should be called");
        Assertions.assertNotNull(caughtException.get(), "Exception should be caught");
        Assertions.assertEquals("Test exception", caughtException.get().getMessage(), "Should catch the thrown exception");
    }

    @Test
    public void testExceptionHandling_DefaultHandler() throws InterruptedException
    {
        // Default handler logs the exception - we just verify it doesn't crash
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger afterExceptionCounter = new AtomicInteger(0);

        scheduler.request(() -> 
        {
            throw new RuntimeException("Test exception");
        });

        // Wait for exception to be handled
        Thread.sleep(SHORT_DELAY + TOLERANCE);

        // Verify scheduler still works after exception
        scheduler.request(() -> 
        {
            afterExceptionCounter.incrementAndGet();
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Scheduler should still work after exception");
        Assertions.assertEquals(1, afterExceptionCounter.get(), "Subsequent task should execute");
    }


    @Test
    public void testMultipleRequestsInDebounceMode() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, true, null);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger executionCount = new AtomicInteger(0);
        AtomicReference<String> lastValue = new AtomicReference<>();

        // Simulate rapid fire requests (like typing)
        for (int i = 1; i <= 10; i++)
        {
            final String value = "Request-" + i;
            scheduler.request(() -> 
            {
                executionCount.incrementAndGet();
                lastValue.set(value);
                latch.countDown();
            });
            Thread.sleep(SHORT_DELAY / 3); // Request faster than delay
        }

        boolean completed = latch.await(SHORT_DELAY * 5, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Task should eventually execute");
        Assertions.assertEquals(1, executionCount.get(), "Should execute only once");
        Assertions.assertEquals("Request-10", lastValue.get(), "Should execute last request");
    }

    @Test
    public void testMultipleWindowsInThrottleMode() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY, false, null);

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger executionCount = new AtomicInteger(0);

        // First window
        scheduler.request(() -> 
        {
            executionCount.incrementAndGet();
            latch.countDown();
        });

        // Wait for first window to complete
        Thread.sleep(SHORT_DELAY + TOLERANCE);

        // Second window
        scheduler.request(() -> 
        {
            executionCount.incrementAndGet();
            latch.countDown();
        });

        boolean completed = latch.await(SHORT_DELAY + TOLERANCE, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Both tasks should execute");
        Assertions.assertEquals(2, executionCount.get(), "Should execute twice (one per window)");
    }

    @Test
    public void testThreadSafety() throws InterruptedException
    {
        scheduler = new CoalescingTaskScheduler(SHORT_DELAY);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch executionLatch = new CountDownLatch(1);
        AtomicInteger executionCount = new AtomicInteger(0);

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // Create multiple threads that will all request tasks simultaneously
        for (int i = 0; i < threadCount; i++)
        {
            final int threadId = i;
            threads[i] = new Thread(() -> 
            {
                try
                {
                    startLatch.await(); // Wait for signal to start
                    for (int j = 0; j < 10; j++)
                    {
                        scheduler.request(() -> 
                        {
                            executionCount.incrementAndGet();
                            executionLatch.countDown();
                        });
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        // Release all threads at once
        startLatch.countDown();

        // Wait for threads to finish
        for (Thread thread : threads)
        {
            thread.join();
        }

        // Wait for execution
        boolean completed = executionLatch.await(SHORT_DELAY * 3, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(completed, "Task should execute");
        // Should execute only once despite many concurrent requests
        Assertions.assertEquals(1, executionCount.get(), "Should coalesce all requests into one execution");
    }
}
