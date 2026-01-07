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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.ImageConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.ai.PokerAI;
import com.donohoedigital.games.poker.dashboard.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.event.PokerTableListener;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.network.OnlineMessage;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import static com.donohoedigital.config.DebugConfig.TESTING;
import static com.donohoedigital.config.DebugConfig.TOGGLE;

public class ShowTournamentTable extends ShowPokerTable implements
                                                        PokerTableInput,
                                                        PokerTableListener,
                                                        PropertyChangeListener,
                                                        TerritorySelectionListener,
                                                        PopupMenuListener,
                                                        FocusListener

{
    public static final int REBUY_BUTTON = 0;
    public static final int REBUY_BROKE = 1;
    public static final int REBUY_LAST = 2;

    private DDPanel buttonbase_;
    private CountdownPanel countdown_;
    private PokerGlassButton buttonFold_;
    private PokerGlassButton buttonBetRaise_;
    private PokerGlassButton buttonAllInBetPot_;
    private PokerGlassButton buttonDeal_;
    private PokerGlassButton buttonCheckCall_;
    private PokerGlassButton buttonRebuy_;
    private GlassButton buttonTestCase_;

    private PokerButton buttonContinueLower_;
    private PokerButton buttonSidePot_;
    private PokerButton buttonContinueMiddle_;

    private boolean bAllIn_ = true;
    private boolean bBetPot_ = false;

    // bet spinner
    private AmountPanel amountPanel_;
    private PokerSpinner amount_;
    private DDNumberSpinner.SpinText amountText_;
    private DDSlider slider_;

    // chat
    private ChatPanel chat_;

    // min chip display
    private PokerIcon minChip_;

    // table we are monitoring
    private volatile PokerTable table_;
    private TournamentDirector td_;

    // keyboard focus indicator
    private DDLabel focus_;
    private static final ImageIcon focusOn_ = ImageConfig.getImageIcon("keyboardfocus-on");
    private static final ImageIcon focusOff_ = ImageConfig.getImageIcon("keyboardfocus-off");

    private TournamentDirector TD()
    {
        if (td_ == null)
        {
            td_ = (TournamentDirector) context_.getGameManager();
        }
        return td_;
    }

    @Override
    public void subclassInit(GameEngine engine, GamePhase gamephase)
    {
        initListeners();

        // minimum chip icon
        minChip_ = new PokerIcon(60, 60, 1120.0f / 1200f, 830.0f / 900.0f, 70f / 1200f);
        minChip_.setCustomImage(new PokerChip());
        minChip_.setUseCustom(true);

        // game buttons
        buttonSidePot_ = new PokerButton(getGameButton("sidepots"), 50 / 0.6f, 10 / 0.6f, 8.40f / 42.0f, 582.0f / 900.0f, 5.5f / 42.0f, true);
        buttonSidePot_.setBorderGap(1, 2, 1, 2);
        buttonSidePot_.setVisible(false);
        buttonSidePot_.setFocusable(true);
        buttonSidePot_.setFocusTraversalKeysEnabled(true);
        buttonContinueMiddle_ = new PokerButton(getGameButton("continue"), 70 / .92f, 20 / .92f, 17.60f / 42.0f, 440.0f / 900.0f, 7f / 42.0f, true);
        buttonContinueMiddle_.setVisible(false);
        buttonContinueMiddle_.setFocusable(true);
        buttonContinueMiddle_.setFocusTraversalKeysEnabled(true);
        buttonContinueMiddle_.setActionID(PokerGame.ACTION_CONTINUE);
        buttonContinueLower_ = new PokerButton(getGameButton("continue"), 70 / .90f, 20 / .90f, 17.55f / 42.0f, 555.0f / 900.0f, 6f / 42.0f, true);
        buttonContinueLower_.setVisible(false);
        buttonContinueLower_.setFocusable(true);
        buttonContinueLower_.setFocusTraversalKeysEnabled(true);
        buttonContinueLower_.setActionID(PokerGame.ACTION_CONTINUE_LOWER);

        // debugging focus...
        //buttonContinueMiddle_.addFocusListener(new GuiUtils.FocusDebugger("continuemiddle"));
        //buttonContinueLower_.addFocusListener(new GuiUtils.FocusDebugger("continuelower"));

        // create bet amount labels
        float nTABLEWIDTH = engine_.getGameboardConfig().getWidth();
        float nTABLEHEIGHT = engine_.getGameboardConfig().getHeight();
        ImageComponent resultIC = ImageComponent.getImage(ResultsPiece.WIN_IC, ResultsPiece.SCALE);
        Territory t;
        TerritoryPoint tp;
        DDText bet;
        PokerIcon icon;
        DDText result;
        int prefw = 135;
        int prefh = 35;
        double width = 7.5f / 42.0f;
        //double height = ((width * prefh) / prefw);
        double icond = 1.45f / 42.0f;
        PokerGameboard.TerritoryInfo info;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            t = PokerUtils.getTerritoryForDisplaySeat(i);
            info = new PokerGameboard.TerritoryInfo();
            PokerGameboard.setTerritoryInfo(t, info);

            tp = t.getTerritoryPoint("bet");
            bet = new PokerBetArea(prefw, prefh, (tp.x_ / nTABLEWIDTH),
                                   (tp.y_ / nTABLEHEIGHT),
                                   width * 1.2, i);
            info.bet = bet;

            tp = t.getTerritoryPoint("icon");
            icon = new PokerIcon(prefh, prefh, (tp.x_ / nTABLEWIDTH) - (icond / 2),
                                 (tp.y_ / nTABLEHEIGHT) - (icond / 2),
                                 icond);
            info.icon = icon;
            info.icon.setCustomImage(new PokerChip(false));

            tp = t.getTerritoryPoint("hole1");
            icon = new PokerIcon(prefh, prefh, (tp.x_ / nTABLEWIDTH) - (icond / 2),
                                 (tp.y_ / nTABLEHEIGHT) - (icond / 2),
                                 icond);
            info.iconfold = icon;

            tp = t.getTerritoryPoint(CardPiece.POINT_HOLE1);
            double textincreasesize = 1.04; // used to make text slightly larger so fits max possible
            double imageWidth = resultIC.getImageWidth() * ResultsPiece.SCALE * textincreasesize;
            double imageHeight = resultIC.getImageHeight() * ResultsPiece.SCALE * textincreasesize;
            result = new PokerResult(resultIC.getImageWidth(), resultIC.getImageHeight(),
                                     ((tp.x_ + ResultsPiece.getXADJUST(t)) / nTABLEWIDTH) - (imageWidth / nTABLEWIDTH / 2),
                                     ((tp.y_ + ResultsPiece.getYADJUST(t)) / nTABLEHEIGHT) - (imageHeight / nTABLEHEIGHT / 2),
                                     imageWidth / nTABLEWIDTH, i);
            info.result = result;

            // add result piece if not there already (could be there from loaded game)
            info.resultpiece = (ResultsPiece) t.getGamePiece(PokerConstants.PIECE_RESULTS, null);
            if (info.resultpiece == null)
            {
                info.resultpiece = new ResultsPiece();
                t.addGamePiece(info.resultpiece);
            }
        }

        // display init
        sync(false);
        game_.setInputMode(MODE_QUITSAVE);
    }

    /**
     * we want resize control
     */
    @Override
    protected boolean createResize()
    {
        return true;
    }

    /**
     * we want camera control
     */
    @Override
    protected boolean createCamera()
    {
        return true;
    }

    /**
     * bottom panel - buttons, chat
     */
    @Override
    protected DDPanel createBottomPanel()
    {
        // fold/bet/check/amount/etc
        buttonbase_ = new DDPanel();
        buttonbase_.setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.CENTER, 5, 0, HorizontalFlowLayout.CENTER));
        buttonbase_.addMouseWheelListener(amountText_); // BUG 359

        // create text (needed when buttons created for mouse wheel listener)
        amountPanel_ = new AmountPanel();

        // gap panel
        DDPanel gap = new DDPanel();
        gap.setPreferredWidth(15);
        gap.addMouseWheelListener(amountText_); // BUG 359

        // buttons
        buttonFold_ = new PokerGlassButton(getGameButton("fold"));
        buttonCheckCall_ = new PokerGlassButton(getGameButton("check"));
        buttonBetRaise_ = new PokerGlassButton(getGameButton("bet"));
        buttonAllInBetPot_ = new PokerGlassButton(getGameButton("allin"));
        Dimension pref = buttonAllInBetPot_.getPreferredSize();
        pref.width += 10; // needs to be slightly larger for "Bet Pot!" to fit
        buttonAllInBetPot_.setPreferredSize(pref);

        // focus indicator
        focus_ = new DDLabel();
        focus_.setPreferredSize(new Dimension(25, 28));
        focus_.setIcon(focusOn_);
        focus_.addMouseWheelListener(amountText_);
        MouseAdapter listener = new MouseAdapter()
        {
            // exit if clicked
            @Override
            public void mouseReleased(MouseEvent e)
            {
                String sNoShowKey = "keyboardfocus";
                if (!DialogPhase.isDialogHidden(sNoShowKey))
                {
                    String sMsg = PropertyConfig.getMessage("msg.keyboardfocus");
                    EngineUtils.displayInformationDialog(context_, sMsg, "msg.keyboardfocus.title", sNoShowKey);
                }
                board_.requestFocus();
            }
        };
        focus_.addMouseListener(listener);

        buttonbase_.add(focus_);

        // deal
        if (!game_.isOnlineGame() || TESTING(PokerConstants.TESTING_ONLINE_AUTO_DEAL_OFF))
        {
            buttonDeal_ = new PokerGlassButton(getGameButton("deal"));
            buttonbase_.add(buttonDeal_);
        }

        buttonbase_.add(buttonFold_);
        buttonbase_.add(gap);
        buttonbase_.add(buttonCheckCall_);
        buttonbase_.add(buttonBetRaise_);
        buttonbase_.add(amountPanel_);
        buttonbase_.add(buttonAllInBetPot_);

        buttonAllInBetPot_.setActionID(PokerGame.ACTION_ALL_IN);
        buttonFold_.setActionID(PokerGame.ACTION_FOLD);

        if (game_.getProfile().isRebuys())
        {
            buttonRebuy_ = new PokerGlassButton(getGameButton("rebuy"));
            buttonbase_.add(buttonRebuy_);
            buttonRebuy_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    NewLevelActions.rebuy(game_, REBUY_BUTTON, game_.getHumanPlayer().getTable().getLevel());
                }
            });
        }

        if (TESTING(PokerConstants.TESTING_TEST_CASE))
        {
            buttonTestCase_ = new GlassButton("testcase", "GlassBig");
            buttonbase_.add(buttonTestCase_);
            buttonTestCase_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    createTestCase();
                }
            });
        }

        DDPanel controlbase = buttonbase_;

        // countdown timer
        if (game_.isOnlineGame())
        {
            controlbase = new DDPanel();
            countdown_ = new CountdownPanel(game_);
            countdown_.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 5));

            // countdown and buttons
            controlbase.add(countdown_, BorderLayout.NORTH);
            controlbase.add(buttonbase_, BorderLayout.CENTER);
        }

        // chat
        DDPanel chatbase = new DDPanel();
        chatbase.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 2));
        chat_ = new ChatPanel(game_, context_, null, "ChatInGame", "BrushedMetal", true);
        chatbase.add(chat_, BorderLayout.CENTER);

        DDPanel bottombase = new DDPanel();
        bottombase.add(GuiUtils.CENTER(controlbase), BorderLayout.NORTH);
        bottombase.add(chatbase, BorderLayout.CENTER);
        bottombase.setMinimumSize(new Dimension(0, 130));
        bottombase.setBorderLayoutGap(5, 0);
        bottombase.addMouseWheelListener(amountText_); // BUG 359

        // add arrow actions to chat field so it controls amount text
        if (chat_.getTextField() != null)
        {
            amount_.addArrowActions(chat_.getTextField());
        }

        return bottombase;
    }

    private boolean bIgnoreNext_ = false;

    ////
    //// Focus listener - used to change icon to indicate when we have focus
    ////
    public void focusGained(FocusEvent e)
    {
        focus_.setIcon(focusOn_);

        // Windows alt-tab bug.  If we get a focus gained event from amountText with no
        // opposite component, ignore the next event if it is a focus gained on the board.
        // This seems to happen when window not at front when this code run (the key is
        // that the amountText gains focus from nothing). This fixes a bug where we get
        // focus gained events in succession on text then board, which invokes the logic to
        // send focus to previous component (something in dashboard).
        //
        if (Utils.ISWINDOWS)
        {
            if (bIgnoreNext_ && e.getSource() == board_)
            {
                return;
            }
            bIgnoreNext_ = e.getSource() == amountText_ && e.getOppositeComponent() == null;
        }

        // if board got focus, but amount text is enabled, transfer focus onward.
        // this works cuz normal order is board_ ==> amountText
        if (e.getSource() == board_ && amountPanel_.isEnabled() && !amountPanel_.isSoonDisabled() &&
            amountPanel_.isVisible())
        {
            // if gained from text, send to previous component of board
            if (e.getOppositeComponent() == amountText_)
            {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(board_);
            }
            // if gained from elsewhere, send to amountText
            else
            {
                amountText_.requestFocus();
            }
        }
    }

    public void focusLost(FocusEvent e)
    {
        // if we lost focus to the board or spinner,
        // (from the board or spinner), then don't
        // turn focus off so we don't see a flash
        if (e.getOppositeComponent() == amountText_ ||
            e.getOppositeComponent() == board_)
        {
            return;
        }

        focus_.setIcon(focusOff_);
    }

    /**
     * spinner with special flag to indicate will be disabled,
     * for focus handling
     */
    private static class PokerSpinner extends DDNumberSpinner
    {
        public PokerSpinner()
        {
            super(0, 199999999, 0, GuiManager.DEFAULT, "PokerTable");
            setBigStep(1000);
            setEditable(true);
            setValue(0);
        }
    }

    /**
     * amount panel
     */
    private class AmountPanel extends DDPanel implements ChangeListener
    {
        private boolean bSoonDisabled_ = false;

        @SuppressWarnings({"ThisEscapedInObjectConstruction"})
        public AmountPanel()
        {
            setBorderLayoutGap(0, 0);

            amount_ = new PokerSpinner();
            amount_.setEnabled(false);
            amount_.addChangeListener(this);
            amountText_ = amount_.getTextField();
            amountText_.addFocusListener(ShowTournamentTable.this);
            GuiUtils.addKeyAction(amountText_, JComponent.WHEN_FOCUSED, "betraise", new BetRaise(), KeyEvent.VK_ENTER, 0);
            board_.addMouseWheelListener(amountText_);
            Dimension size = amount_.getPreferredSize();
            size.height -= 7;
            amount_.setPreferredSize(size);

            slider_ = new DDSlider(GuiManager.DEFAULT, "PokerTable");
            slider_.setEnabled(false);
            slider_.setFocusable(false);
            slider_.addChangeListener(this);
            slider_.setValue(0);
            slider_.setSnapToTicks(true);
            size = slider_.getPreferredSize();
            size.width = amount_.getPreferredSize().width;
            slider_.setPreferredSize(size);

            add(amount_, BorderLayout.CENTER);
            add(slider_, BorderLayout.SOUTH);
        }

        public void setSoonDisabled(boolean b)
        {
            bSoonDisabled_ = b;
        }

        public boolean isSoonDisabled()
        {
            return bSoonDisabled_;
        }

        @Override
        public void setEnabled(boolean b)
        {
            if (!b) bSoonDisabled_ = false;
            super.setEnabled(b);
            slider_.setEnabled(b);
            amount_.setEnabled(b);
        }

        public void setMin(int nMin)
        {
            amount_.setMin(nMin);
            slider_.setMinimum(nMin);
        }

        public void setMax(int nMax)
        {
            amount_.setMax(nMax);
            slider_.setMaximum(nMax);
        }

        public void setValue(int nValue)
        {
            amount_.setValue(nValue);
            slider_.setValue(nValue);
        }

        public void setStep(int nStep)
        {
            amount_.setStep(nStep);
            slider_.setMinorTickSpacing(nStep);
        }

        public void setBigStep(int nBigStep)
        {
            amount_.setBigStep(nBigStep);
            slider_.setMajorTickSpacing(nBigStep);
        }

        private boolean bUpdating = false;

        public void stateChanged(ChangeEvent e)
        {
            if (bUpdating) return;
            bUpdating = true;

            // don't check if amount is disabled (ignore)
            if ((e.getSource() == amount_) && amount_.isEnabled())
            {
                // Spinner change - enable/disable bet/raise button
                boolean b = buttonBetRaise_.isEnabled();
                boolean nu = amount_.isValidData();

                if (b != nu)
                {
                    buttonBetRaise_.setEnabled(nu);
                }

                if (nu)
                {
                    slider_.setValue(amount_.getValue());
                }
            }
            else if (e.getSource() == slider_ && slider_.isEnabled())
            {
                int nValue = PokerUtils.roundAmountMinChip(table_, slider_.getValue());
                amount_.setValue(nValue);
            }
            bUpdating = false;
        }
    }

    /**
     * Our dashboard components
     */
    @Override
    protected void addDashboardItems(DashboardManager manager)
    {
        TournamentProfile profile = game_.getProfile();
        boolean bDashOnline = profile.isAllowDash();
        boolean bDashAdvisor = profile.isAllowAdvisor();

        if (game_.isOnlineGame())
        {
            if (game_.getOnlineMode() == PokerGame.MODE_CLIENT)
            {
                manager.addItem(new HostStatusDash(context_), true);
            }
            else
            {
                manager.addItem(new HostDash(context_), true);
            }
            manager.addItem(new OnlineDash(context_), true);
        }

        manager.addItem(new DashboardClock(context_), true);
        manager.addItem(new UpNext(context_), false);
        manager.addItem(new AdvanceAction(context_), false);
        manager.addItem(new MyHand(context_), true);

        if (!game_.isOnlineGame() || bDashOnline)
        {
            manager.addItem(new PotOdds(context_), true);
            manager.addItem(new ImproveOdds(context_), true);
            manager.addItem(new HandStrengthDash(context_), true);
            manager.addItem(new SimulatorDash(context_), false);
        }
        manager.addItem(new MyTable(context_), false);
        manager.addItem(new Rank(context_), false);
        if (!game_.isOnlineGame() || bDashAdvisor)
        {
            manager.addItem(new DashboardAdvisor(context_), false);
            manager.addItem(new DashboardPlayerInfo(context_), false);
            if (!game_.isOnlineGame()) manager.addItem(new CheatDash(context_), false);
        }

        if (game_.isOnlineGame())
        {
            manager.addItem(new ObserversDash(context_), false);
            manager.addItem(new PlayerInfo(context_), false);
        }

        manager.addItem(new DebugDash(context_), false);

        super.addDashboardItems(manager);
    }

    /**
     * poker image button sub class
     */
    private class PokerGlassButton extends GlassButton
    {
        public PokerGlassButton(GameButton button)
        {
            super(button.getName(), "GlassBig");
            setPreferredSize(new Dimension(64, 28));
            setBorderGap(0, 0, 0, 0);
            setFocusable(false);
            addActionListener(new EngineButtonListener(context_, ShowTournamentTable.this, button));
            addActionListener(ShowTournamentTable.this);
            addMouseWheelListener(amountText_); // BUG 359
        }
    }

    /**
     * Synchronize display with data
     */
    public void sync(boolean bRepaint)
    {
        // match display to any loaded hands
        HoldemHand hhand = table_.getHoldemHand();
        PokerUtils.clearCards(false);
        PokerUtils.clearResults(context_, false);
        DealDisplay.syncCards(table_);
        DealCommunity.syncCards(table_);
        ButtonDisplay.displayButton(table_, 0);
        updatePotDisplay(false);
        updateMinChip(false);
        if (hhand != null)
        {
            // if showdown, redisplay in case options changed regarding what to display
            if (hhand.getRound() == HoldemHand.ROUND_SHOWDOWN)
            {
                Showdown.displayShowdown(engine_, context_, hhand);
            }
        }
        if (bRepaint) board_.repaintAll();
    }

    /**
     * init - setu chat panel, start TournamentDirector, display AUTOPILOT message
     */
    @Override
    public void start()
    {
        super.start();

        // do remaining start a bit later to allow for
        // UI to draw
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                poststart();
            }
        });
    }

    /**
     * startup after drawing - start TD, init chat
     */
    private void poststart()
    {
        // start TournamentDirector so we can tell chat what it is
        nextPhaseNow();

        // initiate chat panel
        chat_.setChatManager((ChatManager) context_.getGameManager());
        chat_.start();
        PokerUtils.setChat(chat_);

        // autopilot message
        if (TESTING(PokerConstants.TESTING_AUTOPILOT_INIT) && !TESTING(PokerConstants.TESTING_AUTOPILOT))
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            EngineUtils.displayInformationDialog(context_, "Autopilot is enabled, but currently paused (F5 or F9).");

                        }
                    }
            );
        }
    }

    /**
     * override to not call nextphase (done explicitly in start(), above)
     */
    @Override
    public void nextPhase()
    {
        // do nothing
    }

    /**
     * init - add poker table listener
     */
    public void initListeners()
    {
        board_.addFocusListener(this);
        board_.addGamePieceSelectionListener(new PokerGamePieceListener());
        game_.addPropertyChangeListener(PokerGame.PROP_CURRENT_TABLE, this);
        board_.addTerritorySelectionListener(this);
        board_.setTerritorySelectionMode(Gameboard.SELECTION_MODE_MULTIPLE);
        trackTable(game_.getCurrentTable(), false);
    }

    /**
     * Cleanup poker table listeners
     */
    @Override
    public void finish()
    {
        board_.removeFocusListener(this);
        // no need to removeGamePieceSelectionListener (board_ goes away when this does)
        board_.removeTerritorySelectionListener(this);
        game_.removePropertyChangeListener(PokerGame.PROP_CURRENT_TABLE, this);
        trackTable(null, false);
        chat_.finish();
        PokerUtils.setChat(null);
        amount_.getParent().removeAll(); // remove so memory can be cleaned earlier (can retain due to focus in this widget)
        super.finish();
    }

    /**
     * track table
     */
    private void trackTable(PokerTable table, boolean bRepaint)
    {
        // cleanup old
        if (table_ != null) table_.removePokerTableListener(this, PokerTableEvent.TYPES_ALL);

        // store new and add listener if different
        if (table_ != table)
        {
            table_ = table;
            if (table_ != null)
            {
                table_.addPokerTableListener(this,
                                             PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED |
                                             PokerTableEvent.TYPE_PLAYER_ACTION |
                                             PokerTableEvent.TYPE_DEALER_ACTION |
                                             PokerTableEvent.TYPE_LEVEL_CHANGED |
                                             PokerTableEvent.TYPE_BUTTON_MOVED |
                                             PokerTableEvent.TYPE_PREFS_CHANGED |
                                             PokerTableEvent.TYPE_PLAYER_REBUY |
                                             PokerTableEvent.TYPE_PLAYER_ADDON |
                                             PokerTableEvent.TYPE_CLEANING_DONE |
                                             PokerTableEvent.TYPE_NEW_PLAYERS_LOADED);
            }

            // repaint board to reflect new table if directed to do so
            if (bRepaint)
            {
                GuiUtils.invoke(new SwingIt(SWING_SYNC));
                if (table_ != null && !TESTING(PokerConstants.TESTING_AUTOPILOT))
                {
                    final PokerPlayer human = game_.getHumanPlayer();
                    final String sMsg = PropertyConfig.getMessage(human.isObserver() ?
                                                                  (human.isWaiting() ? "msg.tablechange.waiting" :
                                                                   "msg.tablechange.observer")
                                                                                     : "msg.tablechange.player",
                                                                  table_.getName(),
                                                                  game_.getHumanPlayer().getSeat() + 1,
                                                                  game_.getSeats());
                    // modal for practice / non-modal for online
                    Runnable rmsg = new Runnable()
                    {
                        String _sMsg = sMsg;
                        PokerPlayer _human = human;

                        public void run()
                        {
                            EngineUtils.displayInformationDialog(context_, _sMsg,
                                                                 null,
                                                                 _human.isWaiting() ? null : // no option to dismiss when waiting
                                                                 (_human.isObserver() ? "change.observer" : "change.player"),
                                                                 _human.isObserver() ? "change.observer" : "change.player",
                                                                 !game_.isOnlineGame());
                        }
                    };

                    if (game_.isOnlineGame()) GuiUtils.invoke(rmsg);
                    else GuiUtils.invokeAndWait(rmsg);
                }

            }
        }
    }


    /**
     * Game property changed - we track current table
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        // table changed
        if (name.equals(PokerGame.PROP_CURRENT_TABLE))
        {
            trackTable(game_.getCurrentTable(), true);
        }
    }

    private static final int SWING_NONE = 0;
    private static final int SWING_REPAINT_SEAT = 1;
    private static final int SWING_DISPLAY_BUTTON = 2;
    private static final int SWING_REBUY_BUTTON = 3;
    private static final int SWING_POT_DISPLAY = 4;
    private static final int SWING_SYNC = 5;
    private static final int SWING_MINCHIP = 6;
    private static final int SWING_RECHECK = 7;

    /**
     * Class to use with invokeLater
     */
    private class SwingIt implements Runnable
    {
        int nType;
        PokerPlayer p;
        PokerTable t;
        boolean bRepaintAll = false;

        SwingIt()
        {
            nType = SWING_NONE;
            bRepaintAll = true;
        }

        SwingIt(PokerPlayer p)
        {
            this.p = p;
            nType = SWING_REPAINT_SEAT;
        }

        SwingIt(int nType)
        {
            this.nType = nType;
        }

        SwingIt(PokerTable t, int nType)
        {
            this.t = t;
            this.nType = nType;
        }

        SwingIt(int nType, boolean bRepaint)
        {
            this(nType);
            this.bRepaintAll = bRepaint;
        }

        public void run()
        {
            if (table_.isZipMode()) return;
            //logger.debug("Swingit: "+ nType);
            switch (nType)
            {
                case SWING_REPAINT_SEAT:
                    if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT))
                        logger.debug("SwingIt calling repaintTerritory for " + p.getName());
                    board_.repaintTerritory(PokerUtils.getTerritoryForTableSeat(p.getTable(), p.getSeat()));
                    break;

                case SWING_DISPLAY_BUTTON:
                    ButtonDisplay.displayButton(t, 0);
                    break;

                case SWING_REBUY_BUTTON:
                    setRebuyButton(true);
                    break;

                case SWING_POT_DISPLAY:
                    updatePotDisplay(true);
                    break;

                case SWING_SYNC:
                    sync(true);
                    break;

                case SWING_MINCHIP:
                    updateMinChip(true);
                    break;

                case SWING_RECHECK:
                    setInputMode(PokerTableInput.MODE_RECHECK, null, null);
                    break;

                case SWING_NONE:
                default:
                    break;
            }

            if (bRepaintAll)
            {
                PokerUtils.getPokerGameboard().repaintAll();
            }
        }
    }

    /**
     * handle changes to table by repainting as appropriate
     */
    public void tableEventOccurred(PokerTableEvent event)
    {
        PokerTable table = event.getTable();
        HoldemHand hhand = table.getHoldemHand();

        if (TournamentDirector.DEBUG_EVENT_DISPLAY) logger.debug("Event received: " + event.toString());
        switch (event.getType())
        {
            // new players added, repaint all
            case PokerTableEvent.TYPE_NEW_PLAYERS_LOADED:
                GuiUtils.invoke(new SwingIt());
                break;

            case PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED:
                PokerPlayer old = hhand.getPlayerAt(event.getOld());
                PokerPlayer nu = hhand.getPlayerAt(event.getNew());

                if (old != null)
                {
                    GuiUtils.invoke(new SwingIt(old));
                }
                if (nu != null)
                {
                    GuiUtils.invoke(new SwingIt(nu));
                }
                break;

            case PokerTableEvent.TYPE_LEVEL_CHANGED:
                // check input incase game changed (forcing buttons to update)
                GuiUtils.invoke(new SwingIt(SWING_MINCHIP, false));
                GuiUtils.invoke(new SwingIt(SWING_RECHECK, false));
                break;

            case PokerTableEvent.TYPE_DEALER_ACTION:
                // repaint all to clear bets 
                if (!table_.isZipMode()) GuiUtils.invoke(new SwingIt());
                break;

            case PokerTableEvent.TYPE_PLAYER_ACTION:
                PokerPlayer p = event.getPlayer();
                GuiUtils.invoke(new SwingIt(SWING_POT_DISPLAY, false));
                GuiUtils.invoke(new SwingIt(p));
                break;

            case PokerTableEvent.TYPE_PREFS_CHANGED:
                GuiUtils.invoke(new SwingIt(SWING_SYNC));
                break;

            case PokerTableEvent.TYPE_BUTTON_MOVED:
                // if button moved while dealing for button, ignore since it will
                // be moved via a specific phase, along with the cards display
                if (table.getTableState() == PokerTable.STATE_DEAL_FOR_BUTTON) break;
                GuiUtils.invoke(new SwingIt(table, SWING_DISPLAY_BUTTON));
                break;

            case PokerTableEvent.TYPE_CLEANING_DONE:
                // set null so pot is redrawn empty (duplicates OtherTables, but
                // this event is called upon change *to* STATE_CLEAN, which
                // then invokes OtherTables - redundancy is okay)
                table.setHoldemHand(null);
                PokerUtils.setNewHand();
                PokerUtils.clearCards(false);
                PokerUtils.clearResults(context_, false);
                GuiUtils.invoke(new SwingIt(SWING_POT_DISPLAY, true));
                break;

            case PokerTableEvent.TYPE_PLAYER_ADDON:
            case PokerTableEvent.TYPE_PLAYER_REBUY:
                if (event.getPlayer().isHuman())
                {
                    GuiUtils.invoke(new SwingIt(event.getPlayer()));
                    GuiUtils.invoke(new SwingIt(SWING_REBUY_BUTTON));
                }
                break;
        }
    }

    /**
     * min chip
     *
     * @param bRepaint
     */
    private void updateMinChip(boolean bRepaint)
    {
        minChip_.setHidden(false);
        PokerChip chip = (PokerChip) minChip_.getCustomImage();
        chip.setValue(table_.getMinChip());
        minChip_.setToolTipText(PropertyConfig.getMessage("msg.tooltip.minchip", table_.getMinChip()));
        if (bRepaint) minChip_.repaint();
    }

    @Override
    public String getDebugDisplay(Territory t)
    {
        // only do player seats
        int nSeat = PokerUtils.getDisplaySeatForTerritory(t);
        if (nSeat == -1) return null;

        // if no player there, skip
        PokerPlayer player = PokerUtils.getPokerPlayer(context_, t);
        if (player == null) return null;

        // get hand strenght
        double hs = player.getHandStrength();
        hs *= 100;

        // get hand pot
        double hp = -1;//player.getHandPotentialDisplay();
        hp *= 100;

        // negative means no value
        if (hs < 0 && hp < 0) return null;

        StringBuilder sb = new StringBuilder();
        if (hs >= 0) sb.append("HS ").append(HandStat.fPerc.form(hs)).append("% ");
        if (hp >= 0)
        {
            //sb.append("HP ").append(HandStat.fPerc.form(hp)).append("% ");
            sb.append("EHS ").append(HandStat.fPerc.form(100 * player.getEffectiveHandStrength())).append("%");
        }

        return sb.toString();
    }

    /**
     * Setup buttons on pokertable based on current mode
     */
    @Override
    public void setInputMode(int nMode, HoldemHand hhand, PokerPlayer player)
    {
        int nOldMode = getInputMode();

        // recheck - set to same value, so fudge old mode so we don't skip out early
        if (nMode == MODE_RECHECK)
        {
            nMode = nOldMode;
            nOldMode = MODE_RECHECK;
        }

        // always do rebuy (due to pending rebuy logic)
        if (nMode != nOldMode || nMode == MODE_REBUY_CHECK) setRebuyButton(true);

        // don't do anything else for a rebuy check
        if (nMode == MODE_REBUY_CHECK) return;

        // skip if already at this mode
        if (nMode == nOldMode) return;

        // remember mode
        super.setInputMode(nMode, hhand, player);

        // no updates in zip mode
        if (table_.isZipMode()) return;

        // get fold check pref
        boolean bFoldCheck = PokerUtils.isOptionOn(PokerConstants.OPTION_CHECKFOLD);

        boolean bAllowContinueLower = false;
        boolean bAllowContinue = false;
        boolean bAllowAllIn = false;
        boolean bAllowBetRaise = false;
        boolean bAllowCheckCall = false;
        boolean bAllowDeal = false;
        boolean bAllowFold = false;
        boolean bAllowTestCase = false;
        boolean bShowTimer = false;

        boolean bAllowSave = !game_.isOnlineGame() || (game_.isOnlineGame() && game_.getLocalPlayer().isHost());
        boolean bAllowQuit = true;

        boolean bAllowAmount = false;

        // set buttons based on mode
        switch (nMode)
        {
            case MODE_INIT:
                break;

            case MODE_NONE:
                bAllowQuit = false;
                bAllowSave = false;
                break;

            case MODE_QUITSAVE:
                break;

            case MODE_DEAL:
                bAllowDeal = true;
                amountPanel_.setValue(0);
                if (!isFocusInChat()) board_.requestFocus();
                break;

            case MODE_CHECK_BET:
                bAllowAllIn = true;
                bAllowCheckCall = true;
                bAllowBetRaise = true;
                bAllowFold = bFoldCheck;
                bAllowAmount = true;
                bAllowTestCase = true;
                bShowTimer = true;
                buttonBetRaise_.rename("bet");
                buttonBetRaise_.setActionID(PokerGame.ACTION_BET);
                buttonCheckCall_.rename("check");
                buttonCheckCall_.setActionID(PokerGame.ACTION_CHECK);
                setBetRaiseButton(hhand, player, false);
                break;

            case MODE_CHECK_RAISE:
                bAllowAllIn = true;
                bAllowCheckCall = true;
                bAllowBetRaise = true;
                bAllowFold = bFoldCheck;
                bAllowAmount = true;
                bAllowTestCase = true;
                bShowTimer = true;
                buttonBetRaise_.rename("raise");
                buttonBetRaise_.setActionID(PokerGame.ACTION_RAISE);
                buttonCheckCall_.rename("check");
                buttonCheckCall_.setActionID(PokerGame.ACTION_CHECK);
                setBetRaiseButton(hhand, player, true);
                break;

            case MODE_CALL_RAISE:
                bAllowAllIn = true;
                bAllowCheckCall = true;
                bAllowBetRaise = true;
                bAllowFold = true;
                bAllowAmount = true;
                bAllowTestCase = true;
                bShowTimer = true;
                buttonBetRaise_.rename("raise");
                buttonBetRaise_.setActionID(PokerGame.ACTION_RAISE);
                buttonCheckCall_.rename("call");
                buttonCheckCall_.setActionID(PokerGame.ACTION_CALL);
                setBetRaiseButton(hhand, player, true);
                break;

            case MODE_CONTINUE_LOWER:
                bAllowContinueLower = true;

                break;

            case MODE_CONTINUE:
                bAllowContinue = true;
                break;

            default:
                throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Invalid mode: " + nMode, null);
        }

        // fix allin/bet pot button
        int nGameType;
        if (hhand != null)
        {
            nGameType = hhand.getGameType();
        }
        else
        {
            nGameType = table_.getProfile().getGameType(table_.getLevel());
        }

        boolean bRevalidate = false;
        if (nGameType == PokerConstants.TYPE_NO_LIMIT_HOLDEM)
        {
            bAllIn_ = true;
            bBetPot_ = false;
            buttonAllInBetPot_.rename("allin");
            if (!buttonAllInBetPot_.isVisible())
            {
                buttonAllInBetPot_.setVisible(true);
                bRevalidate = true;
            }
            if (!amountPanel_.isVisible())
            {
                amountPanel_.setVisible(true);
                bRevalidate = true;
            }

        }
        else if (nGameType == PokerConstants.TYPE_POT_LIMIT_HOLDEM)
        {
            bAllIn_ = false;
            bBetPot_ = true;
            buttonAllInBetPot_.rename("betpot");
            if (!buttonAllInBetPot_.isVisible())
            {
                buttonAllInBetPot_.setVisible(true);
                bRevalidate = true;
            }
            if (!amountPanel_.isVisible())
            {
                amountPanel_.setVisible(true);
                bRevalidate = true;
            }
        }
        else // limit
        {
            bAllIn_ = false;
            bBetPot_ = false;
            if (buttonAllInBetPot_.isVisible())
            {
                buttonAllInBetPot_.setVisible(false);
                bRevalidate = true;
            }
            if (amountPanel_.isVisible())
            {
                if (amount_.hasFocus()) board_.requestFocus();
                amountPanel_.setVisible(false);
                bRevalidate = true;
            }
        }

        if (bRevalidate)
        {
            updateButtonBase();
        }

        if (nMode != MODE_INIT)
        {
            buttonContinueLower_.setEnabled(bAllowContinueLower);
            buttonContinueLower_.setVisible(bAllowContinueLower);
            if (bAllowContinueLower)
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                // in case not yet enabled, wait a bit
                                // this seems like a hack, but in some cases
                                // the request focus doesn't occur, and this
                                // seems to fix it.  Doh.
                                if (!buttonContinueMiddle_.isEnabled())
                                {
                                    Utils.sleepMillis(100);
                                }
                                buttonContinueLower_.requestFocus();
                            }
                        }
                );
            }

            buttonContinueMiddle_.setEnabled(bAllowContinue);
            buttonContinueMiddle_.setVisible(bAllowContinue);
            if (bAllowContinue)
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                // see note above
                                if (!buttonContinueMiddle_.isEnabled())
                                {
                                    Utils.sleepMillis(100);
                                }

                                buttonContinueMiddle_.requestFocus();
                            }
                        }
                );
            }

            if (countdown_ != null) countdown_.countdown(bShowTimer);

            buttonAllInBetPot_.setEnabled(bAllowAllIn);
            buttonCheckCall_.setEnabled(bAllowCheckCall);
            if (buttonDeal_ != null) buttonDeal_.setEnabled(bAllowDeal);
            buttonFold_.setEnabled(bAllowFold);
            buttonSave_.setEnabled(bAllowSave);
            buttonQuit_.setEnabled(bAllowQuit);
            if (buttonTestCase_ != null) buttonTestCase_.setEnabled(bAllowTestCase);

            if (!bAllowBetRaise) buttonBetRaise_.setEnabled(false);

            if (!bAllowAmount && amountPanel_.isVisible())
            {
                amountPanel_.setSoonDisabled(true);
                if (amount_.hasFocus() && !bAllowContinueLower && !bAllowContinue)
                {
                    board_.requestFocusDirect();
                }

                // run later so focus transfers smoothly
                // otherwise focus will flash to scrollbar
                // when we set disabled
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        amountPanel_.setEnabled(false);
                    }
                });
            }
        }
    }

    private void updateButtonBase()
    {
        buttonbase_.revalidate();
        buttonbase_.doLayout();
        buttonbase_.repaint();
    }

    /**
     * set rebuy button
     */
    private void setRebuyButton(boolean bEnabledIfAvailable)
    {
        // if no rebuy button, do nothing
        if (buttonRebuy_ == null) return;

        // see if no more rebuys for human.  If so, remove rebuy button
        PokerGame game = (PokerGame) context_.getGame();
        PokerPlayer human = game.getHumanPlayer();
        PokerTable table = human.getTable();
        if (human.isObserver() || human.isEliminated() || table.isRebuyDone(human))
        {
            buttonbase_.remove(buttonRebuy_);
            buttonRebuy_ = null;
            updateButtonBase();
            return;
        }

        // still can rebuy - do enabled.  Set to true only if
        // rebuy is currently allowed
        boolean bEnable = bEnabledIfAvailable;

        if (bEnable)
        {
            bEnable = table.isRebuyAllowed(human);

            // BUG 420 - don't allow rebuy when broke at showdown
            // since the user will be auto-prompted
            HoldemHand hhand = table.getHoldemHand();
            if (bEnable && human.getChipCount() == 0 &&
                hhand != null && hhand.getRound() == HoldemHand.ROUND_SHOWDOWN)
            {
                bEnable = false;
            }
        }
        buttonRebuy_.setEnabled(bEnable);
    }

    /**
     * Do raise button
     */
    private void setBetRaiseButton(HoldemHand hhand, PokerPlayer player, boolean bRaise)
    {
        boolean bEnabled = false;
        int nMax = 0;

        // figure out max (diff if raise/call button)
        if (hhand != null)
        {
            if (bRaise) nMax = hhand.getMaxRaise(player);
            else nMax = hhand.getMaxBet(player);
        }

        // if max is non zero, set value, min, max
        if (nMax > 0)
        {
            // figure out min bet (usually big blind)
            int nMin = hhand.getMinBet();
            if (bRaise)
            {
                nMin = hhand.getMinRaise();
            }

            // maker sure min isn't bigger than max
            if (nMin > nMax) nMin = nMax;

            // step are based on blinds
            int nMinChip = hhand.getMinChip();
            amountPanel_.setStep(nMinChip);
            amountPanel_.setBigStep(nMinChip * 10);

            // set min/max (max before min to ensure correct updating
            // in case previous max is less then new min)
            amountPanel_.setMax(nMax);
            amountPanel_.setMin(nMin);
            amountPanel_.setValue(nMin);

            bEnabled = true;
        }

        if (!bEnabled) amountPanel_.setSoonDisabled(true);

        if (!amountPanel_.isVisible() || !bEnabled)
        {
            if (!isFocusInChat()) board_.requestFocusDirect();
        }
        else
        {
            // request focus handled after enabled set to true (below) - BUG 502
        }

        buttonBetRaise_.setEnabled(bEnabled);
        if (bEnabled)
        {
            // run later (like set false) so ordering is correct in case of
            // rapid, multiple calls to this
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    amountPanel_.setEnabled(true);
                    if (amountPanel_.isVisible())
                    {
                        if (!isFocusInChat()) amount_.requestFocus();
                    }
                }
            });
        }
        else
        {
            // run later so focus transfers smoothly
            // otherwise focus will flash to scrollbar
            // when we set disabled
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    amountPanel_.setEnabled(false);
                }
            });
        }
    }

    /**
     * bet raise button
     */
    private class BetRaise extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            if (buttonBetRaise_.isEnabled())
            {
                buttonBetRaise_.doClick(10);
                buttonBetRaise_.setEnabled(false);
            }
        }
    }

    /**
     * Display side pot button if there are side pots; hide otherwise
     *
     * @param bRepaint
     */
    private void updatePotDisplay(boolean bRepaint)
    {
        boolean bSide = false;
        HoldemHand hhand = table_.getHoldemHand();

        if (hhand != null)
        {
            int nNumPots = hhand.getNumPotsExcludingOverbets();

            if (nNumPots > 1)
            {
                bSide = true;
            }

            // get side pot msg
            nNumPots -= 1; // subtract main pot

            String sSideMsg = !bSide ? null :
                              PropertyConfig.getMessage(nNumPots == 1 ? "msg.pot.side.singular" : "msg.pot.side.plural",
                                                        nNumPots);

            // update side pot text
            if (bSide && !buttonSidePot_.getText().equals(sSideMsg))
            {
                buttonSidePot_.setText(sSideMsg);
            }
        }

        if (buttonSidePot_.isVisible() != bSide)
        {
            buttonSidePot_.setVisible(bSide);
        }

        if (bRepaint && !table_.isZipMode()) PokerUtils.repaintPot();
    }

    /**
     * deal
     */
    void deal()
    {
        if (buttonDeal_ != null && buttonDeal_.isEnabled())
        {
            buttonDeal_.doClick(10);
            buttonDeal_.setEnabled(false);
        }
    }


