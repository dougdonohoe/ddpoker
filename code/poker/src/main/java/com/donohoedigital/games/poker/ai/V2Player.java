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
package com.donohoedigital.games.poker.ai;

import com.ddpoker.holdem.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;

import java.security.*;

@DataCoder('2')
public class V2Player extends V1Player implements AIConstants
{
    private StringBuilder debug_ = null;

    RuleEngine re;

    private float ppot_;
    private float npot_;
    private float biasedPositivePotential_;
    private float biasedNegativePotential_;
    private float behs_;

    private float positivePotential_[][] = null;
    private float negativePotential_[][] = null;
    private PocketMatrixFloat fieldMatrix_ = null;

    private float rawHandStrength_;
    private float biasedHandStrength_;

    private int myHandScore_;
    private int otherHandScore_[][] = null;

    private long fpPocket_ = 0;
    private long fpCommunity_ = 0;

    private boolean potRaised_[] = new boolean[10];

    private int maPotRaised_ = 0;

    private int lastPotStatus_;

    private float stealSuspicion_;

    private float dSteam_ = 0.0f;

    static
    {
        //AISpy.setDialogVisible(true);
    }

    public void init()
    {
        super.init();

        initRuleEngine();
    }

    public RuleEngine getRuleEngine()
    {
        return re;
    }

    public void gameLoaded()
    {
        super.gameLoaded();

        // always load my own reputation
        loadReputation();

        computeOdds();
    }

    public void setPokerPlayer(PokerPlayer player)
    {
        super.setPokerPlayer(player);

        init();
        computeOdds();
    }

    public PlayerAction getAction(boolean quick)
    {
        betAmount_ = 0;

        //if (isPreFlop())
        //{
            re.execute(this);

            return re.getAction();
        /*
        }
        else
        {
            return super.getAction(quick);
        }
        */
    }

    public float getHandStrength()
    {
        if (isPreFlop())
        {
            if (getHandSelectionScheme() == null)
            {
                printMessage("Request for hand strength but no hand selection scheme set.");

                return 0.0f;
            }
            else
            {
                return getHandSelectionScheme().getHandStrength(getPocket());
            }
        }

        printMessage("Request for hand strength post-flop, not implemented.");

        return 0.0f;
    }

    public String getHandStrengthDisplay()
    {
        return PropertyConfig.getMessage("msg.handstrength." + ((int)(getHandStrength() * 100.0f))/10);
    }

    public String getPostFlopHandStrengthDisplay()
    {
        return PropertyConfig.getMessage("msg.handstrength." + ((int)(getBiasedHandStrength() * 100.0f))/10);
    }

    protected void printMessage(String sMessage)
    {
        super.printMessage("V2 AI : " + getPlayerName() + " in seat " + getSeatNumber() + " : " + sMessage);
    }

    public void newHand()
    {
        stealSuspicion_ = 0.0f;

        loadReputation();
    }

    public void dealtFlop()
    {
        int maOld_ = maPotRaised_;

        maPotRaised_ -= (potRaised_[0] ? 1 : 0);
        System.arraycopy(potRaised_, 1, potRaised_, 0, 9);

        switch (lastPotStatus_)
        {
            case PokerConstants.RAISED_POT:
            case PokerConstants.RERAISED_POT:
                potRaised_[9] = true;
                maPotRaised_ += 1;
                break;
           default:
                potRaised_[9] = false;
                break;
        }

        if (maPotRaised_ != maOld_)
        {
            //logger.info("Pre-flop raise metric changed from " + maOld_ + " to " + maPotRaised_);
        }
    }

    public void dealtTurn()
    {
    }

    public void endHand()
    {
        super.endHand();
        
        PokerPlayer player = getPokerPlayer();

        HoldemHand hhand = player.getHoldemHand();

        float badBeat = computeBadBeatScore(hhand, getPokerPlayer());

        if (badBeat > 0)
        {
            dSteam_ += Math.log(badBeat);
        }
        else
        {
            dSteam_ *= 0.5f;
        }

        saveReputation();
    }

    public void playerActed(PokerPlayer player, int action, int amount)
    {
        behs_ = -1;

        lastPotStatus_ = getPotStatus();

        HoldemHand hhand = player.getHoldemHand();

        if (isPreFlop())
        {
            if (getPotStatus() == PokerConstants.NO_POT_ACTION)
            {
                // player has opened the pot
                if (action == HandAction.ACTION_RAISE)
                {
                    // suspect steal from late position
                    // TODO: adjust for individual player behavior
                    switch (getStartingPositionCategory(player))
                    {
                        case PokerAI.POSITION_MIDDLE:
                            stealSuspicion_ = 0.03f;
                            break;
                        case PokerAI.POSITION_LATE:
                            stealSuspicion_ = (player.getSeat() == player.getTable().getButton()) ? 0.10f : 0.08f;
                            break;
                        case PokerAI.POSITION_SMALL:
                            stealSuspicion_ = 0.12f;
                            break;
                    }
                }
            }
            else
            {
                switch (action)
                {
                    case HandAction.ACTION_RAISE:
                        // raiser has been re-raised already
                        stealSuspicion_ = 0.0f;
                        break;
                    case HandAction.ACTION_CALL:
                        if ((float)player.getHoldemHand().getTotalPotChipCount() / (float)amount < 3.0)
                        {
                            // raiser has been called with weak pot odds
                            stealSuspicion_ = 0.0f;
                        }
                        else
                        {
                            // reduce adjustment with successive callers
                            stealSuspicion_ /= 2.0f;
                        }
                        break;
                }
            }
        }
        else if ((action == HandAction.ACTION_BET) || (action == HandAction.ACTION_RAISE))
        {
            //System.out.println("amount="+amount+",pot="+hhand.getTotalPotChipCount());
            if (amount > hhand.getTotalPotChipCount() / 2)
            {
                player.getOpponentModel().overbetPotPostFlop = true;
                //System.out.println("overbet");
            }
        }
    }

