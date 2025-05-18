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
 * PokerServlet.java
 *
 * Created on March 6, 2003, 6:23 PM
 */

package com.donohoedigital.games.poker.server;


import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.db.DBUtils;
import com.donohoedigital.games.comms.ActionItem;
import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import com.donohoedigital.games.poker.network.OnlineMessage;
import com.donohoedigital.games.poker.network.PokerConnect;
import com.donohoedigital.games.poker.network.PokerURL;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.server.ActionHandler;
import com.donohoedigital.games.server.EngineServlet;
import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.service.BannedKeyService;
import com.donohoedigital.jsp.JspEmail;
import com.donohoedigital.mail.DDPostalService;
import com.donohoedigital.p2p.Peer2PeerClient;
import com.donohoedigital.p2p.Peer2PeerMessage;
import com.donohoedigital.udp.UDPServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;

import static com.donohoedigital.config.DebugConfig.TESTING;
import static com.donohoedigital.games.config.EngineConstants.*;
import static com.donohoedigital.games.poker.service.OnlineGameService.OrderByType.mode;

/**
 * @author donohoe
 */
public class PokerServlet extends EngineServlet
{
    @Autowired
    private OnlineGameService onlineGameService;
    @Autowired
    private OnlineProfileService onlineProfileService;

    /**
     * init from gameserver
     */
    @Override
    public void afterConfigInit()
    {
        super.afterConfigInit();
    }

    /**
     * Return action handler
     */
    @Override
    public final ActionHandler getActionHandler()
    {
        return null;
    }

    /**
     * Get version
     */
    @Override
    public final Version getLatestVersion()
    {
        return PokerConstants.VERSION;
    }

    /**
     * Return latest client version
     *
     * @return
     */
    @Override
    public Version getLatestClientVersion(String os)
    {
        if (Utils.isMacOS(os)) return PokerConstants.LATEST_MAC;
        if (Utils.isLinux(os)) return PokerConstants.LATEST_LINUX;
        if (Utils.isWindows(os)) return PokerConstants.LATEST_WINDOWS;
        throw new ApplicationError("Unknown os: " + os);
    }

    /**
     * Get keystart
     */
    @Override
    public final int getKeyStart(Version received)
    {
        return PokerConstants.getKeyStart(received);
    }

    /**
     * Return action which indicates an online game is missing
     */
    @Override
    protected ActionItem getMissingGameAction()
    {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getMissingGameAction() not used in PokerServlet", null);
    }

    /**
     * test for banned key
     */
    private EngineMessage banCheck(DDMessage received)
    {
        BannedKey ban = bannedKeyService.getIfBanned(received.getKey());
        if (ban != null)
        {
            EngineMessage ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                                  EngineMessage.PLAYER_SERVER,
                                                  EngineMessage.CAT_APPL_ERROR);

            ret.setApplicationErrorMessage(getAndLogBanMessage(ban, received));
            return ret;
        }

