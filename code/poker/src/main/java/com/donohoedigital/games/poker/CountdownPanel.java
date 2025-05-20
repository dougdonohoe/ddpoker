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
package com.donohoedigital.games.poker;

import com.donohoedigital.gui.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 22, 2006
 * Time: 11:04:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class CountdownPanel extends DDPanel implements ActionListener
{
    private PokerGame game_;
    private Timer count_;
    private int timeout_;
    private int thinkbank_;
    private double total_;
    private long timepoint_;
    private long elapsed_;
    private GameClock clock_;
    private boolean bSupported_; // BUG 498

    public CountdownPanel(PokerGame game)
    {
        game_ = game;
        clock_ = game_.getGameClock();
        setPreferredHeight(5);
        bSupported_ = !game_.getHost().getVersion().isBefore(PokerConstants.VERSION_COUNTDOWN_CHANGED);
    }

    /**
     * set time
     */
    private void setTime(int nTimeOutMillis, int nThinkBankMillis)
    {
        timeout_ = nTimeOutMillis;
        thinkbank_ = nThinkBankMillis;
        if (thinkbank_ < 1000)
        {
            timeout_ += thinkbank_;
            thinkbank_ = 0;
        }
        total_ = timeout_ + thinkbank_;
    }

    /**
     * elapsed time
     */
    private long getElapsed()
    {
        return elapsed_;
    }

    /**
     * add elapsed
     */
    private void addElapsed()
    {
        long now = System.currentTimeMillis();
        if (!clock_.isPaused())
        {
            elapsed_ += now - timepoint_;
        }
        timepoint_ = now;
    }

    /**
     * add to elapsed;
     */

    /**
     * start/stop countdown
     */
    public void countdown(boolean b)
    {
        if (!PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_COUNTDOWN, true) || !bSupported_) b = false;

        if (b)
        {
            if (count_ != null) return;
            PokerPlayer player = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();
            setTime(player.getTimeoutMillis(), player.getThinkBankMillis());
            timepoint_ = System.currentTimeMillis();
            elapsed_ = 0;
            count_ = new Timer(100, this);
            setToolTip();
            count_.start();
        }
        else
        {
            if (count_ == null) return;
            setToolTip();
            count_.stop();
            count_ = null;
            actionPerformed(null);
        }
    }

    /**
     * timer event - repaint
     */
    public void actionPerformed(ActionEvent e)
    {
        addElapsed();
        setToolTip();
        repaint();
    }

    private String secPlural = PropertyConfig.getMessage("msg.seconds.plural", "");
    private String secSingular = PropertyConfig.getMessage("msg.seconds.singular", "");

    /**
     * tooltip text
     */
    private void setToolTip()
    {
        String sToolTip = null;
        if (count_ != null)
        {

            int to = timeout_ / 1000;
            int tb = thinkbank_ / 1000;
            sToolTip = PropertyConfig.getMessage("msg.tooltip.countdown",
                                                 to, (to == 1 ? secSingular : secPlural),
                                                 tb, (tb == 1 ? secSingular : secPlural),
                                                 getElapsed() / 1000,  ((int) total_) / 1000);
        }
        setToolTipText(sToolTip);
    }

    /**
     * paint countdown bar
     */
    public void paintComponent(Graphics g1)
    {
        if (count_ == null) return; // no painting if no counter

        Graphics2D g = (Graphics2D) g1;
        Insets in = getInsets();

        int x = in.left;
        int y = in.top;
        int width = getWidth() - (in.left + in.right);
        int height = getHeight() - (in.top + in.bottom);

        double time = timeout_ / total_;
        int timeoutW = (int) (width * time);
        int thinkW = width - timeoutW;

        // bg
        g.setColor(SHADOW);
        g.fillRect(x+1, y+1, width, height);

        // timeout
        g.setColor(TIMEOUT);
        g.fillRect(x, y, timeoutW, height);

        // thinkbank
        if (thinkbank_ > 0)
        {
            g.setColor(THINK);
            g.fillRect(x + timeoutW, y, thinkW, height);
        }

        long elapsed = getElapsed();
        time = elapsed / total_;
        int expired = (int) (width * time);
        if (expired > width) expired = width;

        // elapsed
        g.setColor(EXPIRED);
        g.fillRect(x, y, expired, height);
    }

    Color SHADOW = new Color(0, 0, 0, 100);
    Color TIMEOUT = new Color(0, 222, 0);
    Color THINK = new Color(255, 255, 0);
    Color EXPIRED = new Color(255, 0, 0, 175);
}
