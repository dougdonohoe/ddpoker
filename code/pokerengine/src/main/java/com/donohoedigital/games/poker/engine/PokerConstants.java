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
 * PokerConstants.java
 *
 * Created on Decmeber 7, 2003, 7:18 PM
 */

package com.donohoedigital.games.poker.engine;

import com.donohoedigital.base.Format;
import com.donohoedigital.comms.Version;
import com.donohoedigital.games.config.GamePlayer;
import com.donohoedigital.p2p.P2PURL;

/**
 * @author Doug Donohoe
 */
public class PokerConstants
{
    // version of Poker
    //public static Version VERSION = new Version(1, 0, true, 8, false, 0, false); // beta
    //public static Version VERSION = new Version(1, 0, false, 0, true, 1, false); // demo 1
    //public static Version VERSION = new Version(1, 0, false); // release 1.0 CD
    //public static Version VERSION = new Version(1, 0, true); // release 1.0 download
    //public static Version VERSION = new Version(1, 1, false); // release 1.1 CD
    //public static Version VERSION = new Version(1, 1, true); // release 1.1 download
    //public static Version VERSION = new Version(1, 2, false, 0, true, 1, false); // release 1.2 demo 1
    //public static Version VERSION = new Version(1, 2, true); // release 1.2 - Patch 2
    //public static Version VERSION = new Version(Version.TYPE_BETA, 2, 0, 6, 4, true); // release 2.0 - Beta 6, Patch 4
    //public static final Version VERSION = new Version(2, 5, 3, true); // release 2.5 - Patch 3
    //public static final Version VERSION = new Version(3, 0, 5, true); // release 3.0, Patch 5
    //public static final Version VERSION = new Version(3, 0, 6, true); // release 3.0, Patch 6
    public static final Version VERSION = new Version(3, 1, 0, true); // release 3.1 (open sourced!)

    // OS versions (can be different if specific patches released)
    public static final Version LATEST_MAC = VERSION;
    public static final Version LATEST_LINUX = VERSION;
    public static final Version LATEST_WINDOWS = VERSION;

    // version alive check added - used for compat purposes
    public static final Version VERSION_ALIVE_CHECK_ADDED = new Version(2, 0, 3, true);
    public static final Version VERSION_ALIVE_LOBBY_ADDED = new Version(2, 0, 8, true);
    public static final Version VERSION_COUNTDOWN_CHANGED = new Version(2, 5, 0, true);
    public static final Version VERSION_HOST_CHECK_ADDED = new Version(3, 0, 0, true);

    // earliest version compat with current version
    // Was 2.1.1 until introduced profile validation
    public static final Version VERSION_LAST_COMPAT = new Version(3, 0, 4, true);

    // get start of license keys

    public static int getKeyStart()
    {
        return getKeyStart(VERSION);
    }

    // get start

    public static int getKeyStart(Version version)
    {
        if (version.isBeta() || version.isAlpha())
            return 22; // Poker Beta 1.0 used 20; Poker Alpha/Beta 2.0 used 19; Poker 3.0 didn't use alpha/beta
        else if (version.getMajor() == 1) return 21; // POKER (2nd game), Version 1
        else if (version.getMajor() == 2) return 22; // POKER (2nd game), Version 2
        else return 23; // POKER (2nd game), Version 3
    }

    // used with dummy profiles in wan_profile table
    public static final String DUMMY_PROFILE_KEY_START = "0000-0000-0000-000";

    // change start id for new objects
    // because poker can have 1000's of players (up to 5600+),
    // territories start at 6000 (see gameboard.xml),
    // observers start at 7000 and tables at 8000
    public static final int START_OBSERVER_ID = 7000;
    public static final int START_TABLE_ID = 8000;  // 5600+ / 2 per table = 2800+ tables.
    public static final int START_OTHER_ID = 11000; // so start OTHER_ID at 11000

    // format
    public static final Format fTimeNum1 = new Format("%1d");
    public static final Format fTimeNum2 = new Format("%02d");
    public static final Format fPerc = new Format("%3.2f");

    public static String formatPercent(double pct)
    {
        return fPerc.form(pct);
    }

    // game piece type numbers - order dictates order in which they are drawn
    public static final int PIECE_CARD = 5;
    public static final int PIECE_BUTTON = 10;
    public static final int PIECE_RESULTS = 15;

    // game options node and defines
    public static final String NODE_OPTION = "poker";

