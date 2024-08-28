package com.donohoedigital.comms;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public void testParseVersion() {
        // major/minor
        Version v = new Version("3.1");
        assertEquals(3, v.getMajor());
        assertEquals(1, v.getMinor());

        // old style patch parsing
        v = new Version("3.1p2");
        assertEquals(3, v.getMajor());
        assertEquals(1, v.getMinor());
        assertEquals(2, v.getPatch());
        assertEquals("3.1.2", v.toString());

        // new style patch
        v = new Version("3.1.4");
        assertEquals(3, v.getMajor());
        assertEquals(1, v.getMinor());
        assertEquals(4, v.getPatch());
        assertEquals("3.1.4", v.toString());
    }
}