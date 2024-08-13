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

import java.sql.*;
import java.text.*;

/**
 * Represents a logical database.
 *
 * @author zak
 * @see DatabaseManager
 */
public class Database
{
    private String name_ = null;
    private String driverClassName_ = null;
    private String driverConnectURL_ = null;
    private String driverFormattedURL_ = null;
    private String username_ = null;
    private String password_ = null;

    /**
     * Create an uninitialized database.
     *
     * @param name database name
     */
    public Database(String name)
    {
        name_ = name;
    }

    /**
     * Get the database name.
     *
     * @return the database name
     */
    public String getName()
    {
        return name_;
    }


    /**
     * Get the driver class name.
     *
     * @return the driver class name
     */
    public String getDriverClassName()
    {
        return driverClassName_;
    }

    /**
     * Set the driver class name.
     *
     * @param driverClassName driver class name
     */
    public void setDriverClassName(String driverClassName)
    {
        driverClassName_ = driverClassName;
    }

    /**
     * Get the driver URL used to create database connections.
     *
     * @return the driver connect URL
     */
    public String getDriverConnectURL()
    {
        return driverConnectURL_;
    }


    /**
     * Set the driver URL used to create database connections.
     *
     * @param driverConnectURL driver connect URL
     */
    public void setDriverConnectURL(String driverConnectURL)
    {
        driverConnectURL_ = driverConnectURL;
    }

    /**
     * Get the username used to make connections.
     *
     * @return the username
     */
    public String getUsername()
    {
        return username_;
    }

    /**
     * Set the username used to make connections.
     *
     * @param username username
     */
    public void setUsername(String username)
    {
        username_ = username;
    }

    /**
     * Get the password used to make connections.
     *
     * @return the password
     */
    public String getPassword()
    {
        return password_;
    }

    /**
     * Set the password used to make connections.
     *
     * @param password password
     */
    public void setPassword(String password)
    {
        password_ = password;
    }

    /**
     * Get a connection to the database.  Implementation is driver/data source specific.
     *
     * @return the database connection
     * @throws ApplicationError if an error occurs retrieving the connection
     */
    public Connection getConnection() throws ApplicationError
    {
        Connection conn = null;

        try
        {
            String connectURL = driverFormattedURL_;
            String username = getUsername();
            String password = getPassword();

            if ((username != null) && (password != null))
            {
                conn = DriverManager.getConnection(connectURL);
            }
            else
            {
                conn = DriverManager.getConnection(connectURL, username, password);
            }
        }
        catch (SQLException e)
        {
            throw new ApplicationError(e);
        }
        catch (Throwable e)
        {
            throw new ApplicationError(e);
        }

        return conn;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("name=");
        buffer.append(getName());
        //buffer.append(", driver=");
        //buffer.append(getDriverClassName());
        buffer.append(", url=");
        buffer.append(getDriverConnectURL());

        return buffer.toString();
    }

    /**
     * Execute database-specific startup logic.
     */
    protected void startup()
    {
    }

    /**
     * Initialize according to the current database settings.
     */
    void init()
    {
        // Force the driver to load.
        try
        {
            Class.forName(getDriverClassName()).newInstance();
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        // Format the connect URL.
        Object[] params = new Object[3];
        params[0] = getName();
        params[1] = getUsername();
        params[2] = getPassword();

        MessageFormat format = new MessageFormat(getDriverConnectURL());
        driverFormattedURL_ = format.format(params);
    }
}
