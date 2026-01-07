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
 * PreShowdown.java
 *
 * Created on July 12, 2005, 8:39 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.engine.ChainPhase;
import com.donohoedigital.games.engine.EngineUtils;
import com.donohoedigital.games.poker.online.TournamentDirector;

/**
 *
 * @author  Doug Donohoe
 */
public class PreShowdown extends ChainPhase
{
    //static Logger logger = LogManager.getLogger(PreShowdown.class);

    public static final String PARAM_WINNERS = "winners";

    /**
     * display cards
     */
    public void process()
    {
        PokerGame game = (PokerGame) context_.getGame();
        TournamentDirector td = (TournamentDirector) context_.getGameManager();
        PokerPlayer player = game.getLocalPlayer();

        // ask show/win question
        if (game.isOnlineGame())
        {
            // get list of winners ... if it is not null and contains the id of
            // the local player, then that player is a winner
            DMArrayList winners = (DMArrayList) gamephase_.getObject(PARAM_WINNERS);
            boolean bWinner = false;
            if (winners != null && winners.contains(player.getID()))
            {
                bWinner = true;
            }

            if (bWinner)
            {
                GameButton bShow = EngineUtils.displayConfirmationDialogCustom(context_, "ShowWinning",null,null,null,null);
                if (bShow != null && bShow.getName().startsWith("yes"))
                {
                    player.setShowWinning(true);
                    td.playerUpdate(player, player.getOnlineSettings());
                }
            }
            else
            {
                GameButton bShow = EngineUtils.displayConfirmationDialogCustom(context_, "ShowLosing",null,null,null,null);
                if (bShow != null && bShow.getName().startsWith("yes"))
                {
                    player.setMuckLosing(false);
                    td.playerUpdate(player, player.getOnlineSettings());
                }
            }
        }

        // notify tournament director that the question has been answered
        if (td != null ) td.removeFromWaitList(player);
    }
}
