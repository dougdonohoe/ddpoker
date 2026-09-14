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
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two UDPServers in one JVM talk over a real UDPLink: connect (hello + MTU discovery),
 * deliver messages in order - including one split into multiple parts - and close
 * cleanly on both ends.  See UDPLinkTester for a manual version that runs across machines.
 */
public class UDPLinkTest
{
    // unusual ports so we don't collide with a running game (11889); bind failover
    // steps down from these if they are taken
    private static final String RECEIVER_PORT = "11779";
    private static final String SENDER_PORT = "11769";

    private static final int NUM_MESSAGES = 50;
    private static final long TIMEOUT_SECONDS = 20;

    private UDPServer receiver;
    private UDPServer sender;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("udptest", ApplicationType.COMMAND_LINE);
    }

    @AfterEach
    public void tearDown()
    {
        if (sender != null) sender.shutdown();
        if (receiver != null) receiver.shutdown();
    }

    @Test
    public void messagesArriveInOrderAndLinkCloses() throws InterruptedException
    {
        Peer rx = new Peer();
        Peer tx = new Peer();
        receiver = rx.start(RECEIVER_PORT);
        sender = tx.start(SENDER_PORT);

        // connect: hello, then MTU discovery
        UDPLink link = sender.manager().getLink(receiver.getPreferredIP(), receiver.getPreferredPort());
        link.connect();
        await(tx.mtuDone, "MTU discovery");
        assertTrue(link.isEstablished(), "link established");

        // mostly small messages, with one in the middle big enough to need several parts;
        // every 5th (including the big one) is sliced out of a larger buffer
        List<String> sent = new ArrayList<>();
        for (int i = 1; i <= NUM_MESSAGES; i++)
        {
            String msg = "message " + i;
            if (i == NUM_MESSAGES / 2) msg = big(msg, link.getMaxDataSize() * 3 + 17);
            sent.add(msg);
            if (i % 5 == 0) queueWithOffset(link, msg);
            else link.queue(Utils.encode(msg));
        }
        rx.expect(sent.size());
        await(rx.allReceived, "all messages received");
        assertEquals(sent, rx.received);

        // close from sender - goodbye reaches receiver and both sides finish
        link.close();
        await(tx.closed, "sender link closed");
        await(rx.closed, "receiver link closed");
    }

    /**
     * Queue msg from the middle of a padded buffer, with an offset longer than short
     * messages, so bad slicing either fails to queue or shows up as padding in what's received
     */
    private static void queueWithOffset(UDPLink link, String msg)
    {
        byte[] bytes = Utils.encode(msg);
        int offset = 300;
        byte[] buffer = new byte[offset + bytes.length + 50];
        Arrays.fill(buffer, (byte) '#');
        System.arraycopy(bytes, 0, buffer, offset, bytes.length);
        link.queue(buffer, offset, bytes.length);
    }

    private static String big(String prefix, int length)
    {
        StringBuilder sb = new StringBuilder(prefix).append(' ');
        while (sb.length() < length - 1) sb.append((char) ('a' + (sb.length() % 26)));
        return sb.append('~').toString();
    }

    private static void await(CountDownLatch latch, String what) throws InterruptedException
    {
        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "timed out waiting for " + what);
    }

    /**
     * One end of the link - starts a server and records what happens on its links
     */
    private static class Peer implements UDPLinkHandler, UDPManagerMonitor, UDPLinkMonitor
    {
        final CountDownLatch mtuDone = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        final List<String> received = new CopyOnWriteArrayList<>();
        volatile int expected = Integer.MAX_VALUE;
        final CountDownLatch allReceived = new CountDownLatch(1);

        UDPServer start(String port)
        {
            UDPServer server = new UDPServer(this, true, true, port);
            server.init();
            server.manager().addMonitor(this);
            server.start();
            return server;
        }

        void expect(int count)
        {
            expected = count;
            checkReceived();
        }

        private void checkReceived()
        {
            if (received.size() >= expected) allReceived.countDown();
        }

        public int getTimeout(UDPLink link)
        {
            return 5000;
        }

        public int getPossibleTimeoutNotificationInterval(UDPLink link)
        {
            return 1000;
        }

        public int getPossibleTimeoutNotificationStart(UDPLink link)
        {
            return 2000;
        }

        public void monitorEvent(UDPManagerEvent event)
        {
            if (event.getType() == UDPManagerEvent.Type.CREATED) event.getLink().addMonitor(this);
        }

        public void monitorEvent(UDPLinkEvent event)
        {
            switch (event.getType())
            {
                case MTU_TEST_FINISHED:
                    mtuDone.countDown();
                    break;
                case CLOSED:
                    closed.countDown();
                    break;
                case RECEIVED:
                    UDPData data = event.getData();
                    if (data.getType() == UDPData.Type.MESSAGE)
                    {
                        received.add(Utils.decode(data.getData(), data.getOffset(), data.getLength()));
                        checkReceived();
                    }
                    break;
            }
        }
    }
}
