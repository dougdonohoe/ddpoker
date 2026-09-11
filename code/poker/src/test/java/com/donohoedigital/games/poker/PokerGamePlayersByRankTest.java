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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies PokerGame.getPlayersByRank() sorts a capture of each player's chips and place
 * rather than reading them live inside the comparator.
 * <p/>
 * Chips and place are mutable state owned by the tournament director.  A comparator which
 * reads them live can answer the same question about the same pair of players two
 * different ways, which Collections.sort detects and rejects with "Comparison method
 * violates its general contract!".  TournamentSummaryPanel sorts on the EDT while the
 * director is still running, so this is reachable in a real game.
 * <p/>
 * These tests pin the property which makes that impossible - each player is read once, up
 * front - rather than trying to provoke the exception.  Whether the sort notices a
 * comparator contradicting itself depends on how the data happens to fall: over a randomized
 * field of 64 with one mid-sort chip movement it throws in well under a tenth of runs, and a
 * seed picked because it happens to throw would be pinning the JDK's merge strategy rather
 * than anything about this code.  One read per player is the guarantee; no read can
 * disagree with a read that never happens.
 */
public class PokerGamePlayersByRankTest extends AbstractPokerTest
{
    /**
     * A player who answers honestly the first time and differently every time after, and
     * who counts how often it was asked.
     * <p/>
     * Capturing reads each player once, so it only ever sees the honest answer.  Reading
     * live inside the comparator reads each player many times, and sees the shifting one -
     * which is what a director moving chips mid-sort looks like from inside a sort.
     */
    private static class ShiftingPlayer extends PokerPlayer
    {
        private final int nChips;
        private final int nPlace;
        private final Random scramble;
        private int nChipReads;
        private int nPlaceReads;

        private ShiftingPlayer(int nId, int nChips, int nPlace)
        {
            super(nId, "P" + nId, true);
            this.nChips = nChips;
            this.nPlace = nPlace;
            scramble = new Random(nId); // seeded, so a failure here reproduces
        }

        @Override
        public int getChipCount()
        {
            return nChipReads++ == 0 ? nChips : scramble.nextInt(10000);
        }

        @Override
        public int getPlace()
        {
            return nPlaceReads++ == 0 ? nPlace : scramble.nextInt(10000);
        }
    }

    private ShiftingPlayer add(int nId, int nChips, int nPlace)
    {
        ShiftingPlayer player = new ShiftingPlayer(nId, nChips, nPlace);
        game_.addPlayer(player);
        return player;
    }

    /**
     * Added in a jumbled order so the result cannot be the insertion order by accident.
     */
    @Test
    public void orderIsTakenFromOneReadPerPlayer()
    {
        ShiftingPlayer p400 = add(1, 400, 0);
        ShiftingPlayer p900 = add(2, 900, 0);
        ShiftingPlayer p100 = add(3, 100, 0);
        ShiftingPlayer p700 = add(4, 700, 0);

        assertEquals(List.of(p900, p700, p400, p100), game_.getPlayersByRank());
    }

    /**
     * The zero-chip tiebreak compares place, which is just as mutable - a player is given
     * one the moment they bust - so it has to be captured too.
     */
    @Test
    public void placeIsCapturedAlongWithChips()
    {
        ShiftingPlayer third = add(1, 0, 3);
        ShiftingPlayer first = add(2, 0, 1);
        ShiftingPlayer fourth = add(3, 0, 4);
        ShiftingPlayer second = add(4, 0, 2);

        assertEquals(List.of(first, second, third, fourth), game_.getPlayersByRank(),
                     "best finish at the top");
    }

    /**
     * The guarantee itself, over a field big enough for the sort to merge runs rather than
     * just insert, and with the ties that make it compare the same pair more than once.
     */
    @Test
    public void eachPlayerIsReadExactlyOnce()
    {
        List<ShiftingPlayer> added = new ArrayList<>();
        for (int i = 1; i <= 64; i++)
        {
            added.add(add(i, (i % 7) * 100, i)); // ties on chips, so the tiebreaks are used too
        }

        List<PokerPlayer> rank = game_.getPlayersByRank();

        for (ShiftingPlayer player : added)
        {
            assertEquals(1, player.nChipReads, "chips read more than once for " + player.getName());
            assertEquals(1, player.nPlaceReads, "place read more than once for " + player.getName());
        }

        assertEquals(added.size(), rank.size());
        assertEquals(new HashSet<>(added), new HashSet<>(rank), "every player comes back, exactly once");
    }
}
