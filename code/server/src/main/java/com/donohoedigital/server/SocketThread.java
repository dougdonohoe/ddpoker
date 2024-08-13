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

package com.donohoedigital.server;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;

import javax.servlet.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class SocketThread extends Thread
{
    static Logger logger = Logger.getLogger(SocketThread.class);
    private static boolean DEBUG = false;
    
    protected static final int READ_TIMEOUT_MILLIS = PropertyConfig.getRequiredIntegerProperty("settings.server.readtimeout.millis");
    protected static final int READ_WAIT_MILLIS = PropertyConfig.getRequiredIntegerProperty("settings.server.readwait.millis");
    
    // per request stuff
    protected GameServletResponse response_;
    protected GameServletRequest request_;
    protected SocketChannel channel_;
    protected ByteBuffer headers_ = ByteBuffer.allocate(10000);
    protected ByteBuffer buffer_ = ByteBuffer.allocate(100000);
    
    // initialization stuff
    protected ThreadPool pool_;
    protected BaseServlet servlet_;
    protected String DD_URI_STARTS_WITH;
    private volatile boolean bDone_ = false;

    /**
     * Empty default constructor for use by newInstance()
     */
    public SocketThread ()
    {
    }
    
    /**
     * Called from ThreadPool
     */
    public void init(ThreadPool pool, BaseServlet servlet)
    {
        pool_ = pool;
        servlet_ = servlet;
        // matches Jboss and client.properties entry "settings.online.server"
        DD_URI_STARTS_WITH = '/' + pool.getServer().getAppName() + "/servlet/";
        DD_URI_STARTS_WITH = DD_URI_STARTS_WITH.toLowerCase();
    }
    
    /**
     * Get server we are part of
     */
    public GameServer getServer()
    {
        return pool_.getServer();
    }
    
    /**
     * process data on channel - wakes up wait() in run() below
     */
    synchronized void processChannel(SocketChannel channel)
    {
        // store channel and say we are reading
        channel_ = channel;
        
        // awaken the thread
        notify();		
    }
    
    // loop forever waiting for work to do
    @Override
    public synchronized void run()
    {
        if (GameServer.DEBUG_POOL) logger.info(getName() + " is ready");
        boolean bShutDown;
        
        while (!bDone_) {
            try {
                // sleep and release object lock
                wait();
            } catch (InterruptedException e) {
                logger.warn("Interrupted: " + Utils.formatExceptionText(e));
                // clear interrupt status
                Thread.interrupted();
            }

            // done?
            if (bDone_) continue;

            // check null just in case
            if (channel_ == null) {
                logger.warn(getName() + " AWAKE but channel_ was null");
                continue;	
            }

            // begin processing
            if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " AWAKE for " + Utils.getIPAddress(channel_));
            bShutDown = false;

            // read post and process data
            try {                    
                initRequest();                    

                readData(channel_);
                if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " after read");

                process();
                if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " after process");
            }
            // handle exceptions
            catch (Throwable t)
            {
                bShutDown = handleException(t);
            }
            finally
            {
                // close channel
                try {
                    // keep-alive, re-register channel for read notifications
                    // unless the channel was closed
                    if (!bShutDown && isKeepAlive() && channel_.isOpen())
                    {
                        try {
                            getServer().registerChannel(channel_, SelectionKey.OP_READ);
                        }
                        catch (IOException ioe)
                        {
                            logger.error("registerChannel error: " + Utils.formatExceptionText(ioe));
                        }
                    // else close socket (clears selection keys)
                    } else {
                        closeChannel(channel_);
                    }
                }
                catch (Throwable ignored)
                {
                    logger.warn("Ignored exception: "+ Utils.formatExceptionText(ignored));
                }
                finally
                {
                    // clear channel (not the radio empire)
                    channel_ = null;
            
                    // done, ready for more, return to pool
                    if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " DONE - returning to pool");
                    pool_.returnWorker (this);
                }
            }
        }

        if (GameServer.DEBUG_POOL) logger.info(getName() + " done.");
    }

    /**
     * shutdown
     */
    public synchronized void shutdown()
    {
        bDone_ = true;
        notify();
    }

    /**
     * Close socket
     */
    protected void closeChannel(SocketChannel channel)
    {
        getServer().closeChannel(channel);
    }

    /**
     * Is this a keep alive thread?  Defaults to false; for overriding.
     * Controls whether the socket is closed after processing, or added
     * back to server's selector for futher reading
     */
    protected boolean isKeepAlive()
    {
        return false;
    }
    
    /**
     * Handle an exception - return true if connection should be shutdown
     */
    protected boolean handleException(Throwable t)
    {
        try {
            String sMsg = null;
            int nCode = 503;
            String sRemoteAddr = Utils.getIPAddress(channel_);
            if (t instanceof ApplicationError)
            {
                ApplicationError ae = (ApplicationError) t;
                if (ae.getErrorCode() == ErrorCodes.ERROR_SERVER_FORBIDDEN)
                {
                    nCode = 403;
                    sMsg = Utils.getExceptionMessage(ae);
                    logger.warn(sMsg + ": ["+sRemoteAddr+"] " + ae.getDetails());
                }
                else
                {
                    sMsg = Utils.formatExceptionText(t);
                    logger.warn(Utils.getExceptionMessage(ae) + ": ["+sRemoteAddr+"] " + ae.getDetails());
                }
            }
            else
            {
                sMsg = Utils.formatExceptionText(t);
                logger.warn(t.getClass().getName() + " ["+sRemoteAddr+"];  buffer: " + getBufferAsString() + "\n stacktrace: " + sMsg);
            }
            response_.sendError(nCode, sMsg);
        } 
        catch (Throwable ignored)
        {
            // used to log this, but seems of little value - JDD 6/3/2008
            //logger.error("Exception in handleException: " + Utils.formatExceptionText(ignored) +
            //                "; occurred while handling exception: " + Utils.formatExceptionText(t));
        }
        
        return true;
    }
    
    /**
     * Init to begin new request
     */
    protected void initRequest()
    {
        response_ = new GameServletResponse(channel_);
        request_ = null;
        buffer_.clear();
        headers_.clear();
    }

    /**
     * Read data
     */
    protected void readData(SocketChannel channel) throws IOException
    {
        // init
        int count;
        int nSleep = 0;        
        int nContentRead;
        int nContentExpected;

        // loop while data available (channel is non-blocking)
        while ((count = channel.read(buffer_)) >= 0)
        {
            // if we read data, check out first read for invalid information
            if (count != 0)
            {
                if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " read " + count);
                //logger.debug("Read " + count + ": <" + Utils.decode(buffer_.array(), 0, buffer_.position())+">");

                // if no request yet, attempt to get it
                if (request_ == null)
                {
                    processHeaders();
                    if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " after process headers");
                }

                // if we have a request and read data, see if we have all
                // that we are expecting
                if (request_ != null)
                {
                    nContentExpected = request_.getContentLength();
                    nContentRead = buffer_.position() - headers_.limit();
                    if (nContentRead >= nContentExpected)
                    {
                        break;
                    }
                }

                nSleep = 0;
            }

            // if still waiting for data, but received none, sleep a little
            if (count == 0)
            {
                // if already slept too much, we timed out
                if (nSleep >= READ_TIMEOUT_MILLIS)
                {
                    throw new ApplicationError(ErrorCodes.ERROR_SERVER_IO,
                                               "Read timeout",
                                               getBufferAsString(), null);
                }
                if (buffer_.position() == buffer_.capacity())
                {
                    throw new ApplicationError(ErrorCodes.ERROR_SERVER_IO,
                                               "Buffer full, but data still left to read",
                                               getBufferAsString(), null);
                }

                nSleep += READ_WAIT_MILLIS;
                if (GameServer.DEBUG_ONLINE)
                    logger.debug("Sleeping... position is " + buffer_.position() + " capacity is " + buffer_.capacity());
                Utils.sleepMillis(READ_WAIT_MILLIS);
            }
        }
        
        // at end (EOF or read enough ends loop), shut down input
        channel.socket().shutdownInput();
        
        if (GameServer.DEBUG_ONLINE) logger.debug(getName() + " after shutdowninput");
        
        // make sure we have a request
        if (request_ == null) throw new ApplicationError(ErrorCodes.ERROR_SERVER_FORBIDDEN, "Forbidden (x3)", getBufferAsString(), null);
        
        // validate that the remaining data matches
        // the content length
        nContentExpected = request_.getContentLength();
        buffer_.flip();
        buffer_.position(headers_.limit());
        if (nContentExpected > 0 && buffer_.remaining() != nContentExpected)
        {
            throw new ApplicationError(ErrorCodes.ERROR_SERVER_IO, 
                                            "Content length mismatch; Content-Length ("+nContentExpected+") does not match data read: " +
                                                buffer_.remaining(),
                                            getBufferAsString(),
                                            null);
        }

        // create input stream with remaining data
        InputStream in = new ByteArrayInputStream(buffer_.array(), buffer_.position(), buffer_.remaining());
        request_.setInputStream2(in);
        
        if (DEBUG) logger.debug("Data: <" + Utils.decode(buffer_.array(), buffer_.position(), buffer_.remaining()) + '>');
    }

    /**
     * look for headers
     */
    private void processHeaders()
    {
        int end = buffer_.position();
        char last = '-'; // just not \r or \n
        char c;
        for (int i = 0; i < end; i++)
        {
            c = (char) buffer_.get(i); // we use ISO-8859-1
            if (c == '\r') continue;
            if (c == '\n' && last == '\n')
            {
                headers_.put(buffer_.array(), 0, i + 1);
                createRequest();
                return;
            }
            last = c;
        }
    }
    
    /**
     * parse headers to create request
     */
    private void createRequest()
    {
        // stuff to gather
        String sContentType = null;
        int nContentLength = 0;
        String sURI = null;
        String sMethod = null;
        String sHTTPVersion = null;
        String sRemoteAddr = Utils.getIPAddress(channel_);
        int nServerPort = channel_.socket().getLocalPort();
        
        // start at beginning of headers
        headers_.flip();
        
        // request
        request_ = new GameServletRequest();

        // first line is request
        String sLine = nextHeaderLine();        
        StringTokenizer st = new StringTokenizer(sLine);
        if (st.hasMoreElements()) sMethod = st.nextToken();
        if (st.hasMoreElements()) sURI = st.nextToken();
        if (st.hasMoreElements()) sHTTPVersion = st.nextToken();
        
        if (DEBUG) logger.debug("Request: " + sLine);
        
        // no method or URI is an error
        if (sMethod == null || sURI == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_SERVER_FORBIDDEN, "Forbidden (x1)", getBufferAsString(), null);
        }
        
        // set http version if none provided
        if (sHTTPVersion == null) sHTTPVersion = "HTTP/1.0";
        
        // get headers
        while (true)
        {
            // line
            sLine = nextHeaderLine();
           
            // empty line means we are done
            if (sLine.length() == 0) break;
            
            // parse line
            int n = sLine.indexOf(':');
            if (n == -1 || (n+1) == sLine.length()) continue;

            String sName = sLine.substring(0,n).trim();
            String sValue = sLine.substring(n+1).trim();
            request_.setHeader(sName, sValue);
            if (DEBUG) logger.debug("HEADER name: " + sName + ", value: <" + sValue + '>');

            if (sName.equalsIgnoreCase("Content-Type"))
            {
                sContentType = sValue;
            }
            // content length
            else if (sName.equalsIgnoreCase("Content-Length"))
            {
                try {
                    nContentLength = Integer.parseInt(sValue);
                } catch (NumberFormatException ignored) { }
            }
        }
        
        // init request with all the gathered data
        request_.initRequest(sMethod, sURI, sHTTPVersion, nContentLength, sContentType, sRemoteAddr, nServerPort);
        
        // validate request for DD Messages - must begin with a POST 
        // URI must begin correctly
        // and must have our user agent
        if (servlet_.isDDMessageHandler())
        {
            String sUserAgent = request_.getHeader("user-agent");
            if (sUserAgent == null) sUserAgent = "";
         
            if (!sMethod.equalsIgnoreCase("POST") ||
                !sURI.startsWith(DD_URI_STARTS_WITH) ||
                !sUserAgent.equalsIgnoreCase(DDMessenger.getUSERAGENT()))
            { 
                throw new ApplicationError(ErrorCodes.ERROR_SERVER_FORBIDDEN, "Forbidden (x2)", getBufferAsString(), null);
            }
        }
    }        
   
    /**
     * Return next line in header buffer
     */
    private String nextHeaderLine()
    {
        int position = headers_.position();
        int end = headers_.limit();
        char last = '-';
        char c;
        for (int i = position; i < end; i++)
        {
            c = (char) headers_.get(); // we use ISO-8859-1
            if (c == '\n')
            {
                end = i;
                headers_.position(end+1);
                if (last == '\r') end--;
                break;
            }
            last = c;
        }
        return Utils.decodeBasic(headers_.array(), position, (end - position));
    }
    
    /**
     * process data that was read (in buffer_)
     */
    protected void process() throws IOException, ServletException
    {
        // have servlet process it
        servlet_.doPost(request_, response_);
    }
    
    /**
     * Return contents of buffer as a string for error messages, limit to
     * 3000 bytes
     */
    protected String getBufferAsString()
    {
        return Utils.getBufferAsString(buffer_, 3000);
    }
    
    /**
     ** What the client is expecting (ok example, then error example)
     *
     *

    HTTP/1.1 200 OK
    Date: Thu, 14 Aug 2003 03:48:48 GMT
    Jetty/4.1.3 (Mac OS X 10.2.6 ppc)
    Servlet-Engine: Jetty/4.1.3 (Servlet 2.3; JSP 1.2; java 1.4.1_01)
    Content-Type: application/x-eaglegames-war-msg

    HTTP/1.1 403 Permission+denied+dude%2E
    Date: Thu, 14 Aug 2003 22:10:40 GMT
    Server: Jetty/4.1.3 (Mac OS X 10.2.6 ppc)
    Servlet-Engine: Jetty/4.1.3 (Servlet 2.3; JSP 1.2; java 1.4.1_01)
    Content-Type: text/html
    Content-Length: 1185

     */
}
