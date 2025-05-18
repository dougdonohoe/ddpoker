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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * The purpose of this test is to provide a rudimentary way to very our hsqldb
 * code is working.
 *
 * To run queries against a test database pause the debugger before the test ends and
 * use run 'tools/bin/hsqldb.sh [jdbc url]' where [jdbc url] is visible in the console
 * output (jdbc:hsqldb:file:/...).
 */
public class PokerDatabaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Unfortunately, there aren't many unit tests of the core poker logic.  In
     * fact this test, written in September 2024, is one of the first.  The lack of
     * testing is evident in that it is hard to create objects/data needed that
     * comprise a hold'em game.  Much of the code assumes it's running within the
     * client.  Alas, I'll slowly chip away at this.  In any case, this is a very
     * basic test to see if hsqldb is working at the most basic level.  It doesn't
     * verify stuff goes in and comes out of the database properly.
     */
    @Test
    public void testBasics() throws IOException {
        Utils.setVersionString("-db-test");
        // init properties like poker client, but headless for test
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // we need a player profile
        File profileFile = tempFolder.newFile("profile.999.dat");
        PlayerProfile profile = new PlayerProfile("poker-database-test");
        profile.setEmail("test@test.com");
        profile.setName("test");
        profile.initFile();
        //profile.save(); // Not necessary to actually save it

        // we need a database, in a temp place
        File tempDir = tempFolder.newFolder("poker-database-test");
        PokerDatabase.init(profile, tempDir);

        // we need a game, tournament, poker player, table and hand
        PokerGame game = new PokerGame(null);
        TournamentProfile tournament = new TournamentProfile("poker-database-test");
        game.setProfile(tournament);
        PokerPlayer player = new PokerPlayer(PokerPlayer.HOST_ID, "test-player", true);
        game.addPlayer(player);
        PokerTable table = new PokerTable(game, 1);
        table.addPlayer(player);
        HoldemHand hand = new HoldemHand(table);
        hand.setAnte(5);

        // store hand and fetch it
        int id = hand.storeHandHistory();
        String[] html = PokerDatabase.getHandAsHTML(id, true, true);
        assertTrue(html != null && html.length > 0);
        assertEquals("<HTML><B>Hand 0 - Table 1</B></HTML>", html[0]);
    }
}