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
package com.donohoedigital.games.poker.network;

import com.donohoedigital.comms.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 7, 2006
 * Time: 10:58:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class OnlinePlayerInfo implements Comparable<OnlinePlayerInfo>
{
    private DMTypedHashMap data_;

    public static final String ONLINE_NAME = "name";
    public static final String ONLINE_ALIASES = "aliases";
    public static final String ONLINE_KEY = "publickey";
    public static final String ONLINE_CREATED = "created";

    /**
     * Creates an uninitialized instance of WanGame
     */
    public OnlinePlayerInfo()
    {
        data_ = new DMTypedHashMap();
    }

    /**
     * Creates a new instance of WanGame
     * with the given source data
     */
    public OnlinePlayerInfo(DMTypedHashMap data)
    {
        data_ = data;
    }

    /**
     * get data
     */
    public DMTypedHashMap getData()
    {
        return data_;
    }

    /**
     * Create time millis
     */
    public void setCreateDate(Long l)
    {
        data_.setLong(ONLINE_CREATED, l);
    }

    /**
     * Create time from date
     */
    public void setCreateDate(Date d)
    {
        data_.setLong(ONLINE_CREATED, d.getTime());
    }

    /**
     * Get create time millis
     */
    public long getCreateDate()
    {
        return data_.getLong(ONLINE_CREATED, 0);
    }

    /**
     * get name
     */
    public String getPublicUseKey()
    {
        return data_.getString(ONLINE_KEY);
    }

    /**
     * Set name
     */
    public void setPublicUseKey(String s)
    {
        data_.setString(ONLINE_KEY, s);
    }

    /**
     * get name
     */
    public String getName()
    {
        return data_.getString(ONLINE_NAME);
    }

    /**
     * get name in lowercase for comparisons
     */
    public String getNameLower()
    {
        return getName().toLowerCase();
    }

    /**
     * Set name
     */
    public void setName(String s)
    {
        data_.setString(ONLINE_NAME, s);
    }

    /**
     * Get player list as array of OnlinePlayerInfo (constructed each time, so caller should cache)
     */
    public List<OnlinePlayerInfo> getAliases()
    {
        OnlinePlayerInfo info;
        DMArrayList<?> raw = (DMArrayList<?>) data_.getList(ONLINE_ALIASES);
        if (raw == null) return null;

        List<OnlinePlayerInfo> list = new ArrayList<OnlinePlayerInfo>(raw.size());
        for (Object aRaw : raw)
        {
            info = new OnlinePlayerInfo((DMTypedHashMap) aRaw);
            info.setPublicUseKey(getPublicUseKey());
            list.add(info);
        }
        return list;
    }

    /**
     * set aliases
     */
    public void setAliases(DMArrayList<DMTypedHashMap> aliases)
    {
        data_.setList(ONLINE_ALIASES, aliases);
    }

    /**
     * equals
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof OnlinePlayerInfo)
        {
            OnlinePlayerInfo info = (OnlinePlayerInfo) o;
            return getNameLower().equals(info.getNameLower()) && getPublicUseKey().equals(info.getPublicUseKey());
        }
        return false;
    }

    /**
     * hashcode
     */
    @Override
    public int hashCode()
    {
        return 31 * getNameLower().hashCode() + getPublicUseKey().hashCode();
    }

    /**
     * comparator for list sorting
     */
    public int compareTo(OnlinePlayerInfo info)
    {
        return getNameLower().compareTo(info.getNameLower());
    }
}
