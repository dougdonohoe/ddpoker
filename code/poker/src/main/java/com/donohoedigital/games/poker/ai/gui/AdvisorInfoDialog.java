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
/*
 * GameInfoDialog.java
 *
 * Created on April 25, 2004, 6:48 PM
 */

package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GamePhase;
import com.donohoedigital.games.engine.DialogPhase;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.AIOutcome;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.ai.RuleEngine;
import com.donohoedigital.games.poker.ai.V2Player;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.Comparator;

import static com.donohoedigital.config.DebugConfig.TESTING;

public class AdvisorInfoDialog extends DialogPhase
{
    static Logger logger = LogManager.getLogger(AdvisorInfoDialog.class);
    
    // members
    private PokerGame game_;
    private TournamentProfile profile_;
    private DDTabbedPane tab_;
    private DDHtmlArea resultHTML_;

    JComponent ladder_ = null;

    private ImageComponent ic_ = new ImageComponent("ddlogo20", 1.0d);

    /**
     * Init phase, storing engine and gamephase.  Called createUI()
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        game_ = (PokerGame) context.getGame();
        profile_ = game_.getProfile();
        if (!game_.isClockMode()) profile_.setPrizePool(game_.getPrizePool(), true); // update to current
        ic_.setScaleToFit(false);
        ic_.setIconWidth(GamePrefsPanel.ICWIDTH);
        ic_.setIconHeight(GamePrefsPanel.ICHEIGHT); // need to be slightly higher for focus
        super.init(engine, context, gamephase);
    }

    public void finish()
    {
        super.finish();

        PokerTable table = game_.getCurrentTable();
        PokerPlayer player = table.getHoldemHand().getCurrentPlayer();

        table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_AI_CHANGED, table, player, player.getSeat()));
    }

    /**
     * Focus here
     */
    public Component getFocusComponent()
    {
        return tab_;
    }
    
    /**
     * create gui
     */
    public JComponent createDialogContents() 
    {
        resultHTML_ = new DDHtmlArea(GuiManager.DEFAULT, "AdvisorSummary");
        resultHTML_.setBorder(BorderFactory.createEmptyBorder());
        DDPanel resultPanel = (DDPanel)GuiUtils.CENTER(resultHTML_);
        resultPanel.setPreferredHeight(40);

        tab_ = new DDTabbedPane(STYLE, null, JTabbedPane.TOP);
        tab_.setOpaque(false);
        tab_.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        SummaryTab summaryTab = new SummaryTab();
        summaryTab.createUI();
        tab_.addTab(PropertyConfig.getMessage("msg.advisortab.summary"), ic_, summaryTab, null);

        StrategyTab strategyTab = new StrategyTab();
        strategyTab.createUI();
        tab_.addTab(PropertyConfig.getMessage("msg.advisortab.strategy"), ic_, strategyTab, null);

        if (TESTING(PokerConstants.TESTING_DEBUG_ADVISOR))
        {
            PlayersTab playersTab = new PlayersTab();
            playersTab.createUI();
            tab_.addTab(PropertyConfig.getMessage("msg.advisortab.players"), ic_, playersTab, null);

            DebugTab debugTab = new DebugTab();
            debugTab.createUI();
            tab_.addTab("Debug", ic_, debugTab, null); // keep label out of config on purpose
        }

        DDPanel base = new DDPanel();

        base.add(resultPanel, BorderLayout.NORTH);
        base.add(tab_, BorderLayout.CENTER);

        updateResult();

        return base;
    }

    private void updateResult()
    {
        HoldemHand hhand = game_.getCurrentTable().getHoldemHand();
        PokerPlayer p = hhand.getCurrentPlayer();
        V2Player ai = (V2Player)p.getPokerAI();
        RuleEngine re = ai.getRuleEngine();
        re.execute(ai);
        resultHTML_.setText("&nbsp;" + p.getHand().toHTML() + "&nbsp;&nbsp;" +
               hhand.getCommunity().toHTML() + "&nbsp;&nbsp;" + re.toHTML(p, true, false));
    }

    private class SummaryTab extends DDTabPanel
    {
        DDLabelBorder situationBorder_;
        DDLabelBorder factorsBorder_;

        DDHtmlArea situationHtmlArea_;
        DDHtmlArea factorsHtmlArea_;

        public void createUI()
        {
            situationHtmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, "OptionsDialog");
            factorsHtmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, "OptionsDialog");

            situationBorder_ = new DDLabelBorder("advisorsituation", "OptionsDialog");
            factorsBorder_ = new DDLabelBorder("advisorfactors", "OptionsDialog");

