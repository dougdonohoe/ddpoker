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
package com.donohoedigital.db;

/**
 * @author Doug Donohoe
 */
public class DBUtils
{
    /**
     * prepend any search string with this to disable wildcard
     */
    public static final String SQL_EXACT_MATCH = "=";

    /**
     * Escape % \ and _ values for use in a SQL like clause
     */
    public static String sqlEscapeWildcards(String original)
    {
        if (original == null) return null;

        StringBuilder sb = new StringBuilder(original.length() + 5);

        for(int i=0; i<original.length(); i++)
        {
            char c = original.charAt(i);

            if (c == '%' || c == '_' || c == '\\')
            {
                sb.append('\\');
            }
            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Return given term as a wildcarded for SQL (append % to front and end)
     */
    public static String sqlWildcard(String term)
    {
        if (term != null && term.startsWith(SQL_EXACT_MATCH))
        {
            return term.substring(SQL_EXACT_MATCH.length());
        }
        return term == null || term.length() == 0 ? "%" : '%' + sqlEscapeWildcards(term) + '%';
    }

    /**
     * return search term as wildcard exact match override
     */
    public static String sqlExactMatch(String term)
    {
        if (term == null) return SQL_EXACT_MATCH;

        return SQL_EXACT_MATCH + term;
    }
}
