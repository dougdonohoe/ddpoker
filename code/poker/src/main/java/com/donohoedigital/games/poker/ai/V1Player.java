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
package com.donohoedigital.games.poker.ai;

import com.ddpoker.holdem.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.config.*;

import java.util.*;

@DataCoder('1')
public class V1Player extends PokerAI
{
    // some times there is no chance of improvement, but
    // you have to account for odds a player is bluffing
    private static int MIN_IMPROVE_ODDS = 7;

    // stored values
    private int nRebuyPropensity_;
    private int nAddonPropensity_;
    private int nTightFactor_; // 0 - 100, 100 being very tight
    private int nBluffFactor_; // 0 - 100, 100 being lots of bluffing

    // fast hand scores
    private HandInfoFaster FASTER = new HandInfoFaster();

    // transient values used in this section, set in getAction(),
    // all start with "_"
    private PokerGame game_;
    protected TournamentProfile profile_;
    private HoldemHand _hhand;
    private int _nToCall;
    private int _nLast;
    private ArrayList _players = new ArrayList();
    private int _nNumAfter;
    private int _nNumBefore;
    private int seatsOccupied_;
    private HandSorted _hole;
    private HandSorted _comm;
    private PokerPlayer _bettor, _raiser;
    private int _bettorPre,  _raiserPre;
    private int _bettorFlop, _raiserFlop;
    private int _bettorTurn, _raiserTurn;
    private int[] _best = new int[5];
    private int _nLastRoundBet;
    private float _potOdds;
    private float _dImprove; // set in getPostFlop()
    private int _skill;
    private float _betPercOfStack;
    protected int betAmount_;

    private boolean bIntendCheckRaise_ = false;
    // ai
    public static final int AI_EASY = 1;
    public static final int AI_MEDIUM = 2;
    public static final int AI_HARD = 3;


    public void setPokerPlayer(PokerPlayer player)
    {
        super.setPokerPlayer(player);

        // set seed with 1st call to dice roller baed on player's name
        // so that each named player has the same behavoir
        nTightFactor_ = DiceRoller.rollDieInt(100, getPokerPlayer().getName().hashCode());
        nBluffFactor_ = DiceRoller.rollDieInt(100);
        nAddonPropensity_ = DiceRoller.rollDieInt(100);
        nRebuyPropensity_ = DiceRoller.rollDieInt(100);
        DiceRoller.newSeed(); // set new seed so subsequent calls to dice roller aren't the same
    }
    /**
     * Return true if we want to rebuy
     */
    public boolean wantsRebuy()
    {
        return WantsRebuy(getPokerPlayer(), nRebuyPropensity_);
    }

    /**
     * Rebuy logic for computer players
     */
    public static boolean WantsRebuy(PokerPlayer player, int nRebuyPropensity)
    {
        int nNumRebuys = player.getNumRebuys();

        // BUG 395 we have to protect players from going broke :-)
        if (nNumRebuys >= 5) return false;

        if (nRebuyPropensity <= 25) return true; // 25% always
        if (nRebuyPropensity <= 50)
        {
            if (nNumRebuys < 3) return true; // 25% 3x
            return false;
        }
        if (nRebuyPropensity <= 75)
        {
            if (nNumRebuys < 2) return true; // 25% 2x
            return false;
        }
        if (nRebuyPropensity <= 90)
        {
            if (nNumRebuys < 1) return true; // 15% 1x
            return false;
        }
        // 10% never

        return false;
    }

    /**
     * init
     */
    protected void checkInit()
    {
        if (game_ == null) game_ = ((PokerPlayer)gamePlayer_).getTable().getGame();
        if (profile_ == null) profile_ = game_.getProfile();
    }

    /**
     * Return true if we want to add-on
     */
    public boolean wantsAddon()
    {
        checkInit();
        return WantsAddon(getPokerPlayer(), nAddonPropensity_, profile_);
    }

    /**
     * Default addon logic for AI
     */
    public static boolean WantsAddon(PokerPlayer player, int nAddonPropensity, TournamentProfile profile)
    {
        int nBuyin = profile.getBuyinChips();
        if (nAddonPropensity < 25) return true; // 25% always
        if (nAddonPropensity < 50)
        {
            if (player.getChipCount() < (3 * nBuyin)) return true; // 25% if chips < 3x buyin
            return false;
        }
        if (nAddonPropensity < 75)
        {
            if (player.getChipCount() < (2 * nBuyin)) return true; // 25% if chips < 2x buyin
            return false;
        }
        return false;
    }

    /**
     * Get tight factor.  Adjusted down if rebuys can still happen
     */
    private int getTightFactor()
    {
        PokerPlayer player = getPokerPlayer();

        int t = nTightFactor_;

        if (game_ != null && !player.getTable().isRebuyDone(player))
        {
            t -= 20;
            if (t < 0) t = 0;
        }

        if (isEasy())
            t += 20;
        else if (isMedium()) t += 10;

        // tight on the river
        if (isHard() && isRiver()) t += 20;

        return t;
    }

    /**
     * Get bluff factor.
     */
    private int getBluffFactor()
    {
        int t = nBluffFactor_;

        if (isEasy())
            t -= 20;
        else if (isMedium()) t -= 10;

        if (t < 0) t = 0;

        // less bluffing on the river
        if (isHard() && isRiver()) t -= 20;

        return t;
    }

    public PlayerAction getAction(boolean quick)
    {
        PokerPlayer player = getPokerPlayer();

        betAmount_ = 0;

        // stuff that won't change
        checkInit();

        // get hand and info needed for quickAction
        _hhand = player.getHoldemHand();
        _nToCall = _hhand.getCall(player);
        _nLast = _hhand.getLastActionThisRound(player);

        // reset check-raise flag
        if (_nLast == HandAction.ACTION_NONE) bIntendCheckRaise_ = false;

        seatsOccupied_ = _hhand.getNumPlayers();

        // get remaining info
        _betPercOfStack = 100.0f * (float) _nToCall / (float) player.getChipCount();
        _dImprove = MIN_IMPROVE_ODDS;
        _players.clear();
        _hhand.getPlayersLeft(_players, player);
        _hole = player.getHandSorted();
        _comm = _hhand.getCommunitySorted();
        _nNumBefore = _hhand.getNumBefore(player);
        _nNumAfter = _hhand.getNumAfter(player);
        _bettor = _hhand.getBettor();
        _raiser = _hhand.getRaiser();
        _potOdds = _hhand.getPotOdds(player);
        _skill = ((PokerAI) player.getGameAI()).getPlayerType().getStratValue("v1skill", AI_EASY);

        // debug
        if (true && TESTING(EngineConstants.TESTING_AI_DEBUG) && player.getTable().isCurrent())
        {
            logger.debug(player.getName() + " tight: " + getTightFactor());
            logger.debug(player.getName() + " bluff: " + getBluffFactor());
            logger.debug(player.getName() + " - bettor: " + (_bettor == null ? "n/a" : _bettor.getName()));
            logger.debug(player.getName() + " - raiser: " + (_raiser == null ? "n/a" : _raiser.getName()));
            logger.debug(player.getName() + " - num before: " + _nNumBefore);
            logger.debug(player.getName() + " - num after: " + _nNumAfter);
            logger.debug(player.getName() + " - pot odds: " + _potOdds);
            logger.debug(player.getName() + " - bet % of stack: " + _betPercOfStack);
            if (isPreFlop()) logger.debug(player.getName() + " - improve: " + _dImprove);
        }

        // testing
        if (TESTING(PokerConstants.TESTING_AI_ALWAYS_CALLS))
        {
            if (_nToCall == 0) return _bet("debug always call");
            return PlayerAction.call().reason("V1:debug always call");
        }

        // pre flop
        if (isPreFlop())
        {
            return getPreFlop();
        }

        // flop/turn/river
        _nLastRoundBet = _hhand.getBet(getBettingRound() - 1);
        _bettorPre = _hhand.getLastActionAI(_bettor, HoldemHand.ROUND_PRE_FLOP);
        _raiserPre = _hhand.getLastActionAI(_raiser, HoldemHand.ROUND_PRE_FLOP);
        _bettorFlop = _hhand.getLastActionAI(_bettor, HoldemHand.ROUND_FLOP);
        _raiserFlop = _hhand.getLastActionAI(_raiser, HoldemHand.ROUND_FLOP);
        _bettorTurn = _hhand.getLastActionAI(_bettor, HoldemHand.ROUND_TURN);
        _raiserTurn = _hhand.getLastActionAI(_raiser, HoldemHand.ROUND_TURN);

        return getPostFlop();
    }


