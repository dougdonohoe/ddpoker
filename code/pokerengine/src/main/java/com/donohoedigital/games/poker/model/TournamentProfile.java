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
 * TournamentProfile.java
 *
 * Created on January 27, 2004, 9:26 AM
 */

package com.donohoedigital.games.poker.model;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.xml.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static com.donohoedigital.config.DebugConfig.*;

/**
 * @author donohoe
 */
@DataCoder('X')
public class TournamentProfile extends BaseProfile implements DataMarshal, SimpleXMLEncodable
{
    static Logger logger = LogManager.getLogger(TournamentProfile.class);

    // defines
    public static final String PROFILE_BEGIN = "tourney";
    public static final String TOURNAMENT_DIR = "tournaments";

    // MAX value
    public static final int MAX_LEVELS = 40;
    public static final int MAX_SPOTS = 560;
    public static final int MIN_SPOTS = 10;
    public static final double MAX_SPOTS_PERCENT = .3333333d;

    // note on max players - if this changes above 6000, need to change
    // ids for territories in gameboard.xml and adjust PokerInit starting IDs
    public static final int MAX_PLAYERS = 5625;
    public static final int MAX_ONLINE_PLAYERS = 30; // limit online to 3 tables; TODO: is this too few?
    public static final int MAX_OBSERVERS = 10;

    public static final int MAX_CHIPS = TESTING(PokerConstants.TESTING_LEVELS) ? 10000000 : 50000;
    public static final int MAX_REBUY_CHIPS = TESTING(PokerConstants.TESTING_LEVELS) ? 10000000 : 1000000;
    public static final int MAX_BUY = TESTING(PokerConstants.TESTING_LEVELS) ? 10000000 : 50000;
    public static final int MAX_BLINDANTE = 100000000;
    public static final int MAX_MINUTES = 120;
    public static final int MAX_HOUSE_PERC = 25;
    public static final int MAX_HOUSE_AMOUNT = 9999;
    public static final int MAX_REBUYS = 99;
    public static final int MAX_PERC = 100;
    public static final int MAX_MAX_RAISES = 9;
    public static final int MAX_AI_RAISES = 4;
    public static final int BREAK_ANTE_VALUE = -1;
    public static final int MIN_TIMEOUT = 5;
    public static final int MAX_TIMEOUT = 60; // stored in poker player, absolute max is 214
    public static final int MAX_THINKBANK = 60; // stored in poker player, absolute max is 999
    public static final int MAX_BOOT_HANDS = 100;
    public static final int MIN_BOOT_HANDS = 5;
    public static final int ROUND_MULT = 1000; // 3 decimal places

    // formatting TODO: need to change %/$ formatting if localizing
    private static final MessageFormat FORMAT_PERC = new MessageFormat("{0}%");
    private static final MessageFormat FORMAT_AMOUNT = new MessageFormat("${0}");

    // saved members
    private DMTypedHashMap map_;

    // param names
    public static final String PARAM_DESC = "desc";
    public static final String PARAM_GREETING = "greeting";
    public static final String PARAM_NUM_PLAYERS = "defplayers";
    public static final String PARAM_TABLE_SEATS = "tableseats";
    public static final String PARAM_MINPERLEVEL_DEFAULT = "minperlevel";
    public static final String PARAM_MINUTES = "minutes";
    public static final String PARAM_BUYIN = "buyin";
    public static final String PARAM_BUYINCHIPS = "buyinchips";
    public static final String PARAM_DEMO = "demo";
    public static final String PARAM_SMALL = "small";
    public static final String PARAM_BIG = "big";
    public static final String PARAM_ANTE = "ante";
    public static final String PARAM_PAYOUT = "payout";
    public static final String PARAM_PAYOUTPERC = "payoutperc";
    public static final String PARAM_PAYOUTNUM = "payoutnum";
    public static final String PARAM_ALLOC = "alloc";
    public static final String PARAM_PRIZEPOOL = "prizepool";
    public static final String PARAM_HOUSE = "house";
    public static final String PARAM_HOUSEPERC = "houseperc";
    public static final String PARAM_HOUSEAMOUNT = "houseamount";
    public static final String PARAM_SPOTAMOUNT = "spotamount";
    public static final String PARAM_DOUBLE = "double";
    public static final String PARAM_REBUYS = "rebuys";
    public static final String PARAM_REBUYCOST = "rebuycost";
    public static final String PARAM_REBUYCHIPS = "rebuychips";
    public static final String PARAM_REBUY_UNTIL = "rebuyuntil";
    public static final String PARAM_MAXREBUYS = "maxrebuys";
    public static final String PARAM_ADDONS = "addons";
    public static final String PARAM_ADDONCOST = "addoncost";
    public static final String PARAM_ADDONCHIPS = "addonchips";
    public static final String PARAM_ADDONLEVEL = "addonlevel";
    public static final String PARAM_LASTLEVEL = "lastlevel";
    public static final String PARAM_MIX = "mix:";
    public static final String PARAM_REBUYEXPR = "rebuyexpr";
    public static final String PARAM_REBUYCHIPCNT = "rebuychipcnt";
    public static final String PARAM_MAXRAISES = "maxraises";
    public static final String PARAM_MAXRAISES_NONE_HEADSUP = "maxheadsup";
    public static final String PARAM_TIMEOUT = "timeout";
    public static final String PARAM_FILL_COMPUTER = "fillai";
    public static final String PARAM_ALLOW_DASH = "allowdash";
    public static final String PARAM_ALLOW_ADVISOR = "allowadvisor";
    public static final String PARAM_ALLOW_DEMO = "allowdemo";
    public static final String PARAM_ONLINE_ACTIVATED_ONLY = "onlineactonly";
    public static final String PARAM_THINKBANK = "thinkbank";
    public static final String PARAM_MAX_OBSERVERS = "maxobservers";
    public static final String PARAM_BOOT_SITOUT = "bootsitout";
    public static final String PARAM_BOOT_SITOUT_COUNT = "bootsitoutcount";
    public static final String PARAM_BOOT_DISCONNECT = "bootdisconnect";
    public static final String PARAM_BOOT_DISCONNECT_COUNT = "bootdisconnectcount";
    public static final String PARAM_PLAYERS = "players";
    public static final String PARAM_INVITE_ONLY = "inviteonly";
    public static final String PARAM_INVITEES = "invitees";
    public static final String PARAM_INVITE_OBS = "publicobs";
    public static final String PARAM_GAMETYPE = "gametype";
    public static final String PARAM_GAMETYPE_DEFAULT = "gametypedefault";
    public static final String DATA_ELEMENT_GAMETYPE = "gameType";
    public static final String PARAM_UPDATE = "update";

    /**
     * Empty constructor for loading from data
     */
    public TournamentProfile()
    {
        this("");
    }

    /**
     * Load profile from string file
     */
    public TournamentProfile(String sFile, boolean bFull)
    {
        super(sFile, bFull);
    }

    /**
     * Load profile from file
     */
    public TournamentProfile(File file, boolean bFull)
    {
        super(file, bFull);
    }

