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
 * LanClientList.java
 *
 * Created on November 15, 2004, 12:34 PM
 */

package com.donohoedigital.p2p;

import org.apache.logging.log4j.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
public class LanClientList
{
    static Logger logger = LogManager.getLogger(LanClientList.class);

    public static final int LAN_HELLO = 10000;
    public static final int LAN_ALIVE = 10001;
    public static final int LAN_GOODBYE = 10002;
    public static final int LAN_UPDATE = 10003;
    public static final int LAN_TIMEOUT = 10004;
    public static final int LAN_REFRESH = 10005;

    /**
     * Process received message.  Caller ensures that the message
     * received is valid.
     */
    public static String toString(int nCategory)
    {
        switch (nCategory)
        {
            case LAN_HELLO: return "hello";
            case LAN_ALIVE: return "alive";
            case LAN_GOODBYE: return "goodbye";
            case LAN_UPDATE: return "update";
            case LAN_TIMEOUT: return "timeout";
            case LAN_REFRESH: return "refresh";
            default:
                return "[unknown category: " + nCategory + "]";
        }
    }

    private Map<String, LanClientInfo> list_ = new HashMap<String, LanClientInfo>();
    private LanControllerInterface controller_;
    
    
    /** 
     * Creates a new instance of LanClientList 
     */
    public LanClientList(LanControllerInterface controller) {
        controller_ = controller;
    }
    
    /**
     * Process received message.  Caller ensures that the message
     * received is valid.
     */
    public synchronized void process(LanClientInfo msg)
    {   
        switch (msg.getCategory())
        {
            case LAN_HELLO:            
                add(msg);
                break;
                
            case LAN_ALIVE:
            case LAN_REFRESH:
                alive(msg);
                break;
                
            case LAN_GOODBYE:
                remove(msg);
                break;
                
            default:
                logger.warn("Received message with incorrect category: " + msg);
                return;
        }
    }
    
    /**
     * debug representation of list
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        LanClientInfo msg;
        for (String sKey : list_.keySet())
        {
            n++;
            msg = get(sKey);
            sb.append("Entry #").append(n).append(" (").append(sKey).append("): ").append(msg).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Add to list
     */
    private void add(LanClientInfo msg)
    {
        String sKey = msg.getKey();
        LanClientInfo old = get(sKey);
        
        // if existing, update it
        if (old != null) {
            msg.setCategory(LAN_ALIVE);
            alive(msg);
            return;
        }
        
        // store new
        msg.setCreateTimeStamp();
        list_.put(sKey, msg);
        
        // notify
        fireAction(LAN_HELLO, sKey);
    }
    
    /**
     * alive update
     */
    private void alive(LanClientInfo msg)
    {
        String sKey = msg.getKey();
        LanClientInfo old = get(sKey);
        
        // if no existing, then do an add
        if (old == null)
        {
            msg.setCategory(LAN_HELLO);
            add(msg);
            return;
        }
        
        // store update
        msg.setCreateTimeStamp();
        list_.put(sKey, msg);
        
        // notify depends on whether the incoming
        // message is different than the existing one
        fireAction(old.isEquivalent(msg, controller_) ? LAN_ALIVE : LAN_UPDATE, sKey);
    }
    
    /**
     * delete from list
     */
    private void remove(LanClientInfo msg)
    {
        String sKey = msg.getKey();
        Object o = list_.remove(sKey);
        if (o != null)
        {
            fireAction(LAN_GOODBYE, sKey);
        }
    }
    
    /**
     * remove entries that have not been updated in given seconds,
     * issue a LAN_TIMEOUT for the key
     */
    public synchronized void timeoutCheck(int nSeconds)
    {
        long millis = nSeconds * 1000;
        long now = System.currentTimeMillis();
        Iterator<String> keys = list_.keySet().iterator();
        String sKey;
        LanClientInfo info;
        while (keys.hasNext())
        {
            sKey = keys.next();
            info = get(sKey);
            if (now - info.getCreateTime() > millis)
            {
                keys.remove();
                fireAction(LAN_TIMEOUT, sKey);
            }
        }
    }
    
    /**
     * Get entry for key
     */
    public synchronized LanClientInfo get(String sKey)
    {
        return list_.get(sKey);
    }
    
    /**
     * Get keys that comprise list
     */
    public synchronized Iterator<String> keys()
    {
        return list_.keySet().iterator();
    }
    
    /**
     * Return this list as an Array List sorted by given data
     * field (constant in LanClientInfo) and whether ascending or not
     */
    public synchronized List<LanClientInfo> getAsList(String sSortKey, boolean bAscending)
    {
        List<LanClientInfo> list = new ArrayList<LanClientInfo>(list_.values());
        Collections.sort(list, new LanSorter(sSortKey, bAscending));
        return list;
    }
    
    /**
     * Sort class
     */
    private static class LanSorter implements Comparator<LanClientInfo>
    {
        private String sSortKey;
        private boolean bAscending;
        
        public LanSorter(String sSortKey, boolean bAscending)
        {
            this.sSortKey = sSortKey;
            this.bAscending = bAscending;
        }
        
        public int compare(LanClientInfo d1, LanClientInfo d2)
        {
            String s1 = d1.getData().getString(sSortKey, "");
            String s2 = d2.getData().getString(sSortKey, "");
            
            if (bAscending)
                return s1.compareToIgnoreCase(s2);
            else
                return s2.compareToIgnoreCase(s1);
        }
        
    }
    
    ////
    //// LanListener stuff
    ////
    
    // listener list
    protected List<LanListener> listenerList = new ArrayList<LanListener>();
    
   /**
     * Adds a listener to the list
     */
    public void addLanListener(LanListener listener) {
        if (listenerList.contains(listener)) return;
        listenerList.add(listener);
    }
    
    /**
     * Removes a listener from the list.
     */
    public void removeLanListener(LanListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Call each listener with the message received.
     */
    protected void fireAction(int nActionID, String sKey) {
        LanEvent event = new LanEvent(this, nActionID, sKey);
        for (int i = listenerList.size() - 1; i >= 0; i -= 1) {
                listenerList.get(i).lanEventReceived(event);
        }
    }
    
}
