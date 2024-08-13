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
 * HandSorted.java
 *
 * Created on March 30, 2004, 6:02 PM
 */

package com.donohoedigital.games.poker.engine;

/**
 * Ascending sorted hand.  Starts sorted and remains sorted when cards added (only
 * use methods defined in this class)
 *
 * @author  donohoe
 */
public class HandSorted extends Hand {
    
    /**
     * New empty
     */
    public HandSorted() {
        super();
    }
    
    /**
     * New empty, n spots
     */
    public HandSorted(int n) {
        super(n);
    }
    
    /** 
     * Creates a new instance of HandSorted 
     */
    public HandSorted(Hand hand) {
        super(hand);
        sortAscending();
    }
    
    /** 
     * Creates a new instance of HandSorted 
     */
    public HandSorted(HandSorted hand) {
        super(hand);
    }
    
    /**
     * new
     */
    public HandSorted(Card one, Card two)
    {
        addCard(one);
        addCard(two);
    }
    
    /**
     * new
     */
    public HandSorted(Card one, Card two, Card three)
    {
        addCard(one);
        addCard(two);
        addCard(three);
    }
    
    /**
     * new
     */
    public HandSorted(Card one, Card two, Card three, Card four)
    {
        addCard(one);
        addCard(two);
        addCard(three);
        addCard(four);
    }
    
    /**
     * new
     */
    public HandSorted(Card one, Card two, Card three, Card four, Card five)
    {
        addCard(one);
        addCard(two);
        addCard(three);
        addCard(four);
        addCard(five);
    }
    
    /**
     * add card into sorted list
     */
    @Override
    public void addCard(Card c)
    {
        addCard(c, 0);
    }
    
    /**
     * Add card sorted starting at given index, returning
     * index at which card was added
     */
    public int addCard(Card add, int nStart)
    {
        int addRank = add.getRank();
        Card c;
        int nSize = size();
        for (int i = nStart; i < nSize; i++)
        {
            c = getCard(i);
            if (addRank <= c.getRank())
            {
                insertCard(add, i);
                return i;
            }
        }
        
        // not added, so append
        super.addCard(add);
        return nSize;
    }
    
    /** 
     * Add all cards in sorted hand
     */
    public void addAll(HandSorted hand)
    {
        int nSize = hand.size();
        Card c;
        int last = 0;
        for (int i = 0; i < nSize; i++)
        {
            c = hand.getCard(i);
            last = addCard(c, last);
        }
    }
    
    /**
     * Add all cards from a regular hand
     */
    public void addAll(Hand hand)
    {
        int nSize = hand.size();
        Card c;
        for (int i = 0; i < nSize; i++)
        {
            c = hand.getCard(i);
            addCard(c, 0);
        }
    }
    
    /**
     * Returns whether this hand is equivalent to the given hand.
     * 1st, the cards in both hands must have the same rank.
     * 2nd, if the cards in this hand are of the same suit, so
     * must the other hand.  This is used in evaluating and ranking
     * the starting 2 cards in holdem.  It assumes there are the 
     * same number of cards and they have been sorted (the same way)
     * prior to calling this method.
     */
    public boolean isEquivalent(HandSorted cards)
    {
        if (size() != cards.size()) return false;
        if (isSuited() != cards.isSuited()) return false;
        
        for (int i = 0; i < size(); i++)
        {
            if (!getCard(i).isSameRank(cards.getCard(i))) return false;
        }
        
        return true;
    }
    
    /**
     * Returns whether this hand (customary use is the board) contains
     * any straight draws (including gut-shot draws like 8c Qs 2s, where
     * a 9x Tx would be completed with a J)
     */
    public boolean hasStraightDraw()
    {
        int nSize = size();
        boolean ace = getCard(nSize - 1).getRank() == Card.ACE;
        Card c1;
        Card c2;
        int diff;
        
        for (int i = ace ? -1 : 0; i < (nSize - 1); i++)
        {
            c1 = (i == -1) ? Card.SPADES_LOW_A : getCard(i);
            c2 = getCard(i+1);
            diff = c2.getRank() - c1.getRank();
            if (diff > 0 && diff <= 4) return true;
        }
        return false;
    }
    
    /**
     * Return whether a pair is in this hand.
     */
    @Override
    public boolean hasPair()
    {
        int nSize = size();
        Card c1;
        Card c2;
        
        for (int i = 0; i < (nSize - 1); i++)
        {
            c1 = getCard(i);
            c2 = getCard(i+1);            
            if (c1.getRank() == c2.getRank()) return true;
        }
        return false;
    }
    
    /**
     * Return whether a connector is in this hand (specifying
     * gap - i.e., a gap of 0 means something like 10 9 where a
     * gap of 1 means something like 10 8).
     */
    public boolean hasConnector(int nGap)
    {
        return hasConnector(nGap, 1);
    }
    
    /**
     * Return whether a connector is in this hand (specifying
     * gap - i.e., a gap of 0 means something like 10 9 where a
     * gap of 1 means something like 10 8).  nMinBottom is
     * the miminum bottom card for the connector (inclusive) - pass
     * 1 for ace)
     */
    public boolean hasConnector(int nGap, int nMinBottom)
    {
        int nSize = size();
        boolean ace = getCard(nSize - 1).getRank() == Card.ACE;
        Card c1;
        Card c2;
        
        for (int i = ace ? -1 : 0; i < (nSize - 1); i++)
        {
            c1 = (i == -1) ? Card.SPADES_LOW_A : getCard(i);
            c2 = getCard(i+1);
            if (c1.getRank() < nMinBottom) continue;
            if (c1.getRank() == c2.getRank()) continue;
            if (c1.getRank() + (nGap+1) >= c2.getRank()) return true;
        }
        return false;
    }
    
    /**
     * Return rank of highest pair.  Returns 0 if no pair.
     */
    public int getHighestPair()
    {
        int nSize = size();
        Card c1;
        Card c2;
        
        for (int i = nSize - 1; i > 0; i--)
        {
            c1 = getCard(i);
            c2 = getCard(i-1);            
            if (c1.getRank() == c2.getRank()) return c1.getRank();
        }
        return 0;
    }
}
