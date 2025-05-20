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
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import com.zookitec.layout.*;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpNext extends DashboardItem
{
    DDLabel labelBlinds_;

    public UpNext(GameContext context)
    {
        super(context, "next");
        trackTableEvents(PokerTableEvent.TYPE_LEVEL_CHANGED);
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();
        ExplicitLayout layout = new ExplicitLayout();
        base.setLayout(layout);

        // blinds
        labelBlinds_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        Expression width = DashboardClock.WIDTH;
        base.add(labelBlinds_, new ExplicitConstraints(labelBlinds_,
                        width.subtract(ComponentEF.preferredWidth(labelBlinds_)).multiply(.5d),
                        MathEF.constant(0),
                        ComponentEF.preferredWidth(labelBlinds_),
                        ComponentEF.preferredHeight(labelBlinds_)
                        ));
        layout.setPreferredLayoutSize(width, ComponentEF.preferredHeight(labelBlinds_));


        return base;
    }


    ///
    /// display logic
    ///

    /**
     * update level
     */
    protected void updateInfo()
    {
         // get level in game
        int nLevel = game_.getLevel();

        // if a current table exists, use level in that table instead
        // Note: this check shouldn't be necessary unless this item is
        // used in the poker clock functionality
        PokerTable table = game_.getCurrentTable();
        if (table != null) nLevel = table.getLevel();

        // next level for this
        nLevel++;
        TournamentProfile profile = game_.getProfile();
        TournamentProfileHtml html = new TournamentProfileHtml(profile);

        // break
        if (profile.isBreak(nLevel))
        {
            labelBlinds_.setText(PropertyConfig.getMessage("msg.dash.break", profile.getMinutes(nLevel)));
        }
        // ante and blinds
        else
        {
            labelBlinds_.setText(html.getBlindsText("msg.dash.", nLevel, false));
        }
    }
}
