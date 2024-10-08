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
package com.donohoedigital.wicket;

import com.donohoedigital.base.*;
import org.apache.wicket.*;
import org.apache.wicket.protocol.http.*;
import org.apache.wicket.protocol.http.request.*;
import org.apache.wicket.util.convert.*;
import org.apache.wicket.util.convert.converters.*;

import javax.servlet.http.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 24, 2008
 * Time: 4:50:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class WicketUtils
{
    //private static Logger logger = LogManager.getLogger(WicketUtils.class);

    /**
     * remove wicket:interface parameter from params
     */
    public static PageParameters removeWicketInterface(PageParameters params)
    {
        if (params != null)
        {
            params.remove(WebRequestCodingStrategy.INTERFACE_PARAMETER_NAME);
        }
        return params;
    }

    /**
     * Get a date from page parameters using given converter
     */
    public static Date getAsDate(PageParameters params, String name, Date def, DateConverter converter)
    {
        String sDate = params.getString(name, null);
        if (sDate == null) return def;

        try {
            return converter.convertToObject(sDate, null);
        }
        catch (ConversionException ignored)
        {
            return def;
        }
    }

    /**
     * Get WebRequestCycle
     */
    public static WebRequestCycle getWebRequestCycle()
    {
        return (WebRequestCycle) RequestCycle.get();
    }

    /**
     * Get WebRequest
     */
    public static WebRequest getWebRequest()
    {
        return getWebRequestCycle().getWebRequest();
    }

    /**
     * Get WebResponse
     */
    public static WebResponse getWebResponse()
    {
        return getWebRequestCycle().getWebResponse();    
    }

    /**
     * Get context path (where app is mounted in appserver space)
     */
    public static String getContextPath()
    {
        String path = getWebRequest().getHttpServletRequest().getContextPath();
        if (Utils.isEmpty(path)) return "/";
        return path;
    }

    /**
     * Get protocol/machine/port/context path
     */
    public static String getBaseUrl()
    {
        StringBuilder sb = new StringBuilder();
        HttpServletRequest req = getWebRequest().getHttpServletRequest();
        sb.append(req.getScheme());
        sb.append("://");
        sb.append(req.getServerName());
        if (req.getServerPort() != 80 || req.getServerPort() != 443) sb.append(':').append(req.getServerPort());
        sb.append(getContextPath());
        return sb.toString();
    }

    /**
     * URL for page/params
     */
    public static String absoluteUrlFor(Class<? extends Page> page)
    {
        return absoluteUrlFor(page, null);
    }

    /**
     * URL for page/params
     */
    public static String absoluteUrlFor(Class<? extends Page> page, PageParameters params)
    {
        return RequestUtils.toAbsolutePath(getWebRequestCycle().urlFor(page, params).toString());
    }

    /**
     * Create a cookie, setting path to root context path and age to 1 year
     */
    public static Cookie createCookie(String name, String value)
    {
        Cookie c = new Cookie(name, WicketURLEncoder.PATH_INSTANCE.encode(value));
        c.setPath(getContextPath());
        c.setMaxAge(60 * 60 * 24 * 365); // one year
        return c;
    }

    /**
     * Add cookie
     */
    public static void addCookie(Cookie c)
    {
        if (c == null) return;
        getWebResponse().addCookie(c);
    }

    /**
     * Get cookie
     */
    public static Cookie getCookie(String name)
    {
        return getWebRequest().getCookie(name);
    }

    /**
     * Get cookie value, decoding on the way
     */
    public static String getCookieValue(String name)
    {
        Cookie c = getCookie(name);
        if (c == null) return null;

        String value = c.getValue();
        if (value == null) return null;

        return WicketURLDecoder.PATH_INSTANCE.decode(value);
    }

    /**
     * Make sure path is set to same as above otherwise will not properly delete
     */
    public static void deleteCookie(String name)
    {
        Cookie c = getCookie(name);
        if (c == null) return;
        c.setPath(getContextPath());
        getWebResponse().clearCookie(c);
    }
}
