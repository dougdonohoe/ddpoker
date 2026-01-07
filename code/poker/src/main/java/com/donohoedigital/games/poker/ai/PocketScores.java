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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.games.poker.HandInfoFaster;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;

import java.util.HashMap;

/**
 * Reusable computation of hand scores with a given board.
 */
public class PocketScores
{
    private static long fpFlop_ = 0;

    private static HashMap cache_ = new HashMap();

    private PocketMatrixInt score_ = new PocketMatrixInt();

    /**
     * PocketScores is a wrapper on PocketMatrixInt, and stores a raw hand score for each possible
     * pocket hand, with a given board.  Instances are returned only by PocketScores.getInstance()
     * so that they can be cached (constructor is private).  The cache is maintained as long as
     * every call is for the same flop cards.  Even when two-card lookahead is performed, this
     * means that while a hand is in play, at most one instance is cached for the flop, one for
     * each possible turn card, and one for each possible river card, for a total of 98 instances.
     *
     * When a call is made to getInstance() with a different flop, the cache is cleared.
     *
     * @param community The cards currently on the board; cannot be null or empty.
     * @return An instance of PocketRanks for the specified board.
     */
    public static PocketScores getInstance(Hand community)
    {
        if (community == null)
        {
            throw new ApplicationError("PocketScores.getInstance() called with null community hand.");
        }

        if (community.size() < 3)
        {
            throw new ApplicationError("PocketScores.getInstance() called with pre-flop community hand.");
        }

        // compute fingerprint for flop - change triggers cache flush
        long fpFlop = (community.size() == 0) ? 0 :
                      1L << community.getCard(0).getIndex() |
                      1L << community.getCard(1).getIndex() |
                      1L << community.getCard(2).getIndex();

        if (fpFlop != fpFlop_)
        {
            cache_.clear();
            fpFlop_ = fpFlop;
        }

        Object key = community.fingerprint();

        PocketScores scores = (PocketScores)cache_.get(key);

        if (scores == null)
        {
            scores = new PocketScores(community);
            cache_.put(key, scores);
        }

        return scores;
    }

    /**
     * Private to force use of caching getInstance method.
     */
    private PocketScores(Hand community)
    {
        HandInfoFaster info = new HandInfoFaster();

        Card card1;
        Card card2;

        Hand pocket = new Hand(Card.BLANK, Card.BLANK);

        for (int i = 1; i < 52; ++i)
        {
            if (community.containsCard(i)) continue;

            card1 = Card.getCard(i);

            pocket.setCard(0, card1);

            for (int j = 0; j < i; ++j)
            {
                if (community.containsCard(j)) continue;

                card2 = Card.getCard(j);

                pocket.setCard(1, card2);

                score_.set(i, j, info.getScore(pocket, community));
            }
        }
    }

    /**
     * @param hand Pocket cards.
     * @return The score for the specified pocket hand.
     */
    public int getScore(Hand hand)
    {
        return score_.get(hand);
    }

    /**
     * @param card1 First pocket card.
     * @param card2 Second pocket card.
     * @return The score for the specified pocket hand.
     */
    public int getScore(Card card1, Card card2)
    {
        return score_.get(card1, card2);
    }

    /**
     * @param card1 First pocket card.
     * @param card2 Second pocket card.
     * @return The score for the specified pocket hand.
     */
    public int getScore(int card1, int card2)
    {
        return score_.get(card1, card2);
    }
}
