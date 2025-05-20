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
package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.server.*;

import java.io.*;


/**
 * Encrypts a string using a given key.
 */
public class EncryptString
{
    /**
     * Sets up application-specific command line options.
     */
    private static void setupCommandLineOptions() {

        CommandLine.setUsage("EncryptString [options]");

        CommandLine.setMinParams(1);
        CommandLine.setMaxParams(1);
        CommandLine.setParamDescription("string", "String to encrypt (UTF-8 encoded)", "string");

        CommandLine.addStringOption("rawKey", null);
        CommandLine.setDescription("rawKey", "File containing the raw key value", "filename");

        CommandLine.addStringOption("encryptedKey", null);
        CommandLine.setDescription("encryptedKey", "File containing the encrypted key value", "filename");

        CommandLine.addStringOption("out", null);
        CommandLine.setDescription("out", "File to which the encrypted string is written", "filename");

        return;
    }


    /**
     * Parse the args and do as told.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        // Use the server security provider.
        SecurityUtils.setSecurityProvider(new ServerSecurityProvider());

        setupCommandLineOptions();
        CommandLine.parseArgs(args);
        TypedHashMap hmOptions = CommandLine.getOptions();
        String[] params = CommandLine.getRemainingArgs();

        String rawKeyPath = hmOptions.getString("rawKey");
        String encryptedKeyPath = hmOptions.getString("encryptedKey");
        String outPath = hmOptions.getString("out");
        String value = params[0];

        byte[] key = null;

        if (rawKeyPath != null)
        {
            FileInputStream fis = null;
            File rawKeyFile = new File(rawKeyPath);
            key = new byte[(int) rawKeyFile.length()];

            try
            {
                fis = new FileInputStream(rawKeyFile);
                fis.read(key);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.exit(-1);
            }
            finally
            {
                try { if (fis != null) fis.close(); } catch (IOException e) { }
            }
        }
        else if (encryptedKeyPath != null)
        {
            FileInputStream fis = null;
            File encryptedKeyFile = new File(encryptedKeyPath);
            byte[] encryptedKey = new byte[(int) encryptedKeyFile.length()];

            try
            {
                fis = new FileInputStream(encryptedKeyFile);
                fis.read(encryptedKey);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.exit(-1);
            }
            finally
            {
                try { if (fis != null) fis.close(); } catch (IOException e) { }
            }

            key = SecurityUtils.decryptKey(Utils.decode(encryptedKey));
        }
        else
        {
            System.out.println("No key file.");
            System.exit(-1);
        }

        String encryptedValue = SecurityUtils.encrypt(Utils.encode(value), key);

        if (outPath != null)
        {
            FileOutputStream fos = null;

            try
            {
                fos = new FileOutputStream(outPath);

                fos.write(Utils.encode(encryptedValue));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.exit(-1);
            }
            finally
            {
                try { if (fos != null) fos.close(); } catch (IOException e) { }
            }
        }
        else
        {
            System.out.println(encryptedValue);
        }
    }
}
