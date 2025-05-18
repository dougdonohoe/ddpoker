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
package com.donohoedigital.db;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Performs database queries using the given metadata.
 */
@SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed"})
public class DatabaseQuery
{
    private static Logger logger = LogManager.getLogger(DatabaseQuery.class);

    private Database database_ = null;
    private Connection conn_ = null;
    private String tableName_ = null;
    private List<String> joinTables_ = null;
    private boolean isDistinct_ = false;
    private TypedHashMap hmColumns_ = null;
    private String whereClause_ = null;
    private String suppClause_ = null;
    private List<Object> bindValues_ = null;
    private int offset_ = 0;
    private int count_ = -1;

    private PreparedStatement pstmt_ = null;

    /**
     * Create an object that represents a query to the given database.  This method retrieves
     * a connection to the database and will throw an error if the connection fails.
     *
     * @param database  database
     * @param tableName tableName table name
     */
    public DatabaseQuery(Database database, String tableName)
    {
        database_ = database;
        conn_ = database.getConnection();
        tableName_ = tableName;
    }

    /**
     * Create an object that represents a query to the given table.  This method allows the
     * database connection to be used for other queries outside this object.  It is therefore
     * the responsibility of the caller to close the connection.
     *
     * @param conn      database connection
     * @param tableName tableName table name
     */
    public DatabaseQuery(Connection conn, String tableName)
    {
        conn_ = conn;
        tableName_ = tableName;
    }

    /**
     * Initializes the query.  Can be called after setting parameters to &quot;reset&quot; the object.
     */
    public void init()
    {
        if (database_ != null)
        {
            conn_ = database_.getConnection();
        }

        hmColumns_ = null;
        whereClause_ = null;
        suppClause_ = null;
        bindValues_ = null;
        offset_ = 0;
        count_ = -1;
    }

    /**
     * Set whether or not the select statement should use the distinct keyword.
     *
     * @param distinct distinct flag
     */
    public void setDistinct(boolean distinct)
    {
        isDistinct_ = distinct;
    }

    /**
     * Add a database column to be used in the query.  See the various query methods for more information.
     * Ignored if column names are set.
     *
     * @param prop   data map property name
     * @param column database column
     */
    public void addColumn(String prop, DatabaseColumn column)
    {
        if (hmColumns_ == null)
        {
            hmColumns_ = new TypedHashMap();
        }

        hmColumns_.put(prop, column);
    }

    /**
     * Set the database columns to be used in the query.  See the various query methods for more information.
     * Ignored if column names are set.
     *
     * @param hmColumns properties and columns
     */
    public void setColumns(TypedHashMap hmColumns)
    {
        hmColumns_ = (TypedHashMap) hmColumns.clone();
    }

    /**
     * Add a table that will be joined in the query.
     *
     * @param tableName table name
     */
    public void addJoinTable(String tableName)
    {
        if (joinTables_ == null)
        {
            // Assume common case of one table.
            joinTables_ = new ArrayList<String>(1);
        }

        joinTables_.add(tableName);
    }

    /**
     * Set tables that will be joined in the query.
     *
     * @param tableNames table names
     */
    public void setJoinTables(String[] tableNames)
    {
        joinTables_ = Arrays.asList(tableNames);
    }

    /**
     * Set a where clause (without the <code>WHERE</code> keyword).
     *
     * @param whereClause where clause
     */
    public void setWhereClause(String whereClause)
    {
        whereClause_ = whereClause;
    }

    /**
     * Set a supplemental clause (e.g., <code>ORDER BY</code>).
     *
     * @param suppClause supplemental clause
     */
    public void setSuppClause(String suppClause)
    {
        suppClause_ = suppClause;
    }