        return null;
    }

    /**
     * test for banned key
     */
    public OnlineMessage banCheck(OnlineProfile profile)
    {
        OnlineMessage resMsg = null;
        String banMsg = banCheck(bannedKeyService, profile);
        if (banMsg != null)
        {
            // Profile already exists, so report an error also set flag so client can
            // deal.  Using PARAM_ELIMINATED cuz I'm lazy.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.getData().setBoolean(EngineMessage.PARAM_ELIMINATED, true);
            resMsg.setApplicationErrorMessage(banMsg);
        }
        return resMsg;
    }

    /**
     * test for banned key
     */
    public static String banCheck(BannedKeyService banService, OnlineProfile profile)
    {
        if (profile == null) return null;
        BannedKey ban = banService.getIfBanned(profile.getLicenseKey(), profile.getEmail(), profile.getName());
        if (ban != null)
        {
            return getAndLogBanMessage(ban, profile);
        }

        return null;
    }

    /**
     * ask if can process given category
     */
    @Override
    protected boolean isSubclassHandling(int nCategory)
    {
        switch (nCategory)
        {
            case OnlineMessage.CAT_TEST:
            case OnlineMessage.CAT_WAN_GAME_ADD:
            case OnlineMessage.CAT_WAN_GAME_UPDATE:
            case OnlineMessage.CAT_WAN_GAME_REMOVE:
            case OnlineMessage.CAT_WAN_GAME_LIST:
            case OnlineMessage.CAT_WAN_GAME_START:
            case OnlineMessage.CAT_WAN_GAME_STOP:
            case OnlineMessage.CAT_WAN_GAME_END:
            case OnlineMessage.CAT_WAN_PROFILE_ADD:
            case OnlineMessage.CAT_WAN_PROFILE_RESET:
            case OnlineMessage.CAT_WAN_PROFILE_LINK:
            case OnlineMessage.CAT_WAN_PROFILE_ACTIVATE:
            case OnlineMessage.CAT_WAN_PROFILE_VALIDATE:
            case OnlineMessage.CAT_WAN_PROFILE_SEND_PASSWORD:
            case OnlineMessage.CAT_WAN_PROFILE_CHANGE_PASSWORD:
            case OnlineMessage.CAT_WAN_PROFILE_SYNC_PASSWORD:
                return true;
            default:
                return false;
        }
    }

    /**
     * is isSubclassHandling returned true, then this is called
     */
    @Override
    protected DDMessage subclassProcessMessage(HttpServletRequest request, HttpServletResponse response, DDMessage ddreceived)
    {
        // disallow all actions if on public game ban list
        EngineMessage ban = banCheck(ddreceived);
        if (ban != null) return ban;

        switch (ddreceived.getCategory())
        {
            case OnlineMessage.CAT_TEST:
                return testP2pConnect(request, ddreceived);

            case OnlineMessage.CAT_WAN_GAME_ADD:
                return addWanGame(ddreceived);

            case OnlineMessage.CAT_WAN_GAME_UPDATE:
                return updateWanGame(ddreceived, false);

            case OnlineMessage.CAT_WAN_GAME_REMOVE:
                return deleteWanGame(ddreceived);

            case OnlineMessage.CAT_WAN_GAME_LIST:
                return getWanGames(ddreceived);

            case OnlineMessage.CAT_WAN_GAME_START:
                return updateWanGame(ddreceived, true);

            case OnlineMessage.CAT_WAN_GAME_STOP:
                return endWanGame(ddreceived);

            case OnlineMessage.CAT_WAN_GAME_END:
                return endWanGame(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_ADD:
                return addOnlineProfile(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_RESET:
                return resetOnlineProfile(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_LINK:
                return linkOnlineProfile(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_ACTIVATE:
                return activateOnlineProfile(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_VALIDATE:
                return validateProfile(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_SEND_PASSWORD:
                return sendOnlineProfilePassword(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_CHANGE_PASSWORD:
                return changeOnlineProfilePassword(ddreceived);

            case OnlineMessage.CAT_WAN_PROFILE_SYNC_PASSWORD:
                return syncOnlineProfilePassword(ddreceived);

            default:
                throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "PokerServlet does not handle this category: " +
                                                                         ddreceived.getCategory(), null);
        }
    }

    /**
     * allow demo users to test internet connection
     */
    @Override
    protected boolean isCategoryValidated(EngineMessage received)
    {
        if (received.getCategory() == OnlineMessage.CAT_TEST &&
            received.getVersion().isDemo())
        {
            return false;
        }

        return super.isCategoryValidated(received);
    }

    /**
     * Is database access required for category?
     */
    @Override
    protected boolean subclassIsDatabaseRequired(int nCategory)
    {
        switch (nCategory)
        {
            case OnlineMessage.CAT_WAN_GAME_ADD:
            case OnlineMessage.CAT_WAN_GAME_UPDATE:
            case OnlineMessage.CAT_WAN_GAME_REMOVE:
            case OnlineMessage.CAT_WAN_GAME_LIST:
            case OnlineMessage.CAT_WAN_GAME_START:
            case OnlineMessage.CAT_WAN_GAME_STOP:
            case OnlineMessage.CAT_WAN_GAME_END:
            case OnlineMessage.CAT_WAN_PROFILE_ADD:
            case OnlineMessage.CAT_WAN_PROFILE_RESET:
            case OnlineMessage.CAT_WAN_PROFILE_LINK:
            case OnlineMessage.CAT_WAN_PROFILE_ACTIVATE:
            case OnlineMessage.CAT_WAN_PROFILE_VALIDATE:
            case OnlineMessage.CAT_WAN_PROFILE_SEND_PASSWORD:
            case OnlineMessage.CAT_WAN_PROFILE_CHANGE_PASSWORD:
            case OnlineMessage.CAT_WAN_PROFILE_SYNC_PASSWORD:
                return true;
            default:
                return false;
        }
    }

    /**
     * As of 3.0, we don't send back the flag to disable the game
     */
    @Override
    protected boolean isResetClientOnBadKey()
    {
        return false;
    }

    /**
     * DESIGN NOTE:  I thought about validating this for DD Poker 3, but decided not to bother.  Our auth
     * logic is kind of a pain and needs a redesign.  We should always be sending down the current player
     * (like we do with version/key).  FIX:  make this better in 3.0++
     */

    /**
     * Test connection to the provided connect URL
     */
    private DDMessage testP2pConnect(HttpServletRequest request, DDMessage ddreceived)
    {
        if (TESTING(UDPServer.TESTING_UDP))
            logger.debug("Starting test public connect --------------------------------");

        // enclose sent message in an OnlineMessage
        OnlineMessage omsg = new OnlineMessage(ddreceived);

        // if error, message key or message text here
        String sErrorKey = null;
        String sErrorMsg = null;

        // return message
        DDMessage ret = new DDMessage(OnlineMessage.CAT_TEST);

        // get connect URL
        PokerURL url = omsg.getConnectURL();
        ApplicationError.assertNotNull(url, "Missing connect URL");

        // test it
        try
        {
            OnlineMessage oreply;
            if (url.isUDP())
            {
                PokerConnect conn = new PokerConnect(((PokerServer) getServer()).getUDPServer(), url, omsg.getUPDID(), null);
                conn.connect(omsg);
                conn.close();
                oreply = conn.getReply();
            }
            else
            {
                Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, omsg.getData());
                Peer2PeerClient p2pClient = new Peer2PeerClient(url, null, null);
                p2pClient.connect();
                Peer2PeerMessage reply = p2pClient.sendGetReply(p2p);
                p2pClient.close();

                oreply = new OnlineMessage(reply.getMessage());
            }

            if (oreply.getCategory() == DDMessage.CAT_APPL_ERROR)
            {
                sErrorMsg = oreply.getApplicationErrorMessage();
                if (sErrorMsg == null && oreply.getData().getStatus() == DDMessageListener.STATUS_TIMEOUT)
                    sErrorKey = "msg.p2p.timeout";
            }
            else
            {
                String guid1 = omsg.getGUID();
                String guid2 = oreply.getGUID();
                ApplicationError.assertNotNull(guid1, "No GUID in original test request.", omsg);
                ApplicationError.assertNotNull(guid2, "No GUID in reply to test.", oreply);

                // if guid's don't match, then user attempted to connect to
                // another dd poker computer
                if (!guid1.equals(guid2))
                {
                    sErrorKey = "msg.p2p.guid.mismatch";
                }
            }
        }
        catch (Throwable t)
        {
            //noinspection ChainOfInstanceofChecks
            if (t instanceof ConnectException)
            {
                sErrorKey = "msg.p2p.refused";
            }
            else if (t instanceof SocketException)
            {
                sErrorKey = "msg.p2p.refused";
            }
            else if (t instanceof SocketTimeoutException ||
                     t instanceof EOFException)
            {
                sErrorKey = "msg.p2p.timeout";
            }
            else
            {
                throw new ApplicationError(t);
            }
        }

        if (sErrorKey != null || sErrorMsg != null)
        {
            logger.info("P2P connect test failed from " + ddreceived.getKey() + " [" +
                        request.getRemoteAddr() + "] to " + url + ": " + sErrorKey);
            ret.setCategory(EngineMessage.CAT_APPL_ERROR);
            if (sErrorMsg != null)
                ret.setApplicationErrorMessage(sErrorMsg);
            else
                ret.setApplicationErrorMessage(PropertyConfig.getMessage(sErrorKey));
        }
        else
        {
            logger.info("P2P connect test succeeded from " + ddreceived.getKey() + " [" +
                        request.getRemoteAddr() + "] to " + url);
        }

        return ret;
    }


    /***
     * DESIGN NOTE TO FUTURE FORGETFUL DOUG:
     *
     * Added host post auth in DD Poker 3.  It is slightly different than what happens in
     * getWanGames.  In this case, we just refuse to post and return a message.  In getWanGames,
     * we return the list and then actually disable the client profile (in FindGames.getWanList()),
     * forcing them to re-enter a password.
     */


    /**
     * Add a WAN game to the list.  If one already exists for the given key/URL combination,
     * then it is replaced with the given game.  This request should only be received from
     * the host player.
     */
    private DDMessage addWanGame(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        Version version = ddreceived.getVersion();
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        OnlineGame game = new OnlineGame(reqMsg.getWanGame());
        OnlineMessage resMsg = null;
        OnlineProfile profile = null;

        // ban check
        resMsg = banCheck(profile);
        if (resMsg != null) return resMsg.getData();

        // Prior to version 3, we didn't send down auth credentials, so just get the
        // profile (we make sure it is activated below)
        boolean before3 = version.isBefore(PokerConstants.VERSION_HOST_CHECK_ADDED);
        if (before3)
        {
            profile = onlineProfileService.getOnlineProfileByName(game.getHostPlayer());
        }
        /// Version 3 and later, we validate user/password
        else
        {
            OnlineProfile auth = new OnlineProfile(reqMsg.getWanAuth());
            profile = onlineProfileService.authenticateOnlineProfile(auth);
        }

        // if no profile - error (either missing or could not auth)
        if (profile == null)
        {
            // Profile does not exist, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty(before3 ?
                                                                               "msg.wanprofile.missing" :
                                                                               "msg.wanprofile.authfailed3"));
        }
        else if (!profile.isActivated())
        {
            // Profile is not activated, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.notactivated"));
        }
        else
        {
            onlineGameService.saveOnlineGame(game);

            // Send an empty response.
            resMsg = new OnlineMessage(ddreceived.getCategory());
        }

        return resMsg.getData();
    }

    /**
     * Update the given WAN game.
     */
    private DDMessage updateWanGame(DDMessage ddreceived, boolean setStartDate)
    {
        // Wrap everything in useable interfaces.
        OnlineGame game = new OnlineGame(new OnlineMessage(ddreceived).getWanGame());

        // don't rely on client date
        if (setStartDate) game.setStartDate(new Date());

        // Update in the database.
        game = onlineGameService.updateOnlineGame(game);

        // game could be null because of bug in OnlineManager.processQuit()
        if (game == null)
        {
            //logger.info("Did not update: " + ddreceived);
        }

        // Send an empty response.
        OnlineMessage resMsg = new OnlineMessage(ddreceived.getCategory());
        return resMsg.getData();
    }

    /**
     * End the given WAN game.  This request should only be received from the game host player.
     */
    @SuppressWarnings({"unchecked"})
    private DDMessage endWanGame(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        OnlineGame game = new OnlineGame(reqMsg.getWanGame());

        // don't rely on date sent from client
        game.setEndDate(new Date());

        // Update the game in the database, saving histories
        List<TournamentHistory> histories = (List<TournamentHistory>) reqMsg.getWanHistories();
        game = onlineGameService.updateOnlineGame(game, new TournamentHistoryList(histories));

        // if game is null, then we didn't save, log an error so we know how often this happens
        // FIX: make this end-game stuff more robust.  Should send down everything from the client so we can deal with this
        if (game == null)
        {
            logger.error("Unable to save ended game because it didn't exist in database: " + game + ";   histories:  " + histories);
        }

        // Send an empty response.
        OnlineMessage resMsg = new OnlineMessage(ddreceived.getCategory());
        return resMsg.getData();
    }

    /**
     * Delete a WAN game from the list.  This request should only be received from the host player.
     */
    private DDMessage deleteWanGame(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineGame game = new OnlineGame(new OnlineMessage(ddreceived).getWanGame());

        // Delete from the database.
        onlineGameService.deleteOnlineGame(game);

        // Send an empty response.
        OnlineMessage resMsg = new OnlineMessage(ddreceived.getCategory());
        return resMsg.getData();
    }

    /**
     * Get a list of available WAN games.
     */
    private DDMessage getWanGames(DDMessage ddreceived)
    {
        // Optionally authenticate the profile making the request.
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        OnlineMessage resMsg = new OnlineMessage(ddreceived.getCategory());
        DMTypedHashMap authProfile = reqMsg.getWanAuth();

        if (authProfile != null)
        {
            OnlineProfile auth = new OnlineProfile(authProfile);

            // ban check
            OnlineMessage banMsg = banCheck(auth);
            if (banMsg != null) return banMsg.getData();

            if (onlineProfileService.authenticateOnlineProfile(auth) != null)
            {
                // If the profile was authenticated, return the same value without the password.
                auth.setPassword(null);
                resMsg.setWanAuth(auth.getData());
            }
            else
            {
                // missing - handled in FindGames.getWanList()
            }
        }

        // Select from the database.
        int nMode = reqMsg.getMode();
        Integer[] modes;

        // in 2.5, if we see combo mode, treat accordingly
        if (nMode == OnlineGame.FETCH_MODE_REG_PLAY)
        {
            modes = new Integer[]{OnlineGame.MODE_REG, OnlineGame.MODE_PLAY};
        }
        // pre 2.5 - passed down a single mode
        else
        {
            modes = new Integer[]{nMode};
        }

        // get games
        OnlineGameList clients = onlineGameService.getOnlineGames(null, reqMsg.getOffset(), reqMsg.getCount(),
                                                                  modes, null, null, null, mode);
        DMArrayList<DMTypedHashMap> games = clients.getAsDMList();

        // Return the list
        resMsg.setCount(clients.getTotalSize());
        resMsg.setWanGames(games);

        return resMsg.getData();
    }

    /**
     * Validate profile
     */
    private DDMessage validateProfile(DDMessage ddreceived)
    {
        // Optionally authenticate the profile making the request.
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        OnlineMessage resMsg = new OnlineMessage(ddreceived.getCategory());
        DMTypedHashMap authProfile = reqMsg.getWanAuth();

        OnlineProfile auth = new OnlineProfile(authProfile);

        // ban check
        OnlineMessage banMsg = banCheck(auth);
        if (banMsg != null) return banMsg.getData();

        if (onlineProfileService.authenticateOnlineProfile(auth) != null)
        {
            // If the profile was authenticated, return the same value without the password.
            auth.setPassword(null);
            resMsg.setWanAuth(auth.getData());
        }
        else
        {
            // missing - handled in FindGames.validateProfile()
        }

        return resMsg.getData();
    }

    /**
     * Add a WAN profile.  If one already exists for the given name, an error indicator is returned.
     */
    private DDMessage addOnlineProfile(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        OnlineProfile profile = new OnlineProfile(reqMsg.getOnlineProfileData()); // sets name
        OnlineMessage resMsg = null;

        // ban check - ignore name
        OnlineProfile banCheck = new OnlineProfile();
        banCheck.setEmail(profile.getEmail());
        banCheck.setLicenseKey(profile.getLicenseKey());
        resMsg = banCheck(banCheck);
        if (resMsg != null) return resMsg.getData();

        // Determine if the profile name is valid.
        if (onlineProfileService.isNameValid(profile.getName()))
        {
            // count existing profiles for email
            int count = onlineProfileService.getMatchingOnlineProfilesCount(null, DBUtils.sqlExactMatch(profile.getEmail()), null, false);
            if (count >= PokerConstants.MAX_PROFILES_PER_EMAIL && !(profile.getEmail().endsWith("donohoe.info")))
            {
                // at max profiles
                resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
                resMsg.setApplicationErrorMessage(PropertyConfig.getMessage("msg.wanprofile.maxemail",
                                                                            profile.getEmail(),
                                                                            count, PokerConstants.MAX_PROFILES_PER_EMAIL));
            }
            else
            {
                // Generate a password and set values.
                String generatedPassword = onlineProfileService.generatePassword();
                profile.setPassword(generatedPassword);
                profile.setLicenseKey(ddreceived.getKey());
                profile.setActivated(false);

                // Insert the database record - returns false if it is a duplicate
                if (onlineProfileService.saveOnlineProfile(profile))
                {
                    // Email the password.
                    sendProfileEmail(postalService, "profile", profile.getEmail(), profile.getName(), generatedPassword, null);

                    // Profile was inserted, so report success.
                    resMsg = new OnlineMessage(ddreceived.getCategory());
                    resMsg.setApplicationStatusMessage(PropertyConfig.getStringProperty("msg.wanprofile.add"));
                }
                else
                {
                    // Profile already exists, so report an error.
                    resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
                    resMsg.setApplicationErrorMessage(PropertyConfig.getMessage("msg.wanprofile.duplicate", profile.getName()));
                }
            }
        }
        else
        {
            // Name is invalid, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getMessage("msg.wanprofile.invalid", profile.getName()));
        }

        return resMsg.getData();
    }

    /**
     * ANOTHER DESIGN NOTE:
     *
     * In DD Poker 2.x one could change email before a profile was activated (i.e., in the case
     * of emails not getting through due to spam filters).  We want to replicate that functionality
     * and keep backwards compatibility.  However, we will be a bit more restrictive.  If no password
     * is included we do the logic below (XXX).
     */

    /**
     * Update a WAN profile email address, mark inactivated and send a new password
     */
    private DDMessage resetOnlineProfile(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineProfile profile = new OnlineProfile(new OnlineMessage(ddreceived).getOnlineProfileData());
        OnlineMessage resMsg = null;

        // ban check
        resMsg = banCheck(profile);
        if (resMsg != null) return resMsg.getData();

        // Authenticate
        OnlineProfile profileToUpdate = onlineProfileService.authenticateOnlineProfile(profile);

        // XXX case (change email for new profile)
        if (profileToUpdate == null && profile.getPassword() == null)
        {
            profileToUpdate = onlineProfileService.getOnlineProfileByName(profile.getName());
            // ignore already activated (suspicious!) or retired profiles
            if (profileToUpdate != null && (profileToUpdate.isActivated() || profileToUpdate.isRetired()))
            {
                profileToUpdate = null;
            }
        }

        // if we have a profile...
        if (profileToUpdate != null)
        {
            // make sure we are exceeding profiles for email
            int count = onlineProfileService.getMatchingOnlineProfilesCount(null, DBUtils.sqlExactMatch(profile.getEmail()), null, false);
            if (count >= PokerConstants.MAX_PROFILES_PER_EMAIL && !(profile.getEmail().endsWith("donohoe.info")))
            {
                // at max profiles
                resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
                resMsg.setApplicationErrorMessage(PropertyConfig.getMessage("msg.wanprofile.maxemail",
                                                                            profile.getEmail(),
                                                                            count, PokerConstants.MAX_PROFILES_PER_EMAIL));
            }
            else
            {
                // generate password and set values
                String generatedPassword = onlineProfileService.generatePassword();
                profileToUpdate.setEmail(profile.getEmail());
                profileToUpdate.setPassword(generatedPassword);
                profileToUpdate.setActivated(false);

                // save changes
                onlineProfileService.updateOnlineProfile(profileToUpdate);

                // Email the password.
                sendProfileEmail(postalService, "profile", profile.getEmail(), profile.getName(), generatedPassword, null);

                // Profile was updated, so report success.
                resMsg = new OnlineMessage(ddreceived.getCategory());
                resMsg.setApplicationStatusMessage(PropertyConfig.getStringProperty("msg.wanprofile.update"));
            }
        }
        else
        {
            // User does not exist, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.missing"));
        }

        return resMsg.getData();
    }

    /**
     * Link to existsing WAN profile.
     */
    private DDMessage linkOnlineProfile(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineProfile profile = new OnlineProfile(new OnlineMessage(ddreceived).getOnlineProfileData());
        OnlineMessage resMsg = null;

        // ban check
        resMsg = banCheck(profile);
        if (resMsg != null) return resMsg.getData();

        // Get the database record.
        OnlineProfile resProfile = onlineProfileService.authenticateOnlineProfile(profile);
        if (resProfile != null)
        {
            // Profile was found, so return it.
            resMsg = new OnlineMessage(ddreceived.getCategory());
            resMsg.setOnlineProfileData(resProfile.getData());
        }
        else
        {
            // Profile does not exist, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.unavailable"));
        }

        return resMsg.getData();
    }

    /**
     * Activate a WAN profile.
     */
    private DDMessage activateOnlineProfile(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineProfile profile = new OnlineProfile(new OnlineMessage(ddreceived).getOnlineProfileData());
        OnlineMessage resMsg = null;

        // ban check
        resMsg = banCheck(profile);
        if (resMsg != null) return resMsg.getData();

        // Authenticate
        OnlineProfile profileToUpdate = onlineProfileService.authenticateOnlineProfile(profile);
        if (profileToUpdate != null)
        {
            // set activated
            profileToUpdate.setActivated(true);

            // save changes
            onlineProfileService.updateOnlineProfile(profileToUpdate);

            // Login information is valid, so report success.
            resMsg = new OnlineMessage(ddreceived.getCategory());
            resMsg.setApplicationStatusMessage(PropertyConfig.getStringProperty("msg.wanprofile.activate"));
        }
        else
        {
            // Login information is invalid, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.authfailed"));
        }

        return resMsg.getData();
    }

    /**
     * Sync a WAN profile password
     */
    private DDMessage syncOnlineProfilePassword(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineProfile profile = new OnlineProfile(new OnlineMessage(ddreceived).getOnlineProfileData());
        OnlineMessage resMsg = null;

        // ban check
        resMsg = banCheck(profile);
        if (resMsg != null) return resMsg.getData();

        // Authenticate
        OnlineProfile profileToUpdate = onlineProfileService.authenticateOnlineProfile(profile);
        if (profileToUpdate != null)
        {
            // Login information is valid, so report success.
            resMsg = new OnlineMessage(ddreceived.getCategory());
            resMsg.setApplicationStatusMessage(PropertyConfig.getStringProperty("msg.wanprofile.sync"));
        }
        else
        {
            // Login information is invalid, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.authfailed2"));
        }

        return resMsg.getData();
    }

    /**
     * Assign a WAN profile password.
     */
    private DDMessage changeOnlineProfilePassword(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        OnlineProfile newpassword = new OnlineProfile(reqMsg.getOnlineProfileData());
        OnlineProfile oldpassword = new OnlineProfile(reqMsg.getWanAuth());
        OnlineMessage resMsg = null;

        // ban check
        resMsg = banCheck(oldpassword);
        if (resMsg != null) return resMsg.getData();

        // Authenticate based on old password (auth profile)
        OnlineProfile profileToUpdate = onlineProfileService.authenticateOnlineProfile(oldpassword);
        if (profileToUpdate != null)
        {
            // save new password
            profileToUpdate.setPassword(newpassword.getPassword());

            // save changes
            onlineProfileService.updateOnlineProfile(profileToUpdate);

            // Profile was updated, so report success.
            resMsg = new OnlineMessage(ddreceived.getCategory());
            resMsg.setApplicationStatusMessage(PropertyConfig.getStringProperty("msg.wanprofile.assign"));
        }
        else
        {
            // User does not exist, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.authfailed2"));
        }

        return resMsg.getData();
    }

    /**
     * Send a WAN profile password (at user request, from client, pre-activate only (so temp password).
     */
    private DDMessage sendOnlineProfilePassword(DDMessage ddreceived)
    {
        // Wrap everything in useable interfaces.
        OnlineMessage reqMsg = new OnlineMessage(ddreceived);
        DMTypedHashMap onlineProfileData = reqMsg.getOnlineProfileData();
        OnlineProfile profile = new OnlineProfile(onlineProfileData);
        OnlineMessage resMsg = null;

        // Retrieve the full profile information.
        profile = onlineProfileService.getOnlineProfileByName(profile.getName());
        if (profile != null)
        {
            // Email the password.
            sendProfileEmail(postalService, "profile", profile.getEmail(), profile.getName(), profile.getPassword(), null);

            // Profile information is valid, so report success.
            resMsg = new OnlineMessage(ddreceived.getCategory());
            resMsg.setApplicationStatusMessage(PropertyConfig.getStringProperty("msg.wanprofile.send"));
        }
        else
        {
            // Login information is invalid, so report an error.
            resMsg = new OnlineMessage(DDMessage.CAT_APPL_ERROR);
            resMsg.setApplicationErrorMessage(PropertyConfig.getStringProperty("msg.wanprofile.missing"));
        }

        return resMsg.getData();
    }

    /**
     * Send profile email to users.
     */
    public static void sendProfileEmail(DDPostalService service, String emailId, String sTo, String sName, String sPassword, String sLocale)
    {
        if (TESTING(TESTING_SKIP_EMAIL))
        {
            logger.info("Skipping profile email to " + sTo + "; name=" + sName + ", password=" + sPassword);
            return;
        }
        // if flag is on, send all emails to override address
        if (TESTING(TESTING_PROFILE_OVERRIDE_EMAIL))
        {
            String overrideEmail = PropertyConfig.getStringProperty(TESTING_PROFILE_OVERRIDE_EMAIL_TO, null, true);
            logger.info("Sending profile email to " + overrideEmail + " for " + sTo + "; name=" + sName + ", password=" + sPassword);
            sTo = overrideEmail;
        }
        if (sTo == null) return; // null for key verification so skip email

        // create and run jsp
        JspEmail email = new JspEmail(emailId, sLocale, null);
        email.getSession().setAttribute(OnlineProfile.PROFILE_NAME, sName);
        email.getSession().setAttribute(OnlineProfile.PROFILE_PASSWORD, sPassword);
        email.executeJSP();

        // only send the html message to Hotmail users since the multipart message gets mangled
        boolean isHotmail = sTo.toLowerCase().endsWith("@hotmail.com");
        String sPlainText = (isHotmail) ? null : email.getPlain();
        String sHtmlText = email.getHtml();

        // get results and send email
        service.sendMail(sTo, PropertyConfig.getRequiredStringProperty("settings.server.profilefrom"),
                         null, email.getSubject(),
                         sPlainText, sHtmlText,
                         null, null);
    }
}
