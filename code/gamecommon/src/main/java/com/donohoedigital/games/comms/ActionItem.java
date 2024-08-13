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
 * ActionItem.java
 *
 * Created on March 8, 2003, 6:26 PM
 */

package com.donohoedigital.games.comms;


import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.log4j.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
@DataCoder('A')
public class ActionItem extends DMTypedHashMap
{   
    static Logger logger = Logger.getLogger(ActionItem.class);
    
    // use _ so as not to conflict with Phase params we might use in action
    public static final String PARAM_ACTION_ID = "_id";
    public static final String PARAM_CREATE = "_create";
    public static final String PARAM_REMIND_EMAIL = "_remindemail";
    
    private static final Long INIT_LONG = (long) -1;
    
    /**
     * Need for demarshalling
     */
    public ActionItem()
    {
        setCreateTimeStamp();
    }
    
    /** 
     * Creates a new instance of ActionItem 
     */
    public ActionItem(int nActionID)
    {
        this();
        setActionID(nActionID);
    }
    
    /**
     * Store current time in millis* 1000 + a sequence number
     */
    public void setCreateTimeStamp()
    {
        setLong(PARAM_CREATE, Utils.getCurrentTimeStamp());
    }
        
    /**
     * Get create time (millis since 1/1/1970 * 1000 + arbitrary sequence number)
     */
    public long getCreateTimeStamp()
    {
        Long cat = getLong(PARAM_CREATE);
        if (cat == null) return 0;
        return cat;
    }
    
    /**
     * Get the date when this was created (new Date created each time)
     */
    public Date getCreateDate()
    {
        long l = getCreateTimeStamp();
        if (l==0) return null;
        return Utils.getDateFromTimeStamp(l);
    }
    
    /**
     * Set id of this action
     */
    public void setActionID(int id)
    {
        setInteger(PARAM_ACTION_ID, id);
    }
    
    /**
     * Get id of this action
     */
    public int getActionID()
    {
        return getInteger(PARAM_ACTION_ID);
    }
  
    /**
     * Set id of this action
     */
    public void setRemindEmailSent(boolean b)
    {
        setBoolean(PARAM_REMIND_EMAIL, b ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /**
     * Get id of this action
     */
    public boolean getRemindEmailSent()
    {
        Boolean b = getBoolean(PARAM_REMIND_EMAIL);
        if (b == null) return false;
        
        return b;
    }
    
    /**
     * Add this player to list that this action affects
     */
    public void addPlayer(int id)
    {
        // set id to false, meaning the player has not yet
        // acted.   The entry itself means the player
        // is involved in this action
        setLong(getString(id), INIT_LONG);
    }
    
    /**
     * Add all players to the list
     */
    public void addAllPlayers(GameInfo game)
    {
        int nNumPlayers = game.getNumPlayers();
        for (int i = 0; i < nNumPlayers; i++)
        {
            addPlayer(i);
        }
    }
    
    /**
     * Add all players to the list, unless
     * they are eliminated
     */
    public void addNonEliminatedPlayers(GameInfo game)
    {
        int nNumPlayers = game.getNumPlayers();
        for (int i = 0; i < nNumPlayers; i++)
        {
            if (!game.isEliminated(i)) addPlayer(i);
        }
    }
    
    /**
     * Remove player
     */
    public void removePlayer(int id)
    {
        removeLong(getString(id));
    }
    
    /**
     * Clear all player entries
     */
    public void removeAllPlayers(int nNumPlayers)
    {
        for (int i = 0; i < nNumPlayers; i++)
        {
            removePlayer(i);
        }
    }
    
    /**
     * Removes all players and sets the given id as the only
     * player in affected by this action
     */
    public void setPlayer(int id, int nNumPlayers)
    {
        removeAllPlayers(nNumPlayers);
        addPlayer(id);
    }
    
    /**
     * Return true if this player needs to
     * respond to this action.
     */
    public boolean isPlayerActionRequired(int id)
    {
        Long l = getLong(getString(id));
        if (l == null) return false;
        return true;
    }
    
    /**
     * Return true if the player has acted on this
     * item, false otherwise.  If player was not required
     * to do anything (i.e., isPlayerActionRequired()
     * returned false, then this returns true)
     */
    public boolean hasPlayerActed(int id)
    {
        Long l = getLong(getString(id));
        if (l == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Asking if player acted, but player not part of this action: " + 
                                    id + "(action " + getActionID() +")", null);
        }
        
        return !l.equals(INIT_LONG);
    }
    
    /**
     * Set this player as having acted. Return false if invalid to do so
     * (either because player wasn't part of the action, or if player already acted)
     */
    public boolean setPlayerActed(int id)
    {
        String sId = getString(id);
        Long l = getLong(sId);
        if (l == null)
        {
            // BUG 199 - don't throw exception.  Log error and return false.
            logger.warn("Setting player acted, but player not part of this action: " + 
                                    id + "(action " + getActionID() +")");
            return false;
        }
        
        if (!l.equals(INIT_LONG))
        {
            // BUG 199 - don't throw exception.  Log error and return false.
            logger.warn("Setting player acted, but player already acted " +
                                    id + "(action " + getActionID() +")");
            return false;
        }
        setLong(sId, Utils.getCurrentTimeStamp());
        return true;
    }
    
    /**
     * Get the date when the player acted (returns new date each time)
     */
    public Date getPlayerActedDate(int id)
    {
        Long l = getLong(getString(id));
        if (l == null) return null;
        
        return Utils.getDateFromTimeStamp(l);
    }
    
    /**
     * Is this action done?  Have all players acted?
     */
    public boolean isDone()
    {
        Iterator iter = keySet().iterator();
        Long l;
        String sName;
        while (iter.hasNext())
        {
            sName = (String)iter.next();
            if (sName.charAt(0) != '.') continue; // skip non player ids
            l = getLong(sName);
            if (l.equals(INIT_LONG))
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * For perf so we don't create lots of strings
     */
    private static String getString(int i)
    {
        switch (i)
        {
            case 0: return ".0";
            case 1: return ".1";
            case 2: return ".2";
            case 3: return ".3";
            case 4: return ".4";
            case 5: return ".5";
            case 6: return ".6";
            case 7: return ".7";
            case 8: return ".8";
            case 9: return ".9";

            default:
                return "." + i;
        }
    }
            
}
