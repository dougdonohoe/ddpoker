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
 * EngineServlet.java
 *
 * Created on March 7, 2003, 2:23 PM
 */

package com.donohoedigital.games.server;


import com.donohoedigital.base.*;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.comms.Version;
import com.donohoedigital.config.*;
import com.donohoedigital.db.DatabaseManager;
import com.donohoedigital.games.comms.ActionItem;
import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.games.comms.RegistrationMessage;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.config.GameConfigUtils;
import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.model.Registration;
import com.donohoedigital.games.server.service.BannedKeyService;
import com.donohoedigital.games.server.service.RegistrationService;
import com.donohoedigital.jsp.JspEmail;
import com.donohoedigital.mail.DDPostalService;
import com.donohoedigital.server.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 * @author donohoe
 */
@SuppressWarnings("unchecked")
public abstract class EngineServlet extends BaseServlet
{
    public static final String EMAIL_PARAM_VERSION = "version";
    public static final String EMAIL_PARAM_NAME = "name";

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    protected BannedKeyService bannedKeyService;

    @Autowired
    protected DDPostalService postalService;

    private ActionHandler handler_;
    private Version version_;
    private File messageFile;
    private File upgradeFile;

    /**
     * Call super class method and then does other needed init
     */
    @Override
    public void afterConfigInit()
    {
        super.afterConfigInit();

        // init various engine stuff
        version_ = getLatestVersion();
        DDMessage.setDefaultVersion(version_);
        handler_ = getActionHandler();

        // if this is null, then we have a basic server
        if (handler_ != null)
        {
            handler_.init();
        }
        postalService.addErrorHandler(ConfigManager.getAppName(), new EngineMailErrorHandler(this));

        // message location
        File dir = new File(ConfigManager.getServerHome(), "messages");
        ApplicationError.assertTrue(dir.exists() && dir.isDirectory(), "Messages dir missing", dir);
        messageFile = new File(dir, ConfigManager.getAppName() + ".html");
        ApplicationError.assertTrue(messageFile.exists() && messageFile.isFile(), "Messages file missing", messageFile);
        upgradeFile = new File(dir, ConfigManager.getAppName() + "-upgrade.html");
        ApplicationError.assertTrue(upgradeFile.exists() && upgradeFile.isFile(), "Upgrade file missing", upgradeFile);
    }

    /**
     * Return action handler - subclass need to implement
     */
    public abstract ActionHandler getActionHandler();

    /**
     * Get latest version
     */
    public abstract Version getLatestVersion();

    /**
     * Get latest version of client given an os string
     */
    public abstract Version getLatestClientVersion(String os);

    /**
     * Get start of license key
     */
    public abstract int getKeyStart(Version received);

    /**
     * Return our subclass of DDMessage
     */
    @Override
    public DDMessage createNewMessage()
    {
        return new EngineMessage();
    }

    //
    /// DEBUGGING
    private static int SEQ = 0;
    private static final Object SEQOBJ = new Object();

    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    private int nextSEQ()
    {
        synchronized (SEQOBJ)
        {
            SEQ++;
            return SEQ;
        }
    }

    private void log(int seq, String sMethod, String sMsg)
    {
        logger.debug(sMethod + " [" + seq + "] " + (sMsg == null ? "" : sMsg));
    }

    /**
     * debugging
     */
    @Override
    public boolean isDebugOn()
    {
        return TESTING(EngineConstants.TESTING_SERVLET);
    }
    ///
    //

    /**
     * Wrapper
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException
    {
        int seq = nextSEQ();
        try
        {
            if (TESTING(EngineConstants.TESTING_SERVLET)) log(seq, "doGet()", "begin");
            super.doGet(request, response);
        }
        finally
        {
            if (TESTING(EngineConstants.TESTING_SERVLET)) log(seq, "doGet()", "end");
        }
    }

    /**
     * Wrapper
     */
    @Override
    protected void returnMessage(HttpServletResponse response, DDMessage ret)
            throws IOException
    {
        int seq = nextSEQ();
        try
        {
            if (TESTING(EngineConstants.TESTING_SERVLET)) log(seq, "returnMessage()", "begin");
            super.returnMessage(response, ret);
        }
        finally
        {
            if (TESTING(EngineConstants.TESTING_SERVLET)) log(seq, "returnMessage()", "end");
        }

    }

    /**
     * wrapper around process message
     */
    @Override
    public DDMessage processMessage(HttpServletRequest request, HttpServletResponse response, DDMessage ddreceived) throws IOException
    {
        int seq = nextSEQ();
        try
        {
            EngineMessage received = (EngineMessage) ddreceived;
            if (TESTING(EngineConstants.TESTING_SERVLET))
                log(seq, "processMessage()", "begin " + received.getDebugInfoShort());
            return _processMessage(request, response, ddreceived);
        }
        finally
        {
            if (TESTING(EngineConstants.TESTING_SERVLET)) log(seq, "processMessage()", "end");
        }
    }

