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
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;

/**
 * Shared setup for tests that need a tournament with players in it.
 * <p/>
 * Everything here runs headless: no GUI, no GameContext, no filesystem, no database.
 * PokerGame's GameContext constructor accepts null, which is what keeps it that way -
 * the no-arg constructor would reach for GameEngine.getGameEngine().getDefaultContext().
 * <p/>
 * The factory methods carry comments explaining the non-obvious calls.  They were learned
 * the hard way; leave them in place.
 */
public abstract class AbstractPokerTest
{
    protected PokerGame game_;

    /**
     * One per JVM.  GameEngine's constructor sets its engine_ singleton and nothing ever
     * clears it, so building a second one would leave the first unreachable but still
     * "current" for anything that already captured it.
     */
    private static TestPokerMain engine_;

    @BeforeEach
    public void setUpGame()
    {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        game_ = new PokerGame(null);
        game_.setProfile(new TournamentProfile("test")); // dealing a hand reads it
    }

    /**
     * The headless engine, with fake transports in place of real servers.  Constructing it
     * also satisfies PokerMain.getPokerMain() for anything that reaches for the singleton.
     */
    protected static synchronized TestPokerMain engine()
    {
        if (engine_ == null)
        {
            // DDMessage stamps every message with this; it is null until someone sets it,
            // and the join path reads it back with getVersion().isBefore(...)
            if (DDMessage.getDefaultVersion() == null)
            {
                DDMessage.setDefaultVersion(PokerConstants.VERSION);
            }
            engine_ = new TestPokerMain();
        }
        engine_.reset();
        return engine_;
    }

    /**
     * Put a player in the tournament without seating them anywhere - which is also what a
     * busted player looks like, once OtherTables.cleanTable() has removed them.
     */
    protected PokerPlayer add(int nId, int nChips)
    {
        PokerPlayer p = new PokerPlayer(nId, "P" + nId, true);
        p.setChipCount(nChips);
        game_.addPlayer(p);
        return p;
    }

    /**
     * A table in this game.
     */
    protected PokerTable table(int nNum)
    {
        PokerTable t = new PokerTable(game_, nNum);
        t.setMinChip(1); // HoldemHand.addToPot() divides by this
        return t;
    }

    /**
     * Seat a player, snapshotting their chips as the count at the start of the hand.
     */
    protected PokerPlayer seat(PokerTable table, int nSeat, int nId, int nChips)
    {
        return seat(table, nSeat, nId, "P" + nId, nChips);
    }

    /**
     * Seat a named player, snapshotting their chips as the count at the start of the hand.
     */
    protected PokerPlayer seat(PokerTable table, int nSeat, int nId, String sName, int nChips)
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
     * <p/>
     * The button, and with it the blinds, has to land somewhere - which seat ends up
     * posting depends on how many are seated, so callers that care say so for themselves.
     */
    protected HoldemHand startHand(PokerTable table)
    {
        table.setButton(1); // seat 0 posts the big blind, so it commits a useful amount
        HoldemHand hhand = new HoldemHand(table);
        table.setHoldemHand(hhand);
        hhand.deal();
        return hhand;
    }
}
