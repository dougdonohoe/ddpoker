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
package com.donohoedigital.games.poker.ai;

import com.ddpoker.holdem.PlayerAction;
import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.DebugConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import static com.donohoedigital.config.DebugConfig.TESTING;

public class RuleEngine implements AIConstants
{
    public boolean USE_CONFIDENCE = false;
    public boolean NEWCODE = true;

    static Logger logger = LogManager.getLogger(RuleEngine.class);

    private static ArrayList outcomeNames_ = new ArrayList();
    private static ArrayList factorNames_ = new ArrayList();
    private static ArrayList curveNames_ = new ArrayList();

    public static final int OUTCOME_NONE = -1;
    public static final int OUTCOME_FOLD = 0;
    public static final int OUTCOME_CHECK = 1;
    public static final int OUTCOME_LIMP = 2;
    public static final int OUTCOME_STEAL = 3;
    public static final int OUTCOME_OPEN_POT = 4;
    public static final int OUTCOME_CALL = 5;
    public static final int OUTCOME_RAISE = 6;
    public static final int OUTCOME_SEMI_BLUFF = 7;
    public static final int OUTCOME_TRAP = 8;
    public static final int OUTCOME_SLOW_PLAY = 9;
    public static final int OUTCOME_CHECK_RAISE = 10;
    public static final int OUTCOME_BET = 11;
    public static final int OUTCOME_ALL_IN = 12;
    public static final int OUTCOME_CONTINUATION_BET = 13;
    public static final int OUTCOME_BLUFF = 14;

    public static final int FACTOR_DEFAULT = 0;
    public static final int FACTOR_HAND_SELECTION = 1;
    public static final int FACTOR_BLIND_STEALING = 2;
    public static final int FACTOR_LEFT_TO_ACT = 3;
//    public static final int FACTOR_BLINDS_SMALL = 4;
//    public static final int FACTOR_BIG_BLIND_SHORT_STACKED = 5;
//    public static final int FACTOR_POT_RAISED = 6;
//    public static final int FACTOR_EARLY_RAISER = 7;
    public static final int FACTOR_POSITION = 8;
    public static final int FACTOR_STACK_SIZE = 9;
//    public static final int FACTOR_LIMP_TENDENCY = 10;
    public static final int FACTOR_POT_ODDS = 11;
    public static final int FACTOR_RAW_HAND_STRENGTH = 12;
    public static final int FACTOR_BIASED_HAND_STRENGTH = 13;
    public static final int FACTOR_HAND_POTENTIAL = 14;
    public static final int FACTOR_BET_TO_CALL = 15;
//    public static final int FACTOR_POT_RERAISED = 16;
//    public static final int FACTOR_TIGHTNESS = 17;
    public static final int FACTOR_AGGRESSION = 18;
//    public static final int FACTOR_TABLE_AGGRESSION = 19;
    public static final int FACTOR_STEAL_SUSPECTED = 20;
//    public static final int FACTOR_LAST_TO_ACT = 21;
    public static final int FACTOR_STRAIGHT_DRAW = 22;
    public static final int FACTOR_FLUSH_DRAW = 23;
//    public static final int FACTOR_PLAYERS_DEALT = 24;
    public static final int FACTOR_PROBE_BET = 25;
    public static final int FACTOR_PLAYERS_LEFT = 26;
    public static final int FACTOR_FIRST_PRE_FLOP_RAISER = 27;
    public static final int FACTOR_LAST_PRE_FLOP_RAISER = 28;
    public static final int FACTOR_ONLY_PRE_FLOP_RAISER = 29;
    public static final int FACTOR_PRE_FLOP_POSITION = 30;
    public static final int FACTOR_RAISER_STACK_SIZE = 31;
    public static final int FACTOR_IMPLIED_ODDS = 32;
    public static final int FACTOR_RAISER_POSITION = 33;
    public static final int FACTOR_RERAISER_POSITION = 34;
    public static final int FACTOR_BOREDOM = 35;
    public static final int FACTOR_STEAM = 36;
    public static final int FACTOR_FIRST_ACTION = 37;
    public static final int FACTOR_OUTDRAW_RISK = 38;
    public static final int FACTOR_CHECKED_AROUND = 39;
    public static final int FACTOR_BLINDS_CLOSING = 40;
    public static final int FACTOR_OPPONENT_BET_FREQUENCY = 41;
    public static final int FACTOR_OPPONENT_RAISE_FREQUENCY = 42;
    public static final int FACTOR_OPPONENT_OVERBET_FREQUENCY = 43;
    public static final int FACTOR_OPPONENT_BET_FOLD_FREQUENCY = 44;
    public static final int FACTOR_STEAL_POTENTIAL = 45;
    public static final int CURVE_LINEAR = 1;
    public static final int CURVE_SQUARE = 2;
    public static final int CURVE_CUBE = 3;

    private V2Player ai_;
    private float score_[];
    private boolean eligible_[];
    private float weights_[];
    private OutcomeAdjustment adjustments_[][];

    private int strongestOutcome_;

    private BetRange betRange_ = null;

    private boolean probeBet_;
    private int nPlayersRemaining_;
    private int nPlayersAfter_;
    private int nAmountToCall_;
    private int nLastAction_;
    private boolean bInPosition_;
    private int round_;
    private HoldemHand hhand_;
    private PokerPlayer self_;
    private int seat_;
    private boolean bAllIn_;
    private boolean bPotCommitted_;
    private boolean bCardsToCome_;
    private float apparentStrength_;
    private float probableStrength_;
    private float drawStrength_;
    private OpponentModel selfModel_;
    private PocketWeights pWeights_;

    AIOutcome outcome_ = null;

    static
    {
        outcomeNames_.add("fold");
        outcomeNames_.add("check");
        outcomeNames_.add("limp");
        outcomeNames_.add("steal");
        outcomeNames_.add("openpot");
        outcomeNames_.add("call");
        outcomeNames_.add("raisevalue");
        outcomeNames_.add("semibluff");
        outcomeNames_.add("trap");
        outcomeNames_.add("slowplay");
        outcomeNames_.add("checkraise");
        outcomeNames_.add("bet");
        outcomeNames_.add("allin");
        outcomeNames_.add("continuationbet");
        outcomeNames_.add("bluff");

        factorNames_.add("default");
        factorNames_.add("handselection");
        factorNames_.add("blindstealing");
        factorNames_.add("lefttoact");
        factorNames_.add("blindssmall");
        factorNames_.add("bbshortstack");
        factorNames_.add("potraised");
        factorNames_.add("earlyraiser");
        factorNames_.add("position");
        factorNames_.add("stacksize");
        factorNames_.add("limptendency");
        factorNames_.add("potodds");
        factorNames_.add("rawhandstrength");
        factorNames_.add("biasedhandstrength");
        factorNames_.add("handpotential");
        factorNames_.add("bettocall");
        factorNames_.add("potreraised");
        factorNames_.add("tightness");
        factorNames_.add("aggression");
        factorNames_.add("tableaggression");
        factorNames_.add("stealsuspected");
        factorNames_.add("lasttoact");
        factorNames_.add("straightdraw");
        factorNames_.add("flushdraw");
        factorNames_.add("playersdealt");
        factorNames_.add("probebet");
        factorNames_.add("playersleft");
        factorNames_.add("firstpreflopraiser");
        factorNames_.add("lastpreflopraiser");
        factorNames_.add("onlypreflopraiser");
        factorNames_.add("preflopposition");
        factorNames_.add("raiserstacksize");
        factorNames_.add("impliedodds");
        factorNames_.add("raiserposition");
        factorNames_.add("reraiserposition");
        factorNames_.add("boredom");
        factorNames_.add("steam");
        factorNames_.add("firstaction");
        factorNames_.add("outdrawrisk");
        factorNames_.add("checkedaround");
        factorNames_.add("blindsclosing");
        factorNames_.add("opponentbetfrequency");
        factorNames_.add("opponentraisefrequency");
        factorNames_.add("opponentoverbetfrequency");
        factorNames_.add("opponentbetfoldfrequency");
        factorNames_.add("stealpotential");

        curveNames_.add("none");
        curveNames_.add("linear");
        curveNames_.add("square");
        curveNames_.add("cube");
    }

    public void init(V2Player ai)
    {
        for (int i = outcomeNames_.size()-1; i >= 0; --i)
        {
            score_[i] = 0.0f;
            weights_[i] = 0.0f;
            eligible_[i] = false;
            for (int j = factorNames_.size()-1; j >= 0; --j)
            {
                adjustments_[i][j] = null;
            }
        }

        probeBet_ = false;

        ai_ = ai;
        self_ = ai.getPokerPlayer();
        seat_ = self_.getSeat();
        hhand_ = self_.getHoldemHand();

        if (hhand_ != null)
        {
            round_ = hhand_.getRound();

            nPlayersRemaining_ = hhand_.getNumWithCards();
            nPlayersAfter_ = ai_.getNumAfter();
            nAmountToCall_ = ai_.getAmountToCall();
            nLastAction_ = hhand_.getLastActionThisRound(self_);

            bInPosition_ = (nPlayersAfter_ == 0);
            bAllIn_ = (self_.getChipCount() == 0);
            bPotCommitted_ = (self_.getChipCount() <= self_.getChipCountAtStart() / 2);

            bCardsToCome_ = (round_ != HoldemHand.ROUND_RIVER);

            selfModel_ = self_.getOpponentModel();

            pWeights_ = PocketWeights.getInstance(hhand_);

            if (hhand_.getRound() >= HoldemHand.ROUND_FLOP)
            {
                apparentStrength_ = pWeights_.getApparentStrength(seat_);
                probableStrength_ = pWeights_.getBiasedRawHandStrength(seat_);
                drawStrength_ = (round_ == HoldemHand.ROUND_FLOP || round_ == HoldemHand.ROUND_TURN) ?
                                pWeights_.getBiasedEffectiveHandStrength(seat_) : probableStrength_;
            }
        }

        outcome_ = null;

        // default bet range to prevent strange bug seen recently
        betRange_ = new BetRange(BetRange.BIG_BLIND, 2.0f, 4.0f);
    }

    public void setEligible(int outcome, boolean eligible)
    {
        eligible_[outcome] = eligible;
    }

    public boolean isEligible(int outcome)
    {
        return eligible_[outcome];
    }

    public RuleEngine()
    {
        score_ = new float[outcomeNames_.size()];
        weights_ = new float[outcomeNames_.size()];
        eligible_ = new boolean[outcomeNames_.size()];
        adjustments_ = new OutcomeAdjustment[outcomeNames_.size()][factorNames_.size()];
    }

    public void execute(V2Player player)
    {
        init(player);

        player.initDebug();

        strongestOutcome_ = eligible_[OUTCOME_CHECK] ? OUTCOME_CHECK : OUTCOME_FOLD;

        switch (player.getRound())
        {
            case HoldemHand.ROUND_NONE:
                return;
            case HoldemHand.ROUND_PRE_FLOP:
                determineEligibleOutcomes(player);
                executePreFlop(player);
                setPreFlopBetRange(player);
                break;
            case HoldemHand.ROUND_FLOP:
            case HoldemHand.ROUND_TURN:
            case HoldemHand.ROUND_RIVER:
                if (NEWCODE) execPostFlop();
                else
                {
                    determineEligibleOutcomes(player);
                    executeFlopTurn(player);
                }
                break;
//            case HoldemHand.ROUND_RIVER:
//                executeRiver(player);
//                break;
        }

        if (TESTING(PokerConstants.TESTING_LOG_AI))
        {
            logResults(player);
        }
    }

