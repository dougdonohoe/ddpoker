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

/**
 * Stored procedures for calling from SQL.
 */
public class PokerDatabaseProcs
{
    public static int getSuitRank(String card)
    {
        if ((card == null) || (card.length() < 2))
        {
            return -1;
        }

        char suit = card.charAt(1);

        switch (suit)
        {
            case 'C':
            case 'c':
                return 0;
            case 'D':
            case 'd':
                return 1;
            case 'H':
            case 'h':
                return 2;
            case 'S':
            case 's':
                return 3;
            default:
                return -1;
        }
    }

    public static int getCardRank(String card)
    {
        if ((card == null) || (card.length() < 1))
        {
            return -1;
        }

        char rank = card.charAt(0);

        switch (rank)
        {
            case 'A':
            case 'a':
                return 14;
            case 'K':
            case 'k':
                return 13;
            case 'Q':
            case 'q':
                return 12;
            case 'J':
            case 'j':
                return 11;
            case 'T':
            case 't':
                return 10;
            case '9':
                return 9;
            case '8':
                return 8;
            case '7':
                return 7;
            case '6':
                return 6;
            case '5':
                return 5;
            case '4':
                return 4;
            case '3':
                return 3;
            case '2':
                return 2;
            default:
                return -1;
        }
    }

    public static int getCardIndex(String card)
    {
        return (getCardRank(card) - 2) * 4 + getSuitRank(card);
    }

    public static long getCardBit(String card)
    {
        int index =- getCardIndex(card);

        if (index < 0)
        {
            return 0;
        }
        else
        {
            return 1L << index;
        }
    }

    public static String getCardRankDisplay(String card)
    {
        int rank = getCardRank(card);

        return getRankDisplay(rank);
    }

    public static String getRankDisplay(int rank)
    {
        switch (rank)
        {
            case 14:
                return "A";
            case 13:
                return "K";
            case 12:
                return "Q";
            case 11:
                return "J";
            case 10:
                return "T";
            case 9:
                return "9";
            case 8:
                return "8";
            case 7:
                return "7";
            case 6:
                return "6";
            case 5:
                return "5";
            case 4:
                return "4";
            case 3:
                return "3";
            case 2:
                return "2";
            default:
                return "x";
        }
    }

    public static String getHandClass(String card1, String card2)
    {
        int rank1 = getCardRank(card1);
        int suit1 = getSuitRank(card1);

        int rank2 = getCardRank(card2);
        int suit2 = getSuitRank(card2);

        if (rank1 == rank2)
        {
            return getRankDisplay(rank1) + getRankDisplay(rank2);
        }
        else if (rank1 > rank2)
        {
            return getRankDisplay(rank1) + getRankDisplay(rank2) + (suit1 == suit2 ? "s" : "o");
        }
        else
        {
            return getRankDisplay(rank2) + getRankDisplay(rank1) + (suit1 == suit2 ? "s" : "o");
        }
    }

    public static int getHandClassRank(String card1, String card2)
    {
        int rank1 = getCardRank(card1);
        int suit1 = getSuitRank(card1);

        int rank2 = getCardRank(card2);
        int suit2 = getSuitRank(card2);

        if (rank1 > rank2)
        {
            return rank1 * 32 + rank2 * 32 + (suit1 == suit2 ? 1 : 0);
        }
        else
        {
            return rank2 * 32 + rank1 * 32 + (suit1 == suit2 ? 1 : 0);
        }
    }
}
