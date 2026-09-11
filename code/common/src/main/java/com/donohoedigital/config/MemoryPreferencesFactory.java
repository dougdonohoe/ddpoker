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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Preferences that live in memory and disappear with the JVM.
 * <p/>
 * Selected with -Djava.util.prefs.PreferencesFactory=com.donohoedigital.config.MemoryPreferencesFactory,
 * which the build sets for tests.  Without it a test run reads and writes the developer's
 * real preferences - and on a Mac that is not something -Duser.home can redirect, because
 * MacOSXPreferences writes to ~/Library/Preferences regardless.
 * <p/>
 * This lives in main rather than test because the system property is set for every
 * module's test JVM, so the class has to be on every module's classpath.
 */
public class MemoryPreferencesFactory implements PreferencesFactory
{
    private static final Preferences USER = new MemoryPreferences(null, "");
    private static final Preferences SYSTEM = new MemoryPreferences(null, "");

    @Override
    public Preferences userRoot()
    {
        return USER;
    }

    @Override
    public Preferences systemRoot()
    {
        return SYSTEM;
    }

    private static class MemoryPreferences extends AbstractPreferences
    {
        private final Map<String, String> values = new ConcurrentHashMap<>();
        private final Map<String, MemoryPreferences> children = new ConcurrentHashMap<>();

        MemoryPreferences(MemoryPreferences parent, String name)
        {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value)
        {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key)
        {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key)
        {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi()
        {
            MemoryPreferences parent = (MemoryPreferences) parent();
            if (parent != null) parent.children.remove(name());
            values.clear();
        }

        @Override
        protected String[] keysSpi()
        {
            return values.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi()
        {
            return children.keySet().toArray(new String[0]);
        }

        @Override
        protected AbstractPreferences childSpi(String name)
        {
            return children.computeIfAbsent(name, n -> new MemoryPreferences(this, n));
        }

        // nothing to sync or flush - there is no backing store

        @Override
        protected void syncSpi()
        {
        }

        @Override
        protected void flushSpi()
        {
        }
    }
}
