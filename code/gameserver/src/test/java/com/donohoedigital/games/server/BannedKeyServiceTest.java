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
package com.donohoedigital.games.server;

import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.service.BannedKeyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2008
 * Time: 2:52:25 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(HackRunner.class)
@Transactional
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@ContextConfiguration(locations = {"/app-context-jpatests.xml"})
public class BannedKeyServiceTest
{
    @Autowired
    private BannedKeyService service;

    @Test
    @Rollback
    public void testLookupAndDelete()
    {
        String key = "0000-0000-1111-2222";
        BannedKey key1 = ServerTestData.createBannedKey(key);

        service.saveBannedKey(key1);
        assertTrue(service.isBanned(key));
        assertEquals(key, service.getIfBanned(key).getKey());
        assertTrue(service.isBanned("blah", key));

        service.deleteBannedKey(key);
        assertFalse(service.isBanned(key));
    }

    @Test
    @Rollback
    public void testBanByOldDate()
    {
        String key = "0000-0000-1111-2222";
        BannedKey key1 = ServerTestData.createBannedKey(key);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        key1.setUntil(cal.getTime());
        service.saveBannedKey(key1);
        assertFalse(service.isBanned(key));
        service.deleteBannedKey(key);
        assertFalse(service.isBanned(key));
    }

    @Test
    @Rollback
    public void testBanByTodaysDate()
    {
        String key = "0000-0000-1111-4444";
        BannedKey key1 = ServerTestData.createBannedKey(key);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        key1.setUntil(cal.getTime());
        service.saveBannedKey(key1);
        assertTrue(service.isBanned(key));
        service.deleteBannedKey(key);
        assertFalse(service.isBanned(key));
    }

    @Test
    @Rollback
    public void testBanByFutureDate()
    {
        String key = "0000-0000-1111-3333";
        BannedKey key1 = ServerTestData.createBannedKey(key);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 100);
        key1.setUntil(cal.getTime());
        service.saveBannedKey(key1);
        assertTrue(service.isBanned(key));
        assertTrue(service.isBanned(key, null)); // test null in params
        service.deleteBannedKey(key);
        assertFalse(service.isBanned(key));
    }

    @Test
    @Rollback
    public void testNull()
    {
        assertFalse(service.isBanned((String) null));
        assertFalse(service.isBanned(null, null));
        assertFalse(service.isBanned((String[]) null));
        assertNull(service.getIfBanned((String) null));
    }
}