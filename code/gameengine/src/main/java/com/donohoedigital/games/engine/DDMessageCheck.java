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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.comms.*;

/**
 *
 */
public class DDMessageCheck extends SendMessageDialog
{
    //static Logger logger = LogManager.getLogger(UpdateCheck.class);

    /**
     * return message for registering online game
     */
    @Override
    protected EngineMessage getMessage()
    {
        EngineMessage msg = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                              EngineMessage.PLAYER_NOTDEFINED,
                                              EngineMessage.CAT_CHECK_DDMSG);

        msg.setString(EngineMessage.PARAM_PATCH_OS, Utils.OS);
        msg.setLong(EngineMessage.PARAM_DDMSG_ID, gamephase_.getLong(EngineMessage.PARAM_DDMSG_ID));
        msg.setString(EngineMessage.PARAM_DDPROFILE, gamephase_.getString(EngineMessage.PARAM_DDPROFILE));

        return msg;
    }

    @Override
    protected String getMessageKey()
    {
        return "msg.ddmsgCheck";
    }

    @Override
    protected boolean doServerQuery()
    {
        return false;
    }

    public static EngineMessage checkUpdate(GameContext context, long last, String playerProfileName)
    {
        // Check for a patch to the current version.
        TypedHashMap params = new TypedHashMap();
        params.setLong(EngineMessage.PARAM_DDMSG_ID, last);
        params.setString(EngineMessage.PARAM_DDPROFILE, playerProfileName);

        // faceless, don't display errors
        params.setBoolean(SendMessageDialog.PARAM_FACELESS, Boolean.TRUE);
        params.setBoolean(SendMessageDialog.PARAM_FACELESS_ERROR, Boolean.FALSE);

        DDMessageCheck dialog = (DDMessageCheck) context.processPhaseNow("DDMessageCheck", params);

        return (dialog.getStatus() == DDMessageListener.STATUS_OK) ? dialog.getReturnMessage() : null;
    }
}