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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.config.*;

public class HandLadder
{
    private static final int HIGHER_TYPE = 0;
    private static final int SAME_TYPE_HIGHER_RANK = 1;
    private static final int SAME_HAND_HIGHER_KICKER = 2;
    private static final int SAME_HAND = 3;
    private static final int SAME_HAND_LOWER_KICKER = 4;
    private static final int SAME_TYPE_LOWER_RANK = 5;
    private static final int LOWER_TYPE = 6;

    private Hand pocket_;
    private Hand community_;

    private int handScore_;
    private int handType_;

    private HandList ladder_[];

    private HandList strongerHandsByType_[];
    private HandList weakerHandsByType_[];

    private int countByType_[];
    private int totalCount_;

    HandInfoFast handInfo_ = new HandInfoFast();
    HandProbabilityMatrix matrix_;

    public HandLadder(Hand pocket, Hand community, HandProbabilityMatrix matrix)
    {
        pocket_ = pocket;
        community_ = community;
        matrix_ = matrix;

        ladder_ = new HandList[7];

        for (int i = 0; i < 7; ++i)
        {
            ladder_[i] = new HandList();
        }

        totalCount_ = 0;

        countByType_ = new int[HandInfo.ROYAL_FLUSH + 1];

        strongerHandsByType_ = new HandList[HandInfo.ROYAL_FLUSH + 1];

        weakerHandsByType_ = new HandList[HandInfo.ROYAL_FLUSH + 1];

        for (int i = HandInfo.HIGH_CARD; i <= HandInfo.ROYAL_FLUSH; ++i)
        {
            strongerHandsByType_[i] = new HandList();
            weakerHandsByType_[i] = new HandList();
        }

        calculate();
    }

    private void calculate()
    {
        Deck deck = new Deck(false);

        deck.removeCards(pocket_);
        deck.removeCards(community_);

        deck.sortDescending();

        HandInfoFast opponentInfo = new HandInfoFast();

        HandSorted opponent;

        int opponentScore;
        int opponentType;

        int ladderCategory = -1;

        handScore_ = handInfo_.getScore(pocket_, community_);
        handType_ = HandInfoFast.getTypeFromScore(handScore_);

        for (int i = 0; i < deck.size()-1; ++i)
        {
            for (int j = i+1; j < deck.size(); ++j)
            {
                ++totalCount_;

                opponent = new HandSorted(deck.getCard(i), deck.getCard(j));

                opponentScore = opponentInfo.getScore(opponent, community_);
                opponentType = HandInfoFast.getTypeFromScore(opponentScore);

                ++countByType_[opponentType];

                if (opponentScore == handScore_)
                {
                    ladderCategory = SAME_HAND;
                }
                else if (handType_ != opponentType)
                {
                    if (opponentScore > handScore_)
                    {
                        strongerHandsByType_[opponentType].add(opponent);
                        ladderCategory = HIGHER_TYPE;
                    }
                    else
                    {
                        weakerHandsByType_[opponentType].add(opponent);
                        ladderCategory = LOWER_TYPE;
                    }
                }
                else
                {
                    switch (handType_)
                    {
                        case HandInfo.STRAIGHT_FLUSH:
                        case HandInfo.FULL_HOUSE:
                        case HandInfo.STRAIGHT:
                            ladderCategory = (opponentScore > handScore_) ? SAME_TYPE_HIGHER_RANK : SAME_TYPE_LOWER_RANK;
                            break;
                        case HandInfo.QUADS:
                            ladderCategory = (handInfo_.getQuadsRank() == opponentInfo.getQuadsRank()) ?
                                ((opponentScore > handScore_) ? SAME_HAND_HIGHER_KICKER : SAME_HAND_LOWER_KICKER) :
                                ((opponentScore > handScore_) ? SAME_TYPE_HIGHER_RANK : SAME_TYPE_LOWER_RANK);
                            break;
                        case HandInfo.FLUSH:
                            ladderCategory = (handInfo_.getFlushHighRank() == opponentInfo.getFlushHighRank()) ?
                                ((opponentScore > handScore_) ? SAME_HAND_HIGHER_KICKER : SAME_HAND_LOWER_KICKER) :
                                ((opponentScore > handScore_) ? SAME_TYPE_HIGHER_RANK : SAME_TYPE_LOWER_RANK);
                            break;
                        case HandInfo.TRIPS:
                            ladderCategory = (handInfo_.getTripsRank() == opponentInfo.getTripsRank()) ?
                                ((opponentScore > handScore_) ? SAME_HAND_HIGHER_KICKER : SAME_HAND_LOWER_KICKER) :
                                ((opponentScore > handScore_) ? SAME_TYPE_HIGHER_RANK : SAME_TYPE_LOWER_RANK);
                            break;
                        case HandInfo.TWO_PAIR:
                        case HandInfo.PAIR:
                            ladderCategory = ((handInfo_.getBigPairRank() == opponentInfo.getBigPairRank()) &&
                                              (handInfo_.getSmallPairRank() == opponentInfo.getSmallPairRank())) ?
                                ((opponentScore > handScore_) ? SAME_HAND_HIGHER_KICKER : SAME_HAND_LOWER_KICKER) :
                                ((opponentScore > handScore_) ? SAME_TYPE_HIGHER_RANK : SAME_TYPE_LOWER_RANK);
                            break;
                        case HandInfo.HIGH_CARD:
                            ladderCategory = (handInfo_.getHighCardRank() == opponentInfo.getHighCardRank()) ?
                                ((opponentScore > handScore_) ? SAME_HAND_HIGHER_KICKER : SAME_HAND_LOWER_KICKER) :
                                ((opponentScore > handScore_) ? SAME_TYPE_HIGHER_RANK : SAME_TYPE_LOWER_RANK);
                            break;
                        default:
                            ApplicationError.assertTrue(false, "Unknown hand type " + handType_ + "!");
                    }
                }

                ladder_[ladderCategory].add(opponent);
            }
        }
    }

