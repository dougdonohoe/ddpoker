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

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the tournament's player list survives being read on one thread while it is
 * changed on another, and that adding a whole field at once behaves like adding one at a
 * time.
 * <p/>
 * The list is changed off the game thread for real: OnlineManager.switchPlayer() moves
 * somebody between player and observer from the EDT, and a join arrives on a message
 * thread.  Meanwhile every ranking path walks it.  While it was a plain ArrayList that was
 * a ConcurrentModificationException waiting for the right moment.
 */
public class PokerGamePlayerListTest extends AbstractPokerTest
{
    /** Big enough that a reader is inside the list for a while, small enough to stay quick. */
    private static final int FIELD = 60;

    /** Wall clock each churn test spends trying to provoke a failure. */
    private static final long RUN_MILLIS = 300;

    private PokerPlayer stable_;
    private PokerPlayer churn_;

    /**
     * Hammer the given reader on this thread while a second thread takes one player out of
     * the tournament and puts them back - which is what switching to observer and back
     * does to the list.  Fails with whatever either thread threw.
     */
    private void underChurn(Runnable reader) throws InterruptedException
    {
        for (int i = 1; i <= FIELD; i++)
        {
            add(i, i * 100);
        }
        stable_ = game_.getPokerPlayerAt(0); // never removed, so always rankable
        churn_ = add(FIELD + 1, 5000);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean stop = new AtomicBoolean();

        Thread mutator = new Thread(() -> {
            try
            {
                while (!stop.get())
                {
                    game_.removePlayer(churn_);
                    game_.addPlayer(churn_);
                }
            }
            catch (Throwable t)
            {
                failure.compareAndSet(null, t);
            }
        }, "player-list-churn");
        mutator.setDaemon(true);
        mutator.start();

        try
        {
            long end = System.currentTimeMillis() + RUN_MILLIS;
            while (System.currentTimeMillis() < end && failure.get() == null)
            {
                reader.run();
            }
        }
        catch (Throwable t)
        {
            failure.compareAndSet(null, t);
        }
        finally
        {
            stop.set(true);
            mutator.join(5000);
        }

        if (failure.get() != null)
        {
            fail("threw while the player list was being changed", failure.get());
        }
    }

    @Test
    public void copyingThePlayerListToleratesAConcurrentSwitch() throws InterruptedException
    {
        underChurn(() -> game_.getPokerPlayersCopy());
    }

    /**
     * The one which matters most: every ranking path walks the list, and the rank of a
     * player who is in it must come back rather than blowing up or reporting "not found".
     */
    @Test
    public void rankingToleratesAConcurrentSwitch() throws InterruptedException
    {
        underChurn(() -> game_.getRank(stable_));
    }

    @Test
    public void sortingByRankToleratesAConcurrentSwitch() throws InterruptedException
    {
        underChurn(() -> game_.getPlayersByRank());
    }

    /**
     * Online, adding or removing a player also rewrites the profile's player list, which
     * walks the whole tournament - so the thread doing the changing is reading it too.
     */
    @Test
    public void onlineChurnRewritesTheProfilePlayerList() throws InterruptedException
    {
        game_.setOnlineGameID("test-online-game");
        underChurn(() -> game_.getPokerPlayersCopy());
    }

    /**
     * setupComputerPlayers() builds the computer field and hands it over in one call,
     * because the list is copy-on-write and adding one at a time copies its backing array
     * per player.  The result has to be indistinguishable from having added them one by
     * one, events included.
     */
    @Test
    public void addPlayersMatchesAddingOneAtATime()
    {
        AtomicInteger events = new AtomicInteger();
        PropertyChangeListener counter = evt -> events.incrementAndGet();
        game_.addPropertyChangeListener(PokerGame.PROP_PLAYERS, counter);

        PokerPlayer first = add(1, 100); // one at a time, for something to follow
        assertEquals(1, events.get());

        List<PokerPlayer> field = new ArrayList<>();
        for (int i = 2; i <= 5; i++)
        {
            PokerPlayer player = new PokerPlayer(i, "P" + i, true);
            player.setChipCount(i * 100);
            field.add(player);
        }
        game_.addPlayers(field);

        assertEquals(5, events.get(), "one event per player added, as addPlayer() fires");
        assertEquals(5, game_.getNumPlayers());

        List<PokerPlayer> expected = new ArrayList<>();
        expected.add(first);
        expected.addAll(field);
        assertEquals(expected, game_.getPokerPlayersCopy(), "added in order, after what was there");

        game_.removePropertyChangeListener(PokerGame.PROP_PLAYERS, counter);
    }
}
