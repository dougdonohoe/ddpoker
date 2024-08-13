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
 * Pot.java
 *
 * Created on January 6, 2004, 1:41 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.comms.*;

import java.util.*;

/**
 * @author donohoe
 */
@DataCoder('P')
public class Pot implements DataMarshal
{
    public static final int NO_SIDE = -1;

    private int nSeq_;
    private int nChips_ = 0;
    private int nBase_ = 0;
    private boolean bBaseAllIn_ = false;
    private int nSideBet_ = NO_SIDE; // used for side pots
    private int nRound_;
    private List<PokerPlayer> players_ = new ArrayList<PokerPlayer>();
    private List<PokerPlayer> winners_ = new ArrayList<PokerPlayer>();

    /**
     * empty constructor for loading
     */
    public Pot()
    {
    }

    /**
     * Creates a new instance of Pot
     */
    public Pot(int nRound, int nSeq)
    {
        nRound_ = nRound;
        nSeq_ = nSeq;
    }

    /**
     * Get betting round of this pot
     */
    public int getRound()
    {
        return nRound_;
    }

    /**
     * Set betting round of this pot
     */
    public void advanceRound()
    {
        nRound_ += 1;
        nSeq_ = 0;
        nBase_ = nChips_;
        bBaseAllIn_ = hasAllInPlayer();
    }

    /**
     * Get num chips in pot
     */
    public int getChipCount()
    {
        return nChips_;
    }

    /**
     * Add chips to the pot
     */
    public void addChips(PokerPlayer player, int n)
    {
        nChips_ += n;
        addPlayer(player);
    }

    /**
     * Get list of players
     */
    public List<PokerPlayer> getPlayers()
    {
        return players_;
    }

    /**
     * Get number of players in this pot
     */
    public int getNumPlayers()
    {
        return players_.size();
    }

    /**
     * is overbet pot?
     */
    public boolean isOverbet()
    {
        return getNumPlayers() == 1;
    }

    /**
     * Get player at
     */
    public PokerPlayer getPlayerAt(int i)
    {
        return players_.get(i);
    }

    /**
     * Add player to list if not already there
     */
    private void addPlayer(PokerPlayer player)
    {
        if (!players_.contains(player))
        {
            players_.add(player);
        }
    }

    /**
     * Return whether player is involved in pot
     */
    public boolean isInPot(PokerPlayer player)
    {
        return players_.contains(player);
    }

    /**
     * Return whether any players in this pot are all in
     */
    private boolean hasAllInPlayer()
    {
        for (PokerPlayer player : this.players_)
        {
            if (player.isAllIn()) return true;
        }
        return false;
    }

    /**
     * Return whether base pot had an all-in player
     */
    public boolean hasBaseAllIn()
    {
        return bBaseAllIn_;
    }

    /**
     * Set amount of bet for side pots
     */
    public void setSideBet(int nSide)
    {
        nSideBet_ = nSide;
    }

    /**
     * Get amount of bet for side pots
     */
    public int getSideBet()
    {
        return nSideBet_;
    }

    /**
     * Reset to base chips (players stay the same)
     */
    public void reset()
    {
        nChips_ = nBase_;
        // players in pot stay the same when resetting
    }

    /**
     * Get list of winners
     */
    public List<PokerPlayer> getWinners()
    {
        return winners_;
    }

    /**
     * Set winners
     */
    public void setWinners(List<PokerPlayer> winners)
    {
        winners_.clear();
        winners_.addAll(winners);
    }

    /**
     * debug
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players_.size(); i++)
        {
            if (i > 0) sb.append(", ");
            sb.append(players_.get(i).getName());
        }
        return "\n                                   Round " +
               nRound_ + "." + nSeq_ + "  (base: " + nBase_ + ")  (side: " + nSideBet_ + ")  (CHIPS: " + nChips_ + ")   Players: " + sb;
    }

    ////
    //// Save/Load
    ////

    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        nChips_ = list.removeIntToken();
        nRound_ = list.removeIntToken();
        nBase_ = list.removeIntToken();
        nSideBet_ = list.removeIntToken();
        bBaseAllIn_ = list.removeBooleanToken();
        nSeq_ = list.removeIntToken();
        int nNum = list.removeIntToken();
        PokerPlayer player;
        for (int i = 0; i < nNum; i++)
        {
            player = (PokerPlayer) state.getObject(list.removeIntegerToken());
            players_.add(player);
        }
        nNum = list.removeIntToken();
        for (int i = 0; i < nNum; i++)
        {
            player = (PokerPlayer) state.getObject(list.removeIntegerToken());
            winners_.add(player);
        }
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(nChips_);
        list.addToken(nRound_);
        list.addToken(nBase_);
        list.addToken(nSideBet_);
        list.addToken(bBaseAllIn_);
        list.addToken(nSeq_);
        list.addToken(players_.size());
        for (PokerPlayer player : players_)
        {
            list.addToken(state.getId(player));
        }
        list.addToken(winners_.size());
        for (PokerPlayer winner : winners_)
        {
            list.addToken(state.getId(winner));
        }
        return list.marshal(state);
    }

}
