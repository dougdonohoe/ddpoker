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

import com.donohoedigital.games.server.dao.BannedKeyDao;
import com.donohoedigital.games.server.dao.RegistrationDao;
import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.model.Registration;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
@ContextConfiguration(locations = {"/app-context-jpatests.xml"})
public class RegistrationTest
{
    private final Logger logger = LogManager.getLogger(RegistrationTest.class);

    @Autowired
    private RegistrationDao dao;

    @Autowired
    private BannedKeyDao bannedDao;

    @Test
    @Rollback
    public void shouldPersist()
    {
        Registration newProfile = ServerTestData.createRegistration("TEST shouldPersist", "5555-5555-5555-5555");
        dao.save(newProfile);

        assertNotNull(newProfile.getId());

        Registration fetch = dao.get(newProfile.getId());
        assertEquals("name should match", newProfile.getName(), fetch.getName());

        String key = "1111-1111-1111-1111";
        newProfile.setLicenseKey(key);
        dao.update(newProfile);

        Registration updated = dao.get(newProfile.getId());
        assertEquals("key should match", key, updated.getLicenseKey());
    }

    @Test
    @Rollback
    public void saveBeforeDelete()
    {
        Registration profile = ServerTestData.createRegistration("TEST saveBeforeDelete", "1234-4321-1234-4321");
        dao.save(profile);
        assertNotNull(profile.getId());
        logger.info(profile.getName() + " saved with id " + profile.getId());

        Registration lookup = dao.get(profile.getId());
        dao.delete(lookup);
        logger.info("Should have deleted registration with id " + lookup.getId());

        Registration delete = dao.get(lookup.getId());
        assertNull(delete);
    }

    @Test
    public void toStringFileTest()
    {
        Registration reg = ServerTestData.createRegistration("TEST, toStringFileTest", "1111-2222-2222-1111");
        logger.info("***** " + ToStringBuilder.reflectionToString(reg, RegistrationFileStringStyle.STYLE, false));
    }

    @Test
    @Rollback
    public void checkDuplicateRegistrations()
    {
        String save1key = "1111-1212-2323-3434";
        String save2key = "2222-2323-3434-5555";

        String differentKey = "aaaa-9999-9999-1234";

        String save1email = "duplicatetest@example.com";
        String differentEmail = "duplicatetest2@example.com";
        String differentIp = "255.255.255.0";

        Registration save1 = ServerTestData.createRegistration("TEST dup 1", save1key);
        Registration likeSave1 = ServerTestData.createRegistration("likeSave1", save1key);
        save1.setEmail(save1email);
        likeSave1.setEmail(save1email);

        Registration save2 = ServerTestData.createRegistration("TEST dup 2", save2key);
        Registration likeSave2 = ServerTestData.createRegistration("TEST dup 2", save2key);
        save2.setDuplicate(true);

        Registration different = ServerTestData.createRegistration("different", differentKey);

        // save objects
        dao.save(save1);
        assertNotNull(save1.getId());
        dao.save(save2);
        assertNotNull(save2.getId());

        // match exact
        assertTrue("Exact same should be a duplicate", dao.isDuplicate(likeSave1));

        // different email
        likeSave1.setEmail(differentEmail);
        assertTrue("Different email should still be a duplicate", dao.isDuplicate(likeSave1));

        likeSave1.setIp(differentIp);
        assertTrue("Different ip should still be a duplicate", dao.isDuplicate(likeSave1));

        likeSave1.setHostNameModified("modify.this.com");
        assertFalse("Different host, now should be no longer be a duplicate", dao.isDuplicate(likeSave1));

        // different registration type
        likeSave1.setEmail(save1.getIp());
        likeSave1.setHostNameModified(save1.getHostNameModified());
        likeSave1.setIp(save1.getIp());
        assertTrue("Revert back email/host/ip same should be a duplicate", dao.isDuplicate(likeSave1));
        likeSave1.setType(Registration.Type.ACTIVATION);
        assertTrue("Activation type should be a duplicate", dao.isDuplicate(likeSave1));
        likeSave1.setHostNameModified(null);
        assertTrue("null host should be a duplicate", dao.isDuplicate(likeSave1));

        // something marked as duplicate
        assertFalse("Marked as duplicate should not be a duplicate", dao.isDuplicate(likeSave2));

        // totally different key
        assertFalse("Different key should not be a duplicate", dao.isDuplicate(different));
    }

