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
/*
 * Prefs.java
 *
 * Created on February 23, 2003, 2:04 PM
 */

package com.donohoedigital.config;


import com.donohoedigital.base.*;

import java.util.prefs.*;

/**
 * @author Doug Donohoe
 */
public class Prefs
{

    static
    {
        if (Utils.ISLINUX)
        {
            // need to change system root to user dir
            // because default is /etc
            String userRoot = System.getProperty("user.home");
            System.setProperty("java.util.prefs.systemRoot", userRoot + "/.java");
            // TODO: this is probably broken now
            System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        }
    }

    // store root node override
    private static String ROOT = null;
    public static final String NODE_OPTIONS = "options/";

    /**
     * Set root node name - overrides default of ConfigManager.getAppName();
     * only sets if ROOT is null.
     */
    public static void setRootNodeName(String s)
    {
        if (ROOT == null || s == null) ROOT = s;
    }

    /**
     * Get root node name for all prefs
     */
    private static String getRootNodeName()
    {
        if (ROOT != null) return ROOT;

        // probably not needed anymore since we don't run multiple
        // apps in an appserver.  commenting out to avoid dependency
        // on ConfigManager so we can use this in the installer easier
        //ApplicationError.assertNotNull(ConfigManager.getAppConfig(), "AppConfig is null");
        //return ConfigManager.getAppName();
        return "generic";
    }

    /**
     * Get root user prefs.  Users "com/donohoedigital/" + appname
     */
    public static Preferences getUserRootPrefs()
    {
        return Preferences.userRoot().node("com/donohoedigital/" + getRootNodeName());
    }

    /**
     * Clear all prefs
     */
    public static void clearAll()
    {
        try
        {
            Preferences prefs = getUserRootPrefs();
            prefs.clear();
            prefs.removeNode();
        }
        catch (BackingStoreException bse)
        {
            // no worries
        }
    }

    /**
     * Get given node under user root prefs.  Nodes are delimited by /
     */
    public static Preferences getUserPrefs(String sNodeName)
    {
        return getUserRootPrefs().node(sNodeName);
    }
}
