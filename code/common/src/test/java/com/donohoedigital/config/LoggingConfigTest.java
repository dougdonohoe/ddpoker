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

import junit.framework.*;
import org.apache.log4j.*;

import java.io.*;
import java.net.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Oct 14, 2008
 * Time: 8:16:40 AM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class LoggingConfigTest extends TestCase
{
    public static final String UNITTEST_APPNAME = "unittests";
    private static final TestRuntimeDirectory runtime = new TestRuntimeDirectory();
    private static final PrintStream ORIGINALOUT = System.out;
    private static final PrintStream ORIGINALERR = System.err;

    /**
     * On setup, verify no temp files exist
     */
    @Override
    protected void setUp()
    {
        checkDirectoryDoesntExist(runtime.getServerHome());
        checkDirectoryDoesntExist(runtime.getClientHome(null));
    }

    private static void checkDirectoryDoesntExist(File dir)
    {
        if (dir.exists())
        {
            ConfigUtils.deleteDir(dir);
            fail(" a previous test failed to clean up after itself");
        }
    }

    /**
     * Test client
     */
    public void testClient()
    {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.CLIENT, runtime, false),
                runtime.getClientHome(null), true);
    }

    /**
     * Test headless client
     */
    public void testHeadlessClient()
    {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.HEADLESS_CLIENT, runtime, false),
                runtime.getClientHome(null), true);
    }

    /**
     * Test server
     */
    public void testServer()
    {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.SERVER, runtime, false),
                runtime.getServerHome(), true);
    }

    /**
     * Test command line
     */
    public void testCommandLine()
    {
        process(new LoggingConfig(UNITTEST_APPNAME, ApplicationType.COMMAND_LINE, runtime, false),
                runtime.getServerHome(), false);
    }

    /**
     * Test webapp
     */
    public void testWebapp()
    {
        // nothing to test here - just configuring log4j
    }

    /**
     * actual test logic
     */
    private void process(LoggingConfig logging, File runtimeDir, boolean verifyLogfile)
    {
        logging.init();

        ConfigUtils.verifyDirectory(runtimeDir);
        ConfigUtils.verifyDirectory(logging.getLogDir());
        Logger logger = Logger.getLogger("com.donohoedigital.test");
        String message = "Test message - A quick brown fox jumped over a lazy cow.";
        logger.info(message);
        //String SYSOUT = "SYSTEM OUT test message to sys-out";
        //System.out.println(SYSOUT);
        //String SYSERR = "SYSTEM ERR test message to sys-err";
        //System.err.println(SYSERR);

        if (verifyLogfile)
        {
            ConfigUtils.verifyFile(logging.getLogFile());
            String logcontents = ConfigUtils.readFile(logging.getLogFile());
            assertTrue("Log file should contain message", logcontents.contains(message));
            //assertTrue("Log file should contain message", logcontents.contains(SYSOUT));
            //assertTrue("Log file should contain message", logcontents.contains(SYSERR));
        }
    }

    /**
     * remove temp directories
     */
    @Override
    protected void tearDown() throws Exception
    {
        // reset log4j so we don't recreate test log file
        resetLog4j();

        // remove directories
        cleanup(runtime.getServerHome());
        cleanup(runtime.getClientHome(null));

        // reset sys out / sys err
        System.setErr(ORIGINALERR);
        System.setOut(ORIGINALOUT);
    }

    /**
     * Reset log4j to default log4j.properties file
     * Adapted from: http://www.bright-green.com/blog/2003_10_28/how_to_re_read_log4jproperties.html
     */
    private void resetLog4j()
    {
        LogManager.resetConfiguration();
        URL log4jprops = getClass().getClassLoader().getResource("log4j.properties");
        if (log4jprops != null)
        {
            PropertyConfigurator.configure(log4jprops);
        }
    }

    /**
     * Remove directory and all contents
     */
    private void cleanup(File dir)
    {
        if (dir.exists() && !ConfigUtils.deleteDir(dir))
        {
            fail("Could not cleanup " + dir.getAbsolutePath());
        }
    }
}