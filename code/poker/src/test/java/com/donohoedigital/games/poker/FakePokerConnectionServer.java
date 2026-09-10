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

import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DDMessageTransporter;
import com.donohoedigital.games.poker.network.PokerConnection;
import com.donohoedigital.games.poker.network.PokerConnectionServer;
import com.donohoedigital.games.poker.network.PokerUDPTransporter;
import com.donohoedigital.p2p.Peer2PeerMessage;

/**
 * A PokerConnectionServer that builds messages of one transport without binding a socket
 * or starting a thread.
 * <p/>
 * newMessage() mirrors the real implementations exactly - PokerUDPServer.newMessage()
 * returns a PokerUDPTransporter, PokerMain.PokerTCPServer.newMessage() returns a
 * Peer2PeerMessage - which is what lets a test tell the two transports apart by the
 * concrete type of reply.
 */
public class FakePokerConnectionServer implements PokerConnectionServer
{
    private final boolean bUDP_;

    private FakePokerConnectionServer(boolean bUDP)
    {
        bUDP_ = bUDP;
    }

    public static FakePokerConnectionServer udp()
    {
        return new FakePokerConnectionServer(true);
    }

    public static FakePokerConnectionServer tcp()
    {
        return new FakePokerConnectionServer(false);
    }

    //
    // PokerConnectionServer
    //

    public boolean isUDP()
    {
        return bUDP_;
    }

    public DDMessageTransporter newMessage(DDMessage msg)
    {
        return bUDP_ ? new PokerUDPTransporter(msg)
                     : new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);
    }

    public int send(PokerConnection connection, DDMessageTransporter message)
    {
        return 0;
    }

    public void init() {}

    public boolean isBound()
    {
        return true;
    }

    public void start() {}

    public void shutdown() {}

    public String getPreferredIP()
    {
        return "127.0.0.1";
    }

    public int getPreferredPort()
    {
        return bUDP_ ? 11889 : 11880;
    }

    public String getConfigPort()
    {
        return Integer.toString(getPreferredPort());
    }

    public void closeConnection(PokerConnection connection) {}
}
