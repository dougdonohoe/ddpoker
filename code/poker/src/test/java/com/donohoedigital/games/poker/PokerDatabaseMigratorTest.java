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

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migration of HSQLDB 1.8 databases, built here with the same bundled 1.8 the migrator uses.
 */
public class PokerDatabaseMigratorTest extends AbstractPokerTest
{
    private static final int HANDS = 200;
    private static final int PLAYERS = 3;

    @TempDir
    File saveDir;

    private File dbDir;

    @BeforeEach
    public void setUp() throws IOException
    {
        Utils.setVersionString("-db-test");
        engine(); // part of the database name
        dbDir = new File(saveDir, "db");
        Files.createDirectories(dbDir.toPath());
    }

    @AfterEach
    public void tearDown()
    {
        PokerDatabase.init(null);
    }

    @Test
    public void testMigrate() throws Exception
    {
        PlayerProfile good = PokerDatabaseTest.profile("good", 1);
        PlayerProfile empty = PokerDatabaseTest.profile("empty", 4);
        PlayerProfile bad = PokerDatabaseTest.profile("bad", 2);
        String goodName = PokerDatabase.getActualDatabaseName(good);
        String badName = PokerDatabase.getActualDatabaseName(bad);
        assertNotEquals(goodName, badName);

        String emptyName = PokerDatabase.getActualDatabaseName(empty);
        createOldDatabase(emptyName, 0); // tables, no rows
        Map<String, Long> expected = createOldDatabase(goodName);
        createOldDatabase(badName);
        Files.write(new File(dbDir, badName + ".script").toPath(), new byte[]{1, 2, 3, 4, 5}); // corrupt

        assertEquals("1.8.0", version(new File(dbDir, goodName + ".properties")));
        assertTrue(PokerDatabaseMigrator.needsMigration(dbDir));

        List<String> progress = new ArrayList<>();
        PokerDatabaseMigrator.Result result = PokerDatabaseMigrator.migrate(dbDir, (name, index, count) ->
                progress.add(name + " " + (index + 1) + "/" + count));

        // told about every database, in order, failures included
        List<String> names = new ArrayList<>(List.of(goodName, badName, emptyName));
        Collections.sort(names);
        assertEquals(List.of(names.get(0) + " 1/3", names.get(1) + " 2/3", names.get(2) + " 3/3"), progress);

        // results and file layout
        assertEquals(Set.of(goodName, emptyName), Set.copyOf(result.getMigrated()));
        assertEquals(Set.of(badName), result.getFailed().keySet());
        File backupDir = new File(dbDir, PokerDatabaseMigrator.BACKUP_DIRECTORY);
        assertEquals(backupDir, result.getBackupDir());
        assertEquals("1.8.0", version(new File(backupDir, goodName + ".properties")));
        assertEquals("1.8.0", version(new File(backupDir, badName + ".properties")));
        assertTrue(new File(backupDir, goodName + ".data").exists());
        assertTrue(version(new File(dbDir, goodName + ".properties")).startsWith("2."));
        assertFalse(new File(dbDir, badName + ".properties").exists());
        assertFalse(new File(dbDir, "upgrade-work").exists());
        assertFalse(PokerDatabaseMigrator.needsMigration(dbDir));

        // the 1.8 driver never reaches application code
        assertEquals("org.hsqldb.jdbc.JDBCDriver", DriverManager.getDriver("jdbc:hsqldb:file:x").getClass().getName());

        // the migrated database works: same data, identities continue, functions created
        PokerDatabase.init(good, saveDir);
        try (Connection conn = PokerDatabase.getDatabase().getConnection())
        {
            assertEquals(expected, PokerDatabaseMigrator.getStats(conn));
        }
        assertEquals(HANDS, PokerDatabase.getHandCount("1=1", null));

        PokerTable table = table(1);
        PokerPlayer human = seat(table, 0, PokerConstants.PLAYER_ID_HOST, "test-player", 1000);
        human.setProfile(good);
        seat(table, 1, 1, 1000);
        int handID = startHand(table).storeHandHistory();
        assertEquals(HANDS + 1, handID);
        assertEquals(HANDS + 1, PokerDatabase.getHandCount("1=1", null));

        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT GET_HAND_CLASS(PLH_CARD_1, PLH_CARD_2) FROM PLAYER_HAND WHERE PLH_HAND_ID=1 AND PLH_PLAYER_ID=1"))
        {
            assertTrue(rs.next());
            assertEquals("AKs", rs.getString(1));
        }

        PokerDatabase.init(empty, saveDir);
        assertEquals(0, PokerDatabase.getHandCount("1=1", null));

        // the failed one starts empty
        PokerDatabase.init(bad, saveDir);
        assertEquals(0, PokerDatabase.getHandCount("1=1", null));
    }

    @Test
    public void testNothingToMigrate() throws Exception
    {
        assertFalse(PokerDatabaseMigrator.needsMigration(dbDir));
        assertFalse(PokerDatabaseMigrator.needsMigration(new File(saveDir, "missing")));

        // a current database isn't migrated
        PlayerProfile profile = PokerDatabaseTest.profile("current", 3);
        PokerDatabase.init(profile, saveDir);
        PokerDatabase.init(null);
        assertFalse(PokerDatabaseMigrator.needsMigration(dbDir));
    }

