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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.*;

/**
 *
 * @author  donohoe
 */
public class HandInfoFaster
{
    // num cards
    private static final int NUM_CARDS = 5;

    // pair/trips/quads
    private byte[] nNumRank_ = new byte[Card.ACE + 1];
    private byte[] nGroupings_ = new byte[NUM_CARDS+1];
    private byte[][] nTopGroupings_ = new byte[NUM_CARDS+1][2];
    private byte TOPGROUPINIT = (byte) 0; // low value is 2 (up to A)
    
    // straight
    private byte nStraightHigh_ = 0;
    private byte nStraightSize_;
    
    // flush
    private byte[] nNumSuit_ = new byte[CardSuit.NUM_SUITS];
    private byte nBiggestSuit_ = 0;
    
    // straight flush
    private boolean[] bExist_ = new boolean[Card.ACE + 1];
        
    // hand
    private SimpleHand all_ = new SimpleHand();

    // local class for hand
    private static class SimpleHand
    {
        SimpleCard cards[] = new SimpleCard[7];
        int size = 0;

        public SimpleHand()
        {
            for (int i = 0; i < cards.length; i++)
            {
                cards[i] = new SimpleCard();
            }
        }

    }

    // local class for card
    private static class SimpleCard
    {
        int suit;
        int rank;
    }

    /**
     * Get score
     */
    public int getScore(Hand h, Hand c)
    {
        all_.size = 0;
        // do here (inlining can be faster)
        for (int i = h.size()-1; i >= 0; i--)
        {
            Card c1 = h.getCard(i);
            all_.cards[all_.size].suit = c1.getSuit();
            all_.cards[all_.size].rank = c1.getRank();
            all_.size++;
        }
        for (int i1 = c.size()-1; i1 >= 0; i1--)
        {
            Card c1 = c.getCard(i1);
            all_.cards[all_.size].suit = c1.getSuit();
            all_.cards[all_.size].rank = c1.getRank();
            all_.size++;
        }
        return getScore(all_);
    }

    /**
     * to string - should not be used
     */
    @Override
    public String toString()
    {
        return "Don't use HandInfoFaster.toString() - use HandInfoFast";
    }

    /**
     * Get major suit from last getScore() call
     */
    public int getLastMajorSuit()
    {
        return nBiggestSuit_;
    }

