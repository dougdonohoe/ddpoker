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
 * UnicastTest2.java
 *
 * Created on October 13, 2005, 8:55 AM 
 */

package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.udp.*;
import org.apache.log4j.*;

/**
 * @author Doug Donohoe
 */
public class UnicastTest2 extends BaseCommandLineApp implements UDPLinkHandler, UDPManagerMonitor, UDPLinkMonitor
{
    // logging
    private Logger logger;

    // members
    private boolean bSend;
    private boolean bDebug;
    private int port;

    /**
     * Run emailer
     */
    public static void main(String[] args)
    {
        try
        {
            Prefs.setRootNodeName("poker3");
            new UnicastTest2("poker", args);
        }

        catch (ApplicationError ae)
        {
            System.err.println("UnicastTest2 ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
        catch (Throwable t)
        {
            System.err.println("UnicastTest2 ending due to ApplicationError: " + Utils.formatExceptionText(t));
            System.exit(1);
        }
    }

    /**
     * Can be overridden for application specific options
     */
    protected void setupApplicationCommandLineOptions()
    {
        CommandLine.addFlagOption("send");
        CommandLine.setDescription("send", "send message");

        CommandLine.addIntegerOption("num", 1);
        CommandLine.setDescription("num", "# messages", "1");

        CommandLine.addIntegerOption("sleep", 0);
        CommandLine.setDescription("sleep", "millis", "25");

        CommandLine.addIntegerOption("step", 1);
        CommandLine.setDescription("step", "# messages before sleep", "50");

        CommandLine.addStringOption("ip", null);
        CommandLine.setDescription("ip", "ip address", "127.0.0.1");

        CommandLine.addIntegerOption("port", 11889);
        CommandLine.setDescription("port", "port", "11889");

        CommandLine.addFlagOption("nodebug");
        CommandLine.setDescription("nodebug", "no debug output");
    }

    UDPServer udp_;

    /**
     * Create War from config file
     */
    public UnicastTest2(String sConfigName, String[] args)
    {
        super(sConfigName, args);

        // init
        logger = Logger.getLogger(getClass());
        bSend = htOptions_.getBoolean("send", false);
        bDebug = !htOptions_.getBoolean("nodebug", false);
        port = htOptions_.getInteger("port");

        // start UDP server
        UDPServer.setDebugFlags();
        UDPServer.DEBUG_OUTGOING = false;
        UDPServer.DEBUG_INCOMING = false;
        UDPServer.DEBUG_ACKS_IN_DETAIL = false;
        //UDPServer.DEBUG_ACKS_OUT = true;
        Perf.setOn(true);
        Perf.start();
        udp_ = new UDPServer(this, true, true, Integer.toString(port));
        udp_.init();
        udp_.manager().addMonitor(this);
        udp_.start();

        // start sending messages
        if (bSend) send();
    }


    private String DEST_HOST = "127.0.0.1";

    /**
     * Test messages
     */
    public void send()
    {
        DEST_HOST = htOptions_.getString("ip", DEST_HOST);

        //int port = PropertyConfig.getRequiredIntegerProperty("settings.udp.port"); // get port on which server should be running

        //linkAckTest();
        UDPLink link = udp_.manager().getLink(DEST_HOST, port);

        if (bDebug)
        {
            logger.debug("MTU: " + link.getMTU());
            logger.debug("MAX PAYLOAD: " + link.getMaxPayloadSize() + " (header: " + UDPLink.IP_UDP_HEADERS + ")");
            logger.debug("MAX MESSAGE: " + link.getMaxMessageSize() + " (header: " + UDPMessage.HEADER_SIZE + ")");
            logger.debug("MAX DATA: " + link.getMaxDataSize() + " (header: " + UDPData.HEADER_SIZE + ")");
            logger.debug("MAX TOTAL: " + (link.getMaxDataSize() * UDPData.MAX_PARTS));
        }

        link.connect();
    }

    private class RunTest extends Thread
    {
        UDPLink link;

        public RunTest(UDPLink link)
        {
            setName("RunTest");
            this.link = link;
        }

        public void run()
        {
            int nNum = htOptions_.getInteger("num", 1);
            int nSleep = htOptions_.getInteger("sleep", 0);
            int nStep = htOptions_.getInteger("step", 1);

            if (bDebug)
            {
                logger.debug("Max long: " + Long.MAX_VALUE + " Max int: " + Integer.MAX_VALUE);
                logger.debug("DATA_SIZE: " + (link.getMaxDataSize()) + " max size: " + (link.getMaxDataSize()) * UDPData.MAX_PARTS);
            }

            StringBuilder sb = new StringBuilder();
            boolean ADD_EXTRA = true;
            boolean MULTI_TEST = false;

            // normal message
            if (true)
            {
                for (int i = 0; i < nNum; i++)
                {
                    sb.setLength(0);
                    sb.append("This is message for the test server to process and handle and stuff " + (i + 1));
                    if (ADD_EXTRA)
                    {
                        sb.append(" ");
                        int size = sb.length();
                        size++; // account for tilde at end
                        for (int j = 0; j < (MULTI_TEST ? ((link.getMaxDataSize() - size) * 2) : ((link.getMaxDataSize() - size))); j++)
                        {
                            sb.append((char) ('a' + (j % 26)));
                        }
                        sb.append("~");
                    }
                    //logger.debug("Message: "+ sb);
                    if (i % 100 == 0) logger.debug("UnicastTest queueing message: " + (i + 1));
                    link.queue(Utils.encode(sb.toString()));
                    if (i % nStep == 0 && nSleep > 0) Utils.sleepMillis(nSleep);
                }
            }
            // message of increasing size
            else
            {
                for (int i = link.getMaxDataSize() - 10; i < (link.getMaxDataSize() * 3 + 10); i++)
                {
                    sb.setLength(0);
                    for (int j = 0; j < i; j++)
                    {
                        sb.append((char) ('a' + (j % 26)));
                    }
                    link.queue(Utils.encode(sb.toString()));
                    if (i % nStep == 0 && nSleep > 0) Utils.sleepMillis(nSleep);
                }
            }

            while (link.queueSize() > 0)
            {
                Utils.sleepMillis(100);
            }

            link.close();
        }
    }

    /**
     * stats
     */
    private void logStats(UDPLink link)
    {
        logger.debug("Stats: " + link.getStats());
        MovingAverage in = udp_.manager().getBytesInMovingAverage();
        MovingAverage out = udp_.manager().getBytesOutMovingAverage();

        logger.debug("Bytes In: " + Utils.formatSizeBytes(in.getAverageLong()) + "/sec" +
                     " (" + Utils.formatSizeBytes(in.getHigh()) + " high - " + Utils.formatSizeBytes(in.getPeak()) + " peak)" +
                     ",  Bytes Out: " + Utils.formatSizeBytes(out.getAverageLong()) + "/sec" +
                     " (" + Utils.formatSizeBytes(out.getHigh()) + " high - " + Utils.formatSizeBytes(out.getPeak()) + " peak)");
    }

    /**
     * UDPLink test
     */
    public void linkAckTest()
    {
        AckList list = new AckList(System.currentTimeMillis());
        list.ackTest(false, 10000, 7);
    }

    ////
    //// UDPLinkHandler
    ////

    public int getTimeout(UDPLink link)
    {
        return 5000;
    }

    public int getPossibleTimeoutNotificationInterval(UDPLink link)
    {
        return 1000;
    }

    public int getPossibleTimeoutNotificationStart(UDPLink link)
    {
        return 2000;
    }

    ////
    //// UDPManagerMonitor
    ////

    public void monitorEvent(UDPManagerEvent event)
    {
        UDPLink link = event.getLink();

        switch (event.getType())
        {
            case CREATED:
                if (bDebug) logger.debug("Created: " + Utils.getAddressPort(link.getRemoteIP()));
                link.addMonitor(this);
                break;

            case DESTROYED:
                if (bDebug) logger.debug("Destroyed: " + Utils.getAddressPort(link.getRemoteIP()));
                link.removeMonitor(this);
                if (bSend)
                {
                    Thread t = new Thread("DoShutdown")
                    {
                        public void run()
                        {
                            udp_.shutdown();
                        }
                    };
                    t.start();
                }
                break;
        }
    }

    ////
    //// UDPLinkMonitor
    ////

    int CNT = 0;

    public void monitorEvent(UDPLinkEvent event)
    {
        UDPLink link = event.getLink();
        long elapsed = event.getElapsed();
        UDPData data = event.getData();

        switch (event.getType())
        {
            case MTU_TEST_FINISHED:
                if (bSend) new RunTest(link).start();
                break;

            case ESTABLISHED:
                if (bDebug) logger.debug("EVENT Established: " + Utils.getAddressPort(link.getRemoteIP()));
                break;

            case CLOSING:
                if (bDebug) logger.debug("EVENT Closing: " + Utils.getAddressPort(link.getRemoteIP()));
                break;

            case CLOSED:
                if (bDebug) logger.debug("EVENT Closed: " + Utils.getAddressPort(link.getRemoteIP()));
                logStats(link);
                break;

            case POSSIBLE_TIMEOUT:
                if (bDebug) logger.debug("EVENT Possible timeout on " + Utils.getAddressPort(link.getRemoteIP()) +
                                         " (no message in last " + elapsed + " millis)");
                break;

            case TIMEOUT:
                if (bDebug) logger.debug("EVENT Timeout on " + Utils.getAddressPort(link.getRemoteIP()) +
                                         " (no message in last " + elapsed + " millis)");
                break;

            case RESEND_FAILURE:
                if (bDebug) logger.debug("EVENT Resend Failure on " + Utils.getAddressPort(link.getRemoteIP()) +
                                         " (unable to send message " + data + ")");
                break;

            case RECEIVED:
                if ((bDebug || (CNT++ % 100 == 0 && data.getType() == UDPData.Type.MESSAGE)))
                {
                    String sMsg = "";
                    if (data.getType() == UDPData.Type.MESSAGE)
                    {
                        if (bDebug)
                            sMsg = " {" + Utils.decode(data.getData(), data.getOffset(), data.getLength()) + "}";
                        else sMsg = "";

                    }

                    logger.debug("EVENT msg from " + Utils.getAddressPort(link.getRemoteIP()) + ": " + data.toString() + sMsg);
                }
                break;
        }
    }
}