    @Test
    public void testInUse() throws Exception
    {
        File lock = new File(dbDir, "poker-1-123.lck");
        Files.write(lock.toPath(), new byte[16]);
        assertTrue(PokerDatabaseMigrator.isInUse(dbDir));

        assertTrue(lock.setLastModified(System.currentTimeMillis() - 60000));
        assertFalse(PokerDatabaseMigrator.isInUse(dbDir));
    }

    /**
     * Build a database the way the 1.8 client did: cached tables, compressed script.
     * Returns its stats, for comparison after migration.
     */
    private Map<String, Long> createOldDatabase(String name) throws Exception
    {
        return createOldDatabase(name, HANDS);
    }

    private Map<String, Long> createOldDatabase(String name, int hands) throws Exception
    {
        try (PokerDatabaseMigrator.Hsqldb18 hsqldb18 = new PokerDatabaseMigrator.Hsqldb18();
             Connection conn = hsqldb18.connect(new File(dbDir, name), false))
        {
            Statement stmt = conn.createStatement();
            stmt.execute("SET SCRIPTFORMAT COMPRESSED");
            stmt.execute("SET WRITE_DELAY false");
            stmt.execute("SET PROPERTY \"sql.enforce_strict_size\" true");
            PokerDatabase.createTables(conn);

            if (hands == 0)
            {
                Map<String, Long> stats = PokerDatabaseMigrator.getStats(conn);
                stmt.execute("SHUTDOWN");
                return stats;
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.executeUpdate("INSERT INTO TOURNAMENT (TRN_TYPE, TRN_NAME, TRN_TOTAL_PLAYERS, TRN_START_DATE) " +
                               "VALUES ('PRACTICE', 'old', " + PLAYERS + ", '" + now + "')");
            stmt.executeUpdate("INSERT INTO TOURNAMENT_FINISH (TRF_PROFILE_CREATE_DATE, TRF_TOURNAMENT_ID, TRF_END_DATE, " +
                               "TRF_FINISH_PLACE, TRF_PRIZE, TRF_BUY_IN, TRF_TOTAL_REBUY, TRF_TOTAL_ADD_ON, TRF_PLAYERS_REMAINING) " +
                               "VALUES ('" + now + "', 1, '" + now + "', 2, 100, 10, 0, 0, 1)");
            for (int p = 1; p <= PLAYERS; p++)
            {
                stmt.executeUpdate("INSERT INTO TOURNAMENT_PLAYER (TPL_TOURNAMENT_ID, TPL_SEQUENCE, TPL_NAME, TPL_PROFILE_CREATE_DATE) " +
                                   "VALUES (1, " + p + ", 'P" + p + "', '" + now + "')");
            }

            PreparedStatement hand = conn.prepareStatement(
                    "INSERT INTO HAND (HND_TABLE, HND_NUMBER, HND_TOURNAMENT_ID, HND_GAME_STYLE, HND_GAME_TYPE, HND_START_DATE, " +
                    "HND_END_DATE, HND_ANTE, HND_SMALL_BLIND, HND_BIG_BLIND, HND_COMMUNITY_CARDS_DEALT, HND_COMMUNITY_CARD_1, " +
                    "HND_COMMUNITY_CARD_2, HND_COMMUNITY_CARD_3) VALUES ('1', ?, 1, 'HOLDEM', 'NOLIMIT', ?, ?, 0, 10, 20, 3, '2c', '7d', 'Js')");
            PreparedStatement playerHand = conn.prepareStatement(
                    "INSERT INTO PLAYER_HAND (PLH_HAND_ID, PLH_PLAYER_ID, PLH_SEAT_NUMBER, PLH_START_CHIPS, PLH_END_CHIPS, " +
                    "PLH_CARD_1, PLH_CARD_2, PLH_PREFLOP_ACTIONS, PLH_FLOP_ACTIONS, PLH_TURN_ACTIONS, PLH_RIVER_ACTIONS, " +
                    "PLH_CARDS_EXPOSED) VALUES (?, ?, ?, 1000, ?, ?, ?, 2, 32, 0, 0, false)");
            PreparedStatement action = conn.prepareStatement(
                    "INSERT INTO PLAYER_ACTION (ACT_HAND_ID, ACT_PLAYER_ID, ACT_SEQUENCE, ACT_ROUND, ACT_TYPE, ACT_AMOUNT, " +
                    "ACT_ALL_IN, ACT_SUB_AMOUNT, ACT_INTENT) VALUES (?, ?, ?, 0, 'CALL', 20, false, 0, 'some reason')");
            for (int h = 1; h <= hands; h++)
            {
                hand.setString(1, String.valueOf(h));
                hand.setTimestamp(2, now);
                hand.setTimestamp(3, now);
                hand.executeUpdate();
                for (int p = 1; p <= PLAYERS; p++)
                {
                    playerHand.setInt(1, h);
                    playerHand.setInt(2, p);
                    playerHand.setInt(3, p);
                    playerHand.setInt(4, 1000 + p);
                    playerHand.setString(5, p == 1 ? "Ah" : "9c");
                    playerHand.setString(6, p == 1 ? "Kh" : "8d");
                    playerHand.executeUpdate();

                    action.setInt(1, h);
                    action.setInt(2, p);
                    action.setInt(3, p);
                    action.executeUpdate();
                }
            }

            Map<String, Long> stats = PokerDatabaseMigrator.getStats(conn);
            stmt.execute("SHUTDOWN");
            return stats;
        }
    }

    private static String version(File properties) throws IOException
    {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(properties))
        {
            props.load(in);
        }
        return props.getProperty("version");
    }
}
