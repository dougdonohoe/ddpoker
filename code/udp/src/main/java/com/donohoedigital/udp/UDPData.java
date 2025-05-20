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
package com.donohoedigital.udp;

import java.nio.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 2, 2006
 * Time: 10:43:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class UDPData
{
    // data types enum
    public enum Type
    {
        PING_ACK("ping-ack"), MESSAGE("message"), HELLO("hello"), GOODBYE("good-bye"),
        MTU_TEST("mtu-test"), MTU_ACK("mtu-ack");

        // name and constructor for name
        private final String sName;
        private Type(String sName)
        {
            this.sName = sName;
        }
        /**
         * Match for deserializing
         */
        static Type getMatch(int i)
        {
            for (Type t : Type.values())
            {
                if (t.ordinal() == i) return t;
            }
            throw new UnsupportedOperationException("No type for: " + i);
        }

        /**
         * get type name for string
         */
        public String toString()
        {
            return sName;
        }
    }

    // data
    private Type nType_ = Type.MESSAGE;
    private int nID_ = 0;
    private short nPartID_ = 1;
    private short nParts_ = 1;
    private int nLength_;
    private int nOffset_ = 0;
    private byte nUserType_ = 0;
    private byte[] data_;

    // header size
    public static final int HEADER_SIZE = 1 // type (byte)
                                          + 1 // send count (byte)
                                          + 4 // id
                                          + 4 // length
                                          + 2 * 2 // partid, #parts
                                          + 1 // user type
                                          ;

    // max parts - number of short values (since we use short for num parts)
    public static final int MAX_PARTS = Short.MAX_VALUE;

    // user type
    public static final byte USER_TYPE_UNSPECIFIED = -1;

    // control - sent
    private byte nSendCnt_ = 0;

    // control - not sent
    private boolean bSent_ = false;
    private long sentAt_ = 0;
    private boolean bQueued_ = false;

    /**
     * Constructor from byte data
     */
    public UDPData(Type nType, int nID, short nPartID, short nParts, ByteData data, byte nUserType)
    {
        this(nType, nID, nPartID, nParts, data.getBytes(), data.getOffest(), data.getLength(), nUserType);
    }

    /**
     * constructor from byte array
     */
    public UDPData(Type nType, int nID, short nPartID, short nParts, byte[] data, int nOffset, int nLength, byte nUserType)
    {
        nType_ = nType;
        nID_ = nID;
        nPartID_ = nPartID;
        nParts_ = nParts;
        data_ = data;
        nOffset_ = nOffset;
        nLength_ = nLength;
        nUserType_ = nUserType;
    }

    /**
     * Put this data into the given buffer
     */
    public void put(ByteBuffer buffer)
    {
        buffer.put((byte)nType_.ordinal());
        buffer.put(nUserType_);
        buffer.put(nSendCnt_);
        buffer.putInt(nID_);
        buffer.putShort(nPartID_);
        buffer.putShort(nParts_);
        buffer.putInt(nLength_);

        if (nLength_ > 0) buffer.put(data_, nOffset_, nLength_);
    }

    /**
     * Create by reading from buffer
     */
    public UDPData(ByteBuffer buffer)
    {
        nType_ = Type.getMatch(buffer.get());
        nUserType_ = buffer.get();
        nSendCnt_ = (byte) (buffer.get() + 1); // add one since (on sending side) not incremented until after sent
        nID_ = buffer.getInt();
        nPartID_ = buffer.getShort();
        nParts_ = buffer.getShort();
        nLength_ = buffer.getInt();
        if (nLength_ > 0)
        {
            data_ = new byte[nLength_];
            buffer.get(data_);
        }
    }

    /**
     * Combine remaining data chunks with this
     */
    void combine(ArrayList<UDPData> array)
    {        
        // determine total length
        int nuLength = nLength_;
        for (UDPData d : array)
        {
            nuLength += d.nLength_;
            nSendCnt_ = (byte) Math.max(nSendCnt_, d.nSendCnt_); // for debugging/display
        }

        // allocate nu size and copy this UDPData
        byte[] nu = new byte[nuLength];
        System.arraycopy(data_, nOffset_, nu, 0, nLength_);

        // copy UDPData's in array
        int offset = nLength_;
        for (UDPData d : array)
        {
            System.arraycopy(d.data_, d.nOffset_, nu, offset, d.nLength_);
            offset += d.nLength_;
        }

        // remember
        nLength_ = nuLength;
        data_ = nu;
        // nNumParts_ is kept the same so we know how many parts it took to send (again, for debugging/display)
    }

    /**
     * Get length needed to buffer data using put()
     */
    public int getBufferedLength()
    {
        return HEADER_SIZE + nLength_;
    }

    /**
     * get type
     */
    public Type getType()
    {
        return nType_;
    }

    /**
     * Get user type
     */
    public byte getUserType()
    {
        return nUserType_;
    }

    /**
     * Get id
     */
    public int getID()
    {
        return nID_;
    }

    /**
     * Get part id (numbered 1 to N) where N is getNumParts()
     */
    public short getPartID()
    {
        return nPartID_;
    }

    /**
     * Return total number of parts
     */
    public short getNumParts()
    {
        return nParts_;
    }

    /**
     * Get byte data
     */
    public byte[] getData()
    {
        return data_;
    }

    /**
     * Return offset into data
     */
    public int getOffset()
    {
        return nOffset_;
    }

    /**
     * Return length of data
     */
    public int getLength()
    {
        return nLength_;
    }

    /**
     * Has this been sent?
     */
    public boolean isSent()
    {
        return bSent_;
    }

    /**
     * has this been queued?
     */
    public boolean isQueued()
    {
        return bQueued_;
    }

    /**
     * record this was queued
     */
    public void queued()
    {
        bQueued_ = true;
    }

    /**
     * record that this was sent
     */
    public void sent()
    {
        bSent_ = true;
        bQueued_ = false;
        nSendCnt_++;
        sentAt_ = System.currentTimeMillis();
    }

    /**
     * mark this for resend
     */
    public void resend()
    {
        bSent_ = false;
        sentAt_ = 0;
    }
    /**
     * get time elapsed since sent.  if not sent, returns 0
     */
    public long elapsed()
    {
        if (sentAt_ == 0) return 0;
        return System.currentTimeMillis() - sentAt_;
    }

    /**
     * Get send cnt
     */
    public int getSendCount()
    {
        return nSendCnt_;
    }

    /**
     * debug
     */
    public String toString()
    {
        return nType_ + " ["+nID_+"."+nPartID_+"/"+nParts_+"] " + nLength_ + " bytes (sent "+nSendCnt_
                                    +(nSendCnt_ == 1 ? " time" : " times") + ")";
    }

    /**
     * debug
     */
    public String toStringShort()
    {
        return nType_ + " ["+nID_+"."+nPartID_+"/"+nParts_+" :: "+nLength_+ " bytes]";
    }


    /**
     * debug
     */
    public String toStringType()
    {
        return nType_ + " ["+nID_+"]";
    }
}
