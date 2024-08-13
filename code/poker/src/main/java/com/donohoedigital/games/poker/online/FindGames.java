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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.beans.*;
import java.util.List;

/**
 * @author zak
 */
public class FindGames extends ListGames
{

    private OnlineGame selected_;


    /**
     * Creates a new instance of TournamentOptions
     */
    public FindGames()
    {
    }

    /**
     * Disable fields if not an online profile.
     *
     * @param engine    engine
     * @param context
     * @param gamephase game phase
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // need online activated profile for public games
        if (!profile_.isActivated())
        {
            connectLabel_.setEnabled(false);
            connectText_.setText(PropertyConfig.getMessage("msg.onlinerequired", profile_.getName()));
            connectText_.setEnabled(false);
            pubPaste_.setEnabled(false);
        }
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting()) return;

        ListSelectionModel lsm = table_.getSelectionModel();
        int index = lsm.getMinSelectionIndex();

        OnlineGame newSelection = null;

        if (index >= 0)
        {
            newSelection = ((WanGameModel) model_).getGameInfo(index);
        }

        // don't update if not new
        if (newSelection == selected_) return;

        // get current values
        boolean updateText = profile_.isActivated();
        String sCurrentText = connectText_.getText().trim();
        String sOldWanConnect = (selected_ != null) ? selected_.getUrl() : null;
        String sNewWanConnect = (newSelection != null) ? newSelection.getUrl() : null;
        TournamentProfile profile = null;
        String sConnect = null;

        // remember new selection
        selected_ = newSelection;

        // update details        
        if (selected_ != null)
        {
            profile = newSelection.getTournament();

            if (updateText)
            {
                sConnect = sNewWanConnect == null ? "" : sNewWanConnect;
            }
        }
        else
        {
            // if no current selection and the text in the connect field matches
            // the previous selection, clear it out
            if (updateText && sOldWanConnect != null && sCurrentText.equals(sOldWanConnect))
            {
                sConnect = "";
            }
        }

        if (profile != null) sum_.updateProfile(profile);
        else sum_.updateEmptyProfile("");

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
            OnlineGame game = ((WanGameModel) model_).getGameInfo(i);

            if (sConnect.equals(game.getUrl()))
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
     * set buttons enabled/disabled based on selection
     */
    @Override
    protected void checkButtons()
    {
        if (profile_.isActivated())
        {
            super.checkButtons();
        }
        else
        {
            // no join or observe if not an activated online profile
            start_.setEnabled(false);
            obs_.setEnabled(false);
        }
    }

    /**
     * double click - press start button by default
     */
    @Override
    protected void doubleClick()
    {
        OnlineGame info = ((WanGameModel) model_).getGameInfo(table_.getSelectedRow());
        if (info.getMode() == OnlineGame.MODE_REG)
        {
            start_.doClick();
        }
        else
        {
            obs_.doClick();
        }
    }

    // client table info
    private static final int[] COLUMN_WIDTHS = new int[]{
            150, 100, 60
    };
    private static final String[] COLUMN_NAMES = new String[]{
            OnlineGame.WAN_TOURNAMENT_NAME, OnlineGame.WAN_HOST_PLAYER, OnlineGame.WAN_MODE
    };

    private static final int ROW_COUNT = 10;

    /**
     * Used by table to display games on wan
     */
    private class WanGameModel extends DDPagingTableModel
    {
        private OnlineGameList list;

        public WanGameModel(OnlineGameList list)
        {
            this.list = list;
        }

        public void cleanup()
        {
        }

        public OnlineGame getGameInfo(int r)
        {
            return list.get(r);
        }

        @Override
        public String getColumnName(int c)
        {
            return COLUMN_NAMES[c];
        }

        @Override
        public int getColumnCount()
        {
            return COLUMN_WIDTHS.length;
        }

        @Override
        public boolean isCellEditable(int r, int c)
        {
            return false;
        }

        @Override
        public int getRowCount()
        {
            if (list == null)
            {
                return 0;
            }
            return list.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex)
        {
            String sName = COLUMN_NAMES[colIndex];
            OnlineGame game = getGameInfo(rowIndex);

            String sValue;
            if (sName.equals(OnlineGame.WAN_TOURNAMENT_NAME))
            {
                sValue = game.getTournament().getName();
            }
            else if (sName.equals(OnlineGame.WAN_MODE))
            {
                int nMode = game.getMode();
                switch (nMode)
                {
                    case OnlineGame.MODE_REG:
                        if (game.getTournament().isInviteOnly())
                        {
                            sValue = JoinGame.GAME_INVITE;
                        }
                        else
                        {
                            sValue = JoinGame.GAME_REG;
                        }
                        break;

                    case OnlineGame.MODE_PLAY:
                        sValue = JoinGame.GAME_PLAYING;
                        break;

                    default:
                        sValue = "";
                }
            }
            else
            {
                // FIX: don't use getData() to fetch information from OnlineGame
                sValue = (String) game.getData().getObject(sName);
            }
            return "<HTML>" + Utils.encodeHTML(sValue);
        }

