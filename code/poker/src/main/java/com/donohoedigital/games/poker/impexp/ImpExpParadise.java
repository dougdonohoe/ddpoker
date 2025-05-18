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
package com.donohoedigital.games.poker.impexp;


import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;

import java.text.*;
import java.util.*;

public class ImpExpParadise implements ImpExp
{
    private String paradisePlayerName_ = "DD Player";

    private HandInfoFast info = new HandInfoFast();

    private NumberFormat chipAmountFormat = NumberFormat.getInstance(Locale.US);

    public void setPlayerName(String name)
    {
        paradisePlayerName_ = name;
    }

    public String exportTournament(ImpExpHand ieHand)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd @ HH:mm:ss.S", Locale.US);

        String newline = "\r\n";

        StringBuilder buf = new StringBuilder();

        if ("NOLIMIT".equals(ieHand.gameType))
        {
            buf.append("No Limit");
        }
        else if ("POTLIMIT".equals(ieHand.gameType))
        {
            buf.append("Pot Limit");
        }
        else if ("LIMIT".equals(ieHand.gameType))
        {
            buf.append("Fixed Limit");
        }

        buf.append(" Texas Hold'em Tournament Summary for table \"");
        buf.append(ieHand.tournamentName);
        buf.append(" ");
        buf.append(ieHand.hndTable);
        buf.append("\"");
        buf.append(newline);

        buf.append("$");
        buf.append(100);
        buf.append(" Prize Pool");

        buf.append(", $");
        buf.append(20);
        buf.append(" Buy-In");

        buf.append(", $");
        buf.append(0);
        buf.append(" Fee");

        buf.append(", ");
        buf.append(10);
        buf.append(" players");
        buf.append(newline);

        buf.append(newline);

        buf.append("Tournament Buy-In: $");
        buf.append(20);
        buf.append(" + $0 Fee");
        buf.append(newline);

        buf.append(newline);

        buf.append("Tournament started ");
        Calendar startDate = Calendar.getInstance(TimeZone.getTimeZone("CST"));
        startDate.setTimeInMillis(ieHand.tournamentStartDate.getTime().getTime());
        buf.append(dateFormat.format(startDate.getTime()).substring(0, 21));
        buf.append(" (CST) -- Game #");
        buf.append(1);
        buf.append(newline);

        buf.append(newline);

        buf.append("  1st: Cards2Envy, $100 prize awarded (Level IX, Game #6)");
        buf.append(newline);
        /*
  2nd: panda bear, $60 prize awarded (Level IX, Game #6)
  3rd: leoben, $40 prize awarded (Level VI, Game #10)

  4th: Sargent1 (Level V, Game #7)
  5th: acedad (Level V, Game #4)
  6th: deznuttzz (Level IV, Game #6)
  7th: jedi master (Level IV, Game #3) ***
  8th: GMATTSON9900 (Level IV, Game #2)
  9th: BASSMASSTER (Level III, Game #4)
 10th: mpshanah (Level I, Game #4)
        */

        buf.append(newline);

        buf.append("Tournament finished ");
        Calendar endDate = Calendar.getInstance(TimeZone.getTimeZone("CST"));
        endDate.setTimeInMillis(ieHand.tournamentEndDate.getTime().getTime());
        buf.append(dateFormat.format(endDate.getTime()).substring(0, 21));
        buf.append(" (CST) -- Game #");
        buf.append(1);
        buf.append(newline);

        buf.append(newline);

        buf.append("----");

