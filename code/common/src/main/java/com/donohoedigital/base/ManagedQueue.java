/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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

import org.apache.logging.log4j.*;

import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jan 10, 2009
 * Time: 3:15:11 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class ManagedQueue<T>
{
    private static final Logger logger = LogManager.getLogger(ManagedQueue.class);

    protected final int capacity;
    protected final long waitLimitWarningMillis;
    protected final long waitLimitErrorMillis;
    protected final T quitItem;
    private final String threadName;

    protected BlockingQueue<T> queue;
    private Thread processor;
    private volatile boolean done = false;
    private boolean stopping = false;
    static final long WAIT_FOR_THREAD_FINISH = TimeConstants.SECOND;

    /**
     * Construct
     *
     * @param capacity               max amount queue can hold
     * @param waitLimitWarningMillis amount to wait attempting to add to queue before printing warning
     * @param waitLimitErrorMillis   after warning, try and add to queue again, throwing exception after this amount of time
     * @param quitItem               an instance of T that is used to indicate last item on the queue
     */
    public ManagedQueue(int capacity, long waitLimitWarningMillis, long waitLimitErrorMillis, T quitItem)
    {
        this(capacity, waitLimitWarningMillis, waitLimitErrorMillis, quitItem, null);
    }

    /**
     * Construct
     *
     * @param capacity               max amount queue can hold
     * @param waitLimitWarningMillis amount to wait attempting to add to queue before printing warning
     * @param waitLimitErrorMillis   after warning, try and add to queue again, throwing exception after this amount of time
     * @param quitItem               an instance of T that is used to indicate last item on the queue
     * @param threadName             name for thread (if null, defaults to class' simple name)
     */
    public ManagedQueue(int capacity, long waitLimitWarningMillis, long waitLimitErrorMillis, T quitItem, String threadName)
    {
        this.capacity = capacity;
        this.waitLimitWarningMillis = waitLimitWarningMillis;
        this.waitLimitErrorMillis = waitLimitErrorMillis;
        this.quitItem = quitItem;
        this.threadName = threadName;
    }

    /**
     * Thread name
     */
    private String getThreadName()
    {
        return threadName == null ? getClass().getSimpleName() : threadName;
    }

    /**
     * Subclass to create actual queue we should use
     */
    protected abstract BlockingQueue<T> createQueue(int c);

    /**
     * Get the thread this queue is running in
     */
    public Thread getProcessor()
    {
        return processor;
    }

    /**
     * start the queue
     */
    public void start()
    {
        ApplicationError.assertTrue(queue == null, "Queue already started");
        queue = createQueue(capacity);
        processor = new QueueThread();
        processor.start();
    }

    /**
     * Waits for thread to processes all items currently on the queue
     * then returns.  Synchronized so only one call to stop at a time.
     */
    public synchronized void stop(boolean finishQueue)
    {
        // don't call more than once
        if (stopping) return;
        stopping = true;

        // if we don't want to finish the queue, set the done flag
        // so we only finish the current item.  Offer item to with no
        // delay to wake queue if nothing on it
        if (!finishQueue)
        {
            done = true;
            queue.offer(quitItem);
        }
        // otherwise, wait to put a queue item there, but only wait so long
        else
        {
            boolean added = false;
            try
            {
                added = queue.offer(quitItem, TimeConstants.MINUTE, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.interrupted();
            }

            // in case quitItem wasn't added to queue
            if (!added)
            {
                done = true;
                finishQueue = false;
            }
        }

        try
        {
            // wait for thread to end.  If finishing queue,
            // wait indefinitely (e.g., until quitItem is processed).
            // Otherwise, only wait a little bit.
            processor.join(finishQueue ? 0 : WAIT_FOR_THREAD_FINISH);
        }
        catch (InterruptedException e)
        {
            Thread.interrupted();
        }

        // last ditch effort - interrupt if sleeping, then try to join again
        if (processor.isAlive())
        {
            processor.interrupt();
            try
            {
                processor.join(WAIT_FOR_THREAD_FINISH);
            }
            catch (InterruptedException e)
            {
                Thread.interrupted();
            }
        }
    }

    /**
     * main loop - waits for an item on the queue
     */
    private void processQueue()
    {
        while (!done)
        {
            try
            {
                T item = queue.take();
                if (item == quitItem)
                {
                    done = true;
                }
                else
                {
                    if (!done) processQueueItem(item);
                }
            }
            catch (InterruptedException e)
            {
                Thread.interrupted();
            }
        }
    }

    /**
     * Process queue item
     */
    protected abstract void processQueueItem(T item);

    /**
     * Add an item to the queue
     */
    protected void add(T item)
    {
        try
        {
            if (!queue.offer(item, waitLimitWarningMillis, TimeUnit.MILLISECONDS))
            {
                logger.warn("Exceeded " + waitLimitWarningMillis +
                            " milliseconds waiting to add item: " + item);
                logger.debug("All stacktraces at this time: \n" + Utils.getAllStacktraces());
                if (waitLimitErrorMillis <= 0 || !queue.offer(item, waitLimitErrorMillis, TimeUnit.MILLISECONDS))
                {
                    throw new ApplicationError("Exceeded " + waitLimitErrorMillis +
                                               " milliseconds waiting to add item: " + item);
                }
            }
        }
        catch (InterruptedException ie)
        {
            Thread.interrupted();
            throw new ApplicationError("Interrupted trying to add item to queue: " + item, ie);
        }
    }

    /**
     * clear queue
     */
    protected void clear()
    {
        queue.clear();
    }

    /**
     * Thread which pulls items off the queue
     */
    private class QueueThread extends Thread
    {
        private QueueThread()
        {
            super(ManagedQueue.this.getThreadName());
            setDaemon(false);
        }

        @Override
        public void run()
        {
            processQueue();
        }
    }

    /**
     * Number of items on the queue
     */
    public int size()
    {
        return queue.size();
    }

    /**
     * Are we done?
     */
    public boolean isDone()
    {
        return done;
    }
}
