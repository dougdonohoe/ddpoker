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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Verifies PokerGame.getRank() counts in a single pass without changing the answer the
 * previous sort-based implementation gave.  Rank is one more than the number of players
 * holding strictly more chips, so players holding equal chips share a rank.
 */
public class PokerGameRankTest
{
    private PokerGame game_;

    @Before
    public void setUp()
    {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        game_ = new PokerGame(null);
    }

    private PokerPlayer add(int nId, int nChips)
    {
        PokerPlayer p = new PokerPlayer(nId, "P" + nId, true);
        p.setChipCount(nChips);
        game_.addPlayer(p);
        return p;
    }

    /**
     * The rank the sort-based implementation produced, kept here so the single-pass
     * version can be held to it.
     */
    private int legacyRank(PokerPlayer player)
    {
        int nLastChips = 0;
        int nRank = 0;
        List<PokerPlayer> rank = game_.getPlayersByRank();
        for (int i = 0; i < rank.size(); i++)
        {
            PokerPlayer p = rank.get(i);
            int nChips = p.getChipCount();
            if (nChips != nLastChips) nRank = (i + 1);
            nLastChips = nChips;
            if (p == player) return nRank;
        }
        throw new IllegalStateException("no rank for " + player);
    }

    @Test
    public void countsPlayersWithStrictlyMoreChips()
    {
        PokerPlayer a = add(1, 1000);
        PokerPlayer b = add(2, 900);
        PokerPlayer c = add(3, 800);

        assertEquals(1, game_.getRank(a));
        assertEquals(2, game_.getRank(b));
        assertEquals(3, game_.getRank(c));
    }

    @Test
    public void tiedPlayersShareARank()
    {
        PokerPlayer a = add(1, 1000);
        PokerPlayer b = add(2, 900);
        PokerPlayer c = add(3, 900);
        PokerPlayer d = add(4, 700);

        assertEquals(1, game_.getRank(a));
        assertEquals("tied players share the better rank", 2, game_.getRank(b));
        assertEquals("tied players share the better rank", 2, game_.getRank(c));
        assertEquals("the rank after a tie skips the shared spot", 4, game_.getRank(d));
    }

    @Test
    public void rankIsIndependentOfPlayerListOrder()
    {
        // added shortest stack first - rank must not depend on insertion order
        PokerPlayer small = add(1, 100);
        PokerPlayer big = add(2, 5000);

        assertEquals(1, game_.getRank(big));
        assertEquals(2, game_.getRank(small));
    }

    /**
     * The equivalence claim, over a randomized field with plenty of ties.
     */
    @Test
    public void matchesLegacySortBasedResult()
    {
        Random random = new Random(20260811L);
        for (int i = 1; i <= 200; i++)
        {
            // small range of stack sizes so ties are common
            add(i, random.nextInt(12) * 100);
        }

        for (int i = 0; i < game_.getNumPlayers(); i++)
        {
            PokerPlayer p = game_.getPokerPlayerAt(i);
            assertEquals("rank differs for " + p.getName() + " with $" + p.getChipCount(),
                         legacyRank(p), game_.getRank(p));
        }
    }

    /**
     * Deliberate difference from the sort-based version, which returned 0 here.  The
     * state does not arise in a live tournament, and 1 is the correct answer for it -
     * 0 also suppressed the rank entirely in the Rank dashboard item.
     */
    @Test
    public void allPlayersBrokeRanksFirstRatherThanZero()
    {
        PokerPlayer a = add(1, 0);
        PokerPlayer b = add(2, 0);

        assertEquals(1, game_.getRank(a));
        assertEquals(1, game_.getRank(b));
        assertEquals("legacy version returned 0 here", 0, legacyRank(a));
    }

    /**
     * TournamentSummaryPanel indexes getPlayersByRank() by payout spot to show "Name paid
     * $X", so the position of an already-paid finisher in that list has to be stable while
     * a hand is in progress.  It is, but only because SortChips breaks ties among zero-chip
     * players by place: a live player who is all-in has zero live chips and no place yet,
     * so place 0 keeps them ahead of every finisher.  Without that tiebreak they would
     * slide into the finished group and shift every paid player down a row.
     */
    @Test
    public void allInPlayerDoesNotDisplaceFinishersInRankOrder()
    {
        PokerTable table = new PokerTable(game_, 1);

        PokerPlayer leader = new PokerPlayer(1, "Leader", true);
        leader.setChipCount(1000);
        leader.newSimulatedHand();
        leader.setTable(table, 0);
        game_.addPlayer(leader);

        PokerPlayer allin = new PokerPlayer(2, "AllIn", true);
        allin.setChipCount(500);
        allin.newSimulatedHand();
        allin.setTable(table, 1);
        game_.addPlayer(allin);

        PokerPlayer third = add(3, 0);
        third.setPlace(3);
        PokerPlayer fourth = add(4, 0);
        fourth.setPlace(4);

        // hand in progress, AllIn shoves their whole stack into the pot
        table.setHoldemHand(new HoldemHand());
        allin.setChipCount(0);

        List<PokerPlayer> rank = game_.getPlayersByRank();
        assertEquals("live players stay ahead of finishers", leader, rank.get(0));
        assertEquals("an all-in player is still a live player", allin, rank.get(1));
        assertEquals("paid finishers keep their index", third, rank.get(2));
        assertEquals("paid finishers keep their index", fourth, rank.get(3));
    }

    @Test
    public void throwsForPlayerNotInTheGame()
    {
        add(1, 1000);
        PokerPlayer stranger = new PokerPlayer(99, "Stranger", true);
        stranger.setChipCount(500);

        try
        {
            game_.getRank(stranger);
            fail("expected ApplicationError for a player not in the game");
        }
        catch (ApplicationError expected)
        {
            // expected
        }
    }
}