    /////
    ///// PRE-FLOP
    /////

    /**
     * Sklansky's system as described pp 128-133 in
     * Tournament Poker for Advanced Players
     */
    private PlayerAction getSystem()
    {
        // pg 131 - pot raised?
        if (isPotRaised())
        {
            if (_hole.isEquivalent(HoldemExpert.AA) ||
                    _hole.isEquivalent(HoldemExpert.KK) ||
                    _hole.isEquivalent(HoldemExpert.AKs))
            {
                betAmount_ = Integer.MAX_VALUE;
                return PlayerAction.bet().reason("V1:system raised");
            }
            else
            {
                return PlayerAction.fold().reason("V1:system raised");
            }
        }

        float nPot = _hhand.getAntesBlinds();
        float nMax = _hhand.getMaxBet(getPokerPlayer());

        float theNumber = nMax / nPot;
        if (_nNumAfter > 0) theNumber *= _nNumAfter;
        theNumber *= (_nNumBefore + 1); // num before for this person is limpers (raise case handled above)

        boolean bAllIn = false;
        if (theNumber >= 400)
        {
            if (_hole.isEquivalent(HoldemExpert.AA)) bAllIn = true;
        }
        else if (theNumber >= 200)
        {
            if (_hole.isEquivalent(HoldemExpert.AA) ||
                    _hole.isEquivalent(HoldemExpert.KK))
                bAllIn = true;
        }
        else if (theNumber >= 150)
        {
            if (_hole.isEquivalent(HoldemExpert.AA) ||
                    _hole.isEquivalent(HoldemExpert.KK) ||
                    _hole.isEquivalent(HoldemExpert.QQ) ||
                    _hole.isEquivalent(HoldemExpert.AKs) ||
                    _hole.isEquivalent(HoldemExpert.AKo)
            )
                bAllIn = true;
        }
        else if (theNumber >= 100)
        {
            if (_hole.isEquivalent(HoldemExpert.AA) ||
                    _hole.isEquivalent(HoldemExpert.KK) ||
                    _hole.isEquivalent(HoldemExpert.QQ) ||
                    _hole.isEquivalent(HoldemExpert.JJ) ||
                    _hole.isEquivalent(HoldemExpert.TT) ||
                    _hole.isEquivalent(HoldemExpert.AKs) ||
                    _hole.isEquivalent(HoldemExpert.AKo) ||
                    _hole.isEquivalent(HoldemExpert.AQs) ||
                    _hole.isEquivalent(HoldemExpert.AQo) ||
                    _hole.isEquivalent(HoldemExpert.KQs) ||
                    _hole.isEquivalent(HoldemExpert.KQo)
            )
                bAllIn = true;
        }
        else if (theNumber >= 80)
        {
            if (_hole.isPair() ||
                    _hole.isEquivalent(HoldemExpert.AKs) ||
                    _hole.isEquivalent(HoldemExpert.AKo) ||
                    _hole.isEquivalent(HoldemExpert.AQs) ||
                    _hole.isEquivalent(HoldemExpert.AQo) ||
                    _hole.isEquivalent(HoldemExpert.KQs) ||
                    _hole.isEquivalent(HoldemExpert.KQo) ||
                    (_hole.isSuited() && _hole.isInHand(Card.ACE)) ||
                    (_hole.isSuited() && _hole.hasConnector(0, 4))
            )
                bAllIn = true;
        }
        else if (theNumber >= 20)
        {
            // 20-40 is same as below plus any suited cards
            if (theNumber < 40 && _hole.isSuited()) bAllIn = true;

            // 40-60 is same as 60-80 plus any king
            if (!bAllIn && theNumber < 60 && _hole.isInHand(Card.KING)) bAllIn = true;

            if (!bAllIn &&
                    _hole.isPair() ||
                    _hole.isInHand(Card.ACE) ||
                    _hole.isEquivalent(HoldemExpert.KQo) ||
                    (_hole.isSuited() && _hole.isInHand(Card.KING)) ||
                    (_hole.isSuited() && _hole.hasConnector(1))
            )
                bAllIn = true;
        }
        else if (theNumber < 20)
        {
            bAllIn = true;
        }

        if (bAllIn)
        {
            betAmount_ = Integer.MAX_VALUE;
            return PlayerAction.bet().reason("V1:system key num=" + theNumber);
        }
        else
        {
            return PlayerAction.fold().reason("V1:system key num=" + theNumber);
        }
    }

