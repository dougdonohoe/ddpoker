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
 * HandFutures.java
 *
 * Created on April 7, 2004, 12:05 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.log4j.*;

/**
 *
 * @author  donohoe
 */
public class HandFutures 
{
    static Logger logger = Logger.getLogger(HandFutures.class);

    static boolean DEBUG = false;
    
    private HandSorted hole_;
    private HandSorted community_;
    private int nTotals_[] = new int[HandInfo.ROYAL_FLUSH + 1];
    private int nTotals2_[] = new int[HandInfo.ROYAL_FLUSH + 1];
    private int nTotal_;
    private int nImprovements_;
    private int nTypeImprovements_;
    private int nTypeImprovements2_;
    private int MORE;
    
    /**
     * Create new hand future calc
     */
    public HandFutures(HandInfoFaster fast, HandInfo info, int nMinHandType)
    {
        hole_ = info.getHole();
        community_ = info.getCommunity();
        calcFutures(fast, hole_, community_, nMinHandType);
    }

    /**
     * Create new hand future calc
     */
    public HandFutures(HandInfoFaster fast, Hand hole, Hand community)
    {
        calcFutures(fast, hole, community, HandInfo.PAIR);
    }
   
    /**
     * Create new hand future calc
     */
    public HandFutures(HandInfoFaster fast, Hand hole, Hand community, int nMinHandType)
    {
        calcFutures(fast, hole, community, nMinHandType);
    }

    /**
     * Return if this hand has a flush draw
     */
    public boolean hasFlushDraw()
    {
        // these percentages are rough estimates (based on test runs in main())
        return (getOddsImproveTo(HandInfo.FLUSH)+getOddsImproveTo(HandInfo.STRAIGHT_FLUSH)) > (MORE == 2 ? 33 : 18);
    }
    
    /**
     * Return if this hand has a straight draw
     */
    public boolean hasStraightDraw()
    {
        // these percentages are rough estimates (based on test runs in main())
        return (getOddsImproveTo(HandInfo.STRAIGHT)+getOddsImproveTo(HandInfo.STRAIGHT_FLUSH)) > (MORE == 2 ? 25 : 15);
    }
    
    /**
     * Return if this hand has a gut-shot straight draw
     */
    public boolean hasGutShotStraightDraw()
    {
        // these percentages are rough estimates (based on test runs in main())
        return (getOddsImproveTo(HandInfo.STRAIGHT)+getOddsImproveTo(HandInfo.STRAIGHT_FLUSH)) > (MORE == 2 ? 15 : 8);
    }
    
    /**
     * Return odds (0-100) of improving to given type of hand.  We use figures
     * from hands which involve the hole cards (e.g., avoiding board-only
     * trips, straights, flushes, etc.)
     */
    public float getOddsImproveTo(int nHandType)
    {
        if (nTotal_ == 0) return 0.0f;
        
        return 100.0f * ((float)nTotals2_[nHandType]) / nTotal_;
    }
    
    /**
     * Return odds (0-100) of improving to a better type of hand than we started with.
     */
    public float getOddsImprove()
    {
        return 100.0f * ((float)nTypeImprovements2_) / nTotal_;
    }
    
