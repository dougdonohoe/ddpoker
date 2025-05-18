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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.prefs.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 1, 2006
 * Time: 8:24:58 AM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "PublicField"})
public class UDPServer extends Thread
{
    static Logger logger = LogManager.getLogger(UDPServer.class);

    // UDP debug control
    static boolean DEBUG_UDP_DETAIL = false;

    public static boolean DEBUG_INCOMING = false;
    public static boolean DEBUG_INCOMING_QUEUE = false;
    public static boolean DEBUG_OUTGOING = false;
    public static boolean DEBUG_RESEND = false;
    public static boolean DEBUG_ACKS_IN = false;
    public static boolean DEBUG_ACKS_IN_DETAIL = false;
    public static boolean DEBUG_ACKS_OUT = false;
    public static boolean DEBUG_ACKS_IGNORED = false;
    public static boolean DEBUG_TIMEOUT = false;
    public static boolean DEBUG_CREATE_DESTROY = false;
    public static boolean DEBUG_MTU = false;

    public static final String TESTING_UDP = "testing.debug.udp";

    // set debug flags
    public static void setDebugFlags()
    {
        boolean detail = DebugConfig.TESTING(TESTING_UDP);
        boolean on = true;
        boolean off = false;
        DEBUG_UDP_DETAIL = detail;
        DEBUG_INCOMING = DEBUG_UDP_DETAIL && on;
        DEBUG_INCOMING_QUEUE = DEBUG_UDP_DETAIL && off;
        DEBUG_OUTGOING = DEBUG_UDP_DETAIL && on;
        DEBUG_RESEND = DEBUG_UDP_DETAIL && on;
        DEBUG_ACKS_IN = DEBUG_UDP_DETAIL && on;
        DEBUG_ACKS_IN_DETAIL = DEBUG_UDP_DETAIL && on;
        DEBUG_ACKS_OUT = DEBUG_UDP_DETAIL && off;
        DEBUG_ACKS_IGNORED = DEBUG_UDP_DETAIL && off;
        DEBUG_TIMEOUT = DEBUG_UDP_DETAIL && on;
        DEBUG_CREATE_DESTROY = DEBUG_UDP_DETAIL && on;
        DEBUG_MTU = DEBUG_UDP_DETAIL && on;
    }

    // pref info for storing GUID
    private static final String PREF_NODE = "udp";
    private static final String PREF_UDP_GUID = "guid";

    // config stuff
    private Selector selector_;
    private boolean bExceptionOnNoBind_;
    private boolean bBindLoopback_;
    private boolean bDone_ = false;
    private String sPort_ = null;
    private boolean bBindFailover_;
    private int nFailoverAttempts_;
    private List<DatagramChannel> channels_ = new ArrayList<>();
    private Map<DatagramChannel, InetSocketAddress> channelToIP_ = new HashMap<>();
    private Map<InetSocketAddress, DatagramChannel> ipToChannel_ = new HashMap<>();
    private Map<InetSocketAddress, UDPID> ipToID_ = new HashMap<>();
    private Shutdown shutdown_;

    // one buffer for incoming (main UDP thread reads and then queues)
    private ByteBuffer bb_ = ByteBuffer.allocate(UDPLink.MAX_PAYLOAD_SIZE);

    // main components
    private UDPManager mgr_;
    private UDPLinkHandler handler_;
    private DispatchQueue dispatchQueue_;
    private OutgoingQueue outgoingQueue_;
    private DatagramChannel defaultChannel_;

    /**
     * New UDPServer.  Pass in a handler.  We'll use one for now - may need
     * to expand in the future, but this is easier.
     */
    public UDPServer(UDPLinkHandler handler, boolean bExceptionOnNoBind)
    {
        this(handler, bExceptionOnNoBind, false, null);
    }

    /**
     * New UDPServer.  Pass in a handler.  We'll use one for now - may need
     * to expand in the future, but this is easier.
     */
    public UDPServer(UDPLinkHandler handler, boolean bExceptionOnNoBind, boolean bBindLoopback, String sPort)
    {
        setName("UDPServer");
        handler_ = handler;
        bExceptionOnNoBind_ = bExceptionOnNoBind;
        bBindLoopback_ = bBindLoopback;
        sPort_ = sPort;
    }