    /**
     * New profile with given name
     */
    public TournamentProfile(String sName)
    {
        super(sName);
        map_ = new DMTypedHashMap();

        // init starting values
        setNumPlayers(10);
        setMinutesPerLevel(30);
        setBuyin(100);
        setBuyinChips(1000);
        setRebuyChipCount(1000);
        setRebuys(false);
        setAddons(false);
        setPayout(PokerConstants.PAYOUT_SPOTS);
        setPayoutSpots(3);
        setOnlineActivatedPlayersOnly(true); // default to true for new tournaments        
        fixAll();
    }

    /**
     * New profile copied from given profile, using new name
     */
    public TournamentProfile(TournamentProfile tp, String sName)
    {
        super(sName);
        map_ = new DMTypedHashMap();
        map_.putAll(tp.map_);
    }

    /**
     * Get begin part of profile name
     */
    @Override
    protected String getBegin()
    {
        return PROFILE_BEGIN;
    }

    /**
     * Get name of directory to store profiles in
     */
    @Override
    protected String getProfileDirName()
    {
        return TOURNAMENT_DIR;
    }

    /**
     * Get profile list
     */
    @Override
    protected List<BaseProfile> getProfileFileList()
    {
        return getProfileList();
    }

    /**
     * Get map
     */
    public DMTypedHashMap getMap()
    {
        return map_;
    }

    /**
     * Set update date as now
     */
    public void setUpdateDate()
    {
        map_.setLong(PARAM_UPDATE, System.currentTimeMillis());
    }

    /**
     * Get create date
     */
    public long getUpdateDate()
    {
        return map_.getLong(PARAM_UPDATE, getCreateDate());
    }

