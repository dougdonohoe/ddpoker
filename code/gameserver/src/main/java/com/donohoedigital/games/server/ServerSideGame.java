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
 * ServerSideGame.java
 *
 * Created on March 8, 2003, 8:58 AM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.jsp.*;
import com.donohoedigital.mail.*;
import org.apache.logging.log4j.*;
import org.springframework.beans.factory.annotation.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * @author donohoe
 */
public class ServerSideGame extends ServerDataFile implements GameInfo
{
    static Logger logger = LogManager.getLogger(ServerSideGame.class);


    // FIX: this file is from WarAOI and probably should be deleted.  Added this postalservice so it will compile
    @Autowired
    protected DDPostalService postalService;

    // constants
    public static final String GAME_EXT = "gam";       // state for online engine

    // TODO: different from/reply to than host?
    //public static final String EMAIL_FROM = PropertyConfig.getRequiredStringProperty("settings.email.from");
    //public static final String EMAIL_REPLYTO = PropertyConfig.getRequiredStringProperty("settings.email.replyto");

    // JSP params
    public static final String PARAM_INDEX = "idx";
    public static final String PARAM_EMAIL_TEXT = "txt";

    // instance data
    private String sAppName_;
    private int nTurn_;
    private boolean bDone_;
    private String gameID_;         // game id (based on path/file name)
    private int nNumPlayers_;       // number of players in the game
    private DMArrayList order_;     // ids, in order of play
    private DMArrayList names_;     // name of each player
    private DMArrayList emails_;    // email address for each player
    private DMArrayList pass_;      // player passwords
    private DMTypedHashMap keys_;   // activation keys (indexed by email)
    private DMTypedHashMap locales_;// locale per player (indexed by email)
    private DMArrayList colors_;    // army color
    private DMArrayList actionList_;  // list of items we are waiting on
    private DMArrayList elim_;      // whether player is eliminated or not
    private DMTypedHashMap options_; // game options
    private ActionHandler handler_; // handler to get actions
    private DMTypedHashMap timestamps_; // activation key mapped to time stamp of last message processed

    /**
     * Create new online game from the message
     */
    public static ServerSideGame newServerSideGame(EngineMessage message, ActionHandler handler)
    {
        return new ServerSideGame(message, handler);
    }

    /**
     * Create a new online game object by loading an existing one
     * given the info in the message
     */
    public static ServerSideGame loadServerSideGame(EngineMessage message, ActionHandler handler)
    {
        ServerSideGame game = new ServerSideGame(handler);
        game.loadFromMessage(message);
        return game;
    }

    /**
     * Create a new online game object by loading an existing one
     * given the info in the message
     */
    public static ServerSideGame loadServerSideGame(String id, ActionHandler handler)
    {
        ServerSideGame game = new ServerSideGame(handler);
        game.loadFromID(id);
        return game;
    }

    /**
     * Create a new online game object by loading an existing one
     * given a game file
     */
    public static ServerSideGame loadServerSideGame(File file, ActionHandler handler)
    {
        ServerSideGame game = new ServerSideGame(handler);
        game.loadFromFile(file);
        return game;
    }

    /**
     * Create empty game
     */
    private ServerSideGame(ActionHandler handler)
    {
        super();
        handler_ = handler;
    }

