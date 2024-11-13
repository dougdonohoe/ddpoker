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
package com.donohoedigital.games.server;

import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by IntelliJ IDEA.
* User: donohoe
* Date: Mar 21, 2008
* Time: 12:47:36 PM
* To change this template use File | Settings | File Templates.
*/
public final class RegistrationFileStringStyle extends ToStringStyle
{
    public static final ToStringStyle STYLE = new RegistrationFileStringStyle();

    private RegistrationFileStringStyle() {
        super();
        this.setUseShortClassName(true);
        this.setUseIdentityHashCode(false);
    }

    /**
     * Override so we can escape the values
     */
    protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
        buffer.append(escape(value.toString()));
    }

    /**
     * escape commas with \
     */
    private String escape(String sValue)
    {
        if (sValue == null) return null;

        final char ESCAPE = '\\';
        final char DELIM = ',';

        char c;
        int length = sValue.length();
        // lazily create StringBuilder only if we are escaping something
        StringBuilder sbEscape = null;
        for (int i = 0; i < length; i++)
        {
            c = sValue.charAt(i);
            switch(c)
            {
                case ESCAPE:
                case DELIM:
                    if (sbEscape == null)
                    {
                        sbEscape = new StringBuilder(length+1);
                        sbEscape.append(sValue.substring(0, i));
                    }
                    sbEscape.append(ESCAPE);
                    break;

                default:
                    break;
            }
            if (sbEscape != null) sbEscape.append(c);
        }
        return sbEscape == null ? sValue : sbEscape.toString();
    }
}
