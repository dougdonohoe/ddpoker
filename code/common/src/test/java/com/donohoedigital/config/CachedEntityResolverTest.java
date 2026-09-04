/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the two schemes {@link CachedEntityResolver} understands, which is how
 * &lt;xsd:include schemaLocation="classpath:..."/&gt; is resolved when the JDK
 * parser validates a config file.
 */
public class CachedEntityResolverTest
{
    @Test
    public void testResolvesClasspath() throws MalformedURLException
    {
        URL url = CachedEntityResolver.instance().getMatch("classpath:config/xml-schema/data-elements.xsd");
        assertNotNull(url);
        assertTrue(url.toString().endsWith("config/xml-schema/data-elements.xsd"), url.toString());
    }

    @Test
    public void testResolvesFile() throws MalformedURLException
    {
        URL url = CachedEntityResolver.instance().getMatch("file:/tmp/whatever.xsd");
        assertNotNull(url);
        assertEquals("file", url.getProtocol());
        assertEquals("/tmp/whatever.xsd", url.getPath());
    }

    @Test
    public void testUnknownSchemeReturnsNull() throws MalformedURLException
    {
        assertNull(CachedEntityResolver.instance().getMatch("config/xml-schema/data-elements.xsd"));
    }

    @Test
    public void testResultIsCached() throws MalformedURLException
    {
        CachedEntityResolver resolver = CachedEntityResolver.instance();
        String name = "classpath:config/xml-schema/data-elements.xsd";
        assertSame(resolver.getMatch(name), resolver.getMatch(name));
    }
}
