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
 * Card.java
 *
 * Created on December 29, 2003, 6:10 PM
 */

package com.donohoedigital.games.poker.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;

/**
 *
 * @author  Doug Donohoe
 */
@DataCoder('C')
public class Card extends com.ddpoker.Card implements DataMarshal, Comparable<Card>
{
    // unknown rank
    public static final int UNKNOWN = 0;

    public static final Card SPADES_2 = new Card(CardSuit.SPADES, TWO);
    public static final Card SPADES_3 = new Card(CardSuit.SPADES, THREE);
    public static final Card SPADES_4 = new Card(CardSuit.SPADES, FOUR);
    public static final Card SPADES_5 = new Card(CardSuit.SPADES, FIVE);
    public static final Card SPADES_6 = new Card(CardSuit.SPADES, SIX);
    public static final Card SPADES_7 = new Card(CardSuit.SPADES, SEVEN);
    public static final Card SPADES_8 = new Card(CardSuit.SPADES, EIGHT);
    public static final Card SPADES_9 = new Card(CardSuit.SPADES, NINE);
    public static final Card SPADES_T = new Card(CardSuit.SPADES, TEN);
    public static final Card SPADES_J = new Card(CardSuit.SPADES, JACK);
    public static final Card SPADES_Q = new Card(CardSuit.SPADES, QUEEN);
    public static final Card SPADES_K = new Card(CardSuit.SPADES, KING);
    public static final Card SPADES_A = new Card(CardSuit.SPADES, ACE);

    public static final Card HEARTS_2 = new Card(CardSuit.HEARTS, TWO);
    public static final Card HEARTS_3 = new Card(CardSuit.HEARTS, THREE);
    public static final Card HEARTS_4 = new Card(CardSuit.HEARTS, FOUR);
    public static final Card HEARTS_5 = new Card(CardSuit.HEARTS, FIVE);
    public static final Card HEARTS_6 = new Card(CardSuit.HEARTS, SIX);
    public static final Card HEARTS_7 = new Card(CardSuit.HEARTS, SEVEN);
    public static final Card HEARTS_8 = new Card(CardSuit.HEARTS, EIGHT);
    public static final Card HEARTS_9 = new Card(CardSuit.HEARTS, NINE);
    public static final Card HEARTS_T = new Card(CardSuit.HEARTS, TEN);
    public static final Card HEARTS_J = new Card(CardSuit.HEARTS, JACK);
    public static final Card HEARTS_Q = new Card(CardSuit.HEARTS, QUEEN);
    public static final Card HEARTS_K = new Card(CardSuit.HEARTS, KING);
    public static final Card HEARTS_A = new Card(CardSuit.HEARTS, ACE);

    public static final Card DIAMONDS_2 = new Card(CardSuit.DIAMONDS, TWO);
    public static final Card DIAMONDS_3 = new Card(CardSuit.DIAMONDS, THREE);
    public static final Card DIAMONDS_4 = new Card(CardSuit.DIAMONDS, FOUR);
    public static final Card DIAMONDS_5 = new Card(CardSuit.DIAMONDS, FIVE);
    public static final Card DIAMONDS_6 = new Card(CardSuit.DIAMONDS, SIX);
    public static final Card DIAMONDS_7 = new Card(CardSuit.DIAMONDS, SEVEN);
    public static final Card DIAMONDS_8 = new Card(CardSuit.DIAMONDS, EIGHT);
    public static final Card DIAMONDS_9 = new Card(CardSuit.DIAMONDS, NINE);
    public static final Card DIAMONDS_T = new Card(CardSuit.DIAMONDS, TEN);
    public static final Card DIAMONDS_J = new Card(CardSuit.DIAMONDS, JACK);
    public static final Card DIAMONDS_Q = new Card(CardSuit.DIAMONDS, QUEEN);
    public static final Card DIAMONDS_K = new Card(CardSuit.DIAMONDS, KING);
    public static final Card DIAMONDS_A = new Card(CardSuit.DIAMONDS, ACE);

