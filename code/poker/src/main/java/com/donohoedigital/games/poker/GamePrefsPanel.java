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
 * GamePrefsPanel.java
 *
 * Created on April 16, 2003, 1:16 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.AudioConfig;
import com.donohoedigital.config.DebugConfig;
import com.donohoedigital.config.Prefs;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.gui.HandSelectionManager;
import com.donohoedigital.games.poker.ai.gui.PlayerTypeManager;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.online.GetPublicIP;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Doug Donohoe
 */
public class GamePrefsPanel extends DDPanel implements ActionListener
{
    static Logger logger = LogManager.getLogger(GamePrefsPanel.class);

    public static final Integer ICWIDTH = 20;
    public static final Integer ICHEIGHT = 30;


    private final TypedHashMap map_ = new TypedHashMap();
    private final GameEngine engine_;
    private final GameContext context_;
    private final String OSTYLE;
    private final String BSTYLE;
    private DDTabbedPane tabs_;
    private final GuiUtils.CheckListener checkListeners_;
    private OptionText onlineServer_;
    private OptionText chatServer_;
    private OptionBoolean onlineEnabled_;
    private GlassButton test_;

    private final String NODE;
    private int GRIDADJUST2;
    private int GRIDADJUST1;
    private int GRIDADJUST3;

    public GamePrefsPanel(GameEngine engine, GameContext context, String sOptionStyle,
                          String sBevelStyle, boolean bDialog, GuiUtils.CheckListener checkListeners)
    {
        engine_ = engine;
        context_ = context;
        OSTYLE = sOptionStyle;
        BSTYLE = sBevelStyle;
        NODE = engine.getPrefsNodeName();
        checkListeners_ = checkListeners;
        getOptions(bDialog);
    }

    private void getOptions(boolean bDialog)
    {

        GRIDADJUST2 = -4;
        GRIDADJUST1 = bDialog ? -6 : -8;
        GRIDADJUST3 = 4;

        tabs_ = new DDTabbedPane(OSTYLE, BSTYLE, JTabbedPane.TOP);
        tabs_.setOpaque(false);
        if (bDialog)
        {
            tabs_.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        }

        add(tabs_, BorderLayout.CENTER);

        ImageComponent ic = new ImageComponent("ddlogo20", 1.0d);
        ImageComponent error = new ImageComponent("taberror", 1.0d);

        ic.setScaleToFit(false);
        error.setScaleToFit(false);

        if (!bDialog)
        {
            ic.setIconWidth(ICWIDTH);
            ic.setIconHeight(ICHEIGHT);
            error.setIconWidth(ICWIDTH);
            error.setIconHeight(ICHEIGHT);
        }

        PokerGame game = (PokerGame) context_.getGame();

        GeneralOptions goptions = new GeneralOptions();
        goptions.createUI(); // first tab, so create now

        tabs_.addTab("msg.options.general", ic, error, goptions);

        if (!bDialog || game == null || (!game.isOnlineGame() && !game.isClockMode()))
        {
            tabs_.addTab("msg.options.practice", ic, error, new PracticeOptions());
        }
        if (!bDialog || game == null || game.isOnlineGame())
        {
            tabs_.addTab("msg.options.online", ic, error, new OnlineOptions());
        }
        if (!bDialog || game == null || game.isClockMode())
        {
            tabs_.addTab("msg.options.clock", ic, error, new ClockOptions());
        }
        if (!bDialog || game == null || !game.isClockMode())
        {
            tabs_.addTab("msg.options.deck", ic, error, new DeckBack());
            tabs_.addTab("msg.options.table", ic, error, new TableDesigns());
        }

        // in-game, leave out playertypes, starting hands (for now)
        if (!bDialog || game == null || (!game.isOnlineGame() && !game.isClockMode()))
        {
            tabs_.addTab("msg.options.playertypes", ic, error, new PlayerTypes());
            tabs_.addTab("msg.options.startinghands", ic, error, new StartingHands());
            tabs_.addTab("msg.options.handgroups", ic, error, new HandGroups());
        }

        // select practice tab if in a game, which happens to be the tab at position 1
        if (bDialog && game != null && tabs_.getTabCount() > 1)
        {
            tabs_.setSelectedIndex(1);
        }
    }

