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
 * Hand.java
 *
 * Created on December 31, 2003, 3:28 PM
 */

package com.donohoedigital.games.poker.engine;

import com.donohoedigital.comms.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
@DataCoder('H')
public class Hand extends DMArrayList<Card>
{
    public static final char TYPE_COLOR_UP = 'c';
    public static final char TYPE_DEAL_HIGH = 'h';
    public static final char TYPE_FACE_UP = 'f';
    public static final char TYPE_NORMAL = 'n';
    
    private char cType_;

    private long fingerprint_ = 0;
    private int fingerprintModCount_ = -1;

    /**
     * Creates a new instance of Hand of TYPE_NORMAL
     */
    public Hand() {
        this(TYPE_NORMAL, 2);
    }
    
    /**
     * Creates a new instance of Hand of TYPE_NORMAL with initial size given
     */
    public Hand(int n)
    {
        this(TYPE_NORMAL, n);
    }
    
    /**
     * Create a new instance of hand of given type
     */
    public Hand(char cType)
    {
        this(cType, 2);
    }
    
    /** 
     * Creates a new instance of Hand 
     */
    public Hand(char cType, int n) {
        super(n);
        cType_ = cType;
    }
    
    /**
     * Creates new hand from two cards
     */
    public Hand(Card one, Card two)
    {
        this(2);
        addCard(one);
        addCard(two);
    }
    
    /**
     * Creates new hand from three cards
     */
    public Hand(Card one, Card two, Card three)
    {
        this(3);
        addCard(one);
        addCard(two);
        addCard(three);
    }
    
    /**
     * Creates new hand from four cards
     */
    public Hand(Card one, Card two, Card three, Card four)
    {
        this(4);
        addCard(one);
        addCard(two);
        addCard(three);
        addCard(four);
    }
    
    /**
     * Creates new hand from five cards
     */
    public Hand(Card one, Card two, Card three, Card four, Card five)
    {
        this(5);
        addCard(one);
        addCard(two);
        addCard(three);
        addCard(four);
        addCard(five);
    }

    /** 
     * Create new hand from existing hand
     */
    public Hand(Hand copy)
    {
        super(copy != null ? copy.size() : 2);
        if (copy != null)
        {
            cType_ = copy.cType_;
            addAll(copy);
        }
    }
    
    /**
     * Create new hand from existing hand, up to a maximum number of cards
     */
    public Hand(Hand copy, int cards)
    {
        super(copy != null ? copy.size() : 2);
        if (copy != null)
        {
            cType_ = copy.cType_;
            for (int i = 0; i < cards; ++i)
            {
                add(copy.getCard(i));
            }
        }
    }

    /**
     * Get hand type
     */
    public char getType()
    {
        return cType_;
    }
    
    /**
     * Set hand type
     */
    public void setType(char c)
    {
        cType_ = c;
    }
    
    /**
     * Add card to players hand
     */
    public void addCard(Card card)
    {
        super.add(card);
    }
    
    /**
     * insert card at front of list
     */
    public void insertCard(Card card)
    {
        super.add(0, card);
    }
    
    /**
     * insert card at given index of list
     */
    public void insertCard(Card card, int i)
    {
        super.add(i, card);
    }

    /**
     * safety
     */
    public boolean add(Card o)
    {
        addCard(o);
        return true;
    }

    /**
     * safety
     */
    public void add(int i, Card o)
    {
        throw new RuntimeException("Use addCard please");
    }
    
    /**
     * Get given card
     */
    public Card getCard(int i)
    {
        return get(i);
    }
    
    /**
     * Set given card
     */
    public Card setCard(int i, Card c)
    {
        fingerprintModCount_ = -1;

        return super.set(i, c);
    }
    
    /**
     * Remove given card
     */
    public Card removeCard(int i)
    {
        return remove(i);
    }

    /**
     * remove any blank cards from hand
     */
    public void removeBlank()
    {
        for (int i = size() - 1; i >= 0; i--)
        {
            if (getCard(i).isBlank()) removeCard(i);
        }
    }

