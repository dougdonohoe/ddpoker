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
/*
 * OnlineManager.java
 *
 * Created on December 2, 2004, 10:19 AM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.p2p.*;
import com.donohoedigital.server.*;
import com.donohoedigital.udp.*;
import org.apache.log4j.*;

import javax.swing.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;

import static com.donohoedigital.config.DebugConfig.*;

/**
 * @author donohoe
 */
public class OnlineManager implements ChatManager
{
    static Logger logger = Logger.getLogger(OnlineManager.class);

    static boolean DEBUG = false; // msgs sent/rcvd

    static int SCNT = 0;
    static int RCNT = 0;

    // members
    private GameContext context_;
    private PokerGame game_;
    private PokerMain main_;
    PokerConnectionServer p2p_;

    boolean bHost_;

    // chat
    private ChatHandler chat_ = null;
    private final List<OnlineMessage> chatQueue_ = new ArrayList<OnlineMessage>();

    // tournament director
    private TournamentDirector td_;
    private final List<OnlineMessage> tdQueue_ = new ArrayList<OnlineMessage>();
    private OnlineManagerQueue oQueue_ = null;
    private PokerPrefsPlayerList banned_;
    private Set<String> sentMessageAboutRejectedPlayer = new HashSet<String>();

    /**
     * Creates a new instance of OnlineManager
     */
    public OnlineManager(PokerGame game)
    {
        //logger.debug("Starting online manager");
        game_ = game;
        context_ = game_.getGameContext();
        main_ = PokerMain.getPokerMain();
        p2p_ = main_.getPokerConnectionServer(game.isUDP());

        // note that we don't store the local player since that can get
        // reloaded with game updates.  However, the "host" status will
        // not change from inception (TODO: if we allow host migration,
        // this will obviously change)
        bHost_ = getLocalPlayer().isHost();
        if (bHost_) banned_ = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_BANNED);

