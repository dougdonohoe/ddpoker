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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IncomingQueue orders received data by id and dispatches complete messages in sequence,
 * reassembling multipart messages.  Also covers link monitor notification.  Uses a link
 * on a bound (not started) server and calls dispatch directly, so everything is synchronous.
 */
public class IncomingQueueTest
{
    private TestPeer peer;
    private UDPLink link;
    private IncomingQueue queue;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("udptest", ApplicationType.COMMAND_LINE);
        peer = new TestPeer();
        peer.bind("11759");
        link = peer.server.manager().getLink("127.0.0.1", 11750);
        queue = new IncomingQueue(link);
    }

    @AfterEach
    public void tearDown()
    {
        peer.shutdown();
    }

    @Test
    public void outOfOrderHeldUntilGapFilled()
    {
        assertTrue(queue.addMessage(msg(3, "three")));
        assertTrue(queue.addMessage(msg(2, "two")));
        assertFalse(queue.dispatch(false));
        assertEquals(List.of(), peer.received);

        assertTrue(queue.addMessage(msg(1, "one")));
        assertFalse(queue.dispatch(false));
        assertEquals(List.of("one", "two", "three"), peer.received);
    }

    @Test
    public void duplicatesRejected()
    {
        assertTrue(queue.addMessage(msg(1, "one")));
        assertFalse(queue.addMessage(msg(1, "one again")), "duplicate while queued");
        queue.dispatch(false);
        assertFalse(queue.addMessage(msg(1, "one again")), "duplicate after dispatch");
        queue.dispatch(false);
        assertEquals(List.of("one"), peer.received);
    }

    @Test
    public void multiPartReassembledOnceAllPartsArrive()
    {
        String text = "abcdefghijklmnopqrstuvwxyz0123456789";
        List<UDPData> parts = parts(1, text, 10); // 4 parts, ids 1-4
        queue.addMessage(msg(5, "after"));
        queue.addMessage(parts.get(3));
        queue.addMessage(parts.get(0));
        queue.addMessage(parts.get(2));
        queue.dispatch(false);
        assertEquals(List.of(), peer.received, "part 2 missing");

        queue.addMessage(parts.get(1));
        queue.dispatch(false);
        assertEquals(List.of(text, "after"), peer.received);
    }

    @Test
    public void dispatchLimitedPerCall()
    {
        for (int id = 1; id <= 25; id++) queue.addMessage(msg(id, "m" + id));

        assertTrue(queue.dispatch(false), "more to dispatch");
        assertEquals(10, peer.received.size());
        assertTrue(queue.dispatch(false), "more to dispatch");
        assertEquals(20, peer.received.size());
        assertFalse(queue.dispatch(false), "all dispatched");
        assertEquals(25, peer.received.size());
    }

    /**
     * The last dispatch (after a GOODBYE) must deliver everything that's ready before closing
     */
    @Test
    public void lastDispatchDeliversAllThenCloses()
    {
        for (int id = 1; id <= 15; id++) queue.addMessage(msg(id, "m" + id));

        assertFalse(queue.dispatch(true));
        assertEquals(15, peer.received.size());
        assertTrue(peer.has(UDPLinkEvent.Type.CLOSED));
    }

    @Test
    public void throwingMonitorDoesNotStopOthers()
    {
        List<UDPLinkEvent.Type> seen = new CopyOnWriteArrayList<>();
        link.addMonitor(_ -> { throw new RuntimeException("expected test exception"); });
        link.addMonitor(event -> seen.add(event.getType()));

        link.notifyHandlers(msg(1, "one"));
        assertEquals(List.of(UDPLinkEvent.Type.RECEIVED), seen);
        assertEquals(List.of("one"), peer.received);
    }

    @Test
    public void monitorsCanChangeMonitorsWhileHandling()
    {
        List<String> seen = new CopyOnWriteArrayList<>();
        UDPLinkMonitor added = _ -> seen.add("added");
        UDPLinkMonitor once = new UDPLinkMonitor()
        {
            public void monitorEvent(UDPLinkEvent event)
            {
                seen.add("once");
                link.removeMonitor(this);
                link.addMonitor(added);
            }
        };
        link.addMonitor(once);
        link.addMonitor(once); // duplicate add ignored

        link.notifyHandlers(msg(1, "one"));
        assertEquals(List.of("once"), seen, "changes apply to the next event");

        link.notifyHandlers(msg(2, "two"));
        assertEquals(List.of("once", "added"), seen);
    }

    private static UDPData msg(int id, String text)
    {
        byte[] bytes = Utils.encode(text);
        return new UDPData(UDPData.Type.MESSAGE, id, (short) 1, (short) 1, bytes, 0, bytes.length, UDPData.USER_TYPE_UNSPECIFIED);
    }

    @SuppressWarnings("SameParameterValue")
    private static List<UDPData> parts(int firstID, String text, int partSize)
    {
        byte[] bytes = Utils.encode(text);
        short nParts = (short) ((bytes.length + partSize - 1) / partSize);
        List<UDPData> parts = new ArrayList<>();
        for (short i = 0; i < nParts; i++)
        {
            int offset = i * partSize;
            parts.add(new UDPData(UDPData.Type.MESSAGE, firstID + i, (short) (i + 1), nParts, bytes, offset,
                                  Math.min(partSize, bytes.length - offset), UDPData.USER_TYPE_UNSPECIFIED));
        }
        return parts;
    }
}
