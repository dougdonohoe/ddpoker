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

package com.donohoedigital.server;

import org.apache.logging.log4j.*;
import com.donohoedigital.base.*;

public abstract class WorkerThread extends Thread
{
    static Logger logger = LogManager.getLogger(WorkerThread.class);
    
    // initialization stuff
    protected WorkerPool pool_;
    private volatile boolean bDone_ = false;

    /**
     * Empty default constructor for use by newInstance()
     */
    public WorkerThread ()
    {
    }
    
    /**
     * Called from ThreadPool
     */
    public void init(WorkerPool pool)
    {
        pool_ = pool;
    }

    /**
     * process data on channel - wakes up wait() in run() below
     */
    protected synchronized void wakeup()
    {
        // awaken the thread
        notify();		
    }
    
    // loop forever waiting for work to do
    public synchronized void run()
    {
        if (GameServer.DEBUG_POOL) logger.info(getName() + " is ready");

        while (!bDone_) {
            try {
                // sleep and release object lock
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                // clear interrupt status
                Thread.interrupted();
            }

            // done?
            if (bDone_) continue;

            // read post and process data
            try {                    
                process();
            }
            // handle exceptions
            catch (Throwable t)
            {
                logger.error("WorkerThread exception: " + Utils.formatExceptionText(t));
            }
            finally
            {
                // done, ready for more, return to pool
                if (GameServer.DEBUG_ONLINE) logger.debug(this.getName() + " DONE - returning to pool");
                pool_.returnWorker (this);
            }
        }

        if (GameServer.DEBUG_POOL) logger.info(getName() + " done.");
    }

    /**
     * shutdown
     */
    public synchronized void shutdown()
    {
        bDone_ = true;
        notify();
    }

    /**
     * processing logic
     */
    public abstract void process();
}
