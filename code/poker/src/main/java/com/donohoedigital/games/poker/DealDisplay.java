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
 * DealDisplay.java
 *
 * Created on January 1, 2004, 7:15 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.dashboard.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
public class DealDisplay extends ChainPhase implements Runnable
{
    static Logger logger = LogManager.getLogger(DealDisplay.class);

    private static final int DEFAULT_CARD_DELAY = 20;

    private int nCardDelay_;

    private TournamentDirector td_;
    private PokerGame game_;
    private PokerTable table_;
    private boolean bHighCard_;
    private boolean bColor_;

    /**
     * Override to not call nextPhase here (do at end of thread)
     */
    @Override
    public void start()
    {
        process();
    }

    /**
     * display cards
     */
    @Override
    public void process()
    {
        PokerUtils.clearCards(false);
        game_ = (PokerGame) context_.getGame();
        table_ = game_.getCurrentTable();
        td_ = (TournamentDirector) context_.getGameManager();

        // get any player, say the button, and determine hand type
        // from that player for local purposes
        PokerPlayer aplayer = table_.getPlayerRequired(table_.getButton());
        switch (aplayer.getHand().getType())
        {
            case Hand.TYPE_DEAL_HIGH:
                bHighCard_ = true;
                break;

            case Hand.TYPE_COLOR_UP:
                bColor_ = true;
                break;
        }

        // start of online tournament - ask guest if the want to play or sitout
        if (game_.isOnlineGame() && bHighCard_)
        {
            PokerPlayer player = game_.getLocalPlayer();
            if (!player.isHost() && !player.isObserver() && player.isSittingOut())
            {
                if (game_.isOnlineGame() && PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_AUDIO, true))
                {
                    AudioConfig.playFX("onlineact");
                }

                GameButton bPlay = EngineUtils.displayConfirmationDialogCustom(context_, "OnlineGuestStart", null, null, null, null);
                if (bPlay != null && bPlay.getName().startsWith("yes"))
                {
                    player.setSittingOut(false);
                    td_.playerUpdate(player, player.getOnlineSettings());
                }
            }
        }

        // show info dialog for high card
        if (!TESTING(PokerConstants.TESTING_AUTOPILOT_INIT) && bHighCard_ && table_.getHandNum() == 0 && !game_.isOnlineGame())
        {
            String sMsg = PropertyConfig.getMessage("msg.highcardbutton");
            EngineUtils.displayInformationDialog(context_, Utils.fixHtmlTextFor15(sMsg), "msg.highcard.title", "DealHighCard");
        }

        // show intro message on hand 5
        if (!TESTING(PokerConstants.TESTING_AUTOPILOT_INIT) && !bHighCard_ && !bColor_ && table_.getHandNum() == 5 && !game_.isOnlineGame())
        {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.intro.info"), "msg.intro.title", "introinfo");
        }

        // show option message on hand 10
        if (table_.getHandNum() == 10 && !TESTING(PokerConstants.TESTING_AUTOPILOT) && !game_.isOnlineGame())
        {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.hint.option.info"), "msg.hint.option.title", "hintoption");
        }

        nCardDelay_ = gamephase_.getBoolean("delay", true) ? DEFAULT_CARD_DELAY : 0;
        if (TESTING(PokerConstants.TESTING_AUTOPILOT)) nCardDelay_ = 0;

        // TODO: remove this if decide to wait for observers in TD wait list
        if (game_.isOnlineGame() && game_.getLocalPlayer().isObserver()) nCardDelay_ = 0;

        // audio
        int nNum = table_.getNumOccupiedSeats();
        AudioConfig.playFX((nNum >= 4 ? "shuffle" : "shuffleshort") + DiceRoller.rollDieInt(3), 0);

        // chat
        if (td_ != null && !bHighCard_ && !bColor_)
        {
            // new deal
            td_.sendDealerChatLocal(PokerConstants.CHAT_1, PokerUtils.chatImportant(PropertyConfig.getMessage("msg.chat.newdeal",
                                                                                                              table_.getHandNum(),
                                                                                                              table_.getNumber())));

            // blinds
            HoldemHand hhand = table_.getHoldemHand();
            List<HandAction> hist = hhand.getHistoryCopy();
            for (HandAction blind : hist)
            {
                if (blind.getAction() == HandAction.ACTION_ANTE ||
                    blind.getAction() == HandAction.ACTION_BLIND_SM ||
                    blind.getAction() == HandAction.ACTION_BLIND_BIG)
                {
                    td_.sendDealerChatLocal(PokerConstants.CHAT_2, blind.getChat(0, null, null));
                }
            }
        }

