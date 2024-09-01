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

import com.donohoedigital.games.server.dao.UpgradedKeyDao;
import com.donohoedigital.games.server.model.UpgradedKey;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;


/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 13, 2008
 * Time: 2:44:16 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = false)
@ContextConfiguration(locations = {"/app-context-jpatests.xml"})
public class UpgradedKeyTest
{
    private final Logger logger = Logger.getLogger(UpgradedKeyTest.class);

    @Autowired
    private UpgradedKeyDao dao;

    @Test
    @Rollback
    public void shouldPersist()
    {
        UpgradedKey newKey = ServerTestData.createUpgradedKey("0000-0000-1111-2222");
        dao.save(newKey);

        assertNotNull(newKey.getId());

        UpgradedKey fetch = dao.get(newKey.getId());
        assertEquals("name should match", newKey.getLicenseKey(), fetch.getLicenseKey());

        String key = "1111-1111-1111-1111";
        newKey.setLicenseKey(key);
        dao.update(newKey);

        UpgradedKey updated = dao.get(newKey.getId());
        assertEquals("key should match", key, updated.getLicenseKey());
    }

    @Test
    @Rollback
    public void saveBeforeDelete()
    {
        UpgradedKey upgradedKey = ServerTestData.createUpgradedKey("9999-8888-7777-6666");
        dao.save(upgradedKey);
        assertNotNull(upgradedKey.getId());
        logger.info(upgradedKey.getLicenseKey() + " saved with id " + upgradedKey.getId());

        UpgradedKey lookup = dao.get(upgradedKey.getId());
        dao.delete(lookup);
        logger.info("Should have deleted profile with id " + lookup.getId());

        UpgradedKey delete = dao.get(lookup.getId());
        assertNull(delete);
    }

        @Test
    @Rollback
    public void loadAll()
    {
        // empty db should not return null
        assertNotNull(dao.getAll());
        
        UpgradedKey key1 = ServerTestData.createUpgradedKey("0000-0000-1111-2222");
        UpgradedKey key2 = ServerTestData.createUpgradedKey("0000-0000-1111-3333");

        dao.save(key1);
        dao.save(key2);

        List<UpgradedKey> list = dao.getAll();
        for (UpgradedKey key : list)
        {
            logger.info("Loaded: " + key);
        }

        assertTrue(list.contains(key1));
        assertTrue(list.contains(key2));
    }

    @Test
    @Rollback
    public void testFindByKey()
    {
        String sKey = "0000-0000-1111-2222";
        UpgradedKey newKey = ServerTestData.createUpgradedKey(sKey);
        dao.save(newKey);
        assertNotNull(newKey.getId());

        UpgradedKey fetch = dao.getByKey(sKey);
        assertNotNull(fetch);
    }
}