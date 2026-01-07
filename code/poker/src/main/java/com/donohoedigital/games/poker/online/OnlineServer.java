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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.model.*;
import static com.donohoedigital.games.poker.model.TournamentHistory.*;
import com.donohoedigital.games.poker.network.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * Provides access to server online game services.
 * <p/>
 * TODO: game and history services
 */
public class OnlineServer
{
    private static Logger logger = LogManager.getLogger(OnlineServer.class);

    private static OnlineServer manager_ = new OnlineServer();

    /**
     * Get a manager instance.
     *
     * @return the manager instance
     */
    public static OnlineServer getWanManager()
    {
        return manager_;
    }

    /**
     * Add the game to the public list
     */
    public boolean addWanGame(PokerGame game, PlayerProfile profile)
    {
        // Wrap the game info
        GameContext context = game.getGameContext();
        OnlineGame onlineGame = createOnlineGame(context);
        onlineGame.setHostPlayer(game.getHost().getName());
        onlineGame.setMode(OnlineGame.MODE_REG);
        onlineGame.setTournament(game.getProfile());

        // Send a message requesting that the game be added

        TypedHashMap hmParams = new TypedHashMap();
        hmParams.setInteger(SendWanGame.PARAM_CATEGORY, OnlineMessage.CAT_WAN_GAME_ADD);
        hmParams.setObject(SendWanGame.PARAM_GAME, onlineGame);
        hmParams.setObject(SendWanGame.PARAM_AUTH, profile.toOnlineProfile());
        SendMessageDialog dialog = (SendMessageDialog) context.processPhaseNow("SendWanGame", hmParams);

        return (dialog.getStatus() == DDMessageListener.STATUS_OK);
    }


    /**
     * Remove the game from the public list
     *
     * @param game
     */
    public void removeWanGame(PokerGame game)
    {
        // send a message requesting that the game be deleted - not done in a dialog
        // since failing to remove the game should not affect the user interaction
        OnlineMessage reqOnlineMsg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_REMOVE);
        OnlineGame onlineGame = createOnlineGame(game.getGameContext());
        logger.debug("Sending game: " + onlineGame);
        reqOnlineMsg.setWanGame(onlineGame.getData());
        EngineMessage reqEngineMsg = new EngineMessage();
        reqOnlineMsg.getData().copyTo(reqEngineMsg);

