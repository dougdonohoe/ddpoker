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
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;

import java.util.HashMap;

/**
 * Reusable computation of relative ranking of hands (Raw Hand Strength) with a given board.
 */
public class PocketRanks
{
    private static long fpFlop_ = 0;

    private static HashMap cache_ = new HashMap();

    private PocketMatrixShort rhs_ = new PocketMatrixShort();

    /**
     * PocketRanks is a wrapper on PocketMatrixShort, and stores a ranking for each possible
     * pocket hand, with a given board.  Instances are returned only by PocketRanks.getInstance()
     * so that they can be cached (constructor is private).  The cache is maintained as long as
     * every call is for the same flop cards.  Even when two-card lookahead is performed, this
     * means that while a hand is in play, at most one instance is cached for the flop, one for
     * each possible turn card, and one for each possible river card, for a total of 98 instances.
     *
     * When a call is made to PocketRanks.getInstance() with a different flop, the cache is cleared.
     *
     * @param community The cards currently on the board; cannot be null or empty.
     * @return An instance of PocketRanks for the specified board.
     */
    public static PocketRanks getInstance(Hand community)
    {
        if (community == null)
        {
            throw new ApplicationError("PocketRanks.getInstance() called with null community hand.");
        }

        if (community.size() < 3)
        {
            throw new ApplicationError("PocketRanks.getInstance() called with pre-flop community hand.");
        }

        // compute fingerprint for flop - change triggers cache flush
        long fpFlop = community.fingerprint(3);

        if (fpFlop != fpFlop_)
        {
            cache_.clear();
            fpFlop_ = fpFlop;
        }

        Object key = community.fingerprint();

        PocketRanks ranks = (PocketRanks)cache_.get(key);

        if (ranks == null)
        {
            // long before = System.currentTimeMillis();
            ranks = new PocketRanks(community);
            // long after = System.currentTimeMillis();
            // System.out.println(
            //         "PocketRanks constructed for " + community +
            //         " in " + Long.toString(after-before) + " milliseconds.");
            cache_.put(key, ranks);
        }

        return ranks;
    }

    /**
     * Private to force use of caching getInstance method.
     */
    private PocketRanks(Hand community)
    {
        PocketScores scores = PocketScores.getInstance(community);

        int score;
        int other;
        int worse;
        int equal;
        int count;

        for (int i = 1; i < 52; ++i)
        {
            if (community.containsCard(i)) continue;

            for (int j = 0; j < i; ++j)
            {
                if (community.containsCard(j)) continue;

                score = scores.getScore(i, j);

                worse = 0;
                equal = 0;
                count = 0;

                for (int k = 1; k < 52; ++k)
                {
                    if ((k == i) || (k == j) || community.containsCard(k)) continue;

                    for (int m = 0; m < k; ++m)
                    {
                        if ((m == i) || (m == j) || community.containsCard(m)) continue;

                        other = scores.getScore(k, m);

                        if (other < score) ++worse;
                        else if (other == score) ++equal;

                        ++count;
                    }
                }

                rhs_.set(i, j, (short)(10000.0f * (worse + equal) / count)); // full-weight ties
                // rhs_.set(i, j, (short)(5000 * (worse*2 + equal) / count)); // half-weight ties
            }
        }
    }

    /**
     * @param hand Pocket cards.
     * @return The percentage (0.0 - 1.0) of opposing hands beat or tied.
     */
    public float getRawHandStrength(Hand hand)
    {
        return ((float)rhs_.get(hand)) / 10000.0f;
    }

    /**
     * @param card1 First pocket card.
     * @param card2 Second pocket card.
     * @return The percentage (0.0 - 1.0) of opposing hands beat or tied.
     */
    public float getRawHandStrength(Card card1, Card card2)
    {
        return ((float)rhs_.get(card1, card2)) / 10000.0f;
    }

    /**
     * @param card1 Index of first pocket card.
     * @param card2 Index of second pocket card.
     * @return The percentage (0.0 - 1.0) of opposing hands beat or tied.
     */
    public float getRawHandStrength(int card1, int card2)
    {
        return ((float)rhs_.get(card1, card2)) / 10000.0f;
    }
}
