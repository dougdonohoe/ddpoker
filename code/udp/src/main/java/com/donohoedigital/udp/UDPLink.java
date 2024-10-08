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
import org.apache.logging.log4j.*;

import java.net.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 3, 2006
 * Time: 9:48:43 AM
 * To change this template use File | Settings | File Templates.
 *
 */
public class UDPLink
{
    static Logger logger = LogManager.getLogger(UDPLink.class);

    // default timeout
    public static final int DEFAULT_TIMEOUT = 7500;

    // time to pause between acks when no new data to ack
    static final int ACK_METER_MILLIS = 2500;

     // default time to wait before auto-resend data
    private static final int RESEND_MIN_MILLIS = 1000;
    private static final int GOODBYE_TIMEOUT = 1000;

    // misc
    private static long TBD_REMOTE_ID = -1;

    // members
    private UDPManager manager_;
    private DispatchQueue dispatchQueue_;
    private UDPID id_;
    private InetSocketAddress local_;
    private InetSocketAddress remote_;
    private String sName_;

    // time params from handler
    private int TIMEOUT_MILLIS;
    private int POSSIBLE_TIMEOUT_NOTIFICATION_START;
    private int POSSIBLE_TIMEOUT_NOTIFICATION_INTERVAL;

    // queue to send (synchronized blocks are used around sendQueue_)
    private LinkedList<UDPData> sendQueue_ = new LinkedList<UDPData>();
    private UDPStats stats_ = new UDPStats();

    // session related stuff (set in resetSession() or newSession())
    private int nMessageID_;
    private IncomingQueue incomingQueue_;
    private OutgoingQueue outgoingQueue_;
    private AckList acks_;
    private AckList mtuAcks_;
    private long localSessionID_;
    private long remoteSessionID_;
    private boolean bHelloReceived_;
    private boolean bHelloSent_;
    private long lastMessageReceived_;
    private boolean bGoodbyeInProgress_ = false;
    private boolean bDone_ = false;
    private ArrayList<UDPLinkMonitor> monitors_ = new ArrayList<UDPLinkMonitor>();
    private long start = System.currentTimeMillis();

    // data size related stuff
    public static final int MIN_MTU = 576;
    public static final int MAX_MTU = 1364;
    public static final int IP_UDP_HEADERS = 20 + 8; // 20 bytes for IP header and 8 for UDP header
    public static final int MAX_PAYLOAD_SIZE = MAX_MTU - IP_UDP_HEADERS;
    private int nMTU_ = MIN_MTU;

    /**
     * New link between this server and given addr/id
     */
    public UDPLink(UDPManager manager, UDPID id, InetSocketAddress local, InetSocketAddress remote)
    {
        manager_ = manager;

        dispatchQueue_ = manager_.server().dispatch();
        outgoingQueue_ = manager_.server().outgoing();
        id_ = id;
        local_ = local;
        remote_ = remote;
        sName_ = Utils.getAddressPort(remote);

        UDPLinkHandler handler = manager.handler();
        TIMEOUT_MILLIS = handler.getTimeout(this);
        POSSIBLE_TIMEOUT_NOTIFICATION_START = handler.getPossibleTimeoutNotificationStart(this);
        POSSIBLE_TIMEOUT_NOTIFICATION_INTERVAL = handler.getPossibleTimeoutNotificationInterval(this);

        resetSession();
    }

    /**
     * finish - cleanup and notify manager
     */
    void finish()
    {
        finish(true);
    }

