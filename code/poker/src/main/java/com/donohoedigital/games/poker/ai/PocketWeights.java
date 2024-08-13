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

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.log4j.*;

import java.util.*;

/**
 * Reusable computation of relative likelihood of pocket hands given hand history.
 * <p/>
 * TODO: reincorporate noise/accuracy
 */
public class PocketWeights
{
    static Logger logger = Logger.getLogger(PocketWeights.class);

    // string representing outcome-affecting state
    private String signature_ = null;

    private HoldemHand hhand_ = null;

    private PocketMatrixFloat weights_[] = null;

    private float apparentStrength_[] = new float[10];
    private int callCount_;
    private int raiseCount_;
    private int potSize_;

    private int bookmark_ = 0;

    private static HandSelectionScheme scheme = null;

    /**
     * PocketWeights is a singleton class (constructor is private) that reviews the action so far
     * in the hand to build a weight matrices reflecting the likelihood of each remaining player
     * having a given pocket hand.  The single instance is returned only by PocketWeights.getInstance()
     * to enforce the singleton model.  When a call is made to PocketWeights.getInstance() with a different
     * instance of HoldemHand, or after an event that might change the outcomes, the results are recomputed,
     * otherwise results are reused.
     *
     * @param hhand The hand in play.
     * @return An instance of PocketWeights for the current state of the specified hand.
     */
    public static PocketWeights getInstance(HoldemHand hhand)
    {
        if (hhand == null)
        {
            throw new ApplicationError("PocketWeights.getInstance() called with null hand.");
        }

        PocketWeights pw = hhand.getPocketWeights();

        String signature =
                Long.toString(hhand.getCommunity().fingerprint()) + ":" +
                Integer.toString(hhand.getHistorySize()) + ":" +
                Integer.toString(hhand.getRound());

        if ((pw == null) || !signature.equals(pw.signature_))
        {
            pw = new PocketWeights(hhand);
            pw.signature_ = signature;
            hhand.setPocketWeights(pw);
        }

        return pw;
    }

    /**
     * Returns the weight table for a particular seat, for display or further computation.
     *
     * @param seat
     * @return weight table for the specified seat.
     */
    public PocketMatrixFloat getWeightTable(int seat)
    {
        return weights_[seat];
    }

    public float getApparentStrength(int seat)
    {
        return apparentStrength_[seat];
    }

    public float getBiasedRawHandStrength(int seat)
    {
        return getBiasedRawHandStrength(seat, -1);
    }

    public float getBiasedRawHandStrength(int seat, int opponentSeat)
    {
        PokerTable table = hhand_.getTable();
        PokerPlayer player = table.getPlayer(seat);

        if (player == null) return 0f;

        Hand hand = player.getHand();

        if (hand == null) return 0f;

        if (hand.containsCard(Card.BLANK)) return 0f;

        if (opponentSeat < 0)
        {
            float v = 1.0f;

            for (opponentSeat = 0; opponentSeat < 10; ++opponentSeat)
            {
                PokerPlayer opp = hhand_.getTable().getPlayer(opponentSeat);
                if ((opponentSeat != seat) && (opp != null) && !opp.isFolded())
                {
                    v *= getBiasedRawHandStrength(seat, opponentSeat);
                }
            }

            return v;
        }

        Hand community = hhand_.getCommunity();

        if (community == null) return 0.0f;

        boolean preflop = (community.size() == 0);

        PocketRanks ranks = null;

        float strength = 0.0f;

        if (preflop)
        {
            strength = SimpleBias.getBiasValue(2, hand);
        }
        else
        {
            ranks = PocketRanks.getInstance(community);

            strength = ranks.getRawHandStrength(hand);
        }

        float best = 0.0f;
        float notbest = 0.0f;

        for (int i = 1; i < 52; ++i)
        {
            if (hand.containsCard(i) || community.containsCard(i)) continue;

            for (int j = 0; j < i; ++j)
            {
                if (hand.containsCard(j) || community.containsCard(j)) continue;

                float weight = weights_[opponentSeat].get(i, j);

                float ohs = 0.0f;

                if (preflop)
                {
                    ohs = SimpleBias.getBiasValue(2, Card.getCard(i), Card.getCard(j));
                }
                else
                {
                    ohs = ranks.getRawHandStrength(i, j);
                }

                if (!Float.isNaN(ohs))
                {
                    float v = ohs * weight;

                    if (ohs <= strength)
                    {
                        best += v;
                    }
                    else
                    {
                        notbest += v;
                    }
                }
            }
        }

        return best / (notbest + best);
    }

