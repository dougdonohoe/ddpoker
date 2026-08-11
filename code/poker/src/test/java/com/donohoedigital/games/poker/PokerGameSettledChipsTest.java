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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies PokerGame.getSettledChipCount() reports a player's chips as of the last
 * point they were settled.  getChipCount() is decremented the moment a bet is
 * committed and not credited back until the pot is awarded, so standings computed
 * while a hand is in progress used to sink whoever had chips in the pot - which is
 * everyone at the current table for most of its hand.
 */
public class PokerGameSettledChipsTest
{
    private PokerGame game_;

    @Before
    public void setUp()
    {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        game_ = new PokerGame(null);
        game_.setProfile(new TournamentProfile("test")); // dealing a hand reads it
    }

    private PokerTable table(int nNum)
    {
        PokerTable t = new PokerTable(game_, nNum);
        t.setMinChip(1); // HoldemHand.addToPot() divides by this
        return t;
    }

    /**
     * Seat a player holding the given chips, with that count snapshotted as their
     * chip count at the start of the hand.
     */
    private PokerPlayer seat(PokerTable table, int nSeat, int nId, String sName, int nChips)
    {
        PokerPlayer p = new PokerPlayer(nId, sName, true);
        p.setChipCount(nChips);
        p.newSimulatedHand(); // snapshots nChipsAtStart_
        table.setPlayer(p, nSeat); // seats at the table AND sets the player's table/seat
        game_.addPlayer(p);
        return p;
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

    @Test
    public void reportsStartOfHandChipsWhileHandInProgress()
    {
        PokerTable table = table(1);
        PokerPlayer doug = seat(table, 0, 1, "Doug", 1000);
        seat(table, 1, 99, "Opponent", 1000); // a hand needs two players
        HoldemHand hhand = startHand(table);
        assertTrue("expected a hand in progress", table.isHandInProgress());

        // dealing posts the blinds, which is chips out of the stack and into the pot
        int nCommitted = hhand.getTotalBet(doug);
        assertTrue("expected the deal to commit chips", nCommitted > 0);

        assertEquals("live count is down by what is in the pot",
                     1000 - nCommitted, doug.getChipCount());
        assertEquals("settled count is the stack before it went in",
                     1000, game_.getSettledChipCount(doug));
    }

    /**
     * PokerTable.startNewHand() fires TYPE_NEW_HAND from setHoldemHand() and only then
     * calls deal(), which is what snapshots PokerPlayer.getChipCountAtStart().  Anything
     * recomputing on that event sees a hand in progress with nothing committed yet, and
     * must report the live count - the start-of-hand snapshot is still the *previous*
     * hand's at that instant.
     */
    @Test
    public void reportsLiveChipsAfterNewHandFiresButBeforeTheDeal()
    {
        PokerTable table = table(1);
        PokerPlayer doug = seat(table, 0, 1, "Doug", 1000);

        // won the last hand - live count is now settled at 1400, but the snapshot still
        // holds 1000 until the next deal calls PokerPlayer.newHand()
        doug.setChipCount(1400);
        assertEquals("snapshot is deliberately stale here", 1000, doug.getChipCountAtStart());

        // new hand created and TYPE_NEW_HAND fired; deal() has not run
        table.setHoldemHand(new HoldemHand(table));
        assertTrue(table.isHandInProgress());

        assertEquals("must not report the previous hand's snapshot",
                     1400, game_.getSettledChipCount(doug));
    }

    @Test
    public void reportsLiveChipsBetweenHands()
    {
        PokerTable table = table(1);
        PokerPlayer doug = seat(table, 0, 1, "Doug", 1000);
        assertFalse("expected no hand in progress", table.isHandInProgress());

        // pot awarded - the live count is the settled count once the hand is over
        doug.setChipCount(1400);

        assertEquals(1400, game_.getSettledChipCount(doug));
    }

    @Test
    public void reportsLiveChipsWhenPlayerHasNoTable()
    {
        PokerPlayer doug = new PokerPlayer(1, "Doug", true);
        doug.setChipCount(1000);
        game_.addPlayer(doug);

        assertEquals(1000, game_.getSettledChipCount(doug));
    }

    /**
     * The regression this all exists for: mid-hand, a player with chips committed
     * must not be ranked below a rival at a settled table whom they are actually
     * still ahead of.
     */
    @Test
    public void midHandPlayerIsNotSunkBelowSettledRivals()
    {
        PokerTable mine = table(1);
        PokerTable theirs = table(2);

        PokerPlayer doug = seat(mine, 0, 1, "Doug", 1000);
        seat(mine, 1, 99, "Opponent", 1000); // a hand needs two players
        PokerPlayer rival = seat(theirs, 0, 2, "Rival", 0);

        // my table is mid-hand, theirs is between hands
        HoldemHand hhand = startHand(mine);
        int nCommitted = hhand.getTotalBet(doug);
        assertTrue("expected the deal to commit chips", nCommitted > 1);

        // park the rival between my live count and my settled one
        rival.setChipCount(1000 - nCommitted + 1);

        // live counts have me behind, which is the bug
        assertTrue("live comparison sinks the mid-hand player",
                   doug.getChipCount() < rival.getChipCount());

        // settled counts have me ahead, which is the truth
        assertTrue("settled comparison keeps the mid-hand player ahead",
                   game_.getSettledChipCount(doug) > game_.getSettledChipCount(rival));
    }
}
