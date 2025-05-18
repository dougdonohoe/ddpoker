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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;

import java.util.*;

/**
 *
 */
public class SendWanGame extends SendMessageDialog
{
    public static final String PARAM_CATEGORY = "category";
    public static final String PARAM_GAME = "game";
    public static final String PARAM_AUTH = "auth";

    private static final Map<Integer, String> hmMessages_ = new HashMap<Integer, String>();

    static
    {
        hmMessages_.put(OnlineMessage.CAT_WAN_GAME_ADD, "msg.addWanGame");
    }

    private OnlineGame game_ = null;
    private OnlineProfile auth_ = null;
    private int category_ = -1;

    /**
     * Retrieve client info then do normal initialization
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        game_ = (OnlineGame) gamephase.getObject(PARAM_GAME);
        auth_ = (OnlineProfile) gamephase.getObject(PARAM_AUTH);
        category_ = gamephase.getInteger(PARAM_CATEGORY, category_);

        super.init(engine, context, gamephase);
    }

    /**
     * message to send to server
     */
    @Override
    protected EngineMessage getMessage()
    {
        OnlineMessage reqOnlineMsg = new OnlineMessage(category_);
        reqOnlineMsg.setWanGame(game_.getData());
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
        return hmMessages_.get(category_);
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