        // online queue for sending messages (TCP only)
        if (!isUDP())
        {
            oQueue_ = new OnlineManagerQueue(this);
        }
    }

    /**
     * done
     */
    public void finish()
    {
        //logger.debug("Shutting down online manager");
        if (oQueue_ != null) oQueue_.finish();
        main_.shutdownPokerConnectionServer(p2p_);
        game_ = null;
        main_ = null;
        p2p_ = null;
    }

    /**
     * Get p2p server
     */
    PokerConnectionServer getP2P()
    {
        return p2p_;
    }

    /**
     * is this a UDP based connection?
     */
    public boolean isUDP()
    {
        return p2p_ instanceof UDPServer;
    }

    // listener for host connection
    private HostConnectionListener hostConnection_;

    /**
     * host listener
     */
    public void setHostConnectionListener(HostConnectionListener listener)
    {
        hostConnection_ = listener;
    }

    /**
     * get game
     */
    public PokerGame getGame()
    {
        return game_;
    }

    /**
     * Socket closed
     */
    public synchronized void connectionClosing(PokerConnection connection)
    {
        PokerPlayer player = game_.getPokerPlayerFromConnection(connection);
        if (player != null)
        {
            // log socket closing
            if (game_.getOnlineMode() != PokerGame.MODE_CANCELLED)
            {
                logger.info("Connection closed for " +
                            player.getName() + " [id #" + player.getID() + "] " +
                            (player.isHost() ? " (host)" : "") +
                            (player.isObserver() ? " (observer)" : "") +
                            ": " +
                            connection);
            }

            // set socket null
            player.setConnection(null);

            // no more processing needed if cancelled
            if (game_.getOnlineMode() == PokerGame.MODE_CANCELLED)
            {
                return;
            }

            // on client, handle lost connection to host
            if (player.isHost())
            {
                if (hostConnection_ != null)
                {
                    if (oQueue_ != null) oQueue_.clearQueue();
                    hostConnection_.hostConnectionLost();
                }
            }
            else
            {
                // remove observer on socket close, player if game not started
                if (player.isObserver() || (!player.isObserver() && game_.getOnlineMode() != PokerGame.MODE_PLAY))
                {
                    OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_QUIT);
                    omsg.setFromPlayerID(player.getID());
                    processQuit(omsg);
                }
                // player disconnected, notify other players
                else
                {
                    connectionStatusChanged(player, false);
                }
            }
        }
    }

    /**
     * Handle message
     */
    public synchronized DDMessageTransporter handleMessage(DDMessageTransporter msg, PokerConnection channel)
    {
        OnlineMessage omsg = new OnlineMessage(msg.getMessage(), channel); // received
        final DDMessageTransporter reply;

        if (TESTING(EngineConstants.TESTING_P2P)) logger.debug("** RECEIVED **: " + msg);

        try
        {
            reply = processMessage(omsg);
        }
        catch (OnlineError error)
        {
            if (TESTING(EngineConstants.TESTING_P2P)) logger.debug("** ERROR REPLY **: " + error.getReply());
            return error.getReply();
        }

        // debug
        if (TESTING(EngineConstants.TESTING_P2P)) logger.debug("** REPLY **: " + reply);

        // no exception, so notify listeners
        fireAction(omsg);

        return reply;
    }

    /**
     * Process message.  Things that can happen:
     * <p/>
     * 1) An error occurs - we respond back with a direct message by
     * throwing an OnlineError
     * <p/>
     * 2) A direct response is desired, like for the CAT_TEST, in which
     * case that is returned directly
     * <p/>
     * 3) No resonse is required.  In some cases, the message is just processed
     * and in other cases a seperate message is sent asynhcronously back
     * to the sender
     */
    private DDMessageTransporter processMessage(OnlineMessage omsg)
    {
        DDMessageTransporter reply = null;

        // validate gameid/password in message
        validate(omsg);

        // record time message received
        PokerPlayer from = game_.getPokerPlayerFromConnection(omsg.getConnection());
        if (from != null)
        {
            from.setMessageReceived();
        }

        // debug
        if (DEBUG)
        {
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            RCNT++;
            if (DEBUG || omsg.getCategory() != OnlineMessage.CAT_CHAT)
            {
                logger.debug(RCNT + " received from " + (from == null ? "[unknown]" : from.getName()) +
                             ": " + omsg.toStringCategorySize());
            }
        }

        // process message
        switch (omsg.getCategory())
        {
            case OnlineMessage.CAT_TEST:
                reply = getTestReply(p2p_, main_.getGUID(), omsg);
                break;

            case OnlineMessage.CAT_ALIVE:
                // do nothing specific for alive messages (above setMessageReceived() call is all we need)
                break;

            case OnlineMessage.CAT_JOIN:
                processJoin(omsg);
                break;

            case OnlineMessage.CAT_CLIENT_JOIN:
                processClientJoin(omsg);
                break;

            case OnlineMessage.CAT_READY:
                processReady(omsg);
                break;

            case OnlineMessage.CAT_QUIT:
                processQuit(omsg);
                break;

            case OnlineMessage.CAT_CONNECTION:
                processConnection(omsg);
                break;

            case OnlineMessage.CAT_CANCEL:
                processCancel(omsg);
                break;

            case OnlineMessage.CAT_CANCEL_ACTION:
                processCancelAction();
                break;

            case OnlineMessage.CAT_GAME_UPDATE:
                processGameUpdate(omsg);
                break;

            case OnlineMessage.CAT_CHAT:
                processChat(omsg);
                break;

            case OnlineMessage.CAT_PROCESS_PHASE:
                processPhase(omsg);
                break;

            case OnlineMessage.CAT_REMOVE_WAIT_LIST:
                processRemoveFromWaitList(omsg);
                break;

            case OnlineMessage.CAT_HAND_ACTION:
                processHandAction(omsg);
                break;

            case OnlineMessage.CAT_ADDON:
                processAddon(omsg);
                break;

            case OnlineMessage.CAT_REBUY:
                processRebuy(omsg);
                break;

            case OnlineMessage.CAT_PLAYER_UPDATE:
                processPlayerUpdate(omsg);
                break;

            case OnlineMessage.CAT_CHANGE_TABLE:
                processChangeTable(omsg);
                break;

            case DDMessage.CAT_APPL_ERROR:
                logger.warn("OnlineManager app error: " + omsg.getApplicationErrorMessage());
                break;

            case DDMessage.CAT_ERROR:
                logger.warn("OnlineManager error: " + omsg.toStringNoData());
                break;

            default:
                throw new OnlineError(getAppErrorReply(p2p_, omsg,
                                                       PropertyConfig.getMessage("msg.p2p.unhandled",
                                                                                 omsg.getCategory()), false));
        }

        return reply;
    }

    /**
     * validate message (id, password, etc.)
     */
    private void validate(OnlineMessage omsg)
    {
        // no need to validate test or error messages
        if (omsg.getCategory() == OnlineMessage.CAT_TEST ||
            omsg.getCategory() == DDMessage.CAT_APPL_ERROR ||
            omsg.getCategory() == DDMessage.CAT_ERROR) return;

        String gid = game_.getOnlineGameID();
        String gpass = game_.getOnlinePassword();
        String mid = omsg.getGameID();
        String mpass = omsg.getPassword();

        if (!gpass.equals(mpass) || !gid.equals(mid))
        {
            String data = "Validation failed gid=" + gid + " gpass=" + gpass + " msg-gid=" + mid + " msg-gpass=" + mpass;
            logger.info(data + " omsg: " + omsg.toStringNoData());
            DDMessageTransporter reply = getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.p2p.validate"), false);
            reply.getMessage().setString("x-validation-data", data);
            throw new OnlineError(reply);
        }
    }

    /**
     * Join game - host side processing.  Checks for dups, makes sure
     * game is accepting registrations. If valid, creates player, sends
     * game data to new player and update to other players
     */
    private void processJoin(OnlineMessage omsg)
    {
        int nMode = game_.getOnlineMode();
        boolean bObs = omsg.isObserve();

        // check version compat - guest has to be same or later version
        Version msgVersion = omsg.getData().getVersion();
        if (msgVersion.isBefore(PokerConstants.VERSION_LAST_COMPAT))
        {
            throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.version",
                                                                                         msgVersion.toString(),
                                                                                         PokerConstants.VERSION_LAST_COMPAT.toString()),
                                                   false));
        }

        // see what mode game is in
        switch (nMode)
        {
            case PokerGame.MODE_INIT:
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.init"),
                                                       false));

            case PokerGame.MODE_REG:
            case PokerGame.MODE_PLAY:
                break; // handle below


            default:
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.nomore"),
                                                       false));
        }

        // look for existing player with this key
        boolean rejoin = false;
        String sKey = omsg.getKey();
        PokerPlayer player = game_.getPokerPlayerFromKey(sKey);
        TournamentProfile profile = game_.getProfile();
        PokerConnection conn = omsg.getConnection();
        if (player != null)
        {
            // host can't join again
            if (player.isHost())
            {
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.dup", player.getName()),
                                                       false));
            }

            // reject dup player if current socket isn't null or player is the host (has no socket)
            if (player.getConnection() != null)
            {
                boolean bSameConnect = player.getConnection().equals(conn);

                // if attempting to join in-game with same key, the player was probably disconnected and
                // is rejoining before host has realized connection is dead.  In this case, instead of making
                // things harder for the rare case of someone joining in with a key, we kill the old socket
                // and let this one takes it place.
                // Patch 8 - added isReconnect check which also does forced reconnect when that flag is
                // set (from HostStatus)
                // 2.5 - added reg, same connect logic
                if (nMode == PokerGame.MODE_PLAY || omsg.isReconnect() ||
                    (nMode == PokerGame.MODE_REG && bSameConnect))
                {
                    // if connection is different, close it
                    //
                    // UDP: added equals check, which will allow existing UDP connection to continue to be re-used.
                    // TCP: use of equals check doesn't change functionality since sockets aren't re-used
                    if (!bSameConnect)
                    {
                        logger.info("Player rejoining: " + player.getName() + ", closing existing, likely bad, connection");
                        PokerConnection c = player.getConnection();

                        // synchronize with sends so we don't kill
                        // a message in-progress to the old socket
                        // (let it time out)
                        synchronized (player.getSendSync())
                        {
                            player.setConnection(null);
                            p2p_.closeConnection(c);
                        }
                    }
                    else
                    {
                        player.addDisconnect(); // disconnection perceived on players end
                        logger.info("Player rejoining: " + player.getName() + ", on existing connection");
                    }
                }
                else
                {
                    throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.dup", player.getName()),
                                                           false));
                }
            }

            // In General...
            // if player was eliminated, should now be observing.
            // if player is not eliminated, should still be playing.
            // Let's set the bObs flag to reflect this...

            // Observers that dropped should not be found because
            // they are removed when their connection drops ... and
            // we remove them from save game files upon load.
            // In any case, this shouldn't happen, but print warning if it does.
            // See SAFETY check below.
            if (player.isObserver())
            {
                logger.warn("processJoin() found existing player who is observer: " + player.getName());
                bObs = true; // and in case it does happen, need to set this flag
            }
            else if (player.isEliminated())
            {
                // special case - player was at final table when tournament ended and
                // therefore was not turned into an observer.  That should be the only
                // case of a player re-joining that is eliminated but still is assigned
                // a table.  This is because when a player is eliminated, they are
                // converted to observers ... and when observers disconnect, they
                // are de-listed as observers, so we should not have a re-join
                // from a player at a table who is eliminated.  This is the admittedly
                // rare case of someone getting eliminated, quitting, and re-joining
                // say to chat
                if (game_.isGameOver() && player.getTable() != null)
                {
                    // player was at final table when tournament ended and therefore not turned into an
                    // observer.  So we can keep them as a player at the table.
                }
                else
                {
                    bObs = true;
                }
            }
            // a player rejoining who was waiting should re-join as an observer
            //
            // otherwise if player is still in tournament and is attempting to rejoin as observer,
            // just pretend like they clicked regular Join Game
            else
            {
                bObs = player.isWaiting();
            }

            // make sure observer list has this player (noop if already listed)
            if (bObs)
            {
                game_.addObserver(player);
            }

            logger.info("Player rejoined: " + player.getName() + (bObs ? " [obs]" : ""));
            rejoin = true;
        }
        // new player coming in
        else
        {
            String sPlayerName = omsg.getPlayerName();
            boolean bAllowObsSpecial = sPlayerName.equals("ddpoker") ||
                                       sPlayerName.equals("DDPoker Support") ||
                                       sPlayerName.equals("Greg King") ||
                                       sPlayerName.equals("Doug Donohoe") ||
                                       sPlayerName.equals("Arcade GameOver") ||
                                       sPlayerName.equals("John14");

            // if player is demo, check if profile allows demo players
            if (omsg.isPlayerDemo() && !profile.isAllowDemo())
            {
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.demo"),
                                                       false));
            }

            // check for banned player
            if ((banned_.containsPlayer(sPlayerName) || banned_.containsKey(omsg.getKey())) && !bAllowObsSpecial)
            {
                sendRejectMessage(sPlayerName, "msg.chat.bannedrejected");
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.banned"),
                                                       false));
            }

            // check invite only (players)
            if (!bObs && profile.isInviteOnly() && !profile.getInvitees().containsPlayer(sPlayerName) && !bAllowObsSpecial)
            {
                sendRejectMessage(sPlayerName, "msg.chat.uninvitedrejected");
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage(
                        profile.isInviteObserversPublic() ?
                        "msg.nojoin.inviteonly.obs" : "msg.nojoin.inviteonly"),
                                                       false));
            }

            // check invite only (observers)
            if (bObs && profile.isInviteOnly() && !profile.isInviteObserversPublic() &&
                !profile.getInvitees().containsPlayer(sPlayerName) && !bAllowObsSpecial)
            {
                sendRejectMessage(sPlayerName, "msg.chat.uninvitedrejected");
                throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.inviteonly"),
                                                       false));
            }

            // if player is online activated, make sure this online activated player has
            // not already joined
            if (omsg.isOnlineActivated())
            {
                PokerPlayer exist;
                for (int i = game_.getNumPlayers() - 1; i >= 0; i--)
                {
                    exist = game_.getPokerPlayerAt(i);
                    if (exist.isOnlineActivated() && exist.getName().equals(sPlayerName))
                    {
                        throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.duponline",
                                                                                                     Utils.encodeHTML(exist.getName())),
                                                               false));
                    }
                }

                for (int i = game_.getNumObservers() - 1; i >= 0; i--)
                {
                    exist = game_.getPokerObserverAt(i);
                    if (exist.isOnlineActivated() && exist.getName().equals(sPlayerName))
                    {
                        throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.duponline",
                                                                                                     Utils.encodeHTML(exist.getName())),
                                                               false));
                    }
                }
            }
            else
            {
                // if not online activated, throw error if game requires it
                if (profile.isOnlineActivatedPlayersOnly())
                {
                    sendRejectMessage(sPlayerName, "msg.chat.impostorrejected");
                    throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.onlineactivatedonly"),
                                                           false));
                }
            }

            // player
            if (!bObs)
            {
                ////
                //// NOTE: CHANGES HERE MUST BE REFLECTED IN switchPlayer() below
                ////

                // if no existing player in PLAY mode, then don't allow new join (already playing)
                if (nMode == PokerGame.MODE_PLAY)
                {
                    throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.started"),
                                                           false));
                }

                // check max players
                if (!isSpaceForPlayer())
                {
                    throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.nomore",
                                                                                                 profile.getMaxOnlinePlayers()),
                                                           false));
                }

                // add new player.  ID is simply next integer (current player count)
                player = new PokerPlayer(sKey, game_.getNextPlayerID(), sPlayerName, true);
                player.setDemo(omsg.isPlayerDemo());
                player.setOnlineActivated(omsg.isOnlineActivated());
                game_.addPlayer(player);

                // Send a message requesting that the game be updated
                if (game_.isPublic())
                {
                    OnlineServer manager = OnlineServer.getWanManager();
                    manager.updateGameProfile(game_);
                }
            }
            // observer
            else
            {
                ////
                //// NOTE: CHANGES HERE MUST BE REFLECTED IN switchPlayer() below
                ////

                // check max observers - subtract busted players from the count
                if (!isSpaceForObserver() && !bAllowObsSpecial)
                {
                    throw new OnlineError(getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.nomore.obs",
                                                                                                 profile.getMaxObservers()),
                                                           false));
                }

                player = new PokerPlayer(sKey, PokerConstants.START_OBSERVER_ID + game_.getNextObserverID(),
                                         sPlayerName, true);
                player.setDemo(omsg.isPlayerDemo());
                player.setOnlineActivated(omsg.isOnlineActivated());
                game_.addObserver(player);
            }
        }

        // if rejoining during game notify TD before setting socket on player
        // (td_ can be null when transitioning from lobby to game)
        if (nMode == PokerGame.MODE_PLAY && td_ != null)
        {
            td_.notifyPlayerRejoinStart(player);
            // note: corresponding call to notifyPlayerRejoinDone() in processReady()
        }

        // store socket, path and connect url (for rejoin, set again in case of changes)
        player.setConnection(conn);
        player.setVersion(omsg.getData().getVersion());
        player.setProfilePath(omsg.getPlayerProfilePath()); // store in host copy so when we send back to player, it is correct
        player.setConnectURL(omsg.getConnectURL()); // URL this player uses to connect to host (store for client's use)

        // udp: set name of link to player name
        if (conn.isUDP())
        {
            ((UDPServer) p2p_).manager().getLink(conn.getUDPID()).setName(player.getName());
        }

        // rejoing during play (after registration closed)
        if (nMode == PokerGame.MODE_PLAY)
        {
            // Need to specify table for observer since normal start logic (HostStart) usually
            // assigns tables.  This is a rejoin, so need to specify the table for observers.
            // Also do SAFETY check if player already has table and not host, reset them to host table.
            PokerTable updateTable = null;
            if (bObs)
            {
                PokerTable hostTable = game_.getHost().getTable();
                if (player.getTable() != null)
                {
                    if (player.getTable() != hostTable)
                    {
                        player.getTable().removeObserver(player);
                        hostTable.addObserver(player);
                        updateTable = hostTable;
                    }
                }
                else
                {
                    hostTable.addObserver(player);
                    updateTable = hostTable;
                }
            }

            // in game (td set) send game data, tables, hide cards and set current table to this player
            if (td_ != null)
            {
                sendClientJoinMessage(player, HostStart.PHASE_CLIENT_INIT, omsg, true);
            }
            // (re)join in lobby after countdown started
            else
            {
                sendClientJoinMessage(player, "Lobby.Player", omsg, false);
            }

            // observer - send new player to other players so they are in list
            if (bObs)
            {
                // if updated host table, update that table so new observer
                // appears in those player's observer list
                if (updateTable != null)
                {
                    sendTableUpdate(updateTable, null, false, false);
                }

                // add observer to all players
                sendPlayerUpdateToAll(player);

                // notify players
                sendDealerChat(PokerConstants.CHAT_1,
                               PropertyConfig.getMessage(
                                       rejoin ? "msg.chat.observerrejoined" : "msg.chat.observerjoined",
                                       Utils.encodeHTML(player.getName())),
                               player.getTable());
            }

            // mark connection change for all players
            if (!bObs || player.isWaiting())
            {
                connectionStatusChanged(player, true);
            }
        }
        // join/rejoin in lobby
        else
        {
            // send game data to new or rejoined player
            sendClientJoinMessage(player, "Lobby.Player", omsg, false);

            // notify other registered players of new player
            if (!rejoin)
            {
                sendPlayerUpdateToAll(player);
                sendDirectorChat(PropertyConfig.getMessage(bObs ? "msg.chat.observerjoined" : "msg.chat.playerjoined",
                                                           Utils.encodeHTML(player.getName())), null);


                // if UDP send note about beta status // TODO: remote this when finalized
                if (conn.isUDP())
                {
                    sendDirectorChat(player, "NOTE:  You are joining a game that uses the new UDP message " +
                                             "infrastructure.  This is a 'beta' feature, meaning there could " +
                                             "be small bugs or issues yet to resolve.");
                }

                // send greeting
                String sGreeting = profile.getGreeting(Utils.encodeHTML(player.getName()));
                if (sGreeting != null)
                {
                    sendChatFromHost(player, sGreeting);
                }
            }
        }
    }

    /**
     * Only post a reject message per messageKey once about a particular player to prevent griefing
     */
    private void sendRejectMessage(String sPlayerName, String messageKey)
    {
        String key = sPlayerName + messageKey;
        if (!sentMessageAboutRejectedPlayer.contains(key))
        {
            sendDirectorChat(PropertyConfig.getMessage(messageKey, Utils.encodeHTML(sPlayerName)), null);
            sentMessageAboutRejectedPlayer.add(key);
        }
    }

    /**
     * see if we can add a player
     */
    public synchronized boolean isSpaceForPlayer()
    {
        TournamentProfile profile = game_.getProfile();
        int nMax = profile.getMaxOnlinePlayers();
        return (game_.getNumPlayers() < nMax);
    }


    /**
     * see if we can add an observer
     */
    public synchronized boolean isSpaceForObserver()
    {
        TournamentProfile profile = game_.getProfile();
        int nMax = profile.getMaxObservers();
        return (game_.getNumObserversNonPlayers() < nMax);
    }

    /**
     * switch player from observer to player (or vice versa) - used from lobby only
     */
    public synchronized void switchPlayer(PokerPlayer player)
    {
        // Note: no need to change id since player/observer ids are in different 'namespaces'
        if (player.isObserver())
        {
            game_.removeObserver(player);
            game_.addPlayer(player);
        }
        else
        {
            game_.removePlayer(player);
            game_.addObserver(player);
        }

        // update players
        sendPlayersObserverListToAll();
        sendDirectorChat(PropertyConfig.getMessage(player.isObserver() ? "msg.chat.switchtoobserver" : "msg.chat.switchtoplayer",
                                                   Utils.encodeHTML(player.getName())), null);

        // Send a message requesting that the game be updated
        if (game_.isPublic())
        {
            OnlineServer manager = OnlineServer.getWanManager();
            manager.updateGameProfile(game_);
        }
    }

    /**
     * send game data to client joining
     */
    private void sendClientJoinMessage(PokerPlayer player, String sPhase, OnlineMessage omsg, boolean bSetCurrentTable)
    {
        OnlineMessage reply = prepareMessage(OnlineMessage.CAT_CLIENT_JOIN);
        SaveDetails details = game_.getSaveDetails(SaveDetails.SAVE_ALL);
        details.setSaveAI(SaveDetails.SAVE_NONE);
        PokerSaveDetails pdetails = (PokerSaveDetails) details.getCustomInfo();
        details.setSaveCurrentPhase(SaveDetails.SAVE_NONE);
        if (bSetCurrentTable) pdetails.setSetCurrentTableToLocal(true);
        pdetails.setHideOthersCards(true);
        pdetails.setPlayerID(player.getID());
        reply.setInReplyTo(omsg.getMessageID());
        reply.setPhaseName(sPhase);
        storeGame(reply, game_, details);
        sendMessage(reply, player);
        player.clearMessageReceived(); // clear message received time when sending client join message
    }

    /**
     * Called after client does the PHASE_CLIENT_INIT and creates
     * the TournamentDirector
     */
    private void processReady(OnlineMessage omsg)
    {
        // td could be null when this is called in response to
        // the normal HostStart, in which case it is safe to
        // ignore since notifyPlayerRejoinStart() is only called
        // during rejoin
        if (td_ != null) td_.notifyPlayerRejoinDone(getPlayer(omsg, true));
    }

    /**
     * process player connection status change (host and client)
     */
    private void processConnection(OnlineMessage omsg)
    {
        // get player by id
        PokerPlayer player = getPlayer(omsg, false);
        if (player == null) return;

        if (isHost())
        {
            // notify players at table that player is disconnected
            sendMessageTable(omsg, player.getTable(), player);

            // send chat to player's table
            sendDealerChat(PokerConstants.CHAT_1,
                           PropertyConfig.getMessage(omsg.isPlayerConnected() ?
                                                     "msg.chat.playerrejoined" : "msg.chat.disconnected",
                                                     Utils.encodeHTML(player.getName())),
                           player.getTable());
        }
        else
        {
            // on client, we need to explicitly set disconnected flag since
            // we don't get a game update when receiving this message
            player.setDisconnected(!omsg.isPlayerConnected());
        }

        // update table (run on host and clients)
        PokerUtils.setConnectionStatus(context_, player, false);
    }

    /**
     * process player quit (host and client)
     */
    private void processQuit(OnlineMessage omsg)
    {
        // get player by id
        PokerPlayer player = getPlayer(omsg, false);
        if (player == null) return;

        // get table before removing player/observer
        // could be null if processing quit in lobby
        PokerTable table = player.getTable();

        // null socket so we don't send them more messages
        // this sets disconnected flag on client as well
        // close connection as well
        PokerConnection c = player.getConnection();
        player.setConnection(null); // null first so we don't do connectionClosing logic and get in endless loop
        p2p_.closeConnection(c);

        // This logic run on client and server
        boolean bObs = player.isObserver();
        if (bObs)
        {
            game_.removeObserver(player);
        }
        else
        {
            // registration mode, just remove player
            // we check game in progress since online mode on client
            // is always "MODE_CLIENT"
            if (!game_.isInProgress())
            {
                game_.removePlayer(player);

                // Send a message requesting that the game be updated
                // Fixed this in 3.0 db rewrite (add isHost() check)
                if (game_.isPublic() && isHost())
                {
                    OnlineServer manager = OnlineServer.getWanManager();
                    manager.updateGameProfile(game_);
                }
            }
            // in-progress game, player is sitting out
            else
            {
                // if not currently sitting out, mark as so
                if (!player.isSittingOut())
                {
                    player.setSittingOut(true);
                }
                // already sitting out, but need to fire settings
                // changed so placard updates properly
                else
                {
                    player.fireSettingsChanged();
                }
            }
        }

        // HOST: send chat info, notify other players as necessary
        if (isHost())
        {
            if (game_.getOnlineMode() != PokerGame.MODE_PLAY) // same as !game_.isInProgress()
            {
                // notify other registered players of quiting player
                sendMessageAll(omsg);

                // send director chat
                sendDirectorChat(PropertyConfig.getMessage(bObs ? "msg.chat.observerquit" : "msg.chat.playerquit",
                                                           Utils.encodeHTML(player.getName())), null);
            }
            else
            {
                if (bObs)
                {
                    // notify all players if observer quit so their observer list is updated
                    sendMessageAll(omsg);
                }
                else
                {
                    // notify players at table that player is sitting out
                    if (table != null) sendMessageTable(omsg, table, player);


                }

                // check to see if all players are now disconnected -
                // do for both playes and waiting observers
                if (player.isWaiting() || !bObs)
                {
                    checkAllConnected();
                }

                // send dealer chat
                if (table != null)
                {
                    sendDealerChat(PokerConstants.CHAT_1, PropertyConfig.getMessage(bObs ? "msg.chat.observerquit" :
                                                                                    (game_.isGameOver() ? "msg.chat.playerquit" : "msg.chat.playerquit2"),
                                                                                    Utils.encodeHTML(player.getName())), table);
                }
            }
        }
    }

    /**
     * process cancel (client only)
     */
    private void processCancel(OnlineMessage omsg)
    {
        game_.setOnlineMode(PokerGame.MODE_CANCELLED); // set cancelled (checked in HostStatus and socketClosing())
        final boolean bBan = omsg.getFromPlayerID() != OnlineMessage.NO_PLAYER;

        // need to update from Swing thread
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        String sKey = bBan ? "msg.cancel.client.ban" :
                                      game_.isInProgress() ? "msg.cancel.client.play" :
                                      "msg.cancel.client.reg";

                        PokerPlayer host = getHost();
                        EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage(sKey,
                                                                                                 Utils.encodeHTML(host.getName())));
                        context_.restart();
                    }
                }
        );
    }

    /**
     * process cancel action
     */
    private void processCancelAction()
    {
        EngineUtils.cancelCancelables();
    }

    ////
    //// game update
    ////

    /**
     * send player/observer list to all
     */
    private void sendPlayersObserverListToAll()
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_GAME_UPDATE);
        SaveDetails details = game_.getSaveDetails(SaveDetails.SAVE_NONE);
        details.setSavePlayers(SaveDetails.SAVE_ALL);
        details.setSaveObservers(SaveDetails.SAVE_ALL);
        storeGame(omsg, game_, details);

        sendMessageAll(omsg);
    }

    /**
     * send game update to all players except given player
     */
    private void sendPlayerUpdateToAll(PokerPlayer player)
    {
        player.setDirty(true);
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_GAME_UPDATE);
        SaveDetails details = game_.getSaveDetails(SaveDetails.SAVE_NONE);
        details.setSavePlayers(SaveDetails.SAVE_DIRTY);
        details.setSaveObservers(SaveDetails.SAVE_DIRTY);
        storeGame(omsg, game_, details);
        player.setDirty(false);

        sendMessageAllExcept(omsg, player, false);
    }

    /**
     * send game update to all players of all dirty players.  Caller is responsible
     * for calling setDirty(true/false) as appropriate.  If bIncludeTables is set
     * to true, then the poker table data is included.  If bIncludeProfile is true,
     * then the tournament profile is sent again
     */
    public void sendDirtyPlayerUpdateToAll(boolean bIncludeTables, boolean bIncludeProfile)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_GAME_UPDATE);
        SaveDetails details = game_.getSaveDetails(SaveDetails.SAVE_NONE);
        PokerSaveDetails pdetails = (PokerSaveDetails) details.getCustomInfo();
        if (bIncludeProfile) pdetails.setSaveProfileData(SaveDetails.SAVE_ALL);
        details.setSavePlayers(SaveDetails.SAVE_DIRTY);
        details.setSaveObservers(SaveDetails.SAVE_DIRTY);
        details.setSaveGameSubclassData(SaveDetails.SAVE_ALL);
        if (bIncludeTables)
        {
            pdetails.setSaveTables(SaveDetails.SAVE_ALL);
            pdetails.setSetCurrentTableToLocal(true);
        }
        storeGame(omsg, game_, details);

        sendMessageAll(omsg);
    }

    /**
     * Send update of table to all players at table (and observers).
     */
    public void sendTableUpdate(PokerTable table, DMArrayList<PokerTableEvent> events, boolean bOnlySendToWaitList, boolean bRunProcessTable)
    {
        PokerPlayer player;

        if (bOnlySendToWaitList)
        {
            int nNum = table.getWaitSize();
            for (int i = 0; i < nNum; i++)
            {
                player = table.getWaitPlayer(i);
                if (player.isRejoining()) continue;
                sendTableUpdate(table, player, events, PokerSaveDetails.NO_OVERRIDE, bRunProcessTable,
                                null, false, null, null, null);
            }
        }
        else
        {
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                player = table.getPlayer(i);
                if (player == null || player.isComputer() || player.isRejoining()) continue;
                sendTableUpdate(table, player, events, PokerSaveDetails.NO_OVERRIDE, bRunProcessTable,
                                null, false, null, null, null);
            }

            // send in reverse order since error causes observer to get removed
            int nNum = table.getNumObservers();
            for (int i = nNum - 1; i >= 0; i--)
            {
                player = table.getObserver(i);
                if (player.isRejoining()) continue;
                sendTableUpdate(table, player, events, PokerSaveDetails.NO_OVERRIDE, bRunProcessTable,
                                null, false, null, null, null);
            }
        }

    }

    /**
     * Send poker table data to given player.  If dirtyTables and/or dirtyPlayers are non-null,
     * then those specific tables and players are marked dirty and sent.  Otherwise the given
     * table (and its players/observers) are marked dirty and sent.
     */
    public void sendTableUpdate(PokerTable table, PokerPlayer playerTo, DMArrayList<PokerTableEvent> events,
                                int nOverrideState, boolean bRunProcessTable,
                                String sPhase, boolean bSetCurrentTableToLocal,
                                List<PokerTable> dirtyTables,
                                List<PokerPlayer> dirtyPlayers,
                                List<PokerTable> removedTables)
    {
        if (playerTo.isHost()) return;

        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_GAME_UPDATE);

        // mark dirty players on this players table
        if (dirtyTables != null || dirtyPlayers != null)
        {
            setDirty(dirtyTables, dirtyPlayers, true);
        }
        else
        {
            table.setPlayersObserversDirty(true);
            table.setDirty(true);
        }

        // save
        SaveDetails details = game_.getSaveDetails(SaveDetails.SAVE_NONE);
        PokerSaveDetails pdetails = (PokerSaveDetails) details.getCustomInfo();
        details.setSavePlayers(SaveDetails.SAVE_DIRTY);
        details.setSaveObservers(SaveDetails.SAVE_DIRTY);
        details.setSaveGameSubclassData(SaveDetails.SAVE_ALL);
        pdetails.setSaveTables(SaveDetails.SAVE_DIRTY);
        pdetails.setPlayerID(playerTo.getID());
        pdetails.setHideOthersCards(true);
        pdetails.setOverrideState(nOverrideState);
        pdetails.setSetCurrentTableToLocal(bSetCurrentTableToLocal);
        pdetails.setRemovedTables(getTableIds(removedTables));
        storeGame(omsg, game_, details);

        // unset dirty flag
        // mark dirty players on this players table
        if (dirtyTables != null || dirtyPlayers != null)
        {
            setDirty(dirtyTables, dirtyPlayers, false);
        }
        else
        {
            table.setPlayersObserversDirty(false);
            table.setDirty(false);
        }

        // set to run process table and events
        omsg.setRunProcessTable(bRunProcessTable);
        omsg.setPokerTableEvents(events);
        omsg.setPhaseName(sPhase);

        // send
        sendMessage(omsg, playerTo);
    }

    /**
     * Get int array of ids for tables in this list.  Return null if
     * null or empty.
     */
    private int[] getTableIds(List<PokerTable> tables)
    {
        if (tables == null || tables.isEmpty()) return null;

        int removed[] = new int[tables.size()];
        for (int i = 0; i < removed.length; i++)
        {
            removed[i] = tables.get(i).getNumber();
        }
        return removed;
    }

    /**
     * set tables in list dirty along with players/observers therein
     */
    private void setDirty(List<PokerTable> tables, List<PokerPlayer> players, boolean bDirty)
    {
        if (tables != null)
        {
            for (PokerTable table : tables)
            {
                table.setDirty(bDirty);
            }
        }

        if (players != null)
        {
            for (PokerPlayer player : players)
            {
                player.setDirty(bDirty);
            }
        }
    }

    ////
    //// client-side join
    ////

    /**
     * Send message to host to join game - assumes the 'parent' PokerGame has
     * been created with a temporary player setup with a connection
     * URL to the host.  Return Boolean.TRUE if join successful.  If successful,
     * the next phase will be run from here.  If failed, return error EngineMessage
     * with details of problem.
     */
    public Object joinGame(boolean bObserve, boolean bReconnect, boolean bHeadless)
    {
        PokerPlayer local = getLocalPlayer();
        OnlineMessage msg = prepareMessage(OnlineMessage.CAT_JOIN);
        msg.setReconnect(bReconnect);
        msg.setPlayerName(local.getName());
        msg.setPlayerDemo(local.isDemo());
        msg.setOnlineActivated(local.getProfile().isActivated());
        msg.setPlayerProfilePath(local.getProfilePath());
        msg.setObserve(bObserve);

        boolean bOK = false;
        OnlineMessage reply = null;
        DDMessage error = null;

        Peer2PeerMessenger msgr = null;
        if (bHeadless)
        {
            if (isUDP())
            {
                PokerConnect conn = new PokerConnect((UDPServer) getP2P(), msg.getConnectURL(), null);
                bOK = conn.connect(msg);

                if (!bOK)
                {
                    error = conn.getError();
                    return error;
                }

                reply = conn.getReply();
            }
            else
            {
                PokerP2PHeadless head = new PokerP2PHeadless(context_, msg);
                head.send();
                msgr = head.getPeer2PeerMessenger();
                bOK = head.getStatus() == DDMessageListener.STATUS_OK;
                error = head.getErrorMessage();
                reply = head.getReply();
            }
        }
        else
        {
            TypedHashMap params = new TypedHashMap();
            params.setString(PokerP2PDialog.PARAM_DISPLAY, PropertyConfig.getMessage("msg.joingame", msg.getConnectURL()));
            params.setObject(PokerP2PDialog.PARAM_MSG, msg);
            params.setBoolean(PokerP2PDialog.PARAM_FACELESS, bReconnect ? Boolean.TRUE : Boolean.FALSE);
            params.setBoolean(PokerP2PDialog.PARAM_FACELESS_ERROR, bReconnect ? Boolean.FALSE : Boolean.TRUE);

            if (isUDP())
            {
                PokerUDPDialog dialog = (PokerUDPDialog) context_.processPhaseNow("ConnectGameUDP", params);
                bOK = dialog.getStatus() == DDMessageListener.STATUS_OK;
                error = dialog.getErrorMessage();
                reply = dialog.getReply();
            }
            else
            {
                PokerP2PDialog dialog = (PokerP2PDialog) context_.processPhaseNow("ConnectGameTCP", params);
                msgr = dialog.getPeer2PeerMessenger();
                bOK = dialog.getStatus() == DDMessageListener.STATUS_OK;
                error = dialog.getErrorMessage();
                reply = dialog.getReply();
            }
        }

        // if not okay, close client and return error
        if (!bOK)
        {
            if (!isUDP() && msgr != null)
            {
                try
                {
                    msgr.getPeer2PeerClient().close();
                }
                catch (IOException ignore)
                {
                }
            }

            return error;
        }

        // run phase in normal join, not in rejoin, except
        // case where rejoin from lobby, but host in game
        if (!bHeadless)
        {
            boolean bRunPhase = !bReconnect;
            String sPhase = reply.getPhaseName();
            Phase current = context_.getCurrentUIPhase();
            String sCurrentName = current.getGamePhase().getName();
            if (bReconnect && !sCurrentName.equals(sPhase) && sCurrentName.startsWith("Lobby"))
            {
                bRunPhase = true;
            }
            if (bRunPhase)
            {
                // process phase returned in message now
                // instead of at end of processClientJoin() so
                // that the visual timing is the dialog
                // goes away first, then the phase is run
                processPhase(reply);
            }
        }
        return Boolean.TRUE;
    }

    /**
     * CLIENT quit: send quit game message to host
     */
    public void quitGame()
    {
        ApplicationError.assertTrue(!isHost(), "quitGame() only supported from client");
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_QUIT);
        omsg.setFromPlayerID(getLocalPlayer().getID());
        game_.setOnlineMode(PokerGame.MODE_CANCELLED); // set cancelled (checked in HostStatus)
        sendMessage(omsg, getHost(), true); // notify host
        p2p_.closeConnection(getHost().getConnection()); // close socket
    }

    /**
     * HOST cancel: send cancel game message to all clients and clean up
     */
    public void cancelGame()
    {
        ApplicationError.assertTrue(isHost(), "cancelGame() only supported from host");
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_CANCEL);
        sendMessageAll(omsg, true); // notify clients

        // remove save file if reg mode, save if play mode
        if (game_.getOnlineMode() == PokerGame.MODE_PLAY)
        {
            if (td_ != null)
            {
                td_.saveGame("cancel");
            }
            else
            {
                game_.saveWriteGame();
            }
        }
        else
        {
            game_.getLastGameState().getFile().delete();
        }

        // close all sockets
        game_.setOnlineMode(PokerGame.MODE_CANCELLED); // set cancelled (checked in socketClosing())
        PokerPlayer p;
        int nNum = game_.getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = game_.getPokerPlayerAt(i);
            p2p_.closeConnection(p.getConnection());
        }

        // and observers too
        nNum = game_.getNumObservers();
        for (int i = nNum - 1; i >= 0; i--)
        {
            p = game_.getPokerObserverAt(i);
            p2p_.closeConnection(p.getConnection());
        }
    }

    /**
     * HOST ban: game message to banned player
     */
    public void banPlayer(PokerPlayer player)
    {
        ApplicationError.assertTrue(isHost(), "banPlayer() only supported from host");

        // notify player
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_CANCEL);
        omsg.setFromPlayerID(player.getID()); // used on receiving end to display ban message
        sendMessage(omsg, player, true);

        // process quit by this player
        omsg = prepareMessage(OnlineMessage.CAT_QUIT);
        omsg.setFromPlayerID(player.getID());
        processQuit(omsg);
    }

    // alive sleep/timeout values
    public static final int ALIVE_SLEEP_MILLIS = 5000;
    public static final int ALIVE_TIMEOUT_MILLIS = 15500;

    /**
     * send alive message (called from TD/Lobby)
     */
    public void alive(boolean bInGame)
    {
        // no need to do alive check with UDP since the infrastructure handles that
        ApplicationError.assertTrue(!isUDP(), "Don't call alive when UDP in use");

        if (bHost_)
        {
            OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_ALIVE);
            sendMessageAll(omsg);

            // player check
            PokerPlayer p;
            int nNum = game_.getNumPlayers();
            for (int i = 0; i < nNum; i++)
            {
                p = game_.getPokerPlayerAt(i);
                // skip host, computer players (for obvious reasons)
                // skip observers who were players (sent below).
                if (p.isHost() || p.isComputer() || p.isObserver()) continue;

                checkAlive(p);
            }

            // observer check
            nNum = game_.getNumObservers();
            for (int i = nNum - 1; i >= 0; i--)
            {
                p = game_.getPokerObserverAt(i);
                if (p.isHost()) continue;

                checkAlive(p);
            }
        }
        else
        {
            PokerPlayer host = getHost();
            if (host.getConnection() != null)
            {
                OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_ALIVE);
                sendMessage(omsg, host);

                // only do alive check if host version is after alive check added
                if (!host.getVersion().isBefore(bInGame ? PokerConstants.VERSION_ALIVE_CHECK_ADDED :
                                                PokerConstants.VERSION_ALIVE_LOBBY_ADDED))
                {
                    checkAlive(host);
                }
            }
        }
    }

    /**
     * check alive
     */
    private void checkAlive(PokerPlayer player)
    {
        long last = player.getLastMessageReceivedMillis();
        long timeSinceLastMsg = System.currentTimeMillis() - last;

        //logger.debug("Check alive: " + player.getName() + " last: " + last);

        // if its been the designated time since a message was received
        // from this player, then close their socket.  Note:  we don't
        // do this if no message received from player ... which avoids
        // timeouts when transitioning to poker table from lobby or
        // upon re-join (TD clears last timeout in start()).
        if (timeSinceLastMsg > ALIVE_TIMEOUT_MILLIS && last != 0 && player.getConnection() != null)
        {
            logger.info("Alive timeout for " + player.getName() + ", forcing socket closed.");
            connectionClosing(player.getConnection());
        }
    }

    /**
     * send message regarding connection
     */
    private void connectionStatusChanged(PokerPlayer player, boolean bConnected)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_CONNECTION);
        omsg.setFromPlayerID(player.getID());
        omsg.setPlayerConnected(bConnected);
        processConnection(omsg);

        checkAllConnected();
    }

    /**
     * check to see whether all clients that should be connected are
     * connected.
     */
    void checkAllConnected()
    {
        boolean bPauseAllDisconnected = PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_PAUSE_ALL_DISCONNECTED, true);
        int nClients = 0;
        int nDisconnected = 0;

        // only do loop if we are pausing when all disconnected
        if (bPauseAllDisconnected)
        {
            PokerPlayer player;
            int nNum = game_.getNumPlayers();
            for (int i = 0; i < nNum; i++)
            {
                player = game_.getPokerPlayerAt(i);
                if (player.isHost() || player.isComputer() || player.isEliminated()) continue;
                nClients++;
                if (player.isDisconnected()) nDisconnected++;
            }
        }

        if (bPauseAllDisconnected && nClients == nDisconnected && nClients != 0)
        {
            TypedHashMap params = new TypedHashMap();
            params.setString(HostPauseDialog.PARAM_MSG_KEY, "msg.host.paused.disconnected");
            params.setBoolean(HostPauseDialog.PARAM_AUTO_CLOSE, Boolean.TRUE);
            context_.processPhase("HostPauseDialog", params);
        }
        // always unpause (user could have changed option during pause)
        else
        {
            HostPauseDialog.autoClose();
        }
    }

    /**
     * Received on client from host when join is accepted
     */
    private DDMessageTransporter processClientJoin(OnlineMessage omsg)
    {
        // load game from data (this replaces temp player)
        loadGame(omsg, game_);

        // get the host
        PokerPlayer host = getHost();

        // remember host socket
        PokerConnection conn = omsg.getConnection();
        host.setConnection(conn);

        // udp: set name of link to player name
        if (conn.isUDP())
        {
            ((UDPServer) p2p_).manager().getLink(conn.getUDPID()).setName(host.getName());
        }

        // remember host version
        host.setVersion(omsg.getData().getVersion());

        // if reconnect, this sends ready message and causes resync
        // on initial connect, does nothing (waits for setTournamentDirector to be set)
        sendReadyMessage(true);

        return null;
    }

    ////
    //// TournamentDirector related methods
    ////

    /**
     * Set the TournamentDirector in use.  If game updates
     * have queued up, process them immediately.
     */
    public void setTournamentDirector(TournamentDirector td)
    {
        synchronized (tdQueue_)
        {
            if (td != null)
                ApplicationError.assertTrue(td_ == null, "Assigning TournamentDirector and already one set");
            td_ = td;

            // if we are the host and now in play mode, adjust number of worker threads based
            // on actual players and observers.  (TCP only - UDP doesn't use threads)
            if (bHost_ && td != null && !isUDP())
            {
                Peer2PeerServer tcp = (Peer2PeerServer) p2p_;
                boolean bMoreThreads = false;

                // our estimate for the number of desired worker threads on host is approximately 1 per every
                // 2 players/observers plus an extra thread per table
                int nDesiredThreads = ((game_.getNumHumans() + game_.getNumObservers()) / 2) + game_.getNumTables();

                // senders
                int nCurrentThreads = oQueue_.pool_.size();
                if (nDesiredThreads < 3) nDesiredThreads = 3;
                if (nDesiredThreads > nCurrentThreads)
                {
                    oQueue_.pool_.addWorkers(nDesiredThreads - nCurrentThreads);
                    bMoreThreads = true;
                }

                // receivers slightly more
                if (nDesiredThreads < 5) nDesiredThreads = 5;
                nCurrentThreads = tcp.getNumWorkerThreads();
                if (nDesiredThreads > nCurrentThreads)
                {
                    tcp.addWorkers(nDesiredThreads - nCurrentThreads);
                    bMoreThreads = true;
                }

                // let threads get started
                if (bMoreThreads)
                {
                    Utils.sleepMillis(500);
                    if (GameServer.DEBUG_POOL) logger.debug("Done adding threads");
                }
            }

            int nNum = tdQueue_.size();
            if (td_ != null && nNum > 0)
            {
                for (int i = 0; i < nNum; i++)
                {
                    _processGameUpdate(tdQueue_.get(i));
                }
                tdQueue_.clear();
            }

            // our TD is set, notify that we are ready
            if (td != null) sendReadyMessage(false);
        }
    }

    /**
     * if client, sends ready message to host
     */
    private void sendReadyMessage(boolean bSyncImmediate)
    {
        if (td_ != null && !bHost_)
        {
            OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_READY);
            omsg.setFromPlayerID(getLocalPlayer().getID());
            sendMessage(omsg, getHost());
            if (bSyncImmediate)
            {
                game_.getCurrentTable().prefsChanged();
            }
            else
            {
                bSyncNextUpdate_ = true;
            }
        }
    }

    /**
     * process a game update message, or if TournamentDirector
     * is null, queue it up
     */
    private void processGameUpdate(OnlineMessage omsg)
    {
        // process message if TD isn't null, or if it is, if msg doesn't
        // require TD.  Otherwise, queue message (should only queue at
        // very beginning, when transitioning from join/lobby screens to
        // poker table display)
        synchronized (tdQueue_)
        {
            if (td_ != null || (td_ == null && !omsg.isRunProcessTable()))
            {
                _processGameUpdate(omsg);
            }
            else
            {
                tdQueue_.add(omsg);
            }
        }
    }

    // this flag is set after the TD is ready
    // so that the display is sync'd with whatever
    // game data is in the next update.  This is
    // done becuase the sync done in ShowTournamentTable
    // may happen on stale data due to the time it
    // takes between joining and actually displaying
    // the data.
    private boolean bSyncNextUpdate_ = false;

    /**
     * process a game update message
     */
    @SuppressWarnings({"unchecked"})
    private void _processGameUpdate(OnlineMessage omsg)
    {
        ApplicationError.assertTrue(!isHost(), "should not run processGameUpdate() as host", omsg);

        // load game data
        loadGame(omsg, game_);

        // resync
        if (bSyncNextUpdate_)
        {
            game_.getCurrentTable().prefsChanged();
            bSyncNextUpdate_ = false;
        }

        // if we have events, fire them
        DMArrayList<PokerTableEvent> events = (DMArrayList<PokerTableEvent>) omsg.getPokerTableEvents();
        if (events != null)
        {
            PokerTableEvent event;
            int nNum = events.size();
            for (int i = 0; i < nNum; i++)
            {
                event = events.get(i);
                if (TournamentDirector.DEBUG_EVENT) logger.debug("Firing event: " + event);
                event.getTable().firePokerTableEvent(event);
            }
        }

        // if any phases here, do them
        processPhase(omsg);

        // run process table
        if (omsg.isRunProcessTable())
        {
            td_.processTable(game_.getCurrentTable());
        }
    }

    /**
     * Send remove from wait list message to host
     */
    public void removeFromWaitList(PokerPlayer player)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_REMOVE_WAIT_LIST);
        omsg.setFromPlayerID(player.getID());
        sendMessage(omsg, getHost());
    }

    /**
     * process wait list message
     */
    public void processRemoveFromWaitList(OnlineMessage omsg)
    {
        PokerPlayer player = getPlayer(omsg, true);
        td_.removeFromWaitList(player);
    }

    /**
     * Send player update message
     */
    public void sendPlayerUpdate(PokerPlayer player, String sSettings)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_PLAYER_UPDATE);
        omsg.setFromPlayerID(player.getID());
        omsg.setPlayerSettings(sSettings);

        // client - send message to host
        if (isHost())
        {
            processPlayerUpdate(omsg);
        }
        else
        {
            sendMessage(omsg, getHost(), true);
        }
    }

    /**
     * process player update
     */
    public void processPlayerUpdate(OnlineMessage omsg)
    {
        PokerPlayer player = getPlayer(omsg, true);
        // could be null when transitioning from lobby to host on client
        if (td_ != null) td_.processPlayerUpdate(player, omsg.getPlayerSettings());

        // if host, propogate to other players
        if (isHost())
        {
            sendMessageTable(omsg, player.getTable(), player);
        }
    }

    /**
     * Send change table message
     */
    public void changeTable(PokerPlayer player, PokerTable table)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_CHANGE_TABLE);
        omsg.setFromPlayerID(player.getID());
        omsg.setTableNumber(table.getNumber());

        sendMessage(omsg, getHost());
    }

    /**
     * process change table
     */
    public void processChangeTable(OnlineMessage omsg)
    {
        PokerPlayer player = getPlayer(omsg, true);
        PokerTable table = game_.getTableByNumber(omsg.getTableNumber());
        td_.changeTable(player, table);
    }

    /**
     * process cancel message
     */
    public void doCancelAction(PokerPlayer player)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_CANCEL_ACTION);

        if (player.isHost())
        {
            processCancelAction();
        }
        else
        {
            sendMessage(omsg, player);
        }
    }

    /**
     * send hand action message to host
     */
    public void doHandAction(HandAction action)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_HAND_ACTION);
        omsg.setHandAction(action);
        sendMessage(omsg, getHost());
    }

    /**
     * send hand action copy to all players at table except host
     */
    public void doHandActionCopy(HandAction action)
    {
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_HAND_ACTION);
        omsg.setHandAction(action);
        omsg.setHandActionCC(true);
        // if forced to fold, update player settings
        PokerPlayer player = action.getPlayer();
        if (action.getAction() == HandAction.ACTION_FOLD && action.getSubAmount() == HandAction.FOLD_FORCED)
        {
            omsg.setPlayerSettings(player.getOnlineSettings());
        }
        PokerTable table = player.getTable();

        // players
        sendMessageTable(omsg, table, null);
    }

    /**
     * process hand action
     */
    public void processHandAction(OnlineMessage omsg)
    {
        HandAction action = (HandAction) omsg.getHandAction();
        ApplicationError.assertNotNull(action, "No hand action in message", omsg);

        // safety check - could be null transitioning from rejoin to lobby
        if (td_ == null) return;

        // if just a copy, just store action (and trigger UI display on clients)
        if (omsg.isHandActionCC())
        {
            td_.storeHandActionCC(action);

            // see if we have player settings to update
            String sSettings = omsg.getPlayerSettings();
            if (sSettings != null)
            {
                td_.processPlayerUpdate(action.getPlayer(), sSettings);
            }
        }
        // otherwise we process action normally
        else
        {
            td_.doHandAction(action, true);
        }
    }

    /**
     * send add on message to host
     */
    public void doAddon(PokerPlayer player, int nCash, int nChips)
    {
        sendCashChips(player, -1, nCash, nChips, false, OnlineMessage.CAT_ADDON);
    }

    /**
     * send rebuy on message to host
     */
    public void doRebuy(PokerPlayer player, int nLevel, int nCash, int nChips, boolean bPending)
    {
        sendCashChips(player, nLevel, nCash, nChips, bPending, OnlineMessage.CAT_REBUY);
    }

    /**
     * conviencence method for addon/rebuy
     */
    private void sendCashChips(PokerPlayer player, int nLevel, int nCash, int nChips, boolean bPending, int nType)
    {
        OnlineMessage omsg = prepareMessage(nType);
        omsg.setFromPlayerID(player.getID());
        omsg.setLevel(nLevel);
        omsg.setCash(nCash);
        omsg.setChips(nChips);
        omsg.setPending(bPending);
        sendMessage(omsg, getHost());
    }

    /**
     * process addon list message
     */
    public void processAddon(OnlineMessage omsg)
    {
        PokerPlayer player = getPlayer(omsg, true);
        td_.doAddon(player, omsg.getCash(), omsg.getChips());
    }

    /**
     * process addon list message
     */
    public void processRebuy(OnlineMessage omsg)
    {
        PokerPlayer player = getPlayer(omsg, true);
        td_.doRebuy(player, omsg.getLevel(), omsg.getCash(), omsg.getChips(), omsg.isPending());
    }

    ////
    //// save/load game utility
    ////

    /**
     * Marshal the game into a string and store it with this message.
     * Only store the game data and the components marked "dirty"
     */
    private void storeGame(OnlineMessage msg, Game game, SaveDetails details)
    {
        GameState state = game.newGameState("OnlineManager");
        game.saveGame(state, details);

        StringWriter writer = new StringWriter();
        state.write(writer);
        msg.setGameData(writer.toString());
    }

    /**
     * Load the game data into given game
     */
    private void loadGame(OnlineMessage msg, Game game)
    {
        GameState state = GameStateFactory.createGameState(msg.getGameData());
        game.loadGame(state, false);
    }

    ////
    //// chat
    ////

    /**
     * Send chat (called from ChatPanel).  If table is specified,
     * message only sent to players at that table.
     */
    public void sendChat(String sMessage, PokerTable table, String sTestData)
    {
        PokerPlayer local = getLocalPlayer();
        OnlineMessage chat = prepareMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(sMessage);
        chat.setFromPlayerID(local.getID());
        if (TESTING(PokerConstants.TESTING_CHAT_PERF) && sTestData != null)
        {
            chat.getData().addData(sTestData);
        }
        if (table != null) chat.setTableNumber(table.getNumber());

        if (local.isHost())
        {
            sendMessageChat(chat, local);
        }
        else
        {
            if (DebugConfig.isTestingOn() && sMessage.startsWith("reconnect test"))
            {
                logger.debug("TESTING 'reconnect test' to host");
                if (isUDP())
                {
                    ((UDPServer) p2p_).manager().getLink(getHost().getConnection().getUDPID()).kill();
                }
                else
                {
                    connectionClosing(getHost().getConnection());
                }
                return;
            }
            sendMessage(chat, getHost());
        }
    }

    /**
     * send chat to given player
     */
    public void sendChat(int nPlayerID, String sMessage)
    {
        PokerPlayer local = getLocalPlayer();
        OnlineMessage chat = prepareMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(sMessage);
        chat.setFromPlayerID(local.getID());
        chat.setChatType(PokerConstants.CHAT_PRIVATE);

        PokerPlayer to = game_.getPokerPlayerFromID(nPlayerID);
        sendMessage(chat, to);
    }

    /**
     * send chat to given player
     */
    private void sendChatFromHost(PokerPlayer to, String sMessage)
    {
        PokerPlayer host = getLocalPlayer();
        OnlineMessage chat = prepareMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(sMessage);
        chat.setFromPlayerID(host.getID());

        if (to.isHost()) deliverChat(chat);
        else sendMessage(chat, to);
    }

    /**
     * send director chat to given player
     */
    private void sendDirectorChat(PokerPlayer to, String sMessage)
    {
        OnlineMessage chat = prepareMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(sMessage);
        chat.setFromPlayerID(OnlineMessage.CHAT_DIRECTOR_MSG_ID);

        sendMessage(chat, to);
    }

    /**
     * Send a director chat message to all
     */
    public void sendDirectorChat(String sMessage, Boolean bPauseClock)
    {
        PokerPlayer local = getLocalPlayer();
        OnlineMessage chat = prepareMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(sMessage);
        chat.setFromPlayerID(OnlineMessage.CHAT_DIRECTOR_MSG_ID);
        chat.setClockPaused(bPauseClock);

        if (local.isHost())
        {
            sendMessageChat(chat, null);
        }
        else
        {
            throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "sendDirectorChat only allowed from host", sMessage);
        }

        // notify local listeners
        deliverChat(chat);
    }

    /**
     * send a dealer chat message to given table
     */
    public void sendDealerChat(int nType, String sMessage, PokerTable table)
    {
        PokerPlayer local = getLocalPlayer();
        OnlineMessage chat = prepareMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(sMessage);
        chat.setChatType(nType);
        chat.setFromPlayerID(OnlineMessage.CHAT_DEALER_MSG_ID);
        chat.setTableNumber(table.getNumber());

        if (local.isHost())
        {
            sendMessageChat(chat, null);
        }
        else
        {
            throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "sendDealerChat only allowed from host", sMessage);
        }

        // notify local listeners
        deliverChat(chat);
    }

    /**
     * process a chat message
     */
    private void processChat(OnlineMessage omsg)
    {
        boolean bDisplayLocally = true;

        // if we are host, propogate to others
        if (isHost())
        {
            // don't propogate messages only to host
            if (omsg.getChatType() != PokerConstants.CHAT_PRIVATE)
            {
                PokerPlayer sender = getPlayer(omsg, true);
                bDisplayLocally = sendMessageChat(omsg, sender);
            }
        }
        // clients, look to see if we should pause the clock -
        // a special case to keep clock in sync with host if
        // host pauses game.  Chat is convienent place to do
        // this since a chat message is always sent when host
        // pauses/unpauses clock.
        else
        {
            Boolean bPauseClock = omsg.isClockPaused();
            if (bPauseClock != null)
            {
                if (bPauseClock)
                {
                    game_.getGameClock().pause();
                }
                else
                {
                    game_.getGameClock().unpause();
                }
            }
        }

        if (bDisplayLocally) deliverChat(omsg);
    }

    /**
     * Do sending of a chat. If a table is specified in the chat message,
     * it should only go to people at that table.  If should display locally,
     * return true.
     */
    private boolean sendMessageChat(OnlineMessage chat, PokerPlayer sender)
    {
        // special commands
        if (chat.getChat().startsWith("./stats") && isUDP())
        {
            String stats = ((UDPServer) p2p_).manager().getStatusHTML(UDPStatus.LINK_COMPARATOR);
            sendChatFromHost(sender, stats);
            return false;
        }

        int nTableNum = chat.getTableNumber();
        if (nTableNum == OnlineMessage.NO_TABLE)
        {
            sendMessageAllExcept(chat, sender, false);
        }
        else
        {
            sendMessageTable(chat, game_.getTableByNumber(nTableNum), sender);
        }

        return true;
    }

    /**
     * deliver chat message to listener
     */
    private void deliverChat(OnlineMessage chat)
    {
        // if chat to specific table, make sure table is current (host only)
        if (isHost())
        {
            int nTableNum = chat.getTableNumber();
            if (nTableNum != OnlineMessage.NO_TABLE)
            {
                PokerTable table = game_.getTableByNumber(nTableNum);
                if (table == null)
                {
                    logger.warn("No table found for table num (" + nTableNum + ") to deliver chat: " + chat.getChat() + ", from-id: " +
                                chat.getFromPlayerID() + ", chat-type: " + chat.getChatType());
                    return;
                }
                if (!table.isCurrent()) return;
            }
        }

        // deliver message to chat handler, or if there is none yet,
        // queue message
        synchronized (chatQueue_)
        {
            if (chat_ != null)
                chat_.chatReceived(chat);
            else
                chatQueue_.add(chat);
        }
    }

    /**
     * Set the chat handler
     */
    public void setChatHandler(ChatHandler chat)
    {
        synchronized (chatQueue_)
        {
            chat_ = chat;

            int nNum = chatQueue_.size();

            if (chat_ != null && nNum > 0)
            {
                for (int i = 0; i < nNum; i++)
                {
                    chat_.chatReceived(chatQueue_.get(i));
                }
                chatQueue_.clear();
            }
        }
    }

    /**
     * display dealer chat local
     */
    public void sendDealerChatLocal(int nType, String sMessage)
    {
        deliverChatLocal(nType, sMessage, OnlineMessage.CHAT_DEALER_MSG_ID);
    }

    /**
     * In practice mode, used due to no online manager, in online
     * used to deliver messages as a result of some other action (to
     * avoid unnecessary network traffic)
     */
    public void deliverChatLocal(int nType, String sMessage, int id)
    {
        if (chat_ != null)
        {
            OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
            chat.setChat(sMessage);
            chat.setChatType(nType);
            chat.setFromPlayerID(id);
            chat_.chatReceived(chat);
        }
    }

    ////
    //// Helper methods
    ////

    /**
     * Get player from id in message
     */
    private PokerPlayer getPlayer(OnlineMessage omsg, boolean bThrowErrorIfMissing)
    {
        int id = omsg.getFromPlayerID();
        PokerPlayer player = game_.getPokerPlayerFromID(id);
        if (player == null)
        {
            if (bThrowErrorIfMissing)
            {
                throw new ApplicationError(ErrorCodes.ERROR_INVALID, "No player found for id in message", omsg.toString(), null);
            }
            else
            {
                logger.warn("No player found for id in message: " + omsg);
            }
        }
        return player;
    }

    /**
     * Send request to do phase to player
     */
    public void onlineProcessPhase(String sPhaseName, DMTypedHashMap params,
                                   boolean bLocalNow, PokerPlayer to)
    {
        // if locally controlled
        if (to.isLocallyControlled())
        {
            if (bLocalNow) context_.processPhaseNow(sPhaseName, params);
            else context_.processPhase(sPhaseName, params);
            return;
        }
        OnlineMessage omsg = prepareMessage(OnlineMessage.CAT_PROCESS_PHASE);
        omsg.setPhaseName(sPhaseName);
        omsg.setPhaseParams(params);
        sendMessage(omsg, to);
    }

    /**
     * Handle processPhase request recieved
     */
    private void processPhase(OnlineMessage omsg)
    {
        // check for null since we call this with no phases sometimes
        String sPhase = omsg.getPhaseName();
        if (sPhase == null) return;

        context_.processPhase(sPhase, omsg.getPhaseParams());
    }

    /**
     * Send message to all players except host
     */
    private void sendMessageAll(OnlineMessage omsg)
    {
        sendMessageAllExcept(omsg, null, false);
    }

    /**
     * Send message to all players except host
     */
    private void sendMessageAll(OnlineMessage omsg, boolean bImmediate)
    {
        sendMessageAllExcept(omsg, null, bImmediate);
    }

    /**
     * Send message to all players at given table except host
     */
    private void sendMessageTable(OnlineMessage omsg, PokerTable table, PokerPlayer exclude)
    {
        // just in case ... could be null if received message
        // from player at table and that table went way in the
        // meantime
        if (table == null) return;

        PokerPlayer p;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = table.getPlayer(i);
            if (p == null || p.isComputer() || p.isHost() || p.isRejoining() || p == exclude) continue;

            sendMessage(omsg, p);
        }

        // send to observers, send in reverse order since failure causes
        // observer to get removed
        int nNum = table.getNumObservers();
        for (int i = nNum - 1; i >= 0; i--)
        {
            p = table.getObserver(i);
            if (p.isHost() || p.isRejoining() || p == exclude) continue;

            sendMessage(omsg, p);
        }
    }

    /**
     * Send message to all players and observers except specified player or host or computer
     */
    private void sendMessageAllExcept(OnlineMessage omsg, PokerPlayer exclude, boolean bImmediate)
    {
        PokerPlayer p;

        // send to players
        int nNum = game_.getNumPlayers();
        for (int i = 0; i < nNum; i++)
        {
            p = game_.getPokerPlayerAt(i);
            // skip excluded player, host, computer players (for obvious reasons)
            // skip observers who were players (sent below).
            // skip eliminated players who have no socket (quit being observers)
            if (p == exclude || p.isHost() || p.isComputer() || p.isObserver()) continue;

            sendMessage(omsg, p, bImmediate);
        }

        // send to observers - loop from end
        // incase an error occurs and the connection is
        // closed, which removes the observer from the list
        nNum = game_.getNumObservers();
        for (int i = nNum - 1; i >= 0; i--)
        {
            p = game_.getPokerObserverAt(i);
            if (p == exclude || p.isHost()) continue;

            sendMessage(omsg, p, bImmediate);
        }
    }

    /**
     * Send message to given player
     */
    private void sendMessage(OnlineMessage omsg, PokerPlayer pTo)
    {
        sendMessage(omsg, pTo, false);
    }

    /**
     * Send message to given player.  If bImmediate, sends message now otherwise
     * adds to queue to send later
     */
    private void sendMessage(OnlineMessage omsg, PokerPlayer pTo, boolean bImmediate)
    {
        PokerConnection pc = pTo.getConnection();
        if (pc == null)
        {
            if (pTo.isHost() && pTo == getLocalPlayer())
            {
                logger.warn("Attempting to send message from host to host " + omsg.toStringNoData());
            }
            else if (pTo.isEliminated())
            {
                return;
            }
            else
            {
                //logger.warn("Can't send message to: " + pTo.getName() + " (no socket): " + omsg.toStringNoData());
            }
            return;
        }

        // TCP: use online manager queue
        if (!isUDP())
        {
            oQueue_.addMessage(omsg, pTo, bImmediate);
        }
        // UDP: just send since the infrastructure queues it already.  The immediate flag is
        // unnecessary since it will be sent pretty much immediately due to use of BlockingQueue
        else
        {
            DDMessageTransporter msg = p2p_.newMessage(omsg.getData());
            PokerUDPServer udp = (PokerUDPServer) p2p_; // cast to avoid catching IOException
            udp.send(pc, msg);

            // disconnect test like in OnlineManagerQueue
            if (DebugConfig.isTestingOn() &&
                omsg.getCategory() == OnlineMessage.CAT_CHAT &&
                omsg.getChat().startsWith("disconnect test"))
            {
                udp.closeConnection(pc);
            }
        }
    }

    /**
     * get error reply
     */
    public static DDMessageTransporter getAppErrorReply(PokerConnectionServer p2p, OnlineMessage omsg, String sMsg, boolean bKeepAlive)
    {
        OnlineMessage oreply = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
        oreply.setApplicationErrorMessage(sMsg);
        oreply.setInReplyTo(omsg.getMessageID());
        DDMessageTransporter reply = p2p.newMessage(oreply.getData());
        reply.setKeepAlive(bKeepAlive);
        return reply;
    }

    /**
     * Return message to be sent in response to test message.  In a static
     * so can be used from PokerMain when no OnlineManager instance exists
     */
    public static DDMessageTransporter getTestReply(PokerConnectionServer p2p, String sGuid, OnlineMessage in)
    {
        // init (we don't enter id/password incase this is from somewhere else)
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_TEST); // test reply
        DDMessageTransporter reply = p2p.newMessage(omsg.getData());

        // don't keep alive (only a test)
        reply.setKeepAlive(false);

        // respond with GUID for match test and reply to
        omsg.setGUID(sGuid);
        omsg.setInReplyTo(in.getMessageID());

        // sleep briefly in udp mode so connection shows up on UDPStatus
        if (p2p instanceof UDPServer)
        {
            Utils.sleepMillis(100);
        }

        return reply;
    }

    /**
     * Create OnlineMessage with standard parameters
     */
    private OnlineMessage prepareMessage(int nCategory)
    {
        PokerPlayer local = getLocalPlayer();
        OnlineMessage msg = new OnlineMessage(nCategory); // prepareMessage
        msg.setGameID(game_.getOnlineGameID());
        msg.setPassword(game_.getOnlinePassword());

        // on client, this is the URL to connect to the host
        // on host, this is null
        msg.setConnectURL(local.getConnectURL());

        return msg;
    }

    /**
     * register socket with p2p server
     */
    void registerSocket(SocketChannel channel)
    {
        try
        {
            if (p2p_ instanceof Peer2PeerServer)
            {
                ((GameServer) p2p_).registerChannel(channel, SelectionKey.OP_READ);
            }
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }

    /**
     * Get local player for this machine
     */
    public PokerPlayer getLocalPlayer()
    {
        return game_.getLocalPlayer();
    }

    /**
     * Get host
     */
    public PokerPlayer getHost()
    {
        return game_.getHost();
    }

    /**
     * return whether this is the host instance
     */
    public boolean isHost()
    {
        return bHost_;
    }

    ////
    //// OnlineMessageListener stuff
    ////

    // listener list
    protected List<OnlineMessageListener> listenerList = new ArrayList<OnlineMessageListener>();

    /**
     * Adds a listener to the list
     */
    public void addOnlineMessageListener(OnlineMessageListener listener)
    {
        if (listenerList.contains(listener)) return;
        listenerList.add(listener);
    }

    /**
     * Removes a listener from the list.
     */
    public void removeOnlineMessageListener(OnlineMessageListener listener)
    {
        listenerList.remove(listener);
    }

    /**
     * Call each listener with the message received.
     */
    protected void fireAction(OnlineMessage omsg)
    {
        for (int i = listenerList.size() - 1; i >= 0; i -= 1)
        {
            listenerList.get(i).messageReceived(omsg);
        }
    }

    ////
    //// For handling errors
    ////

    /**
     * Methods throw this error with the message to return
     */
    private static class OnlineError extends RuntimeException
    {
        private DDMessageTransporter reply;

        private OnlineError(DDMessageTransporter reply)
        {
            this.reply = reply;
        }

        public DDMessageTransporter getReply()
        {
            return reply;
        }
    }
}
