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
package com.ddpoker;

public abstract class Card
{
    /**
     * Numeric rank value for Ace.
     */
    public static final int ACE = 14;

    /**
     * Numeric rank value for King.
     */
    public static final int KING = 13;

    /**
     * Numeric rank value for Queen.
     */
    public static final int QUEEN = 12;

    /**
     * Numeric rank value for Jack.
     */
    public static final int JACK = 11;

    /**
     * Numeric rank value for Ten.
     */
    public static final int TEN = 10;

    /**
     * Numeric rank value for Nine.
     */
    public static final int NINE = 9;

    /**
     * Numeric rank value for Eight.
     */
    public static final int EIGHT = 8;

    /**
     * Numeric rank value for Seven.
     */
    public static final int SEVEN = 7;

    /**
     * Numeric rank value for Six.
     */
    public static final int SIX = 6;

    /**
     * Numeric rank value for Five.
     */
    public static final int FIVE = 5;

    /**
     * Numeric rank value for Four.
     */
    public static final int FOUR = 4;

    /**
     * Numeric rank value for Three.
     */
    public static final int THREE = 3;

    /**
     * Numeric rank value for Two.
     */
    public static final int TWO = 2;

    /**
     * Numeric suit value for Clubs.
     */
    public static final int CLUBS = 0;

    /**
     * Numeric suit value for Diamonds.
     */
    public static final int DIAMONDS = 1;

    /**
     * Numeric suit value for Hearts.
     */
    public static final int HEARTS = 2;

    /**
     * Numeric suit value for Spades.
     */
    public static final int SPADES = 3;

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
    public abstract int getRank();

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
    public abstract int getSuit();
}
