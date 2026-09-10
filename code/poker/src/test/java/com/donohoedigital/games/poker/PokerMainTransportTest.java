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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.network.PokerConnection;
import com.donohoedigital.udp.UDPID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * PokerMain.getPokerConnectionServer(PokerConnection) names the server a message arrived
 * on, so a reply can be built with the right transport.
 * <p/>
 * It is deliberately distinct from the boolean overload, which decides which transport the
 * game itself uses and creates or shuts down real servers as a side effect.  Calling that
 * one to answer "what did this arrive on?" would tear down the game's server.
 */
public class PokerMainTransportTest
{
    private TestPokerMain main_;
    private SocketChannel channel_;

    @BeforeEach
    public void setUp() throws IOException
    {
        main_ = AbstractPokerTest.engine();
        channel_ = SocketChannel.open(); // never connected; only its identity is used
    }

    @Test
    public void udpConnectionRoutesToUdpServer()
    {
        PokerConnection conn = new PokerConnection(new UDPID("00000000-0000-0000-0000-000000000001"));
        assertSame(main_.getFakeUDP(), main_.getPokerConnectionServer(conn));
    }

    @Test
    public void tcpConnectionRoutesToTcpServer()
    {
        assertSame(main_.getFakeTCP(), main_.getPokerConnectionServer(new PokerConnection(channel_)));
    }

    /**
     * Nothing to route by, so fall back to whatever the game is using.
     */
    @Test
    public void noConnectionFallsBackToGameTransport()
    {
        main_.setGameTransport(main_.getFakeUDP());
        assertSame(main_.getFakeUDP(), main_.getPokerConnectionServer(null));

        main_.setGameTransport(main_.getFakeTCP());
        assertSame(main_.getFakeTCP(), main_.getPokerConnectionServer(null));
    }

    /**
     * A game with no server yet has nothing to fall back to.  PokerMain.messageReceived()
     * relies on this to drop the message rather than build an unsendable reply.
     */
    @Test
    public void noGameTransportGivesNull()
    {
        main_.setGameTransport(null);
        assertNull(main_.getPokerConnectionServer(null));
    }
}
