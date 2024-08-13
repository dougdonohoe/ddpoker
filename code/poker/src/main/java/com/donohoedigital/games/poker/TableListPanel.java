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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 6, 2005
 * Time: 8:58:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableListPanel extends DDTabPanel implements ChangeListener, ActionListener
{
    private static final int NUMDISPLAY = 2;

    private GameContext context_;
    private PokerGame game_;
    private String STYLE;
    private DDPanel tbls_;
    private TablePanel tables_[];
    private DDSlider slider_;
    private DDCheckBox showtype_;

    public TableListPanel(GameContext context, String sStyle)
    {
        context_ = context;
        game_ = (PokerGame) context.getGame();
        STYLE = sStyle;
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    }

    public void createUI()
    {
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(10, 0);
        add(GuiUtils.NORTH(base), BorderLayout.CENTER);

        int nNumTables = game_.getNumTables();
        int nNumPlayers = game_.isGameOver() ? game_.getTable(0).getNumOccupiedSeats() : 
                          (game_.getNumPlayers() - game_.getNumPlayersOut());

        // top label and checkbox
        DDPanel toppanel = new DDPanel();
        add(toppanel, BorderLayout.NORTH);

        DDLabel topinfo = new DDLabel(GuiManager.DEFAULT, "TableList");
        topinfo.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        toppanel.add(topinfo, BorderLayout.CENTER);
        setBorderLayoutGap(10, 0);
        PokerPlayer p = game_.getLocalPlayer();
        List<PokerPlayer> waiting = game_.getWaitList();
        String sWait = "";
        if (waiting != null)
        {
            sWait = PropertyConfig.getMessage("msg.tablesum.waiting", waiting.size());
            nNumPlayers -= waiting.size();
        }
        topinfo.setText(PropertyConfig.getMessage(p.isObserver() ? "msg.tablesum.observer" : "msg.tablesum.player",
                        (nNumTables == 1 ? PropertyConfig.getMessage("msg.table") :
                                          PropertyConfig.getMessage("msg.tables", nNumTables)),
                        nNumPlayers,
                        p.getTable().getName(),
                        p.getSeat() + 1,
                        sWait));

        if (!game_.isOnlineGame())
        {
            String NODE = GameEngine.getGameEngine().getPrefsNodeName();
            OptionBoolean ob = OptionMenu.add(new OptionBoolean(NODE, PokerConstants.OPTION_SHOW_PLAYER_TYPE,
                                                                "OptionsDialog", new TypedHashMap(), true),
                                              toppanel, BorderLayout.EAST);
            showtype_ = ob.getCheckBox();
            showtype_.addActionListener(this);
        }

        // tables
        tbls_ = new DDPanel();
        tbls_.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

        int nNumDisplay = Math.min(nNumTables, NUMDISPLAY);
        tables_ = new TablePanel[nNumDisplay];
        for (int i = 0; i < nNumDisplay; i++)
        {
            tables_[i] = new TablePanel();
            tbls_.add(tables_[i]);
            tables_[i].updateTable(game_.getTable(i), isShowType());
        }
        base.add(tbls_, BorderLayout.CENTER);

        // bottom
        DDPanel bottom = new DDPanel();
        bottom.setBorderLayoutGap(10, 0);
        base.add(bottom, BorderLayout.SOUTH);

        // slider
        if (nNumTables > 2)
        {
            slider_ = new DDSlider(GuiManager.DEFAULT, STYLE);
            slider_.setMinimum(0);
            slider_.setValue(0);
            slider_.setMaximum(nNumTables - nNumDisplay);
            slider_.setPaintTicks(false);
            slider_.setMajorTickSpacing(10);
            slider_.setMinorTickSpacing(1);
            slider_.setSnapToTicks(true);
            bottom.add(slider_, BorderLayout.NORTH);
            slider_.addChangeListener(this);
            slider_.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        }

        // list waiting players
        if (waiting != null)
        {
            StringBuilder players = new StringBuilder();
            PokerPlayer player;
            for (int i = 0; i < waiting.size(); i++)
            {
                player = waiting.get(i);
                if (i > 0) players.append(", ");
                players.append(Utils.encodeHTML(player.getDisplayName(game_.isOnlineGame())));
            }
            DDLabel label = new DDLabel(GuiManager.DEFAULT, STYLE);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            bottom.add(label, BorderLayout.CENTER);
            label.setText(PropertyConfig.getMessage("msg.tablesum.waitlist", players.toString()));
        }
    }

    private boolean isShowType()
    {
        if (showtype_ == null) return false;
        return showtype_.isSelected();
    }

    public void stateChanged(ChangeEvent e)
    {
        int nStart = slider_ == null ? 0 : slider_.getValue();
        for (TablePanel table : tables_)
        {
            table.updateTable(game_.getTable(nStart++), isShowType());
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        stateChanged(null);
    }

    private class TablePanel extends DDPanel
    {
        DDLabel clabel;
        ChipLeaderPanel.PlayerModel model;
        DDScrollTable scroll;

        public TablePanel()
        {
            setBorderLayoutGap(5, 0);

            clabel = new DDLabel(GuiManager.DEFAULT, "TableList");
            add(clabel, BorderLayout.NORTH);

            scroll = new DDScrollTable(GuiManager.DEFAULT, STYLE, STYLE, PLAYING_NAMES, PLAYING_WIDTHS);
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // set to as needed to trigger size set below
            add(scroll, BorderLayout.CENTER);

            DDTable ddtable = scroll.getDDTable();
            model = new ChipLeaderPanel.PlayerModel(game_, null, PLAYING_NAMES, PLAYING_WIDTHS);
            ddtable.setModel(model);
            ddtable.setExporter(new TableExporter(context_, "tablelist"));
            ddtable.setShowHorizontalLines(true);
            ddtable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            ddtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            ddtable.setAlign(2, SwingConstants.RIGHT);
        }

        private void updateTable(PokerTable table, boolean bShowPlayerType)
        {
            clabel.setText("<HTML><B><font color=yellow>"+table.getName()+"</font></B>");

            int nNumObs = table.getNumObservers();
            List<ChipLeaderPanel.RankInfo> players = new ArrayList<ChipLeaderPanel.RankInfo>(PokerConstants.SEATS + nNumObs);
            PokerPlayer p;
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                p = table.getPlayer(i);
                players.add(new ChipLeaderPanel.RankInfo(p, i+1));
            }
            for (int i = 0; i < nNumObs; i++)
            {
                p = table.getObserver(i);
                players.add(new ChipLeaderPanel.RankInfo(p, -1));
            }

            int nPolicy = scroll.getVerticalScrollBarPolicy();
            int nNewPolicy = (nNumObs > 0) ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER;
            if (nPolicy != nNewPolicy)
            {
                scroll.setVerticalScrollBarPolicy(nNewPolicy);
                scroll.setPreferredSize(null);
                scroll.setPreferredSize(new Dimension(scroll.getPreferredWidth(), 189));
                tbls_.revalidate();
            }

            model.updatePlayers(players, bShowPlayerType);
        }
    }

    private static final String[] PLAYING_NAMES = new String[] {
       ChipLeaderPanel.COL_SEAT, ChipLeaderPanel.COL_NAME_PLAYERTYPE, ChipLeaderPanel.COL_CHIPS
    };
    // client table info
    private static final int[] PLAYING_WIDTHS = new int[] {
        40, ChipLeaderPanel.NW + 80, ChipLeaderPanel.CW
    };
}
