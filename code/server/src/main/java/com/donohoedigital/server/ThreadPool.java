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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
public class ThreadPool
{
    static Logger logger = Logger.getLogger(ThreadPool.class);
    
    private GameServer server_;
    private final List<SocketThread> idle_ = new LinkedList<SocketThread>();
    private final List<SocketThread> workers_ = new ArrayList<SocketThread>();
    private Class<?> socketClass_;
    private BaseServlet servlet_;

    public ThreadPool(GameServer server, int poolSize, BaseServlet servlet, String sSocketClass)
    {
        // store server
        server_ = server;
        servlet_ = servlet;

        // get SocketThread class
        //logger.info("Using SocketThread class: " + sSocketClass);
        socketClass_ = ConfigUtils.getClass(sSocketClass, true);

        // fill up the pool with worker threads
        addWorkers(poolSize, false);

        // let all threads get started
        Utils.sleepMillis(250);
        if (GameServer.DEBUG_POOL) logger.debug("Thread pool ready");
    }

    /**
     * Add nWorkers to pool
     */
    public void addWorkers(int nWorkers)
    {
        addWorkers(nWorkers, true);
    }

    /**
     * add workers to pool, print debug info
     */
    private void addWorkers(int nWorkers, boolean bInfo)
    {
        if (bInfo)
        {
            logger.info("Growing thread pool by " + nWorkers + " to " + (nWorkers + workers_.size()) + " workers.");
        }
        SocketThread thread;
        for (int i = 0; i < nWorkers; i++)
        {
            // create new thread
            try {
                thread = (SocketThread) socketClass_.newInstance();
            }
            catch (Exception e) {
                throw new ApplicationError(e);
            }

            // initialize it
            thread.init(this, servlet_);

            // set thread name for debugging, start it
            thread.setName("SocketThread-" + (workers_.size() + 1));
            thread.start();

            workers_.add(thread);
            returnWorker(thread);
        }
    }

    /**
     * Get pool size
     */
    public int size()
    {
        return workers_.size();
    }

    /**
     * Get server
     */
    public GameServer getServer() {
        return server_;
    }

    /**
     * shutdown
     */
    public void shutdown()
    {
        for (SocketThread worker : workers_)
        {
            worker.shutdown();
        }
        workers_.clear();
        idle_.clear();
    }

    /**
     * Find an idle worker thread, if any.  Could return null.
     */
    public SocketThread getWorker()
    {
        SocketThread worker = null;

        synchronized (idle_) {
            if (idle_.size() > 0) {
                worker = idle_.remove(0);
            }
        }

        return worker;
    }

    /**
     * Called by the socket thread to return itself to the idle pool.
     */
    public void returnWorker(SocketThread worker)
    {
        synchronized (idle_) {
            idle_.add(worker);
        }
    }
    
    /**
     * Get number of idle workers
     */
    public int getNumIdleWorkers()
    {
        synchronized (idle_) {
            return idle_.size();
        }
    }
}