    public static final Card CLUBS_2 = new Card(CardSuit.CLUBS, TWO);
    public static final Card CLUBS_3 = new Card(CardSuit.CLUBS, THREE);
    public static final Card CLUBS_4 = new Card(CardSuit.CLUBS, FOUR);
    public static final Card CLUBS_5 = new Card(CardSuit.CLUBS, FIVE);
    public static final Card CLUBS_6 = new Card(CardSuit.CLUBS, SIX);
    public static final Card CLUBS_7 = new Card(CardSuit.CLUBS, SEVEN);
    public static final Card CLUBS_8 = new Card(CardSuit.CLUBS, EIGHT);
    public static final Card CLUBS_9 = new Card(CardSuit.CLUBS, NINE);
    public static final Card CLUBS_T = new Card(CardSuit.CLUBS, TEN);
    public static final Card CLUBS_J = new Card(CardSuit.CLUBS, JACK);
    public static final Card CLUBS_Q = new Card(CardSuit.CLUBS, QUEEN);
    public static final Card CLUBS_K = new Card(CardSuit.CLUBS, KING);
    public static final Card CLUBS_A = new Card(CardSuit.CLUBS, ACE);

    // special "low ace" - used for particular purposes
    public static final Card SPADES_LOW_A = new Card(CardSuit.SPADES, 1);

    // special "unknown card" - used by simulator
    public static final Card BLANK = new Card(CardSuit.UNKNOWN, UNKNOWN);

    // all cards
    private static final Card[] cards_ = new Card[53];

    static
    {
        cards_[BLANK.index_] = BLANK;

        cards_[SPADES_2.index_] = SPADES_2;
        cards_[SPADES_3.index_] = SPADES_3;
        cards_[SPADES_4.index_] = SPADES_4;
        cards_[SPADES_5.index_] = SPADES_5;
        cards_[SPADES_6.index_] = SPADES_6;
        cards_[SPADES_7.index_] = SPADES_7;
        cards_[SPADES_8.index_] = SPADES_8;
        cards_[SPADES_9.index_] = SPADES_9;
        cards_[SPADES_T.index_] = SPADES_T;
        cards_[SPADES_J.index_] = SPADES_J;
        cards_[SPADES_Q.index_] = SPADES_Q;
        cards_[SPADES_K.index_] = SPADES_K;
        cards_[SPADES_A.index_] = SPADES_A;

        cards_[HEARTS_2.index_] = HEARTS_2;
        cards_[HEARTS_3.index_] = HEARTS_3;
        cards_[HEARTS_4.index_] = HEARTS_4;
        cards_[HEARTS_5.index_] = HEARTS_5;
        cards_[HEARTS_6.index_] = HEARTS_6;
        cards_[HEARTS_7.index_] = HEARTS_7;
        cards_[HEARTS_8.index_] = HEARTS_8;
        cards_[HEARTS_9.index_] = HEARTS_9;
        cards_[HEARTS_T.index_] = HEARTS_T;
        cards_[HEARTS_J.index_] = HEARTS_J;
        cards_[HEARTS_Q.index_] = HEARTS_Q;
        cards_[HEARTS_K.index_] = HEARTS_K;
        cards_[HEARTS_A.index_] = HEARTS_A;

        cards_[DIAMONDS_2.index_] = DIAMONDS_2;
        cards_[DIAMONDS_3.index_] = DIAMONDS_3;
        cards_[DIAMONDS_4.index_] = DIAMONDS_4;
        cards_[DIAMONDS_5.index_] = DIAMONDS_5;
        cards_[DIAMONDS_6.index_] = DIAMONDS_6;
        cards_[DIAMONDS_7.index_] = DIAMONDS_7;
        cards_[DIAMONDS_8.index_] = DIAMONDS_8;
        cards_[DIAMONDS_9.index_] = DIAMONDS_9;
        cards_[DIAMONDS_T.index_] = DIAMONDS_T;
        cards_[DIAMONDS_J.index_] = DIAMONDS_J;
        cards_[DIAMONDS_Q.index_] = DIAMONDS_Q;
        cards_[DIAMONDS_K.index_] = DIAMONDS_K;
        cards_[DIAMONDS_A.index_] = DIAMONDS_A;

        cards_[CLUBS_2.index_] = CLUBS_2;
        cards_[CLUBS_3.index_] = CLUBS_3;
        cards_[CLUBS_4.index_] = CLUBS_4;
        cards_[CLUBS_5.index_] = CLUBS_5;
        cards_[CLUBS_6.index_] = CLUBS_6;
        cards_[CLUBS_7.index_] = CLUBS_7;
        cards_[CLUBS_8.index_] = CLUBS_8;
        cards_[CLUBS_9.index_] = CLUBS_9;
        cards_[CLUBS_T.index_] = CLUBS_T;
        cards_[CLUBS_J.index_] = CLUBS_J;
        cards_[CLUBS_Q.index_] = CLUBS_Q;
        cards_[CLUBS_K.index_] = CLUBS_K;
        cards_[CLUBS_A.index_] = CLUBS_A;
    }