    /**
     * finish - cleanup, notify manager if specified
     */
    void finish(boolean bNotifyManager)
    {
        if (bDone_) return; // don't run finish multiple times

        bDone_ = true;

        synchronized (sendQueue_)
        {
            sendQueue_.clear();
        }

        // notify listeners we are done
        fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.CLOSED, this));

        // notify manager unless if required (set to false when called *from* manager)
        if (bNotifyManager) manager_.remove(this);
    }

    /**
     * done?
     */
    public boolean isDone()
    {
        return bDone_;
    }

    /**
     * Get ID
     */
    public UDPID getID()
    {
        return id_;
    }

    /**
     * Set ID
     */
    void setID(UDPID id)
    {
        id_= id;
    }

    /**
     * Get IP
     */
    public InetSocketAddress getLocalIP()
    {
        return local_;
    }

    /**
     * Set IP
     */
    void setLocalIP(InetSocketAddress addr)
    {
        local_ = addr;
    }

    /**
     * Get IP
     */
    public InetSocketAddress getRemoteIP()
    {
        return remote_;
    }

    /**
     * Set IP
     */
    void setRemoteIP(InetSocketAddress addr)
    {
        remote_ = addr;
    }

    /**
     * get name
     */
    public String getName()
    {
        return sName_;
    }

    /**
     * Set name
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }

    /**
     * Get time connected
     */
    public long getTimeConnected()
    {
        return System.currentTimeMillis() - start;
    }

    /**
     * Add a monitor
     */
    public void addMonitor(UDPLinkMonitor monitor)
    {
        synchronized(monitors_)
        {
            if (monitors_.contains(monitor)) return;
            monitors_.add(monitor);
        }
    }

    /**
     * remove a monitor
     */
    public void removeMonitor(UDPLinkMonitor monitor)
    {
        synchronized(monitors_)
        {
            monitors_.remove(monitor);
        }
    }

    /**
     * fire event
     */
    private void fireEvent(UDPLinkEvent event)
    {
        // copy to avoid deadlock situations
        UDPLinkMonitor[] mons = null;
        UDPLinkMonitor mon = null;
        synchronized(monitors_)
        {
            // do nothing if no monitors
            int nNum = monitors_.size();
            if (nNum == 0) return;

            // only one, so avoid array alloc
            if (nNum == 1)
            {
                mon = monitors_.get(0);
            }
            // multiple
            else
            {
                mons = new UDPLinkMonitor[nNum];
                monitors_.toArray(mons);
            }
        }

        // handle case of one
        if (mon != null)
        {
            fireEvent(mon, event);
        }
        // handle case of multiple
        else
        {
            for (UDPLinkMonitor monitor : mons)
            {
                fireEvent(monitor, event);
            }
        }
    }

    /**
     * fire event
     */
    private void fireEvent(UDPLinkMonitor monitor, UDPLinkEvent event)
    {
        // notify hanlder of new message
        try {
            monitor.monitorEvent(event);
        } catch (Throwable t)
        {
            logger.error("Monitor error on event "+event+ ": "+ Utils.formatExceptionText(t));
        }
    }

    /////
    ///// OUTGOING METHODS
    /////

    /**
     * Get MTU
     */
    public int getMTU()
    {
        return nMTU_;
    }

    /**
     * max payload size
     */
    public int getMaxPayloadSize()
    {
        return nMTU_ - IP_UDP_HEADERS;
    }

    /**
     * max message size
     */
    public int getMaxMessageSize()
    {
        return getMaxPayloadSize() - UDPMessage.HEADER_SIZE;
    }

    /**
     * max data size
     */
    public int getMaxDataSize()
    {
        return getMaxMessageSize() - UDPData.HEADER_SIZE;
    }

    // mtu test num (used to identify new test on receiving end)
    private byte testNum = 0;
    private int nLastMTUTest_;
    private boolean bMTUTestDone_;

    /**
     * Send varying MTU messages to discover the largest possible
     */
    public void mtuPathDiscovery()
    {
        bMTUTestDone_  = false;

        // test from MIN_MTU up to our MAX_MTU
        int headers = IP_UDP_HEADERS + UDPMessage.HEADER_SIZE + UDPData.HEADER_SIZE;
        int maxPayload = MAX_MTU - headers;
        int minPayload = MIN_MTU - headers;
        int FACTOR = 128;
        int FUDGE = (MAX_MTU - MIN_MTU) % FACTOR;
        byte data[] = new byte[maxPayload];
        Arrays.fill(data, (byte) 'd');
        int id = 0;

        // do steps of FACTOR bytes
        for (int i = minPayload; i <= maxPayload;)
        {
            // id is total size of data
            id = i + headers;

            // queue it
            queue(new UDPData(UDPData.Type.MTU_TEST, id, (short) 1, (short) 1, data, 0, i, testNum));

            // increment differently based on proximity to max
            if (i < (maxPayload - FUDGE)) i += FACTOR;
            else i += FUDGE;
        }

        nLastMTUTest_ = id;
    }

    /**
     * Is this last MTU test data?
     */
    private boolean isLastMTU(UDPData data)
    {
        return data.getID() == nLastMTUTest_;
    }

    /**
     * last mtu
     */
    private void mtuTestDone()
    {
        if (UDPServer.DEBUG_MTU)
        {
            logger.debug("  MTU done, set to " + nMTU_ + " bytes " + toStringNameIP());
        }
        bMTUTestDone_ = true;
        fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.MTU_TEST_FINISHED, this));
    }

    /**
     * Queue a hello message
     */
    private boolean hello()
    {
        if (!bHelloSent_)
        {
            queue(UDPData.Type.HELLO);
            mtuPathDiscovery();
            bHelloSent_ = true;
            return true;
        }
        return false;
    }

    /**
     * Tell link to shutdown
     */
    public void close()
    {
        queue(UDPData.Type.GOODBYE); // send goodbye to remote
        manager_.addLinkToSend(this); // send now
        if (!bGoodbyeInProgress_)
        {
            // notify listeners we are done
            fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.CLOSING, this));
        }
        bGoodbyeInProgress_ = true;
    }

    /**
     * force close - no goodbye message (testing use)
     */
    public void kill()
    {
        finish(true);
    }

    /**
     * Queue a message of the given type (no data)
     */
    public void queue(UDPData.Type type)
    {
        queue(type, null, 0, 0, UDPData.USER_TYPE_UNSPECIFIED);
    }

    /**
     * Queue a message of type UDPData.Type.MESSAGE
     */
    public void queue(byte[] data)
    {
        queue(UDPData.Type.MESSAGE, data, 0, data == null ? 0 : data.length, UDPData.USER_TYPE_UNSPECIFIED);
    }

    /**
     * Queue a message of type UDPData.Type.MESSAGE
     */
    public void queue(ByteData bd)
    {
        queue(bd, UDPData.USER_TYPE_UNSPECIFIED);
    }

    /**
     * Queue a message of type UDPData.Type.MESSAGE
     */
    public void queue(ByteData bd, byte nUserType)
    {
        queue(UDPData.Type.MESSAGE, bd.getBytes(), bd.getOffest(), bd.getLength(), nUserType);
    }

    /**
     * Queue a message of type UDPData.Type.MESSAGE
     */
    public void queue(byte[] data, int offset, int length)
    {
        queue(UDPData.Type.MESSAGE, data, offset, length, UDPData.USER_TYPE_UNSPECIFIED);
    }

    /**
     * Queue a message of type UDPData.Type.MESSAGE
     */
    public void queue(byte[] data, int offset, int length, byte nUserType)
    {
        queue(UDPData.Type.MESSAGE, data, offset, length, nUserType);
    }

    /**
     * Queue a message of the given type with given data at specifyied offset and length.
     * Need synchronized due to calls to nextID()
     */
    public synchronized void queue(UDPData.Type type, byte[] data, int offset, int length, byte nUserType)
    {
        UDPData part;

        // no more messages if good-bye in progress (check here also to avoid alloc new objects)
        if (bGoodbyeInProgress_) return;

        if (length == 0)
        {
            part = new UDPData(type, nextID(), (short) 1, (short) 1, null, 0, 0, nUserType);
            queue(part);
        }
        else
        {
            short nPartID = 0;
            int partlength;
            int MAX_DATA_SIZE = getMaxDataSize();
            short nParts = (short) (length / MAX_DATA_SIZE);
            if (length % MAX_DATA_SIZE != 0) nParts++;

            // unlikely to happen, but always safe to check.  This allows
            // max size of message (UDPMessage.DATA_SIZE - UDPData.HEADER_SIZE) * Short.MAX_VALUE,
            // or approx. 44,169,916 bytes.
            if (nParts > UDPData.MAX_PARTS)
            {
                ApplicationError.assertTrue(false, "Data too big to send: " + length + " bytes (max: " +
                                                   (MAX_DATA_SIZE * UDPData.MAX_PARTS) + " bytes at " +
                                                   MAX_DATA_SIZE + " per chunk ... MTU="+getMTU());
            }

            // Each UDPdata has its own id.  A multi part messsage is made
            // up of sequential ids.  The nPartID of nParts is used to identify
            // these parts.
            for (int i = offset; i < length; i += MAX_DATA_SIZE)
            {
                nPartID++;  // part IDs start at 1
                partlength = Math.min(MAX_DATA_SIZE, length + offset - i);
                part = new UDPData(type, nextID(), nPartID, nParts, data, i, partlength, nUserType);
                queue(part);
            }

            if (nPartID != nParts)
            {
                ApplicationError.assertTrue(false, "# parts mismatch - expected " + nParts + " but got " +
                                                   nPartID + " for length " + length + " and MAX_DATA_SIZE " +
                                                   MAX_DATA_SIZE);
            }
        }
    }

    /**
     * next id - IDs start at 1.  Synchronized above.
     */
    private int nextID()
    {
        nMessageID_++;
        if (nMessageID_ == 0)
        {
            logger.warn("Message ID rolled over"); // TODO - what to do when ID rolls over?
        }
        return nMessageID_;
    }

    /**
     * queue a message part at end
     */
    void queue(UDPData part)
    {
        queue(part, -1);
    }

    /**
     * Queue a message part at given index, or if -1, at end
     */
    void queue(UDPData part, int nIndex)
    {
        // no more messages if good-bye in progress
        if (bGoodbyeInProgress_) return;

        // queue it
        synchronized(sendQueue_)
        {
            if (nIndex == -1)
            {
                sendQueue_.add(part);
            }
            else
            {
                sendQueue_.add(nIndex, part);
            }
        }

        // stats
        if (part.getType() != UDPData.Type.PING_ACK && part.getType() != UDPData.Type.MTU_ACK) stats_.recordDataOut();
    }

    /**
     * Queue size (total of sent (non-acked) and to-be-sent messages
     */
    public int queueSize()
    {
        synchronized(sendQueue_)
        {
            return sendQueue_.size();
        }
    }

    /**
     * Does the following things:
     *
     * 1) Adds acks to the queue
     * 2) check for resends - any message not ack'd in reasonable time is marked for resending
     * 3) sends all queued messages (acks, resends, new messages)
     */
    void sendAll()
    {
        if (bDone_) return;

        sendAcks(false);

        // expect a response back in RESEND_MIN_MILLIS milliseconds as a basis,
        // or 125% the moving average, whichever is larger
        long minAck = Math.max(RESEND_MIN_MILLIS, (long) (stats_.getAverage() * 1.25));
        minAck = Math.min(minAck, RESEND_MIN_MILLIS * 7);

        synchronized(sendQueue_)
        {
            UDPData data;
            Iterator<UDPData> iter = sendQueue_.iterator();
            UDPData.Type type;

            // absolute max sends is 128 (since we store as a byte)
            int mtuMaxAttempts = 3;
            int msgMaxAttempts = 25;
            int maxResends = 5;
            int resendCNT = 0;

            LOOP: while (iter.hasNext() && resendCNT < maxResends)
            {
                data = iter.next();
                type = data.getType();
                if (data.elapsed() > minAck)
                {
                    switch (type)
                    {
                        case MTU_TEST:
                            if (data.getSendCount() == mtuMaxAttempts)
                            {
                                if (UDPServer.DEBUG_MTU)
                                {
                                    logger.debug("  MTU failed - " + data.getID() + " bytes (attempted "+mtuMaxAttempts+" times) " + toStringNameIP());
                                }

                                iter.remove();

                                if (isLastMTU(data))
                                {
                                    mtuTestDone();
                                }
                                continue;
                            }
                            break;

                       default:
                            if (data.getSendCount() == msgMaxAttempts)
                            {
                                if (UDPServer.DEBUG_RESEND)
                                {
                                    logger.debug("  XXXX RESEND FAIL " + data + " " + toStringNameIP());
                                }

                                iter.remove();

                                fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.RESEND_FAILURE, this, data));
                                close();
                                break LOOP;
                            }
                            break;
                    }

                    if (UDPServer.DEBUG_RESEND)
                    {
                        logger.debug("  **** RESEND " + data.elapsed() + " ms (min: " + minAck +"): " + data + " " + toStringNameIP());
                    }

                    data.resend();
                    stats_.recordDataResend();
                    resendCNT++;
                }
            }
        }

        send();
    }

    /**
     * send acks/ping out
     */
    void sendAcksPing()
    {
        // send acks out if connection still alive
        if (aliveCheck())
        {
            sendAcks(true);
        }
    }

    /**
     * send acks out
     */
    private void sendAcks(boolean bSendImmediate)
    {
        if (acks_ == null || bGoodbyeInProgress_ || bDone_) return;

        // send out acks (unless it is empty)
        if (UDPServer.DEBUG_ACKS_OUT) logger.debug("ACKS out: "+ acks_);
        acks_.queueAcks(this, UDPData.Type.PING_ACK);
        if (bSendImmediate) send();
    }

    /**
     * Send everything in queue, fitting as many into each message as possible
     */
    public void send()
    {
        synchronized(sendQueue_)
        {
            int nNum = sendQueue_.size();
            if (nNum == 0) return;

            UDPMessage msg = null;
            UDPData data;
            Iterator<UDPData> iter = sendQueue_.iterator();
            boolean bLimitOne = false;
            UDPData.Type type;

            ITER: while (iter.hasNext())
            {
                data = iter.next();
                type = data.getType();

                // if we are done, exit now (so we don't waste time sending)
                if (bDone_) return;

                // already queued or sent, but not ack'd
                if (data.isSent() || data.isQueued()) continue;

                // if this is a message and we haven't finished the mtu test, skip for now
                if (type == UDPData.Type.MESSAGE && !bMTUTestDone_)
                {
                    if (UDPServer.DEBUG_MTU)
                    {
                        //logger.debug("  SKP (mtu test not done): " + data);
                    }
                    continue;
                }

                // if we have a message, but no space for this chunk,
                // (or this chunk or previous is a MTU_TEST message)
                // then send current message (and start new message)
                if (msg != null && (type == UDPData.Type.MTU_TEST || type == UDPData.Type.MTU_ACK || bLimitOne ||
                                    !msg.hasSpace(getMaxPayloadSize(), data)))
                {
                    outgoingQueue_.addSend(this, msg);
                    msg = null;
                    bLimitOne = false;
                }

                // no message (or starting new one), so create message
                if (msg == null)
                {
                    msg = new UDPMessage(manager_.server(), localSessionID_, id_, local_, remote_);
                }

                // add the data
                msg.addData(data);
                data.queued();

                // remove ping/acks here. Even though there is
                // a small possibility they won't be received, that is
                // okay since we'll send them again
                if (type == UDPData.Type.PING_ACK || type == UDPData.Type.MTU_ACK)
                {
                    iter.remove();
                }
                // if MTU_TEST or MTU_ACK, only allow one per message
                else if (type == UDPData.Type.MTU_TEST || type == UDPData.Type.MTU_ACK )
                {
                    bLimitOne = true;
                }
            }

            // if we have a message, send it
            if (msg != null)
            {
                outgoingQueue_.addSend(this, msg);
            }
        }
    }

    // error handling
    private String sLastError_ = null;
    private int nLastErrorCnt_ = 0;

    /**
     * Send message on channel (called from OutgoingQueue only)
     */
    void send(UDPMessage msg)
    {
        try {
            stats_.recordPacketSent();
            stats_.recordBytesOut(msg.getPacketLength());

            // debug
            //logger.debug("Sending " + msg.toStringIDs());
            if (UDPServer.DEBUG_OUTGOING && !msg.DEBUG_isSinglePingAck())
            {
                //logger.debug("Sending to " + Utils.getAddressPort(getRemoteIP()) + " ["+ msg.getNumData() +"]:");
                msg.DEBUG_log(this);
            }

            // send message
            msg.write(manager_.server());

            // if we had previous errors, note total that occurred and clear cnt/error
            if (nLastErrorCnt_ > 0)
            {
                logger.info(Utils.getAddressPort(local_)+" send successful.  Last send error ("+sLastError_+") occurred a total of "+(nLastErrorCnt_+1)+" times");
            }
            nLastErrorCnt_ = 0;
            sLastError_ = null;
        }
        catch (Throwable ioe)
        {
            stats_.recordPacketSendError();

            // get message portion to see if we are repeating
            String sError = Utils.getExceptionMessage(ioe);
            if (sError.equals(sLastError_))
            {
                nLastErrorCnt_++;
                if (nLastErrorCnt_ % 10 == 0)
                {
                    logger.error(Utils.getAddressPort(local_)+ " last send error ("+sError+") repeated "+nLastErrorCnt_+" times");
                }
                return;
            }

            // new error - log it and remember it
            sLastError_ = sError;
            logger.error(Utils.getAddressPort(local_)+" send error " + Utils.formatExceptionText(ioe));
        }
    }

    /////
    ///// INCOMING METHODS
    /////

    private long lastNotification_;

    /**
     * Alive check.  Return true if 'alive'.  Otherwise, notify.
     */
    private boolean aliveCheck()
    {
        // if haven't sent or received message out yet, skip
        if ((nMessageID_ == 0 && acks_ == null) || bDone_) return false;

        // first time through, set to current time
        long now = System.currentTimeMillis();
        if (lastMessageReceived_ == 0) lastMessageReceived_ = now;

        long elapsed = now - lastMessageReceived_;

        // if good bye is in progress and no ack, then just exit
        if (bGoodbyeInProgress_ && elapsed > GOODBYE_TIMEOUT) // TODO: does this work?
        {
            finish(); // sender finish (no ack from remote)
            return false;
        }

        if (elapsed > TIMEOUT_MILLIS)
        {
            if (UDPServer.DEBUG_TIMEOUT) logger.debug("Timeout after " + elapsed + " millis on " + Utils.getAddressPort(remote_) + " (new session "+localSessionID_+")");
            fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.TIMEOUT, this, elapsed));
            resetSession();
            finish();
            return false;
        }
        else if (elapsed > POSSIBLE_TIMEOUT_NOTIFICATION_START)
        {
            long elapsedNot = now - lastNotification_;
            if (elapsedNot > POSSIBLE_TIMEOUT_NOTIFICATION_INTERVAL)
            {
                fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.POSSIBLE_TIMEOUT, this, elapsed));
                lastNotification_ = now;
            }
        }
        return true;
    }

    /**
     * process message on this link
     */
    void processMessage(UDPMessage msg)
    {
        if (bDone_) return;

        // stats
        stats_.recordPacketReceived();
        stats_.recordBytesIn(msg.getPacketLength());

        // note time
        lastMessageReceived_ = System.currentTimeMillis();

        // check for new session
        boolean bNewSession = false;
        long sessionID = msg.getSessionID();

        // message from old session - ignore
        if (sessionID < remoteSessionID_)
        {
            return;
        }
        // new session on remote side, note this
        else if (sessionID > remoteSessionID_)
        {
            bNewSession = true;
            bHelloReceived_ = false;
        }

        // debug
        //if (UDPServer.DEBUG_INCOMING && !msg.DEBUG_isSinglePingAck()) logger.debug("Incoming on " + this + " ["+ msg.getNumData() +"] ");

        // process all data chunks
        boolean bLastDispatch = false;
        boolean bDispatch = false;
        UDPData data;
        UDPData.Type type;
        int nNum = msg.getNumData();
        LOOP: for (int i = 0; i < nNum; i++)
        {
            data = msg.getData(i);
            type = data.getType();
            if (type != UDPData.Type.PING_ACK && type != UDPData.Type.MTU_ACK) stats_.recordDataIn();

            switch (type)
            {
                case HELLO:
                    bHelloReceived_ = true;
                    // if session changed, create new session on first
                    // HELLO UDPData that comes through
                    if (bNewSession)
                    {
                        newRemoteSession(sessionID);
                        bNewSession = false;
                    }
                    // FALL THROUGH TO MESSAGE

                case MESSAGE:

                    if (!bHelloReceived_) break;

                    // Attempt to add to queue if HELLO message received and we haven't already
                    // added it (by checking acks).  If we added, then update acklist and
                    // mark for dispatching
                    if (!acks_.contains(data) && incomingQueue_.addMessage(data))
                    {
                        if (UDPServer.DEBUG_INCOMING)
                        {
                            logger.debug("  IN  " + data + " (" + incomingQueue_.size() +" queue) " + toStringNameIP());
                            if (UDPServer.DEBUG_INCOMING_QUEUE || incomingQueue_.hasGapAtBeginning()) logger.debug("Queue now " + toStringNameIP() +": "+ incomingQueue_);
                        }

                        acks_.ack(data);
                        bDispatch = true;
                    }
                    // otherwise it is a duplicate, so ignore it
                    else
                    {
                        if (UDPServer.DEBUG_INCOMING)
                        {
                            //logger.debug("  <== " + data + " (dup-ignored)");
                        }
                        stats_.recordDataDups();
                    }
                    break;

                case GOODBYE:
                    // debug
                    if (UDPServer.DEBUG_INCOMING)
                    {
                        logger.debug("  IN  " + data + " (" + incomingQueue_.size() +" queue) " + toStringNameIP());
                        if (UDPServer.DEBUG_INCOMING_QUEUE || incomingQueue_.hasGapAtBeginning()) logger.debug("Queue now " + toStringNameIP() +": "+ incomingQueue_);
                    }

                    // send final acks (could be null if duplicate goodbye received)
                    if (acks_ != null) acks_.ack(data);
                    sendAcks(true);
                    bLastDispatch = true;

                    // ignore data after this one, but continue to allow final dispatch
                    break LOOP;

                case PING_ACK:
                    processAcks(data, false);
                    break;

                case MTU_TEST:
                    if (!bHelloReceived_) break;

                    // if no mtu acks list, or the test id changed, then new ack list
                    byte tstid = data.getUserType();
                    if (mtuAcks_ == null || mtuAcks_.getSessionID() != tstid)
                    {
                        if (UDPServer.DEBUG_MTU)
                        {
                            //logger.debug("  TST-"+tstid+" starting");
                        }
                        mtuAcks_ = new AckList(tstid);
                        mtuAcks_.setMeter(false);
                    }

                    // if this is a new test message, ack it and log it
                    if (!mtuAcks_.contains(data))
                    {
                        if (UDPServer.DEBUG_MTU)
                        {
                            //logger.debug("  TST-"+tstid+" " + data + " (" + msg.getPacketLength() +" pkt size) " + toStringIPs());
                        }

                        mtuAcks_.ack(data);
                    }
                    // otherwise it is a duplicate, so ignore it
                    else
                    {
                        if (UDPServer.DEBUG_MTU)
                        {
                            //logger.debug("  TST-"+tstid+" " + data + " (duplicate) " + toStringIPs());
                        }
                        stats_.recordDataDups();
                    }

                    // always send ack in case a duplicate message meant ack wasn't received
                    mtuAcks_.queueAcks(this, UDPData.Type.MTU_ACK);
                    send();
                    break;

                case MTU_ACK:
                    processAcks(data, true);
                    break;

            }
        }

        // dispatch new messages
        if (bDispatch || bLastDispatch)
        {
            dispatchQueue_.addQueue(incomingQueue_, bLastDispatch);
        }
    }

    /**
     * new local session
     */
    private void resetSession()
    {
        bHelloReceived_ = false;
        lastMessageReceived_ = 0;
        remoteSessionID_ = TBD_REMOTE_ID;
        acks_ = null;
        mtuAcks_ = null;
        incomingQueue_ = null;

        newLocalSession();
    }

    /**
     * reset to reflect new session on our end (discard previous messages)
     */
    private void newLocalSession()
    {
        //  + start new local session so old messages are ignored on remote
        //  + reset message id to 0
        //  + clear outgoing messages (since message id reset)
        //  + clear round trip stats (new session/new stats)
        localSessionID_ = System.currentTimeMillis();
        nMessageID_ = 0;
        synchronized(sendQueue_) { sendQueue_.clear(); }
        stats_.clear();
        bHelloSent_ = false;
        nMTU_ = MIN_MTU;
        bMTUTestDone_ = false;
    }

    /**
     * New session - need to clear all queues since message ids
     * are now different
     */
    private void newRemoteSession(long newSessionID)
    {
        boolean bFirstMessageFromRemote = remoteSessionID_ == TBD_REMOTE_ID;
        if (!bFirstMessageFromRemote)
        {
            //logger.info("Remote session changed from " + remoteSessionID_ + " to " + newSessionID + " on " + this);
            fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.SESSION_CHANGED, this));
        }

        // remember new id
        remoteSessionID_ = newSessionID;

        //  + clear acks (since message ids reset)
        //  + clear incoming queue (old messages invalid)
        acks_ = new AckList(remoteSessionID_);
        mtuAcks_ = null;
        incomingQueue_ = new IncomingQueue(this);

        // if this wasn't the first message from the remote, then we have
        // communicated with them before.  Since they changed their session,
        // that means any outgoing messages we have are invalid.  Thus,
        // create a new session id and throw away all messages.
        if (!bFirstMessageFromRemote)
        {
            newLocalSession();
        }

        // finally, send hello message out now to firmly establish new connection
        connect();
    }

    /**
     * initiate session
     */
    public void connect()
    {
        hello();
        send();
    }

    /**
     * process received acks
     */
    private void processAcks(UDPData data, boolean bMTUAcks)
    {
        String ACK = bMTUAcks ? "MTUACK" : "ACK";
        AckList acks = new AckList(data);
        if (!bMTUAcks && acks.getSessionID() != localSessionID_)
        {
            // if we are receiving acks from a previous session
            // but a hello hasn't been sent, then the other side
            // isn't aware we are back with a new session.  The
            // HELLO will notify them.  If we can't reach them,
            // then we'll time out.  If hello has been sent, then
            // this is a no-op
            if (hello())
            {
                if (UDPServer.DEBUG_ACKS_IGNORED) logger.debug("ACKS ignored, HELLO sent: "+ acks);
            }
            return;
        }

        synchronized (sendQueue_)
        {
            // loop through queue, removing messages that have bene ack'd
            int nOldSize = sendQueue_.size();
            Iterator<UDPData> iter = sendQueue_.iterator();
            UDPData qData;
            UDPData.Type type;

            while (iter.hasNext())
            {
                qData = iter.next();
                type = qData.getType();

                // if doing MTU_TEST acks, only process those (and vice versa)
                if (type == UDPData.Type.MTU_TEST && !bMTUAcks) continue;
                if (type != UDPData.Type.MTU_TEST && bMTUAcks) continue;

                if (acks.contains(qData))
                {
                    iter.remove();
                    stats_.recordRoundTripTime(qData);
                    if (UDPServer.DEBUG_ACKS_IN_DETAIL && !bMTUAcks) logger.debug("  "+ACK+" " + qData + " average round trip is " + stats_.getAverage() + " " + toStringNameIP());

                    // if our hello was acknowledged, the connection is established
                    switch(type)
                    {
                        case HELLO:
                            fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.ESTABLISHED, this));
                            break;

                        case GOODBYE:
                            finish(); // sender finish (acks received)
                            return;

                        case MTU_TEST:
                            // id of MTU_TEST is the total size of the packet
                            if (nMTU_ < qData.getID())
                            {
                                nMTU_ = qData.getID();
                                if (UDPServer.DEBUG_MTU)
                                {
                                    //logger.debug("  MTU increased to " + nMTU_ + " bytes " + toStringNameIP());
                                }

                                if (isLastMTU(qData))
                                {
                                    mtuTestDone();
                                }
                            }
                            break;
                    }
                }
            }

            if (UDPServer.DEBUG_ACKS_IN && nOldSize != sendQueue_.size() && !bMTUAcks) {
                logger.debug(ACK+"S in " + acks+" from " + getName() +", queue now has " + sendQueue_.size() + " messages ("+(nOldSize-sendQueue_.size())+" removed)");
            }
        }
    }

    /**
     * return true if connection was established
     */
    public boolean isEstablished()
    {
        return bHelloReceived_;
    }

    /**
     * notify handlers of full message received
     */
    void notifyHandlers(UDPData data)
    {
        fireEvent(new UDPLinkEvent(UDPLinkEvent.Type.RECEIVED, this, data));
    }

    ////
    //// Stats
    ////

    /**
     * Get average roundtrip
     */
    public UDPStats getStats()
    {
        return stats_;
    }

    /**
     * Class to track average time for ack to come back after being received.
     * Tracks last 100
     */
    public static class UDPStats
    {
        // send/error/resend
        private int packetReceived;
        private int dataOut;
        private int dataresend;
        private int dataIn;
        private int dataDups;
        private int packetSent;
        private int packetError;

        // data size
        private long bytesIn;
        private long bytesOut;
        private long bytesInCP;
        private long bytesOutCP;

        // roundtrip
        private MovingAverage roundtrip = new MovingAverage(100);

        /**
         * constructor
         */
        private UDPStats()
        {
            clear();
        }

        /**
         * Clear stats
         */
        private void clear()
        {
            packetReceived = 0;
            dataOut = 0;
            dataresend = 0;
            packetSent = 0;
            packetError = 0;
            roundtrip.reset();
        }

        /**
         * record bytes in
         */
        private void recordBytesIn(int bytes)
        {
            bytesIn += bytes;
        }

        /**
         * get bytes in
         */
        public long getBytesIn()
        {
            return bytesIn;
        }

        /**
         * get bytes in since last checkpoint
         */
        public long getBytesInCheckpoint()
        {
            long diff = bytesIn - bytesInCP;
            bytesInCP = bytesIn;
            return diff;
        }

        /**
         * get bytes out
         */
        public long getBytesOut()
        {
            return bytesOut;
        }


        /**
         * get bytes out since last checkpoint
         */
        public long getBytesOutCheckpoint()
        {
            long diff = bytesOut - bytesOutCP;
            bytesOutCP = bytesOut;
            return diff;
        }

        /**
         * record bytes in
         */
        private void recordBytesOut(int bytes)
        {
            bytesOut += bytes;
        }

        /**
         * record receive
         */
        private void recordPacketReceived()
        {
            packetReceived++;
        }

        /**
         * get number of udp packets received
         */
        public int getPacketReceived()
        {
            return packetReceived;
        }

        /**
         * record datas
         */
        private void recordDataOut()
        {
            dataOut++;
        }

        /**
         * get number of datas queued for sending (excludes ping/acks)
         */
        public int getDataOut()
        {
            return dataOut;
        }

        /**
         * record datas
         */
        private void recordDataIn()
        {
            dataIn++;
        }

        /**
         * get number of datas received
         */
        public int getDataIn()
        {
            return dataIn;
        }

        /**
         * record datas
         */
        private void recordDataDups()
        {
            dataDups++;
        }

        /**
         * get number of data duplicates recieved
         */
        public int getDataDups()
        {
            return dataDups;
        }

        /**
         * record send
         */
        private void recordPacketSent()
        {
            packetSent++;
        }

        /**
         * Get number of UDPMessages sent (includes ping/acks)
         */
        public int getPacketSent()
        {
            return packetSent;
        }

        /**
         * record resend
         */
        private void recordDataResend()
        {
            dataresend++;
        }

        /**
         * Get number of datas marked for resend
         */
        public int getDataResend()
        {
            return dataresend;
        }

        /**
         * record error
         */
        private void recordPacketSendError()
        {
            packetError++;
        }

        /**
         * get number of send errors
         */
        public int getPacketSendErrors()
        {
            return packetError;
        }

        /**
         * store round trip ack time
         */
        private void recordRoundTripTime(UDPData data)
        {
            roundtrip.record(data.elapsed());
        }

        /**
         * Get average
         */
        public long getAverage()
        {
            return roundtrip.getAverageLong();
        }

        /**
         * debug
         */
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("AVG: "+ getAverage() + ", OUT: " + dataOut +", RE: " + dataresend +
                      ", IN: "+ dataIn + ", DUP: "+ dataDups +
                      ", BIN: " + Utils.formatSizeBytes(bytesIn) + ", BOUT: "+ Utils.formatSizeBytes(bytesOut));

            return sb.toString();
        }
    }

    /**
     * debug
     */
    public String toString()
    {
        return "{" + Utils.getAddressPort(local_) +" <==> " + Utils.getAddressPort(remote_)  + "/" + id_ +"}";
    }

    /**
     * debug
     */
    public String toStringNameIP()
    {
        String ip = Utils.getAddressPort(remote_);
        if (sName_.equals(ip))
        {
            return "{" + sName_ + "}";
        }
        else
        {
            return "{" + sName_ +" :: " + ip  + "}";
        }
    }
}
