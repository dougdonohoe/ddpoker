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
 * GameEngine.java
 *
 * Created on October 27, 2002, 2:46 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.CommandLine;
import com.donohoedigital.base.RandomGUID;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.Version;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.config.GameConfigUtils;
import com.donohoedigital.games.config.GameboardConfig;
import com.donohoedigital.games.config.GamedefConfig;
import com.donohoedigital.gui.*;
import com.donohoedigital.udp.UDPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 * @author Doug Donohoe
 */
public abstract class GameEngine extends BaseApp
{
    private final Logger logger = LogManager.getLogger(GameEngine.class);

    // private stuff - does not change once created
    private static GameEngine engine_ = null;
    private GamedefConfig gamedef_;
    private final String sMainModule_;

    // subclass access
    protected SplashScreen splashscreen_;
    protected boolean bCheckFailed_ = false;

    // shared by GameContext
    GameboardConfig gameconfig_;

    // other private stuff
    private String sOverrideKey_ = null;
    private String sHeadlessKey_;
    private String sLastReal_ = null;
    private String sLastGen_ = null;
    private EnginePrefs prefNode_;
    private final String sPrefNode_;
    private String sKeyNode_;
    private String guid_;
    private boolean bReady_ = false;
    private boolean bFull_ = false;

    // variable based on current state/game
    private GameContext defaultContext_;

    /**
     * Create GameEngine from config file
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
    public GameEngine(String sConfigName, String sMainModule, String sVersionString, String[] args, boolean bHeadless)
            throws ApplicationError
    {
        // init
        super(sConfigName, sVersionString, args, bHeadless);

        // remember who we are
        engine_ = this;

        // define the node for game preferences
        sPrefNode_ = sConfigName + "-prefs";
        sMainModule_ = sMainModule;
    }

    public String name() {
        return super.sAppName;
    }

    /**
     * primary initialization
     */
    @Override
    public void init()
    {
        super.init();

        // BUG 278 - use user dir for save files
        // make sure save files in user's dir have all files
        // in installation dir
        if (!bHeadless_) copySaveFiles();

        // set theme
        if (!bHeadless_) GuiUtils.setTheme(new EngineMetalTheme());

        // set version
        Version v = getVersion();
        DDMessage.setDefaultVersion(v);

        // set locale in version
        v.setLocale(getLocale());

        // figure out prefs key for license information
        String sVers = "" + v.getMajor();
        if (v.isAlpha()) sVers += "a" + v.getAlphaBetaVersion();
        if (v.isBeta()) sVers += "b" + v.getAlphaBetaVersion();
        sKeyNode_ = "key/" + sAppName + "-" + sVers;

        // BUG 199 - added to specify a different key during testing
        if (TESTING(EngineConstants.TESTING_OVERRIDE_KEY))
        {
            sOverrideKey_ = getCommandLineOptions().getString("key", null);
            if (sOverrideKey_ != null)
            {
                logger.debug("Activation key set to {}", sOverrideKey_);
            }
        }

        // get activation key
        String sKey = getRealLicenseKey();

        // validate key
        if (bHeadless_)
        {
            setHeadless();
        }
        else
        {
            DDMessage.setDefaultRealKey(sKey);
            DDMessage.setDefaultKey(getPublicUseKey());
        }

        // check prereqs
        if (!checkPreReq() || !checkSize())
        {
            bCheckFailed_ = true;
            return;
        }

        // no more UI stuff if headless
        if (bHeadless_) return;

        // set desktop pane - used so windows can reorder
        JDesktopPane desktop = new JDesktopPane();
        MyDesktopUI ui = new MyDesktopUI();
        desktop.setUI(ui);
        ui.uninstallKeyboardActions(); // uninstall - breaks arrow key scrolling in ScollGameboard
        // seems slow desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        frame_.setLayeredPane(desktop);

        // set prefs for music - unwieldy, but who cares right now?
        setAudioPrefs();

        // As of DD Poker 3, disable screen mode stuff
        bFull_ = false;

        // change splash screen UI to show details available after config files loaded
        if (splashscreen_ != null)
        {
            splashscreen_.changeUI(this, true, null);
        }
    }

