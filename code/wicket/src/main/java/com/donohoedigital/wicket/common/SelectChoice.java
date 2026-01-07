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
package com.donohoedigital.wicket.common;

import java.io.*;

/**
 * Based class for representing a select list choice.  Represents a user-defined type T and its display value.
 *
 * Can be sorted via Collections.sort if T implements Comparable.
 *
 * @param <T> class of key
 *
 * @author Doug Donohoe
 */
public abstract class SelectChoice<T extends Serializable> implements Serializable, Comparable<SelectChoice<T>>
{
    private static final long serialVersionUID = 42L;

    private T key;
    private String display;

    public SelectChoice(T key, String display)
    {
        this.key = key;
        this.display = display;
    }

    /**
     * @return display value
     */
    public String getDisplay()
    {
        return display;
    }

    /**
     * Set the display value
     * @param display value to set
     */
    public void setDisplay(String display)
    {
        this.display = display;
    }

    /**
     * @return key value
     */
    public T getKey()
    {
        return key;
    }

    /**
     * Set the key value
     * @param key value to set
     */
    public void setKey(T key)
    {
        this.key = key;
    }

    /**
     * @return return String representation of the key
     */
    public String getKeyAsString()
    {
        return key.toString();
    }

    /**
     * Return String version of key so {@link org.apache.wicket.Component#getDefaultModelObjectAsString()} returns
     * a sensible value.  Useful if storing the value in {@link org.apache.wicket.PageParameters}.
     *
     * @return {@link #getKeyAsString()}
     */
    @Override
    public String toString()
    {
        return getKeyAsString();
    }

    /**
     * Implementation of Comparable.
     *
     * @param o the item to compare
     * @return this.key.compareTo(o.key)
     * @throws UnsupportedOperationException if the underlying key class does not implement Comparable
     */
    @SuppressWarnings({"unchecked"})
    public int compareTo(SelectChoice<T> o)
    {
        if (!(key instanceof Comparable))
        {
            throw new UnsupportedOperationException("Cannot compare non-comparable object: "+ key.getClass().getName());
        }

        Comparable<T> comparableKey = (Comparable<T>) key;

        return comparableKey.compareTo(o.key);        
    }
}
