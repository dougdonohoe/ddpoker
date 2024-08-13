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
package com.donohoedigital.udp;

import com.donohoedigital.base.*;

import java.nio.*;


/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 2, 2006
 * Time: 2:01:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class UDPID
{
    private static final String UNKNOWN = "????????-????-????-????-????????????";
    public static final UDPID UNKNOWN_ID = new UDPID();
    public static final int LENGTH = 36; // RandomGUID length

    private String sID_;
    private byte[] bytes_;
    private boolean bUnknown_;

    /**
     * Unknown ID
     */
    private UDPID()
    {
        this(UNKNOWN);
    }

    // NOTE:  reading is done single-threaded via UDPServer, so we can share
    // objects for perf for the ByteBuffer constructor
    private static byte[] _data = new byte[LENGTH];
    private static char[] _chars = new char[LENGTH];

    /**
     * load from ByteBuffer
     */
    UDPID(ByteBuffer buffer)
    {
        buffer.get(_data);
        for (int i = 0; i < LENGTH; i++)
        {
            _chars[i] = (char) _data[i];
        }
        setID(new String(_chars)); // String copies chars
    }

    /**
     * Create UPDID
     */
    public UDPID(String sID)
    {
        setID(sID);
    }

    /**
     * set ID, validate
     */
    private void setID(String sID)
    {
        sID_ = sID;
        if (sID_.length() != 36) ApplicationError.assertTrue(false, "UPDID length not 36 ("+sID_.length()+")", sID_);
        bUnknown_ = sID_.equals(UNKNOWN);
        bytes_ = null;
    }

    /**
     * is this Unknown?
     */
    public boolean isUnknown()
    {
        return bUnknown_;
    }

    /**
     * String version
     */
    public String toString()
    {
        return sID_;
    }

    /**
     * byte version
     */
    public byte[] toBytes()
    {
        if (bytes_ == null)
        {
            int nLength = sID_.length();
            bytes_ = new byte[nLength];
            for (int i = 0; i < nLength; i++)
            {
                bytes_[i] = (byte) sID_.charAt(i);
            }
        }
        return bytes_;
    }

    /**
     * Equals
     */
    public boolean equals(Object o)
    {
        if (this == o) return true;
        
        if (o instanceof UDPID)
        {
            return ((UDPID) o).sID_.equals(sID_);
        }
        return false;
    }

    /**
     * hashcode
     */
    public int hashCode()
    {
        return sID_.hashCode();
    }
}