    /**
     * pre flop
     */
    private PlayerAction getPreFlop()
    {
        PokerPlayer player = getPokerPlayer();

        // hard mode, 5% of players use Sklansky's "all-in system"
        // if more than 4 players at the table
        if (isHard() && nTightFactor_ <= 5 && seatsOccupied_ > 4)
        {
            return getSystem();
        }

        // sklansky rank
        int sklanskyRank = HoldemExpert.getSklanskyRank(_hole);

        // random number from 1-100
        int nRandom = DiceRoller.rollDieInt(100);

        // when to play loose
        int nLooseThreshold;
        int nTight = getTightFactor();
        if (nTight <= 25)
            nLooseThreshold = 20;
        else if (nTight <= 50)
            nLooseThreshold = 15;
        else if (nTight <= 75)
            nLooseThreshold = 10;
        else
            nLooseThreshold = 5;

        ////
        //// EARLY
        ////
        if (player.isEarly())
        {
            if (isPotRaised())
            {
                if (_hole.isEquivalent(HoldemExpert.AA) ||
                        _hole.isEquivalent(HoldemExpert.KK) ||
                        _hole.isEquivalent(HoldemExpert.QQ) ||
                        _hole.isEquivalent(HoldemExpert.AKs))
                {
                    return _raise("early raised AA-AKs");
                }
                else if (sklanskyRank <= HoldemExpert.MAXGROUP2)
                {
                    return PlayerAction.call().reason("V1:early raised G2");
                    // TODO: tight game, throw away AJs, KQs
                }
                else
                {
                    return _foldLooseCheck("early raised");
                }
                // TODO: loose, add Group 3, beware AQ
            }
            else
            {
                // SK18 - no callers
                if (_hhand.getNumCallers() == 0)
                {
                    if (_hole.isEquivalent(HoldemExpert.AA) ||
                            _hole.isEquivalent(HoldemExpert.KK) ||
                            _hole.isEquivalent(HoldemExpert.QQ) ||
                            _hole.isEquivalent(HoldemExpert.AKo) ||
                            _hole.isEquivalent(HoldemExpert.AQo)
                    )
                    {
                        // SK18 - almost always raise
                        if (nRandom <= 97)
                        {
                            return _raise("early 0 caller, 97%");
                        }
                        else
                        {
                            return PlayerAction.call().reason("V1:early 0 caller, 0%");
                        }
                    }
                    // other high hands
                    else if (
                            _hole.isEquivalent(HoldemExpert.AKs) ||
                            _hole.isEquivalent(HoldemExpert.KQs) ||
                            _hole.isEquivalent(HoldemExpert.AJs) ||
                            _hole.isEquivalent(HoldemExpert.KQs)
                    )
                    {
                        // SK18 - raise 2/3rds with these
                        if (nRandom <= 66)
                        {
                            return _raise("early 0 caller 66%");
                        }
                        else
                        {
                            return PlayerAction.call().reason("V1:early 0f caller 33%");
                        }
                    }
                    // SK18 occasionally raise these hands
                    else if (nRandom <= 33 && sklanskyRank <= HoldemExpert.MAXGROUP4 && _hole.isSuited())
                    {
                        return _raise("early suited G4 33%");
                    }
                }

                // SK16 - play 1-3 (assuming tough)
                if (sklanskyRank <= HoldemExpert.MAXGROUP3)
                {
                    return PlayerAction.call().reason("V1:early G3");
                    // TODO: GROUP4 (normal game)
                    // TODO: GROUP5 (loose game)
                }
                // SK17 - "occasionally" play suited/paired lesser hands, 2 groups less,
                // (we'll play threshold%)
                else if (sklanskyRank <= (HoldemExpert.MAXGROUP3 + (2 * HoldemExpert.MULT))
                        && nRandom <= nLooseThreshold &&
                        (_hole.isSuited() || _hole.isPair()))
                {
                    return PlayerAction.call().reason("V1:early 5%");
                }

                return _foldLooseCheck("early");
            }
        }
        ////
        //// MIDDLE
        ////
        else if (player.isMiddle())
        {
            if (isPotRaised())
            {
                // SK21 - almost always reraise with these
                if (_hole.isEquivalent(HoldemExpert.AA) ||
                        _hole.isEquivalent(HoldemExpert.KK) ||
                        _hole.isEquivalent(HoldemExpert.QQ) ||
                        _hole.isEquivalent(HoldemExpert.AKs) ||
                        _hole.isEquivalent(HoldemExpert.AKo))
                {
                    // 97% of time re-raise
                    if (nRandom <= 97)
                    {
                        return _raise("middle raised 97%");
                    }
                    else
                    {
                        return PlayerAction.call().reason("V1:middle raised 3%");
                    }
                }
                // SK22 - occasionally re-raise with group 4 and better
                else if (nRandom <= 5 && sklanskyRank <= HoldemExpert.MAXGROUP4)
                {
                    return _raise("middle raised 5% G4");
                }
                else if (sklanskyRank <= HoldemExpert.MAXGROUP3)
                {
                    return PlayerAction.call().reason("V1:middle raised G3");
                }
                else
                {
                    return _foldLooseCheck("middle raised");
                }
                // TODO: loose, add Group 3, beware AQ
                // TODO: re-raise with AA, KK, QQ, AK ?
            }
            else
            {
                // SK22 - no callers
                if (_hhand.getNumCallers() == 0)
                {
                    // SK22 - raise 1,2,3
                    if (sklanskyRank <= HoldemExpert.MAXGROUP3)
                    {
                        return _raise("middle 0 callers G3");
                    }
                    else if (sklanskyRank <= HoldemExpert.MAXGROUP6)
                    {
                        return PlayerAction.call().reason("V1:middle 0 callers G6");
                    }
                }
                else
                {
                    // SK22 - callers - raise 1,2 (not 3)
                    if (sklanskyRank <= HoldemExpert.MAXGROUP2)
                    {
                        return _raise("middle G2");
                    }
                }

                if (sklanskyRank <= HoldemExpert.MAXGROUP5)
                {
                    // TODO: GROUP3 - strong opponents, call; otherwise raise
                    // TODO: GROUP6 - loose,passive
                    return PlayerAction.call().reason("V1:middle G5");
                }
                // SK17 - "occasionally" play suited/paired lesser hands, 2 groups less,
                // (we'll play threshold%)
                else if (sklanskyRank <= (HoldemExpert.MAXGROUP4 + (2 * HoldemExpert.MULT))
                        && nRandom <= nLooseThreshold &&
                        (_hole.isSuited() || _hole.isPair()))
                {
                    return PlayerAction.call().reason("V1:middle suited 5%");
                }

                return _foldLooseCheck("middle");
            }
        }
        ////
        //// LATE
        ////
        else if (player.isLate())
        {
            if (isPotRaised())
            {
                // SK21 - almost always reraise with these
                if (_hole.isEquivalent(HoldemExpert.AA) ||
                        _hole.isEquivalent(HoldemExpert.KK) ||
                        _hole.isEquivalent(HoldemExpert.QQ) ||
                        _hole.isEquivalent(HoldemExpert.AKs) ||
                        _hole.isEquivalent(HoldemExpert.AKo))
                {
                    // 97% of time re-raise
                    if (nRandom <= 97)
                    {
                        return _raise("late raised 97%");
                    }
                    else
                    {
                        return PlayerAction.call().reason("V1:late raised 3%");
                    }
                }
                // SK22 - occasionally re-raise with group 2 and better
                else if (nRandom <= 5 && sklanskyRank <= HoldemExpert.MAXGROUP3)
                {
                    return _raise("late raised 5% G4");
                }
                // SK158 (11) - call a raise cold - need "very good" hand
                else if (sklanskyRank <= HoldemExpert.MAXGROUP2)
                {
                    return PlayerAction.call().reason("V1:late raised G2");
                }
                else
                {
                    if (seatsOccupied_ <= 3)
                    {
                        if ((_hole.isSuited() && _hole.getCard(0).getRank() > 7) ||
                                _hole.hasConnector(0, 9) ||
                                _hole.isInHand(Card.ACE) ||
                                _hole.isPair() ||
                                ((_hole.isInHand(Card.KING) || _hole.isInHand(Card.QUEEN) || _hole.isInHand(Card.JACK)) && _hole.hasConnector(2, 9)))
                        {
                            if (nRandom < 25)
                            {
                                return _raise("late <= 3 players");
                            }
                            else
                            {
                                return PlayerAction.call().reason("V1:late <= 3 players");
                            }
                        }
                    }
                    return _foldLooseCheck("late raised");
                }
                // TODO: loose, add Group 3, beware AQ
            }
            else
            {
                // no callers
                if (_hhand.getNumCallers() == 0)
                {
                    // SK157 (1) - any hand
                    if (sklanskyRank <= HoldemExpert.MAXGROUP8)
                    {
                        return _raise("late 0 callers G8");
                    }
                    // TODO: SK22 - if there is a chance,
                    // TODO: as low as 25%, and 5-6 have folded,
                    // TODO: raise with groups 6 and better

                    // TODO: attempt to steal blinds if bigblind
                    // folds frequently
                }
                else
                {
                    // SK22 - callers - raise 1,2 (not 3)
                    if (sklanskyRank <= HoldemExpert.MAXGROUP3)
                    {
                        return _raise("late G3");
                    }
                    else if (sklanskyRank <= HoldemExpert.MAXGROUP4 && nRandom < 15)
                    {
                        return _raise("late G4 15%");
                    }
                }

                if (sklanskyRank <= HoldemExpert.MAXGROUP6)
                {
                    // TODO: 1-7 calls
                    return PlayerAction.call().reason("V1:late G6");
                }
                // SK17 - "occasionally" play suited/paired lesser hands, 2 groups less,
                // (we'll play threshold%)
                else if (sklanskyRank <= (HoldemExpert.MAXGROUP6 + (2 * HoldemExpert.MULT))
                        && nRandom <= nLooseThreshold &&
                        (_hole.isSuited() || _hole.isPair()))
                {
                    return PlayerAction.call().reason("V1:late suited 5%");
                }

                if (seatsOccupied_ <= 3)
                {
                    if ((_hole.isSuited() && _hole.getCard(0).getRank() > 7) ||
                            _hole.hasConnector(0, 9) ||
                            _hole.isInHand(Card.ACE) ||
                            _hole.isPair() ||
                            ((_hole.isInHand(Card.KING) || _hole.isInHand(Card.QUEEN)) && _hole.hasConnector(2, 9)))
                    {
                        if (nRandom < 25)
                        {
                            if (_nToCall == 0)
                                return _bet("late <= 3 players");
                            else
                                return _raise("late <= 3 players");
                        }
                        else
                        {
                            if (_nToCall == 0)
                                return _bet("late <= 3 players");
                            else
                                return PlayerAction.call().reason("V1:late <= 3 players");
                        }
                    }
                }
                else
                {
                    nRandom = DiceRoller.rollDieInt(100);
                    if (nRandom > getTightFactor())
                    {
                        int nConn = 0;
                        if (getTightFactor() < 10) nConn = 1;
                        nRandom = DiceRoller.rollDieInt(100);
                        if (_hole.isSuited() || _hole.hasConnector(nConn) || _hole.isInHand(Card.ACE) || _hole.isPair())
                        {
                            if (nRandom < 25 && !isEasy())
                            {
                                return _raise("loose suited/connector raise");
                            }
                            else
                            {
                                return PlayerAction.call().reason("V1:loose suited/connector limp");
                            }
                        }
                        else
                        {
                            if (nRandom < 15 && !isEasy())
                            {
                                return _raise("loose garbage raise");
                            }
                            else if (nRandom < 50)
                            {
                                return PlayerAction.call().reason("V1:loose garbage limp");
                            }
                        }
                    }
                }

                return _foldLooseCheck("late");
            }
        }
        ////
        //// BLINDS
        ////
        else if (player.isBlind())
        {
            if (isPotRaised())
            {
                if (sklanskyRank <= HoldemExpert.MAXGROUP1)
                {
                    return _raise("blind raised G1");
                }
                // SK160 (5) - AK group in big blind and are called by only one or two
                else if (player.isBigBlind() && sklanskyRank <= HoldemExpert.MAXGROUP2 && _hhand.getNumCallers() <= 2)
                {
                    return _raise("bigblind raised G2, <= 2 callers");
                }
                else if (sklanskyRank <= HoldemExpert.MAXGROUP2)
                {
                    return PlayerAction.call().reason("V1:blind raised G2");
                }
            }
            else
            {
                if (sklanskyRank <= HoldemExpert.MAXGROUP2)
                {
                    return _raise("blind G2");
                }
                else if (sklanskyRank <= HoldemExpert.MAXGROUP3)
                {
                    return PlayerAction.call().reason("V1:blind G3");
                }
            }

            nRandom = DiceRoller.rollDieInt(100);
            if (nRandom > (getTightFactor() + 10)) // tighten up a bit on blind
            {
                nRandom = DiceRoller.rollDieInt(100);
                if (_hole.isSuited() || _hole.hasConnector(0) || _hole.isInHand(Card.ACE) || _hole.isPair())
                {
                    if (nRandom < 15 && !isEasy())
                    {
                        return _raise("blind loose suited/connector raise");
                    }
                    else
                    {
                        return PlayerAction.call().reason("V1:blind loose suited/connector limp");
                    }
                }
                else
                {
                    if (nRandom < 10 && !isEasy())
                    {
                        return _raise("blind loose garbage raise");
                    }
                    else if (nRandom < 40)
                    {
                        return PlayerAction.call().reason("V1:blind loose garbage limp");
                    }
                }
            }

            if (_nToCall == 0)
                return PlayerAction.check().reason("V1:0 to call blind");

            return _foldLooseCheck("blind");
        }

        return _foldLooseCheck("default");
    }


