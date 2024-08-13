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
 * HandAction.java
 *
 * Created on January 6, 2004, 2:19 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.comms.*;
import org.apache.log4j.*;
import com.donohoedigital.config.*;
import com.donohoedigital.base.*;

/**
 *
 * @author  Doug Donohoe
 */
@DataCoder('?')
public class HandAction implements DataMarshal
{    
    static Logger logger = Logger.getLogger(HandAction.class);
    
    public static final int ACTION_NONE = -1;
    public static final int ACTION_FOLD = 0;
    public static final int ACTION_CHECK = 1;
    public static final int ACTION_CHECK_RAISE = 2;
    public static final int ACTION_CALL = 3;
    public static final int ACTION_BET = 4;
    public static final int ACTION_RAISE = 5;
    public static final int ACTION_BLIND_BIG = 6;
    public static final int ACTION_BLIND_SM = 7;
    public static final int ACTION_ANTE = 8;
    public static final int ACTION_WIN = 9;
    public static final int ACTION_OVERBET = 10;
    public static final int ACTION_LOSE = 11;

    // fold type
    public static final int FOLD_NORMAL = 0; // cooresponds to nSubAmount_ default initialization to 0
    public static final int FOLD_FORCED = -1;
    public static final int FOLD_SITTING_OUT = -2;

    private int nRound_;
    private int nAction_;
    private PokerPlayer player_;
    private int nAmount_;
    private int nSubAmount_; // stores call portion of a raise, pot number for win/overbet, and fold type
    private boolean bAllIn_;

    // debug
    private String sDebug_ = null;
    
    /**
     * Empty constructor for load
     */
    public HandAction()
    {
    }
    
    /** 
     * Creates a new instance of HandAction where amount defaults to 0
     */
    public HandAction(PokerPlayer player, int nRound, int nAction) 
    {
        this(player, nRound, nAction, 0);
    }
    
    /** 
     * Creates a new instance of HandAction where amount defaults to 0
     */
    public HandAction(PokerPlayer player, int nRound, int nAction, String sDebug) 
    {
        this(player, nRound, nAction, 0, sDebug);
    }
    
    /** 
     * Creates a new instance of HandAction 
     */
    public HandAction(PokerPlayer player, int nRound, int nAction, int nAmount) 
    {
        this(player, nRound, nAction, nAmount, null);
    }
    
    /** 
     * Creates a new instance of HandAction 
     */
    public HandAction(PokerPlayer player, int nRound, int nAction, int nAmount, String sDebug) 
    {
        this(player, nRound, nAction, nAmount, 0, sDebug);
    }
    
    /** 
     * Creates a new instance of HandAction 
     */
    public HandAction(PokerPlayer player, int nRound, int nAction, int nAmount, int nSubAmount, String sDebug) 
    {
        player_ = player;
        nRound_ = nRound;
        nAction_ = nAction;
        nAmount_ = nAmount;
        nSubAmount_ = nSubAmount;
        sDebug_ = sDebug;
        switch (nAction)
        {
            case ACTION_ANTE:
            case ACTION_BLIND_SM:
            case ACTION_BLIND_BIG:
            case ACTION_CALL:
            case ACTION_BET:
            case ACTION_RAISE:
                // all in if player has no chips when the hand action is created
                // we can check == 0 because in PokerPlayer, the chips are adjusted
                // before the appropriate method is called no HoldemHand, which
                // in turn creates the HandAction
                bAllIn_ = player.getChipCount() == 0;
                break;
            default:
                bAllIn_ = false;
                break;
        }
    }
    
    /**
     * Get player
     */
    public PokerPlayer getPlayer()
    {
        return player_;
    }
    
    /**
     * Get action
     */
    public int getAction()
    {
        return nAction_;
    }
    
    /**
     * Get round
     */
    public int getRound()
    {
        return nRound_;
    }
    
    /**
     * Get amount
     */
    public int getAmount()
    {
        return nAmount_;
    }
    
    /**
     * Get sub amount (used for call portion of a raise, pot number in win/overbet, and fold type )
     */
    public int getSubAmount()
    {
        return nSubAmount_;
    }
    
    /**
     * Get amount less subamount
     */
    public int getAdjustedAmount()
    {
        int nSub = nSubAmount_;

        // this check is just in case getAdjustedAmount is called
        // on a non-raise action (some use subamount for other purposes)
        if (nAction_ != ACTION_RAISE)
        {
            nSub = 0;
        }
        return nAmount_ - nSub;
    }
    
