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

import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.udp.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether a chat server address is reachable and is actually a chat server,
 * without logging in to the lobby.
 * <p/>
 * Sends a USERTYPE_PING over a UDP link.  The UDP handshake (our HELLO being acked)
 * shows something running DD Poker's UDP stack is at the address; ChatServer answers
 * the ping with a USERTYPE_PONG, while a game client answers with the same "not a chat
 * lobby" admin error it sends for a stray lobby hello.
 */
public class ChatPing implements UDPLinkMonitor
{
    public static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    // name given to links created for the test, so the client can ignore their closing
    public static final String LINK_NAME = "Chat Server Test";

    public enum Result
    {
        OK,              // chat server answered
        NOT_CHAT_SERVER, // a game client answered
        NO_REPLY,        // reachable, but no answer - older chat server or something else
        UNREACHABLE      // UDP handshake never completed
    }

    private final CountDownLatch done_ = new CountDownLatch(1);
    private volatile boolean bEstablished_;
    private volatile Result reply_;

    private ChatPing()
    {
    }

    /**
     * Ping the chat server at the given address, using links from the given manager.  Blocks
     * for up to timeoutMillis, so don't call from the Swing thread.  If a link to the server
     * already exists (e.g., we are in the lobby), it is used and left open.
     */
    public static Result check(UDPManager manager, InetSocketAddress server, int timeoutMillis)
    {
        List<UDPLink> links = new ArrayList<>();
        manager.getLinks(links);
        boolean bExisting = links.stream().anyMatch(l -> l.getRemoteIP().equals(server) && !l.isDone());

        ChatPing ping = new ChatPing();
        UDPLink link = manager.getLink(server);
        link.addMonitor(ping);
        try
        {
            if (bExisting)
            {
                if (link.isEstablished()) ping.bEstablished_ = true;
            }
            else
            {
                link.setName(LINK_NAME);
                link.connect();
            }

            link.queue(ping().getData(), PokerConstants.USERTYPE_PING);
            manager.addLinkToSend(link);

            //noinspection ResultOfMethodCallIgnored
            ping.done_.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            link.removeMonitor(ping);
            if (!bExisting) link.close();
        }

        if (ping.reply_ != null) return ping.reply_;
        return ping.bEstablished_ ? Result.NO_REPLY : Result.UNREACHABLE;
    }

    /**
     * is this a link created by check()?
     */
    public static boolean isPingLink(UDPLink link)
    {
        return LINK_NAME.equals(link.getName());
    }

    /**
     * Ping message (the user type is what matters - the message just has to be valid)
     */
    public static PokerUDPTransporter ping()
    {
        return new PokerUDPTransporter(new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN).getData());
    }

    /**
     * Pong message - ChatServer's reply to a ping
     */
    public static PokerUDPTransporter pong()
    {
        return new PokerUDPTransporter(new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN).getData());
    }

    private void finish(Result result)
    {
        reply_ = result;
        done_.countDown();
    }

    public void monitorEvent(UDPLinkEvent event)
    {
        switch (event.getType())
        {
            case ESTABLISHED:
                bEstablished_ = true;
                break;

            case RECEIVED:
                UDPData data = event.getData();
                if (data.getType() != UDPData.Type.MESSAGE) break;

                if (data.getUserType() == PokerConstants.USERTYPE_PONG)
                {
                    finish(Result.OK);
                }
                else if (data.getUserType() == PokerConstants.USERTYPE_CHAT)
                {
                    // a game client's "not a chat lobby" reply
                    OnlineMessage omsg = new OnlineMessage(new PokerUDPTransporter(data).getMessage());
                    if (omsg.getCategory() == OnlineMessage.CAT_CHAT_ADMIN &&
                        omsg.getChatType() == PokerConstants.CHAT_ADMIN_ERROR)
                    {
                        finish(Result.NOT_CHAT_SERVER);
                    }
                }
                break;

            case TIMEOUT:
            case RESEND_FAILURE:
            case CLOSED:
                done_.countDown(); // no point waiting any longer
                break;
        }
    }
}
