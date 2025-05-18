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
 * Peer2PeerMessage.java
 *
 * Created on November 1, 2004, 10:25 AM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.zip.*;

/**
 *
 * @author  donohoe
 */
public class Peer2PeerMessage implements DDMessageTransporter
{
    static Logger logger = LogManager.getLogger(Peer2PeerMessage.class);
    
    // debug
    private static boolean DEBUG = false;
    
    // timeout
    public static final int READ_TIMEOUT_MILLIS = PropertyConfig.getRequiredIntegerProperty("settings.server.readtimeout.millis");
    public static final int READ_WAIT_MILLIS = PropertyConfig.getRequiredIntegerProperty("settings.server.readwait.millis");
    public static final int WRITE_TIMEOUT_MILLIS = PropertyConfig.getRequiredIntegerProperty("settings.server.writetimeout.millis");
    public static final int WRITE_WAIT_MILLIS = PropertyConfig.getRequiredIntegerProperty("settings.server.writewait.millis");
    
    // other
    private static final int BUFFER_SIZE = 2048;
    
    // types
    public static final int P2P_TEST = 1;
    public static final int P2P_MSG = 2;
    public static final int P2P_REPLY = 3;
    
    // members
    private int nProtocol_;
    private int nType_;
    private DDMessage ddmsg_;
    private boolean bKeepAlive_ = true;
    private String sFromIP_;
    
    /** 
     * Creates a new instance of Peer2PeerMessage 
     */
    public Peer2PeerMessage() {
    }
    
    /**
     * New instance with member data
     */
    public Peer2PeerMessage(int nType, DDMessage msg)
    {
        nProtocol_ = 1;
        nType_ = nType;
        ddmsg_ = msg;
    }
    
    /**
     * Get version - reserved for future usage in case we need
     * to change the protocol
     */
    public int getProtocol()
    {
        return nProtocol_;
    }
    
    /**
     * Get type
     */
    public int getType()
    {
        return nType_;
    }
    
    /**
     * Set type
     */
    public void setType(int n)
    {
        nType_ = n;
    }
    
    /**
     * Get Message
     */
    public DDMessage getMessage()
    {
        return ddmsg_;
    }
    
    /**
     * Set Message
     */
    public void setMessage(DDMessage msg)
    {
        ddmsg_ = msg;
    }
    
    /**
     * Set whether the connection should be kept alive after
     * sending this message (default is true).  Use of false
     * is used when returning a message to a client that is
     * invalid or just sending a test message
     */
    public void setKeepAlive(boolean b)
    {
        bKeepAlive_ = b;
    }
    
    /**
     * Get whether the connection should be kept alive after
     * sending this message
     */
    public boolean isKeepAlive()
    {
        return bKeepAlive_;
    }
    
    /**
     * Set from IP (set by Peer2PeerSocketThread)
     */
    public void setFromIP(String sIP)
    {
        sFromIP_ = sIP;
    }
    
    /**
     * Get from IP
     */
    public String getFromIP()
    {
        return sFromIP_;
    }

    /**
     * write data, return #bytes written
     */
    public int write(SocketChannel channel) throws IOException
    {
        DDByteArrayOutputStream ddmsg = new DDByteArrayOutputStream(BUFFER_SIZE);
        ddmsg_.write(ddmsg);        
        int nSize = ddmsg.size();
        
        // checksum
        CRC32 crc32 = new CRC32();
        
        // write data (byte buffer has extra room)
        ByteBuffer outBuffer = ByteBuffer.allocate(nSize + 44);
        outBuffer.putChar('D');
        putInt(outBuffer, crc32, nProtocol_);
        putInt(outBuffer, crc32, nType_);
        putInt(outBuffer, crc32, nSize);
        outBuffer.putLong(crc32.getValue());
        outBuffer.put(ddmsg.getBuffer(), 0, nSize);
        
        outBuffer.flip();
        int nSleep = 0;
        int nWrote;
        int nToWrite = outBuffer.remaining();
        boolean bDone = false;
        while (!bDone)
        {
            nWrote = channel.write(outBuffer);
            if (nWrote > 0) nSleep = 0;
            
            if (outBuffer.hasRemaining())
            {
                // if already slept too much, we timed out
                if (nSleep >= WRITE_TIMEOUT_MILLIS)
                {
                    throw new SocketTimeoutException("P2P write timeout. Left to write: " + outBuffer.remaining() + " of " + nToWrite +
                                                     " send buffer size: " + channel.socket().getSendBufferSize() +
                                                     " recv buffer size: " + channel.socket().getReceiveBufferSize() +
                                                     " nodelay: "+ channel.socket().getTcpNoDelay() +
                                                     " linger: " + channel.socket().getSoLinger() +
                                                     " alive: "+ channel.socket().getKeepAlive());
                }
                
                nSleep += WRITE_WAIT_MILLIS;
                Utils.sleepMillis(WRITE_WAIT_MILLIS);
            }
            else
            {
                bDone = true;
            }
        }
        return nToWrite;
    }
    