    /**
     * Can be overridden for application specific options
     */
    @Override
    protected void setupApplicationCommandLineOptions()
    {
        // JDD - remove test since this flag is set
        // after this method is called.  Okay, since this
        // is ignored if common setting isn't on and
        // hardly anyone can figure out command line
        //if (TESTING(EngineConstants.TESTING_OVERRIDE_KEY))
        //{
        CommandLine.addStringOption("key", null);
        //}

        // used for shifting starting position for testing
        CommandLine.addIntegerOption("x", -1);
        CommandLine.setDescription("x", "x coor", "#");
        CommandLine.addIntegerOption("y", -1);
        CommandLine.setDescription("y", "y coor", "#");
        CommandLine.addFlagOption("reset");
        CommandLine.setDescription("reset", "reset window sizes/positions");
    }

    /**
     * stuff to do pre-config manager init
     */
    @Override
    protected void preConfigManagerInit()
    {
        if (!bHeadless_)
        {
            URL file = new MatchingResources("classpath*:config/" + sAppName + "/images/" + getSplashBackgroundFile()).getSingleRequiredResourceURL();
            URL icon = new MatchingResources("classpath*:config/" + sAppName + "/images/" + getSplashIconFile()).getSingleRequiredResourceURL();
            splashscreen_ = new SplashScreen(file, icon, getSplashTitle());
            splashscreen_.setVisible(true);
        }
    }

    /**
     * Get version
     */
    public abstract Version getVersion();

    /**
     * Get start of activation key
     */
    protected abstract int getKeyStart();

    /**
     * Use by subclass to prevent startup
     * if any pre-reqs not met
     */
    protected boolean checkPreReq()
    {
        return true;
    }

    /**
     * initial splash file name
     */
    protected String getSplashBackgroundFile()
    {
        return "splash.jpg";
    }

    /**
     * initial splash icon file name
     */
    protected String getSplashIconFile()
    {
        return "icon.gif";
    }

    /**
     * initial splash title
     */
    protected String getSplashTitle()
    {
        return "Donohoe Digital Presents...";
    }

    /**
     * UI for handling keyboard actions
     */
    private static class MyDesktopUI extends javax.swing.plaf.basic.BasicDesktopPaneUI
    {
        @Override
        public void uninstallKeyboardActions()
        {
            super.uninstallKeyboardActions();
        }
    }

    /**
     * Get name of node to use for preferences
     */
    public String getPrefsNodeName()
    {
        return sPrefNode_;
    }

    /**
     * Get Node used for prefs
     */
    public EnginePrefs getPrefsNode()
    {
        if (prefNode_ == null) prefNode_ = new EnginePrefs(DDOption.getOptionPrefs(sPrefNode_));
        return prefNode_;
    }

    /**
     * start headless mode - cannot be un-done
     */
    private void setHeadless()
    {
        sKeyNode_ += "h";
        DDMessage.setDefaultRealKey(getHeadlessLicenseKey());
        DDMessage.setDefaultKey(getPublicUseKey());
    }

    /**
     * Return guid
     */
    public String getGUID()
    {
        if (guid_ == null) guid_ = new RandomGUID(ConfigUtils.getLocalHost(true), true).toString();
        return guid_;
    }

    /**
     * Get license key
     */
    public String getRealLicenseKey()
    {
        if (TESTING(EngineConstants.TESTING_OVERRIDE_KEY) && sOverrideKey_ != null)
        {
            return sOverrideKey_;
        }
        Preferences node = Prefs.getUserPrefs(sKeyNode_);
        String key = node.get(Activation.REGKEY, null);

        // auto create key if we don't have one
        if (key == null)
        {
            key = Activation.createKeyFromGuid(getKeyStart(), getGUID(), getLocale());
            logger.debug("NEW KEY: {}", key);
            setLicenseKey(key);
        }
        return key;
    }

    /**
     * Headless license key - unique each time run
     */
    public String getHeadlessLicenseKey()
    {
        if (sHeadlessKey_ == null)
        {
            sHeadlessKey_ = "HEADLESS-" + getGUID();
        }
        return sHeadlessKey_;
    }

    /**
     * Get obfuscated license key for use in online games
     */
    public String getPublicUseKey()
    {
        String sReal;

        // get key upon which public key is based
        if (isHeadless())
        {
            sReal = getHeadlessLicenseKey();
        }
        else
        {
            sReal = getRealLicenseKey();
        }

        // generate public key if needed
        if (sLastGen_ == null || sLastReal_ == null || !sLastReal_.equals(sReal))
        {
            if (sReal != null && !sReal.isEmpty())
            {
                sLastReal_ = sReal;
                if (isHeadless())
                {
                    sLastGen_ = "H-" + Activation.getPublicKey("headless", sLastReal_);
                }
                else
                {
                    sLastGen_ = "P-" + Activation.getPublicKey("public", sLastReal_);
                }

                //logger.debug("GEN: " + sLastGen_);
            }
            else
            {
                sLastReal_ = null;
                sLastGen_ = null;
                //logger.debug("GEN cleared");
            }
        }
        return sLastGen_;
    }

