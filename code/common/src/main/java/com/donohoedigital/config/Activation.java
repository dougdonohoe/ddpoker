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
 * Registration.java
 *
 * Created on August 1, 2003, 11:31 AM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Base64;
import com.donohoedigital.base.Utils;

import java.security.MessageDigest;

/**
 * Key of form FFNN-NNNN-NNAA-AAAA
 * <p/>
 * where FF is product id/version
 * NN-NNNN-NN is an 8 digit sequence number (unique)
 * AA-AAAA is a hash of FFNN-NNNN-NN
 *
 * @author donohoe
 */
public class Activation
{
    public static final String REGKEY = "reg";
    private static final String GUID_KEY_START = "KEY-";
    private static final int GUID_LENGTH = 36;
    private static final int RETAIL_KEY_LENGTH = 19;
    private static final int HASH_LENGTH = 7;
    private static final String GUID_PATTERN = "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}";

    public static String createKeyFromGuid(int nStart, String guid, String locale)
    {
        // sample: BD4206D4-72B0-EA5D-6FF7-1B50F92CCD49
        ApplicationError.assertTrue(guid.length() == GUID_LENGTH, "GUID should be " + GUID_LENGTH + " long");
        ApplicationError.assertTrue(guid.matches(GUID_PATTERN), "GUID should be proper format");

        return GUID_KEY_START + nStart + "-" + guid + "-" + hash(guid + nStart, locale);
    }

    /**
     * Validate an ID
     */
    public static boolean validate(int nStart, String sID, String sLocale)
    {
        if (sID == null) return false;

        if (sID.length() == RETAIL_KEY_LENGTH)
        {
            return validateRetail(nStart, sID, sLocale);
        }
        else if (sID.length() > 50 && sID.startsWith(GUID_KEY_START))
        {
            return validateGuid(nStart, sID, sLocale);
        }
        return false;
    }

    private static boolean validateRetail(int nStart, String sID, String sLocale)
    {
        // validate start
        if (!sID.startsWith(String.valueOf(nStart))) return false;

        // validate hash
        String sSeq = sID.substring(0, RETAIL_KEY_LENGTH - HASH_LENGTH);
        String sHash = sID.substring(RETAIL_KEY_LENGTH - HASH_LENGTH);
        return sHash.equals(hash(sSeq, sLocale));
    }

    private static boolean validateGuid(int nStart, String sID, String sLocale)
    {
        // validate start
        final String prefix = GUID_KEY_START + nStart + "-";
        if (!sID.startsWith(prefix)) return false;

        // validate guid
        String guid = sID.substring(prefix.length(), prefix.length() + GUID_LENGTH);
        if (!guid.matches(GUID_PATTERN)) return false;

        // validate hash
        String sHash = sID.substring(sID.length() - HASH_LENGTH);
        return sHash.equals(hash(guid + nStart, sLocale));
    }

    /**
     * Return a hash of the given id
     */
    private static MessageDigest md_ = null;
    private static final byte[] foo = new byte[25];
    private static final StringBuilder sb_ = new StringBuilder(20);

    /**
     * init message digest
     */
    private static synchronized void initDigest()
    {
        if (md_ == null)
        {
            try
            {
                md_ = MessageDigest.getInstance("SHA");
                for (int i = 0; i < foo.length; i++)
                {
                    foo[i] = (byte) ((i * (i + 235) * (i + 231)) % 128);
                }
            }
            catch (Exception e)
            {
                debug(Utils.formatExceptionText(e));
                throw new ApplicationError(e);
            }
        }
        else
        {
            md_.reset();
        }
    }

    /**
     * Guessing that synchronizing is better than creating
     * new md/sb every time
     */
    private static synchronized String hash(String sID, String sLocale)
    {
        initDigest();
        byte[] digest;

        md_.update(Utils.encodeBasic("Writing games is really hard.  Security is hard.  Let's go shopping"));
        md_.update(Utils.encodeBasic(sID));
        md_.update(foo);
        md_.update(Utils.encodeBasic(sID.replace('-', '!')));
        if (sLocale != null) md_.update(Utils.encodeBasic(sLocale));
        digest = md_.digest();

        sb_.setLength(0);
        long nValue;
        for (int i = 0; i < 6; i++)
        {
            nValue = Math.abs(digest[i]) % 10;
            sb_.append(nValue);
            if (i == 1) sb_.append('-');
        }
        return sb_.toString();
    }

    /**
     * Create a hash of real key and string and return it
     */
    public static synchronized String getPublicKey(String sStart, String skey)
    {
        initDigest();
        byte[] digest;
        md_.update(Utils.encodeBasic("if i were clever i'd put something funny here"));
        md_.update(Utils.encodeBasic(sStart));
        md_.update(Utils.encodeBasic(skey));
        digest = md_.digest();
        return Base64.encodeBytes(digest);
    }

    /**
     * Debugging
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    private static void debug(String s)
    {
        System.out.print(s + '\n');
    }
}
