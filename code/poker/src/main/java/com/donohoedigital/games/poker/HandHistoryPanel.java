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
import com.donohoedigital.db.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class HandHistoryPanel extends DDPanel
{

    private String where_;
    private BindArray bindArray_;
    private HoldemHand currentHand_;
    private int pageSize_;

    private int handCount_;
    private int handFirst_;

    private int handID_;
    private HoldemHand hhand_;

    private List<Object> hands_;

    private GameContext context_;
    private DDTabbedPane tabs_;
    private DDHtmlArea detailsHtmlArea_;
    private DDHtmlArea summaryHtmlArea_;
    private DDCheckBox showAllCheckbox_;
    private DDCheckBox showReasonCheckbox_;
    private ListPanel handsList_;
    private JScrollPane summaryScroll_;
    private DDLabel titleLabel_;
    private DDLabel pagingLabel_;
    private DDButton pageDownButton_;
    private DDButton pageUpButton_;
    private DDButton exportButton_;
    private ImageIcon icon_ = ImageConfig.getImageIcon("ddlogo20");

    public HandHistoryPanel(GameContext context, String sStyle, String where, BindArray bindArray, HoldemHand currentHand, int pageSize)
    {
        context_ = context;
        pageSize_ = pageSize;

        where_ = where;
        bindArray_ = bindArray;
        currentHand_ = currentHand;

        /*
        if (true)
        {
            where_ += " AND 0.5 < (SELECT MAX(ABS(PLH_END_CHIPS - PLH_START_CHIPS) / PLH_START_CHIPS) FROM PLAYER_HAND WHERE PLH_HAND_ID=HND_ID)";
        }
        */

        if (currentHand_ != null)
        {
            where_ += " AND HND_START_DATE < ?";
            bindArray_.addValue(Types.TIMESTAMP, new Timestamp(currentHand_.getStartDate()));
        }

        handCount_ = PokerDatabase.getHandCount(where_, bindArray_);

        if (currentHand_ != null)
        {
            ++handCount_;
        }

        handFirst_ = Math.max(handCount_ - pageSize_, 0);

        // widgets
        detailsHtmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);
        detailsHtmlArea_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        summaryHtmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);
        summaryHtmlArea_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        handsList_ = new ListPanel(HandListItemPanel.class, sStyle);
        handsList_.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                setHandIndex(handsList_.getSelectedIndex());
            }
        });
        handsList_.setOpaque(false);
        handsList_.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        Insets insets = handsList_.getInsets();
        handsList_.setPreferredSize(new Dimension(176, 30 * pageSize_ + insets.top + insets.bottom));

        DDPanel westPanel = new DDPanel(GuiManager.DEFAULT, sStyle);

        DDPanel handsHeader = new DDPanel(GuiManager.DEFAULT, sStyle);
        DDLabel handLabel = new DDLabel("histpocket", sStyle);
        DDLabel boardLabel = new DDLabel("histcommunity", sStyle);
        handLabel.setHorizontalAlignment(DDLabel.CENTER);
        handLabel.setPreferredWidth(55);
        boardLabel.setHorizontalAlignment(DDLabel.CENTER);
        boardLabel.setPreferredWidth(100);
        handsHeader.add(handLabel, BorderLayout.WEST);
        handsHeader.add(GuiUtils.WEST(boardLabel), BorderLayout.CENTER);
        westPanel.add(handsHeader, BorderLayout.NORTH);

        DDPanel pagingPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        pagingPanel.setBorderLayoutGap(5, 0);
        DDPanel pagingButtonPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        pagingLabel_ = new DDLabel(GuiManager.DEFAULT, sStyle);
        pagingLabel_.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        pageDownButton_ = new GlassButton("pagedown", "Glass");
        pageDownButton_.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handFirst_ -= pageSize_;
                        setHands();
                    }
                }
        );
        pageUpButton_ = new GlassButton("pageup", "Glass");
        pageUpButton_.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        handFirst_ += pageSize_;
                        setHands();
                    }
                }
        );

        exportButton_ = new GlassButton("export", "Glass");
        exportButton_.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        export();
                    }
                }
        );

        pagingButtonPanel.setLayout(new GridLayout(1, 3, 8, 0));
        pagingButtonPanel.add(pageUpButton_);
        pagingButtonPanel.add(exportButton_);
        pagingButtonPanel.add(pageDownButton_);

        pagingPanel.add(pagingLabel_, BorderLayout.NORTH);
        pagingPanel.add(GuiUtils.CENTER(pagingButtonPanel), BorderLayout.CENTER);

        DDPanel hsPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        hsPanel.add(handsList_, BorderLayout.CENTER);
        hsPanel.setBorder(BorderFactory.createEtchedBorder());
        westPanel.add(hsPanel, BorderLayout.CENTER);
        westPanel.add(pagingPanel, BorderLayout.SOUTH);

        // description
        // scroll (use NORTH for correct sizing)
        JScrollPane detailsScroll = new DDScrollPane(detailsHtmlArea_, sStyle, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailsScroll.setOpaque(false);
        detailsScroll.setPreferredSize(new Dimension(450, 400));
        detailsScroll.setViewportBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        detailsScroll.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        summaryScroll_ = new DDScrollPane(summaryHtmlArea_, sStyle, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        summaryScroll_.setOpaque(false);
        summaryScroll_.setPreferredSize(new Dimension(450, 400));
        summaryScroll_.setViewportBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        summaryScroll_.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        titleLabel_ = new DDLabel();

        DDPanel optionPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        optionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                                 BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        showAllCheckbox_ = new DDCheckBox("handhistaifaceup", sStyle);
        showAllCheckbox_.setEnabled(false);

        showReasonCheckbox_ = new DDCheckBox("handhistaireason", sStyle);
        showReasonCheckbox_.setEnabled(false);

        DDPanel showAllPanel = new DDPanel(GuiManager.DEFAULT, sStyle);

        showAllPanel.setLayout(new GridLayout(1, 2));

        showAllPanel.add(showAllCheckbox_);
        showAllPanel.add(showReasonCheckbox_);

        optionPanel.add(showAllPanel, BorderLayout.CENTER);

        showAllCheckbox_.setSelected(PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_AIFACEUP));

        showAllCheckbox_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                showReasonCheckbox_.setEnabled(showAllCheckbox_.isSelected());
                setHistoryText();
            }
        });
        showReasonCheckbox_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setHistoryText();
            }
        });

        tabs_ = new DDTabbedPane(sStyle, null, DDTabbedPane.TOP);

        tabs_.addTab(PropertyConfig.getMessage("msg.histdetails"), icon_, detailsScroll);

        DDPanel eastPanel = new DDPanel(GuiManager.DEFAULT, sStyle);

        eastPanel.add(titleLabel_, BorderLayout.NORTH);
        eastPanel.add(tabs_, BorderLayout.CENTER);
        eastPanel.add(optionPanel, BorderLayout.SOUTH);
        eastPanel.setBorderLayoutGap(4, 0);

        add(westPanel, BorderLayout.WEST);
        add(eastPanel, BorderLayout.CENTER);

        setBorderLayoutGap(0, 8);

        setHands();
    }

    private void checkButtons()
    {
        pageDownButton_.setEnabled(handFirst_ > 0);
        pageUpButton_.setEnabled(handFirst_ < handCount_ - pageSize_);
        exportButton_.setEnabled(handID_ >= 0 && handCount_ > 0);
    }

    private void setHands()
    {
        List<Integer> hands = PokerDatabase.getHandIDs(where_, bindArray_, handFirst_, pageSize_);
        hands_ = new ArrayList<Object>(hands.size());
        for (int i = hands.size() - 1; i >= 0; --i)
        {
            hands_.add(hands.get(i));
        }

        if ((currentHand_ != null) && (handFirst_ + pageSize_ >= handCount_))
        {
            hands_.add(0, currentHand_);
        }

        handsList_.setItems(hands_);

        if (hands_.size() == 0)
        {
            titleLabel_.setText(PropertyConfig.getMessage("msg.nohistory"));
            pagingLabel_.setText("");
            detailsHtmlArea_.setText("");
            summaryHtmlArea_.setText("");
            pageUpButton_.setEnabled(false);
            pageDownButton_.setEnabled(false);
            exportButton_.setEnabled(false);
        }
        else
        {
            handsList_.setSelectedIndex(0);
            pagingLabel_.setText(PropertyConfig.getMessage
                    ("msg.histpage", Math.max(handFirst_, 0) + 1,
                     Math.min(handFirst_ + pageSize_, handCount_),
                     handCount_));
            setHandIndex(handsList_.getSelectedIndex());
        }
    }

    private void setHistoryText()
    {
        if (handID_ < 0)
        {
            titleLabel_.setText(PropertyConfig.getMessage("msg.currenthand"));
            detailsHtmlArea_.setText(getCurrentHistoryText());

            if (tabs_.getTabCount() > 1)
            {
                tabs_.removeTabAt(1);
            }
        }
        else
        {
            String[] handHTML = PokerDatabase.getHandAsHTML(handID_, showAll(), showReason());
            titleLabel_.setText(handHTML[0]);
            summaryHtmlArea_.setText(handHTML[1]);
            detailsHtmlArea_.setText(handHTML[2]);

            summaryHtmlArea_.setCaretPosition(0); // scroll to top

            if (tabs_.getTabCount() < 2)
            {
                tabs_.addTab(PropertyConfig.getMessage("msg.histsummary"), icon_, summaryScroll_);
            }
        }

        detailsHtmlArea_.setCaretPosition(0); // scroll to top

        detailsHtmlArea_.repaint();
    }

    private String getCurrentHistoryText()
    {
        List<HandAction> hist = hhand_.getHistoryCopy();
        StringBuilder sb = new StringBuilder();

        if (hhand_.getAnte() > 0)
        {
            sb.append(getHist(hist, HoldemHand.ROUND_PRE_FLOP, hhand_, true));
        }
        sb.append(getHist(hist, HoldemHand.ROUND_PRE_FLOP, hhand_, false));
        sb.append(getHist(hist, HoldemHand.ROUND_FLOP, hhand_, false));
        sb.append(getHist(hist, HoldemHand.ROUND_TURN, hhand_, false));
        sb.append(getHist(hist, HoldemHand.ROUND_RIVER, hhand_, false));

        // I don't believe this will ever happen as it is now, but perhaps in the future
        // when not every hand is stored in the database, or more precisely when you can
        // view history for hands that aren't stored, for whatever reason.
        if (hhand_.getRound() == HoldemHand.ROUND_SHOWDOWN)
        {
            PokerDatabase.appendShowdown(sb, hhand_.getHistoryCopy(), hhand_.getCommunity(), showAll());
        }

        return sb.toString();
    }

    private String getHist(List<HandAction> hist, int nRound, HoldemHand hhand, boolean bAnte)
    {
        StringBuilder sb = new StringBuilder();
        PokerPlayer p;
        int nNum = 0;
        int nPrior = 0;

        HandInfoFast info = new HandInfoFast();

        Hand community = hhand.getCommunity();

        if (nRound == HoldemHand.ROUND_PRE_FLOP)
        {
            community = new Hand();
        }
        else if ((nRound == HoldemHand.ROUND_FLOP) && (community.size() > 3))
        {
            community = new Hand(community.getCard(0), community.getCard(1), community.getCard(2));
        }
        else if ((nRound == HoldemHand.ROUND_TURN) && (community.size() > 4))
        {
            community = new Hand(community.getCard(0), community.getCard(1), community.getCard(2), community.getCard(3));
        }

        for (HandAction action : hist)
        {
            p = action.getPlayer();

            // must be from this round
            if (action.getRound() != nRound || (!bAnte && action.getAction() == HandAction.ACTION_ANTE)) continue;
            if (bAnte && action.getAction() != HandAction.ACTION_ANTE) continue;

            Hand hand = p.getHand();

            String handHTML;
            String handShown = "";
            String sReason = showReason() ? PokerDatabase.decodeReason(action.getDebug()) : null;

            if (sReason == null) sReason = "";
            else sReason = " " + PropertyConfig.getMessage("msg.hist.reason", sReason);

            if (p.isCardsExposed() || (p.isHuman() && p.isLocallyControlled()) || showAll())
            {
                handHTML = hand.toHTML();

                if (community.size() > 0)
                {
                    info.getScore(hand, community);
                    handShown = "&nbsp;-&nbsp;" + info.toString(", ", false);
                }
            }
            else
            {
                handHTML = "<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">";
            }

            String sSnippet = action.getHTMLSnippet("msg.handhist", nPrior, null);

            // get right raise icon
            if (action.getAction() == HandAction.ACTION_RAISE)
            {
                nPrior++;
            }

            // count actions added
            nNum++;

            // append message
            sb.append(PropertyConfig.getMessage("msg.hist.x", Utils.encodeHTML(p.getName()), sSnippet,
                                                handHTML, handShown, sReason));
            sb.append("\n");
        }

        if (nNum == 0) return "";

        // if doing antes, change round (match client.properties)
        if (bAnte) nRound = 9;

        return PropertyConfig.getMessage("msg.hand.history",
                                         PropertyConfig.getMessage("msg.round." + nRound),
                                         sb.toString(), community.toHTML());
    }

    private void setHandIndex(int index)
    {

        Object o = hands_.get(index);

        if (o instanceof Integer)
        {
            if (handID_ == (Integer) o) return;
            handID_ = (Integer) o;
            hhand_ = null;
            showAllCheckbox_.setEnabled(PokerDatabase.isPracticeHand(handID_));
        }
        else
        {
            if (hhand_ == o) return;
            handID_ = -1;
            hhand_ = (HoldemHand) o;
            showAllCheckbox_.setEnabled(!hhand_.getTable().getGame().isOnlineGame());
        }

        checkButtons();

        showReasonCheckbox_.setEnabled(showAll());

        setHistoryText();
    }

    private boolean showAll()
    {
        return showAllCheckbox_.isEnabled() && showAllCheckbox_.isSelected();
    }

    private boolean showReason()
    {
        return showAll() && showReasonCheckbox_.isEnabled() && showReasonCheckbox_.isSelected();
    }

    @SuppressWarnings({"PublicInnerClass"})
    public static class HandListItemPanel extends ListItemPanel
    {
        private DDHtmlArea display_;

        public HandListItemPanel(ListPanel panel, Object item, String sStyle)
        {
            super(panel, item, sStyle);

            display_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);
            display_.setBorder(BorderFactory.createEmptyBorder());

            add(display_, BorderLayout.CENTER);
        }

        @Override
        public void update()
        {
            Object o = getItem();

            if (o instanceof Integer)
            {
                display_.setText("<html><body>" + PokerDatabase.getHandListHTML((Integer) getItem()) + "</body></html>");
            }
            else
            {
                display_.setText("<html><body>" + ((HoldemHand) o).getHandListHTML() + "</body></html>");
            }
        }
    }

    private void export()
    {
        TypedHashMap params = new TypedHashMap();
        params.setObject(HistoryExportDialog.PARAM_HAND_ID, handID_);
        context_.processPhaseNow("HistoryExportDialog", params);
    }
}
