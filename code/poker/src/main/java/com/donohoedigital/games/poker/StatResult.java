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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.*;

public class StatResult
{
    int win_, lose_, tie_;
    private HandList handList_;
    private double winPercent_;
    private double losePercent_;
    private double tiePercent_;
    private double winOrTiePercent_;
    private int handCount_;
    private String name_;
    private Hand hole_;

    // retain 3 decimal places of precision,
    // so when displaying 2 places, rounding is proper
    private static final long MULT = 100000L;
    private static final double DIV = 1000.0d;

    public StatResult()
    {

    }

    public StatResult(Hand hole, HandList handList, int win, int lose, int tie)
    {
        win_ = win;
        lose_ = lose;
        tie_ = tie;
        calcTotal();
        handList_ = handList;
        name_ = (handList == null) ? null : handList.getName();
        hole_ = hole;
    }

    public StatResult(String name, int win, int lose, int tie)
    {
        win_ = win;
        lose_ = lose;
        tie_ = tie;
        calcTotal();
        handList_ = null;
        name_ = name;
    }

    public void calcTotal()
    {
        handCount_ = win_ + lose_ + tie_;
        if (handCount_ > 0)
        {
            winPercent_ = ((MULT * win_) / handCount_) / DIV;
            losePercent_ = ((MULT * lose_) / handCount_) / DIV;
            tiePercent_ = ((MULT * tie_) / handCount_) / DIV;
            winOrTiePercent_ = ((MULT * (win_ + tie_)) / handCount_) / DIV;
        }
        else
        {
            winPercent_ = 0;
            losePercent_ = 0;
            tiePercent_ = 0;
            winOrTiePercent_ = 0;
        }
    }

    public void addWin(boolean bCalcTotal)
    {
        win_++;
        if (bCalcTotal) calcTotal();
    }

    public void addLose(boolean bCalcTotal)
    {
        lose_++;
        if (bCalcTotal) calcTotal();
    }

    public void addTie(boolean bCalcTotal)
    {
        tie_++;
        if (bCalcTotal) calcTotal();
    }

    public Hand getHole() {
        return hole_;
    }
    public double getWinPercent() {
        return winPercent_;
    }

    public double getLosePercent() {
        return losePercent_;
    }

    public double getTiePercent() {
        return tiePercent_;
    }

    public double getWinOrTiePercent() {
        return winOrTiePercent_;
    }

    public int getHandCount() {
        return handCount_;
    }

    public HandList getHandList() {
        return handList_;
    }

    public String toHTML() {
        return PropertyConfig.getMessage("msg.stats.html",
                Double.toString(winPercent_),
                Double.toString(losePercent_),
                Double.toString(tiePercent_),
                Integer.toString(handCount_));
    }

    public String toHTML(String sKey)
    {
        return PropertyConfig.getMessage(sKey,
                       PokerConstants.formatPercent(winPercent_),
                       PokerConstants.formatPercent(tiePercent_),
                       PokerConstants.formatPercent(losePercent_),
                       new Integer(handCount_)
                        );
    }

    public String toString() {
        return name_ + " -- Win: " + winPercent_ + "% Lose: " + losePercent_ + "% Tie: " + tiePercent_ +
                "% (" + handCount_ + " hands)";
    }
}
