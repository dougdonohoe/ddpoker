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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
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
 * <p/>
 * Test items in OnlineProfileService that are more than pass-throughs to the OnlineProfile DAO
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(locations = {"/app-context-pokerservertests.xml"})
public class OnlineProfileServiceTest
{
    @Autowired
    private OnlineProfileService service;

    @Test
    @Rollback
    public void testDisallowed()
    {
        // assumes disallowed.txt is read, should be false
        assertFalse(service.isNameValid("ddpoker"));
        assertFalse(service.isNameValid("???"));
    }

    @Test
    @Rollback
    public void testSave()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter");
        assertTrue(service.saveOnlineProfile(profile));
        assertNotNull(profile.getId());

        // check dup fails
        OnlineProfile dup = PokerTestData.createOnlineProfile("Dexter");
        assertFalse(service.saveOnlineProfile(dup));
    }

    @Test
    @Rollback
    public void testAuth()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter");
        assertTrue(service.saveOnlineProfile(profile));
        assertNotNull(profile.getId());

        // check auth
        OnlineProfile dup = PokerTestData.createOnlineProfile("Dexter");
        assertNotNull(service.authenticateOnlineProfile(dup));

        // check without password
        dup.setPassword(null);
        assertNull(service.authenticateOnlineProfile(dup));

        // check different name
        dup.setName("Zorro");
        assertNull(service.authenticateOnlineProfile(dup));
    }
}