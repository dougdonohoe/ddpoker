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
package com.donohoedigital.wicket.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

/**
 * @author Doug Donohoe
 */
public abstract class AbstractOnClickBehavior extends AbstractPrototypeBehavior
{
    private static final long serialVersionUID = 42L;

    private boolean noop;

    /**
     * Construct.
     * @param noop  Indicates whether 'return false;' is appended to the javascript, indicating that clicking
     * the link doesn't do anything else.
     */
    public AbstractOnClickBehavior(boolean noop)
    {
        this.noop = noop;
    }

    /**
     * Adds javascript to onClick attribute of tag.  Appends 'return false;' to end of javascript
     * if noop was set to true
     */
    @Override
    public void onComponentTag(Component component, ComponentTag tag)
    {
        StringBuilder sb = getJavascript();
        if (noop) sb.append("return false;");
        tag.put("onClick", sb.toString());
    }
}