    /**
     * count number of given card in hand
     */
    public int countCard(Card c)
    {
        int nNum = 0;
        for (int i = size() - 1; i >= 0; i--)
        {
            if (getCard(i).equals(c)) nNum++;
        }
        return nNum;
    }

    /**
     * order hand
     */
    public void sortAscending()
    {
        Collections.sort(this);
    }
    
    /**
     * order hand
     */
    public void sortDescending()
    {
        Collections.sort(this);
        Collections.reverse(this);        
    }
    
    /**
     * String rep
     */
    public String toStringRankSuit()
    {
        StringBuilder sb = new StringBuilder();
        Card c;
        for (int i = 0; i < size(); i++)
        {
            c = getCard(i);
            
            if (i > 0)
            {
                sb.append(" ");
            }
            sb.append(c.getRankDisplay());
            sb.append(c.getSuitDisplay());
        }
        return sb.toString();
    }
    
    /**
     * String rep
     */
    public String toStringRank()
    {
        StringBuilder sb = new StringBuilder();
        Card c;
        for (int i = 0; i < size(); i++)
        {
            c = getCard(i);
            
            if (i > 0)
            {
                sb.append(" ");
            }
            sb.append(c.getRankDisplay());
        }
        return sb.toString();
    }
    
    /**
     * String rep for sims - ranks only + '*' for suited
     */
    public String toStringSuited()
    {
        StringBuilder sb = new StringBuilder("[");
        Card c;
        for (int i = 0; i < size(); i++)
        {
            c = getCard(i);
            
            if (i > 0)
            {
                sb.append(" ");
            }
            
            sb.append(c.getRankDisplaySingle());
        }
        
        sb.append("]");
        
        if (isSuited()) {
            sb.append("*");
        }
        return sb.toString();
    }

    /**
     * Are cards of adjacent rank within the given range?
     */
    public boolean isConnectors(int minRank, int maxRank)
    {
        int rank1 = getCard(0).getRank();
        int rank2 = getCard(1).getRank();

        return (rank1 >= minRank) && (rank1 <= maxRank) &&
               (rank2 >= minRank) && (rank2 <= maxRank) &&
               (Math.abs(rank1 - rank2) == 1);
    }

    /**
     * Are all cards of the same suit?.  If no cards in this hand, returns false.
     */
    public boolean isSuited()
    {
        int nSize = size();
        if (nSize == 0) return false;
        
        Card first = getCard(0);       
        for (int i = 1; i < nSize; i++)
        {
            if (!getCard(i).isSameSuit(first)) return false;
        }
        
        return true;
    }

    /**
     * Returns the highest rank of any card in the hand.
     */
    public int getHighestRank()
    {
        int rank = Card.UNKNOWN;

        for (int i = 0; i < size(); ++i)
        {
            Card card = getCard(i);

            if (card.getRank() > rank)
            {
                rank = card.getRank();
            }

            if (rank == Card.ACE) return rank;
        }

        return rank;
    }

    /**
     * Returns the lowest rank of any card in the hand.
     */
    public int getLowestRank()
    {
        int rank = Card.UNKNOWN;

        for (int i = 0; i < size(); ++i)
        {
            Card card = getCard(i);

            if ((card.getRank() < rank) || (rank == Card.UNKNOWN))
            {
                rank = card.getRank();
            }

            if (rank == Card.TWO) return rank;
        }

        return rank;
    }

    /**
     * return highest number of suited cards
     */
    public int getHighestSuited()
    {
        int nSpades = 0;
        int nClubs = 0;
        int nDiamonds = 0;
        int nHearts = 0;
        
        // count suits
        Card c;
        for (int i = 0; i < size(); i++)
        {
            c = getCard(i);
            if (c.isSpades()) nSpades++;
            if (c.isHearts()) nHearts++;
            if (c.isClubs())  nClubs++;
            if (c.isDiamonds()) nDiamonds++;
        }
        
        return Math.max(Math.max(nSpades, nHearts), Math.max(nClubs, nDiamonds));
    }
    
