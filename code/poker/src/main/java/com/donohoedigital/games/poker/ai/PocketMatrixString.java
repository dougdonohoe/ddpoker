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

import com.donohoedigital.games.poker.engine.*;

/**
 * Encapsulates tight storage of string values for every possible pocket.
 */
public class PocketMatrixString
{
    private static final int SZ = 1326; // 52 choose 2

    private String[] data_ = new String[SZ];

    public PocketMatrixString()
    {
        this(null);
    }

    public PocketMatrixString(String v)
    {
        clear(v);
    }

    /**
     * Sets all the bytes in the matrix to the same value.
     */
    public void clear(String v)
    {
        for (int x = 0; x < SZ; ++x) data_[x] = v;
    }

    /**
     * Sets the value corresponding to the first two cards in a hand object.
     */
    public void set(Hand h, String v)
    {
        set(h.getCard(0), h.getCard(1), v);
    }

    /**
     * Sets the value corresponding to a particular pair of cards.
     */
    public void set(Card i, Card j, String v)
    {
        set(i.getIndex(), j.getIndex(), v);
    }

    /**
     * Sets the value corresponding to a particular pair of cards.
     */
    public void set(int i, int j, String v)
    {
        int x = (i > j) ? (i*(i+1))/2+j-i : (j*(j+1))/2+i-j;

        data_[x] = v;
    }

    /**
     * Gets the value corresponding to the first two cards in a hand object.
     */
    public String get(Hand h)
    {
        return get(h.getCard(0), h.getCard(1));
    }

    /**
     * Gets the value corresponding to a particular pair of cards.
     */
    public String get(Card i, Card j)
    {
        return get(i.getIndex(), j.getIndex());
    }

    /**
     * Gets the value corresponding to a particular pair of cards.
     */
    public String get(int i, int j)
    {
        int x = (i > j) ? (i*(i+1))/2+j-i : (j*(j+1))/2+i-j;

        return data_[x];
    }
}
