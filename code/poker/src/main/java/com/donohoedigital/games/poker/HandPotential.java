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
/*
 * HandPotential.java
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

import java.util.*;

@SuppressWarnings({"DuplicatedCode", "CommentedOutCode", "StringConcatenationArgumentToLogCall"})
public class HandPotential
{
    /**
     * Statistics array indexes.
     */
    public static final int ROYAL_FLUSH;
    public static final int STRAIGHT_FLUSH;
    public static final int FOUR_OF_A_KIND;
    public static final int FULL_HOUSE;
    public static final int FULL_HOUSE_PAIR_ON_BOARD;
    public static final int FULL_HOUSE_TRIPS_ON_BOARD;
    public static final int THREE_OF_A_KIND;
    public static final int SET;
    public static final int TRIPS;
    public static final int TRIPS_ON_BOARD;
    public static final int TWO_PAIR;
    public static final int TWO_PAIR_NO_PAIR_ON_BOARD;
    public static final int TWO_PAIR_PAIR_ON_BOARD;
    public static final int PAIR;
    public static final int OVERPAIR;
    public static final int TOP_PAIR;
    public static final int TOP_PAIR_WITH_OVERCARD;
    public static final int MIDDLE_OR_BOTTOM_PAIR;
    public static final int MIDDLE_PAIR_WITH_OVERCARD;
    public static final int BOTTOM_PAIR_WITH_OVERCARD;
    public static final int MIDPAIR;
    public static final int UNDERPAIR;
    public static final int PAIR_ON_BOARD;
    public static final int PAIR_ON_BOARD_WITH_ONE_OVERCARD;
    public static final int PAIR_ON_BOARD_WITH_TWO_OVERCARDS;
    public static final int FLUSH;
    public static final int NUT_FLUSH;
    public static final int SECOND_NUT_FLUSH;
    public static final int WEAK_FLUSH;
    public static final int STRAIGHT;
    public static final int NUT_STRAIGHT;
    public static final int NON_NUT_STRAIGHT;

    /**
     * Statistics array indexes for draws.
     */
    public static final int FLUSH_DRAW;
    public static final int FLUSH_DRAW_WITH_TWO_CARDS;
    public static final int NUT_FLUSH_DRAW_WITH_TWO_CARDS;
    public static final int SECOND_NUT_FLUSH_DRAW_WITH_TWO_CARDS;
    public static final int WEAK_FLUSH_DRAW_WITH_TWO_CARDS;
    public static final int FLUSH_DRAW_WITH_ONE_CARD;
    public static final int NUT_FLUSH_DRAW_WITH_ONE_CARD;
    public static final int SECOND_NUT_FLUSH_DRAW_WITH_ONE_CARD;
    public static final int WEAK_FLUSH_DRAW_WITH_ONE_CARD;
    public static final int STRAIGHT_DRAW;
    public static final int STRAIGHT_DRAW_8_OUTS;
    public static final int STRAIGHT_DRAW_6_OUTS;
    public static final int STRAIGHT_DRAW_4_OUTS;
    public static final int STRAIGHT_DRAW_3_OUTS;
    public static final int TWO_OVERCARDS;
    public static final int ONE_OVERCARD;
    public static final int HIGH_CARD;

    private static final ArrayList<String> aStatKeys_ = new ArrayList<>();
    private static final ArrayList<Boolean> bStatKeys_ = new ArrayList<>();
    private static final int nFirstDrawKey_;

    private static Logger logger;

    private final Hand pocket_;
    private final Hand community_;

    private final int[][] handCounts_ = new int[aStatKeys_.size()][2];
    private final int[] totalHandCount_ = new int[2];

    // debug
    private static final boolean PERF = false;
    
    // indicies into hp[3] array
    private static final int AHEAD = 0;
    private static final int TIED = 1;
    private static final int BEHIND = 2;
    
    static
    {
        ROYAL_FLUSH = addStatKey("royalflush", true);
        STRAIGHT_FLUSH = addStatKey("straightflush", true);
        FOUR_OF_A_KIND = addStatKey("fourofakind", true);
        FULL_HOUSE = addStatKey("fullhouse", true);
            FULL_HOUSE_PAIR_ON_BOARD = addStatKey("fullhousepob", false);
            FULL_HOUSE_TRIPS_ON_BOARD = addStatKey("fullhousetob", false);
        FLUSH = addStatKey("flush", true);
            NUT_FLUSH = addStatKey("nutflush", false);
            SECOND_NUT_FLUSH = addStatKey("2ndnutflush", false);
            WEAK_FLUSH = addStatKey("weakflush", false);
        STRAIGHT = addStatKey("straight", true);
            NUT_STRAIGHT = addStatKey("nutstraight", false);
            NON_NUT_STRAIGHT = addStatKey("nonnutstraight", false);
        THREE_OF_A_KIND = addStatKey("threeofakind", true);
            SET = addStatKey("set", false);
            TRIPS = addStatKey("trips", false);
            TRIPS_ON_BOARD = addStatKey("tripsonboard", false);
        TWO_PAIR = addStatKey("twopair", true);
            TWO_PAIR_NO_PAIR_ON_BOARD = addStatKey("twopairnpob", false);
            TWO_PAIR_PAIR_ON_BOARD = addStatKey("twopairpob", false);
        PAIR = addStatKey("pair", true);
            OVERPAIR = addStatKey("overpair", false);
            TOP_PAIR = addStatKey("toppair", false);
            TOP_PAIR_WITH_OVERCARD = addStatKey("toppairwithover", false);
            MIDDLE_OR_BOTTOM_PAIR = addStatKey("middleorbottompair", false);
            MIDDLE_PAIR_WITH_OVERCARD = addStatKey("middlepairwithover", false);
            BOTTOM_PAIR_WITH_OVERCARD = addStatKey("bottompairwithover", false);
            MIDPAIR = addStatKey("midpair", false);
            UNDERPAIR = addStatKey("underpair", false);
            PAIR_ON_BOARD = addStatKey("paironboard", false);
            PAIR_ON_BOARD_WITH_TWO_OVERCARDS = addStatKey("pobwith2over", false);
            PAIR_ON_BOARD_WITH_ONE_OVERCARD = addStatKey("pobwith1over", false);
            TWO_OVERCARDS = addStatKey("2over", false);
            ONE_OVERCARD = addStatKey("1over", false);
        HIGH_CARD = addStatKey("highcard", true);
        nFirstDrawKey_ = FLUSH_DRAW = addStatKey("flushdraw", true);
            FLUSH_DRAW_WITH_TWO_CARDS = addStatKey("flushdraw2cards", false);
            NUT_FLUSH_DRAW_WITH_TWO_CARDS = addStatKey("nutflushdraw2cards", false);
            SECOND_NUT_FLUSH_DRAW_WITH_TWO_CARDS = addStatKey("2ndnutflushdraw2cards", false);
            WEAK_FLUSH_DRAW_WITH_TWO_CARDS = addStatKey("weakflushdraw2cards", false);
            FLUSH_DRAW_WITH_ONE_CARD = addStatKey("flushdraw1card", false);
            NUT_FLUSH_DRAW_WITH_ONE_CARD = addStatKey("nutflushdraw1card", false);
            SECOND_NUT_FLUSH_DRAW_WITH_ONE_CARD = addStatKey("2ndnutflushdraw1card", false);
            WEAK_FLUSH_DRAW_WITH_ONE_CARD = addStatKey("weakflushdraw1card", false);
        STRAIGHT_DRAW = addStatKey("straightdraw", true);
            STRAIGHT_DRAW_8_OUTS = addStatKey("straightdraw8outs", false);
            STRAIGHT_DRAW_6_OUTS = addStatKey("straightdraw6outs", false);
            STRAIGHT_DRAW_4_OUTS = addStatKey("straightdraw4outs", false);
            STRAIGHT_DRAW_3_OUTS = addStatKey("straightdraw3outs", false);
    }

    /**
     * Allocates a slot in the statistics array for the named accumulator.<br>
     * <br>
     * The messageKey is looked up as <code>msg.handstats.</code><i>messageKey</i><code>
     *
     * @return newly assigned array index
     */
    private static int addStatKey(String messageKey, boolean bBold)
    {
        aStatKeys_.add(messageKey);
        bStatKeys_.add(bBold ? Boolean.TRUE : Boolean.FALSE);

        return aStatKeys_.size()-1;
    }

    /**
     * Constructor for pre-flop analysis.
     */
    @SuppressWarnings("unused")
    public HandPotential(Hand pocket) {

        this(pocket, null);
    }

    /**
     * Constructor for post-flop analysis.
     */
    public HandPotential(Hand pocket, Hand community)
    {
        pocket_ = pocket;
        community_ = community;

        calculate();
    }

    private void calculate() {

        Deck deck = new Deck(false);

        deck.removeAll(pocket_);

        if (community_ != null)
        {
            deck.removeAll(community_);
        }

        HandInfoFast infoHand = new HandInfoFast();

        Hand community;

        switch ((community_ == null) ? 0 : community_.size()) {
            case 0:
                community = new Hand(null, null, null);
                for (int i = 0; i < deck.size() - 2; ++i)
                {
                    for (int j = i + 1; j < deck.size() - 1; ++j)
                    {
                        for (int k = j + 1; k < deck.size(); ++k)
                        {
                            community.setCard(0, deck.getCard(i));
                            community.setCard(1, deck.getCard(j));
                            community.setCard(2, deck.getCard(k));
                            processHand(infoHand, community, 0);
                        }
                    }
                }
                break;
            case 1:
                community = new Hand(community_.getCard(0), null, null);
                for (int i = 0; i < deck.size() - 1; ++i)
                {
                    for (int j = i + 1; j < deck.size(); ++j)
                    {
                        community.setCard(1, deck.getCard(i));
                        community.setCard(2, deck.getCard(j));
                        processHand(infoHand, community, 0);
                    }
                }
                break;
            case 2:
                community = new Hand(community_.getCard(0), community_.getCard(1), null);
                for (int i = 0; i < deck.size(); ++i)
                {
                    community.setCard(2, deck.getCard(i));
                    processHand(infoHand, community, 0);
                }
                break;
            case 3:
                community = new Hand
                    (community_.getCard(0), community_.getCard(1), community_.getCard(2), null);
                for (int i = 0; i < deck.size(); ++i)
                {
                    community.setCard(3, deck.getCard(i));
                    processHand(infoHand, community, 0);
                }
                community = new Hand
                        (community_.getCard(0), community_.getCard(1), community_.getCard(2), null, null);
                for (int i = 0; i < deck.size() - 1; ++i)
                {
                    for (int j = i + 1; j < deck.size(); ++j)
                    {
                        community.setCard(3, deck.getCard(i));
                        community.setCard(4, deck.getCard(j));
                        processHand(infoHand, community, 1);
                    }
                }
                break;
            case 4:
                community = new Hand
                    (community_.getCard(0), community_.getCard(1), community_.getCard(2), community_.getCard(3), null);
                for (int i = 0; i < deck.size(); ++i)
                {
                    community.setCard(4, deck.getCard(i));
                    processHand(infoHand, community, 0);
                }
                break;
        }

    }

    private void processHand(HandInfoFast infoHand, Hand community, int index)
    {
        ++totalHandCount_[index];

        int straightDrawOuts;

        infoHand.getScore(pocket_, community);

        straightDrawOuts = infoHand.getStraightDrawOuts();

        if (straightDrawOuts > 0)
        {
            ++handCounts_[STRAIGHT_DRAW][index];
        }

        switch (straightDrawOuts)
        {
            case 8:
                ++handCounts_[STRAIGHT_DRAW_8_OUTS][index];
                break;
            case 6:
                ++handCounts_[STRAIGHT_DRAW_6_OUTS][index];
                break;
            case 4:
                ++handCounts_[STRAIGHT_DRAW_4_OUTS][index];
                break;
            case 3:
                ++handCounts_[STRAIGHT_DRAW_3_OUTS][index];
                break;
            case 0:
                break;
        }
        if (infoHand.hasFlushDraw())
        {
            ++handCounts_[FLUSH_DRAW][index];
            switch (infoHand.getFlushDrawPocketsPlayed())
            {
                case 2:
                    ++handCounts_[FLUSH_DRAW_WITH_TWO_CARDS][index];
                    if (infoHand.hasNutFlushDraw())
                    {
                        ++handCounts_[NUT_FLUSH_DRAW_WITH_TWO_CARDS][index];
                    }
                    else if (infoHand.has2ndNutFlushDraw())
                    {
                        ++handCounts_[SECOND_NUT_FLUSH_DRAW_WITH_TWO_CARDS][index];
                    }
                    else
                    {
                        ++handCounts_[WEAK_FLUSH_DRAW_WITH_TWO_CARDS][index];
                    }
                    break;
                case 1:
                    ++handCounts_[FLUSH_DRAW_WITH_ONE_CARD][index];
                    if (infoHand.hasNutFlushDraw())
                    {
                        ++handCounts_[NUT_FLUSH_DRAW_WITH_ONE_CARD][index];
                    }
                    else if (infoHand.has2ndNutFlushDraw())
                    {
                        ++handCounts_[SECOND_NUT_FLUSH_DRAW_WITH_ONE_CARD][index];
                    }
                    else
                    {
                        ++handCounts_[WEAK_FLUSH_DRAW_WITH_ONE_CARD][index];
                    }
                    break;
            }
        }

        switch (infoHand.getHandType())
        {
            case HandInfo.ROYAL_FLUSH:
                ++handCounts_[ROYAL_FLUSH][index];
                break;
            case HandInfo.STRAIGHT_FLUSH:
                ++handCounts_[STRAIGHT_FLUSH][index];
                break;
            case HandInfo.QUADS:
                ++handCounts_[FOUR_OF_A_KIND][index];
                break;
            case HandInfo.FLUSH:
                ++handCounts_[FLUSH][index];
                switch (infoHand.getBetterFlushCardCount())
                {
                    case 0:
                        ++handCounts_[NUT_FLUSH][index];
                        break;
                    case 1:
                        ++handCounts_[SECOND_NUT_FLUSH][index];
                        break;
                    default:
                        ++handCounts_[WEAK_FLUSH][index];
                        break;
                }
                break;
            case HandInfo.FULL_HOUSE:
                ++handCounts_[FULL_HOUSE][index];
                if (community.hasTrips())
                {
                    ++handCounts_[FULL_HOUSE_TRIPS_ON_BOARD][index];
                }
                else
                {
                    ++handCounts_[FULL_HOUSE_PAIR_ON_BOARD][index];
                }
                break;
            case HandInfo.STRAIGHT:
                ++handCounts_[STRAIGHT][index];
                if (infoHand.getStraightHighRank() == infoHand.getNutStraightHighRank())
                {
                    ++handCounts_[NUT_STRAIGHT][index];
                }
                else
                {
                    ++handCounts_[NON_NUT_STRAIGHT][index];
                }
                break;
            case HandInfo.TRIPS:
                ++handCounts_[THREE_OF_A_KIND][index];
                if (pocket_.hasPair())
                {
                    ++handCounts_[SET][index];
                }
                else
                {
                    if (community.hasTrips())
                    {
                        ++handCounts_[TRIPS_ON_BOARD][index];
                    }
                    else
                    {
                        ++handCounts_[TRIPS][index];
                    }
                }
                break;
            case HandInfo.TWO_PAIR:
                ++handCounts_[TWO_PAIR][index];
                if (pocket_.hasPair())
                {
                    ++handCounts_[TWO_PAIR_PAIR_ON_BOARD][index];
                }
                else
                {
                    if (community.hasPair())
                    {
                        ++handCounts_[TWO_PAIR_PAIR_ON_BOARD][index];
                    }
                    else
                    {
                        ++handCounts_[TWO_PAIR_NO_PAIR_ON_BOARD][index];
                    }
                }
                break;
            case HandInfo.PAIR:

                ++handCounts_[PAIR][index];

                int pairRank = infoHand.getBigPairRank();

                if (pocket_.hasPair())
                {
                    if (pairRank > infoHand.getHighestBoardRank())
                    {
                        ++handCounts_[OVERPAIR][index];
                    }
                    else if (pairRank < infoHand.getLowestBoardRank())
                    {
                        ++handCounts_[UNDERPAIR][index];
                    }
                    else
                    {
                        ++handCounts_[MIDPAIR][index];
                    }
                }
                else if (community.hasPair())
                {
                    ++handCounts_[PAIR_ON_BOARD][index];

                    switch (infoHand.getOvercardCount())
                    {
                        case 2:
                            ++handCounts_[PAIR_ON_BOARD_WITH_TWO_OVERCARDS][index];
                            break;
                        case 1:
                            ++handCounts_[PAIR_ON_BOARD_WITH_ONE_OVERCARD][index];
                            break;
                    }
                }
                else
                {
                    if (pairRank == infoHand.getHighestBoardRank())
                    {
                        ++handCounts_[TOP_PAIR][index];

                        if (infoHand.getOvercardCount() > 0)
                        {
                            ++handCounts_[TOP_PAIR_WITH_OVERCARD][index];
                        }
                    }
                    else
                    {
                        ++handCounts_[MIDDLE_OR_BOTTOM_PAIR][index];

                        if (infoHand.getOvercardCount() > 0)
                        {
                            if (pairRank == infoHand.getLowestBoardRank())
                            {
                                ++handCounts_[BOTTOM_PAIR_WITH_OVERCARD][index];
                            }
                            else
                            {
                                ++handCounts_[MIDDLE_PAIR_WITH_OVERCARD][index];
                            }
                        }
                    }
                }
                break;
            case HandInfo.HIGH_CARD:
                ++handCounts_[HIGH_CARD][index];
                switch (infoHand.getOvercardCount())
                {
                    case 2:
                        ++handCounts_[TWO_OVERCARDS][index];
                        break;
                    case 1:
                        ++handCounts_[ONE_OVERCARD][index];
                        break;
                }
                break;
        }
    }

    /**
     * Returns presentable HTML summarizing results.
     */
    public String toHTML()
    {
        return toHTML(HoldemHand.ROUND_NONE);
    }

    /**
     * Returns presentable HTML summarizing results.
     */
    public String toHTML(int round)
    {
        StringBuilder buf = new StringBuilder();

        int stage;

        if (community_ == null)
        {
            stage = 0;
        }
        else
        {
            switch (community_.size())
            {
                case 0:
                    stage = 0;
                    break;
                case 3:
                    stage = 1;
                    break;
                default:
                    stage = 2;
            }
        }

        boolean twoColumns = ((stage == 1) && (round == HoldemHand.ROUND_NONE));

        buf.append("<table width=\"100%\">");

        if (twoColumns)
        {
            buf.append("<tr>");
            buf.append("<td rowspan=\"2\"><font color=\"white\"><b>Hand Potential</b></font></td>");
            buf.append("<td colspan=\"3\" align=\"center\"><font color=\"white\"><b>Turn</b></font></td>");
            buf.append("<td colspan=\"3\" align=\"center\"><font color=\"white\"><b>River</b></font></td>");
            buf.append("</tr>");
            buf.append("<tr>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Count</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Probability</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Odds</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Count</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Probability</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Odds</b></font></td>");
            buf.append("</tr>");
        }
        else
        {
            buf.append("<tr>");
            buf.append("<td><font color=\"white\"><b>Hand Potential</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Count</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Probability</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Odds</b></font></td>");
            buf.append("</tr>");
        }

        String message;

        int handCountA;
        int handCountB = 0;

        int a;
        boolean bold;

        for (int i = 0; i < aStatKeys_.size(); ++i)
        {
            if (twoColumns)
            {
                a = 0;
                handCountA = handCounts_[i][0];
                handCountB = handCounts_[i][1];
            }
            else
            {
                a = ((round == HoldemHand.ROUND_RIVER) && (stage == 1)) ? 1 : 0;
                handCountA = handCounts_[i][a];
            }

            if (((handCountA > 0) || (handCountB > 0)) && ((stage != 2) || i < nFirstDrawKey_))
            {
                // special case ; full house without pocket pair requires no breakdown
                if ((i == FULL_HOUSE_PAIR_ON_BOARD) || (i == FULL_HOUSE_TRIPS_ON_BOARD))
                {
                    if (!pocket_.hasPair()) continue;
                }

                // special case ; flush draws don't need to be broken out by 1 or 2 cards played
                // until we implement omaha, if ever, since in hold'em they're mutually exclusive
                if ((i == FLUSH_DRAW_WITH_ONE_CARD) || (i == FLUSH_DRAW_WITH_TWO_CARDS))
                {
                    continue;
                }

                bold = bStatKeys_.get(i);
                message = PropertyConfig.getMessage("msg.handstats." + aStatKeys_.get(i));

                buf.append("<tr>");

                buf.append("<td>");
                buf.append(bold(message, bold));
                buf.append("</td>");

                if (handCountA > 0)
                {
                    buf.append("<td align=\"right\">");
                    buf.append(bold(""+handCountA, bold));
                    buf.append("</td><td align=\"right\">");
                    buf.append(bold(PokerConstants.formatPercent(handCountA / ((float) totalHandCount_[a] / 100)) + "%", bold));
                    buf.append("</td><td align=\"right\">");
                    buf.append(bold(PokerConstants.formatPercent(((float) totalHandCount_[a] - handCountA) / handCountA) + ":1", bold));
                    buf.append("</td>");
                }
                else if (twoColumns)
                {
                    buf.append("<td colspan=\"3\"></td>");
                }

                if (twoColumns)
                {
                    if ((handCountB > 0) && (i < nFirstDrawKey_))
                    {
                        buf.append("<td align=\"right\">");
                        buf.append(bold(""+handCountB, bold));
                        buf.append("</td><td align=\"right\">");
                        buf.append(bold(PokerConstants.formatPercent(handCountB / ((float) totalHandCount_[1] / 100)) + "%", bold));
                        buf.append("</td><td align=\"right\">");
                        buf.append(bold(PokerConstants.formatPercent(((float) totalHandCount_[1] - handCountB) / handCountB) + ":1", bold));
                        buf.append("</td>");
                    }
                    else
                    {
                        buf.append("<td colspan=\"3\"></td>");
                    }
                }

                buf.append("</tr>");
                buf.append("\n");
            }
        }

        buf.append("</table>");

        return buf.toString();
    }

    private String bold(String s, boolean bold)
    {
        if (!bold) return s;
        return "<font color=#0000BB>" + s + "</font>";
    }

    public int getHandCount(int type, int index)
    {
        return handCounts_[type][index];
    }

    /**
     * Calculate strength of hand against N opponents.  Return is float from
     * 0 to 1 indicating probability of this hand being the best hand
     */
    @SuppressWarnings("ConstantValue")
    public static float getPotential(Hand hole, Hand community)
    {
        if (PERF) Perf.start();
        
        // init (use floats for counting to avoid casting)
        float[][] hp = new float[3][3];
        float[] hpTotal = new float[3];
        Hand holecopy = new Hand(hole); // copy for reuse
        Hand commcopy = new Hand(community); // copy for reuse
        int nComm = community.size();
        int MORE = 5 - nComm;
        if (MORE == 2) MORE = 1; // TESTING force to 1 card more
        ApplicationError.assertTrue(MORE == 2 || MORE == 1, "HandPotential called with wrong board", community);
        
        // our rank
        HandInfoFaster FAST = new HandInfoFaster();
        int ourscore = FAST.getScore(hole, community);
        
        // get remaining cards (new deck less hole, community)
        Deck deck = new Deck(false);
        deck.removeCards(hole);
        deck.removeCards(community);
        
        // more init
        int oppscore;
        int nSize = deck.size();
        int index, ourbest, oppbest;
        
        // loop through deck for all possible opponent cards
        for (int i = 0; i < nSize - 1; i++)
        {
            for (int j = i+1; j < nSize; j++)
            {
                holecopy.setCard(0, deck.getCard(i));
                holecopy.setCard(1, deck.getCard(j));
                oppscore = FAST.getScore(holecopy, community);
                
                if (ourscore > oppscore) index = AHEAD;
                else if (ourscore == oppscore) index = TIED;
                else index = BEHIND;
                
                int nEND = nSize - 1;
                if (MORE == 1) nEND = nSize;

                for (int next1 = 0; next1 < nEND; next1 ++)
                {
                    if (next1 == i || next1 == j) continue;
                    commcopy.addCard(deck.getCard(next1));
                    
                    for (int next2 = next1 + 1; next2 < nSize; next2++)
                    {
                        // if we need 2 cards, get next one
                        if (MORE == 2)
                        {
                            if (next2 == i || next2 == j) continue; 
                            commcopy.addCard(deck.getCard(next2));
                        }
                        
                        hpTotal[index]++;

                        // calc
                        ourbest = FAST.getScore(hole, commcopy);
                        oppbest = FAST.getScore(holecopy, commcopy);
                        if (ourbest > oppbest)          hp[index][AHEAD]++;
                        else if (ourbest == oppbest)    hp[index][TIED]++;
                        else                            hp[index][BEHIND]++;
                        
                        //logger.debug(index+ " check: " + hole + " (" + ourbest +") , " + holecopy + " ("+oppbest+"), " + commcopy);
                        
                        // remove 2nd card added
                        if (MORE == 2)
                        {
                            commcopy.removeCard(commcopy.size() - 1);
                        }
                        
                        // if we only needed one, don't loop anymore
                        // this is a "hack" way to avoid repeating calc
                        // code above (note that if MORE == 1, a second
                        // card was not added to the board)
                        if (MORE == 1)
                        {
                            break;
                        }
                    }
                    
                    // remove 1st card added
                    commcopy.removeCard(commcopy.size() - 1);
                }
            }
        }
        
        // positive potential: were behind, but moved up to tie/ahead
        float ppot = (hp[BEHIND][AHEAD] + hp[BEHIND][TIED]/2 + hp[TIED][AHEAD]/2) / (hpTotal[BEHIND] + hpTotal[TIED]/2);
        
        // negative potential: were ahead or tied, but moved down to tie/behind
        float npot = (hp[AHEAD][BEHIND] + hp[TIED][BEHIND]/2 + hp[AHEAD][TIED]/2) / (hpTotal[AHEAD] + hpTotal[TIED]/2);
        
        if (PERF) Perf.stop();
        
//        StringBuilder log = new StringBuilder("Array:\n");
//        int sum = 0;
//        for (int i = 0; i < 3; i++)
//        {
//            sum = 0;
//            for (int j = 0; j < 3; j++)
//            {
//                log.append(HandStat.fChip.form((int)hp[i][j]));
//                sum += hp[i][j];
//            }
//            log.append(HandStat.fChip.form(sum));
//            log.append("\n");
//        }
//        logger.debug(log.toString());
        
        if ((TESTING(EngineConstants.TESTING_AI_DEBUG)))
        {
            logger.debug("POTENTIAL for " + hole +"," + community+ ": " + 
                HandStat.fPerc.form(ppot) + " ppot   " +
                HandStat.fPerc.form(npot) + " npot");
        }
        return ppot;
    }
    
    /**
     * Testing
     */
    public static void main(String[] args)
    {
        LoggingConfig loggingConfig = new LoggingConfig("plain", ApplicationType.COMMAND_LINE);
        loggingConfig.init();
        logger = LogManager.getLogger(HandPotential.class);

        Perf.setOn(true);
        new ConfigManager("poker", ApplicationType.CLIENT);

        new Hand(Card.CLUBS_3, Card.CLUBS_5);
        Hand hand;
        Hand comm = new Hand(Card.CLUBS_4, Card.DIAMONDS_6);
        comm.addCard(Card.CLUBS_K);
        
        logger.debug("Start");
        
//        for (int i = 0; i < 5; i++)
//        {
//            getPotential(hand, comm);
//        }
        
        hand = new Hand(Card.DIAMONDS_A, Card.CLUBS_Q);
        comm = new Hand(Card.HEARTS_J, Card.CLUBS_4);
        comm.addCard(Card.HEARTS_3);
        
        getPotential(hand, comm);
    }
    
}