            situationHtmlArea_.setDisplayOnly(true);
            factorsHtmlArea_.setDisplayOnly(true);

            //situationHtmlArea_.setOpaque(true);
            //situationHtmlArea_.setBackground(Color.DARK_GRAY);

            factorsHtmlArea_.setOpaque(true);
            factorsHtmlArea_.setBackground(Color.BLACK);
            factorsHtmlArea_.setMargin(new Insets(4,4,4,4));

            situationHtmlArea_.setBorder(BorderFactory.createEmptyBorder());
            //factorsHtmlArea_.setBorder(BorderFactory.createEmptyBorder());

            DDScrollPane scroll;

            scroll = new DDScrollPane(situationHtmlArea_, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            scroll.setPreferredSize(new Dimension(600,100));

            situationBorder_.add(scroll);

            scroll = new DDScrollPane(factorsHtmlArea_, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            scroll.setPreferredSize(new Dimension(600,250));

            factorsBorder_.add(scroll);

            add(situationBorder_, BorderLayout.NORTH);
            add(factorsBorder_, BorderLayout.CENTER);

            repaint();
        }

        public void ancestorAdded(AncestorEvent event)
        {
            PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();

            V2Player ai = (V2Player)p.getPokerAI();
            ai.getRuleEngine().execute(ai);

            situationHtmlArea_.setText(ai.getSituationHTML());
            factorsHtmlArea_.setText(ai.getFactorsHTML());

            super.ancestorAdded(event);
        }
    }


    private class DebugTab extends DDTabPanel
    {
        DDHtmlArea htmlArea_;

        public void createUI()
        {
            htmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, "OptionsDialog");

            htmlArea_.setDisplayOnly(true);
            htmlArea_.setBorder(BorderFactory.createEmptyBorder(2,3,2,3));

            // scroll (use NORTH for correct sizing)
            DDScrollPane scroll = new DDScrollPane(htmlArea_, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            scroll.setPreferredSize(new Dimension(600,400));
            add(scroll, BorderLayout.CENTER);

            repaint();
        }

        public void ancestorAdded(AncestorEvent event)
        {
            PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();

            V2Player ai = (V2Player)p.getPokerAI();

            htmlArea_.setText(ai.getDebugText() + "<br>" + ai.getRuleEngine().getResultsTable());

            super.ancestorAdded(event);
        }
    }

    private class PlayersTab extends DDTabPanel
    {
        public void createUI()
        {
            PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();
            V2Player ai = (V2Player)p.getPokerAI();

            DDHtmlArea htmlArea = new DDHtmlArea(GuiManager.DEFAULT, "OptionsDialog");

            htmlArea.setDisplayOnly(true);
            htmlArea.setBorder(BorderFactory.createEmptyBorder(2,3,2,3));

            // scroll (use NORTH for correct sizing)
            DDScrollPane scroll = new DDScrollPane(htmlArea, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setOpaque(false);
            scroll.setPreferredSize(new Dimension(600,400));
            add(scroll, BorderLayout.CENTER);

            htmlArea.setText(ai.getPlayersInfoHTML());

            repaint();
        }
    }

    private class StrategyTab extends DDTabPanel implements ChangeListener
    {
        PlayerTypeSlidersPanel slidersPanel_;
        AdvisorGridPanel grid_ = null;
        DDProgressBar progressBar_;