        sendEngineMessage(reqEngineMsg);
    }

    /**
     * Update the current WAN game's profile
     */
    public void updateGameProfile(PokerGame game)
    {
        // Send a message requesting that the game be updated.
        OnlineGame onlineGame = createOnlineGame(game.getGameContext());
        OnlineMessage reqOnlineMsg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_UPDATE);
        onlineGame.setTournament(game.getProfile());
        //logger.debug("Updating game: " + onlineGame);
        reqOnlineMsg.setWanGame(onlineGame.getData());
        EngineMessage reqEngineMsg = new EngineMessage();
        reqOnlineMsg.getData().copyTo(reqEngineMsg);

        sendEngineMessage(reqEngineMsg);
    }

    /**
     * Start the current WAN game.
     *
     * @param game current poker game
     */
    public void startGame(PokerGame game)
    {
        // Send a message requesting that the game be updated and the histories added.
        int category = OnlineMessage.CAT_WAN_GAME_START;
        OnlineGame onlineGame = getServerGame(game.getGameContext(), category);
        onlineGame.setTournament(game.getProfile());
        OnlineMessage reqOnlineMsg = new OnlineMessage(category);
        //logger.debug("Starting game: " + onlineGame);
        reqOnlineMsg.setWanGame(onlineGame.getData());
        EngineMessage reqEngineMsg = new EngineMessage();
        reqOnlineMsg.getData().copyTo(reqEngineMsg);

        sendEngineMessage(reqEngineMsg);
    }

    /**
     * End the current WAN game.
     *
     * @param game current poker game
     */
    public void endGame(PokerGame game, boolean bDone)
    {
        // Create tournament history objects for all players.
        TournamentHistory history = null;
        DMArrayList<TournamentHistory> histories = null;
        PokerPlayer player = null;

        // important to go in order so place is correct
        // and also so DDR1 is calculated correct (assumes
        // TournamentHistories are in order in WanHistoryList)
        List<PokerPlayer> players = game.getPlayersByRank();
        int playerCount = players.size();
        int nRank = 0, nChips, nLastChips = -1;

        for (int i = 0; i < playerCount; ++i)
        {
            player = players.get(i);
            nChips = player.getChipCount();

            // if no place, game still in progress, so rank
            // is based on chip count - allow for ties
            if (player.getPlace() == 0)
            {
                if (nChips != nLastChips)
                {
                    nRank = (i + 1);
                }
                nLastChips = nChips;
            }
            else
            {
                nRank = (i + 1);
            }

            if (histories == null)
            {
                histories = new DMArrayList<TournamentHistory>();
            }

            history = createTournamentHistory(game, player, nRank);
            histories.add(history);
        }

        // Send a message requesting that the game be updated and the histories added.
        int category = bDone ? OnlineMessage.CAT_WAN_GAME_END : OnlineMessage.CAT_WAN_GAME_STOP;
        OnlineGame onlineGame = getServerGame(game.getGameContext(), category);
        OnlineMessage reqOnlineMsg = new OnlineMessage(category);
        logger.debug("Sending game: " + onlineGame);
        reqOnlineMsg.setWanGame(onlineGame.getData());
        logger.debug("Sending histories: " + histories);
        if (histories != null) reqOnlineMsg.setWanHistories(histories);
        EngineMessage reqEngineMsg = new EngineMessage();
        reqOnlineMsg.getData().copyTo(reqEngineMsg);

        sendEngineMessage(reqEngineMsg);
    }

    private TournamentHistory createTournamentHistory(PokerGame game, PokerPlayer player, int nRank)
    {
        TournamentHistory hist = new TournamentHistory();
        hist.setTournamentName(game.getProfile().getName());
        hist.setPlayerName(player.getName());
        hist.setPlayerType(player.isComputer() ? PLAYER_TYPE_AI : (player.isOnlineActivated() ? PLAYER_TYPE_ONLINE : PLAYER_TYPE_LOCAL));
        hist.setEndDate(new Date());
        hist.setBuyin(player.getBuyin());
        hist.setAddon(player.getAddon());
        hist.setRebuy(player.getRebuy());
        hist.setDisconnects(player.getDisconnects());
        hist.setPlacePrizeNumPlayers(player.getPlace(), player.getPrize(), game.getNumPlayers(), nRank, player.getChipCount());
        return hist;
    }

    /**
     * Get an initialized game object for sending to the server.
     */
    private OnlineGame getServerGame(GameContext context, int category)
    {
        // Determine the new mode and set the start/end date.
        OnlineGame game = createOnlineGame(context);
        int mode = -1;
        Date date = new Date();

        switch (category)
        {
            case OnlineMessage.CAT_WAN_GAME_START:
                mode = OnlineGame.MODE_PLAY;
                game.setStartDate(date);
                break;
            case OnlineMessage.CAT_WAN_GAME_STOP:
                mode = OnlineGame.MODE_STOP;
                game.setEndDate(date);
                break;
            case OnlineMessage.CAT_WAN_GAME_END:
                mode = OnlineGame.MODE_END;
                game.setEndDate(date);
                break;
            default:
                return game;
        }

        game.setMode(mode);

        return game;
    }

    /**
     * Create a new online game based on the given context.
     */
    private OnlineGame createOnlineGame(GameContext context)
    {
        PokerMain main = (PokerMain) GameEngine.getGameEngine();
        OnlineGame game = new OnlineGame();

        game.setLicenseKey(main.getRealLicenseKey());
        game.setUrl(((PokerGame) context.getGame()).getPublicConnectURL());

        return game;
    }

    /**
     * Send the given engine message and log any errors.
     *
     * @param reqMsg message
     */
    private void sendEngineMessage(EngineMessage reqMsg)
    {
        EngineMessage resMsg = GameMessenger.SendEngineMessage(null, reqMsg, null);

        if (resMsg.getStatus() == DDMessageListener.STATUS_APPL_ERROR)
        {
            logger.error("WAN Game server error: " + resMsg.getApplicationErrorMessage());
        }
    }
}