    public PocketMatrixFloat getFieldMatrixArray()
    {
        if (fieldMatrix_ == null)
        {
            fieldMatrix_ = new PocketMatrixFloat(0.0f);
        }

        return fieldMatrix_;
    }

    private int[][] getOtherHandScoreArray()
    {
        if (otherHandScore_ == null)
        {
            otherHandScore_ = new int[52][52];
        }

        return otherHandScore_;
    }

    public void computeOdds()
    {
        PokerPlayer player = getPokerPlayer();

        HoldemHand hhand = player.getHoldemHand();
        if (hhand == null || player.isFolded() || player.isAllIn() || hhand.isAllInShowdown())
        {
            return;
        }

        Hand hole = getPocket();

        // if pocket is null, this player was not in this hand; probably moved from another table
        if (hole == null)
        {
            return;
        }

        Hand community = new Hand(getCommunity()); // copy because we will be altering

        if (community.size() == 0)
        {
            return;
        }

        if ((fpPocket_ == hole.fingerprint()) &&
            (fpCommunity_ == community.fingerprint()))
        {
            return;
        }

        fpPocket_ = hole.fingerprint();
        fpCommunity_ = community.fingerprint();

        //if (TESTING(EngineConstants.TESTING_PERFORMANCE)) Perf.start();
        _computeOdds(hole, community);
        //if (TESTING(EngineConstants.TESTING_PERFORMANCE)) Perf.stop();
    }

    private boolean noPotential = false;

    /**
     * actual compute logic
     */
    private void _computeOdds(Hand hole, Hand community)
    {
        float[][] positivePotential = getPositivePotential();
        float[][] negativePotential = getNegativePotential();
        int[][] otherHandScore = getOtherHandScoreArray();

        Deck deck = new Deck(false);

        deck.removeCards(hole);
        deck.removeCards(community);

        Hand hand = new Hand(Card.BLANK, Card.BLANK);

        HandInfoFaster info = new HandInfoFaster();

        PocketRanks ranks = PocketRanks.getInstance(community);

        myHandScore_ = info.getScore(hole, community);

        int count = 0;
        int win = 0;

        int ohs;

        float ppotoa = 0.0f;
        float ppotoadiv = 0.0f;
        float npotoa = 0.0f;
        float npotoadiv = 0.0f;

        /*
        int aa = 0;
        int at = 0;
        int ab = 0;
        int ta = 0;
        int tt = 0;
        int tb = 0;
        int ba = 0;
        int bt = 0;
        int bb = 0;
        */

        for (int i = 51; i >= 0; --i)
        {
            hand.setCard(0, Card.getCard(i % 4, i / 4 + 2));

            for (int j = 51; j > i; --j)
            {
                hand.setCard(1, Card.getCard(j % 4, j / 4 + 2));

                if (hole.containsAny(hand) || community.containsAny(hand))
                {
                    otherHandScore[i][j] = 0;
                    positivePotential[i][j] = 0;
                    negativePotential[i][j] = 0;
                    continue;
                }

                ohs = otherHandScore[i][j] = info.getScore(hand, community);

                if (ohs <= myHandScore_)
                {
                    ++win;
                }

                ++count;

                if ((community.size() < 5) && !noPotential)
                {
                    deck.removeCards(hand);

                    // compute 1 card hand potential

                    int total = deck.size();
                    float pdiv = 0.0f;
                    float ndiv = 0.0f;

                    Card next;

                    float ppot = 0.0f;
                    float npot = 0.0f;

                    for (int k = 0; k < total; ++k)
                    {
                        next = deck.getCard(k);

                        community.add(next);

                        //HandRanking pranking = HandRanking.getInstance(community);
                        //float phs = Math.pow(pranking.getRawHandStrength(hand), 2);
                        float phs = (float)Math.pow(ranks.getRawHandStrength(hand), 2);

                        int myNewScore = info.getScore(hole, community);
                        int otherNewScore = info.getScore(hand, community);

                        // currently better hands

                        pdiv += phs;
                        ndiv += 1.0;

                        if (ohs > myHandScore_)
                        {
                            if (myNewScore == otherNewScore)
                            {
                                // improve to tie
                                ppot += phs * 0.5;
                                pdiv -= phs * 0.5;
                                //++bt;
                            }
                            else if (myNewScore > otherNewScore)
                            {
                                // improve to win
                                ppot += phs * 1.0;
                                //++ba;
                            }
                        }

                        // currently equal hands

                        else if (ohs == myHandScore_)
                        {
                            if (myNewScore > otherNewScore)
                            {
                                // improve to better
                                ppot += phs * 0.5;
                                pdiv -= phs * 0.5;
                            }
                            else if (myNewScore < otherNewScore)
                            {
                                // weaken to worse
                                npot += 1.0f;
                            }
                            else
                            {
                                // weaken to equal
                                npot += 0.5f;
                                ndiv -= 0.5f;
                            }
                        }

                        // currently worse hands
                        else
                        {
                            if (myNewScore == otherNewScore)
                            {
                                // weaken to equal
                                npot += 0.5f;
                                ndiv -= 0.5;
                            }
                            else if (myNewScore < otherNewScore)
                            {
                                // weaken to worse
                                npot += 1.0f;
                            }
                        }

                        community.remove(next);
                    }

                    ppot = pdiv > 0 ? ppot / pdiv : 0;
                    npot = ndiv > 0 ? npot / ndiv : 0;

                    /*
                    if (hand.contains(Card.CLUBS_K) && hand.contains(Card.SPADES_K))
                    {
                        System.out.println("Against KcKs ppot=" + ppot);
                    }
                    */

                    positivePotential[i][j] = ppot;
                    negativePotential[i][j] = npot;

                    if (otherHandScore[i][j] > myHandScore_)
                    {
                        ppotoa += ppot;
                        ppotoadiv += 1.0;
                    }

                    // currently equal hands

                    else if (otherHandScore[i][j] == myHandScore_)
                    {
                        ppotoa += ppot * 0.5;
                        ppotoadiv += 0.5; // I really don't know about this, but it's how they do it
                        npotoa += npot * 0.5f;
                        npotoadiv += 0.5f;
                    }

                    // currently worse hands

                    else
                    {
                        npotoa += npot;
                        npotoadiv += 1.0f;
                    }

                    deck.addAll(hand);
                }
            }
        }

        rawHandStrength_ = (float)win / count;

        if (ppotoadiv != 0.0f)
        {
            ppot_ = ppotoa / ppotoadiv;
        }
        else
        {
            ppot_ = 0.0f;
        }

        if (npotoadiv != 0.0f)
        {
            npot_ = npotoa / npotoadiv;
        }
        else
        {
            npot_ = 0.0f;
        }

        biasedHandStrength_ = -1;
        behs_ = -1;

        //System.out.println("RPP for " + getPokerPlayer().getName() + ":" + ppot_);
        //System.out.println("RNP for " + getPokerPlayer().getName() + ":" + npot_);

        if ((community.size() == 5) || noPotential)
        {
            biasedPositivePotential_ = 0.0f;
            biasedNegativePotential_ = 0.0f;
            return;
        }

        computeBiasedPotential();
    }

