/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.wicket.pages;

import com.donohoedigital.games.poker.wicket.PokerWicketConstants;
import com.donohoedigital.games.poker.wicket.panels.CopyrightFooter;
import com.donohoedigital.games.poker.wicket.panels.CurrentProfile;
import com.donohoedigital.games.poker.wicket.panels.TopNavigation;
import com.donohoedigital.games.poker.wicket.util.LoginUtils;
import com.donohoedigital.wicket.components.VoidContainer;
import com.donohoedigital.wicket.pages.BasePage;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.JQueryResourceReference;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 18, 2008
 * Time: 11:02:44 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BasePokerPage extends BasePage<Void>
{
    private static final long serialVersionUID = 42L;
    
    private static final String PROFILE_NAME = "profile";

    @SuppressWarnings({"AbstractMethodCallInConstructor"})
    public BasePokerPage(PageParameters parameters)
    {
        super(parameters);

        new LoginUtils(this).loginFromCookie();

        add(getTopNavigation("nav"));
        add(new CurrentProfile(PROFILE_NAME, isLoginPanelVisible()).setVisible(isCurrentProfileDisplayed()));
        add(new CopyrightFooter("footer"));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // Ensure jQuery is always included in the head
        response.render(JavaScriptHeaderItem.forReference(JQueryResourceReference.getV3()));
    }

    /**
     * subclass needs to implement - return top navigation component
     */
    protected abstract TopNavigation getTopNavigation(String id);

    /**
     * subclass needs to implement - return if current profile panel is displayed
     */
    protected boolean isCurrentProfileDisplayed() {
        return !TESTING(PokerWicketConstants.DEBUG_DOC_MODE);
    }

    /**
     * Is login panel visible (in current profile panel - result passed to CurrentProfile constructor).  Default is false.
     * @return whether login panel should be visible in CurrentProfile.
     */
    protected boolean isLoginPanelVisible()
    {
        return false;
    }

    /**
     * get current profile panel
     */
    public CurrentProfile getCurrentProfile()
    {
        return (CurrentProfile) get(PROFILE_NAME);
    }
}
