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
 * PokerGame.java
 *
 * Created on December 30, 2003, 4:34 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.p2p.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.security.*;
import java.util.*;

/**
 * @author donohoe
 */
public class PokerGame extends Game implements PlayerActionListener
{
    static Logger logger = LogManager.getLogger(PokerGame.class);

    public static final int ACTION_FOLD = 1;
    public static final int ACTION_CHECK = 2;
    public static final int ACTION_CALL = 3;
    public static final int ACTION_BET = 4;
    public static final int ACTION_RAISE = 5;
    public static final int ACTION_ALL_IN = 6;
    public static final int ACTION_CONTINUE_LOWER = 7;
    public static final int ACTION_CONTINUE = 8;

    // home game save starts with
    public static final String HOME_BEGIN = "home";

    // denominations for chips
    private static final int nChipDenom_[] = new int[]{1, 5, 25, 100, 500, 1000, 5000, 10000, 50000, 100000};

    /**
     * Name used in PropertyChangeEvents when current table changed
     */
    public static final String PROP_CURRENT_TABLE = "_current_table_";

    /**
     * Name used in PropertyChangeEvents when current level changed
     */
    public static final String PROP_CURRENT_LEVEL = "_current_level_";

    /**
     * Name used in PropertyChangeEvents when profile changed
     */
    public static final String PROP_PROFILE = "_profile_";

    /**
     * Name used in PropertyChangeEvents when tables added/removed
     */
    public static final String PROP_TABLES = "_tables_";

    /**
     * Name used in PropertyChangeEvents when player busts out
     */
    public static final String PROP_PLAYER_FINISHED = "_busted_";

    // game info
    private DMArrayList<PokerTable> tables_ = new DMArrayList<PokerTable>();
    private TournamentProfile profile_;
    private int nLevel_ = 0;
    private boolean bClockMode_ = false;
    private boolean bSimulatorMode_ = false;
    private long id_;
    private int nMinChipIdx_ = 0;
    private int nLastMinChipIdx_ = 0;
    private int nExtraChips_ = 0;
    private int nClockCash_ = 0;

    // online game and other info added for 2.0
    private String sLocalIP_;
    private String sPublicIP_;
    private boolean bPublic_;
    private int nPort_;
    private int nOnlineMode_ = MODE_NONE;
    private PokerTable currentTable_;
    private int lastHandSaved_ = 0;
    private int nNumOut_ = 0;

    // clock object used to store seconds remaining, used in tournament/poker night manager
    private GameClock clock_ = new GameClock();

    // input mode
    public static final int MODE_NONE = -1;
    public static final int MODE_INIT = 0;
    public static final int MODE_REG = 1;
    public static final int MODE_PLAY = 2;
    public static final int MODE_CLIENT = 3;
    public static final int MODE_CANCELLED = 4;

    ////
    //// members below are transient (not saved)
    ////

    // total chips
    private int totalChipsInPlay_;
    private boolean deleteHandsAfterSaveDate_ = false;

    // UI bridge
    private PokerTableInput input_ = null;

    // online transient
    private OnlineManager onlineMgr_;
    private boolean bStartFromLobby_;

    /**
     *
     */
    public PokerGame()
    {
        super(GameEngine.getGameEngine().getDefaultContext()); // TODO: get right game
    }

    /**
     * empty constructor for loading
     *
     * @param context
     */
    public PokerGame(GameContext context)
    {
        super(context);
    }

    /**
     * Set whether the TD should run the start logic
     * to move clients from the lobby
     */
    public void setStartFromLobby(boolean b)
    {
        bStartFromLobby_ = b;
    }

    /**
     * Was this game started from the lobby
     */
    public boolean isStartFromLobby()
    {
        return bStartFromLobby_;
    }

    /**
     * Return default for tournaments and "home" for home games
     */
    @Override
    public String getBegin()
    {
        if (isClockMode()) return HOME_BEGIN;
        else return super.getBegin(); // handles on-line too
    }

    /**
     * Get description for save game
     */
    @Override
    public String getDescription()
    {
        if (isClockMode()) return profile_.getName();
        else if (isOnlineGame())
        {
            if (profile_ == null) return "";
            return PropertyConfig.getMessage("msg.savegame.desc.o", profile_.getName(), getNumPlayers());
        }
        else
        {
            PokerPlayer human = getHumanPlayer();
            Integer chips = getHumanPlayer().getChipCount();
            return PropertyConfig.getMessage("msg.savegame.desc", human.getName(), chips, profile_.getName());
        }
    }

    /**
     * Set clock mode
     */
    public void setClockMode(boolean b)
    {
        bClockMode_ = b;
    }

    /**
     * Is clock mode?
     */
    public boolean isClockMode()
    {
        return bClockMode_;
    }

    /**
     * Set simulator mode
     */
    public void setSimulatorMode(boolean b)
    {
        bSimulatorMode_ = b;
    }

    /**
     * Is simulator mode?
     */
    public boolean isSimulatorMode()
    {
        return bSimulatorMode_;
    }

    /**
     * set cash collected in home game
     */
    public void setClockCash(int n)
    {
        nClockCash_ = n;
    }

    /**
     * Get home cash
     */
    public int getClockCash()
    {
        return nClockCash_;
    }

    /**
     * Update profile for online game too (override completely so
     * prop change event happens after profile updated)
     */
    @Override
    public void addPlayer(GamePlayer player)
    {
        players_.add(player);
        updatePlayerList((PokerPlayer) player);
        firePropertyChange(PROP_PLAYERS, null, player);
    }

    /**
     * Update profile for online game too (override completely so
     * prop change event happens after profile updated)
     */
    @Override
    public void removePlayer(GamePlayer player)
    {
        players_.remove(player);
        updatePlayerList((PokerPlayer) player);
        firePropertyChange(PROP_PLAYERS, player, null);
    }

    /**
     * Return copy of player list (thus it can be changed)
     */
    public List<PokerPlayer> getPokerPlayersCopy()
    {
        List<PokerPlayer> copy = new ArrayList<PokerPlayer>();
        for (GamePlayer p : players_)
        {
            copy.add((PokerPlayer) p);
        }
        return copy;
    }

    /**
     * update list of players in tournament profile based on given player
     */
    public void updatePlayerList(PokerPlayer player)
    {
        if (isOnlineGame() && !player.isComputer())
        {
            updatePlayerList();
        }
    }

    /**
     * Update list of players in tournament profile
     */
    private void updatePlayerList()
    {
        if (profile_ != null)
        {
            List<String> list = new ArrayList<String>();
            int nNum = getNumPlayers();
            for (int i = 0; i < nNum; i++)
            {
                PokerPlayer p = getPokerPlayerAt(i);
                if (p.isHuman() && !p.isEliminated())
                {
                    list.add(p.getName());
                }
            }

            // save in profile
            profile_.setPlayers(list);
        }
    }

