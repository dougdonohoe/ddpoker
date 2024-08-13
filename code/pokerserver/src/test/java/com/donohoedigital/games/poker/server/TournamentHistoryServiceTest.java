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

import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.service.TournamentHistoryService;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static com.donohoedigital.games.poker.model.TournamentHistory.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2008
 * Time: 2:52:25 PM
 * Test items in TournamentHistoryService that are more than pass-throughs to the TournamentHistory DAO
 */
@RunWith(HackRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = false)
@ContextConfiguration(locations = {"/app-context-pokerservertests.xml"})
public class TournamentHistoryServiceTest
{
    private static final Logger logger = Logger.getLogger(TournamentHistoryServiceTest.class);

    @Autowired
    private TournamentHistoryService histService;

    @Autowired
    private OnlineGameService gameService;

    @Autowired
    private OnlineProfileService profileService;

    @Test
    @Rollback
    public void testSave()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter");
        OnlineProfile guest1 = PokerTestData.createOnlineProfile("Zorro");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-999");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);
        profileService.saveOnlineProfile(guest1);

        TournamentHistoryList list = new TournamentHistoryList();

        // entry for host and guest
        list.add(PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE));
        list.add(PokerTestData.createTournamentHistory(guest1.getName(), PLAYER_TYPE_ONLINE));

        // some ai
        for (int i = 0; i < 4; i++)
        {
            list.add(PokerTestData.createTournamentHistory("AI #"+(i+1), PLAYER_TYPE_AI));
        }

        // some local
        for (int i = 0; i < 4; i++)
        {
            list.add(PokerTestData.createTournamentHistory("Local #"+(i+1), PLAYER_TYPE_LOCAL));
        }

        // a non-existent online
        list.add(PokerTestData.createTournamentHistory("Mystery Player", PLAYER_TYPE_ONLINE));

        // set places
        int place = list.size();
        for (TournamentHistory hist : list)
        {
            hist.setPlace(place);
            place--;
        }

        // insert them
        gameService.updateOnlineGame(game, list);

        // fetch
        TournamentHistoryList allForGame = histService.getAllTournamentHistoriesForGame(null, 0, list.size()*2, game.getId());
        assertEquals(allForGame.size(), list.size());
        assertEquals(allForGame.getTotalSize(), list.size());

        // spit them out - no easy way to verify, so eyeballing :-(
        for (TournamentHistory hist : allForGame)
        {
            logger.info("RETURN: " + hist);
            assertEquals(game.getTournament().getName(), hist.getTournamentName());
            assertEquals(list.size(), hist.getNumPlayers());
        }
    }
}