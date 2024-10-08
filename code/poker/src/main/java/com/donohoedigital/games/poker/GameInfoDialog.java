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
/*
 * GameInfoDialog.java
 *
 * Created on April 25, 2004, 6:48 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.db.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GameInfoDialog extends DialogPhase
{
    static Logger logger = LogManager.getLogger(GameInfoDialog.class);
    
    // members
    private PokerGame game_;
    private TournamentProfile profile_;
    private DDTabbedPane tab_;
    private ImageComponent ic_ = new ImageComponent("ddlogo20", 1.0d);
    private boolean bLobbyMode_;

    /**
     * Init phase, storing engine and gamephase.  Called createUI()
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        bLobbyMode_ = gamephase.getBoolean("lobby", false);
        game_ = (PokerGame) context.getGame();
        profile_ = game_.getProfile();
        if (!game_.isClockMode() && !bLobbyMode_) profile_.setPrizePool(game_.getPrizePool(), true); // update to current
        ic_.setScaleToFit(false);
        ic_.setIconWidth(GamePrefsPanel.ICWIDTH);
        ic_.setIconHeight(new Integer(GamePrefsPanel.ICHEIGHT.intValue() + 6)); // need to be slightly higher for focus
        super.init(engine, context, gamephase);
    }
    
    /**
     * Focus here
     */
    public Component getFocusComponent()
    {
        return tab_;
    }
    
    /**
     * create gui
     */
    public JComponent createDialogContents() 
    {
        tab_ = new DDTabbedPane(STYLE, null, JTabbedPane.TOP);
        tab_.setOpaque(false);
        tab_.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // chip leaders
        if (!game_.isClockMode() && !bLobbyMode_)
        {
            // hand history panel
            HandHistoryTab handbase = new HandHistoryTab();
            handbase.createUI();
            tab_.addTab(PropertyConfig.getMessage("msg.handhistory"), ic_, handbase, null);

            // chip leader panel
            ChipLeaderPanel chipbase = new ChipLeaderPanel(context_, 189);
            chipbase.setPreferredSize(new Dimension(700, 350));
            chipbase.createUI();
            tab_.addTab(PropertyConfig.getMessage("msg.chipleader"), ic_, chipbase, null);

            // table list
            TableListPanel tbllist = new TableListPanel(context_, STYLE);
            tab_.addTab(PropertyConfig.getMessage("msg.tablelist"), ic_, tbllist, null);
        }

        // tournament settings
        GamePanel base = new GamePanel();

        // clock or lobby mode - just display the tournament summary
        if (game_.isClockMode() || bLobbyMode_)
        {
            base.createUI();
            return base;
        }
        // otherwise add it as a tab
        tab_.addTab(PropertyConfig.getMessage("msg.gametab"), ic_, base, null);

        // online settings
        if (game_.isOnlineGame())
        {
            tab_.addTab(PropertyConfig.getMessage("msg.online2"), ic_, new OnlineTab(), null);
        }

        return tab_;
    }

    private class GamePanel extends DDTabPanel
    {
        public void createUI()
        {
            setPreferredSize(new Dimension(650, 350));
            setBorderLayoutGap(10, 0);

            // label
            DDLabel label = new DDLabel(GuiManager.DEFAULT, "TournamentSummaryName");
            label.setText(PropertyConfig.getMessage("msg.tourneyname", Utils.encodeHTML(profile_.getName())));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            add(label, BorderLayout.NORTH);

            // description
            TournamentSummaryPanel sum = new TournamentSummaryPanel(context_, "TournamentSummaryDialog",
                                                                    null,
                                                                    "OptionsDialog",
                                                                    GuiManager.DEFAULT, 1.0d,
                                                                    (!game_.isOnlineGame() && !game_.isClockMode()) ||
                                                                    (game_.isOnlineGame() && game_.getLocalPlayer().isHost()), false);
            sum.updateProfile(profile_);
            add(sum, BorderLayout.CENTER);

            repaint();
        }
    }

    private class OnlineTab extends DDTabPanel
    {
        public void createUI()
        {
            setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, VerticalFlowLayout.LEFT));

            DDLabelBorder area = Lobby.createURLPanel(game_, STYLE, STYLE, "PokerStandardDialog", 5);
            add(area);

            if (game_.getLocalPlayer().isHost())
            {
                TypedHashMap dummy = new TypedHashMap();

                // timeout
                DDLabelBorder timeout = new DDLabelBorder("timeout", STYLE);
                timeout.setLayout(new GridLayout(0, 1, 0, 4));
                add(timeout);
                OptionInteger oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_TIMEOUT, STYLE,
                                                                    dummy, null, TournamentProfile.MIN_TIMEOUT,
                                                                    TournamentProfile.MAX_TIMEOUT, 50, true), timeout);
                oi.setBorder(BorderFactory.createEmptyBorder(0,5,5,5));
                oi.setMap(profile_.getMap());
                oi.resetToMap();

                oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_THINKBANK, STYLE,
                                                                    dummy, null, 0,
                                                                    TournamentProfile.MAX_THINKBANK, 50, true), timeout);
                oi.setBorder(BorderFactory.createEmptyBorder(0,5,5,5));
                oi.setMap(profile_.getMap());
                oi.resetToMap();

                // boot
                DDLabelBorder bootbase = TournamentProfileDialog.createBootControls(STYLE, dummy, profile_);
                add(bootbase);
            }
            repaint();
        }
    }

    private class HandHistoryTab extends DDTabPanel
    {
        public void createUI()
        {
            setPreferredSize(new Dimension(650, 363));

            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.INTEGER, new Integer(PokerDatabase.storeTournament(game_)));

            HoldemHand hhand = game_.getCurrentTable().getHoldemHand();

            if (hhand == null || hhand.isStoredInDatabase())
            {
                hhand = null;
            }

            add(new HandHistoryPanel(context_, STYLE, "HND_TOURNAMENT_ID=?", bindArray, hhand, 9), BorderLayout.CENTER);
        }
    }
}
