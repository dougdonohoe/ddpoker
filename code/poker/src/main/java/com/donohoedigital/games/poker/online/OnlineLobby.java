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
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 19, 2006
 * Time: 3:04:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class OnlineLobby extends BasePhase implements ChatHandler, DDTable.TableMenuItems, ChatManager, Runnable
{
    static Logger logger = LogManager.getLogger(OnlineLobby.class);

    private static OnlineLobby LOBBY = null;

    private PokerMain main_;
    private DDPanel base_;
    //private DDHtmlArea text_;
    private ChatLobbyPanel chat_;

    private PlayerModel model_;

    private boolean bRunning_ = false;
    private PlayerProfile profile_;
    private PokerPrefsPlayerList muted_;
    private PokerPrefsPlayerList banned_;
    private OnlinePlayerInfo me_;


    /**
     * Chat has focus?
     */
    public static boolean hasFocus()
    {
        if (LOBBY == null) return false;
        return LOBBY.chat_.hasFocus();
    }

    /**
     * Allowed to show the lobby?
     */
    public static boolean showLobby(GameEngine engine, GameContext context, PlayerProfile profile)
    {
        if (engine.isDemo())
        {
            EngineUtils.displayInformationDialog(context, PropertyConfig.getMessage("msg.onlinelobby.demo"));
            return false;
        }

        if (!profile.isActivated())
        {
            // dialog
            EngineUtils.displayInformationDialog(context, PropertyConfig.getMessage("msg.lobby.profilereq",
                                                    Utils.encodeHTML(profile.getName())));

            return false;
        }

        return true;
    }

    /**
     * init
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        main_ = (PokerMain) engine_;
        String STYLE = gamephase_.getString("style", "default");
        LOBBY = this;

        // mute/ban list
        muted_ = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_MUTE);
        banned_ = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_BANNED);

        // base
        base_ = new DDPanel();
        base_.setBorderLayoutGap(0, 1);
        base_.setBorder(BorderFactory.createEmptyBorder(2, 3, 5, 2));

        // help text - TODO: is this going to be used?
        //text_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        //text_.setDisplayOnly(true);
        //text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());

        // chat (chat server created with first call to getChatServer())
        profile_ = PlayerProfileOptions.getDefaultProfile();
        me_ = new OnlinePlayerInfo();
        me_.setName(profile_.getName());
        me_.setPublicUseKey(engine.getPublicUseKey());
        chat_ = new ChatLobbyPanel(context_, main_.getChatServer(), profile_, "ChatLobby", "BrushedMetal");

        // player list
        DDPanel left = new DDPanel();
        left.setBorderLayoutGap(3, 0);

        // nicer table header
        DDLabel header = new DDLabel(GuiManager.DEFAULT, STYLE);
        header.setText(PropertyConfig.getMessage("msg.lobby.header"));
        left.add(GuiUtils.CENTER(header), BorderLayout.NORTH);

        ///
        /// player list table
        ///
        DDPanel playerbase = new DDPanel();
        left.add(playerbase, BorderLayout.CENTER);

        DDScrollTable playerScroll = new DDScrollTable(GuiManager.DEFAULT, "ChatLobby", "BrushedMetal", COLUMN_NAMES, COLUMN_WIDTHS);
        playerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        playerScroll.setPreferredSize(new Dimension(playerScroll.getPreferredWidth(), COLUMN_WIDTHS[0]));
        playerbase.add(playerScroll, BorderLayout.CENTER);

        DDTable table = playerScroll.getDDTable();
        model_ = new PlayerModel();
        table.setFocusable(false);
        table.setTableMenuItems(this);
        table.setSelectOnRightClick(true);
        table.setModel(model_);
        table.setShowHorizontalLines(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setTableHeader(null);

        // put it together
        base_.add(left, BorderLayout.WEST);
        base_.add(chat_, BorderLayout.CENTER);
        //base_.add(text_, BorderLayout.SOUTH);
    }

    /**
     * Start of phase
     */
    @Override
    public void start()
    {
        // if users presses launch button again, this will be called.  don't run logic again in this case
        if (bRunning_) return;
        bRunning_ = true;

        // set help text
        //context_.getWindow().setHelpTextWidget(text_);

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, base_, true, chat_.getFocusWidget());

        // double check profile is ok
        if (!profile_.isActivated())
        {
            // dialog
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.lobby.profilereq",
                                                    Utils.encodeHTML(profile_.getName())));
            context_.close();
        }
        else
        {
            Thread t = new Thread(this, "OnlineLobbyInit");
            t.start();
        }
    }

    /**
     * at start, connect to chat server
     */
    public void run()
    {
        main_.setChatLobbyHandler(this);
        chat_.start();
        main_.getChatServer().sendChat(profile_, null); // send hello (SPECIAL case when null message)
    }

    /**
     * finish
     */
    @Override
    public void finish()
    {
        bRunning_ = false;

        profile_ = null;
        chat_.finish();
        main_.setChatLobbyHandler(null);
        main_.shutdownChatServer();
        super.finish();
    }

    /**
     * chat related received.
     */
    public void chatReceived(OnlineMessage omsg)
    {
        if (omsg.getCategory() == OnlineMessage.CAT_CHAT)
        {
            if (TESTING(EngineConstants.TESTING_UDP_APP)) logger.debug("CHAT "+omsg.getPlayerName() +" said " + omsg.getChat());
            chat_.chatReceived(omsg);
        }
        else if (omsg.getCategory() == OnlineMessage.CAT_CHAT_ADMIN)
        {
            if (TESTING(EngineConstants.TESTING_UDP_APP)) logger.debug("CHAT admin " + PokerConstants.toStringAdminType(omsg.getChatType()) +
                                                         (omsg.getChat() != null ? " - "+omsg.getChat() : ""));
            switch (omsg.getChatType())
            {
                case PokerConstants.CHAT_ADMIN_WELCOME:
                    model_.setList(omsg.getPlayerList());
                    break;

                case PokerConstants.CHAT_ADMIN_JOIN:
                    model_.addPlayer(omsg.getPlayerInfo());
                    break;

                case PokerConstants.CHAT_ADMIN_LEAVE:
                    model_.removePlayer(omsg.getPlayerInfo());
                    break;

                case PokerConstants.CHAT_ADMIN_ERROR:
                    // alter chat window
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run()
                            {
                                chat_.removeBottomControls(context_);
                            }
                        }
                    );
                    break;
            }

            // display any message
            chat_.chatReceived(omsg);
        }
        else
        {
            logger.warn("CHAT don't know how to handle this: "+ omsg.toStringCategory());
        }
    }

    // client table info
    private static final int[] COLUMN_WIDTHS = new int[] {
        125
    };
    private static final String[] COLUMN_NAMES = new String[] {
        "onlineplayer"
    };

    /**
     * Used by table to display players in game
     */
    private class PlayerModel extends DefaultTableModel
    {
        private List<OnlinePlayerInfo> list = new ArrayList<OnlinePlayerInfo>();

        @Override
        public String getColumnName(int c) {
            return COLUMN_NAMES[c];
        }

        @Override
        public int getColumnCount() {
            return COLUMN_WIDTHS.length;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public int getRowCount() {
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex)
        {
            OnlinePlayerInfo player = getPlayer(rowIndex);

            if (COLUMN_NAMES[colIndex].equals("onlineplayer"))
            {
                return player.getName();
            }
            return "[bad column]";
        }

        private OnlinePlayerInfo getPlayer(int rowIndex)
        {
            return list.get(rowIndex);
        }

        public void setList(List<OnlinePlayerInfo> list)
        {
            this.list = list;
            changed();
        }

        public void addPlayer(OnlinePlayerInfo player)
        {
            if (!list.contains(player))
            {
                list.add(player);
                changed();
            }
        }

        public void removePlayer(OnlinePlayerInfo player)
        {
            // don't remove self (special case when player joins with same key/profile from
            // another computer)
            if (!me_.equals(player) && list.remove(player))
            {
                changed();
            }
        }

        private void changed()
        {
            // sort
            Collections.sort(list);

            // table changed
            GuiUtils.invoke(new Runnable() {
                public void run() {
                    fireTableDataChanged();
                }
            });
        }
    }

    /**
     * get selected player
     */
    private OnlinePlayerInfo getSelectedPlayer(DDTable table)
    {
        int n = table.getSelectedRow();
        if (n < 0) return null;
        return model_.getPlayer(n);
    }

    ////
    //// Table menu interface
    ////

    public boolean isItemsToBeAdded(DDTable table)
    {
        return getSelectedPlayer(table) != null;
    }

    private static ImageIcon infoIcon_ = ImageConfig.getImageIcon("menuicon.info");

    public void addMenuItems(DDTable table, DDPopupMenu menu)
    {
        OnlinePlayerInfo pi = getSelectedPlayer(table);
        PokerPlayer p = new PokerPlayer(pi.getPublicUseKey(), 0, pi.getName(), true);

        // player info
        menu.add(new PlayerInfoMenu(pi));

        // mute/ban
        if (!p.getName().equals(profile_.getName()))
        {
            menu.add(new ShowTournamentTable.MutePlayer("PokerTable", p, muted_.containsPlayer(p.getName()), this, true));
            menu.add(new ShowTournamentTable.BanPlayer(context_, "PokerTable", p, banned_.containsPlayer(p.getName()), null, this, true, false));
        }
    }

    /**
     * sitout menu item
     */
    private class PlayerInfoMenu extends DDMenuItem implements ActionListener
    {
        OnlinePlayerInfo oinfo;

        PlayerInfoMenu(OnlinePlayerInfo oinfo)
        {
            super(GuiManager.DEFAULT, "PokerTable");
            this.oinfo = oinfo;
            setDisplayMode(MODE_NORMAL);
            setText(PropertyConfig.getMessage("menuitem.playerinfo"));
            setIcon(infoIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            StringBuilder sb = new StringBuilder();
            List<OnlinePlayerInfo> aliases = oinfo.getAliases();
            String sDate = PropertyConfig.getDateFormat(GameEngine.getGameEngine().getLocale()).format(new Date(oinfo.getCreateDate()));
            if (aliases == null || aliases.isEmpty())
            {
                sb.append(PropertyConfig.getMessage("msg.alias.none"));
            }
            else
            {
                int nCnt = 0;
                for (OnlinePlayerInfo alias : aliases)
                {
                    if (nCnt > 0)
                    {
                        sb.append(", ");
                        if (nCnt % 3 == 0) sb.append("<BR>");
                    }
                    sb.append(alias.getName());
                    nCnt++;
                }
            }

            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.chat.playerinfo",
                                                                                     Utils.encodeHTML(oinfo.getName()),
                                                                                     sb.toString(),
                                                                                     sDate));
        }
    }

    ////
    //// ChatManager (used to display ban/mute messages only)
    ////

    public void sendChat(int nPlayerID, String sMessage) { }

    public void sendChat(String sMessage, PokerTable table, String sTestData) { }

    public void sendDirectorChat(String sMessage, Boolean bPauseClock) { }

    public void setChatHandler(ChatHandler chat) { }

    public void deliverChatLocal(int nType, String sMessage, int id)
    {
        chat_.displayMessage(null, null, OnlineMessage.CAT_CHAT_ADMIN, sMessage, true);
    }
}
