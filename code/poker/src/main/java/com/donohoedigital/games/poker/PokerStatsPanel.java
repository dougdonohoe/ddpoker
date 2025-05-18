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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class PokerStatsPanel extends DDTabPanel
{
    static Logger logger = LogManager.getLogger(PokerStatsPanel.class);

    public static final int FLOP = 1;
    public static final int TURN = 2;
    public static final int RIVER = 3;
    public static final int LADDER = 4;
    public static final int STRENGTH = 5;

    private DDScrollPane scroll_;

    private Hand pocket_;
    private Hand community_;
    private int mode_;
    private boolean bDemo_;

    DDHtmlArea htmlArea_;
    DDHtmlArea header_;

    public PokerStatsPanel(int mode)
    {
        this(null, mode);
    }

    public PokerStatsPanel(PokerPlayer player, int mode)
    {
        super();

        mode_ = mode;
        bDemo_ = GameEngine.getGameEngine().isDemo();

        if (player != null)
        {
            PokerTable table = player.getTable();
            HoldemHand hand = table.getHoldemHand();
            pocket_ = player.getHand();
            community_ = hand.getCommunityForDisplay();
        }

        setPreferredSize(new Dimension(460, 350));
    }

    protected void createUI()
    {
        setBorderLayoutGap(0, 0);

        // top
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(10,0);
        add(top, BorderLayout.NORTH);

        // header
        header_ = new DDHtmlArea(GuiManager.DEFAULT, "PokerStatsHeader");
        header_.setBorder(BorderFactory.createEmptyBorder());
        top.add(header_, BorderLayout.NORTH);

        // html results
        htmlArea_ = new DDHtmlArea("PokerStats", "PokerStats");
        htmlArea_.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        scroll_ = new DDScrollPane(htmlArea_, "PokerStandardDialog", null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setOpaque(false);
        add(scroll_, BorderLayout.CENTER);

        // update stats
        if (pocket_ != null)
        {
            updateHeader();
            updateStats();
        }
    }

    /**
     * header
     */
    private void updateHeader()
    {
        String sMsg = null;
        int nMin = 0;
        switch (mode_)
        {
            case FLOP:
                nMin = 3;
                sMsg = PropertyConfig.getMessage("msg.sim.flop");
                break;
            case TURN:
                nMin = 4;
                sMsg = PropertyConfig.getMessage("msg.sim.turn");
                break;
            case RIVER:
                nMin = 5;
                sMsg = PropertyConfig.getMessage("msg.sim.river");
                break;
            case LADDER:
                nMin = 5;
                sMsg = PropertyConfig.getMessage("msg.sim.ladder");
                break;
            case STRENGTH:
                nMin = 5;
                sMsg = PropertyConfig.getMessage("msg.sim.strength");
                break;
        }

        Hand pocket = new Hand(pocket_);
        while (pocket.size() < 2) pocket.addCard(Card.BLANK);

        Hand community = new Hand(community_);
        while (community.size() < nMin) community.addCard(Card.BLANK);
        int nNumComm = community.size();
        header_.setText(PropertyConfig.getMessage("msg.sim.header.generic",
                                                     pocket.toHTML(),
                                                     "&nbsp;&nbsp;" + community.toHTML(),
                                                     (2+nNumComm) * 23 + 5,
                                                     sMsg));
    }

    /**
     * run update thread
     */
    public void updateStats()
    {
        if (checkRequiredCards())
        {
            new UpdateThread().start();
        }
    }

    /**
     * check prereqs
     */
    private boolean checkRequiredCards()
    {
        String sText = null;
        if (pocket_.size() != 2 && !(bDemo_ && (mode_ == TURN || mode_ == RIVER)))
        {
            sText = PropertyConfig.getMessage("msg.sim.needboth");
        }
        else
        {
            int com = community_.size();
            switch (mode_)
            {
                case FLOP:
                    if (com >= 3) sText = PropertyConfig.getMessage("msg.sim.seenflop");
                    break;
                case TURN:
                    if (bDemo_) sText = PropertyConfig.getMessage("msg.sim.demo");
                    else if (com <= 2) sText = PropertyConfig.getMessage("msg.sim.needflop.1");
                    else if (com == 4 || com == 5) sText = PropertyConfig.getMessage("msg.sim.seenturn");
                    break;
                case RIVER:
                    if (bDemo_) sText = PropertyConfig.getMessage("msg.sim.demo");
                    else if (com <= 2) sText = PropertyConfig.getMessage("msg.sim.needflop.2");
                    else if (com == 5) sText = PropertyConfig.getMessage("msg.sim.seenriver");
                    break;
                case LADDER:
                    // ladder is good anytime
                    break;
                case STRENGTH:
                    if (com < 3) sText = PropertyConfig.getMessage("msg.sim.needflop.3");
                    break;
            }
        }

        if (sText != null)
        {
            htmlArea_.setText(sText);
            return false;
        }

        return true;
    }

    /**
     * update thread with given pocket/community
     */
    public void updateStats(Hand pocket, Hand community)
    {
        // same hand, skip update
        if (pocket_ != null && pocket.fingerprint() == pocket_.fingerprint() &&
            community_ != null && community.fingerprint() == community_.fingerprint())
        {
            return;
        }

        // store new values
        pocket_ = pocket;
        community_ = community;

        // UI not created, so stop here
        if (header_ == null) return;

        // update
        updateHeader();
        updateStats();
    }

    /**
     * thread generates info then updates html
     */
    private class UpdateThread extends Thread
    {
        HandStrength strength_;
        HandPotential potential_;
        HandLadder ladder_;

        public UpdateThread()
        {
            super("UpdateThread");
        }

        public void run()
        {
            switch (mode_)
            {
                case LADDER:
                    ladder_ = new HandLadder(pocket_, community_, null);
                    break;

                case STRENGTH:
                    strength_ = new HandStrength();
                    break;

                default:
                    potential_ = new HandPotential(pocket_, community_);
            }

            SwingUtilities.invokeLater(new Thread("PokerStatsPanel")
            {
                public void run()
                {
                    switch (mode_)
                    {
                        case FLOP:
                            htmlArea_.setText(potential_.toHTML(HoldemHand.ROUND_FLOP));
                            break;
                        case TURN:
                            htmlArea_.setText(potential_.toHTML(HoldemHand.ROUND_TURN));
                            break;
                        case RIVER:
                            htmlArea_.setText(potential_.toHTML(HoldemHand.ROUND_RIVER));
                            break;
                        case LADDER:
                            htmlArea_.setText(ladder_.toHTML());
                            break;
                        case STRENGTH:
                            htmlArea_.setText(strength_.toHTML(pocket_, community_, 9));
                            break;
                    }
                    htmlArea_.setCaretPosition(0); // scroll to top
                }
            });
        }
    }

}
