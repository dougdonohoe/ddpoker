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
 * PokerStartMenu.java
 *
 * Created on November 10, 2004, 9:39 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.Prefs;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.gui.*;
import com.zookitec.layout.ComponentEF;
import com.zookitec.layout.ExplicitConstraints;
import com.zookitec.layout.ExplicitLayout;
import com.zookitec.layout.MathEF;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * @author donohoe
 */
@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
public class PokerStartMenu extends StartMenu
{
    //private static final Logger logger = Logger.getLogger(PokerStartMenu.class);

    private static boolean messageCheck = true;
    private static boolean firstTimeMusic = true;

    private PlayerProfile profile_ = null;
    private DDLabel label_;
    private boolean bProfileCheck_ = false;

    /**
     * Creates a new instance of PokerStartMenu
     */
    public PokerStartMenu()
    {
    }

    /**
     * Layout the container - place text where desired and add other
     * components
     */
    @Override
    protected void layoutMenu(DDPanel base, JComponent helptext)
    {
        // if expired or regular menu, let super handle
        if (bExpired_)
        {
            super.layoutMenu(base, helptext);
            return;
        }

        ////
        //// big suit buttons

        DDPanel bigbuttons = new DDPanel();
        base.add(bigbuttons, BorderLayout.WEST);
        ExplicitLayout elayout = new ExplicitLayout();
        bigbuttons.setLayout(elayout);
        addBigButton(bigbuttons, "practice", -18, 85);
        addBigButton(bigbuttons, "analysis", 240, 85);
        addBigButton(bigbuttons, "pokerclock", 102 + 3, 205);
        addBigButton(bigbuttons, "online", 102, 0);
        elayout.setPreferredLayoutSize(MathEF.constant(430), MathEF.constant(450));

        ////
        //// right side controls
        ////

        DDPanel rightbase = new DDPanel();
        rightbase.setBorderLayoutGap(10, 0);
        base.add(rightbase, BorderLayout.CENTER);

        DDPanel controlbase = new DDPanel();
        controlbase.setBorderLayoutGap(7, 0);
        controlbase.setBorder(new DDBevelBorder("BrushedMetal", DDBevelBorder.LOWERED));
        rightbase.add(controlbase, BorderLayout.CENTER);
        controlbase.add(helptext, BorderLayout.CENTER);

        DDPanel ctrlbuttonbase = new DDPanel();
        controlbase.add(GuiUtils.CENTER(ctrlbuttonbase), BorderLayout.NORTH);
        ctrlbuttonbase.setLayout(new GridLayout(1, 4, 0, 0));
        addControlButton(ctrlbuttonbase, "exit");
        addControlButton(ctrlbuttonbase, "calc");
        addControlButton(ctrlbuttonbase, "options");
        if (!engine_.isDemo())
        {
            addControlButton(ctrlbuttonbase, "register");
        }
        else
        {
            addControlButton(ctrlbuttonbase, "order");
        }
        addControlButton(ctrlbuttonbase, "support");
        addControlButton(ctrlbuttonbase, "help");


        ////
        //// player profile
        ////

        // set flag indicating we should do a profile check
        bProfileCheck_ = true;

        // base panel setup
        DDPanel playerInfo = new DDPanel(GuiManager.DEFAULT, STYLE);
        playerInfo.setBorderLayoutGap(0, 5);
        playerInfo.setBorder(BorderFactory.createCompoundBorder(
                new DDBevelBorder("BrushedMetal", DDBevelBorder.RAISED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5))
        );

        // add to bottom of menu's center panel (mouse over text is in CENTER)
        rightbase.add(playerInfo, BorderLayout.SOUTH);

        // current profile info
        label_ = new InitLabel(GuiManager.DEFAULT, "ProfileSummary");
        playerInfo.add(label_, BorderLayout.CENTER);
        label_.setText(PropertyConfig.getMessage("msg.profile.empty"));
        label_.setVerticalAlignment(SwingConstants.TOP);
        label_.setHorizontalAlignment(SwingConstants.LEFT);

        // button
        EngineButtonListener listener = new EngineButtonListener(context_, this, gamephase_.getButtonNameFromParam("profile"));
        DDImageButton button = new DDImageButton(listener.getGameButton().getName());
        button.addActionListener(listener);
        playerInfo.add(GuiUtils.NORTH(button), BorderLayout.WEST);

        // get profile
        profile_ = PlayerProfileOptions.getDefaultProfile();
    }

    private void addControlButton(JComponent parent, String sName)
    {
        EngineButtonListener listener = new EngineButtonListener(context_, this, gamephase_.getButtonNameFromParam(sName));
        DDImageButton practice = new DDImageButton(listener.getGameButton().getName());
        practice.addActionListener(listener);
        parent.add(practice);
    }

