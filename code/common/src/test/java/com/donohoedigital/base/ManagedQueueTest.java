/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * 
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials) 
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 * 
 * For inquiries regarding commercial licensing of this source code or 
 * the use of names, logos, images, text, or other assets, please contact 
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.base;

import junit.framework.*;
import org.apache.log4j.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jan 10, 2009
 * Time: 3:18:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManagedQueueTest extends TestCase
{
    private static final Logger logger = Logger.getLogger(ManagedQueueTest.class);

    private Thread mainThread;
    private final List<SampleItem> messages = new ArrayList<SampleItem>();
    private static final SampleItem SAMPLE = new SampleItem();

    @Override
    public void setUp()
    {
        mainThread = Thread.currentThread();
    }

    private long delay = 0;

    private static class SampleItem
    {
    }

    private class TestQueue extends ManagedQueue<SampleItem>
    {
        public TestQueue(int capacity, long waitLimitWarningMillis, long waitLimitErrorMillis)
        {
            super(capacity, waitLimitWarningMillis, waitLimitErrorMillis, new SampleItem());
            start();
        }

        @Override
        protected BlockingQueue<SampleItem> createQueue(int c)
        {
            return new ArrayBlockingQueue<SampleItem>(c);
        }

        @Override
        protected void processQueueItem(SampleItem item)
        {
            // handle message should come from different thread
            assertNotSame(mainThread, Thread.currentThread());
            messages.add(item);

            if (delay > 0)
            {
                logger.debug(Thread.currentThread().getName() + " sleeping " + delay);
                try
                {
                    Thread.sleep(delay);
                }
                catch (InterruptedException e)
                {
                    logger.warn(Thread.currentThread() + " interrupted while sleeping " + delay);
                }
            }
        }
    }

    public void testQueue()
    {
        TestQueue queue = new TestQueue(10, 100, 0);
        int num = 20;
        for (int i = 0; i < num; i++)
        {
            queue.add(SAMPLE);
        }
        queue.stop(true);
        assertFalse("thread dead", queue.getProcessor().isAlive());
        assertEquals(num, messages.size());
    }

    public void testSlowAdd()
    {
        delay = 100;
        TestQueue queue = new TestQueue(1, 100, 200);
        queue.add(SAMPLE); // first gets processed right away, but held up by delay
        queue.add(SAMPLE); // second one fills up queue
        queue.add(SAMPLE); // should time out once (visual test - WARN message should show up in log)
        queue.stop(true);
        assertFalse("thread dead", queue.getProcessor().isAlive());
        assertEquals(3, messages.size());
    }

    public void testOverflow()
    {
        delay = TimeConstants.SECOND;
        TestQueue queue = new TestQueue(1, 1, 1);
        queue.add(SAMPLE); // first gets processed right away, but held up by delay
        queue.add(SAMPLE); // second one fills up queue
        try
        {
            queue.add(SAMPLE); // this one should time out
            fail("Should have thrown exception");
        }
        catch (ApplicationError o)
        {
            logger.debug("Expected error: " + o.getMessage());
        }
        queue.stop(false);
        assertFalse("thread dead", queue.getProcessor().isAlive());
    }

    public void testQuitEarly()
    {
        delay = TimeConstants.MILLISECOND * 10;
        TestQueue queue = new TestQueue(1000, 100, 100);
        int num = 500;
        for (int i = 0; i < num; i++)
        {
            queue.add(SAMPLE);
        }
        queue.stop(false);
        assertFalse("thread dead", queue.getProcessor().isAlive());
        assertTrue("Not all messages should have been processed", num > messages.size());
    }

    public void testInterrupt()
    {
        TestQueue queue = new TestQueue(10, 100, 100);
        Utils.sleepMillis(TimeConstants.TENTH_SECOND);
        queue.getProcessor().interrupt();
        int num = 2000;
        for (int i = 0; i < num; i++)
        {
            queue.add(SAMPLE);
            if (i % 5 == 0)
            {
                queue.getProcessor().interrupt();
            }
        }
        queue.stop(true);
        assertFalse("thread dead", queue.getProcessor().isAlive());
        assertEquals(num, messages.size());
    }

    public void testInterruptDuringShutdown()
    {
        delay = TimeConstants.SECOND * 2;
        TestQueue queue = new TestQueue(1, 1, 1);
        queue.add(SAMPLE); // first gets processed right away, but held up by delay
        queue.add(SAMPLE); // second one fills up queue (use handleMessage for code coverage)
        assertTrue("max one message should have been processed", messages.size() <= 1);
        new Interrupter(Thread.currentThread()).start();
        queue.stop(true); // waits because queue is full, should be interrupted
        assertFalse("thread dead", queue.getProcessor().isAlive());
        assertTrue("max one message should have been processed", messages.size() <= 1);
    }

    public void testQuitNoItemsNoFinish()
    {
        TestQueue queue = new TestQueue(1, 1, 1);
        Utils.sleepMillis(100);  // wait for thread to start
        long now = System.currentTimeMillis();
        queue.stop(false);
        long after = System.currentTimeMillis();
        assertTrue(after - now < ManagedQueue.WAIT_FOR_THREAD_FINISH);
        assertFalse("thread dead", queue.getProcessor().isAlive());
    }

    public void testInterruptDuringAdd()
    {
        delay = TimeConstants.SECOND * 1;
        TestQueue queue = new TestQueue(1, TimeConstants.MINUTE, 100);
        queue.add(SAMPLE); // first gets processed right away, but held up by delay
        queue.add(SAMPLE); // second one fills up queue (use handleMessage for code coverage)
        assertTrue("max one message should have been processed", messages.size() <= 1);
        new Interrupter(Thread.currentThread()).start();
        try
        {
            queue.add(SAMPLE); // should be interrupted since wait is MINUTE, but interrupted goes off sooner
            fail("should have thrown exception");
        }
        catch (ApplicationError o)
        {
            logger.debug("Expected error: " + o.getMessage());
        }
        queue.stop(true);
        assertFalse("thread dead", queue.getProcessor().isAlive());
    }

    private class Interrupter extends Thread
    {
        final Thread interruptMe;

        public Interrupter(Thread interruptMe)
        {
            this.interruptMe = interruptMe;
        }

        @Override
        public void run()
        {
            logger.debug("Interrupting: " + interruptMe.getName());
            Utils.sleepMillis(TimeConstants.QUARTER_SECOND);
            interruptMe.interrupt();
        }
    }
}

