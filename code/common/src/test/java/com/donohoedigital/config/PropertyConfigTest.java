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

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 6, 2008
 * Time: 4:43:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyConfigTest extends TestCase
{
    public void testLoadClient()
    {
        System.getProperties().setProperty("user.name", "unit-tester");
        String[] modules = {"common", "testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.CLIENT, null, true);

        assertTrue(PropertyConfig.getRequiredBooleanProperty("test.common"));
        assertTrue(PropertyConfig.getRequiredBooleanProperty("test.common.override"));

        assertTrue(PropertyConfig.getRequiredBooleanProperty("test.boolean.true"));
        assertFalse(PropertyConfig.getRequiredBooleanProperty("test.boolean.false"));
        assertTrue(PropertyConfig.getRequiredBooleanProperty("test.boolean.yes"));
        assertFalse(PropertyConfig.getRequiredBooleanProperty("test.boolean.no"));
        assertTrue(PropertyConfig.getRequiredBooleanProperty("test.boolean.+"));
        assertFalse(PropertyConfig.getRequiredBooleanProperty("test.boolean.-"));
        assertTrue(PropertyConfig.getRequiredBooleanProperty("test.boolean.1"));
        assertFalse(PropertyConfig.getRequiredBooleanProperty("test.boolean.0"));

        assertEquals(PropertyConfig.getRequiredStringProperty("test.string"), "This is a string");
        assertEquals(PropertyConfig.getRequiredIntegerProperty("test.integer"), 42);
        assertEquals(PropertyConfig.getRequiredDoubleProperty("test.double"), 3.14159d, .0000001d);

        assertEquals(PropertyConfig.getMessage("test.message"), "No replacement");
        assertEquals(PropertyConfig.getMessage("test.message.one", "just"), "Replace just one.");
        assertEquals(PropertyConfig.getMessage("test.message.two", "this", "that"), "Replace this and that.");

        // override in unit-tester.properties
        assertTrue(PropertyConfig.getRequiredBooleanProperty("override.set"));
    }
}
