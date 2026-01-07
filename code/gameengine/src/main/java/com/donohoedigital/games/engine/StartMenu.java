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
/*
 * StartMenu.java
 *
 * Created on November 15, 2002, 3:41 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.gui.*;

import java.awt.*;
import java.awt.event.*;

/**
 * @author Doug Donohoe
 */
public class StartMenu extends MenuPhase
{
    //static Logger logger = LogManager.getLogger(StartMenu.class);

    static final String PARAM_EXPIRED = "expired";

    protected boolean bExpired_ = false;

    /**
     * if expired, show expired message
     */
    @Override
    protected void addButtons(DDPanel parent)
    {
        bExpired_ = gamephase_.getBoolean(PARAM_EXPIRED, false);

        // put buttons in the menubox_
        if (!bExpired_)
        {
            super.addButtons(parent);
        }
        else
        {
            // expired message - just exit button
            DDButton exit = new GlassButton("exit", "GlassBig");
            parent.add(GuiUtils.CENTER(exit), BorderLayout.SOUTH);
            exit.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    System.exit(0);
                }
            });
        }
    }

    @Override
    public void start()
    {

        if (!bExpired_)
        {

        }

        super.start();

        // set help text
        if (bExpired_)
        {
            context_.getWindow().setHelpTextWidget(null);
            helptext_.setText(engine_.getExpiredMessage());
        }
    }

    /**
     * Is this the actual very first start menu?
     */
    protected boolean isStartMenu()
    {
        return gamephase_.getName().equals("StartMenu");
    }
}
