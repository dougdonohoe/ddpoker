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

/**
 * Rank of a player among a group of players, by chips.
 *
 * PokerGame and PokerTable both need this - the same count over a different set of
 * players - so the count lives here and each supplies the players to walk.
 */
public class RankUtils
{
    /**
     * The players a rank is counted over.
     *
     * Deliberately an indexed list rather than a java.util.List: neither caller has
     * one to hand, and copying into one on every call is exactly what counting in a
     * single pass exists to avoid.
     */
    public interface Players
    {
        /**
         * Number of slots to walk.
         */
        int size();

        /**
         * Player in slot n, or null if the slot is empty - a table's seats are a
         * fixed-size array with gaps in it.
         */
        PokerPlayer getPlayerAt(int n);
    }

    /**
     * Return rank of a player among the given players, based on chips.  Players
     * holding equal chips share a rank, so this is one more than the number holding
     * strictly more - the same result the previous sort-based version produced.
     *
     * Returns 0 when the player is not among them.  Whether that is an error is the
     * caller's to decide: it is for a tournament-wide rank, where every player is in
     * the list by definition, and is not for a table-scoped one, which is asked about
     * whoever is under the mouse and who may be seated anywhere.
     *
     * Compares settled chip counts, not live ones - see PokerGame.getSettledChipCount().
     * A rank is read at arbitrary moments: the Rank dashboard item recomputes when
     * another table finishes a hand, which lands in the middle of ours, and PlayerInfo
     * recomputes on every mouse-over.  Live counts sink whoever has chips in a pot.
     */
    public static int getRank(PokerGame game, Players players, PokerPlayer player)
    {
        int nChips = game.getSettledChipCount(player);
        int nRank = 1;
        boolean bFound = false;

        // size() is re-read on every pass on purpose.  PokerGame's list can shrink
        // from another thread (see PokerGame.removePlayer(), called from OnlineManager
        // when a player switches to observer), and a bound captured up front would
        // index off the end.  It does not close the window between reading size() and
        // reading the slot - that race is older than this method and unchanged by it.
        for (int i = 0; i < players.size(); i++)
        {
            PokerPlayer p = players.getPlayerAt(i);
            if (p == null) continue;
            if (p == player)
            {
                bFound = true;
                continue;
            }
            if (game.getSettledChipCount(p) > nChips) nRank++;
        }

        return bFound ? nRank : 0;
    }
}
