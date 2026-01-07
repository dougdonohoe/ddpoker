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
import com.donohoedigital.config.*;
import com.donohoedigital.p2p.*;
import com.donohoedigital.udp.*;
import org.apache.logging.log4j.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 12, 2006
 * Time: 12:15:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class PokerConnect implements UDPLinkMonitor
{
    static Logger logger = LogManager.getLogger(PokerConnect.class);

    // timeout
    private static final int TIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.poker.connect.timeout.millis");

    // members
    private UDPServer udp_;
    private UDPLink link_;
    private PokerURL url_;
    private UDPID to_;
    private OnlineMessage omsg_ = null;
    private OnlineMessage reply_ = null;
    private DDMessage error_ = null;
    private Utils.WaitBoolean done_ = new Utils.WaitBoolean(this);
    private DDMessageListener listener_;

    /**
     * Constructor
     */
    public PokerConnect(UDPServer udp, PokerURL url, DDMessageListener listener)
    {
        this(udp, url, null, listener);
    }

    /**
     * Constructor
     */
    public PokerConnect(UDPServer udp, PokerURL url, UDPID to, DDMessageListener listener)
    {
        udp_ = udp;
        url_ = url;
        to_ = to;
        listener_ = listener;
    }

    /**
     * connect, return true if success, false if failure
     */
    public boolean connect(OnlineMessage msg)
    {
        omsg_ = msg;

        if (udp_.isBound())
        {
            if (listener_ != null) listener_.updateStep(DDMessageListener.STEP_CONNECTING);
            link_ = udp_.manager().getLink(to_, url_.getHost(), url_.getPort());
            link_.addMonitor(this);
            link_.connect();
            PokerUDPTransporter pudp = new PokerUDPTransporter(msg.getData());
            link_.queue(pudp.getData());
            link_.send();

            // wait until done
            if (listener_ != null)  listener_.updateStep(DDMessageListener.STEP_WAITING_FOR_REPLY);
            Utils.wait(done_, TIMEOUT);
        }

        // no reply, we had an error
        boolean bOK = true;
        if (reply_ == null)
        {
            // no error set, we timed out
            if (error_ == null)
            {
                timeout();
            }
            reply_ = new OnlineMessage(error_);
        }

        // OK if no error
        bOK = (error_ == null);

        // if error, close link
        if (!bOK)
        {
            close();
        }

        // cleanup
        if (link_ != null) link_.removeMonitor(this);

        // last update
        if (listener_ != null)  listener_.updateStep(DDMessageListener.STEP_DONE);

        return bOK;
    }

    /**
     * close
     */
    public void close()
    {
        if (link_ != null) link_.close();
    }

    /**
     * Link events - shutdown if receive a timeout or if link was closed
     */
    public void monitorEvent(UDPLinkEvent event)
    {
        switch(event.getType())
        {
            case CLOSED:
            case TIMEOUT:
            case RESEND_FAILURE:
                timeout();
                break;

            case RECEIVED:
                // process message
                UDPData data = event.getData();
                if (data.getType() == UDPData.Type.MESSAGE)
                {
                    PokerUDPTransporter msg = new PokerUDPTransporter(data);
                    messageReceived(new OnlineMessage(msg.getMessage()));
                }
        }
    }

    /**
     * message received
     */
    private void messageReceived(OnlineMessage reply)
    {
        if (done_.isDone()) return;

        if (reply.getInReplyTo() == omsg_.getMessageID())
        {
            // get status (in terms of existing framework)
            DDMessage ret = reply.getData();
            int nStatus = Peer2PeerMessenger.getStatus(ret); // TODO: clean this up?
            ret.setStatus(nStatus);
            reply_ = reply;

            // if status isn't okay, this is an error return
            if (nStatus != DDMessageListener.STATUS_OK)
            {
                error_ = ret;
            }

            // finished - update status
            done_.done();
        }
    }

    /**
     * Get reply
     */
    public OnlineMessage getReply()
    {
        return reply_;
    }

    /**
     * Get error
     */
    public DDMessage getError()
    {
        return error_;
    }

    /**
     * timeout
     */
    private void timeout()
    {
        if (error_ != null) return;

        error_ = new DDMessage(DDMessage.CAT_APPL_ERROR);
        error_.setStatus(DDMessageListener.STATUS_TIMEOUT);
        done_.done();
    }
}
