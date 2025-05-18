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
 * GameOver.java
 *
 * Created on April 17, 2003, 9:20 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Doug Donohoe
 */
public class GameOver extends DialogPhase
{
    static Logger logger = LogManager.getLogger(GameOver.class);

    private PokerGame game_;
    private boolean bOnline_;

    /**
     * init phase
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        // get your game on
        game_ = (PokerGame) context.getGame();
        bOnline_ = game_.isOnlineGame();

        // after eliminated, need to check rebuy button
        game_.setInputMode(PokerTableInput.MODE_REBUY_CHECK);

        // init (this creates dialog by calling below)
        super.init(engine, context, gamephase);

        // record results of the human
        PokerPlayer human = game_.getHumanPlayer();
        PlayerProfile profile = human.getProfile();
        profile.addTournamentHistory(game_, human);
    }

    /**
     * start up, play any music
     */
    @Override
    public void start()
    {
        // music (before super call in case this is modal)
        EngineUtils.startBackgroundMusic(gamephase_, true);

        super.start();
    }

    /**
     * finish up, stop music
     */
    @Override
    public void finish()
    {
        AudioConfig.stopBackgroundMusic();
    }

    /**
     * end game
     */
    @Override
    public JComponent createDialogContents()
    {
        // setup window
        if (!bOnline_) getDialog().setClosable(false);
        if (bOnline_ || game_.isGameOver()) getDialog().setIconifiable(true);
        PokerPlayer human = game_.getHumanPlayer();

        // buttons
        game_.setInputMode(PokerTableInput.MODE_QUITSAVE);

        // base
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // demo?
        String sExtra = "";
        if (engine_.isDemo())
        {
            sExtra = PropertyConfig.getMessage("msg.gameover.demo",
                                               bOnline_ ? PokerUtils.DEMO_LIMIT_ONLINE : PokerUtils.DEMO_LIMIT);

            GlassButton order = new GlassButton("order", "Glass");
            order.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    context_.processPhase("Order");
                }
            });
            back_.getButtonBox().addButton(order);
            removeMatchingButton("yesWatch");
        }
        else if (game_.isGameOver())
        {
            sExtra = PropertyConfig.getMessage("msg.gameover.done");
        }
        else if (bOnline_)
        {
            if (human.isHost())
            {
                // remove quit button for host. We do this as opposed
                // to having a different gamedef.xml entry because the
                // TD supports only specifying one phase to run (as
                // opposed to one phase for host, one for client).  This
                // is the only instance of that, so we just do this here
                // instead of adding support in TD for different host/client
                // phases.
                removeMatchingButton("quit");
                sExtra = PropertyConfig.getMessage("msg.gameover.online.host");
            }
            else
            {
                sExtra = PropertyConfig.getMessage("msg.gameover.online");
            }
        }
        else
        {
            sExtra = PropertyConfig.getMessage("msg.gameover.practice");
        }

        String sMsg;
        if (human.getPlace() == 1)
        {
            sMsg = PropertyConfig.getMessage("msg.gameover.out.win");
        }
        else if (human.getPrize() > 0)
        {
            sMsg = PropertyConfig.getMessage("msg.gameover.out.money");
        }
        else if (game_.isGameOver())
        {
            sMsg = PropertyConfig.getMessage("msg.gameover.out.observer");
        }
        else if (engine_.isDemo() && human.isDemoLimit())
        {
            sMsg = PropertyConfig.getMessage("msg.gameover.out.demo");
        }
        else
        {
            sMsg = PropertyConfig.getMessage("msg.gameover.out.busted");
        }

        // label
        boolean bObs = human.isObserver() && human.getPlace() == 0;
        DDLabel label = new DDLabel(GuiManager.DEFAULT, STYLE);
        label.setText(Utils.fixHtmlTextFor15(PropertyConfig.getMessage(bObs ? "msg.gameover.obs" : "msg.gameover",
                                                                       sMsg,
                                                                       PropertyConfig.getPlace(human.getPlace()),
                                                                       game_.getNumPlayers(),
                                                                       human.getTotalSpent(),
                                                                       human.getPrize(),
                                                                       sExtra)));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        base.add(GuiUtils.CENTER(label), BorderLayout.NORTH);

        // results
        ChipLeaderPanel leader = new ChipLeaderPanel(context_, 120);
        leader.setPreferredSize(new Dimension(700, 330));
        leader.createUI();
        base.add(leader, BorderLayout.CENTER);

        // if human wins play cheers
        if (human.getPrize() > 0)
        {
            PokerUtils.cheerAudio();
        }

        return base;
    }

    @Override
    public boolean processButton(GameButton button)
    {
        if (button.getName().startsWith("playAgain"))
        {
            TypedHashMap params = new TypedHashMap();
            params.setObject(RestartTournament.PARAM_PROFILE, game_.getProfile());
            removeDialog();
            context_.restart("RestartTournament", params);
            return true;
        }

        return super.processButton(button);
    }
}
