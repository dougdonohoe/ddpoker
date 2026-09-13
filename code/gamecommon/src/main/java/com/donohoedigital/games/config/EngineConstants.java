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
/*
 * EngineConstants.java
 *
 * Created on March 26, 2003, 2:24 PM
 */

package com.donohoedigital.games.config;

/**
 *
 * @author  donohoe
 */
public class EngineConstants
{
    // used when TESTING_CHANGE_STARTING_SIZE is on
    public static final int TESTING_CHANGE_SIZE_WIDTH = 800;//1000;
    public static final int TESTING_CHANGE_SIZE_HEIGHT = 600;//750;

    // debug settings configured in common.properties file
    public static final String TESTING_OVERRIDE_KEY = "settings.debug.override.key";
    public static final String TESTING_SKIP_DUP_KEY_CHECK = "settings.debug.skip.dup.key.check";
    public static final String TESTING_PERFORMANCE = "settings.debug.performance";
    public static final String TESTING_PROFILE_EDITABLE = "settings.debug.editprofile";
    public static final String TESTING_CHANGE_STARTING_SIZE = "settings.debug.changesize";
    public static final String TESTING_SERVLET = "settings.debug.servlet";
    public static final String TESTING_SKIP_EMAIL = "settings.debug.skipemail";
    public static final String TESTING_PROFILE_OVERRIDE_EMAIL = "settings.debug.profile.email.override";
    public static final String TESTING_PROFILE_OVERRIDE_EMAIL_TO = "settings.debug.profile.email.override.to";
    public static final String TESTING_DEBUG_REPAINT = "settings.debug.repaint";
    public static final String TESTING_DEBUG_REPAINT_DETAILS = "settings.debug.repaint.details";
    public static final String TESTING_AI_DEBUG = "settings.debug.debug.ai";
    public static final String TESTING_P2P = "settings.debug.p2p";
    public static final String TESTING_UDP_APP = "settings.debug.udp.app";
    public static final String TESTING_NO_EXTERNAL = "settings.debug.no.external";

    // game options node and defines
    public static final String PREF_WINDOW_MODE = "windowmode";
    
    // window mode options
    public static final int MODE_ASK = 1;
    public static final int MODE_WINDOW = 2;
    public static final int MODE_FULL = 3;
    
    public static final String PREF_FX = "fx";
    public static final String PREF_FX_VOL = "fxvol";
    public static final String PREF_MUSIC = "music";
    public static final String PREF_BGMUSIC = "bgmusic";
    public static final String PREF_MUSIC_VOL = "musicvol";
    public static final String PREF_BGMUSIC_VOL = "bgvol";
    
    // auto save
    public static final String PREF_AUTOSAVE = "autosave";
    
    // fill option
    public static final String PREF_FILL = "fill";
    public static final String PREF_SCROLL = "scroll";

    // size/location
    public static final String PREF_X = "x";
    public static final String PREF_Y = "y";
    public static final String PREF_W = "w";
    public static final String PREF_H = "h";
    public static final String PREF_MAXIMIZED = "maximized";

    // common options
    public static final String OPTION_ONLINE_ENABLED = "onlineenabled";
    public static final String OPTION_ONLINE_SERVER = "onlineserver";
}
