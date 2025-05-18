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
 * OnlineManagerQueue.java
 *
 * Created on June 23, 2005, 11:07 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.server.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

/**
 * @author  donohoe
 */
public class OnlineManagerQueue implements Runnable
{
    static Logger logger = LogManager.getLogger(OnlineManagerQueue.class);

    // settings
    private static int SLEEP_UNAVAIL = 25; // millis to sleep when no worker thread available
    private static int LOG_UNAVAIL = 1000; // millis to wait before logging no worker warning

    // info
    private OnlineManager mgr_;
    private Thread threadQ_ = null;
    WorkerPool pool_;

    // instance info
    private ArrayList msgQ_ = new ArrayList();
    private boolean bDone_ = false;
    private int nWait_ = 100;
    private boolean bSleeping_ = false;
    private Object SLEEPCHECK = new Object();
    private int nElapsedNoWorkerTime_ = 0;

    /**
     * Set up the Q
     */
    public OnlineManagerQueue(OnlineManager mgr)
    {
        this.mgr_ = mgr;

        // create thread pool before main thread started
        pool_ = new WorkerPool(mgr_.bHost_ ? 3 : 1, OnlineSendThread.class);

        // main queue thread
        threadQ_ = new Thread(this, "OnlineManagerQueue");
        threadQ_.start();
    }
        
    /**
     * Cleanup
     */
    public void finish()
    {
        //logger.info("Starting OnlineManagerQueue Shutdown...");
        cleanup();
        wakeup();
        try {
            threadQ_.join();
        } catch (InterruptedException ie) {
            Thread.interrupted();
        }
        threadQ_ = null;
    }

    /**
     * Start queue
     */
    public void run()
    {
        //logger.info("OnlineManagerQueue started, wait millis = " + nWait_);

        // loop until done
        while (!bDone_)
        {
            // sleep first, so if we are woken up by interrupt,
            // we process any message that came through
            bSleeping_ = true;
            Utils.sleepMillis(nWait_);
            bSleeping_ = false;
            
            // use while loop in case messages come in while we are processing
            // skip if last send attempt failed (so we can sleep then try again
            // in a bit)
            synchronized(SLEEPCHECK)
            {
                ArrayList list = null;
                while ((list = getMessageQueue()) != null)
                {
                    sendMessages(list);
                }
            }
        }
        
        //logger.info("Shutting down OnlineMessageQueue...");
        ArrayList list = getMessageQueue();
        if (list != null && list.size() > 0)
        {
            logger.warn("Left " + list.size() + " messages to send");
        }

        // remove workers
        pool_.shutdown();

        //logger.info("OnlineMessageQueue shut down.");
    }
    
    /**
     * Wake up thread if sleeping
     */
    private void wakeup()
    {
        synchronized(SLEEPCHECK)
        {
            if (bSleeping_) {
                //logger.info("Waking up sleeping OnlineMessageQueue...");
                threadQ_.interrupt();
            }
        }
    }
    
    /**
     * Cleanup queue (thread about to stop)
     */
    public void cleanup()
    {
        bDone_ = true;
    }
    
    /** 
     * Add a message to the Queue
     */
    protected synchronized void addMessage(OnlineMessage msg, PokerPlayer to, boolean bImmediate)
    {
        Qentry q = new Qentry(msg, to);
        if (!bImmediate)
        {
            msgQ_.add(q);

            // debug info
            if (msgQ_.size() >= 50 && (msgQ_.size() % 25 == 0))
            {
                logger.info("Queue is now at ("+msgQ_.size()+")...");
                debugQ();
            }
        }
        else
        {
            sendMessage(q);
        }
    }

