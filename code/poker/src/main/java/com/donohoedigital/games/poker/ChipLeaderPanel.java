/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 6, 2005
 * Time: 8:58:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChipLeaderPanel extends DDTabPanel
{
    GameContext context_;
    PokerGame game_;
    int nTopHeight_;

    public ChipLeaderPanel(GameContext context, int nTopHeight)
    {
        context_ = context;
        game_ = (PokerGame) context.getGame();
        nTopHeight_ = nTopHeight;
    }

    public void createUI()
    {
        PokerPlayer p;
        List<PokerPlayer> leaders = game_.getPlayersByRank();
        List<RankInfo> finished = new ArrayList<RankInfo>();
        List<RankInfo> current = new ArrayList<RankInfo>();
        int nNum = leaders.size();
        boolean bDone = game_.getNumPlayers() - game_.getNumPlayersOut() == 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
        int nChips;
        PokerPlayer human = game_.getHumanPlayer();
        int nHumanRank = 0;
        int nRank = 0;
        int nLastChips = 0;

        // current players
        for (int i = 0; !bDone && i < nNum; i++)
        {
            p = leaders.get(i);
            nChips = p.getChipCount();
            if (nChips == 0 && p.getPlace() != 0) continue;
            if (nChips != nLastChips)
            {
                nRank = (i+1);
            }
            nLastChips = nChips;
            current.add(new RankInfo(p, nRank));
            if (nChips < min) min = nChips;
            if (nChips > max) max = nChips;
            if (p == human)
            {
                nHumanRank = nRank;
            }
        }

        // finished players
        for (int i = 0; i < nNum; i++)
        {
            p = leaders.get(i);
            if ((p.getChipCount() != 0 && !bDone) || p.getPlace() == 0) continue;
            finished.add(new RankInfo(p, i+1));
            if (p == human)
            {
                nHumanRank = i+1;
            }
        }

        // base
        setBorderLayoutGap(10,0);

        // current players
        if (current.size() > 0)
        {
            DDPanel top = new DDPanel();
            top.setBorderLayoutGap(5, 10);
            add(top, BorderLayout.NORTH);

            DDPanel topleft = new DDPanel();
            topleft.setBorderLayoutGap(5, 0);
            top.add(topleft, BorderLayout.WEST);

            DDLabel clabel = new DDLabel("chipleaders", "TourneyStats");
            topleft.add(clabel, BorderLayout.NORTH);

            DDScrollTable scrollCurrent = new DDScrollTable(GuiManager.DEFAULT, "OptionsDialog", "OptionsDialog", PLAYING_NAMES, PLAYING_WIDTHS);
            scrollCurrent.setPreferredSize(new Dimension(scrollCurrent.getPreferredWidth(), nTopHeight_));
            topleft.add(scrollCurrent, BorderLayout.CENTER);

            DDTable table = scrollCurrent.getDDTable();
            PlayerModel model = new PlayerModel(game_, current, PLAYING_NAMES, PLAYING_WIDTHS);
            table.setModel(model);
            table.setExporter(new TableExporter(context_, "chipleaders"));
            table.setShowHorizontalLines(true);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setAlign(3, SwingConstants.RIGHT);

            DDPanel topright = new DDPanel();
            topright.setBorderLayoutGap(5, 0);
            top.add(topright, BorderLayout.CENTER);

            clabel = new DDLabel("tournstats", "TourneyStats");
            topright.add(clabel, BorderLayout.NORTH);

            DDHtmlArea summary = new DDHtmlArea(GuiManager.DEFAULT, "TourneyStats");
            summary.setDisplayOnly(true);
            summary.setBorder(BorderFactory.createEmptyBorder(2,3,2,3));
            topright.add(summary, BorderLayout.CENTER);

            int avg = game_.getAverageStack();
            int total = game_.getTotalChipsInPlay();
            int left = current.size();
            int num = game_.getNumPlayers();

            String details = "";

            if (human.getPlace() == 0 && !human.isObserver())
            {
                int chip = human.getChipCount();
                int big = game_.getProfile().getLastBigBlind(human.getTable().getLevel());
                double perc = ((double) chip / (double) total) * 100.0d;
                double multavg = (double) chip / (double) avg;
                double bbavg = (double) chip / (double) big;

                details = PropertyConfig.getMessage("msg.mystats",
                                                    chip,
                                                    PokerConstants.formatPercent(perc),
                                                    PokerConstants.formatPercent(multavg),
                                                    PokerConstants.formatPercent(bbavg),
                                                    PropertyConfig.getPlace(nHumanRank),
                                                    left
                );
            }
            else if (human.getPlace() > 0)
            {
                details = PropertyConfig.getMessage("msg.mystats2",
                                                    PropertyConfig.getPlace(nHumanRank),
                                                    num
                );
            }

            summary.setText(PropertyConfig.getMessage("msg.tournstats",
                                                      left, num,
                                                      total,
                                                      max,
                                                      avg,
                                                      min,
                                                      details
                                                      ));
        }

        // out players
        DDPanel bottom = new DDPanel();
        bottom.setBorderLayoutGap(5, 10);
        add(bottom, BorderLayout.CENTER);

        DDLabel clabel = new DDLabel(bDone ? "chipend":"chipingame", "TourneyStats");
        bottom.add(clabel, BorderLayout.NORTH);

        DDScrollTable scrollOut = new DDScrollTable(GuiManager.DEFAULT, "OptionsDialog", "OptionsDialog", RESULTS_NAMES, RESULTS_WIDTHS);
        scrollOut.setPreferredSize(new Dimension(scrollOut.getPreferredWidth(), 100));
        bottom.add(GuiUtils.WEST(scrollOut), BorderLayout.CENTER);

        DDTable table = scrollOut.getDDTable();
        PlayerModel model = new PlayerModel(game_, finished, RESULTS_NAMES, RESULTS_WIDTHS);
        table.setModel(model);
        table.setExporter(new TableExporter(context_, "results"));
        table.setShowHorizontalLines(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 2; i <= 6; i++)
        {
            table.setAlign(i, SwingConstants.RIGHT);
        }
    }

    static class RankInfo
    {
        PokerPlayer player;
        int nRank;

        RankInfo(PokerPlayer player, int nRank)
        {
            this.player = player;
            this.nRank = nRank;
        }
    }

    public static final String COL_SEAT = "seat";
    public static final String COL_RANK = "rank";
    public static final String COL_FINISH = "finish";
    public static final String COL_NAME = "name";
    public static final String COL_NAME_PLAYERTYPE = "nameptype";
    public static final String COL_TABLE = "table";
    public static final String COL_CHIPS = "chips";
    public static final String COL_BUYIN = "buyin";
    public static final String COL_REBUY = "rebuy";
    public static final String COL_ADDON = "addon";
    public static final String COL_PRIZE = "prize";
    public static final String COL_PROFIT ="profit";

    static final int RW = 60; // rank width
    static final int CW = 100; // chip width
    static final int NW = 100; // name width

    private static final String[] PLAYING_NAMES = new String[] {
       COL_RANK, COL_NAME, COL_TABLE, COL_CHIPS
    };
    // client table info
    private static final int[] PLAYING_WIDTHS = new int[] {
        RW, NW, 75, CW
    };
    private static final String[] RESULTS_NAMES = new String[] {
       COL_FINISH, COL_NAME, COL_PRIZE, COL_BUYIN, COL_REBUY, COL_ADDON, COL_PROFIT
    };
    // client table info
    private static final int[] RESULTS_WIDTHS = new int[] {
        RW, NW, CW, CW, CW, CW, CW
    };

    /**
     * Used by table to display players in game
     */
    static class PlayerModel extends DefaultTableModel
    {
        PokerGame game;
        List<RankInfo> players;
        String[] names;
        int[] widths;
        boolean bShowPlayerType;

        public PlayerModel(PokerGame game, List<RankInfo> players, String names[], int[] widths)
        {
            this.game = game;
            this.names = names;
            this.widths = widths;
            this.players = players;
        }

        public void updatePlayers(List<RankInfo> infoList, boolean b)
        {
            this.players = infoList;
            this.bShowPlayerType = b;
            fireTableDataChanged();
        }

        public PokerPlayer getPokerPlayer(int r) {
            return players.get(r).player;
        }

        public int getRank(int r) {
            return players.get(r).nRank;
        }

        public String getColumnName(int c) {
            return names[c];
        }

        public int getColumnCount() {
            return widths.length;
        }

        public boolean isCellEditable(int r, int c) {
            return false;
        }

        public int getRowCount() {
            if (players == null) return 0;
            return players.size();
        }

        public Object getValueAt(int rowIndex, int colIndex)
        {
            PokerPlayer p = getPokerPlayer(rowIndex);

            String sValue = "[bad column]";

            if (names[colIndex].equals(COL_RANK) ||
                names[colIndex].equals(COL_FINISH))
            {
                sValue = PropertyConfig.getPlace(getRank(rowIndex));
            }
            else if (names[colIndex].equals(COL_SEAT))
            {
                int seat = getRank(rowIndex);
                if (seat < 0) sValue = PropertyConfig.getMessage("msg.table.observer");
                else sValue = ""+getRank(rowIndex);
            }
            else if (names[colIndex].equals(COL_NAME) ||
                     names[colIndex].equals(COL_NAME_PLAYERTYPE))
            {
                if (p == null) return "";

                boolean bSet = false;
                if (bShowPlayerType && p != null && names[colIndex].equals(COL_NAME_PLAYERTYPE) &&
                                p.isComputer() &&
                                (!game.isOnlineGame() ||
                                 (game.isOnlineGame() && game.getLocalPlayer().isHost())))
                {
                    PlayerType ai = p.getPlayerType();
                    if (ai != null)
                    {
                        sValue = PropertyConfig.getMessage("msg.playername.playertype",
                                                           p.getName(), ai.getName());
                        bSet = true;
                    }
                }

                if (!bSet)
                {
                    sValue = p.getDisplayName(game.isOnlineGame());
                }
            }
            else if (names[colIndex].equals(COL_CHIPS))
            {
                if (p == null || getRank(rowIndex) < 0) return "";
                sValue = getNumber(p.getChipCount());
            }
            else if (names[colIndex].equals(COL_BUYIN))
            {
                sValue = getNumber(p.getBuyin());
            }
            else if (names[colIndex].equals(COL_REBUY))
            {
                sValue = getNumber(p.getRebuy());
            }
            else if (names[colIndex].equals(COL_ADDON))
            {
                sValue = getNumber(p.getAddon());
            }
            else if (names[colIndex].equals(COL_PRIZE))
            {
                sValue = getNumber(p.getPrize());
            }
            else if (names[colIndex].equals(COL_PROFIT))
            {
                sValue = getNumber(p.getPrize() - p.getTotalSpent());
            }
            else if (names[colIndex].equals(COL_TABLE))
            {
                // PATCH 2 - players waiting for table, just show "waiting"
                if (p.isWaiting())
                {
                    sValue = PropertyConfig.getMessage("msg.waiting");
                }
                // BUG 404 - could be null if window left up after players go out
                else if (p.getTable() == null)
                {
                    sValue = "";
                }
                else sValue = p.getTable().getName();
            }

            if (p != null && p.isLocallyControlled() && p.isHuman())
            {
                if (names[colIndex].equals(COL_NAME)||
                    names[colIndex].equals(COL_NAME_PLAYERTYPE)) sValue = Utils.encodeHTML(sValue);
                return "<HTML><font color=blue>" + sValue + "</font>";
            }
            return sValue;
        }

        private String getNumber(int nNum)
        {
            if (nNum < 0)
            {
                return "<HTML>" +
                       PropertyConfig.getMessage("msg.chip.net.neg", -1 * nNum);
            }
            else
            {
                return "<HTML>" +
                       PropertyConfig.getMessage("msg.chip.net.pos", nNum);
            }
        }
    }
}