    /**
     * Set demo
     */
    public void setDemo(boolean b)
    {
        map_.setBoolean(PARAM_DEMO, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * is demo?
     */
    boolean isDemo()
    {
        return map_.getBoolean(PARAM_DEMO, false);
    }

    /**
     * Get description
     */
    public String getDescription()
    {
        return map_.getString(PARAM_DESC, "");
    }

    /**
     * Get greeting, replacing variable $name with given sName.
     */
    public String getGreeting(String sName)
    {
        String sGreeting = map_.getString(PARAM_GREETING, "").trim();
        if (sGreeting.length() == 0) return null;
        sGreeting = Utils.replace(sGreeting, "\\$name", sName);
        return sGreeting;
    }

    /**
     * Get num players
     */
    public int getNumPlayers()
    {
        return map_.getInteger(PARAM_NUM_PLAYERS, 0, 2, MAX_PLAYERS);
    }

    /**
     * Set num players
     */
    public void setNumPlayers(int n)
    {
        map_.setInteger(PARAM_NUM_PLAYERS, n);
    }

    /**
     * Get num online players - minimum of getNumPlayers() and MAX_ONLINE_PLAYERS
     */
    public int getMaxOnlinePlayers()
    {
        return Math.min(getNumPlayers(), MAX_ONLINE_PLAYERS);
    }

    /**
     * Set player list
     */
    public void setPlayers(List<String> players)
    {
        DMArrayList<String> list = (DMArrayList<String>) map_.getList(PARAM_PLAYERS);
        if (list == null)
        {
            list = new DMArrayList<String>();
            map_.setList(PARAM_PLAYERS, list);
        }
        else
        {
            list.clear();
        }

        for (String name : players)
        {
            list.add(name);
        }

        // change update date so it updates in LAN clients
        setUpdateDate();
    }

    /**
     * Get player list
     */
    public List<String> getPlayers()
    {
        DMArrayList<String> players = (DMArrayList<String>) map_.getList(PARAM_PLAYERS);
        if (players == null) players = new DMArrayList<String>();
        return players;
    }

    /**
     * is invite only?
     */
    public boolean isInviteOnly()
    {
        return map_.getBoolean(PARAM_INVITE_ONLY, false);
    }

    /**
     * Set invite only
     */
    public void setInviteOnly(boolean b)
    {
        map_.setBoolean(PARAM_INVITE_ONLY, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * is invite only?
     */
    public boolean isInviteObserversPublic()
    {
        return map_.getBoolean(PARAM_INVITE_OBS, false);
    }

    /**
     * Set public observers
     */
    public void setInviteObserversPublic(boolean b)
    {
        map_.setBoolean(PARAM_INVITE_OBS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get invitee player list
     */
    public AbstractPlayerList getInvitees()
    {
        return new InviteePlayerList(this);
    }

    /**
     * PlayerList which stores data in TournamentProfile
     */
    private static class InviteePlayerList extends AbstractPlayerList
    {
        TournamentProfile profile;

        private InviteePlayerList(TournamentProfile profile)
        {
            this.profile = profile;
            fetch();
        }

        @Override
        public String getName()
        {
            return "invited";
        }

        @Override
        protected void saveNames(String sNames)
        {
            profile.map_.setString(PARAM_INVITEES, sNames);
        }

        @Override
        protected String fetchNames()
        {
            return profile.map_.getString(PARAM_INVITEES, null);
        }

        @Override
        protected void saveKeys(String sKeys)
        { /* no keys */ }

        @Override
        protected String fetchKeys()
        {
            return null;
        }
    }

    /**
     * Get max players at a table
     */
    public int getSeats()
    {
        return map_.getInteger(PARAM_TABLE_SEATS, PokerConstants.SEATS, 2, PokerConstants.SEATS);
    }

    /**
     * Get buyin
     */
    public int getBuyinCost()
    {
        return map_.getInteger(PARAM_BUYIN, 0, 1, MAX_BUY);
    }

    /**
     * set buyin
     */
    public void setBuyin(int n)
    {
        map_.setInteger(PARAM_BUYIN, n);
    }

    /**
     * Get buyin chips
     */
    public int getBuyinChips()
    {
        return map_.getInteger(PARAM_BUYINCHIPS, 0, 1, MAX_CHIPS);
    }

    /**
     * set buyin chips
     */
    public void setBuyinChips(int n)
    {
        map_.setInteger(PARAM_BUYINCHIPS, n);
    }

    /**
     * Get rebuy chip cnt
     */
    public int getRebuyChipCount()
    {
        return map_.getInteger(PARAM_REBUYCHIPCNT, getBuyinChips(), 0, MAX_REBUY_CHIPS);
    }

    /**
     * set rebuy chip count
     */
    public void setRebuyChipCount(int n)
    {
        map_.setInteger(PARAM_REBUYCHIPCNT, n);
    }

    /**
     * Get rebuy expression
     */
    public int getRebuyExpressionType()
    {
        return map_.getInteger(PARAM_REBUYEXPR, PokerConstants.REBUY_LTE,
                               PokerConstants.REBUY_LT, PokerConstants.REBUY_LTE);
    }

    /**
     * Get rebuy expression
     */
    public void setRebuyExpression(int n)
    {
        map_.setInteger(PARAM_REBUYEXPR, n);
    }

    /**
     * remove entries for level
     */
    public void clearLevel(int nLevel)
    {
        map_.remove(PARAM_ANTE + nLevel);
        map_.remove(PARAM_SMALL + nLevel);
        map_.remove(PARAM_BIG + nLevel);
        map_.remove(PARAM_MINUTES + nLevel);
        map_.remove(PARAM_GAMETYPE + nLevel);
    }

    /**
     * Get small blind
     */
    public int getSmallBlind(int nLevel)
    {
        return getAmount(PARAM_SMALL, nLevel);
    }

    /**
     * Get big blind
     */
    public int getBigBlind(int nLevel)
    {
        return getAmount(PARAM_BIG, nLevel);
    }

    /**
     * Get ante
     */
    public int getAnte(int nLevel)
    {
        return getAmount(PARAM_ANTE, nLevel);
    }

    /**
     * Get last small blind - if current level is a break, returns
     * first prior non-break level
     */
    public int getLastSmallBlind(int nLevel)
    {
        while (isBreak(nLevel) && nLevel > 0)
        {
            nLevel--;
        }
        return getSmallBlind(nLevel);
    }

    /**
     * Get last big blind - if current level is a break, returns
     * first prior non-break level
     */
    public int getLastBigBlind(int nLevel)
    {
        while (isBreak(nLevel) && nLevel > 0)
        {
            nLevel--;
        }
        return getBigBlind(nLevel);
    }

    /**
     * Get last ante - if current level is a break, returns
     * first prior non-break level
     */
    public int getLastAnte(int nLevel)
    {
        while (isBreak(nLevel) && nLevel > 0)
        {
            nLevel--;
        }
        return getAnte(nLevel);
    }

    /**
     * Get default time limit
     */
    public int getDefaultMinutesPerLevel()
    {
        return map_.getInteger(PARAM_MINPERLEVEL_DEFAULT, 0, 1, MAX_MINUTES);
    }

    /**
     * Set default level minutes
     */
    public void setMinutesPerLevel(int n)
    {
        map_.setInteger(PARAM_MINPERLEVEL_DEFAULT, n);
    }

    /**
     * Get minutes in level
     */
    public int getMinutes(int nLevel)
    {
        int nAmount = getAmountFromString(PARAM_MINUTES + nLevel, false);
        if (nAmount == 0)
        {
            nAmount = getDefaultMinutesPerLevel();
        }
        int nMax = MAX_MINUTES;
        if (isDemo()) nMax = TESTING(EngineConstants.TESTING_DEMO) ? 1 : 5;
        if (nAmount > nMax) nAmount = nMax;
        return nAmount;
    }

    /**
     * Get default game type
     */
    public String getDefaultGameTypeString()
    {
        return map_.getString(PARAM_GAMETYPE_DEFAULT, PokerConstants.DE_NO_LIMIT_HOLDEM);
    }

    /**
     * Get string version of game type
     */
    public String getGameTypeString(int nLevel)
    {
        String sType = map_.getString(PARAM_GAMETYPE + nLevel);
        if (sType == null || sType.length() == 0)
        {
            sType = getDefaultGameTypeString();
        }
        return sType;
    }

    /**
     * Get game type display for level, returns blank if type equals default.
     */
    public String getGameTypeDisplay(int i)
    {
        String sGame = getGameTypeString(i);
        if (sGame.equals(getDefaultGameTypeString()))
        {
            return "";
        }
        else
        {
            return DataElement.getDisplayValue(DATA_ELEMENT_GAMETYPE, sGame);
        }
    }

    /**
     * Get game type for level, returning an int (see PokerContants)
     */
    public int getGameType(int nLevel)
    {
        String sType = getGameTypeString(nLevel);

        if (sType.equals(PokerConstants.DE_NO_LIMIT_HOLDEM))
        {
            return PokerConstants.TYPE_NO_LIMIT_HOLDEM;
        }
        else if (sType.equals(PokerConstants.DE_POT_LIMIT_HOLDEM))
        {
            return PokerConstants.TYPE_POT_LIMIT_HOLDEM;
        }
        if (sType.equals(PokerConstants.DE_LIMIT_HOLDEM))
        {
            return PokerConstants.TYPE_LIMIT_HOLDEM;
        }

        throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Unknown poker game type", sType, null);
    }

    /**
     * is given level a break?
     */
    public boolean isBreak(int nLevel)
    {
        return getAmountFromString(PARAM_ANTE + nLevel, true) == BREAK_ANTE_VALUE;
    }

    /**
     * Set given level as a break
     */
    public void setBreak(int nLevel, int nMinutes)
    {
        map_.setString(PARAM_ANTE + nLevel, Integer.toString(BREAK_ANTE_VALUE));
        map_.setString(PARAM_MINUTES + nLevel, Integer.toString(nMinutes));
        map_.remove(PARAM_SMALL + nLevel);
        map_.remove(PARAM_BIG + nLevel);
        map_.remove(PARAM_GAMETYPE + nLevel);
    }

    /**
     * Get max raises
     */
    public int getMaxRaises(int nNumWithCards, boolean isComputer)
    {
        if (nNumWithCards <= 2 && isRaiseCapIgnoredHeadsUp())
        {
            // cap ai players at 4 so they don't raise each other indefinitely
            if (isComputer) return MAX_AI_RAISES;
            return Integer.MAX_VALUE;
        }

        int nMax = (isComputer) ? MAX_AI_RAISES : MAX_MAX_RAISES;
        return map_.getInteger(PARAM_MAXRAISES, 3, 1, nMax);
    }

    /**
     * Observe max raises when heads-up?
     */
    public boolean isRaiseCapIgnoredHeadsUp()
    {
        return map_.getBoolean(PARAM_MAXRAISES_NONE_HEADSUP, true);
    }

    /**
     * Get level amount
     */
    private int getAmount(String sName, int nLevel)
    {
        ApplicationError.assertTrue(!isBreak(nLevel), "Attempting to get value for a break level", sName);
        int nAmount;
        int nLast = getLastLevel();
        if (nLevel > nLast)
        {
            while (isBreak(nLast) && nLast > 0)
            {
                nLast--;
                nLevel--;
            }
            nAmount = getAmountFromString(sName + nLast, false);
            if (isDoubleAfterLastLevel())
            {
                //old clever way before we had to check for max int
                //nAmount *= Math.pow(2, (nLevel - nLast));

                // double until we go over MAX int
                long l = nAmount;
                for (int i = 0; i < (nLevel - nLast); i++)
                {
                    l *= 2;
                    if (l >= MAX_BLINDANTE)
                    {
                        l /= 2;
                        break;
                    }
                }
                nAmount = (int) l;

            }
            return round(nAmount);
        }
        else
        {
            return getAmountFromString(sName + nLevel, false);
        }
    }

    /**
     * set payout type
     */
    public void setPayout(int nType)
    {
        map_.setInteger(PARAM_PAYOUT, nType);
    }

    /**
     * Get payout type
     */
    public int getPayoutType()
    {
        return map_.getInteger(PARAM_PAYOUT, PokerConstants.PAYOUT_PERC,
                               PokerConstants.PAYOUT_SPOTS, PokerConstants.PAYOUT_SATELLITE);
    }

    /**
     * Set payout spots
     */
    public void setPayoutSpots(int n)
    {
        map_.setInteger(PARAM_PAYOUTNUM, n);
    }

    /**
     * Set payout spots
     */
    public void setPayoutPercent(int n)
    {
        map_.setInteger(PARAM_PAYOUTPERC, n);
    }

    /**
     * Get spots to payout
     */
    private int getPayoutSpots()
    {
        return map_.getInteger(PARAM_PAYOUTNUM, 3, 1, MAX_SPOTS);
    }

    /**
     * get percent of spots to payout
     */
    public int getPayoutPercent()
    {
        return map_.getInteger(PARAM_PAYOUTPERC, 5, 1, MAX_PERC);
    }

    /**
     * Update num players, adjust payout if necessary
     */
    public void updateNumPlayers(int nNumPlayers)
    {
        boolean bChange = false;

        int nType = getPayoutType();
        if (nType == PokerConstants.PAYOUT_PERC)
        {
            int spot = getPayoutPercent();
            int max = getMaxPayoutPercent(nNumPlayers);
            if (spot > max)
            {
                bChange = true;
                setPayoutPercent(max);
            }
        }
        else if (nType == PokerConstants.PAYOUT_SPOTS)
        {
            int spot = getPayoutSpots();
            int max = getMaxPayoutSpots(nNumPlayers);
            if (spot > max)
            {
                bChange = true;
                setPayoutSpots(max);
            }
        }
        else // PokerConstants.PAYOUT_SATELLITE
        {
            // no need to update if num players change
        }

        // store new num players
        setNumPlayers(nNumPlayers);

        // if a change in payout spots occurred, update
        if (bChange)
        {
            if (isAllocFixed() || isAllocPercent())
            {
                setAlloc(PokerConstants.ALLOC_AUTO);
            }
        }

        // if auto alloc, update spots
        if (isAllocAuto())
        {
            setAutoSpots();
        }

        // fix all (html may have changed, remove old spots)
        fixAll();
    }

    /**
     * Return number of payout spots
     */
    public int getNumSpots()
    {
        int nRet;
        int nType = getPayoutType();
        if (nType == PokerConstants.PAYOUT_PERC)
        {
            int spot = getPayoutPercent();
            nRet = (int) Math.ceil((((double) spot) / 100d) * (double) getNumPlayers());
        }
        else if (nType == PokerConstants.PAYOUT_SPOTS)
        {
            nRet = getPayoutSpots();
        }
        else // PokerConstants.PAYOUT_SATELLITE
        {
            int nPrize = getPrizePool();
            int nAmount = getSatellitePayout();
            if (nAmount == 0)
            {
                nRet = 1;
            }
            else
            {
                nRet = nPrize / nAmount;
                int nExtra = nPrize % nAmount;
                if (nExtra > 0) nRet++;
            }
        }

        // always ensure 1 spot paid out
        if (nRet == 0) nRet = 1;

        return nRet;
    }

    /**
     * In isAllocSatellite() case, this returns
     * the amount each spot gets
     */
    public int getSatellitePayout()
    {
        return (int) getSpot(1);
    }

    /**
     * Set alloc type
     */
    public void setAlloc(int nType)
    {
        map_.setInteger(PARAM_ALLOC, nType);
    }

    /**
     * Return if pool is auto allocated
     */
    public boolean isAllocAuto()
    {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO,
                                                      PokerConstants.ALLOC_AUTO,
                                                      PokerConstants.ALLOC_AMOUNT) == PokerConstants.ALLOC_AUTO;
    }

    /**
     * Return if pool is perc allocated
     */
    public boolean isAllocPercent()
    {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO,
                                                      PokerConstants.ALLOC_AUTO,
                                                      PokerConstants.ALLOC_AMOUNT) == PokerConstants.ALLOC_PERC;
    }

    /**
     * Return if pool is fixed amount allocated
     */
    public boolean isAllocFixed()
    {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC,
                                                      PokerConstants.ALLOC_AUTO,
                                                      PokerConstants.ALLOC_AUTO,
                                                      PokerConstants.ALLOC_AMOUNT) == PokerConstants.ALLOC_AMOUNT;
    }

    /**
     * Return if pool is satellite allocated
     */
    public boolean isAllocSatellite()
    {
        return getPayoutType() == PokerConstants.PAYOUT_SATELLITE;
    }

    /**
     * Get prize pool amount
     */
    public int getPrizePool()
    {
        // pool - get amount set during a tournament,
        int nPool = map_.getInteger(PARAM_PRIZEPOOL, -1);
        if (nPool != -1)
        {
            return nPool;
        }

        // or if not set yet, estimate from number of players
        int nTotalPool = getNumPlayers() * getBuyinCost();
        return getPoolAfterHouseTake(nTotalPool);
    }

    /**
     * Set actual prize pool (updates spots if auto-allocated).
     */
    public void setPrizePool(int nPool, boolean bAdjustForHouseTake)
    {
        if (bAdjustForHouseTake) nPool = getPoolAfterHouseTake(nPool);
        map_.setInteger(PARAM_PRIZEPOOL, nPool);
        if (isAllocAuto()) setAutoSpots();
    }

    /**
     * Get house take
     */
    public int getPoolAfterHouseTake(int nPool)
    {
        int nNumPlayers = getNumPlayers();
        if (getHouseCutType() == PokerConstants.HOUSE_PERC)
        {
            nPool -= (((double) getHousePercent()) / 100d) * (double) nPool;
        }
        else
        {
            nPool -= getHouseAmount() * nNumPlayers;
        }

        return nPool;
    }

    /**
     * return type of house cut
     */
    public int getHouseCutType()
    {
        return map_.getInteger(PARAM_HOUSE, PokerConstants.HOUSE_PERC, PokerConstants.HOUSE_AMOUNT, PokerConstants.HOUSE_PERC);
    }

    /**
     * get house percent integer (0-100)
     */
    public int getHousePercent()
    {
        return map_.getInteger(PARAM_HOUSEPERC, 0, 0, MAX_HOUSE_PERC);
    }

    /**
     * Get house cut amount
     */
    public int getHouseAmount()
    {
        return map_.getInteger(PARAM_HOUSEAMOUNT, 0, 0, MAX_HOUSE_AMOUNT);
    }

    /**
     * Get true buyin (less house cost) - this is used to figure out the multiple
     * to use for minimum payouts
     */
    public int getTrueBuyin()
    {
        int nType = getHouseCutType();
        int buy = getBuyinCost();
        if (nType == PokerConstants.HOUSE_AMOUNT)
        {
            buy -= getHouseAmount();
        }
        return buy;
    }

    /**
     * Get max number of spots for given number of players
     */
    public int getMaxPayoutSpots(int nNumPlayers)
    {
        int nMax = (int) (nNumPlayers * MAX_SPOTS_PERCENT);
        nMax = Math.min(nMax, MAX_SPOTS);
        if (nMax < MIN_SPOTS) nMax = MIN_SPOTS;
        if (nMax > nNumPlayers) nMax = nNumPlayers;

        return nMax;
    }

    /**
     * Get max percetage of spots
     */
    public int getMaxPayoutPercent(int nNumPlayers)
    {
        int nMax = 0;
        if (nNumPlayers > 0)
        {
            nMax = (Math.min(MAX_SPOTS, getMaxPayoutSpots(nNumPlayers))) * 100 / nNumPlayers;
        }

        return nMax;
    }

    /**
     * Set automatic spot percentages
     */
    public void setAutoSpots()
    {
        int nFinalSpots = 10; // top ten finishers use fibbo math

        int nPool = getPrizePool();
        int nNumSpots = getNumSpots();
        int nNonFinal = nNumSpots - nFinalSpots;
        int amount[] = new int[nNumSpots];

        int nMin = getTrueBuyin();
        // add a rebuy to min in actual tournament calculation
        // as suggested by "Tex" - we do this if the
        // pool has had enough rebuys to cover each spot
        if (nPool >= (getPoolAfterHouseTake(getBuyinCost() * getNumPlayers()) + (nNumSpots * getRebuyCost())))
        {
            nMin += getRebuyCost();
        }

        int nAllocdPool = 0;
        int nIndex = 0;
        int nAlloc;

        int nRound;
        if (nMin < 100) nRound = 1;
        else if (nMin < 500) nRound = 10;
        else if (nMin <= 1000) nRound = 100;
        else if (nMin <= 5000) nRound = 500;
        else if (nMin <= 10000) nRound = 1000;
        else nRound = 5000;

        double inc = .5d;
        double mult;
        if (nNonFinal > 0)
        {
            // estimate total pool - if non-final payouts are too
            // high, lower increment paid until total is in desired
            // range
            int nMinBottom = (int) (nFinalSpots / MAX_SPOTS_PERCENT); // based on max percentange
            // we won't be in here unless at 30+ players
            double dLow = .01d;
            double dHigh = .33d;
            // range for non-final portion of the pool is 1% at 30 players to 33% at max players
            double dRange = dLow + ((dHigh - dLow) * (getNumPlayers() - nMinBottom) / (double) (MAX_PLAYERS - nMinBottom));
            double dMinRange = ((nMin * nNonFinal) / (double) nPool);
            if (dRange < dMinRange) dRange = dMinRange;

            //logger.debug("Range: " + (dRange* 100));
            while (true)
            {
                int nFull = nNonFinal / PokerConstants.SEATS;
                int nExtra = nNonFinal % PokerConstants.SEATS;
                int nInc = (int) (nMin * inc);
                if (nInc == 0) nInc = 1;
                // this formula:
                //
                // #seats (10) * num full tables * min payout (buyin) +
                // #seats (10) * sum (1 .. full) * incremental payout
                double nEst = (PokerConstants.SEATS * nFull * nMin) +
                              (PokerConstants.SEATS * ((nFull * (nFull + 1)) / 2) * nInc);
                int nFinalInc = (int) (nMin * (inc * (nFull + 1)));

                // add payout to extra players
                nEst += nExtra * (nMin + nFinalInc);
                if (nEst / nPool <= dRange)
                {
                    break;
                }
                else
                {
                    inc -= .05d;
                    if (inc <= 0)
                    {
                        inc = 0;
                        break;
                    }
                }
            }

            mult = 1.0d + inc;

            while (nNonFinal > 0)
            {
                nAlloc = (int) (nMin * mult);
                if ((nAlloc % nRound) > 0)
                {
                    nAlloc = nAlloc - (nAlloc % nRound) + nRound;
                }
                amount[nIndex] = nAlloc;
                nAllocdPool += nAlloc;
                nIndex++;
                nNonFinal--;

                if (nIndex % PokerConstants.SEATS == 0 && nNonFinal > 0)
                {
                    mult += inc;
                }
            }
        }
        else
        {
            mult = 1.0d;
        }
        int nLeft = nNumSpots - nIndex;
        int sum;
        int fibo[] = new int[Math.max(2, nNumSpots)];

        // STEP 1: do fibonnaci sequence
        fibo[0] = 2;
        fibo[1] = 3;
        sum = fibo[0] + fibo[1];
        for (int i = 2; i < nLeft; i++)
        {
            fibo[i] = fibo[i - 1] + fibo[i - 2];
            sum += fibo[i];
        }

        // STEP 2: compute percentage
        int nPoolLeft = nPool - nAllocdPool;
        nMin *= mult;
        int nSplit = nPoolLeft / nLeft;
        if (nMin >= (nSplit * .8)) nMin = 0;
        if (nMin == 0) nRound = 1;

        //logger.debug("min: " + nMin + " round: " + nRound + " nSplit: " + nSplit + " nPoolLeft: " + nPoolLeft + " nLeft: " + nLeft + " mult: " + mult);

        if ((nMin % nRound) > 0)
        {
            nMin = nMin - (nMin % nRound) + nRound;
        }

        //logger.debug("min: "+ nMin);

        double perc;
        for (int i = 0; i < nLeft - 1; i++)
        {
            perc = (double) fibo[i] / (double) sum;
            nAlloc = (int) (nPoolLeft * perc);

            if (nAlloc < nMin)
            {
                nAlloc = nMin;
            }
            else
            {
                if ((nAlloc % nRound) > 0)
                {
                    nAlloc = nAlloc - (nAlloc % nRound) + nRound;
                }
            }

            amount[nIndex] = nAlloc;
            nAllocdPool += nAlloc;
            nIndex++;

        }
        amount[nNumSpots - 1] = nPool - nAllocdPool;

        // odd case where #1 is less than #2 - swap
        if (nNumSpots > 1 && amount[nNumSpots - 1] < amount[nNumSpots - 2])
        {
            int swap = amount[nNumSpots - 1];
            amount[nNumSpots - 1] = amount[nNumSpots - 2];
            amount[nNumSpots - 2] = swap;
        }

        // set values
        String text;
        for (int i = 0; i < MAX_SPOTS; i++)
        {
            if (i >= nNumSpots)
            {
                map_.removeString(PARAM_SPOTAMOUNT + (i + 1));
            }
            else
            {
                text = FORMAT_AMOUNT.format(new Object[]{amount[amount.length - i - 1]});
                map_.setString(PARAM_SPOTAMOUNT + (i + 1), text);
            }
        }
    }

    /**
     * Get value of payout spot
     */
    public double getSpot(int nNum)
    {
        return getSpotFromString(PARAM_SPOTAMOUNT + nNum);
    }

    /**
     * Get value of payout spot as string
     */
    public String getSpotAsString(int nNum)
    {
        return map_.getString(PARAM_SPOTAMOUNT + nNum, "");
    }

    /**
     * Get payout based on spot
     */
    public int getPayout(int nNum)
    {
        // safety check
        int nNumSpots = getNumSpots();
        if (nNum < 0 || nNum > nNumSpots) return 0; // BUG 315

        // prize pool
        int nPrizePool = getPrizePool();

        // satellite alloc
        if (isAllocSatellite())
        {
            int nSatSpot = getSatellitePayout();

            // last spot gets any remaining amount
            int nExtra = nPrizePool % nSatSpot;
            if (nNum == nNumSpots && nExtra != 0)
            {
                return nExtra;
            }

            if (nPrizePool < nSatSpot)
            {
                nSatSpot = nPrizePool;
            }

            // TODO: possible minor bug: left over amounts if rebuy period continues after payouts started
            return nSatSpot;
        }
        else
        {
            double spot = getSpot(nNum);
            if (isAllocPercent())
            {
                // top spot gets prize pool less amount paid to other
                // spots to account for rounding/fractional error
                if (nNum == 1)
                {
                    int nTotal = 0;
                    for (int i = 2; i <= nNumSpots; i++)
                    {
                        nTotal += getPayout(i);
                    }

                    // TODO: possible minor bug: incorrect amounts if rebuy period continues after payout started
                    return nPrizePool - nTotal;
                }
                return (int) (nPrizePool * spot / 100);
            }
            else
            {
                return (int) spot;
            }
        }
    }

    /**
     * Get integer from string, throws exception if not there,
     * used for items stored as strings
     */
    private int getAmountFromString(String sName, boolean allowNegative)
    {
        String s = map_.getString(sName);
        if (s == null || s.length() == 0) return 0;

        int n = Integer.parseInt(s);
        if (!allowNegative && n < 0) n = 0;
        if (n > MAX_BLINDANTE) n = MAX_BLINDANTE;

        return n;
    }

    /**
     * Get double from string, throws exception if not there,
     * used for items stored as strings
     */
    private double getSpotFromString(String sName)
    {
        String s = map_.getString(sName);
        double ret = Utils.parseStringToDouble(s, ROUND_MULT);

        if (ret < 0) ret = 0;
        if (ret > MAX_BLINDANTE) ret = MAX_BLINDANTE;
        return ret;
    }

    /**
     * Get whether an online game is filled with ai players
     */
    public boolean isFillComputer()
    {
        return map_.getBoolean(PARAM_FILL_COMPUTER, true);
    }

    /**
     * Get whether an online game allows demo players
     */
    public boolean isAllowDemo()
    {
        return map_.getBoolean(PARAM_ALLOW_DEMO, true);
    }

    /**
     * Get whether an online game only allows online activated players
     */
    public boolean isOnlineActivatedPlayersOnly()
    {
        // Added 3.0p3 - defaults to false since new option
        return map_.getBoolean(PARAM_ONLINE_ACTIVATED_ONLY, false);
    }

    /**
     * set online activated
     */
    public void setOnlineActivatedPlayersOnly(boolean onlineActivatedPlayersOnly)
    {
        map_.setBoolean(PARAM_ONLINE_ACTIVATED_ONLY, onlineActivatedPlayersOnly);
    }

    /**
     * Get whether an online game allows dashboard usage
     */
    public boolean isAllowDash()
    {
        return map_.getBoolean(PARAM_ALLOW_DASH, false);
    }

    /**
     * Get whether an online game allows advisor usage
     */
    public boolean isAllowAdvisor()
    {
        return map_.getBoolean(PARAM_ALLOW_ADVISOR, false);
    }

    /**
     * Get whether an online game boots sitout players
     */
    public boolean isBootSitout()
    {
        return map_.getBoolean(PARAM_BOOT_SITOUT, false);
    }

    /**
     * Get whether an online game boots disconnected players
     */
    public boolean isBootDisconnect()
    {
        return map_.getBoolean(PARAM_BOOT_DISCONNECT, true);
    }

    /**
     * get boot sitout count
     */
    public int getBootSitoutCount()
    {
        return map_.getInteger(PARAM_BOOT_SITOUT_COUNT, 25, MIN_BOOT_HANDS, MAX_BOOT_HANDS);
    }

    /**
     * get boot disconnect count
     */
    public int getBootDisconnectCount()
    {
        return map_.getInteger(PARAM_BOOT_DISCONNECT_COUNT, 10, MIN_BOOT_HANDS, MAX_BOOT_HANDS);
    }

    /**
     * Get whether the blinds double after last level
     */
    public boolean isDoubleAfterLastLevel()
    {
        return map_.getBoolean(PARAM_DOUBLE, true);
    }

    /**
     * Get whether there are rebuys
     */
    public boolean isRebuys()
    {
        return map_.getBoolean(PARAM_REBUYS, false);
    }

    /**
     * set whether there are rebuys
     */
    public void setRebuys(boolean b)
    {
        map_.setBoolean(PARAM_REBUYS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get rebuy cost
     */
    public int getRebuyCost()
    {
        return map_.getInteger(PARAM_REBUYCOST, 0, 1, MAX_BUY);
    }

    /**
     * Get rebuy chips
     */
    public int getRebuyChips()
    {
        return map_.getInteger(PARAM_REBUYCHIPS, 0, 1, MAX_CHIPS);
    }

    /**
     * Get last rebuy level
     */
    public int getLastRebuyLevel()
    {
        return map_.getInteger(PARAM_REBUY_UNTIL, 0, 1, MAX_LEVELS);
    }

    /**
     * Get max rebuys
     */
    public int getMaxRebuys()
    {
        return map_.getInteger(PARAM_MAXREBUYS, 0, 0, MAX_REBUYS);
    }

    /**
     * Get whether there are addons
     */
    public boolean isAddons()
    {
        return map_.getBoolean(PARAM_ADDONS, false);
    }

    /**
     * set whether there are addons
     */
    public void setAddons(boolean b)
    {
        map_.setBoolean(PARAM_ADDONS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get addon cost
     */
    public int getAddonCost()
    {
        return map_.getInteger(PARAM_ADDONCOST, 0, 1, MAX_BUY);
    }

    /**
     * Get rebuy chips
     */
    public int getAddonChips()
    {
        return map_.getInteger(PARAM_ADDONCHIPS, 0, 1, MAX_CHIPS);
    }

    /**
     * Get add on level
     */
    public int getAddonLevel()
    {
        return map_.getInteger(PARAM_ADDONLEVEL, 0, 1, MAX_LEVELS);
    }

    /**
     * get online player timeout for acting
     */
    public int getTimeoutSeconds()
    {
        return map_.getInteger(PARAM_TIMEOUT, 30, MIN_TIMEOUT, MAX_TIMEOUT);
    }

    /**
     * get player thinkbank for acting
     */
    public int getThinkBankSeconds()
    {
        return map_.getInteger(PARAM_THINKBANK, 15, 0, MAX_THINKBANK);
    }

    /**
     * get maximum number of observers
     */
    public int getMaxObservers()
    {
        if (DebugConfig.isTestingOn()) return 30;
        return map_.getInteger(PARAM_MAX_OBSERVERS, 5, 0, MAX_OBSERVERS);
    }


    /**
     * Fix levels, eliminating missing rows, filling in missing
     * blinds
     */
    public void fixLevels()
    {
        int nLevel = 0;
        int nNonBreakLevel = 0;
        int nAnte, nSmall, nBig, nMinutes;
        String sAnte, sSmall, sBig, sMinutes, sType;
        String sSmallL = null, sBigL = null;
        String a, s, b, m, g;
        boolean bUpdate;
        boolean bBreak;

        for (int i = 1; i <= MAX_LEVELS; i++)
        {
            bUpdate = false;
            a = PARAM_ANTE + i;
            s = PARAM_SMALL + i;
            b = PARAM_BIG + i;
            m = PARAM_MINUTES + i;
            g = PARAM_GAMETYPE + i;
            sAnte = map_.getString(a, "");
            sSmall = map_.getString(s, "");
            sBig = map_.getString(b, "");
            sMinutes = map_.getString(m, "");
            sType = map_.getString(g, "");
            bBreak = false;

            try
            {
                nAnte = Integer.parseInt(sAnte);
                if (nAnte == 0)
                {
                    sAnte = "";
                    map_.setString(a, "");
                }
                if (nAnte == BREAK_ANTE_VALUE) bBreak = true;
            }
            catch (NumberFormatException ignored)
            {
                sAnte = "";
                map_.setString(a, "");
            }
            try
            {
                nSmall = Integer.parseInt(sSmall);
                if (nSmall == 0)
                {
                    sSmall = "";
                    map_.setString(s, "");
                }
            }
            catch (NumberFormatException ignored)
            {
                sSmall = "";
                map_.setString(s, "");
            }
            try
            {
                nBig = Integer.parseInt(sBig);
                if (nBig == 0)
                {
                    sBig = "";
                    map_.setString(b, "");
                }
            }
            catch (NumberFormatException ignored)
            {
                sBig = "";
                map_.setString(b, "");
            }
            try
            {
                nMinutes = Integer.parseInt(sMinutes);
                if (nMinutes == 0)
                {
                    sMinutes = "";
                    map_.setString(m, "");
                }
            }
            catch (NumberFormatException ignored)
            {
                sMinutes = "";
                map_.setString(m, "");
            }

            if (sAnte.length() == 0 && sSmall.length() == 0 && sBig.length() == 0) continue;

            // increment level (we have a valid level)
            nLevel++;
            if (!bBreak) nNonBreakLevel++;

            if (sBig.length() == 0 && sBigL != null && !bBreak)
            {
                sBig = sBigL;
                bUpdate = true;
            }

            if (sSmall.length() == 0 && sSmallL != null && !bBreak)
            {
                sSmall = sSmallL;
                bUpdate = true;
            }

            if (nLevel != i)
            {
                map_.setString(PARAM_ANTE + nLevel, sAnte);
                map_.setString(PARAM_MINUTES + nLevel, sMinutes);

                if (bBreak)
                {
                    map_.remove(PARAM_SMALL + nLevel);
                    map_.remove(PARAM_BIG + nLevel);
                    map_.remove(PARAM_GAMETYPE + nLevel);
                }
                else
                {
                    map_.setString(PARAM_SMALL + nLevel, sSmall);
                    map_.setString(PARAM_BIG + nLevel, sBig);
                    map_.setString(PARAM_GAMETYPE + nLevel, sType);
                }
                map_.setString(a, "");
                map_.setString(s, "");
                map_.setString(b, "");
                map_.setString(m, "");
                map_.setString(g, "");
            }
            else if (bUpdate)
            {
                map_.setString(s, sSmall);
                map_.setString(b, sBig);
            }

            if (!bBreak)
            {
                sSmallL = sSmall;
                sBigL = sBig;
            }
        }

        // if user defined no levels, set level 1
        if (nNonBreakLevel == 0)
        {
            nLevel++;
            map_.setString(PARAM_SMALL + (nLevel), "1");
            map_.setString(PARAM_BIG + (nLevel), "2");
        }

        // record (needed in getAnte() et al)
        map_.setInteger(PARAM_LASTLEVEL, nLevel);

        // now verify amounts and cleanup      
        int nAnteP = 0, nSmallP = 0, nBigP = 0;
        int nFrac;
        int minutesPerLevel = getDefaultMinutesPerLevel();
        String sGameTypeDefault = getDefaultGameTypeString();
        for (int i = 1; i <= nLevel; i++)
        {
            if (isBreak(i))
            {
                // always leave minutes for break
                // leave ante value as is
                continue;
            }

            nAnte = getAnte(i);
            nSmall = getSmallBlind(i);
            nBig = getBigBlind(i);
            sType = getGameTypeString(i);
            nMinutes = getMinutes(i);
            if (nMinutes == minutesPerLevel) nMinutes = 0; // equals default, so don't save
            if (sType.equals(sGameTypeDefault)) sType = null; // equals default, so don't save

            if (i == 1)
            {
                if (nSmall == 0)
                {
                    if (nBig > 2) nSmall = nBig / 2;
                    else nSmall = 1;
                }
                if (nBig == 0) nBig = nSmall * 2;
            }

            // adjust if non-zero
            if (nAnte != 0 || nSmall != 0 || nBig != 0)
            {
                // big should be bigger than small
                if (nBig < nSmall) nBig = nSmall;

                // validate it is >= last round
                // (except ante, which can go back to 0)
                if (i > 1)
                {
                    if (nAnte < nAnteP && nAnte != 0) nAnte = nAnteP;
                    if (nSmall < nSmallP) nSmall = nSmallP;
                    if (nBig < nBigP) nBig = nBigP;
                }

                // ante should be at least 5% of small blind
                // but not bigger than the small blind
                nFrac = (int) (nSmall * .05f);
                if (nAnte != 0)
                {
                    if (nAnte < nFrac) nAnte = nFrac;
                    if (nAnte > nSmall) nAnte = nSmall;
                }

                // round
                nAnte = round(nAnte);
                nSmall = round(nSmall);
                nBig = round(nBig);
            }

            if (nMinutes > MAX_MINUTES) nMinutes = MAX_MINUTES;

            // update - set new value or remove completely if 0
            if (nAnte > 0) map_.setString(PARAM_ANTE + i, "" + nAnte);
            else map_.removeString(PARAM_ANTE + i);
            if (nSmall > 0) map_.setString(PARAM_SMALL + i, "" + nSmall);
            else map_.removeString(PARAM_SMALL + i);
            if (nBig > 0) map_.setString(PARAM_BIG + i, "" + nBig);
            else map_.removeString(PARAM_BIG + i);
            if (nMinutes > 0) map_.setString(PARAM_MINUTES + i, "" + nMinutes);
            else map_.removeString(PARAM_MINUTES + i);
            if (sType != null) map_.setString(PARAM_GAMETYPE + i, sType);
            else map_.removeString(PARAM_GAMETYPE + i);

            nAnteP = (nAnte == 0 ? nAnteP : nAnte); // don't store ante if its set to 0 (keep at last value)
            nSmallP = nSmall;
            nBigP = nBig;
        }

        // cleanup remaining levels
        for (int i = nLevel + 1; i <= MAX_LEVELS; i++)
        {
            map_.removeString(PARAM_ANTE + i);
            map_.removeString(PARAM_SMALL + i);
            map_.removeString(PARAM_BIG + i);
            map_.removeString(PARAM_MINUTES + i);
            map_.removeString(PARAM_GAMETYPE + i);
        }
    }

    /**
     * round ante/blind
     */
    private int round(int n)
    {
        int nRound;
        if (n <= 100) nRound = 1;
        else if (n <= 500) nRound = 5;
        else if (n <= 1000) nRound = 25;
        else if (n <= 10000) nRound = 100;
        else if (n <= 100000) nRound = 1000;
        else if (n <= 1000000) nRound = 10000;
        else nRound = 100000;

        int nRemain = n % nRound;
        n -= nRemain;
        if (nRound > 1 && nRemain >= (nRound / 2)) n += nRound;

        return n;
    }

    /**
     * Clean up alloc entries
     */
    private void fixAllocs()
    {
        int nNumSpots = getNumSpots();
        if (isAllocSatellite()) nNumSpots = 1; // only need 1 entry for satellite
        double d;
        String s;
        for (int i = 1; i <= nNumSpots; i++)
        {
            d = getSpot(i);
            if (isAllocPercent())
            {
                s = FORMAT_PERC.format(new Object[]{d});
            }
            else
            {
                s = FORMAT_AMOUNT.format(new Object[]{(int) d});
            }
            map_.setString(PARAM_SPOTAMOUNT + i, s);
        }

        // BUG 315 - clear out other spots
        for (int i = nNumSpots + 1; i <= MAX_SPOTS; i++)
        {
            map_.removeString(PARAM_SPOTAMOUNT + i);
        }
    }

    /**
     * Return last defined level (assumes fixLevels called)
     */
    public int getLastLevel()
    {
        return map_.getInteger(PARAM_LASTLEVEL, 0);
    }

    /**
     * set percent for given player type
     */
    public void setPlayerTypePercent(String sPlayerTypeUniqueId, int pct)
    {
        if (sPlayerTypeUniqueId == null) return;

        if (pct <= 0)
        {
            map_.remove(PARAM_MIX + sPlayerTypeUniqueId);
        }
        else
        {
            map_.setInteger(PARAM_MIX + sPlayerTypeUniqueId, pct);
        }
    }

    /**
     * get percent for given player type
     */
    public int getPlayerTypePercent(String sPlayerTypeUniqueId)
    {
        Integer pct = map_.getInteger(PARAM_MIX + sPlayerTypeUniqueId);

        if (pct == null)
        {
            return 0;
        }
        else
        {
            return pct;
        }
    }

    /**
     * Does game have any limit levels?
     */
    public boolean hasLimitLevels()
    {
        if (getDefaultGameTypeString().equals(PokerConstants.DE_LIMIT_HOLDEM)) return true;

        for (int i = getLastLevel(); i > 0; i--)
        {
            if (getGameTypeString(i).equals(PokerConstants.DE_LIMIT_HOLDEM)) return true;
        }

        return false;
    }

    /**
     * to string for logging
     */
    @Override
    public String toString()
    {
        return getName();
    }

    ////
    //// Saved tournaments
    ////

    /**
     * allow editing of all tournaments, even pre-shipped ones
     */
    @Override
    public boolean canEdit()
    {
        return true;
    }

    /**
     * save - override to consolidate levels first
     */
    @Override
    public void save()
    {
        fixAll();
        super.save();
    }

    /**
     * fixstuff
     */
    public void fixAll()
    {
        fixLevels();
        fixAllocs();

        // rebuys if < 0, change to <=
        if (getRebuyExpressionType() == PokerConstants.REBUY_LT &&
            getRebuyChipCount() == 0)
        {
            setRebuyExpression(PokerConstants.REBUY_LTE);
        }
    }

    /**
     * subclass implements to load its contents from the given reader
     */
    @Override
    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        map_ = new DMTypedHashMap();
        map_.demarshal(null, buf.readLine());
    }

    /**
     * subclass implements to save its contents to the given writer
     */
    @Override
    public void write(Writer writer) throws IOException
    {
        super.write(writer);

        writer.write(map_.marshal(null));
        writeEndEntry(writer);
    }


    /**
     * Get list of save files in save directory
     */
    public static List<BaseProfile> getProfileList()
    {
        return BaseProfile.getProfileList
                (TOURNAMENT_DIR, Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), TournamentProfile.class, false);
    }

    ////
    //// DataMarshal 
    ////

    public void demarshal(MsgState state, String sData)
    {
        StringReader reader = new StringReader(sData);
        try
        {
            read(reader, true);
        }
        catch (IOException io)
        {
            throw new ApplicationError(io);
        }
    }

    public String marshal(MsgState state)
    {
        StringWriter writer = new StringWriter();
        try
        {
            write(writer);
        }
        catch (IOException io)
        {
            throw new ApplicationError(io);
        }
        return writer.toString();
    }

    ////
    //// XML Encoding
    ////

    public void encodeXML(SimpleXMLEncoder encoder)
    {
        encoder.setCurrentObject(this, "tournamentFormat");
        encoder.addAllTagsExcept("map", "fileNum", "file", "dir", "fileName", "lastModified", "createDate", "updateDate", "invitees", "players");

        // levels
        encoder.setCurrentObject("levels");
        for (int i = 1; i <= getLastLevel(); i++)
        {
            encoder.setCurrentObject("level");

            encoder.addTag("number", i);
            encoder.addTag("minutes", getMinutes(i));

            if (isBreak(i))
            {
                encoder.addTag("break", true);
            }
            else
            {
                encoder.addTag("gameType", getGameTypeString(i));
                encoder.addTag("ante", getAnte(i));
                encoder.addTag("small", getBigBlind(i));
                encoder.addTag("big", getSmallBlind(i));
            }

            encoder.finishCurrentObject();
        }
        encoder.finishCurrentObject(); // levels

        // payouts
        encoder.setCurrentObject("prizes");
        for (int i = 1; i <= getNumSpots(); i++)
        {
            encoder.setCurrentObject("prize");

            encoder.addTag("place", i);
            encoder.addTag("amount", getSpotAsString(i));

            encoder.finishCurrentObject(); // prize
        }
        encoder.finishCurrentObject(); // prizes

        // invitees
        encoder.setCurrentObject("invitees");
        for (AbstractPlayerList.PlayerInfo player : getInvitees())
        {
            encoder.addTag("player", player.getName());
        }
        encoder.finishCurrentObject(); // invitees

        // players
        encoder.setCurrentObject("players");
        for (String player : getPlayers())
        {
            encoder.addTag("player", player);
        }
        encoder.finishCurrentObject(); // players

        encoder.finishCurrentObject(); // tournament
    }
}
