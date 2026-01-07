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

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List of SelectChoice&lt;?&gt; items.
 *
 * @author Doug Donohoe
 */
public class SelectChoiceList<T extends SelectChoice<?>> extends ArrayList<T> implements IChoiceRenderer<T>
{
    private static final long serialVersionUID = 42L;

    /**
     * Create list with initial list of items.
     *
     * @param items initial items
     */
    @SafeVarargs
    public SelectChoiceList(T... items)
    {
        Collections.addAll(this, items);
    }

    @Override
    public T getObject(String id, IModel<? extends List<? extends T>> choices) {
        List<? extends T> _choices = choices.getObject();
        for (int index = 0; index < _choices.size(); index++)
        {
            // Get next choice
            final T choice = _choices.get(index);
            if (getIdValue(choice, index).equals(id))
            {
                return choice;
            }
        }
        return null;
    }

    /**
     * Add item to list if not there and then sort.  Returns pre-existing item (if there) or item passed in.
     *
     * @param item to add to list
     * @return item added
     */
    public T addSorted(T item)
    {
        for (T t : this)
        {
            if (t.getKey().equals(item.getKey())) return t;
        }

        add(item);
        Collections.sort(this);
        return item;
    }

    /**
     * Get the display value from the SelectChoice (what the user sees)
     *
     * @param object a SelectChoice object
     * @return object.getDisplay()
     */
    public Object getDisplayValue(T object)
    {
        return object.getDisplay();
    }

    /**
     * Get key value (what is returned from browser)
     *
     * @param object a SelectChoice object
     * @param index not used
     * @return object.getKeyAsString()
     */
    public String getIdValue(T object, int index)
    {
        return object.getKeyAsString();
    }
}