    /**
     * Internal storage of card rank.  Package access for performance (methods are slower).
     */
    int rank_;

    /**
     * Internal storage of card suit.  Package access for performance (methods are slower).
     */
    int suit_;
    private CardSuit cardSuit_;

    // calculated values
    private int index_;
    private long fingerprint_;

    /**
     * Empty constructor for saving
     */
    public Card()
    {
    }
    
    /** 
     * Creates a new instance of Card 
     */
    public Card(CardSuit suit, int rank)
    {
        cardSuit_ = suit;
        suit_ = suit.getRank();
        rank_ = rank;
        index_ = computeIndex();
        fingerprint_ = 1L << index_;
    }

    /**
     * Copy value of a card.  Warning:  Use carefully!!!  The fingerprint of any hands
     * this card is in will not be updated when this happens.  You must call
     * cardsChanged() on the Hand after using this.  
     */
    public void setValue(Card c)
    {
        cardSuit_ = c.cardSuit_;
        suit_ = c.suit_;
        rank_ = c.rank_;
        index_ = c.index_;
        fingerprint_ = c.fingerprint_;
    }

    public static String getRankSingle(int rank)
    {
        if (rank == 10) return getDisplayT();
        else return getRank(rank);
    }

    public static int getRank(char cRank)
    {
        switch (cRank)
        {
            case 'A':
            case 'a':
                return ACE;
            case 'K':
            case 'k':
                return KING;
            case 'Q':
            case 'q':
                return QUEEN;
            case 'J':
            case 'j':
                return JACK;
            case 'T':
            case 't':
                return TEN;
            case '9':
                return NINE;
            case '8':
                return EIGHT;
            case '7':
                return SEVEN;
            case '6':
                return SIX;
            case '5':
                return FIVE;
            case '4':
                return FOUR;
            case '3':
                return THREE;
            case '2':
                return TWO;
            default:
                return UNKNOWN;
        }
    }

    public static String getRank(int rank)
    {
        String sRank = null;
        switch (rank)
        {
            case 1: // special use only (low ace)
            case ACE:
                sRank = getDisplayA();
                break;
                
            case KING:
                sRank = getDisplayK();
                break;
                
            case QUEEN:
                sRank = getDisplayQ();
                break;
                
            case JACK:
                sRank = getDisplayJ();
                break;
                
            case 10:
            case 9:
            case 8:
            case 7:
            case 6:
            case 5:
            case 4:
            case 3:
            case 2:
                sRank = Integer.toString(rank);
                break;

            case UNKNOWN:
                sRank = getDisplayUnknown();
                break;

            default:
                ApplicationError.assertTrue(false, "Invalid rank: " + rank);
        }
        return sRank;
    }
    
