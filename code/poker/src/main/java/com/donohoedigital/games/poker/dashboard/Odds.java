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

import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.gui.*;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Odds extends DashboardItem
{
    DDLabel labelInfo_;

    public Odds(GameContext context, String sName)
    {
        super(context, sName);
        trackTableEvents(PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED |
                         PokerTableEvent.TYPE_DEALER_ACTION |
                         PokerTableEvent.TYPE_CARD_CHANGED |
                         PokerTableEvent.TYPE_NEW_HAND |
                         PokerTableEvent.TYPE_END_HAND);
    }

    @Override
    protected JComponent createBody()
    {
        labelInfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        return labelInfo_;
    }

    /**
     * handle changes to table by repainting as appropriate
     */
    @Override
    public void tableEventOccurred(PokerTableEvent event)
    {
        PokerTable table = event.getTable();

        if (table.isZipMode() || !isDisplayed()) return;

        boolean bUpdateOdds = false;
        HoldemHand hhand = table.getHoldemHand();
        if (hhand == null) return; // saw NPE on Linux in line XX below ... timing issue?

        switch (event.getType())
        {
            case PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED:
                PokerPlayer nu = hhand.getPlayerAt(event.getNew()); // XX
                if (nu != null && nu.isHumanControlled())
                {
                    bUpdateOdds = true;
                }
                break;

            case PokerTableEvent.TYPE_DEALER_ACTION:
            case PokerTableEvent.TYPE_CARD_CHANGED:
                // update odds after flop/turn/river unless
                // hand is in all in showdown
                if (isUpdatedDuringAllInShowdown() || !hhand.isAllInShowdown())
                {
                    bUpdateOdds = true;
                }
                break;

            case PokerTableEvent.TYPE_NEW_HAND:
            case PokerTableEvent.TYPE_END_HAND:
                bUpdateOdds = true;
                break;
        }

        // if odds display needs updating, do so
        if (bUpdateOdds)
        {
            super.tableEventOccurred(event);
        }
    }

    /**
     * update during all in showdown?  Default is false
     */
    protected boolean isUpdatedDuringAllInShowdown()
    {
        return false;
    }

    ///
    /// display logic
    ///

    /**
     * update level
     */
    @Override
    protected void updateInfo()
    {
        // init
        PokerTable table = game_.getCurrentTable();
        HoldemHand hhand = table.getHoldemHand();
        PokerPlayer asViewedBy = game_.getHumanPlayer();

        // update message text and update labels
        String sOdds = updateOdds(hhand, asViewedBy, false);
        labelInfo_.setText(sOdds);
    }

    /**
     * Update odds display
     */
    protected String updateOdds(HoldemHand hhand, PokerPlayer asViewedBy, boolean bMouseOver)
    {
        // no hand
        if (hhand == null
            //    || (!bMouseOver && asViewedBy.isHuman() && engine.getPrefsNode().getBooleanOption(PokerConstants.OPTION_HOLE_CARDS_DOWN))
                )
        {
            return "";
        }

        // if folded or pre-flop
        Hand hand = asViewedBy.getHand();
        int nRound = hhand.getRound();
        if (asViewedBy.isFolded() || asViewedBy.isObserver() || hand == null ||
            (nRound == HoldemHand.ROUND_SHOWDOWN && hhand.getNumWithCards() == 1))
        {
            return "";
        }

        return getDisplay(nRound, hhand, asViewedBy, hand);

    }

    protected abstract String getDisplay(int nRound, HoldemHand hhand, PokerPlayer asViewedBy, Hand hand);
}
