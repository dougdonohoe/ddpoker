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
 * StringHttpServletRequest.java
 *
 * Created on March 11, 2003, 11:45 AM
 */

package com.donohoedigital.jsp;

import org.apache.logging.log4j.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class StringHttpServletRequest implements HttpServletRequest
{
    static Logger logger = LogManager.getLogger(StringHttpServletRequest.class);
    StringHttpSession session_;
    Map params_  = new HashMap();
    String sServletPath_ = null;
    
    /** Creates a new instance of StringHttpServletRequest */
    public StringHttpServletRequest()
    {
        session_ = new StringHttpSession();
    }
    
    public Object getAttribute(String str)
    {
        if (str.equals("org.apache.catalina.jsp_file")) return null; // okay - we know about this
        if (str.equals("javax.servlet.include.servlet_path")) return null; // Jasper hook
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
    
    public int getContentLength()
    {
        logger.warn("getContentLength called");
		return 0;
    }
    
    public String getContentType()
    {
        logger.warn("getContentType called");
		return null;
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
    
    public String getHeader(String str)
    {
        logger.warn("getHeader called " + str);
		return null;
    }
    
    public Enumeration getHeaderNames()
    {
        logger.warn("getHeaderNames called");
		return null;
    }
    
    public Enumeration getHeaders(String str)
    {
        logger.warn("getHeaders called");
		return null;
    }
    
    public ServletInputStream getInputStream() throws java.io.IOException
    {
		throw new IOException("unsupported");
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
		return "GET";
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
    
    @SuppressWarnings({"unchecked"})
    public Enumeration getParameterNames()
    {
        logger.warn("geParameterNames called");
        return Collections.enumeration(params_.keySet());
    }
    
    public String[] getParameterValues(String str)
    {
        logger.warn("getParameterValues called " + str);
		return null;
    }
    
    public String getPathInfo()
    {
        //logger.debug("getPathInfo called");
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
        //logger.debug("getQueryString called");
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
    
    public String getRemoteAddr()
    {
        logger.warn("getRemoteAddr called");
		return null;
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
        logger.warn("getRequestURI called");
		return null;
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
        logger.warn("getServerPort called");
		return 0;
    }
    
    public void setServletPath(String s)
    {
        sServletPath_ = s;
    }
    
    public String getServletPath()
    {
		return sServletPath_;
    }
    
    public HttpSession getSession()
    {
		return session_;
    }
    
    public HttpSession getSession(boolean param)
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
    
    public void setCharacterEncoding(String str) throws UnsupportedEncodingException
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

    // Servlet API 3.1 additions

    @Override
    public String changeSessionId() {
        return "";
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return Collections.emptyList();
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}
