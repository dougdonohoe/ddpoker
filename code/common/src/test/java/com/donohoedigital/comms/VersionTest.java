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
package com.donohoedigital.comms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for parsing and ordering versions - what {@code UpdateCheck} in the poker module
 * relies on when it compares a GitHub release tag against the running build.
 */
public class VersionTest {

    // -------------------------------------------------------------------------
    // parse / toString round-trip
    // -------------------------------------------------------------------------

    @Test
    public void parse_roundTripsToString() {
        assertRoundTrip("3.1");
        assertRoundTrip("3.1.8");
        assertRoundTrip("2.0b6.4");
        assertRoundTrip("3.1a2");
        assertRoundTrip("1.2d");
        assertRoundTrip("3.1.8_en");
        assertRoundTrip("10.20.30");
        assertRoundTrip("2.0b12_fr");
    }

    private static void assertRoundTrip(String s) {
        Version v = Version.parse(s);
        assertTrue(v != null, "did not parse: " + s);
        assertEquals(s, v.toString());
    }

    @Test
    public void parse_readsTheParts() {
        // major/minor
        Version v = Version.parse("3.1");
        assertEquals(3, v.getMajor());
        assertEquals("3", v.getMajorAsString());
        assertEquals(1, v.getMinor());
        assertEquals(0, v.getPatch());
        assertTrue(v.isProduction());
        assertFalse(v.isDemo());
        assertNull(v.getLocale());

        // new style patch
        v = Version.parse("3.1.4");
        assertEquals(3, v.getMajor());
        assertEquals(1, v.getMinor());
        assertEquals(4, v.getPatch());

        Version beta = Version.parse("2.0b6.4");
        assertEquals(2, beta.getMajor());
        assertEquals(0, beta.getMinor());
        assertEquals(6, beta.getAlphaBetaVersion());
        assertEquals(4, beta.getPatch());
        assertTrue(beta.isBeta());
        assertFalse(beta.isAlpha());
        assertFalse(beta.isProduction());

        Version alpha = Version.parse("2.0a2");
        assertTrue(alpha.isAlpha());
        assertFalse(alpha.isBeta());

        assertTrue(Version.parse("1.2d").isDemo());
        assertEquals("en", Version.parse("3.1.8_en").getLocale());
    }

    /** The old way of writing a patch, still seen in saved data and old version strings. */
    @Test
    public void parse_understandsOldStylePatch() {
        Version v = Version.parse("3.1p2");
        assertEquals(3, v.getMajor());
        assertEquals(1, v.getMinor());
        assertEquals(2, v.getPatch());
        assertEquals("3.1.2", v.toString());
    }

    /** A leading "v" is tolerated, since tags elsewhere are often written that way. */
    @Test
    public void parse_toleratesVPrefix() {
        assertEquals(Version.parse("3.1.8"), Version.parse("v3.1.8"));
    }

    @Test
    public void parse_trimsWhitespace() {
        assertEquals(Version.parse("3.1.8"), Version.parse("  3.1.8\n"));
    }

    /** Anything that is not a version returns null rather than throwing - see Version.parse. */
    @Test
    public void parse_rejectsNonVersions() {
        assertNull(Version.parse(null));
        assertNull(Version.parse(""));
        assertNull(Version.parse("   "));
        assertNull(Version.parse("3"));
        assertNull(Version.parse("latest"));
        assertNull(Version.parse("3.1.x"));
        assertNull(Version.parse("v3"));
        assertNull(Version.parse("2.0c1"));
        assertNull(Version.parse("2.0b"));
        assertNull(Version.parse("3.1.0.1"));
        assertNull(Version.parse("<!DOCTYPE html><html lang=\"en\">"));
        assertNull(Version.parse("releases/tag/3.1.8"));
        // too big for an int
        assertNull(Version.parse("99999999999.0"));
    }

    // -------------------------------------------------------------------------
    // Ordering
    // -------------------------------------------------------------------------

    @Test
    public void isNewerThan_comparesNumbers() {
        assertNewer("3.1.8", "3.1.7");
        assertNewer("3.1", "3.0.6");
        assertNewer("4.0", "3.9.9");
        assertNewer("3.1.10", "3.1.9");
    }

