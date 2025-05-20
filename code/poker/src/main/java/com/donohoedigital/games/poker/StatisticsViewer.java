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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.db.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.List;

public class StatisticsViewer extends BasePhase implements ActionListener
{
    public static final String COL_FINISH = "finish";
    public static final String COL_NAME = "name";
    public static final String COL_ENDDATE = "enddate";
    public static final String COL_TOTALBUYIN = "totalbuyin";
    public static final String COL_REBUY = "rebuy";
    public static final String COL_ADDON = "addon";
    public static final String COL_PRIZE = "prize";
    public static final String COL_PROFIT ="profit";

    private static final int RW = 120; // rank width
    private static final int CW = 100; // chip width
    private static final int NW = 188; // name width
    private static final int DW = 140; // name width

    private DDTable finishTable_;

    private static final String[] RESULTS_NAMES = new String[] {
       COL_FINISH, COL_NAME, COL_ENDDATE, COL_TOTALBUYIN, COL_PRIZE, COL_PROFIT
    };
    // client table info
    private static final int[] RESULTS_WIDTHS = new int[] {
        RW, NW, DW, CW, CW, CW
    };

    private ResultsModel resultsModel_;
    private PlayerProfile profile_;
    private MenuBackground menu_;
    private ButtonBox buttonbox_;
    private DDTabbedPane tabs_;
    private GlassButton delete_;

    public StatisticsViewer()
    {
    }

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(5, 10);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        DDPanel topinfo = new DDPanel();
        top.add(topinfo, BorderLayout.NORTH);

        DDLabel clabel = new DDLabel(GuiManager.DEFAULT, "Analysis");
        topinfo.add(clabel, BorderLayout.CENTER);
        topinfo.setBorder(BorderFactory.createEmptyBorder(0,0,0,17));
        profile_ = PlayerProfileOptions.getDefaultProfile();
        clabel.setText(PropertyConfig.getMessage("msg.statsfor", profile_.getName()));

