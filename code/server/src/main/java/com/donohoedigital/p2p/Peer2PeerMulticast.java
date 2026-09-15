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
/*
 * Peer2PeerMulticast.java
 *
 * Created on November 8, 2004, 7:44 PM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.DDByteArrayOutputStream;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DDMessageListener;

import java.net.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 *
 * @author  donohoe
 */
public class Peer2PeerMulticast implements Runnable
{
    static Logger logger = LogManager.getLogger(Peer2PeerMulticast.class);
    
    private static final int PACKET_SIZE = 49152; // 64K max, including overhead
    
    private Thread t_;
    private final int nPort_;
    private final String sIP_;
    private InetAddress ia_;
    private MulticastSocket ms_;
    private boolean bDone_;
    
    /** 
     * Creates a new instance of Peer2PeerMulticast 
     */
    public Peer2PeerMulticast() 
    {
        nPort_ = PropertyConfig.getRequiredIntegerProperty("settings.multicast.port");
        sIP_ = PropertyConfig.getRequiredStringProperty("settings.multicast.address");


        logger.info("Multicast using {}:{}", sIP_, nPort_);
        try {   
            // multicast address we listen to
            ia_ = InetAddress.getByName(sIP_);
            
            // create a multicast socket on the specified local port number
			ms_ = new MulticastSocket(nPort_);

			// set right interface
            NetworkInterface nic = setInterface();
            
            // join the group, set TTL
            ms_.setTimeToLive(32);
            ms_.joinGroup(new InetSocketAddress(ia_, nPort_), nic);
        } 
        catch (UnknownHostException uhe)
        {
            throw new ApplicationError(uhe);
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }

    /**
     * Fix for java.net.SocketException: Can't assign requested address
     * https://stackoverflow.com/questions/18747134/getting-cant-assign-requested-address-java-net-socketexception-using-ehcache
     */
    private NetworkInterface setInterface() throws SocketException {
        InetAddress lan = getLanAddress();
        if (lan != null) {
            NetworkInterface nic = NetworkInterface.getByInetAddress(lan);
            if (nic != null) {
                logger.info("Multicast interface {} ({})", nic.getName(), lan.getHostAddress());
                ms_.setNetworkInterface(nic);
                return nic;
            }
        }
        // null tells joinGroup() to use the interface chosen by the OS
        return null;
    }

    /**
     * Get the local LAN address, or null if none found.  Prefers the address the OS uses for
     * outbound traffic, because the first site-local address may belong to a virtual interface
     * (e.g., Docker's bridge100 on Mac), where multicast never reaches other clients.
     */
    public static InetAddress getLanAddress() {
        // connecting a UDP socket sends nothing; it just picks the outbound route
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress local = s.getLocalAddress();
            if (isLanAddress(local)) {
                NetworkInterface nic = NetworkInterface.getByInetAddress(local);
                if (nic != null && nic.supportsMulticast() && !nic.isPointToPoint()) {
                    return local;
                }
            }
        } catch (IOException ignored) {
            // no route (e.g., offline); fall back to scanning interfaces
        }

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                Enumeration<InetAddress> addresses = networkInterfaces.nextElement().getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (isLanAddress(inetAddress)) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException se) {
            logger.warn("Unable to list network interfaces: {}", se.getMessage());
        }
        return null;
    }

    private static boolean isLanAddress(InetAddress inetAddress) {
        return inetAddress instanceof Inet4Address
                && inetAddress.isSiteLocalAddress()
                && !inetAddress.isAnyLocalAddress()
                && !inetAddress.isLinkLocalAddress()
                && !inetAddress.isLoopbackAddress()
                && !inetAddress.isMulticastAddress();
    }

    /**
     * Start listening for messages (in a separate thread)
     */
    public void start()
    {        
        // start listen thread
        bDone_ = false;
        t_ = new Thread(this, "Peer2PeerMulticast-Receiver");
        t_.start();
    }
    
    /**
     * Stop listening
     */
    public void stop()
    {
        bDone_ = true;
        ms_.close();
        t_ = null;
    }
    
    /**
     * Send logic
     */
    public void send(DDMessage msg)
    {
        DDByteArrayOutputStream out = new DDByteArrayOutputStream(PACKET_SIZE);
        
        try {
            msg.write(out);
            ApplicationError.assertTrue(out.size() <= PACKET_SIZE, "Data overrun for message, " + out.size() +" exceeds " + PACKET_SIZE, msg);
            DatagramPacket dp = new DatagramPacket(out.getBuffer(), out.size(), ia_, nPort_);
            ms_.send(dp);
        }
        catch (IOException ioe) 
        {
            throw new ApplicationError(ioe);
        }
    }
    
    /**
     * Receiver logic
     */
    public void run() 
    {
        DatagramPacket dp = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        while (!bDone_)
        {
            try {
                // wait for message
                ms_.receive(dp);
                
                // in case we are done
                if (dp.getLength() == 0 || bDone_) continue;
                
                // decode DDMessage
                DDMessage msg = new DDMessage();
                ByteArrayInputStream is = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
                msg.read(is, dp.getLength());
                
                // testing
                //InetSocketAddress from = (InetSocketAddress) dp.getSocketAddress();
                //msg.setString("from", from.getAddress().getHostAddress());
                
                // notify listeners
                fireMessageReceived(msg);
            }
            catch (SocketException se)
            {
                if (!bDone_)
                {
                    String data = new String(dp.getData(), 0, dp.getLength());
                    logger.error("receive() socket error: [{}]; {}", Utils.getPrintableString(data, 1000), Utils.formatExceptionText(se));
                }
            }
            catch (IOException ioe)
            {
                String data = new String(dp.getData(), 0, dp.getLength());
                logger.error("receive() ioerror: [{}]; {}", Utils.getPrintableString(data, 1000), Utils.formatExceptionText(ioe));
            }        
            catch (Throwable t)
            {
                String data = new String(dp.getData(), 0, dp.getLength());
                logger.error("receive() error: [{}]; {}", Utils.getPrintableString(data, 1000), Utils.formatExceptionText(t));
            }
        }
        
        logger.info("Multicast done");
    }    
    
    // events
    protected  ArrayList listenerList = new ArrayList();
    
   /**
     * Adds a DDMessageListener to the list
     */
    public void addDDMessageListener(DDMessageListener listener) {
        if (listenerList.contains(listener)) return;
        listenerList.add(listener);
    }
    
    /**
     * Removes a listener from the list.
     */
    public void removeDDMessageListener(DDMessageListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Call each listener with the message received.
     */
    protected void fireMessageReceived(DDMessage msg) {
        for (int i = listenerList.size() - 1; i >= 0; i -= 1) {
                ((DDMessageListener)listenerList.get(i)).messageReceived(msg);
        }
    }
}

/** Logic to get network interfaces **/
//            Enumeration nets = NetworkInterface.getNetworkInterfaces();
//            NetworkInterface net;
//            while (nets.hasMoreElements())
//            {
//                net = (NetworkInterface) nets.nextElement();
//                Enumeration inet = net.getInetAddresses();
//                while (inet.hasMoreElements())
//                {
//                    InetAddress i = (InetAddress) inet.nextElement();
//                    logger.debug("net: " + net.getDisplayName() + " " + i);
//                }
//            }
