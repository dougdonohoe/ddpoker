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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.donohoedigital.udp.TestPeer.waitFor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Two UDPServers in one JVM talk over real UDPLinks on this machine.  Packet loss and
 * path MTU limits are simulated with UDPServer's drop filter.  See UDPLinkTester for a
 * manual version that runs across machines.
 */
public class UDPLinkTest
{
    // unusual ports so we don't collide with a running game (11889); bind failover
    // steps down from these if they are taken
    private static final String RECEIVER_PORT = "11779";
    private static final String SENDER_PORT = "11769";

    private TestPeer rx;
    private TestPeer tx;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("udptest", ApplicationType.COMMAND_LINE);
        rx = new TestPeer();
        tx = new TestPeer();
    }

    @AfterEach
    public void tearDown()
    {
        tx.shutdown();
        rx.shutdown();
    }

    @Test
    public void messagesArriveInOrderAndLinkCloses()
    {
        rx.start(RECEIVER_PORT);
        tx.start(SENDER_PORT);
        UDPLink link = tx.connect(rx);
        assertTrue(link.isEstablished(), "link established");

        // mostly small messages, with one in the middle big enough to need several parts;
        // every 5th (including the big one) is sliced out of a larger buffer
        List<String> sent = new ArrayList<>();
        for (int i = 1; i <= 50; i++)
        {
            String msg = "message " + i;
            if (i == 25) msg = big(msg, link.getMaxDataSize() * 3 + 17);
            sent.add(msg);
            if (i % 5 == 0) queueWithOffset(link, msg);
            else link.queue(Utils.encode(msg));
        }
        waitFor(() -> rx.received.size() == sent.size(), "all messages received");
        assertEquals(sent, rx.received);

        // close from sender - goodbye reaches receiver and both sides finish
        link.close();
        waitFor(() -> tx.has(UDPLinkEvent.Type.CLOSED), "sender link closed");
        waitFor(() -> rx.has(UDPLinkEvent.Type.CLOSED), "receiver link closed");
    }

    /**
     * Production code queues a reply and closes right away (e.g., ChatServer.sendError())
     */
    @Test
    public void messagesQueuedJustBeforeCloseAreDelivered()
    {
        rx.start(RECEIVER_PORT);
        tx.start(SENDER_PORT);
        UDPLink link = tx.connect(rx);

        List<String> sent = List.of("one", "two", "three");
        for (String msg : sent) link.queue(Utils.encode(msg));
        link.close();

        waitFor(() -> rx.has(UDPLinkEvent.Type.CLOSED), "receiver link closed");
        assertEquals(sent, rx.received);
    }

    @Test
    public void bothDirectionsSurvivePacketLoss()
    {
        rx.start(RECEIVER_PORT).setDropFilter(lossy(15, 1));
        tx.start(SENDER_PORT).setDropFilter(lossy(15, 2));
        UDPLink txLink = tx.connect(rx);
        waitFor(() -> rx.link != null && rx.link.isEstablished(), "receiver link established");
        UDPLink rxLink = rx.link;

        List<String> toRx = new ArrayList<>();
        List<String> toTx = new ArrayList<>();
        for (int i = 1; i <= 30; i++)
        {
            toRx.add("to receiver " + i);
            toTx.add("to sender " + i);
            txLink.queue(Utils.encode(toRx.getLast()));
            rxLink.queue(Utils.encode(toTx.getLast()));
        }
        String big = big("big", txLink.getMaxDataSize() * 4);
        toRx.add(big);
        txLink.queue(Utils.encode(big));

        waitFor(() -> rx.received.size() >= toRx.size() && tx.received.size() >= toTx.size(), "all messages received");
        assertEquals(toRx, rx.received, "in order, no duplicates");
        assertEquals(toTx, tx.received, "in order, no duplicates");
        assertTrue(txLink.getStats().getDataResend() + rxLink.getStats().getDataResend() > 0, "loss caused resends");
    }

    /**
     * A GOODBYE can arrive before earlier messages that were lost - those must still be delivered
     */
    @Test
    public void closeWithPacketLossDeliversEverything()
    {
        rx.start(RECEIVER_PORT);
        tx.start(SENDER_PORT);
        UDPLink link = tx.connect(rx);

        // lose the first send of every message so the goodbye arrives before any of them
        tx.server.setDropFilter(msg -> {
            for (int i = 0; i < msg.getNumData(); i++)
            {
                UDPData data = msg.getData(i);
                if (data.getType() == UDPData.Type.MESSAGE && data.getSendCount() == 0) return true;
            }
            return false;
        });

        List<String> sent = new ArrayList<>();
        for (int i = 1; i <= 5; i++)
        {
            sent.add("message " + i);
            link.queue(Utils.encode(sent.getLast()));
        }
        link.send();
        link.close();

        waitFor(() -> rx.has(UDPLinkEvent.Type.CLOSED), "receiver link closed");
        waitFor(() -> tx.has(UDPLinkEvent.Type.CLOSED), "sender link closed");
        assertEquals(sent, rx.received);
    }

    @Test
    public void mtuDiscoveryHonorsPathLimit()
    {
        rx.start(RECEIVER_PORT);
        tx.start(SENDER_PORT).setDropFilter(msg -> msg.getPacketLength() > 1000);
        UDPLink link = tx.connect(rx);

        // probes are 576, 704, 832, 960, 1088, ... bytes
        assertEquals(960, link.getMTU());

        String big = big("big", 5000);
        link.queue(Utils.encode(big));
        waitFor(() -> rx.received.size() == 1, "big message received");
        assertEquals(big, rx.received.getFirst());
    }

    @Test
    public void peerDisappearingTimesOut()
    {
        rx.start(RECEIVER_PORT);
        tx.timeouts(1500, 300, 300).start(SENDER_PORT);
        tx.connect(rx);

        rx.shutdown(); // no goodbye

        waitFor(() -> tx.has(UDPLinkEvent.Type.CLOSED), "sender link closed");
        List<UDPLinkEvent.Type> events = tx.events;
        int possible = events.indexOf(UDPLinkEvent.Type.POSSIBLE_TIMEOUT);
        int timeout = events.indexOf(UDPLinkEvent.Type.TIMEOUT);
        assertTrue(possible >= 0, "possible timeout notified: " + events);
        assertTrue(timeout > possible, "timeout after possible timeout: " + events);
        assertTrue(events.indexOf(UDPLinkEvent.Type.CLOSED) > timeout, "closed after timeout: " + events);
    }

    @Test
    public void peerRestartStartsNewSession()
    {
        rx.start(RECEIVER_PORT);
        tx.start(SENDER_PORT);
        tx.connect(rx).queue(Utils.encode("before restart"));
        waitFor(() -> rx.received.size() == 1, "first message received");

        // sender restarts (no goodbye) on same port and reconnects
        tx.shutdown();
        TestPeer tx2 = new TestPeer();
        tx = tx2; // so tearDown shuts it down
        tx2.start(SENDER_PORT);
        tx2.connect(rx).queue(Utils.encode("after restart"));

        waitFor(() -> rx.received.size() == 2, "message after restart received");
        assertEquals(List.of("before restart", "after restart"), rx.received);
        assertTrue(rx.has(UDPLinkEvent.Type.SESSION_CHANGED), "receiver saw new session: " + rx.events);
    }

    @Test
    public void junkPacketsIgnored() throws Exception
    {
        rx.start(RECEIVER_PORT);
        tx.start(SENDER_PORT);

        // random bytes of various sizes, including one that is header-sized
        InetSocketAddress to = new InetSocketAddress(rx.server.getPreferredIP(), rx.server.getPreferredPort());
        Random random = new Random(42);
        try (DatagramSocket socket = new DatagramSocket())
        {
            for (int size : new int[] {1, 20, UDPMessage.HEADER_SIZE, 500, UDPLink.MAX_PAYLOAD_SIZE + 100})
            {
                byte[] junk = new byte[size];
                random.nextBytes(junk);
                socket.send(new DatagramPacket(junk, size, to));
            }
        }

        UDPLink link = tx.connect(rx);
        link.queue(Utils.encode("still works"));
        waitFor(() -> rx.received.size() == 1, "message received");
        assertEquals(List.of("still works"), rx.received);

        List<UDPLink> links = new ArrayList<>();
        rx.server.manager().getLinks(links);
        assertEquals(1, links.size(), "junk created no links");
    }

    /**
     * Drop percent of outgoing packets, repeatably
     */
    @SuppressWarnings("SameParameterValue")
    private static java.util.function.Predicate<UDPMessage> lossy(int percent, long seed)
    {
        Random random = new Random(seed);
        return _ -> random.nextInt(100) < percent;
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
}
