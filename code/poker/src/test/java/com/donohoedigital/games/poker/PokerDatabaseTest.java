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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Tests of the client's hsqldb hand-history database.
 *
 * To run queries against a test database pause the debugger before the test ends and
 * run 'tools/bin/hsqldb.sh [path]' where [path] is the database path visible in the console
 * output (jdbc:hsqldb:file:[path]).
 */
public class PokerDatabaseTest extends AbstractPokerTest
{
    @TempDir
    File tempFolder;

    private PlayerProfile profile_;
    private PokerTable table_;
    private PokerPlayer human_;

    @BeforeEach
    public void setUpDatabase()
    {
        Utils.setVersionString("-db-test");
        engine(); // storing players asks the engine for its key; also part of the database name
        profile_ = profile("poker-database-test", 999);
        PokerDatabase.init(profile_, tempFolder);

        table_ = table(1);
        human_ = seat(table_, 0, PokerConstants.PLAYER_ID_HOST, "test-player", 1000);
        human_.setProfile(profile_);
        PlayerProfileOptions.setDefaultProfileForTest(profile_);
        seat(table_, 1, 1, 1000);
        seat(table_, 2, 2, 1000);
    }

    @AfterEach
    public void tearDownDatabase()
    {
        PokerDatabase.init(null);
        PlayerProfileOptions.setDefaultProfileForTest(null);
    }

    /**
     * A profile with the given file number, which the database name is built from.  Set directly,
     * since initFile() numbers from the profiles on disk.
     */
    static PlayerProfile profile(String name, int fileNum)
    {
        PlayerProfile profile = new PlayerProfile(name)
        {
            {
                file_ = new File("profile." + fileNum + ".dat");
                sFileName_ = file_.getName();
            }
        };
        profile.setEmail("test@test.com");
        return profile;
    }

    private int storeHand()
    {
        HoldemHand hhand = startHand(table_);
        return hhand.storeHandHistory();
    }

    @Test
    public void testBasics()
    {
        HoldemHand hand = new HoldemHand(table_);
        hand.setAnte(5);

        int id = hand.storeHandHistory();
        String[] html = PokerDatabase.getHandAsHTML(id, true, true);
        assertTrue(html != null && html.length > 0);
        assertEquals("<HTML><B>Hand 0 - Table 1</B></HTML>", html[0]);
    }

    @Test
    public void testStoreAndQuery()
    {
        int first = storeHand();
        int second = storeHand();
        int third = storeHand();
        assertTrue(first < second && second < third);

        assertEquals(3, PokerDatabase.getHandCount("1=1", null));
        assertEquals(1, PokerDatabase.getTournamentCount("1=1", null));
        assertEquals(List.of(second, third), PokerDatabase.getHandIDs("1=1 ORDER BY HND_ID", null, 1, 2));
        assertEquals(first, PokerDatabase.getPreviousHandID(game_, second));
        assertEquals(third, PokerDatabase.getNextHandID(game_, second));
        assertTrue(PokerDatabase.isPracticeHand(first));
        assertNotNull(PokerDatabase.getHandForExport(second));
        assertNotNull(PokerDatabase.getHandListHTML(second));
        assertTrue(PokerDatabase.getHandAsHTML(second, true, true).length > 0);
        assertNotNull(PokerDatabase.getOverallHistory(profile_));

        List<TournamentHistory> hist = PokerDatabase.getTournamentHistory(profile_);
        assertEquals(1, hist.size());

        PokerDatabase.deleteTournament(hist.get(0));
        assertEquals(0, PokerDatabase.getHandCount("1=1", null));
        assertTrue(PokerDatabase.getTournamentHistory(profile_).isEmpty());
    }

    @Test
    public void testNameChangeAndDeleteAll()
    {
        storeHand();
        human_.setName("renamed");
        PokerDatabase.playerNameChanged(game_, human_);

        PokerDatabase.deleteAllTournaments(profile_);
        assertEquals(0, PokerDatabase.getHandCount("1=1", null));
        assertEquals(0, PokerDatabase.getTournamentCount("1=1", null));
    }

