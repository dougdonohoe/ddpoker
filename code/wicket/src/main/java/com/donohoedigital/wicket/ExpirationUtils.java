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
package com.donohoedigital.wicket;

import jakarta.servlet.http.Cookie;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * @author Doug Donohoe
 */
public class ExpirationUtils
{
    //private static final Logger logger = LogManager.getLogger(ExpirationUtils.class);

    private static final String LAST_PATH = "last-path";

    /**
     * Store last bookmarkable request path in a cookie
     */
    public static void rememberPath()
    {
        RequestCycle requestCycle = WicketUtils.getRequestCycle();
        // if a bookmarkable page, remember URL
        if (requestCycle.getActiveRequestHandler() instanceof RenderPageRequestHandler)
        {
            Cookie c = WicketUtils.createCookie(LAST_PATH, WicketUtils.getHttpServletRequest().getServletPath());
            WicketUtils.addCookie(c);
            //logger.debug("Remembering URL: {} on {}", c.getValue(), c.getPath());
        }
    }

    public static String getLastPath()
    {
        return WicketUtils.getCookieValue(LAST_PATH);
    }

}