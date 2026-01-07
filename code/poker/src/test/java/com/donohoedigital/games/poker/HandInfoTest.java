/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandSorted;
import org.junit.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.junit.Assert.assertEquals;

public class HandInfoTest {

    @Test
    public void testHandInfo() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        verify("Royal Flush (clubs)        ", 10000014, CLUBS_A, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_K);
        verify("Royal Flush (spades)       ", 10000014, SPADES_A, SPADES_J, SPADES_K, SPADES_Q, SPADES_T, SPADES_2, HEARTS_K);
        verify("Straight Flush K, (hearts) ", 9000013, HEARTS_9, HEARTS_J, HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T, HEARTS_3);
        verify("Straight Flush K, A str    ", 9000013, HEARTS_9, HEARTS_J, HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T, CLUBS_A);
        verify("Straight Flush (+1 hearts) ", 9000006, HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5, HEARTS_6, SPADES_A);
        verify("Straight Flush (low hearts)", 9000005, HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5, CLUBS_A, SPADES_A);
        verify("Quads                      ", 8000135, CLUBS_8, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);
        verify("Full House (two trips)     ", 7000200, CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_Q, HEARTS_7);
        verify("Full House                 ", 7000140, CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_7, HEARTS_7);
        verify("Full House/Trips           ", 7000135, CLUBS_K, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);
        verify("Flush (clubs)              ", 6904104, CLUBS_8, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        verify("Flush (clubs lower kicker) ", 6904103, CLUBS_7, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        verify("Straight (6 high)          ", 5000006, CLUBS_2, HEARTS_A, HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_6, SPADES_A);
        verify("Straight (5 high)          ", 5000005, CLUBS_2, HEARTS_A, HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_A, SPADES_A);
        verify("Straight/Two Pair          ", 5000008, CLUBS_8, HEARTS_8, DIAMONDS_6, SPADES_5, SPADES_7, DIAMONDS_7, HEARTS_4);
        verify("Trips                      ", 4002267, CLUBS_K, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_J, HEARTS_3);
        verify("Pair                       ", 2028376, CLUBS_8, HEARTS_A, DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_6);
        verify("High Card                  ", 1973958, CLUBS_8, HEARTS_A, DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_Q);
    }

    private static void verify(String sName, int expected, Card c1, Card c2, Card c3, Card c4, Card c5, Card c6, Card c7) {
        HandSorted hand = new HandSorted();
        PokerPlayer testPlayer = new PokerPlayer(0, "Test", true);
        testPlayer.setName(sName);

        if (c1 != null) hand.addCard(c1);
        if (c2 != null) hand.addCard(c2);
        if (c3 != null) hand.addCard(c3);
        if (c4 != null) hand.addCard(c4);
        if (c5 != null) hand.addCard(c5);
        if (c6 != null) hand.addCard(c6);
        if (c7 != null) hand.addCard(c7);

        HandInfo info = new HandInfo(testPlayer, hand, null);
        int fastScore = new HandInfoFast().getScore(info.getHole(), info.getCommunity());
        int fasterScore = new HandInfoFaster().getScore(info.getHole(), info.getCommunity());

        System.out.println("====================================================================================================================");
        System.out.println(sName + " - " + info + " fastscore=" + fastScore + " fasterScore=" + fasterScore);
        System.out.println(info.toStringDebug());
        System.out.println();
        assertEquals("Score doesn't match expected", expected, fastScore);
        assertEquals("Fast doesn't match score", info.getScore(), fastScore);
        assertEquals("Faster doesn't match score", info.getScore(), fasterScore);
    }
}