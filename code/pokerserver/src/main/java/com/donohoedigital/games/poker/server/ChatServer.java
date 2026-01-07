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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.poker.service.*;
import com.donohoedigital.games.server.service.*;
import com.donohoedigital.udp.*;
import org.apache.logging.log4j.*;
import org.springframework.beans.factory.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 16, 2006
 * Time: 11:58:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatServer implements UDPLinkHandler, UDPManagerMonitor, UDPLinkMonitor
{
    private static Logger logger = LogManager.getLogger(ChatServer.class);

    private OnlineProfileService onlineProfileService;
    private BannedKeyService bannedKeyService;

    // members
    private UDPServer udp_;
    private int nPort_;
    private final List<LinkInfo> links_ = Collections.synchronizedList(new ArrayList<LinkInfo>());

    /**
     * Constructor
     */
    ChatServer(UDPServer udp)
    {
        udp_ = udp;
        nPort_ = PropertyConfig.getRequiredIntegerProperty("settings.udp.chat.port");
    }

    /**
     * Get online profile service
     */
    public OnlineProfileService getOnlineProfileService()
    {
        return onlineProfileService;
    }

    /**
     * Set online game service
     */
    @Autowired
    public void setOnlineProfileService(OnlineProfileService onlineProfileService)
    {
        this.onlineProfileService = onlineProfileService;
    }

    /**
     * Get banned key service
     */
    public BannedKeyService getBannedKeyService()
    {
        return bannedKeyService;
    }

    /**
     * Set banned key service
     */
    @Autowired
    public void setBannedKeyService(BannedKeyService bannedKeyService)
    {
        this.bannedKeyService = bannedKeyService;
    }

    /**
     * is this a chat link?
     */
    public boolean isChat(UDPLink link)
    {
        return (link.getLocalIP().getPort() == nPort_);
    }

    ////
    //// UDPLinkMonitor (UDP)
    ////

    public void monitorEvent(UDPLinkEvent event)
    {
        UDPLink link = event.getLink();
        UDPData data = event.getData();

        switch (event.getType())
        {
            case RECEIVED:
                // process message
                if (data.getType() == UDPData.Type.MESSAGE)
                {
                    switch (data.getUserType())
                    {
                        case PokerConstants.USERTYPE_HELLO:
                            addUser(link, data);
                            break;

                        case PokerConstants.USERTYPE_CHAT:
                            if (!logChat(link, data))
                            {
                                synchronized (links_)
                                {
                                    for (LinkInfo fwd : links_)
                                    {
                                        if (fwd.link != link)
                                        {
                                            // creates new UDPData (needed to track send status per link), but shares bytes
                                            fwd.link.queue(data.getData(), data.getOffset(), data.getLength(), data.getUserType());

                                            // queue for sending
                                            udp_.manager().addLinkToSend(fwd.link);
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
                break;
        }
    }

    /**
     * add user to list
     */
    private void addUser(UDPLink link, UDPData data)
    {
        PokerUDPTransporter msg = new PokerUDPTransporter(data);
        OnlineMessage om = new OnlineMessage(msg.getMessage());
        DMTypedHashMap authData = om.getWanAuth();
        OnlineProfile auth = new OnlineProfile(authData);
        String sRealKey = auth.getLicenseKey();

        // validate license key
        EngineMessage validateThis = new EngineMessage("ChatServer", EngineMessage.PLAYER_SERVER, EngineMessage.CAT_VERIFY_KEY);
        validateThis.setKey(sRealKey);
        validateThis.setVersion(om.getData().getVersion());
        EngineMessage validate = PokerServlet.validateKeyAndVersion(validateThis, Utils.getAddress(link.getRemoteIP()),
                                                                    bannedKeyService,
                                                                    PokerConstants.VERSION,
                                                                    PokerConstants.getKeyStart(validateThis.getVersion()),
                                                                    true, true, false);
        if (validate != null)
        {
            sendError(link, validate.getApplicationErrorMessage());
            return;
        }

        // get details
        OnlineProfile user = onlineProfileService.getOnlineProfileByName(auth.getName());

        // check for temp ban (handles null user)
        String banCheck = PokerServlet.banCheck(bannedKeyService, user);
        if (banCheck != null)
        {
            sendError(link, banCheck);
            return;
        }
        // if no user or bad auth or not activated, return message
        if (user == null || !user.getPassword().equals(auth.getPassword()) || !user.isActivated())
        {
            sendError(link, PropertyConfig.getMessage("msg.wanprofile.unavailable"));
            return;
        }

        // save player (look for dups)
        OnlinePlayerInfo oinfo = new OnlinePlayerInfo();
        oinfo.setName(user.getName());
        oinfo.setPublicUseKey(om.getData().getKey());
        oinfo.setCreateDate(user.getCreateDate());
        LinkInfo info = new LinkInfo(link, sRealKey, oinfo); // store key that is being used

        // get aliases of original profile (using original key, not current key)
        List<OnlineProfile> aliases = onlineProfileService.getAllOnlineProfilesForEmail(user.getEmail(), user.getName());
        if (!aliases.isEmpty())
        {
            OnlinePlayerInfo oalias;
            DMArrayList<DMTypedHashMap> oaliases = new DMArrayList<DMTypedHashMap>(aliases.size());
            for (OnlineProfile alias : aliases)
            {
                oalias = new OnlinePlayerInfo();
                oalias.setName(alias.getName());
                oalias.setCreateDate(alias.getCreateDate());

                // TODO: stats?

                oaliases.add(oalias.getData());
            }
            oinfo.setAliases(oaliases);
        }

        // add link (look for duplicate)
        synchronized (links_)
        {
            LinkInfo loop;
            Iterator<LinkInfo> iter = links_.iterator();
            while (iter.hasNext())
            {
                loop = iter.next();

                // same link
                if (loop.equals(info))
                {
                    // already there, probably reconnecting due to perceived time-out, remove and re-add player info below
                    iter.remove();
                    break;
                }
                // same key different link - close existing
                else if (loop.sRealKey.equals(sRealKey))
                {
                    logger.info("Duplicate key rejected: " + sRealKey + " for " + info.player.getName());
                    sendError(loop.link, PropertyConfig.getMessage("msg.chat.dupkey", Utils.encodeHTML(info.player.getName()),
                                                                   sRealKey));
                    break;
                }
                // same profile different link - close existing
                else if (loop.player.getNameLower().equals(info.player.getNameLower()))
                {
                    logger.info("Duplicate profile rejected: " + sRealKey + " for " + info.player.getName());
                    sendError(loop.link, PropertyConfig.getMessage("msg.chat.dupprofile", Utils.encodeHTML(info.player.getName()),
                                                                   Utils.getAddress(link.getRemoteIP())));
                    break;
                }
            }

            links_.add(info);
        }

        // set name of link to player name
        link.setName(info.player.getName());

        // notify users, send welcome
        sendWelcome(link, PropertyConfig.getMessage("msg.chat.welcome", Utils.encodeHTML(info.player.getName())));
        sendJoinLeaveAll(info, true, link);

        // log hello
        logger.info(info + " HELLO (" + Utils.getAddressPort(link.getRemoteIP()) + ')');

        // list all players
        logPlayers();
    }

    private void logPlayers()
    {
        synchronized (links_)
        {
            logger.debug(links_.size() + " players in lobby:");
            for (LinkInfo i : links_)
            {
                logger.debug("  ==> " + i);

            }
        }
    }

    /**
     * remove user from the list
     */
    private void removeUser(UDPLink link)
    {
        LinkInfo search = null;
        synchronized (links_)
        {
            for (LinkInfo info : links_)
            {
                if (info.link == link)
                {
                    search = info;
                }
            }

            if (search == null) return;

            links_.remove(search);
        }

        // notify users
        sendJoinLeaveAll(search, false, null);

        // log goodbye
        logger.info(search + " GOODBYE");
    }

    /**
     * send chat to all from admin
     */
    private void sendJoinLeaveAll(LinkInfo who, boolean bJoin, UDPLink skip)
    {
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        omsg.setChat(PropertyConfig.getMessage(bJoin ? "msg.chat.hello" : "msg.chat.goodbye", Utils.encodeHTML(who.player.getName())));
        omsg.setChatType(bJoin ? PokerConstants.CHAT_ADMIN_JOIN : PokerConstants.CHAT_ADMIN_LEAVE);
        omsg.setPlayerInfo(who.player);
        PokerUDPTransporter pudp = new PokerUDPTransporter(omsg.getData());

        synchronized (links_)
        {
            for (LinkInfo info : links_)
            {
                if (info.link == skip) continue;

                // creates new UDPData (needed to track send status per link), but shares bytes
                info.link.queue(pudp.getData(), PokerConstants.USERTYPE_CHAT);

                // queue for sending
                udp_.manager().addLinkToSend(info.link);
            }
        }
    }

    /**
     * send chat to all from admin
     */
    private void sendWelcome(UDPLink link, String sMessage)
    {
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        omsg.setChat(sMessage);
        omsg.setChatType(PokerConstants.CHAT_ADMIN_WELCOME);
        omsg.setPlayerList(getPlayerList());
        PokerUDPTransporter pudp = new PokerUDPTransporter(omsg.getData());

        // creates new UDPData (needed to track send status per link), but shares bytes
        link.queue(pudp.getData(), PokerConstants.USERTYPE_CHAT);

        // queue for sending
        udp_.manager().addLinkToSend(link);
    }

    /**
     * send chat to all from admin
     */
    private void sendMessage(UDPLink link, String sMessage)
    {
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        omsg.setChat(sMessage);
        PokerUDPTransporter pudp = new PokerUDPTransporter(omsg.getData());

        // creates new UDPData (needed to track send status per link), but shares bytes
        link.queue(pudp.getData(), PokerConstants.USERTYPE_CHAT);

        // queue for sending
        udp_.manager().addLinkToSend(link);
    }

    /**
     * send chat to all from admin
     */
    private void sendError(UDPLink link, String sMessage)
    {
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        omsg.setChat(sMessage);
        omsg.setChatType(PokerConstants.CHAT_ADMIN_ERROR);
        PokerUDPTransporter pudp = new PokerUDPTransporter(omsg.getData());

        // creates new UDPData (needed to track send status per link), but shares bytes
        link.queue(pudp.getData(), PokerConstants.USERTYPE_CHAT);

        // remove user, close link
        removeUser(link);
        link.close();
    }

    /**
     * Get arraylist of playerinfo
     */
    private DMArrayList<DMTypedHashMap> getPlayerList()
    {
        synchronized (links_)
        {
            DMArrayList<DMTypedHashMap> list = new DMArrayList<DMTypedHashMap>(links_.size());
            for (LinkInfo info : links_)
            {
                list.add(info.player.getData());
            }
            return list;
        }
    }

    /**
     * debug
     */
    private boolean logChat(UDPLink from, UDPData data)
    {
        PokerUDPTransporter msg = new PokerUDPTransporter(data);
        OnlineMessage om = new OnlineMessage(msg.getMessage());
        String chat = om.getChat();
        logger.debug(om.getPlayerName() + " said \"" + chat + '\"');
        if (chat.startsWith("./stats"))
        {
            sendMessage(from, udp_.manager().getStatusHTML(null));
            return true;
        }
        else if (chat.startsWith("./dump"))
        {
            sendMessage(from, "<PRE>" + Utils.getAllStacktraces() + "</PRE>");
            return true;
        }

        return false;
    }

    /**
     * list of links
     */
    private class LinkInfo implements Comparable<LinkInfo>
    {
        UDPLink link;
        String sRealKey;
        OnlinePlayerInfo player;

        private LinkInfo(UDPLink link, String sRealKey, OnlinePlayerInfo player)
        {
            this.link = link;
            this.sRealKey = sRealKey;
            this.player = player;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof LinkInfo)) return false;

            LinkInfo l = (LinkInfo) o;
            return link.getID().equals(l.link.getID());
        }

        @Override
        public int hashCode()
        {
            return link.getID().hashCode();
        }

        @Override
        public String toString()
        {
            return player.getName() + " {" + getTimeConnected() + ' ' + link.getStats() + '}';
        }

        public String getTimeConnected()
        {
            return Utils.getTimeString(link.getTimeConnected(), false);
        }

        public int compareTo(LinkInfo l)
        {
            return player.compareTo(l.player);
        }
    }

    ////
    //// UDPManagerMonitor
    ////

    public void monitorEvent(UDPManagerEvent event)
    {
        UDPLink link = event.getLink();

        switch (event.getType())
        {
            case CREATED:
                //if (TESTING(UDPServer.TESTING_UDP)) logger.debug("Created: "+ Utils.getAddressPort(link.getRemoteIP()));
                link.addMonitor(this);
                //createUser(link) called when HELLO message receved
                break;

            case DESTROYED:
                //if (TESTING(UDPServer.TESTING_UDP)) logger.debug("Destroyed: "+ Utils.getAddressPort(link.getRemoteIP()));
                link.removeMonitor(this);
                removeUser(link);
                break;
        }
    }

    ////
    //// UDPLinkHandler interface
    ////

    public int getTimeout(UDPLink link)
    {
        return UDPLink.DEFAULT_TIMEOUT;
    }

    public int getPossibleTimeoutNotificationInterval(UDPLink link)
    {
        return getTimeout(link);
    }

    public int getPossibleTimeoutNotificationStart(UDPLink link)
    {
        return getTimeout(link);
    }
}
