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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.gui.*;
import com.zookitec.layout.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyHand extends DashboardItem
{
    // current instance of my hand - used by DealDisplay to notify when a deal is finished
    private static MyHand impl_ = null;

    DDHtmlArea htmlHand_;
    HandInfoFast fast_ = new HandInfoFast();
    String sHand_;
    String sHandTitle_;

    public MyHand(GameContext context)
    {
        super(context, "myhand");
        impl_ = this;
        setDynamicTitle(true);
        trackTableEvents(PokerTableEvent.TYPE_END_HAND |
                         PokerTableEvent.TYPE_CARD_CHANGED |
                         PokerTableEvent.TYPE_NEW_HAND);
    }

    /**
     * finish - clear impl_
     */
    public void finish()
    {
        impl_ = null;
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();
        ExplicitLayout layout = new ExplicitLayout();
        base.setLayout(layout);

        htmlHand_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        htmlHand_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        htmlHand_.setPreferredSize(new Dimension(150, DDCardView.HEIGHT + 14));

        Expression width = DashboardClock.WIDTH;
        base.add(htmlHand_, new ExplicitConstraints(htmlHand_,
                        width.subtract(ComponentEF.preferredWidth(htmlHand_)).multiply(.5d),
                        MathEF.constant(0),
                        ComponentEF.preferredWidth(htmlHand_),
                        ComponentEF.preferredHeight(htmlHand_)
                        ));
        layout.setPreferredLayoutSize(width, ComponentEF.preferredHeight(htmlHand_));

        return base;
    }

    /**
     * dynamic title param, called after updateInfo()
     */
    protected Object getDynamicTitleParam()
    {
        return sHandTitle_;
    }

    /**
     * handle changes to table by repainting as appropriate
     */
    public void tableEventOccurred(PokerTableEvent event)
    {
        if (event.getType() == PokerTableEvent.TYPE_END_HAND)
        {
            // proceed
        }
        else if (event.getType() == PokerTableEvent.TYPE_NEW_HAND)
        {
            if (isDisplayed()) clear();
            return;
        }

        PokerTable table = event.getTable();
        if (!table.isZipMode())
        {
            super.tableEventOccurred(event);
        }
    }

    /**
     * notify of end of deal
     * @param table
     */
    public static void cardsChanged(PokerTable table)
    {
        if (impl_ != null && impl_.isDisplayed() && !table.isZipMode())
        {
            GuiUtils.invoke(new Runnable() {
                public void run()
                {
                    impl_.updateAll();
                }
            });
        }
    }

    ///
    /// display logic
    ///

    /**
     * update level
     */
    protected void updateInfo()
    {
        // init values
        sHand_ = "";
        sHandTitle_ = null;

        // update message text and update labels
        updateMessages();
        htmlHand_.setText(sHand_);
    }

    /**
     * Clear - used when showing new hand
     */
    private void clear()
    {
        sHand_ = "";
        sHandTitle_ = null;
        htmlHand_.setText(sHand_);
        setTitle(getTitle());
    }

    protected void updateMessages()
    {
        PokerTable table = game_.getCurrentTable();
        HoldemHand hhand = table.getHoldemHand();
        PokerPlayer asViewedBy = game_.getHumanPlayer();
        Hand hand = asViewedBy.getHand();

        // if no hand, or an observer
        if (hhand == null || asViewedBy.isObserver() || hand == null)
        {
            sHand_ = PropertyConfig.getMessage("msg.myhand.none");
            sHandTitle_ = null;
            return;
        }

        // figure out number of boards cards visible.  We do this by
        // getting cards in the Flop territory and counting visible ones
        // this is easier since DealCommunity handles the complex logic
        // of which cards should be displayed based on all the options
        int nNumBoard = 0;
        CardPiece card;
        Territory flop = PokerUtils.getFlop();
        synchronized (flop.getMap())
        {
            List<GamePiece> cards = EngineUtils.getMatchingPieces(flop, PokerConstants.PIECE_CARD);
            for (int i = cards.size() - 1; i >= 0; i--)
            {
                card = (CardPiece) cards.get(i);
                if (card.isVisible()) nNumBoard++;
            }
        }

        // pre-flop or folded
        boolean bFold = (asViewedBy.isFolded() && !asViewedBy.showFoldedHand());
        if ((nNumBoard < 3) || bFold)
        {
            String sKey = bFold ? "msg.myhand.folded" : "msg.myhand.preflop";
            sHand_ = PropertyConfig.getMessage(sKey, hand.toHTML());
            sHandTitle_ = hand.toStringRank();
            if (bFold) sHandTitle_ = PropertyConfig.getMessage("msg.myhand.folded.title", sHandTitle_);
        }
        else
        {
            Hand allcommunity = hhand.getCommunity();
            Hand community = new Hand();
            for (int i = 0; i < nNumBoard; i++)
            {
                community.add(allcommunity.getCard(i));
            }

            HandSorted csorted = new HandSorted(community);

            // get hand
            HandInfo info_ = new HandInfo(asViewedBy, asViewedBy.getHandSorted(), csorted);
            fast_.getScore(hand, community);
            Hand best = info_.getBest();
            sHand_ = PropertyConfig.getMessage("msg.myhand.postflop", best.toHTML(),
                                               fast_.toString(", ",false));
            sHandTitle_ = best.toStringRank();
        }
    }
}
