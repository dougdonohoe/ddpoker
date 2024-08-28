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
 * Poker.java
 *
 * Created on December 7, 2003, 8:42 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.p2p.*;
import com.donohoedigital.udp.*;
import org.apache.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * @author Doug Donohoe
 */
public class PokerMain extends GameEngine implements Peer2PeerControllerInterface, LanControllerInterface,
                                                     UDPLinkHandler, UDPManagerMonitor, UDPLinkMonitor
{
    private static final Logger logger = Logger.getLogger(GameEngine.class);

    private String sFileParam_ = null;
    private final boolean bLoadNames;

    static {
        // forget why I set this
        System.setProperty("sun.java2d.noddraw", "true");

        // Mac: Menu Name
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "DD Poker"); // TODO + version?
        System.setProperty("apple.awt.application.name", "DD Poker"); // TODO + version?

        // avoid java.lang.NullPointerException
        //	at javax.swing.plaf.metal.MetalSliderUI.installUI(MetalSliderUI.java:110)
        System.setProperty("swing.defaultlaf", "javax.swing.plaf.metal.MetalLookAndFeel");
    }

    /**
     * Run Poker
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public static void main(String[] args)
    {
        try
        {
            PokerMain main = new PokerMain("poker", "poker", args);
            main.init();
        }
        catch (ApplicationError ae)
        {
            System.err.println("Poker ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }
        catch (OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
    }

    /**
     * Get profile override
     */
    String getProfileOverride()
    {
        return getCommandLineOptions().getString("profile", null);
    }

    /**
     * cast getGameEngine to PokerMain
     */
    public static PokerMain getPokerMain()
    {
        return (PokerMain) GameEngine.getGameEngine();
    }

    /**
     * Create Poker from config file
     */
    public PokerMain(String sConfigName, String sMainModule, String[] args)
            throws ApplicationError
    {
        this(sConfigName, sMainModule, args, false, true);
    }

    /**
     * Create Poker from config file
     */
    public PokerMain(String sConfigName, String sMainModule, String[] args, boolean bHeadless, boolean bLoadNames)
            throws ApplicationError
    {
        super(sConfigName, sMainModule, "" + PokerConstants.VERSION.getMajor(), args, bHeadless);
        this.bLoadNames = bLoadNames;
    }

    /**
     * init
     */
    @Override
    public void init()
    {
        super.init();
        if (bCheckFailed_) return;

        // init
        GameState.setDelegate(new PokerGameStateDelegate());

        // get args
        String[] otherargs = CommandLine.getRemainingArgs();
        if (otherargs != null && otherargs.length > 0)
        {
            sFileParam_ = otherargs[0];
        }

        // load names for computer players
        if (bLoadNames) loadNames();

        // show main window
        if (!bHeadless_) initMainWindow();

        // register our custom tag display classes
        DDHtmlEditorKit.registerTagViewClass("ddcard", DDCardView.class);
        DDHtmlEditorKit.registerTagViewClass("ddhandgroup", DDHandGroupView.class);
    }

    /**
     * copy any v2 files save files
     */
    @Override
    protected void copySaveFiles()
    {
        // get standard files
        super.copySaveFiles();

        // copy v2 changes
        File userSaveV3 = GameConfigUtils.getSaveDir();
        File userSaveV2 = new File(userSaveV3.getAbsolutePath().replaceAll("poker3", "poker2"));
        if (userSaveV2.exists() && userSaveV2.isDirectory())
        {
            ConfigUtils.copyDir(userSaveV2, userSaveV3, new UpgradeFilter());
        }

    }

    /**
     * Filter for upgrade to skip legacy dirs and database files
     */
    private static class UpgradeFilter implements FilenameFilter
    {

        public boolean accept(File dir, String name)
        {
            // skip these files
            if (name.startsWith(".") ||
                name.equals("db") ||
                name.equals("advisors") ||
                name.equals("preflopstrategies"))
            {
                return false;
            }
            return true;
        }
    }


    /**
     * debug options
     */
    @Override
    protected void setupApplicationCommandLineOptions()
    {
        super.setupApplicationCommandLineOptions();
        CommandLine.addStringOption("profile", null);
    }

    /**
     * Get version
     */
    @Override
    public Version getVersion()
    {
        return PokerConstants.VERSION;
    }

    /**
     * What do the license keys start with?
     */
    @Override
    protected int getKeyStart()
    {
        return PokerConstants.getKeyStart();
    }

    /**
     * See if we can load the default profile.  If not, another
     * copy of the game is already in use
     */
    @Override
    protected boolean checkPreReq()
    {
        // skip on init if no key
        if (bHeadless_ || getRealLicenseKey() == null || isActivationNeeded()) return true;

        // in dev, allow one failure, then wait 3 seconds in case
        // we just killed and restarted right away
        if (DebugConfig.isTestingOn())
        {
            try
            {
                PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
                if (profile != null) profile.testDB();
                return true;
            }
            catch (Throwable t)
            {
                // FIX: if database driver is missing, this fails silently.
                // This code is kind of a mess.  Log message for now
                logger.warn(Utils.formatExceptionText(t));
            }

            // if first attempt failed, try again after 3 seconds to see
            // if lock clears
            Utils.sleepMillis(PokerConstants.PROFILE_RETRY_MILLIS);
        }

        try
        {
            PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
            if (profile != null) profile.testDB();
        }
        catch (ApplicationError ae)
        {
            Throwable source = ae.getException();
            if (source != null)
            {
                String sMessage = source.getMessage();
                if (source instanceof SQLException &&
                    sMessage != null && (
                        sMessage.contains("database is already in use") ||
                        sMessage.contains("File input/output error")))
                {
                    logger.warn("Another copy running (database already in use).  Showing warning splash.");
                    String sMsg = PropertyConfig.getMessage("msg.2ndcopy");
                    splashscreen_.changeUI(this, true, sMsg);
                    return false;
                }
            }
            throw ae;
        }
        return true;
    }

    /**
     * initial splash file name
     */
    @Override
    protected String getSplashBackgroundFile()
    {
        return "poker-splash-nochoice.jpg";
    }

    /**
     * initial splash file name
     */
    @Override
    protected String getSplashIconFile()
    {
        return "pokericon32.gif";
    }

    /**
     * initial splash title
     */
    @Override
    protected String getSplashTitle()
    {
        return "DD Poker";
    }

    @Override
    protected boolean isAutoGenLicenseKey()
    {
        return true;
    }

    @Override
    public boolean isActivationNeeded()
    {
        return false;
    }

    /**
     * Create a context - create our PokerContext
     */
    @Override
    protected GameContext createGameContext(Game game,
                                            String sName, int nDesiredMinWidth, int nDesiredMinHeight,
                                            boolean bQuitOnClose)
    {

        return new PokerContext(this, (PokerGame) game, sName, nDesiredMinWidth, nDesiredMinHeight,
                                bQuitOnClose);
    }

    /**
     * Create a context - here for overridding
     */
    @Override
    protected GameContext createInternalGameContext(GameContext context,
                                                    String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        return new PokerContext((PokerContext) context, sName, nDesiredMinWidth, nDesiredMinHeight);
    }

    /**
     * Key validated - cleanup
     */
    @Override
    public void keyValidated(boolean bPatch)
    {
        // alpha/beta testing - remove old player profiles, save games
        // upon activation
        // turn off clean for B5 TODO: remove altogether for release?
//        if (!bPatch)
//        {
//            clean();
//            copySaveFiles();
//        }
        super.keyValidated(bPatch);
    }

