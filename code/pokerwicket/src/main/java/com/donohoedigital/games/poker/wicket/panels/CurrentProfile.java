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
package com.donohoedigital.games.poker.wicket.panels;

import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.PokerUser;
import com.donohoedigital.games.poker.wicket.util.LoginUtils;
import com.donohoedigital.wicket.behaviors.HideShowSwapLabel;
import com.donohoedigital.wicket.components.VoidContainer;
import com.donohoedigital.wicket.components.VoidPanel;
import com.donohoedigital.wicket.labels.StringLabel;
import com.donohoedigital.wicket.models.StringModel;
import com.donohoedigital.wicket.pages.BasePage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 19, 2008
 * Time: 8:05:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class CurrentProfile extends VoidPanel
{
    //private static Logger logger = LogManager.getLogger(CurrentProfile.class);

    private static final long serialVersionUID = 42L;

    private final Login loginPanel;
    private final StringLabel loginLabel;
    private final boolean loginVisibleAtStart;

    public CurrentProfile(String id, boolean loginVisibleAtStart)
    {
        super(id);

        this.loginVisibleAtStart = loginVisibleAtStart;

        // get current user
        PokerUser user = PokerSession.get().getLoggedInUser();
        boolean loggedIn = (user != null);

        // user name
        add(new StringLabel("name", loggedIn ? user.getName() : "not logged in"));

        // login panel
        loginPanel = new Login("loginPanel", loginVisibleAtStart);
        add(loginPanel);

        // login link and label
        loginLabel = new StringLabel("loginLabel", new StringModel("Login"));
        WebMarkupContainer link = getLoginLink("loginLink");
        add(link.setVisible(!loggedIn && !loginVisibleAtStart));
        link.add(loginLabel);

        // logout link
        add(new Link<Void>("logoutLink")
        {
            private static final long serialVersionUID = 42L;

            @Override
            public void onClick()
            {
                new LoginUtils((BasePage<?>)getPage()).logout();
            }
        }.setVisible(loggedIn));
    }

    public WebMarkupContainer getLoginLink(String id)
    {
        VoidContainer link = new VoidContainer(id);
        link.add(new HideShowSwapLabel(loginPanel, loginVisibleAtStart, loginLabel, "Cancel", "Login"));
        return link;
    }
}