    /**
     * Set the license key (assumes key is valid)
     */
    private void setLicenseKey(String sKey)
    {
        Preferences node = Prefs.getUserPrefs(sKeyNode_);
        node.put(Activation.REGKEY, sKey);
        DDMessage.setDefaultRealKey(sKey);
        DDMessage.setDefaultKey(getPublicUseKey());
    }

    /**
     * Clear all prefs, keeping banned key
     */
    public void clearAllPrefs()
    {
        prefNode_ = null;
        Prefs.clearAll();
        setAudioPrefs();
    }

    /**
     * Set whether to display full screen
     */
    public void setFull(boolean b)
    {
        bFull_ = b;
    }

    /**
     * copy any pre-installed save files
     */
    protected void copySaveFiles()
    {
        File userSave = GameConfigUtils.getSaveDir();
        ConfigUtils.copyURLs("save/" + sAppName, "**/*.*", userSave);
    }

    /**
     * set all audio prefs
     */
    private void setAudioPrefs()
    {
        EnginePrefs prefs = getPrefsNode();

        AudioConfig.setFXGain(prefs.getIntOption(EngineConstants.PREF_FX_VOL));
        AudioConfig.setMusicGain(prefs.getIntOption(EngineConstants.PREF_MUSIC_VOL));
        AudioConfig.setBGMusicGain(prefs.getIntOption(EngineConstants.PREF_BGMUSIC_VOL));

        AudioConfig.setMuteFX(!prefs.getBooleanOption(EngineConstants.PREF_FX));
        AudioConfig.setMuteMusic(!prefs.getBooleanOption(EngineConstants.PREF_MUSIC));
        AudioConfig.setMuteBGMusic(!prefs.getBooleanOption(EngineConstants.PREF_BGMUSIC));
    }

    // sizes
    protected static final int DESIRED_MIN_WIDTH = 800;
    protected static final int DESIRED_MIN_HEIGHT = 600;

    // min size
    private static final int MIN_WIDTH = 768;
    private static final int MIN_HEIGHT = 600;

    /**
     * See if display is min size - 800x600 (allow less to accommodate tablet PC 768x1024)
     */
    private boolean checkSize()
    {
        if (bHeadless_) return true;

        DisplayMode mode = frame_.getDisplayMode();
        if ((mode.getWidth() < MIN_WIDTH || mode.getHeight() < MIN_HEIGHT) && splashscreen_ != null)
        {
            String sMessage = PropertyConfig.getMessage("msg.wrongsize",
                                                        mode.getWidth(),
                                                        mode.getHeight());
            splashscreen_.changeUI(this, true, sMessage);

            return false;
        }
        return true;
    }

    /**
     * Does this game use a gameboard?
     */
    protected boolean loadGameboardConfig()
    {
        return true;
    }

    /**
     * Get starting size (defaults to 800x600)
     */
    protected Dimension getStartingSize()
    {
        return new Dimension(DESIRED_MIN_WIDTH, DESIRED_MIN_HEIGHT);
    }

    /**
     * return UDP server in use (default returns null; should be overridden)
     */
    public UDPServer getUDPServer()
    {
        return null;
    }

    /**
     * Create main game window
     */
    @Override
    protected BaseFrame createMainWindow()
    {
        defaultContext_ = createGameContext(null, "main", DESIRED_MIN_WIDTH, DESIRED_MIN_HEIGHT, true);
        return defaultContext_.getFrame();
    }

    /**
     * Create a context - here for overridding
     */
    protected GameContext createGameContext(Game game,
                                            String sName, int nDesiredMinWidth, int nDesiredMinHeight,
                                            boolean bQuitOnClose)
    {
        return new GameContext(this, game, sName, nDesiredMinWidth, nDesiredMinHeight, bQuitOnClose);
    }

    /**
     * Create a context - here for overridding
     */
    protected GameContext createInternalGameContext(GameContext context,
                                                    String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        return new GameContext(context, sName, nDesiredMinWidth, nDesiredMinHeight);
    }

    /**
     * Get default context
     */
    public GameContext getDefaultContext()
    {
        return defaultContext_;
    }

