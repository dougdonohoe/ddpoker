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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;

public class HandProbabilityMatrix
{
    float prob_[][] = new float[52][52];
    float score_[][] = new float[52][52];

    Deck deck = new Deck(false);

    public HandProbabilityMatrix()
    {
        init(1.0f);
    }

    public void init(float prob)
    {
        for (int i = 0; i < 52; ++i)
        {
            for (int j = i+1; j < 52; ++j)
            {
                prob_[i][j] = prob;
                score_[i][j] = 0.0f;
            }
        }
    }

    //int position, int potStatus,
    public void adjustWeightsPreFlop
            (HandSelectionScheme scheme, PokerPlayer player, int action, Hand hole)
    {
        Hand hand = new Hand(Card.BLANK, Card.BLANK);

        for (int i = 51; i >= 0; --i)
        {
            hand.setCard(0, Card.getCard(i % 4, i / 4 + 2));

            for (int j = 51; j > i; --j)
            {
                // skip if probability already zero
                if (prob_[i][j] == 0) continue;

                hand.setCard(1, Card.getCard(j % 4, j / 4 + 2));

                if (hole.containsAny(hand))
                {
                    prob_[i][j] = 0;
                }
                else
                {
                    // TODO: set sign based on canonical simulation-based hand strength
                }
            }
        }
    }

    /*
    public void adjustForFlop(HandContext handContext)
    {
        Hand hole = handContext.getPocket();
        Hand community = handContext.getCommunity();

        Hand hand = new Hand(Card.BLANK, Card.BLANK);

        HandInfoFast info = new HandInfoFast();

        int score = info.getScore(hole, community);

        for (int i = 51; i >= 0; --i)
        {
            hand.setCard(0, Card.getCard(i % 4, i / 4 + 2));

            for (int j = 51; j > i; --j)
            {
                hand.setCard(1, Card.getCard(j % 4, j / 4 + 2));

                if (hole.containsAny(hand) || community.containsAny(hand))
                {
                    prob_[i][j] = 0;
                    otherHandScore_[i][j] = 0;
                }
                else
                {
                    otherHandScore_[i][j] = info.getScore(hand, community);
                }
            }
        }
    }
    */

    public float getProbability(Card card1, Card card2)
    {
        int i = (card1.getRank() - 2) * 4 + card1.getSuit();
        int j = (card2.getRank() - 2) * 4 + card2.getSuit();

        if (j > i)
        {
            return prob_[i][j];
        }
        else
        {
            return prob_[j][i];
        }
    }

    /*
    public float getOdds(Card card1, Card card2)
    {
        int i = (card1.getRank() - 2) * 4 + card1.getSuit();
        int j = (card2.getRank() - 2) * 4 + card2.getSuit();

        if (j > i)
        {
            return odds_[i][j];
        }
        else
        {
            return odds_[j][i];
        }
    }
    */
}
