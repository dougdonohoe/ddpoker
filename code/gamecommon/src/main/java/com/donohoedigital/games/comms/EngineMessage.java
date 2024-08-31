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
 * EngineMessage.java
 *
 * Created on March 7, 2003, 4:16 PM
 */

package com.donohoedigital.games.comms;

import com.donohoedigital.comms.*;

import java.io.*;

/**
 * @author donohoe
 */
@DataCoder('E')
public class EngineMessage extends DDMessage
{
    // game id
    public static final String GAME_NOTDEFINED = null;

    // player id
    public static final int PLAYER_NOTDEFINED = -1;
    public static final int PLAYER_SERVER = -2;
    public static final int PLAYER_GROUP = -3; // several ids, specified by PARAM_PLAYER_IDS

    // Message types from client
    public static final int CAT_SERVER_QUERY = 0;
    public static final int CAT_NEW_GAME = 1;
    public static final int CAT_JOIN_GAME = 2;
    public static final int CAT_POLL_UPDATES = 3;
    public static final int CAT_GET_GAME_STATE = 4;
    public static final int CAT_ACTION_DONE = 5;
    public static final int CAT_CHAT = 6;
    public static final int CAT_INFO = 7;
    public static final int CAT_ACTION_REQUEST = 8;
    public static final int CAT_PLAYER_UPDATE = 9;
    public static final int CAT_STATUS = 10;
    public static final int CAT_USER_REG = 11;
    public static final int CAT_VERIFY_KEY = 12;
    public static final int CAT_PUBLIC_IP = 13;
    // deprecated - public static final int CAT_CHECK_PATCH = 14;
    public static final int CAT_CHECK_DDMSG = 15;

    // Return messages to client
    public static final int CAT_COMPOSITE_MESSAGE = 100;
    public static final int CAT_GAME_DATA = 101;
    public static final int CAT_OK = 102;
    public static final int CAT_GAME_UPDATE = 103;
    public static final int CAT_EMPTY = 104;

    // sent in email
    public static final int CAT_EMAIL_JOIN_GAME = 200;

    // Error message types returned to client
    public static final int CAT_ERROR_BAD_EMAIL = 1000;

    /**
     * convert cat to readable string
     */
    public String getDebugCat()
    {
        int nCat = getCategory();
        switch (nCat)
        {
            case CAT_SERVER_QUERY:
                return "server query";
            case CAT_NEW_GAME:
                return "new game";
            case CAT_JOIN_GAME:
                return "join game";
            case CAT_POLL_UPDATES:
                return "poll";
            case CAT_GET_GAME_STATE:
                return "get game state";
            case CAT_ACTION_DONE:
                return "action done";
            case CAT_CHAT:
                return "chat";
            case CAT_INFO:
                return "info";
            case CAT_ACTION_REQUEST:
                return "action request";
            case CAT_PLAYER_UPDATE:
                return "player update";
            case CAT_STATUS:
                return "status";
            case CAT_USER_REG:
                return "user registration";
            case CAT_VERIFY_KEY:
                return "verify key";
            case CAT_PUBLIC_IP:
                return "public ip";
            case CAT_CHECK_DDMSG:
                return "check DD msg";

            case CAT_COMPOSITE_MESSAGE:
                return "composite";
            case CAT_GAME_DATA:
                return "game data";
            case CAT_OK:
                return "ok";
            case CAT_GAME_UPDATE:
                return "game update";
            case CAT_EMPTY:
                return "empty";

            case CAT_EMAIL_JOIN_GAME:
                return "join email";

            case CAT_ERROR_BAD_EMAIL:
                return "bad email";

            default:
                return "(" + nCat + ")";
        }
    }

    public static final String PARAM_GAME_OPTIONS = "options";  // game options hash map
    public static final String PARAM_LAST_TIMESTAMPS = "last";  // time stamp of last message recieved and processed
    public static final String PARAM_PLAYER_TIMESTAMPS = "playerts"; // timestamp of last action by given player
    public static final String PARAM_GAME_ID = "gid";           // all messages (constructor)
    public static final String PARAM_FROM_PLAYER_ID = "pid";    // all messages (constructor)
    public static final String PARAM_SEQ_ID = "seq";            // all messages (set upon send)
    public static final String PARAM_TO_PLAYER_ID = "tid";      // id of player message intended for (typically set in PlayerQueue)
    public static final String PARAM_NUM_PLAYERS = "np";        // register games
    public static final String PARAM_EMAIL_ADDRS = "em";        // register game emails
    public static final String PARAM_NAMES = "nm";              // register game names
    public static final String PARAM_COLORS = "co";             // register game colors
    public static final String PARAM_REF_PLAYER_ID = "rid";     // multiple uses - target player 
    public static final String PARAM_EMAIL_ID = "eid";          // email id (identifies jsp)
    public static final String PARAM_EMAIL_TEXT = "etxt";       // text for email
    public static final String PARAM_EMAIL = "email";           // used in game registration
    public static final String PARAM_PASSWORD = "pass";         // ditto
    public static final String PARAM_PLAYER_IDS = "ids";        // list of player ids a client controls
    public static final String PARAM_ACTION = "act";            // action server is waiting on / or player requesting
    public static final String PARAM_UPDATE_TYPE = "updtyp";    // update type for incorporating game changes
    public static final String PARAM_RESULT = "result";         // result from in an action confirmation (optional)
    public static final String PARAM_PARAMS = "params";         // extra params (for action requests)
    public static final String PARAM_OPTION = "option";         // option param (used in game data updates)
    public static final String PARAM_ELIMINATED = "elim";       // player eliminated
    public static final String PARAM_EVICTED = "evict";         // player evicted
    public static final String PARAM_GAME_OVER = "gameover";    // sent when game over
    public static final String PARAM_GAME_DELETED = "gamedel";  // sent when game missing on server
    public static final String PARAM_GAME_IDS = "gids";         // ids for status query
    public static final String PARAM_PASSWORDS = "passes";      // passwords for status query
    public static final String PARAM_STATUS = "status";         // status data returned
    public static final String PARAM_BAD_KEY = "badkey";        // boolean to indicate bad registration number
    public static final String PARAM_BANNED_KEY = "bannedkey";  // boolean to indicate banned registration
    public static final String PARAM_URL = "url";               // URL for redirects (server query)
    public static final String PARAM_DDMSG = "ddmsg";           // message to user
    public static final String PARAM_DDMSG_ID = "ddmsgid";      // message id (to track what user has seen)
    public static final String PARAM_DDPROFILE = "ddprofile";   // player profile name

