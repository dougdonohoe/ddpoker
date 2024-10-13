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

import com.donohoedigital.base.ApplicationError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import static com.donohoedigital.config.ApplicationType.*;

/**
 * Class to initiate logging.  Should be as close to the first thing that happens
 * in an application since it resets log4j configuration, which can leave any
 * loggers created prior to this turned off.  The 'debugLoggers' flag is useful
 * in development to see which loggers are created prior to init().
 */
public class LoggingConfig
{
    private static Logger logger = LogManager.getLogger(LoggingConfig.class);

    private static LoggingConfig loggingConfig;

    private final String appName;
    private final ApplicationType type;
    private final RuntimeDirectory runtimeDirectory;
    private final boolean allowUserOverrides;

    private LoggerContext loggerContext;
    private File logDir = null;
    private File logFile = null;

    /**
     * Default constructor
     */
    public LoggingConfig(String appName, ApplicationType type) {
        this(appName, type, new DefaultRuntimeDirectory(), true);
    }

    /**
     * Full constructor
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
    public void init()
    {
        if (loggingConfig != null) {
            if (!loggingConfig.appName.equals(appName) && !loggingConfig.type.equals(type)) {
                ApplicationError.warnNotNull(loggingConfig,
                        "LoggingConfig already initialized with different app/type (" +
                        loggingConfig.appName + "/" + loggingConfig.type + "), but now attempting to init with " +
                        appName + "/" + type);
            }
            return;
        }
        loggingConfig = this;

        boolean useDefault = false;
        String configStub = null;

        switch (type)
        {
            case CLIENT:
            case HEADLESS_CLIENT:
                configStub = "log4j2.client";
                break;

            case WEBAPP:
                configStub = "log4j2.webapp";
                break;

            case SERVER:
                configStub = "log4j2.server";
                break;

            case COMMAND_LINE:
                configStub = "log4j2.cmdline";
                break;
        }

        String sConfigName = configStub + ".properties";
        String sName;
        URL useroverride = null, useroverride2 = null;

        // look for user override
        if (allowUserOverrides) {
            sName = ConfigUtils.getUserName() + ".log4j2.properties";
            useroverride = new MatchingResources("classpath*:config/override/" + sName).getSingleResourceURL();

            sName = ConfigUtils.getUserName() + '.' + configStub + ".properties";
            useroverride2 = new MatchingResources("classpath*:config/override/" + sName).getSingleResourceURL();
        }

        // look for app-specific log4j file
        sName = sConfigName;
        URL appoverride = new MatchingResources("classpath*:config/" + appName + "/" + sName).getSingleResourceURL();

        // get type-specific log4j file
        URL url = new MatchingResources("classpath*:config/common/" + sConfigName).getSingleResourceURL();

        // if not there, look for default log4j2.properties
        if (url == null)
        {
            sName = "log4j2.properties";
            url = new MatchingResources("classpath*:" + sName).getSingleResourceURL();

            if (url != null)
            {
                useDefault = true;
                logger.warn("Log4j configuration file not found: {}", sConfigName);
                logger.warn("Using default log4j configuration file {}", url);
            }
        }

        // if still not found, configure basic
        if (url == null)
        {
            if (type != WEBAPP)
            {
                LogManager.shutdown();
                loggerContext = Configurator.initialize(null);
            }
            logger.warn("Log4j configuration file not found: {}", sConfigName);
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
                LogManager.shutdown();
                logger.warn("Log4j log directory not found: {}", logDir.getAbsolutePath(), o);
                loggerContext = Configurator.initialize(null);
            }

            // specify log file name
            String sFileName;
            if (type == CLIENT || type == HEADLESS_CLIENT || type == COMMAND_LINE) sFileName = appName + ".log";
            else if (type == WEBAPP) sFileName = appName + "-web.log";
            else sFileName = appName + "-server.log";
            logFile = new File(logDir, sFileName);

            // set properties - ${sys:log4j-*} are referenced in the configuration file(s)
            System.setProperty("log4j-logfile", logFile.getAbsolutePath());
            System.setProperty("log4j-logpath", logDir.getAbsolutePath());
            System.setProperty("log4j-appname", appName);
            System.setProperty("log4j-username", ConfigUtils.getUserName());
            System.setProperty("log4j-hostname", ConfigUtils.getLocalHost(false));
            System.setProperty("log4j-start", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

            // configure log4j based on config file
            if (!useDefault)
            {
                loggerContext = configure(url, appoverride, useroverride, useroverride2);
                if (type != COMMAND_LINE) logger.info("Log4j configured using: {}", url);
                if (appoverride != null) logger.info("Log4j app override file used: {}", appoverride);
                if (useroverride != null) logger.info("Log4j user override file used: {}", useroverride);
                if (useroverride2 != null) logger.info("Log4j user override file 2 used: {}", useroverride2);
            }
        }
    }

    private InputStream loadProperties(URL ...urls) {
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
            logger.error("Could not read configuration file from URL [{}].", url, e);
            logger.error("Ignoring configuration file [{}].", url);
        }

        // Write the merged properties to the output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            props.store(outputStream, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final boolean debugLoggers = false;

    /**
     * Load all properties files, then configure log4j
     */
    private LoggerContext configure(URL... urls) {

        // helpful to see what loggers exist, that will become disabled when we reload logging
        if (debugLoggers) {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Collection<org.apache.logging.log4j.core.Logger> loggers = context.getLoggers();
            loggers.forEach(
                    logger -> System.err.println(logger.getName() + ": " + logger.getLevel())
            );
        }

        // Need to shut down any log4j config already loaded
        LogManager.shutdown();

        // merge all properties files
        ConfigurationSource source;
        try {
            source = new ConfigurationSource(loadProperties(urls));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PropertiesConfiguration config = new PropertiesConfigurationFactory().getConfiguration(null, source);
        LoggerContext ctx = Configurator.initialize(config);

        // we have a new context, recreate the few loggers that are creating before this is run
        logger = LogManager.getLogger(LoggingConfig.class);
        ConfigUtils.resetLogger();

        return ctx;
    }

    public void shutdown() {
        if (loggerContext != null) {
            loggerContext.close();
            loggerContext = null;
        }
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

    public static LoggingConfig getLoggingConfig() {
        return loggingConfig;
    }

    /**
     * for testing
     */
    static void reset() {
        if (loggingConfig != null) {
            loggingConfig.shutdown();
            loggingConfig = null;
        }
    }
}