    public float getRawHandStrength()
    {
        computeOdds();

        //return rawHandStrength_;

        return (float)Math.pow(rawHandStrength_, getPokerPlayer().getHoldemHand().getNumWithCards() - 1);
    }

    public float getPositiveHandPotential()
    {
        computeOdds();

        return ppot_;
    }

    public float getNegativeHandPotential()
    {
        computeOdds();

        return npot_;
    }

    public float getEffectiveHandStrength()
    {
        computeOdds();

        float handStrength = getRawHandStrength();

        //System.out.println("Unmanipulated raw hand strength: " + handStrength);

        //return handStrength;

        return (float)Math.pow(handStrength + (1 - handStrength) * getPositiveHandPotential(), getPokerPlayer().getHoldemHand().getNumWithCards() - 1);
    }

    public float getBiasedEffectiveHandStrength(float potOdds)
    {
        computeOdds();

        if (behs_ >= 0) return behs_;

        float rhs = getRawHandStrength();
        float bhs = getBiasedHandStrength();

        //float foldProbability = 0.3f;

        behs_ = (float)Math.pow(
                    Math.min(
                        bhs - rhs * getBiasedNegativePotential() +
                        Math.min((1 - bhs), getBiasedPositivePotential() * (potOdds + 1)), 1.0f
                    ),
                    getPokerPlayer().getHoldemHand().getNumWithCards() - 1);

        return behs_;
    }

    public float getBiasedPositivePotential()
    {
        computeOdds();

        return biasedPositivePotential_;
    }

    public float getBiasedNegativePotential()
    {
        computeOdds();

        return biasedNegativePotential_;
    }

    private void computeBiasedPotential()
    {
        PokerPlayer player = getPokerPlayer();

        PocketMatrixFloat fieldMatrix = updateFieldMatrix();

        float[][] positivePotential = getPositivePotential();
        float[][] negativePotential = getNegativePotential();

        //Hand hole = getPocket();
        Hand community = getCommunity();
        PocketRanks ranks = PocketRanks.getInstance(community);
        Hand hand = new Hand(Card.BLANK, Card.BLANK);
        Card card1;

        float weight;
        float bpp = 0.0f;
        float bnp = 0.0f;
        float total = 0.0f;

        int skip = player.getSeat();

        for (int i = 51; i >= 0; --i)
        {
            card1 = Card.getCard(i % 4, i / 4 + 2);

            hand.setCard(0, card1);

            for (int j = 51; j > i; --j)
            {
                card1 = Card.getCard(j % 4, j / 4 + 2);

                hand.setCard(1, card1);

                for (int seat = 0; seat < 10; ++seat)
                {
                    if (seat == skip) continue;

                    //PokerPlayer opponent = player.getTable().getPlayer(seat);

                    weight = fieldMatrix.get(i,j);

                    float rhs = ranks.getRawHandStrength(hand);

                    bpp += positivePotential[i][j] * weight * rhs;
                    bnp += negativePotential[i][j] * weight * rhs;

                    //total += weight;
                    total += weight * rhs;
                }
            }
        }

        biasedPositivePotential_ = (total > 0) ? bpp / total : 0.0f;
        biasedNegativePotential_ = (total > 0) ? bnp / total : 0.0f;

        //System.out.println("BPP for " + getPokerPlayer().getName() + ":" + biasedPositivePotential_);
        //System.out.println("BNP for " + getPokerPlayer().getName() + ":" + biasedNegativePotential_);
    }