    /////
    ///// FLOP/TURN/RIVER
    /////

    /**
     * flop/turn/river
     */
    private PlayerAction getPostFlop()
    {
        PokerPlayer player = getPokerPlayer();

        // get our score, hand type and whether our hole cards are part of the hand
        int nScore = FASTER.getScore(_hole, _comm);
        int nMajorSuit = FASTER.getLastMajorSuit();
        int nType = HandInfoFast.getTypeFromScore(nScore);
        boolean inHole = HandInfo.isOurHandInvolved(_hole, nScore, nMajorSuit, false);
        HandInfoFast.getCards(nScore, _best);

        // calculate hand strength
        // TODO: adjust strength based on actions of other players still in hand
        // for example, reduce impact of hands like 72o since is unlikely this
        // would still be around after the flop if the pot was raised
        float dStrength = player.getHandStrength() * 100.0f;

        // draws
        int nNumOppStraights = player.getOppNumStraights();
        int nMaxSuitBoard = _comm.getHighestSuited();
        boolean bFlushDraw = (nMaxSuitBoard == 2);
        boolean bThreeFlush = (nMaxSuitBoard >= 3);
        boolean bBoardPair = _comm.hasPair();
        boolean bStraightDraw = _comm.hasStraightDraw();

        // fix draws on river (no more cards coming!)
        if (isRiver())
        {
            bStraightDraw = false;
            bFlushDraw = false;
        }

        // if not the river betting round, figure out what the
        // future holds
        HandFutures fut = isRiver() ? null : new HandFutures(FASTER, _hole, _comm);

        _dImprove = (fut != null) ? fut.getOddsImprove() : 0.0f;
        // set improve odds to x% on 5th street to represent odds that
        // bettor is bluffing and call is small
        if (_dImprove < MIN_IMPROVE_ODDS) _dImprove = MIN_IMPROVE_ODDS;

        if (TESTING(EngineConstants.TESTING_AI_DEBUG) && player.getTable().isCurrent()) logger.debug(player.getName() + " - improve: " + _dImprove);

        if (false && TESTING(EngineConstants.TESTING_AI_DEBUG) && player.getTable().isCurrent())
        {
            logger.debug("**** POST FLOP for " + player.getName() +
                    ": [ #str/draw? " + nNumOppStraights + "/" + bStraightDraw + "]  [2/3flush? " +
                    bFlushDraw + "/" + bThreeFlush + "]  [pair on board:" + bBoardPair + "] " +
                    " " + HandStat.fPerc.form(dStrength) + "% [" + HandStat.fPerc.form(_potOdds) + "% odds]" +
                    " [" + HandStat.fPerc.form(_dImprove) + "% improve]");
        }

        // random number from 1-100
        int nRandom = DiceRoller.rollDieInt(100);

        // other init
        PlayerAction action;

        // look for check raise
        if ((_nLast == HandAction.ACTION_CHECK) && bIntendCheckRaise_)
        {
            ApplicationError.assertTrue(_nToCall != 0, "Check raise previous action, but nothing to call", _hhand);

            if (nRandom < 5)
            {
                betAmount_ = Integer.MAX_VALUE;
                return PlayerAction.bet().reason("V1:CheckRaise");
            }
            else if (nRandom < 50)
            {
                return _raise(_nToCall * 2, "CheckRaise x2");
            }
            else
            {
                return _raise(_nToCall * 3, "CheckRaise x3");
            }
        }

        // iter over hand types
        switch (nType)
        {
            /////
            ///// ROYAL FLUSH
            ///// STRAIGHT FLUSH
            ///// QUADS
            ///// FULL HOUSE
            /////
            case HandInfo.ROYAL_FLUSH:
            case HandInfo.STRAIGHT_FLUSH:
            case HandInfo.QUADS:
            case HandInfo.FULL_HOUSE:
                if (inHole)
                {
                    return doRaiseCheckRaiseBet(25, 25, "StrFlush/FH");
                }
                // full house, royal, straight flush on board
                else
                {
                    // TODO: bluff we have better hand (if not royal)
                    if (_nToCall > 0)
                    {
                        return PlayerAction.call().reason("V1:StrFlushBoard");
                    }
                    else
                    {
                        return PlayerAction.check().reason("V1:StrFlushBoard");
                    }
                }

                /////
                ///// FLUSH
                /////
            case HandInfo.FLUSH:
                // TODO: look for full house possibility - maybe using hand
                // TODO: strength, or if board has a pair
                if (inHole)
                {
                    // flopped a flush
                    if (isFlop())
                    {
                        // nut flush
                        if (HandInfo.isNutFlush(_hole, _comm, nMajorSuit, 1))
                        {
                            return doRaiseCheckRaiseBet(5, 25, "Nut Flush");
                        }
                        // close to nut flush (2nd or 3rd best)
                        else if (HandInfo.isNutFlush(_hole, _comm, nMajorSuit, 3))
                        {
                            // TODO: when to fold? what other criteria? pair on board?
                            if (isReRaised())
                            {
                                if (nRandom < 50)
                                    return _foldPotOdds("re-raised 2nd nut flush");
                                else
                                    return PlayerAction.call().reason("V1:re-raised 2nd nut flush");
                            }
                            return doRaiseCheckRaiseBet(5, 0, "2nd Nut Flush");
                        }
                        // regular flush
                        else
                        {
                            if (_nToCall == 0)
                            {
                                return _bet("flush");
                            }
                            else
                            {
                                // TODO: when to fold? what other criteria?
                                if (isReRaised())
                                {
                                    if (nRandom < 50)
                                        return PlayerAction.fold().reason("V1:re-raised 2nd nut flush");
                                    else
                                        return PlayerAction.call().reason("V1:re-raised 2nd nut flush");
                                }
                                return _raise("flush");
                            }
                        }
                    }
                    else
                        if (isTurn() || isRiver())
                        {
                            // nut flush
                            if (HandInfo.isNutFlush(_hole, _comm, nMajorSuit, 1))
                            {
                                return doRaiseCheckRaiseBet(60, 5, "Nut Flush");
                            }
                            // close to nut flush (2nd or 3rd best)
                            else if (HandInfo.isNutFlush(_hole, _comm, nMajorSuit, 3))
                            {
                                // TODO: when to fold? what other criteria?
                                if (isReRaised())
                                {
                                    if (nRandom < 50)
                                        return PlayerAction.fold().reason("V1:re-raised 2nd nut flush");
                                    else
                                        return PlayerAction.call().reason("V1:re-raised 2nd nut flush");
                                }
                                return doRaiseCheckRaiseBet(60, 0, "2nd Nut Flush");
                            }
                            // regular flush
                            else
                            {
                                if (_nToCall == 0)
                                {
                                    return _bet("flush");
                                }
                                else
                                {
                                    // TODO: when to fold? what other criteria?
                                    if (isReRaised())
                                    {
                                        if (nRandom < 50)
                                            return PlayerAction.fold().reason("V1:re-raised 2nd nut flush");
                                        else
                                            return PlayerAction.call().reason("V1:re-raised 2nd nut flush");
                                    }
                                    return _raise("flush");
                                }
                            }
                        }
                }
                // flush on board
                else
                {
                    // TODO: bluff
                    if (_nToCall == 0)
                    {
                        return PlayerAction.check().reason("V1:Flush, no hole");
                    }
                    else
                    {
                        // TODO: see if possible whether
                        // TODO: bettor's actions indicate a good hand
                        // TODO: maybe we should call sometimes/ raise too?
                        return _foldPotOdds("Flush, no hole");
                    }
                }
                break;

                /////
                ///// STRAIGHT
                /////
            case HandInfo.STRAIGHT:
                if (inHole)
                {
                    // 3 flush cards out there
                    if (bThreeFlush)
                    {
                        return doBiggerHandPossibleBets("str, pos flush", 50, 15, 20, HandInfo.STRAIGHT);
                    }
                    else
                    {
                        if (isRaised())
                        {
                            if (bBoardPair) return _foldPotOdds("re-raised, paired board");
                        }
                        // don't check raise, allowing someone to draw out
                        return doRaiseCheckRaiseBet(isFlop() || bFlushDraw ? 0 : 60, 2, "Straight");
                    }
                }
                // straight on board
                else
                {
                    if (_nToCall == 0)
                    {
                        // bluff we have higher straight
                        if (_nLastRoundBet == 0 && nRandom < 25)
                        {
                            return _bet("Straight, no hole");
                        }
                        return PlayerAction.check().reason("V1:Straight, no hole");
                    }
                    else
                    {
                        // TODO: see if possible whether
                        // TODO: bettor's actions indicate a good hand
                        // TODO: maybe we should call sometimes/ raise too?
                        return _foldPotOdds("Straight, no hole");
                    }
                }

                /////
                ///// TRIPS
                /////
            case HandInfo.TRIPS:

                if (inHole)
                {
                    // "hidden"  trips
                    if (_hole.isPair())
                    {
                        // board can't be paired here otherwise we have a full house
                        if (bThreeFlush || nNumOppStraights > 0)
                        {
                            return doBiggerHandPossibleBets("trips(hole), pos flush/str", 60, 25, 30, HandInfo.TRIPS);
                        }
                        else
                        {
                            // top trip
                            if (isTopMatch())
                            {
                                return doRaiseCheckRaiseBet(isFlop() ? 5 : 60, 5, "Top Trips");
                            }
                            else
                            {
                                return doRaiseCheckRaiseBet(isFlop() ? 0 : 20, 0, "Middle/low Trips");
                            }
                        }
                    }
                    // pair on board, with one matching in hand
                    else
                    {
                        // TODO: take into account kicker
                        if (bThreeFlush || nNumOppStraights > 0)
                        {
                            return doBiggerHandPossibleBets("trips(2 board), pos flush/str", 60, 25, 30, HandInfo.TRIPS);
                        }
                        else
                        {
                            if (isReRaised())
                            {
                                if (nRandom < 50)
                                    return PlayerAction.call().reason("V1:possible full house reraised");
                                else
                                    return PlayerAction.fold().reason("V1:possible full house reraised");
                            }

                            // top trip
                            if (isTopMatch())
                            {
                                return doRaiseCheckRaiseBet(isFlop() ? 5 : 60, 0, "Top Trips,Pair on board");
                            }
                            else
                            {
                                return doRaiseCheckRaiseBet(isFlop() ? 0 : 20, 0, "Middle/low Trips,Pair on board");
                            }
                        }
                    }
                }
                // trips on board
                else
                {
                    // TODO: analyze kickers, if AKQJ on board (not part of trips), then
                    // TODO: we should analyze betting of previous rounds to determine if
                    // TODO: it is likely someone else has one of those
                    if (_best[1] == Card.ACE && _hole.isInHand(Card.ACE))
                    {
                        // TODO: if not likely someone holding a pair or full house, raise
                        return PlayerAction.call().reason("V1:trips, ace kicker");
                    }
                    else
                    {
                        if (_nToCall == 0)
                        {
                            return PlayerAction.check().reason("V1:trips, non ace kicker");
                        }
                        else
                        {
                            return _foldPotOdds("trips, non ace kicker");
                        }
                    }
                }

                /////
                ///// TWO PAIR
                /////
            case HandInfo.TWO_PAIR:
                if (inHole)
                {
                    if (bThreeFlush || nNumOppStraights > 0 || bBoardPair)
                    {
                        return doBiggerHandPossibleBets("2 pair, pos flush/str/board paired", 60, 25, 35, HandInfo.TWO_PAIR);
                    }
                    else
                    {
                        return doRaiseCheckRaiseBet(isFlop() ? 0 : 20, 0, "2 pair, no bigger pos");
                    }
                }
                else
                {
                    if (_nToCall == 0)
                    {
                        if (_nNumAfter == 0 && nRandom < 50)
                        {
                            return _bet("two pair on board bluff");
                        }
                        return PlayerAction.check().reason("V1:two pair on board");
                    }
                    else
                    {
                        if (isRaised() || bFlushDraw || nNumOppStraights > 0)
                        {
                            return _foldPotOdds("raised two pair on board");
                        }
                        else
                        {
                            if (_nNumAfter == 0 || _hole.isInHand(Card.ACE))
                            {
                                return PlayerAction.call().reason("V1:two pair on board");
                            }
                            else
                            {
                                return _foldPotOdds("two pair on board");
                            }
                        }
                    }
                }

                /////
                ///// PAIR
                /////
            case HandInfo.PAIR:
                if (inHole)
                {
                    if (_hole.isPair())
                    {
                        // overpair?
                        if (_hole.getCard(0).getRank() > _comm.getCard(_comm.size() - 1).getRank())
                        {
                            if (bThreeFlush || nNumOppStraights > 0)
                            {
                                return doBiggerHandPossibleBets("overpair, pos flush/str", 70, 30, 35, HandInfo.PAIR);
                            }
                            else
                            {
                                if (isReRaised())
                                {
                                    return _foldPotOdds("re-raised overpair");
                                }

                                if (_nToCall == 0)
                                {
                                    return _bet("overpair");
                                }
                                else
                                {
                                    return _raise("overpair");
                                }
                            }
                        }
                        // TODO: not overpair
                    }
                    else
                    {
                        // top pair?
                        if (isTopMatch())
                        {
                            if (bThreeFlush || nNumOppStraights > 0)
                            {
                                return doBiggerHandPossibleBets("top pair, pos flush/str", 70, 30, 35, HandInfo.PAIR);
                            }
                            else
                            {
                                if (_nToCall == 0)
                                {
                                    return _bet("top pair");
                                }
                                else
                                {
                                    if (isGoodKicker(nType))
                                    {
                                        return _raise("toppair, good kicker");
                                    }
                                    else
                                    {
                                        return PlayerAction.call().reason("V1:toppair");
                                    }
                                }
                            }
                        }
                        // middle other pair
                        else
                        {
                            if (_nToCall == 0 && _nNumAfter == 0)
                            {
                                return _bet("pair, none after");
                            }
                            else if (_nToCall == 0 && _nNumAfter == 1)
                            {
                                if (nRandom < 50)
                                {
                                    return _bet("pair, one after");
                                }
                            }
                            else if (_nToCall > 0)
                            {
                                if (!isRaised() && _nNumAfter == 0)
                                {
                                    if (nRandom < 50)
                                    {
                                        return PlayerAction.call().reason("V1:pair, one after");
                                    }
                                }
                            }

                        }
                    }
                }
                else // pair on board
                {
                }
                action = doPotOdds("pair");
                if (action != null) return action;
                break;

                /////
                ///// HIGH CARD
                /////
            case HandInfo.HIGH_CARD:
                // proceed with caution
                // overcards
                if (_hole.isInHand(Card.ACE))
                {
                    if (_nNumAfter == 0 && _nToCall == 0)
                    {
                        return _bet("high card, last to act, no bet");
                    }
                    else if (_nNumAfter == 1 && _nNumBefore == 0)
                    {
                        // TODO: look at last round betting - if no raises, bet
                    }

                }
                action = doPotOdds("high");
                if (action != null) return action;
                break;

            default:
                ApplicationError.assertTrue(false, "Type not handled: " + nType);
        }

        /// generic hand strength
        boolean better = (bThreeFlush || nNumOppStraights > 0 || bBoardPair);

        if (dStrength > 90 && !better)
        {
            return _raise("90+ strength");
        }
        else if (dStrength > 75)
        {
            if (_nToCall == 0)
            {
                return _bet("75+ strength");
            }
            else
            {
                return PlayerAction.call().reason("V1:75+ strength");
            }
        }
        else if (_nToCall == 0)
        {

            /// checked around previous hand
            if (_nLastRoundBet == 0)
            {
                if (_nNumAfter == 0)
                {
                    if (!better)
                    {
                        return _bet("check around, last to act");
                    }
                    else if (getBluffFactor() > 85)
                    {
                        return _bet("check around, last to act bet");
                    }
                }
                else
                {
                    if (getBluffFactor() > 50)
                    {
                        return _bet("check around bluff");
                    }
                }
            }
            else if (_dImprove > 30)
            {
                return _bet("improve odds good (semi-bluff)");
            }
            else
            {
                if (_nNumAfter == 0 && _hhand.getNumWithCards() <= 3 &&
                        _dImprove > 17)
                {
                    return _bet("none after, improve odds decent, <3 left");
                }
            }
            return PlayerAction.check().reason("V1:no bet");
        }

        return _foldPotOdds("default");
    }

