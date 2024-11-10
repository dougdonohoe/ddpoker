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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyTable extends DashboardItem implements ActionListener
{
    private DDLabel labelInfo_;
    private GlassButton change_;
    private JComponent changebase_;

    public MyTable(GameContext context)
    {
        super(context, "mytable");
        setDynamicTitle(true);
        trackTableEvents(PokerTableEvent.TYPE_NEW_HAND);
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(5,0);

        labelInfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        base.add(labelInfo_, BorderLayout.NORTH);

        change_ = new GlassButton("changetable", "Glass");
        changebase_ = GuiUtils.CENTER(change_);
        base.add(changebase_, BorderLayout.CENTER);
        change_.addActionListener(this);

        return base;
    }

    /**
     * change table
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        context_.processPhaseNow("ChangeTableDialog", null);
    }

    protected Object getDynamicTitleParam()
    {
        PokerTable table = game_.getCurrentTable();
        return table.getNumber();
    }

    ///
    /// display logic
    ///

    /**
     * update level
     */
    protected void updateInfo()
    {
        PokerTable table = game_.getCurrentTable();
        PokerPlayer human = game_.getHumanPlayer();

        String sMsgKey;
        int nHandNum = table.getHandNum();
        boolean bObs = human.isObserver();

        if (nHandNum == 0)
		{
			sMsgKey = bObs ? "msg.mytable.start.obs" : "msg.mytable.start";
		}
		else
		{
            sMsgKey = bObs ? "msg.mytable.obs" : "msg.mytable";
        }

        labelInfo_.setText(PropertyConfig.getMessage(sMsgKey,
                                human.getTable().getNumber(),
                                bObs ? null : human.getSeat() + 1,
                                ""+nHandNum) // use "" + to not get commas
                                );

        if (changebase_.isVisible() != bObs)
        {
            changebase_.setVisible(bObs);
        }
    }
}
