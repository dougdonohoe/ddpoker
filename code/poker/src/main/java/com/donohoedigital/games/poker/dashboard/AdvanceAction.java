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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.DiceRoller;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 20, 2005
 * Time: 9:18:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdvanceAction extends DashboardItem implements ActionListener
{
    private static AdvanceAction impl_; // TODO store in PokerGame or PokerContext

    private DDPanel cheatbase_;
    private ArrayList buttons_ = new ArrayList();
    private DDLabel label_;
    private Advance checkfold_;
    private Advance call_;
    private Advance bet_;
    private Advance raise_;
    private Advance betpot_;
    private Advance raisepot_;
    private Advance allin_;
    private Advance autopilot_;
    private int nCallAmount_ = 0;
    private boolean bObserverSet_ = false;
    private boolean bShowingLabel_ = false;
    private boolean bHumanActing_ = false;

    /**
     * Cheat dash list
     */
    public AdvanceAction(GameContext context)
    {
        super(context, "advance");
        impl_ = this;
        trackTableEvents(PokerTableEvent.TYPE_PLAYER_ACTION |
                         PokerTableEvent.TYPE_NEW_HAND |
                         PokerTableEvent.TYPE_DEALER_ACTION);
    }

    /**
     * finish - clear impl_
     */
    public void finish()
    {
        impl_ = null;
    }

    /**
     * create list of options
     */
    protected JComponent createBody()
    {
        cheatbase_ = new DDPanel();
        cheatbase_.setLayout(new DDGridLayout(0,1,0,-8));

        label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        if (!game_.isOnlineGame() || game_.getProfile().isAllowAdvisor())
        {
            autopilot_ = new Advance("autopilot");
        }
        checkfold_ = new Advance("checkfold");
        call_ = new Advance("call");
        bet_ = new Advance("bet");
        raise_ = new Advance("raise");
        betpot_ = new Advance("betpot");
        raisepot_ = new Advance("raisepot");
        allin_ = new Advance("allin");

        return cheatbase_;
    }

    /**
     * our version of button group, but we allow unselecting of selected choice
     */
    public void actionPerformed(ActionEvent e)
    {
        Advance source = (Advance) e.getSource();
        Advance other;
        for (int i = buttons_.size() -1; i >= 0; i--)
        {
            other = (Advance) buttons_.get(i);
            if (other != source && other.isSelected())
            {
                other.setSelected(false);
            }
        }
    }

    /**
     * checkbox
     */
    private class Advance extends DDCheckBox
    {
        Advance(String sName)
        {
            super(sName, "DashSmaller");
            buttons_.add(this);
            addActionListener(AdvanceAction.this);
            setDisabledTextColor(Color.lightGray);
        }
    }

    /**
     * handle changes to table by repainting as appropriate
     */
    public void tableEventOccurred(PokerTableEvent event)
    {
        if (event.getType() == PokerTableEvent.TYPE_NEW_HAND ||
            event.getType() == PokerTableEvent.TYPE_DEALER_ACTION)
        {
            if (isDisplayed()) clearButtons();
        }

        PokerTable table = event.getTable();
        if (!table.isZipMode())
        {
            super.tableEventOccurred(event);
        }
    }

    /**
     * new hand
     */
    private void clearButtons()
    {
        Advance other;
        for (int i = buttons_.size() -1; i >= 0; i--)
        {
            other = (Advance) buttons_.get(i);
            if (other != autopilot_ && other.isSelected())
            {
                other.setSelected(false);
            }
        }
    }

    /**
     * Clear components
     */
    private void removeComponents()
    {
        cheatbase_.removeAll();
    }

    /**
     * udpate display
     */
    protected void updateInfo()
    {
        if (!isOpen()) return; // don't update if we aren't visible

        // if observing and already set message, nothing to update
        PokerPlayer human = game_.getHumanPlayer();
        if (bObserverSet_)
        {
            // if still observer, nothing to do
            if (human.isObserver()) return;

            // else remove components and continue on
            removeComponents();
            bObserverSet_ = false;
        }

        // init
        PokerTable table = game_.getCurrentTable();
        HoldemHand hhand = table.getHoldemHand();
        Hand hand = human.getHand();

        // if observer, just display message once
        if (human.isObserver())
        {
            bObserverSet_ = true;
            removeComponents();
            cheatbase_.add(label_);
            label_.setText(PropertyConfig.getMessage(human.isWaiting() ? "label.waiting.label":"label.observing.label"));
            return;
        }

        // if no current table hand or player hand (moved from another table),
        // just display a message
        if (hhand == null || hand == null)
        {
            if (!bShowingLabel_)
            {
                bShowingLabel_ = true;
                removeComponents();
                cheatbase_.add(label_);
                label_.setText(PropertyConfig.getMessage("msg.myhand.none"));
            }
            return;
        }

        // remove label if showing
        if (bShowingLabel_ || cheatbase_.getComponentCount() == 0)
        {
            // add buttons back
            removeComponents();
            Advance other;
            for (int i = buttons_.size() -1; i >= 0; i--)
            {
                other = (Advance) buttons_.get(i);
                cheatbase_.add(other,0);
            }
        }

        // get basic info
        boolean bNoAction = human.isFolded() || human.isAllIn() || bHumanActing_ ||
                                  hhand.getRound() == HoldemHand.ROUND_SHOWDOWN || hhand.getNumWithCards() == 1;
        int nToCall = 0;
        int nBet = 0;
        int nMaxRaise = 0;
        boolean bActedThisRound = false;

        if (!bNoAction)
        {
            nToCall = hhand.getCall(human);
            nBet = hhand.getBet();
            nMaxRaise = hhand.getMaxRaise(human);
            bActedThisRound = hhand.hasPlayerActed(human);
        }

        int nMode;
        int nGameType = hhand.getGameType();

        if (nToCall == 0)
        {
            if (nBet == 0)
            {
                nMode = PokerTableInput.MODE_CHECK_BET;
            }
            else
            {
                nMode = PokerTableInput.MODE_CHECK_RAISE;
            }
        }
        else
        {
            nMode = PokerTableInput.MODE_CALL_RAISE;
        }

        // checkfold_
        checkfold_.setEnabled(!bNoAction);

        // call_
        if (nMode == PokerTableInput.MODE_CALL_RAISE && !bNoAction)
        {
            if (nToCall != nCallAmount_)
            {
                nCallAmount_ = nToCall;
                call_.setSelected(false);
                call_.setText(PropertyConfig.getMessage("checkbox.call$.label", nCallAmount_));
            }
            call_.setEnabled(true);
        }
        else
        {
            nCallAmount_ = 0;
            call_.setSelected(false);
            call_.setText(PropertyConfig.getMessage("checkbox.call.label"));
            call_.setEnabled(false);
        }

        // bet_
        if (nGameType == PokerConstants.TYPE_LIMIT_HOLDEM)
        {
            bet_.setVisible(true);
            bet_.setEnabled(nMode == PokerTableInput.MODE_CHECK_BET && !bNoAction && !bActedThisRound);
            if (!bet_.isEnabled()) bet_.setSelected(false);
        }
        else
        {
            bet_.setVisible(false);
        }

        // raise_
        if (nGameType == PokerConstants.TYPE_LIMIT_HOLDEM)
        {
            raise_.setVisible(true);
            raise_.setEnabled(nMode != PokerTableInput.MODE_CHECK_BET && !bNoAction && nMaxRaise > 0);
            if (!raise_.isEnabled()) raise_.setSelected(false);
        }
        else
        {
            raise_.setVisible(false);
        }

        // betpot_
        if (nGameType == PokerConstants.TYPE_POT_LIMIT_HOLDEM)
        {
            betpot_.setVisible(true);
            betpot_.setEnabled(nMode == PokerTableInput.MODE_CHECK_BET && !bNoAction && !bActedThisRound);
            if (!betpot_.isEnabled()) betpot_.setSelected(false);
        }
        else
        {
            betpot_.setVisible(false);
        }

        // raisepot_
        if (nGameType == PokerConstants.TYPE_POT_LIMIT_HOLDEM)
        {
            raisepot_.setVisible(true);
            raisepot_.setEnabled(nMode != PokerTableInput.MODE_CHECK_BET && !bNoAction && nMaxRaise > 0);
            if (!raisepot_.isEnabled()) raisepot_.setSelected(false);
        }
        else
        {
            raisepot_.setVisible(false);
        }

        // allin_
        if (nGameType == PokerConstants.TYPE_NO_LIMIT_HOLDEM)
        {
            allin_.setVisible(true);
            allin_.setEnabled(!bNoAction);
        }
        else
        {
            allin_.setVisible(false);
        }
    }

    /**
     * Get action indicated by player and clear buttons
     */
    public static HandAction getAdvanceAction()
    {
        if (impl_ != null && impl_.buttons_.size() != 0)
        {
            HandAction action = impl_._getAdvanceAction();
            impl_.clearButtons();
            return action;
        }

        return null;
    }

    /**
     * get action indicated by player
     */
    private HandAction _getAdvanceAction()
    {
        PokerTable table = game_.getCurrentTable();
        HoldemHand hhand = table.getHoldemHand();
        int nRound = hhand.getRound();
        PokerPlayer human = game_.getHumanPlayer();
        int nAction;
        int nAmount = 0;

        if (autopilot_ != null && autopilot_.isSelected())
        {
            // online game, sleep random amount
            if (game_.isOnlineGame())
            {
                int nDelay = 5 + DiceRoller.rollDieInt(15);
                Utils.sleepMillis(nDelay * 100);
            }
            return human.getAction(false);
        }
        else if (checkfold_.isSelected())
        {
            if (hhand.getCall(human) == 0)
            {
                nAction = HandAction.ACTION_CHECK;
            }
            else
            {
                nAction = HandAction.ACTION_FOLD;
            }
        }
        else if (call_.isSelected())
        {
            nAction = HandAction.ACTION_CALL;
        }
        else if (bet_.isSelected())
        {
            nAction = HandAction.ACTION_BET;
            nAmount = Integer.MAX_VALUE; // reduced to max bet in PokerPlayer.bet();
        }
        else if (raise_.isSelected())
        {
            nAction = HandAction.ACTION_RAISE;
            nAmount = Integer.MAX_VALUE; // reduced to max raise in PokerPlayer.raise()
        }
        else if (betpot_.isSelected())
        {
            nAction = HandAction.ACTION_BET;
            nAmount = Integer.MAX_VALUE; // reduced to max bet in PokerPlayer.bet();
        }
        else if (raisepot_.isSelected())
        {
            nAction = HandAction.ACTION_RAISE;
            nAmount = Integer.MAX_VALUE; // reduced to max raise in PokerPlayer.raise()
        }
        else if (allin_.isSelected())
        {
            nAction = HandAction.ACTION_RAISE;
            nAmount = Integer.MAX_VALUE; // reduced to max raise in PokerPlayer.raise()
        }
        else
        {
            return null;
        }

        return new HandAction(human, nRound, nAction, nAmount, "advance");
    }

    /**
     * human acting
     */
    public static void humanActing(boolean b)
    {
        if (impl_ != null)
        {
            impl_._humanActing(b);
        }
    }

    /**
     * human is acting, disable buttons
     */
    private void _humanActing(boolean b)
    {
        bHumanActing_ = b;
        if (cheatbase_ != null) updateAll();
    }
}
