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
 * UDPMessage.java
 *
 * Created on November 1, 2004, 10:25 AM
 */

package com.donohoedigital.udp;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;

/**
 *
 * @author  donohoe
 */
public class UDPMessage 
{
    static Logger logger = LogManager.getLogger(UDPMessage.class);
    
    // timeout
    private static final int WRITE_TIMEOUT_MILLIS = 1000;
    private static final int WRITE_WAIT_MILLIS = 50;

    // members
    private byte bProtocol_ = 'A'; // reserve for future use
    private long sessionID_;
    private UDPID srcID_;
    private UDPID dstID_;
    private InetSocketAddress srcAddrActual_;
    private InetSocketAddress srcAddrApparent_;
    private InetSocketAddress dstAddr_; // TODO: will this be needed?  In future, could use for UDP tunneling
    private ArrayList<UDPData> data_ = new ArrayList<UDPData>(5);

    // uknown address
    public static final InetSocketAddress ADDRESS_UNKNOWN = new InetSocketAddress("0.0.0.0", 0);
    
    /**
     * New instance with member data
     */
    UDPMessage(UDPServer server, long sessionID, UDPID to, InetSocketAddress src, InetSocketAddress dest)
    {
        sessionID_ = sessionID;
        srcAddrActual_ = src;
        srcAddrApparent_ = ADDRESS_UNKNOWN;
        dstAddr_ = dest;
        srcID_ = server.getID(src);
        dstID_ = to;
    }

    /**
     * Get version - reserved for future usage in case we need
     * to change the protocol
     */
    public byte getProtocol()
    {
        return bProtocol_;
    }

    /**
     * Get session id - used to track if messages coming in are from a new
     * session or an old session
     */
    public long getSessionID()
    {
        return sessionID_;
    }

    /**
     * Get who message is from
     */
    public UDPID getSourceID()
    {
        return srcID_;
    }

    /**
     * Get who message is to
     */
    public UDPID getDestinationID()
    {
        return dstID_;
    }

    /**
     * Get actual source IP (ip of the machine that sent it)
     */
    public InetSocketAddress getSourceIPActual()
    {
        return srcAddrActual_;
    }

    /**
     * Get apparent source IP (ip as it appears to receiver, e.g. after NAT)
     */
    public InetSocketAddress getSourceIPApparent()
    {
        return srcAddrApparent_;
    }

    /**
     * Get destination IP (who it was sent to)
     */
    public InetSocketAddress getDestinationIP()
    {
        return dstAddr_;
    }

    /**
     * Get number of data chunks
     */
    public int getNumData()
    {
        return data_.size();
    }

    /**
     * Get data chunk N
     */
    public UDPData getData(int n)
    {
        return data_.get(n);
    }

    /**
     * Add message data
     */
    void addData(UDPData data)
    {
        data_.add(data);
    }

    /**
     * does this message data contain a GOODBYE message or contain all acks?
     */
    boolean requiresExistingLink()
    {
        int nNonAckCnt = 0;
        UDPData.Type type;
        for (UDPData data : data_)
        {
            type = data.getType();
            if (type == UDPData.Type.GOODBYE) return true;
            if (type != UDPData.Type.PING_ACK && type != UDPData.Type.MTU_ACK) nNonAckCnt++;
        }
        return nNonAckCnt == 0;
    }

    /**
     * Get size to write this message out
     */
    public int getBufferedLength()
    {
        int nSize = HEADER_SIZE;
        int nNumData = data_.size();
        for (int i = 0; i < nNumData; i++)
        {
            nSize += data_.get(i).getBufferedLength();
        }
        return nSize;
    }

    /**
     * Get size of packet this will send in (getBufferredLenght + IP/UDP headers)
     */
    public int getPacketLength()
    {
        return getBufferedLength() + UDPLink.IP_UDP_HEADERS;
    }

    /**
     * can fit data chunk?
     */
    public boolean hasSpace(int nMaxPayloadSize, UDPData data)
    {
        return nMaxPayloadSize - getBufferedLength() >= data.getBufferedLength();
    }

