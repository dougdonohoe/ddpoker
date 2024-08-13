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

import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.base.*;

import java.io.*;
import java.util.*;

public class HandList extends BaseProfile {

    public static final String GROUP_BEGIN = "handgroup";
    public static final String GROUP_DIR = "handgroups";

    private int count_ = 0;

    private String sDescription_ = null;

    private ArrayList alHands_ = null;

    // saved members
    private DMTypedHashMap map_;

    public HandList() {
        this(null, null);
    }

    public HandList(String name) {
        this(null, name);
    }

    public HandList(File file) {
        // TODO: implement
        this(null, null);
    }

    public HandList(HandList proto) {
        this(proto, null);
    }

    public HandList(HandList proto, String name) {
        if (proto == null) {
            alHands_ = new ArrayList();
            sName_ = "New Group";
        } else {
            alHands_ = new ArrayList(proto.alHands_);
            sName_ = proto.getName();
        }
        if (name != null)
        {
            sName_ = name;
        }
        map_ = new DMTypedHashMap();
    }

    public void addAll(HandList group) {
        alHands_.addAll(group.alHands_);
    }

    public void addAllPairs(int rank)
    {

        Card c = Card.getCard(CardSuit.CLUBS, rank);
        Card d = Card.getCard(CardSuit.DIAMONDS, rank);
        Card h = Card.getCard(CardSuit.HEARTS, rank);
        Card s = Card.getCard(CardSuit.SPADES, rank);

        add(new HandSorted(c, d));
        add(new HandSorted(c, h));
        add(new HandSorted(c, s));
        add(new HandSorted(d, h));
        add(new HandSorted(d, s));
        add(new HandSorted(h, s));

        ++count_;
    }

    public void removeAllPairs(int rank)
    {
        Card c = Card.getCard(CardSuit.CLUBS, rank);
        Card d = Card.getCard(CardSuit.DIAMONDS, rank);
        Card h = Card.getCard(CardSuit.HEARTS, rank);
        Card s = Card.getCard(CardSuit.SPADES, rank);

        if (containsAny(rank, rank)) --count_;

        remove(new HandSorted(c, d));
        remove(new HandSorted(c, h));
        remove(new HandSorted(c, s));
        remove(new HandSorted(d, h));
        remove(new HandSorted(d, s));
        remove(new HandSorted(h, s));
    }

    public void addAllPairs(int rank1, int rank2)
    {

        if (rank1 > rank2)
        {
            addAllPairs(rank2, rank1);
            return;
        }

        for (int rank = rank1; rank <= rank2; ++rank)
        {
            addAllPairs(rank);
        }
    }

    public void addAllSuited(int rank1, int rank2) {

        // there are, obviously, no suited pairs
        if (rank1 == rank2) {
            return;
        }

        Card c1 = Card.getCard(CardSuit.CLUBS, rank1);
        Card d1 = Card.getCard(CardSuit.DIAMONDS, rank1);
        Card h1 = Card.getCard(CardSuit.HEARTS, rank1);
        Card s1 = Card.getCard(CardSuit.SPADES, rank1);

        Card c2 = Card.getCard(CardSuit.CLUBS, rank2);
        Card d2 = Card.getCard(CardSuit.DIAMONDS, rank2);
        Card h2 = Card.getCard(CardSuit.HEARTS, rank2);
        Card s2 = Card.getCard(CardSuit.SPADES, rank2);

        add(new HandSorted(c1,c2));
        add(new HandSorted(d1,d2));
        add(new HandSorted(h1,h2));
        add(new HandSorted(s1,s2));

        ++count_;
    }

