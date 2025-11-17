/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * EnginePrefs.java
 *
 * Created on February 23, 2003, 3:15 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.gui.DDOption;

import java.util.prefs.Preferences;

/**
 *
 * @author  Doug Donohoe
 */
public class EnginePrefs {

    // Used to track "don't show again" for various info dialogs
    public static final String NODE_DIALOG_PHASE = "dialog-phase";

    private final Preferences prefs_;

    /**
     * Wrapper around Preferences, adding getters for options, with support for
     * fetching default value from .properties file (option.[name].default).
     */
    public EnginePrefs(Preferences prefs) {
        this.prefs_ = prefs;
    }

    public Preferences getPrefs() {
        return prefs_;
    }

    /**
     * For getting a boolean option that has default in client.properties
     */
    public boolean getBooleanOption(String prefName) {
        return prefs_.getBoolean(prefName,
                PropertyConfig.getRequiredBooleanProperty(DDOption.GetDefaultKey(prefName)));
    }

    /**
     * For getting a string option that has default in client.properties
     */
    public String getStringOption(String prefName) {
        return prefs_.get(prefName,
                PropertyConfig.getRequiredStringProperty(DDOption.GetDefaultKey(prefName)));
    }

    /**
     * For getting an int option that has default in client.properties
     */
    public int getIntOption(String prefName) {
        return prefs_.getInt(prefName,
                PropertyConfig.getRequiredIntegerProperty(DDOption.GetDefaultKey(prefName)));
    }

    public String get(String key, String def) {
        return prefs_.get(key, def);
    }

    public int getInt(String key, int def) {
        return prefs_.getInt(key, def);
    }

    public boolean getBoolean(String key, boolean def) { return prefs_.getBoolean(key, def); }

    public void put(String key, String value) {
        prefs_.put(key, value);
    }

    public void putInt(String key, int value) {
        prefs_.putInt(key, value);
    }

    public void putBoolean(String key, boolean value) {
        prefs_.putBoolean(key, value);
    }

    public void remove(String key) {
        prefs_.remove(key);
    }
}
