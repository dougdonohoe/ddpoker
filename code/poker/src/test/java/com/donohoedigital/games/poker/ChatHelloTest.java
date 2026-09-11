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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.network.OnlineMessage;
import com.donohoedigital.games.poker.network.PokerUDPTransporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A game client that receives a chat-lobby hello says so, instead of dropping it.
 * <p/>
 * This happens when someone's chat server address points at a game client rather than at
 * the chat server - which is easy to do, because a client that fails to bind its
 * configured UDP port drifts to the next one and can land on an address another client is
 * pointed at.  Dropping the hello left that sender waiting out a timeout with no clue why.
 * <p/>
 * The reply reuses the chat server's own "go away" protocol rather than inventing one:
 * ChatServer.sendError() sends a CHAT_ADMIN_ERROR admin chat and closes the link, and
 * OnlineLobby.chatReceived() already displays it and removes the chat input controls.
 */
public class ChatHelloTest
{
    /** the address the stray hello arrived on - what the sender has wrong in their options */
    private static final InetSocketAddress LOCAL = new InetSocketAddress("192.168.64.1", 11888);

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    private OnlineMessage reply()
    {
        return new OnlineMessage(PokerMain.notChatLobbyReply(LOCAL).getMessage());
    }

    @Test
    public void replyIsAnAdminErrorChat()
    {
        PokerUDPTransporter reply = PokerMain.notChatLobbyReply(LOCAL);
        assertNotNull(reply);

        OnlineMessage omsg = new OnlineMessage(reply.getMessage());
        assertEquals(OnlineMessage.CAT_CHAT_ADMIN, omsg.getCategory(),
                     "must be an admin chat so OnlineLobby routes it to the error branch");
        assertEquals(PokerConstants.CHAT_ADMIN_ERROR, omsg.getChatType(),
                     "CHAT_ADMIN_ERROR is what removes the sender's chat input controls");
    }

    /**
     * The text has to actually say something - it is the only thing the sender sees.
     */
    @Test
    public void replyExplainsWhatIsWrong()
    {
        String chat = reply().getChat();

        assertNotNull(chat, "msg.chat.notlobby must resolve");
        assertFalse(chat.isBlank(), "msg.chat.notlobby must not be empty");
        assertFalse(chat.contains("{0}"), "no unsubstituted parameters: " + chat);
    }

    /**
     * It names the address the hello arrived on, because that is the value the sender has
     * wrong.  This discloses nothing - they just sent a packet to it.
     */
    @Test
    public void replyNamesTheAddressTheSenderGotWrong()
    {
        String chat = reply().getChat();

        assertTrue(chat.contains("192.168.64.1:11888"),
                   "should name the address the sender used: " + chat);
    }

    /**
     * It must say nothing about the game or the player - the sender is unauthenticated and
     * all they are entitled to know is that they have the wrong address.
     */
    @Test
    public void replyCarriesNoGameOrPlayerDetail()
    {
        OnlineMessage omsg = reply();

        assertNull(omsg.getGameID(), "must not name the game");
        assertNull(omsg.getPlayerName(), "must not name the player");
        // getPlayerList() builds a list and would throw if the key were absent, so ask the
        // underlying data instead - the real chat server only sets this on a welcome
        assertNull(omsg.getData().getList(OnlineMessage.ON_PLAYER_LIST),
                   "must not list who is online");
    }
}