    public static final String OPTION_SHOW_PLAYER_TYPE = "showplayertype";
    public static final String OPTION_AUTO_CHECK_UPDATE = "autocheckupdate";
    public static final String OPTION_RIGHT_CLICK_ONLY = "rightclickonly";
    public static final String OPTION_DISABLE_SHORTCUTS = "disableshortcuts";
    public static final String OPTION_HOLE_CARDS_DOWN = "holecarddown";
    public static final String OPTION_PAUSE_ALLIN = "pauseallin";
    public static final String OPTION_PAUSE_COLOR = "pausecolor";
    public static final String OPTION_ZIP_MODE = "zipmode";
    public static final String OPTION_CHECKFOLD = "checkfold";
    public static final String OPTION_LARGE_CARDS = "largecards";
    public static final String OPTION_FOUR_COLOR_DECK = "fourcolordeck";
    public static final String OPTION_STYLIZED_FACE_CARDS = "stylized";
    public static final String OPTION_HANDS_PER_HOUR = "handsperhour";
    public static final String OPTION_DELAY = "delay";
    public static final String OPTION_AUTODEAL = "autodeal";
    public static final String OPTION_AUTODEALHAND = "autodealhand";
    public static final String OPTION_AUTODEALFOLD = "autodealfold";
    public static final String OPTION_AUTODEALONLINE = "autodealonline";
    public static final String OPTION_ONLINESTART = "onlinestart";
    public static final String OPTION_CLOCK_COLOUP = "clockcolorup";
    public static final String OPTION_CLOCK_PAUSE = "clockpause";
    public static final String OPTION_DEFAULT_ADVISOR = "defaultadvisor";
    public static final String OPTION_ONLINE_AUDIO = "onlineaudio";
    public static final String OPTION_ONLINE_FRONT = "onlinefront";
    public static final String OPTION_ONLINE_PAUSE = "onlinepause";
    public static final String OPTION_ONLINE_PAUSE_ALL_DISCONNECTED = "onlinepauseall";
    public static final String OPTION_ONLINE_UDP = "onlineudp";
    public static final String OPTION_ONLINE_COUNTDOWN = "countdown";
    public static final String OPTION_ONLINE_ENABLED = "onlineenabled";
    public static final String OPTION_ONLINE_SERVER = "onlineserver";
    public static final String OPTION_ONLINE_CHAT = "onlinechat";
    public static final String OPTION_SCREENSHOT_MAX_WIDTH = "screenshotmaxw";
    public static final String OPTION_SCREENSHOT_MAX_HEIGHT = "screenshotmaxh";

    public static final String OPTION_CHAT_PLAYERS = "chatplayers";
    public static final String OPTION_CHAT_OBSERVERS = "chatobservers";
    public static final String OPTION_CHAT_TIMEOUT = "chattimeout";
    public static final String OPTION_CHAT_DEALER = "chatdealer";
    public static final String OPTION_CHAT_DISPLAY = "chatdisplay";

    public static final String OPTION_CHEAT_POPUP = "popups";
    public static final String OPTION_CHEAT_SHOWWINNINGHAND = "showdown";
    public static final String OPTION_CHEAT_RABBITHUNT = "river";
    public static final String OPTION_CHEAT_MOUSEOVER = "mouseover";
    public static final String OPTION_CHEAT_AIFACEUP = "aifaceup";
    public static final String OPTION_CHEAT_SHOWFOLD = "showfold";
    public static final String OPTION_CHEAT_NEVERBROKE = "neverbroke";
    public static final String OPTION_CHEAT_PAUSECARDS = "pausecards";
    public static final String OPTION_CHEAT_MANUAL_BUTTON = "manualbutton";
    public static final String OPTION_CHEAT_SHOW_MUCKED = "showmuck";

    // other prefs
    public static final String PREF_DASHBOARD = "dashboard";
    public static final String PREF_LAST_JOIN = "lastjoin";

    // dealer chat options
    public static final int DEALER_NONE = 1;         // show none
    public static final int DEALER_NO_PLAYER_ACTION = 2; // show CHAT_1 only
    public static final int DEALER_ALL = 3;          // show CHAT_1 and CHAT_2

    // display chat options
    public static final int DISPLAY_ONE = 1;         // one chat window
    public static final int DISPLAY_TAB = 2;         // tabbed chat
    public static final int DISPLAY_SPLIT = 3;       // split chat

    // dealer chat levles
    public static final int CHAT_PRIVATE = -1;
    public static final int CHAT_ALWAYS = 0;
    public static final int CHAT_1 = 1;
    public static final int CHAT_2 = 2;
    public static final int CHAT_TIMEOUT = 3;

    // payout options
    public static final int PAYOUT_SPOTS = 1;
    public static final int PAYOUT_PERC = 2;
    public static final int PAYOUT_SATELLITE = 3;

    // house cut
    public static final int HOUSE_AMOUNT = 1;
    public static final int HOUSE_PERC = 2;

    // allocation
    public static final int ALLOC_AUTO = 1;
    public static final int ALLOC_PERC = 2;
    public static final int ALLOC_AMOUNT = 3;

    // rebuy expression
    public static final int REBUY_LT = 1;
    public static final int REBUY_LTE = 2;

    // game types
    public static final int TYPE_NO_LIMIT_HOLDEM = 1;
    public static final int TYPE_POT_LIMIT_HOLDEM = 2;
    public static final int TYPE_LIMIT_HOLDEM = 3;

    // game type strings (must match data-elements.xsd)
    public static final String DE_NO_LIMIT_HOLDEM = "nolimit";
    public static final String DE_POT_LIMIT_HOLDEM = "potlimit";
    public static final String DE_LIMIT_HOLDEM = "limit";

    // general
    public static final int PLAYER_ID_HOST = GamePlayer.HOST_ID;
    public static final int PLAYER_ID_TEMP = -1;