    /**
     * get poker player
     */
    public PokerPlayer getPokerPlayerAt(int n)
    {
        return (PokerPlayer) getPlayerAt(n);
    }

    /**
     * Get poker observer
     */
    public PokerPlayer getPokerObserverAt(int n)
    {
        return (PokerPlayer) getObserverAt(n);
    }

    /**
     * Get number of observers who were not players
     */
    public int getNumObserversNonPlayers()
    {
        int cnt = 0;
        int n = getNumObservers();
        for (int i = 0; i < n; i++)
        {
            // observer was a player if they have a finish
            if (getPokerObserverAt(i).getPlace() == 0)
            {
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * Remove observer - override to make sure players
     * is removed from their table's observer list too
     *
     * @param player
     */
    @Override
    public void removeObserver(GamePlayer player)
    {
        PokerPlayer pokerPlayer = ((PokerPlayer) player);
        PokerTable table = pokerPlayer.getTable();
        if (table != null) table.removeObserver(pokerPlayer);
        super.removeObserver(player);
    }

    /**
     * Get poker player by id.  Player returned
     * could be an observer.
     */
    public PokerPlayer getPokerPlayerFromID(int n)
    {
        return (PokerPlayer) getPlayerFromID(n, true);
    }

    /**
     * Get poker player by key - used in online games.  Player returned
     * could be an observer.
     */
    public PokerPlayer getPokerPlayerFromKey(String sKey)
    {
        // there should only be one (human) player
        // per key - due to logic in OnlineManager.
        // Thus, we don't check for multiple, we just
        // get the first one we find.
        PokerPlayer p;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPokerPlayerAt(i);
            if (p.isComputer()) continue;
            if (sKey.equals(p.getKey())) return p;
        }

        // search observers 2nd
        nNum = getNumObservers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPokerObserverAt(i);
            if (sKey.equals(p.getKey())) return p;
        }

        return null;
    }

    /**
     * Get poker player by socket - used in online games.  Player returned
     * could be an observer.
     */
    public PokerPlayer getPokerPlayerFromConnection(PokerConnection connection)
    {
        PokerPlayer p;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPokerPlayerAt(i);
            if (connection.equals(p.getConnection())) return p;
        }

        nNum = getNumObservers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPokerObserverAt(i);
            if (connection.equals(p.getConnection())) return p;
        }

