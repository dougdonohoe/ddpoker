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
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 21, 2005
 * Time: 7:40:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimulatorDialog extends BasePhase implements ChangeListener
{
    static Logger logger = Logger.getLogger(SimulatorDialog.class);

    private static ImageIcon blankIcon_ = ImageConfig.getImageIcon("menuicon.blank");
    private static ImageIcon checkedIcon_ = ImageConfig.getImageIcon("menuicon.checked");

    static int MENU_CLEAR_ALL = 0;
    static int MENU_CLEAR_OPP = 1;
    static int MENU_CLEAR_BOARD = 2;
    static int MENU_LOAD_ALL = 3;
    static int MENU_LOAD_MY = 4;
    static int MENU_CHANGE_SHOWALL_AI = 5;

    PokerTable table_;
    HoldemHand hhand_;
    PokerPlayer my_;
    boolean bSimRunning_;
    private CardSelectorPanel selector_;
    private ActionListener last_;
    private DDPanel base_;
    private DDTabbedPane tab_;
    private PokerShowdownPanel showdown_;
    private PokerStatsPanel flopstats_ = null;
    private PokerStatsPanel turnstats_ = null;
    private PokerStatsPanel riverstats_ = null;
    private PokerStatsPanel handstrength_ = null;
    private PokerSimulatorPanel simulations_ = null;
    private PokerStatsPanel ladder_ = null;
    private boolean bShowAllAi_ = false;
    private boolean bCustomized_ = false;
    private String STYLE;
    private boolean bRunning_ = false;

    /**
     * get game - we get game from current context since it can change from underneath us
     */
    private static PokerGame getGame()
    {
        return (PokerGame) GameEngine.getGameEngine().getDefaultContext().getGame(); // TODO: active context when multi-game
    }

    /**
     * init data
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        STYLE = gamephase_.getString("style", "default");

        table_ = new PokerTable(null, 0);
        table_.setSimulation(true);
        PokerPlayer player;
        Hand hand;
        for (int i = 0; i < 10; i++)
        {
            player = new PokerPlayer(i, "Sim " + i, true);
            hand = player.newHand(Hand.TYPE_NORMAL);
            hand.addCard(new Card(CardSuit.UNKNOWN, Card.UNKNOWN));
            hand.addCard(new Card(CardSuit.UNKNOWN, Card.UNKNOWN));
            table_.setPlayer(player, i);
        }

        my_ = table_.getPlayer(0);

        hhand_ = new HoldemHand(table_);
        table_.setHoldemHand(hhand_);
        table_.setButton(0);
        hhand_.setPlayerOrder(false);
        hand = hhand_.getCommunity();
        for (int i = 0; i < 5; i++)
        {
            hand.addCard(new Card(CardSuit.UNKNOWN, Card.UNKNOWN));
        }

        createDialogContents();
    }

    /**
     * create dialog ui
     */
    private void createDialogContents()
    {
        // setup ic
        ImageComponent ic = new ImageComponent("ddlogo20", 1.0d);
        ic.setScaleToFit(false);
        ic.setIconWidth(GamePrefsPanel.ICWIDTH);
        ic.setIconHeight(GamePrefsPanel.ICHEIGHT + 6); // need to be slightly higher for focus

        // ui
        base_ = new DDPanel();
        base_.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        base_.setBorderLayoutGap(0, 0);

        // top cards/buttons
        DDPanel top = new DDPanel();
        top.setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.LEFT, 10, 0, HorizontalFlowLayout.CENTER));
        base_.add(top, BorderLayout.NORTH);

        DDLabelBorder mycards = new DDLabelBorder("myhand", STYLE);
        top.add(mycards, BorderLayout.WEST);
        SimHandPanel myhand = new SimHandPanel(this, table_, my_.getHand());
        myhand.setPreferredWidth(myhand.getPreferredSize().width + 4); // need to make it a bit wider for label to show
        mycards.add(myhand);

        DDLabelBorder boardcards = new DDLabelBorder("community", STYLE);
        top.add(boardcards);
        boardcards.add(new SimHandPanel(this, table_, hhand_.getCommunity()));

        // tabs
        tab_ = new DDTabbedPane(STYLE, null, JTabbedPane.TOP);

        tab_.setOpaque(false);
        tab_.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        base_.add(tab_, BorderLayout.CENTER);

        // showdown        
        showdown_ = new PokerShowdownPanel(context_, this, table_, STYLE);
        showdown_.createUI();
        tab_.addTab("msg.showdown", ic, ic, showdown_);

        // flop/turn/river/ladder
        flopstats_ = new PokerStatsPanel(PokerStatsPanel.FLOP);
        tab_.addTab("msg.flopstats", ic, ic, flopstats_);

        turnstats_ = new PokerStatsPanel(PokerStatsPanel.TURN);
        tab_.addTab("msg.turnstats", ic, ic, turnstats_);

        riverstats_ = new PokerStatsPanel(PokerStatsPanel.RIVER);
        tab_.addTab("msg.riverstats", ic, ic, riverstats_);

        ladder_ = new PokerStatsPanel(PokerStatsPanel.LADDER);
        tab_.addTab("msg.handladder", ic, ic, ladder_);

        // hand strength
        handstrength_ = new PokerStatsPanel(PokerStatsPanel.STRENGTH);
        tab_.addTab("msg.handstrength", ic, ic, handstrength_);

        // hand group sims
        simulations_ = new PokerSimulatorPanel(this);
        tab_.addTab("msg.simulations", ic, ic, simulations_);

        // listen to iconify/close
        if (context_.isInternal())
        {
            context_.getDialog().addInternalFrameListener(new InternalFrameAdapter()
            {
                @Override
                public void internalFrameClosed(InternalFrameEvent e)
                {
                    hidden();
                }

                @Override
                public void internalFrameIconified(InternalFrameEvent e)
                {
                    hidden();
                }

                @Override
                public void internalFrameDeiconified(InternalFrameEvent e)
                {
                    shown();
                }
            });
        }
        else
        {
            context_.getFrame().addWindowListener(new WindowAdapter()
            {

                @Override
                public void windowClosed(WindowEvent e)
                {
                    hidden();
                }

                @Override
                public void windowIconified(WindowEvent e)
                {
                    hidden();
                }

                @Override
                public void windowDeiconified(WindowEvent e)
                {
                    shown();
                }
            });
        }

        // listen to tab changes
        tab_.addChangeListener(this);
    }

    /**
     * Start of phase
     */
    @Override
    public void start()
    {
        // always reload if the push calctool button
        update(MENU_LOAD_ALL);

        // if users presses launch button again, this will be called.  don't run logic again in this case
        if (bRunning_) return;
        bRunning_ = true;

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, base_, true, tab_);
    }

    /**
     * finish
     */
    @Override
    public void finish()
    {
        bRunning_ = false;
        super.finish();
    }

    /**
     * TODO: implement buttons
     */
    @Override
    public boolean processButton(GameButton button)
    {
        if (button.getName().startsWith("help"))
        {
            return true;
        }

        return super.processButton(button);
    }

    /**
     * when window closed or iconified, stop any current sims
     */
    private void hidden()
    {
        showdown_.setStopRequested();
        simulations_.setStopRequested();
    }

    /**
     * when window deiconified, update display if cards not customized
     */
    private void shown()
    {
        if (!bCustomized_)
        {
            update(MENU_LOAD_ALL);
        }
    }

    /**
     * UI representation of a hand
     */
    static class SimHandPanel extends DDPanel
    {
        Hand hand;

        SimHandPanel(SimulatorDialog sim, PokerTable table, Hand hand)
        {
            this.hand = hand;
            CardPanel cp;
            int nCards = hand.size();
            setLayout(new GridLayout(1, nCards, 4, 0));
            for (int i = 0; i < nCards; i++)
            {
                cp = new SimCardPanel(sim, table, hand, hand.getCard(i));
                add(cp);
            }
        }

        @Override
        public void setEnabled(boolean b)
        {
            super.setEnabled(b);
            int nNum = getComponentCount();
            for (int i = 0; i < nNum; i++)
            {
                getComponent(i).setEnabled(b);
            }
        }
    }

    /**
     * UI representation of a card
     */
    private static class SimCardPanel extends CardPanel implements MouseListener, ActionListener
    {
        private DDPopupMenu menu;
        private PokerTable table;
        private SimulatorDialog sim;
        private Hand hand;

        SimCardPanel(SimulatorDialog sim, PokerTable table, Hand hand, Card c)
        {
            super(c, true);
            this.sim = sim;
            this.table = table;
            this.hand = hand;
            getCardPiece().setLarge(false);
            getCardPiece().setGradient(true);
            getCardPiece().setStroke(true);
            setPreferredSize(new Dimension(30, 42));
            addMouseListener(this);
        }

        public void mouseReleased(MouseEvent e)
        {
            if (sim.bSimRunning_) return;
            if (e.isControlDown())
            {
                replaceCard(table, getCardPiece().getCard(), hand, Card.BLANK, sim);
            }
            else
            {
                menu = selectCard(this, e.getPoint(), getCardPiece(), this, sim);
            }
        }

        public void actionPerformed(ActionEvent e)
        {
            menu.setVisible(false);
            menu = null;
            CardSelectorPanel cardSelector = (CardSelectorPanel) e.getSource();
            Card selectedCard = cardSelector.getSelectedCard();
            if (selectedCard != null && !selectedCard.equals(getCardPiece().getCard()))
            {
                replaceCard(table, getCardPiece().getCard(), hand, selectedCard, sim);
            }
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mousePressed(MouseEvent e)
        {
        }

        public void mouseEntered(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
        }
    }

    /**
     * popup card selector
     */
    private static void replaceCard(PokerTable table, Card replaceThis, Hand inThisHand, Card withThis,
                                    SimulatorDialog repaintThis)
    {
        HoldemHand hhand = table.getHoldemHand();
        Card duplicate = null;
        PokerPlayer p;
        //Deck deck = new Deck(true);
        Hand hand;

        if (!withThis.equals(Card.BLANK))
        {
            // go through all players, see if replacement card is in use and remove
            // existing cards from deck (in case we need a replacement)
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                p = table.getPlayer(i);
                hand = p.getHand();
                if (hand.containsCard(withThis))
                {
                    ApplicationError.assertTrue(duplicate == null, "Found two of same card in multiple hands", withThis);
                    //logger.debug("Duplicate of " + withThis + " found in " + p.getName());
                    duplicate = hand.getCard(hand.indexOf(withThis));
                    hand.cardsChanged();
                }
            }

            // ditto for community
            hand = hhand.getCommunity();
            if (hand.containsCard(withThis))
            {
                ApplicationError.assertTrue(duplicate == null, "Found two of same card in multiple hands", withThis);
                //logger.debug("Duplicate of " + withThis + " found in board");
                duplicate = hand.getCard(hand.indexOf(withThis));
                hand.cardsChanged();
            }

            // if we have a duplicate, change it to blank
            if (duplicate != null)
            {
                //logger.debug("duplicate replaced with blank");
                duplicate.setValue(Card.BLANK);
            }
        }

        // change value of replacement
        replaceThis.setValue(withThis);

        // recalc fingerprint
        inThisHand.cardsChanged();

        // repaint
        if (repaintThis != null) repaintThis.updateDisplay(true);
    }

    /**
     * tab changed
     */
    public void stateChanged(ChangeEvent e)
    {
        hidden();
        updateDisplay(false);
    }

    /**
     * logic to update after cards changed
     */
    private void updateDisplay(boolean bCardsChanged)
    {
        if (bCardsChanged) bCustomized_ = true;

        Hand pocket = copyHandNoBlank(my_.getHand());
        Hand community = copyHandNoBlank(hhand_.getCommunity());

        if (showdown_.isSelectedTab()) showdown_.updateDisplay(bCardsChanged);
        if (simulations_.isSelectedTab()) simulations_.updateStats(pocket, community);
        if (handstrength_.isSelectedTab()) handstrength_.updateStats(pocket, community);
        if (flopstats_.isSelectedTab()) flopstats_.updateStats(pocket, community);
        if (turnstats_.isSelectedTab()) turnstats_.updateStats(pocket, community);
        if (riverstats_.isSelectedTab()) riverstats_.updateStats(pocket, community);
        if (ladder_.isSelectedTab()) ladder_.updateStats(pocket, community);
        context_.getWindow().repaint();
    }

    /**
     * copy all cards with new instances of cards to avoid change issues
     */
    private Hand copyHandNoBlank(Hand hand)
    {
        Hand copy = new Hand();
        Card c;
        for (int i = 0; i < hand.size(); i++)
        {
            if (!hand.getCard(i).isBlank())
            {
                c = new Card();
                c.setValue(hand.getCard(i));
                copy.addCard(c);
            }
        }
        return copy;
    }


    /**
     * actual popup menu
     */
    private static DDPopupMenu selectCard(Component parent, Point point, CardPiece cardPiece,
                                          ActionListener listener,
                                          SimulatorDialog sim)
    {
        DDPopupMenu menu = new DDPopupMenu();

        DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, "PokerTable");
        title.setText(PropertyConfig.getMessage("menuitem.changecard2.title", cardPiece.getCard().getDisplay()));
        title.setDisplayMode(DDMenuItem.MODE_TITLE);
        menu.add(title);

        if (sim.selector_ == null)
        {
            sim.selector_ = new CardSelectorPanel(cardPiece.getCard(), true);

        }
        else
        {
            if (sim.last_ != null) sim.selector_.removeActionListener(sim.last_);
            sim.selector_.reinit(cardPiece.getCard());
        }
        sim.selector_.addActionListener(listener);
        sim.last_ = listener;
        menu.add(GuiUtils.CENTER(sim.selector_));

        menu.add(new SimMenuItem("clearall", MENU_CLEAR_ALL, sim));
        menu.add(new SimMenuItem("clearopp", MENU_CLEAR_OPP, sim));
        menu.add(new SimMenuItem("clearboard", MENU_CLEAR_BOARD, sim));

        PokerGame game = getGame();
        if (game != null && !game.isClockMode())
        {
            PokerPlayer local = game.getLocalPlayer();
            menu.add(new SimMenuItem("loadall", MENU_LOAD_ALL, sim));
            menu.add(new SimMenuItem(local.isObserver() ? "loadcomm" : "loadmy", MENU_LOAD_MY, sim));
            if (!game.isOnlineGame())
            {
                SimMenuItem showallai = new SimMenuItem("showallai", MENU_CHANGE_SHOWALL_AI, sim);
                if (sim.bShowAllAi_) showallai.setIcon(checkedIcon_);
                menu.add(showallai);
            }
        }

        menu.show(parent, point.x, point.y);
        return menu;
    }

    /**
     * menu item
     */
    private static class SimMenuItem extends DDMenuItem implements ActionListener
    {
        int nType;
        SimulatorDialog sim;

        SimMenuItem(String sName, int nType, SimulatorDialog sim)
        {
            super(sName, "PokerTable");
            this.nType = nType;
            this.sim = sim;
            setIcon(blankIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            sim.update(nType);
        }
    }

    /**
     * change cards to match current hand(s)
     */
    private void update(int nType)
    {
        PokerGame game = getGame();
        PokerPlayer p;
        PokerTable table = null;
        HoldemHand hhand = null;
        boolean bCardsChanged = false;

        if (game != null)
        {
            table = game.getCurrentTable();
            if (table != null)
            {
                hhand = table.getHoldemHand();
            }
        }

        if (nType == MENU_CLEAR_ALL)
        {
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                p = table_.getPlayer(i);
                bCardsChanged |= changeCard(p.getHand(), 0, Card.BLANK);
                bCardsChanged |= changeCard(p.getHand(), 1, Card.BLANK);
            }
            Hand comm = hhand_.getCommunity();
            for (int i = 0; i < 5; i++)
            {
                bCardsChanged |= changeCard(comm, i, Card.BLANK);
            }
        }
        else if (nType == MENU_CLEAR_OPP)
        {
            for (int i = 1; i < PokerConstants.SEATS; i++)
            {
                p = table_.getPlayer(i);
                bCardsChanged |= changeCard(p.getHand(), 0, Card.BLANK);
                bCardsChanged |= changeCard(p.getHand(), 1, Card.BLANK);
            }
        }
        else if (nType == MENU_CLEAR_BOARD)
        {
            Hand comm = hhand_.getCommunity();
            for (int i = 0; i < 5; i++)
            {
                bCardsChanged |= changeCard(comm, i, Card.BLANK);
            }
        }
        else if (hhand != null)
        {
            if (nType == MENU_CHANGE_SHOWALL_AI)
            {
                bShowAllAi_ = !bShowAllAi_;
            }

            // see if local player is seated (they correspond to Seat 0 at sim table)
            PokerPlayer local = game.getLocalPlayer();
            int nStartSeat = 0;
            for (int i = 0; !local.isObserver() && i < PokerConstants.SEATS; i++)
            {
                p = table.getPlayer(i);
                if (p == local)
                {
                    nStartSeat = p.getSeat();
                }
            }

            int nUpdateIndex = 0;
            Hand nu;
            int nSeat = nStartSeat;
            int nNum = (nType == MENU_LOAD_ALL || nType == MENU_CHANGE_SHOWALL_AI) ? PokerConstants.SEATS :
                       (local.isObserver() ? 0 : 1); // LOAD_MY
            for (int i = 0; i < nNum; i++)
            {
                nu = null;
                p = table.getPlayer(nSeat);
                nSeat++;
                if (nSeat == PokerConstants.SEATS) nSeat = 0;
                if (p != null && (p == local || !p.isFolded()))
                {
                    nu = p.getHand();
                    // for all oppponents cards, set to blank unless show all ai cards options is on
                    // in a practice game or the cards have been exposed.
                    boolean bShowBlank = true;
                    if (bShowAllAi_ && !game.isOnlineGame()) bShowBlank = false;
                    if (p.isCardsExposed()) bShowBlank = false;
                    if (nu != null && i > (local.isObserver() ? -1 : 0) && bShowBlank)
                    {
                        nu = new Hand(Card.BLANK, Card.BLANK);
                    }
                }

                if (nu != null)
                {
                    p = table_.getPlayer(nUpdateIndex++);
                    if (i == 0 && nType == MENU_CHANGE_SHOWALL_AI) continue;
                    bCardsChanged |= changeCard(p.getHand(), 0, nu.getCard(0));
                    bCardsChanged |= changeCard(p.getHand(), 1, nu.getCard(1));
                }
            }

            // set remaining players to unknown
            if (nType == MENU_LOAD_ALL)
            {
                for (int i = nUpdateIndex; i < PokerConstants.SEATS; i++)
                {
                    p = table_.getPlayer(i);
                    bCardsChanged |= changeCard(p.getHand(), 0, Card.BLANK);
                    bCardsChanged |= changeCard(p.getHand(), 1, Card.BLANK);
                }
            }

            if (nType == MENU_LOAD_ALL) showdown_.setNumOpponents(nUpdateIndex - 1);

            // board
            if (nType == MENU_LOAD_ALL || nType == MENU_LOAD_MY)
            {
                int nRound = hhand.getRound();
                if (nRound == HoldemHand.ROUND_SHOWDOWN)
                {
                    HandAction last = hhand.getLastAction();
                    nRound = last.getRound();
                }
                int nNumComm = 0;
                switch (nRound)
                {
                    case HoldemHand.ROUND_RIVER:
                        nNumComm = 5;
                        break;

                    case HoldemHand.ROUND_TURN:
                        nNumComm = 4;
                        break;

                    case HoldemHand.ROUND_FLOP:
                        nNumComm = 3;
                        break;
                }

                nu = hhand.getCommunityForDisplay();
                Card c;
                for (int i = 0; i < 5; i++)
                {
                    if (i < nu.size() && i < nNumComm)
                    {
                        c = nu.getCard(i);
                    }
                    else
                    {
                        c = Card.BLANK;
                    }
                    bCardsChanged |= changeCard(hhand_.getCommunity(), i, c);
                }
            }
        }

        updateDisplay(bCardsChanged);

        if (nType == MENU_LOAD_ALL)
        {
            bCustomized_ = false;
        }
    }

    private boolean changeCard(Hand hand, int i, Card c)
    {
        Card old = hand.getCard(i);
        if (!old.equals(c))
        {
            old.setValue(c);
            hand.cardsChanged();// for fingerprint
            return true;
        }
        return false;
    }
}