    /**
     * add to buffer and crc at same time
     */
    private void putInt(ByteBuffer outBuffer, CRC32 crc, int n)
    {
        outBuffer.putInt(n);
        crc.update(n);
    }
    
    /**
     * Read data
     */
    public void read(SocketChannel channel) throws IOException
    {
        // int reading
        ByteBuffer intBuffer = ByteBuffer.allocate(4);
        ByteBuffer longBuffer = ByteBuffer.allocate(8);
        ByteBuffer charBuffer = ByteBuffer.allocate(2);
        
        // look for starting char
        char d = readChar(channel, charBuffer);
        if (d != 'D') {
            // throw invalid message error so socket is shutdown
            throw new ApplicationError(ErrorCodes.ERROR_INVALID_MESSAGE, "Invalid message - no starting D", null);
        }
        
        // checksum
        CRC32 crc32 = new CRC32();
                    
        // read ints
        nProtocol_ = readInt(channel, intBuffer, crc32);
        nType_ = readInt(channel, intBuffer, crc32);
        int length = readInt(channel, intBuffer, crc32);
        
        // read checksum and validate
        long crcRead = readLong(channel, longBuffer);
        long crcCalc = crc32.getValue();
        if (crcRead != crcCalc)
        {
            // throw invalid message error so socket is shutdown
            throw new ApplicationError(ErrorCodes.ERROR_INVALID_MESSAGE, "Invalid message - CRC mismatch, msg: " + crcRead +
                                            " != calculated " + crcCalc, null);
        }
        
        // make sure length is reasonable
        if (length > 500000) 
        {
            // throw invalid message error so socket is shutdown
            throw new ApplicationError(ErrorCodes.ERROR_INVALID_MESSAGE, "Message to long: " + length, null);
        }
        
        // data of message
        ByteBuffer dataBuffer = ByteBuffer.allocate(length);
        readBytes(channel, dataBuffer);
        
        // read into buffer
        ddmsg_ = new DDMessage();
        ddmsg_.read(new ByteArrayInputStream(dataBuffer.array()), length);
    }
    
    /**
     * Read 4 bytes into buffer and return the cooresponding int.  Buffer is
     * cleared after reading (so can be reused)
     */
    private int readInt(SocketChannel channel, ByteBuffer buffer, CRC32 crc) throws IOException
    {
        readBytes(channel, buffer);
        buffer.flip();
        int ret = buffer.getInt();
        buffer.clear();
        if (crc != null) crc.update(ret);
        return ret;
    }
    
    /**
     * Read 8 bytes into buffer and return the cooresponding long.  Buffer is
     * cleared after reading (so can be reused)
     */
    private long readLong(SocketChannel channel, ByteBuffer buffer) throws IOException
    {
        readBytes(channel, buffer);
        buffer.flip();
        long ret = buffer.getLong();
        buffer.clear();
        return ret;
    }
    
    /**
     * Read 2 bytes and return corresponding char.  Buffer is
     *cleared after reading (so can be reused)
     */
    private char readChar(SocketChannel channel, ByteBuffer buffer) throws IOException
    {
        readBytes(channel, buffer);
        buffer.flip();
        char ret = buffer.getChar();
        buffer.clear();
        return ret;
    }
    
    /**
     * Read until byte buffer is at capacity
     */
    private void readBytes(SocketChannel channel, ByteBuffer buffer) throws IOException
    { 
        
        // init
        int count;
        int nSleep = 0;
        
        // loop while data available (channel is non-blocking)
        LOOP: while ((count = channel.read(buffer)) >= 0) 
        {
            // if we read data, check out first read for invalid information
            if (count != 0)
            {
                if (DEBUG) logger.debug("Read " + count);
                //logger.debug("Read " + count + ": <" + Utils.decode(buffer_.array(), 0, buffer_.position())+">");
                             
                // see if we are full
                if (buffer.position() == buffer.capacity()) {
                    break LOOP;
                }
                
                nSleep = 0;
            }

            // if still waiting for data, but received none, sleep a little
            if (count == 0)
            {
                // if already slept too much, we timed out
                if (nSleep >= READ_TIMEOUT_MILLIS)
                {
                    throw new SocketTimeoutException("P2P read timeout.  Read " + buffer.position() + " bytes");
                }
                
                nSleep += READ_WAIT_MILLIS;
                if (DEBUG) logger.debug("Sleeping... position is " + buffer.position() + " capacity is " + buffer.capacity());
                Utils.sleepMillis(READ_WAIT_MILLIS);
            }
        }
        
        // if EOF, that is an error (socket should remain open)
        // throw EOF error so socket is shutdown in SocketThread
        if (count == -1)
        {
            throw new EOFException("EOF, read " + buffer.position() + " of " + buffer.capacity());
        }
    }
    
    /**
     * debug
     */
    public String toString()
    {
        String sType;
        switch (nType_)
        {
            case P2P_MSG: sType = "msg"; break;
            case P2P_REPLY: sType = "reply"; break;
            case P2P_TEST: sType = "test"; break;
            default: sType = "[" + nType_ + "]";
        }
        return "P2P type="+sType+"; fromip="+sFromIP_ +"; msg="+ddmsg_;
    }
    
}