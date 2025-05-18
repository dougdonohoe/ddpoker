/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
/*
 * PropertyConfig.java
 *
 * Created on November 16, 2002, 6:49 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
public class PropertyConfig extends Properties
{
    private static Logger logger = LogManager.getLogger(PropertyConfig.class);

    // config file names
    private static final String PROPS_CONFIG_COMMON = "common.properties";
    private static final String PROPS_CONFIG_CLIENT = "client.properties";
    private static final String PROPS_CONFIG_CMDLINE = "cmdline.properties";
    private static final String PROPS_CONFIG_SERVER = "server.properties";

    // the one instance
    private static PropertyConfig propConfig = null;

    // testing - don't throw missing exceptions
    private static boolean testing = false;

    /**
     * Creates a new instance of PropertyConfig from the Appconfig file
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    public PropertyConfig(String appName, String[] modules, ApplicationType type, String sLocale, boolean allowOverrides)
    {
        ApplicationError.warnNotNull(propConfig, "PropertyConfig already initialized");
        propConfig = this;

        init(appName, modules, type, sLocale, allowOverrides);
    }

    /**
     * is initialized?
     */
    public static boolean isInitialized()
    {
        return propConfig != null;
    }

    /**
     * Load each .properties file from each module
     */
    private void init(String appName, String[] modules, ApplicationType type, String sLocale, boolean allowOverrides)
    {
        ApplicationError.assertNotNull(modules, "Modules list is null");

        // file only used for combining dir/name
        File file;

        for (String module : modules)
        {
            // common props
            file = new File(module, PROPS_CONFIG_COMMON);
            loadURL(file, sLocale, false);

            // props specific to client/server
            String name;
            switch (type)
            {
                case CLIENT:
                case HEADLESS_CLIENT:
                    name = PROPS_CONFIG_CLIENT;
                    break;

                case COMMAND_LINE:
                    name = PROPS_CONFIG_CMDLINE;
                    break;

                default:
                    name = PROPS_CONFIG_SERVER;
            }
            file = new File(module, name);
            loadURL(file, sLocale, false);

            // look for user specific overrides
            if (allowOverrides)
            {
                String user = ConfigUtils.getUserName();
                file = new File(module + "/override/" + user.toLowerCase() + ".properties");
                loadURL(file, sLocale, true);
            }
        }

        // look for overrides in data dir
        if (type == ApplicationType.CLIENT)
        {
            RuntimeDirectory dir = new DefaultRuntimeDirectory();
            File userdir = dir.getClientHome(appName);
            File override = new File(userdir, "testing.properties");
            if (override.exists())
            {
                logger.info("Loading testing overrides from " + override.getPath());
                FileInputStream stream = ConfigUtils.getFileInputStream(override);
                try
                {
                    load(stream);
                }
                catch (IOException e)
                {
                    throw new ApplicationError(e);
                }
                finally
                {
                    ConfigUtils.close(stream);
                }
            }

            // look for buildnumber.properties
            file = new File("buildnumber.properties");
            loadURL(file, null, false);
        }
    }

    private void loadURL(File file, String sLocale, boolean bOverride)
    {
        URL props = null;

        // if locale, look for file based on that locale
        if (sLocale != null)
        {
            props = new MatchingResources("classpath*:config/" + file.getPath() + '.' + sLocale).getSingleResourceURL();
        }

        // if no locale, look for regular file
        if (props == null)
        {
            props = new MatchingResources("classpath*:config/" + file.getPath()).getSingleResourceURL();
        }

        // if props file is not there, no big deal
        if (props == null) return;

        // log if doing overrides
        if (bOverride)
        {
            logger.info("Loading local overrides from " + file.getPath());
        }

        //logger.debug("Loading: " + props);

        InputStream is = null;
        try
        {
            is = props.openStream();
            load(is);
        }
        // since we verified, file should exist
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            ConfigUtils.close(is);
        }
    }

    /**
     * return all property/values where key starts with given value
     */
    @SuppressWarnings({"unchecked"})
    public Map<String, String> getMatching(String sStartsWith)
    {
        Map<String, String> map = new HashMap<String, String>();
        Enumeration<String> enumer = (Enumeration<String>) propertyNames();
        while (enumer.hasMoreElements())
        {
            String propName = enumer.nextElement();
            if (propName.startsWith(sStartsWith))
            {
                map.put(propName, getProperty(propName));
            }
        }

        return map;
    }

    public static int getRequiredIntegerProperty(String sKey)
    {
        getRequiredStringProperty(sKey);
        return getIntegerProperty(sKey, 0);
    }

    /**
     * Get property, return nDefault if not there
     */
    public static int getIntegerProperty(String sKey, int nDefault)
    {
        try
        {
            String sNum = getStringProperty(sKey, null, false);
            if (sNum == null) return nDefault;
            return Integer.parseInt(sNum);
        }
        catch (NumberFormatException ignored)
        {
            if (testing) return 0;
            String sMsg = "Error converting '" + sKey + "' value to int: " + getStringProperty(sKey);
            logger.warn(sMsg);
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg, "Make sure value is a valid integer");
        }
    }

    public static double getRequiredDoubleProperty(String sKey)
    {
        getRequiredStringProperty(sKey);
        return getDoubleProperty(sKey, 0.0);
    }

    /**
     * Get property, return dDefault if not there
     */
    public static double getDoubleProperty(String sKey, double dDefault)
    {
        try
        {
            String sNum = getStringProperty(sKey, null, false);
            if (sNum == null) return dDefault;
            return Double.parseDouble(sNum);
        }
        catch (NumberFormatException ignored)
        {
            if (testing) return 0d;
            String sMsg = "Error converting '" + sKey + "' value to double: " + getStringProperty(sKey);
            logger.error(sMsg);
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg, "Make sure value is a valid double");
        }
    }

    public static boolean getRequiredBooleanProperty(String sKey)
    {
        getRequiredStringProperty(sKey);
        return getBooleanProperty(sKey, false);
    }

    /**
     * Get property, return sDefault if not there or not valid boolean
     */
    public static boolean getBooleanProperty(String sKey, boolean bDefault)
    {
        return getBooleanProperty(sKey, bDefault, true);
    }

    /**
     * Get property, return sDefault if not there or not valid boolean
     */
    public static boolean getBooleanProperty(String sKey, boolean bDefault, boolean bReportMissing)
    {
        String sValue = getStringProperty(sKey, null, bReportMissing);
        if (sValue == null) return bDefault;

        Boolean bool = Utils.parseBoolean(sValue);
        if (bool == null)
        {
            if (testing) return false;
            String sMsg = "Error converting '" + sKey + "' value to boolean: " + getStringProperty(sKey);
            logger.error(sMsg);
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg, "Make sure value is a valid boolean");
        }
        return bool;
    }

    /**
     * Get a string property that should be there, throw
     * runtime error if not
     */
    public static String getRequiredStringProperty(String sKey)
    {
        String sValue = getStringProperty(sKey);
        if (sValue == null)
        {
            logger.error("Property value not found for: '" + sKey + '\'');
        }
        return sValue;
    }

    /**
     * Get property, return sDefault if not there
     */
    public static String getStringProperty(String sKey, String sDefault)
    {
        String s = getStringProperty(sKey);
        if (s == null) return sDefault;
        return s;
    }

    /**
     * Get property, return sDefault if not there
     */
    public static String getStringProperty(String sKey, String sDefault, boolean bReportMissing)
    {
        String s = getStringProperty(sKey, bReportMissing);
        if (s == null) return sDefault;
        return s;
    }

    /**
     * Get a string property
     */
    private static String getStringProperty(String sKey, boolean bReportMissing)
    {
        ApplicationError.assertNotNull(propConfig, "PropertyConfig has not been initialized");
        String sValue = propConfig.getProperty(sKey);
        if (sValue != null) sValue = sValue.trim(); // need to remove trailing spaces

        if (sValue == null && testing) return "TESTING-MISSING-" + sKey;

        if (sValue == null && bReportMissing)
        {
            if (!sKey.contains("default"))
            {
                logger.warn("Property value not found for: '" + sKey + '\'');
            }
        }

        return sValue;
    }

    public static String getStringProperty(String sKey)
    {
        return getStringProperty(sKey, true);
    }

    // cache formats
    private static final Map<String, MessageFormat> formats_ = new HashMap<String, MessageFormat>();

    /**
     * Get a message and insert the params into it (params replaced
     * where strings of the form {#} are found).
     */
    public static String getMessage(String sKey, Object... oParams)
    {
        String sMsg = null;
        String sFormatThis = getRequiredStringProperty(sKey);
        if (sFormatThis != null)
        {
            sMsg = sFormatThis;
            if (oParams != null && oParams.length > 0)
            {
                // cache long ones since they seem to take longer to parse
                if (oParams.length > 4)
                {
                    synchronized (formats_)
                    {
                        MessageFormat format = formats_.get(sKey);
                        if (format == null)
                        {
                            format = new MessageFormat(sFormatThis);
                            formats_.put(sKey, format);
                        }

                        sMsg = format.format(oParams);
                    }
                }
                else
                {
                    sMsg = MessageFormat.format(sFormatThis, oParams);
                }
            }
        }
        return sMsg;
    }

    /**
     * Get a message and insert the params into it (params replaced
     * where strings of the form {#} are found).
     */
    public static String getLocalizedMessage(String sKey, String sLocale, Object... oParams)
    {
        // localize key
        String keyLocalized = localize(sKey, sLocale);

        // get message
        String message = getMessage(keyLocalized, oParams);

        // if no message, see if default value is there (only if key was localized)
        if (message == null && !sKey.equals(keyLocalized))
        {
            message = getMessage(sKey, oParams);
        }
        return message;
    }

    /**
     * Return localized version of message key
     */
    public static String localize(String sKey, String sLocale)
    {
        if (sLocale == null) return sKey;
        return sLocale + '.' + sKey;
    }

    /**
     * get yes/no
     */
    public static String getYesNo(boolean bValue)
    {
        return getMessage(bValue ? "msg.yes" : "msg.no");
    }

    /**
     * Convienence method so you don't have to create object array
     */
    public static String formatMessage(String sKey, Object p0)
    {
        Object[] params = new Object[1];
        params[0] = p0;
        return formatMessage(sKey, params);
    }

    /**
     * Format message, inserting params where {n} appear
     */
    public static String formatMessage(String sMessage, Object[] oParams)
    {
        MessageFormat format = new MessageFormat(sMessage);
        return format.format(oParams);
    }

    // store locales
    private static final Map<String, SimpleDateFormat> dates_ = new HashMap<String, SimpleDateFormat>();
    private static final Map<String, Locale> locales_ = new HashMap<String, Locale>();

    /**
     * get date format (msg.format.datetime key)
     */
    public static SimpleDateFormat getDateFormat(String sLocale)
    {
        return getDateFormat("msg.format.datetime", sLocale);
    }

    /**
     * Get date format (given key)
     */
    public static SimpleDateFormat getDateFormat(String sMsgKey, String sLocale)
    {
        String sLookup = sMsgKey + sLocale;
        synchronized (dates_)
        {
            SimpleDateFormat formatter = dates_.get(sLookup);
            if (formatter == null)
            {
                String sKey = localize(sMsgKey, sLocale);
                formatter = new SimpleDateFormat(getMessage(sKey), getLocale(sLocale));
                dates_.put(sLookup, formatter);
            }
            return formatter;
        }

    }

    /**
     * Get java locale for string
     */
    public static Locale getLocale(String sLocale)
    {
        if (sLocale != null)
        {
            synchronized (locales_)
            {
                Locale locale = locales_.get(sLocale);
                if (locale == null)
                {
                    locale = new Locale(sLocale, "", "");
                    locales_.put(sLocale, locale);
                }
                return locale;
            }
        }
        else
        {
            return Locale.US;
        }
    }

    /**
     * Return number as a place "e.g., 1=1st, 2=2nd, etc.
     */
    public static String getPlace(int n)
    {
        return getPlace(n, n);
    }

    /**
     * return number as a place "e.g., 1=1st, 2=2nd".  This version
     * takes the pre-formatted display value of value
     *
     * @param valueAlreadyFormatted
     * @param value
     * @return
     */
    public static String getPlace(int value, Object valueAlreadyFormatted)
    {
        value %= 100;
        int mod = value % 10;

        int nCase;
        if (value > 3 && value < 20) nCase = 4;
        else if (mod == 1) nCase = 1;
        else if (mod == 2) nCase = 2;
        else if (mod == 3) nCase = 3;
        else nCase = 4;

        String sKey = null;
        switch (nCase)
        {
            case 1:
                sKey = "msg.1st";
                break;
            case 2:
                sKey = "msg.2nd";
                break;
            case 3:
                sKey = "msg.3rd";
                break;
            case 4:
                sKey = "msg.Xth";
                break;
        }

        return getMessage(sKey, valueAlreadyFormatted);
    }

    /**
     * Get amount for display - use K for large numbers
     */
    public static String getAmount(int n)
    {
        return getAmount(n, true, false);
    }

    /**
     * Get amount for display - use K for large numbers if bDoK is true,
     * use bDoM for 1,000,000+ if true
     */
    public static String getAmount(int n, boolean bDoK, boolean bDoM)
    {
        if (n == 0) return "";

        if (n < 10000)
        {
            return getMessage("msg.amount", n);
        }
        else if (n < 1000000)
        {
            if (bDoK)
            {
                int nodd = n % 1000;
                if (nodd > 0) nodd /= 100;
                n /= 1000;
                return getMessage(nodd > 0 ? "msg.amount.k.odd" : "msg.amount.k", n, nodd);
            }
        }
        else
        {
            if (bDoM)
            {
                int nodd = n % 1000000;
                if (nodd > 0) nodd /= 100000;
                n /= 1000000;

                return getMessage(nodd > 0 ? "msg.amount.m.odd" : "msg.amount.m", n, nodd);
            }
            else if (bDoK)
            {
                int nodd = n % 1000;
                if (nodd > 0) nodd /= 100;
                n /= 1000;
                return getMessage(nodd > 0 ? "msg.amount.k.odd" : "msg.amount.k", n, nodd);
            }
        }

        return getMessage("msg.amount", n);
    }

    /**
     * Get build number
     */
    public static String getBuildNumber()
    {
        return getStringProperty("build.number", "[dev]", false);
    }
}
