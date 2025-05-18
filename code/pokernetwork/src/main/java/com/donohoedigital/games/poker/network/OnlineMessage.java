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
 * OnlineMessage.java
 *
 * Created on December 2, 2004, 8:10 AM
 */

package com.donohoedigital.games.poker.network;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.udp.*;

import java.util.*;

/**
 * Wrapper class to provide easy access to online data
 * contained in a DDMessage object (used to pass data
 * between games)
 *
 * @author donohoe
 */
public class OnlineMessage
{
    DDMessage data_;

    // message categories
    public static final int CAT_TEST = 20000;
    public static final int CAT_JOIN = 20001;
    public static final int CAT_CHAT = 20002;
    public static final int CAT_GAME_UPDATE = 20003;
    public static final int CAT_QUIT = 20004;
    public static final int CAT_CANCEL = 20005;
    public static final int CAT_REMOVE_WAIT_LIST = 20006;
    public static final int CAT_HAND_ACTION = 20007;
    public static final int CAT_REBUY = 20008;
    public static final int CAT_ADDON = 20009;
    public static final int CAT_READY = 20010;
    public static final int CAT_WAN_GAME_ADD = 20011;
    public static final int CAT_WAN_GAME_REMOVE = 20012;
    public static final int CAT_WAN_GAME_LIST = 20013;
    public static final int CAT_WAN_PROFILE_ADD = 20014;
    public static final int CAT_WAN_PROFILE_RESET = 20015; // formerly CAT_WAN_PROFILE_UPDATE
    public static final int CAT_WAN_PROFILE_VALIDATE = 20016;
    public static final int CAT_WAN_PROFILE_ACTIVATE = 20017;
    public static final int CAT_CANCEL_ACTION = 20018;
    public static final int CAT_PLAYER_UPDATE = 20019;
    public static final int CAT_WAN_PROFILE_SEND_PASSWORD = 20020;
    public static final int CAT_WAN_PROFILE_CHANGE_PASSWORD = 20021;
    public static final int CAT_CONNECTION = 200022;
    public static final int CAT_CHANGE_TABLE = 20023;
    public static final int CAT_ALIVE = 20024;
    public static final int CAT_WAN_GAME_START = 20025;
    public static final int CAT_WAN_GAME_STOP = 20026;
    public static final int CAT_WAN_GAME_END = 20027;
    public static final int CAT_WAN_PROFILE_LINK = 20028; // formerly CAT_WAN_PROFILE_GET
    public static final int CAT_WAN_GAME_UPDATE = 20029;
    public static final int CAT_CHAT_HELLO = 20030;
    public static final int CAT_CHAT_ADMIN = 20031;
    public static final int CAT_WAN_PROFILE_SYNC_PASSWORD = 20032;

    // server to client messages
    public static final int CAT_CLIENT_JOIN = 21001;
    public static final int CAT_PROCESS_PHASE = 21002;