    public String getDisplay()
    {
        return getRank(rank_) + cardSuit_.getAbbr();
    }

    public String getRankDisplay()
    {
        return getRank(rank_);
    }
    
    /**
     * Return rank as single char (used in debug output)
     */
    public String getRankDisplaySingle()
    {
        if (rank_ == 10) return "T";
        return getRank(rank_);
    }

    @Override
    public String toString()
    {
        return getDisplay();
    }

    public String toStringSingle()
    {
        return getRankDisplaySingle() + getSuitDisplay();
    }

    public String toHTML()
    {
        return "<DDCARD CARD=\"" + getRankDisplaySingle() + getSuitDisplay() + "\">";
    }

    public CardSuit getCardSuit()
    {
        return cardSuit_;
    }
    
    public String getSuitDisplay()
    {
        return cardSuit_.getAbbr();
    }
    
    public boolean isHearts()
    {
        return cardSuit_ == CardSuit.HEARTS;
    }
    
    public boolean isDiamonds()
    {
        return cardSuit_ == CardSuit.DIAMONDS;
    }
    
    public boolean isClubs()
    {
        return cardSuit_ == CardSuit.CLUBS;
    }
        
    public boolean isSpades()
    {
        return cardSuit_ == CardSuit.SPADES;
    }
    
    public boolean isSameSuit(Card c)
    {
        return c.cardSuit_ == cardSuit_;
    }
    
    public boolean isSameRank(Card c)
    {
        return c.rank_ == rank_;
    }
    
    public boolean isFaceCard()
    {
        return rank_ >= JACK && rank_ <= KING;
    }

