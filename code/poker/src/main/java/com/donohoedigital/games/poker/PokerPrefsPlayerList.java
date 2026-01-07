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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.config.*;

import java.util.*;
import java.util.prefs.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Nov 1, 2005
 * Time: 10:34:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class PokerPrefsPlayerList extends AbstractPlayerList
{
    // these have to match gamedef.xml entry
    
    public static final String LIST_MUTE = "muted";
    public static final String LIST_BANNED = "banned";

    private String sListName_;
    private String sListNameKey_;
    protected boolean bUseKey_ = false;

    private static Map<String, PokerPrefsPlayerList> share_ = null;

    /**
     * Get shared PlayerList
     */
    public synchronized static PokerPrefsPlayerList getSharedList(String sListName)
    {
        if (share_ == null)
        {
            share_ = new HashMap<String, PokerPrefsPlayerList>();
        }

        PokerPrefsPlayerList list = share_.get(sListName);
        if (list == null)
        {
            list = new PokerPrefsPlayerList(sListName);
            share_.put(sListName, list);
        }
        return list;
    }

    /**
     * get unique PlayerList
     */
    public static PokerPrefsPlayerList getUniqueList(String sListName)
    {
        return new PokerPrefsPlayerList(sListName);
    }

    /**
     * Get the preferences.
     *
     * @return the preferences
     */
    private static Preferences getPreferences()
    {
        GameEngine engine = GameEngine.getGameEngine();
        String NODE = engine.getPrefsNodeName() + "/playerlist";
        return Prefs.getUserPrefs(Prefs.NODE_OPTIONS + NODE);
    }

    /**
     * construct list
     */
    private PokerPrefsPlayerList(String sListName)
    {
        sListName_ = sListName;
        sListNameKey_ = sListName_+".key";
        bUseKey_ = sListName.equals(LIST_BANNED); // use key lookup for banned keys
        fetch();
    }

    /**
     * Is this key in the list?
     */
    public boolean containsKey(String sKey)
    {
        if (!bUseKey_) return false;
        return super.containsKey(sKey);
    }

    /**
     * list name
     */
    public String getName()
    {
        return sListName_;
    }

    /**
     * fetch string list of names
     */
    protected String fetchNames()
    {
        return getPreferences().get(sListName_, null);
    }

    /**
     * fetch string list of keys
     */
    protected String fetchKeys()
    {
        return getPreferences().get(sListNameKey_, null);
    }

    /**
     * Save names
     */
    protected void saveNames(String sNames)
    {
        getPreferences().put(sListName_, sNames);
    }

    /**
     * Save keys
     */
    protected void saveKeys(String sKeys)
    {
        getPreferences().put(sListNameKey_, sKeys);
    }

}