    public float getBiasedHandStrength()
    {
        computeOdds();

        if (biasedHandStrength_ >= 0.0) return biasedHandStrength_;

        PokerPlayer player = getPokerPlayer();

//        int[][] otherHandScore = new int[52][52];
        int[][] otherHandScore = getOtherHandScoreArray();

        Hand hole = getPocket();
        Hand community = getCommunity();

        PocketRanks ranks = PocketRanks.getInstance(community);

        Hand hand = new Hand(Card.BLANK, Card.BLANK);
        Card card1;
        Card card2;

        float weight;
        float bhs[] = new float[10];
        float total[] = new float[10];

        int skip = player.getSeat();

        HoldemHand hhand = player.getHoldemHand();

        //boolean couldLimp[] = new boolean[hhand.getNumPlayers()];
        boolean paid[] = new boolean[hhand.getNumPlayers()];
        //boolean limped[] = new boolean[hhand.getNumPlayers()];

        for (int p = hhand.getNumPlayers() - 1; p >= 0; --p)
        {
            PokerPlayer opponent = hhand.getPlayerAt(p);

            //couldLimp[p] = hhand.couldLimp(opponent);
            paid[p] = hhand.paidToPlay(opponent);
            //limped[p] = couldLimp[p] && hhand.limped(opponent);
        }

        for (int i = 50; i >= 0; --i)
        {
            card1 = Card.getCard(i % 4, i / 4 + 2);

            hand.setCard(0, card1);

            for (int j = 51; j > i; --j)
            {
                card2 = Card.getCard(j % 4, j / 4 + 2);

                hand.setCard(1, card2);

                if (hole.containsAny(hand) || community.containsAny(hand)) continue;

                for (int p = hhand.getNumPlayers() - 1; p >= 0; --p)
                {
                    PokerPlayer opponent = hhand.getPlayerAt(p);

                    if ((opponent == null) || opponent.isFolded()) continue;

                    int seat = opponent.getSeat();

                    if (seat == skip) continue;

                    int x;
                    int y;

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
                        } else
                        {
                            x = Card.ACE - card2.getRank();
                            y = Card.ACE - card1.getRank();
                        }
                    }

                    float percentPaid =
                        opponent.getOpponentModel().handsPaid.getPercentTrue(0.30f);

                    int biasIndex = (int)(percentPaid * 100.0f) / 10;

                    if (paid[p])
                    {
                        weight = SimpleBias.simpleBias_[biasIndex][x][y] / 1000.0f;
                    }
                    else
                    {
                        // didn't pay, so could have any hand
                        weight = 1.0f;
                    }
                    /*
                    boolean wasRaisedPreFlop = hhand.wasRaisedPreFlop();

                    if (couldLimp[p])
                    {
                        float percentLimped =
                                handsLimped_[seat].getPercentTrue(percentPaid);
                        float percentRaised =
                                (float)Math.max(1.0f - percentLimped - handsFoldedUnraised_[seat].getPercentTrue(0.60f), 0.0f);

                        int limpIndex = (int)((percentLimped + percentRaised) * 100.0f + 4) / 10;
                        int raiseIndex = (int)(percentRaised * 100.0f + 4) / 10;

                        if (limped[p])
                        {
                            // allow for stronger hands if limped then called a raise
                            if (wasRaisedPreFlop)
                            {
                                raiseIndex = Math.max(raiseIndex-1, 0);
                            }
                            weight = (simpleBias_[limpIndex][x][y] - simpleBias_[raiseIndex][x][y]) / 1000.0f;
                        }
                        else
                        {
                            // first raiser
                            weight = simpleBias_[raiseIndex][x][y] / 1000.0f;
                        }
                    }
                    else if (paid[p])
                    {
                        int bhsIndex = (int)(percentPaid * 100.0f + 4) / 10;

                        // allow for stronger hands if player called a raise
                        if (wasRaisedPreFlop)
                        {
                            bhsIndex = Math.max(bhsIndex-2, 0);
                        }

                        weight = simpleBias_[bhsIndex][x][y] / 1000.0f;
                    }
                    else
                    {
                        // didn't pay, so could have any hand
                        weight = 1.0f;
                    }
                    */

                    //weight = getOpponentModel(seat).getHandProbabilityMatrix().getProbability(card1, card2);

                    float rhs = ranks.getRawHandStrength(hand);

                    if (otherHandScore[i][j] <= myHandScore_)
                    {
                        bhs[seat] += weight * rhs;
                    }

                    //total[seat] += weight;
                    total[seat] += weight * rhs;
                }
            }
        }

        // RETURN AVERAGE RATHER THAN PRODUCT

        float bhsoa = 0.0f;
        int count = 0;

        for (int p = hhand.getNumPlayers()-1;  p >= 0; --p)
        {
            PokerPlayer opponent = hhand.getPlayerAt(p);

            int seat = opponent.getSeat();

            if (seat == skip) continue;

            if (opponent.isFolded()) continue;

            if (total[seat] > 0)
            {
                bhsoa += bhs[seat] / total[seat];
            }

            ++count;
            //if (total[seat] > 0) bhsoa *= bhs[seat] / total[seat];
        }

        biasedHandStrength_ = bhsoa / count;

        /*
        float bhsoa = 1.0f;

        for (int seat = 0; seat < 10; ++seat)
        {
            if (seat == skip) continue;

            PokerPlayer opponent = player.getTable().getPlayer(seat);

            if ((opponent == null) || opponent.isFolded()) continue;

            if (total[seat] > 0) bhsoa *= bhs[seat] / total[seat];
        }

        biasedHandStrength_ = bhsoa;
        */

        return biasedHandStrength_;
    }

    public float[][] getPositivePotential()
    {
        if (positivePotential_ == null)
        {
            positivePotential_ = new float[52][52];
        }

        return positivePotential_;
    }

    public float[][] getNegativePotential()
    {
        if (negativePotential_ == null)
        {
            negativePotential_ = new float[52][52];
        }

        return negativePotential_;
    }

    public float getPositivePotential(Card card1, Card card2)
    {
        float[][] positivePotential = getPositivePotential();

        int i = card1.getIndex();
        int j = card2.getIndex();

        if (j > i)
        {
            return positivePotential[i][j];
        }
        else
        {
            return positivePotential[j][i];
        }
    }

    private PocketMatrixFloat updateFieldMatrix()
    {
        PokerPlayer player = getPokerPlayer();

        PocketMatrixFloat fieldMatrix = getFieldMatrixArray();

        int skip = player.getSeat();

        HoldemHand hhand = player.getHoldemHand();

        boolean couldLimp[] = new boolean[hhand.getNumPlayers()];
        boolean paid[] = new boolean[hhand.getNumPlayers()];
        boolean limped[] = new boolean[hhand.getNumPlayers()];

        for (int p = hhand.getNumPlayers() - 1; p >= 0; --p)
        {
            PokerPlayer opponent = hhand.getPlayerAt(p);

            couldLimp[p] = hhand.couldLimp(opponent);
            paid[p] = hhand.paidToPlay(opponent);
            limped[p] = couldLimp[p] && hhand.limped(opponent);
        }

        int numPlayers = hhand.getNumPlayers();

        float max;

        Card card1;
        Card card2;

        float weight;

        for (int i = 51; i >= 0; --i)
        {
            card1 = Card.getCard(i % 4, i / 4 + 2);

            for (int j = 51; j > i; --j)
            {
                card2 = Card.getCard(j % 4, j / 4 + 2);

                max = 0.0f;

                for (int p = 0; p < numPlayers; ++p)
                {
                    PokerPlayer opponent = hhand.getPlayerAt(p);

                    int seat = opponent.getSeat();

                    if (seat == skip) continue;

                    if (opponent.isFolded()) continue;

                    int x;
                    int y;

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
                        } else
                        {
                            x = Card.ACE - card2.getRank();
                            y = Card.ACE - card1.getRank();
                        }
                    }

                    float percentPaid =
                        opponent.getOpponentModel().handsPaid.getPercentTrue(0.30f);

                    boolean wasRaisedPreFlop = hhand.wasRaisedPreFlop();

                    if (couldLimp[p])
                    {
                        float percentLimped =
                                opponent.getOpponentModel().handsLimped.getPercentTrue(percentPaid);
                        float percentRaised =
                                Math.max(1.0f - percentLimped - opponent.getOpponentModel().handsFoldedUnraised.getPercentTrue(0.60f), 0.0f);

                        int limpIndex = (int)((percentLimped + percentRaised) * 100.0f + 4) / 10;
                        int raiseIndex = (int)(percentRaised * 100.0f + 4) / 10;

                        if (limped[p])
                        {
                            // allow for stronger hands if limped then called a raise
                            if (wasRaisedPreFlop)
                            {
                                raiseIndex = Math.max(raiseIndex-1, 0);
                            }
                            weight = (SimpleBias.simpleBias_[limpIndex][x][y] - SimpleBias.simpleBias_[raiseIndex][x][y]) / 1000.0f;
                        }
                        else
                        {
                            // first raiser
                            weight = SimpleBias.simpleBias_[raiseIndex][x][y] / 1000.0f;
                        }
                    }
                    else if (paid[p])
                    {
                        int bhsIndex = (int)(percentPaid * 100.0f + 4) / 10;

                        // allow for stronger hands if player called a raise
                        if (wasRaisedPreFlop)
                        {
                            bhsIndex = Math.max(bhsIndex-2, 0);
                        }

                        weight = SimpleBias.simpleBias_[bhsIndex][x][y] / 1000.0f;
                    }
                    else
                    {
                        // didn't pay, so could have any hand
                        weight = 1.0f;
                    }

                    max = Math.max(max, weight);
                }

                fieldMatrix.set(i, j, max);
            }
        }

        return fieldMatrix;
    }

    public float getImmediatePotOdds()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().getPotOdds(player) / 100.0f;
    }

    public HandSelectionScheme getHandSelectionScheme()
    {
        HandSelectionScheme scheme = null;

        switch (getNumPlayers())
        {
            case 10:
            case 9:
            case 8:
            case 7:
                scheme = playerType_.getHandSelectionFull();
                break;
            case 6:
            case 5:
                scheme = playerType_.getHandSelectionShort();
                break;
            case 4:
            case 3:
                scheme = playerType_.getHandSelectionVeryShort();
                break;
            case 2:
                scheme = playerType_.getHandSelectionHup();
                break;
        }

        // printMessage("Using selection scheme " + scheme.getName());

        return scheme;
    }

    public int getBetAmount()
    {
        BetRange range = re.getBetRange();

        return range.chooseBetAmount(getPokerPlayer()) + getAmountToCall();
    }

    public void marshal(MsgState state, TokenizedList list)
    {
        DMTypedHashMap map = getMap();

        map.setInteger("maPotRaised", maPotRaised_);
        map.setInteger("lastPotStatus", lastPotStatus_);
        for (int i = 0; i < potRaised_.length; ++i)
        {
            map.setBoolean("potRaised" + i, potRaised_[i]);
        }

        map.setDouble("stealSuspicion", (double) stealSuspicion_);
        map.setDouble("steam", (double) dSteam_);

        super.marshal(state, list);
    }

    public void demarshal(MsgState state, TokenizedList list)
    {
        super.demarshal(state, list);

        DMTypedHashMap map = getMap();

        maPotRaised_ = map.getInteger("maPotRaised", 0);
        lastPotStatus_ = map.getInteger("lastPotStatus", 0);
        for (int i = 0; i < potRaised_.length; ++i)
        {
            potRaised_[i] = map.getBoolean("potRaised" + i, false);
        }

        stealSuspicion_ = (float)map.getDouble("stealSuspicion", 0.0f);
        dSteam_ = (float)map.getDouble("steam", 0.0d);
    }

    private void initRuleEngine()
    {
        re = new RuleEngine();
    }

    public void playerAdded(int seat)
    {
    }

    /**
     * Used by advisor info dialog.
     */
    public String getSituationHTML()
    {
        StringBuilder buf = new StringBuilder();

        PokerPlayer player = getPokerPlayer();
        HoldemHand hhand = player.getHoldemHand();

        int potStatus = getPotStatus();
        int nLeftToAct = hhand.getNumAfter(player);
        int nLeftWithCards = hhand.getNumWithCards();
        int nLeftWithChips = hhand.getNumWithChips();

        if (potStatus == PokerConstants.NO_POT_ACTION)
        {
            buf.append("First to act in ");
        }
        else if (nLeftToAct == 0)
        {
            buf.append("Last to act in ");
        }
        else
        {
            buf.append("In ");
        }

        switch (hhand.getRound())
        {
            case HoldemHand.ROUND_PRE_FLOP:
                buf.append(getStartingPositionDisplay()).append(" position with a ")
                .append(getHandStrengthDisplay()).append(" hand, will act ")
                .append(getPostFlopPositionDisplay()).append(" after the flop.<br><br>");
                break;
            case HoldemHand.ROUND_FLOP:
            case HoldemHand.ROUND_TURN:
            case HoldemHand.ROUND_RIVER:
                buf.append(getPostFlopPositionDisplay()).append(" position with a ")
                .append(getPostFlopHandStrengthDisplay()).append(" hand.<br><br>");
                break;
        }

        switch (potStatus)
        {
            case PokerConstants.RERAISED_POT:
                buf.append("Pot has been Re-Raised.<br><br>");
                break;
            case PokerConstants.RAISED_POT:
                buf.append("Pot has been Raised.<br><br>");
                break;
            case PokerConstants.CALLED_POT:
                buf.append("Big Blind has been called by ");
                buf.append(getNumLimpers());
                buf.append(" limper(s).<br><br>");
                break;
        }

        buf.append(nLeftWithCards);
        buf.append(" players left");
        if (nLeftWithCards != nLeftWithChips)
        {
            buf.append(" (");
            buf.append(nLeftWithCards - nLeftWithChips);
            buf.append(" All In)");
        }
        if (nLeftToAct > 0)
        {
            buf.append(", ");
            buf.append(nLeftToAct);
            buf.append(" left to act.");
        }
        buf.append("<br><br>");
        if (re.isProbeBetAppropriate())
        {
            buf.append("A probe bet is appropriate here.<br><br>");
        }

        return buf.toString();
    }

    /**
     * Used by advisor info dialog.
     */
    public String getFactorsHTML()
    {
        StringBuilder buf = new StringBuilder();

        buf.append(re.getReasoningHTML());

        return buf.toString();
    }

    /**
     * Used by advisor info dialog.
     */
    public String getPlayersInfoHTML()
    {
        StringBuilder buf = new StringBuilder();

        HoldemHand hhand = getPokerPlayer().getHoldemHand();

        int nNumPlayers = hhand.getNumPlayers();

        buf.append("<table cellpadding=\"4\">");

        buf.append("<tr>");
        buf.append("<td>&nbsp;</td>");
        buf.append("<td>&nbsp;</td>");
        buf.append("<td colspan=\"4\" align=\"center\"><b>Pre-Flop</b><hr></td>");
        buf.append("<td colspan=\"4\" align=\"center\"><b>Flop</b><hr></td>");
        buf.append("<td colspan=\"4\" align=\"center\"><b>Turn</b><hr></td>");
        buf.append("<td colspan=\"4\" align=\"center\"><b>River</b><hr></td>");
        buf.append("</tr>");

        buf.append("<tr>");
        buf.append("<td>Player</td>");
        buf.append("<td align=\"center\"><b>Hands</b><hr></td>");

        buf.append("<td align=\"center\">P</td>");
        buf.append("<td align=\"center\">L</td>");
        buf.append("<td align=\"center\">P</td>");
        buf.append("<td align=\"center\">R</td>");

        buf.append("<td align=\"center\">A</td>");
        buf.append("<td align=\"center\">C</td>");
        buf.append("<td align=\"center\">O</td>");
        buf.append("<td align=\"center\">R</td>");

        buf.append("<td align=\"center\">A</td>");
        buf.append("<td align=\"center\">C</td>");
        buf.append("<td align=\"center\">O</td>");
        buf.append("<td align=\"center\">R</td>");

        buf.append("<td align=\"center\">A</td>");
        buf.append("<td align=\"center\">C</td>");
        buf.append("<td align=\"center\">O</td>");
        buf.append("<td align=\"center\">R</td>");

        buf.append("<td align=\"center\">B/F</td>");
        buf.append("<td align=\"center\">OB</td>");

        buf.append("</tr>");

        for (int i = 0; i < nNumPlayers; ++i)
        {
            PokerPlayer opponent = hhand.getPlayerAt(i);

            String pre = (opponent.isFolded()) ? "" : "<font color=\"yellow\">";
            String post = (opponent.isFolded()) ? "" : "</font>";

            OpponentModel om = opponent.getOpponentModel();
            BooleanTracker pass = om.handsFoldedUnraised;
            BooleanTracker limp = om.handsLimped;
            BooleanTracker pay = om.handsPaid;

            buf.append("<tr><td>");
            buf.append(pre);
            buf.append(opponent.getName());
            if (!opponent.isHuman() && opponent.getPlayerType() != null) // null check for guest use in online games
            {
                buf.append(" (");
                buf.append(opponent.getPlayerType().getName());
                buf.append(")");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(om.getHandsPlayed());
            buf.append("</td><td align=\"center\">");
            if (pass.isReady())
            {
                buf.append((int)(pass.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (limp.isReady())
            {
                buf.append((int)(limp.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (pay.isReady())
            {
                buf.append((int)(pay.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.handsRaisedPreFlop.isReady())
            {
                buf.append((int)(om.handsRaisedPreFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.actFlop.isReady())
            {
                buf.append((int)(om.actFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.checkFoldFlop.isReady())
            {
                buf.append((int)(om.checkFoldFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.openFlop.isReady())
            {
                buf.append((int)(om.openFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.raiseFlop.isReady())
            {
                buf.append((int)(om.raiseFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.actTurn.isReady())
            {
                buf.append((int)(om.actTurn.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.checkFoldTurn.isReady())
            {
                buf.append((int)(om.checkFoldTurn.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.openTurn.isReady())
            {
                buf.append((int)(om.openTurn.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.raiseTurn.isReady())
            {
                buf.append((int)(om.raiseTurn.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.actRiver.isReady())
            {
                buf.append((int)(om.actRiver.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.checkFoldRiver.isReady())
            {
                buf.append((int)(om.checkFoldRiver.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.openRiver.isReady())
            {
                buf.append((int)(om.openRiver.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.raiseRiver.isReady())
            {
                buf.append((int)(om.raiseRiver.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.handsBetFoldPostFlop.isReady())
            {
                buf.append((int)(om.handsBetFoldPostFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td><td align=\"center\">");
            buf.append(pre);
            if (om.handsOverbetPotPostFlop.isReady())
            {
                buf.append((int)(om.handsOverbetPotPostFlop.getWeightedPercentTrue(0.0f)*100));
            }
            else
            {
                buf.append("-");
            }
            buf.append(post);
            buf.append("</td></tr>");
        }
        buf.append("</table>");

        return buf.toString();
    }

    public int getNumLimpers()
    {
        return getPokerPlayer().getHoldemHand().getNumLimpers();
    }

    public boolean wasFirstRaiserPreFlop()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().wasFirstRaiserPreFlop(player);
    }

    public boolean wasLastRaiserPreFlop()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().wasLastRaiserPreFlop(player);
    }

    public boolean wasOnlyRaiserPreFlop()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().wasOnlyRaiserPreFlop(player);
    }

    public float getHohM()
    {
        PokerPlayer player = getPokerPlayer();

        return getHohM(player);
    }

    public float getTableAverageHohM()
    {
        int numPlayers = getNumPlayers();
        HoldemHand hhand = getPokerPlayer().getHoldemHand();

        float total = 0.0f;

        for (int i = 0; i < numPlayers; ++i)
        {
            PokerPlayer player = hhand.getPlayerAt(i);

            float m = (float)player.getChipCountAtStart() /
                      (float)(hhand.getAnte() * hhand.getNumPlayers() + hhand.getSmallBlind() + hhand.getBigBlind());

            total += m * (2.0f/3.0f + (float)(hhand.getNumPlayers() - 1) / 27.0f);
        }

        return total / numPlayers;
    }

    public float getRemainingAverageHohM()
    {
        PokerPlayer player = getPokerPlayer();
        int numPlayers = getNumPlayers();
        HoldemHand hhand = player.getHoldemHand();
        int mDivisor = hhand.getAnte() * numPlayers + hhand.getSmallBlind() + hhand.getBigBlind();

        float total = 0.0f;
        int count = 0;

        for (int i = 0; i < numPlayers; ++i)
        {
            PokerPlayer opponent = hhand.getPlayerAt(i);

            if (opponent.isFolded() || opponent.isAllIn() || (opponent == player)) continue;

            float m = (float)opponent.getChipCountAtStart() /
                      (float)mDivisor;

            total += m * (2.0f/3.0f + (float)(hhand.getNumPlayers() - 1) / 27.0f);

            ++count;
        }

        return total / count;
    }

    public float getHohM(PokerPlayer player)
    {
        HoldemHand hhand = player.getHoldemHand();

        // compute base M

        float m = (float)player.getChipCountAtStart() /
                   (float)(hhand.getAnte() * hhand.getNumPlayers() + hhand.getSmallBlind() + hhand.getBigBlind());

        // adjust to get effective M

        return m * (2.0f/3.0f + (float)(hhand.getNumPlayers() - 1) / 27.0f);
    }

    public float getHohQ()
    {
        PokerPlayer player = getPokerPlayer();

        return getHohQ(player);
    }

    public float getHohQ(PokerPlayer player)
    {
        HoldemHand hhand = player.getHoldemHand();

        PokerTable table = hhand.getTable();

        PokerGame game = table.getGame();

        return (float)player.getChipCountAtStart() / (float)game.getAverageStack();
    }

    public String getHohZoneName()
    {
        float m = getHohM();

        if (m <=  1.0f) return "Dead";
        if (m <=  5.0f) return "Red";
        if (m <= 10.0f) return "Orange";
        if (m <= 20.0f) return "Yellow";

        return "Green";
    }

    public int getHohZone()
    {
        PokerPlayer player = getPokerPlayer();

        return getHohZone(player);
    }

    public int getHohZone(PokerPlayer player)
    {
        float m = getHohM(player);

        if (m <=  1.0f) return AIConstants.HOH_DEAD;
        if (m <=  5.0f) return AIConstants.HOH_RED;
        if (m <= 10.0f) return AIConstants.HOH_ORANGE;
        if (m <= 20.0f) return AIConstants.HOH_YELLOW;

        return AIConstants.HOH_GREEN;
    }

    public boolean debugEnabled()
    {
        return TESTING(PokerConstants.TESTING_DEBUG_ADVISOR);
    }

    public synchronized void initDebug()
    {
        if (TESTING(PokerConstants.TESTING_DEBUG_ADVISOR))
        {
            if (debug_ == null)
            {
                debug_ = new StringBuilder();
            }
            debug_.setLength(0);
        }
    }

    public void appendDebug(String s)
    {
        if (TESTING(PokerConstants.TESTING_DEBUG_ADVISOR))
        {
            debug_.append(s);
        }
    }

    public String getDebugText()
    {
        if ((TESTING(PokerConstants.TESTING_DEBUG_ADVISOR)) && (debug_ != null))
        {
            return debug_.toString();
        }
        else
        {
            return "";
        }
    }

    public float getStealSuspicion()
    {
        return stealSuspicion_;
    }

    public int getConsecutiveHandsUnpaid()
    {
        BooleanTracker tracker = getOpponentModel().handsPaid;
        return tracker.getConsecutive(false);
    }

    public float computeBadBeatScore(HoldemHand hhand, PokerPlayer player)
    {
        // http://www.pokersavvy.com/article/badbeat.html

        /*
        Bad-Beat-o-Meter

        Just how bad was that beat? The Pokersavvy.com Bad-Beat-o-Meter has the answers.
        Click on the cards to enter the favored starting hand, the eventual winner, and the entire board.
        Also make sure to give the blinds and the total pot size on each street.
        NOTE: do not enter the amount that went in on each betting round, but the total pot size.
        For example, if the betting was finished pre-flop because a player was all-in, the pot size would
        be the same for each betting round.

        Launch the Bad-Beat-o-Meter

        How does it work? We've created an algorithm that determines how big a favorite a hand was on each street
        and multiplies that by the amount of money that went in on each street.
        So the more money you get in as a more prohibitive favorite, the higher your score.
        The equation subtracts points for money put into the pot as an underdog. The full equation is as follows:

        (pre flop percentage favorite-50 * pot size pre flop) +
        (on flop percentage favorite-50 * money added to pot on flop) +
        (on turn percentage favorite-50 * money added to pot on turn) -
        money added to pot on river
        ________________________________________________________________

        little blind + big blind

        A score of 2600 is a horribly bad beat.

        MODIFICATIONS:
        - CONSIDER ANTES AS WELL
        - NO PENALTY FOR BETS WHEN TRAILING
        */

        // TODO: side pots and suck-out splits

        // determine if player went to the showdown
        if (player.isFolded())
        {
            return 0.0f;
        }

        // for now, consider only heads-up showdowns
        if (hhand.getNumWithCards() != 2)
        {
            return 0.0f;
        }

        // figure out who beat us
        PokerPlayer winner = null;

        for (int i = hhand.getNumPlayers()-1; i >= 0; --i)
        {
            winner = hhand.getPlayerAt(i);

            HandAction action = hhand.getPotResult(winner, 0);

            if ((action != null) && (action.getAction() == HandAction.ACTION_WIN))
            {
                if (winner == player) return 0.0f; else break;
            }
        }

        Hand losingHand = player.getHand();

        HandList winningHands = new HandList();

        winningHands.add(winner.getHandSorted());

        Hand community = hhand.getCommunity();

        Hand trial = new Hand();

        Deck deck = new Deck(false);

        deck.removeCards(losingHand);

        StatResult preflop = HoldemSimulator.simulate(losingHand, trial, 5000, deck, winningHands, null, 0, 0);

        trial.addCard(community.getCard(0));
        deck.removeCard(community.getCard(0));
        trial.addCard(community.getCard(1));
        deck.removeCard(community.getCard(1));
        trial.addCard(community.getCard(2));
        deck.removeCard(community.getCard(2));

        StatResult flop = HoldemSimulator.iterate(losingHand, trial, 0, deck, winningHands, null, 0, 0);

        trial.addCard(community.getCard(3));
        deck.removeCard(community.getCard(3));

        StatResult turn = HoldemSimulator.iterate(losingHand, trial, 0, deck, winningHands, null, 0, 0);

        int preflopBets = hhand.getTotalPotChipCount(HoldemHand.ROUND_PRE_FLOP);
        int flopBets = hhand.getTotalPotChipCount(HoldemHand.ROUND_FLOP) - preflopBets;
        int turnBets = hhand.getTotalPotChipCount(HoldemHand.ROUND_TURN) - preflopBets - flopBets;
        //int riverBets = hhand.getTotalPotChipCount() - preflopBets - flopBets - turnBets;

        float score = (float)(
               (Math.max(preflop.getWinOrTiePercent() - 50d, 0) * preflopBets +
                Math.max(flop.getWinOrTiePercent() - 50d, 0) * flopBets +
                Math.max(turn.getWinOrTiePercent() - 50d, 0) * turnBets)) /
               (float)(hhand.getAnte() * hhand.getNumPlayers() + hhand.getSmallBlind() + hhand.getBigBlind());

/*
        printMessage(losingHand.toString() + " beat by " + winner.getHand());
        printMessage("Amount put in the pot before the flop:" + preflopBets);
        printMessage("Amount put in the pot on the flop:" + flopBets);
        printMessage("Amount put in the pot on the turn:" + turnBets);
        printMessage("Amount put in the pot on the river:" + riverBets);

        printMessage("Percent win before the flop:" + preflop.getWinOrTiePercent());
        printMessage("Percent win on the flop:" + flop.getWinOrTiePercent());
        printMessage("Percent win on the turn:" + turn.getWinOrTiePercent());

        printMessage("Bad beat score:" + score);
*/

        return score;
    }

    public float getSteam()
    {
        return dSteam_;
    }

    public float getStratFactor(String name, Hand hand, float min, float max)
    {
        int iMod = 0;

        if (!getPokerPlayer().isHuman())
        {
            DMTypedHashMap map = getMap();
            Integer mod = map.getInteger("strat." + name);
            if (mod == null)
            {
                //Random rand = new Random(seed);
                SecureRandom rand = SecurityUtils.getSecureRandom();
                mod = rand.nextInt(21) - 10;
                map.setInteger("strat." + name, mod);
            }

            iMod = mod;
        }
        return playerType_.getStratFactor(name, hand, min, max, iMod);
    }

    public float getStratFactor(String name, float min, float max)
    {
        return getStratFactor(name, null, min, max);
    }

    public boolean wasNoPotAction(int round)
    {
        return getPokerPlayer().getHoldemHand().getWasPotAction(round);
    }

    public int getHandsBeforeBigBlind()
    {
        PokerPlayer player = getPokerPlayer();
        HoldemHand hhand = player.getHoldemHand();

        int numPlayers = getNumPlayers();

        for (int i = 0; i < numPlayers; ++i)
        {
            if (hhand.getPlayerAt(i) == player)
            {
                if (getRound() == HoldemHand.ROUND_PRE_FLOP)
                {
                    return i;
                }
                else
                {
                    return (i + numPlayers - 2) % numPlayers;
                }
            }
        }

        return 0;
    }

    private void loadReputation()
    {
        if (getPokerPlayer().getTable().getGame().isOnlineGame()) return;

        PokerPlayer player = getPokerPlayer();

        if (!player.isHuman()) return;

        PlayerProfile profile = player.getProfile();

        if (profile.getStatsMap() == null) return;

        getOpponentModel().loadFromMap(profile.getStatsMap(), "rep.");
    }

    private void saveReputation()
    {
        if (getPokerPlayer().getTable().getGame().isOnlineGame()) return;

        PokerPlayer player = getPokerPlayer();

        if (!player.isHuman()) return;

        PlayerProfile profile = player.getProfile();

        if (profile.getStatsMap() == null) return;

        getOpponentModel().saveToMap(profile.getStatsMap(), "rep.");

        profile.save();
    }
}
