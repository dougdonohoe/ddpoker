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
 * PokerTable.java
 *
 * Created on December 30, 2003, 2:12 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
public class PokerTable implements ObjectID
{
    static Logger logger = LogManager.getLogger(PokerTable.class);

    // no seat
    public static final int NO_SEAT = -1;

    // data
    private PokerGame game_;
    PokerPlayer players_[] = new PokerPlayer[PokerConstants.SEATS];
    private int nNum_;
    private String sName_;
    private int nButton_ = NO_SEAT;
    private int nLevel_ = 1; // start at level 1
    private int nMinChip_;
    private int nNextMinChip_ = 0;
    private boolean bColoringUp_;
    private boolean bColoringUpDisplay_;
    private int nHandNum_ = 0;
    private boolean bCurrent_ = false;
    private boolean bZipMode_ = false;
    private HoldemHand hhand_;
    private List<PokerPlayer> waitList_ = new ArrayList<PokerPlayer>();
    private List<PokerPlayer> addedList_ = new ArrayList<PokerPlayer>();
    private List<PokerPlayer> addonList_ = new ArrayList<PokerPlayer>();
    private List<PokerPlayer> rebuyList_ = new ArrayList<PokerPlayer>();
    private List<PokerPlayer> observers_ = new ArrayList<PokerPlayer>();
    private int nTableState_ = STATE_NONE;
    private int nPrevState_ = STATE_NONE;
    private int nPendingState_ = STATE_NONE;
    private String sPendingPhase_;
    private DMTypedHashMap dmPendingPhaseParams_;

    // transient
    private int nRejoinState_ = REJOIN_NONE;
    private long nRejoinStateSet_ = 0;
    private boolean bDirty_ = false;
    private long nLastStateChange_;
    private int nSeats_ = -1; // use getSeats() internally
    private boolean bSimulation_ = false;

    // REJOIN states
    public static final int REJOIN_NONE = 0;
    public static final int REJOIN_START = 1;
    public static final int REJOIN_PROCESS = 2;

    // PokerTable states
    public static final int STATE_NONE = 0;
    public static final int STATE_PENDING = 1;
    public static final int STATE_DEAL_FOR_BUTTON = 2;
    public static final int STATE_BEGIN = 3;
    public static final int STATE_BEGIN_WAIT = 4;
    public static final int STATE_CHECK_END_HAND = 5;
    public static final int STATE_CLEAN = 6;
    public static final int STATE_NEW_LEVEL_CHECK = 7;
    public static final int STATE_COLOR_UP = 8;
    public static final int STATE_START_HAND = 9;
    public static final int STATE_BETTING = 10;
    public static final int STATE_COMMUNITY = 11;
    public static final int STATE_SHOWDOWN = 12;
    public static final int STATE_DONE = 13;
    public static final int STATE_GAME_OVER = 14;
    public static final int STATE_PENDING_LOAD = 15;
    public static final int STATE_ON_HOLD = 16;
    public static final int STATE_BREAK = 17;
    public static final int STATE_PRE_SHOWDOWN = 18;

    public String toStringTableState()
    {
        return getStringForState(nTableState_);
    }
    
    public static String getStringForState(int nTableState)
    {
        switch (nTableState)
        {
            case STATE_NONE: return "none";
            case STATE_PENDING: return "pending";
            case STATE_DEAL_FOR_BUTTON: return "button";
            case STATE_BEGIN: return "begin";
            case STATE_BEGIN_WAIT: return "begin-wait";
            case STATE_CLEAN: return "clean";
            case STATE_CHECK_END_HAND: return "check-end-hand";
            case STATE_NEW_LEVEL_CHECK: return "new-level-check";
            case STATE_COLOR_UP: return "color-up";
            case STATE_START_HAND: return "start";
            case STATE_BETTING: return "betting";
            case STATE_COMMUNITY: return "community";
            case STATE_SHOWDOWN: return "showdown";
            case STATE_DONE: return "done";   
            case STATE_GAME_OVER: return "game-over";
            case STATE_PENDING_LOAD: return "pending-load";
            case STATE_ON_HOLD: return "on-hold";
            case STATE_BREAK: return "break";
            case STATE_PRE_SHOWDOWN: return "pre-showdown";
            default: return "unknown - " + nTableState;
        }
    }
    
    // transient (no need to save)
    private boolean bRemoved_ = false;
    private long pauseUntil_ = 0;
    private boolean bAllComputer_ = false;
    private boolean bInitAllComputer_ = false;
    
    /**
     * Empty constructor needed for demarshalling
     */
    public PokerTable() {}
    
    /** 
     * Creates a new instance of PokerTable
     */
    public PokerTable(PokerGame game, int nNum) {
        game_ = game;
        nNum_ = nNum;
        sName_ = PropertyConfig.getMessage("msg.table.name", nNum_);
    }
    
    /**
     * Get name of table
     */
    public String getName()
    {
        return sName_;
    }

    /**
     * Get game
     */
    public PokerGame getGame()
    {
        return game_;
    }

    /**
     * Get number of table
     */
    public int getNumber()
    {
        return nNum_;
    }

    /**
     * Set simulation
     */
    public void setSimulation(boolean b)
    {
        bSimulation_ = b;
    }

    /**
     * Is table used for sims? (meaning no AI)
     */
    public boolean isSimulation()
    {
        return bSimulation_;
    }

    /**
     * get max players at the table
     */
    public int getSeats()
    {
        if (nSeats_ == -1)
        {
            PokerGame game = getGame();
            nSeats_ = game == null ? 10 : game.getSeats();
        }
        return nSeats_;
    }

    /**
     * used in online to set a time to pause until next action.  Pass
     * in the millis to wait - this method adds that to System.currentTimeMillis()
     */
    public void setPause(long wait)
    {
        pauseUntil_ = System.currentTimeMillis() + wait;
    }
    
    /**
     * Get pause time
     */
    public long getPause()
    {
        return pauseUntil_;
    }
    
    /**
     * Object id for saving
     */
    public int getObjectID() 
    {
        return PokerConstants.START_TABLE_ID + nNum_;
    }
    
