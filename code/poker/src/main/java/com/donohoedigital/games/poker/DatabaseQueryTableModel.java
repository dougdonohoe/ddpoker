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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.db.*;

import java.sql.*;
import java.util.*;

public class DatabaseQueryTableModel extends DDPagingTableModel
{
    private Database database_;
    private String query_;
    private BindArray bindArray_;

    public boolean isCellEditable(int row, int column)
    {
        return false;
    }

    public int getTotalCount()
    {
        return getRowCount();
    }

    public void refresh(int offset, int rowCount)
    {
        Connection conn = database_.getConnection();

        try
        {
            PreparedStatement pstmt;

            pstmt = conn.prepareStatement(query_);

            int bindValueCount = (bindArray_ != null) ? bindArray_.size() : 0;

            for (int i = 0; i < bindValueCount; ++i)
            {
                pstmt.setObject(i + 1, bindArray_.getValue(i));
            }

            try
            {
                ResultSet rs = pstmt.executeQuery();

                try
                {
                    while (rs.next())
                    {
                        Vector v = new Vector();

                        for (int i = 1; i <= getColumnCount(); ++i)
                        {
                            v.add(rs.getObject(i));
                        }
                        addRow(v);
                    }
                } finally
                {
                    rs.close();
                }
            } finally
            {
                pstmt.close();
            }
        } catch (SQLException e)
        {
            throw new ApplicationError(e);
        } finally
        {
            try
            {
                conn.close();
            } catch (SQLException e)
            {
            }
        }
    }

    public DatabaseQueryTableModel(Database database, String[] colNames, String query, BindArray bindArray)
    {
        database_ = database;
        query_ = query;
        bindArray_ = bindArray;

        for (int i = 0; i < colNames.length; ++i)
        {
            addColumn(colNames[i]);
        }

       refresh(0, 0);
    }
}
