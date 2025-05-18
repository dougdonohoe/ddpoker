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
public class PotOdds extends Odds
{
    String sOdds_;

    public PotOdds(GameContext context)
    {
        super(context, "potodds");
        setDynamicTitle(true);
    }

    /**
     * dynamic title param, called after updateInfo()
     */
    protected Object getDynamicTitleParam()
    {
        return sOdds_;
    }

	///
    /// display logic
    ///

    protected String getDisplay(int nRound, HoldemHand hhand, PokerPlayer asViewedBy, Hand hand)
    {
        // figure out pot odds
        if (nRound != HoldemHand.ROUND_SHOWDOWN && hhand.getCall(asViewedBy) > 0)
        {
            double po = hhand.getPotOdds(asViewedBy);
            double oo = (100.0d - po) / po;
            sOdds_ = PokerConstants.formatPercent(po);
            return PropertyConfig.getMessage("msg.odds.pot", sOdds_, PokerConstants.formatPercent(oo));
        }
        else
        {
            sOdds_ = null;
            return PropertyConfig.getMessage("msg.odds.pot.none");
        }
    }
}
