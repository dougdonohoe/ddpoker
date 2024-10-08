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

import com.donohoedigital.config.ConfigUtils;
import com.donohoedigital.config.MatchingResources;
import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.model.OnlineProfile;
import org.apache.logging.log4j.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
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
@ContextConfiguration(locations = {"/app-context-pokerservertests.xml"})
public class OnlineProfileTest
{
    private final Logger logger = LogManager.getLogger(OnlineProfileTest.class);

    @Autowired
    private OnlineProfileDao dao;

    @Test
    @Rollback
    public void shouldPersist()
    {
        String sPassword = "foobar";
        OnlineProfile newProfile = PokerTestData.createOnlineProfile("TEST shouldPersist");
        newProfile.setPassword(sPassword);
        dao.save(newProfile);

        assertNotNull(newProfile.getId());

        OnlineProfile fetch = dao.get(newProfile.getId());
        assertEquals("name should match", newProfile.getName(), fetch.getName());
        assertEquals("passwords should match", sPassword, fetch.getPassword());

        String key = "1111-1111-1111-1111";
        newProfile.setLicenseKey(key);
        dao.update(newProfile);

        OnlineProfile updated = dao.get(newProfile.getId());
        assertEquals("key should match", key, updated.getLicenseKey());
    }

    @Test
    @Rollback
    public void saveBeforeDelete()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("TEST saveBeforeDelete");
        dao.save(profile);
        assertNotNull(profile.getId());
        logger.info(profile.getName() + " saved with id " + profile.getId());

        OnlineProfile lookup = dao.get(profile.getId());
        dao.delete(lookup);
        logger.info("Should have deleted profile with id " + lookup.getId());

        OnlineProfile delete = dao.get(lookup.getId());
        assertNull(delete);
    }

    @Test
    @Rollback
    public void testGetByName()
    {
        String name = "TEST getByName";
        OnlineProfile newProfile = PokerTestData.createOnlineProfile(name);
        dao.save(newProfile);

        OnlineProfile fetch = dao.getByName(name);
        assertNotNull(fetch);
        assertEquals(fetch.getName(), name);

        fetch = dao.getByName("no such name dude");
        assertNull(fetch);
    }

    @Test
    @Rollback
    public void testGetAllByKey()
    {
        String email1 = "dexter@example.com";
        String email2 = "zorro@example.com";
        String name = "Test all ";
        int total = 10;
        for (int i = 1; i <= total; i++)
        {
            OnlineProfile newProfile = PokerTestData.createOnlineProfile(name + i);
            newProfile.setEmail(i % 2 == 0 ? email1 : email2);
            dao.save(newProfile);
        }

        List<OnlineProfile> list1 = dao.getAllForEmail(email1, null);
        assertEquals(list1.size(), total / 2);

        for (OnlineProfile p : list1)
        {
            assertEquals(p.getEmail(), email1);
        }

        List<OnlineProfile> list2 = dao.getAllForEmail(email2, null);
        assertEquals(list2.size(), total / 2);

        for (OnlineProfile p : list2)
        {
            assertEquals(p.getEmail(), email2);
        }

        // test exclude behavior
        String nameFetch = name + 1;
        List<OnlineProfile> list3 = dao.getAllForEmail(email2, nameFetch);
        assertEquals(list3.size(), (total / 2) - 1);

        for (OnlineProfile p : list3)
        {
            assertNotEquals(p.getName(), nameFetch);
        }

        // test none found returns empty list
        List<OnlineProfile> list4 = dao.getAllForEmail("xxxx", null);
        assertTrue(list4 != null && list4.isEmpty());
    }

    @Test
    @Rollback
    public void testSearch()
    {
        String key1 = "aaaa-bbbb-cccc-dddd";
        String key2 = "zzzz-yyyy-wwww-xxxx";
        String email = "dexter@example.com";
        String name = "Find Me Special_Chars 100% \\/";
        int total = 10;
        for (int i = 1; i <= total; i++)
        {
            OnlineProfile newProfile = PokerTestData.createOnlineProfile(name + i);
            newProfile.setEmail(email);
            newProfile.setLicenseKey(i % 2 == 0 ? key1 : key2);
            dao.save(newProfile);
        }

        // add extra to test non-matching
        int nonmatchtotal = 5;
        for (int i = 1; i <= nonmatchtotal; i++)
        {
            OnlineProfile newProfile = PokerTestData.createOnlineProfile("TOTALLY DIFFERENT NAME" + i);
            newProfile.setEmail("foo@blah.com");
            newProfile.setLicenseKey("1234-4444-4444-4444");
            dao.save(newProfile);
        }
        int max = total + nonmatchtotal;

        List<OnlineProfile> list;

        list = dao.getMatching(null, 0, max, "Find", null, null, false);
        assertEquals(total, list.size());

        list = dao.getMatching(null, 0, max, null, "example", null, false);
        assertEquals(total, list.size());

        list = dao.getMatching(null, 0, max, "%", null, null, false);
        assertEquals(total, list.size());

        list = dao.getMatching(null, 0, max, "_", null, null, false);
        assertEquals(total, list.size());

        list = dao.getMatching(null, 0, max, "\\", null, null, false);
        assertEquals(total, list.size());

        list = dao.getMatching(null, 0, max, null, null, key1, false);
        assertEquals(total / 2, list.size());

        list = dao.getMatching(null, 0, max, "Me", null, key1, false);
        assertEquals(total / 2, list.size());

        list = dao.getMatching(null, 0, max, "blah", "noone", "3333", false);
        assertEquals(0, list.size());
    }

    @Test
    @Rollback
    public void testUTF8()
    {
        verifyFile("greek.utf8.txt");
        verifyFile("russian.utf8.txt");
        verifyFile("japanese.utf8.txt");
        verifyFile("chinese.utf8.txt");
        verifyFile("arabic.utf8.txt");
        verifyFile("swedish.utf8.txt");
    }

    private void verifyFile(String filename)
    {
        URL url = new MatchingResources("classpath:" + filename).getSingleRequiredResourceURL();
        String utf8 = ConfigUtils.readURL(url).trim();
        logger.debug(filename + ": " + utf8);
        OnlineProfile newProfile = PokerTestData.createOnlineProfile(utf8);
        dao.save(newProfile);
        assertNotNull(newProfile.getId());

        dao.flush();
        dao.clear();

        OnlineProfile fetch = dao.get(newProfile.getId());
        assertNotSame(newProfile, fetch);
        assertEquals("name should match", newProfile.getName(), fetch.getName());
        assertEquals(utf8, fetch.getName());
        logger.debug("Name: " + utf8);
    }

}