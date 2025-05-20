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
/*
 * Peer2PeerMessenger.java
 *
 * Created on November 29, 2004, 9:58 AM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import java.io.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Peer2PeerMessenger
{
    static Logger logger = LogManager.getLogger(Peer2PeerMessenger.class);
    
    Thread thread_ = null;
    Peer2PeerClient conn_ = null;
    
    /** 
     * Creates a new instance of Messenger 
     */
    public Peer2PeerMessenger() 
    {
    }
    
    /**
     * Return a status code for the given message
     */
    public static int getStatus(DDMessage ret)
    {
        int nStatus = DDMessageListener.STATUS_OK;
        if (ret.getCategory() == DDMessage.CAT_ERROR)
        {
            nStatus = DDMessageListener.STATUS_SERVER_ERROR;
        }
        else if (ret.getCategory() == DDMessage.CAT_APPL_ERROR)
        {
            nStatus = DDMessageListener.STATUS_APPL_ERROR;
        }
        return nStatus;
    }
    
    /**
     * Send the p2p message to the given destination.  If bWaitReply is
     * true, then the underlying Peer2PeerClient will wait for a reply
     * after sending that reply will be returned.  If it is false, then the
     * message is sent and null is returned.
     */
    public DDMessage sendMessage(P2PURL url, DDMessage send, DDMessageListener listener, boolean bWaitReply)
    {
        int nStatus;
        DDMessage ret;
        
        try 
        {
            Peer2PeerMessage reply = _sendMessage(url, send, listener, bWaitReply);
            if (!bWaitReply) return null;
            
            ret = reply.getMessage();
            nStatus = getStatus(ret);
        }
        // error handling closely linked to SendMessageDialog.java
        catch (ApplicationError ae)
        {
            Throwable e = ae.getException();
            if (e == null) e = ae;
            
            ret = new DDMessage();
            ret.setCategory(DDMessage.CAT_ERROR);
            ret.addData(Utils.formatExceptionText(e));

            nStatus = DDMessageListener.STATUS_UNKNOWN_ERROR;
            if (e instanceof ApplicationError)
            {
                ApplicationError ae2 = (ApplicationError) e;
                if (ae2.getErrorCode() == ErrorCodes.ERROR_404 ||
                    ae2.getErrorCode() == ErrorCodes.ERROR_403 ||
                    ae2.getErrorCode() == ErrorCodes.ERROR_503)
                {
                    nStatus = DDMessageListener.STATUS_CONNECT_FAILED;
                }
            }
            else if (e instanceof java.net.ConnectException)
            {
                nStatus = DDMessageListener.STATUS_CONNECT_FAILED;
            }
            else if (e instanceof java.net.SocketException)
            {
                nStatus = DDMessageListener.STATUS_CONNECT_FAILED;
            }
            else if (e instanceof java.net.UnknownHostException)
            {
                nStatus = DDMessageListener.STATUS_UNKNOWN_HOST;
            }
            else if (e instanceof java.net.SocketTimeoutException)
            {
                nStatus = DDMessageListener.STATUS_TIMEOUT;
            }
            else if (e instanceof java.io.EOFException)
            {
                nStatus = DDMessageListener.STATUS_TIMEOUT;
            }
            else if (e instanceof DNSTimeoutException)
            {
                nStatus = DDMessageListener.STATUS_DNS_TIMEOUT;
            }

            ret.setString(DDMessage.PARAM_EXCEPTION_MESSAGE, Utils.getExceptionMessage(e));
        }
        
        ret.setStatus(nStatus);
        if (listener != null) listener.messageReceived(ret);
        return ret;
    }
    
    /**
     * Send a message in a thread and return response to the DDMessageListener
     */
    public void sendMessageAsync(P2PURL url, DDMessage send, DDMessageListener listener, boolean bWaitReply)
    {
        P2PURLThread urlthread = new P2PURLThread(url, send, listener, bWaitReply);
        thread_ = new Thread(urlthread, "P2PUrlThread");
        thread_.start();
    }
    
    /**
     * Class to request url in a thread and send result back to a listener
     */
    private class P2PURLThread implements Runnable
    {
        P2PURL url;
        DDMessage send;
        DDMessageListener listener;
        boolean bWaitReply;
        
        public P2PURLThread(P2PURL url, DDMessage send,  DDMessageListener listener, boolean bWaitReply)
        {
            this.url = url;
            this.send = send;
            this.listener = listener;
            this.bWaitReply = bWaitReply;
        }
        
        public void run()
        {
            sendMessage(url, send, listener, bWaitReply);
        }
    }
    
    /**
     * Send message
     */
    private Peer2PeerMessage _sendMessage(P2PURL url, DDMessage send, DDMessageListener listener, boolean bWaitReply)
    {
         try 
        {    
            // create http client if we have none
            if (conn_ == null)
            {
                conn_ = new Peer2PeerClient(url, null, listener);
                // open connection
                conn_.connect();
            }
            
            // send message and wait for reply
            Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, send);
            if (bWaitReply)
            {
                Peer2PeerMessage reply = conn_.sendGetReply(p2p);     
                // finished - notify listener and return data
                if (listener != null) listener.updateStep(DDMessageListener.STEP_DONE);
                return reply;
            }
            else
            {
                conn_.send(p2p);
                return null;
            }
                
        }
        catch (Exception io)
        {
            throw new ApplicationError(io);
        }
    }

    /**
     * abort connection (e.g., external timeout)
     */
    public void abort()
    {
        if (thread_ != null)
        {
            thread_.interrupt();
        }

        if (conn_ != null)
        {
            try
            {
                conn_.close();
            }
            catch (IOException e)
            {
                // ignore
            }
        }
    }

    /**
     * Get Peer2Peer client used by this messenger
     */
    public Peer2PeerClient getPeer2PeerClient()
    {
        return conn_;
    }
    
    /**
     * Set Peer2Peer client
     */
    public void setPeer2PeerClient(Peer2PeerClient c)
    {
        conn_ = c;
    }
}
