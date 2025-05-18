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
package com.donohoedigital.games.poker.wicket.admin;

import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.PokerUser;
import com.donohoedigital.games.poker.wicket.admin.pages.AdminLogin;
import com.donohoedigital.games.poker.wicket.admin.pages.AdminPokerPage;
import org.apache.wicket.Page;
import org.apache.wicket.authorization.strategies.page.SimplePageAuthorizationStrategy;

/**
 * @author Doug Donohoe
 */
public class AdminAuthorizationStrategy extends SimplePageAuthorizationStrategy
{
    private static final Class< ? extends Page> login = AdminLogin.class;

    /**
     * Construct.
     */
    public AdminAuthorizationStrategy()
    {
        super(AdminPokerPage.class, login);
    }

    /**
     * Authorized if user is logged in and an admin user
     */
    @Override
    protected boolean isAuthorized()
    {
        PokerUser user = PokerSession.get().getLoggedInUser();

        return (user != null && user.isAdmin() && user.isAuthenticated());
    }

    /**
     * @see org.apache.wicket.authorization.strategies.page.AbstractPageAuthorizationStrategy#isPageAuthorized(Class)
     */
    @Override
    protected <T extends Page> boolean isPageAuthorized(final Class<T> pageClass)
    {
        // allow if it is the login page
        if (instanceOf(pageClass, login))
		{
			return true;
		}

        return super.isPageAuthorized(pageClass);
    }
}
