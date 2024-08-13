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
 * HandInfoFast.java
 *
 * Created on March 31, 2004, 6:00 PM
 */

package com.donohoedigital.games.poker;

import org.apache.log4j.*;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.*;

/**
 *
 * @author  donohoe
 */
public class HandInfoFast 
{
    static Logger logger = Logger.getLogger(HandInfoFast.class);
    
    // num cards
    private static final int NUM_CARDS = 5;

    // pair/trips/quads
    int nScore_;

    int nHighestBoardRank_;
    int nLowestBoardRank_;

    int nBigPairRank_;
    int nSmallPairRank_;

    int nTripsRank_;
    int nQuadsRank_;

    int nHighCardRank_;

    int nOvercardCount_;

    private byte[] nNumRank_ = new byte[Card.ACE + 1];
    private byte[] nGroupings_ = new byte[NUM_CARDS+1];
    private byte[][] nTopGroupings_ = new byte[NUM_CARDS + 1][2];
    private byte TOPGROUPINIT = (byte) 0; // low value is 2 (up to A)
    
    // straight
    private byte nNutStraightHigh_ = -1;
    private byte nStraightHigh_ = 0;
    private byte nStraightSize_;
    private int nStraightDrawOuts_;

    // flush
    private byte[] nNumSuit_ = new byte[CardSuit.NUM_SUITS];
    private long[] nHoleSuitBits_ = new long[CardSuit.NUM_SUITS];
    private byte[] nHoleSuitHigh_ = new byte[CardSuit.NUM_SUITS];
    private byte[] nBoardNumSuit_ = new byte[CardSuit.NUM_SUITS];
    private byte nBiggestSuit_;
    private int nFlushHighRank_;

    private long[] nBoardRankBits_ = new long[CardSuit.NUM_SUITS + 1];

    // straight flush
    private boolean[] bExist_ = new boolean[Card.ACE + 1];

    // hand
    private Hand pocket_ = new Hand(2);
    private Hand community_ = new Hand(5);
    private Hand all_ = new Hand(7);

    private int nBetterFlushCards_;

    private boolean bFlushDraw_ = false;
    private boolean bNutFlushDraw_ = false;
    private boolean b2ndNutFlushDraw_ = false;
    private boolean bWeakFlushDraw_ = false;

    public HandInfoFast()
    {
    }

    public int getHandType()
    {
        return getTypeFromScore(nScore_);
    }

    public Hand getCommunity()
    {
        return community_;
    }

    public Hand getPocket()
    {
        return pocket_;
    }

    public int getLowestBoardRank()
    {
        return nLowestBoardRank_;
    }

    public int getHighestBoardRank()
    {
        return nHighestBoardRank_;
    }

    public int getBigPairRank()
    {
        return nBigPairRank_;
    }

    public int getSmallPairRank()
    {
        return nSmallPairRank_;
    }

    public int getTripsRank()
    {
        return nTripsRank_;
    }

    public int getQuadsRank()
    {
        return nQuadsRank_;
    }

    public int getHighCardRank()
    {
        return nHighCardRank_;
    }

    public int getOvercardCount()
    {
        return nOvercardCount_;
    }

    /**
     * Return hand type from score
     */
    public static int getTypeFromScore(int nScore)
    {
        return nScore / HandInfo.BASE;
    }
    
    /**
     * Get major suit from last getScore() call
     */
    public int getLastMajorSuit()
    {
        return nBiggestSuit_;
    }
 
    /**
     * Get card ranks from score and store in given int array.  The
     * most important cards start at index 0. 
     */
    public static void getCards(int nScore, int[] cards)
    {
        nScore = nScore % HandInfo.BASE;
        int cnt = 0;
        int n;
        for (int i = 16; i >= 0; i -= 4)
        {
            n = (nScore >> i) % 16;
            if (n == 0) continue;
            cards[cnt++] = n;
        }
    }
    
