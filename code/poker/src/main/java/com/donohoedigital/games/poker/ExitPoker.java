/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * ExitPoker.java
 *
 * Created on May 11, 2005, 1:43 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.online.*;
import org.apache.logging.log4j.*;

/**
 * @author Doug Donohoe
 */
public class ExitPoker extends BasePhase
{
    static Logger logger = LogManager.getLogger(ExitPoker.class);

    private PokerGame game_;
    private boolean bCancelFromJoin_ = false;

    @Override
    public void start()
    {
        boolean bConfirm = gamephase_.getBoolean("confirm", true);

        String sConfirm = gamephase_.getString("msg-key", getDefaultKey());
        String sDetailsKey = null;
        String sNoShowKey = getDefaultNoShowKey(); // if set to null, then no checkbox displayed and no way to opt-out
        String sParam1 = null;

        // pause TD while asking this question
        TournamentDirector td = (TournamentDirector) context_.getGameManager();
        if (td != null)
        {
            td.setPaused(true);
        }

        // determine confirmation message - if game is online game and the player has a real id (a temp id means
        // quit was clicked when a join-failure message was displayed)
        game_ = (PokerGame) context_.getGame();
        if (game_ != null && !game_.isClockMode())
            bCancelFromJoin_ = game_.getLocalPlayer().getID() == PokerConstants.PLAYER_ID_TEMP;
        if (game_ != null && game_.isOnlineGame() && !bCancelFromJoin_)
        {
            switch (game_.getOnlineMode())
            {
                case PokerGame.MODE_INIT:
                case PokerGame.MODE_NONE:
                    break;

                case PokerGame.MODE_CLIENT:
                    // client in lobby if game not in progress.  Exception:
                    // host started count-down, in which case game not in progress,
                    // but we know this because we have tables.  In this case, show
                    // other message since player is considered part of game now
                    if (!game_.isInProgress() && game_.getNumTables() == 0)
                    {
                        // Note: QuitPokerGame not available in lobby, so this is okay considering subclassing
                        sConfirm = "msg.confirm.lobby.client.exit";
                        sNoShowKey = null;
                    }
                    else
                    {
                        PokerPlayer human = game_.getHumanPlayer();
                        if (human.isObserver())
                        {
                            // use default message for observers and allow no-show checkbox to be displayed/used
                        }
                        else
                        {
                            sDetailsKey = game_.isGameOver() ? null : "msg.confirm.client";
                            sNoShowKey = null;
                        }
                    }

                    break;

                case PokerGame.MODE_REG:
                    // Note: QuitPokerGame not available in lobby, so this is okay considering subclassing
                    sConfirm = "msg.confirm.lobby.host.exit";
                    bConfirm = true;
                    sNoShowKey = null;
                    break;

                case PokerGame.MODE_PLAY:
                    sDetailsKey = game_.isGameOver() ? "msg.confirm.host.gameover" : "msg.confirm.host";
                    bConfirm = true;
                    sNoShowKey = null;
                    sParam1 = game_.getLastGameState().getGameName();
                    break;
            }
        }

        // confirm exit
        String sDetails = "";
        if (sDetailsKey != null) sDetails = PropertyConfig.getMessage(sDetailsKey, getActionDisplay(), sParam1);
        String sMsg = PropertyConfig.getMessage(sConfirm, sDetails);
        if (!bConfirm || EngineUtils.displayConfirmationDialog(context_, sMsg, sNoShowKey))
        {
            doCleanup();
            doQuitExit();
        }
        else
        {
            // no exit - unpause TD
            if (td != null)
            {
                td.setPaused(false);
            }
        }
    }

    /**
     * Exit game, clean up as necessary
     */
    protected void doCleanup()
    {
        if (game_ != null && game_.isOnlineGame() && !bCancelFromJoin_)
        {
            OnlineManager mgr = game_.getOnlineManager();
            if (mgr == null) return;

            switch (game_.getOnlineMode())
            {
                case PokerGame.MODE_INIT:
                case PokerGame.MODE_NONE:
                    break;

                case PokerGame.MODE_CLIENT:
                    mgr.quitGame();
                    break;

                case PokerGame.MODE_REG:
                case PokerGame.MODE_PLAY:
                    stopWanGame();
                    mgr.cancelGame();
                    break;

            }
        }
    }

    protected String getActionDisplay()
    {
        return PropertyConfig.getMessage("msg.confirm.exit");
    }

    protected String getDefaultNoShowKey()
    {
        return "exitconfirm";
    }

    protected String getDefaultKey()
    {
        return "msg.exit.confirm";
    }


    protected void doQuitExit()
    {
        engine_.exit(0);
    }

    /**
     * Stop the WAN game.
     */
    private void stopWanGame()
    {
        // No server processing if not the host, not a public game, or game has already ended.
        if ((game_.getOnlineMode() != PokerGame.MODE_PLAY) || !(game_.isPublic()) || (game_.isGameOver()))
        {
            return;
        }

        // Send a message requesting that the game be ended and results stored.
        OnlineServer manager = OnlineServer.getWanManager();
        manager.endGame(game_, false);
    }
}
