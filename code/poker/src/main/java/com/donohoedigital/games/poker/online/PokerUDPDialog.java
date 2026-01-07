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
 * PokerUDPDialog.java
 *
 * Created on November 28, 2004, 8:11 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.udp.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerUDPDialog extends SendMessageDialog implements Runnable
{
    static Logger logger = LogManager.getLogger(PokerUDPDialog.class);

    public static final String P2P_ERROR_MSG_MODIFIER = "p2p";
    public static final String PARAM_MSG = "online-msg";
    public static final String PARAM_DISPLAY = "online-display";

    private OnlineManager mgr_;
    private PokerURL url_;
    private PokerConnect connect_;
    private OnlineMessage omsg_;

    /**
     * Get url before doing super work
     */
    @Override
    public void start()
    {
        PokerGame game = (PokerGame) context_.getGame();

        mgr_ = game.getOnlineManager();
        ApplicationError.assertNotNull(mgr_, "No OnlineManager found", null);

        omsg_ = (OnlineMessage) gamephase_.getObject(PARAM_MSG);
        ApplicationError.assertNotNull(omsg_, "No OnlineMessage to send", null);
        
        url_ = omsg_.getConnectURL();
        ApplicationError.assertNotNull(omsg_, "No URL to send to", null);
        
        super.start();
    }
    
    /**
     * Port part of URL for use in error message
     */
    @Override
    protected int getPort()
    {
        return url_.getPort();
    }
    
    /**
     * Return "p2p" for error modifier if none provided in gamephase params
     */
    @Override
    protected String getErrorMod()
    {
        return gamephase_.getString(PARAM_ERROR_KEY_MOD, P2P_ERROR_MSG_MODIFIER);
    }
    
    /**
     * When window is opened, send the message
     */
    @Override
    protected void opened()
    {
        sendMessage();        
    }
    
    /**
     * Send the message to the given URL, or if null, use the default destination
     */
    protected void sendMessage()
    {
        // send async
        connect_ = new PokerConnect((UDPServer) mgr_.getP2P(), url_, this);
        Thread t = new Thread(this, "PokerConnect");
        t.start();
    }

    /**
     * runnable for async thread
     */
    public void run()
    {
        // this blocks until message received
        boolean bOK = connect_.connect(omsg_);

        // deliver message
        if (bOK)
        {
            messageReceived(connect_.getReply().getData());
        }
        else
        {
            messageReceived(connect_.getError());
        }
    }
        
    /**
     * Return message to display from gamephase params PARAM_DISPLAY
     */
    @Override
    protected String getMessageString()
    {
        return gamephase_.getString(PARAM_DISPLAY);
    }

    /**
     * Get error message
     */
    public DDMessage getErrorMessage()
    {
        return connect_.getError();
    }

    /**
     * get reply OnlineMessage
     */
    public OnlineMessage getReply()
    {
        return connect_.getReply();
    }

    /**
     * We don't use getMessage()
     */
    @Override
    public EngineMessage getMessage()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getMessage() not used in PokerUDPDialog", null);
    }

    /**
     * We don't use getReturnMessage()
     */
    @Override
    public EngineMessage getReturnMessage()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getReturnMessage() not used in PokerUDPDialog", null);
    }
}
