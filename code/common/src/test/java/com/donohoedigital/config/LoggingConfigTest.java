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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Oct 14, 2008
 * Time: 8:16:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoggingConfigTest extends TestCase {
    public static final String UNITTEST_APPNAME = "unittests";
    private static final TestRuntimeDirectory runtime = new TestRuntimeDirectory();

    /**
     * On setup, verify no temp files exist
     */
    @Override
    protected void setUp() {
        LoggingConfig.reset();
        checkDirectoryDoesntExist(runtime.getServerHome());
        checkDirectoryDoesntExist(runtime.getClientHome(null));
    }

    private static void checkDirectoryDoesntExist(File dir) {
        if (dir.exists()) {
            ConfigUtils.deleteDir(dir);
            fail(" a previous test failed to clean up after itself");
        }
    }

    /**
     * Test client
     */
    public void testClient() {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.CLIENT, runtime, false),
                runtime.getClientHome(null), "GUI", true);
    }

    /**
     * Test headless client
     */
    public void testHeadlessClient() {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.HEADLESS_CLIENT, runtime, false),
                runtime.getClientHome(null), "GUI", true);
    }

    /**
     * Test server
     */
    public void testServer() {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.SERVER, runtime, false),
                runtime.getServerHome(), "SRV", true);
    }

    /**
     * Test command line
     */
    public void testCommandLine() {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.COMMAND_LINE, runtime, false),
                runtime.getServerHome(), "CLI", false);
    }

    /**
     * Test webapp
     */
    public void testWebapp() {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.WEBAPP, runtime, false),
                runtime.getServerHome(), "WEB", true);
    }

    public void testOverride() {
        process(new LoggingConfig("unit-test-override", ApplicationType.HEADLESS_CLIENT, runtime, false),
                runtime.getClientHome(null), "OVER", true);
    }

    /**
     * actual test logic
     */
    private void process(LoggingConfig logging, File runtimeDir, String slug, boolean verifyLogfile) {
        TeePrintStream tee = new TeePrintStream();
        try {
            logging.init();

            ConfigUtils.verifyDirectory(runtimeDir);
            ConfigUtils.verifyDirectory(logging.getLogDir());
            Logger logger = LogManager.getLogger("com.donohoedigital.test");
            String message = "Test message - A quick brown fox jumped over a lazy cow.";
            logger.info(message);

            // capture lines include 0-2 informational messages from LoggingConfig
            //  + if CMD, 1st line "Log4j configured using ..." is skipped
            //  + if OVER (testing overrides), extra line for that override file
            int expected = slug.equals("CLI") ? 1 : slug.equals("OVER") ? 3 : 2;

            // inspect stdout
            String[] lines = tee.getCapturedLines();
            assertEquals(expected, lines.length);
            String line = lines[expected - 1];
            assertTrue("should contain " + slug + " [main", line.contains(" " + slug + " [main"));
            assertTrue("Stdout file should contain message: " + message, line.contains(message));

            if (verifyLogfile) {
                ConfigUtils.verifyFile(logging.getLogFile());
                String contents = ConfigUtils.readFile(logging.getLogFile());
                assertTrue("should contain " + slug + " [main", contents.contains(" " + slug + " [main"));
                assertTrue("Log file should contain message: " + message, contents.contains(message));
            }
        } finally {
            tee.restoreOriginal();
            logging.shutdown();
        }
    }

    /**
     * remove temp directories
     */
    @Override
    protected void tearDown() {
        // remove directories
        cleanup(runtime.getServerHome());
        cleanup(runtime.getClientHome(null));
    }

    /**
     * Remove directory and all contents
     */
    private void cleanup(File dir) {
        if (dir.exists() && !ConfigUtils.deleteDir(dir)) {
            fail("Could not cleanup " + dir.getAbsolutePath());
        }
    }
}