    /////
    ///// HELPER
    /////

    /**
     * Do fold unless pot odds say otherwise
     */
    private PlayerAction _foldPotOdds(String sDebug)
    {
        PlayerAction action = doPotOdds(sDebug);
        if (action != null) return action;

        int nRandom = DiceRoller.rollDieInt(100);
        if (seatsOccupied_ > 4 && nRandom > getTightFactor())
        {
            int nThresh = 10;
            if (nRandom < nThresh)
            {
                return _raise("loose potodds " + sDebug + " [nTight: " + getTightFactor() + " " + nRandom + "r]");
            }
            else if (nRandom < (nThresh + 15))
            {
                return PlayerAction.call().reason("V1:loose potodds " + sDebug + " [nTight: " + getTightFactor() + " " + nRandom + "r]");
            }
        }

        return PlayerAction.fold().reason("V1:" + sDebug);
    }

    /**
     * Do fold unless raiser/bettor is very loose
     */
    private PlayerAction _foldLooseCheck(String sDebug)
    {
        PokerPlayer player = getPokerPlayer();

        int nLoose = 0;
        int nThreshold = 0;
        if (!isEasy())
        {
            if (_raiser != null)
            {
                nLoose = getRaiseFreq(_raiser);
                nThreshold = 15;
            }
            else if (_bettor != null)
            {
                nLoose = getBetFreq(_bettor);
                nThreshold = 40;
            }

            // more raising/betting when shorthanded
            if (seatsOccupied_ <= 3) nThreshold += 20;
        }

        // if the loose level is greater than the threshold,
        // re-raise or call with a half way decent hand
        if (nLoose > nThreshold &&
                (_hole.isSuited() || _hole.hasConnector(1, 7) || _hole.isInHand(Card.ACE) || _hole.isPair()))
        {
            int nRandom = DiceRoller.rollDieInt(100);
            if (getBluffFactor() > 75 && nRandom < 15)
            {
                return _raise("loose check - " + sDebug);
            }
            else
            {
                return PlayerAction.call().reason("V1:loose check - " + sDebug);
            }
        }

        if (isEasy())
        {
            if (_hhand.getNumCallers() == 0 &&
                    (_hole.isSuited() || _hole.hasConnector(1, 6) ||
                    _hole.isPair() || _hole.isInHand(Card.ACE)))
            {
                int nRandom = DiceRoller.rollDieInt(100);
                if (player.isEarly() && nRandom <= 15 ||
                        player.isMiddle() && nRandom <= 35 ||
                        player.isLate() && nRandom <= 60 ||
                        player.isBlind() && nRandom <= 80)
                {
                    return PlayerAction.call().reason("V1:easy - more hands");
                }
            }
        }

        // if pot odds are high, call
        if (_dImprove >= _potOdds)
            return PlayerAction.call().reason("V1:" + _dImprove + " > " + _potOdds + " pot odds " + sDebug);

        return PlayerAction.fold().reason("V1:" + sDebug);
    }

