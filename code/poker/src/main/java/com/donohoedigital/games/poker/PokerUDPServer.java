/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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

import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.udp.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.config.*;
import com.donohoedigital.p2p.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import java.net.*;

/**
 * Poker UDP server
 */
public class PokerUDPServer extends UDPServer implements PokerConnectionServer, ChatLobbyManager
{
    private static Logger logger = LogManager.getLogger(PokerUDPServer.class);

    private PokerMain main_;
    private UDPLink chatLink_;
    private InetSocketAddress chatServer_;

    PokerUDPServer(PokerMain pokerMain)
    {
        super(pokerMain, false);
        main_ = pokerMain;
    }

    InetSocketAddress getChatServer()
    {
        return chatServer_;
    }

    private InetSocketAddress _getChatServer()
    {
        if (chatServer_ == null)
        {
            String sChatServer = GameEngine.getGameEngine().getPrefsNode().getStringOption(PokerConstants.OPTION_ONLINE_CHAT);
            if (sChatServer == null || sChatServer.isEmpty()) {
                return null;
            }
            P2PURL url = new P2PURL("chat://"+sChatServer+'/'); // easy parsing
            chatServer_ = new InetSocketAddress(url.getHost(), url.getPort());
        }
        return chatServer_;
    }

    UDPLink getChatLink()
    {
        return chatLink_;
    }

    void closeChatLink()
    {
        if (chatLink_ != null)
        {
            chatLink_.close();
            chatLink_ = null;
        }
    }

    void nullChatLink()
    {
        chatLink_ = null;
    }

    public void closeConnection(PokerConnection connection)
    {
        if (connection == null) return;

        UDPLink link = manager().getLink(connection.getUDPID());
        if (link == null) return;

        link.close();
    }

    public int send(PokerConnection connection, DDMessageTransporter message)
    {
        if (connection == null) return 0;

        // get link, if none - log error and return
        UDPLink link = manager().getLink(connection.getUDPID());
        if (link == null)
        {
            PokerGame game = (PokerGame) main_.getDefaultContext().getGame();
            PokerPlayer p = game.getPokerPlayerFromConnection(connection);
            logger.warn("No link found for " + p.getName() + " ("+connection+"), skipping message");
            return 0;
        }

        // queue message and request to send
        PokerUDPTransporter pudp = (PokerUDPTransporter) message;
        ByteData data = pudp.getData();
        link.queue(data);
        manager().addLinkToSend(link);

        // not likely to be used, but return length of data we will be sending
        return data.getLength();
    }

    public DDMessageTransporter newMessage(DDMessage msg)
    {
        return new PokerUDPTransporter(msg);
    }

    public void sendChat(PlayerProfile profile, String sMessage)
    {
        OnlineMessage omsg;
        PokerUDPTransporter pudp;

        // can't do anything if not bound
        if (!isBound())
        {
            main_.notifyTimeout();
            return;
        }

        // see if connected
        if (checkConnected(profile))
        {

            if (sMessage != null)
            {
                // send chat
                omsg = new OnlineMessage(OnlineMessage.CAT_CHAT);
                omsg.setPlayerName(profile.getName());
                omsg.setChat(sMessage);
                pudp = new PokerUDPTransporter(omsg.getData());
                chatLink_.queue(pudp.getData(), PokerConstants.USERTYPE_CHAT);
            }

            // queue for sending
            manager().addLinkToSend(chatLink_);
        }
    }

    private boolean checkConnected(PlayerProfile profile)
    {
        OnlineMessage omsg;
        PokerUDPTransporter pudp;

        if (chatLink_ == null || chatLink_.isDone())
        {
            ChatHandler handler = main_.getChatLobbyHandler();

            // if not enabled, just show same message we show in the SendMessageDialog
            if (!GameEngine.getGameEngine().getPrefsNode().getBooleanOption(EngineConstants.OPTION_ONLINE_ENABLED)) {
                // msg.msgerror.7
                if (handler != null)
                {
                    OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
                    chat.setChat(PropertyConfig.getMessage("msg.msgerror.7"));
                    handler.chatReceived(chat);
                }
                return false;
            }

            // make sure addr is resolved
            InetSocketAddress addr = _getChatServer();
            if (addr == null || addr.isUnresolved())
            {
                if (handler != null)
                {
                    OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
                    if (addr == null) {
                        chat.setChat(PropertyConfig.getMessage("msg.chat.lobby.notset"));
                    } else {
                        chat.setChat(PropertyConfig.getMessage("msg.chat.lobby.unresolved", addr.getHostString()));
                    }
                    handler.chatReceived(chat);
                }
                return false;
            }

            // display message that we are connecting...
            if (handler != null)
            {
                OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
                chat.setChat(PropertyConfig.getMessage("msg.chat.lobby.connect"));
                handler.chatReceived(chat);
            }

            // new link, so send hello
            chatLink_ = manager().getLink(_getChatServer());
            chatLink_.setName("Chat Server");
            chatLink_.connect();
            omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_HELLO);
            OnlineProfile auth = new OnlineProfile(profile.getName());
            auth.setLicenseKey(main_.getRealLicenseKey());
            auth.setPassword(profile.getPassword());
            omsg.setWanAuth(auth.getData());
            pudp = new PokerUDPTransporter(omsg.getData());
            chatLink_.queue(pudp.getData(), PokerConstants.USERTYPE_HELLO);
        }

        return true;
    }
}
