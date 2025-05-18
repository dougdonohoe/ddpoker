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
 * HandInfo.java
 *
 * Created on January 9, 2004, 9:48 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.LoggingConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandSorted;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to determine what a player has and has a chance of making 
 *
 * @author  donohoe
 */
@SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "CommentedOutCode", "DuplicatedCode"})
public class HandInfo implements Comparable<HandInfo>
{
    //static Logger logger = LogManager.getLogger(HandInfo.class);

    // types of hands
    public static final int ROYAL_FLUSH = 10;
    public static final int STRAIGHT_FLUSH = 9;
    public static final int QUADS = 8;
    public static final int FULL_HOUSE = 7;
    public static final int FLUSH = 6;
    public static final int STRAIGHT = 5;
    public static final int TRIPS = 4;
    public static final int TWO_PAIR = 3;
    public static final int PAIR = 2;
    public static final int HIGH_CARD = 1;

    // descriptions
    private static final String[] desc_ = new String[ROYAL_FLUSH + 1];

    // scoring
    static final int BASE = 1000000;
    static final int H0 = 1;     // 16 ^ 0  ==  2 ^ 0
    static final int H1 = 16;    // 16 ^ 1  ==  2 ^ 4
    static final int H2 = 256;   // 16 ^ 2  ==  2 ^ 8
    static final int H3 = 4096;  // 16 ^ 3  ==  2 ^ 12
    static final int H4 = 65536; // 16 ^ 4  ==  2 ^ 16

    // member
    private final PokerPlayer player_;
    private HandSorted hand_;
    private HandSorted comm_;
    private HandSorted all_;
    
    // hand info    
    private int nSpades_;
    private int nClubs_;
    private int nDiamonds_;
    private int nHearts_;
    private int nPairs_;
    private int nTrips_;
    private int nQuads_;
    private int[] nNumRank_;
    private ArrayList<Hand> seq_;
    private ArrayList<Hand> seqFlush_;
    
    // summary
    private int nType_;
    private int nScore_;
    private Hand best_;

    /** 
     * Creates a new instance of HandInfo with null player
     */
    public HandInfo(HandSorted hand, HandSorted community) 
    {
        this(null, hand, community);
    }
    
    /** 
     * Creates a new instance of HandInfo 
     */
    public HandInfo(PokerPlayer player, HandSorted hand, HandSorted community) 
    {
        player_ = player;
        hand_ = hand;
        comm_ = community;
        
        categorize();
    }
    
    /**
     * Re-categorize with new hand and return score
     */
    @SuppressWarnings("unused")
    public int rescoreHand(HandSorted hand)
    {
        hand_ = hand;
        categorize();
        return getScore();
    }
    
    /**
     * Re-categorize with new hand and return score
     */
    @SuppressWarnings("unused")
    public int rescoreHand(HandSorted hand, HandSorted community)
    {
        comm_ = community;
        hand_ = hand;
        categorize();
        return getScore();
    }

