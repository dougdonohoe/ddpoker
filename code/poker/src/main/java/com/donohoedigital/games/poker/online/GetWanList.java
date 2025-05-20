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
/*
 * GetWanList.java
 *
 * Created on November 17, 2004, 7:16 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;

/**
 *
 */
public class GetWanList extends SendMessageDialog
{
    // private static Logger logger = LogManager.getLogger(GetWanList.class);

    public static final String PARAM_OFFSET = "offset";
    public static final String PARAM_COUNT = "count";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_AUTH = "auth";

    private int offset_ = 0;
    private int count_ = -1;
    private int mode_ = -1;
    private OnlineProfile auth_ = null;


    /**
     * Retrieve client info then do normal initialization
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        offset_ = gamephase.getInteger(PARAM_OFFSET, offset_);
        count_ = gamephase.getInteger(PARAM_COUNT, count_);
        mode_ = gamephase.getInteger(PARAM_MODE, mode_);
        auth_ = (OnlineProfile) gamephase.getObject(PARAM_AUTH);

        super.init(engine, context, gamephase);
    }

    /**
     * message to send to server
     */
    @Override
    protected EngineMessage getMessage()
    {
        // Send a message requesting the current list
        OnlineMessage reqOnlineMsg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);
        reqOnlineMsg.setOffset(offset_);
        reqOnlineMsg.setCount(count_);
        if (mode_ > 0) reqOnlineMsg.setMode(mode_);
        if (auth_ != null) reqOnlineMsg.setWanAuth(auth_.getData());
        EngineMessage reqEngineMsg = new EngineMessage();
        reqOnlineMsg.getData().copyTo(reqEngineMsg);

        return reqEngineMsg;
    }

    /**
     * Message to display to user
     */
    @Override
    protected String getMessageKey()
    {
        return "msg.getWanList";
    }

    /**
     * Don't do server redirect query
     */
    @Override
    protected boolean doServerQuery()
    {
        return false;
    }
}
