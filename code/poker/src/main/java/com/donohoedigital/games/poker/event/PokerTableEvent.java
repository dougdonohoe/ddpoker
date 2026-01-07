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
 * PokerTableEvent.java
 *
 * Created on December 14, 2004, 12:58 PM
 */

package com.donohoedigital.games.poker.event;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.poker.*;

/**
 *
 * @author  donohoe
 */
@DataCoder('v')
public class PokerTableEvent implements DataMarshal
{
    public static final int TYPE_NEW_HAND = 1;
    public static final int TYPE_PLAYER_ADDED = 1 << 1;
    public static final int TYPE_PLAYER_REMOVED = 1 << 2;
    public static final int TYPE_CURRENT_PLAYER_CHANGED = 1 << 3;
    public static final int TYPE_PLAYER_ACTION = 1 << 4; // check, call, bet, raise, fold
    public static final int TYPE_DEALER_ACTION = 1 << 5; // deal, flop, turn, river, showdown
    public static final int TYPE_LEVEL_CHANGED = 1 << 6;
    public static final int TYPE_TABLE_REMOVED = 1 << 7;
    public static final int TYPE_BUTTON_MOVED = 1 << 8;
    public static final int TYPE_PREFS_CHANGED = 1 << 9;
    public static final int TYPE_STATE_CHANGED = 1 << 10;
    public static final int TYPE_PLAYER_REBUY = 1 << 11;
    public static final int TYPE_PLAYER_ADDON = 1 << 12;
    public static final int TYPE_END_HAND = 1 << 13;
    public static final int TYPE_OBSERVER_ADDED = 1 << 14;
    public static final int TYPE_OBSERVER_REMOVED = 1 << 15;
    public static final int TYPE_CLEANING_DONE = 1 << 16;
    public static final int TYPE_NEW_PLAYERS_LOADED = 1 << 17;
    public static final int TYPE_PLAYER_AI_CHANGED = 1 << 18;
    public static final int TYPE_CARD_CHANGED = 1 << 19;
    public static final int TYPE_PLAYER_CHIPS_CHANGED = 1 << 20;
    public static final int TYPE_PLAYER_SETTINGS_CHANGED = 1 << 21;

    // composite defines
    public static final int TYPES_ALL = -1; // that compsci degree paying off
    public static final int TYPES_PLAYERS_CHANGED = TYPE_PLAYER_ADDED | TYPE_PLAYER_REMOVED;
    public static final int TYPES_OBSERVERS_CHANGED = TYPE_OBSERVER_ADDED | TYPE_OBSERVER_REMOVED;

    // no value defined
    public static final int NOT_DEFINED = -999;
    
    // members
    private int nType_;
    private PokerTable table_;
    private PokerPlayer player_;
    private HandAction action_;
    private int nOne_ = NOT_DEFINED;
    private int nTwo_ = NOT_DEFINED;
    private boolean bFlag_ = false;

    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        
        nType_ = list.removeIntToken();
        table_ = (PokerTable) state.getObject(list.removeIntegerToken());
        player_ = (PokerPlayer) state.getObject(list.removeIntegerToken());
        action_ = (HandAction) list.removeToken();
        nOne_ = list.removeIntToken();
        nTwo_ = list.removeIntToken();
        bFlag_ = list.removeBooleanToken();
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        
        list.addToken(nType_);
        list.addToken(state.getId(table_));
        list.addToken(state.getId(player_));
        list.addToken(action_);
        list.addToken(nOne_);
        list.addToken(nTwo_);
        list.addToken(bFlag_);

