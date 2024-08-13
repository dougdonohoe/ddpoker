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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.service.*;
import com.donohoedigital.games.poker.wicket.*;
import static com.donohoedigital.games.poker.wicket.util.LoginUtils.LoginType.*;
import com.donohoedigital.games.server.model.*;
import com.donohoedigital.games.server.service.*;
import com.donohoedigital.wicket.*;
import com.donohoedigital.wicket.pages.*;
import org.apache.log4j.*;

import javax.servlet.http.*;
import java.text.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
public class LoginUtils
{
    private static Logger logger = Logger.getLogger(LoginUtils.class);

    private static final String LOGIN = "login";

    private BasePage<?> page;

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
        String ip = WicketUtils.getWebRequest().getHttpServletRequest().getRemoteAddr();

        // profile should be there and activated
        if (profile == null || !profile.isActivated() || profile.isRetired())
        {
            if (profile == null)
            {
                if (type == PAGE)
                    page.error(PropertyConfig.getMessage("msg.web.poker.invalidprofile")); // FIX: use wicket properties files
                logger.info(String.valueOf(type) + ": " + ip + ' ' + name + " login failed (no such user).");
            }
            else if (profile.isRetired())
            {
                if (type == PAGE)
                    page.error(PropertyConfig.getMessage("msg.web.poker.retired", Utils.encodeHTML(name))); // FIX: use wicket properties files
                logger.info(String.valueOf(type) + ": " + ip + ' ' + name + " login failed (retired).");
            }
            else // !profile.isActivated()
            {
                if (type == PAGE)
                    page.error(PropertyConfig.getMessage("msg.web.poker.invalidprofile")); // FIX: use wicket properties files
                logger.info(String.valueOf(type) + ": " + ip + ' ' + name + " login failed (not activated).");
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
            logger.info(String.valueOf(type) + ": " + ip + ' ' + name + " login failed (banned): " + profile + "; ban: " + ban);
            return false;
        }

        // verfiy password (if login from page)
        boolean authenticated = false;
        if (type == PAGE)
        {
            if (!profile.getPassword().equals(password))
            {
                page.error(PropertyConfig.getMessage("msg.web.poker.invalidprofile")); // FIX: use wicket properties files
                logger.info(String.valueOf(type) + ": " + ip + ' ' + name + " login failed (password mismatch).");
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
        logger.info(String.valueOf(type) + ": " + ip + ' ' + name + " logged in (authenticated=" + user.isAuthenticated() + ").");

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
            //noinspection unchecked
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
        page.setResponsePage(page.getPageClass(), WicketUtils.removeWicketInterface(page.getPageParameters()));
    }
}