    /**
     * Debug message about Q - print each player with messages and number of good/bad messages
     */
    private void debugQ()
    {
        PokerGame game = this.mgr_.getGame();
        int nNumP = game.getNumPlayers();
        PokerPlayer p;
        for (int i = 0; i < nNumP; i++)
        {
            p = game.getPokerPlayerAt(i);
            if (p.isObserver()) continue;
            p.nBadMsgs_ = 0;
            p.nGoodMsgs_ = 0;
        }

        int nNumO = game.getNumObservers();
        for (int i = 0; i < nNumO; i++)
        {
            p = game.getPokerObserverAt(i);
            p.nBadMsgs_ = 0;
            p.nGoodMsgs_ = 0;
        }

        int qsize = msgQ_.size();
        Qentry q;
        for (int i = 0; i < qsize; i++)
        {
            q = (Qentry) msgQ_.get(i);
            if (q.to.getConnection() != null && q.to.getConnection().equals(q.connection)) q.to.nGoodMsgs_++;
            else q.to.nBadMsgs_++;
        }

        for (int i = 0; i < nNumP; i++)
        {
            p = game.getPokerPlayerAt(i);
            if (p.isObserver()) continue;
            if (p.nBadMsgs_ > 0 || p.nGoodMsgs_ > 0)
            {
                logger.info("Queued messages for " + p.getName() + ":  good="+p.nGoodMsgs_+",  stale="+p.nBadMsgs_);
            }
        }

        for (int i = 0; i < nNumO; i++)
        {
            p = game.getPokerObserverAt(i);
            if (p.nBadMsgs_ > 0 || p.nGoodMsgs_ > 0)
            {
                logger.info("Queued messages for " + p.getName() + " [obs]:  good="+p.nGoodMsgs_+",  stale="+p.nBadMsgs_);
            }
        }
    }

    /**
     * Get current contents of queue and start new list
     */
    private synchronized ArrayList getMessageQueue()
    {
        if (msgQ_.size() == 0) return null; // avoid new object if empty
        
        ArrayList list = msgQ_;
        msgQ_ = new ArrayList();
        return list;
    }

    /**
     * Clear the queue - done when connection to host lost in client
     */
    public synchronized void clearQueue()
    {
        logger.info("Clearing " + msgQ_.size() + " messages from queue due to disconnect");
        msgQ_.clear();
    }

    /**
     * Process current messages in the queue.  Return true if successful
     * and returns false if an exception was received.
     */
    private void sendMessages(ArrayList list)
    {
        int nNum = (list == null) ? 0 : list.size();

        if (nNum >= 50) logger.debug("OnlineMessageQueue: " + nNum + " messages to process");

        if (nNum == 0) return;

        Qentry msg;
        int index = 0;
        while (list.size() > 0)
        {
            msg = null;

            // get next message to a player not currently
            // being sent a message
            for (int i = 0; i < list.size(); i++)
            {
                msg = (Qentry) list.get(i);
                if (msg.to.getSenderThread() == null)
                {
                    index = i;
                    break;
                }
                else
                {
                    msg = null;
                }
            }

            // if no message to send, wait
            if (msg == null)
            {
                //logger.info("Waiting....da da da");
                Utils.sleepMillis(5);
                continue;
            }

            // process message
            if (processMessage(msg))
            {
                list.remove(index);
            }
        }
    }

    /**
     * implementation of sender thread
     */
    public static class OnlineSendThread extends WorkerThread
    {
        OnlineManagerQueue q;
        Qentry entry;

        public OnlineSendThread()
        {
        }

        public void wakeup(OnlineManagerQueue q, Qentry entry)
        {
            this.q = q;
            this.entry = entry;
            entry.to.setSenderThread(this);
            super.wakeup();
        }

        public void process()
        {
            //logger.debug(getName() + " sending to " + entry.to.getName());
            try {
                q.sendMessage(entry);
            }
            finally
            {
                entry.to.setSenderThread(null);
            }
        }
    }

    /**
     * Get worker from pool to handle socket connection
	 */
	private boolean processMessage(Qentry q)
	{
        // make sure current player's socket is not null and that
        // it equals the socket in use when the message was queued
        // this last check is useful for re-connecting players, to
        // avoid trying to send old messages to them.
        PokerPlayer pTo = q.to;
        PokerConnection sc = pTo.getConnection();
        if (sc == null || !(sc.equals(q.connection)))
        {
            OnlineMessage omsg = q.msg;
            if (sc == null) logger.warn("Socket now null, skipping message to " + pTo.getName() + ": "+omsg.toStringCategory());
            else logger.warn("Socket changed, skipping message to " + pTo.getName() + ": "+omsg.toStringCategory());
            return true;
        }

        // get worker
		OnlineSendThread worker = (OnlineSendThread) pool_.getWorker();

		if (worker == null)
        {
            // No threads available, do nothing, the above
			// loop will keep calling this method until a
			// thread becomes available

            // warn if worker is null and sleep
            // to allow time for another thread to finish
            if (nElapsedNoWorkerTime_ >= LOG_UNAVAIL)
            {
                logger.warn("*** NO worker thread available for " + nElapsedNoWorkerTime_ + " millis " +
                            "(sleep is " + SLEEP_UNAVAIL + "), current player=" + q.to.getName());
                nElapsedNoWorkerTime_ = 0;
            }

            Utils.sleepMillis(SLEEP_UNAVAIL);
            nElapsedNoWorkerTime_ += SLEEP_UNAVAIL;

			return false;
		}
        else
        {
            nElapsedNoWorkerTime_ = 0;
        }

		// invoking this wakes up the worker thread then returns
		worker.wakeup(this, q);

        return true;
    }

