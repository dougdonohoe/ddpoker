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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.games.poker.PokerUtils;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;

import java.util.HashMap;

/**
 * Reusable computation of odds of winning (Effective Hand Strength) with a given pocket hand, board, and cards to come.
 *
 * Computations are based on one-card lookahead (two cards is expensive and not particularly valuable).
 */
public class PocketOdds
{
    private static long fpBoard_ = 0;

    private static HashMap cache_ = new HashMap();

    private PocketMatrixShort ehs_ = new PocketMatrixShort();

    private float ehsAverage_ = 0.0f;

    /**
     * PocketScores is a wrapper on PocketMatrixShort, and stores a win/tie probability for a
     * pocket hand, with a given board.  Instances are returned only by PocketOdds.getInstance()
     * so that they can be cached (constructor is private).  The cache is maintained as long as
     * every call is for the same board cards.  This means that while a hand is in play, at most
     * one instance is cached for each combination of the 49 cards not in the flop, or 1176 1128 1081instances.
     *
     * When a call is made to getInstance() with a different flop, the cache is cleared.
     *
     * @param community The cards currently on the board; cannot be null or empty.
     * @param pocket The pocket cards to compute odds for.
     * @return An instance of PocketOdds for the specified board and pocket cards.
     */
    public static PocketOdds getInstance(Hand community, Hand pocket)
    {
        if (community == null)
        {
            throw new ApplicationError("PocketOdds.getInstance() called with null community hand.");
        }

        if (community.size() < 3)
        {
            throw new ApplicationError("PocketOdds.getInstance() called before the flop.");
        }

        if (community.size() == 5)
        {
            throw new ApplicationError("PocketOdds.getInstance() called after the river.");
        }

        if (pocket == null)
        {
            throw new ApplicationError("PocketOdds.getInstance() called with null pocket hand.");
        }

        if (pocket.size() < 2)
        {
            throw new ApplicationError("PocketOdds.getInstance() called with empty pocket hand.");
        }

        // compute fingerprint for board - change triggers cache flush
        long fpBoard = community.fingerprint();

        if (fpBoard != fpBoard_)
        {
            cache_.clear();
            fpBoard_ = fpBoard;
        }

        Object key = pocket.fingerprint();

        PocketOdds odds = (PocketOdds)cache_.get(key);

        if (odds == null)
        {
            //long before = System.currentTimeMillis();
            odds = new PocketOdds(community, pocket);
            //long after = System.currentTimeMillis();
            //System.out.println("PocketOdds constructed in " + (after-before) + " milliseconds.");
            cache_.put(key, odds);
        }

        return odds;
    }

    /**
     * Private to force use of caching getInstance method.
     */
    private PocketOdds(Hand community, Hand pocket)
    {
        int index = community.size();

        long divisor = PokerUtils.nChooseK(50-index, 2);

        PocketScores scores = null;

        community = new Hand(community);
        community.addCard(Card.BLANK);

        for (int k = 0; k < 52; ++k)
        {
            if (community.containsCard(k) || pocket.containsCard(k))
            {
                continue;
            }

            community.setCard(index, Card.getCard(k));

            scores = PocketScores.getInstance(community);

            for (int i = 1; i < 52; ++i)
            {
                if ((i == k) || community.containsCard(i) || pocket.containsCard(i)) continue;

                for (int j = 0; j < i; ++j)
                {
                    if ((j == k) || community.containsCard(j) || pocket.containsCard(j)) continue;

                    if (scores.getScore(pocket) >= scores.getScore(i, j))
                    {
                        ehs_.set(i, j, (short)(ehs_.get(i,j) + 1));
                    }
                }
            }
        }

        for (int i = 1; i < 52; ++i)
        {
            for (int j = 0; j < i; ++j)
            {
                ehs_.set(i, j, (short)(10000 * ehs_.get(i,j) / (50-index)));

                ehsAverage_ += ehs_.get(i,j) / 10000.0f / divisor;
            }
        }
    }

    /**
     * @return The percentage chance (0.0 - 1.0) of beating or tiying a random opposing hand after next card.
     */
    public float getEffectiveHandStrength()
    {
        return ehsAverage_;
    }

    /**
     * @param hand Pocket cards.
     * @return The percentage chance (0.0 - 1.0) of beating or tiying the specified opposing hand after next card.
     */
    public float getEffectiveHandStrength(Hand hand)
    {
        return ((float)ehs_.get(hand)) / 10000.0f;
    }

    /**
     * @param card1 First pocket card.
     * @param card2 Second pocket card.
     * @return The percentage chance (0.0 - 1.0) of beating or tiying the specified opposing hand after next card.
     */
    public float getEffectiveHandStrength(Card card1, Card card2)
    {
        return ((float)ehs_.get(card1, card2)) / 10000.0f;
    }

    /**
     * @param card1 Index of first pocket card.
     * @param card2 Index of second pocket card.
     * @return The percentage chance (0.0 - 1.0) of beating or tiying the specified opposing hand after next card.
     */
    public float getEffectiveHandStrength(int card1, int card2)
    {
        return ((float)ehs_.get(card1, card2)) / 10000.0f;
    }
}
