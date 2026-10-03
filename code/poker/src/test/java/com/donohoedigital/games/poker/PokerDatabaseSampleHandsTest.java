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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import com.donohoedigital.db.BindArray;
import com.donohoedigital.games.poker.model.TournamentHistory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plays every {@link SampleHands} hand through the game code, stores it, and checks what the
 * database, hand history and statistics make of it.
 */
public class PokerDatabaseSampleHandsTest extends AbstractPokerTest
{
    @TempDir
    File tempFolder;

    private PlayerProfile profile_;
    private List<Integer> handIDs_;

    @BeforeEach
    public void setUpDatabase()
    {
        Utils.setVersionString("-db-test");
        engine();
        profile_ = PokerDatabaseTest.profile("you", 998); // setProfile() names the player after it
        PokerDatabase.init(profile_, tempFolder);
        PlayerProfileOptions.setDefaultProfileForTest(profile_);

        handIDs_ = SampleHands.playAll(game_, profile_);
    }

    @AfterEach
    public void tearDownDatabase()
    {
        PokerDatabase.init(null);
        PlayerProfileOptions.setDefaultProfileForTest(null);
    }

    @Test
    public void testAllStored()
    {
        assertEquals(SampleHands.all().size(), PokerDatabase.getHandCount("1=1", null));
        for (int id : handIDs_)
        {
            assertTrue(id > 0);
            assertTrue(PokerDatabase.getHandAsHTML(id, true, true).length > 0, "hand " + id);
            assertNotNull(PokerDatabase.getHandForExport(id), "hand " + id);
            assertNotNull(PokerDatabase.getHandListHTML(id), "hand " + id);
        }
        assertEquals(1, PokerDatabase.getTournamentHistory(profile_).size());
        assertNotNull(PokerDatabase.getOverallHistory(profile_));
    }

    /**
     * Every pot is fully awarded: what anyone won, someone else lost
     */
    @Test
    public void testChipsConserved() throws SQLException
    {
        assertEquals(0, count("SELECT PLH_HAND_ID FROM PLAYER_HAND GROUP BY PLH_HAND_ID " +
                              "HAVING SUM(PLH_END_CHIPS - PLH_START_CHIPS) <> 0"));
    }

    /**
     * The per-round action bits the statistics are built from
     */
    @Test
    public void testActions() throws SQLException
    {
        String you = "PLH_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_NAME='you')";

        // you fold once in each round - twice pre-flop, counting the small blind when folded to the big blind
        assertEquals(2, count(you, "PLH_PREFLOP_ACTIONS", PokerDatabase.BIT_FOLD));
        assertEquals(1, count(you, "PLH_FLOP_ACTIONS", PokerDatabase.BIT_FOLD));
        assertEquals(1, count(you, "PLH_TURN_ACTIONS", PokerDatabase.BIT_FOLD));
        assertEquals(1, count(you, "PLH_RIVER_ACTIONS", PokerDatabase.BIT_FOLD));

        // and check-raise once on each street
        for (String round : new String[]{"PLH_FLOP_ACTIONS", "PLH_TURN_ACTIONS", "PLH_RIVER_ACTIONS"})
        {
            assertEquals(1, count("SELECT * FROM PLAYER_HAND WHERE " + you + " AND BITAND(" + round + ", " +
                                  PokerDatabase.BIT_CHECK + ") <> 0 AND BITAND(" + round + ", " + PokerDatabase.BIT_RAISE + ") <> 0"),
                         round);
        }

        // re-raise pre-flop: the three-bet, and the short all-in over a raise
        assertEquals(2, count(you, "PLH_PREFLOP_ACTIONS", PokerDatabase.BIT_RERAISE));

        // all-ins: called pre-flop, made on flop, turn and river, uncalled, and short with a side pot
        assertEquals(6, count("SELECT DISTINCT ACT_HAND_ID FROM PLAYER_ACTION WHERE ACT_ALL_IN AND ACT_PLAYER_ID IN " +
                              "(SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_NAME='you')"));

        // a three-way split
        assertEquals(1, count("SELECT PLH_HAND_ID FROM PLAYER_HAND GROUP BY PLH_HAND_ID " +
                              "HAVING COUNT(*) = 3 AND SUM(CASE WHEN BITAND(PLH_RIVER_ACTIONS, " + PokerDatabase.BIT_WIN + ") <> 0 THEN 1 ELSE 0 END) = 3"));
    }

