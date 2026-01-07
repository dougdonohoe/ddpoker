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
 * PokerP2PHeadless.java
 *
 * Created on November 28, 2004, 8:11 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.p2p.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerP2PHeadless implements OnlineMessageListener, DDMessageListener
{
    static Logger logger = LogManager.getLogger(PokerP2PHeadless.class);

    // members
    private PokerGame game_;
    private OnlineManager mgr_;
    private P2PURL url_;
    private Peer2PeerMessenger msgr_;
    private OnlineMessage omsg_;
    private OnlineMessage oreply_;
    private DDMessage mReturn_;
    private int nStatus_;
    private boolean bDone_ = false;
    private Thread timer_;

    public PokerP2PHeadless(GameContext context, OnlineMessage omsg)
    {
        omsg_ = omsg;

        game_ = (PokerGame) context.getGame();
        mgr_ = game_.getOnlineManager();
        ApplicationError.assertNotNull(mgr_, "No OnlineManager found", null);

        url_ = omsg_.getConnectURL();
        ApplicationError.assertNotNull(omsg_, "No URL to send to", null);
    }

    /**
     * When window is opened, send the message
     */
    public synchronized OnlineMessage send()
    {
        // send async
        mgr_.addOnlineMessageListener(this);
        timer_ = new Thread(new Timeout(), "PokerP2PTimer");
        msgr_ = new Peer2PeerMessenger();
        msgr_.sendMessageAsync(url_, omsg_.getData(), this, false);
        timer_.start();

        logger.debug("waiting...");
        try{
            wait();
        } catch (InterruptedException ie) { }

        logger.debug("Done Waiting...");
        removeListener();
        return oreply_;
    }

    int nLastStep = -1;

    /**
     * Called to indicate message step - register host socket when we know
     * we are connected
     */
    public void updateStep(int nStep) 
    {
        if (nStep == DDMessageListener.STEP_SENDING)
        {
            // register this socket for more messages from server
            mgr_.registerSocket(msgr_.getPeer2PeerClient().getSocketChannel());
        }
        // only display status if this is a new step (could be getting an
        // old step like waiting for reply due to timing issues)
        if (nStep > nLastStep)
        {
            nLastStep = nStep;
            // logger.debug("step...");
        }
    }

    public void debugStep(int nStep, String sMsg)
    {
    }

    /**
     * Get messenger used to send the message
     */
    public Peer2PeerMessenger getPeer2PeerMessenger()
    {
        return msgr_;
    }

    /**
     * Get status
     */
    public int getStatus()
    {
        return nStatus_;
    }

    /**
     * Get error message
     */
    public DDMessage getErrorMessage()
    {
        if (nStatus_ != DDMessageListener.STATUS_OK)
        {
            return mReturn_;
        }

        return null;
    }

    /**
     * when the return message is received, this is called
     */
    public synchronized void messageReceived(DDMessage message)
    {
        //message.debugPrint();
        mReturn_ = message;
        nStatus_ = message.getStatus();
        if (bDone_) return;
        bDone_ = true;
        notify();
    }

    /**
     * called when message received on P2P channel
     */
    public synchronized void messageReceived(OnlineMessage reply) 
    {
        if (reply.getInReplyTo() == omsg_.getMessageID())
        {
            // finished - update status
            oreply_ = reply;
            timer_.interrupt();   
            DDMessage ret = reply.getData();
            ret.setStatus(msgr_.getStatus(ret));
            messageReceived(ret);
        }
    }
    
    /**
     * get reply OnlineMessage
     */
    public OnlineMessage getReply()
    {
        return oreply_;
    }
    
    /**
     * timed-out, generate TIMEOUT error message
     */
    private synchronized void timedOut()
    {
        if (!bDone_)
        {
            removeListener();
            DDMessage ret = new DDMessage(DDMessage.CAT_APPL_ERROR);
            ret.setStatus(DDMessageListener.STATUS_TIMEOUT);
            messageReceived(ret);
            msgr_.abort();
        }
    }
    
    /**
     * clean up
     */
    private synchronized void removeListener()
    {
        if (mgr_ == null) return;
        mgr_.removeOnlineMessageListener(this);
        mgr_ = null;
    }
    
    /**
     * Thread to track timeout
     */
    private class Timeout implements Runnable
    {
       /**
        * Sleep for timeout then call timedOut()
        */
       public void run() {
           Utils.sleepMillis(Peer2PeerMessage.READ_TIMEOUT_MILLIS);
           timedOut();
       }
    }
}
