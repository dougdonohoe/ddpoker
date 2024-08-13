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
package com.donohoedigital.wicket.common;

import com.donohoedigital.wicket.models.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.repeater.data.*;
import org.apache.wicket.model.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 27, 2008
 * Time: 10:36:32 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PageableServiceProvider<T> implements IDataProvider<T>
{
    //private static Logger logger = Logger.getLogger(PageableServiceProvider.class);

    private static final long serialVersionUID = 42L;

    private Integer size;

    /**
     * Fetch 'pagesize' rows starting at index 'first' and return as an Iterator
     */
    public abstract Iterator<T> iterator(int first, int pagesize);

    /**
     * Implementers should implement logic to fetch the total count of all
     * items.  This method is only called once - the value is cached
     * in the instance and in the session.
     */
    public abstract int calculateSize();

    /**
     * If the size has not been set via setSize(), it is determined by
     * calling calculateSize() and cached in the instance.
     *
     * @return total count of all items represented by this provider
     */
    public int size()
    {
        if (size == null)
        {
            size = calculateSize();
        }
        return size;
    }

    /**
     * reset size so it is fetched again
     */
    public void resetSize()
    {
        size = null;
    }

    /**
     * Set total size - used to set size from a previous use of
     * this provider (e.g., as stored as a PageParameter value in conjunction
     * with a BookmarkablePagingNavigator).  Useful to avoid repeated
     * calls to the database to fetch the count.
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Set size by retrieving 'name' from 'params'.  If a value isn't
     * present, then the size is calculated and saved in the 'params'.
     * This is used so consumers of this class can store the size
     * in the URL and skip calculating the size.  Useful in conjunction
     * with BookmarkablePagingNavigator.
     */
    public void processSizeFromParams(PageParameters params, String name)
    {
        Integer n = params.getAsInteger(name);
        if (n == null)
        {
            n = size();
            params.put(name, n);
        }
        else
        {
            setSize(n);
        }
    }

    /**
     * @return true if size() == 0
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * Returns object (something fetched from the iterator()) in a EntityModel wrapped
     * by a CompoundPropertyModel
     */
    public IModel<T> model(T object)
    {
        return new CompoundPropertyModel<T>(new EntityModel<T>(object));
    }

    /**
     * Do any necessary cleanup
     */
    public void detach()
    {
    }
}