    /**
     * Get score - not thread safe for perf reasons
     */
    public int getScore(SimpleHand h)
    {
        byte nMaxHandSize = (byte)(h.size >= NUM_CARDS ? NUM_CARDS : h.size);
        
        boolean bStraight = false;
        boolean bFlush = false;
        int r,c;
        byte rank,suit;

        // init
        for (r=Card.TWO; r <= Card.ACE; r++) nNumRank_[r] = 0; 
        for (r=0; r <= NUM_CARDS; r++) nGroupings_[r] = 0;
        for (r=0; r < CardSuit.NUM_SUITS; r++) nNumSuit_[r] = 0;
        for (int i=0; i < (NUM_CARDS+1); i++) {
            nTopGroupings_[i][0] = TOPGROUPINIT;
            nTopGroupings_[i][1] = TOPGROUPINIT;
        }
        nStraightHigh_ = 0;
        nStraightSize_ = 0;
        
        // determine num of each rank, check flush
        for (r=0; r < h.size; r++)
        {
            rank = (byte)h.cards[r].rank;
            suit = (byte)h.cards[r].suit;

            nNumRank_[rank]++;
            
            nGroupings_[nNumRank_[rank]]++;
            if (nNumRank_[rank] != 0) {
                nGroupings_[nNumRank_[rank]-1]--;
            }
            
            if ((++nNumSuit_[suit]) >= NUM_CARDS)
            {
                bFlush = true;
            }

            if (nNumSuit_[suit] > nNumSuit_[nBiggestSuit_])
            {
                nBiggestSuit_ = suit;
            }
        }
        
        // Ace present for low straight
        nStraightSize_ = (byte)(nNumRank_[Card.ACE] != 0 ? 1 : 0);

        // check for straight and pair data
        for (r=Card.TWO; r <= Card.ACE; r++) 
        {
            // check straight
            if (nNumRank_[r] != 0) 
            {
                if ((++nStraightSize_) >= NUM_CARDS ) 
                {
                    bStraight = true;
                    nStraightHigh_ = (byte)r;
                }
            } else {
                nStraightSize_ = 0;
            }

            // get top set of each type (pair,trips,quads,etc)
            c = nNumRank_[r];
            if ( c != 0 ) 
            {
                nTopGroupings_[c][1] = nTopGroupings_[c][0];
                nTopGroupings_[c][0] = (byte)r;
            }
        }

        // now id type
        int score;
  
        if ( bStraight && bFlush && isStraightFlush(h)) 
        {
            if (nStraightHigh_ == Card.ACE) score = HandInfo.ROYAL_FLUSH * HandInfo.BASE;
            else score = HandInfo.STRAIGHT_FLUSH * HandInfo.BASE;
            score += nStraightHigh_ * HandInfo.H0;
        } 
        else if (nGroupings_[4] != 0) 
        {
            score = HandInfo.QUADS * HandInfo.BASE;
            score += nTopGroupings_[4][0] * HandInfo.H1;
            nTopGroupings_[4][1] = TOPGROUPINIT;    // just in case 2 sets quads
            score += getKickers(1, nTopGroupings_[4], HandInfo.H0);
        } 
        else if (nGroupings_[3]>=2) 
        {
            score = HandInfo.FULL_HOUSE * HandInfo.BASE;
            score += nTopGroupings_[3][0] * HandInfo.H1;
            score += nTopGroupings_[3][1] * HandInfo.H0;
        } 
        else if (nGroupings_[3]==1 && nGroupings_[2]!=0) 
        {
            score = HandInfo.FULL_HOUSE * HandInfo.BASE;
            score += nTopGroupings_[3][0] * HandInfo.H1;
            score += nTopGroupings_[2][0] * HandInfo.H0;
        } 
        else if (bFlush) 
        {
            score = HandInfo.FLUSH * HandInfo.BASE;
            score += getFlushKickers(h, 5, nBiggestSuit_, HandInfo.H4);
        } 
        else if (bStraight) 
        {
            score = HandInfo.STRAIGHT * HandInfo.BASE;
            score += nStraightHigh_ * HandInfo.H0;
        }
        else if (nGroupings_[3]==1) 
        {
            score = HandInfo.TRIPS * HandInfo.BASE;
            score += nTopGroupings_[3][0] * HandInfo.H2;
            score += getKickers(nMaxHandSize-3, nTopGroupings_[3], HandInfo.H1);
        }
        else if (nGroupings_[2]>=2) 
        {
            score = HandInfo.TWO_PAIR * HandInfo.BASE;
            score += nTopGroupings_[2][0] * HandInfo.H2;
            score += nTopGroupings_[2][1] * HandInfo.H1;
            score += getKickers(nMaxHandSize-4, nTopGroupings_[2], HandInfo.H0);
        }
        else if (nGroupings_[2]==1) 
        {
            score = HandInfo.PAIR * HandInfo.BASE;
            score += nTopGroupings_[2][0] * HandInfo.H3;
            score += getKickers(nMaxHandSize-2, nTopGroupings_[2], HandInfo.H2);
        }
        else 
        {
            score = HandInfo.HIGH_CARD * HandInfo.BASE;
            score += getKickers(nMaxHandSize, nTopGroupings_[2], HandInfo.H4);
        }
        return score;
    }

    /**
     * Does this hand have a straight flush?
     */
    private boolean isStraightFlush(SimpleHand h)
    {
        // init
        int i;
        for (i=Card.TWO; i <= Card.ACE; i++) bExist_[i] = false;

        // note which cards in hand are from flush suit
        for (i=0; i < h.size; i++)
        {
            if (h.cards[i].suit == nBiggestSuit_)
            {
                    bExist_[h.cards[i].rank] = true;
            }
        }

        // check for straight
        int straight = bExist_[Card.ACE] ? 1 : 0;
        byte high = 0;
        for (i=Card.TWO; i <= Card.ACE; i++) 
        {
            if (bExist_[i])
            {
                if ((++straight) >= NUM_CARDS) {
                        high = (byte)i;
                }
            } else {
                straight = 0;
            }
        }
        
        /// if high is set, we have a straight
        if (high == 0) return false;
        
        // store new high card
        nStraightHigh_ = high;
        return true;
    }

    /**
     * Get kickers
     */
    private int getKickers(int nNumKickers, byte[] not_allowed, int H_start) 
    {
        int i = Card.ACE;
        int value=0;
        while (nNumKickers != 0)
        {
            while (nNumRank_[i]==0 || i==not_allowed[0] || i==not_allowed[1]) {
                i--;
            }
            nNumKickers--;
            value += (i * H_start);
            H_start = (H_start >> 4);
            i--;
        }
        return value;
    }

    /**
     * Get suited kickers
     */
    private int getFlushKickers(SimpleHand h, int nNumKickers, byte suit, int H_start)
    {
        int i;
        int value=0;

        for (i=Card.TWO; i <= Card.ACE; i++) bExist_[i] = false;

        for (i=0; i < h.size; i++)
        {
           if (h.cards[i].suit == suit)
           {
                bExist_[h.cards[i].rank] = true;
           }
        }

        i = Card.ACE;
        while (nNumKickers != 0) {
            while (bExist_[i] == false) i--;
            nNumKickers--;
            value += (i * H_start);
            H_start = (H_start >> 4);
            i--;
        }
        return value;
    }
}
