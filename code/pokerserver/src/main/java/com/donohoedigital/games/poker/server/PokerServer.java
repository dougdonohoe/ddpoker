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
/*
 * PokerServer.java
 *
 * Created on August 13, 2003, 3:47 PM
 */

package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.server.EngineServer;
import com.donohoedigital.udp.*;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 *
 * @author  donohoe
 */
public class PokerServer extends EngineServer implements UDPLinkHandler, UDPManagerMonitor
{
    // udp server for test connections and chat
    private UDPServer udp_;
    private ChatServer chat_;

    /**
     * Initialize, start UDP and run
     */
    @Override
    public void init()
    {
        super.init();
        udp_.manager().addMonitor(this);
        udp_.start();
        start();
    }

    /**
     * Set UDP
     */
    public void setUDPServer(UDPServer udp)
    {
        udp_ = udp;
    }

    /**
     * Get UDP
     */
    public UDPServer getUDPServer()
    {
        return udp_;
    }

    /**
     * Set chat server
     */
    public void setChatServer(ChatServer chat)
    {
        chat_ = chat;
    }

    ////
    //// UDPManagerMonitor
    ////

    public void monitorEvent(UDPManagerEvent event)
    {
        UDPLink link = event.getLink();
        if (chat_.isChat(link)) chat_.monitorEvent(event);
        else switch(event.getType())
        {
            case CREATED:
                if (TESTING(UDPServer.TESTING_UDP))
                    logger.debug("PublicTest Created: {}", Utils.getAddressPort(link.getRemoteIP()));
                break;

            case DESTROYED:
                if (TESTING(UDPServer.TESTING_UDP))
                    logger.debug("PublicTest Destroyed: {}", Utils.getAddressPort(link.getRemoteIP()));
                break;
        }
        //logger.debug("Event: "+ event + " on " + Utils.getAddressPort(event.getLink().getLocalIP()));
    }

    ////
    //// UDPLinkHandler interface
    ////

    public int getTimeout(UDPLink link)
    {
        if (chat_.isChat(link)) return chat_.getTimeout(link);
        return UDPLink.DEFAULT_TIMEOUT; // Timeout for TestConnection
    }

    public int getPossibleTimeoutNotificationInterval(UDPLink link)
    {
        if (chat_.isChat(link)) return chat_.getPossibleTimeoutNotificationInterval(link);
        return getTimeout(link);
    }

    public int getPossibleTimeoutNotificationStart(UDPLink link)
    {
        if (chat_.isChat(link)) return chat_.getPossibleTimeoutNotificationStart(link);
        return getTimeout(link);
    }
}