    /**
     * Mark table as removed
     */
    public void setRemoved(boolean b)
    {
        bRemoved_ = b;
        if (bRemoved_)
        {
            firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_TABLE_REMOVED, this));
        }
    }
    
    /**
     * Is removed?
     */
    public boolean isRemoved()
    {
        return bRemoved_;
    }

    /**
     * dirty
     */
    public void setDirty(boolean b)
    {
        bDirty_ = b;
    }

    /**
     * is dirty?
     */
    public boolean isDirty()
    {
        return bDirty_;
    }

    /**
     * add observer
     */
    public void addObserver(PokerPlayer p)
    {
        ApplicationError.assertTrue(p.isObserver(), "Player is not an observer", p);
        _addObserver(p, true);
    }

    /**
     * add observer w/out isObserver check, for internal use
     */
    private void _addObserver(PokerPlayer p, boolean bSetTable)
    {
        if (observers_.contains(p)) return;

        observers_.add(p);
        if (bSetTable || p.getTable() == null)
        {
            p.setTable(this, NO_SEAT);
        }
        firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_OBSERVER_ADDED, this, p, NO_SEAT));
    }

    /**
     * remove observer
     */
    public void removeObserver(PokerPlayer p)
    {
        if (!observers_.contains(p)) return;

        observers_.remove(p);
        p.setTable(null, NO_SEAT);
        firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_OBSERVER_REMOVED, this, p, NO_SEAT));
    }

    /**
     * Place player in specific seat (must be empty)
     */
    public void setPlayer(PokerPlayer p, int nSeat)
    {
        if (players_[nSeat] != null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, toString() + " already has a player in seat " + nSeat +
                                        ": " + players_[nSeat] + ", cannot seat: " + p, null);
        }

        // seat player
        players_[nSeat] = p;
        addedList_.add(p);
        p.setTable(this, nSeat);

        // need to force check
        bInitAllComputer_ = false;

        // instantiate AI as appropriate
        createPokerAI();

        // fire table added - the new player's listeners will get this event
        firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDED, this, p, nSeat));
    }

    /**
     * create poker ai
     */
    private void createPokerAI()
    {
        if ((!isAllComputer() || isCurrent()) && !isSimulation())
        {
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                if (players_[i] != null)
                {
                    players_[i].createPokerAI();
                }
            }
        }
    }

    /**
     * Add player to any open seat
     */
    public void addPlayer(PokerPlayer p)
    {
        if (getNumOpenSeats() == 0)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, toString() + " has no open seat for player " + p, null);
        }
        if (isRemoved())
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, toString() + " is removed but trying to add player " + p, null);
        }

        // a little slower, but let's try this random thing
        int i;
        while (true)
        {
            i = DiceRoller.rollDieInt(PokerConstants.SEATS) - 1;
            if (players_[i] == null) 
            {
                setPlayer(p, i);
                return;
            }
        }
    }
    
    /**
     * Return player in given seat
     */
    public PokerPlayer getPlayer(int nSeat)
    {
        return players_[nSeat];
    }
    
    /**
     * Return player in given seat (must be there otherwise exception is thrown)
     */
    public PokerPlayer getPlayerRequired(int nSeat)
    {
        if (players_[nSeat] == null) {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "No player at seat " + (nSeat+1), toString());
        }
        return players_[nSeat];
    }

    /**
     * Return array of players sorted by last time moved, with least recently
     * moved at the top of the array
     */
    public PokerPlayer[] getPlayersSortedByLastMove()
    {
        int nOcc = getNumOccupiedSeats();
        PokerPlayer players[] = new PokerPlayer[nOcc];
        PokerPlayer player;
        int nCnt = 0;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player == null) continue;
            players[nCnt++] = player;
        }

        if (nOcc > 1) Arrays.sort(players, SORTBYMOVED);
        return players;
    }

    // instances for sorting
    private static SortByMoved SORTBYMOVED = new SortByMoved();

    // sort players by when they last moved - smaller # hands played
    // at last moved means moved less recently
    private static class SortByMoved implements Comparator<PokerPlayer>
    {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerPlayer p1, PokerPlayer p2)
        {
            return p1.getHandsPlayedAtLastMove() - p2.getHandsPlayedAtLastMove();
        }
    }

    /**
     * Get seat offset so that local human player is at seat 5 (index 4).
     * Add this number to seat (modulus PokerTable.SEATS) to get
     * territory seat
     */
    public int getSeatOffset()
    {
        int nSeat = NO_SEAT;
        PokerPlayer player;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player == null) continue;

            if (player.isLocallyControlled() && player.isHuman())
            {
                nSeat = i;
            }
        }

        if (nSeat == NO_SEAT) return 0;

        return 4 - nSeat; // (seat 5 is index 4)
    }

   /**
     * Get the territory seat number to display given seat, adjusting for offset
     */
    public int getDisplaySeat(int nSeat)
    {
        nSeat += getSeatOffset();
        if (nSeat >= PokerConstants.SEATS)
        {
            nSeat -= PokerConstants.SEATS;
        }
        else if (nSeat < 0)
        {
            nSeat += PokerConstants.SEATS;
        }

        return nSeat;
    }

    /**
     * Get the seat at this table which cooresponds to the display seat, adjusting
     * for offset (does the opposite of getDisplaySeat)
     */
    public int getTableSeat(int nDisplaySeat)
    {
        nDisplaySeat -= getSeatOffset();
        if (nDisplaySeat >= PokerConstants.SEATS)
        {
            nDisplaySeat -= PokerConstants.SEATS;
        }
        else if (nDisplaySeat < 0)
        {
            nDisplaySeat += PokerConstants.SEATS;
        }

        return nDisplaySeat;
    }

    /**
     * Remove player from given seat
     */
    public PokerPlayer removePlayer(int nSeat)
    {
        if (players_[nSeat] == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, toString() + " has no player in seat " + nSeat, null);
        }

        PokerPlayer p = players_[nSeat];
        players_[nSeat] = null;

        // move button if button was at this player
        // TODO: dead button support (we assume button always at a player in various places like deal display)
        if (nButton_ == nSeat && getNumOccupiedSeats() > 0)
        {
            moveButton();
        }

        p.setTable(null, NO_SEAT);

        bInitAllComputer_ = false;

        // notify listeners of player removed
        firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REMOVED, this, p, nSeat));
        
        return p;
    }

    /**
     * Set all players on this table dirty, including observers
     */
    public void setPlayersObserversDirty(boolean bDirty)
    {
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            if (players_[i] != null) players_[i].setDirty(bDirty);
        }

        int nNum = getNumObservers();
        for (int i = 0; i < nNum; i++)
        {
            getObserver(i).setDirty(bDirty);
        }
    }
    
    /**
     * Are all players computers
     */
    public boolean isAllComputer()
    {
        // see note in load logic for purpose of this
        if (!bInitAllComputer_)
        {
            setAllComputer();
        }
        return bAllComputer_;
    }
    
    /**
     * Determine if is all computer.  We cache the value since this
     * is called quite frequently in TournamentDirector
     */
    private void setAllComputer()
    {
        bInitAllComputer_ = true;
        bAllComputer_ = true;

        // if debug flag is on, pretend this is not an all-computer table
        if (TESTING(PokerConstants.TESTING_PROCESS_ALL_COMPUTER_TABLES)) { bAllComputer_ = false; return; }

        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            if (players_[i] != null && players_[i].isHuman()) 
            {
                bAllComputer_ = false;
                break;
            }
        }
    }

    /**
     * Return number of open seats at this table
     */
    public int getNumOpenSeats()
    {
        return getSeats() - getNumOccupiedSeats();
    }

    /**
     * Return number of occupied seats at this table
     */
    public int getNumOccupiedSeats()
    {
        int nNum = 0;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            if (players_[i] != null) nNum++;
        }
        return nNum;
    }
    
    /**
     * get currrent state table is in
     */
    public int getTableState()
    {
        return nTableState_;
    }

    /**
     * Get previous state table was in
     */
    public int getPreviousTableState()
    {
        return nPrevState_;
    }

    /**
     * Set table state.  Synchronized so we can coordiate with
     * tournament director.
     */
    public synchronized void setTableState(int n)
    {
        int nOld = nTableState_;
        nTableState_ = n;
        if (nOld != nTableState_)
        {
            // order is important here.  Fire event before updating previous state
            // such that receipients event can see the "previous previous" state
            // by calling getPreviousTableState, the "previous" event from nOld
            // in the event and the new event from the table/or event.
            firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_STATE_CHANGED, this,
                                                    nOld, nTableState_));
            
            // set prev state unless old was a pending load (special case)
            touchLastStateChangeTime();
            if (nOld != STATE_PENDING_LOAD)
            {
                nPrevState_ = nOld;
            }
        }
    }

    /**
     * touch last state change to current time
     */
    public void touchLastStateChangeTime()
    {
        nLastStateChange_ = System.currentTimeMillis();
    }

    /**
     * get timestamp of last state change
     */
    public long getLastStateChangeTime()
    {
        return nLastStateChange_;
    }

    /**
     * add millis to last state change timestamp and
     * player think banks - used when TD is paused.
     */
    public void adjustForPause(long asleepMillis)
    {
        nLastStateChange_ += asleepMillis;

        // BUG 467 - account for think bank too
        PokerPlayer p;
        long nLast = 0;
        for (int i = 0; i < PokerConstants.SEATS; i ++)
        {
            p = getPlayer(i);
            if (p == null) continue;
            nLast = p.getThinkBankAccessed();
            if (nLast == 0) continue;
            nLast += asleepMillis;
            p.setThinkBankAccessed(nLast);
        }
    }

    /**
     * Get millis since last state change
     */
    public long getMillisSinceLastStateChange()
    {
        return System.currentTimeMillis() - nLastStateChange_;
    }
    
    /**
     * Get pending state
     */
    public int getPendingTableState()
    {
        return nPendingState_;
    }
    
    /**
     * Set pending state
     */
    public void setPendingTableState(int n)
    {
        nPendingState_ = n;
    }
    
    /**
     * Set phase run while pending
     */
    public void setPendingPhase(String s)
    {
        sPendingPhase_ = s;
    }
    
    /**
     * Get phase run while pending
     */
    public String getPendingPhase()
    {
        return sPendingPhase_;
    }
    
    /**
     * Set phase run while pending params
     */
    public void setPendingPhaseParams(DMTypedHashMap s)
    {
        dmPendingPhaseParams_ = s;
    }
    
    /**
     * Get phase run while pending params
     */
    public DMTypedHashMap getPendingPhaseParams()
    {
        return dmPendingPhaseParams_;
    }
    
    // used for debugging to track previous state output
    @SuppressWarnings({"PublicField"})
    public int nDebugLast_ = 0;

    /**
     * Get array list of observers
     */
    public List<PokerPlayer> getObservers()
    {
        return observers_;
    }

    /**
     *  Get number of observers
     */
    public int getNumObservers()
    {
        return observers_.size();
    }

    /**
     * Get observer at
     */
    public PokerPlayer getObserver(int i)
    {
        return observers_.get(i);
    }

    /**
     * Get array list of players recently added
     */
    public List<PokerPlayer> getAddedList()
    {
        return addedList_;
    }
    
    /**
     * Get array list of players woh recently did addon
     */
    public List<PokerPlayer> getAddonList()
    {
        return addonList_;
    }
    
    /**
     * Get array list of players recently did rebuy
     */
    public List<PokerPlayer> getRebuyList()
    {
        return rebuyList_;
    }


    /**
     * Get array list of players we are waiting on
     */
    public List<PokerPlayer> getWaitList()
    {
        return waitList_;
    }

    /**
     * add a player to wait list
     */
    public void addWait(PokerPlayer p)
    {
        if (p.isDisconnected()) logger.warn("Disconnected player added to waiting list: " + p.getName());
        if (waitList_.contains(p))
        {
            //noinspection ThrowableInstanceNeverThrown
            logger.warn("Attempting to add player already on wait list: " + p + " from: "+ Utils.formatExceptionText(new Throwable()));
            return;
        }
        waitList_.add(p);
    }
    
    /**
     * remove a player from the wait list
     */
    public void removeWait(PokerPlayer p)
    {
        waitList_.remove(p);
    }
    
    /**
     * remove all players from wait list
     */
    public void removeWaitAll()
    {
        waitList_.clear();
    }
    
    /**
     * return true if wait list contains given player
     */
    public boolean isWaitListMember(PokerPlayer p)
    {
        return waitList_.contains(p);
    }
    
    /**
     * Add all human players to wait list
     */
    public void addWaitAllHumans()
    {
        PokerPlayer p;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = players_[i];
            if (p == null) continue;
            if (p.isHuman() && !p.isDisconnected()) addWait(p);
        }
    }
    
    /**
     * size of wait list
     */
    public int getWaitSize()
    {
        return waitList_.size();
    }

    /**
     *  Get wait list player
     */
    public PokerPlayer getWaitPlayer(int n)
    {
        return waitList_.get(n);
    }

    /**
     * Get wait list player at position 0, null if list is empty
     */
    public PokerPlayer getWaitPlayer()
    {
        if (waitList_.isEmpty()) return null;
        return getWaitPlayer(0);
    }

    /**
     * set rejoining state - used to handle "re-integration" of player
     */
    public void setRejoinState(int n)
    {
        nRejoinState_ = n;
        nRejoinStateSet_ = System.currentTimeMillis();
    }

    /**
     * get time rejoing state changed
     */
    public long getLastRejoinStateChangeTime()
    {
        return nRejoinStateSet_;
    }

    /**
     * Get rejoin state
     */
    public int getRejoinState()
    {
        return nRejoinState_;
    }

    /**
     * get button
     */
    public int getButton()
    {
        return nButton_;
    }
    
    /**
     * Set button
     */
    public void setButton(int n)
    {
        int nOld = nButton_;
        nButton_ = n;
        if (players_[nButton_] == null)
        {
            ApplicationError.assertNotNull(players_[nButton_], "No player for button " + n);
        }
        firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_BUTTON_MOVED, this, nOld, nButton_));
    }
    
    /**
     * Set the button, by dealing high card if a human is at the table,
     * randomly if an all computer table
     */
    public void setButton()
    {
        // if all computer (but not current), set button randomly
        if (isAllComputer() && !isCurrent()) setButtonRandom();
        else setButtonHighCard();
    }
    
    /**
     * Randomly set the button
     */
    private void setButtonRandom()
    {
        int n = DiceRoller.rollDieInt(getNumOccupiedSeats()) - 1;
        
        // we do this loop to skip empty seats
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            if (players_[i] != null)
            {
                n--;
                if (n < 0)
                {
                    setButton(i);
                    return;
                }
            }
        }
        ApplicationError.assertTrue(false, "Failed to randomly assign button", null);
    }
    
    /**
     * Set button by dealing by hard card deal
     */
    private void setButtonHighCard()
    {
        // get deck, with set seed if demo
        long seed = 0;
        if (GameEngine.getGameEngine().isDemo())
        {
            seed = 149399;//with current seeds, causes button to be placed such that human gets AA on 1st deal
        }
        Deck deck = new Deck(true, seed);
        
        // init
        PokerPlayer player;
        Hand hand;
        Card card;
        Card high = null;
        int nHighSeat = 0;
        
        // assign each player a card, track the highest
        // so we can assign the button there
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player != null)
            {
                hand = player.newHand(Hand.TYPE_DEAL_HIGH);
                card = deck.nextCard();
                hand.addCard(card);
                if (card.isGreaterThan(high))
                {
                    high = card;
                    nHighSeat = i;
                }
            }
        }
        
        // set the button
        setButton(nHighSeat);
    }
    
    /**
     * Get seat after button that has a player
     */
    public int getNextSeatAfterButton()
    {
        return getNextSeat(nButton_);
    }
    
    /**
     * Get next seat at table after given seat
     */
    public int getNextSeat(int nSeat)
    {
        int nStart = nSeat;
        while (true)
        {
            int nNext = nSeat + 1;
            if (nNext >= PokerConstants.SEATS) nNext = 0;

            ApplicationError.assertTrue(nNext != nStart, "No next seat found");
            if (players_[nNext] == null)
            {
                nSeat = nNext;
                continue;
            }

            return nNext;
        }
    }
    
    /**
     * move button
     */
    public void moveButton()
    {
        setButton(getNextSeatAfterButton());
    }
    
    /**
     * Get hand num - 0 means no
     * hand yet dealt
     */
    public int getHandNum()
    {
        return nHandNum_;
    }
    
    /**
     * Get level this hand was started at
     */
    public int getLevel()
    {
        return nLevel_;
    }
    
    /**
     * Set min chip
     */
    public void setMinChip(int n)
    {
        nMinChip_ = n;
    }
    
    /**
     * Get min chip in use on this table
     */
    public int getMinChip()
    {
        return nMinChip_;
    }
    
    /**
     * Set next min chip, for use in color up and changing levels
     */
    public void setNextMinChip(int n)
    {
        nNextMinChip_ = n;
    }
    
    /**
     * Get next min chip for next level
     */
    public int getNextMinChip()
    {
        return nNextMinChip_;
    }

    /**
     * Set that we are displaying color up to user
     */
    public void setColoringUpDisplay(boolean b)
    {
        bColoringUpDisplay_ = b;
    }

    /**
     * Are we displaying color up to user?
     */
    public boolean isColoringUpDisplay()
    {
        return bColoringUpDisplay_;
    }

    /**
     * Set that we are coloring up
     */
    private void setColoringUp(boolean b)
    {
        bColoringUp_ = b;
    }

    /**
     * Are we coloring up?
     */
    public boolean isColoringUp()
    {
        return bColoringUp_;
    }
    
    /**
     * determine whether players have odd chips
     */
    public void doColorUpDetermination()
    {
        // BUG 513 - verify chip count is consistent with *current* min chip
        verifyChips();

        // get info
        PokerPlayer player;
        int nMin = getNextMinChip();
        int nLastMin = getMinChip();
        int nOdd;
        
        // go through all players and determine number of odd chips
        int nTotalOdd = 0;
        int nChips;
        int nLeft;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player == null) continue;

            nChips = player.getChipCount();
            if (nChips == 0) continue;
            nLeft = nChips % nMin;
            nOdd = nLeft / nLastMin;
            //logger.debug(player.getName() + " " + nChips + " chips, remainder = " + nLeft + " odd=" + nOdd);
            player.setOddChips(nOdd);
            nTotalOdd += nOdd;
        }

        // if we have players with odd chips, we need to color up
        if (nTotalOdd > 0)
        {
            // note that we are coloring up
            setColoringUp(true);
            
            // assign card per odd chip
            Hand hand;
            Card card;

            long seed = 0;
            if (GameEngine.getGameEngine().isDemo())
            {
                seed = 94876564;
            }
            Deck deck = new Deck(true, seed);

            // assign each player a card for each odd chip
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                player = getPlayer(i);
                if (player == null) continue;
                
                hand = player.newHand(Hand.TYPE_COLOR_UP);
                nOdd = player.getOddChips();
                
                for (int j = 0; j < nOdd; j++)
                {
                    card = deck.nextCard();
                    hand.addCard(card);
                }
            }
        }        
    }
    
    public void colorUp()
    {    
        int nMin = getNextMinChip();
        int nMinLast = getMinChip();
        List<PokerPlayer> players = new ArrayList<PokerPlayer>();
        PokerPlayer player;
        int nTotalOdd = 0;
        int nOdd;
        int nPlayerOddTotal;
        int nToCombine = nMin / nMinLast;
        int nChips;

        // add players to array
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player == null) continue;
            
            nOdd = player.getOddChips();
            if (nOdd > 0)
            {
                nTotalOdd += nOdd;
                players.add(player);
            }
        }

        // shortcut if no odd chips - note should not
        // happen, but just in case...
        if (players.isEmpty()) return;

        // sort
        Collections.sort(players, SORTCHIPRACE);

        // allocate chips
        boolean bWon;
        boolean bUnevenCheck = false;
        for (PokerPlayer p : players)
        {
            player = p;
            bWon = false;
            nPlayerOddTotal = player.getOddChips() * nMinLast;

            // players broke by chip race always get a chip
            // regardless of odd chip left
            if (player.getChipCount() == nPlayerOddTotal)
            {
                bWon = true;
                player.setBrokeChipRace(true);
            }
            // next player gets chip if there are enough odd chips
            // left to combine
            else if (nTotalOdd >= nToCombine)
            {
                bWon = true;
            }
            // finally if there are odd odd chips left, the next
            // player gets a chip if they have >= 50% of the chips 
            // needed to combine to next chip (only the next player
            // gets this chance)
            else if (!bUnevenCheck && nTotalOdd > 0)
            {
                if ((float) player.getOddChips() >= (nToCombine / 2.0f))
                {
                    bWon = true;
                }
                bUnevenCheck = true;
            }

            nChips = player.getChipCount();
            nChips -= player.getOddChips() * nMinLast;

            if (bWon)
            {
                player.setWonChipRace(true);
                nChips += nMin;
                nTotalOdd -= nToCombine;
            }

            player.setChipCount(nChips);
            player.setOddChips(0);
        }

        // account for odd chips
        getGame().addExtraChips((-nTotalOdd) * nMinLast);
    }
    
    /**
     * Cleanup after colorup
     */
    public void colorUpFinish()
    {
        setColoringUp(false);
        setColoringUpDisplay(false); // just to be sure (even though we call it in ColorUpFinish)

        PokerPlayer player;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player == null) continue;
            
            player.setWonChipRace(false);
            player.setBrokeChipRace(false);
            player.removeHand();
        }
    }
    
    // instances for sorting
    private SortChipRace SORTCHIPRACE = new SortChipRace();

    // sort players by top card in hand - players who would
    // go broke from chip race end up at top
    private class SortChipRace implements Comparator<PokerPlayer>
    {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerPlayer p1, PokerPlayer p2)
        {    
            int nMinLast = getMinChip();

            int nOddTotal1 = p1.getOddChips() * nMinLast;
            int nOddTotal2 = p2.getOddChips() * nMinLast;
            
            if (p1.getChipCount() == nOddTotal1) return -1;
            if (p2.getChipCount() == nOddTotal2) return 1;
            
            HandSorted h1 = p1.getHandSorted();
            HandSorted h2 = p2.getHandSorted();
            
            Card c1 = h1.getCard(h1.size() - 1);
            Card c2 = h2.getCard(h2.size() - 1);
            
            return c2.compareTo(c1);            
        }
    }

    /**
     * Set current
     */
    public void setCurrent(boolean b)
    {
        bCurrent_ = b;

        // instantiate AI as appropriate
        createPokerAI();
    }
    
    /**
     * is current table?
     */
    public boolean isCurrent()
    {
        return bCurrent_;
    }
    
    /**
     * Set zip mode
     */
    public void setZipMode(boolean b)
    {
        bZipMode_ = b;
    }
    
    /**
     * is zip mode
     */
    public boolean isZipMode()
    {
        return bZipMode_;
    }
    
    /**
     * Start a new hand
     */
    public void startNewHand()
    {
        PokerGame game = getGame();
        
        // increment hand number
        newHand(game);
                
        // create new hand
        HoldemHand hand = new HoldemHand(this);
        setHoldemHand(hand);

        // deal
        hhand_.deal();        
    }
    
    /**
     * This method is used to randomly distribute money from between players
     * as if a hand had taken place.  It is used for all-computer tables where
     * actually playing a hand takes too much time.
     */
    public void simulateHand()
    {
        PokerGame game = getGame();

        // start hand to advance button
        newHand(game);
        
        // simulate hand
        HoldemHand.simulateHand(game, this);        
    }

    /**
     * verify all ai are removed from all players at this table (used
     * when a table is all-computer)
     */
    public void verifyAllAIRemoved()
    {
        // check to make sure any ai are removed - there could
        // be ai left after a human is moved from the table
        PokerPlayer p;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = getPlayer(i);
            // use getGameAI to avoid extra logic in getPokerAI()
            if (p != null && p.getGameAI() != null)
            {
                if (DebugConfig.isTestingOn()) logger.debug("Clearing left-over ai on " + p.getName());
                p.setPokerAI(null);
            }
        }
    }

    boolean bSkipNextButtonMove_ = false;

    /**
     * Set to skip next button move - used when manually
     * moving the button at the end of a hand
     */
    public void setSkipNextButtonMove(boolean b)
    {
        bSkipNextButtonMove_ = b;
    }

    /**
     * increment hand - stuff to do when we have a new hand
     */
    private void newHand(PokerGame game)
    {
        // move button (unless 1st hand)
        if (nHandNum_ > 0)
        {
            if (nButton_ == NO_SEAT || (!bSkipNextButtonMove_ &&
                                        (game.isOnlineGame() ||
                                         !PokerUtils.isCheatOn(game.getGameContext(), PokerConstants.OPTION_CHEAT_MANUAL_BUTTON))))
            {
                moveButton();
            }
            bSkipNextButtonMove_ = false;
        }
        
        // increment hand num
        nHandNum_++;

        // see if level has changed
        levelCheck(game);
    }

    /**
     * handle break
     */
    public void startBreak()
    {
        levelCheck(getGame());
    }

    /**
     * Check to see if table's level matches game's level
     */
    public void levelCheck(PokerGame game)
    {
        // look at level
        int nCurrentLevel = game.getLevel();
        if (nCurrentLevel != nLevel_)
        {
            // update level and advance minchip if set
            nLevel_ = nCurrentLevel;
            if (nNextMinChip_ != 0)
            {
                nMinChip_ = nNextMinChip_;
                nNextMinChip_ = 0;
            }

            // notify that level has changed
            firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_LEVEL_CHANGED, this, nCurrentLevel, nLevel_));
        }

        // BUG 513 - always verify chips are correct
        verifyChips();
    }

    /**
     * BUG 513 - ensure chips are valid
     */
    private void verifyChips()
    {
        PokerPlayer p;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = getPlayer(i);
            if (p == null) continue;

            // check players chips - must be a multiple of the min chips at this table,
            // if not, round any min chips up.  This can happen in case if player
            // is moved from a table not-yet colored up to one that was (a timing issue
            // that we have yet to correct)
            int nChips_ = p.getChipCount();
            int nOddChips = nMinChip_ == 0 ? 0 : nChips_ % nMinChip_;
            if (nOddChips != 0)
            {
                int nAdd = nMinChip_ - nOddChips;
                int nNewChips = nChips_ + nAdd;
                p.setChipCount(nNewChips);
                getGame().addExtraChips(nAdd);
                logger.info(p.getName() + " chip count fixed by adding " + nAdd + " for total of $"+nNewChips);
            }
        }
    }

    /**
     * Get holdem hand
     */
    public HoldemHand getHoldemHand()
    {
        return hhand_;
    }
    
    /**
     * Set holdem hand (called directly for testing/other usage, normal way is to startNewHand(),
     * which calls this)
     */
    public void setHoldemHand(HoldemHand hand)
    {
        hhand_ = hand;
        if (hhand_ != null) firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_NEW_HAND, this));
    }

    /**
     * Return profile for game (can be null if none setup)
     */
    public TournamentProfile getProfile()
    {
        PokerGame game = getGame();
        if (game == null) return null;
        
        return game.getProfile();
    }
    
    /**
     * Is addon allowed at current level?
     * @param player
     */
    public boolean isAddonAllowed(PokerPlayer player)
    {
        // for correct display of rebuy button
        if (player.isObserver()) return false;

        TournamentProfile profile = getProfile();
        return (profile.isAddons() && profile.getAddonLevel() == getLevel());
    }

    /**
     * Is rebuy allowed at current level?
     */
    public boolean isRebuyAllowed(PokerPlayer player)
    {
        return isRebuyAllowed(player, getLevel());
    }

    /**
     * Is rebuy allowed at given level?
     */
    public boolean isRebuyAllowed(PokerPlayer player, int nLevel)
    {
        // for correct display of rebuy button
        if (player.isObserver()) return false;

        // in case player busted out and this is a late-arriving
        // online guest rebuy
        if (player.isEliminated()) return false;

        // if player is booted, disallow (prevents asking for rebuy
        // after booting player)
        if (player.isBooted()) return false;

        // get profile
        TournamentProfile profile = getProfile();

        // first, see if we are doing rebuys
        if (!profile.isRebuys()) return false;

        // get some basic info
        int nLast = profile.getLastRebuyLevel();
        int nMax = profile.getMaxRebuys();
        if (nMax == 0) nMax = Integer.MAX_VALUE;

        // start chip count with any pending rebuys
        int nChipCount = player.getRebuysChipsPending();

        // and add existing chips...
        // BUG 420 - use chip count at start of hand to determine if
        // player can rebuy during a hand.  This prevents someone from
        // going all-in and then rebuying
        if (hhand_ != null && hhand_.getRound() != HoldemHand.ROUND_SHOWDOWN)
        {
            nChipCount += player.getChipCountAtStart();
        }
        // else use actual chip count
        else
        {
            nChipCount += player.getChipCount();
        }
        int nRebuyChips = profile.getRebuyChipCount();

        // check chips remaining < or <= based on profile
        switch (profile.getRebuyExpressionType())
        {
            case PokerConstants.REBUY_LT:
                if (!(nChipCount < nRebuyChips)) return false;
                break;

           case PokerConstants.REBUY_LTE:
                if (!(nChipCount <= nRebuyChips)) return false;
                break;
        }

        // rebuy is okay
        return nLevel <= nLast &&
               (player.getNumRebuys() + player.getNumRebuysPending()) < nMax;
    }

    /**
     * Are rebuys done for this player?
     */
    public boolean isRebuyDone(PokerPlayer player)
    {
        // for correct display of rebuy button
        if (player.isObserver()) return true;

        // get profile
        TournamentProfile profile = getProfile();
        
        if (!profile.isRebuys()) return true;

        int nLast = profile.getLastRebuyLevel();
        int nMax = profile.getMaxRebuys();
        if (nMax == 0) nMax = Integer.MAX_VALUE;

        return getLevel() > nLast || (player.getNumRebuys() + player.getNumRebuysPending()) >= nMax;

    }
    
    /**
     * call addPendingRebuys on all players
     */
    public void addPendingRebuys()
    {
        PokerPlayer player;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = getPlayer(i);
            if (player == null) continue;
            player.addPendingRebuys();
        }
    }

    /**
     * ask ai about buyin.  Add to rebuy ArrayList those who rebought
     */
    public void aiRebuy()
    {
        // get profile
        TournamentProfile profile = getProfile();
        int nLevel = getLevel();
        rebuyList_.clear();
        
        // avoid loop if not necessary
        if (!profile.isRebuys() || nLevel > profile.getLastRebuyLevel()) return;

        PokerPlayer p;
        int nChips = profile.getRebuyChips();
        int nAmount = profile.getRebuyCost();
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = getPlayer(i);
            if (p == null) continue;
            if (p.isHuman()) continue;

            if (isRebuyAllowed(p, nLevel))
            {
                boolean bWantsRebuy;
                PokerAI ai = p.getPokerAI();
                if (ai != null)
                {
                    bWantsRebuy = ai.isRebuy();
                }
                else
                {
                    // for computer players with no AI instantiated, do simple
                    // check with basically the same logic
                    bWantsRebuy = V1Player.WantsRebuy(p, DiceRoller.rollDieInt(100));
                }

                if (bWantsRebuy)
                {
                    //logger.debug(p.getName() + " rebought at " + getName());
                    p.addRebuy(nAmount, nChips, false);
                    rebuyList_.add(p);
                }
            }
        }
    }
    
    /**
     * ask ai at table about addon.  Add to addon ArrayList those who added-on.
     */
    public void aiAddOn()
    {
        // get profile
        TournamentProfile profile = getProfile();
        addonList_.clear();

        PokerPlayer p;
        int nChips = profile.getAddonChips();
        int nAmount = profile.getAddonCost();
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = getPlayer(i);
            if (p == null) continue;
            if (p.isHuman()) continue;
            if (!isAddonAllowed(p)) continue;

            boolean bWantsAddon;
            PokerAI ai = p.getPokerAI();
            if (ai != null)
            {
                bWantsAddon = ai.isAddon();
            }
            else
            {
                // for computer players with no AI instantiated, do simple
                // check with basically the same logic
                bWantsAddon = V1Player.WantsAddon(p, DiceRoller.rollDieInt(100), profile);
            }

            if (bWantsAddon)
            {
                //logger.debug(p.getName() + " added on");
                p.addAddon(nAmount, nChips);
                addonList_.add(p);
            }
        }
    }
    
    /**
     * Get string describing table
     */
    @Override
    public String toString()
    {
        return toString(false);
    }
    
    /**
     * Debugging
     */
    public String toString(boolean bShort)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(": ");
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            sb.append('(').append(i + 1);
            if (nButton_ == i) sb.append('*');
            sb.append(") ");
            if (players_[i] == null) {
                sb.append("<empty>");
            } else {
                if (bShort)
                {
                    sb.append(players_[i].getName());
                }
                else
                {
                    sb.append(players_[i].toStringShort());
                }
            }
            if (i < (PokerConstants.SEATS - 1)) sb.append(", ");
        }
        return sb.toString();
    }
    
    /**
     * another to string
     */
    public String toStringSummary()
    {
         StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("; B[");
        sb.append(nButton_);
        sb.append("]; ");
        sb.append(getNumOccupiedSeats());
        sb.append(" players");
        return sb.toString();
    }
    
    ////
    //// PokerTableListener
    ////
    private List<ListenerInfo> listeners_ = new ArrayList<ListenerInfo>();
    
    /**
     * notify table that display preferences changed so listeners can react
     */
    public void prefsChanged()
    {
        firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PREFS_CHANGED, this));
    }
    
    /**
     * Add listener
     */
    public synchronized void addPokerTableListener(PokerTableListener l, int nTypes)
    {
        ListenerInfo nu = new ListenerInfo(l, nTypes);
        int old = listeners_.indexOf(nu);
        if (old != -1)
        {
            nu = listeners_.get(old);
            ApplicationError.assertTrue(nu.listener == l, "Mismatched listeners", null);
            
            nu.nTypes |= nTypes;
        }
        else
        {
            listeners_.add(nu);
        }
    }
    
    /**
     * Remove listener
     */
    public synchronized void removePokerTableListener(PokerTableListener l, int nTypes)
    {
        ListenerInfo nu = new ListenerInfo(l, nTypes);
        int old = listeners_.indexOf(nu);
        if (old != -1)
        {
            nu = listeners_.get(old);
            ApplicationError.assertTrue(nu.listener == l, "Mismatched listeners", null);
            
            nu.nTypes &= ~nTypes;

            if (nu.nTypes == 0)
            {
                listeners_.set(old, ListenerInfo.NULL_LISTENER);
            }
        }
    }
    
    /**
     * Fire listener
     */
    public synchronized void firePokerTableEvent(PokerTableEvent event)
    {
        //logger.debug("Firing: " + event);
        ListenerInfo info;
        int nSize = listeners_.size();
        for (int i = 0; i < nSize;)
        {
            info = listeners_.get(i);

            if (info == ListenerInfo.NULL_LISTENER)
            {
                listeners_.remove(i);

                --nSize;
            }
            else
            {
                if ((info.nTypes & event.getType()) > 0)
                {
                    info.listener.tableEventOccurred(event);
                }

                //noinspection AssignmentToForLoopParameter
                i++;
            }
        }
    }

    /**
     * Helper class to track listener and the event types it is interested in
     */
    private static class ListenerInfo
    {
        public static final ListenerInfo NULL_LISTENER = new ListenerInfo(null, 0);

        private PokerTableListener listener;
        private int nTypes;

        private ListenerInfo(PokerTableListener listener, int nTypes)
        {
            this.listener = listener;
            this.nTypes = nTypes;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof ListenerInfo)) return false;
            ListenerInfo info = (ListenerInfo) o;
            return info.listener == listener;
        }

        @Override
        public int hashCode()
        {
            return listener.hashCode();
        }
    }
    
    ////
    //// Save/Load
    ////
    
    /**
     * Return this player encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();
        
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_DATA);
        state.addEntry(entry);
        entry.addToken(nNum_);
        entry.addToken(sName_);
        entry.addToken(nButton_);
        entry.addToken(nLevel_);
        entry.addToken(nMinChip_);
        entry.addToken(nNextMinChip_);
        entry.addToken(bColoringUp_);
        entry.addToken(bColoringUpDisplay_);
        entry.addToken(nHandNum_);
        entry.addToken(hhand_);
        entry.addToken(TournamentDirector.getStateForSave(state, this));
        entry.addToken(nPrevState_);
        entry.addToken(nPendingState_);
        entry.addToken(sPendingPhase_);
        entry.addToken(dmPendingPhaseParams_);
        entry.addToken(bZipMode_);
        
        // only retain current on full save
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL && !pdetails.isSetCurrentTableToLocal())
        {
            entry.addToken(bCurrent_);
        }
 
        // players at table
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            entry.addToken(state.getId(players_[i]));
        }  
        
        // other player lists
        addPlayerList(state, entry, waitList_);
        addPlayerList(state, entry, addedList_);
        addPlayerList(state, entry, addonList_);
        addPlayerList(state, entry, rebuyList_);
        addPlayerList(state, entry, observers_);

        return entry;
    }
    
    /**
     * save array of players
     */
    static void addPlayerList(MsgState state, TokenizedList entry, List<PokerPlayer> list)
    {
        int nNum = list.size();
        entry.addToken(nNum);
        for (int i = 0; i < nNum; i++)
        {
            entry.addToken(state.getId(list.get(i)));
        }
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(PokerGame game, GameState state, GameStateEntry entry)
    {
        SaveDetails details = state.getSaveDetails();
        PokerSaveDetails pdetails = (PokerSaveDetails) details.getCustomInfo();

        game_ = game;
        nNum_ = entry.removeIntToken();
        sName_ = entry.removeStringToken();
        nButton_ = entry.removeIntToken();
        nLevel_ = entry.removeIntToken();
        nMinChip_ = entry.removeIntToken();
        nNextMinChip_ = entry.removeIntToken();
        bColoringUp_ = entry.removeBooleanToken();
        bColoringUpDisplay_ = entry.removeBooleanToken();
        nHandNum_ = entry.removeIntToken();
        hhand_ = (HoldemHand) entry.removeToken();
        nTableState_ = entry.removeIntToken();
        nPrevState_ = entry.removeIntToken();
        nPendingState_ = entry.removeIntToken();
        sPendingPhase_ = entry.removeStringToken();
        dmPendingPhaseParams_ = (DMTypedHashMap) entry.removeToken();
        bZipMode_ = entry.removeBooleanToken();
        
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL && !pdetails.isSetCurrentTableToLocal())
        {
            bCurrent_ = entry.removeBooleanToken();
        }
        
        // players at table
        PokerPlayer player;
        bNewPlayers_ = false;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = (PokerPlayer) state.getObjectNullOkay(entry.removeIntegerToken());
            if (players_[i] == null && player != null && details.getSavePlayers() == SaveDetails.SAVE_DIRTY)
            {
                bNewPlayers_ = true;
            }
            players_[i] = player;
        }
        
        // other player lists
        loadPlayerList(state, entry, waitList_);
        loadPlayerList(state, entry, addedList_);
        loadPlayerList(state, entry, addonList_);
        loadPlayerList(state, entry, rebuyList_);
        loadObserverList(state, entry);

        // NOTE: would like to all setAllComputer() here, but on load the
        // player objects haven't been demarshalled at this point, so we
        // can't query them for isHuman().  That is why we have the lazy
        // init method in isAllComputer()
        bInitAllComputer_ = false;
    }
    
    /**
     * Load into array
     */
    static void loadPlayerList(MsgState state, TokenizedList entry, List<PokerPlayer> list)
    {
        int nNum = entry.removeIntToken();
        list.clear();
        for (int i = 0; i < nNum; i++)
        {
            list.add((PokerPlayer)state.getObject(entry.removeIntegerToken()));
        }
    }

    /**
     * Load observer list - use add/remove observer calls so events are fired
     */
    private void loadObserverList(MsgState state, TokenizedList entry)
    {
        // create list of players loaded
        int nNum = entry.removeIntToken();
        List<PokerPlayer> load = new ArrayList<PokerPlayer>(nNum);
        for (int i = 0; i < nNum; i++)
        {
            load.add((PokerPlayer)state.getObject(entry.removeIntegerToken()));
        }

        // remove observers if not in new list
        PokerPlayer player;
        nNum = getNumObservers();
        for (int i = nNum - 1; i >= 0; i--)
        {
            player = getObserver(i);
            if (!load.contains(player))
            {
                removeObserver(player);
            }
        }

        // add all loaded (no-op if already in list)
        nNum = load.size();
        for (int i = 0; i < nNum; i++)
        {
            player = load.get(i);
            _addObserver(player, false);
        }
    }

    // are there player's new on dirty load (transient) - used for repaint purposes
    private boolean bNewPlayers_ = false;

    /**
     * fire new players loaded event (used so UI can repaint to reflect fact that
     * new players are on table)
     */
    public void gameLoaded()
    {
        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            PokerPlayer player = getPlayer(seat);

            if (player != null)
            {
                player.gameLoaded();
            }
        }

        if (bNewPlayers_)
        {
            firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_NEW_PLAYERS_LOADED, this));
        }
        bNewPlayers_ = false;
    }
}