    private void execPostFlop()
    {
        // for now limit to heads up play where opponent has bet

        if (nPlayersRemaining_ != 2)
        {
            executeFlopTurn(ai_);
            return;
        }

        HandInfoFast info = new HandInfoFast();
        info.getScore(self_.getHand(), hhand_.getCommunity());


        ArrayList players = new ArrayList();

        hhand_.getPlayersLeft(players, self_);

        PokerPlayer opponent = (PokerPlayer)players.get(0);

        int opponentSeat = opponent.getSeat();

        OpponentModel opponentModel = opponent.getOpponentModel();

        boolean bOpponentAllIn = (opponent.getChipCount() == 0);
        boolean bOpponentPotCommitted = (opponent.getChipCount() <= opponent.getChipCountAtStart() / 2);

        float opponentProbableStrength = pWeights_.getBiasedRawHandStrength(opponentSeat);

        int potTotal = ai_.getTotalPotAmount();

        if (bOpponentPotCommitted)
        {
            potTotal += Math.min(opponent.getChipCount(), self_.getChipCount() - nAmountToCall_);

            ai_.appendDebug("Expected pot total: " + potTotal + "<br>");
        }
        else
        {
            ai_.appendDebug("Pot total: " + potTotal + "<br>");
        }

        float potOdds = (float)potTotal / (float)nAmountToCall_;
        float breakEvenPercent = 1.0f / (potOdds + 1);

        if (nAmountToCall_ > 0)
        {
            ai_.appendDebug("Pot odds " + PokerConstants.formatPercent(potOdds) + " to 1.<br>");
            ai_.appendDebug("Break even percent: " + PokerConstants.formatPercent(breakEvenPercent*100) + "%<br>");
        }

        ai_.appendDebug("Probable Strength: " + PokerConstants.formatPercent(probableStrength_*100) + "%<br>");
        ai_.appendDebug("Draw Strength: " + PokerConstants.formatPercent(drawStrength_*100) + "%<br>");

        AIOutcome outcome = new AIOutcome(self_.getHoldemHand(), self_);

        ai_.appendDebug("Heads up");

        if (bInPosition_)
        {
            ai_.appendDebug(" in position");

            if (ai_.hasActedThisRound())
            {
                if (nLastAction_ == HandAction.ACTION_BET)
                {
                    ai_.appendDebug(", opponent has check-raised");
                }
                else
                {
                    ai_.appendDebug(", opponent has re-raised");
                }

                if (bOpponentAllIn)
                {
                    ai_.appendDebug(" all-in");
                }

                // raise with very likely winners
                if (!bOpponentAllIn && (probableStrength_ > 0.85f))
                {
                    setEligible(OUTCOME_RAISE, true);
                    outcome.addTuple(AIOutcome.RAISE,  "Very Likely Best Hand", 0f, 1f-probableStrength_, probableStrength_);
                }
                else if (breakEvenPercent <= drawStrength_)
                {
                    if (probableStrength_ < .4f)
                    {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL,  "Pot Odds / Likely Worst Hand", 0f, 1f, 0f);
                    }
                    else if (probableStrength_ > .6f)
                    {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL,  "Pot Odds / Likely Best Hand", 0f, 1f, 0f);
                    }
                    else
                    {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL,  "Pot Odds", 0f, 1f, 0f);
                    }
                }
                else
                {
                    setEligible(OUTCOME_FOLD, true);
                    outcome.addTuple(AIOutcome.FOLD, "Pot Odds", 1f, 0f, 0f);
                }
            }
            else
            {
                if (nAmountToCall_ == 0)
                {
                    ai_.appendDebug(", opponent has checked");

                    // no point continuation betting against a pot committed opponent,
                    // or if we don't have at least half the pot to bet
                    if (!bOpponentPotCommitted && (self_.getChipCount() > hhand_.getTotalPotChipCount() / 2) &&
                        (round_ == HoldemHand.ROUND_FLOP) && ai_.wasLastRaiserPreFlop() && (apparentStrength_ > 0.5f))
                    {
                        setEligible(OUTCOME_CONTINUATION_BET, true);
                        outcome.addTuple(AIOutcome.BET,  "Continuation Bet", .35f, 0f, .65f);
                    }

                    if (probableStrength_ > 0.5f)
                    {
                        setEligible(OUTCOME_BET, true);
                        outcome.addTuple(AIOutcome.BET,  "Likely Best Hand", .25f, 0f, .75f);
                    }
                    else
                    {
                        setEligible(OUTCOME_CHECK, true);
                        outcome.addTuple(AIOutcome.CHECK,  "Likely Worst Hand", 1f, 0f, 0f);
                        // no point bluffing against a pot committed opponent,
                        // or if we don't have at least half the pot to bet
                        if (!isEligible(OUTCOME_CONTINUATION_BET) && !bOpponentPotCommitted && (self_.getChipCount() > hhand_.getTotalPotChipCount() / 2))
                        {
                            setEligible(OUTCOME_BLUFF, true);
                            outcome.addTuple(AIOutcome.BET,  "Bluff", .85f, 0f, .15f);
                        }
                    }

                    if (self_.getChipCount() >= hhand_.getTotalPotChipCount())
                    {
                        if (drawStrength_ > 0.85f)
                        {
                            setEligible(OUTCOME_TRAP, true);
                            outcome.addTuple(AIOutcome.BET,  "Trap", 0f, 0f, 1f);
                        }
                    }

                    if (drawStrength_ > 0.95f)
                    {
                        setEligible(OUTCOME_SLOW_PLAY, true);
                        outcome.addTuple(AIOutcome.CHECK,  "Slow-Play", .20f, .4f, .4f);
                    }
                }
                else

                {
                    ai_.appendDebug(", opponent has bet");

                    if (bOpponentAllIn)
                    {
                        ai_.appendDebug(" all-in");
                    }

                    ai_.appendDebug(".<br>");

                    // raise with very likely winners
                    if (!bOpponentAllIn && (probableStrength_ > 0.85f))
                    {
                        setEligible(OUTCOME_RAISE, true);
                        outcome.addTuple(AIOutcome.RAISE,  "Very Likely Best Hand", 0f, 1f-probableStrength_, probableStrength_);
                    }
                    else if (breakEvenPercent <= drawStrength_)
                    {
                        if (probableStrength_ < .4f)
                        {
                            setEligible(OUTCOME_CALL, true);
                            outcome.addTuple(AIOutcome.CALL,  "Pot Odds / Likely Worst Hand", 0f, 1f, 0f);
                        }
                        else if (probableStrength_ > .6f)
                        {
                            setEligible(OUTCOME_CALL, true);
                            outcome.addTuple(AIOutcome.CALL,  "Pot Odds / Likely Best Hand", 0f, 1f, 0f);
                        }
                        else
                        {
                            setEligible(OUTCOME_CALL, true);
                            outcome.addTuple(AIOutcome.CALL,  "Pot Odds", 0f, 1f, 0f);
                        }
                    }
                    else
                    {
                        setEligible(OUTCOME_FOLD, true);
                        outcome.addTuple(AIOutcome.FOLD,  "Pot Odds", 1f, 0f, 0f);
                    }
                }
            }
        }
        else
        {
            ai_.appendDebug(" out of position");

            if (ai_.hasActedThisRound())
            {
                switch (nLastAction_)
                {
                    case HandAction.ACTION_CHECK:
                        ai_.appendDebug(", opponent has bet");
                        break;
                    case HandAction.ACTION_BET:
                        ai_.appendDebug(", opponent has raised");
                        break;
                    case HandAction.ACTION_RAISE:
                        ai_.appendDebug("opponent has re-raised");
                        break;
                }

                if (bOpponentAllIn)
                {
                    ai_.appendDebug(" all-in");
                }

                // raise with very likely winners
                if (!bOpponentAllIn && (probableStrength_ > 0.85f))
                {
                    setEligible(OUTCOME_RAISE, true);
                    outcome.addTuple(AIOutcome.RAISE,  "Very Likely Best Hand", 0f, 1f-probableStrength_, probableStrength_);
                }
                else if (breakEvenPercent <= drawStrength_)
                {
                    if (probableStrength_ < .4f)
                    {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL,  "Pot Odds / Likely Worst Hand", 0f, 1f, 0f);
                    }
                    else if (probableStrength_ > .6f)
                    {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL,  "Pot Odds / Likely Best Hand", 0f, 1f, 0f);
                    }
                    else
                    {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL,  "Pot Odds", 0f, 1f, 0f);
                    }
                }
                else
                {
                    setEligible(OUTCOME_FOLD, true);
                    outcome.addTuple(AIOutcome.FOLD,  "Pot Odds", 1f, 0f, 0f);
                }
            }
            else
            {
                ai_.appendDebug(", first to act");

                // no point continuation betting against a pot committed opponent,
                // or if we don't have at least half the pot to bet
                if (!bOpponentPotCommitted && (self_.getChipCount() > hhand_.getTotalPotChipCount() / 2) &&
                    (round_ == HoldemHand.ROUND_FLOP) && ai_.wasLastRaiserPreFlop() && (apparentStrength_ > 0.5f))
                {
                    setEligible(OUTCOME_CONTINUATION_BET, true);
                    outcome.addTuple(AIOutcome.BET,  "Continuation Bet", .35f, 0f, .65f);
                }

                if (probableStrength_ > 0.5f)
                {
                    setEligible(OUTCOME_BET, true);
                    outcome.addTuple(AIOutcome.BET,  "Likely Best Hand", .2f, 0f, .8f);

                    if (drawStrength_ > 0.75f)
                    {
                        // no point in check-raise or trap if we have less than a pot worth of chips
                        if (self_.getChipCount() >= hhand_.getTotalPotChipCount())
                        {
                            setEligible(OUTCOME_CHECK_RAISE,  true);
                            outcome.addTuple(AIOutcome.CHECK,  "Check-Raise", .2f, 0f, .8f);

                            if (drawStrength_ > 0.85f)
                            {
                                setEligible(OUTCOME_TRAP, true);
                                outcome.addTuple(AIOutcome.BET,  "Trap", 0f, 0f, 1f);
                            }
                        }

                        if (drawStrength_ > 0.95f)
                        {
                            setEligible(OUTCOME_SLOW_PLAY, true);
                            outcome.addTuple(AIOutcome.CHECK,  "Slow-Play", .2f, 0f, .8f);
                        }
                    }
                }
                else
                {
                    if (probableStrength_ < 0.5f)
                    {
                        setEligible(OUTCOME_CHECK,  true);
                        outcome.addTuple(AIOutcome.CHECK,  "Likely Worst Hand", 1f, 0f, 0f);
                    }

                    // no point bluffing against a pot committed opponent,
                    // or if we don't have at least half the pot to bet
                    if (!bOpponentPotCommitted && (self_.getChipCount() > hhand_.getTotalPotChipCount() / 2))
                    {
                        if (bCardsToCome_ && (drawStrength_ > 0.5f))
                        {
                            setEligible(OUTCOME_SEMI_BLUFF, true);
                            outcome.addTuple(AIOutcome.BET,  "Semi-Bluff", .5f, 0f, .5f);
                        }
                        else
                        {
                            setEligible(OUTCOME_BLUFF, true);
                            outcome.addTuple(AIOutcome.BET,  "Bluff", .85f, 0f, .15f);
                        }
                    }
                }
            }
        }

        ai_.appendDebug(".<br>");

        if (!bOpponentAllIn && bOpponentPotCommitted)
        {
            ai_.appendDebug("Opponent is pot-committed.");
        }

        float amountNumerator = 0f;
        int amountDivisor = 0;

        if (probableStrength_ > 0.85f)
        {
            ai_.appendDebug("Very likely best hand.<br>");
            amountNumerator += 1/3f;
            ++amountDivisor;
        }
        else if (probableStrength_ < 0.30f)
        {
            ai_.appendDebug("Very likely worst hand.<br>");
            amountNumerator += 1f;
            ++amountDivisor;
        }
        else
        {
            ai_.appendDebug("Moderately strong hand.<br>");
            amountNumerator += 2/3f;
            ++amountDivisor;
        }

        float chanceToImprove = drawStrength_ - probableStrength_;

        if (chanceToImprove > 1/3f)
        {
            ai_.appendDebug("Very likely to improve.<br>");
            amountNumerator += 1/2f;
            ++amountDivisor;
        }
        else if (chanceToImprove > 0.15f)
        {
            ai_.appendDebug("Primary draw.<br>");
            amountNumerator += 2/3f;
            ++amountDivisor;
        }
        else if ((probableStrength_ > 0.5f) && (chanceToImprove < -0.10))
        {
            ai_.appendDebug("Outdraw risk.<br>");
            amountNumerator += 1f;
            ++amountDivisor;
        }

        float amountRatio = amountNumerator / amountDivisor;

        int betAmount = (int)(amountRatio * hhand_.getTotalPotChipCount());

        ai_.appendDebug("Bet amount: " + betAmount + " (" + PokerConstants.formatPercent(amountRatio*100) + "% of the pot).<br>");

        betRange_ = new BetRange(BetRange.ALL_IN);

        String allInReason = null;

        if (bPotCommitted_)
        {
            allInReason = "Pot Committed";
        }
        else if (bOpponentPotCommitted)
        {
            allInReason = "Opponent is Pot Committed";
        }
        else if ((self_.getChipCount() - betAmount) <= self_.getChipCountAtStart() / 2)
        {
            allInReason = "Reasonable Bet Will Commit Me to the Pot";
        }
        else if ((opponent.getChipCount() - betAmount) <= opponent.getChipCountAtStart() / 2)
        {
            allInReason = "Call of Reasonable Bet Will Commit Opponent to the Pot";
        }
        else
        {
            betRange_ = new BetRange(BetRange.POT_SIZE, amountRatio, amountRatio);
        }

        // TODO: add consideration of opponent's likeliness to have hit the flop

        /*
        int count = 0;

        //for (int i = 0; i < score_.length; ++i) if (eligible_[i]) ++count;

        for (int i = 0; i < score_.length; ++i) if (eligible_[i])
        {
            if (count == 0)
            {
                ai_.appendDebug("Recommend ");
            }
            else
            {
                ai_.appendDebug("or ");
            }

            ai_.appendDebug(getOutcomeLabel(i));
            ai_.appendDebug(" ");
            ++count;
        }
        */
        ai_.appendDebug("<br>");
        ai_.appendDebug(outcome.toHTML());

        strongestOutcome_ = outcome.selectOutcome(hhand_.getPotStatus());

        // TODO: JDD commented this out - need to pass in a GameContext