    /**
     * Are all cards of the same rank?  If no cards in this hand, return false.
     */
    public boolean isRanked()
    {
        int nSize = size();
        if (nSize == 0) return false;
        
        Card first = getCard(0);       
        for (int i = 1; i < nSize; i++)
        {
            if (!getCard(i).isSameRank(first)) return false;
        }
        
        return true;
    }
    
    /**
     * Is this hand a pair?
     */
    public boolean isPair()
    {
        return size() == 2 && isRanked();
    }
    
    /**
     * Is this card rank in this hand?
     */
    public boolean isInHand(int rank)
    {
        int nStart = size() - 1;
        for (;nStart >=0; nStart--)
        {
            if (getCard(nStart).getRank() == rank) return true;
        }
        
        return false;
    }
    
    /**
     * Is this card rank and suit in this hand?
     */
    public boolean isInHand(int rank, int suit)
    {
        Card c;
        int nStart = size() - 1;
        for (;nStart >=0; nStart--)
        {
            c = getCard(nStart);
            if (c.getRank() == rank &&
                c.getCardSuit().getRank() == suit) return true;
        }
        
        return false;
    }

    /**
     * Does hand contain the given card?
     */
    public boolean containsCard(Card card) {
        return (card.fingerprint() & fingerprint()) != 0L;
    }

    /**
     * Does hand contain the given card?
     */
    public boolean containsCard(int index) {
        return ((1L << index) & fingerprint()) != 0L;
    }

    /**
     * Does hand contain any cards of given hand?
     */
    public boolean containsAny(Hand hand) {
        return (hand.fingerprint() & fingerprint()) != 0L;
    }

    /**
     * Does hand contain any card of given rank?
     */
    public boolean containsRank(int rank)
    {
        return (((1L << Card.getCard(CardSuit.CLUBS_RANK, rank).getIndex()) |
                 (1L << Card.getCard(CardSuit.DIAMONDS_RANK, rank).getIndex()) |
                 (1L << Card.getCard(CardSuit.HEARTS_RANK, rank).getIndex()) |
                 (1L << Card.getCard(CardSuit.SPADES_RANK, rank).getIndex())) &
                fingerprint()) > 0L;
    }

    private static final long CLUBS_MASK = 0x1111111111111L;

    /**
     * Does hand contain any card of given suit?
     */
    public boolean containsSuit(int suit)
    {
        return ((CLUBS_MASK << suit) & fingerprint()) > 0L;
    }

    /**
     * Does hand have possible flush?
     */
    public boolean hasPossibleFlush()
    {
        switch (size())
        {
            case 0:
            case 1:
                return false;
            case 2:
                return getCard(0).getSuit() == getCard(1).getSuit() &&
                       getCard(0).getSuit() == getCard(2).getSuit();
            default:
                return getMaxSuitCount() > 2;
        }
    }

    /**
     * Does hand have flush?
     */
    public boolean hasFlush()
    {
        switch (size())
        {
            case 0:
            case 1:
                return false;
            case 2:
                return getCard(0).getSuit() == getCard(1).getSuit() &&
                       getCard(0).getSuit() == getCard(2).getSuit();
            default:
                return getMaxSuitCount() > 4;
        }
    }

    private int getMaxSuitCount()
    {
        int suit[] = new int[CardSuit.NUM_SUITS];
        for (int i = size()-1; i >= 0; --i)
        {
            ++suit[getCard(i).getSuit()];
        }
        int max = 0;
        for (int i = 0; i < CardSuit.NUM_SUITS; ++i) {
            if (suit[i] > max) max = suit[i];
        }
        return max;
    }