    public boolean isBlank()
    {
        return rank_ == UNKNOWN && cardSuit_ == CardSuit.UNKNOWN;
    }

    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        cardSuit_ = CardSuit.forRank(list.removeIntToken());
        suit_ = cardSuit_.getRank();
        rank_  = list.removeIntToken();
        index_ = computeIndex();
        fingerprint_ = 1L << index_;
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(cardSuit_.getRank());
        list.addToken(rank_);
        return list.marshal(state);
    }

    /**
     * Is given card higher than this card?
     */
    public boolean isGreaterThan(Card c)
    {
        return compareTo(c) > 0;
    }
    
    /**
     * Is given card less than this card
     */
    public boolean isLessThan(Card c)
    {
        return compareTo(c) < 0;
    }
    
    /**
     * Comparable interface for sorting
     */
    public int compareTo(Card c)
    {
        if (c == null) return Integer.MAX_VALUE;

        if (rank_ == c.rank_)
        {
            return cardSuit_.compareTo(c.cardSuit_);
        }
        
        return rank_ - c.rank_;
    }
    
    /**
     * Equals
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Card)) return false;
        
        Card c = (Card) o;
        return (rank_ == c.rank_ && cardSuit_ == c.cardSuit_);
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode()
    {
        return 31 * cardSuit_.hashCode() + rank_;
    }

    public static Card getCard(String card)
    {
        if (card == null) return Card.BLANK;
        
        int suit = CardSuit.UNKNOWN_RANK;

        switch (card.charAt(1))
        {
            case 'c':
            case 'C':
                suit = CardSuit.CLUBS_RANK;
                break;
            case 'd':
            case 'D':
                suit = CardSuit.DIAMONDS_RANK;
                break;
            case 'h':
            case 'H':
                suit = CardSuit.HEARTS_RANK;
                break;
            case 's':
            case 'S':
                suit = CardSuit.SPADES_RANK;
                break;
        }

        int rank = getRank(card.charAt(0));

        return getCard(suit, rank);
    }

    public static Card getCard(int index)
    {
        return cards_[index];
    }

    public static Card getCard(int suit, int rank)
    {
        return cards_[computeIndex(suit, rank)];
    }

    public static Card getCard(CardSuit suit, int rank)
    {
        return cards_[computeIndex(suit.getRank(), rank)];
    }

    /**
     * Get index for arrays and hand fingerprinting.
     */
    public int getIndex()
    {
        return index_;
    }

    public long fingerprint()
    {
        return fingerprint_;
    }

    private int computeIndex()
    {
        return computeIndex(suit_, rank_);
    }

    private static int computeIndex(int suit, int rank)
    {
        // blank or otherwise unknown
        if (rank < TWO || rank > ACE || suit < CardSuit.CLUBS_RANK || suit > CardSuit.SPADES_RANK)
        {
            return 52;
        }
        return (rank - 2) * 4 + suit;
    }

    /**
     * Get the numeric value of this card's rank.
     * @return One of the following:
     * <ul>
     * <li>Card.ACE</li>
     * <li>Card.KING</li>
     * <li>Card.QUEEN</li>
     * <li>Card.JACK</li>
     * <li>Card.TEN</li>
     * <li>Card.NINE</li>
     * <li>Card.EIGHT</li>
     * <li>Card.SEVEN</li>
     * <li>Card.SIX</li>
     * <li>Card.FIVE</li>
     * <li>Card.FOUR</li>
     * <li>Card.THREE</li>
     * <li>Card.TWO</li>
     * </ul>
     */
    @Override
    public int getRank()
    {
        return rank_;
    }

    /**
     * Get the numeric value of this card's suit.
     * @return One of the following:
     * <ul>
     * <li>Card.CLUBS</li>
     * <li>Card.DIAMONDS</li>
     * <li>Card.HEARTS</li>
     * <li>Card.SPADES</li>
     * </ul>
     */
    @Override
    public int getSuit()
    {
        return suit_;
    }

    // cache lookup of display to avoid lots of calls to PropertyConfig
    // non thread safe is okay since return value is the same regardless of thread
    private static String DISPLAY_UNKNOWN = null;
    private static String DISPLAY_A = null;
    private static String DISPLAY_K = null;
    private static String DISPLAY_Q = null;
    private static String DISPLAY_J = null;
    private static String DISPLAY_T = null;

    @SuppressWarnings({"NonThreadSafeLazyInitialization"})
    private static String getDisplayUnknown()
    {
        if (DISPLAY_UNKNOWN == null) DISPLAY_UNKNOWN = PropertyConfig.getMessage("msg.card.unknown");
        return DISPLAY_UNKNOWN;
    }

    @SuppressWarnings({"NonThreadSafeLazyInitialization"})
    private static String getDisplayA()
    {
        if (DISPLAY_A == null) DISPLAY_A = PropertyConfig.getMessage("msg.card.ace");
        return DISPLAY_A;
    }

    @SuppressWarnings({"NonThreadSafeLazyInitialization"})
    private static String getDisplayK()
    {
        if (DISPLAY_K == null) DISPLAY_K = PropertyConfig.getMessage("msg.card.king");
        return DISPLAY_K;
    }

    @SuppressWarnings({"NonThreadSafeLazyInitialization"})
    private static String getDisplayQ()
    {
        if (DISPLAY_Q == null) DISPLAY_Q = PropertyConfig.getMessage("msg.card.queen");
        return DISPLAY_Q;
    }

    @SuppressWarnings({"NonThreadSafeLazyInitialization"})
    private static String getDisplayJ()
    {
        if (DISPLAY_J == null) DISPLAY_J = PropertyConfig.getMessage("msg.card.jack");
        return DISPLAY_J;
    }

    @SuppressWarnings({"NonThreadSafeLazyInitialization"})
    private static String getDisplayT()
    {
        if (DISPLAY_T == null) DISPLAY_T = PropertyConfig.getMessage("msg.card.ten");
        return DISPLAY_T;
    }
}
