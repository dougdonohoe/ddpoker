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
 * DDHttpClient.java
 *
 * Created on July 3, 2003, 9:13 AM
 */

package com.donohoedigital.comms;

import com.donohoedigital.base.*;
import com.donohoedigital.base.Base64;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Simple client to use in messaging architecture, handles DNS timeout
 * and other things
 *
 * @author  donohoe
 */
@SuppressWarnings("CommentedOutCode")
public class DDHttpClient
{
    static Logger logger = LogManager.getLogger(DDHttpClient.class);
    
    private static final String CRLF = "\r\n";
    private static final int HEADER_BUFFER = 512;
    
    private static final boolean DEBUG_DETAILS = false;
    private static final boolean DEBUG_LISTENER = false;
    
    // timeout control
    private static final int WAITMILLIS = 100; // time to wait while looping
    private static final int DNSTIMEOUT;
    private static final int CONNECTTIMEOUT;
    private static final int SOCKETTIMEOUT;
    
    /*
     * Create static messenger for default usage
     */
    static 
    {
        SOCKETTIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.http.readtimeout.millis");
        CONNECTTIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.http.connecttimeout.millis");
        DNSTIMEOUT = PropertyConfig.getRequiredIntegerProperty("settings.http.dnstimeout.millis");
        System.getProperties().setProperty("sun.net.client.defaultReadTimeout", Integer.toString(SOCKETTIMEOUT));
        System.getProperties().setProperty("sun.net.client.defaultConnectTimeout", Integer.toString(CONNECTTIMEOUT));
    }
    
    // cache dns lookups
    private static final Map<String, InetSocketAddress> DNSCACHE = Collections.synchronizedMap(new HashMap<>());

    // initialization members
    private InetSocketAddress addr_;
    private final URL url_;
    private final SocketChannel sc_;
    
    // members used during read/write
    private InputStream is_;
    private int nContentLength_ = -1;
    private String sContentType_ = null;
    private int nResponseCode_ = 0;
    private final DDMessageListener listener_;
    private final HttpOptions options_;
    private final DDByteArrayOutputStream buffer_ = new DDByteArrayOutputStream(1000);
    