    /**
     * Figure out type of hand
     */
    private void categorize()
    {
        // init (or re-init in some cases)
        nSpades_ = 0;
        nClubs_ = 0;
        nDiamonds_ = 0;
        nHearts_ = 0;
        nPairs_ = 0;
        nTrips_ = 0;
        nQuads_ = 0;

        if (nNumRank_ == null) nNumRank_ = new int[Card.ACE+1];
        else Arrays.fill(nNumRank_, 0);
        
        if (seq_ == null) seq_ = new ArrayList<>();
        else seq_.clear();
        
        if (seqFlush_ == null) seqFlush_ = new ArrayList<>();
        else seqFlush_.clear();
        
        if (all_ == null) all_ = new HandSorted(hand_);
        else { all_.clear(); all_.addAll(hand_); }
        
        if (comm_ != null) all_.addAll(comm_);
        
        if (best_ == null) best_ = new Hand();
        else best_.clear();        
        
        // TESTING
        //if (true) return;
        
        // get basic info
        Hand seq = new Hand();
        seq_.add(seq);
        Hand seqFlush;
        Card last;
        Card c = null;
        for (int i = 0; i < all_.size(); i++)
        {
            last = c;
            c = all_.getCard(i);
            
            // get straight sequences
            if (last == null || (last.getRank() + 1) == c.getRank())
            {
                seq.addCard(c);
            }
            else //noinspection StatementWithEmptyBody
                if (last.getRank() == c.getRank())
            {
                // do nothing - duplicate rank
            }
            else
            {
                seq = new Hand();
                seq_.add(seq);
                seq.addCard(c);
            }
            
            // get straight flush sequences
            last = null;
            seqFlush = getSeqForSuit(c);
            if (seqFlush != null && !seqFlush.isEmpty()) last = seqFlush.getCard(seqFlush.size() - 1);
            if (seqFlush != null && (last == null || (last.getRank() + 1) == c.getRank()))
            {
                seqFlush.addCard(c);
            }
            else
            {
                seqFlush = new Hand();
                seqFlush_.add(seqFlush);
                seqFlush.addCard(c);
            }

            // count suits
            if (c.isSpades()) nSpades_++;
            if (c.isHearts()) nHearts_++;
            if (c.isClubs())  nClubs_++;
            if (c.isDiamonds()) nDiamonds_++;
            
            // count similar ranks (for pair/trips/quads)
            nNumRank_[c.getRank()]++;
        }
        
        // handle low aces for straights, straight flushes
        for (int i = all_.size() - 1; i >= 0; i--)
        {
            c = all_.getCard(i);
            if (c.getRank() < Card.ACE) break;
            
            // straights
            for (Hand cards : seq_) {
                seq = cards;
                if (seq.isEmpty()) continue;
                if (seq.getCard(0).getRank() == Card.TWO) {
                    seq.insertCard(c);
                }
            }
            
            // straight flushes
            for (Hand cards : seqFlush_) {
                seq = cards;
                if (seq.isEmpty()) continue;
                if (seq.getCard(0).getRank() == Card.TWO &&
                        seq.getCard(0).isSameSuit(c)) {
                    seq.insertCard(c);
                }
            }
        }
        
        // count up pairs, trips, quads
        for (int i = Card.TWO; i <= Card.ACE; i++)
        {
            if (nNumRank_[i] == 2) nPairs_++;
            if (nNumRank_[i] == 3) nTrips_++;
            if (nNumRank_[i] == 4) nQuads_++;
        }
        
        // now categorize (each method fills in best_ hand)
        if (hasRoyalFlush()) nType_ = ROYAL_FLUSH;
        else if (hasStraightFlush()) nType_ = STRAIGHT_FLUSH;
        else if (hasQuads()) nType_ = QUADS;
        else if (hasFullHouse()) nType_ = FULL_HOUSE;
        else if (hasFlush()) nType_ = FLUSH;
        else if (hasStraight()) nType_ = STRAIGHT;
        else if (hasTrips()) nType_ = TRIPS;
        else if (has2Pair()) nType_ = TWO_PAIR;
        else if (hasPair()) nType_ = PAIR;
        else if (hasHighCard()) nType_ = HIGH_CARD;
        else throw new ApplicationError();
        
        // just make sure best hand has 5 cards
        ApplicationError.assertTrue(best_.size() == 5, "Best hand should have 5 cards", best_);

        // figure out score
        calcScore();
    }
    
    /**
     * For determining straight flush runs,
     * we need to get the last seq for the suit
     */
    private Hand getSeqForSuit(Card c)
    {
        Hand seq;
        for (int i = (seqFlush_.size() - 1); i >= 0; i--)
        {
            seq = seqFlush_.get(i);
            if (seq.isEmpty()) continue;
            if (seq.getCard(0).isSameSuit(c)) return seq;
        }
        return null;
    }
    
