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
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 8, 2005
 * Time: 7:24:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TournamentSummaryPanel extends DDPanel
{
    private String STYLE;
    private String BEVEL_STYLE;
    private GameContext context_;
    private DDHtmlArea html_, onlineHtml_;
    private DDLabel double_;
    private DDLabel payinfo_;
    private TournamentModel payout_;
    private TournamentModel levels_;
    private TournamentModel opponents_;
    private DDTabbedPane tab_;
    private String sHelpName_;
    private TournamentProfile profile_;
    private TournamentProfileHtml profileHtml_;
    private ImageComponent ic_ = new ImageComponent("ddlogo20", 1.0d);
    private boolean bListMode_;


    public TournamentSummaryPanel(GameContext context, String sStyle, String sTabStyle, String sTableBevelStyle,
                                  String sHelpName, double dScale,
                                  boolean bShowOppTab, boolean bListMode)
    {
        // init
        context_ = context;
        STYLE = sStyle;
        BEVEL_STYLE = sTableBevelStyle;
        sHelpName_ = sHelpName;
        bListMode_ = bListMode;
        tab_ = new DDTabbedPane(STYLE, sTabStyle, DDTabbedPane.TOP);
        tab_.setOpaque(false);
        tab_.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        add(tab_, BorderLayout.CENTER);

        for (int i = 0; i < LEVELS_WIDTHS.length; i++)
        {
            LEVELS_WIDTHS[i] *= dScale;
        }
        for (int i = 0; i < PAYOUT_WIDTHS.length; i++)
        {
            PAYOUT_WIDTHS[i] *= dScale;
        }
        for (int i = 0; i < OPP_WIDTHS.length; i++)
        {
            OPP_WIDTHS[i] *= dScale;
        }

        SummaryPanel sumtab = new SummaryPanel();
        sumtab.createUI();
        tab_.addTab(PropertyConfig.getMessage("msg.tournsummary"), ic_, sumtab, null);
        tab_.addTab(PropertyConfig.getMessage("msg.levelssummary"), ic_, new LevelsTab(), null);
        tab_.addTab(PropertyConfig.getMessage("msg.payouts"), ic_, new PayoutTab(), null);

        if (bShowOppTab)
        {
            tab_.addTab(PropertyConfig.getMessage("msg.oppmix"), ic_, new OpponentTab(), null);
        }

        tab_.addTab(PropertyConfig.getMessage("msg.online"), ic_, new OnlineTab(), null);
    }

    private class SummaryPanel extends DDTabPanel
    {
        @Override
        public void createUI()
        {
            // html summary
            html_ = new DDHtmlArea(sHelpName_, STYLE);
            html_.setDisplayOnly(true);
            html_.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
            JScrollPane scroll = new DDScrollPane(html_, STYLE, null,
                                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            add(scroll, BorderLayout.CENTER);
            setSummaryText();
        }
    }

    private class OnlineTab extends DDTabPanel
    {
        @Override
        public void createUI()
        {
            // html summary
            onlineHtml_ = new DDHtmlArea(sHelpName_, STYLE);
            onlineHtml_.setDisplayOnly(true);
            onlineHtml_.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
            JScrollPane scroll = new DDScrollPane(onlineHtml_, STYLE, null,
                                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            add(scroll, BorderLayout.CENTER);
            setOnlineText();
        }
    }

    private class LevelsTab extends DDTabPanel
    {
        @Override
        public void createUI()
        {
            setBorderLayoutGap(10, 0);
            DDScrollTable scrollLevels = new DDScrollTable(GuiManager.DEFAULT,
                                                           STYLE, BEVEL_STYLE,
                                                           LEVELS_NAMES, LEVELS_WIDTHS);
            scrollLevels.setPreferredSize(new Dimension(scrollLevels.getPreferredWidth(), 10));
            add(GuiUtils.WEST(scrollLevels), BorderLayout.CENTER);

            DDTable table = scrollLevels.getDDTable();
            levels_ = new TournamentModel(context_, profileHtml_, LEVELS_NAMES, LEVELS_WIDTHS);
            table.setModel(levels_);
            table.setExporter(new TableExporter(context_, "levels"));
            table.setShowHorizontalLines(true);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setAlign(1, SwingConstants.RIGHT);
            table.setAlign(2, SwingConstants.RIGHT);
            table.setAlign(3, SwingConstants.RIGHT);
            table.setAlign(4, SwingConstants.RIGHT);

            double_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            add(double_, BorderLayout.SOUTH);
            setDoubleText();
        }
    }

    private class PayoutTab extends DDTabPanel
    {
        @Override
        public void createUI()
        {
            setBorderLayoutGap(10, 0);
            DDScrollTable scrollPayouts = new DDScrollTable(GuiManager.DEFAULT,
                                                            STYLE, BEVEL_STYLE,
                                                            PAYOUT_NAMES, PAYOUT_WIDTHS);
            scrollPayouts.setPreferredSize(new Dimension(scrollPayouts.getPreferredWidth(), 10));
            add(GuiUtils.WEST(scrollPayouts), BorderLayout.CENTER);

            DDTable table = scrollPayouts.getDDTable();
            payout_ = new TournamentModel(context_, profileHtml_, PAYOUT_NAMES, PAYOUT_WIDTHS);
            table.setModel(payout_);
            table.setExporter(new TableExporter(context_, "payouts"));
            table.setShowHorizontalLines(true);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setAlign(1, SwingConstants.RIGHT);

            payinfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            add(payinfo_, BorderLayout.NORTH);
            setPayinfoText();
        }
    }

    private class OpponentTab extends DDTabPanel
    {
        @Override
        public void createUI()
        {
            DDScrollTable scrollOpp = new DDScrollTable(GuiManager.DEFAULT,
                                                        STYLE, BEVEL_STYLE,
                                                        OPP_NAMES, OPP_WIDTHS);
            scrollOpp.setPreferredSize(new Dimension(scrollOpp.getPreferredWidth(), 10));
            add(GuiUtils.WEST(scrollOpp), BorderLayout.CENTER);

            DDTable table = scrollOpp.getDDTable();
            opponents_ = new TournamentModel(context_, profileHtml_, OPP_NAMES, OPP_WIDTHS);
            table.setModel(opponents_);
            table.setExporter(new TableExporter(context_, "opponents"));
            table.setShowHorizontalLines(true);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setAlign(1, SwingConstants.RIGHT);
        }
    }

    public void updateProfile(TournamentProfile profile)
    {
        profile_ = profile;
        profileHtml_ = profile_ == null ? null : new TournamentProfileHtml(profile_);
        setSummaryText();
        if (payout_ != null) payout_.updateProfile(profileHtml_);
        if (levels_ != null) levels_.updateProfile(profileHtml_);
        if (opponents_ != null) opponents_.updateProfile(profileHtml_);
        setDoubleText();
        setPayinfoText();
        setOnlineText();
    }

    public void updateEmptyProfile(String sMsg)
    {
        updateProfile(null);
        html_.setText(sMsg);
        tab_.setSelectedIndex(0);
    }

    private void setSummaryText()
    {
        if (html_ == null) return;
        if (profileHtml_ == null)
        {
            html_.setText("");
        }
        else
        {
            html_.setText(profileHtml_.toHTMLSummary(bListMode_, GameEngine.getGameEngine().getLocale()));
        }
    }

    private void setOnlineText()
    {
        if (onlineHtml_ == null) return;
        if (profileHtml_ == null)
        {
            onlineHtml_.setText("");
        }
        else
        {
            onlineHtml_.setText(profileHtml_.toHTMLOnline());
        }
    }

    private void setDoubleText()
    {
        if (double_ == null) return;
        if (profile_ == null)
        {
            double_.setText("");
        }
        else
        {
            int nLevel = profile_.getLastLevel() + 1;
            double_.setText(
                    PropertyConfig.getMessage(profile_.isDoubleAfterLastLevel() ?
                                              "msg.double2.true" : "msg.double2.false",
                                              nLevel,
                                              nLevel - 1));
        }
    }

    private void setPayinfoText()
    {
        if (payinfo_ == null) return;
        if (profile_ == null)
        {
            payinfo_.setText("");
        }
        else
        {
            PokerGame game = (PokerGame) context_.getGame();
            int nGrossPool, nHouseTake, nNetPool;
            if (game == null)
            {
                nGrossPool = profile_.getNumPlayers() * profile_.getBuyinCost();
            }
            else
            {
                if (game.isClockMode())
                {
                    nGrossPool = game.getClockCash();
                    if (nGrossPool == 0)
                    {
                        nGrossPool = profile_.getNumPlayers() * profile_.getBuyinCost();
                    }
                }
                else
                {
                    nGrossPool = game.getPrizePool();
                }
            }
            nNetPool = profile_.getPoolAfterHouseTake(nGrossPool);
            nHouseTake = nGrossPool - nNetPool;
            String sType;
            if (profile_.isAllocSatellite())
            {
                sType = PropertyConfig.getMessage("msg.pay.satellite");
            }
            else if (profile_.isAllocPercent())
            {
                sType = PropertyConfig.getMessage("msg.pay.percent");
            }
            else if (profile_.isAllocFixed())
            {
                sType = PropertyConfig.getMessage("msg.pay.fixed");
            }
            else //if (profile.isAllocAuto())
            {
                sType = PropertyConfig.getMessage("msg.pay.auto");
            }

            String sHouse;
            int nHousePercent = profile_.getHousePercent();
            int nHouseAmount = profile_.getHouseAmount();
            int nType = profile_.getHouseCutType();

            if (nType == PokerConstants.HOUSE_PERC && nHousePercent > 0)
            {
                sHouse = PropertyConfig.getMessage("msg.house.percent", nHousePercent);
            }
            else if (nType == PokerConstants.HOUSE_AMOUNT && nHouseAmount > 0)
            {
                sHouse = PropertyConfig.getMessage("msg.house.amount", nHouseAmount);
            }
            else
            {
                sHouse = PropertyConfig.getMessage("msg.house.none");
            }

            String sMay = "";
            if (profile_.isRebuys() || profile_.isAddons())
            {
                sMay = PropertyConfig.getMessage("msg.pool.mayincrease2");
            }

            payinfo_.setText(
                    PropertyConfig.getMessage("msg.payinfo",
                                              nGrossPool,
                                              nHouseTake,
                                              nNetPool,
                                              sType,
                                              sHouse,
                                              sMay

                    ));
        }
    }

    // columns
    public static final String COL_ANTE = "ante";
    public static final String COL_SMALL = "small";
    public static final String COL_BIG = "big";
    public static final String COL_TIME = "time";
    public static final String COL_GAMETYPE = "gametype";
    public static final String COL_PLACE = "place";
    public static final String COL_PAYOUT = "payout";
    public static final String COL_NUM = "#";
    public static final String COL_OPPONENT_TYPE = "opponent";
    public static final String COL_PERC = "percent";

    static int RW = 60; // rank width
    static int CW = 88; // chip width

    private static final String[] PAYOUT_NAMES = new String[]{
            COL_PLACE, COL_PAYOUT
    };
    // client table info
    private int[] PAYOUT_WIDTHS = new int[]{
            55, 275
    };
    private static String[] LEVELS_NAMES = new String[]{
            COL_NUM, COL_ANTE, COL_SMALL, COL_BIG, COL_TIME, COL_GAMETYPE
    };
    // client table info
    private int[] LEVELS_WIDTHS = new int[]{
            35, CW, CW, CW, 35, 70
    };
    private static String[] OPP_NAMES = new String[]{
            COL_OPPONENT_TYPE, COL_PERC
    };
    // client table info
    private int[] OPP_WIDTHS = new int[]{
            250, 80
    };

    /**
     * Used by table to display players in game
     */
    static class TournamentModel extends DefaultTableModel
    {
        GameContext context;
        List<PokerPlayer> rank;
        TournamentProfile profile;
        TournamentProfileHtml html;
        List<BaseProfile> playerTypes;
        String[] names;
        int[] widths;
        boolean bPayout;
        boolean bOppMix;

        TournamentModel(GameContext context, TournamentProfileHtml profileHtml, String names[], int[] widths)
        {
            this.context = context;
            this.names = names;
            this.widths = widths;
            bPayout = names == PAYOUT_NAMES;
            bOppMix = names == OPP_NAMES;
            updateProfile(profileHtml);
        }

        public void updateProfile(TournamentProfileHtml h)
        {
            if (h == null) return;
            profile = h.getProfile();
            html = h;
            if (bOppMix && profile != null)
            {
                playerTypes = new ArrayList<BaseProfile>();
                List<BaseProfile> types = PlayerType.getProfileListCached();

                for (BaseProfile profile1 : types)
                {
                    PlayerType type = (PlayerType) profile1;

                    int percent = profile.getPlayerTypePercent(type.getUniqueKey());

                    if (percent > 0)
                    {
                        playerTypes.add(type);
                    }
                }
            }
            PokerGame game = (PokerGame) context.getGame();
            if (game != null) rank = game.getPlayersByRank();
            else rank = null;
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int c)
        {
            return names[c];
        }

        @Override
        public int getColumnCount()
        {
            return widths.length;
        }

        @Override
        public boolean isCellEditable(int r, int c)
        {
            return false;
        }

        @Override
        public int getRowCount()
        {
            if (profile == null) return 0;

            if (bPayout) return profile.getNumSpots();
            else if (bOppMix) return playerTypes.size();
            else return profile.getLastLevel();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex)
        {
            String sValue = "[bad column]";
            int idx = rowIndex + 1;

            if (names[colIndex].equals(COL_NUM))
            {
                sValue = "" + idx;
            }
            else if (names[colIndex].equals(COL_ANTE))
            {
                if (profile.isBreak(idx)) return PropertyConfig.getMessage("msg.break");
                else sValue = getNumber(profile.getAnte(idx));
            }
            else if (names[colIndex].equals(COL_SMALL))
            {
                if (profile.isBreak(idx)) sValue = "";
                else sValue = getNumber(profile.getSmallBlind(idx));
            }
            else if (names[colIndex].equals(COL_BIG))
            {
                if (profile.isBreak(idx)) sValue = "";
                else sValue = getNumber(profile.getBigBlind(idx));
            }
            else if (names[colIndex].equals(COL_TIME))
            {
                int nTime = profile.getMinutes(idx);
                if (nTime != profile.getDefaultMinutesPerLevel())
                {
                    sValue = "" + nTime;
                }
                else
                {
                    sValue = "";
                }
            }
            else if (names[colIndex].equals(COL_GAMETYPE))
            {
                //if (profile.isBreak(idx)) sValue = "";
                sValue = ' ' + profile.getGameTypeDisplay(idx);
            }
            else if (names[colIndex].equals(COL_PLACE))
            {
                sValue = PropertyConfig.getPlace(idx);
            }
            else if (names[colIndex].equals(COL_PAYOUT))
            {
                boolean bSet = false;
                // if we have a rank list, display actual prize paid
                if (rank != null && rowIndex < rank.size())
                {
                    PokerPlayer at = rank.get(rowIndex);
                    if (at.getPrize() > 0)
                    {
                        PokerGame game = (PokerGame) context.getGame();
                        sValue = PropertyConfig.getMessage("msg.spot.paid",
                                                           Utils.encodeHTML(at.getDisplayName(game.isOnlineGame())),
                                                           at.getPrize());
                        bSet = true;
                    }
                }
                if (!bSet)
                {
                    sValue = html.getSpotHTML(idx, true, "2");
                }
            }
            else if (names[colIndex].equals(COL_PERC))
            {
                PlayerType type = (PlayerType) playerTypes.get(rowIndex);
                sValue = profile.getPlayerTypePercent(type.getUniqueKey()) + "%";
            }
            else if (names[colIndex].equals(COL_OPPONENT_TYPE))
            {
                PlayerType type = (PlayerType) playerTypes.get(rowIndex);
                sValue = type.getName();
            }

            return sValue;
        }

        private String getNumber(int nNum)
        {
            if (nNum == 0) return "";
            return PropertyConfig.getMessage("msg.chip.net.pos", nNum);
        }
    }
}
