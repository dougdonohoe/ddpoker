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
package com.donohoedigital.server;

import org.apache.log4j.*;

import javax.servlet.http.*;
import java.util.*;

public class ServletDebug
{
    static Logger logger = Logger.getLogger(ServletDebug.class);
    
    public static boolean bVerbose_ = true; // extra output
    public static boolean bDebug_ = true; // print URL
    // TODO: get from config file
    private static String sFile_ = null;
    private static boolean bAppend_ = true;
    private static final String THIS_REQUEST_HANDLED="_trh_";

    public static void setVerbose(boolean b)
    {
        bVerbose_ = b;
    }

    public static void debugGet(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse)
    {
        if (!bDebug_) return;

        if (httpservletrequest.getAttribute(THIS_REQUEST_HANDLED) != null) {
            return;
        }

        httpservletrequest.setAttribute(THIS_REQUEST_HANDLED, Boolean.TRUE);

        if (bVerbose_) {
            println("");
            println("---------------------------GET-------------------------------");
        }

        long lnow = System.currentTimeMillis();
        println(lnow + " " + constructURL(httpservletrequest));

        if (bVerbose_) {
            println("GETTING pathinfo: " + httpservletrequest.getPathInfo());
            println("GETTING requesturi: " + httpservletrequest.getRequestURI());
            println("GETTING method: " + httpservletrequest.getMethod());
            println("GETTING query: " + httpservletrequest.getQueryString());
            println("GETTING servlet: " + httpservletrequest.getServletPath());
            println("GETTING scheme: " + httpservletrequest.getScheme());
            println("GETTING content-type: " + httpservletrequest.getContentType());
            println("GETTING content-length: " + httpservletrequest.getContentLength());
            println("GETTING host: " + httpservletrequest.getServerName() +  ":"+
                                        httpservletrequest.getServerPort());
            println("GETTING params: " + getParams(httpservletrequest,false));
            printCookies(httpservletrequest);
            printHeaders(httpservletrequest);
        }
    }

    public static void printCookies(HttpServletRequest httpservletrequest)
    {
        Cookie acookie[] = httpservletrequest.getCookies();
        if (acookie != null)
        {
            for(int i = 0; i < acookie.length; i++)
            {
                println("COOKIE[" + i + "]" + acookie[i].getName() + "=" + acookie[i].getValue());
            }
        }
    }

    public static String getParams(HttpServletRequest httpservletrequest, boolean bIgnoreSessionIds)
    {
        Enumeration names = httpservletrequest.getParameterNames();
        String sParams = "";
        if (names != null)
        {
            String sName, sValue;
            while (names.hasMoreElements())
            {
                sName = (String) names.nextElement();
                if (bIgnoreSessionIds &&
                   (sName.equals("sessionID") || sName.equals("RequisiteSession"))) {
                    continue;
                }
                sValue = httpservletrequest.getParameter(sName);
                if (sParams.length() > 0) sParams += "&";
                sParams = sParams + sName + "=" + sValue;
            }
        }

        return sParams;
    }

    public static void printHeaders(HttpServletRequest httpservletrequest)
    {
        Enumeration names = httpservletrequest.getHeaderNames();
        String sParams = "";
        if (names != null)
        {
            String sName, sValue;
            while (names.hasMoreElements())
            {
                sName = (String) names.nextElement();
                sValue = httpservletrequest.getHeader(sName);
                println("HEADER " + sName + "=" + sValue);
            }
        }
    }

    public static final String POST_DELIMITER = "--POST--";
    public static final String CONTENT_TYPE_DELIMITER = "--CONTENT-TYPE--";
    public static String constructURL(HttpServletRequest httpservletrequest)
    {
        StringBuilder sb = new StringBuilder(256);

        sb.append(httpservletrequest.getScheme());
        sb.append("://");
        sb.append(httpservletrequest.getServerName());
        sb.append(":");
        sb.append(httpservletrequest.getServerPort());
        sb.append(httpservletrequest.getRequestURI());
        String sParams = getParams(httpservletrequest, false);
        if (sParams != null && sParams.length() > 0)
        {
            sb.append("?");
            sb.append(sParams);
        }
        /*
        String sPost = httpservletrequest.getReaderData();
        if (sPost != null && sPost.length() > 0)
        {
            sb.append(POST_DELIMITER);
            sb.append(sPost);
            sb.append(CONTENT_TYPE_DELIMITER);
            sb.append(httpservletrequest.getContentType());
        }
        */

        // TODO: allow URL file to be multiple lines
        // transform newlines to spaces, generally okay since the
        // things that are getting posted are XML docs, for which whitespace
        // has no meaning really
        int len = sb.length();
        char c;
        for (int i=0; i < len; i++)
        {
            c = sb.charAt(i);
            if (c == '\n' || c == '\r')
            {
                sb.setCharAt(i, ' ');
            }
        }

        return sb.toString();
    }

    public static void println(String s)
    {
        logger.debug(s);
    }
}
