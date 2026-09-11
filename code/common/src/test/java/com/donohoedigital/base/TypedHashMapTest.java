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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises every typed get/set/remove on TypedHashMap, along with the values
 * returned for missing keys and the bounds checking in getInteger().
 */
public class TypedHashMapTest
{
    private static final String KEY = "key";
    private static final String MISSING = "missing";

    @Test
    public void testString()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getString(MISSING));
        assertEquals("default", map.getString(MISSING, "default"));

        map.setString(KEY, "value");
        assertEquals("value", map.getString(KEY));
        assertEquals("value", map.getString(KEY, "default"));

        assertEquals("value", map.removeString(KEY));
        assertNull(map.removeString(KEY));
        assertTrue(map.isEmpty());

        // a stored null looks the same as a missing key to the getters
        map.setString(KEY, null);
        assertEquals(1, map.size());
        assertNull(map.getString(KEY));
        assertEquals("default", map.getString(KEY, "default"));
    }

    @Test
    public void testInteger()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getInteger(MISSING));
        assertEquals(42, map.getInteger(MISSING, 42));

        map.setInteger(KEY, 7);
        assertEquals(Integer.valueOf(7), map.getInteger(KEY));
        assertEquals(7, map.getInteger(KEY, 42));

        assertEquals(Integer.valueOf(7), map.removeInteger(KEY));
        assertNull(map.removeInteger(KEY));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testIntegerBounds()
    {
        TypedHashMap map = new TypedHashMap();

        map.setInteger(KEY, 5);
        assertEquals(5, map.getInteger(KEY, 0, 1, 10));      // in range
        assertEquals(6, map.getInteger(KEY, 0, 6, 10));       // raised to min
        assertEquals(4, map.getInteger(KEY, 0, 1, 4));        // lowered to max

        // the default is returned as given - deliberately not bounds checked, because
        // callers use an out-of-range default as a "not set" sentinel (see the Javadoc)
        assertEquals(99, map.getInteger(MISSING, 99, 1, 10));
        assertEquals(-1, map.getInteger(MISSING, -1, 0, 100));
    }

    @Test
    public void testLong()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getLong(MISSING));
        assertEquals(42L, map.getLong(MISSING, 42L));

        map.setLong(KEY, 7L);
        assertEquals(Long.valueOf(7L), map.getLong(KEY));
        assertEquals(7L, map.getLong(KEY, 42L));

        assertEquals(Long.valueOf(7L), map.removeLong(KEY));
        assertNull(map.removeLong(KEY));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testDate()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getLongAsDate(MISSING));

        Date date = new Date(1234567890000L);
        map.setLongFromDate(KEY, date);
        assertEquals(Long.valueOf(date.getTime()), map.getLong(KEY));
        assertEquals(date, map.getLongAsDate(KEY));

        // a null date removes the key instead of storing a null
        map.setLongFromDate(KEY, null);
        assertFalse(map.containsKey(KEY));
        assertNull(map.getLongAsDate(KEY));
    }

    @Test
    public void testDouble()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getDouble(MISSING));
        assertEquals(4.25, map.getDouble(MISSING, 4.25));

        map.setDouble(KEY, 1.5);
        assertEquals(Double.valueOf(1.5), map.getDouble(KEY));
        assertEquals(1.5, map.getDouble(KEY, 4.25));

        assertEquals(Double.valueOf(1.5), map.removeDouble(KEY));
        assertNull(map.removeDouble(KEY));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testBoolean()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getBoolean(MISSING));
        assertTrue(map.getBoolean(MISSING, true));
        assertFalse(map.getBoolean(MISSING, false));

        map.setBoolean(KEY, Boolean.TRUE);
        assertEquals(Boolean.TRUE, map.getBoolean(KEY));
        assertTrue(map.getBoolean(KEY, false));

        map.setBoolean(KEY, Boolean.FALSE);
        assertFalse(map.getBoolean(KEY, true));

        assertEquals(Boolean.FALSE, map.removeBoolean(KEY));
        assertNull(map.removeBoolean(KEY));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testList()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getList(MISSING));

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");

        map.setList(KEY, list);
        assertSame(list, map.getList(KEY));

        // the map holds the caller's list, it does not copy it
        list.add("c");
        assertEquals(3, map.getList(KEY).size());

        assertSame(list, map.removeList(KEY));
        assertNull(map.removeList(KEY));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testObject()
    {
        TypedHashMap map = new TypedHashMap();

        assertNull(map.getObject(MISSING));

        Object value = new Object();
        map.setObject(KEY, value);
        assertSame(value, map.getObject(KEY));

        assertSame(value, map.removeObject(KEY));
        assertNull(map.removeObject(KEY));
        assertTrue(map.isEmpty());

        // every setter is just put() - the typed getters see whatever was stored
        map.setObject(KEY, "string");
        assertEquals("string", map.getString(KEY));
        map.setString(KEY, "other");
        assertEquals("other", map.getObject(KEY));
    }

    @Test
    public void testWrongTypeThrows()
    {
        TypedHashMap map = new TypedHashMap();

        map.setInteger(KEY, 7);
        assertThrows(ClassCastException.class, () -> map.getString(KEY));
        assertThrows(ClassCastException.class, () -> map.getString(KEY, "default"));
        assertThrows(ClassCastException.class, () -> map.getLong(KEY));
        assertThrows(ClassCastException.class, () -> map.getDouble(KEY));
        assertThrows(ClassCastException.class, () -> map.getBoolean(KEY));
        assertThrows(ClassCastException.class, () -> map.getList(KEY));

        // remove() happens before the cast, so a failed removeXXX still empties the entry
        assertThrows(ClassCastException.class, () -> map.removeString(KEY));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToString()
    {
        TypedHashMap map = new TypedHashMap();
        assertEquals("", map.toString());

        List<String> list = new ArrayList<>();
        list.add("x");
        list.add("y");

        map.setString("b", "two");
        map.setInteger("a", 1);
        map.setList("c", list);
        map.setString("d", null);

        // TreeMap, so keys come out sorted, and lists are bracketed
        assertEquals("a=1, b=two, c=[x, y], d=null", map.toString());
    }
}
