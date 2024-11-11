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
 * EmbeddedServletConfig.java
 *
 * Created on August 15, 2003, 12:47 PM
 */

package com.donohoedigital.jsp;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author donohoe
 */
public class EmbeddedServletConfig implements ServletConfig
{
    static Logger logger = LogManager.getLogger(EmbeddedServletConfig.class);

    ServletContext context_;
    String scratch_;

    /**
     * Creates a new instance of GameServletConfig
     */
    public EmbeddedServletConfig(String jspResourceDir, String scratch)
    {
        context_ = new EmbeddedServletContext(jspResourceDir);
        scratch_ = scratch;
    }

    public String getInitParameter(String str)
    {
        // okay to ignore these - someday may want to 
        // respond with stuff to control JSP engine
        if (str.equals("scratchdir"))
        {
            return scratch_;
        }
        //logger.info("getInitParameter called: " + str);
        return null;
    }

    @SuppressWarnings({"RawUseOfParameterizedType"})
    public Enumeration<String> getInitParameterNames()
    {
        List<String> params = new ArrayList<String>();
        return Collections.enumeration(params);
    }

    public jakarta.servlet.ServletContext getServletContext()
    {
        return context_;
    }

    public String getServletName()
    {
        return "GameServlet";
    }
}