    public void removeAllSuited(int rank1, int rank2)
    {

        // there are, obviously, no suited pairs
        if (rank1 == rank2)
        {
            return;
        }

        Card c1 = Card.getCard(CardSuit.CLUBS, rank1);
        Card d1 = Card.getCard(CardSuit.DIAMONDS, rank1);
        Card h1 = Card.getCard(CardSuit.HEARTS, rank1);
        Card s1 = Card.getCard(CardSuit.SPADES, rank1);

        Card c2 = Card.getCard(CardSuit.CLUBS, rank2);
        Card d2 = Card.getCard(CardSuit.DIAMONDS, rank2);
        Card h2 = Card.getCard(CardSuit.HEARTS, rank2);
        Card s2 = Card.getCard(CardSuit.SPADES, rank2);

        if (containsAny(rank1, rank2, true)) --count_;

        remove(new HandSorted(c1, c2));
        remove(new HandSorted(d1, d2));
        remove(new HandSorted(h1, h2));
        remove(new HandSorted(s1, s2));
    }

    public void addAllUnsuited(int rank1, int rank2)
    {

        // same rank, add only valid pairs
        if (rank1 == rank2)
        {
            addAllPairs(rank1);
            return;
        }

        Card c1 = Card.getCard(CardSuit.CLUBS, rank1);
        Card d1 = Card.getCard(CardSuit.DIAMONDS, rank1);
        Card h1 = Card.getCard(CardSuit.HEARTS, rank1);
        Card s1 = Card.getCard(CardSuit.SPADES, rank1);

        Card c2 = Card.getCard(CardSuit.CLUBS, rank2);
        Card d2 = Card.getCard(CardSuit.DIAMONDS, rank2);
        Card h2 = Card.getCard(CardSuit.HEARTS, rank2);
        Card s2 = Card.getCard(CardSuit.SPADES, rank2);

        add(new HandSorted(c1, d2));
        add(new HandSorted(c1, h2));
        add(new HandSorted(c1, s2));

        add(new HandSorted(d1, c2));
        add(new HandSorted(d1, h2));
        add(new HandSorted(d1, s2));

        add(new HandSorted(h1, c2));
        add(new HandSorted(h1, d2));
        add(new HandSorted(h1, s2));

        add(new HandSorted(s1, c2));
        add(new HandSorted(s1, d2));
        add(new HandSorted(s1, h2));

        ++count_;
    }

    public void removeAllUnsuited(int rank1, int rank2)
    {

        // same rank, add only valid pairs
        if (rank1 == rank2)
        {
            addAllPairs(rank1);
            return;
        }

        Card c1 = Card.getCard(CardSuit.CLUBS, rank1);
        Card d1 = Card.getCard(CardSuit.DIAMONDS, rank1);
        Card h1 = Card.getCard(CardSuit.HEARTS, rank1);
        Card s1 = Card.getCard(CardSuit.SPADES, rank1);

        Card c2 = Card.getCard(CardSuit.CLUBS, rank2);
        Card d2 = Card.getCard(CardSuit.DIAMONDS, rank2);
        Card h2 = Card.getCard(CardSuit.HEARTS, rank2);
        Card s2 = Card.getCard(CardSuit.SPADES, rank2);

        if (containsAny(rank1, rank2, false)) --count_;

        remove(new HandSorted(c1, d2));
        remove(new HandSorted(c1, h2));
        remove(new HandSorted(c1, s2));

        remove(new HandSorted(d1, c2));
        remove(new HandSorted(d1, h2));
        remove(new HandSorted(d1, s2));

        remove(new HandSorted(h1, c2));
        remove(new HandSorted(h1, d2));
        remove(new HandSorted(h1, s2));

        remove(new HandSorted(s1, c2));
        remove(new HandSorted(s1, d2));
        remove(new HandSorted(s1, h2));
    }