    /**
     * Add values that are to be bound to any query parameters.  The order in which they are added
     * should match the order of the original parameter definitions.
     *
     * @param type  SQL data type
     * @param value value to be bound to the parameter
     */
    public void addBindValue(int type, Object value)
    {
        // Set bind value according to its SQL type.
        Object bindValue = null;

        if (value != null)
        {
            switch (type)
            {
                case Types.BINARY:
                case Types.BLOB:
                case Types.LONGVARBINARY:
                {
                    // Query binary data using a byte array.  In theory this will never, ever be used.
                    byte[] bytes = (byte[]) value;
                    bindValue = new ByteArrayInputStream(bytes);
                    break;
                }
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                {
                    // Query timestamp data using a long.
                    //noinspection ChainOfInstanceofChecks
                    if (value instanceof Calendar)
                    {
                        bindValue = new Timestamp(((Calendar) value).getTimeInMillis());
                    }
                    else if (value instanceof Long)
                    {
                        bindValue = new Timestamp((Long) value);
                    }
                    else
                    {
                        throw new RuntimeException("Unknown bind type: " + value.getClass().getName() + " value=" + value);
                    }
                    break;
                }
                default:
                {
                    // Assumes correct mapping.
                    bindValue = value;
                    break;
                }
            }
        }

        if (bindValues_ == null)
        {
            bindValues_ = new ArrayList<Object>();
        }

        bindValues_.add(bindValue);
    }

    /**
     * Set the bind values.
     *
     * @param values bind values
     */
    public void setBindValues(BindArray values)
    {
        if (values == null)
        {
            return;
        }

        int size = values.size();

        for (int i = 0; i < size; ++i)
        {
            addBindValue(values.getType(i), values.getValue(i));
        }
    }

    /**
     * Insert a record into the database using the given map.  Names must match the property names given
     * when adding a column and alues must map to the associated JDBC type.  Column names that do not have
     * an associated value will be set to null.  This method cannot currently be used if column names were set.
     *
     * @param hmValues values to insert
     * @throws SQLException if a database error occurs
     */
    public void insert(TypedHashMap hmValues) throws SQLException
    {
        // Loop through the columns to create the insert statement.
        StringBuilder buffer = new StringBuilder();
        buffer.append("INSERT INTO ");
        buffer.append(tableName_);

        DatabaseColumn column = null;

        if ((hmColumns_ != null) && (hmColumns_.size() > 0))
        {
            // Add the column names.
            Iterator<Object> colIter = hmColumns_.values().iterator();

            buffer.append("(");

            while (colIter.hasNext())
            {
                column = (DatabaseColumn) colIter.next();

                buffer.append(column.getName());
                buffer.append(", ");
            }

            // Trim the last comma.
            buffer.setLength(buffer.length() - 2);
            buffer.append(") ");
        }
        else
        {
            throw new SQLException("Missing column information.");
        }

        // Add the parameters.
        Iterator<Object> valuesiter = hmColumns_.values().iterator();

        buffer.append("VALUES(");

        while (valuesiter.hasNext())
        {
            column = (DatabaseColumn) valuesiter.next();

            if (column.isCreateDate() || column.isModifyDate())
            {
                // MySQL: Create/modify dates are explicitly set.
                buffer.append("NOW(), ");
            }
            else
            {
                buffer.append("?, ");
            }
        }

        // Trim the last comma.
        buffer.setLength(buffer.length() - 2);
        buffer.append(")");

        // Execute the query using the given values.
        try
        {
            String sql = buffer.toString();
            pstmt_ = conn_.prepareStatement(sql);

            int columnIndex = 1;
            String prop = null;
            Object value = null;

            for (Map.Entry<String, Object> entry : hmColumns_.entrySet())
            {
                prop = entry.getKey();
                column = (DatabaseColumn) entry.getValue();

                if (column.isCreateDate() || column.isModifyDate())
                {
                    // MySQL: Create dates, and modify dates were previously set.
                    continue;
                }
                else if (column.isSequence())
                {
                    // MySQL: Sequences are automatically inserted.
                    value = null;
                }
                else
                {
                    value = hmValues.get(prop);
                }

                setColumnValue(pstmt_, column, columnIndex, value);
                columnIndex++;
            }

            pstmt_.executeUpdate();
        }
        finally
        {
            close();
        }
    }