    /**
     * Calculate strenght of hand against N opponents.  Return is float from
     * 0 to 1 indicating probability of this hand being the best hand
     */
    private void calcFutures(HandInfoFaster FAST, Hand hole, Hand community, int nMinHandType)
    {
        Hand commcopy = new Hand(community); // copy for reuse
        int nComm = community.size();
        MORE = 5 - nComm;
        ApplicationError.assertTrue(MORE == 2 || MORE == 1, "HandFutures called with wrong board", community);
        
        // our current rank
        int ourscore = FAST.getScore(hole, community);
        int ourtype = HandInfoFast.getTypeFromScore(ourscore);
        
        // get remaining cards (new deck less hole, community)
        Deck deck = new Deck(false);
        deck.removeCards(hole);
        deck.removeCards(community);
        
        // more init
        int newscore, nType, suit;
        int nSize = deck.size();
        int nEND = nSize -1;
        if (MORE == 1) nEND = nSize;
        
        // loop over all remaining board cards
        for (int next1 = 0; next1 < nEND; next1 ++)
        {
            commcopy.addCard(deck.getCard(next1));

            for (int next2 = next1 + 1; MORE == 1 || next2 < nSize; next2++)
            {
                // if we need 2 cards, get next one
                if (MORE == 2)
                {
                    commcopy.addCard(deck.getCard(next2));
                }

                nTotal_++;

                // calc
                newscore = FAST.getScore(hole, commcopy);
                suit = FAST.getLastMajorSuit();
                nType = HandInfoFast.getTypeFromScore(newscore);
                nTotals_[nType]++;
                
                // count where our score improves
                if (newscore > ourscore) {
                    nImprovements_++;
                    //logger.debug("IMPROVE: " + hole + " " + commcopy + ": " + HandInfo.getHandTypeDesc(nType) + " " + newscore + " - " +
                        //HandInfo.isOurHandInvolved(hole, newscore, suit,ourtype != HandInfo.HIGH_CARD) + " oursocore: " + ourscore);
                }
                
                // count where hand type improves, but only do so
                // if it includes cards from hole.  For example,
                // if two pair, two must be from our hand
                if (nType > ourtype && nType > nMinHandType)
                {
                    nTypeImprovements_++;
                    if (HandInfo.isOurHandInvolved(hole, newscore, suit, ourtype != HandInfo.HIGH_CARD))
                    {
                        nTypeImprovements2_++;
                        nTotals2_[nType]++;
                    }
                    //logger.debug("HAND: " + hole + " " + commcopy + ": " + HandInfo.getHandTypeDesc(nType) + " " + newscore + " - " +
                      //HandInfo.isOurHandInvolved(hole, newscore, suit, ourtype != HandInfo.HIGH_CARD));
                }


                // remove 2nd card added
                if (MORE == 2)
                {
                    commcopy.removeCard(commcopy.size() - 1);
                }

                // if we only needed one, don't loop anymore
                // this is a "hack" way to avoid repeating calc
                // code above (note that if MORE == 1, a second
                // card was not added to the board)
                if (MORE == 1)
                {
                    break;
                }
            }

            // remove 1st card added
            commcopy.removeCard(commcopy.size() - 1);
        }        
        
        // print results
        if (DEBUG)
        {
            logger.debug("");
            logger.debug("HAND: " + hole + "," + community);
            logger.debug("Start: " + fName.form(HandInfo.getHandTypeDesc(ourtype)) + " " + ourscore);
            logger.debug("TOTAL hands: " + nTotal_);
            int sum = 0;
            for (int i = HandInfo.HIGH_CARD; i <= HandInfo.ROYAL_FLUSH; i++)
            {
                logger.debug("   " + fName.form(HandInfo.getHandTypeDesc(i)) + ": "+ 
                                     HandStat.fCnt.form(nTotals_[i]) + " (" +
                                     HandStat.fPerc.form(100.0d * ((float)nTotals_[i]) / nTotal_) + "%)" + " -- "+
                                     HandStat.fCnt.form(nTotals2_[i]) + " (" +
                                     HandStat.fPerc.form(getOddsImproveTo(i)) + "%)"
                                     );
                sum += nTotals_[i];
            }
            logger.debug("SUM: " + sum);
            logger.debug("Improve:       " + nImprovements_ + " (" + HandStat.fPerc.form(100.0f * ((float)nImprovements_) / nTotal_) + "%)");
            logger.debug("Type Improve:  " + nTypeImprovements_ + " (" + HandStat.fPerc.form(100.0f * ((float)nTypeImprovements_) / nTotal_) + "%)");
            logger.debug("Type Improve2: " + nTypeImprovements2_ + " (" + HandStat.fPerc.form(100.0f * ((float)nTypeImprovements2_) / nTotal_) + "%)");
            logger.debug("Has Straight Draw: " + hasStraightDraw());
            logger.debug("Has GutShot Draw:  " + hasGutShotStraightDraw());
            logger.debug("Has Flush Draw:    " + hasFlushDraw() +"\n");
        }
    }
    
