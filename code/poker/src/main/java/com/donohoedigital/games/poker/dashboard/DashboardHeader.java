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

import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 17, 2005
 * Time: 3:15:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class DashboardHeader extends PillPanel
{
    DDImageCheckBox check_;
    DDImageButton delete_;
    DDImageButton up_;
    DDImageButton down_;

    public DashboardHeader(String sStyle, boolean bEditMode)
    {
        super(sStyle);
        setBorder(BorderFactory.createEmptyBorder(1,4,1,4));

        check_ = new DDImageCheckBox(bEditMode ? "dashedit":"dashboard", sStyle);
        check_.setVerticalAlignment(SwingConstants.TOP);
        check_.setIconTextGap(4);
        add(check_, BorderLayout.CENTER);

        if (!bEditMode)
        {
            delete_ = new DDImageButton("dashx");
            delete_.setTransparentIgnored(false);
            add(delete_, BorderLayout.EAST);
        }
        else
        {
            DDPanel move = new DDPanel();
            move.setLayout(new GridLayout(1,2,2,0));
            up_ = new DDImageButton("dashup");
            down_ = new DDImageButton("dashdown");
            up_.setTransparentIgnored(false);
            down_.setTransparentIgnored(false);

            move.add(down_);
            move.add(up_);
            
            add(move, BorderLayout.EAST);
        }
    }

    public void setText(String s)
    {
        String sNow = check_.getText();
        if (sNow == null || !sNow.equals(s))
        {
            check_.setText(s);
        }
    }
}