    /**
     * process the received message and return a response
     */
    private DDMessage _processMessage(HttpServletRequest request, HttpServletResponse response, DDMessage ddreceived) throws IOException
    {
        // validate license key and version
        EngineMessage received = (EngineMessage) ddreceived;
        EngineMessage ret = validateKeyAndVersion(received, request.getRemoteAddr());

        // if we have a banned key during a verify/registration attempt, log it
        if (ret != null && ret.getBoolean(EngineMessage.PARAM_BANNED_KEY, false) &&
            (received.getCategory() == EngineMessage.CAT_VERIFY_KEY ||
             received.getCategory() == EngineMessage.CAT_USER_REG))
        {
            processUserRegistration(request, received, true);
        }

        // non-null return means something is invalid about this request
        if (ret != null) return ret;

        // process message
        int nCat = received.getCategory();
        switch (nCat)
        {
            // Testing message used for perf/other things
            case DDMessage.CAT_TESTING:
                return new DDMessage(DDMessage.CAT_TESTING, (String) null);

            // no message category set
            case DDMessage.CAT_NONE:
                throw new ApplicationError(ErrorCodes.ERROR_SERVER_INVALID_MESSAGE,
                                           "Message category not defined",
                                           "Specify a message category");

                // send back empty message (key is verified by virtue of getting here)
            case EngineMessage.CAT_VERIFY_KEY:
            case EngineMessage.CAT_USER_REG:
                return processUserRegistration(request, received, false);

            // server query - return URL that client should go to
            case EngineMessage.CAT_SERVER_QUERY:
                return getServerQuery();

            // new online game
            case EngineMessage.CAT_NEW_GAME:
                ServerSideGame game = ServerSideGame.newServerSideGame(received, handler_);
                return getGameData(game, game.getEmailAt(0));

            case EngineMessage.CAT_STATUS:
                return getOnlineStatus(received);

            case EngineMessage.CAT_PUBLIC_IP:
                return getPublicIP(request);

            case EngineMessage.CAT_CHECK_DDMSG:
                return checkDDmsg(received);

            // default - if a subclass says they'll handle the category, then
            // let them.  Otherwise, assume it is for an existing traditional
            // online game and handle it that way.
            default:
                if (isSubclassHandling(nCat)) return subclassProcessMessage(request, response, ddreceived);
                break;
        }

        return processExistingGameMessage(response, received);
    }

    /**
     * validate license key and version set in this EngineMessage - return an EngineMessage with
     * application error message set if there was a problem; null if no problem.
     */
    private EngineMessage validateKeyAndVersion(EngineMessage received, String sFromForLogging)
    {
        return validateKeyAndVersion(received, sFromForLogging,
                                     bannedKeyService,
                                     version_,
                                     getKeyStart(received.getVersion()),
                                     isCategoryValidated(received),
                                     isDatabaseRequired(received.getCategory()),
                                     isResetClientOnBadKey());
    }

    /**
     * For subclass to change this behavior
     */
    protected boolean isResetClientOnBadKey()
    {
        return true;
    }