        // display deal
        Thread tDeal = new Thread(this, "DealDisplay");
        tDeal.start();
    }

    /**
     * thread
     */
    public void run()
    {
        boolean bDone = false;
        int nNum = 0;
        while (!bDone)
        {
            bDone = showCard(nNum);
            nNum++;
        }

        // move along
        nextPhase();
    }

    @Override
    public void nextPhase()
    {
        // only notify done if not doing color up (that is done in ColorUpFinish)
        if (!bColor_)
        {
            // notify dash item that hand is dealt.  WE do this directly
            // instead of via events because there is no real event to
            // trigger off of.  We could do state change from deal to betting,
            // but that isn't fired on the client.  This is easier, for a one-off
            // case.  We do this, BTW, so the MyHand doesn't show the cards until
            // they are dealt.  A small detail, but nice to not see you hand
            // before the cards are actually dealt
            MyHand.cardsChanged(table_);

            // start pause (puts into pause mode, so processTable() isn't
            // called when this player is removed from the wait list)
            if (!bHighCard_ && PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_PAUSECARDS))
            {
                PokerUtils.TDPAUSER(context_).pause();
            }

            // in online game, after showing deal, need to set current player
            // (see note in TournamentDirector.doBetting() for some details).
            // As noted in doBetting(), we don't want to set current player
            // before deal for UI purposes.  In an online game, remote clients
            // may get a hand action before they get the updated HoldemHand
            // (due to the way doBetting() works).  Thus, we do this check here
            // primarily for user-displayed tables (host and remote).
            HoldemHand hhand = table_.getHoldemHand();
            if (hhand != null)
            {
                // just makes sure the player list is set
                hhand.getCurrentPlayerInitIndex();
            }

            // notify tournament director that cards have the player has
            // seen the cards dealt (td is null in simulator)
            if (td_ != null) td_.removeFromWaitList(game_.getHumanPlayer());
        }

        super.nextPhase();
    }

    /**
     * show card in hand with given index
     */
    private boolean showCard(int c)
    {
        int nSeat = 0;
        if (bHighCard_ || bColor_) nSeat = table_.getNextSeat(-1);
        else nSeat = table_.getNextSeatAfterButton();

        PokerPlayer player;
        Hand hand;

        int nNum = table_.getNumOccupiedSeats();
        int nCards = 0;

        int nCnt;

        for (int i = 0; i < nNum; i++)
        {
            // get player and hand
            player = table_.getPlayerRequired(nSeat);
            hand = player.getHand();

            // check in case player moved to table after hand
            // started (due to table break up)
            if (hand != null)
            {
                // display card
                nCnt = hand.size();

                // color up - show all in hand
                if (hand.getType() == Hand.TYPE_COLOR_UP)
                {
                    for (c = 0; c < nCnt; c++)
                    {
                        // don't increment nCards so we return true from this method
                        displayCard(context_, player, c, true, nCardDelay_);
                    }
                }
                // normal show given card
                else
                {
                    if (c < nCnt)
                    {
                        nCards++;
                        displayCard(context_, player, c, true, nCardDelay_);
                    }
                }
            }

            // increment seat (unless at end - fixes issue when no next seat
            // when only 1 player seated)
            if ((i + 1) < nNum) nSeat = table_.getNextSeat(nSeat);
        }

        return nCards == 0;
    }

    /**
     * Make sure cards in players hands in table match what
     * is actually displayed
     */
    public static void syncCards(PokerTable table)
    {
        PokerPlayer player;
        Hand hand;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = table.getPlayer(i);
            if (player == null) continue;

            hand = player.getHand();
            if (hand == null) continue;

            for (int c = 0; c < hand.size(); c++)
            {
                displayCard(table.getGame().getGameContext(), player, c, false, 0);
            }
        }
    }

    /**
     * Display card for given player
     */
    private static void displayCard(GameContext context, PokerPlayer player, int c,
                                    boolean bRepaint, int nCardDelay)
    {
        int nSeat = player.getSeat();
        Hand hand = player.getHand();
        boolean bUp = hand.getType() != Hand.TYPE_NORMAL;
        GameEngine engine = GameEngine.getGameEngine();
        boolean bDealDown = engine.getPrefsNode().getBoolean(PokerConstants.OPTION_HOLE_CARDS_DOWN, false);
        boolean bAIFaceUp = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_AIFACEUP);

        // get card and create a piece around it
        Territory t;
        CardPiece piece = new CardPiece(context, player, CardPiece.POINT_HOLE1,
                                        bUp ||
                                        (player.isHuman() && player.isLocallyControlled() && !bDealDown) ||
                                        (player.isComputer() && bAIFaceUp),
                                        c);
        if (hand.getType() == Hand.TYPE_COLOR_UP) piece.setThumbnailMode(true);
        t = PokerUtils.getTerritoryForTableSeat(player.getTable(), nSeat);
        t.addGamePiece(piece);

        if (bRepaint)
        {
            final Territory tParam = t;
            GuiUtils.invokeAndWait(
                    new Runnable()
                    {
                        public void run()
                        {
                            // check null for clearer exit
                            if (PokerUtils.getGameboard() != null)
                            {
                                PokerUtils.getGameboard().repaintTerritory(tParam, true);
                            }
                        }
                    }
            );

            // sleep
            Utils.sleepMillis(nCardDelay);
        }
    }

}