    /**
     * Creates a new instance of ServerSideGame
     */
    private ServerSideGame(EngineMessage message, ActionHandler handler)
    {
        this(handler);

        // turn
        nTurn_ = 1;
        bDone_ = false;

        // appname
        sAppName_ = ConfigManager.getAppName();

        // number of players
        Integer nNum = message.getInteger(EngineMessage.PARAM_NUM_PLAYERS);
        ApplicationError.assertNotNull(nNum, "Number of players not defined");
        nNumPlayers_ = nNum;

        // their names
        names_ = (DMArrayList) message.getList(EngineMessage.PARAM_NAMES);
        ApplicationError.assertNotNull(names_, "Player names not defined");

        // their army colors - TODO: this is specific to War!
        colors_ = (DMArrayList) message.getList(EngineMessage.PARAM_COLORS);
        ApplicationError.assertNotNull(colors_, "Player colors not defined");

        // their email addresses
        emails_ = (DMArrayList) message.getList(EngineMessage.PARAM_EMAIL_ADDRS);
        ApplicationError.assertNotNull(emails_, "Player email addresses not defined");

        // game options
        options_ = (DMTypedHashMap) message.getObject(EngineMessage.PARAM_GAME_OPTIONS);
        ApplicationError.assertNotNull(options_, "Game options not defined");

        // key for host
        String sKey = message.getKey();

        // order (initially order is same as id sequence)
        order_ = new DMArrayList(nNumPlayers_);
        elim_ = new DMArrayList(nNumPlayers_);
        for (int i = 0; i < nNumPlayers_; i++)
        {
            order_.add(i);
            elim_.add(Boolean.FALSE);
        }

        // new wait list and passwords and keys
        actionList_ = new DMArrayList();
        pass_ = new DMArrayList(nNumPlayers_);
        keys_ = new DMTypedHashMap();
        timestamps_ = new DMTypedHashMap();
        locales_ = new DMTypedHashMap();

        // email message from host
        String sMsg = message.getString(EngineMessage.PARAM_EMAIL_TEXT);

        // create dir_
        Date date = new Date();
        SimpleDateFormat dirformatter = new SimpleDateFormat("yyyy/MM/dd/", Locale.US);
        SimpleDateFormat idformatter = new SimpleDateFormat("yyyyMMdd", Locale.US);

        dir_ = new File(SAVE_DIR, dirformatter.format(date));

        // sync on parent static SAVE_DIR in case multiple games created at once
        // ServerSideGame is the master file, so it gets created first.  We
        // basically look for the next same number and immediately create
        // an empty file to "reserve" that number.  We leave the sync
        // block right after this so others waiting can move on and get
        // the next save file
        //
        // TODO:  if we move to multiple JVM instances, we can synchronize
        //        using FileLock
        synchronized (SAVE_DIR)
        {
            // verify directory exists, if not create it
            ConfigUtils.verifyNewDirectory(dir_);
            int nFileNum = getNextSaveNumber(dir_, GAME_EXT);
            sFileNum_ = fNum_.form(nFileNum);
            file_ = createFile(dir_, BEGIN, GAME_EXT);
            ConfigUtils.verifyNewFile(file_);
        }

        // create id
        gameID_ = idformatter.format(date) + sFileNum_;
        logger.info("NEW GAMEID " + gameID_);

        // create PlayerQueue entries and passwords
        //
        // NOTE: player ids assume to start at 0, matching what
        // the client does (e.g., in War, look at NewGame)
        MersenneTwisterFast random = new MersenneTwisterFast();
        PlayerQueue q;
        HashMap map = new HashMap();
        String sEmail;
        String sPass;
        for (int i = 0; i < nNumPlayers_; i++)
        {
            // create queue
            q = PlayerQueue.newPlayerQueue(dir_, sFileNum_, i);

            // get email
            sEmail = getEmailAtLower(i);

            // store activation key
            if (i == 0)
            {
                keys_.put(sEmail, sKey); // email is lowercase                
                logger.info("  Key for host " + sEmail + " is " + sKey);
            }

            // set locale for all players to host
            // this can change when joining
            locales_.put(sEmail, message.getLocale());

            // create password - each unique email has its
            // own password (case:  one player is playing
            // multiple armies)
            sPass = (String) map.get(sEmail);
            if (sPass == null)
            {
                sPass = createPassword(random, i);
                map.put(sEmail, sPass);
            }
            logger.info("  Password for " + sEmail + "=" + sPass);
            pass_.add(sPass);
        }

        // get 1st action
        ActionItem first = handler_.getNextActionItem(this, null);
        addActionItem(first);

        // save our contents out
        save();

        // save game data
        GameData data = new GameData(dir_, sFileNum_, message.getDataAsString());

        // send invite to all players
        if (!TESTING(EngineConstants.TESTING_SKIP_EMAIL))
        {
            sendInvite(sMsg, message.getLocale());
        }
    }

