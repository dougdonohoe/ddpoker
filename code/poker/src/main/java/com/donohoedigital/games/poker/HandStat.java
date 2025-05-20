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
 * HandStat.java
 *
 * Created on February 27, 2004, 8:23 AM
 */

package com.donohoedigital.games.poker;


import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.engine.*;

/**
 *
 * @author  donohoe
 */
public class HandStat implements Comparable<HandStat>
{
    public int nChip;
    public int nCnt;
    public int nWon;
    public HandSorted hand;
    public double lastExpectation_;
    public int nLastChip;
    public int nLastWon;
    public int nLastCnt;
    public static double noise_ =    0.40f;
    public static double noiseAdj_ = 0.05f;
    public static int BET = 10;
    
    /**
     * Reduce noise by noiseAdj_
     */
    public static void lowerNoise()
    {
        noise_ -= noiseAdj_;
        if (noise_ < 0) noise_ = 0;
    }

    /**
     * New hand stat
     */
    public HandStat(HandSorted hand)
    {
        this.hand = hand;
        this.nChip = 0;
        this.nCnt = 0;
        this.nWon = 0;
    }

    /**
     * Record results
     */
    public void record(int nAmount)
    {
        nCnt ++;
        nChip += nAmount;
        if (nAmount > 0) nWon++;
    }

    /**
     * for sorting
     */
    public int compareTo(HandStat s) {
        return (int) (s.getExpectationX() - getExpectationX()); // reverse so sorts descending
    } 

    /**
     * Returns expected win as a multiplier of the original bet
     */
    public double getExpectation()
    {
        if (nCnt == 0) return lastExpectation_;
        return (((double) nChip)/ nCnt)/ BET;
    }

    /**
     * Return Expectation * 1000 for proper sorting
     */
    public double getExpectationX()
    {
        return getExpectation() * 1000.0d;
    }

    /**
     * remember current Expectation
     */
    public void fixExpectation()
    {
        lastExpectation_ = getExpectation();
        nLastChip = nChip;
        nLastCnt = nCnt;
        nLastWon = nWon;
    }

    /**
     * Return whether expectation is positive
     */
    public boolean isExpectationPositive()
    {
        return (lastExpectation_ + noise_) >= 0.0d;
    }
    
    public static Format fHole = new Format("%-8s");
    public static Format fChip = new Format("%7d");
    public static Format fCnt = new Format("%7d");
    public static Format fExp = new Format("%6.3f");
    public static Format fPerc = new Format("%5.2f");

    /**
     * String rep
     */
    public String toString()
    {
        return  fHole.form(hand.toStringSuited()) + " " +
                fChip.form(nChip) + " chips (" + fChip.form(nLastChip) + ")  " +
                fCnt.form(nCnt) + " hands (" + fCnt.form(nLastCnt) + ")  " +
                fCnt.form(nWon) + " wins (" + fCnt.form(nLastWon) + ")" + " [" + fPerc.form(((double) nWon*100) / nCnt) +"%] " + 
                fExp.form(getExpectation()) + " exp   " + 
                fExp.form(lastExpectation_) + " last";
    }
}
    