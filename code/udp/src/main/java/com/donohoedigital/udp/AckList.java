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

import org.apache.logging.log4j.*;
import com.donohoedigital.base.*;

import java.nio.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 8, 2006
 * Time: 12:15:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class AckList
{
    static Logger logger = LogManager.getLogger(AckList.class);

    // ping ack ids are in a different "space" than regular messages -
    // they are primarily used for debugging - to see how many are
    // coming across.
    private int nAckMessageID_ = 0;

    // session id these messags came in on
    private long sessionID_;

    // first node
    private Ack header_ = null;

    // mod count - used to see if acks have changed
    private boolean bMeter_ = true;
    private long modCount_ = 0;
    private long lastAckSend_;
    private long modCountLastAck_;
    private int nAcksAtThisCnt_;

    /**
     * Default constuctor
     */
    public AckList(long sessionID)
    {
        sessionID_ = sessionID;
        //ackTest(false, 1000, .5f);
    }

    /**
     * set whether or not we should meter the acks going out based on modcount changes
     */
    public void setMeter(boolean b)
    {
        bMeter_ = b;
    }

    /**
     * return session id these acks are for
     */
    public long getSessionID()
    {
        return sessionID_;
    }

    /**
     * Add UDPData's id to the ack list
     */
    void ack(UDPData data)
    {
        ack(data.getID());
    }

    /**
     * Add id to the ack list
     */
    void ack(int nID)
    {
        // first one, create a new ack
        if (header_ == null)
        {
            header_ = new Ack(null, null, nID, nID);
            modCount_++;
            return;
        }

        // look for a place to either add this ID to a range or
        // a place to create a new range
        Ack ack = header_;
        Ack nu = null;
        do
        {
            // if we can include this id in this ack range, do so
            if (ack.include(nID))
            {
                // then see if we can combine this range with an adjacent
                // pass in existing order
                combine(ack.prev, ack);
                combine(ack, ack.next);
                modCount_++;
                break;
            }
            // else see if need to add a new ack range before
            else if (nID < ack.nStart)
            {
                nu = new Ack(ack.prev, ack, nID, nID);
                if (ack.prev != null) ack.prev.next = nu;
                ack.prev = nu;
                if (header_ == ack) header_ = nu;
                modCount_++;
                break;
            }
            // if we get to end, need to add a new ack at end
            else if (ack.next == null)
            {
                nu = new Ack(ack, null, nID, nID);
                ack.next = nu;
                modCount_++;
                break;
            }
            ack = ack.next;
        } while (true);
    }


    /**
     * see if we can combine this ack range with the given ack range
     */
    private boolean combine(Ack one, Ack two)
    {
        // both must be non-null
        if (one == null || two == null) return false;

        // one is prior to two
        if (one.nEnd == two.nStart - 1)
        {
            one.nEnd = two.nEnd;
            if (two.next != null) two.next.prev = one;
            one.next = two.next;
            two.next = null;
            two.prev = null;
            return true;
        }
        return false;
    }

    /**
     * debug
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        Ack ack = header_;
        if (ack == null) sb.append("[empty]");
        else while (ack != null)
        {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ack);
            ack = ack.next;
        }
        return sb.toString();
    }

    /**
     * Return number of nodes
     */
    public int size()
    {
        int nNum = 0;
        Ack ack = header_;
        while (ack != null)
        {
            nNum++;
            ack = ack.next;
        }
        return nNum;
    }

    /**
     * return if given UDPData is in any range
     */
    boolean contains(UDPData data)
    {
        Ack ack = header_;
        while (ack != null)
        {
            if (ack.contains(data.getID()))
            {
                return true;
            }
            ack = ack.next;
        }
        return false;
    }

    private static int SIZEOF_NUM = 4;
    private static int SIZEOF_SESSION_ID = 8;
    private static int SIZEOF_MESSAGE_ID = 4;

    /**
     * queue acks on queue - using as many data's as it takes
     */
    void queueAcks(UDPLink link, UDPData.Type type)
    {
        // skip ack if we haven't changed ack list in set time
        long now = System.currentTimeMillis();
        long diff = now - lastAckSend_;
        if (bMeter_ && modCount_ == modCountLastAck_ && diff < UDPLink.ACK_METER_MILLIS && nAcksAtThisCnt_ > 5) return;

        if (modCount_ == modCountLastAck_)
        {
            nAcksAtThisCnt_++;
        }
        else
        {
            modCountLastAck_ = modCount_;
            nAcksAtThisCnt_ = 1;
        }
        lastAckSend_ = now;

        //logger.debug("Sending ack, mod count = " + modCount_);

        int MAX_BYTES = link.getMaxDataSize() - SIZEOF_NUM - SIZEOF_SESSION_ID;
        int MAX_COUNT = MAX_BYTES / (SIZEOF_MESSAGE_ID * 2);

        int nNum = size();
        Ack ack = header_;
        int nIndex = 0;

        while (nNum > 0)
        {
            int count = Math.min(nNum, MAX_COUNT);
            ByteBuffer buffer = ByteBuffer.allocate(SIZEOF_NUM + SIZEOF_SESSION_ID + (count * SIZEOF_MESSAGE_ID * 2));
            buffer.putLong(sessionID_);
            buffer.putInt(count);

            while (count-- > 0)
            {
                buffer.putInt(ack.nStart).putInt(ack.nEnd);
                ack = ack.next;
                nNum--;
            }
            // insert at front of queue (in order we send) so it gets sent quickly (to minimize resends/timeouts)
            link.queue(new UDPData(type, ++nAckMessageID_, (short) 1, (short) 1,
                               buffer.array(), buffer.arrayOffset(), buffer.position(), UDPData.USER_TYPE_UNSPECIFIED),
                       nIndex++);
        }

    }

    /**
     * Construct from UDPData
     */
    AckList(UDPData data)
    {
        ByteBuffer buffer = ByteBuffer.wrap(data.getData(), data.getOffset(), data.getLength());
        sessionID_ = buffer.getLong();
        int nNum = buffer.getInt();
        Ack ack = null, prev = null;
        for (int i = 0; i < nNum; i++)
        {
            prev = ack;
            ack = new Ack(prev, null, buffer.getInt(), buffer.getInt());
            if (prev == null)
            {
                header_ = ack;
            }
            else
            {
                prev.next = ack;
            }
        }
    }

    /**
     * tracks an range of messages received
     */
    private static class Ack
    {
        // for linked list
        Ack prev;
        Ack next;

        // start/end of ack range
        int nStart;
        int nEnd;

        /**
         * Constructor
         */
        public Ack(Ack prev, Ack next, int nStart, int nEnd)
        {
            this.next = next;
            this.prev = prev;
            this.nStart = nStart;
            this.nEnd = nEnd;
        }

        /**
         * Return if id is in this range
         */
        public boolean contains(int id)
        {
            return (nStart <= id && id <= nEnd);
        }

        /**
         * see if we can include this id in the range
         */
        public boolean include(int id)
        {
            // if inside the range (duplicate)
            if (nStart <= id && id <= nEnd)
            {
                return true;
            }
            // else if one prior to start
            else if (id == (nStart - 1))
            {
                nStart = id;
                return true;
            }
            // else if one after end
            else if (id == (nEnd + 1))
            {
                nEnd = id;
                return true;
            }
            return false;
        }

        public String toString()
        {
            if (nStart == nEnd) return "["+nStart+"]";
            return "[" + nStart + "..." + nEnd + "]";
        }
    }

    ////
    //// TESTING
    ////
    public void ackTest(boolean bShortTest, int size, float iters)
    {
        if (false)
        {
            int acks[] = { 200, 201, 202, 205, 204, 203, 197, 195, 196, 198, 199, 180, 185, 190, 192,
                           193, 195, 194, 183, 196, 197, 201, 184, 182, 189, 186, 188, 187, 181, 191, 182 };

            for (int i = 0; i < acks.length; i++)
            {
                ack(acks[i]);
                logger.debug("Added " + acks[i] + ": "+ this);
            }
        }
        else
        {
            int hits[] = new int[size+1];
            MersenneTwisterFast random_ = new MersenneTwisterFast();
            for (int i = 0; i < size * iters; i++)
            {
                int nNum = random_.nextInt(size) + 1;
                hits[nNum] = 1;
                ack(nNum);
                //logger.debug("Added " + nNum + ": "+ list);
            }
            logger.debug("After " + size +": "+ this);
            StringBuilder missed = new StringBuilder();
            for (int i = 1; i < (size+1); i++)
            {
                if (hits[i] == 0)
                {
                    if (missed.length() > 0) missed.append(", ");
                    missed.append(i);
                }
            }
            logger.debug("Missed: " + missed);
        }
    }
}