    /**
     * Return options for game
     */
    public DMTypedHashMap getGameOptions()
    {
        return options_;
    }

    /**
     * Return hash of emails to activation key
     */
    public DMTypedHashMap getKeys()
    {
        return keys_;
    }

    /**
     * Return hash of emails to locale
     */
    public DMTypedHashMap getLocales()
    {
        return locales_;
    }

    /**
     * Change order of array so given player is at index 0
     */
    public void makePlayerFirst(int id)
    {
        int move;
        while ((move = getPlayerIdAt(0)) != id)
        {
            order_.add(order_.remove(0));
        }
    }

    /**
     * Return the player id at the given index of the order array
     */
    public int getPlayerIdAt(int i)
    {
        return (Integer) order_.get(i);
    }

    /**
     * Add an action item to the list
     */
    public void addActionItem(ActionItem item)
    {
        if (item == null) return;
        item.setRemindEmailSent(false); // make sure this is false in case of re-used action
        actionList_.add(item);
    }

    /**
     * Get last action item in list
     */
    public ActionItem getLastActionItem()
    {
        if (actionList_.size() == 0) return null;
        return (ActionItem) actionList_.get(actionList_.size() - 1);
    }

    /**
     * Process an action request (online game spawns a specific action)
     */
    public void processActionRequest(EngineMessage message)
    {
        ActionItem next = handler_.processActionRequest(this, message);
        addActionItem(next);
    }

    /**
     * Process a player update message
     */
    public void processPlayerUpdate(EngineMessage message)
    {
        int id = message.getFromPlayerID();

        Boolean elim = message.getBoolean(EngineMessage.PARAM_ELIMINATED);
        boolean bEvict = message.getBoolean(EngineMessage.PARAM_EVICTED, false);

        if (elim != null)
        {
            elim_.set(id, elim);

            // BUG 268 - allow host to evict a player
            // remove that player from the action required list
            if (elim && bEvict)
            {
                ActionItem item = null;

                // set acted on all active items
                for (int i = 0; i < actionList_.size(); i++)
                {
                    item = (ActionItem) actionList_.get(i);

                    if (item.isPlayerActionRequired(id) &&
                        !item.hasPlayerActed(id))
                    {
                        item.setPlayerActed(id);

                        // WAR PATCH 2 - set an evict flag on this item in case it is needed
                        item.setBoolean(EngineMessage.PARAM_EVICTED, Boolean.TRUE);
                    }
                }

                // check done last item
                if (item != null)
                {
                    checkDone(item, true);
                }
            }
        }

        String email = message.getString(EngineMessage.PARAM_EMAIL);
        if (email != null)
        {
            emails_.set(id, email);
        }
    }

