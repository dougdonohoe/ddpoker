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
import org.apache.log4j.*;
import com.donohoedigital.html.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 4, 2006
 * Time: 11:04:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class UDPManager extends Thread implements Comparator<UDPLink>
{
    static Logger logger = Logger.getLogger(UDPManager.class);

    // wake up every X millis to send acks or resend
    static int ACK_SEND_MILLIS = 333;

    // members
    private UDPServer server_;
    private UDPLinkHandler handler_;
    private final List<UDPManagerMonitor> monitors_ = new ArrayList<UDPManagerMonitor>();
    private final LinkedBlockingQueue<Object> queue_ = new LinkedBlockingQueue<Object>();
    private final List<UDPLink> links_ = Collections.synchronizedList(new ArrayList<UDPLink>());
    private List<UDPLink> linksCopy_ = Collections.synchronizedList(new ArrayList<UDPLink>());
    boolean bDone_ = false;
    private Timer timer_;

    // control messages
    private Object QUIT = new Object();
    private Object SENDALL = new Object();
    private Object SENDACK = new Object();

    /**
     * new udp manager
     * @param server
     */
    public UDPManager(UDPServer server)
    {
        super("UDPManager");
        server_ = server;
        handler_ = server_.handler();
        timer_ = new Timer("UDPTimer", false);
    }

    /**
     * return our server
     */
    public UDPServer server()
    {
        return server_;
    }

    /**
     * return handler
     */
    public UDPLinkHandler handler()
    {
        return handler_;
    }

    /**
     * Add a monitor
     */
    public void addMonitor(UDPManagerMonitor monitor)
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
    public void removeMonitor(UDPManagerMonitor monitor)
    {
        synchronized(monitors_)
        {
            monitors_.remove(monitor);
        }
    }

    /**
     * fire event
     */
    private void fireEvent(UDPManagerEvent event)
    {
        // copy to avoid deadlock situations
        UDPManagerMonitor[] mons = null;
        UDPManagerMonitor mon = null;
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
                mons = new UDPManagerMonitor[nNum];
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
            for (UDPManagerMonitor monitor : mons)
            {
                fireEvent(monitor, event);
            }
        }
    }

    /**
     * fire event
     */
    private void fireEvent(UDPManagerMonitor monitor, UDPManagerEvent event)
    {
        // notify hanlder of new message
        try {
            monitor.monitorEvent(event);
        } catch (Throwable t)
        {
            logger.error("Monitor error on event "+event+ ": "+ Utils.formatExceptionText(t));
        }
    }

    /**
     * thread start
     */
    public void run()
    {
        // create timer events to have us wake up and send
        timer_.scheduleAtFixedRate(new SendAckTask(), ACK_SEND_MILLIS, ACK_SEND_MILLIS);
        timer_.scheduleAtFixedRate(new StatTask(), STAT_INTERVAL_MILLIS, STAT_INTERVAL_MILLIS);

        // we use a blocking queue as our work "to do" list.   It blocks until another thread
        // puts something on the queue.  We put several types of Objects.  The QUIT and SENDALL instances are
        // used to respectively quit (cleanup) and send all queued messages on each link.  A UDPLink on the
        // work queue means to send messages to that queue.  A UDPMessage on the queue means it is an incoming
        // message that should be handled by the UDPLink
        Object msg;
        while (!bDone_)
        {
            try {
                msg = queue_.take();
                if (msg == QUIT || bDone_) { /* continue */ }
                else if (msg == SENDALL) processSendAll();
                else if (msg == SENDACK) processSendAcksPing();
                else if (msg instanceof UDPMessage) processMessage((UDPMessage) msg);
                else if (msg instanceof UDPLink) processSend((UDPLink) msg);
                else if (msg instanceof CloseLink) processRemove((CloseLink) msg);
            }
            catch(InterruptedException e)
            {
                interrupted();
            }
            catch (Throwable t)
            {
                logger.error("UDPManager error: " + Utils.formatExceptionText(t));
            }
        }
        logger.info("UDPManager Done.");
    }

    /**
     * cleanup
     */
    public void finish()
    {
        //logger.info("Stopping UDPManager...");

        // set done flag and send quit message to wakeup queue (if it is waiting on something)
        bDone_ = true;
        timer_.cancel();

        // remove all links
        removeAll();

        // stop queue
        quit();

        // wait for thread to complete
        try {
            join(5000);
        } catch (InterruptedException ignored) {}
    }

    /**
     * add message to queue
     */
    void addMessage(UDPMessage msg)
    {
        add(msg);
    }

    /**
     * add UDPLink to queue (means we should send its messages)
     */
    public void addLinkToSend(UDPLink link)
    {
        add(link);
    }

    /**
     * add object to UDPManager work queue
     */
    private void add(Object o)
    {
        try {
            queue_.put(o);
        }
        catch (InterruptedException e)
        {
            // happens if queue gets full, which is unlikely.
            // if it does happen, just ignore - the message will be resent
        }
    }

    /**
     * To quit, add a QUIT message to the queue
     */
    private void quit()
    {
        add(QUIT);
    }

    /**
     * remove link
     */
    void remove(UDPLink link)
    {
        add(new CloseLink(link));
    }

    /**
     * class for closing a link to avoid concurrent modification exception
     */
    private class CloseLink
    {
        UDPLink link;

        CloseLink(UDPLink link)
        {
            this.link = link;
        }
    }

    /**
     * time task
     */
    private class SendAckTask extends TimerTask
    {
        int nCnt;

        public void run()
        {
            nCnt++;
            if (nCnt % 3 == 0)
            {
                add(SENDALL);
            }
            else
            {
                add(SENDACK);
            }
        }
    }

    /**
     * stat task
     */
    private class StatTask extends TimerTask
    {
        public void run()
        {
            long bytesIn = 0;
            long bytesOut = 0;

            synchronized(links_)
            {
                for (UDPLink link : links_)
                {
                    bytesIn += link.getStats().getBytesInCheckpoint();
                    bytesOut += link.getStats().getBytesOutCheckpoint();
                }
            }

            bytesIn_.record(bytesIn);
            bytesOut_.record(bytesOut);
        }
    }

    /**
     * process message, return true if done message
     */
    private void processMessage(UDPMessage msg)
    {
        // Get link for this message.  This will create a new link if this is a new message unless the
        // message contains a GOODBYE UDPData or is only acks, in which case we ignore it (it may be a GOODBYE/acks arriving
        // after the link was closed)
        UDPLink link = getLink(msg.getSourceID(), msg.getDestinationIP(), msg.getSourceIPApparent(), !msg.requiresExistingLink());
        if (link != null && !link.isDone())
        {
            link.processMessage(msg);
        }
    }

    /**
     * send messages pending on all links
     */
    private void processSend(UDPLink link)
    {
        link.send();
    }

    /**
     * resend any messages
     */
    private void processSendAll()
    {
        List<UDPLink> copy = copyLinks();
        for (UDPLink link : copy)
        {
            link.sendAll();
        }
    }

    /**
     * send ack messages (also acts as a ping)
     */
    private void processSendAcksPing()
    {
        List<UDPLink> copy = copyLinks();
        for (UDPLink link : copy)
        {
            link.sendAcksPing();
        }
    }


    /**
     * get link to another server
     */
    public UDPLink getLink(String sIP, int nPort)
    {
        return getLink(new InetSocketAddress(sIP, nPort));
    }

    /**
     * get link to another server
     */
    public UDPLink getLink(UDPID to, String sIP, int nPort)
    {
        return getLink(to, server_.getIP(server_.getDefaultChannel()), new InetSocketAddress(sIP, nPort), true);
    }

    /**
     * get link to another server
     */
    public UDPLink getLink(InetSocketAddress remote)
    {
        return getLink(null, server_.getIP(server_.getDefaultChannel()), remote, true);
    }

    /**
     * Get link
     */
    public UDPLink getLink(UDPID id)
    {
        return getLink(id, null, null, false);
    }

    /**
     * Get link.  If no matching link create a new link if bCreateNew is true.
     */
    private UDPLink getLink(UDPID id, InetSocketAddress local, InetSocketAddress remote, boolean bCreateNew)
    {
        if (id == null) id = UDPID.UNKNOWN_ID;

        synchronized (links_)
        {
            // TODO: this isn't efficient for large numbers of links, but for now it is okay
            for (UDPLink link : links_)
            {
                // if ID matches, local must be null (ID only lookup) or if local is specified, local must match
                // to ensure we get the right link
                if (!id.isUnknown() && link.getID().equals(id) &&
                    (local == null || link.getLocalIP().equals(local))) return link;

                // if we find a match for the remote, same check on local
                if (link.getRemoteIP().equals(remote) &&
                    (local == null || link.getLocalIP().equals(local)))
                {
                    // if the ID was previously unknown, this is the first response back ... so set the ID
                    if (link.getID().isUnknown())
                    {
                        if (!id.isUnknown())
                        {
                            link.setID(id);
                            if (UDPServer.DEBUG_CREATE_DESTROY) logger.debug("ID updated " + link);
                        }
                    }
                    // if the id is not unknown, it changed for some reason, so update (shouldn't really occur)
                    else if (!id.isUnknown())
                    {
                        // TODO: what to do if ID changes?  Will this actually occur?
                        logger.warn("ID changed at addr " + Utils.getAddressPort(remote) + " from " + link.getID() + " to " + id);
                        link.setID(id);
                    }
                    return link;
                }
                //
                // Note: if there happend to be a match on ID and/or remote and the local didn't match, then that
                // is a different connection - to a different port
            }

            UDPLink link = null;

            if (bCreateNew)
            {
                // no link found - a new one here
                link = new UDPLink(this, id, local, remote);
                links_.add(link);


                // notify of creation
                if (UDPServer.DEBUG_CREATE_DESTROY) logger.debug("New Link " + link);
                fireEvent(new UDPManagerEvent(UDPManagerEvent.Type.CREATED, link));
            }

            return link;
        }
    }

    /**
     * Get all links
     */
    public void getLinks(List<UDPLink> links)
    {
        synchronized(links_)
        {
            links.addAll(links_);
        }
    }

    /**
     * process remove link (generated from link itself)
     */
    private void processRemove(CloseLink cl)
    {
        synchronized(links_)
        {
            if (links_.remove(cl.link))
            {
                notifyRemoved(cl.link);
            }
            else
            {
                logger.warn("Link remove requested, but not found: "+ cl.link);
            }
        }
    }

    /**
     * remove all links
     */
    private void removeAll()
    {
        // cleanup links
        synchronized(links_)
        {
            while (links_.size() > 0)
            {
                UDPLink link = links_.remove(0);
                link.finish(false);
                notifyRemoved(link);
            }
        }
    }

    /**
     * notify handlers of a removed link
     */
    private void notifyRemoved(UDPLink link)
    {
        if (UDPServer.DEBUG_CREATE_DESTROY) logger.debug("Link removed: "+ link);
        fireEvent(new UDPManagerEvent(UDPManagerEvent.Type.DESTROYED, link));
    }

    /**
     * copy links to copylist for looping
     */
    private List<UDPLink> copyLinks()
    {
        synchronized(links_)
        {
            linksCopy_.clear();
            linksCopy_.addAll(links_);
            return linksCopy_;
        }
    }

    ////
    //// Stats - calculate bytes per second over last sample interval
    ////


    // calc stats interval - every second
    private static final int STAT_INTERVAL_MILLIS = 1000;

    MovingAverage bytesIn_ = new MovingAverage(5);
    MovingAverage bytesOut_ = new MovingAverage(5);

    /**
     * Get Bytes in moving average
     */
    public MovingAverage getBytesInMovingAverage()
    {
        return bytesIn_;
    }

    /**
     * Get Bytes out moving average
     */
    public MovingAverage getBytesOutMovingAverage()
    {
        return bytesOut_;
    }

    /**
     * Get bytes on outgoing queue to send
     */
    public long getBytesOnOutgoingQueue()
    {
        return server_.outgoing().getBytesOnQueue();
    }

    /**
     * Get msgs on outgoing queue to send
     */
    public int getMessageOnOutGoingQueue()
    {
        return server_.outgoing().getMessagesOnQueue();
    }

    /**
     * Get peak on outgoing queue
     */
    public int getPeakMessageOnOutGoingQueue()
    {
        return server_.outgoing().getPeakMessagesOnQueue();
    }

    /**
     * return udp status as html for given links
     */
    public String getStatusHTML(Comparator<UDPLink> comparator)
    {
        List<UDPLink> links = null;

        synchronized(links_)
        {
            links = new ArrayList<UDPLink>(links_.size());
            links.addAll(links_);
        }

        if (comparator == null) comparator = this;
        Collections.sort(links, comparator);

        // create table
        Table table  = new Table(3, 1);
        String sHeaderColor = "#CCCCFF";
        int num = links.size();
        table.addColumn(new TableColumn(num + (num == 1 ? " Player":" Players"), TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, sHeaderColor));
        table.addColumn(new TableColumn("IP", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, sHeaderColor));
        table.addColumn(new TableColumn("MTU", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Time", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Ping", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Send", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Resend", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Rcvd", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Dups", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Bytes-In", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));
        table.addColumn(new TableColumn("Bytes-Out", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, sHeaderColor));

        // add rows
        TableRow row;
        UDPLink.UDPStats stats;
        for (UDPLink link : links)
        {
            stats = link.getStats();
            row = new TableRow();
            table.addRow(row);
            row.addData(Utils.encodeHTML(link.getName()));
            row.addData(Utils.getAddressPort(link.getRemoteIP()));
            row.addData(""+link.getMTU());
            row.addData(Utils.getTimeString(link.getTimeConnected(), false));
            row.addData(""+stats.getAverage());
            row.addData(""+stats.getDataOut());
            row.addData(""+stats.getDataResend());
            row.addData(""+stats.getDataIn());
            row.addData(""+stats.getDataDups());
            row.addData(Utils.formatSizeBytes(stats.getBytesIn()));
            row.addData(Utils.formatSizeBytes(stats.getBytesOut()));
        }

        StringBuilder sb = table.toStringBuilder();
        MovingAverage in = getBytesInMovingAverage();
        MovingAverage out = getBytesOutMovingAverage();
        sb.append("In: ")
                .append(Utils.formatSizeBytes(in.getAverageLong())).append("/sec (")
                .append(Utils.formatSizeBytes(in.getHigh())).append(" high - ")
                .append(Utils.formatSizeBytes(in.getPeak())).append(" peak)").append(",&nbsp;&nbsp;")
                .append("Out: ")
                .append(Utils.formatSizeBytes(out.getAverageLong())).append("/sec (")
                .append(Utils.formatSizeBytes(out.getHigh())).append(" high - ")
                .append(Utils.formatSizeBytes(out.getPeak())).append(" peak) Queue: ")
                .append(Utils.formatSizeBytes(getBytesOnOutgoingQueue())).append(" (")
                .append(getMessageOnOutGoingQueue()).append(" msgs - ")
                .append(getPeakMessageOnOutGoingQueue()).append(" peak)");

        return sb.toString();
    }

    /**
     * link comparator - sort by name
     */
    public int compare(UDPLink link1, UDPLink link2)
    {
        return (link1.getName().compareToIgnoreCase(link2.getName()));
    }
}