    public float getBiasedEffectiveHandStrength(int seat)
    {
        return getBiasedEffectiveHandStrength(seat, -1);
    }

    public float getBiasedEffectiveHandStrength(int seat, int opponentSeat)
    {
        Hand community = hhand_.getCommunity();

        if (community.size() == 0)
        {
            throw new ApplicationError("PocketWeights.getBiasedEffectiveHandStrength() called before the flop.");
        }

        if (community.size() == 5)
        {
            throw new ApplicationError("PocketWeights.getBiasedEffectiveHandStrength() called after the river.");
        }

        if (opponentSeat < 0)
        {
            float v = 1.0f;

            for (opponentSeat = 0; opponentSeat < 10; ++opponentSeat)
            {
                PokerPlayer opp = hhand_.getTable().getPlayer(opponentSeat);
                if ((opponentSeat != seat) && (opp != null) && !opp.isFolded())
                {
                    v *= getBiasedEffectiveHandStrength(seat, opponentSeat);
                }
            }

            return v;
        }

        Hand hand = hhand_.getTable().getPlayer(seat).getHand();

        if (community == null) return 0.0f;

        PocketOdds odds = PocketOdds.getInstance(community, hand);

        float total = 0.0f;
        float divisor = 0.0f;

        for (int i = 1; i < 52; ++i)
        {
            if (hand.containsCard(i) || community.containsCard(i)) continue;

            for (int j = 0; j < i; ++j)
            {
                if (hand.containsCard(j) || community.containsCard(j)) continue;

                float weight = weights_[opponentSeat].get(i, j);
                float strength = odds.getEffectiveHandStrength(i, j);

                total += weight * strength;
                divisor += weight;
            }
        }

        return total / divisor;
    }

    private PocketWeights(HoldemHand hhand)
    {
        if (scheme == null)
        {
            scheme = HandSelectionScheme.getByName("Phil Gordon - Average Full");
        }
        if (scheme == null)
        {
            scheme = HandSelectionScheme.getByName("Full Table");
        }

        hhand_ = hhand;

        process();
    }

    private void process()
    {
        if (processHistory())
        {
            computeStrengths();
            balanceWeights();
        }
    }

    private void computeStrengths()
    {
        Hand community = hhand_.getCommunity();

        boolean bPreFlop = community.size() == 0;

        PocketRanks ranks = null;

        if (!bPreFlop)
        {
            ranks = PocketRanks.getInstance(community);
        }

        for (int seat = 0; seat < 10; ++seat)
        {
            float sum = 0.0f;
            float div = 0.0f;

            PocketMatrixFloat weights = weights_[seat];

            for (int i = 1; i < 52; ++i)
            {
                for (int j = 0; j < i; ++j)
                {
                    float v = weights.get(i, j);
                    float s;

                    if (!Float.isNaN(v))
                    {
                        if (bPreFlop)
                        {
                            s = SimpleBias.getBiasValue(4, Card.getCard(i), Card.getCard(j));
                        }
                        else
                        {
                            s = ranks.getRawHandStrength(i, j);
                        }
                        sum += v * s;
                        div += v;
                    }
                }
            }

            apparentStrength_[seat] = (sum / div);
        }
    }

    private boolean processHistory()
    {
        List<HandAction> hist = hhand_.getHistoryCopy();

        boolean changed = false;

        if (bookmark_ == 0)
        {
            if (weights_ == null)
            {
                weights_ = new PocketMatrixFloat[10];
            }

            for (int i = 0; i < 10; ++i)
            {
                if (weights_[i] == null)
                {
                    weights_[i] = new PocketMatrixFloat(1.0f);
                }
                else
                {
                    weights_[i].clear(1.0f);
                }
            }

            callCount_ = 0;
            raiseCount_ = 0;
            potSize_ = 0;
            changed = true;
        }

        HandAction action;

        while (bookmark_ < hist.size())
        {
            action = hist.get(bookmark_++);

            if (action.getRound() > HoldemHand.ROUND_RIVER) break;

            if (action.getRound() < HoldemHand.ROUND_FLOP)
            {
                processPreFlopAction(action);

                switch (action.getAction())
                {
                    case HandAction.ACTION_CALL:
                        ++callCount_;
                        break;
                    case HandAction.ACTION_RAISE:
                        ++raiseCount_;
                        callCount_ = 0;
                        break;
                }

                changed = true;
            }
            else
            {
                processPostFlopAction(new Hand(hhand_.getCommunity(), action.getRound() + 2), action);

                changed = true;
            }
        }

        return changed;
    }

