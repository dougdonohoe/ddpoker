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
package com.donohoedigital.udp;

import com.donohoedigital.base.Utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * One end of a UDP link for tests - runs a UDPServer and records what happens on its links
 */
class TestPeer implements UDPLinkHandler, UDPManagerMonitor, UDPLinkMonitor
{
    static final long WAIT_MILLIS = 30000;

    final List<UDPLinkEvent.Type> events = new CopyOnWriteArrayList<>();
    final List<String> received = new CopyOnWriteArrayList<>();
    volatile UDPLink link; // most recently created link on this side
    UDPServer server;

    private int timeout = 5000;
    private int possibleTimeoutStart = 2000;
    private int possibleTimeoutInterval = 1000;

    /**
     * Shorter timeouts, for tests that wait for them
     */
    @SuppressWarnings("SameParameterValue")
    TestPeer timeouts(int timeoutMillis, int possibleStartMillis, int possibleIntervalMillis)
    {
        timeout = timeoutMillis;
        possibleTimeoutStart = possibleStartMillis;
        possibleTimeoutInterval = possibleIntervalMillis;
        return this;
    }

    /**
     * Bind a server on port (falls over to lower ports if taken) without starting its threads
     */
    UDPServer bind(String port)
    {
        server = new UDPServer(this, true, true, port);
        server.init();
        server.manager().addMonitor(this);
        return server;
    }

    /**
     * Bind and start
     */
    UDPServer start(String port)
    {
        bind(port).start();
        return server;
    }

    /**
     * Connect to other peer and wait for MTU discovery to finish
     */
    UDPLink connect(TestPeer other)
    {
        UDPLink to = server.manager().getLink(other.server.getPreferredIP(), other.server.getPreferredPort());
        to.connect();
        waitFor(() -> has(UDPLinkEvent.Type.MTU_TEST_FINISHED), "MTU discovery");
        return to;
    }

    void shutdown()
    {
        if (server != null) server.shutdown();
        server = null;
    }

    boolean has(UDPLinkEvent.Type type)
    {
        return events.contains(type);
    }

    static void waitFor(BooleanSupplier condition, String what)
    {
        long end = System.currentTimeMillis() + WAIT_MILLIS;
        while (!condition.getAsBoolean())
        {
            if (System.currentTimeMillis() > end) fail("timed out waiting for " + what);
            Utils.sleepMillis(20);
        }
    }

    public int getTimeout(UDPLink link)
    {
        return timeout;
    }

    public int getPossibleTimeoutNotificationInterval(UDPLink link)
    {
        return possibleTimeoutInterval;
    }

    public int getPossibleTimeoutNotificationStart(UDPLink link)
    {
        return possibleTimeoutStart;
    }

    public void monitorEvent(UDPManagerEvent event)
    {
        if (event.getType() == UDPManagerEvent.Type.CREATED)
        {
            link = event.getLink();
            link.addMonitor(this);
        }
    }

    public void monitorEvent(UDPLinkEvent event)
    {
        events.add(event.getType());
        if (event.getType() == UDPLinkEvent.Type.RECEIVED)
        {
            UDPData data = event.getData();
            if (data.getType() == UDPData.Type.MESSAGE)
            {
                received.add(Utils.decode(data.getData(), data.getOffset(), data.getLength()));
            }
        }
    }
}
