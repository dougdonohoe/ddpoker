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
package com.donohoedigital.udp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 8, 2006
 * Time: 7:43:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class IncomingQueue
{
    static Logger logger = LogManager.getLogger(IncomingQueue.class);

    // message comparator
    private static final UDPMessageComparator comparator_ = new UDPMessageComparator();

    // members
    private final ArrayList<UDPData> queue_ = new ArrayList<>();
    private int nLastProcessedID_;
    private final UDPLink link_;

    /**
     * Default constructor
     */
    public IncomingQueue(UDPLink link)
    {
        link_ = link;
    }

    /**
     * clear queue (init)
     */
    public void clear()
    {
        synchronized(queue_)
        {
            queue_.trimToSize();
            queue_.clear();
            nLastProcessedID_ = 0; // messages start at 1 on a new session
        }
    }

    /**
     * size
     */
    public int size()
    {
        synchronized(queue_)
        {
            return queue_.size();
        }
    }

    /**
     * Debug purposes: Is the queue waiting for a
     * message prior to the first message on the queue?
     * If queue is empty, true is returned
     */
    boolean hasGapAtBeginning()
    {
        synchronized(queue_)
        {
            if (queue_.isEmpty()) return false;
            return (queue_.getFirst().getID() - 1 != nLastProcessedID_);
        }
    }

    //
    // QUEUE
    //

    /**
     * Add message to the queue, in sort order.  Return if added
     */
    public boolean addMessage(UDPData data)
    {
        // if already processed, ignore (shouldn't happen, but we
        // double-check even though we are using acks list in UDPLink)
        if (data.getID() <= nLastProcessedID_)
        {
            return false;
        }

        synchronized (queue_)
        {
            int nPlace = Collections.binarySearch(queue_, data, comparator_);
            if (nPlace < 0)
            {
                queue_.add(-(nPlace + 1), data); // insert sorted
                return true;
            }
            else
            {
                // already there - just ignore (shouldn't happen for same reason as above)
                return false;
            }
        }
    }

    /**
     * Sort UDPData by message id
     */
    private static class UDPMessageComparator implements Comparator<UDPData>
    {
        public int compare(UDPData u1, UDPData u2)
        {
            return u1.getID() - u2.getID();
        }
    }

    ////
    //// DISPATCH
    ////

    private final ArrayList<UDPData> process_ = new ArrayList<>(10);

    /**
     * Dispatch messages.  Basically the messages in the queue are sorted
     * by message id.  We start at the top of the queue and look to see if
     * the message at the top is the next message we are expecting.  If it
     * is, we make sure all parts are there (if it is a multipart).  If so,
     * we remove from the queue then, reassemble (if multipart), then
     * dispatch to the handler.  The logic while sync'ing on the queue is
     * short and should be quick, so as not to hold up the UDPManager
     * thread that is putting items on the queue.  Number of messages dispatched
     * is limited to nNum - used to prevent DispatchQueue hogging by any one link.
     * If more messages to dispatch after nNum, return true (so this is added back
     * to DispatchQueue)
     */
    boolean dispatch(boolean bLastDispatch)
    {
        // last dispatch (goodbye received) delivers everything since the link closes after
        int MAX = bLastDispatch ? Integer.MAX_VALUE : 10;

        // dispatch
        try {
            int nParts;
            UDPData data;

            while (true)
            {
                synchronized (queue_)
                {
                    if (queue_.isEmpty()) return false;

                    // first item on queue must be next message in sequence
                    data = queue_.getFirst();
                    int id = data.getID();
                    if (id - 1 != nLastProcessedID_) return false;

                    // look at how many parts this message has
                    nParts = data.getNumParts();

                    // see if queue has enough parts to check
                    if (nParts - 1 > queue_.size() - 1) return false;

                    // if multipart, see if remaining parts are there (last part id must match)
                    // (we used sequential ids)
                    if (nParts > 1 && queue_.get(nParts - 1).getID() != id + (nParts - 1)) return false;

                    // a message to process, but we have processed max, so return true to indicate
                    // further processing needed (except if last dispatch)
                    if (MAX == 0) return !bLastDispatch;

                    // got here so actually remove the elements
                    for (int i = 0; i < nParts; i++)
                    {
                        process_.add(queue_.removeFirst());
                    }
                }

                // process data found (outside sync loop)
                nLastProcessedID_ += process_.size();
                data = process_.removeFirst();
                if (!process_.isEmpty())
                {
                    data.combine(process_);
                    process_.clear();
                }

                // debug
                if (UDPServer.DEBUG_INCOMING)
                {
                    logger.debug("  *** dispatching {}", data.toStringShort());
                }

                // pass completed message on to handlers
                link_.notifyHandlers(data);

                // decrement count and move on
                MAX--;
            }
        }
        // tell link we are finished
        finally
        {
            if (bLastDispatch)
            {
                link_.finish();
            }
        }
    }

    //
    // DEBUG
    //

    /**
     * debug
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        synchronized (queue_)
        {
            for (int i = 0; i < queue_.size(); i++)
            {
                if (i > 0) sb.append(", ");
                sb.append(queue_.get(i).toStringShort());
            }
        }
        return sb.toString();
    }
}
