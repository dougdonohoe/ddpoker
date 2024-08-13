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
 * UpdateDialog.java
 *
 * Created on January 25, 2003, 10:11 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;
import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 *
 * @author  Doug Donohoe
 */
public class UpdateDialog extends DialogPhase
{
    static Logger logger = Logger.getLogger(UpdateDialog.class);

    public static final String PARAM_TYPE = "type";
    public static final String PARAM_RELEASES = "releases";

    public static final int STATUS_ERROR = 0;
    public static final int STATUS_OK = 1;

    private DMArrayList releases_ = null;

    private int status_ = STATUS_OK;

    private DDLabel infoText_ = null;
    private DDProgressBar progressBar_ = null;

    /**
     * create chat ui
     */
    public JComponent createDialogContents()
    {
        // Releases to install.
        releases_ = (DMArrayList) gamephase_.getObject(PARAM_RELEASES);

        // contents
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));

        infoText_ = new DDLabel(GuiManager.DEFAULT, "UpdateRequest");
        base.add(GuiUtils.CENTER(infoText_), BorderLayout.NORTH);

        progressBar_ = new DDProgressBar(GuiManager.DEFAULT, "UpdateRequest");
        progressBar_.setPreferredSize(new Dimension(300, 50));
        progressBar_.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        base.add(GuiUtils.CENTER(progressBar_), BorderLayout.CENTER);

        // Keep OK button disabled until the request completes.
        okayButton_.setEnabled(false);

        // Start the install.
        Thread t = new Thread(new Installer(), "UpdateDialog");
        t.start();

        return base;
    }

    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return null;
    }

    /**
     * Closes the dialog unless an error occurs saving the profile information
     */
    public boolean processButton(GameButton button)
    {

        removeDialog();
        setResult(Boolean.TRUE);

        return true;
    }

    public int getStatus()
    {
        return status_;
    }

    /**
     * Set the request info message.
     */
    private void setInfoMessage(String sMsg, Object param)
    {
        infoText_.setText(PropertyConfig.getMessage(sMsg, param));
    }

    /**
     * Updates the progress bar during download.
     */
    private class DownloadListener implements UpdateDownloader.Listener
    {
        /**
         * Updates the progress bar percent value.
         *
         * @param percent percent read
         */
        public void setPercentDone(int percent)
        {
            progressBar_.setPercentDone(percent);
        }
    }

    /**
     * Updates the progress bar during installation.
     */
    private class InstallListener implements UpdateInstaller.Listener
    {
        /**
         * Updates the progress bar percent value.
         *
         * @param percent percent read
         */
        public void setPercentDone(int percent)
        {
            progressBar_.setPercentDone(percent);
        }
    }

    private class Installer implements Runnable {

        public void run()
        {
            // Download and install the releases in dependency order (most recent release is at the front of the list).
            int releaseCount = releases_.size();
            PatchRelease release = null;

            // Download the releases.
            for (int i = (releaseCount - 1); i >= 0; --i)
            {
                release = (PatchRelease) releases_.get(i);

                if (!download(release))
                {
                    okayButton_.setEnabled(true);
                    return;
                }
            }

            // Install the releases.
            GameEngine engine = GameEngine.getGameEngine();
            Version version = engine.getVersion();

            for (int i = (releaseCount - 1); i >= 0; --i)
            {
                release = (PatchRelease) releases_.get(i);

                if (!install(version, release))
                {
                    okayButton_.setEnabled(true);
                    return;
                }

                version = release.getVersion();
            }

            okayButton_.setEnabled(true);
        }

        /**
         * Download the given release.
         *
         * @param release release
         *
         * @return <code>true</code> if the release was downloaded successfully, <code>false</code> otherwise
         */
        private boolean download(final PatchRelease release)
        {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        setInfoMessage("msg.update.request.download", release.getVersion());
                        progressBar_.setPercentDone(0);
                    }
                }
            );

            try
            {
                UpdateDownloader downloader = new UpdateDownloader(new DownloadListener());
                downloader.downloadUpdate(release);
            }
            catch (Throwable t)
            {
                status_ = STATUS_ERROR;
                reportError("msg.update.request.downloadError", t);
                return false;
            }

            return true;
        }

        /**
         * Install the given release.
         *
         * @param version version
         * @param release release
         *
         * @return <code>true</code> if the release was installed successfully, <code>false</code> otherwise
         */
        private boolean install(final Version version, final PatchRelease release)
        {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        setInfoMessage("msg.update.request.install", release.getVersion());
                        progressBar_.setPercentDone(0);
                    }
                }
            );

            try
            {
                File file = UpdateDownloader.getDownloadFile(release);
                UpdateInstaller installer = new UpdateInstaller(new InstallListener());
                installer.installUpdate(version, release, file);
                status_ = STATUS_OK;
                reportSuccess();
            }
            catch (Throwable t)
            {
                status_ = STATUS_ERROR;
                reportError("msg.update.request.installError", t);
                return false;
            }

            return true;
        }
    }

    /**
     * Report a request success.
     */
    private void reportSuccess()
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    setInfoMessage("msg.update.request.success", null);
                }
            }
        );
    }

    /**
     * Report a request error.
     */
    private void reportError(final String message, Throwable t)
    {
        logger.debug("Error: " + t.getClass().getName());
        if (t instanceof ApplicationError) {
            logger.error(t.toString());
        } else {
            logger.error(Utils.formatExceptionText(t));
        }

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    setInfoMessage(message, null);
                    repaint();
                }
            }
        );
    }
}
