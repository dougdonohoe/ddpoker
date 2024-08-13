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
 * OnlineConfiguration.java
 *
 * Created on November 17, 2004, 7:49 AM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.udp.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import static com.donohoedigital.config.DebugConfig.*;

/**
 * @author donohoe
 */
public class OnlineConfiguration extends BasePhase implements PropertyChangeListener
{
    static Logger logger = Logger.getLogger(OnlineConfiguration.class);

    private DDHtmlArea text_;
    private MenuBackground menu_;
    private DDButton start_;
    private String STYLE;
    private PokerGame game_;
    private PlayerProfile profile_;
    private boolean bTestPassed_ = false;
    private String sLastValidTest_ = null;

    private DDTextField pubText_;
    private DDTextField pubIPText_;
    private DDLabel pubLabel_;
    private DDLabel pubIPLabel_;
    private com.donohoedigital.gui.DDButton pubCopy_;
    private com.donohoedigital.gui.DDButton generate_;
    private com.donohoedigital.gui.DDButton test_;
    private DDCheckBox configurePublic_;
    private DDCheckBox listPublic_;
    private DDCheckBox hostAsObs_;

    /**
     * Creates a new instance of TournamentOptions
     */
    public OnlineConfiguration()
    {
    }

    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // wake alive thread to send message that we are almost ready
        ((PokerMain) engine).getLanManager().wakeAliveThread();

        // game
        game_ = (PokerGame) context_.getGame();

        // name of style used for all widgets in data area
        STYLE = gamephase_.getString("style", "default");

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        //menu_.addAncestorListener(this);
        DDPanel menubox = menu_.getMenuBox();
        String sHelpName = menu_.getHelpName();

        // put buttons in the menubox_
        ButtonBox buttonbox = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox.add(buttonbox, BorderLayout.SOUTH);
        start_ = buttonbox.getDefaultButton();

