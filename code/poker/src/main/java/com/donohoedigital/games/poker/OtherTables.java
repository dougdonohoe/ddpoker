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
/*
 * OtherTables.java
 *
 * Created on April 20, 2004, 10:08 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.config.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
public class OtherTables
{
    @SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization"})
    private static boolean DEBUG = TournamentDirector.DEBUG_CLEANUP_TABLE;
    
    static Logger logger = LogManager.getLogger(OtherTables.class);

    /**
     * Remove broke players, consolidate tables.
     *
     * @param game the game
     * @param tables the list of tables to possibly break (players are moved to any table in the game)
     */
    public static void consolidateTables(PokerGame game, List<PokerTable> tables)
    {
        // clear player-added list in all tables
        clearAddedList(tables);

        // get number total seats open and max seats per table based on num players
        int nOpenTotal = getNumOpenSeats(game);
        int nMax = getMaxPlayersPerTable(game);

        // seat any waiting players
        if (nOpenTotal > 0)
        {
            List<PokerPlayer> waiting = game.getWaitList();
            if (waiting != null)
            {
                PokerPlayer player;
                for (int i = 0; i < waiting.size() && nOpenTotal > 0; i++)
                {
                    player = waiting.get(i);
                    if (DEBUG) logger.debug("++++++++++ SEATING WAITING PLAYER: " + player.getName() + " currently observing " +
                                                                                    (player.getTable() == null ? "[nothing - ai]" :
                                                                                                    player.getTable().getName()));
                    // TD doClean() adds human's as observer, so we need to undo that
                    // here.
                    if (player.isHuman())
                    {
                        game.removeObserver(player); // this removes from player's table as well
                    }

                    placePlayer(game, null, player, nMax, nOpenTotal);
                    player.setWaiting(false); // set waiting after all moves for TDClean purposes
                    nOpenTotal--;
                }
            }
        }

        // no need to consolidate when we're at one table
        if (game.getNumTables() == 1) return;

        // sort array if more than one table
        if (tables.size() > 1)
        {
            tables = new ArrayList<PokerTable>(tables);
            Collections.sort(tables, SORTFILLEDSEATS);
        }

        // fill open spots by breaking up tables
        // loop from top of list were tables have most
        // open spots (least full)
        PokerTable table;
        int nNum = tables.size();
        for (int i = 0; nOpenTotal > 0 && i < nNum; i++)
        {
            table = tables.get(i);
            nOpenTotal = breakTable(game, table, nOpenTotal, nMax);
        }

        // if we have more than one table left after consolidating, then:
        // if any tables have more than max players,
        // move those extra players so tables are balanced.
        // By definition, if a table is above the calculated
        // max, some other table must be below it.
        // nOpenTotal check a perf shortcut (if no open seats, nothing to move)
        if (game.getNumTables() > 1 && nOpenTotal > 0)
        {
            for (int i = 0; i < nNum; i++)
            {
                table = tables.get(i);
                if (table.isRemoved()) continue;
                int nExtra = table.getNumOccupiedSeats() - nMax;
                if (nExtra > 0)
                {
                    if (DEBUG) logger.debug("+++++++++ EXTRA AT " + table.getName() + " players to move: " + nExtra);
                    // TODO: move from big blind? as TDA rules state (also where to place in new table?)
                    // TODO: for now, we move the least recently moved
                    PokerPlayer[] movethese = table.getPlayersSortedByLastMove();
                    PokerPlayer player;
                    for (int j = 0; j < nExtra; j++)
                    {
                        player = movethese[j];
                        table.removePlayer(player.getSeat());
                        placePlayer(game, table, player, nMax, nOpenTotal);
                        // no need to decrement nOpenTotal since we are just
                        // moving players around (it's usage in placePlayer
                        // is simply for debugging purposes)
                    }
                }
            }

            // after moving players, look for any single players at a table - if
            // they exist, make them remove them and set as waiting.  The  TD
            // will move them to a table as an observer.
            for (int i = 0; i < nNum; i++)
            {
                table = tables.get(i);
                if (table.isRemoved()) continue;

                if (table.getNumOccupiedSeats() == 1)
                {
                    PokerPlayer theone = table.getPlayersSortedByLastMove()[0];
                    theone.setWaiting(true); // set waiting before moves for TDClean purposes
                    table.removePlayer(theone.getSeat());

                    if (DEBUG) logger.debug("++++++++++ TABLE REMOVED (1 player): " + table.getName() + " player on wait list: "+theone.getName());
                    game.removeTable(table);
                }
            }
        }
    }

    /**
     * Return total number of open seats in the tournament.
     */
    private static int getNumOpenSeats(PokerGame game)
    {
        PokerTable table;
        int nOpen = 0;
        int nOpenTotal = 0;

        // loop through all to see number of open spots
        for (int i = game.getNumTables() - 1; i >= 0; i--)
        {
            table = game.getTable(i);
            nOpen = table.getNumOpenSeats();
            ApplicationError.assertTrue(nOpen != table.getSeats(), "Table with no occupants", table);

            // add to open count
            nOpenTotal += nOpen;

            if (DEBUG && (nOpen > 0 || game.getNumTables() <= 20)) logger.debug(table.getName() + " has open: " + nOpen);
        }
        if (DEBUG) logger.debug("TOTAL open spots: " + nOpenTotal);
        return nOpenTotal;
    }

    /**
     * clear added list
     */
    private static void clearAddedList(List<PokerTable> tables)
    {
        PokerTable table;

        // loop through tables
        for (int i = tables.size() - 1; i >= 0; i--)
        {
            table = tables.get(i);
            table.getAddedList().clear(); // clear so list is accurate
        }
    }

    /**
     * Figure out the maximum number of players per table based on players left in tournament
     */
    private static int getMaxPlayersPerTable(PokerGame game)
    {
        int nNumPlayers = game.getNumPlayers() - game.getNumPlayersOut();
        int nNumTables = nNumPlayers / game.getSeats();
        if (nNumPlayers % game.getSeats() > 0) nNumTables++;
        int nMax = nNumPlayers / nNumTables;
        if (nNumPlayers % nNumTables > 0) nMax++;
        return nMax;
    }

    /**
     * Debug version of above to figure out BUG 386
     */
    private static int getMaxPlayersPerTableDebug(PokerGame game)
    {
        logger.debug("MAX total players="+game.getNumPlayers()+", out=" +game.getNumPlayersOut());
        int nNumPlayers = game.getNumPlayers() - game.getNumPlayersOut();
        logger.debug("MAX nNumPlayer="+nNumPlayers);
        int nNumTables = nNumPlayers / game.getSeats();
        logger.debug("MAX nNumTables="+nNumTables);
        if (nNumPlayers % game.getSeats() > 0) nNumTables++;
        logger.debug("MAX nNumTables after mod="+nNumTables);
        int nMax = nNumPlayers / nNumTables;
        logger.debug("MAX nMax="+nMax);
        if (nNumPlayers % nNumTables > 0) nMax++;
        logger.debug("MAX nMax after mod="+nMax);
        return nMax;
    }

    /**
     * Break the table if it is possible.  Return the number of open seats left after breaking the table.
     */
    private static int breakTable(PokerGame game, PokerTable table, int nOpenTotal, int nMax)
    {
        if (table.isRemoved()) return nOpenTotal;

        PokerPlayer player;
        // note: this if is same as if (nOpenTotal >= 10) - a little counterintuitive, but a table is never broke
        // unless their are 10 open spots total
        int nAvail = table.getNumOccupiedSeats();
        if ((nOpenTotal - table.getNumOpenSeats()) >= nAvail)
        {
            // adjust open total when breaking a table since we won't be
            // filling the open spots on this table
            nOpenTotal -= table.getNumOpenSeats();

            if (DEBUG) logger.debug("++++++++++ BREAKING TABLE: " + table.getName() + " spots to move: " + nAvail);
            // break up this table
            for (int j = 0; j < PokerConstants.SEATS; j++)
            {
                player = table.getPlayer(j);
                if (player == null) continue;
                table.removePlayer(j);
                placePlayer(game, table, player, nMax, nOpenTotal);
                nOpenTotal--;
            }

            ApplicationError.assertTrue(table.getNumOccupiedSeats() == 0, "at end of breakTable, but still people left", table);

            if (DEBUG) logger.debug("++++++++++ TABLE REMOVED: " + table.getName());
            game.removeTable(table);
        }
        return nOpenTotal;
    }

    /**
     * place player in an open spot
     */
    private static void placePlayer(PokerGame game, PokerTable from, PokerPlayer player, int nMax, int nOpenTotal)
    {
        PokerTable table;

        // get sorted array of tables (do each time since as players added, things change)
        // this is not super expensive as not many players are moved at any given time
        List<PokerTable> tables = new ArrayList<PokerTable>(game.getTables());
        Collections.sort(tables, SORTFILLEDSEATS);
        
        // otherwise add to table with least spots open
        // (at bottom of sorted list)
        for (int i = tables.size() - 1; i >= 0; i--)
        {
            table = tables.get(i);
            if (table == from) continue;
            if (table.isRemoved()) continue; // don't move to removed table
            
            if (table.getNumOpenSeats() > 0 && table.getNumOccupiedSeats() < nMax)
            {
                if (DEBUG) logger.debug(player.getName() + " moved to " + table.getName() + " from " + (from == null ? "[null]" : from.getName()));
                player.removeHand();
                table.addPlayer(player);
                return;
            }
        }

        // if got here, we have a problem
        logger.error("UNABLE TO PLACE PLAYER " + player.getName() + ", from=" + from);
        logger.error("Max=" + nMax+ ", nOpenTotal=" + nOpenTotal + ", table info:");
        getMaxPlayersPerTableDebug(game);
        for (int i  = tables.size() - 1; i >= 0; i--)
        {
            table = tables.get(i);
            logger.error(table.getName() +
                         ":  numOpen="+table.getNumOpenSeats() + 
                         ", numOcc=" + table.getNumOccupiedSeats() +
                         ", removed=" + table.isRemoved());
            logger.error(table.getName() + "=" + table);
        }
        int nNum = game.getNumPlayers();
        PokerPlayer p;
        logger.debug("Player info: " + nNum + " players:");
        for (int i = 0; i < nNum; i++)
        {
            p = game.getPokerPlayerAt(i);
            table = p.getTable();
            logger.debug("Player " + i + " ("+p.getName()+") chips: "+ p.getChipCount() + " elim: " + p.isEliminated() +
                         " table: " + (table == null ? "null" : table.getName()));
        }
        ApplicationError.assertTrue(false, "Unable to place player (see above for details)");
    }

    /////
    ///// CLEAN routines
    /////

    /**
     * Remove broke players and add to the removed array.  If bRemovePlayers is false,
     * then the players are not actually taken off the table (used for display purposes).
     */
    public static void cleanTable(PokerTable table, List<PokerPlayer> removed, boolean bRemovePlayers)
    {
        // remove ref to old holdem hand
        if (bRemovePlayers) table.setHoldemHand(null);
            
        // remove broke players
        PokerPlayer player;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = table.getPlayer(i);
            if (player != null && player.getChipCount() == 0)
            {
                if (DEBUG) logger.debug("Removed " + player.getName() + " from " + table.getName());
                if (bRemovePlayers)
                {
                    table.removePlayer(i);
                    player.removeHand();
                }
                if (removed != null) removed.add(player);
            }
        }
    }

    /**
     * Record each player's placement in the tournament.
     */
    public static void recordPlayerPlacement(TournamentDirector td, PokerGame game, List<PokerPlayer> removed)
    {
        Collections.sort(removed, SORTCHIPSATSTART);
        boolean bUpdate = false;
        for (PokerPlayer p : removed)
        {
            // if already elimintated, don't eliminate again
            if (p.isEliminated()) continue;

            // notify game player is out so prize/place can be determined
            game.playerOut(p);
            if (!p.isComputer()) bUpdate = true;

            // send chat (online only)
            if (game.isOnlineGame())
            {
                int nPrize = p.getPrize();
                td.sendDirectorChat(PropertyConfig.getMessage(nPrize > 0 ? "msg.chat.finish.money" : "msg.chat.finish.none",
                                                              Utils.encodeHTML(p.getDisplayName(game.isOnlineGame())),
                                                              PropertyConfig.getPlace(p.getPlace()),
                                                              nPrize), null);
            }

            // debug
            if (DEBUG || TournamentDirector.DEBUG_CLEANUP_TABLE)
            {
                logger.debug("   " + p.getName() + "(" + p.getID() + ") out chips at start: " +
                             p.getChipCountAtStart() + ", win=" +
                             p.getPrize() + ", place=" + p.getPlace());
            }
        }

        // if we removed players in a public online game, send an update to server.
        // we do this here (instead of in playerOut() in case multiple players busted
        // (to consolidate into one send).  The final player out is updated when
        // the final results are sent down.
        if (bUpdate && game.isOnlineGame() && game.isPublic())
        {
            OnlineServer manager = OnlineServer.getWanManager();
            manager.updateGameProfile(game);
        }
    }

    /////
    ///// SORT stuff
    /////

    // instances for sorting
    private static SortChipsAtStart SORTCHIPSATSTART = new SortChipsAtStart();
    private static SortFilledSeats SORTFILLEDSEATS = new SortFilledSeats();
    
    // sort players by chips they have at start of hand
    private static class SortChipsAtStart implements Comparator<PokerPlayer>
    {        
        /** 
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerPlayer p1, PokerPlayer p2)
        {    
            return p1.getChipCountAtStart() - p2.getChipCountAtStart();
        }
    }
    
    // sort tables by number of occupied seats
    private static class SortFilledSeats implements Comparator<PokerTable>
    {        
        /** 
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerTable p1, PokerTable p2)
        {    
            int nNum = p1.getNumOccupiedSeats() - p2.getNumOccupiedSeats();
            if (nNum != 0) return nNum;
            return p1.getNumber() - p2.getNumber();
        }
    }
}
