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
 * Peer2PeerClient.java
 *
 * Created on November 1, 2004, 1:16 PM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;

/**
 * Simple client to use in messaging architecture, handles DNS timeout
 * and other things
 *
 * @author  donohoe
 */
public class Peer2PeerClient 
{
    static Logger logger = LogManager.getLogger(Peer2PeerClient.class);
    
    // debug
    private static boolean DEBUG_DETAILS = false;
    public static boolean DEBUG_LISTENER = false;
        
    // timeout control
    private static int WAITMILLIS = 100; // time to wait while looping
    //private static int DNSTIMEOUT;
    private static int CONNECTTIMEOUT;  
    private static int SOCKETTIMEOUT;    
    
    /**
     * Create static messanger for default usage
     */
    static 
    {
        SOCKETTIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.p2p.sockettimeout.millis");
        CONNECTTIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.p2p.connecttimeout.millis");
        //DNSTIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.http.dnstimeout.millis");
        //System.getProperties().setProperty("sun.net.client.defaultReadTimeout", Integer.toString(SOCKETTIMEOUT));
        //System.getProperties().setProperty("sun.net.client.defaultConnectTimeout", Integer.toString(CONNECTTIMEOUT));
    }
    
    // initialization members
    private InetSocketAddress addr_;
    private SocketChannel sc_;
    private DDMessageListener listener_;
    private Peer2PeerMessageListener plistener_;
    
    // members used during read/write

    /**
     * Create p2p client from connect string
     */
    public Peer2PeerClient(P2PURL url, 
                            Peer2PeerMessageListener plistener, DDMessageListener listener)
    {
        this(url, plistener, listener, null);
    }
    
    /**
     * Create p2p client from connect string, passing in an existing socket to use
     */
    public Peer2PeerClient(P2PURL url, 
                            Peer2PeerMessageListener plistener, DDMessageListener listener,
                            SocketChannel sc)
    {
        init(url.getHost(), url.getPort(), plistener, listener, sc);
    }
    
    /** 
     * Creates a new instance of Peer2PeerClient 
     */
    public Peer2PeerClient(String sIP, int nPort,
                            Peer2PeerMessageListener plistener, DDMessageListener listener)
    {
        this(sIP, nPort, plistener, listener, null);
    }
    
    /** 
     * Creates a new instance of Peer2PeerClient, passing in an existing socket to use
     */
    public Peer2PeerClient(String sIP, int nPort,
                            Peer2PeerMessageListener plistener, DDMessageListener listener,
                            SocketChannel sc)
    {
        init(sIP, nPort, plistener, listener, sc);
    }
    
    /**
     * initialize
     */
    private void init(String sIP, int nPort,
                        Peer2PeerMessageListener plistener, DDMessageListener listener,
                        SocketChannel sc)
    {
        try {
            listener_ = listener;
            plistener_ = plistener;
            addr_ = new InetSocketAddress(sIP, nPort);
            sc_ = sc;
            if (sc_ == null)
            {
                sc_ = SocketChannel.open();
                sc_.configureBlocking(false);
                Socket socket = sc_.socket();
                socket.setReuseAddress(true);
                socket.setKeepAlive(false);
                if (Utils.TCPNODELAY) socket.setTcpNoDelay(true);
                socket.setSoLinger(false, 0);
                socket.setSoTimeout(SOCKETTIMEOUT);
                socket.setSendBufferSize(64 * 1024);
                socket.setReceiveBufferSize(64 * 1024);
            }
        } 
        catch (Exception e) 
        {
            throw new ApplicationError(e);
        }
    }
    
    /**
     * Connect to server
     */
    public void connect() throws IOException
    {
        int nWait = 0;
        if (listener_ != null) listener_.updateStep(DDMessageListener.STEP_CONNECTING);
        
        // catch close by interrupt exception so we can clear interrupted flag
        // so it doesn't affect other operations.  Also, catch all other exceptions
        // just in case
        try {
            sc_.connect(addr_);
        }
        catch (ClosedByInterruptException cbie)
        {
            Thread.currentThread().interrupted();
            SocketException se = new SocketException("Interrupted - connect()");
            se.initCause(cbie);
            throw se;
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
        catch (Exception e)
        {
            throw new ApplicationError(ErrorCodes.ERROR_UNEXPECTED_EXCEPTION, 
                        "Unexpected exception connect() to socket "+ addr_.getAddress().getHostAddress()+":"+addr_.getPort(), 
                        e, null);
        }

        if (DEBUG_LISTENER && listener_ != null) listener_.debugStep(DDMessageListener.DEBUG_STEP_BEGUN_CONNECTING, 
                                   "establishing connection to " + addr_.getAddress().getHostAddress() +":"+addr_.getPort());
        
        // loop and sleep until connected
        while (!sc_.finishConnect())
        {
            if (nWait > CONNECTTIMEOUT) {
                try {
                    sc_.close();
                } catch (IOException ioe) {}

                throw new SocketTimeoutException("Timeout connecting to " + addr_);
            }
            if (DEBUG_LISTENER && listener_ != null) listener_.debugStep(DDMessageListener.DEBUG_STEP_SLEEP_CONNECTING, 
                                                    "sleeping " + WAITMILLIS + " millis, total slept = " + nWait +
                                                    " ("+CONNECTTIMEOUT+" timeout)");
            Utils.sleepMillis(WAITMILLIS);
            nWait += WAITMILLIS;
        }
    }
    
    /**
     * Send the message - don't wait for a response
     */
    public void send(Peer2PeerMessage msg) throws IOException
    {        
        if (listener_ != null) listener_.updateStep(DDMessageListener.STEP_SENDING);
        msg.write(sc_);
        if (listener_ != null) listener_.updateStep(DDMessageListener.STEP_WAITING_FOR_REPLY);
    }
    
    /**
     * Send the message, wait for response and return it
     */
    public Peer2PeerMessage sendGetReply(Peer2PeerMessage msg) throws IOException
    {   
        send(msg);     
        
        Peer2PeerMessage reply = new Peer2PeerMessage();
        reply.read(sc_);
        
        if (plistener_ != null) plistener_.messageReceived(reply);
        return reply;
    }
    
    /**
     * close the socket
     */
    public void close() throws IOException
    {
        sc_.close();
    }
    
    /** 
     * Return the socket
     */
    public SocketChannel getSocketChannel()
    {
        return sc_;
    }
}