    /**
     * Must call this after constructor (to do binding and other init) before calling
     * start() - to start in its own thread or run() - to run in current thread
     */
    public void init()
    {
        try
        {
            _init();
        }
        catch (IOException ioe)
        {
            System.err.println("GameServer error: " + Utils.formatExceptionText(ioe));
        }
    }

    /**
     * Initialize - bind to configured IPs
     */
    private void _init() throws IOException
    {
        // get debug flags
        setDebugFlags();

        // add destroy hook
        shutdown_ = new Shutdown();
        Runtime.getRuntime().addShutdownHook(shutdown_);

        // get startup settings
        if (sPort_ == null) sPort_ = PropertyConfig.getRequiredStringProperty("settings.udp.port");
        String sIP = PropertyConfig.getStringProperty("settings.udp.ip", null, false);
        bBindFailover_ = PropertyConfig.getBooleanProperty("settings.udp.failover", true, false);
        nFailoverAttempts_ = PropertyConfig.getIntegerProperty("settings.udp.failover.attempts", 3);

        // display info
        logger.info("Config port(s): " + sPort_);

        // create a new Selector for use below
        selector_ = Selector.open();

        // init
        int nPort;
        String port = null;
        StringTokenizer ports = new StringTokenizer(sPort_, ",");
        DatagramChannel channel;
        DatagramSocket socket;

        // get all IPs to bind to
        StringTokenizer ips = sIP == null ? null : new StringTokenizer(sIP, ",");
        List<String> configIPs = new ArrayList<>();
        List<InetAddress> actualIPs = new ArrayList<>();
        List<InetAddress> activeIPs = new ArrayList<>();

        // config file IPs (may be empty)
        while (ips != null && ips.hasMoreTokens())
        {
            configIPs.add(ips.nextToken());
        }

        // loop over all IPs
        Enumeration<NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();
        while (enu.hasMoreElements())
        {
            NetworkInterface ni = enu.nextElement();
            Enumeration<InetAddress> ias = ni.getInetAddresses();
            while (ias.hasMoreElements())
            {
                // if an IP4 (non loopback), add it to list
                InetAddress i = ias.nextElement();
                if (i instanceof Inet4Address)
                {
                    if (!bBindLoopback_ && i.isLoopbackAddress()) continue;

                    actualIPs.add(i);

                    if (configIPs.contains(i.getHostAddress()))
                    {
                        activeIPs.add(i);
                        configIPs.remove(i.getHostAddress());
                        logger.info("Using specific address: " + i.getHostAddress());
                    }
                }
            }
        }

        // if activeIPs is empty, use all IPs
        if (activeIPs.isEmpty())
        {
            logger.info("Using all addresses (UDP)");
            activeIPs = actualIPs;
        }

        // loop over all ports
        while (ports.hasMoreTokens())
        {
            try
            {
                port = ports.nextToken();
                nPort = Integer.parseInt(port);

                // set the port the server channel will listen to
                logger.info("Processing port " + nPort + "...");

                for (InetAddress i : activeIPs)
                {
                    // allocate an unbound server socket channel
                    channel = DatagramChannel.open();
                    socket = channel.socket();
                    socket.setReceiveBufferSize(32 * 1024);
                    socket.setSendBufferSize(32 * 1024);

                    logger.info("Binding: " + i.getHostAddress() + ":" + nPort +
                                " (send=" + socket.getSendBufferSize() +
                                ", rcv=" + socket.getReceiveBufferSize() + ")");

                    InetSocketAddress ip;
                    try
                    {
                        ip = bind(socket, i, nPort);
                    }
                    catch (SocketException be)
                    {
                        logger.error("Unable to bind: " + Utils.getExceptionMessage(be));
                        continue;
                    }

                    // Keep track of channels, ips and ids
                    channels_.add(channel);
                    channelToIP_.put(channel, ip);
                    ipToChannel_.put(ip, channel);
                    ipToID_.put(ip, getUDPID(ip.getPort()));

                    // default channel - first non loopback
                    if (defaultChannel_ == null || defaultChannel_.socket().getLocalAddress().isLoopbackAddress())
                    {
                        defaultChannel_ = channel;
                    }

                    // set non-blocking mode
                    channel.configureBlocking(false);

                    // register with selector
                    channel.register(selector_, SelectionKey.OP_READ);
                }
            }
            catch (NumberFormatException nfe)
            {
                logger.error("Unable to parse: " + port);
            }
        }

        // make sure we have one valid port
        if (channels_.isEmpty() && bExceptionOnNoBind_)
        {
            throw new ApplicationError(ErrorCodes.ERROR_SERVER_NO_PORTS,
                                       "No ports were bound", sPort_, null);
        }

        // store first port as preferred
        if (!channels_.isEmpty())
        {
            logger.info("Preferred UDP (chat) address set to " + Utils.getLocalAddressPort(getDefaultChannel()));
        }

        // create dispatch queue
        dispatchQueue_ = new DispatchQueue();

        // create outgoing queue
        outgoingQueue_ = new OutgoingQueue();

        // create manager
        mgr_ = new UDPManager(this);
    }

