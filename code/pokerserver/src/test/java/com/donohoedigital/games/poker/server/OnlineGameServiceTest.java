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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.service.OnlineGameService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2008
 * Time: 2:52:25 PM
 * Test items in OnlineGameService that are more than pass-throughs to the OnlineGame DAO
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(locations = {"/app-context-pokerservertests.xml"})
public class OnlineGameServiceTest
{
    @Autowired
    private OnlineGameService service;

    @Test
    @Rollback
    public void testSave()
    {
        String sKey = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(sKey);
        service.saveOnlineGame(newGame);
        assertNotNull(newGame.getId());

        // create dup with same key/url, but different name
        OnlineGame dupGame = PokerTestData.createOnlineGame("dupGame", 1, "XXX-333");
        dupGame.setLicenseKey(sKey);
        service.saveOnlineGame(dupGame);
        assertNotEquals(newGame.getId(), dupGame.getId());

        // first game should be deleted
        assertNull(service.getOnlineGameById(newGame.getId()));
    }

    @Test
    @Rollback
    public void testUpdate()
    {
        String sKey = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(sKey);
        service.saveOnlineGame(newGame);
        assertNotNull(newGame.getId());

        // create dup with same key/url, but different name
        OnlineGame dupGame = PokerTestData.createOnlineGame("dupGame", 1, "XXX-333");
        dupGame.setLicenseKey(sKey);
        dupGame = service.updateOnlineGame(dupGame);
        assertEquals(newGame.getId(), dupGame.getId());

        // fetch to make sure name updated
        OnlineGame fetch = service.getOnlineGameById(dupGame.getId());
        assertEquals(fetch.getHostPlayer(), dupGame.getHostPlayer());
    }

    @Test
    @Rollback
    public void testUpdateDoesNotInsert()
    {
        String sKey = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(sKey);
        newGame = service.updateOnlineGame(newGame);
        assertNull(newGame);
    }

    @Test
    @Rollback
    public void testDelete()
    {
        String sKey = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(sKey);
        service.saveOnlineGame(newGame);
        assertNotNull(newGame.getId());

        // create dup with same key/url, but different name
        OnlineGame dupGame = PokerTestData.createOnlineGame("dupGame", 1, "XXX-333");
        dupGame.setLicenseKey(sKey);
        service.deleteOnlineGame(dupGame);

        // first game should be deleted
        assertNull(service.getOnlineGameById(newGame.getId()));
    }
}