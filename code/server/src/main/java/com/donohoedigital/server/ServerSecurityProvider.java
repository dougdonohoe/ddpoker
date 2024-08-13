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
package com.donohoedigital.server;

import com.donohoedigital.base.*;
import org.apache.log4j.*;

import java.net.*;
import java.security.*;

/**
 * Contains information to drive security routines.
 */
public class ServerSecurityProvider extends com.donohoedigital.base.SecurityProvider
{
    private static final byte[] SALT;
    private static final byte[] ID;

    static
    {
        SALT = new byte[25];

        for (int i = 0; i < SALT.length; i++)
        {
            SALT[i] = (byte) ((i * (i + 235) * (i + 231)) % 128);
        }

        byte[] id;
        try
        {
            id = InetAddress.getLocalHost().getAddress();
        }
        catch (UnknownHostException e)
        {
            Logger.getLogger(ServerSecurityProvider.class).warn("ServerSecurityProvider unable to determine ip address: " +
                                                                e.getMessage());
            id = Utils.encode("0.0.0.0");
        }
        ID = id;
    }

    @Override
    public byte[] k()
    {
        MessageDigest md = null;

        try
        {
            md = MessageDigest.getInstance(getHashAlgorithm());
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        md.update(SALT);
        md.update(ID);
        md.update(Utils.encode(SecurityUtils.class.getName())); // For backward compatibility.

        byte raw[] = md.digest();
        int rawLength = raw.length;
        int lenDiff = (getEncryptionKeyLength() - rawLength);

        if (lenDiff > 0)
        {
            int newLength = rawLength + lenDiff;
            byte[] temp = new byte[newLength];
            System.arraycopy(raw, 0, temp, 0, rawLength);
            raw = temp;

            for (int i = rawLength; i < newLength; ++i)
            {
                raw[i] = ID[0];
            }
        }

        return raw;
    }
}
