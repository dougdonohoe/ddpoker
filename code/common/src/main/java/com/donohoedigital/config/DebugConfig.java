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
package com.donohoedigital.config;

import org.apache.logging.log4j.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 3, 2008
 * Time: 8:06:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class DebugConfig
{
    private static Logger logger = LogManager.getLogger(DebugConfig.class);
    private static Boolean TESTING_ENABLED = null;
    private static final Map<String, Boolean> cache = new HashMap<String, Boolean>();

    /**
     * Return true if given debug testing property is on.
     */
    public static boolean TESTING(String s)
    {
        if (!isTestingOn()) return false;

        // take sync hit only if testing is on
        synchronized (cache)
        {
            Boolean on = cache.get(s);
            if (on == null)
            {
                on = getDebugProperty(s);
                cache.put(s, on);
            }
            return on;
        }
    }

    public static void TOGGLE(String s)
    {
        // take sync hit only if testing is on
        synchronized (cache)
        {
            Boolean on = cache.get(s);
            if (on == null)
            {
                on = getDebugProperty(s);
            }
            cache.put(s, !on);
        }
    }

    /**
     * Returns a debug property and debug-prints if it is turned on
     */
    private static Boolean getDebugProperty(String sName)
    {
        boolean b = PropertyConfig.getBooleanProperty(sName, false, false);
        if (b)
        {
            logger.debug("Debug setting " + sName + " is on.");
        }
        return b;
    }

    /**
     * Is "settings.debug.enabled" property on?
     */
    public static boolean isTestingOn()
    {
        if (TESTING_ENABLED == null)
        {
            if (!PropertyConfig.isInitialized())
            {
                logger.warn("Checking isTestingOn() before PropertyConfig initialized.");
                return false;
            }

            //noinspection NonThreadSafeLazyInitialization
            TESTING_ENABLED = PropertyConfig.getBooleanProperty("settings.debug.enabled", false, false);
            if (TESTING_ENABLED) logger.debug("Debug testing on.");
        }
        return TESTING_ENABLED;
    }
}
