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

import com.donohoedigital.base.*;
import static com.donohoedigital.config.ApplicationType.*;

import java.io.*;

public class ConfigManager
{
    private static ConfigManager configMgr = null;
    private static String appName = null;

    // config things we load/store
    private final String locale;
    private final String extraModule;
    private final String[] modules;
    private final RuntimeDirectory runtimeDir;

    /**
     * Load config files for given appname of specified type
     */
    public ConfigManager(String sAppName, ApplicationType type)
    {
        this(sAppName, type, null, null, true);
    }

    /**
     * Load config files for given appname of specified type
     */
    public ConfigManager(String sAppName, ApplicationType type, boolean allowOverrides)
    {
        this(sAppName, type, null, null, allowOverrides);
    }

    /**
     * Load config files using extra module and locale
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
    public ConfigManager(String sAppName, ApplicationType type, String sExtraModule, String sLocale, boolean allowOverrides)
    {
        ApplicationError.warnNotNull(configMgr, "ConfigManager already initialized");
        configMgr = this;
        appName = sAppName;
        locale = sLocale;
        extraModule = sExtraModule;
        runtimeDir = new DefaultRuntimeDirectory();

        // set root prefs node
        Prefs.setRootNodeName(sAppName);

        // modules to load
        modules = sExtraModule == null ? new String[]{"common", sAppName} :
                  new String[]{"common", sAppName, sExtraModule};

        // Load properties (needs to be available for data elements)
        new PropertyConfig(sAppName, modules, type, locale, allowOverrides);

        // these items only used on client
        if (type == CLIENT)
        {
            // Load data elements
            new DataElementConfig(sAppName, extraModule);

            // audio
            loadAudioConfig();

            // Load help info
            new HelpConfig(modules, locale);

            // gui stuff (images/styles)
            loadGuiConfig();
        }
    }

    public void loadAudioConfig()
    {
        // Load audio info
        new AudioConfig(modules);
    }

    /**
     * load display-related config files (images and styles)
     */
    public void loadGuiConfig()
    {
        // Load images
        new ImageConfig(modules);

        // Load colors
        new StylesConfig(modules);
    }

    /**
     * Get configuration directory for application
     */
    public String getLocale()
    {
        return locale;
    }

    /**
     * Get extra module
     */
    public String getExtraModule()
    {
        return extraModule;
    }

    /**
     * Get the app name used to create this config
     */
    public static String getAppName()
    {
        return appName;
    }

    /**
     * Get config manager currently applicable
     */
    public static ConfigManager getConfigManager()
    {
        return configMgr;
    }

    /**
     * Get config manager currently applicable
     */
    public static File getUserHome()
    {
        return configMgr.runtimeDir.getClientHome(appName);
    }

    /**
     * Get server home
     */
    public static File getServerHome()
    {
        return configMgr.runtimeDir.getServerHome();
    }
}