    /**
     * Give this hand a score - used for sorting, etc.
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void calcScore()
    {
        // best_ is ordered where the best cards are first in
        // list (e.g., trips of a full house first)
        nScore_ = BASE * nType_;
        
        switch (nType_)
        {
            case ROYAL_FLUSH:
            case STRAIGHT_FLUSH:
                nScore_ += best_.getCard(0).getRank() * H0; // high card
                break;
                
            case QUADS:
                nScore_ += best_.getCard(0).getRank() * H1; // quads
                nScore_ += best_.getCard(4).getRank() * H0; // kicker 1
                break;
                
            case FULL_HOUSE:
                nScore_ += best_.getCard(0).getRank() * H1; // trips
                nScore_ += best_.getCard(3).getRank() * H0; // pair
                break;
                
            case FLUSH:
                nScore_ += best_.getCard(0).getRank() * H4; // kicker 1
                nScore_ += best_.getCard(1).getRank() * H3; // kicker 2
                nScore_ += best_.getCard(2).getRank() * H2; // kicker 3
                nScore_ += best_.getCard(3).getRank() * H1; // kicker 4
                nScore_ += best_.getCard(4).getRank() * H0; // kicker 5
                break;
                
            case STRAIGHT:
                nScore_ += best_.getCard(0).getRank() * H0; // high card
                break;
                
            case TRIPS:
                nScore_ += best_.getCard(0).getRank() * H2; // trips
                nScore_ += best_.getCard(3).getRank() * H1; // kicker 1
                nScore_ += best_.getCard(4).getRank() * H0; // kicker 2
                break;
                
            case TWO_PAIR:
                nScore_ += best_.getCard(0).getRank() * H2; // top pair
                nScore_ += best_.getCard(2).getRank() * H1; // 2nd pair
                nScore_ += best_.getCard(4).getRank() * H0; // kicker 1
                break;
                
            case PAIR:
                nScore_ += best_.getCard(0).getRank() * H3; // pair
                nScore_ += best_.getCard(2).getRank() * H2; // kicker 1
                nScore_ += best_.getCard(3).getRank() * H1; // kicker 2
                nScore_ += best_.getCard(4).getRank() * H0; // kicker 3
                break;
                
            case HIGH_CARD:
                nScore_ += best_.getCard(0).getRank() * H4; // kicker 1
                nScore_ += best_.getCard(1).getRank() * H3; // kicker 2
                nScore_ += best_.getCard(2).getRank() * H2; // kicker 3
                nScore_ += best_.getCard(3).getRank() * H1; // kicker 4
                nScore_ += best_.getCard(4).getRank() * H0; // kicker 5
                break;
                
        }
    }
    
    /**
     * Get type
     */
    public int getHandType()
    {
        return nType_;
    }
    
    /**
     * Get type desc
     */
    public String getHandTypeDesc()
    {
        return getHandTypeDesc(nType_);
    }

    /**
     * Get type desc
     */
    public static String getHandTypeDesc(int nType)
    {
        init();
        return desc_[nType];
    }
    
    private static boolean init = false;
    private static synchronized void init()
    {
        if (init) return;
        init = true;
        for (int i = HIGH_CARD; i <= ROYAL_FLUSH; i++)
        {
            desc_[i] = PropertyConfig.getMessage("msg.hand." + i);
        }
    }
    
    /**
     * Get score
     */
    public int getScore()
    {
        return nScore_;
    }
    
    /**
     * Get best hand
     */
    public Hand getBest()
    {
        return best_;
    }
    
    /**
     * Get hole cards
     */
    public HandSorted getHole()
    {
        return hand_;
    }
    
    /**
     * Get community cards
     */
    public HandSorted getCommunity()
    {
        return comm_;
    }
    