    private class PreFlopActor
    {
        public Tuple compute(PokerPlayer player, int card1, int card2, float handStrength, Tuple tuple)
        {
            OpponentModel model = player.getOpponentModel();

            int startingPosition = player.getStartingPositionCategory();
            float tightness = model.getPreFlopTightness(startingPosition, .5f);
            //float aggression = 2*model.getPreFlopAggression(startingPosition, .5f);

            int tightnessBiasIndex =
                    Math.round((0.5f - ((tightness > 0.5f) ? (tightness - 0.5f) : tightness)) / 0.05f);

            float tightnessBiasStrength = SimpleBias.getBiasValue(tightnessBiasIndex, card1, card2);

            float foldStrengthDelta = 0.0f;

            if (((tightness >= 0.5f) && (tightnessBiasStrength < handStrength)) ||
                ((tightness < 0.5f) && (tightnessBiasStrength > handStrength)))
            {
                foldStrengthDelta = (tightnessBiasStrength - handStrength) * Math.abs(tightness - 0.5f) * 2;
            }

            float adjustedHandStrength = handStrength + foldStrengthDelta;

            int startingOrder = player.getPokerAI().getStartingOrder();

            int numPlayers = player.getPokerAI().getNumPlayers();

            float positionAdjustment =
                    (float) (startingOrder + PokerConstants.SEATS - numPlayers) / PokerConstants.SEATS;

            float threatAdjustment = 1f - adjustedHandStrength;

            if (raiseCount_ > 0)
            {
                threatAdjustment *= raiseCount_ * -.2f + callCount_ * -.4f;
            }
            else
            {
                threatAdjustment *= callCount_ * -.1f;
            }

            if (adjustedHandStrength + positionAdjustment + threatAdjustment * 10 >= 1f)
            {
                tuple.call = adjustedHandStrength;

                tuple.betRaise = adjustedHandStrength;

                tuple.checkFold = 1f - tuple.call;
            }
            else
            {
                tuple.call = 0f;
                tuple.betRaise = 0f;
                tuple.checkFold = 1f;
            }

            return tuple;
        }
    }

    private final class Tuple
    {
        public float checkFold;
        public float call;
        public float betRaise;
    }

    private interface PreFlopWeightFunction
    {
        public float adjustWeight(PokerPlayer player, int card1, int card2, float weight, float rhs);
    }

    private PreFlopActor preflopActor = new PreFlopActor();
    private Tuple tuple = new Tuple();

