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

import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class PokerTableRankTest extends AbstractPokerTest
{
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
        assertEquals(2, table.getRank(b), "tied players share the better rank");
        assertEquals(2, table.getRank(c), "tied players share the better rank");
        assertEquals(4, table.getRank(d), "the rank after a tie skips the shared spot");
    }

    /**
     * The seat array is walked in full rather than up to getSeats(): addPlayer() seats
     * at a random index in 0..SEATS-1 whatever the table's size, so a player can sit in
     * a high seat at a short table.  Ranking to a shorter bound would not see them.
     *
     * The table has to be genuinely short for this to test anything - at the default
     * ten seats the two bounds are the same number and the test passes either way.
     * The profile must be set before the table is built, since getSeats() caches.
     */
    @Test
    public void countsPlayersInEverySeat()
    {
        game_.getProfile().getMap().setInteger(TournamentProfile.PARAM_TABLE_SEATS, 6);
        PokerTable table = table(1);
        assertTrue(table.getSeats() < PokerConstants.SEATS, "expected a short table");

        PokerPlayer low = seat(table, 0, 1, 500);
        PokerPlayer high = seat(table, PokerConstants.SEATS - 1, 2, 1500);

        assertEquals(1, table.getRank(high));
        assertEquals(2, table.getRank(low), "the player past the last seat of a short table has to be counted");
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

        assertEquals(1, ours.getRank(shortStack), "first of two here");
        assertEquals(3, game_.getRank(shortStack), "but third of four overall");
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
        PokerPlayer busted = add(2, 0);

        assertEquals(0, table.getRank(busted));
    }

    /**
     * SimulatorDialog builds a table with no game behind it.  The player is seated, so
     * this fails if the null-game guard goes: without it the answer would come from
     * counting, and a seated player counts as first rather than as not found.
     */
    @Test
    public void returnsZeroWhenTableHasNoGame()
    {
        PokerTable orphan = new PokerTable(null, 0);
        PokerPlayer p = new PokerPlayer(1, "P1", true);
        p.setChipCount(1000);
        orphan.setPlayer(p, 0);

        assertEquals(0, orphan.getRank(p), "no game to read chip counts through, seated or not");
    }

    /**
     * A player with nothing left is still seated, so they rank last rather than 0 -
     * the same 0 an unseated player gets, which is the confusion worth pinning down.
     */
    @Test
    public void brokePlayerWhoIsStillSeatedRanksLastNotZero()
    {
        PokerTable table = table(1);
        seat(table, 0, 1, 1000);
        seat(table, 1, 2, 900);
        PokerPlayer broke = seat(table, 2, 3, 0);

        assertEquals(3, table.getRank(broke));
    }

    /**
     * Table-scoped twin of PokerGameRankTest.allPlayersBrokeRanksFirstRatherThanZero.
     * Both count through RankUtils, so a table where nobody has chips has to answer the
     * same way the tournament-wide version does - first, not the not-seated-here 0.
     */
    @Test
    public void allPlayersAtThisTableBrokeRankFirstRatherThanZero()
    {
        PokerTable table = table(1);
        PokerPlayer a = seat(table, 0, 1, 0);
        PokerPlayer b = seat(table, 1, 2, 0);

        assertEquals(1, table.getRank(a));
        assertEquals(1, table.getRank(b));
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
        PokerPlayer rival = seat(table, 0, 2, 500);
        seat(table, 1, 3, 500);
        seat(table, 2, 4, 500);
        PokerPlayer leader = seat(table, 3, 1, 1000);
        HoldemHand hhand = startHand(table);

        assertTrue(table.isHandInProgress(), "expected a hand in progress");
        int nCommitted = hhand.getTotalBet(leader);
        int nRivalCommitted = hhand.getTotalBet(rival);
        assertTrue(nCommitted - nRivalCommitted > 1, "expected the blind to cost the leader more than the rival paid");

        // Leave the rival one chip behind on settled counts, which puts them ahead on
        // live ones - the leader has more in the pot.  Derived from what the deal
        // actually charged rather than assuming it: the stakes belong to the profile,
        // and a failure here should be about ranking, not about level one being 1/2.
        rival.setChipCount(leader.getChipCount() + nCommitted - nRivalCommitted - 1);
        assertTrue(leader.getChipCount() < rival.getChipCount(), "the leader's live count must have dropped below the rival's");

        assertEquals(1, table.getRank(leader), "settled counts still put the leader first");
        assertEquals(2, table.getRank(rival));
    }

    /**
     * Heads-up, where both players post every hand: the button takes the small blind
     * and the other the big.  Two stacks close enough together can come out of the deal
     * holding the same live count - a live comparison would tie them and rank both
     * first.  Settled counts still tell them apart, by the difference between the posts.
     */
    @Test
    public void usesSettledChipsHeadsUp()
    {
        PokerTable table = table(1);
        PokerPlayer leader = seat(table, 0, 1, 1000);
        PokerPlayer rival = seat(table, 1, 2, 1000);
        HoldemHand hhand = startHand(table);

        assertTrue(table.isHandInProgress(), "expected a hand in progress");
        assertTrue(hhand.getTotalBet(leader) > 0 &&
                                                    hhand.getTotalBet(rival) > 0, "expected both players to post");
        assertTrue(hhand.getTotalBet(leader) > hhand.getTotalBet(rival), "the button takes the small blind, so seat 0 must have posted more");

        // level the live counts, whatever the blinds cost: settled is live plus what is
        // in the pot, so the big blind stays ahead by the difference between the posts
        rival.setChipCount(leader.getChipCount());

        assertEquals(1, table.getRank(leader), "settled counts still put the leader first");
        assertEquals(2, table.getRank(rival), "and the rival second, where a live count would tie them");
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
            assertEquals(game_.getRank(p), table.getRank(p), "table and tournament rank differ for " + p.getName());
        }
    }

    /**
     * Seats do not hold still: a tournament breaks tables and moves players between them
     * all the way down to the final table, and a busted player just vacates a seat.
     * <p/>
     * The rank has to follow that.  It is counted over a view of the seat array rather
     * than a copy of it, which only stays correct while the array itself is the same
     * object - every seating and un-seating writes a slot in place.  Anyone replacing it
     * with a fresh array would break this silently, hence the test.
     */
    @Test
    public void rankFollowsPlayersMovingBetweenTables()
    {
        PokerTable table = table(1);
        PokerTable other = table(2);

        PokerPlayer big = seat(table, 0, 1, 5000);
        PokerPlayer middle = seat(table, 1, 2, 3000);
        PokerPlayer small = seat(table, 2, 3, 1000);

        assertEquals(3, table.getRank(small));

        // the two above them bust out, and their seats are vacated
        table.removePlayer(0);
        table.removePlayer(1);
        assertEquals(1, table.getRank(small), "last one seated is the chip leader of this table");
        assertEquals(0, table.getRank(big), "and a player no longer seated here has no rank here");

        // the table fills back up from a broken one, seated in the gaps just vacated
        PokerPlayer arrival = seat(table, 1, 4, 9000);
        assertEquals(2, table.getRank(small), "the new arrival counts against the ones already here");
        assertEquals(1, table.getRank(arrival));

        // and someone moved away is ranked at the table they moved to
        other.setPlayer(middle, 0);
        assertEquals(1, other.getRank(middle));
        assertEquals(0, table.getRank(middle), "no longer ranked where they came from");
    }
}
