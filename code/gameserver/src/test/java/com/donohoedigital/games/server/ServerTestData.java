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
package com.donohoedigital.games.server;

import com.donohoedigital.comms.*;
import com.donohoedigital.games.server.model.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2008
 * Time: 12:24:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerTestData
{

    public static Registration createRegistration(String sName, String sKey)
    {
        Registration reg = new Registration();
        reg.setAddress("123 Test Street");
        reg.setBanAttempt(false);
        reg.setCity("Wayzata");
        reg.setCountry("US");
        reg.setDuplicate(false);
        reg.setEmail("test@example.com");
        reg.setHostName("dexter.example.com");
        reg.setHostNameModified("zorro.example.com");
        reg.setIp("192.168.1.1");
        reg.setJavaVersion("1.5.0.03-b07");
        reg.setLicenseKey(sKey);
        reg.setName(sName);
        reg.setOperatingSystem("MacOS 10.5");
        reg.setPort(80);
        reg.setPostal("90210");
        reg.setType(Registration.Type.REGISTRATION);
        reg.setServerTime(new Date());
        reg.setState("MN");
        reg.setVersion(new Version(2, 5, 3, true));

        return reg;
    }

    public static BannedKey createBannedKey(String sKey)
    {
        BannedKey key = new BannedKey();
        key.setComment("Test ban comment");
        key.setKey(sKey);

        return key;
    }

    public static UpgradedKey createUpgradedKey(String sKey)
    {
        UpgradedKey key = new UpgradedKey();
        key.setCount(11);
        key.setLicenseKey(sKey);

        return key;
    }
}