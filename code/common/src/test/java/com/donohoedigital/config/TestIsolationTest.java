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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test run must not read or write the developer's real DD Poker data.
 * <p/>
 * Both halves of that are set up in the surefire configuration in code/pom.xml, and both
 * are easy to lose in a build change without anyone noticing until a test has scribbled
 * on a real profile or preference.  These assertions fail loudly instead.
 */
public class TestIsolationTest
{
    /**
     * Everything the client writes - logs, saved games, profiles, the database - hangs
     * off user.home, which the build points at target/test-home.
     */
    @Test
    public void userHomeIsRedirectedIntoTheBuildDirectory()
    {
        String home = System.getProperty("user.home");

        assertTrue(home.contains("test-home"),
                   "user.home should point into the build directory, but was: " + home);
        assertTrue(new File(home).getAbsolutePath().contains("target"),
                   "user.home should be under target/ so 'mvn clean' discards it: " + home);
    }

    /**
     * user.home does not cover java.util.prefs - on a Mac the backing store is
     * ~/Library/Preferences no matter what user.home says - so the build also swaps in
     * an in-memory factory.
     */
    @Test
    public void preferencesAreInMemory()
    {
        assertEquals("com.donohoedigital.config.MemoryPreferencesFactory",
                     System.getProperty("java.util.prefs.PreferencesFactory"),
                     "the build should select the in-memory preferences factory");

        // the real store would be MacOSXPreferences / FileSystemPreferences
        String impl = Preferences.userRoot().getClass().getName();
        assertTrue(impl.startsWith("com.donohoedigital.config.MemoryPreferencesFactory"),
                   "preferences should be in memory, but the root was: " + impl);
    }

    /**
     * And they really are a scratch store - a value written here is gone next JVM, so it
     * cannot leak into the developer's preferences or between builds.
     */
    @Test
    public void preferencesStartEmptyAndRoundTrip() throws Exception
    {
        Preferences node = Preferences.userRoot().node("dd-isolation-check");

        assertNull(node.get("written-by-a-previous-run", null),
                   "preferences should not survive from an earlier JVM");

        node.put("key", "value");
        assertEquals("value", node.get("key", null));

        node.removeNode();
    }
}
