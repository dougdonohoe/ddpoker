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
 * HoldemHand.java
 *
 * Created on January 5, 2004, 8:46 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.model.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
@SuppressWarnings({"SynchronizeOnNonFinalField"})
@DataCoder('*')
public class HoldemHand implements DataMarshal
{
    // DESIGN NOTE:  added synchronized around any pots_/history_ to avoid concurrent modification
    // error when drawing UI (for v3.0p1).  Sigh.  This whole poker engine needs a re-write, with
    // unit tests!
    //

    static Logger logger = LogManager.getLogger(HoldemHand.class);

    private boolean bStoredInDatabase_ = false;

    // must be in order (see advanceRound())
    public static final int ROUND_NONE = -1;
    public static final int ROUND_PRE_FLOP = 0;
    public static final int ROUND_FLOP = 1;
    public static final int ROUND_TURN = 2;
    public static final int ROUND_RIVER = 3;
    public static final int ROUND_SHOWDOWN = 4;

    // TESTING
    private static final String sDealPlayableHands_ = null;
    // private static String sDealPlayableHands_ = "Sklansky";

    /**
     * Get name for debugging
     */
    public static String getRoundName(int n)
    {
        switch (n)
        {
            case HoldemHand.ROUND_PRE_FLOP:
                return "preflop";
            case HoldemHand.ROUND_FLOP:
                return "flop";
            case HoldemHand.ROUND_TURN:
                return "turn";
            case HoldemHand.ROUND_RIVER:
                return "river";
            case HoldemHand.ROUND_SHOWDOWN:
                return "show";
            default:
                return "none: " + n;
        }
    }

    private PokerTable table_;
    private Deck deck_;
    private DMArrayList<Pot> pots_;
    private DMArrayList<HandAction> history_;
    private int nRound_;
    private int nGameType_ = PokerConstants.TYPE_NO_LIMIT_HOLDEM;
    private int nAnte_;
    private int nSmallBlind_;
    private int nBigBlind_;

    // set to -1 to force lazy load due to player seats not being set prior to demarshal
    private int nSmallBlindSeat_ = -1;
    private int nBigBlindSeat_ = -1;

    private final List<PokerPlayer> playerOrder_ = new ArrayList<>();
    private Hand muck_;
    private Hand community_;
    private HandSorted communitySorted_;

    // 2.0 additions
    public static final int NO_CURRENT_PLAYER = -999;
    private int nCurrentPlayerIndex_ = NO_CURRENT_PLAYER; // index into playerOrder array
    private boolean bAllInShowdown_;
    private int potStatus_;
    private long startDate_ = 0;
    private long endDate_ = 0;
    private final List<PokerPlayer> winners_ = new ArrayList<>(3);
    private final List<PokerPlayer> losers_ = new ArrayList<>(5);

    /**
     * Empty for load
     */
    public HoldemHand()
    {
    }

    ////
    //// seed for deck
    ////

    private static int lastSEED = (int) System.currentTimeMillis();
    private static int SEEDADJ = 2; // BUG 510 - must be non-one
    private static long lastADJ = System.currentTimeMillis();

    /**
     * Seed - degree of randomness from timing of hands and number of actions in hand
     * MersenneTwisterFast recommends passing in an int
     */
    private synchronized static int NEXT_SEED()
    {
        long mult = (long) lastSEED * (long) SEEDADJ;
        int seed = (int) (mult % Integer.MAX_VALUE);
        if (seed == 0 || seed == lastSEED) seed = lastSEED / SEEDADJ;
        if (seed == 0 || seed == lastSEED) seed = SEEDADJ;
        if (seed == lastSEED) seed = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        lastSEED = seed;
        return lastSEED;
    }

    /**
     * adj seed
     */
    private synchronized static void ADJUST_SEED()
    {
        long now = System.currentTimeMillis();
        long adj = SEEDADJ + (now - lastADJ);
        if (adj >= Integer.MAX_VALUE) adj = 2;  // BUG 516 - extremely unlikely to occur, but this is correct
        lastADJ = now;
        SEEDADJ = (int) (adj % Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of HoldemHand
     */
    @SuppressWarnings("CommentedOutCode")
    public HoldemHand(PokerTable table)
    {
        table_ = table;

        long seed = NEXT_SEED();
        //logger.debug("SEED: "+ seed + " SEEDADJ: "+ SEEDADJ);
        GameEngine engine = GameEngine.getGameEngine();
        if (engine != null && engine.isDemo())
        {
            // game could be null from calctool
            PokerGame game = table.getGame();
            if (game != null && !game.isClockMode())
            {
                PokerPlayer player = game.getHumanPlayer();
                int nNum = (player.isObserver()) ? table.getHandNum() : player.getHandsPlayed();
                seed = 9183349 + (nNum * 129L);
            }
        }
        deck_ = new Deck(true, seed);
        //deck_ = Deck.getDeckBUG280(); // BUG 280 debugging
        //deck_ = Deck.getDeckBUG284(); // BUG 284 debugging
        //deck_ = Deck.getDeckBUG316(); // BUG 316 debugging
        pots_ = new DMArrayList<>();
        pots_.add(new Pot(ROUND_PRE_FLOP, 0));
        history_ = new DMArrayList<>();
        nRound_ = ROUND_PRE_FLOP;
        potStatus_ = PokerConstants.NO_POT_ACTION;
        muck_ = new Hand(3);
        community_ = new Hand(5);
        communitySorted_ = new HandSorted(5);

        // if we have a game, get blind/ante
        TournamentProfile tp = table.getProfile();
        if (tp != null)
        {
            int nLevel = table.getLevel();
            nAnte_ = tp.getAnte(nLevel);
            nSmallBlind_ = tp.getSmallBlind(nLevel);
            nBigBlind_ = tp.getBigBlind(nLevel);
            nGameType_ = tp.getGameType(nLevel);
        }
    }

    /**
     * Get table this hand is on
     */
    public PokerTable getTable()
    {
        return table_;
    }

    /**
     * Get pot status code
     */
    public int getPotStatus()
    {
        return potStatus_;
    }

    /**
     * Set big blind (used in stats)
     */
    public void setBigBlind(int n)
    {
        nBigBlind_ = n;
    }

    /**
     * Set small blind
     */
    public void setSmallBlind(int n)
    {
        nSmallBlind_ = n;
    }

    /**
     * Set ante
     */
    public void setAnte(int n)
    {
        nAnte_ = n;
    }

    /**
     * Get game type
     */
    public int getGameType()
    {
        return nGameType_;
    }

    /**
     * is no limit?
     */
    public boolean isNoLimit()
    {
        return nGameType_ == PokerConstants.TYPE_NO_LIMIT_HOLDEM;
    }

    /**
     * is pot limit?
     */
    public boolean isPotLimit()
    {
        return nGameType_ == PokerConstants.TYPE_POT_LIMIT_HOLDEM;
    }

    /**
     * is no limit?
     */
    public boolean isLimit()
    {
        return nGameType_ == PokerConstants.TYPE_LIMIT_HOLDEM;
    }

    /**
     * set blinds and deal
     */
    public void deal()
    {
        setPlayerOrder(false);

        startDate_ = System.currentTimeMillis();

        dealCards(2); // do before antes/blinds so init is correct in PokerPlayer.newHand()
        doAntes();
        doBlinds();

        // fire event for deal
        table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_DEALER_ACTION, table_, nRound_));

        // init current player
        // NOTE: moved this to TournamentDirector.doBetting()
        // for display purposes, so the current player isn't
        // displayed until after the deal.  Leaving this here
        // in case that has un-intended side effects - JDD
        //initPlayerIndex();
    }