    /**
     * Call to init main window
     */
    protected void initMainWindow()
    {
        // load config files
        gamedef_ = new GamedefConfig(sMainModule_);
        if (loadGameboardConfig()) gameconfig_ = new GameboardConfig(sMainModule_);

        // ready to go
        bReady_ = true;

        // show main window now if now waiting for splash screen (or skipping splash screen)
        showMainWindow();
    }

    /**
     * Call when ready to show main window.  Removes the
     * splash screen if visible
     */
    public void showMainWindow()
    {
        // if splash screen visible, remove it
        if (splashscreen_ != null)
        {
            splashscreen_.setVisible(false);
            splashscreen_.dispose();
            splashscreen_ = null;
        }

        // wait until ready (for case when user clicks splash choice
        // [which calls this method] before initMainWindow is done)
        while (!bReady_) Utils.sleepMillis(100);

        // init main window
        defaultContext_.getFrame().init(null, true, getStartingSize(), bFull_, PropertyConfig.getRequiredStringProperty("msg.application.name"), true);

        // need to do after init so title is set
        contextInited(defaultContext_);

        // start the engine with the first phase
        initialStart();

        // display the frame
        displayMainWindow();
    }

    /**
     * Very first start phase - calls start() by default, but
     * can be overridden (e.g., for load saved game)
     */
    protected void initialStart()
    {
        defaultContext_.processPhase(gamedef_.getStartPhaseName(), null, true);
    }

    /**
     * Called from window closing - calls Exit phase and returns
     * false (meaning that the caller won't close the app - instead,
     * Exit does that)
     */
    @Override
    public boolean okayToClose()
    {
        // this prompts users
        defaultContext_.processPhase("Exit"); // TODO: active context?
        return false;
    }

    /**
     * Called in some OS when Preferences menu item selected
     */
    @Override
    public void showPrefs()
    {
        defaultContext_.processPhase("Prefs"); // TODO: active context?
    }

    /**
     * Called in some OS when About menu item selected
     */
    @Override
    public void showAbout()
    {
        String sMsg = PropertyConfig.getMessage("label.about.label", getVersion());
        EngineUtils.displayInformationDialog(getDefaultContext(), sMsg);
    }

    /**
     * Return the engine
     */
    public static GameEngine getGameEngine()
    {
        return engine_;
    }

    /**
     * Get GamedefConfig used by this engine
     */
    public GamedefConfig getGamedefconfig()
    {
        return gamedef_;
    }

    /**
     * Get GameboardConfig used by this engine
     */
    public GameboardConfig getGameboardConfig()
    {
        return gameconfig_;
    }

    ////
    //// Keep track of contexts
    ////

    // list of contexts
    private final Map<String, ContextTracker> contexts_ = new HashMap<>();

    /**
     * note that a context was created
     */
    void contextInited(GameContext context)
    {
        DDWindow window = context.getWindow();
        String sName = window.getName();
        ContextTracker tracker = contexts_.get(sName);
        if (tracker == null)
        {
            tracker = new ContextTracker(sName);
            contexts_.put(sName, tracker);
        }

        tracker.add(context);
    }

    /**
     * note that a window was desroyed
     */
    void contextDestroyed(GameContext context)
    {
        DDWindow window = context.getWindow();
        String sName = window.getName();
        ContextTracker tracker = contexts_.get(sName);
        ApplicationError.assertNotNull(tracker, "No tracker for window", sName);
        if (tracker.remove(context))
        {
            contexts_.remove(sName);
        }
    }

    /**
     * Get first context of given name
     */
    GameContext getContext(String sName)
    {
        ContextTracker tracker = contexts_.get(sName);
        if (tracker != null)
        {
            return tracker.get();
        }

        return null;
    }

    /**
     * class to track all instances of a window
     */
    private static class ContextTracker
    {
        int nNum;
        String sName;
        List<GameContext> contexts = new ArrayList<>();

        // constructor
        ContextTracker(String sName)
        {
            this.sName = sName;
        }

        // add window to list
        void add(GameContext context)
        {
            contexts.add(context);
            nNum++;

            if (nNum > 1)
            {
                String sTitle = context.getWindow().getTitle() + " - " + nNum;
                context.getWindow().setTitle(sTitle);
            }
        }

        // remove window, return true if tracker now empty
        boolean remove(GameContext context)
        {
            ApplicationError.assertTrue(contexts.remove(context), "Window not found in list", context.getWindow().getName());
            return contexts.isEmpty();
        }

        // GameContext 1st window
        GameContext get()
        {
            return contexts.getFirst();
        }
    }
}
