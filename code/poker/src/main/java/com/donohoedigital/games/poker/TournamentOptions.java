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
 * TournamentOptions.java
 *
 * Created on January 25, 2004, 4:25 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Doug Donohoe
 */
public class TournamentOptions extends BasePhase implements ChangeListener, AncestorListener
{
    static Logger logger = LogManager.getLogger(TournamentOptions.class);

    private DDHtmlArea text_;
    private MenuBackground menu_;
    private DDButton start_;
    private DDLabelBorder statsBorder_;
    private TournamentProfile selected_ = null;
    private ProfileList profileList_;
    private TournamentSummaryPanel summary_;

    private boolean bHomeMode_ = false;
    private boolean bOnlineMode_ = false;

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // name of style used for all widgets in data area
        String STYLE = gamephase_.getString("style", "default");
        bHomeMode_ = gamephase_.getBoolean("home", false);
        bOnlineMode_ = gamephase_.getBoolean("online", false);

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        menu_.addAncestorListener(this);
        DDPanel menubox = menu_.getMenuBox();
        String sHelpName = menu_.getHelpName();

        // put buttons in the menubox_
        ButtonBox buttonbox = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox.add(buttonbox, BorderLayout.SOUTH);
        start_ = buttonbox.getDefaultButton();

        // holds data we are gathering
        DDPanel data = new DDPanel(sHelpName);
        BorderLayout layout = (BorderLayout) data.getLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        data.setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 10));
        menubox.add(data, BorderLayout.CENTER);

        // help text
        text_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());
        data.add(text_, BorderLayout.CENTER);

        // top
        DDPanel top = new DDPanel(sHelpName);
        layout = (BorderLayout) top.getLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        data.add(top, BorderLayout.NORTH);

        // left side
        DDPanel left = new DDPanel(sHelpName);
        top.add(left, BorderLayout.WEST);

        // get current profile list and sort it
        List<BaseProfile> profiles = TournamentProfile.getProfileList();
        Collections.sort(profiles);

        // player list
        DDLabelBorder pborder = new DDLabelBorder("tournaments", STYLE);
        pborder.setPreferredSize(new Dimension(250, 448));
        left.add(pborder, BorderLayout.CENTER);
        profileList_ = new TournamentProfileList(engine_, profiles, "Profile", "tournament", sHelpName, "pokericon16png", true);
        profileList_.addChangeListener(this);
        pborder.add(profileList_, BorderLayout.CENTER);

        // stats
        statsBorder_ = new DDLabelBorder("settings", STYLE);
        statsBorder_.setPreferredSize(new Dimension(500, 0));
        top.add(statsBorder_, BorderLayout.CENTER);
        summary_ = new TournamentSummaryPanel(context_, "TournamentSummaryBig", "BrushedMetal", "BrushedMetal", sHelpName, 1.0d, true, false);
        statsBorder_.add(summary_, BorderLayout.CENTER);
    }

    /**
     * Our list editor
     */
    private class TournamentProfileList extends ProfileList
    {
        private TournamentProfileList(GameEngine engine, List<BaseProfile> profiles,
                                      String sStyle,
                                      String sMsgName,
                                      String sPanelName,
                                      String sIconName,
                                      boolean bUseCopyButton)
        {
            super(engine, context_, profiles, sStyle, sMsgName, sPanelName, sIconName, bUseCopyButton);
        }

        /**
         * Create empty profile
         */
        @Override
        protected BaseProfile createEmptyProfile()
        {
            TournamentProfile t = new TournamentProfile();
            t.setPlayerTypePercent(PlayerType.getDefaultProfile().getUniqueKey(), 100);
            return t;
        }

        /**
         * Copy profile
         */
        @Override
        protected BaseProfile copyProfile(BaseProfile profile, boolean bForEdit)
        {
            TournamentProfile tp = (TournamentProfile) profile;
            String sName = bForEdit ? tp.getName() : PropertyConfig.getMessage("msg.copy", tp.getName());
            return new TournamentProfile(tp, sName);
        }
    }

    /**
     * Start of phase
     */
    @Override
    public void start()
    {
        // set help text
        context_.getWindow().setHelpTextWidget(text_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, profileList_);

        // check button states and focus
        checkButtons();
    }

    /**
     * Returns true
     */
    @Override
    public boolean processButton(GameButton button)
    {
        if (button.getName().equals(start_.getName()))
        {
            // get game
            final PokerGame game;

            // home mode - create new game
            if (bHomeMode_)
            {
                // show warning for fixed alloc home tournaments
                if (selected_.isAllocFixed())
                {
                    String sMsg = PropertyConfig.getMessage("msg.home.fixed",
                                                            selected_.getName(),
                                                            selected_.getNumPlayers());
                    if (!EngineUtils.displayConfirmationDialog(context_, Utils.fixHtmlTextFor15(sMsg), "homeallocfixed"))
                    {
                        return false;
                    }
                }

                // create game, set home mode
                game = new PokerGame(context_);
                game.setClockMode(true);
                selected_.setDemo(engine_.isDemo()); // used to limit rounds time limit
                context_.setGame(game);
            }
            // online game
            else if (bOnlineMode_)
            {
                PokerMain main = PokerMain.getPokerMain();

                // show warning for tournaments with default players greater than max
                if (selected_.getNumPlayers() > selected_.getMaxOnlinePlayers())
                {
                    String sMsg = PropertyConfig.getMessage("msg.online.max",
                                                            selected_.getName(),
                                                            selected_.getNumPlayers(),
                                                            selected_.getMaxOnlinePlayers());
                    if (!EngineUtils.displayConfirmationDialog(context_, Utils.fixHtmlTextFor15(sMsg), "onlinemaxplayers"))
                    {
                        return false;
                    }

                    // change player count to be max players
                    selected_.updateNumPlayers(selected_.getMaxOnlinePlayers());
                }

                // create game, set ID
                game = new PokerGame(context_);
                game.setTempOnlineGameID();
                GameState state = game.newGameState("temp", GameListPanel.SAVE_EXT);
                // TODO: UDP only if testing is enabled
                String prefix = (DebugConfig.isTestingOn() && PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_UDP)) ?
                                PokerConstants.ONLINE_GAME_PREFIX_UDP : PokerConstants.ONLINE_GAME_PREFIX_TCP;
                String id = prefix + state.getFileNumber();
                String sName = PropertyConfig.getMessage("msg.onlineGameName.host", id);
                state.setName(sName);
                game.setOnlineGameID(id);

                // need to create p2p server to get ip/port, so attempt that
                if (!canHost(main, game))
                {
                    return false;
                }

                // we can get ip/port, so continue on
                game.setGameState(state); // save so can save later
                game.setOnlinePassword(generatePassword());
                game.setLocalIP(main.getIP());
                game.setPort(main.getPort());

                // create player for host
                PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
                PokerPlayer player = new PokerPlayer(engine_.getPublicUseKey(), game.getNextPlayerID(), profile, true);
                player.setPlayerType(PlayerType.getAdvisor());
                game.addPlayer(player);

                // init after everything setup
                game.initOnline(PokerGame.MODE_INIT);
                context_.setGame(game);
            }
            // regular tournament
            else
            {
                game = setupPracticeGame(engine_, context_);
            }

            // prevent double click on this button (because initTournament()
            // can take time for large tournaments
            start_.setEnabled(false);

            // setup computer
            game.initTournament(selected_);
        }

        return true;
    }


    /**
     * test if can host by creating p2p server
     */
    private boolean canHost(PokerMain main, PokerGame game)
    {
        PokerConnectionServer p2p = main.getPokerConnectionServer(game.isUDP());
        if (p2p != null && !p2p.isBound())
        {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.canthost",
                                                                                     p2p.getConfigPort()));
            main.shutdownPokerConnectionServer(p2p);
            return false;
        }
        return true;
    }

    /**
     * practice game setup
     */
    public static PokerGame setupPracticeGame(GameEngine engine, GameContext context)
    {
        PokerGame game;
        // create game
        game = new PokerGame(context);
        context.setGame(game);

        // setup human player (computer players added in TournamentOptions)
        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        PokerPlayer player = new PokerPlayer(engine.getPublicUseKey(), game.getNextPlayerID(), profile, true);
        player.setPlayerType(PlayerType.getAdvisor());
        game.addPlayer(player);
        return game;
    }

    /**
     * Get online password
     */
    private String generatePassword()
    {
        // password format:  id-AAA-123
        MersenneTwisterFast random = new MersenneTwisterFast();
        StringBuilder sbPass = new StringBuilder();
        int num;

        // do letters, skipping letter O (to avoid confusing with number 0)
        for (int i = 0; i < 3; i++)
        {
            do
            {
                num = random.nextInt(26);
            }
            while (num == 14); // no letter 0
            sbPass.append((char) (65 + num));
        }

        sbPass.append('-');

        // do numbers, skipping number 0
        for (int i = 0; i < 3; i++)
        {
            num = random.nextInt(9) + 1; // no zeroes
            sbPass.append(num);
        }

        return sbPass.toString();
    }

    /**
     * set buttons enabled/disabled based on selection
     */
    private void checkButtons()
    {
        boolean bValid = selected_ != null;
        start_.setEnabled(bValid);
    }

    /**
     * Called when a spinner or tournament list changes
     */
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == profileList_)
        {
            TournamentProfile pp = (TournamentProfile) profileList_.getSelectedProfile();

            if (pp != null)
            {
                // set current selected profile and update stats label
                selected_ = pp;
                summary_.updateProfile(selected_);
                statsBorder_.setText(PropertyConfig.getMessage("labelborder.settings.label2",
                                                               selected_.getName()));
                statsBorder_.repaint();
            }
            else
            {
                selected_ = null;
                summary_.updateProfile(selected_);
                statsBorder_.setText(PropertyConfig.getMessage("labelborder.settings.label"));
                statsBorder_.repaint();
            }
        }

        checkButtons();
    }

    /**
     * Called when the source or one of its ancestors is made visible
     * either by setVisible(true) being called or by its being
     * added to the component hierarchy.  The method is only called
     * if the source has actually become visible.  For this to be true
     * all its parents must be visible and it must be in a hierarchy
     * rooted at a Window
     */
    public void ancestorAdded(AncestorEvent event)
    {
        // select 1st row
        profileList_.selectInit();

    }

    /**
     * Called when either the source or one of its ancestors is moved.
     */
    public void ancestorMoved(AncestorEvent event)
    {
    }

    /**
     * Called when the source or one of its ancestors is made invisible
     * either by setVisible(false) being called or by its being
     * remove from the component hierarchy.  The method is only called
     * if the source has actually become invisible.  For this to be true
     * at least one of its parents must by invisible or it is not in
     * a hierarchy rooted at a Window
     */
    public void ancestorRemoved(AncestorEvent event)
    {
    }

}