    /**
     * Get UPDID for port
     */
    private UDPID getUDPID(int nPort)
    {
        Preferences pref = Prefs.getUserPrefs(PREF_NODE);
        String sKey = PREF_UDP_GUID + ":" + nPort;
        String pGUID = pref.get(sKey, null);
        if (pGUID == null)
        {
            RandomGUID guid = new RandomGUID(ConfigUtils.getLocalHost(true), true);
            pGUID = guid.toString();
            pref.put(sKey, pGUID);
            logger.info("Created new UDP GUID: " + pGUID + " (port " + nPort + ")");
        }
        else
        {
            logger.info("Using existing UDP GUID: " + pGUID + " (port " + nPort + ")");
        }
        return new UDPID(pGUID);
    }

    /**
     * Attempt bind, failover to next port if desired, return addr actually bound
     */
    private InetSocketAddress bind(DatagramSocket socket, InetAddress ia, int nPort) throws SocketException
    {
        int nAttempts = nFailoverAttempts_;
        SocketException e = null;
        for (int i = 0; i < nAttempts; i++)
        {
            try
            {
                InetSocketAddress addr = new InetSocketAddress(ia, nPort);
                socket.bind(addr);
                return addr;
            }
            catch (SocketException e2)
            {
                if (!bBindFailover_) throw e2;
                logger.info("Failed binding to " + ia.getHostAddress() + ":" + nPort + ", trying port " + (nPort - 1));
                nPort--;
                e = e2;
            }
        }
        if (e != null) {
            throw e;
        }
        return null;
    }

    /**
     * Return UDP Manager
     */
    public UDPManager manager()
    {
        return mgr_;
    }

    /**
     * Return dispatch queue
     */
    DispatchQueue dispatch()
    {
        return dispatchQueue_;
    }

    /**
     * Return outgoing queue
     */
    OutgoingQueue outgoing()
    {
        return outgoingQueue_;
    }

    /**
     * Return handler
     */
    public UDPLinkHandler handler()
    {
        return handler_;
    }

    /**
     * return preferred port
     */
    public int getPreferredPort()
    {
        if (defaultChannel_ != null) return defaultChannel_.socket().getLocalPort();
        return -1;
    }

    /**
     * return preferred ip
     */
    public String getPreferredIP()
    {
        if (defaultChannel_ != null) return defaultChannel_.socket().getLocalAddress().getHostAddress();
        return "127.0.0.1";
    }

    /**
     * return if bound
     */
    public boolean isBound()
    {
        return defaultChannel_ != null;
    }

    /**
     * Return port(s) specified in config file
     */
    public String getConfigPort()
    {
        return sPort_;
    }

    /**
     * return default DatagramChannel
     */
    public DatagramChannel getDefaultChannel()
    {
        return defaultChannel_;
    }

    /**
     * Get ip for channel
     */
    public InetSocketAddress getIP(DatagramChannel channel)
    {
        return channelToIP_.get(channel);
    }

    /**
     * Get channel for ip
     */
    public DatagramChannel getChannel(InetSocketAddress ip)
    {
        return ipToChannel_.get(ip);
    }

