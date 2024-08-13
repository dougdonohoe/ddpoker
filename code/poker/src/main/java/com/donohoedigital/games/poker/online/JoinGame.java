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
 * JoinGame.java
 *
 * Created on November 27, 2004, 4:59 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.p2p.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.List;

/**
 *
 * @author  donohoe
 */
public class JoinGame extends ListGames
{
    private LanManager lanManager_;
    private LanClientInfo selected_;

    /**
     * Creates a new instance of TournamentOptions 
     */
    public JoinGame()
    {
    }

    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        lanManager_ = ((PokerMain)engine).getLanManager();
        super.init(engine, context, gamephase);
    }

    /**
     * Extra component for bottom
     */
    protected JComponent getExtra()
    {
        DDLabelBorder pub = new DDLabelBorder("publiclist", STYLE);

        DDPanel inside = new DDPanel();
        inside.setBorder(BorderFactory.createEmptyBorder(0, 15, 4, 0));
        inside.setBorderLayoutGap(0, 15);
        pub.add(inside, BorderLayout.CENTER);

        GlassButton find = new GlassButton("okayfind", "Glass");
        find.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (engine_.isDemo())
                {
                    EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.playerprofile.demo2"));
                }
                else
                {
                    context_.processPhase("FindGames");
                }
            }
        });
        inside.add(find, BorderLayout.WEST);

        DDLabel label = new DDLabel("okayfind", "PokerStandardSmall");
        inside.add(label, BorderLayout.CENTER);

        return pub;
    }

    /**
     * Start
     */
    public void start()
    {
        // send continous LAN_REFRESH ping so we get list of
        // all clients on the LAN.  This is done here so
        // we aren't continuously sending alive messages when
        // no one is listening (or cares).
        lanManager_.setAliveThread(true, true);
        super.start();
    }

    /**
     * Finish
     */
    public void finish()
    {
        lanManager_.setAliveThread(false, false);
        ((LanClientModel) model_).cleanup();
        super.finish();
    }

    /**
     * Called whenever the value of the selection changes.
     * @param e the event that characterizes the change.
     *
     */
    public void valueChanged(ListSelectionEvent e) 
    {
        if (e.getValueIsAdjusting()) return;
        
        ListSelectionModel lsm = table_.getSelectionModel();
        int index = lsm.getMinSelectionIndex();
        
        LanClientInfo newSelection = null;
        
        if (index >= 0 ) 
        {
            newSelection = ((LanClientModel) model_).getClientInfo(index);
        }
        
        // don't update if not new
        if (newSelection == selected_) return;

        // get current values
        String sCurrentText = connectText_.getText().trim();
        String sOldLanConnect = getLanConnect(selected_);
        String sNewLanConnect = getLanConnect(newSelection);
        String sDetails = null;
        TournamentProfile profile = null;
        String sConnect = null;
               
        // remember new selection
        selected_ = newSelection;
        
        // update details        
        if (selected_ != null)
        {
            switch (getOnlineMode(selected_))
            {
                case PokerGame.MODE_INIT:
                    sDetails = PropertyConfig.getMessage("msg.initgame", selected_.getPlayerName());
                    break;
                    
                case PokerGame.MODE_REG:
                    profile = getProfile(selected_);
                    if (profile == null) sDetails = "";
                    break;
                    
                case PokerGame.MODE_PLAY:
                    sDetails = PropertyConfig.getMessage("msg.closedgame", selected_.getPlayerName());
                    break;
                    
                case PokerGame.MODE_NONE: 
                default:
                    sDetails = PropertyConfig.getMessage("msg.nogame", selected_.getPlayerName());
            }
            
            sConnect = sNewLanConnect == null ? "" : sNewLanConnect;
        } 
        else 
        {
            // if no current selection and the text in the connect field matches
            // the previous selection, clear it out
            if (sOldLanConnect != null && sCurrentText.equals(sOldLanConnect))
            {
                sConnect = "";
            }
        }

        if (profile != null) sum_.updateProfile(profile);
        else sum_.updateEmptyProfile(sDetails);

        if (sConnect != null && !sConnect.equals(sCurrentText))
        {
            bIgnoreTextChange_ = true;
            connectText_.setText(sConnect);
            bIgnoreTextChange_ = false;
        }
    }
       
    /**
     * When text field changes
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        checkButtons();
        if (bIgnoreTextChange_) return;

        // see if we match something in the table, if so, highlight it
        String sConnect = connectText_.getText().trim();


        ListSelectionModel selmodel = table_.getSelectionModel();
        int nNum = model_.getRowCount();
        for (int i = 0; sConnect != null && i < nNum; i++)
        {
            DMTypedHashMap ginfo = (DMTypedHashMap) ((LanClientModel) model_).getClientInfo(i).getGameData();
            if (ginfo == null) continue;

            if (sConnect.equals(ginfo.getString(PokerMain.ONLINE_GAME_LAN_CONNECT)))
            {
                selmodel.setSelectionInterval(i, i);
                table_.scrollRectToVisible(table_.getCellRect(i, 0, true));
                return;
            }
        }

        if (!selmodel.isSelectionEmpty())
        {
            selmodel.clearSelection();
        }
    }

    /**
     * helper method to get TournamentProfile from LanClientInfo
     */
    private TournamentProfile getProfile(LanClientInfo info)
    {
        if (info == null) return null;
        DMTypedHashMap ginfo = (DMTypedHashMap) info.getGameData();
        if (ginfo != null) {
            return (TournamentProfile) ginfo.getObject(PokerMain.ONLINE_GAME_PROFILE);
        }
        return null;
    }


    /**
     * helper method to get online mode from LanClientInfo
     */
    private int getOnlineMode(LanClientInfo info)
    {
        if (info == null) return PokerGame.MODE_NONE;
        DMTypedHashMap ginfo = (DMTypedHashMap) info.getGameData();
        if (ginfo != null) {
            return ginfo.getInteger(PokerMain.ONLINE_GAME_STATUS, PokerGame.MODE_NONE);
        }
        return PokerGame.MODE_NONE;
    }

    /**
     * helper method to get lan connect string from LanClientInfo
     */
    private String getLanConnect(LanClientInfo info)
    {
        if (info == null) return null;
        DMTypedHashMap ginfo = (DMTypedHashMap) info.getGameData();
        if (ginfo != null) {
            return ginfo.getString(PokerMain.ONLINE_GAME_LAN_CONNECT);
        }
        return null;
    }

    /**
     * Return key of selected row
     */
    private String getSelectedRowKey()
    {
        int n = table_.getSelectedRow();
        if (n == -1) return null;
        return ((LanClientModel) model_).getClientInfo(n).getKey();
    }

    /**
     * Select row whose client info has given key
     */
    private void selectRow(String sKey)
    {
        if (sKey == null) return;
        ListSelectionModel selmodel = table_.getSelectionModel();
        int nNum = model_.getRowCount();
        for (int i = 0; i < nNum; i++)
        {
            if (((LanClientModel) model_).getClientInfo(i).getKey().equals(sKey))
            {
                selmodel.setSelectionInterval(i, i);
                table_.scrollRectToVisible(table_.getCellRect(i, 0, true));
                return;
            }
        }
    }

    /**
     * double click - press start button by default
     */
    protected void doubleClick()
    {
        LanClientInfo info = ((LanClientModel) model_).getClientInfo(table_.getSelectedRow());
        int mode = getOnlineMode(info);
        if (mode != PokerGame.MODE_PLAY)
        {
            start_.doClick();
        }
        else
        {
            obs_.doClick();
        }
    }

    // client table info
    private static final int[] COLUMN_WIDTHS = new int[] {
        100, 60, 100, 50
    };
    private static final String[] COLUMN_NAMES = new String[] {
        LanClientInfo.LAN_PLAYER_NAME, LanClientInfo.LAN_HOST_NAME, LanClientInfo.LAN_TCPIP_ADDRESS, LanClientInfo.LAN_GAME
    };

    static String GAME_INIT = PropertyConfig.getMessage("msg.mode.init");
    static String GAME_REG = PropertyConfig.getMessage("msg.mode.reg");
    static String GAME_INVITE = PropertyConfig.getMessage("msg.mode.invite");
    static String GAME_PLAYING = PropertyConfig.getMessage("msg.mode.play");

    /**
     * Used by table to display clients on lan
     */
    private class LanClientModel extends DDPagingTableModel implements LanListener
    {
        private LanClientList list;
        private List<LanClientInfo> clients;
        private String sSortKey = LanClientInfo.LAN_PLAYER_NAME;
        private boolean bSortAsc = true;

        public LanClientModel(LanClientList list)
        {
            this.list = list;
            list.addLanListener(this);
            getClientList();

        }

        private void getClientList()
        {
            clients = list.getAsList(sSortKey, bSortAsc);
        }

        public void cleanup()
        {
            list.removeLanListener(this);
        }

        public LanClientInfo getClientInfo(int r) {
            return clients.get(r);
        }

        public String getColumnName(int c) {
            return JoinGame.COLUMN_NAMES[c];
        }

        public int getColumnCount() {
            return JoinGame.COLUMN_WIDTHS.length;
        }

        public boolean isCellEditable(int r, int c) {
            return false;
        }

        public int getRowCount() {
            if (clients == null) {
                return 0;
            }
            return clients.size();
        }

        public Object getValueAt(int rowIndex, int colIndex)
        {
            String sName = JoinGame.COLUMN_NAMES[colIndex];
            LanClientInfo info = getClientInfo(rowIndex);

            if (sName.equals(LanClientInfo.LAN_GAME))
            {
                switch (getOnlineMode(info))
                {
                    case PokerGame.MODE_INIT: return GAME_INIT;
                    case PokerGame.MODE_REG:
                        if (getProfile(info).isInviteOnly()) return GAME_INVITE;
                        else return GAME_REG;
                    case PokerGame.MODE_PLAY: return GAME_PLAYING;
                    case PokerGame.MODE_NONE:
                    default:
                        return "";
                }
            }
            else
            {
                return info.getData().getObject(sName);
            }
        }

        /**
         * Get the total number of records.
         */
        public int getTotalCount()
        {
            return getRowCount();
        }

        /**
         * Refresh the table contents using the given offset and row count.
         */
        public void refresh(int offset, int rowCount)
        {
            // Does not currently use paging.
        }

        /**
         * update table when lan event received
         */
        public void lanEventReceived(LanEvent event)
        {
            if (event.getAction() == LanClientList.LAN_ALIVE) return;

            // run in swing thread
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        String sKey = getSelectedRowKey();
                        getClientList();
                        fireTableDataChanged();
                        selectRow(sKey);
                    }
                }
            );
        }
    }

    /**
     * Get the list display name
     */
    public String getListName()
    {
        return "lanlist";
    }

    /**
     * Get the list column names
     */
    public String[] getListColumnNames()
    {
        return COLUMN_NAMES;
    }

    /**
     * Get the list column widths
     */
    public int[] getListColumnWidths()
    {
        return COLUMN_WIDTHS;
    }

    /**
     * Returns a value to enable paging.
     */
    public int getListRowCount()
    {
        return -1;
    }

    /**
     * Get the model backing the list table
     */
    public DDPagingTableModel createListTableModel()
    {
        LanClientList list = lanManager_.getList();
        return new LanClientModel(list);
    }

    /**
     * show use last button
     */
    public boolean isDisplayUseLastButton()
    {
        return true;
    }
}
