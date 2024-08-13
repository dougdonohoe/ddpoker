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

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 29, 2005
 * Time: 8:07:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class PokerErrorDialog extends MessageErrorDialog
{
    public static final String PARAM_URL = "url";
    public static final String PARAM_ATTEMPT = "attempts";

    private PokerURL url_;
    private DDLabel attempts_;

    /**
     * get url
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        url_ = (PokerURL) gamephase.getObject(PARAM_URL);
        super.init(engine, context, gamephase);
    }

    /**
     * Create UI
     */
    public JComponent createDialogContents()
    {
        JComponent comp = super.createDialogContents();

        // add our own label
        attempts_ = new DDLabel(GuiManager.DEFAULT, "Rejoin");
        bottom_.add(attempts_, BorderLayout.SOUTH);
        attempts_.setBorder(BorderFactory.createEmptyBorder(0,25,5,0));

        return comp;
    }

    /**
     * Overriden to just center so as to not obscure LED in lobby
     */
    protected int getDialogPosition(InternalDialog dialog)
    {
        //dialog.setCenterAdjust(0, -(nHeightAdjust_/4));
        return InternalDialog.POSITION_CENTER_ADJUST;
    }

    /**
     * update reconnect attempts label before passing to super
     */
    public void messageReceived(DDMessage message)
    {
        int n = gamephase_.getInteger(PARAM_ATTEMPT, 1);
        attempts_.setText(PropertyConfig.getMessage("msg.reconnect.attempts", new Integer(n)));
        super.messageReceived(message);
    }

    /**
     * Return message to display from gamephase params PARAM_DISPLAY
     */
    protected String getMessageString()
    {
        return gamephase_.getString(PokerP2PDialog.PARAM_DISPLAY);
    }

    /**
     * Port part of URL for use in error message
     */
    protected int getPort()
    {
        return url_.getPort();
    }
}
