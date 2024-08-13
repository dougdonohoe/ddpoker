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
 * JspEmail.java
 *
 * Created on March 11, 2003, 5:40 PM
 */

package com.donohoedigital.jsp;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.jasper.servlet.*;
import org.apache.log4j.*;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * @author donohoe
 */
public class JspEmail
{
    static Logger logger = Logger.getLogger(JspEmail.class);

    static {
        Jsp.init();
    }

    public static final String PARAM_SUBJECT = "subject";
    public static final String PARAM_CALLER = "caller";

    private String sSubject_ = null;
    private String sHtml_ = null;
    private String sPlain_ = null;
    private final String sHtmlName_;
    private final String sPlainName_;
    private final StringHttpServletRequest request_;
    private final StringHttpServletResponse response_;
    private static JspServlet jsp_ = null;
    private final boolean LOCALJSP = true;

    /**
     * Creates a new instance of JspEmail
     */
    public JspEmail(String sFileID, String sLocale, Object oCaller)
    {
        if (sLocale != null)
        {
            sFileID = sLocale + "-" + sFileID;
        }

        sHtmlName_ = "/" + sFileID + "-html.jsp";
        sPlainName_ = "/" + sFileID + "-plain.jsp";

        request_ = new StringHttpServletRequest();
        response_ = new StringHttpServletResponse();
        request_.getSession().setAttribute(PARAM_CALLER, oCaller);

        // if using GameServer and jsp servlet engine not created, create it
        synchronized (JspEmail.class)
        {
            if (LOCALJSP && jsp_ == null)
            {
                jsp_ = new JspServlet();
                try
                {
                    File logDir = new File(new DefaultRuntimeDirectory().getServerHome(), "log");
                    File scratch = new File(logDir, "jsp-" + ConfigManager.getAppName());
                    ConfigUtils.verifyNewDirectory(scratch);
                    logger.info("JSP Email scratch in " + scratch.getAbsolutePath());
                    ServletConfig config = new EmbeddedServletConfig("email", scratch.getAbsolutePath());
                    config.getServletContext().setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
                    jsp_.init(config);
                }
                catch (ServletException se)
                {
                    throw new ApplicationError(se);
                }
            }
        }
    }

    /**
     * Get session which is passed into JSP pages
     */
    public HttpSession getSession()
    {
        return request_.getSession();
    }

    /**
     * Execute the JSPs
     */
    public void executeJSP()
    {
        sHtml_ = executeJSP(sHtmlName_, request_, response_);
        response_.reset();
        sPlain_ = executeJSP(sPlainName_, request_, response_);
    }

    /**
     * get contents of a jsp
     */
    private String executeJSP(String sName, StringHttpServletRequest request, StringHttpServletResponse response)
    {
        try
        {
            // for use when GameServer is running things
            if (LOCALJSP)
            {
                request.setServletPath(sName);
                jsp_.service(request, response);
            }
            // this is for use in an application server
            else
            {
                throw new UnsupportedOperationException("Not supported in application server yet");
            }

            if (sSubject_ == null)
            {
                sSubject_ = (String) request.getSession().getAttribute(PARAM_SUBJECT);
            }
            return response.getContents();
        }
        catch (Exception se)
        {
            throw new ApplicationError(se);
        }
    }

    /**
     * Return subject set by JSP
     */
    public String getSubject()
    {
        if (sSubject_ == null) return "No subject";
        return sSubject_;
    }

    /**
     * Return HTML version
     */
    public String getHtml()
    {
        return sHtml_;
    }

    /**
     * Return plain version
     */
    public String getPlain()
    {
        return sPlain_;
    }
}