    /**
     * Get debug
     */
    public String getDebug()
    {
        return sDebug_;
    }

    /**
     * Used by PokerDatabase when loading history.
     */
    public void setAllIn(boolean bAllIn)
    {
        bAllIn_ = bAllIn;
    }

    public boolean isAllIn()
    {
        return bAllIn_;
    }

    /**
     * debug
     */
    public String getName()
    {
        return getActionName(nAction_);
    }

    public String getActionCode()
    {
        switch (nAction_) {
            case ACTION_FOLD: return "FOLD";
            case ACTION_BET: return "BET";
            case ACTION_CHECK: return "CHECK";
            case ACTION_CHECK_RAISE: return "CHECK";
            case ACTION_CALL: return "CALL";
            case ACTION_RAISE: return "RAISE";
            case ACTION_BLIND_SM: return "SMALL";
            case ACTION_BLIND_BIG: return "BIG";
            case ACTION_ANTE: return "ANTE";
            case ACTION_WIN: return "WIN";
            case ACTION_OVERBET: return "OVER";
            case ACTION_LOSE: return "LOSE";
            default: return null;
        }
    }

    public static int decodeActionType(String code)
    {
        if (code == null) return ACTION_NONE;

        if (code.equals("FOLD")) return ACTION_FOLD;
        if (code.equals("BET")) return ACTION_BET;
        if (code.equals("CHECK")) return ACTION_CHECK;
        if (code.equals("CALL")) return ACTION_CALL;
        if (code.equals("RAISE")) return ACTION_RAISE;
        if (code.equals("SMALL")) return ACTION_BLIND_SM;
        if (code.equals("BIG")) return ACTION_BLIND_BIG;
        if (code.equals("ANTE")) return ACTION_ANTE;
        if (code.equals("WIN")) return ACTION_WIN;
        if (code.equals("OVER")) return ACTION_OVERBET;
        if (code.equals("LOSE")) return ACTION_LOSE;

        return ACTION_NONE;
    }

    /**
     * debug
     */
    public static String getActionName(int nAction)
    {
        String sAction = "<undefined>";
        switch (nAction) {
            case ACTION_BET: sAction =         "bet"; break;
            case ACTION_CHECK: sAction =       "check";break;
            case ACTION_CHECK_RAISE: sAction = "checkraise";break;
            case ACTION_CALL: sAction =        "call";break;
            case ACTION_FOLD: sAction =        "fold";break;
            case ACTION_RAISE: sAction =       "raise";break;
            case ACTION_BLIND_SM: sAction =    "smallblind";break;
            case ACTION_BLIND_BIG: sAction =   "bigblind";break;
            case ACTION_ANTE: sAction =        "ante";break;
            case ACTION_WIN: sAction =         "win";break;
            case ACTION_OVERBET: sAction =     "overbet";break;
            case ACTION_LOSE: sAction=         "lose";break;
        }
        return sAction;
    }

    /**
     * debug
     */
    public String toString()
    {
        return toString(true);
    }

    /**
     * debug
     */
    public String toString(boolean bShort)
    {
        String sDebug = sDebug_;
        String sAction = "<undefined>";
        switch (nAction_) {
            case ACTION_BET: sAction =         "bet        $"+ PokerPlayer.fChip.form(nAmount_); break;
            case ACTION_CHECK: sAction =       "check              "; break; // fchip is 7 spaces, so we add that here
            case ACTION_CHECK_RAISE: sAction = "checkraise         "; break; // fchip is 7 spaces, so we add that here
            case ACTION_CALL: sAction =        "call       $"+ PokerPlayer.fChip.form(nAmount_); break;
            case ACTION_FOLD: sAction =        "fold               "; break;
            case ACTION_RAISE: sAction =       "raise      $"+ PokerPlayer.fChip.form(nAmount_)+
                                                        " ("+nSubAmount_+" call)"; break;
            case ACTION_BLIND_SM: sAction =    "smallblind $"+ PokerPlayer.fChip.form(nAmount_); break;
            case ACTION_BLIND_BIG: sAction =   "bigblind   $"+ PokerPlayer.fChip.form(nAmount_); break;
            case ACTION_ANTE: sAction =        "ante       $"+ PokerPlayer.fChip.form(nAmount_); break;
            case ACTION_WIN: sAction =         "win        $"+ PokerPlayer.fChip.form(nAmount_); sDebug = "Pot " + nSubAmount_;break;
            case ACTION_OVERBET: sAction =     "overbet    $"+ PokerPlayer.fChip.form(nAmount_); sDebug = "Pot " + nSubAmount_;break;
            case ACTION_LOSE: sAction =        "lose               "; sDebug = "Pot " + nSubAmount_;break;
        }
        
        String sRound = "<undefined>";
        switch (nRound_) {
            case HoldemHand.ROUND_PRE_FLOP: sRound = " deal"; break;
            case HoldemHand.ROUND_FLOP:     sRound = " flop"; break;
            case HoldemHand.ROUND_TURN: sRound =     " turn"; break;
            case HoldemHand.ROUND_RIVER: sRound =    "river"; break;
            case HoldemHand.ROUND_SHOWDOWN: sRound=  " show"; break;
        }

        if (bShort)
        {
            return player_.getName() + ": "+ sAction;
        }
        else
        {
            return player_.toStringLong() + ", " + sRound + ": " + sAction + " (" + PokerPlayer.fStringLong.form(sDebug == null ? "" : sDebug)+")";
        }

    }

