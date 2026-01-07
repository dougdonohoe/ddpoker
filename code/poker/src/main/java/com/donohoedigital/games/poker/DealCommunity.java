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
 * DealCommunity.java
 *
 * Created on January 6, 2004, 9:29 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.dashboard.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

import javax.swing.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DealCommunity extends ChainPhase implements PlayerActionListener
{
    static Logger logger = LogManager.getLogger(DealCommunity.class);

    private TournamentDirector td_;
    private PokerGame game_;
    private PokerTable table_;
    private HoldemHand hhand_;
    private int nRound_;
    private int nNumWithCards_;
    private static int nLastHandAllin_ = 0;
    private Hand community_;
    private boolean bDrawnNormal_;
    private boolean bDrawn_;

    /**
     * override to control process() and nextPhase()
     */
    public void start()
    {
        game_ = (PokerGame) context_.getGame();
        td_ = (TournamentDirector) context_.getGameManager();
        table_ = game_.getCurrentTable();
        game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
        hhand_ = table_.getHoldemHand();
        community_ = hhand_.getCommunity();
        nNumWithCards_ = hhand_.getNumWithCards();
        nRound_ = hhand_.getRound();
        
        // if only one player with cards left, don't show
        // more community cards unless the option is on
        // keep these in sync with syncCards() method too
        boolean bRabbitHunt = PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_RABBITHUNT);
        bDrawnNormal_ = nNumWithCards_ > 1;
        bDrawn_ = bRabbitHunt || bDrawnNormal_;

        // if we are done betting before we start, then
        // we are in an all-in showdown.  Show cards.
        if (hhand_.isAllInShowdown() && !table_.isZipMode())
        {
            boolean bPause = (PokerUtils.isOptionOn(PokerConstants.OPTION_PAUSE_ALLIN) ||
                              PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_PAUSECARDS)) &&
                             !TESTING(PokerConstants.TESTING_AUTOPILOT) && !game_.isOnlineGame();

            // show information dialog in practice games about pause
            if (bPause && table_.getHandNum() != nLastHandAllin_)
            {
                EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.allin.info"), "msg.allin.title", "allininfo");
            }
            nLastHandAllin_ = table_.getHandNum();

            // expose all cards
            PokerPlayer player;
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                player = table_.getPlayer(i);
                if (player == null || player.isFolded() || player.getHand() == null) continue;
                player.getHand().setType(Hand.TYPE_FACE_UP); // change type for saving
                PokerUtils.showCards(player, true);
            }

            // display stats
            Showdown.displayAllin(hhand_, false);

            if (bPause)
            {
                game_.setInputMode(PokerTableInput.MODE_CONTINUE_LOWER);
                game_.setPlayerActionListener(this);
            }
            else
            {
                int nWaitTenths = game_.isOnlineGame() ? 10 : PokerUtils.getIntOption(PokerConstants.OPTION_DELAY);
                nWaitTenths = Math.max(nWaitTenths, 7);
                process(nWaitTenths); // TODO: configure all-in-showdown pause online/practice?
            }

            // return (continue when player presses button and
            // playerActionPerformed called or thread moves along)
            return;
        }

        process();
    }

    /**
     * Continue button
     */
    public void playerActionPerformed(int action, int nAmount)
    {
        game_.setPlayerActionListener(null);
        game_.setInputMode(PokerTableInput.MODE_QUITSAVE);

        // display with no delay (all-in showdown)
        displayCards();
    }

    /**
     * Process drawing
     */
    public void process()
    {
        process(0);
    }
    
    /**
     * process drawing
     */
    private void process(int nWaitTenths)
    {
        if (bDrawn_ && !table_.isZipMode() && nNumWithCards_ > 1)
        {
            // wait after showing cards
            if (nWaitTenths == 0) nWaitTenths = game_.isOnlineGame() ? 0 : PokerUtils.getIntOption(PokerConstants.OPTION_DELAY);
            if (nWaitTenths > 0)
            {
                Thread t = new Thread(new DealWait(nWaitTenths), "DealWait");
                t.start();
                return;
            }
        }
        
        // display cards directly if thread not started
        displayCards();
    }
    
    /**
     * Thread to sleep between cards
     */
    private class DealWait implements Runnable
    {
        int nWaitTenths;
        public DealWait(int n)
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
                            displayCards();
                    }
                }
            );
        }
    }
    
    /**
     * display cards
     */
    private void displayCards()
    {
        String sHand = null;
        switch(nRound_)
        {
            case HoldemHand.ROUND_FLOP:
                addCard(table_, CardPiece.POINT_FLOP1, 0, bDrawnNormal_, bDrawn_, false);
                addCard(table_, CardPiece.POINT_FLOP2, 1, bDrawnNormal_, bDrawn_, false);
                addCard(table_, CardPiece.POINT_FLOP3, 2, bDrawnNormal_, bDrawn_, bDrawn_);
                sHand = community_.toHTML();
                break;

            case HoldemHand.ROUND_TURN:
                addCard(table_, CardPiece.POINT_FLOP4, 3, bDrawnNormal_, bDrawn_, bDrawn_);
                sHand = community_.getCard(3).toHTML();
                break;

            case HoldemHand.ROUND_RIVER:
                addCard(table_, CardPiece.POINT_FLOP5, 4, bDrawnNormal_, bDrawn_, bDrawn_);
                sHand = community_.getCard(4).toHTML();
                break;

            default:
                throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Invalid round: " + HoldemHand.getRoundName(nRound_), null);
        }

        // chat display
        if (bDrawn_ && !table_.isZipMode())
        {
            String sRound = PropertyConfig.getMessage("msg.round." + nRound_);
            td_.sendDealerChatLocal(PokerConstants.CHAT_2, PropertyConfig.getMessage("msg.chat.community",
                                                              sRound,
                                                              sHand));
        }

        // notify dash item (see note in DealDisplay.java for details)
        MyHand.cardsChanged(table_);

        // move along
        nextPhase();
    }
    
    public void nextPhase()
    {


        // start pause (puts into pause mode, so processTable() isn't
        // called when this player is removed from the wait list)
        if (PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_PAUSECARDS) &&
            (!hhand_.isAllInShowdown() || hhand_.getRound() == HoldemHand.ROUND_RIVER)
            && !table_.isZipMode())
        {
            PokerUtils.TDPAUSER(context_).pause();
        }

        // in online game, after showing cards, need to set current player
        // (see note in TournamentDirector.doBetting() for some details).
        // As noted in doBetting(), we don't want to set current player
        // before deal for UI purposes.  In an online game, remote clients
        // may get a hand action before they get the updated HoldemHand
        // (due to the way doBetting() works).  Thus, we do this check here
        // primarily for user-displayed tables (host and remote).
        hhand_.getCurrentPlayerInitIndex();

        // notify tournament director that cards have the player has
        // seen the cards dealt
        td_.removeFromWaitList(game_.getHumanPlayer());
        
        super.nextPhase();
    }
    
    /**
     * Make sure board cards match what is actually displayed
     */
    public static void syncCards(PokerTable table)
    {
        HoldemHand hhand = table.getHoldemHand();
        if (hhand == null) return;

        // get last betting round and current round
        HandAction last = hhand.getLastAction();
        int nLastBettingRound = last.getRound();

        // these flags match above
        int nNumWithCards = hhand.getNumWithCards();
        boolean bRabbitHunt = PokerUtils.isCheatOn(table.getGame().getGameContext(), PokerConstants.OPTION_CHEAT_RABBITHUNT);
        boolean bDrawnNormal = nNumWithCards > 1;
        boolean bDrawn = bRabbitHunt || bDrawnNormal;

        // all-in-showdown happening, so only show cards up to previous round
        // due to the way saves are done, DealCommunity is called again which
        // will re-display the placards (this is why we don't call initial a sync()
        // from the GamePrefsDialog)
        int nRound = hhand.getRoundForDisplay();

        // all cases fall through on purpose
        boolean bCardDealt;
        switch(nRound)
        {
            case HoldemHand.ROUND_SHOWDOWN:
            case HoldemHand.ROUND_RIVER:
                bCardDealt = nLastBettingRound >= HoldemHand.ROUND_RIVER;
                addCard(table, CardPiece.POINT_FLOP5, 4, bDrawnNormal||bCardDealt, bDrawn||bCardDealt, false);
            case HoldemHand.ROUND_TURN:
                bCardDealt = nLastBettingRound >= HoldemHand.ROUND_TURN;
                addCard(table, CardPiece.POINT_FLOP4, 3, bDrawnNormal||bCardDealt, bDrawn||bCardDealt, false);
            case HoldemHand.ROUND_FLOP:
                bCardDealt = nLastBettingRound >= HoldemHand.ROUND_FLOP;
                addCard(table, CardPiece.POINT_FLOP3, 2, bDrawnNormal||bCardDealt, bDrawn||bCardDealt, false);
                addCard(table, CardPiece.POINT_FLOP2, 1, bDrawnNormal||bCardDealt, bDrawn||bCardDealt, false);
                addCard(table, CardPiece.POINT_FLOP1, 0, bDrawnNormal||bCardDealt, bDrawn||bCardDealt, false);
        }

        // update dash item to match
        MyHand.cardsChanged(table);
    }
    
    /**
     * bDrawn - is card displayed (might be yes if show river cards is on)
     * bDrawnNormal - is card displayed normally
     */
    private static void addCard(PokerTable table, String sTP, int c, boolean bDrawnNormal, boolean bDrawn, boolean bRepaint)
    {
        CommunityCardPiece piece = new CommunityCardPiece(table, sTP, c);
        piece.setNotDrawn(!bDrawn);
        piece.setDrawnNormal(bDrawnNormal);
        Territory t = PokerUtils.getFlop();
        t.addGamePiece(piece);
        if (bRepaint) PokerUtils.getPokerGameboard().repaintTerritory(t);
    }
}