    @Test
    @Rollback
    public void checkDuplicateActivations()
    {
        String save1key = "1111-1212-2323-3434";
        String save2key = "2222-2323-3434-5555";
        String save3key = "9999-2323-3434-5555";

        String differentKey = "aaaa-9999-9999-1234";

        String save1Ip = "121.121.121.111";
        String differentIp = "255.255.255.0";

        Registration save1 = ServerTestData.createRegistration("TEST dup 1", save1key);
        Registration likeSave1 = ServerTestData.createRegistration("Like Save 1", save1key);
        save1.setIp(save1Ip);
        likeSave1.setIp(save1Ip);
        save1.setType(Registration.Type.ACTIVATION);
        likeSave1.setType(Registration.Type.ACTIVATION);

        Registration save2 = ServerTestData.createRegistration("TEST dup 2", save2key);
        Registration likeSave2 = ServerTestData.createRegistration("Like Save 2", save2key);
        save2.setType(Registration.Type.ACTIVATION);
        likeSave2.setType(Registration.Type.ACTIVATION);
        save2.setDuplicate(true);

        Registration save3 = ServerTestData.createRegistration("TEST dup 3", save3key);
        Registration likeSave3 = ServerTestData.createRegistration("Like Save 3", save3key);
        save3.setHostNameModified(null);
        save3.setType(Registration.Type.ACTIVATION);
        save3.setIp(save1Ip);
        likeSave3.setHostNameModified(null);
        likeSave3.setType(Registration.Type.ACTIVATION);
        likeSave3.setIp(save1Ip);

        Registration different = ServerTestData.createRegistration("different", differentKey);
        different.setType(Registration.Type.ACTIVATION);

        // save objects
        dao.save(save1);
        assertNotNull(save1.getId());
        dao.save(save2);
        assertNotNull(save2.getId());
        dao.save(save3);
        assertNotNull(save3.getId());

        // match exact
        assertTrue("Exact same should be a duplicate", dao.isDuplicate(likeSave1));

        // different ip/null host
        likeSave1.setIp(differentIp);
        likeSave1.setHostNameModified(null);
        assertFalse("Different ip (null host) should not be a duplicate", dao.isDuplicate(likeSave1));

        // different ip/same host
        likeSave1.setIp(differentIp);
        likeSave1.setHostNameModified(save1.getHostNameModified());
        assertTrue("Different ip (same host) should be a duplicate", dao.isDuplicate(likeSave1));

        // different ip/different host
        likeSave1.setIp(differentIp);
        likeSave1.setHostNameModified("extra." + save1.getHostName());
        assertFalse("Different ip (diff host) should not be a duplicate", dao.isDuplicate(likeSave1));

        // different registration type
        likeSave1.setType(Registration.Type.REGISTRATION);
        assertFalse("Registration type should not be a duplicate", dao.isDuplicate(likeSave1));

        // something marked as duplicate
        assertFalse("Marked as duplicate should not be a duplicate", dao.isDuplicate(likeSave2));

        // null host should be duplicate
        assertTrue("Null host in db should be duplicate", dao.isDuplicate(likeSave3));
        likeSave3.setIp(differentIp);
        assertFalse("Null host in db, different ip should not be a duplicate", dao.isDuplicate(likeSave3));

        // totally different key
        assertFalse("Different key should not be a duplicate", dao.isDuplicate(different));
    }

    @Test
    @Rollback
    public void checkMarkDuplicates()
    {
        String save1key = "1111.5555.7777.7777";
        Registration save1 = ServerTestData.createRegistration("TEST act 1", save1key);
        save1.setType(Registration.Type.ACTIVATION);

        Registration save2 = ServerTestData.createRegistration("TEST act 2", save1key);

        // save objects
        dao.save(save1);
        assertNotNull(save1.getId());

        // should not be a duplicate
        assertFalse(dao.isDuplicate(save2));

        // mark duplicates of save2
        dao.markDuplicates(save2);

        // reload to verify save1 was marked a duplicate
        dao.refresh(save1);
        assertTrue(save1.isDuplicate());
    }

    @Test
    @Rollback
    public void testSelectBanned()
    {
        String okay = "1111-2222-3333-4444";
        String ban = "2222-3333-4444-5555";

        BannedKey banned = ServerTestData.createBannedKey(ban);
        bannedDao.save(banned);

        int total = 100;
        for (int i = 0; i < total; i++)
        {
            dao.save(ServerTestData.createRegistration("TEST " + i, i % 2 == 0 ? okay : ban));
        }

        List<Registration> list = dao.getAllBanned();
        assertEquals(list.size(), total / 2);

        for (Registration r : list)
        {
            assertEquals(r.getLicenseKey(), ban);
        }
    }

    @Test
    @Rollback
    public void testSelectSuspect()
    {
        String okay = "1111-2222-3333-4444";
        String ban = "2222-3333-4444-5555";
        String suspect = "5555-3333-2222-111"; // last digit filled in below

        BannedKey banned = ServerTestData.createBannedKey(ban);
        bannedDao.save(banned);

        dao.save(ServerTestData.createRegistration("Okay", okay));

        int totalBan = 5;
        for (int i = 0; i < totalBan; i++)
        {
            dao.save(ServerTestData.createRegistration("TEST " + i, ban));
        }

        int numSuspect = 10;
        for (int i = 0; i < numSuspect; i++)
        {
            // enter i+1 registrations for each suspect key
            // thus there will be 1,2,3,4,5,6,...
            for (int j = 0; j < (i + 1); j++)
            {
                dao.save(ServerTestData.createRegistration("TEST " + i + "-" + j, suspect + i));
            }
        }

        for (int nMin = 2; nMin <= 8; nMin++)
        {
            int expected = numSuspect - nMin + 1;
            List<String> list = dao.getAllSuspectKeys(nMin);
            assertEquals("nMin:" + nMin + " size=" + list.size() + " matches expected=" + expected, list.size(), expected);

            for (String key : list)
            {
                assertTrue(key.startsWith(suspect));
            }
        }
    }

    @Test
    @Rollback
    public void testGetAllForKey()
    {
        String one = "1111-2222-3333-4444";
        String two = "2222-3333-4444-5555";

        int total = 100;
        for (int i = 0; i < total; i++)
        {
            dao.save(ServerTestData.createRegistration("TEST " + i, i % 2 == 0 ? one : two));
        }

        List<Registration> list = dao.getAllForKey(one);
        assertEquals(list.size(), total / 2);

        for (Registration r : list)
        {
            assertEquals(r.getLicenseKey(), one);
        }

        list = dao.getAllForKey(two);
        assertEquals(list.size(), total / 2);

        for (Registration r : list)
        {
            assertEquals(r.getLicenseKey(), two);
        }
    }
}