    /**
     * Update records in the database using the given map.  Names must match the property names given
     * when adding a column and values must map to the associated JDBC type.  Column names that do not have
     * an associated value will be set to null.
     *
     * @param hmValues values to insert
     * @return the number of records updated
     * @throws SQLException if a database error occurs
     */
    public int update(TypedHashMap hmValues) throws SQLException
    {
        // Loop through the columns to create the update statement.
        StringBuilder buffer = new StringBuilder();
        buffer.append("UPDATE ");
        buffer.append(tableName_);
        buffer.append(" SET ");

        if ((hmColumns_ != null) && (hmColumns_.size() > 0))
        {
            // Add the column names.

            for (Object o : hmColumns_.values())
            {
                DatabaseColumn column = (DatabaseColumn) o;

                // Skip sequence columns.
                if (!column.isSequence())
                {
                    if (column.isSequence() || column.isCreateDate())
                    {
                        // MySQL: Sequences and create dates are set during insert.
                    }
                    else
                    {
                        buffer.append(column.getName());

                        if (column.isModifyDate())
                        {
                            // MySQL: Modify dates are explicitly set.
                            buffer.append(" = NOW(), ");
                        }
                        else
                        {
                            buffer.append(" = ?, ");
                        }
                    }
                }
            }

            // Trim the last comma.
            buffer.setLength(buffer.length() - 2);
        }
        else
        {
            throw new SQLException("Missing column information.");
        }

        // Add the optional clauses.
        if (whereClause_ != null)
        {
            buffer.append(" WHERE ");
            buffer.append(whereClause_);
        }

        // Execute the query using the given values.
        try
        {
            String sql = buffer.toString();
            pstmt_ = conn_.prepareStatement(sql);

            int columnIndex = 1;
            String prop = null;
            Object value = null;

            for (Map.Entry<String, Object> entry : hmColumns_.entrySet())
            {
                prop = entry.getKey();
                DatabaseColumn column = (DatabaseColumn) entry.getValue();

                if (column.isSequence() || column.isCreateDate() || column.isModifyDate())
                {
                    // MySQL: Sequences, create dates, and modify dates were previously set.
                    continue;
                }
                else
                {
                    value = hmValues.get(prop);
                }

                setColumnValue(pstmt_, column, columnIndex, value);
                columnIndex++;
            }

            int bindValueCount = (bindValues_ != null) ? bindValues_.size() : 0;

            for (int i = 0; i < bindValueCount; ++i)
            {
                pstmt_.setObject(columnIndex, bindValues_.get(i));
                columnIndex++;
            }

            return pstmt_.executeUpdate();
        }
        finally
        {
            close();
        }
    }

    /**
     * Delete records from the database.
     *
     * @return the number of records deleted
     * @throws SQLException if a database error occurs
     */
    public int delete() throws SQLException
    {
        // Use the where map and where clause information to create the delete statement.
        StringBuilder buffer = new StringBuilder();
        buffer.append("DELETE FROM ");
        buffer.append(tableName_);

        // Add the optional clauses.
        if (whereClause_ != null)
        {
            buffer.append(" WHERE ");
            buffer.append(whereClause_);
        }

        // Execute the query using any given bind values.
        try
        {
            // Execute the query using any given bind values.
            String sql = buffer.toString();
            pstmt_ = conn_.prepareStatement(sql);

            int bindValueCount = (bindValues_ != null) ? bindValues_.size() : 0;

            for (int i = 0; i < bindValueCount; ++i)
            {
                pstmt_.setObject(i + 1, bindValues_.get(i));
            }

            return pstmt_.executeUpdate();
        }
        finally
        {
            close();
        }
    }

    /**
     * execute a given update query
     */
    public int executeUpdate(String sql) throws SQLException
    {
        pstmt_ = conn_.prepareStatement(sql);
        return pstmt_.executeUpdate();
    }

    /**
     * Retrieve the data from the given result set and store in a map.
     *
     * @param rs result set
     * @return the result map, or <code>null</code> if the result set does not contain any more data
     * @throws SQLException if a database exception occurs
     */
    DMTypedHashMap getResultMap(ResultSet rs, DMTypedHashMap hmResultMap) throws SQLException
    {
        // Retrieve the values and set them into the result map.
        if (!rs.next())
        {
            return null;
        }

        if (hmResultMap == null)
        {
            hmResultMap = new DMTypedHashMap();
        }

        ResultSetMetaData rsMetaData = rs.getMetaData();
        DatabaseColumn column = null;
        int columnCount = rsMetaData.getColumnCount();
        String prop = null;
        Object value = null;

        for (int i = 1; i <= columnCount; ++i)
        {
            prop = rsMetaData.getColumnLabel(i);
            column = (DatabaseColumn) hmColumns_.get(prop);
            value = getColumnValue(rs, column, i);

            if (value != null)
            {
                hmResultMap.put(prop, value);
            }
        }

        return hmResultMap;
    }

