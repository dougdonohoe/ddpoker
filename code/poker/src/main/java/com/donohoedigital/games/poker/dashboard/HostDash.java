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
import com.donohoedigital.games.engine.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 15, 2005
 * Time: 11:08:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class HostDash extends DashboardItem implements ActionListener
{
    private GlassButton button_;
    private static HostDash impl_;

    public HostDash(GameContext context)
    {
        super(context, "host");
        impl_ = this;
    }

    /**
     * finish - clear impl_
     */
    public void finish()
    {
        impl_ = null;
    }    

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));

        button_ = new GlassButton("hostpause", "Glass");
        button_.addActionListener(this);
        base.add(GuiUtils.CENTER(button_), BorderLayout.CENTER);

        return base;
    }

    public void actionPerformed(ActionEvent e)
    {
        context_.processPhaseNow("HostPauseDialog", null);
    }

    public static void setEnabled(boolean b)
    {
        if (impl_ != null && impl_.button_ != null)
        {
            impl_.button_.setEnabled(b);
        }
    }
}
