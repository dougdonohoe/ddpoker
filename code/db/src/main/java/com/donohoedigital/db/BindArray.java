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

import java.util.*;

/**
 * Contains a list of bind values.
 *
 * @see DatabaseQuery
 */
public class BindArray
{
    private ArrayList values_ = null;

    /**
     * Create an empty bind array.
     */
    public BindArray()
    {
        values_ = new ArrayList();
    }

    /**
     * Create a bind array that will contain the given number of parameters.
     *
     * @param count parameter count
     */
    public BindArray(int count)
    {
        values_ = new ArrayList(count);
    }

    /**
     * Add a value.
     *
     * @param type type
     * @param value value
     */
    public void addValue(int type, Object value)
    {
        Value bindValue = new Value();
        bindValue.type_ = type;
        bindValue.value_ = value;

        values_.add(bindValue);
    }

    /**
     * Get the type at the given index.
     *
     * @param index index
     *
     * @return the index, or <code>-1</code> if the index is out of range
     */
    public int getType(int index)
    {
        int size = size();
        return ((size > 0) && (index < size)) ? ((Value) values_.get(index)).type_ : -1;
    }

    /**
     * Get the value at the given index.
     *
     * @param index index
     *
     * @return the value, or <code>null</code> if the index is out of range
     */
    public Object getValue(int index)
    {
        int size = size();
        return ((size > 0) && (index < size)) ? ((Value) values_.get(index)).value_ : null;
    }

    /**
     * Get the total number of bind values added.
     *
     * @return the array size
     */
    public int size()
    {
        return values_.size();
    }
    
    /**
     * Type/value pair.
     */
    private static class Value
    {
        public int type_;
        public Object value_;
    }

    public String toString()
    {
        int valueCount = values_.size();
        StringBuilder buffer = new StringBuilder();
        Value value = null;

        for (int i = 0; i < valueCount; ++i)
        {
            value = (Value) values_.get(i);
            buffer.append(value.value_);
            buffer.append(", ");
        }

        buffer.setLength(buffer.length() - 2);

        return buffer.toString();
    }
}
