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
package com.donohoedigital.games.poker.network;

import com.donohoedigital.base.*;
import com.donohoedigital.udp.*;

import java.nio.channels.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 11, 2006
 * Time: 7:56:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class PokerConnection
{
    private UDPID udpConn;
    private SocketChannel tcpConn;

    public PokerConnection(UDPID id)
    {
        udpConn = id;
    }

    public PokerConnection(SocketChannel socket)
    {
        tcpConn = socket;
    }

    public SocketChannel getSocket()
    {
        return tcpConn;
    }

    public UDPID getUDPID()
    {
        return udpConn;
    }

    public boolean isTCP()
    {
        return tcpConn != null;
    }

    public boolean isUDP()
    {
        return udpConn != null;
    }

    @Override
    public String toString()
    {
        if (isTCP())
        {
            return Utils.getIPAddress(tcpConn);
        }
        else
        {
            return udpConn.toString();
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;

        if (!(o instanceof PokerConnection)) return false;
        PokerConnection c = (PokerConnection) o;

        if (isTCP() && !c.isTCP()) return false;
        if (isUDP() && !c.isUDP()) return false;

        if (isTCP())
        {
            return tcpConn.equals(c.tcpConn);
        }
        else
        {
            return udpConn.equals(c.udpConn);
        }
    }

    @Override
    public int hashCode()
    {
        if (isTCP()) return tcpConn.hashCode();
        if (isUDP()) return udpConn.hashCode();
        return super.hashCode();
    }
}