    /**
     * Clean up query resources.
     */
    void close()
    {
        // Just capture any exceptions so that they don't hide exceptions thrown
        // in the query logic.
        try
        {
            if (pstmt_ != null) pstmt_.close();
        }
        catch (SQLException e)
        {
            logger.warn("Exception on close: " + Utils.formatExceptionText(e));
        }
        finally
        {
            pstmt_ = null;
        }

        if (database_ != null)
        {
            try
            {
                if (conn_ != null) conn_.close();
            }
            catch (SQLException e)
            {
                logger.warn("Exception on close: " + Utils.formatExceptionText(e));
            }
        }

    }

    /**
     * Get a column value.
     *
     * @param rs     result set
     * @param column database column
     * @param i      column index
     * @return the column value
     * @throws SQLException if a database exception occurs
     */
    private Object getColumnValue(ResultSet rs, DatabaseColumn column, int i) throws SQLException
    {
        // Get "native" value according to its SQL type.
        Object value = null;

        switch (column.getType())
        {
            case Types.BINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
            {
                // Store binary data as a byte array.
                InputStream is = rs.getBinaryStream(i);

                if (is != null)
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] bytes = new byte[512];
                    int byteCount = 0;

                    try
                    {
                        while ((byteCount = is.read(bytes)) >= 0)
                        {
                            bos.write(bytes, 0, byteCount);
                        }
                    }
                    catch (IOException e)
                    {
                        SQLException se = new SQLException();
                        se.initCause(e);
                        throw se;
                    }

                    value = bos.toByteArray();
                }

                break;
            }
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            {
                // Store timestamp data as a long.
                Timestamp timestamp = rs.getTimestamp(i);

                if (timestamp != null)
                {
                    value = timestamp.getTime();
                }

                break;
            }
            case Types.INTEGER:
            {
                // Usually returned as a "long" by getObject(), so explicitly retrieve an integer.
                Number number = (Number) rs.getObject(i);

                if (number != null)
                {
                    value = number.intValue();
                }

                break;
            }
            default:
            {
                if (column.isDataMarshal())
                {

                    // Demarshal from a string.
                    String string = rs.getString(i);

                    if (string != null)
                    {
                        value = DataMarshaller.demarshal(rs.getString(i));
                    }
                }
                else
                {
                    // Assumes correct mapping.
                    value = rs.getObject(i);
                }

                break;
            }
        }

        return value;
    }

    /**
     * Set a column value.
     *
     * @param pstmt  prepared statement
     * @param column database column
     * @param i      column index
     * @param value  object value
     * @throws SQLException if a database exception occurs
     */
    private void setColumnValue(PreparedStatement pstmt, DatabaseColumn column, int i, Object value) throws SQLException
    {
        // Set "native" value according to its SQL type.
        if (value == null)
        {
            if (column.getType() == Types.BOOLEAN)
            {
                pstmt.setBoolean(i, false);
            }
            else
            {
                pstmt.setNull(i, column.getType());
            }

            return;
        }

        switch (column.getType())
        {
            case Types.BINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
            {
                // Store binary data as a byte array.
                byte[] bytes = (byte[]) value;
                InputStream is = new ByteArrayInputStream((byte[]) value);
                pstmt.setBinaryStream(i, is, bytes.length);

                break;
            }
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            {
                // Store timestamp data as a long.
                Timestamp timestamp = new Timestamp((Long) value);
                pstmt.setTimestamp(i, timestamp);

                break;
            }
            case Types.INTEGER:
            {
                pstmt.setInt(i, ((Number) value).intValue());

                break;
            }
            default:
            {
                if (column.isDataMarshal())
                {
                    // Marshal into a string.
                    String stringValue = DataMarshaller.marshal((DataMarshal) value);
                    pstmt.setString(i, stringValue);
                }
                else
                {
                    // Assumes correct mapping.
                    pstmt.setObject(i, value);
                }

                break;
            }
        }
    }
}