    private void processPreFlopAction(HandAction action)
    {
        int actionType = action.getAction();

        PokerPlayer player = action.getPlayer();

        PreFlopWeightFunction func = null;

        //float bias = 1.0f;

        int amount = action.getAmount();

        switch (actionType)
        {
            case HandAction.ACTION_ANTE:
            case HandAction.ACTION_BLIND_BIG:
            case HandAction.ACTION_BLIND_SM:
            case HandAction.ACTION_LOSE:
            case HandAction.ACTION_OVERBET:
            case HandAction.ACTION_WIN:
                return;
            case HandAction.ACTION_FOLD:
            case HandAction.ACTION_CHECK:
                func = new PreFlopWeightFunction()
                {
                    public float adjustWeight(PokerPlayer player, int card1, int card2, float weight, float rhs)
                    {
                        return preflopActor.compute(player, card1, card2, rhs, tuple).checkFold;
                        /*
                        if (callCount_ > 0 || raiseCount_ > 0)
                        {
                            return (float)(Math.pow(1.0f-rhs, callCount_))*weight;
                        }
                        else
                        {
                            return preflopActor.compute(player, card1, card2, rhs, tuple).checkFold * (1f-rhs);
                        }
                        */
                    }
                };
                break;
            case HandAction.ACTION_CALL:
                //bias = 1.0f / ((((float)potSize_) / ((float)amount)) + 1.0f);
                func = new PreFlopWeightFunction()
                {
                    public float adjustWeight(PokerPlayer player, int card1, int card2, float weight, float rhs)
                    {
                        return preflopActor.compute(player, card1, card2, rhs, tuple).call;
                        /*
                        if (callCount_ > 0 || raiseCount_ > 0)
                        {
                            return (float)(Math.pow(rhs, raiseCount_ * 2 + callCount_ + 1)*weight);
                        }
                        else
                        {
                            return preflopActor.compute(player, card1, card2, rhs, tuple).call * rhs;
                        }
                        */
                    }
                };
                break;
            case HandAction.ACTION_BET:
            case HandAction.ACTION_RAISE:
                //bias = ((float)amount) / ((float)potSize_);
                func = new PreFlopWeightFunction()
                {
                    public float adjustWeight(PokerPlayer player, int card1, int card2, float weight, float rhs)
                    {
                        return preflopActor.compute(player, card1, card2, rhs, tuple).betRaise;
                        /*
                        if (callCount_ > 0 || raiseCount_ > 0)
                        {
                            return (float)(Math.pow(rhs, raiseCount_ * 2 + callCount_ + 2)*weight);
                        }
                        else
                        {
                            return preflopActor.compute(player, card1, card2, rhs, tuple).betRaise * rhs;
                        }
                        */
                    }
                };
                break;
            default:
                throw new ApplicationError("Unhandled action type " + actionType);
        }

        potSize_ += amount;

        PocketMatrixFloat weightTable = weights_[action.getPlayer().getSeat()];

        Card card1;
        Card card2;

        int x;
        int y;

        OpponentModel model = player.getOpponentModel();
        int position = player.getStartingPositionCategory();

        for (int i = 1; i < 52; ++i)
        {
            card1 = Card.getCard(i % 4, i / 4 + 2);

            for (int j = 0; j < i; ++j)
            {
                card2 = Card.getCard(j % 4, j / 4 + 2);

                if (card1.getRank() == card2.getRank())
                {
                    x = Card.ACE - card1.getRank();
                    y = Card.ACE - card2.getRank();
                }
                else if (card1.getSuit() == card2.getSuit())
                {
                    if (card1.getRank() > card2.getRank())
                    {
                        x = Card.ACE - card1.getRank();
                        y = Card.ACE - card2.getRank();
                    }
                    else
                    {
                        x = Card.ACE - card2.getRank();
                        y = Card.ACE - card1.getRank();
                    }
                }
                else
                {
                    if (card1.getRank() < card2.getRank())
                    {
                        x = Card.ACE - card1.getRank();
                        y = Card.ACE - card2.getRank();
                    }
                    else
                    {
                        x = Card.ACE - card2.getRank();
                        y = Card.ACE - card1.getRank();
                    }
                }

                weightTable.set(i, j, func.adjustWeight(player, i, j, weightTable.get(i, j),
                                                        scheme.getHandStrength
                                                                (Card.getCard(i), Card.getCard(j))));
//                        PreFlopBias.getOpenPotWeight(Card.getCard(i), Card.getCard(j), Math.max(position-1, PokerAI.POSITION_EARLY), .5f)));            }
            }
        }
    }

    private interface PostFlopWeightFunction
    {
        public float adjustWeight(float weight, float rhs, float ppot, float npot);
    }