    /**
     * An alpha or beta of a version comes before the version itself, and a patch applies within
     * a release - 2.0b6.4 is Beta 6, Patch 4, which still precedes 2.0.
     */
    @Test
    public void isNewerThan_ranksPrereleases() {
        assertNewer("2.0", "2.0b6");
        assertNewer("2.0", "2.0b6.4");
        assertNewer("2.0", "2.0a1");
        assertNewer("2.0b6.4", "2.0b6");
        assertNewer("2.0b8", "2.0b7");
        assertNewer("2.0b1", "2.0a9");
    }

    @Test
    public void isNewerThan_isFalseForSameOrOlder() {
        assertFalse(Version.parse("3.1.8").isNewerThan(Version.parse("3.1.8")));
        assertFalse(Version.parse("2.0b6.4").isNewerThan(Version.parse("2.0b6.4")));
        assertFalse(Version.parse("3.1.5").isNewerThan(Version.parse("3.1.8")));
    }

    /** A failed lookup hands back null; that must not read as "newer". */
    @Test
    public void isNewerThan_isFalseForNull() {
        assertFalse(Version.parse("9.9.9").isNewerThan(null));
    }

    /** The locale says who a build is for, not when it is from; same for the demo flag. */
    @Test
    public void ordering_ignoresLocaleAndDemo() {
        assertEquals(Version.parse("3.1.8"), Version.parse("3.1.8_fr"));
        assertEquals(Version.parse("1.2"), Version.parse("1.2d"));
        assertFalse(Version.parse("3.1.8_fr").isNewerThan(Version.parse("3.1.8_en")));
    }

    /**
     * The real ladder DD Poker has shipped (the history in {@code PokerConstants}, oldest first).
     * Shuffled and re-sorted, it has to come back in the same order.
     */
    @Test
    public void ordering_reproducesShippedHistory() {
        List<String> oldestFirst = List.of(
                "2.0b6.4", "2.5.3", "3.0.5", "3.0.6", "3.1", "3.1.1", "3.1.2", "3.1.3",
                "3.1.4", "3.1.5", "3.1.6", "3.1.7", "3.1.8");

        List<Version> shuffled = new ArrayList<>(oldestFirst.stream().map(Version::parse).toList());
        Collections.shuffle(shuffled);
        Collections.sort(shuffled);

        assertEquals(oldestFirst, shuffled.stream().map(Version::toString).toList());
    }

    /** isBefore/isAfter are the older spelling of the same ordering, used by online compat checks. */
    @Test
    public void isBeforeAndIsAfter_followTheSameOrder() {
        Version older = Version.parse("3.0.4");
        Version newer = Version.parse("3.1.8");

        assertTrue(older.isBefore(newer));
        assertFalse(newer.isBefore(older));
        assertTrue(newer.isAfter(older));
        assertFalse(older.isAfter(newer));

        // neither, for the same version
        assertFalse(older.isBefore(Version.parse("3.0.4")));
        assertFalse(older.isAfter(Version.parse("3.0.4")));

        assertTrue(Version.parse("2.0b6.4").isBefore(Version.parse("2.0")));
    }

    /** isMajorMinorBefore ignores the patch, so builds of the same release tie. */
    @Test
    public void isMajorMinorBefore_ignoresPatch() {
        assertFalse(Version.parse("3.1.8").isMajorMinorBefore(Version.parse("3.1")));
        assertFalse(Version.parse("3.1").isMajorMinorBefore(Version.parse("3.1.8")));
        assertTrue(Version.parse("3.0.6").isMajorMinorBefore(Version.parse("3.1")));
        assertTrue(Version.parse("2.0b6.4").isMajorMinorBefore(Version.parse("2.0")));
    }

    @Test
    public void equalsAndHashCode_agreeWithCompareTo() {
        Version a = Version.parse("3.1.8");
        Version b = new Version(3, 1, 8, true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, Version.parse("3.1.7"));
        assertNotEquals("3.1.8", a);
        assertNotEquals(null, a);
    }

    private static void assertNewer(String newer, String older) {
        Version n = Version.parse(newer);
        Version o = Version.parse(older);
        assertTrue(n.isNewerThan(o), newer + " should be newer than " + older);
        assertFalse(o.isNewerThan(n), older + " should not be newer than " + newer);
    }
}
