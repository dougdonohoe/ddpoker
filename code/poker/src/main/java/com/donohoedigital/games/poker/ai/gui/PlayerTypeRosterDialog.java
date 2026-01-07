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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import java.awt.*;

public class PlayerTypeRosterDialog extends DialogPhase
{
    public static final String PARAM_PROFILE = "profile";

    DDHtmlArea html_;
    DDTextArea list_;

    private PlayerType playerType_;

    public JComponent createDialogContents()
    {
        playerType_ = (PlayerType)gamephase_.getObject(PARAM_PROFILE);

        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        ((BorderLayout)base.getLayout()).setVgap(10);

        html_ = new DDHtmlArea();
        html_.setBorder(BorderFactory.createEmptyBorder());
        html_.setPreferredSize(new Dimension(600,120));
        html_.setText(PropertyConfig.getMessage("panel.roster.help", playerType_.getName()));

        list_ = new DDTextArea();
        list_.setPreferredSize(new Dimension(600,300));
        list_.setText(Roster.getRoster(playerType_));
        list_.setLineWrap(true);
        list_.setWrapStyleWord(true);

        base.add(html_, BorderLayout.NORTH);
        base.add(list_, BorderLayout.CENTER);

        return base;
    }

    public boolean processButton(GameButton button)
    {
        setResult(null);

        if (button.getName().equals(okayButton_.getName()))
        {
            Roster.setRoster(playerType_, list_.getText());
        }

        removeDialog();

        return true;
    }
}