        // holds data we are gathering
        DDPanel data = new DDPanel(sHelpName);
        data.setBorderLayoutGap(10, 10);
        data.setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 10));
        menubox.add(data, BorderLayout.CENTER);

        // help text
        text_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());
        data.add(text_, BorderLayout.CENTER);

        // IP panel
        DDPanel iptop = new DDPanel();
        iptop.setBorderLayoutGap(10, 0);
        data.add(iptop, BorderLayout.NORTH);

        //**// LAN IP
        DDLabelBorder iplan = new DDLabelBorder("iplan", STYLE);
        iptop.add(iplan, BorderLayout.NORTH);
        DDPanel ippanel = new DDPanel();
        ippanel.setBorderLayoutGap(5, 0);
        ippanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
        iplan.add(ippanel, BorderLayout.CENTER);

        Widgets w = addIPField(ippanel, BorderLayout.NORTH, false);
        w.text.setText(game_.getLocalIP());
        w.text.setDisplayOnly(true);

        DDTextField lanText_ = addIPText("connect.lan", ippanel, BorderLayout.CENTER, STYLE, "BrushedMetal", game_).text;
        lanText_.setText(game_.getLanConnectURL());

        //**// Internet IP
        DDLabelBorder ippub = new DDLabelBorder("ippub", STYLE);
        iptop.add(ippub, BorderLayout.CENTER);
        ippanel = new DDPanel();
        ippanel.setBorderLayoutGap(5, 0);
        ippanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 2, 5));
        ippub.add(ippanel, BorderLayout.CENTER);

        DDPanel pbox = new DDPanel();
        pbox.setBorder(BorderFactory.createEmptyBorder(0, 15, 5, 0));
        pbox.setBorderLayoutGap(0, 10);
        ippub.add(pbox, BorderLayout.NORTH);
        configurePublic_ = new DDCheckBox("publicgame", STYLE);
        configurePublic_.setSelected(false);
        configurePublic_.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                doCheckBox();
            }
        });
        configurePublic_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (configurePublic_.isSelected())
                {
                    String sMsg = PropertyConfig.getMessage("msg.publicip.notice", "" + game_.getPort());
                    EngineUtils.displayInformationDialog(context_, sMsg, "PublicIP");
                }
            }
        });
        pbox.add(configurePublic_, BorderLayout.WEST);

        w = addIPField(ippanel, BorderLayout.NORTH, true);
        generate_ = w.button;
        pubIPLabel_ = w.label;
        pubIPText_ = w.text;
        pubIPText_.addPropertyChangeListener("value", this);

        w = addIPText("connect.pub", ippanel, BorderLayout.CENTER, STYLE, "BrushedMetal", game_);
        pubText_ = w.text;
        pubLabel_ = w.label;
        pubCopy_ = w.button;

        String con = game_.getPublicConnectURL();
        if (con != null)
            pubText_.setText(con);

        // test ip button
        DDPanel pubbuttons = new DDPanel();
        pubbuttons.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        ippanel.add(GuiUtils.CENTER(pubbuttons), BorderLayout.SOUTH);

        test_ = new GlassButton("testip", "Glass");
        pubbuttons.add(test_);
        test_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                testConnection();
            }
        });

        ////// BOTTOM
        DDPanel bottom = new DDPanel();
        bottom.setBorderLayoutGap(0, 10);
        iptop.add(bottom, BorderLayout.SOUTH);

        //**// Public game
        DDLabelBorder publicgame = new DDLabelBorder("publicgame", STYLE);
        bottom.add(publicgame, BorderLayout.WEST);

        // public game list
        DDPanel gbox = new DDPanel();
        gbox.setBorder(BorderFactory.createEmptyBorder(0, 15, 2, 0));
        gbox.setBorderLayoutGap(-4, 10);
        publicgame.add(gbox, BorderLayout.WEST);

        // set player profile to take into account any changes
        profile_ = PlayerProfileOptions.getDefaultProfile();
        PokerPlayer player = game_.getHost();
        player.setProfile(profile_);
        game_.updatePlayerList(player);

        // add profile
        EngineButtonListener listener = new EngineButtonListener(context_, this, gamephase_.getButtonNameFromParam("profile"));
        DDImageButton button = new DDImageButton(listener.getGameButton().getName());
        button.addActionListener(listener);
        gbox.add(button, BorderLayout.WEST);

        DDPanel profilepanel = new DDPanel();
        profilepanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, 3, VerticalFlowLayout.LEFT));
        gbox.add(profilepanel, BorderLayout.CENTER);

        DDLabel label = new DDLabel(GuiManager.DEFAULT, "StartMenuSmall");
        String profileText = PropertyConfig.getMessage("msg.publiclist.profile",
                                                       Utils.encodeHTML(profile_.getName()));
        label.setText(profileText);
        profilepanel.add(label);

        label = new DDLabel((profile_.isActivated() ? "publiclist.enabled" : "publiclist.disabled"), "StartMenuSmall");
        profilepanel.add(label);

        // add game checkbox
        listPublic_ = new DDCheckBox("publiclist", STYLE);
        listPublic_.setSelected(false);

        // shouldn't happen unless player copies a player profile file over
        listPublic_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (listPublic_.isSelected() && engine_.isDemo())
                {
                    EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.playerprofile.demo"));
                    listPublic_.setSelected(false);
                }
            }
        });
        gbox.add(listPublic_, BorderLayout.SOUTH);

        //**// options
        DDLabelBorder options = new DDLabelBorder("onlineoptions", STYLE);
        bottom.add(options, BorderLayout.CENTER);

        DDPanel optlist = new DDPanel();
        optlist.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        optlist.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, VerticalFlowLayout.LEFT));
        options.add(optlist, BorderLayout.CENTER);

        hostAsObs_ = new DDCheckBox("hostasobs", STYLE);
        hostAsObs_.setSelected(false);
        optlist.add(hostAsObs_);

        // init
        doCheckBox();
    }

    /**
     * conveinence method to add ip label/txt
     */
    static Widgets addIPText(String sName, JComponent parent, Object layout, String STYLE, String BEVELSTYLE, PokerGame game)
    {
        Widgets w = new Widgets();
        DDPanel panel = new DDPanel();
        parent.add(panel, layout);
        panel.setBorderLayoutGap(0, 10);

        DDLabel label = new DDLabel(sName, STYLE);
        final DDTextField text = new DDTextField(sName, STYLE, BEVELSTYLE);
        if (game != null) text.setRegExp(game.getConnectRegExp());
        text.setDisplayOnly(true);
        panel.add(label, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        DDButton copy = new GlassButton("copyurl", "Glass");
        panel.add(GuiUtils.CENTER(copy), BorderLayout.EAST);
        copy.addActionListener(new ActionListener()
        {
            DDTextField _text = text;

            public void actionPerformed(ActionEvent e)
            {
                GuiUtils.copyToClipboard(text.getText());
            }
        });

        w.label = label;
        w.text = text;
        w.button = copy;

        return w;
    }

    /**
     * conveinence method to add public ip field/button
     */
    private Widgets addIPField(JComponent parent, Object layout, boolean bPublic)
    {
        Widgets w = new Widgets();
        DDPanel panel = new DDPanel();
        parent.add(GuiUtils.WEST(panel), layout);
        panel.setBorderLayoutGap(0, 10);

        String sName = bPublic ? "publicip" : "lanip";
        DDLabel label = new DDLabel(sName, STYLE);
        DDTextField text = new DDTextField(sName, STYLE, "BrushedMetal");
        text.setRegExp(PokerConstants.REGEXP_IP_ADDRESS);
        text.setColumns(12);
        panel.add(label, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        if (bPublic)
        {
            DDButton copy = new GlassButton("publicip", "Glass");
            panel.add(GuiUtils.CENTER(copy), BorderLayout.EAST);
            copy.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    getPublicIP();
                }
            });
            w.button = copy;
        }

        String sHelp = GuiManager.getDefaultHelp(label);
        sHelp = PropertyConfig.formatMessage(sHelp, "" + game_.getPort());
        label.setHelpText(sHelp);
        text.setHelpText(sHelp);

        w.label = label;
        w.text = text;

        return w;
    }

    /**
     * Return widgets created by above method
     */
    static class Widgets
    {
        DDLabel label;
        DDTextField text;
        com.donohoedigital.gui.DDButton button;
    }

    /**
     * get public ip
     */
    private void getPublicIP()
    {
        SendMessageDialog dialog = (SendMessageDialog) context_.processPhaseNow("GetPublicIP", null);
        if (dialog.getStatus() == DDMessageListener.STATUS_OK)
        {
            String sIP = dialog.getReturnMessage().getString(EngineMessage.PARAM_IP);
            pubIPText_.setText(sIP);
            game_.setPublicIP(sIP);
        }
    }

    /**
     * test connection
     */
    private void testConnection()
    {
        if (TESTING(UDPServer.TESTING_UDP))
            logger.debug("Starting test public connect --------------------------------");
        DMTypedHashMap params = new DMTypedHashMap();
        params.setObject(TestPublicConnect.PARAM_URL, new PokerURL(game_.getPublicConnectURL()));
        SendMessageDialog dialog = (SendMessageDialog) context_.processPhaseNow("TestPublicConnect", params);
        if (dialog.getStatus() == DDMessageListener.STATUS_OK)
        {
            bTestPassed_ = true;
            sLastValidTest_ = game_.getPublicConnectURL();
        }
        else
        {
            bTestPassed_ = false;
            sLastValidTest_ = null;
        }

        checkButtons();
    }

    /**
     * Start of phase
     */
    public void start()
    {
        // set help text
        context_.getWindow().setHelpTextWidget(text_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, configurePublic_);

        // check button states and focus
        checkButtons();
    }

    /**
     * Finish
     */
    public void finish()
    {

    }

    /**
     * Returns true
     */
    public boolean processButton(GameButton button)
    {
        if (button.getName().equals(start_.getName()))
        {
            // start button enabled only if valid test occurred
            // and internet enabled
            if (configurePublic_.isSelected() && !bTestPassed_)
            {
                GameButton bSkipTest = EngineUtils.displayConfirmationDialogCustom(context_,
                                                                                   "OnlineTestConfirmation",
                                                                                   PropertyConfig.getMessage("msg.testpublic"), null, "TestPublic", null);
                if (bSkipTest != null && bSkipTest.getName().startsWith("doTest"))
                {
                    testConnection();
                    if (!bTestPassed_)
                    {
                        return false;
                    }
                }
                else if (bSkipTest != null && bSkipTest.getName().startsWith("cancel"))
                {
                    return false;
                }
            }

            // if box isn't selected, then unset the public IP
            // incase it was selected and deselected
            if (!configurePublic_.isSelected())
            {
                game_.setPublicIP(null);
            }

            // if listed, send the game to the server
            // (make sure config public is selected too)
            if (configurePublic_.isSelected() && listPublic_.isSelected())
            {
                if (!OnlineServer.getWanManager().addWanGame(game_, profile_))
                {
                    // failed to connect
                    return false;
                }

                game_.setPublic(true);
            }

            // prevent double click on this button
            start_.setEnabled(false);

            // if host as observer, switch host to an observer
            if (hostAsObs_.isSelected())
            {
                PokerPlayer host = game_.getHost();
                game_.removePlayer(host);
                game_.addObserver(host);
            }

            // ready to accept registrations
            game_.setOnlineMode(PokerGame.MODE_REG);

            // for save file, record the phase we are going to
            // and save game
            context_.setSpecialSavePhase(button.getGotoPhase());
            game_.saveWriteGame();
            context_.setSpecialSavePhase(null);

            // display a message for the host
            if (game_.isPublic())
            {
                game_.getOnlineManager().sendDirectorChat(PropertyConfig.getMessage("msg.chat.publicgame"), null);
            }
            else
            {
                game_.getOnlineManager().sendDirectorChat(PropertyConfig.getMessage("msg.chat.privategame"), null);
            }
            game_.getOnlineManager().sendDirectorChat(PropertyConfig.getMessage("msg.chat.lobbyopen"), null);
        }
        else if (button.getName().startsWith("cancel"))
        {
            context_.setGame(null);
        }

        return true;
    }

    /**
     * When text field changes
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String ip = pubIPText_.getText().trim();
        game_.setPublicIP(ip);
        if (ip.length() > 0)
        {
            pubText_.setText(game_.getPublicConnectURL());
        }
        else
        {
            pubText_.setText("");
        }

        bTestPassed_ = pubIPText_.isValidData() && sLastValidTest_ != null && game_.getPublicConnectURL().equals(sLastValidTest_);
        checkButtons();
    }

    /**
     * set buttons enabled/disabled based on selection
     */
    private void checkButtons()
    {
        boolean bValid = true;
        test_.setEnabled(pubText_.isValidData() && configurePublic_.isSelected());
        if (configurePublic_.isSelected() && !pubText_.isValidData()) bValid = false;
        start_.setEnabled(bValid);
    }

    /**
     * Handle checkbox change
     */
    private void doCheckBox()
    {
        boolean bPub = configurePublic_.isSelected();

        // must be a valid online profile to list a game
        if (profile_.isActivated())
        {
            listPublic_.setEnabled(bPub);
        }
        else
        {
            listPublic_.setEnabled(false);
        }

        pubCopy_.setEnabled(bPub);
        pubLabel_.setEnabled(bPub);
        pubIPLabel_.setEnabled(bPub);
        pubText_.setEnabled(bPub);
        pubIPText_.setEnabled(bPub);
        generate_.setEnabled(bPub);

        // test/start buttons
        checkButtons();
    }
}