    /**
     * Get id for ip
     */
    public UDPID getID(InetSocketAddress ip)
    {
        return ipToID_.get(ip);
    }

    /**
     * process requests (called via Thread start())
     */
    public void run()
    {
        int n;

        // start the dispatch queue
        dispatchQueue_.start();

        // start outgoing queue
        outgoingQueue_.start();

        // start the manager
        mgr_.start();

        // MAIN: loop forever, processing requests
        while (!bDone_)
        {
            // this may block for a long time, upon return the
            // selected set contains keys of the ready channels
            try
            {
                n = selector_.select();
            }
            catch (Throwable t)
            {
                // don't print to log if interrupted system call - happens on shutdown,
                // in particular on Linux
                if (!bDone_ && !Utils.getExceptionMessage(t).contains("Interrupted system call"))
                {
                    logger.error("selector.select() error: " + Utils.formatExceptionText(t));
                }
                continue;
            }

            try
            {
                // process selection
                if (n > 0)
                {
                    processSelection();
                }
            }
            catch (Throwable t)
            {
                logger.error("UDPServer error: " + Utils.formatExceptionText(t));
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
            setName("UDPServer-Shutdown");
        }

        public void run()
        {
            logger.info("UDPServer shutting down...");
            shutdown(false);
        }
    }

    /**
     * Stop the server
     */
    public void shutdown()
    {
        shutdown(true);
    }

    /**
     * stop the game server, remove shutdown hook if directed to do so
     */
    private void shutdown(boolean bRemoveHook)
    {
        // set done flag, remove shutdown hook
        if (bDone_) return; // don't run multiple times
        bDone_ = true;

        if (bRemoveHook) Runtime.getRuntime().removeShutdownHook(shutdown_);

        // close selector
        try
        {
            selector_.wakeup();
            selector_.close();
        }
        catch (Throwable ignore)
        {
            //logger.debug("Caught exception shutting down selector: " + Utils.formatExceptionText(ignore));
        }

        // stop the dispatch queue
        dispatchQueue_.finish();

        // stop the outgoing queue
        outgoingQueue_.finish();

        // stop manager
        mgr_.finish();

        // close all sockets to release ports
        for (DatagramChannel channel : channels_)
        {
            try
            {
                if (channel.socket() != null)
                {
                    logger.info("UDPServer closing " + Utils.getLocalAddressPort(channel));
                    channel.socket().close();
                }
            }
            catch (Throwable ignore)
            {
                //logger.debug("Caught exception shutting down socket: " + Utils.formatExceptionText(ignore));
            }
        }

        logger.info("UDPServer Done.");
    }

    /**
     * Logic to process selected keys
     */
    private void processSelection()
    {
        // get an iterator over the set of selected keys
        Iterator<SelectionKey> iter;
        SelectionKey key;

        try
        {
            iter = selector_.selectedKeys().iterator();
        }
        catch (ClosedSelectorException cse)
        {
            if (!bDone_)
            {
                logger.error("processSelection error: " + Utils.formatExceptionText(cse));
            }
            return;
        }

        // look at each key in the selected set
        while (iter.hasNext())
        {
            key = iter.next();

            try
            {
                if (key.isReadable())
                {
                    processChannel(key);
                }
            }
            catch (IOException ioe)
            {
                logger.error("processSelection error: " + Utils.formatExceptionText(ioe));
            }
            finally
            {
                // remove key from selected set, as it has been handled
                iter.remove();
            }
        }
    }

    /**
     * Get worker from pool to handle socket connection
     */
    private void processChannel(SelectionKey key) throws IOException
    {
        // get channel
        DatagramChannel channel = (DatagramChannel) key.channel();

        // read packet
        bb_.clear();
        InetSocketAddress from = (InetSocketAddress) channel.receive(bb_);
        if (from == null)
        {
            // this will seem to happen on windows if the other side dies.  Not
            // sure why.  Maybe be related to ongoing outgoing messages.
            //logger.warn("No data available on " + Utils.getLocalAddressPort(channel));
            return;
        }
        bb_.flip();
        mgr_.addMessage(new UDPMessage(this, bb_, getIP(channel), from)); // TODO: compare to destIP?
    }
}
