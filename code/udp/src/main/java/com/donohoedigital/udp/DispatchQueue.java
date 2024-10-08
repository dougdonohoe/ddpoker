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
package com.donohoedigital.udp;

import org.apache.logging.log4j.*;
import com.donohoedigital.base.*;

import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 9, 2006
 * Time: 8:08:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class DispatchQueue extends Thread
{
    static Logger logger = LogManager.getLogger(DispatchQueue.class);

    // members
    private LinkedBlockingQueue queue_ = new LinkedBlockingQueue();
    private boolean bDone_ = false;

    // control messages
    private Object QUIT = new Object();

    /**
     * new dispatch queue
     */
    public DispatchQueue()
    {
        super("DispatchQueue");
    }

    /**
     * thread start
     */
    public void run()
    {
        // we use a blocking queue as our work "to do" list.   It blocks until another thread
        // puts something on the queue.  We put several types of Objects.  The QUIT is
        // used to quit (cleanup).  A IncomingQueue on the work queue means to process messages
        // on that queue.
        Object msg;
        while (!bDone_)
        {
            try {
                msg = queue_.take();
                if (msg == QUIT || bDone_) continue;
                else processQueue((QueueInfo) msg);
            }
            catch(InterruptedException e)
            {
                interrupted();
            }
            catch (Throwable t)
            {
                logger.error("DispatchQueue error: " + Utils.formatExceptionText(t));
            }
        }
        logger.info("DispatchQueue Done.");
    }

    /**
     * cleanup
     */
    public void finish()
    {
        //logger.info("Stopping DispatchQueue...");

        // set done flag and send quit message to wakeup queue (if it is waiting on something)
        bDone_ = true;
        quit();

        // wait for thread to complete
        try {
            join(5000);
        } catch (InterruptedException ie) {}
    }

    /**
     * add object to dispatch queue
     */
    private void add(Object o)
    {
        try {
            queue_.put(o);
        }
        catch (InterruptedException e)
        {
            // happens if queue gets full, which won't happen with a linked list
            // if it does happen, just ignore - the queue will be processed again
        }
    }

    /**
     * To quit, add a QUIT message to the queue
     */
    private void quit()
    {
        add(QUIT);
    }

    /**
     * Add IncomingQueue to dispatch queue
     */
    void addQueue(IncomingQueue queue, boolean bLast)
    {
        add(new QueueInfo(queue, bLast));
    }

    /**
     * process the queue
     */
    private void processQueue(QueueInfo info)
    {
        // if true returned, more to handle so add again to end of list
        if (info.queue.dispatch(info.bLast))
        {
            add(info);
        }
    }

    /**
     * incoming queue data
     */
    private class QueueInfo
    {
        boolean bLast;
        IncomingQueue queue;

        QueueInfo(IncomingQueue queue, boolean bLast)
        {
            this.queue = queue;
            this.bLast = bLast;
        }
    }
}
