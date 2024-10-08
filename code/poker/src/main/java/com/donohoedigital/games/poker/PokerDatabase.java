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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.db.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.impexp.*;
import com.donohoedigital.games.poker.model.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.math.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Represents the client database(s) for storing stats, etc.
 */
public class PokerDatabase
{
    static Logger logger = LogManager.getLogger(PokerDatabase.class);

    public static final int INTENT_LENGTH = 512;

    public static final byte BIT_CHECK = 1;
    public static final byte BIT_CALL = 2;
    public static final byte BIT_BET = 4;
    public static final byte BIT_RAISE = 8;
    public static final byte BIT_RERAISE = 16;
    public static final byte BIT_FOLD = 32;
    public static final byte BIT_WIN = 64;

    private static final String CLIENT_DATABASE_NAME = "poker";

    private static final String DATABASE_DIRECTORY = "db";
    private static final String DATABASE_DRIVER_CLASS = "org.hsqldb.jdbcDriver";
    private static final String DATABASE_DRIVER_URL_PREFIX = "jdbc:hsqldb:file:";
    private static final String DATABASE_USERNAME = "sa";
    private static final String DATABASE_PASSWORD = "";

    private static PlayerProfile profile_ = null;

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                setName("PokerDatabase-Shutdown");
                if (DatabaseManager.isInitialized())
                {
                    logger.debug("Shutting down...");
                    shutdownDatabase();
                }
            }
        });
    }

    /**
     * Perform initialization
     */
    public static void init(PlayerProfile profile)
    {
        init(profile, null);
    }

    /**
     * Perform initialization (testing version, pass in saveDir)
     */
    static void init(PlayerProfile profile, File saveDir)
    {
        if ((profile_ != null) && (!profile_.equals(profile)))
        {
            shutdownDatabase();
        }

        if (profile == null)
        {
            profile_ = null;
            return;
        }

        if ((profile_ != null) && (profile_.equals(profile)))
        {
            return;
        }

        //logger.debug("init database");

        // Initialize the logical database object(s).
        initDatabase(profile, saveDir);

        // Create the database, schema, etc.
        Database database = getDatabase();
        Connection conn = database.getConnection();
        try
        {
            initSchema(conn);
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException ignore)
            {
            }
        }

        profile_ = profile;
    }

    private static void initSchema(Connection conn) throws SQLException
    {
        Statement stmt = conn.createStatement();

        if (!conn.getMetaData().getTables(null, null, "TOURNAMENT", null).next())
        {
            stmt.executeUpdate(
                    "CREATE CACHED TABLE TOURNAMENT (\n" +
                    "TRN_ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,\n" +
                    "TRN_TYPE VARCHAR(8) NOT NULL,\n" +
                    "TRN_NAME VARCHAR(128) NOT NULL,\n" +
                    "TRN_TOTAL_PLAYERS INTEGER NOT NULL,\n" +
                    "TRN_START_DATE TIMESTAMP NOT NULL\n" +
                    ")");
            stmt.executeUpdate("CREATE INDEX TRN_START_DATE ON TOURNAMENT (TRN_START_DATE)");
        }

        if (!conn.getMetaData().getTables(null, null, "TOURNAMENT_FINISH", null).next())
        {
            stmt.executeUpdate(
                    "CREATE CACHED TABLE TOURNAMENT_FINISH (\n" +
                    "TRF_ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,\n" +
                    "TRF_PROFILE_CREATE_DATE TIMESTAMP NOT NULL,\n" +
                    "TRF_TOURNAMENT_ID INTEGER NOT NULL,\n" +
                    "TRF_END_DATE TIMESTAMP NOT NULL,\n" +
                    "TRF_FINISH_PLACE INTEGER NOT NULL,\n" +
                    "TRF_PRIZE DECIMAL NOT NULL,\n" +
                    "TRF_BUY_IN DECIMAL NOT NULL,\n" +
                    "TRF_TOTAL_REBUY DECIMAL NOT NULL,\n" +
                    "TRF_TOTAL_ADD_ON DECIMAL NOT NULL,\n" +
                    "TRF_PLAYERS_REMAINING INTEGER NOT NULL,\n" +
                    "FOREIGN KEY (TRF_TOURNAMENT_ID) REFERENCES TOURNAMENT (TRN_ID)\n" +
                    ")");
        }

        /*
        if (!conn.getMetaData().getColumns(null, null, "TOURNAMENT_FINISH", "TRF_PLAYERS_REMAINING").next())
        {
            stmt.executeUpdate(
                "ALTER TABLE TOURNAMENT_FINISH ADD COLUMN TRF_PLAYERS_REMAINING INTEGER NOT NULL DEFAULT 1"
            );
        }
        */

        if (!conn.getMetaData().getTables(null, null, "HAND", null).next())
        {
            stmt.executeUpdate(
                    "CREATE CACHED TABLE HAND (\n" +
                    "HND_ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,\n" +
                    "HND_TABLE VARCHAR(32),\n" +
                    "HND_NUMBER VARCHAR(16),\n" +
                    "HND_TOURNAMENT_ID INTEGER,\n" +
                    "HND_GAME_STYLE VARCHAR(16),\n" +
                    "HND_GAME_TYPE VARCHAR(9),\n" +
                    "HND_START_DATE TIMESTAMP,\n" +
                    "HND_END_DATE TIMESTAMP,\n" +
                    "HND_ANTE DECIMAL,\n" +
                    "HND_SMALL_BLIND DECIMAL,\n" +
                    "HND_BIG_BLIND DECIMAL,\n" +
                    "HND_COMMUNITY_CARDS_DEALT TINYINT,\n" +
                    "HND_COMMUNITY_CARD_1 CHAR(2),\n" +
                    "HND_COMMUNITY_CARD_2 CHAR(2),\n" +
                    "HND_COMMUNITY_CARD_3 CHAR(2),\n" +
                    "HND_COMMUNITY_CARD_4 CHAR(2),\n" +
                    "HND_COMMUNITY_CARD_5 CHAR(2),\n" +
                    "FOREIGN KEY (HND_TOURNAMENT_ID) REFERENCES TOURNAMENT (TRN_ID)\n" +
                    ")");
        }

        if (!conn.getMetaData().getTables(null, null, "TOURNAMENT_PLAYER", null).next())
        {
            stmt.executeUpdate(
                    "CREATE CACHED TABLE TOURNAMENT_PLAYER (\n" +
                    "TPL_ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,\n" +
                    "TPL_TOURNAMENT_ID INTEGER,\n" +
                    "TPL_SEQUENCE INTEGER,\n" +
                    "TPL_NAME VARCHAR(128) NOT NULL,\n" +
                    "TPL_PROFILE_CREATE_DATE TIMESTAMP,\n" +
                    "FOREIGN KEY (TPL_TOURNAMENT_ID) REFERENCES TOURNAMENT (TRN_ID)\n" +
                    ")");
        }

        if (!conn.getMetaData().getTables(null, null, "PLAYER_HAND", null).next())
        {
            stmt.executeUpdate(
                    "CREATE CACHED TABLE PLAYER_HAND (\n" +
                    "PLH_HAND_ID INTEGER NOT NULL,\n" +
                    "PLH_PLAYER_ID INTEGER NOT NULL,\n" +
                    "PLH_SEAT_NUMBER INTEGER NOT NULL,\n" +
                    "PLH_START_CHIPS DECIMAL,\n" +
                    "PLH_END_CHIPS DECIMAL,\n" +
                    "PLH_CARD_1 CHAR(2),\n" +
                    "PLH_CARD_2 CHAR(2),\n" +
                    "PLH_CARD_3 CHAR(2),\n" +
                    "PLH_CARD_4 CHAR(2),\n" +
                    "PLH_PREFLOP_ACTIONS TINYINT,\n" +
                    "PLH_FLOP_ACTIONS TINYINT,\n" +
                    "PLH_TURN_ACTIONS TINYINT,\n" +
                    "PLH_RIVER_ACTIONS TINYINT,\n" +
                    "PLH_CARDS_EXPOSED BOOLEAN,\n" +
                    "PRIMARY KEY (PLH_HAND_ID, PLH_PLAYER_ID),\n" +
                    "FOREIGN KEY (PLH_HAND_ID) REFERENCES HAND (HND_ID),\n" +
                    "FOREIGN KEY (PLH_PLAYER_ID) REFERENCES TOURNAMENT_PLAYER (TPL_ID)\n" +
                    ")");
        }

        if (!conn.getMetaData().getTables(null, null, "PLAYER_ACTION", null).next())
        {
            stmt.executeUpdate(
                    "CREATE CACHED TABLE PLAYER_ACTION (\n" +
                    "ACT_HAND_ID INTEGER NOT NULL,\n" +
                    "ACT_PLAYER_ID INTEGER NOT NULL,\n" +
                    "ACT_SEQUENCE INTEGER NOT NULL,\n" +
                    "ACT_ROUND INTEGER NOT NULL,\n" +
                    "ACT_TYPE VARCHAR(5) NOT NULL,\n" +
                    "ACT_AMOUNT DECIMAL,\n" +
                    "ACT_ALL_IN BOOLEAN,\n" +
                    "ACT_SUB_AMOUNT DECIMAL,\n" +
                    "ACT_INTENT VARCHAR(" + INTENT_LENGTH + ")," +
                    "PRIMARY KEY (ACT_HAND_ID, ACT_PLAYER_ID, ACT_SEQUENCE),\n" +
                    "FOREIGN KEY (ACT_HAND_ID, ACT_PLAYER_ID) REFERENCES PLAYER_HAND (PLH_HAND_ID, PLH_PLAYER_ID),\n" +
                    "FOREIGN KEY (ACT_HAND_ID) REFERENCES HAND (HND_ID),\n" +
                    "FOREIGN KEY (ACT_PLAYER_ID) REFERENCES TOURNAMENT_PLAYER (TPL_ID)\n" +
                    ")");
        }
    }

    public static void testConnection()
    {
        Database database = getDatabase();
        if (database == null) return;

        Connection conn = database.getConnection();

        try
        {
            conn.close();
        }
        catch (SQLException e)
        {
        }
    }

    public static void delete(PlayerProfile profile)
    {
        if (profile.equals(profile_)) shutdownDatabase();

        File saveDir = GameConfigUtils.getSaveDir();
        File databaseDir = new File(saveDir, DATABASE_DIRECTORY);

        final String databaseName = getActualDatabaseName(profile);

        File[] files = databaseDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.startsWith(databaseName + ".");
            }
        });

        for (int i = 0; i < files.length; ++i)
        {
            files[i].delete();
        }

        if (profile.equals(profile_))
        {
            profile_ = null;
        }
    }

    private static int identity(Connection conn) throws SQLException
    {
        Statement stmt = conn.createStatement();

        try
        {
            ResultSet rs = stmt.executeQuery("CALL IDENTITY()");

            try
            {
                rs.next();

                return rs.getInt(1);
            }
            finally
            {
                rs.close();
            }
        }
        finally
        {
            stmt.close();
        }
    }

    public static int storeHandHistory(HoldemHand hhand)
    {
/*
        for (int i = 0; i < 100; ++i)
        {
            storeHandHistory2(hhand);
        }
    }
    public static void storeHandHistory2(HoldemHand hhand)
    {
*/
        PokerTable table = hhand.getTable();
        PokerGame game = table.getGame();

        List<HandAction> history = hhand.getHistoryCopy();

        int numPlayers = hhand.getNumPlayers();

        byte[][] act = new byte[PokerConstants.SEATS][4];

        boolean bRaised = false;

        int[] lastRound = new int[PokerConstants.SEATS];

        byte communityCardsDealt = 0;

        for (HandAction action : history)
        {
            int seat = action.getPlayer().getSeat();
            int round = action.getRound();

            switch (action.getAction())
            {
                case HandAction.ACTION_OVERBET:
                case HandAction.ACTION_WIN:
                case HandAction.ACTION_LOSE:
                    break;
                default:
                    lastRound[seat] = action.getRound();
                    switch (round)
                    {
                        case HoldemHand.ROUND_FLOP:
                            communityCardsDealt = 3;
                            break;
                        case HoldemHand.ROUND_TURN:
                            communityCardsDealt = 4;
                            break;
                        case HoldemHand.ROUND_RIVER:
                            communityCardsDealt = 5;
                            break;
                    }
                    break;
            }

            if (hhand.isAllInShowdown())
            {
                communityCardsDealt = 5;
            }

            switch (action.getAction())
            {
                case HandAction.ACTION_CHECK:
                case HandAction.ACTION_CHECK_RAISE:
                    // wins are applied to the round in which the player last acted
                    act[seat][round] |= BIT_CHECK;
                    break;
                case HandAction.ACTION_CALL:
                    act[seat][round] |= BIT_CALL;
                    break;
                case HandAction.ACTION_BET:
                    act[seat][round] |= BIT_BET;
                    break;
                case HandAction.ACTION_RAISE:
                    act[seat][round] |= BIT_RAISE;
                    if (bRaised)
                    {
                        act[seat][round] |= BIT_RERAISE;
                    }
                    else
                    {
                        bRaised = true;
                    }
                    break;
                case HandAction.ACTION_FOLD:
                    act[seat][round] |= BIT_FOLD;
                    break;
                case HandAction.ACTION_WIN:
                    // wins are applied to the round in which the player last acted
                    act[seat][lastRound[seat]] |= BIT_WIN;
                    break;
            }
        }

        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt;

            int tournamentID = storeTournament(conn, game);

            if (game.isDeleteHandsAfterSaveDate())
            {
                int lastHand = game.getLastHandSaved();

                if (lastHand != 0)
                {
                    pstmt = conn.prepareStatement(
                            "DELETE FROM PLAYER_ACTION\n" +
                            "WHERE ACT_HAND_ID > ?\n" +
                            "AND EXISTS (SELECT * FROM HAND WHERE HND_ID=ACT_HAND_ID AND HND_TOURNAMENT_ID=?)"
                    );

                    try
                    {
                        pstmt.setInt(1, lastHand);
                        pstmt.setInt(2, tournamentID);
                        pstmt.executeUpdate();
                    }
                    finally
                    {
                        pstmt.close();
                    }

                    pstmt = conn.prepareStatement(
                            "DELETE FROM PLAYER_HAND\n" +
                            "WHERE PLH_HAND_ID > ?\n" +
                            "AND PLH_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_TOURNAMENT_ID=?)"
                    );

                    try
                    {
                        pstmt.setInt(1, lastHand);
                        pstmt.setInt(2, tournamentID);
                        pstmt.executeUpdate();
                    }
                    finally
                    {
                        pstmt.close();
                    }

                    pstmt = conn.prepareStatement(
                            "DELETE FROM HAND\n" +
                            "WHERE HND_ID > ?\n" +
                            "AND HND_TOURNAMENT_ID=?"
                    );

                    try
                    {
                        pstmt.setInt(1, lastHand);
                        pstmt.setInt(2, tournamentID);
                        pstmt.executeUpdate();
                    }
                    finally
                    {
                        pstmt.close();
                    }
                }

                game.setDeleteHandsAfterSaveDate(false);
            }

            // record results of the human
            PokerPlayer human = game.getHumanPlayer();
            storeTournamentFinish(conn, game, human);

            // insert hand info

            pstmt = conn.prepareStatement(
                    "INSERT INTO HAND (\n" +
                    "HND_NUMBER,\n" +
                    "HND_TABLE,\n" +
                    "HND_TOURNAMENT_ID,\n" +
                    "HND_GAME_STYLE,\n" +
                    "HND_GAME_TYPE,\n" +
                    "HND_START_DATE,\n" +
                    "HND_END_DATE,\n" +
                    "HND_ANTE,\n" +
                    "HND_SMALL_BLIND,\n" +
                    "HND_BIG_BLIND,\n" +
                    "HND_COMMUNITY_CARDS_DEALT,\n" +
                    "HND_COMMUNITY_CARD_1,\n" +
                    "HND_COMMUNITY_CARD_2,\n" +
                    "HND_COMMUNITY_CARD_3,\n" +
                    "HND_COMMUNITY_CARD_4,\n" +
                    "HND_COMMUNITY_CARD_5\n" +
                    ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            try
            {
                pstmt.setString(1, Integer.toString(hhand.getTable().getHandNum()));
                pstmt.setString(2, Integer.toString(hhand.getTable().getNumber()));
                pstmt.setInt(3, tournamentID);
                pstmt.setString(4, "HOLDEM");
                pstmt.setString(5, hhand.isNoLimit() ? "NOLIMIT" : hhand.isPotLimit() ? "POTLIMIT" : "LIMIT");
                long startDate = hhand.getStartDate();
                if (startDate > 0)
                {
                    pstmt.setTimestamp(6, new Timestamp(startDate));
                }
                else
                {
                    pstmt.setNull(6, Types.TIMESTAMP);
                }
                long endDate = hhand.getEndDate();
                if (startDate > 0)
                {
                    pstmt.setTimestamp(7, new Timestamp(endDate));
                }
                else
                {
                    pstmt.setNull(7, Types.TIMESTAMP);
                }
                pstmt.setBigDecimal(8, new BigDecimal(hhand.getAnte()));
                pstmt.setBigDecimal(9, new BigDecimal(hhand.getSmallBlind()));
                pstmt.setBigDecimal(10, new BigDecimal(hhand.getBigBlind()));
                pstmt.setByte(11, (byte) communityCardsDealt);

                Hand community = hhand.getCommunity();

                for (int i = 0; i < 5; ++i)
                {
                    if (i < community.size())
                    {
                        pstmt.setString(i + 12, toString(community.getCard(i)));
                    }
                    else
                    {
                        pstmt.setNull(i + 12, Types.VARCHAR);
                    }
                }

                pstmt.executeUpdate();
            }
            finally
            {
                pstmt.close();
            }

            int handID = identity(conn);

            // insert hand info

            pstmt = conn.prepareStatement(
                    "INSERT INTO PLAYER_HAND (\n" +
                    "PLH_HAND_ID,\n" +
                    "PLH_PLAYER_ID,\n" +
                    "PLH_SEAT_NUMBER,\n" +
                    "PLH_START_CHIPS,\n" +
                    "PLH_END_CHIPS,\n" +
                    "PLH_CARD_1,\n" +
                    "PLH_CARD_2,\n" +
                    "PLH_CARD_3,\n" +
                    "PLH_CARD_4,\n" +
                    "PLH_PREFLOP_ACTIONS,\n" +
                    "PLH_FLOP_ACTIONS,\n" +
                    "PLH_TURN_ACTIONS,\n" +
                    "PLH_RIVER_ACTIONS,\n" +
                    "PLH_CARDS_EXPOSED\n" +
                    ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            int[] playerID = new int[PokerConstants.SEATS];

            try
            {
                // since players at table may not have been in hand.  I
                // added the pocket == null check to catch this, so current
                // code may be okay, but semantically, should go by HoldemHand player list
                for (int p = 0; p < numPlayers; ++p)
                {
                    PokerPlayer player = hhand.getPlayerAt(p);
                    Hand pocket = player.getHand();
                    int seat = player.getSeat();

                    playerID[seat] = storePlayer(conn, tournamentID, player);

                    pstmt.setInt(1, handID);
                    pstmt.setInt(2, playerID[seat]);
                    pstmt.setInt(3, seat);
                    pstmt.setBigDecimal(4, new BigDecimal(player.getChipCountAtStart()));
                    pstmt.setBigDecimal(5, new BigDecimal(player.getChipCount()));

                    for (int i = 0; i < 4 && i < pocket.size(); ++i)
                    {
                        pstmt.setString(i + 6, toString(pocket.getCard(i)));
                    }

                    pstmt.setByte(10, act[seat][HoldemHand.ROUND_PRE_FLOP]);
                    pstmt.setByte(11, act[seat][HoldemHand.ROUND_FLOP]);
                    pstmt.setByte(12, act[seat][HoldemHand.ROUND_TURN]);
                    pstmt.setByte(13, act[seat][HoldemHand.ROUND_RIVER]);
                    pstmt.setBoolean(14, player.isCardsExposed() || (player.isHuman() && player.isLocallyControlled()));

                    pstmt.executeUpdate();
                }
            }
            finally
            {
                pstmt.close();
            }

            pstmt = conn.prepareStatement(
                    "INSERT INTO PLAYER_ACTION (\n" +
                    "ACT_HAND_ID,\n" +
                    "ACT_PLAYER_ID,\n" +
                    "ACT_SEQUENCE,\n" +
                    "ACT_ROUND,\n" +
                    "ACT_TYPE,\n" +
                    "ACT_AMOUNT,\n" +
                    "ACT_SUB_AMOUNT,\n" +
                    "ACT_ALL_IN,\n" +
                    "ACT_INTENT\n" +
                    ") VALUES (?,?,?,?,?,?,?,?,?)");

            try
            {
                for (int i = 0; i < history.size(); ++i)
                {
                    HandAction action = (HandAction) history.get(i);

                    pstmt.setInt(1, handID);
                    pstmt.setInt(2, playerID[action.getPlayer().getSeat()]);
                    pstmt.setInt(3, i);
                    pstmt.setInt(4, action.getRound());
                    pstmt.setString(5, action.getActionCode());
                    pstmt.setBigDecimal(6, new BigDecimal(action.getAmount()));
                    pstmt.setBigDecimal(7, new BigDecimal(action.getSubAmount()));
                    pstmt.setBoolean(8, action.isAllIn());
                    String intent = action.getDebug();
                    if ((intent != null) && intent.length() > INTENT_LENGTH)
                    {
                        logger.warn(
                                "Value of action.getDebug() is longer than " + INTENT_LENGTH + " characters.  Truncating.\n" +
                                intent);
                        intent = intent.substring(0, INTENT_LENGTH);
                    }
                    pstmt.setString(9, intent);

                    pstmt.executeUpdate();
                }
            }
            finally
            {
                pstmt.close();
            }

            game.setLastHandSaved(handID);
            return handID;
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }

        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException ignore)
            {
            }
        }
    }

    public static int storeTournament(PokerGame game)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            return storeTournament(conn, game);
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    private static int storeTournament(Connection conn, PokerGame game) throws SQLException
    {
        int tournamentID = -1;

        // insert tournament info

        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT TRN_ID FROM TOURNAMENT\n" +
                "WHERE TRN_START_DATE=?"
        );

        try
        {
            pstmt.setTimestamp(1, new Timestamp(game.getStartDate()));

            ResultSet rs = pstmt.executeQuery();

            try
            {
                if (rs.next())
                {
                    tournamentID = rs.getInt(1);
                }
            }
            finally
            {
                rs.close();
            }
        }
        finally
        {
            pstmt.close();
        }

        if (tournamentID == -1)
        {
            pstmt = conn.prepareStatement(
                    "INSERT INTO TOURNAMENT (\n" +
                    "TRN_TYPE,\n" +
                    "TRN_NAME,\n" +
                    "TRN_TOTAL_PLAYERS,\n" +
                    "TRN_START_DATE\n" +
                    ") VALUES (?,?,?,?)");

            try
            {
                pstmt.setString(1, game.isOnlineGame() ? "ONLINE" : "PRACTICE");
                pstmt.setString(2, game.getProfile().getName());
                pstmt.setInt(3, game.getNumPlayers());
                pstmt.setTimestamp(4, new Timestamp(game.getStartDate()));

                pstmt.executeUpdate();
            }
            finally
            {
                pstmt.close();
            }

            tournamentID = identity(conn);
        }

        return tournamentID;
    }

    public static int storeTournamentFinish(PokerGame game, PokerPlayer player)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            return storeTournamentFinish(conn, game, player);
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    private static int storeTournamentFinish(Connection conn, PokerGame game, PokerPlayer player) throws SQLException
    {
        int tournamentID = storeTournament(conn, game);

        int finishID = -1;

        PreparedStatement pstmt;

        // delete pre-existing row
        pstmt = conn.prepareStatement(
                "DELETE FROM TOURNAMENT_FINISH\n" +
                "WHERE TRF_PROFILE_CREATE_DATE=? AND\n" +
                "TRF_TOURNAMENT_ID IN (SELECT TRN_ID FROM TOURNAMENT WHERE TRN_START_DATE=?)"
        );

        try
        {
            pstmt.setTimestamp(1, new Timestamp(player.getProfile().getCreateDate()));
            pstmt.setTimestamp(2, new Timestamp(game.getStartDate()));

            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        pstmt = conn.prepareStatement(
                "INSERT INTO TOURNAMENT_FINISH (\n" +
                "TRF_PROFILE_CREATE_DATE,\n" +
                "TRF_TOURNAMENT_ID,\n" +
                "TRF_END_DATE,\n" +
                "TRF_FINISH_PLACE,\n" +
                "TRF_PRIZE,\n" +
                "TRF_BUY_IN,\n" +
                "TRF_TOTAL_REBUY,\n" +
                "TRF_TOTAL_ADD_ON,\n" +
                "TRF_PLAYERS_REMAINING\n" +
                ") VALUES (?,?,?,?,?,?,?,?,?)");

        try
        {
            pstmt.setTimestamp(1, new Timestamp(player.getProfile().getCreateDate()));
            pstmt.setInt(2, tournamentID);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(4, player.getPlace());
            pstmt.setBigDecimal(5, new BigDecimal(player.getPrize()));
            pstmt.setBigDecimal(6, new BigDecimal(player.getBuyin()));
            pstmt.setBigDecimal(7, new BigDecimal(player.getRebuy()));
            pstmt.setBigDecimal(8, new BigDecimal(player.getAddon()));
            pstmt.setInt(9, game.getNumPlayers() - game.getNumPlayersOut());

            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        finishID = identity(conn);

        return finishID;
    }

    public static void deleteTournament(TournamentHistory hist)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            deleteTournament(conn, hist);
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    private static void deleteTournament(Connection conn, TournamentHistory hist) throws SQLException
    {
        PreparedStatement pstmt;

        // delete player actions
        pstmt = conn.prepareStatement(
                "DELETE FROM PLAYER_ACTION\n" +
                "WHERE ACT_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_TOURNAMENT_ID=?)"
        );

        try
        {
            pstmt.setLong(1, hist.getGameId());
            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        // delete player hands
        pstmt = conn.prepareStatement(
                "DELETE FROM PLAYER_HAND\n" +
                "WHERE PLH_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_TOURNAMENT_ID=?)"
        );

        try
        {
            pstmt.setLong(1, hist.getGameId());
            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        // delete hands
        pstmt = conn.prepareStatement(
                "DELETE FROM HAND\n" +
                "WHERE HND_TOURNAMENT_ID=?"
        );

        try
        {
            pstmt.setLong(1, hist.getGameId());
            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        // delete players
        pstmt = conn.prepareStatement(
                "DELETE FROM TOURNAMENT_PLAYER\n" +
                "WHERE TPL_TOURNAMENT_ID=?"
        );

        try
        {
            pstmt.setLong(1, hist.getGameId());
            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        // delete tournament finishes
        pstmt = conn.prepareStatement(
                "DELETE FROM TOURNAMENT_FINISH\n" +
                "WHERE TRF_TOURNAMENT_ID=?"
        );

        try
        {
            pstmt.setLong(1, hist.getGameId());

            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }

        // delete tournament
        pstmt = conn.prepareStatement(
                "DELETE FROM TOURNAMENT\n" +
                "WHERE TRN_ID=?"
        );

        try
        {
            pstmt.setLong(1, hist.getGameId());

            pstmt.executeUpdate();
        }
        finally
        {
            pstmt.close();
        }
    }

    public static void deleteAllTournaments(PlayerProfile profile)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            deleteAllTournaments(conn, profile);
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    private static void deleteAllTournaments(Connection conn, PlayerProfile profile) throws SQLException
    {
        Statement stmt = conn.createStatement();

        try
        {
            stmt.executeUpdate("DELETE FROM PLAYER_ACTION");
            stmt.executeUpdate("DELETE FROM PLAYER_HAND");
            stmt.executeUpdate("DELETE FROM HAND");
            stmt.executeUpdate("DELETE FROM TOURNAMENT_PLAYER");
            stmt.executeUpdate("DELETE FROM TOURNAMENT_FINISH");
            stmt.executeUpdate("DELETE FROM TOURNAMENT");
        }
        finally
        {
            stmt.close();
        }
    }

    public static List<TournamentHistory> getTournamentHistory(PlayerProfile profile)
    {
        List<TournamentHistory> hist = new ArrayList<TournamentHistory>();

        if (profile == null)
        {
            return hist;
        }

        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "TRN_ID,\n" +
                    "TRN_TYPE,\n" +
                    "TRN_NAME,\n" +
                    "TRN_TOTAL_PLAYERS,\n" +
                    "TRF_ID,\n" +
                    "TRN_START_DATE,\n" +
                    "TRF_END_DATE,\n" +
                    "TRF_BUY_IN,\n" +
                    "TRF_TOTAL_ADD_ON,\n" +
                    "TRF_TOTAL_REBUY,\n" +
                    "TRF_FINISH_PLACE,\n" +
                    "TRF_PRIZE,\n" +
                    "TRF_PLAYERS_REMAINING\n" +
                    "FROM TOURNAMENT, TOURNAMENT_FINISH\n" +
                    "WHERE TRN_ID=TRF_TOURNAMENT_ID\n" +
                    "AND TRF_PROFILE_CREATE_DATE=? ORDER BY TRF_END_DATE DESC");

            try
            {
                pstmt.setTimestamp(1, new Timestamp(profile.getCreateDate()));

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    while (rs.next())
                    {
                        TournamentHistory h = new TournamentHistory();

                        h.setGameId(rs.getInt(1));
                        h.setTournamentType(rs.getString(2));
                        h.setTournamentName(rs.getString(3));
                        h.setNumPlayers(rs.getInt(4));
                        h.setId(rs.getLong(5));
                        h.setStartDate(rs.getTimestamp(6));
                        h.setEndDate(rs.getTimestamp(7));
                        h.setBuyin(rs.getBigDecimal(8).intValue());
                        h.setAddon(rs.getBigDecimal(9).intValue());
                        h.setRebuy(rs.getBigDecimal(10).intValue());
                        h.setPlace(rs.getInt(11));
                        h.setPrize(rs.getBigDecimal(12).intValue());
                        h.setNumRemaining(rs.getInt(13));

                        hist.add(h);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }

        return hist;
    }

    public static TournamentHistory getOverallHistory(PlayerProfile profile)
    {
        if (profile == null)
        {
            return null;
        }

        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "COUNT(*),\n" +
                    "SUM(TRF_BUY_IN),\n" +
                    "SUM(TRF_TOTAL_ADD_ON),\n" +
                    "SUM(TRF_TOTAL_REBUY),\n" +
                    "SUM(TRF_PRIZE)\n" +
                    "FROM TOURNAMENT, TOURNAMENT_FINISH\n" +
                    "WHERE TRN_ID=TRF_TOURNAMENT_ID\n" +
                    "AND TRF_PROFILE_CREATE_DATE=?");

            try
            {
                pstmt.setTimestamp(1, new Timestamp(profile.getCreateDate()));

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    if (rs.next())
                    {
                        TournamentHistory h = new TournamentHistory();

                        int count = rs.getInt(1);

                        h.setTournamentName(PropertyConfig.getMessage(count == 1 ? "msg.overalltourney.singular" :
                                                                      "msg.overalltourney.plural",
                                                                      count));

                        if (count > 0)
                        {
                            h.setBuyin(rs.getBigDecimal(2).intValue());
                            h.setAddon(rs.getBigDecimal(3).intValue());
                            h.setRebuy(rs.getBigDecimal(4).intValue());
                            h.setPrize(rs.getBigDecimal(5).intValue());
                        }

                        return h;
                    }
                    else
                    {
                        return null;
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    public static void playerNameChanged(PokerGame game, PokerPlayer player)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            int tournamentID = -1;

            // insert tournament info

            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT TRN_ID FROM TOURNAMENT\n" +
                    "WHERE TRN_START_DATE=?"
            );

            try
            {
                pstmt.setTimestamp(1, new Timestamp(game.getStartDate()));

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    if (rs.next())
                    {
                        tournamentID = rs.getInt(1);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            if (tournamentID == -1) return;

            int playerID = -1;

            // insert tournament info

            pstmt = conn.prepareStatement(
                    "SELECT TPL_ID FROM TOURNAMENT_PLAYER\n" +
                    "WHERE TPL_TOURNAMENT_ID=? AND TPL_SEQUENCE=?");

            try
            {
                pstmt.setInt(1, tournamentID);
                pstmt.setInt(2, player.getID());

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    if (rs.next())
                    {
                        playerID = rs.getInt(1);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            if (playerID != -1)
            {
                pstmt = conn.prepareStatement(
                        "UPDATE TOURNAMENT_PLAYER\n" +
                        "SET TPL_NAME=?\n" +
                        "WHERE TPL_ID=?");

                try
                {
                    pstmt.setString(1, player.getName());
                    pstmt.setInt(2, playerID);

                    pstmt.executeUpdate();
                }
                finally
                {
                    pstmt.close();
                }
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    private static int storePlayer(Connection conn, int tournamentID, PokerPlayer player) throws SQLException
    {
        int playerID = -1;

        boolean useProfile = player.isHuman() && player.isLocallyControlled();

        // insert tournament info

        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT TPL_ID FROM TOURNAMENT_PLAYER\n" +
                "WHERE TPL_TOURNAMENT_ID=? AND TPL_SEQUENCE=?");

        try
        {
            pstmt.setInt(1, tournamentID);
            pstmt.setInt(2, player.getID());

            ResultSet rs = pstmt.executeQuery();

            try
            {
                if (rs.next())
                {
                    playerID = rs.getInt(1);
                }
            }
            finally
            {
                rs.close();
            }
        }
        finally
        {
            pstmt.close();
        }

        if (playerID == -1)
        {
            pstmt = conn.prepareStatement(
                    "INSERT INTO TOURNAMENT_PLAYER (\n" +
                    "TPL_TOURNAMENT_ID,\n" +
                    "TPL_SEQUENCE,\n" +
                    "TPL_NAME,\n" +
                    "TPL_PROFILE_CREATE_DATE\n" +
                    ") VALUES (?,?,?,?)");

            try
            {
                pstmt.setInt(1, tournamentID);
                pstmt.setInt(2, player.getID());
                pstmt.setString(3, player.getName());

                if (useProfile)
                {
                    pstmt.setTimestamp(4, new Timestamp(player.getProfile().getCreateDate()));
                }
                else
                {
                    pstmt.setNull(4, Types.TIMESTAMP);
                }

                pstmt.executeUpdate();
            }
            finally
            {
                pstmt.close();
            }

            playerID = identity(conn);
        }

        return playerID;
    }

    /**
     * Boy would I like to change the return from Card.toString() but I'm afraid to.
     */
    private static String toString(Card card)
    {
        return card.getRankDisplaySingle() + card.getSuitDisplay();
    }

    public static boolean isPracticeHand(int handID)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt;

            pstmt = conn.prepareStatement("SELECT TRN_TYPE FROM TOURNAMENT, HAND\n" +
                                          "WHERE HND_TOURNAMENT_ID=TRN_ID AND HND_ID = ?");

            try
            {
                pstmt.setInt(1, handID);

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    if (rs.next())
                    {
                        return "PRACTICE".equals(rs.getString(1));
                    }
                    else
                    {
                        return false;
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    public static int getHandCount(String where, BindArray bindArray)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt = conn.prepareStatement
                    ("SELECT COUNT(*) FROM HAND WHERE " + where);

            try
            {
                int bindValueCount = (bindArray != null) ? bindArray.size() : 0;

                for (int i = 0; i < bindValueCount; ++i)
                {
                    pstmt.setObject(i + 1, bindArray.getValue(i));
                }

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    rs.next();

                    return rs.getInt(1);

                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    public static class ExportSummary
    {
        public List<Integer> handIDs;
        public int tournamentCount;
    }

    public static ExportSummary getExportSummary(String where, BindArray bindArray)
    {
        ExportSummary exp = new ExportSummary();

        exp.handIDs = getHandIDs(where, bindArray, 0, 0);
        exp.tournamentCount = getTournamentCount(where, bindArray);

        return exp;
    }

    public static List<Integer> getHandIDs(String where, BindArray bindArray, int first, int page)
    {
        if (first < 0)
        {
            page += first;
            first = 0;
        }

        Database database = getDatabase();
        Connection conn = database.getConnection();

        List<Integer> hands = new ArrayList<Integer>();

        try
        {
            PreparedStatement pstmt = conn.prepareStatement
                    ("SELECT HND_ID FROM HAND WHERE " + where);

            try
            {
                int bindValueCount = (bindArray != null) ? bindArray.size() : 0;

                for (int i = 0; i < bindValueCount; ++i)
                {
                    pstmt.setObject(i + 1, bindArray.getValue(i));
                }

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    rs.setFetchSize(page);

                    // TODO: is there a way to make rs.relative() work so i don't have to loop?
                    for (int i = 0; i < first; ++i)
                    {
                        rs.next();
                    }

                    while (rs.next() && ((page == 0) || (hands.size() < page)))
                    {
                        hands.add(rs.getInt(1));
                    }

                    return hands;
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    public static int getTournamentCount(String where, BindArray bindArray)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        ArrayList hands = new ArrayList();

        try
        {
            PreparedStatement pstmt = conn.prepareStatement
                    ("SELECT COUNT(DISTINCT HND_TOURNAMENT_ID) FROM HAND WHERE " + where);

            try
            {
                int bindValueCount = (bindArray != null) ? bindArray.size() : 0;

                for (int i = 0; i < bindValueCount; ++i)
                {
                    pstmt.setObject(i + 1, bindArray.getValue(i));
                }

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    if (rs.next())
                    {
                        return rs.getInt(1);
                    }
                    else
                    {
                        return 0;
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    public static int getPreviousHandID(PokerGame game, int handID)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        int foundID = 0;

        try
        {
            PreparedStatement pstmt;

            int tournamentID = storeTournament(conn, game);

            pstmt = conn.prepareStatement(
                    "SELECT MAX(HND_ID) FROM HAND\n" +
                    "WHERE HND_TOURNAMENT_ID = ? AND HND_ID < ?"
            );

            try
            {
                pstmt.setInt(1, tournamentID);
                pstmt.setInt(2, handID);

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    if (rs.next())
                    {
                        foundID = rs.getInt(1);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }

        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }

        return foundID;
    }

    public static int getNextHandID(PokerGame game, int handID)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        int foundID = 0;

        try
        {
            PreparedStatement pstmt;

            int tournamentID = storeTournament(conn, game);

            pstmt = conn.prepareStatement(
                    "SELECT MIN(HND_ID) FROM HAND\n" +
                    "WHERE HND_TOURNAMENT_ID = ? AND HND_ID > ?"
            );

            try
            {
                pstmt.setInt(1, tournamentID);
                pstmt.setInt(2, handID);

                ResultSet rs = pstmt.executeQuery();

                try
                {
                    if (rs.next())
                    {
                        foundID = rs.getInt(1);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }

        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }

        return foundID;
    }

    /**
     * returns three strings; title, summary, details
     * <p/>
     * TODO: use ImpExpHand
     */
    public static String[] getHandAsHTML(int handID, boolean bShowAll, boolean bShowReason)
    {
        Database database = getDatabase();
        Connection conn = database.getConnection();

        String title;
        String summary;

        StringBuilder details = new StringBuilder();

        try
        {
            PreparedStatement pstmt;

            Hand community = new Hand();
            int tournamentID;
            String hndTable;
            String hndNumber;
            String gameStyle;
            String gameType;
            Timestamp startDate;
            Timestamp endDate;
            int ante;
            int smBlind;
            int bigBlind;

            pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "HND_TABLE,\n" +
                    "HND_NUMBER,\n" +
                    "HND_GAME_STYLE,\n" +
                    "HND_GAME_TYPE,\n" +
                    "HND_START_DATE,\n" +
                    "HND_END_DATE,\n" +
                    "HND_ANTE,\n" +
                    "HND_SMALL_BLIND,\n" +
                    "HND_BIG_BLIND,\n" +
                    "HND_TOURNAMENT_ID,\n" +
                    "HND_COMMUNITY_CARDS_DEALT,\n" +
                    "HND_COMMUNITY_CARD_1,\n" +
                    "HND_COMMUNITY_CARD_2,\n" +
                    "HND_COMMUNITY_CARD_3,\n" +
                    "HND_COMMUNITY_CARD_4,\n" +
                    "HND_COMMUNITY_CARD_5\n" +
                    "FROM HAND\n" +
                    "WHERE HND_ID=?");

            try
            {
                pstmt.setInt(1, handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    if (rs.next())
                    {
                        hndTable = rs.getString(1);
                        hndNumber = rs.getString(2);
                        gameStyle = rs.getString(3);
                        gameType = rs.getString(4);
                        startDate = rs.getTimestamp(5);
                        endDate = rs.getTimestamp(6);
                        ante = rs.getBigDecimal(7).intValue();
                        smBlind = rs.getBigDecimal(8).intValue();
                        bigBlind = rs.getBigDecimal(9).intValue();
                        tournamentID = rs.getInt(10);

                        byte communityCardsDealt = rs.getByte(11);

                        String card;
                        card = rs.getString(12);
                        if ((card != null) && (communityCardsDealt > 0)) community.addCard(Card.getCard(card));
                        card = rs.getString(13);
                        if ((card != null) && (communityCardsDealt > 1)) community.addCard(Card.getCard(card));
                        card = rs.getString(14);
                        if ((card != null) && (communityCardsDealt > 2)) community.addCard(Card.getCard(card));
                        card = rs.getString(15);
                        if ((card != null) && (communityCardsDealt > 3)) community.addCard(Card.getCard(card));
                        card = rs.getString(16);
                        if ((card != null) && (communityCardsDealt > 4)) community.addCard(Card.getCard(card));
                    }
                    else
                    {
                        return null;
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            PokerPlayer players[] = new PokerPlayer[PokerConstants.SEATS];
            int over[] = new int[PokerConstants.SEATS];
            int win[] = new int[PokerConstants.SEATS];
            int start[] = new int[PokerConstants.SEATS];
            int end[] = new int[PokerConstants.SEATS];

            pstmt = conn.prepareStatement(
                    "SELECT DISTINCT\n" +
                    "PLH_SEAT_NUMBER,\n" +
                    "PLH_START_CHIPS,\n" +
                    "PLH_END_CHIPS,\n" +
                    "PLH_CARD_1,\n" +
                    "PLH_CARD_2,\n" +
                    "PLH_CARD_3,\n" +
                    "PLH_CARD_4,\n" +
                    "PLH_PREFLOP_ACTIONS,\n" +
                    "PLH_FLOP_ACTIONS,\n" +
                    "PLH_TURN_ACTIONS,\n" +
                    "PLH_RIVER_ACTIONS,\n" +
                    "PLH_CARDS_EXPOSED,\n" +
                    "TPL_NAME,\n" +
                    "TPL_SEQUENCE\n" + // this column selected just to disentangle duplicate names
                    "FROM TOURNAMENT_PLAYER, PLAYER_HAND\n" +
                    "WHERE PLH_PLAYER_ID = TPL_ID AND TPL_TOURNAMENT_ID=? AND PLH_HAND_ID = ?\n" +
                    "ORDER BY PLH_SEAT_NUMBER"
            );

            try
            {
                pstmt.setInt(1, tournamentID);
                pstmt.setInt(2, handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    while (rs.next())
                    {
                        int seat = rs.getInt(1);

                        players[seat] = new PokerPlayer();
                        start[seat] = rs.getBigDecimal(2).intValue();
                        end[seat] = rs.getBigDecimal(3).intValue();
                        players[seat].setChipCount(end[seat]);

                        String card;
                        card = rs.getString(4);
                        if (card != null) players[seat].getHand().addCard(Card.getCard(card));
                        card = rs.getString(5);
                        if (card != null) players[seat].getHand().addCard(Card.getCard(card));
                        card = rs.getString(6);
                        if (card != null) players[seat].getHand().addCard(Card.getCard(card));
                        card = rs.getString(7);
                        if (card != null) players[seat].getHand().addCard(Card.getCard(card));

                        players[seat].setFolded(((rs.getByte(8) | rs.getByte(9) | rs.getByte(10) | rs.getByte(11)) & BIT_FOLD) > 0);
                        players[seat].setCardsExposed(rs.getBoolean(12));
                        players[seat].setName(rs.getString(13));
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            ArrayList hist = new ArrayList();

            pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "ACT_ROUND,\n" +
                    "PLH_SEAT_NUMBER,\n" +
                    "ACT_TYPE,\n" +
                    "ACT_AMOUNT,\n" +
                    "ACT_SUB_AMOUNT,\n" +
                    "ACT_ALL_IN,\n" +
                    "ACT_INTENT\n" +
                    "FROM PLAYER_HAND, PLAYER_ACTION\n" +
                    "WHERE PLH_PLAYER_ID = ACT_PLAYER_ID AND PLH_HAND_ID=ACT_HAND_ID AND PLH_HAND_ID = ?\n" +
                    "ORDER BY ACT_SEQUENCE"
            );

            boolean bAnte = false;

            try
            {
                pstmt.setInt(1, handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    while (rs.next())
                    {
                        int round = rs.getInt(1);
                        int seat = rs.getInt(2);
                        int actionType = HandAction.decodeActionType(rs.getString(3));
                        int amount = rs.getBigDecimal(4).intValue();
                        int subamount = rs.getBigDecimal(5).intValue();
                        boolean bAllIn = rs.getBoolean(6);
                        String intent = rs.getString(7);

                        HandAction action = new HandAction(players[seat], round, actionType, amount, subamount, intent);
                        action.setAllIn(bAllIn);
                        hist.add(action);

                        switch (actionType)
                        {
                            case HandAction.ACTION_OVERBET:
                                over[seat] += amount;
                                break;
                            case HandAction.ACTION_WIN:
                                win[seat] += amount;
                                break;
                            case HandAction.ACTION_ANTE:
                                bAnte = true;
                                break;
                        }
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            title = PropertyConfig.getMessage("msg.handhist.title", new Object[]{hndTable, hndNumber});

            StringBuilder sb2 = new StringBuilder();

            for (int i = 0; i < PokerConstants.SEATS; ++i)
            {
                int delta = end[i] - start[i];

                if (delta != 0)
                {
                    sb2.append(PropertyConfig.getMessage
                            ("msg.handhist.chipdelta", players[i].getName(),
                             PropertyConfig.getMessage("msg.handhist." + ((delta > 0) ? "wonchips" : "lostchips")),
                             Math.abs(delta),
                             (players[i].getChipCount() == 0) ? PropertyConfig.getMessage("msg.handhist.busted") : "&nbsp;"));
                }
            }

            String unit = PropertyConfig.getMessage("msg.chipunit.cash");

            Object[] oParams = new Object[]{
                    sb2.toString(),
                    PropertyConfig.getMessage("msg.gamestyle." + gameStyle.toLowerCase()),
                    PropertyConfig.getMessage("list.gameType." + gameType.toLowerCase()),
                    (ante > 0) ? unit : "",
                    (ante > 0) ?
                    ante :
                    (Object) PropertyConfig.getMessage("msg.forcedbet.none"),
                    unit, smBlind,
                    unit, bigBlind,
                    (startDate != null) ?
                    new Date(startDate.getTime()) :
                    (Object) PropertyConfig.getMessage("msg.value.unknown"),
                    (endDate != null) ?
                    PropertyConfig.getMessage("msg.handhist.enddate", new Date(endDate.getTime())) :
                    ""
            };

            summary = PropertyConfig.getMessage("msg.handhist.summary", oParams);

            sb2.setLength(0);

            if (bAnte) appendHistory(details, community, hist, HoldemHand.ROUND_PRE_FLOP, true, bShowAll, bShowReason);
            appendHistory(details, community, hist, HoldemHand.ROUND_PRE_FLOP, false, bShowAll, bShowReason);
            appendHistory(details, community, hist, HoldemHand.ROUND_FLOP, false, bShowAll, bShowReason);
            appendHistory(details, community, hist, HoldemHand.ROUND_TURN, false, bShowAll, bShowReason);
            appendHistory(details, community, hist, HoldemHand.ROUND_RIVER, false, bShowAll, bShowReason);

            appendShowdown(details, hist, community, bShowAll);
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }

        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }

        //System.out.println(summary.toString());

        return new String[]{title, summary, details.toString()};
    }

    public static ImpExpHand getHandForExport(int handID)
    {
        ImpExpHand ieHand = new ImpExpHand();

        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();

        ieHand.profileNumber = profile.getFileNumber(profile.getFile());
        ieHand.handID = handID;

        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt;

            pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "HND_TABLE,\n" +
                    "HND_NUMBER,\n" +
                    "HND_GAME_STYLE,\n" +
                    "HND_GAME_TYPE,\n" +
                    "HND_START_DATE,\n" +
                    "HND_END_DATE,\n" +
                    "HND_ANTE,\n" +
                    "HND_SMALL_BLIND,\n" +
                    "HND_BIG_BLIND,\n" +
                    "HND_TOURNAMENT_ID,\n" +
                    "HND_COMMUNITY_CARDS_DEALT,\n" +
                    "HND_COMMUNITY_CARD_1,\n" +
                    "HND_COMMUNITY_CARD_2,\n" +
                    "HND_COMMUNITY_CARD_3,\n" +
                    "HND_COMMUNITY_CARD_4,\n" +
                    "HND_COMMUNITY_CARD_5\n" +
                    "FROM HAND\n" +
                    "WHERE HND_ID=?");

            try
            {
                pstmt.setInt(1, ieHand.handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    if (rs.next())
                    {
                        ieHand.hndTable = rs.getString(1);
                        ieHand.hndNumber = rs.getString(2);
                        ieHand.gameStyle = rs.getString(3);
                        ieHand.gameType = rs.getString(4);
                        ieHand.startDate.setTime(new Date(rs.getTimestamp(5).getTime()));
                        ieHand.endDate.setTime(new Date(rs.getTimestamp(6).getTime()));
                        ieHand.ante = rs.getBigDecimal(7).intValue();
                        ieHand.smallBlind = rs.getBigDecimal(8).intValue();
                        ieHand.bigBlind = rs.getBigDecimal(9).intValue();
                        ieHand.tournamentID = rs.getInt(10);

                        byte communityCardsDealt = rs.getByte(11);

                        String card;
                        card = rs.getString(12);
                        if ((card != null) && (communityCardsDealt > 0)) ieHand.community.addCard(Card.getCard(card));
                        card = rs.getString(13);
                        if ((card != null) && (communityCardsDealt > 1)) ieHand.community.addCard(Card.getCard(card));
                        card = rs.getString(14);
                        if ((card != null) && (communityCardsDealt > 2)) ieHand.community.addCard(Card.getCard(card));
                        card = rs.getString(15);
                        if ((card != null) && (communityCardsDealt > 3)) ieHand.community.addCard(Card.getCard(card));
                        card = rs.getString(16);
                        if ((card != null) && (communityCardsDealt > 4)) ieHand.community.addCard(Card.getCard(card));
                    }
                    else
                    {
                        return null;
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "TRN_NAME,\n" +
                    "TRN_START_DATE\n" +
                    "FROM TOURNAMENT\n" +
                    "WHERE TRN_ID=?");

            try
            {
                pstmt.setInt(1, ieHand.tournamentID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    rs.next();

                    ieHand.tournamentName = rs.getString(1);
                    ieHand.tournamentStartDate.setTime(new Date(rs.getTimestamp(2).getTime()));
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }


            int players = 0;

            pstmt = conn.prepareStatement(
                    "SELECT DISTINCT\n" +
                    "PLH_SEAT_NUMBER,\n" +
                    "PLH_START_CHIPS,\n" +
                    "PLH_END_CHIPS,\n" +
                    "PLH_CARD_1,\n" +
                    "PLH_CARD_2,\n" +
                    "PLH_CARD_3,\n" +
                    "PLH_CARD_4,\n" +
                    "PLH_PREFLOP_ACTIONS,\n" +
                    "PLH_FLOP_ACTIONS,\n" +
                    "PLH_TURN_ACTIONS,\n" +
                    "PLH_RIVER_ACTIONS,\n" +
                    "PLH_CARDS_EXPOSED,\n" +
                    "TPL_NAME,\n" +
                    "TPL_PROFILE_CREATE_DATE,\n" +
                    "TPL_SEQUENCE\n" + // this column selected just to disentangle duplicate names
                    "FROM TOURNAMENT_PLAYER, PLAYER_HAND\n" +
                    "WHERE PLH_PLAYER_ID = TPL_ID AND TPL_TOURNAMENT_ID=? AND PLH_HAND_ID = ?\n" +
                    "ORDER BY PLH_SEAT_NUMBER"
            );

            try
            {
                pstmt.setInt(1, ieHand.tournamentID);
                pstmt.setInt(2, ieHand.handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    while (rs.next())
                    {
                        int seat = rs.getInt(1);

                        ieHand.players[seat] = new PokerPlayer();
                        ieHand.players[seat].setSeat(seat);
                        ieHand.betChips[seat] = 0;
                        ieHand.startChips[seat] = rs.getBigDecimal(2).intValue();
                        ieHand.endChips[seat] = rs.getBigDecimal(3).intValue();
                        ieHand.players[seat].setChipCount(ieHand.endChips[seat]);

                        String card;
                        card = rs.getString(4);
                        if (card != null) ieHand.players[seat].getHand().addCard(Card.getCard(card));
                        card = rs.getString(5);
                        if (card != null) ieHand.players[seat].getHand().addCard(Card.getCard(card));
                        card = rs.getString(6);
                        if (card != null) ieHand.players[seat].getHand().addCard(Card.getCard(card));
                        card = rs.getString(7);
                        if (card != null) ieHand.players[seat].getHand().addCard(Card.getCard(card));

                        ieHand.players[seat].setFolded(((rs.getByte(8) | rs.getByte(9) | rs.getByte(10) | rs.getByte(11)) & BIT_FOLD) > 0);
                        ieHand.players[seat].setCardsExposed(rs.getBoolean(12));
                        ieHand.players[seat].setName(rs.getString(13));

                        Timestamp ts = rs.getTimestamp(14);
                        if ((ts != null) && (ts.getTime() == PlayerProfileOptions.getDefaultProfile().getCreateDate()))
                        {
                            ieHand.localHumanPlayerSeat = seat;
                        }

                        ++players;
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            pstmt = conn.prepareStatement(
                    "SELECT\n" +
                    "ACT_ROUND,\n" +
                    "PLH_SEAT_NUMBER,\n" +
                    "ACT_TYPE,\n" +
                    "ACT_AMOUNT,\n" +
                    "ACT_SUB_AMOUNT,\n" +
                    "ACT_ALL_IN,\n" +
                    "ACT_INTENT\n" +
                    "FROM PLAYER_HAND, PLAYER_ACTION\n" +
                    "WHERE PLH_PLAYER_ID = ACT_PLAYER_ID AND PLH_HAND_ID=ACT_HAND_ID AND PLH_HAND_ID = ?\n" +
                    "ORDER BY ACT_SEQUENCE"
            );

            int firstBlindSeat = -1;

            try
            {
                pstmt.setInt(1, ieHand.handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    while (rs.next())
                    {
                        int round = rs.getInt(1);
                        int seat = rs.getInt(2);
                        int actionType = HandAction.decodeActionType(rs.getString(3));
                        int amount = rs.getBigDecimal(4).intValue();
                        int subamount = rs.getBigDecimal(5).intValue();
                        boolean bAllIn = rs.getBoolean(6);
                        String intent = rs.getString(7);

                        HandAction action = new HandAction(ieHand.players[seat], round, actionType, amount, subamount, intent);
                        action.setAllIn(bAllIn);
                        ieHand.hist.add(action);

                        switch (actionType)
                        {
                            case HandAction.ACTION_OVERBET:
                                ieHand.overbetChips[seat] += amount;
                                break;
                            case HandAction.ACTION_WIN:
                                ieHand.winChips[seat] += amount;
                                break;
                            case HandAction.ACTION_CALL:
                            case HandAction.ACTION_BET:
                            case HandAction.ACTION_RAISE:
                            case HandAction.ACTION_ANTE:
                                ieHand.betChips[seat] += amount;
                                break;
                            case HandAction.ACTION_BLIND_SM:
                            case HandAction.ACTION_BLIND_BIG:
                                ieHand.betChips[seat] += amount;
                                if (firstBlindSeat < 0) firstBlindSeat = seat;
                                break;
                        }
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            if (players == 2)
            {
                ieHand.buttonSeat = firstBlindSeat;
            }
            else
            {
                // figure out where the button must have been
                for (int seat = firstBlindSeat - 1; seat > firstBlindSeat - PokerConstants.SEATS; --seat)
                {
                    if (ieHand.players[(seat + PokerConstants.SEATS) % PokerConstants.SEATS] != null)
                    {
                        ieHand.buttonSeat = (seat + PokerConstants.SEATS) % PokerConstants.SEATS;
                        break;
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }

        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }

        return ieHand;
    }

    private static void appendHistory
            (StringBuilder sb, Hand community, ArrayList hist, int nRound,
             boolean bAnte, boolean bShowAll, boolean bShowReason)
    {
        StringBuilder sb2 = new StringBuilder();

        PokerPlayer p;
        HandAction action;
        int nNum = 0;
        int nPrior = 0;

        HandInfoFast info = new HandInfoFast();

        if (nRound == HoldemHand.ROUND_PRE_FLOP)
        {
            community = new Hand();
        }
        else if ((nRound == HoldemHand.ROUND_FLOP) && (community.size() > 3))
        {
            community = new Hand(community.getCard(0), community.getCard(1), community.getCard(2));
        }
        else if ((nRound == HoldemHand.ROUND_TURN) && (community.size() > 4))
        {
            community = new Hand(community.getCard(0), community.getCard(1), community.getCard(2), community.getCard(3));
        }

        for (int i = 0; i < hist.size(); i++)
        {
            action = (HandAction) hist.get(i);
            p = action.getPlayer();

            // must be from this round
            if (action.getRound() != nRound || (!bAnte && action.getAction() == HandAction.ACTION_ANTE)) continue;
            if (bAnte && action.getAction() != HandAction.ACTION_ANTE) continue;

            Hand hand = p.getHandSorted();

            String handHTML;
            String handShown = "";
            String sReason = (bShowAll && bShowReason) ? decodeReason(action.getDebug()) : null;

            if (sReason == null) sReason = "";
            else sReason = " " + PropertyConfig.getMessage("msg.hist.reason", sReason);

            if (p.isCardsExposed() || bShowAll)
            {
                handHTML = hand.toHTML();

                if (!community.isEmpty())
                {
                    info.getScore(hand, community);
                    handShown = "&nbsp;-&nbsp;" + info.toString(", ", false);
                }
            }
            else
            {
                handHTML = "<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">";
            }

            String sSnippet = action.getHTMLSnippet("msg.handhist", nPrior, null);

            // get right raise icon
            if (action.getAction() == HandAction.ACTION_RAISE)
            {
                nPrior++;
            }

            // count actions added
            nNum++;

            // append message
            sb2.append(PropertyConfig.getMessage("msg.hist.x", Utils.encodeHTML(p.getName()), sSnippet,
                                                 handHTML, handShown, sReason));
            sb2.append("\n");
        }

        if (nNum != 0)
        {

            // if doing antes, change round (match client.properties)
            if (bAnte) nRound = 9;

            sb.append(PropertyConfig.getMessage("msg.hand.history",
                                                PropertyConfig.getMessage("msg.round." + nRound),
                                                sb2.toString(), community.toHTML()));
        }
    }

    /**
     * Get the logical database object.
     */
    public static Database getDatabase()
    {
        return DatabaseManager.getDatabase(CLIENT_DATABASE_NAME);
    }

    private static String getActualDatabaseName(PlayerProfile profile)
    {
        GameEngine gameEngine = GameEngine.getGameEngine();
        String uniqueKey = (gameEngine == null) ? "no-engine" : gameEngine.getPublicUseKey();

        return CLIENT_DATABASE_NAME +
               ((profile == null) ? "" : ("-" + profile.getFileNum())) +
               ((uniqueKey == null) ? "" : ("-" + Math.abs(uniqueKey.hashCode())));
    }

    /**
     * Initialize a logical database object.
     */
    private static void initDatabase(PlayerProfile profile, File saveDir)
    {
        // Format the driver URL using a unique database name.
        if (saveDir == null) {
            saveDir = GameConfigUtils.getSaveDir();
        }
        File databaseDir = new File(saveDir, DATABASE_DIRECTORY);
        String databaseName = getActualDatabaseName(profile);

        File clientPath = new File(databaseDir, databaseName);
        String driverURL = DATABASE_DRIVER_URL_PREFIX + clientPath.getAbsolutePath();

        // Add the database.
        Map<String, String> htParams = new HashMap<String, String>();
        htParams.put(DatabaseManager.PARAM_DRIVER_CLASS, DATABASE_DRIVER_CLASS);
        htParams.put(DatabaseManager.PARAM_DRIVER_URL, driverURL);
        htParams.put(DatabaseManager.PARAM_USERNAME, DATABASE_USERNAME);
        htParams.put(DatabaseManager.PARAM_PASSWORD, DATABASE_PASSWORD);
        DatabaseManager.addDatabase(CLIENT_DATABASE_NAME, htParams);

        Connection conn = getDatabase().getConnection();
        try
        {
            Statement stmt = conn.createStatement();
            if (!DebugConfig.isTestingOn())
            {
                stmt.executeUpdate("SET SCRIPTFORMAT COMPRESSED");
            }
            else
            {
                stmt.executeUpdate("SET SCRIPTFORMAT TEXT");
            }
            stmt.executeUpdate("SET WRITE_DELAY false");
            stmt.executeUpdate("SET PROPERTY \"sql.enforce_strict_size\" true");
            stmt.executeUpdate("SET PROPERTY \"hsqldb.cache_scale\" 10");
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException ignore)
            {
            }
        }
    }

    public static void shutdownDatabase()
    {
        Connection conn = getDatabase().getConnection();
        try
        {
            conn.createStatement().executeUpdate("SHUTDOWN");
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException ignore)
            {
            }
        }
    }

    public static String getHandListHTML(int handID)
    {
        Timestamp playerCreateDate = new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate());

        Database database = getDatabase();
        Connection conn = database.getConnection();

        try
        {
            PreparedStatement pstmt;

            // select player's hand (separate in case current player was an observer)
            pstmt = conn.prepareStatement(
                    "SELECT " +
                    "PLH_CARD_1,\n" +
                    "PLH_CARD_2\n" +
                    "FROM PLAYER_HAND, TOURNAMENT_PLAYER\n" +
                    "WHERE PLH_HAND_ID=?\n" +
                    "  AND PLH_PLAYER_ID=TPL_ID AND TPL_PROFILE_CREATE_DATE=?");

            String holeCards[] = new String[2];

            try
            {
                pstmt.setInt(1, handID);
                pstmt.setTimestamp(2, playerCreateDate);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    if (rs.next())
                    {
                        holeCards[0] = rs.getString(1);
                        holeCards[1] = rs.getString(2);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }

            // select community cards
            pstmt = conn.prepareStatement(
                    "SELECT " +
                    "HND_COMMUNITY_CARDS_DEALT,\n" +
                    "HND_COMMUNITY_CARD_1,\n" +
                    "HND_COMMUNITY_CARD_2,\n" +
                    "HND_COMMUNITY_CARD_3,\n" +
                    "HND_COMMUNITY_CARD_4,\n" +
                    "HND_COMMUNITY_CARD_5\n" +
                    "FROM HAND\n" +
                    "WHERE HND_ID=?");

            try
            {
                pstmt.setInt(1, handID);

                ResultSet rs = DatabaseManager.executeQuery(pstmt);

                try
                {
                    if (rs.next())
                    {
                        StringBuilder buf = new StringBuilder();

                        String card;

                        buf.append(Card.getCard(holeCards[0]).toHTML());
                        buf.append(Card.getCard(holeCards[1]).toHTML());
                        buf.append("&nbsp;&nbsp;");
                        byte communityCardsDealt = rs.getByte(1);
                        card = rs.getString(2);
                        if ((card != null) && (communityCardsDealt > 0)) buf.append(Card.getCard(card).toHTML());
                        card = rs.getString(3);
                        if ((card != null) && (communityCardsDealt > 1)) buf.append(Card.getCard(card).toHTML());
                        card = rs.getString(4);
                        if ((card != null) && (communityCardsDealt > 2)) buf.append(Card.getCard(card).toHTML());
                        card = rs.getString(5);
                        if ((card != null) && (communityCardsDealt > 3)) buf.append(Card.getCard(card).toHTML());
                        card = rs.getString(6);
                        if ((card != null) && (communityCardsDealt > 4)) buf.append(Card.getCard(card).toHTML());

                        return buf.toString();
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            finally
            {
                pstmt.close();
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException ignore)
            {
            }
        }

        return "";
    }

    public static void appendShowdown(StringBuilder sb, List<HandAction> hist, Hand community, boolean bShowAll)
    {
        StringBuilder sb2 = new StringBuilder();

        for (int i = 8; i >= 0; i--)  // can have a max of 9 pots
        {
            appendShowdown(sb2, hist, community, bShowAll, i);
        }

        sb.append(PropertyConfig.getMessage("msg.hand.history",
                                            PropertyConfig.getMessage("msg.round." + HoldemHand.ROUND_SHOWDOWN),
                                            sb2.toString(), community.toHTML()));
    }

    private static void appendShowdown(StringBuilder sb, List<HandAction> hist, Hand community, boolean bShowAll, int nPot)
    {
        HandAction action;

        int nNum = 0;
        int potTotal = 0;

        // first loop to count and sum

        for (int i = 0; i < hist.size(); i++)
        {
            action = (HandAction) hist.get(i);

            if (action.getRound() != HoldemHand.ROUND_SHOWDOWN) continue;
            if (action.getSubAmount() != nPot) continue;

            if (action.getAction() == HandAction.ACTION_WIN)
            {
                potTotal += action.getAmount();
            }

            nNum++;
        }

        if (nNum == 0)
        {
            return;
        }

        String sHeaderKey = null;

        if ((sb.length() > 0) || nPot > 0) // hack?  dunno.  no header if there's only the main pot
        {
            if (nPot == 0)
            {
                sHeaderKey = "msg.handhist.pot.main";
            }
            else
            {
                sHeaderKey = "msg.handhist.pot.side";
            }
        }

        PokerPlayer player;

        HandInfoFast info = new HandInfoFast();

        StringBuilder sb2 = new StringBuilder();

        for (int i = 0; i < hist.size(); i++)
        {
            action = (HandAction) hist.get(i);

            if (action.getRound() != HoldemHand.ROUND_SHOWDOWN) continue;
            if (action.getSubAmount() != nPot) continue;

            player = action.getPlayer();

            // don't actually display headers for overbet side pots
            if (action.getAction() == HandAction.ACTION_OVERBET)
            {
                sHeaderKey = null;
            }

            Hand hand = player.getHandSorted();

            String handHTML;
            String handShown = "";

            if (player.isCardsExposed() || bShowAll)
            {
                handHTML = hand.toHTML();

                if (!community.isEmpty())
                {
                    info.getScore(hand, community);
                    handShown = "&nbsp;-&nbsp;" + info.toString(", ", false);
                }
            }
            else
            {
                handHTML = "<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">";
            }

            String sSnippet = action.getHTMLSnippet("msg.handhist", 0, null);

            // append message
            sb2.append(PropertyConfig.getMessage("msg.hist.x", Utils.encodeHTML(player.getName()), sSnippet,
                                                 handHTML, handShown, ""));
            sb2.append("\n");
        }

        if (sHeaderKey != null)
        {
            String sHeader = PropertyConfig.getMessage(sHeaderKey, nPot, potTotal);

            sb.append(PropertyConfig.getMessage("msg.handhist.pot", sHeader, sb2.toString()));
        }
        else
        {
            sb.append(sb2.toString());
        }
    }

    public static String decodeReason(String sReason)
    {
        if (sReason == null)
        {
            return null;
        }

        if (sReason.startsWith("V1:"))
        {
            return null;

            // might be useful, might not - for now, just returning null
            // return DebugConfig.isTestingOn() ? sReason.substring(3) : null;
        }

        try
        {
            return PropertyConfig.getMessage("msg.aioutcome." + sReason);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
