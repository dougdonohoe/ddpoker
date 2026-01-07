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
 * SendWanProfile.java
 *
 * Created on November 17, 2004, 7:16 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;

import java.util.*;

/**
 *
 */
public class SendWanProfile extends SendMessageDialog
{
    public static final String PARAM_AUTH = "auth";
    public static final String PARAM_CATEGORY = "category";
    public static final String PARAM_PROFILE = "profile";

    private static final Map<Integer, String> hmMessages_ = new HashMap<Integer, String>();

    static
    {
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_ADD, "msg.addWanProfile");
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_ACTIVATE, "msg.activateWanProfile");
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_CHANGE_PASSWORD, "msg.changePassWanProfile");
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_LINK, "msg.getWanProfile");
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_SEND_PASSWORD, "msg.sendWanProfile");
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_SYNC_PASSWORD, "msg.sendWanProfile");
        hmMessages_.put(OnlineMessage.CAT_WAN_PROFILE_RESET, "msg.updateWanProfile");
    }

    private OnlineProfile profile_ = null;
    private OnlineProfile auth_ = null;
    private int category_ = -1;
    private boolean autoClose_ = false;

    /**
     * Retrieve client info then do normal initialization
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        profile_ = (OnlineProfile) gamephase.getObject(PARAM_PROFILE);
        auth_ = (OnlineProfile) gamephase.getObject(PARAM_AUTH);
        category_ = gamephase.getInteger(PARAM_CATEGORY, -1);

        // Close immediately after a delete and get
        if (OnlineMessage.CAT_WAN_PROFILE_LINK == category_)
        {
            autoClose_ = true;
        }

        super.init(engine, context, gamephase);
    }

    /**
     * Message to send to server
     */
    @Override
    protected EngineMessage getMessage()
    {
        OnlineMessage reqOnlineMsg = new OnlineMessage(category_);
        reqOnlineMsg.setOnlineProfileData(profile_.getData());
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

    /**
     * Optionally keep the dialog open to display status text
     */
    @Override
    protected boolean isAutoClose()
    {
        return autoClose_;
    }

    /**
     * Send the profile
     */
    static boolean sendWanProfile(GameContext context, int category, OnlineProfile profile, OnlineProfile auth)
    {
        SendMessageDialog dialog = sendWanProfileDialog(context, category, profile, auth);

        return (dialog.getStatus() == DDMessageListener.STATUS_OK);
    }

    static SendMessageDialog sendWanProfileDialog(GameContext context, int category, OnlineProfile profile, OnlineProfile auth)
    {
        // Send a message with the given category
        TypedHashMap hmParams = new TypedHashMap();
        hmParams.setObject(PARAM_PROFILE, profile);
        if (auth != null) hmParams.setObject(PARAM_AUTH, auth);
        hmParams.setInteger(PARAM_CATEGORY, category);

        return (SendMessageDialog) context.processPhaseNow("SendWanProfile", hmParams);
    }

}
