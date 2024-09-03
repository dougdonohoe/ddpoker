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

import com.donohoedigital.games.server.model.Registration;
import com.donohoedigital.games.server.service.RegistrationService;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Feb 17, 2008
 * Time: 9:08:21 PM
 * Simple Spring test - configure logging, load app context, get a bean
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/app-context-jpatests.xml"})
public class SpringCreatedServiceTest extends TestCase
{
    private final Logger logger = Logger.getLogger(SpringCreatedServiceTest.class);

    /**
     * Load app config
     */
    @Test
    public void testSpring()
    {
        // TODO: auto-wire service instead?
        String[] contextPaths = new String[] {"app-context-jpatests.xml"};
        ApplicationContext ctx = new ClassPathXmlApplicationContext(contextPaths);

        RegistrationService service = (RegistrationService) ctx.getBean("registrationService");

        Registration reg = ServerTestData.createRegistration("RegistrationServiceTest", "3333-3333-7777-6666");
        service.saveRegistration(reg);

        logger.info("Saved: " + reg);
        assertNotNull(reg.getId());

        service.deleteRegistration(reg);
    }
}