    /**
     * return whether data is valid on all tabs
     */
    public boolean isValidData()
    {
        return tabs_.doValidCheck();
    }

    /**
     * Base class for tabs which use options
     */
    private abstract class OptionTab extends DDTabPanel
    {
        private final List<DDOption> localOptions = new ArrayList<DDOption>();

        OptionTab()
        {
            setPreferredSize(new Dimension(700, 360));
        }

        @Override
        public void createUI()
        {
            createUILocal();
            GuiUtils.getDDOptions(this, localOptions);
            checkListeners_.addListeners(localOptions);
        }

        protected abstract void createUILocal();
    }

    /**
     * Tournament options
     */
    private class GeneralOptions extends OptionTab
    {

        @Override
        protected void createUILocal()
        {
            DDPanel base = new DDPanel();
            base.setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.LEFT, 10, 0, HorizontalFlowLayout.TOP));
            add(GuiUtils.WEST(base), BorderLayout.NORTH);

            DDPanel leftside = new DDPanel();
            leftside.setBorderLayoutGap(10, 0);
            base.add(leftside);

            DDLabelBorder generalbase = new DDLabelBorder("general", OSTYLE);
            generalbase.setBorderLayoutGap(4, 0);
            leftside.add(generalbase, BorderLayout.NORTH);

            DDPanel generalbasetop = new DDPanel();
            generalbasetop.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));
            generalbasetop.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 8));
            generalbase.add(generalbasetop, BorderLayout.NORTH);

            DDButton resetdialog = new GlassButton("resetdialog", "Glass");
            resetdialog.addActionListener(GamePrefsPanel.this);
            resetdialog.setBorderGap(2, 5, 2, 6);

            DDPanel buttonbase = new DDPanel();
            buttonbase.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));
            buttonbase.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));
            buttonbase.add(resetdialog);
            generalbase.add(buttonbase, BorderLayout.SOUTH);

            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_LARGE_CARDS, OSTYLE, map_, true), generalbasetop);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_FOUR_COLOR_DECK, OSTYLE, map_, true), generalbasetop);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_STYLIZED_FACE_CARDS, OSTYLE, map_, true), generalbasetop);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_HOLE_CARDS_DOWN, OSTYLE, map_, true), generalbasetop);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_CHECKFOLD, OSTYLE, map_, true), generalbasetop);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_RIGHT_CLICK_ONLY, OSTYLE, map_, true), generalbasetop);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_DISABLE_SHORTCUTS, OSTYLE, map_, true), generalbasetop);

            if (!engine_.isDemo()) // no auto update in the demo
            {
                OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_AUTO_CHECK_UPDATE, OSTYLE, map_, true), generalbasetop);
            }

            // screen mode
            // Turned off screen-mode in DD Poker 3 since no one uses it, and it doesn't work on Vista