    /**
     * If pots odds dictate calling, do so
     */
    private PlayerAction doPotOdds(String sDebug)
    {
        if (_nToCall == 0) return null;
        float dFudgeFactor = 0.0f;
        if (_betPercOfStack < 10) dFudgeFactor = 3.0f;
        if ((_dImprove + dFudgeFactor) >= _potOdds)
        {
            return PlayerAction.call().reason("V1:pot odds: " + sDebug);
        }

        return null;
    }

    /**
     * Helper to do a raise (if already money in the pot), or to do a
     * bet/checkraise/allin bet if no bet yet in the pot
     */
    private PlayerAction doRaiseCheckRaiseBet(int nCheckRaisePercent, int nAllInPercent, String sDebug)
    {
        // random number from 1-100
        int nRandom = DiceRoller.rollDieInt(100);

        if (_nToCall > 0)
        {
            // raise 1-3 times current bet
            nRandom %= 3;
            nRandom++;
            return _raise(_nToCall * nRandom, sDebug);
        }
        else
        {
            // check raise sometimes (if people to act after us)
            if (isHard() && _nNumAfter > 0 && nRandom <= nCheckRaisePercent)
            {
                bIntendCheckRaise_ = true;
                return PlayerAction.check().reason("V1:" + sDebug);
            }
            else if (!isEasy() && nRandom > nCheckRaisePercent && nRandom < (nCheckRaisePercent + nAllInPercent))
            {
                betAmount_ = Integer.MAX_VALUE;
                return PlayerAction.bet().reason("V1:" + sDebug);
            }
            else
            {
                return _bet(sDebug);
            }
        }
    }