        public void createUI()
        {
            PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();
            V2Player ai = (V2Player)p.getPokerAI();
            PlayerType playerType = ai.getPlayerType();
            HandSelectionPanel handSelectionPanel = new HandSelectionPanel(playerType, STYLE);
            slidersPanel_ = new PlayerTypeSlidersPanel(STYLE);
            slidersPanel_.setItems(playerType.getSummaryNodes(false));
            slidersPanel_.setPreferredHeight(320);
            add(handSelectionPanel, BorderLayout.NORTH);
            add(slidersPanel_, BorderLayout.CENTER);

            setPreferredWidth(700);

            grid_ = new AdvisorGridPanel();
            grid_.setPreferredSize(new Dimension(209,209));
            grid_.setEnabled(false);
            grid_.setBorder(BorderFactory.createEmptyBorder(16,4, 0, 4));
            DDLabelBorder gridBorder = new DDLabelBorder("handmatrix", STYLE);
            gridBorder.add(GuiUtils.CENTER(grid_), BorderLayout.CENTER);
            slidersPanel_.add(gridBorder, BorderLayout.EAST);

            grid_.setPreFlop(ai.getRound() == HoldemHand.ROUND_PRE_FLOP);

            progressBar_ = new DDProgressBar(GuiManager.DEFAULT, "PokerStats", false);
            //progressBar_.setBackground(Color.BLACK);
            progressBar_.setOpaque(true);

            grid_.addMouseMotionListener(new MouseMotionListener()
            {
                public void mouseDragged(MouseEvent e) {}
                public void mouseMoved(MouseEvent e) {

                    if (updateThread_ != null) return;  // for now, no mouseover while computing

                    PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();
                    String sOutcome = grid_.getOutcomeString(e);
                    if (sOutcome == null)
                    {
                        int outcome = grid_.getOutcome(e);
                        sOutcome = (outcome >= 0) ? RuleEngine.getOutcomeLabel(outcome) : null;
                    }
                    String sHand = grid_.getHand(e);
                    grid_.setToolTipText((sOutcome == null) ? sHand : sHand + " - " + sOutcome);
                    if (sOutcome != null)
                    {
                        slidersPanel_.getHelpPanel().setText(
                            PropertyConfig.getMessage("help.strat",
                                PropertyConfig.getMessage("msg.advisor.handmatrix"),
                                PropertyConfig.getMessage("msg.advisor.handmatrix.mouseover",
                                        sHand,
                                        Utils.encodeHTML(p.getPlayerType().getName()),
                                        sOutcome)));
                    }
                    else
                    {
                        slidersPanel_.getHelpPanel().setText("");
                        grid_.setToolTipText(null);
                    }
                }
            });

            grid_.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent e) {}

                public void mousePressed(MouseEvent e) {}

                public void mouseReleased(MouseEvent e) {}

                public void mouseEntered(MouseEvent e) {}

                public void mouseExited(MouseEvent e) {
                    slidersPanel_.getHelpPanel().setText("");
                }
            });