    /**
     * For debugging, get string name of cat
     */
    public String toStringCategory()
    {
        switch (getCategory())
        {
            case CAT_TEST:
                return "test";
            case CAT_JOIN:
                return "join";
            case CAT_CHAT:
                return "chat" + (getPlayerName() == null ? ": " : " from " + getPlayerName() + ": ") + getChat();
            case CAT_CHAT_HELLO:
                return "chat-hello from " + getPlayerName();
            case CAT_CHAT_ADMIN:
                return "chat-admin: " + PokerConstants.toStringAdminType(getChatType());
            case CAT_GAME_UPDATE:
                return "game-update";// (run phase: " + getPhaseName() +", runProcessTable(): " + isRunProcessTable() +")";
            case CAT_QUIT:
                return "quit";
            case CAT_CANCEL:
                return "cancel";
            case CAT_REMOVE_WAIT_LIST:
                return "remove-wait-list id " + getFromPlayerID();
            case CAT_HAND_ACTION:
                return "hand-action " + getHandAction().toString();
            case CAT_REBUY:
                return "rebuy cash: " + getCash() + "; chips: " + getChips();
            case CAT_ADDON:
                return "add-on cash: " + getCash() + "; chips: " + getChips();
            case CAT_READY:
                return "ready id " + getFromPlayerID();
            case CAT_WAN_GAME_ADD:
                return "wan-game-add";
            case CAT_WAN_GAME_REMOVE:
                return "wan-game-remove";
            case CAT_WAN_GAME_LIST:
                return "wan-game-list";
            case CAT_WAN_GAME_START:
                return "wan-game-start";
            case CAT_WAN_GAME_STOP:
                return "wan-game-pause";
            case CAT_WAN_GAME_END:
                return "wan-game-end";
            case CAT_WAN_PROFILE_ADD:
                return "wan-profile-add";
            case CAT_WAN_PROFILE_RESET:
                return "wan-profile-reset";
            case CAT_WAN_PROFILE_VALIDATE:
                return "wan-profile-validate";
            case CAT_WAN_PROFILE_ACTIVATE:
                return "wan-profile-activate";
            case CAT_CANCEL_ACTION:
                return "cancel action";
            case CAT_PLAYER_UPDATE:
                return "player update " + getPlayerSettings();
            case CAT_WAN_PROFILE_SEND_PASSWORD:
                return "wan-profile-send";
            case CAT_WAN_PROFILE_CHANGE_PASSWORD:
                return "wan-profile-change-password";
            case CAT_WAN_PROFILE_SYNC_PASSWORD:
                return "wan-profile-sync-password";
            case CAT_CONNECTION:
                return "connection-status";
            case CAT_CHANGE_TABLE:
                return "change-table";
            case CAT_ALIVE:
                return "alive";

            case CAT_CLIENT_JOIN:
                return "client-join";
            case CAT_PROCESS_PHASE:
                return "process-phase " + getPhaseName();

            default:
                return "Undefined cat (" + getCategory() + ')';
        }
    }

    /**
     * for debugging, cat plus size of data
     */
    public String toStringCategorySize()
    {
        return toStringCategory() + " - " + data_.toStringSize();
    }

    // used to store data in underlying DDMessage
    public static final String ON_MSG_ID = "msgid";
    public static final String ON_IN_REPLY_TO = "replyto";
    public static final String ON_CONNECTION_URL = "url";
    public static final String ON_UDPID = "udpid";
    public static final String ON_PLAYER_NAME = "playername";
    public static final String ON_PLAYER_PROFILE_PATH = "profile";
    public static final String ON_GAME_ID = "gameid";
    public static final String ON_PASSWORD = "pass";
    public static final String ON_GUID = "guid";
    public static final String ON_CHAT = "chat";
    public static final String ON_CHAT_TYPE = "chattype";
    public static final String ON_PLAYER_INFO = "playerinfo";
    public static final String ON_PLAYER_LIST = "playerlist";
    public static final String ON_FROM = "from"; // used in chat, hand action, quit, (any message from a particular player)
    public static final String ON_TABLE = "table"; // used in chat to indicate table number chat is going to & change table
    public static final String ON_PHASE_NAME = "phasename";
    public static final String ON_PHASE_PARAMS = "phaseparams";
    public static final String ON_HAND_ACTION = "handaction";
    public static final String ON_RUN_PROCESS_TABLE = "run-pt";
    public static final String ON_HAND_ACTION_CC = "handactioncc";
    public static final String ON_POKER_TABLE_EVENTS = "events";
    public static final String ON_CASH = "cash";
    public static final String ON_CHIPS = "chips";
    public static final String ON_LEVEL = "level";
    public static final String ON_PENDING = "pending";
    public static final String ON_OBSERVE = "observe";
    public static final String ON_OFFSET = "offset";
    public static final String ON_COUNT = "count";
    public static final String ON_MODE = "mode";
    public static final String ON_WAN_AUTH = "auth";
    public static final String ON_WAN_PROFILE = "profile";
    public static final String ON_WAN_GAME = "game";
    public static final String ON_WAN_GAMES = "games";
    public static final String ON_WAN_HISTORIES = "histories";
    public static final String ON_PLAYER_SETTINGS = "settings";
    public static final String ON_DEMO = "demo";
    public static final String ON_ONLINE_ACTIVATED = "online";
    public static final String ON_CONNECTED = "connected";
    public static final String ON_PAUSE_CLOCK = "pauseclock";
    public static final String ON_RECONNECT = "reconnect";

