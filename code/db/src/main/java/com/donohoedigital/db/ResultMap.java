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
package com.donohoedigital.db;

import com.donohoedigital.comms.*;

import java.sql.*;

/**
 * Contains query results whose values are set according to the current result row.
 */
public class ResultMap extends DMTypedHashMap
{
    private DatabaseQuery query_ = null;
    private ResultSet rs_ = null;

    private int index_ = 0;
    private int count_ = -1;

    /**
     * Retrieve the next row and store the results.
     *
     * @return <code>true</code> if there was a new result row, <code>false</code> otherwise
     */
    public boolean next() throws SQLException
    {
        if (size() > 0)
        {
            clear();
        }

        return (query_.getResultMap(rs_, this) != null) ? true : false;

        // This method assumes paging is built into the SQL query (e.g., the LIMIT clause on MySQL).
        // Uncomment the block below to perform paging on a full result set.
        /*
        return ((index_++ < count_) && (query_.getResultMap(rs_, this) != null)) ? true : false;
        */
    }

    /**
     * Close the result set.
     */
    public void close()
    {
        query_.close();
    }
}

