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

import java.sql.*;

/**
 * Represents a database column.
 */
public class DatabaseColumn
{
    private String name_ = null;
    private int type_ = Types.VARCHAR;

    private boolean isSequence_ = false;
    private boolean isCreateDate_ = false;
    private boolean isModifyDate_ = false;
    private boolean isDataMarshal_ = false;

    /**
     * Create a column with the given name and type.
     *
     * @param name column name
     * @param type column type
     */
    public DatabaseColumn(String name, int type)
    {
        name_ = name;
        type_ = type;
    }

    /**
     * Get the column name.
     *
     * @return the column name
     */
    public String getName()
    {
        return name_;
    }

    /**
     * easier access to name for constructing SQL
     */
    public String toString()
    {
        return getName();
    }

    /**
     * Get the column type.
     *
     * @return the column type
     */
    public int getType()
    {
        return type_;
    }

    /**
     * Set the column type.  The given value must be one of the standard JDBC types.
     *
     * @param type column type
     *
     * @see Types
     */
    public void setType(int type)
    {
        type_ = type;

    }

    /**
     * Determine if the column represents an auto-incrementing sequence.
     *
     * @return <code>true</code> if a sequence, <code>false</code> otherwise
     */
    public boolean isSequence()
    {
        return isSequence_;
    }

    /**
     * Set whether or not column represents an auto-incrementing sequence.
     *
     * @param sequence sequence flag
     */
    public void setSequence(boolean sequence)
    {
        isSequence_ = sequence;
    }

    /**
     * Determine if the column represents an auto-assigned create date.
     *
     * @return <code>true</code> if a create date, <code>false</code> otherwise
     */
    public boolean isCreateDate()
    {
        return isCreateDate_;
    }

    /**
     * Set whether or not column represents an auto-assigned create date.
     *
     * @param createDate create date flag
     */
    public void setCreateDate(boolean createDate)
    {
        isCreateDate_ = createDate;
    }

    /**
     * Determine if the column represents an auto-assigned modify date.
     *
     * @return <code>true</code> if a modify date, <code>false</code> otherwise
     */
    public boolean isModifyDate()
    {
        return isModifyDate_;
    }

    /**
     * Set whether or not column represents an auto-assigned modify date.
     *
     * @param modifyDate modify date flag
     */
    public void setModifyDate(boolean modifyDate)
    {
        isModifyDate_ = modifyDate;
    }

    /**
     * Determine if the column represents a marshalled object.
     *
     * @return <code>true</code> if a marshalled object, <code>false</code> otherwise
     */
    public boolean isDataMarshal()
    {
        return isDataMarshal_;
    }

    /**
     * Set whether or not column represents a marshalled object.
     *
     * @param marshal marshalled flag
     */
    public void setDataMarshal(boolean marshal)
    {
        isDataMarshal_ = marshal;
    }
}