    /**
     * Get player
     */
    public PokerPlayer getPlayer()
    {
        return player_;
    }
    
//    /////
//    ///// possible hands
//    /////
//    
//    public int getOutsForFlush()
//    {
//        if (nType_ == FLUSH) return 0;
//
//        int nMax = 0;
//        nMax = Math.max(nMax, nSpades_);
//        nMax = Math.max(nMax, nClubs_);
//        nMax = Math.max(nMax, nDiamonds_);
//        nMax = Math.max(nMax, nHearts_);
//        return 5 - nMax;
//    }
//    
//    public int getCardsForStraight()
//    {
//        Hand seq;
//        Card best = null;
//        for (int i = 0; i < seq_.size(); i++)
//        {
//            seq = (Hand) seq_.get(i);
//            if (seq.size() >= 5) 
//            {
//                Card last = seq.getCard(seq.size() -1);
//                if (best == null || last.getRank() > best.getRank())
//                {
//                    best = last;
//                    best_.clear();
//                    for (int j = 0; j < 5; j++)
//                    {
//                        best_.addCard(seq.getCard(seq.size() - (j+1)));
//                    }
//                }
//            }
//        }
//        
//    }
    
    /////
    ///// Methods to determine type & fill in best_ hand
    /////
    
    /**
     * Does this hand have a straight flush?
     */
    private boolean hasRoyalFlush()
    {       
        Hand seqFlush;
        for (Hand cards : seqFlush_) {
            seqFlush = cards;
            if (seqFlush.size() >= 5) {
                Card last = seqFlush.getCard(seqFlush.size() - 1);
                if (last.getRank() == Card.ACE) {
                    best_.clear();
                    for (int j = 0; j < 5; j++) {
                        best_.addCard(seqFlush.getCard(seqFlush.size() - (j + 1)));
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Does this hand have a straight flush?
     */
    private boolean hasStraightFlush()
    {       
        Hand seqFlush;
        Card best = null;
        for (Hand cards : seqFlush_) {
            seqFlush = cards;
            if (seqFlush.size() >= 5) {
                Card last = seqFlush.getCard(seqFlush.size() - 1);
                if (best == null || last.getRank() > best.getRank()) {
                    best = last;
                    best_.clear();
                    for (int j = 0; j < 5; j++) {
                        best_.addCard(seqFlush.getCard(seqFlush.size() - (j + 1)));
                    }
                }
            }
        }
        return best != null;
    }

    /**
     * does this hand have quads?
     */
    private boolean hasQuads()
    {
        if (nQuads_ <= 0) return false;
        
        // get highest quads
        for (int i = Card.ACE; i >= Card.TWO; i--)
        {
            if (nNumRank_[i] == 4) 
            {
                best_.clear();
                Card card;
                Hand dup = new Hand(all_);
                
                // copy quads
                for (int j = dup.size() - 1; j >= 0; j--)
                {
                    card = dup.getCard(j);
                    if (card.getRank() == i)
                    {
                        dup.remove(j);
                        best_.addCard(card);
                    }
                }
                
                // card left at end is kicker
                best_.addCard(dup.getCard(dup.size() - 1));
                
                return true;
            }
        }
        
        throw new ApplicationError();
    }
    
    /**
     * does this hand have a full house?
     */
    private boolean hasFullHouse()
    {
        // full house is when you have
        // a trip and a pair (also possible
        // to have two sets of trips).
        // we ignore quads since if you have
        // quads, you don't care about a full house
        if ((nTrips_ > 0 && nPairs_ > 0) ||
            (nTrips_ > 1))
        {
            best_.clear();
            Card card;
            Hand dup = new Hand(all_);
            int topset = 0;
                    
            // get highest trips
            for (int i = Card.ACE; i >= Card.TWO; i--)
            {
                if (nNumRank_[i] == 3) 
                {
                    // copy trips
                    topset = i;
                    for (int j = dup.size() - 1; j >= 0; j--)
                    {
                        card = dup.getCard(j);
                        if (card.getRank() == i)
                        {
                            dup.remove(j);
                            best_.addCard(card);
                        }
                    }
                    break;
                }
            }
            
            // get the highest pair
            for (int i = Card.ACE; i >= Card.TWO; i--)
            {
                if (topset == i) continue;
                
                if (nNumRank_[i] >= 2) 
                {
                    // copy remaining pair
                    for (int j = dup.size() - 1; j >= 0; j--)
                    {
                        card = dup.getCard(j);
                        if (card.getRank() == i && best_.size() < 5)
                        {
                            dup.remove(j);
                            best_.addCard(card);
                        }
                    }
                    break;
                }
            }

            return true;
        }
        return false;
    }
    
    /**
     * Does this hand have a flush?
     */
    private boolean hasFlush()
    {
        if (nSpades_ >= 5) 
        {
            fillSuit(CardSuit.SPADES);
            return true;
        }
        if (nHearts_ >= 5)
        {
            fillSuit(CardSuit.HEARTS);
            return true;
        }
        if (nDiamonds_ >= 5)
        {
            fillSuit(CardSuit.DIAMONDS);
            return true;
        }
        if (nClubs_ >= 5)
        {
            fillSuit(CardSuit.CLUBS);
            return true;
        }
        return false;
    }
    
    /**
     * Fill best hand with the highest cards from the given suit
     */
    private void fillSuit(CardSuit suit)
    {
        Card c;
        best_.clear();
        for (int j = all_.size() - 1; best_.size() < 5; j--)
        {
            c = all_.getCard(j);
            if (c.getCardSuit() == suit)
            {
                best_.addCard(c);
            }
        }
    }
    
    /**
     * does this hand have a straight?
     */
    private boolean hasStraight()
    {
        Hand seq;
        Card best = null;
        for (Hand cards : seq_) {
            seq = cards;
            if (seq.size() >= 5) {
                Card last = seq.getCard(seq.size() - 1);
                if (best == null || last.getRank() > best.getRank()) {
                    best = last;
                    best_.clear();
                    for (int j = 0; j < 5; j++) {
                        best_.addCard(seq.getCard(seq.size() - (j + 1)));
                    }
                }
            }
        }
        return best != null;
    }
    
    
    /**
     * does this hand have trips?
     */
    private boolean hasTrips()
    {
        if (nTrips_ <= 0) return false;
        
        // get highest quads
        for (int i = Card.ACE; i >= Card.TWO; i--)
        {
            if (nNumRank_[i] == 3) 
            {
                best_.clear();
                Card card;
                Hand dup = new Hand(all_);
                
                // copy quads
                for (int j = dup.size() - 1; j >= 0; j--)
                {
                    card = dup.getCard(j);
                    if (card.getRank() == i)
                    {
                        dup.remove(j);
                        best_.addCard(card);
                    }
                }
                
                // two cards left at end are kickers
                best_.addCard(dup.removeCard(dup.size() - 1));
                best_.addCard(dup.removeCard(dup.size() - 1));
                
                return true;
            }
        }
        
        throw new ApplicationError();
    }

    /**
     * does this hand have two pair
     * (returns true if num pairs is > 2)
     */
    private boolean has2Pair()
    {
        if (nPairs_ < 2) return false;
        
        best_.clear();
        Card card;
        Hand dup = new Hand(all_);
        int toppair = 0;

        // get the highest pair
        for (int i = Card.ACE; i >= Card.TWO; i--)
        {
            if (nNumRank_[i] == 2) 
            {
                // copy 1st pair
                toppair = i;
                for (int j = dup.size() - 1; j >= 0; j--)
                {
                    card = dup.getCard(j);
                    if (card.getRank() == i)
                    {
                        dup.remove(j);
                        best_.addCard(card);
                    }
                }
                break;
            }
        }

        // get next highest pair
        for (int i = Card.ACE; i >= Card.TWO; i--)
        {
            if (toppair == i) continue;
            if (nNumRank_[i] == 2) 
            {
                // copy remaining pair
                for (int j = dup.size() - 1; j >= 0; j--)
                {
                    card = dup.getCard(j);
                    if (card.getRank() == i)
                    {
                        dup.remove(j);
                        best_.addCard(card);
                    }
                }
                break;
            }
        }
        
        // card left at end is kicker
        best_.addCard(dup.removeCard(dup.size() - 1));

        return true;  
    }
    
    
    /**
     * does this hand have a pair (or more)
     */
    private boolean hasPair()
    {
        if (nPairs_ < 1) return false;
       
        best_.clear();
        Card card;
        Hand dup = new Hand(all_);

        // get the highest pair
        for (int i = Card.ACE; i >= Card.TWO; i--)
        {
            if (nNumRank_[i] == 2) 
            {
                // copy pair
                for (int j = dup.size() - 1; j >= 0; j--)
                {
                    card = dup.getCard(j);
                    if (card.getRank() == i)
                    {
                        dup.remove(j);
                        best_.addCard(card);
                    }
                }
                break;
            }
        }
        
        // three cards left at end are kickers
        best_.addCard(dup.removeCard(dup.size() - 1));
        best_.addCard(dup.removeCard(dup.size() - 1));
        best_.addCard(dup.removeCard(dup.size() - 1));
        
        return true;
    }
    
    /**
     * copy high cards
     */
    private boolean hasHighCard()
    {
        best_.clear();
        for (int i = 0; i < all_.size() && i < 5; i++)
        {
            best_.addCard(all_.getCard(all_.size() - (1+i)));
        }
        return true;
    }
    
    /**
     * Compare hands
     */
    public int compareTo(HandInfo i)
    {
        return nScore_ - i.nScore_;
    }

    /**
     * Return true if the given hole cards are part of the "significant" part
     * of the hand type (e.g., one of the cards in a pair, one in trips, etc.).
     * Strict two pair means both hole cards must be used.  Otherwise, only one
     * of the two pair must match.
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static boolean isOurHandInvolved(Hand hole, int score, int suit, boolean bStrictTwoPair)
    {
        int[] cards = new int[5];
        HandInfoFast.getCards(score, cards);
        int nType = HandInfoFast.getTypeFromScore(score);
        
        switch (nType)
        {
            case HandInfo.HIGH_CARD:
                return hole.isInHand(cards[0]);
                
            case HandInfo.PAIR:
                return hole.isInHand(cards[0]);
            
            case HandInfo.TWO_PAIR:
                if (bStrictTwoPair)
                    return hole.isInHand(cards[0]) && hole.isInHand(cards[1]);
                else
                    return hole.isInHand(cards[0]) || hole.isInHand(cards[1]);
                
            case HandInfo.TRIPS:
                return hole.isInHand(cards[0]);
                
            case HandInfo.STRAIGHT:
                return hole.isInHand(cards[0])   || hole.isInHand(cards[0] - 1) ||
                       hole.isInHand(cards[0]-2) || hole.isInHand(cards[0] - 3) ||
                       hole.isInHand(cards[0] > 5 ? cards[0]-4 : Card.ACE); // BUG 316 - ace low straight
                
            case HandInfo.FLUSH:
                return hole.isInHand(cards[0], suit) || hole.isInHand(cards[1], suit) ||
                       hole.isInHand(cards[2], suit) || hole.isInHand(cards[3], suit) ||
                       hole.isInHand(cards[4], suit);
                
            case HandInfo.FULL_HOUSE:
                return hole.isInHand(cards[0]) || hole.isInHand(cards[1]);
                
            case HandInfo.QUADS:
                return hole.isInHand(cards[0]);
                
            case HandInfo.STRAIGHT_FLUSH:
            case HandInfo.ROYAL_FLUSH:
                return hole.isInHand(cards[0], suit)   || hole.isInHand(cards[0] - 1, suit) ||
                       hole.isInHand(cards[0]-2, suit) || hole.isInHand(cards[0] - 3, suit) ||
                       hole.isInHand(cards[0] > 5 ? cards[0]-4 : Card.ACE, suit); // BUG 316 - ace low straight
        }
        
        return false;
    }
    
    /**
     * assuming the given hand is a flush, return whether the hole cards represent
     * a nut flush (nCards == 1).  If nCards > 1, then this return true if
     * this hand contains the 1st/2nd best flush (2), 1st/2nd/3rd best flush (3), etc.
     */
    public static boolean isNutFlush(Hand hole, HandSorted community, int nSuit, int nCards)
    {
        // if AKQJ are in hand, we need the 10 for the nuts.  Of course,
        // that means we'd have a royal, so this probably wouldn't be called.
        int nNeedCard = 10;
        for (int i = Card.ACE; i >= Card.JACK; i--)
        {
            //noinspection StatementWithEmptyBody
            if (community.isInHand(i, nSuit))
            {
                // continue to next
            }
            else 
            {
                nNeedCard = i;
                break;               
            }
        }
        
        while (nCards > 0)
        {
            if (!community.isInHand(nNeedCard, nSuit)) {
                if (hole.isInHand(nNeedCard, nSuit)) return true;
                nCards--;
            }
            nNeedCard--;
        }
        return false;
    }
    
    ////
    //// TESTING
    ////

    // TODO: make a unit test!
    public static void main(String[] args)
    {
        LoggingConfig loggingConfig = new LoggingConfig("plain", ApplicationType.COMMAND_LINE);
        loggingConfig.init();

        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        testPlayer = new PokerPlayer(0, "Test", true);
        
        test("Royal Flush (clubs)        ", Card.CLUBS_A, Card.CLUBS_J, Card.CLUBS_K, Card.CLUBS_Q, Card.CLUBS_T, Card.SPADES_2, Card.HEARTS_K);
        test("Straight Flush (hearts)    ", Card.HEARTS_9, Card.HEARTS_J, Card.HEARTS_K, Card.HEARTS_Q, Card.CLUBS_Q, Card.HEARTS_T, Card.HEARTS_3);
        test("A straight, K str flush    ", Card.HEARTS_9, Card.HEARTS_J, Card.HEARTS_K, Card.HEARTS_Q, Card.CLUBS_Q, Card.HEARTS_T, Card.CLUBS_A);
        test("Flush (clubs)              ", Card.CLUBS_8, Card.CLUBS_J, Card.CLUBS_K, Card.CLUBS_Q, Card.CLUBS_T, Card.SPADES_2, Card.HEARTS_3);
        test("Full House (two trips)     ", Card.CLUBS_8, Card.HEARTS_8, Card.DIAMONDS_8, Card.CLUBS_Q, Card.SPADES_Q, Card.DIAMONDS_Q, Card.HEARTS_7);
        test("Full House                 ", Card.CLUBS_8, Card.HEARTS_8, Card.DIAMONDS_8, Card.CLUBS_Q, Card.SPADES_Q, Card.DIAMONDS_7, Card.HEARTS_7);
        test("Quads                      ", Card.CLUBS_8, Card.HEARTS_8, Card.DIAMONDS_8, Card.SPADES_8, Card.SPADES_7, Card.DIAMONDS_7, Card.HEARTS_7);
        test("Trips/FH                   ", Card.CLUBS_K, Card.HEARTS_8, Card.DIAMONDS_8, Card.SPADES_8, Card.SPADES_7, Card.DIAMONDS_7, Card.HEARTS_7);
        test("Trips                      ", Card.CLUBS_K, Card.HEARTS_8, Card.DIAMONDS_8, Card.SPADES_8, Card.SPADES_7, Card.DIAMONDS_J, Card.HEARTS_3);
        test("2 Pair/ST                  ", Card.CLUBS_8, Card.HEARTS_8, Card.DIAMONDS_6, Card.SPADES_5, Card.SPADES_7, Card.DIAMONDS_7, Card.HEARTS_4);
        test("Pair                       ", Card.CLUBS_8, Card.HEARTS_A, Card.DIAMONDS_6, Card.SPADES_5, Card.SPADES_K, Card.DIAMONDS_2, Card.HEARTS_6);
        test("High Card                  ", Card.CLUBS_8, Card.HEARTS_A, Card.DIAMONDS_6, Card.SPADES_5, Card.SPADES_K, Card.DIAMONDS_2, Card.HEARTS_Q);
        test("Straight Flush (low hearts)", Card.HEARTS_A, Card.HEARTS_2, Card.HEARTS_3, Card.HEARTS_4, Card.HEARTS_5, Card.CLUBS_A, Card.SPADES_A);
        test("Straight Flush (+1 hearts) ", Card.HEARTS_A, Card.HEARTS_2, Card.HEARTS_3, Card.HEARTS_4, Card.HEARTS_5, Card.HEARTS_6, Card.SPADES_A);
        test("Straight (5 high)          ", Card.CLUBS_2, Card.HEARTS_A, Card.HEARTS_3, Card.SPADES_4, Card.DIAMONDS_5, Card.CLUBS_A, Card.SPADES_A);
        test("Straight (6 high)          ", Card.CLUBS_2, Card.HEARTS_A, Card.HEARTS_3, Card.SPADES_4, Card.DIAMONDS_5, Card.CLUBS_6, Card.SPADES_A);
    }
    
    private static PokerPlayer testPlayer;
    private static void test(String sName, Card c1, Card c2, Card c3, Card c4, Card c5, Card c6, Card c7)
    {
        HandSorted hand = new HandSorted();
        testPlayer.setName(sName);
        if (c1 != null) hand.addCard(c1);
        if (c2 != null) hand.addCard(c2);
        if (c3 != null) hand.addCard(c3);
        if (c4 != null) hand.addCard(c4);
        if (c5 != null) hand.addCard(c5);
        if (c6 != null) hand.addCard(c6);
        if (c7 != null) hand.addCard(c7);
        HandInfo info = new HandInfo(testPlayer, hand, null);
        System.out.println(sName + " - " + info);
    }
    

    public String toString()
    {
        return best_ + " " + getHandTypeDesc() + " (score=" + nScore_ + ")";
    }

    /**
     * debug
     */
    public String toStringDebug()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\n--- "+player_.getName()+" ---- " + all_ +"\n");

        // normal to string
        sb.append(this);
        sb.append("\n");

        // suits
        sb.append("Spades: " + nSpades_ + "  Hearts: " + nHearts_ +
                "  Diamonds: " + nDiamonds_ + "  Clubs: " + nClubs_ + "\n");

        // ranks
        String sValue;
        for (int i = 2; i <= Card.ACE; i++)
        {
            if (i <= 10) sValue = Integer.toString(i);
            else if (i == Card.JACK) sValue = "J";
            else if (i == Card.QUEEN) sValue = "Q";
            else if (i == Card.KING) sValue = "K";
            else /* if (i == Card.ACE) */ sValue = "A";
            sb.append("(" + sValue + "=" + nNumRank_[i] +") ");
        }
        sb.append("\n");

        // straight seq
        Hand seq;
        for (int i = 0; i < seq_.size(); i++)
        {
            seq = seq_.get(i);
            sb.append("Seq " + (i+1) +": "+seq+"\n");
        }

        // straight flush seq
        for (int i = 0; i < seqFlush_.size(); i++)
        {
            seq = seqFlush_.get(i);
            sb.append("SeqFlush " + (i+1) +": "+seq+"\n");
        }

        // pairs, quads, trips
        sb.append("Pairs: " + nPairs_ + "  Trips: " + nTrips_ + "  Quads: " + nQuads_);
        return sb.toString();
    }
}
