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
 * Registration.java
 *
 * Created on August 1, 2003, 11:31 AM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;

import java.security.*;
import java.util.regex.*;

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
    public static final String OLDKEY = "old";
    public static final String BANKEY = "ban";
    public static final String DEMOKEY = "demo";
    public static final Pattern KEY_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$");
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

        // validae hash
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
    private static byte foo[] = new byte[25];
    private static StringBuilder sb_ = new StringBuilder(20);

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

    // COMMENT OUT FOR RELEASE

//    /**
//     * Generate list of sequence numbers
//     */
//    public static void generateIDs(int nKeyStart, int nStart, int nEnd, String sLocale)
//    {
//        long nBase = 100000000l * nKeyStart;
//        long nNum;
//        long nLast = 0;
//        int nMod;
//        String sHash;
//        String sSEQ;
//        String sID;
//        for (int i = nStart; i < nEnd; i++)
//        {
//            // modify each number so gap in sequence isn't predictable
//            // the mod % 29/37 is done so it is less than the multiple
//            // of 43 - we use all primes too for math/mods
//            if (i % 2 == 0) {
//                nMod = (i*(i+29)) % 29;
//            } else {
//                nMod = (i*(i-241)) % 37;
//            }
//            // make sure it is positive
//            nMod = Math.abs(nMod);
//
//            // calclate num
//            nNum = nBase + (i * 43) - nMod;
//
//            // make sure num isn't less than last num
//            if (nNum <= nLast) { debug("ERROR nNum=" + nNum + " nLast="+nLast +" nBase="+nBase + " nMod+"+nMod); return; }
//
//            // get id
//            sSEQ = format(""+nNum);
//
//            // get digest
//            sHash = hash(sSEQ, sLocale);
//
//            // full id
//            sID = sSEQ+sHash;
//
//            // debug print
//            //debug(i+":  SEQ: " + sSEQ + "   ID: " + sID + "   diff: " + (nNum - nLast) + "   valid: " + validate(nKeyStart, sID));
//            debug(sID);
//
//            // remember last
//            nLast = nNum;
//
//            if (i % 10000 == 0) { System.err.println("At: " +i); }
//        }
//    }
//
//    /**
//     * Format the sequence number with dashes
//     */
//    private static String format(String s)
//    {
//        sb_.setLength(0);
//        for (int i = 0; i < s.length(); i++)
//        {
//            sb_.append(s.charAt(i));
//            if (((i+1)%4) == 0 && i != (s.length() -1)) sb_.append("-");
//        }
//        return sb_.toString();
//    }
//
//    /**
//     * Main function to generate IDs
//     */
//    public static void main(String args[])
//    {
//        //// WAR! AGE OF IMPERIALISM
//
//        // note the 11 start matches WarConstants.java
//        //generateIDs(11, 9000, 9100); // War! release 1.0, test ids - BANNED
//        //generateIDs(11, 9100, 9200); // War! release 1.0, private ids
//        //generateIDs(11, 10000, 32000); // War! release 1.0, sent to Denon Digital 8/31/03
//        //generateIDs(11, 40000, 41000); // War! Release 1.2, download version sent to Kati @ Eagle
//        //generateIDs(11, 50000, 50100, "fr"); // War! Release 1.3, french version (private)
//        //generateIDs(11, 51000, 55000, "fr");   // War! Release 1.3, french (sent to nobilis)
//        //generateIDs(11, 55000, 55050, "fr");   // War! Release 1.3, french (sent to nobilis, press keys)
//        //generateIDs(11, 200000, 201000, null);   // War! Release 1.3 downloadable keys set 1 (increase to 200K due to new alg.)
//        //generateIDs(11, 201000, 201100, null);   // War! Release 1.3 downloadable keys IGF set
//
//        //// POKER 1.x
//
//        //generateIDs(20, 1000, 1100, null); // Poker beta keys (20 start matches PokerConstants.java)
//        //generateIDs(21, 1, 100, null); // Poker 1.0 private keys
//        //generateIDs(21, 100, 200, null); // more Poker 1.0 private keys
//        //generateIDs(21, 1000, 2000, null); // Poker 1.0 downloadable keys set 1
//        //generateIDs(21, 2000, 3000, null); // Poker 1.0 downloadable keys set 2
//        //generateIDs(21, 3000, 4000, null); // Poker 1.0 downloadable keys set 3
//        //generateIDs(21, 4000, 5000, null); // Poker 1.0 downloadable keys set 4
//        //generateIDs(21, 5000, 6000, null); // Poker 1.0 downloadable keys set 5
//        //generateIDs(21, 10000, 71000, null); // Poker 1.0 61K for Insight world
//        //generateIDs(21, 71000, 72000, null); // Poker 1.0 1K for Insight world
//        //generateIDs(21, 72000, 92500, null); // Poker 1.1 22.5K for Insight world (sam's club run)
//        //generateIDs(21, 92500, 93200, null); // Poker 1.1 +700 for Insight world (sam's club run)
//        //generateIDs(21, 93200, 156200, null); // Poker 1.1 63K for Insight world (2nd run)
//        //generateIDs(21, 156200, 166100, null); // Poker 1.1 9.9K for Insight world (Ultimate Poker Giftset)
//        //generateIDs(21, 166100, 188100, null); // Poker 1.1 22K for Insight world (3rd run)
//        //generateIDs(21, 188100, 298100, null); // Poker 1.1 110K for Insight world (3rd run)
//
//        //// Poker 2.x
//        //generateIDs(19, 1000, 1100, null); // Poker beta keys (19 start matches PokerConstants.java)
//        //generateIDs(19, 1100, 1200, null); // Poker beta keys (19 start matches PokerConstants.java)
//        //generateIDs(22, 0, 1000, null); // poker private key list
//        //generateIDs(22, 1000, 3000, null); // download set 1
//        //generateIDs(22, 3000, 5000, null); // download set 2
//        //generateIDs(22, 5000, 7000, null); // download set 3
//        generateIDs(22, 7000, 9000, null); // download set 4
//        //generateIDs(22, 20000, 60000, null); // retail set 1
//        //generateIDs(22, 60000, 85000, null); // retail set 2
//        //generateIDs(22, 85000, 90010, null); // retail - trymedia
//    }
// 
}
