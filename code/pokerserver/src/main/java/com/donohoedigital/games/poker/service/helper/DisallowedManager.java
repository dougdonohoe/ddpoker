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
package com.donohoedigital.games.poker.service.helper;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jan 4, 2009
 * Time: 10:37:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class DisallowedManager
{

    private static final String DISALLOWED_PATTERN_PREFIX = ":";
    private static final List<String> disallowedContains = new ArrayList<String>();
    private static final List<Pattern> disallowedPatterns = new ArrayList<Pattern>();

    public DisallowedManager()
    {
        // Load the list of invalid names.
        URL url = new MatchingResources("classpath*:config/poker/disallowed.txt").getSingleRequiredResourceURL();
        String contents = ConfigUtils.readURL(url);

        // Add offensive words.
        try
        {
            BufferedReader reader = new BufferedReader(new StringReader(contents));
            String line = null;
            Pattern pattern = null;

            while ((line = reader.readLine()) != null)
            {
                line = line.trim();

                if (line.startsWith("\\s*#")) continue; // comment
                line = line.replaceAll("\\s*#.*", ""); // trailing comment
                if (line.length() == 0) continue; // blank line

                if (line.startsWith(DISALLOWED_PATTERN_PREFIX))
                {
                    line = line.substring(1);
                    pattern = Pattern.compile(line, Pattern.CASE_INSENSITIVE);
                    disallowedPatterns.add(pattern);
                }
                else
                {
                    line = line.toLowerCase();
                    disallowedContains.add(line);
                }
            }
        }
        catch (IOException e)
        {
            throw new ApplicationError(e);
        }
    }

    public boolean isNameValid(String sName)
    {
        String name = sName.toLowerCase();

        // First check contains values.
        for (String disallowed : disallowedContains)
        {
            if (name.contains(disallowed))
            {
                return false;
            }
        }

        // Next check regex values.
        for (Pattern pattern : disallowedPatterns)
        {
            if (pattern.matcher(name).matches())
            {
                return false;
            }
        }

        return true;
    }
}
