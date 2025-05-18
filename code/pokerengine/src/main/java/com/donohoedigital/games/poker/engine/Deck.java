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
 * Deck.java
 *
 * Created on December 29, 2003, 6:09 PM
 */

package com.donohoedigital.games.poker.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import java.security.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
@DataCoder('K')
public class Deck extends DMArrayList<Card>
{
    static Logger logger = LogManager.getLogger(Deck.class);

    // random for normal shuffles - changed to secure random in 3.0
    private static SecureRandom random = new SecureRandom();

    // quick random for calc tool
    private static MersenneTwisterFast qrandom = new MersenneTwisterFast();

    /**
     * Empty deck for loading (please use the constructor with a boolean)
     */
    public Deck()
    {
    }

    /**
     * Creates a new deck, shuffled if bShuffle is true
     */
    public Deck(boolean bShuffle)
    {
        this(bShuffle, 0);
    }

    /**
     * Creates a new deck, shuffled if bShuffle is true
     * and sets random seed to given value if non-zero
     */
    public Deck(boolean bShuffle, long seed)
    {
        add(Card.SPADES_2);
        add(Card.SPADES_3);
        add(Card.SPADES_4);
        add(Card.SPADES_5);
        add(Card.SPADES_6);
        add(Card.SPADES_7);
        add(Card.SPADES_8);
        add(Card.SPADES_9);
        add(Card.SPADES_T);
        add(Card.SPADES_J);
        add(Card.SPADES_Q);
        add(Card.SPADES_K);
        add(Card.SPADES_A);

        add(Card.HEARTS_2);
        add(Card.HEARTS_3);
        add(Card.HEARTS_4);
        add(Card.HEARTS_5);
        add(Card.HEARTS_6);
        add(Card.HEARTS_7);
        add(Card.HEARTS_8);
        add(Card.HEARTS_9);
        add(Card.HEARTS_T);
        add(Card.HEARTS_J);
        add(Card.HEARTS_Q);
        add(Card.HEARTS_K);
        add(Card.HEARTS_A);

        add(Card.DIAMONDS_2);
        add(Card.DIAMONDS_3);
        add(Card.DIAMONDS_4);
        add(Card.DIAMONDS_5);
        add(Card.DIAMONDS_6);
        add(Card.DIAMONDS_7);
        add(Card.DIAMONDS_8);
        add(Card.DIAMONDS_9);
        add(Card.DIAMONDS_T);
        add(Card.DIAMONDS_J);
        add(Card.DIAMONDS_Q);
        add(Card.DIAMONDS_K);
        add(Card.DIAMONDS_A);

        add(Card.CLUBS_2);
        add(Card.CLUBS_3);
        add(Card.CLUBS_4);
        add(Card.CLUBS_5);
        add(Card.CLUBS_6);
        add(Card.CLUBS_7);
        add(Card.CLUBS_8);
        add(Card.CLUBS_9);
        add(Card.CLUBS_T);
        add(Card.CLUBS_J);
        add(Card.CLUBS_Q);
        add(Card.CLUBS_K);
        add(Card.CLUBS_A);

        if (bShuffle)
        {
            if (seed > 0) random.setSeed(seed);
            Collections.shuffle(this, random);
        }
    }

    ////
    //// shuffle logic borrowed from Collections
    ////

    /**
     * shuffle
     */
    public void shuffle()
    {
        for (int i = size(); i > 1; i--)
        {
            set(i - 1, set(qrandom.nextInt(i), get(i - 1)));
        }
    }

    /**
     * quick shuffle - shuffle half deck, used for quicker addRandom()
     */
    private void qshuffle()
    {
        for (int i = Math.min(26, size()); i > 1; i--)
        {
            set(i - 1, set(qrandom.nextInt(i), get(i - 1)));
        }
    }

    /**
     * Return next card from top of deck
     */
    public Card nextCard()
    {
        //ApplicationError.assertTrue(size() > 0, "No cards left");
        return remove(0);
    }

    /**
     * Inserts a card at a random location in the deck.
     */
    public void addRandom(Card c)
    {
        //ApplicationError.assertTrue(!contains(c), "Card already in deck!");
        add(c);
        qshuffle();
    }