    /**
     * process the done action
     */
    public void processActionDone(EngineMessage message)
    {
        // see if the done flag is sent down
        Boolean bDone = message.getBoolean(EngineMessage.PARAM_GAME_OVER);
        if (bDone != null && bDone)
        {
            bDone_ = true;
        }

        ActionItem item = getLastActionItem();
        Integer id = message.getInteger(EngineMessage.PARAM_ACTION);
        ApplicationError.assertNotNull(id, "Action id missing");

        // make sure this action is one we are expecting
        // BUG 199 - if previous action confirmation, id is different, so ignore
        if (item.getActionID() != id)
        {
            logger.warn(gameID_ + ": Processing action " + id + " but item at end of list is " + item.getActionID());
            return;
        }

        int playerid = message.getFromPlayerID();

        // if marking done for a group, loop through each id
        if (playerid == EngineMessage.PLAYER_GROUP)
        {
            DMArrayList ids = (DMArrayList) message.getList(EngineMessage.PARAM_PLAYER_IDS);
            ApplicationError.assertNotNull(ids, "Player ids missing");

            int nNumIDs = ids.size();
            int pid;
            for (int i = 0; i < nNumIDs; i++)
            {
                pid = (Integer) ids.get(i);
                // BUG 199 - if duplicate action confirmation, ignore
                // BUG 268 - only warn if the player is not eliminated
                if (!item.setPlayerActed(pid) && !isEliminated(pid))
                {
                    logger.warn(gameID_ + " ignoring invalid group setPlayerActed() call, id=" + pid);
                    return;
                }
            }
        }
        // else mark individual id sent down
        else if (playerid >= 0 && playerid < nNumPlayers_)
        {
            // BUG 199 - if duplicate action confirmation, ignore
            if (!item.setPlayerActed(playerid))
            {
                logger.warn(gameID_ + " ignoring invalid individual setPlayerActed() call, id=" + playerid);
                return;
            }
        }
        else
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR,
                                       "Processing action " + id + " with invalid player id " + playerid, null);
        }

        // look for data in action
        checkActionData(item, message);

        // give handler a chance to do something with done message
        handler_.processActionDone(this, item, message);

        checkDone(item, true);
    }

    /**
     * Check to see if an action item is done, if so, get next one
     */
    private void checkDone(ActionItem item, boolean bCheckEmpty)
    {
        // if done, remove item and get next one
        if (item.isDone())
        {
            actionList_.remove(item);
            ActionItem next = handler_.getNextActionItem(this, item);
            if (next != null)
            {
                addActionItem(next);
            }
            else if (bCheckEmpty)
            {
                // no next item means we are falling through to item at 
                // top of stack - see if we are done with that item due to
                // eviction
                item = getLastActionItem();
                if (item != null) checkDone(item, false);
            }
        }
    }

    /**
     * See if data in action update - if so, it is a game update, so
     * send it on to the other players (skip those in player id list)
     */
    private void checkActionData(ActionItem action, EngineMessage message)
    {
        // if we have data, it is a game update.
        byte[] data = message.getData();
        if (data == null) return;

        // get update type
        Integer nUpdateType = message.getInteger(EngineMessage.PARAM_UPDATE_TYPE);
        ApplicationError.assertNotNull(nUpdateType, "No update type defined");

        // get from player id
        int playerid = message.getFromPlayerID();

        // make sure we have a player id
        if (playerid < 0 || playerid >= nNumPlayers_)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR,
                                       "Processing action " + action.getActionID() + " with invalid player id " + playerid, null);
        }

        // Update all ids not controlled by same player (usually only one,
        // but in case of multiple armies, the other ones controlled by that player already
        // know, so they don't need the update)
        String sPass = getPasswordAt(playerid); // password of player sending update
        EngineMessage update = new EngineMessage(gameID_, playerid, EngineMessage.CAT_GAME_UPDATE, data);
        update.setInteger(EngineMessage.PARAM_UPDATE_TYPE, nUpdateType);

        // add message to appropriate queues
        File file;
        long lasttime;
        for (int i = 0; i < nNumPlayers_; i++)
        {
            // skip if password same (password is same for all players
            // undercontrol of the same person)
            if (getPasswordAt(i).equals(sPass)) continue;

            PlayerQueue queue = getPlayerQueue(i);

            // WAR PATCH 2 
            // preserve last modified time stamp so
            // "last online" in client is correct
            file = queue.getFile();
            lasttime = file.lastModified();
            queue.addMessage(update);
            queue.save();
            file.setLastModified(lasttime);
        }
    }

    /**
     * Return a new player queue for the given index
     * Returns a new object every time, so cache return for perf
     */
    public PlayerQueue getPlayerQueue(int i)
    {
        ApplicationError.assertTrue(i >= 0 && i < nNumPlayers_, "Index out of bounds");
        return PlayerQueue.loadPlayerQueue(dir_, sFileNum_, i);
    }

    /**
     * Return file representing player queue
     */
    public File getPlayerQueueFile(int i)
    {
        ApplicationError.assertTrue(i >= 0 && i < nNumPlayers_, "Index out of bounds");
        return PlayerQueue.getPlayerQueueFile(dir_, sFileNum_, i, false);
    }

    /**
     * Return file representing player queue
     */
    public File getGameDataFile()
    {
        return GameData.getGameDataFile(dir_, sFileNum_, true);
    }

    /**
     * init this by looking for file based on information in given message
     */
    private void loadFromMessage(EngineMessage message)
    {
        // gameid
        String gameID = message.getGameID();
        ApplicationError.assertNotNull(gameID, "GameID missing from message");

        loadFromID(gameID);
    }

    /**
     * Load given game id
     */
    private void loadFromID(String gameID)
    {
        gameID_ = gameID;

        // appname
        sAppName_ = ConfigManager.getAppName();

        // get dir, file and num from gameid
        String sDir = gameID_.substring(0, 4) + "/" + gameID_.substring(4, 6) + "/" + gameID_.substring(6, 8);
        sFileNum_ = gameID_.substring(8);
        dir_ = new File(SAVE_DIR, sDir);
        ConfigUtils.verifyDirectory(dir_);
        file_ = createFile(dir_, BEGIN, GAME_EXT);
        ConfigUtils.verifyFile(file_);

        // load file info
        load();
    }

    /**
     * Load given the file - must be same as above
     */
    private void loadFromFile(File file)
    {
        // appname
        sAppName_ = ConfigManager.getAppName();

        // our file is the one passed in
        file_ = file;

        // our location is the files parent
        dir_ = file.getParentFile();

        // construct the file num and game id from the file/path info
        sFileNum_ = file.getName().substring(5, 10);
        File month = dir_.getParentFile();
        File year = month.getParentFile();
        gameID_ = year.getName() + month.getName() + dir_.getName() + sFileNum_;

        // load file info
        load();
    }

    /**
     * Is game over?
     */
    public boolean isGameOver()
    {
        return bDone_;
    }

    /**
     * Get whether player is eliminated
     */
    public boolean isEliminated(int id)
    {
        return (Boolean) elim_.get(id);
    }

    /**
     * Get name at
     */
    public String getNameAt(int i)
    {
        return (String) names_.get(i);
    }

    /**
     * Get email at
     */
    public String getEmailAt(int i)
    {
        return (String) emails_.get(i);
    }

    /**
     * Get email at, all lowercase
     */
    public String getEmailAtLower(int i)
    {
        return ((String) emails_.get(i)).toLowerCase();
    }

    /**
     * Get locale at
     */
    public String getLocaleAt(int i)
    {
        return (String) locales_.get(getEmailAtLower(i));
    }

    /**
     * Get passoword at
     */
    public String getPasswordAt(int i)
    {
        return (String) pass_.get(i);
    }

    /**
     * Get color at
     */
    public String getColorAt(int i)
    {
        return (String) colors_.get(i);
    }

    /**
     * Get number of players
     */
    public int getNumPlayers()
    {
        return nNumPlayers_;
    }

    /**
     * Get game id
     */
    public String getGameID()
    {
        return gameID_;
    }

    /**
     * Get current turn #
     */
    public int getTurn()
    {
        return nTurn_;
    }

    /**
     * Set turn #
     */
    public void setTurn(int n)
    {
        nTurn_ = n;
    }

    /**
     * Get last timestamp for given key
     */
    public long getLastTimeStamp(String sKey)
    {
        return timestamps_.getLong(sKey, 0);
    }

    /**
     * Set last timestamp for given key
     */
    public void setLastTimeStamp(String sKey, Long lng)
    {
        timestamps_.setLong(sKey, lng);
    }

    /**
     * Get last seq-id for given key.   If no seq id previously
     * set, -1 is returned
     */
    public long getLastSeqID(String sKey)
    {
        // We store in timestamps_ hash
        // for backwards compatability (added 2/9/04)
        return timestamps_.getLong("seq-" + sKey, -1);
    }

    /**
     * Set last timestamp for given key
     */
    public void setLastSeqID(String sKey, long lng)
    {
        timestamps_.setLong("seq-" + sKey, lng);
    }

    // may change mind in future about saving last poll in
    // this file, but avoid it for now because I don't want to
    // save this file on each poll (from EngineServlet)
    // maybe change if we go to a cached model where we don't load/save
    // this each time (e.g., keep around some if active)

