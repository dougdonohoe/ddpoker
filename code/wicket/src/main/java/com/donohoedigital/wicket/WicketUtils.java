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

import com.donohoedigital.base.Utils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.converter.DateConverter;
import org.apache.wicket.util.encoding.UrlDecoder;
import org.apache.wicket.util.encoding.UrlEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 24, 2008
 * Time: 4:50:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class WicketUtils
{
    /**
     * Get a date from page parameters using given converter
     */
    public static Date getAsDate(PageParameters params, String name, Date def, DateConverter converter)
    {
        String sDate = params.get(name).toString();
        if (sDate == null) return def;

        try {
            return converter.convertToObject(sDate, null);
        }
        catch (ConversionException ignored)
        {
            return def;
        }
    }

    public static int getAsInt(PageParameters params, String name, Integer def) {
        String value = params.get(name).toString();
        if (value == null || value.isEmpty()) return def;
        return Integer.parseInt(value);
    }

    public static long getAsLong(PageParameters params, String name, Long def) {
        String value = params.get(name).toString();
        if (value == null || value.isEmpty()) return def;
        return Long.parseLong(value);
    }

    public static <T extends Enum<T>> T getAsEnum(PageParameters params, String key, Class<T> eClass, T defaultValue)
    {
        return getEnumImpl(params, key, eClass, defaultValue);
    }

    /**
     * get enum implementation
     */
    @SuppressWarnings( { "unchecked" })
    private static <T extends Enum<T>> T getEnumImpl(PageParameters params, String key, Class<?> eClass, T defaultValue)
    {
        if (eClass == null) throw new IllegalArgumentException("eClass value cannot be null");

        String value = params.get(key).toString();
        if (value == null) return defaultValue;

        Method valueOf;
        try
        {
            valueOf = eClass.getMethod("valueOf", String.class);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Could not find method valueOf(String s) for " + eClass.getName(), e);
        }

        try
        {
            return (T)valueOf.invoke(eClass, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not invoke method valueOf(String s) on " + eClass.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            // IllegalArgumentException thrown if enum isn't defined - just return default
            if (e.getCause() instanceof IllegalArgumentException)
            {
                return defaultValue;
            }
            throw new RuntimeException(e); // shouldn't happen
        }
    }

    /**
     * Get WebRequestCycle
     */
    public static RequestCycle getRequestCycle()
    {
        return RequestCycle.get();
    }

    /**
     * Get WebRequest
     */
    public static WebRequest getWebRequest()
    {
        return (WebRequest) getRequestCycle().getRequest();
    }

    /**
     * Get WebResponse
     */
    public static WebResponse getWebResponse()
    {
        return (WebResponse) getRequestCycle().getResponse();
    }

    /**
     * Get HttpServletResponse
     */
    public static HttpServletRequest getHttpServletRequest() {
        return (HttpServletRequest) getWebRequest().getContainerRequest();
    }

    /**
     * Get context path (where app is mounted in appserver space)
     */
    public static String getContextPath()
    {
        String path = getHttpServletRequest().getContextPath();
        if (Utils.isEmpty(path)) return "/";
        return path;
    }

    public static String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName;

        if (serverPort != 80 && serverPort != 443) {
            baseUrl += ":" + serverPort;
        }

        return baseUrl + "/";
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
        HttpServletRequest req = getHttpServletRequest();
        return RequestUtils.toAbsolutePath(getBaseUrl(req), getRequestCycle().mapUrlFor(page, params).toString());
    }

    /**
     * Create a cookie, setting path to root context path and age to 1 year
     */
    public static Cookie createCookie(String name, String value)
    {
        Cookie c = new Cookie(name, UrlEncoder.PATH_INSTANCE.encode(value, StandardCharsets.UTF_8));
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

        return UrlDecoder.PATH_INSTANCE.decode(value, StandardCharsets.UTF_8);
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
