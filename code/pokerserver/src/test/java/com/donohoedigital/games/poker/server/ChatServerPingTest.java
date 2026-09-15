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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.network.ChatPing;
import com.donohoedigital.udp.UDPLink;
import com.donohoedigital.udp.UDPLinkHandler;
import com.donohoedigital.udp.UDPServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The real ChatServer answers the Options -> Online "Test Chat" ping, with no login and no
 * database (the ping never reaches the profile or ban services).  ChatPingTest
 * (pokernetwork) covers the other results.
 */
public class ChatServerPingTest
{
    // unusual ports so we don't collide with a running game or server
    private static final String CHAT_PORT = "11739";
    private static final String PINGER_PORT = "11729";

    private UDPServer chatUdp;
    private UDPServer pinger;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("poker", ApplicationType.SERVER);

        // ChatServer needs its UDPServer, and the UDPServer needs a handler - wire them
        // together the way PokerServer does
        ChatServer[] chat = new ChatServer[1];
        chatUdp = new UDPServer(new UDPLinkHandler()
        {
            public int getTimeout(UDPLink link)
            {
                return chat[0].getTimeout(link);
            }

            public int getPossibleTimeoutNotificationInterval(UDPLink link)
            {
                return chat[0].getPossibleTimeoutNotificationInterval(link);
            }

            public int getPossibleTimeoutNotificationStart(UDPLink link)
            {
                return chat[0].getPossibleTimeoutNotificationStart(link);
            }
        }, true, true, CHAT_PORT);
        chat[0] = new ChatServer(chatUdp);
        chatUdp.init();
        chatUdp.manager().addMonitor(chat[0]);
        chatUdp.start();

        pinger = new UDPServer(chat[0], true, true, PINGER_PORT); // borrow chat's timeouts
        pinger.init();
        pinger.start();
    }

    @AfterEach
    public void tearDown()
    {
        pinger.shutdown();
        chatUdp.shutdown();
    }

    @Test
    public void chatServerAnswersPing()
    {
        InetSocketAddress chat = new InetSocketAddress(chatUdp.getPreferredIP(), chatUdp.getPreferredPort());
        assertEquals(ChatPing.Result.OK, ChatPing.check(pinger.manager(), chat, 2000));
    }
}