    static Format fName = new Format("%-15s");
    
    /**
     * Testing
     */
    public static void main(String args[])
    {
        new ConfigManager("poker", ApplicationType.CLIENT);
        logger.debug("Start");
                
        DEBUG = true;
        
        HandSorted hand;
        HandSorted comm;
        HandInfoFaster fast = new HandInfoFaster();

        hand = new HandSorted(Card.CLUBS_3, Card.CLUBS_5);
        comm = new HandSorted(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6);
        new HandFutures(fast, hand, comm);
        
        hand = new HandSorted(Card.CLUBS_3, Card.CLUBS_5);
        comm = new HandSorted(Card.CLUBS_4, Card.DIAMONDS_6, Card.HEARTS_5);
        new HandFutures(fast, hand, comm);

        hand = new HandSorted(Card.CLUBS_A, Card.CLUBS_5);
        comm = new HandSorted(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_9);
        new HandFutures(fast, hand, comm);
        
        hand = new HandSorted(Card.DIAMONDS_A, Card.CLUBS_Q);
        comm = new HandSorted(Card.HEARTS_J, Card.CLUBS_4, Card.HEARTS_3);
        new HandFutures(fast, hand, comm);
        
        hand = new HandSorted(Card.DIAMONDS_5, Card.CLUBS_Q);
        comm = new HandSorted(Card.HEARTS_5, Card.SPADES_Q, Card.HEARTS_8);
        new HandFutures(fast, hand, comm);        
        
        hand = new HandSorted(Card.DIAMONDS_5, Card.CLUBS_7);
        comm = new HandSorted(Card.HEARTS_4, Card.SPADES_8, Card.HEARTS_Q);
        new HandFutures(fast, hand, comm, HandInfo.TRIPS);
        
        hand = new HandSorted(Card.DIAMONDS_Q, Card.CLUBS_K);
        comm = new HandSorted(Card.HEARTS_4, Card.SPADES_5, Card.HEARTS_6);
        new HandFutures(fast, hand, comm);
        
        hand = new HandSorted(Card.DIAMONDS_5, Card.CLUBS_7);
        comm = new HandSorted(Card.HEARTS_3, Card.SPADES_3, Card.CLUBS_J);
        new HandFutures(fast, hand, comm);
        
        hand = new HandSorted(Card.DIAMONDS_5, Card.CLUBS_J);
        comm = new HandSorted(Card.HEARTS_3, Card.HEARTS_8, Card.HEARTS_J);
        new HandFutures(fast, hand, comm);

        logger.debug("RIVER: Flush draw");
        hand = new HandSorted(Card.HEARTS_5, Card.CLUBS_J);
        comm = new HandSorted(Card.HEARTS_3, Card.HEARTS_8, Card.HEARTS_J, Card.DIAMONDS_2);
        new HandFutures(fast, hand, comm);
        
        logger.debug("RIVER: Gut Shot Straight draw");
        hand = new HandSorted(Card.HEARTS_5, Card.CLUBS_6);
        comm = new HandSorted(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_J, Card.DIAMONDS_2);
        new HandFutures(fast, hand, comm);
        
        logger.debug("RIVER: Straight draw");
        hand = new HandSorted(Card.HEARTS_7, Card.CLUBS_6);
        comm = new HandSorted(Card.HEARTS_9, Card.HEARTS_8, Card.DIAMONDS_Q, Card.DIAMONDS_2);
        new HandFutures(fast, hand, comm);
        
        logger.debug("RIVER: Straight-Flush draw");
        hand = new HandSorted(Card.CLUBS_3, Card.CLUBS_5);
        comm = new HandSorted(Card.CLUBS_4, Card.DIAMONDS_6, Card.CLUBS_6, Card.HEARTS_3);
        new HandFutures(fast, hand, comm);
    }
    
}
