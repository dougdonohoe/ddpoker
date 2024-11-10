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
 * LanClientInfo.java
 *
 * Created on November 30, 2004, 2:58 PM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DataMarshal;

/**
 * Wrapper of DDMessage for lan client messages
 *
 * @author  donohoe
 */
public class LanClientInfo
{
    DDMessage data_;
    
    public static final String LAN_HOST_NAME = "hostname";
    public static final String LAN_PLAYER_NAME = "playername";
    public static final String LAN_TCPIP_ADDRESS = "ip";
    public static final String LAN_GUID = "guid";
    public static final String LAN_ALIVE_MILLIS = "alivemillis";
    public static final String LAN_GAME = "game";
    
    /**
     * comapre this to given LanClientInfo and return true
     * if they are equivalent for purposes of data display
     */
    public boolean isEquivalent(LanClientInfo info, LanControllerInterface controller)
    {
        if (info == null) return false;
        if (!getPlayerName().equals(info.getPlayerName())) return false;
        if (!getHostName().equals(info.getHostName())) return false;
        if (!getIP().equals(info.getIP())) return false;
        return controller.isEquivalentOnlineGame(this.getGameData(), info.getGameData());
    }
    /** 
     * Creates a new instance of LanClientInfo 
     * with the DDMessage source data
     */
    LanClientInfo(DDMessage data) {
        data_ = data;
    }
    
    /**
     * Create a new instance of LanClientInfo 
     * that is empty
     */
    LanClientInfo(int nCategory) {
        data_ = new DDMessage(nCategory);
    }
    
    public String toString()
    {
        return "LanClientInfo: " + data_;
    }
    
    public DDMessage getData()
    {
        return data_;
    }
    
    public String getKey()
    {
        return data_.getKey();
    }

    public void setCategory(int nCategory)
    {
        data_.setCategory(nCategory);
    }

    public int getCategory()
    {
        return data_.getCategory();
    }
    
    public void setCreateTimeStamp()
    {
        data_.setCreateTimeStamp();
    }
    
    /**
     * Return millis portion of create time stamp
     */
    public long getCreateTime()
    {
        return Utils.getMillisFromTimeStamp(data_.getCreateTimeStamp());
    }
    
    public String getHostName()
    {
        return data_.getString(LAN_HOST_NAME, "[no host]");
    }
    
    public void setHostName(String s)
    {
        data_.setString(LAN_HOST_NAME, s);
    }
    
    public String getPlayerName()
    {
        return data_.getString(LAN_PLAYER_NAME, "[no name]");
    }
    
    public void setPlayerName(String s)
    {
        data_.setString(LAN_PLAYER_NAME, s);
    }
    
    public String getIP()
    {
        return data_.getString(LAN_TCPIP_ADDRESS, "[no ip]");
    }
    
    public void setIP(String s)
    {
        data_.setString(LAN_TCPIP_ADDRESS, s);
    }
    
    public String getGuid()
    {
        return data_.getString(LAN_GUID);
    }
    
    public void setGuid(String s)
    {
        data_.setString(LAN_GUID, s);
    }
    
    public long getAliveMillis()
    {
        return data_.getLong(LAN_ALIVE_MILLIS, 0);
    }
    
    public void setAliveMillis(long s)
    {
        data_.setLong(LAN_ALIVE_MILLIS, s);
    }
    
    public DataMarshal getGameData()
    {
        return (DataMarshal) data_.getObject(LAN_GAME);
    }
    
    public void setGameData(DataMarshal s)
    {
        data_.setObject(LAN_GAME, s);
    }
}
