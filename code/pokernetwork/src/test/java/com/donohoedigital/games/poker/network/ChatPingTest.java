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
package com.donohoedigital.games.poker.network;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.udp.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatPing against real UDPServers standing in for a chat server, a game client, and
 * something that speaks UDP but not chat.  ChatServerPingTest (pokerserver) covers the
 * real ChatServer's reply.
 */
public class ChatPingTest
{
    // unusual ports so we don't collide with a running game or server; bind failover
    // steps down from these if they are taken
    private static final String PINGER_PORT = "11759";
    private static final String PEER_PORT = "11749";
    private static final int TIMEOUT_MILLIS = 2000;

    private enum Mode { PONG, NOT_LOBBY, SILENT }

    private UDPServer pinger;
    private Peer peer;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("udptest", ApplicationType.COMMAND_LINE);
        pinger = new UDPServer(new Timeouts(), true, true, PINGER_PORT);
        pinger.init();
        pinger.start();
    }

    @AfterEach
    public void tearDown()
    {
        if (peer != null) peer.server.shutdown();
        pinger.shutdown();
    }

    @Test
    public void chatServerAnswers()
    {
        peer = new Peer(Mode.PONG);
        assertEquals(ChatPing.Result.OK, ChatPing.check(pinger.manager(), peer.address(), TIMEOUT_MILLIS));
    }

    @Test
    public void gameClientSaysItIsNotALobby()
    {
        peer = new Peer(Mode.NOT_LOBBY);
        assertEquals(ChatPing.Result.NOT_CHAT_SERVER, ChatPing.check(pinger.manager(), peer.address(), TIMEOUT_MILLIS));
    }

    /**
     * An older chat server (or anything else running our UDP stack) completes the handshake
     * but ignores the ping
     */
    @Test
    public void reachableButNoReply()
    {
        peer = new Peer(Mode.SILENT);
        assertEquals(ChatPing.Result.NO_REPLY, ChatPing.check(pinger.manager(), peer.address(), TIMEOUT_MILLIS));
    }

    @Test
    public void nothingListeningIsUnreachable() throws Exception
    {
        int port;
        try (DatagramSocket socket = new DatagramSocket(0, InetAddress.getByName(pinger.getPreferredIP())))
        {
            port = socket.getLocalPort(); // free now that it is closed
        }
        InetSocketAddress nobody = new InetSocketAddress(pinger.getPreferredIP(), port);
        assertEquals(ChatPing.Result.UNREACHABLE, ChatPing.check(pinger.manager(), nobody, TIMEOUT_MILLIS));
    }

    @Test
    public void testLinkIsClosedAfterwards()
    {
        peer = new Peer(Mode.PONG);
        ChatPing.check(pinger.manager(), peer.address(), TIMEOUT_MILLIS);

        waitFor(() -> openLinks().isEmpty(), "test link closed");
    }

    /**
     * Already in the lobby: the test uses that link and leaves it open
     */
    @Test
    public void existingLinkIsUsedAndLeftOpen()
    {
        peer = new Peer(Mode.PONG);
        UDPLink lobby = pinger.manager().getLink(peer.address());
        lobby.connect();
        waitFor(lobby::isEstablished, "lobby link established");

        assertEquals(ChatPing.Result.OK, ChatPing.check(pinger.manager(), peer.address(), TIMEOUT_MILLIS));
        Utils.sleepMillis(500); // a close would be under way by now
        assertFalse(lobby.isDone(), "lobby link still open");
        assertFalse(ChatPing.isPingLink(lobby), "lobby link not renamed");
        assertEquals(List.of(lobby), openLinks());
    }

    private List<UDPLink> openLinks()
    {
        List<UDPLink> links = new ArrayList<>();
        pinger.manager().getLinks(links);
        links.removeIf(UDPLink::isDone);
        return links;
    }

    private static void waitFor(java.util.function.BooleanSupplier condition, String what)
    {
        long end = System.currentTimeMillis() + 10000;
        while (!condition.getAsBoolean())
        {
            if (System.currentTimeMillis() > end) fail("timed out waiting for " + what);
            Utils.sleepMillis(20);
        }
    }

    /**
     * short timeouts all round
     */
    private static class Timeouts implements UDPLinkHandler
    {
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
    }

    /**
     * Other end of the ping, answering as mode says
     */
    private static class Peer extends Timeouts implements UDPManagerMonitor, UDPLinkMonitor
    {
        final UDPServer server;
        private final Mode mode;

        Peer(Mode mode)
        {
            this.mode = mode;
            server = new UDPServer(this, true, true, PEER_PORT);
            server.init();
            server.manager().addMonitor(this);
            server.start();
        }

        InetSocketAddress address()
        {
            return new InetSocketAddress(server.getPreferredIP(), server.getPreferredPort());
        }

        public void monitorEvent(UDPManagerEvent event)
        {
            if (event.getType() == UDPManagerEvent.Type.CREATED) event.getLink().addMonitor(this);
        }

        public void monitorEvent(UDPLinkEvent event)
        {
            UDPData data = event.getData();
            if (event.getType() != UDPLinkEvent.Type.RECEIVED || data.getType() != UDPData.Type.MESSAGE ||
                data.getUserType() != PokerConstants.USERTYPE_PING) return;

            UDPLink link = event.getLink();
            switch (mode)
            {
                case PONG:
                    link.queue(ChatPing.pong().getData(), PokerConstants.USERTYPE_PONG);
                    link.send();
                    break;

                case NOT_LOBBY: // what PokerMain does
                    OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
                    omsg.setChat("not a lobby");
                    omsg.setChatType(PokerConstants.CHAT_ADMIN_ERROR);
                    link.queue(new PokerUDPTransporter(omsg.getData()).getData(), PokerConstants.USERTYPE_CHAT);
                    link.send();
                    link.close();
                    break;

                case SILENT:
                    break;
            }
        }
    }
}
