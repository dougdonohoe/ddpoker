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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.comms.*;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
public class Peer2PeerMulticast implements Runnable
{
    static Logger logger = LogManager.getLogger(Peer2PeerMulticast.class);
    
    private static int PACKET_SIZE = 49152; // 64K max, including overhead
    
    private Thread t_;
    private int nPort_;
    private String sIP_;
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


        logger.info("Multicast using " + sIP_ + ":"+nPort_);
        try {   
            // multicast address we listen to
            ia_ = InetAddress.getByName(sIP_);
            
            // create a multicast socket on the specified local port number
			ms_ = new MulticastSocket(nPort_);

			// set right interface
            setInterface();
            
            // join the group, set TTL
            ms_.setTimeToLive(32);
            ms_.joinGroup(ia_);
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
    private void setInterface() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addressesFromNetworkInterface = networkInterface.getInetAddresses();
            while (addressesFromNetworkInterface.hasMoreElements()) {
                InetAddress inetAddress = addressesFromNetworkInterface.nextElement();
                if (inetAddress.isSiteLocalAddress()
                        && !inetAddress.isAnyLocalAddress()
                        && !inetAddress.isLinkLocalAddress()
                        && !inetAddress.isLoopbackAddress()
                        && !inetAddress.isMulticastAddress()) {
                    ms_.setNetworkInterface(NetworkInterface.getByName(networkInterface.getName()));
                    //logger.debug("Setting " + networkInterface);
                    return;
                } else {
                    //logger.debug("Not setting " + networkInterface);
                }
            }
        }
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
                    logger.error("receive() socket error: [" + Utils.getPrintableString(data, 1000) + "]; "
                             + Utils.formatExceptionText(se));
                }
            }
            catch (IOException ioe)
            {
                String data = new String(dp.getData(), 0, dp.getLength());
                logger.error("receive() ioerror: [" + Utils.getPrintableString(data, 1000) + "]; "
                             + Utils.formatExceptionText(ioe));
            }        
            catch (Throwable t)
            {
                String data = new String(dp.getData(), 0, dp.getLength());
                logger.error("receive() error: [" + Utils.getPrintableString(data, 1000) + "]; "
                             + Utils.formatExceptionText(t));
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
