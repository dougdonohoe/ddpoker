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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.model.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2008
 * Time: 12:24:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class PokerTestData
{
    public static OnlineGame createOnlineGame(String host, int urlGameid, String urlPassword)
    {
        TournamentProfile tp = new TournamentProfile("Test Tournament");
        OnlineGame game = new OnlineGame();
        game.setLicenseKey("0000-0000-0000-0000");
        game.setHostPlayer(host);
        game.setMode(OnlineGame.MODE_PLAY);
        game.setUrl("poker://192.1.1.100:11885/n-"+urlGameid+ '/' +urlPassword);
        game.setTournament(tp);
        game.setStartDate(new Date());
        game.setEndDate(new Date());
        return game;
    }

    public static OnlineProfile createOnlineProfile(String sName)
    {
        OnlineProfile profile = new OnlineProfile();
        profile.setName(sName);
        profile.setEmail("hibernate@example.com");
        profile.setLicenseKey("0000-0000-0000-0000");
        profile.setActivated(false);
        profile.setPassword("password");
        return profile;
    }

    public static TournamentHistory createTournamentHistory(String sName, int urlGameId, String urlPassword)
    {
        OnlineProfile profile = createOnlineProfile(sName);
        OnlineGame game = createOnlineGame(sName, urlGameId, urlPassword);
        return createTournamentHistory(game, profile, sName);
    }

    public static TournamentHistory createTournamentHistory(String sName, int nPlayerType)
    {
        return createTournamentHistory(null, null, sName, nPlayerType);
    }

    public static TournamentHistory createTournamentHistory(OnlineGame game, OnlineProfile profile,
                                                            String sName)
    {
        return createTournamentHistory(game, profile, sName, TournamentHistory.PLAYER_TYPE_ONLINE);
    }

    public static TournamentHistory createTournamentHistory(OnlineGame game, OnlineProfile profile,
                                                            String sName, int nPlayerType)
    {
        TournamentHistory history = new TournamentHistory();
        history.setProfile(profile);
        history.setGame(game);
        history.setTournamentName(game != null ? game.getTournament().getName() : "Tournament by " + sName);
        history.setNumPlayers(1);
        history.setBuyin(10000);
        history.setAddon(25000);
        history.setRebuy(5000);
        history.setDisconnects(10);
        history.setEnded(true);
        history.setPlace(5);
        history.setPrize(100000);
        history.setRank1(4433);
        history.setPlayerName(sName);
        history.setPlayerType(nPlayerType);
        history.setEndDate(new Date());
        return history;
    }
}
