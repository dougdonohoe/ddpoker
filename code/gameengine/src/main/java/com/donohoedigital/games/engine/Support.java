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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 11, 2005
 * Time: 1:50:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class Support extends OptionMenu
{
    private DDTextArea log_;

    /**
     * Create UI
     */
    @Override
    protected JComponent getOptions()
    {
        String STYLE = gamephase_.getString("menubox-style", "default");
        String sTextBorderStyle_ = gamephase_.getString("text-border-style", "default");

        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(10, 0);

        DDHtmlArea info = new DDHtmlArea("support", "Support");
        base.add(info, BorderLayout.NORTH);
        info.setText(Utils.fixHtmlTextFor15(PropertyConfig.getMessage("msg.support")));
        info.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        info.setPreferredSize(new Dimension(0, 135));
        info.addHyperlinkListener(GuiUtils.HYPERLINK_HANDLER);

        log_ = new DDTextArea("support", "Support");
        log_.setOpaque(true);
        log_.setEditable(false);
        log_.setFocusable(true);
        log_.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        updateLog();

        DDScrollPane scroll = new DDScrollPane(log_, STYLE, sTextBorderStyle_, DDScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                               DDScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        base.add(scroll, BorderLayout.CENTER);

        return base;
    }

    /**
     * set current log file into text field
     */
    private void updateLog()
    {
        Properties props = System.getProperties();
        StringBuilder data = new StringBuilder();

        data.append("------------------------------------------------------------------------------------\n");
        data.append("Activation Number: ").append(engine_.isDemo() ? "demo" : engine_.getRealLicenseKey()).append("\n");
        data.append("Version:           ").append(engine_.getVersion()).append("\n");
        data.append("Build Number:      ").append(PropertyConfig.getBuildNumber()).append("\n");
        data.append("Java Version:      ").append(props.getProperty("java.runtime.version")).append("\n");
        data.append("Operating System:  ").append(Utils.OS).append("\n");
        data.append("User directory:    ").append(ConfigManager.getUserHome()).append("\n");
        data.append("Free memory:       ").append(Runtime.getRuntime().freeMemory()).append("\n");
        data.append("Total memory:      ").append(Runtime.getRuntime().totalMemory()).append("\n");
        data.append("Max memory:        ").append(Runtime.getRuntime().maxMemory()).append("\n");
        data.append("------------------------------------------------------------------------------------\n");
        data.append("Log file:\n\n");

        File logFile = ConfigManager.getConfigManager().getLoggingConfig().getLogFile();
        if (logFile != null && logFile.exists() && logFile.isFile())
        {
            data.append(ConfigUtils.readFile(logFile));
        }
        else
        {
            data.append("No log file found.");
        }

        log_.setText(data.toString());
        log_.setCaretPosition(0);
    }

    /**
     * focus to log text area
     */
    @Override
    protected JComponent getFocusComponent()
    {
        return buttonbox_.getButton("copylog");
    }

    /**
     * help text area
     */
    @Override
    protected int getTextPreferredHeight()
    {
        return 40;
    }

    /**
     * clipboard
     */
    @Override
    @SuppressWarnings({"CallToRuntimeExecWithNonConstantString"})
    public boolean processButton(GameButton button)
    {
        if (button.getName().startsWith("copylog"))
        {
            doClipboard();
            return false;
        }
        else if (button.getName().startsWith("myfiles"))
        {
            File f = ConfigManager.getUserHome();
            if (Utils.ISMAC)
            {
                try
                {
                    Runtime.getRuntime().exec("/usr/bin/open " + f.getAbsolutePath());
                }
                catch (Throwable e)
                {
                    logger.error("Unable to exec: " + Utils.formatExceptionText(e));
                    EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.error.myfiles", f.getAbsolutePath()));
                }
            }
            else if (Utils.ISWINDOWS)
            {
                try
                {
                    Runtime.getRuntime().exec("explorer " + f.getAbsolutePath());
                }
                catch (Throwable e)
                {
                    logger.error("Unable to exec: " + Utils.formatExceptionText(e));
                    EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.error.myfiles", f.getAbsolutePath()));
                }
            }
            else if (Utils.ISLINUX)
            {
                try
                {
                    Runtime.getRuntime().exec("/usr/bin/gnome-open " + f.getAbsolutePath());
                }
                catch (Throwable e)
                {
                    logger.error("Unable to exec: " + Utils.formatExceptionText(e));
                    EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.error.myfiles", f.getAbsolutePath()));
                }
            }
        }
        return super.processButton(button);
    }

    /**
     * copy log file and other debug information to clipboard
     */
    private void doClipboard()
    {
        GuiUtils.copyToClipboard(log_.getText());
        EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.support.copied"), "support-copy");
    }
}