    // default number of seats in a table
    public static final int SEATS = 10;

    // pot actions
    public static final int NO_POT_ACTION = 0;
    public static final int CALLED_POT = 1;
    public static final int RAISED_POT = 2;
    public static final int RERAISED_POT = 3;

    // online
    public static final int MAX_PROFILES_PER_EMAIL = 3;
    public static final String ONLINE_GAME_PREFIX_TCP = "n-";
    public static final String ONLINE_GAME_PREFIX_UDP = "u-";
    public static final String URL_START = "poker" + P2PURL.PROTOCOL_DELIM;
    public static final String ID_PASS_DELIM = "/";
    public static final String REGEXP_DOLLAR_AMOUNT = "^\\$?[0-9\\,]*$";
    public static final String REGEXP_IP_ADDRESS =
            "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
    public static final String REGEXP_GAME_URL = URL_START +
                                                 REGEXP_IP_ADDRESS +
                                                 P2PURL.PORT_DELIM +
                                                 "\\d{1,5}" + // port
                                                 P2PURL.URI_DELIM +
                                                 '(' + ONLINE_GAME_PREFIX_TCP + '|' + ONLINE_GAME_PREFIX_UDP + ')' + "\\d{1,5}" + // gameid
                                                 ID_PASS_DELIM +
                                                 "[A-NP-Z]{3}-[1-9]{3}"; // password

    // join game file information
    public static final String CONTENT_TYPE_JOIN = "application/x-ddpoker-join";
    public static final String JOIN_FILE_EXT = "ddpokerjoin";
    public static final String JOIN_OBSERVER_QUERY = "?obs";

    // chat/info server
    @SuppressWarnings({"PublicStaticArrayField"})
    public static final byte CHAT_BYTES[] = {'6', 'e', 'h', 'g', '@', '!', 'T', 'A', 'Z', 'D', 'C', '%'};
    public static final byte USERTYPE_CHAT = 1;
    public static final byte USERTYPE_HELLO = 2;

    // admin chat types
    public static final byte CHAT_ADMIN_MSG = 0;
    public static final byte CHAT_ADMIN_JOIN = 1;
    public static final byte CHAT_ADMIN_LEAVE = 2;
    public static final byte CHAT_ADMIN_WELCOME = 3;
    public static final byte CHAT_ADMIN_ERROR = 4;

    // get string representation of chat admin type

    public static String toStringAdminType(int n)
    {
        switch (n)
        {
            case CHAT_ADMIN_MSG:
                return "message";
            case CHAT_ADMIN_JOIN:
                return "join";
            case CHAT_ADMIN_LEAVE:
                return "leave";
            case CHAT_ADMIN_WELCOME:
                return "welcome";
            case CHAT_ADMIN_ERROR:
                return "error";
            default:
                return "unknown-" + n;
        }
    }

    // misc
    public static final int PROFILE_RETRY_MILLIS = 3000;
    public static final int VERTICAL_SCREEN_FREE_SPACE = 200;

    // Debug
    public static final String TESTING_DOUG_CONTROLS_AI = "settings.debug.dougcontrolsai";
    public static final String TESTING_PAUSE_AI = "settings.debug.pauseai";
    public static final String TESTING_DEBUG_ADVISOR = "settings.debug.advisordebug";
    public static final String TESTING_AI_ALWAYS_CALLS = "settings.debug.aialwayscalls";
    public static final String TESTING_ADVISOR_VERBOSE = "settings.debug.advisorverbose";
    public static final String TESTING_DEBUG_POT = "settings.debug.pots";
    public static final String TESTING_LEVELS = "settings.debug.levels";
    public static final String TESTING_AUTOPILOT_INIT = "settings.debug.autopilot";
    public static final String TESTING_AUTOPILOT = "settings.debug.autopilot.on";
    public static final String TESTING_FAST_SAVE = "settings.debug.fastsave";
    public static final String TESTING_TEST_CASE = "settings.debug.testcase";
    public static final String TESTING_ONLINE_AUTO_DEAL_OFF = "settings.debug.onlineautodealoff";
    public static final String TESTING_ALLOW_SINGLE_PLAYER_ONLINE = "settings.debug.singleplayeronline";
    public static final String TESTING_ALLOW_CHEAT_ONLINE = "settings.debug.cheatonline";
    public static final String TESTING_CHAT_PERF = "settings.debug.chat.perf";
    public static final String TESTING_ALLOW_CHANGE_LEVEL = "settings.debug.changelevel";
    public static final String TESTING_MATRIX_POST_FLOP = "settings.debug.matrixpostflop";
    public static final String TESTING_LOG_AI = "settings.debug.logai";
    public static final String TESTING_HAND_WEIGHT_GRID = "settings.debug.handweightgrid";
    public static final String TESTING_CHAT_AI = "settings.debug.chatai";
    public static final String TESTING_SPLIT_HUMANS = "settings.debug.onlinesplithumans";
    public static final String TESTING_ONLINE_AI_NO_WAIT = "settings.debug.onlineainowait";
    public static final String TESTING_PROCESS_ALL_COMPUTER_TABLES = "settings.debug.processallaitables";
}