//    /**
//     * Get last poll timestamp for given player id.   If no seq id previously
//     * set, -1 is returned
//     */
//    public long getLastPoll(int id)
//    {
//        // We store in timestamps_ hash
//        // for backwards compatability (added 3/5/04)
//        String sEmail = getEmailAtLower(id);
//        return timestamps_.getLong("poll-"+sEmail, -1);
//    }
//    
//    /**
//     * Set last poll timestamp for given player id
//     */
//    public void setLastPoll(int id, long lng)
//    {
//        String sEmail = getEmailAtLower(id);
//        timestamps_.setLong("poll-"+sEmail, new Long(lng));
//    }


    /**
     * Send an invite to each player
     */
    private void sendInvite(String sMsg, String sLocale)
    {
        // invite new players
        HashMap map = new HashMap();
        String sEmail;
        Object exist;
        for (int i = 0; i < nNumPlayers_; i++)
        {
            // only send one message per email
            sEmail = getEmailAtLower(i);
            exist = map.get(sEmail);
            if (exist == null)
            {
                sendEmail("invite", sLocale, i, sMsg);
                map.put(sEmail, sEmail);
            }
        }
    }

    /**
     * Send email with the given name to player i, using params for getMessage()
     */
    private void sendEmail(String sEmailID, String sLocale, int i, String sMsg)
    {
        // create and run jsp
        JspEmail email = new JspEmail(sEmailID, sLocale, this);
        email.getSession().setAttribute(PARAM_INDEX, i);
        email.getSession().setAttribute(PARAM_EMAIL_TEXT, sMsg);
        email.executeJSP();

        // attachment with message containing game info
        DDAttachment attach = null;
        if (i != 0) // skip attachment for host
        {
            EngineMessage msg = new EngineMessage(gameID_, EngineMessage.PLAYER_SERVER, EngineMessage.CAT_EMAIL_JOIN_GAME);
            msg.setString(EngineMessage.PARAM_PASSWORD, getPasswordAt(i));
            msg.setString(EngineMessage.PARAM_EMAIL, getEmailAt(i));
            attach = new DDAttachment(handler_.getInviteAttachmentName(), msg.marshal(null),
                                      handler_.getInviteAttachmentMime());
        }

        // error info
        EngineMailErrorInfo info = new EngineMailErrorInfo(sAppName_,
                                                           gameID_, i, sEmailID);

        // get results and send email
        String sFrom = (String) emails_.get(0); // from the host
        String sTo = (String) emails_.get(i);

        // TODO: send from server w/ bounce handling? DDMail.sendMail(sTo, EMAIL_FROM, EMAIL_REPLYTO, email.getSubject(), 
        postalService.sendMail(sTo, sFrom, null, email.getSubject(),
                               email.getPlain(), email.getHtml(),
                               attach, info);
    }

    /**
     * Create a password for use in verifying invitations
     */
    private String createPassword(MersenneTwisterFast random, int id)
    {
        int passwordid = id + 1; // add one to avoid 0

        StringBuilder sbPass = new StringBuilder();

        // first number is user id + 1 (used in client for naming reasons)
        sbPass.append(passwordid);
        sbPass.append("-");
        int num;

        // password format:  id-AAA-123

        // do letters, skipping letter O (to avoid confusing with number 0)
        for (int i = 0; i < 3; i++)
        {
            do
            {
                num = random.nextInt(26);
            }
            while (num == 14); // no letter 0
            sbPass.append((char) (65 + num));
        }

        sbPass.append("-");

        // do numbers, skipping number 0
        for (int i = 0; i < 3; i++)
        {
            num = random.nextInt(9) + 1; // no zeroes
            sbPass.append(num);
        }

        return sbPass.toString();
    }

    ////
    //// Save Files
    ////

    /**
     * Get next save file number given the list of existing files.
     * Assumes the files are in sorted order, with last one being
     * highest number
     */
    private int getNextSaveNumber(File dir, String sExt)
    {
        File files[] = Utils.getFileList(dir, DELIM + sExt, null);
        int nNum = 1;
        if (files != null && files.length > 0)
        {
            File last = files[files.length - 1];
            int nValue = getFileNumber(last);
            nNum = nValue + 1;
        }

        return nNum;
    }

    /**
     * Write contents out
     */
    public void write(Writer writer) throws IOException
    {
        // scalar values
        TokenizedList list = new TokenizedList();
        list.addToken(nNumPlayers_);
        list.addToken(nTurn_);
        list.addToken(bDone_);
        writer.write(list.marshal(null));
        writeEndEntry(writer);

        // game options
        writer.write(options_.marshal(null));
        writeEndEntry(writer);

        // names
        writer.write(names_.marshal(null));
        writeEndEntry(writer);

        // passwords
        writer.write(pass_.marshal(null));
        writeEndEntry(writer);

        // keys
        writer.write(keys_.marshal(null));
        writeEndEntry(writer);

        // emails
        writer.write(emails_.marshal(null));
        writeEndEntry(writer);

        // colors
        writer.write(colors_.marshal(null));
        writeEndEntry(writer);

        // order
        writer.write(order_.marshal(null));
        writeEndEntry(writer);

        // eliminated
        writer.write(elim_.marshal(null));
        writeEndEntry(writer);

        // wait items
        writer.write(actionList_.marshal(null));
        writeEndEntry(writer);

        // keys to timestamps
        writer.write(timestamps_.marshal(null));
        writeEndEntry(writer);

        // keys to locales
        writer.write(locales_.marshal(null));
        writeEndEntry(writer);
    }

    /**
     * Read our data from given reader
     */
    public void read(Reader reader, boolean bFull) throws IOException
    {
        // scalar values
        BufferedReader buf = new BufferedReader(reader);
        TokenizedList list = readTokenizedList(buf);
        nNumPlayers_ = list.removeIntToken();
        nTurn_ = list.removeIntToken();
        bDone_ = list.removeBooleanToken();

        // options
        options_ = new DMTypedHashMap();
        options_.demarshal(null, buf.readLine());

        // names
        names_ = new DMArrayList(nNumPlayers_);
        names_.demarshal(null, buf.readLine());

        // passwords
        pass_ = new DMArrayList(nNumPlayers_);
        pass_.demarshal(null, buf.readLine());

        // keys
        keys_ = new DMTypedHashMap();
        keys_.demarshal(null, buf.readLine());

        // emails
        emails_ = new DMArrayList(nNumPlayers_);
        emails_.demarshal(null, buf.readLine());

        // colors
        colors_ = new DMArrayList(nNumPlayers_);
        colors_.demarshal(null, buf.readLine());

        // order
        order_ = new DMArrayList(nNumPlayers_);
        order_.demarshal(null, buf.readLine());

        // eliminated
        elim_ = new DMArrayList(nNumPlayers_);
        elim_.demarshal(null, buf.readLine());

        // wait items
        actionList_ = new DMArrayList();
        actionList_.demarshal(null, buf.readLine());

        // BUG 199 - msgids (added for Patch 1, so need to optionally load)
        timestamps_ = new DMTypedHashMap();
        String line = buf.readLine();
        if (line != null) timestamps_.demarshal(null, line);

        // WAR FRENCH - locales (added for Patch 2, so need to optionally load)
        locales_ = new DMTypedHashMap();
        line = buf.readLine();
        if (line != null) locales_.demarshal(null, line);
    }
}