    /**
     * Get cards from score
     */
    public static void debugScore(int nScore)
    {
        int nType = getTypeFromScore(nScore);
        logger.debug(nScore + " TYPE: " + HandInfo.getHandTypeDesc(nType));
        nScore = nScore % HandInfo.BASE;
        int n;
        for (int i = 16; i >= 0; i -= 4)
        {
            n = (nScore >> i);
            n = n % 16;
            if (n == 0)
            {
                //skip
            }
            else
            {
                logger.debug("Card: " + Card.getRank(n));
            }
        }
    }

    public int getScore(Hand pocket, Hand community)
    {
        try
        {
            return getScoreInternal(pocket, community);
        }
        catch (RuntimeException e)
        {
            System.out.println("Caught runtime exception in getScore(" + pocket + ", " + community + ")");

            throw e;
        }
    }

    private int getScoreInternal(Hand pocket, Hand community)
    {
        nScore_ = 0;

        pocket_.clear();
        community_.clear();
        all_.clear();

        if (pocket != null)
        {
            pocket_.addAll(pocket);
            all_.addAll(pocket);
        }

        if (community != null)
        {
            community_.addAll(community);
            all_.addAll(community);
        }

        byte nMaxHandSize = (byte)(all_.size() >= NUM_CARDS ? NUM_CARDS : all_.size());

        boolean bStraight = false;
        boolean bFlush = false;
        int r,c;
        byte rank,suit;

        // init
        for (r=Card.TWO; r <= Card.ACE; r++) nNumRank_[r] = 0;
        for (r=0; r <= NUM_CARDS; r++) nGroupings_[r] = 0;

        for (suit=0; suit < CardSuit.NUM_SUITS; suit++) {
            nNumSuit_[suit] = 0;
            nBoardRankBits_[suit] = 0;
            nHoleSuitBits_[suit] = 0;
            nHoleSuitHigh_[suit] = 0;
            nBoardNumSuit_[suit] = 0;
        }
        nBoardRankBits_[CardSuit.NUM_SUITS] = 0;

        for (int i=0; i < (NUM_CARDS+1); i++) {
            nTopGroupings_[i][0] = TOPGROUPINIT;
            nTopGroupings_[i][1] = TOPGROUPINIT;
        }
        nNutStraightHigh_ = -1;
        nStraightHigh_ = 0;
        nStraightSize_ = 0;
        nStraightDrawOuts_ = 0;

        nHighestBoardRank_ = Card.TWO - 1;
        nLowestBoardRank_ = Card.ACE + 1;

        nOvercardCount_ = 0;
        nBiggestSuit_ = 0;

        // determine num of each rank, check flush
        // descending iteration so that board cards first, permitting overcard identification
        for (r=all_.size()-1; r >=0; r--)
        {
            rank = (byte)all_.getCard(r).getRank();
            suit = (byte)all_.getCard(r).getCardSuit().getRank();

            if (r < pocket_.size())
            {
                if (rank > nHighestBoardRank_)
                {
                    ++nOvercardCount_;
                }

                nHoleSuitBits_[suit] |= 1 << rank;

                if (rank > nHoleSuitHigh_[suit]) {
                    nHoleSuitHigh_[suit] = rank;
                }
            }
            else
            {
                if (rank < nLowestBoardRank_)
                {
                    nLowestBoardRank_ = rank;
                }

                if (rank > nHighestBoardRank_)
                {
                    nHighestBoardRank_ = rank;
                }

                nBoardRankBits_[suit] |= (1 << rank);
                nBoardRankBits_[CardSuit.NUM_SUITS] |= (1 << rank);
                if (rank == Card.ACE) {
                    // low ace
                    nBoardRankBits_[suit] |= 2;
                    nBoardRankBits_[CardSuit.NUM_SUITS] |= 2;
                }

                ++nBoardNumSuit_[suit];
            }

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

        nBetterFlushCards_ = 0;
        nFlushHighRank_ = 0;

        if (bFlush)
        {

            if (nHoleSuitHigh_[nBiggestSuit_] == 0)
            {
                // board flush
                int flushcount = 0;
                for (int flushrank = Card.ACE; flushcount < 5; --flushrank)
                {
                    if ((nBoardRankBits_[nBiggestSuit_] & (1 << flushrank)) == 0)
                    {
                        ++nBetterFlushCards_;
                    }
                    else
                    {
                        ++flushcount;

                        if (nFlushHighRank_ == 0)
                        {
                            nFlushHighRank_ = flushrank;
                        }
                    }
                }
            }
            else
            {
                for (int flushrank = Card.ACE; flushrank > nHoleSuitHigh_[nBiggestSuit_]; --flushrank)
                {
                    if ((nBoardRankBits_[nBiggestSuit_] & (1 << flushrank)) == 0)
                    {
                        ++nBetterFlushCards_;
                    }
                    else if (nFlushHighRank_ == 0)
                    {
                        nFlushHighRank_ = flushrank;
                    }
                }

                // handles cases where the high flush cards are not on the board

                if (nFlushHighRank_ == 0)
                {
                    nFlushHighRank_ = nHoleSuitHigh_[nBiggestSuit_];
                }
            }
        }

        bFlushDraw_ = false;
        bNutFlushDraw_ = false;
        b2ndNutFlushDraw_ = false;
        bWeakFlushDraw_ = false;

        // check flush draws
        if (!bFlush && (nNumSuit_[nBiggestSuit_] == 4))
        {
            bFlushDraw_ = true;
            if (nBoardNumSuit_[nBiggestSuit_] == 4)
            {
                // board flush draw
            }
            else
            {
                switch (nHoleSuitHigh_[nBiggestSuit_]) {
                    case Card.ACE:
                        bNutFlushDraw_ = true;
                        break;
                    case Card.KING:
                        if (nBoardRankBits_[nBiggestSuit_] >= 16384) // 1 << A
                        {
                            bNutFlushDraw_ = true;
                        } else {
                            b2ndNutFlushDraw_ = true;
                        }
                        break;
                    case Card.QUEEN:
                        switch((int)(nBoardRankBits_[nBiggestSuit_] >> Card.KING))
                        {
                            case 3: // AK on board
                                bNutFlushDraw_ = true;
                                break;
                            case 2: // A on board
                            case 1: // K on board
                                b2ndNutFlushDraw_ = true;
                                break;
                            default:
                                bWeakFlushDraw_ = true;
                                break;
                        }
                        break;
                    case Card.JACK:
                        switch ((int) (nBoardRankBits_[nBiggestSuit_] >> Card.QUEEN))
                        {
                            case 7: // AKQ on board
                                bNutFlushDraw_ = true;
                                break;
                            case 6: // AK on board
                            case 5: // AQ on board
                            case 3: // KQ on board
                                b2ndNutFlushDraw_ = true;
                                break;
                            default:
                                bWeakFlushDraw_ = true;
                                break;
                        }
                        break;
                    case Card.TEN:
                        switch ((int) (nBoardRankBits_[nBiggestSuit_] >> Card.JACK))
                        {
                            case 15: // AKQJ on board
                                bNutFlushDraw_ = true;
                                break;
                            case 14: // AKQ on board
                            case 13: // AKJ on board
                            case 11: // AQJ on board
                            case 7: // KQJ on board
                                b2ndNutFlushDraw_ = true;
                                break;
                            default:
                                bWeakFlushDraw_ = true;
                                break;
                        }
                        break;
                    case Card.NINE:
                        switch ((int) (nBoardRankBits_[nBiggestSuit_] >> Card.TEN))
                        {
                            case 30: // AKQJ on board
                            case 29: // AKQT on board
                            case 27: // AKJT on board
                            case 23: // AQJT on board
                            case 15: // KQJT on board
                                b2ndNutFlushDraw_ = true;
                                break;
                            default:
                                bWeakFlushDraw_ = true;
                                break;
                        }
                        break;
                    default:
                        bWeakFlushDraw_ = true;
                        break;
                }
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
                if (!bStraight) {

                    long boardBits = (nBoardRankBits_[CardSuit.NUM_SUITS] >> (r - nStraightSize_)) & 31;
                    boolean pocketOver = (r - nStraightSize_ <= Card.NINE) && (nNumRank_[r+5-nStraightSize_] != 0) &&
                            ((nBoardRankBits_[CardSuit.NUM_SUITS] >> (r - nStraightSize_ - 1) & 1) == 0);

                    switch (nStraightSize_) {
                        case 4:
                            // add in wheel draws for the ace with 2345
                            if (r == 6)
                            {
                                nStraightDrawOuts_ += (bFlushDraw_ ? 3 : 4);
                            }
                            // exclude ass-end draws, and draws that don't involve pocket
                            if (pocketOver || ((boardBits != 15) && (boardBits != 14) && (boardBits !=12)))
                            {
                                nStraightDrawOuts_ += (bFlushDraw_ ? 3 : 4);
                                //System.out.println("Hand:" + pocket_ + community_ + " : " + Integer.toString(bFlushDraw_ ? 3 : 4) + " straight outs : " + r);
                            }
                            break;
                        case 3:
                            // exclude ass-end draws, and draws that don't involve pocket
                            if (pocketOver || ((boardBits != 23) && (boardBits != 22) && (boardBits != 20)))
                            {
                                if ((r < Card.ACE) && (nNumRank_[r+1] != 0))
                                {
                                    nStraightDrawOuts_ += (bFlushDraw_ ? 3 : 4);
                                    //System.out.println("Hand:" + pocket_ + community_ + " : " + Integer.toString(bFlushDraw_ ? 3 : 4) + " straight outs : " + r);
                                }
                            }
                            break;
                        case 2:
                            // exclude ass-end draws, and draws that don't involve pocket
                            if (pocketOver || ((boardBits != 27) && (boardBits != 26) && (boardBits != 24)))
                            {
                                if ((r < Card.KING) && (nNumRank_[r + 1] != 0) && (nNumRank_[r + 2] != 0))
                                {
                                    nStraightDrawOuts_ += (bFlushDraw_ ? 3 : 4);
                                    //System.out.println("Hand:" + pocket_ + community_ + " : " + Integer.toString(bFlushDraw_ ? 3 : 4) + " straight outs : " + r);
                                }
                            }
                            break;
                        case 1:
                            // exclude ass-end draws, and draws that don't involve pocket
                            if (pocketOver || ((boardBits != 29) && (boardBits != 28) && (boardBits != 26)))
                            {
                                if ((r < Card.QUEEN) && (nNumRank_[r + 1] != 0) && (nNumRank_[r + 2] != 0) && (nNumRank_[r + 3] != 0))
                                {
                                    nStraightDrawOuts_ += (bFlushDraw_ ? 3 : 4);
                                    //System.out.println("Hand:" + pocket_ + community_ + " : " + Integer.toString(bFlushDraw_ ? 3 : 4) + " straight outs : " + r);
                                }
                            }
                            break;
                        case 0:
                            // exclude ass-end draws, and draws that don't involve pocket
                            if (boardBits != 30)
                            {
                                if ((r < Card.JACK) && (nNumRank_[r + 1] != 0) && (nNumRank_[r + 2] != 0) && (nNumRank_[r + 3] != 0) && (nNumRank_[r + 4] != 0))
                                {
                                    nStraightDrawOuts_ += (bFlushDraw_ ? 3 : 4);
                                }
                            }
                            break;
                    }
                }
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

        nSmallPairRank_ = 0;
        nBigPairRank_ = 0;
        nTripsRank_ = 0;
        nQuadsRank_ = 0;
        nHighCardRank_ = 0;

        // now id type
        int score;

        if ( bStraight && bFlush && isStraightFlush(all_))
        {
            if (nStraightHigh_ == Card.ACE) score = HandInfo.ROYAL_FLUSH * HandInfo.BASE;
            else score = HandInfo.STRAIGHT_FLUSH * HandInfo.BASE;
            score += nStraightHigh_ * HandInfo.H0;
            nSmallPairRank_ = 0;
            nBigPairRank_ = 0;
            nStraightDrawOuts_ = 0;
        }
        else if (nGroupings_[4] != 0)
        {
            score = HandInfo.QUADS * HandInfo.BASE;
            score += nTopGroupings_[4][0] * HandInfo.H1;
            nTopGroupings_[4][1] = TOPGROUPINIT;    // just in case 2 sets quads
            score += getKickers(1, nTopGroupings_[4], HandInfo.H0);
            nQuadsRank_ = nTopGroupings_[4][0];
            nStraightDrawOuts_ = 0;
        }
        else if (nGroupings_[3]>=2)
        {
            score = HandInfo.FULL_HOUSE * HandInfo.BASE;
            score += nTopGroupings_[3][0] * HandInfo.H1;
            score += nTopGroupings_[3][1] * HandInfo.H0;
            nTripsRank_ = nTopGroupings_[3][0];
            nBigPairRank_ = nTopGroupings_[3][1];
            nSmallPairRank_ = nTopGroupings_[3][1];
            nStraightDrawOuts_ = 0;
        }
        else if (nGroupings_[3]==1 && nGroupings_[2]!=0)
        {
            score = HandInfo.FULL_HOUSE * HandInfo.BASE;
            score += nTopGroupings_[3][0] * HandInfo.H1;
            score += nTopGroupings_[2][0] * HandInfo.H0;
            nTripsRank_ = nTopGroupings_[3][0];
            nBigPairRank_ = nTopGroupings_[2][0];
            nSmallPairRank_ = nTopGroupings_[2][0];
            nStraightDrawOuts_ = 0;
        }
        else if (bFlush)
        {
            score = HandInfo.FLUSH * HandInfo.BASE;
            score += getFlushKickers(all_, 5, nBiggestSuit_, HandInfo.H4);
            nStraightDrawOuts_ = 0;
        }
        else if (bStraight)
        {
            score = HandInfo.STRAIGHT * HandInfo.BASE;
            score += nStraightHigh_ * HandInfo.H0;
            nStraightDrawOuts_ = 0;
        }
        else if (nGroupings_[3]==1)
        {
            score = HandInfo.TRIPS * HandInfo.BASE;
            score += nTopGroupings_[3][0] * HandInfo.H2;
            score += getKickers(nMaxHandSize-3, nTopGroupings_[3], HandInfo.H1);
            nTripsRank_ = nTopGroupings_[3][0];
        }
        else if (nGroupings_[2]>=2)
        {
            score = HandInfo.TWO_PAIR * HandInfo.BASE;
            score += nTopGroupings_[2][0] * HandInfo.H2;
            score += nTopGroupings_[2][1] * HandInfo.H1;
            score += getKickers(nMaxHandSize-4, nTopGroupings_[2], HandInfo.H0);
            nBigPairRank_ = nTopGroupings_[2][0];
            nSmallPairRank_ = nTopGroupings_[2][1];
        }
        else if (nGroupings_[2]==1)
        {
            score = HandInfo.PAIR * HandInfo.BASE;
            score += nTopGroupings_[2][0] * HandInfo.H3;
            score += getKickers(nMaxHandSize-2, nTopGroupings_[2], HandInfo.H2);
            nBigPairRank_ = nTopGroupings_[2][0];
            nSmallPairRank_ = nTopGroupings_[2][0];
        }
        else
        {
            score = HandInfo.HIGH_CARD * HandInfo.BASE;
            score += getKickers(nMaxHandSize, nTopGroupings_[2], HandInfo.H4);
            nHighCardRank_ = getKickers(1, nTopGroupings_[2], 1);
        }

        nScore_ = score;

        return score;
    }
        
    /**
     * Does this hand have a straight flush?
     */
    private boolean isStraightFlush(Hand h) 
    {
        // init
        int i;
        for (i=Card.TWO; i <= Card.ACE; i++) bExist_[i] = false;

        // note which cards in hand are from flush suit
        for (i=0; i < h.size(); i++)
        {
            if (h.getCard(i).getCardSuit().getRank() == nBiggestSuit_)
            {
                    bExist_[h.getCard(i).getRank()] = true;
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
    private int getFlushKickers(Hand h, int nNumKickers, byte suit, int H_start) 
    {
            int i;
            int value=0;

            for (i=Card.TWO; i <= Card.ACE; i++) bExist_[i] = false;

            for (i=0;i<h.size();i++)
            {
                   if (h.getCard(i).getCardSuit().getRank() == suit)
                   {
                            bExist_[h.getCard(i).getRank()] = true;
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

    public boolean hasNutFlushDraw()
    {
        return bNutFlushDraw_;
    }

    public boolean has2ndNutFlushDraw()
    {
        return b2ndNutFlushDraw_;
    }

    public boolean hasWeakFlushDraw()
    {
        return bWeakFlushDraw_;
    }

    public boolean hasFlushDraw()
    {
        return bFlushDraw_;
    }

    public int getFlushDrawPocketsPlayed() {
        return 4-nBoardNumSuit_[nBiggestSuit_];
    }

    public boolean hasStraightDraw()
    {
        return nStraightDrawOuts_ > 0;
    }

    public int getStraightDrawOuts()
    {
        return nStraightDrawOuts_;
    }

    public int getBetterFlushCardCount()
    {
        return nBetterFlushCards_;
    }

    public int  getNutStraightHighRank()
    {
        // lazy eval
        if (nNutStraightHigh_ < 0)
        {
            nNutStraightHigh_ = 0;

            int cards =
                    (community_.containsRank(Card.ACE) ? 1 : 0) +
                    (community_.containsRank(Card.KING) ? 1 : 0) +
                    (community_.containsRank(Card.QUEEN) ? 1 : 0) +
                    (community_.containsRank(Card.JACK) ? 1 : 0);

            for (int rank = Card.ACE; rank >= Card.SIX; --rank)
            {
                cards += (community_.containsRank(rank - 4) ? 1 : 0);

                if (cards > 2)
                {
                    nNutStraightHigh_ = (byte)rank;
                    break;
                }

                cards -= (community_.containsRank(rank) ? 1 : 0);
            }

            if (nNutStraightHigh_ == 0)
            {
                cards += (community_.containsRank(Card.ACE) ? 1 : 0);

                if (cards > 2)
                {
                    nNutStraightHigh_ = (byte)Card.FIVE;
                }
            }
        }

        return nNutStraightHigh_;
    }

    public int getStraightHighRank()
    {
        return nStraightHigh_;
    }

    public int getStraightLowRank()
    {
        return nStraightHigh_ - 4;
    }

    public int getFlushHighRank()
    {
        return nFlushHighRank_;
    }

    public String toString()
    {
        return toString(", ", true);
    }

    public String toString(String divider, boolean bLongNames)
    {
        StringBuilder buf = new StringBuilder();

        int type = getTypeFromScore(nScore_);

        buf.append(HandInfo.getHandTypeDesc(type));

        switch (type)
        {
            case HandInfo.HIGH_CARD:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getHighCardRank(), bLongNames, false)));
                break;
            case HandInfo.PAIR:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getBigPairRank(), bLongNames, true)));
                break;
            case HandInfo.TWO_PAIR:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getBigPairRank(), bLongNames, true),
                        getCard(getSmallPairRank(), bLongNames, true)));
                break;
            case HandInfo.TRIPS:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getTripsRank(), bLongNames, true)));
                break;
            case HandInfo.STRAIGHT:
            case HandInfo.STRAIGHT_FLUSH:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getStraightLowRank(), bLongNames, false),
                        getCard(getStraightHighRank(), bLongNames, false)));
                break;
            case HandInfo.FLUSH:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getFlushHighRank(), bLongNames, false)));
                break;
            case HandInfo.FULL_HOUSE:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getTripsRank(), bLongNames, true),
                        getCard(getBigPairRank(), bLongNames, true)));
                break;
            case HandInfo.QUADS:
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        getCard(getQuadsRank(), bLongNames, true)));
                break;
            case HandInfo.ROYAL_FLUSH:
                break;
        }

        return buf.toString();
    }

    private String getCard(int nRank, boolean bLongNames, boolean bPlural)
    {
        if (bLongNames)
        {
            String sKey = bPlural?"msg.cardrank.plural."+nRank:"msg.cardrank.singular."+nRank;
            return PropertyConfig.getMessage(sKey);
        }
        else
        {
            String sKey = bPlural?"msg.cardrank.plural":"msg.cardrank.singular";
            return PropertyConfig.getMessage(sKey, Card.getRank(nRank));
        }
    }
}