    public static final String PARAM_WAIT_MIN = "waitmin";      // these and below control OnlineGameManager
    public static final String PARAM_WAIT_ADD = "waitadd";
    public static final String PARAM_WAIT_ADD_PER = "waitaddper";
    public static final String PARAM_WAIT_MAX = "waitmax";
    public static final String PARAM_WAIT_ERROR = "waiterror";

    public static final String PARAM_IP = "ipaddr";             // IP address return for P2P

    public static final String PARAM_PATCH_OS = "patchos";

    /**
     * empty constructor
     */
    public EngineMessage()
    {
    }

    /**
     * Create new message no data
     */
    public EngineMessage(String sGameID, int fromPlayerID, int nCat)
    {
        this(sGameID, fromPlayerID, nCat, (byte[]) null);
    }

    /**
     * Create new message with string data
     */
    public EngineMessage(String sGameID, int fromPlayerID, int nCat, String sData)
    {
        super(nCat, sData);
        setGameID(sGameID);
        setFromPlayerID(fromPlayerID);
    }

    /**
     * Create new message with byte data
     */
    public EngineMessage(String sGameID, int fromPlayerID, int nCat, byte[] baData)
    {
        super(nCat, baData);
        setGameID(sGameID);
        setFromPlayerID(fromPlayerID);
    }

    /**
     * Create new message with file data
     */
    public EngineMessage(String sGameID, int fromPlayerID, int nCat, File filedata)
    {
        super(nCat, filedata);
        setGameID(sGameID);
        setFromPlayerID(fromPlayerID);
    }

    /**
     * Create new message with file array
     */
    public EngineMessage(String sGameID, int fromPlayerID, int nCat, File[] filedatas)
    {
        super(nCat, filedatas);
        setGameID(sGameID);
        setFromPlayerID(fromPlayerID);
    }

    /**
     * Get game id
     */
    public String getGameID()
    {
        String gid = getString(PARAM_GAME_ID);
        if (gid == null) return GAME_NOTDEFINED;
        return gid;
    }

    /**
     * Set game id
     */
    public void setGameID(String gid)
    {
        setString(PARAM_GAME_ID, gid);
    }

    /**
     * Get player id
     */
    public int getFromPlayerID()
    {
        Integer pid = getInteger(PARAM_FROM_PLAYER_ID);
        if (pid == null) return PLAYER_NOTDEFINED;
        return pid;
    }

    /**
     * Set player id
     */
    public void setFromPlayerID(int id)
    {
        setInteger(PARAM_FROM_PLAYER_ID, id);
    }

    /**
     * Get seq id.  Returns "-1" if not there (note that the starting
     * seq ID, as set from Game.java will always be 1
     */
    public long getSeqID()
    {
        Long pid = getLong(PARAM_SEQ_ID);
        if (pid == null) return -1;
        return pid;
    }

    /**
     * Set seq id
     */
    public void setSeqID(long id)
    {
        setLong(PARAM_SEQ_ID, id);
    }

    /////
    ///// DEBUGGING
    /////

    public String getDebugInfoShort()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CAT: [").append(getDebugCat()).append("]");
        sb.append(", time=").append(getLong(PARAM_TIME));
        sb.append(", key=").append(getKey());
        return sb.toString();
    }

    public String getDebugInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CAT: [").append(getDebugCat()).append("]");
        sb.append(", WHO: ").append(getDebugFrom());
        sb.append(", PARAMS: ").append(toStringParams());
        return sb.toString();
    }

    public String getDebugInfoLong()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CAT: [").append(getDebugCat()).append("]");
        sb.append(", WHO: ").append(getDebugFrom());
        sb.append(", PARAMS: ").append(toStringParams());
        sb.append(", DATA: ").append(getDataAsString());
        return sb.toString();
    }

    /**
     * convert from id to a readable string
     */
    private String getDebugFrom()
    {
        int id = getFromPlayerID();
        switch (id)
        {
            case PLAYER_NOTDEFINED:
                return "(not defined)";
            case PLAYER_SERVER:
                return "(server)";
            case PLAYER_GROUP:
                return "(group)";
            default:
                return "(player " + id + ")";
        }
    }
}