    public void addAll(int rank1, int rank2) {

        // same rank, add only valid pairs
        if (rank1 == rank2) {
            addAllPairs(rank1);
            return;
        }

        Card c1 = Card.getCard(CardSuit.CLUBS, rank1);
        Card d1 = Card.getCard(CardSuit.DIAMONDS, rank1);
        Card h1 = Card.getCard(CardSuit.HEARTS, rank1);
        Card s1 = Card.getCard(CardSuit.SPADES, rank1);

        Card c2 = Card.getCard(CardSuit.CLUBS, rank2);
        Card d2 = Card.getCard(CardSuit.DIAMONDS, rank2);
        Card h2 = Card.getCard(CardSuit.HEARTS, rank2);
        Card s2 = Card.getCard(CardSuit.SPADES, rank2);

        add(new HandSorted(c1,c2));
        add(new HandSorted(c1,d2));
        add(new HandSorted(c1,h2));
        add(new HandSorted(c1,s2));

        add(new HandSorted(d1,c2));
        add(new HandSorted(d1,d2));
        add(new HandSorted(d1,h2));
        add(new HandSorted(d1,s2));

        add(new HandSorted(h1,c2));
        add(new HandSorted(h1,d2));
        add(new HandSorted(h1,h2));
        add(new HandSorted(h1,s2));

        add(new HandSorted(s1,c2));
        add(new HandSorted(s1,d2));
        add(new HandSorted(s1,h2));
        add(new HandSorted(s1,s2));
    }

    public void add(HandSorted hand)
    {
        alHands_.add(hand);
    }

    public void remove(HandSorted hand)
    {
        alHands_.remove(hand);
    }

    public String getName() {
        return sName_;
    }

    public String getFileName() {
        return sFileName_;
    }

    /**
     * Get begin part of profile name
     */
    protected String getBegin()
    {
        return GROUP_BEGIN;
    }

    /**
     * Get name of directory to store profiles in
     */
    protected String getProfileDirName()
    {
        return PROFILE_DIR;
    }

    /**
     *  Get profile list
     */
    protected ArrayList getProfileFileList() {
        return null;
    }

    public HandSorted get(int index) {
        return (HandSorted)alHands_.get(index);
    }

    public int size() {
        return alHands_.size();
    }

    public String toHTML() {
        StringBuilder buf = new StringBuilder();
        if (getDescription() != null) {
            buf.append(Utils.encodeHTML(getDescription()));
            buf.append("<br><br>");
        }
        int count = size();
        for (int i = 0; i < count; ++i) {
            if (i > 0) buf.append(", ");
            buf.append(get(i).toString());
        }
        return buf.toString();
    }

    // all hand groups are equal in the eyes of the law
    public int compareTo(BaseProfile o) {
        return 0;
    }

    public String getDescription() {
        return sDescription_;
    }

    public void setDescription(String sDescription) {
        if ((sDescription == null) || (sDescription.length() == 0)) {
            sDescription_ = null;
        } else {
            sDescription_ = sDescription;
        }
    }

    /**
     * Get map
     */
    public DMTypedHashMap getMap()
    {
        return map_;
    }

    public String toString()
    {
        return getName();
    }

    public boolean containsAny(int rank1, int rank2)
    {
        for (int i = size() - 1; i >= 0; --i)
        {
            Hand hand = get(i);
            Card card1 = hand.getCard(0);
            Card card2 = hand.getCard(1);
            if (((card1.getRank() == rank1) && (card2.getRank() == rank2)) ||
                    ((card1.getRank() == rank2) && (card2.getRank() == rank1)))
            {
                return true;
            }
        }
        return false;
    }

    public boolean containsAny(int rank1, int rank2, boolean suited)
    {
        for (int i = size() - 1; i >= 0; --i)
        {
            Hand hand = get(i);
            Card card1 = hand.getCard(0);
            Card card2 = hand.getCard(1);
            if (((card1.getCardSuit().getRank() == card2.getCardSuit().getRank()) == suited) &&
                (((card1.getRank() == rank1) && (card2.getRank() == rank2)) ||
                    ((card1.getRank() == rank2) && (card2.getRank() == rank1))))
            {
                return true;
            }
        }
        return false;
    }

    public int getCount()
    {
        return count_;
    }

    public double getPercent()
    {
        return size() / 13.26;
    }
}