//            if (!Utils.ISLINUX && !Utils.ISMAC) // no window options on linux or mac
//            {
//                DDLabelBorder modebase = null;
//                modebase = new DDLabelBorder("mode", OSTYLE);
//                modebase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));
//                ButtonGroup modegroup = new ButtonGroup();
//                OptionMenu.add(new OptionRadio(NODE, EngineConstants.PREF_WINDOW_MODE, OSTYLE, map_, "mode.always", modegroup, EngineConstants.MODE_ASK), modebase);
//                OptionMenu.add(new OptionRadio(NODE, EngineConstants.PREF_WINDOW_MODE, OSTYLE, map_, "mode.window", modegroup, EngineConstants.MODE_WINDOW), modebase);
//                OptionMenu.add(new OptionRadio(NODE, EngineConstants.PREF_WINDOW_MODE, OSTYLE, map_, "mode.full", modegroup, EngineConstants.MODE_FULL), modebase);
//
//                leftside.add(modebase, BorderLayout.CENTER);
//            }

            ////
            //// RIGHT side - audio/chat
            ////

            DDPanel rightbase = new DDPanel();
            rightbase.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, HorizontalFlowLayout.LEFT));
            base.add(rightbase);

            ///
            /// AUDIO
            ///

            // sound
            DDLabelBorder audiobase = new DDLabelBorder("audio", OSTYLE);
            audiobase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST2));

            Dimension size;
            OptionBoolean music, fx;//, battle;

            // fx
            OptionInteger fxvolume = new OptionInteger(NODE, EngineConstants.PREF_FX_VOL, OSTYLE, map_, null, 5, 100, 60);
            fxvolume.setEditable(true);
            fx = OptionMenu.add(new OptionBoolean(NODE, EngineConstants.PREF_FX, OSTYLE, map_, true, fxvolume), audiobase);

            fx.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    DDCheckBox box = ((OptionBoolean) e.getSource()).getCheckBox();
                    AudioConfig.setMuteFX(!box.isSelected());
                    playFX();
                }
            });

            fxvolume.getSpinner().addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    DDNumberSpinner spinner = (DDNumberSpinner) e.getSource();
                    AudioConfig.setFXGain(spinner.getValue());
                    playFX();
                }
            });

            // background music

            OptionInteger muvolume = new OptionInteger(NODE, EngineConstants.PREF_BGMUSIC_VOL, OSTYLE, map_, null, 5, 100, 60);
            muvolume.setEditable(true);
            music = OptionMenu.add(new OptionBoolean(NODE, EngineConstants.PREF_BGMUSIC, OSTYLE, map_, true, muvolume), audiobase);

            music.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    DDCheckBox box = ((OptionBoolean) e.getSource()).getCheckBox();
                    AudioConfig.setMuteBGMusic(!box.isSelected());
                }
            });

            muvolume.getSpinner().addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {

                    DDNumberSpinner spinner = (DDNumberSpinner) e.getSource();
                    AudioConfig.setBGMusicGain(spinner.getValue());
                }
            });

            // size fx, music to same
            size = music.getCheckBox().getPreferredSize();
            size.width += 10;

            fx.getCheckBox().setPreferredSize(size);
            music.getCheckBox().setPreferredSize(size);

            rightbase.add(audiobase);

            ///
            /// CHAT
            ///

            // messages
            DDLabelBorder chatbase = new DDLabelBorder("chatoptions", OSTYLE);
            chatbase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));
            ButtonGroup chatgroup = new ButtonGroup();
            OptionMenu.add(new OptionRadio(NODE, PokerConstants.OPTION_CHAT_DEALER, OSTYLE, map_, "dealer.all", chatgroup, PokerConstants.DEALER_ALL), chatbase);
            OptionMenu.add(new OptionRadio(NODE, PokerConstants.OPTION_CHAT_DEALER, OSTYLE, map_, "dealer.noaction", chatgroup, PokerConstants.DEALER_NO_PLAYER_ACTION), chatbase);
            OptionMenu.add(new OptionRadio(NODE, PokerConstants.OPTION_CHAT_DEALER, OSTYLE, map_, "dealer.none", chatgroup, PokerConstants.DEALER_NONE), chatbase);

            DDPanel spacer = new DDPanel();
            spacer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            spacer.add(chatbase, BorderLayout.NORTH);
            rightbase.add(spacer);

            ///
            /// SCREENSHOT
            ///

            OptionInteger oi;
            DDLabelBorder screenbase = new DDLabelBorder("screenshot", OSTYLE);

            screenbase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST3));

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_SCREENSHOT_MAX_WIDTH, OSTYLE, map_, null, 640, 2560, 70), screenbase);
            oi.setEditable(true);

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_SCREENSHOT_MAX_HEIGHT, OSTYLE, map_, null, 480, 1600, 70), screenbase);
            oi.setEditable(true);

            leftside.add(screenbase);
        }
    }

    /**
     * Practice options
     */
    private class PracticeOptions extends OptionTab
    {
        @Override
        protected void createUILocal()
        {
            DDPanel base = new DDPanel();
            base.setBorderLayoutGap(10, 0);
            add(GuiUtils.WEST(base), BorderLayout.NORTH);

            DDPanel topside = new DDPanel();
            topside.setBorderLayoutGap(0, 10);
            base.add(topside, BorderLayout.NORTH);

            // practice
            DDLabelBorder practicebase = new DDLabelBorder("practice", OSTYLE);
            topside.add(practicebase, BorderLayout.WEST);
            practicebase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));

            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_AUTODEAL, OSTYLE, map_, true), practicebase);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_PAUSE_ALLIN, OSTYLE, map_, true), practicebase);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_PAUSE_COLOR, OSTYLE, map_, true), practicebase);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ZIP_MODE, OSTYLE, map_, true), practicebase);
            OptionMenu.add(new OptionBoolean(NODE, EngineConstants.PREF_AUTOSAVE, OSTYLE, map_, true), practicebase);

            // delay
            OptionInteger oi;
            DDLabelBorder delaybase = new DDLabelBorder("delayopt", OSTYLE);
            topside.add(GuiUtils.WEST(delaybase), BorderLayout.CENTER);

            delaybase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST3));

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_DELAY, OSTYLE, map_, null, 0, 40, 55), delaybase);
            oi.setEditable(true);

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_AUTODEALHAND, OSTYLE, map_, null, 0, 100, 55), delaybase);
            oi.setEditable(true);

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_AUTODEALFOLD, OSTYLE, map_, null, 0, 100, 55), delaybase);
            oi.setEditable(true);

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_HANDS_PER_HOUR, OSTYLE, map_, null, 10, 250, 55), delaybase);
            oi.setEditable(true);

            // cheat
            DDLabelBorder cheatbase = new DDLabelBorder("cheat", OSTYLE);
            cheatbase.setLayout(new GridLayout(0, 2, 10, GRIDADJUST1));
            addCheatOptions(NODE, cheatbase, OSTYLE, map_, true);

            base.add(GuiUtils.NORTH(cheatbase), BorderLayout.CENTER);
        }
    }

    public static void addCheatOptions(String node, JPanel cheatbase, String ostyle, TypedHashMap map, boolean b2Cols)
    {
        //  (indented items are in 2nd column)
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_POPUP, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_MOUSEOVER, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_SHOWWINNINGHAND, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_AIFACEUP, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_SHOWFOLD, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_PAUSECARDS, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_SHOW_MUCKED, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_MANUAL_BUTTON, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_RABBITHUNT, ostyle, map, true), cheatbase);
        OptionMenu.add(new OptionBoolean(node, PokerConstants.OPTION_CHEAT_NEVERBROKE, ostyle, map, true), cheatbase);

        if (!b2Cols)
        {
            moveToEnd(cheatbase, 1);
            moveToEnd(cheatbase, 2);
            moveToEnd(cheatbase, 3);
            moveToEnd(cheatbase, 4);
            moveToEnd(cheatbase, 5);
        }
    }

    private static void moveToEnd(JPanel cheatbase, int i)
    {
        Component c = cheatbase.getComponent(i);
        cheatbase.remove(i);
        cheatbase.add(c);
    }

    /**
     * Online options
     */
    private class OnlineOptions extends OptionTab
    {
        public static final int ONLINE_SERVER_LIMIT = 50;
        public static final String ONLINE_SERVER_REGEXP = // server.domain.com:port or ip:port
           "^(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b):\\d{1,5}$";

        @Override
        protected void createUILocal()
        {
            DDPanel base = new DDPanel();
            add(GuiUtils.WEST(base), BorderLayout.NORTH);
            base.setBorderLayoutGap(0, 10);

            DDPanel leftbase = new DDPanel();
            leftbase.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, VerticalFlowLayout.LEFT));
            base.add(leftbase, BorderLayout.CENTER);

            // delay
            DDLabelBorder delaybase = new DDLabelBorder("delayopt", OSTYLE);
            leftbase.add(delaybase);

            delaybase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST3));

            OptionInteger oi;
            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_AUTODEALONLINE, OSTYLE, map_, null, 0, 100, 55), delaybase);
            oi.setEditable(true);

            oi = OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_ONLINESTART, OSTYLE, map_, null, 1, 25, 55), delaybase);
            oi.setEditable(true);

            // other/misc
            DDLabelBorder misc = new DDLabelBorder("onlineother", OSTYLE);
            leftbase.add(misc);

            DDPanel miscoptions = new DDPanel();
            miscoptions.setBorderLayoutGap(4, 0);
            misc.add(miscoptions, BorderLayout.CENTER);

            miscoptions.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));

            OptionBoolean ob = OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ONLINE_UDP, OSTYLE, map_, true), miscoptions);
            // TODO: turn off
            if (!DebugConfig.isTestingOn())
            {
                ob.getCheckBox().setSelected(false);
                ob.getCheckBox().setToolTipText("Hosting via UDP is reserved for DD Poker personnel only while testing continues.");
                ob.setEnabled(false);
            }
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ONLINE_PAUSE, OSTYLE, map_, true), miscoptions);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ONLINE_PAUSE_ALL_DISCONNECTED, OSTYLE, map_, true), miscoptions);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ONLINE_COUNTDOWN, OSTYLE, map_, true), miscoptions);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ONLINE_AUDIO, OSTYLE, map_, true), miscoptions);
            if (Utils.ISWINDOWS)
                OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_ONLINE_FRONT, OSTYLE, map_, true), miscoptions);

            // buttons
            DDButton bannedplayers = new GlassButton("bannedplayers", "Glass");
            bannedplayers.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    context_.processPhaseNow("BannedPlayerList", null);
                }
            });
            bannedplayers.setBorderGap(2, 5, 2, 6);

            DDButton mutedplayers = new GlassButton("mutedplayers", "Glass");
            mutedplayers.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    context_.processPhaseNow("MutedPlayerList", null);
                }
            });
            mutedplayers.setBorderGap(2, 5, 2, 6);

            DDPanel buttonbase = new DDPanel();
            buttonbase.setLayout(new GridLayout(1, 0, 10, 0));
            buttonbase.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 2));
            buttonbase.add(bannedplayers);
            buttonbase.add(mutedplayers);
            misc.add(GuiUtils.CENTER(buttonbase), BorderLayout.SOUTH);

            // chat display
            DDLabelBorder dispbase = new DDLabelBorder("chatdisplay", OSTYLE);
            dispbase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));
            ButtonGroup chatgroup2 = new ButtonGroup();
            OptionMenu.add(new OptionRadio(NODE, PokerConstants.OPTION_CHAT_DISPLAY, OSTYLE, map_, "display.split", chatgroup2, PokerConstants.DISPLAY_SPLIT), dispbase);
            OptionMenu.add(new OptionRadio(NODE, PokerConstants.OPTION_CHAT_DISPLAY, OSTYLE, map_, "display.tab", chatgroup2, PokerConstants.DISPLAY_TAB), dispbase);
            OptionMenu.add(new OptionRadio(NODE, PokerConstants.OPTION_CHAT_DISPLAY, OSTYLE, map_, "display.one", chatgroup2, PokerConstants.DISPLAY_ONE), dispbase);

            // chat options
            DDLabelBorder detailbase = new DDLabelBorder("chatdetails", OSTYLE);
            detailbase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_CHAT_PLAYERS, OSTYLE, map_, true), detailbase);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_CHAT_OBSERVERS, OSTYLE, map_, true), detailbase);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_CHAT_TIMEOUT, OSTYLE, map_, true), detailbase);

            // put right side together
            DDPanel rightbase = new DDPanel();
            rightbase.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, VerticalFlowLayout.LEFT));
            rightbase.add(dispbase);
            rightbase.add(detailbase);
            base.add(GuiUtils.NORTH(rightbase), BorderLayout.EAST);

            //
            // online server section
            //

            DDLabelBorder serverBorder = new DDLabelBorder("onlineserver", OSTYLE);
            serverBorder.setLayout(new BorderLayout(10, 0));
            base.add(serverBorder, BorderLayout.SOUTH);

            // online enabled checkbox
            onlineEnabled_ = new OptionBoolean(NODE, EngineConstants.OPTION_ONLINE_ENABLED, OSTYLE, map_, true);
            serverBorder.add(GuiUtils.NORTH(onlineEnabled_), BorderLayout.WEST);
            onlineEnabled_.addChangeListener(e -> doOnlineEnabled());

            // servers list (online, chat)
            DDPanel serversTable = new DDPanel();
            serversTable.setLayout(new GridBagLayout());

            onlineServer_ = new OptionText(NODE, EngineConstants.OPTION_ONLINE_SERVER, OSTYLE, map_,
                    ONLINE_SERVER_LIMIT, ONLINE_SERVER_REGEXP, 400, true);
            chatServer_ = new OptionText(NODE, PokerConstants.OPTION_ONLINE_CHAT, OSTYLE, map_,
                    ONLINE_SERVER_LIMIT, ONLINE_SERVER_REGEXP, 400, true);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(2, 0, 2, 10);

            serversTable.add(onlineServer_, gbc);
            gbc.gridy = 1;
            serversTable.add(chatServer_, gbc);
            serverBorder.add(serversTable, BorderLayout.CENTER);

            // test button
            test_ = new GlassButton("testonline", "Glass");
            serverBorder.add(GuiUtils.CENTER(test_), BorderLayout.EAST);
            test_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    testConnection();
                }
            });

            // update text fields based on pref
            doOnlineEnabled();
        }

        private void doOnlineEnabled() {
            boolean enabled = onlineEnabled_.getCheckBox().isSelected();
            onlineServer_.setEnabled(enabled);
            chatServer_.setEnabled(enabled);
            test_.setEnabled(enabled);
        }
    }

    private void testConnection() {
        DMTypedHashMap params = new DMTypedHashMap();
        params.setBoolean(GetPublicIP.PARAM_TEST_SERVER, true);
        SendMessageDialog dialog = (SendMessageDialog) context_.processPhaseNow("GetPublicIP", params);
        if (dialog.getStatus() == DDMessageListener.STATUS_OK)
        {
            dialog.getReturnMessage().getString(EngineMessage.PARAM_IP);
        }
    }

    /**
     * Clock options
     */
    private class ClockOptions extends OptionTab
    {

        @Override
        protected void createUILocal()
        {
            DDLabelBorder cheatbase = new DDLabelBorder("clock", OSTYLE);
            cheatbase.setLayout(new GridLayout(0, 1, 0, GRIDADJUST1));
            add(GuiUtils.WEST(cheatbase), BorderLayout.NORTH);

            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_CLOCK_COLOUP, OSTYLE, map_, true), cheatbase);
            OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_CLOCK_PAUSE, OSTYLE, map_, true), cheatbase);
        }
    }


    /**
     * deck back
     */
    private class DeckBack extends OptionTab
    {
        @Override
        protected void createUILocal()
        {
            add(new DeckProfilePanel(engine_, context_, OSTYLE), BorderLayout.CENTER);
        }
    }

    /**
     * Screen options
     */
    private class PlayerTypes extends OptionTab
    {
        @Override
        protected void createUILocal()
        {
            add(new PlayerTypeManager(engine_, context_, OSTYLE), BorderLayout.CENTER);
        }
    }

    /**
     * Hand groups
     */
    private class HandGroups extends OptionTab
    {
        @Override
        protected void createUILocal()
        {
            add(new HandGroupManager(engine_, context_, OSTYLE), BorderLayout.CENTER);
        }
    }

    /**
     * Hand groups
     */
    private class TableDesigns extends OptionTab
    {
        @Override
        protected void createUILocal()
        {
            // run default profile logic (for first time usage)
            TableDesignManager.getDefaultProfile();
            add(new TableDesignManager(engine_, context_, OSTYLE), BorderLayout.CENTER);
        }
    }

    /**
     * Screen options
     */
    private class StartingHands extends OptionTab
    {

        @Override
        protected void createUILocal()
        {
            add(new HandSelectionManager(engine_, context_, OSTYLE), BorderLayout.CENTER);
        }
    }

    public JComponent getFocusComponent()
    {
        return tabs_;
    }

    /**
     * Reset dialog button
     */
    public void actionPerformed(ActionEvent e)
    {
        Preferences prefs = Prefs.getUserPrefs(EnginePrefs.NODE_DIALOG_PHASE);
        try
        {
            prefs.removeNode();
        }
        catch (BackingStoreException bse)
        {
            logger.warn("Unable to clear prefs for node: " + EnginePrefs.NODE_DIALOG_PHASE);
        }
        EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.resetdialog"));
    }

    /**
     * play fx sound after volume changed
     */
    private void playFX()
    {
        AudioConfig.playFX("preffx", 0);
    }
}
