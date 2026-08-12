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
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies PokerTable.getRank() ranks a player among those seated at one table, using
 * the same count as PokerGame.getRank() - both go through RankUtils, so the two can
 * never disagree about ties or about which chip count they compare.
 *
 * The differences from the tournament-wide version are what is worth pinning down: it
 * walks a fixed seat array with gaps in it, it ignores players at other tables, and it
 * returns 0 rather than throwing when the player is not seated here, since it is asked
 * about whoever is under the mouse.
 */
public class PokerTableRankTest
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
     * A player in the tournament but not seated anywhere - which is what a busted
     * player is, once OtherTables.cleanTable() has removed them.
     */
    private PokerPlayer unseated(int nId, int nChips)
    {
        PokerPlayer p = new PokerPlayer(nId, "P" + nId, true);
        p.setChipCount(nChips);
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
    public void countsPlayersAtThisTableWithStrictlyMoreChips()
    {
        PokerTable table = table(1);
        PokerPlayer a = seat(table, 0, 1, 1000);
        PokerPlayer b = seat(table, 1, 2, 900);
        PokerPlayer c = seat(table, 2, 3, 800);

        assertEquals(1, table.getRank(a));
        assertEquals(2, table.getRank(b));
        assertEquals(3, table.getRank(c));
    }

    @Test
    public void tiedPlayersShareARank()
    {
        PokerTable table = table(1);
        PokerPlayer a = seat(table, 0, 1, 1000);
        PokerPlayer b = seat(table, 1, 2, 900);
        PokerPlayer c = seat(table, 2, 3, 900);
        PokerPlayer d = seat(table, 3, 4, 700);

        assertEquals(1, table.getRank(a));
        assertEquals("tied players share the better rank", 2, table.getRank(b));
        assertEquals("tied players share the better rank", 2, table.getRank(c));
        assertEquals("the rank after a tie skips the shared spot", 4, table.getRank(d));
    }

    /**
     * The seat array is walked in full rather than up to getSeats(): addPlayer() seats
     * at a random index in 0..SEATS-1 whatever the table's size, so a player can sit in
     * a high seat at a short table.  Ranking to a shorter bound would not see them.
     */
    @Test
    public void countsPlayersInEverySeat()
    {
        PokerTable table = table(1);
        PokerPlayer low = seat(table, 0, 1, 500);
        PokerPlayer high = seat(table, PokerConstants.SEATS - 1, 2, 1500);

        assertEquals(1, table.getRank(high));
        assertEquals("the player in the last seat has to be counted", 2, table.getRank(low));
    }

    /**
     * The whole point of the table-scoped rank: a short stack here is still first at
     * this table however the rest of the tournament is doing.
     */
    @Test
    public void ignoresPlayersAtOtherTables()
    {
        PokerTable ours = table(1);
        PokerTable theirs = table(2);

        PokerPlayer shortStack = seat(ours, 0, 1, 300);
        seat(ours, 1, 2, 200);
        seat(theirs, 0, 3, 9000);
        seat(theirs, 1, 4, 8000);

        assertEquals("first of two here", 1, ours.getRank(shortStack));
        assertEquals("but third of four overall", 3, game_.getRank(shortStack));
    }

    @Test
    public void returnsZeroForPlayerSeatedAtAnotherTable()
    {
        PokerTable ours = table(1);
        PokerTable theirs = table(2);
        seat(ours, 0, 1, 1000);
        PokerPlayer elsewhere = seat(theirs, 0, 2, 1000);

        assertEquals(0, ours.getRank(elsewhere));
    }

    /**
     * A busted player has no table at all.  The tournament-wide version throws when it
     * cannot find a player; this one has to answer, because it is asked about whoever
     * the mouse is over.
     */
    @Test
    public void returnsZeroForPlayerWithNoTable()
    {
        PokerTable table = table(1);
        seat(table, 0, 1, 1000);
        PokerPlayer busted = unseated(2, 0);

        assertEquals(0, table.getRank(busted));
    }

    /**
     * SimulatorDialog builds a table with no game behind it.
     */
    @Test
    public void returnsZeroWhenTableHasNoGame()
    {
        PokerTable orphan = new PokerTable(null, 0);
        PokerPlayer p = new PokerPlayer(1, "P1", true);
        p.setChipCount(1000);

        assertEquals(0, orphan.getRank(p));
    }

    /**
     * Ranks are read at arbitrary moments - PlayerInfo recomputes on every mouse-over,
     * which lands mid-hand.  Comparing live counts would sink whoever has chips in the
     * pot, so the leader would drop a place the instant they posted a blind.
     */
    @Test
    public void usesSettledChipsWhileAHandIsInProgress()
    {
        // seats 1 and 2 take the button and the small blind, which puts the big blind
        // on the leader in seat 3 and leaves the rival in seat 0 posting nothing
        PokerTable table = table(1);
        PokerPlayer rival = seat(table, 0, 2, 999);
        seat(table, 1, 3, 500);
        seat(table, 2, 4, 500);
        PokerPlayer leader = seat(table, 3, 1, 1000);
        HoldemHand hhand = startHand(table);

        assertTrue("expected a hand in progress", table.isHandInProgress());
        assertTrue("expected the leader to post a blind", hhand.getTotalBet(leader) > 0);
        assertEquals("the rival must not have posted", 0, hhand.getTotalBet(rival));
        assertTrue("the leader's live count must have dropped below the rival's",
                   leader.getChipCount() < rival.getChipCount());

        assertEquals("settled counts still put the leader first", 1, table.getRank(leader));
        assertEquals(2, table.getRank(rival));
    }

    /**
     * Heads-up, where both players post every hand: the button takes the small blind
     * and the other the big.  The blinds differ by one chip, so two stacks a chip apart
     * come out of the deal holding exactly the same live count - a live comparison
     * would tie them and rank both first.  Settled counts still tell them apart.
     */
    @Test
    public void usesSettledChipsHeadsUp()
    {
        PokerTable table = table(1);
        PokerPlayer leader = seat(table, 0, 1, 1000);
        PokerPlayer rival = seat(table, 1, 2, 999);
        HoldemHand hhand = startHand(table);

        assertTrue("expected a hand in progress", table.isHandInProgress());
        assertTrue("expected both players to post", hhand.getTotalBet(leader) > 0 &&
                                                    hhand.getTotalBet(rival) > 0);
        assertEquals("the blinds must leave the live counts level",
                     leader.getChipCount(), rival.getChipCount());

        assertEquals("settled counts still put the leader first", 1, table.getRank(leader));
        assertEquals("and the rival second, where a live count would tie them",
                     2, table.getRank(rival));
    }

    /**
     * At a single table the two scales are the same field, so they must give the same
     * answer for every player - which is what sharing RankUtils buys.
     */
    @Test
    public void agreesWithTournamentRankWhenEveryoneIsAtOneTable()
    {
        PokerTable table = table(1);
        seat(table, 0, 1, 1000);
        seat(table, 1, 2, 900);
        seat(table, 2, 3, 900);
        seat(table, 3, 4, 700);
        seat(table, 4, 5, 0);

        for (int i = 0; i < game_.getNumPlayers(); i++)
        {
            PokerPlayer p = game_.getPokerPlayerAt(i);
            assertEquals("table and tournament rank differ for " + p.getName(),
                         game_.getRank(p), table.getRank(p));
        }
    }
}