    // crc stuff
    private static final int CRC_SIZE =   8; // long
    private static final byte[] crcExtra = { 8, 6, 7, 5, 3, 0, 9, 55, 39, 1, 10, 31, 19, 68, 5, 27, 20, 01 };

    // header size of our UDPMessage
    public static final int HEADER_SIZE = 1 // protocol (byte)
                                          + 8 // session id
                                          + UDPID.LENGTH * 2 // src/dst
                                          + 4 * 2 // 4 byte address (ip) times two (dst/srcActual)
                                          + 2 * 2 // 2 byte short (port) times two (dst/srcActual)
                                          + 2 // short count of data chunks
                                          + CRC_SIZE // checksum
                                          ;

    /**
     * write data, return #bytes written
     */
    public void write(UDPServer server) throws IOException
    {
        DatagramChannel channel = server.getChannel(srcAddrActual_);

        // allocate buffer
        ByteBuffer outBuffer = ByteBuffer.allocate(getBufferedLength());

        // put header - this should match the HEADER_SIZE definition above
        outBuffer.put(bProtocol_);
        outBuffer.putLong(sessionID_);
        outBuffer.put(srcID_.toBytes());
        outBuffer.put(dstID_.toBytes());
        putAddress(outBuffer, dstAddr_);
        putAddress(outBuffer, srcAddrActual_);
        outBuffer.putShort((short) data_.size());

        // checksum
        CRC32 crc32 = new CRC32();
        crc32.update(outBuffer.array(), outBuffer.arrayOffset(), outBuffer.position());
        crc32.update(crcExtra);
        outBuffer.putLong(crc32.getValue());

        // verify size of header
        if (outBuffer.position() != HEADER_SIZE) ApplicationError.assertTrue(false,
                                                            "Mismatched header size " + outBuffer.position() +
                                                            " != HEADER_SIZE of " + HEADER_SIZE);

        // put data
        for (UDPData data : data_)
        {
            data.put(outBuffer);
        }

        // verify we filled the buffer
        if (outBuffer.capacity() != outBuffer.position()) ApplicationError.assertTrue(false,
                                                                     "Capacity " + outBuffer.capacity() +
                                                                     " != Position " + outBuffer.position());

        // send the buffer
        outBuffer.flip();
        int nSleep = 0;
        boolean bDone = false;
        while (!bDone)
        {
            synchronized (channel)
            {
                channel.send(outBuffer, dstAddr_);
                Utils.sleepMillis(7); // TODO: calc based on user's def of conn (35 = DSL)
            }

            // UDP will either send all or nothing - wait and try again.  This is very unlikely to happen
            if (outBuffer.hasRemaining())
            {
                // if already slept too much, we timed out
                if (nSleep >= WRITE_TIMEOUT_MILLIS)
                {
                    throw new SocketTimeoutException("UDP send timeout. Packet size: " + outBuffer.remaining() +
                                                     " send buffer size: " + channel.socket().getSendBufferSize() +
                                                     " recv buffer size: " + channel.socket().getReceiveBufferSize());
                }

                nSleep += WRITE_WAIT_MILLIS;
                Utils.sleepMillis(WRITE_WAIT_MILLIS);
            }
            else
            {
                bDone = true;
            }
        }

        // mark data as sent
        for (UDPData data : data_)
        {
            data.sent();
        }
    }

    //
    // NOTE:  reading is done single-threaded via UDPServer, so we can share objects for perf
    //
    private static CRC32 _crc32 = new CRC32();
    private static byte[] _addr = new byte[4];