    /**
     * Ante - SYNC with simulateHand()
     */
    private void doAntes()
    {
        if (nAnte_ <= 0) return;

        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            getPlayerAt(i).ante(nAnte_);
        }
    }

    /**
     * bet blinds - SYNC with simulateHand()
     */
    private void doBlinds()
    {
        int small = table_.getNextSeatAfterButton();
        int big = table_.getNextSeat(small);

        // head's up - small blind is on the button
        if (getNumPlayers() == 2)
        {
            int nSwitch = small;
            small = big;
            big = nSwitch;
        }
        if (nSmallBlind_ > 0) table_.getPlayerRequired(small).smallblind(nSmallBlind_);
        if (nBigBlind_ > 0) table_.getPlayerRequired(big).bigblind(nBigBlind_);

        nSmallBlindSeat_ = small;
        nBigBlindSeat_ = big;
    }

    private static final List<PokerPlayer> sORDER = new ArrayList<>(PokerConstants.SEATS);

    /**
     * This method is used to randomly distribute money from between players
     * as if a hand had taken place.  It is used for all-computer tables where
     * actually playing a hand takes too much time.
     * NOTE: This method is not thread safe.  It is not synchronized to avoid
     * the performance hit
     */
    public static void simulateHand(PokerGame game, PokerTable table)
    {
//        if (true) // for testing table moves
//        {
//            if (table.getNumber() != 1 &&
//                table.getNumber() != 10
//            ) return;
//            PokerPlayer x;
//            int nCnt = 0;
//            for (int i = 0; nCnt < 1 && i < 10; i++)
//            {
//                x = table.getPlayer(i);
//                if (x == null) continue;
//                int nChips = x.getChipCount();
//                if (true || nChips <= 300000000)
//                {
//                    logger.debug("TEST BUSTING " + x.getName() + " at " + table.getName());
//                    x.setChipCount(0);
//                    game.addExtraChips(-nChips);
//                    nCnt++;
//                }
//            }
//            return;
//        }

        // random number from 1-1000
        int nNum = game.getNumPlayers() - game.getNumPlayersOut();
        if (nNum < 500) nNum = 500;

        // get order of players based on button
        HoldemHand.setPlayerOrder(table, sORDER, HoldemHand.ROUND_PRE_FLOP, false);

        // bet size
        TournamentProfile tp = table.getProfile();
        int nLevel = table.getLevel();
        int nAnte = tp.getAnte(nLevel);
        int nSmallBlind = tp.getSmallBlind(nLevel);
        int nBigBlind = tp.getBigBlind(nLevel);
        int nTotalPot = 0;

        // init bet and ante
        int nNumPlayers = sORDER.size();
        PokerPlayer p;
        for (PokerPlayer pokerPlayer : sORDER) {
            p = pokerPlayer;
            p.newSimulatedHand();
            if (nAnte > 0) nTotalPot += p.addSimulatedBet(nAnte);
        }

        // blinds
        int small = table.getNextSeatAfterButton();
        int big = table.getNextSeat(small);

        // head's up - small blind is on the button
        if (nNumPlayers == 2)
        {
            int nSwitch = small;
            small = big;
            big = nSwitch;
        }
        if (nSmallBlind > 0) nTotalPot += table.getPlayerRequired(small).addSimulatedBet(nSmallBlind);
        if (nBigBlind > 0) nTotalPot += table.getPlayerRequired(big).addSimulatedBet(nBigBlind);

        // do betting until done
        int nBet = nBigBlind + nAnte;
        int nBetBefore;
        int nToCall;
        int nRandom;
        int nRaise;
        boolean bDone = false;
        while (!bDone)
        {
            nBetBefore = nBet;

            for (int i = 0; i < nNumPlayers; i++)
            {
                p = sORDER.get(i);
                if (p.isAllIn() || p.isFolded()) continue;

                // amount to call
                nToCall = nBet - p.getSimulatedBet();

                // scale to 10000
                nRandom = (int) ((10000d * DiceRoller.rollDieInt(nNum)) / nNum);

                // something to call
                if (nToCall > 0)
                {
                    if (nRandom <= 7000)
                    {
                        p.setFolded(true);
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " folded");
                    }
                    else if (nRandom <= 9500)
                    {
                        nTotalPot += p.addSimulatedBet(nToCall);
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " called " + nToCall);
                    }
                    else if (nRandom <= 9997)
                    {
                        nTotalPot += p.addSimulatedBet(nToCall);
                        nRaise = V1Player.getRaise(nBigBlind);
                        nRaise = p.addSimulatedBet(nRaise);
                        nBet += nRaise;
                        nTotalPot += nRaise;
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " raised " + nRaise);
                    }
                    else
                    {
                        nTotalPot += p.addSimulatedBet(nToCall);
                        nRaise = p.addSimulatedBet(Integer.MAX_VALUE);
                        nBet += nRaise;
                        nTotalPot += nRaise;
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " allin " + nRaise);
                    }
                }
                // nothing to call
                else
                {
                    //noinspection StatementWithEmptyBody
                    if (nRandom <= 9500)
                    {
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " checked");
                    }
                    else if (nRandom <= 9996)
                    {
                        nRaise = V1Player.getRaise(nBigBlind);
                        nRaise = p.addSimulatedBet(nRaise);
                        nBet += nRaise;
                        nTotalPot += nRaise;
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " raised " + nRaise);
                    }
                    else
                    {
                        nRaise = p.addSimulatedBet(Integer.MAX_VALUE);
                        nBet += nRaise;
                        nTotalPot += nRaise;
                        //logger.debug(table.getName() + " player " + p.getName() + " random: " + nRandom + " allin " + nRaise);
                    }
                }
            }

            // debug
            if (DebugConfig.isTestingOn())
            {
                int nCheck = 0;
                for (int i = 0; i < nNumPlayers; i++)
                {
                    p = sORDER.get(i);
                    nCheck += p.getSimulatedBet();
                }
                ApplicationError.assertTrue(nCheck == nTotalPot, "Pot mismatch, actual=" + nCheck + ", running total=" + nTotalPot);
            }

            // done if no raises
            if (nBetBefore == nBet) bDone = true;
        }

        // randomly order to determine winner(s)
        p = null;
        while (!sORDER.isEmpty())
        {
            p = sORDER.remove(DiceRoller.rollDieInt(sORDER.size()) - 1);
            if (p.isFolded()) continue;
            break;
        }

        // give all the money to the winner.  No, this isn't fair to a player
        // that went all-in with $1 and won $5000, but this isn't meant to be fair, just a way
        // to gradually reduce the computer players.  if someone is kept alive,
        // well, good for them.
        if (p != null) {
            p.addChips(nTotalPot);
        }

        //logger.debug(table.getName() + " ************************************** player " + p.getName() + " won " + nTotalPot);
    }

    /**
     * Deal cards
     */
    @SuppressWarnings("SameParameterValue")
    private void dealCards(int nNumCards)
    {
        PokerPlayer player;
        Hand hand;

        // get num seats at table with players
        int nNum = table_.getNumOccupiedSeats();

        //noinspection ConstantValue
        if (sDealPlayableHands_ != null)
        {
            HandSelectionScheme scheme = HandSelectionScheme.getByName(sDealPlayableHands_);

            int nSeat = table_.getNextSeatAfterButton();

            for (int i = 0; i < nNum; i++)
            {
                player = table_.getPlayerRequired(nSeat);

                hand = player.newHand(Hand.TYPE_NORMAL);

                do
                {
                    deck_.addRandom(hand);
                    hand.clear();

                    hand.addCard(deck_.nextCard());
                    hand.addCard(deck_.nextCard());
                }
                while (scheme.getHandStrength(hand) == 0);

                nSeat = table_.getNextSeat(nSeat);
            }

            return;
        }

        // deal 2 cards
        for (int c = 0; c < nNumCards; c++)
        {
            int nSeat = table_.getNextSeatAfterButton();

            for (int i = 0; i < nNum; i++)
            {
                // get player and hand
                player = table_.getPlayerRequired(nSeat);

                // 1st card, new hand
                if (c == 0)
                {
                    hand = player.newHand(Hand.TYPE_NORMAL);
                }
                else
                {
                    hand = player.getHand();
                }

                // get a card and add it
                hand.addCard(deck_.nextCard());

                // increment seat
                nSeat = table_.getNextSeat(nSeat);
            }
        }
    }

    /**
     * Set player betting order
     */
    public void setPlayerOrder(boolean bCardsRequired)
    {
        setPlayerOrder(table_, playerOrder_, nRound_, bCardsRequired);
    }

    /**
     * set player order
     */
    @SuppressWarnings("SameParameterValue")
    private void setPlayerOrder(boolean bCardsRequired, int nRound)
    {
        setPlayerOrder(table_, playerOrder_, nRound, bCardsRequired);
    }

    /**
     * Set the order of players in this hand based on the round
     */
    public static void setPlayerOrder(PokerTable table, List<PokerPlayer> playerOrder, int nRound, boolean bRequireCards)
    {
        PokerPlayer player;
        int nSeat;

        // pre-flop, betting starts after blinds
        if (nRound == ROUND_PRE_FLOP)
        {
            // except when two-handed, betting starts on button
            if (table.getNumOccupiedSeats() == 2)
            {
                nSeat = table.getButton();
            }
            else
            {
                // get seat after two blinds
                nSeat = table.getNextSeat(table.getNextSeat(table.getNextSeatAfterButton()));
            }
        }
        // showdown - no betting, but order by who shows cards first
        else if (nRound == ROUND_SHOWDOWN)
        {
            nSeat = table.getHoldemHand().getFirstToShow().getSeat();
        }
        // regular betting
        else
        {
            nSeat = table.getNextSeatAfterButton();
        }

        // clear list (after check since getFirstToShow uses list)
        playerOrder.clear();

        int nOccupied = table.getNumOccupiedSeats();
        for (int i = 0; i < nOccupied; i++)
        {
            player = table.getPlayer(nSeat);
            // check null hand in case player added mid-hand from another table
            if (player != null && (player.getHand() != null || !bRequireCards))
            {
                playerOrder.add(player);
                player.setPosition(playerOrder.size(), nRound);
            }
            nSeat = table.getNextSeat(nSeat);
        }
    }

    /**
     * Get player order size (number players which started hand)
     */
    public int getNumPlayers()
    {
        return playerOrder_.size();
    }

    /**
     * Get player at in active player list.  If NO_CURRENT_PLAYER is
     * passed as the index, null is returned.  Any other non-valid
     * indexes will throw an index out of bounds exception.
     */
    public PokerPlayer getPlayerAt(int index)
    {
        if (index == NO_CURRENT_PLAYER) return null;
        return playerOrder_.get(index);
    }

    /**
     * Get players left in hand (not folded) and add them to the
     * list.  Skip given player.
     */
    public void getPlayersLeft(List<PokerPlayer> left, PokerPlayer player)
    {
        PokerPlayer p;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPlayerAt(i);
            if (p == player) continue;
            if (!p.isFolded()) left.add(p);
        }
    }

    /**
     * Get num players that act before given player (players with cards)
     */
    public int getNumBefore(PokerPlayer player)
    {
        int nCnt = 0;
        PokerPlayer p;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPlayerAt(i);
            if (p == player) return nCnt;
            if (!p.isFolded()) nCnt++;
        }

        ApplicationError.fail("Shouldn't get here for player", player);
        return 0;
    }

    /**
     * Get num players that act after given player (players with cards)
     */
    public int getNumAfter(PokerPlayer player)
    {
        int nCnt = 0;
        boolean bStartCounting = false;
        PokerPlayer p;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = getPlayerAt(i);
            if (p == player)
            {
                bStartCounting = true;
                continue;
            }
            if (bStartCounting && !p.isFolded()) nCnt++;
        }

        return nCnt;
    }

    /**
     * Get copy of history (copy so no need to synchronize when iterating)
     */
    public List<HandAction> getHistoryCopy()
    {
        synchronized (history_)
        {
            return new ArrayList<>(history_);
        }
    }

    /**
     * Get history size
     */
    public int getHistorySize()
    {
        synchronized (history_)
        {
            return history_.size();
        }
    }

    /**
     * Get deck
     */
    public Deck getDeck()
    {
        return deck_;
    }

    /**
     * Get muck
     */
    public Hand getMuck()
    {
        return muck_;
    }

    /**
     * Get community cards
     */
    public Hand getCommunity()
    {
        return community_;
    }

    /**
     * Get sorted community cards
     */
    public HandSorted getCommunitySorted()
    {
        if (communitySorted_.fingerprint() != community_.fingerprint())
        {
            communitySorted_ = new HandSorted(community_);
        }
        return communitySorted_;
    }

    /**
     * Get community to display to user - takes into account
     * whether we are in all-in showdown, and if so, returns
     * community as user is seeing it.
     */
    public Hand getCommunityForDisplay()
    {
        Hand community = getCommunity();
        if (isAllInShowdown())
        {
            community = new Hand(community);
            switch (nRound_)
            {
                case HoldemHand.ROUND_FLOP:
                    community.clear();
                    break;

                case HoldemHand.ROUND_TURN:
                case HoldemHand.ROUND_RIVER:
                    community.remove(community.size() - 1);
                    break;
            }
        }
        return community;
    }

    /**
     * get round that corresponds with getCommunityForDisplay
     */
    public int getRoundForDisplay()
    {
        int nRound = nRound_;
        if (isAllInShowdown())
        {
            switch (nRound)
            {
                case HoldemHand.ROUND_FLOP:
                case HoldemHand.ROUND_TURN:
                case HoldemHand.ROUND_RIVER:
                    nRound--;
                    break;
            }
        }
        return nRound;
    }

    /**
     * return current pot (that chips go into - needed
     * because of side pots
     */
    public Pot getCurrentPot()
    {
        synchronized (pots_)
        {
            return pots_.get(pots_.size() - 1);
        }
    }

    /**
     * Get pot odds for given player (0 to 100)
     */
    public float getPotOdds(PokerPlayer player)
    {
        int nCall = getCall(player);
        int nPotChips = getTotalPotChipCount();

        // if call will put us all in, we need to adjust chip count
        // because we might be in on all the pot
        if (nCall == player.getChipCount())
        {
            // our share of the pot from each player is the total money we put in
            int nChips = getTotalBet(player);
            int nPlayerChips = nChips + nCall;
            nPotChips = nChips;
            PokerPlayer p;

            // for each player, get their total money in the pot.  if they have
            // put more in than we can call, adjust to our max
            int nNum = getNumPlayers();
            for (int i = 0; i < nNum; i++)
            {
                p = getPlayerAt(i);
                if (p == player) continue;
                nChips = getTotalBet(p);
                if (nChips > nPlayerChips) nChips = nPlayerChips;
                nPotChips += nChips;
            }
        }
        return 100.0f * ((float) nCall / ((float) nCall + (float) nPotChips));
    }

    /**
     * Get number of chips in all pots
     */
    public int getTotalPotChipCount()
    {
        int nNum = 0;
        Pot pot;

        synchronized (pots_)
        {
            for (int i = pots_.size() - 1; i >= 0; i--)
            {
                pot = pots_.get(i);
                nNum += pot.getChipCount();
            }
        }
        return nNum;
    }

    /**
     * Get number of chips in all pots at the end of a given betting round
     */
    public int getTotalPotChipCount(int nRound)
    {
        HandAction hist;
        int total = 0;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > nRound) break;

                // TODO: adjust for overbets?

                switch (hist.getAction()) {
                    case HandAction.ACTION_ANTE:
                    case HandAction.ACTION_BLIND_SM:
                    case HandAction.ACTION_BLIND_BIG:
                    case HandAction.ACTION_BET:
                    case HandAction.ACTION_CALL:
                    case HandAction.ACTION_RAISE:
                        total += hist.getAmount();
                        break;
                }
            }
        }

        return total;
    }

    /**
     * get num pots
     */
    public int getNumPots()
    {
        synchronized (pots_)
        {
            return pots_.size();
        }
    }

    /**
     * Get num pots not counting overbets
     */
    public int getNumPotsExcludingOverbets()
    {
        int nNumPots = 0;
        Pot pot;

        synchronized (pots_)
        {
            for (int i = pots_.size() - 1; i >= 0; i--)
            {
                pot = pots_.get(i);
                if (!pot.isOverbet()) nNumPots++;
            }
        }

        return nNumPots;
    }

    /**
     * Get pot at given index
     */
    public Pot getPot(int i)
    {
        synchronized (pots_)
        {
            return pots_.get(i);
        }
    }

    /**
     * Get big blind
     */
    public int getBigBlind()
    {
        return nBigBlind_;
    }

    /**
     * Get small blind
     */
    public int getSmallBlind()
    {
        return nSmallBlind_;
    }

    /**
     * Get big blind seat
     */
    public int getBigBlindSeat()
    {
        if (nBigBlindSeat_ < 0)
        {
            // reconstruct some stuff from history
            synchronized (history_)
            {
                for (HandAction action : history_)
                {
                    if (action.getAction() == HandAction.ACTION_BLIND_BIG)
                    {
                        nBigBlindSeat_ = action.getPlayer().getSeat();
                        break;
                    }
                }
            }
        }
        return nBigBlindSeat_;
    }

    /**
     * Get small blind seat
     */
    public int getSmallBlindSeat()
    {
        if (nSmallBlindSeat_ < 0)
        {
            // reconstruct some stuff from history
            synchronized (history_)
            {
                for (HandAction action : history_)
                {
                    if (action.getAction() == HandAction.ACTION_BLIND_SM)
                    {
                        nSmallBlindSeat_ = action.getPlayer().getSeat();
                        break;
                    }
                }
            }
        }
        return nSmallBlindSeat_;
    }

    /**
     * Get ante
     */
    public int getAnte()
    {
        return nAnte_;
    }

    /**
     * Return round
     */
    public int getRound()
    {
        return nRound_;
    }

    /**
     * Advance round - deal community cards
     */
    @SuppressWarnings("UnusedReturnValue")
    public int advanceRound()
    {
        nRound_++;
        //logger.debug("ROUND now " + getRoundName(nRound_));
        getCurrentPot().advanceRound();
        potStatus_ = PokerConstants.NO_POT_ACTION;
        Card c;

        // deal out community cards
        switch (nRound_)
        {
            case ROUND_FLOP:
                // burn & flop
                muck_.addCard(deck_.nextCard());
                c = deck_.nextCard();
                community_.addCard(c);
                communitySorted_.addCard(c);
                c = deck_.nextCard();
                community_.addCard(c);
                communitySorted_.addCard(c);
                c = deck_.nextCard();
                community_.addCard(c);
                communitySorted_.addCard(c);
                setPlayerOrder(true); // changes after flop
                break;

            case ROUND_TURN:
                // burn & turn
                muck_.addCard(deck_.nextCard());
                c = deck_.nextCard();
                community_.addCard(c);
                communitySorted_.addCard(c);
                break;

            case ROUND_RIVER:
                // burn & river
                muck_.addCard(deck_.nextCard());
                c = deck_.nextCard();
                community_.addCard(c);
                communitySorted_.addCard(c);
                break;

            case ROUND_SHOWDOWN:
                // set player order based on who is first to show
                // moved to preResolve()
                //setPlayerOrder(true);
                break;
        }

        // start action with 1st player in list
        if (nRound_ != ROUND_SHOWDOWN)
        {
            // init current player
            // NOTE: moved this to TournamentDirector.doBetting()
            // for display purposes, so the current player isn't
            // displayed until after the cards shown.  Leaving this here
            // in case that has un-intended side effects - JDD
            //initPlayerIndex();

            // if we are done betting after advancing the
            // round and there are still multiple players
            // left, we are in an all-in showdown situation.
            if (!bAllInShowdown_ && isDone() && getNumWithCards() > 1)
            {
                bAllInShowdown_ = true;
            }
        }
        else
        {
            setCurrentPlayerIndex(NO_CURRENT_PLAYER);
        }

        // notify of new round
        table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_DEALER_ACTION, table_, nRound_));

        return nRound_;
    }

    /**
     * If there is no more betting to be done because all but one
     * of the players is all-in, then we are in an all-in showdown
     * situation.
     */
    public boolean isAllInShowdown()
    {
        return bAllInShowdown_;
    }

    /**
     * set player index to first player to act on a round
     */
    private void initPlayerIndex()
    {
        // start at -1, then call player acted to
        // find "next" player to act, where search
        // will start at player 0
        playerActed(-1);
    }

    /**
     * note player acted, advance current player index
     */
    private void playerActed()
    {
        playerActed(nCurrentPlayerIndex_);
    }

    /**
     * do real work, start at given index
     */
    private void playerActed(int nStart)
    {
        PokerPlayer player;

        if (isDone())
        {
            setCurrentPlayerIndex(NO_CURRENT_PLAYER);
            return;
        }

        boolean bDone = false;

        while (!bDone)
        {
            nStart++;
            if (nStart >= getNumPlayers()) nStart = 0;

            // if next player is all in or folded
            // then go to next one
            player = getPlayerAt(nStart);
            if (player.isFolded() || player.isAllIn()) continue;

            bDone = true;
        }

        setCurrentPlayerIndex(nStart);
    }

    /**
     * Set current player index.  In a normal holdem game, this is set automatically
     * when players act.  Could be set from outside for simulations, and backwards compat for 1.0
     */
    public void setCurrentPlayerIndex(int n)
    {
        // if hand is done for the round, then override current player
        if (n != NO_CURRENT_PLAYER && isDone())
        {
            n = NO_CURRENT_PLAYER;
        }

        int nOld = nCurrentPlayerIndex_;
        nCurrentPlayerIndex_ = n;

        PokerPlayer oldPlayer = nOld == NO_CURRENT_PLAYER ? null : getPlayerAt(nOld);
        PokerPlayer newPlayer = n == NO_CURRENT_PLAYER ? null : getPlayerAt(n);

        if (oldPlayer != null) oldPlayer.setCurrentGamePlayer(false);
        if (newPlayer != null) newPlayer.setCurrentGamePlayer(true);

        if (oldPlayer != newPlayer)
        {
            table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED, table_, nOld, nCurrentPlayerIndex_));
        }
    }

    /**
     * return index of current player for use with getPlayerAt()
     */
    @SuppressWarnings("unused")
    public int getCurrentPlayerIndex()
    {
        return nCurrentPlayerIndex_;
    }

    /**
     * Returns current player (who we're waiting on) as defined by
     * getCurrentPlayerIndex(), or null if no current player
     */
    public PokerPlayer getCurrentPlayer()
    {
        return getPlayerAt(nCurrentPlayerIndex_);
    }

    /**
     * Returns current player.  If that isn't set,
     * initializes player order (special case use -
     * if you aren't sure you should be using this,
     * you probably shouldn't).  This method is
     * synchronized because on clients in online games,
     * could be called from different threads (e.g.,
     * DealDisplay and TournamentDirector.storeHandAction())
     */
    // TODO: remove sync if decide to wait for observers in TD wait list
    public synchronized PokerPlayer getCurrentPlayerInitIndex()
    {
        PokerPlayer current = getCurrentPlayer();

        // if current player is not defined, then this is the
        // first betting round after the deal.  We init the
        // current player.  This was moved here from
        // HoldemHand.deal() so that the current player
        // isn't highlighted during the dealing of cards
        if (current == null)
        {
            initPlayerIndex();
            current = getCurrentPlayer();
        }

        return current;
    }

    /**
     * Was there any actions in given round?
     */
    public boolean isActionInRound(int nRound)
    {
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if (hist.getRound() == nRound)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get last action by player
     */
    public int getLastAction(PokerPlayer player)
    {
        HandAction hist = getLastHandAction(player);

        if (hist != null)
        {
            return hist.getAction();
        }
        else
        {
            return HandAction.ACTION_NONE;
        }
    }

    /**
     * Get last action by player in a given round
     */
    public HandAction getLastAction(PokerPlayer player, int round)
    {
        HandAction hist;

        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);

                if ((hist.getPlayer() == player) && (hist.getRound() == round))
                {
                    return hist;
                }
            }
        }

        return null;
    }

    /**
     * Get last action by player
     */
    public HandAction getLastHandAction(PokerPlayer player)
    {
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if (hist.getPlayer() == player)
                {
                    return hist;
                }
            }
        }

        return null;
    }

    /**
     * Of the actions by the player on the last round, return
     * the one most meaningful for ai: raise - bet - call - check
     */
    public int getLastActionAI(PokerPlayer player, int nRound)
    {
        if (player == null) return HandAction.ACTION_NONE;

        HandAction hist;
        int nAction = HandAction.ACTION_NONE;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if (hist.getPlayer() == player &&
                    hist.getRound() == nRound)
                {
                    if (hist.getAction() > nAction)
                    {
                        nAction = hist.getAction();
                        if (nAction == HandAction.ACTION_CHECK_RAISE)
                        {
                            nAction = HandAction.ACTION_CHECK;
                        }
                    }
                }
            }
        }

        return nAction;
    }

    /**
     * Get last action by player
     */
    public int getLastActionThisRound(PokerPlayer player)
    {
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if (hist.getPlayer() == player &&
                    hist.getRound() == nRound_)
                {
                    return hist.getAction();
                }
            }
        }

        return HandAction.ACTION_NONE;
    }

    /**
     * Get last betting action by any player in any round (any action
     * other than OVERBET or WIN)
     */
    public HandAction getLastAction()
    {
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if (hist.getAction() != HandAction.ACTION_OVERBET &&
                    hist.getAction() != HandAction.ACTION_WIN &&
                    hist.getAction() != HandAction.ACTION_LOSE)
                {
                    return hist;
                }
            }
        }

        return null;
    }

    /**
     * Get num raises prior to this player
     */
    public int getNumPriorRaises(PokerPlayer player)
    {
        int nCount = 0;
        boolean bStart = false;
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);

                // only count this round's actions
                if (hist.getRound() != nRound_) continue;

                if (bStart && hist.getAction() == HandAction.ACTION_RAISE)
                {
                    nCount++;
                }

                if (!bStart && hist.getPlayer() == player)
                {
                    // else start counting
                    bStart = true;
                }
            }
        }

        return nCount;
    }

    /**
     * Get num raises this round
     */
    public int getNumRaises()
    {
        int nCount = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);

                // only count this round's actions
                if (hist.getRound() != nRound_) continue;


                if (hist.getAction() == HandAction.ACTION_RAISE)
                {
                    nCount++;
                }

            }
        }

        return nCount;
    }

    /**
     * get round folded
     */
    public int getFoldRound(PokerPlayer player)
    {
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_FOLD && hist.getPlayer() == player) {
                    return hist.getRound();
                }
            }
        }

        return ROUND_NONE;
    }


    /**
     * is given player folded?
     */
    public boolean isFolded(PokerPlayer player)
    {
        return player.isFolded();
    }

    /**
     * Player antes
     */
    public void ante(PokerPlayer player, int nChips)
    {
        addToPot(player, nChips, HandAction.ACTION_ANTE, null);
    }

    /**
     * Player posts small blind
     */
    public void smallblind(PokerPlayer player, int nChips)
    {
        addToPot(player, nChips, HandAction.ACTION_BLIND_SM, null);
    }

    /**
     * Player posts big blind
     */
    public void bigblind(PokerPlayer player, int nChips)
    {
        addToPot(player, nChips, HandAction.ACTION_BLIND_BIG, null);
    }

    /**
     * Player folds
     */
    public void fold(PokerPlayer player, String sDebug, int nFoldType)
    {
        addHistory(new HandAction(player, nRound_, HandAction.ACTION_FOLD, 0, nFoldType, sDebug));
    }

    /**
     * Player checks
     */
    public void check(PokerPlayer player, String sDebug)
    {
        addHistory(new HandAction(player, nRound_, HandAction.ACTION_CHECK, 0, sDebug));
    }

    /**
     * Player checks with intention of raising
     */
    public void checkraise(PokerPlayer player, String sDebug)
    {
        addHistory(new HandAction(player, nRound_, HandAction.ACTION_CHECK_RAISE, 0, sDebug));
    }

    /**
     * Player calls
     */
    public void call(PokerPlayer player, int nChips, String sDebug)
    {
        addToPot(player, nChips, HandAction.ACTION_CALL, sDebug);
    }

    /**
     * Player bets
     */
    public void bet(PokerPlayer player, int nChips, String sDebug)
    {
        addToPot(player, nChips, HandAction.ACTION_BET, sDebug);
    }

    /**
     * Player calls and raises
     */
    public void raise(PokerPlayer player, int nCall, int nRaise, String sDebug)
    {
        addToPot(player, nCall + nRaise, nCall, HandAction.ACTION_RAISE, sDebug);
    }

    /**
     * Player wins pot
     */
    public void wins(PokerPlayer player, int nChips, int nPot)
    {
        addHistory(new HandAction(player, nRound_, HandAction.ACTION_WIN, nChips, nPot, null));
    }

    /**
     * Player gets back overbet
     */
    public void overbet(PokerPlayer player, int nChips, int nPot)
    {
        addHistory(new HandAction(player, nRound_, HandAction.ACTION_OVERBET, nChips, nPot, null));
    }

    /**
     * Player loses pot
     */
    public void lose(PokerPlayer player, int nPot)
    {
        addHistory(new HandAction(player, nRound_, HandAction.ACTION_LOSE, 0, nPot, null));
    }

    /**
     * any time money is placed in the pot
     */
    private void addToPot(PokerPlayer player, int nChips, int nAction, String sDebug)
    {
        addToPot(player, nChips, 0, nAction, sDebug);
    }

    /**
     * add money to pot, specify nCall for RAISE action (indicates portion of nChips that
     * was the call of the previous bet)
     */
    private void addToPot(PokerPlayer player, int nChips, int nCall, int nAction, String sDebug)
    {
        if (nChips < 0)
        {
            debugPrint();
            ApplicationError.fail("Adding negative chips " + nChips + " debug: " +
                                               sDebug + " action: " + nAction + " pots: " + pots_);
        }

        int nMinChip = getMinChip();
        if (nChips % nMinChip != 0)
        {
            debugPrint();
            ApplicationError.fail("Adding uneven chips " + nChips + " debug: " +
                                               sDebug + " action: " + nAction + " pots: " + pots_);
        }
        addHistory(new HandAction(player, nRound_, nAction, nChips, nCall, sDebug));
    }

    /**
     * get min chip
     */
    public int getMinChip()
    {
        if (table_ == null) return 1;
        else return table_.getMinChip();
    }

    /**
     * Figure out how many pots we have and allocate to side pots
     * as necessary
     */
    private void calcPots()
    {
        // create PotInfo for each player
        List<PotInfo> info = new ArrayList<>();
        PokerPlayer player;
        int nPlayerBet;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);
            nPlayerBet = getBet(player) + getAnte(player);
            info.add(new PotInfo(player, nPlayerBet));
        }

        // sort it using comparable of PotInfo,
        // placing those short of bet at beginning
        Collections.sort(info);
        if (TESTING(PokerConstants.TESTING_DEBUG_POT)) logger.debug("PotInfo: {}", info);

        // add new pots as needed
        int nLastSideBet = 0;
        synchronized (pots_)
        {
            Pot pot = resetMainPotForRound();
            for (int i = 0; i < info.size(); i++)
            {
                PotInfo potinfo = info.get(i);
                if (potinfo.needSide() && ((i == 0 && pot.hasBaseAllIn() && potinfo.nBet == 0) || potinfo.nBet > nLastSideBet))
                {
                    pot.setSideBet(potinfo.nBet - nLastSideBet);
                    pot = new Pot(nRound_, i + 1);
                    pots_.add(pot);
                    nLastSideBet = potinfo.nBet;
                }
            }

            // for each pot, go through each player and
            // allocate their bets to each pot
            int nSide;
            int nBet;
            for (Pot pot2 : pots_)
            {
                if (pot2.getRound() != nRound_) continue;
                nSide = pot2.getSideBet();

                for (PotInfo potinfo : info)
                {
                    if (potinfo.nBet == 0) continue;

                    if (nSide == Pot.NO_SIDE)
                    {
                        pot2.addChips(potinfo.player, potinfo.nBet);
                        potinfo.nBet = 0;
                    }
                    else
                    {
                        nBet = Math.min(nSide, potinfo.nBet);
                        pot2.addChips(potinfo.player, nBet);
                        potinfo.nBet -= nBet;
                    }
                }
            }
        }
    }

    /**
     * Return the main pot for the current round, eliminating
     * all side pots and resetting main one.
     */
    private Pot resetMainPotForRound()
    {
        Pot last = null;
        Pot pot;
        synchronized (pots_)
        {
            for (int i = pots_.size() - 1; i >= 0; i--)
            {
                pot = pots_.get(i);
                if (pot.getRound() != nRound_)
                {
                    break;
                }

                if (last != null) pots_.remove(last);

                last = pot;
            }
        }

        if (last != null) {
            last.reset();
        }
        return last;
    }

    /**
     * Class to track pot info
     */
    private class PotInfo implements Comparable<PotInfo>
    {
        int nBet;
        PokerPlayer player;
        int nCurrent;
        boolean bNeedSide;

        public PotInfo(PokerPlayer player, int nBet)
        {
            this.player = player;
            this.nBet = nBet;
            this.nCurrent = getBet();
            this.bNeedSide = nBet < nCurrent && player.isAllIn();
        }

        public int compareTo(PotInfo i)
        {
            if (needSide())
            {
                if (i.needSide())
                {
                    return nBet - i.nBet;
                }
                else
                {
                    return -1;
                }
            }
            else if (i.needSide())
            {
                return 1;
            }
            else return nBet - i.nBet;
        }

        @Override
        public String toString()
        {
            return player.getName() + ": $" + nBet + (needSide() ? " (side)" : "");
        }

        public boolean needSide()
        {
            return bNeedSide;
        }

    }

    /**
     * Return current bet on the table for current round
     * (i.e., the highest bet by any player)
     */
    public int getBet()
    {
        return getBet(nRound_);
    }

    /**
     * Return bet for round
     */
    public int getBet(int nRound)
    {
        int nBet = 0;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            nBet = Math.max(nBet, getBet(getPlayerAt(i), nRound));
        }

        return nBet;
    }

    /**
     * return current bet in front of player
     */
    public int getBet(PokerPlayer player)
    {
        return getBet(player, nRound_);
    }

    /**
     * Return bet by player for given round
     */
    public int getBet(PokerPlayer player, int nRound)
    {
        // add up all bets in given round
        int nBet = 0;
        HandAction hist;
        synchronized (history_)
        {
            int nAction;
            for (HandAction handAction : history_) {
                hist = handAction;
                nAction = hist.getAction();
                if (hist.getRound() == nRound &&
                        hist.getPlayer() == player &&
                        (nAction == HandAction.ACTION_BET ||
                                nAction == HandAction.ACTION_RAISE ||
                                nAction == HandAction.ACTION_CALL ||
                                nAction == HandAction.ACTION_BLIND_SM ||
                                nAction == HandAction.ACTION_BLIND_BIG)
                ) {
                    nBet += hist.getAmount();
                }
            }
        }

        return nBet;
    }

    /**
     * Return biggest bet or raise this hand - smallest value to be
     * returned is big blind
     */
    public int getBiggestBetRaise()
    {
        int nBet = getMinBet();
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_BET ||
                        hist.getAction() == HandAction.ACTION_RAISE) {
                    if (hist.getAmount() > nBet) nBet = hist.getAdjustedAmount();
                }
            }
        }

        return nBet;
    }

    /**
     * Return sum of all bets by player (including antes) in all rounds
     */
    public int getTotalBet(PokerPlayer player)
    {
        // add up all bets in given round
        int nBet = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if ((hist.getAction() == HandAction.ACTION_BET ||
                        hist.getAction() == HandAction.ACTION_RAISE ||
                        hist.getAction() == HandAction.ACTION_CALL ||
                        hist.getAction() == HandAction.ACTION_BLIND_SM ||
                        hist.getAction() == HandAction.ACTION_BLIND_BIG ||
                        hist.getAction() == HandAction.ACTION_ANTE) &&
                        hist.getPlayer() == player) {
                    nBet += hist.getAmount();
                }
            }
        }

        return nBet;
    }

    /**
     * return player who started betting in a round. If no bets in round yet,
     * null is returned.
     */
    public PokerPlayer getBettor()
    {
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_BET &&
                        hist.getRound() == nRound_) {
                    return hist.getPlayer();
                }
            }
        }

        return null;
    }

    /**
     * return player who is big blind.
     */
    public PokerPlayer getBigBlindPlayer()
    {
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_BLIND_BIG) {
                    return hist.getPlayer();
                }
            }
        }

        return null;
    }

    /**
     * return player who last raised in a round. If no raises in round yet,
     * null is returned.
     */
    public PokerPlayer getRaiser()
    {
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if ((hist.getAction() == HandAction.ACTION_RAISE) &&
                    hist.getRound() == nRound_)
                {
                    return hist.getPlayer();
                }
            }
        }

        return null;
    }

    /**
     * return player who initiated betting/raising.  This is the
     * first player to bet in a round or the last player to raise.
     * If no betting happened in a round, the previous rounds are
     * searched.
     */
    public PokerPlayer getFirstToShow()
    {
        HandAction saved = null;
        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);

                // if we have a saved hand action, and we are now looking
                // at history from a previous round, return the
                // save action
                if (saved != null && hist.getRound() < saved.getRound())
                {
                    return saved.getPlayer();
                }

                // last raiser in round is first to show
                if (hist.getAction() == HandAction.ACTION_RAISE)
                {
                    return hist.getPlayer();
                }
                // else remember bets - first bettor in round
                // is first to act (triggered above)
                else if (hist.getAction() == HandAction.ACTION_BET)
                {
                    saved = hist;
                }
            }
        }

        // bettor in first round ... there should not be any
        // bettors in first round ... that is the big blind
        // but this is just a safety check
        if (saved != null)
        {
            return saved.getPlayer();
        }

        // default is 1st player after button.  This would happen
        // if no one raises big blind and checks checks checks...
        return getPlayerAt(0);
    }

    /**
     * return number of calls in this round
     */
    public int getNumCallers()
    {
        // count calls in this round
        int nCalls = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if ((hist.getAction() == HandAction.ACTION_CALL) &&
                        hist.getRound() == nRound_) {
                    nCalls++;
                }
            }
        }

        return nCalls;
    }

    /**
     * return amount ante'd by player this round
     */
    public int getAnte(PokerPlayer player)
    {
        // add up all bets in this round
        int nBet = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_ANTE &&
                        hist.getPlayer() == player &&
                        hist.getRound() == nRound_) {
                    nBet += hist.getAmount();
                }
            }
        }

        return nBet;
    }

    /**
     * return antes/small blinds bet by this player in all rounds
     */
    public int getAnteSmallBlind(PokerPlayer player)
    {
        // add up all bets in this round
        int nBet = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if ((hist.getAction() == HandAction.ACTION_ANTE ||
                        hist.getAction() == HandAction.ACTION_BLIND_SM) &&
                        hist.getPlayer() == player) {
                    nBet += hist.getAmount();
                }
            }
        }

        return nBet;
    }

    /**
     * return antes/blinds bet by all players
     */
    public int getAntesBlinds()
    {
        int nBet = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_ANTE ||
                        hist.getAction() == HandAction.ACTION_BLIND_SM ||
                        hist.getAction() == HandAction.ACTION_BLIND_BIG) {
                    nBet += hist.getAmount();
                }
            }
        }

        return nBet;
    }


    /**
     * Return the maximum total bet the other active players
     * at the table could make
     */
    private int getMaxChipsOtherPlayers(PokerPlayer current, boolean bAddCurrentBet, boolean bSubtractCurrentCall)
    {
        PokerPlayer other;
        int nOtherLeft;
        int nMaxOther = 0;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            other = getPlayerAt(i);
            if (other.isFolded()) continue;
            if (other == current) continue;
            nOtherLeft = other.getChipCount();
            if (bAddCurrentBet) nOtherLeft += getBet(other);
            if (bSubtractCurrentCall) nOtherLeft -= getCall(other);
            nMaxOther = Math.max(nMaxOther, nOtherLeft);
        }
        return nMaxOther;
    }


    /**
     * Return amount needed to call
     */
    public int getCall(PokerPlayer player)
    {
        int nPlayerBet = getBet(player);

        // call is current bet less any bets already made
        int nCall = getBet() - nPlayerBet;

        // make sure min call in any round is big blind (in case big blind or initial
        // bet puts a player all in)
        if (nCall > 0 && nCall < getMinBet() && nPlayerBet == 0)
        {
            nCall = Math.min(getMinBet(), getMaxChipsOtherPlayers(player, true, false));
        }

        // if call is more than players chip count, reduce it to that
        if (nCall > player.getChipCount())
        {
            nCall = player.getChipCount();
        }

        if (nCall < 0) ApplicationError.fail("Negative call " + nCall, player);
        return nCall;
    }

    /**
     * get minimum bet (big blind in limit/pot limit, big blind or 2x big blind (turn/river) in limit
     */
    public int getMinBet()
    {
        int nMin = getBigBlind();
        if (isLimit() && (nRound_ == ROUND_TURN || nRound_ == ROUND_RIVER))
        {
            nMin *= 2;
        }
        return nMin;
    }

    /**
     * Get max bet a player can make in a round
     */
    public int getMaxBet(PokerPlayer player)
    {
        // init
        int nLeft = player.getChipCount();

        int nMax;
        if (isNoLimit())
        {
            nMax = nLeft;
        }
        else if (isPotLimit())
        {
            nMax = Math.max(getTotalPotChipCount(), getMinBet());
        }
        else if (isLimit())
        {
            nMax = getMinBet();
        }
        else
        {
            throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Unknown game type " + nGameType_, null);
        }

        // check against chips left (only needed for limit/pot limit)
        if (!isNoLimit()) nMax = Math.min(nMax, nLeft);

        // check other players
        nMax = Math.min(getMaxChipsOtherPlayers(player, false, false), nMax);

        return nMax;
    }

    /**
     * Return max raise.  For limit games, returns 0 if max raises have been reached
     */
    public int getMaxRaise(PokerPlayer player)
    {
        if (isLimit() && getTable().getGame().getProfile().getMaxRaises(getNumWithCards(), player.isComputer()) <= getNumRaises())
        {
            return 0;
        }

        int nMax = 0;
        int nCall = getCall(player);
        int nLeft = player.getChipCount() - nCall;
        if (nLeft > 0)
        {
            if (isNoLimit())
            {
                nMax = nLeft;
            }
            else if (isPotLimit())
            {
                nMax = Math.max(getTotalPotChipCount() + nCall, getMinBet());
            }
            else if (isLimit())
            {
                nMax = getMinBet();
            }
            else
            {
                throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Unknown game type " + nGameType_, null);
            }

            // check against chips left (only needed for limit/pot limit)
            if (!isNoLimit()) nMax = Math.min(nMax, nLeft);

            // check other players
            nMax = Math.min(getMaxChipsOtherPlayers(player, false, true), nMax);
        }

        return nMax;
    }

    /**
     * Return minimum raise, which is the big blind, if no raises yet,
     * or the largest previous bet/raise.
     */
    public int getMinRaise()
    {
        int nMin = getMinBet();
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getRound() == nRound_ &&
                        (hist.getAction() == HandAction.ACTION_BET ||
                                hist.getAction() == HandAction.ACTION_RAISE)) {
                    nMin = Math.max(hist.getAdjustedAmount(), nMin);
                }
            }
        }
        return nMin;
    }

    public boolean isStoredInDatabase()
    {
        return bStoredInDatabase_;
    }

    /**
     * return whether action for the current round is complete
     */
    public boolean isDone()
    {
        // if only one player left who hasn't folded, we are done
        if (isUncontested()) return true;

        // count players that have acted
        // all players must have acted
        PokerPlayer player;
        int nPlayersToAct = 0;
        int nPlayersActed = 0;
        int nAllIn = 0;
        int nNumPlayers = getNumPlayers();
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = getPlayerAt(i);

            // if folded or all in, the player doesn't have to act
            if (player.isFolded()) continue;

            // increment players that have to act (i.e., any player still in hand)
            nPlayersToAct++;

            // if they have acted, bump counter
            if (hasPlayerActed(player))
            {
                nPlayersActed++;
            }
            // count all-in
            else if (player.isAllIn())
            {
                // PATCH 1 - if a player went all in on the blind/ante
                // mark them a having acted so we don't trigger
                // the early exit below (x) - they aren't noted
                // as having acted above because they went all in
                // on the forced bet (hasPlayerActed() ignores antes/blinds)
                if (nRound_ == HoldemHand.ROUND_PRE_FLOP)
                {
                    nPlayersActed++;
                }
                else
                {
                    nAllIn++;
                }
            }
            // if not all in, but we have nothing to call, and we can't raise,
            // then mark as acted
            else if (getCall(player) == 0 && getMaxRaise(player) == 0)
            {
                nPlayersActed++;
            }
        }

        // (x)
        // if only one player left who can bet (everyone else all in),
        // and there are no players yet acted we are done
        if (nPlayersActed == 0 && getNumWithChips() <= 1) return true;

        // all players must have acted (or be all in)
        if (nPlayersToAct != (nPlayersActed + nAllIn)) return false;

        // not done if pot not good (players may have all acted, but still
        // have raises to call)
        return isPotGood();

    }

    /**
     * Is this hand uncontested (getNumWithCards() == 1)
     */
    public boolean isUncontested()
    {
        return getNumWithCards() == 1;
    }

    /**
     * Is pot good?  Meaning have all players
     * have matched the current bet on the table
     * or gone all-in?
     */
    public boolean isPotGood()
    {
        PokerPlayer player;
        int nBet = getBet();
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);
            if (player.isFolded()) continue;
            if (player.isAllIn()) continue;
            if (getBet(player) != nBet) return false;
        }
        return true;
    }

    /**
     * Get number of players that still have cards
     */
    public int getNumWithCards()
    {
        PokerPlayer player;
        int nNum = 0;
        int nNumP = getNumPlayers();
        for (int i = 0; i < nNumP; i++)
        {
            player = getPlayerAt(i);
            if (!player.isFolded()) nNum++;
        }
        return nNum;
    }

    /**
     * Get number of players that have chips left to bet
     * (and have cards in this hand)
     */
    public int getNumWithChips()
    {
        PokerPlayer player;
        int nNum = 0;
        int nNumP = getNumPlayers();
        for (int i = 0; i < nNumP; i++)
        {
            player = getPlayerAt(i);
            if (player.isFolded()) continue;
            if (!player.isAllIn()) nNum++;
        }
        return nNum;
    }

    /**
     * verify sum of all pots matches sum of all player bets
     */
    private void verifyPot()
    {
        int nPots = 0;
        int nBets = 0;
        synchronized (pots_)
        {
            for (Pot pot : pots_)
            {
                nPots += pot.getChipCount();
            }
        }

        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                nBets += hist.getAmount();
            }
        }

        if (nBets != nPots)
        {
            debugPrint();
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR,
                                       "Pot amount (" + nPots + ") doesn't equal num bets (" + nBets + ")", null);
        }
    }

    /**
     * Has player acted this round (any check,bet,call,raise,fold except for
     * blinds and antes)
     */
    public boolean hasPlayerActed(PokerPlayer player)
    {
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getPlayer() == player &&
                        hist.getRound() == nRound_ &&
                        hist.getAction() != HandAction.ACTION_ANTE &&
                        hist.getAction() != HandAction.ACTION_BLIND_SM &&
                        hist.getAction() != HandAction.ACTION_BLIND_BIG &&
                        hist.getAction() != HandAction.ACTION_WIN &&
                        hist.getAction() != HandAction.ACTION_OVERBET &&
                        hist.getAction() != HandAction.ACTION_LOSE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * return winnings by player
     */
    public int getWin(PokerPlayer player)
    {
        // if hand was folded return 0
        if (isFolded(player)) return 0;

        int nWin = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_WIN &&
                        hist.getPlayer() == player) {
                    nWin += hist.getAmount();
                }
            }
        }

        return nWin;
    }

    /**
     * return total overbet returned to player
     */
    public int getOverbet(PokerPlayer player)
    {
        // if hand was folded return 0 (shortcut)
        if (isFolded(player)) return 0;

        int nWin = 0;
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                if (hist.getAction() == HandAction.ACTION_OVERBET &&
                        hist.getPlayer() == player) {
                    nWin += hist.getAmount();
                }
            }
        }

        return nWin;
    }

    /**
     * return pot result for given player
     */
    public HandAction getPotResult(PokerPlayer player, int nPot)
    {
        // if hand was folded return 0 (shortcut)
        if (isFolded(player)) return null;

        HandAction hist;
        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; i--)
            {
                hist = history_.get(i);
                if ((hist.getAction() == HandAction.ACTION_OVERBET ||
                     hist.getAction() == HandAction.ACTION_WIN ||
                     hist.getAction() == HandAction.ACTION_LOSE) &&
                    hist.getPlayer() == player &&
                    hist.getSubAmount() == nPot)
                {
                    return hist;
                }
            }
        }

        return null;
    }

    /**
     * Add something to history
     */
    private void addHistory(HandAction action)
    {
        // adjust every history action - will be different each game
        ADJUST_SEED();

        PokerPlayer player = action.getPlayer();
        int nAction = action.getAction();

        // ai debugging
        if ((TESTING(EngineConstants.TESTING_AI_DEBUG) || TESTING(PokerConstants.TESTING_DEBUG_POT)) && player.getTable().isCurrent())
        {
            debugPrint(action);
        }

        // set time of action and add to history
        synchronized (history_)
        {
            history_.add(action);
        }

        // if we need to calculate the pot, do so (ante, blinds, call, bet, raise)
        switch (nAction)
        {
            case HandAction.ACTION_ANTE:
            case HandAction.ACTION_BLIND_SM:
            case HandAction.ACTION_BLIND_BIG:
            case HandAction.ACTION_CALL:
            case HandAction.ACTION_BET:
            case HandAction.ACTION_RAISE:
                calcPots();
                if (TESTING(PokerConstants.TESTING_DEBUG_POT)) logger.debug("POTS AT END: {}", pots_);
                verifyPot();
                break;
        }

        // finally fire event now that all is done
        table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ACTION, table_, action));

        // update pot status
        switch (nAction)
        {
            case HandAction.ACTION_CALL:
                if (potStatus_ == PokerConstants.NO_POT_ACTION)
                {
                    potStatus_ = PokerConstants.CALLED_POT;
                }
                break;
            case HandAction.ACTION_BET:
            case HandAction.ACTION_RAISE:
                switch (potStatus_)
                {
                    case PokerConstants.NO_POT_ACTION:
                    case PokerConstants.CALLED_POT:
                        potStatus_ = PokerConstants.RAISED_POT;
                        break;
                    case PokerConstants.RAISED_POT:
                        potStatus_ = PokerConstants.RERAISED_POT;
                        break;
                }
                break;
        }

        // if we need to note that a player acted (fold, check, call, bet, raise), do so
        switch (nAction)
        {
            case HandAction.ACTION_FOLD:
            case HandAction.ACTION_CHECK:
            case HandAction.ACTION_CHECK_RAISE:
            case HandAction.ACTION_CALL:
            case HandAction.ACTION_BET:
            case HandAction.ACTION_RAISE:
                playerActed();
                break;
        }
    }

    /**
     * debug print history
     */
    @SuppressWarnings("CommentedOutCode")
    private void debugPrint(HandAction action)
    {
        PokerPlayer player = action.getPlayer();
        //int nRank = HoldemExpert.getSklanskyRank(player.getHandSorted());
        //int nGroup = HoldemExpert.getGroupFromRank(nRank);
        logger.debug("{}{} {}", player.isHuman() ? "HU: " : "AI: ", action.toString(false), (action.getAction() == HandAction.ACTION_FOLD) ? "" : getCommunity().toString());
    }

    /**
     * debug print all history
     */
    public void debugPrint()
    {
        HandAction hist;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                debugPrint(hist);
            }
        }
    }

    /**
     * pre-resolve pot, creating HandInfo for each player and
     * figuring out winners, losers who don't have to show.
     */
    public void preResolve(boolean bDoWinnersLosers)
    {
        setPlayerOrder(true, ROUND_SHOWDOWN);

        // pre-resolve each pot
        if (bDoWinnersLosers)
        {
            winners_.clear();
            losers_.clear();
            List<PokerPlayer> localWinners = new ArrayList<>();
            List<PokerPlayer> localLosers = new ArrayList<>();

            int nNumPots = getNumPots();
            for (int i = 0; i < nNumPots; i++)
            {
                localWinners.clear();
                localLosers.clear();
                preResolvePot(i, localWinners, localLosers);
                for (PokerPlayer localWinner : localWinners)
                {
                    if (!winners_.contains(localWinner))
                    {
                        winners_.add(localWinner);
                    }
                }
                for (PokerPlayer localLoser : localLosers)
                {
                    if (!losers_.contains(localLoser))
                    {
                        losers_.add(localLoser);
                    }
                }
            }
        }
    }

    /**
     * get list of winners calculated by preResolve()
     */
    public List<PokerPlayer> getPreWinners()
    {
        return winners_;
    }

    /**
     * get list of losers calculated by preResolve()
     */
    public List<PokerPlayer> getPreLosers()
    {
        return losers_;
    }

    /**
     * figure out who won, allocate pot to winners
     */
    public void resolve()
    {
        endDate_ = System.currentTimeMillis();

        // resolve each pot
        int nNumPots = getNumPots();
        for (int i = 0; i < nNumPots; i++)
        {
            resolvePot(i, isUncontested());
        }

        // end-hand housekeeping
        PokerPlayer player;
        int nNum = getNumPlayers();
        int nLeft = getNumWithCards();
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);

            // record information about hand now that it is done
            // current table only
            if (table_.isCurrent()) recordHandInfo(player, nLeft);

            // record whether player was sitting out or disconnected
            player.endHand();
        }

        table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_END_HAND, table_));
    }

    /**
     * store history for this hand in the DB
     */
    public int storeHandHistory()
    {
        if (!table_.isAllComputer())
        {
            int id = PokerDatabase.storeHandHistory(this);
            bStoredInDatabase_ = true;
            return id;
        }
        return -1;
    }

    /**
     * record hand information in profile
     */
    @SuppressWarnings("CommentedOutCode")
    private void recordHandInfo(PokerPlayer player, int nLeft)
    {
        PlayerProfile prof = player.getProfileInitCheck();
        boolean[] bRounds = new boolean[ROUND_SHOWDOWN + 1];
        Arrays.fill(bRounds, false);
        int nRound;
        HandAction hist;
        int nAction;
        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;
                nRound = hist.getRound();
                nAction = hist.getAction();
                if (hist.getPlayer() != player) continue;
                if (nRound == ROUND_SHOWDOWN) continue;

                // record fact we reached this round
                if (!bRounds[nRound]) {
                    prof.rounds_[nRound]++;
                    bRounds[nRound] = true;
                }

                // record fold-raise
                if (nAction <= HandAction.ACTION_RAISE) {
                    // record action
                    prof.actions_[nAction]++;
                    prof.nActionCnt_++;

                    // record action in round
                    prof.roundactions_[nRound][nAction]++;
                    prof.nRoundActionCnt_[nRound]++;
                }

            }
        }

        // if no record for showdown and player hasn't folded, then they lost, so record that
        // we saw the river
        if (!isFolded(player))
        {
            // record an action on all rounds if we are here with other players
            // and nothing yet recorded (happens when all-in before showdown)
            for (int i = ROUND_FLOP; nLeft > 1 && i <= ROUND_SHOWDOWN; i++)
            {
                if (!bRounds[i]) prof.rounds_[i]++;
            }

            // record if money was won
            int nWin = getWin(player);
            if (nWin > 0)
            {
                prof.nWins_++;
            }
        }

        // if we saw the flop, record our position at the table
        if (bRounds[ROUND_FLOP])
        {
            prof.flops_[player.getStartingPositionCategory()]++;
        }

        // This was a 1.0 thing and is no longer necessary.  We
        // used to save player stats so the AI could remember
        // from game to game, but the V1 ai isn't really used
        // anymore, so no longer necessary to save each hand.
        //prof.saveIfPossible();
        //if (player.isHuman()) prof.debugPrint();
    }

    /**
     * pre-resolve pot, figuring out winners and losers who don't have to show
     */
    private void preResolvePot(int nPotNum, List<PokerPlayer> winners, List<PokerPlayer> losers)
    {
        Pot pot = getPot(nPotNum);
        PokerPlayer player;
        HandInfo info;

        // get info for each player in pot.
        // This iterates in showdown order.
        int nHighScore = 0;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);
            if (player.isFolded()) continue;
            if (!pot.isInPot(player)) continue;
            info = player.getHandInfo();

            // if score is greater than or equal to the highest score,
            // this player's cards should be exposed
            if (info.getScore() >= nHighScore)
            {
                if (info.getScore() > nHighScore) winners.clear();
                nHighScore = info.getScore();
                winners.add(player);
            }
            // this loser doesn't have to show hand unless in
            // all-in showdown
            else if (!isAllInShowdown())
            {
                losers.add(player);
            }
        }
    }

    /**
     * resolve pot
     */
    private void resolvePot(int nPotNum, boolean bUncontested)
    {
        Pot pot = getPot(nPotNum);
        PokerPlayer player;
        HandInfo info;
        List<PokerPlayer> winners = new ArrayList<>();

        // get info for each player in pot.
        // This iterates in showdown order.
        // Mark whether hand displayed.
        int nHighScore = 0;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);
            if (player.isFolded()) continue;
            if (!pot.isInPot(player)) continue;
            info = player.getHandInfo();

            // if score is greater than or equal to the highest score,
            // this player's cards should be exposed
            if (info.getScore() >= nHighScore)
            {
                nHighScore = info.getScore();
                if (!bUncontested || player.isShowWinning()) player.setCardsExposed(true);
            }
            else if (!player.isMuckLosing())
            {
                player.setCardsExposed(true);
            }

            // regardless of win or lose, if we had an all-in
            // showdown, cards were exposed
            if (isAllInShowdown())
            {
                player.setCardsExposed(true);
            }
        }

        // make sure we have a winner
        if (nHighScore == 0)
        {
            logger.warn("WARNING: no winners for pot: {}", pot);
            debugPrint();
            return;
        }

        // count winners (order is based on button, so first
        // winner found is closest to button for odd chips;
        // we need to fetch order as of flop since it changes
        // in showdown to order of card display)
        List<PokerPlayer> order = new ArrayList<>();
        HoldemHand.setPlayerOrder(table_, order, HoldemHand.ROUND_FLOP, true);
        for (int i = 0; i < nNum; i++)
        {
            player = order.get(i);
            if (player.isFolded()) continue;
            if (!pot.isInPot(player)) continue;
            info = player.getHandInfo();
            if (info.getScore() == nHighScore)
            {
                winners.add(info.getPlayer());
            }
        }

        // remember winners of this pot
        pot.setWinners(winners);

        // determine share of pot
        int nWinners = winners.size();
        int nMinChip = getMinChip();
        int nPot = pot.getChipCount();
        int nShare = nPot / nWinners;
        nShare -= nShare % nMinChip;
        int nRemainder = nPot - (nShare * nWinners);
        if (nRemainder % nMinChip != 0)
        {
            logger.warn("Remainder of {} isn't multiple of chip {}  pot:{}", nRemainder, nMinChip, pot);
        }

        // allocate pot to winners, go through winner array
        // so odd chips allocated properly to players nearest
        // the dealer button
        int nAmount;
        for (PokerPlayer winner : winners)
        {
            nAmount = nShare;

            // closest to button is at top of list and they get extra chips
            if (nRemainder > 0)
            {
                nAmount += nMinChip;
                nRemainder -= nMinChip;
            }
            player = winner;
            player.setPendingWin(nAmount);
        }

        // assigning winnings, note winners, losers.  We do this
        // in card display order so hand history order
        // reflects showdown order.
        int nTotalCheck = 0;
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);
            if (player.isFolded()) continue;
            if (!pot.isInPot(player)) continue;

            if (winners.contains(player))
            {
                nAmount = player.getPendingWin();
                // if num players in pot is only 1, this is return of an overbet
                if (pot.isOverbet())
                {
                    player.overbet(nAmount, nPotNum);
                }
                else
                {
                    player.wins(nAmount, nPotNum);
                }
                nTotalCheck += nAmount;
                player.setPendingWin(0);
            }
            else
            {
                player.lose(nPotNum);
            }
        }

        // verify we allocated the entire pot
        if (nTotalCheck != nPot)
        {
            logger.warn("Amount awarded ({}) != pot amount ({})", nTotalCheck, nPot);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        table_ = (PokerTable) state.getObject(list.removeIntegerToken());
        deck_ = (Deck) list.removeToken();
        pots_ = (DMArrayList<Pot>) list.removeToken();
        history_ = (DMArrayList<HandAction>) list.removeToken();
        nRound_ = list.removeIntToken();
        nAnte_ = list.removeIntToken();
        nBigBlind_ = list.removeIntToken();
        nSmallBlind_ = list.removeIntToken();
        nGameType_ = list.removeIntToken();
        muck_ = (Hand) list.removeToken();
        community_ = (Hand) list.removeToken();
        communitySorted_ = new HandSorted(community_);
        nCurrentPlayerIndex_ = list.removeIntToken();
        bAllInShowdown_ = list.removeBooleanToken();
        potStatus_ = list.removeIntToken();
        startDate_ = list.removeLongToken();
        endDate_ = list.removeLongToken();
        PokerTable.loadPlayerList(state, list, playerOrder_);
        if (list.hasMoreTokens())
        {
            PokerTable.loadPlayerList(state, list, winners_);
            PokerTable.loadPlayerList(state, list, losers_);
        }
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(state.getId(table_));
        list.addToken(deck_);
        synchronized (pots_)
        {
            list.addToken(pots_);
        }
        synchronized (history_)
        {
            list.addToken(history_);
        }
        list.addToken(nRound_);
        list.addToken(nAnte_);
        list.addToken(nBigBlind_);
        list.addToken(nSmallBlind_);
        list.addToken(nGameType_);
        list.addToken(muck_);
        list.addToken(community_);
        // communitySorted_ not stored (recreated upon load)
        list.addToken(nCurrentPlayerIndex_);
        list.addToken(bAllInShowdown_);
        list.addToken(potStatus_);
        list.addToken(startDate_);
        list.addToken(endDate_);
        PokerTable.addPlayerList(state, list, playerOrder_);
        PokerTable.addPlayerList(state, list, winners_);
        PokerTable.addPlayerList(state, list, losers_);
        return list.marshal(state);
    }

    public String getHandListHTML()
    {
        PokerPlayer player = getTable().getGame().getHumanPlayer();
        Hand hand = (player != null && !player.isObserver()) ? player.getHand() : null;
        return ((hand != null) ? hand.toHTML() : Card.BLANK.toHTML() + Card.BLANK.toHTML()) +
               "&nbsp;&nbsp;" +
               getCommunity().toHTML();
    }

    public long getStartDate()
    {
        return startDate_;
    }

    public long getEndDate()
    {
        return endDate_;
    }

    Boolean wasRaisedPreFlop_ = null;

    public boolean wasRaisedPreFlop()
    {
        if (wasRaisedPreFlop_ == null)
        {
            synchronized (history_)
            {
                HandAction hist;

                for (HandAction handAction : history_) {
                    hist = handAction;

                    if (hist.getRound() > ROUND_PRE_FLOP) {
                        wasRaisedPreFlop_ = Boolean.FALSE;
                        break;
                    }

                    if (hist.getAction() == HandAction.ACTION_RAISE) {
                        wasRaisedPreFlop_ = Boolean.TRUE;
                        break;
                    }
                }
            }

            if (wasRaisedPreFlop_ == null)
            {
                wasRaisedPreFlop_ = Boolean.FALSE;
            }
        }

        return wasRaisedPreFlop_;
    }

    /**
     * Returns true if the player put chips in the pot voluntarily pre-flop (even if subsequently raised out).
     * <p/>
     * Note this means false will be returned if a player checked the big blind.
     */
    public boolean paidToPlay(PokerPlayer player)
    {
        if (player == null) return false;

        HandAction hist;

        synchronized (history_)
        {
            for (int i = history_.size() - 1; i >= 0; --i)
            {
                hist = history_.get(i);

                if ((hist.getRound() == ROUND_PRE_FLOP) && (hist.getPlayer() == player))
                {
                    switch (hist.getAction())
                    {
                        case HandAction.ACTION_BET:
                        case HandAction.ACTION_CALL:
                        case HandAction.ACTION_RAISE:
                            return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the number of players who have limped into the pot so far.
     * <p/>
     * Once the pot is raised, this method will return zero.
     */
    public int getNumLimpers()
    {
        HandAction hist;

        int limpers = 0;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() == ROUND_PRE_FLOP) {
                    switch (hist.getAction()) {
                        case HandAction.ACTION_CALL:
                            ++limpers;
                            break;
                        case HandAction.ACTION_RAISE:
                            limpers = 0;
                            break;
                    }
                } else {
                    break;
                }
            }
        }

        return limpers;
    }

    /**
     * Returns the player's first voluntary action in a given round.
     */
    public HandAction getFirstVoluntaryAction(PokerPlayer player, int round)
    {
        if (player == null) return null;

        HandAction hist;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() == round) {
                    if (hist.getPlayer() == player) {
                        switch (hist.getAction()) {
                            case HandAction.ACTION_BLIND_BIG:
                            case HandAction.ACTION_BLIND_SM:
                            case HandAction.ACTION_ANTE:
                                continue;
                            default:
                                return hist;
                        }
                    }
                } else if (hist.getRound() > round) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Returns the player who put in the first bet or raise in a particular round, or null.
     * If withChips is true, all-in players are skipped.
     */
    public PokerPlayer getFirstBettor(int round, boolean withChips)
    {
        HandAction hist;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                PokerPlayer player = hist.getPlayer();

                if ((hist.getRound() == round) && (!withChips || player.getChipCount() > 0)) {
                    switch (hist.getAction()) {
                        case HandAction.ACTION_BET:
                        case HandAction.ACTION_RAISE:
                            return player;
                    }
                } else if (hist.getRound() > round) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Returns the player who put in the last bet or raise in a particular round, or null.
     * If withChips is true, all-in players are skipped.
     */
    public PokerPlayer getLastBettor(int round, boolean withChips)
    {
        HandAction hist;

        synchronized (history_)
        {
            int size = history_.size();

            for (int i = size - 1; i >= 0; --i)
            {
                hist = history_.get(i);

                PokerPlayer player = hist.getPlayer();

                if ((hist.getRound() == round) && (!withChips || player.getChipCount() > 0))
                {
                    switch (hist.getAction())
                    {
                        case HandAction.ACTION_BET:
                        case HandAction.ACTION_RAISE:
                            return player;
                    }
                }
                else if (hist.getRound() < round)
                {
                    return null;
                }
            }
        }

        return null;
    }

    public int getNumFoldsSinceLastBet()
    {
        HandAction hist;
        int count = 0;

        synchronized (history_)
        {
            int size = history_.size();

            for (int i = size - 1; i >= 0; --i)
            {
                hist = history_.get(i);

                switch (hist.getAction())
                {
                    case HandAction.ACTION_FOLD:
                        ++count;
                        break;
                    case HandAction.ACTION_BET:
                    case HandAction.ACTION_RAISE:
                        return count;
                }
            }
        }

        return count;
    }

    /**
     * Returns true if the player could have limped on first action.
     */
    public boolean couldLimp(PokerPlayer player)
    {
        if (player == null) return false;

        HandAction hist;

        synchronized (history_)
        {
            boolean canLimp = true;

            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > ROUND_PRE_FLOP) {
                    return false;
                }
                if (hist.getPlayer() == player) {
                    switch (hist.getAction()) {
                        case HandAction.ACTION_BLIND_BIG:
                        case HandAction.ACTION_BLIND_SM:
                        case HandAction.ACTION_ANTE:
                            continue;
                        case HandAction.ACTION_CHECK:
                            return false;
                        default:
                            return canLimp;
                    }
                } else {
                    if (hist.getAction() == HandAction.ACTION_RAISE) {
                        canLimp = false;
                    }
                }
            }
        }

        return false;
    }


    /**
     * Returns Boolean.TRUE if the player took the opportunity to open the pot in the given round.
     * Returns Boolean.FALSE if the player passed up the opportunity to open the pot in the given round.
     * Returns null if the player didn't have the opportunity to open the pot in the given round.
     */
    public Boolean betPot(PokerPlayer player, int round)
    {
        if (player == null) return null;

        HandAction hist;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > round) return null;
                if (hist.getRound() < round) continue;
                if (hist.getPlayer() != player) continue;
                if (hist.getAction() == HandAction.ACTION_CHECK) return Boolean.FALSE;
                if (hist.getAction() == HandAction.ACTION_BET) return Boolean.TRUE;
            }
        }

        return null;
    }

    /**
     * Returns Boolean.TRUE if the player took the opportunity to raise the pot in the given round.
     * Returns Boolean.FALSE if the player passed up the opportunity to raise the pot in the given round.
     * Returns null if the player didn't have the opportunity to raise the pot in the given round.
     */
    public Boolean raisedPot(PokerPlayer player, int round)
    {
        if (player == null) return null;

        HandAction hist;
        boolean acted = false;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > round) return null;
                if (hist.getRound() < round) continue;
                if (hist.getPlayer() != player) continue;
                if (hist.getAction() == HandAction.ACTION_RAISE) return Boolean.TRUE;
                acted = true;
            }
        }

        return acted ? Boolean.FALSE : null;
    }

    /**
     * Returns true if the player was the first raiser pre-flop.
     */
    public boolean wasFirstRaiserPreFlop(PokerPlayer player)
    {
        if (player == null) return false;

        HandAction hist;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > ROUND_PRE_FLOP) {
                    return false;
                }
                if (hist.getAction() == HandAction.ACTION_RAISE) {
                    return (hist.getPlayer() == player);
                }
            }
        }

        return false;
    }

    /**
     * Returns true if the player was the last raiser pre-flop.
     */
    public boolean wasLastRaiserPreFlop(PokerPlayer player)
    {
        if (player == null) return false;

        HandAction hist;
        boolean lastRaiser = false;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > ROUND_PRE_FLOP) {
                    break;
                }
                if (hist.getAction() == HandAction.ACTION_RAISE) {
                    lastRaiser = (hist.getPlayer() == player);
                }
            }
        }

        return lastRaiser;
    }

    /**
     * Returns true if the player was the only raiser pre-flop.
     */
    public boolean wasOnlyRaiserPreFlop(PokerPlayer player)
    {
        if (player == null) return false;

        HandAction hist;
        boolean onlyRaiser = false;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > ROUND_PRE_FLOP) {
                    break;
                }
                if (hist.getAction() == HandAction.ACTION_RAISE) {
                    if (hist.getPlayer() != player) {
                        return false;
                    } else {
                        onlyRaiser = true;
                    }
                }
            }
        }

        return onlyRaiser;
    }


    /**
     * Returns true if the player limped on first action.
     */
    public boolean limped(PokerPlayer player)
    {
        HandAction firstAction = getFirstVoluntaryAction(player, ROUND_PRE_FLOP);
        int action = (firstAction != null) ? firstAction.getAction() : HandAction.ACTION_NONE;
        return couldLimp(player) && (action == HandAction.ACTION_CALL);
    }

    /**
     * Returns true if the player folded pre-flop.
     * <p/>
     * Note this means false will be returned if a player checked the big blind.
     */
    @SuppressWarnings("unused")
    public boolean foldedPreFlop(PokerPlayer player)
    {
        if (player == null) return false;

        HandAction hist;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > ROUND_PRE_FLOP) {
                    return false;
                }
                if (
                        (hist.getRound() == ROUND_PRE_FLOP) &&
                                (hist.getPlayer() == player) &&
                                (hist.getAction() == HandAction.ACTION_FOLD)
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean getWasPotAction(int round)
    {
        HandAction hist;

        synchronized (history_)
        {
            for (HandAction handAction : history_) {
                hist = handAction;

                if (hist.getRound() > round) {
                    break;
                }
                if ((hist.getRound() == round) && ((hist.getAction() == HandAction.ACTION_BET) || (hist.getAction() == HandAction.ACTION_RAISE))) {
                    return true;
                }
            }
        }

        return false;
    }

    //
    // AI common data
    //
    private PocketWeights pw_;

    public PocketWeights getPocketWeights()
    {
        return pw_;
    }

    public void setPocketWeights(PocketWeights pw)
    {
        pw_ = pw;
    }
}
