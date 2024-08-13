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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.dashboard.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 15, 2005
 * Time: 11:23:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class HostPauseDialog extends DialogPhase
{
    private TournamentDirector td_;
    private PokerGame game_;
    private DDLabel label_;
    private boolean bAutoClose_;

    // for passing in alternate message key
    public static final String PARAM_MSG_KEY = "msg-key";

    // for marking this instace as auto-closable
    public static final String PARAM_AUTO_CLOSE = "auto-close";

    // current instance of this dialog
    private static HostPauseDialog impl_;

    /**
     * close current instance of this dialog
     */
    public static void autoClose()
    {
        if (impl_ != null && impl_.bAutoClose_)
        {
            GuiUtils.invoke(new Runnable() {
                public void run()
                {
                    impl_.removeDialog();
                }
            });
        }
    }

    /**
     * init ui
     */
    public JComponent createDialogContents()
    {
        game_ = (PokerGame) context_.getGame();
        td_ = (TournamentDirector) context_.getGameManager();

        label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        label_.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        return label_;
    }


    /**
     * start - set text (this is a cached phase, so need to update)
     */
    public void start()
    {
        impl_ = this;
        bAutoClose_ = gamephase_.getBoolean(PARAM_AUTO_CLOSE, false);
        String sKey = gamephase_.getString(PARAM_MSG_KEY, "msg.host.paused");
        label_.setText(PropertyConfig.getMessage(sKey));
        Dimension size = label_.getPreferredSize();
        size.height += 50;
        size.width += 20;
        getDialog().setSize(size);
        getDialog().center();

        HostDash.setEnabled(false);
        // if already visible, already paused, so don't re-pause
        if (!getDialog().isVisible())
        {
            td_.setPaused(true);
        }
        td_.sendDirectorChat(PropertyConfig.getMessage("msg.chat.paused"), Boolean.TRUE);
        game_.getGameClock().pause();
        super.start();
    }

    /**
     * cleanup
     */
    public void finish()
    {
        impl_ = null;
        game_.getGameClock().unpause();
        td_.sendDirectorChat(PropertyConfig.getMessage("msg.chat.resumed"), Boolean.FALSE);
        td_.setPaused(false);
        HostDash.setEnabled(true);
        super.finish();
    }
}