    /**
     * The statistics queries call PokerDatabaseProcs through SQL functions
     */
    @Test
    public void testStatisticsQueries() throws SQLException
    {
        storeHand();
        storeHand();

        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT PLH_CARD_1, PLH_CARD_2, GET_HAND_CLASS(PLH_CARD_1, PLH_CARD_2), " +
                                              "GET_HAND_CLASS_RANK(PLH_CARD_1, PLH_CARD_2) FROM PLAYER_HAND"))
        {
            int rows = 0;
            while (rs.next())
            {
                rows++;
                assertEquals(PokerDatabaseProcs.getHandClass(rs.getString(1), rs.getString(2)), rs.getString(3));
                assertEquals(PokerDatabaseProcs.getHandClassRank(rs.getString(1), rs.getString(2)), rs.getInt(4));
            }
            assertEquals(6, rows);
        }

        // same group by/order by the viewer uses
        String groupBy = "GET_HAND_CLASS(PLH_CARD_1, PLH_CARD_2)";
        String orderBy = "MAX(GET_HAND_CLASS_RANK(PLH_CARD_1, PLH_CARD_2)) DESC";
        assertTrue(new StatisticsViewer.ByHandModel(null, groupBy, orderBy, null, true).getRowCount() > 0);
        assertTrue(new StatisticsViewer.ByHandModel(null, groupBy, orderBy, null, false).getRowCount() > 0);
        // these hands end at the deal, so give every round an action - a round's rows are only
        // read (and the model's columns checked against the query's) if there are some
        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate("UPDATE PLAYER_HAND SET PLH_PREFLOP_ACTIONS=" + PokerDatabase.BIT_CALL +
                               ", PLH_FLOP_ACTIONS=" + PokerDatabase.BIT_CHECK + ", PLH_TURN_ACTIONS=" + PokerDatabase.BIT_BET +
                               ", PLH_RIVER_ACTIONS=" + PokerDatabase.BIT_FOLD);
        }
        for (int round = HoldemHand.ROUND_PRE_FLOP; round <= HoldemHand.ROUND_RIVER; round++)
        {
            assertTrue(new StatisticsViewer.ByRoundModel(round, null, groupBy, orderBy, null).getRowCount() > 0);
        }
    }

    /**
     * Loading a practice save rewinds hands stored after it - but only in the database it was saved in (TODO #13)
     */
    @Test
    public void testRewindOnlyInSameDatabase()
    {
        storeHand();
        int saved = storeHand();
        assertEquals(PokerDatabase.getCurrentDatabaseName(), game_.getLastHandSavedDatabase());
        storeHand();
        storeHand();

        // as if loaded from a save made in another profile's database
        game_.setLastHandSaved(saved, "poker-1-12345");
        game_.setDeleteHandsAfterSaveDate(true);
        storeHand();
        assertEquals(5, PokerDatabase.getHandCount("1=1", null));
        assertFalse(game_.isDeleteHandsAfterSaveDate());

        // older save without the database name
        game_.setLastHandSaved(saved, null);
        game_.setDeleteHandsAfterSaveDate(true);
        storeHand();
        assertEquals(6, PokerDatabase.getHandCount("1=1", null));

        // saved in this database: hands after the save are replaced
        game_.setLastHandSaved(saved, PokerDatabase.getCurrentDatabaseName());
        game_.setDeleteHandsAfterSaveDate(true);
        int next = storeHand();
        assertEquals(3, PokerDatabase.getHandCount("1=1", null));
        assertEquals(1, PokerDatabase.getHandCount("HND_ID > " + saved, null));
        assertTrue(next > saved);
    }

    /**
     * A database that fails every insert (here, an identity behind the existing IDs) can be reset
     */
    @Test
    public void testResetAfterStoreFails() throws SQLException
    {
        storeHand();
        storeHand();
        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate("ALTER TABLE HAND ALTER COLUMN HND_ID RESTART WITH 1");
        }

        assertStoreFails();
        PokerDatabase.reset();

        assertEquals(0, PokerDatabase.getHandCount("1=1", null));
        storeHand();
        assertEquals(1, PokerDatabase.getHandCount("1=1", null));
    }

    /**
     * Files damaged while the profile is in use: a script cut mid-statement can't be opened at all
     */
    @Test
    public void testResetUnopenableFiles() throws IOException, SQLException
    {
        // mid-way through the first CREATE TABLE
        testResetTruncatedScript(script -> script.indexOf("CREATE CACHED TABLE") + 20);
    }

    /**
     * A script cut between statements opens, but without its trailing GRANTs SA isn't an admin,
     * so the database can't be shut down either - reset() has to close it some other way
     */
    @Test
    public void testResetUnclosableFiles() throws IOException, SQLException
    {
        testResetTruncatedScript(script -> script.indexOf("\nGRANT ") + 1);
    }

    private void testResetTruncatedScript(ToIntFunction<String> length) throws IOException, SQLException
    {
        storeHand();
        String databaseName = PokerDatabase.getCurrentDatabaseName();

        // the script is compressed unless debug settings are on (PokerDatabase.initDatabase())
        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate("SET FILES SCRIPT FORMAT TEXT");
        }
        PokerDatabase.shutdownDatabase();

        File script = new File(new File(tempFolder, "db"), databaseName + ".script");
        String text = Files.readString(script.toPath());
        int cut = length.applyAsInt(text);
        assertTrue(cut > 0 && cut < text.length(), "cut point not found");
        Files.writeString(script.toPath(), text.substring(0, cut));

        assertStoreFails();
        PokerDatabase.reset();

        assertEquals(databaseName, PokerDatabase.getCurrentDatabaseName());
        storeHand();
        assertEquals(1, PokerDatabase.getHandCount("1=1", null));
    }

    /**
     * Deleting a profile removes its databases for every key, not just the current one
     */
    @Test
    public void testDeleteAllKeys() throws IOException
    {
        storeHand();
        File dbDir = new File(tempFolder, "db");
        String current = PokerDatabase.getCurrentDatabaseName();
        assertTrue(new File(dbDir, current + ".script").exists());

        // the profile under other keys, and other profiles whose numbers share a prefix
        for (String name : List.of("poker-999-111", "poker-999-222", "poker-99-111", "poker-9990-111"))
        {
            assertTrue(new File(dbDir, name + ".script").createNewFile());
            assertTrue(new File(dbDir, name + ".properties").createNewFile());
        }

        PokerDatabase.delete(profile_, tempFolder);

        String[] left = dbDir.list();
        assertNotNull(left);
        Arrays.sort(left);
        assertArrayEquals(new String[] {"poker-99-111.properties", "poker-99-111.script",
                                        "poker-9990-111.properties", "poker-9990-111.script"}, left);
    }

    private void assertStoreFails()
    {
        ApplicationError e = assertThrows(ApplicationError.class, this::storeHand);
        assertInstanceOf(SQLException.class, e.getException());
    }
}
