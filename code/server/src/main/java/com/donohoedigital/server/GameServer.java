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
package com.donohoedigital.server;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public abstract class GameServer extends Thread
{
    protected static Logger logger = Logger.getLogger(GameServer.class);

    static boolean DEBUG_ONLINE = false;
    public static final boolean DEBUG_POOL = false;

    // sleep/log stuff
    private int SLEEP_UNAVAIL; // millis to sleep when no worker thread available
    private int LOG_UNAVAIL; // millis to wait before logging no worker warning
    private int LOG_STATUS; // millis to wait before logging server status
    private long nLastLogTime_;
    private long nNum_ = 0;

    // config stuff
    private String sPortKey_ = "settings.server.port";
    private boolean exceptionOnNoPortsBound = true;
    private boolean bindLoopback = false;
    private boolean configLoadRequired = true;
    private boolean logStatus = true;
    private String appName;
    private Selector selector_;
    private boolean bDone_ = false;
    private String sPort_;
    private int nElapsedNoWorkerTime_ = 0;
    private int nRunningNoWorkerCnt_ = 0;
    private int nHits_;
    private boolean bBindFailover_;
    private int nFailoverAttempts_;
    private final List<ServerSocketChannel> channels_ = new ArrayList<>();
    private Shutdown shutdown_;

    // main components
    private BaseServlet servlet_;
    private ThreadPool pool_;
    private Thread mainThread_;
    private final List<Qentry> registerQ_ = new ArrayList<>();
    private ServerSocketChannel defaultChannel_;

    /**
     * default constructor
     */
    public GameServer()
    {
        setName("GameServer");
    }

    /**
     * Return app name
     */
    public final String getAppName()
    {
        return appName;
    }

    public final void setAppName(String appName)
    {
        setName(appName); // set thread name too
        this.appName = appName;
    }

    /**
     * allow override of the properties key used to lookup our port
     */
    public final void setPortKey(String portKey) {
        sPortKey_ = portKey;
    }

    /**
     * Set the servlet.  Calls setServer(this) on the given servlet
     */
    public final void setServlet(BaseServlet servlet)
    {
        servlet_ = servlet;
        servlet_.setServer(this);
    }

    /**
     * Get the servlet
     */
    public final BaseServlet getServlet()
    {
        return servlet_;
    }

    /**
     * Is ConfigManager init required?
     * Default is true.  Returning false allows this
     * framework to be used in client applications
     */
    public final boolean isConfigLoadRequired()
    {
        return configLoadRequired;
    }

    /**
     * Set whether config file loading is required (do so prior to init())
     */
    public final void setConfigLoadRequired(boolean b)
    {
        configLoadRequired = b;
    }

    /**
     * Should the loopback address (127.0.0.1) be bound?  Default is false
     */
    public final boolean isBindLoopback()
    {
        return bindLoopback;
    }

    /**
     * Set whether to bind loopback address
     */
    public final void setBindLoopback(boolean bindLoopback)
    {
        this.bindLoopback = bindLoopback;
    }

    /**
     * Should an exception should be thrown if no ports are bound.  Default is true.
     */
    public final boolean isExceptionOnNoPortsBound()
    {
        return exceptionOnNoPortsBound;
    }

    /**
     * Set whether an exception should be thrown if no ports are bound.
     */
    public final void setExceptionOnNoPortsBound(boolean exceptionOnNoPortsBound)
    {
        this.exceptionOnNoPortsBound = exceptionOnNoPortsBound;
    }

    /**
     * Should log status messages?  Default is true.
     */
    public boolean isLogStatus()
    {
        return logStatus;
    }

    /**
     * Set whether to log status messages.  Default is true.
     */
    public void setLogStatus(boolean logStatus)
    {
        this.logStatus = logStatus;
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
        catch (Throwable t)
        {
            System.err.println("GameServer error: " + Utils.formatExceptionText(t));
            throw new ApplicationError(t);
        }
    }

    /**
     * standard init of ConfigFiles, logger, DDMailQueue
     */
    protected void initConfig()
    {
        // Use the server security provider.
        // Not used with change to Hibernate - commenting out as of DD Poker 3
        //SecurityUtils.setSecurityProvider(new ServerSecurityProvider());

        // init config stuff
        if (isConfigLoadRequired())
        {
            ApplicationError.assertNotNull(getAppName(), "Application name must be set");
            new ConfigManager(getAppName(), ApplicationType.SERVER);
        }
        else
        {
            setAppName(ConfigManager.getAppName());
        }

        // log version
        logger.info("Java version: " + System.getProperties().get("java.runtime.version"));
    }

    /**
     * Start up server
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    private void _init() throws IOException
    {
        // add destroy hook
        shutdown_ = new Shutdown();
        Runtime.getRuntime().addShutdownHook(shutdown_);

        // load config, init logger
        initConfig();

        // let servlet do config-based inits
        ApplicationError.assertNotNull(servlet_, "Servlet must be set");
        servlet_.afterConfigInit();

        // setup debugging
        DEBUG_ONLINE = servlet_.isDebugOn();

        // get startup settings and create thread pool
        SLEEP_UNAVAIL = PropertyConfig.getRequiredIntegerProperty("settings.server.noworker.sleep.millis");
        LOG_UNAVAIL = PropertyConfig.getRequiredIntegerProperty("settings.server.noworker.log.millis");
        LOG_STATUS = PropertyConfig.getRequiredIntegerProperty("settings.server.status.log.seconds") * 1000;
        int nThreads = PropertyConfig.getRequiredIntegerProperty("settings.server.threads");
        sPort_ = PropertyConfig.getRequiredStringProperty(sPortKey_);
        String ip = PropertyConfig.getStringProperty("settings.server.ip", null, false);
        bBindFailover_ = PropertyConfig.getBooleanProperty("settings.server.failover", false, false);
        nFailoverAttempts_ = PropertyConfig.getIntegerProperty("settings.server.failover.attempts", 2);
        String sSocketThreadClass = PropertyConfig.getStringProperty("settings.server.thread.class", SocketThread.class.getName(), false);

        // display info
        logger.info("Listening on port(s) " + sPort_ + ";  threads: " + nThreads);

        // record current time
        nLastLogTime_ = System.currentTimeMillis();

        // create a new Selector for use below
        selector_ = Selector.open();

        // init
        int nPort;
        String port = null;
        StringTokenizer ports = new StringTokenizer(sPort_, ",");
        ServerSocketChannel channel;
        ServerSocket socket;

        // get all IPs to bind to
        StringTokenizer ips = ip == null ? null : new StringTokenizer(ip, ",");
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
                    if (!bindLoopback && i.isLoopbackAddress()) continue;

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
            logger.info("Using all addresses (TCP)");
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
                    channel = ServerSocketChannel.open();
                    socket = channel.socket();

                    logger.info("Binding: " + i.getHostAddress() + ":" + nPort);

                    try
                    {
                        bind(socket, i, nPort);
                    }
                    catch (SocketException be)
                    {
                        logger.error("Unable to bind: " + Utils.getExceptionMessage(be));
                        continue;
                    }

                    // Keep track of channels
                    channels_.add(channel);

                    // default channel - first non loopback
                    if (defaultChannel_ == null || defaultChannel_.socket().getInetAddress().isLoopbackAddress())
                    {
                        defaultChannel_ = channel;
                    }

                    // set non-blocking mode
                    channel.configureBlocking(false);

                    // register with selector
                    channel.register(selector_, SelectionKey.OP_ACCEPT);
                }
            }
            catch (NumberFormatException nfe)
            {
                logger.error("Unable to parse: " + port);
            }
        }

        // make sure we have one valid port
        if (channels_.isEmpty() && exceptionOnNoPortsBound)
        {
            throw new ApplicationError(ErrorCodes.ERROR_SERVER_NO_PORTS,
                                       "No ports were bound", sPort_, null);
        }

        // store first port as preferred
        if (!channels_.isEmpty())
        {
            logger.info("Preferred TCP (online server) address set to " + Utils.getLocalAddressPort(getDefaultChannel()));
        }

        // create pool
        pool_ = new ThreadPool(this, nThreads, servlet_, sSocketThreadClass);
    }

    /**
     * Attempt bind, failover to next port if desired
     */
    private void bind(ServerSocket socket, InetAddress ia, int nPort) throws IOException
    {
        int nAttempts = nFailoverAttempts_;
        IOException e = null;
        for (int i = 0; i < nAttempts; i++)
        {
            try
            {
                socket.bind(new InetSocketAddress(ia, nPort));
                return;
            }
            catch (IOException e2)
            {
                if (!bBindFailover_) throw e2;
                logger.info("Failed binding to " + ia.getHostAddress() + ":" + nPort + ", trying port " + (nPort + 1));
                nPort++;
                e = e2;
            }
        }
        if (e != null) {
            throw e;
        }
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
        if (defaultChannel_ != null) return defaultChannel_.socket().getInetAddress().getHostAddress();
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
    public ServerSocketChannel getDefaultChannel()
    {
        return defaultChannel_;
    }

    /**
     * Get number of workers in the pool
     */
    public int getNumWorkerThreads()
    {
        return pool_.size();
    }

    /**
     * Increase pool size by given number
     */
    public void addWorkers(int nWorkers)
    {
        pool_.addWorkers(nWorkers);
    }

    /**
     * run method
     */
    public void run()
    {
        // our thread
        mainThread_ = Thread.currentThread();

        // vars
        List<Qentry> q;
        int n;

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

                // register any new sockets
                q = getRegisterQueue();
                if (q != null)
                {
                    selector_.selectNow(); // clear cancelled keys
                    for (Qentry entry : q)
                    {
                        try
                        {
                            registerChannel(entry.channel, entry.ops);
                        }
                        catch (IOException ioe)
                        {
                            logger.error("registerChannel error: " + Utils.formatExceptionText(ioe));
                        }

                    }
                }
            }
            catch (Throwable t)
            {
                logger.error("processing error: " + Utils.formatExceptionText(t));
            }
        }

        // close workers
        pool_.shutdown();
        pool_ = null;
    }

    /**
     * Shutdown thread for cleanup
     */
    private class Shutdown extends Thread
    {
        public Shutdown()
        {
            setName("GameServer-Shutdown");
        }

        public void run()
        {
            logger.debug("GameServer shutting down...");
            shutdown(false);
        }
    }

    /**
     * Stop the game server
     */
    public void shutdown()
    {
        shutdown(true);
    }

    /**
     * stop the game server, remove shutdown hook if directed to do so
     */
    protected void shutdown(boolean bRemoveHook)
    {
        // set done flag, remove shutdown hook
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

        // close all sockets to release ports
        for (ServerSocketChannel channel : channels_)
        {
            try
            {
                logger.info("GameServer closing " + Utils.getLocalAddressPort(channel));
                channel.socket().close();
            }
            catch (Throwable ignore)
            {
                //logger.debug("Caught exception shutting down socket: " + Utils.formatExceptionText(ignore));
            }
        }

        // cleanup servlet
        servlet_.destroy();

        logger.info("Gameserver Done.");
    }

    /**
     * Logic to process selected keys
     */
    private void processSelection()
    {
        // get an iterator over the set of selected keys
        Iterator<SelectionKey> iter = selector_.selectedKeys().iterator();

        // look at each key in the selected set
        while (iter.hasNext())
        {
            nNum_++;

            SelectionKey key = iter.next();

            try
            {
                // Is a new connection coming in?
                if (key.isAcceptable())
                {
                    if (DEBUG_ONLINE) logger.debug("[" + nNum_ + "] ACCEPTING NEW CONNECTION");

                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();

                    // socket options
                    if (Utils.TCPNODELAY) channel.socket().setTcpNoDelay(true);
                    channel.socket().setSoLinger(false, 0);
                    channel.socket().setSendBufferSize(64 * 1024);
                    channel.socket().setReceiveBufferSize(64 * 1024);

                    if (DEBUG_ONLINE) logger.debug("[" + nNum_ + "] ACCEPTED " + Utils.getIPAddress(channel));

                    // register for read; no need to wake since this in
                    // same thread that select() is called
                    registerChannel(channel, SelectionKey.OP_READ);

                    if (DEBUG_ONLINE) logger.debug("[" + nNum_ + "] REGISTERED " + Utils.getIPAddress(channel));
                }
                // is there data to read on this channel?
                else if (key.isReadable())
                {
                    processChannel(key, nNum_);
                }
                // write (testing)
                //else if (key.isWritable())
                //{
                //    logger.debug("[" + nNum_ + "] WRITEABLE " + ((SocketChannel)key.channel()).socket().getInetAddress().getHostAddress());
                //}
                else
                {
                    logger.debug("[" + nNum_ + "] NOTHING TO DO: key ready ops are " + key.readyOps());
                }
            }
            catch (IOException ioe)
            {
                logger.error("processSelection error: " + Utils.formatExceptionText(ioe));
            }

            // remove key from selected set, as it has been handled
            iter.remove();
        }

        // log status if enough time has passed (if we are waiting without
        // requests, we never get here, so we don't log when nothing happens)
        long nNow = System.currentTimeMillis();
        if ((nNow - nLastLogTime_) > LOG_STATUS)
        {
            nLastLogTime_ = nNow;
            logStatus();
        }
    }

    /**
     * Log status about state of server
     */
    private void logStatus()
    {
        if (!isLogStatus()) return;

        logger.info("STATUS:  available workers: " + pool_.getNumIdleWorkers() +
                    ",  hits: " + nHits_ +
                    ",  misses: " + nRunningNoWorkerCnt_);
        nRunningNoWorkerCnt_ = 0;
        nHits_ = 0;
    }

    /**
     * Register the given channel with the given selector for
     * the given operations of interest.  Queues the request
     * if from some other thread than the main GameServer thread.
     */
    public void registerChannel(SocketChannel channel, int ops)
            throws IOException
    {
        if (channel == null)
        {
            return;        // could happen
        }

        // queue if different thread
        if (Thread.currentThread() != mainThread_)
        {
            addToQueue(channel, ops);
            selector_.wakeup();
        }
        else
        {
            // set the new channel non-blocking
            channel.configureBlocking(false);
            channel.register(selector_, ops);
        }
    }

    /*
     * Add a message to the Queue
     */
    private void addToQueue(SocketChannel channel, int ops)
    {
        synchronized (registerQ_)
        {
            registerQ_.add(new Qentry(channel, ops));
        }
    }

    /**
     * Get current contents of queue and start new list
     */
    private synchronized List<Qentry> getRegisterQueue()
    {
        synchronized (registerQ_)
        {
            if (registerQ_.isEmpty()) return null; // avoid new object if empty

            List<Qentry> list = new ArrayList<>(registerQ_.size());
            list.addAll(registerQ_);
            registerQ_.clear();
            return list;
        }
    }

    /**
     * q entry
     */
    private static class Qentry
    {
        SocketChannel channel;
        int ops;

        Qentry(SocketChannel channel, int ops)
        {
            this.channel = channel;
            this.ops = ops;
        }
    }

    /**
     * close channel, first calling socketClosing()
     */
    public void closeChannel(SocketChannel channel)
    {
        if (channel == null) return;

        // notify
        try
        {
            socketClosing(channel);
        }
        catch (Throwable notUsed)
        {
            logger.warn("Ignored exception: " + Utils.formatExceptionText(notUsed));
        }

        // showdown output so remote client gets socket closed
        // (just calling close doesn't cut it apparently)
        // we don't shutdownInput() because that causes errors
        // on this side; and isn't necessary
        try
        {
            channel.socket().shutdownOutput();
        }
        catch (Throwable ignored)
        {
        }

        try
        {
            channel.socket().close();
        }
        catch (Throwable ignored)
        {
        }
    }

    /**
     * Called to notify subclass of imminent closure of
     * this thread's current socket channel - called before
     * socket().close() so IP address is still valid
     */
    protected void socketClosing(SocketChannel channel)
    {
    }

    /**
     * Get worker from pool to handle socket connection
     */
    private void processChannel(SelectionKey key, long nNum)
    {
        // get channel
        SocketChannel channel = (SocketChannel) key.channel();

        // get worker
        SocketThread worker = pool_.getWorker();

        if (worker == null)
        {
            nRunningNoWorkerCnt_++;

            // No threads available, do nothing, the selection
            // loop will keep calling this method until a
            // thread becomes available

            // warn if worker is null and sleep
            // to allow time for another thread to finish
            if (nElapsedNoWorkerTime_ >= LOG_UNAVAIL)
            {
                logger.warn("*** NO worker thread available for " + nElapsedNoWorkerTime_ + " millis " +
                            "(sleep is " + SLEEP_UNAVAIL + "), current ip=" + Utils.getIPAddress(channel));
                nElapsedNoWorkerTime_ = 0;
            }

            Utils.sleepMillis(SLEEP_UNAVAIL);

            nElapsedNoWorkerTime_ += SLEEP_UNAVAIL;

            return;
        }
        else
        {
            nElapsedNoWorkerTime_ = 0;
        }

        // hit count
        nHits_++;

        // we have a worker, so process it
        if (DEBUG_ONLINE) logger.debug("[" + nNum + "] READING " + Utils.getIPAddress(channel));

        // need to cancel key otherwise can't change blocking for replies
        key.cancel();

        // invoking this wakes up the worker thread then returns
        worker.processChannel(channel);
    }
}
