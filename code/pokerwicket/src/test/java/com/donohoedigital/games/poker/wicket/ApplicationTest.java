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
package com.donohoedigital.games.poker.wicket;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.wicket.pages.online.Search;
import com.donohoedigital.games.server.service.BannedKeyService;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.test.AnnotApplicationContextMock;
import org.apache.wicket.spring.test.ApplicationContextMock;
import org.apache.wicket.util.tester.WicketTester;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 12, 2008
 * Time: 10:47:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationTest extends TestCase
{
    Logger logger = Logger.getLogger(ApplicationTest.class);

    private WicketTester tester;

    private String searchString = "+";

    @Override
    public void setUp()
    {
        new ConfigManager("test", ApplicationType.WEBAPP);

        // test profile and aliases
        OnlineProfile profile = new OnlineProfile();
        profile.setName("Tahoe+Zorro");
        profile.setEmail("hibernate@example.com");
        profile.setLicenseKey("0000-0000-0000-0000");
        profile.setActivated(false);
        profile.setPassword("password");

        List<OnlineProfile> array = new ArrayList<OnlineProfile>(1);
        List<OnlineProfile> aliases = new ArrayList<OnlineProfile>(0);
        array.add(profile);

        // services
        OnlineProfileService onlineProfileService = createMock(OnlineProfileService.class);
        BannedKeyService bannedKeyService = createMock(BannedKeyService.class);
        OnlineGameService onlineGameService = createMock(OnlineGameService.class);

        // mock expected calls
        expect(onlineProfileService.getMatchingOnlineProfilesCount("doug", null, null, true)).andStubReturn(1);
        expect(onlineProfileService.getMatchingOnlineProfilesCount(searchString, null, null, true)).andStubReturn(1);
        //expectLastCall().atLeastOnce();
        expect(onlineProfileService.getMatchingOnlineProfiles(array.size(), 0, array.size(), "doug", null, null, true)).andStubReturn(array);
        expect(onlineProfileService.getMatchingOnlineProfiles(array.size(), 0, array.size(), "+", null, null, true)).andStubReturn(array);
        //expectLastCall().atLeastOnce();
        expect(onlineProfileService.getAllOnlineProfilesForEmail(profile.getEmail(), profile.getName())).andStubReturn(aliases);
        //expectLastCall().atLeastOnce();
        replay(onlineProfileService);

        // mock spring
        ApplicationContextMock appctx = new AnnotApplicationContextMock();
        appctx.putBean("onlineProfileService", onlineProfileService);
        appctx.putBean("bannedKeyService", bannedKeyService);
        appctx.putBean("onlineGameService", onlineGameService);

        // create app
        PokerWicketApplication wicketApplication = new PokerWicketApplication();
        wicketApplication.setApplicationContext(appctx);

        // our tester
        tester = new WicketTester(wicketApplication);
    }

    public void testSearch()
    {
        //start and render the test page
        tester.startPage(Search.class);

        logger.debug("Request URL: " + tester.getServletRequest().getRequestURL());

        // set search criteria
        tester.setParameterForNextRequest("form:name", "doug");
        tester.submitForm("form");

        // to get actual HTML
        tester.dumpPage();

        logger.debug("XXXXXXXXXXXXXXXXXXX ===== form submit ===== XXXXXXXXXXXXXXXXXXX");

        // set search criteria
        tester.setParameterForNextRequest("form:name", searchString);
        tester.submitForm("form");

        //
        logger.debug("Request URL: " + tester.getServletRequest().getRequestURL());
        tester.dumpPage();

    }
}