    /**
     * Helper to do fold/call/bet/raise logic when there are better hands possible
     */
    private PlayerAction doBiggerHandPossibleBets(String sDebug, int nReRaiseFoldPerc,
                                                int nRaiseFoldPerc, int nFoldPerc, int nType)
    {

        // if many players, odds are greater that a better hand will
        // have been made
        if (seatsOccupied_ >= 4)
        {
            nReRaiseFoldPerc += 10;
            nRaiseFoldPerc += 10;
            nFoldPerc += 10;
        }
        // likewise, with less players, odds are worse
        else if (seatsOccupied_ <= 2)
        {
            nReRaiseFoldPerc -= 10;
            nRaiseFoldPerc -= 10;
            nFoldPerc -= 10;
        }

        // random number from 1-100
        int nRandom = DiceRoller.rollDieInt(100);
        int nThresh;

        // no bet yet
        if (_nToCall == 0)
        {
            // we are first to act
            if (_nNumBefore == 0)
            {
                if (_nLastRoundBet == 0 || nRandom < 25 || getBluffFactor() > 75)
                {
                    return _bet(sDebug + ":" + "no bet last round");
                }
                return PlayerAction.check().reason("V1:" + sDebug + ":" + "no bet, 1st to act");
            }
            else
            {
                nThresh = 45;
                if (_nLastRoundBet == 0 || nRandom < nThresh || getBluffFactor() > 50)
                {
                    return _bet(sDebug + ":" + "no bet");
                }
                else
                {
                    return PlayerAction.check().reason("V1:" + sDebug + ":" + "no bet");
                }
            }
        }
        else
        {

            // flop, player raising limped into pot
            if (isFlop() && (_raiserPre == HandAction.ACTION_CALL ||
                    _raiserPre == HandAction.ACTION_CHECK))
            {
                sDebug += " [pre limp]";
                nReRaiseFoldPerc += 20;
                nRaiseFoldPerc += 20;
            }
            else
                if (isTurn() && (_raiserFlop == HandAction.ACTION_CALL ||
                        _raiserFlop == HandAction.ACTION_CHECK))
                {
                    sDebug += " [flop limp]";
                    nReRaiseFoldPerc += 25;
                    nRaiseFoldPerc += 25;
                }
                else
                    if (isRiver() && (_raiserTurn == HandAction.ACTION_CALL ||
                            _raiserTurn == HandAction.ACTION_CHECK))
                    {
                        sDebug += " [turn limp]";
                        nReRaiseFoldPerc += 30;
                        nRaiseFoldPerc += 30;
                    }

            if (isFlop() && (_raiserPre == HandAction.ACTION_RAISE))
            {
                if (nType >= HandInfo.TRIPS)
                {
                    sDebug += " [flop trips+]";
                    nReRaiseFoldPerc -= 20;
                    nRaiseFoldPerc -= 20;
                }
            }

            // if we were re-raised, probably facing a better hand
            if (isReRaised())
            {
                if (nRandom > nReRaiseFoldPerc)
                    return PlayerAction.call().reason("V1:" + sDebug + ":" + "rereraised");
                else
                    return _foldPotOdds(sDebug + ":" + "rereraised");
            }

            // if we are facing a raise, probably facing a better hand too
            if (isRaised())
            {
                if (nRandom > nRaiseFoldPerc)
                    return PlayerAction.call().reason("V1:" + sDebug + ":" + "reraise");
                else
                    return _foldPotOdds(sDebug + ":" + "raised");
            }

            // flop, player raising limped into pot
            if (isFlop() && (_bettorPre == HandAction.ACTION_CALL ||
                    _bettorPre == HandAction.ACTION_CHECK))
            {
                sDebug += " [pre limp]";
                nFoldPerc += 20;
            }
            else
                if (isTurn() && (_bettorFlop == HandAction.ACTION_CALL ||
                        _bettorFlop == HandAction.ACTION_CHECK))
                {
                    sDebug += " [flop limp]";
                    nFoldPerc += 25;
                }
                else
                    if (isRiver() && (_bettorTurn == HandAction.ACTION_CALL ||
                            _bettorTurn == HandAction.ACTION_CHECK))
                    {
                        sDebug += " [turn limp]";
                        nFoldPerc += 30;
                    }

            if (isFlop() && (_bettorPre == HandAction.ACTION_RAISE))
            {
                if (nType >= HandInfo.TRIPS)
                {
                    sDebug += " [flop trips+]";
                    nFoldPerc -= 20;
                }
            }

            // just call
            if (nRandom > nFoldPerc)
            {
                return PlayerAction.call().reason("V1:" + sDebug);
            }
            // sometimes fold
            else
            {
                return _foldPotOdds(sDebug);
            }
        }
    }

