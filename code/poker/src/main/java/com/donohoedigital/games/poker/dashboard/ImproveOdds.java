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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.engine.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImproveOdds extends Odds
{
    String sTotal_;

    public ImproveOdds(GameContext context)
    {
        super(context, "improveodds");
        setDynamicTitle(true);
    }

    /**
     * dynamic title param, called after updateInfo()
     */
    protected Object getDynamicTitleParam()
    {
        return sTotal_;
    }

	///
    /// display logic
    ///

    /**
     * we update during all-in showdown
     */
    protected boolean isUpdatedDuringAllInShowdown()
    {
        return true;
    }

    /**
     * get display string
     */
    protected String getDisplay(int nRound, HoldemHand hhand, PokerPlayer asViewedBy, Hand hand)
    {
        sTotal_ = null;

        // get community cards
        Hand community = hhand.getCommunityForDisplay();
        HandSorted csorted = new HandSorted(community);
        nRound = hhand.getRoundForDisplay();

        // pre-flop
        if (nRound == HoldemHand.ROUND_PRE_FLOP)
        {
            return "";
        }

        // hand info
        HandInfoFaster fast = new HandInfoFaster();
        HandInfo info = new HandInfo(asViewedBy, asViewedBy.getHandSorted(), csorted);
        HandFutures fut = null;

        // switch based on round
        switch (nRound)
        {
            case HoldemHand.ROUND_FLOP:
            case HoldemHand.ROUND_TURN:
                //HandFutures.DEBUG = true;
                fut = new HandFutures(fast, hand, community);
                //HandFutures.DEBUG = false;
                StringBuilder sb = new StringBuilder();
                double d;
                double dTotal = 0.0;
                int nCnt = 0;
                for (int i = Math.max(info.getHandType() + 1, HandInfo.TRIPS); i <= HandInfo.ROYAL_FLUSH; i++)
                {
                    d = fut.getOddsImproveTo(i);
                    if (d == 0.0d) continue;
                    nCnt++;
                    dTotal += d;
                    sb.append(PropertyConfig.getMessage("msg.odds.imptype",
                                    HandInfo.getHandTypeDesc(i),
                                    PokerConstants.formatPercent(d)));
                }
                if (sb.length() > 0)
                {
                    sb.insert(0, PropertyConfig.getMessage("msg.odds.table.start"));
                    sTotal_ = PokerConstants.formatPercent(dTotal);
                    if (nCnt > 1)
                    {
                        sb.append(PropertyConfig.getMessage("msg.odds.total", sTotal_));
                    }
                    sb.append(PropertyConfig.getMessage("msg.odds.table.end"));
                    return sb.toString();
                }
                break;

            case HoldemHand.ROUND_RIVER:
            case HoldemHand.ROUND_SHOWDOWN:
            default:
                break;
        }

        sTotal_ = "0";
        return PropertyConfig.getMessage("msg.odds.imptype.none");
    }
}
