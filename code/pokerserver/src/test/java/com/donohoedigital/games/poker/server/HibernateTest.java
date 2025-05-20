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

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.model.OnlineProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 10, 2008
 * Time: 10:37:50 AM
 *
 * Test of Hibernate/JPA without Spring.
 */
public class HibernateTest extends TestCase
{
    @SuppressWarnings({"RawUseOfParameterizedType"})
    public void testHibernate() throws FileNotFoundException
    {
        Logger logger = LogManager.getLogger(HibernateTest.class);

        // "poker" is from persistence.xml (aka the persistence unit)
        // emf is a singleton (thread-safe)
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("poker");

        // create / update
        // em's are single-threaded
        EntityManager insEm = emf.createEntityManager();
        EntityTransaction insTx = insEm.getTransaction();
        insTx.begin();

        OnlineProfile profile = new OnlineProfile();
        profile.setName("Hibernate Test");
        profile.setEmail("hibernate@example.com");
        profile.setLicenseKey("0000-0000-0000-0000");
        profile.setActivated(false);
        profile.setPassword("password");

        insEm.persist(profile);
        logger.info("Id after commit: " + profile.getId());

        // sleep and update date
        Utils.sleepSeconds(1);
        profile.setActivated(true);

        // call not needed since all updates are automatically tracked
        // the above change is committed to DB when commit() is called.
        //em.merge(profile);

        insTx.commit();
        insEm.close();

        // fetch
        EntityManager fetchEm = emf.createEntityManager();
        EntityTransaction fetchTx = fetchEm.getTransaction();
        fetchTx.begin();

        OnlineProfile lookup = fetchEm.find(OnlineProfile.class,  profile.getId());
        assertEquals(lookup, profile);

        List profiles = fetchEm.createQuery("select p from OnlineProfile p " +
                                            "where p.name like '%Hib%' " +
                                            "order by p.name asc").getResultList();

        logger.info(profiles.size() + " profile(s) found");
        for (Object p : profiles)
        {
            OnlineProfile wp = (OnlineProfile) p;
            logger.info(wp);
        }

        fetchTx.commit();
        fetchEm.close();

        /// delete
        EntityManager delEm = emf.createEntityManager();
        EntityTransaction delTx = delEm.getTransaction();
        delTx.begin();

        // need to merge since profile is now detached ... it returns a new instance
        profile = delEm.merge(profile);
        delEm.remove(profile);

        delTx.commit();
        delEm.close();

        // close factory
        emf.close();
    }
}