    /**
     * Send message to given player
     */
    private void sendMessage(Qentry q)
    {
        PokerPlayer pTo = q.to;
        OnlineMessage omsg = q.msg;

        // synchronize for case of immediate sends, so we only send one message at a time to a player
        synchronized (pTo.getSendSync())
        {
            PokerConnection connection = pTo.getConnection();

            // make sure current player's socket is not null and that
            // it equals the socket in use when the message was queued
            // this last check is useful for re-connecting players, to
            // avoid trying to send old messages to them.
            if (connection == null || !(connection.equals(q.connection)))
            {
                if (connection == null) logger.warn("Socket now null, skipping message to " + pTo.getName() + ": "+omsg.toStringCategory());
                else logger.warn("Socket changed, skipping message to " + pTo.getName() + ": "+omsg.toStringCategory());
                return;
            }

            // debug
            if (OnlineManager.DEBUG)
            {
                OnlineManager.SCNT++;
                if (OnlineManager.DEBUG || omsg.getCategory() != OnlineMessage.CAT_CHAT)
                {
                    logger.debug(OnlineManager.SCNT+ " sending to " + pTo.getName() +": " +
                                 omsg.toStringCategorySize());
                }
            }
            if (TESTING(EngineConstants.TESTING_P2P))
            {
                logger.debug("Sending a message to " + pTo.getName() +": "
                                                //+omsg.toStringNoData()
                                                );
            }

            // create message and write it to socket
            DDMessageTransporter msg = mgr_.p2p_.newMessage(omsg.getData());

            try
            {
                //int nBytes =
                mgr_.p2p_.send(connection, msg);
                //logger.debug("Wrote " + nBytes + " bytes to " + pTo.getName() + ", data: " + omsg.toStringNoData());

                if (DebugConfig.isTestingOn() &&
                    omsg.getCategory() == OnlineMessage.CAT_CHAT &&
                    omsg.getChat().startsWith("disconnect test"))
                {
                    throw new TestException();
                }
            }
            catch (IOException ioe)
            {
                if (ioe instanceof TestException)
                {
                    logger.warn("Test exception caught sending message to: " + pTo.getName() +  ": " + omsg.toStringNoData());
                }
                else if (ioe instanceof SocketTimeoutException)
                {
                    logger.warn("Timeout sending message to: " + pTo.getName() +  ": " +
                                ioe.getMessage());
                }
                else if (ioe instanceof ClosedChannelException)
                {
                    logger.warn("Socket already closed sending message to: " + pTo.getName() +  ": " +
                                Utils.getExceptionMessage(ioe));
                }
                else
                {
                    String sMsg = Utils.getExceptionMessage(ioe);
                    if (sMsg.indexOf("forcibly") != -1)
                    {
                        if (sMsg.indexOf("remote") != -1)
                        {
                            logger.warn("REMOTE Socket forcibly closed sending message to: " + pTo.getName() +  ": " +
                                Utils.getExceptionMessage(ioe));
                        }
                        else
                        {
                            logger.warn("LOCAL Socket forcibly closed sending message to: " + pTo.getName() +  ": " +
                                Utils.getExceptionMessage(ioe));
                        }
                    }
                    else
                    {
                        logger.warn("Error sending message to: " + pTo.getName() +  ": " +
                                omsg.toStringNoData() + ";  " + Utils.formatExceptionText(ioe) +
                                "\nOriginated at: " + Utils.formatExceptionText(q.source));
                    }
                }
                // we got an error - let's close the socket since it is likely not
                // useable anymore - this will trigger socketClosing in OnlineManager.
                mgr_.p2p_.closeConnection(connection);
            }
        }
    }

    /**
     * Testing purposes
     */
    private class TestException extends IOException
    {
        TestException()
        {
            super("Test Exception");
        }
    }

    /**
     * q entry
     */
    private class Qentry
    {
        OnlineMessage msg;
        PokerPlayer to;
        PokerConnection connection;
        Throwable source;

        Qentry(OnlineMessage msg, PokerPlayer to)
        {
            this.msg = msg;
            this.to = to;
            this.connection = to.getConnection();
            source = new Throwable();
        }
    }
}