        return null;
    }

    /**
     * Return the human player.   In online games, returns
     * local player.
     */
    public PokerPlayer getHumanPlayer()
    {
        if (isOnlineGame()) return getLocalPlayer();
        else return getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
    }

    /**
     * Get locally controlled player
     */
    public PokerPlayer getLocalPlayer()
    {
        String sKey = getPublicUseKey();
        PokerPlayer local = getPokerPlayerFromKey(sKey);
        ApplicationError.assertNotNull(local, "No player matching current key", sKey);
        return local;
    }

    private String sPubKey_ = null;

    /**
     * get public use key from engine (cache locally to avoid unnecessary String creation)
     */
    private String getPublicUseKey()
    {
        if (sPubKey_ == null) sPubKey_ = GameEngine.getGameEngine().getPublicUseKey();
        return sPubKey_;
    }

    /**
     * Get the host of online games
     */
    public PokerPlayer getHost()
    {
        return getPokerPlayerFromID(GamePlayer.HOST_ID);
    }

    /**
     * Return rank of player based on chips
     */
    public int getRank(PokerPlayer player)
    {
        PokerPlayer p;
        int nLastChips = 0;
        int nRank = 0;
        int nChips;
        List<PokerPlayer> rank = getPlayersByRank();
        for (int i = 0; i < rank.size(); i++)
        {
            p = rank.get(i);
            nChips = p.getChipCount();
            if (nChips != nLastChips)
            {
                nRank = (i + 1);
            }
            nLastChips = nChips;
            if (p == player) return nRank;
        }
        throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "No rank for player", player.toString());
    }

    /**
     * Get sorted list of players
     */
    public List<PokerPlayer> getPlayersByRank()
    {
        List<PokerPlayer> sort = getPokerPlayersCopy();
        Collections.sort(sort, SORTCHIPS);
        return sort;
    }

    // instances for sorting
    private static SortChips SORTCHIPS = new SortChips();

    // sort players by chips they have at start of hand
    private static class SortChips implements Comparator<PokerPlayer>
    {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerPlayer p1, PokerPlayer p2)
        {
            // reverse comparison so highest chips at top
            int diff = p2.getChipCount() - p1.getChipCount();
            if (diff != 0) return diff;

            // if no diff, and chip count is zero, sort by place
            // normal comparison so best finish at top
            if (p1.getChipCount() == 0)
            {
                diff = p1.getPlace() - p2.getPlace();
                if (diff != 0) return diff;
            }

            // if still no diff, rank by id, which puts human towards the top
            return p1.getID() - p2.getID();
        }
    }

    /**
     * Set current poker table
     */
    public void setCurrentTable(PokerTable current)
    {
        if (current == currentTable_) return;

        PokerTable old = currentTable_;
        currentTable_ = current;

        if (old != null) old.setCurrent(false);
        if (current != null) current.setCurrent(true);

        firePropertyChange(PROP_CURRENT_TABLE, old, current);
    }

    /**
     * Get poker table being displayed
     */
    public PokerTable getCurrentTable()
    {
        return currentTable_;
    }

    /**
     * get num tables
     */
    public int getNumTables()
    {
        return tables_.size();
    }

    /**
     * Add a table to the list of tables maintained by the game.
     * Added table passed as "new" value in PROP_TABLES event.
     */
    public void addTable(PokerTable table)
    {
        tables_.add(table);
        firePropertyChange(PROP_TABLES, null, table);

    }

    /**
     * Get table at index
     */
    public PokerTable getTable(int i)
    {
        return tables_.get(i);
    }

    /**
     * Get table by number
     */
    public PokerTable getTableByNumber(int nTableNum)
    {
        PokerTable table;
        int nNum = getNumTables();
        for (int i = 0; i < nNum; i++)
        {
            table = getTable(i);
            if (table.getNumber() == nTableNum)
            {
                return table;
            }
        }
        return null;
    }

    /**
     * Remove table from tournament.
     * Removed table passed as "old" value in PROP_TABLES event.
     */
    public void removeTable(PokerTable table)
    {
        tables_.remove(table);
        table.setRemoved(true);
        firePropertyChange(PROP_TABLES, table, null);
    }

    /**
     * Get array of tables
     */
    public List<PokerTable> getTables()
    {
        return tables_;
    }

    /**
     * Get current level
     */
    public int getLevel()
    {
        return nLevel_;
    }

    /**
     * init chip count over all players (and specify their buyin),
     * init think bank millis and profile
     */
    private void initPlayers(int n, boolean bOnline)
    {
        PokerPlayer p;
        for (int i = 0; i < getNumPlayers(); i++)
        {
            p = getPokerPlayerAt(i);
            p.setChipCount(n);
            p.setBuyin(profile_.getBuyinCost());
            if (p.isProfileDefined()) p.getProfile().init(); // init profile at start of tournament
            if (bOnline && !p.isComputer() && !p.isHost()) p.setSittingOut(true); // sitout at start
        }
    }

    /**
     * Calc next min chip
     */
    private int calcMinChip(int nLevel)
    {
        // use current level (up to max)
        int nMin = Integer.MAX_VALUE;

        // figure out min chip amount for all players if level 1
        if (nLevel == 1)
        {
            if (isClockMode())
            {
                nMin = profile_.getBuyinChips();
            }
            else
            {
                int nNum = getNumPlayers();
                for (int i = 0; i < nNum; i++)
                {
                    nMin = Math.min(nMin, getMaxDenom(getPokerPlayerAt(i).getChipCount()));
                }
            }
        }

        // if can rebuy, take into account those chips
        if (profile_.isRebuys() && nLevel <= profile_.getLastRebuyLevel())
        {
            nMin = Math.min(nMin, getMaxDenom(profile_.getRebuyChips()));
        }

        // if addon, take into account those chips too
        if (profile_.isAddons() && nLevel <= profile_.getAddonLevel())
        {
            nMin = Math.min(nMin, getMaxDenom(profile_.getAddonChips()));
        }

        // from this level up to remaining levels, figure out smallest max chip to
        // cover all antes/blinds
        int nAnte, nBig, nSmall;
        for (int i = nLevel; i <= Math.max(nLevel, TournamentProfile.MAX_LEVELS); i++)
        {
            if (profile_.isBreak(i)) continue;
            nAnte = profile_.getAnte(i);
            nSmall = profile_.getSmallBlind(i);
            nBig = profile_.getBigBlind(i);
            if (nAnte > 0) nMin = Math.min(nMin, getMaxDenom(nAnte));
            nMin = Math.min(nMin, Math.min(getMaxDenom(nBig), getMaxDenom(nSmall)));
        }

        return nMin;
    }

    /**
     * Get largest chip denom which divides into value
     */
    private int getMaxDenom(int n)
    {
        int nChip = 1;
        // start at largest chip and look for a chip that
        // is less than or equal to min ante/blind and
        // is evenly divisble by that blind
        for (int i = nChipDenom_.length - 1; i >= 0; i--)
        {
            if (nChipDenom_[i] <= n &&
                n % nChipDenom_[i] == 0)
            {
                nChip = nChipDenom_[i];
                break;
            }
        }
        return nChip;
    }

    /**
     * Add extra chips created during race-offs
     */
    public void addExtraChips(int n)
    {
        nExtraChips_ += n;
    }

    /**
     * Get min chip denomination
     */
    public int getMinChip()
    {
        return nChipDenom_[nMinChipIdx_];
    }

    /**
     * Get chip denom prior to given denom
     */
    public int getLastMinChip()
    {
        return nChipDenom_[nLastMinChipIdx_];
    }

    /**
     * Get chip denom index for next (non-break) level
     */
    private int getNextMinChipIndex()
    {
        // init
        int nLevel = nLevel_ + 1;

        // if next level is a break, look for next
        // non-break level
        while (profile_.isBreak(nLevel))
        {
            nLevel++;
        }
        int nMinIdx = nMinChipIdx_;

        // if min chip for this level is greater
        // than last recorded min chip, increase it to
        // the next level - we increase chip level one
        // step at a time
        int nNewMinChip = calcMinChip(nLevel);
        int nMinChip = getMinChip();
        if (nNewMinChip > nMinChip)
        {
            if (nLevel == 1)
            {
                for (int i = 0; i < nChipDenom_.length; i++)
                {
                    if (nNewMinChip == nChipDenom_[i])
                    {
                        nMinIdx = i;
                        break;
                    }
                }
            }
            else
            {
                nMinIdx++;
                if (nMinIdx == nChipDenom_.length) nMinIdx--;
            }
        }
        return nMinIdx;
    }

    /**
     * Is there only one player left with chips
     */
    public boolean isOnePlayerLeft()
    {
        // shortcut - if we have multiple tables left, we
        // still have multiple players
        if (getNumTables() > 1) return false;

        // check all players (could be some not at a table, waiting)
        int nNumWithChips = 0;
        PokerPlayer player;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            player = getPokerPlayerAt(i);
            // safety check in case of erroneous chips being added to eliminated player (like late-registering rebuy)
            if (player.isEliminated()) continue;
            if (player.getChipCount() > 0) nNumWithChips++;
            if (nNumWithChips > 1) return false;
        }
        return (nNumWithChips == 1);
    }

    /**
     * Get big blind
     */
    public int getBigBlind()
    {
        return profile_.getBigBlind(nLevel_);
    }

    /**
     * Get small blind
     */
    public int getSmallBlind()
    {
        return profile_.getSmallBlind(nLevel_);
    }

    /**
     * Get ante
     */
    public int getAnte()
    {
        return profile_.getAnte(nLevel_);
    }

    /**
     * Get profile
     */
    public TournamentProfile getProfile()
    {
        return profile_;
    }

    /**
     * Set profile
     */
    public void setProfile(TournamentProfile profile)
    {
        TournamentProfile old = profile_;
        profile_ = profile;
        firePropertyChange(PROP_PROFILE, old, profile_);
    }

    /**
     * Change the level
     */
    public void changeLevel(int n)
    {
        int nOld = nLevel_;
        nLevel_ += n;
        clock_.setSecondsRemaining(getSecondsInLevel(nLevel_));
        firePropertyChange(PROP_CURRENT_LEVEL, nOld, nLevel_);
    }

    /**
     * Go to previous level (poker night use only)
     */
    public void prevLevel()
    {
        changeLevel(-1);
    }

    /**
     * Go to next level
     */
    public void nextLevel()
    {
        // get new min chip before level is changed
        int nNewMinIdx = getNextMinChipIndex();
        if (nMinChipIdx_ != nNewMinIdx)
        {
            nLastMinChipIdx_ = nLevel_ == 0 ? nNewMinIdx : nMinChipIdx_;
            nMinChipIdx_ = nNewMinIdx;
        }

        changeLevel(+1);
    }

    /**
     * Get seconds in a level
     */
    public int getSecondsInLevel(int nLevel)
    {
        return profile_.getMinutes(nLevel) * 60;
    }

    /**
     * Get number of seconds a portion of a hand takes.  We use
     * the number of hands per hour as the basis and assume 5 parts
     * per hand:  deal, flop, turn, river, showdown.
     */
    private int getSecondsPerHandAction()
    {
        GameEngine engine = GameEngine.getGameEngine();
        int nHandsPerHour = engine.getPrefsNode().getInt(PokerConstants.OPTION_HANDS_PER_HOUR, 30);
        int nSecondsPerHand = 3600 / nHandsPerHour;
        return nSecondsPerHand / 5;
    }

    /**
     * indicate a clock action took place - subtract appropriate amount of seconds
     */
    public void advanceClock()
    {
        int nAction = getSecondsPerHandAction();
        int nNew = clock_.getSecondsRemaining() - nAction;
        if (nNew < 0) nNew = 0;
        clock_.setSecondsRemaining(nNew);
    }

    /**
     * advance clock during a break.  Advances 120 seconds, which
     * happens every 200 millis, so breaks go by quickly in
     * practice mode
     */
    public void advanceClockBreak()
    {
        int nNew = clock_.getSecondsRemaining() - 120;
        if (nNew < 0) nNew = 0;
        clock_.setSecondsRemaining(nNew);
    }

    /**
     * Get clock used for game
     */
    public GameClock getGameClock()
    {
        return clock_;
    }

    /**
     * check whether blinds need to increase.  return true if they do
     */
    public boolean isLevelExpired()
    {
        return clock_.isExpired();
    }

    /**
     * Get start date of game (millis).
     */
    public long getStartDate()
    {
        return Utils.getMillisFromTimeStamp(id_);
    }

    /**
     * Get id of game
     */
    public long getID()
    {
        return id_;
    }

    /**
     * Setup tournament given profile and num players (called from TournamentOptions).
     * For clock, nextLevel() is called to init the level and for practice mode,
     * setupTournament is called.  For online games, nothing is done - setupTournament
     * needs to be called explicitly after all players have joined the game.
     */
    public void initTournament(TournamentProfile profile)
    {
        // id used to uniquely identify a tournament in player profiles
        id_ = Utils.getCurrentTimeStamp();
        nLevel_ = 0;
        profile_ = profile;

        if (isClockMode())
        {
            nextLevel();
        }
        else if (isOnlineGame())
        {
            // little initial work done from TournamentOptions, rather
            // done when required start conditions are met.  We do
            // init profile so current players list has host.
            updatePlayerList();
        }
        else
        {
            setupTournament(false, true, profile_.getNumPlayers());
        }
    }

    /**
     * return if game is in progress
     */
    public boolean isInProgress()
    {
        // game is in registration mode (online) until HostStart calls setupTournament
        return nLevel_ > 0;
    }

    /**
     * Setup the tournament by creating computer players, creating players and
     * assigning to tables, setting initial chip count for each player and
     * setting the first level
     */
    public void setupTournament(boolean bOnline, boolean bFillComputers, int nMaxPlayers)
    {
        // if need to fill remaining seats with computers, do so
        if (bFillComputers)
        {
            setupComputerPlayers(nMaxPlayers);
        }

        // assign players to tables and give them their buyin
        assignTables(bOnline);
        initPlayers(profile_.getBuyinChips(), bOnline);

        computeTotalChipsInPlay();

        // init first level - calc's min chip (need to do after initChipCount)
        nextLevel();

        // set initial min chip now that its been set
        PokerTable table;
        for (int i = 0; i < getNumTables(); i++)
        {
            table = getTable(i);
            table.setMinChip(getMinChip());
        }

        // init DDMessage MsgState
        initMsgState(true);
    }

    /**
     * setup the players in the game
     */
    private void setupComputerPlayers(int nNumPlayers)
    {
        PokerPlayer player;
        List<String> names = new ArrayList<String>(PokerMain.getPokerMain().getNames());
        int nNumHumans = getNumPlayers();

        // if we don't have enough names, add more (enough
        // to account for non-used names that conflict with
        // humans)
        if (names.size() < nNumPlayers)
        {
            int step = names.size() / (nNumPlayers - names.size());
            int index = DiceRoller.rollDieInt(step) - 1;
            while (names.size() < nNumPlayers)
            {
                names.add(names.get(index) + " D.");
                index += step;
            }
        }

        Set<String> hsUsed = new HashSet<String>();

        for (int i = 0; i < nNumHumans; i++)
        {
            hsUsed.add(getPokerPlayerAt(i).getName());
        }

        // fill remaining players with computer players
        PlayerType playerType;
        String sName;
        String sKey = getPublicUseKey();
        Map<String, List<String>> hmRoster = new HashMap<String, List<String>>();
        List<String> roster;
        for (int i = getNumPlayers(); i < nNumPlayers; i++)
        {
            playerType = getNextPlayerType(/*i - nNumHumans, nNumPlayers - nNumHumans*/);
            roster = hmRoster.get(playerType.getFileName());
            if (roster == null)
            {
                roster = Roster.getRosterNameList(playerType);
                hmRoster.put(playerType.getFileName(), roster);
            }
            sName = getName(names, roster, hsUsed);
            player = new PokerPlayer(sKey, getNextPlayerID(), sName, false);
            player.setPlayerType(playerType);
            addPlayer(player);
        }
    }

    private PlayerType getNextPlayerType()
    {
        PlayerType result = null;

        SecureRandom random = SecurityUtils.getSecureRandom();
        int percentile = random.nextInt(100);
        // SecureRandom replaced:  (Math.random() * 100);
        // FIX: verify this works as expected.  The index/total params
        // not used, but this code was commented out:
        // (index * 100) / total;

        List<BaseProfile> playerTypes = PlayerType.getProfileListCached();

        for (BaseProfile playerType : playerTypes)
        {
            PlayerType type = (PlayerType) playerType;

            int percent = profile_.getPlayerTypePercent(type.getUniqueKey());

            if (percent > percentile)
            {
                result = type;
                break;
            }
            else
            {
                percentile -= percent;
            }
        }

        if (result == null) result = PlayerType.getDefaultProfile();

        return result;
    }

    /**
     * get random name not equal to any human
     */
    private String getName(List<String> names, List<String> roster, Set<String> hsUsed)
    {
        String sName;

        while (true)
        {
            if (!roster.isEmpty())
            {
                sName = roster.remove(DiceRoller.rollDieInt(roster.size()) - 1);
            }
            else
            {
                sName = names.remove(DiceRoller.rollDieInt(names.size()) - 1);
            }

            if (!hsUsed.contains(sName))
            {
                hsUsed.add(sName);

                return sName;
            }
        }
    }

    /**
     * Get # seats at table (call-through to TournamentProfile)
     */
    public int getSeats()
    {
        return profile_.getSeats();
    }

    /**
     * Assign all players to tables
     */
    private void assignTables(boolean bOnline)
    {
        int nNumPlayers = getNumPlayers();
        List<PokerPlayer> players = getPokerPlayersCopy(); // clone since we will remove players as we go

        // evenly distribute players across tables
        int nNumTables = nNumPlayers / getSeats();
        if (nNumPlayers % getSeats() > 0) nNumTables++;
        int nMinPerTable = nNumPlayers / nNumTables;
        int nExtra = nNumPlayers % nNumTables;
        int nMax;
        for (int i = 0; i < nNumTables; i++)
        {
            nMax = nMinPerTable;
            if (nExtra > 0)
            {
                nMax++;
                nExtra--;
            }
            fillSeats(players, nMax, bOnline);
        }

        // if host is an observer, assign to table 1
        PokerPlayer host = getHost();
        if (host.isObserver())
        {
            getTable(0).addObserver(host);
        }

        // assign observers tables
        PokerTable hostTable = host.getTable();
        nNumPlayers = getNumObservers();
        PokerPlayer obs;
        for (int i = 0; i < nNumPlayers; i++)
        {
            obs = getPokerObserverAt(i);
            if (obs.isHost()) continue;
            hostTable.addObserver(obs);
        }
    }

    /**
     * Fill seats in a table from list of players up to max
     */
    private void fillSeats(List<PokerPlayer> players, int nMax, boolean bOnline)
    {
        int idx;
        PokerTable table = new PokerTable(this, tables_.size() + 1);
        table.setTableState(PokerTable.STATE_PENDING);
        table.setPendingTableState(PokerTable.STATE_DEAL_FOR_BUTTON);
        table.setMinChip(getMinChip());

        // num seats to fill
        int nOpen = table.getNumOpenSeats();
        boolean bDemo = GameEngine.getGameEngine().isDemo();

        // randomly assign player
        PokerPlayer player;
        for (int i = 0; i < nOpen && i < nMax; i++)
        {
            // if demo, set seed so order is same
            // do in loop since adding player to table
            // triggers AI creation, which could change seed
            if (bDemo)
            {
                DiceRoller.setSeed(49469233 + i);
            }

            idx = DiceRoller.rollDieInt(players.size()) - 1;

            // testing with two players and more than 10 players - keep at diff tables
            if (TESTING(PokerConstants.TESTING_SPLIT_HUMANS) && ((getNumPlayers() > 10 && getNumHumans() == 2) ||
                                                                 (getNumPlayers() > 20 && getNumHumans() == 3)))
            {
                if (getNumTables() == 0 && table.getNumOccupiedSeats() == 0)
                {
                    logger.debug("TESTING: placing host at table 1");
                    idx = 0;
                }
                else
                {
                    boolean bDone = false;
                    while (!bDone)
                    {
                        PokerPlayer peek = players.get(idx);
                        if (peek.isHuman() && table.getNumOccupiedSeats() > 0 && !table.isAllComputer())
                        {
                            idx = DiceRoller.rollDieInt(players.size()) - 1;
                            logger.debug("TESTING: skip placing " + peek.getName() + " on table " + table.getName() +
                                         " new index to check: " + idx);
                        }
                        else bDone = true;
                    }
                }
            }
            player = players.remove(idx);
            table.addPlayer(player);

            // if practice mode and we place human, mark this table
            // as current (online setCurrent handled in HostStart
            // and in load-game logic)
            if (!bOnline && player.isHuman())
            {
                setCurrentTable(table);
            }
        }

        addTable(table);
    }

    private int nLastPool_ = 0;

    /**
     * handle a player getting out of tournament
     */
    public void playerOut(PokerPlayer player)
    {
        // figure finish spot
        int nTotalOut = getNumPlayersOut();
        int nFinish = getNumPlayers() - nTotalOut;

        // update pool total
        int nPool = getPrizePool();
        if (nPool != nLastPool_)
        {
            nLastPool_ = nPool;
            profile_.setPrizePool(nPool, true);
        }

        // get amount paid
        int nPrize;

        // if 1st place, get rest of pool not paid.  This
        // accounts for underpayments to people out before
        // rebuys/addons over yet still received prize money (rare case)
        if (nFinish == 1)
        {
            // get prizepool as profile defines it (to account for house cut)
            nPrize = profile_.getPrizePool() - getPrizesPaid();
        }
        // else get from profile
        else
        {
            nPrize = profile_.getPayout(nFinish);
        }

        // set player's prize, place and note eliminated
        player.setEliminated(true);
        player.setPlace(nFinish);
        player.setPrize(nPrize);
        nNumOut_++;

        // update player list (online games)
        updatePlayerList(player);

        // event
        firePropertyChange(PROP_PLAYER_FINISHED, null, player);
    }

    /**
     * Get number of players out
     */
    public int getNumPlayersOut()
    {
        return nNumOut_;
    }

    /**
     * Get players in wait list, sorted by time added to list
     * (longest wait on list at top)
     */
    public List<PokerPlayer> getWaitList()
    {
        List<PokerPlayer> wait = null;
        int nNum = getNumPlayers();
        PokerPlayer p;
        for (int i = 0; i < nNum; i++)
        {
            p = getPokerPlayerAt(i);
            if (p.isWaiting())
            {
                if (wait == null) wait = new ArrayList<PokerPlayer>();
                wait.add(p);
            }
        }

        if (wait != null && wait.size() > 1)
        {
            Collections.sort(wait, SORTBYWAIT);
        }

        return wait;
    }

    // instances for sorting
    private static SortByWait SORTBYWAIT = new SortByWait();

    // sort players by when they were added to wait list
    private static class SortByWait implements Comparator<PokerPlayer>
    {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerPlayer p1, PokerPlayer p2)
        {
            return (int) (p1.getWaitListTimeStamp() - p2.getWaitListTimeStamp());
        }
    }

    /**
     * Get prize pool based on actual buy-ins and rebuys
     */
    public int getPrizePool()
    {
        int n = 0;
        PokerPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            p = getPokerPlayerAt(i);
            n += p.getTotalSpent();
        }
        return n;
    }

    /**
     * Get total paid out so far
     */
    public int getPrizesPaid()
    {
        int n = 0;
        PokerPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            p = getPokerPlayerAt(i);
            n += p.getPrize();
        }
        return n;
    }

    /**
     * Verify chips count equals sum of all rebuys,addons,buyins
     */
    public void verifyChipCount()
    {
        int nChips = 0;
        int nBought = 0;
        PokerPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            p = getPokerPlayerAt(i);
            nChips += p.getChipCount();
            nBought += profile_.getBuyinChips() +
                       (p.getAddon() == 0 ? 0 : profile_.getAddonChips()) +
                       p.getNumRebuys() * profile_.getRebuyChips();
        }

        // for online, add in chips in pots at all tables, since
        // hands aren't in sync and money could be in pots when we
        // do this calculation
        if (isOnlineGame())
        {
            PokerTable table;
            HoldemHand hhand;
            for (int i = getNumTables() - 1; i >= 0; i--)
            {
                table = getTable(i);
                hhand = table.getHoldemHand();
                if (hhand == null) continue;

                // if in showdown, pot has already been allocated to players
                // and this would result in extra chips
                if (hhand.getRound() == HoldemHand.ROUND_SHOWDOWN) continue;

                nChips += hhand.getTotalPotChipCount();
            }
        }

        if (nChips != (nBought + nExtraChips_))
        {
            logger.error("Chip count off.  Bought=" + nBought + "   chips=" + nChips + "   nExtra=" + nExtraChips_);
        }
    }

    /**
     * Debug print tables
     */
    public void debugPrintTables(boolean bShort)
    {
        for (int i = 0; i < getNumTables(); i++)
        {
            logger.debug(getTable(i).toString(bShort));
        }
    }


    ////
    //// 2.0 online stuff
    ////

    // transient (recreated upon load)
    private PokerGameState state_;

    private void initMsgState(boolean bInitIds)
    {
        if (isOnlineGame() && state_ == null)
        {
            // create a poker game state for this game so marshalling of
            // DDMessages containing objects like HandAction work properly
            // Only needed in online play.
            state_ = new PokerGameState(this, bInitIds);
            DDMessage.setMsgState(state_); // FIX: ick!  Figure out a way to do this non-staticly (prohibits multi-games)
        }
    }

    /**
     * finish - cleanup
     */
    @Override
    public void finish()
    {
        clock_.stop();
        if (state_ != null)
        {
            state_.finish();
            state_ = null;
            DDMessage.setMsgState(null); // FIX: ick!!!!
        }

        setOnlineMode(MODE_NONE);

        if (onlineMgr_ != null)
        {
            onlineMgr_.finish();
            onlineMgr_ = null;
        }

        super.finish();
    }

    /**
     * Initialize game for online play - create OnlineManager
     */
    public void initOnline(int mode)
    {
        setOnlineMode(mode);
        initOnlineManager(null);
    }

    /**
     * init online manager
     */
    public OnlineManager initOnlineManager(TournamentDirector td)
    {
        OnlineManager mgr = new OnlineManager(this);
        if (td != null) mgr.setTournamentDirector(td);
        onlineMgr_ = mgr;
        return getOnlineManager();
    }

    /**
     * Get online manager
     */
    public OnlineManager getOnlineManager()
    {
        return onlineMgr_;
    }

    /**
     * Get online mode
     */
    public int getOnlineMode()
    {
        return nOnlineMode_;
    }

    /**
     * Set online mode
     */
    public void setOnlineMode(int n)
    {
        nOnlineMode_ = n;
    }

    /**
     * Is this a listed public game?
     */
    public boolean isPublic()
    {
        return bPublic_;
    }

    /**
     * Is this a listed public game?
     */
    public void setPublic(boolean bPublic)
    {
        bPublic_ = bPublic;
    }

    /**
     * Is online ready?
     */
    public boolean isAcceptingRegistrations()
    {
        // we must have a host
        PokerPlayer p = getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
        if (p == null) return false;

        // and the host's key must equal our game
        String sKey = getPublicUseKey();
        if (!sKey.equals(p.getKey())) return false;

        return nOnlineMode_ == MODE_REG ||
               nOnlineMode_ == MODE_PLAY;
    }

    /**
     * Get local ip
     */
    public String getLocalIP()
    {
        return sLocalIP_;
    }

    /**
     * Set local ip
     */
    public void setLocalIP(String s)
    {
        sLocalIP_ = s;
    }

    /**
     * Get public ip
     */
    public String getPublicIP()
    {
        return sPublicIP_;
    }

    /**
     * Set public ip
     */
    public void setPublicIP(String s)
    {
        sPublicIP_ = s;
    }

    /**
     * Get port (used both local/public)
     */
    public int getPort()
    {
        return nPort_;
    }

    /**
     * Set prot
     */
    public void setPort(int n)
    {
        nPort_ = n;
    }

    /**
     * Get lan connect string
     */
    public String getLanConnectURL()
    {
        return getConnectURL(getLocalIP());
    }

    /**
     * Get public connect string
     */
    public String getPublicConnectURL()
    {
        String sPub = getPublicIP();
        if (sPub == null) return null;
        return getConnectURL(sPub);
    }

    /**
     * Return if this is a UDP connection
     */
    public boolean isUDP()
    {
        return PokerURL.isUDP(getOnlineGameID());
    }

    /**
     * Get regular expression for connect URL for this game.  Essentially, it
     * is a regexp which valiates a proper IP address
     */
    public String getConnectRegExp()
    {
        return '^' + getConnectURL(PokerConstants.REGEXP_IP_ADDRESS) + '$';
    }

    /**
     * Get connect string
     */
    private String getConnectURL(String IP)
    {
        // SAMPLE:  poker://192.111.2.101:11885/n-1/QPF-841
        StringBuilder sb = new StringBuilder();
        sb.append(PokerConstants.URL_START);
        sb.append(IP);
        sb.append(P2PURL.PORT_DELIM);
        sb.append(getPort());
        sb.append(P2PURL.URI_DELIM);
        sb.append(getOnlineGameID());
        sb.append(PokerConstants.ID_PASS_DELIM);
        sb.append(getOnlinePassword());
        return sb.toString();
    }

    ////
    //// misc overrides
    ////

    /**
     * Override - not used
     */
    @Override
    public void setOnlinePlayerIDs(DMArrayList<Integer> ids)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setOnlinePlayerIDs() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public DMArrayList<Integer> getOnlinePlayerIDs()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getOnlinePlayerIDs() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public boolean isOnlinePlayer(GamePlayer player)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "isOnlinePlayer() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    @Override
    public DMArrayList getResendList()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getResendList() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    @Override
    public DMArrayList getTimestampList()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getTimestampList() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void addCompletedPhase(String sPhase)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "addCompletedPhase() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public boolean isCompletedPhase(String sPhase)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "isCompletedPhase() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void clearCompletedPhases()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "clearCompletedPhases() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setTurn(int num)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setTurn() not used in PokerGame", null);
    }

    /**
     * Get turn # for game
     */
    @Override
    public int getTurn()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getTurn() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setCurrentPlayer(int i)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setCurrentPlayer() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setCurrentPlayerByID(int id)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setCurrentPlayerByID() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setCurrentPlayer(GamePlayer player)
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setCurrentPlayer() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public int getCurrentPlayerIndex()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getCurrentPlayerIndex() not used in PokerGame - use HoldemHand's instead", null);
    }

    /**
     * Get the current player
     */
    @Override
    public GamePlayer getCurrentPlayer()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getCurrentPlayer() not used in PokerGame - used HoldemHand's instead", null);
    }

    ////
    //// save/load logic
    ////

    /**
     * Get save details with given init value
     */
    @Override
    public SaveDetails getSaveDetails(int nInit)
    {
        SaveDetails details = new SaveDetails(nInit);
        PokerSaveDetails pdetails = new PokerSaveDetails(nInit);
        details.setCustomInfo(pdetails);
        details.setSaveTerritories(SaveDetails.SAVE_NONE); // we never save territories
        details.setSaveGameHashData(SaveDetails.SAVE_ALL); // we always save game hash data
        return details;
    }

    /**
     * Override to update description
     */
    @Override
    public void saveGame(GameState state)
    {
        state.setDescription(getDescription());
        super.saveGame(state);
    }

    /**
     * save poker specific data
     */
    @Override
    protected void saveSubclassData(GameState state)
    {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        // create entry with game info (including num of entries)
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_NUM_GAMEDATA);

        // info
        entry.addToken(nLevel_);
        entry.addToken(bClockMode_);
        entry.addToken(clock_.marshal(state));
        entry.addToken(id_);
        entry.addToken(nMinChipIdx_);
        entry.addToken(nLastMinChipIdx_);
        entry.addToken(nExtraChips_);

        // profiles
        if (pdetails.getSaveProfileData() != SaveDetails.SAVE_NONE)
        {
            try
            {
                // tournament profile
                StringWriter writer = new StringWriter();
                profile_.write(writer);
                entry.addToken(writer.toString());
                writer.getBuffer().setLength(0);
            }
            catch (IOException ioe)
            {
                throw new ApplicationError(ioe);
            }
        }

        // player saving for
        PokerTable table;
        PokerPlayer playerForSave = null;
        boolean bSaveOtherTable = false;
        PokerTable tableForPlayer = null;
        int nPlayerID = pdetails.getPlayerID();
        if (nPlayerID != PokerSaveDetails.NO_PLAYER)
        {
            playerForSave = getPokerPlayerFromID(nPlayerID);
            tableForPlayer = playerForSave.getTable();
        }

        // num tables
        int nNum = 0;
        int nNumTables = getNumTables();
        switch (pdetails.getSaveTables())
        {
            case SaveDetails.SAVE_ALL:
                nNum = nNumTables;
                if (playerForSave != null) bSaveOtherTable = true;
                break;

            case SaveDetails.SAVE_DIRTY:
                for (int i = 0; i < nNumTables; i++)
                {
                    table = getTable(i);
                    if (table.isDirty())
                    {
                        nNum++;
                        if (playerForSave != null && table != tableForPlayer) bSaveOtherTable = true;
                    }
                }
                break;

            case SaveDetails.SAVE_NONE:
                nNum = 0;
                break;

        }
        entry.addToken(nNum);

        // this is used to indicate that when saving for a particular player
        // if the playerForSave file includes table information for tables other
        // than the player's table
        pdetails.setOtherTableUpdate(bSaveOtherTable);

        // home game cash
        entry.addToken(nClockCash_);

        // DD POKER 2.0 new fields
        entry.addToken(sLocalIP_);
        entry.addToken(sPublicIP_);
        entry.addToken(nPort_);
        entry.addToken(bPublic_);
        entry.addToken(nOnlineMode_);

        // only playerForSave current table for ALL (file loads)
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL &&
            !pdetails.isSetCurrentTableToLocal())
        {
            entry.addToken(state.getId(currentTable_));
        }

        // DD Poker 2.0, BETA 5
        entry.addToken(lastHandSaved_);

        // DD Poker 2.0, FCS
        entry.addToken(nNumOut_);

        // done with this entry
        state.addEntry(entry);

        // tables (entry per)
        switch (pdetails.getSaveTables())
        {
            case SaveDetails.SAVE_ALL:
                for (int i = 0; i < nNumTables; i++)
                {
                    table = getTable(i);
                    table.addGameStateEntry(state);
                }
                break;

            case SaveDetails.SAVE_DIRTY:

                for (int i = 0; i < nNumTables; i++)
                {
                    table = getTable(i);
                    if (table.isDirty())
                    {
                        table.addGameStateEntry(state);
                    }
                }
                break;

            case SaveDetails.SAVE_NONE:
                break;

        }
    }

    /**
     * load poker specific data
     */
    @Override
    protected void loadSubclassData(GameState state)
    {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        GameStateEntry entry = state.removeEntry();

        // level
        nLevel_ = entry.removeIntToken();
        bClockMode_ = entry.removeBooleanToken();
        clock_.demarshal(state, entry.removeStringToken());
        id_ = entry.removeLongToken();
        nMinChipIdx_ = entry.removeIntToken();
        nLastMinChipIdx_ = entry.removeIntToken();
        nExtraChips_ = entry.removeIntToken();

        // profiles
        if (pdetails.getSaveProfileData() != SaveDetails.SAVE_NONE)
        {
            try
            {
                // tournament profile
                String sData = entry.removeStringToken();
                StringReader reader = new StringReader(sData);
                profile_ = new TournamentProfile();
                profile_.read(reader, true);
            }
            catch (IOException ioe)
            {
                throw new ApplicationError(ioe);
            }
        }

        // num tables
        int nNum = entry.removeIntToken();

        // home game
        nClockCash_ = entry.removeIntToken();

        // DD POKER 2.0 new fields
        sLocalIP_ = entry.removeStringToken();
        sPublicIP_ = entry.removeStringToken();
        nPort_ = entry.removeIntToken();
        bPublic_ = entry.removeBooleanToken();
        int nMode = entry.removeIntToken();
        // don't override mode if currently client
        if (nOnlineMode_ != MODE_CLIENT)
        {
            if (isGameOver()) nOnlineMode_ = MODE_NONE; // on load, don't allow re-joining game
            else nOnlineMode_ = nMode;
        }

        // only load current table for ALL (file loads)
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL &&
            !pdetails.isSetCurrentTableToLocal())
        {
            currentTable_ = (PokerTable) state.getObjectNullOkay(entry.removeIntegerToken());
        }

        // 2.0 additions
        lastHandSaved_ = entry.removeIntToken();
        nNumOut_ = entry.removeIntToken();

        // tables (entry per)
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL) tables_.clear(); // make sure empty on full load
        PokerTable table;
        for (int i = 0; i < nNum; i++)
        {
            entry = state.removeEntry();
            table = (PokerTable) entry.getObject();
            table.loadFromGameStateEntry(this, state, entry);

            // add if not already in list
            if (!tables_.contains(table)) tables_.add(table);
        }

        // removed tables
        int[] removed = pdetails.getRemovedTables();
        if (removed != null)
        {
            for (int aRemoved : removed)
            {
                table = getTableByNumber(aRemoved);
                removeTable(table);
                if (TournamentDirector.DEBUG_CLEANUP_TABLE)
                {
                    logger.debug("Removed on load: " + table.getName());
                }
            }
        }
    }

    /**
     * allow subclass to do final setup after load
     */
    @Override
    protected void gameLoaded(GameState state)
    {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        // if full load, make sure player 0 (host/human)'s key matches
        // that of the game doing the load.  This can not match if a different
        // user is loading someone else's save game.
        fixKeys(state);

        // load profiles now that all players are loaded and keys are set
        // we do this here since the AI init can look for other player's
        // data which may not yet be loaded
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            getPokerPlayerAt(i).loadProfile(state);
        }

        // set disconnected flag
        fixDisconnected(state);

        // fix IP address if necessary
        if (state.getFile() != null && isOnlineGame())
        {
            // check IP address to see if it changed from last save
            LanManager lan = PokerMain.getPokerMain().getLanManager();
            if (lan != null)
            {
                String sIP = lan.getIP();
                String sSavedIP = getLocalIP();
                if (!sSavedIP.equals(sIP) && !sIP.equals("127.0.0.1"))
                {
                    logger.info("Updating local ip from: " + sSavedIP + " to: " + sIP);
                    setLocalIP(sIP);
                    // TODO: notify user and check about public ip
                }
            }
        }

        // upon load make sure DDMessage MsgState is created
        // no need to load since after gameLoaded is called,
        // a Load event is fired.
        initMsgState(false);

        // if we are to set current table to the local players table, do so
        if (pdetails.isSetCurrentTableToLocal())
        {
            PokerPlayer local = getLocalPlayer();
            ApplicationError.assertNotNull(local.getTable(), "Table should not be null", local);
            setCurrentTable(local.getTable());
        }

        // compute total chips
        computeTotalChipsInPlay();

        // if practice load, set flag so "future" hand history cleaned up
        if (state.getFile() != null && !isOnlineGame())
        {
            setDeleteHandsAfterSaveDate(true);
        }

        // go through all tables
        for (int i = getNumTables() - 1; i >= 0; i--)
        {
            getTable(i).gameLoaded();
        }
    }

    /**
     * fix keys to match current game
     */
    private void fixKeys(GameState state)
    {
        // only fix when loading from a file
        if (state.getFile() == null || isClockMode())
        {
            return;
        }

        PokerPlayer host = getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
        String sHostKey = host.getKey();
        String sCurrentKey = getPublicUseKey();
        if (!sHostKey.equals(sCurrentKey))
        {
            PokerPlayer player;
            for (int i = getNumPlayers() - 1; i >= 0; i--)
            {
                player = getPokerPlayerAt(i);
                if (player.getKey().equals(sHostKey))
                {
                    player.setKey(sCurrentKey);

                    // info message
                    if (player.isHuman())
                    {
                        logger.info("Key in save file updated to current key for: " + player.getName());
                    }
                }
            }
        }
    }

    /**
     * fix disconnected flag - when reloading an online game, human players
     * are no longer connected.  Also remove observers, who are no longer
     * connected and don't need to be loaded
     */
    private void fixDisconnected(GameState state)
    {
        // only fix when loading from a file
        if (state.getFile() == null || !isOnlineGame())
        {
            return;
        }

        // set disconnected state for players
        PokerPlayer player;
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            player = getPokerPlayerAt(i);
            player.setDisconnected(!(player.isHost() || player.isComputer()));
            // I'm leaving this off - if a host exits and restarts right
            // away, there might be players waiting to rejoing right
            // away and setting them to sitting out seems wrong
            //if (player.isDisconnected()) player.setSittingOut(true);
        }

        // remove observers
        for (int i = getNumObservers() - 1; i >= 0; i--)
        {
            player = getPokerObserverAt(i);
            if (!player.isHost())
            {
                removeObserver(player);
            }
        }
    }
    ////
    //// ui bridge methods
    ////

    public void setInput(PokerTableInput input)
    {
        input_ = input;
    }

    public void setInputMode(int nMode)
    {
        setInputMode(nMode, null, null);
    }

    public void setInputMode(int nMode, HoldemHand hhand, PokerPlayer player)
    {
        if (input_ != null)
        {
            input_.setInputMode(nMode, hhand, player);
        }
    }

    public int getInputMode()
    {
        if (input_ != null)
        {
            return input_.getInputMode();
        }
        else
        {
            return PokerTableInput.MODE_NONE;
        }
    }

    private PlayerActionListener playerActionListener_ = null;

    public PlayerActionListener getPlayerActionListener()
    {
        return playerActionListener_;
    }

    public void setPlayerActionListener(PlayerActionListener listener)
    {
        ApplicationError.assertTrue(playerActionListener_ == null || listener == null, "Attempt to replace existing listener.");
        playerActionListener_ = listener;
    }

    public void playerActionPerformed(int action, int nAmount)
    {
        if (playerActionListener_ != null)
        {
            playerActionListener_.playerActionPerformed(action, nAmount);
        }
    }

    /**
     * total chips in play
     */
    public void computeTotalChipsInPlay()
    {
        int chips = nExtraChips_;

        PokerPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            p = getPokerPlayerAt(i);
            chips += profile_.getBuyinChips() +
                     (p.getAddon() == 0 ? 0 : profile_.getAddonChips()) +
                     p.getNumRebuys() * profile_.getRebuyChips();
        }

        totalChipsInPlay_ = chips;
    }

    /**
     * add chips
     */
    public void chipsBought(int nChips)
    {
        totalChipsInPlay_ += nChips;
    }

    /**
     * return total chips in play
     */
    public int getTotalChipsInPlay()
    {
        return totalChipsInPlay_;
    }

    /**
     * Get average stack size.
     */
    public int getAverageStack()
    {
        return getTotalChipsInPlay() / (getNumPlayers() - getNumPlayersOut());
    }

    /**
     * set id of last hand saved
     */
    public void setLastHandSaved(int handID)
    {
        lastHandSaved_ = handID;
    }

    /**
     * Get id of last hand saved
     */
    public int getLastHandSaved()
    {
        return lastHandSaved_;
    }

    /**
     * if set, hands are deleted upon first save
     */
    public void setDeleteHandsAfterSaveDate(boolean b)
    {
        deleteHandsAfterSaveDate_ = b;
    }

    /**
     * should "future" hand history be removed
     */
    public boolean isDeleteHandsAfterSaveDate()
    {
        return deleteHandsAfterSaveDate_;
    }
}
