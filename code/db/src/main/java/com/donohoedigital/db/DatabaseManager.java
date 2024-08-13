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
package com.donohoedigital.db;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import org.apache.log4j.*;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * Provide metadata driven database access.  Associated parameters are stored in <code>server.properties</code>.
 *
 * @author zak
 */
public class DatabaseManager
{
    private static Logger logger = Logger.getLogger(DatabaseManager.class);

    private static final String PROPERTY_PREFIX = "settings.database.";

    public static final String PARAM_INIT = "init";
    public static final String PARAM_KEY = "key";
    public static final String PARAM_DEFAULT = "default";

    public static final String PARAM_CLASS = "class";
    public static final String PARAM_DRIVER_CLASS = "driver";
    public static final String PARAM_DRIVER_URL = "url";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";

    private static boolean initialized_ = false;
    private static Map<String, Database> hmDatabases_ = new HashMap<String, Database>();

    /**
     * Determine if the manager has been initialized.
     *
     * @return <code>true</code> if database services are available, <code>false</code> otherwise
     */
    public static boolean isInitialized()
    {
        return initialized_;
    }

    /**
     * Retrieve an interface for the given database.
     *
     * @param name database name as defined in the metadata
     *
     * @return the database, or <code>null</code> if the given database is not defined
     */
    public static Database getDatabase(String name)
    {
        return hmDatabases_.get(name);
    }

    /**
     * Add a logical database.
     *
     * @param name database name
     * @param htParams database parameters
     */
    public static void addDatabase(String name, Map<String, String> htParams)
    {
        // Mark initialized if a database is explicitly added at runtime.
        if (!initialized_)
        {
            initialized_ = true;
        }

        //logger.debug("Adding database: " + name);
        // Create database and set associated values.
        Database database = null;
        String databaseClassName = getParamValue(name, PARAM_CLASS, htParams);

        if (databaseClassName != null)
        {
            // Allow the database class to be overridden.  Useful for application-specific initialization, etc.
            try
            {
                Class<?> databaseClass = Class.forName(databaseClassName);
                Constructor<?> databaseConstructor = databaseClass.getConstructor(String.class);
                database = (Database) databaseConstructor.newInstance(name);
            }
            catch (Exception e)
            {
                throw new ApplicationError(e);
            }
        }
        else
        {
            // Use the default database class.
            database = new Database(name);
        }

        database.setDriverClassName(getParamValue(name, PARAM_DRIVER_CLASS, htParams));
        database.setDriverConnectURL(getParamValue(name, PARAM_DRIVER_URL, htParams));
        database.setUsername(getParamValue(name, PARAM_USERNAME, htParams));
        String password = getParamValue(name, PARAM_PASSWORD, htParams);

        database.setPassword(password);

        // Initialize the database.
        database.init();
        hmDatabases_.put(name, database);

        logger.info("Loaded database: " + database);
    }

    /**
     * Get a database initialization parameter value.
     *
     * @param databaseName database name
     * @param paramName parameter name
     * @param htParams parameter values
     *
     * @return the parameter value, or <code>null</code> if it could not be found
     */
    private static String getParamValue(String databaseName, String paramName, Map<String, String> htParams)
    {
        // First look up the value using the database name (as in the property configuration).
        String databaseParamName = PROPERTY_PREFIX + databaseName + "." + paramName;
        String paramValue = htParams.get(databaseParamName);

        if (paramValue == null)
        {
            // Next try using the given parameter name (as in runtime configuration).
            paramValue = htParams.get(paramName);
        }

        return paramValue;
    }

    /**
     * Execute the given query
     *
     * @param pstmt The statement
     * @return the results
     * @throws SQLException
     */
    public static ResultSet executeQuery(PreparedStatement pstmt) throws SQLException
    {
        if (TESTING("settings.debug.dbperf"))
        {
            long before = System.currentTimeMillis();
            try
            {
                return pstmt.executeQuery();
            }
            finally
            {
                long after = System.currentTimeMillis();

                //noinspection ThrowableInstanceNeverThrown
                StackTraceElement stack[] = new Throwable().getStackTrace();
                StringBuilder buf = new StringBuilder();

                String className;

                buf.append("Database call from line executed in ");
                buf.append((after - before) / 1000);
                buf.append(".");
                buf.append((after - before) % 1000);
                buf.append(" seconds,");

                for (StackTraceElement frame : stack)
                {
                    className = frame.getClassName();
                    if (className.equals(DatabaseManager.class.getName())) continue;
                    if (className.startsWith("com.donohoedigital.gui.")) break;
                    if (!className.startsWith("com.donohoedigital.") &&
                        !className.startsWith("com.ddpoker.")) break;
                    buf.append("\n    at ");
                    buf.append(className);
                    buf.append(".");
                    buf.append(frame.getMethodName());
                    buf.append("(");
                    buf.append(frame.getFileName());
                    buf.append(":");
                    buf.append(frame.getLineNumber());
                    buf.append(")");
                }

                logger.info(buf.toString());
            }
        }
        else
        {
            return pstmt.executeQuery();
        }
    }
}