    private void processPostFlopAction(Hand community, HandAction action)
    {
        PocketRanks ranks = PocketRanks.getInstance(community);

        int actionType = action.getAction();

        PostFlopWeightFunction func = null;

        float bias = 1.0f;

        int amount = action.getAmount();

        switch (actionType)
        {
            case HandAction.ACTION_ANTE:
            case HandAction.ACTION_BLIND_BIG:
            case HandAction.ACTION_BLIND_SM:
            case HandAction.ACTION_LOSE:
            case HandAction.ACTION_OVERBET:
            case HandAction.ACTION_WIN:
                return;
            case HandAction.ACTION_FOLD:
            case HandAction.ACTION_CHECK:
                func = new PostFlopWeightFunction()
                {
                    public float adjustWeight(float weight, float rhs, float ppot, float npot)
                    {
                        //return (rhs < .75f) ? (weight+1f)/2f : weight/2f;
                        //return ((float)(Math.pow((.8f-rhs)/.8f,2d))+weight)/2f;
                        //return (rhs < .8f) ? ((float)(Math.pow((.8f-rhs)/.8f,2d))+weight)/2f : 0f;
                        return (float) (0.75d + Math.sin(1.25d * Math.PI * (rhs - npot) + Math.PI / 2.0d) / 4.0d) * weight;
                        //return (1.0f-rhs)*weight;
                    }
                };
                break;
            case HandAction.ACTION_CALL:
                //bias = ((float)amount) / ((float)potSize);
                // asymptotically approaches zero with higher pot odds
                bias = 1.0f / ((((float) potSize_) / ((float) amount)) + 1.0f);
                func = new PostFlopWeightFunction()
                {
                    public float adjustWeight(float weight, float rhs, float ppot, float npot)
                    {
                        //return (rhs > .75f) ? (weight+1f)/2f : weight/2f;
                        //return (rhs > .6f) ? ((float)(Math.pow((rhs-.6f)/.4f,2d))+weight)/2f : 0f;
                        return (float) (0.35d + Math.sin(1.5d * Math.PI * ((rhs + ppot - npot) + 1.0d)) / 4.0d) * weight;
                    }
                };
                break;
            case HandAction.ACTION_BET:
                bias = ((float) amount) / ((float) potSize_);
                func = new PostFlopWeightFunction()
                {
                    public float adjustWeight(float weight, float rhs, float ppot, float npot)
                    {
                        //return (rhs > .85f) ? (weight+1f)/2f : weight/2f;
                        //return Math.min(weight+rhs*rhs, 1f);
                        //return (rhs > .85f) ? ((float)(Math.pow((rhs-.85f)/.15f,2d))+weight)/2f : 0f;
                        return (float) (Math.pow(0.9d * (rhs + npot), 2.0d) + 0.1d) * weight;
                    }
                };
                break;
            case HandAction.ACTION_RAISE:
                func = new PostFlopWeightFunction()
                {
                    public float adjustWeight(float weight, float rhs, float ppot, float npot)
                    {
                        //return (rhs > .9f) ? ((float)(Math.pow((rhs-.9f)/.1f,2d))+weight)/2f : 0f;
                        //return (rhs > .95f) ? (weight+1f)/2f : weight/2f;
                        return (float) (0.35d + Math.sin(1.5d * Math.PI * ((rhs + ppot - npot) + 1.0d)) / 4.0d) *
                               (float) (Math.pow(0.9d * (rhs + npot + ppot), 20.0d) + 0.1d) * weight;
                    }
                };
                break;
            default:
                throw new ApplicationError("Unhandled action type " + actionType);
        }

        bias = 1.0f;

        potSize_ += amount;

        PocketMatrixFloat weightTable = weights_[action.getPlayer().getSeat()];

        //float rhs = getBiasedRawHandStrength(player.getSeat(), true);

        for (int i = 1; i < 52; ++i)
        {
            if (community.containsCard(i))
            {
                // System.out.println("Community contains " + Card.getCard(i));

                for (int j = 0; j < i; ++j)
                {
                    weightTable.set(i, j, Float.NaN);
                }
            }
            else
            {
                for (int j = 0; j < i; ++j)
                {
                    if (community.containsCard(j))
                    {
                        // System.out.println("Community contains " + Card.getCard(j));
                        weightTable.set(i, j, Float.NaN);
                    }
                    else
                    {
                        float rhs = ranks.getRawHandStrength(i, j);
                        //float rhs = getBiasedRawHandStrength(player.getSeat(), true);

                        if (Float.isNaN(rhs))
                        {
                            // System.out.println("rhs is NaN for " + Card.getCard(i) + Card.getCard(j));
                            weightTable.set(i, j, Float.NaN);
                        }
                        else
                        {
                            float oldWeight = weightTable.get(i, j);

                            float newWeight = func.adjustWeight(
                                    oldWeight,
                                    ranks.getRawHandStrength(i, j),
                                    0.0f,
                                    0.0f);

                            weightTable.set(i, j, oldWeight * (1.0f - bias) + newWeight * bias);
                        }
                    }
                }
            }
        }
    }

    /**
     * Spreads out the weight values in each matrix to range from 0.0 to 1.0.
     */
    private void balanceWeights()
    {
        for (int seat = 0; seat < PokerConstants.SEATS; ++seat)
        {
            balanceWeights(seat);
        }
    }

    /**
     * Spreads out the weight values in a single matrix to range from 0.0 to 1.0.
     */
    private void balanceWeights(int seat)
    {
        // find range

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        PocketMatrixFloat weights = weights_[seat];

        for (int i = 1; i < 52; ++i)
        {
            for (int j = 0; j < i; ++j)
            {
                float v = weights.get(i, j);

                if (Float.isNaN(v)) continue;

                if (v < min) min = v;

                if (v > max) max = v;
            }
        }

        float range = max - min;

        // apply range

        if (range > 0)
        {
            for (int i = 1; i < 52; ++i)
            {
                for (int j = 0; j < i; ++j)
                {
                    weights.set(i, j, .2f + (weights.get(i, j) / max) * .8f);
                    //weights.set(i, j, (weights.get(i,j) - min) / range);
                    //weights.set(i, j, (float)Math.sqrt((weights.get(i,j) - min) / range));
                    //weights.set(i, j, 0.5f + ((weights.get(i,j) - min) / range - 0.5f) / 2.0f);
                }
            }
        }
    }
}