            repaint();
        }

        public void ancestorAdded(AncestorEvent event)
        {
            HandSelectionPanel.changeListener = this;
            PlayerTypeSlidersPanel.changeListener = this;

            if (grid_ != null) updateGrid(grid_, progressBar_);
        }

        public void ancestorRemoved(AncestorEvent event)
        {
            HandSelectionPanel.changeListener = null;
            PlayerTypeSlidersPanel.changeListener = null;

            if (updateThread_ != null)
            {
                updateThread_.abort = true;
                try { updateThread_.join(); } catch (InterruptedException e) {}
            }
        }

        public void stateChanged(ChangeEvent e)
        {
            if ((e != null) && (e.getSource() instanceof DDSlider))
            {
                if (((DDSlider)e.getSource()).getValueIsAdjusting()) return;
            }

            PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();
            V2Player ai = (V2Player)p.getPokerAI();
            PlayerType playerType = ai.getPlayerType();
            playerType.setName(PropertyConfig.getMessage("msg.advisor.profilename"));
            playerType.save();

            if (updateThread_ != null)
            {
                updateThread_.abort = true;
                try { updateThread_.join(); } catch (InterruptedException ie) {}
            }

            updateResult();

            if (grid_ != null) updateGrid(grid_, progressBar_);
        }
    }

    private UpdateThread updateThread_ = null;

    private class UpdateThread extends Thread
    {
        boolean abort = false;

        PokerGame game_;
        AdvisorGridPanel grid_;
        DDProgressBar progressBar_;
        JComponent progressPanel_;

        public UpdateThread(PokerGame game, AdvisorGridPanel grid, DDProgressBar progressBar)
        {
            super("HandMatrixUpdateThread");
            game_ = game;
            grid_ = grid;
            progressBar_ = progressBar;
        }

        public void run()
        {
            PokerPlayer p = game_.getCurrentTable().getHoldemHand().getCurrentPlayer();

            V2Player ai = (V2Player)p.getPokerAI();

            RuleEngine re = ai.getRuleEngine();

            Hand pocket = p.getHand();
            Hand backup = new Hand(pocket);

            if (ai.getRound() == HoldemHand.ROUND_PRE_FLOP)
            {
                boolean suited;

                for (int rank1 = Card.TWO; rank1 <= Card.ACE; ++rank1)
                {
                    for (int rank2 = Card.TWO; rank2 <= Card.ACE; ++rank2)
                    {
                        suited = (rank1 > rank2);
                        pocket.clear();
                        pocket.addCard(Card.getCard(CardSuit.CLUBS, rank1));
                        pocket.addCard(Card.getCard(suited ? CardSuit.CLUBS : CardSuit.DIAMONDS, rank2));
                        //ai.computeOdds();
                        re.execute(ai);
                        AIOutcome outcome = re.getOutcome();
                        String outcomeString = (outcome == null) ? null : outcome.toHTML(2);
                        grid_.setOutcome(rank1, rank2, suited, re.getStrongestOutcome(), outcomeString);
                    }
                }

                GuiUtils.invoke(new Runnable()
                {
                    public void run()
                    {
                        grid_.repaint();
                    }
                });
            }
            else
            {
                grid_.clear();

                GuiUtils.invoke(new Runnable()
                {
                    public void run()
                    {
                        grid_.repaint();
                    }
                });

                progressBar_.setPercentDone(0);

                GuiUtils.invoke(new Runnable()
                {
                    public void run()
                    {
                        progressPanel_ = GuiUtils.CENTER(progressBar_);
                        grid_.add(progressPanel_, BorderLayout.CENTER);
                    }
                });

                Hand community = ai.getCommunity();

                Deck deck = new Deck(false);
                deck.removeCards(community);
                Collections.sort(deck, new Comparator()
                {
                    public int compare(Object o1, Object o2) {
                        return ((Card)o2).getRank() * 4 - ((Card)o2).getSuit() - ((Card)o1).getRank() * 4 + ((Card)o1).getSuit();
                    }
                    public boolean equals(Object obj) { return compare(this, obj) == 0; }
                });

                pocket.clear();
                pocket.addCard(Card.BLANK);
                pocket.addCard(Card.BLANK);

                int suitEquivalenceValues[] = new int[13*13];

                int suitCount = community.getNumSuits();

                boolean suitEquivalence;
                int suitEquivalenceIndex = -1;

                //RuleEngine.matrix = true;
                //ai.noPotential = true;

                Card card1;
                Card card2;
                int outcome;

                int total = deck.size() * (deck.size()-1) / 2;
                int count = 0;

                int deckSize = deck.size();

                long before = System.currentTimeMillis();

                for (int i = 0; i < deckSize; ++i)
                {
                    card1 = deck.getCard(i);
                    pocket.setCard(0, card1);

                    for (int j = i+1; j < deckSize && !abort; ++j)
                    {
                        card2 = deck.getCard(j);
                        pocket.setCard(1, card2);

                        if (pocket.isSuited())
                        {
                            suitEquivalence = (suitCount == 4) ||
                                    ((suitCount == 3) && !community.containsSuit(card1.getSuit()));
                        }
                        else
                        {
                            // TODO: add cases where not enough cards in one of my suits
                            suitEquivalence = (suitCount > 2) ||
                                    ((suitCount == 2) &&
                                     !community.containsSuit(card1.getSuit()) &&
                                     !community.containsSuit(card2.getSuit()));
                        }

                        outcome = -1;

                        if (suitEquivalence)
                        {
                            suitEquivalenceIndex = (card1.getRank() - Card.TWO)*13 + card2.getRank() - Card.TWO;
                            outcome = suitEquivalenceValues[suitEquivalenceIndex] - 1;
                        }

                        if (outcome < 0)
                        {
                            //System.out.println(pocket.toString() + " - computing");

                            //ai.computeOdds();
                            re.execute(ai);
                            outcome = re.getStrongestOutcome();

                            if (suitEquivalence)
                            {
                                suitEquivalenceValues[suitEquivalenceIndex] = outcome + 1;
                            }
                        }
                        else
                        {
                            //System.out.println(pocket.toString() + " - skipping");
                        }

                        grid_.setOutcome(pocket, outcome);

                        ++count;

                        GuiUtils.invoke(new Runnable()
                        {
                            public void run()
                            {
                                grid_.repaint(500);
                            }
                        });

                        progressBar_.setPercentDone((count*100)/total);
                    }
                }

                long after = System.currentTimeMillis();

                //System.out.println("Elapsed time: " + ((after - before) / 1000) + " seconds.");

                //ai.noPotential = false;
                //RuleEngine.matrix = false;

                GuiUtils.invoke(new Runnable()
                {
                    public void run()
                    {
                        grid_.remove(progressPanel_);
                        grid_.repaint();
                    }
                });
            }

            pocket.clear();
            pocket.addAll(backup);
            ai.computeOdds();
            re.execute(ai);

            updateThread_ = null;
        }
    }

    private void updateGrid(AdvisorGridPanel grid, DDProgressBar progressBar)
    {
        updateThread_ = new UpdateThread(game_, grid, progressBar);
        updateThread_.setPriority(Thread.MIN_PRIORITY);
        updateThread_.start();
    }
}
