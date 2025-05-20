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
/*
 * EmbeddedServletContext.java
 *
 * Created on August 15, 2003, 12:50 PM
 */

package com.donohoedigital.jsp;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import jakarta.servlet.*;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author donohoe
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class EmbeddedServletContext implements ServletContext
{
    static Logger logger = LogManager.getLogger(EmbeddedServletContext.class);

    private String jspResourceDir;
    private HashMap<String, Object> attributes = new HashMap<>(10);

    /**
     * Creates a new instance of GameServletContext
     */
    public EmbeddedServletContext(String jspResourceDir)
    {
        this.jspResourceDir = jspResourceDir;
    }

    public Object getAttribute(String str)
    {
        // okay to ignore - we may want to respond
        // someday to requests so that we can configure jsp engine

        //logger.warn("getAttribute called: " + str);
        return attributes.get(str);
    }

    public Enumeration getAttributeNames()
    {
        logger.warn("getAttributeNames called");
        Vector<String> vector = new Vector<>(attributes.keySet());
        return vector.elements();
    }

    public String getContextPath()
    {
        logger.warn("getContextPath called");
        return null;
    }

    public jakarta.servlet.ServletContext getContext(String str)
    {
        logger.warn("getContext called: " + str);
        return null;
    }

    public String getInitParameter(String str)
    {
        //logger.warn("getInitParameter called: " + str);
        return null;
    }

    public Enumeration<String> getInitParameterNames()
    {
        logger.warn("getInitParameterNames called");
        return null;
    }

    /**
     * Not really important what we return
     */
    public int getMajorVersion()
    {
        return 1;
    }

    public String getMimeType(String str)
    {
        logger.warn("getMimeType called: " + str);
        return null;
    }

    /**
     * Not really important what we return
     */
    public int getMinorVersion()
    {
        return 0;
    }

    public jakarta.servlet.RequestDispatcher getNamedDispatcher(String str)
    {
        logger.warn("getNamedDispatcher called: " + str);
        return null;
    }

    /**
     * Return location of file in filesystem
     */
    public String getRealPath(String str)
    {
        return jspResourceDir + str;
    }

    public RequestDispatcher getRequestDispatcher(String str)
    {
        logger.warn("getRequestDispatcher called: " + str);
        return null;
    }

    /**
     * Return jsp page as a resource from the classpath.
     */
    public URL getResource(String str) throws java.net.MalformedURLException
    {
        return ClassLoader.getSystemResource(jspResourceDir + str);
    }

    /**
     * Return the jsp file as an input stream
     */
    public InputStream getResourceAsStream(String str)
    {
        try
        {
            URL url = getResource(str);
            if (url != null) return url.openStream();
            return null;
        }
        catch (IOException e)
        {
            throw new ApplicationError(e);
        }
    }

    public Set getResourcePaths(String str)
    {
        logger.warn("getResourcePaths called: " + str);
        return new java.util.HashSet();
    }

    public String getServerInfo()
    {
        return "Donohoe Digital Game Server";
    }

    public jakarta.servlet.Servlet getServlet(String str) throws jakarta.servlet.ServletException
    {
        logger.warn("getServlet called: " + str);
        return null;
    }

    public String getServletContextName()
    {
        logger.warn("getServletContextName called");
        return null;
    }

    public Enumeration getServletNames()
    {
        logger.warn("getServletNames called");
        return null;
    }

    public Enumeration getServlets()
    {
        logger.warn("getServlets called");
        return null;
    }

    /**
     * Log info through to our logger
     */
    public void log(String str)
    {
        logger.info(str);
    }

    /**
     * Log info through to our logger
     */
    public void log(Exception exception, String str)
    {
        logger.warn(str + " exception: " + Utils.formatExceptionText(exception));
    }

    /**
     * Log info through to our logger
     */
    public void log(String str, Throwable throwable)
    {
        logger.warn(str + " exception: " + Utils.formatExceptionText(throwable));
    }

    public void removeAttribute(String str)
    {
        logger.warn("removeAttribute called: " + str);
        attributes.remove(str);
    }

    public void setAttribute(String str, Object obj)
    {
        //logger.info("setAttribute called: " + str + " to " + obj);
        attributes.put(str, obj);
    }

    // Servlet API 3.1 additions

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public boolean setInitParameter(String s, String s1) {
        return false;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String s, String s1) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> aClass) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String s) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.emptyMap();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String s, String s1) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> aClass) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String s) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.emptyMap();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> set) {
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Collections.emptySet();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Collections.emptySet();
    }

    @Override
    public void addListener(String s) {
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
    }

    @Override
    public void addListener(Class<? extends EventListener> aClass) {
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> aClass) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClass().getClassLoader();
    }

    @Override
    public void declareRoles(String... strings) {
    }

    @Override
    public String getVirtualServerName() {
        return "";
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return "";
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return "";
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {

    }

    @Override
    public void setRequestCharacterEncoding(Charset encoding) {
        ServletContext.super.setRequestCharacterEncoding(encoding);
    }

    @Override
    public void setResponseCharacterEncoding(Charset encoding) {
        ServletContext.super.setResponseCharacterEncoding(encoding);
    }
}
