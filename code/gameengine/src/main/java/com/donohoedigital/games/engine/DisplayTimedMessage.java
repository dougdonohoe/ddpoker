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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.gui.DDLabel;
import com.donohoedigital.gui.GuiManager;
import com.donohoedigital.gui.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 12, 2005
 * Time: 1:02:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class DisplayTimedMessage extends DisplayMessage implements CancelablePhase
{
    public static final String PARAM_SECONDS = "seconds";
    private DDLabel timer_;
    private int nSecondsLeft_;
    private volatile boolean bDone_ = false;
    private Thread thread_;

    /**
     * add timer display label
     */
    public JComponent createDialogContents()
    {
        nSecondsLeft_ = gamephase_.getInteger(PARAM_SECONDS, 5);

        JComponent sup = super.createDialogContents();
        timer_ = new DDLabel(GuiManager.DEFAULT, "DisplayTimedMessage");
        back_.getButtonBase().add(timer_, BorderLayout.SOUTH);
        timer_.setBorder(BorderFactory.createEmptyBorder(4, 10, 2, 0));
        updateTimer();
        return sup;
    }

    /**
     * start timer thread
     */
    public void start()
    {
        EngineUtils.addCancelable(this);
        thread_ = new Thread(new DisplayTimer(), "DisplayTimer");
        thread_.start();
        super.start();
    }

    /**
     * timer thread
     */
    private class DisplayTimer implements Runnable
    {
        public void run()
        {
            while (!bDone_)
            {
                Utils.sleepSeconds(1);
                if (bDone_) return;

                nSecondsLeft_--;
                if (nSecondsLeft_ <= 0)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            if (cancelButton_ != null) cancelButton_.doClick();
                        }
                    });
                    return;
                }
                else
                {
                    updateTimer();
                }
            }
        }
    }

    public void finish()
    {
        EngineUtils.removeCancelable(this);
        bDone_ = true;
        if (thread_ != null)
        {
            thread_.interrupt();
            thread_ = null;
        }
    }

    /**
     * process button - stop timer thread
     */
    public boolean processButton(GameButton button)
    {
        if (bDone_) return false;
        bDone_ = true;
        return super.processButton(button);
    }

    /**
     * display time left to act
     */
    private void updateTimer()
    {
        GuiUtils.invoke(new Runnable() {
            public void run() {
                String sSeconds = PropertyConfig.getMessage(nSecondsLeft_ == 1 ? "msg.seconds.singular" : "msg.seconds.plural",
                                                            nSecondsLeft_);

                timer_.setText(PropertyConfig.getMessage("msg.dialog.timer", sSeconds));
            }
        });
    }

    /**
     * Cancelable
     */
    public void cancelPhase()
    {
        if (cancelButton_ != null)
        {
            cancelButton_.doClick();
        }
        else
        {
            removeDialog();
        }
    }
}