    // no table/player set
    public static final int NO_TABLE = -1;
    public static final int NO_PLAYER = -1;

    // used in chat to indicate a "dealer/host" info message, different than -1 above
    public static final int CHAT_DIRECTOR_MSG_ID = -2;
    public static final int CHAT_DEALER_MSG_ID = -3;


    // transient info
    private PokerConnection connection_;

    // id (sequence)
    private static int ID = 0;

    private static synchronized int getNextID()
    {
        ID++;
        return ID;
    }

    /**
     * Creates a new instance of OnlineMessage
     * with the DDMessage source data
     */
    public OnlineMessage(DDMessage data)
    {
        this(data, null);
    }

    /**
     * Creates a new instance of OnlineMessage
     * with the DDMessage source data and the socket channel
     * it was received on
     */
    public OnlineMessage(DDMessage data, PokerConnection c)
    {
        data_ = data;
        ApplicationError.assertNotNull(data_, "data cannot be null", null);
        connection_ = c;
    }

    /**
     * Create a new instance of OnlineMessage
     * that is empty
     */
    public OnlineMessage(int nCategory)
    {
        data_ = new DDMessage(nCategory);
        setMessageID(getNextID());
    }

    /**
     * Socket this message came in on
     */
    public PokerConnection getConnection()
    {
        return connection_;
    }

    @Override
    public String toString()
    {
        return "OnlineMessage: " + data_;
    }

    public String toStringNoData()
    {
        return "OnlineMessage: " + data_.toString(false);
    }

    public DDMessage getData()
    {
        return data_;
    }

    public void setGameData(String data)
    {
        data_.addData(data);
    }

    public byte[] getGameData()
    {
        return data_.getData();
    }

    public String getKey()
    {
        return data_.getKey();
    }

    public int getCategory()
    {
        return data_.getCategory();
    }

    public String getApplicationErrorMessage()
    {
        return data_.getApplicationErrorMessage();
    }

    public void setApplicationErrorMessage(String sMsg)
    {
        data_.setApplicationErrorMessage(sMsg);
    }

    public String getApplicationStatusMessage()
    {
        return data_.getApplicationStatusMessage();
    }

    public void setApplicationStatusMessage(String sMsg)
    {
        data_.setApplicationStatusMessage(sMsg);
    }

    public String getPlayerName()
    {
        return data_.getString(ON_PLAYER_NAME);
    }

    public void setPlayerName(String s)
    {
        data_.setString(ON_PLAYER_NAME, s);
    }

    public boolean isPlayerDemo()
    {
        return data_.getBoolean(ON_DEMO, false);
    }

