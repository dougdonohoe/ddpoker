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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.engine.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 16, 2006
 * Time: 2:30:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatLobbyPanel extends ChatPanel
{
    private ChatLobbyManager mgr_;
    private PlayerProfile profile_;
    protected String cAdmin_;
    protected String cAdminBG_;

    /**
     * new chat panel
     */
    public ChatLobbyPanel(GameContext context, ChatLobbyManager mgr, PlayerProfile profile, String sStyle, String sBevelStyle)
    {
        super(null, context, null, sStyle, sBevelStyle, false);
        mgr_ = mgr;
        profile_ = profile;
        cAdmin_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.admin", Color.black));
        cAdminBG_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.adminbg", Color.white));
    }

    /**
     * Size in OnlineStartMenu
     */
    protected Dimension getChatListPreferredSize()
    {
        return new Dimension(250, 245);
    }

    /**
     * cleanup
     */
    public void finish()
    {
        super.finish();
        profile_ = null;
    }

    /**
     * Send a chat message
     */
    protected void sendChat()
    {
        String sMsg = Utils.encodeHTML(msg_.getText(), false);

        msg_.setText("");
        if (mgr_ != null) mgr_.sendChat(profile_, sMsg);

        displayMessage(profile_.getName(), "key-not-needed", OnlineMessage.CAT_CHAT, sMsg, true);
    }

    /**
     * Send test chat
     */
    protected void sendChatTest(String sMsg, String sTestData)
    {
        mgr_.sendChat(profile_, sMsg);
    }

    /**
     * OnlineMessage received.
     */
    public void chatReceived(OnlineMessage omsg)
    {
        // calls to this are synchronized through the OnlineManager
        final String sMsg = omsg.getChat();
        final String sFrom = omsg.getPlayerName();
        final String sFromKey = omsg.getKey();
        final int nType = omsg.getCategory();

        if (sMsg == null) return; // for chat-admin messages with no chat

        // need to update from Swing thread
        SwingUtilities.invokeLater(
            new Runnable() {
                String _sMsg = sMsg;
                String _sFrom = sFrom;
                String _sFromKey = sFromKey;
                int _nType = nType;
                public void run() {
                    displayMessage(_sFrom, _sFromKey, nType, _sMsg, false);
                }
            }
        );
    }

    /**
     * for test messages only
     */
    protected void displayMessage(int nFrom, int nType, String sMsg, boolean bLocal)
    {
        displayMessage("Test", "key-not-needed", OnlineMessage.CAT_CHAT, sMsg, bLocal);
    }

    /**
     * add message to chat window
     */
    void displayMessage(String sFrom, String sFromKey, int nType, String sMsg, boolean bLocal)
    {
        String bgColor = null;
        String chatColor = null;
        String sName = null;
        String sKey;

        if (nType == OnlineMessage.CAT_CHAT_ADMIN || sMsg.startsWith("tahoezorro"))
        {
            chatColor = cAdmin_;
            bgColor = cAdminBG_;
            sKey = "msg.chat.admin";
        }
        else
        {
            // muted specifically?
            if (!bLocal && (muted_.containsPlayer(sFrom) || muted_.containsKey(sFromKey))) return;

            sName = Utils.encodeHTML(sFrom);
            chatColor = bLocal ? cLocal_ : cRemote_;
            if (isDDPoker(sFrom) && !bLocal)
            {
                chatColor = cDDPoker_;
            }
            sKey = "msg.chat";
        }

        String sAppend = PropertyConfig.getMessage(sKey, chatColor, sName, sMsg, bgColor);

        displayMessage(chatList_[0], sAppend, -1);
    }
}
