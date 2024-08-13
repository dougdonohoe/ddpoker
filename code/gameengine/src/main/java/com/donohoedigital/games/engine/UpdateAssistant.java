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
 * UpdateAssistant.java
 *
 * Created on September 1, 2003, 9:35 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import java.awt.*;
import java.util.*;
import java.util.prefs.*;

/**
 * @author Doug Donohoe
 */
public class UpdateAssistant extends BasePhase
{
    //static Logger logger = Logger.getLogger(UpdateAssistant.class);

    public static final String PREF_AUTOCHECKTIME = "autochecktime";
    public static final String PREF_CHECKTIME = "checktime";
    public static final String PREF_INSTALLTIME = "installtime";

    public static final String PARAM_RELEASES = "releases";

    /**
     * Auto check interval of one week.
     */
    public static final long AUTO_CHECK_INTERVAL_MILLIS = (60 * 60 * 24 * 7) * 1000;

    private ButtonBox buttonbox_;
    private MenuBackground menu_;
    private DDHtmlArea infoText_;
    private DDHtmlArea statusText_;
    private DDButton checkButton_;
    private DDButton installButton_;
    private DDButton returnButton_;
    private DDButton closeButton_;
    private DDButton cancelButton_;

    private Preferences prefs_;
    private long nCheckTime_ = -1;
    private long nInstallTime_ = -1;

    private DMArrayList<PatchRelease> releases_ = null;

    /**
     * Get the update preferences.
     *
     * @return the preferences
     */
    public static Preferences getPreferences()
    {
        GameEngine engine = GameEngine.getGameEngine();
        String NODE = engine.getPrefsNodeName() + "/update";
        return DDOption.getOptionPrefs(NODE);
    }

    /**
     * Creates a new instance of UpdateAssistant.
     */
    public UpdateAssistant()
    {
    }

    /**
     * Initialize the phase.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // Check for a release.  Allows update check to occur externally.
        releases_ = (DMArrayList<PatchRelease>) gamephase.getObject(PARAM_RELEASES);

        // Node info.
        prefs_ = getPreferences();
        nCheckTime_ = prefs_.getLong(PREF_CHECKTIME, nCheckTime_);
        nInstallTime_ = prefs_.getLong(PREF_INSTALLTIME, nInstallTime_);

        // Create base panel which holds everything.
        menu_ = new MenuBackground(gamephase);

        // Buttons.
        buttonbox_ = new ButtonBox(context_, gamephase, this, "empty", false, false);
        menu_.getMenuBox().add(buttonbox_, BorderLayout.SOUTH);
        checkButton_ = buttonbox_.getDefaultButton();
        installButton_ = buttonbox_.getButton("installupdate");
        ApplicationError.assertNotNull(installButton_, "No installupdate button");
        returnButton_ = buttonbox_.getButton("returnmain");
        ApplicationError.assertNotNull(returnButton_, "No returnmain button");
        closeButton_ = buttonbox_.getButton("closegame");
        ApplicationError.assertNotNull(closeButton_, "No closegame button");
        cancelButton_ = buttonbox_.getButton("cancel");
        ApplicationError.assertNotNull(cancelButton_, "No cancel button");

        if (releases_ != null)
        {
            buttonbox_.removeButton(checkButton_);
            buttonbox_.removeButton(returnButton_);
            buttonbox_.removeButton(closeButton_);
        }
        else
        {
            buttonbox_.removeButton(installButton_);
            buttonbox_.removeButton(returnButton_);
            buttonbox_.removeButton(closeButton_);
        }

        // Base area for displaying information.
        DDPanel base = new DDPanel();
        menu_.getMenuBox().add(base, BorderLayout.CENTER);

        // Status text.
        statusText_ = new DDHtmlArea(GuiManager.DEFAULT, "UpdateStatus");
        statusText_.setDisplayOnly(true);
        statusText_.setBorder(EngineUtils.getStandardMenuTextBorder());
        setStatusMessage();
        base.add(statusText_, BorderLayout.NORTH);

        // Info text.
        infoText_ = new DDHtmlArea(GuiManager.DEFAULT, "UpdateInfo");
        infoText_.setDisplayOnly(true);
        infoText_.setBorder(EngineUtils.getStandardMenuTextBorder());
        infoText_.addHyperlinkListener(GuiUtils.HYPERLINK_HANDLER);
        base.add(infoText_, BorderLayout.CENTER);

// Turned off in DD Poker 3
//        // Check that the user has permissions to install.
//        UpdateInstaller installer = new UpdateInstaller(null);
//
//        if (installer.checkPermissions())
//        {
//            if (releases_ != null)
//            {
//                // Show the proper screen and then perform the install by click the appropriate button.
//                PatchRelease release = (PatchRelease) releases_.get(0);
//                String desc = release.getDescription();
//
//                if (desc == null)
//                {
//                    desc = PropertyConfig.getMessage("msg.update.available", engine_.getVersion());
//                }
//
//                setInfoMessage("msg.update.install", desc);
//
//                new Thread(new AutoInstall(), "AutoInstall").start();
//            }
//            else
//            {
//                setInfoMessage("msg.update.check", null);
//            }
//        }
//        else
//        {
//            setInfoMessage("msg.update.permission", null);
//            checkButton_.setEnabled(false);
//        }
    }

    /**
     * Start the phase.
     */
    @Override
    public void start()
    {
        // Place the whole thing in the Engine's base panel.
        context_.setMainUIComponent(this, menu_, false, null);
    }

