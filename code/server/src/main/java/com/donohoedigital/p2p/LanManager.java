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
 * LanManager.java
 *
 * Created on November 9, 2004, 9:19 AM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.log4j.*;

import java.net.*;
import java.util.*;

/**
 * @author donohoe
 */
public class LanManager implements DDMessageListener
{
    static Logger logger = Logger.getLogger(LanManager.class);

    private static final boolean DEBUG = false;

    private static int ALIVE_SECONDS = 5;
    private static int ALIVE_REFRESH_CNT = 10;
    private static int ALIVE_INIT_CNT = 10;

    private Peer2PeerMulticast multi_;
    private Alive alive_;
    private String sLocalHost_;
    private String sLocalIP_;
    private LanControllerInterface controller_;
    private LanClientList clients_;
    private String guid_;
    private String key_;
    private long startTime_ = System.currentTimeMillis();

    /**
     * Creates a new instance of LanManager
     */
    public LanManager(LanControllerInterface controller)
    {
        // controller
        controller_ = controller;
        ApplicationError.assertNotNull(controller_, "No controller");

        // create client list
        clients_ = new LanClientList(controller_);

        // get guid and key
        guid_ = controller.getGUID();
        key_ = controller.getPublicUseKey();

        // our host
        try
        {
            InetAddress local = InetAddress.getLocalHost();
            sLocalHost_ = local.getHostName();
            sLocalIP_ = local.getHostAddress();

            // strip ".local" at end of mac host name
            if (Utils.ISMAC)
            {
                sLocalHost_ = sLocalHost_.replaceAll("\\.local$", "");
            }
        }
        catch (UnknownHostException uhe)
        {
            StringTokenizer st = new StringTokenizer(uhe.getMessage(), ":");
            if (st.hasMoreTokens())
            {
                sLocalHost_ = st.nextToken();
                logger.warn("Unable to determine local host name, guessing it is: " + sLocalHost_);
            }
            else
            {
                logger.warn("Unable to determine local host name: " + uhe.getMessage());
                sLocalHost_ = "[unknown]";
            }

            // try and determine local IP from network interface
            try
            {
                // default to loopback addr
                sLocalIP_ = "127.0.0.1";

                // loop over all IPs
                Enumeration<NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();
                while (enu.hasMoreElements())
                {
                    Enumeration<InetAddress> ias = enu.nextElement().getInetAddresses();
                    while (ias.hasMoreElements())
                    {
                        // if an IP4 (non loopback), add it to list
                        InetAddress i = ias.nextElement();
                        if (i instanceof Inet4Address)
                        {
                            if (i.isLoopbackAddress()) continue;
                            sLocalIP_ = i.getHostAddress();
                        }
                    }
                }

                logger.warn("Local ip set to: " + sLocalIP_);
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    /**
     * Start the multicast listener, send HELLO message and
     * then start alive thread for periodic alive messages
     */
    public void start()
    {
        // create alive thread
        alive_ = new Alive();

        // create and start multicast listener
        try
        {
            multi_ = new Peer2PeerMulticast();
            multi_.addDDMessageListener(this);
            multi_.start();
        }
        catch (ApplicationError ae)
        {
            if (ae.getException() != null && ae.getException().getMessage() != null &&
                ae.getException().getMessage().contains("error setting options"))
            {
                logger.debug("Unable to start Peer2PeerMulticast (error setting options), local ip: " + sLocalIP_);
            }
            else
            {
                logger.error("Unable to start Peer2PeerMulticast: " + Utils.formatExceptionText(ae));
            }
            return;
        }
        catch (Throwable t)
        {
            logger.error("Unable to start Peer2PeerMulticast: " + Utils.formatExceptionText(t));
            return;
        }

        // start alive thread
        alive_.start();

        // send hello message to let others know we are alive
        sendMessage(LanClientList.LAN_HELLO);

        // add destroy hook
        Runtime.getRuntime().addShutdownHook(new Shutdown());
    }

    /**
     * Get ip
     */
    public String getIP()
    {
        return sLocalIP_;
    }

    /**
     * Get time alive in millis
     */
    public long getTimeAlive()
    {
        return System.currentTimeMillis() - startTime_;
    }

    /**
     * Get list of clients on LAN.  You can add LanListener to
     * this to get notification of changes to the list.
     */
    public LanClientList getList()
    {
        return clients_;
    }

    /**
     * Send message of given category
     */
    public void sendMessage(int nCategory)
    {
        if (DEBUG) logger.debug("Sending message: " + LanClientList.toString(nCategory));

        LanClientInfo info = new LanClientInfo(nCategory);
        info.setHostName(sLocalHost_);
        info.setPlayerName(controller_.getPlayerName());
        info.setIP(sLocalIP_);
        info.setGuid(guid_);
        info.setAliveMillis(getTimeAlive());
        info.setGameData(controller_.getOnlineGame());

        try
        {
            multi_.send(info.getData());
        }
        catch (ApplicationError e)
        {
            logger.error("Send error: " + e.toString());
        }
        catch (Throwable t)
        {
            logger.error("Send error: " + Utils.formatExceptionText(t));
        }
    }

    /**
     * message received
     */
    public void messageReceived(DDMessage message)
    {
        LanClientInfo info = new LanClientInfo(message);

        // validate key
        if (!controller_.isValid(info.getData()))
        {
            logger.error("Invalid key: " + info);
            return;
        }

        // get GUID
        String guid = info.getGuid();
        if (guid == null)
        {
            logger.error("Missing GUID: " + info);
            return;
        }

        // ignore our own messages
        if (guid.equals(guid_)) return;

        // check for duplicate key or another copy on same machine
        String sKey = info.getKey();
        String sIP = info.getIP();
        if ((sIP.equals(sLocalIP_) || sKey.equals(key_)) && !controller_.allowDuplicate())
        {
            long time = info.getAliveMillis();
            if (getTimeAlive() < time)
            {
                if (sKey.equals(key_))
                {
                    // notify user of duplicate
                    controller_.handleDuplicateKey(
                            info.getPlayerName(),
                            info.getHostName(),
                            info.getIP());
                }
                else
                {
                    // notify user of duplicate
                    controller_.handleDuplicateIp(
                            info.getPlayerName(),
                            info.getHostName(),
                            info.getIP());
                }
            }
            else
            {
                alive_.resetAliveCnt();
                alive_.interrupt();
            }
            return;
        }

        // process message (may change category, e.g.,
        // from ALIVE to HELLO if we missed hello from
        // this client)
        clients_.process(info);

        // if hello, send some alive messages
        if (info.getCategory() == LanClientList.LAN_HELLO ||
            info.getCategory() == LanClientList.LAN_REFRESH)
        {
            alive_.resetAliveCnt();
            alive_.interrupt();
        }
    }

    /**
     * DDMessageListener: Not used *
     */
    public void updateStep(int nStep)
    {
    }

    /**
     * DDMessageListener: Not used *
     */
    public void debugStep(int nStep, String sMsg)
    {
    }

    /**
     * interface to alive from application.  bContinous true means
     * the alive thread will send out an alive ping continously.
     * bSendRefresh true means it will send out the LAN_REFRESH message
     * instead of LAN_ALIVE, which will wake up other clients out there.
     */
    public void setAliveThread(boolean bContinous, boolean bSendRefresh)
    {
        alive_.setContinous(bContinous);
        alive_.setSendRefresh(bSendRefresh);
        alive_.interrupt();
    }

    /**
     * wake up alive thread so it sends immediately
     */
    public void wakeAliveThread()
    {
        alive_.interrupt();
    }

    /**
     * alive thread
     */
    private class Alive extends Thread
    {
        boolean bDone_ = false;
        boolean bRefresh_ = false;
        private int nAliveCnt_ = ALIVE_INIT_CNT;
        private boolean bContinuous_ = false;

        public Alive()
        {
            setName("LanManager-Alive");
        }

        public void resetAliveCnt()
        {
            if (nAliveCnt_ < ALIVE_REFRESH_CNT)
            {
                nAliveCnt_ = ALIVE_REFRESH_CNT;
            }
            if (DEBUG) logger.debug("Alive cnt reset to: " + nAliveCnt_);
        }

        public void setContinous(boolean b)
        {
            bContinuous_ = b;
        }

        public void setSendRefresh(boolean bRefresh)
        {
            bRefresh_ = bRefresh;
        }

        public void finish()
        {
            bDone_ = true;
            interrupt();
        }

        public void run()
        {
            while (!bDone_)
            {
                Utils.sleepSeconds(ALIVE_SECONDS);

                // clear interrupted flag incase was set by an update above.
                // this is a no-op if not interrupted
                interrupted();

                // if not done, and in continous mode or have ticks left in alive cnt, sent msg
                if (!bDone_ && (bContinuous_ || nAliveCnt_ > 0))
                {
                    if (!bContinuous_) nAliveCnt_--;
                    sendMessage(bRefresh_ ? LanClientList.LAN_REFRESH : LanClientList.LAN_ALIVE);
                }

                if (!bDone_)
                {
                    clients_.timeoutCheck((ALIVE_SECONDS * 2) + 1);
                }
            }
        }
    }

    /**
     * Shutdown thread for cleanup
     */
    private class Shutdown extends Thread
    {
        public Shutdown()
        {
            setName("LanManager-Shutdown");
        }

        public void run()
        {
            logger.debug("Shutting down...");
            alive_.finish();
            sendMessage(LanClientList.LAN_GOODBYE);
            multi_.stop();
        }
    }

}