    /**
     * Does hand have pair?
     */
    public boolean hasPair()
    {
        switch (size())
        {
            case 0:
            case 1:
                return false;
            case 2:
                return getCard(0).getRank() == getCard(1).getRank();
            default:
                {
                    int rank[] = new int[Card.ACE+1];
                    for (int i = size()-1; i >= 0; --i)
                    {
                        ++rank[getCard(i).getRank()];
                    }
                    for (int i = Card.TWO; i <= Card.ACE; ++i) {
                        if (rank[i] == 2) return true;
                    }
                    return false;
                }
        }
    }

    /**
     * Does hand have trips?
     */
    public boolean hasTrips()
    {
        switch (size())
        {
            case 0:
            case 1:
            case 2:
                return false;
            case 3:
                {
                    int rank = getCard(0).getRank();
                    return getCard(1).getRank() == rank && getCard(2).getRank() == rank;
                }
            default:
                {
                    int rank[] = new int[Card.ACE+1];
                    for (int i = size() - 1; i >= 0; --i)
                    {
                        ++rank[getCard(i).getRank()];
                    }
                    for (int i = Card.TWO; i <= Card.ACE; ++i)
                    {
                        if (rank[i] == 3) return true;
                    }
                    return false;
                }
        }
    }

    /**
     * Does hand have quads?
     */
    public boolean hasQuads()
    {
        switch (size())
        {
            case 0:
            case 1:
            case 2:
            case 3:
                return false;
            case 4:
                {
                    int rank = getCard(0).getRank();
                    return getCard(1).getRank() == rank &&
                           getCard(2).getRank() == rank &&
                           getCard(3).getRank() == rank;
                }
            default:
                {
                    int rank[] = new int[Card.ACE+1];
                    for (int i = size() - 1; i >= 0; --i)
                    {
                        ++rank[getCard(i).getRank()];
                    }
                    for (int i = Card.TWO; i <= Card.ACE; ++i)
                    {
                        if (rank[i] == 4) return true;
                    }
                    return false;
                }
        }
    }

    /////
    ///// SAVE
    /////
    
    /**
     * get type before passing to array list logic
     */
    public void demarshal(MsgState state, String sData)
    {
        cType_ = sData.charAt(0);
        super.demarshal(state, sData.substring(1));
    }
    
    /**
     * add type to array list marshal
     */
    public String marshal(MsgState state) 
    {
        return cType_ + super.marshal(state);
    }

    /**
     * Computes fingerprint for first n cards in hand.
     *
     * If fewer than n cards, returns zero.
     */
    public long fingerprint(int n)
    {
        if (size() < n) return 0L;

        long fingerprint = 0L;

        Card card;

        for (int i = n-1; i >= 0; --i)
        {
            card = getCard(i);
            fingerprint |= (1L << card.getIndex());
        }

        return fingerprint;
    }

    /**
     * notify hand that cards changed (used in simulator)
     */
    public void cardsChanged()
    {
        fingerprintModCount_ = -1;
    }

    /**
     * do fingerprint
     */
    public long fingerprint()
    {
        // recalculate if modified
        if (modCount != fingerprintModCount_)
        {
            int size = size();

            Card card;

            long fingerprint = 0;

            for (int i = 0; i < size; ++i)
            {
                card = getCard(i);
                fingerprint |= (1L << card.getIndex());
            }

            fingerprint_ = fingerprint;
            fingerprintModCount_ = modCount;
        }

        return fingerprint_;
    }

    /**
     * Returns the number of distinct suits in hand.
     */
    public int getNumSuits()
    {
        return
                (containsSuit(Card.CLUBS) ? 1 : 0) +
                (containsSuit(Card.DIAMONDS) ? 1 : 0) +
                (containsSuit(Card.HEARTS) ? 1 : 0) +
                (containsSuit(Card.SPADES) ? 1 : 0);
    }
    
    public Card set(int index, Card element)
    {
        fingerprintModCount_ = -1;

        return super.set(index, element);
    }

    public String toHTML()
    {
        if (size() == 0) return "";

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < size(); ++i)
        {
            buf.append(getCard(i).toHTML());
        }

        return buf.toString();
    }
}
