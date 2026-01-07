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
package com.donohoedigital.config;

import static com.donohoedigital.config.ShutdownManager.Type.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Sep 22, 2008
 * Time: 4:30:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShutdownManager implements Thread.UncaughtExceptionHandler
{
    private static final Logger logger = LogManager.getLogger(ShutdownManager.class);

    // the manager
    private static ShutdownManager manager = null;

    // verbosity
    private static boolean verbose = true;

    /**
     * Enum of shutdown types
     */
    public enum Type
    {
        NORMAL, SIGNAL, UNCAUGHTEXCEPTION, ABNORMAL_BY_USER
    }

    // shutdown listeners
    private final List<ShutdownListener> listeners = new ArrayList<ShutdownListener>();

    // shutdown type and reason (default to normal)
    private Type shutdownType = NORMAL;
    private String shutdownDetails = "normal application completion";

    /**
     * Install our own shutdown manager.
     */
    public synchronized static void install()
    {
        if (manager == null) {
            manager = new ShutdownManager();
        }
    }

    /**
     * Set verbose message flag (default is true).  If verbose, prints messages on abnormal shutdown or
     * any shutdown w/listeners defined during: start of shutdown, calling of listeners and finish.
     * If set to false, no info messages are displayed.
     *
     * @param b
     */
    public static void setVerbose(boolean b)
    {
        verbose = b;
    }

    /**
     * Add a shutdown listener.
     * Shutdown listeners are called in the reverse order they were instantiated
     *
     * @param shutdownListener listener which will be called on shutdown
     */
    public synchronized static void addShutdownListener(final ShutdownListener shutdownListener)
    {
        install(); // make sure we have a shutdown manager
        manager.listeners.add(shutdownListener);
    }

    /**
     * For program to exit abnormally (ABNORMAL_BY_USER type)
     *
     * @param details
     */
    public synchronized static void exitAbnormal(String details)
    {
        install(); // make sure we have a shutdown manager
        manager.exitAbnormal(ABNORMAL_BY_USER, details);
    }

    /**
     * Construct and register it to handle shutdowns, signals, uncaught
     * exceptions, and log what's going on
     */
    @SuppressWarnings({"ThisEscapedInObjectConstruction"})
    private ShutdownManager()
    {
        // add shutdown handler to JVM (which calls this class)
        Thread hook = new Thread()
        {
            @Override public void run()
            {
                shuttingDown();
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);

        // set this as default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * UncaughtExceptionHandler: log handler, exit
     */
    public void uncaughtException(Thread t, Throwable e)
    {
        logger.fatal("Uncaught exception in thread " + t.getName(), e);
        exitAbnormal(UNCAUGHTEXCEPTION, "uncaught exception: " + e.getMessage());
    }

    /**
     * exit abnormally
     */
    private void exitAbnormal(Type type, String details)
    {
        shutdownType = type;
        shutdownDetails = details;
        System.exit(1);
    }

    /**
     * Called when shutting down - log messages and invoke any hooks
     */
    private synchronized void shuttingDown()
    {
        // prevent infinite loop if there's a problem in the shutdown hooks
        Thread.setDefaultUncaughtExceptionHandler(null);

        // log message if non-normal or if we ran shutdown hooks
        boolean log = verbose && (shutdownType != NORMAL || !listeners.isEmpty());
        if (log) {
            logger.info("Shutting down (" + shutdownType + "): " + shutdownDetails + " ...");
        }

        // run listeners
        Collections.reverse(listeners);
        for (ShutdownListener listener : listeners) {
            try {
                if (log) logger.info("Calling shutdown listener: " + listener);
                listener.shutdown(shutdownType, shutdownDetails);
            }
            catch (Throwable t) {
                logger.fatal("exception in shutdown listener", t);
            }
        }

        // log message if non-normal or if we ran shutdown hooks
        if (shutdownType != NORMAL || !listeners.isEmpty()) {
            if (log) logger.info("Shutdown complete.");
        }
    }
}