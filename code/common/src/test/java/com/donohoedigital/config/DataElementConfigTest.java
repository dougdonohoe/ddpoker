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

import junit.framework.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 7, 2008
 * Time: 8:28:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class DataElementConfigTest extends TestCase
{
    private static Logger logger = LogManager.getLogger(DataElementConfigTest.class);

    private DataElementConfig load()
    {
        String[] modules = {"common", "testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.CLIENT, null, true);
        return new DataElementConfig("testapp", null);
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    public void testLoad()
    {
        DataElementConfig dec = load();

        DataElement dogs = dec.get("dogs");
        assertNotNull(dogs);

        List<?> values = dogs.getListValues();
        for (Object o : values)
        {
            logger.info("Value: " + o);
        }
        assertTrue(values.contains("tahoe"));
        assertTrue(values.contains("dexter"));
        assertTrue(values.contains("zorro"));
        assertFalse(values.contains("rugby"));
    }

    /**
     * Enumeration values must come back in the order they are declared in the
     * xsd - combo boxes are populated straight from this list.
     */
    public void testEnumerationOrder()
    {
        DataElement dogs = load().get("dogs");
        assertTrue(dogs.isList());
        assertEquals(Arrays.asList("tahoe", "dexter", "zorro"), dogs.getListValues());
    }

    /**
     * Display values are looked up from list.&lt;element&gt;.&lt;value&gt; properties
     * (see testapp/client.properties)
     */
    public void testDisplayValues()
    {
        DataElement dogs = load().get("dogs");
        assertEquals("Tahoe", dogs.getDisplayValue("tahoe"));
        assertEquals("Dexter", dogs.getDisplayValue("dexter"));
        assertEquals("Zorro", dogs.getDisplayValue("zorro"));
    }

    /**
     * testapp's data-elements.xsd pulls in the global one with
     * &lt;xsd:include schemaLocation="classpath:..."/&gt; - types declared only
     * there must be present too.
     */
    public void testClasspathIncludeIsFollowed()
    {
        DataElement territoryType = load().get("territoryType");
        assertNotNull("territoryType comes from the included global data-elements.xsd", territoryType);
        assertEquals(Arrays.asList("land", "water", "edge", "decoration"), territoryType.getListValues());
    }

    /**
     * A named simple type with no enumeration is still registered, but is not a list
     */
    public void testNonEnumeratedTypeIsNotAList()
    {
        DataElement string = load().get("string");
        assertNotNull("plain simple types are registered too", string);
        assertFalse(string.isList());
        assertNull(string.getListValues());
    }
}
