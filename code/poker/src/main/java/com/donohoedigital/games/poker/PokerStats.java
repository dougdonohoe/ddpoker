/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
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
 * PokerStats.java
 *
 * Created on February 24, 2004, 12:32 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.Format;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.LoggingConfig;
import com.donohoedigital.games.poker.engine.HandSorted;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author donohoe
 */
public class PokerStats {
    private static Logger logger;

    static boolean DEBUG = false;

    // formatting
    static Format fType = new Format("%-14s");
    static Format fHole = new Format("%-8s");
    static Format fBest = new Format("%-22s");

    // members
    private final PokerTable table_;
    private HoldemHand hhand_;
    private final ArrayList<HandStat> stats_ = new ArrayList<>();
    private final int SEATS = PokerConstants.SEATS;
    private boolean bFold_ = false;

    /**
     * Creates a new instance of PokerStats
     */
    public PokerStats() {
        PokerPlayer player;

        // create game
        TournamentProfile profile = new TournamentProfile("poker-stats");
        PokerGame game = new PokerGame(null);
        game.setProfile(profile);

        // create table and players
        table_ = new PokerTable(game, 0);
        table_.setMinChip(1);
        table_.setSimulation(true);
        for (int i = 0; i < SEATS; i++) {
            player = new PokerPlayer(i, "Player " + i, false);
            table_.setPlayer(player, i);
            game.addPlayer(player);
        }
        table_.setButton(0);
    }

    /**
     * Run nNum loops
     */
    public void run(int nNum, int nIter) {
        boolean bDoResults;

        for (int i = 0; i < nNum; i++) {

            if ((i % 1000) == 0) logger.debug("Loop {}", i);
            if (DEBUG) logger.debug("");
            bDoResults = deal();
            if (DEBUG) logger.debug("HAND {} ---- {} -----------------------", i, hhand_.getCommunity().toString());
            if (bDoResults) results();

            if (i > 0 && (i % nIter) == 0) {
                printResults();

                if (bFold_) {
                    HandStat.lowerNoise();
                }

                if (HandStat.noise_ > 0.0d) {
                    logger.debug("Noise now : {}", HandStat.noise_);
                    fixExpectations();
                }

                bFold_ = true;
            }

            //if (i > 0 && (i % 25000) == 0) printResults();
        }

        printResults();
    }

    /**
     * Print ordered results
     */
    private void printResults() {
        logger.debug("");
        logger.debug("RESULTS:");
        HandStat stat;
        Collections.sort(stats_);
        int nTotal = 0;
        for (int i = 0; i < stats_.size(); i++) {
            stat = getStat(i);
            System.out.println(stat.toString());
            nTotal += stat.nChip;
        }
        logger.debug("TOTAL chips (should be 0): {}", nTotal);
    }

    /**
     *
     */
    public boolean deal() {
        // init chips
        PokerPlayer player;
        for (int i = 0; i < SEATS; i++) {
            player = table_.getPlayerRequired(i);
            player.setChipCount(HandStat.BET);
        }

        // setup hand - no blinds, no ante
        // we just use an equal bet for each player
        hhand_ = new HoldemHand(table_);
        table_.setHoldemHand(hhand_);
        hhand_.setBigBlind(0);
        hhand_.setSmallBlind(0);
        hhand_.setAnte(0);

        // deal hand
        hhand_.deal();
        hhand_.getCurrentPlayerInitIndex();

        // do stats
        HandSorted hand;
        HandStat stat;
        for (int i = 0; i < SEATS; i++) {
            player = table_.getPlayerRequired(i);
            hand = player.getHandSorted();

            // fold non-premium hands based on previous expectations
            if (bFold_) {
                stat = getMatchingStat(hand);
                if (stat != null && !stat.isExpectationPositive()) {
                    player.fold(null, HandAction.FOLD_NORMAL);
                    if (DEBUG) logger.debug("Folded: {}", hand.toStringSuited());
                }
            }

            if (!player.isFolded()) {
                player.betTest(HandStat.BET);
            }
        }

        // if everyone folded or all but one folded,
        // then don't count stats since no showdown
        if (hhand_.getNumWithCards() <= 1) {
            return false;
        }

        // community
        hhand_.advanceRound(); // flop
        hhand_.advanceRound(); // turn
        hhand_.advanceRound(); // river

        // figure winner(s)
        hhand_.preResolve(false);
        hhand_.resolve();

        return true;
    }

    private void results() {
        // process results
        PokerPlayer player;
        int nAmount;
        String sResult;
        HandInfo info;
        for (int i = 0; i < hhand_.getNumPlayers(); i++) {
            player = hhand_.getPlayerAt(i);
            if (player.isFolded()) continue;

            nAmount = hhand_.getWin(player);

            if (DEBUG) {
                info = player.getHandInfo();
                sResult = player.getName() + " " +
                        fHole.form(player.getHand().toStringSuited()) + " " +
                        fType.form(info.getHandTypeDesc()) + " " +
                        fBest.form(info.getBest().toString());
                if (nAmount > 0) {
                    sResult += " WIN " + nAmount;
                }

                if (DEBUG) logger.debug(sResult);
            }

            // subtract the bet
            nAmount -= HandStat.BET;

            // record the win
            recordWin(player.getHandSorted(), nAmount);
        }
    }

    /**
     * Add hand to stats
     */
    private void recordWin(HandSorted hand, int nAmount) {
        HandStat stat = getMatchingStat(hand);

        if (stat == null) {
            stat = new HandStat(hand);
            stats_.add(stat);
        }

        stat.record(nAmount);
    }

    /**
     * Remember all expectations
     */
    private void fixExpectations() {
        // loop through all
        HandStat stat;
        for (int i = 0; i < stats_.size(); i++) {
            stat = getStat(i);
            stat.fixExpectation();
        }
    }

    /**
     * Get stat for given hand
     */
    private HandStat getMatchingStat(HandSorted hand) {
        HandStat stat;

        // look for match
        for (int i = 0; i < stats_.size(); i++) {
            stat = getStat(i);
            if (stat.hand.isEquivalent(hand)) {
                return stat;
            }
        }

        return null;
    }

    /**
     * Get stat at given index
     */
    private HandStat getStat(int i) {
        return stats_.get(i);
    }

    /**
     * Main
     */
    public static void main(String[] args) {
        LoggingConfig loggingConfig = new LoggingConfig("plain", ApplicationType.COMMAND_LINE);
        loggingConfig.init();
        logger = LogManager.getLogger(PokerStats.class);

        // arg must be present
        if (args.length < 2) {
            System.out.println("PokerStats [loops] [loops-before-remember]");
            System.exit(-1);
        }

        // get number of runs
        int nNum = 1;
        try {
            nNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignore) {
        }

        // get number of runs before remembering hand values and using that to fold
        int nIter;
        try {
            nIter = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            nIter = 50000;
        }

        // init config
        new ConfigManager("poker", ApplicationType.CLIENT);

        // run        
        PokerStats stats = new PokerStats();
        stats.run(nNum, nIter);
    }
}
