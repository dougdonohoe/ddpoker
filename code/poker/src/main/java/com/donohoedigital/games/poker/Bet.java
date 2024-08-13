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
/*
 * Bet.java
 *
 * Created on January 6, 2004, 9:22 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.dashboard.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Bet extends ChainPhase implements PlayerActionListener, CancelablePhase
{
    static Logger logger = Logger.getLogger(Bet.class);

    private PokerPlayer player_;
    private PokerTable table_;
    private HoldemHand hhand_;
    private PokerGame game_;
    private int nRound_;
    private boolean bSetZipModeAtEnd_ = false;

    /**
     * Override to skip nextPhase() (called explicitly)
     */
    @Override
    public void start()
    {
        process();
    }

    /**
     * logic and stuff
     */
    @Override
    public void process()
    {
        // get player, table and current hand
        game_ = (PokerGame) context_.getGame();
        table_ = game_.getCurrentTable();
        hhand_ = table_.getHoldemHand();
        player_ = hhand_.getCurrentPlayer();
        nRound_ = hhand_.getRound();

        //logger.debug("BET started for " + player_.getName() + " round: " + hhand_.getRoundName(hhand_.getRound()));

        // check for old phase.  Before we implemented online time outs,
        // the setPlayerActionListener() would throw an exception if we
        // tried to set a listener with one already existing (which was good
        // because it meant something not cleaned up properly).  However,
        // if a player's hand is auto-folded online due to a timeout, then
        // this Bet might not be cleaned up (if cancel request is lost),
        // so we'll do that here.

        if (((PokerContext) context_).getCurrentBetPhase() != null)
        {
            ((PokerContext) context_).getCurrentBetPhase().finish();
        }

        // remember, add to cancellist, set action listener
        ((PokerContext) context_).setCurrentBetPhase(this);
        EngineUtils.addCancelable(this);
        game_.setPlayerActionListener(this);

        // debugging
        if (TESTING(PokerConstants.TESTING_PAUSE_AI) || TESTING(PokerConstants.TESTING_FAST_SAVE))
        {
            setupDebugger(this);
        }

        // TESTING - used to save at first betting round, for testing AI so
        // the hand is saved before any AI decisions take place.  Also
        // allows saving via 'S' key at any time without having to go
        // into save menu
        if (TESTING(PokerConstants.TESTING_FAST_SAVE))
        {
            if (hhand_.getRound() == HoldemHand.ROUND_PRE_FLOP)
            {
                if (!((PokerContext)context_).isFastSaveTest())
                {
                    fastSave();
                    ((PokerContext)context_).setFastSaveTest(true);
                }
            }
        }

        // human controlled player
        if (player_.isHumanControlled())
        {
            // perform advance action only for actual human
            if (player_.isHuman())
            {
                HandAction action = AdvanceAction.getAdvanceAction();
                if (action != null)
                {
                    //logger.debug("Advance action: " + action);
                    handleAction(action);
                    return;
                }
                else
                {
                    AdvanceAction.humanActing(true);
                }

                // if fold key hit, fold now
                if (PokerUtils.isFoldKey())
                {
                    playerActionPerformed(PokerGame.ACTION_FOLD, 0);
                    return;
                }
            }

            // online games - play sound if option is on
            if (game_.isOnlineGame() && PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_AUDIO, true))
            {
                AudioConfig.playFX("onlineact");
            }

            // online games - move window to front if option is on
            if (Utils.ISWINDOWS && game_.isOnlineGame() && PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_FRONT, true))
            {
                BaseFrame frame = context_.getFrame();
                if (!frame.isFullScreen())
                {
                    if (frame.isMinimized())
                    {
                        if (frame.isMaximized())
                        {
                            frame.setMaximized();
                        }
                        else
                        {
                            frame.setNormal();
                        }
                    }
                    frame.toFront();

                }

            }

            // get basic info
            int nToCall = hhand_.getCall(player_);
            int nBet = hhand_.getBet();

            if (nToCall == 0)
            {
                if (nBet == 0)
                {
                    game_.setInputMode(PokerTableInput.MODE_CHECK_BET, hhand_, player_);
                }
                else
                {
                    game_.setInputMode(PokerTableInput.MODE_CHECK_RAISE, hhand_, player_);
                }
            }
            else
            {
                game_.setInputMode(PokerTableInput.MODE_CALL_RAISE, hhand_, player_);
            }
        }
        // Computer controlled player at active table
        else
        {
            // no button actions
            game_.setInputMode(PokerTableInput.MODE_QUITSAVE);

            // if the fold key was pressed, zip along
            if (PokerUtils.isFoldKey() && !table_.isZipMode())
            {
                boolean bZip = engine_.getPrefsNode().getBoolean(PokerConstants.OPTION_ZIP_MODE, false);
                if (bZip && !TESTING(PokerConstants.TESTING_DOUG_CONTROLS_AI) && !game_.isOnlineGame())
                {
                    bSetZipModeAtEnd_ = true;
                }
            }

            // do ai
            if (!TESTING(PokerConstants.TESTING_PAUSE_AI) || table_.isZipMode())
            {
                int nWaitTenths = game_.isOnlineGame() ?
                                  (TESTING(PokerConstants.TESTING_ONLINE_AI_NO_WAIT) ? 0 : TournamentDirector.AI_PAUSE_TENTHS) :
                                  engine_.getPrefsNode().getInt(PokerConstants.OPTION_DELAY, 8);

                // encore idea - have ai pause to increase drama after human has bet - to
                // make it appear like ai is "thinking" ... even if no delay is set
                // TODO: off for now - need to think more about this, maybe make an option
                if (false && !table_.isZipMode() && !game_.isOnlineGame() && hhand_.getRound() == HoldemHand.ROUND_RIVER)
                {
                    PokerPlayer human = game_.getHumanPlayer();
                    int action = hhand_.getLastActionThisRound(human);
                    if (action == HandAction.ACTION_BET ||
                        action == HandAction.ACTION_RAISE)
                    {
                        // if human bet or raised, have ai wait 2 to 5 seconds
                        int nNewWait = DiceRoller.rollDieInt(30) + 20;
                        if (nNewWait > nWaitTenths)
                        {
                            nWaitTenths = nNewWait;
                        }
                    }
                }

                // run ai
                if (nWaitTenths > 0 && !table_.isZipMode())
                {
                    Thread t = new Thread(new AIWait(nWaitTenths), "AIWait");
                    t.start();
                }
                else
                {
                    doAI();
                }
            }
        }
    }

    /**
     * New thread to wait for other tables to finish
     * before calling process. This allows swing loop
     * to handle redraws
     */
    private class AIWait implements Runnable
    {
        int nWaitTenths;
        public AIWait(int n)
        {
            nWaitTenths = n;
        }

        public void run()
        {
            // wait
            int nSleep = nWaitTenths * 100;
            Utils.sleepMillis(nSleep);

            // do processing
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                            doAI();
                    }
                }
            );
        }
    }

    /**
     * ai
     */
    public void doAI()
    {
        // if no AI, just fold (online, auto pilot case)
        if (TESTING(PokerConstants.TESTING_AUTOPILOT) && player_.getPokerAI() == null)
        {
            handleAction(fold());
        }
        // otherwise do player action
        else
        {
            handleAction(player_.getAction(false));
        }
    }
    
    /**
     * a button was pressed
     */
    public void playerActionPerformed(int nAction, int nAmount)
    {
        HandAction action = null;
        
        switch (nAction)
        {
            case PokerGame.ACTION_FOLD:
                action = fold();
                break;
            case PokerGame.ACTION_BET:
            case PokerGame.ACTION_RAISE:
                action = betRaise(nAmount);
                break;
            case PokerGame.ACTION_CHECK:
            case PokerGame.ACTION_CALL:
                action = checkCall();
                break;
            case PokerGame.ACTION_ALL_IN:
                action = allin();
                break;
            default:
                throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Shouldn't be here", null);
        }
        
        if (action == null) return;
        
        handleAction(action);
    }

    /**
     * Handle an action by player (AI and human)
     */
    private void handleAction(HandAction action)
    {
        // check fold
        int nAction = action.getAction();
        if (!table_.isZipMode())
        {
            switch (nAction)
            {
                case HandAction.ACTION_FOLD:
                    foldHumanCheck();
                    break;
            }
        }

        // notify TD of action
        TournamentDirector td = (TournamentDirector) context_.getGameManager();
        td.doHandAction(action, false);

        // update dash item to show we folded, need to do
        // after td call since that actually marks player
        // folded
        if (nAction == HandAction.ACTION_FOLD && player_.isHuman())
        {
            MyHand.cardsChanged(table_);
        }
        nextPhase();
    }

    /**
     * Cleanup then go to next phase
     */
    @Override
    public void nextPhase()
    {
        finish();
        super.nextPhase();
    }

    /**
     * finish
     */
    @Override
    public void finish()
    {
        // cleanup
        ((PokerContext) context_).setCurrentBetPhase(null);

        // remove cancel
        EngineUtils.removeCancelable(this);

        // change input mode before setting zip mode
        game_.setInputMode(PokerTableInput.MODE_QUITSAVE);

        // zip mode
        if (bSetZipModeAtEnd_ || hhand_.getNumWithCards() == 1) table_.setZipMode(true);

        // cleanup listeners
        game_.setPlayerActionListener(null);
        if (TESTING(PokerConstants.TESTING_PAUSE_AI) || TESTING(PokerConstants.TESTING_FAST_SAVE))
        {
            setupDebugger(null);
        }

        // notify AdvanceAction we are done with betting
        if (player_.isHuman())
        {
            AdvanceAction.humanActing(false);
        }
    }

    /**
     * forced cancel
     */
    public void cancelPhase()
    {
        finish();
    }


    /**
     * fold (human)
     */
    private HandAction fold()
    {
        // fold and move on
        return new HandAction(player_, nRound_, HandAction.ACTION_FOLD, "foldbtn");
    }

    /**
     * do zip processing when human folds
     */
    private void foldHumanCheck()
    {
        boolean bZip = engine_.getPrefsNode().getBoolean(PokerConstants.OPTION_ZIP_MODE, false);
        if (player_.isHuman() && bZip && !TESTING(PokerConstants.TESTING_DOUG_CONTROLS_AI) && !game_.isOnlineGame())
        {
            bSetZipModeAtEnd_ = true;
        }
    }

    /**
     * All in
     */
    private HandAction allin()
    {
        // allin with max - reduced to correct value in raise()
        return new HandAction(player_, nRound_, HandAction.ACTION_RAISE, Integer.MAX_VALUE, "allinbtn");
    }

    /**
     * bet raise
     */
    private HandAction betRaise(int nAmount)
    {
        // bet/raise by appropriate amount (in case user typed in value not
        // a multiple of min chip)
        int nNewAmount = PokerUtils.roundAmountMinChip(table_, nAmount);
        if (nNewAmount != nAmount)
        {
            String sMsg = PropertyConfig.getMessage("msg.betodd", table_.getMinChip(),
                                                    nAmount, nNewAmount);
            if (EngineUtils.displayConfirmationDialog(context_, sMsg, "msg.windowtitle.betodd", "betodd", "betodd"))
            {
                nAmount = nNewAmount;
            }
            else
            {
                 SwingUtilities.invokeLater(
                            new Runnable() {
                                public void run() {
                                    game_.setInputMode(PokerTableInput.MODE_RECHECK, hhand_, player_);
                                }
                            }
                            );
                return null;
            }
        }
        
        if (game_.getInputMode() == PokerTableInput.MODE_CHECK_BET)
        {
            return new HandAction(player_, nRound_, HandAction.ACTION_BET, nAmount, "betbtn");
        }
        else
        {
            return new HandAction(player_, nRound_, HandAction.ACTION_RAISE, nAmount, "raisebtn");
        }
    }

    /**
     * check/call
     */
    private HandAction checkCall()
    {
        // check or call
        if ((game_.getInputMode() == PokerTableInput.MODE_CHECK_BET ||
                game_.getInputMode() == PokerTableInput.MODE_CHECK_RAISE))
        {
            return new HandAction(player_, nRound_, HandAction.ACTION_CHECK, "checkbtn");
        }
        else
        {
            return new HandAction(player_, nRound_, HandAction.ACTION_CALL, "callbtn");
        }
    }


    /////
    ///// Debugging
    /////

    private static BetDebug betDebugger = null;

    /**
     * setup debugger
     */
    private static void setupDebugger(Bet bet)
    {
        if (betDebugger == null)
        {
            betDebugger = new BetDebug();
        }

        betDebugger.setBet(bet);
    }

    /**
     * class to respond to AWT events during debugging
     */
    private static class BetDebug implements AWTEventListener
    {
        Bet bet;

        public void setBet(Bet bet)
        {
            this.bet = bet;

            // remove in either case, just to be safe (in case called twice in a row)
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);

            if (bet != null)
            {
                Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
            }
        }

        /**
         * TESTING(PokerInit.TESTING_PAUSE_AI)
         */
        public void eventDispatched(AWTEvent event)
        {
            if (bet == null) return;

            if (event instanceof KeyEvent)
            {
                KeyEvent k = (KeyEvent) event;

                 // if not pressed or source is a text component, ignore (unless
                // the text source is the amount spinner)
                if (k.getID() != KeyEvent.KEY_PRESSED ||
                    (k.getSource() instanceof javax.swing.text.JTextComponent &&
                     !(k.getSource() instanceof DDNumberSpinner.SpinText)) ||
                     k.getSource() instanceof javax.swing.JTabbedPane) return;

                switch (k.getKeyCode())
                {
                    case KeyEvent.VK_N:
                        if (TESTING(PokerConstants.TESTING_PAUSE_AI)) bet.doAI();
                        break;

                    case KeyEvent.VK_S: // TESTING_FAST_SAVE use
                        bet.fastSave();
                        break;
                }
            }
        }
    }

    /**
     * debug autosave
     */
    private void fastSave()
    {
        if (game_.canSave())
        {
            logger.debug("FAST SAVE");
            game_.saveWriteGame();
        }
        else
        {
            logger.debug("SKIPPING FAST SAVE - not yet saved manually");
        }
    }
}
