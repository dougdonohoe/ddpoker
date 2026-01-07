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
package com.donohoedigital.wicket.models;

import org.apache.wicket.Component;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 28, 2008
 * Time: 9:18:29 AM
 *
 * Inspired by BoundCompoundPropertyModel, but aliases an id to an expression instead
 * of a component.  Easier to use since you don't need to have a component
 * created when you make the alias.
 */
public class AliasedCompoundPropertyModel<T> extends CompoundPropertyModel<T>
{
    private static final long serialVersionUID = 42L;

    /**
	 * Internal alias representation.
     */
	private class Alias implements IClusterable
	{
		private static final long serialVersionUID = 1L;

		private final String id;
		private final String alias;

		private Alias(final String id, final String alias)
		{
			this.id = id;
			this.alias = alias;
		}

		/**
		 * @see Object#toString()
		 */
		@Override
        public String toString()
		{
            StringBuilder sb = new StringBuilder("Alias(");
			sb.append(":id=[").append(id).append(']');
			sb.append(":alias=[").append(alias).append(']');
			sb.append(')');
			return sb.toString();
		}
    }

	/**
	 * List of Aliases. Although a Map would be a more natural implementation here, a List is much
	 * more compact in terms of space. Although it may take longer to find a component alias in
	 * theory, in practice it's unlikely that any AliasedCompoundPropertyModel will really have enough
	 * aliases to matter.
	 */
	@SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private final ArrayList<Alias> aliases = new ArrayList<Alias>(1);


	@SuppressWarnings("unchecked")
	public AliasedCompoundPropertyModel(final Object model)
	{
		super((IModel<T>) model);
	}

	/**
	 * Adds a property alias.
	 *
	 * @param id
	 *            The id to bind
	 * @param alias
	 *            The alias (a property expression pointing to the property in this model)
	 * @return The component, for convenience in adding components
	 */
	public void alias(final String id, final String alias)
	{
		if (id == null)
		{
			throw new IllegalArgumentException("component must be not null");
		}
		if (alias == null)
		{
			throw new IllegalArgumentException("alias must be not null");
		}

		aliases.add(new Alias(id, alias));
	}

	/**
	 * @see CompoundPropertyModel#detach()
	 */
	@Override
    public void detach()
	{
		super.detach();

		// Minimize the size of the aliases list
		aliases.trimToSize();
	}

	/**
	 * @see Object#toString()
	 */
	@Override
    public String toString()
	{
        StringBuilder sb = new StringBuilder(super.toString());
		sb.append(":aliases=[");
		for (int i = 0, size = aliases.size(); i < size; i++)
		{
			if (i > 0)
			{
				sb.append(',');
			}
			sb.append(aliases.get(i));
		}
		sb.append(']');
		return sb.toString();
	}

	/**
	 * @param component
	 *            Component to get alias for
	 * @return The alias information
	 */
	private Alias getAlias(final Component component)
	{
        for (Alias alias : aliases)
        {
            if (component.getId().equals(alias.id))
            {
                return alias;
            }
        }
		return null;
	}

	/**
	 * @see CompoundPropertyModel#propertyExpression(Component)
	 */
	@Override
    protected String propertyExpression(final Component component)
	{
		final Alias alias = getAlias(component);
		if (alias != null)
		{
			return alias.alias;
		}
		else if (component != null)
		{
			return component.getId();
		}
		return null;
	}
}