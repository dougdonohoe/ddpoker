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
/*
 * PokerPlayer.java
 *
 * Created on November 23, 2002, 1:18 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.server.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerPlayer extends GamePlayer
{
    // info about this player
    private boolean bHuman_;
    private PokerTable table_;
    private int nSeat_;
    private int nChips_;
    private int nChipsAtStart_;
    private Hand hand_ = new Hand();
    private HandSorted handSorted_;
    private String sProfileLocation_;
    private int nPosition_;
    private int nStartingPositionCat_;
    private int nPrize_;
    private int nBuyin_;
    private int nAddon_;
    private int nRebuy_;
    private int nNumRebuy_;
    private int nPendingRebuyChips_;
    private int nPendingRebuyAmount_;
    private int nPendingRebuyCnt_;
    private int nPlace_;
    private boolean bFolded_;
    private int nOddChips_ = 0;
    private boolean bWonChipRace_ = false;
    private boolean bBrokeChipRace_ = false;
    private int nTimeMillis_; // store think bank in millis (first 6 digits ... up to 999 seconds), timeout next 4 (214.7 max)
    private String sKey_;
    private PokerURL url_;
    private PlayerType playerType_ = null;
    private boolean bCardsExposed_ = false;
    private boolean bSittingOut_ = false;
    private boolean bMuckLosing_ = true;
    private boolean bShowWinning_ = false;
    private boolean bAskShowLosing_ = false;
    private boolean bAskShowWinning_ = false;
    private boolean bDisconnected_ = false;
    private int nHandsPlayed_ = 0;
    private int nHandsPlayedLastMove_ = 0;
    private boolean bOnWaitList_ = false;
    private long nWaitListTime_ = 0;
    private boolean bOnlineActivated_;
    private int nHandsDisconnected_ = 0;
    private int nHandsSitout_ = 0;
    private boolean bBooted_ = false;

    // transient info
    private HandInfo handInfo_;
    private PlayerProfile profile_;
    private boolean bLoadProfileNeeded_ = false;
    private String sAllInPerc_ = null;
    private int nAllInWin_ = 0;
    private int nAllInScore_ = 0;
    private int nSimulatedBet_ = 0;
    private long nThinkTankAccessed_ = 0;
    private int nTimeoutMsgSent_ = 0;
    private int nPendingWin_ = 0;
    private boolean bDemoLimit_ = false;

    // online transient info
    private volatile WorkerThread worker_;
    private PokerConnection connection_;
    private int nConnCnt_;
    private Object sendSync_;
    private boolean bRejoining_;
    private volatile long nLastMessageRcvd_;
    private Version version_ = PokerConstants.VERSION;

    /**
     * Empty constructor needed for demarshalling
     */
    public PokerPlayer() {}

    /**
     * Creates a new instance of PokerPlayer
     */
    public PokerPlayer(int id, String sName, boolean bHuman)
    {
        this(null, id, sName, bHuman);
    }

    /**
     * Creates a new instance of PokerPlayer based on a name.  A PlayerProfile
     * is created for the player (for use during game; not saved).
     */
    public PokerPlayer(String sKey, int id, String sName, boolean bHuman)
    {
        super(id, sName);
        bHuman_ = bHuman;
        sKey_ = sKey;
        profile_ = null;
    }

    /**
     * Creates a new instance of PokerPlayer
     */
    public PokerPlayer(String sKey, int id, PlayerProfile profile, boolean bHuman)
    {
        super(id, profile.getName());
        bHuman_ = bHuman;
        sKey_ = sKey;
        setProfile(profile);
    }

    /**
     * Get display name - in online game, adds (ai) to ai players and (demo) to demo players
     * and (host) to the host.
     */
    public String getDisplayName(boolean bOnline)
    {
        return getDisplayName(bOnline, true);
    }

    /**
     * Get display name - in online game, adds (ai) to ai players and (demo) to demo players
     * and (host) to the host. If bLong is false, (d) and (h) are appended instead.
     */
    public String getDisplayName(boolean bOnline, boolean bLong)
    {
        if (!bOnline) return getName();

        String sExtra = bLong ? "" : ".s";

        if (isHost())
        {
            return PropertyConfig.getMessage(isDemo() ? "msg.playername.host.demo"+sExtra:
                                             "msg.playername.host"+sExtra, getName());
        }
        else if (isComputer())
        {
            return PropertyConfig.getMessage("msg.playername.ai", getName());
        }
        else if (isDemo())
        {
            return PropertyConfig.getMessage("msg.playername.demo"+sExtra   , getName());
        }
        else
        {
            return getName();
        }
    }

    /**
     * Set the associated profile.  Throws an error if the current profile has already been loaded.
     */
    public void setProfile(PlayerProfile profile)
    {
        if (profile_ != profile)
        {
            profile_ = profile;
            if (profile_ != null)
            {
                setOnlineActivated(profile_.isActivated());
                setName(profile.getName());

                File file = profile_.getFile();
                if (file != null)
                {
                    sProfileLocation_ = file.getAbsolutePath();
                }
            }
        }
    }

    /**
     * Is player from an activate online profile
     */
    public boolean isOnlineActivated()
    {
        return bOnlineActivated_;
    }

    /**
     * Set whether player is registered online public server.  Only
     * really used when creating player for online,
     * where the remote player may or may not be
     * registered oline
     */
    public void setOnlineActivated(boolean b)
    {
        bOnlineActivated_ = b;
    }

    /**
     * Set PokerAI type.
     */
    public void setPlayerType(PlayerType playerType)
    {
        if (playerType != playerType_)
        {
            playerType_ = playerType;
            createPokerAI();
        }
    }

   /**
    *  Get player type
    */
    public PlayerType getPlayerType()
    {
        return playerType_;
    }

    /**
     * create ai for the player
     */
    public void createPokerAI()
    {
        // never create AI for players who are not seated
        if (table_ == null) return;

        // no player type implies remote player - will be instantiated lazily
        if (playerType_ == null) return;

        // don't create AI for computer-only tables that are not being observed
        if (table_.isAllComputer() && !table_.isCurrent()) return;

        // don't create AI if no ai in tournament and no advisors
        if (!isAIUsed()) return;

        // get existing poker ai
        PokerAI ai = getPokerAI();

        // just set the player type if existing AI is the right class
        if ((ai != null) && (ai.getPlayerType().getAIClassName().equals(playerType_.getAIClassName())))
        {
            if (ai.getPlayerType() != playerType_)
            {
                if (DebugConfig.isTestingOn()) logger.debug("Updating player type on existing AI. (" + playerType_.getName() + ") for player " + getName());
                ai.setPlayerType(playerType_);
            }
        }
        // instantiate a new AI
        else
        {
            if (DebugConfig.isTestingOn()) logger.debug("Creating new AI. (" + playerType_.getName() + ") for player " + getName());
            setPokerAI(PokerAI.createPokerAI(playerType_));
        }
    }

    /**
     * is ai used for this player in this game?
     */
    private boolean isAIUsed()
    {
        PokerGame game = table_.getGame();
        TournamentProfile profile = game.getProfile();
        return !(game.isOnlineGame() &&
                 ((!profile.isFillComputer() && !profile.isAllowAdvisor()) ||
                  (!table_.isCurrent() && !game.getHost().isLocallyControlled())));

    }

    /**
     * Convienence method which casts to PokerAI
     */
    public PokerAI getPokerAI()
    {
        PokerAI ai = (PokerAI) getGameAI();

        if ((ai == null) && (playerType_ == null) && isAIUsed())
        {
            // this is for online game guests
            if (isLocallyControlled() && isHuman() && !isObserver())
            {
                playerType_ = PlayerType.getAdvisor();
                if (DebugConfig.isTestingOn()) logger.debug("Lazily creating advisor: " + playerType_.getName() + " for " + getName());
                setPokerAI(PokerAI.createPokerAI(playerType_));
            }
            else
            {
                if (DebugConfig.isTestingOn()) logger.debug("Lazily instantiating dummy ai for player " + getName());
                ai = new PokerAI();
                ai.init();
                setPokerAI(ai);
            }
        }

        return ai;
    }

    /**
     * Set PokerAI - private because actual instantiation is cleverly managed.
     */
    public void setPokerAI(PokerAI ai)
    {
        if (ai != null)
        {
            PokerPlayer player = ai.getPokerPlayer();

            if ((player != null) && (player != this))
            {
                throw new ApplicationError("PokerAI objects cannot be shared by multiple players.");
            }
        }

        PokerAI old = (PokerAI)getGameAI(); // call getGameAI to avoid infinite recursion

        if (old != ai)
        {
            if (old != null)
            {
                old.setPokerPlayer(null);
            }

            if (ai != null)
            {
                ai.setPokerPlayer(this);
            }

            if (ai == null && DebugConfig.isTestingOn())
            {
                logger.debug("AI cleared for " + getName());
            }

            super.setGameAI(ai);
        }
    }

    public void setGameAI(GameAI ai)
    {
        throw new ApplicationError("Use PokerPlayer.setPokerAI(), which has critical side effects.");
    }

    /**
     * Get key
     */
    public String getKey()
    {
        return sKey_;
    }

    /**
     * Set key
     */
    void setKey(String sKey)
    {
        sKey_ = sKey;
        bLOCAL = null;
    }

    // cache since this is accessed alot and doesn't change
    private Boolean bLOCAL;
    /**
     * Is locally controlled?
     */
    public boolean isLocallyControlled()
    {
        if (bLOCAL == null)
        {
            bLOCAL = getPublicUseKey().equals(sKey_) ? Boolean.TRUE : Boolean.FALSE;
        }
        return bLOCAL;
    }

    private String sPubKey_ = null;
    /**
     * get public use key from engine (cache locally)
     */
    private String getPublicUseKey()
    {
        if (sPubKey_ == null) sPubKey_ = GameEngine.getGameEngine().getPublicUseKey();
        return sPubKey_;
    }

    /**
	** is computer player?
	*/
    public boolean isComputer()
    {
		return !bHuman_;
    }

    /**
     * is human?
     */
    public boolean isHuman()
    {
        return bHuman_;
    }

    /**
     * is human controlled?
     */
    public boolean isHumanControlled() {
        return (!TESTING(PokerConstants.TESTING_AUTOPILOT) && (TESTING(PokerConstants.TESTING_DOUG_CONTROLS_AI) || isHuman()));
    }

    //private static int CNT = 0;
    /**
     * Get player profile, lazily create empty profile if none exists.
     */
    public PlayerProfile getProfile()
    {
        if (profile_ == null)
        {
            profile_ = new PlayerProfile(sName_);
            //logger.debug("Creating profile " + (CNT++) + " for " + sName_);
        }
        return profile_;
    }

    /**
     * Get player profile, init it if required
     */
    public PlayerProfile getProfileInitCheck()
    {
        PlayerProfile profile = getProfile();
        profile.initCheck();
        return profile;
    }

     /**
      * Return if profile is defined - useful since we
      * lazily create the profile.
     */
    public boolean isProfileDefined()
    {
        return profile_ != null;
    }

    /**
     * Get player profile location
     */
    public String getProfilePath()
    {
        return sProfileLocation_;
    }

    /**
     * Set player profile location (used in online games by host only)
     */
    public void setProfilePath(String sPath)
    {
        sProfileLocation_ = sPath;
    }

    /**
     * Get url this player uses to connect to host
     * in online games
     */
    public PokerURL getConnectURL()
    {
        return url_;
    }

    /**
     * Set the url this player uses to connect to host
     * in online games
     */
    public void setConnectURL(PokerURL url)
    {
        url_ = url;
    }

    /**
     * Get total spent
     */
    public int getTotalSpent()
    {
        return getBuyin() + getRebuy() + getAddon();
    }

    /**
     * set amount of buyin
     */
    public void setBuyin(int n)
    {
        nBuyin_ = n;
    }

    /**
     * get amount of buyin
     */
    public int getBuyin()
    {
        return nBuyin_;
    }
    
    /**
     * is player in hand?
     */
    public boolean isInHand()
    {
        HoldemHand hhand = getHoldemHand();
        if (hhand == null) return false;
        if (isFolded()) return false;
        return (hhand.getRound() != HoldemHand.ROUND_SHOWDOWN);
    }

    /**
     * Add a rebuy (increment total rebuy $ and count)
     */
    public void addRebuy(int nAmount, int nChips, boolean bPending)
    {
        if (bPending)
        {
            nPendingRebuyCnt_++;
            nPendingRebuyAmount_ += nAmount;
            nPendingRebuyChips_ += nChips;
        }
        else
        {
            nNumRebuy_++;
            nRebuy_ += nAmount;
            nChips_ += nChips;

            getTable().getGame().chipsBought(nChips);
        }

        if (table_ != null) table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REBUY,
                                                        table_, this, nAmount, nChips, bPending));
    }
    
    /**
     * Handle pending rebuys
     */
    public void addPendingRebuys()
    {
        nNumRebuy_ += nPendingRebuyCnt_;
        nRebuy_ += nPendingRebuyAmount_;
        nChips_ += nPendingRebuyChips_;

        getTable().getGame().chipsBought(nPendingRebuyChips_);

        nPendingRebuyCnt_ = 0;
        nPendingRebuyAmount_ = 0;
        nPendingRebuyChips_ = 0;
    }

    /**
     * Get number of rebuys
     */
    public int getNumRebuys()
    {
        return nNumRebuy_;
    }
    
    /**
     * Get number of pending rebuys
     */
    public int getNumRebuysPending()
    {
        return nPendingRebuyCnt_;
    }
    
    /**
     * Get number of pending rebuy chips
     */
    public int getRebuysChipsPending()
    {
        return nPendingRebuyChips_;
    }

    /**
     * get amount of rebuy
     */
    public int getRebuy()
    {
        return nRebuy_;
    }

    /**
     * set amount of addon
     */
    public void addAddon(int nAmount, int nChips)
    {
        if (nAddon_ > 0) return; // prevent duplicate add-on
        
        nAddon_ += nAmount;
        nChips_ += nChips;

        getTable().getGame().chipsBought(nChips);

        if (table_ != null) table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDON,
                                                        table_, this, nAmount, nChips, false));
    }

    /**
     * get amount of addon
     */
    public int getAddon()
    {
        return nAddon_;
    }

    /**
     * set amount of prize won
     */
    public void setPrize(int n)
    {
        nPrize_ = n;
    }

    /**
     * get amount of prize
     */
    public int getPrize()
    {
        return nPrize_;
    }

    /**
     * set place finished in tournament
     */
    public void setPlace(int n)
    {
        nPlace_ = n;
    }

    /**
     * get place finished in tournament
     */
    public int getPlace()
    {
        return nPlace_;
    }

    /**
     * Get number of chips this player has
     */
    public int getChipCount()
    {
        return nChips_;
    }

    /**
     * Get number of odd chips
     */
    public int getOddChips()
    {
        return nOddChips_;
    }

    /**
     * Set number of odd chips
     */
    public void setOddChips(int n)
    {
        nOddChips_ = n;
    }

    /**
     * Get whether a chip won in chip race
     */
    public boolean isWonChipRace()
    {
        return bWonChipRace_;
    }

    /**
     * Set whether a chip was won in chip race
     */
    public void setWonChipRace(boolean b)
    {
        bWonChipRace_ = b;
    }

    /**
     * Get whether a chip won in chip race to keep from going broke
     */
    public boolean isBrokeChipRace()
    {
        return bBrokeChipRace_;
    }

    /**
     * Set whether a chip was won in chip race to keep from going broke
     */
    public void setBrokeChipRace(boolean b)
    {
        bBrokeChipRace_ = b;
    }

    /**
     * Get number of chips player had at start of last hand.  This
     * is used in resolving ties for prizes.
     */
    public int getChipCountAtStart()
    {
        return nChipsAtStart_;
    }

    /**
     * Set number of chips
     */
    public void setChipCount(int n)
    {
        nChips_ = n;
    }

    /**
     * Called when chip count is manipulated by cheat options, so that AI can properly adjust to new stack size. 
     */
    void adjustChipCountAtStart(int n)
    {
        nChipsAtStart_ += n;
    }

    /**
     * add chips
     */
    public void addChips(int n)
    {
        nChips_ += n;
    }

    /**
     * demo limit
     */
    public void setDemoLimit()
    {
        bDemoLimit_ = true;
    }

    /**
     * is demo limit reached?
     */
    public boolean isDemoLimit()
    {
        return bDemoLimit_;
    }

    // Store timeout and thinkbank in same int (so we don't have
    // to change save format - not accessed on client prior to 2.5)
    // Store thinkbank in first million (as millis) and timeout
    // in digits above a million (as tenths)
    private static final int MILLION = 1000000;

    /**
     * Set timeout millis (stored as tenths, so truncated)
     */
    public void setTimeoutMillis(int n)
    {
        n /= 100; // convert to tenths
        nTimeMillis_ = (n * MILLION) + getThinkBankMillis();
    }

    /**
     * Get timeout millis
     */
    public int getTimeoutMillis()
    {
        return (nTimeMillis_ / MILLION) * 100;
    }

    /**
     * Set think bank millis
     */
    public void setThinkBankMillis(int n)
    {
        nTimeMillis_ = (nTimeMillis_ - getThinkBankMillis()) + n;
    }

    /**
     * Get think bank millis (use int since limited to 10 minutes)
     */
    public int getThinkBankMillis()
    {
        return nTimeMillis_ % MILLION;
    }

    /**
     * set last time thinkbank seconds were changed (transient)
     */
    public void setThinkBankAccessed(long n)
    {
        nThinkTankAccessed_ = n;
    }

    /**
     * Get time thinkbank last accessed
     */
    public long getThinkBankAccessed()
    {
        return nThinkTankAccessed_;
    }

    /**
     * set seconds left when timeout message was sent
     */
    public void setTimeoutMessageSecondsLeft(int n)
    {
        nTimeoutMsgSent_ = n;
    }

    /**
     * Get seconds left when last timeout message was sent
     */
    public int getTimeoutMessageSecondsLeft()
    {
        return nTimeoutMsgSent_;
    }

    /**
     * Set the table we are at (done from PokerTable)
     */
    void setTable(PokerTable table, int nSeat)
    {
        if (table == null) {
            setPokerAI(null);
            table_ = null;
            nSeat_ = PokerTable.NO_SEAT;
            return;
        }

        if (table_ != null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR,
                toString() + " already at table " + table_ + ", but trying to assign to another table: "+
                table, null);
        }
        table_ = table;
        nSeat_ = nSeat;
        nHandsPlayedLastMove_ = nHandsPlayed_;
    }

    /**
     * Get the table we are at
     */
    public PokerTable getTable()
    {
        return table_;
    }

    /**
     * Get the seat we are at
     */
    public int getSeat()
    {
        return nSeat_;
    }

    /**
     * Set the seat we are at (used only by PokerDatabase)
     */
    public void setSeat(int nSeat)
    {
        nSeat_ = nSeat;
    }

    /**
     * Get the current hand
     */
    public Hand getHand()
    {
        return hand_;
    }

    /**
     * Get sorted hand (ascending) - for use in AI and other stats work
     */
    public HandSorted getHandSorted()
    {
        if ((hand_ != null) && ((handSorted_ == null) || (handSorted_.fingerprint() != hand_.fingerprint())))
        {
            handSorted_ = new HandSorted(hand_);
        }
        return handSorted_;
    }

    /**
     *  Remove players hand
     */
    public void removeHand()
    {
        hand_ = null;
        handSorted_ = null;
        handInfo_ = null;
    }

    /*
     * Get new hand (old one discarded)
     */
    public Hand newHand(char cType)
    {
        removeHand();
        hand_ = new Hand(cType);
        if (cType == Hand.TYPE_NORMAL) nHandsPlayed_ ++;
        nLastCalcFingerprint_ = -1;
        nLastCalcPotRound_ = -1;
        nChipsAtStart_ = nChips_; // track for resolving ties when exiting tournament
        sAllInPerc_ = null;
        bFolded_ = false;
        bCardsExposed_ = false;
        if (table_ != null) setThinkBankMillis(table_.getGame().getProfile().getThinkBankSeconds() * 1000);
        setShowWinning(false);
        setMuckLosing(true);

        return getHand();
    }

    /**
     * init for simulated hand, similar to newHand() init
     */
    public void newSimulatedHand()
    {
        removeHand();
        bFolded_ = false;
        nSimulatedBet_ = 0;
        nChipsAtStart_ = nChips_;
    }

    /**
     * end hand housekeeping (sit-out/disconnect count)
     */
    public void endHand()
    {
        if (isDisconnected()) nHandsDisconnected_++;
        else if (isSittingOut()) nHandsSitout_++;
    }

    /**
     * Set all in percentage display
     */
    public void setAllInPerc(String s)
    {
        sAllInPerc_ = s;
    }

    /**
     * Get all in percentage display
     */
    public String getAllInPerc()
    {
        return sAllInPerc_;
    }

    /**
     * increment all-in win
     */
    public void addAllInWin()
    {
        nAllInWin_++;
    }

    /**
     * Get all-in win
     */
    public int getAllInWin()
    {
        return nAllInWin_;
    }

    /**
     * Clear all-in win
     */
    public void clearAllInWin()
    {
        nAllInWin_ = 0;
    }

    /**
     * Set all-in score
     */
    public void setAllInScore(int n)
    {
        nAllInScore_ = n;
    }

    /**
     * Get all-in score
     */
    public int getAllInScore()
    {
        return nAllInScore_;
    }

    /**
     * Fold
     */
    public void fold(String sDebug, int nFoldType)
    {
        setFolded(true); // set before notifying hand due to listeners that may query player
        getHoldemHand().fold(this, sDebug, nFoldType);
    }

    /**
     * Set folded, without side effects on the hand.
     */
    public void setFolded(boolean bFolded)
    {
        bFolded_ = bFolded;
    }

    /**
     * Is folded
     */
    public boolean isFolded()
    {
        return bFolded_;
    }

    /**
     * Set whether player exposed cards
     */
    public void setCardsExposed(boolean b)
    {
        bCardsExposed_ = b;
    }

    /**
     * Were cards exposed for current hand?
     */
    public boolean isCardsExposed()
    {
        return bCardsExposed_;
    }

    /**
     * Set whether player is sitting out
     */
    public void setSittingOut(boolean b)
    {
        if (b != bSittingOut_)
        {
            bSittingOut_ = b;
            fireSettingsChanged();
        }
    }

    /**
     * Is player sitting out?
     */
    public boolean isSittingOut()
    {
        return bSittingOut_;
    }

    /**
     * Set whether player mucks losing hands
     */
    public void setMuckLosing(boolean b)
    {
        if (b != bMuckLosing_)
        {
            bMuckLosing_ = b;
            fireSettingsChanged();
        }
    }

    /**
     * Is player mucking losing hands?
     */
    public boolean isMuckLosing()
    {
        return bMuckLosing_;
    }

    /**
     * Set whether player shows winning hands (when uncontested)
     */
    public void setShowWinning(boolean b)
    {
        if (b != bShowWinning_)
        {
            bShowWinning_ = b;
            fireSettingsChanged();
        }
    }

    /**
     * Is player mucking showing winning hands (when uncontested)?
     */
    public boolean isShowWinning()
    {
        return bShowWinning_;
    }

    /**
     * Set whether player should be asked to show losing hand
     */
    public void setAskShowLosing(boolean b)
    {
        if (b != bAskShowLosing_)
        {
            bAskShowLosing_ = b;
            fireSettingsChanged();
        }
    }

    /**
     * Should player be asked to showing losing hands?
     */
    public boolean isAskShowLosing()
    {
        return bAskShowLosing_;
    }

    /**
     * Set whether player should be asked to show winning hand
     */
    public void setAskShowWinning(boolean b)
    {
        if (b != bAskShowWinning_)
        {
            bAskShowWinning_ = b;
            fireSettingsChanged();
        }
    }

    /**
     * Should player be asked to showing winning hands (when uncontested)?
     */
    public boolean isAskShowWinning()
    {
        return bAskShowWinning_;
    }

    /**
     * Set whether player is disconnected
     */
    public void setDisconnected(boolean b)
    {
        bDisconnected_ = b;
    }

    /**
     * Is player disconnected?
     */
    public boolean isDisconnected()
    {
        return bDisconnected_;
    }

    /**
     * Get number of hands played
     */
    public int getHandsPlayed()
    {
        return nHandsPlayed_;
    }

    /**
     * Get number of hands played when last moved -
     * this is used to sort my most recently moved
     */
    public int getHandsPlayedAtLastMove()
    {
        return nHandsPlayedLastMove_;
    }

    /**
     * get number of hands played while disconnected
     */
    public int getHandsPlayedDisconnected()
    {
        return nHandsDisconnected_;
    }

    /**
     * get number of hands played while disconnected
     */
    public int getHandsPlayedSitout()
    {
        return nHandsSitout_;
    }

    /**
     * return if this player was booted
     */
    public boolean isBooted()
    {
        return bBooted_;
    }

    /**
     * set that this player is booted
     */
    public void setBooted(boolean b)
    {
        bBooted_ = b;
    }

    /**
     * return if this player is waiting to be seated
     */
    public boolean isWaiting()
    {
        return bOnWaitList_;
    }

    /**
     * set that this player is waiting for a seat
     */
    public void setWaiting(boolean b)
    {
        bOnWaitList_ = b;
        nWaitListTime_ =  bOnWaitList_ ? Utils.getCurrentTimeStamp() : 0;
        if (bOnWaitList_) removeHand();
    }

    /**
     * Get timestamp when player was added to wait list
     */
    public long getWaitListTimeStamp()
    {
        return nWaitListTime_;
    }

    /**
     * return true if player bet something (other than ante), it is the showdown,
     * and the show folded option is true
     */
    public boolean showFoldedHand()
    {
        boolean bShowFold = PokerUtils.isCheatOn(getTable().getGame().getGameContext(), PokerConstants.OPTION_CHEAT_SHOWFOLD);
        HoldemHand hhand = getHoldemHand();
        return
            bShowFold &&
            hhand != null &&
            hhand.getRound() == HoldemHand.ROUND_SHOWDOWN &&
            (isHuman() || (hhand.getTotalBet(this) - hhand.getAnteSmallBlind(this)) > 0);
    }

    public void processAction(HandAction action)
    {
        // process it (we do it this way because we might want to get the
        // action w/out processing it - i.e., for help or hints)
        switch (action.getAction())
        {
            case HandAction.ACTION_FOLD:
                fold(action.getDebug(), action.getSubAmount());
                break;

            case HandAction.ACTION_CHECK:
                check(action.getDebug());
                break;

            case HandAction.ACTION_CHECK_RAISE:
                checkraise(action.getDebug());
                break;

            case HandAction.ACTION_BET:
                bet(action.getAmount(), action.getDebug());
                break;

           case HandAction.ACTION_CALL:
                call(action.getDebug());
                break;

           case HandAction.ACTION_RAISE:
                raise(action.getAmount(), action.getDebug());
                break;

            default:
                ApplicationError.assertTrue(false, "Unknown HandAction action: " + action.getAction());
        }
    }

    /**
     * ante
     */
    public void ante(int nAmount)
    {
        if (nAmount > nChips_) nAmount = nChips_;
        HoldemHand hhand = getHoldemHand();
        nChips_ -= nAmount;
        hhand.ante(this, nAmount);
    }

    /**
     * small blind
     */
    public void smallblind(int nAmount)
    {
        if (nAmount > nChips_) nAmount = nChips_;
        HoldemHand hhand = getHoldemHand();
        nChips_ -= nAmount;
        // only record blind if amount > 0 - this check
        // because player could have gone all-in on the
        // ante
        if (nAmount > 0) hhand.smallblind(this, nAmount);
    }

    /**
     * bigblind
     */
    public void bigblind(int nAmount)
    {
        if (nAmount > nChips_) nAmount = nChips_;
        HoldemHand hhand = getHoldemHand();
        nChips_ -= nAmount;
        // only record blind if amount > 0 - this check
        // because player could have gone all-in on the
        // ante
        if (nAmount > 0) hhand.bigblind(this, nAmount);
    }

    /**
     * Bet
     */
    public void bet(int nAmount, String sDebug)
    {
        if (nAmount > nChips_) nAmount = nChips_;
        HoldemHand hhand = getHoldemHand();

        // safety check:
        // if asking to bet, but we need to call, make
        // the right call instead
        int nCall = hhand.getCall(this);
        if (nCall != 0)
        {
            hhand.debugPrint();
            throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Attempt to bet $"+nAmount+
                                                                 "when a call of $"+nCall+" needs to be made", sDebug, null);
        }

        // can't bet more than the max
        int nMaxBet = hhand.getMaxBet(this);
        if (nAmount > nMaxBet) nAmount = nMaxBet;

        nChips_ -= nAmount;
        hhand.bet(this, nAmount, sDebug);
    }

    /**
     * Bet for testing (less error checking)
     */
    public void betTest(int nAmount)
    {
        if (nAmount > nChips_) nAmount = nChips_;
        HoldemHand hhand = getHoldemHand();

        nChips_ -= nAmount;
        hhand.bet(this, nAmount, null);
    }

    /**
     * Raise amount (this is added to the amount we need to call)
     */
    public void raise(int nAmount, String sDebug)
    {
        // get hand and call amount
        HoldemHand hhand = getHoldemHand();
        int nCall = hhand.getCall(this);

        // if only enough chips to call, then just call
        if (nCall >= nChips_)
        {
            call(sDebug);
            return;
        }

        // safety check:
        // if nothing to call and we haven't bet yet, make this a bet instead
        if (nCall == 0 && hhand.getBet(this) == 0)
        {
            bet(nAmount, sDebug);
            return;
        }

        // see what we will have left to raise after calling
        int nChipsAfterCall = nChips_ - nCall;
        if (nAmount > nChipsAfterCall) nAmount = nChipsAfterCall;

        // can't raise more than the max less already bet
        int nMaxRaise = hhand.getMaxRaise(this);
        if (nAmount > nMaxRaise) nAmount = nMaxRaise;

        // make sure raise is at least min raise (taking into account chips left after call)
        int nMinRaise = hhand.getMinRaise();
        if (nAmount < nMinRaise && nAmount < nChipsAfterCall && nAmount > 0 && nAmount < nMaxRaise)
        {
            hhand.debugPrint();
            throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Attempt to raise $"+nAmount+
                                                                 " which is less than min raise of $"+nMinRaise,
                                                                 sDebug, null);
        }

        // if nothing to raise, just call (<= is a safety check)
        if (nAmount <= 0)
        {
            call(sDebug);
            return;
        }

        nChips_ -= (nCall + nAmount);
        hhand.raise(this, nCall, nAmount, sDebug);
    }

    /**
     * Call
     */
    public void call(String sDebug)
    {
        HoldemHand hhand = getHoldemHand();
        int nAmount = hhand.getCall(this);

        // if no call, this is a check
        if (nAmount == 0)
        {
            check(sDebug);
            return;
        }

        if (nAmount > nChips_) nAmount = nChips_;
        nChips_ -= nAmount;
        hhand.call(this, nAmount, sDebug);
    }

    /**
     * check
     */
    public void check(String sDebug)
    {
        HoldemHand hhand = getHoldemHand();
        int nAmount = hhand.getCall(this);
        ApplicationError.assertTrue(nAmount == 0, "Checking with bet to call", sDebug);
        hhand.check(this, sDebug);
    }

    /**
     * checkraise
     */
    public void checkraise(String sDebug)
    {
        HoldemHand hhand = getHoldemHand();
        hhand.checkraise(this, sDebug);
    }

    /**
     * Wins pot
     */
    public void wins(int nAmount, int nPot)
    {
        HoldemHand hhand = getHoldemHand();
        nChips_ += nAmount;
        hhand.wins(this, nAmount, nPot);
    }

    /**
     * Overbet returned
     */
    public void overbet(int nAmount, int nPot)
    {
        HoldemHand hhand = getHoldemHand();
        nChips_ += nAmount;
        hhand.overbet(this, nAmount, nPot);
    }

    /**
     * Loses pot
     */
    public void lose(int nPot)
    {
        HoldemHand hhand = getHoldemHand();
        hhand.lose(this, nPot);
    }

    /**
     * transient - used during pot resolution
     */
    void setPendingWin(int nAmount)
    {
        nPendingWin_ = nAmount;
    }

    /**
     * transient - get pending win
     */
    int getPendingWin()
    {
        return nPendingWin_;
    }

    /**
     * is this player all in?
     */
    public boolean isAllIn()
    {
        return nChips_ == 0;
    }

    /**
     * Get holdem hadn
     */
    public HoldemHand getHoldemHand()
    {
        if (table_ == null) return null;
        return table_.getHoldemHand();
    }

    /**
     * Get hand info (lazy creation)
     */
    public HandInfo getHandInfo()
    {
        if (handInfo_ == null)
        {
            HandSorted sorted = getHandSorted();
            HoldemHand hhand = getHoldemHand();
            if (sorted != null && hhand != null)
            {
                handInfo_ = new HandInfo(this, sorted, hhand.getCommunitySorted());
            }
        }
        return handInfo_;
    }

    ////
    //// In game logic
    ////

    // position categories - start at 0 for use in arrays
    public static final int EARLY = 0;
    public static final int MIDDLE = 1;
    public static final int LATE = 2;
    public static final int SMALL = 3;
    public static final int BIG = 4;

    /**
     * Get name for debugging
     */
    public static String getPositionName(int n)
    {
        switch (n)
        {
            case EARLY: return "early";
            case MIDDLE: return "middle";
            case LATE:     return "late";
            case SMALL:    return "small";
            case BIG: return "big";
                default: return "none";
        }
    }

    /**
     * Set position of this player (from HoldemHand.setPlayerOrder()).
     * Value ranges from 1 to # players
     */
    void setPosition(int n, int nRound)
    {
        nPosition_ = n;
        if (nRound == HoldemHand.ROUND_PRE_FLOP)
        {
            nStartingPositionCat_ = getPositionCategory();
        }
    }

    /**
     * Get position at table
     */
    int getPosition()
    {
        return nPosition_;
    }

    /**
     * Get starting position category
     */
    public int getStartingPositionCategory()
    {
        return nStartingPositionCat_;
    }

    /**
     * Get position debug style
     */
    public String getPositionDebug()
    {
        HoldemHand hand = getHoldemHand();
        if (hand == null) return "?";

        int nNumPlayers = hand.getNumPlayers();

        if (isEarly()) return "E";
        if (isMiddle()) return "M";
        if (isLate()) return "L";
        if (isSmallBlind()) return "S";
        if (isBigBlind()) return "B";

        ApplicationError.assertTrue(false, "Bad position: " + nPosition_ + " numplayers: " + nNumPlayers);
        return null;
    }

    /**
     * get early,middle,late,etc - pre-flop use only
     */
    public int getPositionCategory()
    {
        int nNumPlayers = getTable().getNumOccupiedSeats();
        if (nNumPlayers - nPosition_ >= 7) return EARLY;

        if (nNumPlayers - nPosition_ >= 4) return MIDDLE;

        if (nNumPlayers - nPosition_ >= 2) return LATE;

        if (nNumPlayers == 2) return LATE;

        if (nPosition_ == (nNumPlayers - 1)) return SMALL;

        if (nPosition_ == nNumPlayers) return BIG;

        throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "No position", null);
    }

    /**
     * is this player in an early position?
     */
    public boolean isEarly()
    {
        return getStartingPositionCategory() == EARLY;
    }

    /**
     * is this player in a middle position?
     */
    public boolean isMiddle()
    {
        return getStartingPositionCategory() == MIDDLE;
    }

    /**
     * is this player in a middle position?
     */
    public boolean isLate()
    {
        return getStartingPositionCategory() == LATE;
    }

    /**
     * is this player a blind?
     */
    public boolean isBlind()
    {
        return isSmallBlind() || isBigBlind();
    }

    /**
     * Is this player the small blind?
     */
    public boolean isSmallBlind()
    {
        return getStartingPositionCategory() == SMALL;
    }

    /**
     * is this player the big blind?
     */
    public boolean isBigBlind()
    {
        return getStartingPositionCategory() == BIG;
    }

    /**
     * Return action AI would take.  This can be called for a human player
     * to get a "hint" if desired.
     */
    public HandAction getAction(boolean bQuick)
    {
        try
        {
//            // TODO - TESTING - make first AI to act raise all in
//            if (getHoldemHand().getRound() == HoldemHand.ROUND_PRE_FLOP &&
//                getHoldemHand().getRaiser() == null)
//            {
//                return new HandAction(this, getHoldemHand().getRound(), HandAction.ACTION_RAISE, Integer.MAX_VALUE, "testing");
//            }
            return getPokerAI().getHandAction(bQuick);
        }
        catch (Throwable e)
        {
            logger.error("AI exception caught. Return 'fold' to keep the game going:\n"+
                         Utils.formatExceptionText(e));
            return new HandAction(this, getHoldemHand().getRound(), HandAction.ACTION_FOLD, "aierror");
        }
    }

    /**
     * add to simulate bet, deduct from chips, return actual bet
     */
    public int addSimulatedBet(int n)
    {
        if (n > nChips_) n = nChips_;
        nSimulatedBet_ += n;
        nChips_ -= n;
        return n;
    }

    /**
     * Get simulated bet
     */
    public int getSimulatedBet()
    {
        return nSimulatedBet_;
    }

    // formatting
    static Format fName = new Format("%-11s");
    static Format fStringLong = new Format("%-20s");
    static Format fChip = new Format("%7d");

    /**
     * Debugging - show player and hand and chips
     */
    public String toString()
    {
        return toStringShort();
    }

    /**
     * long form, formatted
     */
    public String toStringLong()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(fName.form(getName() != null ? getName() : "[null]"));
        if (hand_ != null) sb.append(fName.form(hand_.toString()));
        else sb.append("[no hand]");
        sb.append(" $");
        sb.append(fChip.form(nChips_));
        sb.append(" <");
        sb.append(getPositionDebug());
        sb.append(">");
        return sb.toString();
    }

    /**
     * Debugging - show player and hand and chips
     */
    public String toStringShort()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" ");
        if (hand_ != null) sb.append(hand_.toString());
        else sb.append("[no hand]");
        sb.append(" $").append(nChips_);
        return sb.toString();
    }

    ////
    //// Game save logic
    ////

    /**
     * Add extra items to entry
     */
    protected void addExtraToGameStateEntry(GameState state, GameStateEntry entry)
    {
        SaveDetails details = state.getSaveDetails();
        PokerSaveDetails pdetails = (PokerSaveDetails) details.getCustomInfo();

        entry.addToken(bHuman_);
        entry.addToken(state.getId(table_));
        entry.addToken(nSeat_);
        entry.addToken(nChips_);
        entry.addToken(nChipsAtStart_);

        // POKER 2.0 - handle case when have to hide cards of players other than the
        // specific one we are sending data to (online games)
        Hand hand = hand_;
        if (hand_ != null &&
            pdetails.isHideOthersCards() &&
            pdetails.getPlayerID() != getID() &&
            hand_.getType() == Hand.TYPE_NORMAL)
        {
            HoldemHand hhand = getHoldemHand();

            // hide cards if
            //  + this hand isn't in an all-in-showown AND
            //     + hand folded OR
            //     + not showdown OR
            //     + showdown and cards not exposed OR
            //
            if (hhand != null && !(hhand.isAllInShowdown() && !isFolded()) &&
                (isFolded() ||
                  hhand.getRound() != HoldemHand.ROUND_SHOWDOWN ||
                  (hhand.getRound() == HoldemHand.ROUND_SHOWDOWN && !isCardsExposed())
                 )
               )
            {
                hand = new Hand(hand_.getType());
                int i = 0;
                while (i < hand_.size())
                {
                    hand.addCard(Card.BLANK);
                    i++;
                }
            }
        }
        entry.addToken(hand);
        // don't store handSorted_

        entry.addToken(nPosition_);
        entry.addToken(nStartingPositionCat_);
        entry.addToken(nPrize_);
        entry.addToken(nBuyin_);
        entry.addToken(nAddon_);
        entry.addToken(nRebuy_);
        entry.addToken(nNumRebuy_);
        entry.addToken(nPendingRebuyAmount_);
        entry.addToken(nPendingRebuyChips_);
        entry.addToken(nPendingRebuyCnt_);
        entry.addToken(nPlace_);
        entry.addToken(bFolded_);
        entry.addToken(nOddChips_);
        entry.addToken(bWonChipRace_);
        entry.addToken(bBrokeChipRace_);
        entry.addToken(nTimeMillis_);
        entry.addToken(bCardsExposed_);
        entry.addToken(bSittingOut_);
        entry.addToken(bDisconnected_);
        entry.addToken(nHandsPlayed_);

        // profile
        entry.addToken(sProfileLocation_);

        // Poker 2.0
        entry.addToken(sKey_);
        entry.addToken(url_ == null ? null : url_.toString());
        if (details.getSaveAI() == SaveDetails.SAVE_ALL)
        {
            entry.addToken(playerType_ == null ? null : playerType_.getUniqueKey());
        }
        else
        {
            entry.addToken((String) null);
        }

        // more settings
        entry.addToken(bMuckLosing_);
        entry.addToken(bShowWinning_);
        entry.addToken(bOnlineActivated_);
        entry.addToken(bAskShowWinning_);
        entry.addToken(bAskShowLosing_);

        // Poker 2.0 Patch 2
        entry.addToken(nHandsPlayedLastMove_);
        entry.addToken(bOnWaitList_);
        entry.addToken(nWaitListTime_);

        // Poker 2.0 Patch 8
        entry.addToken(nHandsDisconnected_);
        entry.addToken(nHandsSitout_);
        entry.addToken(bBooted_);
    }

    /**
     * Get extra items from entry
     */
    protected void loadExtraFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        SaveDetails details = state.getSaveDetails();
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        bHuman_ = entry.removeBooleanToken();
        table_ = (PokerTable) state.getObject(entry.removeIntegerToken());
        nSeat_ = entry.removeIntToken();
        nChips_ = entry.removeIntToken();
        nChipsAtStart_ = entry.removeIntToken();
        hand_ = (Hand) entry.removeToken();
        handSorted_ = null; // new hand, so null out (created on demand)
        handInfo_ = null; // new hand, so null out (created on demand)
        nPosition_ = entry.removeIntToken();
        nStartingPositionCat_ = entry.removeIntToken();
        nPrize_ = entry.removeIntToken();
        nBuyin_ = entry.removeIntToken();
        nAddon_ = entry.removeIntToken();
        nRebuy_ = entry.removeIntToken();
        nNumRebuy_ = entry.removeIntToken();
        nPendingRebuyAmount_ = entry.removeIntToken();
        nPendingRebuyChips_ = entry.removeIntToken();
        nPendingRebuyCnt_ = entry.removeIntToken();
        nPlace_ = entry.removeIntToken();
        bFolded_ = entry.removeBooleanToken();
        nOddChips_ = entry.removeIntToken();
        bWonChipRace_ = entry.removeBooleanToken();
        bBrokeChipRace_ = entry.removeBooleanToken();
        nTimeMillis_ = entry.removeIntToken();
        bCardsExposed_ = entry.removeBooleanToken();
        bSittingOut_ = entry.removeBooleanToken();
        bDisconnected_ = entry.removeBooleanToken(); // NOTE: this flag is adjusted by PokerGame after load
        nHandsPlayed_ = entry.removeIntToken();

        // profile
        sProfileLocation_ = entry.removeStringToken();

        // Poker 2.0
        String url;
        sKey_ = entry.removeStringToken();
        url_ = ((url = entry.removeStringToken()) == null) ? null : new PokerURL(url);
        String sPlayerTypeKey = entry.removeStringToken();
        if (details.getSaveAI() == SaveDetails.SAVE_ALL)
        {
            playerType_ = sPlayerTypeKey == null ? null : PlayerType.getByUniqueKey(sPlayerTypeKey, pdetails);
        }

        // more settings
        bMuckLosing_ = entry.removeBooleanToken();
        bShowWinning_ = entry.removeBooleanToken();
        bOnlineActivated_ = entry.removeBooleanToken();
        bAskShowWinning_ = entry.removeBooleanToken();
        bAskShowLosing_ = entry.removeBooleanToken();

        // Poker 2.0 Patch 2
        if (entry.hasMoreTokens())
        {
            nHandsPlayedLastMove_ = entry.removeIntToken();
            bOnWaitList_ = entry.removeBooleanToken();
            nWaitListTime_ = entry.removeLongToken();

            // Poker 2.0 Patch 8
            if (entry.hasMoreTokens())
            {
                nHandsDisconnected_ = entry.removeIntToken();
                nHandsSitout_ = entry.removeIntToken();
                bBooted_ = entry.removeBooleanToken();
            }
        }

        // loadProfile() call moved to PokerGame.gameLoaded() as of 2.5
        // this flag is used to only load profiles for players actually
        // loaded
        bLoadProfileNeeded_ = true;
    }

    /**
     * load player profile
     */
    void loadProfile(GameState state)
    {
        if (!bLoadProfileNeeded_) return;
        bLoadProfileNeeded_ = false;

        String sKey = getPublicUseKey();
        // only do profile load if the players key matches the
        // games key (if it doesn't, this is a remote player)
        // don't load profile if we already have one (online game performance
        // tweak since players get loaded many times and the profile won't
        // change during a game)
        if (sProfileLocation_ != null && sKey.equals(sKey_) && profile_ == null)
        {
            File file = new File(sProfileLocation_);

            // if no file, first look in regular save location
            // (in case it moved due to Patch 2)
            if (!file.exists())
            {
                int nLastSep = sProfileLocation_.lastIndexOf(File.separatorChar);
                if (nLastSep != -1)
                {
                    String sName = sProfileLocation_.substring(nLastSep + 1);
                    File nfile = PlayerProfile.getProfileFile(sName);
                    String sNew = nfile.getAbsolutePath();
                    if (!sNew.equals(sProfileLocation_))
                    {
                        logger.warn("Player profile missing: " + sProfileLocation_  +"; change to: " + sNew);
                        sProfileLocation_ = sNew;
                        file = nfile;
                    }
                }
            }

            // if no file, it was deleted.  Use empty one.
            if (!file.exists())
            {
                logger.warn("Player profile missing: " + sProfileLocation_);
            }
            else
            {
                if (DebugConfig.isTestingOn()) logger.debug("Loading file: "+file.getAbsolutePath());
                profile_ = new PlayerProfile(file, true);
                setName(profile_.getName()); // update name
            }
        }

        // do AI check here because isLocallyControlled() driven off of key.
        // If the poker AI is not null and this is a practice game load,
        // check to see if human's advisor needs to be updated.
        boolean bOnline = state.getFile() == null;
        GameAI gameAI = getGameAI();
        if (gameAI != null && !bOnline)
        {
            if (isLocallyControlled() && isHuman())
            {
                PokerAI pokerAI = getPokerAI();
                PlayerType playerType = pokerAI.getPlayerType();
                PlayerType preferredAdvisor = PlayerType.getAdvisor();

                if (playerType.compareTo(preferredAdvisor) != 0)
                {
                    setPlayerType(preferredAdvisor);
                }
            }
        }
    }

    //////
    ////// AI stats
    //////

    private float nStrength_;
    private int nNumStraights_;
    private long nLastCalcFingerprint_ = 0;

    /**
     * hand strength - return -1 if this player is folded, or
     * there is no hand, or if no community cards are out
     */
    public float getHandStrength()
    {
        HoldemHand hhand = getHoldemHand();
        if (hhand == null) return -1;

        // if folded
        if (isFolded()) return -1;

        // if too early a round, indicate
        HandSorted comm = hhand.getCommunitySorted();
        if (comm.size() < 3) return -1;

        long fingerprint = comm.fingerprint() | getHand().fingerprint();
        // if already calc'd this round, return it
        if (fingerprint == nLastCalcFingerprint_) return nStrength_;

        // new calc
        HandStrength hs = new HandStrength();
        nStrength_ = hs.getStrength(getHandSorted(), comm, hhand.getNumWithCards() - 1);
        nNumStraights_ = hs.getNumStraights();
        nLastCalcFingerprint_ = fingerprint;

        return nStrength_;
    }

    /**
     * hand strength ancilliary - return number of straights made by
     * opponents during last call to getHandStrength()
     */
    public int getOppNumStraights()
    {
        return nNumStraights_;
    }

    private float nPotential_;
    private int nLastCalcPotRound_ = -1;

    /**
     * hand strength - return -1 if this player is folded, or
     * there is no hand, or if no community cards are out
     */
    public float getHandPotential()
    {
        HoldemHand hhand = getHoldemHand();
        if (hhand == null) return -1;

        // if folded
        if (isFolded()) return -1;

        // only do potential after flop & turn
        int nRound = hhand.getRound();
        if (nRound != HoldemHand.ROUND_FLOP &&
            nRound != HoldemHand.ROUND_TURN) return -1;

        // if already calced this round, return it
        if (nRound == nLastCalcPotRound_) return nPotential_;

        // do calc
        nPotential_ = HandPotential.getPotential(getHandSorted(), hhand.getCommunitySorted());
        nLastCalcPotRound_ = hhand.getRound();

        return nPotential_;
    }

    /**
     * Hand potential - debug (only returns if calc'd
     */
    public float getHandPotentialDisplay()
    {
        HoldemHand hhand = getHoldemHand();
        if (hhand == null) return -1;

        // if folded
        if (isFolded()) return -1;

        // only do potential after flop & turn
        int nRound = hhand.getRound();
        if (nRound != HoldemHand.ROUND_FLOP &&
            nRound != HoldemHand.ROUND_TURN) return -1;

        return nPotential_;
    }

    /**
     * Get effective hand strength
     */
    public float getEffectiveHandStrength()
    {
        float hs = getHandStrength();
        return hs + (1 - hs) * getHandPotential();
    }

    // instances for sorting
    public static final Comparator<PokerPlayer> SORTBYNAME = new SortName();

    // sort players by chips they have at start of hand
    private static class SortName implements Comparator<PokerPlayer>
    {
        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         */
        public int compare(PokerPlayer p1, PokerPlayer p2)
        {
            return p1.getName().compareTo(p2.getName());
        }
    }

    /////
    ///// Online
    /////

    /**
     * For online games, the host uses this to store the connection
     * through which this player is connected.  For locally
     * controlled players on the host, this is null.  For all
     * players on remote clients, this is null
     */
    public PokerConnection getConnection()
    {
        return connection_;
    }

    /**
     * Set socket used by this player
     */
    public void setConnection(PokerConnection connection)
    {
        clearMessageReceived(); // reset timestamp when socket changes
        connection_ = connection;
        setDisconnected(connection_ == null); // note connected status (which is marshalled; socket is not)
        if (connection_ != null) nConnCnt_++; // track number of connections made (discos equal this less 1)
    }

    /**
     * Add to number of disconnections
     */
    public int addDisconnect()
    {
        return nConnCnt_++;
    }

    /**
     * Get number of disconnections
     */
    public int getDisconnects()
    {
        if (isHost() && isLocallyControlled()) return 0;
        return nConnCnt_ - 1;
    }

    /**
     * set rejoining flag - used to handle "re-integration" of player
     */
    public void setRejoining(boolean b)
    {
        bRejoining_ = b;
    }
    
    /**
     * Is rejoining?
     */
    public boolean isRejoining()
    {
        return bRejoining_;
    }

    /**
     * Get object to sync on for player sends
     */
    public Object getSendSync()
    {
        if (sendSync_ == null) sendSync_ = new Object();
        return sendSync_;
    }

    /**
     * get time last message received from this player
     */
    public long getLastMessageReceivedMillis()
    {
        return nLastMessageRcvd_;
    }

    /**
     * note time message received
     */
    public void setMessageReceived()
    {
        nLastMessageRcvd_ = System.currentTimeMillis();
    }

    /**
     * clear time message received
     */
    public void clearMessageReceived()
    {
        nLastMessageRcvd_ = 0;
    }

    /**
     * Store version of DD this player is using
     */
    public void setVersion(Version v)
    {
        version_ = v;
    }

    /**
     * Get version of DD this player is using
     */
    public Version getVersion()
    {
        return version_;
    }
    
    /**
     * Set thread currently sending message to this player
     */
    public void setSenderThread(WorkerThread worker)
    {
        worker_  = worker;
    }

    /**
     * Get thread currently sending messages to this player
     */
    public WorkerThread getSenderThread()
    {
        return worker_;
    }


    ///
    /// Player prefs - we use separate marshal/unmarshal to
    /// send updates to host because we don't want to send
    /// updates from client to host and override the most
    /// recent data
    ///

    /**
     * marshal player-settings to string
     */
    public String getOnlineSettings()
    {
        TokenizedList list = new TokenizedList();
        list.addToken(bSittingOut_);
        list.addToken(bMuckLosing_);
        list.addToken(bShowWinning_);
        list.addToken(bAskShowWinning_);
        list.addToken(bAskShowLosing_);
        return list.marshal(null);
    }

    /**
     * unmarshal player-settings from string
     */
    public void setOnlineSettings(String s)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(null, s);

        bSittingOut_ = list.removeBooleanToken();
        bMuckLosing_ = list.removeBooleanToken();
        bShowWinning_ = list.removeBooleanToken();
        bAskShowWinning_ = list.removeBooleanToken();
        bAskShowLosing_ = list.removeBooleanToken();

        // notify settings changed
        fireSettingsChanged();
    }

    /**
     * Fire settings changed
     */
    public void fireSettingsChanged()
    {
        if (table_ != null)
        {
            table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_SETTINGS_CHANGED, table_, this, getSeat()));
        }
    }

    /**
     * Game loaded - notify AI
     */
    public void gameLoaded()
    {
        PokerAI ai = getPokerAI();

        if (ai != null)
        {
            ai.gameLoaded();
        }
    }

    // debug
    @SuppressWarnings({"PublicField"})
    public int nGoodMsgs_;

    @SuppressWarnings({"PublicField"})
    public int nBadMsgs_;

    public OpponentModel getOpponentModel()
    {
        PokerAI ai = getPokerAI();

        if (ai != null)
        {
            return ai.getOpponentModel();
        }
        else
        {
            return null;
        }
    }
}
