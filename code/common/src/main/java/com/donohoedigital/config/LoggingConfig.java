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

import com.donohoedigital.base.*;
import static com.donohoedigital.config.ApplicationType.*;
import org.apache.log4j.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * Class to initiate logging
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class LoggingConfig
{
    private static final Logger logger = Logger.getLogger(LoggingConfig.class);

    private final String appName;
    private final ApplicationType type;
    private final RuntimeDirectory runtimeDirectory;
    private final boolean allowUserOverrides;

    private File logDir = null;
    private File logFile = null;

    private static PrintStream origStdErr = null;
    private static RedirectStream stdErr = null;

    /**
     * Construct
     */
    public LoggingConfig(String appName, ApplicationType type, RuntimeDirectory runtimeDirectory,
                         boolean allowUserOverrides)
    {
        this.appName = appName;
        this.type = type;
        this.runtimeDirectory = runtimeDirectory;
        this.allowUserOverrides = allowUserOverrides;
    }

    /**
     * Initialize logging based on specified application type.
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    public void init()
    {
        ApplicationError.warnNotNull(logDir, "LoggingConfig already initialized");

        boolean useDefault = false;
        String configStub = null;

        switch (type)
        {
            case CLIENT:
            case HEADLESS_CLIENT:
                configStub = "log4j.client";
                break;

            case WEBAPP:
                configStub = "log4j.webapp";
                break;

            case SERVER:
                configStub = "log4j.server";
                break;

            case COMMAND_LINE:
                configStub = "log4j.cmdline";
                break;
        }

        String sConfigName = configStub + ".properties";
        String sName;
        URL useroverride = null, useroverride2 = null;

        // look for user override
        if (allowUserOverrides) {
            sName = ConfigUtils.getUserName() + ".log4j.properties";
            useroverride = new MatchingResources("classpath*:config/override/" + sName).getSingleResourceURL();

            sName = ConfigUtils.getUserName() + '.' + configStub + ".properties";
            useroverride2 = new MatchingResources("classpath*:config/override/" + sName).getSingleResourceURL();
        }

        // look for app-specific log4j file
        sName = sConfigName;
        URL appoverride = new MatchingResources("classpath*:config/" + appName + "/" + sName).getSingleResourceURL();

        // get type-specific log4j file
        URL url = new MatchingResources("classpath*:config/common/" + sConfigName).getSingleResourceURL();

        // if not there, look for default log4j.properties
        if (url == null)
        {
            sName = "log4j.properties";
            url = new MatchingResources("classpath*:" + sName).getSingleResourceURL();

            if (url != null)
            {
                useDefault = true;
                logger.warn("Log4j configuration file not found: " + sConfigName);
                logger.warn("Using default log4j configuration file " + url);
            }
        }

        // if still not found, configure basic
        if (url == null)
        {
            if (type != WEBAPP)
            {
                BasicConfigurator.resetConfiguration();
                BasicConfigurator.configure();
            }
            logger.warn("Log4j configuration file not found: " + sConfigName);
            logger.warn("Log4j set to basic configuration (console)");
        }
        else
        {
            // determine log dir
            File parentdir;
            if (type == CLIENT || type == HEADLESS_CLIENT)
            {
                parentdir = runtimeDirectory.getClientHome(appName);
            }
            else
            {
                parentdir = runtimeDirectory.getServerHome();
            }
            logDir = new File(parentdir, "log");

            // make sure it exists
            try
            {
                ConfigUtils.verifyNewDirectory(logDir);
            }
            catch (ApplicationError o)
            {
                BasicConfigurator.configure();
                logger.warn("Log4j log directory not found: " + logDir.getAbsolutePath(), o);
                BasicConfigurator.resetConfiguration();
            }

            // specify log file name
            String sFileName;
            if (type == CLIENT || type == HEADLESS_CLIENT || type == COMMAND_LINE) sFileName = appName + ".log";
            else if (type == WEBAPP) sFileName = appName + "-web.log";
            else sFileName = appName + "-server.log";
            logFile = new File(logDir, sFileName);

            // set properties - ${log4j-XXXXX} are referenced in the configuration file(s)
            System.setProperty("log4j-logfile", logFile.getAbsolutePath());
            System.setProperty("log4j-logpath", logDir.getAbsolutePath());
            System.setProperty("log4j-appname", appName);
            System.setProperty("log4j-username", ConfigUtils.getUserName());
            System.setProperty("log4j-hostname", ConfigUtils.getLocalHost(false));
            System.setProperty("log4j-start", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

            // configure log4j based on config file
            if (!useDefault)
            {
                configure(url, appoverride, useroverride, useroverride2);
                if (type != COMMAND_LINE) logger.info("Log4j configured using: " + url);
                if (appoverride != null) logger.info("Log4j app override file used: " + appoverride);
                if (useroverride != null) logger.info("Log4j user override file used: " + useroverride);
                if (useroverride2 != null) logger.info("Log4j user override file 2 used: " + useroverride2);
            }

            // Redirect stderr/stdout through log4j
            if (type != WEBAPP)
            {
                // only set once (to avoid issues during testing)
                if (origStdErr == null)
                {
                    //noinspection NonThreadSafeLazyInitialization
                    origStdErr = System.err;

                    // standard error is easy
                    stdErr = new RedirectStream("STDERR ", true);
                    System.setErr(new PrintStream(stdErr.getStream(), true));
                }

//                // TODO: standard out is hard if Console appender is on ... can't quite get this to work properly
//                Enumeration<?> e = Logger.getRootLogger().getAllAppenders();
//                while (e.hasMoreElements()) {
//                    Appender a = (Appender) e.nextElement();
//                    logger.debug("Appender: "+ a);
//                    if (a instanceof ConsoleAppender) {
//                        ConsoleAppender consoleAppender = (ConsoleAppender) a;
//                        if (consoleAppender.getFollow()) {
//                            consoleAppender.setFollow(false);
//                            consoleAppender.activateOptions();
//                        }
//                        //Logger.getRootLogger().removeAppender(a);
//                    }
//                }
//
//                System.setOut(new PrintStream(new DebugStream("STDOUT ", false), true));

            }
        }
    }

    /**
     * Load all properties files, then configure log4j
     */
    private void configure(URL... urls)
    {
        Properties props = new Properties();
        URL url = null;
        try
        {
            for (URL u : urls)
            {
                url = u;
                if (url == null) continue;
                props.load(url.openStream());
            }
        }
        catch (java.io.IOException e)
        {
            logger.error("Could not read configuration file from URL [" + url + "].", e);
            logger.error("Ignoring configuration file [" + url + "].");
        }
        PropertyConfigurator.configure(props);
    }

    /**
     * Get application name
     */
    public String getAppName()
    {
        return appName;
    }

    /**
     * Get application type
     */
    public ApplicationType getType()
    {
        return type;
    }

    /**
     * Get runtime directory
     */
    public RuntimeDirectory getConfigDir()
    {
        return runtimeDirectory;
    }

    /**
     * Return logging dir
     */
    public File getLogDir()
    {
        return logDir;
    }

    /**
     * Return logging dir
     */
    public File getLogFile()
    {
        return logFile;
    }

    /**
     * Get stderr redirect stream
     */
    public static RedirectStream getStdErr()
    {
        return stdErr;
    }
}
