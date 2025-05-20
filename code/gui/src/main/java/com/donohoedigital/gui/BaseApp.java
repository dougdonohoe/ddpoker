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
package com.donohoedigital.gui;

import com.donohoedigital.base.CommandLine;
import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

@SuppressWarnings("CommentedOutCode")
public abstract class BaseApp
{
    private final Logger logger = LogManager.getLogger(BaseApp.class);

    static Thread mainThread_ = null;
    protected static BaseApp app_ = null;

    protected String sLocale_ = null;
    protected BaseFrame frame_;
    protected TypedHashMap htOptions_;
    protected boolean bHeadless_;
    protected String sAppName;

    private boolean bReady_ = false;
    private String sVersionString;
    private final String[] args;

    public BaseApp(String sAppName, String sVersionString, String[] args)
    {
        this(sAppName, sVersionString, args, false);
    }

    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
    public BaseApp(String sAppName, String sVersionString, String[] args, boolean bHeadless)
    {
        mainThread_ = Thread.currentThread();
        app_ = this;
        bHeadless_ = bHeadless;

        //
        // If mac, we need to instantiate by name the mac application class
        // we do this so we can compile this on all platforms
        //
        if (Utils.ISMAC && !bHeadless)
        {
            setupMac();
            Utils.sleepMillis(500); // BUG 266 - allow for this to register so we can get app events
        }

        this.sAppName = sAppName;
        this.sVersionString = sVersionString;
        this.args = args;
    }

    private void setupMac() {
        // Set handlers for macOS-specific actions
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();

            desktop.setAboutHandler(e -> {
                if (!app_.isReady()) return;
                app_.showAbout();
            });

            desktop.setOpenFileHandler(e -> {
                if (!e.getFiles().isEmpty()) {
                    CommandLine.setMacFileArg(e.getFiles().get(0).getAbsolutePath());
                }
            });

            desktop.setPreferencesHandler(e -> {
                if (!app_.isReady()) return;
                app_.showPrefs();
            });

            desktop.setQuitHandler((e, response) -> {
                if (app_.isReady()) {
                    app_.quit();
                    response.cancelQuit();  // Indicates that app handles quit
                } else {
                    response.performQuit(); // Forces quit if app isn't ready
                }
            });
        }
    }
    /**
     * Can be overridden for application specific options
     */
    protected void setupApplicationCommandLineOptions()
    {
    }

    /**
     * Get command line options
     */
    public TypedHashMap getCommandLineOptions()
    {
        return htOptions_;
    }

    /**
     * Sets up application-specific command line options.
     */
    private void setupStandardCommandLineOptions()
    {

        CommandLine.setUsage(getClass().getName() + " [options]");

        CommandLine.addStringOption("module", null);
        CommandLine.setDescription("module", "Extra Module (runtime)", "module");
        CommandLine.addStringOption("locale", null);
        CommandLine.setDescription("locale", "Locale", "locale");
    }

    /**
     * Main init function after construction
     */
    public void init()
    {
        // setup command line options
        setupStandardCommandLineOptions();
        setupApplicationCommandLineOptions();

        // get command line options
        CommandLine.parseArgs(args);
        htOptions_ = CommandLine.getOptions();
        sLocale_ = htOptions_.getString("locale");
        String sExtraModule = htOptions_.getString("module");

        // set locale
        Locale.setDefault(PropertyConfig.getLocale(sLocale_));

        // set version string items
        if (sVersionString == null) sVersionString = "";
        Utils.setVersionString(sVersionString); // keep in sync with FileCleanup
        Prefs.setRootNodeName(sAppName + sVersionString); // keep in sync with FileCleanup

        // preinit
        preConfigManagerInit();

        // init config files
        ApplicationType type = bHeadless_ ? ApplicationType.HEADLESS_CLIENT : ApplicationType.CLIENT;
        new ConfigManager(sAppName, type, sExtraModule, sLocale_, true);

        // create base frame
        if (!bHeadless_)
        {
            frame_ = createMainWindow();
            frame_.setTitle(sAppName);
        }

        //debug
        //printAllModes();
    }

    /**
     * stuff to do pre-config manager init
     */
    protected void preConfigManagerInit()
    {
    }

    /**
     * Create main game window
     */
    protected BaseFrame createMainWindow()
    {
        return new BaseFrame();
    }

    /**
     * Display main window and set ready flag when done
     */
    protected void displayMainWindow()
    {
        frame_.display();

        // invoke later to set ready flag (used for Mac integration)
        SwingUtilities.invokeLater(
                () -> {
                    bReady_ = true;
                    //logger.debug("INITIAL frame size: " + initSize_);
                }
        );
    }

    /**
     * is this headless?
     */
    public boolean isHeadless()
    {
        return bHeadless_;
    }

    /**
     * Return if ready for processing
     */
    public boolean isReady()
    {
        return bReady_;
    }

    /**
     * Get locale
     */
    public String getLocale()
    {
        return sLocale_;
    }

    /**
     * Subclass must implement - called from window closing
     * event.  If true return, frame is closed and app exits
     */
    public abstract boolean okayToClose();

    /**
     * Called in some OS when Preferences menu item selected.
     * Should be overridden to do something.
     */
    public void showPrefs()
    {
    }

    /**
     * Called in some OS when About menu item selected.
     * Should be overridden to do something.
     */
    public void showAbout()
    {
    }

    /**
     * quit app - if okayToClose
     */
    public void quit()
    {
        if (!okayToClose()) return;

        exit(0);
    }

    /**
     * Use this to exit application cleanly
     */
    public void exit(int nCode)
    {
        AudioConfig.stopBackgroundMusic();

        if (frame_ != null)
        {
            try
            {
                frame_.cleanup();
            }
            catch (Throwable t)
            {
                logger.debug("Error trying to cleanup: {}", Utils.formatExceptionText(t));
            }

            exitAfterWindowClosed(nCode);
        }
        else
        {
            _exit(nCode);
        }
    }

    /**
     * Handle final cleanup
     */
    private void _exit(int nCode)
    {
        System.exit(nCode);
    }

    /**
     * deferred exit
     */
    private void exitAfterWindowClosed(final int nCode)
    {
        SwingUtilities.invokeLater(
                () -> _exit(nCode)
        );
    }

    /**
     * Return current base app
     */
    public static BaseApp getBaseApp()
    {
        return app_;
    }

    /**
     * Get graphics of default (main) window
     */
    public static Graphics getGraphicsDefault()
    {
        return getBaseApp().frame_.getGraphics();
    }

//    ////
//    //// Debugging help
//    ////
//    private void printAllModes()
//    {
//        DisplayMode mode = frame_.getDisplayMode();
//        printMode("Current", mode);
//        DisplayMode modes[] = frame_.getDisplayModes();
//        for (int i = 0; i < modes.length; i++)
//        {
//            if (modes[i].getRefreshRate() != mode.getRefreshRate()) continue;
//            if (modes[i].getBitDepth() != mode.getBitDepth()) continue;
//            printMode("#"+i,modes[i]);
//        }
//    }
//
//    private void printMode(String sName, DisplayMode mode)
//    {
//        logger.debug("Mode " + sName + ": " + mode.getWidth() + "x" + mode.getHeight() +
//                    " " + mode.getRefreshRate() + "mhz " + mode.getBitDepth() + "bits");
//    }
}
