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
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 16, 2005
 * Time: 2:08:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class OnlineDash extends DashboardItem
{
    private static final String SITOUT = PropertyConfig.getMessage("msg.sitout.title");
    private static final String OBSERVING = PropertyConfig.getMessage("msg.observing.title");

    private DDPanel base_;
    private PokerPlayer player_;
    private DDCheckBox sitout_, mucklose_, muckwin_;
    private TournamentDirector td_;

    public OnlineDash(GameContext context)
    {
        super(context, "onlineopt");

        player_ = game_.getLocalPlayer();
        setDynamicTitle(true);
        trackTableEvents(PokerTableEvent.TYPE_PLAYER_SETTINGS_CHANGED|
                         PokerTableEvent.TYPE_NEW_HAND); // for demo
        game_.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
        //game_.addPropertyChangeListener(PokerGame.PROP_GAME_OVER, this); // may be needed in future
    }

    private TournamentDirector getTD()
    {
        if (td_ == null)
        {
            td_ = (TournamentDirector) context_.getGameManager();
        }
        return td_;
    }

    protected JComponent createBody()
    {
        base_ = new DDPanel();
        base_.setLayout(new GridLayout(0,1,0,-5));
        createComponents();
        return base_;
    }

    /**
     * Create UI - mean to be called to re-init after player changes to observer
     */
    private void createComponents()
    {
        if (base_.getComponentCount() > 0) base_.removeAll();

        if (player_.isObserver())
        {
            createLabel();
            sitout_ = null;
            mucklose_ = null;
            muckwin_ = null;
        }
        else
        {
            sitout_ = new DDCheckBox("sitout", STYLE);
            sitout_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (PokerUtils.isDemoOver(context_, player_, true) && !sitout_.isSelected())
                    {
                        EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.onlinedone.demo"));
                        sitout_.setSelected(true);
                        return;
                    }

                    player_.setSittingOut(sitout_.isSelected());
                    getTD().playerUpdate(player_, player_.getOnlineSettings());
                }
            });
            base_.add(sitout_);

            mucklose_ = new DDCheckBox("mucklose", STYLE);
            mucklose_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    player_.setAskShowLosing(!mucklose_.isSelected());
                    getTD().playerUpdate(player_, player_.getOnlineSettings());
                }
            });
            base_.add(mucklose_);

            muckwin_ = new DDCheckBox("muckwin", STYLE);
            muckwin_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    player_.setAskShowWinning(!muckwin_.isSelected());
                    getTD().playerUpdate(player_, player_.getOnlineSettings());
                }
            });
            base_.add(muckwin_);
        }
    }

    private void createLabel()
    {
        DDLabel label = new DDLabel(player_.isWaiting() ? "waiting":"observing", STYLE);
        base_.add(label);
    }

    /**
     * dynamic title param, called after updateInfo()
     */
    protected Object getDynamicTitleParam()
    {
        if (player_.isObserver()) return OBSERVING;
        return player_.isSittingOut() ? SITOUT : null;
    }

    /**
     * update display if our player's settings changed
     */
    public void tableEventOccurred(PokerTableEvent event)
    {
        switch (event.getType())
        {
            case PokerTableEvent.TYPE_PLAYER_SETTINGS_CHANGED:
                // update board if settings changed for any player at table -
                // this single place catches all changes to setSittingOut()
                PokerPlayer player = event.getPlayer();
                if (player.getTable() == player_.getTable())
                {
                    PokerUtils.setConnectionStatus(context_, player, false);
                }

                if (player == player_)
                {
                    super.tableEventOccurred(event);
                }
                return;
        }

        super.tableEventOccurred(event);
    }


    /**
     * track when game loaded, might need to update if sitting out
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        if (name.equals(PokerGame.PROP_GAME_LOADED))
        {
            if (isDisplayed()) GuiUtils.invoke(updateRunner_);
        }
        // keep this incase we need to deal with game over
        //else if (name.equals(PokerGame.PROP_GAME_OVER))
        //{
        //    if (isDisplayed()) GuiUtils.invoke(updateRunner_);
        //}
        super.propertyChange(evt);
    }

    // runnable for setting label text in swing thread
    private Runnable updateRunner_ = new Runnable()
                        {
                            public void run()
                            {
                                updateAll();
                            }
                        };

    ///
    /// display logic
    ///

    /**
     * update level
     */
    protected void updateInfo()
    {
        if ((sitout_ != null && player_.isObserver()) ||
            (sitout_ == null && !player_.isObserver()))
        {
            createComponents();
            base_.repaint();
            return;
        }

        if (sitout_ != null && sitout_.isSelected() != player_.isSittingOut())
        {
            sitout_.setSelected(player_.isSittingOut());
        }

        if (mucklose_ != null && mucklose_.isSelected() != !player_.isAskShowLosing())
        {
            mucklose_.setSelected(!player_.isAskShowLosing());
        }

        if (muckwin_ != null && muckwin_.isSelected() != !player_.isAskShowWinning())
        {
            muckwin_.setSelected(!player_.isAskShowWinning());
        }

        if (sitout_ != null && PokerUtils.isDemoOver(context_, player_, true))
        {
            sitout_.setSelected(true);
            sitout_.setText(PropertyConfig.getMessage("checkbox.sitoutdemo.label"));
        }
    }
}
