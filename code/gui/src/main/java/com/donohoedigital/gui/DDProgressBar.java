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
package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 25, 2005
 * Time: 8:50:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDProgressBar extends DDPanel implements DDProgressFeedback, DDTextVisibleComponent
{
    private DDProgressFeedback feedback_;
    private JProgressBar progress_;
    private DDLabel label_;

    /**
     * Creates a new instance of DDProgressBar
     */
    public DDProgressBar() {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT, true);
    }

    public DDProgressBar(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT, true);
    }

    public DDProgressBar(String sName, String sStyle)
    {
        super();
        init(sName, sStyle, true);
    }

    public DDProgressBar(String sName, String sStyle, boolean bShowLabel)
    {
        super();
        init(sName, sStyle, bShowLabel);
    }

    private void init(String sName, String sStyle, boolean bShowLabel)
    {
        setBorderLayoutGap(4,0);

        progress_ = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progress_.setValue(0);
        progress_.setStringPainted(true);
        progress_.setOpaque(false);
        add(progress_, BorderLayout.CENTER);

        if (bShowLabel)
        {
            label_ = new DDLabel();
            add(label_, BorderLayout.SOUTH);
        }

        GuiManager.init(this, sName, sStyle);
    }

    public JProgressBar getProgressBar()
    {
        return progress_;
    }

    public void setProgressFeedback(DDProgressFeedback feedback)
    {
        feedback_ = feedback;
    }

    public void setBackground(Color bg)
    {
        super.setBackground(bg);
        if (label_ != null) label_.setBackground(bg);
        if (progress_ != null) progress_.setBackground(bg);
    }

    public void setForeground(Color fg)
    {
        super.setForeground(fg);
        if (label_ != null) label_.setForeground(fg);
        if (progress_ != null) progress_.setForeground(fg);
    }

    public void setFont(Font font)
    {
        super.setFont(font);
        if (label_ != null) label_.setFont(font);
        if (progress_ != null) progress_.setFont(font);
    }

    public boolean isStopRequested()
    {
        if (feedback_ != null)
        {
            return feedback_.isStopRequested();
        }
        return false;
    }

    public void setMessage(String sMessage)
    {
        _setMessage(sMessage);

        if (feedback_ != null)
        {
            feedback_.setMessage(sMessage);
        }
    }

    public void _setMessage(final String sMessage)
    {
       if (label_ == null) return;
        GuiUtils.invoke(new Runnable() {
            String msg_ = sMessage;
            public void run()
            {
                label_.setText(msg_);
            }
        });
    }

    public void setPercentDone(final int n)
    {
        GuiUtils.invoke(new Runnable() {
            int n_ = n;
            public void run()
            {
                progress_.setValue(n_);
                progress_.setString(n_ + "%");
            }
        });

        if (feedback_ != null)
        {
            feedback_.setPercentDone(n);
        }
    }

    public void setFinalResult(Object oResult)
    {
        _setMessage("");
        
        if (feedback_ != null)
        {
            feedback_.setFinalResult(oResult);
        }
    }

    public void setIntermediateResult(Object oResult)
    {
        if (feedback_ != null)
        {
            feedback_.setIntermediateResult(oResult);
        }
    }

    public String getType()
    {
        return "progress";
    }
}