    /**
     * Perform auto install.
     */
    // Turned off for DD Poker 3
//    private class AutoInstall implements Runnable
//    {
//        public void run()
//        {
//            // Sleep to let the UI show.
//            Utils.sleepMillis(500);
//            SwingUtilities.invokeLater(new Runnable()
//            {
//                public void run()
//                {
//                    installButton_.doClick();
//                }
//            });
//        }
//    }

    /**
     * Check for update or install an update.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public boolean processButton(GameButton button)
    {
        if (button.getName().equals(checkButton_.getName()))
        {
            // Check for an update.
            UpdateDownloader downloader = new UpdateDownloader(null);
            EngineMessage msg = downloader.checkUpdate(context_, true);

            nCheckTime_ = System.currentTimeMillis();
            prefs_.putLong(PREF_CHECKTIME, nCheckTime_);

            setStatusMessage();

            // Keep the current screen if the server request failed.
            if (msg != null)
            {
                // Report release availability.
                releases_ = (DMArrayList<PatchRelease>) msg.getObject(EngineMessage.PARAM_PATCH_RELEASES);

                if (releases_ != null)
                {
                    PatchRelease release = releases_.get(0);
                    String desc = release.getDescription();

                    if (desc == null)
                    {
                        desc = PropertyConfig.getMessage("msg.update.available", engine_.getVersion());
                    }

                    setInfoMessage("msg.update.install", desc);

                    buttonbox_.removeButton(checkButton_);
                    buttonbox_.removeButton(cancelButton_);
                    buttonbox_.addButton(installButton_);
                    buttonbox_.addButton(cancelButton_);
                }
                else
                {
                    setInfoMessage("msg.update.none", null);

                    buttonbox_.removeButton(checkButton_);
                    buttonbox_.removeButton(cancelButton_);
                    buttonbox_.addButton(returnButton_);
                }

                buttonbox_.revalidate();
            }

            return false;
        }
        else if (button.getName().equals(installButton_.getName()))
        {
            // Install the update in a separate dialog.
            TypedHashMap hmParams = new TypedHashMap();
            hmParams.setObject(UpdateDialog.PARAM_RELEASES, releases_);

            UpdateDialog dialog = (UpdateDialog) context_.processPhaseNow("UpdateDialog", hmParams);

            // Keep the current screen if the server request failed.
            if (dialog.getStatus() == UpdateDialog.STATUS_OK)
            {
                // Update the install time and report success.
                PatchRelease release = releases_.get(0);

                nInstallTime_ = System.currentTimeMillis();
                prefs_.putLong(PREF_INSTALLTIME, nInstallTime_);

                setStatusMessage();
                setInfoMessage("msg.update.success", release.getVersion());

                buttonbox_.removeButton(installButton_);
                buttonbox_.removeButton(cancelButton_);
                buttonbox_.addButton(closeButton_);

                buttonbox_.revalidate();
            }

            return false;
        }
        else if (button.getName().equals(closeButton_.getName()))
        {
            // Close the game.
            System.exit(0);
        }
        else
        {
            // If other button press, process TO-DO phase if that is defined
            // (from activation) and return false to prevent normal processing.
            if (context_.hasTODO())
            {
                context_.processTODO();
                return false;
            }

        }

        return true;
    }

    /**
     * Set the info message.
     */
    private void setInfoMessage(String sMsg, Object oParam)
    {
        infoText_.setText(Utils.fixHtmlTextFor15(PropertyConfig.getMessage(sMsg, oParam)));
    }

    /**
     * Set the status message.
     */
    private void setStatusMessage()
    {
        String sCheckParam = null;
        String sUpdateParam = null;

        if (nCheckTime_ == -1)
        {
            sCheckParam = PropertyConfig.getMessage("msg.update.never");
        }
        else
        {
            sCheckParam = PropertyConfig.getDateFormat(engine_.getLocale()).format(new Date(nCheckTime_));
        }

        if (nInstallTime_ == -1)
        {
            sUpdateParam = PropertyConfig.getMessage("msg.update.never");
        }
        else
        {
            sUpdateParam = PropertyConfig.getDateFormat(engine_.getLocale()).format(new Date(nInstallTime_));
        }

        String sText = PropertyConfig.getMessage("msg.update.status",
                                                 engine_.getVersion(),
                                                 sCheckParam,
                                                 sUpdateParam);

        statusText_.setText(sText);
    }
}
