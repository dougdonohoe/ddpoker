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
 * RegMessage.java
 *
 * Created on September 28, 2004, 9:24 AM
 */

package com.donohoedigital.games.comms;

import com.donohoedigital.base.*;

/**
 * Convienience class to represent registration information.
 *
 * @author donohoe
 */
public class RegistrationMessage extends EngineMessage
{
    public static final String PARAM_JAVA_VERSION = "regjava";
    public static final String PARAM_OS = "regos";
    public static final String PARAM_PATCH = "regpatch"; // for client/backward compatibility
    public static final String PARAM_NAME = "regname";
    public static final String PARAM_REG_EMAIL = "regemail";
    public static final String PARAM_ADDRESS = "regaddress";
    public static final String PARAM_CITY = "regcity";
    public static final String PARAM_STATE = "regstate";
    public static final String PARAM_POSTAL = "regpostal";
    public static final String PARAM_COUNTRY = "regcountry";

    /**
     * Creates a new instance of RegMessage
     */
    public RegistrationMessage(int nCategory)
    {
        super(GAME_NOTDEFINED, PLAYER_NOTDEFINED, nCategory);
        setOS();
        setJava();
    }

    /**
     * create a new instance from given message
     */
    public RegistrationMessage(EngineMessage msg)
    {
        msg.copyTo(this);
    }

    /**
     * Is activation?  Yes, if no email sent down.
     */
    public boolean isActivation()
    {
        return getEmail() == null;
    }

    /**
     * Is this a patch activation?
     */
    public boolean isPatch()
    {
        return getBoolean(PARAM_PATCH, false);
    }

    /**
     * Set that this is a patch
     */
    public void setPatch(boolean b)
    {
        setBoolean(PARAM_PATCH, b);
    }

    /**
     * Get email.
     */
    public String getEmail()
    {
        return getString(PARAM_REG_EMAIL);
    }

    /**
     * Get name
     */
    public String getName()
    {
        return getString(PARAM_NAME);
    }

    /**
     * Get address
     */
    public String getAddress()
    {
        return getString(PARAM_ADDRESS);
    }

    /**
     * Get city
     */
    public String getCity()
    {
        return getString(PARAM_CITY);
    }

    /**
     * Get state
     */
    public String getState()
    {
        return getString(PARAM_STATE);
    }

    /**
     * Get postal code
     */
    public String getPostal()
    {
        return getString(PARAM_POSTAL);
    }

    /**
     * Get country
     */
    public String getCountry()
    {
        return getString(PARAM_COUNTRY);
    }

    /**
     * Get operating system
     */
    public String getOS()
    {
        return getString(PARAM_OS);
    }

    /**
     * Set operating system
     */
    private void setOS()
    {
        setString(PARAM_OS, Utils.OS);
    }

    /**
     * Get java version
     */
    public String getJava()
    {
        return getString(PARAM_JAVA_VERSION);
    }

    /**
     * Set java version
     */
    private void setJava()
    {
        setString(PARAM_JAVA_VERSION, System.getProperties().getProperty("java.runtime.version"));
    }
}
