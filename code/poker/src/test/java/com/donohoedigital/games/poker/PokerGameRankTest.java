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
import com.donohoedigital.games.poker.model.TournamentProfile;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies PokerGame.getRank() counts in a single pass without changing the answer the
 * previous sort-based implementation gave.  Rank is one more than the number of players
 * holding strictly more chips, so players holding equal chips share a rank.
 */
public class PokerGameRankTest
{
    private PokerGame game_;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        game_ = new PokerGame(null);
        game_.setProfile(new TournamentProfile("test")); // dealing a hand reads it
    }

    private PokerPlayer add(int nId, int nChips)
    {
        PokerPlayer p = new PokerPlayer(nId, "P" + nId, true);
        p.setChipCount(nChips);
        game_.addPlayer(p);
        return p;
    }

    private PokerTable table(int nNum)
    {
        PokerTable t = new PokerTable(game_, nNum);
        t.setMinChip(1); // HoldemHand.addToPot() divides by this
        return t;
    }

    /**
     * Put a hand in progress at the table, far enough along that chips can be committed.
     * setPlayerOrder() is the first thing HoldemHand.deal() does, and the pot bookkeeping
     * needs it before any bet is recorded.
     */
    private HoldemHand startHand(PokerTable table)
    {
        table.setButton(1); // seat 0 posts the big blind, so it commits a useful amount
        HoldemHand hhand = new HoldemHand(table);
        table.setHoldemHand(hhand);
        hhand.deal();
        return hhand;
    }

    /**
     * Seat a player, snapshotting their chips as the count at the start of the hand.
     */
    private PokerPlayer seat(PokerTable table, int nSeat, int nId, int nChips)
    {
        PokerPlayer p = new PokerPlayer(nId, "P" + nId, true);
        p.setChipCount(nChips);
        p.newSimulatedHand();
        table.setPlayer(p, nSeat); // seats at the table AND sets the player's table/seat
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
        assertEquals(2, game_.getRank(b), "tied players share the better rank");
        assertEquals(2, game_.getRank(c), "tied players share the better rank");
        assertEquals(4, game_.getRank(d), "the rank after a tie skips the shared spot");
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
            assertEquals(legacyRank(p), game_.getRank(p), "rank differs for " + p.getName() + " with $" + p.getChipCount());
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
        assertEquals(0, legacyRank(a), "legacy version returned 0 here");
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
        PokerTable table = table(1);

        PokerPlayer leader = new PokerPlayer(1, "Leader", true);
        leader.setChipCount(1000);
        leader.newSimulatedHand();
        table.setPlayer(leader, 0);
        game_.addPlayer(leader);

        PokerPlayer allin = new PokerPlayer(2, "AllIn", true);
        allin.setChipCount(500);
        allin.newSimulatedHand();
        table.setPlayer(allin, 1);
        game_.addPlayer(allin);

        PokerPlayer third = add(3, 0);
        third.setPlace(3);
        PokerPlayer fourth = add(4, 0);
        fourth.setPlace(4);

        // hand in progress and AllIn has no chips left in front of them
        startHand(table);
        allin.setChipCount(0);

        List<PokerPlayer> rank = game_.getPlayersByRank();
        assertEquals(leader, rank.get(0), "live players stay ahead of finishers");
        assertEquals(allin, rank.get(1), "an all-in player is still a live player");
        assertEquals(third, rank.get(2), "paid finishers keep their index");
        assertEquals(fourth, rank.get(3), "paid finishers keep their index");
    }

    /**
     * Tables are not in step with each other - online they are independent state
     * machines - so a rank computed while one table is mid-hand must not sink the
     * players sitting at it.  This is the shape of the online bug: the Rank dashboard
     * item recomputes when another table finishes a hand, which lands in the middle of
     * ours, and PlayerInfo recomputes on every mouse-over.
     */
    @Test
    public void comparesSettledChipsAcrossTablesMidHand()
    {
        PokerTable mine = table(1);
        PokerTable theirs = table(2);

        PokerPlayer doug = seat(mine, 0, 1, 1000);
        seat(mine, 1, 98, 1); // a hand needs two players
        PokerPlayer rival = seat(theirs, 0, 2, 0);

        // my table is mid-hand - dealing put my blind in the pot - and theirs is between
        // hands.  Park the rival between my live count and my settled one.
        HoldemHand hhand = startHand(mine);
        int nCommitted = hhand.getTotalBet(doug);
        assertTrue(nCommitted > 1, "expected the deal to commit chips");
        rival.setChipCount(1000 - nCommitted + 1);

        assertTrue(doug.getChipCount() < rival.getChipCount(), "live counts would invert these two");

        assertEquals(1, game_.getRank(doug), "chips in the pot must not cost me a place");
        assertEquals(2, game_.getRank(rival));
    }

    /**
     * The mid-hand player must not gain a place either - only the chips actually
     * committed are restored, not the pot they might win.
     */
    @Test
    public void settledChipsDoNotOverstateTheMidHandPlayer()
    {
        PokerTable mine = table(1);
        PokerTable theirs = table(2);

        PokerPlayer doug = seat(mine, 0, 1, 900);
        seat(mine, 1, 98, 1); // a hand needs two players
        PokerPlayer rival = seat(theirs, 0, 2, 950);

        HoldemHand hhand = startHand(mine);
        assertTrue(hhand.getTotalBet(doug) > 0, "expected the deal to commit chips");

        assertEquals(2, game_.getRank(doug), "still behind on settled chips");
        assertEquals(1, game_.getRank(rival));
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