        return buf.toString();
    }

    public String exportHand(ImpExpHand ieHand)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.S", Locale.US);

        String newline = "\r\n";

        StringBuilder buf = new StringBuilder();

        buf.append("Game #");
        buf.append(ieHand.profileNumber * 1000000L + ieHand.handID);
        buf.append(" - Tournament DD Poker ");
        buf.append(ieHand.tournamentName);
        buf.append(" - ");
        buf.append(ieHand.smallBlind);
        buf.append("/");
        buf.append(ieHand.bigBlind);
        if ("NOLIMIT".equals(ieHand.gameType))
        {
            buf.append(" No Limit");
        }
        else if ("POTLIMIT".equals(ieHand.gameType))
        {
            buf.append(" Pot Limit");
        }
        else if ("LIMIT".equals(ieHand.gameType))
        {
            buf.append(" Fixed Limit");
        }
        Calendar startDate = Calendar.getInstance(TimeZone.getTimeZone("CST"));
        startDate.setTimeInMillis(ieHand.startDate.getTime().getTime());
        buf.append(" Texas Hold'em - ");
        buf.append(dateFormat.format(startDate.getTime()).substring(0, 21));
        buf.append(" (CST)");
        buf.append(newline);

        buf.append("Table \"DD Poker ");
        buf.append(ieHand.tournamentName);
        buf.append(" ");
        buf.append(ieHand.hndTable);
        buf.append("\"");
        buf.append(" Hand #");
        buf.append(ieHand.hndNumber);
        buf.append(" -- Seat ");
        buf.append(ieHand.buttonSeat%9 + 1);
        buf.append(" is the button");
        buf.append(newline);

        PokerPlayer lastPlayerLeft = null;

        int playersLeft = 0;

        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            PokerPlayer player = ieHand.players[seat];

            if (player == null) continue;

            buf.append("Seat ");
            if (seat < 9) buf.append(" ");
            buf.append(seat + 1);
            buf.append(": ");
            appendPlayerName(ieHand, buf, player);
            buf.append("  (");
            buf.append(chipAmountFormat.format(ieHand.startChips[seat]));
            buf.append(" in chips)");
            buf.append(newline);

            if (!player.isFolded())
            {
                lastPlayerLeft = player;
                ++playersLeft;
            }
        }

        int round = HoldemHand.ROUND_PRE_FLOP;

        for (int i = 0; i < ieHand.hist.size(); ++i)
        {
            HandAction action = (HandAction)ieHand.hist.get(i);

            while (action.getRound() > round)
            {
                switch (round++)
                {
                    case HoldemHand.ROUND_PRE_FLOP:
                        if (ieHand.community.size() >= 3)
                        {
                            buf.append("*** FLOP *** : [ ");
                            buf.append(ieHand.community.getCard(0).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(0).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(1).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(1).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(2).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(2).getSuitDisplay());
                            buf.append(" ]");
                            buf.append(newline);
                        }
                        break;
                    case HoldemHand.ROUND_FLOP:
                        if (ieHand.community.size() >= 4)
                        {
                            buf.append("*** TURN *** : [ ");
                            buf.append(ieHand.community.getCard(0).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(0).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(1).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(1).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(2).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(2).getSuitDisplay());
                            buf.append(" ] [ ");
                            buf.append(ieHand.community.getCard(3).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(3).getSuitDisplay());
                            buf.append(" ]");
                            buf.append(newline);
                        }
                        break;
                    case HoldemHand.ROUND_TURN:
                        if (ieHand.community.size() >= 5)
                        {
                            buf.append("*** RIVER *** : [ ");
                            buf.append(ieHand.community.getCard(0).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(0).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(1).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(1).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(2).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(2).getSuitDisplay());
                            buf.append(" ");
                            buf.append(ieHand.community.getCard(3).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(3).getSuitDisplay());
                            buf.append(" ] [ ");
                            buf.append(ieHand.community.getCard(4).getRankDisplaySingle());
                            buf.append(ieHand.community.getCard(4).getSuitDisplay());
                            buf.append(" ]");
                            buf.append(newline);
                        }
                        break;
                }
            }

            switch (action.getAction())
            {
                case HandAction.ACTION_ANTE:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Ante (");
                    buf.append(action.getAmount());
                    buf.append(")");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_BLIND_SM:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Post Small Blind (");
                    buf.append(action.getAmount());
                    buf.append(")");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_BLIND_BIG:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Post Big Blind (");
                    buf.append(action.getAmount());
                    buf.append(")");
                    buf.append(newline);
                    buf.append("Dealing...");
                    buf.append(newline);
                    buf.append("Dealt to ");
                    buf.append(paradisePlayerName_);
                    buf.append(" [ ");
                    buf.append(ieHand.players[ieHand.localHumanPlayerSeat].getHand().getCard(0).getRankDisplaySingle());
                    buf.append(ieHand.players[ieHand.localHumanPlayerSeat].getHand().getCard(0).getSuitDisplay());
                    buf.append(" ]");
                    buf.append(newline);
                    buf.append("Dealt to ");
                    buf.append(paradisePlayerName_);
                    buf.append(" [ ");
                    buf.append(ieHand.players[ieHand.localHumanPlayerSeat].getHand().getCard(1).getRankDisplaySingle());
                    buf.append(ieHand.players[ieHand.localHumanPlayerSeat].getHand().getCard(1).getSuitDisplay());
                    buf.append(" ]");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_FOLD:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Fold");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_CHECK:
                case HandAction.ACTION_CHECK_RAISE:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Check");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_CALL:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Call (");
                    buf.append(action.getAmount());
                    buf.append(")");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_BET:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Bet (");
                    buf.append(action.getAmount());
                    buf.append(")");
                    buf.append(newline);
                    break;
                case HandAction.ACTION_RAISE:
                    appendPlayerName(ieHand, buf, action.getPlayer());
                    buf.append(": Raise (");
                    buf.append(action.getAmount());
                    buf.append(")");
                    buf.append(newline);
                    break;
            }
        }

        if (playersLeft == 1)
        {
            appendPlayerName(ieHand, buf, lastPlayerLeft);
            buf.append(" : Winner");
            if (lastPlayerLeft.isCardsExposed())
            {
                buf.append(" -- doesn't show cards");
            }
            else
            {
                buf.append(" -- doesn't show cards");
            }
            buf.append(newline);
        }

        buf.append("*** SUMMARY ***");
        buf.append(newline);

        for (int i = 0; i < 9; ++i)  // can have a max of 9 pots
        {
            appendPot(ieHand, buf, i);
        }

        buf.append(" | ");

        if (ieHand.community.size() > 0)
        {
            buf.append("Board: [");
            for (int i = 0; i < ieHand.community.size(); ++i)
            {
                buf.append(" ");
                buf.append(ieHand.community.getCard(i).getRankDisplaySingle());
                buf.append(ieHand.community.getCard(i).getSuitDisplay());
            }
            buf.append(" ]");
            buf.append(newline);
        }

        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            PokerPlayer player = ieHand.players[seat];

            if (player == null) continue;

            appendPlayerName(ieHand, buf, player);

            if ((ieHand.startChips[seat] == ieHand.endChips[seat]) && (ieHand.winChips[seat] == 0))
            {
                buf.append(" didn't bet (folded)");
            }
            else if ((ieHand.startChips[seat] > ieHand.endChips[seat]) && (ieHand.winChips[seat] == 0))
            {
                buf.append(" lost ");
                buf.append(chipAmountFormat.format((ieHand.startChips[seat] - ieHand.endChips[seat])));
            }
            else
            {
                buf.append(" bet ");
                buf.append(chipAmountFormat.format(ieHand.betChips[seat]));
                buf.append(", collected ");
                buf.append(chipAmountFormat.format((ieHand.winChips[seat] + ieHand.overbetChips[seat])));
                buf.append(", net ");
                if (ieHand.endChips[seat] > ieHand.startChips[seat])
                {
                    buf.append("+");
                }
                buf.append(chipAmountFormat.format((ieHand.endChips[seat] - ieHand.startChips[seat])));
            }

            if (player.isCardsExposed())
            {
                buf.append(" (showed hand) [ ");
                buf.append(player.getHand().getCard(0).getRankDisplaySingle());
                buf.append(player.getHand().getCard(0).getSuitDisplay());
                buf.append(" ");
                buf.append(player.getHand().getCard(1).getRankDisplaySingle());
                buf.append(player.getHand().getCard(1).getSuitDisplay());
                buf.append(" ] (");
                appendFinalHand(buf, ieHand.community, player.getHand());
                buf.append(")");
            }

            buf.append(newline);
        }

        buf.append(newline);
        buf.append("-----------------------------------------------------");

        return buf.toString();
    }

    private void appendPlayerName(ImpExpHand ieHand, StringBuilder buf, PokerPlayer player)
    {
        if (player.getSeat() == ieHand.localHumanPlayerSeat)
        {
            buf.append(paradisePlayerName_);
        }
        else
        {
            buf.append(player.getName());
        }
    }

    private void appendPot(ImpExpHand ieHand, StringBuilder buf, int nPot)
    {
        HandAction action;

        int nNum = 0;
        int potTotal = 0;

        // first loop to count and sum

        for (int i = 0; i < ieHand.hist.size(); i++)
        {
            action = (HandAction) ieHand.hist.get(i);

            if (action.getRound() != HoldemHand.ROUND_SHOWDOWN) continue;
            if (action.getSubAmount() != nPot) continue;

            if (action.getAction() == HandAction.ACTION_WIN)
            {
                potTotal += action.getAmount();
            }

            nNum++;
        }

        if ((nNum > 0) && (potTotal > 0))
        {
            if (nPot == 0)
            {
                buf.append("Pot: ");
            }
            else
            {
                buf.append(" | Side pot ");
                buf.append(nPot);
                buf.append(": ");
            }

            buf.append(chipAmountFormat.format(potTotal));
        }
    }

    private static final String rankName_[] = new String[]
    {
        null,null,
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "jack",
        "queen",
        "king",
        "ace"
    };

    private static final String rankPName_[] = new String[]
    {
        null,null,
        "twos",
        "threes",
        "fours",
        "fives",
        "sixes",
        "sevens",
        "eights",
        "nines",
        "tens",
        "jacks",
        "queens",
        "kings",
        "aces"
    };

    private void appendFinalHand(StringBuilder buf, Hand hand, Hand community)
    {
        int score = info.getScore(hand, community);
        
        switch (info.getHandType())
        {
            case HandInfo.ROYAL_FLUSH:
                buf.append("a royal flush");
                break;
            case HandInfo.STRAIGHT_FLUSH:
                buf.append("a straight flush, ");
                buf.append(rankName_[info.getStraightLowRank()]);
                buf.append(" to ");
                buf.append(rankName_[info.getStraightHighRank()]);
                break;
            case HandInfo.QUADS:
                buf.append("four of a kind, ");
                buf.append(rankPName_[info.getQuadsRank()]);
                break;
            case HandInfo.FULL_HOUSE:
                buf.append("a full house, ");
                buf.append(rankPName_[info.getTripsRank()]);
                buf.append(" full of ");
                buf.append(rankPName_[info.getBigPairRank()]);
                break;
            case HandInfo.FLUSH:
                buf.append("a flush, ");
                buf.append(rankName_[info.getFlushHighRank()]);
                buf.append(" high");
                break;
            case HandInfo.STRAIGHT:
                buf.append("a straight, ");
                buf.append(rankName_[info.getStraightLowRank()]);
                buf.append(" to ");
                buf.append(rankName_[info.getStraightHighRank()]);
                break;
            case HandInfo.TRIPS:
                buf.append("three of a kind, ");
                buf.append(rankPName_[info.getTripsRank()]);
                break;
            case HandInfo.TWO_PAIR:
                buf.append("two pair, ");
                buf.append(rankPName_[info.getBigPairRank()]);
                buf.append(" and ");
                buf.append(rankPName_[info.getSmallPairRank()]);
                break;
            case HandInfo.PAIR:
                buf.append("a pair of ");
                buf.append(rankPName_[info.getBigPairRank()]);
                break;
            case HandInfo.HIGH_CARD:
                buf.append("high card ");
                buf.append(rankName_[info.getHighCardRank()]);
                break;
        }
    }
}
