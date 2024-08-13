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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;

import java.awt.*;
import java.awt.event.*;

public class PlayerTypeManager extends ProfileManagerPanel
{
    private GlassButton roster_;
    private GameContext context_;

    public PlayerTypeManager(GameEngine engine, GameContext context, String sStyle)
    {
        super(engine, context, GuiManager.DEFAULT, sStyle, "playertype", PlayerType.class);

        context_ = context;

        roster_ = new GlassButton("roster", "Glass");
        roster_.setPreferredSize(new Dimension(80, 24));
        roster_.setBorderGap(0, 0, 0, 0);
        roster_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setRoster();
            }
        });

        summaryBorder_.add(GuiUtils.EAST(roster_), BorderLayout.SOUTH);
    }

    /**
     * Set roster
     */
    private void setRoster()
    {
        PlayerType playerType = (PlayerType)profileList_.getSelectedProfile();
        TypedHashMap params = new TypedHashMap();

        params.setObject(PlayerTypeRosterDialog.PARAM_PROFILE, playerType);

        context_.processPhaseNow("PlayerTypeRosterDialog", params);

        /*
        ArrayList names = Roster.getRosterNameList(playerType);

        System.out.println("Roster contains " + names.size() + " names.");

        for (int i = 0; i < names.size(); ++i)
        {
            System.out.println("#" + i + ": '" + names.get(i) + "'");
        }
        */
    }
}