        GlassButton change = new GlassButton("changeprofile", "Glass");
        topinfo.add(change, BorderLayout.EAST);
        change.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                context_.processPhase("PlayerProfileOptions");
            }
        });

        DDScrollTable scrollOut = new DDScrollTable
                (GuiManager.DEFAULT, "PokerPrefsPlayerList", "BrushedMetal", RESULTS_NAMES, RESULTS_WIDTHS);
        scrollOut.setPreferredSize(new Dimension(scrollOut.getPreferredWidth(), 104));
        top.add(GuiUtils.WEST(scrollOut), BorderLayout.CENTER);

        finishTable_ = scrollOut.getDDTable();

        resultsModel_ = new ResultsModel(getFinishes(), RESULTS_NAMES, RESULTS_WIDTHS);
        finishTable_.setModel(resultsModel_);
        finishTable_.setExporter(new TableExporter(context_, "tournaments"));
        finishTable_.setRowSelectionInterval(0, 0);
        finishTable_.setShowHorizontalLines(true);
        finishTable_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        finishTable_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 3; i <= 5; i++)
        {
            finishTable_.setAlign(i, SwingConstants.RIGHT);
        }

        menu_ = new MenuBackground(gamephase);
        buttonbox_ = new ButtonBox(context_, gamephase, this, "empty", false, false);
        menu_.getMenuBox().add(buttonbox_, BorderLayout.SOUTH);

        String style = gamephase_.getString("menubox-style", "StartMenu");

        DDPanel base = new DDPanel(GuiManager.DEFAULT, style);
        menu_.getMenuBox().setBorderLayoutGap(5,0);
        menu_.getMenuBox().add(base, BorderLayout.CENTER);

        tabs_ = new DDTabbedPane(style, "BrushedMetal", JTabbedPane.TOP);
        tabs_.setOpaque(false);
        tabs_.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                checkDetailsButton();
            }
        });

        base.add(GuiUtils.CENTER(top), BorderLayout.NORTH);
        DDPanel overlay = new DDPanel();
        base.add(overlay, BorderLayout.CENTER);
        overlay.setLayout(new SVLayout());

        DDPanel deletebase = new DDPanel();
        deletebase.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        delete_ = new GlassButton("delete", "Glass");
        delete_.addActionListener(this);
        checkButtons();
        deletebase.add(delete_, BorderLayout.CENTER);
        overlay.add(deletebase, new ScaleConstraintsFixed(SwingConstants.TOP, SwingConstants.RIGHT));
        overlay.add(tabs_, BorderLayout.CENTER);

        ImageComponent ic = new ImageComponent("ddlogo20", 1.0d);

        ic.setScaleToFit(false);
        ic.setIconWidth(GamePrefsPanel.ICWIDTH);
        ic.setIconHeight(GamePrefsPanel.ICHEIGHT);

        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.overall"), ic, new OverallPanel(), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.byhand"), ic, new ByHandPanel(), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.preflop"), ic, new ByRoundPanel(HoldemHand.ROUND_PRE_FLOP), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.flop"), ic, new ByRoundPanel(HoldemHand.ROUND_FLOP), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.turn"), ic, new ByRoundPanel(HoldemHand.ROUND_TURN), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.river"), ic, new ByRoundPanel(HoldemHand.ROUND_RIVER), null);

        finishTable_.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if (e.getValueIsAdjusting()) return;

                if (finishTable_.getSelectedRow() < 0)
                {
                    finishTable_.setRowSelectionInterval(0, 0);
                    finishTable_.repaint();
                }
                else
                {
                    Component c = tabs_.getSelectedComponent();

                    if (c instanceof OverallPanel) ((OverallPanel)c).refresh();
                    if (c instanceof ByHandPanel) ((ByHandPanel)c).refresh();
                    if (c instanceof ByRoundPanel) ((ByRoundPanel)c).refresh();
                }
            }
        });
    }

    /**
     * get array of finishes
     */
    private List<TournamentHistory> getFinishes()
    {
        List<TournamentHistory> finishes = profile_.getHistory();
        finishes.add(0, profile_.getOverallHistory());
        return finishes;
    }

    /**
     * set delete button enabled
     */
    private void checkButtons()
    {
        delete_.setEnabled(finishTable_.getRowCount() > 1);
    }

    /**
     * delete button
     */
    public void actionPerformed(ActionEvent e)
    {
        boolean bDelete = false;
        if (finishTable_.getSelectedRow() == 0)
        {
            bDelete = PlayerProfileOptions.deleteAllHistory(context_, profile_);
        }
        else
        {
            bDelete = PlayerProfileOptions.deleteHistory(context_, profile_, finishTable_.getSelectedRow() - 1);
        }
        if (bDelete)
        {
            resultsModel_.updateFinishes(getFinishes());
            checkButtons();
        }
    }

    /**
     * Set component
     */
    @Override
    public void start()
    {
        context_.setMainUIComponent(this, menu_, false, tabs_);
    }

    /**
     * interface to indicate if details can be shown
     */
    private interface ShowDetails
    {
        public boolean canShowDetails();
        public void showDetails();
        public void exportHistory();
    }

    private static final String[] byHandColNames_ =
            {
                "stats.cards",
                "stats.numhands",
                "stats.winpct",
                "stats.losepct",
                "stats.passpct",
                "stats.winbets",
                "stats.seeflop",
                "stats.seeturn",
                "stats.seeriver",
                "stats.seeshowdown"
            };

    private static class ByHandModel extends DatabaseQueryTableModel
    {
        public ByHandModel(String sWhere, String sGroupBy, String sOrderBy, BindArray bindArray, boolean bHands)
        {
            // bHands is a hack to work around an apparent sproc bug in hsqldb

            super(PokerDatabase.getDatabase(),
                    byHandColNames_,
                    "SELECT " + (bHands ? "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)" : "'??'") + ',' +
                    "COUNT(*),\n" +
                    "CONCAT(SUM(CASE WHEN PLH_END_CHIPS > PLH_START_CHIPS THEN 1.0 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN PLH_END_CHIPS < PLH_START_CHIPS THEN 1.0 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN PLH_END_CHIPS = PLH_START_CHIPS THEN 1.0 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "AVG((PLH_END_CHIPS - PLH_START_CHIPS) / (1.00 * HND_BIG_BLIND)),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(PLH_PREFLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 THEN 0 ELSE 1 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(PLH_PREFLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 OR  " +
                    "BITAND(PLH_FLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 THEN 0 ELSE 1 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(PLH_PREFLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 OR " +
                    "BITAND(PLH_FLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 OR " +
                    "BITAND(PLH_TURN_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 THEN 0 ELSE 1 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(PLH_PREFLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 OR " +
                    "BITAND(PLH_FLOP_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 OR " +
                    "BITAND(PLH_TURN_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 OR " +
                    "BITAND(PLH_RIVER_ACTIONS , " + PokerDatabase.BIT_FOLD + ") <> 0 THEN 0 ELSE 1 END) * 100 / COUNT(*),'%')\n" +
                    "FROM TOURNAMENT_PLAYER, PLAYER_HAND, HAND\n" +
                    "WHERE PLH_HAND_ID=HND_ID AND PLH_PLAYER_ID=TPL_ID" +
                    (sWhere == null ? "" : "\nAND " + sWhere) +
                    (sGroupBy == null ? "" : "\nGROUP BY " + sGroupBy) +
                    (sOrderBy == null ? "" : "\nORDER BY " + sOrderBy), bindArray);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex == 0)
            {
                return Object.class;
            } else
            {
                return Number.class;
            }
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            Object v = super.getValueAt(row, column);

            if ("0%".equals(v))
                return null;
            else
                return v;
        }
    }

    private class OverallModel extends DefaultTableModel
    {
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }

    private class OverallPanel extends DDTabPanel implements ShowDetails
    {
        DDTable table_;

        public OverallPanel()
        {
            super();
            createUI(); // first panel, so create at outset
        }

        public void refresh()
        {
            OverallModel tmodel = new OverallModel();
            tmodel.setColumnCount(2);

            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.TIMESTAMP, new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate()));
            TournamentHistory hist = getSelectedTournament();
            if (hist.getGameId() != 0)
            {
                bindArray.addValue(Types.INTEGER, hist.getGameId());
            }
            ByHandModel dmodel = new ByHandModel(
                    "PLH_PLAYER_ID IN (\n" +
                    "SELECT TPL_ID FROM TOURNAMENT_PLAYER\n" +
                    "WHERE TPL_PROFILE_CREATE_DATE=?" +
                    (hist.getGameId() != 0 ? " AND TPL_TOURNAMENT_ID=?" : "") +
                                                                              ')',null, null, bindArray, false);

            GameEngine engine = GameEngine.getGameEngine();
            String sLocale = null;

            if (engine != null)
            {
                sLocale = engine.getLocale();
            }

            int nPlace = hist.getPlace();

            if (hist.getGameId() != 0)
            {
                tmodel.addRow(new Object[]{
                    PropertyConfig.getMessage("msg.handhistory.overall.tournamentname"),
                    hist.getTournamentName() +
                        " (" +
                        PropertyConfig.getMessage("msg.handhistory.tournamenttype." + hist.getTournamentType()) +
                                                                                                                ')'});
                tmodel.addRow(new Object[]{
                    PropertyConfig.getMessage("msg.handhistory.overall.startdate"),
                    PropertyConfig.getDateFormat(sLocale).format(hist.getStartDate())});

                if (nPlace == 0)
                {
                    tmodel.addRow(new Object[]{
                        PropertyConfig.getMessage("msg.handhistory.overall.enddate"),
                        PropertyConfig.getMessage("msg.incomplete")});
                    tmodel.addRow(new Object[]{
                        PropertyConfig.getMessage("msg.handhistory.overall.lastdate"),
                        PropertyConfig.getDateFormat(sLocale).format(hist.getEndDate())});
                }
                else
                {
                    tmodel.addRow(new Object[]{
                        PropertyConfig.getMessage("msg.handhistory.overall.enddate"),
                        PropertyConfig.getDateFormat(sLocale).format(hist.getEndDate())});
                }
            }
            else
            {
                tmodel.addRow(new Object[]{
                    PropertyConfig.getMessage("msg.handhistory.overall.tournamentname"),
                    hist.getTournamentName()});
            }
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.buyin"),
                getNumber(hist.getBuyin(), false)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.totalrebuys"),
                getNumber(hist.getRebuy(), false)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.totaladdons"),
                getNumber(hist.getAddon(), false)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.totalbuyin"),
                getNumber(hist.getTotalSpent(), false)});
            if (nPlace > 0)
            {
                tmodel.addRow(new Object[]{
                    PropertyConfig.getMessage("msg.handhistory.overall.prize"),
                    getNumber(hist.getPrize(), false)});
                tmodel.addRow(new Object[]{
                    PropertyConfig.getMessage("msg.handhistory.overall.profit"),
                    getNumber(hist.getPrize() - hist.getTotalSpent(), false)});
            }
            if (hist.getGameId() != 0)
            {
                if (nPlace > 0)
                {
                    tmodel.addRow(new Object[]{
                        PropertyConfig.getMessage("msg.handhistory.overall.finishplace"),
                        PropertyConfig.getMessage("msg.finishoutof",
                                PropertyConfig.getPlace(nPlace),
                                hist.getNumPlayers())});
                }
                else
                {
                    tmodel.addRow(new Object[]{
                        PropertyConfig.getMessage("msg.handhistory.overall.playersremaining"),
                        PropertyConfig.getMessage("msg.finishoutof",
                                                  hist.getNumRemaining(),
                                                  hist.getNumPlayers())});
                }
            }
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.numhands"),
                dmodel.getValueAt(0, 1)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.winpct"),
                dmodel.getValueAt(0, 2)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.losepct"),
                dmodel.getValueAt(0, 3)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.passpct"),
                dmodel.getValueAt(0, 4)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.winbets"),
                dmodel.getValueAt(0, 5)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.seeflop"),
                dmodel.getValueAt(0, 6)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.seeturn"),
                dmodel.getValueAt(0, 7)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.seeriver"),
                dmodel.getValueAt(0, 8)});
            tmodel.addRow(new Object[]{
                PropertyConfig.getMessage("msg.handhistory.overall.seeshowdown"),
                dmodel.getValueAt(0, 9)});
            table_.setModel(tmodel);
            table_.setExporter(new TableExporter(context_, "overall"));
        }

        @Override
        public void ancestorAdded(AncestorEvent event)
        {
            super.ancestorAdded(event);
            refresh();
        }

        public boolean canShowDetails()
        {
            return true;
        }

        public void exportHistory()
        {
            context_.processPhaseNow("HistoryExportDialog", getDetailsParams());
        }

        public void showDetails()
        {
            context_.processPhaseNow("HandHistoryDialog", getDetailsParams());
        }

        private TypedHashMap getDetailsParams()
        {
            TypedHashMap params = new TypedHashMap();
            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.TIMESTAMP, new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate()));
            TournamentHistory hist = getSelectedTournament();
            if (hist.getGameId() != 0)
            {
                bindArray.addValue(Types.INTEGER, hist.getGameId());
            }
            params.setString("where",
                    "HND_ID IN (SELECT PLH_HAND_ID FROM PLAYER_HAND\n" +
                    "WHERE PLH_PLAYER_ID IN (\n" +
                    "SELECT TPL_ID FROM TOURNAMENT_PLAYER\n" +
                    "WHERE TPL_PROFILE_CREATE_DATE=?" +
                    (hist.getGameId() != 0 ? " AND TPL_TOURNAMENT_ID=?" : "") +
                    "))");
            params.setObject("bindArray", bindArray);

            return params;
        }

        @Override
        public void createUI()
        {
            DDScrollTable scrollTable = new DDScrollTable
                    ("stats", "PokerPrefsPlayerList", "BrushedMetal",
                            new String[]{ "stats.statname", "stats.statvalue"},
                            new int[]{160, 200});
            table_ = scrollTable.getDDTable();
            table_.setTableHeader(null);
            table_.setRowSelectionAllowed(false);
            /*
            table_.getSelectionModel().addListSelectionListener(new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    checkDetailsButton();
                }
            });
            table_.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        showDetails();
                    }
                }

                public void mousePressed(MouseEvent e)
                {
                }

                public void mouseReleased(MouseEvent e)
                {
                }

                public void mouseEntered(MouseEvent e)
                {
                }

                public void mouseExited(MouseEvent e)
                {
                }
            });
            */
            scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            add(GuiUtils.CENTER(scrollTable), BorderLayout.CENTER);
            refresh();
            checkDetailsButton();
        }
    }

    private class ByHandPanel extends DDTabPanel implements ShowDetails
    {
        DDTable table_;

        public ByHandPanel()
        {
            super();
        }

        public void refresh()
        {
            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.TIMESTAMP, new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate()));
            TournamentHistory hist = getSelectedTournament();
            if (hist.getGameId() != 0)
            {
                bindArray.addValue(Types.INTEGER, hist.getGameId());
            }
            table_.setModel(new ByHandModel(
                    "TPL_PROFILE_CREATE_DATE=?" + (hist.getGameId() == 0 ? "" : " AND TPL_TOURNAMENT_ID=?"),
                    "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)",
                    "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClassRank\"(PLH_CARD_1, PLH_CARD_2) DESC", bindArray, true));
            table_.setExporter(new TableExporter(context_, "byhand"));
        }

        @Override
        public void ancestorAdded(AncestorEvent event)
        {
            super.ancestorAdded(event);
            refresh();
        }

        public boolean canShowDetails()
        {
            return (table_ != null) && table_.getSelectedRow() >= 0;
        }

        public void exportHistory()
        {
            context_.processPhaseNow("HistoryExportDialog", getDetailsParams());
        }

        public void showDetails()
        {
            context_.processPhaseNow("HandHistoryDialog", getDetailsParams());
        }

        private TypedHashMap getDetailsParams()
        {
            TypedHashMap params = new TypedHashMap();
            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.TIMESTAMP, new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate()));
            TournamentHistory hist = getSelectedTournament();
            if (hist.getGameId() != 0)
            {
                bindArray.addValue(Types.INTEGER, hist.getGameId());
            }
            int rowcount = 0;
            for (int i = 0; i < table_.getRowCount(); ++i)
            {
                if (table_.isRowSelected(i))
                {
                    bindArray.addValue(Types.VARCHAR, table_.getValueAt(i, 0));
                    ++rowcount;
                }
            }
            params.setString("where",
                    "HND_ID IN (SELECT PLH_HAND_ID FROM PLAYER_HAND\n" +
                    "WHERE PLH_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_PROFILE_CREATE_DATE=?\n" +
                    (hist.getGameId() == 0 ? "" : " AND TPL_TOURNAMENT_ID=?") + ") " +
                    " AND " +
                    "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)" + rowComparison(rowcount) + ')');
            params.setObject("bindArray", bindArray);

            return params;
        }

        @Override
        public void createUI()
        {
            DDScrollTable scrollTable = new DDScrollTable("stats", "PokerPrefsPlayerList", "BrushedMetal", byHandColNames_, new int[]{
                40,
                40,
                40,
                40,
                40,
                40,
                40,
                40,
                40,
                40
            });

            scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            add(scrollTable, BorderLayout.CENTER);

            table_ = scrollTable.getDDTable();

            table_.getSelectionModel().addListSelectionListener(new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    checkDetailsButton();
                }
            });
            table_.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        showDetails();
                    }
                }

                public void mousePressed(MouseEvent e)
                {
                }

                public void mouseReleased(MouseEvent e)
                {
                }

                public void mouseEntered(MouseEvent e)
                {
                }

                public void mouseExited(MouseEvent e)
                {
                }
            });
            refresh();
            checkDetailsButton();
        }
    }

    private static final String[] byRoundPreFlopColNames_ =
            {
                "stats.cards",
                "stats.numhands",
                "stats.roundchecked",
                "stats.roundcalled",
                "stats.roundbet",
                "stats.roundraised",
                "stats.roundreraised",
                "stats.roundfolded",
                "stats.roundwon"
            };

    private static final String[] byRoundColNames_ =
            {
                "stats.cards",
                "stats.numhands",
                "stats.roundchecked",
                "stats.roundcheckraised",
                "stats.roundcalled",
                "stats.roundbet",
                "stats.roundraised",
                "stats.roundreraised",
                "stats.roundfolded",
                "stats.roundwon"
            };

    private static class ByRoundModel extends DatabaseQueryTableModel
    {
        public ByRoundModel(int nRound, String sWhere, String sGroupBy, String sOrderBy, BindArray bindArray)
        {
            super(PokerDatabase.getDatabase(),
                    byRoundColNames_,
                    "SELECT \"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)," +
                    "COUNT(*),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_CHECK + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    ((nRound == HoldemHand.ROUND_PRE_FLOP) ? "" : "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_CHECK + ") > 0 AND BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_RAISE + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n") +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_CALL + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_BET + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_RAISE + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_RERAISE + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_FOLD + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%'),\n" +
                    "CONCAT(SUM(CASE WHEN BITAND(" + getRoundColumn(nRound) + ',' + PokerDatabase.BIT_WIN + ") > 0 THEN 1 ELSE 0 END) * 100 / COUNT(*),'%')\n" +
                    "FROM TOURNAMENT_PLAYER, PLAYER_HAND\n" +
                    where(nRound, sWhere) +
                    (sGroupBy == null ? "" : "\nGROUP BY " + sGroupBy) +
                    (sOrderBy == null ? "" : "\nORDER BY " + sOrderBy), bindArray);
        }

        public static String where(int nRound, String sWhere)
        {
            StringBuilder buf = new StringBuilder();

            buf.append("\nWHERE PLH_PLAYER_ID=TPL_ID AND ");

            buf.append(getRoundColumn(nRound));
            buf.append("<>0");

            if (sWhere != null)
            {
                buf.append(" AND ");
                buf.append('(');
                buf.append(sWhere);
                buf.append(')');
            }

            return buf.toString();
        }
        public static String getRoundColumn(int nRound)
        {
            switch (nRound)
            {
                case HoldemHand.ROUND_PRE_FLOP:
                    return "PLH_PREFLOP_ACTIONS";
                case HoldemHand.ROUND_FLOP:
                    return "PLH_FLOP_ACTIONS";
                case HoldemHand.ROUND_TURN:
                    return "PLH_TURN_ACTIONS";
                case HoldemHand.ROUND_RIVER:
                    return "PLH_RIVER_ACTIONS";
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex == 0)
            {
                return Object.class;
            } else
            {
                return Number.class;
            }
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            Object v = super.getValueAt(row, column);

            if ("0%".equals(v)) return null;
            else return v;
        }
    }

    @Override
    public boolean processButton(GameButton button)
    {
        if ("export".equals(button.getName()))
        {
            Component c = tabs_.getSelectedComponent();

            if (c instanceof ShowDetails)
            {
                ((ShowDetails) c).exportHistory();
            }
        }
        else if ("handdetails".equals(button.getName()))
        {
            Component c = tabs_.getSelectedComponent();

            if (c instanceof ShowDetails)
            {
                ((ShowDetails) c).showDetails();
            }
        }
        return super.processButton(button);
    }

    private void checkDetailsButton()
    {
        Component c = tabs_.getSelectedComponent();

        if (c instanceof ShowDetails)
        {
            buttonbox_.getButton("export").setEnabled(((ShowDetails) c).canShowDetails());
            buttonbox_.getButton("handdetails").setEnabled(((ShowDetails) c).canShowDetails());
        }
        else
        {
            buttonbox_.getButton("export").setEnabled(false);
            buttonbox_.getButton("handdetails").setEnabled(false);
        }
    }

    private class ByRoundPanel extends DDTabPanel implements ShowDetails
    {
        DDTable table_;
        int nRound_;

        public ByRoundPanel(int nRound)
        {
            super();
            nRound_ = nRound;
        }

        public void refresh()
        {
            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.TIMESTAMP, new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate()));
            TournamentHistory hist = getSelectedTournament();
            if (hist.getGameId() != 0)
            {
                bindArray.addValue(Types.INTEGER, hist.getGameId());
            }
            table_.setModel(new ByRoundModel(nRound_,
                    "TPL_PROFILE_CREATE_DATE=?" + (hist.getGameId() == 0 ? "" : " AND TPL_TOURNAMENT_ID=?"),
                    "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)",
                    "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClassRank\"(PLH_CARD_1, PLH_CARD_2) DESC", bindArray));
            table_.setExporter(new TableExporter(context_, HoldemHand.getRoundName(nRound_)));
        }

        @Override
        public void ancestorAdded(AncestorEvent event)
        {
            super.ancestorAdded(event);
            refresh();
        }

        public boolean canShowDetails()
        {
            return (table_ != null) && table_.getSelectedRow() >= 0;
        }

        public void exportHistory()
        {
            context_.processPhaseNow("HistoryExportDialog", getDetailsParams());
        }

        public void showDetails()
        {
            context_.processPhaseNow("HandHistoryDialog", getDetailsParams());
        }

        private TypedHashMap getDetailsParams()
        {
            TypedHashMap params = new TypedHashMap();
            BindArray bindArray = new BindArray();
            bindArray.addValue(Types.TIMESTAMP, new Timestamp(PlayerProfileOptions.getDefaultProfile().getCreateDate()));
            TournamentHistory hist = getSelectedTournament();
            if (hist.getGameId() != 0)
            {
                bindArray.addValue(Types.INTEGER, hist.getGameId());
            }
            int rowcount = 0;
            for (int i = 0; i < table_.getRowCount(); ++i)
            {
                if (table_.isRowSelected(i))
                {
                    bindArray.addValue(Types.VARCHAR, table_.getValueAt(i, 0));
                    ++rowcount;
                }
            }
            /*
            ArrayList hands = PokerDatabase.getHandIDs("EXISTS (SELECT * FROM PLAYER_HAND\n" +
                    ByRoundModel.where(nRound_,
                            "PLH_HAND_ID=HND_ID\n" +
                    " AND PLH_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_PROFILE_CREATE_DATE=?) " +
                    " AND " +
                    "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)=?)"),
                    bindArray);
            */
            params.setString("where",
                    "HND_ID IN (SELECT PLH_HAND_ID FROM PLAYER_HAND, TOURNAMENT_PLAYER\n" +
                    ByRoundModel.where(nRound_,
                        "PLH_PLAYER_ID IN (SELECT TPL_ID FROM TOURNAMENT_PLAYER WHERE TPL_PROFILE_CREATE_DATE=?" +
                        (hist.getGameId() == 0 ? "" : " AND TPL_TOURNAMENT_ID=?") + ") " +
                        " AND " +
                        "\"com.donohoedigital.games.poker.PokerDatabaseProcs.getHandClass\"(PLH_CARD_1, PLH_CARD_2)" + rowComparison(rowcount) + ')'
                    )
            );
            params.setObject("bindArray", bindArray);

            return params;
        }

        @Override
        public void createUI()
        {
            DDScrollTable scrollTable = new DDScrollTable("stats", "PokerPrefsPlayerList", "BrushedMetal",
                (nRound_ == HoldemHand.ROUND_PRE_FLOP) ?
                    byRoundPreFlopColNames_ : byRoundColNames_,
                (nRound_ == HoldemHand.ROUND_PRE_FLOP) ?
                    new int[]{30, 30, 40, 40, 40, 40, 40, 40, 40} :
                    new int[]{30, 30, 40, 40, 40, 40, 40, 40, 40, 40}
            );

            scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            add(scrollTable, BorderLayout.CENTER);

            table_ = scrollTable.getDDTable();

            table_.getSelectionModel().addListSelectionListener(new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    checkDetailsButton();
                }
            });
            table_.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        showDetails();
                    }
                }

                public void mousePressed(MouseEvent e)
                {
                }

                public void mouseReleased(MouseEvent e)
                {
                }

                public void mouseEntered(MouseEvent e)
                {
                }

                public void mouseExited(MouseEvent e)
                {
                }
            });
            checkDetailsButton();
        }
    }

    /**
     * Used by table to display tournament finishes.
     */
    static class ResultsModel extends DefaultTableModel
    {
        List<TournamentHistory> finishes;
        String[] names;
        int[] widths;

        public ResultsModel(List<TournamentHistory> finishes, String names[], int[] widths)
        {
            this.finishes = finishes;
            this.names = names;
            this.widths = widths;
        }

        public void updateFinishes(List<TournamentHistory> f)
        {
            this.finishes = f;
            fireTableDataChanged();
        }

        public TournamentHistory getTournamentHistory(int r) {
            return finishes.get(r);
        }

        public int getRank(int r) {
            return getTournamentHistory(r).getPlace();
        }

        @Override
        public String getColumnName(int c) {
            return names[c];
        }

        @Override
        public int getColumnCount() {
            return widths.length;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public int getRowCount() {
            if (finishes == null) return 0;
            return finishes.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex)
        {
            TournamentHistory h = getTournamentHistory(rowIndex);

            if (h == null) return null;

            String sValue = "[bad column]";

            if (names[colIndex].equals(COL_FINISH))
            {
                int finish = getRank(rowIndex);
                if (finish > 0)
                {
                    sValue = PropertyConfig.getMessage(
                            "msg.finishoutof",
                            PropertyConfig.getPlace(finish),
                            h.getNumPlayers());
                }
                else if (rowIndex == 0)
                {
                    sValue = PropertyConfig.getMessage("msg.summary");
                }
                else
                {
                    sValue = null;
                }
            }
            else if (names[colIndex].equals(COL_NAME))
            {
                sValue = h.getTournamentName();
            }
            else if (names[colIndex].equals(COL_ENDDATE))
            {
                if (h.getPlace() > 0)
                {
                    GameEngine engine = GameEngine.getGameEngine();
                    String sLocale = null;

                    if (engine != null)
                    {
                        sLocale = engine.getLocale();
                    }

                    sValue = PropertyConfig.getDateFormat("msg.format.shortdatetime", sLocale).format(h.getEndDate());
                }
                else
                {
                    sValue = (rowIndex == 0) ? null : PropertyConfig.getMessage("msg.incomplete");
                }
            }
            else if (names[colIndex].equals(COL_TOTALBUYIN))
            {
                sValue = getNumber(h.getTotalSpent(), rowIndex == 0);
            }
            else if (names[colIndex].equals(COL_PRIZE))
            {
                sValue = getNumber(h.getPrize(), rowIndex == 0);
            }
            else if (names[colIndex].equals(COL_PROFIT))
            {
                sValue = getNumber(h.getPrize() - h.getTotalSpent(), rowIndex == 0);
            }

            return sValue;
        }
    }

    private TournamentHistory getSelectedTournament()
    {
        int selectedTournament = finishTable_.getSelectedRow();

        return ((ResultsModel)finishTable_.getModel()).getTournamentHistory(selectedTournament);
    }

    private String rowComparison(int rowcount)
    {
        if (rowcount > 1)
        {
            StringBuilder buf = new StringBuilder(" IN (");
            for (int i = 0; i < rowcount; ++i)
            {
                buf.append("?,");
            }
            buf.setCharAt(buf.length()-1, ')');
            return buf.toString();
        }
        else
        {
            return "=?";
        }
    }

    private static String getNumber(int nNum, boolean bBold)
    {
        if (nNum < 0)
        {
            return "<HTML>" + (bBold ? "<B>" : "") +
                   PropertyConfig.getMessage("msg.chip.net.neg", -1 * nNum)+
                   (bBold ? "</B>" : "");
        }
        else
        {
            return "<HTML>" + (bBold ? "<B>" : "") +
                   PropertyConfig.getMessage("msg.chip.net.pos", nNum) +
                   (bBold ? "</B>" : "");
        }
    }

    private class SVLayout extends BorderLayout
    {
        Component extra;
        ScaleConstraintsFixed scf;

        @Override
        public void addLayoutComponent(Component comp, Object constraints)
        {
            if (constraints instanceof ScaleConstraintsFixed)
            {
                extra = comp;
                scf = (ScaleConstraintsFixed) constraints;
                return;
            }
            super.addLayoutComponent(comp, constraints);
        }

        @Override
        public void layoutContainer(Container target)
        {
            if (extra != null)
            {
                ScaleLayout.layoutScaleConstraintsFixed(scf, extra, target, target.getInsets());
            }
            super.layoutContainer(target);
        }
    }
}
