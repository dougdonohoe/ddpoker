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
package com.donohoedigital.base;

import javax.crypto.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;

/**
 * Provides security related utility methods.  Base64 is used for all encoding.
 */
public class SecurityUtils
{
    /**
     * Use default provider.
     */
    private static SecurityProvider provider_ = new SecurityProvider();

    /**
     * Set the security provider to use.
     */
    public static void setSecurityProvider(SecurityProvider provider)
    {
        provider_ = provider;
    }

    /**
     * Hash and encode a value using the given key (e.g., &quot;salt&quot;).
     *
     * @param stream    stream
     * @param key       optional key
     * @param algorithm optional algorithm
     * @param algorithm
     * @return the hashed value
     * @throws IOException if there is an error reading from the stream
     */
    public static String hash(InputStream stream, byte[] key, String algorithm) throws IOException
    {
        byte[] raw = hashRaw(stream, key, algorithm);
        return Base64.encodeBytes(raw);
    }

    /**
     * Hash a value using the given key (e.g., &quot;salt&quot;).
     *
     * @param stream    stream
     * @param key       optional key
     * @param algorithm optional algorithm
     * @param algorithm
     * @return the raw hashed value
     * @throws IOException if there is an error reading from the stream
     */
    public static byte[] hashRaw(InputStream stream, byte[] key, String algorithm) throws IOException
    {
        if (algorithm == null)
        {
            algorithm = provider_.getHashAlgorithm();
        }

        MessageDigest md = null;

        try
        {
            md = MessageDigest.getInstance(algorithm);
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        if (key != null) md.update(key);

        byte[] bytes = new byte[1024];
        int bytesRead = -1;

        while ((bytesRead = stream.read(bytes)) != -1)
        {
            md.update(bytes, 0, bytesRead);
        }

        return md.digest();
    }

    /**
     * Hash and encode a value using the given key (e.g., &quot;salt&quot;).
     *
     * @param value     value
     * @param key       optional key
     * @param algorithm optional algorithm
     * @param algorithm
     * @return the hashed value
     */
    public static String hash(byte[] value, byte[] key, String algorithm)
    {
        byte[] raw = hashRaw(value, key, algorithm);
        return Base64.encodeBytes(raw);
    }

    /**
     * Hash a value using the given key (e.g., &quot;salt&quot;).
     *
     * @param value     value
     * @param key       optional key
     * @param algorithm optional algorithm
     * @param algorithm
     * @return the raw hashed value
     */
    public static byte[] hashRaw(byte[] value, byte[] key, String algorithm)
    {
        if (algorithm == null)
        {
            algorithm = provider_.getHashAlgorithm();
        }

        MessageDigest md = null;

        try
        {
            md = MessageDigest.getInstance(algorithm);
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        if (key != null) md.update(key);
        md.update(value);

        return md.digest();
    }

    /**
     * Encrypt and encode a value using the given key.
     *
     * @param value value
     * @param key   key
     * @return the encrypted value
     */
    public static String encrypt(byte[] value, byte[] key)
    {
        byte[] raw = encryptRaw(value, key);
        return Base64.encodeBytes(raw);
    }

    /**
     * Encrypt a value using the given key.
     *
     * @param value value
     * @param key   key
     * @return the raw encrypted value
     */
    public static byte[] encryptRaw(byte[] value, byte[] key)
    {
        byte[] raw = null;

        try
        {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key);

            raw = cipher.doFinal(value);
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        return raw;
    }

    /**
     * Decode and decrypt a value using the given key.
     *
     * @param value value
     * @param key   key
     * @return the decypted value
     */
    public static byte[] decrypt(String value, byte[] key)
    {
        byte[] raw = Base64.decode(value);
        return decryptRaw(raw, key);
    }

    /**
     * Decrypt a value using the given key.
     *
     * @param value value
     * @param key   key
     * @return the decrypted value
     */
    public static byte[] decryptRaw(byte[] value, byte[] key)
    {
        byte[] decrypted = null;

        try
        {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key);
            decrypted = cipher.doFinal(value);
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        return decrypted;
    }

    /**
     * Generate a random key for use in hashing and encryption.
     *
     * @return random key
     */
    public static byte[] generateKey()
    {
        byte[] generated = null;

        try
        {
            KeyGenerator generator = KeyGenerator.getInstance(provider_.getEncryptionAlgorithm());
            generator.init(provider_.getEncryptionKeySize(), new SecureRandom());
            Key key = generator.generateKey();
            generated = key.getEncoded();
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        return generated;
    }

    /**
     * Encrypt a key for storing in files and databases.
     */
    public static String encryptKey(byte[] key)
    {
        return encrypt(key, provider_.k());
    }

    /**
     * Encrypt a key for storing in files and databases.
     */
    public static byte[] decryptKey(String key)
    {
        return decrypt(key, provider_.k());
    }

    /**
     * Get a cipher used for encryption/decryption.
     *
     * @param key  key
     * @param mode
     * @return the cipher
     */
    private static Cipher getCipher(int mode, byte[] key)
    {
        Cipher cipher = null;

        try
        {
            KeySpec keySpec = provider_.getEncryptionKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(provider_.getEncryptionAlgorithm());
            SecretKey secret = keyFactory.generateSecret(keySpec);

            cipher = Cipher.getInstance(provider_.getEncryptionAlgorithm());
            cipher.init(mode, secret);
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        return cipher;
    }

    // share (thread safe) instance due to long instantiation time
    private static SecureRandom random = null;

    /**
     * Get secure random object
     */
    public synchronized static SecureRandom getSecureRandom()
    {
        if (random == null)
        {
            try
            {
                random = SecureRandom.getInstance("SHA1PRNG");
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new ApplicationError(e);
            }
        }
        return random;
    }

    /**
     * get MD5 hash of string
     */
    public static String getMD5Hash(String s, byte key[])
    {
        return hash(s.getBytes(), key, "MD5");
    }
}