    /**
     * validate license key and version set in this EngineMessage - return an EngineMessage with
     * application error message set if there was a problem; null if no problem.
     */
    public static EngineMessage validateKeyAndVersion(EngineMessage received, String sFromForLogging,
                                                      BannedKeyService banService,
                                                      Version serverVersion,
                                                      int receivedKeyStart,
                                                      boolean bCategoryValidated,
                                                      boolean bDatabaseRequired,
                                                      boolean setFlagToResetClient)
    {
        EngineMessage ret;
        String locale = received.getLocale();
        Version version = received.getVersion();

        // check version
        if (version == null)
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);

            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.noversion", locale));
            return ret;
        }

        // check alpha
        if (version.isAlpha() && version.isBefore(serverVersion))
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);

            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.wrongversion.alpha", locale, version));
            return ret;
        }

        // check beta
        if (version.isBeta() && version.isMajorMinorBefore(serverVersion))
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);

            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.wrongversion.beta", locale, version));
            return ret;
        }

        // demo version just in case
        if (version.isDemo() && bCategoryValidated)
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);

            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.nodemo", locale, version));
            return ret;
        }

        // check if valid license key
        String sKey = received.getKey();
        //noinspection PointlessNullCheck
        if (bCategoryValidated && (sKey == null || !Activation.validate(receivedKeyStart, sKey, locale)))
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);

            if (sKey == null) sKey = PropertyConfig.getLocalizedMessage("msg.missing", locale);
            if (setFlagToResetClient) ret.setBoolean(EngineMessage.PARAM_BAD_KEY, Boolean.TRUE);
            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.invalidkey", locale, sKey));
            logger.warn("Invalid key: " + sKey + " from " + sFromForLogging + ", msg: " + received.getDebugInfo());
            return ret;
        }

        // check database if required by the given category, see if undergoing maintenance
        if (bDatabaseRequired && PropertyConfig.getBooleanProperty("settings.db.maintenance", false))
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);
            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.db.maintenance", locale));

            return ret;
        }

        // check if banned license key (requires DB)
        BannedKey banMsg = banService.getIfBanned(sKey);
        if (banMsg != null)
        {
            ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                    EngineMessage.PLAYER_SERVER,
                                    EngineMessage.CAT_APPL_ERROR);

            if (setFlagToResetClient) ret.setBoolean(EngineMessage.PARAM_BANNED_KEY, Boolean.TRUE);
            ret.setApplicationErrorMessage(getAndLogBanMessage(banMsg, sKey + " from " + sFromForLogging + ", msg: " + received.getDebugInfo()));
            return ret;
        }

        return null;
    }


    /**
     * Ban message and logging
     */
    protected static String getAndLogBanMessage(BannedKey ban, Object thing)
    {
        DateFormat sf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        logger.warn("Banned rejection: " + thing + " (ban: " + ban + ")");
        return PropertyConfig.getMessage("msg.banned", sf.format(ban.getUntil()));
    }

    /**
     * return true if message with given category should be
     * checked for a valid key.  Default is true except
     * for CAT_PUBLIC_IP, which is false if demo.
     */
    protected boolean isCategoryValidated(EngineMessage received)
    {
        return !(received.getCategory() == EngineMessage.CAT_PUBLIC_IP &&
                 received.getVersion().isDemo());

    }

    /**
     * ask if can process given category
     */
    protected boolean isSubclassHandling(int nCategory)
    {
        return false;
    }

    /**
     * is isSubclassHandling returned true, then this is called
     */
    protected DDMessage subclassProcessMessage(HttpServletRequest request, HttpServletResponse response, DDMessage ddreceived)
    {
        return null;
    }

    /**
     * Process a message from an existing game.  Done using a lock around the
     * GameID, so that one thing happens at a time.  We return the message
     * explicitly, so it is done within a lock (important because files pointers
     * are used to return message data, and the files aren't read until the
     * message is written to the stream, so we need to write the message within
     * the lock)
     */
    private EngineMessage processExistingGameMessage(HttpServletResponse response,
                                                     EngineMessage received) throws IOException
    {
        DDByteArrayOutputStream retdata;

        // get an object unique to the game to prevent multiple requests accessing
        // game at the same time.
        String sGameID = received.getGameID();
        ObjectLock lock = GameConfigUtils.getGameLockingObject(sGameID);
        try
        {
            synchronized (lock)
            {
                EngineMessage ret; // okay to return null message (client can handle it)\

                // load game
                try
                {
                    ServerSideGame game = ServerSideGame.loadServerSideGame(received, handler_);
                    ret = processExistingGameMessageLocked(game, received);
                }
                catch (ApplicationError ae)
                {
                    if (ae.getErrorCode() == ErrorCodes.ERROR_FILE_NOT_FOUND)
                    {
                        if (received.getCategory() == EngineMessage.CAT_JOIN_GAME)
                        {
                            ret = getErrorMessage(null,
                                                  PropertyConfig.getLocalizedMessage("msg.joinfailed", received.getLocale()),
                                                  "Invalid join (no such game): " +
                                                  received.getDebugInfo());
                        }
                        else
                        {
                            ret = new EngineMessage(sGameID,
                                                    EngineMessage.PLAYER_SERVER,
                                                    EngineMessage.CAT_APPL_ERROR);

                            ret.setBoolean(EngineMessage.PARAM_GAME_DELETED, Boolean.TRUE);
                            ret.setApplicationErrorMessage(PropertyConfig.getLocalizedMessage("msg.missing", received.getLocale(), sGameID));
                        }
                    }
                    else throw ae;
                }

                // Patch 2 - get message contents (then release lock, then write)
                // need to write out message now (while game is locked)
                // so we can return the data when game is not locked
                retdata = new DDByteArrayOutputStream();
                ret.write(retdata);
            }
        }
        // in finally block so this is always done
        finally
        {
            GameConfigUtils.removeGameLockingObject(lock);
        }

        // we process the message here using retdata
        if (response != null)
        {
            returnMessage(response, retdata);
        }

        // return null to superclass
        return null;
    }

    /**
     * Assume game lock exists, used to re-enter this method w/out reacquiring lock
     */
    private EngineMessage processExistingGameMessageLocked(ServerSideGame game, EngineMessage received) throws IOException
    {
        EngineMessage ret = null;
        PlayerQueue queue;
        int nNum, id;
        DMArrayList<Integer> toIDs;

        // BUG 199 - store last time stamp and skip previously seen messages
        String key = received.getKey();
        boolean bSkipProcessing = false;
        long last, now;

        // Patch 2 - use sequence id for comparisons instead
        //           this if is to retain backwards compatability with War! 1.2 and prior
        long seq = received.getSeqID();
        if (seq > 0)
        {
            last = game.getLastSeqID(key);
            now = seq;
        }
        else
        {
            last = game.getLastTimeStamp(key);
            now = received.getCreateTimeStampLong();
        }

        // if there is a key (key can be null in cases like CAT_ERROR_BAD_EMAIL)
        // then make sure this message timestamp is after last one we recorded
        // ignore this on join game messages
        //
        // Always allow poll and join game messages to go through
        if (key != null && received.getCategory() != EngineMessage.CAT_JOIN_GAME &&
            received.getCategory() != EngineMessage.CAT_POLL_UPDATES)
        {
            //logger.debug(key + " - " + received.getDebugCat() + " - " + received.get(EngineMessage.PARAM_RESULT) + ",  now: " + now + " last: " + last + " diff: " + (now - last));
            if (now <= last)
            {
                bSkipProcessing = true;
                logger.warn(game.getGameID() + ": skipping duplicate message (last=" + last + ", now=" + now + "): " + received.getDebugInfo());
            }
        }

        // process message
        if (!bSkipProcessing)
        {
            boolean bSkipLastTime = false;
            boolean bSaveGame = false;

            switch (received.getCategory())
            {
                // join online game
                case EngineMessage.CAT_JOIN_GAME:
                    return joinOnlineGame(game, received);

                case EngineMessage.CAT_POLL_UPDATES:
                    ret = verifyPassword(game, received);
                    if (ret != null) break;

                    // BUG 6 - message chaining
                    // loop through any attached messages
                    // and process them.  I assume that none of the 
                    // attached messages require a response, which
                    // is true since I wrote them all.  Look below.
                    EngineMessage att;
                    int nNumChunks = received.getNumData();
                    byte[] bdata;
                    ByteArrayInputStream in;
                    for (int i = 0; i < nNumChunks; i++)
                    {
                        bdata = received.getDataAt(i);
                        in = new ByteArrayInputStream(bdata);
                        att = new EngineMessage();
                        att.read(in, bdata.length);
                        // if something returned, we have an error
                        ret = processExistingGameMessageLocked(game, att);
                        if (ret != null) break;
                    }

                    // return client update
                    if (ret == null)
                    {
                        ret = getClientUpdate(game, received);
                    }

                    // don't record time stamp of poll
                    bSkipLastTime = true;

                    break;

                case EngineMessage.CAT_ERROR_BAD_EMAIL:
                    // don't verify password here (sent from server)
                    bSkipLastTime = true;
                    break;

                case EngineMessage.CAT_GAME_UPDATE:
                case EngineMessage.CAT_INFO:
                case EngineMessage.CAT_CHAT:
                    ret = verifyPassword(game, received);
                    if (ret != null) break;
                    toIDs = (DMArrayList<Integer>) received.getList(EngineMessage.PARAM_PLAYER_IDS);
                    ApplicationError.assertNotNull(toIDs, "No ids to send message to");
                    received.setCreateTimeStamp(); // update to server time
                    nNum = toIDs.size();
                    File file;
                    long lasttime;
                    for (int i = 0; i < nNum; i++)
                    {
                        id = toIDs.get(i);
                        queue = game.getPlayerQueue(id);
                        queue.addMessage(received);

                        // WAR PATCH 2 
                        // preserve last modified time stamp so
                        // "last online" in client is correct
                        file = queue.getFile();
                        lasttime = file.lastModified();
                        queue.save();
                        //noinspection ResultOfMethodCallIgnored
                        file.setLastModified(lasttime);
                    }
                    break;

                case EngineMessage.CAT_ACTION_DONE:
                    ret = verifyPassword(game, received);
                    if (ret != null) break;
                    game.processActionDone(received);
                    bSaveGame = true;
                    break;

                case EngineMessage.CAT_ACTION_REQUEST:
                    ret = verifyPassword(game, received);
                    if (ret != null) break;
                    game.processActionRequest(received);
                    bSaveGame = true;
                    break;

                case EngineMessage.CAT_PLAYER_UPDATE:
                    ret = verifyPassword(game, received, true);
                    if (ret != null) break;
                    game.processPlayerUpdate(received);
                    bSaveGame = true;
                    break;

                default:
                    ret = new EngineMessage(game.getGameID(), EngineMessage.PLAYER_SERVER, EngineMessage.CAT_ERROR,
                                            "Message category unknown: " + received.getCategory());
            }

            // BUG 199 - store time stamp of message just processed
            if (!bSkipLastTime)
            {
                // Patch 2 - use seq id instead
                if (seq > 0)
                {
                    game.setLastSeqID(key, now);
                }
                else
                {
                    game.setLastTimeStamp(key, now);
                }
                bSaveGame = true;
            }

            if (bSaveGame) game.save();
        }

        return ret;
    }

    /**
     * return whether database is required to handle category
     */
    private boolean isDatabaseRequired(int nCategory)
    {
        switch (nCategory)
        {
            case EngineMessage.CAT_VERIFY_KEY:
            case EngineMessage.CAT_USER_REG:
                return true;
            default:
                if (isSubclassHandling(nCategory)) return subclassIsDatabaseRequired(nCategory);
                break;
        }
        return false;
    }

    /**
     * subclass callout to see if database is required to handle category
     */
    protected boolean subclassIsDatabaseRequired(int nCategory)
    {
        return false;
    }

    /**
     * verify password
     */
    private EngineMessage verifyPassword(ServerSideGame game, EngineMessage received)
    {
        return verifyPassword(game, received, false);
    }

    /**
     * Verify password received in a regular message from client
     */
    private EngineMessage verifyPassword(ServerSideGame game, EngineMessage received, boolean bSearchAll)
    {
        String sPass = received.getString(EngineMessage.PARAM_PASSWORD);
        int nID = received.getFromPlayerID();

        // if group, get 1st id in list
        if (nID == EngineMessage.PLAYER_GROUP)
        {
            DMArrayList<Integer> ids = (DMArrayList<Integer>) received.getList(EngineMessage.PARAM_PLAYER_IDS);
            ApplicationError.assertNotNull(ids, "ID list null", received);
            ApplicationError.assertTrue(!ids.isEmpty(), "ID list empty", received);
            nID = ids.get(0);
        }
        // if from "server" (chat message usage), then we need
        // to verify password against any player, like we
        // do for status update
        // also if we want to search all ids anyway (bSearchAll)
        else if (bSearchAll || nID == EngineMessage.PLAYER_SERVER)
        {
            nID = EngineMessage.PLAYER_GROUP;
        }
        else if (nID < 0)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Verify password for invalid id", "ID=" + nID);
        }

        return verifyPassword(game, nID, sPass, received);
    }

    /**
     * verify player(nID)'s password matches that in game.
     */
    private EngineMessage verifyPassword(ServerSideGame game, int nID, String sPass, EngineMessage received)
    {
        boolean bMatch = false;

        // passing EngineMessage.PLAYER_GROUP means we have to loop through all players (used
        // when getting online status)
        if (nID == EngineMessage.PLAYER_GROUP)
        {
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                if (game.getPasswordAt(i).equalsIgnoreCase(sPass))
                {
                    bMatch = true;
                    break;
                }
            }
        }
        // otherwise, just look at given id
        else
        {
            if (game.getPasswordAt(nID).equalsIgnoreCase(sPass))
            {
                bMatch = true;
            }
        }

        // if password for id doesn't match what the game thinks it should be
        // then send back an error message and log a warning    
        if (!bMatch)
        {
            return getErrorMessage(null, PropertyConfig.getLocalizedMessage("msg.badpass", received.getLocale(), game.getGameID()),
                                   "PASSWORD MISMATCH: " + received.getDebugInfo());
        }

        return null;
    }

    /**
     * get update (set of messages not yet received)
     */
    private EngineMessage getClientUpdate(ServerSideGame game, EngineMessage received)
    {
        int nID = received.getFromPlayerID();
        if (nID != EngineMessage.PLAYER_GROUP)
        {
            logger.warn("Get client update requires PLAYER_GROUP id.  Received instead: " + nID);
            return new EngineMessage(game.getGameID(), EngineMessage.PLAYER_SERVER,
                                     EngineMessage.CAT_EMPTY, (byte[]) null);
        }

        // get ids
        DMArrayList<Integer> ids = (DMArrayList<Integer>) received.getList(EngineMessage.PARAM_PLAYER_IDS);
        ApplicationError.assertNotNull(ids, "ID list null");
        int nNum = ids.size();
        ApplicationError.assertTrue(nNum > 0, "ID list empty");

        // get last timestamp for messages
        DMArrayList<Long> stamps = (DMArrayList<Long>) received.getList(EngineMessage.PARAM_LAST_TIMESTAMPS);
        ApplicationError.assertNotNull(stamps, "Timestamp list null");
        ApplicationError.assertTrue(stamps.size() == nNum, "Timestamp list not same size as id list");

        // create list of player queue files for each id (player) the client is
        // representing.  Remove messages already received
        File[] files = new File[nNum];
        int id;
        long timestamp;
        PlayerQueue queue;
        for (int i = 0; i < nNum; i++)
        {
            id = ids.get(i);
            timestamp = stamps.get(i);
            queue = game.getPlayerQueue(id);
            queue.removeMessagesUpTo(timestamp);
            queue.save();
            files[i] = queue.getFile();

            // WAR PATCH 2 - this should not be needed
            // since the file was saved above
            // BUG 42 - player polled, update timestamp
            // files[i].setLastModified(System.currentTimeMillis());
        }

        // get action needed to be performed
        ActionItem last = game.getLastActionItem();

        // create the message
        EngineMessage ret = new EngineMessage(game.getGameID(), EngineMessage.PLAYER_SERVER,
                                              EngineMessage.CAT_COMPOSITE_MESSAGE,
                                              files);
        if (last != null)
        {
            ret.setObject(EngineMessage.PARAM_ACTION, last);
        }

        // update settings if to control polling behavior
        updatePollSettings(ret);

        // BUG 42: insert array representing time each player last acted
        DMArrayList<Long> list = new DMArrayList<>();
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            list.add(game.getPlayerQueueFile(i).lastModified());
        }
        ret.setList(EngineMessage.PARAM_PLAYER_TIMESTAMPS, list);

        return ret;
    }

    /**
     * Return status of online games in list
     */
    private EngineMessage getOnlineStatus(EngineMessage received)
    {
        EngineMessage message;
        DMTypedHashMap status = new DMTypedHashMap();
        DMArrayList<String> ids = (DMArrayList<String>) received.getList(EngineMessage.PARAM_GAME_IDS);
        DMArrayList<String> passs = (DMArrayList<String>) received.getList(EngineMessage.PARAM_PASSWORDS);
        ApplicationError.assertNotNull(ids, "No ids to get status for");
        ApplicationError.assertNotNull(passs, "No passwords for status");

        int nNum = ids.size();
        String id, pass;

        // loop through each game and get status info
        for (int i = 0; i < nNum; i++)
        {
            id = ids.get(i);
            pass = passs.get(i);

            ObjectLock lock = GameConfigUtils.getGameLockingObject(id);
            try
            {
                synchronized (lock)
                {
                    // load game
                    ServerSideGame game = null;
                    ActionItem last;

                    try
                    {
                        game = ServerSideGame.loadServerSideGame(id, handler_);
                    }
                    catch (ApplicationError ae)
                    {
                        // rethrow if other than file not found (game not on server)
                        if (ae.getErrorCode() != ErrorCodes.ERROR_FILE_NOT_FOUND)
                        {
                            throw ae;
                        }
                    }

                    // if game not found, send back action item indicating

                    if (game == null)
                    {
                        last = getMissingGameAction();
                    }
                    else
                    {
                        // verify password
                        message = verifyPassword(game, EngineMessage.PLAYER_GROUP, pass, received);
                        if (message != null) return message;

                        // get last action as status
                        last = game.getLastActionItem();
                    }
                    status.setObject(id, last);
                }
            }
            catch (Exception t)
            {
                throw new ApplicationError(t);
            }
            // in finally block so this is always done
            finally
            {
                GameConfigUtils.removeGameLockingObject(lock);
            }
        }

        message = new EngineMessage(EngineMessage.GAME_NOTDEFINED, EngineMessage.PLAYER_SERVER, EngineMessage.CAT_STATUS);
        message.setObject(EngineMessage.PARAM_STATUS, status);
        return message;
    }

    /**
     * Validate that the given "join" message corresponds to an
     * actual online game.  Return null if it does not.
     */
    public EngineMessage joinOnlineGame(ServerSideGame game, EngineMessage message)
    {
        // get password, email and activation key from message
        String sJoinPassword = message.getString(EngineMessage.PARAM_PASSWORD);
        if (sJoinPassword == null) return getErrorMessage(game,
                                                          PropertyConfig.getLocalizedMessage("msg.joinfailed", message.getLocale()),
                                                          "Missing password " + message.getDebugInfo());

        String sJoinEmail = message.getString(EngineMessage.PARAM_EMAIL);
        if (sJoinEmail == null) return getErrorMessage(game,
                                                       PropertyConfig.getLocalizedMessage("msg.joinfailed", message.getLocale()),
                                                       "Missing email " + message.getDebugInfo());

        String sKey = message.getKey();
        if (sKey == null) return getErrorMessage(game,
                                                 PropertyConfig.getLocalizedMessage("msg.joinfailed", message.getLocale()),
                                                 "Missing key " + message.getDebugInfo());

        // now verify password
        String sEmail, sPassword;
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            sEmail = game.getEmailAtLower(i);
            sPassword = game.getPasswordAt(i);

            // if password and email match, we are cool
            if (sEmail.equalsIgnoreCase(sJoinEmail) &&
                sPassword.equalsIgnoreCase(sJoinPassword))
            {
                DMTypedHashMap keys = game.getKeys();
                DMTypedHashMap locales = game.getLocales();

                Iterator<String> iter = keys.keySet().iterator();
                String sExistEmail, sExistKey;
                while (iter.hasNext())
                {
                    sExistEmail = iter.next();
                    sExistKey = (String) keys.get(sExistEmail);
                    if (sExistEmail.equalsIgnoreCase(sJoinEmail))
                    {
                        return getErrorMessage(game,
                                               PropertyConfig.getLocalizedMessage("msg.joinagain", message.getLocale(), game.getGameID(), i),
                                               "Joining again (already joined): " + sJoinEmail + " for game " + game.getGameID());
                    }

                    if (!TESTING(EngineConstants.TESTING_SKIP_DUP_KEY_CHECK) && sExistKey.equals(sKey))
                    {
                        return getErrorMessage(game,
                                               PropertyConfig.getLocalizedMessage("msg.dupkey", message.getLocale(), game.getGameID(), sExistKey, sExistEmail),
                                               "Invalid join (duplicate key of player " + sExistEmail + "): " + message.getDebugInfo());
                    }
                }

                // store this key and locale, then save game
                keys.put(sEmail, sKey); // email is lowercase
                locales.put(sEmail, message.getLocale());
                game.save();

                // return game data
                return getGameData(game, message.getString(EngineMessage.PARAM_EMAIL));
            }
        }

        // if we got here, no match - bad password or email
        return getErrorMessage(game,
                               PropertyConfig.getLocalizedMessage("msg.joinfailed", message.getLocale()),
                               "Invalid join (bad password): " + message.getDebugInfo());
    }

    /**
     * Little function to return an error message and log something
     */
    private EngineMessage getErrorMessage(ServerSideGame game, String sMsg, String sLog)
    {
        logger.warn(sLog);
        EngineMessage ret = new EngineMessage(game != null ? game.getGameID() : EngineMessage.GAME_NOTDEFINED,
                                              EngineMessage.PLAYER_SERVER,
                                              EngineMessage.CAT_APPL_ERROR);

        ret.setApplicationErrorMessage(sMsg);
        return ret;
    }

    /**
     * Return game data to new game/join game.
     */
    private EngineMessage getGameData(ServerSideGame game, String sEmail)
    {
        // return game data file
        EngineMessage msg = new EngineMessage(game.getGameID(), EngineMessage.PLAYER_SERVER,
                                              EngineMessage.CAT_GAME_DATA, game.getGameDataFile());
        // include array list of armies this player is controlling
        DMArrayList<Integer> ids = new DMArrayList<>();
        int nNum = game.getNumPlayers();
        String sPass = null;
        for (int i = 0; i < nNum; i++)
        {
            if (sEmail.equalsIgnoreCase(game.getEmailAt(i)))
            {
                ids.add(i);
                sPass = game.getPasswordAt(i);
            }
        }
        msg.setList(EngineMessage.PARAM_PLAYER_IDS, ids);
        msg.setString(EngineMessage.PARAM_PASSWORD, sPass);
        return msg;
    }

    /**
     * Returns action used by client to indicate a
     * game is missing on server
     */
    protected abstract ActionItem getMissingGameAction();

    /**
     * Server query - used (in future) to route traffic to
     * different URLs and to specify wait times
     */
    private EngineMessage getServerQuery()
    {
        EngineMessage ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                              EngineMessage.PLAYER_NOTDEFINED,
                                              EngineMessage.CAT_SERVER_QUERY);
        // TODO: should we need to do load balancing, new server URLs
        // can be set here
        //ret.setString(EngineMessage.PARAM_URL, "http://games.donohoedigital.com:8764/war-aoi/servlet/");
        updatePollSettings(ret);
        return ret;
    }

    private void updatePollSettings(EngineMessage ret)
    {
        ret.setInteger(EngineMessage.PARAM_WAIT_MIN, 3);       // default is 3
        ret.setInteger(EngineMessage.PARAM_WAIT_ADD, 3);       // default is 2
        ret.setInteger(EngineMessage.PARAM_WAIT_ADD_PER, 120);   // default is 120
        ret.setInteger(EngineMessage.PARAM_WAIT_MAX, 180);       // default is 120
        ret.setInteger(EngineMessage.PARAM_WAIT_ERROR, 10);     // default is 10
    }

    // registration file constants
    public static final String REGDIR_PREFIX = "reg-";
    public static final String REGFILE_PREFIX = "usrreg.";
    public static final String REGLINE_PREFIX = "[--- ";

    /**
     * Process a user's registration
     */
    private synchronized EngineMessage processUserRegistration(HttpServletRequest request,
                                                               EngineMessage eng, boolean bBanAttempt)
    {
        // get a usable object
        RegistrationMessage msg = new RegistrationMessage(eng);
        Registration reg = new Registration();

        // determine type
        Registration.Type type = Registration.Type.REGISTRATION; // default
        if (msg.isPatch()) type = Registration.Type.PATCH; // 1.x use only
        else if (msg.isActivation()) type = Registration.Type.ACTIVATION;
        reg.setType(type);

        reg.setName(msg.getName());
        reg.setEmail(msg.getEmail());
        reg.setAddress(msg.getAddress());
        reg.setCity(msg.getCity());
        reg.setState(msg.getState());
        reg.setCountry(msg.getCountry());
        reg.setPostal(msg.getPostal());

        reg.setOperatingSystem(msg.getOS());
        reg.setJavaVersion(msg.getJava());
        reg.setLicenseKey(msg.getKey());
        reg.setVersion(msg.getVersion());

        reg.setServerTime(new Date());
        reg.setIp(request.getRemoteAddr());
        reg.setPort(request.getServerPort());
        reg.setHostName(Utils.getHostForIP(reg.getIp()));
        reg.setHostNameModified(Registration.generifyHostName(reg.getHostName()));
        reg.setBanAttempt(bBanAttempt);

        // testing errors
        if ("__ERROR__".equals(reg.getName()))
        {
            throw new ApplicationError("TESTING ERROR");
        }

        // save to database
        try
        {
            if (DatabaseManager.isInitialized())
            {
                registrationService.saveRegistration(reg);
            }
        }
        catch (Throwable t)
        {
            logger.error("Error saving registration to database: " + Utils.formatExceptionText(t));
        }

        // log to a file
        if (!bBanAttempt) logger.info("Registration: " + reg);
        writeUserRegistrationFile(reg);

        // send email if not a registration with a banned key
        if (!bBanAttempt)
        {
            sendEmail(reg.getEmail(), reg.getName(), msg.getLocale(), msg.getVersion());
        }

        // if ban attempt return null
        if (bBanAttempt) return null;

        // return OK
        return new EngineMessage(EngineMessage.GAME_NOTDEFINED, EngineMessage.PLAYER_NOTDEFINED, EngineMessage.CAT_OK);
    }

    /**
     * Write the given registration record to the log file
     */
    private void writeUserRegistrationFile(Registration reg)
    {
        Date now = reg.getServerTime();
        String sNow = formatter2_.format(now);

        File logdir = new File(new DefaultRuntimeDirectory().getServerHome(), "log");
        File regdir = new File(logdir, REGDIR_PREFIX + getServer().getAppName());
        ConfigUtils.verifyNewDirectory(regdir);
        File regfile = new File(regdir, REGFILE_PREFIX + formatter_.format(now) + ".log");
        FileOutputStream logout = ConfigUtils.getFileOutputStream(regfile, true);
        try
        {
            logout.write(Utils.encode(REGLINE_PREFIX + sNow + " ---]\n"));
            logout.write(Utils.encode(ToStringBuilder.reflectionToString(reg, RegistrationFileStringStyle.STYLE, false)));
            logout.write(Utils.encode("\n"));
        }
        catch (IOException io)
        {
            throw new ApplicationError(io);
        }
        finally
        {
            ConfigUtils.close(logout);
        }
    }

    /**
     * Send email to user who registered
     */
    private void sendEmail(String sTo, String sName, String sLocale, Version version)
    {
        if (sTo == null) return; // null for key verification so skip email

        // testing/debug
        if (TESTING(EngineConstants.TESTING_SKIP_EMAIL))
        {
            logger.info("SKIP EMAIL to " + sTo);
            return;
        }

        // create and run jsp
        JspEmail email = new JspEmail("reg", sLocale, null);
        email.getSession().setAttribute(EMAIL_PARAM_NAME, sName);
        email.getSession().setAttribute(EMAIL_PARAM_VERSION, version);
        email.executeJSP();

        // get results and send email
        postalService.sendMail(sTo, PropertyConfig.getRequiredStringProperty("settings.server.regfrom"),
                               null, email.getSubject(),
                               email.getPlain(), email.getHtml(),
                               null, null);
    }

    private static final SimpleDateFormat formatter_ = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    // no need to localize, this is for DD eyes only
    private static final SimpleDateFormat formatter2_ = new SimpleDateFormat("MMMMM dd, yyyy 'at' HH:mm:ss", Locale.US);

    ///
    /// P2P additions (generic)
    ///

    /**
     * Return the IP the request is coming from (public IP of the user)
     */
    private EngineMessage getPublicIP(HttpServletRequest request)
    {
        EngineMessage ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                              EngineMessage.PLAYER_SERVER,
                                              EngineMessage.CAT_PUBLIC_IP);
        ret.setString(EngineMessage.PARAM_IP, request.getRemoteAddr());
        return ret;
    }

    //
    // DD Message
    //

    private static final Object MSG_FILE_SYNC = new Object();
    private long lastUpdate = 0;
    private long lastUpgradeUpdate = 0;
    private String ddMessage = null;
    private String upMessage = null;

    /**
     * Return the current dd message if not seen by user
     */
    private EngineMessage checkDDmsg(DDMessage ddreceived)
    {
        EngineMessage ret = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                              EngineMessage.PLAYER_SERVER,
                                              EngineMessage.CAT_CHECK_DDMSG);

        String os = ddreceived.getString(EngineMessage.PARAM_PATCH_OS);
        long last = ddreceived.getLong(EngineMessage.PARAM_DDMSG_ID, 0);
        String player = ddreceived.getString(EngineMessage.PARAM_DDPROFILE);
        Version version = ddreceived.getVersion();

        logger.info("Message check from " + player + " (" + ddreceived.getKey() + ") on version " +
                    version + " for " + os + " (" + last + " last check)");

        // if no version available, always send upgrade message back
        if (version.isBefore(getLatestClientVersion(os)))
        {
            // if message file changed, load it
            synchronized (MSG_FILE_SYNC)
            {
                if (upgradeFile.lastModified() > lastUpgradeUpdate)
                {
                    lastUpgradeUpdate = upgradeFile.lastModified();
                    upMessage = ConfigUtils.readFile(upgradeFile);
                }
            }

            ret.setString(EngineMessage.PARAM_DDMSG, upMessage);
            ret.setLong(EngineMessage.PARAM_DDMSG_ID, last);
        }
        else
        {

            // if message file changed, load it
            synchronized (MSG_FILE_SYNC)
            {
                if (messageFile.lastModified() > lastUpdate)
                {
                    lastUpdate = messageFile.lastModified();
                    ddMessage = ConfigUtils.readFile(messageFile);
                }
            }

            if (last < lastUpdate)
            {
                ret.setString(EngineMessage.PARAM_DDMSG, ddMessage);
                ret.setLong(EngineMessage.PARAM_DDMSG_ID, lastUpdate);
            }
        }

        return ret;
    }
}
