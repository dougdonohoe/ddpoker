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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.gui.*;
import com.zookitec.layout.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class DashboardClock extends DashboardItem implements GameClockListener
{
    static Expression WIDTH = MathEF.constant(ShowPokerTable.LEFT_PANEL_WIDTH - 30);

    DDLabel labelTime_;
    DDLabel labelLevel_;
    DDLabel labelBlinds_;

    public DashboardClock(GameContext context)
    {
        super(context, "clock");
        game_.getGameClock().addGameClockListener(this);
        setDynamicTitle(true);
        trackTableEvents(PokerTableEvent.TYPE_LEVEL_CHANGED);
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(2,0);

        // clock/level
        DDPanel top = new DDPanel();
        top.setLayout(new FlowLayout(FlowLayout.CENTER,20,0));
        base.add(top, BorderLayout.NORTH);

        labelLevel_ = new DDLabel(GuiManager.DEFAULT, STYLE_BIGGER);
        top.add(labelLevel_);

        DDPanel timepanel = new DDPanel();
        timepanel.setBorder(new DDBevelBorder("BrushedMetal", DDBevelBorder.LOWERED));
        labelTime_ = new DDLabel(GuiManager.DEFAULT, "DashboardClock");
        labelTime_.setOpaque(true);
        labelTime_.setHorizontalAlignment(SwingConstants.RIGHT);
        labelTime_.setText("2:00:00"); // force size of label to display hours
        labelTime_.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
        labelTime_.setPreferredSize(labelTime_.getPreferredSize());
        timepanel.add(labelTime_, BorderLayout.CENTER);
        top.add(timepanel);

        // blinds
        labelBlinds_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        base.add(GuiUtils.CENTER(labelBlinds_), BorderLayout.CENTER);

        // centering panel
        DDPanel centering = new DDPanel();
        ExplicitLayout layout = new ExplicitLayout();
        centering.setLayout(layout);
        centering.add(base, new ExplicitConstraints(base,
                        WIDTH.subtract(ComponentEF.preferredWidth(base)).multiply(.5d),
                        MathEF.constant(0),
                        ComponentEF.preferredWidth(base),
                        ComponentEF.preferredHeight(base)
                        ));
        layout.setPreferredLayoutSize(WIDTH, ComponentEF.preferredHeight(base));
        return centering;
    }

    protected Object getDynamicTitleParam()
    {
        return PokerUtils.getTimeString(game_);
    }

    protected void bodyHidden()
    {
        updateTime();
    }

    protected void headerDisplayed()
    {
        if (!isOpen()) updateTime();
    }


    public void tableEventOccurred(PokerTableEvent event)
    {
        updateLevel();
    }

    ///
    /// display logic
    ///

    protected void updateInfo()
    {
        updateTime();
        updateLevel();
    }

    protected void updateTitle()
    {
        // handled in updateTime()
    }

    /**
     * update time
     */
    private void updateTime()
    {
        if (!isDisplayed()) return;

        PokerUtils.updateTime(game_, labelTime_);
        setTitle(getTitle());
    }

    /**
     * update level
     */
    private void updateLevel()
    {
        if (!isDisplayed()) return;

         // get level in game
        int nLevel = game_.getLevel();

        // if a current table exists, use level in that table instead
        // Note: this check shouldn't be necessary unless this item is
        // used in the poker clock functionality
        PokerTable table = game_.getCurrentTable();
        if (table != null) nLevel = table.getLevel();

        String sLevel = PropertyConfig.getMessage("msg.dash.level", nLevel);
        labelLevel_.setText(sLevel);
        TournamentProfile profile = game_.getProfile();

        // break
        if (profile.isBreak(nLevel))
        {
            labelBlinds_.setText(PropertyConfig.getMessage("msg.dash.break",
                                                           profile.getMinutes(nLevel)));
        }
        // ante and blinds
        else
        {
            // show gametype if different from default
            String sGameType = profile.getGameTypeDisplay(nLevel);
            if (!sGameType.isEmpty()) sGameType = PropertyConfig.getMessage("msg.dash.gametype", sGameType);
            
            int nAnte = profile.getAnte(nLevel);
            int nBig = profile.getBigBlind(nLevel);
            int nSmall = profile.getSmallBlind(nLevel);
            labelBlinds_.setText(PropertyConfig.getMessage(nAnte == 0 ? "msg.dash.blinds" : "msg.dash.blinds.a",
                                                       nSmall,
                                                       nBig,
                                                       nAnte == 0 ? null : nAnte,
                                                       sGameType));
        }

    }

    ////
    //// GameClockListener
    ////

    public void gameClockStarted(GameClock clock)
    {
    }

    public void gameClockStopped(GameClock clock)
    {
    }

    public void gameClockTicked(GameClock clock)
    {
        GuiUtils.invoke(updateTimeRunner_);
    }

    public void gameClockSet(GameClock clock)
    {
        GuiUtils.invoke(updateTimeRunner_);
    }

    // runnable for invoking clock ticked event in swing thread
    private Runnable updateTimeRunner_ = new Runnable()
                        {
                            public void run()
                            {
                                updateTime();
                            }
                        };
}