    /** 
     * Creates a new instance of DDHttpClient 
     */
    public DDHttpClient(URL url, DDMessageListener listener, HttpOptions options) throws IOException
    {
        url_ = url;
        listener_ = listener;
        options_ = options;
        String sHost = url.getHost();
        int nPort = url.getPort();
        if (nPort == -1) nPort = 80;

        if (options_ != null)
        {
            if (options_.bProxyPassThru) {
                nResponseCode_ = 200;
                sContentType_ = "proxy/pass-thru";
                nContentLength_ = 0;
            }
        }

        // cache using host and port since inet address is defined using both
        boolean bFromCache = true;
        String sKey = sHost + ':' + nPort;
        addr_ = DNSCACHE.get(sKey);
        if (addr_ == null)
        {
            bFromCache = false;
            InetAddress addr = LookupHost.getByName(sHost);
            addr_ = new InetSocketAddress(addr, nPort);
            DNSCACHE.put(sKey, addr_);
        }
        if (DEBUG_DETAILS) logger.debug(sHost + " is " + addr_.getAddress().getHostAddress() + (bFromCache ? " (from cache)" : ""));

        sc_ = SocketChannel.open();
        sc_.configureBlocking(false);
        Socket socket = sc_.socket();
        socket.setReuseAddress(true);
        socket.setKeepAlive(false);
        if (Utils.TCPNODELAY) socket.setTcpNoDelay(true);
        socket.setSoTimeout(SOCKETTIMEOUT);
        socket.setSendBufferSize(64 * 1024);
        socket.setReceiveBufferSize(64 * 1024);
        //logger.debug(sHost + " is " + addr_.getAddress().getHostAddress()  +
        //             " send buffer size: " + socket.getSendBufferSize() +
        //             " recv buffer size: " + socket.getReceiveBufferSize());
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
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
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
                        "Unexpected exception connect() to socket "+ addr_.getAddress().getHostAddress()+ ':' +addr_.getPort(),
                        e, null);
        }

        if (DEBUG_LISTENER && listener_ != null) listener_.debugStep(DDMessageListener.DEBUG_STEP_BEGUN_CONNECTING, 
                                   "establishing connection to " + addr_.getAddress().getHostAddress() + ':' +addr_.getPort());
        
        // loop and sleep until connected
        //noinspection DuplicatedCode
        while (!sc_.finishConnect())
        {
            if (nWait > CONNECTTIMEOUT) {
                try {
                    sc_.close();
                } catch (IOException ignored) {}

                throw new SocketTimeoutException("Timeout connecting to " + url_);
            }
            if (DEBUG_LISTENER && listener_ != null) listener_.debugStep(DDMessageListener.DEBUG_STEP_SLEEP_CONNECTING, 
                                                    "sleeping " + WAITMILLIS + " millis, total slept = " + nWait +
                                                    " ("+CONNECTTIMEOUT+" timeout)");
            Utils.sleepMillis(WAITMILLIS);
            nWait += WAITMILLIS;
        }
    }
    
    /**
     * Do the write - this calls the appropriate write method based on the
     * HTTP Options provided and the existence of the writer
     */
    public void write(PostWriter writer, String sPostContentType, String sUserAgent) throws IOException
    {
        boolean bProxyConnect = (options_ != null && options_.sConnectDestViaProxy != null);
        boolean bProxyPassThru = (!bProxyConnect && options_ != null && options_.bProxyPassThru);

        // if we have data to send, either a POST or CONNECT (connect proxy) or
        // just pass through data (regular proxy)
        DDByteArrayOutputStream data;
        
        // post data
        if (writer != null) 
        {
            // write post data to stream so we can get 
            // the size
            DDByteArrayOutputStream post = new DDByteArrayOutputStream(512);
            writer.write(post);
            
            // create data stream based on post size
            data = new DDByteArrayOutputStream(post.size() + HEADER_BUFFER);
            
            // proxy via CONNECT
            if (bProxyConnect)
            {
                writeProxyConnectHeaders(data, options_.sConnectDestViaProxy);
            }
            // proxy pass thru
            else
            {
                if (!bProxyPassThru) {
                    // regular post
                    writeHeaders(data, "POST", sPostContentType, sUserAgent, post.size());
                } // no headers if proxy
            }

            data.write(post.getBuffer(), 0, post.size());
        }
        // normal get
        else
        {
            data = new DDByteArrayOutputStream(HEADER_BUFFER);
            writeHeaders(data, "GET", null, sUserAgent, 0);
        }
        
        // write data out all at once - better for server performance
        if (listener_ != null) listener_.updateStep(DDMessageListener.STEP_SENDING);
        sc_.configureBlocking(true);
        OutputStream os = sc_.socket().getOutputStream();
        os.write(data.getBuffer(), 0, data.size());
        os.flush();
        
        // Not needed with re-rewrite of server code - server now uses
        // Content type, and closing output fails with some proxies
        //sc_.socket().shutdownOutput(); // 
    }
    
    /**
     * Write request to server (header, user agent, content type, post data (nLength bytes of post[])
     */
    public void writeHeaders(OutputStream os, String sMethod, String sContentType, String sUserAgent, int nLength) throws IOException
    {
        StringBuilder sb = new StringBuilder(HEADER_BUFFER).append(sMethod).append(' ');
        String file = url_.getFile();
        if (file == null || file.isEmpty()) file = "/";
        sb.append(file);
        sb.append(" HTTP/1.1").append(CRLF);
        sb.append("host: ").append(url_.getHost()).append(CRLF); // need for http 1.1
        if (sUserAgent != null) sb.append("user-agent: ").append(sUserAgent).append(CRLF);
        //sb.append("Accept: text/html, image/gif, image/jpeg, image/png, *; q=.2, */*; q=.2").append(CRLF);
        if (sContentType != null) sb.append("Content-Type: ").append(sContentType).append(CRLF);
        if (nLength > 0) sb.append("Content-Length: ").append(nLength).append(CRLF);
        if (options_ != null)
        {
            if (options_.sUsername != null && options_.sPassword != null)
            {
                String sEncode = options_.sUsername + ':' + options_.sPassword;
                sEncode = Base64.encodeBytes(sEncode.getBytes());
                sb.append("Authorization: Basic ").append(sEncode).append(CRLF);
            }

            if ((options_.nBeginRange > 0) || (options_.nEndRange > 0))
            {
                sb.append("range: bytes=");
                if (options_.nBeginRange > 0) sb.append(options_.nBeginRange);
                sb.append('-');
                if (options_.nEndRange > 0) sb.append(options_.nBeginRange);
                sb.append(CRLF);
            }
        }
        sb.append(CRLF);
        os.write(Utils.encodeBasic(sb.toString()));
    }
    
    /**
     * Write request to proxy server - CONNECT line plus data to send to
     * proxied host
     */
    public void writeProxyConnectHeaders(OutputStream os, String sDestHostAndPort) throws IOException
    {
        String sb = "CONNECT " + sDestHostAndPort + " HTTP/1.0" + CRLF + CRLF;
        os.write(Utils.encodeBasic(sb));
    }
    
    /**
     * Begin reading the response (up to headers), unless
     * bProxyPassThru is true in HttpOptions, in which case we simply initialize
     * the input stream
     */
    public void startRead() throws IOException
    {
        if (listener_ != null) listener_.updateStep(DDMessageListener.STEP_WAITING_FOR_REPLY);
        is_ = sc_.socket().getInputStream();
        
        // if doing a proxy pass thru, don't process headers
        if (options_ != null && options_.bProxyPassThru) return;
        
        // read a char at a time, looking for headers along the way
        char c;
        int n;
        DDByteArrayOutputStream line = new DDByteArrayOutputStream(50);
        StringTokenizer tok;
        boolean bDone = false;
        boolean bFirstLine = true;
        boolean bFirstChar = true;
        
        while (!bDone)
        {
            // read character
            n = is_.read();
            
            // save for error
            buffer_.write(n);
            
            // if -1, EOF, which is bad
            if (n == -1)
            {
                throw new EOFException("Invalid response from " + url_ + " data received: [" + 
                        Utils.decodeBasic(buffer_.getBuffer(), 0, buffer_.size()) + ']');
            }
            c = (char) n; // we use ISO-8859-1 for sending headers, so we know this is valid            
            //logger.debug("Read " + n + " <"+c+">");
            
            // if first character read, notify listener
            if (listener_ != null && bFirstChar) { listener_.updateStep(DDMessageListener.STEP_RECEIVING); bFirstChar = false; }
            
            // ignore linefeed
            if (c == '\r') continue;
            
            // if carriage return, end of line
            if (c == '\n')
            {
                //logger.debug("READ: " + line);
                // if nothing in line, done reading headers
                if (line.size() == 0) {
                    bDone = true;
                }
                // else process header
                else
                {
                    String sLine = Utils.decodeBasic(line.getBuffer(), 0, line.size());
                    if (DEBUG_DETAILS) logger.debug(sLine);
                    
                    // first line has response code
                    if (bFirstLine)
                    {
                        // first line is status
                        bFirstLine = false;
                        tok = new StringTokenizer(sLine, " \n");
                        tok.nextToken(); // HTTP/1.1
                        
                        try {
                            nResponseCode_ = Integer.parseInt(tok.nextToken());
                        } catch (NumberFormatException nfe)
                        {
                             throw new IOException("Invalid response code from " + url_ + " data received: [" + 
                                        Utils.decodeBasic(buffer_.getBuffer(), 0, buffer_.size()) + ']');
                        }
                    }
                    // other lines have headers - only pick the ones we are interested in
                    else
                    {
                        tok = new StringTokenizer(sLine, " ");
                        String sName = tok.nextToken();
                        if (sName.equalsIgnoreCase("Content-Type:"))
                        {
                            sContentType_ = tok.nextToken();
                        }
                        else if (sName.equalsIgnoreCase("Content-Length:"))
                        {
                            String sNum = tok.nextToken();
                            try {
                                nContentLength_ = Integer.parseInt(sNum);
                            } catch (NumberFormatException ignored) { }
                        }
                    }
                    // clear the line
                    line.reset();
                }
            }
            // if not end of line, just append so we keep line growing
            else
            {
                line.write(n);
            }
        }
    }
    
    /**
     * Get buffer with header data
     */
    public DDByteArrayOutputStream getHeaderBuffer()
    {
        return buffer_;
    }
    
    /**
     * After startWrite(), this returns content length
     */
    public int getContentLength()
    {
        return nContentLength_;
    }
    
    /**
     * After startWrite(), this returns content type
     */
    public String getContentType()
    {
        return sContentType_;
    }
    
    /**
     * After startWrite(), this returns response code
     */
    public int getResponseCode()
    {
        return nResponseCode_;
    }

    /**
     * After startWrite(), this returns input stream for remaining data
     */
    public InputStream getInputStream()
    {
        return is_;
    }
    
    /**
     * close the socket
     */
    public void close() throws IOException
    {
        sc_.close();
    }
    
    /**
     * Class to look up a host in a thread so
     * we can timeout if takes too long
     */
    private static class LookupHost implements Runnable
    {
        String host;
        InetAddress addr = null;
        UnknownHostException uhe = null;
        
        /**
         * Run lookup in sep thread so we can timeout
         */
        public static InetAddress getByName(String sHost) throws UnknownHostException, SocketTimeoutException
        {
            LookupHost h = new LookupHost(sHost);
            Thread t = new Thread(h, "LookupHost - " + sHost);
            t.start();
            try {
                t.join(DNSTIMEOUT);
            }
            catch (InterruptedException ie) {
                //noinspection ResultOfMethodCallIgnored
                Thread.interrupted();
            }
            
            if (t.isAlive())
            {
                t.interrupt();
                throw new DNSTimeoutException("DNS timeout looking up " + sHost);
            }
            
            return h.getAddress();
        }
        
        /**
         * Create new host
         */
        private LookupHost(String host)
        {
            this.host = host;
        }
        
        /**
         * Lookup address, store unknown host exception if it occurs
         */
        public void run() {
            try {
                addr = InetAddress.getByName(host);
            } 
            catch (UnknownHostException e)
            {
                uhe = e;
            }
        }
        
        /*
         * Returns true if address defined or exception occurred
         * Not used, but keep it in case I remember why here in first place
         */
//        private boolean isDone()
//        {
//            return addr != null || uhe != null;
//        }
        
        /**
         * Returns address, or if exception occurred, throws that
         */
        private InetAddress getAddress() throws UnknownHostException
        {
            if (uhe != null) throw uhe;
            
            // unsure that this will ever happen, but I put this here
            // to ensure this method never returns null
            if (addr == null) throw new UnknownHostException("Lookup returned no result for " + host);
            
            return addr;
        }
    }
    
    /**
     * Options for use when connecting to a URL
     */
    @SuppressWarnings({"PublicInnerClass", "PublicField"})
    public static class HttpOptions
    {
        /**
         * Basic auth - username
         */ 
        public String sUsername;
        
        /**
         * Basic auth - password
         */
        public String sPassword;
        
        /**
         * If non-null, set to a host or IP,
         * means the URL given is the proxy
         * and this host is the ultimate destination,
         * via the CONNECT method
         */
        public String sConnectDestViaProxy;
        
        /**
         * If true, the URL given is the proxy and the "post"
         * data is the original get/post with headers, with a modified
         * URI/host (caller is responsible for modifying the URI/host)
         * Note: this and sConnectDestViaProxy are independent; and
         * sConnectDestViaProxy takes precedence.
         * This is basically used to simulate the transparent use
         * of a 3rd party proxy
         */
        public boolean bProxyPassThru;

        /**
         * The begin byte range for the requested data.
         */
        public int nBeginRange;

        /**
         * The end byte range for the requested data.
         */
        public int nEndRange;
    }
}
