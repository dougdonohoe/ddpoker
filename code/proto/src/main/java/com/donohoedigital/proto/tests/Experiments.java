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
package com.donohoedigital.proto.tests;

import com.donohoedigital.base.Utils;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.util.convert.converter.DateConverter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 4, 2008
 * Time: 12:53:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class Experiments extends TestCase
{
    private Logger logger = LogManager.getLogger(Experiments.class);

    public void testNullEquals()
    {
        String foo = null;
        String bar = "Hi";

        logger.debug("bar.equals(foo): " + bar.equals(foo));
    }

    public void testResourceLoader()
    {
        ClassLoader cl = getClass().getClassLoader();
        try
        {
            Enumeration<URL> resources = cl.getResources("com/donohoedigital");
            while (resources.hasMoreElements())
            {
                logger.info("Found: " + resources.nextElement());
            }
        }
        catch (IOException e)
        {
            logger.info(Utils.formatExceptionText(e));
        }
    }

    public void testSpringResolver()
    {
        PathMatchingResourcePatternResolver match;
        match = new PathMatchingResourcePatternResolver();
        try
        {
            Resource[] resources = match.getResources("classpath*:com/donohoedigital/**/*.class");
            for (Resource r : resources)
            {
                logger.info("Found: " + r.getURL());
            }
        }
        catch (IOException e)
        {
            logger.info(Utils.formatExceptionText(e));
        }
    }

    public void testPrimitiveArray()
    {
        byte[] data = new byte[0];

        logger.info("Class for data: " + data.getClass());
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void testNoLog4j()
    {
        System.out.println("Before log4j logger call");
        Utils.sleepSeconds(1);
        logger.info("logger.info call");
        Utils.sleepSeconds(1);
        System.out.println("After log4j logger call");
    }

    public void testNullInstanceOf()
    {
        Object foo = null;

        if (foo instanceof String)
        {
            logger.debug("never true");
        }

        assertTrue("null instanceof works if we get here", true);
    }

    public void testURLEncoder()
    {
        String dot = ".|";

        try
        {
            logger.debug("Encoding of '" + dot + "': " + URLEncoder.encode(dot, Utils.CHARSET_BASIC_NAME));
        }
        catch (UnsupportedEncodingException e)
        {
            fail();
        }
    }

    public void testEncodeSlash()
    {
        String enc = "><//'>";
        enc = enc.replaceAll("/", "&#47;");
        logger.debug("ENCODE: " + enc);
    }

    public void testReplacePercent()
    {
        logger.debug("PERCENT: " + "%and%that%".replaceAll("%", "\\\\%"));
    }

    public void testDateConverter()
    {
        DateConverter c = new DateConverter();
        String s = c.convertToString(new Date(), null);
        logger.debug("Date: " + s);

        logger.debug("Reversed: " + c.convertToObject(s, null));
    }

    public void testUser()
    {
        Properties props = System.getProperties();

        if (true)
        {
            String sKey;
            Enumeration<Object> enu = props.keys();
            while (enu.hasMoreElements())
            {
                sKey = (String) enu.nextElement();
                logger.debug(sKey + '=' + props.getProperty(sKey));
            }
        }

        String user = (String) props.get("user.name");
        logger.debug("User: " + user);
    }
}