    /**
     * Every stored reason is one "Show Reason" can display (msg.aioutcome.*)
     */
    @Test
    public void testReasons() throws SQLException
    {
        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT ACT_INTENT FROM PLAYER_ACTION WHERE ACT_INTENT IS NOT NULL"))
        {
            int reasons = 0;
            while (rs.next())
            {
                reasons++;
                assertNotNull(PokerDatabase.decodeReason(rs.getString(1)), rs.getString(1));
            }
            assertTrue(reasons > 0);
        }
    }

    /**
     * The statistics screens' queries, with data in every round
     */
    @Test
    public void testStatistics()
    {
        String groupBy = "GET_HAND_CLASS(PLH_CARD_1, PLH_CARD_2)";
        String orderBy = "MAX(GET_HAND_CLASS_RANK(PLH_CARD_1, PLH_CARD_2)) DESC";
        assertTrue(new StatisticsViewer.ByHandModel(null, groupBy, orderBy, null, true).getRowCount() > 0);
        for (int round = HoldemHand.ROUND_PRE_FLOP; round <= HoldemHand.ROUND_RIVER; round++)
        {
            assertTrue(new StatisticsViewer.ByRoundModel(round, null, groupBy, orderBy, null).getRowCount() > 0,
                       HoldemHand.getRoundName(round));
        }
    }

    /**
     * The hand history and statistics screens only show hands of the profile's own player, found
     * by its create date (StatisticsViewer), so the sample hands have to be stored as the game does
     */
    @Test
    public void testProfileFilters()
    {
        Timestamp created = new Timestamp(profile_.getCreateDate());
        BindArray bind = new BindArray();
        bind.addValue(Types.TIMESTAMP, created);
        assertEquals(SampleHands.all().size(), PokerDatabase.getHandCount(
                "HND_ID IN (SELECT PLH_HAND_ID FROM PLAYER_HAND WHERE PLH_PLAYER_ID IN (" +
                "SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_PROFILE_CREATE_DATE=?))", bind));

        String groupBy = "GET_HAND_CLASS(PLH_CARD_1, PLH_CARD_2)";
        String orderBy = "MAX(GET_HAND_CLASS_RANK(PLH_CARD_1, PLH_CARD_2)) DESC";
        assertTrue(new StatisticsViewer.ByHandModel("TPL_PROFILE_CREATE_DATE=?", groupBy, orderBy, bind, true).getRowCount() > 0);
        assertTrue(new StatisticsViewer.ByRoundModel(HoldemHand.ROUND_PRE_FLOP, "TPL_PROFILE_CREATE_DATE=?", groupBy, orderBy, bind).getRowCount() > 0);

        // a real start date, not the epoch
        TournamentHistory hist = PokerDatabase.getTournamentHistory(profile_).getFirst();
        assertTrue(System.currentTimeMillis() - hist.getStartDate().getTime() < 60000, "start " + hist.getStartDate());
    }

    private int count(String you, String column, int bit) throws SQLException
    {
        return count("SELECT * FROM PLAYER_HAND WHERE " + you + " AND BITAND(" + column + ", " + bit + ") <> 0");
    }

    private int count(String sql) throws SQLException
    {
        try (Connection conn = PokerDatabase.getDatabase().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql))
        {
            int rows = 0;
            while (rs.next()) rows++;
            return rows;
        }
    }
}