    private void addBigButton(JComponent parent, String sName,
                              int x, int y)
    {
        EngineButtonListener listener = new EngineButtonListener(context_, this, gamephase_.getButtonNameFromParam(sName));
        DDImageButton practice = new DDImageButton(listener.getGameButton().getName());
        practice.addActionListener(listener);
        parent.add(practice, new ExplicitConstraints(practice,
                                                     MathEF.constant(x), MathEF.constant(y),
                                                     ComponentEF.preferredWidth(practice),
                                                     ComponentEF.preferredHeight(practice)));
    }

    @Override
    protected javax.swing.border.Border getHelpTextBorder()
    {
        return BorderFactory.createEmptyBorder(0, 10, 5, 10);
    }

    /**
     * we override to do nothing
     */
    @Override
    protected void addButtons(DDPanel parent)
    {
        // if expired or regular menu, let super handle
        if (bExpired_)
        {
            super.addButtons(parent);
        }

        // otherwise we don't use the standard MenuPhase button positions
    }

    /**
     * Returns false if profile check hasn't happened
     */
    @Override
    public boolean processButton(GameButton button)
    {
        // if we need to do a profile check, skip button presses to
        // avoid processing Enter key press right after startup
        // and before dialog displayed        
        if (bProfileCheck_) return false;
        return super.processButton(button);
    }

    /**
     * license agreement check
     */
    private void licenseCheck()
    {
        String key = "agreed";
        Preferences prefs = Prefs.getUserPrefs("license");
        if (!prefs.getBoolean(key, false))
        {
            License lic = (License) context_.processPhaseNow("License", null);

            GameButton result = (GameButton) lic.getResult();
            if (result.getName().startsWith("yes"))
            {
                prefs.putBoolean(key, true);
            }
            else
            {
                System.exit(0);
            }
        }
    }


    /**
     * profile check
     */
    private void profileCheck()
    {
        // Check the profile.
        if (profile_ == null)
        {
            ProfileList list = PlayerProfileOptions.getPlayerProfileList(engine_, context_);
            profile_ = (PlayerProfile) list.newProfile("startmenu");
            if (profile_ != null)
            {
                list.rememberProfile(profile_);
            }
        }
        updateProfileLabel();
        bProfileCheck_ = false;

        // play background music
        if (profile_ != null && isStartMenu())
        {
            EngineUtils.startBackgroundMusic(gamephase_, firstTimeMusic);
            firstTimeMusic = false;
        }

        // Only do server code if enabled
        boolean enabled = engine_.getPrefsNode().getBooleanOption(EngineConstants.OPTION_ONLINE_ENABLED);

        // Preform message check the first time the start menu is displayed.
        if (enabled && messageCheck && profile_ != null && engine_.getPrefsNode().getBoolean(PokerConstants.OPTION_AUTO_CHECK_UPDATE, true))
        {
            String LASTMSG_KEY = "lastmsg";

            // get last msg key and check for update
            Preferences prefs = DDOption.getOptionPrefs(engine_.getPrefsNodeName() + "/ddmsg");
            long last = prefs.getLong(LASTMSG_KEY, 0);

            EngineMessage msg = DDMessageCheck.checkUpdate(context_, last, profile_.getName());

            if (msg != null && msg.getString(EngineMessage.PARAM_DDMSG) != null)
            {
                prefs.putLong(LASTMSG_KEY, msg.getLong(EngineMessage.PARAM_DDMSG_ID));
                String userMessage = msg.getString(EngineMessage.PARAM_DDMSG);
                PokerUtils.displayInformationDialog(context_, userMessage, "msg.windowtitle.ddmsgMessage", null);
            }
        }

        messageCheck = false;
    }

    /**
     * Update profile label
     */
    private void updateProfileLabel()
    {
        if (profile_ != null)
        {
            int nPrize = profile_.getTotalPrizeMoneyEarned();
            int nSpent = profile_.getTotalMoneySpent();
            int nProfit = nPrize - nSpent;
            label_.setText(PropertyConfig.getMessage("msg.profile.summary",
                                                     Utils.encodeHTML(profile_.getName()),
                                                     nSpent,
                                                     nPrize,
                                                     nProfit));
            label_.repaint();
        }
    }

    /**
     * Private class used to ensure StartMenu is displayed
     * before profileCheck() is called
     */
    private class InitLabel extends DDLabel
    {
        boolean bCheck = true;

        public InitLabel(String sName, String sStyle)
        {
            super(sName, sStyle);
        }

        @Override
        public void paintComponent(Graphics g1)
        {
            super.paintComponent(g1);
            if (bProfileCheck_ && bCheck)
            {
                bCheck = false;
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                licenseCheck();
                                profileCheck();
                            }
                        }
                );
            }
        }
    }

}
