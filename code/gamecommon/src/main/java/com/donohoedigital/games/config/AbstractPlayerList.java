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
package com.donohoedigital.games.config;

import com.donohoedigital.base.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 3, 2008
 * Time: 3:09:29 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractPlayerList extends ArrayList<AbstractPlayerList.PlayerInfo>
{
    private static final String DELIM = "\n";


    public abstract String getName();

    /**
     * Add player to list
     */
    public void add(String sName, String sKey, boolean bSave)
    {
        if (sKey == null) sKey = "NoKey-"+sName;
        add(new PlayerInfo(sName, sKey), bSave);
    }

    /**
     * Add PlayerInfo
     */
    private void add(PlayerInfo info, boolean bSave)
    {
        int nPlace = Collections.binarySearch(this, info);
        if (nPlace < 0)
        {
            add(-(nPlace + 1), info); // insert sorted
            if (bSave) save();
        }
    }

    /**
     * remove player from list
     */
    public void remove(String sName, boolean bSave)
    {
        int nPlace = Collections.binarySearch(this, new PlayerInfo(sName, null));
        if (nPlace >= 0)
        {
            remove(nPlace);
            if (bSave) save();
        }
    }

    /**
     * Is this player in the list?
     * @param sName
     * @param sKey
     */
    public boolean containsPlayer(String sName, String sKey)
    {
        return containsPlayer(sName) || containsKey(sKey);
    }

    /**
     * Is this key in the list?
     */
    public boolean containsKey(String sKey)
    {
        PlayerInfo p;
        for (int i = size() - 1; i >= 0; i--)
        {
            p = get(i);
            if (p.sKey != null && p.sKey.equals(sKey)) return true;
        }
        return false;
    }

    /**
     * Is this player in the list?
     */
    public boolean containsPlayer(String sName)
    {
        int nPlace = Collections.binarySearch(this, new PlayerInfo(sName, null));
        return nPlace >= 0;
    }

    /**
     * update list from prefs
     */
    public void fetch()
    {
        clear();

        String data = fetchNames();
        if (data == null) return;
        String keys = fetchKeys();

        StringTokenizer st = new StringTokenizer(data, DELIM);
        StringTokenizer stk = keys == null ? null : new StringTokenizer(keys, DELIM); // check for null for back compat
        while (st.hasMoreTokens())
        {
            add(st.nextToken(), stk == null ? null : stk.nextToken(), false);
        }
    }

    protected abstract String fetchNames();

    protected abstract String fetchKeys();

    /**
     * save list to prefs
     */
    public void save()
    {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbk = new StringBuilder();
        for (int i = 0; i < size(); i++)
        {
            if (i > 0) sb.append(DELIM);
            sb.append(get(i).sName);

            if (i > 0) sbk.append(DELIM);
            sbk.append(get(i).sKey);
        }
        saveNames(sb.toString());
        saveKeys(sbk.toString());
    }

    protected abstract void saveNames(String sNames);

    protected abstract void saveKeys(String sKeys);

    /**
     * to String - CSV format
     */
    @Override
    public String toString()
    {
        return toCSV();
    }
    
    /**
     * to CSV
     */
    public String toCSV()
    {
        StringBuilder sb = new StringBuilder();
        PlayerInfo p;
        for (int i = 0; i < size(); i++)
        {
            p = get(i);
            if (i > 0) sb.append(", ");
            sb.append(Utils.encodeCSV(p.sName));
        }
        return sb.toString();
    }

    /**
     * from CSV - preserve existing name info
     */
    public void fromCSV(String sText, boolean bSave)
    {
        sText = sText.replace('\n', ',');
        List<PlayerInfo> keep = new ArrayList<PlayerInfo>(size());
        String sName;
        PlayerInfo search = new PlayerInfo(null, null);
        String[] names = CSVParser.parseLine(sText);
        for (String name : names)
        {
            sName = name.trim();
            if (sName.length() == 0) continue;
            search.sName = sName;
            int nPlace = Collections.binarySearch(this, new PlayerInfo(sName, null));
            if (nPlace >= 0)
            {
                keep.add(get(nPlace));
            }
            else
            {
                keep.add(new PlayerInfo(sName, null));
            }
        }

        // add saved names to list, due one at a time for sorting
        clear();
        for (PlayerInfo info : keep)
        {
            add(info, false);
        }

        // save if requested
        if (bSave) save();
    }

    /**
     * class to hold player info (name & key)
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class PlayerInfo implements Comparable<PlayerInfo>
    {
        private String sName, sKey;

        PlayerInfo(String sName, String sKey)
        {
            this.sName = sName;
            this.sKey = sKey;
        }

        public String getName()
        {
            return sName;
        }

        public String getKey()
        {
            return sKey;
        }

        public int compareTo(PlayerInfo p)
        {
            return String.CASE_INSENSITIVE_ORDER.compare(sName, p.sName);
        }
    }
}
