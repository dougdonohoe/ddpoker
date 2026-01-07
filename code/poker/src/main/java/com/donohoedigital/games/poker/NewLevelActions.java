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
 * NewLevelActions.java
 *
 * Created on January 26, 2005, 9:52 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.base.*;

/**
 *
 * @author  donohoe
 */
public class NewLevelActions extends ChainPhase implements CancelablePhase
{
    private PokerGame game_;
    private TournamentDirector td_;
    private boolean bCanceled_ = false;

    public void process()
    {
        EngineUtils.addCancelable(this);

        game_ = (PokerGame) context_.getGame();
        PokerTable table = game_.getCurrentTable();
        td_ = (TournamentDirector) context_.getGameManager();

        TournamentProfile profile = game_.getProfile();
        int nThisLevel = table.getLevel();
        int nNextLevel = nThisLevel + 1;

        if (!profile.isBreak(nThisLevel))
        {
            // check last human rebuy
            PokerPlayer human = game_.getHumanPlayer();
            if (!bCanceled_ && nThisLevel == profile.getLastRebuyLevel() &&
                table.isRebuyAllowed(human, nThisLevel))
            {
                rebuy(game_, ShowTournamentTable.REBUY_LAST, nThisLevel);
            }
            PokerUtils.showComputerBuys(context_, game_, table.getRebuyList(),
                                        nThisLevel == profile.getLastRebuyLevel() ? "rebuylast" : "rebuy");

            // check human add-on
            if (!bCanceled_ && table.isAddonAllowed(human))
            {
                addon();
            }
            PokerUtils.showComputerBuys(context_, game_, table.getAddonList(), "addon");
        }

        // notify about new level
        if (!TESTING(PokerConstants.TESTING_AUTOPILOT))
        {
            String sMsg;
            if (profile.isBreak(nNextLevel))
            {
                String sKey = game_.isOnlineGame() ? "msg.chat.break" : "msg.dialog.break";
                sMsg = PropertyConfig.getMessage(sKey,
                                                 nNextLevel,
                                                 profile.getMinutes(nNextLevel));

                if (game_.isOnlineGame())
                {
                    td_.sendDealerChatLocal(PokerConstants.CHAT_1, PokerUtils.chatInformation(sMsg));
                }
                else
                {
                    EngineUtils.displayInformationDialog(context_, sMsg, "msg.windowtitle.break", "newbreak", "nobreak");
                }

            }
            else
            {
                int nAnte  = profile.getAnte(nNextLevel);
                int nBig = profile.getBigBlind(nNextLevel);
                int nSmall = profile.getSmallBlind(nNextLevel);

                String sKey = null;
                if (game_.isOnlineGame())
                {
                    sKey = nAnte > 0 ? "msg.chat.next.ante" : "msg.chat.next";
                }
                else
                {
                    sKey = nAnte > 0 ? "msg.dialog.next.ante" : "msg.dialog.next";
                }

                sMsg = PropertyConfig.getMessage(sKey,
                                                 nNextLevel,
                                                 nSmall,
                                                 nBig,
                                                 nAnte);

                if (game_.isOnlineGame())
                {
                    td_.sendDealerChatLocal(PokerConstants.CHAT_1, PokerUtils.chatInformation(sMsg));
                }
                else
                {
                    EngineUtils.displayInformationDialog(context_, Utils.fixHtmlTextFor15(sMsg), "msg.windowtitle.level", "newlevel", "nolevel");
                }
            }
        }
    }

    /**
     * note cancelled (set flag so we don't display any more dialogs)
     */
    public void cancelPhase()
    {
        bCanceled_ = true;
    }

    /**
     * Next phase
     */
    public void nextPhase()
    {
        // remove cancel
        EngineUtils.removeCancelable(this);

        // don't bother removing wait list if cancelled
        if (bCanceled_) return;

        // notify tournament director that cards have the player has
        // done the new level actions
        td_.removeFromWaitList(game_.getHumanPlayer());

        super.nextPhase();
    }

    /**
     * Rebuy - return true if player did rebuy
     */
    public static boolean rebuy(PokerGame game, int nType, int nLevel)
    {
        PokerPlayer player = game.getHumanPlayer();

        // just a safety check for case where rebuy is pressed/triggered
        // before it can be removed
        if (player.isObserver() || player.isEliminated()) return false;

        TournamentProfile prof = game.getProfile();
        TournamentDirector td = (TournamentDirector) game.getGameContext().getGameManager();
        int nCost = prof.getRebuyCost();
        int nChips = prof.getRebuyChips();
        boolean bPending = player.isInHand();
        String sPending = "";
        if (bPending) sPending = PropertyConfig.getMessage("msg.dorebuy.pending");

        String sMsg = PropertyConfig.getMessage("msg.dorebuy."+nType,
                                                nCost, nChips,
                                                prof.getLastRebuyLevel(),
                        sPending);

        if (game.isOnlineGame() && PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_AUDIO))
        {
            AudioConfig.playFX("onlineact");
        }

        if (TESTING(PokerConstants.TESTING_AUTOPILOT) ||
            EngineUtils.displayCancelableConfirmationDialog(game.getGameContext(), sMsg, "msg.windowtitle.rebuy", null, null,
                                                            !game.isOnlineGame() ? 0 : 10))
        {

            td.doRebuy(player, nLevel, nCost, nChips, bPending);
            return true;
        }

        return false;
    }
    
    /**
     * Addon
     */
    private void addon()
    {
        TournamentProfile prof = game_.getProfile();
        TournamentDirector td = (TournamentDirector) context_.getGameManager();
        int nCost = prof.getAddonCost();
        int nChips = prof.getAddonChips();

        String sMsg = PropertyConfig.getMessage("msg.doaddon",
                                                nCost, nChips,
                                                prof.getAddonLevel());

        if (game_.isOnlineGame() && PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_AUDIO))
        {
            AudioConfig.playFX("onlineact");
        }

        if (TESTING(PokerConstants.TESTING_AUTOPILOT) ||
            EngineUtils.displayCancelableConfirmationDialog(context_, sMsg, "msg.windowtitle.addon", null, null,
                                                            !game_.isOnlineGame() ? 0 : 10))
        {
            PokerPlayer p = game_.getHumanPlayer();
            td.doAddon(p, nCost, nChips);
        }
    }
}