    /**
     * get Chat display
     */
    public String getChat(int nPrior, Object[] extraParams, String sKeySuffix)
    {
        if (sKeySuffix == null) sKeySuffix = ""; else sKeySuffix = "." + sKeySuffix;
        StringBuilder sb = new StringBuilder();
        sb.append("<TABLE CELLPADDING=0 CELLSPACING=0><TR><TD>");
        sb.append(getHTMLSnippet("msg.handchat"+sKeySuffix, nPrior, extraParams));
        sb.append("</TD></TR></TABLE>");

        return sb.toString();
    }

    /**
     * Get HTML snippet of action.  This returns something that needs to go inside a <TD> </TD> block.
     */
    public String getHTMLSnippet(String sPropertyName, int nPrior, Object[] extraParams)
    {
        String sIcon = null;

        // set amount of action
        int nAmount = getAmount();

        // if raise, determine call
        int nCall = 0;
        String sExtraKey = "";
        int nAction = getAction();
        if (nAction == HandAction.ACTION_RAISE)
        {
            // figure call and actual raise
            nCall = getSubAmount();
            if (nCall == 0) sExtraKey = ".bb";
            nAmount -= nCall;

            // get right raise icon
            if (nPrior >= 2)
            {
                sIcon = "icon-rereraise";
            } else if (nPrior == 1)
            {
                sIcon = "icon-reraise";
            } else
            {
                sIcon = "icon-raise";
            }
        }
        else if (nAction == HandAction.ACTION_FOLD)
        {
            if (getSubAmount() == FOLD_FORCED) sExtraKey = ".forced";
            else if (getSubAmount() == FOLD_SITTING_OUT) sExtraKey = ".sitout";
        }

        // if all in, display full amount bet
        if (isAllIn()) nAmount += nCall;

        Object[] params = new Object[4 + (extraParams != null ? extraParams.length : 0)];
        params[0] = new Integer(nAmount);
        params[1] = new Integer(nCall);
        params[2] = sIcon;
        params[3] = Utils.encodeHTML(getPlayer().getName());
        if (extraParams != null && extraParams.length > 0)
        {
            System.arraycopy(extraParams, 0, params, 4, extraParams.length);
        }

        return PropertyConfig.getMessage(
                            isAllIn() ? sPropertyName+".allin" : sPropertyName+"." + nAction + sExtraKey,
                            params);
    }
    
    public void demarshal(MsgState state, String sData) 
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        nRound_ = list.removeIntToken();
        nAction_ = list.removeIntToken();
        player_ = (PokerPlayer) state.getObject(list.removeIntegerToken());
        nAmount_ = list.removeIntToken();
        nSubAmount_ = list.removeIntToken();
        bAllIn_ = list.removeBooleanToken();
        if (list.hasMoreTokens())
        {
            sDebug_ = list.removeStringToken();
        }
    }
    
    public String marshal(MsgState state) 
    {
        TokenizedList list = new TokenizedList();
        list.addToken(nRound_);
        list.addToken(nAction_);
        list.addToken(state.getId(player_));
        list.addToken(nAmount_);
        list.addToken(nSubAmount_);
        list.addToken(bAllIn_);
        list.addToken(sDebug_);
        return list.marshal(state);
    }

    
}