        return list.marshal(state);
    }
    
    /**
     * type as string for debugging
     */
    public String getTypeAsString()
    {
        switch (nType_)
        {
            case TYPE_NEW_HAND: return "new-hand";
            case TYPE_PLAYER_ADDED: return "player-added";
            case TYPE_PLAYER_REMOVED: return "player-removed";
            case TYPE_CURRENT_PLAYER_CHANGED: return "current-player-changed";
            case TYPE_PLAYER_ACTION: return "player-action";
            case TYPE_DEALER_ACTION: return "dealer-action";
            case TYPE_LEVEL_CHANGED: return "level-changed";
            case TYPE_TABLE_REMOVED: return "table-removed";
            case TYPE_BUTTON_MOVED: return "button-moved";
            case TYPE_PREFS_CHANGED: return "prefs-changed";
            case TYPE_STATE_CHANGED: return "state-changed";
            case TYPE_PLAYER_ADDON: return "addon";
            case TYPE_PLAYER_REBUY: return "rebuy";
            case TYPE_END_HAND: return "end-hand";
            case TYPE_OBSERVER_ADDED: return "observer-added";
            case TYPE_OBSERVER_REMOVED: return "observer-removed";
            case TYPE_CLEANING_DONE: return "cleaning-done";
            case TYPE_PLAYER_AI_CHANGED: return "player-ai-changed";
            case TYPE_PLAYER_SETTINGS_CHANGED: return "player-settings-changed";
        }

        return "unknown ("+nType_+")";
    }

    /**
     * debug
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TABLE=").append(table_.getName());
        sb.append("; TYPE=");
        sb.append(getTypeAsString());       
        if (action_ == null && player_ != null)
        {
            sb.append(";  PLAYER=").append(player_.getName());
            if (player_.isWaiting()) sb.append(" (w)"); 
        }
        if (action_ != null) sb.append(";  ACTION=").append(action_);
        if (nType_ == TYPE_STATE_CHANGED)
        {
            sb.append("; old=").append(PokerTable.getStringForState(nOne_));
            sb.append("; new=").append(PokerTable.getStringForState(nTwo_));
        }
        else if (nType_ == TYPE_DEALER_ACTION)
        {
            sb.append("; round=").append(HoldemHand.getRoundName(nOne_));
        }
        else if (nType_ == TYPE_PLAYER_REBUY || nType_ == TYPE_PLAYER_ADDON)
        {
            sb.append("; cash=").append(nOne_);
            sb.append("; chips=").append(nTwo_);
            if (bFlag_) sb.append(" (pending)");
        }
        else
        {
            if (nOne_ != NOT_DEFINED) sb.append("; 1st=").append(nOne_);
            if (nTwo_ != NOT_DEFINED) sb.append("; 2nd=").append(nTwo_);
        }        
        return sb.toString();
    }

    /**
     * Empty constructor for demarshaling
     */
    public PokerTableEvent()
    {
    }
    
    /**
     * Various poker table events
     */
    public PokerTableEvent(int nType, PokerTable table)
    {
        nType_ = nType;
        table_ = table;
        ApplicationError.assertNotNull(table_, "Poker table must be non-null", null);
    }

    /**
     * Player added/removed events
     */
    public PokerTableEvent(int nType, PokerTable table, PokerPlayer player, int nSeat)
    {
        this(nType, table);
        player_ = player;
        nOne_ = nSeat;
    }

    /**
     * Button moved events and other old value/new value events
     */
    public PokerTableEvent(int nType, PokerTable table, int nOld, int nNew)
    {
        this(nType, table);
        nOne_ = nOld;
        nTwo_ = nNew;
    }

    /**
     * Player rebuy/addon events
     */
    public PokerTableEvent(int nType, PokerTable table, PokerPlayer player, int nCash, int nChips, boolean bPending)
    {
        this(nType, table);
        nOne_ = nCash;
        nTwo_ = nChips;
        player_ = player;
        bFlag_ = bPending;
    }

    /**
     * Dealer action events
     */
    public PokerTableEvent(int nType, PokerTable table, int nValue)
    {
        this(nType, table);
        nOne_ = nValue;
    }

    /**
     * Hand action events
     */
    public PokerTableEvent(int nType, PokerTable table, HandAction action)
    {
        this(nType, table);
        action_ = action;
        player_ = action_.getPlayer();
    }

    /**
     * Get event type
     */
    public int getType()
    {
        return nType_;
    }

    /**
     * Get poker table this event was generated from.  Always non-null
     */
    public PokerTable getTable()
    {
        return table_;
    }

    /**
     * Get poker player this event refers to.  Only availble for PLAYER event types
     */
    public PokerPlayer getPlayer()
    {
        return player_;
    }

    /**
     * Get seat added to or removed from for TYPE_PLAYER_ADDED and TYPE_PLAYER_REMOVED events
     */
    public int getSeat()
    {
        return nOne_;
    }

    /**
     * Get round in TYPE_PLAYER_ACTION events
     */
    public int getRound()
    {
        return nOne_;
    }

    /**
     * Get hand action taken by player for TYPE_PLAYER_ACTION events.
     */
    public HandAction getAction()
    {
        return action_;
    }

    /**
     * Get old int value.  For TYPE_BUTTON_MOVED, returns old button seat.
     * For TYPE_CURRENT_PLAYER_CHANGED returns index of old current player.
     * For TYPE_LEVEL_CHANGED returns old level.
     * For TYPE_STATE_CHANGED returns old state.
     */
    public int getOld()
    {
        return nOne_;
    }

    /**
     * Get new int value.  For TYPE_BUTTON_MOVED, returns new button seat.
     * For TYPE_CURRENT_PLAYER_CHANGED returns index of new current player.
     * For TYPE_LEVEL_CHANGED returns new level.
     * For TYPE_STATE_CHANGED returns new state.
     */
    public int getNew()
    {
        return nTwo_;
    }

    /**
     * Get amount spent for TYPE_PLAYER_REBUY and TYPE_PLAYER_ADDON events
     */
    public int getAmount()
    {
        return nOne_;
    }

    /**
     * Get chips received for TYPE_PLAYER_REBUY and TYPE_PLAYER_ADDON events
     */
    public int getChips()
    {
        return nTwo_;
    }
    
    /**
     * Is pending rebuy event?
     */
    public boolean isPending()
    {
        return bFlag_;
    }
}