//    /**
//     * Clean up various profiles from previous alpha/beta
//     */
//    private void clean()
//    {
//        if (getVersion().isAlpha() || getVersion().isBeta())
//        {
//            File userSave = GameConfigUtils.getSaveDirLocation(true);
//            cleanDir(userSave);
//        }
//    }
//
//    /**
//     * clean logic
//     */
//    private void cleanDir(File dir)
//    {
//        File files[] = dir.listFiles();
//        File file;
//        String sName;
//        boolean bDelete;
//        for (int i = 0; files != null && i < files.length; i++)
//        {
//            file = files[i];
//            sName = file.getName().toLowerCase();
//            if (sName.indexOf("cvs") != -1) continue;
//
//            if (file.isDirectory())
//            {
//                cleanDir(file);
//            }
//
//            // remove save files (online/save/home), profiles, db
//            if (sName.startsWith("online.") ||
//                //sName.startsWith("save.") ||
//                //sName.startsWith("home.") ||
//                sName.startsWith("profile.") ||
//                sName.startsWith("playertype.") ||
//                sName.startsWith("handgroup.") ||
//                sName.startsWith("handselection.") ||
//                sName.startsWith("tourney.") ||
//                dir.getName().equals("db"))
//            {
//                bDelete = false;
//                logger.debug("Alpha/Beta cleanup.  Removing: " + sName);
//                try {
//                    bDelete = file.delete();
//                }
//                catch (Throwable t)
//                {
//                    bDelete = false;
//                    logger.warn(Utils.formatExceptionText(t));
//                }
//                if (!bDelete)
//                {
//                    logger.warn("Unable to delete: " + sName);
//                }
//            }
//        }
//    }

    /**
     * for case where we just activated, need to start P2P before we
     * do to-do phase
     *
     * @param context
     */
    @Override
    protected void processingTODO(GameContext context)
    {
        initP2P();
        super.processingTODO(context);
    }

    /**
     * We use gameboard config
     */
    @Override
    protected boolean loadGameboardConfig()
    {
        return true;
    }

    /**
     * start P2P after showing main window
     * If we had a command line param, handle it
     */
    @Override
    protected void initialStart()
    {
        // start p2p server if we are validated (have a valid key)
        // see processTODO() below for case startup after activation
        // start after main window initialized required (so engine is ready)
        if (getRealLicenseKey() != null && !isActivationNeeded())
        {
            initP2P();
        }

        /**
         * handle load
         */
        if (!isDemo() && sFileParam_ != null)
        {
            Logger log = Logger.getLogger(PokerMain.class);
            if (sFileParam_.endsWith(GameListPanel.SAVE_EXT))
            {
                log.info("Loading saved game: " + sFileParam_);
                File file = new File(sFileParam_).getAbsoluteFile();
                try
                {
                    ConfigUtils.verifyFile(file);
                    GameState state = GameStateFactory.createGameState(file, false);
                    LoadSavedGame.loadGame(getDefaultContext(), state);
                    return;
                }
                catch (ApplicationError ae)
                {
                    log.error("Unable to load saved game: " + sFileParam_);
                    log.error(ae.toString());
                    // bad save file, so we just do normal initialStart
                }
            }
            else if (sFileParam_.endsWith(PokerConstants.JOIN_FILE_EXT))
            {
                log.info("Loading online game join: " + sFileParam_);
                File file = new File(sFileParam_).getAbsoluteFile();
                try
                {
                    // create message from file
                    String sURL = ConfigUtils.readFile(file).trim();
                    TypedHashMap params = new TypedHashMap();
                    params.setString(ListGames.PARAM_URL, sURL);

                    // seed path to JoinGame so cancel buttons work
                    getDefaultContext().seedHistory("StartMenu");
                    getDefaultContext().seedHistory("OnlineMenu");

                    // start JoinOnline phase
                    getDefaultContext().processPhase("JoinGame", params);
                    return;
                }
                catch (ApplicationError ae)
                {
                    log.error("Unable to load game join: " + sFileParam_);
                    log.error(ae.toString());
                    // bad join file, so we just do normal initialStart
                }
            }
        }

        super.initialStart();
    }

    /**
     * Called in some OS when Preferences menu item selected
     */
    @Override
    public void showPrefs()
    {
        boolean bShowDialog = true;
        Phase current = getDefaultContext().getCurrentUIPhase();
        if (current != null)
        {
            String sName = current.getGamePhase().getName();
            if (sName.equals("GamePrefs")) return; // already displayed
            if (sName.equals("StartMenu")) bShowDialog = false; // go to prefs screen
        }

        if (!bShowDialog)
        {
            getDefaultContext().processPhase("GamePrefs"); // TODO: active context?
        }
        else
        {
            getDefaultContext().processPhase("GamePrefsDialog"); // TODO: active context?
        }
    }

    /**
     * Message for expired copies
     */
    @Override
    protected String getExpiredMessage()
    {
        return "<font color=\"white\"> Version " + getVersion() + "</font>" +
               " of DD Poker has expired.  Please contact Donohoe Digital to get the " +
               " most recent version.";
    }

    /**
     * Get starting size - we set proportional to
     * 800x600, but at a size 200 less than screen height
     */
    @Override
    protected Dimension getStartingSize()
    {
        DisplayMode mode = frame_.getDisplayMode();
        int height = Math.max(DESIRED_MIN_HEIGHT, mode.getHeight() - PokerConstants.VERTICAL_SCREEN_FREE_SPACE);
        int width = Math.max(DESIRED_MIN_WIDTH, (DESIRED_MIN_WIDTH * height) / DESIRED_MIN_HEIGHT);
        return new Dimension(width, height);
    }

    ////
    //// Player names
    ////

    private List<String> names_ = new ArrayList<String>();

    /**
     * get names array list
     */
    public List<String> getNames()
    {
        return Collections.unmodifiableList(names_);
    }

    /**
     * Load names
     */
    private void loadNames()
    {
        URL names = new MatchingResources("classpath*:config/" + sAppName + "/names.txt").getSingleRequiredResourceURL();
        Reader reader = ConfigUtils.getReader(names);

        try
        {
            BufferedReader buf = new BufferedReader(reader);

            String sName;
            while ((sName = buf.readLine()) != null)
            {
                names_.add(sName);
            }
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
        finally
        {
            ConfigUtils.close(reader);
        }
    }

    ////
    //// Peer2Peer stuff
    ////

    private PokerConnectionServer p2p_;
    private PokerUDPServer udp_;
    private PokerTCPServer tcp_;
    private PokerUDPServer chat_;
    private LanManager lan_;

    public synchronized void initP2P()
    {
        ApplicationError.assertTrue(lan_ == null, "Attempting to start lan server twice");

        // create multicast server
        lan_ = new LanManager(this);
        lan_.start();
    }

    /**
     * Get lan manager
     */
    public LanManager getLanManager()
    {
        return lan_;
    }

    /**
     * Get udp server
     */
    @Override
    public UDPServer getUDPServer()
    {
        return udp_;
    }

    /**
     * Get udp server - used for chat/games
     */
    private PokerUDPServer getCreateUDPServer()
    {
        if (udp_ == null)
        {
            udp_ = new PokerUDPServer(this);
            udp_.init();
            udp_.manager().addMonitor(this);
            // nofify OnlineLobby
            UDPStatus.setUDPServer(udp_);
            if (udp_.isBound()) udp_.start();
        }
        return udp_;
    }

    /**
     * Get tcp server - used for games
     */
    private PokerTCPServer getTCPServer()
    {
        if (tcp_ == null)
        {
            tcp_ = new PokerTCPServer();
            tcp_.init();
            tcp_.start(); // always start regardless if bound
        }
        return tcp_;
    }

    /**
     * Get p2p server - UDP if true, TCP otherwise
     */
    public PokerConnectionServer getPokerConnectionServer(boolean bUDP)
    {
        // if asking for P2P and not null, make sure we return the right type
        if (p2p_ != null)
        {
            if ((bUDP && p2p_ != udp_) ||
                (!bUDP && p2p_ != tcp_))
            {
                shutdownPokerConnectionServer(p2p_);
            }
        }

        if (p2p_ == null)
        {
            // p2p server
            if (bUDP)
            {
                p2p_ = getCreateUDPServer();
            }
            else
            {
                p2p_ = getTCPServer();
            }
        }
        return p2p_;
    }

    /**
     * get chat server
     */
    public PokerUDPServer getChatServer()
    {
        if (chat_ == null)
        {
            chat_ = getCreateUDPServer();
        }
        return chat_;
    }

    /**
     * shutdown p2p server
     */
    public void shutdownPokerConnectionServer(PokerConnectionServer p2p)
    {
        if (p2p_ == null) return;

        p2p_ = null;

        if (p2p == tcp_)
        {
            tcp_.shutdown();
            tcp_ = null;
        }
        else
        {
            shutdownUDP();
        }
    }


    /**
     * shut down chat server
     */
    public void shutdownChatServer()
    {
        chat_ = null;
        shutdownUDP();
    }

    /**
     * shutdown UDP server
     */
    private void shutdownUDP()
    {
        if (chat_ == null && (p2p_ == null || p2p_ == tcp_))
        {
            udp_.shutdown();
            udp_ = null;
            UDPStatus.setUDPServer(null);
        }
    }

    /**
     * Poker TCP/IP server
     */
    private class PokerTCPServer extends Peer2PeerServer implements PokerConnectionServer
    {
        private PokerTCPServer()
        {
            super(PokerMain.this);
            setAppName("PokerTCPServer");
        }

        public void closeConnection(PokerConnection connection)
        {
            if (connection == null) return;
            super.closeChannel(connection.getSocket());
        }

        public int send(PokerConnection connection, DDMessageTransporter message) throws IOException
        {
            if (connection == null) return 0;

            Peer2PeerMessage p2p = (Peer2PeerMessage) message;
            return p2p.write(connection.getSocket());
        }

        public DDMessageTransporter newMessage(DDMessage msg)
        {
            return new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);
        }
    }

    /**
     * Notify by PokerConnectionServer that a connection is closing
     */
    public void connectionClosing(PokerConnection connection)
    {
        PokerGame game = (PokerGame) getDefaultContext().getGame();
        OnlineManager mgr = null;

        // if we have a game, look for online mgr
        if (game != null)
        {
            mgr = game.getOnlineManager();
        }

        // if no online manager, return error
        if (mgr != null)
        {
            mgr.connectionClosing(connection);
        }
        else
        {
            logger.warn("Connection closing notification received with no OnlineManager from: " + connection);
        }
    }

    /**
     * Handle p2p message received - hand off to OnlineManager
     */
    public DDMessageTransporter messageReceived(PokerConnection connection, DDMessageTransporter msg)
    {
        PokerGame game = (PokerGame) getDefaultContext().getGame();
        OnlineManager mgr = null;

        // if we have a game, look for online mgr
        if (game != null)
        {
            mgr = game.getOnlineManager();
        }

        // if no online manager, return error
        if (mgr == null)
        {
            // possibly disappeared in the interim
            if (p2p_ == null) return null;

            OnlineMessage omsg = new OnlineMessage(msg.getMessage());

            switch (omsg.getCategory())
            {
                // reply like Online Manager, but with bogus guid
                // so server test responds with appropriate message
                case OnlineMessage.CAT_TEST:
                    return OnlineManager.getTestReply(p2p_, "guid-no-online-game", omsg);

                // respond to any other type of message with same response,
                // as if some one was trying to join
                default:
                    //logger.warn("Message received with no OnlineManager: " + msg);
                    return OnlineManager.getAppErrorReply(p2p_, omsg, PropertyConfig.getMessage("msg.nojoin.nogame"), false);
            }
        }
        else
        {
            return mgr.handleMessage(msg, connection);
        }
    }

    /**
     * port used to direct connect
     */
    public int getPort()
    {
        return p2p_ == null ? 0 : p2p_.getPreferredPort();
    }

    /**
     * ip used to direct connect
     */
    public String getIP()
    {
        return p2p_ == null ? "127.0.0.1" : p2p_.getPreferredIP();
    }


    ////
    //// Peer2PeerControllerInterface (TCP)
    ////

    /**
     * Handle p2p message received - hand off to OnlineManager
     */
    public DDMessageTransporter p2pMessageReceived(SocketChannel channel, DDMessageTransporter msg)
    {
        return messageReceived(new PokerConnection(channel), msg);
    }

    /**
     * Handle when a socket is closed.
     */
    public void socketClosing(SocketChannel channel)
    {
        connectionClosing(new PokerConnection(channel));
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
                if (TESTING(UDPServer.TESTING_UDP))
                    logger.debug("POKER Created: " + Utils.getAddressPort(link.getRemoteIP()));
                link.addMonitor(this);
                break;

            case DESTROYED:
                if (TESTING(UDPServer.TESTING_UDP))
                    logger.debug("POKER Destroyed: " + Utils.getAddressPort(link.getRemoteIP()) +
                                 " stats: " + link.getStats());
                link.removeMonitor(this);
                break;
        }
    }

    ////
    //// UDPLinkMonitor (UDP)
    ////

    public void monitorEvent(UDPLinkEvent event)
    {
        PokerUDPTransporter msg;
        PokerUDPTransporter reply;

        UDPLink link = event.getLink();
        long elapsed = event.getElapsed();
        UDPData data = event.getData();

        switch (event.getType())
        {
            case ESTABLISHED:
                if (TESTING(UDPServer.TESTING_UDP)) logger.debug("POKER Established: " + link.toStringNameIP());
                break;

            case CLOSING:
                if (TESTING(UDPServer.TESTING_UDP)) logger.debug("POKER Closing: " + link.toStringNameIP());
                if (!link.getRemoteIP().equals(udp_.getChatServer()))
                    connectionClosing(new PokerConnection(link.getID()));
                break;

            case CLOSED:
                if (TESTING(UDPServer.TESTING_UDP)) logger.debug("POKER Closed: " + link.toStringNameIP());
                if (!link.getRemoteIP().equals(udp_.getChatServer()))
                    connectionClosing(new PokerConnection(link.getID()));
                break;

            case POSSIBLE_TIMEOUT:
                if (TESTING(UDPServer.TESTING_UDP)) logger.debug("POKER Possible timeout on " + link.toStringNameIP() +
                                                                 " (no message in last " + elapsed + " millis)");
                break;

            case TIMEOUT:
                if (link == udp_.getChatLink())
                {
                    notifyTimeout();
                }
                else
                {
                    // application - nothing to do since link will be closed by udp manager
                    logger.info("Timeout on " + link.toStringNameIP() + " (no message in last " + elapsed + " millis)");
                }
                break;

            case RESEND_FAILURE:
                if (link == udp_.getChatLink())
                {
                    notifyTimeout();
                }
                else
                {
                    // application - nothing to do since link will be closed by udp manager
                    logger.info("Resend Failure on " + link.toStringNameIP() + " (unable to send message " + data + ')');
                }
                break;

            case SESSION_CHANGED:
                if (TESTING(UDPServer.TESTING_UDP)) logger.debug("POKER session-changed: " + link.toStringNameIP());

                // force new hello since chat server was restarted
                if (link == udp_.getChatLink())
                {
                    notifyTimeout(); // this notifies user of disco
                    udp_.nullChatLink(); // this forces new hello
                }
                // if online game link, close on session change to force re-join
                else
                {
                    PokerGame game = (PokerGame) getDefaultContext().getGame();
                    if (game != null)
                    {
                        if (game.getHost().isLocallyControlled())
                        {
                            logger.info("Session changed (assuming rejoin): " + link.toStringNameIP());
                        }
                        else
                        {
                            logger.warn("Session changed on host (this shouldn't happen), closing link: " + link.toStringNameIP());
                            link.close();
                        }
                    }
                }
                break;

            case RECEIVED:
                // process message
                if (data.getType() == UDPData.Type.MESSAGE)
                {
                    msg = new PokerUDPTransporter(data);

                    // DEBUG
                    if (TESTING(EngineConstants.TESTING_UDP_APP))
                    {
                        OnlineMessage om = new OnlineMessage(msg.getMessage());
                        String sMsg = " {" + om.toStringCategory() + '}';
                        logger.debug("POKER msg from " + link.toStringNameIP() + ": " + data.toStringShort() + sMsg);
                    }

                    if (data.getUserType() == PokerConstants.USERTYPE_CHAT)
                    {
                        if (chatHandler_ != null) chatHandler_.chatReceived(new OnlineMessage(msg.getMessage()));
                    }
                    else
                    {
                        reply = (PokerUDPTransporter) messageReceived(new PokerConnection(link.getID()), msg);
                        if (reply != null)
                        {
                            link.queue(reply.getData());
                            link.send(); // send right away
                            if (!reply.isKeepAlive())
                            {
                                link.close();
                            }
                        }
                    }
                }
                else
                {
                    if (TESTING(EngineConstants.TESTING_UDP_APP))
                        logger.debug("POKER msg from " + link.toStringNameIP() + ": " + data.toStringShort());
                }
                break;
        }
    }

    /**
     * notify of timeout to chat server
     */
    protected void notifyTimeout()
    {
        if (chatHandler_ != null)
        {
            String sInfo = PropertyConfig.getMessage(!udp_.isBound() ? "msg.chat.lobby.notbound" :
                                                     udp_.getChatLink().isEstablished() ? "msg.chat.lobby.lost" :
                                                     "msg.chat.lobby.timeout");
            OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
            omsg.setChat(PropertyConfig.getMessage("msg.chat.lobby.unavail", sInfo,
                                                   udp_.getChatLink() != null ?
                                                   "" + udp_.getChatLink().getRemoteIP().getPort() :
                                                   udp_.getConfigPort()));
            chatHandler_.chatReceived(omsg);
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
        return getTimeout(link); // we don't ues this in poker
    }

    public int getPossibleTimeoutNotificationStart(UDPLink link)
    {
        return getTimeout(link); // we don't ues this in poker
    }

    ////
    //// Chat
    ////

    private ChatHandler chatHandler_;

    ChatHandler getChatLobbyHandler()
    {
        return chatHandler_;
    }

    public void setChatLobbyHandler(ChatHandler mgr)
    {
        chatHandler_ = mgr;

        // if no chat handler, close link to chat server
        if (chatHandler_ == null && udp_ != null)
        {
            udp_.closeChatLink();
        }
    }

    ////
    //// LanControllerInterface methods
    ////

    //
    // Interface methods implemented by super class:
    //
    //  + public String getGUID()
    //  + public String getLicenseKey()
    //

    /**
     * player name (current player)
     */
    public String getPlayerName()
    {
        // note: PlayerProfileOptions cache's the profile, so this isn't
        // expensive
        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        if (profile == null) return PropertyConfig.getMessage("msg.playername.undefined");
        return profile.getName();
    }

    /**
     * return whether key in message is valid
     */
    public boolean isValid(DDMessage msg)
    {
        // look for key
        String sKey = msg.getKey();
        if (sKey == null)
        {
            logger.warn("Message has no key: " + msg);
            return false;
        }

        // Get version
        Version version = msg.getVersion();
        if (version == null)
        {
            logger.warn("Message has no version: " + version);
            return false;
        }

        return true;
    }

    /**
     * Allow duplicate keys?
     */
    public boolean allowDuplicate()
    {
        return TESTING(EngineConstants.TESTING_SKIP_DUP_KEY_CHECK);
    }

    /**
     * handle duplicate key case
     */
    public void handleDuplicateKey(String sName, String sHost, String sIP)
    {
        handleDuplicateIp(sName, sHost, sIP);
    }

    /**
     * Handle same IP (disallows running 2.x and 3.x at same time)
     */
    public void handleDuplicateIp(String sName, String sHost, String sIP)
    {
        if (bDup_) return; // already handling
        bDup_ = true;

        final String sMsg = PropertyConfig.getMessage("msg.dupip.exit", sName, sHost, sIP);
        // do processing
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        PokerUtils.displayInformationDialog(getDefaultContext(), Utils.fixHtmlTextFor15(sMsg), "msg.title.dupdd", null);
                        System.exit(1);
                    }
                }
        );
    }

    private boolean bDup_ = false;

    // constants for online game description
    public static final String ONLINE_GAME_LAN_CONNECT = "lan-connect";
    public static final String ONLINE_GAME_PROFILE = "profile";
    public static final String ONLINE_GAME_STATUS = "status";

    /**
     * Get online game description
     */
    public DataMarshal getOnlineGame()
    {
        PokerGame game = (PokerGame) getDefaultContext().getGame();
        DMTypedHashMap gamedata = null;

        // if there is a game with an online manager, then this
        // is a host that has a game, so prepare data
        if (game != null && game.getOnlineManager() != null &&
            game.getOnlineManager().isHost())
        {
            gamedata = new DMTypedHashMap();
            gamedata.setInteger(ONLINE_GAME_STATUS, game.getOnlineMode());
            if (game.isAcceptingRegistrations())
            {
                gamedata.setString(ONLINE_GAME_LAN_CONNECT, game.getLanConnectURL());
                gamedata.setObject(ONLINE_GAME_PROFILE, game.getProfile());
            }
        }
        return gamedata;
    }

    /**
     * Compare games
     */
    public boolean isEquivalentOnlineGame(DataMarshal one, DataMarshal two)
    {
        if (one == null && two == null) return true;

        if ((one == null && two != null) ||
            (one != null && two == null)) return false;

        DMTypedHashMap d1 = (DMTypedHashMap) one;
        DMTypedHashMap d2 = (DMTypedHashMap) two;

        // Zero: game status must be same
        if (d1.getInteger(ONLINE_GAME_STATUS, -1) !=
            d2.getInteger(ONLINE_GAME_STATUS, -1)) return false;

        // One: lan string must be same
        if (!d1.getString(ONLINE_GAME_LAN_CONNECT, "").equals(
                d2.getString(ONLINE_GAME_LAN_CONNECT, ""))) return false;

        // Two: profile name and create date must be the same
        TournamentProfile p1 = (TournamentProfile) d1.getObject(ONLINE_GAME_PROFILE);
        TournamentProfile p2 = (TournamentProfile) d2.getObject(ONLINE_GAME_PROFILE);

        if (p1 == null && p2 == null) return true;

        if ((p1 == null && p2 != null) ||
            (p1 != null && p2 == null)) return false;

        return p1.getName().equals(p2.getName()) &&
               p1.getCreateDate() == p2.getCreateDate() &&
               p1.getUpdateDate() == p2.getUpdateDate();
    }
}