    /**
     * Read data via constructor
     */
    UDPMessage(UDPServer server, ByteBuffer inBuffer, InetSocketAddress to, InetSocketAddress from)
    {
        // validate size - must be at least HEADER_SIZE
        if (inBuffer.limit() < HEADER_SIZE)
        {
            throw new ApplicationError(ErrorCodes.ERROR_INVALID_MESSAGE, "Invalid message from " + Utils.getAddressPort(from) +
                                                                         " - too small.  Expected at least " +
                                                                         HEADER_SIZE + " bytes, received " + inBuffer.limit(), null);
        }

        // calculate checksum
        _crc32.reset();
        _crc32.update(inBuffer.array(), inBuffer.arrayOffset(), HEADER_SIZE - CRC_SIZE); // CRC is on header less CRC at end
        _crc32.update(crcExtra);
        long crcCalc = _crc32.getValue();
        long crcRead = inBuffer.getLong(HEADER_SIZE - CRC_SIZE); // get checksum before processing header

        // validate checksum - used to verify this is a DD message and not some random message
        if (crcRead != crcCalc)
        {
            // throw invalid message error so socket is shutdown
            throw new ApplicationError(ErrorCodes.ERROR_INVALID_MESSAGE, "Invalid message from " + Utils.getAddressPort(from) +
                                                                         " - CRC mismatch. crcRead " + crcRead +
                                                                         " != crcCalc " + crcCalc, null);
        }

        // process header
        bProtocol_ = inBuffer.get();
        sessionID_ = inBuffer.getLong();
        srcID_ = new UDPID(inBuffer);
        dstID_ = new UDPID(inBuffer);
        dstAddr_ = getAddress(inBuffer, _addr);
        dstAddr_ = to;
        // TODO: compare received and read
        srcAddrActual_ = getAddress(inBuffer, _addr);
        srcAddrApparent_ = from;
        short nNumData = inBuffer.getShort();
        crcRead = inBuffer.getLong(); // read again to advance position

        // read data - no need to checksum this data since UDP does its own checksum
        UDPData data;
        for (int i = 0; i < nNumData; i++)
        {
            data = new UDPData(inBuffer);
            addData(data);
        }

        // if the destination ID is not known, then we are the destination
        if (dstID_.isUnknown())
        {
            dstID_ = server.getID(to);
        }
    }

    /**
     * put an address into the buffer
     */
    private void putAddress(ByteBuffer outBuffer, InetSocketAddress addr)
    {
        outBuffer.put(addr.getAddress().getAddress()); // TODO: 2's complement to avoid possible NAT issues?
        outBuffer.putShort((short)addr.getPort());
    }

    /**
     * read an address off the buffer
     */
    private InetSocketAddress getAddress(ByteBuffer inBuffer, byte[] addr)
    {
        int nPort = 0;
        try {
            inBuffer.get(addr);
            nPort = 0xFFFF & inBuffer.getShort(); // deal with ports up to 65535
            return new InetSocketAddress(InetAddress.getByAddress(addr), nPort);
        }
        catch (UnknownHostException uhe) // only thrown if addr incorrect size, but log something just in case
        {
            logger.warn("Error getting address: " + Utils.formatExceptionText(uhe));
            return new InetSocketAddress("0.0.0.0", nPort);
        }
    }

    /**
     * debug
     */
    public String toString()
    {
        int nSize = data_.size();
        return "From: " + srcID_ + " (" + Utils.getAddressPort(srcAddrActual_) +"/"+Utils.getAddressPort(srcAddrApparent_)+")"+
               " to: " + dstID_ + " (" + Utils.getAddressPort(dstAddr_) +") ["+nSize+" "+
               (nSize == 1 ? "part" : "parts") + "]";
    }

    /**
     * debug
     */
    public String toStringIDs()
    {
        int numData = getNumData();
        if (numData == 1) return "" + getData(0).toStringType();
        else
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numData; i++)
            {
                if (i > 0) sb.append(", ");
                sb.append(getData(i).toStringType());
            }
            return sb.toString();
        }
    }

    /**
     * debug
     */
    boolean DEBUG_isSinglePingAck()
    {
        return getNumData() == 1 && (getData(0).getType() == UDPData.Type.PING_ACK || getData(0).getType() == UDPData.Type.MTU_ACK);
    }

    /**
     * to string - all parts
     */
    public void DEBUG_log(UDPLink link)
    {
        for (UDPData data : data_)
        {
            if (data.getType() == UDPData.Type.MTU_TEST) continue;
            logger.debug("  OUT "+data.toStringShort() + " " + link.toStringNameIP());
        }
    }

}
