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
package com.donohoedigital.games.poker.engine;

import com.donohoedigital.config.*;

/**
 * Global representation of card suits.
 */
public class CardSuit implements Comparable<CardSuit>
{
    public static final int NUM_SUITS = 4;

    // numbered based on bridge rank / reverse alphabetical
    public static final int UNKNOWN_RANK = -1;
    public static final int CLUBS_RANK = com.ddpoker.Card.CLUBS;
    public static final int DIAMONDS_RANK = com.ddpoker.Card.DIAMONDS;
    public static final int HEARTS_RANK = com.ddpoker.Card.HEARTS;
    public static final int SPADES_RANK = com.ddpoker.Card.SPADES;

    public static final CardSuit UNKNOWN = new CardSuit(UNKNOWN_RANK, "unknown");
    public static final CardSuit CLUBS = new CardSuit(CLUBS_RANK, "club");
    public static final CardSuit DIAMONDS = new CardSuit(DIAMONDS_RANK, "diamond");
    public static final CardSuit HEARTS = new CardSuit(HEARTS_RANK, "heart");
    public static final CardSuit SPADES = new CardSuit(SPADES_RANK, "spade");

    private int rank_;
    private String name_;
    private String abbr_;

    /**
     * Constructor is private; only four instances exist.
     */
    private CardSuit(int rank, String name)
    {
        rank_ = rank;
        name_ = name;
        abbr_ = null;
    }

    public static CardSuit forRank(int rank)
    {
        switch (rank)
        {
            case CLUBS_RANK:
                return CLUBS;
            case DIAMONDS_RANK:
                return DIAMONDS;
            case HEARTS_RANK:
                return HEARTS;
            case SPADES_RANK:
                return SPADES;
            case UNKNOWN_RANK:
                return UNKNOWN;
            default:
                return null;
        }
    }

    public int getRank()
    {
        return rank_;
    }

    public String getName()
    {
        return name_;
    }

    public String getAbbr()
    {
        if (abbr_ == null) abbr_ = PropertyConfig.getMessage("msg.card." + name_);
        return abbr_;
    }

    public int compareTo(CardSuit cs)
    {
        return rank_ - cs.rank_;
    }
}
