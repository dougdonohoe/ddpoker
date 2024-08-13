/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
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
 * Peer2PeerSocketThread.java
 *
 * Created on November 1, 2004, 9:37 AM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.*;
import org.apache.log4j.*;
import com.donohoedigital.server.*;

import javax.servlet.*;
import java.io.*;
import java.nio.channels.*;

/**
 * Class for handling incoming p2p connections.
 *
 * @author  donohoe
 * 
 */
public class Peer2PeerSocketThread extends SocketThread
{
    static Logger logger = Logger.getLogger(Peer2PeerSocketThread.class);
    
    private Peer2PeerMessage msg_;
    private boolean bKeepAlive_ = true;
    private Peer2PeerControllerInterface controller_;
    
    /** 
     * Creates a new instance of Peer2PeerSocketThread 
     */
    public Peer2PeerSocketThread() 
    {
    }
    
    /**
     * init - get controller from server too
     */
    public void init(ThreadPool pool, BaseServlet servlet)
    {
        super.init(pool, servlet);
        Peer2PeerServer server = (Peer2PeerServer) pool.getServer();
        controller_ = server.getController();
    }
    
    /**
     * We want to keep alive socket after reading
     */
    protected boolean isKeepAlive()
    {
        return bKeepAlive_;
    }
    
    /**
     * Handle an exception before we get to processing a valid message
     */
    protected boolean handleException(Throwable t)
    {
        boolean bShutDown = false;
        
        try {
            String sMsg = null;
            String sRemoteAddr = Utils.getIPAddress(channel_);
            if (t instanceof ApplicationError)
            {
                ApplicationError ae = (ApplicationError) t;
                if (ae.getErrorCode() == ErrorCodes.ERROR_INVALID_MESSAGE)
                {
                    bShutDown = true;
                }
                
                // TODO: shutdown on other ApplicationErrors?
                
                sMsg = Utils.formatExceptionText(ae);
                logger.error(Utils.getExceptionMessage(ae) + ": ["+sRemoteAddr+"]; stacktrace: " + sMsg);
            }
            else
            {
                bShutDown = true;
                
                boolean bLogError = true;
                if (t instanceof EOFException)
                {
                    // EOF is thrown by Peer2PeerMessage and is thrown when a socket
                    // has been closed on the other side
                    bLogError = false;    
                }
                else if (t instanceof IOException)
                {
                    String sMessage = Utils.getExceptionMessage(t);
                    if (sMessage.indexOf("forcibly closed") != -1 ||
                        sMessage.indexOf("reset by peer") != -1)
                    {
                        // Remote host forcibly closed connection, which
                        // means internet connection died or remote client
                        // shutdown improperly
                        bLogError = false;
                    }
                }
                
                if (bLogError)
                {
                    sMsg = Utils.formatExceptionText(t);
                    logger.error(t.getClass().getName() + " ["+sRemoteAddr+"]; stacktrace: " + sMsg);
                }
            }
        } 
        catch (Throwable tt) 
        {
            logger.error("Exception in handleException: " + Utils.formatExceptionText(tt) +
                            "; occurred while handling exception: " + Utils.formatExceptionText(t));
        }
        
        return bShutDown;
    }

    /**
     * Init to begin new request
     */
    protected void initRequest()
    {
        msg_ = null;
    }

    /**
     * Read data
     */
    protected void readData(SocketChannel channel) throws IOException
    {
        bKeepAlive_ = true;
        msg_ = new Peer2PeerMessage();
        msg_.setFromIP(Utils.getIPAddress(channel));
        msg_.read(channel);
    }
   
    /**
     * process data that was read
     */
    protected void process() throws IOException, ServletException
    {
        if (controller_ != null)
        {
            Peer2PeerMessage reply = (Peer2PeerMessage) controller_.p2pMessageReceived(channel_, msg_);
            if (reply != null)
            {
                bKeepAlive_ = reply.isKeepAlive();
                reply.write(channel_);
            }
        }
    }
}
