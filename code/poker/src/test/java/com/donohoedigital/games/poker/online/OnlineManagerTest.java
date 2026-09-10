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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.comms.DDMessageTransporter;
import com.donohoedigital.games.poker.AbstractPokerTest;
import com.donohoedigital.games.poker.FakePokerConnectionServer;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.TestPokerMain;
import com.donohoedigital.games.poker.network.OnlineMessage;
import com.donohoedigital.games.poker.network.PokerConnection;
import com.donohoedigital.games.poker.network.PokerUDPTransporter;
import com.donohoedigital.p2p.Peer2PeerMessage;
import com.donohoedigital.udp.UDPID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A reply must be built with the transport the message arrived on, not the transport the
 * game happens to be using.
 * <p/>
 * These are not the same thing.  A hosted game is TCP unless testing turns UDP on, while
 * the chat lobby is always UDP, so a client can be handed a UDP message while its game
 * runs over TCP.  Before this was fixed, every reply came from p2p_ - the game's transport
 * - and PokerMain.monitorEvent() then cast it to PokerUDPTransporter:
 * <pre>
 * ClassCastException: com.donohoedigital.p2p.Peer2PeerMessage cannot be cast to
 *                     com.donohoedigital.games.poker.network.PokerUDPTransporter
 * </pre>
 * It was not fatal - UDPLink.fireEvent() catches it - but the reply was dropped and the
 * link never closed, so the sender got silence and waited out a timeout.
 */
public class OnlineManagerTest extends AbstractPokerTest
{
    private static final String TCP_GAME_ID = "n-1"; // "n-" is the TCP prefix
    private static final String UDP_GAME_ID = "u-1"; // "u-" is the UDP prefix
    private static final String PASSWORD = "secret";

    private TestPokerMain main_;
    private OnlineManager mgr_;
    private SocketChannel channel_;

    @BeforeEach
    public void setUpOnline() throws IOException
    {
        main_ = engine();
        game_.setOnlineGameID(TCP_GAME_ID);
        game_.setOnlinePassword(PASSWORD);
        channel_ = SocketChannel.open(); // never connected; only its identity is used
    }

    @AfterEach
    public void tearDownOnline() throws IOException
    {
        if (mgr_ != null) mgr_.finish(); // shuts down the OnlineManagerQueue threads
        mgr_ = null;
        channel_.close();
    }

    /**
     * Build a manager for a game on the given transport.  The local player is deliberately
     * not the host: host construction reaches for PokerPrefsPlayerList, which reads
     * java.util.prefs and caches statically across tests.
     */
    private OnlineManager manager(FakePokerConnectionServer gameTransport)
    {
        main_.setGameTransport(gameTransport);
        PokerPlayer local = new PokerPlayer(1, "local", true);
        game_.addPlayer(local);
        mgr_ = new OnlineManager(game_, main_, gameTransport, local);
        return mgr_;
    }

    private PokerConnection udpConnection()
    {
        return new PokerConnection(new UDPID("00000000-0000-0000-0000-000000000001"));
    }

    private PokerConnection tcpConnection()
    {
        return new PokerConnection(channel_);
    }

    /**
     * A message that fails validation, which is what a stray lobby message looks like:
     * it carries no game id and no password, so it can never match a running game.
     */
    private DDMessageTransporter strayMessage(FakePokerConnectionServer from)
    {
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_HELLO);
        return from.newMessage(omsg.getData());
    }

    //
    // the regression
    //

    /**
     * The bug, exactly: a UDP message reaches a TCP-hosted game, validation rejects it,
     * and the rejection has to go back out over UDP.  Against the old code this returned a
     * Peer2PeerMessage and monitorEvent() threw ClassCastException.
     */
    @Test
    public void udpMessageToTcpGameGetsUdpReply()
    {
        OnlineManager mgr = manager(main_.getFakeTCP());

        DDMessageTransporter reply = mgr.handleMessage(strayMessage(main_.getFakeUDP()), udpConnection());

        assertNotNull(reply, "a rejected message must still produce a reply");
        assertInstanceOf(PokerUDPTransporter.class, reply,
                         "reply to a UDP message must be UDP even though the game is TCP");
    }

    /**
     * The ordinary case is unchanged - a TCP message to a TCP game replies over TCP.
     */
    @Test
    public void tcpMessageToTcpGameGetsTcpReply()
    {
        OnlineManager mgr = manager(main_.getFakeTCP());

        DDMessageTransporter reply = mgr.handleMessage(strayMessage(main_.getFakeTCP()), tcpConnection());

        assertNotNull(reply);
        assertInstanceOf(Peer2PeerMessage.class, reply);
    }

    /**
     * And a UDP-hosted game still replies over UDP.
     */
    @Test
    public void udpMessageToUdpGameGetsUdpReply()
    {
        game_.setOnlineGameID(UDP_GAME_ID);
        OnlineManager mgr = manager(main_.getFakeUDP());

        DDMessageTransporter reply = mgr.handleMessage(strayMessage(main_.getFakeUDP()), udpConnection());

        assertNotNull(reply);
        assertInstanceOf(PokerUDPTransporter.class, reply);
    }

    /**
     * The same rule on the other reply path - an unrecognized category, which is answered
     * from processMessage()'s default branch rather than from validate().
     */
    @Test
    public void unhandledCategoryRepliesOnArrivingTransport()
    {
        OnlineManager mgr = manager(main_.getFakeTCP());

        OnlineMessage omsg = new OnlineMessage(-999); // no such category
        omsg.setGameID(TCP_GAME_ID); // valid, so it reaches the switch
        omsg.setPassword(PASSWORD);
        DDMessageTransporter msg = main_.getFakeUDP().newMessage(omsg.getData());

        DDMessageTransporter reply = mgr.handleMessage(msg, udpConnection());

        assertNotNull(reply);
        assertInstanceOf(PokerUDPTransporter.class, reply);
    }

    /**
     * A message we built ourselves carries no connection, so there is nothing to route by.
     * That falls back to the game's own transport.
     */
    @Test
    public void messageWithNoConnectionFallsBackToGameTransport()
    {
        OnlineManager mgr = manager(main_.getFakeTCP());

        DDMessageTransporter reply = mgr.handleMessage(strayMessage(main_.getFakeTCP()), null);

        assertNotNull(reply);
        assertInstanceOf(Peer2PeerMessage.class, reply, "no connection means use the game transport");
    }

    /**
     * The rejection carries the diagnostic string that says why it failed - it is the only
     * record of a mismatch once the message is gone.
     */
    @Test
    public void validationFailureCarriesDiagnosticData()
    {
        OnlineManager mgr = manager(main_.getFakeTCP());

        DDMessageTransporter reply = mgr.handleMessage(strayMessage(main_.getFakeUDP()), udpConnection());

        String data = reply.getMessage().getString("x-validation-data", null);
        assertNotNull(data, "rejection should record what did not match");
        assertTrue(data.contains("gid=" + TCP_GAME_ID), "should name the game id: " + data);
    }

    /**
     * A valid message on a category with nothing to say produces no reply at all - so the
     * transport rule above is about replies, not about every message.
     */
    @Test
    public void aliveMessageProducesNoReply()
    {
        OnlineManager mgr = manager(main_.getFakeTCP());

        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_ALIVE);
        omsg.setGameID(TCP_GAME_ID);
        omsg.setPassword(PASSWORD);

        assertNull(mgr.handleMessage(main_.getFakeUDP().newMessage(omsg.getData()), udpConnection()));
    }
}