        /**
         * Get the total number of records.
         */
        @Override
        public int getTotalCount()
        {
            return list.getTotalSize();
        }

        /**
         * Refresh the table contents using the given offset and row count.
         */
        @Override
        public void refresh(int offset, int rowCount)
        {
            list = getWanList(offset, rowCount, true);
            fireTableDataChanged();
        }
    }

    /**
     * Info panel displaying profile information
     */
    @Override
    public Component getListInfo()
    {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        DDPanel gbox = new DDPanel();
        gbox.setLayout(layout);
        gbox.setBorder(BorderFactory.createEmptyBorder(0, 15, 5, 0));

        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.BOTH;

        EngineButtonListener listener = new EngineButtonListener(context_, this, gamephase_.getButtonNameFromParam("profile"));
        DDImageButton button = new DDImageButton(listener.getGameButton().getName());
        button.addActionListener(listener);
        layout.setConstraints(button, constraints);
        gbox.add(button);

        constraints.insets = new Insets(5, 10, 5, 0);

        DDPanel profilepanel = new DDPanel();
        profilepanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, 3, VerticalFlowLayout.LEFT));
        layout.setConstraints(profilepanel, constraints);
        gbox.add(profilepanel);

        DDLabel label = new DDLabel(GuiManager.DEFAULT, "StartMenuSmall");
        String profileText = PropertyConfig.getMessage("msg.publicjoin.profile",
                                                       Utils.encodeHTML(profile_.getName()));
        label.setText(profileText);
        profilepanel.add(label);

        label = new DDLabel((profile_.isActivated() ? "publicjoin.enabled" : "publicjoin.disabled"), "StartMenuSmall");
        profilepanel.add(label);

        // add spacer
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;

        label = new DDLabel();
        layout.setConstraints(label, constraints);
        gbox.add(label);

        return gbox;
    }

    /**
     * Get the list display name
     */
    @Override
    public String getListName()
    {
        return "publicjoin";
    }

    /**
     * Get the list column names
     */
    @Override
    public String[] getListColumnNames()
    {
        return COLUMN_NAMES;
    }

    /**
     * Get the list column widths
     */
    @Override
    public int[] getListColumnWidths()
    {
        return COLUMN_WIDTHS;
    }

    /**
     * Returns a value to enable paging.
     */
    @Override
    public int getListRowCount()
    {
        return ROW_COUNT;
    }

    /**
     * Get the model backing the list table
     */
    @Override
    public DDPagingTableModel createListTableModel()
    {
        OnlineGameList list = getWanList(0, getListRowCount(), false);
        return new WanGameModel(list);
    }

    /**
     * Don't show use last button
     */
    @Override
    public boolean isDisplayUseLastButton()
    {
        return false;
    }

    /**
     * Get the current WAN list from the server.
     */
    private OnlineGameList getWanList(int offset, int count, boolean faceless)
    {
        // Send a message requesting that the game be added
        OnlineProfile auth = null;
        TypedHashMap hmParams = new TypedHashMap();
        hmParams.setInteger(GetWanList.PARAM_OFFSET, offset);
        hmParams.setInteger(GetWanList.PARAM_COUNT, count);
        hmParams.setInteger(GetWanList.PARAM_MODE, OnlineGame.FETCH_MODE_REG_PLAY);

        if (faceless)
        {
            hmParams.setBoolean(SendMessageDialog.PARAM_FACELESS, Boolean.TRUE);
        }
        else
        {
            // If the current profile is an activated online profile, then attempt to authenticate
            // it on the server the first time the list is retrieved.
            if (profile_.isActivated())
            {
                auth = new OnlineProfile(profile_.getName());
                auth.setPassword(profile_.getPassword());

                hmParams.setObject(GetWanList.PARAM_AUTH, auth);
            }

            hmParams.setBoolean(SendMessageDialog.PARAM_FACELESS, Boolean.FALSE);
        }

        SendMessageDialog dialog = (SendMessageDialog) context_.processPhaseNow("GetWanList", hmParams);

        OnlineGameList clients = new OnlineGameList();

        if (dialog.getStatus() == DDMessageListener.STATUS_OK)
        {
            EngineMessage resEngineMsg = dialog.getReturnMessage();
            OnlineMessage resOnlineMsg = new OnlineMessage(resEngineMsg);

            // Return a local copy of the list
            List<DMTypedHashMap> games = resOnlineMsg.getWanGames();

            if (auth != null)
            {
                // see PokerServlet.getWanGames()
                if (resOnlineMsg.getWanAuth() == null)
                {
                    // Authentication failed, so reset the local profile.
                    resetProfile();
                }
            }

            clients.setTotalSize(resOnlineMsg.getCount());

            if (games != null)
            {
                for (DMTypedHashMap game : games)
                {
                    clients.add(new OnlineGame(game));
                }
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
            }
        }

        return clients;
    }

}