    public void setPlayerDemo(boolean b)
    {
        data_.setBoolean(ON_DEMO, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isOnlineActivated()
    {
        return data_.getBoolean(ON_ONLINE_ACTIVATED, false);
    }

    public void setOnlineActivated(boolean b)
    {
        data_.setBoolean(ON_ONLINE_ACTIVATED, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isPlayerConnected()
    {
        return data_.getBoolean(ON_CONNECTED, false);
    }

    public void setPlayerConnected(boolean b)
    {
        data_.setBoolean(ON_CONNECTED, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public Boolean isClockPaused()
    {
        return data_.getBoolean(ON_PAUSE_CLOCK);
    }

    public void setClockPaused(Boolean b)
    {
        data_.setBoolean(ON_PAUSE_CLOCK, b);
    }

    public String getPlayerProfilePath()
    {
        return data_.getString(ON_PLAYER_PROFILE_PATH);
    }

    public void setPlayerProfilePath(String s)
    {
        data_.setString(ON_PLAYER_PROFILE_PATH, s);
    }

    public String getGameID()
    {
        return data_.getString(ON_GAME_ID);
    }

    public void setGameID(String s)
    {
        data_.setString(ON_GAME_ID, s);
    }

    public String getPassword()
    {
        return data_.getString(ON_PASSWORD);
    }

    public void setPassword(String s)
    {
        data_.setString(ON_PASSWORD, s);
    }

    public String getGUID()
    {
        return data_.getString(ON_GUID);
    }

    public void setGUID(String s)
    {
        data_.setString(ON_GUID, s);
    }

    public String getPlayerSettings()
    {
        return data_.getString(ON_PLAYER_SETTINGS);
    }

    public void setPlayerSettings(String s)
    {
        data_.setString(ON_PLAYER_SETTINGS, s);
    }

    public String getChat()
    {
        return data_.getString(ON_CHAT);
    }

    public void setChat(String s)
    {
        data_.setString(ON_CHAT, s);
    }

    public int getChatType()
    {
        return data_.getInteger(ON_CHAT_TYPE, PokerConstants.CHAT_ALWAYS);
    }

    public void setChatType(int type)
    {
        data_.setInteger(ON_CHAT_TYPE, type);
    }

    public int getFromPlayerID()
    {
        return data_.getInteger(ON_FROM, NO_PLAYER);
    }

    public void setFromPlayerID(int id)
    {
        data_.setInteger(ON_FROM, id);
    }

    public int getTableNumber()
    {
        return data_.getInteger(ON_TABLE, NO_TABLE);
    }

    public void setTableNumber(int id)
    {
        data_.setInteger(ON_TABLE, id);
    }

    public PokerURL getConnectURL()
    {
        String url = data_.getString(ON_CONNECTION_URL);
        return (url == null) ? null : new PokerURL(url);
    }

    public void setConnectURL(PokerURL url)
    {
        if (url == null) return;
        data_.setString(ON_CONNECTION_URL, url.toString());
    }

    public UDPID getUPDID()
    {
        String id = data_.getString(ON_UDPID);
        return (id == null) ? null : new UDPID(id);
    }

    public void setUPDID(UDPID id)
    {
        if (id == null) return;
        data_.setString(ON_UDPID, id.toString());
    }

    public int getMessageID()
    {
        return data_.getInteger(ON_MSG_ID, 0);
    }

    private void setMessageID(int id)
    {
        data_.setInteger(ON_MSG_ID, id);
    }

    public int getInReplyTo()
    {
        return data_.getInteger(ON_IN_REPLY_TO, 0);
    }

    public void setInReplyTo(int id)
    {
        data_.setInteger(ON_IN_REPLY_TO, id);
    }

    public String getPhaseName()
    {
        return data_.getString(ON_PHASE_NAME);
    }

    public void setPhaseName(String s)
    {
        data_.setString(ON_PHASE_NAME, s);
    }

    public DMTypedHashMap getPhaseParams()
    {
        return (DMTypedHashMap) data_.getObject(ON_PHASE_PARAMS);
    }

    public void setPhaseParams(DMTypedHashMap params)
    {
        data_.setObject(ON_PHASE_PARAMS, params);
    }

    public Object getHandAction()
    {
        return data_.getObject(ON_HAND_ACTION);
    }

    public void setHandAction(Object ha)
    {
        data_.setObject(ON_HAND_ACTION, ha);
    }

    public void setHandActionCC(boolean b)
    {
        data_.setBoolean(ON_HAND_ACTION_CC, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isHandActionCC()
    {
        return data_.getBoolean(ON_HAND_ACTION_CC, false);
    }

    public void setRunProcessTable(boolean b)
    {
        data_.setBoolean(ON_RUN_PROCESS_TABLE, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isRunProcessTable()
    {
        return data_.getBoolean(ON_RUN_PROCESS_TABLE, false);
    }

    public void setPokerTableEvents(DMArrayList<? extends DataMarshal> events)
    {
        data_.setList(ON_POKER_TABLE_EVENTS, events);
    }

    public DMArrayList<? extends DataMarshal> getPokerTableEvents()
    {
        return (DMArrayList<? extends DataMarshal>) data_.getList(ON_POKER_TABLE_EVENTS);
    }

    public int getCash()
    {
        return data_.getInteger(ON_CASH, 0);
    }

    public void setCash(int id)
    {
        data_.setInteger(ON_CASH, id);
    }

    public int getChips()
    {
        return data_.getInteger(ON_CHIPS, 0);
    }

    public void setChips(int id)
    {
        data_.setInteger(ON_CHIPS, id);
    }

    public int getLevel()
    {
        return data_.getInteger(ON_LEVEL, 0);
    }

    public void setLevel(int level)
    {
        data_.setInteger(ON_LEVEL, level);
    }

    public void setPending(boolean b)
    {
        data_.setBoolean(ON_PENDING, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isPending()
    {
        return data_.getBoolean(ON_PENDING, false);
    }

    public void setObserve(boolean b)
    {
        data_.setBoolean(ON_OBSERVE, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isObserve()
    {
        return data_.getBoolean(ON_OBSERVE, false);
    }

    public void setReconnect(boolean b)
    {
        data_.setBoolean(ON_RECONNECT, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isReconnect()
    {
        return data_.getBoolean(ON_RECONNECT, false);
    }

    public int getOffset()
    {
        return data_.getInteger(ON_OFFSET, -1);
    }

    public void setOffset(int offset)
    {
        data_.setInteger(ON_OFFSET, offset);
    }

    public int getCount()
    {
        return data_.getInteger(ON_COUNT, -1);
    }

    public void setCount(int count)
    {
        data_.setInteger(ON_COUNT, count);
    }

    public int getMode()
    {
        return data_.getInteger(ON_MODE, -1);
    }

    public void setMode(int mode)
    {
        data_.setInteger(ON_MODE, mode);
    }

    public DMTypedHashMap getWanAuth()
    {
        return (DMTypedHashMap) data_.getObject(ON_WAN_AUTH);
    }

    public void setWanAuth(DMTypedHashMap m)
    {
        data_.setObject(ON_WAN_AUTH, m);
    }

    public DMTypedHashMap getOnlineProfileData()
    {
        return (DMTypedHashMap) data_.getObject(ON_WAN_PROFILE);
    }

    public void setOnlineProfileData(DMTypedHashMap m)
    {
        data_.setObject(ON_WAN_PROFILE, m);
    }

    public DMTypedHashMap getWanGame()
    {
        return (DMTypedHashMap) data_.getObject(ON_WAN_GAME);
    }

    public void setWanGame(DMTypedHashMap m)
    {
        data_.setObject(ON_WAN_GAME, m);
    }

    public DMArrayList<DMTypedHashMap> getWanGames()
    {
        return (DMArrayList<DMTypedHashMap>) data_.getList(ON_WAN_GAMES);
    }

    public void setWanGames(DMArrayList<DMTypedHashMap> l)
    {
        data_.setList(ON_WAN_GAMES, l);
    }

    public DMArrayList<? extends DataMarshal> getWanHistories()
    {
        return (DMArrayList<? extends DataMarshal>) data_.getList(ON_WAN_HISTORIES);
    }

    public void setWanHistories(DMArrayList<? extends DataMarshal> l)
    {
        data_.setList(ON_WAN_HISTORIES, l);
    }

    public OnlinePlayerInfo getPlayerInfo()
    {
        return new OnlinePlayerInfo((DMTypedHashMap) data_.getObject(ON_PLAYER_INFO));
    }

    public void setPlayerInfo(OnlinePlayerInfo info)
    {
        data_.setObject(ON_PLAYER_INFO, info.getData());
    }

    /**
     * Get player list as array of OnlinePlayerInfo (constructed each time, so caller should cache)
     */
    public List<OnlinePlayerInfo> getPlayerList()
    {
        DMArrayList<DMTypedHashMap> raw = (DMArrayList<DMTypedHashMap>) data_.getList(ON_PLAYER_LIST);
        List<OnlinePlayerInfo> list = new ArrayList<OnlinePlayerInfo>(raw.size());
        for (DMTypedHashMap map : raw)
        {
            list.add(new OnlinePlayerInfo(map));
        }
        return list;
    }

    /**
     * Store OnlinePlayerInfo list as DMArrayList of DMTypedHashMap
     */
    public void setPlayerList(DMArrayList<?> l)
    {
        data_.setList(ON_PLAYER_LIST, l);
    }
}
