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
/*
 * GameServletRequest.java
 *
 * Created on August 13, 2003, 3:16 PM
 */

package com.donohoedigital.server;

import org.apache.log4j.*;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class GameServletRequest implements HttpServletRequest
{
    static Logger logger = Logger.getLogger(GameServletRequest.class);

    // needed to handle request
    Map<String, Object> headers_ = new TreeMap<String, Object>(new CaseInsensitiveCompare());
    int nContentLength_;
    String sUserAgent_;
    InputStream in_;
    String sRemoteAddr_;
    String sContentType_;
    String sURI_;
    int nServerPort_;
    String sMethod_;
    String sHTTPVersion_;
    
    /** Creates a new instance of GameServletRequest */
    public GameServletRequest()
    {
    }
    
    public void initRequest(String sMethod, String sURI, 
                                String sHTTPVersion,
                                int nContentLength, String sContentType,
                                String sRemoteAddr, 
                                int nServerPort)
    {
        sMethod_ = sMethod;
        sURI_ = sURI;
        sHTTPVersion_ = sHTTPVersion;
        sContentType_ = sContentType;
        nContentLength_ = nContentLength;
        sRemoteAddr_ = sRemoteAddr;        
        nServerPort_ = nServerPort;
    }
    
    public String getHTTPVersion()
    {
        return sHTTPVersion_;
    }
    
    public Object getAttribute(String str)
    {
        logger.warn("getAttribute called " + str);
		return null;
    }
    
    public Enumeration getAttributeNames()
    {
        logger.warn("getAttribute called");
		return null;
    }
    
    public String getAuthType()
    {
        logger.warn("getAuthType called");
		return null;
    }
    
    public String getCharacterEncoding()
    {
        logger.warn("getCharacterEncoding called");
		return null;
    }
    
    /**
     * Return content length
     */
    public int getContentLength()
    {
		return nContentLength_;
    }
    
    /**
     * Return content type
     */
    public String getContentType()
    {
		return sContentType_;
    }
    
    public String getContextPath()
    {
        logger.warn("getContentPath called");
		return null;
    }
    
    public javax.servlet.http.Cookie[] getCookies()
    {
        logger.warn("getCookies called");
		return null;
    }
    
    public long getDateHeader(String str)
    {
        logger.warn("getDateHeader called " + str);
		return 0;
    }
    
    /**
     * Return header
     */
    public String getHeader(String str)
    {
        return (String) headers_.get(str);
    }
    
    /**
     * set a header value
     */
    public void setHeader(String sHeader, Object oValue)
    {
        headers_.put(sHeader, oValue);
    }
    
    public Enumeration getHeaderNames()
    {
         return Collections.enumeration(headers_.entrySet());
    }
    
    public Enumeration getHeaders(String str)
    {
        logger.warn("getHeaders called");
		return null;
    }
    
    public javax.servlet.ServletInputStream getInputStream() throws java.io.IOException
    {
		throw new IOException("unsupported");
    }
    
    /**
     * Return the input stream used to read any posted data
     */
    public InputStream getInputStream2()
    {
        return in_;
    }
    
    /**
     * Set input stream
     */
    public void setInputStream2(InputStream in)
    {
        in_ = in;
    }
    
    public int getIntHeader(String str)
    {
        logger.warn("getIntHeader called " + str);
		return 0;
    }
    
    public java.util.Locale getLocale()
    {
        logger.warn("getLocale called");
		return Locale.US;
    }
    
    public Enumeration getLocales()
    {
        logger.warn("getLocales called");
		return null;
    }
    
    public String getMethod()
    {
		return sMethod_;
    }
    
    public String getParameter(String str)
    {
        logger.warn("geParameter called " + str);
		return null;
    }
    
    public Map getParameterMap()
    {
        logger.warn("geParameterMap called");
		return null;
    }
    
    public Enumeration getParameterNames()
    {
        logger.warn("geParameterNames called");
		return null;
    }
    
    public String[] getParameterValues(String str)
    {
        logger.warn("getParameterValues called " + str);
		return null;
    }
    
    public String getPathInfo()
    {
        logger.warn("getPathInfo called");
		return null;
    }
    
    public String getPathTranslated()
    {
        logger.warn("getPathTranslated called");
		return null;
    }
    
    public String getProtocol()
    {
        logger.warn("getProtocol called");
		return null;
    }
    
    public String getQueryString()
    {
        logger.warn("getQueryString called");
		return null;
    }
    
    public java.io.BufferedReader getReader() throws java.io.IOException
    {
        logger.warn("getBufferedReader called");
		throw new IOException("unsupported");
    }
    
    public String getRealPath(String str)
    {
        logger.warn("getRealPath called " + str);
		return null;
    }
    
    /**
     * Return remote address
     */
    public String getRemoteAddr()
    {
		return sRemoteAddr_;
    }
    
    public String getRemoteHost()
    {
        logger.warn("getRemoteHost called");
		return null;
    }
    
    public String getRemoteUser()
    {
        logger.warn("getRemoteUser called");
		return null;
    }
    
    public javax.servlet.RequestDispatcher getRequestDispatcher(String str)
    {
        logger.warn("getRequestDispatcher called " + str);
		return null;
    }
    
    public String getRequestURI()
    {
		return sURI_;
    }
    
    public StringBuffer getRequestURL()
    {
        logger.warn("getRequestURL called");
		return null;
    }
    
    public String getRequestedSessionId()
    {
        logger.warn("getRequestSessionId called");
		return null;
    }
    
    public String getScheme()
    {
        logger.warn("getScheme called");
		return null;
    }
    
    public String getServerName()
    {
        logger.warn("getServerName called");
		return null;
    }
    
    public int getServerPort()
    {
		return nServerPort_;
    }
    
    public String getServletPath()
    {
        logger.warn("getServletPath called");
		return null;
    }
    
    public javax.servlet.http.HttpSession getSession()
    {
        logger.warn("getSession called");
		return null;
    }
    
    public javax.servlet.http.HttpSession getSession(boolean param)
    {
        logger.warn("getSession called " + param);
		return null;
    }
    
    public java.security.Principal getUserPrincipal()
    {
        logger.warn("getUserPrincipal called");
		return null;
    }
    
    public boolean isRequestedSessionIdFromCookie()
    {
        logger.warn("isRequestSessionIdFromCookie called");
		return false;
    }
    
    public boolean isRequestedSessionIdFromURL()
    {
        logger.warn("isRequestSessionIdFromURL called");
		return false;
    }
    
    public boolean isRequestedSessionIdFromUrl()
    {
        logger.warn("isRequestSessionIdFromUrl called");
		return false;
    }
    
    public boolean isRequestedSessionIdValid()
    {
        logger.warn("isRequestSessionIdValid called");
		return false;
    }
    
    public boolean isSecure()
    {
        logger.warn("isSecure called");
		return false;
    }
    
    public boolean isUserInRole(String str)
    {
        logger.warn("isUserInRole called " + str);
		return false;
    }
    
    public void removeAttribute(String str)
    {
		logger.warn("removeAttribute called " + str);
    }
    
    public void setAttribute(String str, Object obj)
    {
		logger.warn("setAttribute called " + str);
    }
    
    public void setCharacterEncoding(String str) throws java.io.UnsupportedEncodingException
    {
		logger.warn("setCharacterEncoding called " + str);
    }
    
    public String getLocalAddr() 
    {
        logger.warn("getLocalAddr called");
        return null;
    }
    
    public String getLocalName() 
    {
        logger.warn("getLocalName called");
        return null;
    }
    
    public int getLocalPort() 
    {
        logger.warn("getLocalPort called");
        return 0;
    }
    
    public int getRemotePort() 
    {
        logger.warn("getRemotePort called");
        return 0;
    }
    
    /**
     * class for case insensitive hash
     */
    private static class CaseInsensitiveCompare implements Comparator<String>
    {
        public int compare(String s1, String s2)
        {
            return s1.toUpperCase().compareTo(s2.toUpperCase());
        }
    }
}
