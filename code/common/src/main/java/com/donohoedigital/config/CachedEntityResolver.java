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
package com.donohoedigital.config;

import org.apache.log4j.*;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 9, 2008
 * Time: 3:30:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class CachedEntityResolver implements EntityResolver, XMLEntityResolver
{
    private static Logger logger = Logger.getLogger(XMLConfigFileLoader.class);

    private Map<String, URL> matches = new HashMap<String, URL>();

    private static CachedEntityResolver resolver = null;

    public synchronized static CachedEntityResolver instance()
    {
        if (resolver == null)
        {
            resolver = new CachedEntityResolver();
        }
        return resolver;
    }

    private CachedEntityResolver()
    {
    }

    public URL getMatch(String name) throws MalformedURLException
    {
        URL url = matches.get(name);
        if (url == null)
        {
            // if it starts with file:, we've already resolved it
            if (name.startsWith("file:"))
            {
                url = new URL(name);
            }
            else if (name.startsWith("classpath:"))
            {
                //logger.debug("Resolving XML include: " + name);
                String search = name.replace("classpath:", "classpath*:"); // * not allowed in XML file
                url = new MatchingResources(search).getSingleRequiredResourceURL();
            }
            if (url != null) matches.put(name, url);
        }
        return url;
    }

    /**
     * Resolve include statements
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
    {
        URL url = getMatch(systemId);
        if (url == null) return null;
        return new InputSource(url.openStream());
    }

    /**
     * Resolve includes
     */
    public XMLInputSource resolveEntity(XMLResourceIdentifier xmlResourceIdentifier) throws XNIException, IOException
    {
        String name = xmlResourceIdentifier.getLiteralSystemId();
        URL url = getMatch(name);
        if (url == null) return null;
        return new XMLInputSource(name, url.toString(), null);
    }
}
