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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.beans.*;

/**
 * Created by IntelliJ IDEA.
 * User: zak
 * Date: Mar 3, 2005
 * Time: 1:49:42 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ListGames extends BasePhase implements PropertyChangeListener, ListSelectionListener
{
    static Logger logger = LogManager.getLogger(ListGames.class);

    protected PlayerProfile profile_;

    protected DDHtmlArea text_;
    protected DDTextField connectText_;
    protected DDLabel connectLabel_;
    protected DDButton pubPaste_;
    protected ButtonBox buttonbox_;
    protected MenuBackground menu_;
    protected DDPanel menubox_;
    protected DDButton start_;
    protected DDButton obs_;
    protected String STYLE;

    protected DDTable table_;
    protected DDPagingTableModel model_;
    protected TournamentSummaryPanel sum_;

    boolean bIgnoreTextChange_ = false;

    public static final String PARAM_URL = "_url_";

    protected static final String OKAY_OBSERVE = "okayobserve";
    protected static final String OKAY_JOIN = "okayjoin";

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // make profile available during initialization
        profile_ = PlayerProfileOptions.getDefaultProfile();

        // name of style used for all widgets in data area
        STYLE = gamephase_.getString("style", "default");

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        //menu_.addAncestorListener(this);
        menubox_ = menu_.getMenuBox();
        String sHelpName = menu_.getHelpName();

        // put buttons in the menubox_
        buttonbox_ = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox_.add(buttonbox_, BorderLayout.SOUTH);
        start_ = buttonbox_.getDefaultButton();
        obs_ = buttonbox_.getButton(OKAY_OBSERVE);

        // holds data we are gathering
        DDPanel data = new DDPanel(sHelpName);
        data.setBorderLayoutGap(10, 10);
        data.setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 10));
        menubox_.add(data, BorderLayout.CENTER);

        // IP panel
        DDPanel iptop = new DDPanel();
        data.add(iptop, BorderLayout.NORTH);

        //**// Game URL
        DDLabelBorder ipborder = new DDLabelBorder("join." + getListName(), STYLE);
        iptop.add(ipborder, BorderLayout.NORTH);
        DDPanel ippanel = new DDPanel();
        ippanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
        ipborder.add(ippanel, BorderLayout.NORTH);

        Widgets w = addIPText("connect." + getListName(), ippanel, BorderLayout.CENTER, isDisplayUseLastButton());
        connectText_ = w.text;
        connectText_.addPropertyChangeListener("value", this);
        connectLabel_ = w.label;
        pubPaste_ = w.button;
        pubPaste_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable value = clip.getContents(null);
                if (value.isDataFlavorSupported(DataFlavor.stringFlavor))
                {
                    try
                    {
                        String s = (String) value.getTransferData(DataFlavor.stringFlavor);
                        if (s != null)
                        {
                            connectText_.setText(s);
                        }
                    }
                    catch (Throwable ignored)
                    {
                    }
                }
            }
        });

        /// middle
        DDPanel middle = new DDPanel();
        middle.setBorderLayoutGap(10, 10);
        data.add(middle, BorderLayout.CENTER);

        ////
        //// Table (north part of 'middle')
        ////
        String listName = getListName();
        DDLabelBorder listborder = new DDLabelBorder(listName, STYLE);
        middle.add(listborder, BorderLayout.NORTH);
        DDPanel listpanel = new DDPanel();
        listpanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
        listpanel.setBorderLayoutGap(0, 10);
        listborder.add(listpanel, BorderLayout.NORTH);

        String[] columnNames = getListColumnNames();
        int[] columnWidths = getListColumnWidths();
        DDPagingTable listTable = new DDPagingTable(GuiManager.DEFAULT,
                                                    "GameList",
                                                    "gamelist",
                                                    columnNames,
                                                    columnWidths,
                                                    0,
                                                    getListRowCount());
        DDScrollTable listScroll = listTable.getDDScrollTable();
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        listpanel.add(listTable, BorderLayout.WEST);

        model_ = createListTableModel();
        listTable.setModel(model_);

        table_ = listScroll.getDDTable();
        table_.setShowHorizontalLines(true);
        table_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table_.getSelectionModel().addListSelectionListener(this);
        table_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table_.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    doubleClick();
                }
            }
        });

        // game details
        sum_ = new TournamentSummaryPanel(context_, "TournamentSummarySmall", "BrushedMetal",
                                          "BrushedMetal", sHelpName, .75d, false, true);
        sum_.setPreferredSize(new Dimension(200, 150));
        listpanel.add(sum_, BorderLayout.CENTER);

        // optional list info
        Component listinfo = getListInfo();

        if (listinfo != null)
        {
            listborder.add(listinfo, BorderLayout.CENTER);
        }

        // this controls height - sort of lame
        listScroll.setPreferredSize(new Dimension(listScroll.getPreferredWidth(), listinfo != null ? 170 : 210));

        // bottom - text and optional component
        DDPanel bottom = new DDPanel();
        middle.add(bottom, BorderLayout.CENTER);

        // opt component
        JComponent opt = getExtra();
        if (opt != null)
        {
            bottom.add(opt, BorderLayout.NORTH);
            middle.setBorderLayoutGap(10, 10);
        }

        // Help text (center part part of 'middle')
        text_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());
        bottom.add(text_, BorderLayout.CENTER);

        // default
        String sDefault = gamephase_.getString(PARAM_URL);
        if (sDefault != null)
        {
            boolean bDefaultIsObs = false;
            if (sDefault.endsWith(PokerConstants.JOIN_OBSERVER_QUERY))
            {
                sDefault = sDefault.substring(0, sDefault.length() - PokerConstants.JOIN_OBSERVER_QUERY.length());
                bDefaultIsObs = true;
            }
            connectText_.setText(sDefault);
            if (connectText_.isValidData())
            {
                DDButton button = bDefaultIsObs ? obs_ : start_;
                button.setEnabled(false);
                new Thread(new AutoJoin(button), "AutoJoin").start();
            }
        }
    }

    /**
     * double click - press start button by default
     */
    protected void doubleClick()
    {
        start_.doClick();
    }

    /**
     * Auto join - sleep to let ui show then click start
     */
    private class AutoJoin implements Runnable
    {
        DDButton button;

        private AutoJoin(DDButton button)
        {
            this.button = button;
        }

        public void run()
        {
            Utils.sleepMillis(500);
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    button.setEnabled(true);
                    button.doClick();
                }
            });
        }
    }

    /**
     * Extra component for bottom
     */
    protected JComponent getExtra()
    {
        return null;
    }

    /**
     * conveinence method to add ip label/txt
     */
    private Widgets addIPText(String sName, JComponent parent, Object layout, boolean bUseLastButton)
    {
        Widgets w = new Widgets();
        DDPanel panel = new DDPanel();
        parent.add(panel, layout);
        panel.setBorderLayoutGap(0, 10);

        DDLabel label = new DDLabel(sName, STYLE);
        final DDTextField text = new DDTextField(sName, STYLE, "BrushedMetal");
        text.setRegExp(PokerConstants.REGEXP_GAME_URL);
        panel.add(label, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);

        DDPanel buttons = new DDPanel();
        buttons.setBorderLayoutGap(0, 5);
        panel.add(GuiUtils.CENTER(buttons), BorderLayout.EAST);

        DDButton paste = new GlassButton("pasteurl", "Glass");
        buttons.add(paste, BorderLayout.CENTER);
        paste.addActionListener(new ActionListener()
        {
            DDTextField _text = text;

            public void actionPerformed(ActionEvent e)
            {
                GuiUtils.copyToClipboard(text.getText());
            }
        });

        if (bUseLastButton)
        {
            final String sLast = engine_.getPrefsNode().get(PokerConstants.PREF_LAST_JOIN, null);
            DDButton uselast = new GlassButton("uselast", "Glass");
            buttons.add(uselast, BorderLayout.EAST);
            if (sLast != null && sLast.length() > 0)
            {
                uselast.addActionListener(new ActionListener()
                {
                    DDTextField _text = text;
                    String _sLast = sLast;

                    public void actionPerformed(ActionEvent e)
                    {
                        text.setText(sLast);
                    }
                });
            }
            else
            {
                uselast.setEnabled(false);
            }
        }

        w.label = label;
        w.text = text;
        w.button = paste;

        return w;
    }

    /**
     * Start of phase
     */
    @Override
    public void start()
    {
        // set help text
        context_.getWindow().setHelpTextWidget(text_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, connectText_);

        // check button states and focus
        checkButtons();
    }

    /**
     * Finish
     */
    @Override
    public void finish()
    {
    }

    boolean bProcessing_ = false;

    /**
     * Returns true
     */
    @Override
    public boolean processButton(GameButton button)
    {
        String sName = button.getName();
        boolean bJoin = sName.equals(OKAY_JOIN);
        boolean bObs = sName.equals(OKAY_OBSERVE);
        if ((bJoin || bObs) && !bProcessing_)
        {
            bProcessing_ = true; // prevent multiple join attempts

            // validate profile first
            if (!validateProfile())
            {
                EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.playerprofile.cantvalidate",
                                                                                         Utils.encodeHTML(profile_.getName())));
                bProcessing_ = false;
                return false;
            }

            // get connect url and player name
            String sConnect = connectText_.getText().trim();
            PokerURL url = new PokerURL(sConnect);

            // create game
            PokerGame game = new PokerGame(context_);
            game.setOnlineGameID(url.getGameID());
            game.setOnlinePassword(url.getPassword());

            // temp player with connect URL provided by user
            PokerPlayer ptemp = new PokerPlayer(engine_.getPublicUseKey(), PokerConstants.PLAYER_ID_TEMP, profile_, true);
            ptemp.setConnectURL(url);
            if (bJoin)
            {
                game.addPlayer(ptemp);
            }
            else
            {
                game.addObserver(ptemp);
            }

            // init to client mode and save game
            game.initOnline(PokerGame.MODE_CLIENT);
            context_.setGame(game);

            // log it
            logger.info("Joining game " + sConnect + "...");

            // have online manager do join, which returns true
            // if successful (in which case it places the game into
            // the engine and invokes the phase returned)
            Object o = game.getOnlineManager().joinGame(bObs, false, false);
            if (o != Boolean.TRUE)
            {
                context_.setGame(null);
                bProcessing_ = false;
                return false;
            }
            engine_.getPrefsNode().put(PokerConstants.PREF_LAST_JOIN, sConnect);
        }

        return true;
    }

    /**
     * set buttons enabled/disabled based on selection
     */
    protected void checkButtons()
    {
        boolean bValid = connectText_.isValidData();
        start_.setEnabled(bValid);
        obs_.setEnabled(bValid);
    }

    /**
     * Return widgets created by above method
     */
    private class Widgets
    {
        DDLabel label;
        DDTextField text;
        DDButton button;
    }

    /**
     * Get an optional info panel (placed below list)
     */
    public Component getListInfo()
    {
        return null;
    }

    /**
     * Get the list display name
     */
    public abstract String getListName();

    /**
     * Get the list column names
     */
    public abstract String[] getListColumnNames();

    /**
     * Get the list column widths
     */
    public abstract int[] getListColumnWidths();

    /**
     * Get the number of rows to show within the table.  Return a number less that zero
     * to disable paging.
     */
    public abstract int getListRowCount();

    /**
     * Get the model backing the list table
     */
    public abstract DDPagingTableModel createListTableModel();

    /**
     * display the use last button?
     */
    public abstract boolean isDisplayUseLastButton();


    /**
     * Validate profile with server.  Return true if valid.  I know I'm copy and pasting.  So what.
     */
    private boolean validateProfile()
    {
        // no need to do anything if not already online activated
        if (!profile_.isActivated())
        {
            return true;
        }

        // if profile was manually changed, the password won't decrypt
        try
        {
            profile_.getPassword();
        }
        catch (Throwable e)
        {
            logger.info("Unable to get password for profile: " + profile_.getName());
            resetProfile();
            return false;
        }

        boolean faceless = false; // for testing

        // Send a message requesting that the game be added
        TypedHashMap hmParams = new TypedHashMap();
        hmParams.setBoolean(SendMessageDialog.PARAM_FACELESS, faceless);
        OnlineProfile auth = new OnlineProfile(profile_.getName());
        auth.setPassword(profile_.getPassword());
        hmParams.setObject(ValidateProfile.PARAM_AUTH, auth);

        SendMessageDialog dialog = (SendMessageDialog) context_.processPhaseNow("ValidateProfile", hmParams);

        if (dialog.getStatus() == DDMessageListener.STATUS_OK)
        {
            EngineMessage resEngineMsg = dialog.getReturnMessage();
            OnlineMessage resOnlineMsg = new OnlineMessage(resEngineMsg);

            // see PokerServlet.validateProfile()
            if (resOnlineMsg.getWanAuth() == null)
            {
                // Authentication failed, so reset the local profile.
                resetProfile();
                return false;
            }
        }
        else
        {
            // if error returned and this flag set (reusing old War-AOI flag),
            // then also reset profile
            EngineMessage regEngineMsg = dialog.getReturnMessage();
            if (regEngineMsg != null && regEngineMsg.getBoolean(EngineMessage.PARAM_ELIMINATED, false))
            {
                resetProfile();
                return false;
            }
        }

        return true;

    }

    protected void resetProfile()
    {
        profile_.setPassword(null);
        profile_.setActivated(false);
        profile_.save();
    }
}
