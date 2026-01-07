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

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.udp.*;
import org.apache.logging.log4j.*;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 11, 2006
 * Time: 3:05:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class PokerUDPTransporter implements DDMessageTransporter
{
    static Logger logger = LogManager.getLogger(PokerUDPTransporter.class);

    private DDMessage msg_;
    private boolean bKeepAlive_;
    private ByteData bytes_;

    public PokerUDPTransporter(DDMessage msg)
    {
        msg_ = msg;
    }

    public PokerUDPTransporter(UDPData data)
    {
        msg_ = new DDMessage();

        try
        {
            msg_.read(new ByteArrayInputStream(data.getData(), data.getOffset(), data.getLength()), data.getLength());
        }
        catch (IOException ignored)
        {
            // nothing will be thrown since using byte array output stream
        }
    }

    public ByteData getData()
    {
        if (bytes_ == null)
        {
            DDByteArrayOutputStream ddmsg = new DDByteArrayOutputStream(2048);
            try
            {
                msg_.write(ddmsg);
            }
            catch (IOException ignored)
            {
                // nothing will be thrown since using byte array output stream
            }
            ddmsg.toByteArray();
            bytes_ = new ByteData(ddmsg.getBuffer(), 0, ddmsg.size());
        }
        return bytes_;
    }

    public DDMessage getMessage()
    {
        return msg_;
    }

    public void setMessage(DDMessage msg)
    {
        msg_ = msg;
        bytes_ = null;
    }

    public void setKeepAlive(boolean b)
    {
        bKeepAlive_ = b;
    }

    public boolean isKeepAlive()
    {
        return bKeepAlive_;
    }
}