// In POKER 2.0, disable right-click to deal cuz it is more natural
// to right click to bring up context menu
//    /**
//     * right click - deal
//     */
//    public void mousePressed(MouseEvent e)
//	{
//        if (e.getButton() != MouseEvent.BUTTON1 && (!(e.getSource() instanceof JButton)))
//		{
//            deal();
//        }
//        else
//        {
//            super.mousePressed(e);
//        }
//    }

    /**
     * Override to only accept keystrokes from board or from amount spinner
     */
    @Override
    protected boolean filterKeystroke(KeyEvent k)
    {
        Object src = k.getSource();

        if (k.getKeyCode() == KeyEvent.VK_F1)
        {
            return false;
        }

        return !(src == board_ || src == amountText_ ||
                 src == buttonContinueLower_ || src == buttonContinueMiddle_);

    }

    @Override
    protected boolean handleKeyPressed(KeyEvent event)
    {
        // get disable shortcuts pref
        boolean bDisableShortcuts = PokerUtils.isOptionOn(PokerConstants.OPTION_DISABLE_SHORTCUTS);

        int key = event.getKeyCode();

        if (event.getModifiersEx() == 0 || event.isShiftDown())
        {
            switch (key)
            {
                case KeyEvent.VK_F1:
                    // F1 key - cycle focus
                    if (isFocusInChat() || chat_.getTextField() == null)
                    {
                        board_.requestFocus();
                    }
                    else if (chat_.getTextField() != null)
                    {
                        chat_.requestFocus();
                    }
                    return true;
                case KeyEvent.VK_D:
                    if (!bDisableShortcuts)
                    {
                        deal();
                    }
                    return true;
                case KeyEvent.VK_F:
                    if (buttonFold_.isEnabled() && !bDisableShortcuts)
                    {
                        buttonFold_.doClick(10);
                        buttonFold_.setEnabled(false);
                    }
                    else if (!bDisableShortcuts)
                    {
                        PokerUtils.setFoldKey(context_);
                    }
                    return true;
                case KeyEvent.VK_A:
                    if (bAllIn_ && buttonAllInBetPot_.isEnabled() && !bDisableShortcuts)
                    {
                        buttonAllInBetPot_.doClick(10);
                        buttonAllInBetPot_.setEnabled(false);
                        return true;
                    }
                    break;
                case KeyEvent.VK_P:
                    if (bBetPot_ && buttonAllInBetPot_.isEnabled() && !bDisableShortcuts)
                    {
                        buttonAllInBetPot_.doClick(10);
                        buttonAllInBetPot_.setEnabled(false);
                        return true;
                    }
                    break;
                case KeyEvent.VK_B:
                    if (buttonBetRaise_.isEnabled() &&
                        getInputMode() == PokerTableInput.MODE_CHECK_BET && !bDisableShortcuts)
                    {
                        buttonBetRaise_.doClick(10);
                        buttonBetRaise_.setEnabled(false);
                        return true;
                    }
                    break;
                case KeyEvent.VK_R:
                    if (buttonBetRaise_.isEnabled() &&
                        getInputMode() != PokerTableInput.MODE_CHECK_BET && !bDisableShortcuts)
                    {
                        buttonBetRaise_.doClick(10);
                        buttonBetRaise_.setEnabled(false);
                        return true;
                    }
                    break;
                case KeyEvent.VK_C:
                    if (buttonContinueMiddle_.isEnabled() && buttonContinueMiddle_.isVisible() && !bDisableShortcuts)
                    {
                        buttonContinueMiddle_.doClick(10);
                        buttonContinueMiddle_.setEnabled(false);
                        return true;
                    }
                    else if (buttonContinueLower_.isEnabled() && buttonContinueLower_.isVisible() && !bDisableShortcuts)
                    {
                        buttonContinueLower_.doClick(10);
                        //buttonContinueLower_.setEnabled(false);  Don't disable - causes BUG 468
                        return true;
                    }
                    else if (buttonCheckCall_.isEnabled() && !bDisableShortcuts)
                    {
                        buttonCheckCall_.doClick(10);
                        buttonCheckCall_.setEnabled(false);
                        return true;
                    }
                    break;

                case KeyEvent.VK_E:
                    if (buttonRebuy_ != null && buttonRebuy_.isEnabled() && !bDisableShortcuts)
                    {
                        buttonRebuy_.doClick(10);
                        //don't do this since modal
                        //buttonRebuy_.setEnabled(false);
                        return true;
                    }
                    break;

                case KeyEvent.VK_F5:
                case KeyEvent.VK_F9:
                    if (TESTING(PokerConstants.TESTING_AUTOPILOT_INIT))
                    {
                        TOGGLE(PokerConstants.TESTING_AUTOPILOT);
                        if (TESTING(PokerConstants.TESTING_AUTOPILOT))
                        {
                            EngineUtils.displayInformationDialog(context_, "Autopilot will resume with next deal.");
                        }
                        else
                        {
                            EngineUtils.displayInformationDialog(context_, "Autopilot paused.");
                        }
                        return true;
                    }
                    break;
            }
        }

        return super.handleKeyPressed(event);
    }

    private boolean isFocusInChat()
    {
        return chat_.hasFocus() || OnlineLobby.hasFocus();
    }

    /**
     * label for bet amount
     */
    private class PokerIcon extends ImageComponent
    {
        public PokerIcon(int nPrefW, int nPrefH,
                         double x, double y, double scale)
        {
            super("icon-fold", 1.0d);
            super.setScaleToFit(true);
            setHidden(true);
            setPreferredSize(new Dimension(nPrefW, nPrefH));
            addMouseMotionListener(mouseTrans_);
            addMouseListener(mouseTrans_);
            ScaleConstraints sc = new ScaleConstraints(x, y, scale, getFont());
            board_.add(this, sc);
            //setBorder(GuiUtils.GREENBORDER); // TESTING
        }
    }

    /**
     * label for result of hand
     */
    private class PokerResult extends DDLabel
    {
        public PokerResult(int nPrefW, int nPrefH,
                           double x, double y, double scale, int nSeat)
        {
            super(GuiManager.DEFAULT, "PokerResult");
            setName("PokerResult " + nSeat);

            // tweak numbers to widen label for resizing allowances
            double adj = scale * .10d;
            x -= (adj * .90);
            scale += (adj * 2);
            nPrefW += (nPrefW * .22d);

            // setup
            setHorizontalAlignment(SwingConstants.CENTER);
            setPreferredSize(new Dimension(nPrefW, nPrefH));
            addMouseMotionListener(mouseTrans_);
            addMouseListener(mouseTrans_);
            ScaleConstraints sc = new ScaleConstraints(x, y, scale, getFont());
            board_.add(this, sc);
            //setBorder(GuiUtils.REDBORDER); // TESTING
        }
    }

    /**
     * label for bet amount
     */
    private class PokerBetArea extends DDFastLabel
    {
        public PokerBetArea(int nPrefW, int nPrefH,
                            double x, double y,
                            double scale,
                            int nSeat)
        {
            super(GuiManager.DEFAULT, "PokerTable");
            setName("PokerBetArea " + nSeat);
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.TOP);
            setPreferredSize(new Dimension(nPrefW, nPrefH));
            addMouseMotionListener(mouseTrans_);
            addMouseListener(mouseTrans_);
            ScaleConstraints sc = new ScaleConstraints(x, y, scale, getFont());
            board_.add(this, sc);
            //setBorder(GuiUtils.REDBORDER); // TESTING
        }
    }

    /**
     * button actions, delegate
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof DDButton)
        {
            DDButton button = (DDButton) e.getSource();

            int action = button.getActionID();

            if (action != 0)
            {
                game_.playerActionPerformed(action, amount_.getValue());
            }
        }
    }

    ////
    //// PokerGameboardDelegate
    ////

    /**
     * transfer focus to continue button or amount spinner
     */
    @Override
    public boolean processRequestFocus()
    {
        if (buttonContinueMiddle_ != null && buttonContinueMiddle_.isVisible())
        {
            buttonContinueMiddle_.requestFocus();
            return true;
        }
        if (buttonContinueLower_ != null && buttonContinueLower_.isVisible())
        {
            buttonContinueLower_.requestFocus();
            return true;
        }
        else if (amountPanel_ != null && amountPanel_.isEnabled() && !amountPanel_.isSoonDisabled() &&
                 amountPanel_.isVisible() && !amount_.hasFocus())
        {
            amount_.requestFocus();
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Game Piece listener to repaint cards when mouse if over
     */
    private class PokerGamePieceListener implements GamePieceSelectionListener
    {
        public boolean allowGamePieceSelection(EngineGamePiece gp, MouseEvent e)
        {
            return false;
        }

        public void gamePieceSelected(EngineGamePiece gp, MouseEvent e)
        {
        }

        public void gamePieceDeselected(EngineGamePiece gp, MouseEvent e)
        {
        }

        public void mouseEntered(Gameboard g, EngineGamePiece gp)
        {
            checkCard(g, gp, true);
        }

        public void mouseExited(Gameboard g, EngineGamePiece gp)
        {
            // if there is another piece under the mouse,
            // don't check card as a mouseEntered event will
            // follow this.
            if (g.getGamePieceUnderMouse() != null) return;

            checkCard(g, gp, false);
        }

        private void checkCard(Gameboard g, EngineGamePiece gp, boolean bVisible)
        {
            if (gp instanceof CardPiece)
            {
                Territory t = gp.getTerritory();
                if (t == null) return; // could happen when cards are removed

                CardPiece card = (CardPiece) gp;
                if (card.isFolded()) return;

                GameEngine engine = GameEngine.getGameEngine();
                boolean bAIPeek = PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_MOUSEOVER);
                boolean bHoleFaceDown = PokerUtils.isOptionOn(PokerConstants.OPTION_HOLE_CARDS_DOWN);

                PokerPlayer player = card.getPokerPlayer();
                if (!card.isUp() && (bAIPeek || (player.isHuman() && bHoleFaceDown && player.isLocallyControlled())))
                {
                    synchronized (t.getMap())
                    {
                        List<GamePiece> cards = EngineUtils.getMatchingPieces(t, gp.getType());
                        for (GamePiece piece : cards)
                        {
                            ((CardPiece) piece).setTemporarilyVisible(bVisible);
                        }
                        g.repaintTerritory(t);
                    }
                }
            }
        }
    }

    protected void createTestCase()
    {
        TypedHashMap params = new TypedHashMap();
        context_.processPhaseNow("CreateTestCase", params);
    }


    ////
    //// TerritorySelectionListener
    ////

    public void mouseEntered(Gameboard g, Territory t)
    {
    }

    public void mouseExited(Gameboard g, Territory t)
    {
    }

    /**
     * Determine which territories we create menus for
     */
    public boolean allowTerritorySelection(Territory t, MouseEvent e)
    {
        return GuiUtils.isPopupTrigger(e, !PokerUtils.isOptionOn(PokerConstants.OPTION_RIGHT_CLICK_ONLY));
    }

    // menu
    private DDPopupMenu menu_ = null;
    private long lastDismissed_ = 0;
    private Territory lastTerritory_ = null;
    //private int POPUP_CNT = 0;

    /**
     * Create popup
     */
    public void territorySelected(Territory t, MouseEvent e)
    {
        DDPopupMenu menu = null;

        boolean bShowCheatItems = PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_POPUP) &&
                                  (!game_.isOnlineGame() || TESTING(PokerConstants.TESTING_ALLOW_CHEAT_ONLINE));

        GamePiece gamePiece = PokerUtils.getPokerGameboard().getGamePieceUnderMouse();
        CardPiece cardPiece = (gamePiece instanceof CardPiece) ? (CardPiece) gamePiece : null;

        // don't show new popup if just recently dismissed one, or if
        // in same territory over a card
        if (System.currentTimeMillis() - lastDismissed_ > 200 ||
            (t == lastTerritory_ && cardPiece != null))
        {
            // initialize
            Point point = board_.getLastMousePoint();
            String sStyle = "PokerTable";
            PokerPlayer p = PokerUtils.getPokerPlayer(context_, t);
            HoldemHand hhand = table_.getHoldemHand();
            boolean bInHand = hhand != null && hhand.getRound() != HoldemHand.ROUND_SHOWDOWN;

            if (PokerUtils.isPot(t) || PokerUtils.isFlop(t) || p == null)
            {
                // TODO: deal when auto-deal on?
                if ((buttonDeal_ != null && buttonDeal_.isEnabled()) || (cardPiece != null))
                {
                    menu = new DDPopupMenu();

                    DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, sStyle);
                    title.setText(PropertyConfig.getMessage("menuitem.nonplayer.title"));
                    title.setDisplayMode(DDMenuItem.MODE_TITLE);

                    // add menu items
                    menu.add(title);

                    if (bShowCheatItems)
                    {
                        if (cardPiece != null && bInHand)
                        {
                            menu.add(new ChangeCard(sStyle, t, point, cardPiece));
                        }

                        // THERE ARE SOME DEFINITE ISSUES WITH ALLOWING THIS, INCLUDING MIN CHIP STUFF,
                        // AND THE SHEER SIZE OF THE MENU, SO IT'S JUST IMPLEMENTED FOR TESTING PURPOSES RIGHT NOW
                        if (TESTING(PokerConstants.TESTING_ALLOW_CHANGE_LEVEL) &&
                            (table_.getHoldemHand() == null || table_.getHoldemHand().getRound() == HoldemHand.ROUND_SHOWDOWN))
                        {
                            menu.add(new ChangeBlinds(sStyle, point));
                        }
                    }

                    if (buttonDeal_ != null && buttonDeal_.isEnabled() && buttonDeal_.isVisible())
                    {
                        menu.add(new Deal(sStyle));
                    }
                }
            }
            else
            {
                menu = new DDPopupMenu();

                // create titles
                TerritoryMenuItem title = new TerritoryMenuItem(sStyle, t);
                title.setText(PropertyConfig.getMessage("menuitem.player.title",
                                                        Utils.encodeHTML(title.player.getName()),
                                                        title.player.getSeat() + 1));
                title.setDisplayMode(DDMenuItem.MODE_TITLE);

                // add menu items
                menu.add(title);

                int nSeat = PokerUtils.getTableSeatForTerritory(table_, t);

                if (game_.isOnlineGame() && p.isHuman() && p.isLocallyControlled())
                {
                    menu.add(new SitOut(sStyle, t, !p.isSittingOut()));
                }

                if (game_.isOnlineGame() && p.isHuman() && !p.isLocallyControlled())
                {
                    PokerPrefsPlayerList muted = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_MUTE);
                    PokerPrefsPlayerList banned = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_BANNED);
                    menu.add(new MutePlayer(sStyle, PokerUtils.getPokerPlayer(context_, t), muted.containsPlayer(p.getName(), p.getKey()), TD(), false));
                    menu.add(new BanPlayer(context_, sStyle, PokerUtils.getPokerPlayer(context_, t), banned.containsPlayer(p.getName(), p.getKey()), null, TD(), false, false));
                }

                if (bShowCheatItems)
                {
                    if (p.isInHand())
                    {
                        if (cardPiece != null && (cardPiece.isUp() || cardPiece.isTemporarilyVisible()))
                        {
                            menu.add(new ChangeCard(sStyle, t, point, cardPiece));
                        }
                    }
                }

                TournamentProfile profile = game_.getProfile();
                boolean bDashAdvisor = profile.isAllowAdvisor();

                if (!game_.isOnlineGame() || bDashAdvisor)
                {
                    // AI players
                    if (!p.isHumanControlled())
                    {
                        if (bShowCheatItems) menu.add(new SelectPlayerType(sStyle, t, point));
                    }
                    // locally controlled players
                    else if (p.isLocallyControlled()) menu.add(new SelectAdvisorType(sStyle, t, point));
                }

                if (bShowCheatItems)
                {
                    if (!p.isHuman())
                    {
                        menu.add(new ChangePlayerName(sStyle, t));
                    }

                    menu.add(new ChangeChipCount(sStyle, t));
                    menu.add(new MoveButton(sStyle, t, table_.getButton() != nSeat));

                    if (p.isComputer() &&
                        (table_.getHoldemHand() == null || table_.getHoldemHand().getRound() == HoldemHand.ROUND_SHOWDOWN))
                    {
                        menu.add(new RemovePlayer(sStyle, t));
                    }
                }

                if (buttonDeal_ != null && buttonDeal_.isEnabled() && buttonDeal_.isVisible())
                {
                    menu.add(new Deal(sStyle));
                }
            }

            // show menu
            if (menu != null)
            {
                //menu.setName("Popup " + (++POPUP_CNT)); // for debugging
                lastTerritory_ = t;

                // showmenu
                showMenu(menu, point.x, point.y);
            }

        }

        // if nothing to show, request focus
        if (menu == null)
        {
            if (!amount_.hasFocus()) board_.requestFocus();
        }

        // deslect so use can click again
        board_.setSelectedTerritory(null);
    }

    ////
    //// popup menu listener
    ////

    private void showMenu(DDPopupMenu menu, int x, int y)
    {
        menu_ = menu;

        // add listener and pause TD if practice since
        // these cheat options can affect game and shouldn't
        // happen when TD is running
        if (!game_.isOnlineGame()) TD().setPaused(true);
        menu_.addPopupMenuListener(this);

        // show popup
        menu_.show(board_, x, y);
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        lastDismissed_ = System.currentTimeMillis();
        menu_ = null;
        if (!game_.isOnlineGame()) TD().setPaused(false);

        // after popup goes away, send focus back to
        // board (need to invoke later because at this
        // time popup is still visible)
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                // don't request if 2nd popup displayed
                if (menu_ != null) board_.requestFocus();
            }
        });
    }

    public void popupMenuCanceled(PopupMenuEvent e)
    {
    }

    ////
    //// popup menu items
    ////

    private static ImageIcon blankIcon_ = ImageConfig.getImageIcon("menuicon.blank");
    private static ImageIcon moneyIcon_ = ImageConfig.getImageIcon("menuicon.money");
    private static ImageIcon buttonIcon_ = ImageConfig.getImageIcon("menuicon.button");
    private static ImageIcon dealIcon_ = ImageConfig.getImageIcon("menuicon.deal");
    private static ImageIcon checkedIcon_ = ImageConfig.getImageIcon("menuicon.checked");
    private static ImageIcon cardIcon_ = ImageConfig.getImageIcon("menuicon.card");
    private static ImageIcon playertypeIcon_ = ImageConfig.getImageIcon("menuicon.playertype");
    private static ImageIcon advisorIcon_ = ImageConfig.getImageIcon("menuicon.advisor");
    private static ImageIcon playerNameIcon_ = ImageConfig.getImageIcon("menuicon.playername");
    private static ImageIcon removePlayerIcon_ = ImageConfig.getImageIcon("menuicon.removeplayer");
    private static ImageIcon sitoutIcon_ = ImageConfig.getImageIcon("menuicon.sitout");
    private static ImageIcon banIcon_ = ImageConfig.getImageIcon("menuicon.ban");
    private static ImageIcon unbanIcon_ = ImageConfig.getImageIcon("menuicon.unban");
    private static ImageIcon muteIcon_ = ImageConfig.getImageIcon("menuicon.mute");
    private static ImageIcon unmuteIcon_ = ImageConfig.getImageIcon("menuicon.unmute");

    /**
     * Class used to track what the menu item does
     */
    private class TerritoryMenuItem extends DDMenuItem
    {
        Territory t;
        PokerPlayer player;

        TerritoryMenuItem(String sStyle, Territory t)
        {
            super(GuiManager.DEFAULT, sStyle);
            this.t = t;
            if (t != null) player = PokerUtils.getPokerPlayer(context_, t);
            setDisplayMode(DDMenuItem.MODE_NORMAL);
        }
    }

    /**
     * change player name menu item
     */
    private class ChangePlayerName extends TerritoryMenuItem implements ActionListener
    {
        ChangePlayerName(String sStyle, Territory t)
        {
            super(sStyle, t);
            setText(PropertyConfig.getMessage("menuitem.playername"));
            setIcon(playerNameIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            String sOldName = player.getName();
            TypedHashMap params = new TypedHashMap();
            params.setObject(ChangeChipCountDialog.PARAM_PLAYER, player);
            context_.processPhaseNow("ChangePlayerNameDialog", params);

            if (!player.getName().equals(sOldName))
            {
                board_.repaintAll(); // could affect amount to call for active non-active player, so repaint all
                PokerDatabase.playerNameChanged(game_, player);
            }
        }
    }

    /**
     * change chip count menu item
     */
    private class ChangeChipCount extends TerritoryMenuItem implements ActionListener
    {
        ChangeChipCount(String sStyle, Territory t)
        {
            super(sStyle, t);
            setText(PropertyConfig.getMessage("menuitem.chipcount"));
            setIcon(moneyIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            int nOldChip = player.getChipCount();
            TypedHashMap params = new TypedHashMap();
            params.setObject(ChangeChipCountDialog.PARAM_PLAYER, player);
            context_.processPhaseNow("ChangeChipCountDialog", params);

            if (player.getChipCount() != nOldChip)
            {
                board_.repaintAll(); // could affect amount to call for active non-active player, so repaint all
                HoldemHand hhand = table_.getHoldemHand();
                if (hhand != null)
                {
                    setInputMode(MODE_RECHECK, hhand, hhand.getCurrentPlayer());
                }
                table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED, table_, player, player.getSeat()));
            }
        }
    }

    /**
     * remove player menu item
     */
    private class RemovePlayer extends TerritoryMenuItem implements ActionListener
    {
        RemovePlayer(String sStyle, Territory t)
        {
            super(sStyle, t);
            setText(PropertyConfig.getMessage("menuitem.removeplayer"));
            setIcon(removePlayerIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            if (EngineUtils.displayConfirmationDialog(context_, PropertyConfig.getMessage("msg.confirm.remove",
                                                                                          Utils.encodeHTML(player.getName())), "removeplayer"))
            {
                PokerGame game = (PokerGame) context_.getGame();
                int chips = player.getChipCount();
                player.setChipCount(0);
                int nSeat = player.getSeat();
                table_.removePlayer(nSeat);
                game.playerOut(player);
                game.addExtraChips(-chips);
                Territory seat = PokerUtils.getTerritoryForTableSeat(table_, nSeat);
                synchronized (seat.getMap())
                {
                    List<GamePiece> pieces = EngineUtils.getMatchingPieces(seat, PokerConstants.PIECE_CARD);
                    for (GamePiece piece : pieces)
                    {
                        seat.removeGamePiece(piece);
                    }
                }
                ResultsPiece piece = (ResultsPiece) seat.getGamePiece(PokerConstants.PIECE_RESULTS, null);
                if (piece != null)
                {
                    piece.setResult(ResultsPiece.HIDDEN, "");
                }
                board_.repaintAll();
            }
        }
    }

    /**
     * select player type menu item
     */
    private class SelectPlayerType extends TerritoryMenuItem implements ActionListener
    {
        Point point_;
        String text_;
        boolean bAdvisor_;

        SelectPlayerType(String sStyle, Territory t, Point point)
        {
            this(sStyle, t, point, false);
        }

        SelectPlayerType(String sStyle, Territory t, Point point, boolean bAdvisor)
        {
            super(sStyle, t);
            point_ = point;
            bAdvisor_ = bAdvisor;
            text_ = PropertyConfig.getMessage("menuitem.playertype", player.getPokerAI().getPlayerType().getName());
            setText(text_);
            setIcon(playertypeIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            selectPlayerType(point_, player, text_, bAdvisor_);
        }
    }

    /**
     * Select player type menu
     */
    private void selectPlayerType(Point point, PokerPlayer player, String sTitle, boolean bAdvisor)
    {
        String sStyle = "PokerTable";

        DDPopupMenu menu = new DDPopupMenu();

        DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, sStyle);
        title.setText(PropertyConfig.getMessage("menuitem.player.title2", sTitle, Utils.encodeHTML(player.getName()), player.getSeat() + 1));
        title.setDisplayMode(DDMenuItem.MODE_TITLE);
        menu.add(title);

        List<BaseProfile> playerTypes = PlayerType.getProfileList();

        Collections.sort(playerTypes);
        PlayerType playerType;
        String aiClassName;

        for (BaseProfile profile : playerTypes)
        {
            playerType = (PlayerType) profile;
            aiClassName = playerType.getAIClassName();

            if (!bAdvisor || ((aiClassName != null) && (aiClassName.endsWith("V2Player"))))
            {
                menu.add(new SetPlayerType(sStyle, player, playerType, bAdvisor));
            }
        }

        showMenu(menu, point.x, point.y);
    }

    /**
     * select advisor type menu item
     */
    private class SelectAdvisorType extends SelectPlayerType
    {
        SelectAdvisorType(String sStyle, Territory t, Point point)
        {
            super(sStyle, t, point, true);
            text_ = PropertyConfig.getMessage("menuitem.advisortype", player.getPokerAI().getPlayerType().getName());
            setText(text_);
            setIcon(advisorIcon_);
        }
    }

    /**
     * set player type menu item
     */
    private class SetPlayerType extends DDMenuItem implements ActionListener
    {
        boolean bAdvisor_;
        PokerPlayer player_;
        PlayerType playerType_;

        SetPlayerType(String sStyle, PokerPlayer player, PlayerType playerType, boolean bAdvisor)
        {
            super(GuiManager.DEFAULT, sStyle);
            bAdvisor_ = bAdvisor;
            player_ = player;
            playerType_ = playerType;
            setText(playerType_.getName());
            PokerAI ai = (PokerAI) player_.getGameAI();
            PlayerType previous = ai.getPlayerType();
            boolean checked;

            if (bAdvisor)
            {
                checked = (!PlayerType.getAdvisorFile().exists() &&
                           playerType_.getUniqueKey().equals(PlayerType.getAdvisorKey()));
            }
            else
            {
                checked = (previous != null) && playerType_.getUniqueKey().equals(previous.getUniqueKey());
            }

            if (checked)
            {
                setIcon(checkedIcon_);
            }
            else
            {
                setIcon(blankIcon_);
            }
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            if (bAdvisor_)
            {
                player_.setPlayerType(PlayerType.setAdvisor(playerType_));
            }
            else
            {
                player_.setPlayerType(playerType_);
            }

            // TODO: does this belong here or in setPokerAI?
            table_.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_AI_CHANGED, table_, player_, player_.getSeat()));
        }
    }

    /**
     * select player cards
     */
    private class ChangeCard extends TerritoryMenuItem implements ActionListener
    {
        String text_;
        CardPiece cardPiece_;
        Point point_;

        ChangeCard(String sStyle, Territory t, Point point, CardPiece cardPiece)
        {
            super(sStyle, t);
            point_ = point;
            text_ = PropertyConfig.getMessage("menuitem.changecard",
                                              cardPiece.getCard().getDisplay());
            cardPiece_ = cardPiece;
            setText(text_);
            setIcon(cardIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            selectCard(point_, player, cardPiece_, text_);
        }
    }

    private void selectCard(Point point, final PokerPlayer player, final CardPiece cardPiece, String sTitle)
    {
        String sStyle = "PokerTable";

        DDPopupMenu menu = new DDPopupMenu();

        DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, sStyle);
        title.setText(PropertyConfig.getMessage("menuitem.changecard.title", sTitle,
                                                (player == null) ? PropertyConfig.getMessage("menuitem.community") : Utils.encodeHTML(player.getName())));
        title.setDisplayMode(DDMenuItem.MODE_TITLE);
        menu.add(title);

        CardSelectorPanel cardSelector = new CardSelectorPanel(cardPiece.getCard(), false);

        cardSelector.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                CardSelectorPanel cs = (CardSelectorPanel) e.getSource();
                Card selectedCard = cs.getSelectedCard();

                if (selectedCard != null && !selectedCard.equals(cardPiece.getCard()) &&
                    !selectedCard.equals(Card.BLANK))
                {
                    HoldemHand hhand = table_.getHoldemHand();
                    Hand community = hhand.getCommunity();
                    Deck deck = hhand.getDeck();

                    Hand hand = null;
                    Territory t = null;

                    // figure out if the selected card is already in play

                    int cardIndex = community.indexOf(selectedCard);

                    if (cardIndex >= 0)
                    {
                        hand = community;
                        t = PokerUtils.getFlop();
                    }
                    else
                    {
                        Hand muck = hhand.getMuck();

                        cardIndex = muck.indexOf(selectedCard);

                        if (cardIndex >= 0)
                        {
                            hand = muck;
                        }
                        else
                        {
                            for (int seat = 0; seat < 10; ++seat)
                            {
                                PokerPlayer pp = table_.getPlayer(seat);

                                if (pp != null)
                                {
                                    if (pp.getHand() != null)
                                    {
                                        cardIndex = pp.getHand().indexOf(selectedCard);

                                        if (cardIndex >= 0)
                                        {
                                            hand = pp.getHand();
                                            t = PokerUtils.getTerritoryForTableSeat(table_, pp.getSeat());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // if the selected card is in play, replace it with a new card from the deck
                    if ((hand != null) && (cardIndex >= 0))
                    {
                        hand.setCard(cardIndex, deck.nextCard());
                        board_.repaintTerritory(t);
                    }
                    // card not in play, in the deck, so remove it from the deck (yeah, sam, think of this case?)
                    else
                    {
                        deck.removeCard(selectedCard);
                    }

                    cardIndex = cardPiece.getCardIndex();

                    hand = (player == null) ? community : player.getHand();

                    // put the card we're replacing back in the deck
                    deck.addRandom(hand.getCard(cardIndex));

                    // set the new cards
                    hand.setCard(cardIndex, selectedCard);

                    if (hhand != null && hhand.isAllInShowdown())
                    {
                        // if we are changing a card after all have been displayed (when pause after
                        // deal is on, need to do showdown based on actual comm cards)
                        int n = EngineUtils.getMatchingPiecesCount(PokerUtils.getFlop(), PokerConstants.PIECE_CARD);
                        Showdown.displayAllin(hhand, n == 5);
                    }
                }

                menu_.setVisible(false);
                board_.repaintTerritory(cardPiece.getTerritory());

                table_.firePokerTableEvent(new PokerTableEvent(
                        PokerTableEvent.TYPE_CARD_CHANGED, table_, player,
                        (player != null) ? player.getSeat() : PokerTableEvent.NOT_DEFINED));
            }
        });

        menu.add(GuiUtils.CENTER(cardSelector));

        showMenu(menu, point.x, point.y);
    }

    /**
     * move button menu item
     */
    private class MoveButton extends TerritoryMenuItem implements ActionListener
    {
        boolean bMove;

        MoveButton(String sStyle, Territory t, boolean bMove)
        {
            super(sStyle, t);
            this.bMove = bMove;
            setText(PropertyConfig.getMessage(bMove ? "menuitem.button" : "menuitem.button.no"));
            setIcon(buttonIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            if (bMove)
            {
                table_.setButton(PokerUtils.getTableSeatForTerritory(table_, t));
            }

            // if button moved at end of a hand, don't move it when
            // next hand starts because player likely wanted button
            // to start there for next hand
            HoldemHand hhand = table_.getHoldemHand();
            if (hhand != null && hhand.getRound() == HoldemHand.ROUND_SHOWDOWN)
            {
                table_.setSkipNextButtonMove(true);
            }
        }
    }

    /**
     * sitout menu item
     */
    private class SitOut extends TerritoryMenuItem implements ActionListener
    {
        boolean bSitout;

        SitOut(String sStyle, Territory t, boolean bSitout)
        {
            super(sStyle, t);
            this.bSitout = bSitout;
            setText(PropertyConfig.getMessage(bSitout ? "menuitem.sitout" : "menuitem.sitout.no"));
            setIcon(sitoutIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            if (PokerUtils.isDemoOver(context_, player, true) && !bSitout)
            {
                EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.onlinedone.demo"));
                return;
            }

            player.setSittingOut(bSitout);
            TD().playerUpdate(player, player.getOnlineSettings());
        }
    }

    /**
     * mute menu item
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class MutePlayer extends DDMenuItem implements ActionListener
    {
        ChatManager chat;
        boolean bMuted;
        boolean bFromLobby;
        PokerPlayer player;

        public MutePlayer(String sStyle, PokerPlayer p, boolean bMuted, ChatManager chat, boolean bFromLobby)
        {
            super(GuiManager.DEFAULT, sStyle);
            this.bMuted = bMuted;
            this.bFromLobby = bFromLobby;
            this.chat = chat;
            this.player = p;
            setDisplayMode(DDMenuItem.MODE_NORMAL);
            String E = bFromLobby ? ".2" : "";
            setText(PropertyConfig.getMessage(bMuted ? "menuitem.muteplayer.un" + E : "menuitem.muteplayer" + E,
                                              player.getName()));
            setIcon(bMuted ? unmuteIcon_ : muteIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            PokerPrefsPlayerList muted = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_MUTE);
            if (bMuted) muted.remove(player.getName(), true);
            else muted.add(player.getName(), player.getKey(), true);

            chat.deliverChatLocal(PokerConstants.CHAT_ALWAYS,
                                  PropertyConfig.getMessage(bMuted ? "msg.chat.unmuted" : "msg.chat.muted",
                                                            Utils.encodeHTML(player.getName())),
                                  bFromLobby ? OnlineMessage.CHAT_DIRECTOR_MSG_ID :
                                  OnlineMessage.CHAT_DEALER_MSG_ID);
        }
    }

    /**
     * ban menu item
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class BanPlayer extends DDMenuItem implements ActionListener
    {
        GameContext context;
        OnlineManager mgr;
        ChatManager chat;
        PokerPlayer player;
        boolean bFromLobby;
        boolean bBanned;
        boolean bBanNow;

        public BanPlayer(GameContext context, String sStyle, PokerPlayer p, boolean bBanned, OnlineManager mgr, ChatManager chat,
                         boolean bFromLobby, boolean bBanNow)
        {
            super(GuiManager.DEFAULT, sStyle);
            this.context = context;
            this.player = p;
            this.bBanned = bBanned;
            this.bFromLobby = bFromLobby;
            this.mgr = mgr;
            this.chat = chat;
            this.bBanNow = bBanNow;
            setDisplayMode(DDMenuItem.MODE_NORMAL);
            String E = bFromLobby ? ".2" : "";
            setText(PropertyConfig.getMessage(bBanned ? "menuitem.banplayer.un" + E : "menuitem.banplayer" + E,
                                              p.getName()));
            setIcon(bBanned ? unbanIcon_ : banIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            // confirm
            if (!bBanNow || EngineUtils.displayConfirmationDialog(context, PropertyConfig.getMessage("msg.confirm.ban",
                                                                                                     Utils.encodeHTML(player.getName()))))
            {
                PokerPrefsPlayerList banned = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_BANNED);
                if (bBanned) banned.remove(player.getName(), true);
                else banned.add(player.getName(), player.getKey(), true);

                chat.deliverChatLocal(PokerConstants.CHAT_ALWAYS, PropertyConfig.getMessage(
                        bBanNow ? "msg.chat.banned" :
                        (bBanned ? "msg.chat.unbanned.ingame" : "msg.chat.banned.ingame"),
                        Utils.encodeHTML(player.getName())),
                                      bFromLobby ? OnlineMessage.CHAT_DIRECTOR_MSG_ID :
                                      OnlineMessage.CHAT_DEALER_MSG_ID);

                if (bBanNow)
                {
                    mgr.banPlayer(player);
                }
            }
        }
    }

    /**
     * move button menu item
     */
    private class Deal extends DDMenuItem implements ActionListener
    {
        Deal(String sStyle)
        {
            super(GuiManager.DEFAULT, sStyle);
            setText(PropertyConfig.getMessage("menuitem.deal"));
            setIcon(dealIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            deal();
        }
    }

    /**
     * change blinds menu item
     */
    private class ChangeBlinds extends DDMenuItem implements ActionListener
    {
        Point point_;

        ChangeBlinds(String sStyle, Point point)
        {
            super(GuiManager.DEFAULT, sStyle);
            point_ = point;
            setText(PropertyConfig.getMessage("menuitem.changeblinds"));
            setIcon(moneyIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            selectBlinds(point_);
        }
    }

    /**
     * Select player type menu
     */
    private void selectBlinds(Point point)
    {
        String sStyle = "PokerTable";

        DDPopupMenu menu = new DDPopupMenu();

        DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, sStyle);
        title.setText(PropertyConfig.getMessage("menuitem.changeblinds"));
        title.setDisplayMode(DDMenuItem.MODE_TITLE);
        menu.add(title);

        TournamentProfile profile = table_.getProfile();

        int levels = profile.getLastLevel();

        for (int i = 1; (i <= levels) ||
                        (i <= TournamentProfile.MAX_LEVELS && profile.isDoubleAfterLastLevel() &&
                         (profile.getBigBlind(i) * 4 < game_.getTotalChipsInPlay())); ++i)
        {
            if (profile.isBreak(i)) continue;
            menu.add(new SetBlinds(sStyle, i));
        }

        showMenu(menu, point.x, point.y);
    }

    private class SetBlinds extends DDMenuItem implements ActionListener
    {
        int level_;

        SetBlinds(String sStyle, int level)
        {
            super(GuiManager.DEFAULT, sStyle);
            level_ = level;
            TournamentProfileHtml html = new TournamentProfileHtml(table_.getProfile());
            setText(html.getBlindsText("msg.menu.", level, true));

            if (level == game_.getLevel())
            {
                setIcon(checkedIcon_);
            }
            else
            {
                setIcon(blankIcon_);
            }

            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            game_.changeLevel(level_ - game_.getLevel());
            table_.levelCheck(game_);
        }
    }

    ////
    //// mouse translation
    ////

    private MouseTranslator mouseTrans_ = new MouseTranslator();

    /**
     * mouse motion/click translation - need for mouse motion/click handling
     * by board for widgets that obscure it
     */
    private class MouseTranslator extends MouseInputAdapter
    {
        ////
        //// Mouse Listener methods
        ////

        /**
         * Finds items under mouse
         */
        @Override
        public void mousePressed(MouseEvent e)
        {
            Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), board_);
            board_.mousePressed(new MouseEvent(board_, e.getID(), e.getWhen(), e.getModifiersEx(),
                                               p.x, p.y, e.getClickCount(), e.isPopupTrigger()));
        }

        /**
         * Finds items under mouse
         */
        @Override
        public void mouseReleased(MouseEvent e)
        {
            Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), board_);
            board_.mouseReleased(new MouseEvent(board_, e.getID(), e.getWhen(), e.getModifiersEx(),
                                                p.x, p.y, e.getClickCount(), e.isPopupTrigger()));
        }

        ////
        //// Mouse Motion Listener methods
        ////

        /**
         * Finds items under mouse
         */
        @Override
        public void mouseDragged(MouseEvent e)
        {
            Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), board_);
            board_.mouseDragged(new MouseEvent(board_, e.getID(), e.getWhen(), e.getModifiersEx(),
                                               p.x, p.y, e.getClickCount(), e.isPopupTrigger()));
        }

        /**
         * Finds items under mouse
         */
        @Override
        public void mouseMoved(MouseEvent e)
        {
            Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), board_);
            board_.mouseMoved(new MouseEvent(board_, e.getID(), e.getWhen(), e.getModifiersEx(),
                                             p.x, p.y, e.getClickCount(), e.isPopupTrigger()));
        }
    }
}