    public int getHandRank()
    {
        return  ladder_[HIGHER_TYPE].size() +
                ladder_[SAME_TYPE_HIGHER_RANK].size() +
                ladder_[SAME_HAND_HIGHER_KICKER].size() + 1;
    }

    public int getHandCount()
    {
        return totalCount_ + 1;
    }

    public String toHTML()
    {
        StringBuilder buf = new StringBuilder();

        buf.append("<font color=\"white\"><b>Summary</b></font>");

        buf.append("<table width=\"100%\">");

        buf.append("<tr><td colspan=\"3\">Current hand is <B>");
        buf.append(handInfo_);
        buf.append("</B></td></tr>");

        buf.append("<tr><td colspan=\"3\">&nbsp;&nbsp;&nbsp;Ranked <B>");
        int nRank = getHandRank();
        buf.append(PropertyConfig.getPlace(nRank));
        buf.append("</B> of ");
        buf.append(totalCount_);
        buf.append(" possible hands.</td></tr>");

        double strongerPercent = (100.0d * (getHandRank() - 1)) / totalCount_;
        double lessEqualPercent = 100.0d - strongerPercent;

        if (getHandRank() > 1)
        {
            buf.append("<tr>");
            buf.append("<td><font color=\"white\"><b>Stronger Hands - " + PokerConstants.formatPercent(strongerPercent) + "%</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Count</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Probability</b></font></td>");
            buf.append("</tr>");

            if (ladder_[HIGHER_TYPE].size() > 0)
            {
                for (int i = HandInfo.ROYAL_FLUSH; i > handType_; --i)
                {
                    if (countByType_[i] > 0)
                    {
                        buf.append("<tr><td>");
                        buf.append(HandInfo.getHandTypeDesc(i));
                        buf.append("</td><td align=\"right\">");
                        buf.append(countByType_[i]);
                        buf.append("</td><td align=\"right\">");
                        buf.append(PokerConstants.formatPercent(100.0 * countByType_[i] / totalCount_));
                        buf.append("%</td></tr>");
                        /*
                        buf.append("<tr><td colspan=\"3\">");
                        buf.append(strongerHandsByType_[i].toHTML());
                        buf.append("</td></tr>");
                        */
                    }
                }
            }

            if (ladder_[SAME_TYPE_HIGHER_RANK].size() > 0)
            {
                buf.append("<tr><td>Higher ");
                buf.append(HandInfo.getHandTypeDesc(handType_));
                buf.append("</td><td align=\"right\">");
                buf.append(ladder_[SAME_TYPE_HIGHER_RANK].size());
                buf.append("</td><td align=\"right\">");
                buf.append(PokerConstants.formatPercent(100.0 * ladder_[SAME_TYPE_HIGHER_RANK].size() / totalCount_));
                buf.append("%</td></tr>");
                /*
                buf.append("<tr><td colspan=\"3\">");
                buf.append(ladder_[SAME_TYPE_HIGHER_RANK].toHTML());
                buf.append("</td></tr>");
                */
            }

            if (ladder_[SAME_HAND_HIGHER_KICKER].size() > 0)
            {
                buf.append("<tr><td>Same Hand, Higher Kicker</td><td align=\"right\">");
                buf.append(ladder_[SAME_HAND_HIGHER_KICKER].size());
                buf.append("</td><td align=\"right\">");
                buf.append(PokerConstants.formatPercent(100.0 * ladder_[SAME_HAND_HIGHER_KICKER].size() / totalCount_));
                buf.append("%</td></tr>");
                /*
                buf.append("<tr><td colspan=\"3\">");
                buf.append(ladder_[SAME_HAND_HIGHER_KICKER].toHTML());
                buf.append("</td></tr>");
                */
            }
        }

        if (getHandRank() < totalCount_)
        {
            buf.append("<tr>");
            buf.append("<td><font color=\"white\"><b>Equal or Weaker Hands - " + PokerConstants.formatPercent(lessEqualPercent) + "%</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Count</b></font></td>");
            buf.append("<td align=\"right\"><font color=\"white\"><b>Probability</b></font></td>");
            buf.append("</tr>");

            if (ladder_[SAME_HAND].size() > 0)
            {
                buf.append("<tr><td>Same Hand</td><td align=\"right\">");
                buf.append(ladder_[SAME_HAND].size());
                buf.append("</td><td align=\"right\">");
                buf.append(PokerConstants.formatPercent(100.0 * ladder_[SAME_HAND].size() / totalCount_));
                buf.append("%</td></tr>");
                /*
                buf.append("<tr><td colspan=\"3\">");
                buf.append(ladder_[SAME_HAND].toHTML());
                buf.append("</td></tr>");
                */
            }

            if (ladder_[SAME_HAND_LOWER_KICKER].size() > 0)
            {
                buf.append("<tr><td>Same Hand, Lower Kicker</td><td align=\"right\">");
                buf.append(ladder_[SAME_HAND_LOWER_KICKER].size());
                buf.append("</td><td align=\"right\">");
                buf.append(PokerConstants.formatPercent(100.0 * ladder_[SAME_HAND_LOWER_KICKER].size() / totalCount_));
                buf.append("%</td></tr>");
                /*
                buf.append("<tr><td colspan=\"3\">");
                buf.append(ladder_[SAME_HAND_LOWER_KICKER].toHTML());
                buf.append("</td></tr>");
                */
            }

            if (ladder_[SAME_TYPE_LOWER_RANK].size() > 0)
            {
                buf.append("<tr><td>Lower ");
                buf.append(HandInfo.getHandTypeDesc(handType_));
                buf.append("</td><td align=\"right\">");
                buf.append(ladder_[SAME_TYPE_LOWER_RANK].size());
                buf.append("</td><td align=\"right\">");
                buf.append(PokerConstants.formatPercent(100.0 * ladder_[SAME_TYPE_LOWER_RANK].size() / totalCount_));
                buf.append("%</td></tr>");
                /*
                buf.append("<tr><td colspan=\"3\">");
                buf.append(ladder_[SAME_TYPE_LOWER_RANK].toHTML());
                buf.append("</td></tr>");
                */
            }

            if (ladder_[LOWER_TYPE].size() > 0)
            {
                for (int i = handType_ - 1; i >= HandInfo.HIGH_CARD; --i)
                {
                    if (countByType_[i] > 0)
                    {
                        buf.append("<tr><td>");
                        buf.append(HandInfo.getHandTypeDesc(i));
                        buf.append("</td><td align=\"right\">");
                        buf.append(countByType_[i]);
                        buf.append("</td><td align=\"right\">");
                        buf.append(PokerConstants.formatPercent(100.0 * countByType_[i] / totalCount_));
                        buf.append("%</td></tr>");
                        /*
                        buf.append("<tr><td colspan=\"3\">");
                        buf.append(weakerHandsByType_[i].toHTML());
                        buf.append("</td></tr>");
                        */
                    }
                }
            }
        }

        buf.append("</table>");

        return buf.toString();
    }
}
