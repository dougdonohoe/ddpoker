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
package com.donohoedigital.games.poker.wicket.util;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.PokerUser;
import com.donohoedigital.games.poker.wicket.PokerWicketApplication;
import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.service.BannedKeyService;
import com.donohoedigital.wicket.WicketUtils;
import com.donohoedigital.wicket.pages.BasePage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.Cookie;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.donohoedigital.games.poker.wicket.util.LoginUtils.LoginType.COOKIE;
import static com.donohoedigital.games.poker.wicket.util.LoginUtils.LoginType.PAGE;

/**
 * @author Doug Donohoe
 */
public class LoginUtils
{
    private static final Logger logger = LogManager.getLogger(LoginUtils.class);

    private static final String LOGIN = "login";

    private final BasePage<?> page;

    enum LoginType
    {
        COOKIE, PAGE
    }

    /**
     * Construct
     */
    public LoginUtils(BasePage<?> page)
    {
        this.page = page;
    }

    /**
     * Login based on cookie (not authenticated)
     */
    public void loginFromCookie()
    {
        String c = WicketUtils.getCookieValue(LOGIN);
        if (c != null && PokerSession.get().getLoggedInUser() == null)
        {
            if (!login(c, null, false, COOKIE))
            {
                deleteLoginCookie();
            }
        }
    }

    /**
     * Login from a page (authenticate).  Stores cookie with login name if remember is true
     */
    public boolean loginFromPage(String name, String password, boolean remember)
    {
        return login(name, password, remember, PAGE);
    }

    /**
     * login logic
     */
    private boolean login(String name, String password, boolean remember, LoginType type)
    {
        OnlineProfileService profileService = PokerWicketApplication.get().getProfileService();
        BannedKeyService banService = PokerWicketApplication.get().getBanService();

        // lookup profile
        OnlineProfile profile = profileService.getOnlineProfileByName(name);

        // get IP for logging
        String ip = WicketUtils.getHttpServletRequest().getRemoteAddr();

        // profile should be there and activated
        if (profile == null || !profile.isActivated() || profile.isRetired())
        {
            if (profile == null)
            {
                if (type == PAGE)
                    page.error(PropertyConfig.getMessage("msg.web.poker.invalidprofile")); // FIX: use wicket properties files
                logger.info("{}: {} {} login failed (no such user).", type, ip, name);
            }
            else if (profile.isRetired())
            {
                if (type == PAGE)
                    page.error(PropertyConfig.getMessage("msg.web.poker.retired", Utils.encodeHTML(name))); // FIX: use wicket properties files
                logger.info("{}: {} {} login failed (retired).", type, ip, name);
            }
            else // !profile.isActivated()
            {
                if (type == PAGE)
                    page.error(PropertyConfig.getMessage("msg.web.poker.invalidprofile")); // FIX: use wicket properties files
                logger.info("{}: {} {} login failed (not activated).", type, ip, name);
            }

            return false;
        }

        // ban check
        BannedKey ban = banService.getIfBanned(profile.getLicenseKey(), profile.getEmail(), profile.getName());
        if (ban != null)
        {
            if (type == PAGE)
            {
                DateFormat sf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                page.error(PropertyConfig.getMessage("msg.banned", sf.format(ban.getUntil())));
            }
            logger.info("{}: {} {} login failed (banned): {}; ban: {}", type, ip, name, profile, ban);
            return false;
        }

        // verify password (if login from page)
        boolean authenticated = false;
        if (type == PAGE)
        {
            if (!profile.getPassword().equals(password))
            {
                page.error(PropertyConfig.getMessage("msg.web.poker.invalidprofile")); // FIX: use wicket properties files
                logger.info("{}: {} {} login failed (password mismatch).", type, ip, name);
                return false;
            }
            else
            {
                authenticated = true;
            }
        }

        // create user and store in session
        PokerUser user = new PokerUser(profile);
        user.setAuthenticated(authenticated);
        PokerSession.get().setLoggedInUser(user);
        logger.info("{}: {} {} logged in (authenticated={}).", type, ip, name, user.isAuthenticated());

        // set cookie
        if (remember)
        {
            Cookie c = WicketUtils.createCookie(LOGIN, name);
            WicketUtils.addCookie(c);
        }

        // continue on
        if (type == PAGE && !page.continueToOriginalDestination())
        {
            // default to page that login form was on
            // need to use "class" so page is re-rendered
            setResponsePage();
        }

        return true;
    }

    /**
     * clear login cookie
     */
    public void deleteLoginCookie()
    {
        WicketUtils.deleteCookie(LOGIN);
    }

    /**
     * logout logic
     */
    public void logout()
    {
        // clear session
        PokerSession.get().invalidate();

        // clear cookie
        deleteLoginCookie();

        // need to use "class" so page is created new
        setResponsePage();
    }

    /**
     * Set response page
     */
    private void setResponsePage()
    {
        //logger.debug("Params: "+ page.getPageParameters());
        page.setResponsePage(page.getPageClass(), page.getPageParameters());
    }
}
