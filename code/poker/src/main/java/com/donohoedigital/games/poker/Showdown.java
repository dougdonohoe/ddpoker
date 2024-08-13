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
 * Showdown.java
 *
 * Created on January 7, 2004, 12:26 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.log4j.*;

import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Showdown extends ChainPhase
{
    static Logger logger = Logger.getLogger(Showdown.class);

    private HoldemHand hhand_;
    private TournamentDirector td_;

    /**
     *
     */
    public void process()
    {
        PokerUtils.setNoFoldKey();
        td_ = (TournamentDirector) context_.getGameManager();
        PokerGame game = (PokerGame) context_.getGame();
        int nMode = game.getInputMode();
        if (nMode != PokerTableInput.MODE_QUITSAVE)
        {
            game.setInputMode(PokerTableInput.MODE_QUITSAVE);
        }
        else
        {
            game.setInputMode(PokerTableInput.MODE_REBUY_CHECK); // BUG 420
        }
        PokerTable table = game.getCurrentTable();
        hhand_ = table.getHoldemHand();

        PokerPlayer human = game.getHumanPlayer();

        // did human go all in and win?
        int win = hhand_.getWin(human);
        if (win == hhand_.getTotalPotChipCount() && human.getChipCount() == win)
        {
            PokerUtils.cheerAudio();
        }

        // display
        displayShowdown(engine_, context_, hhand_);

        // chat
        displayChat();
    }

    /**
     * Display showdown results - designed to be called multiple times,
     * adjusting for changes in options
     */
    static void displayShowdown(GameEngine engine, GameContext context, HoldemHand hhand)
    {
        PokerGame game = (PokerGame) context.getGame();
        PokerPlayer player;
        ResultsPiece piece;
        Territory t;
        HandInfo info;
        boolean bUncontested = hhand.isUncontested();
        boolean bShowRiver = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_RABBITHUNT);
        boolean bShowWin = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_SHOWWINNINGHAND);
        boolean bShowMuck = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_SHOW_MUCKED);
        boolean bHumanUp = !engine.getPrefsNode().getBoolean(PokerConstants.OPTION_HOLE_CARDS_DOWN, false);
        boolean bAIFaceUp = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_AIFACEUP);
        boolean bSeenRiver = hhand.isActionInRound(HoldemHand.ROUND_RIVER);
        boolean bShowCards;
        boolean bShowHandType = !bUncontested || ((bShowRiver||bSeenRiver) && bShowWin);
        boolean bShowHandTypeFold = !bUncontested || bShowRiver || bSeenRiver;
        boolean bShowHandTypeLocal;

        // display results
        int nAmount;
        int nOverbet;
        int nTotal;
        String sResult;
        int nResult;
        boolean bWon;

        for (int i = 0; i < hhand.getNumPlayers(); i++)
        {
            // get info
            player = hhand.getPlayerAt(i);
            if (player.getTable() == null) continue; // could be null due to saving after RemovePlayer cheat option used
            t = PokerUtils.getTerritoryForTableSeat(player.getTable(), player.getSeat());
            piece = PokerGameboard.getTerritoryInfo(t).resultpiece;

            // all-in showdown, just show current hand
            if (hhand.getRound() < HoldemHand.ROUND_SHOWDOWN)
            {
                if (!player.isFolded())
                {
                    if (hhand.getRound() == HoldemHand.ROUND_PRE_FLOP)
                    {
                        piece.setResult(ResultsPiece.ALLIN,
                            PropertyConfig.getMessage("msg.hand.allin",
                                    player.getHand().toStringRank()));
                    }
                    else
                    {
                        info = new HandInfo(player, player.getHandSorted(), hhand.getCommunitySorted());
                        piece.setResult(ResultsPiece.ALLIN,
                            PropertyConfig.getMessage("msg.hand.allin",
                                info.getHandTypeDesc(),
                                info.getBest().toStringRank()));
                    }
                }
                continue;
            }

            // cleanup % win (all-in)
            player.setAllInPerc(null);

            ///
            /// folded players
            ///
            if (player.isFolded())
            {
                if (player.showFoldedHand())
                {
                    int nRound = hhand.getFoldRound(player);
                    info = new HandInfo(player, player.getHandSorted(), hhand.getCommunitySorted());
                    String sRound = PropertyConfig.getMessage("msg.round."+nRound);
                    piece.setResult(ResultsPiece.FOLD,
                                PropertyConfig.getMessage(bShowHandTypeFold ?
                                                "msg.hand.fold" : "msg.hand.fold.noshow",
                                                sRound, info.getHandTypeDesc(),
                                                info.getBest().toStringRank()));
                    PokerUtils.showCards(player, true);
                }
                else
                {
                    // online game - use disconnected logic so we don't remove
                    // a sitting out/disconnected placard
                    if (game.isOnlineGame())
                    {
                        PokerUtils.setConnectionStatus(context, player, true);
                    }
                    else
                    {
                        piece.setResult(ResultsPiece.HIDDEN, "");
                    }
                    PokerUtils.showCards(player, false);
                }
                continue;
            }

            ///
            /// players who reached showdown
            ///

            // amount won
            nAmount = hhand.getWin(player);
            nOverbet = hhand.getOverbet(player);
            nTotal = nAmount + nOverbet;
            bWon = (nAmount > 0);

            // determine whether cards and hand types are shown

            bShowCards =    player.isCardsExposed() ||
                            (!bUncontested && (bShowMuck && !bWon)) ||
                            (bShowWin && bWon) ||
                            (player.isHuman() && player.isLocallyControlled() && bHumanUp) ||
                            (player.isComputer() && bAIFaceUp);

            bShowHandTypeLocal = bShowHandType;
            // human cards are always known to user (mouse over hole cards immaterial),
            // so show handtype if showing river
            if (player.isHuman() && bShowRiver) bShowHandTypeLocal = true;

            // uncontested, but winning player is showing hand
            if (bUncontested && player.isShowWinning() && (bShowRiver||bSeenRiver)) bShowHandTypeLocal = true;

            // get hand info
            info = player.getHandInfo();

            // overbet / win text
            if (nTotal > 0)
            {
                sResult =PropertyConfig.getMessage(bShowHandTypeLocal ? "msg.hand.win" : "msg.hand.win.noshow",
                                info.getHandTypeDesc(),
                                info.getBest().toStringRank(),
                                nTotal);
            }
            // lose text
            else
            {
                sResult =PropertyConfig.getMessage(bShowCards ? "msg.hand.lose":"msg.hand.lose.muck",
                                info.getHandTypeDesc(),
                                info.getBest().toStringRank());
            }

            // placard choice
            if (nTotal == 0)
            {
                nResult = ResultsPiece.LOSE;
            }
            else
            {
                if (nOverbet == nTotal)
                {
                    nResult = ResultsPiece.OVERBET;
                }
                else
                {
                    nResult = ResultsPiece.WIN;
                }
            }
            piece.setResult(nResult, sResult);
            PokerUtils.showCards(player, bShowCards);
        }

        // update board cards too (for change in option)
        Territory flop = PokerUtils.getFlop();
        List<GamePiece> cards;
        CardPiece card;
        synchronized (flop.getMap())
        {
            cards = EngineUtils.getMatchingPieces(flop, PokerConstants.PIECE_CARD);
            for (GamePiece gp : cards)
            {
                card = (CardPiece) gp;
                card.setNotDrawn(!card.isDrawnNormal() && !bShowRiver);
            }
        }

        PokerUtils.getPokerGameboard().repaintAll();
    }

    /**
     * Display showdown results - designed to be called multiple times,
     * adjusting for changes in options
     */
    static void displayAllin(HoldemHand hhand, boolean bAllCardsDisplayed)
    {
        PokerPlayer player;
        ResultsPiece piece;
        Territory t;
        HandInfo info;

        // we know next card before we display it, so do all in
        // percentages based on community cards before current cards
        HandSorted comm = new HandSorted(bAllCardsDisplayed ? hhand.getCommunity() : hhand.getCommunityForDisplay());
        HandStrength.doAllInPercentages(hhand, comm);

        int nMax = 0;
        for (int i = 0; i < hhand.getNumPlayers(); i++)
        {
            player = hhand.getPlayerAt(i);
            if (player.isFolded()) continue;
            if (player.getAllInWin() > nMax)
            {
                nMax = player.getAllInWin();
            }
        }

        int nResult;
        for (int i = 0; i < hhand.getNumPlayers(); i++)
        {
            // get info
            player = hhand.getPlayerAt(i);
            t = PokerUtils.getTerritoryForTableSeat(player.getTable(), player.getSeat());
            piece = PokerGameboard.getTerritoryInfo(t).resultpiece;

            nResult = ResultsPiece.ALLIN;
            if (player.getAllInWin() == nMax) nResult = ResultsPiece.WIN;

            if (!player.isFolded())
            {
                // when this is called, round has advanced already
                if (hhand.getRound() == HoldemHand.ROUND_FLOP)
                {
                    piece.setResult(nResult,
                        PropertyConfig.getMessage("msg.hand.allin.pre",
                                player.getAllInPerc(),
                                player.getHand().toStringRank()));
                }
                else
                {
                    info = new HandInfo(player, player.getHandSorted(), comm);
                    piece.setResult(nResult,
                        PropertyConfig.getMessage("msg.hand.allin",
                            player.getAllInPerc(),
                            info.getHandTypeDesc(),
                            info.getBest().toStringRank()));
                }
            }
        }

        PokerUtils.getPokerGameboard().repaintAll();
    }

    /**
     * Show chat for all pots
     */
    private void displayChat()
    {
        int nNum = hhand_.getNumPots();
        for (int i = nNum - 1; i >= 0; i--)
        {
            displayChatPot(i);
        }
    }

    private int nSide_ = 0;

    /**
     * Show chat for given pot
     */
    private void displayChatPot(int nPot)
    {
        Pot pot = hhand_.getPot(nPot);
        List<PokerPlayer> winners = pot.getWinners();
        int nNum = pot.getNumPlayers();
        HandAction action;
        PokerPlayer player;

        String sKey;
        // overbet
        if (nNum == 1)
        {
            player = winners.get(0);
            action = hhand_.getPotResult(player, nPot);
            td_.sendDealerChatLocal(PokerConstants.CHAT_1, action.getChat(0, null, null));
            return;
        }
        else if (nPot == 0)
        {
            sKey = "msg.chat.pot.main";
        }
        else
        {
            sKey = "msg.chat.pot.side";
            nSide_++;
        }

        // header
        String sHeader = PokerUtils.chatImportant(PropertyConfig.getMessage(sKey, nSide_,
                                                                            pot.getChipCount()));

        StringBuilder sb = new StringBuilder();
        HandInfoFast info = new HandInfoFast();
        Object[] extraParams = new Object[2];
        boolean bUncontested = hhand_.isUncontested();
        int nAction;

        // loop in order card shown
        int nNumPlayers = hhand_.getNumPlayers();
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = hhand_.getPlayerAt(i);
            if (player.isFolded()) continue;
            if (!pot.isInPot(player)) continue;

            // don't compute score if cards not exposed (in online games,
            // when run on client, this would cause an error cuz we
            // don't send the cards in uncontested pots)
            if (!player.isCardsExposed())
            {
                extraParams[0] = null;
            }
            else
            {
                extraParams[0] = player.getHand().toHTML();
                if (!bUncontested)
                {
                    info.getScore(player.getHand(), hhand_.getCommunity());
                    extraParams[1] = info.toString();
                }
            }

            // get result
            action = hhand_.getPotResult(player, nPot);

            // check for null - can be null if showdown happens in online games after next hand started
            // (due to slow swing dispatching)  TODO: remove if we modify how showdown gets the holdemhand
            // this is mainly seen on observers who aren't waitlisted
            if (action == null) return;

            nAction = action.getAction();
            if (nAction == HandAction.ACTION_OVERBET)
            {
                sb.append(action.getChat(0, null, null));
            }
            else if (nAction == HandAction.ACTION_LOSE)
            {
                sb.append(action.getChat(0, extraParams, !player.isCardsExposed() ? "muck" : null));
            }
            else
            {
                sb.append(action.getChat(0, extraParams, bUncontested ?
                                                         (player.isShowWinning() ? "uncontested.show":"uncontested") : null));
            }
        }

        td_.sendDealerChatLocal(PokerConstants.CHAT_1, PropertyConfig.getMessage("msg.chat.pot", sHeader, sb.toString()));
    }
}