    /**
     * Inserts a hand full of cards at random locations in the deck.
     */
    public void addRandom(Hand h)
    {
        Card c;
        for (int i = h.size() - 1; i >= 0; --i)
        {
            c = h.getCard(i);
            //ApplicationError.assertTrue(!contains(c), "Card already in deck!");
            add(c);
        }
        qshuffle();
    }

    /**
     * Get card at given index
     */
    public Card getCard(int i)
    {
        return get(i);
    }

    /**
     * Remove given card from the deck
     */
    public void removeCard(Card c)
    {
        remove(c);
    }

    /**
     * Remove card from deck and replace it at top
     */
    public void moveToTop(Card c)
    {
        remove(c);
        add(0, c);
    }

    /**
     * Remove all cards in hand from this deck
     */
    public void removeCards(Hand hand)
    {
        if (hand == null) return; // BUG 340

        for (int i = 0; i < hand.size(); i++)
        {
            removeCard(hand.getCard(i));
        }
    }

    /**
     * Sort ascending (first card off deck is lowest)
     */
    public void sortAscending()
    {
        Collections.sort(this);
    }

    /**
     * sort descending (1st card off deck is highest)
     */
    public void sortDescending()
    {
        Collections.sort(this);
        Collections.reverse(this);
    }

    /**
     * Return string representation of the deck
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Card c : this)
        {
            sb.append(c);
            sb.append(" ");
        }
        return sb.toString();
    }

    // testing decks

    // stacked deck for BUG 280

    public static Deck getDeckBUG280()
    {
        Deck deck = new Deck(true);

        deck.moveToTop(Card.CLUBS_T); // turn
        deck.moveToTop(Card.SPADES_4); // burn

        deck.moveToTop(Card.HEARTS_8); // flop 3
        deck.moveToTop(Card.HEARTS_3); // flop 2
        deck.moveToTop(Card.HEARTS_A); // flop 1
        deck.moveToTop(Card.SPADES_5); // burn

        deck.moveToTop(Card.HEARTS_Q); // opp 2, card 2
        deck.moveToTop(Card.SPADES_K); // opp 1, card 2

        deck.moveToTop(Card.CLUBS_K); // opp 2, card 1
        deck.moveToTop(Card.SPADES_Q); // opp 1, card 1

        return deck;
    }

    // stacked deck for BUG 284
    public static Deck getDeckBUG284()
    {
        Deck deck = new Deck(true);

        deck.moveToTop(Card.DIAMONDS_4); // river
        deck.moveToTop(Card.HEARTS_2); // burn

        deck.moveToTop(Card.CLUBS_9); // turn
        deck.moveToTop(Card.SPADES_2); // burn

        deck.moveToTop(Card.HEARTS_Q); // flop 3
        deck.moveToTop(Card.CLUBS_Q); // flop 2
        deck.moveToTop(Card.DIAMONDS_A); // flop 1
        deck.moveToTop(Card.SPADES_4); // burn

        deck.moveToTop(Card.SPADES_8); // opp 1, card 2
        deck.moveToTop(Card.HEARTS_A); // opp 2, card 2

        deck.moveToTop(Card.CLUBS_A); // opp 2, card 1
        deck.moveToTop(Card.SPADES_5); // opp 1, card 1

        return deck;
    }

    // stacked deck for BUG 316
    public static Deck getDeckBUG316()
    {
        Deck deck = new Deck(true);

        deck.moveToTop(Card.DIAMONDS_4); // river
        deck.moveToTop(Card.HEARTS_2); // burn

        deck.moveToTop(Card.CLUBS_9); // turn
        deck.moveToTop(Card.SPADES_2); // burn

        deck.moveToTop(Card.SPADES_2); // flop 3
        deck.moveToTop(Card.HEARTS_5); // flop 2
        deck.moveToTop(Card.HEARTS_4); // flop 1
        deck.moveToTop(Card.SPADES_4); // burn

        deck.moveToTop(Card.CLUBS_J); // opp 1, card 2
        deck.moveToTop(Card.HEARTS_A); // opp 2, card 2

        deck.moveToTop(Card.CLUBS_A); // opp 2, card 1
        deck.moveToTop(Card.DIAMONDS_9); // opp 1, card 1

        return deck;
    }

}