    /**
     * Is top pair (or one/two pair) or trip top?
     */
    private boolean isTopMatch()
    {
        return (_best[0] == _comm.getCard(_comm.size() - 1).getRank());
    }

    /**
     * Is good kicker?
     */
    private boolean isGoodKicker(int nType)
    {
        int kick = -1;
        switch (nType)
        {
            case HandInfo.ROYAL_FLUSH:
            case HandInfo.STRAIGHT_FLUSH:
            case HandInfo.FULL_HOUSE:
            case HandInfo.FLUSH:
            case HandInfo.STRAIGHT:
                return false;

            case HandInfo.QUADS:
                kick = getKicker(1, 1);
                break;

            case HandInfo.TRIPS:
                kick = getKicker(1, 2);
                break;

            case HandInfo.TWO_PAIR:
                kick = getKicker(2, 1);
                break;

            case HandInfo.PAIR:
                kick = getKicker(1, 3);
                break;

            case HandInfo.HIGH_CARD:
                kick = getKicker(1, 4);
                break;
        }

        //if (TESTING(EngineConstants.TESTING_AI_DEBUG)) logger.debug("KICKER ***** " + player_.getName() + " kicker is " + kick);

        if (kick == -1) return false;
        if (kick == Card.ACE) return true;
        if (kick == Card.KING && _comm.isInHand(Card.ACE)) return true;

        return false;
    }

    /**
     * Get highest kicker not on board
     */
    private int getKicker(int nStart, int nNum)
    {
        int kick;
        for (int i = nStart; i < nStart + nNum; i++)
        {
            kick = _best[i];
            if (_hole.isInHand(kick))
            {
                return kick;
            }
        }
        return -1;
    }

    /**
     *  easy players
     */
    private boolean isEasy()
    {
        return _skill == AI_EASY;
    }

    /**
     *  med players
     */
    private boolean isMedium()
    {
        return _skill == AI_MEDIUM;
    }

    /**
     *  hard players
     */
    private boolean isHard()
    {
        return _skill == AI_HARD;
    }

    /**
     * Were we re-raised?
     */
    private boolean isReRaised()
    {
        if (_nLast == HandAction.ACTION_RAISE)
        {
            return true;
        }

        return false;
    }

    /**
     * Were we raised (true if our last action was
     * a call, bet or raise)
     */
    private boolean isRaised()
    {
        if (_nLast == HandAction.ACTION_CALL ||
                _nLast == HandAction.ACTION_BET ||
                _nLast == HandAction.ACTION_RAISE)
        {
            return true;
        }

        return false;
    }


    /**
     * get frequency of raise
     */
    private int getRaiseFreq(PokerPlayer p)
    {
        if (p == null) return 0;
        return p.getProfileInitCheck().getFrequency(getBettingRound(), HandAction.ACTION_RAISE);
    }

    /**
     * get frequency of bet
     */
    private int getBetFreq(PokerPlayer p)
    {
        if (p == null) return 0;
        return p.getProfileInitCheck().getFrequency(getBettingRound(), HandAction.ACTION_BET);
    }

    /**
     * return new bet hand action
     */
    private PlayerAction _bet(int nAmount, String sDebug)
    {
        PokerPlayer player = getPokerPlayer();

        int nBiggest = _hhand.getBiggestBetRaise();
        if (nAmount < nBiggest) nAmount = nBiggest;
        if ((float) nAmount > (player.getChipCount() * 0.95d))
        {
            nAmount = player.getChipCount();
            sDebug += " [>95%]";
        }
        betAmount_ = nAmount;
        return PlayerAction.bet().reason("V1:" + sDebug);
    }

    /**
     * return new raise hand action
     */
    private PlayerAction _raise(int nAmount, String sDebug)
    {
        PokerPlayer player = getPokerPlayer();

        // avoid several re-raises in a row
        // unless 3 players or less - go all-in
        if (isReRaised())
        {
            if (!isEasy() && seatsOccupied_ <= 3)
            {
                betAmount_ = Integer.MAX_VALUE;
                return PlayerAction.bet().reason("V1:" + sDebug + " [raise-under3]");
            }
            return PlayerAction.call().reason("V1:" +sDebug + " [force-call]");
        }
        int nBiggest = _hhand.getBiggestBetRaise();
        if ((float) nAmount > (player.getChipCount() * 0.95d))
        {
            nAmount = player.getChipCount();
            sDebug += " [>95%]";
        }
        if (nAmount < nBiggest) nAmount = nBiggest;
        betAmount_ = nAmount + _nToCall;
        return PlayerAction.raise().reason("V1:" + sDebug);
    }

    /**
     * return new raise hand action, using default amount
     */
    private PlayerAction _raise(String sDebug)
    {
        return _raise(getRaise(_hhand.getMinBet()), sDebug);
    }
    
    /**
     * get raise
     */
    public static int getRaise(int nBig)
    {
        int nRandom = DiceRoller.rollDieInt(100);
        int nRaise;

        // raise 3-4 times big blind
        // TJ 121 -  TODO: use size of pot
        // TODO: all in
        if (nRandom < 75)
            nRaise = nBig * 3;
        else
            nRaise = nBig * 4;
        
        return nRaise;
    }

    /**
     * return new bet hand action, using random amount from 1th to 4/4th of pot
     */
    private PlayerAction _bet(String sDebug)
    {
        int nRandom = DiceRoller.rollDieInt(10);
        if (nRandom <= 2)
            nRandom = 1;
        else if (nRandom <= 5)
            nRandom = 2;
        else if (nRandom <= 8)
            nRandom = 3;
        else
            nRandom = 4;
        int nBet = (int) ((float) _hhand.getTotalPotChipCount() * (0.0d + nRandom / 4.0d));
        int nMin = _hhand.getMinBet();
        int nOdd = nBet % nMin;
        if (nOdd > 0) nBet += (nMin - nOdd);
        if (nBet < nMin) nBet = nMin;

        return _bet(nBet, sDebug);
    }

    //////
    ////// Marshalling code
    //////

    /**
     * Get our own stuff
     */
    public void demarshal(MsgState state, TokenizedList list)
    {
        super.demarshal(state, list);

        nRebuyPropensity_ = list.removeIntToken();
        nAddonPropensity_ = list.removeIntToken();
        nTightFactor_ = list.removeIntToken();
        nBluffFactor_ = list.removeIntToken();
    }

    /**
     * Add our own stuff
     */
    public void marshal(MsgState state, TokenizedList list)
    {
        super.marshal(state, list);

        list.addToken(nRebuyPropensity_);
        list.addToken(nAddonPropensity_);
        list.addToken(nTightFactor_);
        list.addToken(nBluffFactor_);
    }

    public int getBetAmount()
    {
        return betAmount_;
    }
}