//        if (NEWCODE && TESTING(PokerInit.TESTING_CHAT_AI))
//        {
//            GameEngine engine = GameEngine.getGameEngine();
//            if (engine != null)
//            {
//                TournamentDirector td = (TournamentDirector)engine.getGameManager();
//                if (td != null)
//                {
//                    td.deliverChatLocal
//                        (PokerConstants.CHAT_1, PokerUtils.chatInformation(ai_.getDebugText()), self_.getID());
//                }
//            }
//            return;
//        }

        V2Player ai = ai_;

        int potStatus = ai.getPotStatus();

        float xBasicsPosition = ai.getStratFactor("basics.position", 0.0f, 2.0f);
        float xBasicsPotOdds = ai.getStratFactor("basics.pot_odds_call", 0.0f, 2.0f);
        float xBasicsAggression = ai.getStratFactor("basics.aggression", -1.0f, +1.0f);
        float xBasicsObservation = ai.getStratFactor("basics.observation", 0.0f, 2.0f);
        float xTilt = ai.getStratFactor("discipline.tilt", 0.0f, 1.0f);

        Hand community = ai.getCommunity();
        Hand pocket = ai.getHand();

        int startingPosition = ai.getStartingPositionCategory();

        int numWithCards = hhand_.getNumWithCards();
        int amountToCall = ai.getAmountToCall();
        int playerChips = ai.getChipCount();

        float breakEvenPercentage = 1.0f / (potOdds + 1);
        float rhs;
        float bhs;
        float ehs;
        float drawPotential;
        float outdrawRisk;

        if (NEWCODE)
        {
            PocketRanks ranks = PocketRanks.getInstance(community);

            rhs = ranks.getRawHandStrength(pocket);

            bhs = pWeights_.getBiasedRawHandStrength(self_.getSeat());

            if (round_ < HoldemHand.ROUND_RIVER)
            {
                ehs = pWeights_.getBiasedEffectiveHandStrength(self_.getSeat());
            }
            else
            {
                ehs = bhs;
            }

            drawPotential = ehs - bhs;
            outdrawRisk = 0.0f;

            if (drawPotential < 0.0f)
            {
                outdrawRisk = -drawPotential;
                drawPotential = 0.0f;
            }
        }
        else
        {
            rhs = ai.getRawHandStrength();
            bhs = ai.getBiasedHandStrength();
            ehs = ai.getBiasedEffectiveHandStrength(xBasicsPotOdds * potOdds);
            drawPotential = ai.getBiasedPositivePotential();
            outdrawRisk = (float)Math.pow(1.0 + ai.getBiasedNegativePotential(), numWithCards - 1) - 1.0f;
        }

        HandPotential potential = new HandPotential(pocket, community);

        int pNutFlush = potential.getHandCount(HandPotential.NUT_FLUSH, 0);
        int pNonNutFlush = potential.getHandCount(HandPotential.FLUSH, 0) - pNutFlush;
        int pNutStraight = potential.getHandCount(HandPotential.NUT_STRAIGHT, 0);
        int pNonNutStraight = potential.getHandCount(HandPotential.NON_NUT_STRAIGHT, 0);

        float xStraightDraw =
                    ai.getStratFactor("draws.straight.nut", -1.0f, 1.0f) * pNutStraight +
                    ai.getStratFactor("draws.straight.non_nut", -1.0f, 1.0f)  * 0.5f * pNonNutStraight;

        float xFlushDraw =
            ai.getStratFactor("draws.flush.nut", -1.0f, 1.0f) * pNutFlush +
            ai.getStratFactor("draws.flush.non_nut", -1.0f, 1.0f) * 0.5f * pNonNutFlush;

        if (community.hasQuads() || (community.hasTrips() && community.hasPair()))
        {
            xStraightDraw *= 0.0f;
            xFlushDraw *= 0.0f;
        }
        else if (community.hasPossibleFlush())
        {
            xFlushDraw *= 0.75d;

            if (community.hasFlush())
            {
                xStraightDraw = 0.0f;
            }
            else
            {
                xStraightDraw *= 0.5f;
            }
        }

        int roundsNoAction = 0;

        switch (round_)
        {
            case HoldemHand.ROUND_RIVER:
                if (!hhand_.getWasPotAction(HoldemHand.ROUND_TURN))
                {
                    ++roundsNoAction;
                }
                else
                {
                    break;
                }
            case HoldemHand.ROUND_TURN:
                if (!hhand_.getWasPotAction(HoldemHand.ROUND_FLOP))
                {
                    ++roundsNoAction;
                }
                break;
        }

        int handsToBB = ai.getHandsBeforeBigBlind();

        if (ai.debugEnabled())
        {
            ai.appendDebug("Raw Tilt Factor: " + xTilt + "<br>");
            ai.appendDebug("Raw Hand Strength: " + rhs + "<br>");
            ai.appendDebug("Biased Hand Strength: " + bhs + "<br>");
            ai.appendDebug("Effective Hand Strength: " + ehs + "<br>");
            ai.appendDebug("Break Even Percentage: " + breakEvenPercentage + "<br>");
            ai.appendDebug("Draw Potential (bppot): " + drawPotential + "<br>");
            ai.appendDebug("Outdraw Risk (bnpot): " + outdrawRisk + "<br>");
            if (!NEWCODE)
            {
                ai.appendDebug("Negative Potential: " + ai.getNegativeHandPotential() + "<br>");
                ai.appendDebug("Biased Negative Potential: " + ai.getBiasedNegativePotential() + "<br>");
            }
            ai.appendDebug("Outdraw Risk: " + outdrawRisk + "<br>");
            //ai.appendDebug("Biased Effective Hand Strength: " + ehsA + "<br>");
            //ai.appendDebug("Position Adjustment: " + adjPosition + "<br>");
            //ai.appendDebug("Position Factor: " + xBasicsPosition + "<br>");
            ai.appendDebug("Pot Total: " + potTotal + "<br>");
            ai.appendDebug("Amount To Call: " + amountToCall + "<br>");
            ai.appendDebug("Pot Odds: " + potOdds + "<br>");
            ai.appendDebug("Pot Odds Factor: " + xBasicsPotOdds + "<br>");
            ai.appendDebug("Straight Draw Factor: " + xStraightDraw + "<br>");
            ai.appendDebug("Flush Draw Factor: " + xFlushDraw + "<br>");
            ai.appendDebug("Aggression Factor: " + xBasicsAggression + "<br>");
            ai.appendDebug("Preceding post-flop rounds w/ no action: " + roundsNoAction + "<br>");
            ai.appendDebug("Hands Before Big Blind: " + handsToBB + "<br>");
        }

        // default bet range 1/2 to whole pot
        betRange_ = new BetRange(BetRange.POT_SIZE, 0.5f, 1.0f);

        /*
        float averageCheckRate = 0.0f;

        for (int i = hhand.getNumPlayers()-1; i >= 0; --i)
        {
            PokerPlayer opponent = hhand.getPlayerAt(i);
            if (opponent == ai.getPokerPlayer()) continue;
            if (opponent.isFolded()) continue;
            OpponentModel om = ai.getOpponentModel(opponent.getSeat());
            float v;
            switch (round)
            {
                case HoldemHand.ROUND_FLOP:
                    v = om.checkFoldFlop.getWeightedPercentTrue(0.8f) * om.actFlop.getWeightedPercentTrue(0.2f);
                    averageCheckRate += v;
                    break;
                case HoldemHand.ROUND_TURN:
                    v = om.checkFoldTurn.getWeightedPercentTrue(0.8f) * om.actTurn.getWeightedPercentTrue(0.2f);
                    averageCheckRate += v;
                    break;
                case HoldemHand.ROUND_RIVER:
                    v = om.checkFoldRiver.getWeightedPercentTrue(0.8f) * om.actRiver.getWeightedPercentTrue(0.2f);
                    averageCheckRate += v;
                    break;
            }
        }

        averageCheckRate /= (numWithCards - 1);
        */

        if ((round_ == HoldemHand.ROUND_RIVER) && (potStatus != PokerConstants.NO_POT_ACTION) && (rhs == 1.0f))
        {
            if (playerChips > amountToCall)
            {
                float allInPotRatio = (float)Math.ceil((float)(playerChips - amountToCall) / (float)potTotal);

                // *always* raise/reraise on the river with the pure nuts
                betRange_ = new BetRange(BetRange.POT_SIZE, (float)(Math.min(0.5f, allInPotRatio)), allInPotRatio);

                adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            }
            else
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            }

            return;
        }

        if (potStatus == PokerConstants.NO_POT_ACTION)
        {
            // initialize default action
            adjustOutcome(OUTCOME_CHECK, FACTOR_DEFAULT, 1.0f);

            float allWeak = 1.0f;

            // of remaining players, compute overall likelihood none has a calling hand
            for (int i = hhand_.getNumPlayers()-1; i >= 0; --i)
            {
                PokerPlayer p = hhand_.getPlayerAt(i);
                if (p.getSeat() == ai.getSeatNumber()-1) continue;
                if (p.isFolded()) continue;
                if (hhand_.getLastActionThisRound(p) == HandAction.ACTION_CHECK) continue;
                allWeak *= p.getOpponentModel().getCheckFoldPostFlop(round_, 0.5f);
            }

            if (ai.debugEnabled())
            {
                ai.appendDebug("All Weak Probability: " + allWeak + "<br>");
            }

            // CONSIDER WHETHER WE CAN OPEN THE BETTING FOR STRENGTH

            setEligible(OUTCOME_BET, true);

            // boost for first action                 ehs * 0.17 makes 85% hands the cutoff
            adjustOutcome(OUTCOME_BET, FACTOR_FIRST_ACTION, (NEWCODE ? ehs * 0.17f : 0.25f));

            // base hand strength scores
            adjustOutcome(OUTCOME_BET, FACTOR_RAW_HAND_STRENGTH, (NEWCODE ? rhs : rhs*rhs + 0.05f));
            adjustOutcome(OUTCOME_BET, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
            adjustOutcome(OUTCOME_BET, FACTOR_HAND_POTENTIAL, drawPotential);
            adjustOutcome(OUTCOME_BET, FACTOR_OUTDRAW_RISK, outdrawRisk);

            // positional disadvantage
            if (!NEWCODE) adjustOutcome(OUTCOME_BET, FACTOR_LEFT_TO_ACT, -0.08f * ai.getNumAfter() * xBasicsPosition);

            // weak field boost (accounted for in new hand strength)
            if (!NEWCODE) adjustOutcome(OUTCOME_BET, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);

            // aggression factor
            adjustOutcome(OUTCOME_BET, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);

            // tilt
            if ((ai.getSteam() > 0.1) && (xTilt > 0))
            {
                adjustOutcome(OUTCOME_BET, FACTOR_STEAM, ai.getSteam() * xTilt * 0.1f);
            }

            if (NEWCODE)
            {
            }
            else
            {
                // if everyone is likely to be weak, boost bet value, but less for very strong
                // hands where we might rather check/call or check-raise
                if ((round_ == HoldemHand.ROUND_FLOP) || (roundsNoAction > 0))
                {
                    adjustOutcome(OUTCOME_BET, FACTOR_STEAL_POTENTIAL, xBasicsObservation * (1.0f-rhs*rhs) * allWeak * 0.50f);
                }
            }

            if ((getStrongestOutcome() == OUTCOME_BET) && (ai.getNumAfter() > 0))
            {
                // CONSIDER CHECK-RAISE AND SLOW-PLAY

                setEligible(OUTCOME_CHECK_RAISE, true);

                // boost for players left to act
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_LEFT_TO_ACT, 0.06f * ai.getNumAfter() * xBasicsPosition * (1.0f-allWeak));

                // base hand strength scores
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_RAW_HAND_STRENGTH, (NEWCODE ? rhs : rhs*rhs*rhs + 0.10f));
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_STEAL_POTENTIAL, xBasicsObservation * allWeak * -0.15f);

                // penalty for outdraw risk
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            }
            else
            {
                // CONSIDER WHETHER WE SHOULD MAKE A CONTINUATION BET

                // TODO: don't make continuation bets when we have good draws and would rather see a free card
                // TODO: don't make continuation bets into very scary flops
                // e.g. AhKhJd when holding 88
                /*
                community.hasPair();
                community.containsRank(Card.ACE);
                community.containsRank(Card.KING);
                community.containsRank(Card.QUEEN);
                community.containsRank(Card.JACK);
                */

                // consider continuation bet on the flop if last raiser pre-flop
                if (ai.wasLastRaiserPreFlop() && (numWithCards < 4) && (round_ == HoldemHand.ROUND_FLOP))
                {
                    setEligible(OUTCOME_CONTINUATION_BET, true);

                    int draws =
                            pNutFlush*3 + pNonNutFlush +
                            pNutStraight*2 + pNonNutStraight;

                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_RAW_HAND_STRENGTH, (NEWCODE ? rhs : rhs*rhs + 0.05f));
                    //adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_HAND_POTENTIAL, (NEWCODE ? drawPotential : drawPotential - 0.01f * draws));
                    if (NEWCODE) adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_OUTDRAW_RISK, outdrawRisk);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_ACTION, (NEWCODE ? ehs * 0.17f : 0.35f));

                    // stronger if first raiser pre-flop
                    if (ai.wasFirstRaiserPreFlop())
                    {
                        // even stronger if raised in-between first and last raise
                        if (ai.wasOnlyRaiserPreFlop())
                        {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_ONLY_PRE_FLOP_RAISER, 0.15f);
                        }
                        else
                        {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_PRE_FLOP_RAISER, 0.17f);
                        }
                    }
                    else
                    {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_LAST_PRE_FLOP_RAISER, 0.05f);
                    }

                    if (ai.wasFirstRaiserPreFlop())
                    {
                        // stronger the earlier position we opened from
                        switch (startingPosition)
                        {
                            case PokerAI.POSITION_EARLY:
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.1f);
                                break;
                            case PokerAI.POSITION_MIDDLE:
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.05f);
                                break;
                        }
                    }

                    // weaker if two opponents
                    if (numWithCards == 3)
                    {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PLAYERS_LEFT, -0.1f);
                    }

                    // stronger the larger our raises were?

                    if (strongestOutcome_ == OUTCOME_CONTINUATION_BET)
                    {
                        betRange_ = new BetRange(BetRange.POT_SIZE, 0.4f, 0.7f);
                    }
                }
                else
                {
                    // consider probe bet if original raiser has checked
                    PokerPlayer firstRaiser = hhand_.getFirstBettor(HoldemHand.ROUND_PRE_FLOP, true);

                    if (firstRaiser != null)
                    {
                        HandAction firstRaiserAction = hhand_.getFirstVoluntaryAction(firstRaiser, round_);

                        if ((firstRaiserAction != null) && (firstRaiserAction.getAction() == HandAction.ACTION_CHECK))
                        {
                            probeBet_ = true;

                            betRange_ = new BetRange(BetRange.POT_SIZE, 0.3f, 0.5f);

                            adjustOutcome(OUTCOME_BET, FACTOR_PROBE_BET, rhs * 0.1f);
                        }
                    }
                }
            }
        }
        else // pot action already
        {
            float xrhs;
            float xbhs;

            adjustOutcome(OUTCOME_FOLD, FACTOR_DEFAULT, 1.0f);

            PokerPlayer bettor = hhand_.getFirstBettor(round_, false);

            // CONSIDER WHETHER WE CAN CALL

            xrhs = (float)Math.sin((rhs - 0.5f) * Math.PI) * 0.5f + 0.50f;
            xbhs = (float)Math.sin((bhs - 0.5f) * Math.PI) * 0.5f + 0.50f;

            float bettorActFrequency = 0.0f;
            float bettorOpenFrequency = 0.0f;
            float bettorRaiseFrequency = 0.0f;
            float bettorCheckFoldFrequency = 0.0f;

            OpponentModel bettorModel = bettor.getOpponentModel();

            switch (round_)
            {
                case HoldemHand.ROUND_FLOP:
                    bettorActFrequency =
                            bettor.getOpponentModel().actFlop.getWeightedPercentTrue(0.1f);
                    bettorCheckFoldFrequency =
                        bettorModel.checkFoldFlop.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorOpenFrequency =
                        bettorModel.openFlop.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorRaiseFrequency =
                        bettorModel.raiseFlop.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    break;
                case HoldemHand.ROUND_TURN:
                    bettorActFrequency =
                            bettorModel.actTurn.getWeightedPercentTrue(0.1f);
                    bettorCheckFoldFrequency =
                        bettorModel.checkFoldTurn.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorOpenFrequency =
                        bettorModel.openTurn.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorRaiseFrequency =
                        bettorModel.raiseTurn.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    break;
                case HoldemHand.ROUND_RIVER:
                    bettorActFrequency =
                            bettorModel.actRiver.getWeightedPercentTrue(0.1f);
                    bettorCheckFoldFrequency =
                        bettorModel.checkFoldRiver.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorOpenFrequency =
                        bettorModel.openRiver.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorRaiseFrequency =
                        bettorModel.raiseRiver.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    break;
            }

            float bettorOverbetFrequency = bettorModel.handsOverbetPotPostFlop.getWeightedPercentTrue(0.5f);
            float bettorBetFoldFrequency = bettorModel.handsBetFoldPostFlop.getWeightedPercentTrue(0.5f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_CALL, FACTOR_LEFT_TO_ACT, -0.08f * ai.getNumAfter() * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, (NEWCODE ? 0.85f - Math.abs(rhs - 0.85f) : xrhs + 0.10f));
            adjustOutcome(OUTCOME_CALL, FACTOR_BIASED_HAND_STRENGTH, (NEWCODE ? bhs-rhs : xbhs - xrhs));
            adjustOutcome(OUTCOME_CALL, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            if (!NEWCODE) adjustOutcome(OUTCOME_CALL, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);
            //adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, adjPosition);
            adjustOutcome(OUTCOME_CALL, FACTOR_STRAIGHT_DRAW, xStraightDraw * 0.075f);
            adjustOutcome(OUTCOME_CALL, FACTOR_FLUSH_DRAW, xFlushDraw * 0.05f);
            if (round_ < HoldemHand.ROUND_RIVER)
            {
                if (NEWCODE)
                {
                    // ehs/breakEvenPercentage = 1 at break-even
                    // bhs = chance we currently have the best hand
                    // ehs-bhs = additional chance the next card gives us to have the next hand
                    adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (1.0f-ehs) * ehs/breakEvenPercentage * xBasicsPotOdds);
                    adjustOutcome(OUTCOME_CALL, FACTOR_HAND_POTENTIAL, drawPotential * ehs/breakEvenPercentage * xBasicsPotOdds);
                }
                else
                {
                    adjustOutcome(OUTCOME_CALL, FACTOR_HAND_POTENTIAL, drawPotential * xBasicsPotOdds * (potOdds+1.0f) / 2);
                }
            }
            else
            {
                if (NEWCODE)
                {
                    adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (1.0f-bhs) * bhs/breakEvenPercentage * xBasicsPotOdds);
                }
                else
                {
                    adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (potOdds + 1.0f) * xBasicsPotOdds * ehs / 2);
                }
            }
            adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL, -0.15f * (float)Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - ehs));
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_BET_FREQUENCY, xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_OVERBET_FREQUENCY, xBasicsObservation * bettorOverbetFrequency * 0.10f);

            // CONSIDER WHETHER WE CAN RAISE / RE-RAISE

            xrhs = (rhs+0.05f)*(rhs+0.05f);
            xbhs = (bhs+0.05f)*(bhs+0.05f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_RAISE, FACTOR_LEFT_TO_ACT, -0.05f * ai.getNumAfter() * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, (NEWCODE ? rhs : xrhs + 0.10f));
            adjustOutcome(OUTCOME_RAISE, FACTOR_BIASED_HAND_STRENGTH, (NEWCODE ? bhs-rhs : xbhs - xrhs));
            adjustOutcome(OUTCOME_RAISE, FACTOR_OUTDRAW_RISK, outdrawRisk);
            if (!NEWCODE) adjustOutcome(OUTCOME_RAISE, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);
            //adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, adjPosition);
            adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);
            //adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);
            if (round_ < HoldemHand.ROUND_RIVER)
            {
                if (NEWCODE)
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, (1.0f-ehs) * ehs/breakEvenPercentage * xBasicsPotOdds);
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_POTENTIAL, drawPotential);
                }
                else
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_POTENTIAL, drawPotential * xBasicsPotOdds * (potOdds + 1.0f) / 2);
                }
            }
            else
            {
                if (NEWCODE)
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, (1.0f-ehs) * ehs/breakEvenPercentage * xBasicsPotOdds / 2);
                }
                else
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, (potOdds + 1.0f) * xBasicsPotOdds * ehs / 2);
                }
            }
            adjustOutcome(OUTCOME_RAISE, FACTOR_BET_TO_CALL, -0.15f * (float)Math.pow(amountToCall / playerChips, 2) * (1.0f - ehs));
            //adjustOutcome(OUTCOME_RAISE, FACTOR_LAST_TO_ACT, nAfter == 0 ? 0.05d : 0.0f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FREQUENCY, xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_OVERBET_FREQUENCY, xBasicsObservation * bettorOverbetFrequency * 0.10f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FOLD_FREQUENCY, xBasicsObservation * bettorBetFoldFrequency * 0.10f);
        }

        /*
        if (matrix)
        {
            float xxx = ehsA;

            //System.out.println(xxx);
            betRange_ = new BetRange(BetRange.ALL_IN);
            if (xxx < 0.60) strongestOutcome_ = OUTCOME_FOLD;
            else if (xxx < 0.70) strongestOutcome_ = OUTCOME_CHECK_RAISE;
            else if (xxx < 0.80) strongestOutcome_ = OUTCOME_LIMP;
            else if (xxx < 0.85) strongestOutcome_ = OUTCOME_CALL;
            else if (xxx < 0.95) strongestOutcome_ = OUTCOME_STEAL;
            else strongestOutcome_ = OUTCOME_BET;

            return;
        }

        float xTournamentStackSize = ai.getStratFactor("tournament.stack_size", 0.0f, 2.0f);
        float xTournamentOpponentStackSize = ai.getStratFactor("tournament.opponent_stack_size", 0.0f, 2.0f);

        int position = ai.getPostFlopPositionCategory();
        float adjPosition = 0.0f;

        int nAfter = ai.getPlayersLeftToAct();
        int nBefore = ai.getPlayersBefore();

        adjPosition = nBefore * 0.05d - nAfter * 0.1d;

        switch (position)
        {
            case PokerAI.POSITION_EARLY:
                adjPosition = -0.1d * xBasicsPosition;
                break;
            case PokerAI.POSITION_MIDDLE:
                adjPosition = 0.0f * xBasicsPosition;
                break;
            case PokerAI.POSITION_LATE:
                adjPosition = 0.1d * xBasicsPosition;
                break;
            case PokerAI.POSITION_LAST:
                adjPosition = 0.2d * xBasicsPosition;
                break;
        }

        //adjustOutcome(OUTCOME_CHECK, FACTOR_BIASED_HAND_STRENGTH, rhs-bhs);
        //adjustOutcome(OUTCOME_FOLD, FACTOR_BIASED_HAND_STRENGTH, rhs-bhs);

        if (ai.isPotReraised())
        {
            adjustOutcome(OUTCOME_RAISE, FACTOR_POT_RERAISED, (bhs-1.0f));
        }
        else if (ai.isPotRaised())
        {
            adjustOutcome(OUTCOME_RAISE, FACTOR_POT_RAISED, -0.4d * amountToCall / ai.getTotalPotAmount());
            //adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, xBasicsPotOdds * Math.max(potOdds - 1.0f, 0.0f) * 0.1);
        }
        */
    }

    private void determineEligibleOutcomes(V2Player player)
    {
        // Establish which outcomes are eligible in the current situation.

        boolean raised = player.isPotRaised();

        int amountToCall = player.getAmountToCall();
        int playerChips = player.getChipCount();
        int totalPot = player.getTotalPotAmount();
        int minBet = player.getPokerPlayer().getHoldemHand().getMinRaise();
        int preFlopPosition = player.getStartingPositionCategory();

        setEligible(OUTCOME_FOLD, (amountToCall > 0));
        setEligible(OUTCOME_CHECK, (amountToCall == 0));

        if (player.isPreFlop())
        {
            setEligible(OUTCOME_RAISE, (amountToCall >= 0) && (playerChips > amountToCall));

            if ((preFlopPosition == PokerAI.POSITION_BIG) || raised)
            {
                setEligible(OUTCOME_CALL, raised);
            }
        }
        else
        {
            setEligible(OUTCOME_CALL, (amountToCall > 0));
            setEligible(OUTCOME_RAISE, (amountToCall > 0) && (playerChips > amountToCall));
            //setEligible(OUTCOME_SEMI_BLUFF, (playerChips > amountToCall));
            //setEligible(OUTCOME_TRAP, (playerChips > amountToCall));
            //setEligible(OUTCOME_SLOW_PLAY, (playerChips > minBet));
            //setEligible(OUTCOME_CHECK_RAISE, (amountToCall == 0) && (playerChips > minBet));
        }
    }

    private void executePreFlop(V2Player player)
    {
        float xTournamentStackSize = player.getStratFactor("tournament.stack_size", 0.0f, 2.0f);
        float xTournamentOpponentStackSize = player.getStratFactor("tournament.opponent_stack_size", 0.0f, 2.0f);

        int potStatus = player.getPotStatus();

        boolean hasActed = player.hasActedThisRound();

        int startingPosition = player.getStartingPositionCategory();

        HoldemHand hhand = player.getPokerPlayer().getHoldemHand();

        int numWithCards = hhand.getNumWithCards();

        int postFlopPosition = player.getPostFlopPositionCategory();

        Hand hand = player.getHand();

        int numPlayers = player.getNumPlayers();

        float handStrength = ((V2Player) player).getHandStrength();

        //float xBasicsTightness = player.getStratFactor("basics.tightness", -0.8f, +0.8f);
        float xBasicsAggression = player.getStratFactor("basics.aggression", -0.2f, +0.2f);
        float xBasicsBoldness = player.getStratFactor("basics.boldness", -0.8f, -0.0f);
        float xBasicsPosition = player.getStratFactor("basics.position", 0.0f, 2.0f);
        float xBasicsObservation = player.getStratFactor("basics.observation", 0.0f, 2.0f);

        float xDisciplineLimp = getLimpFactor(player, hand);

        float dFoldExponent = 1.5f;
        float dBetExponent = 3.0f;

        int hohZone = player.getHohZone();

        /*
        float zoneLoosenUp = 0.0f;
        float zoneAggression = 0.0f;
        float zoneFirstInMultiplier = 1.0f;
        float zoneConserveStack = 0.0f;

        // basic zone stuff
        switch (hohZone)
        {
            case V2Player.HOH_GREEN:
                break;
            case V2Player.HOH_YELLOW:
                zoneLoosenUp = 0.1d;
                zoneAggression = 0.1d;
                zoneFirstInMultiplier = 1.5f;
                zoneConserveStack = 0.1d;
                break;
            case V2Player.HOH_ORANGE:
                zoneLoosenUp = 0.2d;
                zoneAggression = 0.2d;
                zoneFirstInMultiplier = 2.0f;
                zoneConserveStack = 0.2d;
                break;
            case V2Player.HOH_RED:
                zoneLoosenUp = 0.3d;
                zoneAggression = 0.3d;
                zoneFirstInMultiplier = 2.5f;
                zoneConserveStack = 0.3d;
                break;
            case V2Player.HOH_DEAD:
                zoneLoosenUp = 0.4d;
                zoneAggression = 0.4d;
                zoneFirstInMultiplier = 3.0f;
                zoneConserveStack = 0.4d;
                break;
        }
        */

        int nLeftToAct = player.getPlayersLeftToAct();

        float profileBasicsTightness = player.getStratFactor("handselection", hand, 1.0f, 0.0f);

        float tableTightness = 0f;

        for (int i = 0; i < PokerConstants.SEATS; ++i)
        {
            PokerPlayer p = hhand.getTable().getPlayer(i);

            if ((p != null) && (p.getSeat() != player.getPokerPlayer().getSeat()) &&
                !p.isFolded() && (p.getHand() != null))
            {
                tableTightness += p.getOpponentModel().getPreFlopTightness(p.getStartingPositionCategory(), .5f);
            }
        }

        tableTightness /= ((float)numWithCards-1f);

        float xBasicsTightness =
                Math.min(Math.max(profileBasicsTightness * 
                    (xBasicsObservation * ((1f+(2f-(tableTightness*2f)))/2)+(1f-xBasicsObservation)), 0f), 1f);

        float xBasicsPotOdds = player.getStratFactor("basics.pot_odds", 0.0f, 2.0f);
        float xTilt = player.getStratFactor("discipline.tilt", 0.0f, 1.0f);
        float xStealBlinds = player.getStratFactor("deception.steal_blinds", 1.0f, 0.0f);

        int tightnessBiasIndex =
                (int)Math.round((0.5f - ((xBasicsTightness > 0.5f) ? (xBasicsTightness - 0.5f) : xBasicsTightness)) / 0.05f);

        float tightnessBiasStrength = SimpleBias.getBiasValue(tightnessBiasIndex, hand);

        float foldStrengthDelta = 0.0f;

        if (((xBasicsTightness >= 0.5f) && (tightnessBiasStrength < handStrength)) ||
            ((xBasicsTightness < 0.5f) && (tightnessBiasStrength > handStrength)))
        {
            foldStrengthDelta = (tightnessBiasStrength - handStrength) * Math.abs(xBasicsTightness - 0.5f) * 2;
        }

        float tightnessAdjustment =
                ((float)Math.pow(1.0f - handStrength, dFoldExponent) -
                 (float)Math.pow(1.0f - handStrength - foldStrengthDelta, dFoldExponent));

        float adjustedHandStrength = handStrength + foldStrengthDelta;

        int bigBlindAmount = player.getBigBlindAmount();
        int playerChips = player.getChipCount();
        int amountToCall = player.getAmountToCall();
        int potTotal = player.getTotalPotAmount();
        float potOdds = amountToCall > 0 ? (float)potTotal / (float)amountToCall : 0;

        float xDisciplineBoredom = player.getStratFactor("discipline.boredom", 1.0f, 0.0f);
        int consecutiveUnpaid = player.getConsecutiveHandsUnpaid();
        float bored = ((float)Math.min(consecutiveUnpaid, 10))/10.0f - xDisciplineBoredom;

        int handsToBB = player.getHandsBeforeBigBlind();

        if (player.debugEnabled())
        {
            player.appendDebug("Hands Before Big Blind: " + handsToBB + "<br>");
            player.appendDebug("Consecutive Hands Unpaid: " + consecutiveUnpaid + "<br>");
            player.appendDebug("Boredom Factor: " + xDisciplineBoredom + "<br>");
            player.appendDebug("Boredom Adjustment: " + bored + "<br>");
            player.appendDebug("Steal Suspicion: " + player.getStealSuspicion() + "<br>");
            player.appendDebug("Effective M (Stack/Blinds): " + (int)player.getHohM() + "<br>");
            player.appendDebug("Q (Stack/Average): " + (float)((int)(player.getHohQ() * 100))/100.0f + "<br>");
            player.appendDebug("Harrington Zone: " + player.getHohZoneName() + "<br>");
            /*
            player.appendDebug("Zone Loosen Up Offset: " + zoneLoosenUp + "<br>");
            player.appendDebug("Zone Aggression Offset: " + zoneAggression + "<br>");
            player.appendDebug("Zone First In Multiplier: " + zoneFirstInMultiplier + "<br>");
            player.appendDebug("Zone Conserve Stack Offset: " + zoneConserveStack + "<br>");
            */
            player.appendDebug("Basics Tightness (Profile): " + profileBasicsTightness + "<br>");
            player.appendDebug("Table Tightness (Remaining Players): " + tableTightness + "<br>");
            player.appendDebug("Basics Tightness (Adjusted): " + xBasicsTightness + "<br>");
            player.appendDebug("Tightness Bias Index: " + tightnessBiasIndex + "<br>");
            player.appendDebug("Hand Strength: " + handStrength + "<br>");
            player.appendDebug("Tightness Bias Strength: " + tightnessBiasIndex + "<br>");
            player.appendDebug("Fold Strength Delta: " + foldStrengthDelta + "<br>");
            player.appendDebug("Tightness Adjustment: " + tightnessAdjustment + "<br>");
            player.appendDebug("Adjusted Hand Strength: " + adjustedHandStrength + "<br>");
            player.appendDebug("Steam: " + player.getSteam() + "<br>");
        }

        adjustOutcome(OUTCOME_FOLD, FACTOR_DEFAULT, 1.0f);
        adjustOutcome(OUTCOME_CHECK, FACTOR_DEFAULT, 1.0f);

        if (!hasActed && (potStatus == PokerConstants.NO_POT_ACTION) && (amountToCall > 0))
        {
            int startingOrder = player.getStartingOrder();

            float positionAdjustment =
                    xBasicsPosition * (startingOrder  + PokerConstants.SEATS - numPlayers) / PokerConstants.SEATS;

            if (xBasicsPosition < 1f)
            {
                positionAdjustment += (1f-xBasicsPosition) * .8f;
            }

            player.appendDebug("Start Order: " + (startingOrder + 1) + " of " + numPlayers + "<br>");
            player.appendDebug("Position Adjust: " + positionAdjustment + "<br>");

            if (adjustedHandStrength + positionAdjustment >= 1f)
            {
                AIOutcome outcome = new AIOutcome(hhand, player.getPokerPlayer());

                float aggression = player.getStratFactor("basics.aggression", 0f, 2f);
                float call = (startingPosition == PokerAI.POSITION_SMALL) ?
                        .25f * (aggression > 1f ? (2f-aggression) : (3.6f*(1f-aggression))+1f) :
                        .05f * (aggression > 1f ? (2f-aggression) : (9f*(1f-aggression))+1f);
                float raise = 1f - call;

                outcome.addTuple(AIOutcome.RAISE, "Desirable opening hand in this position.", 0f, call, raise);

                outcome_ = outcome;
                strongestOutcome_ = outcome.getStrongestOutcome(potStatus);
            }

            return;
        }

        // TODO: CONSIDER ALL-IN CALLS

        // SPECIAL RED/DEAD ZONE STRATEGY

        if ((hohZone == AIConstants.HOH_RED) || (hohZone == AIConstants.HOH_DEAD))
        {
            // TODO: consider other player zones; need stronger hand with short stacks left to act
            // TODO: lower standards as blinds get closer

            setEligible(OUTCOME_ALL_IN, true);

            float allInOdds = potTotal / playerChips;

            switch (potStatus)
            {
                case PokerConstants.NO_POT_ACTION:
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.20f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_BLINDS_CLOSING, 0.40f * (9 - handsToBB) / 9.0f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_POT_ODDS, allInOdds/10.0f);
                    break;
                case PokerConstants.CALLED_POT:
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.15f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_BLINDS_CLOSING, 0.25f * (9 - handsToBB) / 9.0f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_POT_ODDS, allInOdds/15.0f);
                    break;
                case PokerConstants.RERAISED_POT:
                case PokerConstants.RAISED_POT:
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.10f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_BLINDS_CLOSING, 0.15f * (9 - handsToBB) / 9.0f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_POT_ODDS, allInOdds/25.0f);
                    break;
            }

            adjustOutcome(OUTCOME_ALL_IN, FACTOR_AGGRESSION, xBasicsAggression);
        }
        else
        {

            // SPECIAL BSB STRATEGY

            // CONSIDER OPENING POT FOR STRENGTH - SINGLE LIMPER IS IGNORED

            if ((potStatus == PokerConstants.NO_POT_ACTION) ||
                ((potStatus == PokerConstants.CALLED_POT) && (player.getNumLimpers() == 1)))
            {
                int outcome = (potStatus == PokerConstants.NO_POT_ACTION) ? OUTCOME_OPEN_POT : OUTCOME_RAISE;

                setEligible(outcome, true);

                adjustOutcome(outcome, FACTOR_HAND_SELECTION, 0.6f + (adjustedHandStrength / 2.0f));
                adjustOutcome(outcome, FACTOR_AGGRESSION, xBasicsAggression);

                switch (startingPosition)
                {
                    case PokerAI.POSITION_MIDDLE:
                        // in middle position, four chip hands become openers
                        adjustOutcome(outcome, FACTOR_POSITION, xBasicsPosition * 0.1f);
                        break;
                    case PokerAI.POSITION_LATE:
                        // in late position, three chip hands become openers
                        adjustOutcome(outcome, FACTOR_POSITION, xBasicsPosition * 0.2f);
                        break;
                }

                // TODO: adjust small pairs and low suited connectors

                switch (hohZone)
                {
                    case AIConstants.HOH_YELLOW:
                        adjustOutcome(outcome, FACTOR_STACK_SIZE, 0.025f * xTournamentStackSize);
                        break;
                    case AIConstants.HOH_ORANGE:
                        adjustOutcome(outcome, FACTOR_STACK_SIZE, 0.05f*xTournamentStackSize);
                        break;
                }

                if ((player.getSteam() > 0.1) && (xTilt > 0))
                {
                    adjustOutcome(outcome, FACTOR_STEAM, player.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER PLAYING AGAINST MULTIPLE LIMPERS

            if ((potStatus == PokerConstants.CALLED_POT) && (player.getNumLimpers() > 1))
            {
                adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, 0.7f + (adjustedHandStrength / 2.0f));
                adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);
                //adjustOutcome(OUTCOME_RAISE, FACTOR_PLAYERS_DEALT, 0.1d * (PokerTable.SEATS - numPlayers) * adjustedHandStrength);
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_SELECTION, 0.5f);

                switch (hasActed ? postFlopPosition : startingPosition)
                {
                    case PokerAI.POSITION_SMALL:
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.075f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.15f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_BIG:
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.06f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.12f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_EARLY:
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.05f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.1f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_MIDDLE:
                        break;
                    case PokerAI.POSITION_LATE:
                    case PokerAI.POSITION_LAST:
                        if (postFlopPosition == PokerAI.POSITION_LAST)
                        {
                            adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.075f * xBasicsPosition);
                            adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.15f * xBasicsPosition);
                        }
                        else
                        {
                            adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.05f * xBasicsPosition);
                            adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.1f * xBasicsPosition);
                        }
                        break;
                }

                if ((player.getSteam() > 0.1) && (xTilt > 0))
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, player.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER WHETHER WE CAN PLAY AGAINST A RAISE

            if (potStatus == PokerConstants.RAISED_POT)
            {
                // TODO: factor in opponent zone and stack size
                // TODO: factor in pot odds and left to act

                // less than three chip hands are out of the question
                /*
                if (adjustedHandStrength < 0.6f)
                {
                    adjustOutcome(OUTCOME_FOLD, FACTOR_HAND_SELECTION, 1.0f);
                    return;
                }
                */

                switch (startingPosition)
                {
                    case PokerAI.POSITION_SMALL:
                    case PokerAI.POSITION_BIG:
                        // CONSIDER DEFENDING BLINDS
                        float stealSuspicion = player.getStealSuspicion();
                        // TODO: not if someone has called the raise already?  * already adjusting value
                        if (stealSuspicion > 0.0f)
                        {
                            adjustOutcome(OUTCOME_CALL, FACTOR_STEAL_SUSPECTED, stealSuspicion);
                            adjustOutcome(OUTCOME_RAISE, FACTOR_STEAL_SUSPECTED, stealSuspicion);
                        }
                }

                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);
                adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL, -0.15f * (float)Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - handStrength));

                // devalue raises from players in dire straits
                PokerPlayer raiser = hhand.getFirstBettor(HoldemHand.ROUND_PRE_FLOP, false);

                int rZone = player.getHohZone(raiser);

                switch (rZone)
                {
                    case AIConstants.HOH_DEAD:
                    case AIConstants.HOH_RED:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize* adjustedHandStrength * 0.05f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.05f);
                        break;
                    case AIConstants.HOH_ORANGE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.025f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.025f);
                        break;
                    case AIConstants.HOH_YELLOW:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.015f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.015f);
                        break;
                }

                // devalue raises from frequent raisers
                float raiserFrequency = (float)Math.max
                        (raiser.getOpponentModel().handsRaisedPreFlop.getWeightedPercentTrue(0.1f) - 0.1, 0.0);

                adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_RAISE_FREQUENCY, xBasicsObservation * raiserFrequency * 0.3f * handStrength);
                adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_RAISE_FREQUENCY, xBasicsObservation * raiserFrequency * 0.3f * handStrength);

                // devalue raises if we're getting low
                switch (hohZone)
                {
                    case AIConstants.HOH_ORANGE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE, xTournamentStackSize* adjustedHandStrength * 0.05f);
                        break;
                    case AIConstants.HOH_YELLOW:
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE, xTournamentStackSize* adjustedHandStrength * 0.02f);
                        break;
                }

                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_SELECTION, 1.0f + (adjustedHandStrength - 0.6f) / 4.0f);

                // boost pocket pairs
                if (hand.isPair())
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.4f);
                }
                else
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.2f);
                }

                adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);

                if (bored > 0)
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_BOREDOM, bored * 0.05f);
                }

                if ((player.getSteam() > 0.1) && (xTilt > 0))
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, player.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER PLAY AGAINST RE-RAISE

            if (potStatus == PokerConstants.RERAISED_POT)
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_SELECTION, 0.9f + (adjustedHandStrength - 0.6f) / 4.0f);
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);
                adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL, -0.15f * (float)Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - handStrength));

                // boost pocket pairs
                if (hand.isPair())
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.1f);
                }
                else
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.05f);
                }

                adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);

                // consider my post-flop position
                switch (postFlopPosition)
                {
                    case PokerAI.POSITION_EARLY:
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.025f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.5f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_MIDDLE:
                        break;
                    case PokerAI.POSITION_LATE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.025f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.05f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_LAST:
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.04f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.08f * xBasicsPosition);
                        break;
                }

                PokerPlayer firstRaiser = hhand.getFirstBettor(HoldemHand.ROUND_PRE_FLOP, false);
                PokerPlayer lastRaiser = hhand.getLastBettor(HoldemHand.ROUND_PRE_FLOP, false);

                // devalue raises from players in dire straits
                int rZone = player.getHohZone(lastRaiser);

                switch (rZone)
                {
                    case AIConstants.HOH_DEAD:
                    case AIConstants.HOH_RED:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.05f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.05f);
                        break;
                    case AIConstants.HOH_ORANGE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.025f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.025f);
                        break;
                    case AIConstants.HOH_YELLOW:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.015f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE, xTournamentOpponentStackSize*adjustedHandStrength * 0.015f);
                        break;
                }

                // devalue raises if we're getting low
                switch (hohZone)
                {
                    case AIConstants.HOH_ORANGE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE, xTournamentStackSize*adjustedHandStrength * 0.05f);
                        break;
                    case AIConstants.HOH_YELLOW:
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE, xTournamentStackSize*adjustedHandStrength * 0.02f);
                        break;
                }

                // devalue possible blind defense raise
                if (lastRaiser.isBlind())
                {
                    switch (firstRaiser.getStartingPositionCategory())
                    {
                        case PokerAI.POSITION_EARLY:
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.01f);
                            break;
                        case PokerAI.POSITION_MIDDLE:
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.03f);
                            break;
                        case PokerAI.POSITION_LATE:
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.08f);
                            break;
                        case PokerAI.POSITION_SMALL:
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.08f);
                            break;
                    }
                }

                int numPlayersWhenRaised = numWithCards + hhand.getNumFoldsSinceLastBet();

                // devalue raises against small number of players
                adjustOutcome(OUTCOME_CALL, FACTOR_PLAYERS_LEFT, (10-numPlayersWhenRaised) * 0.01f);

                // boost raises with players left to act
                switch (lastRaiser.getStartingPositionCategory())
                {
                    case PokerAI.POSITION_EARLY:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.07f);
                        break;
                    case PokerAI.POSITION_MIDDLE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.04f);
                        break;
                    case PokerAI.POSITION_LATE:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.02f);
                        break;
                    case PokerAI.POSITION_SMALL:
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.01f);
                        break;
                }

                if ((player.getSteam() > 0.1) && (xTilt > 0))
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, player.getSteam() * xTilt * 0.05f);
                }
            }

            // consider stealing if no pot action, not already raising,
            // and enough chips to raise at least one big blind
            if ((getStrongestOutcome() != OUTCOME_OPEN_POT) &&
                (potStatus == PokerConstants.NO_POT_ACTION) &&
                (playerChips - amountToCall >= bigBlindAmount))
            {
                setEligible(OUTCOME_STEAL, (playerChips > amountToCall));

                adjustOutcome(OUTCOME_STEAL, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.65f);
                adjustOutcome(OUTCOME_STEAL, FACTOR_AGGRESSION, xBasicsAggression);

                int stealBiasIndex =
                    (int)Math.round((0.5f - ((xStealBlinds > 0.5f) ? (xStealBlinds - 0.5f) : xStealBlinds)) / 0.05d);

                float stealBiasValue = SimpleBias.getBiasValue(stealBiasIndex, hand);

                if (((xStealBlinds >= 0.5f) && (stealBiasValue < handStrength)) ||
                    ((xStealBlinds < 0.5f) && (stealBiasValue > handStrength)))
                {
                    adjustOutcome(OUTCOME_STEAL, FACTOR_BLIND_STEALING,
                            1.5f * (stealBiasValue - handStrength) * Math.abs(xStealBlinds - 0.5f));
                }

                switch (startingPosition)
                {
                    case PokerAI.POSITION_EARLY:
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, -0.4f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_MIDDLE:
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, -0.2f * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_LATE:
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, (player.isButton() ? 0.2f : 0.1f) * xBasicsPosition);
                        break;
                    case PokerAI.POSITION_SMALL:
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, -0.15f * xBasicsPosition);
                        break;
                }

                if (bored > 0)
                {
                    adjustOutcome(OUTCOME_STEAL, FACTOR_BOREDOM, bored * 0.1f);
                }

                float xDeceptionBluffStealBlinds =
                        player.getStratFactor("deception.bluff.steal_blinds", 0.0f, 2.0f);

                // make adjustment to steal score for short stacked blinds
                // make adjustment to steal score for loose/tight blinds

                if ((player.getSteam() > 0.1) && (xTilt > 0))
                {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, player.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER LIMPING

            if (((potStatus == PokerConstants.NO_POT_ACTION) || (potStatus == PokerConstants.CALLED_POT)) &&
                    (strongestOutcome_ == OUTCOME_FOLD))
            {
                setEligible(OUTCOME_LIMP, true);

                // re-apply tightness adjustment to get more limping
                adjustOutcome(OUTCOME_LIMP, FACTOR_HAND_SELECTION, Math.min(1.0f + (adjustedHandStrength+foldStrengthDelta - 0.70f) / 2.0f, 1.0f));

                switch (startingPosition)
                {
                    case PokerAI.POSITION_MIDDLE:
                        adjustOutcome(OUTCOME_LIMP, FACTOR_POSITION, xBasicsPosition * 0.02f);
                        break;
                    case PokerAI.POSITION_LATE:
                        adjustOutcome(OUTCOME_LIMP, FACTOR_POSITION, xBasicsPosition * 0.05f);
                        break;
                }

                adjustOutcome(OUTCOME_LIMP, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);

                if (hand.isConnectors(Card.TWO, Card.KING))
                {
                    if (hand.isSuited())
                    {
                        adjustOutcome(OUTCOME_LIMP, FACTOR_IMPLIED_ODDS, potOdds * xBasicsPotOdds * hand.getHighestRank() * 0.0025f);
                    }
                    else
                    {
                        adjustOutcome(OUTCOME_LIMP, FACTOR_IMPLIED_ODDS, potOdds * xBasicsPotOdds * hand.getHighestRank() * 0.00225f);
                    }
                }

                if (bored > 0)
                {
                    adjustOutcome(OUTCOME_LIMP, FACTOR_BOREDOM, bored * 0.2f);
                }
            }
        }

            /*
            adjustOutcome(OUTCOME_RAISE, FACTOR_PLAYERS_DEALT, 0.1d * (PokerTable.SEATS - numPlayers) * handStrength);

            adjustOutcome(OUTCOME_LIMP, FACTOR_TIGHTNESS, tightnessAdjustment);
            adjustOutcome(OUTCOME_CALL, FACTOR_TIGHTNESS, tightnessAdjustment);
            adjustOutcome(OUTCOME_RAISE, FACTOR_TIGHTNESS, tightnessAdjustment);

            adjustOutcome(OUTCOME_LIMP, FACTOR_LIMP_TENDENCY, 0.3d * xDisciplineLimp);

            //adjustOutcome(OUTCOME_FOLD, FACTOR_TIGHTNESS, xBasicsTightness);

            adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);

            adjustOutcome(OUTCOME_LIMP, FACTOR_TABLE_AGGRESSION, maPotRaised_ * -0.02d);

            adjustOutcome(OUTCOME_LIMP, FACTOR_POT_ODDS, potOdds * 0.01);
            adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, potOdds * 0.01);

            if (stealSuspicion_ > 0.0f)
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_STEAL_SUSPECTED, stealSuspicion_);
                adjustOutcome(OUTCOME_RAISE, FACTOR_STEAL_SUSPECTED, stealSuspicion_);
            }

            if (player.isPotRaised())
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_RAISED, xBasicsBoldness * 0.5f);
                adjustOutcome(OUTCOME_STEAL, FACTOR_POT_RAISED, xBasicsBoldness);
                adjustOutcome(OUTCOME_RAISE, FACTOR_POT_RAISED, xBasicsBoldness * 0.75d);
            }
            */

            //adjustOutcome(OUTCOME_RAISE, FACTOR_LAST_TO_ACT, nLeftToAct == 0 ? 0.05d : 0.0f);
            //adjustOutcome(OUTCOME_STEAL, FACTOR_LAST_TO_ACT, nLeftToAct == 0 ? 0.1d : 0.0f);

            /*
            float xDeceptionBluffBully = player.getStratFactor("deception.bluff.bully", -0.5f, 1.0f);
            float xDeceptionBluffBuyPot = player.getStratFactor("deception.bluff.buypot", -1.0f, 1.0f);

            // less inclined to raise with large raise to call
            // more inclined to limp with higher pot odds (more callers)
            // more inclined to call raise with higher pot odds
            // less inclined to call large raise
            // more inclined to limp when fewer players left to act
            // more inclined to steal when blinds are small compared to stack
            // more inclined to steal blinds with stronger hands
            // more inclined to steal when fewer players left to act
            // more inclined to limp when blinds are small compared to stack
            // more inclined to steal when big blind is short stacked
            // adjust for blind stealing tendency
            // more inclined to protect blinds with stronger hands
            // less inclined to protect blinds with large raise to call
            // more inclined to buy the pot with stronger hands
            // more inclined to buy the pot when fewer players left to act
            // more inclined to buy the pot when blinds are small compared to stack
            // more inclined to buy the pot when big blind is short stacked
            // more inclined to buy the pot with stronger hands
            // more inclined to buy the pot when fewer players left to act
            */
    }

    private void setPreFlopBetRange(V2Player player)
    {
        if ((strongestOutcome_ != OUTCOME_ALL_IN) && (player.getRemainingAverageHohM() < 10.0f))
        {
            betRange_ = new BetRange(BetRange.POT_SIZE, 0.50f, 0.75f);
        }
        else switch (strongestOutcome_)
        {
            case OUTCOME_OPEN_POT:
                switch (player.getStartingPositionCategory())
                {
                    case PokerAI.POSITION_EARLY:
                        betRange_ = new BetRange(BetRange.BIG_BLIND, 2.5f, 3.0f);
                        break;
                    case PokerAI.POSITION_MIDDLE:
                        betRange_ = new BetRange(BetRange.BIG_BLIND, 3.0f, 3.5f);
                        break;
                    case PokerAI.POSITION_LATE:
                        betRange_ = new BetRange(BetRange.BIG_BLIND, 3.5f, 4.0f);
                        break;
                    case PokerAI.POSITION_SMALL:
                        betRange_ = new BetRange(BetRange.BIG_BLIND, 3.0f, 3.0f);
                        break;
                    default:
                        betRange_ = new BetRange(BetRange.BIG_BLIND, 3.0f, 5.0f);
                        break;
                }
                break;
            case OUTCOME_STEAL:
                betRange_ = new BetRange(BetRange.BIG_BLIND, 2.0f, 4.0f);
                break;
            case OUTCOME_ALL_IN:
                betRange_ = new BetRange(BetRange.ALL_IN);
                break;
            case OUTCOME_RAISE:
                betRange_ = new BetRange(BetRange.BIG_BLIND, 3.0f, 4.5f);
                break;
        }

        if (outcome_ != null)
        {
            outcome_.setBetRange(betRange_, null);
        }
    }

    private float getLimpFactor(V2Player player, Hand hand)
    {
        float xDisciplineLimp = 0.0f;

        // limping with small pairs

        if (hand.isPair() && (hand.getCard(0).getRank() <= 6))
        {
            xDisciplineLimp = player.getStratFactor("discipline.limp.small_pair", -1.0f, 1.0f);
        }

        // this case comes before connectors because A2 and AK aren't considered connectors here

        else if ((hand.getCard(0).getRank() == Card.ACE) ||
                 (hand.getCard(1).getRank() == Card.ACE))
        {
            if (hand.isSuited())
            {
                xDisciplineLimp = player.getStratFactor("discipline.limp.suited_ace", -1.0f, 1.0f);
            }
            else
            {
                xDisciplineLimp = player.getStratFactor("discipline.limp.unsuited_ace", -1.0f, 1.0f);
            }
        }

        // limping with connectors

        else if (Math.abs(hand.getCard(0).getRank() - hand.getCard(1).getRank()) == 1)
        {
            if (hand.isSuited())
            {
                xDisciplineLimp = player.getStratFactor("discipline.limp.suited_connectors", -1.0f, 1.0f);
            }
            else
            {
                xDisciplineLimp = player.getStratFactor("discipline.limp.unsuited_connectors", -1.0f, 1.0f);
            }
        }
        else
        {
            xDisciplineLimp = player.getStratFactor("discipline.limp.other", -1.0f, 1.0f);
        }
        return xDisciplineLimp;
    }

    public static boolean matrix = false;

    private void executeFlopTurn(V2Player player)
    {
        /*
        System.out.println("Board quality   0% for " + player.getCommunity() + " : " + player.getBoardQuality(0.0));
        System.out.println("Board quality  10% for " + player.getCommunity() + " : " + player.getBoardQuality(0.1));
        System.out.println("Board quality  20% for " + player.getCommunity() + " : " + player.getBoardQuality(0.2));
        System.out.println("Board quality  30% for " + player.getCommunity() + " : " + player.getBoardQuality(0.3));
        System.out.println("Board quality  40% for " + player.getCommunity() + " : " + player.getBoardQuality(0.4));
        System.out.println("Board quality  50% for " + player.getCommunity() + " : " + player.getBoardQuality(0.5));
        System.out.println("Board quality  60% for " + player.getCommunity() + " : " + player.getBoardQuality(0.6));
        System.out.println("Board quality  70% for " + player.getCommunity() + " : " + player.getBoardQuality(0.7));
        System.out.println("Board quality  80% for " + player.getCommunity() + " : " + player.getBoardQuality(0.8));
        System.out.println("Board quality  90% for " + player.getCommunity() + " : " + player.getBoardQuality(0.9));
        System.out.println("Board quality 100% for " + player.getCommunity() + " : " + player.getBoardQuality(1.0));
        */

        float xBasicsPosition = player.getStratFactor("basics.position", 0.0f, 2.0f);
        float xBasicsPotOdds = player.getStratFactor("basics.pot_odds_call", 0.0f, 2.0f);
        float xBasicsAggression = player.getStratFactor("basics.aggression", -1.0f, +1.0f);
        float xBasicsObservation = player.getStratFactor("basics.observation", 0.0f, 2.0f);
        float xTilt = player.getStratFactor("discipline.tilt", 0.0f, 1.0f);

        HoldemHand hhand = player.getPokerPlayer().getHoldemHand();

        Hand community = player.getCommunity();
        Hand pocket = player.getHand();

        int round = player.getRound();
        int startingPosition = player.getStartingPositionCategory();

        int numWithCards = hhand.getNumWithCards();
        int potStatus = player.getPotStatus();
        int amountToCall = player.getAmountToCall();
        int potTotal = player.getTotalPotAmount();
        int playerChips = player.getChipCount();

        float potOdds = amountToCall > 0 ? (float)potTotal / (float)amountToCall : 0;
        float rhs = player.getRawHandStrength();
        float bhs = player.getBiasedHandStrength();
        float drawPotential = player.getBiasedPositivePotential();
        float outdrawRisk = (float)Math.pow(1.0 + player.getBiasedNegativePotential(), numWithCards - 1) - 1.0f;

        float ehs = player.getBiasedEffectiveHandStrength(xBasicsPotOdds * potOdds);

        HandPotential potential = new HandPotential(pocket, community);

        int pNutFlush = potential.getHandCount(HandPotential.NUT_FLUSH, 0);
        int pNonNutFlush = potential.getHandCount(HandPotential.FLUSH, 0) - pNutFlush;
        int pNutStraight = potential.getHandCount(HandPotential.NUT_STRAIGHT, 0);
        int pNonNutStraight = potential.getHandCount(HandPotential.NON_NUT_STRAIGHT, 0);

        float xStraightDraw =
                    player.getStratFactor("draws.straight.nut", -1.0f, 1.0f) * pNutStraight +
                    player.getStratFactor("draws.straight.non_nut", -1.0f, 1.0f)  * 0.5f * pNonNutStraight;

        float xFlushDraw =
            player.getStratFactor("draws.flush.nut", -1.0f, 1.0f) * pNutFlush +
            player.getStratFactor("draws.flush.non_nut", -1.0f, 1.0f) * 0.5f * pNonNutFlush;

        if (community.hasQuads() || (community.hasTrips() && community.hasPair()))
        {
            xStraightDraw *= 0.0f;
            xFlushDraw *= 0.0f;
        }
        else if (community.hasPossibleFlush())
        {
            xFlushDraw *= 0.75d;

            if (community.hasFlush())
            {
                xStraightDraw = 0.0f;
            }
            else
            {
                xStraightDraw *= 0.5f;
            }
        }

        int roundsNoAction = 0;

        switch (round)
        {
            case HoldemHand.ROUND_RIVER:
                if (!hhand.getWasPotAction(HoldemHand.ROUND_TURN))
                {
                    ++roundsNoAction;
                }
                else
                {
                    break;
                }
            case HoldemHand.ROUND_TURN:
                if (!hhand.getWasPotAction(HoldemHand.ROUND_FLOP))
                {
                    ++roundsNoAction;
                }
                break;
        }

        int handsToBB = player.getHandsBeforeBigBlind();

        if (player.debugEnabled())
        {
            player.appendDebug("Raw Tilt Factor: " + xTilt + "<br>");
            player.appendDebug("Raw Hand Strength: " + rhs + "<br>");
            player.appendDebug("Biased Hand Strength: " + bhs + "<br>");
            player.appendDebug("Effective Hand Strength: " + ehs + "<br>");
            player.appendDebug("Positive Potential: " + player.getPositiveHandPotential() + "<br>");
            player.appendDebug("Biased Positive Potential: " + player.getBiasedPositivePotential() + "<br>");
            player.appendDebug("Negative Potential: " + player.getNegativeHandPotential() + "<br>");
            player.appendDebug("Biased Negative Potential: " + player.getBiasedNegativePotential() + "<br>");
            player.appendDebug("Outdraw Risk: " + outdrawRisk + "<br>");
            //player.appendDebug("Biased Effective Hand Strength: " + ehsA + "<br>");
            //player.appendDebug("Position Adjustment: " + adjPosition + "<br>");
            //player.appendDebug("Position Factor: " + xBasicsPosition + "<br>");
            player.appendDebug("Pot Total: " + potTotal + "<br>");
            player.appendDebug("Amount To Call: " + amountToCall + "<br>");
            player.appendDebug("Pot Odds: " + potOdds + "<br>");
            player.appendDebug("Pot Odds Factor: " + xBasicsPotOdds + "<br>");
            player.appendDebug("Straight Draw Factor: " + xStraightDraw + "<br>");
            player.appendDebug("Flush Draw Factor: " + xFlushDraw + "<br>");
            player.appendDebug("Aggression Factor: " + xBasicsAggression + "<br>");
            player.appendDebug("Preceding post-flop rounds w/ no action: " + roundsNoAction + "<br>");
            player.appendDebug("Hands Before Big Blind: " + handsToBB + "<br>");
        }

        // default bet range 1/2 to whole pot
        betRange_ = new BetRange(BetRange.POT_SIZE, 0.5f, 1.0f);

        /*
        float averageCheckRate = 0.0f;

        for (int i = hhand.getNumPlayers()-1; i >= 0; --i)
        {
            PokerPlayer opponent = hhand.getPlayerAt(i);
            if (opponent == player.getPokerPlayer()) continue;
            if (opponent.isFolded()) continue;
            OpponentModel om = player.getOpponentModel(opponent.getSeat());
            float v;
            switch (round)
            {
                case HoldemHand.ROUND_FLOP:
                    v = om.checkFoldFlop.getWeightedPercentTrue(0.8f) * om.actFlop.getWeightedPercentTrue(0.2f);
                    averageCheckRate += v;
                    break;
                case HoldemHand.ROUND_TURN:
                    v = om.checkFoldTurn.getWeightedPercentTrue(0.8f) * om.actTurn.getWeightedPercentTrue(0.2f);
                    averageCheckRate += v;
                    break;
                case HoldemHand.ROUND_RIVER:
                    v = om.checkFoldRiver.getWeightedPercentTrue(0.8f) * om.actRiver.getWeightedPercentTrue(0.2f);
                    averageCheckRate += v;
                    break;
            }
        }

        averageCheckRate /= (numWithCards - 1);
        */

        if ((round == HoldemHand.ROUND_RIVER) && (potStatus != PokerConstants.NO_POT_ACTION) && (rhs == 1.0f))
        {
            if (playerChips > amountToCall)
            {
                float allInPotRatio = (float)Math.ceil((float)(playerChips - amountToCall) / (float)potTotal);

                // *always* raise/reraise on the river with the pure nuts
                betRange_ = new BetRange(BetRange.POT_SIZE, (float)(Math.min(0.5f, allInPotRatio)), allInPotRatio);

                adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            }
            else
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            }

            return;
        }

        if (potStatus == PokerConstants.NO_POT_ACTION)
        {
            // initialize default action
            setEligible(OUTCOME_CHECK, true);
            adjustOutcome(OUTCOME_CHECK, FACTOR_DEFAULT, 1.0f);

            float allWeak = 1.0f;

            // of remaining players, compute overall likelihood none has a calling hand
            for (int i = hhand.getNumPlayers()-1; i >= 0; --i)
            {
                PokerPlayer p = hhand.getPlayerAt(i);
                if (p.getSeat() == player.getSeatNumber()-1) continue;
                if (p.isFolded()) continue;
                if (hhand.getLastActionThisRound(p) == HandAction.ACTION_CHECK) continue;
                allWeak *= p.getOpponentModel().getCheckFoldPostFlop(round, 0.5f);
            }

            if (player.debugEnabled())
            {
                player.appendDebug("All Weak Probability: " + allWeak + "<br>");
            }

            // CONSIDER WHETHER WE CAN OPEN THE BETTING FOR STRENGTH

            setEligible(OUTCOME_BET, true);

            // boost for first action
            adjustOutcome(OUTCOME_BET, FACTOR_FIRST_ACTION, 0.25f);
            // penalty for players left to act
            adjustOutcome(OUTCOME_BET, FACTOR_LEFT_TO_ACT, -0.08f * player.getNumAfter() * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_BET, FACTOR_RAW_HAND_STRENGTH, rhs*rhs + 0.05f);
            adjustOutcome(OUTCOME_BET, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
            adjustOutcome(OUTCOME_BET, FACTOR_HAND_POTENTIAL, drawPotential);
            adjustOutcome(OUTCOME_BET, FACTOR_OUTDRAW_RISK, outdrawRisk);
            adjustOutcome(OUTCOME_BET, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);
            adjustOutcome(OUTCOME_BET, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);

            // if everyone is likely to be weak, boost bet value, but less for very strong
            // hands where we might rather check/call or check-raise
            if ((round == HoldemHand.ROUND_FLOP) || (roundsNoAction > 0))
            {
                adjustOutcome(OUTCOME_BET, FACTOR_STEAL_POTENTIAL, xBasicsObservation * (1.0f-rhs*rhs) * allWeak * 0.50f);
            }

            // adjust for tilt
            if ((player.getSteam() > 0.1) && (xTilt > 0))
            {
                adjustOutcome(OUTCOME_BET, FACTOR_STEAM, player.getSteam() * xTilt * 0.1f);
            }

            if ((getStrongestOutcome() == OUTCOME_BET) && (player.getNumAfter() > 0))
            {
                // CONSIDER CHECK-RAISE AND SLOW-PLAY

                setEligible(OUTCOME_CHECK_RAISE, true);

                // boost for players left to act
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_LEFT_TO_ACT, 0.06f * player.getNumAfter() * xBasicsPosition * (1.0f-allWeak));

                // base hand strength scores
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_RAW_HAND_STRENGTH, rhs*rhs*rhs + 0.10f);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_STEAL_POTENTIAL, xBasicsObservation * allWeak * -0.15f);

                // penalty for outdraw risk
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            }
            else
            {
                // CONSIDER WHETHER WE SHOULD MAKE A CONTINUATION BET

                // TODO: don't make continuation bets when we have good draws and would rather see a free card
                // TODO: don't make continuation bets into very scary flops
                // e.g. AhKhJd when holding 88
                /*
                community.hasPair();
                community.containsRank(Card.ACE);
                community.containsRank(Card.KING);
                community.containsRank(Card.QUEEN);
                community.containsRank(Card.JACK);
                */

                // consider continuation bet on the flop if last raiser pre-flop
                if (player.wasLastRaiserPreFlop() && (numWithCards < 4) && (round == HoldemHand.ROUND_FLOP))
                {
                    setEligible(OUTCOME_CONTINUATION_BET, true);

                    int draws =
                            pNutFlush*3 + pNonNutFlush +
                            pNutStraight*2 + pNonNutStraight;

                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_RAW_HAND_STRENGTH, rhs*rhs + 0.05f);
                    //adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_HAND_POTENTIAL, drawPotential - 0.01f * draws);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_ACTION, 0.35f);

                    // stronger if first raiser pre-flop
                    if (player.wasFirstRaiserPreFlop())
                    {
                        // even stronger if raised in-between first and last raise
                        if (player.wasOnlyRaiserPreFlop())
                        {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_ONLY_PRE_FLOP_RAISER, 0.15f);
                        }
                        else
                        {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_PRE_FLOP_RAISER, 0.17f);
                        }
                    }
                    else
                    {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_LAST_PRE_FLOP_RAISER, 0.05f);
                    }

                    if (player.wasFirstRaiserPreFlop())
                    {
                        // stronger the earlier position we opened from
                        switch (startingPosition)
                        {
                            case PokerAI.POSITION_EARLY:
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.1f);
                                break;
                            case PokerAI.POSITION_MIDDLE:
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.05f);
                                break;
                        }
                    }

                    // weaker if two opponents
                    if (numWithCards == 3)
                    {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PLAYERS_LEFT, -0.1f);
                    }

                    // stronger the larger our raises were?

                    if (strongestOutcome_ == OUTCOME_CONTINUATION_BET)
                    {
                        betRange_ = new BetRange(BetRange.POT_SIZE, 0.4f, 0.7f);
                    }
                }
                else
                {
                    // consider probe bet if original raiser has checked
                    PokerPlayer firstRaiser = hhand.getFirstBettor(HoldemHand.ROUND_PRE_FLOP, true);

                    if (firstRaiser != null)
                    {
                        HandAction firstRaiserAction = hhand.getFirstVoluntaryAction(firstRaiser, round);

                        if ((firstRaiserAction != null) && (firstRaiserAction.getAction() == HandAction.ACTION_CHECK))
                        {
                            probeBet_ = true;

                            betRange_ = new BetRange(BetRange.POT_SIZE, 0.3f, 0.5f);

                            adjustOutcome(OUTCOME_BET, FACTOR_PROBE_BET, rhs * 0.1f);
                        }
                    }
                }
            }
        }
        else // pot action already
        {
            float xrhs;
            float xbhs;

            setEligible(OUTCOME_FOLD, true);
            adjustOutcome(OUTCOME_FOLD, FACTOR_DEFAULT, 1.0f);

            setEligible(OUTCOME_CALL, true);
            setEligible(OUTCOME_RAISE, amountToCall < playerChips);

            PokerPlayer bettor = hhand.getFirstBettor(round, false);
            OpponentModel bettorModel = bettor.getOpponentModel();

            // CONSIDER WHETHER WE CAN CALL

            xrhs = (float)Math.sin((rhs - 0.5f) * Math.PI) * 0.5f + 0.50f;
            xbhs = (float)Math.sin((bhs - 0.5f) * Math.PI) * 0.5f + 0.50f;

            float bettorActFrequency = 0.0f;
            float bettorOpenFrequency = 0.0f;
            float bettorRaiseFrequency = 0.0f;
            float bettorCheckFoldFrequency = 0.0f;

            switch (round)
            {
                case HoldemHand.ROUND_FLOP:
                    bettorActFrequency =
                        bettorModel.actFlop.getWeightedPercentTrue(0.1f);
                    bettorCheckFoldFrequency =
                        bettorModel.checkFoldFlop.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorOpenFrequency =
                        bettorModel.openFlop.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorRaiseFrequency =
                        bettorModel.raiseFlop.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    break;
                case HoldemHand.ROUND_TURN:
                    bettorActFrequency =
                        bettorModel.actTurn.getWeightedPercentTrue(0.1f);
                    bettorCheckFoldFrequency =
                        bettorModel.checkFoldTurn.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorOpenFrequency =
                        bettorModel.openTurn.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorRaiseFrequency =
                        bettorModel.raiseTurn.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    break;
                case HoldemHand.ROUND_RIVER:
                    bettorActFrequency =
                        bettorModel.actRiver.getWeightedPercentTrue(0.1f);
                    bettorCheckFoldFrequency =
                        bettorModel.checkFoldRiver.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorOpenFrequency =
                        bettorModel.openRiver.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    bettorRaiseFrequency =
                        bettorModel.raiseRiver.getWeightedPercentTrue(0.5f) *
                        bettorActFrequency;
                    break;
            }

            float bettorOverbetFrequency = bettorModel.handsOverbetPotPostFlop.getWeightedPercentTrue(0.5f);
            float bettorBetFoldFrequency = bettorModel.handsBetFoldPostFlop.getWeightedPercentTrue(0.5f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_CALL, FACTOR_LEFT_TO_ACT, -0.08f * player.getNumAfter() * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, xrhs + 0.10f);
            adjustOutcome(OUTCOME_CALL, FACTOR_BIASED_HAND_STRENGTH, xbhs - xrhs);
            adjustOutcome(OUTCOME_CALL, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            adjustOutcome(OUTCOME_CALL, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);
            //adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, adjPosition);
            adjustOutcome(OUTCOME_CALL, FACTOR_STRAIGHT_DRAW, xStraightDraw * 0.075f);
            adjustOutcome(OUTCOME_CALL, FACTOR_FLUSH_DRAW, xFlushDraw * 0.05f);
            if (round < HoldemHand.ROUND_RIVER)
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_POTENTIAL, drawPotential * xBasicsPotOdds * (potOdds+1.0f) / 2);
            }
            else
            {
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (potOdds + 1.0f) * xBasicsPotOdds * ehs / 2);
            }
            adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL, -0.15f * (float)Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - ehs));
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_BET_FREQUENCY, xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_OVERBET_FREQUENCY, xBasicsObservation * bettorOverbetFrequency * 0.10f);

            // CONSIDER WHETHER WE CAN RAISE / RE-RAISE

            xrhs = (rhs+0.05f)*(rhs+0.05f);
            xbhs = (bhs+0.05f)*(bhs+0.05f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_RAISE, FACTOR_LEFT_TO_ACT, -0.05f * player.getNumAfter() * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, xrhs + 0.10f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_BIASED_HAND_STRENGTH, xbhs - xrhs);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OUTDRAW_RISK, outdrawRisk);
            adjustOutcome(OUTCOME_RAISE, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);
            //adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, adjPosition);
            adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);
            //adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);
            if (round < HoldemHand.ROUND_RIVER)
            {
                adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_POTENTIAL, drawPotential * xBasicsPotOdds * (potOdds + 1.0f) / 2);
            }
            else
            {
                adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, (potOdds + 1.0f) * xBasicsPotOdds * ehs / 2);
            }
            adjustOutcome(OUTCOME_RAISE, FACTOR_BET_TO_CALL, -0.15f * (float)Math.pow(amountToCall / playerChips, 2) * (1.0f - ehs));
            //adjustOutcome(OUTCOME_RAISE, FACTOR_LAST_TO_ACT, nAfter == 0 ? 0.05d : 0.0f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FREQUENCY, xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_OVERBET_FREQUENCY, xBasicsObservation * bettorOverbetFrequency * 0.10f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FOLD_FREQUENCY, xBasicsObservation * bettorBetFoldFrequency * 0.10f);
        }

        /*
        if (matrix)
        {
            float xxx = ehsA;

            //System.out.println(xxx);
            betRange_ = new BetRange(BetRange.ALL_IN);
            if (xxx < 0.60) strongestOutcome_ = OUTCOME_FOLD;
            else if (xxx < 0.70) strongestOutcome_ = OUTCOME_CHECK_RAISE;
            else if (xxx < 0.80) strongestOutcome_ = OUTCOME_LIMP;
            else if (xxx < 0.85) strongestOutcome_ = OUTCOME_CALL;
            else if (xxx < 0.95) strongestOutcome_ = OUTCOME_STEAL;
            else strongestOutcome_ = OUTCOME_BET;

            return;
        }

        float xTournamentStackSize = player.getStratFactor("tournament.stack_size", 0.0f, 2.0f);
        float xTournamentOpponentStackSize = player.getStratFactor("tournament.opponent_stack_size", 0.0f, 2.0f);

        int position = player.getPostFlopPositionCategory();
        float adjPosition = 0.0f;

        int nAfter = player.getPlayersLeftToAct();
        int nBefore = player.getPlayersBefore();

        adjPosition = nBefore * 0.05d - nAfter * 0.1d;

        switch (position)
        {
            case PokerAI.POSITION_EARLY:
                adjPosition = -0.1d * xBasicsPosition;
                break;
            case PokerAI.POSITION_MIDDLE:
                adjPosition = 0.0f * xBasicsPosition;
                break;
            case PokerAI.POSITION_LATE:
                adjPosition = 0.1d * xBasicsPosition;
                break;
            case PokerAI.POSITION_LAST:
                adjPosition = 0.2d * xBasicsPosition;
                break;
        }

        //adjustOutcome(OUTCOME_CHECK, FACTOR_BIASED_HAND_STRENGTH, rhs-bhs);
        //adjustOutcome(OUTCOME_FOLD, FACTOR_BIASED_HAND_STRENGTH, rhs-bhs);

        if (player.isPotReraised())
        {
            adjustOutcome(OUTCOME_RAISE, FACTOR_POT_RERAISED, (bhs-1.0f));
        }
        else if (player.isPotRaised())
        {
            adjustOutcome(OUTCOME_RAISE, FACTOR_POT_RAISED, -0.4d * amountToCall / player.getTotalPotAmount());
            //adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, xBasicsPotOdds * Math.max(potOdds - 1.0f, 0.0f) * 0.1);
        }
        */
    }

    private void adjustOutcome(int outcome, int factor, float delta)
    {
        adjustOutcome(outcome, factor, CURVE_LINEAR, false, 1.0f, 0.0f, 0.0f, delta);
    }

    private void adjustOutcome(int outcome, int factor, int curve, boolean invert, float weight, float min, float max, float value)
    {
        if (Float.isNaN(value))
        {
            if (DebugConfig.isTestingOn())
            {
                throw new ApplicationError("NaN in value!");
            }
            else
            {
                logger.warn("NaN in value!");
            }
        }
        else if (Float.isNaN(weight))
        {
            if (DebugConfig.isTestingOn())
            {
                throw new ApplicationError("NaN in weight!");
            }
            else
            {
                logger.warn("NaN in weight!");
            }
            return;
        }
        if (eligible_[outcome])
        {
            OutcomeAdjustment adjustment = new OutcomeAdjustment
                    (outcome, factor, curve, invert, weight, min, max, value);

            score_[outcome] += adjustment.fx * weight;
            weights_[outcome] += weight;
            addAdjustment(outcome, adjustment);

            /*
            System.out.println("Factor " + factorNames_.get(factor) + " applied to outcome " +
                               outcomeNames_.get(outcome) + " (" +
                               min + " - " + value + " - " + max + ") x=" + adjustment.x +
                               " curve " + curveNames_.get(curve) +
                               (invert ? " (inverted) " : "") +
                               " f(x)=" + adjustment.fx +
                               " weight " + weight);
                               */
        }
    }

    private void determineStrongestOutcome()
    {
        float strongestScore = 0.0f;

        float score;

        for (int i = 0; i < score_.length; ++i)
        {
            if (eligible_[i] && ((int)(score_[i] * 100) > 0))
            {
                score = score_[i]; // / adjustments_[i].size(); // weights_[i];

                if (score >= strongestScore)
                {
                    strongestOutcome_ = i;
                    strongestScore = score;
                }
            }
        }
    }

    public AIOutcome getOutcome()
    {
        return outcome_;
    }

    public PlayerAction getAction()
    {
        String reason = getDebug(strongestOutcome_);

        switch (strongestOutcome_)
        {
            case OUTCOME_CHECK:
            case OUTCOME_CHECK_RAISE:
                return PlayerAction.check().reason(reason);
            case OUTCOME_FOLD:
                return PlayerAction.fold().reason(reason);
            case OUTCOME_LIMP:
            case OUTCOME_CALL:
            case OUTCOME_SLOW_PLAY:
                return PlayerAction.call().reason(reason);
            case OUTCOME_ALL_IN:
            case OUTCOME_OPEN_POT:
            case OUTCOME_BET:
            case OUTCOME_CONTINUATION_BET:
            case OUTCOME_STEAL:
            case OUTCOME_RAISE:
            case OUTCOME_SEMI_BLUFF:
            case OUTCOME_TRAP:
            case OUTCOME_BLUFF:
                return PlayerAction.raise().reason(reason);
        }

        System.out.println("Unhandled outcome " + strongestOutcome_);
        
        return PlayerAction.fold().reason("default");
    }

    private void addAdjustment(int outcome, OutcomeAdjustment adjustment)
    {
        adjustment.next = adjustments_[outcome][adjustment.factor];

        adjustments_[outcome][adjustment.factor] = adjustment;

        determineStrongestOutcome();
    }

    private String getDebug(int outcome)
    {
        // for now, just store outcome name
        return (outcome != OUTCOME_NONE) ? (String)outcomeNames_.get(outcome) : null;
    }

    private class OutcomeAdjustment
    {
        OutcomeAdjustment next = null;

        public int outcome;
        public int factor;
        public int curve;
        public boolean invert;
        public float weight;
        public float min;
        public float max;
        public float value;
        public float x;
        public float fx;

        private String display_ = null;

        public OutcomeAdjustment
            (int outcome, int factor, int curve, boolean invert, float weight, float min, float max, float value)
        {
            this.outcome = outcome;
            this.factor = factor;
            this.curve = curve;
            this.invert = invert;
            this.weight = weight;
            this.min = min;
            this.max = max;
            this.value = value;

            if (min != max)
            {
                if (value > max)
                {
                    value = max;
                }
                else if (value < min)
                {
                    value = min;
                }

                x = (value - min) / (max - min);
            }
            else
            {
                x = value;
            }

            fx = (float)Math.pow(x, curve);

            if (invert)
            {
                fx = 1.0f - fx;
            }
        }

        public String getDisplay()
        {
            Object[] oParams = null;

            if (display_ == null)
            {
                switch (factor)
                {
                    case FACTOR_HAND_SELECTION :
                    case FACTOR_LEFT_TO_ACT:
                    case FACTOR_POSITION:
                        break;
                }

                display_ = PropertyConfig.getMessage
                        ("msg.aireason." + factorNames_.get(factor), oParams);
            }

            return display_;
        }
    }

    public boolean isBetting()
    {
        switch(getAction().getType())
        {
            case HandAction.ACTION_BET:
            case HandAction.ACTION_RAISE:
                return true;
            default:
                return false;
        }
    }

    public String getResultsTable()
    {
            StringBuilder buf = new StringBuilder();

            buf.append("<TABLE CELLSPACING=\"0\" CELLPADDING=\"2\" BORDER=\"1\">");

            buf.append("<TR><TD>&nbsp;</TD>");

            for (int outcome = 0; outcome < outcomeNames_.size(); ++outcome)
            {
                if (!isEligible(outcome)) continue;

                buf.append("<TD WIDTH=\"60\" ALIGN=\"CENTER\"><B>");
                buf.append(getOutcomeLabel(outcome));
                buf.append("</B></TD>");
            }

            buf.append("</TR>");

            for (int factor = 0; factor < factorNames_.size(); ++factor)
            {
                int position = buf.length();
                boolean bVal = false;

                buf.append("<TR><TD><B>");
                buf.append(getFactorLabel(factor));
                buf.append("</B></TD>");
                for (int outcome = 0; outcome < outcomeNames_.size(); ++outcome)
                {
                    if (!isEligible(outcome)) continue;

                    OutcomeAdjustment adjustment = adjustments_[outcome][factor];

                    buf.append("<TD ALIGN=\"RIGHT\">");

                    float sum = 0.0f;

                    int count = 0;

                    while (adjustment != null)
                    {
                        float diff = adjustment.fx * adjustment.weight;
                        if (Math.abs(diff) >= 0.01d)
                        {
                            if (diff > 0.0f)
                            {
                                buf.append("+");
                            }
                            buf.append((int) (diff * 100.0));
                            sum += diff;
                            ++count;
                            bVal = true;
                        }
                        adjustment = adjustment.next;
                        if ((Math.abs(diff) >= 0.01d) && (adjustment != null))
                        {
                            buf.append("<br>");
                        }
                    }

                    if (count == 0)
                    {
                        buf.append("&nbsp;");
                    }
                    else if (count > 1)
                    {
                        buf.append("<hr>");
                        if (sum > 0)
                        {
                            buf.append("+");
                        }
                        buf.append((int)(sum*100.0));
                    }
/*
                    else
                    {
                        buf.append("&nbsp;");
                    }
*/
                    buf.append("</TD>");
                }
                buf.append("</TR>");

                if (!bVal)
                {
                    buf.setLength(position);
                }
            }

            buf.append("<TR><TD>&nbsp;</TD>");

            for (int outcome = 0; outcome < outcomeNames_.size(); ++outcome)
            {
                if (!isEligible(outcome)) continue;

                int score = (int) (score_[outcome] * 100.0);

                String color = (outcome == strongestOutcome_) ? "#44CC44" :
                        (score < 0) ? "#CC4444" : "#4444CC";

                buf.append("<TD ALIGN=\"RIGHT\">");
                buf.append("<FONT COLOR=\">");
                buf.append(color);
                buf.append("\">");
                if (score > 0)
                {
                    buf.append("+");
                }
                buf.append(score);
                buf.append("</FONT>");
                buf.append("</TD>");
            }

            buf.append("</TR>");

            buf.append("</TABLE>");

            return buf.toString();
    }

    public String toHTML(PokerPlayer player, boolean bVerbose, boolean bBreak)
    {
        if (bVerbose)
        {
            PlayerType playerType = player.getPlayerType();

            boolean isLimitGame = player.getHoldemHand().isLimit();
            boolean canCheck = player.getPokerAI().getAmountToCall() == 0;

            if (outcome_ == null)
            return PropertyConfig.getMessage("msg.advisor.verbose",
                    Utils.encodeHTML(playerType.getName()),
                    (bBreak ? "<br>" : " "),
                    (strongestOutcome_ == OUTCOME_ALL_IN && isLimitGame) ?
                        getOutcomeLabel((String) outcomeNames_.get(canCheck ? OUTCOME_BET : OUTCOME_RAISE)) :
                        getOutcomeLabel((String) outcomeNames_.get(strongestOutcome_)),
                    (bBreak ? "<br>" : " "),
                    ((isBetting() && !isLimitGame) ?  betRange_.toString(player, bBreak) : ""));
            else
            return PropertyConfig.getMessage("msg.advisor.verbose2",
                    Utils.encodeHTML(playerType.getName()),
                    (bBreak ? "<br>" : " "),
                    outcome_.toHTML(2));
        }
        else
        {
            if (outcome_ == null)
            return PropertyConfig.getMessage("msg.advisor.brief", HandAction.getActionName(getAction().getType()));
            else
            return PropertyConfig.getMessage("msg.advisor.brief", outcome_.toHTML(5));
        }
    }

    public ArrayList getEligibleOutcomeNames()
    {
        ArrayList list = new ArrayList();

        for (int i = 0; i < outcomeNames_.size(); ++i)
        {
            if (eligible_[i])
            {
                list.add(outcomeNames_.get(i));
            }
        }

        return list;
    }

    public int getStrongestOutcome()
    {
        return strongestOutcome_;
    }

    public String getStrongestOutcomeName()
    {
        return (String) outcomeNames_.get(strongestOutcome_);
    }

    public static String getOutcomeLabel(String outcomeName)
    {
        return PropertyConfig.getMessage("msg.aioutcome." + outcomeName);
    }

    public static String getOutcomeLabel(int outcome)
    {
        return getOutcomeLabel((String) outcomeNames_.get(outcome));
    }

    public static String getFactorLabel(String factorName)
    {
        return PropertyConfig.getMessage("msg.aireason." + factorName);
    }

    private static String getFactorLabel(int factor)
    {
        return getFactorLabel((String)factorNames_.get(factor));
    }

    public BetRange getBetRange()
    {
        return betRange_;
    }

    public boolean isProbeBetAppropriate()
    {
        return probeBet_;
    }

    public String getReasoningHTML()
    {
        // sort outcomes
        ArrayList outcomes = new ArrayList();

        for (int i = 0; i < outcomeNames_.size(); ++i)
        {
            // if (i == strongestOutcome_) continue;

            if (!eligible_[i]) continue;

            if (score_[i] == 0) continue;

            int index;

            for (index = 0; index < outcomes.size(); ++index)
            {
                if (score_[(Integer) outcomes.get(index)] < score_[i]) break;
            }

            outcomes.add(index, i);
        }

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < outcomes.size(); ++i)
        {
            buf.append(getReasoningHTML((Integer) outcomes.get(i)));
            buf.append("<br><br>");
        }

        /*
        buf.append(getReasoningHTML(strongestOutcome_));

        for (int i = 0; i < outcomeNames_.size(); ++i)
        {
            if (i == strongestOutcome_) continue;

            if (!eligible_[i]) continue;

            if (score_[i] == 0) continue;

            buf.append("<br><br>");
            buf.append(getReasoningHTML(i));
        }
        */

        return buf.toString();
    }

    public String getReasoningHTML(int outcome)
    {
        StringBuilder buf = new StringBuilder();

        // figure out order (simple insertion sort)
        ArrayList adjustments = new ArrayList();

        for (int i = 0; i < factorNames_.size(); ++i)
        {
            // if (i == FACTOR_DEFAULT) continue;

            OutcomeAdjustment adjustment = (OutcomeAdjustment)adjustments_[outcome][i];
            OutcomeAdjustment head = adjustment;

            float total = 0.0f;

            while (adjustment != null)
            {
                total += adjustment.weight * adjustment.fx;
                adjustment = adjustment.next;
            }

            if (Math.abs(total) < 0.01d) continue;

            int index;

            for (index = 0; index < adjustments.size(); index+=2)
            {
                if ((Float) adjustments.get(index + 1) < total)
                {
                    break;
                }
            }

            adjustments.add(index, total);
            adjustments.add(index, i);
        }

        if (!adjustments.isEmpty())
        {
            buf.append(PropertyConfig.getMessage("msg.aireason.heading", getOutcomeLabel(outcome),
                    getStrengthColor(score_[outcome] - 0.5f)));
        }

        for (int i = 0; i < adjustments.size(); i +=2)
        {
            int factor = (Integer) (adjustments.get(i));
            float value = (Float) (adjustments.get(i + 1));

            String display = PropertyConfig.getMessage
                        ("msg.aireason." + factorNames_.get(factor));

            if (display != null)
            {
                buf.append("<br>&nbsp;&nbsp;-&nbsp;<font color=");
                buf.append(getStrengthColor(value));
                buf.append(">");
                buf.append(display);
                buf.append("</font>\n");
            }
        }

        //System.out.println(buf.toString());
        return buf.toString();
    }

    public static String getStrengthColor(float value)
    {
        int index;

        if (value > 0.5f)
        {
            index = 5;
        }
        else if (value > 0.25)
        {
            index = 4;
        }
        else if (value > 0.0f)
        {
            index = 3;
        }
        else if (value > -0.25d)
        {
            index = 2;
        }
        else if (value > -0.5f)
        {
            index = 1;
        }
        else
        {
            index = 0;
        }

        return PropertyConfig.getMessage("msg.strengthcolor." + index);
    }

    private void logResults(V2Player player)
    {
        logger.debug("RuleEngine results for " + player.getPokerPlayer().getName());

        for (int outcome = 0; outcome < outcomeNames_.size(); ++outcome)
        {
            if (!isEligible(outcome)) continue;

            logger.debug(getOutcomeLabel(outcome));

            for (int factor = 0; factor < factorNames_.size(); ++factor)
            {
                boolean bVal = false;

                OutcomeAdjustment adjustment = adjustments_[outcome][factor];

                float sum = 0.0f;

                while (adjustment != null)
                {
                    float diff = adjustment.fx * adjustment.weight;

                    if (Math.abs(diff) >= 0.01d)
                    {
                        sum += diff;
                        bVal = true;
                    }
                    adjustment = adjustment.next;
                }
                if ((sum * 100.0) > 0.0f)
                {
                    logger.debug("    " + getFactorLabel(factor) + " +" + (int) (sum * 100.0));
                }
                else if ((sum * 100.0) < 0.0f)
                {
                    logger.debug("    " + getFactorLabel(factor) + " " + (int) (sum * 100.0));
                }
            }

            int score = (int) (score_[outcome] * 100.0);

            if (score >= 0)
            {
                logger.debug("==> +" + score);
            }
            else
            {
                logger.debug("==> " + score);
            }
        }
    }
}

